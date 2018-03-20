package com.android.launcher3.util.focus;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import com.android.launcher3.Utilities;

public abstract class FocusListener implements OnKeyListener, OnFocusChangeListener {
    public abstract void onFocusIn(View view);

    public abstract void onFocusOut(View view);

    public abstract boolean onKeyPressDown(View view, int i, KeyEvent keyEvent);

    public abstract boolean onKeyPressLeft(View view, int i, KeyEvent keyEvent);

    public abstract boolean onKeyPressRight(View view, int i, KeyEvent keyEvent);

    public abstract boolean onKeyPressUp(View view, int i, KeyEvent keyEvent);

    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            onFocusIn(v);
        } else {
            onFocusOut(v);
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (isRtl()) {
            if (keyCode == 21) {
                keyCode = 22;
            } else if (keyCode == 22) {
                keyCode = 21;
            }
        }
        switch (event.getAction()) {
            case 0:
                return onKeyDown(v, keyCode, event);
            case 1:
                return onKeyUp(v, keyCode, event);
            default:
                return false;
        }
    }

    public boolean onKeyUp(View v, int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyDown(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 19:
                if (onKeyPressUp(v, keyCode, event)) {
                    v.playSoundEffect(2);
                    return true;
                }
                break;
            case 20:
                if (onKeyPressDown(v, keyCode, event)) {
                    v.playSoundEffect(4);
                    return true;
                }
                break;
            case 21:
                if (onKeyPressLeft(v, keyCode, event)) {
                    v.playSoundEffect(1);
                    return true;
                }
                break;
            case 22:
                if (onKeyPressRight(v, keyCode, event)) {
                    v.playSoundEffect(3);
                    return true;
                }
                break;
        }
        return false;
    }

    protected View searchPredefinedFocus(View v, int direction) {
        View nextView = v.focusSearch(direction);
        if (nextView == null) {
            return null;
        }
        switch (direction) {
            case 17:
                if (v.getId() != nextView.getNextFocusLeftId()) {
                    return null;
                }
                return nextView;
            case 33:
                if (v.getId() != nextView.getNextFocusUpId()) {
                    return null;
                }
                return nextView;
            case 66:
                if (v.getId() != nextView.getNextFocusRightId()) {
                    return null;
                }
                return nextView;
            case 130:
                if (v.getId() != nextView.getNextFocusDownId()) {
                    return null;
                }
                return nextView;
            default:
                return null;
        }
    }

    private boolean isRtl() {
        return Utilities.sIsRtl;
    }
}
