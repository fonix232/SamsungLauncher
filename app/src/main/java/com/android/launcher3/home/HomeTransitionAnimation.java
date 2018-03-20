package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.multiselect.MultiSelectPanel;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.animation.LauncherViewPropertyAnimator;
import com.sec.android.app.launcher.R;
import java.util.HashMap;

class HomeTransitionAnimation {
    private float mFolderBgGrowFactor;
    private HomeController mHomeController;
    private View mHomeRootView;
    private boolean mIsRunningOverviewAnimation;
    private Launcher mLauncher;
    private float mOverviewShrinkFactor;
    private float mScreenGridShrinkFactor;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final Interpolator mSineInOut50 = ViInterpolator.getInterploator(31);
    private final Interpolator mSineInOut70 = ViInterpolator.getInterploator(33);
    private final Interpolator mSineInOut80 = ViInterpolator.getInterploator(34);
    private AnimatorSet mStageAnimator;
    private AnimatorSet mStateAnimator;
    private final Interpolator mSwipeInterpolator = new ScrollInterpolator();
    private TrayManager mTrayManager;

    private static class ScrollInterpolator implements Interpolator {
        ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((((t * t) * t) * t) * t) + 1.0f;
        }
    }

    HomeTransitionAnimation(Launcher launcher, HomeController homeController, TrayManager trayManager) {
        this.mLauncher = launcher;
        this.mHomeController = homeController;
        this.mTrayManager = trayManager;
        this.mFolderBgGrowFactor = this.mLauncher.getResources().getFraction(R.fraction.config_folderBgGrowFactor, 1, 1);
        setupShrinkFactor();
    }

    private void setupShrinkFactor() {
        this.mOverviewShrinkFactor = ((float) this.mLauncher.getResources().getInteger(R.integer.config_workspaceOverviewShrinkPercentage)) / 100.0f;
        this.mScreenGridShrinkFactor = ((float) this.mLauncher.getResources().getInteger(R.integer.config_workspaceScreenGridShrinkPercentage)) / 100.0f;
    }

    void setupView() {
        this.mHomeRootView = this.mLauncher.findViewById(R.id.home_view);
    }

    Animator getEnterFromAppsAnimation(boolean animated, @Nullable HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            boolean z;
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            if (layerViews != null) {
                layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            }
            AnimatorSet animatorSet = this.mStageAnimator;
            if (layerViews == null) {
                z = true;
            } else {
                z = false;
            }
            animateSwipeHometray(animatorSet, z, true, -1, false);
        } else {
            this.mHomeRootView.setTranslationY(0.0f);
            this.mHomeRootView.setAlpha(1.0f);
            this.mHomeRootView.setScaleX(1.0f);
            this.mHomeRootView.setScaleY(1.0f);
            View homePageIndicator = this.mHomeController.getHomePageIndicatorView();
            if (homePageIndicator != null) {
                homePageIndicator.setScaleX(1.0f);
                homePageIndicator.setScaleY(1.0f);
            }
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getEnterFromFolderAnimation(boolean animated, HashMap<View, Integer> layerViews, View anchorView) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(5, 1);
            layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            int[] iconLoc = new int[2];
            if (anchorView != null) {
                anchorView.getLocationOnScreen(iconLoc);
                if (iconLoc[0] < 0 && iconLoc[1] < 0) {
                    iconLoc[0] = this.mHomeRootView.getWidth() / 2;
                    iconLoc[1] = this.mHomeRootView.getHeight() / 2;
                }
            } else {
                iconLoc[0] = this.mHomeRootView.getWidth() / 2;
                iconLoc[1] = this.mHomeRootView.getHeight() / 2;
            }
            this.mHomeRootView.setPivotX((float) iconLoc[0]);
            this.mHomeRootView.setPivotY((float) iconLoc[1]);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.ALPHA.getName(), 0.0f, 1.0f);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            this.mStageAnimator.play(alphaAnim);
            Animator scaleXAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.SCALE_X.getName(), this.mFolderBgGrowFactor, 1.0f);
            scaleXAnim.setDuration((long) duration);
            scaleXAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleXAnim);
            Animator scaleYAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.SCALE_Y.getName(), this.mFolderBgGrowFactor, 1.0f);
            scaleYAnim.setDuration((long) duration);
            scaleYAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleYAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    HomeTransitionAnimation.this.mHomeRootView.setVisibility(View.VISIBLE);
                }

                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    HomeTransitionAnimation.this.mHomeRootView.setScaleX(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setScaleY(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setAlpha(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setPivotX(((float) HomeTransitionAnimation.this.mHomeRootView.getWidth()) / 2.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setPivotY(((float) HomeTransitionAnimation.this.mHomeRootView.getHeight()) / 2.0f);
                    HomeTransitionAnimation.this.mStageAnimator = null;
                }
            });
        } else {
            this.mHomeRootView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getEnterFromAppsPickerAnimation(boolean animated, HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        setNormalStateView(animated);
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            Animator enterAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.enter_home_from_appspicker);
            enterAnimator.setTarget(this.mHomeRootView);
            enterAnimator.addListener(new StageChangeListener(this.mHomeRootView, accessibilityEnabled, true));
            this.mStageAnimator.play(enterAnimator);
            this.mHomeRootView.setPivotX(((float) this.mHomeRootView.getWidth()) / 2.0f);
            this.mHomeRootView.setPivotY(((float) this.mHomeRootView.getHeight()) / 2.0f);
        } else {
            this.mHomeRootView.setScaleX(1.0f);
            this.mHomeRootView.setScaleY(1.0f);
            this.mHomeRootView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getEnterFromWidgetsAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean toNormal) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        if (toNormal) {
            setNormalStateView(animated);
        }
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            Animator enterAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.enter_home_from_widget);
            enterAnimator.setTarget(this.mHomeRootView);
            enterAnimator.addListener(new StageChangeListener(this.mHomeRootView, accessibilityEnabled, true));
            this.mStageAnimator.play(enterAnimator);
            this.mHomeRootView.setPivotX(((float) this.mHomeRootView.getWidth()) / 2.0f);
            this.mHomeRootView.setPivotY(((float) this.mHomeRootView.getHeight()) / 2.0f);
        } else {
            this.mHomeRootView.setTranslationY(0.0f);
            this.mHomeRootView.setScaleX(1.0f);
            this.mHomeRootView.setScaleY(1.0f);
            this.mHomeRootView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getExitToAppsAnimation(boolean animated, @Nullable HashMap<View, Integer> layerViews) {
        boolean z = true;
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            if (layerViews != null) {
                layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            }
            AnimatorSet animatorSet = this.mStageAnimator;
            if (layerViews != null) {
                z = false;
            }
            animateSwipeHometray(animatorSet, z, false, -1, false);
        } else {
            this.mHomeRootView.setTranslationY((float) this.mLauncher.getResources().getDimensionPixelSize(R.dimen.tray_slip_y_on_transition_type_3));
            this.mHomeRootView.setAlpha(0.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getExitToFolderAnimation(boolean animated, HashMap<View, Integer> layerViews, View anchorView) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(1, 5);
            layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            int[] iconLoc = new int[2];
            if (anchorView != null) {
                anchorView.getLocationOnScreen(iconLoc);
            } else {
                iconLoc[0] = this.mHomeRootView.getWidth() / 2;
                iconLoc[1] = this.mHomeRootView.getHeight() / 2;
            }
            this.mHomeRootView.setPivotX((float) iconLoc[0]);
            this.mHomeRootView.setPivotY((float) iconLoc[1]);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.ALPHA.getName(), 0.0f);
            alphaAnim.setDuration((long) (duration / 2));
            alphaAnim.setInterpolator(this.mSineInOut70);
            this.mStageAnimator.play(alphaAnim);
            Animator scaleXAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.SCALE_X.getName(), 1.0f, this.mFolderBgGrowFactor);
            scaleXAnim.setDuration((long) duration);
            scaleXAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleXAnim);
            Animator scaleYAnim = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.SCALE_Y.getName(), 1.0f, this.mFolderBgGrowFactor);
            scaleYAnim.setDuration((long) duration);
            scaleYAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleYAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    HomeTransitionAnimation.this.mHomeRootView.setVisibility(View.GONE);
                    HomeTransitionAnimation.this.mHomeRootView.setScaleX(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setScaleY(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setAlpha(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setPivotX(((float) HomeTransitionAnimation.this.mHomeRootView.getWidth()) / 2.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setPivotY(((float) HomeTransitionAnimation.this.mHomeRootView.getHeight()) / 2.0f);
                    HomeTransitionAnimation.this.mStageAnimator = null;
                }
            });
        } else {
            this.mHomeRootView.setAlpha(0.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    Animator getExitToWidgetsAnimation(boolean animated, HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(this.mHomeRootView, Integer.valueOf(1));
            Animator exitAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.exit_home_to_widget);
            exitAnimator.setTarget(this.mHomeRootView);
            exitAnimator.addListener(new StageChangeListener(this.mHomeRootView, accessibilityEnabled, false));
            this.mStageAnimator.play(exitAnimator);
            this.mHomeRootView.setPivotX(((float) this.mHomeRootView.getWidth()) / 2.0f);
            this.mHomeRootView.setPivotY(((float) this.mHomeRootView.getHeight()) / 2.0f);
        } else {
            this.mHomeRootView.setAlpha(0.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    AnimatorSet getOverviewAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean stageChanged, boolean enter) {
        final boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        final Resources res = this.mLauncher.getResources();
        final Workspace workspace = this.mHomeController.getWorkspace();
        final OverviewPanel overviewPanel = this.mHomeController.getOverviewPanel();
        final View hotseat = this.mHomeController.getHotseat();
        final PageIndicator pageIndicator = workspace.getPageIndicator();
        int duration = getStateAnimationDuration(4);
        final float workspaceShrinkFactor = enter ? this.mOverviewShrinkFactor : 1.0f;
        final float workspaceTranslationY = enter ? (float) res.getDimensionPixelSize(R.dimen.home_workspace_animate_offsetY_overview) : 0.0f;
        final float hotseatAlphaValue = enter ? 0.0f : 1.0f;
        final float overviewPanelAlphaValue = enter ? 1.0f : 0.0f;
        final float pageIndicatorTranslationY = enter ? (float) (this.mLauncher.getDeviceProfile().homeGrid.getIndicatorBottom() - (res.getDimensionPixelSize(R.dimen.home_workspace_indicator_margin_bottom_overview) - this.mLauncher.getDeviceProfile().getOffsetIndicator())) : 0.0f;
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(hotseat, Integer.valueOf(1));
            layerViews.put(overviewPanel, Integer.valueOf(1));
            Animator launcherViewPropertyAnimator = new LauncherViewPropertyAnimator(workspace);
            launcherViewPropertyAnimator.scaleX(workspaceShrinkFactor);
            launcherViewPropertyAnimator.scaleY(workspaceShrinkFactor);
            launcherViewPropertyAnimator.translationY(workspaceTranslationY);
            launcherViewPropertyAnimator.setDuration((long) duration);
            this.mStateAnimator.play(launcherViewPropertyAnimator);
            if (pageIndicator != null) {
                Animator pageIndicatorY = new LauncherViewPropertyAnimator(pageIndicator).translationY(pageIndicatorTranslationY);
                pageIndicatorY.setDuration((long) duration);
                pageIndicatorY.setInterpolator(new DecelerateInterpolator());
                this.mStateAnimator.play(pageIndicatorY);
            }
            Animator hotseatAlpha = new LauncherViewPropertyAnimator(hotseat).alpha(hotseatAlphaValue);
            hotseatAlpha.addListener(new AlphaUpdateListener(hotseat, accessibilityEnabled));
            Animator overviewPanelAlpha = new LauncherViewPropertyAnimator(overviewPanel).alpha(overviewPanelAlphaValue);
            overviewPanelAlpha.addListener(new AlphaUpdateListener(overviewPanel, accessibilityEnabled));
            if (enter) {
                hotseatAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                overviewPanelAlpha.setInterpolator(null);
            } else {
                hotseatAlpha.setInterpolator(null);
                overviewPanelAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            }
            overviewPanelAlpha.setDuration((long) duration);
            hotseatAlpha.setDuration((long) duration);
            this.mStateAnimator.play(overviewPanelAlpha);
            this.mStateAnimator.play(hotseatAlpha);
            final boolean z = enter;
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    HomeTransitionAnimation.this.mIsRunningOverviewAnimation = true;
                }

                public void onAnimationCancel(Animator animation) {
                    overviewPanel.setAlpha(overviewPanelAlphaValue);
                    hotseat.setAlpha(hotseatAlphaValue);
                    workspace.setScaleX(workspaceShrinkFactor);
                    workspace.setScaleY(workspaceShrinkFactor);
                    workspace.setTranslationY(workspaceTranslationY);
                    if (pageIndicator != null) {
                        pageIndicator.setTranslationY(pageIndicatorTranslationY);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    HomeTransitionAnimation.this.mIsRunningOverviewAnimation = false;
                    HomeTransitionAnimation.this.mStateAnimator = null;
                    if (accessibilityEnabled && z) {
                        View currentPage = workspace.getChildAt(workspace.getCurrentPage());
                        currentPage.performAccessibilityAction(64, null);
                        currentPage.announceForAccessibility(res.getString(R.string.tts_changed_to_home_screen_edit_mode) + " " + currentPage.getContentDescription());
                    }
                }
            });
            if (stageChanged) {
                animateSwipeHometray(this.mStateAnimator, false, true, -1, false);
            }
        } else {
            overviewPanel.setAlpha(overviewPanelAlphaValue);
            AlphaUpdateListener.updateVisibility(overviewPanel, accessibilityEnabled);
            hotseat.setAlpha(hotseatAlphaValue);
            AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
            workspace.setScaleX(workspaceShrinkFactor);
            workspace.setScaleY(workspaceShrinkFactor);
            workspace.setTranslationY(workspaceTranslationY);
            if (pageIndicator != null) {
                pageIndicator.setTranslationY(pageIndicatorTranslationY);
            }
            if (accessibilityEnabled && enter) {
                View currentPage = workspace.getChildAt(workspace.getCurrentPage());
                currentPage.performAccessibilityAction(64, null);
                currentPage.announceForAccessibility(res.getString(R.string.tts_changed_to_home_screen_edit_mode) + " " + currentPage.getContentDescription());
            }
        }
        changeOverviewBackground(this.mStateAnimator, workspace, animated, enter, duration);
        if (stageChanged) {
            animateExitAppsOrWidget(this.mStateAnimator, animated, accessibilityEnabled);
        }
        return this.mStateAnimator;
    }

    private void setNormalStateView(boolean animated) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        Workspace workspace = this.mHomeController.getWorkspace();
        OverviewPanel overviewPanel = this.mHomeController.getOverviewPanel();
        View hotseat = this.mHomeController.getHotseat();
        PageIndicator pageIndicator = workspace.getPageIndicator();
        int duration = getStateAnimationDuration(4);
        overviewPanel.setAlpha(0.0f);
        AlphaUpdateListener.updateVisibility(overviewPanel, accessibilityEnabled);
        hotseat.setAlpha(1.0f);
        AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
        workspace.setScaleX(1.0f);
        workspace.setScaleY(1.0f);
        workspace.setTranslationY(0.0f);
        if (pageIndicator != null) {
            pageIndicator.setTranslationY(0.0f);
        }
        if (accessibilityEnabled && overviewPanel.getVisibility() == 0) {
            overviewPanel.getChildAt(0).performAccessibilityAction(64, null);
        }
        changeOverviewBackground(this.mStateAnimator, workspace, animated, false, duration);
    }

    AnimatorSet getDragAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean enter, boolean fromWidget, boolean fromSelectState) {
        View workspace = this.mHomeController.getWorkspace();
        if (fromWidget) {
            boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
            int duration = getStateAnimationDuration(2);
            PageIndicator pageIndicator = workspace.getPageIndicator();
            cancelStateAnimation();
            if (animated) {
                this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
                Animator launcherViewPropertyAnimator = new LauncherViewPropertyAnimator(workspace);
                launcherViewPropertyAnimator.scaleX(1.0f).scaleY(1.0f).translationY(0.0f).setDuration((long) duration);
                this.mStateAnimator.play(launcherViewPropertyAnimator);
                if (pageIndicator != null) {
                    LauncherViewPropertyAnimator pageIndicatorY = new LauncherViewPropertyAnimator(pageIndicator).translationY(0.0f);
                    pageIndicatorY.setDuration((long) duration);
                    pageIndicatorY.setInterpolator(new DecelerateInterpolator());
                    this.mStateAnimator.play(pageIndicatorY);
                }
                this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        HomeTransitionAnimation.this.mStateAnimator = null;
                    }
                });
                animateSwipeHometray(this.mStateAnimator, false, true, -1, true);
            } else {
                workspace.setScaleX(1.0f);
                workspace.setScaleY(1.0f);
                workspace.setTranslationY(0.0f);
                if (pageIndicator != null) {
                    pageIndicator.setTranslationY(0.0f);
                }
            }
            changeDragBackground(workspace, enter);
            animateExitAppsOrWidget(this.mStateAnimator, animated, accessibilityEnabled);
            return this.mStateAnimator;
        }
        changeDragBackground(workspace, enter);
        if (!fromSelectState) {
            return null;
        }
        return getSelectAnimation(animated, layerViews, !enter, true);
    }

    AnimatorSet getSelectAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean enter) {
        return getSelectAnimation(animated, layerViews, enter, false);
    }

    AnimatorSet getSelectAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean enter, boolean toDragState) {
        int duration;
        final MultiSelectPanel multiSelectPanel = this.mLauncher.getMultiSelectManager().getMultiSelectPanel();
        final HomeContainer homeContainer = this.mHomeController.getHomeContainer();
        final View homePageIndicator = this.mHomeController.getHomePageIndicatorView();
        Resources res = this.mLauncher.getResources();
        int transY = res.getDimensionPixelSize(R.dimen.multi_select_panel_translation_y);
        int transYDuration = res.getInteger(R.integer.config_multiSelectTranslationYDuration);
        if (LauncherFeature.supportMultiSelectSlideVI()) {
            duration = transYDuration;
        } else {
            duration = getStateAnimationDuration(6);
        }
        float alpha = enter ? 1.0f : 0.0f;
        float scale = enter ? 1.0f : 0.95f;
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(multiSelectPanel, Integer.valueOf(1));
            Animator alphaAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.ALPHA.getName(), alpha);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            if (LauncherFeature.supportMultiSelectSlideVI()) {
                String name = View.TRANSLATION_Y.getName();
                float[] fArr = new float[2];
                fArr[0] = enter ? (float) (-transY) : 0.0f;
                fArr[1] = enter ? 0.0f : (float) (-transY);
                Animator transYAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, name, fArr);
                transYAnim.setDuration((long) duration);
                transYAnim.setInterpolator(this.mSineInOut50);
                name = View.TRANSLATION_Y.getName();
                fArr = new float[2];
                fArr[0] = enter ? 0.0f : (float) transY;
                fArr[1] = enter ? (float) transY : 0.0f;
                Animator pageIndicatorTransYAnim = LauncherAnimUtils.ofFloat(homePageIndicator, name, fArr);
                pageIndicatorTransYAnim.setDuration((long) duration);
                pageIndicatorTransYAnim.setInterpolator(this.mSineInOut50);
                name = View.TRANSLATION_Y.getName();
                fArr = new float[2];
                fArr[0] = enter ? 0.0f : (float) transY;
                fArr[1] = enter ? (float) transY : 0.0f;
                Animator homeContainerAnim = LauncherAnimUtils.ofFloat(homeContainer, name, fArr);
                homeContainerAnim.setDuration((long) duration);
                homeContainerAnim.setInterpolator(this.mSineInOut50);
                this.mStateAnimator.playTogether(new Animator[]{alphaAnim, transYAnim, pageIndicatorTransYAnim, homeContainerAnim});
            } else {
                Animator scaleXAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.SCALE_X.getName(), scale);
                scaleXAnim.setDuration((long) duration);
                scaleXAnim.setInterpolator(this.mSineInOut80);
                Animator scaleYAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.SCALE_Y.getName(), scale);
                scaleYAnim.setDuration((long) duration);
                scaleYAnim.setInterpolator(this.mSineInOut80);
                this.mStateAnimator.playTogether(new Animator[]{alphaAnim, scaleXAnim, scaleYAnim});
            }
            final boolean z = enter;
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (z) {
                        multiSelectPanel.setVisibility(View.VISIBLE);
                        multiSelectPanel.bringToFront();
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    HomeTransitionAnimation.this.mStateAnimator = null;
                    if (!z) {
                        multiSelectPanel.setVisibility(View.GONE);
                        if (LauncherFeature.supportMultiSelectSlideVI()) {
                            multiSelectPanel.setTranslationY(0.0f);
                            homePageIndicator.setTranslationY(0.0f);
                            homeContainer.setTranslationY(0.0f);
                        }
                    }
                }
            });
        } else {
            int i;
            if (LauncherFeature.supportMultiSelectSlideVI()) {
                multiSelectPanel.setTranslationY(0.0f);
                homePageIndicator.setTranslationY(enter ? (float) transY : 0.0f);
                homeContainer.setTranslationY(enter ? (float) transY : 0.0f);
            } else {
                multiSelectPanel.setScaleX(scale);
                multiSelectPanel.setScaleY(scale);
            }
            multiSelectPanel.setAlpha(alpha);
            if (enter) {
                i = 0;
            } else {
                i = 8;
            }
            multiSelectPanel.setVisibility(i);
        }
        if (!toDragState) {
            changeDragBackground(this.mHomeController.getWorkspace(), enter);
        }
        return this.mStateAnimator;
    }

    AnimatorSet getScreenGridAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean stageChanged, boolean enter, boolean toNormal) {
        int i;
        float pageIndicatorTranslationY;
        final boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        final Resources res = this.mLauncher.getResources();
        Workspace workspace = this.mHomeController.getWorkspace();
        final OverviewPanel overviewPanel = this.mHomeController.getOverviewPanel();
        View hotseat = this.mHomeController.getHotseat();
        View screengridPanel = this.mHomeController.getScreenGridPanel();
        View pageIndicator = workspace.getPageIndicator();
        final View screenGridTopContainer = screengridPanel.getScreenGridTopConatiner();
        int duration = getStateAnimationDuration(5);
        float workspaceShrinkFactor = enter ? this.mScreenGridShrinkFactor : toNormal ? 1.0f : this.mOverviewShrinkFactor;
        float workspaceTranslationY = toNormal ? 0.0f : enter ? (float) res.getDimensionPixelSize(R.dimen.home_workspace_animate_offsetY_screengrid) : (float) res.getDimensionPixelSize(R.dimen.home_workspace_animate_offsetY_overview);
        float hotseatAlphaValue = toNormal ? 1.0f : 0.0f;
        float overviewPanelAlphaValue = enter ? 0.0f : toNormal ? 0.0f : 1.0f;
        float screengridAlphaValue = enter ? 1.0f : 0.0f;
        int currentPage = workspace.getCurrentPage();
        int pageCount = workspace.getPageCount();
        int layoutTransitionOffsetForPage = workspace.getLayoutTransitionOffsetForPage(currentPage) * 2;
        if (Utilities.sIsRtl) {
            i = pageCount - currentPage;
        } else {
            i = currentPage;
        }
        int workspaceOffsetX = layoutTransitionOffsetForPage * i;
        int pageIndicatorMarginBottomNormal = this.mLauncher.getDeviceProfile().homeGrid.getIndicatorBottom();
        int pageIndicatorMarginBottomOverview = res.getDimensionPixelSize(R.dimen.home_workspace_indicator_margin_bottom_overview) - this.mLauncher.getDeviceProfile().getOffsetIndicator();
        int pageIndicatorMarginBottomScreenGrid = res.getDimensionPixelSize(R.dimen.home_workspace_indicator_margin_bottom_screengrid) - this.mLauncher.getDeviceProfile().getOffsetIndicatorForScreenGrid();
        if (toNormal) {
            pageIndicatorTranslationY = 0.0f;
        } else {
            if (!enter) {
                pageIndicatorMarginBottomScreenGrid = pageIndicatorMarginBottomOverview;
            }
            pageIndicatorTranslationY = (float) (pageIndicatorMarginBottomNormal - pageIndicatorMarginBottomScreenGrid);
        }
        cancelStateAnimation();
        if (toNormal && workspaceOffsetX != 0) {
            workspace.scrollTo(workspace.getScrollForPage(currentPage) + workspaceOffsetX, 0);
        }
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(hotseat, Integer.valueOf(1));
            layerViews.put(overviewPanel, Integer.valueOf(1));
            layerViews.put(screengridPanel, Integer.valueOf(1));
            layerViews.put(screenGridTopContainer, Integer.valueOf(1));
            Animator launcherViewPropertyAnimator = new LauncherViewPropertyAnimator(workspace);
            launcherViewPropertyAnimator.scaleX(workspaceShrinkFactor);
            launcherViewPropertyAnimator.scaleY(workspaceShrinkFactor);
            launcherViewPropertyAnimator.translationY(workspaceTranslationY);
            launcherViewPropertyAnimator.setDuration((long) duration);
            this.mStateAnimator.play(launcherViewPropertyAnimator);
            if (pageIndicator != null) {
                Animator pageIndicatorY = new LauncherViewPropertyAnimator(pageIndicator).translationY(pageIndicatorTranslationY);
                pageIndicatorY.setDuration((long) duration);
                pageIndicatorY.setInterpolator(new DecelerateInterpolator());
                this.mStateAnimator.play(pageIndicatorY);
            }
            Animator hotseatAlpha = new LauncherViewPropertyAnimator(hotseat).alpha(hotseatAlphaValue);
            hotseatAlpha.addListener(new AlphaUpdateListener(hotseat, accessibilityEnabled));
            Animator overviewPanelAlpha = new LauncherViewPropertyAnimator(overviewPanel).alpha(overviewPanelAlphaValue);
            overviewPanelAlpha.addListener(new AlphaUpdateListener(overviewPanel, accessibilityEnabled));
            Animator screengridPanelAlpha = new LauncherViewPropertyAnimator(screengridPanel).alpha(screengridAlphaValue);
            screengridPanelAlpha.addListener(new AlphaUpdateListener(screengridPanel, accessibilityEnabled));
            Animator topContainerAlpha = new LauncherViewPropertyAnimator(screenGridTopContainer).alpha(screengridAlphaValue);
            topContainerAlpha.addListener(new AlphaUpdateListener(screenGridTopContainer, accessibilityEnabled));
            if (enter) {
                hotseatAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                overviewPanelAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                screengridPanelAlpha.setInterpolator(null);
                topContainerAlpha.setInterpolator(null);
            } else {
                hotseatAlpha.setInterpolator(null);
                overviewPanelAlpha.setInterpolator(null);
                screengridPanelAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                topContainerAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            }
            overviewPanelAlpha.setDuration((long) duration);
            hotseatAlpha.setDuration((long) duration);
            screengridPanelAlpha.setDuration((long) duration);
            topContainerAlpha.setDuration((long) duration);
            this.mStateAnimator.play(overviewPanelAlpha);
            this.mStateAnimator.play(screengridPanelAlpha);
            this.mStateAnimator.play(hotseatAlpha);
            this.mStateAnimator.play(topContainerAlpha);
            final boolean z = enter;
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    HomeTransitionAnimation.this.mStateAnimator = null;
                    if (accessibilityEnabled) {
                        if (overviewPanel.getVisibility() == 0) {
                            overviewPanel.getChildAt(0).performAccessibilityAction(64, null);
                        }
                        if (z) {
                            Talk.INSTANCE.postSay(res.getString(R.string.home_screen_screengrid));
                        }
                    }
                    if (!z) {
                        screenGridTopContainer.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            overviewPanel.setAlpha(overviewPanelAlphaValue);
            AlphaUpdateListener.updateVisibility(overviewPanel, accessibilityEnabled);
            screengridPanel.setAlpha(screengridAlphaValue);
            AlphaUpdateListener.updateVisibility(screengridPanel, accessibilityEnabled);
            screenGridTopContainer.setAlpha(screengridAlphaValue);
            AlphaUpdateListener.updateVisibility(screenGridTopContainer, accessibilityEnabled);
            hotseat.setAlpha(hotseatAlphaValue);
            AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
            workspace.setTranslationY(workspaceTranslationY);
            workspace.setScaleX(workspaceShrinkFactor);
            workspace.setScaleY(workspaceShrinkFactor);
            if (pageIndicator != null) {
                pageIndicator.setTranslationY(pageIndicatorTranslationY);
            }
            if (accessibilityEnabled) {
                if (overviewPanel.getVisibility() == 0) {
                    overviewPanel.getChildAt(0).performAccessibilityAction(64, null);
                }
                if (enter) {
                    Talk.INSTANCE.postSay(res.getString(R.string.home_screen_screengrid));
                }
            }
        }
        changeScreenGridBackground(workspace, animated, enter, duration, toNormal);
        if (stageChanged) {
            animateExitAppsOrWidget(this.mStateAnimator, animated, accessibilityEnabled);
        }
        return this.mStateAnimator;
    }

    Animator getTrayReturnAnimation(boolean animated, boolean isHomeStage) {
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            boolean z = isHomeStage;
            animateSwipeHometray(this.mStageAnimator, false, z, (long) (getStageAnimationDuration(2, 1) / 2), false);
        } else {
            this.mHomeRootView.setTranslationY(0.0f);
        }
        return this.mStageAnimator;
    }

    boolean isRunningOverviewAnimation() {
        return this.mIsRunningOverviewAnimation;
    }

    private void animateSwipeHometray(AnimatorSet animatorSet, boolean byTray, boolean enter, long duration, boolean fromWidget) {
        int range;
        float toTranslationY;
        long animDuration;
        float movingDistance = 0.0f;
        if (this.mTrayManager != null) {
            range = this.mTrayManager.getTrayMovingRange();
            movingDistance = this.mTrayManager.getTrayMovingDistance();
        } else {
            range = Utilities.getFullScreenHeight(this.mLauncher);
        }
        if (enter) {
            toTranslationY = 0.0f;
        } else if (this.mHomeRootView.getTranslationY() > 0.0f) {
            toTranslationY = (float) (range - 0);
        } else {
            toTranslationY = (float) ((-range) + 0);
        }
        if (duration < 0) {
            float abs = (range == 0 || movingDistance == 0.0f) ? 0.0f : enter ? Math.abs(movingDistance / ((float) range)) : 1.0f - Math.abs(movingDistance / ((float) range));
            animDuration = (long) getSwipeAnimationDuration(byTray, enter, abs);
        } else {
            animDuration = duration;
        }
        Animator homeTranslate = LauncherAnimUtils.ofFloat(this.mHomeRootView, View.TRANSLATION_Y.getName(), toTranslationY);
        homeTranslate.setDuration(animDuration);
        homeTranslate.setInterpolator(this.mSwipeInterpolator);
        final boolean z = fromWidget;
        final boolean z2 = enter;
        homeTranslate.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (HomeTransitionAnimation.this.mTrayManager != null) {
                    HomeTransitionAnimation.this.mTrayManager.setDisallowCallBacksVisibity(z);
                    HomeTransitionAnimation.this.mTrayManager.setSwipeAnimationStatus(true, toTranslationY, animDuration);
                    HomeTransitionAnimation.this.mHomeController.resetBlurRunnable();
                }
            }

            public void onAnimationEnd(Animator animation) {
                if (z2) {
                    HomeTransitionAnimation.this.mHomeRootView.setTranslationY(0.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setAlpha(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setScaleX(1.0f);
                    HomeTransitionAnimation.this.mHomeRootView.setScaleY(1.0f);
                }
                if (HomeTransitionAnimation.this.mTrayManager != null) {
                    HomeTransitionAnimation.this.mTrayManager.setDisallowCallBacksVisibity(false);
                    HomeTransitionAnimation.this.mTrayManager.setSwipeAnimationStatus(false, toTranslationY, animDuration);
                }
            }
        });
        animatorSet.play(homeTranslate);
    }

    private int getSwipeAnimationDuration(boolean byTray, boolean enter, float movingDistance) {
        int duration;
        if (enter) {
            duration = getStageAnimationDuration(2, 1);
        } else {
            duration = getStageAnimationDuration(1, 2);
        }
        if (!byTray || this.mTrayManager == null) {
            return duration;
        }
        if (movingDistance == 0.0f) {
            return 0;
        }
        return this.mTrayManager.calculateDuration(movingDistance, duration);
    }

    private int getStageAnimationDuration(int fromStage, int toStage) {
        Resources res = this.mLauncher.getResources();
        if (fromStage == 1) {
            if (toStage == 5) {
                return res.getInteger(R.integer.config_folderBgGlowDuration);
            }
            if (toStage == 2) {
                return res.getInteger(R.integer.config_enterAppsDuration);
            }
        } else if (fromStage == 2) {
            if (toStage == 1) {
                return res.getInteger(R.integer.config_enterAppsDuration);
            }
        } else if (fromStage == 5 && toStage == 1) {
            return res.getInteger(R.integer.config_folderCloseDuration);
        }
        return 0;
    }

    private int getStateAnimationDuration(int toState) {
        Resources res = this.mLauncher.getResources();
        if (toState == 4) {
            return res.getInteger(R.integer.config_overviewTransitionDuration);
        }
        if (toState == 2) {
            return res.getInteger(R.integer.config_homeDragTransitionDuration);
        }
        if (toState == 5) {
            return res.getInteger(R.integer.config_homeScreenGridTransitionDuration);
        }
        if (toState == 6) {
            return res.getInteger(R.integer.config_multiSelectTransitionDuration);
        }
        return 0;
    }

    private void cancelStageAnimation() {
        if (this.mStageAnimator != null) {
            this.mStageAnimator.setDuration(0);
            this.mStageAnimator.cancel();
        }
        this.mStageAnimator = null;
    }

    private void cancelStateAnimation() {
        if (this.mStateAnimator != null) {
            this.mStateAnimator.setDuration(0);
            this.mStateAnimator.cancel();
        }
        this.mStateAnimator = null;
    }

    private void animateExitAppsOrWidget(AnimatorSet animatorSet, boolean animated, boolean accessibilityEnabled) {
        this.mHomeController.getOverviewPanel().setVisibility(4);
        Hotseat hotseat = this.mHomeController.getHotseat();
        hotseat.setAlpha(1.0f);
        AlphaUpdateListener.updateVisibility(hotseat, accessibilityEnabled);
        if (!animated || animatorSet == null) {
            this.mHomeRootView.setScaleX(1.0f);
            this.mHomeRootView.setScaleY(1.0f);
            this.mHomeRootView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mHomeRootView, accessibilityEnabled);
            return;
        }
        LauncherViewPropertyAnimator exitAnimator = new LauncherViewPropertyAnimator(this.mHomeRootView).alpha(1.0f);
        exitAnimator.setDuration((long) getStageAnimationDuration(1, 2));
        exitAnimator.addListener(new AlphaUpdateListener(this.mHomeRootView, accessibilityEnabled));
        this.mHomeRootView.setScaleX(1.0f);
        this.mHomeRootView.setScaleY(1.0f);
        animatorSet.play(exitAnimator);
    }

    private void changeOverviewBackground(AnimatorSet animatorSet, Workspace workspace, boolean animated, boolean enter, int duration) {
        float backgroundAlpha = enter ? 1.0f : 0.0f;
        int childCount = workspace.getChildCount();
        int currentPage = workspace.getCurrentPage();
        int defaultPage = workspace.getDefaultPage();
        int i = 0;
        while (i < childCount) {
            long screenId = workspace.getScreenIdForPageIndex(i);
            if (!(screenId == -401 || screenId == -301 || screenId == -501)) {
                final WorkspaceCellLayout cl = (WorkspaceCellLayout) workspace.getChildAt(i);
                if (!animated || animatorSet == null) {
                    if (enter) {
                        cl.setBgImage(4, i == defaultPage);
                    } else {
                        cl.setBgImage(1, false);
                    }
                    cl.setBackgroundAlpha(backgroundAlpha);
                } else {
                    if (enter) {
                        cl.setBgImage(4, i == defaultPage);
                        if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                            cl.setBackgroundAlpha(backgroundAlpha);
                        }
                    } else if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                        cl.setBgImage(1, false);
                        cl.setBackgroundAlpha(backgroundAlpha);
                    }
                    if (i == currentPage || i == currentPage - 1 || i == currentPage + 1) {
                        ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha", new float[]{cl.getBackgroundAlpha(), backgroundAlpha});
                        bgAnim.setDuration((long) duration);
                        final boolean z = enter;
                        bgAnim.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                if (!z && cl != null) {
                                    cl.setBgImage(1, false);
                                }
                            }
                        });
                        animatorSet.play(bgAnim);
                    }
                }
            }
            i++;
        }
    }

    private void changeDragBackground(Workspace workspace, boolean enter) {
        float backgroundAlpha;
        if (enter) {
            backgroundAlpha = 1.0f;
        } else {
            backgroundAlpha = 0.0f;
        }
        int childCount = workspace.getChildCount();
        int currentPage = workspace.getCurrentPage();
        int i = 0;
        while (i < childCount) {
            WorkspaceCellLayout cl = (WorkspaceCellLayout) workspace.getChildAt(i);
            if (enter) {
                cl.setBgImage(2, false);
            } else {
                cl.setBgImage(1, false);
            }
            if (i == currentPage || workspace.getScreenIdForPageIndex(i) == -201) {
                cl.setBackgroundAlpha(0.0f);
            } else {
                cl.setBackgroundAlpha(backgroundAlpha);
            }
            i++;
        }
    }

    private void changeScreenGridBackground(Workspace workspace, boolean animated, boolean enter, int duration, boolean toNormal) {
        float backgroundAlpha = enter ? 1.0f : toNormal ? 0.0f : 1.0f;
        int childCount = workspace.getChildCount();
        int currentPage = workspace.getCurrentPage();
        int defaultPage = workspace.getDefaultPage();
        int i = 0;
        while (i < childCount) {
            long screenId = workspace.getScreenIdForPageIndex(i);
            if (!(screenId == -401 || screenId == -301 || screenId == -501)) {
                final WorkspaceCellLayout cl = (WorkspaceCellLayout) workspace.getChildAt(i);
                if (animated) {
                    if (enter) {
                        cl.setBgImage(5, i == defaultPage);
                        if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                            cl.setBackgroundAlpha(backgroundAlpha);
                        }
                    } else if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                        if (toNormal) {
                            cl.setBgImage(1, false);
                        } else {
                            cl.setBgImage(4, i == defaultPage);
                        }
                        cl.setBackgroundAlpha(backgroundAlpha);
                    }
                    if (i == currentPage || i == currentPage - 1 || i == currentPage + 1) {
                        ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha", new float[]{cl.getBackgroundAlpha(), backgroundAlpha});
                        bgAnim.setDuration((long) duration);
                        final boolean z = enter;
                        final boolean z2 = toNormal;
                        bgAnim.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                if (!z && z2) {
                                    cl.setBgImage(1, false);
                                }
                            }
                        });
                        this.mStateAnimator.play(bgAnim);
                    }
                } else {
                    if (enter) {
                        cl.setBgImage(5, i == defaultPage);
                    } else if (toNormal) {
                        cl.setBgImage(1, false);
                    } else {
                        cl.setBgImage(4, i == defaultPage);
                    }
                    cl.setBackgroundAlpha(backgroundAlpha);
                }
            }
            i++;
        }
    }

    void onConfigurationChangedIfNeeded() {
        setupShrinkFactor();
    }
}
