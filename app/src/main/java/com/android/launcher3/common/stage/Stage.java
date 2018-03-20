package com.android.launcher3.common.stage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.LauncherTransitionable;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.animation.LauncherAnimUtils;

public abstract class Stage {
    private static final int BACKGROUND_ANIMATION_MIN_DURATION = 70;
    public static final int BUILD_AND_SET_LAYER = 1;
    public static final int BUILD_LAYER = 0;
    public static final int STAGE_MODE_APPS = 2;
    public static final int STAGE_MODE_APPSPICKER = 6;
    public static final int STAGE_MODE_FOLDER = 5;
    public static final int STAGE_MODE_HOME = 1;
    public static final int STAGE_MODE_NONE = 0;
    public static final int STAGE_MODE_WIDGETFOLDER = 4;
    public static final int STAGE_MODE_WIDGETS = 3;
    private static final String TAG = "Stage";
    private Animator mBackgroundDimAnim;
    private float mBackgroundDimFinalAlpha;
    private StageConfig mConfig;
    private boolean mIsNeedConfigurationChange = false;
    protected Launcher mLauncher;
    private int mMode = 0;
    protected int mNavigationBarPosition = -1;
    private AnimatorSet mStateSwitchAnim;
    protected boolean mViewInitiated = false;

    protected abstract float getBackgroundBlurAmountForState(int i);

    protected abstract float getBackgroundDimAlphaForState(int i);

    protected abstract float getBackgroundImageAlphaForState(int i);

    public abstract View getContainerView();

    protected abstract int getInternalState();

    protected abstract Animator onStageEnter(StageEntry stageEntry);

    protected abstract Animator onStageEnterByTray();

    protected abstract Animator onStageExit(StageEntry stageEntry);

    protected abstract Animator onStageExitByTray();

    public abstract boolean searchBarHasFocus();

    protected abstract boolean supportNavigationBarForState(int i);

    protected abstract boolean supportStatusBarForState(int i);

    public void setMode(int mode) {
        this.mMode = mode;
    }

    public int getMode() {
        return this.mMode;
    }

    protected Stage() {
    }

    public void initialize(Launcher activity, int mode) {
        this.mLauncher = activity;
        this.mMode = mode;
        this.mConfig = new StageConfig(this.mLauncher);
    }

    protected void initStageView() {
        if (this.mViewInitiated) {
            throw new RuntimeException("initStageView was called duplicately");
        }
        this.mViewInitiated = true;
        setPaddingForNavigationBarIfNeeded();
    }

    final void switchState(StageEntry data) {
        cancelStateSwitchAnimation();
        Animator switchAnim = switchInternalState(data);
        long animDuration = Utilities.getAnimationDuration(switchAnim);
        AnimatorSet animatorSet = null;
        if (switchAnim != null) {
            if (switchAnim instanceof AnimatorSet) {
                animatorSet = (AnimatorSet) switchAnim;
            } else {
                animatorSet = LauncherAnimUtils.createAnimatorSet();
                animatorSet.play(switchAnim);
            }
        }
        updateSystemUIForState(data.getInternalStateTo(), animatorSet, animDuration);
        playStateTransitAnim(animatorSet, data);
    }

    protected Animator switchInternalState(StageEntry data) {
        return null;
    }

    protected void onStageMovingToInitial(StageEntry data) {
    }

    public void setup() {
    }

    public void onStartActivity() {
    }

    public void onResumeActivity() {
    }

    public void onPauseActivity() {
    }

    public void onStopActivity() {
    }

    public void onDestroyActivity() {
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onRestoreInstanceState(Bundle state) {
    }

    public void restoreState(Bundle savedState, boolean isOnTop) {
    }

    public void onConfigurationChangedIfNeeded() {
    }

    protected void setPaddingForNavigationBarIfNeeded() {
    }

    public void onChangeColorForBg(boolean whiteBg) {
    }

    public void onCheckedChanged(View view, boolean isChecked) {
    }

    void onChangeGrid() {
    }

    protected boolean onBackPressed() {
        return false;
    }

    boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onClick(View v) {
        Log.w("", "invalid onClick : " + v);
        return false;
    }

    public DragSource getDragSourceForLongKey() {
        return null;
    }

    protected boolean keepInstance() {
        return true;
    }

    public void onStartForResult(int requestCode) {
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    boolean isViewInitiated() {
        return this.mViewInitiated;
    }

    protected boolean isRestorable() {
        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return false;
    }

    public boolean supportStatusBar() {
        return supportStatusBarForState(getInternalState());
    }

    protected int getSupportSoftInputParam(Window window) {
        return window.getAttributes().softInputMode | 16;
    }

    protected void updateSoftInputParam(Window window, int param) {
        if (window.getAttributes().softInputMode != param) {
            window.setSoftInputMode(param);
        }
    }

    void updateSystemUIForState(int internalState, AnimatorSet animatorSet, long animDuration) {
        if (isTopStage()) {
            Window window = this.mLauncher.getWindow();
            if (window != null) {
                Utilities.hideStatusBar(window, !supportStatusBarForState(internalState));
                Utilities.hideNavigationBar(window, !supportNavigationBarForState(internalState));
                float blurAmount = getBackgroundBlurAmountForState(internalState);
                if (blurAmount >= 0.0f) {
                    boolean showBlur = blurAmount > 0.0f;
                    if (!showBlur) {
                        blurAmount = 0.0f;
                    }
                    BlurUtils.blurByWindowManager(showBlur, window, blurAmount, animDuration);
                }
                updateSoftInputParam(window, getSupportSoftInputParam(window));
            }
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (dragLayer != null) {
                float startAlpha = dragLayer.getBackgroundAlpha();
                float finalAlpha = getBackgroundDimAlphaForState(internalState);
                if (this.mBackgroundDimAnim != null) {
                    if (this.mBackgroundDimFinalAlpha != finalAlpha) {
                        this.mBackgroundDimAnim.cancel();
                        this.mBackgroundDimAnim = null;
                    } else {
                        return;
                    }
                }
                if (startAlpha != finalAlpha && finalAlpha >= 0.0f && finalAlpha <= 1.0f) {
                    this.mBackgroundDimFinalAlpha = finalAlpha;
                    if (animDuration <= 0) {
                        animDuration = 70;
                    }
                    this.mBackgroundDimAnim = LauncherAnimUtils.ofFloat(dragLayer, "backgroundAlpha", startAlpha, finalAlpha);
                    this.mBackgroundDimAnim.setDuration(animDuration);
                    this.mBackgroundDimAnim.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            Stage.this.mBackgroundDimAnim = null;
                            Stage.this.mBackgroundDimFinalAlpha = -1.0f;
                        }
                    });
                    if (animatorSet != null) {
                        animatorSet.play(this.mBackgroundDimAnim);
                    } else {
                        this.mBackgroundDimAnim.start();
                    }
                }
            }
        }
    }

    protected StageManager getStageManager() {
        return this.mLauncher.getStageManager();
    }

    private void cancelStateSwitchAnimation() {
        if (this.mStateSwitchAnim != null) {
            this.mStateSwitchAnim.setDuration(0);
            this.mStateSwitchAnim.cancel();
            this.mStateSwitchAnim = null;
        }
    }

    private void cleanupSwitchAnimation() {
        this.mStateSwitchAnim = null;
    }

    public final boolean isRunningStateChangeAnimation() {
        return this.mStateSwitchAnim != null;
    }

    public boolean finishOnTouchOutSide() {
        return false;
    }

    private void playStateTransitAnim(Animator switchAnim, final StageEntry entry) {
        boolean toWorkSpace;
        Log.d(TAG, "playStateTransitAnim : " + getMode());
        if (getMode() == 1) {
            toWorkSpace = true;
        } else {
            toWorkSpace = false;
        }
        if (switchAnim != null) {
            final AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
            animatorSet.play(switchAnim);
            this.mStateSwitchAnim = animatorSet;
            transitAnimPrepare(true, toWorkSpace, entry);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    Log.d(Stage.TAG, "stateTransitionAnim onAnimationEnd");
                    Stage.this.cleanupSwitchAnimation();
                    Stage.this.transitAnimEnd(true, toWorkSpace, entry);
                    entry.notifyOnCompleteRunnables();
                    for (View v : entry.getLayerViews().keySet()) {
                        if (((Integer) entry.getLayerViews().get(v)).intValue() == 1) {
                            v.setLayerType(0, null);
                        }
                    }
                }
            });
            final long time = System.currentTimeMillis();
            final StageEntry stageEntry = entry;
            Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    Log.d(Stage.TAG, "stateTransitionAnim start runnable time gap : " + (System.currentTimeMillis() - time));
                    if (Stage.this.mStateSwitchAnim != animatorSet) {
                        animatorSet.cancel();
                        Log.d(Stage.TAG, "fail to switch ");
                        return;
                    }
                    Stage.this.transitAnimStart(true, toWorkSpace, stageEntry);
                    for (View v : stageEntry.getLayerViews().keySet()) {
                        if (((Integer) stageEntry.getLayerViews().get(v)).intValue() == 1) {
                            v.setLayerType(2, null);
                        }
                        if (v.isAttachedToWindow()) {
                            v.buildLayer();
                        }
                    }
                    animatorSet.start();
                }
            };
            View postView = getContainerView();
            if (postView == null) {
                throw new IllegalStateException(getMode() + " : there is no containerview");
            }
            postView.post(startAnimRunnable);
            return;
        }
        transitAnimPrepare(false, toWorkSpace, entry);
        transitAnimStart(false, toWorkSpace, entry);
        transitAnimEnd(false, toWorkSpace, entry);
        entry.notifyOnCompleteRunnables();
    }

    private void transitAnimPrepare(boolean animated, boolean toWorkspace, StageEntry entry) {
        View v = getContainerView();
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this.mLauncher, animated, toWorkspace, entry);
        }
    }

    private void transitAnimStart(boolean animated, boolean toWorkspace, StageEntry entry) {
        View v = getContainerView();
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this.mLauncher, animated, toWorkspace, entry);
        }
    }

    private void transitAnimEnd(boolean animated, boolean toWorkspace, StageEntry entry) {
        View v = getContainerView();
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this.mLauncher, animated, toWorkspace, entry);
        }
    }

    public void setDataWithOutStageChange(StageEntry data) {
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    StageConfig getStageConfig() {
        return this.mConfig;
    }

    void setStageConfig(StageConfig newConfig) {
        this.mConfig.mOrientation = newConfig.mOrientation;
        this.mConfig.mMobileKeyboard = newConfig.mMobileKeyboard;
        this.mConfig.mWidthDp = newConfig.mWidthDp;
        this.mConfig.mHeightDp = newConfig.mHeightDp;
    }

    protected void onStagePreEnter() {
        StageManagerConfigHelper helper = getStageManager().getConfigHelper();
        if (this.mIsNeedConfigurationChange || helper.isConfigDifferentFromActivity(this.mConfig)) {
            setStageConfig(helper.makeStageConfigByHelper());
            setPaddingForNavigationBarIfNeeded();
            onConfigurationChangedIfNeeded();
            this.mIsNeedConfigurationChange = false;
            return;
        }
        if (getMode() == 1) {
            DataLoader.reinflateWidgetsIfNecessary();
        }
        setPaddingForNavigationBarIfNeeded();
    }

    public void onStageEnterDragState(boolean animated) {
    }

    public void checkIfConfigIsDifferentFromActivity() {
        if (!isTopStage() && !this.mIsNeedConfigurationChange && getStageManager().getConfigHelper().isConfigDifferentFromActivity(this.mConfig)) {
            this.mIsNeedConfigurationChange = true;
        }
    }

    private boolean isTopStage() {
        return equals(getStageManager().getTopStage());
    }

    protected void setPaddingForNavigationBarIfNeeded(View view, View pagedView) {
        if (LauncherFeature.supportNavigationBar() && this.mNavigationBarPosition != Utilities.getNavigationBarPositon()) {
            this.mNavigationBarPosition = Utilities.getNavigationBarPositon();
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (this.mNavigationBarPosition == 0) {
                view.setPadding(0, view.getPaddingTop(), 0, dp.navigationBarHeight);
            } else if (this.mNavigationBarPosition != 1) {
                view.setPadding(0, view.getPaddingTop(), dp.navigationBarHeight, 0);
            } else if (dp.isMultiwindowMode) {
                view.setPadding(0, view.getPaddingTop(), 0, 0);
            } else {
                view.setPadding(dp.navigationBarHeight, view.getPaddingTop(), 0, 0);
            }
            if (pagedView instanceof PagedView) {
                ((PagedView) pagedView).updateMarginForPageIndicator();
            }
        }
    }
}
