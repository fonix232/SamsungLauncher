package com.android.launcher3.home;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.TransactionTooLargeException;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableInt;
import android.util.Pair;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.PackageUpdatedTask;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.PackageInstallerCompat.PackageInstallInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.OpenMarketCustomization;
import com.android.launcher3.common.customer.OpenMarketCustomization.ItemChangedListener;
import com.android.launcher3.common.deviceprofile.GridInfo;
import com.android.launcher3.common.model.AutoInstallsLayout;
import com.android.launcher3.common.model.BadgeCache;
import com.android.launcher3.common.model.BadgeCache.CacheKey;
import com.android.launcher3.common.model.CursorInfo;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.DataLoader.DataLoaderCallback;
import com.android.launcher3.common.model.DataLoader.DataLoaderState;
import com.android.launcher3.common.model.DataLoader.PageLoaderTask;
import com.android.launcher3.common.model.DataUpdater;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.common.model.nano.LauncherDumpProto.DumpTarget;
import com.android.launcher3.common.model.nano.LauncherDumpProto.LauncherImpression;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.common.receiver.StartupReceiver;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.logging.DumpTargetWrapper;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PairAppsUtilities;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.ShortcutTray;
import com.android.launcher3.util.StringFilter;
import com.android.launcher3.util.logging.SALogging;
import com.google.protobuf.nano.MessageNano;
import com.sec.android.app.launcher.R;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class HomeLoader extends DataLoader {
    private static final String ACTION_APPWIDGET_SINGLE_INSTANCE = "com.sec.launcher.action.APPWIDGET_SINGLE_INSTANCE";
    private static final String ACTION_APPWIDGET_SINGLE_INSTANCE_PACKAGE = "com.sec.launcher.action.APPWIDGET_SINGLE_INSTANCE_PACKAGE";
    private static final Comparator<ItemInfo> HOTSEAT_COMPARATOR = new Comparator<ItemInfo>() {
        public final int compare(ItemInfo a, ItemInfo b) {
            if (a.screenId == b.screenId) {
                return 0;
            }
            if (a.screenId == -1) {
                return 1;
            }
            if (b.screenId == -1) {
                return -1;
            }
            if (a.screenId < b.screenId) {
                return -1;
            }
            return 1;
        }
    };
    private static final long INVALID_SCREEN_ID = -1;
    private static final int ITEMS_CHUNK = 6;
    private static final String TAG = "HomeLoader";
    private static final Object sBgWidgetLock = new Object();
    static HashMap<ComponentKey, LauncherAppWidgetProviderInfo> sBgWidgetProviders;
    private final boolean DEBUG_LOADERS = true;
    private final int EASY_APPS_INDEX = 2;
    private final int OCCUPIED_HOTSEAT = 1;
    private final int OCCUPIED_WORKSPACE = 0;
    private final int OUTSIDE_ITEM = 3;
    private final int OVERLAP_ITEM = 4;
    private final int REMOVE_ITEM = 1;
    private final int RESTORED_ITEM = 2;
    private final ArrayList<ItemInfo> mBgHotseatItems = new ArrayList();
    private WeakReference<HomeCallbacks> mCallbacks;
    private HashMap<Long, ArrayList<ItemInfo>> mExtraItemsAfterGridChanged = new HashMap();
    private boolean mIsFirstBind = true;
    private HomeItemPositionHelper mItemPositionHelper;
    private final ArrayList<Runnable> mLoadCompleteRunnables = new ArrayList();
    private final Object mLock = new Object();
    private HashMap<Long, NewScreenInfo> mNewPageIdsAfterGridChanged = new HashMap();

    public static class AppsAvailabilityCheck extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            synchronized (HomeLoader.sBgLock) {
                ArrayList<String> packagesRemoved = new ArrayList();
                ArrayList<String> packagesUnavailable = new ArrayList();
                for (Entry<UserHandleCompat, HashSet<String>> entry : HomeLoader.sPendingPackages.entrySet()) {
                    UserHandleCompat user = (UserHandleCompat) entry.getKey();
                    packagesRemoved.clear();
                    packagesUnavailable.clear();
                    Iterator it = ((HashSet) entry.getValue()).iterator();
                    while (it.hasNext()) {
                        ComponentName cn = ComponentName.unflattenFromString((String) it.next());
                        if (!HomeLoader.sLauncherApps.isActivityEnabledForProfile(cn, user)) {
                            boolean packageOnSdcard = HomeLoader.sLauncherApps.isAppEnabled(HomeLoader.sPackageManager, cn.getPackageName(), 8192);
                            boolean isValidComp = HomeLoader.checkIfValidLauncherComponent(HomeLoader.sLauncherApps.getActivityList(cn.getPackageName(), user), cn);
                            if (packageOnSdcard && isValidComp) {
                                Launcher.addDumpLog(HomeLoader.TAG, "Component found on sd-card: " + cn, true);
                                packagesUnavailable.add(cn.getPackageName());
                            } else {
                                Launcher.addDumpLog(HomeLoader.TAG, "Component not found: " + cn, true);
                                packagesRemoved.add(cn.getPackageName());
                            }
                        }
                    }
                    if (!packagesRemoved.isEmpty()) {
                        String[] packages = (String[]) packagesRemoved.toArray(new String[packagesRemoved.size()]);
                        LauncherModel access$7100 = HomeLoader.sLauncherModel;
                        LauncherModel access$7000 = HomeLoader.sLauncherModel;
                        access$7000.getClass();
                        access$7100.enqueueItemUpdatedTask(new PackageUpdatedTask(3, packages, user));
                    }
                    if (!packagesUnavailable.isEmpty()) {
                        packages = (String[]) packagesUnavailable.toArray(new String[packagesUnavailable.size()]);
                        access$7100 = HomeLoader.sLauncherModel;
                        access$7000 = HomeLoader.sLauncherModel;
                        access$7000.getClass();
                        access$7100.enqueueItemUpdatedTask(new PackageUpdatedTask(4, packages, user));
                    }
                }
                HomeLoader.sPendingPackages.clear();
            }
        }
    }

    interface HomeCallbacks {
        void bindAddScreens(ArrayList<Long> arrayList);

        void bindAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo);

        void bindAppsAdded(ArrayList<Long> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3);

        void bindAppsInFolderRemoved(ArrayList<FolderInfo> arrayList, ArrayList<ItemInfo> arrayList2);

        void bindComponentsRemoved(ArrayList<String> arrayList, HashSet<ComponentName> hashSet, UserHandleCompat userHandleCompat, int i);

        void bindFestivalPageIfNecessary();

        void bindFolderTitle(ItemInfo itemInfo);

        void bindFolders(LongArrayMap<FolderInfo> longArrayMap);

        void bindHotseatItems(ArrayList<ItemInfo> arrayList);

        void bindInsertScreens(long j, int i);

        void bindItem(ItemInfo itemInfo, boolean z);

        void bindItems(ArrayList<ItemInfo> arrayList, int i, int i2, boolean z);

        void bindItemsRemoved(ArrayList<ItemInfo> arrayList);

        void bindRemoveScreen(int i);

        void bindRestoreItemsChange(HashSet<ItemInfo> hashSet);

        void bindScreens(ArrayList<Long> arrayList);

        void bindShortcutsChanged(ArrayList<IconInfo> arrayList, ArrayList<IconInfo> arrayList2, UserHandleCompat userHandleCompat);

        void bindUpdateContainer(boolean z, FolderInfo folderInfo, IconInfo iconInfo);

        void bindUpdatePosition(ArrayList<ItemInfo> arrayList);

        void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> arrayList);

        void finishBindingItems();

        int getCurrentWorkspaceScreen();

        void initFestivalPageIfNecessary();

        void onPageBoundSynchronously(int i);

        void startBinding();
    }

    private static class NewScreenInfo {
        long originalScreenId;
        int plusIndex;

        NewScreenInfo(long screenId, int index) {
            this.originalScreenId = screenId;
            this.plusIndex = index;
        }
    }

    private class HomeLoaderTask extends PageLoaderTask implements DataLoaderState {
        private HomeLoaderTask() {
            super();
        }

        protected Boolean doInBackground(DataLoaderCallback... params) {
            if (params.length > 0 && params[0] != null) {
                HomeLoader.this.mLoaderCallback = params[0];
            }
            boolean needNextPageLoad = false;
            if (!this.mStopped) {
                for (int i = 0; i < HomeLoader.this.mIsPageLoaded.length; i++) {
                    if (!HomeLoader.this.mIsPageLoaded[i]) {
                        HomeLoader.this.bindPageItems(HomeLoader.this.loadPageItems(i, this), null, this);
                        needNextPageLoad = true;
                        break;
                    }
                }
                if (!needNextPageLoad) {
                    finishBind();
                }
            }
            return Boolean.valueOf(needNextPageLoad);
        }

        protected void onPostExecute(Boolean loadNextPage) {
            if (loadNextPage.booleanValue()) {
                Log.d(HomeLoader.TAG, "onPostExecute, loadNextPage : " + loadNextPage + " mStopped : " + this.mStopped);
                if (!this.mStopped) {
                    HomeLoader.this.mPageLoaderTask = new HomeLoaderTask();
                    HomeLoader.this.mPageLoaderTask.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new DataLoaderCallback[0]);
                }
            }
        }

        private void finalCheckForDuplicateInfo() {
            final ArrayList<ItemInfo> duplicateList = new ArrayList();
            ArrayList<ItemInfo> homeItems = HomeLoader.this.getAllAppItemInHome();
            ArrayList<ComponentKey> tmpItems = new ArrayList();
            Iterator it = homeItems.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                ComponentKey compKey = new ComponentKey(info.componentName, info.user);
                boolean isExist = false;
                Iterator it2 = tmpItems.iterator();
                while (it2.hasNext()) {
                    if (compKey.equals((ComponentKey) it2.next())) {
                        isExist = true;
                        break;
                    }
                }
                if (isExist) {
                    duplicateList.add(info);
                } else {
                    tmpItems.add(compKey);
                }
            }
            if (!duplicateList.isEmpty()) {
                HomeLoader.this.mFavoritesUpdater.deleteItemsFromDatabase(duplicateList);
                final HomeCallbacks callbacks = HomeLoader.this.getCallback();
                HomeLoader.this.runOnMainThread(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (cb != null && callbacks == cb) {
                            cb.bindItemsRemoved(duplicateList);
                        }
                    }
                });
            }
        }

        private void loadRemainApps() {
            Log.d(HomeLoader.TAG, "loadRemainApps start");
            HashMap<ComponentKey, LauncherActivityInfoCompat> allAppsMap = new HashMap();
            ArrayList<ItemInfo> addList = new ArrayList();
            List<UserHandleCompat> profiles = UserManagerCompat.getInstance(HomeLoader.sContext).getUserProfiles();
            for (UserHandleCompat profileUser : profiles) {
                List<LauncherActivityInfoCompat> apps = HomeLoader.sLauncherApps.getActivityList(null, profileUser);
                if (apps == null || apps.isEmpty()) {
                    Log.d(HomeLoader.TAG, "There is no getActivityList apps for user " + profileUser);
                } else {
                    for (LauncherActivityInfoCompat info : apps) {
                        allAppsMap.put(new ComponentKey(info.getComponentName(), profileUser), info);
                    }
                    ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(HomeLoader.sContext, profileUser);
                    if (heuristic != null) {
                        heuristic.processUserApps(apps);
                    }
                }
            }
            ManagedProfileHeuristic.processAllUsers(profiles, HomeLoader.sContext);
            Iterator it = HomeLoader.this.getAllApps().iterator();
            while (it.hasNext()) {
                ItemInfo iteminfo = (ItemInfo) it.next();
                allAppsMap.remove(new ComponentKey(iteminfo.getIntent().getComponent(), iteminfo.user));
            }
            Log.d(HomeLoader.TAG, "RemainApps count : " + allAppsMap.size());
            for (LauncherActivityInfoCompat info2 : allAppsMap.values()) {
                Log.d(HomeLoader.TAG, "RemainApps add : " + info2.getLabel());
                addList.add(IconInfo.fromActivityInfo(info2, HomeLoader.sContext));
            }
            HomeLoader.this.restoreStkPositionIfNecessary(addList);
            HomeLoader.this.addAndBindAddedWorkspaceItems(HomeLoader.sContext, addList, false);
        }

        private void loadHiddenApps() {
            Log.d(HomeLoader.TAG, "loadHiddenApps start");
            ArrayList<ItemInfo> pageItems = new ArrayList();
            HashMap<Integer, ArrayList<?>> needHandlingItems = new HashMap();
            needHandlingItems.put(Integer.valueOf(4), new ArrayList());
            needHandlingItems.put(Integer.valueOf(3), new ArrayList());
            needHandlingItems.put(Integer.valueOf(1), new ArrayList());
            needHandlingItems.put(Integer.valueOf(2), new ArrayList());
            LongArrayMap<ItemInfo[][]> occupied = new LongArrayMap();
            occupied.put(0, (ItemInfo[][]) Array.newInstance(ItemInfo.class, new int[]{HomeLoader.sProfile.homeGrid.getCellCountX() + 1, HomeLoader.sProfile.homeGrid.getCellCountY() + 1}));
            Cursor cursor = HomeLoader.sContentResolver.query(Favorites.CONTENT_URI, null, "container=-100 AND itemType=0 AND screen=-1 AND (hidden!=0)", null, null);
            if (cursor != null) {
                Log.d(HomeLoader.TAG, "HiddenApps count : " + cursor.getCount());
                CursorInfo cursorIconInfo = new CursorInfo(cursor);
                while (cursor.moveToNext()) {
                    try {
                        HomeLoader.this.createShortcutItem(cursor, cursorIconInfo, needHandlingItems, occupied);
                    } catch (Exception e) {
                        Launcher.addDumpLog(HomeLoader.TAG, "hidden items loading interrupted", e, true);
                    } catch (Throwable th) {
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }
                HomeLoader.this.doHandlingItems(needHandlingItems, pageItems);
            }
        }

        private void loadExtraAppsAfterChangeGrid() {
            if (!HomeLoader.this.mExtraItemsAfterGridChanged.isEmpty()) {
                Log.i(HomeLoader.TAG, "loadExtraAppsAfterChangeGrid");
                for (Long longValue : HomeLoader.this.mExtraItemsAfterGridChanged.keySet()) {
                    long screenId = longValue.longValue();
                    ArrayList<ItemInfo> items = (ArrayList) HomeLoader.this.mExtraItemsAfterGridChanged.get(Long.valueOf(screenId));
                    if (items == null || items.isEmpty()) {
                        Log.d(HomeLoader.TAG, "loadExtraAppsAfterChangeGrid - screen : " + screenId + " items is empty");
                        return;
                    }
                    int insertScreenIndex = HomeLoader.this.mBgOrderedScreens.indexOf(Long.valueOf(((NewScreenInfo) HomeLoader.this.mNewPageIdsAfterGridChanged.get(Long.valueOf(screenId))).originalScreenId)) + ((NewScreenInfo) HomeLoader.this.mNewPageIdsAfterGridChanged.get(Long.valueOf(screenId))).plusIndex;
                    HomeLoader.this.insertWorkspaceScreen(HomeLoader.sContext, insertScreenIndex, screenId);
                    Log.d(HomeLoader.TAG, "loadExtraAppsAfterChangeGrid add screen id : " + screenId + " index : " + insertScreenIndex + " item size  : " + items.size());
                    ArrayList<ContentProviderOperation> ops = new ArrayList();
                    Iterator it = items.iterator();
                    while (it.hasNext()) {
                        ItemInfo item = (ItemInfo) it.next();
                        Uri uri = Favorites.getContentUri(item.id);
                        ContentValues values = new ContentValues();
                        values.put("screen", Long.valueOf(item.screenId));
                        values.put("cellX", Integer.valueOf(item.cellX));
                        values.put("cellY", Integer.valueOf(item.cellY));
                        values.put("spanX", Integer.valueOf(item.spanX));
                        values.put("spanY", Integer.valueOf(item.spanY));
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
                    }
                    try {
                        HomeLoader.sContentResolver.applyBatch("com.sec.android.app.launcher.settings", ops);
                    } catch (RemoteException e) {
                        Log.d(HomeLoader.TAG, "occurred RemoteException during loadExtraAppsAfterChangeGrid - " + e.getMessage());
                    } catch (OperationApplicationException e2) {
                        Log.d(HomeLoader.TAG, "occurred OperationApplicationException during loadExtraAppsAfterChangeGrid - " + e2.getMessage());
                    }
                    it = items.iterator();
                    while (it.hasNext()) {
                        HomeLoader.this.addPagesItem((ItemInfo) it.next());
                    }
                    HomeLoader.this.bindPageItems(items, null, this);
                }
            }
        }

        private void unpinShortcutIfNotExist() {
            if (!HomeLoader.this.mShortcutKeyToPinnedShortcuts.isEmpty()) {
                ArrayList<ShortcutKey> shortcutKeys = new ArrayList();
                ArrayList<Uri> uris = new ArrayList();
                uris.add(LauncherAppState.getInstance().isHomeOnlyModeEnabled(false) ? Favorites_HomeApps.CONTENT_URI : Favorites_HomeOnly.CONTENT_URI);
                uris.add(LauncherAppState.getInstance().isEasyModeEnabled() ? Favorites_Standard.CONTENT_URI : Favorites_Easy.CONTENT_URI);
                String selection = "itemType=6";
                String[] projection = new String[]{"intent", BaseLauncherColumns.PROFILE_ID};
                Iterator it = uris.iterator();
                while (it.hasNext()) {
                    Cursor cursor = HomeLoader.sContentResolver.query((Uri) it.next(), projection, selection, null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            try {
                                ShortcutKey shortcutKey = ShortcutKey.fromIntent(Intent.parseUri(cursor.getString(0), 0), (UserHandleCompat) HomeLoader.this.mAllUsers.get(cursor.getLong(1)));
                                if (!shortcutKeys.contains(shortcutKey)) {
                                    shortcutKeys.add(shortcutKey);
                                }
                            } catch (URISyntaxException e) {
                                Log.d(HomeLoader.TAG, "URISyntaxException " + e.toString());
                                if (!cursor.isClosed()) {
                                    cursor.close();
                                }
                            } catch (Throwable th) {
                                if (!cursor.isClosed()) {
                                    cursor.close();
                                }
                            }
                        }
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                }
                for (ShortcutKey key : HomeLoader.this.mShortcutKeyToPinnedShortcuts.keySet()) {
                    MutableInt numTimesPinned = (MutableInt) HomeLoader.sBgPinnedShortcutCounts.get(key);
                    if ((numTimesPinned == null || numTimesPinned.value == 0) && !shortcutKeys.contains(key)) {
                        Log.d(HomeLoader.TAG, "unpin shortcut that not exist on workspace : " + key.toString());
                        HomeLoader.this.mDeepShortcutManager.unpinShortcut(key);
                    }
                }
            }
        }

        private void finishBind() {
            Log.i(HomeLoader.TAG, "finishBind, task : " + this);
            if (this.mStopped) {
                Log.w(HomeLoader.TAG, "finishBind pageLoaderTask is stopped");
                return;
            }
            loadExtraAppsAfterChangeGrid();
            if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                loadHiddenApps();
                loadRemainApps();
                finalCheckForDuplicateInfo();
            }
            synchronized (HomeLoader.sBgLock) {
                boolean mapIsNotEmpty = !HomeLoader.sBgItemsIdMap.isEmpty();
            }
            if (HomeLoader.this.makeSingleInstanceAppWidgetList() && mapIsNotEmpty) {
                Iterator it = HomeLoader.this.getWidgetsInHome().iterator();
                while (it.hasNext()) {
                    HomeLoader.this.checkAppWidgetSingleInstanceList((LauncherAppWidgetInfo) it.next());
                }
            }
            if (LauncherFeature.supportFestivalPage()) {
                HomeLoader.this.bindFestivalPageIfNecessary(this);
            }
            unpinShortcutIfNotExist();
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    if (HomeLoaderTask.this.mStopped) {
                        Log.i(HomeLoader.TAG, "finishBind, stopped : clearSBgDataStructures");
                        HomeLoader.this.clearSBgDataStructures();
                    } else if (HomeLoader.this.mLoaderCallback != null) {
                        HomeLoader.this.mLoaderCallback.onLoaderComplete();
                    }
                    Log.i(HomeLoader.TAG, "finishBind, try to register AppsAvailabilityCheck : mStooped=" + HomeLoaderTask.this.mStopped + " mIsBootCompleted=" + HomeLoader.mIsBootCompleted + " sPendingPackages.size=" + HomeLoader.sPendingPackages.size());
                    if (!(HomeLoaderTask.this.mStopped || HomeLoader.mIsBootCompleted || HomeLoader.sPendingPackages.isEmpty())) {
                        Log.i(HomeLoader.TAG, "finishBind, registerReceiver : AppsAvailabilityCheck");
                        HomeLoader.sContext.registerReceiver(new AppsAvailabilityCheck(), new IntentFilter(StartupReceiver.SYSTEM_READY), null, HomeLoader.sWorkerHandler);
                    }
                    HomeLoader.this.mLoaderCallback = null;
                }
            });
            final HomeCallbacks oldCallbacks = HomeLoader.this.getCallback();
            if (oldCallbacks == null) {
                Log.w(HomeLoader.TAG, "finishBind running with no launcher");
                return;
            }
            final HomeLoaderTask task = this;
            HomeLoader.this.runOnMainThread(new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, task);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                        HomeLoader.sIsLoadingAndBindingWorkspace = false;
                        HomeLoader.this.runAllBindCompleteRunnables();
                        HomeLoader.this.mPageLoaderTask = null;
                    }
                }
            });
        }

        public boolean isStopped() {
            return this.mStopped;
        }
    }

    public HomeLoader(Context context, LauncherAppState app, LauncherModel model, IconCache cache, BadgeCache badgeCache) {
        init(context, app, model, cache, badgeCache, this);
        this.mFavoritesUpdater = new FavoritesUpdater(context, model, cache, this);
        this.mItemPositionHelper = new HomeItemPositionHelper(sContentResolver);
        OpenMarketCustomization.getInstance().setListener(new ItemChangedListener() {
            public void onItemChanged(final IconInfo item, final ContentValues values, final boolean isRemoved) {
                final ArrayList<IconInfo> updatedIcons = new ArrayList();
                final ArrayList<IconInfo> removedIcons = new ArrayList();
                if (isRemoved) {
                    removedIcons.add(item);
                } else {
                    updatedIcons.add(item);
                }
                Runnable updateRunnable = new Runnable() {
                    public void run() {
                        if (isRemoved) {
                            HomeLoader.this.mFavoritesUpdater.deleteItem(item);
                        } else {
                            HomeLoader.this.mFavoritesUpdater.updateItem(values, item);
                        }
                    }
                };
                final HomeCallbacks oldCallbacks = HomeLoader.this.getCallback();
                HomeLoader.sHandler.post(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (cb != null && oldCallbacks == cb) {
                            cb.bindShortcutsChanged(updatedIcons, removedIcons, UserHandleCompat.myUserHandle());
                        }
                    }
                });
                DataLoader.runOnWorkerThread(updateRunnable);
            }
        }, false);
    }

    HomeItemPositionHelper getItemPositionHelper() {
        return this.mItemPositionHelper;
    }

    void clearPreservedPosition() {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                HomeLoader.this.mItemPositionHelper.clearPreservedPosition();
            }
        });
    }

    public void startPageLoaderTask(final DataLoaderCallback loaderCallback, final DataLoaderState task) {
        runOnMainThread(new Runnable() {
            public void run() {
                Log.d(HomeLoader.TAG, "startPageLoaderTask");
                if (HomeLoader.this.isStopped(task)) {
                    Log.d(HomeLoader.TAG, "stopped.");
                    return;
                }
                HomeLoader.this.mPageLoaderTask = new HomeLoaderTask();
                HomeLoader.this.mPageLoaderTask.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new DataLoaderCallback[]{loaderCallback});
            }
        });
    }

    void registerCallbacks(HomeCallbacks callbacks) {
        synchronized (this.mLock) {
            this.mCallbacks = new WeakReference(callbacks);
        }
    }

    public void unRegisterCallbacks() {
        synchronized (this.mLock) {
            this.mCallbacks = new WeakReference(null);
        }
    }

    public ArrayList<ItemInfo> loadPageItems(int rank, DataLoaderState task) {
        HomeCallbacks oldCallbacks = getCallback();
        if (oldCallbacks == null) {
            Log.w(TAG, "loadPageItems running with no launcher");
            return null;
        }
        if (this.mBgOrderedScreens.isEmpty()) {
            Log.w(TAG, "loadPageItems running with no screen : check favorites");
            if (checkWorkspaceIsEmpty()) {
                Log.w(TAG, "loadPageItems running with no screen : mBgOrderedScreens.isEmpty()");
                return null;
            }
        }
        long t = SystemClock.uptimeMillis();
        if (rank < 0) {
            rank = Math.min(oldCallbacks.getCurrentWorkspaceScreen(), this.mBgOrderedScreens.size() - 1);
        }
        if (rank < 0 || rank >= this.mBgOrderedScreens.size()) {
            Log.w(TAG, "loadPageItems this rank is invalid : rank " + rank);
            return null;
        }
        String selection;
        String[] selectionArg;
        long screen = ((Long) this.mBgOrderedScreens.get(rank)).longValue();
        ArrayList<ItemInfo> pageItems = new ArrayList();
        HashMap<Integer, ArrayList<?>> needHandlingItems = new HashMap();
        needHandlingItems.put(Integer.valueOf(4), new ArrayList());
        needHandlingItems.put(Integer.valueOf(3), new ArrayList());
        needHandlingItems.put(Integer.valueOf(1), new ArrayList());
        needHandlingItems.put(Integer.valueOf(2), new ArrayList());
        Uri contentUri = Favorites.CONTENT_URI;
        boolean isReArrangeHotseatData = false;
        LongArrayMap<ItemInfo[][]> occupied = new LongArrayMap();
        occupied.put(0, (ItemInfo[][]) Array.newInstance(ItemInfo.class, new int[]{sProfile.homeGrid.getCellCountX() + 1, sProfile.homeGrid.getCellCountY() + 1}));
        if (this.mIsFirstBind) {
            selection = "(screen=? AND container=?) OR (container=?)";
            selectionArg = new String[]{String.valueOf(screen), String.valueOf(-100), String.valueOf(Favorites.CONTAINER_HOTSEAT)};
            isReArrangeHotseatData = true;
            occupied.put(1, (ItemInfo[][]) Array.newInstance(ItemInfo.class, new int[]{LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount(), 1}));
        } else {
            selection = "screen=? AND container=?";
            selectionArg = new String[]{String.valueOf(screen), String.valueOf(-100)};
        }
        Cursor c = sContentResolver.query(contentUri, null, selection, selectionArg, null);
        if (c == null) {
            return null;
        }
        CursorInfo cursorIconInfo = new CursorInfo(c);
        try {
            LongArrayMap<ItemInfo> folders = createItems(c, cursorIconInfo, needHandlingItems, pageItems, occupied, task);
            if (isStopped(task)) {
                return null;
            }
            if (folders != null && folders.size() > 0) {
                c = sContentResolver.query(contentUri, null, "container in (" + makeFoldersIdToString(folders) + ')', null, null);
                if (c != null) {
                    try {
                        createItems(c, cursorIconInfo, needHandlingItems, pageItems, occupied, task);
                    } finally {
                        if (!c.isClosed()) {
                            c.close();
                        }
                    }
                } else {
                    Log.d(TAG, "Cursor is null. Exist empty folder. folders size : " + folders.size());
                }
            }
            if (isStopped(task)) {
                return null;
            }
            String line;
            doHandlingItems(needHandlingItems, pageItems);
            sortItemsInFolder(folders);
            if (isReArrangeHotseatData) {
                reArrangeHotseatData(pageItems, 0);
            }
            Log.d(TAG, "loaded workspace in " + (SystemClock.uptimeMillis() - t) + "ms");
            Log.d(TAG, "workspace layout: ");
            int countX = sProfile.homeGrid.getCellCountX();
            int countY = sProfile.homeGrid.getCellCountY();
            int y = 0;
            while (y < countY) {
                line = "";
                ItemInfo[][] page = (ItemInfo[][]) occupied.get(0);
                int x = 0;
                while (x < countX) {
                    if (x >= page.length || y >= page[x].length) {
                        line = line + "!";
                    } else {
                        line = line + (page[x][y] != null ? "#" : ".");
                    }
                    x++;
                }
                Log.d(TAG, "[ " + line + " ]");
                y++;
            }
            if (this.mIsFirstBind) {
                Log.d(TAG, "hotseat layout: ");
                int hotseatCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
                line = "";
                ItemInfo[][] hotseat = (ItemInfo[][]) occupied.get(1);
                for (int i = 0; i < hotseatCount; i++) {
                    line = line + (hotseat[i][0] != null ? "#" : ".");
                }
                Log.d(TAG, "[ " + line + " ]");
            }
            if (rank < 0) {
                return pageItems;
            }
            this.mIsPageLoaded[rank] = true;
            return pageItems;
        } finally {
            if (!c.isClosed()) {
                c.close();
            }
        }
    }

    private void doHandlingItems(HashMap<Integer, ArrayList<?>> needHandlingItems, ArrayList<ItemInfo> pageItems) {
        if (((ArrayList) needHandlingItems.get(Integer.valueOf(1))).size() > 0) {
            String where = Utilities.createDbSelectionQuery("_id", (Iterable) needHandlingItems.get(Integer.valueOf(1)));
            sContentResolver.delete(Favorites.CONTENT_URI, where, null);
            Log.d(TAG, "doHandlingItems Removed = " + where);
            for (Long longValue : sFavoritesProvider.deleteEmptyFolders()) {
                long folderId = longValue.longValue();
                synchronized (sBgLock) {
                    ItemInfo removeFolderItem = (ItemInfo) sBgFolders.get(folderId);
                    removePagesItem(removeFolderItem);
                    pageItems.remove(removeFolderItem);
                    sBgFolders.remove(folderId);
                    sBgItemsIdMap.remove(folderId);
                }
            }
        }
        if (((ArrayList) needHandlingItems.get(Integer.valueOf(2))).size() > 0) {
            ContentValues values = new ContentValues();
            where = Utilities.createDbSelectionQuery("_id", (Iterable) needHandlingItems.get(Integer.valueOf(2)));
            values.put("restored", Integer.valueOf(0));
            sContentResolver.update(Favorites.CONTENT_URI, values, where, null);
            Log.d(TAG, "doHandlingItems restored = " + where);
        }
        if (((ArrayList) needHandlingItems.get(Integer.valueOf(4))).size() > 0) {
            ArrayList<ItemInfo> overlapItems = (ArrayList) needHandlingItems.get(Integer.valueOf(4));
            int countX = sProfile.homeGrid.getCellCountX();
            int countY = sProfile.homeGrid.getCellCountY();
            Iterator it = overlapItems.iterator();
            while (it.hasNext()) {
                ItemInfo item = (ItemInfo) it.next();
                item.cellX = countX;
                item.cellY = countY;
                Log.d(TAG, "doHandlingItems overlap = " + item.toString());
            }
        }
        if (((ArrayList) needHandlingItems.get(Integer.valueOf(4))).size() > 0 || ((ArrayList) needHandlingItems.get(Integer.valueOf(3))).size() > 0) {
            handleOutsideItems(pageItems);
            Log.d(TAG, "doHandlingItems extra = " + ((ArrayList) needHandlingItems.get(Integer.valueOf(3))).toString());
        }
    }

    private LongArrayMap<ItemInfo> createItems(Cursor c, CursorInfo cursorIconInfo, HashMap<Integer, ArrayList<?>> needHandlingItems, ArrayList<ItemInfo> pageItems, LongArrayMap<ItemInfo[][]> occupied, DataLoaderState task) {
        LongArrayMap<ItemInfo> folders = null;
        while (!isStopped(task) && c.moveToNext()) {
            try {
                ItemInfo info = null;
                switch (c.getInt(cursorIconInfo.itemTypeIndex)) {
                    case 0:
                    case 1:
                    case 6:
                    case 7:
                        info = createShortcutItem(c, cursorIconInfo, needHandlingItems, occupied);
                        break;
                    case 2:
                        info = createFolderItem(c, cursorIconInfo, needHandlingItems, occupied);
                        if (info != null) {
                            if (folders == null) {
                                folders = new LongArrayMap();
                            }
                            folders.put(info.id, info);
                            break;
                        }
                        break;
                    case 4:
                    case 5:
                        info = createWidgetItem(c, cursorIconInfo, needHandlingItems, occupied);
                        break;
                }
                if (info != null && info.hidden == 0) {
                    pageItems.add(info);
                }
            } catch (Exception e) {
                Launcher.addDumpLog(TAG, "Desktop items loading interrupted", e, true);
            }
        }
        return folders;
    }

    private ItemInfo createWidgetItem(Cursor c, CursorInfo cursorInfo, HashMap<Integer, ArrayList<?>> needHandlingItems, LongArrayMap<ItemInfo[][]> occupied) {
        int container = c.getInt(cursorInfo.containerIndex);
        boolean customWidget = c.getInt(cursorInfo.itemTypeIndex) == 5;
        int appWidgetId = c.getInt(cursorInfo.appWidgetIdIndex);
        long serialNumber = (long) c.getInt(cursorInfo.profileIdIndex);
        String savedProvider = c.getString(cursorInfo.appWidgetProviderIndex);
        long id = c.getLong(cursorInfo.idIndex);
        UserHandleCompat user = (UserHandleCompat) this.mAllUsers.get(serialNumber);
        ArrayList<Long> itemsToRemove = (ArrayList) needHandlingItems.get(Integer.valueOf(1));
        if (user == null) {
            itemsToRemove.add(Long.valueOf(id));
            return null;
        }
        AppWidgetProviderInfo provider;
        ComponentName component = ComponentName.unflattenFromString(savedProvider);
        int restoreStatus = c.getInt(cursorInfo.restoredIndex);
        boolean isIdValid = (restoreStatus & 1) == 0;
        boolean wasProviderReady = (restoreStatus & 2) == 0;
        AppWidgetManagerCompat wm = AppWidgetManagerCompat.getInstance(sContext);
        if (appWidgetId > 0) {
            provider = wm.getAppWidgetInfo(appWidgetId);
        } else {
            provider = getProviderInfo(sContext, ComponentName.unflattenFromString(savedProvider), user);
        }
        boolean isProviderReady = LauncherModel.isValidProvider(provider);
        if (sIsSafeMode || customWidget || !wasProviderReady || isProviderReady) {
            ItemInfo appWidgetInfo;
            if (isProviderReady) {
                appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, provider.provider);
                int status = restoreStatus & -9;
                if (!wasProviderReady) {
                    if (isIdValid) {
                        status = 4;
                    } else {
                        status &= -3;
                    }
                }
                appWidgetInfo.restoreStatus = status;
            } else {
                int i;
                Log.v(TAG, "Widget restore pending id=" + id + " appWidgetId=" + appWidgetId + " status =" + restoreStatus);
                appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, component);
                appWidgetInfo.restoreStatus = restoreStatus;
                Integer installProgress = sInstallingPkgs == null ? null : (Integer) sInstallingPkgs.get(component.getPackageName());
                if ((restoreStatus & 8) == 0) {
                    if (installProgress != null) {
                        appWidgetInfo.restoreStatus |= 8;
                    } else if (!sIsSafeMode) {
                        Launcher.addDumpLog(TAG, "Unrestored widget removed: " + component, true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                }
                if (installProgress == null) {
                    i = 0;
                } else {
                    i = installProgress.intValue();
                }
                appWidgetInfo.installProgress = i;
            }
            appWidgetInfo.id = id;
            appWidgetInfo.screenId = (long) c.getInt(cursorInfo.screenIndex);
            appWidgetInfo.cellX = c.getInt(cursorInfo.cellXIndex);
            appWidgetInfo.cellY = c.getInt(cursorInfo.cellYIndex);
            appWidgetInfo.spanX = c.getInt(cursorInfo.spanXIndex);
            appWidgetInfo.spanY = c.getInt(cursorInfo.spanYIndex);
            appWidgetInfo.user = user;
            if (container == -100 || container == -101) {
                appWidgetInfo.container = (long) container;
                if (checkItemPlacement((ItemInfo[][]) occupied.get(0), appWidgetInfo, needHandlingItems)) {
                    if (!customWidget) {
                        String providerName = appWidgetInfo.providerName.flattenToString();
                        if (!(providerName.equals(savedProvider) && appWidgetInfo.restoreStatus == restoreStatus)) {
                            ContentValues values = new ContentValues();
                            values.put(Favorites.APPWIDGET_PROVIDER, providerName);
                            values.put("restored", Integer.valueOf(appWidgetInfo.restoreStatus));
                            updateItem(id, values);
                        }
                    }
                    putItemToIdMap(appWidgetInfo);
                    addPagesItem(appWidgetInfo);
                    return appWidgetInfo;
                }
                itemsToRemove.add(Long.valueOf(id));
                return null;
            }
            Log.e(TAG, "Widget found where container != CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
            itemsToRemove.add(Long.valueOf(id));
            return null;
        }
        String log = "Deleting widget that isn't installed anymore: id=" + id + " appWidgetId=" + appWidgetId;
        Log.e(TAG, log);
        Launcher.addDumpLog(TAG, log, false);
        itemsToRemove.add(Long.valueOf(id));
        return null;
    }

    private ItemInfo createFolderItem(Cursor c, CursorInfo cursorInfo, HashMap<Integer, ArrayList<?>> needHandlingItems, LongArrayMap<ItemInfo[][]> occupied) {
        ItemInfo folderInfo;
        long id = c.getLong(cursorInfo.idIndex);
        int container = c.getInt(cursorInfo.containerIndex);
        boolean restored = c.getInt(cursorInfo.restoredIndex) != 0;
        ItemInfo returnItem = null;
        synchronized (sBgLock) {
            folderInfo = (FolderInfo) sBgFolders.get(id);
        }
        ArrayList<Long> itemsToRemove = (ArrayList) needHandlingItems.get(Integer.valueOf(1));
        ArrayList<Long> restoredRows = (ArrayList) needHandlingItems.get(Integer.valueOf(2));
        if (folderInfo == null) {
            folderInfo = new FolderInfo();
        }
        folderInfo.title = c.getString(cursorInfo.titleIndex);
        folderInfo.id = id;
        folderInfo.container = (long) container;
        folderInfo.screenId = (long) c.getInt(cursorInfo.screenIndex);
        folderInfo.cellX = c.getInt(cursorInfo.cellXIndex);
        folderInfo.cellY = c.getInt(cursorInfo.cellYIndex);
        folderInfo.spanX = 1;
        folderInfo.spanY = 1;
        folderInfo.options = c.getInt(cursorInfo.optionsIndex);
        folderInfo.color = c.getInt(cursorInfo.colorIndex);
        if (checkItemPlacement((ItemInfo[][]) occupied.get(folderInfo.container == -101 ? 1 : 0), folderInfo, needHandlingItems)) {
            switch (container) {
                case Favorites.CONTAINER_HOTSEAT /*-101*/:
                case -100:
                    addPagesItem(folderInfo);
                    returnItem = folderInfo;
                    break;
            }
            if (restored) {
                restoredRows.add(Long.valueOf(id));
            }
            putItemToIdMap(folderInfo);
            return returnItem;
        }
        itemsToRemove.add(Long.valueOf(id));
        return null;
    }

    ItemInfo createShortcutItem(Cursor c, CursorInfo cursorInfo, HashMap<Integer, ArrayList<?>> needHandlingItems, LongArrayMap<ItemInfo[][]> occupied) {
        ItemInfo info = null;
        String intentDescription = c.getString(cursorInfo.intentIndex);
        int container = c.getInt(cursorInfo.containerIndex);
        long id = c.getLong(cursorInfo.idIndex);
        long serialNumber = (long) c.getInt(cursorInfo.profileIdIndex);
        UserHandleCompat user = (UserHandleCompat) this.mAllUsers.get(serialNumber);
        int promiseType = c.getInt(cursorInfo.restoredIndex);
        boolean restored = promiseType != 0;
        int disabledState = 0;
        int itemType = c.getInt(cursorInfo.itemTypeIndex);
        ArrayList<Long> itemsToRemove = (ArrayList) needHandlingItems.get(Integer.valueOf(1));
        ArrayList<Long> restoredRows = (ArrayList) needHandlingItems.get(Integer.valueOf(2));
        if (user == null) {
            itemsToRemove.add(Long.valueOf(id));
            boolean allowMissingTarget = false;
            return null;
        }
        Intent intent;
        Intent intent2;
        boolean useLowResIcon;
        Integer progress;
        FolderInfo folderInfo;
        ItemInfo itemInfo = null;
        boolean isAppShortcut = false;
        if (itemType == 0) {
            try {
                intent = Intent.parseUri(intentDescription, 0);
            } catch (URISyntaxException e) {
                Launcher.addDumpLog(TAG, "Invalid uri: " + intentDescription, true);
                itemsToRemove.add(Long.valueOf(id));
                allowMissingTarget = false;
                return null;
            }
        }
        intent = Intent.parseUri(intentDescription, 4);
        ComponentName cn = intent.getComponent();
        if (cn == null || cn.getPackageName() == null) {
            if (cn == null) {
                restoredRows.add(Long.valueOf(id));
                restored = false;
                allowMissingTarget = false;
                intent2 = intent;
            }
            allowMissingTarget = false;
            intent2 = intent;
        } else {
            boolean validPkg = sLauncherApps.isPackageEnabledForProfile(cn.getPackageName(), user);
            boolean validComponent = validPkg && sLauncherApps.isActivityEnabledForProfile(cn, user);
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
                if (itemType == 1 && Utilities.isLauncherAppTarget(intent) && sPackageManager.resolveActivity(intent, 0) != null) {
                    isAppShortcut = true;
                    allowMissingTarget = false;
                    intent2 = intent;
                }
                allowMissingTarget = false;
                intent2 = intent;
            } else if (validPkg) {
                Intent validPkgIntent = null;
                if (!((promiseType & 2) == 0 && (promiseType & 32) == 0)) {
                    validPkgIntent = sPackageManager.getLaunchIntentForPackage(cn.getPackageName());
                    if (validPkgIntent != null) {
                        values = new ContentValues();
                        values.put("intent", validPkgIntent.toUri(0));
                        updateItem(id, values);
                    }
                }
                if (validPkgIntent == null) {
                    backupStkPositionIfNecessary(cn.getPackageName(), (long) container, (long) c.getInt(cursorInfo.screenIndex), c.getInt(cursorInfo.cellXIndex), c.getInt(cursorInfo.cellYIndex));
                    if ((promiseType & 32) != 0) {
                        Launcher.addDumpLog(TAG, "package not yet restored (validPkg): " + cn, true);
                        intent2 = intent;
                    } else {
                        Launcher.addDumpLog(TAG, "Invalid intent removed: " + cn, true);
                        itemsToRemove.add(Long.valueOf(id));
                        allowMissingTarget = false;
                        return null;
                    }
                }
                intent2 = validPkgIntent;
                if ((promiseType & 32) != 0) {
                    values = new ContentValues();
                    values.put("title", "");
                    values.put("icon", (byte[]) null);
                    updateItem(id, values);
                }
                restoredRows.add(Long.valueOf(id));
                restored = false;
                allowMissingTarget = false;
            } else if (restored) {
                Launcher.addDumpLog(TAG, "package not yet restored: " + cn, true);
                if (!((promiseType & 2) == 0 && (promiseType & 1) == 0 && (promiseType & 64) == 0) && (sInstallingPkgs == null || !sInstallingPkgs.containsKey(cn.getPackageName()))) {
                    Launcher.addDumpLog(TAG, "UnRestored package removed: " + cn, true);
                    itemsToRemove.add(Long.valueOf(id));
                    allowMissingTarget = false;
                    return null;
                }
                allowMissingTarget = false;
                intent2 = intent;
            } else {
                int notAvailableType = isNotAvailableApps(cn.getPackageName());
                if (notAvailableType > 0) {
                    Launcher.addDumpLog(TAG, "Package unavailable  : " + cn + " by " + notAvailableType, true);
                    disabledState = 0 | notAvailableType;
                    allowMissingTarget = true;
                    intent2 = intent;
                } else if (!mIsBootCompleted) {
                    Launcher.addDumpLog(TAG, "Invalid package: " + cn + " (check again later)", true);
                    addAppToPendingPackages(cn.flattenToString(), user);
                    allowMissingTarget = true;
                    intent2 = intent;
                } else if (DualAppUtils.supportDualApp(sContext) && itemType == 1 && DualAppUtils.DUAL_APP_DAAGENT_PACKAGE_NAME.equals(cn.getPackageName())) {
                    Log.d(TAG, "Dual app shortcut : " + intent.toUri(0));
                    allowMissingTarget = false;
                    intent2 = intent;
                } else {
                    Launcher.addDumpLog(TAG, "Invalid package removed: " + cn, true);
                    itemsToRemove.add(Long.valueOf(id));
                    allowMissingTarget = false;
                    return null;
                }
            }
        }
        if (container >= 0) {
            if (c.getInt(cursorInfo.rankIndex) >= 9) {
                useLowResIcon = true;
                if (restored) {
                    if (user.equals(UserHandleCompat.myUserHandle())) {
                        Launcher.addDumpLog(TAG, "Don't restore items for other profiles: " + user, true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                    Launcher.addDumpLog(TAG, "constructing info for restored package promiseType: " + promiseType, true);
                    info = getRestoredItemInfo(c, cn, intent2, promiseType, cursorInfo);
                    if (info != null) {
                        Launcher.addDumpLog(TAG, "restore item's info is null", true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                    intent2 = getRestoredItemIntent(intent2, promiseType);
                } else if (itemType != 0 || isAppShortcut) {
                    info = getAppShortcutInfo(intent2, user, c, cursorInfo, allowMissingTarget, useLowResIcon, isAppShortcut);
                    if (info == null) {
                        Launcher.addDumpLog(TAG, "This item's info is null", true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                } else if (itemType == 1) {
                    info = getShortcutInfo(intent2, c, cursorInfo, user, cn);
                } else if (itemType == 6) {
                    ShortcutKey key = ShortcutKey.fromIntent(intent2, user);
                    ShortcutInfoCompat pinnedShortcut = (ShortcutInfoCompat) this.mShortcutKeyToPinnedShortcuts.get(key);
                    if (pinnedShortcut == null) {
                        Launcher.addDumpLog(TAG, "The pinned shortcut is no longer valid", true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                    r0 = new IconInfo(pinnedShortcut, sContext);
                    intent2 = r0.intent;
                    DataLoader.incrementPinnedShortcutCount(key, false);
                } else if (itemType == 7) {
                    if (PairAppsUtilities.isValidComponents(sContext, intentDescription)) {
                        r0 = new LauncherPairAppsInfo(sContext, intentDescription);
                    } else {
                        Launcher.addDumpLog(TAG, "The Pairapps shortcut is no longer valid", true);
                        itemsToRemove.add(Long.valueOf(id));
                        return null;
                    }
                }
                if (info == null) {
                    info.id = id;
                    info.intent = intent2;
                    info.container = (long) container;
                    info.screenId = (long) c.getInt(cursorInfo.screenIndex);
                    info.cellX = c.getInt(cursorInfo.cellXIndex);
                    info.cellY = c.getInt(cursorInfo.cellYIndex);
                    info.rank = c.getInt(cursorInfo.rankIndex);
                    info.spanX = 1;
                    info.spanY = 1;
                    info.hidden = c.getInt(cursorInfo.hiddenIndex);
                    if (!(info.itemType == 1 || info.itemType == 6)) {
                        info.intent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
                        if (info.promisedIntent != null) {
                            info.promisedIntent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
                        }
                    }
                    info.isDisabled |= disabledState;
                    if (sIsSafeMode && !Utilities.isSystemApp(sContext, intent2)) {
                        info.isDisabled |= 1;
                    }
                    if (info.hidden == 0) {
                        if (!checkItemPlacement((ItemInfo[][]) occupied.get(info.container != -101 ? 1 : 0), info, needHandlingItems)) {
                            itemsToRemove.add(Long.valueOf(id));
                            return null;
                        }
                    }
                    if (intent2 != null) {
                        cn = info.getTargetComponent();
                        if (cn != null) {
                            info.mBadgeCount = sBadgeCache.getBadgeCount(new CacheKey(cn, user));
                        }
                    }
                    if (restored) {
                        cn = info.getTargetComponent();
                        if (cn != null) {
                            progress = sInstallingPkgs != null ? null : (Integer) sInstallingPkgs.get(cn.getPackageName());
                            if (progress == null) {
                                info.setInstallProgress(progress.intValue());
                            } else {
                                info.status &= -9;
                            }
                        }
                    }
                    switch (container) {
                        case Favorites.CONTAINER_HOTSEAT /*-101*/:
                        case -100:
                            addPagesItem(info);
                            itemInfo = info;
                            break;
                        default:
                            synchronized (sBgLock) {
                                folderInfo = (FolderInfo) sBgFolders.get((long) container);
                            }
                            if (folderInfo != null) {
                                folderInfo.add((IconInfo) info);
                                break;
                            }
                            break;
                    }
                    putItemToIdMap(info);
                    return itemInfo;
                }
                throw new RuntimeException("Unexpected null IconInfo");
            }
        }
        useLowResIcon = false;
        if (restored) {
            if (user.equals(UserHandleCompat.myUserHandle())) {
                Launcher.addDumpLog(TAG, "Don't restore items for other profiles: " + user, true);
                itemsToRemove.add(Long.valueOf(id));
                return null;
            }
            Launcher.addDumpLog(TAG, "constructing info for restored package promiseType: " + promiseType, true);
            info = getRestoredItemInfo(c, cn, intent2, promiseType, cursorInfo);
            if (info != null) {
                intent2 = getRestoredItemIntent(intent2, promiseType);
            } else {
                Launcher.addDumpLog(TAG, "restore item's info is null", true);
                itemsToRemove.add(Long.valueOf(id));
                return null;
            }
        }
        if (itemType != 0) {
        }
        info = getAppShortcutInfo(intent2, user, c, cursorInfo, allowMissingTarget, useLowResIcon, isAppShortcut);
        if (info == null) {
            Launcher.addDumpLog(TAG, "This item's info is null", true);
            itemsToRemove.add(Long.valueOf(id));
            return null;
        }
        if (info == null) {
            throw new RuntimeException("Unexpected null IconInfo");
        }
        info.id = id;
        info.intent = intent2;
        info.container = (long) container;
        info.screenId = (long) c.getInt(cursorInfo.screenIndex);
        info.cellX = c.getInt(cursorInfo.cellXIndex);
        info.cellY = c.getInt(cursorInfo.cellYIndex);
        info.rank = c.getInt(cursorInfo.rankIndex);
        info.spanX = 1;
        info.spanY = 1;
        info.hidden = c.getInt(cursorInfo.hiddenIndex);
        info.intent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
        if (info.promisedIntent != null) {
            info.promisedIntent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
        }
        info.isDisabled |= disabledState;
        info.isDisabled |= 1;
        if (info.hidden == 0) {
            if (info.container != -101) {
            }
            if (checkItemPlacement((ItemInfo[][]) occupied.get(info.container != -101 ? 1 : 0), info, needHandlingItems)) {
                itemsToRemove.add(Long.valueOf(id));
                return null;
            }
        }
        if (intent2 != null) {
            cn = info.getTargetComponent();
            if (cn != null) {
                info.mBadgeCount = sBadgeCache.getBadgeCount(new CacheKey(cn, user));
            }
        }
        if (restored) {
            cn = info.getTargetComponent();
            if (cn != null) {
                if (sInstallingPkgs != null) {
                }
                if (progress == null) {
                    info.status &= -9;
                } else {
                    info.setInstallProgress(progress.intValue());
                }
            }
        }
        switch (container) {
            case Favorites.CONTAINER_HOTSEAT /*-101*/:
            case -100:
                addPagesItem(info);
                itemInfo = info;
                break;
            default:
                synchronized (sBgLock) {
                    folderInfo = (FolderInfo) sBgFolders.get((long) container);
                }
                if (folderInfo != null) {
                    folderInfo.add((IconInfo) info);
                    break;
                }
                break;
        }
        putItemToIdMap(info);
        return itemInfo;
    }

    public void bindPageItems(ArrayList<ItemInfo> pageItems, ArrayList<Runnable> deferredBindRunnables, DataLoaderState task) {
        if (isStopped(task)) {
            Log.i(TAG, "bindPageItems task is stopped");
            return;
        }
        final HomeCallbacks oldCallbacks = getCallback();
        if (oldCallbacks == null) {
            Log.w(TAG, "bindPageItems running with no launcher");
            return;
        }
        if (LauncherFeature.supportFestivalPage()) {
            initFestivalPageIfNecessary(task);
        }
        if (this.mIsFirstBind) {
            final DataLoaderState dataLoaderState = task;
            runOnMainThread(new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            });
            ArrayList<Long> orderedScreenIds = new ArrayList();
            synchronized (sBgLock) {
                orderedScreenIds.addAll(this.mBgOrderedScreens);
            }
            bindWorkspaceScreens(oldCallbacks, orderedScreenIds, task);
            this.mIsFirstBind = false;
        }
        if (pageItems == null || pageItems.isEmpty()) {
            Log.w(TAG, "bindPageItems page item is null or empty!");
            return;
        }
        Runnable r;
        int i;
        ArrayList<ItemInfo> shortcuts = new ArrayList();
        ArrayList<ItemInfo> widgets = new ArrayList();
        final ArrayList<ItemInfo> hotseatShortcuts = new ArrayList();
        final LongArrayMap<FolderInfo> folders = new LongArrayMap();
        Iterator it = pageItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info.itemType == 2) {
                folders.put(info.id, (FolderInfo) info);
            } else if (info.itemType == 5 || info.itemType == 4) {
                widgets.add(info);
            }
            switch (info.itemType) {
                case 0:
                case 1:
                case 2:
                case 6:
                case 7:
                    if (info.container != -101) {
                        shortcuts.add(info);
                        break;
                    } else {
                        hotseatShortcuts.add(info);
                        break;
                    }
                default:
                    Log.w(TAG, "no shortcut itemType!");
                    break;
            }
        }
        if (hotseatShortcuts.size() > 0) {
            dataLoaderState = task;
            r = new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                    if (callbacks != null) {
                        callbacks.bindItems(hotseatShortcuts, 0, hotseatShortcuts.size(), false);
                    }
                }
            };
            if (deferredBindRunnables != null) {
                synchronized (deferredBindRunnables) {
                    deferredBindRunnables.add(r);
                }
            } else {
                runOnMainThread(r);
            }
        }
        int N = widgets.size();
        for (i = 0; i < N; i++) {
            final LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) widgets.get(i);
            dataLoaderState = task;
            r = new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                    if (callbacks != null) {
                        callbacks.bindAppWidget(widget);
                    }
                }
            };
            if (deferredBindRunnables != null) {
                synchronized (deferredBindRunnables) {
                    deferredBindRunnables.add(r);
                }
            } else {
                runOnMainThread(r);
            }
        }
        ScreenGridUtilities.getOutSideItems(shortcuts, 4);
        N = shortcuts.size();
        for (i = 0; i < N; i++) {
            final ItemInfo shortcut = (ItemInfo) shortcuts.get(i);
            dataLoaderState = task;
            r = new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                    if (callbacks != null) {
                        callbacks.bindItem(shortcut, false);
                    }
                }
            };
            if (deferredBindRunnables != null) {
                synchronized (deferredBindRunnables) {
                    deferredBindRunnables.add(r);
                }
            } else {
                runOnMainThread(r);
            }
        }
        if (!folders.isEmpty()) {
            dataLoaderState = task;
            r = new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                    if (callbacks != null) {
                        callbacks.bindFolders(folders);
                    }
                }
            };
            if (deferredBindRunnables != null) {
                synchronized (deferredBindRunnables) {
                    deferredBindRunnables.add(r);
                }
                return;
            }
            runOnMainThread(r);
        }
    }

    public void setup(DataLoaderState task) {
        unbindItemsOnMainThread();
        clearSBgDataStructures();
        createPinnedShortcutMap();
        this.mBgOrderedScreens.addAll(sFavoritesProvider.loadScreensFromDb());
        this.mIsPageLoaded = new boolean[this.mBgOrderedScreens.size()];
        this.mIsFirstBind = true;
        mIsBootCompleted = Utilities.isBootCompleted();
    }

    private void createPinnedShortcutMap() {
        for (UserHandleCompat user : UserManagerCompat.getInstance(sContext).getUserProfiles()) {
            List<ShortcutInfoCompat> pinnedShortcuts = this.mDeepShortcutManager.queryForPinnedShortcuts(null, user);
            if (this.mDeepShortcutManager.wasLastCallSuccess()) {
                for (ShortcutInfoCompat shortcut : pinnedShortcuts) {
                    this.mShortcutKeyToPinnedShortcuts.put(ShortcutKey.fromInfo(shortcut), shortcut);
                }
            }
        }
    }

    private IconInfo getAppShortcutInfo(Intent intent, UserHandleCompat user, Cursor c, CursorInfo cursorItemInfo, boolean allowMissingTarget, boolean useLowResIcon, boolean isAppShortcut) {
        if (user == null) {
            Log.d(TAG, "Null user found in getShortcutInfo");
            return null;
        }
        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            Log.d(TAG, "Missing component found in getAppShortcutInfo: " + componentName);
            return null;
        }
        LauncherActivityInfoCompat lai = null;
        if (isAppShortcut || sIsSafeMode) {
            Intent newIntent = new Intent(intent.getAction(), null);
            newIntent.addCategory("android.intent.category.LAUNCHER");
            newIntent.setComponent(componentName);
            lai = sLauncherApps.resolveActivity(newIntent, user);
            if (lai == null && !allowMissingTarget) {
                Log.d(TAG, "Missing activity found in getAppShortcutInfo: " + componentName);
                return null;
            }
        }
        for (LauncherActivityInfoCompat launcherInfo : sLauncherApps.getActivityList(componentName.getPackageName(), user)) {
            if (launcherInfo.getComponentName().equals(componentName)) {
                lai = launcherInfo;
                break;
            }
        }
        if (lai == null && !allowMissingTarget) {
            ActivityInfo activityInfo = null;
            if (UserHandleCompat.myUserHandle().equals(user)) {
                try {
                    activityInfo = sPackageManager.getActivityInfo(componentName, 0);
                } catch (NameNotFoundException e) {
                    Log.d(TAG, "This component is not exist - " + componentName);
                }
            }
            if (activityInfo == null || !activityInfo.exported) {
                Log.d(TAG, "ComponentName does not match with the Launcher category : " + componentName);
                return null;
            }
            lai = sLauncherApps.resolveActivity(intent, user);
            if (lai == null) {
                Log.d(TAG, "Missing activity found in getAppShortcutInfo: " + componentName);
                return null;
            }
            Log.d(TAG, "change item type to shortcut type : " + componentName);
            ContentValues values = new ContentValues();
            values.put("itemType", Integer.valueOf(1));
            updateItem(c.getLong(cursorItemInfo.idIndex), values);
            isAppShortcut = true;
        }
        IconInfo info = new IconInfo();
        sIconCache.getTitleAndIcon(info, componentName, lai, user, false, useLowResIcon);
        if (sIconCache.isDefaultIcon(info.getIcon(sIconCache), user) && c != null) {
            if (!allowMissingTarget) {
                List<LauncherActivityInfoCompat> apps = sLauncherApps.getActivityList(componentName.getPackageName(), user);
                if (apps.isEmpty() || DataLoader.findActivityInfo(apps, componentName, user) == null) {
                    Log.d(TAG, componentName + " is defaultIcon and activityInfo is null");
                    return null;
                }
            }
            Bitmap icon = BitmapUtils.createIconBitmap(c, cursorItemInfo.iconIndex, sContext);
            if (icon == null) {
                icon = sIconCache.getDefaultIcon(user);
            }
            info.setIcon(icon);
        }
        if (lai != null && Utilities.isAppSuspended(lai.getApplicationInfo())) {
            info.isDisabled = 4;
        }
        if (TextUtils.isEmpty(info.title) && c != null) {
            info.title = Utilities.trim(c.getString(cursorItemInfo.titleIndex));
        }
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.isAppShortcut = isAppShortcut;
        info.itemType = isAppShortcut ? 1 : 0;
        info.user = user;
        info.contentDescription = sUserManager.getBadgedLabelForUser(info.title, info.user);
        if (lai != null) {
            info.flags = IconInfo.initFlags(lai);
            return info;
        } else if (info.itemType != 0 || Utilities.isSystemApp(sContext, intent)) {
            return info;
        } else {
            info.flags = 1;
            return info;
        }
    }

    private IconInfo getShortcutInfo(Intent intent, Cursor c, CursorInfo iconInfo, UserHandleCompat user, ComponentName cn) {
        IconInfo info = new IconInfo();
        info.user = user;
        info.itemType = 1;
        if (intent.getAction() == null || !intent.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW)) {
            info.title = Utilities.trim(c.getString(iconInfo.titleIndex));
            Bitmap icon = iconInfo.loadIcon(c, info, sContext);
            if (icon == null) {
                Log.d(TAG, info.title + " shortcut's icon is null. use default icon");
                icon = sIconCache.getDefaultIcon(info.user);
                info.usingFallbackIcon = true;
            }
            if (OpenThemeManager.getInstance().isDefaultTheme()) {
                info.setOriginalIcon(icon);
                icon = ShortcutTray.getIcon(sContext, icon, cn);
                info.customIcon = true;
            }
            if (DualAppUtils.supportDualApp(sContext) && DualAppUtils.isDualAppId(info.user)) {
                if (!info.customIcon) {
                    info.customIcon = true;
                    info.setOriginalIcon(icon);
                }
                icon = DualAppUtils.makeUserBadgedIcon(sContext, icon, sProfile.homeGrid.getIconSize(), info.user.getUser());
            }
            info.setIcon(icon);
            if (cn != null) {
                info.componentName = cn;
                Intent newIntent = new Intent(intent.getAction(), null);
                newIntent.setComponent(cn);
                LauncherActivityInfoCompat lai = sLauncherApps.resolveActivity(newIntent, user);
                if (lai != null) {
                    info.flags = IconInfo.initFlags(lai);
                }
            }
        } else {
            info.isAppsButton = true;
            info.title = sContext.getResources().getString(R.string.apps_button_label);
            LauncherAppState.getInstance().setAppsButtonEnabled(true);
        }
        return info;
    }

    private boolean checkItemPlacement(ItemInfo[][] occupied, ItemInfo item, HashMap<Integer, ArrayList<? extends Object>> needHandlingItems) {
        int countX = sProfile.homeGrid.getCellCountX();
        int countY = sProfile.homeGrid.getCellCountY();
        ArrayList<ItemInfo> overlapItems = (ArrayList) needHandlingItems.get(Integer.valueOf(4));
        ArrayList<Long> outsideItems = (ArrayList) needHandlingItems.get(Integer.valueOf(3));
        long containerIndex = item.screenId;
        if (item.container == -101) {
            int hotseatCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
            if (item.screenId >= ((long) hotseatCount)) {
                Log.e(TAG, "Error loading shortcut " + item + " into hotseat position " + item.screenId + ", position out of bounds: (0 to " + (hotseatCount - 1) + ")");
                return false;
            } else if (occupied[(int) item.screenId][0] != null) {
                Log.e(TAG, "Error loading shortcut into hotseat " + item + " into position (" + item.screenId + ":" + item.cellX + "," + item.cellY + ") occupied by " + occupied[(int) item.screenId][0]);
                return false;
            } else {
                occupied[(int) item.screenId][0] = item;
                return true;
            }
        } else if (item.container != -100) {
            return true;
        } else {
            if (item.cellX < 0 || item.cellY < 0 || item.cellX + item.spanX > countX || item.cellY + item.spanY > countY) {
                Log.e(TAG, "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screenId + ":" + item.cellX + "," + item.cellY + ") out of screen bounds ( " + countX + DefaultLayoutParser.ATTR_X + countY + ")");
                outsideItems.add(Long.valueOf(item.id));
                return true;
            }
            int x;
            int y;
            for (x = item.cellX; x < item.cellX + item.spanX; x++) {
                for (y = item.cellY; y < item.cellY + item.spanY; y++) {
                    if (occupied[x][y] != null) {
                        Log.e(TAG, "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screenId + ":" + x + "," + y + ") occupied by " + occupied[x][y]);
                        overlapItems.add(item);
                        return true;
                    }
                }
            }
            for (x = item.cellX; x < item.cellX + item.spanX; x++) {
                for (y = item.cellY; y < item.cellY + item.spanY; y++) {
                    occupied[x][y] = item;
                }
            }
            return true;
        }
    }

    protected void clearSBgDataStructures() {
        synchronized (sBgLock) {
            this.mBgPagesItems.clear();
            sBgFolders.clear();
            sBgItemsIdMap.clear();
            this.mBgOrderedScreens.clear();
            this.mBgHotseatItems.clear();
            sBgPinnedShortcutCounts.clear();
            this.mShortcutKeyToPinnedShortcuts.clear();
            this.added.clear();
            this.modified.clear();
            this.removed.clear();
        }
        this.mNewPageIdsAfterGridChanged.clear();
        this.mExtraItemsAfterGridChanged.clear();
    }

    private static boolean checkIfValidLauncherComponent(List<LauncherActivityInfoCompat> apps, ComponentName component) {
        for (LauncherActivityInfoCompat info : apps) {
            if (info.getComponentName().equals(component)) {
                return true;
            }
        }
        return false;
    }

    public static LauncherAppWidgetProviderInfo getProviderInfo(Context ctx, ComponentName name, UserHandleCompat user) {
        LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo;
        synchronized (sBgWidgetLock) {
            if (sBgWidgetProviders == null) {
                getWidgetProviders(ctx, false);
            }
            launcherAppWidgetProviderInfo = (LauncherAppWidgetProviderInfo) sBgWidgetProviders.get(new ComponentKey(name, user));
        }
        return launcherAppWidgetProviderInfo;
    }

    public static List<LauncherAppWidgetProviderInfo> getWidgetProviders(Context context, boolean refresh) {
        ArrayList<LauncherAppWidgetProviderInfo> results = new ArrayList();
        try {
            synchronized (sBgWidgetLock) {
                if (sBgWidgetProviders == null || refresh) {
                    HashMap<ComponentKey, LauncherAppWidgetProviderInfo> tmpWidgetProviders = new HashMap();
                    AppWidgetManagerCompat wm = AppWidgetManagerCompat.getInstance(context);
                    for (AppWidgetProviderInfo pInfo : wm.getAllProviders()) {
                        LauncherAppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.fromProviderInfo(context, pInfo);
                        tmpWidgetProviders.put(new ComponentKey(info.provider, wm.getUser(info)), info);
                    }
                    sBgWidgetProviders = tmpWidgetProviders;
                }
                results.addAll(sBgWidgetProviders.values());
            }
        } catch (Exception e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                synchronized (sBgWidgetLock) {
                    if (sBgWidgetProviders != null) {
                        results.addAll(sBgWidgetProviders.values());
                    }
                }
            } else {
                throw e;
            }
        }
        return results;
    }

    public static boolean checkNeedToRefreshWidget(String[] packages, UserHandleCompat user, boolean needToRefresh) {
        synchronized (sBgWidgetLock) {
            if (sBgWidgetProviders != null) {
                HashSet<String> pkgSet = new HashSet();
                Collections.addAll(pkgSet, packages);
                for (ComponentKey key : sBgWidgetProviders.keySet()) {
                    int i = (key.user.equals(user) && pkgSet.contains(key.componentName.getPackageName())) ? 1 : 0;
                    needToRefresh |= i;
                }
            }
        }
        return needToRefresh;
    }

    private HomeCallbacks tryGetCallbacks(HomeCallbacks oldCallbacks, DataLoaderState task) {
        synchronized (this.mLock) {
            if (isStopped(task)) {
                return null;
            } else if (this.mCallbacks == null) {
                return null;
            } else {
                HomeCallbacks callbacks = (HomeCallbacks) this.mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                } else if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                } else {
                    return callbacks;
                }
            }
        }
    }

    public void unbindItemsOnMainThread() {
        final ArrayList<ItemInfo> tmpItems = new ArrayList();
        synchronized (sBgLock) {
            tmpItems.addAll(this.mBgHotseatItems);
            for (ArrayList<ItemInfo> infos : this.mBgPagesItems.values()) {
                tmpItems.addAll(infos);
            }
        }
        runOnMainThread(new Runnable() {
            public void run() {
                Iterator it = tmpItems.iterator();
                while (it.hasNext()) {
                    ((ItemInfo) it.next()).unbind();
                }
            }
        });
    }

    private void bindWorkspaceScreens(final HomeCallbacks oldCallbacks, final ArrayList<Long> orderedScreens, final DataLoaderState task) {
        runOnMainThread(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, task);
                if (callbacks != null) {
                    callbacks.bindScreens(orderedScreens);
                }
            }
        });
    }

    private void initFestivalPageIfNecessary(final DataLoaderState task) {
        final HomeCallbacks oldCallbacks = getCallback();
        if (oldCallbacks == null) {
            Log.w(TAG, "initFestivalPageIfNecessary failed with no launcher");
        } else {
            runOnMainThread(new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, task);
                    if (callbacks != null) {
                        callbacks.initFestivalPageIfNecessary();
                    }
                }
            });
        }
    }

    private void bindFestivalPageIfNecessary(final DataLoaderState task) {
        final HomeCallbacks oldCallbacks = getCallback();
        if (oldCallbacks == null) {
            Log.w(TAG, "bindFestivalPageIfNecessary failed with no launcher");
        } else {
            runOnMainThread(new Runnable() {
                public void run() {
                    HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, task);
                    if (callbacks != null) {
                        callbacks.bindFestivalPageIfNecessary();
                    }
                }
            });
        }
    }

    public void setPackageState(final PackageInstallInfo installInfo) {
        if (installInfo != null && installInfo.packageName != null) {
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    final HashSet<ItemInfo> updates = new HashSet();
                    if (installInfo.state != 0) {
                        Iterator it = HomeLoader.this.getAllItemInHome().iterator();
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
                            } else if (info instanceof LauncherAppWidgetInfo) {
                                LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) info;
                                if (widget.providerName.getPackageName().equals(installInfo.packageName)) {
                                    widget.installProgress = installInfo.progress;
                                    updates.add(widget);
                                }
                            }
                        }
                        if (!updates.isEmpty()) {
                            final HomeCallbacks oldCallbacks = HomeLoader.this.getCallback();
                            HomeLoader.sHandler.post(new Runnable() {
                                public void run() {
                                    HomeCallbacks callbacks = HomeLoader.this.getCallback();
                                    if (callbacks != null && oldCallbacks == callbacks) {
                                        callbacks.bindRestoreItemsChange(updates);
                                    }
                                }
                            });
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
                    final ArrayList<IconInfo> updates = new ArrayList();
                    final UserHandleCompat user = UserHandleCompat.myUserHandle();
                    Iterator it = HomeLoader.this.getAllItemInHome().iterator();
                    while (it.hasNext()) {
                        ItemInfo info = (ItemInfo) it.next();
                        if (info instanceof IconInfo) {
                            IconInfo iconInfo = (IconInfo) info;
                            ComponentName cn = iconInfo.getTargetComponent();
                            if (iconInfo.isPromise() && cn != null && packageName.equals(cn.getPackageName())) {
                                if (iconInfo.hasStatusFlag(2)) {
                                    HomeLoader.sIconCache.getTitleAndIcon(iconInfo, iconInfo.promisedIntent, user, iconInfo.shouldUseLowResIcon());
                                } else if (!iconInfo.hasStatusFlag(36)) {
                                    iconInfo.updateIcon(HomeLoader.sIconCache);
                                }
                                updates.add(iconInfo);
                            }
                        }
                    }
                    if (!updates.isEmpty()) {
                        final HomeCallbacks oldCallbacks = HomeLoader.this.getCallback();
                        HomeLoader.sHandler.post(new Runnable() {
                            public void run() {
                                HomeCallbacks callbacks = HomeLoader.this.getCallback();
                                if (callbacks != null && oldCallbacks == callbacks) {
                                    callbacks.bindShortcutsChanged(updates, new ArrayList(), user);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    public void updateShortcut(HashSet<String> updatedPackages, final UserHandleCompat user) {
        final ArrayList<IconInfo> updatedShortcuts = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if ((info instanceof IconInfo) && user.equals(info.user) && info.itemType == 0) {
                    IconInfo si = (IconInfo) info;
                    ComponentName cn = si.getTargetComponent();
                    if (cn != null && updatedPackages.contains(cn.getPackageName())) {
                        si.updateIcon(sIconCache);
                        updatedShortcuts.add(si);
                    }
                }
            }
        }
        final HomeCallbacks oldCallbacks = getCallback();
        sHandler.post(new Runnable() {
            public void run() {
                HomeCallbacks cb = HomeLoader.this.getCallback();
                if (cb != null && oldCallbacks == cb) {
                    cb.bindShortcutsChanged(updatedShortcuts, new ArrayList(), user);
                }
            }
        });
    }

    public void getIgnorePackage(HashSet<String> packagesToIgnore) {
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info instanceof IconInfo) {
                    IconInfo si = (IconInfo) info;
                    if (si.isPromise() && si.getTargetComponent() != null) {
                        String packageName = si.getTargetComponent().getPackageName();
                        packagesToIgnore.add(packageName);
                        Log.i(TAG, "IgnorePackage: " + packageName + ", status: " + si.status);
                    }
                } else if (info instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo lawi = (LauncherAppWidgetInfo) info;
                    if (lawi.hasRestoreFlag(2)) {
                        packagesToIgnore.add(lawi.providerName.getPackageName());
                    }
                }
            }
        }
    }

    void bindItemAfterChangePosition(final ItemInfo item) {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                boolean isExist = true;
                synchronized (HomeLoader.sBgLock) {
                    if (HomeLoader.sBgItemsIdMap.get(item.id) == null) {
                        Log.d(HomeLoader.TAG, "bindItemAfterChangePosition : " + item + " is already deleted");
                        isExist = false;
                    }
                }
                if (isExist) {
                    HomeLoader.this.mFavoritesUpdater.deleteItem(item);
                    ItemInfo itemInfo = item;
                    ItemInfo itemInfo2 = item;
                    ItemInfo itemInfo3 = item;
                    item.cellY = -1;
                    itemInfo3.cellX = -1;
                    long j = (long) -1;
                    itemInfo2.screenId = j;
                    itemInfo.id = j;
                    ArrayList<ItemInfo> items = new ArrayList();
                    items.add(item);
                    HomeLoader.this.addAndBindAddedWorkspaceItems(HomeLoader.sContext, items, false);
                }
            }
        });
    }

    void addAndBindAddedWorkspaceItems(Context context, ArrayList<? extends ItemInfo> workspaceApps, boolean needToMovePage) {
        final HomeCallbacks callbacks = getCallback();
        if (!workspaceApps.isEmpty()) {
            final ArrayList<? extends ItemInfo> arrayList = workspaceApps;
            final Context context2 = context;
            final boolean z = needToMovePage;
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    ArrayList<ItemInfo> addedItemsFinal = new ArrayList();
                    final ArrayList<ItemInfo> updatedItemsFinal = new ArrayList();
                    final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList();
                    ArrayList<Long> workspaceScreens = HomeLoader.sFavoritesProvider.loadScreensFromDb();
                    synchronized (HomeLoader.sBgLock) {
                        Iterator it = arrayList.iterator();
                        while (it.hasNext()) {
                            ItemInfo item = (ItemInfo) it.next();
                            if ((item instanceof IconInfo) && !(item instanceof LauncherPairAppsInfo) && LauncherAppState.getInstance().isHomeOnlyModeEnabled() && HomeLoader.this.shortcutExists(item.getIntent(), item.user)) {
                                Log.d(HomeLoader.TAG, "shortcut exist in workspace : " + item.title);
                            } else if ((item instanceof IconInfo) || (item instanceof FolderInfo) || (item instanceof LauncherAppWidgetInfo) || (item instanceof LauncherPairAppsInfo)) {
                                ItemInfo itemInfo = item;
                                if (-1 == itemInfo.container) {
                                    itemInfo.container = -100;
                                }
                                if (-1 == itemInfo.screenId || -1 == itemInfo.cellX || -1 == itemInfo.cellY) {
                                    Pair<Long, int[]> coords = HomeLoader.this.findSpaceForItem(workspaceScreens, addedWorkspaceScreensFinal, item.spanX, item.spanY, true, itemInfo.screenId);
                                    int[] coordinates = coords.second;
                                    itemInfo.screenId = ((Long) coords.first).longValue();
                                    itemInfo.cellX = coordinates[0];
                                    itemInfo.cellY = coordinates[1];
                                }
                                if (itemInfo.id == -1 || HomeLoader.sBgItemsIdMap.get(itemInfo.id) == null) {
                                    HomeLoader.this.mFavoritesUpdater.addItem(itemInfo);
                                    addedItemsFinal.add(itemInfo);
                                } else {
                                    ContentValues values = new ContentValues();
                                    values.put("screen", Long.valueOf(itemInfo.screenId));
                                    values.put("cellX", Integer.valueOf(itemInfo.cellX));
                                    values.put("cellY", Integer.valueOf(itemInfo.cellY));
                                    HomeLoader.this.mFavoritesUpdater.updateItem(values, itemInfo);
                                    updatedItemsFinal.add(itemInfo);
                                }
                                Log.d(HomeLoader.TAG, "addAndBindAddedWorkspaceItems item : " + itemInfo.title + " screenId : " + itemInfo.screenId + " cellX : " + itemInfo.cellX + " cellY : " + itemInfo.cellY);
                            } else {
                                throw new RuntimeException("Unexpected info type : " + item.itemType);
                            }
                        }
                    }
                    HomeLoader.this.mFavoritesUpdater.updateScreenOrder(context2, workspaceScreens);
                    if (!addedWorkspaceScreensFinal.isEmpty()) {
                        HomeLoader.this.runOnMainThread(new Runnable() {
                            public void run() {
                                HomeCallbacks cb = HomeLoader.this.getCallback();
                                if (cb != null && callbacks == cb) {
                                    callbacks.bindAddScreens(addedWorkspaceScreensFinal);
                                }
                            }
                        });
                    }
                    if (!updatedItemsFinal.isEmpty()) {
                        HomeLoader.this.runOnMainThread(new Runnable() {
                            public void run() {
                                HomeCallbacks cb = HomeLoader.this.getCallback();
                                if (cb != null && callbacks == cb) {
                                    cb.bindUpdatePosition(updatedItemsFinal);
                                }
                            }
                        });
                    }
                    HomeLoader.this.bindItems(addedItemsFinal, z);
                }
            });
        }
    }

    void bindItems(ArrayList<ItemInfo> addedItemsFinal, boolean needToMovePage) {
        final HomeCallbacks callbacks = getCallback();
        if (!addedItemsFinal.isEmpty()) {
            final ItemInfo item;
            ArrayList<ItemInfo> addAnimated = new ArrayList();
            ArrayList<ItemInfo> addNotAnimated = new ArrayList();
            int currentScreenIndex = 0;
            if (callbacks != null) {
                currentScreenIndex = callbacks.getCurrentWorkspaceScreen();
            }
            if (currentScreenIndex >= this.mBgOrderedScreens.size()) {
                currentScreenIndex = this.mBgOrderedScreens.size() - 1;
            }
            if (currentScreenIndex < 0) {
                Log.d(TAG, "bindItems currentScreenIndex : " + currentScreenIndex + " mBgOrderedScreens.size : " + this.mBgOrderedScreens.size());
                currentScreenIndex = 0;
            }
            long currentScreenId = ((Long) this.mBgOrderedScreens.get(currentScreenIndex)).longValue();
            Iterator it = addedItemsFinal.iterator();
            while (it.hasNext()) {
                ItemInfo i = (ItemInfo) it.next();
                if ((i.screenId != currentScreenId || (i instanceof LauncherAppWidgetInfo)) && !needToMovePage) {
                    addNotAnimated.add(i);
                } else {
                    addAnimated.add(i);
                }
            }
            Iterator it2 = addAnimated.iterator();
            while (it2.hasNext()) {
                item = (ItemInfo) it2.next();
                if (item instanceof IconInfo) {
                    ((IconInfo) item).getIcon(sIconCache);
                }
                runOnMainThread(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (callbacks == cb && cb != null) {
                            if (item.container > 0) {
                                ItemInfo folder = HomeLoader.this.getItemById(item.container);
                                if ((folder instanceof FolderInfo) && (item instanceof IconInfo)) {
                                    ((FolderInfo) folder).add((IconInfo) item);
                                    return;
                                } else {
                                    Log.e(HomeLoader.TAG, "Non Folder is exist : " + item.toString());
                                    return;
                                }
                            }
                            callbacks.bindItem(item, true);
                        }
                    }
                });
            }
            it2 = addNotAnimated.iterator();
            while (it2.hasNext()) {
                item = (ItemInfo) it2.next();
                if (item instanceof IconInfo) {
                    ((IconInfo) item).getIcon(sIconCache);
                }
                runOnMainThread(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (callbacks == cb && cb != null) {
                            if (item instanceof LauncherAppWidgetInfo) {
                                callbacks.bindAppWidget((LauncherAppWidgetInfo) item);
                            } else if (item.container > 0) {
                                ItemInfo folder = HomeLoader.this.getItemById(item.container);
                                if ((folder instanceof FolderInfo) && (item instanceof IconInfo)) {
                                    ((FolderInfo) folder).add((IconInfo) item);
                                } else {
                                    Log.e(HomeLoader.TAG, "Non Folder is exist : " + item.toString());
                                }
                            } else {
                                callbacks.bindItem(item, false);
                            }
                        }
                    }
                });
            }
        }
    }

    void removeWorkspaceItem(final ArrayList<? extends ItemInfo> workspaceApps) {
        final HomeCallbacks callbacks = getCallback();
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                final ArrayList<ItemInfo> removeItems = new ArrayList();
                synchronized (HomeLoader.sBgLock) {
                    Iterator it = workspaceApps.iterator();
                    while (it.hasNext()) {
                        ItemInfo item = (ItemInfo) it.next();
                        if (HomeLoader.sBgItemsIdMap.get(item.id) != null) {
                            removeItems.add(item);
                        } else {
                            Log.i(HomeLoader.TAG, "removeWorkspaceItem : no cached item (" + item.title + ")," + item.id);
                        }
                    }
                }
                if (!removeItems.isEmpty()) {
                    HomeLoader.this.mFavoritesUpdater.deleteItemsFromDatabase(removeItems);
                    HomeLoader.this.runOnMainThread(new Runnable() {
                        public void run() {
                            HomeCallbacks cb = HomeLoader.this.getCallback();
                            if (callbacks == cb && cb != null) {
                                cb.bindItemsRemoved(removeItems);
                            }
                        }
                    });
                }
            }
        });
    }

    void removeWorkspaceItem(boolean isWidget, int appWidgetId, String title, Intent intent, boolean duplicate) {
        final HomeCallbacks callbacks = getCallback();
        final boolean isHomeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        final boolean z = isWidget;
        final String str = title;
        final int i = appWidgetId;
        final Intent intent2 = intent;
        final boolean z2 = duplicate;
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                final ArrayList<ItemInfo> removeItems = new ArrayList();
                Log.d(HomeLoader.TAG, "removeWorkspaceItem is widget " + z + " title " + str);
                synchronized (HomeLoader.sBgLock) {
                    Iterator it = HomeLoader.this.getAllItemInHome().iterator();
                    while (it.hasNext()) {
                        ItemInfo info = (ItemInfo) it.next();
                        if (!z) {
                            boolean skipRemove = isHomeOnlyMode ? (info.itemType == 1 || info.itemType == 6) ? false : true : (info.itemType == 1 || info.itemType == 6 || info.itemType == 0) ? false : true;
                            if (skipRemove) {
                                continue;
                            } else {
                                if (intent2.getFlags() != 0 && info.getIntent().getFlags() == 0) {
                                    intent2.setFlags(0);
                                }
                                if (intent2.toUri(0).equals(info.getIntent().toUri(0))) {
                                    if (str == null || str.equals(info.title)) {
                                        removeItems.add(info);
                                    }
                                    if (!z2) {
                                        break;
                                    }
                                } else {
                                    continue;
                                }
                            }
                        } else if (info.itemType == 4) {
                            if (i == ((LauncherAppWidgetInfo) info).appWidgetId) {
                                removeItems.add(info);
                            }
                        }
                    }
                }
                if (!removeItems.isEmpty()) {
                    Log.d(HomeLoader.TAG, str + " workspace item will be removed : " + removeItems.size());
                    HomeLoader.this.mFavoritesUpdater.deleteItemsFromDatabase(removeItems);
                    HomeLoader.this.runOnMainThread(new Runnable() {
                        public void run() {
                            HomeCallbacks cb = HomeLoader.this.getCallback();
                            if (callbacks == cb && cb != null) {
                                cb.bindItemsRemoved(removeItems);
                            }
                        }
                    });
                }
            }
        });
    }

    private Pair<Long, int[]> findSpaceForItem(ArrayList<Long> workspaceScreens, ArrayList<Long> addedWorkspaceScreensFinal, int spanX, int spanY, boolean lastPosition, long screenIdForPosition) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.container == -100 && info.hidden == 0) {
                    ArrayList<ItemInfo> items = (ArrayList) screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }
        long screenId = 0;
        int[] coordinates = new int[2];
        int defaultScreenIndex = Utilities.getHomeDefaultPageKey(sContext);
        boolean needNewScreen = false;
        int screenCount = workspaceScreens.size();
        if (lastPosition) {
            for (int i = screenCount - 1; i > -1; i--) {
                screenId = ((Long) workspaceScreens.get(i)).longValue();
                if (screenItems.get(screenId) != null) {
                    break;
                }
            }
        } else if (screenIdForPosition != -1) {
            int screenIndex = workspaceScreens.indexOf(Long.valueOf(screenIdForPosition));
            if (screenIndex < 0) {
                Log.e(TAG, "findSpaceForItem - screenIndex error : " + screenIndex);
                screenId = ((Long) workspaceScreens.get(0)).longValue();
            } else {
                screenId = screenIdForPosition;
            }
        } else {
            defaultScreenIndex++;
            if (defaultScreenIndex >= workspaceScreens.size()) {
                needNewScreen = true;
            } else {
                if (defaultScreenIndex < 0) {
                    Log.e(TAG, "findSpaceForItem - defaultScreenIndex error : " + defaultScreenIndex);
                    defaultScreenIndex = 0;
                }
                screenId = ((Long) workspaceScreens.get(defaultScreenIndex)).longValue();
            }
        }
        boolean found = false;
        if (!needNewScreen) {
            found = this.mItemPositionHelper.findNextAvailableIconSpaceInScreen((ArrayList) screenItems.get(screenId), screenId, coordinates, spanX, spanY, lastPosition);
            if (!found) {
                int screen = defaultScreenIndex + 1;
                if (lastPosition || screenIdForPosition != -1) {
                    screen = workspaceScreens.indexOf(Long.valueOf(screenId)) + 1;
                }
                while (screen < screenCount) {
                    screenId = ((Long) workspaceScreens.get(screen)).longValue();
                    if (this.mItemPositionHelper.findNextAvailableIconSpaceInScreen((ArrayList) screenItems.get(screenId), screenId, coordinates, spanX, spanY, lastPosition)) {
                        found = true;
                        break;
                    }
                    screen++;
                }
            }
        }
        if (!found) {
            screenId = FavoritesProvider.getInstance().generateNewScreenId();
            workspaceScreens.add(Long.valueOf(screenId));
            addedWorkspaceScreensFinal.add(Long.valueOf(screenId));
            if (!this.mItemPositionHelper.findNextAvailableIconSpaceInScreen((ArrayList) screenItems.get(screenId), screenId, coordinates, spanX, spanY, lastPosition)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return Pair.create(Long.valueOf(screenId), coordinates);
    }

    private HomeCallbacks getCallback() {
        HomeCallbacks homeCallbacks;
        synchronized (this.mLock) {
            homeCallbacks = this.mCallbacks != null ? (HomeCallbacks) this.mCallbacks.get() : null;
        }
        return homeCallbacks;
    }

    FolderInfo findFolderById(Long folderId) {
        FolderInfo folderInfo;
        synchronized (sBgLock) {
            folderInfo = (FolderInfo) sBgFolders.get(folderId.longValue());
        }
        return folderInfo;
    }

    public void dumpState() {
        synchronized (sBgLock) {
            Object obj;
            Log.d(TAG, "HomeLoader.mContext=" + sContext);
            String str = TAG;
            StringBuilder append = new StringBuilder().append("Home PageLoaderTask.mStopped=");
            if (this.mPageLoaderTask == null) {
                obj = "task null ";
            } else {
                obj = Boolean.valueOf(((HomeLoaderTask) this.mPageLoaderTask).isStopped());
            }
            Log.d(str, append.append(obj).toString());
            Log.d(TAG, "HotSeat Items size=" + this.mBgHotseatItems.size());
            Log.d(TAG, "Workspace Items size=" + this.mBgPagesItems.size());
        }
    }

    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, String[] args, ArrayList<ItemInfo> allAppsList) {
        Iterator it;
        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "All apps list: size=" + allAppsList.size());
            it = allAppsList.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                writer.println(prefix + "   title=\"" + info.title + "\" iconBitmap=" + ((IconInfo) info).mIcon + " componentName=" + info.componentName.getPackageName());
            }
        }
        ArrayList<Long> workspaceScreens = new ArrayList();
        synchronized (sBgLock) {
            workspaceScreens.addAll(this.mBgOrderedScreens);
            MultiHashMap<ComponentKey, String> deepShortcutMap = sBgDeepShortcutMap.clone();
        }
        LongArrayMap<ItemInfo> itemsIdMap = new LongArrayMap();
        ArrayList<ItemInfo> workspaceItems = new ArrayList();
        LongArrayMap<FolderInfo> folders = new LongArrayMap();
        ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList();
        filterWorkspaceItems(itemsIdMap, workspaceItems, folders, appWidgets);
        if (args.length <= 0 || !TextUtils.equals(args[0], "--proto")) {
            int i;
            writer.println(prefix + "Data Model:");
            writer.print(prefix + " ---- workspace screens: ");
            for (i = 0; i < workspaceScreens.size(); i++) {
                writer.print(" " + ((Long) workspaceScreens.get(i)).toString());
            }
            writer.println();
            writer.println(prefix + " ---- workspace items ");
            for (i = 0; i < workspaceItems.size(); i++) {
                writer.println(prefix + '\t' + ((ItemInfo) workspaceItems.get(i)).toString());
            }
            writer.println(prefix + " ---- appwidget items ");
            for (i = 0; i < appWidgets.size(); i++) {
                writer.println(prefix + '\t' + ((LauncherAppWidgetInfo) appWidgets.get(i)).toString());
            }
            writer.println(prefix + " ---- folder items ");
            for (i = 0; i < folders.size(); i++) {
                writer.println(prefix + '\t' + ((FolderInfo) folders.valueAt(i)).toString());
            }
            writer.println(prefix + " ---- items id map ");
            for (i = 0; i < itemsIdMap.size(); i++) {
                writer.println(prefix + '\t' + ((ItemInfo) itemsIdMap.valueAt(i)).toString());
            }
            if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
                writer.println(prefix + "shortcuts");
                for (ArrayList<String> map : deepShortcutMap.values()) {
                    writer.print(prefix + "  ");
                    it = map.iterator();
                    while (it.hasNext()) {
                        PrintWriter printWriter = writer;
                        printWriter.print(((String) it.next()).toString() + ", ");
                    }
                    writer.println();
                }
                return;
            }
            return;
        }
        dumpProto(workspaceScreens, folders, workspaceItems, appWidgets, prefix, fd, writer, args);
    }

    private synchronized void dumpProto(ArrayList<Long> workspaceScreens, LongArrayMap<FolderInfo> folders, ArrayList<ItemInfo> workspaceItems, ArrayList<LauncherAppWidgetInfo> appWidgets, String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        int i;
        DumpTargetWrapper hotseat = new DumpTargetWrapper(2, 0);
        LongArrayMap<DumpTargetWrapper> workspaces = new LongArrayMap();
        for (i = 0; i < workspaceScreens.size(); i++) {
            workspaces.put(((Long) workspaceScreens.get(i)).longValue(), new DumpTargetWrapper(1, i));
        }
        GridInfo folderGrid = LauncherAppState.getInstance().getDeviceProfile().folderGrid;
        int maxCountOfFolderPage = folderGrid.getCellCountX() * folderGrid.getCellCountY();
        for (i = 0; i < folders.size(); i++) {
            FolderInfo fInfo = (FolderInfo) folders.valueAt(i);
            DumpTargetWrapper dtw = new DumpTargetWrapper(3, folders.size());
            dtw.writeToDumpTarget(fInfo);
            Iterator it = fInfo.contents.iterator();
            while (it.hasNext()) {
                ItemInfo sInfo = (IconInfo) it.next();
                DumpTargetWrapper child = new DumpTargetWrapper(sInfo, sInfo.rank / maxCountOfFolderPage);
                child.writeToDumpTarget(sInfo);
                dtw.add(child);
            }
            if (fInfo.container == -101) {
                hotseat.add(dtw);
            } else if (fInfo.container == -100) {
                ((DumpTargetWrapper) workspaces.get(fInfo.screenId)).add(dtw);
            }
        }
        for (i = 0; i < workspaceItems.size(); i++) {
            ItemInfo info = (ItemInfo) workspaceItems.get(i);
            if (!(info instanceof FolderInfo)) {
                dtw = new DumpTargetWrapper(info);
                dtw.writeToDumpTarget(info);
                if (info.container == -101) {
                    hotseat.add(dtw);
                } else if (info.container == -100) {
                    ((DumpTargetWrapper) workspaces.get(info.screenId)).add(dtw);
                }
            }
        }
        for (i = 0; i < appWidgets.size(); i++) {
            info = (ItemInfo) appWidgets.get(i);
            dtw = new DumpTargetWrapper(info);
            dtw.writeToDumpTarget(info);
            if (info.container == -101) {
                hotseat.add(dtw);
            } else if (info.container == -100) {
                ((DumpTargetWrapper) workspaces.get(info.screenId)).add(dtw);
            }
        }
        ArrayList<DumpTarget> targetList = new ArrayList();
        targetList.addAll(hotseat.getFlattenedList());
        for (i = 0; i < workspaces.size(); i++) {
            targetList.addAll(((DumpTargetWrapper) workspaces.valueAt(i)).getFlattenedList());
        }
        if (args.length <= 1 || !TextUtils.equals(args[1], "--debug")) {
            LauncherImpression proto = new LauncherImpression();
            proto.targets = new DumpTarget[targetList.size()];
            for (i = 0; i < targetList.size(); i++) {
                proto.targets[i] = (DumpTarget) targetList.get(i);
            }
            try {
                new FileOutputStream(fd).write(MessageNano.toByteArray(proto));
                Log.d(TAG, MessageNano.toByteArray(proto).length + "Bytes");
            } catch (IOException e) {
                Log.e(TAG, "Exception writing dumpsys --proto", e);
            }
        } else {
            for (i = 0; i < targetList.size(); i++) {
                writer.println(prefix + DumpTargetWrapper.getDumpTargetStr((DumpTarget) targetList.get(i)));
            }
        }
    }

    private void filterWorkspaceItems(LongArrayMap<ItemInfo> allWorkspaceItems, ArrayList<ItemInfo> iconItems, LongArrayMap<FolderInfo> folders, ArrayList<LauncherAppWidgetInfo> widgets) {
        ArrayList<Long> folderIds = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it.next();
                if (folderInfo.container == -100 || folderInfo.container == -101) {
                    folderIds.add(Long.valueOf(folderInfo.id));
                    folders.put(folderInfo.id, folderInfo);
                }
            }
            Iterator it2 = sBgItemsIdMap.iterator();
            while (it2.hasNext()) {
                ItemInfo info = (ItemInfo) it2.next();
                if (info.container == -100 || info.container == -101 || folderIds.contains(Long.valueOf(info.container))) {
                    if (info.itemType == 5 || info.itemType == 4) {
                        widgets.add((LauncherAppWidgetInfo) info);
                    } else if (!(info.itemType == 2 || info.itemType == 7)) {
                        iconItems.add(info);
                    }
                    allWorkspaceItems.put(info.id, info);
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addOrUpdater(java.lang.String[] r29, java.util.ArrayList<com.android.launcher3.common.base.item.IconInfo> r30, java.util.HashMap<android.content.ComponentName, com.android.launcher3.common.base.item.IconInfo> r31, com.android.launcher3.common.compat.UserHandleCompat r32) {
        /*
        r28 = this;
        r5 = new java.util.ArrayList;
        r5.<init>();
        r6 = new java.util.ArrayList;
        r6.<init>();
        r25 = new java.util.ArrayList;
        r25.<init>();
        r20 = new java.util.HashSet;
        r2 = java.util.Arrays.asList(r29);
        r0 = r20;
        r0.<init>(r2);
        r3 = sBgLock;
        monitor-enter(r3);
        r9 = r28.getAllItemInHome();	 Catch:{ all -> 0x0103 }
        r2 = r9.iterator();	 Catch:{ all -> 0x0103 }
    L_0x0025:
        r7 = r2.hasNext();	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x027c;
    L_0x002b:
        r16 = r2.next();	 Catch:{ all -> 0x0103 }
        r16 = (com.android.launcher3.common.base.item.ItemInfo) r16;	 Catch:{ all -> 0x0103 }
        r0 = r16;
        r7 = r0 instanceof com.android.launcher3.common.base.item.IconInfo;	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0215;
    L_0x0037:
        r0 = r16;
        r7 = r0.user;	 Catch:{ all -> 0x0103 }
        r0 = r32;
        r7 = r0.equals(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0215;
    L_0x0043:
        r0 = r16;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0103 }
        r14 = r0;
        r17 = 0;
        r15 = 0;
        r7 = r14.iconResource;	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x008f;
    L_0x004f:
        r7 = r14.iconResource;	 Catch:{ all -> 0x0103 }
        r7 = r7.packageName;	 Catch:{ all -> 0x0103 }
        r0 = r20;
        r7 = r0.contains(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x008f;
    L_0x005b:
        r7 = r14.iconResource;	 Catch:{ all -> 0x0103 }
        r7 = r7.packageName;	 Catch:{ all -> 0x0103 }
        r0 = r14.iconResource;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r0 = r26;
        r0 = r0.resourceName;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r27 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r26;
        r1 = r27;
        r13 = com.android.launcher3.util.BitmapUtils.createIconBitmap(r7, r0, r1);	 Catch:{ all -> 0x0103 }
        if (r13 == 0) goto L_0x008f;
    L_0x0075:
        r14.setOriginalIcon(r13);	 Catch:{ all -> 0x0103 }
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r14.componentName;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r0 = r26;
        r13 = com.android.launcher3.util.ShortcutTray.getIcon(r7, r13, r0);	 Catch:{ all -> 0x0103 }
        r7 = 1;
        r14.customIcon = r7;	 Catch:{ all -> 0x0103 }
        r14.setIcon(r13);	 Catch:{ all -> 0x0103 }
        r7 = 0;
        r14.usingFallbackIcon = r7;	 Catch:{ all -> 0x0103 }
        r17 = 1;
    L_0x008f:
        r12 = r14.getTargetComponent();	 Catch:{ all -> 0x0103 }
        if (r12 == 0) goto L_0x0137;
    L_0x0095:
        r7 = r12.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r20;
        r7 = r0.contains(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0137;
    L_0x00a1:
        r0 = r31;
        r10 = r0.get(r12);	 Catch:{ all -> 0x0103 }
        r10 = (com.android.launcher3.common.base.item.IconInfo) r10;	 Catch:{ all -> 0x0103 }
        r7 = r14.isPromise();	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x01e1;
    L_0x00af:
        r7 = sLauncherApps;	 Catch:{ all -> 0x0103 }
        r26 = r12.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r26;
        r1 = r32;
        r11 = r7.getActivityList(r0, r1);	 Catch:{ all -> 0x0103 }
        r0 = r32;
        r8 = com.android.launcher3.common.model.DataLoader.findActivityInfo(r11, r12, r0);	 Catch:{ all -> 0x0103 }
        r7 = 102; // 0x66 float:1.43E-43 double:5.04E-322;
        r7 = r14.hasStatusFlag(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x010a;
    L_0x00cb:
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r22 = r7.getPackageManager();	 Catch:{ all -> 0x0103 }
        r7 = new android.content.Intent;	 Catch:{ all -> 0x0103 }
        r26 = "android.intent.action.MAIN";
        r0 = r26;
        r7.<init>(r0);	 Catch:{ all -> 0x0103 }
        r7 = r7.setComponent(r12);	 Catch:{ all -> 0x0103 }
        r26 = "android.intent.category.LAUNCHER";
        r0 = r26;
        r7 = r7.addCategory(r0);	 Catch:{ all -> 0x0103 }
        r26 = 65536; // 0x10000 float:9.18355E-41 double:3.2379E-319;
        r0 = r22;
        r1 = r26;
        r19 = r0.resolveActivity(r7, r1);	 Catch:{ all -> 0x0103 }
        if (r19 != 0) goto L_0x010a;
    L_0x00f2:
        r7 = r12.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r22;
        r18 = r0.getLaunchIntentForPackage(r7);	 Catch:{ all -> 0x0103 }
        if (r18 != 0) goto L_0x0106;
    L_0x00fe:
        r6.add(r14);	 Catch:{ all -> 0x0103 }
        goto L_0x0025;
    L_0x0103:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x0103 }
        throw r2;
    L_0x0106:
        r0 = r18;
        r14.promisedIntent = r0;	 Catch:{ all -> 0x0103 }
    L_0x010a:
        if (r8 == 0) goto L_0x0112;
    L_0x010c:
        r7 = com.android.launcher3.common.base.item.IconInfo.initFlags(r8);	 Catch:{ all -> 0x0103 }
        r14.flags = r7;	 Catch:{ all -> 0x0103 }
    L_0x0112:
        r7 = r14.promisedIntent;	 Catch:{ all -> 0x0103 }
        r14.intent = r7;	 Catch:{ all -> 0x0103 }
        r7 = 0;
        r14.promisedIntent = r7;	 Catch:{ all -> 0x0103 }
        r7 = 0;
        r14.status = r7;	 Catch:{ all -> 0x0103 }
        r7 = r14.itemType;	 Catch:{ all -> 0x0103 }
        if (r7 != 0) goto L_0x0123;
    L_0x0120:
        r7 = 0;
        r14.customIcon = r7;	 Catch:{ all -> 0x0103 }
    L_0x0123:
        r17 = 1;
        r7 = sIconCache;	 Catch:{ all -> 0x0103 }
        r14.updateIcon(r7);	 Catch:{ all -> 0x0103 }
    L_0x012a:
        r7 = r14.isDisabled;	 Catch:{ all -> 0x0103 }
        r7 = r7 & 2;
        if (r7 == 0) goto L_0x0206;
    L_0x0130:
        r7 = r14.isDisabled;	 Catch:{ all -> 0x0103 }
        r7 = r7 & -3;
        r14.isDisabled = r7;	 Catch:{ all -> 0x0103 }
        r15 = 1;
    L_0x0137:
        if (r17 != 0) goto L_0x013b;
    L_0x0139:
        if (r15 == 0) goto L_0x013e;
    L_0x013b:
        r5.add(r14);	 Catch:{ all -> 0x0103 }
    L_0x013e:
        if (r17 == 0) goto L_0x0155;
    L_0x0140:
        r23 = new android.content.ContentValues;	 Catch:{ all -> 0x0103 }
        r23.<init>();	 Catch:{ all -> 0x0103 }
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r23;
        r14.onAddToDatabase(r7, r0);	 Catch:{ all -> 0x0103 }
        r0 = r28;
        r7 = r0.mFavoritesUpdater;	 Catch:{ all -> 0x0103 }
        r0 = r23;
        r7.updateItem(r0, r14);	 Catch:{ all -> 0x0103 }
    L_0x0155:
        r0 = r16;
        r7 = r0 instanceof com.android.launcher3.home.LauncherPairAppsInfo;	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x015b:
        r0 = r16;
        r0 = (com.android.launcher3.home.LauncherPairAppsInfo) r0;	 Catch:{ all -> 0x0103 }
        r21 = r0;
        r0 = r21;
        r7 = r0.mFirstApp;	 Catch:{ all -> 0x0103 }
        r7 = r7.getCN();	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x017f;
    L_0x016b:
        r0 = r21;
        r7 = r0.mFirstApp;	 Catch:{ all -> 0x0103 }
        r7 = r7.getCN();	 Catch:{ all -> 0x0103 }
        r7 = r7.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r20;
        r7 = r0.contains(r7);	 Catch:{ all -> 0x0103 }
        if (r7 != 0) goto L_0x019d;
    L_0x017f:
        r0 = r21;
        r7 = r0.mSecondApp;	 Catch:{ all -> 0x0103 }
        r7 = r7.getCN();	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x0189:
        r0 = r21;
        r7 = r0.mSecondApp;	 Catch:{ all -> 0x0103 }
        r7 = r7.getCN();	 Catch:{ all -> 0x0103 }
        r7 = r7.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r20;
        r7 = r0.contains(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x019d:
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r0 = r0.mFirstApp;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r0 = r21;
        r0 = r0.mSecondApp;	 Catch:{ all -> 0x0103 }
        r27 = r0;
        r0 = r26;
        r1 = r27;
        r13 = com.android.launcher3.util.PairAppsUtilities.buildIcon(r7, r0, r1);	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r0.setIcon(r13);	 Catch:{ all -> 0x0103 }
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r0 = r0.mFirstApp;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r26 = r26.getCN();	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r0 = r0.mSecondApp;	 Catch:{ all -> 0x0103 }
        r27 = r0;
        r27 = r27.getCN();	 Catch:{ all -> 0x0103 }
        r0 = r26;
        r1 = r27;
        r7 = com.android.launcher3.util.PairAppsUtilities.buildLabel(r7, r0, r1);	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r0.title = r7;	 Catch:{ all -> 0x0103 }
        r0 = r21;
        r5.add(r0);	 Catch:{ all -> 0x0103 }
        goto L_0x0025;
    L_0x01e1:
        if (r10 == 0) goto L_0x012a;
    L_0x01e3:
        r7 = "android.intent.action.MAIN";
        r0 = r14.intent;	 Catch:{ all -> 0x0103 }
        r26 = r0;
        r26 = r26.getAction();	 Catch:{ all -> 0x0103 }
        r0 = r26;
        r7 = r7.equals(r0);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x012a;
    L_0x01f5:
        r7 = r14.itemType;	 Catch:{ all -> 0x0103 }
        if (r7 != 0) goto L_0x012a;
    L_0x01f9:
        r7 = sIconCache;	 Catch:{ all -> 0x0103 }
        r14.updateIcon(r7);	 Catch:{ all -> 0x0103 }
        r7 = r10.contentDescription;	 Catch:{ all -> 0x0103 }
        r14.contentDescription = r7;	 Catch:{ all -> 0x0103 }
        r17 = 1;
        goto L_0x012a;
    L_0x0206:
        r7 = r14.isDisabled;	 Catch:{ all -> 0x0103 }
        r7 = r7 & 32;
        if (r7 == 0) goto L_0x0137;
    L_0x020c:
        r7 = r14.isDisabled;	 Catch:{ all -> 0x0103 }
        r7 = r7 & -33;
        r14.isDisabled = r7;	 Catch:{ all -> 0x0103 }
        r15 = 1;
        goto L_0x0137;
    L_0x0215:
        r0 = r16;
        r7 = r0 instanceof com.android.launcher3.home.LauncherAppWidgetInfo;	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x021b:
        r0 = r16;
        r0 = (com.android.launcher3.home.LauncherAppWidgetInfo) r0;	 Catch:{ all -> 0x0103 }
        r24 = r0;
        r0 = r24;
        r7 = r0.user;	 Catch:{ all -> 0x0103 }
        r0 = r32;
        r7 = r0.equals(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x022d:
        r7 = 2;
        r0 = r24;
        r7 = r0.hasRestoreFlag(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x0236:
        r0 = r24;
        r7 = r0.providerName;	 Catch:{ all -> 0x0103 }
        r7 = r7.getPackageName();	 Catch:{ all -> 0x0103 }
        r0 = r20;
        r7 = r0.contains(r7);	 Catch:{ all -> 0x0103 }
        if (r7 == 0) goto L_0x0025;
    L_0x0246:
        r0 = r24;
        r7 = r0.restoreStatus;	 Catch:{ all -> 0x0103 }
        r7 = r7 & -11;
        r0 = r24;
        r0.restoreStatus = r7;	 Catch:{ all -> 0x0103 }
        r0 = r24;
        r7 = r0.restoreStatus;	 Catch:{ all -> 0x0103 }
        r7 = r7 | 4;
        r0 = r24;
        r0.restoreStatus = r7;	 Catch:{ all -> 0x0103 }
        r0 = r25;
        r1 = r24;
        r0.add(r1);	 Catch:{ all -> 0x0103 }
        r23 = new android.content.ContentValues;	 Catch:{ all -> 0x0103 }
        r23.<init>();	 Catch:{ all -> 0x0103 }
        r7 = sContext;	 Catch:{ all -> 0x0103 }
        r0 = r24;
        r1 = r23;
        r0.onAddToDatabase(r7, r1);	 Catch:{ all -> 0x0103 }
        r0 = r28;
        r7 = r0.mFavoritesUpdater;	 Catch:{ all -> 0x0103 }
        r0 = r23;
        r1 = r24;
        r7.updateItem(r0, r1);	 Catch:{ all -> 0x0103 }
        goto L_0x0025;
    L_0x027c:
        monitor-exit(r3);	 Catch:{ all -> 0x0103 }
        r4 = r28.getCallback();
        if (r4 != 0) goto L_0x028b;
    L_0x0283:
        r2 = "HomeLoader";
        r3 = "addOrUpdater. Nobody to tell about the new app.  Launcher is probably loading.";
        android.util.Log.w(r2, r3);
    L_0x028a:
        return;
    L_0x028b:
        r2 = com.android.launcher3.LauncherFeature.supportHomeModeChange();
        if (r2 == 0) goto L_0x02b9;
    L_0x0291:
        r2 = com.android.launcher3.LauncherAppState.getInstance();
        r2 = r2.isHomeOnlyModeEnabled();
        if (r2 == 0) goto L_0x02b9;
    L_0x029b:
        if (r30 == 0) goto L_0x02b9;
    L_0x029d:
        r2 = r30.isEmpty();
        if (r2 != 0) goto L_0x02b9;
    L_0x02a3:
        r2 = new java.util.ArrayList;
        r0 = r30;
        r2.<init>(r0);
        r0 = r28;
        r0.restoreStkPositionIfNecessary(r2);
        r2 = sContext;
        r3 = 0;
        r0 = r28;
        r1 = r30;
        r0.addAndBindAddedWorkspaceItems(r2, r1, r3);
    L_0x02b9:
        r2 = r5.isEmpty();
        if (r2 == 0) goto L_0x02c5;
    L_0x02bf:
        r2 = r6.isEmpty();
        if (r2 != 0) goto L_0x02e2;
    L_0x02c5:
        r26 = sHandler;
        r2 = new com.android.launcher3.home.HomeLoader$22;
        r3 = r28;
        r7 = r32;
        r2.<init>(r4, r5, r6, r7);
        r0 = r26;
        r0.post(r2);
        r2 = r6.isEmpty();
        if (r2 != 0) goto L_0x02e2;
    L_0x02db:
        r0 = r28;
        r2 = r0.mFavoritesUpdater;
        r2.deleteItemsFromDatabase(r6);
    L_0x02e2:
        r2 = r25.isEmpty();
        if (r2 != 0) goto L_0x028a;
    L_0x02e8:
        r2 = sHandler;
        r3 = new com.android.launcher3.home.HomeLoader$23;
        r0 = r28;
        r1 = r25;
        r3.<init>(r4, r1);
        r2.post(r3);
        goto L_0x028a;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.addOrUpdater(java.lang.String[], java.util.ArrayList, java.util.HashMap, com.android.launcher3.common.compat.UserHandleCompat):void");
    }

    public void showApps(ArrayList<ItemInfo> items, boolean isGameApp) {
        updateAndBindItems(items, isGameApp);
    }

    public void hideApps(ArrayList<ItemInfo> items, boolean isGameApp) {
        updateHideItems(items, isGameApp);
    }

    private void updateAndBindItems(ArrayList<ItemInfo> updateItems, boolean isGameApp) {
        final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList();
        ArrayList<Long> workspaceScreens = sFavoritesProvider.loadScreensFromDb();
        ArrayList<ContentValues> contentValuesList = new ArrayList();
        Iterator it = updateItems.iterator();
        while (it.hasNext()) {
            ItemInfo itemInfo = (ItemInfo) it.next();
            itemInfo.container = -100;
            itemInfo.screenId = -1;
            Pair<Long, int[]> coords = findSpaceForItem(workspaceScreens, addedWorkspaceScreensFinal, itemInfo.spanX, itemInfo.spanY, false, -1);
            int[] coordinates = coords.second;
            itemInfo.screenId = ((Long) coords.first).longValue();
            itemInfo.cellX = coordinates[0];
            itemInfo.cellY = coordinates[1];
            itemInfo.hidden = itemInfo.setUnHidden(isGameApp ? 4 : 2);
            ContentValues contentValues = new ContentValues();
            contentValues.put("container", Long.valueOf(itemInfo.container));
            contentValues.put("cellX", Integer.valueOf(itemInfo.cellX));
            contentValues.put("cellY", Integer.valueOf(itemInfo.cellY));
            contentValues.put("screen", Long.valueOf(itemInfo.screenId));
            contentValues.put("hidden", Integer.valueOf(itemInfo.hidden));
            contentValuesList.add(contentValues);
        }
        this.mFavoritesUpdater.updateItemsInDatabaseHelper(sContext, contentValuesList, updateItems);
        this.mFavoritesUpdater.updateScreenOrder(sContext, workspaceScreens);
        final HomeCallbacks callbacks = getCallback();
        final ArrayList<ItemInfo> arrayList = updateItems;
        sHandler.post(new Runnable() {
            public void run() {
                HomeCallbacks cb = HomeLoader.this.getCallback();
                if (cb != null && callbacks == cb) {
                    callbacks.bindAppsAdded(addedWorkspaceScreensFinal, arrayList, null);
                }
            }
        });
    }

    private void updateHideItems(ArrayList<ItemInfo> items, boolean isGameApp) {
        ArrayList<ContentValues> contentValues = new ArrayList();
        final ArrayList<ItemInfo> hideItems = new ArrayList();
        final ArrayList<ItemInfo> itemsInFolder = new ArrayList();
        final ArrayList<FolderInfo> folderInfos = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            ComponentName componentName = item.componentName;
            if (componentName == null) {
                componentName = ((IconInfo) item).getTargetComponent();
            }
            if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                item.hidden = item.setHidden(isGameApp ? 4 : 2);
                ContentValues values = new ContentValues();
                values.put("hidden", Integer.valueOf(item.hidden));
                values.put("screen", Integer.valueOf(-1));
                if (item.container == -100 || item.container == -101) {
                    hideItems.add(item);
                    removePagesItem(item);
                } else {
                    synchronized (sBgLock) {
                        folderInfos.add(sBgFolders.get(item.container));
                    }
                    itemsInFolder.add(item);
                    item.container = -100;
                    values.put("container", Long.valueOf(item.container));
                }
                contentValues.add(values);
            } else {
                hideItems.add(item);
                Iterator it2 = DataLoader.getItemInfoByComponentName(componentName, item.user, true).iterator();
                while (it2.hasNext()) {
                    ItemInfo itemByComponentName = (ItemInfo) it2.next();
                    if (!isAllAppItemInApps(itemByComponentName)) {
                        hideItems.add(itemByComponentName);
                        this.mFavoritesUpdater.deleteItem(itemByComponentName);
                    }
                }
            }
        }
        if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            this.mFavoritesUpdater.updateItemsInDatabaseHelper(sContext, contentValues, items);
        }
        final HomeCallbacks callbacks = getCallback();
        sHandler.post(new Runnable() {
            public void run() {
                HomeCallbacks cb = HomeLoader.this.getCallback();
                if (cb != null && callbacks == cb) {
                    cb.bindAppsInFolderRemoved(folderInfos, itemsInFolder);
                    cb.bindItemsRemoved(hideItems);
                }
            }
        });
    }

    public void updateItemsOnlyDB(ArrayList<ItemInfo> items) {
        ArrayList<ContentValues> contentValues = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            ContentValues values = new ContentValues();
            values.put("hidden", Integer.valueOf(item.hidden));
            contentValues.add(values);
        }
        this.mFavoritesUpdater.updateItemsInDatabaseHelper(sContext, contentValues, items);
    }

    public ArrayList<ItemInfo> removePackagesAndComponents(ArrayList<String> removedPackageNames, ArrayList<ItemInfo> removedApps, UserHandleCompat user, int reason) {
        final HashSet<ComponentName> removedComponents = new HashSet();
        ArrayList<ItemInfo> removedAppsInfos = new ArrayList();
        if (reason == 0) {
            Iterator it = removedPackageNames.iterator();
            while (it.hasNext()) {
                this.mFavoritesUpdater.deletePackageFromDatabase((String) it.next(), user);
            }
            Iterator it2 = removedApps.iterator();
            while (it2.hasNext()) {
                ItemInfo a = (ItemInfo) it2.next();
                ComponentName cn = a instanceof LauncherAppWidgetInfo ? ((LauncherAppWidgetInfo) a).providerName : a.componentName;
                this.mFavoritesUpdater.deleteItemsFromDatabase(DataLoader.getItemInfoByComponentName(cn, user, false));
                removedComponents.add(cn);
                removedAppsInfos.add(a);
            }
        }
        ExternalRequestQueue.removeFromExternalRequestQueue(sContext, (ArrayList) removedPackageNames, user);
        final HomeCallbacks callbacks = getCallback();
        if (callbacks == null) {
            Log.w(TAG, "removePackagesAndComponents Nobody to tell about the new app.  Launcher is probably loading.");
        } else {
            final ArrayList<String> arrayList = removedPackageNames;
            final UserHandleCompat userHandleCompat = user;
            final int i = reason;
            sHandler.post(new Runnable() {
                public void run() {
                    if (callbacks == HomeLoader.this.getCallback() && callbacks != null) {
                        callbacks.bindComponentsRemoved(arrayList, removedComponents, userHandleCompat, i);
                    }
                }
            });
        }
        return removedAppsInfos;
    }

    ItemInfo getItemById(long key) {
        ItemInfo itemInfo;
        synchronized (sBgLock) {
            itemInfo = (ItemInfo) sBgItemsIdMap.get(key);
        }
        return itemInfo;
    }

    public void bindItemsSync(int synchronousBindPage, DataLoaderState task) {
        final HomeCallbacks oldCallbacks = getCallback();
        if (oldCallbacks == null) {
            Log.w(TAG, "bindItemsSync running with no launcher");
            return;
        }
        long currentScreenId;
        Iterator it;
        if (LauncherFeature.supportFestivalPage()) {
            initFestivalPageIfNecessary(task);
        }
        ArrayList<Long> orderedScreenIds = new ArrayList();
        synchronized (sBgLock) {
            orderedScreenIds.addAll(this.mBgOrderedScreens);
        }
        int currScreen = synchronousBindPage != -1001 ? synchronousBindPage : oldCallbacks.getCurrentWorkspaceScreen();
        if (currScreen >= orderedScreenIds.size()) {
            currScreen = -1001;
        }
        final int currentScreen = currScreen;
        if (currentScreen < 0) {
            currentScreenId = -1;
        } else {
            currentScreenId = ((Long) orderedScreenIds.get(currentScreen)).longValue();
        }
        unbindItemsOnMainThread();
        ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList();
        ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList();
        synchronized (sBgLock) {
            for (Long longValue : this.mBgPagesItems.keySet()) {
                long screenId = longValue.longValue();
                if (screenId == currentScreenId) {
                    currentWorkspaceItems.addAll((Collection) this.mBgPagesItems.get(Long.valueOf(screenId)));
                } else {
                    otherWorkspaceItems.addAll((Collection) this.mBgPagesItems.get(Long.valueOf(screenId)));
                }
            }
        }
        final DataLoaderState dataLoaderState = task;
        runOnMainThread(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                if (callbacks != null) {
                    callbacks.startBinding();
                }
            }
        });
        synchronized (sBgLock) {
            Iterator it2 = sBgFolders.iterator();
            while (it2.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it2.next();
                if (folderInfo.container == -100 || folderInfo.container == -101) {
                    ArrayList<IconInfo> removeList = new ArrayList();
                    it = folderInfo.contents.iterator();
                    while (it.hasNext()) {
                        IconInfo info = (IconInfo) it.next();
                        if (!sBgItemsIdMap.containsKey(info.id)) {
                            Log.d(TAG, "this item is not exist in BgItemsIdMap. so remove : " + info);
                            removeList.add(info);
                        }
                    }
                    it = removeList.iterator();
                    while (it.hasNext()) {
                        folderInfo.contents.remove((IconInfo) it.next());
                    }
                }
            }
        }
        bindWorkspaceScreens(oldCallbacks, orderedScreenIds, task);
        bindPageItems(this.mBgHotseatItems, null, task);
        bindPageItems(currentWorkspaceItems, null, task);
        dataLoaderState = task;
        runOnMainThread(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                if (callbacks != null && currentScreen != -1001) {
                    callbacks.onPageBoundSynchronously(currentScreen);
                }
            }
        });
        clearDeferredBindRunnable();
        bindPageItems(otherWorkspaceItems, this.mDeferredBindRunnables, task);
        if (LauncherFeature.supportFestivalPage()) {
            dataLoaderState = task;
            addDeferredBindRunnable(new Runnable() {
                public void run() {
                    HomeLoader.this.bindFestivalPageIfNecessary(dataLoaderState);
                }
            });
        }
        dataLoaderState = task;
        addDeferredBindRunnable(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, dataLoaderState);
                if (callbacks != null) {
                    callbacks.finishBindingItems();
                }
                HomeLoader.this.runAllBindCompleteRunnables();
            }
        });
    }

    public void filterCurrentPageItems(long currentScreenId, ArrayList<ItemInfo> allWorkspaceItems, ArrayList<ItemInfo> currentScreenItems, ArrayList<ItemInfo> otherScreenItems) {
        Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
        while (iter.hasNext()) {
            if (((ItemInfo) iter.next()) == null) {
                iter.remove();
            }
        }
        Set<Long> itemsOnScreen = new HashSet();
        Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                return (int) (lhs.container - rhs.container);
            }
        });
        Iterator it = allWorkspaceItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info.container == -100) {
                if (info.screenId == currentScreenId) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(Long.valueOf(info.id));
                } else {
                    otherScreenItems.add(info);
                }
            } else if (info.container == -101) {
                currentScreenItems.add(info);
                itemsOnScreen.add(Long.valueOf(info.id));
            } else if (itemsOnScreen.contains(Long.valueOf(info.container))) {
                currentScreenItems.add(info);
                itemsOnScreen.add(Long.valueOf(info.id));
            } else {
                otherScreenItems.add(info);
            }
        }
    }

    void bindRemainingSynchronousPages() {
        if (!this.mDeferredBindRunnables.isEmpty()) {
            synchronized (this.mDeferredBindRunnables) {
                Runnable[] deferredBindRunnables = (Runnable[]) this.mDeferredBindRunnables.toArray(new Runnable[this.mDeferredBindRunnables.size()]);
                this.mDeferredBindRunnables.clear();
            }
            for (Runnable r : deferredBindRunnables) {
                sHandler.post(r);
            }
        }
    }

    IconInfo infoFromShortcutIntent(Context context, Intent data) {
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        Parcelable bitmap = data.getParcelableExtra("android.intent.extra.shortcut.ICON");
        UserHandleCompat intentUser = DualAppUtils.supportDualApp(context) ? UserHandleCompat.fromIntent(data) : null;
        if (intent == null) {
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }
        Bitmap icon = null;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;
        if (bitmap instanceof Bitmap) {
            icon = BitmapUtils.createIconBitmap((Bitmap) bitmap, context);
        } else {
            Parcelable extra = data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (extra instanceof ShortcutIconResource) {
                iconResource = (ShortcutIconResource) extra;
                icon = BitmapUtils.createIconBitmap(iconResource.packageName, iconResource.resourceName, context);
            }
        }
        IconInfo info = new IconInfo();
        info.itemType = 1;
        if (icon != null) {
            info.setOriginalIcon(icon);
            customIcon = true;
        }
        if (intentUser == null) {
            intentUser = UserHandleCompat.myUserHandle();
        }
        info.user = intentUser;
        Log.d(TAG, "infoFromShortcutIntent EXTRA_USER " + info.user.toString());
        if (IconView.isKnoxShortcut(intent)) {
            long userSerial = (long) intent.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, (int) sUserManager.getSerialNumberForUser(info.user));
            if (userSerial >= 100) {
                info.user = sUserManager.getUserForSerialNumber(userSerial);
            }
        }
        if (icon == null) {
            icon = sIconCache.getDefaultIcon(info.user);
            info.usingFallbackIcon = true;
        }
        if (customIcon) {
            icon = ShortcutTray.getIcon(context, icon, intent.getComponent());
            if (DualAppUtils.supportDualApp(context) && DualAppUtils.isDualAppId(info.user)) {
                icon = DualAppUtils.makeUserBadgedIcon(context, icon, sProfile.homeGrid.getIconSize(), info.user.getUser());
            }
        }
        info.setIcon(icon);
        if (!(intent.getComponent() == null || IconView.isKnoxShortcut(intent))) {
            ResolveInfo resolveInfo = sContext.getPackageManager().resolveActivity(intent, 0);
            if (resolveInfo != null) {
                LauncherActivityInfoCompat launcherInfo = LauncherActivityInfoCompat.fromResolveInfo(resolveInfo, sContext);
                if (launcherInfo != null) {
                    info.flags = IconInfo.initFlags(launcherInfo);
                }
            }
        }
        info.componentName = intent.getComponent();
        info.title = Utilities.trim(name);
        info.contentDescription = sUserManager.getBadgedLabelForUser(info.title, info.user);
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    private void runAllBindCompleteRunnables() {
        Log.i(TAG, "runAllBindCompleteRunnables, count : " + sBindCompleteRunnables.size());
        if (!sBindCompleteRunnables.isEmpty()) {
            synchronized (sBindCompleteRunnables) {
                Iterator it = sBindCompleteRunnables.iterator();
                while (it.hasNext()) {
                    DataLoader.runOnWorkerThread((Runnable) it.next());
                }
                sBindCompleteRunnables.clear();
            }
        }
    }

    protected DataUpdater getUpdater() {
        return this.mFavoritesUpdater;
    }

    public int getWorkspaceScreenCount(boolean fromDb) {
        if (fromDb) {
            return sFavoritesProvider.loadScreensFromDb().size();
        }
        if (this.mBgOrderedScreens.size() == 0) {
            this.mBgOrderedScreens.addAll(sFavoritesProvider.loadScreensFromDb());
        }
        return this.mBgOrderedScreens.size();
    }

    int getWorkspaceScreenCount() {
        return getWorkspaceScreenCount(false);
    }

    long getWorkspaceScreenId(int rank) {
        if (getWorkspaceScreenCount() >= rank) {
            return ((Long) this.mBgOrderedScreens.get(rank)).longValue();
        }
        Log.e(TAG, "wrong rank value for screen requested");
        return -1;
    }

    public int getHotseatItemCount() {
        if (this.mIsFirstBind) {
            return sFavoritesProvider.loadHotseatCount();
        }
        return this.mBgHotseatItems.size();
    }

    private void reArrangHotseatItemsByRemoved(int removedIndex) {
        synchronized (sBgLock) {
            ArrayList<ItemInfo> needToRearrangeItems = new ArrayList();
            Iterator it = this.mBgHotseatItems.iterator();
            while (it.hasNext()) {
                ItemInfo hotseatItem = (ItemInfo) it.next();
                if (hotseatItem.screenId > ((long) removedIndex)) {
                    needToRearrangeItems.add(hotseatItem);
                }
            }
            if (needToRearrangeItems.size() > 0) {
                reArrangeHotseatData(needToRearrangeItems, removedIndex);
            }
        }
    }

    private void reArrangeHotseatData(ArrayList<ItemInfo> homeItems, int startPosition) {
        ArrayList<ItemInfo> hotseatItems = new ArrayList();
        Iterator it = homeItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.container == -101) {
                hotseatItems.add(item);
            }
        }
        if (hotseatItems.size() > 0) {
            Collections.sort(hotseatItems, HOTSEAT_COMPARATOR);
            int itemPosition = startPosition;
            it = hotseatItems.iterator();
            while (it.hasNext()) {
                item = (ItemInfo) it.next();
                ContentValues values = new ContentValues();
                if (item.screenId != ((long) itemPosition)) {
                    item.screenId = (long) itemPosition;
                    values.put("screen", Long.valueOf(item.screenId));
                }
                int calculatedCellX = getCellXFromHotseatOrder((int) item.screenId);
                int calculatedCellY = getCellYFromHotseatOrder((int) item.screenId);
                if (!(item.cellX == calculatedCellX && item.cellY == calculatedCellY)) {
                    item.cellX = calculatedCellX;
                    item.cellY = calculatedCellY;
                    values.put("cellX", Integer.valueOf(item.cellX));
                    values.put("cellY", Integer.valueOf(item.cellY));
                }
                if (values.size() > 0) {
                    this.mFavoritesUpdater.updateItem(values, item);
                }
                itemPosition++;
            }
        }
    }

    public void addPagesItem(ItemInfo item) {
        if (item.screenId >= 0 && item.hidden == 0) {
            synchronized (sBgLock) {
                if (item.container == -101) {
                    this.mBgHotseatItems.add(item);
                    SALogging.getInstance().updateLogValuesForHomeItems();
                } else if (item.container == -100) {
                    if (this.mBgPagesItems.get(Long.valueOf(item.screenId)) == null) {
                        this.mBgPagesItems.put(Long.valueOf(item.screenId), new ArrayList());
                    }
                    ((ArrayList) this.mBgPagesItems.get(Long.valueOf(item.screenId))).add(item);
                    SALogging.getInstance().updateLogValuesForHomeItems();
                } else {
                    Log.d(TAG, "addPagesItem : input item container error " + item.container);
                }
            }
        }
    }

    public void removePagesItem(ItemInfo item) {
        synchronized (sBgLock) {
            if (!this.mBgHotseatItems.contains(item)) {
                for (Long screenId : this.mBgPagesItems.keySet()) {
                    if (((ArrayList) this.mBgPagesItems.get(screenId)).contains(item)) {
                        ((ArrayList) this.mBgPagesItems.get(screenId)).remove(item);
                        SALogging.getInstance().updateLogValuesForHomeItems();
                        break;
                    }
                }
            }
            this.mBgHotseatItems.remove(item);
            SALogging.getInstance().updateLogValuesForHomeItems();
        }
    }

    public void updatePagesItem(ItemInfo item) {
        if (!this.mBgHotseatItems.contains(item)) {
            for (Long screenId : this.mBgPagesItems.keySet()) {
                if (((ArrayList) this.mBgPagesItems.get(screenId)).contains(item)) {
                    if (!(screenId.longValue() == item.screenId && item.container == -100)) {
                        ((ArrayList) this.mBgPagesItems.get(screenId)).remove(item);
                        addPagesItem(item);
                    }
                    SALogging.getInstance().updateLogValuesForHomeItems();
                    return;
                }
            }
        } else if (item.hidden != 0) {
            this.mBgHotseatItems.remove(item);
            SALogging.getInstance().updateLogValuesForHomeItems();
        } else if (item.container != -101) {
            this.mBgHotseatItems.remove(item);
            addPagesItem(item);
            SALogging.getInstance().updateLogValuesForHomeItems();
        }
    }

    public boolean containPagesItem(ItemInfo item) {
        if (this.mBgHotseatItems.contains(item)) {
            return true;
        }
        for (ArrayList<ItemInfo> pageItems : this.mBgPagesItems.values()) {
            if (pageItems.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public void setOrderedScreen(ArrayList<Long> screen) {
        this.mBgOrderedScreens.clear();
        this.mBgOrderedScreens.addAll(screen);
    }

    public void removeUnRestoredItems(boolean itemLoaded) {
        Runnable r = new Runnable() {
            public void run() {
                final ArrayList<ItemInfo> removeItems = HomeLoader.this.getUnRestoredItems(HomeLoader.this.getAllItemInHome());
                if (!removeItems.isEmpty()) {
                    Iterator it = removeItems.iterator();
                    while (it.hasNext()) {
                        Log.d(HomeLoader.TAG, "This item is not restored. remove : " + ((ItemInfo) it.next()).toString());
                    }
                    HomeLoader.this.mFavoritesUpdater.deleteItemsFromDatabase(removeItems);
                    final HomeCallbacks oldCallbacks = HomeLoader.this.getCallback();
                    if (oldCallbacks != null) {
                        HomeLoader.this.runOnMainThread(new Runnable() {
                            public void run() {
                                HomeCallbacks callbacks = HomeLoader.this.tryGetCallbacks(oldCallbacks, HomeLoader.sLauncherModel.getLoaderTask());
                                if (callbacks != null) {
                                    callbacks.bindItemsRemoved(removeItems);
                                }
                            }
                        });
                    }
                }
            }
        };
        Log.d(TAG, "removeUnRestoredItems " + itemLoaded);
        if (itemLoaded) {
            r.run();
            return;
        }
        Log.d(TAG, "home item is not loaded run after load all item");
        synchronized (this.mLoadCompleteRunnables) {
            this.mLoadCompleteRunnables.add(r);
        }
    }

    private HomeDefaultLayoutParser getDefaultLayoutParser() {
        return new HomeDefaultLayoutParser(sContext, sFavoritesProvider.getAppWidgetHost(), sFavoritesProvider, sContext.getResources(), 0);
    }

    protected void loadDefaultLayout(AutoInstallsLayout autoInstallLayout, boolean isSwitchedDb, boolean reloadPostPosition) {
        HomeDefaultLayoutParser parser = null;
        if (autoInstallLayout != null) {
            Log.d(TAG, "use auto install layout for home");
            Resources res = autoInstallLayout.getResources();
            int workspaceResId = res.getIdentifier("default_workspace", "xml", autoInstallLayout.getPackageName());
            if (workspaceResId != 0) {
                parser = new HomeDefaultLayoutParser(sContext, sFavoritesProvider.getAppWidgetHost(), sFavoritesProvider, res, workspaceResId);
            } else {
                Log.e(TAG, "default_workspace layout not found in package: " + autoInstallLayout.getPackageName());
            }
        }
        boolean usingExternallyProvidedLayout;
        if (parser != null) {
            usingExternallyProvidedLayout = true;
        } else {
            usingExternallyProvidedLayout = false;
        }
        if (parser == null) {
            parser = getDefaultLayoutParser();
        }
        if (!reloadPostPosition) {
            if (isSwitchedDb) {
                sFavoritesProvider.deleteTable("favorites");
                sFavoritesProvider.deleteTable(WorkspaceScreens.TABLE_NAME);
            } else {
                LauncherAppState.getLauncherProvider().createEmptyDB();
                if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled(false)) {
                    sFavoritesProvider.switchTable(1, true);
                }
                if (LauncherFeature.supportEasyModeChange() && LauncherAppState.getInstance().isEasyModeEnabled()) {
                    sFavoritesProvider.switchTable(2, true);
                }
            }
        }
        parser.setReloadPostPosition(reloadPostPosition);
        if (sFavoritesProvider.loadFavorites(parser) <= 0 && usingExternallyProvidedLayout) {
            if (!(isSwitchedDb || reloadPostPosition)) {
                LauncherAppState.getLauncherProvider().createEmptyDB();
                if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled(false)) {
                    sFavoritesProvider.switchTable(1, true);
                }
                if (LauncherFeature.supportEasyModeChange() && LauncherAppState.getInstance().isEasyModeEnabled()) {
                    sFavoritesProvider.switchTable(2, true);
                }
            }
            sFavoritesProvider.loadFavorites(getDefaultLayoutParser());
        }
    }

    protected void loadDefaultLayoutCompleted() {
        if (LauncherFeature.supportCustomerDialerChange()) {
            saveCustomerPageKey();
            if (System.getInt(sContentResolver, "skt_phone20_settings", 0) == 1) {
                changeDialerAppOnLoadDefaultLayout();
            }
        }
        if (LauncherFeature.supportEasyModeChange() && LauncherAppState.getInstance().isEasyModeEnabled()) {
            addAppsButtonForEasy();
        }
    }

    private void saveCustomerPageKey() {
        Intent customerDialerIntent = new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(new ComponentName(LauncherFeature.getCustomerDialerPackageName(), LauncherFeature.getCustomerDialerClassName())).setFlags(270532608);
        String[] selectionArg = new String[]{String.valueOf(-100), customerDialerIntent.toUri(0)};
        Cursor cursor = sContentResolver.query(Favorites.CONTENT_URI, new String[]{"screen"}, "container=? AND intent=?", selectionArg, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    long screenId = cursor.getLong(0);
                    sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putLong(LauncherFiles.CUSTOMER_PAGE_KEY, screenId).apply();
                    Log.d(TAG, "save customer page key " + screenId);
                } else {
                    Log.d(TAG, "customer dialer is not exist in the workspace");
                }
                cursor.close();
            } catch (Throwable th) {
                cursor.close();
            }
        } else {
            Log.d(TAG, "customer dialer is not exist in the workspace");
        }
    }

    private void changeDialerAppOnLoadDefaultLayout() {
        Log.d(TAG, "changeDialerAppOnLoadDefaultLayout : OEM -> T phone app");
        ComponentName oemDialerCN = new ComponentName(LauncherFeature.getOemDialerPackageName(sContext), LauncherFeature.getOemDialerClassName());
        ComponentName customerDialerCN = new ComponentName(LauncherFeature.getCustomerDialerPackageName(), LauncherFeature.getCustomerDialerClassName());
        Intent oemDialerIntent = new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(oemDialerCN).setFlags(270532608);
        Intent customerDialerIntent = new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(customerDialerCN).setFlags(270532608);
        String[] selectionArg = new String[]{String.valueOf(Favorites.CONTAINER_HOTSEAT), oemDialerIntent.toUri(0)};
        Cursor cursor = sContentResolver.query(Favorites.CONTENT_URI, new String[]{"_id"}, "container=? AND intent=?", selectionArg, null);
        if (cursor != null) {
            try {
                ContentValues values;
                String selection;
                if (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    values = new ContentValues();
                    values.put("intent", customerDialerIntent.toUri(0));
                    sContentResolver.update(Favorites.getContentUri(id), values, null, null);
                } else {
                    Log.d(TAG, "Oem dialer is not exist in the hotseat");
                }
                cursor.close();
                if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    selection = "container=? AND intent=?";
                    selectionArg = new String[]{String.valueOf(-100), customerDialerIntent.toUri(0)};
                } else {
                    long customerPage = Utilities.getCustomerPageKey(sContext);
                    selection = "container=? AND intent=? AND screen=?";
                    selectionArg = new String[]{String.valueOf(-100), customerDialerIntent.toUri(0), String.valueOf(customerPage)};
                }
                cursor = sContentResolver.query(Favorites.CONTENT_URI, new String[]{"_id"}, selection, selectionArg, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToNext()) {
                            Long id2 = Long.valueOf(cursor.getLong(0));
                            values = new ContentValues();
                            values.put("intent", oemDialerIntent.toUri(0));
                            sContentResolver.update(Favorites.getContentUri(id2.longValue()), values, null, null);
                        } else {
                            Log.d(TAG, "Customer dialer is not exist in the workspace");
                        }
                        cursor.close();
                    } catch (Throwable th) {
                        cursor.close();
                    }
                } else {
                    Log.d(TAG, "Customer dialer is not exist in the workspace");
                }
            } catch (Throwable th2) {
                cursor.close();
            }
        } else {
            Log.d(TAG, "Oem dialer is not exist in the hotseat");
        }
    }

    String checkDuplicatedSingleInstanceWidgetExist(PendingAddItemInfo info) {
        if (this.mBgPagesItems == null || this.mBgPagesItems.size() == 0) {
            return null;
        }
        Iterator it = getWidgetsInHome().iterator();
        while (it.hasNext()) {
            LauncherAppWidgetInfo item = (LauncherAppWidgetInfo) it.next();
            if (item.providerName.getPackageName().equals(info.componentName.getPackageName()) && !item.providerName.equals(info.componentName)) {
                return item.hostView.getAppWidgetInfo().label;
            }
        }
        return null;
    }

    private boolean makeSingleInstanceAppWidgetList() {
        HomeController.sSingleInstanceAppWidgetList.clear();
        HomeController.sSingleInstanceAppWidgetPackageList.clear();
        PackageManager pm = sContext.getPackageManager();
        Intent singleInstanceIntent = new Intent(ACTION_APPWIDGET_SINGLE_INSTANCE);
        for (ResolveInfo info : pm.queryBroadcastReceivers(singleInstanceIntent, 0)) {
            ComponentName mComponentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            if (!HomeController.sSingleInstanceAppWidgetList.containsKey(mComponentName.flattenToShortString())) {
                Log.d(TAG, "This widget is single instance - " + mComponentName.flattenToShortString());
                HomeController.sSingleInstanceAppWidgetList.put(mComponentName.flattenToShortString(), new LongSparseArray());
            }
        }
        singleInstanceIntent.setAction(ACTION_APPWIDGET_SINGLE_INSTANCE_PACKAGE);
        for (ResolveInfo info2 : pm.queryBroadcastReceivers(singleInstanceIntent, 0)) {
            if (!HomeController.sSingleInstanceAppWidgetPackageList.containsKey(info2.activityInfo.packageName)) {
                Log.d(TAG, "This widget is single instance - " + info2.activityInfo.packageName);
                HomeController.sSingleInstanceAppWidgetPackageList.put(info2.activityInfo.packageName, new LongSparseArray());
            }
        }
        if (HomeController.sSingleInstanceAppWidgetPackageList.isEmpty() && HomeController.sSingleInstanceAppWidgetList.isEmpty()) {
            return false;
        }
        return true;
    }

    void checkAppWidgetSingleInstanceList(LauncherAppWidgetInfo info) {
        if (HomeController.sSingleInstanceAppWidgetList.containsKey(info.providerName.flattenToShortString())) {
            ((LongSparseArray) HomeController.sSingleInstanceAppWidgetList.get(info.providerName.flattenToShortString())).put(Long.valueOf(sUserManager.getSerialNumberForUser(info.user)).longValue(), Integer.valueOf(1));
        }
        if (HomeController.sSingleInstanceAppWidgetPackageList.containsKey(info.providerName.getPackageName())) {
            ((LongSparseArray) HomeController.sSingleInstanceAppWidgetPackageList.get(info.providerName.getPackageName())).put(Long.valueOf(sUserManager.getSerialNumberForUser(info.user)).longValue(), Integer.valueOf(1));
        }
    }

    private ArrayList<LauncherAppWidgetInfo> getWidgetsInHome() {
        ArrayList<LauncherAppWidgetInfo> widgets = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.itemType == 4) {
                    widgets.add((LauncherAppWidgetInfo) info);
                }
            }
        }
        return widgets;
    }

    public ArrayList<ItemInfo> getAllAppItemInHome() {
        ArrayList<ItemInfo> apps = new ArrayList();
        ArrayList<Long> folderIds = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it.next();
                if (folderInfo.container == -101 || folderInfo.container == -100) {
                    folderIds.add(Long.valueOf(folderInfo.id));
                }
            }
            it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.itemType == 0 && (info.container == -101 || info.container == -100 || folderIds.contains(Long.valueOf(info.container)))) {
                    if (info.componentName == null) {
                        info.componentName = info.getIntent().getComponent();
                        apps.add(info);
                    } else {
                        apps.add(info);
                    }
                }
            }
        }
        return apps;
    }

    ArrayList<ItemInfo> getItemsForDexSync() {
        ArrayList<ItemInfo> apps = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.container > 0) {
                    ItemInfo folder = getItemById(info.container);
                    if (!(folder == null || folder.container == -102)) {
                        apps.add(info);
                    }
                } else if (!(info.container == -102 || info.itemType == 4)) {
                    apps.add(info);
                }
            }
        }
        return apps;
    }

    void updateFolderTitle(final ItemInfo item) {
        final HomeCallbacks oldCallbacks = getCallback();
        runOnMainThread(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.getCallback();
                if (callbacks != null && callbacks == oldCallbacks) {
                    callbacks.bindFolderTitle(item);
                }
            }
        });
    }

    void updateContainerForDexSync(boolean addToFolder, FolderInfo folder, IconInfo item) {
        final HomeCallbacks oldCallbacks = getCallback();
        final boolean z = addToFolder;
        final FolderInfo folderInfo = folder;
        final IconInfo iconInfo = item;
        runOnMainThread(new Runnable() {
            public void run() {
                HomeCallbacks callbacks = HomeLoader.this.getCallback();
                if (callbacks != null && callbacks == oldCallbacks) {
                    callbacks.bindUpdateContainer(z, folderInfo, iconInfo);
                }
            }
        });
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isAllAppItemInApps(com.android.launcher3.common.base.item.ItemInfo r11) {
        /*
        r10 = this;
        r8 = -102; // 0xffffffffffffff9a float:NaN double:NaN;
        r2 = 1;
        r0 = new java.util.ArrayList;
        r0.<init>();
        r3 = sBgLock;
        monitor-enter(r3);
        r4 = r11.itemType;	 Catch:{ all -> 0x0039 }
        if (r4 != 0) goto L_0x004a;
    L_0x000f:
        r4 = r11.container;	 Catch:{ all -> 0x0039 }
        r4 = (r4 > r8 ? 1 : (r4 == r8 ? 0 : -1));
        if (r4 != 0) goto L_0x0017;
    L_0x0015:
        monitor-exit(r3);	 Catch:{ all -> 0x0039 }
    L_0x0016:
        return r2;
    L_0x0017:
        r4 = sBgFolders;	 Catch:{ all -> 0x0039 }
        r4 = r4.iterator();	 Catch:{ all -> 0x0039 }
    L_0x001d:
        r5 = r4.hasNext();	 Catch:{ all -> 0x0039 }
        if (r5 == 0) goto L_0x003c;
    L_0x0023:
        r1 = r4.next();	 Catch:{ all -> 0x0039 }
        r1 = (com.android.launcher3.folder.FolderInfo) r1;	 Catch:{ all -> 0x0039 }
        r6 = r1.container;	 Catch:{ all -> 0x0039 }
        r5 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1));
        if (r5 != 0) goto L_0x001d;
    L_0x002f:
        r6 = r1.id;	 Catch:{ all -> 0x0039 }
        r5 = java.lang.Long.valueOf(r6);	 Catch:{ all -> 0x0039 }
        r0.add(r5);	 Catch:{ all -> 0x0039 }
        goto L_0x001d;
    L_0x0039:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x0039 }
        throw r2;
    L_0x003c:
        r4 = r11.container;	 Catch:{ all -> 0x0039 }
        r4 = java.lang.Long.valueOf(r4);	 Catch:{ all -> 0x0039 }
        r4 = r0.contains(r4);	 Catch:{ all -> 0x0039 }
        if (r4 == 0) goto L_0x004a;
    L_0x0048:
        monitor-exit(r3);	 Catch:{ all -> 0x0039 }
        goto L_0x0016;
    L_0x004a:
        monitor-exit(r3);	 Catch:{ all -> 0x0039 }
        r2 = 0;
        goto L_0x0016;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.isAllAppItemInApps(com.android.launcher3.common.base.item.ItemInfo):boolean");
    }

    long insertWorkspaceScreen(Context context, int insertIndex, long insertScreenId) {
        long j = -1;
        ArrayList<Long> workspaceScreens = sFavoritesProvider.loadScreensFromDb();
        if (workspaceScreens.size() < insertIndex) {
            Log.e(TAG, "insert page should be less than total workspace screen count.");
        } else {
            if (insertScreenId == -1) {
                j = FavoritesProvider.getInstance().generateNewScreenId();
            } else {
                j = insertScreenId;
            }
            workspaceScreens.add(insertIndex, Long.valueOf(j));
            this.mFavoritesUpdater.updateScreenOrder(context, workspaceScreens);
            if (!workspaceScreens.isEmpty()) {
                final HomeCallbacks callbacks = getCallback();
                final int i = insertIndex;
                runOnMainThread(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (cb != null && callbacks == cb) {
                            cb.bindInsertScreens(j, i);
                        }
                    }
                });
            }
        }
        return j;
    }

    public ArrayList<ItemInfo> getAllItemInHome() {
        ArrayList<ItemInfo> items = new ArrayList();
        ArrayList<Long> folderIds = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = sBgFolders.iterator();
            while (it.hasNext()) {
                FolderInfo folderInfo = (FolderInfo) it.next();
                if (folderInfo.container == -100 || folderInfo.container == -101) {
                    folderIds.add(Long.valueOf(folderInfo.id));
                }
            }
            it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.container == -100 || info.container == -101 || folderIds.contains(Long.valueOf(info.container))) {
                    items.add(info);
                }
            }
        }
        return items;
    }

    private void handleOutsideItems(ArrayList<ItemInfo> pageItems) {
        if (pageItems == null || pageItems.isEmpty()) {
            Log.d(TAG, "pageItems is null or empty");
            return;
        }
        ArrayList<ItemInfo> reArrangeItems = new ArrayList();
        Iterator it = pageItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.container == -100) {
                reArrangeItems.add(item);
            }
        }
        ArrayList<ItemInfo> outsideItems = reArrangeByGrid(reArrangeItems);
        it = outsideItems.iterator();
        while (it.hasNext()) {
            item = (ItemInfo) it.next();
            pageItems.remove(item);
            removePagesItem(item);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        it = pageItems.iterator();
        while (it.hasNext()) {
            item = (ItemInfo) it.next();
            if (item.container != -101) {
                Uri uri = Favorites.getContentUri(item.id);
                ContentValues values = new ContentValues();
                values.put("cellX", Integer.valueOf(item.cellX));
                values.put("cellY", Integer.valueOf(item.cellY));
                values.put("spanX", Integer.valueOf(item.spanX));
                values.put("spanY", Integer.valueOf(item.spanY));
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
            }
        }
        try {
            sContentResolver.applyBatch("com.sec.android.app.launcher.settings", ops);
        } catch (RemoteException e) {
            Log.d(TAG, "occurred RemoteException during handleOutsideItems - " + e.getMessage());
        } catch (OperationApplicationException e2) {
            Log.d(TAG, "occurred OperationApplicationException during handleOutsideItems - " + e2.getMessage());
        }
        int plusIndex = 1;
        while (outsideItems != null && !outsideItems.isEmpty()) {
            ArrayList<ItemInfo> arrangedItems = outsideItems;
            long newScreenId = FavoritesProvider.getInstance().generateNewScreenId();
            int plusIndex2 = plusIndex + 1;
            this.mNewPageIdsAfterGridChanged.put(Long.valueOf(newScreenId), new NewScreenInfo(((ItemInfo) outsideItems.get(0)).screenId, plusIndex));
            outsideItems = arrangeItemToNewScreen(outsideItems, newScreenId);
            this.mExtraItemsAfterGridChanged.put(Long.valueOf(newScreenId), arrangedItems);
            plusIndex = plusIndex2;
        }
    }

    private void fillOccupied(int countX, int countY, int cellX, int cellY, int spanX, int spanY, boolean[][] occupied) {
        if (cellX >= 0 && cellY >= 0 && cellX + spanX <= countX && cellY + spanY <= countY) {
            int x = cellX;
            while (x < cellX + spanX && x < countX) {
                int y = cellY;
                while (y < cellY + spanY && y < countY) {
                    occupied[x][y] = true;
                    y++;
                }
                x++;
            }
        }
    }

    private void calculateDiffXY(int[] diffXY, int countX, int countY, ArrayList<ItemInfo> pageItems) {
        int i;
        int i2 = 0;
        int pageItemsMaxX = countX;
        int pageItemsMaxY = countY;
        Iterator it = pageItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.cellX + item.spanX > pageItemsMaxX) {
                pageItemsMaxX = item.cellX + item.spanX;
            }
            if (item.cellY + item.spanY > pageItemsMaxY) {
                pageItemsMaxY = item.cellY + item.spanY;
            }
        }
        if (pageItemsMaxX > countX) {
            i = pageItemsMaxX - countX;
        } else {
            i = 0;
        }
        diffXY[0] = i;
        if (pageItemsMaxY > countY) {
            i2 = pageItemsMaxY - countY;
        }
        diffXY[1] = i2;
    }

    private ArrayList<ItemInfo> reArrangeByGrid(ArrayList<ItemInfo> pageItems) {
        int y;
        int spanX;
        ArrayList<ItemInfo> outSideItems = new ArrayList();
        int countX = sProfile.homeGrid.getCellCountX();
        int countY = sProfile.homeGrid.getCellCountY();
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{countX, countY});
        int[] diffXY = new int[2];
        Iterator it = pageItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info.cellX < 0 || info.cellY < 0) {
                info.cellX = countX;
                info.cellY = countY;
            }
        }
        calculateDiffXY(diffXY, countX, countY, pageItems);
        int diffIndexX = diffXY[0] > 0 ? diffXY[0] - 1 : 0;
        int diffIndexY = diffXY[1] > 0 ? diffXY[1] - 1 : 0;
        int position = ScreenGridUtilities.getOutSidePosition(pageItems, countX, countY, diffIndexX, diffIndexY);
        for (int i = pageItems.size() - 1; i > -1; i--) {
            int x;
            ItemInfo item = (ItemInfo) pageItems.get(i);
            if (item != null) {
                x = item.cellX;
                y = item.cellY;
                if (item.spanX > countX) {
                    item.spanX = countX;
                    if (position == 1 || position == 3) {
                        x += diffXY[0];
                    }
                }
                if (item.spanY > countY) {
                    item.spanY = countY;
                    if (position == 2 || position == 3) {
                        y += diffXY[1];
                    }
                }
                spanX = item.spanX;
                int spanY = item.spanY;
                if (position == 0) {
                    if (x + spanX > countX || y + spanY > countY) {
                        outSideItems.add(item);
                    }
                } else if (position == 1) {
                    if (x <= diffIndexX || y + spanY > countY) {
                        outSideItems.add(item);
                    } else {
                        x -= diffXY[0];
                    }
                } else if (position == 2) {
                    if (x + spanX > countX || y <= diffIndexY) {
                        outSideItems.add(item);
                    } else {
                        y -= diffXY[1];
                    }
                } else if (position == 3) {
                    if (x <= diffIndexX || y <= diffIndexY) {
                        outSideItems.add(item);
                    } else {
                        x -= diffXY[0];
                        y -= diffXY[1];
                    }
                }
                if (item.cellX != x || item.cellY != y) {
                    item.cellX = x;
                    item.cellY = y;
                }
            } else {
                Log.d(TAG, "HomeLoader: changeGrid() item is Null");
            }
        }
        Iterator it2 = pageItems.iterator();
        while (it2.hasNext()) {
            item = (ItemInfo) it2.next();
            if (!outSideItems.contains(item)) {
                fillOccupied(countX, countY, item.cellX, item.cellY, item.spanX, item.spanY, occupied);
            }
        }
        Log.d(TAG, "reArrangeByGrid occupied: ");
        y = 0;
        while (y < countY) {
            String line = "";
            x = 0;
            while (x < countX) {
                if (x >= occupied.length || y >= occupied[x].length) {
                    line = line + "!";
                } else {
                    line = line + (occupied[x][y] ? "#" : ".");
                }
                x++;
            }
            Log.d(TAG, "[ " + line + " ]");
            y++;
        }
        ScreenGridUtilities.getOutSideItems(outSideItems, position);
        List<ItemInfo> removeFromOutSideItems = new ArrayList();
        Iterator it3 = outSideItems.iterator();
        while (it3.hasNext()) {
            item = (ItemInfo) it3.next();
            int[] tmpXY = new int[2];
            int correctedX = item.cellX;
            int correctedY = item.cellY;
            spanX = item.spanX > 0 ? item.spanX : 1;
            spanY = item.spanY > 0 ? item.spanY : 1;
            if (correctedX > countX - 1) {
                correctedX -= diffXY[0];
            }
            if (correctedY > countY - 1) {
                correctedY -= diffXY[1];
            }
            if (this.mItemPositionHelper.findNearVacantCell(tmpXY, correctedX, correctedY, spanX, spanY, countX, countY, occupied)) {
                Log.d(TAG, "outside item - find new cell " + tmpXY[0] + "/" + tmpXY[1] + " " + item.toString());
                item.cellX = tmpXY[0];
                item.cellY = tmpXY[1];
                fillOccupied(countX, countY, item.cellX, item.cellY, spanX, spanY, occupied);
                removeFromOutSideItems.add(item);
            }
        }
        for (ItemInfo item2 : removeFromOutSideItems) {
            outSideItems.remove(item2);
        }
        return outSideItems;
    }

    private ArrayList<ItemInfo> arrangeItemToNewScreen(ArrayList<ItemInfo> items, long newScreenId) {
        int countX = sProfile.homeGrid.getCellCountX();
        int countY = sProfile.homeGrid.getCellCountY();
        ArrayList<ItemInfo> againMoveItems = new ArrayList();
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{countX, countY});
        int[] tmpXY = new int[2];
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            item.screenId = newScreenId;
            if (this.mItemPositionHelper.findEmptyCellWithOccupied(tmpXY, item.spanX, item.spanY, countX, countY, occupied)) {
                item.cellX = tmpXY[0];
                item.cellY = tmpXY[1];
                fillOccupied(countX, countY, item.cellX, item.cellY, item.spanX, item.spanY, occupied);
            } else {
                againMoveItems.add(item);
            }
        }
        Iterator it2 = againMoveItems.iterator();
        while (it2.hasNext()) {
            items.remove((ItemInfo) it2.next());
        }
        return againMoveItems;
    }

    private int getCellXFromHotseatOrder(int rank) {
        return sProfile.isVerticalBarLayout() ? 0 : rank;
    }

    private int getCellYFromHotseatOrder(int rank) {
        return sProfile.isVerticalBarLayout() ? sProfile.homeGrid.getCellCountY() - (rank + 1) : 0;
    }

    public IconInfo getAppsButton() {
        synchronized (sBgLock) {
            Iterator it = this.mBgHotseatItems.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if ((info instanceof IconInfo) && ((IconInfo) info).isAppsButton) {
                    IconInfo iconInfo = (IconInfo) info;
                    return iconInfo;
                }
            }
            return null;
        }
    }

    public void updatePackageFlags(StringFilter pkgFilter, UserHandleCompat user, FlagOp flagOp) {
        final ArrayList<IconInfo> updatedShortcuts = new ArrayList();
        final ArrayList<ItemInfo> removedShortcuts = new ArrayList();
        if (pkgFilter.matches("")) {
            this.mQuietModeUsers.put(sUserManager.getSerialNumberForUser(user), Boolean.valueOf(flagOp.apply(8) > 0));
        }
        synchronized (sBgLock) {
            Iterator it = getAllItemInHome().iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info instanceof IconInfo) {
                    IconInfo iconInfo = (IconInfo) info;
                    ComponentName cn = iconInfo.getTargetComponent();
                    if (cn != null && iconInfo.user.equals(user)) {
                        if (!pkgFilter.matches(cn.getPackageName())) {
                            continue;
                        } else if (sLauncherApps.getActivityList(cn.getPackageName(), user).isEmpty()) {
                            Log.d(TAG, "updatePackageFlags remove item " + cn);
                            removedShortcuts.add(iconInfo);
                            this.mFavoritesUpdater.deleteItem(iconInfo);
                        } else {
                            iconInfo.isDisabled = flagOp.apply(iconInfo.isDisabled);
                            updatedShortcuts.add(iconInfo);
                        }
                    }
                }
            }
        }
        final HomeCallbacks oldCallback = getCallback();
        final UserHandleCompat userHandleCompat = user;
        sHandler.post(new Runnable() {
            public void run() {
                HomeCallbacks cb = HomeLoader.this.getCallback();
                if (cb != null && oldCallback == cb) {
                    if (!removedShortcuts.isEmpty()) {
                        cb.bindItemsRemoved(removedShortcuts);
                    }
                    cb.bindShortcutsChanged(updatedShortcuts, new ArrayList(), userHandleCompat);
                }
            }
        });
    }

    UserHandleCompat getUser(long serialNumber) {
        return (UserHandleCompat) this.mAllUsers.get(serialNumber);
    }

    private void addAppsButtonForEasy() {
        IconInfo appsButton = Utilities.createAppsButton(sContext);
        ContentValues values = new ContentValues();
        LauncherAppState.getInstance().setAppsButtonEnabled(true);
        appsButton.screenId = 2;
        int cellXFromHotseatOrder = getCellXFromHotseatOrder((int) appsButton.screenId);
        appsButton.cellY = cellXFromHotseatOrder;
        appsButton.cellX = cellXFromHotseatOrder;
        appsButton.onAddToDatabase(sContext, values);
        if (appsButton.id == -1) {
            appsButton.id = sFavoritesProvider.generateNewItemId();
            values.put("_id", Long.valueOf(appsButton.id));
            sContentResolver.insert(Favorites.CONTENT_URI, values);
            return;
        }
        Log.e(TAG, "HomeLoader:Don't add Apps button in EasyMode");
    }

    public void updateUnavailablePackage(String disabledPackages, UserHandleCompat user, int reason) {
        Iterator it = getAllItemInHome().iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item instanceof IconInfo) {
                IconInfo iconInfo = (IconInfo) item;
                ComponentName cn = iconInfo.getTargetComponent();
                if (cn != null && disabledPackages.equals(cn.getPackageName()) && user.equals(iconInfo.getUserHandle())) {
                    iconInfo.isDisabled |= reason;
                    Log.i(TAG, "updateUnavailablePackage unavailable = " + item);
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateShortcutIcons() {
        /*
        r13 = this;
        r9 = sBgLock;
        monitor-enter(r9);
        r1 = r13.getAllItemInHome();	 Catch:{ all -> 0x0077 }
        r7 = new java.util.ArrayList;	 Catch:{ all -> 0x0077 }
        r7.<init>();	 Catch:{ all -> 0x0077 }
        r10 = r1.iterator();	 Catch:{ all -> 0x0077 }
    L_0x0010:
        r8 = r10.hasNext();	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x009a;
    L_0x0016:
        r4 = r10.next();	 Catch:{ all -> 0x0077 }
        r4 = (com.android.launcher3.common.base.item.ItemInfo) r4;	 Catch:{ all -> 0x0077 }
        r8 = r4 instanceof com.android.launcher3.common.base.item.IconInfo;	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x007a;
    L_0x0020:
        r0 = r4;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0077 }
        r8 = r0;
        r8 = r8.customIcon;	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x007a;
    L_0x0028:
        r8 = r4.itemType;	 Catch:{ all -> 0x0077 }
        r11 = 1;
        if (r8 != r11) goto L_0x007a;
    L_0x002d:
        r0 = r4;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0077 }
        r8 = r0;
        r5 = r8.getOriginalIcon();	 Catch:{ all -> 0x0077 }
        if (r5 == 0) goto L_0x0010;
    L_0x0037:
        r3 = r5;
        r8 = com.android.launcher3.util.ShortcutTray.isIconTrayEnabled();	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x0046;
    L_0x003e:
        r8 = sContext;	 Catch:{ all -> 0x0077 }
        r11 = r4.componentName;	 Catch:{ all -> 0x0077 }
        r3 = com.android.launcher3.util.ShortcutTray.getIcon(r8, r5, r11);	 Catch:{ all -> 0x0077 }
    L_0x0046:
        r8 = sContext;	 Catch:{ all -> 0x0077 }
        r8 = com.android.launcher3.util.DualAppUtils.supportDualApp(r8);	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x006a;
    L_0x004e:
        r8 = r4.user;	 Catch:{ all -> 0x0077 }
        r8 = com.android.launcher3.util.DualAppUtils.isDualAppId(r8);	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x006a;
    L_0x0056:
        r8 = sContext;	 Catch:{ all -> 0x0077 }
        r11 = sProfile;	 Catch:{ all -> 0x0077 }
        r11 = r11.homeGrid;	 Catch:{ all -> 0x0077 }
        r11 = r11.getIconSize();	 Catch:{ all -> 0x0077 }
        r12 = r4.user;	 Catch:{ all -> 0x0077 }
        r12 = r12.getUser();	 Catch:{ all -> 0x0077 }
        r3 = com.android.launcher3.util.DualAppUtils.makeUserBadgedIcon(r8, r3, r11, r12);	 Catch:{ all -> 0x0077 }
    L_0x006a:
        r0 = r4;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0077 }
        r8 = r0;
        r8.setIcon(r3);	 Catch:{ all -> 0x0077 }
        r4 = (com.android.launcher3.common.base.item.IconInfo) r4;	 Catch:{ all -> 0x0077 }
        r7.add(r4);	 Catch:{ all -> 0x0077 }
        goto L_0x0010;
    L_0x0077:
        r8 = move-exception;
        monitor-exit(r9);	 Catch:{ all -> 0x0077 }
        throw r8;
    L_0x007a:
        r8 = r4 instanceof com.android.launcher3.home.LauncherPairAppsInfo;	 Catch:{ all -> 0x0077 }
        if (r8 == 0) goto L_0x0010;
    L_0x007e:
        r0 = r4;
        r0 = (com.android.launcher3.home.LauncherPairAppsInfo) r0;	 Catch:{ all -> 0x0077 }
        r6 = r0;
        r8 = sContext;	 Catch:{ all -> 0x0077 }
        r11 = r6.mFirstApp;	 Catch:{ all -> 0x0077 }
        r12 = r6.mSecondApp;	 Catch:{ all -> 0x0077 }
        r3 = com.android.launcher3.util.PairAppsUtilities.buildIcon(r8, r11, r12);	 Catch:{ all -> 0x0077 }
        r0 = r4;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0077 }
        r8 = r0;
        r8.setIcon(r3);	 Catch:{ all -> 0x0077 }
        r4 = (com.android.launcher3.common.base.item.IconInfo) r4;	 Catch:{ all -> 0x0077 }
        r7.add(r4);	 Catch:{ all -> 0x0077 }
        goto L_0x0010;
    L_0x009a:
        r8 = r7.isEmpty();	 Catch:{ all -> 0x0077 }
        if (r8 != 0) goto L_0x00b9;
    L_0x00a0:
        r2 = r13.getCallback();	 Catch:{ all -> 0x0077 }
        if (r2 != 0) goto L_0x00af;
    L_0x00a6:
        r8 = "HomeLoader";
        r10 = "updateShortcutIcons. Nobody to tell about the new app. Launcher is probably loading.";
        android.util.Log.w(r8, r10);	 Catch:{ all -> 0x0077 }
        monitor-exit(r9);	 Catch:{ all -> 0x0077 }
    L_0x00ae:
        return;
    L_0x00af:
        r8 = sHandler;	 Catch:{ all -> 0x0077 }
        r10 = new com.android.launcher3.home.HomeLoader$38;	 Catch:{ all -> 0x0077 }
        r10.<init>(r2, r7);	 Catch:{ all -> 0x0077 }
        r8.post(r10);	 Catch:{ all -> 0x0077 }
    L_0x00b9:
        monitor-exit(r9);	 Catch:{ all -> 0x0077 }
        goto L_0x00ae;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.updateShortcutIcons():void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateDeepShortcutIcons() {
        /*
        r9 = this;
        r6 = sBgLock;
        monitor-enter(r6);
        r1 = r9.getAllItemInHome();	 Catch:{ all -> 0x0034 }
        r4 = new java.util.ArrayList;	 Catch:{ all -> 0x0034 }
        r4.<init>();	 Catch:{ all -> 0x0034 }
        r7 = r1.iterator();	 Catch:{ all -> 0x0034 }
    L_0x0010:
        r5 = r7.hasNext();	 Catch:{ all -> 0x0034 }
        if (r5 == 0) goto L_0x0037;
    L_0x0016:
        r3 = r7.next();	 Catch:{ all -> 0x0034 }
        r3 = (com.android.launcher3.common.base.item.ItemInfo) r3;	 Catch:{ all -> 0x0034 }
        r5 = r3.itemType;	 Catch:{ all -> 0x0034 }
        r8 = 6;
        if (r5 != r8) goto L_0x0010;
    L_0x0021:
        r5 = r3 instanceof com.android.launcher3.common.base.item.IconInfo;	 Catch:{ all -> 0x0034 }
        if (r5 == 0) goto L_0x0010;
    L_0x0025:
        r0 = r3;
        r0 = (com.android.launcher3.common.base.item.IconInfo) r0;	 Catch:{ all -> 0x0034 }
        r5 = r0;
        r8 = sContext;	 Catch:{ all -> 0x0034 }
        r5.updateDeepShortcutIcon(r8);	 Catch:{ all -> 0x0034 }
        r3 = (com.android.launcher3.common.base.item.IconInfo) r3;	 Catch:{ all -> 0x0034 }
        r4.add(r3);	 Catch:{ all -> 0x0034 }
        goto L_0x0010;
    L_0x0034:
        r5 = move-exception;
        monitor-exit(r6);	 Catch:{ all -> 0x0034 }
        throw r5;
    L_0x0037:
        r5 = r4.isEmpty();	 Catch:{ all -> 0x0034 }
        if (r5 != 0) goto L_0x0056;
    L_0x003d:
        r2 = r9.getCallback();	 Catch:{ all -> 0x0034 }
        if (r2 != 0) goto L_0x004c;
    L_0x0043:
        r5 = "HomeLoader";
        r7 = "updateDeepShortcutIcons. Nobody to tell about the new app. Launcher is probably loading.";
        android.util.Log.w(r5, r7);	 Catch:{ all -> 0x0034 }
        monitor-exit(r6);	 Catch:{ all -> 0x0034 }
    L_0x004b:
        return;
    L_0x004c:
        r5 = sHandler;	 Catch:{ all -> 0x0034 }
        r7 = new com.android.launcher3.home.HomeLoader$39;	 Catch:{ all -> 0x0034 }
        r7.<init>(r2, r4);	 Catch:{ all -> 0x0034 }
        r5.post(r7);	 Catch:{ all -> 0x0034 }
    L_0x0056:
        monitor-exit(r6);	 Catch:{ all -> 0x0034 }
        goto L_0x004b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.updateDeepShortcutIcons():void");
    }

    public void bindUpdatedDeepShortcuts(ArrayList<IconInfo> updatedShortcuts, UserHandleCompat user) {
        bindUpdatedDeepShortcuts(updatedShortcuts, new ArrayList(), user);
    }

    public void bindUpdatedDeepShortcuts(ArrayList<IconInfo> updatedShortcuts, ArrayList<IconInfo> removedShortcuts, UserHandleCompat user) {
        if (!updatedShortcuts.isEmpty() || !removedShortcuts.isEmpty()) {
            final HomeCallbacks callbacks = getCallback();
            final ArrayList<IconInfo> arrayList = updatedShortcuts;
            final ArrayList<IconInfo> arrayList2 = removedShortcuts;
            final UserHandleCompat userHandleCompat = user;
            sHandler.post(new Runnable() {
                public void run() {
                    HomeCallbacks cb = HomeLoader.this.getCallback();
                    if (cb != null && callbacks == cb) {
                        cb.bindShortcutsChanged(arrayList, arrayList2, userHandleCompat);
                    }
                }
            });
        }
    }

    public void updateDeepShortcutsChanged(String packageName, List<ShortcutInfoCompat> shortcuts, UserHandleCompat user, boolean updateIdMap) {
        ArrayList<IconInfo> removedShortcutInfos = new ArrayList();
        MultiHashMap<String, IconInfo> idsToWorkspaceShortcutInfos = new MultiHashMap();
        synchronized (sBgLock) {
            Iterator it = sBgItemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo itemInfo = (ItemInfo) it.next();
                if (itemInfo.itemType == 6) {
                    IconInfo ii = (IconInfo) itemInfo;
                    if (ii.getPromisedIntent().getPackage().equals(packageName) && ii.user.equals(user)) {
                        idsToWorkspaceShortcutInfos.addToList(ii.getDeepShortcutId(), ii);
                    }
                }
            }
        }
        Context context = LauncherAppState.getInstance().getContext();
        ArrayList<IconInfo> updatedShortcutInfos = new ArrayList();
        if (!idsToWorkspaceShortcutInfos.isEmpty()) {
            for (ShortcutInfoCompat fullDetails : this.mDeepShortcutManager.queryForFullDetails(packageName, new ArrayList(idsToWorkspaceShortcutInfos.keySet()), user)) {
                List<IconInfo> shortcutInfos = (List) idsToWorkspaceShortcutInfos.remove(fullDetails.getId());
                if (fullDetails.isPinned()) {
                    for (IconInfo shortcutInfo : shortcutInfos) {
                        shortcutInfo.updateFromDeepShortcutInfo(fullDetails, context);
                        updatedShortcutInfos.add(shortcutInfo);
                    }
                } else {
                    removedShortcutInfos.addAll(shortcutInfos);
                }
            }
        }
        for (String id : idsToWorkspaceShortcutInfos.keySet()) {
            removedShortcutInfos.addAll((Collection) idsToWorkspaceShortcutInfos.get(id));
        }
        bindUpdatedDeepShortcuts(updatedShortcutInfos, removedShortcutInfos, user);
        if (!removedShortcutInfos.isEmpty()) {
            this.mFavoritesUpdater.deleteItemsFromDatabase(removedShortcutInfos);
        }
        if (updateIdMap) {
            updateDeepShortcutMap(packageName, user, shortcuts);
            bindDeepShortcuts();
        }
    }

    private void restoreStkPositionIfNecessary(ArrayList<ItemInfo> addList) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && addList != null && !addList.isEmpty()) {
            SharedPreferences prefs = null;
            for (String stkPkg : STK_PKG_LIST) {
                Iterator it = addList.iterator();
                while (it.hasNext()) {
                    ItemInfo info = (ItemInfo) it.next();
                    if (!(info == null || info.componentName == null)) {
                        if (stkPkg.equals(info.componentName.getPackageName())) {
                            info.container = -1;
                            info.screenId = -1;
                            info.cellX = -1;
                            info.cellY = -1;
                            if (prefs == null) {
                                prefs = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                            }
                            String savedInfo = prefs.getString(stkPkg, null);
                            if (!(savedInfo == null || savedInfo.isEmpty())) {
                                prefs.edit().remove(savedInfo).apply();
                                String[] data = savedInfo.split(";");
                                if (data.length == 4) {
                                    long container = Long.parseLong(data[0]);
                                    long screen = (long) Integer.parseInt(data[1]);
                                    int cellX = Integer.parseInt(data[2]);
                                    int cellY = Integer.parseInt(data[3]);
                                    if (container > 0) {
                                        synchronized (sBgLock) {
                                            FolderInfo folder = null;
                                            Iterator it2 = sBgFolders.iterator();
                                            while (it2.hasNext()) {
                                                FolderInfo fInfo = (FolderInfo) it2.next();
                                                if (fInfo.id == container) {
                                                    folder = fInfo;
                                                }
                                            }
                                            if (folder != null) {
                                                info.container = container;
                                                info.screenId = screen;
                                                info.cellX = cellX;
                                                info.cellY = cellY;
                                            }
                                        }
                                    } else if (container == -101) {
                                        int maxHotseatCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
                                        if (screen < ((long) maxHotseatCount) && this.mBgHotseatItems.size() != maxHotseatCount) {
                                            info.container = container;
                                            info.screenId = screen;
                                            info.cellX = cellX;
                                            info.cellY = cellY;
                                        }
                                    } else if (container == -100) {
                                        boolean isOutSide;
                                        int countX = sProfile.homeGrid.getCellCountX();
                                        int countY = sProfile.homeGrid.getCellCountY();
                                        ArrayList<Long> workspaceScreens = sFavoritesProvider.loadScreensFromDb();
                                        if (cellX >= 0 && cellY >= 0 && cellX < countX && cellY < countY) {
                                            if (workspaceScreens.contains(Long.valueOf(screen))) {
                                                isOutSide = false;
                                                if (!isOutSide) {
                                                    if (this.mItemPositionHelper.findEmptyCell(new int[]{cellX, cellY}, screen, 1, 1, true)) {
                                                        info.screenId = screen;
                                                        info.cellX = cellX;
                                                        info.cellY = cellY;
                                                    }
                                                }
                                            }
                                        }
                                        isOutSide = true;
                                        if (!isOutSide) {
                                            if (this.mItemPositionHelper.findEmptyCell(new int[]{cellX, cellY}, screen, 1, 1, true)) {
                                                info.screenId = screen;
                                                info.cellX = cellX;
                                                info.cellY = cellY;
                                            }
                                        }
                                    }
                                } else {
                                    continue;
                                }
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
    }

    public void titleUpdate() {
        ArrayList<ItemInfo> needUpdateItems = getNeedTitleUpdateIcons(getAllItemInHome());
        if (LauncherAppState.getInstance().getAppsButtonEnabled()) {
            IconInfo apps = getAppsButton();
            if (apps != null) {
                apps.title = sContext.getResources().getString(R.string.apps_button_label);
                needUpdateItems.add(apps);
            }
        }
        final HomeCallbacks callbacks = getCallback();
        if (callbacks == null) {
            Log.w(TAG, "titleUpdate. Nobody to tell about the new app. Launcher is probably loading.");
            return;
        }
        final ArrayList<IconInfo> finalUpdateItems = new ArrayList();
        synchronized (sBgLock) {
            Iterator it = needUpdateItems.iterator();
            while (it.hasNext()) {
                finalUpdateItems.add((IconInfo) ((ItemInfo) it.next()));
            }
        }
        runOnMainThread(new Runnable() {
            public void run() {
                if (callbacks == HomeLoader.this.getCallback() && callbacks != null) {
                    callbacks.bindShortcutsChanged(finalUpdateItems, null, null);
                }
            }
        });
    }

    void updateContactShortcutInfo(final long id, final Intent intent) {
        final Runnable r = new Runnable() {
            public void run() {
                synchronized (HomeLoader.sBgLock) {
                    ItemInfo info = (ItemInfo) HomeLoader.sBgItemsIdMap.get(id);
                    if ((info instanceof IconInfo) && info.itemType == 1) {
                        ((IconInfo) info).intent = intent;
                        Log.d(HomeLoader.TAG, "updateContactShortcutInfo " + id);
                    }
                }
            }
        };
        runOnMainThread(new Runnable() {
            public void run() {
                if (HomeLoader.sIsLoadingAndBindingWorkspace) {
                    synchronized (HomeLoader.sBindCompleteRunnables) {
                        HomeLoader.sBindCompleteRunnables.add(r);
                    }
                    return;
                }
                DataLoader.runOnWorkerThread(r);
            }
        });
    }

    private boolean shortcutExists(Intent intent, UserHandleCompat user) {
        if (!Utilities.isLauncherAppTarget(intent)) {
            return false;
        }
        String intentWithPkg;
        String intentWithoutPkg;
        if (intent.getComponent() != null) {
            String packageName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(packageName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }
        Iterator it = getAllItemInHome().iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item instanceof IconInfo) {
                IconInfo info = (IconInfo) item;
                Intent targetIntent = info.promisedIntent == null ? info.intent : info.promisedIntent;
                if (!(targetIntent == null || user == null || !user.equals(info.user))) {
                    String strIntent = targetIntent.toUri(0);
                    if (intentWithPkg.equals(strIntent) || intentWithoutPkg.equals(strIntent)) {
                        Log.d(TAG, "shortcutExists : " + info.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkWorkspaceIsEmpty() {
        String[] projection = new String[]{"screen"};
        Cursor cursor = sContentResolver.query(Favorites.CONTENT_URI, projection, "container=-100", null, null);
        if (cursor == null) {
            return true;
        }
        ArrayList<Long> screenIds = new ArrayList();
        while (cursor.moveToNext()) {
            long screenId = cursor.getLong(0);
            if (screenId < 0) {
                Log.d(TAG, "screen Id < 0");
            } else {
                try {
                    if (!screenIds.contains(Long.valueOf(screenId))) {
                        screenIds.add(Long.valueOf(screenId));
                    }
                } finally {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            }
        }
        if (screenIds.isEmpty()) {
            Log.d(TAG, "really no screen");
            return true;
        }
        Collections.sort(screenIds);
        int rank = 0;
        ContentValues values = new ContentValues();
        Iterator it = screenIds.iterator();
        while (it.hasNext()) {
            Long id = (Long) it.next();
            Log.d(TAG, "add workspace screen " + id);
            values.clear();
            values.put("_id", id);
            values.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(rank));
            sContentResolver.insert(WorkspaceScreens.CONTENT_URI, values);
            this.mBgOrderedScreens.add(id);
            rank++;
        }
        this.mIsPageLoaded = new boolean[this.mBgOrderedScreens.size()];
        sFavoritesProvider.setMaxScreenId(sFavoritesProvider.initializeMaxItemId(WorkspaceScreens.TABLE_NAME));
        return false;
    }

    public void runLoadCompleteRunnables() {
        Log.i(TAG, "runLoadCompleteRunnables, count : " + this.mLoadCompleteRunnables.size());
        if (!this.mLoadCompleteRunnables.isEmpty()) {
            synchronized (this.mLoadCompleteRunnables) {
                Iterator it = this.mLoadCompleteRunnables.iterator();
                while (it.hasNext()) {
                    DataLoader.runOnWorkerThread((Runnable) it.next());
                }
                this.mLoadCompleteRunnables.clear();
            }
        }
    }

    public void removeScreen(final int index) {
        DataLoader.runOnWorkerThread(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                r9 = this;
                r6 = com.android.launcher3.home.HomeLoader.sBgLock;
                monitor-enter(r6);
                r3 = r2;	 Catch:{ all -> 0x006f }
                r7 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r7 = r7.mBgOrderedScreens;	 Catch:{ all -> 0x006f }
                r7 = r7.size();	 Catch:{ all -> 0x006f }
                if (r3 < r7) goto L_0x001c;
            L_0x0013:
                r3 = "HomeLoader";
                r7 = "removeScreen : remove page should be less than total workspace screen count.";
                android.util.Log.w(r3, r7);	 Catch:{ all -> 0x006f }
            L_0x001a:
                monitor-exit(r6);	 Catch:{ all -> 0x006f }
            L_0x001b:
                return;
            L_0x001c:
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mBgOrderedScreens;	 Catch:{ all -> 0x006f }
                r7 = r2;	 Catch:{ all -> 0x006f }
                r3 = r3.get(r7);	 Catch:{ all -> 0x006f }
                r3 = (java.lang.Long) r3;	 Catch:{ all -> 0x006f }
                r4 = r3.longValue();	 Catch:{ all -> 0x006f }
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mBgPagesItems;	 Catch:{ all -> 0x006f }
                r7 = java.lang.Long.valueOf(r4);	 Catch:{ all -> 0x006f }
                r3 = r3.get(r7);	 Catch:{ all -> 0x006f }
                if (r3 == 0) goto L_0x0054;
            L_0x003e:
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mBgPagesItems;	 Catch:{ all -> 0x006f }
                r7 = java.lang.Long.valueOf(r4);	 Catch:{ all -> 0x006f }
                r3 = r3.get(r7);	 Catch:{ all -> 0x006f }
                r3 = (java.util.ArrayList) r3;	 Catch:{ all -> 0x006f }
                r3 = r3.isEmpty();	 Catch:{ all -> 0x006f }
                if (r3 == 0) goto L_0x0072;
            L_0x0054:
                r1 = 1;
            L_0x0055:
                r2 = new java.util.ArrayList;	 Catch:{ all -> 0x006f }
                r2.<init>();	 Catch:{ all -> 0x006f }
                if (r1 != 0) goto L_0x0090;
            L_0x005c:
                r3 = com.android.launcher3.LauncherAppState.getInstance();	 Catch:{ all -> 0x006f }
                r3 = r3.isHomeOnlyModeEnabled();	 Catch:{ all -> 0x006f }
                if (r3 == 0) goto L_0x0074;
            L_0x0066:
                r3 = "HomeLoader";
                r7 = "removeScreen : it can't remove a page with items on HomeOnlyMode";
                android.util.Log.w(r3, r7);	 Catch:{ all -> 0x006f }
                monitor-exit(r6);	 Catch:{ all -> 0x006f }
                goto L_0x001b;
            L_0x006f:
                r3 = move-exception;
                monitor-exit(r6);	 Catch:{ all -> 0x006f }
                throw r3;
            L_0x0072:
                r1 = 0;
                goto L_0x0055;
            L_0x0074:
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mBgPagesItems;	 Catch:{ all -> 0x006f }
                r7 = java.lang.Long.valueOf(r4);	 Catch:{ all -> 0x006f }
                r3 = r3.get(r7);	 Catch:{ all -> 0x006f }
                r3 = (java.util.Collection) r3;	 Catch:{ all -> 0x006f }
                r2.addAll(r3);	 Catch:{ all -> 0x006f }
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mFavoritesUpdater;	 Catch:{ all -> 0x006f }
                r3.deleteItemsFromDatabase(r2);	 Catch:{ all -> 0x006f }
            L_0x0090:
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mBgOrderedScreens;	 Catch:{ all -> 0x006f }
                r7 = r2;	 Catch:{ all -> 0x006f }
                r3.remove(r7);	 Catch:{ all -> 0x006f }
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r3 = r3.mFavoritesUpdater;	 Catch:{ all -> 0x006f }
                r7 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x006f }
                r8 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r8 = r8.mBgOrderedScreens;	 Catch:{ all -> 0x006f }
                r3.updateScreenOrder(r7, r8);	 Catch:{ all -> 0x006f }
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r0 = r3.getCallback();	 Catch:{ all -> 0x006f }
                r3 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x006f }
                r7 = new com.android.launcher3.home.HomeLoader$44$1;	 Catch:{ all -> 0x006f }
                r7.<init>(r0, r2);	 Catch:{ all -> 0x006f }
                r3.runOnMainThread(r7);	 Catch:{ all -> 0x006f }
                goto L_0x001a;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.44.run():void");
            }
        });
    }

    public void removeItemsByPosition(int screen, int cellX, int cellY, int spanX, int spanY) {
        final int i = screen;
        final int i2 = cellX;
        final int i3 = cellY;
        final int i4 = spanX;
        final int i5 = spanY;
        DataLoader.runOnWorkerThread(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                r19 = this;
                r13 = com.android.launcher3.home.HomeLoader.sBgLock;
                monitor-enter(r13);
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r12 = r12.mBgOrderedScreens;	 Catch:{ all -> 0x00fc }
                r9 = r12.size();	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r12 = r2;	 Catch:{ all -> 0x00fc }
                if (r12 < r9) goto L_0x003f;
            L_0x0017:
                r12 = "HomeLoader";
                r14 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00fc }
                r14.<init>();	 Catch:{ all -> 0x00fc }
                r15 = "removeItemsByPosition : currentPageCount = ";
                r14 = r14.append(r15);	 Catch:{ all -> 0x00fc }
                r14 = r14.append(r9);	 Catch:{ all -> 0x00fc }
                r15 = " removeIndex = ";
                r14 = r14.append(r15);	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r15 = r2;	 Catch:{ all -> 0x00fc }
                r14 = r14.append(r15);	 Catch:{ all -> 0x00fc }
                r14 = r14.toString();	 Catch:{ all -> 0x00fc }
                android.util.Log.d(r12, r14);	 Catch:{ all -> 0x00fc }
                monitor-exit(r13);	 Catch:{ all -> 0x00fc }
            L_0x003e:
                return;
            L_0x003f:
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r12 = r12.mBgOrderedScreens;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r14 = r2;	 Catch:{ all -> 0x00fc }
                r12 = r12.get(r14);	 Catch:{ all -> 0x00fc }
                r12 = (java.lang.Long) r12;	 Catch:{ all -> 0x00fc }
                r10 = r12.longValue();	 Catch:{ all -> 0x00fc }
                r7 = new java.util.ArrayList;	 Catch:{ all -> 0x00fc }
                r7.<init>();	 Catch:{ all -> 0x00fc }
                r4 = new java.util.ArrayList;	 Catch:{ all -> 0x00fc }
                r4.<init>();	 Catch:{ all -> 0x00fc }
                r8 = new android.graphics.Rect;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r12 = r3;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r14 = r4;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r15 = r3;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r0 = r5;	 Catch:{ all -> 0x00fc }
                r16 = r0;
                r15 = r15 + r16;
                r0 = r19;
                r0 = r4;	 Catch:{ all -> 0x00fc }
                r16 = r0;
                r0 = r19;
                r0 = r6;	 Catch:{ all -> 0x00fc }
                r17 = r0;
                r16 = r16 + r17;
                r0 = r16;
                r8.<init>(r12, r14, r15, r0);	 Catch:{ all -> 0x00fc }
                r5 = new android.graphics.Rect;	 Catch:{ all -> 0x00fc }
                r5.<init>();	 Catch:{ all -> 0x00fc }
                r6 = new java.util.ArrayList;	 Catch:{ all -> 0x00fc }
                r6.<init>();	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r12 = r12.mBgPagesItems;	 Catch:{ all -> 0x00fc }
                r14 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00fc }
                r12 = r12.get(r14);	 Catch:{ all -> 0x00fc }
                r12 = (java.util.Collection) r12;	 Catch:{ all -> 0x00fc }
                r6.addAll(r12);	 Catch:{ all -> 0x00fc }
                r12 = r6.iterator();	 Catch:{ all -> 0x00fc }
            L_0x00ab:
                r14 = r12.hasNext();	 Catch:{ all -> 0x00fc }
                if (r14 == 0) goto L_0x010e;
            L_0x00b1:
                r3 = r12.next();	 Catch:{ all -> 0x00fc }
                r3 = (com.android.launcher3.common.base.item.ItemInfo) r3;	 Catch:{ all -> 0x00fc }
                r14 = r3.cellX;	 Catch:{ all -> 0x00fc }
                r15 = r3.cellY;	 Catch:{ all -> 0x00fc }
                r0 = r3.cellX;	 Catch:{ all -> 0x00fc }
                r16 = r0;
                r0 = r3.spanX;	 Catch:{ all -> 0x00fc }
                r17 = r0;
                r16 = r16 + r17;
                r0 = r3.cellY;	 Catch:{ all -> 0x00fc }
                r17 = r0;
                r0 = r3.spanY;	 Catch:{ all -> 0x00fc }
                r18 = r0;
                r17 = r17 + r18;
                r0 = r16;
                r1 = r17;
                r5.set(r14, r15, r0, r1);	 Catch:{ all -> 0x00fc }
                r14 = android.graphics.Rect.intersects(r8, r5);	 Catch:{ all -> 0x00fc }
                if (r14 == 0) goto L_0x00ab;
            L_0x00dc:
                r14 = com.android.launcher3.LauncherAppState.getInstance();	 Catch:{ all -> 0x00fc }
                r14 = r14.isHomeOnlyModeEnabled();	 Catch:{ all -> 0x00fc }
                if (r14 == 0) goto L_0x00ff;
            L_0x00e6:
                r14 = r3.itemType;	 Catch:{ all -> 0x00fc }
                if (r14 == 0) goto L_0x00ef;
            L_0x00ea:
                r14 = r3.itemType;	 Catch:{ all -> 0x00fc }
                r15 = 2;
                if (r14 != r15) goto L_0x00ff;
            L_0x00ef:
                r14 = -1;
                r3.cellY = r14;	 Catch:{ all -> 0x00fc }
                r3.cellX = r14;	 Catch:{ all -> 0x00fc }
                r14 = r3.screenId;	 Catch:{ all -> 0x00fc }
                r3.oldScreenId = r14;	 Catch:{ all -> 0x00fc }
                r4.add(r3);	 Catch:{ all -> 0x00fc }
                goto L_0x00ab;
            L_0x00fc:
                r12 = move-exception;
                monitor-exit(r13);	 Catch:{ all -> 0x00fc }
                throw r12;
            L_0x00ff:
                r7.add(r3);	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r14 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r14 = r14.mFavoritesUpdater;	 Catch:{ all -> 0x00fc }
                r14.deleteItem(r3);	 Catch:{ all -> 0x00fc }
                goto L_0x00ab;
            L_0x010e:
                r12 = r7.isEmpty();	 Catch:{ all -> 0x00fc }
                if (r12 != 0) goto L_0x012a;
            L_0x0114:
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r2 = r12.getCallback();	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r14 = new com.android.launcher3.home.HomeLoader$45$1;	 Catch:{ all -> 0x00fc }
                r0 = r19;
                r14.<init>(r2, r7);	 Catch:{ all -> 0x00fc }
                r12.runOnMainThread(r14);	 Catch:{ all -> 0x00fc }
            L_0x012a:
                r12 = r4.isEmpty();	 Catch:{ all -> 0x00fc }
                if (r12 != 0) goto L_0x013c;
            L_0x0130:
                r0 = r19;
                r12 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x00fc }
                r14 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x00fc }
                r15 = 1;
                r12.addAndBindAddedWorkspaceItems(r14, r4, r15);	 Catch:{ all -> 0x00fc }
            L_0x013c:
                monitor-exit(r13);	 Catch:{ all -> 0x00fc }
                goto L_0x003e;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.45.run():void");
            }
        });
    }

    public void removeItem(final ComponentName cn, final boolean isWidget, final UserHandleCompat user) {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                synchronized (HomeLoader.sBgLock) {
                    ArrayList<ItemInfo> itemsByComponentName = DataLoader.getItemInfoByComponentName(cn, user, !isWidget);
                    final ArrayList<ItemInfo> removeList = new ArrayList();
                    Iterator it = itemsByComponentName.iterator();
                    while (it.hasNext()) {
                        ItemInfo itemByComponentName = (ItemInfo) it.next();
                        if (!HomeLoader.this.isAllAppItemInApps(itemByComponentName)) {
                            removeList.add(itemByComponentName);
                            HomeLoader.this.mFavoritesUpdater.deleteItem(itemByComponentName);
                        }
                    }
                    if (!removeList.isEmpty()) {
                        final HomeCallbacks callbacks = HomeLoader.this.getCallback();
                        HomeLoader.this.runOnMainThread(new Runnable() {
                            public void run() {
                                HomeCallbacks cb = HomeLoader.this.getCallback();
                                if (cb != null && callbacks == cb) {
                                    cb.bindItemsRemoved(removeList);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void enableAppsButton(final Context context, final IconInfo appsButton) {
        if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            Log.d(TAG, "Do not call enableAppsButton in homeOnlyMode");
        } else {
            DataLoader.runOnWorkerThread(new Runnable() {
                public void run() {
                    synchronized (HomeLoader.sBgLock) {
                        int index = HomeLoader.this.mBgHotseatItems.size();
                        if (index == LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount()) {
                            index--;
                            Iterator it = HomeLoader.this.mBgHotseatItems.iterator();
                            while (it.hasNext()) {
                                ItemInfo info = (ItemInfo) it.next();
                                if (info.screenId == ((long) index)) {
                                    HomeLoader.this.mFavoritesUpdater.deleteItem(info);
                                    info.screenId = -1;
                                    info.container = -1;
                                    ArrayList<ItemInfo> addedItemsFinal = new ArrayList();
                                    addedItemsFinal.add(info);
                                    HomeLoader.this.addAndBindAddedWorkspaceItems(context, addedItemsFinal, false);
                                    Log.i(HomeLoader.TAG, "Enable apps button replacedItem is " + info);
                                    break;
                                }
                            }
                        }
                        HomeLoader.this.addHotseatItemByItemInfo(appsButton, index);
                    }
                }
            });
        }
    }

    public void disableAppsButton() {
        removeHotseatItemByItemInfo(getAppsButton());
    }

    public void removeHotseatItemByIndex(final int index) {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                synchronized (HomeLoader.sBgLock) {
                    Iterator it = HomeLoader.this.mBgHotseatItems.iterator();
                    while (it.hasNext()) {
                        ItemInfo info = (ItemInfo) it.next();
                        if (!((info instanceof IconInfo) && ((IconInfo) info).isAppsButton) && info.screenId == ((long) index)) {
                            HomeLoader.this.removeHotseatItemByItemInfo(info);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void removeHotseatItemByItemInfo(final ItemInfo info) {
        if (info == null) {
            Log.d(TAG, "removeHotseatItemByItemInfo info is null!!");
            return;
        }
        final HomeCallbacks callbacks = getCallback();
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                synchronized (HomeLoader.sBgLock) {
                    HomeLoader.this.reArrangHotseatItemsByRemoved((int) info.screenId);
                    Log.i(HomeLoader.TAG, "removeHotseatItemByItemInfo info : " + info);
                    HomeLoader.this.mFavoritesUpdater.deleteItem(info);
                    if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && (info.itemType == 0 || info.itemType == 2)) {
                        info.screenId = -1;
                        info.container = -1;
                        ArrayList<ItemInfo> addedItemsFinal = new ArrayList();
                        addedItemsFinal.add(info);
                        HomeLoader.this.addAndBindAddedWorkspaceItems(HomeLoader.sContext, addedItemsFinal, false);
                    }
                }
                final ArrayList<ItemInfo> hotseatItems = new ArrayList();
                hotseatItems.addAll(HomeLoader.this.mBgHotseatItems);
                HomeLoader.this.runOnMainThread(new Runnable() {
                    public void run() {
                        HomeCallbacks cb = HomeLoader.this.getCallback();
                        if (cb != null && callbacks == cb) {
                            cb.bindHotseatItems(hotseatItems);
                        }
                    }
                });
            }
        });
    }

    public void addHotseatItemByComponentName(final ComponentName componentName, final int index, final UserHandleCompat user) {
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                IconInfo info = null;
                if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    ArrayList<ItemInfo> itemInfos = DataLoader.getItemInfoByComponentName(componentName, user, true);
                    if (!itemInfos.isEmpty()) {
                        info = (IconInfo) itemInfos.get(0);
                    }
                } else {
                    List<LauncherActivityInfoCompat> apps = HomeLoader.sLauncherApps.getActivityList(componentName.getPackageName(), user);
                    if (apps != null) {
                        for (LauncherActivityInfoCompat i : apps) {
                            if (componentName.equals(i.getComponentName())) {
                                info = IconInfo.fromActivityInfo(i, HomeLoader.sContext);
                                info.container = -101;
                                break;
                            }
                        }
                    }
                }
                if (info == null) {
                    Log.d(HomeLoader.TAG, componentName + "is not exist");
                } else {
                    HomeLoader.this.addHotseatItemByItemInfo(info, index);
                }
            }
        });
    }

    private void addHotseatItemByItemInfo(final ItemInfo info, final int index) {
        if (info == null) {
            Log.d(TAG, "addHotseatItemByItemInfo info is null!!");
            return;
        }
        final HomeCallbacks callbacks = getCallback();
        DataLoader.runOnWorkerThread(new Runnable() {
            public void run() {
                final ArrayList<ItemInfo> needToRemoveItem = new ArrayList();
                synchronized (HomeLoader.sBgLock) {
                    IconInfo originalItem = info;
                    int maxHotseatCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
                    ArrayList<ItemInfo> needToRearrangeItems = new ArrayList();
                    IconInfo newItem = originalItem.isAppsButton ? originalItem : originalItem.makeCloneInfo();
                    int currentHotseatCount = HomeLoader.this.mBgHotseatItems.size();
                    if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                        if (originalItem.container != -101) {
                            Log.i(HomeLoader.TAG, "remove workspace item(" + originalItem + ")");
                            needToRemoveItem.add(originalItem);
                        } else if (originalItem.screenId == ((long) index)) {
                            Log.d(HomeLoader.TAG, info.title + " is already in index " + index + " in homeOnly");
                            return;
                        } else {
                            currentHotseatCount--;
                            HomeLoader.this.reArrangHotseatItemsByRemoved((int) originalItem.screenId);
                        }
                        HomeLoader.this.mFavoritesUpdater.deleteItem(originalItem);
                    }
                    if (currentHotseatCount == maxHotseatCount) {
                        Log.d(HomeLoader.TAG, "Hotseat is already max(" + maxHotseatCount + ") to add an item");
                        return;
                    }
                    int finalIndex;
                    if (!(LauncherAppState.getInstance().isHomeOnlyModeEnabled() || !LauncherAppState.getInstance().getAppsButtonEnabled() || newItem.isAppsButton)) {
                        currentHotseatCount--;
                    }
                    if (index > currentHotseatCount) {
                        finalIndex = currentHotseatCount;
                    } else {
                        finalIndex = index;
                    }
                    newItem.container = -101;
                    newItem.screenId = (long) finalIndex;
                    newItem.cellX = HomeLoader.this.getCellXFromHotseatOrder(finalIndex);
                    newItem.cellY = HomeLoader.this.getCellYFromHotseatOrder(finalIndex);
                    Iterator it = HomeLoader.this.mBgHotseatItems.iterator();
                    while (it.hasNext()) {
                        ItemInfo hotseatItem = (ItemInfo) it.next();
                        if (hotseatItem.screenId >= ((long) finalIndex)) {
                            needToRearrangeItems.add(hotseatItem);
                        }
                    }
                    if (needToRearrangeItems.size() > 0) {
                        HomeLoader.this.reArrangeHotseatData(needToRearrangeItems, finalIndex + 1);
                    }
                    HomeLoader.this.mFavoritesUpdater.addItem(newItem);
                    final ArrayList<ItemInfo> hotseatItems = new ArrayList();
                    hotseatItems.addAll(HomeLoader.this.mBgHotseatItems);
                    HomeLoader.this.runOnMainThread(new Runnable() {
                        public void run() {
                            HomeCallbacks cb = HomeLoader.this.getCallback();
                            if (cb != null && callbacks == cb) {
                                if (!needToRemoveItem.isEmpty()) {
                                    callbacks.bindItemsRemoved(needToRemoveItem);
                                }
                                callbacks.bindHotseatItems(hotseatItems);
                            }
                        }
                    });
                }
            }
        });
    }

    public void addOrMoveItem(int screen, int cellX, int cellY, int spanX, int spanY, ComponentName cn, boolean isWidget) {
        final int i = screen;
        final boolean z = isWidget;
        final ComponentName componentName = cn;
        final int i2 = spanX;
        final int i3 = spanY;
        final int i4 = cellX;
        final int i5 = cellY;
        DataLoader.runOnWorkerThread(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                r35 = this;
                r29 = com.android.launcher3.home.HomeLoader.sBgLock;
                monitor-enter(r29);
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r28 = r28.mBgOrderedScreens;	 Catch:{ all -> 0x017c }
                r23 = r28.size();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r2;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r1 = r23;
                if (r0 < r1) goto L_0x0073;
            L_0x001f:
                r28 = "HomeLoader";
                r30 = new java.lang.StringBuilder;	 Catch:{ all -> 0x017c }
                r30.<init>();	 Catch:{ all -> 0x017c }
                r31 = "addShortcut : currentPageCount = ";
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r0 = r30;
                r1 = r23;
                r30 = r0.append(r1);	 Catch:{ all -> 0x017c }
                r31 = " addIndex = ";
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r2;	 Catch:{ all -> 0x017c }
                r31 = r0;
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r30 = r30.toString();	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                android.util.Log.d(r0, r1);	 Catch:{ all -> 0x017c }
                r14 = r23;
            L_0x0051:
                r0 = r35;
                r0 = r2;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                if (r14 > r0) goto L_0x0073;
            L_0x005b:
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r30 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r32 = -1;
                r0 = r28;
                r1 = r30;
                r2 = r32;
                r0.insertWorkspaceScreen(r1, r14, r2);	 Catch:{ all -> 0x017c }
                r14 = r14 + 1;
                goto L_0x0051;
            L_0x0073:
                r0 = r35;
                r0 = r3;	 Catch:{ all -> 0x017c }
                r28 = r0;
                if (r28 != 0) goto L_0x0156;
            L_0x007b:
                r28 = com.android.launcher3.LauncherAppState.getInstance();	 Catch:{ all -> 0x017c }
                r28 = r28.isHomeOnlyModeEnabled();	 Catch:{ all -> 0x017c }
                if (r28 == 0) goto L_0x0156;
            L_0x0085:
                r18 = 1;
            L_0x0087:
                r6 = 0;
                r8 = new java.util.ArrayList;	 Catch:{ all -> 0x017c }
                r8.<init>();	 Catch:{ all -> 0x017c }
                r20 = new java.util.ArrayList;	 Catch:{ all -> 0x017c }
                r20.<init>();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r28 = r28.mBgOrderedScreens;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r2;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r28;
                r1 = r30;
                r28 = r0.get(r1);	 Catch:{ all -> 0x017c }
                r28 = (java.lang.Long) r28;	 Catch:{ all -> 0x017c }
                r24 = r28.longValue();	 Catch:{ all -> 0x017c }
                r26 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r3;	 Catch:{ all -> 0x017c }
                r28 = r0;
                if (r28 == 0) goto L_0x017f;
            L_0x00bc:
                r28 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r28 = com.android.launcher3.common.compat.AppWidgetManagerCompat.getInstance(r28);	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r28;
                r1 = r30;
                r2 = r26;
                r22 = r0.findProvider(r1, r2);	 Catch:{ all -> 0x017c }
                r9 = new android.appwidget.AppWidgetHost;	 Catch:{ all -> 0x017c }
                r28 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r30 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
                r0 = r28;
                r1 = r30;
                r9.<init>(r0, r1);	 Catch:{ all -> 0x017c }
                r10 = r9.allocateAppWidgetId();	 Catch:{ all -> 0x017c }
                if (r22 == 0) goto L_0x00ff;
            L_0x00e9:
                r28 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r28 = com.android.launcher3.common.compat.AppWidgetManagerCompat.getInstance(r28);	 Catch:{ all -> 0x017c }
                r30 = 0;
                r0 = r28;
                r1 = r22;
                r2 = r30;
                r28 = r0.bindAppWidgetIdIfAllowed(r10, r1, r2);	 Catch:{ all -> 0x017c }
                if (r28 != 0) goto L_0x015a;
            L_0x00ff:
                r28 = "HomeLoader";
                r30 = new java.lang.StringBuilder;	 Catch:{ all -> 0x017c }
                r30.<init>();	 Catch:{ all -> 0x017c }
                r31 = "addOrMoveItem : Unable to bind app widget id ";
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r0 = r30;
                r30 = r0.append(r10);	 Catch:{ all -> 0x017c }
                r31 = " component ";
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r31 = r0;
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r30 = r30.toString();	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                android.util.Log.d(r0, r1);	 Catch:{ all -> 0x017c }
                r9.deleteAppWidgetId(r10);	 Catch:{ all -> 0x017c }
            L_0x0130:
                if (r6 != 0) goto L_0x01fa;
            L_0x0132:
                r28 = "HomeLoader";
                r30 = new java.lang.StringBuilder;	 Catch:{ all -> 0x017c }
                r30.<init>();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r31 = r0;
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r31 = "is not exist";
                r30 = r30.append(r31);	 Catch:{ all -> 0x017c }
                r30 = r30.toString();	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                android.util.Log.d(r0, r1);	 Catch:{ all -> 0x017c }
                monitor-exit(r29);	 Catch:{ all -> 0x017c }
            L_0x0155:
                return;
            L_0x0156:
                r18 = 0;
                goto L_0x0087;
            L_0x015a:
                r6 = new com.android.launcher3.home.LauncherAppWidgetInfo;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r6.<init>(r10, r0);	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r5;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r6.spanX = r0;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r6;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r6.spanY = r0;	 Catch:{ all -> 0x017c }
                goto L_0x0130;
            L_0x017c:
                r28 = move-exception;
                monitor-exit(r29);	 Catch:{ all -> 0x017c }
                throw r28;
            L_0x017f:
                if (r18 == 0) goto L_0x01ae;
            L_0x0181:
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r30 = 0;
                r0 = r28;
                r1 = r26;
                r2 = r30;
                r17 = com.android.launcher3.common.model.DataLoader.getItemInfoByComponentName(r0, r1, r2);	 Catch:{ all -> 0x017c }
                r28 = r17.iterator();	 Catch:{ all -> 0x017c }
            L_0x0197:
                r30 = r28.hasNext();	 Catch:{ all -> 0x017c }
                if (r30 == 0) goto L_0x0130;
            L_0x019d:
                r16 = r28.next();	 Catch:{ all -> 0x017c }
                r16 = (com.android.launcher3.common.base.item.ItemInfo) r16;	 Catch:{ all -> 0x017c }
                r0 = r16;
                r0 = r0.itemType;	 Catch:{ all -> 0x017c }
                r30 = r0;
                if (r30 != 0) goto L_0x0197;
            L_0x01ab:
                r6 = r16;
                goto L_0x0130;
            L_0x01ae:
                r28 = com.android.launcher3.home.HomeLoader.sLauncherApps;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r30 = r30.getPackageName();	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                r2 = r26;
                r11 = r0.getActivityList(r1, r2);	 Catch:{ all -> 0x017c }
                if (r11 == 0) goto L_0x0130;
            L_0x01c8:
                r28 = r11.iterator();	 Catch:{ all -> 0x017c }
            L_0x01cc:
                r30 = r28.hasNext();	 Catch:{ all -> 0x017c }
                if (r30 == 0) goto L_0x0130;
            L_0x01d2:
                r13 = r28.next();	 Catch:{ all -> 0x017c }
                r13 = (com.android.launcher3.common.compat.LauncherActivityInfoCompat) r13;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r4;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r31 = r13.getComponentName();	 Catch:{ all -> 0x017c }
                r30 = r30.equals(r31);	 Catch:{ all -> 0x017c }
                if (r30 == 0) goto L_0x01cc;
            L_0x01e8:
                r28 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r0 = r28;
                r6 = com.android.launcher3.common.base.item.IconInfo.fromActivityInfo(r13, r0);	 Catch:{ all -> 0x017c }
                r30 = -100;
                r0 = r30;
                r6.container = r0;	 Catch:{ all -> 0x017c }
                goto L_0x0130;
            L_0x01fa:
                r7 = new android.graphics.Rect;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r7;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r35;
                r0 = r8;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r35;
                r0 = r7;	 Catch:{ all -> 0x017c }
                r31 = r0;
                r0 = r6.spanX;	 Catch:{ all -> 0x017c }
                r32 = r0;
                r31 = r31 + r32;
                r0 = r35;
                r0 = r8;	 Catch:{ all -> 0x017c }
                r32 = r0;
                r0 = r6.spanY;	 Catch:{ all -> 0x017c }
                r33 = r0;
                r32 = r32 + r33;
                r0 = r28;
                r1 = r30;
                r2 = r31;
                r3 = r32;
                r7.<init>(r0, r1, r2, r3);	 Catch:{ all -> 0x017c }
                r19 = new android.graphics.Rect;	 Catch:{ all -> 0x017c }
                r19.<init>();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r28 = r28.mBgPagesItems;	 Catch:{ all -> 0x017c }
                r30 = java.lang.Long.valueOf(r24);	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                r21 = r0.get(r1);	 Catch:{ all -> 0x017c }
                r21 = (java.util.ArrayList) r21;	 Catch:{ all -> 0x017c }
                if (r21 == 0) goto L_0x02ac;
            L_0x024a:
                r28 = com.android.launcher3.home.AutoAlignHelper.ITEM_ALIGN_UPWARD;	 Catch:{ all -> 0x017c }
                r0 = r21;
                r1 = r28;
                java.util.Collections.sort(r0, r1);	 Catch:{ all -> 0x017c }
                r28 = r21.iterator();	 Catch:{ all -> 0x017c }
            L_0x0257:
                r30 = r28.hasNext();	 Catch:{ all -> 0x017c }
                if (r30 == 0) goto L_0x02ac;
            L_0x025d:
                r15 = r28.next();	 Catch:{ all -> 0x017c }
                r15 = (com.android.launcher3.common.base.item.ItemInfo) r15;	 Catch:{ all -> 0x017c }
                r0 = r15.cellX;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r15.cellY;	 Catch:{ all -> 0x017c }
                r31 = r0;
                r0 = r15.cellX;	 Catch:{ all -> 0x017c }
                r32 = r0;
                r0 = r15.spanX;	 Catch:{ all -> 0x017c }
                r33 = r0;
                r32 = r32 + r33;
                r0 = r15.cellY;	 Catch:{ all -> 0x017c }
                r33 = r0;
                r0 = r15.spanY;	 Catch:{ all -> 0x017c }
                r34 = r0;
                r33 = r33 + r34;
                r0 = r19;
                r1 = r30;
                r2 = r31;
                r3 = r32;
                r4 = r33;
                r0.set(r1, r2, r3, r4);	 Catch:{ all -> 0x017c }
                r0 = r19;
                r30 = android.graphics.Rect.intersects(r7, r0);	 Catch:{ all -> 0x017c }
                if (r30 == 0) goto L_0x0257;
            L_0x0294:
                r30 = -1;
                r0 = r30;
                r15.cellY = r0;	 Catch:{ all -> 0x017c }
                r0 = r30;
                r15.cellX = r0;	 Catch:{ all -> 0x017c }
                r0 = r15.screenId;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r30;
                r15.oldScreenId = r0;	 Catch:{ all -> 0x017c }
                r0 = r20;
                r0.add(r15);	 Catch:{ all -> 0x017c }
                goto L_0x0257;
            L_0x02ac:
                if (r18 == 0) goto L_0x02c7;
            L_0x02ae:
                r0 = r6.container;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r32 = -100;
                r28 = (r30 > r32 ? 1 : (r30 == r32 ? 0 : -1));
                if (r28 == 0) goto L_0x0388;
            L_0x02b8:
                r0 = r6.container;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r30;
                r6.setPrevContainer(r0);	 Catch:{ all -> 0x017c }
                r30 = -100;
                r0 = r30;
                r6.container = r0;	 Catch:{ all -> 0x017c }
            L_0x02c7:
                r0 = r24;
                r6.screenId = r0;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r7;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r6.cellX = r0;	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = r8;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r0 = r28;
                r6.cellY = r0;	 Catch:{ all -> 0x017c }
                r8.add(r6);	 Catch:{ all -> 0x017c }
                if (r18 == 0) goto L_0x0392;
            L_0x02e4:
                r27 = new android.content.ContentValues;	 Catch:{ all -> 0x017c }
                r27.<init>();	 Catch:{ all -> 0x017c }
                r28 = "container";
                r0 = r6.container;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r30 = java.lang.Long.valueOf(r30);	 Catch:{ all -> 0x017c }
                r0 = r27;
                r1 = r28;
                r2 = r30;
                r0.put(r1, r2);	 Catch:{ all -> 0x017c }
                r28 = "screen";
                r0 = r6.screenId;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r30 = java.lang.Long.valueOf(r30);	 Catch:{ all -> 0x017c }
                r0 = r27;
                r1 = r28;
                r2 = r30;
                r0.put(r1, r2);	 Catch:{ all -> 0x017c }
                r28 = "cellX";
                r0 = r6.cellX;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r30 = java.lang.Integer.valueOf(r30);	 Catch:{ all -> 0x017c }
                r0 = r27;
                r1 = r28;
                r2 = r30;
                r0.put(r1, r2);	 Catch:{ all -> 0x017c }
                r28 = "cellY";
                r0 = r6.cellY;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r30 = java.lang.Integer.valueOf(r30);	 Catch:{ all -> 0x017c }
                r0 = r27;
                r1 = r28;
                r2 = r30;
                r0.put(r1, r2);	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r28 = r28.mFavoritesUpdater;	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r27;
                r0.updateItem(r1, r6);	 Catch:{ all -> 0x017c }
            L_0x0346:
                r28 = r20.isEmpty();	 Catch:{ all -> 0x017c }
                if (r28 != 0) goto L_0x0363;
            L_0x034c:
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r30 = com.android.launcher3.home.HomeLoader.sContext;	 Catch:{ all -> 0x017c }
                r31 = 0;
                r0 = r28;
                r1 = r30;
                r2 = r20;
                r3 = r31;
                r0.addAndBindAddedWorkspaceItems(r1, r2, r3);	 Catch:{ all -> 0x017c }
            L_0x0363:
                if (r18 == 0) goto L_0x03a2;
            L_0x0365:
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r12 = r28.getCallback();	 Catch:{ all -> 0x017c }
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r30 = new com.android.launcher3.home.HomeLoader$52$1;	 Catch:{ all -> 0x017c }
                r0 = r30;
                r1 = r35;
                r0.<init>(r12, r8);	 Catch:{ all -> 0x017c }
                r0 = r28;
                r1 = r30;
                r0.runOnMainThread(r1);	 Catch:{ all -> 0x017c }
            L_0x0385:
                monitor-exit(r29);	 Catch:{ all -> 0x017c }
                goto L_0x0155;
            L_0x0388:
                r0 = r6.screenId;	 Catch:{ all -> 0x017c }
                r30 = r0;
                r0 = r30;
                r6.oldScreenId = r0;	 Catch:{ all -> 0x017c }
                goto L_0x02c7;
            L_0x0392:
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r28 = r28.mFavoritesUpdater;	 Catch:{ all -> 0x017c }
                r0 = r28;
                r0.addItem(r6);	 Catch:{ all -> 0x017c }
                goto L_0x0346;
            L_0x03a2:
                r0 = r35;
                r0 = com.android.launcher3.home.HomeLoader.this;	 Catch:{ all -> 0x017c }
                r28 = r0;
                r30 = 0;
                r0 = r28;
                r1 = r30;
                r0.bindItems(r8, r1);	 Catch:{ all -> 0x017c }
                goto L_0x0385;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeLoader.52.run():void");
            }
        });
    }

    public void removeWidgetIfNeeded(String packageName, UserHandleCompat user) {
        ArrayList<LauncherAppWidgetInfo> widgetItems = getWidgetsInHome();
        ArrayList<String> removePackages = new ArrayList();
        ArrayList<ItemInfo> removeComponents = new ArrayList();
        Iterator it = widgetItems.iterator();
        while (it.hasNext()) {
            LauncherAppWidgetInfo item = (LauncherAppWidgetInfo) it.next();
            ComponentName cn = item.providerName;
            if (cn != null && user.equals(item.user) && packageName.equals(cn.getPackageName()) && AppWidgetManagerCompat.getInstance(sContext).findProvider(cn, user) == null) {
                boolean isVaild = false;
                try {
                    if (Utilities.isComponentActive(sContext, cn, sPackageManager.getPackageInfo(packageName, 2).receivers)) {
                        isVaild = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "removeWidgetIfNeeded e : " + e.toString());
                }
                if (!isVaild) {
                    Log.e(TAG, "Can't find widget component info : " + cn.flattenToShortString() + ", therefore it will be removed");
                    removeComponents.add(item);
                }
            }
        }
        removePackagesAndComponents(removePackages, removeComponents, user, 0);
    }

    public static boolean checkHiddenWidget(Context context, ComponentName name) {
        for (AppWidgetProviderInfo pInfo : AppWidgetManager.getInstance(context).semGetInstalledProviders(32768)) {
            if (pInfo.provider.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
