package com.android.launcher3.folder.view;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.WhiteBgManager;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Array;

public class FolderCellLayout extends CellLayout {
    private ImageView mPartialBgImage;

    public FolderCellLayout(Context context) {
        this(context, null);
    }

    public FolderCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void initChildren(Context context) {
        this.mPartialBgImage = new ImageView(context);
        addView(this.mPartialBgImage, 0);
        super.initChildren(context);
    }

    public void setCellDimensions() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellWidth = dp.folderGrid.getCellWidth();
        this.mFixedCellWidth = cellWidth;
        this.mCellWidth = cellWidth;
        cellWidth = dp.folderGrid.getCellHeight();
        this.mFixedCellHeight = cellWidth;
        this.mCellHeight = cellWidth;
        this.mWidthGap = dp.folderGrid.getCellGapX();
        this.mHeightGap = dp.folderGrid.getCellGapY();
        this.mIconStartPadding = dp.folderGrid.getIconInfo().getIconStartPadding();
        this.mCountX = dp.folderGrid.getCellCountX();
        this.mCountY = dp.folderGrid.getCellCountY();
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
        this.mLandscape = dp.isLandscape;
        if (!(this.mOccupied == null || (this.mOccupied.length == this.mCountX && this.mOccupied[0].length == this.mCountY))) {
            this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        }
        calculatePadding();
    }

    public void setGridSize(int x, int y) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellWidth = dp.folderGrid.getCellWidth();
        this.mFixedCellWidth = cellWidth;
        this.mCellWidth = cellWidth;
        cellWidth = dp.folderGrid.getCellHeight();
        this.mFixedCellHeight = cellWidth;
        this.mCellHeight = cellWidth;
        this.mWidthGap = dp.folderGrid.getCellGapX();
        this.mHeightGap = dp.folderGrid.getCellGapY();
        super.setGridSize(x, y);
        calculatePadding();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculatePadding();
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        LayoutParams lp = (LayoutParams) this.mPartialBgImage.getLayoutParams();
        this.mPartialBgImage.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
    }

    private void calculatePadding() {
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int contentsWidth = (getDesiredWidth() - getPaddingLeft()) - getPaddingRight();
        int contentsHeight = (getDesiredHeight() - getPaddingTop()) - getPaddingBottom();
        if (measuredWidth > contentsWidth || measuredHeight > contentsHeight) {
            int paddingLR = (measuredWidth - contentsWidth) / 2;
            setPadding(paddingLR, getResources().getDimensionPixelSize(R.dimen.open_folder_content_margin_top), paddingLR, 0);
        }
    }

    public int getContentIconSize() {
        return this.mLauncher.getDeviceProfile().folderGrid.getIconSize();
    }

    public int getContentTop() {
        return this.mLauncher.getDeviceProfile().folderGrid.getContentTop();
    }

    public void updateCellDimensionsIfNeeded() {
        setCellDimensions();
        int childCount = this.mChildren.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = this.mChildren.getChildAt(i);
            if (v instanceof IconView) {
                IconView icon = (IconView) v;
                icon.applyStyle();
                icon.reapplyItemInfo((ItemInfo) v.getTag());
            }
        }
    }

    public void setBgImage(int state, int bgWidth, int bgHeight, boolean whiteBg) {
        if (state == 2) {
            LayoutParams lp = (LayoutParams) this.mPartialBgImage.getLayoutParams();
            int width = bgWidth > 0 ? bgWidth : getMeasuredWidth();
            int height = bgHeight > 0 ? bgHeight : getMeasuredHeight();
            lp.width = width;
            lp.height = height;
            lp.x = (getMeasuredWidth() - width) / 2;
            lp.y = (getMeasuredHeight() - height) / 2;
            this.mPartialBgImage.setImageDrawable(getResources().getDrawable(whiteBg ? R.drawable.page_view_overlay_select_03_w : R.drawable.page_view_overlay_select_03, null));
            this.mPartialBgImage.setScaleType(ScaleType.FIT_XY);
            Drawable fullBg = getResources().getDrawable(R.drawable.page_view_overlay, null);
            WhiteBgManager.changeColorFilterForBg(getContext(), fullBg, whiteBg);
            setBackground(fullBg);
            return;
        }
        setBackground(null);
        this.mPartialBgImage.setImageDrawable(null);
    }

    public void setPartialBackgroundAlpha(float alpha) {
        setPartialBackgroundAlpha(alpha, false);
    }

    public void setPartialBackgroundAlpha(float alpha, boolean animate) {
        if (animate) {
            ValueAnimator animator;
            if (alpha == 0.0f) {
                animator = ValueAnimator.ofInt(new int[]{255, 0});
            } else {
                animator = ValueAnimator.ofInt(new int[]{0, 255});
            }
            animator.setDuration(200);
            animator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FolderCellLayout.this.mPartialBgImage.setImageAlpha(((Integer) valueAnimator.getAnimatedValue()).intValue());
                }
            });
            animator.start();
            return;
        }
        this.mPartialBgImage.setImageAlpha((int) (255.0f * alpha));
    }

    public void setAccessibilityEnabled(boolean enabled) {
        int accessibility = enabled ? 1 : 2;
        setImportantForAccessibility(accessibility);
        int childCount = this.mChildren.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mChildren.getChildAt(i).setImportantForAccessibility(accessibility);
        }
    }

    protected boolean supportWhiteBg() {
        return !((FolderPagedView) getParent()).isAppsFolder();
    }
}
