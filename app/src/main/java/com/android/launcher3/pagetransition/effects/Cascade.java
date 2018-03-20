package com.android.launcher3.pagetransition.effects;

import com.android.launcher3.pagetransition.PageTransitionManager;

public class Cascade extends AbstractTransitionEffects {
    protected float getZoomValueX(float scrollProgress, float zoom) {
        return zoom - (4.0f * (0.1f * Math.abs(scrollProgress)));
    }

    protected float getZoomValueY(float scrollProgress, float zoom) {
        return zoom - (0.5f * (0.1f * Math.abs(scrollProgress)));
    }

    protected float getRotationValue(float scrollProgress, float rotation) {
        if (PageTransitionManager.mLeftMove) {
            return Math.abs(rotation);
        }
        return -Math.abs(rotation);
    }

    protected float getTranslationValueX(float scrollProgress, float translationX) {
        if (!PageTransitionManager.mLeftMove || scrollProgress >= 0.0f) {
            return (PageTransitionManager.mLeftMove || scrollProgress < 0.0f) ? 0.0f : translationX;
        } else {
            return translationX;
        }
    }

    protected float getTranslationValueDeltaX(float scrollProgress, float zoom) {
        return ((1.0f - zoom) - (3.0f * (0.1f * Math.abs(scrollProgress)))) * this.mShrinkTranslateX;
    }

    protected float getTranslationValueY(float scrollProgress, float zoom) {
        return ((1.0f - zoom) - (3.0f * (0.1f * Math.abs(scrollProgress)))) * this.mShrinkTranslateY;
    }

    protected float getPivotValueX(float scrollProgress, int pageWidth) {
        if (PageTransitionManager.mLeftMove) {
            return (float) pageWidth;
        }
        return 0.0f;
    }

    protected float getPivotValueY(float scrollProgress, int pageHeight) {
        return ((float) pageHeight) / 2.0f;
    }

    protected float getScrollDrawInward() {
        return 0.7f;
    }

    protected float adjustScrollProgress(float scrollProgress) {
        return Math.abs(scrollProgress);
    }
}
