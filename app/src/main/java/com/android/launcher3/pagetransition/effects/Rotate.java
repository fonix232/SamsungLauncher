package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;

public class Rotate extends PageTransitionEffects {
    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            float alpha = Math.min(1.0f, 1.0f - Math.abs(scrollProgress));
            CellLayout cl = (CellLayout) page;
            cl.invalidate();
            float translationX = scrollProgress * ((float) cl.getWidth());
            int pageWidth = cl.getMeasuredWidth();
            if (scrollProgress <= 0.0f) {
                cl.setTranslationX(translationX);
                cl.setTranslationX(((1.0f - alpha) * ((float) pageWidth)) + translationX);
                cl.setPivotX(0.0f);
                cl.setRotationY((1.0f - alpha) * 90.0f);
                return;
            }
            cl.setTranslationX(translationX - ((1.0f - alpha) * ((float) pageWidth)));
            cl.setPivotX((float) pageWidth);
            cl.setRotationY((-(1.0f - alpha)) * 90.0f);
        }
    }
}
