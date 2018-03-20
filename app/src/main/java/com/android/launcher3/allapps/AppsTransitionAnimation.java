package com.android.launcher3.allapps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.view.AppsPagedView;
import com.android.launcher3.allapps.view.AppsViewCellLayout;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.multiselect.MultiSelectPanel;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.animation.LauncherViewPropertyAnimator;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AppsTransitionAnimation {
    private final String TAG = "AppsTransitionAnimation";
    private final ViewGroup mAppsContentView;
    private final AppsController mAppsController;
    private final View mAppsPageIndicatorView;
    private final AppsPagedView mAppsPagedView;
    private final float mFolderBgGrowFactor;
    private final Launcher mLauncher;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final Interpolator mSineInOut70 = ViInterpolator.getInterploator(33);
    private final Interpolator mSineInOut80 = ViInterpolator.getInterploator(34);
    private AnimatorSet mStageAnimator;
    private AnimatorSet mStateAnimator;
    private final TrayManager mTrayManager;

    public AppsTransitionAnimation(Launcher launcher, AppsController appsController, TrayManager trayManager) {
        this.mLauncher = launcher;
        this.mAppsController = appsController;
        this.mAppsContentView = (ViewGroup) this.mAppsController.getContainerView();
        this.mAppsPagedView = this.mAppsController.getAppsPagedView();
        this.mAppsPageIndicatorView = this.mAppsController.getAppsPageIndicatorView();
        this.mFolderBgGrowFactor = launcher.getResources().getFraction(R.fraction.config_folderBgGrowFactor, 1, 1);
        this.mTrayManager = trayManager;
    }

    public Animator getEnterFromHomeAnimation(boolean animated, @Nullable HashMap<View, Integer> layerViews) {
        float f = 0.0f;
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (!animated) {
            this.mAppsContentView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        } else if (layerViews == null) {
            float movingDistance = 0.0f;
            int range = 0;
            if (this.mTrayManager != null) {
                range = this.mTrayManager.getTrayMovingRange();
                movingDistance = this.mTrayManager.getTrayMovingDistance();
            }
            if (!(range == 0 || movingDistance == 0.0f)) {
                f = 1.0f - Math.abs(movingDistance / ((float) range));
            }
            int duration = getSwipeAnimationDuration(true, f);
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsPageIndicatorView, View.ALPHA.getName(), 1.0f);
            alphaAnim.setDuration((long) duration);
            this.mStageAnimator.play(alphaAnim);
        }
        return this.mStageAnimator;
    }

    public Animator getEnterFromFolderAnimation(boolean animated, HashMap<View, Integer> layerViews, View anchorView) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(5, 2);
            layerViews.put(this.mAppsContentView, Integer.valueOf(1));
            int[] iconLoc = new int[2];
            if (anchorView != null) {
                anchorView.getLocationOnScreen(iconLoc);
                if (iconLoc[0] < 0 && iconLoc[1] < 0) {
                    iconLoc[0] = this.mAppsContentView.getWidth() / 2;
                    iconLoc[1] = this.mAppsContentView.getHeight() / 2;
                }
            } else {
                iconLoc[0] = this.mAppsContentView.getWidth() / 2;
                iconLoc[1] = this.mAppsContentView.getHeight() / 2;
            }
            this.mAppsContentView.setPivotX((float) iconLoc[0]);
            this.mAppsContentView.setPivotY((float) iconLoc[1]);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.ALPHA.getName(), 0.0f, 1.0f);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            this.mStageAnimator.play(alphaAnim);
            Animator scaleXAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.SCALE_X.getName(), this.mFolderBgGrowFactor, 1.0f);
            scaleXAnim.setDuration((long) duration);
            scaleXAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleXAnim);
            Animator scaleYAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.SCALE_Y.getName(), this.mFolderBgGrowFactor, 1.0f);
            scaleYAnim.setDuration((long) duration);
            scaleYAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleYAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    AppsTransitionAnimation.this.mAppsContentView.setVisibility(View.VISIBLE);
                }

                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    AppsTransitionAnimation.this.mAppsContentView.setScaleX(1.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setScaleY(1.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setAlpha(1.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setPivotX(((float) AppsTransitionAnimation.this.mAppsContentView.getWidth()) / 2.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setPivotY(((float) AppsTransitionAnimation.this.mAppsContentView.getHeight()) / 2.0f);
                    AppsTransitionAnimation.this.mStageAnimator = null;
                }
            });
        } else {
            this.mAppsContentView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    public Animator getExitToHomeAnimation(boolean animated, @Nullable HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (!animated) {
            this.mAppsContentView.setAlpha(0.0f);
            if (this.mAppsPageIndicatorView != null) {
                this.mAppsPageIndicatorView.setAlpha(0.0f);
            }
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        } else if (layerViews == null) {
            float f;
            float movingDistance = 0.0f;
            int range = 0;
            if (this.mTrayManager != null) {
                range = this.mTrayManager.getTrayMovingRange();
                movingDistance = this.mTrayManager.getTrayMovingDistance();
            }
            if (range == 0 || movingDistance == 0.0f) {
                f = 0.0f;
            } else {
                f = Math.abs(movingDistance / ((float) range));
            }
            int duration = getSwipeAnimationDuration(false, f);
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsPageIndicatorView, View.ALPHA.getName(), 0.0f);
            alphaAnim.setDuration((long) duration);
            this.mStageAnimator.play(alphaAnim);
        }
        return this.mStageAnimator;
    }

    public Animator getExitToFolderAnimation(boolean animated, HashMap<View, Integer> layerViews, View anchorView) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(2, 5);
            layerViews.put(this.mAppsContentView, Integer.valueOf(1));
            int[] iconLoc = new int[2];
            if (anchorView != null) {
                anchorView.getLocationOnScreen(iconLoc);
            } else {
                iconLoc[0] = this.mAppsContentView.getWidth() / 2;
                iconLoc[1] = this.mAppsContentView.getHeight() / 2;
            }
            this.mAppsContentView.setPivotX((float) iconLoc[0]);
            this.mAppsContentView.setPivotY((float) iconLoc[1]);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.ALPHA.getName(), 0.0f);
            alphaAnim.setDuration((long) (duration / 2));
            alphaAnim.setInterpolator(this.mSineInOut70);
            this.mStageAnimator.play(alphaAnim);
            Animator scaleXAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.SCALE_X.getName(), 1.0f, this.mFolderBgGrowFactor);
            scaleXAnim.setDuration((long) duration);
            scaleXAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleXAnim);
            Animator scaleYAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.SCALE_Y.getName(), 1.0f, this.mFolderBgGrowFactor);
            scaleYAnim.setDuration((long) duration);
            scaleYAnim.setInterpolator(this.mSineInOut80);
            this.mStageAnimator.play(scaleYAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    AppsTransitionAnimation.this.mAppsContentView.setVisibility(View.GONE);
                    AppsTransitionAnimation.this.mAppsContentView.setScaleX(1.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setScaleY(1.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setPivotX(((float) AppsTransitionAnimation.this.mAppsContentView.getWidth()) / 2.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setPivotY(((float) AppsTransitionAnimation.this.mAppsContentView.getHeight()) / 2.0f);
                    AppsTransitionAnimation.this.mStageAnimator = null;
                }
            });
        } else {
            this.mAppsContentView.setAlpha(0.0f);
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    private int getStageAnimationDuration(int fromStage, int toStage) {
        Resources res = this.mLauncher.getResources();
        if (fromStage == 2) {
            if (toStage == 5) {
                return res.getInteger(R.integer.config_folderBgGlowDuration);
            }
            if (toStage == 1) {
                return res.getInteger(R.integer.config_enterAppsDuration);
            }
        } else if (fromStage == 1) {
            if (toStage == 2) {
                return res.getInteger(R.integer.config_enterAppsDuration);
            }
        } else if (fromStage == 5 && toStage == 2) {
            return res.getInteger(R.integer.config_folderCloseDuration);
        }
        return 0;
    }

    private int getSwipeAnimationDuration(boolean enter, float movingDistance) {
        int duration;
        if (enter) {
            duration = getStageAnimationDuration(1, 2);
        } else {
            duration = getStageAnimationDuration(2, 1);
        }
        if (this.mTrayManager == null) {
            return duration;
        }
        if (movingDistance == 0.0f) {
            return 0;
        }
        return this.mTrayManager.calculateDuration(movingDistance, duration);
    }

    private int getStateAnimationDuration(int toState) {
        Resources res = this.mLauncher.getResources();
        if (toState == 3) {
            return res.getInteger(R.integer.config_appsSearchTransitionDuration);
        }
        if (toState == 1) {
            return res.getInteger(R.integer.config_appsDragTransitionDuration);
        }
        if (toState == 2) {
            return res.getInteger(R.integer.config_appsSelectTransitionDuration);
        }
        if (toState == 4) {
            return res.getInteger(R.integer.config_appsTidyUpTransitionDuration);
        }
        if (toState == 0) {
            return res.getInteger(R.integer.config_appsViewTypeTransitionDuration);
        }
        if (toState == 5) {
            return res.getInteger(R.integer.config_appsScreenGridTransitionDuration);
        }
        return 0;
    }

    public AnimatorSet getChangeGridAnimation(boolean animated, HashMap<View, Integer> layerViews, StageEntry entry) {
        int duration = getStateAnimationDuration(0);
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            boolean addedListener = false;
            final ArrayList<View> animateViews = new ArrayList();
            ArrayList<View> viewsTobeShow = this.mAppsController.prepareViewsForReposition();
            int pageCount = this.mAppsPagedView.getChildCount();
            int currentPage = this.mAppsPagedView.getNextPage();
            int i = 0;
            while (i < pageCount) {
                if (i == currentPage || i == currentPage - 1 || i == currentPage + 1) {
                    View iconView;
                    Animator scaleUpAnimSet;
                    CellLayoutChildren cellLayout = this.mAppsPagedView.getCellLayout(i).getCellLayoutChildren();
                    animateViews.add(cellLayout);
                    layerViews.put(cellLayout, Integer.valueOf(1));
                    Animator cellLayoutAlphaToHide = LauncherAnimUtils.ofFloat(cellLayout, "alpha", 0.0f);
                    cellLayoutAlphaToHide.setDuration((long) duration);
                    Animator cellLayoutAlphaToShow = LauncherAnimUtils.ofFloat(cellLayout, "alpha", 1.0f);
                    cellLayoutAlphaToShow.setDuration((long) duration);
                    if (!addedListener) {
                        final StageEntry stageEntry = entry;
                        final ArrayList<View> arrayList = viewsTobeShow;
                        cellLayoutAlphaToHide.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationStart(Animator animation) {
                                AppsTransitionAnimation.this.mAppsPagedView.setCrosshairsVisibilityChilds(8);
                            }

                            public void onAnimationEnd(Animator animation) {
                                AppsTransitionAnimation.this.mAppsController.repositionBy(stageEntry);
                                Iterator it = arrayList.iterator();
                                while (it.hasNext()) {
                                    View iconView = (View) it.next();
                                    iconView.setScaleX(0.9f);
                                    iconView.setScaleY(0.9f);
                                }
                            }

                            public void onAnimationCancel(Animator animation) {
                                onAnimationEnd(animation);
                                Iterator it = animateViews.iterator();
                                while (it.hasNext()) {
                                    View v = (View) it.next();
                                    v.setScaleX(1.0f);
                                    v.setScaleY(1.0f);
                                    v.setAlpha(1.0f);
                                }
                            }
                        });
                        Iterator it = viewsTobeShow.iterator();
                        while (it.hasNext()) {
                            iconView = (View) it.next();
                            animateViews.add(iconView);
                            scaleUpAnimSet = LauncherAnimUtils.createAnimatorSet();
                            Animator[] animatorArr = new Animator[2];
                            animatorArr[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 1.0f);
                            animatorArr[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 1.0f);
                            scaleUpAnimSet.playTogether(animatorArr);
                            this.mStateAnimator.play(scaleUpAnimSet).with(cellLayoutAlphaToShow);
                        }
                        cellLayoutAlphaToShow.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationStart(Animator animation) {
                                AppsTransitionAnimation.this.mAppsPagedView.setCrosshairsVisibilityChilds(0);
                            }

                            public void onAnimationCancel(Animator animation) {
                                Iterator it = animateViews.iterator();
                                while (it.hasNext()) {
                                    View v = (View) it.next();
                                    v.setScaleX(1.0f);
                                    v.setScaleY(1.0f);
                                    v.setAlpha(1.0f);
                                }
                            }
                        });
                        addedListener = true;
                    }
                    for (int index = 0; index < cellLayout.getChildCount(); index++) {
                        iconView = cellLayout.getChildAt(index);
                        animateViews.add(iconView);
                        AnimatorSet scaleDownAnimSet = LauncherAnimUtils.createAnimatorSet();
                        Animator[] animatorArr2 = new Animator[2];
                        animatorArr2[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 0.9f);
                        animatorArr2[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 0.9f);
                        scaleDownAnimSet.playTogether(animatorArr2);
                        scaleUpAnimSet = LauncherAnimUtils.createAnimatorSet();
                        animatorArr2 = new Animator[2];
                        animatorArr2[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 1.0f);
                        animatorArr2[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 1.0f);
                        scaleUpAnimSet.playTogether(animatorArr2);
                        this.mStateAnimator.play(scaleDownAnimSet).with(cellLayoutAlphaToHide);
                        this.mStateAnimator.play(scaleUpAnimSet).with(cellLayoutAlphaToShow).after(scaleDownAnimSet);
                        if (viewsTobeShow.contains(iconView)) {
                            viewsTobeShow.remove(iconView);
                        }
                    }
                    this.mStateAnimator.play(cellLayoutAlphaToHide);
                    this.mStateAnimator.play(cellLayoutAlphaToShow).after(cellLayoutAlphaToHide);
                }
                i++;
            }
        } else {
            this.mAppsPagedView.setCrosshairsVisibilityChilds(8, false);
            this.mAppsController.repositionBy(entry);
            this.mAppsPagedView.setCrosshairsVisibilityChilds(0, false);
        }
        return this.mStateAnimator;
    }

    private AnimatorSet getRepositionEnterAnimation(boolean animated, HashMap<View, Integer> layerViews, StageEntry data) {
        int duration = getStateAnimationDuration(0);
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            getViewInOutAnimator(layerViews, duration, data, null);
        } else {
            this.mAppsController.repositionBy(data);
        }
        return this.mStateAnimator;
    }

    public AnimatorSet getChangeViewTypeAnimation(boolean animated, HashMap<View, Integer> layerViews, StageEntry data) {
        return getRepositionEnterAnimation(animated, layerViews, data);
    }

    public AnimatorSet getScreenGridEnterExitAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean enter) {
        float appsPagedViewTranslationY;
        int appsPagedViewOffsetX;
        final boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        Resources res = this.mLauncher.getResources();
        final AppsPagedView appsPagedView = this.mAppsController.getAppsPagedView();
        final View searchBarContainerView = this.mAppsController.getAppsSearchBarView();
        final AppsScreenGridPanel appsScreengridPanel = this.mAppsController.getAppsScreenGridPanel();
        final View pageIndicator = this.mAppsPagedView.getPageIndicator();
        final View appsScreenGridTopContainer = this.mAppsController.getAppsScreenGridPanel().getScreenGridTopContainer();
        int duration = getStateAnimationDuration(5);
        final float appsPagedViewShrinkFactor = enter ? getGridShrinkFactor() : 1.0f;
        float appsPagedViewTranslationScreenGridY = (float) (res.getDimensionPixelSize(R.dimen.apps_paged_view_offsetY_screengrid) + res.getDimensionPixelSize(R.dimen.all_apps_grid_top_bottom_padding));
        if (enter) {
            appsPagedViewTranslationY = -appsPagedViewTranslationScreenGridY;
        } else {
            appsPagedViewTranslationY = (float) (-res.getDimensionPixelSize(R.dimen.all_apps_grid_top_bottom_padding));
        }
        final float searchBarAlphaValue = enter ? 0.0f : 1.0f;
        final float screenGridAlphaValue = enter ? 1.0f : 0.0f;
        int currentPage = appsPagedView.getNextPage();
        int pageCount = appsPagedView.getPageCount();
        if (pageCount == 0) {
            appsPagedViewOffsetX = 0;
        } else {
            appsPagedViewOffsetX = (appsPagedView.getLayoutTransitionOffsetForPage(currentPage) * 2) * (Utilities.sIsRtl ? pageCount - currentPage : currentPage);
        }
        final float pageIndicatorTranslationY = enter ? (float) (-res.getDimensionPixelSize(R.dimen.apps_indicator_margin_bottom_screengrid)) : 0.0f;
        cancelStateAnimation();
        if (appsPagedViewOffsetX != 0) {
            appsPagedView.scrollTo(appsPagedView.getScrollForPage(currentPage) + appsPagedViewOffsetX, 0);
        }
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(appsScreengridPanel, Integer.valueOf(1));
            layerViews.put(searchBarContainerView, Integer.valueOf(1));
            layerViews.put(appsScreenGridTopContainer, Integer.valueOf(1));
            Animator launcherViewPropertyAnimator = new LauncherViewPropertyAnimator(appsPagedView);
            launcherViewPropertyAnimator.scaleX(appsPagedViewShrinkFactor);
            launcherViewPropertyAnimator.scaleY(appsPagedViewShrinkFactor);
            launcherViewPropertyAnimator.translationY(appsPagedViewTranslationY);
            launcherViewPropertyAnimator.setDuration((long) duration);
            this.mStateAnimator.play(launcherViewPropertyAnimator);
            if (pageIndicator != null) {
                Animator pageIndicatorY = new LauncherViewPropertyAnimator(pageIndicator).translationY(pageIndicatorTranslationY);
                pageIndicatorY.setDuration((long) duration);
                pageIndicatorY.setInterpolator(new DecelerateInterpolator());
                this.mStateAnimator.play(pageIndicatorY);
            }
            Animator searchBarAlpha = new LauncherViewPropertyAnimator(searchBarContainerView).alpha(searchBarAlphaValue);
            searchBarAlpha.addListener(new AlphaUpdateListener(searchBarContainerView, accessibilityEnabled));
            Animator screenGridPanelAlpha = new LauncherViewPropertyAnimator(appsScreengridPanel).alpha(screenGridAlphaValue);
            screenGridPanelAlpha.addListener(new AlphaUpdateListener(appsScreengridPanel, accessibilityEnabled));
            Animator topContainerAlpha = new LauncherViewPropertyAnimator(appsScreenGridTopContainer).alpha(screenGridAlphaValue);
            topContainerAlpha.addListener(new AlphaUpdateListener(appsScreenGridTopContainer, accessibilityEnabled));
            if (enter) {
                searchBarAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                screenGridPanelAlpha.setInterpolator(null);
                topContainerAlpha.setInterpolator(null);
            } else {
                searchBarAlpha.setInterpolator(null);
                screenGridPanelAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
                topContainerAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            }
            searchBarAlpha.setDuration((long) duration);
            screenGridPanelAlpha.setDuration((long) duration);
            topContainerAlpha.setDuration((long) duration);
            this.mStateAnimator.play(searchBarAlpha);
            this.mStateAnimator.play(screenGridPanelAlpha);
            this.mStateAnimator.play(topContainerAlpha);
            final boolean z = enter;
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    AppsTransitionAnimation.this.mStateAnimator = null;
                    searchBarContainerView.setAlpha(searchBarAlphaValue);
                    AlphaUpdateListener.updateVisibility(searchBarContainerView, accessibilityEnabled);
                    appsScreengridPanel.setAlpha(screenGridAlphaValue);
                    AlphaUpdateListener.updateVisibility(appsScreengridPanel, accessibilityEnabled);
                    appsScreenGridTopContainer.setAlpha(screenGridAlphaValue);
                    AlphaUpdateListener.updateVisibility(appsScreenGridTopContainer, accessibilityEnabled);
                    appsPagedView.setScaleX(appsPagedViewShrinkFactor);
                    appsPagedView.setScaleY(appsPagedViewShrinkFactor);
                    appsPagedView.setTranslationY(z ? appsPagedViewTranslationY : 0.0f);
                    int childCount = AppsTransitionAnimation.this.mAppsPagedView.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        ((AppsViewCellLayout) appsPagedView.getChildAt(i)).requestLayout();
                    }
                    if (pageIndicator != null) {
                        pageIndicator.setTranslationY(pageIndicatorTranslationY);
                    }
                }
            });
        } else {
            searchBarContainerView.setAlpha(searchBarAlphaValue);
            AlphaUpdateListener.updateVisibility(searchBarContainerView, accessibilityEnabled);
            appsScreengridPanel.setAlpha(screenGridAlphaValue);
            AlphaUpdateListener.updateVisibility(appsScreengridPanel, accessibilityEnabled);
            appsScreenGridTopContainer.setAlpha(screenGridAlphaValue);
            AlphaUpdateListener.updateVisibility(appsScreenGridTopContainer, accessibilityEnabled);
            appsPagedView.setScaleX(appsPagedViewShrinkFactor);
            appsPagedView.setScaleY(appsPagedViewShrinkFactor);
            if (!enter) {
                appsPagedViewTranslationY = 0.0f;
            }
            appsPagedView.setTranslationY(appsPagedViewTranslationY);
            int childCount = this.mAppsPagedView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((AppsViewCellLayout) appsPagedView.getChildAt(i)).requestLayout();
            }
            if (pageIndicator != null) {
                pageIndicator.setTranslationY(pageIndicatorTranslationY);
            }
        }
        changeScreenGridBackground(appsPagedView, animated, enter, duration);
        return this.mStateAnimator;
    }

    public AnimatorSet getTidyUpAnimation(boolean animated, HashMap<View, Integer> layerViews, final boolean enter, StageEntry data) {
        int i = 0;
        int duration = getStateAnimationDuration(4);
        final float searchBarAlphaValue = enter ? 0.0f : 1.0f;
        final View searchBarContainerView = this.mAppsController.getAppsSearchBarView();
        final View tidyUpContainerView = this.mAppsController.getTidyUpContainerView();
        Runnable r = new Runnable() {
            public void run() {
                AppsTransitionAnimation.this.changeCellLayoutBackground(4, enter);
            }
        };
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(searchBarContainerView, Integer.valueOf(1));
            LauncherViewPropertyAnimator searchBarAlpha = new LauncherViewPropertyAnimator(searchBarContainerView);
            searchBarAlpha.alpha(searchBarAlphaValue).setDuration((long) duration);
            searchBarAlpha.addListener(new AlphaUpdateListener(searchBarContainerView, Talk.INSTANCE.isAccessibilityEnabled()));
            searchBarAlpha.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (enter) {
                        tidyUpContainerView.setVisibility(View.VISIBLE);
                    }
                }
            });
            getViewInOutAnimator(layerViews, duration, data, r);
            if (!enter) {
                tidyUpContainerView.setVisibility(View.GONE);
            }
            this.mStateAnimator.play(searchBarAlpha);
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    AppsTransitionAnimation.this.mStateAnimator = null;
                }

                public void onAnimationCancel(Animator animation) {
                    searchBarContainerView.setAlpha(searchBarAlphaValue);
                }
            });
        } else {
            int i2;
            searchBarContainerView.setAlpha(searchBarAlphaValue);
            if (enter) {
                i2 = 8;
            } else {
                i2 = 0;
            }
            searchBarContainerView.setVisibility(i2);
            if (!enter) {
                i = 8;
            }
            tidyUpContainerView.setVisibility(i);
            this.mAppsController.repositionBy(data);
            changeCellLayoutBackground(4, enter);
        }
        return this.mStateAnimator;
    }

    private void getViewInOutAnimator(HashMap<View, Integer> layerViews, int duration, StageEntry entry, Runnable animationEndRunnable) {
        if (this.mStateAnimator == null || this.mAppsPagedView.getPageCount() == 0) {
            Log.d("AppsTransitionAnimation", "mStateAnimator is null. It must be created before using ViewAnimator");
            return;
        }
        final ArrayList<View> animateViews = new ArrayList();
        CellLayout cellLayout = this.mAppsPagedView.getCellLayout(this.mAppsPagedView.getNextPage());
        animateViews.add(cellLayout);
        layerViews.put(cellLayout, Integer.valueOf(1));
        Animator cellLayoutAlphaToHide = LauncherAnimUtils.ofFloat(cellLayout, "alpha", 0.0f);
        cellLayoutAlphaToHide.setDuration((long) duration);
        final StageEntry stageEntry = entry;
        final Runnable runnable = animationEndRunnable;
        cellLayoutAlphaToHide.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                AppsTransitionAnimation.this.mAppsController.repositionBy(stageEntry);
                if (runnable != null) {
                    runnable.run();
                }
            }

            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
                Iterator it = animateViews.iterator();
                while (it.hasNext()) {
                    View v = (View) it.next();
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    v.setAlpha(1.0f);
                }
            }
        });
        Animator cellLayoutAlphaToShow = LauncherAnimUtils.ofFloat(cellLayout, "alpha", 1.0f);
        cellLayoutAlphaToShow.setDuration((long) duration);
        ArrayList<View> viewsTobeShow = this.mAppsController.prepareViewsForReposition();
        for (int index = 0; index < cellLayout.getPageChildCount(); index++) {
            View iconView = cellLayout.getChildOnPageAt(index);
            animateViews.add(iconView);
            AnimatorSet scaleDownAnimSet = LauncherAnimUtils.createAnimatorSet();
            Animator[] animatorArr = new Animator[2];
            animatorArr[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 0.9f);
            animatorArr[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 0.9f);
            scaleDownAnimSet.playTogether(animatorArr);
            AnimatorSet scaleUpAnimSet = LauncherAnimUtils.createAnimatorSet();
            animatorArr = new Animator[2];
            animatorArr[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 1.0f);
            animatorArr[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 1.0f);
            scaleUpAnimSet.playTogether(animatorArr);
            this.mStateAnimator.play(scaleDownAnimSet).with(cellLayoutAlphaToHide);
            this.mStateAnimator.play(scaleUpAnimSet).with(cellLayoutAlphaToShow).after(scaleDownAnimSet);
            if (viewsTobeShow.contains(iconView)) {
                viewsTobeShow.remove(iconView);
            }
        }
        Iterator it = viewsTobeShow.iterator();
        while (it.hasNext()) {
            iconView = (View) it.next();
            animateViews.add(iconView);
            scaleUpAnimSet = LauncherAnimUtils.createAnimatorSet();
            iconView.setScaleX(0.9f);
            iconView.setScaleY(0.9f);
            Animator[] animatorArr2 = new Animator[2];
            animatorArr2[0] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_X.getName(), 1.0f);
            animatorArr2[1] = LauncherAnimUtils.ofFloat(iconView, View.SCALE_Y.getName(), 1.0f);
            scaleUpAnimSet.playTogether(animatorArr2);
            this.mStateAnimator.play(scaleUpAnimSet).with(cellLayoutAlphaToShow);
        }
        this.mStateAnimator.play(cellLayoutAlphaToHide);
        this.mStateAnimator.play(cellLayoutAlphaToShow).after(cellLayoutAlphaToHide);
    }

    public AnimatorSet getSearchAnimation(boolean animated, boolean enter) {
        cancelStateAnimation();
        if (!animated) {
            this.mAppsController.setPagedViewVisibility(!enter);
        }
        return this.mStateAnimator;
    }

    public AnimatorSet getDragAnimation(boolean animated, HashMap<View, Integer> layerViews, final boolean enter, boolean withSelect) {
        View topLayout;
        int duration = getStateAnimationDuration(1);
        final float searchBarAlphaValue = enter ? 0.0f : 1.0f;
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        View searchBarContainerView = this.mAppsController.getAppsSearchBarView();
        if (withSelect) {
            topLayout = this.mLauncher.getMultiSelectManager().getMultiSelectPanel();
        } else {
            topLayout = searchBarContainerView;
        }
        changeCellLayoutBackground(1, enter);
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(topLayout, Integer.valueOf(1));
            LauncherViewPropertyAnimator topBarAlpha = new LauncherViewPropertyAnimator(topLayout).alpha(searchBarAlphaValue);
            topBarAlpha.addListener(new AlphaUpdateListener(topLayout, accessibilityEnabled));
            if (enter) {
                topBarAlpha.setInterpolator(new DecelerateInterpolator(2.0f));
            } else {
                topBarAlpha.setInterpolator(null);
            }
            topBarAlpha.setDuration((long) duration);
            this.mStateAnimator.play(topBarAlpha);
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    AppsTransitionAnimation.this.mStateAnimator = null;
                    topLayout.setVisibility(enter ? 8 : 0);
                }

                public void onAnimationCancel(Animator animation) {
                    topLayout.setAlpha(searchBarAlphaValue);
                }
            });
        } else {
            topLayout.setAlpha(searchBarAlphaValue);
            topLayout.setVisibility(enter ? 8 : 0);
        }
        return this.mStateAnimator;
    }

    public AnimatorSet getSelectAnimation(boolean animated, HashMap<View, Integer> layerViews, boolean enter) {
        final MultiSelectPanel multiSelectPanel = this.mLauncher.getMultiSelectManager().getMultiSelectPanel();
        int duration = enter ? getStateAnimationDuration(2) : 0;
        int startDelay = enter ? duration : 0;
        float searchBarAlphaValue = enter ? 0.0f : 1.0f;
        float panelAlpha = enter ? 1.0f : 0.0f;
        float panelScale = enter ? 1.0f : 0.95f;
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        View searchBarContainerView = this.mAppsController.getAppsSearchBarView();
        changeCellLayoutBackground(2, enter);
        cancelStateAnimation();
        if (animated) {
            this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
            layerViews.put(multiSelectPanel, Integer.valueOf(1));
            layerViews.put(searchBarContainerView, Integer.valueOf(1));
            LauncherViewPropertyAnimator searchBarAlpha = new LauncherViewPropertyAnimator(searchBarContainerView).alpha(searchBarAlphaValue);
            searchBarAlpha.addListener(new AlphaUpdateListener(searchBarContainerView, accessibilityEnabled));
            searchBarAlpha.setInterpolator(this.mSineInOut70);
            searchBarAlpha.setDuration((long) duration);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.ALPHA.getName(), panelAlpha);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            alphaAnim.setStartDelay((long) startDelay);
            Animator scaleXAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.SCALE_X.getName(), panelScale);
            scaleXAnim.setDuration((long) duration);
            scaleXAnim.setInterpolator(this.mSineInOut80);
            scaleXAnim.setStartDelay((long) startDelay);
            Animator scaleYAnim = LauncherAnimUtils.ofFloat(multiSelectPanel, View.SCALE_Y.getName(), panelScale);
            scaleYAnim.setDuration((long) duration);
            scaleYAnim.setInterpolator(this.mSineInOut80);
            scaleYAnim.setStartDelay((long) startDelay);
            this.mStateAnimator.playTogether(new Animator[]{searchBarAlpha, alphaAnim, scaleXAnim, scaleYAnim});
            final boolean z = enter;
            this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (z) {
                        multiSelectPanel.setVisibility(View.VISIBLE);
                        multiSelectPanel.bringToFront();
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    AppsTransitionAnimation.this.mStateAnimator = null;
                    if (z) {
                        multiSelectPanel.setVisibility(View.VISIBLE);
                    } else {
                        multiSelectPanel.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            searchBarContainerView.setAlpha(searchBarAlphaValue);
            searchBarContainerView.setVisibility(!enter ? 0 : 8);
            multiSelectPanel.setScaleX(panelScale);
            multiSelectPanel.setScaleY(panelScale);
            multiSelectPanel.setAlpha(panelAlpha);
            multiSelectPanel.setVisibility(enter ? 0 : 8);
        }
        return this.mStateAnimator;
    }

    public Animator getEnterFromSettingAnim(boolean animated, HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(5, 2);
            layerViews.put(this.mAppsContentView, Integer.valueOf(1));
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.ALPHA.getName(), 1.0f);
            alphaAnim.setStartDelay(100);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            this.mStageAnimator.play(alphaAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    AppsTransitionAnimation.this.mAppsContentView.setAlpha(0.0f);
                    AppsTransitionAnimation.this.mAppsContentView.setVisibility(View.VISIBLE);
                }

                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    AppsTransitionAnimation.this.mAppsContentView.setAlpha(1.0f);
                }
            });
        } else {
            this.mAppsContentView.setTranslationY(0.0f);
            this.mAppsContentView.setAlpha(1.0f);
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        }
        return this.mStageAnimator;
    }

    public Animator getExitToWidgetAnim(boolean animated, HashMap<View, Integer> layerViews) {
        boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
        cancelStageAnimation();
        if (animated) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            int duration = getStageAnimationDuration(5, 2);
            layerViews.put(this.mAppsContentView, Integer.valueOf(1));
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mAppsContentView, View.ALPHA.getName(), 0.0f);
            alphaAnim.setDuration((long) duration);
            alphaAnim.setInterpolator(this.mSineInOut33);
            this.mStageAnimator.play(alphaAnim);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    AppsTransitionAnimation.this.mAppsContentView.setAlpha(1.0f);
                }

                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    AppsTransitionAnimation.this.mAppsContentView.setVisibility(4);
                    AppsTransitionAnimation.this.mStageAnimator = null;
                }
            });
        } else {
            this.mAppsContentView.setAlpha(0.0f);
            AlphaUpdateListener.updateVisibility(this.mAppsContentView, accessibilityEnabled);
        }
        return this.mStageAnimator;
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

    private void changeCellLayoutBackground(int state, boolean enter) {
        float backgroundAlpha;
        AppsPagedView appsPagedView = this.mAppsController.getAppsPagedView();
        if (enter) {
            backgroundAlpha = 1.0f;
        } else {
            backgroundAlpha = 0.0f;
        }
        int childCount = appsPagedView.getChildCount();
        int currentPage = appsPagedView.getNextPage();
        for (int i = 0; i < childCount; i++) {
            AppsViewCellLayout cl = (AppsViewCellLayout) appsPagedView.getChildAt(i);
            if (enter) {
                cl.setBgImage(state);
            } else {
                cl.setBgImage(0);
            }
            if (i == currentPage) {
                cl.setBackgroundAlpha(0.0f);
            } else {
                cl.setBackgroundAlpha(backgroundAlpha);
            }
        }
    }

    private void changeScreenGridBackground(AppsPagedView appsPagedView, boolean animated, final boolean enter, int duration) {
        float backgroundAlpha = enter ? 1.0f : 0.0f;
        int childCount = appsPagedView.getChildCount();
        int currentPage = appsPagedView.getNextPage();
        int i = 0;
        while (i < childCount) {
            final AppsViewCellLayout cl = (AppsViewCellLayout) appsPagedView.getChildAt(i);
            if (animated) {
                if (enter) {
                    cl.setBgImage(5);
                    if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                        cl.setBackgroundAlpha(backgroundAlpha);
                    }
                } else if (!(i == currentPage || i == currentPage - 1 || i == currentPage + 1)) {
                    cl.setBgImage(0);
                    cl.setBackgroundAlpha(backgroundAlpha);
                }
                if (i == currentPage || i == currentPage - 1 || i == currentPage + 1) {
                    ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha", new float[]{cl.getBackgroundAlpha(), backgroundAlpha});
                    bgAnim.setDuration((long) duration);
                    bgAnim.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            if (!enter) {
                                cl.setBgImage(0);
                            }
                        }
                    });
                    this.mStateAnimator.play(bgAnim);
                }
            } else {
                if (enter) {
                    cl.setBgImage(5);
                } else {
                    cl.setBgImage(0);
                }
                cl.setBackgroundAlpha(backgroundAlpha);
            }
            i++;
        }
    }

    private float getGridShrinkFactor() {
        return ((float) this.mLauncher.getResources().getInteger(R.integer.config_apps_gridShrinkPercentage)) / 100.0f;
    }
}
