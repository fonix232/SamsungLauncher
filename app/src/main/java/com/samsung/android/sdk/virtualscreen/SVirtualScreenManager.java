package com.samsung.android.sdk.virtualscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;

public class SVirtualScreenManager {
    private static boolean mVirtualScreenAvailable = false;
    private static boolean mVirtualScreenAvailableChecked = false;
    private SVirtualScreenManagerReflector mSVirtualScreenManagerReflector;

    public static class LaunchParams {
        public static int FLAG_BASE_ACTIVITY = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_BASE_ACTIVITY;
        public static int FLAG_CLEAR_TASKS = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_CLEAR_TASKS;
        public static int FLAG_FOCUS_POLICY = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_FOCUS_POLICY;
        public static int FLAG_LAYOUT_POLICY = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_LAYOUT_POLICY;
        public static int FLAG_NO_ANIMATION = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_NO_ANIMATION;
        public static int FLAG_RECREATE_VIRTUALSCREEN = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_RECREATE_VIRTUALSCREEN;
        public static int FLAG_REUSE_TASK_POLICY = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_REUSE_TASK_POLICY;
        public static int FLAG_ZEROPAGE_POLICY = com.samsung.android.sdk.virtualscreen.SVirtualScreenManagerReflector.LaunchParams.FLAG_ZEROPAGE_POLICY;
        public int baseDisplayId = -1;
        public Rect bounds = null;
        public int displayId = -1;
        public int flags = 0;
    }

    private SVirtualScreenManager() {
    }

    public SVirtualScreenManager(Context context) {
        this.mSVirtualScreenManagerReflector = new SVirtualScreenManagerReflector(context);
    }

    public boolean setOffset(int offsetX, int offsetY) {
        return setOffset(offsetX, offsetY, false);
    }

    public boolean setOffset(int offsetX, int offsetY, boolean force) {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.setOffset(offsetX, offsetY, force);
        }
        return false;
    }

    public Point getOffset() {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.getOffset();
        }
        return new Point();
    }

    public int startActivity(Intent intent, Bundle options, LaunchParams params) {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.startActivity(intent, options, params);
        }
        return -1;
    }

    public boolean bindVirtualScreen() {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.bindVirtualScreen();
        }
        return false;
    }

    public boolean unBindVirtualScreen() {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.unBindVirtualScreen();
        }
        return false;
    }

    public boolean isMoving() {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.isMoving();
        }
        return false;
    }

    public boolean updateVirtualScreen(Rect bound, int flags) {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.updateVirtualScreen(bound, flags);
        }
        return false;
    }

    public int getDisplayIdByPackage(String packageName) {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.getDisplayIdByPackage(packageName);
        }
        return -1;
    }

    public Intent updateMultiScreenLaunchParams(Intent intent, LaunchParams params) {
        if (isVirtualScreenAvailable()) {
            return this.mSVirtualScreenManagerReflector.updateMultiScreenLaunchParams(intent, params);
        }
        return null;
    }

    private boolean isVirtualScreenAvailable() {
        if (mVirtualScreenAvailableChecked) {
            return mVirtualScreenAvailable;
        }
        if (new SVirtualScreen().isFeatureEnabled(1)) {
            boolean z = this.mSVirtualScreenManagerReflector != null && this.mSVirtualScreenManagerReflector.initialized();
            mVirtualScreenAvailable = z;
        }
        mVirtualScreenAvailableChecked = true;
        return mVirtualScreenAvailable;
    }
}
