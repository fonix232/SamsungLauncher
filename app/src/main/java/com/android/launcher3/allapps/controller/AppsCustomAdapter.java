package com.android.launcher3.allapps.controller;

import android.content.ContentValues;
import android.content.Context;
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

public class AppsCustomAdapter extends AppsAdapter {
    private static final String TAG = "AppsCustomAdapter";
    private final Normalizer<Object> mNormalizer = new CustomNormalizer();
    private int mState = 0;
    private Normalizer<Object> mTidyupNormalizer;

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

    public /* bridge */ /* synthetic */ void terminate() {
        super.terminate();
    }

    public /* bridge */ /* synthetic */ void updateIconAndTitle(ArrayList arrayList) {
        super.updateIconAndTitle(arrayList);
    }

    public /* bridge */ /* synthetic */ void updateItem(ContentValues contentValues, ItemInfo itemInfo) {
        super.updateItem(contentValues, itemInfo);
    }

    public /* bridge */ /* synthetic */ void updateRestoreItems(HashSet hashSet) {
        super.updateRestoreItems(hashSet);
    }

    AppsCustomAdapter(Context context, DataListener listener, DataHolder holder) {
        super(context, listener, holder);
    }

    protected void updateAppsContents(ArrayList<ItemInfo> oldItems) {
        if (this.mItems.containsAll(oldItems) && this.mItems.size() == oldItems.size()) {
            Log.d(TAG, "no change items");
            return;
        }
        ArrayList<ItemInfo> removed = new ArrayList(oldItems);
        removed.removeAll(this.mItems);
        AppsAdapter.debugItemInfo("removeApps : ", (ArrayList) removed);
        this.mListener.removeApps(removed);
        ArrayList<ItemInfo> added = new ArrayList(this.mItems);
        added.removeAll(oldItems);
        getNormalizer().normalize(this.mItems, this.mListener.getMaxItemsPerScreen(), this.mListener.getCellCountX());
        Collections.sort(added, ORDER_COMPARATOR);
        if (this.mUpdateLocked) {
            Log.d(TAG, "ignore update db because of updateLocked");
        } else {
            updateDirtyItems();
        }
        AppsAdapter.debugItemInfo("addItemView", (ArrayList) added);
        addItemView(added, true);
    }

    protected void updateFolderContents(LongArrayMap<ArrayList<ItemInfo>> folderItems) {
        int count = folderItems.size();
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
                AppsAdapter.debugItemInfo("updateFolderContents target : ", this.mAppsModel.getItemById(key));
                AppsAdapter.debugItemInfo("update folder item : ", (ArrayList) updated);
                AppsAdapter.debugItemInfo("remove folder item : ", (ArrayList) deleted);
            }
        }
    }

    public ViewType getViewType() {
        return ViewType.CUSTOM_GRID;
    }

    public Normalizer<Object> getNormalizer() {
        if (!isTidyUpState()) {
            return this.mNormalizer;
        }
        if (this.mTidyupNormalizer == null) {
            this.mTidyupNormalizer = new TidyUpNormalizer();
        }
        return this.mTidyupNormalizer;
    }

    private boolean isTidyUpState() {
        return this.mState == 4;
    }

    public void setStateAndUpdateLock(int state, boolean updateLock) {
        this.mState = state;
        this.mUpdateLocked = updateLock;
    }

    public void createFolderAndAddItem(FolderInfo folderInfo, ItemInfo targetItem, ArrayList<IconInfo> infos) {
        AppsAdapter.debugItemInfo("createFolderAndAddItem", (ItemInfo) folderInfo);
        folderInfo.container = -102;
        final long oldParentId = targetItem.container;
        final boolean isInApps = oldParentId == -102;
        folderInfo.screenId = isInApps ? targetItem.screenId : -1;
        folderInfo.rank = isInApps ? targetItem.rank : -1;
        folderInfo.cellX = isInApps ? targetItem.cellX : -1;
        folderInfo.cellY = isInApps ? targetItem.cellY : -1;
        final ContentValues values = new ContentValues();
        values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
        values.put("screen", Long.valueOf(folderInfo.screenId));
        values.put(BaseLauncherColumns.RANK, Integer.valueOf(folderInfo.rank));
        values.put("cellX", Integer.valueOf(folderInfo.cellX));
        values.put("cellY", Integer.valueOf(folderInfo.cellY));
        this.mAppsModel.addItem(folderInfo);
        ArrayList<ContentValues> valueList = new ArrayList();
        final ArrayList<ItemInfo> itemInfos = new ArrayList();
        Iterator it = infos.iterator();
        while (it.hasNext()) {
            IconInfo item = (IconInfo) it.next();
            item.container = folderInfo.id;
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
                if (AppsCustomAdapter.this.mDataHolder.mDestroyed || AppsCustomAdapter.this.mAppsModel.isUpdateLocked() || AppsCustomAdapter.this.mItems.isEmpty()) {
                    Log.d(AppsCustomAdapter.TAG, "createFolderAndAddItem ignore : " + AppsCustomAdapter.this.mAppsModel.isUpdateLocked() + " , " + AppsCustomAdapter.this.mItems.isEmpty());
                } else if (AppsCustomAdapter.this.mItems.contains(folderInfo2)) {
                    Iterator it = arrayList.iterator();
                    while (it.hasNext()) {
                        IconInfo item = (IconInfo) it.next();
                        if (AppsCustomAdapter.this.mItems.contains(item)) {
                            AppsCustomAdapter.this.mItems.remove(item);
                            AppsCustomAdapter.this.removeViewAndReorder(item);
                        }
                    }
                    Log.d(AppsCustomAdapter.TAG, "already folder created " + folderInfo2);
                } else {
                    if (isInApps && folderInfo2.rank != itemInfo.rank) {
                        Log.w(AppsCustomAdapter.TAG, "createFolderAndAddItem : targetItem position is changed folderInfo = " + folderInfo2 + " targetItem = " + itemInfo);
                        folderInfo2.screenId = itemInfo.screenId;
                        folderInfo2.rank = itemInfo.rank;
                        folderInfo2.cellX = itemInfo.cellX;
                        folderInfo2.cellY = itemInfo.cellY;
                        folderInfo2.mDirty = true;
                    }
                    AppsCustomAdapter.this.mItems.removeAll(itemInfos);
                    AppsCustomAdapter.this.mItems.add(folderInfo2);
                    if (!isInApps) {
                        AppsCustomAdapter.this.getNormalizer().normalize(AppsCustomAdapter.this.mItems, AppsCustomAdapter.this.mListener.getMaxItemsPerScreen(), AppsCustomAdapter.this.mListener.getCellCountX());
                        AppsCustomAdapter.this.updateItem(values, folderInfo2);
                        View v = AppsCustomAdapter.this.mListener.getAppsIconByItemId(oldParentId);
                        if (v != null) {
                            ((FolderInfo) v.getTag()).remove((IconInfo) itemInfo);
                        }
                    }
                    if (folderInfo2.screenId == -1 || folderInfo2.rank == -1) {
                        AppsCustomAdapter.this.getNormalizer().normalize(AppsCustomAdapter.this.mItems, AppsCustomAdapter.this.mListener.getMaxItemsPerScreen(), AppsCustomAdapter.this.mListener.getCellCountX());
                    }
                    ArrayList<ItemInfo> itemInfos = new ArrayList();
                    itemInfos.addAll(arrayList);
                    if (AppsCustomAdapter.this.mListener.getAppsIconByItemId(itemInfo.id) == null) {
                        AppsCustomAdapter.this.mListener.removeEmptyCellsAndViews(itemInfos);
                        AppsCustomAdapter.this.mListener.makeEmptyCellAndReorder((int) folderInfo2.screenId, folderInfo2.rank);
                        AppsCustomAdapter.this.addItemView(folderInfo2);
                    } else {
                        AppsCustomAdapter.this.addItemView(folderInfo2);
                        AppsCustomAdapter.this.mListener.removeEmptyCellsAndViews(itemInfos);
                    }
                    if (!AppsCustomAdapter.this.mUpdateLocked) {
                        AppsCustomAdapter.this.updateDirtyItems();
                    }
                    AppsAdapter.debugItemInfo("createfolder for postposition", folderInfo2);
                    AppsAdapter.debugItemInfo("addItem to folder for postposition", arrayList);
                    Log.d(AppsCustomAdapter.TAG, "postposition-createFolder : " + AppsCustomAdapter.this.mAppsModel.isUpdateLocked() + " , " + AppsCustomAdapter.this.mDataHolder.mFirstLoaded + " , " + AppsCustomAdapter.this.mDataHolder.mModelPrepared + " , " + AppsCustomAdapter.this.mItems.size());
                }
            }
        });
    }

    public String getTag() {
        return TAG;
    }

    protected void removeViewAndReorder(IconInfo item) {
        ArrayList<ItemInfo> items = new ArrayList();
        items.add(item);
        this.mListener.removeEmptyCellsAndViews(items);
    }
}
