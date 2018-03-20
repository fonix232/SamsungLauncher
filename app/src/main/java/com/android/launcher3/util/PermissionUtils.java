package com.android.launcher3.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.android.launcher3.Launcher;
import java.util.ArrayList;

public class PermissionUtils {
    public static final int CHECK_ALREADY_PERMISSION = 0;
    public static final int CHECK_ERROR_PERMISSION = -1;
    public static final int CHECK_NEED_PERMISSION = 1;
    public static final String[] PERMISSIONS_STORAGE = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    public static final String[] PERMISSIONS_STORAGE_CONTACTS = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_CONTACTS"};
    public static final int REQUEST_BACKUP = 0;
    public static final int REQUEST_LAUCHER_EXTRACTOR = 2;
    public static final int REQUEST_RESTORE = 1;
    private static final String TAG = "PermissionUtils";

    public static int hasSelfPermission(Context context, String[] permissions, ArrayList<String> PermissionsList) {
        if (permissions == null || permissions.length < 1 || context == null) {
            Log.d(TAG, "hasSelfPermission : permissions, context is null.");
            return -1;
        }
        for (int index = 0; index < permissions.length; index++) {
            if (ContextCompat.checkSelfPermission(context, permissions[index]) != 0) {
                PermissionsList.add(permissions[index]);
            }
        }
        Log.d(TAG, "hasSelfPermission. PermissionList Size : " + PermissionsList.size());
        if (PermissionsList.size() <= 0) {
            return 0;
        }
        return 1;
    }

    public static String[] getPermissions(int request) {
        switch (request) {
            case 0:
                return PERMISSIONS_STORAGE_CONTACTS;
            case 1:
            case 2:
                return PERMISSIONS_STORAGE;
            default:
                return null;
        }
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activities, ArrayList<String> needPermissionsList) {
        if (needPermissionsList == null || needPermissionsList.size() < 1) {
            Log.d(TAG, "shouldShowRequestPermissionRationale : needPermissionsList is null.");
            return false;
        }
        String[] needPermission = (String[]) needPermissionsList.toArray(new String[needPermissionsList.size()]);
        int index = 0;
        while (index < needPermission.length) {
            Log.d(TAG, "shouldShowRequestPermissionRationale. permission : " + needPermission[index]);
            if (ActivityCompat.shouldShowRequestPermissionRationale(activities, needPermission[index])) {
                Log.d(TAG, "    true : " + needPermission[index]);
                index++;
            } else {
                Log.d(TAG, "    false : " + needPermission[index]);
                return false;
            }
        }
        return true;
    }

    public static void requestPermissions(Launcher launcher, ArrayList<String> needPermissionsList, int requestCode) {
        Log.d(TAG, "requestPermission : permission has NOT been granted. Requesting permissions.");
        if (needPermissionsList == null || needPermissionsList.size() < 1) {
            Log.d(TAG, "requestPermissions : needPermissionsList is null.");
        } else {
            ActivityCompat.requestPermissions(launcher, (String[]) needPermissionsList.toArray(new String[needPermissionsList.size()]), requestCode);
        }
    }

    public static boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }
        for (int result : grantResults) {
            if (result != 0) {
                return false;
            }
        }
        return true;
    }
}
