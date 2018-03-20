package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.ViewOutlineProvider;

public class UiThreadCircularReveal {
    public static ValueAnimator createCircularReveal(View v, int x, int y, float r0, float r1) {
        return createCircularReveal(v, x, y, r0, r1, ViewOutlineProvider.BACKGROUND);
    }

    public static ValueAnimator createCircularReveal(View v, int x, int y, float r0, float r1, final ViewOutlineProvider originalProvider) {
        ValueAnimator va = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final View revealView = v;
        final RevealOutlineProvider outlineProvider = new RevealOutlineProvider(x, y, r0, r1);
        final float elevation = v.getElevation();
        va.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                revealView.setOutlineProvider(outlineProvider);
                revealView.setClipToOutline(true);
                revealView.setTranslationZ(-elevation);
            }

            public void onAnimationEnd(Animator animation) {
                revealView.setOutlineProvider(originalProvider);
                revealView.setClipToOutline(false);
                revealView.setTranslationZ(0.0f);
            }
        });
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator arg0) {
                outlineProvider.setProgress(arg0.getAnimatedFraction());
                revealView.invalidateOutline();
            }
        });
        return va;
    }
}
