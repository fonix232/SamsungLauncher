package com.android.launcher3.common.model;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableInt;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.Callbacks;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.scloud.SCloudBnr;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.PackageInstallerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.OpenMarketCustomization;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.BadgeCache.CacheKey;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.home.LauncherPairAppsInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.PairAppsUtilities;
import com.android.launcher3.util.ShortcutTray;
import com.android.launcher3.util.StringFilter;
import com.android.launcher3.util.StringJoiner;
import com.samsung.android.knox.SemPersonaManager;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class DataLoader {
    protected static final String[] STK_PKG_LIST = new String[]{"com.android.stk", "com.android.stk2"};
    protected static final String STK_SPLIT = ";";
    private static final String TAG = "DataLoader";
    protected static boolean mIsBootCompleted;
    protected static BadgeCache sBadgeCache;
    protected static final MultiHashMap<ComponentKey, String> sBgDeepShortcutMap = new MultiHashMap();
    protected static final LongArrayMap<FolderInfo> sBgFolders = new LongArrayMap();
    protected static final LongArrayMap<ItemInfo> sBgItemsIdMap = new LongArrayMap();
    protected static final Object sBgLock = new Object();
    protected static final Map<ShortcutKey, MutableInt> sBgPinnedShortcutCounts = new HashMap();
    protected static final ArrayList<Runnable> sBindCompleteRunnables = new ArrayList();
    protected static ContentResolver sContentResolver;
    protected static Context sContext;
    protected static FavoritesProvider sFavoritesProvider;
    protected static DeferredHandler sHandler;
    protected static IconCache sIconCache;
    protected static volatile HashMap<String, Integer> sInstallingPkgs;
    protected static boolean sIsLoadingAndBindingWorkspace;
    protected static boolean sIsSafeMode;
    protected static LauncherAppsCompat sLauncherApps;
    protected static LauncherModel sLauncherModel;
    private static final ArrayList<DataLoader> sLoaderList = new ArrayList();
    private static List<ResolveInfo> sOmcActivity = null;
    protected static PackageManager sPackageManager;
    protected static final HashMap<UserHandleCompat, HashSet<String>> sPendingPackages = new HashMap();
    protected static DeviceProfile sProfile;
    protected static UserManagerCompat sUserManager;
    protected static Handler sWorkerHandler;
    public ArrayList<IconInfo> added = new ArrayList();
    protected LongSparseArray<UserHandleCompat> mAllUsers;
    protected final ArrayList<Long> mBgOrderedScreens = new ArrayList();
    protected final HashMap<Long, ArrayList<ItemInfo>> mBgPagesItems = new HashMap();
    protected DeepShortcutManager mDeepShortcutManager;
    protected final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList();
    protected FavoritesUpdater mFavoritesUpdater;
    protected boolean[] mIsPageLoaded;
    protected DataLoaderCallback mLoaderCallback;
    protected PageLoaderTask mPageLoaderTask;
    protected LongSparseArray<Boolean> mQuietModeUsers;
    protected Map<ShortcutKey, ShortcutInfoCompat> mShortcutKeyToPinnedShortcuts = new HashMap();
    public ArrayList<IconInfo> modified = new ArrayList();
    public ArrayList<IconInfo> removed = new ArrayList();

    public interface DataLoaderCallback {
        void onLoaderComplete();
    }

    public interface DataLoaderState {
        boolean isStopped();
    }

    public interface ItemInfoFilter {
        boolean filterItem(ItemInfo itemInfo, ItemInfo itemInfo2, ComponentName componentName);
    }

    private static class KnoxBadgeUpdates {
        private static Map<String, String> BaseTargetMapping = new HashMap();
        private static final String TAG = KnoxBadgeUpdates.class.getSimpleName();
        private static String TARGET_PACKAGE;
        private static int aliasActivityCount;
        private static boolean[] isAliasActivitiesBadgeCleared;
        private static boolean isKnoxIconsBadgeInitialised = false;

        private KnoxBadgeUpdates() {
        }

        static {
            TARGET_PACKAGE = null;
            Log.d(TAG, "KnoxBadgeUpdates: " + UserHandle.semGetMyUserId());
            if (!SemPersonaManager.isSecureFolderId(UserHandle.semGetMyUserId()) && SemPersonaManager.isKnoxId(UserHandle.semGetMyUserId())) {
                TARGET_PACKAGE = "com.samsung.android.knox.containeragent";
                Log.d(TAG, "KnoxBadgeUpdates: initialising baseTarget map");
                BaseTargetMapping.put("com.samsung.android.messaging", TARGET_PACKAGE + ".switcher.SMSIcon");
                BaseTargetMapping.put("com.samsung.android.contacts", TARGET_PACKAGE + ".switcher.PhoneIcon");
                setAliasActivityCount(BaseTargetMapping.size());
            }
        }

        private static void setAliasActivityCount(int count) {
            if (!isKnoxIconsBadgeInitialised) {
                aliasActivityCount = count;
                isAliasActivitiesBadgeCleared = new boolean[count];
                Arrays.fill(isAliasActivitiesBadgeCleared, Boolean.TRUE.booleanValue());
                isKnoxIconsBadgeInitialised = true;
            }
        }

        private static boolean isKnoxBadgeUpdated() {
            boolean ret = true;
            for (int counter = 0; counter < aliasActivityCount; counter++) {
                ret &= isAliasActivitiesBadgeCleared[counter];
            }
            return ret;
        }

        public static void updateBadgeCountForKnoxIcons(ArrayList<ItemInfo> updated, Map<PackageUserKey, BadgeInfo> badgeMap) {
            if (!SemPersonaManager.isSecureFolderId(UserHandle.semGetMyUserId()) && SemPersonaManager.isKnoxId(UserHandle.semGetMyUserId())) {
                int i = 0;
                for (String basePackage : BaseTargetMapping.keySet()) {
                    int i2 = i + 1;
                    ArrayList<ItemInfo> arrayList = updated;
                    Map<PackageUserKey, BadgeInfo> map = badgeMap;
                    updateBadgeCountForKnoxIconsForComponent(arrayList, map, TARGET_PACKAGE, (String) BaseTargetMapping.get(basePackage), new PackageUserKey(basePackage, UserHandleCompat.fromUser(UserHandle.getUserHandleForUid(0))), i);
                    i = i2;
                }
            }
        }

        private static void updateBadgeCountForKnoxIconsForComponent(ArrayList<ItemInfo> updated, Map<PackageUserKey, BadgeInfo> badgeMap, String targetPackageName, String targetComponent, PackageUserKey basePackageUserKey, int aliasCounter) {
            BadgeInfo baseBadgeInfo = (BadgeInfo) badgeMap.get(basePackageUserKey);
            PackageUserKey targetPackageUserKey = new PackageUserKey(targetPackageName, UserHandleCompat.myUserHandle());
            targetPackageUserKey.setTargetActivity(targetComponent);
            if (baseBadgeInfo != null || (!isAliasActivitiesBadgeCleared[aliasCounter] && !isKnoxBadgeUpdated())) {
                List<ItemInfo> targetItemInfo = new ArrayList();
                Iterator<ItemInfo> it = DataLoader.sBgItemsIdMap.iterator();
                while (it.hasNext()) {
                    ItemInfo itemInfo = (ItemInfo) it.next();
                    if (itemInfo instanceof IconInfo) {
                        IconInfo iconInfo = (IconInfo) itemInfo;
                        if (!(iconInfo.getTargetComponent() == null || iconInfo.getTargetComponent().getPackageName() == null || iconInfo.getTargetComponent().getClassName() == null || !iconInfo.getTargetComponent().getPackageName().equals(targetPackageName) || !iconInfo.getTargetComponent().getClassName().equals(targetComponent))) {
                            Log.d(TAG, "target component current count [" + iconInfo.mBadgeCount + "] componentName [" + iconInfo.getTargetComponent().getClassName() + "]");
                            targetItemInfo.add(iconInfo);
                        }
                    }
                }
                if (baseBadgeInfo != null) {
                    baseBadgeInfo.setPackageUserKey(targetPackageUserKey);
                    Log.d(TAG, "new count [" + baseBadgeInfo.getNotificationCount() + "]");
                    for (ItemInfo iteminfo : targetItemInfo) {
                        iteminfo.mBadgeCount = baseBadgeInfo.getNotificationCount();
                        iteminfo.mShowBadge = true;
                        updated.add(iteminfo);
                    }
                    if (baseBadgeInfo.getNotificationCount() != 0) {
                        badgeMap.put(targetPackageUserKey, baseBadgeInfo);
                    }
                    isAliasActivitiesBadgeCleared[aliasCounter] = false;
                    return;
                }
                Log.d(TAG, "setting count to 0");
                for (ItemInfo iteminfo2 : targetItemInfo) {
                    iteminfo2.mBadgeCount = 0;
                    iteminfo2.mShowBadge = false;
                    updated.add(iteminfo2);
                }
                isAliasActivitiesBadgeCleared[aliasCounter] = true;
            }
        }
    }

    public abstract class PageLoaderTask extends AsyncTask<DataLoaderCallback, Void, Boolean> {
        protected boolean mStopped;

        void setStopped(boolean isStopped) {
            synchronized (this) {
                Log.d(DataLoader.TAG, "setStopped isStopped " + isStopped);
                this.mStopped = isStopped;
            }
        }
    }

    public abstract void addPagesItem(ItemInfo itemInfo);

    public abstract void bindItemsSync(int i, DataLoaderState dataLoaderState);

    public abstract void bindPageItems(ArrayList<ItemInfo> arrayList, ArrayList<Runnable> arrayList2, DataLoaderState dataLoaderState);

    protected abstract void clearSBgDataStructures();

    public abstract boolean containPagesItem(ItemInfo itemInfo);

    public abstract void filterCurrentPageItems(long j, ArrayList<ItemInfo> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3);

    protected abstract DataUpdater getUpdater();

    protected abstract ArrayList<ItemInfo> loadPageItems(int i, DataLoaderState dataLoaderState);

    public abstract void removePagesItem(ItemInfo itemInfo);

    public abstract void removeUnRestoredItems(boolean z);

    public abstract void setOrderedScreen(ArrayList<Long> arrayList);

    public abstract void setup(DataLoaderState dataLoaderState);

    public abstract void titleUpdate();

    public abstract void unbindItemsOnMainThread();

    public abstract void updatePackageFlags(StringFilter stringFilter, UserHandleCompat userHandleCompat, FlagOp flagOp);

    public abstract void updatePagesItem(ItemInfo itemInfo);

    public abstract void updateUnavailablePackage(String str, UserHandleCompat userHandleCompat, int i);

    protected void init(Context context, LauncherAppState app, LauncherModel model, IconCache cache, BadgeCache badgeCache, DataLoader loader) {
        if (sContext == null) {
            sContext = context;
            sWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
            sContentResolver = context.getContentResolver();
            sIsSafeMode = context.getPackageManager().isSafeMode();
            sLauncherApps = LauncherAppsCompat.getInstance(context);
            sProfile = app.getDeviceProfile();
            sUserManager = UserManagerCompat.getInstance(context);
            sPackageManager = context.getPackageManager();
            sOmcActivity = sPackageManager.queryIntentActivities(OpenMarketCustomization.getOmcIntent(null), 0);
            sIconCache = cache;
            sBadgeCache = badgeCache;
            sLauncherModel = model;
            sHandler = model.getHandler();
            sFavoritesProvider = FavoritesProvider.getInstance();
        }
        this.mDeepShortcutManager = app.getShortcutManager();
        updateUsersList();
        if (!sLoaderList.contains(loader)) {
            sLoaderList.add(loader);
        }
    }

    public void updateUsersList() {
        if (this.mAllUsers == null) {
            this.mAllUsers = new LongSparseArray();
        }
        if (this.mQuietModeUsers == null) {
            this.mQuietModeUsers = new LongSparseArray();
        }
        this.mAllUsers.clear();
        this.mQuietModeUsers.clear();
        for (UserHandleCompat user : sUserManager.getUserProfiles()) {
            long serialNo = sUserManager.getSerialNumberForUser(user);
            this.mAllUsers.put(serialNo, user);
            this.mQuietModeUsers.put(serialNo, Boolean.valueOf(sUserManager.isQuietModeEnabled(user)));
        }
    }

    public static void setDeviceProfile(DeviceProfile dp) {
        sProfile = dp;
    }

    protected String makeFoldersIdToString(LongArrayMap<ItemInfo> folders) {
        StringJoiner joiner = new StringJoiner(",");
        Iterator it = folders.iterator();
        while (it.hasNext()) {
            joiner.append(((ItemInfo) it.next()).id);
        }
        return joiner.toString();
    }

    protected void sortItemsInFolder(LongArrayMap<ItemInfo> folders) {
        if (folders != null) {
            Iterator it = folders.iterator();
            while (it.hasNext()) {
                FolderInfo folder = (FolderInfo) ((ItemInfo) it.next());
                folder.sortContents();
                int pos = 0;
                Iterator it2 = folder.contents.iterator();
                while (it2.hasNext()) {
                    ItemInfo info = (ItemInfo) it2.next();
                    if (((IconInfo) info).usingLowResIcon) {
                        ((IconInfo) info).updateIcon(sIconCache, false);
                    }
                    pos++;
                    if (pos >= 9) {
                        break;
                    }
                }
            }
        }
    }

    protected void runOnMainThread(Runnable r) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            sHandler.post(r);
        }
    }

    protected static void runOnWorkerThread(Runnable r) {
        if (LauncherModel.sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sWorkerHandler.post(r);
        }
    }

    public void clearDeferredBindRunnable() {
        synchronized (this.mDeferredBindRunnables) {
            this.mDeferredBindRunnables.clear();
        }
    }

    protected void addDeferredBindRunnable(Runnable r) {
        synchronized (this.mDeferredBindRunnables) {
            this.mDeferredBindRunnables.add(r);
        }
    }

    public void setLoaderTaskStop(boolean isStopped) {
        Log.d(TAG, "setLoaderTaskStop isStopped " + isStopped);
        PageLoaderTask oldTask = this.mPageLoaderTask;
        if (oldTask != null) {
            Log.d(TAG, "oldTask is not null. set stop & cancel");
            oldTask.setStopped(isStopped);
            oldTask.cancel(true);
        }
    }

    protected boolean isStopped(DataLoaderState task) {
        return task == null ? false : task.isStopped();
    }

    public static void setLoadingAndBindingWorkspace(boolean isLoadAndBinding) {
        sIsLoadingAndBindingWorkspace = isLoadAndBinding;
    }

    protected void putItemToIdMap(ItemInfo info) {
        synchronized (sBgLock) {
            sBgItemsIdMap.put(info.id, info);
            if (info instanceof FolderInfo) {
                sBgFolders.put(info.id, (FolderInfo) info);
            }
        }
    }

    protected ArrayList<ItemInfo> getAllApps() {
        ArrayList<ItemInfo> allApps = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.itemType == 0) {
                    allApps.add(info);
                }
            }
        }
        return allApps;
    }

    public static List<ItemInfo> getItemList() {
        List<ItemInfo> list = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                list.add((ItemInfo) it.next());
            }
        }
        return list;
    }

    public static ArrayList<ItemInfo> getItemsByPackageName(final String pn, final UserHandleCompat user) {
        ArrayList<ItemInfo> filterItemInfo;
        ItemInfoFilter filter = new ItemInfoFilter() {
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                return cn.getPackageName().equals(pn) && info.user.equals(user);
            }
        };
        synchronized (sBgLock) {
            filterItemInfo = filterItemInfo(sBgItemsIdMap, filter, false);
        }
        return filterItemInfo;
    }

    public static ArrayList<ItemInfo> getItemInfoByComponentName(final ComponentName cName, final UserHandleCompat user, boolean exceptWidget) {
        ArrayList<ItemInfo> filterItemInfo;
        ItemInfoFilter filter = new ItemInfoFilter() {
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (info.user == null) {
                    return cn.equals(cName);
                }
                return cn.equals(cName) && info.user.equals(user);
            }
        };
        synchronized (sBgLock) {
            filterItemInfo = filterItemInfo(sBgItemsIdMap, filter, exceptWidget);
        }
        return filterItemInfo;
    }

    public static ArrayList<ItemInfo> filterItemInfo(Iterable<ItemInfo> infos, ItemInfoFilter f, boolean exceptWidget) {
        HashSet<ItemInfo> filtered = new HashSet();
        for (ItemInfo i : infos) {
            ComponentName cn;
            if (i instanceof IconInfo) {
                IconInfo info = (IconInfo) i;
                cn = info.getTargetComponent();
                if (cn != null && f.filterItem(null, info, cn)) {
                    filtered.add(info);
                } else if (i instanceof LauncherPairAppsInfo) {
                    LauncherPairAppsInfo pairApps = (LauncherPairAppsInfo) i;
                    if ((pairApps.mFirstApp.getCN() != null && f.filterItem(null, info, pairApps.mFirstApp.getCN())) || (pairApps.mSecondApp.getCN() != null && f.filterItem(null, info, pairApps.mSecondApp.getCN()))) {
                        filtered.add(info);
                    }
                }
            } else if (i instanceof FolderInfo) {
                FolderInfo info2 = (FolderInfo) i;
                Iterator it = ((ArrayList) info2.contents.clone()).iterator();
                while (it.hasNext()) {
                    IconInfo s = (IconInfo) it.next();
                    cn = s.getTargetComponent();
                    if (cn != null && f.filterItem(info2, s, cn)) {
                        filtered.add(s);
                    }
                }
            } else if ((i instanceof LauncherAppWidgetInfo) && !exceptWidget) {
                LauncherAppWidgetInfo info3 = (LauncherAppWidgetInfo) i;
                cn = info3.providerName;
                if (cn != null && f.filterItem(null, info3, cn)) {
                    filtered.add(info3);
                }
            }
        }
        return new ArrayList(filtered);
    }

    public static ArrayList<ItemInfo> updateBadgeCounts(Map<CacheKey, Integer> badgeCounts) {
        ArrayList<ItemInfo> updated = new ArrayList(badgeCounts.size());
        synchronized (sBgLock) {
            for (Entry<CacheKey, Integer> entry : badgeCounts.entrySet()) {
                CacheKey cacheKey = (CacheKey) entry.getKey();
                Iterator<ItemInfo> it = sBgItemsIdMap.iterator();
                while (it.hasNext()) {
                    ItemInfo itemInfo = (ItemInfo) it.next();
                    if (itemInfo instanceof IconInfo) {
                        IconInfo iconInfo = (IconInfo) itemInfo;
                        if (cacheKey.componentName.equals(iconInfo.getTargetComponent()) && cacheKey.user.equals(iconInfo.getUserHandle()) && iconInfo.mShowBadge) {
                            int newCount = ((Integer) entry.getValue()).intValue();
                            if (iconInfo.mBadgeCount != newCount && newCount >= 0) {
                                iconInfo.mBadgeCount = newCount;
                                updated.add(iconInfo);
                            }
                        }
                    }
                }
            }
        }
        return updated;
    }

    public static ArrayList<ItemInfo> updateNotificationBadgeCounts(Set<PackageUserKey> updatedBadges, Map<PackageUserKey, BadgeInfo> badgeMap) {
        ArrayList<ItemInfo> updated = new ArrayList(updatedBadges.size());
        PackageUserKey packageUserKey = new PackageUserKey(null, null);
        int badgeSettingValue = Utilities.getBadgeSettingValue(sContext);
        for (PackageUserKey updatedBadgePackageUserKey : updatedBadges) {
            synchronized (sBgLock) {
                Iterator<ItemInfo> it = sBgItemsIdMap.iterator();
                while (it.hasNext()) {
                    ItemInfo itemInfo = (ItemInfo) it.next();
                    if (itemInfo instanceof IconInfo) {
                        IconInfo iconInfo = (IconInfo) itemInfo;
                        if (packageUserKey.updateFromItemInfo(iconInfo) && packageUserKey.equals(updatedBadgePackageUserKey)) {
                            String targetClass = updatedBadgePackageUserKey.getTargetActivity();
                            if (targetClass == null) {
                                setBadgeCount(updated, badgeSettingValue, iconInfo);
                            } else if (targetClass.equalsIgnoreCase(iconInfo.getTargetComponent().getClassName())) {
                                setBadgeCount(updated, badgeSettingValue, iconInfo);
                            }
                        }
                    }
                }
            }
        }
        KnoxBadgeUpdates.updateBadgeCountForKnoxIcons(updated, badgeMap);
        return updated;
    }

    private static void setBadgeCount(ArrayList<ItemInfo> updated, int badgeSettingValue, IconInfo iconInfo) {
        boolean z = false;
        BadgeInfo badgeInfo = sLauncherModel.getBadgeInfoForItem(iconInfo);
        if (badgeInfo == null) {
            iconInfo.mBadgeCount = 0;
            iconInfo.mShowBadge = false;
            updated.add(iconInfo);
        } else if (iconInfo.mBadgeCount != badgeInfo.getNotificationCount() || LauncherModel.mPreBadgeSettingValue != badgeSettingValue || (!iconInfo.mShowBadge && badgeInfo.getNotificationCount() > 0)) {
            iconInfo.mBadgeCount = badgeInfo.getNotificationCount();
            if (badgeInfo.getNotificationCount() > 0) {
                z = true;
            }
            iconInfo.mShowBadge = z;
            updated.add(iconInfo);
        }
    }

    protected void setBadgeCount(IconInfo item) {
        item.mBadgeCount = sBadgeCache.getBadgeCount(new CacheKey(item.getTargetComponent(), item.user));
    }

    public static void setInstallingPackage(Context context) {
        if (sInstallingPkgs == null) {
            sInstallingPkgs = new HashMap();
        } else {
            sInstallingPkgs.clear();
        }
        sInstallingPkgs = PackageInstallerCompat.getInstance(context).updateAndGetActiveSessionCache();
    }

    public static void reinflateWidgetsIfNecessary() {
        runOnWorkerThread(new Runnable() {
            public void run() {
                int orientation = Utilities.getOrientation();
                if (Utilities.canScreenRotate() || orientation != 2) {
                    synchronized (DataLoader.sBgLock) {
                        Iterator it = DataLoader.sBgItemsIdMap.iterator();
                        while (it.hasNext()) {
                            ItemInfo itemInfo = (ItemInfo) it.next();
                            if (itemInfo instanceof LauncherAppWidgetInfo) {
                                ((LauncherAppWidgetInfo) itemInfo).reinflateWidgetsIfNecessary();
                            }
                        }
                    }
                    return;
                }
                Log.i(DataLoader.TAG, "reinflateWidgetsIfNecessary return");
            }
        });
    }

    public static void loadDefaultLayoutIfNecessary() {
        Iterator it;
        if (PostPositionController.getInstance(sContext).isReloadNeeded()) {
            it = sLoaderList.iterator();
            while (it.hasNext()) {
                ((DataLoader) it.next()).loadDefaultLayout(null, false, true);
            }
            Log.d(TAG, "noFDR OMCItem - " + sContentResolver.delete(Favorites.CONTENT_URI, "restored=?", new String[]{String.valueOf(32)}) + " items removed.");
        } else {
            SharedPreferences sp = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
            boolean isEmptyDb = false;
            boolean SwitchedDb = false;
            if (sp.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                Log.d(TAG, "loading default workspace");
                isEmptyDb = true;
            } else if (sp.getBoolean(FavoritesProvider.EMPTY_DATABASE_SWITCHED, false)) {
                Log.d(TAG, "loading default workspace without switch table");
                SwitchedDb = true;
                isEmptyDb = true;
            }
            if (isEmptyDb) {
                AutoInstallsLayout autoInstallLayout = null;
                if (!SwitchedDb) {
                    autoInstallLayout = AutoInstallsLayout.get(sContext);
                    OpenMarketCustomization.getInstance().loadOmcIfNecessary(sContext);
                }
                it = sLoaderList.iterator();
                while (it.hasNext()) {
                    ((DataLoader) it.next()).loadDefaultLayout(autoInstallLayout, SwitchedDb, false);
                }
                if (SwitchedDb) {
                    sFavoritesProvider.clearFlagEmptyDbSwitched();
                } else {
                    LauncherAppState.getLauncherProvider().clearFlagEmptyDbCreated();
                }
            }
            sFavoritesProvider.checkChangedComponentExist(isEmptyDb);
            if (isEmptyDb) {
                it = sLoaderList.iterator();
                while (it.hasNext()) {
                    ((DataLoader) it.next()).loadDefaultLayoutCompleted();
                }
            }
        }
        PostPositionController.getInstance(sContext).checkAndEnablePositioner();
    }

    protected ArrayList<ItemInfo> getUnRestoredItems(ArrayList<ItemInfo> items) {
        ArrayList<ItemInfo> removeItems = new ArrayList();
        ArrayList<Long> updateIds = new ArrayList();
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info instanceof IconInfo) {
                IconInfo iconInfo = (IconInfo) info;
                if ((iconInfo.status & 4) != 0) {
                    ComponentName cn;
                    if (iconInfo.componentName != null) {
                        cn = iconInfo.componentName;
                    } else if (iconInfo.intent == null || iconInfo.intent.getComponent() == null) {
                        removeItems.add(iconInfo);
                    } else {
                        cn = iconInfo.intent.getComponent();
                    }
                    if (launcherApps.isActivityEnabledForProfile(cn, iconInfo.user)) {
                        updateIds.add(Long.valueOf(iconInfo.id));
                        iconInfo.status = 0;
                    } else {
                        removeItems.add(iconInfo);
                    }
                }
            }
        }
        if (!updateIds.isEmpty()) {
            Log.d(TAG, "update restored value " + updateIds);
            ContentValues values = new ContentValues();
            values.put("restored", Integer.valueOf(0));
            sContentResolver.update(Favorites.CONTENT_URI, values, Utilities.createDbSelectionQuery("_id", updateIds), null);
        }
        return removeItems;
    }

    protected void loadDefaultLayout(AutoInstallsLayout autoInstallLayout, boolean isSwitchedDb, boolean reloadPostPosition) {
    }

    protected void loadDefaultLayoutCompleted() {
    }

    protected void updateItem(long itemId, ContentValues update) {
        sContentResolver.update(Favorites.CONTENT_URI, update, "_id= ?", new String[]{Long.toString(itemId)});
    }

    protected IconInfo getRestoredItemInfo(Cursor c, ComponentName cn, Intent intent, int promiseType, CursorInfo iconInfo) {
        String title = null;
        IconInfo info = new IconInfo();
        info.user = UserHandleCompat.myUserHandle();
        info.itemType = c != null ? c.getInt(iconInfo.itemTypeIndex) : 0;
        if ((promiseType & 4) == 0 || info.itemType != 0) {
            Bitmap icon = c == null ? null : iconInfo.loadIcon(c, info, sContext);
            if (icon == null) {
                sIconCache.getTitleAndIcon(info, intent, info.user, false);
            } else {
                info.setOriginalIcon(icon);
                info.setIcon(ShortcutTray.getIcon(sContext, icon, cn));
                info.customIcon = true;
            }
            if ((promiseType & 1) != 0) {
                if (c != null) {
                    title = c.getString(iconInfo.titleIndex);
                }
                if (!TextUtils.isEmpty(title)) {
                    info.title = Utilities.trim(title);
                }
            } else if ((promiseType & 2) == 0 && (promiseType & 32) == 0 && (promiseType & 4) == 0 && (promiseType & 64) == 0) {
                throw new InvalidParameterException("Invalid restoreType " + promiseType);
            } else if (TextUtils.isEmpty(info.title)) {
                if (c != null) {
                    title = c.getString(iconInfo.titleIndex);
                }
                info.title = Utilities.trim(title);
            }
        } else {
            SCloudBnr.getRestoreDummyInfo(sContext, cn, info);
            if (info.mIcon == null) {
                Log.i(TAG, "getRestoredItemInfo SCloud dummy icon is null cn = " + cn);
                return null;
            }
        }
        if (info.title == null) {
            info.title = "";
        }
        info.contentDescription = sUserManager.getBadgedLabelForUser(info.title, info.user);
        info.promisedIntent = intent;
        info.status = promiseType;
        return info;
    }

    protected Intent getRestoredItemIntent(Intent intent, int promiseType) {
        if ((promiseType & 1) != 0 || (promiseType & 2) != 0 || (promiseType & 64) != 0) {
            return LauncherModel.getMarketIntent(intent.getComponent().getPackageName());
        }
        if ((promiseType & 32) == 0) {
            return intent;
        }
        if (sOmcActivity == null || sOmcActivity.size() <= 0 || intent.getComponent() == null || intent.getComponent().getPackageName().isEmpty()) {
            return OpenMarketCustomization.getOmcIntent();
        }
        return OpenMarketCustomization.getOmcIntent(intent.getComponent().getPackageName());
    }

    protected static LauncherActivityInfoCompat findActivityInfo(List<LauncherActivityInfoCompat> apps, ComponentName component, UserHandleCompat user) {
        if (component == null || user == null) {
            return null;
        }
        for (LauncherActivityInfoCompat info : apps) {
            if (user.equals(info.getUser()) && component.equals(info.getComponentName())) {
                return info;
            }
        }
        return null;
    }

    protected int isNotAvailableApps(String packageName) {
        boolean internalInstalled = true;
        try {
            ApplicationInfo appInfo = sPackageManager.getApplicationInfo(packageName, 8192);
            if (appInfo == null) {
                return 0;
            }
            boolean isSystemApp;
            boolean externalInstalled;
            if ((appInfo.flags & 1) != 0) {
                isSystemApp = true;
            } else {
                isSystemApp = false;
            }
            if ((appInfo.flags & 262144) != 0) {
                externalInstalled = true;
            } else {
                externalInstalled = false;
            }
            if (isSystemApp || externalInstalled || (appInfo.flags & 8388608) == 0) {
                internalInstalled = false;
            }
            if (sIsSafeMode) {
                if (internalInstalled || externalInstalled) {
                    return 2;
                }
                return 0;
            } else if (externalInstalled) {
                return 32;
            } else {
                return 0;
            }
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    public static FolderInfo getFolderInfo(int id) {
        FolderInfo folderInfo;
        synchronized (sBgLock) {
            folderInfo = (FolderInfo) sBgFolders.get((long) id);
        }
        return folderInfo;
    }

    public void updatePackage(String packageName, ArrayList<ItemInfo> currentItems, UserHandleCompat user) {
        List<LauncherActivityInfoCompat> matches = sLauncherApps.getActivityList(packageName, user);
        Log.d(TAG, "updatePackage:" + packageName);
        if (!matches.isEmpty()) {
            updatePackageActivities(packageName, currentItems, matches, user);
        } else if (!"com.sec.android.app.magnifier".equals(packageName)) {
            removePackage(packageName, currentItems, user);
        }
    }

    public void removePackage(String packageName, ArrayList<ItemInfo> currentItems, UserHandleCompat user) {
        Iterator<ItemInfo> it = currentItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            IconInfo info = (IconInfo) item;
            ComponentName cn = info.componentName;
            if (cn != null && user.equals(item.user) && packageName.equals(cn.getPackageName())) {
                this.removed.add(info);
                it.remove();
                backupStkPositionIfNecessary(packageName, info.container, info.screenId, info.cellX, info.cellY);
                sIconCache.remove(cn, user);
                this.mFavoritesUpdater.deleteItem(item);
            }
        }
    }

    private synchronized void updatePackageActivities(String packageName, ArrayList<ItemInfo> currentItems, List<LauncherActivityInfoCompat> activities, UserHandleCompat user) {
        Iterator<ItemInfo> it = currentItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            IconInfo info = (IconInfo) item;
            ComponentName cn = info.componentName;
            if (cn != null) {
                if (user.equals(item.user) && packageName.equals(cn.getPackageName()) && !findActivity(activities, cn)) {
                    this.removed.add(info);
                    it.remove();
                    sIconCache.remove(cn, user);
                    this.mFavoritesUpdater.deleteItem(item);
                }
            }
        }
        for (LauncherActivityInfoCompat app : activities) {
            ArrayList<ItemInfo> items = getItemInfoByComponentName(app.getComponentName(), currentItems, user, true);
            if (items.size() > 0) {
                Iterator it2 = items.iterator();
                while (it2.hasNext()) {
                    IconInfo icon = (IconInfo) ((ItemInfo) it2.next());
                    icon.isDisabled = 0;
                    this.modified.add(icon);
                }
            } else if (sFavoritesProvider != null) {
                info = new IconInfo(sContext, app, user, sIconCache, sUserManager.isQuietModeEnabled(user));
                if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    info.container = -102;
                    info.mDirty = true;
                }
                this.added.add(info);
            }
        }
    }

    private ArrayList<ItemInfo> getItemInfoByComponentName(final ComponentName cName, ArrayList<ItemInfo> currentItems, final UserHandleCompat user, boolean exceptWidget) {
        ArrayList<ItemInfo> filterItemInfo;
        ItemInfoFilter filter = new ItemInfoFilter() {
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (info.user == null) {
                    return cn.equals(cName);
                }
                return cn.equals(cName) && info.user.equals(user);
            }
        };
        synchronized (sBgLock) {
            filterItemInfo = filterItemInfo(currentItems, filter, exceptWidget);
        }
        return filterItemInfo;
    }

    protected static boolean findActivity(List<LauncherActivityInfoCompat> apps, ComponentName component) {
        for (LauncherActivityInfoCompat info : apps) {
            if (info.getComponentName().equals(component)) {
                return true;
            }
        }
        return false;
    }

    protected void backupStkPositionIfNecessary(String pkgName, long container, long screen, int cellX, int cellY) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            boolean needToBackupPosition = false;
            for (String s : STK_PKG_LIST) {
                if (pkgName.equals(s)) {
                    needToBackupPosition = true;
                    break;
                }
            }
            if (needToBackupPosition) {
                int countX = sProfile.homeGrid.getCellCountX();
                int countY = sProfile.homeGrid.getCellCountY();
                if (cellX < 0 || cellY < 0 || cellX >= countX || cellY >= countY) {
                    Log.d(TAG, "Stk outside position cellX : " + cellX + " cellY : " + cellY);
                    return;
                }
                Editor prefs = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
                prefs.putString(pkgName, container + STK_SPLIT + screen + STK_SPLIT + cellX + STK_SPLIT + cellY);
                prefs.apply();
            }
        }
    }

    protected ArrayList<ItemInfo> getNeedTitleUpdateIcons(ArrayList<ItemInfo> allItems) {
        ArrayList<ItemInfo> needUpdateItems;
        synchronized (sBgLock) {
            needUpdateItems = new ArrayList();
            Iterator it = allItems.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (!(info instanceof IconInfo) || ((IconInfo) info).intent == null || ((IconInfo) info).getTargetComponent() == null) {
                    if (info instanceof LauncherPairAppsInfo) {
                        ((LauncherPairAppsInfo) info).title = PairAppsUtilities.buildLabel(sContext, ((LauncherPairAppsInfo) info).mFirstApp.getCN(), ((LauncherPairAppsInfo) info).mSecondApp.getCN());
                        needUpdateItems.add(info);
                    }
                } else if ((info.itemType != 1 && info.itemType != 6) || ((IconInfo) info).isAppShortcut) {
                    Intent newIntent = new Intent(((IconInfo) info).intent.getAction(), null);
                    newIntent.addCategory("android.intent.category.LAUNCHER");
                    newIntent.setComponent(((IconInfo) info).getTargetComponent());
                    LauncherActivityInfoCompat lai = sLauncherApps.resolveActivity(newIntent, info.getUserHandle());
                    if (lai != null) {
                        ((IconInfo) info).title = Utilities.trim(sIconCache.getPackageItemTitle(lai));
                        ((IconInfo) info).contentDescription = sUserManager.getBadgedLabelForUser(((IconInfo) info).title, ((IconInfo) info).user);
                        needUpdateItems.add(info);
                    }
                }
            }
        }
        return needUpdateItems;
    }

    public int getBadgeCount(ComponentName cn, UserHandleCompat user) {
        return sBadgeCache.getBadgeCount(new CacheKey(cn, user));
    }

    protected synchronized void addAppToPendingPackages(String componentName, UserHandleCompat user) {
        HashSet<String> pkgs = (HashSet) sPendingPackages.get(user);
        if (pkgs == null) {
            pkgs = new HashSet();
            sPendingPackages.put(user, pkgs);
        }
        pkgs.add(componentName);
    }

    public void updateDeepShortcutMap(String packageName, UserHandleCompat user, List<ShortcutInfoCompat> shortcuts) {
        if (packageName != null) {
            Iterator<ComponentKey> keysIter = sBgDeepShortcutMap.keySet().iterator();
            while (keysIter.hasNext()) {
                ComponentKey next = (ComponentKey) keysIter.next();
                if (next.componentName.getPackageName().equals(packageName) && next.user.equals(user)) {
                    keysIter.remove();
                }
            }
        }
        for (ShortcutInfoCompat shortcut : shortcuts) {
            boolean shouldShowInContainer = shortcut.isEnabled() && (shortcut.isDeclaredInManifest() || shortcut.isDynamic());
            if (shouldShowInContainer) {
                sBgDeepShortcutMap.addToList(new ComponentKey(shortcut.getActivity(), shortcut.getUserHandle()), shortcut.getId());
            }
        }
    }

    public void bindDeepShortcuts() {
        final MultiHashMap<ComponentKey, String> shortcutMapCopy = sBgDeepShortcutMap.clone();
        runOnMainThread(new Runnable() {
            public void run() {
                Callbacks callbacks = DataLoader.sLauncherModel.getCallback();
                if (callbacks != null) {
                    callbacks.bindDeepShortcutMap(shortcutMapCopy);
                }
            }
        });
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void decrementPinnedShortcutCount(com.android.launcher3.common.quickoption.shortcuts.ShortcutKey r3) {
        /*
        r2 = sBgLock;
        monitor-enter(r2);
        r1 = sBgPinnedShortcutCounts;	 Catch:{ all -> 0x0022 }
        r0 = r1.get(r3);	 Catch:{ all -> 0x0022 }
        r0 = (android.util.MutableInt) r0;	 Catch:{ all -> 0x0022 }
        if (r0 == 0) goto L_0x0015;
    L_0x000d:
        r1 = r0.value;	 Catch:{ all -> 0x0022 }
        r1 = r1 + -1;
        r0.value = r1;	 Catch:{ all -> 0x0022 }
        if (r1 != 0) goto L_0x0020;
    L_0x0015:
        r1 = com.android.launcher3.LauncherAppState.getInstance();	 Catch:{ all -> 0x0022 }
        r1 = r1.getShortcutManager();	 Catch:{ all -> 0x0022 }
        r1.unpinShortcut(r3);	 Catch:{ all -> 0x0022 }
    L_0x0020:
        monitor-exit(r2);	 Catch:{ all -> 0x0022 }
        return;
    L_0x0022:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0022 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.DataLoader.decrementPinnedShortcutCount(com.android.launcher3.common.quickoption.shortcuts.ShortcutKey):void");
    }

    public static void incrementPinnedShortcutCount(ShortcutKey pinnedShortcut, boolean shouldPin) {
        synchronized (sBgLock) {
            MutableInt count = (MutableInt) sBgPinnedShortcutCounts.get(pinnedShortcut);
            if (count == null) {
                count = new MutableInt(1);
                sBgPinnedShortcutCounts.put(pinnedShortcut, count);
            } else {
                count.value++;
            }
            if (shouldPin && count.value == 1) {
                LauncherAppState.getInstance().getShortcutManager().pinShortcut(pinnedShortcut);
            }
        }
    }
}
