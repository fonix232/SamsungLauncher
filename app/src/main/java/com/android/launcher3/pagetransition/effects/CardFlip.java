package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;

public class CardFlip extends PageTransitionEffects {
    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            CellLayout cl = (CellLayout) page;
            float alpha = Math.min(1.0f, 1.0f - Math.abs(scrollProgress));
            cl.invalidate();
            float translationX = scrollProgress * ((float) cl.getWidth());
            int pageWidth = cl.getMeasuredWidth();
            if (scrollProgress <= 0.0f) {
                cl.setTranslationX(translationX);
                cl.setScaleX(Math.max(0.5f, alpha));
                cl.setScaleY(Math.max(0.5f, alpha));
                cl.setAlpha(alpha);
                return;
            }
            cl.setTranslationX(translationX - (((1.0f - alpha) * ((float) pageWidth)) * 2.0f));
            cl.setPivotX((float) pageWidth);
            cl.setRotationY((-(1.0f - alpha)) * 50.0f);
        }
    }
}
