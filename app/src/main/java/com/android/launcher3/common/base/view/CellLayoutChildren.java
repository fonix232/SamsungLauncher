package com.android.launcher3.common.base.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.android.launcher3.home.LauncherAppWidgetHostView.ResizeResult;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import java.util.ArrayList;

public class CellLayoutChildren extends ViewGroup {
    private int mCellHeight;
    private int mCellWidth;
    private int mCountX;
    private int mHeightGap;
    private final int[] mTmpCellXY = new int[2];
    private int mWidthGap;

    public CellLayoutChildren(Context context) {
        super(context);
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int widthGap, int heightGap, int countX) {
        this.mCellWidth = cellWidth;
        this.mCellHeight = cellHeight;
        this.mWidthGap = widthGap;
        this.mHeightGap = heightGap;
        this.mCountX = countX;
    }

    public View getChildAt(ItemInfo item) {
        if (item == null) {
            return null;
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            Object tag = v.getTag();
            if ((tag instanceof ItemInfo) && ((ItemInfo) tag).id == item.id) {
                return v;
            }
        }
        return null;
    }

    public View getChildAt(int x, int y) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.cellX <= x && x < lp.cellX + lp.cellHSpan && lp.cellY <= y && y < lp.cellY + lp.cellVSpan) {
                return child;
            }
        }
        return null;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                measureChild(child);
            }
        }
    }

    public void setupLp(LayoutParams lp) {
        lp.setup(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
    }

    public void measureChild(View child) {
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (!LauncherAppWidgetHostView.PRV_HOSTVIEW.equals(child.getTag())) {
            lp.setup(cellWidth, cellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
        }
        CellLayout cl = (CellLayout) getParent();
        int childWidth = lp.width;
        int childHeight = lp.height;
        if (child instanceof LauncherAppWidgetHostView) {
            LauncherAppWidgetInfo awi = (LauncherAppWidgetInfo) child.getTag();
            if (cl != null) {
                int spanX = lp.useTmpCoords ? lp.cellHSpan : awi.spanX;
                int spanY = lp.useTmpCoords ? lp.cellVSpan : awi.spanY;
                int targetWidth = (spanX * this.mCellWidth) + (spanX >= 2 ? this.mWidthGap * (spanX - 1) : 0);
                int targetHeight = (spanY * this.mCellHeight) + (spanY >= 2 ? this.mHeightGap * (spanY - 1) : 0);
                lp.cellHSpan = spanX;
                lp.cellVSpan = spanY;
                ResizeResult result = LauncherAppWidgetHostView.calculateWidgetSize(spanX, spanY, targetWidth, targetHeight);
                ((LauncherAppWidgetHostView) child).setResizeScaleResult(result);
                childWidth = result.width;
                childHeight = result.height;
            }
            lp.width = childWidth;
            lp.height = childHeight;
        }
        child.measure(MeasureSpec.makeMeasureSpec(lp.width, 1073741824), MeasureSpec.makeMeasureSpec(lp.height, 1073741824));
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, lp.width + childLeft, lp.height + childTop);
                buildCustomAnimationSet(child, childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                if (lp.dropped) {
                    lp.dropped = false;
                    getLocationOnScreen(this.mTmpCellXY);
                }
            }
        }
    }

    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            if (!view.isHardwareAccelerated() && enabled) {
                view.buildDrawingCache(true);
            }
        }
    }

    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public ArrayList<ItemInfo> getChildrenAllItems() {
        ArrayList<ItemInfo> items = new ArrayList();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null && (child.getTag() instanceof ItemInfo)) {
                ItemInfo item = (ItemInfo) child.getTag();
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    public ArrayList<View> getChildren() {
        ArrayList<View> views = new ArrayList();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null) {
                views.add(child);
            }
        }
        return views;
    }

    protected void buildCustomAnimationSet(View childView, int left, int top, int right, int bottom) {
    }

    public void callRefreshLiveIcon() {
    }
}
