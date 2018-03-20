package com.android.launcher3.common.quickoption;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import com.android.launcher3.util.animation.LauncherAnimUtils;

class ItemBounceAnimation {
    private final float finalDeltaY;
    private float initDeltaY;
    private ValueAnimator mAnimator;
    private final View mView;

    ItemBounceAnimation(View v, boolean needSmallDelta) {
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
                ItemBounceAnimation.this.mView.setTranslationY((ItemBounceAnimation.this.finalDeltaY * r) + ((1.0f - r) * ItemBounceAnimation.this.initDeltaY));
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationRepeat(Animator animation) {
                ItemBounceAnimation.this.initDeltaY = 0.0f;
            }

            public void onAnimationEnd(Animator animation) {
                ItemBounceAnimation.this.mView.animate().translationY(ItemBounceAnimation.this.initDeltaY);
            }
        });
        this.mAnimator.start();
    }

    public void stop() {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        this.mAnimator = null;
    }
}
