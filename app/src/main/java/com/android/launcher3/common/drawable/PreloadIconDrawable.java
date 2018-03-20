package com.android.launcher3.common.drawable;

import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class PreloadIconDrawable extends Drawable {
    private static final float ANIMATION_PROGRESS_COMPLETED = 1.0f;
    private static final float ANIMATION_PROGRESS_STARTED = 0.0f;
    private static final float ANIMATION_PROGRESS_STOPPED = -1.0f;
    private static final float ICON_SCALE_FACTOR = 0.5f;
    private float mAnimationProgress = ANIMATION_PROGRESS_STOPPED;
    private ObjectAnimator mAnimator;
    private final Drawable mIcon;

    public PreloadIconDrawable(Drawable icon) {
        this.mIcon = icon;
        setBounds(icon.getBounds());
        if (this.mIcon instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) this.mIcon).setGhostModeEnabled(true);
        }
    }

    protected void onBoundsChange(Rect bounds) {
        this.mIcon.setBounds(bounds);
    }

    public void draw(Canvas canvas) {
        float iconScale;
        Rect r = new Rect(getBounds());
        if (this.mAnimationProgress < 0.0f || this.mAnimationProgress >= 1.0f) {
            iconScale = 1.0f;
        } else {
            iconScale = ICON_SCALE_FACTOR + (this.mAnimationProgress * ICON_SCALE_FACTOR);
        }
        canvas.save();
        canvas.scale(iconScale, iconScale, r.exactCenterX(), r.exactCenterY());
        this.mIcon.draw(canvas);
        canvas.restore();
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
        this.mIcon.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter cf) {
        this.mIcon.setColorFilter(cf);
    }

    protected boolean onLevelChange(int level) {
        if ((this.mIcon instanceof FastBitmapDrawable) && level == 100) {
            ((FastBitmapDrawable) this.mIcon).setGhostModeEnabled(false);
            invalidateSelf();
        }
        return true;
    }

    public void maybePerformFinishedAnimation() {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        setAnimationProgress(0.0f);
        this.mAnimator = ObjectAnimator.ofFloat(this, "animationProgress", new float[]{0.0f, 1.0f});
        this.mAnimator.start();
    }

    public void setAnimationProgress(float progress) {
        if (progress != this.mAnimationProgress) {
            this.mAnimationProgress = progress;
            invalidateSelf();
        }
    }

    public boolean hasNotCompleted() {
        return this.mAnimationProgress < 1.0f;
    }

    public int getIntrinsicHeight() {
        return this.mIcon.getIntrinsicHeight();
    }

    public int getIntrinsicWidth() {
        return this.mIcon.getIntrinsicWidth();
    }

    public Drawable getIcon() {
        return this.mIcon;
    }
}
