package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import java.util.ArrayList;
import java.util.EnumSet;

public class LauncherViewPropertyAnimator extends Animator implements AnimatorListener {
    float mAlpha;
    long mDuration;
    FirstFrameAnimatorHelper mFirstFrameHelper;
    TimeInterpolator mInterpolator;
    ArrayList<AnimatorListener> mListeners = new ArrayList();
    EnumSet<Properties> mPropertiesToSet = EnumSet.noneOf(Properties.class);
    float mRotationY;
    boolean mRunning = false;
    float mScaleX;
    float mScaleY;
    long mStartDelay;
    View mTarget;
    float mTranslationX;
    float mTranslationY;
    ViewPropertyAnimator mViewPropertyAnimator;

    enum Properties {
        TRANSLATION_X,
        TRANSLATION_Y,
        SCALE_X,
        SCALE_Y,
        ROTATION_Y,
        ALPHA,
        START_DELAY,
        DURATION,
        INTERPOLATOR,
        WITH_LAYER
    }

    public LauncherViewPropertyAnimator(View target) {
        this.mTarget = target;
    }

    public void addListener(AnimatorListener listener) {
        this.mListeners.add(listener);
    }

    public void cancel() {
        if (this.mViewPropertyAnimator != null) {
            this.mViewPropertyAnimator.cancel();
        }
    }

    public Animator clone() {
        throw new RuntimeException("Not implemented");
    }

    public void end() {
        throw new RuntimeException("Not implemented");
    }

    public long getDuration() {
        return this.mDuration;
    }

    public ArrayList<AnimatorListener> getListeners() {
        return this.mListeners;
    }

    public long getStartDelay() {
        return this.mStartDelay;
    }

    public void onAnimationCancel(Animator animation) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AnimatorListener) this.mListeners.get(i)).onAnimationCancel(this);
        }
        this.mRunning = false;
    }

    public void onAnimationEnd(Animator animation) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AnimatorListener) this.mListeners.get(i)).onAnimationEnd(this);
        }
        this.mRunning = false;
    }

    public void onAnimationRepeat(Animator animation) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AnimatorListener) this.mListeners.get(i)).onAnimationRepeat(this);
        }
    }

    public void onAnimationStart(Animator animation) {
        this.mFirstFrameHelper.onAnimationStart(animation);
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AnimatorListener) this.mListeners.get(i)).onAnimationStart(this);
        }
        this.mRunning = true;
    }

    public boolean isRunning() {
        return this.mRunning;
    }

    public boolean isStarted() {
        return this.mViewPropertyAnimator != null;
    }

    public void removeAllListeners() {
        this.mListeners.clear();
    }

    public void removeListener(AnimatorListener listener) {
        this.mListeners.remove(listener);
    }

    public Animator setDuration(long duration) {
        this.mPropertiesToSet.add(Properties.DURATION);
        this.mDuration = duration;
        return this;
    }

    public void setInterpolator(TimeInterpolator value) {
        this.mPropertiesToSet.add(Properties.INTERPOLATOR);
        this.mInterpolator = value;
    }

    public void setStartDelay(long startDelay) {
        this.mPropertiesToSet.add(Properties.START_DELAY);
        this.mStartDelay = startDelay;
    }

    public void setTarget(Object target) {
        throw new RuntimeException("Not implemented");
    }

    public void setupEndValues() {
    }

    public void setupStartValues() {
    }

    public void start() {
        this.mViewPropertyAnimator = this.mTarget.animate();
        this.mFirstFrameHelper = new FirstFrameAnimatorHelper(this.mViewPropertyAnimator, this.mTarget);
        if (this.mPropertiesToSet.contains(Properties.TRANSLATION_X)) {
            this.mViewPropertyAnimator.translationX(this.mTranslationX);
        }
        if (this.mPropertiesToSet.contains(Properties.TRANSLATION_Y)) {
            this.mViewPropertyAnimator.translationY(this.mTranslationY);
        }
        if (this.mPropertiesToSet.contains(Properties.SCALE_X)) {
            this.mViewPropertyAnimator.scaleX(this.mScaleX);
        }
        if (this.mPropertiesToSet.contains(Properties.ROTATION_Y)) {
            this.mViewPropertyAnimator.rotationY(this.mRotationY);
        }
        if (this.mPropertiesToSet.contains(Properties.SCALE_Y)) {
            this.mViewPropertyAnimator.scaleY(this.mScaleY);
        }
        if (this.mPropertiesToSet.contains(Properties.ALPHA)) {
            this.mViewPropertyAnimator.alpha(this.mAlpha);
        }
        if (this.mPropertiesToSet.contains(Properties.START_DELAY)) {
            this.mViewPropertyAnimator.setStartDelay(this.mStartDelay);
        }
        if (this.mPropertiesToSet.contains(Properties.DURATION)) {
            this.mViewPropertyAnimator.setDuration(this.mDuration);
        }
        if (this.mPropertiesToSet.contains(Properties.INTERPOLATOR)) {
            this.mViewPropertyAnimator.setInterpolator(this.mInterpolator);
        }
        if (this.mPropertiesToSet.contains(Properties.WITH_LAYER)) {
            this.mViewPropertyAnimator.withLayer();
        }
        this.mViewPropertyAnimator.setListener(this);
        this.mViewPropertyAnimator.start();
        LauncherAnimUtils.cancelOnDestroyActivity(this);
    }

    public LauncherViewPropertyAnimator translationX(float value) {
        this.mPropertiesToSet.add(Properties.TRANSLATION_X);
        this.mTranslationX = value;
        return this;
    }

    public LauncherViewPropertyAnimator translationY(float value) {
        this.mPropertiesToSet.add(Properties.TRANSLATION_Y);
        this.mTranslationY = value;
        return this;
    }

    public LauncherViewPropertyAnimator scaleX(float value) {
        this.mPropertiesToSet.add(Properties.SCALE_X);
        this.mScaleX = value;
        return this;
    }

    public LauncherViewPropertyAnimator scaleY(float value) {
        this.mPropertiesToSet.add(Properties.SCALE_Y);
        this.mScaleY = value;
        return this;
    }

    public LauncherViewPropertyAnimator rotationY(float value) {
        this.mPropertiesToSet.add(Properties.ROTATION_Y);
        this.mRotationY = value;
        return this;
    }

    public LauncherViewPropertyAnimator alpha(float value) {
        this.mPropertiesToSet.add(Properties.ALPHA);
        this.mAlpha = value;
        return this;
    }

    public LauncherViewPropertyAnimator withLayer() {
        this.mPropertiesToSet.add(Properties.WITH_LAYER);
        return this;
    }
}
