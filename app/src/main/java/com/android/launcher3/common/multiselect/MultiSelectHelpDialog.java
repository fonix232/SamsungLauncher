package com.android.launcher3.common.multiselect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.PagedView.PageScrollListener;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.deviceprofile.GridInfo;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;

public class MultiSelectHelpDialog extends LinearLayout implements PageScrollListener {
    private static final String TAG = "MultiSelectHelpDialog";
    private AnimatorSet mAnimator;
    private int mCurX;
    private int mInitPage;
    private int mInitX;
    private boolean mIsHotseat;
    private boolean mIsTopPicker;
    private Launcher mLauncher;
    private MultiSelectManager mMultiSelectManager;
    private int mPadding;
    private int mPivotX;

    public MultiSelectHelpDialog(Context context) {
        this(context, null);
    }

    public MultiSelectHelpDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiSelectHelpDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInitX = 0;
        this.mCurX = 0;
        this.mInitPage = -1;
        this.mPadding = 0;
        this.mIsHotseat = false;
        this.mAnimator = null;
        this.mIsTopPicker = false;
        this.mPivotX = 0;
        this.mLauncher = (Launcher) context;
        this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
    }

    private boolean layout(View view) {
        Resources res = this.mLauncher.getResources();
        int widthPixels = res.getDisplayMetrics().widthPixels;
        int pickerWidth = res.getDimensionPixelSize(R.dimen.multi_select_help_dialog_arrow_width);
        int pickerMargin = res.getDimensionPixelSize(R.dimen.multi_select_help_dialog_arrow_margin);
        int dialogMarginTop = res.getDimensionPixelSize(R.dimen.multi_select_help_dialog_margin_top);
        int dialogMarginLeft = res.getDimensionPixelSize(R.dimen.multi_select_help_dialog_margin_left);
        int dialogPadding = res.getDimensionPixelSize(R.dimen.multi_select_help_dialog_padding);
        int panelHeight = res.getDimensionPixelSize(R.dimen.multi_select_panel_height);
        int translationY = res.getDimensionPixelSize(R.dimen.multi_select_panel_translation_y);
        View pickerTop = findViewById(R.id.multi_select_help_dialog_picker_up);
        View pickerBottom = findViewById(R.id.multi_select_help_dialog_picker_down);
        ItemInfo info = (ItemInfo) view.getTag();
        boolean z = info != null && info.container == -101;
        this.mIsHotseat = z;
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        GridIconInfo gridIconInfo = this.mIsHotseat ? dp.hotseatGridIcon : null;
        GridInfo gridInfo = this.mLauncher.isHomeStage() ? dp.homeGrid : this.mLauncher.isAppsStage() ? dp.appsGrid : dp.folderGrid;
        int iconSize = gridIconInfo != null ? gridIconInfo.getIconSize() : gridInfo.getIconSize();
        int[] absLoc = new int[2];
        view.setTranslationY(0.0f);
        view.getLocationOnScreen(absLoc);
        if (absLoc[0] < 0 || absLoc[0] > widthPixels) {
            Log.d(TAG, "The help bubble does not exist on current screen");
            return false;
        }
        int y;
        int i = absLoc[0];
        int width = (!dp.isLandscape || LauncherFeature.isTablet()) ? (view.getWidth() - iconSize) / 2 : ((IconView) view).getIconInfo().getIconStartPadding();
        absLoc[0] = width + i;
        absLoc[1] = (gridIconInfo != null ? gridIconInfo.getContentTop() : gridInfo.getContentTop()) + absLoc[1];
        if (dp.isMultiwindowMode) {
            if (dp.isLandscape) {
                absLoc[0] = absLoc[0] - dp.getMultiWindowPanelSize();
            } else {
                absLoc[1] = absLoc[1] - dp.getMultiWindowPanelSize();
            }
        }
        measure(0, 0);
        int maxWidth = widthPixels - (dialogMarginLeft * 2);
        if (getMeasuredWidth() >= maxWidth) {
            ((TextView) findViewById(R.id.multi_select_help_dialog_body)).getLayoutParams().width = maxWidth - dialogPadding;
            measure(0, 0);
        }
        int dialogWidth = getMeasuredWidth();
        int dialogHeight = getMeasuredHeight();
        float transY = (float) absLoc[1];
        float x = ((float) (absLoc[0] + (iconSize / 2))) - (((float) dialogWidth) / 2.0f);
        if (x < ((float) dialogMarginLeft)) {
            x = (float) dialogMarginLeft;
        } else if (((float) dialogWidth) + x > ((float) (widthPixels - dialogMarginLeft))) {
            x = (float) ((widthPixels - dialogWidth) - dialogMarginLeft);
        }
        int pickerX = (int) (((float) ((absLoc[0] + (iconSize / 2)) - (pickerWidth / 2))) - x);
        this.mPivotX = (pickerWidth / 2) + pickerX;
        if (Utilities.sIsRtl) {
            pickerX -= dialogWidth - pickerWidth;
            setLeft(widthPixels - dialogWidth);
            setRight(widthPixels);
        }
        setX(x);
        this.mIsTopPicker = false;
        if (transY - ((float) panelHeight) <= ((float) gridInfo.getPageTop())) {
            y = (int) (((((float) iconSize) + transY) + ((float) dialogMarginTop)) + ((float) pickerMargin));
            this.mIsTopPicker = true;
        } else {
            y = (int) (((transY - ((float) dialogHeight)) - ((float) dialogMarginTop)) - ((float) pickerMargin));
        }
        if (!(LauncherFeature.supportMultiSelectSlideVI() && this.mLauncher.isHomeStage() && !this.mLauncher.isFolderStage())) {
            translationY = 0;
        }
        setY((float) (y + translationY));
        pickerBottom.setTranslationX((float) pickerX);
        pickerTop.setTranslationX((float) pickerX);
        if (this.mIsTopPicker) {
            pickerTop.setVisibility(View.VISIBLE);
            pickerBottom.setVisibility(View.GONE);
        } else {
            pickerTop.setVisibility(View.GONE);
            pickerBottom.setVisibility(View.VISIBLE);
        }
        width = (int) getX();
        this.mCurX = width;
        this.mInitX = width;
        setScaleX(0.0f);
        setScaleY(0.0f);
        setAlpha(0.0f);
        if (this.mLauncher.isFolderStage()) {
            FolderView folderView = this.mLauncher.getOpenFolderView();
            if (!(folderView == null || folderView.getContent() == null)) {
                folderView.getContent().setPageScrollListener(this);
            }
        }
        return true;
    }

    void show(View sourceView, boolean animate) {
        Log.d(TAG, "show help bubble - animate = " + animate);
        if (isShowingHelpDialog()) {
            hide(false);
        }
        this.mInitPage = -1;
        if (layout(sourceView)) {
            animateDialog(true, animate);
        }
    }

    void hide(boolean animate) {
        Log.d(TAG, "hide help bubble - animate = " + animate);
        animateDialog(false, animate);
        this.mInitPage = -1;
    }

    boolean isShowingHelpDialog() {
        return getVisibility() == 0;
    }

    public void onPageScroll(int page, int x, int y, int scrollX, int pageCount) {
        if (!this.mIsHotseat && isShowingHelpDialog()) {
            int pageIndex;
            if (Utilities.sIsRtl) {
                pageIndex = (pageCount - 1) - page;
            } else {
                pageIndex = page;
            }
            if (this.mInitPage == -1) {
                this.mInitPage = pageIndex;
                this.mPadding = pageIndex == 0 ? scrollX : Math.abs((scrollX - (this.mLauncher.getResources().getDisplayMetrics().widthPixels * pageIndex)) / pageIndex);
            }
            int curX = (this.mCurX + scrollX) - x;
            int finalX = pageIndex == this.mInitPage ? curX : pageIndex < this.mInitPage ? Math.max(this.mInitX, curX) : Math.min(this.mInitX, curX);
            setX((float) finalX);
        }
    }

    public void onPageChange(int page, int scrollX, int pageCount) {
        if (!this.mIsHotseat && isShowingHelpDialog() && this.mInitPage != -1) {
            int pageIndex;
            if (Utilities.sIsRtl) {
                pageIndex = (pageCount - 1) - page;
            } else {
                pageIndex = page;
            }
            int initPage = this.mInitPage * this.mLauncher.getResources().getDisplayMetrics().widthPixels;
            int padding = this.mPadding * (pageIndex < this.mInitPage ? pageIndex + 1 : pageIndex - 1);
            if (pageIndex != this.mInitPage) {
                this.mCurX = ((this.mInitX + initPage) - scrollX) - padding;
                setX((float) this.mCurX);
                return;
            }
            this.mCurX = this.mInitX;
        }
    }

    private void animateDialog(final boolean show, boolean animate) {
        float f = 0.0f;
        float value = 1.0f;
        if (this.mAnimator != null && this.mAnimator.isRunning()) {
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
        if (animate) {
            if (!show) {
                value = 0.0f;
            }
            ObjectAnimator scaleX = LauncherAnimUtils.ofFloat(this, View.SCALE_X.getName(), value);
            ObjectAnimator scaleY = LauncherAnimUtils.ofFloat(this, View.SCALE_Y.getName(), value);
            ObjectAnimator alpha = LauncherAnimUtils.ofFloat(this, View.ALPHA.getName(), value);
            setPivotX((float) this.mPivotX);
            if (!this.mIsTopPicker) {
                f = (float) getMeasuredHeight();
            }
            setPivotY(f);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.playTogether(new Animator[]{scaleX, scaleY, alpha});
            this.mAnimator.setDuration(300);
            this.mAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (show) {
                        MultiSelectHelpDialog.this.setVisibility(View.VISIBLE);
                        MultiSelectHelpDialog.this.bringToFront();
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (!show) {
                        MultiSelectHelpDialog.this.setVisibility(View.GONE);
                    }
                    MultiSelectHelpDialog.this.mAnimator = null;
                }

                public void onAnimationCancel(Animator animation) {
                    if (show) {
                        MultiSelectHelpDialog.this.setScaleX(1.0f);
                        MultiSelectHelpDialog.this.setScaleY(1.0f);
                        MultiSelectHelpDialog.this.setAlpha(1.0f);
                    }
                }
            });
            this.mAnimator.start();
            return;
        }
        setVisibility(show ? 0 : 8);
        if (show) {
            setScaleX(1.0f);
            setScaleY(1.0f);
            setAlpha(1.0f);
            bringToFront();
        }
    }

    boolean handleTouchDown(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        Rect hitRect = new Rect();
        getHitRect(hitRect);
        if (hitRect.contains(x, y)) {
            return true;
        }
        hide(true);
        this.mMultiSelectManager.setEnableHelpDialog(false);
        this.mLauncher.getDragLayer().removeView(this);
        return false;
    }
}
