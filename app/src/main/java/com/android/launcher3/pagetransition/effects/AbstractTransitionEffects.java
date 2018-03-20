package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.pagetransition.PageTransitionManager;

public abstract class AbstractTransitionEffects extends PageTransitionEffects {
    protected abstract float adjustScrollProgress(float f);

    protected abstract float getPivotValueX(float f, int i);

    protected abstract float getPivotValueY(float f, int i);

    protected abstract float getRotationValue(float f, float f2);

    protected abstract float getScrollDrawInward();

    protected abstract float getTranslationValueDeltaX(float f, float f2);

    protected abstract float getTranslationValueX(float f, float f2);

    protected abstract float getTranslationValueY(float f, float f2);

    protected abstract float getZoomValueX(float f, float f2);

    protected abstract float getZoomValueY(float f, float f2);

    public void applyTransform(View page, float scrollProgress, int index) {
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            float zoom = 1.0f * PageTransitionEffects.mix(1.0f, 0.7f, adjustScrollProgress(scrollProgress));
            int i = index;
            float scrollAlpha = backgroundAlphaThreshold(Math.abs(scrollProgress));
            CellLayout cl = (CellLayout) page;
            cl.invalidate();
            float rotation = 15.0f * scrollProgress;
            float translationX = (scrollProgress * getScrollDrawInward()) * ((float) cl.getWidth());
            int pageWidth = cl.getMeasuredWidth();
            int pageHeight = cl.getMeasuredHeight();
            this.mEndPage = false;
            if (isLoopingEnabled()) {
                cl.setPivotX(getPivotValueX(scrollProgress, pageWidth));
                cl.setPivotY(getPivotValueY(scrollProgress, pageHeight));
            } else {
                float vtransition_rotation_max = transition_rotation_max;
                if (i == 0 && scrollProgress < 0.0f) {
                    cl.setPivotX(TRANSITION_PIVOT * ((float) pageWidth));
                    rotation = ((-vtransition_rotation_max) * scrollProgress) / maxOverScroll();
                    translationX = PageTransitionManager.getScrollX();
                    zoom = 1.0f;
                } else if (i != PageTransitionManager.getChildCount() - 1 || scrollProgress <= 0.0f) {
                    cl.setPivotX(getPivotValueX(scrollProgress, pageWidth));
                    cl.setPivotY(getPivotValueY(scrollProgress, pageHeight));
                } else {
                    this.mEndPage = true;
                    cl.setPivotX((float) pageWidth);
                    cl.setPivotY(((float) pageHeight) / 2.0f);
                    rotation = ((-(vtransition_rotation_max / 2.0f)) * scrollProgress) / maxOverScroll();
                    translationX = PageTransitionManager.getScrollX() - PageTransitionManager.getMaxScrollX();
                    zoom = 1.0f;
                }
            }
            cl.setScaleY(getZoomValueY(scrollProgress, zoom));
            cl.setScaleX(getZoomValueX(scrollProgress, zoom));
            if (zoom < 1.0f) {
                if (this.mShrinkTranslateX != 0.0f) {
                    translationX += getTranslationValueDeltaX(scrollProgress, zoom);
                }
                if (this.mShrinkTranslateY != 0.0f) {
                    cl.setTranslationY(getTranslationValueY(scrollProgress, zoom));
                }
            }
            cl.setTranslationX(getTranslationValueX(scrollProgress, translationX));
            cl.setRotationY(getRotationValue(scrollProgress, rotation));
            cl.setBackgroundAlpha(PageTransitionEffects.mix(scrollAlpha, 1.0f, 0.0f));
        }
    }
}
