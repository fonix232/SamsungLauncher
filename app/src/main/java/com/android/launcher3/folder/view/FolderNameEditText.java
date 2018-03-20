package com.android.launcher3.folder.view;

import android.content.Context;
import android.graphics.Rect;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class FolderNameEditText extends EditText {
    private KeyListener mBackupKeyListener;
    private OnEventListener mOnEventListener;

    interface OnEventListener {
        void onKeyDown(int i, KeyEvent keyEvent);

        void onLayoutUpdated();

        boolean onPreImeBackKey();
    }

    public FolderNameEditText(Context context) {
        super(context);
    }

    public FolderNameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderNameEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    void setOnEventListener(OnEventListener listener) {
        this.mOnEventListener = listener;
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode != 4 || event.getAction() != 1) {
            return super.onKeyPreIme(keyCode, event);
        }
        if (this.mOnEventListener != null) {
            return this.mOnEventListener.onPreImeBackKey();
        }
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mOnEventListener != null) {
            if (keyCode == 20) {
                this.mOnEventListener.onKeyDown(keyCode, event);
                return true;
            } else if (keyCode == 22 && getSelectionStart() >= length()) {
                this.mOnEventListener.onKeyDown(keyCode, event);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mOnEventListener != null) {
            this.mOnEventListener.onLayoutUpdated();
        }
    }

    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused) {
            KeyListener kl = getKeyListener();
            if (kl != null) {
                this.mBackupKeyListener = kl;
            }
            setKeyListener(null);
        } else if (this.mBackupKeyListener != null) {
            setKeyListener(this.mBackupKeyListener);
        }
    }
}
