package com.android.launcher3.util.event;

import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;

public class CheckLongPressHelper {
    public static final int LONG_PRESS_TIME_OUT_DEFAULT = 300;
    public static final int LONG_PRESS_TIME_OUT_SHORT = 200;
    private static final int LONG_PRESS_TIME_OUT_SYSTEM_DEFAULT = 500;
    private boolean mHasPerformedLongPress;
    private OnLongClickListener mListener;
    private int mLongPressTimeout = LONG_PRESS_TIME_OUT_DEFAULT;
    private CheckForLongPress mPendingCheckForLongPress;
    private final View mView;

    class CheckForLongPress implements Runnable {
        CheckForLongPress() {
        }

        public void run() {
            if (CheckLongPressHelper.this.mView.getParent() != null && CheckLongPressHelper.this.mView.hasWindowFocus() && !CheckLongPressHelper.this.mHasPerformedLongPress) {
                boolean handled;
                if (CheckLongPressHelper.this.mListener != null) {
                    handled = CheckLongPressHelper.this.mListener.onLongClick(CheckLongPressHelper.this.mView);
                } else {
                    handled = CheckLongPressHelper.this.mView.performLongClick();
                }
                if (handled) {
                    CheckLongPressHelper.this.mView.setPressed(false);
                    CheckLongPressHelper.this.mHasPerformedLongPress = true;
                }
            }
        }
    }

    public CheckLongPressHelper(View v) {
        this.mView = v;
    }

    public CheckLongPressHelper(View v, OnLongClickListener listener) {
        this.mView = v;
        this.mListener = listener;
    }

    public void setLongPressTimeout(int longPressTimeout) {
        this.mLongPressTimeout = longPressTimeout;
    }

    public void postCheckForLongPress() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress == null) {
            this.mPendingCheckForLongPress = new CheckForLongPress();
        }
        int timeout = ViewConfiguration.getLongPressTimeout();
        View view = this.mView;
        Runnable runnable = this.mPendingCheckForLongPress;
        if (timeout == 500) {
            timeout = this.mLongPressTimeout;
        }
        view.postDelayed(runnable, (long) timeout);
    }

    public void cancelLongPress() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress != null) {
            this.mView.removeCallbacks(this.mPendingCheckForLongPress);
            this.mPendingCheckForLongPress = null;
        }
    }

    public boolean hasPerformedLongPress() {
        return this.mHasPerformedLongPress;
    }

    public void setHasPerformedLongPress(boolean value) {
        this.mHasPerformedLongPress = value;
    }
}
