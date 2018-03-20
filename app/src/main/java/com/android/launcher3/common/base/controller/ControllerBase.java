package com.android.launcher3.common.base.controller;

import android.view.View;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.folder.FolderInfo;
import java.util.ArrayList;

public interface ControllerBase {
    long addItemToDb(ItemInfo itemInfo, long j, long j2, int i, int i2);

    void addItemToDb(ItemInfo itemInfo, long j, long j2, int i);

    void addOrMoveItemInDb(ItemInfo itemInfo, long j, long j2, int i, int i2, int i3);

    void addOrMoveItems(ArrayList<IconInfo> arrayList, long j, long j2);

    void deleteFolder(FolderInfo folderInfo);

    void deleteItemFromDb(ItemInfo itemInfo);

    ItemInfo getLocationInfoFromDB(ItemInfo itemInfo);

    int getPageIndexForDragView(ItemInfo itemInfo);

    void modifyItemsInDb(ArrayList<ItemInfo> arrayList, long j, int i);

    void moveItemFromFolder(IconInfo iconInfo);

    void notifyCapture(boolean z);

    void notifyControllerItemsChanged();

    void onUpdateAlphabetList(ItemInfo itemInfo);

    boolean recoverCancelItemForFolderLock(IconInfo iconInfo, long j, long j2, int i, int i2, int i3);

    void replaceFolderWithFinalItem(ItemInfo itemInfo, int i, View view);

    void updateItemInDb(ItemInfo itemInfo);
}
