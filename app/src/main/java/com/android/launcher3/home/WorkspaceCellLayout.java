package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.android.launcher3.util.event.StylusEventHelper;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceCellLayout extends CellLayout {
    private static final float DISABLE_BUTTON_ALPHA = 0.4f;
    private static final float ENABLE_BUTTON_ALPHA = 1.0f;
    private static final long PAGE_FULL_VI_DURATION = 200;
    static final String TAG = "WorkspaceCellLayout";
    private ImageView mAddBtnView;
    private ImageView mAlignButtonBottom;
    private ImageView mAlignButtonTop;
    private LinearLayout mAlignLayoutBottom;
    private int mAlignLayoutHeight;
    private ArrayList<LinearLayout> mAlignLayoutList;
    private LinearLayout mAlignLayoutTop;
    public LongSparseArray<BackupItem> mBackupItems;
    private int mDefaultPadding;
    private View mDividerBottom;
    private View mDividerTop;
    private Drawable mDragBackground;
    public boolean mGridChanged;
    private CheckLongPressHelper mLongPressHelper;
    private boolean mNeedCustomLayout;
    protected boolean[][] mOccupiedForGrid;
    public List<Pair<ItemInfo, View>> mOutSideItems;
    private float mOverviewShrinkFactorReverse;
    private ImageView mPageDeleteBtn;
    private ValueAnimator mPageFullVI;
    private boolean mPageFullVI_Started;
    public List<Pair<ItemInfo, View>> mRestoredItems;
    private int mScreenGridHeightPadding;
    private float mScreenGridShrinkFactorReverse;
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;
    public boolean mTempPage;
    private Switch mZeroPageSwitch;
    private int mZeroPageSwitchHeight;
    private LinearLayout mZeroPageSwitchLayout;

    public WorkspaceCellLayout(Context context) {
        this(context, null);
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAlignLayoutList = new ArrayList();
        this.mOutSideItems = new ArrayList();
        this.mRestoredItems = new ArrayList();
        this.mBackupItems = new LongSparseArray();
        this.mTempPage = false;
        this.mGridChanged = false;
        this.mNeedCustomLayout = false;
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mLongPressHelper = new CheckLongPressHelper(this);
        setupOverviewLayout();
        setupScreenGridLayout();
        changeColorForBg(WhiteBgManager.isWhiteBg());
    }

    private void setupOverviewLayout() {
        this.mDefaultPadding = getResources().getDimensionPixelSize(R.dimen.home_workspace_page_padding);
        this.mOverviewShrinkFactorReverse = 100.0f / ((float) getResources().getInteger(R.integer.config_workspaceOverviewShrinkPercentage));
        this.mAlignLayoutHeight = (int) (((float) getResources().getDimensionPixelSize(R.dimen.overview_align_layout_height)) * this.mOverviewShrinkFactorReverse);
        this.mZeroPageSwitchHeight = (int) (((float) getResources().getDimensionPixelSize(R.dimen.overview_zeropage_switch_height)) * this.mOverviewShrinkFactorReverse);
        setPadding(this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding);
        int alignbuttonPadding = (int) (((float) getResources().getDimensionPixelSize(R.dimen.overview_align_button_padding)) * this.mOverviewShrinkFactorReverse);
        if (this.mAlignButtonTop != null && this.mAlignButtonBottom != null) {
            this.mAlignButtonTop.setPadding(alignbuttonPadding, alignbuttonPadding, alignbuttonPadding, alignbuttonPadding);
            this.mAlignButtonBottom.setPadding(alignbuttonPadding, alignbuttonPadding, alignbuttonPadding, alignbuttonPadding);
        }
    }

    private void setupScreenGridLayout() {
        this.mScreenGridShrinkFactorReverse = 100.0f / ((float) getResources().getInteger(R.integer.config_workspaceScreenGridShrinkPercentage));
        this.mScreenGridHeightPadding = (int) (((float) getResources().getDimensionPixelSize(R.dimen.screen_grid_workspace_height_padding)) * this.mScreenGridShrinkFactorReverse);
    }

    protected void initChildren(Context context) {
        this.mChildren = new WorkspaceCellLayoutChildren(context);
    }

    private boolean isEqualCellDimensions() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        return this.mCellWidth == dp.homeGrid.getCellWidth() && this.mCellHeight == dp.homeGrid.getCellHeight() && this.mWidthGap == dp.homeGrid.getCellGapX() && this.mHeightGap == dp.homeGrid.getCellGapY() && this.mIconStartPadding == dp.homeGrid.getIconInfo().getIconStartPadding() && this.mLandscape == dp.isLandscape;
    }

    public void setCellDimensions() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellWidth = dp.homeGrid.getCellWidth();
        this.mFixedCellWidth = cellWidth;
        this.mCellWidth = cellWidth;
        cellWidth = dp.homeGrid.getCellHeight();
        this.mFixedCellHeight = cellWidth;
        this.mCellHeight = cellWidth;
        this.mWidthGap = dp.homeGrid.getCellGapX();
        this.mHeightGap = dp.homeGrid.getCellGapY();
        this.mIconStartPadding = dp.homeGrid.getIconInfo().getIconStartPadding();
        this.mCountX = dp.homeGrid.getCellCountX();
        this.mCountY = dp.homeGrid.getCellCountY();
        this.mLandscape = dp.isLandscape;
        if (!(this.mOccupied == null || (this.mOccupied.length == this.mCountX && this.mOccupied[0].length == this.mCountY))) {
            this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        }
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Workspace workspace = (Workspace) getParent();
        return (workspace == null || !workspace.isOverviewState() || workspace.isPlusPage(this) || workspace.indexOfChild(this) == workspace.getCurrentPage()) ? false : true;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        if (((Workspace) getParent()).isOverviewState() && this.mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            this.mLongPressHelper.cancelLongPress();
            handled = true;
        }
        switch (ev.getAction() & 255) {
            case 0:
                if (!this.mStylusEventHelper.inStylusButtonPressed()) {
                    this.mLongPressHelper.setLongPressTimeout(((Workspace) getParent()).isOverviewState() ? 200 : CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT);
                    this.mLongPressHelper.postCheckForLongPress();
                    break;
                }
                break;
            case 1:
            case 3:
            case 5:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                if (!(this.mLongPressHelper.hasPerformedLongPress() || Utilities.pointInView(this, ev.getX(), ev.getY(), this.mSlop))) {
                    this.mLongPressHelper.cancelLongPress();
                    break;
                }
        }
        return handled;
    }

    int[] spanToPixel(int spanX, int spanY) {
        return new int[]{(getCellWidth() * spanX) + ((spanX - 1) * getWidthGap()), (getCellHeight() * spanY) + ((spanY - 1) * getHeightGap())};
    }

    void setBgImage(int state, boolean defaultPage) {
        setBgImageResource(state, defaultPage, WhiteBgManager.isWhiteBg());
    }

    void setBgImageResource(int state, boolean defaultPage, boolean whiteBg) {
        int i = R.drawable.home_edit_panel_bg_default_black;
        Drawable bg = null;
        if (state == 1) {
            bg = getResources().getDrawable(R.drawable.page_normal_bg, null);
            WhiteBgManager.changeColorFilterForBg(getContext(), bg, whiteBg);
        } else if (state == 2) {
            bg = getResources().getDrawable(R.drawable.page_drag_bg, null);
            WhiteBgManager.changeColorFilterForBg(getContext(), bg, whiteBg);
        } else if (state == 4) {
            if (defaultPage) {
                bg = getResources().getDrawable(whiteBg ? R.drawable.home_edit_panel_bg_main_black : R.drawable.home_edit_panel_bg_main, null);
            } else {
                r3 = getResources();
                if (!whiteBg) {
                    i = R.drawable.home_edit_panel_bg_default;
                }
                bg = r3.getDrawable(i, null);
            }
        } else if (state == 5) {
            if (defaultPage) {
                bg = getResources().getDrawable(whiteBg ? R.drawable.screen_grid_bg_main_black : R.drawable.screen_grid_bg_main, null);
            } else {
                r3 = getResources();
                if (!whiteBg) {
                    i = R.drawable.home_edit_panel_bg_default;
                }
                bg = r3.getDrawable(i, null);
            }
        }
        setBackground(bg);
        setPadding(this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding);
    }

    void startPageFullVI() {
        DragManager dragMgr = this.mLauncher.getDragMgr();
        Workspace ws = (Workspace) getParent();
        if (ws != null && dragMgr != null && dragMgr.isDragging()) {
            if (this.mPageFullVI_Started && ws.indexOfChild(this) == ws.getCurrentPage() && getBackgroundAlpha() == 0.0f) {
                this.mPageFullVI_Started = false;
            }
            if (!this.mPageFullVI_Started) {
                int[] cellXY = new int[2];
                int spanX = 1;
                int spanY = 1;
                if (!(dragMgr == null || dragMgr.getDragObject() == null || (!(dragMgr.getDragObject().dragInfo instanceof PendingAddItemInfo) && !(dragMgr.getDragObject().dragInfo instanceof LauncherAppWidgetInfo)))) {
                    ItemInfo widget = dragMgr.getDragObject().dragInfo;
                    spanX = widget.minSpanX;
                    spanY = widget.minSpanY;
                }
                if (!findCellForSpan(cellXY, spanX, spanY, false)) {
                    Drawable bg = getResources().getDrawable(R.drawable.page_full_red_bg, null);
                    if (bg != null) {
                        if (this.mPageFullVI != null) {
                            this.mPageFullVI.cancel();
                        }
                        if (this.mDragBackground == null) {
                            this.mDragBackground = getResources().getDrawable(R.drawable.page_drag_bg, null);
                        }
                        setBackground(bg);
                        setPadding(this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding);
                        this.mPageFullVI = ObjectAnimator.ofFloat(this, "backgroundAlpha", new float[]{0.0f, 1.0f});
                        this.mPageFullVI.setDuration(PAGE_FULL_VI_DURATION);
                        this.mPageFullVI.start();
                        this.mPageFullVI_Started = true;
                    }
                }
            }
        }
    }

    void endPageFullVI() {
        if (this.mPageFullVI_Started) {
            if (this.mPageFullVI != null) {
                this.mPageFullVI.cancel();
            }
            Workspace ws = (Workspace) getParent();
            if (ws.indexOfChild(this) == ws.getCurrentPage()) {
                this.mPageFullVI = ObjectAnimator.ofFloat(this, "backgroundAlpha", new float[]{1.0f, 0.0f});
                this.mPageFullVI.setDuration(PAGE_FULL_VI_DURATION);
                this.mPageFullVI.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        WorkspaceCellLayout.this.setBackground(WorkspaceCellLayout.this.mDragBackground);
                        WorkspaceCellLayout.this.setPadding(WorkspaceCellLayout.this.mDefaultPadding, WorkspaceCellLayout.this.mDefaultPadding, WorkspaceCellLayout.this.mDefaultPadding, WorkspaceCellLayout.this.mDefaultPadding);
                    }
                });
                this.mPageFullVI.start();
            } else if (this.mDragBackground != null) {
                setBackground(this.mDragBackground);
                setPadding(this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding, this.mDefaultPadding);
            }
            this.mPageFullVI_Started = false;
        }
    }

    boolean isPageFullVIStarted() {
        return this.mPageFullVI_Started;
    }

    public void onDragEnter() {
        super.onDragEnter();
        DragManager dragMgr = this.mLauncher.getDragMgr();
        Workspace ws = (Workspace) getParent();
        if (dragMgr != null && !dragMgr.isInScrollArea() && !ws.isScrolling()) {
            startPageFullVI();
        }
    }

    public void onDragExit() {
        super.onDragExit();
        endPageFullVI();
    }

    public void visualizeDropLocation(ItemInfo info, Drawable dragOutline, int cellX, int cellY, int spanX, int spanY, boolean resize) {
        if (!((Workspace) getParent()).isScrolling()) {
            if (cellX < 0 || cellY < 0 || spanY < 0 || spanX < 0) {
                startPageFullVI();
            } else {
                endPageFullVI();
            }
        }
        super.visualizeDropLocation(info, dragOutline, cellX, cellY, spanX, spanY, resize);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mNeedCustomLayout && !((Workspace) getParent()).isScreenGridState()) {
            heightMeasureSpec += this.mAlignLayoutHeight * 2;
            if (this.mZeroPageSwitchLayout != null) {
                this.mZeroPageSwitchLayout.measure(widthMeasureSpec, this.mZeroPageSwitchHeight);
            }
        }
        if (((Workspace) getParent()).isVisibleGridPanel()) {
            heightMeasureSpec += this.mScreenGridHeightPadding * 2;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mNeedCustomLayout && !((Workspace) getParent()).isScreenGridState()) {
            int dividerHeight = (int) (((float) getResources().getDimensionPixelSize(R.dimen.overview_align_layout_divider_height)) * this.mOverviewShrinkFactorReverse);
            if (this.mZeroPageSwitch == null) {
                if (this.mPageDeleteBtn != null) {
                    int i;
                    int pageDeleteBtnSize = (int) (this.mOverviewShrinkFactorReverse * ((float) getResources().getDimensionPixelOffset(R.dimen.overview_align_layout_height)));
                    ImageView imageView = this.mPageDeleteBtn;
                    int measuredWidth = Utilities.sIsRtl ? 0 : getMeasuredWidth() - pageDeleteBtnSize;
                    if (Utilities.sIsRtl) {
                        i = pageDeleteBtnSize;
                    } else {
                        i = getMeasuredWidth();
                    }
                    imageView.layout(measuredWidth, 0, i, pageDeleteBtnSize);
                }
                if (!(this.mAlignLayoutTop == null || this.mAlignLayoutBottom == null)) {
                    this.mAlignLayoutTop.layout(0, 0, getMeasuredWidth(), this.mAlignLayoutHeight);
                    this.mAlignButtonTop.layout(0, 0, getMeasuredWidth(), this.mAlignLayoutHeight);
                    this.mDividerTop.layout(0, this.mAlignLayoutHeight - dividerHeight, getMeasuredWidth(), this.mAlignLayoutHeight);
                    this.mAlignLayoutBottom.layout(0, getMeasuredHeight() - this.mAlignLayoutHeight, getMeasuredWidth(), getMeasuredHeight());
                    this.mAlignButtonBottom.layout(0, 0, getMeasuredWidth(), this.mAlignLayoutHeight);
                    this.mDividerBottom.layout(0, 0, getMeasuredWidth(), dividerHeight);
                }
            } else {
                this.mZeroPageSwitchLayout.layout(0, 0, getMeasuredWidth(), this.mZeroPageSwitchHeight);
                this.mDividerTop.layout(0, this.mZeroPageSwitchHeight - dividerHeight, getMeasuredWidth(), this.mZeroPageSwitchHeight);
            }
        }
        if (((WorkspaceCellLayoutChildren) this.mChildren).isGridChanging()) {
            this.mChildren.requestLayout();
        }
    }

    protected void setChildrenLayout(int l, int t, int r, int b) {
        Workspace workspace = (Workspace) getParent();
        if (!(!this.mNeedCustomLayout || workspace == null || workspace.isScreenGridState() || workspace.isPlusPage(this) || this.mZeroPageSwitch != null)) {
            t += this.mAlignLayoutHeight;
            b -= this.mAlignLayoutHeight;
        }
        if (((Workspace) getParent()).isVisibleGridPanel()) {
            t += this.mScreenGridHeightPadding;
            b -= this.mScreenGridHeightPadding;
        }
        super.setChildrenLayout(l, t, r, b);
    }

    public int getContentIconSize() {
        return this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
    }

    public int getContentTop() {
        return this.mLauncher.getDeviceProfile().homeGrid.getContentTop();
    }

    void restoreGridSize(int x, int y) {
        if (this.mCountX != x || this.mCountY != y) {
            this.mCountX = x;
            this.mCountY = y;
            this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
            this.mOccupiedForGrid = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
            this.mTempRectStack.clear();
            reset(0, 0, 0);
            Log.d(TAG, "restoreGridSize(" + x + ", " + y + ")");
        }
    }

    private void gridSizeChanged(boolean animated) {
        ((WorkspaceCellLayoutChildren) this.mChildren).setGridChangeState(animated);
        for (int i = getPageChildCount() - 1; i >= 0; i--) {
            View childView = getChildOnPageAt(i);
            childView.clearAnimation();
            ((WorkspaceCellLayoutChildren) this.mChildren).makePreviousRectMap(childView);
            if (childView instanceof FolderIconView) {
                ((FolderIconView) childView).applyStyle();
            } else if (childView instanceof IconView) {
                IconView iconView = (IconView) childView;
                iconView.applyStyle();
                iconView.reapplyItemInfo((ItemInfo) childView.getTag());
            }
        }
    }

    void updateIconViews(boolean isRotation) {
        for (int i = getPageChildCount() - 1; i >= 0; i--) {
            View childView = getChildOnPageAt(i);
            childView.clearAnimation();
            if (childView instanceof FolderIconView) {
                FolderIconView folderVIew = (FolderIconView) childView;
                folderVIew.applyStyle();
                folderVIew.refreshBadge();
            } else if (childView instanceof IconView) {
                IconView iconView = (IconView) childView;
                iconView.applyStyle();
                iconView.reapplyItemInfo((ItemInfo) childView.getTag());
            } else if (childView instanceof LauncherAppWidgetHostView) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) childView.getTag();
                if (info != null) {
                    if (isRotation) {
                        info.isRotating = true;
                    } else {
                        int i2;
                        int i3 = this.mCellWidth * info.spanX;
                        if (info.spanX >= 2) {
                            i2 = this.mWidthGap * (info.spanX - 1);
                        } else {
                            i2 = 0;
                        }
                        int targetWidth = i3 + i2;
                        i3 = this.mCellHeight * info.spanY;
                        if (info.spanY >= 2) {
                            i2 = this.mHeightGap * (info.spanY - 1);
                        } else {
                            i2 = 0;
                        }
                        ((LauncherAppWidgetHostView) childView).setResizeScaleResult(LauncherAppWidgetHostView.calculateWidgetSize(info.spanX, info.spanY, targetWidth, i3 + i2));
                        info.notifyWidgetSizeChanged(this.mLauncher);
                    }
                    removeViewInLayout(childView);
                    info.unbind();
                    ((Workspace) getParent()).createAndBindWidget(info);
                    info.isRotating = false;
                }
            }
        }
    }

    void updateItem(ItemInfo item) {
        View v = this.mChildren.getChildAt(item);
        if (v != null) {
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            lp.cellX = item.cellX;
            lp.cellY = item.cellY;
            lp.cellHSpan = item.spanX;
            lp.cellVSpan = item.spanY;
            lp.tmpCellX = item.cellX;
            lp.tmpCellY = item.cellY;
            this.mChildren.setupLp(lp);
        }
    }

    private void reset(int position, int diffX, int diffY) {
        clearOccupied();
        int childCount = this.mChildren.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) this.mChildren.getChildAt(i).getLayoutParams();
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, this.mOccupied, true);
            int cellX = lp.cellX;
            int cellY = lp.cellY;
            int spanX = lp.cellHSpan;
            int spanY = lp.cellVSpan;
            if (lp.cellHSpan > this.mCountX) {
                spanX = this.mCountX;
                if (position == 1 || position == 3) {
                    cellX = lp.cellX + diffX;
                }
            }
            if (lp.cellVSpan > this.mCountY) {
                spanY = this.mCountY;
                if (position == 2 || position == 3) {
                    cellY = lp.cellY + diffY;
                }
            }
            if (position == 0) {
                if (cellX + spanX <= this.mCountX && cellY + spanY <= this.mCountY) {
                    markCellsForView(cellX, cellY, spanX, spanY, this.mOccupiedForGrid, true);
                }
            } else if (position == 1) {
                if (cellX > diffX - 1 && cellY + spanY <= this.mCountY) {
                    markCellsForView(cellX - diffX, cellY, spanX, spanY, this.mOccupiedForGrid, true);
                }
            } else if (position == 2) {
                if (cellX + spanX <= this.mCountX && cellY > diffY - 1) {
                    markCellsForView(cellX, cellY - diffY, spanX, spanY, this.mOccupiedForGrid, true);
                }
            } else if (position == 3 && cellX > diffX - 1 && cellY > diffY - 1) {
                markCellsForView(cellX - diffX, cellY - diffY, spanX, spanY, this.mOccupiedForGrid, true);
            }
        }
    }

    void setGridSizeForScreenGrid(int x, int y, boolean animated, int position, int diffX, int diffY) {
        if (LauncherFeature.supportFlexibleGrid()) {
            gridSizeChanged(animated);
        }
        this.mCountX = x;
        this.mCountY = y;
        this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        this.mOccupiedForGrid = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        this.mTempRectStack.clear();
        reset(position, diffX + 1, diffY + 1);
        requestLayout();
        if (this.mCrossHairView != null) {
            this.mCrossHairView.invalidate();
        }
    }

    void removeItem(ItemInfo itemInfo) {
        View view = this.mChildren.getChildAt(itemInfo);
        if (view != null) {
            removeView(view);
        }
    }

    List<Pair<ItemInfo, View>> getOutSideItems(int outSidePosition) {
        return ScreenGridUtilities.getPairOutSideItems(this.mOutSideItems, outSidePosition);
    }

    void findNearestVacantAreaWithCell(int cellX, int cellY, int spanX, int spanY, int[] result, boolean changeGrid) {
        if (!Utilities.findVacantCellToLeftTop(result, spanX, spanY, this.mCountX, this.mCountY, changeGrid ? this.mOccupiedForGrid : this.mOccupied, cellX, cellY)) {
            if (!Utilities.findVacantCellToRightBottom(result, spanX, spanY, this.mCountX, this.mCountY, changeGrid ? this.mOccupiedForGrid : this.mOccupied, cellX, cellY)) {
                result[1] = -1;
                result[0] = -1;
            }
        }
    }

    void markCellsForGrid(int cellX, int cellY, int spanX, int spanY) {
        markCellsForView(cellX, cellY, spanX, spanY, this.mOccupiedForGrid, true);
    }

    private void clearOccupied() {
        clearOccupiedCells();
        for (int x = 0; x < this.mCountX; x++) {
            for (int y = 0; y < this.mCountY; y++) {
                this.mOccupiedForGrid[x][y] = false;
            }
        }
    }

    void updateOccupied() {
        if (this.mOccupiedForGrid != null) {
            this.mOccupied = this.mOccupiedForGrid;
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    void setCustomFlag(boolean needCustomLayout) {
        this.mNeedCustomLayout = needCustomLayout;
    }

    private void setupZeroPageSwitchLayout() {
        int i;
        int padding = this.mLauncher.getDeviceProfile().homeGrid.getPagePadding() - this.mDefaultPadding;
        ViewGroup.LayoutParams lp = this.mZeroPageSwitch.getLayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels - (padding * 2);
        this.mZeroPageSwitch.setLayoutParams(lp);
        int paddingforScale = (int) ((((float) lp.width) - (((float) lp.width) / this.mOverviewShrinkFactorReverse)) / 2.0f);
        int paddingleft = getResources().getDimensionPixelOffset(R.dimen.overview_zeropage_switch_text_padding_left);
        int paddingRight = getResources().getDimensionPixelOffset(R.dimen.overview_zeropage_switch_text_padding_right);
        Switch switchR = this.mZeroPageSwitch;
        if (Utilities.sIsRtl) {
            i = paddingRight;
        } else {
            i = paddingleft;
        }
        i += paddingforScale;
        if (!Utilities.sIsRtl) {
            paddingleft = paddingRight;
        }
        switchR.setPadding(i, 0, paddingleft + paddingforScale, 0);
        this.mZeroPageSwitch.setTextAppearance(R.style.ZeroPageSwitch);
        this.mZeroPageSwitch.setMinHeight(this.mZeroPageSwitchHeight);
        this.mZeroPageSwitch.setScaleX(this.mOverviewShrinkFactorReverse);
        this.mZeroPageSwitch.setScaleY(this.mOverviewShrinkFactorReverse);
    }

    void addZeroPageSwitch(String appName, boolean isChecked, OnCheckedChangeListener ccl) {
        this.mZeroPageSwitchLayout = new LinearLayout(this.mLauncher);
        this.mZeroPageSwitch = new Switch(this.mLauncher);
        this.mZeroPageSwitchLayout.addView(this.mZeroPageSwitch);
        setupZeroPageSwitchLayout();
        this.mZeroPageSwitch.setChecked(isChecked);
        this.mZeroPageSwitch.setOnCheckedChangeListener(ccl);
        this.mZeroPageSwitch.setOnKeyListener(HomeFocusHelper.ZERO_PAGE_SWITCH_KEY_LISTENER);
        this.mZeroPageSwitch.setText(appName);
        this.mDividerTop = new View(this.mLauncher);
        this.mDividerTop.setFocusable(false);
        this.mDividerTop.setBackground(this.mLauncher.getDrawable(R.color.overview_pannel_align_divider_color));
        this.mZeroPageSwitchLayout.addView(this.mDividerTop);
        addView(this.mZeroPageSwitchLayout);
        if (WhiteBgManager.isWhiteBg()) {
            WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mZeroPageSwitch, true);
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mDividerTop.getBackground(), true);
        }
    }

    void addPageDeleteBtn(OnClickListener onClickListener) {
        this.mPageDeleteBtn = new ImageView(this.mLauncher);
        this.mPageDeleteBtn.setImageDrawable(this.mLauncher.getDrawable(R.drawable.homescreen_btn_delete));
        this.mPageDeleteBtn.setScaleType(ScaleType.FIT_CENTER);
        this.mPageDeleteBtn.setFocusable(true);
        this.mPageDeleteBtn.setClickable(true);
        this.mPageDeleteBtn.setBackground(this.mLauncher.getDrawable(R.drawable.focusable_button_bg));
        this.mPageDeleteBtn.setOnClickListener(onClickListener);
        String description = getResources().getString(R.string.quick_option_remove);
        this.mPageDeleteBtn.setContentDescription(description);
        Utilities.setHoverPopupContentDescription(this.mPageDeleteBtn, description);
        this.mPageDeleteBtn.setOnKeyListener(HomeFocusHelper.PAGE_DELETE_BUTTON_KEY_LISTENER);
        addView(this.mPageDeleteBtn);
    }

    public void touchPageDeleteBtn() {
        this.mPageDeleteBtn.callOnClick();
    }

    void addAlignLayoutTop(OnClickListener onClickListener) {
        this.mAlignLayoutTop = new LinearLayout(this.mLauncher);
        this.mAlignButtonTop = new ImageView(this.mLauncher);
        this.mDividerTop = new View(this.mLauncher);
        setupAlignLayout(this.mAlignLayoutTop, this.mAlignButtonTop, this.mDividerTop, this.mLauncher.getDrawable(R.drawable.homescreen_ic_align_top), getResources().getString(R.string.align_to_top), onClickListener);
    }

    void addAlignLayoutBottom(OnClickListener onClickListener) {
        this.mAlignLayoutBottom = new LinearLayout(this.mLauncher);
        this.mAlignButtonBottom = new ImageView(this.mLauncher);
        this.mDividerBottom = new View(this.mLauncher);
        setupAlignLayout(this.mAlignLayoutBottom, this.mAlignButtonBottom, this.mDividerBottom, this.mLauncher.getDrawable(R.drawable.homescreen_ic_align_bottom), getResources().getString(R.string.align_to_bottom), onClickListener);
    }

    private void setupAlignLayout(LinearLayout alignlayout, ImageView alignbutton, View divider, Drawable drawable, String description, OnClickListener onClickListener) {
        int alignbuttonPadding = (int) (((float) getResources().getDimensionPixelSize(R.dimen.overview_align_button_padding)) * this.mOverviewShrinkFactorReverse);
        alignbutton.setImageDrawable(drawable);
        drawable.setAutoMirrored(true);
        alignbutton.setScaleType(ScaleType.FIT_CENTER);
        alignbutton.setFocusable(true);
        alignbutton.setClickable(true);
        alignbutton.setBackground(this.mLauncher.getDrawable(R.drawable.focusable_button_bg));
        alignbutton.setPadding(alignbuttonPadding, alignbuttonPadding, alignbuttonPadding, alignbuttonPadding);
        alignbutton.setContentDescription(description);
        Utilities.setHoverPopupContentDescription(alignbutton, description);
        alignbutton.setOnClickListener(onClickListener);
        alignbutton.setOnKeyListener(HomeFocusHelper.ALIGN_BUTTON_KEY_LISTENER);
        divider.setFocusable(false);
        divider.setBackground(this.mLauncher.getDrawable(R.color.overview_pannel_align_divider_color));
        alignlayout.addView(alignbutton);
        alignlayout.addView(divider);
        addView(alignlayout);
        this.mAlignLayoutList.add(alignlayout);
        if (WhiteBgManager.isWhiteBg()) {
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, alignbutton, true);
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, divider.getBackground(), true);
        }
    }

    ArrayList<LinearLayout> getAlignLayoutList() {
        return this.mAlignLayoutList;
    }

    View getPageDeleteBtn() {
        return this.mPageDeleteBtn;
    }

    Switch getZeroPageSwitch() {
        return this.mZeroPageSwitch;
    }

    LinearLayout getZeroPageSwitchLayout() {
        return this.mZeroPageSwitchLayout;
    }

    void setEnabledOnAlignButton(boolean isTop) {
        boolean z = true;
        float f = 1.0f;
        this.mAlignButtonTop.setEnabled(!isTop);
        ImageView imageView = this.mAlignButtonTop;
        if (isTop) {
            z = false;
        }
        imageView.setFocusable(z);
        this.mAlignButtonTop.setAlpha(isTop ? DISABLE_BUTTON_ALPHA : 1.0f);
        this.mAlignButtonBottom.setEnabled(isTop);
        this.mAlignButtonBottom.setFocusable(isTop);
        imageView = this.mAlignButtonBottom;
        if (!isTop) {
            f = DISABLE_BUTTON_ALPHA;
        }
        imageView.setAlpha(f);
    }

    void setEnabledOnAlignButton(boolean canAlignTop, boolean canAlignBottom) {
        float f = 1.0f;
        this.mAlignButtonTop.setEnabled(canAlignTop);
        this.mAlignButtonTop.setFocusable(canAlignTop);
        this.mAlignButtonTop.setAlpha(canAlignTop ? 1.0f : DISABLE_BUTTON_ALPHA);
        this.mAlignButtonBottom.setEnabled(canAlignBottom);
        this.mAlignButtonBottom.setFocusable(canAlignBottom);
        ImageView imageView = this.mAlignButtonBottom;
        if (!canAlignBottom) {
            f = DISABLE_BUTTON_ALPHA;
        }
        imageView.setAlpha(f);
    }

    void onConfigurationChangedIfNeeded() {
        if (isEqualCellDimensions()) {
            Log.d(TAG, "onConfigurationChangedIfNeeded skip, all dimmesion is same");
            return;
        }
        setupOverviewLayout();
        setupScreenGridLayout();
        if (this.mZeroPageSwitchLayout == null) {
            final Workspace workspace = (Workspace) getParent();
            if (workspace == null) {
                return;
            }
            if (workspace.isPlusPage(this)) {
                setupAddBtnLayout();
                return;
            }
            setCellDimensions();
            post(new Runnable() {
                public void run() {
                    if (workspace == WorkspaceCellLayout.this.getParent()) {
                        WorkspaceCellLayout.this.updateIconViews(true);
                        WorkspaceCellLayout.this.mLauncher.getHomeController().updateNotificationHelp(false);
                        if (WorkspaceCellLayout.this.mCrossHairView != null && WorkspaceCellLayout.this.mCrossHairView.getVisibility() == 0) {
                            WorkspaceCellLayout.this.mCrossHairView.invalidate();
                        }
                    }
                }
            });
            return;
        }
        setupZeroPageSwitchLayout();
        updateZeroPageLayout();
    }

    private void updateZeroPageLayout() {
        int childCount = this.mChildren.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = this.mChildren.getChildAt(i);
            if (view.getId() == -1) {
                LayoutParams lp = ZeroPageController.getLayoutParamsForZeroPagePreview(this.mLauncher);
                if (lp.cellX >= 0 && lp.cellX <= this.mCountX - 1 && lp.cellY >= 0 && lp.cellY <= this.mCountY - 1) {
                    if (lp.cellHSpan < 0) {
                        lp.cellHSpan = this.mCountX;
                    }
                    if (lp.cellVSpan < 0) {
                        lp.cellVSpan = this.mCountY;
                    }
                }
                view.setLayoutParams(lp);
            }
        }
    }

    void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mAlignButtonTop, whiteBg);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mAlignButtonBottom, whiteBg);
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mZeroPageSwitch, whiteBg);
        changeCrossHairFliter(whiteBg);
        if (this.mDividerTop != null) {
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mDividerTop.getBackground(), whiteBg);
        }
        if (this.mDividerBottom != null) {
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mDividerBottom.getBackground(), whiteBg);
        }
    }

    protected int getTopPaddingCustomPage() {
        int screenGridHeightPadding = (int) (((float) getResources().getDimensionPixelSize(R.dimen.screen_grid_workspace_height_padding)) * (100.0f / ((float) getResources().getInteger(R.integer.config_workspaceScreenGridShrinkPercentage))));
        if (this.mLauncher.getHomeController().isVisibleGridPanel()) {
            return screenGridHeightPadding;
        }
        return 0;
    }

    public void callRefreshLiveIcon() {
        if (this.mChildren != null) {
            this.mChildren.callRefreshLiveIcon();
        }
    }

    protected void addCrossHairView() {
        addView(this.mCrossHairView, indexOfChild(this.mChildren));
    }

    boolean hasPerformedLongPress() {
        return this.mLongPressHelper.hasPerformedLongPress();
    }

    void setHasPerformedLongPress(boolean value) {
        this.mLongPressHelper.setHasPerformedLongPress(value);
    }

    void setupAddBtnLayout() {
        LayoutParams lp;
        int i = 3;
        ImageView addBtnView = getAddBtnView();
        int childCount = this.mChildren.getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View view = this.mChildren.getChildAt(i2);
            if (view != null && view.getId() == -1) {
                removeView(addBtnView);
                break;
            }
        }
        boolean isLandscape = this.mLauncher.getDeviceProfile().isLandscape;
        setCellDimensions(-1, -1, 0, 0);
        if (!LauncherFeature.isTablet()) {
            if (!isLandscape) {
                i = 5;
            }
            setGridSize(5, i);
            if (isLandscape) {
                i = 1;
            } else {
                i = 2;
            }
            lp = new LayoutParams(2, i, 1, 1);
        } else if (isLandscape) {
            setGridSize(9, 7);
            lp = new LayoutParams(4, 3, 1, 1);
        } else {
            setGridSize(7, 9);
            lp = new LayoutParams(3, 4, 1, 1);
        }
        setPadding(0, 0, 0, 0);
        addViewToCellLayout(addBtnView, 0, -1, lp, true);
    }

    private ImageView getAddBtnView() {
        if (this.mAddBtnView == null) {
            this.mAddBtnView = new ImageView(this.mLauncher);
            this.mAddBtnView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            this.mAddBtnView.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    Workspace workspace = (Workspace) WorkspaceCellLayout.this.getParent();
                    if (workspace != null && workspace.isOverviewState()) {
                        workspace.addNewWorkspaceScreen();
                    }
                }
            });
        }
        Drawable addBtn = getResources().getDrawable(R.drawable.plus_page_bg, null);
        addBtn = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(((BitmapDrawable) addBtn).getBitmap(), (int) (((float) Math.round((float) addBtn.getIntrinsicWidth())) * this.mOverviewShrinkFactorReverse), (int) (((float) Math.round((float) addBtn.getIntrinsicHeight())) * this.mOverviewShrinkFactorReverse), false));
        this.mAddBtnView.setImageDrawable(addBtn);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, addBtn, WhiteBgManager.isWhiteBg());
        return this.mAddBtnView;
    }

    Drawable getAddBtnDrawable() {
        if (this.mAddBtnView != null) {
            return this.mAddBtnView.getDrawable();
        }
        return null;
    }
}
