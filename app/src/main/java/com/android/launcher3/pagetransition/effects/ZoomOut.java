package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;

public class ZoomOut extends PageTransitionEffects {
    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            CellLayout cl = (CellLayout) page;
            float alpha = Math.min(1.0f, 1.0f - Math.abs(scrollProgress));
            cl.invalidate();
            float translationX = scrollProgress * ((float) cl.getWidth());
            int pageWidth = cl.getMeasuredWidth();
            cl.setScaleX(alpha);
            cl.setScaleY(alpha);
            cl.setTranslationX(translationX);
            if (scrollProgress <= 0.0f) {
                cl.setPivotX((float) pageWidth);
            } else {
                cl.setPivotX(0.0f);
            }
        }
    }
}
