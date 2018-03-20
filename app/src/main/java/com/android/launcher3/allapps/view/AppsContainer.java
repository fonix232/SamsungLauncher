package com.android.launcher3.allapps.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.util.event.ScreenDivision;
import com.sec.android.app.launcher.R;

public class AppsContainer extends FrameLayout {
    private final Rect mClearRect;
    private AppsController mController;
    private View mExternalPageIndicator;
    private final Launcher mLauncher;
    private final float mPageIndicatorScaleRatio;
    private ScreenDivision mScreenDivision;
    private TrayManager mTrayManager;

    public AppsContainer(Context context) {
        this(context, null);
    }

    public AppsContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mClearRect = new Rect();
        this.mLauncher = (Launcher) context;
        this.mPageIndicatorScaleRatio = getResources().getFraction(R.fraction.config_appsPageIndicatorScaleRatio, 1, 1);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mTrayManager != null && this.mClearRect.right == 0) {
            this.mClearRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setScreenDivision();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mExternalPageIndicator != null) {
            this.mExternalPageIndicator.setVisibility(visibility);
        }
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (this.mExternalPageIndicator != null) {
            this.mExternalPageIndicator.setAlpha(Math.max(0.0f, 1.0f - ((1.0f - alpha) * this.mPageIndicatorScaleRatio)));
        }
    }

    public void bindController(ControllerBase controller) {
        if (controller instanceof AppsController) {
            this.mController = (AppsController) controller;
        }
    }

    public void setTrayManager(TrayManager trayManager) {
        this.mTrayManager = trayManager;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mTrayManager == null || this.mController == null || !this.mController.canMoveTray()) {
            return super.onInterceptTouchEvent(ev);
        }
        return this.mTrayManager.onInterceptTouchEvent(this.mController, ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mTrayManager == null || this.mController == null || !this.mController.canMoveTray()) {
            return super.onTouchEvent(ev);
        }
        return this.mTrayManager.onTouchEvent(this.mController, ev);
    }

    public void setDrawBoundaryY(float offsetY, boolean updateImmediately, boolean disallowVisible) {
        int visible = 8;
        LayoutParams lp = (LayoutParams) getLayoutParams();
        int boundaryY;
        if (offsetY > 0.0f) {
            boundaryY = (int) (((float) lp.topMargin) + offsetY);
            this.mClearRect.top = boundaryY;
            this.mClearRect.bottom = getHeight();
            if (boundaryY > 0) {
                visible = 0;
            }
        } else {
            boundaryY = (int) (((double) ((((float) getHeight()) + offsetY) - ((float) lp.topMargin))) + 0.9d);
            this.mClearRect.top = 0;
            this.mClearRect.bottom = boundaryY;
            if (boundaryY < getHeight()) {
                visible = 0;
            }
        }
        if ((!(this.mLauncher.isAppsStage() || this.mLauncher.isHomeStage()) || disallowVisible) && visible == 0) {
            visible = 8;
        }
        if ((this.mTrayManager == null || !this.mTrayManager.isMoveAndAnimated()) && getVisibility() != visible) {
            new VisibilityChange(visible, this).run();
        }
        if (updateImmediately) {
            invalidate();
        }
    }

    private void setScreenDivision() {
        if (this.mScreenDivision == null) {
            this.mScreenDivision = new ScreenDivision(10, 0, 1, new Rect(0, 0, getWidth(), getHeight()), true).builder();
        }
    }

    public ScreenDivision getScreenDivision() {
        return this.mScreenDivision;
    }

    public void initExternalPageIndicator(View pageIndicator) {
        if (this.mExternalPageIndicator == null && pageIndicator != null && !equals(pageIndicator.getParent())) {
            this.mExternalPageIndicator = pageIndicator;
        }
    }
}
