package com.android.launcher3.allapps.controller;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import com.android.launcher3.allapps.controller.AppsAdapterProvider.DataHolder;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class AppsAlphabetAdapter extends AppsAdapter {
    private static final String TAG = "AppsAlphabetAdapter";
    private final Normalizer<Object> mNormalizer = new AlphabetNormalizer();

    private static final class RawItemInfo {
        int rank;
        int screenId;

        private RawItemInfo() {
            this.screenId = -1;
            this.rank = -1;
        }
    }

    public /* bridge */ /* synthetic */ long addItem(ItemInfo itemInfo) {
        return super.addItem(itemInfo);
    }

    public /* bridge */ /* synthetic */ void addItemToFolder(FolderInfo folderInfo, ArrayList arrayList) {
        super.addItemToFolder(folderInfo, arrayList);
    }

    public /* bridge */ /* synthetic */ void deleteItem(ItemInfo itemInfo) {
        super.deleteItem(itemInfo);
    }

    public /* bridge */ /* synthetic */ ItemInfo getItemById(long j) {
        return super.getItemById(j);
    }

    public /* bridge */ /* synthetic */ View inflateView(ItemInfo itemInfo) {
        return super.inflateView(itemInfo);
    }

    public /* bridge */ /* synthetic */ void notifyUpdate(ArrayList arrayList) {
        super.notifyUpdate(arrayList);
    }

    public /* bridge */ /* synthetic */ void onLoadComplete(int i) {
        super.onLoadComplete(i);
    }

    public /* bridge */ /* synthetic */ void onLoadStart() {
        super.onLoadStart();
    }

    public /* bridge */ /* synthetic */ void removeAllItems() {
        super.removeAllItems();
    }

    public /* bridge */ /* synthetic */ void resume() {
        super.resume();
    }

    public /* bridge */ /* synthetic */ void setStateAndUpdateLock(int i, boolean z) {
        super.setStateAndUpdateLock(i, z);
    }

    public /* bridge */ /* synthetic */ void terminate() {
        super.terminate();
    }

    public /* bridge */ /* synthetic */ void updateItem(ContentValues contentValues, ItemInfo itemInfo) {
        super.updateItem(contentValues, itemInfo);
    }

    public /* bridge */ /* synthetic */ void updateRestoreItems(HashSet hashSet) {
        super.updateRestoreItems(hashSet);
    }

    AppsAlphabetAdapter(Context context, DataListener listener, DataHolder holder) {
        super(context, listener, holder);
    }

    protected void updateAppsContents(ArrayList<ItemInfo> oldItems) {
        if (this.mItems.containsAll(oldItems) && this.mItems.size() == oldItems.size()) {
            Log.d(TAG, "no change items");
            return;
        }
        ArrayList<ItemInfo> removed = new ArrayList(oldItems);
        removed.removeAll(this.mItems);
        this.mListener.removeApps(removed);
        ArrayList<ItemInfo> added = new ArrayList(this.mItems);
        added.removeAll(oldItems);
        this.mNormalizer.normalize(this.mItems, this.mListener.getMaxItemsPerScreen(), this.mListener.getCellCountX());
        Collections.sort(added, ORDER_COMPARATOR);
        this.mListener.rearrangeAllViews(false);
        addItemView(added, true);
        AppsAdapter.debugItemInfo("removeApps : ", (ArrayList) removed);
        AppsAdapter.debugItemInfo("addItemView", (ArrayList) added);
    }

    protected void updateFolderContents(LongArrayMap<ArrayList<ItemInfo>> folderItems) {
        int count = folderItems.size();
        boolean rearrange = false;
        for (int index = 0; index < count; index++) {
            long key = folderItems.keyAt(index);
            if (key != -102) {
                ArrayList<ItemInfo> items = (ArrayList) folderItems.valueAt(index);
                ArrayList<ItemInfo> deleted = new ArrayList();
                ArrayList<ItemInfo> updated = new ArrayList();
                Iterator it = items.iterator();
                while (it.hasNext()) {
                    ItemInfo item = (ItemInfo) it.next();
                    if (this.mAppsModel.getItemById(item.id) == null || item.hidden != 0) {
                        deleted.add(item);
                    } else {
                        updated.add(item);
                    }
                }
                this.mListener.updateApps(updated);
                this.mListener.removeApps(deleted);
                AppsAdapter.debugItemInfo("removeApps : ", this.mAppsModel.getItemById(key));
                AppsAdapter.debugItemInfo("update folder item : ", (ArrayList) updated);
                AppsAdapter.debugItemInfo("remove folder item : ", (ArrayList) deleted);
                if (!deleted.isEmpty()) {
                    rearrange = true;
                }
            }
        }
        if (rearrange) {
            this.mNormalizer.normalize(this.mItems, this.mListener.getMaxItemsPerScreen(), this.mListener.getCellCountX());
            this.mListener.rearrangeAllViews(false);
        }
    }

    public void updateIconAndTitle(final ArrayList<ItemInfo> updated) {
        this.mHandler.post(new Runnable() {
            public void run() {
                if (AppsAlphabetAdapter.this.mDataHolder.mDestroyed) {
                    Log.d(AppsAlphabetAdapter.TAG, "ignore updateIconAndTitle because of destroyed : " + AppsAlphabetAdapter.this.mLauncher);
                    return;
                }
                AppsAlphabetAdapter.this.mListener.updateApps(updated);
                AppsAlphabetAdapter.this.getNormalizer().normalize(AppsAlphabetAdapter.this.mItems, AppsAlphabetAdapter.this.mListener.getMaxItemsPerScreen(), AppsAlphabetAdapter.this.mListener.getCellCountX());
                AppsAlphabetAdapter.this.mListener.rearrangeAllViews(false);
            }
        });
    }

    public Normalizer<Object> getNormalizer() {
        return this.mNormalizer;
    }

    public ViewType getViewType() {
        return ViewType.ALPHABETIC_GRID;
    }

    public void createFolderAndAddItem(FolderInfo folderInfo, ItemInfo targetItem, ArrayList<IconInfo> infos) {
        AppsAdapter.debugItemInfo("createFolderAndAddItem", (ItemInfo) folderInfo);
        ContentResolver resolver = this.mLauncher.getContentResolver();
        Uri contentUri = Favorites.CONTENT_URI;
        String[] projection = new String[]{"_id", "screen", BaseLauncherColumns.RANK};
        String selection = "_id=" + targetItem.id;
        RawItemInfo rawItemInfo = new RawItemInfo();
        Cursor c = null;
        try {
            c = resolver.query(contentUri, projection, selection, null, null);
            if (c == null) {
                throw new IllegalStateException("query fail, itemInfo : " + targetItem);
            }
            while (c.moveToNext()) {
                rawItemInfo.screenId = c.getInt(c.getColumnIndexOrThrow("screen"));
                rawItemInfo.rank = c.getInt(c.getColumnIndexOrThrow(BaseLauncherColumns.RANK));
            }
            folderInfo.container = -102;
            final long oldParentId = targetItem.container;
            final boolean isInApps = oldParentId == -102;
            folderInfo.container = -102;
            folderInfo.screenId = -1;
            folderInfo.rank = -1;
            this.mAppsModel.addItem(folderInfo);
            ContentValues values = new ContentValues();
            values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
            values.put("screen", Integer.valueOf(isInApps ? rawItemInfo.screenId : -1));
            values.put(BaseLauncherColumns.RANK, Integer.valueOf(isInApps ? rawItemInfo.rank : -1));
            this.mAppsModel.updateItem(values, folderInfo);
            Log.d(TAG, "position info to create folder : " + values.getAsLong("container") + " , " + values.getAsLong("screen") + " , " + values.getAsLong(BaseLauncherColumns.RANK));
            ArrayList<ContentValues> valueList = new ArrayList();
            final ArrayList<ItemInfo> itemInfos = new ArrayList();
            Iterator it = infos.iterator();
            while (it.hasNext()) {
                IconInfo item = (IconInfo) it.next();
                item.container = folderInfo.id;
                item.screenId = -1;
                item.rank = -1;
                item.mDirty = true;
                ContentValues update = new ContentValues();
                update.put("container", Long.valueOf(folderInfo.id));
                update.put("screen", Integer.valueOf(-1));
                update.put(BaseLauncherColumns.RANK, Integer.valueOf(-1));
                valueList.add(update);
                itemInfos.add(item);
            }
            this.mAppsModel.updateItemsInDatabaseHelper(this.mLauncher, valueList, itemInfos);
            folderInfo.add((ArrayList) infos);
            final FolderInfo folderInfo2 = folderInfo;
            final ArrayList<IconInfo> arrayList = infos;
            final ItemInfo itemInfo = targetItem;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (AppsAlphabetAdapter.this.mDataHolder.mDestroyed || AppsAlphabetAdapter.this.mAppsModel.isUpdateLocked() || AppsAlphabetAdapter.this.mItems.isEmpty()) {
                        Log.d(AppsAlphabetAdapter.TAG, "createFolderAndAddItem ignore : " + AppsAlphabetAdapter.this.mAppsModel.isUpdateLocked() + " , " + AppsAlphabetAdapter.this.mItems.isEmpty());
                    } else if (AppsAlphabetAdapter.this.mItems.contains(folderInfo2)) {
                        Iterator it = arrayList.iterator();
                        while (it.hasNext()) {
                            IconInfo item = (IconInfo) it.next();
                            if (AppsAlphabetAdapter.this.mItems.contains(item)) {
                                AppsAlphabetAdapter.this.mItems.remove(item);
                                AppsAlphabetAdapter.this.removeViewAndReorder(item);
                            }
                        }
                        Log.d(AppsAlphabetAdapter.TAG, "already folder created " + folderInfo2);
                    } else {
                        if (!isInApps) {
                            View v = AppsAlphabetAdapter.this.mListener.getAppsIconByItemId(oldParentId);
                            if (v != null) {
                                ((FolderInfo) v.getTag()).remove((IconInfo) itemInfo);
                            }
                        }
                        AppsAlphabetAdapter.this.mItems.removeAll(itemInfos);
                        AppsAlphabetAdapter.this.mListener.removeEmptyCellsAndViews(itemInfos);
                        AppsAlphabetAdapter.this.mItems.add(folderInfo2);
                        AppsAlphabetAdapter.this.mNormalizer.normalize(AppsAlphabetAdapter.this.mItems, AppsAlphabetAdapter.this.mListener.getMaxItemsPerScreen(), AppsAlphabetAdapter.this.mListener.getCellCountX());
                        AppsAlphabetAdapter.this.mListener.rearrangeAllViews(false);
                        AppsAlphabetAdapter.this.addItemView(folderInfo2);
                        AppsAdapter.debugItemInfo("createfolder for postposition", folderInfo2);
                        AppsAdapter.debugItemInfo("addItem to folder for postposition", arrayList);
                        Log.d(AppsAlphabetAdapter.TAG, "postposition-createFolder : " + AppsAlphabetAdapter.this.mAppsModel.isUpdateLocked() + " , " + AppsAlphabetAdapter.this.mDataHolder.mFirstLoaded + " , " + AppsAlphabetAdapter.this.mDataHolder.mModelPrepared + " , " + AppsAlphabetAdapter.this.mItems.size());
                    }
                }
            });
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public String getTag() {
        return TAG;
    }

    protected void removeViewAndReorder(IconInfo item) {
        ArrayList<ItemInfo> items = new ArrayList();
        items.add(item);
        this.mListener.removeEmptyCellsAndViews(items);
        this.mNormalizer.normalize(this.mItems, this.mListener.getMaxItemsPerScreen(), this.mListener.getCellCountX());
        this.mListener.rearrangeAllViews(false);
    }
}
