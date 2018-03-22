package com.android.launcher3.common.quickoption.shortcuts;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MultiHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TargetApi(25)
public class DeepShortcutManager {
    private static final int FLAG_GET_ALL = 11;
    private static final String TAG = "DeepShortcutManager";
    private MultiHashMap<ComponentKey, String> mDeepShortcutMap = new MultiHashMap(0);
    private final LauncherApps mLauncherApps;
    private boolean mWasLastCallSuccess;

    public DeepShortcutManager(Context context, ShortcutCache cache) {
        this.mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    public static boolean supportsShortcuts(ItemInfo info) {
        return info.itemType == 0;
    }

    public static boolean supportsBadgeType(ItemInfo info) {
        return info.itemType == 0 || ((info instanceof IconInfo) && ((IconInfo) info).isAppShortcut);
    }

    public boolean wasLastCallSuccess() {
        return this.mWasLastCallSuccess;
    }

    public ShortcutInfoCompat queryForShortcutKey(ShortcutKey key) {
        List<ShortcutInfoCompat> shortcuts = query(11, key.componentName.getPackageName(), null, Collections.singletonList(key.getId()), key.user);
        return shortcuts.size() >= 1 ? (ShortcutInfoCompat) shortcuts.get(0) : null;
    }

    public List<ShortcutInfoCompat> queryForFullDetails(String packageName, List<String> shortcutIds, UserHandleCompat user) {
        return query(11, packageName, null, shortcutIds, user);
    }

    @TargetApi(25)
    public List<ShortcutInfoCompat> queryForShortcutsContainer(ComponentName activity, List<String> ids, UserHandleCompat user) {
        return query(9, activity.getPackageName(), activity, ids, user);
    }

    @TargetApi(25)
    public void unpinShortcut(ShortcutKey key) {
        RuntimeException e;
        if (Utilities.ATLEAST_N_MR1) {
            String packageName = key.componentName.getPackageName();
            String id = key.getId();
            UserHandleCompat user = key.user;
            List<String> pinnedIds = extractIds(queryForPinnedShortcuts(packageName, user));
            pinnedIds.remove(id);
            try {
                this.mLauncherApps.pinShortcuts(packageName, pinnedIds, user.getUser());
                this.mWasLastCallSuccess = true;
                return;
            } catch (SecurityException e2) {
                e = e2;
            } catch (IllegalStateException e3) {
                e = e3;
            }
        } else {
            return;
        }
        Log.w(TAG, "Failed to unpin shortcut", e);
        this.mWasLastCallSuccess = false;
    }

    @TargetApi(25)
    public void pinShortcut(ShortcutKey key) {
        RuntimeException e;
        if (Utilities.ATLEAST_N_MR1) {
            String packageName = key.componentName.getPackageName();
            String id = key.getId();
            UserHandleCompat user = key.user;
            List<String> pinnedIds = extractIds(queryForPinnedShortcuts(packageName, user));
            pinnedIds.add(id);
            try {
                this.mLauncherApps.pinShortcuts(packageName, pinnedIds, user.getUser());
                this.mWasLastCallSuccess = true;
                return;
            } catch (SecurityException e2) {
                e = e2;
            } catch (IllegalStateException e3) {
                e = e3;
            }
        } else {
            return;
        }
        Log.w(TAG, "Failed to pin shortcut", e);
        this.mWasLastCallSuccess = false;
    }

    @TargetApi(25)
    public void startShortcut(ShortcutKey key) {
        startShortcut(key.componentName.getPackageName(), key.getId(), null, null, key.user);
    }

    @TargetApi(25)
    public void startShortcut(String packageName, String id, Rect sourceBounds, Bundle startActivityOptions, UserHandleCompat user) {
        RuntimeException e;
        if (Utilities.ATLEAST_N_MR1) {
            try {
                this.mLauncherApps.startShortcut(packageName, id, sourceBounds, startActivityOptions, user.getUser());
                this.mWasLastCallSuccess = true;
                return;
            } catch (SecurityException e2) {
                e = e2;
            } catch (IllegalStateException e3) {
                e = e3;
            } catch (ActivityNotFoundException e4) {
                e = e4;
            }
        } else {
            return;
        }
        Log.e(TAG, "Failed to start shortcut", e);
        this.mWasLastCallSuccess = false;
    }

    @TargetApi(25)
    public Drawable getShortcutIconDrawable(ShortcutInfoCompat shortcutInfo) {
        if (Utilities.ATLEAST_N_MR1) {
            try {
                Drawable icon = this.mLauncherApps.getShortcutIconDrawable(shortcutInfo.getShortcutInfo(), 640);
                this.mWasLastCallSuccess = true;
                return icon;
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to get shortcut icon", e);
            }
        }
        this.mWasLastCallSuccess = false;
        return null;
    }

    public List<ShortcutInfoCompat> queryForPinnedShortcuts(String packageName, UserHandleCompat user) {
        return query(2, packageName, null, null, user);
    }

    public List<ShortcutInfoCompat> queryForAllShortcuts(UserHandleCompat user) {
        return query(11, null, null, null, user);
    }

    private List<String> extractIds(List<ShortcutInfoCompat> shortcuts) {
        List<String> shortcutIds = new ArrayList(shortcuts.size());
        for (ShortcutInfoCompat shortcut : shortcuts) {
            shortcutIds.add(shortcut.getId());
        }
        return shortcutIds;
    }

    @TargetApi(25)
    private List<ShortcutInfoCompat> query(int flags, String packageName, ComponentName activity, List<String> shortcutIds, UserHandleCompat user) {
        RuntimeException e;
        List<ShortcutInfoCompat> shortcutInfoCompats;
        if (!Utilities.ATLEAST_N_MR1) {
            return Collections.EMPTY_LIST;
        }
        ShortcutQuery q = new ShortcutQuery();
        q.setQueryFlags(flags);
        if (packageName != null) {
            q.setPackage(packageName);
            q.setActivity(activity);
            q.setShortcutIds(shortcutIds);
        }
        List<ShortcutInfo> list = null;
        try {
            list = this.mLauncherApps.getShortcuts(q, user.getUser());
            this.mWasLastCallSuccess = true;
        } catch (SecurityException e2) {
            e = e2;
            Log.e(TAG, "Failed to query for shortcuts", e);
            this.mWasLastCallSuccess = false;
            if (list != null) {
                return Collections.EMPTY_LIST;
            }
            shortcutInfoCompats = new ArrayList(list.size());
            for (ShortcutInfo shortcutInfo : list) {
                shortcutInfoCompats.add(new ShortcutInfoCompat(shortcutInfo));
            }
            return shortcutInfoCompats;
        } catch (IllegalStateException e3) {
            e = e3;
            Log.e(TAG, "Failed to query for shortcuts", e);
            this.mWasLastCallSuccess = false;
            if (list != null) {
                return Collections.EMPTY_LIST;
            }
            shortcutInfoCompats = new ArrayList(list.size());
            // TODO: Fix this code
//            while (r5.hasNext()) {
//                shortcutInfoCompats.add(new ShortcutInfoCompat(shortcutInfo));
//            }
            return shortcutInfoCompats;
        }
        if (list != null) {
            return Collections.EMPTY_LIST;
        }
        shortcutInfoCompats = new ArrayList(list.size());
        // TODO: Fix this code
//        while (r5.hasNext()) {
//            shortcutInfoCompats.add(new ShortcutInfoCompat(shortcutInfo));
//        }
        return shortcutInfoCompats;
    }

    @TargetApi(25)
    public boolean hasHostPermission() {
        RuntimeException e;
        if (Utilities.ATLEAST_N_MR1) {
            try {
                return this.mLauncherApps.hasShortcutHostPermission();
            } catch (SecurityException e2) {
                e = e2;
                Log.e(TAG, "Failed to make shortcut manager call", e);
                return false;
            } catch (IllegalStateException e3) {
                e = e3;
                Log.e(TAG, "Failed to make shortcut manager call", e);
                return false;
            }
        }
        return false;
    }

    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        this.mDeepShortcutMap = deepShortcutMapCopy;
        Log.d(TAG, "bindDeepShortcutMap: " + this.mDeepShortcutMap);
    }

    public List<String> getShortcutIdsForItem(IconInfo info) {
        if (!supportsShortcuts(info)) {
            return Collections.EMPTY_LIST;
        }
        ComponentName component = info.getTargetComponent();
        if (component == null) {
            return Collections.EMPTY_LIST;
        }
        List<String> ids = (List) this.mDeepShortcutMap.get(new ComponentKey(component, info.user));
        return ids == null ? Collections.EMPTY_LIST : ids;
    }

    public void closeShortcutsContainer(Context context) {
        closeShortcutsContainer(context, true);
    }

    private void closeShortcutsContainer(Context context, boolean animate) {
        DeepShortcutsContainer deepShortcutsContainer = getOpenShortcutsContainer(context);
        if (deepShortcutsContainer == null) {
            return;
        }
        if (animate) {
            deepShortcutsContainer.animateClose();
        } else {
            deepShortcutsContainer.close();
        }
    }

    public DeepShortcutsContainer getOpenShortcutsContainer(Context context) {
        if (!LauncherFeature.supportDeepShortcut()) {
            return null;
        }
        DragLayer dragLayer = ((Launcher) context).getDragLayer();
        for (int i = dragLayer.getChildCount() - 1; i >= 0; i--) {
            View child = dragLayer.getChildAt(i);
            if ((child instanceof DeepShortcutsContainer) && ((DeepShortcutsContainer) child).isOpen()) {
                return (DeepShortcutsContainer) child;
            }
        }
        return null;
    }
}
