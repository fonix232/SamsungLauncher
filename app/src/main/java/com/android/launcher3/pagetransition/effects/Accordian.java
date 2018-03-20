package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;

public class Accordian extends PageTransitionEffects {
    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            float alpha = Math.min(1.0f, 1.0f - Math.abs(scrollProgress));
            CellLayout cl = (CellLayout) page;
            cl.invalidate();
            float translationX = scrollProgress * ((float) cl.getWidth());
            int pageWidth = cl.getMeasuredWidth();
            if (scrollProgress <= 0.0f) {
                cl.setTranslationX((((1.0f - alpha) * ((float) pageWidth)) / 2.0f) + translationX);
                cl.setScaleX(alpha);
                return;
            }
            cl.setTranslationX(translationX - (((1.0f - alpha) * ((float) pageWidth)) / 2.0f));
            cl.setScaleX(alpha);
        }
    }
}
