package com.android.launcher3.util.animation;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

public class RevealOutlineProvider extends ViewOutlineProvider {
    private int mCenterX;
    private int mCenterY;
    private int mCurrentRadius;
    private final Rect mOval = new Rect();
    private float mRadius0;
    private float mRadius1;

    public RevealOutlineProvider(int x, int y, float r0, float r1) {
        this.mCenterX = x;
        this.mCenterY = y;
        this.mRadius0 = r0;
        this.mRadius1 = r1;
    }

    public void setProgress(float progress) {
        this.mCurrentRadius = (int) (((1.0f - progress) * this.mRadius0) + (this.mRadius1 * progress));
        this.mOval.left = this.mCenterX - this.mCurrentRadius;
        this.mOval.top = this.mCenterY - this.mCurrentRadius;
        this.mOval.right = this.mCenterX + this.mCurrentRadius;
        this.mOval.bottom = this.mCenterY + this.mCurrentRadius;
    }

    public void getOutline(View v, Outline outline) {
        outline.setRoundRect(this.mOval, (float) this.mCurrentRadius);
    }
}
