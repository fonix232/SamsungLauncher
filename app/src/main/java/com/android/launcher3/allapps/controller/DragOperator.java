package com.android.launcher3.allapps.controller;

import android.view.View;
import com.android.launcher3.allapps.DragAppIcon;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.drag.DragObject;
import java.util.ArrayList;
import java.util.Iterator;

abstract class DragOperator {
    boolean mRestorePositionOnDrop = false;

    abstract void animateViewIntoPosition(DragObject dragObject, ItemInfo itemInfo, View view, int i, Runnable runnable, View view2, boolean z, boolean z2);

    abstract void dragOver(DragObject dragObject);

    abstract void dropCompletedWithOutExtra(boolean z);

    abstract void dropExtraObjects(DragObject dragObject, int i, int i2, ArrayList<DragObject> arrayList, boolean z);

    abstract void refreshObjectsToPosition(DragObject dragObject, int i, int i2, ArrayList<DragObject> arrayList);

    abstract void updateItemPosition(ItemInfo itemInfo, long j, int i);

    DragOperator() {
    }

    void dragStart() {
        setRestorePosition(true);
    }

    void dropCreateFolder(ItemInfo dragItem, boolean internal) {
    }

    void dropAddToExistingFolder() {
    }

    void setRestorePosition(boolean restorePosition) {
        this.mRestorePositionOnDrop = restorePosition;
    }

    void addItemToTarget(View cell, DragAppIcon target) {
    }

    boolean getRestorePosition() {
        return this.mRestorePositionOnDrop;
    }

    void makeEmptyCellAndReorderIfNecessary(int screenId, int rank) {
    }

    void removeEmptyCellIfNecessary(DragAppIcon empty) {
    }

    void updateDirtyItemsToDb() {
    }

    boolean acceptDrop(DragObject d) {
        if (!this.mRestorePositionOnDrop) {
            return true;
        }
        d.cancelled = true;
        if (d.extraDragInfoList != null) {
            Iterator it = d.extraDragInfoList.iterator();
            while (it.hasNext()) {
                ((DragObject) it.next()).cancelled = true;
            }
        }
        return false;
    }

    void onAdjustDraggedObjectPosition(DragObject dragObject, int startPos, int endPos, int screenId) {
    }

    void dropCompleted() {
        setRestorePosition(false);
    }

    void dropInternal(DragObject d, ItemInfo item, View dragView) {
    }

    void dropExternal(DragObject d, ItemInfo item, View view, Runnable exitDragStateRunnable) {
    }
}
