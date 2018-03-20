package com.android.launcher3.common.quickoption.shortcuts;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.DeferredLauncherActivityInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;

@TargetApi(25)
public class ShortcutInfoCompat {
    public static final String EXTRA_SHORTCUT_ID = "shortcut_id";
    public static final String INTENT_CATEGORY = "com.android.launcher3.DEEP_SHORTCUT";
    private final ShortcutInfo mShortcutInfo;

    public ShortcutInfoCompat(ShortcutInfo shortcutInfo) {
        this.mShortcutInfo = shortcutInfo;
    }

    @TargetApi(25)
    public Intent makeIntent(Context context) {
        return new Intent("android.intent.action.MAIN").addCategory(INTENT_CATEGORY).setComponent(getActivity()).setPackage(getPackage()).setFlags(270532608).putExtra(ItemInfo.EXTRA_PROFILE, UserManagerCompat.getInstance(context).getSerialNumberForUser(getUserHandle())).putExtra(EXTRA_SHORTCUT_ID, getId());
    }

    @TargetApi(25)
    public Intent makeIntent(Context context, String packageName) {
        return new Intent("android.intent.action.MAIN").addCategory(INTENT_CATEGORY).setComponent(getActivity()).setPackage(packageName).setFlags(270532608).putExtra(ItemInfo.EXTRA_PROFILE, UserManagerCompat.getInstance(context).getSerialNumberForUser(getUserHandle())).putExtra(EXTRA_SHORTCUT_ID, getId());
    }

    public ShortcutInfo getShortcutInfo() {
        return this.mShortcutInfo;
    }

    public String getPackage() {
        return this.mShortcutInfo.getPackage();
    }

    public String getId() {
        return this.mShortcutInfo.getId();
    }

    public CharSequence getShortLabel() {
        return this.mShortcutInfo.getShortLabel();
    }

    public CharSequence getLongLabel() {
        return this.mShortcutInfo.getLongLabel();
    }

    public ComponentName getActivity() {
        return this.mShortcutInfo.getActivity();
    }

    public UserHandleCompat getUserHandle() {
        return UserHandleCompat.fromUser(this.mShortcutInfo.getUserHandle());
    }

    public boolean isPinned() {
        return this.mShortcutInfo.isPinned();
    }

    public boolean isDeclaredInManifest() {
        return this.mShortcutInfo.isDeclaredInManifest();
    }

    public boolean isEnabled() {
        return this.mShortcutInfo.isEnabled();
    }

    public boolean isDynamic() {
        return this.mShortcutInfo.isDynamic();
    }

    public int getRank() {
        return this.mShortcutInfo.getRank();
    }

    public CharSequence getDisabledMessage() {
        return this.mShortcutInfo.getDisabledMessage();
    }

    public String toString() {
        return this.mShortcutInfo.toString();
    }

    public LauncherActivityInfoCompat getActivityInfo(Context context) {
        return new DeferredLauncherActivityInfo(getActivity(), getUserHandle(), context);
    }
}
