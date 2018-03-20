package com.android.launcher3.util.event;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.launcher3.Utilities;

public class StylusEventHelper {
    private boolean mIsButtonPressed;
    private final View mView;

    public StylusEventHelper(View view) {
        this.mView = view;
    }

    public boolean checkAndPerformStylusEvent(MotionEvent event) {
        float slop = (float) ViewConfiguration.get(this.mView.getContext()).getScaledTouchSlop();
        if (!this.mView.isLongClickable()) {
            return false;
        }
        boolean stylusButtonPressed = isStylusButtonPressed(event);
        switch (event.getAction()) {
            case 0:
                this.mIsButtonPressed = false;
                if (!stylusButtonPressed || !this.mView.performLongClick()) {
                    return false;
                }
                this.mIsButtonPressed = true;
                return true;
            case 1:
            case 3:
                this.mIsButtonPressed = false;
                return false;
            case 2:
                if (!Utilities.pointInView(this.mView, event.getX(), event.getY(), slop)) {
                    return false;
                }
                if (!this.mIsButtonPressed && stylusButtonPressed && this.mView.performLongClick()) {
                    this.mIsButtonPressed = true;
                    return true;
                } else if (!this.mIsButtonPressed || stylusButtonPressed) {
                    return false;
                } else {
                    this.mIsButtonPressed = false;
                    return false;
                }
            default:
                return false;
        }
    }

    public boolean inStylusButtonPressed() {
        return this.mIsButtonPressed;
    }

    private static boolean isStylusButtonPressed(MotionEvent event) {
        if (event.getToolType(0) == 2 && (event.getButtonState() & 2) == 2) {
            return true;
        }
        return false;
    }
}
