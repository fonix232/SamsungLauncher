package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.util.Log;
import android.view.View;

public class SearchedAppBounceAnimation {
    float finalDeltaY;
    float initDeltaY;
    ValueAnimator mAnimator;
    View mView;

    public SearchedAppBounceAnimation(View v, boolean needSmallDelta) {
        this.mView = v;
        this.initDeltaY = v.getTranslationY();
        this.finalDeltaY = needSmallDelta ? 7.0f : 15.0f;
    }

    public void animate() {
        this.mAnimator = LauncherAnimUtils.ofFloat(this.mView, 0.0f, 1.0f);
        this.mAnimator.setRepeatMode(2);
        this.mAnimator.setRepeatCount(-1);
        this.mAnimator.setDuration(300);
        this.mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float r = ((Float) animation.getAnimatedValue()).floatValue();
                SearchedAppBounceAnimation.this.mView.setTranslationY((SearchedAppBounceAnimation.this.finalDeltaY * r) + ((1.0f - r) * SearchedAppBounceAnimation.this.initDeltaY));
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationRepeat(Animator animation) {
                SearchedAppBounceAnimation.this.initDeltaY = 0.0f;
            }

            public void onAnimationEnd(Animator animation) {
                Log.d("JYK", "SearchedAppBounceAnimation end ");
                SearchedAppBounceAnimation.this.mView.animate().translationY(SearchedAppBounceAnimation.this.initDeltaY);
            }
        });
        this.mAnimator.start();
    }

    public void stop() {
        Log.d("JYK", "SearchedAppBounceAnimation stop is called");
        if (this.mAnimator != null) {
            Log.d("JYK", "SearchedAppBounceAnimation stop ");
            this.mAnimator.cancel();
        }
        this.mAnimator = null;
    }
}
