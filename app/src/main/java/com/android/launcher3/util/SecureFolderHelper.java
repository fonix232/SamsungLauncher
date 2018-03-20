package com.android.launcher3.util;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.view.PointerIconCompat;
import android.util.Log;
import android.view.View;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.view.IconView;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.Iterator;

public final class SecureFolderHelper {
    private static final String INTENT_INSTALL_APK = "knox.lwc.action.INSTALL_APK";
    private static final String PERMISSION_INSTALL_APK = "com.sec.knox.APK_INSTALL_LWC";
    public static final String SECURE_FOLDER_PACKAGE_NAME = "com.samsung.knox.securefolder";
    private static final String TAG = "SecureFolderHelper";
    private static int sSecureFolderId = 0;

    private SecureFolderHelper() {
    }

    public static boolean canAddAppToSecureFolder(Context context, UserHandleCompat user, String packageName) {
        if (DualAppUtils.isDualAppId(user)) {
            return false;
        }
        sSecureFolderId = getSecureFolderId(context);
        return canAddAppToSecureFolder(context, sSecureFolderId, packageName);
    }

    @SuppressLint({"WrongConstant"})
    private static boolean canAddAppToSecureFolder(Context context, int secureFolderId, String packageName) {
        SemPersonaManager personaManager = (SemPersonaManager) context.getSystemService("persona");
        return personaManager != null && personaManager.isInstallableAppInContainer(context, secureFolderId, packageName);
    }

    @SuppressLint({"WrongConstant"})
    private static int getSecureFolderId(Context context) {
        SemPersonaManager personaManager = (SemPersonaManager) context.getSystemService("persona");
        if (personaManager == null) {
            return 0;
        }
        int userId = 0;
        Iterator<Bundle> itr = personaManager.getMoveToKnoxMenuList(context).iterator();
        while (itr.hasNext()) {
            Bundle item = (Bundle) itr.next();
            if (item.getInt("com.sec.knox.moveto.containerType") == PointerIconCompat.TYPE_HAND) {
                userId = item.getInt("com.sec.knox.moveto.containerId");
            }
        }
        return userId;
    }

    public static void addAppToSecureFolder(Context context, String packageName) {
        ArrayList<ArrayList<String>> packageList = new ArrayList();
        ArrayList<String> component = new ArrayList();
        component.add(0, packageName);
        component.add(1, "");
        packageList.add(component);
        sendBroadcastAsUser(context, packageList);
    }

    public static void addAppToSecureFolder(Context context, ArrayList<View> appsViewList) {
        ArrayList<ArrayList<String>> packageList = new ArrayList();
        sSecureFolderId = getSecureFolderId(context);
        Iterator it = appsViewList.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) ((View) it.next()).getTag();
            ComponentName cn = info.componentName;
            if (cn == null) {
                cn = ((IconInfo) info).getTargetComponent();
            }
            if (cn != null) {
                String packageName = cn.getPackageName();
                if (canAddAppToSecureFolder(context, sSecureFolderId, packageName)) {
                    ArrayList<String> component = new ArrayList();
                    component.add(0, packageName);
                    component.add(1, "");
                    packageList.add(component);
                }
            }
        }
        if (packageList.size() > 0) {
            sendBroadcastAsUser(context, packageList);
        }
    }

    private static void sendBroadcastAsUser(Context context, ArrayList<ArrayList<String>> packageList) {
        Intent intent = new Intent(INTENT_INSTALL_APK);
        intent.putExtra("from_app", "Launcher");
        intent.putExtra(IconView.EXTRA_SHORTCUT_USER_ID, sSecureFolderId);
        intent.putExtra("packages", packageList);
        intent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        context.sendBroadcastAsUser(intent, UserHandle.SEM_ALL, PERMISSION_INSTALL_APK);
    }

    public static boolean isSecureFolderExist(Context context) {
        if (context != null) {
            for (UserHandle userHandle : ((UserManager) context.getSystemService("user")).getUserProfiles()) {
                if (SemPersonaManager.isSecureFolderId(userHandle.semGetIdentifier())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getSecureFolderTitle(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(SECURE_FOLDER_PACKAGE_NAME, 0));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
