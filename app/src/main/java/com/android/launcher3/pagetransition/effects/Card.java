package com.android.launcher3.pagetransition.effects;

import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.pagetransition.PageTransitionManager;
import com.android.launcher3.util.ViInterpolator;

public class Card extends PageTransitionEffects {
    private final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private Interpolator mSineIO70Interpolator = ViInterpolator.getInterploator(33);
    private Interpolator mSineIO80Interpolator = ViInterpolator.getInterploator(34);

    public void reset(View page) {
        super.reset(page);
        ((CellLayout) page).setBackgroundAlpha(0.0f);
    }

    public void applyTransform(View page, float scrollProgress, int index) {
        boolean BOTTOM_CARD = scrollProgress < 0.0f;
        CellLayout cl = (CellLayout) page;
        int i = index;
        float absScrollProgress = Math.abs(scrollProgress);
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            cl.invalidate();
            float rotation = 0.0f;
            float translationX = 0.0f;
            float scrollAlpha = backgroundAlphaThreshold(absScrollProgress);
            float panelAlpha = 1.0f;
            if (BOTTOM_CARD) {
                translationX = scrollProgress * ((float) cl.getWidth());
                if (absScrollProgress < 0.67f) {
                    panelAlpha = this.mSineIO80Interpolator.getInterpolation((0.67f - absScrollProgress) / (1.0f - 0.33f));
                } else {
                    panelAlpha = 0.0f;
                }
            }
            cl.setBackgroundAlpha(PageTransitionEffects.mix(scrollAlpha, 1.0f, PageTransitionManager.getPageBackgroundAlpha()));
            float zoom = 1.0f - (0.1f * this.mSineIO80Interpolator.getInterpolation(absScrollProgress));
            if (!isLoopingEnabled()) {
                int pageWidth = cl.getMeasuredWidth();
                int pageHeight = cl.getMeasuredHeight();
                float vtransition_rotation_max = transition_rotation_max;
                if (i == 0 && scrollProgress < 0.0f) {
                    cl.setPivotX(TRANSITION_PIVOT * ((float) pageWidth));
                    rotation = ((-vtransition_rotation_max) * scrollProgress) / maxOverScroll();
                    zoom = 1.0f;
                    translationX = PageTransitionManager.getScrollX();
                    panelAlpha = 1.0f;
                } else if (i != PageTransitionManager.getChildCount() - 1 || scrollProgress <= 0.0f) {
                    cl.setPivotY(((float) pageHeight) / 2.0f);
                    cl.setPivotX(((float) pageWidth) / 2.0f);
                } else {
                    translationX = PageTransitionManager.getScrollX() - PageTransitionManager.getMaxScrollX();
                    zoom = 1.0f - ((0.1f * this.mSineIO70Interpolator.getInterpolation(absScrollProgress)) / maxOverScroll());
                    cl.setScaleX(zoom);
                    cl.setScaleY(zoom);
                    panelAlpha = 1.0f;
                }
            }
            cl.setAlpha(panelAlpha);
            if (BOTTOM_CARD) {
                cl.setScaleX(zoom);
                cl.setScaleY(zoom);
            }
            cl.setTranslationX(translationX);
            cl.setRotationY(rotation);
        }
    }
}
