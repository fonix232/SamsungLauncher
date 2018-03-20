package com.android.launcher3;

import android.app.Notification;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SemSystemProperties;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.Log;
import com.android.launcher3.allapps.model.AppsModel;
import com.android.launcher3.allapps.model.AppsModel.ModelListener;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.scloud.SCloudBnr;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat.OnAppsChangedCallbackCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.model.BadgeCache;
import com.android.launcher3.common.model.BadgeCache.CacheKey;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.DataLoader.DataLoaderCallback;
import com.android.launcher3.common.model.DataLoader.DataLoaderState;
import com.android.launcher3.common.model.DeferredHandler;
import com.android.launcher3.common.model.DisableableAppCache;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.home.HomeLoader;
import com.android.launcher3.home.ManagedProfileHeuristic;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.notification.NotificationListener.NotificationsChangedListener;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.ShortcutTray;
import com.android.launcher3.util.StringFilter;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.model.WidgetLoader;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.knox.SemPersonaManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LauncherModel extends BroadcastReceiver implements OnAppsChangedCallbackCompat, NotificationsChangedListener {
    public static final String ACTION_EDM_UNINSTALL_STATUS_INTERNAL = "com.samsung.android.knox.intent.action.EDM_UNINSTALL_STATUS_INTERNAL";
    public static final String ACTION_MANAGED_PROFILE_REFRESH = "com.samsung.android.knox.container.MANAGED_PROFILE_REFRESH";
    public static final String ACTION_SPR_FORCE_REFRESH = "com.sec.sprextension.FORCE_LAUNCHER_REFRESH";
    public static final String ACTION_STK_TITLE_IS_LOADED = "android.intent.action.STK_TITLE_IS_LOADED";
    private static final boolean DEBUG_LOADERS = true;
    private static final boolean DEBUG_RECEIVER = false;
    public static final String ICON_BACKGROUNDS_CHANGED = "com.samsung.settings.ICON_BACKGROUNDS_CHANGED";
    private static final String NOTI_TAG = "Notification.Badge";
    private static final String TAG = "Launcher.Model";
    public static int mPreBadgeSettingValue;
    private static final Object sBgLock = new Object();
    private static final ArrayList<Runnable> sPackageChangeRunnables = new ArrayList();
    public static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());
    private boolean mAllAppsLoaded;
    private final LauncherAppState mApp;
    private final boolean mAppsCanBeOnRemovableStorage;
    private AppsModel mAppsModel;
    private BadgeCache mBadgeCache;
    private WeakReference<Callbacks> mCallbacks;
    private volatile int mConfigMccWhenLoaded;
    private volatile int mConfigMncWhenLoaded;
    private DeepShortcutManager mDeepShortcutManager;
    private boolean mDeepShortcutsLoaded;
    private DisableableAppCache mDisableableAppCache;
    private final DeferredHandler mHandler = new DeferredHandler();
    private boolean mHasLoaderCompletedOnce;
    private HomeLoader mHomeLoader;
    private IconCache mIconCache;
    private boolean mIsLoaderTaskRunning;
    private LoaderTask mLoaderTask;
    private final Object mLock = new Object();
    private boolean mNeedLoadAllAppItemList = false;
    private OnAllAppItemListLoadCompletedListener mOnAllAppItemListLoadCompletedListener;
    private final ArrayList<OnBadgeBindingCompletedListener> mOnBadgeBindingCompletedListeners = new ArrayList();
    private final ArrayList<OnLauncherBindingItemsCompletedListener> mOnLauncherBindingItemsCompletedListeners = new ArrayList();
    private final ArrayList<OnNotificationPreviewBindingListener> mOnNotificationPreviewBindingListener = new ArrayList();
    private final ArrayList<OnRefreshLiveIconListener> mOnRefreshLiveIconListener = new ArrayList();
    private final Map<PackageUserKey, BadgeInfo> mPackageUserToBadgeInfos = new HashMap();
    private WidgetLoader mWidgetLoader;
    private boolean mWorkspaceLoaded;

    public interface Callbacks {
        void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> multiHashMap);

        void dumpLogsToLocalData();

        boolean isHomeNormal();

        boolean isTrayAnimating();

        void recreateLauncher();

        void refreshNotifications();

        void relayoutLauncher();

        boolean setLoadOnResume();

        void updateZeroPage(int i);
    }

    public interface OnAllAppItemListLoadCompletedListener {
        boolean isTopStage();

        void onAllAppItemListLoadCompleted(ArrayList<IconInfo> arrayList);
    }

    public interface OnBadgeBindingCompletedListener {
        void onBadgeBindingCompleted(ArrayList<ItemInfo> arrayList);
    }

    public interface OnLauncherBindingItemsCompletedListener {
        void onLauncherBindingItemsCompleted();
    }

    public interface OnNotificationPreviewBindingListener {
        void onNotificationPreviewBinding(Map<PackageUserKey, BadgeInfo> map);
    }

    public interface OnRefreshLiveIconListener {
        void onRefreshLiveIcon();
    }

    public class PackageUpdatedTask implements Runnable {
        public static final int OP_ADD = 1;
        public static final int OP_REMOVE = 3;
        static final int OP_SUSPEND = 5;
        public static final int OP_UNAVAILABLE = 4;
        static final int OP_UNSUSPEND = 6;
        public static final int OP_UPDATE = 2;
        static final int OP_USER_AVAILABILITY_CHANGE = 7;
        final int mOp;
        String[] mPackages;
        final UserHandleCompat mUser;

        public PackageUpdatedTask(int op, String[] packages, UserHandleCompat user) {
            this.mOp = op;
            this.mPackages = packages;
            this.mUser = user;
        }

        public void run() {
            if (LauncherModel.this.mHasLoaderCompletedOnce) {
                checkZeroPageUpdate();
                if (this.mPackages == null) {
                    Log.i(LauncherModel.TAG, "PackageUpdatedTask, package is empty!");
                    return;
                }
                Context context = LauncherModel.this.mApp.getContext();
                if (UserManagerCompat.getInstance(context).getUserProfiles().contains(this.mUser)) {
                    int i;
                    String[] packages = this.mPackages;
                    boolean isHomeOnly = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
                    DataLoader loader = isHomeOnly ? LauncherModel.this.mHomeLoader : LauncherModel.this.mAppsModel;
                    ArrayList<ItemInfo> currentItems = isHomeOnly ? LauncherModel.this.mHomeLoader.getAllAppItemInHome() : LauncherModel.this.mAppsModel.getAllAppItemInApps();
                    ManagedProfileHeuristic heuristic;
                    FlagOp flagOp;
                    StringFilter pkgFilter;
                    switch (this.mOp) {
                        case OP_ADD:
                            for (String pkg : packages) {
                                Log.d(LauncherModel.TAG, "Loader.addPackage " + pkg);
                                LauncherModel.this.mIconCache.updateIconsForPkg(pkg, this.mUser);
                                LauncherModel.this.mDisableableAppCache.updateForPkg(pkg);
                                loader.updatePackage(pkg, currentItems, this.mUser);
                            }
                            heuristic = ManagedProfileHeuristic.get(context, this.mUser);
                            if (heuristic != null) {
                                heuristic.processPackageAdd(this.mPackages);
                                if (isHomeOnly) {
                                    Log.d(LauncherModel.TAG, "This user package was added by ManagedProfileHeuristic");
                                    loader.added.clear();
                                    break;
                                }
                            }
                            break;
                        case OP_UPDATE:
                            for (String pkg2 : packages) {
                                Log.d(LauncherModel.TAG, "Loader.updatePackage " + pkg2);
                                LauncherModel.this.mIconCache.updateIconsForPkg(pkg2, this.mUser);
                                loader.updatePackage(pkg2, currentItems, this.mUser);
                                LauncherModel.this.mApp.getWidgetCache().removePackage(pkg2, this.mUser);
                            }
                            break;
                        case OP_REMOVE:
                            heuristic = ManagedProfileHeuristic.get(context, this.mUser);
                            if (heuristic != null) {
                                heuristic.processPackageRemoved(this.mPackages);
                            }
                            for (String pkg22 : packages) {
                                Log.d(LauncherModel.TAG, "Loader.removePackage " + pkg22);
                                LauncherModel.this.mIconCache.removeIconsForPkg(pkg22, this.mUser);
                                loader.removePackage(pkg22, currentItems, this.mUser);
                                LauncherModel.this.mApp.getWidgetCache().removePackage(pkg22, this.mUser);
                            }
                            break;
                        case OP_UNAVAILABLE:
                            for (String pkg222 : packages) {
                                Log.d(LauncherModel.TAG, "Loader.disablePackages " + pkg222);
                                loader.updateUnavailablePackage(pkg222, this.mUser, 32);
                                LauncherModel.this.mApp.getWidgetCache().removePackage(pkg222, this.mUser);
                            }
                            break;
                        case OP_SUSPEND:
                        case OP_UNSUSPEND:
                            if (this.mOp == 5) {
                                flagOp = FlagOp.addFlag(4);
                            } else {
                                flagOp = FlagOp.removeFlag(4);
                            }
                            pkgFilter = StringFilter.of(new HashSet(Arrays.asList(packages)));
                            LauncherModel.this.mHomeLoader.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                            if (!isHomeOnly) {
                                LauncherModel.this.mAppsModel.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                                return;
                            }
                            return;
                        case OP_USER_AVAILABILITY_CHANGE:
                            if (UserManagerCompat.getInstance(context).isQuietModeEnabled(this.mUser)) {
                                flagOp = FlagOp.addFlag(8);
                            } else {
                                flagOp = FlagOp.removeFlag(8);
                            }
                            pkgFilter = StringFilter.matchesAll();
                            LauncherModel.this.mHomeLoader.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                            if (!isHomeOnly) {
                                LauncherModel.this.mAppsModel.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                                return;
                            }
                            return;
                    }
                    ArrayList<IconInfo> added = null;
                    ArrayList<IconInfo> modified = null;
                    ArrayList<ItemInfo> removedApps = new ArrayList();
                    if (loader.added.size() > 0) {
                        added = new ArrayList(loader.added);
                        loader.added.clear();
                    }
                    if (loader.modified.size() > 0) {
                        modified = new ArrayList(loader.modified);
                        loader.modified.clear();
                    }
                    if (loader.removed.size() > 0) {
                        removedApps.addAll(loader.removed);
                        loader.removed.clear();
                    }
                    final Callbacks callbacks = LauncherModel.this.getCallback();
                    if (callbacks != null || LauncherModel.this.mWorkspaceLoaded || LauncherModel.this.mAllAppsLoaded) {
                        Iterator it;
                        IconInfo ai;
                        HashMap<ComponentName, IconInfo> addedOrUpdatedApps = new HashMap();
                        if (added != null) {
                            it = added.iterator();
                            while (it.hasNext()) {
                                ai = (IconInfo) it.next();
                                addedOrUpdatedApps.put(ai.componentName, ai);
                            }
                        }
                        if (modified != null) {
                            it = modified.iterator();
                            while (it.hasNext()) {
                                ai = (IconInfo) it.next();
                                addedOrUpdatedApps.put(ai.componentName, ai);
                            }
                        }
                        ArrayList<String> removedPackageNames = new ArrayList();
                        if (this.mOp == 3 || this.mOp == 4) {
                            removedPackageNames.addAll(Arrays.asList(packages));
                        } else if (this.mOp == 2) {
                            for (String pkg2222 : packages) {
                                if (LauncherModel.isPackageDisabled(context, pkg2222, this.mUser)) {
                                    removedPackageNames.add(pkg2222);
                                }
                            }
                        }
                        if (!(removedPackageNames.isEmpty() && removedApps.isEmpty())) {
                            int removeReason;
                            if (this.mOp == 4) {
                                removeReason = 32;
                                it = removedPackageNames.iterator();
                                while (it.hasNext()) {
                                    LauncherModel.this.mHomeLoader.updateUnavailablePackage((String) it.next(), this.mUser, 32);
                                }
                            } else {
                                removeReason = 0;
                            }
                            ArrayList<ItemInfo> removedAppItems = LauncherModel.this.mHomeLoader.removePackagesAndComponents(removedPackageNames, removedApps, this.mUser, removeReason);
                            if (!isHomeOnly) {
                                LauncherModel.this.mAppsModel.removePackagesAndComponents(removedAppItems, this.mUser);
                            }
                        }
                        if ((this.mOp == 1 || this.mOp == 2) && !addedOrUpdatedApps.isEmpty()) {
                            LauncherModel.this.mHomeLoader.addOrUpdater(packages, added, addedOrUpdatedApps, this.mUser);
                            if (!isHomeOnly) {
                                LauncherModel.this.mAppsModel.addOrUpdater(packages, addedOrUpdatedApps, this.mUser);
                            }
                        }
                        if ((this.mOp == 2 || this.mOp == 3) && callbacks != null) {
                            for (String pkg22222 : packages) {
                                if (LauncherModel.isPackageDisabled(context, pkg22222, this.mUser) && pkg22222.equals(GameHomeManager.GAME_HOME_PACKAGE) && this.mUser.equals(UserHandleCompat.myUserHandle())) {
                                    Log.d(LauncherModel.TAG, "GameHome is disabled, show all game apps : " + this.mOp);
                                    GameHomeManager.getInstance().resetGameHomeHiddenValue();
                                }
                            }
                        }
                        if (this.mOp == 1 || this.mOp == 3 || this.mOp == 2) {
                            boolean needToRefresh;
                            if (this.mOp == 2) {
                                for (String pkg222222 : packages) {
                                    LauncherModel.this.mHomeLoader.removeWidgetIfNeeded(pkg222222, this.mUser);
                                }
                            }
                            synchronized (LauncherModel.sBgLock) {
                                needToRefresh = HomeLoader.checkNeedToRefreshWidget(this.mPackages, this.mUser, !this.mUser.equals(UserHandleCompat.myUserHandle()));
                            }
                            if (!(needToRefresh || this.mOp == 3)) {
                                PackageManager pm = context.getPackageManager();
                                for (String pkg2222222 : this.mPackages) {
                                    needToRefresh |= !pm.queryBroadcastReceivers(new Intent("android.appwidget.action.APPWIDGET_UPDATE").setPackage(pkg2222222), 0).isEmpty() ? 1 : 0;
                                    if (pm.queryIntentActivities(new Intent("android.intent.action.CREATE_SHORTCUT").setPackage(pkg2222222), 0).isEmpty()) {
                                        i = 0;
                                    } else {
                                        i = 1;
                                    }
                                    needToRefresh |= i;
                                }
                                PostPositionController pp = PostPositionController.getInstance(LauncherModel.this.mApp.getContext());
                                if (pp != null && pp.isEnabled()) {
                                    for (String pkg22222222 : this.mPackages) {
                                        pp.addItem(pkg22222222, true);
                                    }
                                }
                            }
                            LauncherModel.this.loadWidgetsAndShortcuts(this.mPackages, this.mUser, needToRefresh);
                        }
                        LauncherModel.this.loadAllAppItemList();
                        LauncherModel.this.mHandler.post(new Runnable() {
                            public void run() {
                                Callbacks cb = LauncherModel.this.getCallback();
                                if (cb != null && callbacks == cb) {
                                    callbacks.dumpLogsToLocalData();
                                }
                            }
                        });
                        return;
                    }
                    Log.w(LauncherModel.TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                    return;
                }
                Log.i(LauncherModel.TAG, "PackageUpdatedTask, " + this.mUser + " is not exist");
                return;
            }
            Log.i(LauncherModel.TAG, "PackageUpdatedTask, Loader is not completed once");
            LauncherModel.sPackageChangeRunnables.add(this);
        }

        private void checkZeroPageUpdate() {
            final Callbacks callbacks = LauncherModel.this.getCallback();
            if (callbacks == null) {
                Log.w(LauncherModel.TAG, "checkZeroPageUpdate return,  Launcher is probably loading.");
            } else if (this.mOp != 2 && this.mOp != 3 && this.mOp != 1) {
                Log.w(LauncherModel.TAG, "checkZeroPageUpdate return, operation is not matched");
            } else if (!LauncherFeature.supportSetToZeroPage()) {
                ArrayList<String> packageList = new ArrayList();
                for (String packageName : this.mPackages) {
                    if (ZeroPageController.sZeroPageCompName.getPackageName().equals(packageName)) {
                        LauncherModel.this.runOnMainThread(new Runnable() {
                            public void run() {
                                if (callbacks == LauncherModel.this.getCallback()) {
                                    Log.i(LauncherModel.TAG, "checkZeroPageUpdate, call updateZeroPage, op : " + PackageUpdatedTask.this.mOp);
                                    callbacks.updateZeroPage(PackageUpdatedTask.this.mOp);
                                }
                            }
                        });
                    } else {
                        packageList.add(packageName);
                    }
                }
                if (packageList.isEmpty()) {
                    this.mPackages = null;
                } else {
                    this.mPackages = (String[]) packageList.toArray(new String[packageList.size()]);
                }
            }
        }
    }

    private class ShortcutsChangedTask implements Runnable {
        private final String mPackageName;
        private final List<ShortcutInfoCompat> mShortcuts;
        private final UserHandleCompat mUser;

        public ShortcutsChangedTask(String packageName, List<ShortcutInfoCompat> shortcuts, UserHandleCompat user) {
            this.mPackageName = packageName;
            this.mShortcuts = shortcuts;
            this.mUser = user;
        }

        public void run() {
            LauncherModel.this.mHomeLoader.updateDeepShortcutsChanged(this.mPackageName, this.mShortcuts, this.mUser, true);
        }
    }

    private class LoaderTask implements Runnable, DataLoaderState, ModelListener {
        private Context mContext;
        private boolean mLoadAndBindStepFinished;
        private boolean mStopped;

        LoaderTask(Context context) {
            this.mContext = context;
        }

        private DataLoaderCallback loadAndBindWorkspace() {
            DataLoader.setLoadingAndBindingWorkspace(true);
            Log.d(LauncherModel.TAG, "loadAndBindWorkspace mWorkspaceLoaded=" + LauncherModel.this.mWorkspaceLoaded);
            if (LauncherModel.this.mWorkspaceLoaded) {
                LauncherModel.this.mHomeLoader.bindItemsSync(-1, this);
                DataLoader.setLoadingAndBindingWorkspace(false);
                return null;
            }
            LauncherModel.this.mHomeLoader.setup(this);
            LauncherModel.this.mAllAppsLoaded = false;
            LauncherModel.this.mHomeLoader.bindPageItems(LauncherModel.this.mHomeLoader.loadPageItems(-1001, this), null, this);
            LauncherModel.this.mPackageUserToBadgeInfos.clear();
            return new DataLoaderCallback() {
                /* JADX WARNING: inconsistent code. */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void onLoaderComplete() {
                    /*
                    r2 = this;
                    r0 = "Launcher.Model";
                    r1 = "onLoaderComplete";
                    android.util.Log.d(r0, r1);
                    r1 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    monitor-enter(r1);
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;	 Catch:{ all -> 0x006f }
                    r0 = r0.mStopped;	 Catch:{ all -> 0x006f }
                    if (r0 == 0) goto L_0x0014;
                L_0x0012:
                    monitor-exit(r1);	 Catch:{ all -> 0x006f }
                L_0x0013:
                    return;
                L_0x0014:
                    monitor-exit(r1);	 Catch:{ all -> 0x006f }
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r1 = 1;
                    r0.mWorkspaceLoaded = r1;
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mHomeLoader;
                    r0.runLoadCompleteRunnables();
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mAllAppsLoaded;
                    if (r0 != 0) goto L_0x0042;
                L_0x0032:
                    r0 = com.android.launcher3.LauncherFeature.supportHomeModeChange();
                    if (r0 == 0) goto L_0x0013;
                L_0x0038:
                    r0 = com.android.launcher3.LauncherAppState.getInstance();
                    r0 = r0.isHomeOnlyModeEnabled();
                    if (r0 == 0) goto L_0x0013;
                L_0x0042:
                    r0 = com.android.launcher3.LauncherFeature.supportHomeModeChange();
                    if (r0 == 0) goto L_0x005e;
                L_0x0048:
                    r0 = com.android.launcher3.LauncherAppState.getInstance();
                    r0 = r0.isHomeOnlyModeEnabled();
                    if (r0 == 0) goto L_0x005e;
                L_0x0052:
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0.updateIconCache();
                    r0 = com.android.launcher3.gamehome.GameHomeManager.getInstance();
                    r0.updateGameAppsVisibility();
                L_0x005e:
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mDisableableAppCache;
                    r0.makeDisableableAppList();
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0.endLoaderTask();
                    goto L_0x0013;
                L_0x006f:
                    r0 = move-exception;
                    monitor-exit(r1);	 Catch:{ all -> 0x006f }
                    throw r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.LauncherModel.LoaderTask.1.onLoaderComplete():void");
                }
            };
        }

        private void waitForIdle() {
            synchronized (this) {
                long workspaceWaitTime = SystemClock.uptimeMillis();
                LauncherModel.this.mHandler.postIdle(new Runnable() {
                    public void run() {
                        synchronized (LoaderTask.this) {
                            LoaderTask.this.mLoadAndBindStepFinished = true;
                            Log.d(LauncherModel.TAG, "done with previous binding step");
                            Log.i(LauncherModel.TAG, "LauncherAccessTestEnd: " + System.currentTimeMillis());
                            LoaderTask.this.notify();
                        }
                    }
                });
                while (!this.mStopped && !this.mLoadAndBindStepFinished) {
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.d(LauncherModel.TAG, "waited " + (SystemClock.uptimeMillis() - workspaceWaitTime) + "ms for previous step to finish binding");
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage == -1001) {
                throw new RuntimeException("Should not call runBindSynchronousPage() without valid page index");
            } else if (LauncherModel.this.mAllAppsLoaded && LauncherModel.this.mWorkspaceLoaded) {
                synchronized (LauncherModel.this.mLock) {
                    if (LauncherModel.this.mIsLoaderTaskRunning) {
                        throw new RuntimeException("Error! Background loading is already running");
                    }
                }
                LauncherModel.this.mHandler.flush();
                LauncherModel.this.mHomeLoader.bindItemsSync(synchronousBindPage, null);
                LauncherModel.this.loadWidgets(null, null, true);
                LauncherModel.this.mAppsModel.bindItemsSync(synchronousBindPage, null);
            } else {
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r10 = this;
            r9 = 1;
            r8 = 0;
            r7 = 0;
            r4 = com.android.launcher3.LauncherModel.this;
            r5 = r4.mLock;
            monitor-enter(r5);
            r4 = r10.mStopped;	 Catch:{ all -> 0x0078 }
            if (r4 == 0) goto L_0x0010;
        L_0x000e:
            monitor-exit(r5);	 Catch:{ all -> 0x0078 }
        L_0x000f:
            return;
        L_0x0010:
            r4 = com.android.launcher3.LauncherModel.this;	 Catch:{ all -> 0x0078 }
            r6 = 1;
            r4.mIsLoaderTaskRunning = r6;	 Catch:{ all -> 0x0078 }
            r4 = com.android.launcher3.LauncherModel.this;	 Catch:{ all -> 0x0078 }
            r6 = 0;
            r4.mHasLoaderCompletedOnce = r6;	 Catch:{ all -> 0x0078 }
            monitor-exit(r5);	 Catch:{ all -> 0x0078 }
            com.android.launcher3.common.model.DataLoader.loadDefaultLayoutIfNecessary();
            r4 = r10.mContext;
            com.android.launcher3.common.model.DataLoader.setInstallingPackage(r4);
            r4 = com.android.launcher3.LauncherModel.this;
            r5 = r10.mContext;
            r4.deleteDeviceOwnerOMCItem(r5);
            r4 = com.android.launcher3.LauncherModel.this;
            r4 = r4.mApp;
            r4 = r4.getContext();
            r5 = com.android.launcher3.LauncherAppState.getSharedPreferencesKey();
            r2 = r4.getSharedPreferences(r5, r8);
            r4 = "com.sec.android.app.launcher.hideapps.prefs";
            r4 = r2.contains(r4);
            if (r4 == 0) goto L_0x0061;
        L_0x0046:
            r4 = "com.sec.android.app.launcher.hideapps.prefs";
            r0 = r2.getStringSet(r4, r7);
            if (r0 == 0) goto L_0x0055;
        L_0x004e:
            r4 = com.android.launcher3.common.model.FavoritesProvider.getInstance();
            r4.applyHideItem(r0);
        L_0x0055:
            r3 = r2.edit();
            r4 = "com.sec.android.app.launcher.hideapps.prefs";
            r3.remove(r4);
            r3.apply();
        L_0x0061:
            r4 = "Launcher.Model";
            r5 = "step 1: loading workspace";
            android.util.Log.d(r4, r5);
            r1 = r10.loadAndBindWorkspace();
            r4 = r10.mStopped;
            if (r4 == 0) goto L_0x007b;
        L_0x0070:
            r4 = "Launcher.Model";
            r5 = "before waitForIdle : if stop, no need next steps.";
            android.util.Log.d(r4, r5);
            goto L_0x000f;
        L_0x0078:
            r4 = move-exception;
            monitor-exit(r5);	 Catch:{ all -> 0x0078 }
            throw r4;
        L_0x007b:
            r10.waitForIdle();
            r4 = r10.mStopped;
            if (r4 == 0) goto L_0x008a;
        L_0x0082:
            r4 = "Launcher.Model";
            r5 = "after waitForIdle : if stop, no need next steps.";
            android.util.Log.d(r4, r5);
            goto L_0x000f;
        L_0x008a:
            if (r1 == 0) goto L_0x009c;
        L_0x008c:
            r4 = "Launcher.Model";
            r5 = "step 1-1: loading other workspace pages";
            android.util.Log.d(r4, r5);
            r4 = com.android.launcher3.LauncherModel.this;
            r4 = r4.mHomeLoader;
            r4.startPageLoaderTask(r1, r10);
        L_0x009c:
            r4 = r10.mStopped;
            if (r4 == 0) goto L_0x00a9;
        L_0x00a0:
            r4 = "Launcher.Model";
            r5 = "step 1-2 : if stop, no need next steps.";
            android.util.Log.d(r4, r5);
            goto L_0x000f;
        L_0x00a9:
            r4 = com.android.launcher3.LauncherFeature.supportHomeModeChange();
            if (r4 == 0) goto L_0x00d2;
        L_0x00af:
            r4 = com.android.launcher3.LauncherAppState.getInstance();
            r4 = r4.isHomeOnlyModeEnabled();
            if (r4 == 0) goto L_0x00d2;
        L_0x00b9:
            r4 = "Launcher.Model";
            r5 = "step 2: HomeOnlyMode. skip loading all apps";
            android.util.Log.d(r4, r5);
            if (r1 != 0) goto L_0x00c5;
        L_0x00c2:
            r10.endLoaderTask();
        L_0x00c5:
            r4 = r10.mStopped;
            if (r4 == 0) goto L_0x00dd;
        L_0x00c9:
            r4 = "Launcher.Model";
            r5 = "step 2-1 : if stop, no need next steps.";
            android.util.Log.d(r4, r5);
            goto L_0x000f;
        L_0x00d2:
            r4 = "Launcher.Model";
            r5 = "step 2: loading all apps";
            android.util.Log.d(r4, r5);
            r10.loadAndBindAllApps();
            goto L_0x00c5;
        L_0x00dd:
            r4 = "Launcher.Model";
            r5 = "step 3: loading widgets";
            android.util.Log.d(r4, r5);
            r4 = com.android.launcher3.LauncherModel.this;
            r4.loadWidgets(r7, r7, r9);
            r4 = com.android.launcher3.LauncherFeature.supportDeepShortcut();
            if (r4 == 0) goto L_0x000f;
        L_0x00ef:
            r4 = "Launcher.Model";
            r5 = "step 4: loading deep shortcuts";
            android.util.Log.d(r4, r5);
            r10.loadAndBindDeepShortcuts();
            goto L_0x000f;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.LauncherModel.LoaderTask.run():void");
        }

        private void endLoaderTask() {
            Log.d(LauncherModel.TAG, "endLoaderTask");
            this.mContext = null;
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mLoaderTask == this) {
                    LauncherModel.this.mLoaderTask = null;
                }
                LauncherModel.this.mIsLoaderTaskRunning = false;
                LauncherModel.this.mHasLoaderCompletedOnce = true;
                if (!LauncherModel.sPackageChangeRunnables.isEmpty()) {
                    Log.i(LauncherModel.TAG, "endLoaderTask, run sPackageChangeRunnables");
                    Iterator it = LauncherModel.sPackageChangeRunnables.iterator();
                    while (it.hasNext()) {
                        ((Runnable) it.next()).run();
                    }
                    LauncherModel.sPackageChangeRunnables.clear();
                }
            }
            if (LauncherModel.this.mNeedLoadAllAppItemList) {
                LauncherModel.this.mNeedLoadAllAppItemList = false;
                LauncherModel.this.loadAllAppItemList();
            }
            PostPositionController.getInstance(LauncherModel.this.mApp.getContext()).onFinishLoaderTask();
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    Callbacks callbacks = LauncherModel.this.getCallback();
                    if (callbacks != null) {
                        callbacks.refreshNotifications();
                    }
                }
            });
        }

        void stopLocked() {
            synchronized (this) {
                Log.i(LauncherModel.TAG, "LoaderTask stopLocked");
                this.mStopped = true;
                LauncherModel.this.mHomeLoader.setLoaderTaskStop(true);
                LauncherModel.this.mAppsModel.setLoaderTaskStop(true);
                LauncherModel.this.mWidgetLoader.setLoaderTaskStop(true);
                notify();
            }
        }

        private void updateIconCache() {
            HashSet<String> packagesToIgnore = new HashSet();
            LauncherModel.this.mHomeLoader.getIgnorePackage(packagesToIgnore);
            LauncherModel.this.mIconCache.updateDbIcons(packagesToIgnore);
        }

        void dumpState() {
            Log.d(LauncherModel.TAG, "mLoaderTask.mStopped=" + this.mStopped);
            Log.d(LauncherModel.TAG, "mLoaderTask.mLoadAndBindStepFinished=" + this.mLoadAndBindStepFinished);
            LauncherModel.this.mHomeLoader.dumpState();
        }

        public boolean isStopped() {
            return this.mStopped;
        }

        private void loadAndBindDeepShortcuts() {
            Log.d(LauncherModel.TAG, "loadAndBindDeepShortcuts mDeepShortcutsLoaded=" + LauncherModel.this.mDeepShortcutsLoaded);
            DataLoader loader = LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? LauncherModel.this.mHomeLoader : LauncherModel.this.mAppsModel;
            if (!LauncherModel.this.mDeepShortcutsLoaded) {
                if (LauncherModel.this.mDeepShortcutManager.hasHostPermission()) {
                    for (UserHandleCompat user : UserManagerCompat.getInstance(this.mContext).getUserProfiles()) {
                        loader.updateDeepShortcutMap(null, user, LauncherModel.this.mDeepShortcutManager.queryForAllShortcuts(user));
                    }
                }
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    LauncherModel.this.mDeepShortcutsLoaded = true;
                }
            }
            loader.bindDeepShortcuts();
        }

        private void loadAndBindAllApps() {
            Log.d(LauncherModel.TAG, "loadAndBindAllApps mAllAppsLoaded=" + LauncherModel.this.mAllAppsLoaded);
            if (!LauncherModel.this.mAllAppsLoaded) {
                Log.i(LauncherModel.TAG, "loadAndBindAllApps mStopped=" + this.mStopped);
                if (!this.mStopped) {
                    Log.e(LauncherModel.TAG, "loadAndBindAllApps setup");
                    LauncherModel.this.mAppsModel.setup(this);
                    LauncherModel.this.mAppsModel.addModelListener(this);
                    Log.e(LauncherModel.TAG, "loadAndBindAllApps notifyDirty");
                    LauncherModel.this.mAppsModel.notifyDirty();
                }
            } else if (!this.mStopped) {
                LauncherModel.this.mAppsModel.bindItemsSync(-1, this);
            }
        }

        public void notifyUpdate(ArrayList<ItemInfo> arrayList) {
        }

        public void onLoadComplete(final int taskState) {
            LauncherModel.runOnWorkerThread(new Runnable() {
                /* JADX WARNING: inconsistent code. */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void run() {
                    /*
                    r3 = this;
                    r2 = 1;
                    r1 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    monitor-enter(r1);
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;	 Catch:{ all -> 0x0049 }
                    r0 = r0.mStopped;	 Catch:{ all -> 0x0049 }
                    if (r0 != 0) goto L_0x0010;
                L_0x000c:
                    r0 = r2;	 Catch:{ all -> 0x0049 }
                    if (r0 == r2) goto L_0x0012;
                L_0x0010:
                    monitor-exit(r1);	 Catch:{ all -> 0x0049 }
                L_0x0011:
                    return;
                L_0x0012:
                    monitor-exit(r1);	 Catch:{ all -> 0x0049 }
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mWorkspaceLoaded;
                    if (r0 == 0) goto L_0x002d;
                L_0x001d:
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0.updateIconCache();
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mDisableableAppCache;
                    r0.makeDisableableAppList();
                L_0x002d:
                    r1 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    monitor-enter(r1);
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;	 Catch:{ all -> 0x004c }
                    r0 = com.android.launcher3.LauncherModel.this;	 Catch:{ all -> 0x004c }
                    r2 = 1;
                    r0.mAllAppsLoaded = r2;	 Catch:{ all -> 0x004c }
                    monitor-exit(r1);	 Catch:{ all -> 0x004c }
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0 = com.android.launcher3.LauncherModel.this;
                    r0 = r0.mWorkspaceLoaded;
                    if (r0 == 0) goto L_0x0011;
                L_0x0043:
                    r0 = com.android.launcher3.LauncherModel.LoaderTask.this;
                    r0.endLoaderTask();
                    goto L_0x0011;
                L_0x0049:
                    r0 = move-exception;
                    monitor-exit(r1);	 Catch:{ all -> 0x0049 }
                    throw r0;
                L_0x004c:
                    r0 = move-exception;
                    monitor-exit(r1);	 Catch:{ all -> 0x004c }
                    throw r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.LauncherModel.LoaderTask.4.run():void");
                }
            });
        }

        public void removeAllItems() {
        }

        public void onLoadStart() {
        }

        public void updateIconAndTitle(ArrayList<ItemInfo> arrayList) {
        }

        public void addItemToFolder(FolderInfo folder, ArrayList<IconInfo> arrayList) {
        }

        public void createFolderAndAddItem(FolderInfo folderInfo, ItemInfo targetItem, ArrayList<IconInfo> arrayList) {
        }

        public void updateRestoreItems(HashSet<ItemInfo> hashSet) {
        }

        public void terminate() {
        }
    }

    static {
        sWorkerThread.start();
    }

    LauncherModel(LauncherAppState app, IconCache iconCache, BadgeCache badgeCache, DisableableAppCache disableableAppCache, DeepShortcutManager deepShortcutManager) {
        Context context = app.getContext();
        this.mAppsCanBeOnRemovableStorage = Environment.isExternalStorageRemovable();
        this.mApp = app;
        this.mHomeLoader = new HomeLoader(context, app, this, iconCache, badgeCache);
        this.mAppsModel = new AppsModel(context, app, this, iconCache, badgeCache);
        this.mWidgetLoader = new WidgetLoader(app);
        this.mIconCache = iconCache;
        this.mBadgeCache = badgeCache;
        this.mDisableableAppCache = disableableAppCache;
        this.mDeepShortcutManager = deepShortcutManager;
    }

    private void runOnMainThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            this.mHandler.post(r);
        } else {
            r.run();
        }
    }

    public static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sWorker.post(r);
        }
    }

    private void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() != Process.myTid() || TestHelper.isRoboUnitTest()) {
            Log.d(TAG, "unbindItemInfosAndClearQueuedBindRunnables: ");
            this.mHomeLoader.clearDeferredBindRunnable();
            this.mAppsModel.clearDeferredBindRunnable();
            this.mHandler.cancelAll();
            this.mHomeLoader.unbindItemsOnMainThread();
            this.mAppsModel.unbindItemsOnMainThread();
            return;
        }
        throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the main thread");
    }

    public void initialize(Callbacks callbacks) {
        synchronized (this.mLock) {
            unbindItemInfosAndClearQueuedBindRunnables();
            this.mCallbacks = new WeakReference(callbacks);
            setNetworkLocked();
            this.mHandler.setCallbacks(this, this.mCallbacks);
        }
    }

    public void onPackageChanged(String packageName, UserHandleCompat user) {
        Log.d(TAG, "onPackageChanged:" + packageName + " user:" + user);
        if (isValidStateInKnoxMode(user)) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(2, new String[]{packageName}, user));
        }
    }

    public void onPackageRemoved(String packageName, UserHandleCompat user) {
        Log.d(TAG, "onPackageRemoved:" + packageName + " user:" + user);
        if (isValidStateInKnoxMode(user)) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(3, new String[]{packageName}, user));
        }
    }

    public void onPackageAdded(String packageName, UserHandleCompat user) {
        Log.d(TAG, "onPackageAdded:" + packageName + " user:" + user);
        if (isValidStateInKnoxMode(user)) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(1, new String[]{packageName}, user));
        }
    }

    public void onPackagesAvailable(String[] packageNames, UserHandleCompat user, boolean replacing) {
        if (!isValidStateInKnoxMode(user)) {
            return;
        }
        if (replacing) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(2, packageNames, user));
            return;
        }
        enqueueItemUpdatedTask(new PackageUpdatedTask(1, packageNames, user));
        if (this.mAppsCanBeOnRemovableStorage) {
            startLoaderFromBackground();
        }
    }

    public void onPackagesUnavailable(String[] packageNames, UserHandleCompat user, boolean replacing) {
        if (isValidStateInKnoxMode(user) && !replacing) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(4, packageNames, user));
        }
    }

    public void onPackagesSuspended(String[] packageNames, UserHandleCompat user) {
        if (isValidStateInKnoxMode(user)) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(5, packageNames, user));
        }
    }

    public void onPackagesUnsuspended(String[] packageNames, UserHandleCompat user) {
        if (isValidStateInKnoxMode(user)) {
            enqueueItemUpdatedTask(new PackageUpdatedTask(6, packageNames, user));
        }
    }

    public void onShortcutsChanged(String packageName, List<ShortcutInfoCompat> shortcuts, UserHandleCompat user) {
        enqueueItemUpdatedTask(new ShortcutsChangedTask(packageName, shortcuts, user));
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            doLocaleChange();
        } else if (ICON_BACKGROUNDS_CHANGED.equals(action)) {
            ShortcutTray.checkIconTrayEnabled(context);
            forceIconReload();
        } else if (LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED.equals(action) || LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED.equals(action) || ACTION_MANAGED_PROFILE_REFRESH.equals(action)) {
            UserManagerCompat.getInstance(context).enableAndResetCache();
            this.mHomeLoader.updateUsersList();
            this.mAppsModel.updateUsersList();
            Bundle bundle = intent.getExtras();
            int userId = bundle.getInt("com.samsung.sec.knox.EXTRA_PERSONA_ID");
            Log.d(TAG, "userId:" + userId);
            if (ACTION_MANAGED_PROFILE_REFRESH.equals(action)) {
                boolean blockCreateShortcut = bundle.getBoolean("com.samsung.android.knox.container.block_create_shortcut", false);
                String folderName = bundle.getString("com.samsung.android.knox.container.name", "");
                int userNumber = bundle.getInt("com.samsung.android.knox.container.userid");
                Log.d(TAG, "ACTION_MANAGED_PROFILE_REFRESH noCreateShortcut : " + blockCreateShortcut + " folder name : " + folderName + " userNumber : " + userNumber);
                String blockCreateShortcutKey = Utilities.BLOCK_CREATE_SHORTCUT_PREFIX + userNumber;
                String folderNameKey = Utilities.USER_FOLDER_NAME_PREFIX + userNumber;
                Editor prefsEditor = context.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0).edit();
                prefsEditor.putBoolean(blockCreateShortcutKey, blockCreateShortcut);
                if (folderName.length() > 0) {
                    prefsEditor.putString(folderNameKey, folderName);
                }
                prefsEditor.apply();
            }
            if (!SemPersonaManager.isKnoxId(userId)) {
                forceReload();
                setHasLoaderCompletedOnce(false);
            }
        } else if (LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action) || LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)) {
            UserHandleCompat user = UserHandleCompat.fromIntent(intent);
            if (user != null) {
                enqueueItemUpdatedTask(new PackageUpdatedTask(7, new String[0], user));
            }
        } else if (ACTION_STK_TITLE_IS_LOADED.equals(action)) {
            Log.d(TAG, "receive ACTION_STK_TITLE_IS_LOADED");
            boolean isFixedStkMenu = SemCscFeature.getInstance().getBoolean("CscFeature_RIL_FixedStkMenu");
            String fixedStkTitle = SemCscFeature.getInstance().getString("CscFeature_Launcher_FixedStkTitleAs");
            String stkTitleFromSIM = SemSystemProperties.get("gsm.STK_SETUP_MENU", null);
            Log.d(TAG, "stkTitleFromSIM : " + stkTitleFromSIM + " fixedStkTitle : " + fixedStkTitle + " isFixedStkMenu : " + isFixedStkMenu);
            if ((fixedStkTitle == null || fixedStkTitle.length() <= 0 || fixedStkTitle.startsWith("NoSIM%")) && stkTitleFromSIM != null && stkTitleFromSIM.length() > 0) {
                String packageName = intent.getData().getSchemeSpecificPart();
                if (packageName == null || packageName.length() == 0) {
                    Log.d(TAG, "ACTION_STK_TITLE_IS_LOADED packageName is null");
                } else if (isFixedStkMenu) {
                    enqueueItemUpdatedTask(new PackageUpdatedTask(2, new String[]{packageName, "com.sec.android.app.latin.launcher.stk"}, UserHandleCompat.myUserHandle()));
                } else {
                    enqueueItemUpdatedTask(new PackageUpdatedTask(2, new String[]{packageName}, UserHandleCompat.myUserHandle()));
                }
            }
        } else if (ACTION_SPR_FORCE_REFRESH.equals(action)) {
            LauncherProviderID providerID = LauncherAppState.getLauncherProviderID();
            if (providerID != null) {
                Log.d(TAG, "[SPRINT] FLR Intent received. Refreshing Launcher...");
                Utilities.setHomeDefaultPageKey(context, providerID.getScreenIndex(), LauncherFiles.HOME_DEFAULT_PAGE_KEY);
                forceReload();
            }
        } else if (ACTION_EDM_UNINSTALL_STATUS_INTERNAL.equals(intent.getAction())) {
            if (this.mDisableableAppCache != null) {
                this.mDisableableAppCache.getEnterprisePolicyBlockUninstallList();
                this.mDisableableAppCache.getEnterprisePolicyBlockUninstallWhitelist();
            }
        } else if ("android.intent.action.CONFIGURATION_CHANGED".equals(action) && isNetworkCodeChanged(this.mApp.getContext().getResources().getConfiguration())) {
            setNetworkLocked();
            forceIconReload();
        }
    }

    private void setNetworkLocked() {
        Configuration config = this.mApp.getContext().getResources().getConfiguration();
        this.mConfigMccWhenLoaded = config.mcc;
        this.mConfigMncWhenLoaded = config.mnc;
    }

    private boolean isNetworkCodeChanged(Configuration config) {
        return (this.mConfigMccWhenLoaded == config.mcc && this.mConfigMncWhenLoaded == config.mnc) ? false : true;
    }

    private void forceIconReload() {
        Log.d(TAG, "forceIconReload");
        this.mIconCache.clearDB();
        runOnWorkerThread(new Runnable() {
            public void run() {
                HashSet<String> packagesToIgnore = new HashSet();
                LauncherModel.this.mHomeLoader.getIgnorePackage(packagesToIgnore);
                LauncherModel.this.mIconCache.updateDbIcons(packagesToIgnore);
                LauncherModel.this.mHomeLoader.updateShortcutIcons();
            }
        });
    }

    private void doLocaleChange() {
        runOnWorkerThread(new Runnable() {
            public void run() {
                LauncherModel.this.mHomeLoader.titleUpdate();
                LauncherModel.this.mAppsModel.titleUpdate();
                LauncherModel.this.loadWidgets(null, null, true);
            }
        });
    }

    private void forceReload() {
        resetLoadedState(true, true);
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (this.mLock) {
            stopLoaderLocked();
            if (resetAllAppsLoaded) {
                this.mAllAppsLoaded = false;
            }
            if (resetWorkspaceLoaded) {
                this.mWorkspaceLoaded = false;
            }
        }
    }

    public void startLoaderFromBackground() {
        boolean runLoader = false;
        Callbacks callbacks = getCallback();
        if (!(callbacks == null || callbacks.setLoadOnResume())) {
            runLoader = true;
        }
        if (runLoader) {
            startLoader(-1001);
        }
    }

    private void stopLoaderLocked() {
        Log.d(TAG, "stopLoaderLocked");
        LoaderTask oldTask = this.mLoaderTask;
        if (oldTask != null) {
            Log.d(TAG, "oldTask is not null. call stop");
            oldTask.stopLocked();
        }
        this.mAppsModel.updateLock(true);
    }

    public boolean isCurrentCallbacks(Callbacks callbacks) {
        return this.mCallbacks != null && this.mCallbacks.get() == callbacks;
    }

    public void startLoader(int synchronousBindPage) {
        DataLoader.setDeviceProfile(this.mApp.getDeviceProfile());
        Log.d(TAG, "startLoader");
        LauncherAppState.getInstance().enableExternalQueue(true);
        SALogging.getInstance().startLoader();
        synchronized (this.mLock) {
            this.mHomeLoader.clearDeferredBindRunnable();
            this.mAppsModel.clearDeferredBindRunnable();
            if (!(this.mCallbacks == null || this.mCallbacks.get() == null)) {
                stopLoaderLocked();
                this.mLoaderTask = new LoaderTask(this.mApp.getContext());
                if (synchronousBindPage == -1001 || !this.mAllAppsLoaded || !this.mWorkspaceLoaded || this.mIsLoaderTaskRunning) {
                    sWorkerThread.setPriority(5);
                    sWorker.post(this.mLoaderTask);
                } else {
                    this.mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                }
            }
        }
    }

    public void stopLoader() {
        Log.d(TAG, "stopLoader");
        synchronized (this.mLock) {
            if (this.mLoaderTask != null) {
                this.mLoaderTask.stopLocked();
            }
            this.mAppsModel.updateLock(true);
        }
    }

    public void onPackageIconsUpdated(HashSet<String> updatedPackages, UserHandleCompat user) {
        this.mHomeLoader.updateShortcut(updatedPackages, user);
        this.mAppsModel.updateIconsAndLabels(updatedPackages, user);
    }

    public void enqueueItemUpdatedTask(Runnable task) {
        sWorker.post(task);
    }

    private void loadWidgetsAndShortcuts(String[] packages, UserHandleCompat user, final boolean refresh) {
        loadWidgets(packages, user, refresh);
        runOnWorkerThread(new Runnable() {
            public void run() {
                LauncherModel.this.updateWidgetsProviders(refresh);
            }
        });
    }

    private void updateWidgetsProviders(boolean refresh) {
        HomeLoader.getWidgetProviders(this.mApp.getContext(), refresh);
    }

    public void updateAppsButton(Context context, boolean enabled, IconInfo appsButton) {
        if (this.mApp.getAppsButtonEnabled() == enabled) {
            Log.d(TAG, "appsButton already set : " + enabled);
            return;
        }
        this.mApp.setAppsButtonEnabled(enabled);
        if (enabled) {
            this.mHomeLoader.enableAppsButton(context, appsButton);
        } else {
            this.mHomeLoader.disableAppsButton();
        }
    }

    public void updateDeepShortcutIcons() {
        this.mHomeLoader.updateDeepShortcutIcons();
    }

    public void updateItemInfo(final ArrayList<ItemInfo> hideItems, final ArrayList<ItemInfo> addItems, final boolean isGameApp) {
        runOnWorkerThread(new Runnable() {
            public void run() {
                if (!hideItems.isEmpty()) {
                    LauncherModel.this.mHomeLoader.hideApps(hideItems, isGameApp);
                    if (!(LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled())) {
                        LauncherModel.this.mAppsModel.hideApps(hideItems, isGameApp);
                    }
                }
                if (!addItems.isEmpty()) {
                    if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                        LauncherModel.this.mHomeLoader.showApps(addItems, isGameApp);
                    } else {
                        LauncherModel.this.mAppsModel.showApps(addItems, isGameApp);
                    }
                }
            }
        });
    }

    public void updateAppsOnlyDB(final ArrayList<ItemInfo> updateItems) {
        runOnWorkerThread(new Runnable() {
            public void run() {
                if (!updateItems.isEmpty() && LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    LauncherModel.this.mHomeLoader.updateItemsOnlyDB(updateItems);
                }
            }
        });
    }

    private void loadAllAppItemList() {
        loadAllAppItemList(null);
    }

    public void loadAllAppItemList(OnAllAppItemListLoadCompletedListener listener) {
        if (this.mOnAllAppItemListLoadCompletedListener == null) {
            this.mOnAllAppItemListLoadCompletedListener = listener;
            Log.d(TAG, "mOnAllAppItemListLoadCompletedListener = " + this.mOnAllAppItemListLoadCompletedListener);
        }
        if (this.mOnAllAppItemListLoadCompletedListener != null && this.mOnAllAppItemListLoadCompletedListener.isTopStage()) {
            if (this.mIsLoaderTaskRunning) {
                this.mNeedLoadAllAppItemList = true;
            } else if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                OnAllAppItemListLoadCompleted(this.mHomeLoader.getAllAppItemInHome());
            } else {
                OnAllAppItemListLoadCompleted(this.mAppsModel.getAllAppItemInApps());
            }
        }
    }

    private static boolean isPackageDisabled(Context context, String packageName, UserHandleCompat user) {
        return !LauncherAppsCompat.getInstance(context).isPackageEnabledForProfile(packageName, user);
    }

    public static boolean isValidPackage(Context context, String packageName, UserHandleCompat user) {
        if (packageName == null) {
            return false;
        }
        return LauncherAppsCompat.getInstance(context).isPackageEnabledForProfile(packageName, user);
    }

    public static Intent getMarketIntent(String packageName) {
        return new Intent("android.intent.action.VIEW").setData(new Builder().scheme("market").authority("details").appendQueryParameter("id", packageName).build());
    }

    public static boolean isValidProvider(AppWidgetProviderInfo provider) {
        return (provider == null || provider.provider == null || provider.provider.getPackageName() == null) ? false : true;
    }

    public void reloadBadges() {
        Log.d(NOTI_TAG, "reloadBadges() entered.");
        runOnWorkerThread(new Runnable() {
            public void run() {
                Map<CacheKey, Integer> badges = LauncherModel.this.mBadgeCache.updateBadgeCounts();
                if (badges.isEmpty()) {
                    Log.d(LauncherModel.NOTI_TAG, "reloadBadges(), badges is empty");
                    return;
                }
                Log.d(LauncherModel.NOTI_TAG, "reloadBadges(), badges count : " + badges.size());
                final ArrayList<ItemInfo> badgeItems = DataLoader.updateBadgeCounts(badges);
                if (!badgeItems.isEmpty()) {
                    LauncherModel.this.mHandler.post(new Runnable() {
                        public void run() {
                            LauncherModel.this.onBadgeBindingCompleted(badgeItems);
                        }
                    });
                }
            }
        });
    }

    public void dumpState() {
        Log.d(TAG, "mCallbacks=" + this.mCallbacks);
        IconInfo.dumpIconInfoList(TAG, "mAllAppsList.added", this.mAppsModel.added);
        IconInfo.dumpIconInfoList(TAG, "mAllAppsList.removed", this.mAppsModel.removed);
        IconInfo.dumpIconInfoList(TAG, "mAllAppsList.modified", this.mAppsModel.modified);
        if (this.mLoaderTask != null) {
            this.mLoaderTask.dumpState();
        } else {
            Log.d(TAG, "mLoaderTask=null");
        }
    }

    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (this.mHomeLoader == null) {
            Log.d(TAG, "mHomeLoader is null");
        } else {
            this.mHomeLoader.dumpState(prefix, fd, writer, args, LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? this.mHomeLoader.getAllAppItemInHome() : this.mAppsModel.getAllAppItemInApps());
        }
    }

    public Callbacks getCallback() {
        return this.mCallbacks != null ? (Callbacks) this.mCallbacks.get() : null;
    }

    public static Looper getWorkerLooper() {
        return sWorkerThread.getLooper();
    }

    public DeferredHandler getHandler() {
        return this.mHandler;
    }

    public HomeLoader getHomeLoader() {
        return this.mHomeLoader;
    }

    public AppsModel getAppsModel() {
        return this.mAppsModel;
    }

    private void loadWidgets(String[] packages, UserHandleCompat user, boolean refresh) {
        this.mWidgetLoader.notifyDirty(packages, user, refresh);
    }

    public WidgetLoader getWidgetsLoader() {
        return this.mWidgetLoader;
    }

    public void registerOnAllAppItemListLoadCompletedListener(OnAllAppItemListLoadCompletedListener listener) {
        Log.d(TAG, "registerOnAllAppItemListLoadCompletedListener");
        this.mOnAllAppItemListLoadCompletedListener = listener;
    }

    public void registerOnLauncherBindingItemsCompletedListener(OnLauncherBindingItemsCompletedListener listener) {
        this.mOnLauncherBindingItemsCompletedListeners.add(listener);
    }

    public void registerOnBadgeBindingCompletedListener(OnBadgeBindingCompletedListener listener) {
        this.mOnBadgeBindingCompletedListeners.add(listener);
    }

    public void registerOnNotificationPreviewListener(OnNotificationPreviewBindingListener listener) {
        this.mOnNotificationPreviewBindingListener.add(listener);
    }

    public void registerOnLiveIconUpdateListener(OnRefreshLiveIconListener listener) {
        this.mOnRefreshLiveIconListener.add(listener);
    }

    public void unregisterOnAllAppItemListLoadCompletedListener(Launcher launcher) {
        if (isCurrentCallbacks(launcher)) {
            Log.d(TAG, "unregisterOnAllAppItemListLoadCompletedListener");
            this.mOnAllAppItemListLoadCompletedListener = null;
        }
    }

    public void unregisterOnLauncherBindingItemsCompletedListener(OnLauncherBindingItemsCompletedListener listener) {
        this.mOnLauncherBindingItemsCompletedListeners.remove(listener);
    }

    public void unregisterOnBadgeBindingCompletedListener(OnBadgeBindingCompletedListener listener) {
        this.mOnBadgeBindingCompletedListeners.remove(listener);
    }

    public void unregisterOnNotificationPreviewListener(OnNotificationPreviewBindingListener listener) {
        this.mOnNotificationPreviewBindingListener.remove(listener);
    }

    public void unregisterOnLiveIconUpdateListener(OnRefreshLiveIconListener listener) {
        this.mOnRefreshLiveIconListener.remove(listener);
    }

    private void OnAllAppItemListLoadCompleted(ArrayList<ItemInfo> appItems) {
        if (this.mOnAllAppItemListLoadCompletedListener != null) {
            final ArrayList<IconInfo> allAppItems = new ArrayList();
            Iterator it = appItems.iterator();
            while (it.hasNext()) {
                ItemInfo item = (ItemInfo) it.next();
                if (item instanceof IconInfo) {
                    IconInfo info = (IconInfo) item;
                    if (info.mIcon == null) {
                        info.mIcon = info.getIcon(LauncherAppState.getInstance().getIconCache());
                    }
                    if (!(info.isHiddenByXML() || info.isHiddenByGame())) {
                        allAppItems.add(info);
                    }
                }
            }
            runOnMainThread(new Runnable() {
                public void run() {
                    LauncherModel.this.mOnAllAppItemListLoadCompletedListener.onAllAppItemListLoadCompleted(allAppItems);
                }
            });
        }
    }

    public void onLauncherBindingItemsCompleted() {
        Iterator it = this.mOnLauncherBindingItemsCompletedListeners.iterator();
        while (it.hasNext()) {
            ((OnLauncherBindingItemsCompletedListener) it.next()).onLauncherBindingItemsCompleted();
        }
    }

    private void onBadgeBindingCompleted(ArrayList<ItemInfo> badgeItems) {
        Iterator it = this.mOnBadgeBindingCompletedListeners.iterator();
        while (it.hasNext()) {
            ((OnBadgeBindingCompletedListener) it.next()).onBadgeBindingCompleted(badgeItems);
        }
    }

    private void onNotificationPreviewBinding(Map<PackageUserKey, BadgeInfo> updatedBadges) {
        Iterator it = this.mOnNotificationPreviewBindingListener.iterator();
        while (it.hasNext()) {
            ((OnNotificationPreviewBindingListener) it.next()).onNotificationPreviewBinding(updatedBadges);
        }
    }

    public void onRefreshLiveIcon() {
        Iterator it = this.mOnRefreshLiveIconListener.iterator();
        while (it.hasNext()) {
            ((OnRefreshLiveIconListener) it.next()).onRefreshLiveIcon();
        }
    }

    private void deleteDeviceOwnerOMCItem(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm != null && dpm.semGetDeviceOwner() != null) {
            String[] selectionArg = new String[]{String.valueOf(32)};
            Log.d(TAG, "deleteDeviceOwnerOMCItem - " + context.getContentResolver().delete(Favorites.CONTENT_URI, "restored=?", selectionArg) + " items removed.");
        }
    }

    public void handleSCloudRestoreComplete(final Context context) {
        Log.d(TAG, "handleSCloudRestoreComplete");
        runOnWorkerThread(new Runnable() {
            public void run() {
                SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                if (prefs.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                    Log.d(LauncherModel.TAG, "launcher DB is empty. so skip~");
                    return;
                }
                Uri tableUri;
                ContentResolver cr = context.getApplicationContext().getContentResolver();
                String selection = "restored=?";
                String[] selectionArg = new String[]{String.valueOf(4)};
                ContentValues values = new ContentValues();
                values.put("restored", Integer.valueOf(0));
                if (LauncherFeature.supportEasyModeChange()) {
                    tableUri = Favorites_Easy.CONTENT_URI;
                    if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                        tableUri = Favorites_Standard.CONTENT_URI;
                    }
                    Log.d(LauncherModel.TAG, "update restored value to " + tableUri);
                    cr.update(tableUri, values, selection, selectionArg);
                }
                if (LauncherFeature.supportHomeModeChange()) {
                    tableUri = Favorites_HomeOnly.CONTENT_URI;
                    if (prefs.getBoolean(LauncherAppState.HOME_ONLY_MODE, false)) {
                        tableUri = Favorites_HomeApps.CONTENT_URI;
                    }
                    Log.d(LauncherModel.TAG, "update restored value to " + tableUri);
                    cr.update(tableUri, values, selection, selectionArg);
                }
                if (!(LauncherModel.this.mWorkspaceLoaded && LauncherModel.this.mAllAppsLoaded)) {
                    Log.d(LauncherModel.TAG, "workspace(" + LauncherModel.this.mWorkspaceLoaded + ") or allapps(" + LauncherModel.this.mAllAppsLoaded + ") not loaded. update restored value");
                    cr.update(Favorites.CONTENT_URI, values, selection, selectionArg);
                }
                LauncherModel.this.mHomeLoader.removeUnRestoredItems(LauncherModel.this.mWorkspaceLoaded);
                LauncherModel.this.mAppsModel.removeUnRestoredItems(LauncherModel.this.mAllAppsLoaded);
                Log.d(LauncherModel.TAG, "delete S Cloud restore directory");
                String dirPath = context.getFilesDir() + SCloudBnr.SCLOUD_DIR_PATH;
                LauncherBnrHelper.deleteDir(dirPath + SCloudBnr.SCLOUD_RESTORE_PATH);
                LauncherBnrHelper.deleteDir(dirPath);
            }
        });
    }

    private boolean isValidStateInKnoxMode(UserHandleCompat user) {
        Log.d(TAG, "isValidStateInKnoxMode:" + Utilities.isKnoxMode() + " user:" + user);
        if (!Utilities.isKnoxMode() || UserHandleCompat.myUserHandle().equals(user)) {
            return true;
        }
        return false;
    }

    public LoaderTask getLoaderTask() {
        return this.mLoaderTask;
    }

    public DisableableAppCache getDisableableAppCache() {
        return this.mDisableableAppCache;
    }

    public boolean isSupportVirtualScreen() {
        return ZeroPageController.supportVirtualScreen();
    }

    public void flushPendingQueue() {
        this.mHandler.startPendingQueueFlush(true);
        runOnWorkerThread(new Runnable() {
            public void run() {
                LauncherModel.this.mHandler.flushPendingQueue();
                LauncherModel.this.mHandler.startPendingQueueFlush(false);
            }
        });
    }

    public boolean isModelIdle() {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            if (!this.mWorkspaceLoaded || this.mIsLoaderTaskRunning) {
                return false;
            }
            return true;
        } else if (this.mAllAppsLoaded && this.mWorkspaceLoaded && !this.mIsLoaderTaskRunning) {
            return true;
        } else {
            return false;
        }
    }

    public void onNotificationPosted(PackageUserKey postedPackageUserKey, NotificationKeyData notificationKey, boolean shouldBeFilteredOut, Notification notification) {
        final PackageUserKey packageUserKey = postedPackageUserKey;
        final NotificationKeyData notificationKeyData = notificationKey;
        final boolean z = shouldBeFilteredOut;
        final Notification notification2 = notification;
        runOnWorkerThread(new Runnable() {
            public void run() {
                boolean badgeShouldBeRefreshed;
                Log.d(LauncherModel.NOTI_TAG, "LauncherModel:onNotificationPosted() : " + packageUserKey.mPackageName + ", count : [" + notificationKeyData.count + "], shouldBeFilteredOut : [" + z + "]");
                BadgeInfo badgeInfo = (BadgeInfo) LauncherModel.this.mPackageUserToBadgeInfos.get(packageUserKey);
                LauncherModel.this.updateTargetActivity(notification2, packageUserKey);
                if (badgeInfo != null) {
                    if (z) {
                        badgeShouldBeRefreshed = badgeInfo.removeNotificationKey(notificationKeyData);
                    } else {
                        badgeShouldBeRefreshed = badgeInfo.addOrUpdateNotificationKey(notificationKeyData);
                    }
                    if (badgeInfo.getNotificationKeys().size() == 0) {
                        LauncherModel.this.mPackageUserToBadgeInfos.remove(packageUserKey);
                    }
                } else if (z) {
                    badgeShouldBeRefreshed = false;
                } else {
                    BadgeInfo newBadgeInfo = new BadgeInfo(packageUserKey);
                    newBadgeInfo.addOrUpdateNotificationKey(notificationKeyData);
                    LauncherModel.this.mPackageUserToBadgeInfos.put(packageUserKey, newBadgeInfo);
                    badgeShouldBeRefreshed = true;
                }
                LauncherModel.this.updateLauncherIconBadges(Utilities.singletonHashSet(packageUserKey), badgeShouldBeRefreshed);
            }
        });
    }

    public void onNotificationRemoved(final PackageUserKey removedPackageUserKey, final NotificationKeyData notificationKey, final Notification notification) {
        runOnWorkerThread(new Runnable() {
            public void run() {
                Log.d(LauncherModel.NOTI_TAG, "LauncherModel:onNotificationRemoved() : " + removedPackageUserKey.mPackageName);
                BadgeInfo oldBadgeInfo = (BadgeInfo) LauncherModel.this.mPackageUserToBadgeInfos.get(removedPackageUserKey);
                if (oldBadgeInfo != null && oldBadgeInfo.removeNotificationKey(notificationKey)) {
                    LauncherModel.this.updateTargetActivity(notification, removedPackageUserKey);
                    if (oldBadgeInfo.getNotificationKeys().size() == 0) {
                        LauncherModel.this.mPackageUserToBadgeInfos.remove(removedPackageUserKey);
                    }
                    LauncherModel.this.updateLauncherIconBadges(Utilities.singletonHashSet(removedPackageUserKey));
                    LauncherModel.this.mHandler.post(new Runnable() {
                        public void run() {
                            LauncherModel.this.onNotificationPreviewBinding(LauncherModel.this.mPackageUserToBadgeInfos);
                        }
                    });
                }
            }
        });
    }

    public void onNotificationFullRefresh(final List<StatusBarNotification> activeNotifications) {
        if (activeNotifications != null) {
            runOnWorkerThread(new Runnable() {
                public void run() {
                    Log.d(LauncherModel.NOTI_TAG, "LauncherModel:onNotificationFullRefresh() : " + activeNotifications.size());
                    int badgeSettingValue = Utilities.getBadgeSettingValue(LauncherModel.this.mApp.getContext());
                    final HashMap<PackageUserKey, BadgeInfo> updatedBadges = new HashMap(LauncherModel.this.mPackageUserToBadgeInfos);
                    LauncherModel.this.mPackageUserToBadgeInfos.clear();
                    NotificationListener notificationListener = NotificationListener.getInstanceIfConnected();
                    if (notificationListener == null) {
                        Log.d(LauncherModel.NOTI_TAG, "LauncherModel:notificationListener is null");
                        return;
                    }
                    for (StatusBarNotification notification : activeNotifications) {
                        boolean shouldBeFilteredOut = notificationListener.shouldBeFilteredOut(notification);
                        PackageUserKey packageUserKey = PackageUserKey.fromNotification(notification);
                        LauncherModel.this.updateTargetActivity(notification.getNotification(), packageUserKey);
                        BadgeInfo badgeInfo = (BadgeInfo) LauncherModel.this.mPackageUserToBadgeInfos.get(packageUserKey);
                        if (!shouldBeFilteredOut) {
                            if (badgeInfo == null) {
                                badgeInfo = new BadgeInfo(packageUserKey);
                                LauncherModel.this.mPackageUserToBadgeInfos.put(packageUserKey, badgeInfo);
                            }
                            badgeInfo.addOrUpdateNotificationKey(NotificationKeyData.fromNotification(notification));
                        }
                    }
                    for (PackageUserKey packageUserKey2 : LauncherModel.this.mPackageUserToBadgeInfos.keySet()) {
                        BadgeInfo prevBadge = (BadgeInfo) updatedBadges.get(packageUserKey2);
                        BadgeInfo newBadge = (BadgeInfo) LauncherModel.this.mPackageUserToBadgeInfos.get(packageUserKey2);
                        if (prevBadge == null) {
                            updatedBadges.put(packageUserKey2, newBadge);
                        } else if (!prevBadge.shouldBeInvalidated(newBadge) && LauncherModel.mPreBadgeSettingValue == badgeSettingValue) {
                            Log.d(LauncherModel.NOTI_TAG, "LauncherModel:onNotificationFullRefresh() not need to update, updatedBadges.remove[" + packageUserKey2.mPackageName + "], count : [" + prevBadge.getNotificationCount() + "]");
                            updatedBadges.remove(packageUserKey2);
                        }
                    }
                    if (!updatedBadges.isEmpty()) {
                        LauncherModel.this.updateLauncherIconBadges(updatedBadges.keySet());
                        LauncherModel.this.mHandler.post(new Runnable() {
                            public void run() {
                                LauncherModel.this.onNotificationPreviewBinding(updatedBadges);
                            }
                        });
                    }
                    LauncherModel.mPreBadgeSettingValue = badgeSettingValue;
                }
            });
        }
    }

    private void updateTargetActivity(Notification notification, PackageUserKey packageUserKey) {
        if (notification != null) {
            ComponentName cn = notification.semBadgeTarget;
            if (cn != null) {
                packageUserKey.setTargetActivity(cn.getClassName());
            }
        }
    }

    private void updateLauncherIconBadges(Set<PackageUserKey> updatedBadges) {
        updateLauncherIconBadges(updatedBadges, true);
    }

    private void updateLauncherIconBadges(Set<PackageUserKey> updatedBadges, boolean shouldRefresh) {
        Iterator<PackageUserKey> iterator = updatedBadges.iterator();
        while (iterator.hasNext()) {
            BadgeInfo badgeInfo = (BadgeInfo) this.mPackageUserToBadgeInfos.get(iterator.next());
            if (!(badgeInfo == null || updateBadgeIcon(badgeInfo) || shouldRefresh)) {
                iterator.remove();
            }
        }
        if (!updatedBadges.isEmpty()) {
            Log.d(NOTI_TAG, "updateLauncherIconBadges : " + updatedBadges.size());
            final ArrayList<ItemInfo> badgeItems = DataLoader.updateNotificationBadgeCounts(updatedBadges, this.mPackageUserToBadgeInfos);
            if (!badgeItems.isEmpty()) {
                Iterator it = badgeItems.iterator();
                while (it.hasNext()) {
                    ItemInfo item = (ItemInfo) it.next();
                    if (item instanceof IconInfo) {
                        IconInfo iconInfo = (IconInfo) item;
                        if (!(!iconInfo.mShowBadge || this.mBadgeCache == null || iconInfo.getIntent() == null || iconInfo.getIntent().getComponent() == null)) {
                            int count = this.mBadgeCache.getBadgeCountFromBadgeProvider(iconInfo.getIntent().getComponent().getPackageName(), iconInfo.getUserHandle());
                            Log.d(NOTI_TAG, "updateLauncherIconBadges() count : " + count);
                            if (count > 0) {
                                item.mBadgeCount = count;
                            }
                        }
                    }
                    Log.d(NOTI_TAG, "updateLauncherIconBadges() item : [" + item.toString() + "], item.mBadgeCount: [" + item.mBadgeCount + "]");
                }
                this.mHandler.post(new Runnable() {
                    public void run() {
                        LauncherModel.this.onBadgeBindingCompleted(badgeItems);
                    }
                });
            }
        }
    }

    private boolean updateBadgeIcon(BadgeInfo badgeInfo) {
        SecurityException e;
        boolean hadNotificationToShow = badgeInfo.hasNotificationToShow();
        NotificationInfo notificationInfo = null;
        NotificationListener notificationListener = NotificationListener.getInstanceIfConnected();
        if (notificationListener != null && badgeInfo.getNotificationKeys().size() >= 1) {
            try {
                Iterator it = badgeInfo.getNotificationKeys().iterator();
                NotificationInfo notificationInfo2 = null;
                while (it.hasNext()) {
                    try {
                        StatusBarNotification[] activeNotifications = notificationListener.getActiveNotifications(new String[]{((NotificationKeyData) it.next()).notificationKey});
                        if (activeNotifications == null || activeNotifications.length != 1) {
                            notificationInfo = notificationInfo2;
                        } else {
                            notificationInfo = new NotificationInfo(LauncherAppState.getInstance().getContext(), activeNotifications[0]);
                            if (notificationInfo.shouldShowIconInBadge()) {
                                break;
                            }
                            notificationInfo = null;
                        }
                        notificationInfo2 = notificationInfo;
                    } catch (SecurityException e2) {
                        e = e2;
                        notificationInfo = notificationInfo2;
                    }
                }
                notificationInfo = notificationInfo2;
            } catch (SecurityException e3) {
                e = e3;
            }
        }
        badgeInfo.setNotificationToShow(notificationInfo);
        if (hadNotificationToShow || badgeInfo.hasNotificationToShow()) {
            return true;
        }
        return false;
        Log.w(NOTI_TAG, "SecurityException e = " + e);
        badgeInfo.setNotificationToShow(notificationInfo);
        if (!hadNotificationToShow) {
        }
        return true;
    }

    public BadgeInfo getBadgeInfoForItem(IconInfo info) {
        if (DeepShortcutManager.supportsBadgeType(info)) {
            return (BadgeInfo) this.mPackageUserToBadgeInfos.get(PackageUserKey.fromItemInfo(info));
        }
        return null;
    }

    public void cancelNotification(String notificationKey) {
        NotificationListener notificationListener = NotificationListener.getInstanceIfConnected();
        if (notificationListener != null) {
            notificationListener.cancelNotification(notificationKey);
        }
    }

    @NonNull
    public List<NotificationKeyData> getNotificationKeysForItem(ItemInfo info) {
        if (info instanceof IconInfo) {
            IconInfo iconInfo = (IconInfo) info;
            BadgeInfo badgeInfo = getBadgeInfoForItem(iconInfo);
            if (badgeInfo != null) {
                String targetClass = badgeInfo.getPackageUserKey() != null ? badgeInfo.getPackageUserKey().getTargetActivity() : null;
                if (targetClass == null) {
                    return badgeInfo.getNotificationKeys();
                }
                if (targetClass.equalsIgnoreCase(iconInfo.getTargetComponent().getClassName())) {
                    return badgeInfo.getNotificationKeys();
                }
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    public List<StatusBarNotification> getStatusBarNotificationsForKeys(List<NotificationKeyData> notificationKeys) {
        NotificationListener notificationListener = NotificationListener.getInstanceIfConnected();
        if (notificationListener == null) {
            return Collections.emptyList();
        }
        return notificationListener.getNotificationsForKeys(notificationKeys);
    }

    public void setHasLoaderCompletedOnce(boolean value) {
        synchronized (this.mLock) {
            this.mHasLoaderCompletedOnce = value;
        }
    }

    public void checkRemovedApps(ArrayList<ComponentKey> removed) {
        Iterator it = removed.iterator();
        while (it.hasNext()) {
            ComponentKey componentKey = (ComponentKey) it.next();
            Log.d(TAG, "Check component that removed in apps : " + componentKey.componentName);
            this.mHomeLoader.removeItem(componentKey.componentName, false, componentKey.user);
        }
    }
}
