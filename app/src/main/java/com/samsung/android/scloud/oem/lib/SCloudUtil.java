package com.samsung.android.scloud.oem.lib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SCloudUtil {
    private static final String ACCOUNT_TYPE = "com.osp.app.signin";
    private static final String SCLOUD_QUERY_PARAM_NAME = "caller_is_syncadapter";
    private static final String SCLOUD_QUERY_PARAM_VALUE = "true";
    private static final String TAG = "SCloudSyncUtil";
    private static final String TIME_DIFFERENCE = "TIME_DIFFERENCE";

    public static Account getSamsungAccount(Context context) {
        AccountManager am = AccountManager.get(context);
        if (am == null) {
            return null;
        }
        Account[] accounts = am.getAccountsByType("com.osp.app.signin");
        if (accounts == null || accounts.length < 1) {
            return null;
        }
        return accounts[0];
    }

    public static boolean isCalledBySCloud(Uri uri) {
        return SCLOUD_QUERY_PARAM_VALUE.equals(uri.getQueryParameter(SCLOUD_QUERY_PARAM_NAME));
    }

    public static long getCurrentSyncTimestamp(Context context) {
        long uTimeDifference;
        try {
            uTimeDifference = System.getLong(context.getContentResolver(), TIME_DIFFERENCE);
        } catch (SettingNotFoundException e) {
            LOG.f(TAG, "Time Difference not stored. " + e.getMessage());
            uTimeDifference = 0;
        }
        return System.currentTimeMillis() - uTimeDifference;
    }

    public static void visibleSync(Account account, String authority, boolean isVisible) {
        LOG.f(TAG, "visibleSync. " + account + ", authority : " + authority + ", isVisible : " + isVisible);
        if (account != null) {
            ContentResolver.setIsSyncable(account, authority, isVisible ? 1 : 0);
            ContentResolver.setSyncAutomatically(account, authority, isVisible);
            if (!isVisible) {
                ContentResolver.cancelSync(account, authority);
            }
        }
    }

    public static void ensureValidFileName(String fileName) {
        if ("..".equals(fileName) || fileName.contains("../") || fileName.contains("/..")) {
            throw new IllegalArgumentException(".. path specifier not allowed. Bad file name: " + fileName);
        }
    }

    public static FileDescriptor getFileDescriptor(Context context, String backupFileUri, String fKey) {
        try {
            return context.getContentResolver().openFileDescriptor(Uri.parse(backupFileUri + fKey), "rw").getFileDescriptor();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}
