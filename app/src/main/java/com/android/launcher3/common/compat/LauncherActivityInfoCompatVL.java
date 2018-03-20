package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;

@TargetApi(21)
public class LauncherActivityInfoCompatVL extends LauncherActivityInfoCompat {
    private LauncherActivityInfo mLauncherActivityInfo;

    LauncherActivityInfoCompatVL(LauncherActivityInfo launcherActivityInfo) {
        this.mLauncherActivityInfo = launcherActivityInfo;
    }

    public ComponentName getComponentName() {
        return this.mLauncherActivityInfo.getComponentName();
    }

    public UserHandleCompat getUser() {
        return UserHandleCompat.fromUser(this.mLauncherActivityInfo.getUser());
    }

    public CharSequence getLabel() {
        return this.mLauncherActivityInfo.getLabel();
    }

    public Drawable getIcon(int density) {
        return this.mLauncherActivityInfo.getIcon(density);
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mLauncherActivityInfo.getApplicationInfo();
    }

    public long getFirstInstallTime() {
        return this.mLauncherActivityInfo.getFirstInstallTime();
    }

    public Drawable getBadgedIcon(int density) {
        return this.mLauncherActivityInfo.getBadgedIcon(density);
    }

    public Drawable getBadgedIconForIconTray(int density) {
        Drawable icon = null;
        try {
            icon = this.mLauncherActivityInfo.semGetBadgedIconForIconTray(density);
        } catch (NoSuchMethodError ne) {
            ne.printStackTrace();
        }
        return icon;
    }
}
