package com.android.launcher3.util;

import android.content.res.Resources;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.launcher3.LauncherAppState;

public enum Talk {
    INSTANCE(LauncherAppState.getInstance());
    
    private final AccessibilityManager mAccessibilityMgr;
    private final Resources mResources;

    private Talk(LauncherAppState app) {
        this.mAccessibilityMgr = (AccessibilityManager) app.getContext().getSystemService("accessibility");
        this.mResources = app.getContext().getResources();
    }

    public void say(String s) {
        if (!isTouchExplorationEnabled()) {
            return;
        }
        if (s == null) {
            throw new IllegalArgumentException("Must provide a valid string to speak");
        } else if (isAccessibilityEnabled()) {
            AccessibilityEvent e = AccessibilityEvent.obtain(16384);
            if (e != null && e.getText() != null) {
                e.getText().clear();
                e.getText().add(s);
                this.mAccessibilityMgr.sendAccessibilityEvent(e);
            }
        }
    }

    public void say(int resourceId) {
        say(this.mResources.getString(resourceId));
    }

    public void postSay(final String s) {
        new Handler().post(new Runnable() {
            public void run() {
                Talk.this.say(s);
            }
        });
    }

    public boolean isAccessibilityEnabled() {
        return this.mAccessibilityMgr != null && this.mAccessibilityMgr.isEnabled();
    }

    public boolean isTouchExplorationEnabled() {
        return this.mAccessibilityMgr != null && this.mAccessibilityMgr.isTouchExplorationEnabled();
    }
}
