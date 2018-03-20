package com.android.launcher3.widget.model;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.TransactionTooLargeException;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.PackageItemInfo;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WidgetLoader {
    public static final int INVALID_VERSION = -1;
    private static final String TAG = "WidgetLoader";
    private final LauncherAppState mApp;
    private long mDataVersion = -1;
    private boolean mHasUninstallApps = false;
    private IconCache mIconCache;
    private ArrayList<LoadListener> mListeners = new ArrayList();
    private LoadTask mLoadTask;
    private List<Object> mWidgetItems;
    private List<LauncherAppWidgetProviderInfo> mWidgetProviders = new ArrayList();

    public interface LoadListener {
        void onLoadComplete();
    }

    private class LoadTask extends Thread {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private SyncContext syncContext;

        private LoadTask() {
            this.mActive = true;
            this.mDirty = true;
            this.syncContext = new SyncContext();
        }

        public void run() {
            boolean updateComplete = false;
            while (this.mActive) {
                synchronized (this) {
                    if (this.mActive && !this.mDirty && updateComplete) {
                        this.mDirty = false;
                        Log.d(WidgetLoader.TAG, "enter wait");
                        WidgetLoader.waitWithoutInterrupt(this);
                    } else {
                        Log.d(WidgetLoader.TAG, "start loading");
                        this.mDirty = false;
                        this.syncContext.reset();
                        List<Object> widgets = WidgetLoader.this.loadWidgetAndShortcut(this.syncContext);
                        if (!this.syncContext.stopped) {
                            WidgetLoader.this.notifyLoadComplete(widgets, this.syncContext.hasUninstallApp);
                        }
                        updateComplete = true;
                    }
                }
            }
        }

        private synchronized void notifyDirty() {
            this.mDirty = true;
            notifyAll();
        }

        private synchronized void terminate() {
            this.mActive = false;
            this.syncContext.stopped = true;
            notifyAll();
        }
    }

    private static class SyncContext {
        public boolean hasUninstallApp;
        public boolean stopped;

        private SyncContext() {
            this.hasUninstallApp = false;
            this.stopped = false;
        }

        public void reset() {
            this.hasUninstallApp = false;
            this.stopped = false;
        }
    }

    public WidgetLoader(LauncherAppState app) {
        this.mApp = app;
        this.mIconCache = this.mApp.getIconCache();
    }

    public void notifyDirty(String[] packages, UserHandleCompat user, boolean refresh) {
        if (!refresh && !needToRefresh(packages, user)) {
            Log.d(TAG, "ignore dirty because widgets are not changed");
        } else if (this.mLoadTask == null) {
            this.mLoadTask = new LoadTask();
            this.mLoadTask.start();
        } else {
            this.mLoadTask.notifyDirty();
        }
    }

    public void setLoaderTaskStop(boolean isStopped) {
        if (isStopped && this.mLoadTask != null) {
            this.mLoadTask.terminate();
            this.mLoadTask = null;
        }
    }

    public boolean getLoaderTaskStop() {
        return this.mLoadTask == null;
    }

    public synchronized void setLoadListener(LoadListener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public synchronized void removeLoadListener(LoadListener listener) {
        if (this.mListeners.contains(listener)) {
            this.mListeners.remove(listener);
        }
    }

    public boolean hasUninstallApps() {
        return this.mHasUninstallApps;
    }

    public synchronized long getDataVersion() {
        return this.mDataVersion;
    }

    public synchronized List<Object> getWidgetItems() {
        return this.mWidgetItems;
    }

    private synchronized void notifyLoadComplete(List<Object> widgets, boolean hasUninstallApps) {
        this.mHasUninstallApps = hasUninstallApps;
        this.mWidgetItems = widgets;
        this.mDataVersion++;
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((LoadListener) it.next()).onLoadComplete();
        }
    }

    private List<Object> loadWidgetAndShortcut(SyncContext syncContext) {
        List<Object> widgets = new ArrayList();
        List<LauncherAppWidgetProviderInfo> widgetProviders = getWidgetProviders();
        widgets.addAll(widgetProviders);
        widgets.addAll(getShortcutProviders());
        if (syncContext.stopped) {
            return null;
        }
        widgets = makeGroupAndSort(widgets, syncContext);
        if (syncContext.stopped) {
            return null;
        }
        if (!syncContext.stopped) {
            synchronized (this.mWidgetProviders) {
                this.mWidgetProviders.clear();
                this.mWidgetProviders.addAll(widgetProviders);
            }
        }
        return widgets;
    }

    private List<Object> makeGroupAndSort(List<Object> widgets, SyncContext syncContext) {
        LinkedHashMap<String, String> widgetNameMap = new LinkedHashMap();
        LinkedHashMap<String, Object> widgetMap = new LinkedHashMap();
        PendingAddItemInfo itemInfo = null;
        for (LauncherAppWidgetProviderInfo provider : widgets) {
            if (syncContext.stopped) {
                break;
            }
            if (provider instanceof LauncherAppWidgetProviderInfo) {
                itemInfo = new PendingAddWidgetInfo(this.mApp.getContext(), provider, null);
            } else if (provider instanceof ResolveInfo) {
                itemInfo = new PendingAddShortcutInfo((ResolveInfo) provider);
            }
            if (itemInfo != null) {
                if (!syncContext.hasUninstallApp) {
                    syncContext.hasUninstallApp = itemInfo.uninstallable(this.mApp.getContext());
                }
                int userId = itemInfo.user.hashCode();
                String key = userId + "@" + itemInfo.componentName.getPackageName();
                ArrayList<PendingAddItemInfo> value = widgetMap.get(key);
                if (value != null) {
                    PackageItemInfo pInfo = new PackageItemInfo(itemInfo.componentName.getPackageName());
                    this.mIconCache.getTitleAndIconForApp(pInfo.packageName, itemInfo.user, true, pInfo);
                    String applicationLabel = pInfo.title == null ? "" : pInfo.title.toString();
                    itemInfo.setApplicationLabel(applicationLabel);
                    ArrayList<PendingAddItemInfo> items = value;
                    if (items.size() == 1) {
                        ((PendingAddItemInfo) items.get(0)).setApplicationLabel(applicationLabel);
                        widgetNameMap.put(key, userId + "@" + applicationLabel);
                    }
                    value.add(itemInfo);
                } else {
                    String nameValue = userId + "@" + itemInfo.getLabel(this.mApp.getContext());
                    List<PendingAddItemInfo> items2 = new ArrayList();
                    items2.add(itemInfo);
                    widgetMap.put(key, items2);
                    widgetNameMap.put(key, nameValue);
                }
            }
        }
        if (syncContext.stopped) {
            return null;
        }
        return new ArrayList(getSortedByPackage(sortByValues(widgetNameMap), widgetMap).values());
    }

    private List<ResolveInfo> getShortcutProviders() {
        return this.mApp.getContext().getPackageManager().queryIntentActivities(new Intent("android.intent.action.CREATE_SHORTCUT"), 0);
    }

    private List<LauncherAppWidgetProviderInfo> getWidgetProviders() {
        ArrayList<LauncherAppWidgetProviderInfo> results = new ArrayList();
        try {
            List<AppWidgetProviderInfo> widgets = AppWidgetManagerCompat.getInstance(this.mApp.getContext()).getAllProviders();
            Set<ComponentName> excludeWidgets = new HashSet();
            if (!LauncherAppState.getInstance().isEasyModeEnabled()) {
                List<ResolveInfo> list = this.mApp.getContext().getPackageManager().queryBroadcastReceivers(new Intent("android.appwidget.action.EASY_MODE"), 0);
                if (!(list == null || list.isEmpty())) {
                    for (ResolveInfo info : list) {
                        excludeWidgets.add(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    }
                }
            }
            if (!TestHelper.isRoboUnitTest() && Utilities.isKnoxMode()) {
                excludeWidgets.add(new ComponentName("com.android.settings", "com.android.settings.dualsimwidget.DualsimWidget_data"));
            }
            for (AppWidgetProviderInfo pInfo : widgets) {
                if (!(excludeWidgets.contains(pInfo.provider) || "com.sec.android.widget.myeventwidget".equals(pInfo.provider.getPackageName()))) {
                    results.add(LauncherAppWidgetProviderInfo.fromProviderInfo(this.mApp.getContext(), pInfo));
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                if (this.mWidgetProviders != null) {
                    results.addAll(this.mWidgetProviders);
                }
                Log.d(TAG, "TransactionTooLargeException : " + results.size());
            } else {
                throw e;
            }
        }
        return results;
    }

    private Map<String, Object> getSortedByPackage(Map<String, String> sortedMap, LinkedHashMap<String, Object> unSortedMap) {
        final Collator collector = Collator.getInstance(Locale.getDefault());
        Map<String, Object> sortedMapItems = sortedMap;
        for (Entry<String, Object> entry : sortedMapItems.entrySet()) {
            String key = (String) entry.getKey();
            List<PendingAddItemInfo> widgets = (List) unSortedMap.get(key);
            if (widgets.size() > 1) {
                Collections.sort(widgets, new Comparator<PendingAddItemInfo>() {
                    public int compare(PendingAddItemInfo L, PendingAddItemInfo R) {
                        String left = L.mLabel != null ? L.mLabel : L.getApplicationLabel();
                        String right = R.mLabel != null ? R.mLabel : R.getApplicationLabel();
                        if (L instanceof PendingAddWidgetInfo) {
                            PendingAddWidgetInfo pL = (PendingAddWidgetInfo) L;
                            if (pL.info.label != null) {
                                left = pL.info.label;
                            }
                        }
                        if (R instanceof PendingAddWidgetInfo) {
                            PendingAddWidgetInfo pR = (PendingAddWidgetInfo) R;
                            if (pR.info.label != null) {
                                right = pR.info.label;
                            }
                        }
                        return collector.compare(left, right);
                    }
                });
            }
            sortedMapItems.put(key, widgets);
        }
        return sortedMapItems;
    }

    public static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Entry<K, V>> entries = new LinkedList(map.entrySet());
        final Collator mCollator = Collator.getInstance(Locale.getDefault());
        Collections.sort(entries, new Comparator<Entry<K, V>>() {
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                if (o1.getValue() != null && o2.getValue() != null) {
                    return mCollator.compare(o1.getValue(), o2.getValue());
                }
                if (o1.getValue() == o2.getValue()) {
                    return 0;
                }
                return o1.getValue() == null ? -1 : 1;
            }
        });
        Map<K, V> sortedMap = new LinkedHashMap();
        for (Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private static void waitWithoutInterrupt(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            Log.w(TAG, "unexpected interrupt: " + object);
        }
    }

    private boolean needToRefresh(String[] packages, UserHandleCompat user) {
        if (this.mWidgetItems == null || this.mWidgetItems.isEmpty()) {
            Log.d(TAG, "needToRefresh, before init");
            return true;
        }
        try {
            for (ArrayList<PendingAddItemInfo> it : this.mWidgetItems) {
                Iterator it2 = it.iterator();
                while (it2.hasNext()) {
                    PendingAddItemInfo item = (PendingAddItemInfo) it2.next();
                    for (String p : packages) {
                        if (item.componentName.getPackageName().equals(p) && item.user.equals(user)) {
                            Log.d(TAG, "needToRefresh : " + p);
                            return true;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.w(TAG, "ignore refresh widget loader");
        }
        return false;
    }
}
