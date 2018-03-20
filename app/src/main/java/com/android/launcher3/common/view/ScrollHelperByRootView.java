package com.android.launcher3.common.view;

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import com.android.launcher3.Utilities;

public class ScrollHelperByRootView {
    private static final int LIMIT_TRACE = 20;
    public static final int LISTENER_APPSCONTROLLER = 1;
    public static final int LISTENER_HOMECONTROLLER = 0;
    private static final int LISTENER_SIZE = 2;
    private static final String TAG = "LauncherScroll";
    private static final int X = 0;
    private static final int Y = 1;
    private float[] mDistanceFromPress = new float[2];
    private int mGradientCount = 0;
    private float mLastGradient = 0.0f;
    private boolean mPressed = false;
    private float[] mPressedXY = new float[2];
    private int mScrollId = 0;
    private ScrollTouchListener[] mScrollTouchListeners = new ScrollTouchListener[2];
    private float mSumOfAccelaration = 0.0f;
    private float[] mTouch = new float[2];
    private StringBuilder mTraceTouchEvent = null;
    private VelocityTracker mVelocityTracker = null;

    public interface ScrollTouchListener {
        int onTouchEvent(MotionEvent motionEvent);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    void requestPress() {
        releaseVelocityTracker();
        this.mPressed = false;
    }

    int setTouchEvent(MotionEvent event) {
        int action;
        acquireVelocityTrackerAndAddMovement(event);
        float x = event.getX();
        float y = event.getY();
        int pointerIndex = (event.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
        switch (event.getAction() & 255) {
            case 0:
                this.mPressed = true;
                this.mTraceTouchEvent = new StringBuilder();
                traceTouchEvent(this.mTraceTouchEvent, (int) x, (int) y, "P");
                reset();
                setTouch(this.mPressedXY, x, y);
                setTouch(this.mTouch, x, y);
                action = 0;
                break;
            case 2:
                if (this.mPressed) {
                    if (enableTrace()) {
                        traceTouchEvent(this.mTraceTouchEvent, (int) x, (int) y, "M");
                    }
                    setTouch(this.mTouch, x, y);
                    setMove();
                    this.mScrollId = event.getPointerId(pointerIndex);
                }
                action = 2;
                break;
            default:
                releaseVelocityTracker();
                this.mPressed = false;
                traceTouchEvent(this.mTraceTouchEvent, (int) x, (int) y, "R");
                displayTrace(this.mTraceTouchEvent);
                this.mTraceTouchEvent = null;
                action = event.getAction() & 255;
                break;
        }
        noticeOnTouchEvent(event);
        return action;
    }

    private void setMove() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.computeCurrentVelocity(16);
            setMove(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
        }
    }

    private void setMove(float velocityX, float velocityY) {
        float gradient = velocityX == 0.0f ? velocityY : velocityY / velocityX;
        this.mSumOfAccelaration += gradient - this.mLastGradient;
        this.mGradientCount++;
        this.mLastGradient = gradient;
    }

    private void reset() {
        this.mSumOfAccelaration = 0.0f;
        this.mGradientCount = 0;
        this.mLastGradient = 0.0f;
    }

    public float getAverageAccelaration() {
        float id = this.mGradientCount == 0 ? this.mSumOfAccelaration : this.mSumOfAccelaration / ((float) this.mGradientCount);
        float[] diff = getDistanceFromPress();
        return (0.8f * (diff[0] == 0.0f ? diff[1] : diff[1] / diff[0])) + (1.7f * id);
    }

    private float[] getDistanceFromPress() {
        this.mDistanceFromPress[0] = this.mTouch[0] - this.mPressedXY[0];
        this.mDistanceFromPress[1] = this.mTouch[1] - this.mPressedXY[1];
        return this.mDistanceFromPress;
    }

    public float getXDistanceFromPress() {
        return getDistanceFromPress()[0];
    }

    public float getYDistanceFromPress() {
        return getDistanceFromPress()[1];
    }

    public int getCount() {
        return this.mGradientCount;
    }

    private void setTouch(float[] touch, float x, float y) {
        if (touch != null) {
            touch[0] = x;
            touch[1] = y;
        }
    }

    private void noticeOnTouchEvent(MotionEvent event) {
        for (int i = 0; i < 2; i++) {
            if (this.mScrollTouchListeners[i] != null) {
                this.mScrollTouchListeners[i].onTouchEvent(event);
            }
        }
    }

    public void addListener(int controller, ScrollTouchListener scrollTouchListener) {
        if (scrollTouchListener != null && controller < 2) {
            this.mScrollTouchListeners[controller] = scrollTouchListener;
        }
    }

    public int getScrollId() {
        return this.mScrollId;
    }

    private void traceTouchEvent(StringBuilder stringBuilder, int x, int y, String action) {
        if (getCount() > 20) {
            stringBuilder = null;
        }
        if (stringBuilder != null) {
            String END = "|";
            stringBuilder.append(action).append(",").append(x).append(",").append(y).append("|");
        }
    }

    private void displayTrace(StringBuilder stringBuilder) {
        if (stringBuilder != null && Utilities.DEBUGGABLE()) {
            Log.i(TAG, stringBuilder.toString());
        }
    }

    private boolean enableTrace() {
        if (getCount() > 20) {
            this.mTraceTouchEvent = null;
        } else if (this.mTraceTouchEvent != null) {
            return true;
        }
        return false;
    }
}
