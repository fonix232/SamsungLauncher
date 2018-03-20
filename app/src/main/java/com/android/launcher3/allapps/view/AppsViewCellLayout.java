package com.android.launcher3.allapps.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Array;

public class AppsViewCellLayout extends CellLayout {
    private int mGridTopBottomPadding;

    public AppsViewCellLayout(Context context) {
        this(context, null);
    }

    public AppsViewCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsViewCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupView();
    }

    public void setCellDimensions() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellWidth = dp.appsGrid.getCellWidth();
        this.mFixedCellWidth = cellWidth;
        this.mCellWidth = cellWidth;
        cellWidth = dp.appsGrid.getCellHeight();
        this.mFixedCellHeight = cellWidth;
        this.mCellHeight = cellWidth;
        this.mWidthGap = dp.appsGrid.getCellGapX();
        this.mHeightGap = dp.appsGrid.getCellGapY();
        this.mIconStartPadding = dp.appsGrid.getIconInfo().getIconStartPadding();
        this.mLandscape = dp.isLandscape;
        this.mCountX = dp.appsGrid.getCellCountX();
        this.mCountY = dp.appsGrid.getCellCountY();
        if (!(this.mOccupied == null || (this.mOccupied.length == this.mCountX && this.mOccupied[0].length == this.mCountY))) {
            this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
            this.mTempRectStack.clear();
        }
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
    }

    public void updateIconViews() {
        for (int i = this.mChildren.getChildCount() - 1; i >= 0; i--) {
            View childView = this.mChildren.getChildAt(i);
            childView.clearAnimation();
            if (childView instanceof FolderIconView) {
                FolderIconView folderVIew = (FolderIconView) childView;
                folderVIew.applyStyle();
                folderVIew.refreshBadge();
            } else if (childView instanceof IconView) {
                IconView iconView = (IconView) childView;
                iconView.applyStyle();
                iconView.reapplyItemInfo((ItemInfo) childView.getTag());
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0 && (((AppsPagedView) getParent()).isGridState() || ((AppsPagedView) getParent()).isTidyState())) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public int getContentIconSize() {
        return this.mLauncher.getDeviceProfile().appsGrid.getIconSize();
    }

    public int getContentTop() {
        return this.mLauncher.getDeviceProfile().appsGrid.getContentTop();
    }

    public void setBgImage(int state) {
        Drawable bg = null;
        if (state == 1) {
            bg = getResources().getDrawable(R.drawable.edit_apps_bg, null);
        } else if (state == 2) {
            bg = getResources().getDrawable(R.drawable.edit_apps_bg, null);
        } else if (state == 4) {
            bg = getResources().getDrawable(R.drawable.page_view_overlay_select, null);
        } else if (state == 5) {
            bg = getResources().getDrawable(R.drawable.page_view_overlay_select, null);
        }
        setBackground(bg);
    }

    public boolean isFullyOccupied() {
        return this.mCountX * this.mCountY <= getCellLayoutChildren().getChildCount();
    }

    protected boolean updateChildIfReorderAnimationCancel() {
        return true;
    }

    protected boolean supportWhiteBg() {
        return false;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isNeedGridPadding()) {
            heightMeasureSpec += this.mGridTopBottomPadding * 2;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void setChildrenLayout(int l, int t, int r, int b) {
        if (isNeedGridPadding()) {
            t += this.mGridTopBottomPadding;
            b -= this.mGridTopBottomPadding;
        }
        super.setChildrenLayout(l, t, r, b);
    }

    private boolean isNeedGridPadding() {
        return ((AppsPagedView) getParent()).isGridState() || ((AppsPagedView) getParent()).isSwitchingGridToNormal();
    }

    private void setupView() {
        this.mGridTopBottomPadding = getResources().getDimensionPixelOffset(R.dimen.all_apps_grid_top_bottom_padding);
    }

    protected int getTopPaddingCustomPage() {
        return isNeedGridPadding() ? this.mGridTopBottomPadding : 0;
    }

    public boolean childToPosition(View child, int cellX, int cellY) {
        CellLayoutChildren clc = getCellLayoutChildren();
        if (this.mOccupied == null) {
            return false;
        }
        if (clc.indexOfChild(child) != -1) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            lp.cellX = cellX;
            lp.cellY = cellY;
            if (info != null) {
                info.cellX = cellX;
                info.cellY = cellY;
            }
            clc.setupLp(lp);
            child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
        }
        return true;
    }
}
