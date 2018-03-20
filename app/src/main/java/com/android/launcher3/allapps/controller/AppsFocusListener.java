package com.android.launcher3.allapps.controller;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.util.focus.FocusListener;

public class AppsFocusListener extends FocusListener {
    String TAG = "AppsFocusListener";

    public boolean onKeyPressUp(View v, int keyCode, KeyEvent event) {
        if (v == null || !(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        int currentFocusIdx = ((ItemInfo) v.getTag()).rank;
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        if (!(cl.getParent() instanceof PagedView)) {
            return false;
        }
        int countX = cl.getCountX();
        if (countX == 0) {
            return false;
        }
        int nextFocusIdx = currentFocusIdx - countX;
        if (nextFocusIdx < 0) {
            return false;
        }
        View nextView = cl.getChildAt(nextFocusIdx % countX, nextFocusIdx / countX);
        if (nextView == null) {
            return true;
        }
        nextView.requestFocus();
        return true;
    }

    public boolean onKeyPressDown(View v, int keyCode, KeyEvent event) {
        if (v == null) {
            return false;
        }
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        int currentFocusIdx = ((ItemInfo) v.getTag()).rank;
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayoutChildren clc = (CellLayoutChildren) v.getParent();
        CellLayout cl = (CellLayout) clc.getParent();
        if (!(cl.getParent() instanceof PagedView)) {
            return false;
        }
        int countX = cl.getCountX();
        if (countX == 0) {
            return false;
        }
        int nextFocusIdx;
        int currentPageItemCount = clc.getChildCount();
        if (currentFocusIdx < (((currentPageItemCount / countX) + (currentPageItemCount % countX > 0 ? 1 : 0)) - 1) * countX) {
            nextFocusIdx = Math.min(currentPageItemCount - 1, currentFocusIdx + countX);
        } else {
            nextFocusIdx = currentFocusIdx;
        }
        View nextView = cl.getChildAt(nextFocusIdx % countX, nextFocusIdx / countX);
        if (nextView == null) {
            return true;
        }
        nextView.requestFocus();
        return true;
    }

    public boolean onKeyPressLeft(View v, int keyCode, KeyEvent event) {
        if (!(v.getTag() instanceof ItemInfo)) {
            return false;
        }
        ItemInfo ii = (ItemInfo) v.getTag();
        int currentFocusIdx = ii.rank;
        int currentPageIdx = (int) ii.screenId;
        if (!(v.getParent() instanceof CellLayoutChildren)) {
            return false;
        }
        CellLayout cl = (CellLayout) ((CellLayoutChildren) v.getParent()).getParent();
        if (!(cl.getParent() instanceof PagedView)) {
            return false;
        }
        PagedView pv = (PagedView) cl.getParent();
        int countX = cl.getCountX();
        if (countX == 0) {
            return false;
        }
        int nextFocusIdx = 0;
        int nextPageIdx = currentPageIdx;
        int pageIdx = currentPageIdx;
        while (pageIdx >= 0) {
            nextFocusIdx = currentFocusIdx - 1;
            if (nextFocusIdx >= 0) {
                break;
            } else if (pageIdx == 0) {
                return true;
            } else {
                currentFocusIdx = ((CellLayout) pv.getChildAt(pageIdx - 1)).getCellLayoutChildren().getChildCount();
                nextPageIdx = pageIdx - 1;
                pageIdx--;
            }
        }
        if (pageIdx < 0) {
            return true;
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
        ItemInfo ii = (ItemInfo) v.getTag();
        int currentFocusIdx = ii.rank;
        int currentPageIdx = (int) ii.screenId;
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
        if (countX == 0) {
            return false;
        }
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
            return true;
        }
        View nextView = ((CellLayout) pv.getChildAt(nextPageIdx)).getChildAt(nextFocusIdx % countX, nextFocusIdx / countX);
        if (nextView == null) {
            return true;
        }
        nextView.requestFocus();
        return true;
    }

    public void onFocusIn(View v) {
        Log.i(this.TAG, "onFocusIn: ");
    }

    public void onFocusOut(View v) {
        Log.i(this.TAG, "onFocusOut: ");
    }
}
