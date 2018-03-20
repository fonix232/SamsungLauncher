package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.launcher3.Utilities;

public abstract class RevealOutlineAnimation extends ViewOutlineProvider {
    protected Rect mOutline = new Rect();
    protected float mOutlineRadius;

    abstract void setProgress(float f);

    abstract boolean shouldRemoveElevationDuringAnimation();

    public ValueAnimator createRevealAnimator(View revealView) {
        return createRevealAnimator(revealView, false);
    }

    public ValueAnimator createRevealAnimator(final View revealView, boolean isReversed) {
        ValueAnimator va = isReversed ? ValueAnimator.ofFloat(new float[]{1.0f, 0.0f}) : ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final float elevation = revealView.getElevation();
        va.addListener(new AnimatorListenerAdapter() {
            private boolean mWasCanceled = false;

            public void onAnimationStart(Animator animation) {
                revealView.setOutlineProvider(RevealOutlineAnimation.this);
                revealView.setClipToOutline(true);
                if (RevealOutlineAnimation.this.shouldRemoveElevationDuringAnimation()) {
                    revealView.setTranslationZ(-elevation);
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.mWasCanceled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (!this.mWasCanceled) {
                    revealView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                    revealView.setClipToOutline(false);
                    if (RevealOutlineAnimation.this.shouldRemoveElevationDuringAnimation()) {
                        revealView.setTranslationZ(0.0f);
                    }
                }
            }
        });
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator arg0) {
                RevealOutlineAnimation.this.setProgress(((Float) arg0.getAnimatedValue()).floatValue());
                revealView.invalidateOutline();
                if (!Utilities.ATLEAST_LOLLIPOP) {
                    revealView.invalidate();
                }
            }
        });
        return va;
    }

    public void getOutline(View v, Outline outline) {
        outline.setRoundRect(this.mOutline, this.mOutlineRadius);
    }
}
