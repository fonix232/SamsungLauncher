package com.android.launcher3.common.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

public class LauncherActivityInfoCompatV16 extends LauncherActivityInfoCompat {
    private final ActivityInfo mActivityInfo;
    private final ComponentName mComponentName = new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
    private final PackageManager mPm;
    private final ResolveInfo mResolveInfo;

    LauncherActivityInfoCompatV16(Context context, ResolveInfo info) {
        this.mResolveInfo = info;
        this.mActivityInfo = info.activityInfo;
        this.mPm = context.getPackageManager();
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public UserHandleCompat getUser() {
        return UserHandleCompat.myUserHandle();
    }

    public CharSequence getLabel() {
        return this.mResolveInfo.loadLabel(this.mPm);
    }

    public Drawable getIcon(int density) {
        int iconRes = this.mResolveInfo.getIconResource();
        Drawable icon = null;
        if (!(density == 0 || iconRes == 0)) {
            try {
                icon = this.mPm.getResourcesForApplication(this.mActivityInfo.applicationInfo).getDrawableForDensity(iconRes, density);
            } catch (NameNotFoundException e) {
            } catch (NotFoundException e2) {
            }
        }
        if (icon == null) {
            icon = this.mResolveInfo.loadIcon(this.mPm);
        }
        if (icon == null) {
            return Resources.getSystem().getDrawableForDensity(17629184, density);
        }
        return icon;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mActivityInfo.applicationInfo;
    }

    public long getFirstInstallTime() {
        try {
            PackageInfo info = this.mPm.getPackageInfo(this.mActivityInfo.packageName, 0);
            if (info != null) {
                return info.firstInstallTime;
            }
            return 0;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    public String getName() {
        return this.mActivityInfo.name;
    }

    public Drawable getBadgedIcon(int density) {
        return getIcon(density);
    }

    public Drawable getBadgedIconForIconTray(int density) {
        return getIcon(density);
    }
}
