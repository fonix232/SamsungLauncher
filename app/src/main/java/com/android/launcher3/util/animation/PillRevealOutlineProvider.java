package com.android.launcher3.util.animation;

import android.annotation.TargetApi;
import android.graphics.Rect;

@TargetApi(21)
public class PillRevealOutlineProvider extends RevealOutlineAnimation {
    private int mCenterX;
    private int mCenterY;
    protected Rect mPillRect;

    public PillRevealOutlineProvider(int x, int y, Rect pillRect) {
        this.mCenterX = x;
        this.mCenterY = y;
        this.mPillRect = pillRect;
        this.mOutlineRadius = ((float) pillRect.height()) / 2.0f;
    }

    public boolean shouldRemoveElevationDuringAnimation() {
        return false;
    }

    public void setProgress(float progress) {
        int currentSize = (int) (((float) Math.max(this.mCenterX, this.mPillRect.width() - this.mCenterX)) * progress);
        this.mOutline.left = Math.max(this.mPillRect.left, this.mCenterX - currentSize);
        this.mOutline.top = Math.max(this.mPillRect.top, this.mCenterY - currentSize);
        this.mOutline.right = Math.min(this.mPillRect.right, this.mCenterX + currentSize);
        this.mOutline.bottom = Math.min(this.mPillRect.bottom, this.mCenterY + currentSize);
        this.mOutlineRadius = ((float) this.mOutline.height()) / 2.0f;
    }
}
