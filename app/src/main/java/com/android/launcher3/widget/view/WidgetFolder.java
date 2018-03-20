package com.android.launcher3.widget.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.DragLayer.LayoutParams;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WidgetFolder extends LinearLayout {
    private static final String TAG = "WidgetFolder";
    private long mAnimDuration;
    private FakeAnchorViewAnim mIconAnim;
    private FrameLayout mIndicatorWrapper;
    private Launcher mLauncher;
    private int mPageIndicatorHeight;
    private WidgetFolderPagedView mPagedView;
    private AnimatorSet mStageAnimator;
    private TextView mTitle;
    private int mTitleHight;

    private static class AnimationInfo {
        int[] location = new int[2];
        float[] scaleBy = new float[2];

        public AnimationInfo() {
            this.location[0] = 0;
            this.location[1] = 0;
            this.scaleBy[0] = 1.0f;
            this.scaleBy[1] = 1.0f;
        }
    }

    private class FakeAnchorViewAnim {
        ImageView mAnimView;

        private FakeAnchorViewAnim() {
        }

        public void prepareAimation(View anchorView, AnimationInfo info, boolean open) {
            if (this.mAnimView == null) {
                this.mAnimView = new ImageView(WidgetFolder.this.getContext());
                prepareFakeAnchorView(anchorView);
                ((DragLayer) WidgetFolder.this.getParent()).addView(this.mAnimView, makeLayoutParams());
            } else {
                prepareFakeAnchorView(anchorView);
                this.mAnimView.setLayoutParams(makeLayoutParams());
            }
            if (open) {
                this.mAnimView.setTranslationX((float) info.location[0]);
                this.mAnimView.setTranslationY((float) info.location[1]);
                this.mAnimView.setScaleX(info.scaleBy[0]);
                this.mAnimView.setScaleY(info.scaleBy[1]);
            }
        }

        public void animateOpen(final View anchorView, AnimatorSet animSet, AnimationInfo info, HashMap<View, Integer> layerViews) {
            layerViews.put(this.mAnimView, Integer.valueOf(1));
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAnimView, "alpha", 0.0f);
            alphaAnim.setDuration(WidgetFolder.this.mAnimDuration / 2);
            alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
            animSet.play(alphaAnim);
            AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
            Animator[] animatorArr = new Animator[4];
            animatorArr[0] = LauncherAnimUtils.ofFloat(this.mAnimView, "scaleX", 1.0f);
            animatorArr[1] = LauncherAnimUtils.ofFloat(this.mAnimView, "scaleY", 1.0f);
            animatorArr[2] = LauncherAnimUtils.ofFloat(this.mAnimView, "translationX", 0.0f);
            animatorArr[3] = LauncherAnimUtils.ofFloat(this.mAnimView, "translationY", 0.0f);
            scaleAnimSet.playTogether(animatorArr);
            scaleAnimSet.setDuration(WidgetFolder.this.mAnimDuration);
            scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(35));
            animSet.play(scaleAnimSet);
            anchorView.setAlpha(0.0f);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    anchorView.setAlpha(1.0f);
                }
            });
        }

        public void animateClose(final View anchorView, AnimatorSet animSet, AnimationInfo info, HashMap<View, Integer> layerViews) {
            layerViews.put(this.mAnimView, Integer.valueOf(1));
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAnimView, "alpha", 1.0f);
            alphaAnim.setDuration(WidgetFolder.this.mAnimDuration);
            alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
            animSet.play(alphaAnim);
            AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
            Animator[] animatorArr = new Animator[4];
            animatorArr[0] = LauncherAnimUtils.ofFloat(this.mAnimView, "scaleX", info.scaleBy[0]);
            animatorArr[1] = LauncherAnimUtils.ofFloat(this.mAnimView, "scaleY", info.scaleBy[1]);
            animatorArr[2] = LauncherAnimUtils.ofFloat(this.mAnimView, "translationX", (float) info.location[0]);
            animatorArr[3] = LauncherAnimUtils.ofFloat(this.mAnimView, "translationY", (float) info.location[1]);
            scaleAnimSet.playTogether(animatorArr);
            scaleAnimSet.setDuration(WidgetFolder.this.mAnimDuration);
            scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(35));
            animSet.play(scaleAnimSet);
            anchorView.setVisibility(4);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    anchorView.setVisibility(View.VISIBLE);
                    WidgetFolder.this.mLauncher.getDragLayer().removeView(FakeAnchorViewAnim.this.mAnimView);
                }
            });
        }

        private void prepareFakeAnchorView(View anchorView) {
            Bitmap b = Bitmap.createBitmap(anchorView.getWidth(), anchorView.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            anchorView.draw(canvas);
            canvas.setBitmap(null);
            this.mAnimView.setImageBitmap(b);
            this.mAnimView.setScaleType(ScaleType.FIT_XY);
        }

        private LayoutParams makeLayoutParams() {
            LayoutParams params = (LayoutParams) WidgetFolder.this.getLayoutParams();
            LayoutParams newParams = new LayoutParams(0, 0);
            newParams.width = params.width;
            newParams.height = params.height;
            newParams.leftMargin = params.leftMargin;
            newParams.topMargin = params.topMargin;
            newParams.setMarginStart(params.x);
            return newParams;
        }

        public void removeView() {
            if (this.mAnimView != null) {
                WidgetFolder.this.mLauncher.getDragLayer().removeView(this.mAnimView);
            }
        }
    }

    public WidgetFolder(Context context) {
        this(context, null);
    }

    public WidgetFolder(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetFolder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLauncher = (Launcher) getContext();
        this.mTitle = (TextView) findViewById(R.id.widget_folder_title);
        if (LauncherFeature.supportNewWidgetList()) {
            this.mTitle.setGravity(17);
            this.mTitle.setPaddingRelative(0, 0, 0, 0);
        } else {
            this.mTitle.setGravity(16);
            this.mTitle.setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.widget_folder_title_padding_start), 0, 0, 0);
        }
        this.mPagedView = (WidgetFolderPagedView) findViewById(R.id.widget_folder_pagedview);
        this.mIndicatorWrapper = (FrameLayout) findViewById(R.id.indicator_wrapper);
        this.mPageIndicatorHeight = getPageIndicatorHeight();
        this.mTitleHight = getTitleBarHeight();
        this.mIconAnim = new FakeAnchorViewAnim();
        this.mAnimDuration = (long) getResources().getInteger(R.integer.config_widgetFolderTransitionDuration);
    }

    public AnimatorSet open(View anchorView, boolean animate, HashMap<View, Integer> layerViews) {
        cleanupAnimation();
        this.mLauncher.getDragLayer().addView(this);
        setItemsAndBind(anchorView);
        Log.d(TAG, "open anchorview : " + (anchorView != null));
        centerAboutIcon();
        if (animate && anchorView != null) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            AnimationInfo info = getAnimationInfo(anchorView);
            prepareOpenAnimation(info);
            animateOpen(this.mStageAnimator, layerViews);
            this.mIconAnim.prepareAimation(anchorView, info, true);
            this.mIconAnim.animateOpen(anchorView, this.mStageAnimator, info, layerViews);
        }
        setVisibility(View.VISIBLE);
        post(new Runnable() {
            public void run() {
                if (WidgetFolder.this.mPagedView != null) {
                    WidgetPageLayout page = WidgetFolder.this.mPagedView.getPageAt(0);
                    if (page != null && page.getChildAt(0) != null) {
                        page.getChildAt(0).requestFocus();
                    }
                }
            }
        });
        if (!(!Talk.INSTANCE.isAccessibilityEnabled() || TestHelper.isRoboUnitTest() || this.mStageAnimator == null)) {
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    Talk.INSTANCE.say(WidgetFolder.this.mLauncher.getResources().getString(R.string.folder_opened) + " " + WidgetFolder.this.mPagedView.getCurrentPageDescription());
                }
            });
        }
        return this.mStageAnimator;
    }

    public AnimatorSet close(View anchorView, boolean animate, HashMap<View, Integer> layerViews) {
        cleanupAnimation();
        Log.d(TAG, "close anchorview : " + (anchorView != null));
        if (!animate || anchorView == null) {
            setVisibility(View.GONE);
            ((DragLayer) getParent()).removeView(this);
            this.mIconAnim.removeView();
        } else {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            AnimationInfo info = getAnimationInfo(anchorView);
            animateClose(this.mStageAnimator, info, layerViews);
            this.mIconAnim.prepareAimation(anchorView, info, false);
            this.mIconAnim.animateClose(anchorView, this.mStageAnimator, info, layerViews);
        }
        return this.mStageAnimator;
    }

    private void cleanupAnimation() {
        if (this.mStageAnimator != null) {
            this.mStageAnimator.setDuration(0);
            this.mStageAnimator.cancel();
        }
        this.mStageAnimator = null;
    }

    private void animateClose(AnimatorSet animSet, AnimationInfo info, HashMap<View, Integer> layerViews) {
        layerViews.put(this, Integer.valueOf(1));
        Animator alphaAnim = LauncherAnimUtils.ofFloat(this, "alpha", 0.0f);
        alphaAnim.setDuration(this.mAnimDuration / 2);
        alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
        animSet.play(alphaAnim);
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        Animator[] animatorArr = new Animator[4];
        animatorArr[0] = LauncherAnimUtils.ofFloat(this, "scaleX", info.scaleBy[0]);
        animatorArr[1] = LauncherAnimUtils.ofFloat(this, "scaleY", info.scaleBy[1]);
        animatorArr[2] = LauncherAnimUtils.ofFloat(this, "translationX", (float) info.location[0]);
        animatorArr[3] = LauncherAnimUtils.ofFloat(this, "translationY", (float) info.location[1]);
        scaleAnimSet.playTogether(animatorArr);
        scaleAnimSet.setDuration(this.mAnimDuration);
        scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(35));
        animSet.play(scaleAnimSet);
        animSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                animationComplete();
            }

            private void animationComplete() {
                WidgetFolder.this.mLauncher.getDragLayer().removeView(WidgetFolder.this);
            }
        });
    }

    private void animateOpen(AnimatorSet animSet, HashMap<View, Integer> layerViews) {
        layerViews.put(this, Integer.valueOf(1));
        Animator alphaAnim = LauncherAnimUtils.ofFloat(this, "alpha", 1.0f);
        alphaAnim.setDuration(this.mAnimDuration);
        alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
        animSet.play(alphaAnim);
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        Animator[] animatorArr = new Animator[4];
        animatorArr[0] = LauncherAnimUtils.ofFloat(this, "scaleX", 1.0f);
        animatorArr[1] = LauncherAnimUtils.ofFloat(this, "scaleY", 1.0f);
        animatorArr[2] = LauncherAnimUtils.ofFloat(this, "translationX", 0.0f);
        animatorArr[3] = LauncherAnimUtils.ofFloat(this, "translationY", 0.0f);
        scaleAnimSet.playTogether(animatorArr);
        scaleAnimSet.setDuration(this.mAnimDuration);
        scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(35));
        animSet.play(scaleAnimSet);
        animSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
            }
        });
    }

    private void prepareOpenAnimation(AnimationInfo info) {
        setTranslationX((float) info.location[0]);
        setTranslationY((float) info.location[1]);
        setScaleX(info.scaleBy[0]);
        setScaleY(info.scaleBy[1]);
        setAlpha(0.0f);
    }

    private AnimationInfo getAnimationInfo(View anchorView) {
        AnimationInfo folderAnimInfo = new AnimationInfo();
        if (anchorView != null) {
            int[] iconLocation = new int[2];
            int[] folderLocation = new int[2];
            anchorView.getLocationOnScreen(iconLocation);
            LayoutParams lp = (LayoutParams) getLayoutParams();
            ((View) getParent()).getLocationOnScreen(folderLocation);
            folderLocation[0] = folderLocation[0] + lp.x;
            folderLocation[1] = folderLocation[1] + lp.y;
            int startY = (int) (((float) (iconLocation[1] - folderLocation[1])) - (((float) (lp.height - anchorView.getHeight())) / 2.0f));
            folderAnimInfo.location[0] = (int) (((float) (iconLocation[0] - folderLocation[0])) - (((float) (lp.width - anchorView.getWidth())) / 2.0f));
            folderAnimInfo.location[1] = startY;
            folderAnimInfo.scaleBy[0] = (((float) anchorView.getWidth()) * 1.0f) / ((float) lp.width);
            folderAnimInfo.scaleBy[1] = (((float) anchorView.getHeight()) * 1.0f) / ((float) lp.height);
        }
        return folderAnimInfo;
    }

    private boolean setItemsAndBind(View anchorView) {
        if (anchorView instanceof WidgetItemView) {
            List<PendingAddItemInfo> items = new ArrayList(((WidgetItemView) anchorView).getWidgets());
            List<Object> widgetItems = new ArrayList();
            for (PendingAddItemInfo item : items) {
                List<PendingAddItemInfo> temp = new ArrayList();
                temp.add(item);
                widgetItems.add(temp);
            }
            setTitle(((PendingAddItemInfo) items.get(0)).getApplicationLabel());
            this.mPagedView.setWidgetItems(widgetItems);
            this.mPagedView.setDataReady();
            this.mPagedView.prepareInOut(1, true);
            this.mIndicatorWrapper.setVisibility(View.VISIBLE);
            return true;
        }
        Log.w(TAG, "no anchorView for bind");
        return false;
    }

    private void setTitle(String title) {
        this.mTitle.setText(title);
    }

    public void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeTextColorForBg(getContext(), this.mTitle, whiteBg);
    }

    public void centerAboutIcon() {
        Point displaySize = new Point();
        Utilities.getScreenSize(getContext(), displaySize);
        LayoutParams lp = new LayoutParams(0, 0);
        measure(MeasureSpec.makeMeasureSpec(displaySize.x, 1073741824), 0);
        int width = getMeasuredWidth();
        int height = getFolderHeight();
        int top = (displaySize.y - height) / 2;
        int left = (displaySize.x - width) / 2;
        lp.width = width;
        lp.height = height;
        lp.x = left;
        lp.y = top;
        lp.topMargin = top;
        lp.setMarginStart(left);
        lp.customPosition = true;
        setLayoutParams(lp);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int contentHeight = getContentAreaHeight();
        int contentAreaWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, 1073741824);
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(contentHeight, 1073741824);
        this.mTitle.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(this.mTitleHight, 1073741824));
        this.mPagedView.measure(contentAreaWidthSpec, contentAreaHeightSpec);
        this.mIndicatorWrapper.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(this.mPageIndicatorHeight, 1073741824));
        setMeasuredDimension(contentWidth, getFolderHeight(contentHeight));
    }

    public void onConfigurationChangedIfNeeded() {
        this.mPagedView.onConfigurationChangedIfNeeded();
        ((LinearLayout.LayoutParams) this.mTitle.getLayoutParams()).height = getResources().getDimensionPixelSize(R.dimen.widget_folder_title_height);
        if (!LauncherFeature.supportNewWidgetList()) {
            this.mTitle.setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.widget_folder_title_padding_start), 0, 0, 0);
        }
        this.mTitleHight = getTitleBarHeight();
        ((LinearLayout.LayoutParams) this.mIndicatorWrapper.getLayoutParams()).height = getResources().getDimensionPixelSize(R.dimen.widget_folder_pageindicator_container_height);
        this.mPageIndicatorHeight = getPageIndicatorHeight();
        centerAboutIcon();
    }

    private int getContentAreaHeight() {
        return this.mPagedView.getDesiredHeight();
    }

    private int getFolderHeight() {
        return getFolderHeight(getContentAreaHeight());
    }

    private int getFolderHeight(int contentAreaHeight) {
        return (((getPaddingTop() + getPaddingBottom()) + contentAreaHeight) + this.mTitleHight) + this.mPageIndicatorHeight;
    }

    private int getPageIndicatorHeight() {
        return (this.mIndicatorWrapper.getPaddingBottom() + this.mIndicatorWrapper.getPaddingTop()) + getResources().getDimensionPixelSize(R.dimen.widget_folder_pageindicator_container_height);
    }

    private int getTitleBarHeight() {
        return (this.mTitle.getPaddingTop() + this.mTitle.getPaddingBottom()) + getResources().getDimensionPixelSize(R.dimen.widget_folder_title_height);
    }
}
