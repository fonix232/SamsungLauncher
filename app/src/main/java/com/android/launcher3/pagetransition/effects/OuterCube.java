package com.android.launcher3.pagetransition.effects;

import com.android.launcher3.pagetransition.PageTransitionManager;

public class OuterCube extends AbstractTransitionEffects {
    protected float getZoomValueX(float scrollProgress, float zoom) {
        return 1.0f;
    }

    protected float getZoomValueY(float scrollProgress, float zoom) {
        return 1.0f;
    }

    protected float getRotationValue(float scrollProgress, float rotation) {
        float value = -rotation;
        if (this.mEndPage) {
            return ((-(transition_rotation_max / 2.0f)) * scrollProgress) / maxOverScroll();
        }
        return value;
    }

    protected float getTranslationValueX(float scrollProgress, float translationX) {
        if (this.mEndPage) {
            return PageTransitionManager.getScrollX() - PageTransitionManager.getMaxScrollX();
        }
        return translationX;
    }

    protected float getTranslationValueDeltaX(float scrollProgress, float zoom) {
        return 0.0f;
    }

    protected float getTranslationValueY(float scrollProgress, float zoom) {
        return 0.0f;
    }

    protected float getPivotValueX(float scrollProgress, int pageWidth) {
        if (scrollProgress <= 0.0f) {
            return 0.0f;
        }
        return (float) pageWidth;
    }

    protected float getPivotValueY(float scrollProgress, int pageHeight) {
        return ((float) pageHeight) / 2.0f;
    }

    protected float getScrollDrawInward() {
        return 0.025f;
    }

    protected float adjustScrollProgress(float scrollProgress) {
        return Math.abs(scrollProgress);
    }
}
