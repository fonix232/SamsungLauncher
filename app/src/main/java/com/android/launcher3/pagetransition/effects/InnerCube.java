package com.android.launcher3.pagetransition.effects;

import com.android.launcher3.pagetransition.PageTransitionManager;

public class InnerCube extends AbstractTransitionEffects {
    protected float getZoomValueX(float scrollProgress, float zoom) {
        if (Math.abs(scrollProgress) > 0.5f) {
            return zoom - (1.0f * (0.01f * Math.abs(scrollProgress)));
        }
        return zoom;
    }

    protected float getZoomValueY(float scrollProgress, float zoom) {
        if (Math.abs(scrollProgress) > 0.5f) {
            return zoom - (1.0f * (0.01f * Math.abs(scrollProgress)));
        }
        return zoom;
    }

    protected float getRotationValue(float scrollProgress, float rotation) {
        return rotation;
    }

    protected float getTranslationValueX(float scrollProgress, float translationX) {
        if (this.mEndPage) {
            return PageTransitionManager.getScrollX() - PageTransitionManager.getMaxScrollX();
        }
        return translationX;
    }

    protected float getTranslationValueDeltaX(float scrollProgress, float zoom) {
        if (Math.abs(scrollProgress) > 0.5f) {
            zoom -= 0.01f * Math.abs(scrollProgress);
        }
        return (1.0f - zoom) * this.mShrinkTranslateX;
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
        return 0.01f;
    }

    protected float adjustScrollProgress(float scrollProgress) {
        if (Math.abs(scrollProgress) <= 0.5f) {
            return Math.abs(scrollProgress);
        }
        return 1.0f - Math.abs(scrollProgress);
    }
}
