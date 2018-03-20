package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class HotseatCellLayout extends CellLayout {
    private static final int REORDER_ANIMATION_DURATION = 300;
    private static final String TAG = "HotseatCellLayout";
    private Hotseat mHotseat;
    private int mLeftPadding;
    private int mMaxCellCount;
    private int mPrevCountX;
    private AnimatorListener reorderAnimListener;

    public HotseatCellLayout(Context context) {
        this(context, null);
    }

    public HotseatCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HotseatCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPrevCountX = this.mCountX;
        this.reorderAnimListener = new AnimatorListener() {
            boolean cancelled = false;

            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                if (this.cancelled) {
                    this.cancelled = false;
                } else {
                    HotseatCellLayout.this.mHotseat.changeGrid(true);
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.cancelled = true;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        };
    }

    public void setCellDimensions() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        this.mWidthGap = dp.homeGrid.getCellGapX();
        this.mHeightGap = dp.homeGrid.getCellGapY();
        this.mLandscape = dp.isLandscape;
        this.mIconStartPadding = dp.homeGrid.getIconInfo().getIconStartPadding();
    }

    void setHotseat(Hotseat hotseat) {
        this.mHotseat = hotseat;
    }

    void setMaxCellCount(int count) {
        this.mMaxCellCount = count;
    }

    int getMaxCellCount() {
        return this.mMaxCellCount;
    }

    public int getContentIconSize() {
        return this.mLauncher.getDeviceProfile().hotseatGridIcon.getIconSize();
    }

    public int getContentTop() {
        return this.mLauncher.getDeviceProfile().hotseatGridIcon.getContentTop();
    }

    boolean isFull() {
        if (getCellLayoutChildren().getChildCount() >= this.mMaxCellCount) {
            return true;
        }
        return false;
    }

    AnimatorSet reorderRemoveCells(boolean animate, boolean[][] occupied) {
        return reorderAllChildren(animate, occupied, 0, false);
    }

    AnimatorSet reorderMakeCells(boolean animate, boolean[][] occupied, int target) {
        return reorderAllChildren(animate, occupied, target, true);
    }

    AnimatorSet reorderAllChildren(boolean animate, boolean[][] occupied, int target, boolean expandedGrid) {
        AnimatorSet set = new AnimatorSet();
        int x = 0;
        int y = 0;
        int length = 0;
        if (occupied.length > 0) {
            length = this.mHotseat.isVerticalHotseat() ? occupied[0].length : occupied.length;
        }
        int i = 0;
        int index = expandedGrid ? cellToPosition(getCountX(), getCountY()) - 1 : 0;
        while (i < length) {
            int index2;
            if (!this.mHotseat.isVerticalHotseat()) {
                x = expandedGrid ? (length - 1) - i : i;
            } else if (expandedGrid) {
                y = (length - 1) - i;
            } else {
                y = i;
            }
            View child = getCellLayoutChildren().getChildAt(x, y);
            if (child == null) {
                index2 = index;
            } else {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (this.mHotseat.isVerticalHotseat()) {
                    if (expandedGrid) {
                        if (lp.cellY >= target) {
                            index2 = index - 1;
                            lp.cellY = index;
                        }
                        index2 = index;
                    } else {
                        index2 = index + 1;
                        lp.cellY = index;
                    }
                } else if (expandedGrid) {
                    if (lp.cellX >= target) {
                        index2 = index - 1;
                        lp.cellX = index;
                    }
                    index2 = index;
                } else {
                    index2 = index + 1;
                    lp.cellX = index;
                }
                if (this.mLandscape && !LauncherFeature.isTablet() && length == 1 && expandedGrid && !this.mHotseat.isVerticalHotseat()) {
                    animate = false;
                }
                if (checkInvalidPosition(this.mHotseat.isVerticalHotseat() ? lp.cellY : lp.cellX)) {
                    Log.d(TAG, "reorderAllChildren Item : " + ((ItemInfo) child.getTag()) + " lp.x : " + lp.cellX + " lp.y : " + lp.cellY + " animate : " + animate);
                    this.mHotseat.dumpHotseatItem();
                    throw new IllegalStateException("attempted to move icon to invalid position");
                }
                set.play(moveIcon(child, -1, -1, animate, false));
            }
            i++;
            index = index2;
        }
        boolean completeDnD = (this.mLauncher.getDragMgr().dragging() || this.mLauncher.getHomeController().getState() == 2) ? false : true;
        if (completeDnD) {
            set.addListener(this.reorderAnimListener);
        }
        return set;
    }

    AnimatorSet realTimeReorder(int empty, int target) {
        int count = this.mHotseat.isVerticalHotseat() ? this.mCountY : this.mCountX;
        AnimatorSet set = new AnimatorSet();
        if (target == empty) {
            return null;
        }
        int direction;
        if (target > empty) {
            direction = 1;
        } else {
            direction = -1;
        }
        int i = empty;
        while (i != target) {
            int nextPos = i + direction;
            View v = getChildAt(this.mHotseat.isVerticalHotseat() ? nextPos / count : nextPos % count, this.mHotseat.isVerticalHotseat() ? nextPos % count : nextPos / count);
            if (!(v == null || v.getTag() == null)) {
                int i2;
                markCellsAsUnoccupiedForView(v);
                int x = this.mHotseat.isVerticalHotseat() ? i / count : i % count;
                int y = this.mHotseat.isVerticalHotseat() ? i % count : i / count;
                if (this.mHotseat.isVerticalHotseat()) {
                    i2 = y;
                } else {
                    i2 = x;
                }
                if (checkInvalidPosition(i2)) {
                    Log.d(TAG, "realTimeReorder Item : " + ((ItemInfo) v.getTag()) + " lp.x : " + x + " lp.y : " + "empty : " + empty + " target : " + target);
                    this.mHotseat.dumpHotseatItem();
                    throw new IllegalStateException("attempted to move icon to invalid position");
                }
                set.play(moveIcon(v, x, y, true, true));
            }
            i += direction;
        }
        return set;
    }

    private Animator moveIcon(final View child, int cellX, int cellY, boolean animate, boolean realTimeReorder) {
        CellLayoutChildren clc = getCellLayoutChildren();
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int oldX = lp.x - this.mLeftPadding;
        final int oldY = lp.y;
        int[] newLp = new int[2];
        if (realTimeReorder) {
            lp.cellX = cellX;
            lp.cellY = cellY;
            clc.setupLp(lp);
            newLp[0] = lp.x;
            newLp[1] = lp.y;
        } else {
            newLp = getNextLp(child);
        }
        final int newX = newLp[0];
        final int newY = newLp[1];
        markCellsAsOccupiedForView(child);
        if (oldX == newX && oldY == newY) {
            lp.isLockedToGrid = true;
            return null;
        }
        lp.isLockedToGrid = false;
        if (animate) {
            Animator va = LauncherAnimUtils.ofFloat(child, 0.0f, 1.0f);
            va.setDuration(300);
            final View view = child;
            va.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float p = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) (((1.0f - p) * ((float) oldX)) + (((float) newX) * p));
                    lp.y = (int) (((1.0f - p) * ((float) oldY)) + (((float) newY) * p));
                    view.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    lp.isLockedToGrid = true;
                    child.requestLayout();
                }
            });
            return va;
        }
        lp.x = newX;
        lp.y = newY;
        lp.isLockedToGrid = true;
        child.requestLayout();
        return null;
    }

    private int[] getNextLp(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int[] newLp = new int[2];
        if (this.mHotseat.isVerticalHotseat()) {
            newLp[0] = lp.x;
            newLp[1] = lp.cellY * (this.mCellHeight + this.mHeightGap);
        } else {
            int iconSize = ((IconView) child).getIconSize();
            int extraMoveDistance = Math.abs(((lp.getWidth() - iconSize) - (this.mCellWidth - iconSize)) / 2);
            if (!LauncherFeature.isTablet() && this.mLandscape) {
                extraMoveDistance = 0;
            }
            newLp[0] = (this.mCellWidth + this.mWidthGap) * (Utilities.sIsRtl ? (this.mCountX - lp.cellX) - 1 : lp.cellX);
            newLp[1] = lp.y;
            if (lp.getWidth() > this.mCellWidth) {
                newLp[0] = newLp[0] - extraMoveDistance;
            } else {
                newLp[0] = newLp[0] + extraMoveDistance;
            }
        }
        return newLp;
    }

    boolean hasEmptyCell() {
        boolean[][] occupied = getOccupied();
        for (int x = 0; x < this.mCountX; x++) {
            for (int y = 0; y < this.mCountY; y++) {
                if (!occupied[x][y]) {
                    return true;
                }
            }
        }
        return false;
    }

    int getEmptyCount(int cellCount) {
        return cellCount - getCellLayoutChildren().getChildCount();
    }

    int cellToPosition(int cellX, int cellY) {
        return this.mHotseat.isVerticalHotseat() ? cellY : cellX;
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params, boolean markCells) {
        if (!hasEmptyCell()) {
            ItemInfo info = (ItemInfo) child.getTag();
            this.mHotseat.getDragController().makeEmptyCell(cellToPosition(info.cellX, info.cellY), true, true);
        }
        return super.addViewToCellLayout(child, index, childId, params, markCells);
    }

    public void removeViewInLayout(View view) {
        super.removeViewInLayout(view);
        if (!hasTargetView()) {
            ItemInfo info = (ItemInfo) view.getTag();
            this.mHotseat.getDragController().removeEmptyCells(true, true);
            if (info instanceof FolderInfo) {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_DELETE, "Folder", -1, false);
            } else {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_DELETE, info.getIntent().getComponent() != null ? info.getIntent().getComponent().getPackageName() : null, -1, false);
            }
            Resources res = this.mLauncher.getResources();
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_QuickOptions), res.getString(R.string.event_RemoveFromDock));
        }
    }

    void gridSizeChanged(final GridIconInfo prevGridIcon, final boolean animated) {
        if (this.mHotseat.getDragController().isReorderRunning()) {
            new Handler().post(new Runnable() {
                public void run() {
                    HotseatCellLayout.this.gridSizeChanged(prevGridIcon, animated);
                }
            });
            return;
        }
        CellLayoutChildren child = getCellLayoutChildren();
        for (int i = child.getChildCount() - 1; i >= 0; i--) {
            View childView = child.getChildAt(i);
            if (childView instanceof FolderIconView) {
                FolderIconView folderVIew = (FolderIconView) childView;
                folderVIew.applyStyle();
                folderVIew.refreshBadge();
            } else if (childView instanceof IconView) {
                IconView iconView = (IconView) childView;
                iconView.applyStyle();
                iconView.reapplyItemInfo((ItemInfo) childView.getTag());
            }
            if (animated) {
                ((IconView) childView).animateChildScale(prevGridIcon);
            }
        }
    }

    void updateFolderGrid() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                CellLayoutChildren child = HotseatCellLayout.this.getCellLayoutChildren();
                for (int i = child.getChildCount() - 1; i >= 0; i--) {
                    View childView = child.getChildAt(i);
                    if (childView instanceof FolderIconView) {
                        FolderIconView folderView = (FolderIconView) childView;
                        folderView.getFolderView().getContent().updateFolderGrid();
                        folderView.getFolderView().rearrangeChildren();
                    }
                }
            }
        }, (long) this.mLauncher.getResources().getInteger(R.integer.config_delay_AppsGridChanged));
    }

    void updateIconViews() {
        CellLayoutChildren child = getCellLayoutChildren();
        int childCount = child.getChildCount();
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        if (deviceProfile.isVerticalBarLayout()) {
            setGridSize(1, childCount);
        } else {
            setGridSize(childCount, 1);
        }
        deviceProfile.setCurrentHotseatGridIcon(childCount);
        Log.d(TAG, "updateIconViews: child count - " + childCount);
        for (int i = childCount - 1; i >= 0; i--) {
            View childView = child.getChildAt(i);
            if (childView instanceof FolderIconView) {
                FolderIconView folderVIew = (FolderIconView) childView;
                Log.d(TAG, "updateIconViews: applying folder style");
                folderVIew.applyStyle();
                folderVIew.refreshBadge();
            } else if (childView instanceof IconView) {
                IconView iconView = (IconView) childView;
                Log.d(TAG, "updateIconViews: applying icon style");
                iconView.applyStyle();
                iconView.reapplyItemInfo((ItemInfo) childView.getTag());
            }
        }
    }

    public void setGridSize(int x, int y) {
        int paddingLeft;
        int width;
        int height;
        boolean needToCenter = true;
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (dp.isVerticalBarLayout()) {
            paddingLeft = getPaddingLeft();
        } else {
            paddingLeft = 0;
        }
        this.mLeftPadding = paddingLeft;
        if (dp.isVerticalBarLayout() || !this.mLandscape || LauncherFeature.isTablet() || x != 1) {
            needToCenter = false;
        }
        if (dp.isVerticalBarLayout()) {
            width = dp.homeGrid.getHotseatBarSize();
        } else {
            width = (dp.availableWidthPx - this.mHotseat.getPaddingLeft()) - this.mHotseat.getPaddingRight();
        }
        if (dp.isVerticalBarLayout()) {
            height = (dp.availableHeightPx - this.mHotseat.getPaddingTop()) - this.mHotseat.getPaddingBottom();
        } else {
            height = dp.homeGrid.getHotseatBarSize();
        }
        if (this.mLandscape && LauncherFeature.supportNavigationBar()) {
            width -= dp.getMultiWindowPanelSize();
        }
        int childWidthSize = width - (this.mLeftPadding + getPaddingRight());
        int childHeightSize = height - (getPaddingTop() + getPaddingBottom());
        paddingLeft = DeviceProfile.calculateCellWidthOrHeight(childWidthSize, this.mWidthGap, x);
        this.mFixedCellWidth = paddingLeft;
        this.mCellWidth = paddingLeft;
        paddingLeft = DeviceProfile.calculateCellWidthOrHeight(childHeightSize, this.mHeightGap, y);
        this.mFixedCellHeight = paddingLeft;
        this.mCellHeight = paddingLeft;
        if (needToCenter) {
            int cellWidth = ((childWidthSize + this.mWidthGap) / (this.mLandscape ? 3 : this.mMaxCellCount)) - this.mWidthGap;
            this.mLeftPadding = (childWidthSize - cellWidth) / 2;
            this.mCellWidth = cellWidth;
        }
        dp.setHotseatCellWidthSize(this.mCellWidth);
        if (!dp.isVerticalBarLayout()) {
            setPadding(this.mLeftPadding, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
        this.mPrevCountX = this.mCountX;
        super.setGridSize(x, y);
    }

    private boolean checkInvalidPosition(int position) {
        return position < 0 || position >= (this.mHotseat.isVerticalHotseat() ? getCountY() : getCountX());
    }

    public String getItemPositionDescription(int x, int y) {
        int pos;
        if (this.mHotseat.isVerticalHotseat()) {
            pos = y;
        } else {
            pos = x;
        }
        return String.format(getContext().getString(R.string.tts_hotseat_move_to), new Object[]{Integer.valueOf(pos + 1)});
    }

    public int getPrevCountX() {
        return this.mPrevCountX;
    }
}
