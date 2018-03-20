package com.android.launcher3.common.base.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.BaseRecyclerViewFastScrollBar;

public abstract class BaseRecyclerView extends RecyclerView implements OnItemTouchListener {
    private static final int SCROLL_DELTA_THRESHOLD_DP = 4;
    protected Rect mBackgroundPadding;
    private float mDeltaThreshold;
    private int mDownX;
    private int mDownY;
    private int mDy;
    private int mLastY;
    protected BaseRecyclerViewFastScrollBar mScrollbar;

    public static class ScrollPositionState {
        public int rowHeight;
        public int rowIndex;
        public int rowTopOffset;
    }

    private class ScrollListener extends OnScrollListener {
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            BaseRecyclerView.this.mDy = dy;
            BaseRecyclerView.this.onUpdateScrollbar(dy);
        }
    }

    protected abstract void getCurScrollState(ScrollPositionState scrollPositionState);

    public abstract void onUpdateScrollbar(int i);

    public abstract String scrollToPositionAtProgress(float f);

    public BaseRecyclerView(Context context) {
        this(context, null);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mDy = 0;
        this.mBackgroundPadding = new Rect();
        this.mDeltaThreshold = getResources().getDisplayMetrics().density * 4.0f;
        this.mScrollbar = new BaseRecyclerViewFastScrollBar(this, getResources());
        setOnScrollListener(new ScrollListener());
    }

    public void reset() {
        this.mScrollbar.reattachThumbToScroll();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
        handleTouchEvent(ev);
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case 0:
                this.mDownX = x;
                this.mLastY = y;
                this.mDownY = y;
                if (shouldStopScroll(ev)) {
                    stopScroll();
                }
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
            case 1:
            case 3:
                onFastScrollCompleted();
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
            case 2:
                this.mLastY = y;
                this.mScrollbar.handleTouchEvent(ev, this.mDownX, this.mDownY, this.mLastY);
                break;
        }
        return this.mScrollbar.isDraggingThumb();
    }

    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    protected boolean shouldStopScroll(MotionEvent ev) {
        if (ev.getAction() != 0 || ((float) Math.abs(this.mDy)) >= this.mDeltaThreshold || getScrollState() == 0) {
            return false;
        }
        return true;
    }

    public void updateBackgroundPadding(Rect padding) {
        this.mBackgroundPadding.set(padding);
    }

    public Rect getBackgroundPadding() {
        return this.mBackgroundPadding;
    }

    public int getMaxScrollbarWidth() {
        return this.mScrollbar.getThumbMaxWidth();
    }

    protected int getAvailableScrollHeight(int rowCount, int rowHeight) {
        return ((getPaddingTop() + (rowCount * rowHeight)) + getPaddingBottom()) - ((getHeight() - this.mBackgroundPadding.top) - this.mBackgroundPadding.bottom);
    }

    protected int getAvailableScrollBarHeight() {
        return ((getHeight() - this.mBackgroundPadding.top) - this.mBackgroundPadding.bottom) - this.mScrollbar.getThumbHeight();
    }

    public int getFastScrollerTrackColor(int defaultTrackColor) {
        return defaultTrackColor;
    }

    public int getFastScrollerThumbInactiveColor(int defaultInactiveThumbColor) {
        return defaultInactiveThumbColor;
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        onUpdateScrollbar(0);
        this.mScrollbar.draw(canvas);
    }

    protected void synchronizeScrollBarThumbOffsetToViewScroll(ScrollPositionState scrollPosState, int rowCount) {
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight(rowCount, scrollPosState.rowHeight);
        if (availableScrollHeight <= 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int scrollBarX;
        int scrollBarY = this.mBackgroundPadding.top + ((int) ((((float) ((getPaddingTop() + (scrollPosState.rowIndex * scrollPosState.rowHeight)) - scrollPosState.rowTopOffset)) / ((float) availableScrollHeight)) * ((float) availableScrollBarHeight)));
        if (Utilities.sIsRtl) {
            scrollBarX = this.mBackgroundPadding.left;
        } else {
            scrollBarX = (getWidth() - this.mBackgroundPadding.right) - this.mScrollbar.getThumbWidth();
        }
        this.mScrollbar.setThumbOffset(scrollBarX, scrollBarY);
    }

    public void onFastScrollCompleted() {
    }
}
