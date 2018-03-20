package com.android.launcher3.pagetransition.effects;

import android.view.View;

public class Fan extends PageTransitionEffects {
    private static final float FAST_ROTATION = 15.0f;
    private static final float SLOW_ROTATION = 20.0f;
    private static final float WORKSPACE_ROTATION = 20.0f;

    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            page.invalidate();
            float rotation = PageTransitionEffects.mix(20.0f, FAST_ROTATION, 0.0f) * scrollProgress;
            float translationX = (PageTransitionEffects.mix(0.025f, this.mFastScrollDrawInward, 0.0f) * scrollProgress) * ((float) page.getWidth());
            int pageWidth = page.getMeasuredWidth();
            int pageHeight = page.getMeasuredHeight();
            if (scrollProgress > 0.0f) {
                pageWidth = 0;
            }
            page.setPivotX((float) pageWidth);
            page.setPivotY((float) pageHeight);
            page.setTranslationX(translationX);
            page.setRotation(-rotation);
        }
    }
}
