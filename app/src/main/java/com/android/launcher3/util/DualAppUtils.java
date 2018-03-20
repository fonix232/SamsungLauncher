package com.android.launcher3.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.UserHandle;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.multiselect.MultiSelectManager;
//import com.samsung.android.app.SemDualAppManager;

public final class DualAppUtils {
    private static final String DUAL_APP_CREATE_CLASS_NAME = "com.samsung.android.da.daagent.CreateDualIM";
    public static final String DUAL_APP_DAAGENT_PACKAGE_NAME = "com.samsung.android.da.daagent";
    private static final String DUAL_APP_REMOVE_CLASS_NAME = "com.samsung.android.da.daagent.RemoveDualIM";
    private static final String PACKAGE_NAME = "packageName";
    private static final String TAG = "DualAppUtils";
    private static final String USER_ID = "userId";

    public static boolean supportDualApp(Context context) {
        try {
            SemDualAppManager dualAppManager = SemDualAppManager.getInstance(context);
            if (dualAppManager != null) {
                return dualAppManager.isSupported();
            }
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "Method not found : " + e.toString());
        }
        return false;
    }

    public static boolean isPackageInDualAppList(Context context, String packageName) {
        return SemDualAppManager.getInstance(context).isWhitelistedPackage(packageName);
    }

    public static boolean canInstallDualApp(Context context, UserHandleCompat user, String packageName) {
        return isPackageInDualAppList(context, packageName) && user.hashCode() == 0 && !SemDualAppManager.isInstalledWhitelistedPackage(packageName);
    }

    public static void installDualApp(Context context, String packageName) {
        Intent intent = new Intent();
        intent.setClassName(DUAL_APP_DAAGENT_PACKAGE_NAME, DUAL_APP_CREATE_CLASS_NAME);
        intent.putExtra("packageName", packageName);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to launch. intent =" + intent, e);
        }
    }

    public static void uninstallOrDisableDualApp(Context context, String packageName, UserHandleCompat user) {
        Intent intent = new Intent();
        intent.setClassName(DUAL_APP_DAAGENT_PACKAGE_NAME, DUAL_APP_REMOVE_CLASS_NAME);
        intent.putExtra("packageName", packageName);
        intent.putExtra(USER_ID, user.hashCode());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MultiSelectManager multiSelectMgr = ((Launcher) context).getMultiSelectManager();
            if (multiSelectMgr != null) {
                multiSelectMgr.postUninstallActivity();
            }
            Log.e(TAG, "Unable to launch. intent =" + intent, e);
        }
    }

    public static boolean isDualAppId(UserHandleCompat user) {
        if (TestHelper.isRoboUnitTest()) {
            return false;
        }
        return SemDualAppManager.isDualAppId(user.getUser().semGetIdentifier());
    }

    public static Bitmap makeUserBadgedIcon(Context context, Bitmap icon, int iconSize, UserHandle user) {
        return BitmapUtils.createIconBitmap(context.getPackageManager().getUserBadgedIcon(BitmapUtils.createIconDrawable(icon, iconSize), user), context);
    }

    public static boolean isDualApp(UserHandleCompat user, String packageName) {
        return isDualAppId(user) && SemDualAppManager.isInstalledWhitelistedPackage(packageName);
    }

    public static boolean hasDualApp(UserHandleCompat user, String packageName) {
        return user.hashCode() == 0 && SemDualAppManager.isInstalledWhitelistedPackage(packageName);
    }
}
