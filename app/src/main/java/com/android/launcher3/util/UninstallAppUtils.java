package com.android.launcher3.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;

public final class UninstallAppUtils {
    private static final String SCHEME = "package";
    private static final String TAG = "UninstallAppUtils";

    public static void startUninstallActivity(Context context, UserHandleCompat user, ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        if (packageName == null || className == null) {
            Log.d(TAG, "fail to uninstall app : " + packageName + " " + className);
            return;
        }
        Intent intent = new Intent("android.intent.action.DELETE", Uri.fromParts(SCHEME, packageName, className));
        intent.setFlags(276824064);
        intent.putExtra("android.intent.extra.USER", user.getUser());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "startUninstallActivity:" + e.toString());
        }
    }

    public static boolean startUninstallActivity(Launcher launcher, Object info) {
        Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
        if (componentInfo == null) {
            Log.e(TAG, "startUnistallActivity - componentInfo == null");
            return false;
        }
        return launcher.startApplicationUninstallActivity((ComponentName) componentInfo.first, ((Integer) componentInfo.second).intValue(), ((ItemInfo) info).user, false);
    }

    public static Pair<ComponentName, Integer> getAppInfoFlags(Object item) {
        if (item instanceof IconInfo) {
            IconInfo info = (IconInfo) item;
            ComponentName component = info.getTargetComponent();
            if (!(info.itemType != 0 || component == null || info.isPromise())) {
                return Pair.create(component, Integer.valueOf(info.flags));
            }
        }
        return null;
    }
}
