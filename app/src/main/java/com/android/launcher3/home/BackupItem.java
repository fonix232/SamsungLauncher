package com.android.launcher3.home;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;

class BackupItem {
    protected int mCellX = -1;
    protected int mCellY = -1;
    protected ItemInfo mItem = null;
    protected long mScreen = -1;
    protected int mSpanX = 0;
    protected int mSpanY = 0;
    protected View mView = null;

    BackupItem() {
    }

    void setItem(ItemInfo item) {
        if (item != null) {
            ItemInfo homeItem = item;
            this.mCellX = homeItem.cellX;
            this.mCellY = homeItem.cellY;
            this.mSpanX = homeItem.spanX;
            this.mSpanY = homeItem.spanY;
            this.mScreen = item.screenId;
            this.mItem = item;
        }
    }

    void setView(View view) {
        if (view != null) {
            this.mView = view;
        }
    }

    int getCellX() {
        return this.mCellX;
    }

    int getCellY() {
        return this.mCellY;
    }

    int getSpanX() {
        return this.mSpanX;
    }

    int getSpanY() {
        return this.mSpanY;
    }

    long getScreen() {
        return this.mScreen;
    }

    ItemInfo getItem() {
        return this.mItem;
    }

    View getView() {
        return this.mView;
    }
}
