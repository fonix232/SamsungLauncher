package com.android.launcher3.folder.controller;

import android.view.KeyEvent;
import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.focus.FocusListener;

public class FolderFocusListener extends FocusListener {
    private static final String TAG = "FolderFocusListener";

    public boolean onKeyDown(View v, int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(v, keyCode, event);
        if (!(v instanceof IconView)) {
            return handled;
        }
        if (keyCode == 21 || keyCode == 22) {
            return true;
        }
        return handled;
    }

    public boolean onKeyPressUp(View v, int keyCode, KeyEvent event) {
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        ItemInfo item = (ItemInfo) v.getTag();
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        int countX = cl.getCountX();
        int countY = cl.getCountY();
        if (countX == 0 || countY == 0) {
            return false;
        }
        int currentFocusIdx = item.rank % (countX * countY);
        int currentX = currentFocusIdx % countX;
        int currentY = currentFocusIdx / countX;
        if (currentY <= 0) {
            return false;
        }
        View nextView = cl.getChildAt(currentX, currentY - 1);
        if (nextView == null) {
            return false;
        }
        nextView.requestFocus();
        return true;
    }

    public boolean onKeyPressDown(View v, int keyCode, KeyEvent event) {
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        ItemInfo item = (ItemInfo) v.getTag();
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        int countX = cl.getCountX();
        int countY = cl.getCountY();
        if (countX == 0 || countY == 0) {
            return false;
        }
        int currentFocusIdx = item.rank % (countX * countY);
        int currentX = currentFocusIdx % countX;
        int currentY = currentFocusIdx / countX;
        if (currentY >= countY - 1) {
            return false;
        }
        View nextView = cl.getChildAt(currentX, currentY + 1);
        if (nextView == null) {
            return false;
        }
        nextView.requestFocus();
        return true;
    }

    public boolean onKeyPressLeft(View v, int keyCode, KeyEvent event) {
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        ItemInfo item = (ItemInfo) v.getTag();
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        if (!(cl.getParent() instanceof PagedView)) {
            return false;
        }
        PagedView pv = (PagedView) cl.getParent();
        int countX = cl.getCountX();
        int countY = cl.getCountY();
        if (countX == 0 || countY == 0) {
            return false;
        }
        int maxItemsPerPage = countX * countY;
        int currentFocusIdx = item.rank % maxItemsPerPage;
        int currentPageIdx = item.rank / maxItemsPerPage;
        int nextFocusIdx = 0;
        int nextPageIdx = currentPageIdx;
        int pageIdx = currentPageIdx;
        while (pageIdx >= 0) {
            nextFocusIdx = currentFocusIdx - 1;
            if (nextFocusIdx >= 0) {
                break;
            } else if (pageIdx == 0) {
                return false;
            } else {
                currentFocusIdx = ((CellLayout) pv.getChildAt(pageIdx - 1)).getCellLayoutChildren().getChildCount();
                nextPageIdx = pageIdx - 1;
                pageIdx--;
            }
        }
        if (nextPageIdx < 0) {
            return false;
        }
        View nextView = ((CellLayout) pv.getChildAt(nextPageIdx)).getChildAt(nextFocusIdx % countX, nextFocusIdx / countX);
        if (nextView == null) {
            return true;
        }
        nextView.requestFocus();
        return true;
    }

    public boolean onKeyPressRight(View v, int keyCode, KeyEvent event) {
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        ItemInfo item = (ItemInfo) v.getTag();
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        if (!(cl.getParent() instanceof PagedView)) {
            return false;
        }
        PagedView pv = (PagedView) cl.getParent();
        int pageCount = pv.getPageCount();
        int countX = cl.getCountX();
        int countY = cl.getCountY();
        if (countX == 0 || countY == 0) {
            return false;
        }
        int maxItemsPerPage = countX * countY;
        int currentFocusIdx = item.rank % maxItemsPerPage;
        int currentPageIdx = item.rank / maxItemsPerPage;
        int nextFocusIdx = 0;
        int nextPageIdx = currentPageIdx;
        while (nextPageIdx < pageCount) {
            nextFocusIdx = currentFocusIdx + 1;
            if (nextFocusIdx < ((CellLayout) pv.getChildAt(currentPageIdx)).getCellLayoutChildren().getChildCount()) {
                break;
            }
            currentFocusIdx = -1;
            nextPageIdx++;
        }
        if (nextPageIdx >= pageCount) {
            return false;
        }
        View nextView = ((CellLayout) pv.getChildAt(nextPageIdx)).getChildAt(nextFocusIdx % countX, nextFocusIdx / countX);
        if (nextView == null) {
            return true;
        }
        nextView.requestFocus();
        return true;
    }

    public void onFocusIn(View v) {
    }

    public void onFocusOut(View v) {
    }
}
