package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

public class InterruptibleInOutAnimator {
    private static final int IN = 1;
    private static final int OUT = 2;
    private static final int STOPPED = 0;
    private ValueAnimator mAnimator;
    private int mDirection = 0;
    private boolean mFirstRun = true;
    private long mOriginalDuration;
    private float mOriginalFromValue;
    private float mOriginalToValue;
    private Object mTag = null;

    public InterruptibleInOutAnimator(View view, long duration, float fromValue, float toValue) {
        this.mAnimator = LauncherAnimUtils.ofFloat(view, fromValue, toValue).setDuration(duration);
        this.mOriginalDuration = duration;
        this.mOriginalFromValue = fromValue;
        this.mOriginalToValue = toValue;
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                InterruptibleInOutAnimator.this.mDirection = 0;
            }
        });
    }

    private void animate(int direction) {
        float startValue;
        long currentPlayTime = this.mAnimator.getCurrentPlayTime();
        float toValue = direction == 1 ? this.mOriginalToValue : this.mOriginalFromValue;
        if (this.mFirstRun) {
            startValue = this.mOriginalFromValue;
        } else {
            startValue = ((Float) this.mAnimator.getAnimatedValue()).floatValue();
        }
        cancel();
        this.mDirection = direction;
        this.mAnimator.setDuration(Math.max(0, Math.min(this.mOriginalDuration - currentPlayTime, this.mOriginalDuration)));
        this.mAnimator.setFloatValues(new float[]{startValue, toValue});
        this.mAnimator.start();
        this.mFirstRun = false;
    }

    public void cancel() {
        this.mAnimator.cancel();
        this.mDirection = 0;
    }

    public void end() {
        this.mAnimator.end();
        this.mDirection = 0;
    }

    public boolean isStopped() {
        return this.mDirection == 0;
    }

    public void animateIn() {
        animate(1);
    }

    public void animateOut() {
        animate(2);
    }

    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public Object getTag() {
        return this.mTag;
    }

    public ValueAnimator getAnimator() {
        return this.mAnimator;
    }
}
