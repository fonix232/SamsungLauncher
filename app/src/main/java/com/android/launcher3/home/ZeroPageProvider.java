package com.android.launcher3.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;

public class ZeroPageProvider extends ContentProvider {
    private static final String AUTHORITY = "com.sec.android.app.launcher.zeropage";
    private static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.zeropage");
    private static final String GET_ZEROPAGE_ACTIVE = "get_zeropage_active";
    private static final String GET_ZEROPAGE_SETTING = "get_zeropage_setting";
    private static final String LAUNCHER_CLASS_NAME = "launcher_class_name";
    private static final String LAUNCHER_PACKAGE_NAME = "launcher_package_name";
    public static final String START_FROM_ZEROPAGE = "start_from_zeropage";
    private static final String ZEROPAGE_ACTIVE = "zeropage_active";
    private static final String ZEROPAGE_CLASS_NAME = "zeropage_class_name";
    public static final String ZEROPAGE_DEFAULT_HOME = "zeropage_default_home";
    private static final String ZEROPAGE_PACKAGE_NAME = "zeropage_package_name";

    public boolean onCreate() {
        return false;
    }

    @Nullable
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    public Bundle call(String method, String arg, Bundle extras) {
        Object obj = -1;
        switch (method.hashCode()) {
            case -1366873051:
                if (method.equals(GET_ZEROPAGE_ACTIVE)) {
                    obj = 1;
                    break;
                }
                break;
            case -570620655:
                if (method.equals(GET_ZEROPAGE_SETTING)) {
                    obj = null;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                return getZeroPageSettings();
            case 1:
                return getZeroPageActive();
            default:
                return null;
        }
    }

    public static void notifyChange(Context context) {
        context.getContentResolver().notifyChange(CONTENT_URI, null);
    }

    private Bundle getZeroPageSettings() {
        boolean z;
        boolean z2 = true;
        Bundle settingBundle = new Bundle();
        String zeropage_package = ZeroPageController.sZeroPageCompName.getPackageName();
        String zeropage_class = ZeroPageController.sZeroPageCompName.getClassName();
        settingBundle.putString(ZEROPAGE_PACKAGE_NAME, zeropage_package);
        settingBundle.putString(ZEROPAGE_CLASS_NAME, zeropage_class);
        settingBundle.putString(LAUNCHER_PACKAGE_NAME, getContext().getPackageName());
        settingBundle.putString(LAUNCHER_CLASS_NAME, Launcher.class.getName());
        boolean isInstalled = Utilities.isPackageExist(getContext(), zeropage_package);
        boolean isActiveZeroPage = ZeroPageController.isActiveZeroPage(getContext(), true);
        boolean isStartFromZeroPage = Utilities.getZeroPageKey(getContext(), START_FROM_ZEROPAGE);
        String str = START_FROM_ZEROPAGE;
        if (isInstalled && isActiveZeroPage && isStartFromZeroPage) {
            z = true;
        } else {
            z = false;
        }
        settingBundle.putBoolean(str, z);
        boolean isDefaultZeroPage = Utilities.getZeroPageKey(getContext(), ZEROPAGE_DEFAULT_HOME);
        String str2 = ZEROPAGE_DEFAULT_HOME;
        if (!(isInstalled && isActiveZeroPage && isDefaultZeroPage)) {
            z2 = false;
        }
        settingBundle.putBoolean(str2, z2);
        return settingBundle;
    }

    private Bundle getZeroPageActive() {
        boolean z = true;
        Bundle activeBundle = new Bundle();
        boolean isInstalled = Utilities.isPackageExist(getContext(), ZeroPageController.sZeroPageCompName.getPackageName());
        boolean isActiveZeroPage = ZeroPageController.isActiveZeroPage(getContext(), true);
        String str = ZEROPAGE_ACTIVE;
        if (!(isInstalled && isActiveZeroPage)) {
            z = false;
        }
        activeBundle.putBoolean(str, z);
        return activeBundle;
    }
}
