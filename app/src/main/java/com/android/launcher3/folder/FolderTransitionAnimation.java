package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.view.DragLayer.LayoutParams;
import com.android.launcher3.folder.view.FolderCellLayout;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderPagedView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class FolderTransitionAnimation {
    public static final int DURATION_PARTIAL_BG_ANIMATION = 200;
    private static final String TAG = FolderTransitionAnimation.class.getSimpleName();
    private int mAddItemsStartDelay;
    private AnimationInfo mAnimInfoForClosing;
    private int mFolderCloseDuration;
    private int mFolderExpandDuration;
    private int mFolderHeaderTransitionDuration;
    private FakeFolderIconAnimation mIconAnim;
    private Launcher mLauncher;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final Interpolator mSineInOut50 = ViInterpolator.getInterploator(31);
    private final Interpolator mSineInOut80 = ViInterpolator.getInterploator(34);
    private AnimatorSet mStageAnimator;
    private AnimatorSet mStateAnimator;

    private static class AnimationInfo {
        boolean isValidView;
        int[] location = new int[2];
        float[] scaleBy = new float[2];
    }

    private class FakeFolderIconAnimation {
        private ImageView mFakeView;

        private FakeFolderIconAnimation() {
        }

        public void prepareAimation(View openFolder, FolderIconView folderIcon, AnimationInfo info, boolean open) {
            if (this.mFakeView == null) {
                this.mFakeView = new ImageView(FolderTransitionAnimation.this.mLauncher);
            } else if (this.mFakeView.getParent() != null) {
                ((ViewGroup) this.mFakeView.getParent()).removeView(this.mFakeView);
            }
            prepareFakeFolderIcon(folderIcon);
            FolderTransitionAnimation.this.mLauncher.getDragLayer().addView(this.mFakeView, makeLayoutParams(openFolder));
            if (open) {
                this.mFakeView.setTranslationX((float) info.location[0]);
                this.mFakeView.setTranslationY((float) info.location[1]);
                this.mFakeView.setScaleX(info.scaleBy[0]);
                this.mFakeView.setScaleY(info.scaleBy[1]);
                return;
            }
            this.mFakeView.setAlpha(0.0f);
        }

        public void animateOpen(AnimatorSet animSet, final View iconView, long duration) {
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mFakeView, View.ALPHA.getName(), 0.0f);
            alphaAnim.setDuration(duration / 5);
            alphaAnim.setInterpolator(FolderTransitionAnimation.this.mSineInOut80);
            animSet.play(alphaAnim);
            AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
            r2 = new Animator[4];
            r2[0] = LauncherAnimUtils.ofFloat(this.mFakeView, View.SCALE_X.getName(), 1.0f);
            r2[1] = LauncherAnimUtils.ofFloat(this.mFakeView, View.SCALE_Y.getName(), 1.0f);
            r2[2] = LauncherAnimUtils.ofFloat(this.mFakeView, View.TRANSLATION_X.getName(), 0.0f);
            r2[3] = LauncherAnimUtils.ofFloat(this.mFakeView, View.TRANSLATION_Y.getName(), 0.0f);
            scaleAnimSet.playTogether(r2);
            scaleAnimSet.setDuration(duration);
            scaleAnimSet.setInterpolator(FolderTransitionAnimation.this.mSineInOut80);
            animSet.play(scaleAnimSet);
            iconView.setVisibility(4);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (iconView != null) {
                        iconView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        public void animateClose(AnimatorSet animSet, final View iconView, long duration) {
            Animator alphaAnim = LauncherAnimUtils.ofFloat(this.mFakeView, View.ALPHA.getName(), 1.0f);
            alphaAnim.setStartDelay(duration / 5);
            alphaAnim.setDuration((4 * duration) / 5);
            alphaAnim.setInterpolator(FolderTransitionAnimation.this.mSineInOut50);
            animSet.play(alphaAnim);
            ValueAnimator scaleAnim = LauncherAnimUtils.ofFloat(this.mFakeView, 0.0f, 1.0f);
            scaleAnim.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (FakeFolderIconAnimation.this.mFakeView != null && FolderTransitionAnimation.this.mAnimInfoForClosing != null) {
                        if (FolderTransitionAnimation.this.mAnimInfoForClosing.isValidView) {
                            float percent = ((Float) animation.getAnimatedValue()).floatValue();
                            FakeFolderIconAnimation.this.mFakeView.setScaleX(1.0f - ((1.0f - FolderTransitionAnimation.this.mAnimInfoForClosing.scaleBy[0]) * percent));
                            FakeFolderIconAnimation.this.mFakeView.setScaleY(1.0f - ((1.0f - FolderTransitionAnimation.this.mAnimInfoForClosing.scaleBy[1]) * percent));
                            FakeFolderIconAnimation.this.mFakeView.setTranslationX(((float) FolderTransitionAnimation.this.mAnimInfoForClosing.location[0]) * percent);
                            FakeFolderIconAnimation.this.mFakeView.setTranslationY(((float) FolderTransitionAnimation.this.mAnimInfoForClosing.location[1]) * percent);
                            return;
                        }
                        FakeFolderIconAnimation.this.mFakeView.setScaleX(0.0f);
                        FakeFolderIconAnimation.this.mFakeView.setScaleY(0.0f);
                    }
                }
            });
            scaleAnim.setDuration(duration);
            scaleAnim.setInterpolator(FolderTransitionAnimation.this.mSineInOut80);
            animSet.play(scaleAnim);
            iconView.setVisibility(4);
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    animationComplete();
                }

                private void animationComplete() {
                    iconView.setVisibility(View.VISIBLE);
                    FolderTransitionAnimation.this.mLauncher.getDragLayer().removeView(FakeFolderIconAnimation.this.mFakeView);
                }
            });
        }

        private void prepareFakeFolderIcon(FolderIconView folderIconView) {
            this.mFakeView.setImageBitmap(null);
            Drawable previewIcons = folderIconView.getIcon();
            Drawable previewBackground = folderIconView.getIconBackground();
            if (previewIcons == null || previewBackground == null) {
                Log.e(FolderTransitionAnimation.TAG, "prepareFakeFolderIcon : can't get preview image");
                return;
            }
            int iconSize = folderIconView.getIconSize();
            float iconScale = ((float) iconSize) / ((float) previewIcons.getIntrinsicWidth());
            float bgScale = ((float) iconSize) / ((float) previewBackground.getIntrinsicWidth());
            Bitmap previewBitmap = Bitmap.createBitmap(iconSize, iconSize, Config.ARGB_8888);
            Canvas canvas = new Canvas(previewBitmap);
            if (bgScale != 1.0f) {
                canvas.save();
                canvas.scale(bgScale, bgScale);
                previewBackground.draw(canvas);
                canvas.restore();
            } else {
                previewBackground.draw(canvas);
            }
            if (iconScale != 1.0f) {
                canvas.save();
                canvas.scale(iconScale, iconScale);
                previewIcons.draw(canvas);
                canvas.restore();
            } else {
                previewIcons.draw(canvas);
            }
            canvas.setBitmap(null);
            this.mFakeView.setImageBitmap(previewBitmap);
            this.mFakeView.setScaleType(ScaleType.FIT_XY);
        }

        private LayoutParams makeLayoutParams(View openFolder) {
            LayoutParams params = (LayoutParams) openFolder.getLayoutParams();
            LayoutParams newParams = new LayoutParams(0, 0);
            newParams.width = params.width;
            newParams.height = params.height;
            newParams.leftMargin = params.leftMargin;
            newParams.topMargin = params.topMargin;
            newParams.setMargins(params.x, params.y, 0, 0);
            return newParams;
        }
    }

    private static class ViewUpdateListener extends AnimatorListenerAdapter {
        private View mTargetView;
        private boolean mToBeShown;

        public ViewUpdateListener(View target, boolean show) {
            this.mTargetView = target;
            this.mToBeShown = show;
        }

        public void onAnimationCancel(Animator animation) {
            if (this.mTargetView != null) {
                if (!this.mToBeShown) {
                    this.mTargetView.setVisibility(View.GONE);
                }
                this.mTargetView.setAlpha(1.0f);
            }
        }

        public void onAnimationEnd(Animator animation) {
            if (this.mTargetView != null) {
                if (!this.mToBeShown) {
                    this.mTargetView.setVisibility(View.GONE);
                }
                this.mTargetView.setAlpha(1.0f);
            }
        }

        public void onAnimationStart(Animator animation) {
            if (this.mTargetView != null) {
                if (this.mToBeShown) {
                    this.mTargetView.setAlpha(0.0f);
                } else {
                    this.mTargetView.setAlpha(1.0f);
                }
                this.mTargetView.setVisibility(View.VISIBLE);
            }
        }
    }

    public FolderTransitionAnimation(Launcher launcher) {
        this.mLauncher = launcher;
        this.mIconAnim = new FakeFolderIconAnimation();
        this.mAddItemsStartDelay = this.mLauncher.getResources().getInteger(R.integer.new_app_delay_duration);
        this.mFolderExpandDuration = this.mLauncher.getResources().getInteger(R.integer.config_folderExpandDuration);
        this.mFolderCloseDuration = this.mLauncher.getResources().getInteger(R.integer.config_folderCloseDuration);
        this.mFolderHeaderTransitionDuration = this.mLauncher.getResources().getInteger(R.integer.config_folderHeaderTransitionDuration);
    }

    public Animator getEnterFromHomeOrAppsAnimation(FolderView openFolder, FolderIconView folderIconView) {
        cancelStageAnimation();
        if (!(openFolder == null || folderIconView == null)) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            AnimationInfo animInfo = new AnimationInfo();
            if (computeAnimationInfo(animInfo, openFolder, folderIconView)) {
                prepareOpenAnimation(animInfo, openFolder);
                animateOpen(this.mStageAnimator, openFolder, (long) this.mFolderExpandDuration);
                this.mIconAnim.prepareAimation(openFolder, folderIconView, animInfo, true);
                this.mIconAnim.animateOpen(this.mStageAnimator, folderIconView, (long) this.mFolderExpandDuration);
            } else {
                animateFadeIn(this.mStageAnimator, openFolder);
            }
            openFolder.setSuppressFolderNameFocus((long) (((float) this.mFolderExpandDuration) * 1.5f));
        }
        return this.mStageAnimator;
    }

    public Animator getEnterFromFolderAddAppsAnimation(FolderView openFolder, ArrayList<IconInfo> items) {
        cancelStageAnimation();
        if (openFolder != null) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            animateFadeIn(this.mStageAnimator, openFolder);
            animateAddItemStart(this.mStageAnimator, openFolder, items);
            openFolder.setSuppressFolderNameFocus((long) (((float) this.mFolderExpandDuration) * 1.5f));
        }
        return this.mStageAnimator;
    }

    public Animator getExitToHomeOrAppsAnimation(FolderView openFolder, FolderIconView folderIconView) {
        cancelStageAnimation();
        if (!(openFolder == null || folderIconView == null)) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            this.mAnimInfoForClosing = new AnimationInfo();
            computeAnimationInfo(this.mAnimInfoForClosing, openFolder, folderIconView);
            animateCloseToPosition(this.mStageAnimator, openFolder, folderIconView, (long) this.mFolderCloseDuration);
            this.mIconAnim.prepareAimation(openFolder, folderIconView, this.mAnimInfoForClosing, false);
            this.mIconAnim.animateClose(this.mStageAnimator, folderIconView, (long) this.mFolderCloseDuration);
            this.mStageAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    FolderTransitionAnimation.this.mAnimInfoForClosing = null;
                }
            });
        }
        return this.mStageAnimator;
    }

    public Animator getExitToFolderAddAppsAnimation(FolderView openFolder) {
        cancelStageAnimation();
        if (openFolder != null) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            animateFadeOut(this.mStageAnimator, openFolder);
        }
        return this.mStageAnimator;
    }

    public AnimatorSet getDragAnimation(boolean animated, FolderView openFolder, final boolean enter) {
        int i = 4;
        float f = 0.0f;
        if (openFolder != null) {
            final View border = openFolder.getBorder();
            final View header = openFolder.getHeader();
            cancelStateAnimation();
            int i2;
            if (animated) {
                this.mStateAnimator = LauncherAnimUtils.createAnimatorSet();
                if (border != null) {
                    float f2;
                    if (enter) {
                        f2 = 0.0f;
                    } else {
                        f2 = 1.0f;
                    }
                    border.setAlpha(f2);
                    if (enter) {
                        i2 = 4;
                    } else {
                        i2 = 0;
                    }
                    border.setVisibility(i2);
                }
                if (header != null) {
                    String name = View.ALPHA.getName();
                    float[] fArr = new float[1];
                    if (!enter) {
                        f = 1.0f;
                    }
                    fArr[0] = f;
                    Animator headerAnim = LauncherAnimUtils.ofFloat(header, name, fArr);
                    headerAnim.setDuration((long) this.mFolderHeaderTransitionDuration);
                    headerAnim.setStartDelay(enter ? 0 : 100);
                    this.mStateAnimator.play(headerAnim);
                }
                if (!enter) {
                    openFolder.setSuppressFolderNameFocus((long) (((float) this.mFolderHeaderTransitionDuration) * 1.5f));
                }
                this.mStateAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animation) {
                        if (!enter) {
                            if (border != null) {
                                border.setVisibility(View.VISIBLE);
                            }
                            if (header != null) {
                                header.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    public void onAnimationEnd(Animator animation) {
                        FolderTransitionAnimation.this.mStateAnimator = null;
                        if (enter) {
                            if (border != null) {
                                border.setVisibility(4);
                            }
                            if (header != null) {
                                header.setVisibility(4);
                            }
                        }
                    }
                });
            } else {
                if (border != null) {
                    border.setAlpha(enter ? 0.0f : 1.0f);
                    if (enter) {
                        i2 = 4;
                    } else {
                        i2 = 0;
                    }
                    border.setVisibility(i2);
                }
                if (header != null) {
                    if (!enter) {
                        f = 1.0f;
                    }
                    header.setAlpha(f);
                    if (!enter) {
                        i = 0;
                    }
                    header.setVisibility(i);
                }
            }
            changeDragBackground(openFolder, enter);
        }
        return this.mStateAnimator;
    }

    public Animator getEnterFromWidgetAnimation(FolderView openFolder) {
        cancelStageAnimation();
        if (openFolder != null) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            animateFadeIn(this.mStageAnimator, openFolder);
        }
        return this.mStageAnimator;
    }

    public Animator getExitToWidgetAnimation(FolderView openFolder) {
        cancelStageAnimation();
        if (openFolder != null) {
            this.mStageAnimator = LauncherAnimUtils.createAnimatorSet();
            animateFadeOut(this.mStageAnimator, openFolder);
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

    private boolean computeAnimationInfo(AnimationInfo folderAnimInfo, FolderView openFolder, FolderIconView folderIconView) {
        if (folderAnimInfo == null || openFolder == null || folderIconView == null) {
            return false;
        }
        View iconView = folderIconView.getIconVew();
        int iconSize = folderIconView.getIconSize();
        int[] iconLocation = new int[2];
        int[] folderLocation = new int[2];
        iconView.getLocationOnScreen(iconLocation);
        if (iconLocation[0] <= 0 || iconLocation[1] <= 0) {
            folderAnimInfo.isValidView = false;
            return false;
        }
        if (this.mLauncher != null) {
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (dp.isMultiwindowMode) {
                if (!dp.isLandscape) {
                    folderLocation[1] = folderLocation[1] + dp.getMultiWindowPanelSize();
                } else if (Utilities.getNavigationBarPositon() == 1) {
                    folderLocation[0] = folderLocation[0] + (dp.getMultiWindowPanelSize() + dp.navigationBarHeight);
                } else {
                    folderLocation[0] = folderLocation[0] + dp.getMultiWindowPanelSize();
                }
            }
        }
        LayoutParams lp = (LayoutParams) openFolder.getLayoutParams();
        folderLocation[0] = folderLocation[0] + lp.x;
        folderLocation[1] = folderLocation[1] + lp.y;
        int startY = (int) (((float) (iconLocation[1] - folderLocation[1])) - (((float) (lp.height - iconSize)) / 2.0f));
        folderAnimInfo.location[0] = (int) (((float) (iconLocation[0] - folderLocation[0])) - (((float) (lp.width - iconSize)) / 2.0f));
        folderAnimInfo.location[1] = startY;
        folderAnimInfo.scaleBy[0] = (((float) iconSize) * 1.0f) / ((float) lp.width);
        folderAnimInfo.scaleBy[1] = (((float) iconSize) * 1.0f) / ((float) lp.height);
        folderAnimInfo.isValidView = true;
        return true;
    }

    private void prepareOpenAnimation(AnimationInfo info, View targetView) {
        targetView.setTranslationX((float) info.location[0]);
        targetView.setTranslationY((float) info.location[1]);
        targetView.setScaleX(info.scaleBy[0]);
        targetView.setScaleY(info.scaleBy[1]);
        targetView.setAlpha(0.0f);
    }

    private void animateOpen(AnimatorSet animSet, View targetView, long duration) {
        Animator alphaAnim = LauncherAnimUtils.ofFloat(targetView, View.ALPHA.getName(), 1.0f);
        alphaAnim.setDuration(duration);
        alphaAnim.setInterpolator(this.mSineInOut80);
        animSet.play(alphaAnim);
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        r2 = new Animator[4];
        r2[0] = LauncherAnimUtils.ofFloat(targetView, View.SCALE_X.getName(), 1.0f);
        r2[1] = LauncherAnimUtils.ofFloat(targetView, View.SCALE_Y.getName(), 1.0f);
        r2[2] = LauncherAnimUtils.ofFloat(targetView, View.TRANSLATION_X.getName(), 0.0f);
        r2[3] = LauncherAnimUtils.ofFloat(targetView, View.TRANSLATION_Y.getName(), 0.0f);
        scaleAnimSet.playTogether(r2);
        scaleAnimSet.setDuration(duration);
        scaleAnimSet.setInterpolator(this.mSineInOut80);
        animSet.play(scaleAnimSet);
        animSet.addListener(new ViewUpdateListener(targetView, true));
    }

    private void animateClose(AnimatorSet animSet, View targetView, long duration) {
        Animator alphaAnim = LauncherAnimUtils.ofFloat(targetView, View.ALPHA.getName(), 0.0f);
        alphaAnim.setDuration((2 * duration) / 3);
        alphaAnim.setInterpolator(this.mSineInOut80);
        animSet.play(alphaAnim);
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        Animator[] animatorArr = new Animator[2];
        animatorArr[0] = LauncherAnimUtils.ofFloat(targetView, View.SCALE_X.getName(), 0.7f);
        animatorArr[1] = LauncherAnimUtils.ofFloat(targetView, View.SCALE_Y.getName(), 0.7f);
        scaleAnimSet.playTogether(animatorArr);
        scaleAnimSet.setDuration(duration);
        scaleAnimSet.setInterpolator(this.mSineInOut80);
        animSet.play(scaleAnimSet);
        animSet.addListener(new ViewUpdateListener(targetView, false));
    }

    private void animateCloseToPosition(AnimatorSet animSet, final FolderView openFolder, final FolderIconView folderIconView, long duration) {
        Animator alphaAnim = LauncherAnimUtils.ofFloat(openFolder, View.ALPHA.getName(), 0.0f);
        alphaAnim.setDuration(duration);
        alphaAnim.setInterpolator(this.mSineInOut80);
        animSet.play(alphaAnim);
        ValueAnimator scaleAnim = LauncherAnimUtils.ofFloat(openFolder, 0.0f, 1.0f);
        scaleAnim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (openFolder != null && folderIconView != null && FolderTransitionAnimation.this.mAnimInfoForClosing != null) {
                    FolderTransitionAnimation.this.computeAnimationInfo(FolderTransitionAnimation.this.mAnimInfoForClosing, openFolder, folderIconView);
                    if (FolderTransitionAnimation.this.mAnimInfoForClosing.isValidView) {
                        float percent = ((Float) animation.getAnimatedValue()).floatValue();
                        openFolder.setScaleX(1.0f - ((1.0f - FolderTransitionAnimation.this.mAnimInfoForClosing.scaleBy[0]) * percent));
                        openFolder.setScaleY(1.0f - ((1.0f - FolderTransitionAnimation.this.mAnimInfoForClosing.scaleBy[1]) * percent));
                        openFolder.setTranslationX(((float) FolderTransitionAnimation.this.mAnimInfoForClosing.location[0]) * percent);
                        openFolder.setTranslationY(((float) FolderTransitionAnimation.this.mAnimInfoForClosing.location[1]) * percent);
                    }
                }
            }
        });
        scaleAnim.setDuration(duration);
        scaleAnim.setInterpolator(this.mSineInOut80);
        animSet.play(scaleAnim);
        animSet.addListener(new ViewUpdateListener(openFolder, false));
    }

    private void animateFadeIn(AnimatorSet animSet, View targetView) {
        Animator enterAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.enter_folder_fade_in);
        enterAnimator.setTarget(targetView);
        enterAnimator.setInterpolator(this.mSineInOut33);
        enterAnimator.addListener(new ViewUpdateListener(targetView, true));
        animSet.play(enterAnimator);
    }

    private void animateFadeOut(AnimatorSet animSet, View targetView) {
        Animator enterAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.exit_folder_fade_out);
        enterAnimator.setTarget(targetView);
        enterAnimator.setInterpolator(this.mSineInOut33);
        enterAnimator.addListener(new ViewUpdateListener(targetView, false));
        animSet.play(enterAnimator);
    }

    private void animateAddItemStart(AnimatorSet animSet, final FolderView openFolder, ArrayList<IconInfo> items) {
        if (items != null && openFolder != null) {
            Iterator it = items.iterator();
            while (it.hasNext()) {
                IconInfo info = (IconInfo) it.next();
                if (openFolder.getInfo().contents.contains(info)) {
                    final View appIcon = openFolder.getViewForInfo(info);
                    if (appIcon != null) {
                        final float scaleX = appIcon.getScaleX();
                        final float scaleY = appIcon.getScaleY();
                        appIcon.setAlpha(0.0f);
                        Animator anim = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.show_new_added_apps);
                        anim.setTarget(appIcon);
                        anim.setStartDelay((long) this.mAddItemsStartDelay);
                        anim.setInterpolator(this.mSineInOut33);
                        anim.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                appIcon.setScaleX(scaleX);
                                appIcon.setScaleY(scaleY);
                            }
                        });
                        animSet.play(anim);
                    }
                }
            }
            animSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    for (int i = 0; i < openFolder.getItemCount(); i++) {
                        View appIcon = (View) openFolder.getItemsInReadingOrder().get(i);
                        if (appIcon != null) {
                            appIcon.setAlpha(1.0f);
                        }
                    }
                }
            });
        }
    }

    private void changeDragBackground(FolderView openFolder, boolean enter) {
        if (openFolder != null) {
            FolderPagedView folderPagedView = openFolder.getContent();
            float backgroundAlpha = enter ? 1.0f : 0.0f;
            int childCount = folderPagedView.getChildCount();
            int currentPage = folderPagedView.getCurrentPage();
            final int contentBorderWidth = openFolder.getContentBorderWidth();
            final int contentBorderHeight = openFolder.getContentBorderHeight();
            final boolean whiteBg = openFolder.isWhiteBg();
            for (int i = 0; i < childCount; i++) {
                final FolderCellLayout cl = (FolderCellLayout) folderPagedView.getChildAt(i);
                if (enter) {
                    cl.setBgImage(2, contentBorderWidth, contentBorderHeight, whiteBg);
                    if (i == currentPage) {
                        cl.setBackgroundAlpha(0.0f);
                        cl.setPartialBackgroundAlpha(backgroundAlpha, true);
                    }
                } else if (i == currentPage) {
                    cl.setBackgroundAlpha(0.0f);
                    cl.setPartialBackgroundAlpha(backgroundAlpha, true);
                    cl.postDelayed(new Runnable() {
                        public void run() {
                            cl.setBgImage(1, contentBorderWidth, contentBorderHeight, whiteBg);
                        }
                    }, 200);
                } else {
                    cl.setBgImage(1, contentBorderWidth, contentBorderHeight, whiteBg);
                }
                if (i != currentPage) {
                    cl.setBackgroundAlpha(backgroundAlpha);
                    cl.setPartialBackgroundAlpha(0.0f);
                }
            }
        }
    }
}
