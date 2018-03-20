package com.android.launcher3.common.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources.IndicatorType;
import com.android.launcher3.util.WhiteBgManager;
import com.sec.android.app.launcher.R;

public class PageIndicatorMarker extends FrameLayout {
    private static final int MARKER_FADE_DURATION = 175;
    private static final String TAG = "PageIndicator";
    private ImageView mActiveMarker;
    private boolean mEnableMarkerAnim;
    private ImageView mInactiveMarker;
    private boolean mIsActive;
    private IndicatorType type;

    public PageIndicatorMarker(Context context) {
        this(context, null);
    }

    public PageIndicatorMarker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorMarker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIsActive = false;
        this.type = IndicatorType.DEFAULT;
        this.mEnableMarkerAnim = true;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mActiveMarker = (ImageView) findViewById(R.id.active);
        this.mInactiveMarker = (ImageView) findViewById(R.id.inactive);
    }

    void setMarkerDrawables(Drawable activeResId, Drawable inactiveResId, IndicatorType type) {
        this.mActiveMarker.setImageDrawable(activeResId);
        this.mInactiveMarker.setImageDrawable(inactiveResId);
        this.type = type;
    }

    public IndicatorType getMarkerType() {
        return this.type;
    }

    void activate(boolean immediate) {
        if (immediate || !this.mEnableMarkerAnim) {
            this.mActiveMarker.animate().cancel();
            this.mActiveMarker.setAlpha(1.0f);
            this.mActiveMarker.setScaleX(1.0f);
            this.mActiveMarker.setScaleY(1.0f);
            this.mInactiveMarker.animate().cancel();
            this.mInactiveMarker.setAlpha(0.0f);
        } else {
            this.mActiveMarker.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(175).start();
            this.mInactiveMarker.animate().alpha(0.0f).setDuration(175).start();
        }
        this.mIsActive = true;
    }

    void inactivate(boolean immediate) {
        if (immediate || !this.mEnableMarkerAnim) {
            this.mInactiveMarker.animate().cancel();
            this.mInactiveMarker.setAlpha(0.4f);
            this.mActiveMarker.animate().cancel();
            this.mActiveMarker.setAlpha(0.0f);
            this.mActiveMarker.setScaleX(0.5f);
            this.mActiveMarker.setScaleY(0.5f);
        } else {
            this.mInactiveMarker.animate().alpha(0.4f).setDuration(175).start();
            this.mActiveMarker.animate().alpha(0.0f).scaleX(0.5f).scaleY(0.5f).setDuration(175).start();
        }
        this.mIsActive = false;
    }

    boolean isActive() {
        return this.mIsActive;
    }

    public void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeColorFilterForBg(getContext(), this.mActiveMarker, whiteBg);
        WhiteBgManager.changeColorFilterForBg(getContext(), this.mInactiveMarker, whiteBg);
    }

    public void disableMarkerAnimation() {
        this.mEnableMarkerAnim = false;
    }

    public void enableMarkerAnimation() {
        this.mEnableMarkerAnim = true;
    }
}
