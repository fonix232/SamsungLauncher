package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import com.android.launcher3.util.ElasticEaseOut;
import com.android.launcher3.util.ViInterpolator;

public class AppIconBounceAnimation {
    private static final int BOUNCE_ANIM_DOWN_DUR = 200;
    public static final float BOUNCE_ANIM_SCALE = 0.9f;
    private static final int BOUNCE_ANIM_UP_DUR = 500;
    private ValueAnimator mAnimator;
    private View mIconView;
    private float mLastScale;
    private View mThumbnailView;

    public AppIconBounceAnimation(View icon) {
        this(icon, null);
    }

    public AppIconBounceAnimation(View icon, float initScale) {
        this(icon);
        this.mLastScale = initScale;
    }

    public AppIconBounceAnimation(View icon, View thumbnail) {
        this.mIconView = icon;
        this.mThumbnailView = thumbnail;
    }

    public void animateDown() {
        cancel();
        this.mAnimator = LauncherAnimUtils.ofFloat(this.mIconView, 1.0f, 0.9f);
        this.mAnimator.setDuration(200);
        this.mAnimator.setInterpolator(ViInterpolator.getInterploator(32));
        this.mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float r = ((Float) animation.getAnimatedValue()).floatValue();
                AppIconBounceAnimation.this.mIconView.setScaleX(r);
                AppIconBounceAnimation.this.mIconView.setScaleY(r);
                if (AppIconBounceAnimation.this.mThumbnailView != null) {
                    AppIconBounceAnimation.this.mThumbnailView.setScaleX(r);
                    AppIconBounceAnimation.this.mThumbnailView.setScaleY(r);
                }
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                AppIconBounceAnimation.this.mLastScale = AppIconBounceAnimation.this.mIconView.getScaleX();
                AppIconBounceAnimation.this.mAnimator = null;
            }
        });
        this.mAnimator.start();
    }

    public void animateUp() {
        cancel();
        this.mAnimator = LauncherAnimUtils.ofFloat(this.mIconView, this.mLastScale, 1.0f);
        this.mAnimator.setDuration(500);
        this.mAnimator.setInterpolator(new ElasticEaseOut(1.0f, 0.4f));
        this.mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float r = ((Float) animation.getAnimatedValue()).floatValue();
                AppIconBounceAnimation.this.mIconView.setScaleX(r);
                AppIconBounceAnimation.this.mIconView.setScaleY(r);
                if (AppIconBounceAnimation.this.mThumbnailView != null) {
                    AppIconBounceAnimation.this.mThumbnailView.setScaleX(r);
                    AppIconBounceAnimation.this.mThumbnailView.setScaleY(r);
                }
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled = false;

            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    AppIconBounceAnimation.this.mIconView.setScaleX(1.0f);
                    AppIconBounceAnimation.this.mIconView.setScaleY(1.0f);
                    if (AppIconBounceAnimation.this.mThumbnailView != null) {
                        AppIconBounceAnimation.this.mThumbnailView.setScaleX(1.0f);
                        AppIconBounceAnimation.this.mThumbnailView.setScaleY(1.0f);
                    }
                } else {
                    AppIconBounceAnimation.this.mIconView.animate().scaleX(1.0f);
                    AppIconBounceAnimation.this.mIconView.animate().scaleY(1.0f);
                    if (AppIconBounceAnimation.this.mThumbnailView != null) {
                        AppIconBounceAnimation.this.mThumbnailView.animate().scaleX(1.0f);
                        AppIconBounceAnimation.this.mThumbnailView.animate().scaleY(1.0f);
                    }
                }
                AppIconBounceAnimation.this.mAnimator = null;
            }

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }
        });
        this.mAnimator.start();
    }

    public void cancel() {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
            this.mIconView.animate().cancel();
            if (this.mThumbnailView != null) {
                this.mThumbnailView.animate().cancel();
            }
        }
    }
}
