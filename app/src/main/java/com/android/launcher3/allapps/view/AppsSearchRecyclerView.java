package com.android.launcher3.allapps.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView.RecycledViewPool;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.Stats;
import com.android.launcher3.Stats.LaunchSourceProvider;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.allapps.AlphabeticalAppsList.FastScrollSectionInfo;
import com.android.launcher3.common.base.view.BaseRecyclerView;
import com.android.launcher3.common.base.view.BaseRecyclerView.ScrollPositionState;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import java.util.List;

public class AppsSearchRecyclerView extends BaseRecyclerView implements LaunchSourceProvider {
    private static final int FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_ROW = 0;
    private static final int FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_SECTIONS = 1;
    private static final int FAST_SCROLL_MODE_FREE_SCROLL = 1;
    private static final int FAST_SCROLL_MODE_JUMP_TO_FIRST_ICON = 0;
    private AlphabeticalAppsList mApps;
    private int mFastScrollFrameIndex;
    private final int[] mFastScrollFrames;
    private int mNumAppsPerRow;
    private int mPrevFastScrollFocusedPosition;
    private final int mScrollBarMode;
    private ScrollPositionState mScrollPosState;
    private Runnable mSmoothSnapNextFrameRunnable;

    public AppsSearchRecyclerView(Context context) {
        this(context, null);
    }

    public AppsSearchRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppsSearchRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        this.mFastScrollFrames = new int[10];
        this.mScrollBarMode = 0;
        this.mScrollPosState = new ScrollPositionState();
        this.mSmoothSnapNextFrameRunnable = new Runnable() {
            public void run() {
                if (AppsSearchRecyclerView.this.mFastScrollFrameIndex < AppsSearchRecyclerView.this.mFastScrollFrames.length) {
                    AppsSearchRecyclerView.this.scrollBy(0, AppsSearchRecyclerView.this.mFastScrollFrames[AppsSearchRecyclerView.this.mFastScrollFrameIndex]);
                    AppsSearchRecyclerView.this.mFastScrollFrameIndex = AppsSearchRecyclerView.this.mFastScrollFrameIndex + 1;
                    AppsSearchRecyclerView.this.postOnAnimation(AppsSearchRecyclerView.this.mSmoothSnapNextFrameRunnable);
                }
            }
        };
        this.mScrollbar.setDetachThumbOnFastScroll();
    }

    public void setApps(AlphabeticalAppsList apps) {
        this.mApps = apps;
    }

    public void setNumAppsPerRow(DeviceProfile grid, int numAppsPerRow) {
        this.mNumAppsPerRow = numAppsPerRow;
        RecycledViewPool pool = getRecycledViewPool();
        int approxRows = (int) Math.ceil((double) (grid.availableHeightPx / grid.appsGrid.getIconSize()));
        pool.setMaxRecycledViews(3, 1);
        pool.setMaxRecycledViews(6, 1);
        pool.setMaxRecycledViews(4, 1);
        pool.setMaxRecycledViews(5, 1);
        pool.setMaxRecycledViews(1, this.mNumAppsPerRow * approxRows);
        pool.setMaxRecycledViews(2, this.mNumAppsPerRow);
        pool.setMaxRecycledViews(0, approxRows);
    }

    public void scrollToTop() {
        if (this.mScrollbar.isThumbDetached()) {
            this.mScrollbar.reattachThumbToScroll();
        }
        scrollToPosition(0);
    }

    protected void dispatchDraw(Canvas canvas) {
        canvas.clipRect(this.mBackgroundPadding.left, this.mBackgroundPadding.top, getWidth() - this.mBackgroundPadding.right, getHeight() - this.mBackgroundPadding.bottom);
        super.dispatchDraw(canvas);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateEmptySearchBackgroundBounds();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    public void fillInLaunchSourceData(Bundle sourceData) {
        sourceData.putString("container", Stats.CONTAINER_ALL_APPS);
        if (this.mApps.hasFilter()) {
            sourceData.putString(Stats.SOURCE_EXTRA_SUB_CONTAINER, Stats.SUB_CONTAINER_ALL_APPS_SEARCH);
        } else {
            sourceData.putString(Stats.SOURCE_EXTRA_SUB_CONTAINER, Stats.SUB_CONTAINER_ALL_APPS_A_Z);
        }
    }

    public void onSearchResultsChanged() {
        scrollToTop();
    }

    public String scrollToPositionAtProgress(float touchFraction) {
        int rowCount = this.mApps.getNumAppRows();
        if (rowCount == 0) {
            return "";
        }
        stopScroll();
        List<FastScrollSectionInfo> fastScrollSections = this.mApps.getFastScrollerSections();
        FastScrollSectionInfo lastInfo = (FastScrollSectionInfo) fastScrollSections.get(0);
        switch (null) {
            case null:
                for (int i = 1; i < fastScrollSections.size(); i++) {
                    FastScrollSectionInfo info = (FastScrollSectionInfo) fastScrollSections.get(i);
                    if (info.touchFraction > touchFraction) {
                        break;
                    }
                    lastInfo = info;
                }
                break;
            case 1:
                lastInfo = (FastScrollSectionInfo) fastScrollSections.get((int) (((float) (fastScrollSections.size() - 1)) * touchFraction));
                break;
            default:
                throw new RuntimeException("Unexpected scroll bar mode");
        }
        getCurScrollState(this.mScrollPosState);
        int availableScrollHeight = getAvailableScrollHeight(rowCount, this.mScrollPosState.rowHeight);
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        if (this.mPrevFastScrollFocusedPosition != lastInfo.fastScrollToItem.position) {
            this.mPrevFastScrollFocusedPosition = lastInfo.fastScrollToItem.position;
        }
        return lastInfo.sectionName;
    }

    public void onFastScrollCompleted() {
        super.onFastScrollCompleted();
        this.mPrevFastScrollFocusedPosition = -1;
    }

    public void onUpdateScrollbar(int dy) {
        if (this.mApps.getAdapterItems().isEmpty() || this.mNumAppsPerRow == 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int rowCount = this.mApps.getNumAppRows();
        getCurScrollState(this.mScrollPosState);
        if (this.mScrollPosState.rowIndex < 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight(this.mApps.getNumAppRows(), this.mScrollPosState.rowHeight);
        if (availableScrollHeight <= 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int scrollBarY = this.mBackgroundPadding.top + ((int) ((((float) ((getPaddingTop() + (this.mScrollPosState.rowIndex * this.mScrollPosState.rowHeight)) - this.mScrollPosState.rowTopOffset)) / ((float) availableScrollHeight)) * ((float) availableScrollBarHeight)));
        if (this.mScrollbar.isThumbDetached()) {
            int scrollBarX;
            if (Utilities.sIsRtl) {
                scrollBarX = this.mBackgroundPadding.left;
            } else {
                scrollBarX = (getWidth() - this.mBackgroundPadding.right) - this.mScrollbar.getThumbWidth();
            }
            if (this.mScrollbar.isDraggingThumb()) {
                this.mScrollbar.setThumbOffset(scrollBarX, (int) this.mScrollbar.getLastTouchY());
                return;
            }
            int thumbScrollY = this.mScrollbar.getThumbOffset().y;
            int diffScrollY = scrollBarY - thumbScrollY;
            if (((float) (diffScrollY * dy)) > 0.0f) {
                if (dy < 0) {
                    thumbScrollY += Math.max((int) (((float) (dy * thumbScrollY)) / ((float) scrollBarY)), diffScrollY);
                } else {
                    thumbScrollY += Math.min((int) (((float) ((availableScrollBarHeight - thumbScrollY) * dy)) / ((float) (availableScrollBarHeight - scrollBarY))), diffScrollY);
                }
                thumbScrollY = Math.max(0, Math.min(availableScrollBarHeight, thumbScrollY));
                this.mScrollbar.setThumbOffset(scrollBarX, thumbScrollY);
                if (scrollBarY == thumbScrollY) {
                    this.mScrollbar.reattachThumbToScroll();
                    return;
                }
                return;
            }
            this.mScrollbar.setThumbOffset(scrollBarX, thumbScrollY);
            return;
        }
        synchronizeScrollBarThumbOffsetToViewScroll(this.mScrollPosState, rowCount);
    }

    private void smoothSnapToPosition(int position, ScrollPositionState scrollPosState) {
        removeCallbacks(this.mSmoothSnapNextFrameRunnable);
        int curScrollY = (getPaddingTop() + (scrollPosState.rowIndex * scrollPosState.rowHeight)) - scrollPosState.rowTopOffset;
        int newScrollY = getScrollAtPosition(position, scrollPosState.rowHeight);
        int numFrames = this.mFastScrollFrames.length;
        for (int i = 0; i < numFrames; i++) {
            this.mFastScrollFrames[i] = (newScrollY - curScrollY) / numFrames;
        }
        this.mFastScrollFrameIndex = 0;
        postOnAnimation(this.mSmoothSnapNextFrameRunnable);
    }

    protected void getCurScrollState(ScrollPositionState stateOut) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.rowHeight = -1;
        List<AdapterItem> items = this.mApps.getAdapterItems();
        if (!items.isEmpty() && this.mNumAppsPerRow != 0) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                int position = getChildPosition(child);
                if (position != -1) {
                    AdapterItem item = (AdapterItem) items.get(position);
                    if (item.viewType == 1 || item.viewType == 2) {
                        stateOut.rowIndex = item.rowIndex;
                        stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
                        stateOut.rowHeight = child.getHeight();
                        return;
                    }
                }
            }
        }
    }

    private int getScrollAtPosition(int position, int rowHeight) {
        int offset = 0;
        AdapterItem item = (AdapterItem) this.mApps.getAdapterItems().get(position);
        if (item.viewType != 1 && item.viewType != 2) {
            return 0;
        }
        if (item.rowIndex > 0) {
            offset = getPaddingTop();
        }
        return offset + (item.rowIndex * rowHeight);
    }

    private void updateEmptySearchBackgroundBounds() {
    }
}
