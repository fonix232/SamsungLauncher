package com.android.launcher3.common.stage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.LauncherTransitionable;
import com.android.launcher3.home.HomeController;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

public class StageManager {
    private static final String RUNTIME_STAGE_STACK = "RUNTIME_STAGE_STACK";
    private static final String RUNTIME_STAGE_STAGES = "RUNTIME_STAGE_STAGES";
    private static final String TAG = "StageManager";
    private final Launcher mActivity;
    private final StageManagerConfigHelper mConfigHelper;
    private AnimatorSet mCurrentAnimation;
    private final boolean mDumpStack = true;
    private boolean mInProgressTransit = false;
    private final Stack<Stage> mStack = new Stack();
    private final HashMap<Integer, Stage> mStageMap = new HashMap();

    public StageManager(Launcher activity, Bundle savedState) {
        this.mActivity = activity;
        onCreate(savedState);
        this.mConfigHelper = new StageManagerConfigHelper(activity);
        LauncherAppState.getInstance().getLauncherProxy().setStageManagerProxyCallbacks(new StageManagerProxyCallBacksImpl(activity, this));
    }

    public void startStage(int stageMode, StageEntry data) {
        if (data == null) {
            data = new StageEntry();
        }
        data.fromStage = this.mStack.isEmpty() ? 0 : ((Stage) this.mStack.peek()).getMode();
        data.toStage = stageMode;
        Log.i(TAG, "startStage : " + data.fromStage + " -> " + data.toStage);
        changeState(stageMode, data);
    }

    public void startStageByTray(int stageMode) {
        changeState(stageMode, null);
    }

    private void changeState(int stageMode, StageEntry data) {
        Stage topStage = getTopStage();
        if (topStage == null || topStage.getMode() != stageMode) {
            assertTransitInProgress();
            Stage toStage = (Stage) this.mStageMap.get(Integer.valueOf(stageMode));
            if (toStage == null) {
                toStage = getStage(stageMode);
            }
            if (!toStage.isViewInitiated()) {
                toStage.initStageView();
            }
            cancelAnimation();
            Stage fromStage = null;
            Animator exitAnim = null;
            if (data != null) {
                if (!this.mStack.isEmpty()) {
                    fromStage = (Stage) this.mStack.peek();
                    exitAnim = fromStage.onStageExit(data);
                }
                this.mStack.push(toStage);
                toStage.onStagePreEnter();
                playTransitAnimation(toStage, fromStage, toStage.onStageEnter(data), exitAnim, data);
                return;
            }
            if (!this.mStack.isEmpty()) {
                exitAnim = ((Stage) this.mStack.peek()).onStageExitByTray();
            }
            if (stageMode == 1) {
                this.mStack.pop();
            } else {
                this.mStack.push(toStage);
            }
            playTransitAnimationByTray(stageMode, toStage.onStageEnterByTray(), exitAnim);
            return;
        }
        Log.w(TAG, "changeState : " + stageMode + " is already on top");
    }

    public void finishStage(int stageMode, StageEntry data) {
        Stage toStage = (Stage) this.mStageMap.get(Integer.valueOf(stageMode));
        if (toStage != null) {
            finishStage(toStage, data);
        } else {
            Log.w(TAG, "Invalid finishStage : " + stageMode);
        }
    }

    public void finishStage(Stage stage, StageEntry data) {
        if (this.mStack.size() <= 1 || stage == null) {
            Log.w(TAG, "Invalid finishStage : stack=" + this.mStack.size() + ", " + stage);
        } else if (this.mStack.peek() == stage) {
            assertTransitInProgress();
            Stage currentStage = (Stage) this.mStack.pop();
            Stage nextStage = (Stage) this.mStack.peek();
            if (data == null) {
                data = new StageEntry();
            }
            data.fromStage = currentStage.getMode();
            data.toStage = nextStage != null ? nextStage.getMode() : 0;
            Log.i(TAG, "finishStage : " + data.toStage + " <- " + data.fromStage);
            cancelAnimation();
            Animator exitAnim = currentStage.onStageExit(data);
            Animator enterAnim = null;
            if (nextStage != null) {
                nextStage.onStagePreEnter();
                enterAnim = nextStage.onStageEnter(data);
            }
            playTransitAnimation(nextStage, stage, enterAnim, exitAnim, data);
            dumpStack();
        } else {
            dumpStack();
            throw new IllegalArgumentException("finishStage : " + this.mStack.peek() + " , " + stage);
        }
    }

    public void switchStage(int stageMode, StageEntry data) {
        if (this.mStack.size() <= 1) {
            Log.w(TAG, "Invalid switchStage : stack=" + this.mStack.size() + ", " + stageMode);
            return;
        }
        assertTransitInProgress();
        if (data == null) {
            data = new StageEntry();
        }
        data.fromStage = getTopStage().getMode();
        data.toStage = stageMode;
        Log.i(TAG, "switchStage : " + data.fromStage + " <-> " + data.toStage);
        Stage toStage = (Stage) this.mStageMap.get(Integer.valueOf(stageMode));
        if (toStage == null) {
            toStage = getStage(stageMode);
        }
        if (!toStage.isViewInitiated()) {
            toStage.initStageView();
        }
        cancelAnimation();
        Stage fromStage = (Stage) this.mStack.pop();
        Animator exitAnim = fromStage.onStageExit(data);
        this.mStack.push(toStage);
        toStage.onStagePreEnter();
        playTransitAnimation(toStage, fromStage, toStage.onStageEnter(data), exitAnim, data);
        dumpStack();
    }

    public void switchInternalState(Stage stage, StageEntry data) {
        stage.switchState(data);
    }

    public void finishAllStage(StageEntry data) {
        if (this.mStack.size() <= 1) {
            Log.w(TAG, "Invalid finishAllStage : stack=" + this.mStack.size());
            return;
        }
        assertTransitInProgress();
        if (data == null) {
            data = new StageEntry();
        }
        data.fromStage = getTopStage().getMode();
        data.toStage = ((Stage) this.mStack.get(0)).getMode();
        data.stageCountOnFinishAllStage = this.mStack.size();
        Log.i(TAG, "finishAllStage : " + data.toStage + " <--- " + data.fromStage + " , stack=" + this.mStack.size());
        cancelAnimation();
        Stage oldStage = (Stage) this.mStack.pop();
        Animator exitAnim = oldStage.onStageExit(data);
        while (this.mStack.size() > 1) {
            ((Stage) this.mStack.pop()).onStageMovingToInitial(data);
        }
        ((Stage) this.mStack.peek()).onStagePreEnter();
        playTransitAnimation((Stage) this.mStack.peek(), oldStage, ((Stage) this.mStack.peek()).onStageEnter(data), exitAnim, data);
        dumpStack();
    }

    public void moveToOverHome(Stage stage, StageEntry data) {
        if (this.mStack.size() == 1) {
            startStage(stage.getMode(), data);
        } else if (this.mStack.size() <= 1) {
        } else {
            if (getTopStage() != stage) {
                Log.w(TAG, "can not move to over Home");
                return;
            }
            if (data == null) {
                data = new StageEntry();
            }
            data.enableAnimation = false;
            data.setInternalStateTo(1);
            finishAllStage(data);
            data = new StageEntry();
            data.enableAnimation = false;
            startStage(stage.getMode(), data);
        }
    }

    public void setConfiguration(@Nullable Configuration newConfig) {
        if (this.mConfigHelper.isOrientationChanged(newConfig)) {
            SALogging.getInstance().insertEventLog(this.mActivity.getResources().getString(R.string.screen_Home_1xxx), this.mActivity.getResources().getString(R.string.event_Rotation));
        }
        this.mConfigHelper.mNeedToCallConfigurationChanged = true;
        this.mConfigHelper.setConfig(newConfig);
    }

    public void onConfigurationChanged() {
        Stage top = (Stage) this.mStack.peek();
        if (top != null && this.mConfigHelper.isConfigDifferentFromActivity(top.getStageConfig())) {
            top.setStageConfig(this.mConfigHelper.makeStageConfigByHelper());
            top.onConfigurationChangedIfNeeded();
        }
    }

    public void navigationBarPositionChanged() {
        Iterator it = new ArrayList(this.mStageMap.values()).iterator();
        while (it.hasNext()) {
            Stage stage = (Stage) it.next();
            if (stage != null) {
                stage.mNavigationBarPosition = -1;
            }
        }
        Stage top = (Stage) this.mStack.peek();
        if (top != null) {
            top.setPaddingForNavigationBarIfNeeded();
        }
    }

    public void onRestoreInstanceState(Bundle state) {
        ArrayList<Stage> tobeRestored = new ArrayList(this.mStageMap.values());
        Iterator it = this.mStack.iterator();
        while (it.hasNext()) {
            Stage stage = (Stage) it.next();
            stage.onRestoreInstanceState(state);
            tobeRestored.remove(stage);
        }
        it = tobeRestored.iterator();
        while (it.hasNext()) {
            ((Stage) it.next()).onRestoreInstanceState(state);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        ArrayList<Stage> tobeSaved = new ArrayList(this.mStageMap.values());
        ArrayList<Integer> inStack = new ArrayList();
        Iterator it = this.mStack.iterator();
        while (it.hasNext()) {
            Stage stage = (Stage) it.next();
            if (!stage.isRestorable()) {
                Log.d(TAG, "does not restore remain-stages in stack : " + stage.getMode());
                break;
            }
            inStack.add(Integer.valueOf(stage.getMode()));
            stage.onSaveInstanceState(outState);
            tobeSaved.remove(stage);
        }
        outState.putIntegerArrayList(RUNTIME_STAGE_STACK, inStack);
        ArrayList<Integer> outStages = new ArrayList();
        it = tobeSaved.iterator();
        while (it.hasNext()) {
            stage = (Stage) it.next();
            if (stage.isRestorable()) {
                outStages.add(Integer.valueOf(stage.getMode()));
                stage.onSaveInstanceState(outState);
            }
        }
        outState.putIntegerArrayList(RUNTIME_STAGE_STAGES, outStages);
        Log.d(TAG, "saved stages : inStack " + inStack + " , outStack" + outStages);
    }

    public void restoreState(Bundle savedState) {
        if (this.mStack.isEmpty()) {
            restoreStack(savedState, true);
        }
        int topMode = getTopStage().getMode();
        Iterator it = this.mStack.iterator();
        while (it.hasNext()) {
            Stage stage = (Stage) it.next();
            if (!stage.mViewInitiated) {
                stage.initStageView();
            }
            stage.restoreState(savedState, topMode == stage.getMode());
        }
        restoreStagesOutOfStack(savedState);
        StageEntry data = new StageEntry();
        data.enableAnimation = false;
        startStage(((Stage) this.mStack.pop()).getMode(), data);
    }

    private void restoreStack(Bundle savedState, boolean initView) {
        ArrayList<Integer> restoredStack = savedState.getIntegerArrayList(RUNTIME_STAGE_STACK);
        if (restoredStack != null) {
            Iterator it = restoredStack.iterator();
            while (it.hasNext()) {
                Integer mode = (Integer) it.next();
                if (mode.intValue() != 2 || !LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    this.mStack.push(getStage(mode.intValue(), initView));
                } else {
                    return;
                }
            }
        }
    }

    private void restoreStagesOutOfStack(Bundle savedState) {
        ArrayList<Integer> restoreStages = savedState.getIntegerArrayList(RUNTIME_STAGE_STAGES);
        if (restoreStages != null) {
            Iterator it = restoreStages.iterator();
            while (it.hasNext()) {
                Integer mode = (Integer) it.next();
                if (mode.intValue() != 2 || !LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    Stage stage = getStage(mode.intValue(), true);
                    stage.restoreState(savedState, false);
                    this.mStageMap.put(mode, stage);
                }
            }
        }
    }

    public Stage getTopStage() {
        if (this.mStack.isEmpty()) {
            return null;
        }
        return (Stage) this.mStack.peek();
    }

    public Stage getSecondTopStage() {
        if (this.mStack.size() >= 2) {
            try {
                return (Stage) this.mStack.get(this.mStack.size() - 2);
            } catch (Exception e) {
                Log.e(TAG, "mStack.size() = " + this.mStack.size(), e);
            }
        }
        return null;
    }

    private void onCreate(Bundle savedState) {
        if (savedState != null) {
            restoreStack(savedState, false);
        }
        getStage(1);
        if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            getStage(2);
        }
        getStage(6);
    }

    public void setupStartupViews() {
        getStage(1).initStageView();
        if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            getStage(2).initStageView();
        }
    }

    public void onStart() {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onStartActivity();
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onStartActivity();
            }
        }
    }

    public void onResume() {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onResumeActivity();
            topStage.updateSystemUIForState(topStage.getInternalState(), null, -1);
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onResumeActivity();
            }
        }
    }

    public void onPause() {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onPauseActivity();
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onPauseActivity();
            }
        }
    }

    public void onStop() {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onStopActivity();
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onStopActivity();
            }
        }
    }

    public void onDestroy() {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onDestroyActivity();
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onDestroyActivity();
            }
        }
        this.mStageMap.clear();
        this.mStack.clear();
    }

    public boolean onPrepareOptionMenu(Menu menu) {
        return getTopStage() != null && getTopStage().onPrepareOptionsMenu(menu);
    }

    public void onBackPressed() {
        if (!this.mStack.isEmpty()) {
            if (this.mActivity.getMultiSelectManager().isMultiSelectMode()) {
                SALogging.getInstance().insertMultiSelectCancelLog(this.mActivity, true, false);
            }
            if (!getTopStage().onBackPressed()) {
                finishStage(getTopStage(), null);
            }
        }
    }

    public void onStartForResult(int requestCode) {
        if (!this.mStack.isEmpty()) {
            Log.d(TAG, "onStartForResult : result " + requestCode + " to " + getTopStage());
            getTopStage().onStartForResult(requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!this.mStack.isEmpty()) {
            Log.d(TAG, "onActivityResult : result " + resultCode + " to " + getTopStage());
            getTopStage().onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return !this.mStack.isEmpty() && getTopStage().dispatchKeyEvent(event);
    }

    public void onChangeColorForBg(boolean whiteBg) {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onChangeColorForBg(whiteBg);
        }
        for (Stage stage : this.mStageMap.values()) {
            if (topStage != stage) {
                stage.onChangeColorForBg(whiteBg);
            }
        }
    }

    public void onClick(View v) {
        if (getTopStage() != null) {
            getTopStage().onClick(v);
        }
    }

    public void onCheckedChanged(View view, boolean isChecked) {
        Stage topStage = getTopStage();
        if (topStage != null) {
            topStage.onCheckedChanged(view, isChecked);
        }
    }

    public void onChangeGrid() {
        ArrayList<Stage> tobeNotify = new ArrayList(this.mStageMap.values());
        Iterator it = this.mStack.iterator();
        while (it.hasNext()) {
            Stage stage = (Stage) it.next();
            stage.onChangeGrid();
            tobeNotify.remove(stage);
        }
        it = tobeNotify.iterator();
        while (it.hasNext()) {
            ((Stage) it.next()).onChangeGrid();
        }
    }

    public void deliverDataWithOutStageChange(int stageMode, StageEntry entry) {
        ((Stage) this.mStageMap.get(Integer.valueOf(stageMode))).setDataWithOutStageChange(entry);
    }

    public Stage getStage(int stageMode) {
        return getStage(stageMode, false);
    }

    private Stage getStage(int stageMode, boolean initView) {
        Stage stage = (Stage) this.mStageMap.get(Integer.valueOf(stageMode));
        if (stage == null) {
            try {
                stage = StageFactory.buildStage(stageMode);
                stage.initialize(this.mActivity, stageMode);
                stage.setup();
                if (stage.keepInstance()) {
                    this.mStageMap.put(Integer.valueOf(stageMode), stage);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(stageMode + " fail to create instance");
            }
        }
        if (initView && !stage.mViewInitiated) {
            stage.initStageView();
        }
        return stage;
    }

    private void playTransitAnimation(Stage toStage, Stage fromStage, Animator enterAnim, Animator exitAnim, StageEntry data) {
        Log.d(TAG, "start stageTransitionAnim : from " + fromStage + " to " + toStage);
        if (LauncherFeature.supportQuickOption() && this.mActivity.getDragMgr().isQuickOptionShowing()) {
            this.mActivity.getDragMgr().removeQuickOptionView();
        }
        if (enterAnim == null && exitAnim == null) {
            transitAnimPrepare(toStage, false, toStage instanceof HomeController, data);
            transitAnimStart(toStage, false, toStage instanceof HomeController, data);
            transitAnimEnd(toStage, false, toStage instanceof HomeController, data);
            transitAnimPrepare(fromStage, false, toStage instanceof HomeController, data);
            transitAnimStart(fromStage, false, toStage instanceof HomeController, data);
            transitAnimEnd(fromStage, false, toStage instanceof HomeController, data);
            toStage.updateSystemUIForState(data.getInternalStateTo(), null, 0);
            data.notifyOnCompleteRunnables();
            dumpStageView(toStage);
        } else {
            AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
            animatorSet.play(enterAnim);
            animatorSet.play(exitAnim);
            this.mCurrentAnimation = animatorSet;
            transitAnimPrepare(toStage, enterAnim != null, toStage instanceof HomeController, data);
            transitAnimPrepare(fromStage, exitAnim != null, toStage instanceof HomeController, data);
            final Stage stage = toStage;
            final Animator animator = enterAnim;
            final StageEntry stageEntry = data;
            final Stage stage2 = fromStage;
            animatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    boolean z;
                    Log.d(StageManager.TAG, "stageTransitionAnim onAnimationEnd");
                    StageManager.this.cleanupAnimation();
                    StageManager stageManager = StageManager.this;
                    Stage stage = stage;
                    if (animator != null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    stageManager.transitAnimEnd(stage, z, stage instanceof HomeController, stageEntry);
                    stageManager = StageManager.this;
                    stage = stage2;
                    if (animator != null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    stageManager.transitAnimEnd(stage, z, stage instanceof HomeController, stageEntry);
                    stageEntry.notifyOnCompleteRunnables();
                    for (View v : stageEntry.getLayerViews().keySet()) {
                        if (((Integer) stageEntry.getLayerViews().get(v)).intValue() == 1) {
                            v.setLayerType(0, null);
                        }
                    }
                    StageManager.this.dumpStageView(stage);
                }
            });
            long animDuration = Utilities.getAnimationDuration(enterAnim);
            if (animDuration < 0) {
                animDuration = Utilities.getAnimationDuration(exitAnim);
            }
            toStage.updateSystemUIForState(data.getInternalStateTo(), animatorSet, animDuration);
            if (this.mCurrentAnimation != animatorSet) {
                animatorSet.cancel();
                Log.d(TAG, "fail to enter " + toStage + " from " + fromStage);
                dumpStack();
                return;
            }
            transitAnimStart(fromStage, enterAnim != null, toStage instanceof HomeController, data);
            transitAnimStart(toStage, enterAnim != null, toStage instanceof HomeController, data);
            for (View v : data.getLayerViews().keySet()) {
                if (((Integer) data.getLayerViews().get(v)).intValue() == 1) {
                    new DvfsUtil(this.mActivity).boostOneFrame();
                    v.setLayerType(2, null);
                }
                if (v.isAttachedToWindow()) {
                    v.buildLayer();
                }
            }
            animatorSet.start();
        }
        this.mInProgressTransit = false;
        dumpStack();
    }

    private void playTransitAnimationByTray(final int stageMode, Animator enterAnim, Animator exitAnim) {
        if (!(enterAnim == null && exitAnim == null)) {
            AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
            animatorSet.play(enterAnim);
            animatorSet.play(exitAnim);
            this.mCurrentAnimation = animatorSet;
            animatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    StageManager.this.cleanupAnimation();
                    if (StageManager.this.mActivity != null && StageManager.this.mActivity.getTrayManager() != null) {
                        StageManager.this.mActivity.getTrayManager().trayMoveEnd(stageMode);
                    }
                }
            });
            animatorSet.start();
        }
        this.mInProgressTransit = false;
    }

    private void cancelAnimation() {
        try {
            if (this.mCurrentAnimation != null) {
                if (!this.mCurrentAnimation.isStarted()) {
                    this.mCurrentAnimation.setDuration(0);
                    this.mCurrentAnimation.start();
                }
                this.mCurrentAnimation.cancel();
                this.mCurrentAnimation = null;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "mCurrentAnimation is null");
        }
    }

    private void cleanupAnimation() {
        this.mCurrentAnimation = null;
    }

    public boolean isRunningAnimation() {
        return this.mCurrentAnimation != null;
    }

    public int getStackSize() {
        return this.mStack.size();
    }

    private void transitAnimPrepare(Stage stage, boolean animated, boolean toWorkspace, StageEntry data) {
        if (stage != null) {
            dispatchOnLauncherTransitionPrepare(stage.getContainerView(), animated, toWorkspace, data);
        }
    }

    private void transitAnimStart(Stage stage, boolean animated, boolean toWorkspace, StageEntry data) {
        if (stage != null) {
            dispatchOnLauncherTransitionStart(stage.getContainerView(), animated, toWorkspace, data);
        }
    }

    private void transitAnimEnd(Stage stage, boolean animated, boolean toWorkspace, StageEntry data) {
        if (stage != null) {
            dispatchOnLauncherTransitionEnd(stage.getContainerView(), animated, toWorkspace, data);
        }
    }

    private void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace, StageEntry data) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this.mActivity, animated, toWorkspace, data);
        }
    }

    private void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace, StageEntry data) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this.mActivity, animated, toWorkspace, data);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace, StageEntry data) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this.mActivity, animated, toWorkspace, data);
        }
    }

    private void assertTransitInProgress() {
        if (this.mInProgressTransit) {
            dumpStack();
            throw new AssertionError();
        } else {
            this.mInProgressTransit = true;
        }
    }

    private void dumpStack() {
        Log.d(TAG, "current stack : ");
        StringBuilder out = new StringBuilder("dump stage\n");
        Iterator it = this.mStack.iterator();
        while (it.hasNext()) {
            out.append((Stage) it.next());
            out.append("\n");
        }
        Log.d(TAG, out.toString());
    }

    private void dumpStageView(Stage stage) {
        if (stage == null || stage.getContainerView() == null) {
            Log.w(TAG, "dump stageview info fail : " + stage);
            return;
        }
        View view = stage.getContainerView();
        Log.d(TAG, String.format("dump stageview info : mode(%d), visible(%d) , alpha(%f)", new Object[]{Integer.valueOf(stage.getMode()), Integer.valueOf(view.getVisibility()), Float.valueOf(view.getAlpha())}));
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return !this.mStack.isEmpty() && getTopStage().dispatchPopulateAccessibilityEvent(event);
    }

    StageManagerConfigHelper getConfigHelper() {
        return this.mConfigHelper;
    }

    public boolean needToCallConfigurationChanged() {
        boolean rtn = this.mConfigHelper.mNeedToCallConfigurationChanged;
        this.mConfigHelper.mNeedToCallConfigurationChanged = false;
        return rtn;
    }
}
