package com.android.launcher3.util.animation;

import android.graphics.Rect;

public class PillWidthRevealOutlineProvider extends PillRevealOutlineProvider {
    private final int mStartLeft;
    private final int mStartRight;

    public PillWidthRevealOutlineProvider(Rect pillRect, int left, int right) {
        super(0, 0, pillRect);
        this.mOutline.set(pillRect);
        this.mStartLeft = left;
        this.mStartRight = right;
    }

    public void setProgress(float progress) {
        this.mOutline.left = (int) ((((float) this.mPillRect.left) * progress) + ((1.0f - progress) * ((float) this.mStartLeft)));
        this.mOutline.right = (int) ((((float) this.mPillRect.right) * progress) + ((1.0f - progress) * ((float) this.mStartRight)));
    }
}
