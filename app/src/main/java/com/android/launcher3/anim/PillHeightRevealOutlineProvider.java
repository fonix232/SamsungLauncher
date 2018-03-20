package com.android.launcher3.anim;

import android.graphics.Rect;
import com.android.launcher3.util.PillRevealOutlineProvider;

public class PillHeightRevealOutlineProvider extends PillRevealOutlineProvider {
    private final int mNewHeight;

    public PillHeightRevealOutlineProvider(Rect pillRect, float radius, int newHeight) {
        super(0, 0, pillRect, radius);
        this.mOutline.set(pillRect);
        this.mNewHeight = newHeight;
    }

    public void setProgress(float progress) {
        this.mOutline.top = 0;
        int heightDifference = this.mPillRect.height() - this.mNewHeight;
        this.mOutline.bottom = (int) (((float) this.mPillRect.bottom) - (((float) heightDifference) * (1.0f - progress)));
    }
}
