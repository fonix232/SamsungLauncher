package com.android.launcher3.allapps.model;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.PackageInstallerCompat.PackageInstallInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.OpenMarketCustomization;
import com.android.launcher3.common.customer.OpenMarketCustomization.ItemChangedListener;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.model.AutoInstallsLayout;
import com.android.launcher3.common.model.BadgeCache;
import com.android.launcher3.common.model.CursorInfo;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.DataLoader.DataLoaderState;
import com.android.launcher3.common.model.DataLoader.ItemInfoFilter;
import com.android.launcher3.common.model.DataUpdater;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.receiver.StartupReceiver;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.home.HomeLoader;
import com.android.launcher3.home.HomeLoader.AppsAvailabilityCheck;
import com.android.launcher3.home.ManagedProfileHeuristic;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.StringFilter;
import com.android.launcher3.util.logging.SALogging;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AppsModel extends DataLoader {
    private static final String TAG = "AppsModel";
    public static final int TASK_STATE_COMPLETE = 1;
    private static final int TASK_STATE_PREPARED = 0;
    private static final Object mPostPositionLock = new Object();
    private final boolean DEBUG_MODEL = Utilities.DEBUGGABLE();
    private final ArrayList<ComponentKey> mAllAppsComponentKey = new ArrayList();
    private final ArrayList<ModelListener> mListeners = new ArrayList();
    private LoadTask mLoadTask;
    private final ArrayList<PendingAddOrUpdateStruct> mPendingUpdateList = new ArrayList();
    private final ArrayList<ComponentKey> mRemovedComponents = new ArrayList();
    private boolean mUpdateLock = true;

    public interface ModelListener {
        void addItemToFolder(FolderInfo folderInfo, ArrayList<IconInfo> arrayList);

        void createFolderAndAddItem(FolderInfo folderInfo, ItemInfo itemInfo, ArrayList<IconInfo> arrayList);

        void notifyUpdate(ArrayList<ItemInfo> arrayList);

        void onLoadComplete(int i);

        void onLoadStart();

        void removeAllItems();

        void terminate();

        void updateIconAndTitle(ArrayList<ItemInfo> arrayList);

        void updateRestoreItems(HashSet<ItemInfo> hashSet);
    }

    private static class PendingAddOrUpdateStruct {
        final HashMap<ComponentName, IconInfo> mAddedOrUpdatedApps = new HashMap();
        final String[] mPackages;
        final UserHandleCompat mUser;

        PendingAddOrUpdateStruct(String[] packages, HashMap<ComponentName, IconInfo> addedOrUpdatedApps, UserHandleCompat user) {
            this.mPackages = packages;
            this.mAddedOrUpdatedApps.clear();
            this.mAddedOrUpdatedApps.putAll(addedOrUpdatedApps);
            this.mUser = user;
        }
    }

    private static class SyncContext {
        public boolean stopped;

        private SyncContext() {
            this.stopped = false;
        }

        public void reset() {
            this.stopped = false;
        }
    }

    private class LoadTask extends Thread implements DataLoaderState {
        private volatile boolean mActive;
        private volatile boolean mDirty;
        private final SyncContext mSyncContext;
        private volatile boolean mUpdateCompleted;

        private LoadTask() {
            this.mActive = true;
            this.mDirty = true;
            this.mUpdateCompleted = false;
            this.mSyncContext = new SyncContext();
        }

        public void run() {
            while (this.mActive) {
                synchronized (this) {
                    if (!this.mActive || ((this.mDirty || !this.mUpdateCompleted) && !this.mSyncContext.stopped)) {
                        Log.d(AppsModel.TAG, "start AppsLoadTask task");
                        this.mDirty = false;
                        this.mUpdateCompleted = false;
                        this.mSyncContext.reset();
                        AppsModel.this.mPendingUpdateList.clear();
                        if (!this.mSyncContext.stopped) {
                            AppsModel.this.notifyLoadStart();
                        }
                        AppsModel.this.loadAllAppsItemsFromDB(this.mSyncContext);
                        if (!this.mSyncContext.stopped) {
                            AppsModel.this.notifyLoadComplete(0);
                        }
                        AppsModel.this.loadRemainedApplications(this.mSyncContext);
                        if (!this.mSyncContext.stopped) {
                            synchronized (AppsModel.sBgLock) {
                                AppsModel.this.updateAppsPostPosition();
                            }
                            AppsModel.this.notifyLoadComplete(1);
                        }
                        if (!this.mSyncContext.stopped) {
                            this.mUpdateCompleted = true;
                            AppsModel.this.checkUpdate();
                        }
                    } else {
                        this.mDirty = false;
                        Log.d(AppsModel.TAG, "AppsLoadTask enter wait : " + this.mSyncContext.stopped);
                        AppsModel.waitWithoutInterrupt(this);
                    }
                }
            }
            Log.e(AppsModel.TAG, "LoadTask This run is expired");
        }

        private synchronized void notifyDirty() {
            this.mDirty = true;
            this.mUpdateCompleted = false;
            this.mSyncContext.reset();
            notifyAll();
        }

        private synchronized void terminate() {
            this.mActive = false;
            this.mSyncContext.stopped = true;
            notifyAll();
        }

        public boolean isStopped() {
            return !this.mActive;
        }

        private boolean isUpdateCompleted() {
            return this.mUpdateCompleted;
        }

        private void forceCompleteAndWait() {
            this.mUpdateCompleted = true;
            this.mDirty = false;
            this.mSyncContext.reset();
            start();
        }
    }

    public com.android.launcher3.common.base.item.ItemInfo getLocationInfoFromDB(com.android.launcher3.common.base.item.ItemInfo r13) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0023 in list [B:13:0x0060]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r12 = this;
        r5 = 0;
        r8 = new com.android.launcher3.common.base.item.ItemInfo;
        r8.<init>();
        r8.copyFrom(r13);
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r2 = 0;
        r3 = "(_id=?)";
        r0 = 1;
        r4 = new java.lang.String[r0];
        r0 = 0;
        r10 = r8.id;
        r9 = java.lang.String.valueOf(r10);
        r4[r0] = r9;
        r0 = sContentResolver;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 != 0) goto L_0x0024;
    L_0x0022:
        r8 = r5;
    L_0x0023:
        return r8;
    L_0x0024:
        r0 = r6.getCount();
        if (r0 > 0) goto L_0x002e;
    L_0x002a:
        r6.close();
        goto L_0x0023;
    L_0x002e:
        r7 = new com.android.launcher3.common.model.CursorInfo;
        r7.<init>(r6);
        r0 = r6.moveToFirst();	 Catch:{ all -> 0x0064 }
        if (r0 == 0) goto L_0x005a;	 Catch:{ all -> 0x0064 }
    L_0x0039:
        r0 = r7.screenIndex;	 Catch:{ all -> 0x0064 }
        r0 = r6.getInt(r0);	 Catch:{ all -> 0x0064 }
        r10 = (long) r0;	 Catch:{ all -> 0x0064 }
        r8.screenId = r10;	 Catch:{ all -> 0x0064 }
        r0 = r7.rankIndex;	 Catch:{ all -> 0x0064 }
        r0 = r6.getInt(r0);	 Catch:{ all -> 0x0064 }
        r8.rank = r0;	 Catch:{ all -> 0x0064 }
        r0 = r7.cellXIndex;	 Catch:{ all -> 0x0064 }
        r0 = r6.getInt(r0);	 Catch:{ all -> 0x0064 }
        r8.cellX = r0;	 Catch:{ all -> 0x0064 }
        r0 = r7.cellYIndex;	 Catch:{ all -> 0x0064 }
        r0 = r6.getInt(r0);	 Catch:{ all -> 0x0064 }
        r8.cellY = r0;	 Catch:{ all -> 0x0064 }
    L_0x005a:
        r0 = r6.isClosed();
        if (r0 != 0) goto L_0x0023;
    L_0x0060:
        r6.close();
        goto L_0x0023;
    L_0x0064:
        r0 = move-exception;
        r5 = r6.isClosed();
        if (r5 != 0) goto L_0x006e;
    L_0x006b:
        r6.close();
    L_0x006e:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.allapps.model.AppsModel.getLocationInfoFromDB(com.android.launcher3.common.base.item.ItemInfo):com.android.launcher3.common.base.item.ItemInfo");
    }

    public AppsModel(Context context, LauncherAppState app, LauncherModel model, IconCache cache, BadgeCache badgeCache) {
        init(context, app, model, cache, badgeCache, this);
        this.mFavoritesUpdater = new FavoritesUpdater(context, model, cache, this);
        OpenMarketCustomization.getInstance().setListener(new ItemChangedListener() {
            public void onItemChanged(final IconInfo item, final ContentValues values, final boolean isRemoved) {
                ArrayList<ItemInfo> updatedIcons = new ArrayList();
                Runnable updateRunnable = new Runnable() {
                    public void run() {
                        if (isRemoved) {
                            AppsModel.this.mFavoritesUpdater.deleteItem(item);
                        } else {
                            AppsModel.this.mFavoritesUpdater.updateItem(values, item);
                        }
                    }
                };
                updatedIcons.add(item);
                AppsModel.this.notifyUpdate(updatedIcons);
                DataLoader.runOnWorkerThread(updateRunnable);
            }
        }, true);
    }

    public void loadDefaultLayout(AutoInstallsLayout autoInstallLayout, boolean isSwitchedDb, boolean reloadPostPosition) {
        AppsDefaultLayoutParser appsDefaultLayoutParser;
        if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled() || reloadPostPosition) {
            appsDefaultLayoutParser = null;
            if (autoInstallLayout != null) {
                Log.d(TAG, "use auto install layout for apps");
                Resources res = autoInstallLayout.getResources();
                int appOrderResId = res.getIdentifier("default_application_order", "xml", autoInstallLayout.getPackageName());
                if (appOrderResId != 0) {
                    appsDefaultLayoutParser = new AppsDefaultLayoutParser(sContext, null, sFavoritesProvider, res, appOrderResId);
                } else {
                    Log.e(TAG, "default_application_order layout not found in package: " + autoInstallLayout.getPackageName());
                }
            }
            boolean usingExternalProvidedLayout = appsDefaultLayoutParser != null;
            if (appsDefaultLayoutParser == null) {
                AppsDefaultLayoutParser appsDefaultLayoutParser2 = new AppsDefaultLayoutParser(sContext, null, sFavoritesProvider, sContext.getResources(), 0);
            }
            appsDefaultLayoutParser.setReloadPostPosition(reloadPostPosition);
            if (sFavoritesProvider.loadAppsFavorites(appsDefaultLayoutParser) < 0 && usingExternalProvidedLayout) {
                sFavoritesProvider.loadAppsFavorites(new AppsDefaultLayoutParser(sContext, null, sFavoritesProvider, sContext.getResources(), 0));
                return;
            }
            return;
        }
        Log.d(TAG, "current mode is home only mode. skip loadDefaultLayout");
        appsDefaultLayoutParser = new AppsDefaultLayoutParser(sContext, null, null, sContext.getResources(), 0);
        ArrayList<String> tables = new ArrayList();
        tables.add("favorites");
        sFavoritesProvider.removeAndAddHiddenApp(appsDefaultLayoutParser, null, tables);
    }

    public void notifyDirty() {
        Log.d(TAG, "notifyDirty");
        updateLock(false);
        if (this.mLoadTask == null) {
            this.mLoadTask = new LoadTask();
            this.mLoadTask.start();
            return;
        }
        this.mLoadTask.notifyDirty();
    }

    public void bindItemsSync(int synchronousBindPage, DataLoaderState task) {
        if (this.mLoadTask == null) {
            this.mLoadTask = new LoadTask();
            this.mLoadTask.forceCompleteAndWait();
        }
        updateLock(false);
        notifyLoadComplete(1);
        Log.d(TAG, "bindItemSync : " + isStopped(task) + " , " + this.mPendingUpdateList.size());
    }

    public void setup(DataLoaderState task) {
        removeAllItems();
        clearSBgDataStructures();
        if (sContext != null) {
            mIsBootCompleted = Utilities.isBootCompleted();
            Log.i(TAG, "setup mIsBootCompleted = " + mIsBootCompleted);
        }
    }

    protected ArrayList<ItemInfo> loadPageItems(int rank, DataLoaderState task) {
        return null;
    }

    public void bindPageItems(ArrayList<ItemInfo> arrayList, ArrayList<Runnable> arrayList2, DataLoaderState task) {
    }

    public void filterCurrentPageItems(long currentScreenId, ArrayList<ItemInfo> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3) {
    }

    public void unbindItemsOnMainThread() {
    }

    protected void clearSBgDataStructures() {
        this.mAllAppsComponentKey.clear();
        this.mRemovedComponents.clear();
        synchronized (sBgLock) {
            Iterator it;
            ArrayList<ItemInfo> apps = getAllAppItemInApps();
            if (apps != null && apps.size() > 0) {
                it = apps.iterator();
                while (it.hasNext()) {
                    sBgItemsIdMap.remove(((ItemInfo) it.next()).id);
                }
            }
            ArrayList<Long> appFolderIds = new ArrayList();
            it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo f = (FolderInfo) it.next();
                if (f.isContainApps()) {
                    appFolderIds.add(Long.valueOf(f.id));
                }
            }
            if (appFolderIds.size() > 0) {
                Iterator it2 = appFolderIds.iterator();
                while (it2.hasNext()) {
                    sBgFolders.remove(((Long) it2.next()).longValue());
                }
            }
        }
    }

    protected DataUpdater getUpdater() {
        return null;
    }

    public void addPagesItem(ItemInfo item) {
        SALogging.getInstance().updateStatusLogValuesForAppsItem();
    }

    public void removePagesItem(ItemInfo item) {
        SALogging.getInstance().updateStatusLogValuesForAppsItem();
    }

    public void updatePagesItem(ItemInfo item) {
        SALogging.getInstance().updateStatusLogValuesForAppsItem();
    }

    public boolean containPagesItem(ItemInfo item) {
        return false;
    }

    public void setOrderedScreen(ArrayList<Long> arrayList) {
    }

    public void removeUnRestoredItems(boolean itemLoaded) {
        Runnable r = new Runnable() {
            public void run() {
                Log.d(AppsModel.TAG, "run removeUnRestoredItems");
                ArrayList<ItemInfo> removeItems = AppsModel.this.getUnRestoredItems(AppsModel.this.getAllAppItemInApps());
                if (!removeItems.isEmpty()) {
                    Iterator it = removeItems.iterator();
                    while (it.hasNext()) {
                        Log.d(AppsModel.TAG, "This item is not restored. remove : " + ((ItemInfo) it.next()).toString());
                    }
                    AppsModel.this.mFavoritesUpdater.deleteItemsFromDatabase(removeItems);
                }
                AppsModel.this.notifyUpdate(null);
            }
        };
        Log.d(TAG, "removeUnRestoredItems " + itemLoaded);
        if (itemLoaded) {
            r.run();
        } else {
            sWorkerHandler.post(r);
        }
    }

    public void updatePackageFlags(StringFilter pkgFilter, UserHandleCompat user, FlagOp flagOp) {
        ArrayList<ItemInfo> updatedIcons = new ArrayList();
        if (pkgFilter.matches("")) {
            this.mQuietModeUsers.put(sUserManager.getSerialNumberForUser(user), Boolean.valueOf(flagOp.apply(8) > 0));
        }
        synchronized (sBgLock) {
            Iterator it = getAllAppItemInApps().iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info instanceof IconInfo) {
                    IconInfo iconInfo = (IconInfo) info;
                    if (iconInfo.componentName != null && iconInfo.user.equals(user) && pkgFilter.matches(iconInfo.componentName.getPackageName())) {
                        if (sLauncherApps.getActivityList(iconInfo.componentName.getPackageName(), user).isEmpty()) {
                            Log.d(TAG, "updatePackageFlags remove item " + iconInfo.componentName);
                            this.mFavoritesUpdater.deleteItem(iconInfo);
                        } else {
                            iconInfo.isDisabled = flagOp.apply(iconInfo.isDisabled);
                            updatedIcons.add(iconInfo);
                        }
                    }
                }
            }
        }
        notifyUpdate(updatedIcons);
    }

    public void updateUnavailablePackage(String disabledPackages, UserHandleCompat user, int reason) {
        ArrayList<ItemInfo> items = getAllAppItemInApps();
        ArrayList<ItemInfo> unavailable = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item instanceof IconInfo) {
                IconInfo iconInfo = (IconInfo) item;
                ComponentName cn = iconInfo.getTargetComponent();
                if (cn != null && disabledPackages.equals(cn.getPackageName()) && user.equals(iconInfo.getUserHandle())) {
                    iconInfo.isDisabled |= reason;
                    Log.i(TAG, "updateUnavailablePackage unavailable = " + item);
                    unavailable.add(item);
                }
            }
        }
        notifyUpdate(unavailable);
    }

    public synchronized void titleUpdate() {
        ArrayList<ItemInfo> needUpdateItems = getNeedTitleUpdateIcons(getAllAppItemInApps());
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).updateIconAndTitle(needUpdateItems);
        }
    }

    public void setPackageState(final PackageInstallInfo installInfo) {
        if (installInfo != null && installInfo.packageName != null) {
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    Log.d(AppsModel.TAG, "setPackageState");
                    HashSet<ItemInfo> updates = new HashSet();
                    if (installInfo.state != 0) {
                        Iterator it = AppsModel.this.getAllAppItemInApps().iterator();
                        while (it.hasNext()) {
                            ItemInfo info = (ItemInfo) it.next();
                            if (info instanceof IconInfo) {
                                IconInfo iconInfo = (IconInfo) info;
                                ComponentName cn = iconInfo.getTargetComponent();
                                if (iconInfo.isPromise() && cn != null && installInfo.packageName.equals(cn.getPackageName())) {
                                    iconInfo.setInstallProgress(installInfo.progress);
                                    if (installInfo.state == 2) {
                                        iconInfo.status &= -9;
                                    }
                                    updates.add(iconInfo);
                                }
                            }
                        }
                        if (!updates.isEmpty()) {
                            AppsModel.this.updateRestoreItems(updates);
                        }
                    }
                }
            });
        }
    }

    public void updateSessionDisplayInfo(final String packageName) {
        if (packageName != null) {
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    Log.d(AppsModel.TAG, "updateSessionDisplayInfo " + packageName);
                    ArrayList<ItemInfo> updates = new ArrayList();
                    UserHandleCompat user = UserHandleCompat.myUserHandle();
                    Iterator it = AppsModel.this.getAllAppItemInApps().iterator();
                    while (it.hasNext()) {
                        ItemInfo info = (ItemInfo) it.next();
                        if (info instanceof IconInfo) {
                            IconInfo iconInfo = (IconInfo) info;
                            ComponentName cn = iconInfo.getTargetComponent();
                            if (iconInfo.isPromise() && cn != null && packageName.equals(cn.getPackageName())) {
                                if (iconInfo.hasStatusFlag(2)) {
                                    AppsModel.sIconCache.getTitleAndIcon(iconInfo, iconInfo.promisedIntent, user, iconInfo.shouldUseLowResIcon());
                                } else if (!iconInfo.hasStatusFlag(36)) {
                                    iconInfo.updateIcon(AppsModel.sIconCache);
                                }
                                updates.add(iconInfo);
                            }
                        }
                    }
                    AppsModel.this.notifyUpdate(updates);
                }
            });
        }
    }

    private void loadAllAppsItemsFromDB(SyncContext syncContext) {
        ArrayList<Long> restoredRows = new ArrayList();
        Uri contentUri = Favorites.CONTENT_URI;
        String[] selectionArg = new String[]{String.valueOf(Favorites.CONTAINER_APPS)};
        Cursor c = sContentResolver.query(contentUri, null, "(container=?)", selectionArg, "screen ASC, rank ASC");
        if (c != null) {
            if (c.getCount() == 0) {
                c.close();
                return;
            }
            CursorInfo cursorInfo = new CursorInfo(c);
            LongArrayMap<ItemInfo> folders = new LongArrayMap();
            ArrayList<IconInfo> items = new ArrayList();
            try {
                createItems(c, cursorInfo, restoredRows, folders, items, syncContext);
                if (folders.size() > 0) {
                    c = sContentResolver.query(contentUri, null, "container in (" + makeFoldersIdToString(folders) + ')', null, "rank ASC");
                    try {
                        createItemsInFolder(c, cursorInfo, restoredRows, items, syncContext);
                    } finally {
                        if (!(c == null || c.isClosed())) {
                            c.close();
                        }
                    }
                }
                if (restoredRows.size() > 0) {
                    ContentValues values = new ContentValues();
                    values.put("restored", Integer.valueOf(0));
                    sContentResolver.update(Favorites.CONTENT_URI, values, Utilities.createDbSelectionQuery("_id", restoredRows), null);
                }
                if (!syncContext.stopped) {
                    Iterator it = items.iterator();
                    while (it.hasNext()) {
                        ItemInfo item = (ItemInfo) it.next();
                        if (this.DEBUG_MODEL) {
                            Log.d(TAG, "loadAllAppsItemsFromDB putToMap item=" + item);
                        }
                        if (!syncContext.stopped) {
                            if (item.container != -102) {
                                FolderInfo folderInfo;
                                synchronized (sBgLock) {
                                    folderInfo = (FolderInfo) sBgFolders.get(item.container);
                                }
                                if (folderInfo != null && item.hidden == 0) {
                                    folderInfo.add((IconInfo) item);
                                }
                            }
                            putItemToIdMap(item);
                        } else {
                            return;
                        }
                    }
                }
            } finally {
                if (!c.isClosed()) {
                    c.close();
                }
            }
        }
    }

    private void loadRemainedApplications(SyncContext syncContext) {
        long loadTime = this.DEBUG_MODEL ? SystemClock.uptimeMillis() : 0;
        ArrayList<ItemInfo> allRemainedItems = new ArrayList();
        List<UserHandleCompat> profiles = sUserManager.getUserProfiles();
        PostPositionController pp = PostPositionController.getInstance(sContext);
        for (UserHandleCompat user : profiles) {
            long qiaTime = this.DEBUG_MODEL ? SystemClock.uptimeMillis() : 0;
            final List<LauncherActivityInfoCompat> apps = sLauncherApps.getActivityList(null, user);
            if (apps == null || apps.isEmpty()) {
                Log.e(TAG, "There is no getActivityList apps for user " + user);
            } else {
                if (this.DEBUG_MODEL) {
                    Log.d(TAG, "getActivityList took " + (SystemClock.uptimeMillis() - qiaTime) + "ms for user " + user);
                    Log.d(TAG, "getActivityList got " + apps.size() + " apps for user " + user);
                }
                for (int i = 0; i < apps.size() && !syncContext.stopped; i++) {
                    IconInfo info = createAppInfoIfNecessary((LauncherActivityInfoCompat) apps.get(i), user);
                    if (!(info == null || sFavoritesProvider == null)) {
                        info.id = sFavoritesProvider.generateNewItemId();
                        long folderId = pp.getAppsAutoFolderId(info.componentName.getPackageName());
                        if (folderId != -1) {
                            FolderInfo folder;
                            synchronized (sBgLock) {
                                folder = (FolderInfo) sBgFolders.get(folderId);
                            }
                            if (folder != null) {
                                info.container = folderId;
                            }
                        }
                        allRemainedItems.add(info);
                        this.mAllAppsComponentKey.add(new ComponentKey(info.componentName, info.user));
                    }
                }
                if (!syncContext.stopped) {
                    final ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(sContext, user);
                    if (heuristic != null) {
                        final Runnable anonymousClass5 = new Runnable() {
                            public void run() {
                                heuristic.processUserApps(apps);
                            }
                        };
                        runOnMainThread(new Runnable() {
                            public void run() {
                                if (AppsModel.sIsLoadingAndBindingWorkspace) {
                                    synchronized (AppsModel.sBindCompleteRunnables) {
                                        AppsModel.sBindCompleteRunnables.add(anonymousClass5);
                                    }
                                    return;
                                }
                                DataLoader.runOnWorkerThread(anonymousClass5);
                            }
                        });
                    }
                    if (this.DEBUG_MODEL) {
                        Log.d(TAG, "Icons processed in " + (SystemClock.uptimeMillis() - loadTime) + "ms");
                    }
                    ManagedProfileHeuristic.processAllUsers(profiles, sContext);
                    if (!syncContext.stopped) {
                        this.mFavoritesUpdater.addItems(allRemainedItems, false);
                    }
                    Iterator it = allRemainedItems.iterator();
                    while (it.hasNext()) {
                        ItemInfo info2 = (ItemInfo) it.next();
                        if (syncContext.stopped) {
                            break;
                        }
                        putItemToIdMap(info2);
                    }
                    allRemainedItems.clear();
                } else {
                    return;
                }
            }
        }
    }

    private void createItems(Cursor c, CursorInfo cursorInfo, ArrayList<Long> restoredRows, LongArrayMap<ItemInfo> folders, ArrayList<IconInfo> items, SyncContext syncContext) {
        ArrayList<Long> itemsToRemove = new ArrayList();
        while (!syncContext.stopped && c.moveToNext()) {
            try {
                if (c.getString(cursorInfo.intentIndex) == null) {
                    ItemInfo folderInfo = createFolderInfo(c, cursorInfo, restoredRows);
                    if (folderInfo != null) {
                        folders.put(folderInfo.id, folderInfo);
                    }
                } else {
                    IconInfo info = createIconInfo(c, cursorInfo, restoredRows);
                    if (info == null) {
                        ComponentName cn = null;
                        try {
                            Intent intent = Intent.parseUri(c.getString(cursorInfo.intentIndex), 0);
                            if (intent != null) {
                                cn = intent.getComponent();
                            }
                        } catch (URISyntaxException e) {
                            Log.d(TAG, "URISyntaxException in createItems");
                        }
                        UserHandleCompat user = (UserHandleCompat) this.mAllUsers.get((long) c.getInt(cursorInfo.profileIdIndex));
                        if (!(cn == null || user == null)) {
                            this.mRemovedComponents.add(new ComponentKey(cn, user));
                        }
                        itemsToRemove.add(Long.valueOf(c.getLong(cursorInfo.idIndex)));
                    } else {
                        items.add(info);
                    }
                }
            } catch (Exception e2) {
                Launcher.addDumpLog(TAG, "Apps items loading interrupted", e2, true);
            }
        }
        removeItems(itemsToRemove);
    }

    private ItemInfo createFolderInfo(Cursor c, CursorInfo cursorInfo, ArrayList<Long> restoredRows) {
        FolderInfo folderInfo;
        long id = c.getLong(cursorInfo.idIndex);
        synchronized (sBgLock) {
            folderInfo = (FolderInfo) sBgFolders.get(id);
        }
        if (folderInfo == null) {
            folderInfo = new FolderInfo();
        }
        boolean restored = c.getInt(cursorInfo.restoredIndex) != 0;
        folderInfo.title = c.getString(cursorInfo.titleIndex);
        folderInfo.id = id;
        folderInfo.container = (long) c.getInt(cursorInfo.containerIndex);
        folderInfo.screenId = (long) c.getInt(cursorInfo.screenIndex);
        folderInfo.itemType = c.getInt(cursorInfo.itemTypeIndex);
        folderInfo.rank = c.getInt(cursorInfo.rankIndex);
        folderInfo.color = c.getInt(cursorInfo.colorIndex);
        folderInfo.lock = c.getInt(cursorInfo.lockIndex);
        folderInfo.options = c.getInt(cursorInfo.optionsIndex);
        if (restored) {
            restoredRows.add(Long.valueOf(id));
        }
        putItemToIdMap(folderInfo);
        return folderInfo;
    }

    private void createItemsInFolder(Cursor c, CursorInfo cursorInfo, ArrayList<Long> restoredRows, ArrayList<IconInfo> items, SyncContext syncContext) {
        ArrayList<Long> itemsToRemove = new ArrayList();
        while (!syncContext.stopped && c.moveToNext()) {
            try {
                IconInfo info = createIconInfo(c, cursorInfo, restoredRows);
                if (info == null) {
                    itemsToRemove.add(Long.valueOf(c.getLong(cursorInfo.idIndex)));
                } else {
                    items.add(info);
                }
            } catch (Exception e) {
                Launcher.addDumpLog(TAG, "Apps items loading interrupted", e, true);
            }
        }
        removeItems(itemsToRemove);
        sFavoritesProvider.deleteInvalidFolders(items);
    }

    private IconInfo createIconInfo(Cursor c, CursorInfo cursorInfo, ArrayList<Long> restoredRows) {
        int container = c.getInt(cursorInfo.containerIndex);
        long id = (long) c.getInt(cursorInfo.idIndex);
        long serialNumber = (long) c.getInt(cursorInfo.profileIdIndex);
        int promiseType = c.getInt(cursorInfo.restoredIndex);
        boolean restored = promiseType != 0;
        try {
            Intent intent = Intent.parseUri(c.getString(cursorInfo.intentIndex), 0);
            ComponentName cn = intent.getComponent();
            UserHandleCompat user = (UserHandleCompat) this.mAllUsers.get(serialNumber);
            if (user == null) {
                return null;
            }
            if (this.mAllAppsComponentKey.contains(new ComponentKey(cn, user))) {
                Log.e(TAG, " createIconInfo fail- already exist:" + cn + " user:" + user);
                return null;
            }
            IconInfo appInfo;
            boolean validPkg = true;
            boolean validComponent = true;
            boolean allowMissingTarget = false;
            int disabledState = 0;
            List<LauncherActivityInfoCompat> apps = sLauncherApps.getActivityList(cn.getPackageName(), user);
            if (apps == null || apps.isEmpty()) {
                validPkg = false;
            }
            LauncherActivityInfoCompat activityInfo = validPkg ? DataLoader.findActivityInfo(apps, cn, user) : null;
            if (activityInfo == null) {
                validComponent = false;
            }
            ContentValues values;
            if (validComponent) {
                if (restored) {
                    if ((promiseType & 32) != 0) {
                        values = new ContentValues();
                        values.put("title", "");
                        values.put("icon", (byte[]) null);
                        updateItem(id, values);
                    }
                    restoredRows.add(Long.valueOf(id));
                    restored = false;
                }
                if (((Boolean) this.mQuietModeUsers.get(serialNumber)).booleanValue()) {
                    disabledState = 8;
                }
            } else if (validPkg) {
                intent = null;
                if (!((promiseType & 2) == 0 && (promiseType & 32) == 0)) {
                    intent = sPackageManager.getLaunchIntentForPackage(cn.getPackageName());
                    if (intent != null) {
                        values = new ContentValues();
                        values.put("intent", intent.toUri(0));
                        updateItem(id, values);
                    }
                }
                if (intent == null) {
                    Launcher.addDumpLog(TAG, "Invalid component removed: " + cn, true);
                    return null;
                }
                if ((promiseType & 32) != 0) {
                    values = new ContentValues();
                    values.put("title", "");
                    values.put("icon", (byte[]) null);
                    updateItem(id, values);
                }
                restoredRows.add(Long.valueOf(id));
                restored = false;
            } else if (restored) {
                Launcher.addDumpLog(TAG, "package not yet restored: " + cn, true);
                if (!((promiseType & 2) == 0 && (promiseType & 1) == 0 && (promiseType & 64) == 0) && (sInstallingPkgs == null || !sInstallingPkgs.containsKey(cn.getPackageName()))) {
                    Launcher.addDumpLog(TAG, "UnRestored package removed: " + cn, true);
                    return null;
                }
            } else {
                int notAvailableType = isNotAvailableApps(cn.getPackageName());
                if (notAvailableType > 0) {
                    Launcher.addDumpLog(TAG, "Package unavailable  : " + cn + " by " + notAvailableType, true);
                    disabledState = 0 | notAvailableType;
                    allowMissingTarget = true;
                } else if (mIsBootCompleted) {
                    Launcher.addDumpLog(TAG, "Invalid package removed: " + cn, true);
                    return null;
                } else {
                    Launcher.addDumpLog(TAG, "Missing pkg(before boot complete), will check later : " + cn, true);
                    addAppToPendingPackages(cn.flattenToString(), user);
                    allowMissingTarget = true;
                }
            }
            if (restored) {
                if (!user.equals(UserHandleCompat.myUserHandle())) {
                    return null;
                }
                Launcher.addDumpLog(TAG, "constructing info for restored package promiseType: " + promiseType, true);
                appInfo = getRestoredItemInfo(c, cn, intent, promiseType, cursorInfo);
                if (appInfo == null) {
                    Launcher.addDumpLog(TAG, "restore item's info is null", true);
                    return null;
                }
                appInfo.componentName = cn;
                appInfo.intent = getRestoredItemIntent(intent, promiseType);
            } else if (!allowMissingTarget) {
                appInfo = new IconInfo(sContext, activityInfo, user, sIconCache);
            } else if ((disabledState & 32) != 0) {
                appInfo = getDisabledExternalItemInfo(c, cn, intent, cursorInfo);
            } else {
                Log.i(TAG, "missingTarget cn=" + cn);
                appInfo = getDisabledItemInfo(c, cn, intent, cursorInfo);
            }
            if (appInfo != null) {
                appInfo.container = (long) container;
                appInfo.screenId = (long) c.getInt(cursorInfo.screenIndex);
                appInfo.rank = c.getInt(cursorInfo.rankIndex);
                appInfo.id = id;
                appInfo.hidden = c.getInt(cursorInfo.hiddenIndex);
                appInfo.isDisabled |= disabledState;
                if (restored) {
                    cn = appInfo.getTargetComponent();
                    if (cn != null) {
                        Integer progress = sInstallingPkgs == null ? null : (Integer) sInstallingPkgs.get(cn.getPackageName());
                        if (progress != null) {
                            appInfo.setInstallProgress(progress.intValue());
                        } else {
                            appInfo.status &= -9;
                        }
                    }
                }
            }
            if (appInfo == null) {
                return appInfo;
            }
            if (this.DEBUG_MODEL) {
                Log.i(TAG, "===> createIconInfo appInfo=" + appInfo);
            }
            this.mAllAppsComponentKey.add(new ComponentKey(appInfo.componentName, appInfo.user));
            setBadgeCount(appInfo);
            return appInfo;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private IconInfo createAppInfoIfNecessary(LauncherActivityInfoCompat info, UserHandleCompat user) {
        IconInfo iconInfo = null;
        if (!this.mAllAppsComponentKey.contains(new ComponentKey(info.getComponentName(), user))) {
            iconInfo = new IconInfo(sContext, info, user, sIconCache);
            iconInfo.container = -102;
            iconInfo.mDirty = false;
            iconInfo.screenId = -1;
            iconInfo.rank = -1;
            if (((Boolean) this.mQuietModeUsers.get(sUserManager.getSerialNumberForUser(user))).booleanValue()) {
                iconInfo.isDisabled |= 8;
            }
            if (this.DEBUG_MODEL) {
                Log.d(TAG, " createAppInfoIfNecessary =" + iconInfo);
            }
        }
        return iconInfo;
    }

    private IconInfo getDisabledExternalItemInfo(Cursor c, ComponentName cn, Intent intent, CursorInfo cursorItemInfo) {
        IconInfo info = new IconInfo();
        info.user = UserHandleCompat.myUserHandle();
        if (!Utilities.isSystemApp(sContext, intent)) {
            info.flags = 1;
        }
        info.setIcon(BitmapUtils.createIconBitmap(LauncherAppState.getInstance().getIconCache().getSDCardBitmap(), sContext));
        if (c != null) {
            info.title = Utilities.trim(c.getString(cursorItemInfo.titleIndex));
        }
        if (info.title == null) {
            info.title = cn.getClassName();
        }
        if (info.title == null) {
            info.title = "";
        }
        info.intent = intent;
        info.componentName = cn;
        info.contentDescription = sUserManager.getBadgedLabelForUser(info.title, info.user);
        if (c != null) {
            info.itemType = c.getInt(cursorItemInfo.itemTypeIndex);
        }
        return info;
    }

    private IconInfo getDisabledItemInfo(Cursor c, ComponentName cn, Intent intent, CursorInfo cursorInfo) {
        IconInfo info = new IconInfo();
        info.user = UserHandleCompat.myUserHandle();
        Bitmap icon = cursorInfo.loadIcon(c, info, sContext);
        if (!Utilities.isSystemApp(sContext, intent)) {
            info.flags = 1;
            if (sIsSafeMode) {
                info.isDisabled |= 1;
            }
        }
        if (icon == null) {
            sIconCache.getTitleAndIcon(info, intent, info.user, false);
        } else {
            info.setIcon(icon);
        }
        info.title = Utilities.trim(c.getString(cursorInfo.titleIndex));
        info.isDisabled = 2;
        info.intent = intent;
        info.componentName = cn;
        info.contentDescription = sUserManager.getBadgedLabelForUser(info.title, info.user);
        info.itemType = c.getInt(cursorInfo.itemTypeIndex);
        return info;
    }

    private void removeItems(ArrayList<Long> ids) {
        if (ids.size() > 0) {
            sContentResolver.delete(Favorites.CONTENT_URI, Utilities.createDbSelectionQuery("_id", ids), null);
            Log.d(TAG, "Removed = " + Utilities.createDbSelectionQuery("_id", ids));
            PostPositionController pp = PostPositionController.getInstance(sContext);
            for (Long longValue : sFavoritesProvider.deleteEmptyFolders()) {
                long folderId = longValue.longValue();
                pp.removeAutoFolderInfo(folderId);
                pp.deleteFolder(folderId);
                synchronized (sBgLock) {
                    sBgFolders.remove(folderId);
                    sBgItemsIdMap.remove(folderId);
                }
            }
            if (pp.isEnabled()) {
                pp.deleteItems(ids);
            }
        }
    }

    protected void putItemToIdMap(ItemInfo info) {
        super.putItemToIdMap(info);
        if (!(info instanceof FolderInfo)) {
            ComponentKey key = new ComponentKey(info.componentName, info.user);
            if (!this.mAllAppsComponentKey.contains(key)) {
                this.mAllAppsComponentKey.add(key);
            }
        }
    }

    public synchronized void addModelListener(ModelListener listener) {
        if (!this.mListeners.contains(listener)) {
            ArrayList<ModelListener> duplicatedListener = new ArrayList();
            Iterator it = this.mListeners.iterator();
            while (it.hasNext()) {
                ModelListener l = (ModelListener) it.next();
                if (l.getClass() == listener.getClass()) {
                    Log.i(TAG, "This class has old ModelListener, so we should remove the old ModelListener of this class");
                    duplicatedListener.add(l);
                }
            }
            if (duplicatedListener.size() > 0) {
                Log.e(TAG, "Remove old class");
                this.mListeners.removeAll(duplicatedListener);
                it = duplicatedListener.iterator();
                while (it.hasNext()) {
                    ((ModelListener) it.next()).terminate();
                }
            }
            Log.i(TAG, "Add new model listener : " + listener);
            this.mListeners.add(listener);
        }
    }

    public synchronized void removeModelListener(ModelListener listener) {
        if (this.mListeners.contains(listener)) {
            Log.i(TAG, "Remove model listener : " + listener);
            this.mListeners.remove(listener);
        }
    }

    private synchronized void notifyLoadComplete(int taskState) {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).onLoadComplete(taskState);
        }
    }

    private synchronized void notifyUpdate(ArrayList<ItemInfo> updated) {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).notifyUpdate(updated);
        }
    }

    private synchronized void notifyLoadStart() {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).onLoadStart();
        }
    }

    private synchronized void removeAllItems() {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).removeAllItems();
        }
    }

    private synchronized void updateRestoreItems(HashSet<ItemInfo> updates) {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).updateRestoreItems(updates);
        }
    }

    public void setLoaderTaskStop(boolean isStopped) {
        if (isStopped) {
            Log.i(TAG, "setLoaderTaskStop oldTask=" + this.mLoadTask);
            if (this.mLoadTask != null) {
                this.mLoadTask.terminate();
                this.mLoadTask = null;
            }
        }
    }

    public void updateLock(boolean updateLock) {
        this.mUpdateLock = updateLock;
    }

    public boolean isUpdateLocked() {
        return this.mUpdateLock;
    }

    private static void waitWithoutInterrupt(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            Log.e(TAG, "waitWithoutInterrupt InterruptedException e=" + e.toString());
        }
    }

    public ArrayList<ItemInfo> getTopLevelItemsInApps() {
        ArrayList<ItemInfo> apps = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.container == -102 && info.hidden == 0) {
                    apps.add(info);
                }
            }
        }
        return apps;
    }

    public ArrayList<ItemInfo> getAllAppItemInApps() {
        ArrayList<ItemInfo> apps = new ArrayList();
        ArrayList<Long> folderIds = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it.next();
                if (folderInfo.container == -102) {
                    folderIds.add(Long.valueOf(folderInfo.id));
                }
            }
            it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo itemInfo = (ItemInfo) it.next();
                if (itemInfo.itemType == 0 && (itemInfo.container == -102 || folderIds.contains(Long.valueOf(itemInfo.container)))) {
                    apps.add(itemInfo);
                }
            }
        }
        return apps;
    }

    public FolderInfo findFolderById(Long folderId) {
        FolderInfo folderInfo;
        synchronized (sBgLock) {
            folderInfo = (FolderInfo) sBgFolders.get(folderId.longValue());
        }
        return folderInfo;
    }

    public ItemInfo getItemById(long key) {
        ItemInfo itemInfo;
        synchronized (sBgLock) {
            itemInfo = (ItemInfo) sBgItemsIdMap.get(key);
        }
        return itemInfo;
    }

    public ItemInfo getItemInfoInAppsForComponentName(final ComponentName cName, final UserHandleCompat user, boolean exceptWidget) {
        ItemInfo itemInfo;
        ItemInfoFilter filter = new ItemInfoFilter() {
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (info.user == null) {
                    return cn.equals(cName);
                }
                return cn.equals(cName) && info.user.equals(user);
            }
        };
        synchronized (sBgLock) {
            ArrayList<ItemInfo> appInfos = DataLoader.filterItemInfo(getAllAppItemInApps(), filter, exceptWidget);
            if (appInfos.size() > 1) {
                Iterator it = appInfos.iterator();
                while (it.hasNext()) {
                    Log.e(TAG, "duplicate app info : " + ((ItemInfo) it.next()));
                }
                throw new RuntimeException("Duplicated app icons in Apps");
            } else if (appInfos.isEmpty()) {
                itemInfo = null;
            } else {
                itemInfo = (ItemInfo) appInfos.get(0);
            }
        }
        return itemInfo;
    }

    public long addItem(ItemInfo info) {
        return this.mFavoritesUpdater.addItem(info);
    }

    public void updateItem(ContentValues value, ItemInfo info) {
        this.mFavoritesUpdater.updateItem(value, info);
    }

    public void updateItemsInDatabaseHelper(Context context, ArrayList<ContentValues> valueList, ArrayList<ItemInfo> items) {
        this.mFavoritesUpdater.updateItemsInDatabaseHelper(context, valueList, items);
    }

    public void deleteItem(ItemInfo info) {
        this.mFavoritesUpdater.deleteItem(info);
    }

    public void updateIconsAndLabels(HashSet<String> packages, UserHandleCompat user) {
        ArrayList<ItemInfo> items = new ArrayList();
        Iterator it = getAllAppItemInApps().iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            IconInfo iconInfo = (IconInfo) info;
            ComponentName componentName = iconInfo.getTargetComponent();
            if (componentName == null) {
                Log.w(TAG, "updateIconsAndLabels componentName is not exist : " + info);
            }
            if (componentName != null && iconInfo.user.equals(user) && packages.contains(componentName.getPackageName())) {
                sIconCache.updateTitleAndIcon(iconInfo);
                items.add(iconInfo);
            }
        }
        it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).updateIconAndTitle(items);
        }
    }

    public void addOrUpdater(String[] packages, HashMap<ComponentName, IconInfo> addedOrUpdatedApps, UserHandleCompat user) {
        if (this.mLoadTask == null || !this.mLoadTask.isUpdateCompleted()) {
            Log.e(TAG, "LoaderTask is in running, so this item will be added in loader task : " + this.mLoadTask);
            this.mPendingUpdateList.add(new PendingAddOrUpdateStruct(packages, addedOrUpdatedApps, user));
            return;
        }
        ComponentName cn;
        Log.i(TAG, "addOrUpdater");
        synchronized (sBgLock) {
            Iterator it = getAllAppItemInApps().iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info instanceof IconInfo) {
                    if (user.equals(info.user)) {
                        IconInfo iconInfo = (IconInfo) info;
                        cn = iconInfo.getTargetComponent();
                        if (cn != null) {
                            IconInfo appInfo = (IconInfo) addedOrUpdatedApps.get(cn);
                            if (appInfo != null && appInfo.isPromise()) {
                                LauncherActivityInfoCompat activityInfo = DataLoader.findActivityInfo(sLauncherApps.getActivityList(cn.getPackageName(), user), cn, user);
                                if (iconInfo.hasStatusFlag(102)) {
                                    PackageManager pm = sContext.getPackageManager();
                                    if (pm.resolveActivity(new Intent("android.intent.action.MAIN").setComponent(cn).addCategory("android.intent.category.LAUNCHER"), 65536) == null) {
                                        Intent intent = pm.getLaunchIntentForPackage(cn.getPackageName());
                                        if (intent != null) {
                                            iconInfo.promisedIntent = intent;
                                        }
                                    }
                                    if (activityInfo != null) {
                                        iconInfo.flags = IconInfo.initFlags(activityInfo);
                                        iconInfo.firstInstallTime = activityInfo.getFirstInstallTime();
                                    }
                                    iconInfo.intent = iconInfo.promisedIntent;
                                    iconInfo.promisedIntent = null;
                                    iconInfo.status = 0;
                                    iconInfo.updateIcon(sIconCache);
                                }
                            }
                            if (appInfo != null) {
                                iconInfo.contentDescription = appInfo.contentDescription;
                            }
                        }
                    }
                }
            }
        }
        ArrayList<ItemInfo> items = new ArrayList();
        for (ComponentName cn2 : addedOrUpdatedApps.keySet()) {
            String str = TAG;
            String str2 = str;
            Log.i(str2, "addOrUpdater cn = " + cn2 + " , " + ((IconInfo) addedOrUpdatedApps.get(cn2)).user);
            info = getItemInfoInAppsForComponentName(cn2, ((IconInfo) addedOrUpdatedApps.get(cn2)).user, false);
            if (info == null) {
                if (this.mAllAppsComponentKey.contains(new ComponentKey(cn2, user))) {
                    Log.e(TAG, "This item will be added in loader task. so we skip : " + cn2);
                } else {
                    info = (ItemInfo) addedOrUpdatedApps.get(cn2);
                    info.container = -102;
                    info.mDirty = true;
                    info.id = sFavoritesProvider.generateNewItemId();
                    info.screenId = -1;
                    info.rank = -1;
                    putItemToIdMap(info);
                    this.mFavoritesUpdater.addItem(info);
                    updateAppsPostPosition();
                }
            }
            if (info instanceof IconInfo) {
                ((IconInfo) info).updateIcon(sIconCache);
            }
            Log.i(TAG, "addOrUpdater update item = " + info);
            items.add(info);
        }
        notifyUpdate(items);
    }

    public void removePackagesAndComponents(ArrayList<ItemInfo> removedApps, UserHandleCompat user) {
        Iterator it = removedApps.iterator();
        while (it.hasNext()) {
            ItemInfo removedApp = (ItemInfo) it.next();
            if (this.mAllAppsComponentKey.remove(new ComponentKey(removedApp.componentName, user))) {
                Log.i(TAG, "Removed in mAllAppsComponentKey : " + removedApp);
            } else {
                Log.e(TAG, "This app is already removed in mAllAppsComponentKey : " + removedApp);
            }
        }
        notifyUpdate(removedApps);
    }

    public void hideApps(ArrayList<ItemInfo> items, boolean isGameApp) {
        if (items != null && items.size() > 0) {
            ArrayList<ContentValues> contentValues = new ArrayList();
            Iterator it = items.iterator();
            while (it.hasNext()) {
                ItemInfo item = (ItemInfo) it.next();
                item.hidden = isGameApp ? item.setHidden(4) : item.setHidden(2);
                ContentValues values = new ContentValues();
                values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
                values.put("hidden", Integer.valueOf(item.hidden));
                values.put("screen", Integer.valueOf(-1));
                values.put(BaseLauncherColumns.RANK, Integer.valueOf(-1));
                contentValues.add(values);
            }
            this.mFavoritesUpdater.updateItemsInDatabaseHelper(sContext, contentValues, items);
            notifyUpdate(items);
        }
    }

    public void showApps(ArrayList<ItemInfo> items, boolean isGameApp) {
        if (items != null && items.size() > 0) {
            ArrayList<ContentValues> contentValues = new ArrayList();
            Iterator it = items.iterator();
            while (it.hasNext()) {
                int unHidden;
                ItemInfo item = (ItemInfo) it.next();
                if (isGameApp) {
                    unHidden = item.setUnHidden(4);
                } else {
                    unHidden = item.setUnHidden(2);
                }
                item.hidden = unHidden;
                item.rank = -1;
                item.screenId = -1;
                item.container = -102;
                putItemToIdMap(item);
                ContentValues values = new ContentValues();
                values.put("hidden", Integer.valueOf(item.hidden));
                values.put("screen", Long.valueOf(item.screenId));
                values.put(BaseLauncherColumns.RANK, Integer.valueOf(item.rank));
                values.put("container", Long.valueOf(item.container));
                contentValues.add(values);
            }
            this.mFavoritesUpdater.updateItemsInDatabaseHelper(sContext, contentValues, items);
            notifyUpdate(items);
        }
    }

    public boolean addItemToFolder(LauncherActivityInfoCompat info, UserHandleCompat user, long folderId) {
        FolderInfo folderInfo = findFolderById(Long.valueOf(folderId));
        Log.d(TAG, "addItemToFolder() - " + folderInfo);
        if (folderInfo != null) {
            Log.d(TAG, "addItemToFolder() - " + folderInfo.title);
            IconInfo item = (IconInfo) getItemInfoInAppsForComponentName(info.getComponentName(), user, true);
            if (item != null) {
                Log.d(TAG, "folder ID = " + folderInfo.id + " , appItem container = " + item.container + ", id : " + item.id);
                if (item.container == -102) {
                    ArrayList<IconInfo> addItems = new ArrayList();
                    addItems.add(item);
                    addToFolderItem(folderInfo, addItems);
                    return true;
                }
                Log.w(TAG, "app is already exist in folder");
                return true;
            }
            Log.d(TAG, "no app to add folder : " + info.getComponentName());
        }
        return false;
    }

    private void addToFolderItem(FolderInfo folder, ArrayList<IconInfo> items) {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).addItemToFolder(folder, items);
        }
    }

    private void createFolderAndAddItem(FolderInfo folderInfo, ItemInfo targetItem, ArrayList<IconInfo> infos) {
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((ModelListener) it.next()).createFolderAndAddItem(folderInfo, targetItem, infos);
        }
    }

    public long createFolderAndAddItem(ItemInfo appItem, String folderTitle, LauncherActivityInfoCompat info) {
        if (appItem == null) {
            return -1;
        }
        FolderInfo fItem = new FolderInfo();
        try {
            fItem.id = FavoritesProvider.getInstance().generateNewItemId();
        } catch (Exception e) {
            Log.e(TAG, "generate new item id for created folder is failed.");
            e.printStackTrace();
        }
        fItem.title = folderTitle;
        IconInfo newItem = (IconInfo) getItemInfoInAppsForComponentName(info.getComponentName(), info.getUser(), true);
        if (newItem == null) {
            return -1;
        }
        ArrayList itemList = new ArrayList();
        itemList.add((IconInfo) appItem);
        itemList.add(newItem);
        putItemToIdMap(fItem);
        createFolderAndAddItem(fItem, appItem, itemList);
        Log.i(TAG, "createFolder() - fItem.id : " + fItem.id);
        return fItem.id;
    }

    private void checkUpdate() {
        if (this.mPendingUpdateList != null && this.mPendingUpdateList.size() > 0) {
            Log.i(TAG, "checkUpdate Pending update item count = " + this.mPendingUpdateList.size());
            Iterator it = this.mPendingUpdateList.iterator();
            while (it.hasNext()) {
                PendingAddOrUpdateStruct pendingItem = (PendingAddOrUpdateStruct) it.next();
                addOrUpdater(pendingItem.mPackages, pendingItem.mAddedOrUpdatedApps, pendingItem.mUser);
            }
            this.mPendingUpdateList.clear();
        }
    }

    public void onLauncherBindingItemsCompleted() {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                Log.i(AppsModel.TAG, "onLauncherBindingItemsCompleted");
                ArrayList<ItemInfo> invalidItems = AppsModel.this.getInvalidItems();
                if (!invalidItems.isEmpty()) {
                    ArrayList<ItemInfo> allItems = AppsModel.this.getAllAppItemInApps();
                    UserHandleCompat user = UserHandleCompat.myUserHandle();
                    Iterator it = invalidItems.iterator();
                    while (it.hasNext()) {
                        AppsModel.this.removePackage(((ItemInfo) it.next()).componentName.getPackageName(), allItems, user);
                    }
                    AppsModel.this.removed.clear();
                    AppsModel.this.removePackagesAndComponents(invalidItems, user);
                }
                GameHomeManager.getInstance().updateGameAppsVisibility();
                HomeLoader homeLoader = AppsModel.sLauncherModel.getHomeLoader();
                IconCache iconCache = LauncherAppState.getInstance().getIconCache();
                HashSet<String> packagesToIgnore = new HashSet();
                if (homeLoader != null) {
                    homeLoader.getIgnorePackage(packagesToIgnore);
                    if (iconCache != null) {
                        Log.i(AppsModel.TAG, "onLauncherBindingItemsCompleted IconCache updateDbIcons");
                        iconCache.updateDbIcons(packagesToIgnore);
                    }
                }
                Log.i(AppsModel.TAG, "onLauncherBindingItemsCompleted mIsBootCompleted = " + AppsModel.mIsBootCompleted);
                if (!(AppsModel.this.mLoadTask == null || AppsModel.this.mLoadTask.isStopped() || AppsModel.mIsBootCompleted || AppsModel.sPendingPackages.isEmpty())) {
                    Log.i(AppsModel.TAG, "Apps finishBind but has PendingPackages, so registerReceiver : AppsAvailabilityCheck");
                    AppsModel.sContext.registerReceiver(new AppsAvailabilityCheck(), new IntentFilter(StartupReceiver.SYSTEM_READY), null, AppsModel.sWorkerHandler);
                }
                AppsModel.sLauncherModel.checkRemovedApps(AppsModel.this.mRemovedComponents);
                AppsModel.this.mRemovedComponents.clear();
                SALogging.getInstance().setDefaultValueForAppStatusLog(AppsModel.sContext);
            }
        });
        sLauncherModel.onLauncherBindingItemsCompleted();
    }

    private ArrayList<ItemInfo> getInvalidItems() {
        ArrayList<ItemInfo> invalidItems = new ArrayList();
        Iterator it = getAllAppItemInApps().iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item instanceof IconInfo) {
                IconInfo iconInfo = (IconInfo) item;
                if (sIconCache.isDefaultIcon(iconInfo.getIcon(sIconCache), iconInfo.user) && !iconInfo.hasStatusFlag(98)) {
                    List<LauncherActivityInfoCompat> apps = sLauncherApps.getActivityList(iconInfo.componentName.getPackageName(), iconInfo.user);
                    if (apps.isEmpty() || DataLoader.findActivityInfo(apps, iconInfo.componentName, iconInfo.user) == null) {
                        Log.d(TAG, iconInfo.componentName + " is defaultIcon and activityInfo is null");
                        invalidItems.add(item);
                    }
                }
            }
        }
        return invalidItems;
    }

    private void updateAppsPostPosition() {
        synchronized (mPostPositionLock) {
            if (PostPositionController.getInstance(sContext) != null) {
                PostPositionController.getInstance(sContext).addAllItems();
            }
        }
    }

    public ArrayList<ItemInfo> getFolderChildUpdate() {
        ArrayList<ItemInfo> removeList;
        synchronized (sBgLock) {
            removeList = new ArrayList();
            Iterator it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it.next();
                if (folderInfo.container == -102) {
                    Iterator it2 = folderInfo.contents.iterator();
                    while (it2.hasNext()) {
                        IconInfo info = (IconInfo) it2.next();
                        if (!sBgItemsIdMap.containsKey(info.id)) {
                            Log.d(TAG, "this item is not exist in BgItemsIdMap. so remove : " + info);
                            removeList.add(info);
                        }
                    }
                    it2 = removeList.iterator();
                    while (it2.hasNext()) {
                        folderInfo.contents.remove((ItemInfo) it2.next());
                    }
                }
            }
        }
        return removeList;
    }
}
