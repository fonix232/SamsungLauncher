package com.android.launcher3.home;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.event.ScreenDivision;
import com.sec.android.app.launcher.R;

public class HomeContainer extends FrameLayout {
    private static final int FACTOR_INTERVAL = 2;
    private static final int FACTOR_VALID_DECIMAL_PLACE = 2;
    private static final String SFINDER_CLS_NAME = "com.samsung.android.app.galaxyfinder.GalaxyFinderActivity";
    private static final String SFINDER_PKG_NAME = "com.samsung.android.app.galaxyfinder";
    private static final String TAG = "HomeContainer";
    private static final int TOUCH_STATE_CONSUME = 1;
    private static final int TOUCH_STATE_REST = 0;
    private boolean mActivateTouchSFinder;
    private HomeController mController;
    private int mDownwardFadeOutEnd;
    private int mDownwardFadeOutStart;
    private View mExternalPageIndicator;
    private float mFadeOutRange;
    private float mFirstDownX;
    private float mFirstDownY;
    private float mHomeAlphaRatio;
    private float mHomeShrinkFactor;
    private boolean mIsInstalledSFinder;
    private Launcher mLauncher;
    private boolean mMultiTouch;
    private int mOverlayEnd;
    private int mOverlayStart;
    private float mPageIndicatorScaleRatio;
    private float mPageIndicatorShrinkFactor;
    private int mPinchDelta;
    ScreenDivision mScreenDivision;
    private boolean mStartedSFinder;
    private int mTouchPointDistance;
    private int mTouchState;
    private TrayManager mTrayManager;
    private int mUpwardFadeOutEnd;
    private int mUpwardFadeOutStart;

    public HomeContainer(Context context) {
        this(context, null);
    }

    public HomeContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mMultiTouch = false;
        this.mTouchPointDistance = 0;
        this.mPinchDelta = 0;
        this.mTouchState = 0;
        this.mIsInstalledSFinder = false;
        this.mStartedSFinder = false;
        this.mActivateTouchSFinder = false;
        this.mFirstDownY = 0.0f;
        this.mFirstDownX = 0.0f;
        this.mLauncher = (Launcher) context;
        Resources res = getResources();
        this.mHomeShrinkFactor = res.getFraction(R.fraction.config_homeShrinkFactor, 1, 1);
        this.mHomeAlphaRatio = res.getFraction(R.fraction.config_homeAlphaRatio, 1, 1);
        this.mPageIndicatorShrinkFactor = res.getFraction(R.fraction.config_homePageIndicatorShrinkFactor, 1, 1);
        this.mPageIndicatorScaleRatio = res.getFraction(R.fraction.config_homePageIndicatorScaleRatio, 1, 1);
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            this.mIsInstalledSFinder = Utilities.isPackageExist(context, SFINDER_PKG_NAME);
        }
        this.mOverlayStart = res.getDimensionPixelSize(R.dimen.tray_overlay_start_on_transition_type_2);
        this.mOverlayEnd = 0;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mTrayManager != null) {
            this.mOverlayEnd = this.mTrayManager.getTrayMovingRange();
            this.mDownwardFadeOutStart = this.mOverlayStart;
            this.mDownwardFadeOutEnd = this.mOverlayEnd;
            this.mUpwardFadeOutStart = getHeight() - this.mOverlayStart;
            this.mUpwardFadeOutEnd = getHeight() - this.mOverlayEnd;
            this.mFadeOutRange = (float) (this.mUpwardFadeOutStart - this.mUpwardFadeOutEnd);
        }
        if (this.mExternalPageIndicator == null) {
            View pageIndicator = this.mController.getHomePageIndicatorView();
            if (pageIndicator != null && !equals(pageIndicator.getParent())) {
                this.mExternalPageIndicator = pageIndicator;
            }
        }
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        Log.d(TAG, "setVisibility : " + visibility);
        if (this.mExternalPageIndicator == null) {
            View pageIndicator = this.mController.getHomePageIndicatorView();
            if (pageIndicator != null && !equals(pageIndicator.getParent())) {
                this.mExternalPageIndicator = pageIndicator;
                this.mExternalPageIndicator.setVisibility(visibility);
                return;
            }
            return;
        }
        this.mExternalPageIndicator.setVisibility(visibility);
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        Log.d(TAG, "setAlpha : " + alpha);
        if (this.mExternalPageIndicator != null) {
            this.mExternalPageIndicator.setAlpha(Math.max(0.0f, 1.0f - ((1.0f - alpha) * this.mPageIndicatorScaleRatio)));
        }
    }

    void bindController(ControllerBase controller) {
        if (controller instanceof HomeController) {
            this.mController = (HomeController) controller;
        }
    }

    void setTrayManager(TrayManager trayManager) {
        this.mTrayManager = trayManager;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mTouchState == 0 && !this.mLauncher.isHomeStage() && this.mTrayManager != null && !this.mTrayManager.isMoving() && (getVisibility() != 0 || getAlpha() < 0.5f)) {
            return false;
        }
        switch (ev.getAction() & 255) {
            case 0:
                this.mTouchState = 1;
                break;
            case 1:
            case 3:
                this.mTouchState = 0;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int dy;
        int dx;
        switch (ev.getAction() & 255) {
            case 0:
                this.mMultiTouch = false;
                break;
            case 1:
            case 3:
            case 6:
                this.mActivateTouchSFinder = false;
                this.mMultiTouch = false;
                break;
            case 2:
                if (this.mMultiTouch && ev.getPointerCount() > 1) {
                    dy = (int) (ev.getY() - ev.getY(1));
                    dx = (int) (ev.getX() - ev.getX(1));
                    this.mPinchDelta = this.mTouchPointDistance - ((int) Math.sqrt((double) ((dy * dy) + (dx * dx))));
                    break;
                }
            case 5:
                if (ev.getPointerCount() == 2) {
                    dy = (int) (ev.getY() - ev.getY(1));
                    dx = (int) (ev.getX() - ev.getX(1));
                    this.mTouchPointDistance = (int) Math.sqrt((double) ((dy * dy) + (dx * dx)));
                    this.mMultiTouch = true;
                    break;
                }
                break;
        }
        if (!this.mMultiTouch && this.mController != null && this.mTouchState == 1 && this.mController.canMoveHometray()) {
            if (this.mTrayManager == null) {
                if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    float x = ev.getRawX();
                    float y = ev.getRawY();
                    switch (ev.getAction() & 255) {
                        case 0:
                            this.mActivateTouchSFinder = true;
                            this.mFirstDownX = x;
                            this.mFirstDownY = y;
                            this.mController.setScrollBlockArea(x, y);
                            this.mController.setScrollTalkBackEnabled();
                            break;
                        case 2:
                            if (!(this.mController == null || !this.mController.canMoveVertically() || this.mStartedSFinder || !this.mActivateTouchSFinder || this.mController.isMovingOnBlock())) {
                                new DvfsUtil(this.mLauncher).boostUpForSupportedModel();
                                int direction = (int) (y - this.mFirstDownY);
                                if (direction < 0 && this.mIsInstalledSFinder) {
                                    launchSfinder("swype_up", true);
                                    return true;
                                } else if (direction > 0) {
                                    if (LauncherAppState.getInstance().getNotificationPanelExpansionEnabled()) {
                                        Utilities.expandNotificationsPanel(this.mLauncher);
                                        return true;
                                    } else if (this.mIsInstalledSFinder) {
                                        launchSfinder("swype_down", false);
                                        return true;
                                    }
                                }
                            }
                            break;
                    }
                }
            }
            return this.mTrayManager.onInterceptTouchEvent(this.mController, ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mTrayManager == null || this.mController == null || !this.mController.canMoveHometray()) {
            return super.onTouchEvent(ev);
        }
        return this.mTrayManager.onTouchEvent(this.mController, ev);
    }

    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        if (this.mController == null || !(this.mController.isSelectState() || this.mController.isRunningStateChangeAnimation())) {
            int range;
            if (!(this.mTrayManager == null || this.mController == null)) {
                this.mTrayManager.onChangeTrayTranslationY(this.mController, translationY, getHeight());
            }
            boolean isMoveAndAnimated = false;
            if (this.mTrayManager != null) {
                range = this.mTrayManager.getTrayMovingRange();
                isMoveAndAnimated = this.mTrayManager.isMoveAndAnimated();
            } else {
                range = Utilities.getFullScreenHeight(this.mLauncher);
            }
            int visible = (translationY == 0.0f || (translationY > ((float) (-range)) && translationY < ((float) range))) ? 0 : 8;
            if (!(isMoveAndAnimated || getVisibility() == visible)) {
                setVisibility(visible);
            }
            updateScaleAndAlphaByTranslationY(translationY);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!(this.mTrayManager == null || this.mUpwardFadeOutStart == (bottom - top) - this.mOverlayStart)) {
            Log.d(TAG, "height is change !! mUpwardFadeOutStart : " + this.mUpwardFadeOutStart + " mOverlayStart : " + this.mOverlayStart + " top : " + top + " bottom : " + bottom);
            this.mUpwardFadeOutStart = (bottom - top) - this.mOverlayStart;
            this.mUpwardFadeOutEnd = (bottom - top) - this.mOverlayEnd;
            this.mFadeOutRange = (float) (this.mUpwardFadeOutStart - this.mUpwardFadeOutEnd);
        }
        setScreenDivision();
    }

    boolean isMultiTouch() {
        return this.mMultiTouch;
    }

    int getPinchDelta() {
        return this.mPinchDelta;
    }

    private void updateScaleAndAlphaByTranslationY(float translationY) {
        float factor;
        if (translationY <= 0.0f) {
            float bottomOfHome = translationY + ((float) getHeight());
            if (bottomOfHome > ((float) this.mUpwardFadeOutStart)) {
                factor = 1.0f;
            } else if (bottomOfHome > ((float) this.mUpwardFadeOutEnd)) {
                factor = Math.min(1.0f, (bottomOfHome - ((float) this.mUpwardFadeOutEnd)) / this.mFadeOutRange);
            } else {
                factor = (float) (translationY == 0.0f ? 1 : 0);
            }
        } else if (translationY < ((float) this.mDownwardFadeOutStart)) {
            factor = 1.0f;
        } else if (translationY < ((float) this.mDownwardFadeOutEnd)) {
            factor = Math.min(1.0f, (((float) this.mDownwardFadeOutEnd) - translationY) / this.mFadeOutRange);
        } else {
            factor = 0.0f;
        }
        Utilities.simplifyDecimalFraction(factor, 2, 2);
        float scale = this.mHomeShrinkFactor + ((1.0f - this.mHomeShrinkFactor) * factor);
        setAlpha(Math.max(0.0f, 1.0f - ((1.0f - factor) * this.mHomeAlphaRatio)));
        setScaleX(scale);
        setScaleY(scale);
        if (this.mExternalPageIndicator != null) {
            float indicatorScale = this.mPageIndicatorShrinkFactor + ((1.0f - this.mPageIndicatorShrinkFactor) * Math.max(0.0f, 1.0f - ((1.0f - factor) * this.mPageIndicatorScaleRatio)));
            this.mExternalPageIndicator.setScaleX(indicatorScale);
            this.mExternalPageIndicator.setScaleY(indicatorScale);
        }
    }

    void resetTouchState() {
        this.mTouchState = 0;
    }

    private void launchSfinder(String extra, boolean isSwipeUp) {
        int swipeEnterAnimResId;
        int swipeExitAnimResId;
        BlurUtils.blurByWindowManager(true, this.mLauncher.getWindow());
        this.mStartedSFinder = true;
        if (isSwipeUp) {
            swipeEnterAnimResId = R.anim.homeonly_sfinder_enter_swipe_up;
            swipeExitAnimResId = R.anim.homeonly_sfinder_exit_swipe_up;
        } else {
            swipeEnterAnimResId = R.anim.homeonly_sfinder_enter_swipe_down;
            swipeExitAnimResId = R.anim.homeonly_sfinder_exit_swipe_down;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SFINDER_PKG_NAME, SFINDER_CLS_NAME));
        intent.putExtra("launch_mode", extra);
        intent.addFlags(268468224);
        try {
            getContext().startActivity(intent, ActivityOptions.makeCustomAnimation(this.mLauncher, swipeEnterAnimResId, swipeExitAnimResId).toBundle());
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to launch. tag= intent=" + intent, e);
        } catch (SecurityException e2) {
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + " intent=" + intent, e2);
        }
    }

    boolean hasStartedSFinder() {
        return this.mStartedSFinder;
    }

    void resetStartedSFinder() {
        this.mStartedSFinder = false;
    }

    private void setScreenDivision() {
        if (this.mScreenDivision == null) {
            View hotseat = findViewById(R.id.hotseat);
            int[] location = new int[2];
            getLocationOnScreen(location);
            Rect homeContainer = new Rect(location[0], location[1], location[0] + getWidth(), location[1] + getHeight());
            hotseat.getLocationOnScreen(location);
            this.mScreenDivision = new ScreenDivision(10, 0, 1, homeContainer, true).customPatition(0, new Rect(location[0], location[1], location[0] + hotseat.getWidth(), location[1] + hotseat.getHeight())).builder();
        }
    }
}
