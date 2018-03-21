package com.android.launcher3.common.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProviderID;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.model.DataUpdater.UpdaterInterface;
import com.android.launcher3.common.model.FavoritesProvider.AppOrderModify;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FavoritesUpdater extends DataUpdater implements UpdaterInterface {
    private static final String TAG = "FavoritesUpdater";
    private final FavoritesProvider mFavoritesProvider = FavoritesProvider.getInstance();
    private final DataLoader mLoader;

    public FavoritesUpdater(Context context, LauncherModel model, IconCache cache, DataLoader loaderInterface) {
        init(context, model, cache);
        this.mLoader = loaderInterface;
    }

    public long addItem(ItemInfo item) {
        return addItem(item, false);
    }

    public long addItem(ItemInfo item, boolean restoreDeepShortcut) {
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(sContext, values);
        if (item.id == -1) {
            item.id = this.mFavoritesProvider.generateNewItemId();
            values.put("_id", Long.valueOf(item.id));
        }
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final ItemInfo itemInfo = item;
        final boolean z = restoreDeepShortcut;
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                DataUpdater.sContentResolver.insert(Favorites.CONTENT_URI, values);
                synchronized (DataLoader.sBgLock) {
                    FavoritesUpdater.checkItemInfoLocked(itemInfo.id, itemInfo, stackTrace);
                    DataLoader.sBgItemsIdMap.put(itemInfo.id, itemInfo);
                    if ((itemInfo instanceof IconInfo) || (itemInfo instanceof LauncherAppWidgetInfo)) {
                        if (FavoritesUpdater.this.isValidContainer(itemInfo.container)) {
                            FavoritesUpdater.this.mLoader.addPagesItem(itemInfo);
                        }
                    } else if (itemInfo instanceof FolderInfo) {
                        DataLoader.sBgFolders.put(itemInfo.id, (FolderInfo) itemInfo);
                        FavoritesUpdater.this.mLoader.addPagesItem(itemInfo);
                        FavoritesUpdater.this.logFolderCount(itemInfo);
                    }
                    if ((itemInfo instanceof IconInfo) && itemInfo.itemType == 6 && !z) {
                        DataLoader.incrementPinnedShortcutCount(ShortcutKey.fromShortcutInfo((IconInfo) itemInfo), true);
                    }
                }
            }
        });
        return item.id;
    }

    public void updateItem(final ContentValues values, final ItemInfo item) {
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                if (DataUpdater.sContentResolver.update(Favorites.getContentUri(item.id), values, null, null) > 0) {
                    synchronized (DataLoader.sBgLock) {
                        if (!FavoritesUpdater.this.isValidContainer(item.container)) {
                            FavoritesUpdater.this.mLoader.removePagesItem(item);
                        } else if (FavoritesUpdater.this.mLoader.containPagesItem(item)) {
                            FavoritesUpdater.this.mLoader.updatePagesItem(item);
                        } else {
                            FavoritesUpdater.this.mLoader.addPagesItem(item);
                        }
                    }
                }
            }
        });
    }

    public void deleteItem(ItemInfo item) {
        deleteItem(item, false);
    }

    public void deleteItem(final ItemInfo item, final boolean restoreDeepShortcut) {
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                if (DataUpdater.sContentResolver.delete(Favorites.getContentUri(item.id), null, null) > 0) {
                    synchronized (DataLoader.sBgLock) {
                        if ((item instanceof IconInfo) || (item instanceof LauncherAppWidgetInfo)) {
                            if (FavoritesUpdater.this.isValidContainer(item.container)) {
                                FavoritesUpdater.this.mLoader.removePagesItem(item);
                            }
                        } else if (item instanceof FolderInfo) {
                            PostPositionController pp = PostPositionController.getInstance(DataUpdater.sContext);
                            FolderInfo folder = (FolderInfo)item;
                            if (folder.contents.size() == 1) {
                                pp.writeFolderReadyIdForNoFDR(item.container, folder.title.toString(), ((IconInfo) folder.contents.get(0)).id);
                            } else {
                                pp.deleteFolder(item.id);
                            }
                            DataLoader.sBgFolders.remove(item.id);
                            FavoritesUpdater.this.mLoader.removePagesItem(item);
                            FavoritesUpdater.this.logFolderCount(item);
                        }
                        if ((item instanceof IconInfo) && item.itemType == 6 && !restoreDeepShortcut) {
                            DataLoader.decrementPinnedShortcutCount(ShortcutKey.fromShortcutInfo((IconInfo) item));
                        }
                        DataLoader.sBgItemsIdMap.remove(item.id);
                    }
                }
            }
        });
    }

    public void updateItemsInDatabaseHelper(Context context, ArrayList<ContentValues> valuesList, ArrayList<ItemInfo> items) {
        final ContentResolver cr = context.getContentResolver();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final ArrayList<ItemInfo> arrayList = items;
        final ArrayList<ContentValues> arrayList2 = valuesList;
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList();
                int count = arrayList.size();
                for (int i = 0; i < count; i++) {
                    ItemInfo item = (ItemInfo) arrayList.get(i);
                    long itemId = item.id;
                    ContentValues values = (ContentValues) arrayList2.get(i);
                    ops.add(ContentProviderOperation.newUpdate(Favorites.getContentUri(itemId)).withValues(values).build());
                    FavoritesUpdater.this.updateItemArrays(item, itemId, stackTrace);
                }
                try {
                    cr.applyBatch("com.sec.android.app.launcher.settings", ops);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateItemArrays(ItemInfo item, long itemId, StackTraceElement[] stackTrace) {
        synchronized (DataLoader.sBgLock) {
            checkItemInfoLocked(itemId, item, stackTrace);
            if (!(isValidContainer(item.container) || DataLoader.sBgFolders.containsKey(item.container))) {
                Log.e(TAG, "item: " + item + " container being set to: " + item.container + ", not in the list of folders");
            }
            ItemInfo modelItem = (ItemInfo) DataLoader.sBgItemsIdMap.get(itemId);
            if (modelItem != null && isValidContainer(modelItem.container)) {
                switch (modelItem.itemType) {
                    case 0:
                    case 1:
                    case 2:
                    case 6:
                    case 7:
                        if (!this.mLoader.containPagesItem(modelItem)) {
                            this.mLoader.addPagesItem(modelItem);
                            break;
                        } else {
                            this.mLoader.updatePagesItem(modelItem);
                            break;
                        }
                }
            }
            this.mLoader.removePagesItem(modelItem);
        }
    }

    private static boolean checkItemsConsistent(ItemInfo item1, ItemInfo item2) {
        if (item1.id == item2.id && item1.itemType == item2.itemType && item1.container == item2.container && item1.screenId == item2.screenId && item1.cellX == item2.cellX && item1.cellY == item2.cellY && item1.spanX == item2.spanX && item1.spanY == item2.spanY) {
            if (item1.dropPos == null && item2.dropPos == null) {
                return true;
            }
            if (item1.dropPos != null && item2.dropPos != null && item1.dropPos[0] == item2.dropPos[0] && item1.dropPos[1] == item2.dropPos[1]) {
                return true;
            }
        }
        return false;
    }

    private static void checkItemInfoLocked(long itemId, ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = (ItemInfo) DataLoader.sBgItemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            IconInfo modelInfo;
            IconInfo info;
            if ((modelItem instanceof IconInfo) && (item instanceof IconInfo)) {
                modelInfo = (IconInfo) modelItem;
                info = (IconInfo) item;
                if (modelInfo.intent.filterEquals(info.intent) && checkItemsConsistent(modelInfo, info)) {
                    return;
                }
            } else if ((modelItem instanceof FolderInfo) && (item instanceof FolderInfo) && checkItemsConsistent((FolderInfo) modelItem, (FolderInfo) item)) {
                return;
            }
            if ((modelItem instanceof IconInfo) && (item instanceof IconInfo)) {
                modelInfo = (IconInfo) modelItem;
                info = (IconInfo) item;
                long container = modelInfo.container;
                if ((container == -102 || (container > 0 && (DataLoader.sBgItemsIdMap.get(container) instanceof FolderInfo) && ((FolderInfo) DataLoader.sBgItemsIdMap.get(container)).container == -102)) && !(modelInfo.cellX == info.cellX && modelInfo.cellY == info.cellY)) {
                    Log.i(TAG, "Position changed apps item, but not problem : item=" + item + " modelItem=" + modelItem);
                    return;
                }
            }
            RuntimeException e = new RuntimeException("item: " + item + "modelItem: " + modelItem + "Error: ItemInfo passed to checkItemInfo doesn't match original");
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
    }

    public void deletePackageFromDatabase(String pn, UserHandleCompat user) {
        deleteItemsFromDatabase(DataLoader.getItemsByPackageName(pn, user));
    }

    public void deleteItemsFromDatabase(final ArrayList<? extends ItemInfo> items) {
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                boolean folderCountChanged = false;
                Iterator it = items.iterator();
                while (it.hasNext()) {
                    ItemInfo item = (ItemInfo) it.next();
                    DataUpdater.sContentResolver.delete(Favorites.getContentUri(item.id), null, null);
                    synchronized (DataLoader.sBgLock) {
                        if ((item instanceof IconInfo) || (item instanceof LauncherAppWidgetInfo)) {
                            if (FavoritesUpdater.this.isValidContainer(item.container)) {
                                FavoritesUpdater.this.mLoader.removePagesItem(item);
                            }
                        } else if (item instanceof FolderInfo) {
                            PostPositionController pp = PostPositionController.getInstance(DataUpdater.sContext);
                            FolderInfo folder = (FolderInfo) item;
                            if (folder.contents.size() == 1) {
                                pp.writeFolderReadyIdForNoFDR(item.container, folder.title.toString(), ((IconInfo) folder.contents.get(0)).id);
                            } else {
                                pp.deleteFolder(item.id);
                            }
                            DataLoader.sBgFolders.remove(item.id);
                            FavoritesUpdater.this.mLoader.removePagesItem(item);
                            folderCountChanged = true;
                        }
                        if ((item instanceof IconInfo) && item.itemType == 6) {
                            DataLoader.decrementPinnedShortcutCount(ShortcutKey.fromShortcutInfo((IconInfo) item));
                        }
                        DataLoader.sBgItemsIdMap.remove(item.id);
                    }
                }
                if (folderCountChanged) {
                    FavoritesUpdater.this.logFolderCount(null);
                }
            }
        });
    }

    public void updateScreenOrder(Context context, ArrayList<Long> screens) {
        final ArrayList<Long> screensCopy = new ArrayList(screens);
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = WorkspaceScreens.CONTENT_URI;
        Iterator<Long> iter = screensCopy.iterator();
        while (iter.hasNext()) {
            if (((Long) iter.next()).longValue() < 0) {
                iter.remove();
            }
        }
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList();
                ops.add(ContentProviderOperation.newDelete(uri).build());
                int count = screensCopy.size();
                for (int i = 0; i < count; i++) {
                    ContentValues v = new ContentValues();
                    v.put("_id", Long.valueOf(((Long) screensCopy.get(i)).longValue()));
                    v.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                    ops.add(ContentProviderOperation.newInsert(uri).withValues(v).build());
                }
                try {
                    cr.applyBatch("com.sec.android.app.launcher.settings", ops);
                    synchronized (DataLoader.sBgLock) {
                        FavoritesUpdater.this.mLoader.setOrderedScreen(screensCopy);
                    }
                    SALogging.getInstance().updatePageLogs();
                    LauncherProviderID providerID = LauncherAppState.getLauncherProviderID();
                    if (providerID != null) {
                        providerID.updateScreenCount();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void deleteFolderContentsFromDatabase(final FolderInfo info) {
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                DataUpdater.sContentResolver.delete(Favorites.getContentUri(info.id), null, null);
                synchronized (DataLoader.sBgLock) {
                    DataLoader.sBgItemsIdMap.remove(info.id);
                    DataLoader.sBgFolders.remove(info.id);
                    FavoritesUpdater.this.mLoader.removePagesItem(info);
                    FavoritesUpdater.this.logFolderCount(info);
                }
                PostPositionController.getInstance(DataUpdater.sContext).deleteFolder(info.id);
                boolean isHomeOnly = LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled();
                if (!isHomeOnly) {
                    DataUpdater.sContentResolver.delete(Favorites.CONTENT_URI, "container=" + info.id, null);
                    synchronized (DataLoader.sBgLock) {
                        Iterator it = info.contents.iterator();
                        while (it.hasNext()) {
                            DataLoader.sBgItemsIdMap.remove(((ItemInfo) it.next()).id);
                        }
                    }
                }
            }
        });
    }

    public static void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                synchronized (DataLoader.sBgLock) {
                    FavoritesUpdater.checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        });
    }

    public void addItems(ArrayList<ItemInfo> items) {
        addItems(items, true);
    }

    public void addItems(ArrayList<ItemInfo> items, boolean addToMap) {
        final List<AppOrderModify> updates = createAppOrderModifyByItem(items);
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final boolean z = addToMap;
        final ArrayList<ItemInfo> arrayList = items;
        DataUpdater.runOnWorkerThread(new Runnable() {
            public void run() {
                if (!updates.isEmpty()) {
                    FavoritesUpdater.this.mFavoritesProvider.updateAppItems(updates);
                }
                if (z) {
                    synchronized (DataLoader.sBgLock) {
                        Iterator it = arrayList.iterator();
                        while (it.hasNext()) {
                            ItemInfo item = (ItemInfo) it.next();
                            FavoritesUpdater.checkItemInfoLocked(item.id, item, stackTrace);
                            DataLoader.sBgItemsIdMap.put(item.id, item);
                        }
                    }
                }
            }
        });
    }

    private List<AppOrderModify> createAppOrderModifyByItem(ArrayList<ItemInfo> items) {
        List<AppOrderModify> updates = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            IconInfo item = (IconInfo) ((ItemInfo) it.next());
            if (item.id == -1) {
                item.id = this.mFavoritesProvider.generateNewItemId();
            }
            AppOrderModify update = new AppOrderModify();
            update.action = 3;
            update.component = item.componentName;
            update.id = item.id;
            update.container = item.container;
            update.screen = item.screenId;
            update.rank = item.rank;
            update.title = item.title;
            update.user = item.user;
            update.itemtype = 0;
            update.modified = System.currentTimeMillis();
            update.status = item.status;
            updates.add(update);
        }
        return updates;
    }

    private void logFolderCount(ItemInfo item) {
        int homeFolderCount = 0;
        int appsFolderCount = 0;
        synchronized (DataLoader.sBgLock) {
            Iterator it = DataLoader.sBgFolders.iterator();
            while (it.hasNext()) {
                if (((FolderInfo) it.next()).container == -102) {
                    appsFolderCount++;
                } else {
                    homeFolderCount++;
                }
            }
        }
        if (item == null) {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_FOLDER_COUNT, null, (long) appsFolderCount, true);
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_FOLDER_COUNT, null, (long) homeFolderCount, true);
        } else if (item.container == -102) {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_FOLDER_COUNT, null, (long) appsFolderCount, true);
        } else {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_FOLDER_COUNT, null, (long) homeFolderCount, true);
        }
    }

    private boolean isValidContainer(long container) {
        return container == -100 || container == -101 || container == -102;
    }
}
