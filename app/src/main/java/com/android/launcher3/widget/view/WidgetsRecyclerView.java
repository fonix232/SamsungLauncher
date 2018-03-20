package com.android.launcher3.widget.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.common.base.view.BaseRecyclerView;
import com.android.launcher3.common.base.view.BaseRecyclerView.ScrollPositionState;
import com.android.launcher3.common.model.PackageItemInfo;
import com.android.launcher3.common.model.WidgetsModel;

public class WidgetsRecyclerView extends BaseRecyclerView {
    private static final String TAG = "WidgetsRecyclerView";
    private ScrollPositionState mScrollPosState;
    private WidgetsModel mWidgets;

    public WidgetsRecyclerView(Context context) {
        this(context, null);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mScrollPosState = new ScrollPositionState();
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    public int getFastScrollerTrackColor(int defaultTrackColor) {
        return -1;
    }

    public void setWidgets(WidgetsModel widgets) {
        this.mWidgets = widgets;
    }

    protected void dispatchDraw(Canvas canvas) {
        canvas.clipRect(this.mBackgroundPadding.left, this.mBackgroundPadding.top, getWidth() - this.mBackgroundPadding.right, getHeight() - this.mBackgroundPadding.bottom);
        super.dispatchDraw(canvas);
    }

    public String scrollToPositionAtProgress(float touchFraction) {
        if (this.mWidgets == null) {
            return "";
        }
        int rowCount = this.mWidgets.getPackageSize();
        if (rowCount == 0) {
            return "";
        }
        stopScroll();
        getCurScrollState(this.mScrollPosState);
        float pos = ((float) rowCount) * touchFraction;
        ((LinearLayoutManager) getLayoutManager()).scrollToPositionWithOffset(0, (int) (-(((float) getAvailableScrollHeight(rowCount, this.mScrollPosState.rowHeight)) * touchFraction)));
        if (touchFraction == 1.0f) {
            pos -= 1.0f;
        }
        PackageItemInfo p = this.mWidgets.getPackageItemInfo((int) pos);
        if (p != null) {
            return p.titleSectionName;
        }
        return "";
    }

    public void onUpdateScrollbar(int dy) {
        if (this.mWidgets != null) {
            int rowCount = this.mWidgets.getPackageSize();
            if (rowCount == 0) {
                this.mScrollbar.setThumbOffset(-1, -1);
                return;
            }
            getCurScrollState(this.mScrollPosState);
            if (this.mScrollPosState.rowIndex < 0) {
                this.mScrollbar.setThumbOffset(-1, -1);
            } else {
                synchronizeScrollBarThumbOffsetToViewScroll(this.mScrollPosState, rowCount);
            }
        }
    }

    protected void getCurScrollState(ScrollPositionState stateOut) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.rowHeight = -1;
        if (this.mWidgets != null && this.mWidgets.getPackageSize() != 0) {
            View child = getChildAt(0);
            stateOut.rowIndex = getChildPosition(child);
            stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
            stateOut.rowHeight = child.getHeight();
        }
    }
}
