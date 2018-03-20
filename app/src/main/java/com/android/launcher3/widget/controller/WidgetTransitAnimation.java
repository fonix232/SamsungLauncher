package com.android.launcher3.widget.controller;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;

public class WidgetTransitAnimation {
    private Resources mResources;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final Interpolator mSineInOut90 = ViInterpolator.getInterploator(35);
    private AnimatorSet mStageAnimator;
    private final View mWidgetView;

    public WidgetTransitAnimation(View view) {
        this.mWidgetView = view;
        this.mResources = view.getResources();
    }

    public AnimatorSet getEnterWidgetAnimation(boolean animated, StageEntry data) {
        cleanupAnimation();
        this.mWidgetView.setVisibility(View.VISIBLE);
        int fromStage = data.fromStage;
        boolean fromSetting = ((Boolean) data.getExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(false))).booleanValue();
        if (animated) {
            data.getLayerViews().put(this.mWidgetView, Integer.valueOf(1));
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            long duration = getAnimDuration(data.toStage, data.fromStage);
            if (fromStage == 4) {
                getEnterWidgetAnimFromFolder(this.mStageAnimator, (View) data.getExtras(WidgetController.KEY_WIDGET_FOLDER_ICON), duration);
            } else if (fromStage != 1 || fromSetting) {
                getEnterWidgetDefaultAnim(this.mStageAnimator, duration);
            } else {
                getEnterWidgetAnimFromHome(this.mStageAnimator, duration);
            }
        }
        return this.mStageAnimator;
    }

    public AnimatorSet getExitWidgetAnimation(boolean animated, StageEntry data) {
        cleanupAnimation();
        int toStage = data.toStage;
        boolean toSetting = ((Boolean) data.getExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(false))).booleanValue();
        if (animated) {
            data.getLayerViews().put(this.mWidgetView, Integer.valueOf(1));
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            long duration = getAnimDuration(data.toStage, data.fromStage);
            if (toStage == 4) {
                getExitWidgetAnimToFolder(this.mStageAnimator, duration, (View) data.getExtras(WidgetController.KEY_WIDGET_FOLDER_ICON));
            } else if (toStage != 1 || toSetting) {
                getExitWidgetDefaultAnim(this.mStageAnimator, duration);
            } else {
                getExitWidgetAnimToHome(this.mStageAnimator, duration);
            }
        } else if (toStage == 4) {
            this.mWidgetView.setVisibility(4);
        } else {
            this.mWidgetView.setVisibility(View.GONE);
        }
        return this.mStageAnimator;
    }

    private void getEnterWidgetAnimFromFolder(AnimatorSet animSet, View anchorView, long duration) {
        if (anchorView != null) {
            int[] iconLoc = new int[2];
            anchorView.getLocationOnScreen(iconLoc);
            this.mWidgetView.setPivotX((float) iconLoc[0]);
            this.mWidgetView.setPivotY((float) iconLoc[1]);
            this.mWidgetView.setAlpha(0.0f);
            this.mWidgetView.setScaleX(1.5f);
            this.mWidgetView.setScaleY(1.5f);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mWidgetView, "alpha", 1.0f);
            alphaAnim.setDuration(duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            animSet.play(alphaAnim);
            AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
            r3 = new Animator[2];
            r3[0] = LauncherAnimUtils.ofFloat(this.mWidgetView, "scaleX", 1.0f);
            r3[1] = LauncherAnimUtils.ofFloat(this.mWidgetView, "scaleY", 1.0f);
            scaleAnimSet.playTogether(r3);
            scaleAnimSet.setDuration(duration);
            scaleAnimSet.setInterpolator(this.mSineInOut90);
            animSet.play(scaleAnimSet);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    WidgetTransitAnimation.this.mWidgetView.setScaleX(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setScaleY(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setAlpha(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
                    WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) / 2.0f);
                    WidgetTransitAnimation.this.mStageAnimator = null;
                }
            });
        }
    }

    private void getExitWidgetAnimToFolder(AnimatorSet animSet, long duration, View anchorView) {
        if (anchorView != null) {
            int[] iconLoc = new int[2];
            anchorView.getLocationOnScreen(iconLoc);
            this.mWidgetView.setPivotX((float) iconLoc[0]);
            this.mWidgetView.setPivotY((float) iconLoc[1]);
            this.mWidgetView.setScaleX(1.0f);
            this.mWidgetView.setScaleY(1.0f);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mWidgetView, "alpha", 0.0f);
            alphaAnim.setDuration(duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            animSet.play(alphaAnim);
            AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
            r5 = new Animator[2];
            r5[0] = LauncherAnimUtils.ofFloat(this.mWidgetView, "scaleX", 1.5f);
            r5[1] = LauncherAnimUtils.ofFloat(this.mWidgetView, "scaleY", 1.5f);
            scaleAnimSet.playTogether(r5);
            scaleAnimSet.setDuration(duration);
            scaleAnimSet.setInterpolator(this.mSineInOut90);
            animSet.play(scaleAnimSet);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    WidgetTransitAnimation.this.mWidgetView.setScaleX(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setScaleY(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setAlpha(1.0f);
                    WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
                    WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) / 2.0f);
                    WidgetTransitAnimation.this.mWidgetView.setVisibility(4);
                    WidgetTransitAnimation.this.mStageAnimator = null;
                }
            });
        }
    }

    private void cleanupAnimation() {
        if (this.mStageAnimator != null) {
            this.mStageAnimator.setDuration(0);
            this.mStageAnimator.cancel();
        }
        this.mStageAnimator = null;
    }

    private void getEnterWidgetAnimFromHome(AnimatorSet animSet, long duration) {
        this.mWidgetView.setAlpha(0.0f);
        Animator enterAnimator = AnimatorInflater.loadAnimator(this.mWidgetView.getContext(), R.animator.enter_widget_from_home);
        enterAnimator.setDuration(duration);
        enterAnimator.setTarget(this.mWidgetView);
        enterAnimator.setInterpolator(this.mSineInOut90);
        animSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) * 1.0f);
                WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
            }

            public void onAnimationEnd(Animator animation) {
                animationComplete();
            }

            private void animationComplete() {
                WidgetTransitAnimation.this.mWidgetView.setScaleX(1.0f);
                WidgetTransitAnimation.this.mWidgetView.setScaleY(1.0f);
                WidgetTransitAnimation.this.mWidgetView.setAlpha(1.0f);
                WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
                WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) / 2.0f);
                WidgetTransitAnimation.this.mStageAnimator = null;
            }
        });
        animSet.play(enterAnimator);
    }

    private void getExitWidgetAnimToHome(AnimatorSet animSet, long duration) {
        Animator exitAnimator = AnimatorInflater.loadAnimator(this.mWidgetView.getContext(), R.animator.exit_widget_to_home);
        exitAnimator.setDuration(duration);
        exitAnimator.setTarget(this.mWidgetView);
        exitAnimator.setInterpolator(this.mSineInOut90);
        exitAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) * 1.0f);
                WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
            }

            public void onAnimationEnd(Animator animation) {
                animationComplete();
            }

            private void animationComplete() {
                WidgetTransitAnimation.this.mWidgetView.setPivotX(((float) WidgetTransitAnimation.this.mWidgetView.getWidth()) / 2.0f);
                WidgetTransitAnimation.this.mWidgetView.setPivotY(((float) WidgetTransitAnimation.this.mWidgetView.getHeight()) / 2.0f);
                WidgetTransitAnimation.this.mWidgetView.setVisibility(View.GONE);
                WidgetTransitAnimation.this.mStageAnimator = null;
            }
        });
        animSet.play(exitAnimator);
    }

    private void getEnterWidgetDefaultAnim(AnimatorSet animSet, long duration) {
        this.mWidgetView.setAlpha(0.0f);
        Animator enterAnimator = AnimatorInflater.loadAnimator(this.mWidgetView.getContext(), R.animator.enter_widget);
        enterAnimator.setDuration(duration);
        enterAnimator.setTarget(this.mWidgetView);
        animSet.play(enterAnimator);
        animSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                animationComplete();
            }

            private void animationComplete() {
                WidgetTransitAnimation.this.mWidgetView.setScaleX(1.0f);
                WidgetTransitAnimation.this.mWidgetView.setScaleY(1.0f);
                WidgetTransitAnimation.this.mWidgetView.setAlpha(1.0f);
                WidgetTransitAnimation.this.mStageAnimator = null;
            }
        });
    }

    private void getExitWidgetDefaultAnim(AnimatorSet animSet, long duration) {
        Animator exitAnimator = AnimatorInflater.loadAnimator(this.mWidgetView.getContext(), R.animator.exit_widget);
        exitAnimator.setDuration(duration);
        exitAnimator.setTarget(this.mWidgetView);
        exitAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                animationComplete();
            }

            private void animationComplete() {
                WidgetTransitAnimation.this.mWidgetView.setVisibility(View.GONE);
                WidgetTransitAnimation.this.mStageAnimator = null;
            }
        });
        animSet.play(exitAnimator);
    }

    private long getAnimDuration(int toStage, int fromStage) {
        if (toStage == 4 || fromStage == 4) {
            return (long) this.mResources.getInteger(R.integer.config_widgetFolderTransitionDuration);
        }
        return (long) this.mResources.getInteger(R.integer.config_widgetTransitionDuration);
    }
}
