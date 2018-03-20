package com.android.launcher3.common.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class DeferredLauncherActivityInfo extends LauncherActivityInfoCompat {
    private LauncherActivityInfoCompat mActualInfo;
    private final ComponentName mComponent;
    private final Context mContext;
    private final UserHandleCompat mUser;

    public DeferredLauncherActivityInfo(ComponentName component, UserHandleCompat user, Context context) {
        this.mComponent = component;
        this.mUser = user;
        this.mContext = context;
    }

    public ComponentName getComponentName() {
        return this.mComponent;
    }

    public UserHandleCompat getUser() {
        return this.mUser;
    }

    private synchronized LauncherActivityInfoCompat getActualInfo() {
        if (this.mActualInfo == null) {
            this.mActualInfo = LauncherAppsCompat.getInstance(this.mContext).resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(this.mComponent), this.mUser);
        }
        return this.mActualInfo;
    }

    public CharSequence getLabel() {
        return getActualInfo().getLabel();
    }

    public Drawable getIcon(int density) {
        return getActualInfo().getIcon(density);
    }

    public ApplicationInfo getApplicationInfo() {
        return getActualInfo().getApplicationInfo();
    }

    public long getFirstInstallTime() {
        return getActualInfo().getFirstInstallTime();
    }

    public Drawable getBadgedIcon(int density) {
        return null;
    }

    public Drawable getBadgedIconForIconTray(int density) {
        return null;
    }
}
