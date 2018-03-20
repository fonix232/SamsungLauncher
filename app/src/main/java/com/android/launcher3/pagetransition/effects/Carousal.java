package com.android.launcher3.pagetransition.effects;

import com.android.launcher3.pagetransition.PageTransitionManager;

public class Carousal extends AbstractTransitionEffects {
    protected float getZoomValueX(float scrollProgress, float zoom) {
        return zoom - (2.0f * (0.1f * Math.abs(scrollProgress)));
    }

    protected float getZoomValueY(float scrollProgress, float zoom) {
        return zoom - (2.0f * (0.1f * Math.abs(scrollProgress)));
    }

    protected float getRotationValue(float scrollProgress, float rotation) {
        return 0.0f;
    }

    protected float getTranslationValueX(float scrollProgress, float translationX) {
        if (this.mEndPage) {
            return PageTransitionManager.getScrollX() - PageTransitionManager.getMaxScrollX();
        }
        return translationX;
    }

    protected float getTranslationValueDeltaX(float scrollProgress, float zoom) {
        return ((1.0f - zoom) - (2.0f * (0.1f * Math.abs(scrollProgress)))) * this.mShrinkTranslateX;
    }

    protected float getTranslationValueY(float scrollProgress, float zoom) {
        return ((1.0f - zoom) - (2.0f * (0.1f * Math.abs(scrollProgress)))) * this.mShrinkTranslateY;
    }

    protected float getPivotValueX(float scrollProgress, int pageWidth) {
        return ((float) pageWidth) / 2.0f;
    }

    protected float getPivotValueY(float scrollProgress, int pageHeight) {
        return ((float) pageHeight) / 2.0f;
    }

    protected float getScrollDrawInward() {
        return this.mNormalScrollDrawInward;
    }

    protected float adjustScrollProgress(float scrollProgress) {
        return Math.abs(scrollProgress);
    }
}
