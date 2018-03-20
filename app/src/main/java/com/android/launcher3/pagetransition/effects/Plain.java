package com.android.launcher3.pagetransition.effects;

import android.view.View;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.pagetransition.PageTransitionManager;

public class Plain extends PageTransitionEffects {
    public void applyTransform(View page, float scrollProgress, int index) {
        CellLayout cl = (CellLayout) page;
        if (scrollProgress <= 1.0f && scrollProgress >= -1.0f) {
            float scrollAlpha = backgroundAlphaThreshold(Math.abs(scrollProgress));
            cl.invalidate();
            if (cl.getCellLayoutChildren().getChildCount() >= 0) {
                cl.setBackgroundAlpha(PageTransitionEffects.mix(scrollAlpha, 1.0f, PageTransitionManager.getPageBackgroundAlpha()));
            }
        }
    }
}
