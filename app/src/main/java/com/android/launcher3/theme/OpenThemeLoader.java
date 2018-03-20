package com.android.launcher3.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import com.android.launcher3.theme.OpenThemeManager.ThemeType;

public class OpenThemeLoader {
    private static final String[] CURRENT_THEME_PACKAGE = new String[]{"", "current_sec_home_theme_package", "current_sec_appicon_theme_package", "current_sec_eventhome_theme_package", "current_sec_active_themepackage", "current_sec_active_themepackage"};
    private static final String TAG = "OpenThemeLoader";
    private boolean[] changedTheme = new boolean[]{false, false, false, false, false, false};
    private boolean isThemeChanged = false;
    private final Context mContext;
    private String[] mThemePackages = new String[]{null, null, null, null, null, null};

    public boolean isThemeChanged() {
        return this.isThemeChanged;
    }

    public void setIsThemeChanged(boolean isThemeChanged) {
        this.isThemeChanged = isThemeChanged;
    }

    public OpenThemeLoader(Context context) {
        this.mContext = context;
        loadCurrentThemePackages();
        setTheme();
    }

    public void reloadThemePackages() {
        loadCurrentThemePackages();
        setTheme();
    }

    private void loadCurrentThemePackages() {
        Resources r;
        int resId;
        int i = 1;
        while (i < this.mThemePackages.length) {
            try {
                this.mThemePackages[i] = null;
                this.mThemePackages[i] = System.getString(this.mContext.getContentResolver(), CURRENT_THEME_PACKAGE[i]);
                if (i == ThemeType.WINSET.value()) {
                    this.mThemePackages[i] = this.mThemePackages[i].concat(".common");
                } else if (i == ThemeType.BADGE.value()) {
                    this.mThemePackages[i] = this.mThemePackages[i].concat(".mms");
                }
            } catch (NullPointerException e) {
                if (!(i == ThemeType.WINSET.value() || i == ThemeType.BADGE.value())) {
                    try {
                        System.putString(this.mContext.getContentResolver(), CURRENT_THEME_PACKAGE[i], this.mContext.getPackageName());
                    } catch (Exception e2) {
                        Log.d(TAG, "fail to add default package name to " + i);
                    }
                    this.mThemePackages[i] = this.mContext.getPackageName();
                }
            }
            i++;
        }
        if (!(this.mThemePackages[1] == null || this.mThemePackages[1].isEmpty())) {
            r = getResources(this.mThemePackages[1]);
            if (r != null) {
                resId = r.getIdentifier("homescreen_menu_page_navi_home_f", "drawable", this.mThemePackages[1]);
                if (resId != 0) {
                    try {
                        r.getDrawable(resId);
                    } catch (NotFoundException e3) {
                        Log.d(TAG, "Theme package[1] has existed but no resources, set Default Theme.");
                        this.mThemePackages[ThemeType.HOME.value()] = this.mContext.getPackageName();
                        this.mThemePackages[ThemeType.APP_ICON.value()] = this.mContext.getPackageName();
                    }
                }
            }
        }
        if (!(this.mThemePackages[2] == null || this.mThemePackages[2].isEmpty())) {
            r = getResources(this.mThemePackages[2]);
            if (r != null) {
                resId = r.getIdentifier("ic_allapps", "drawable", this.mThemePackages[2]);
                if (resId != 0) {
                    try {
                        r.getDrawable(resId);
                    } catch (NotFoundException e4) {
                        Log.d(TAG, "Theme package[2] has existed but no resources, set Default Theme.");
                        this.mThemePackages[ThemeType.HOME.value()] = this.mContext.getPackageName();
                        this.mThemePackages[ThemeType.APP_ICON.value()] = this.mContext.getPackageName();
                    }
                }
            }
        }
        i = 0;
        while (i < this.mThemePackages.length) {
            if (this.mThemePackages[i] == null || this.mThemePackages[i].isEmpty()) {
                this.mThemePackages[i] = this.mContext.getPackageName();
            }
            i++;
        }
    }

    private void setTheme() {
        boolean isNotFound = false;
        PackageManager pm = this.mContext.getPackageManager();
        for (int i = 1; i < this.mThemePackages.length; i++) {
            String themePackageName = getThemePackageName(i);
            Log.i(TAG, "OpenThemeLoader::setTheme() (" + i + ") currentTheme : " + themePackageName);
            if (themePackageName.isEmpty()) {
                setChangedTheme(i, false);
                if (i < ThemeType.WINSET.value()) {
                    setThemePackageName(i, this.mContext.getPackageName());
                }
            } else if (pm != null) {
                try {
                    pm.getApplicationInfo(themePackageName, 0);
                    isNotFound = false;
                } catch (NameNotFoundException e) {
                    Log.d(TAG, "Theme package " + themePackageName + " not founded");
                    if (i < ThemeType.WINSET.value()) {
                        setThemePackageName(i, this.mContext.getPackageName());
                    } else {
                        setThemePackageName(i, getThemePackageName(ThemeType.HOME.value()));
                    }
                    isNotFound = true;
                }
            } else {
                Log.d(TAG, "the variable pm is null in setTheme()");
            }
            if (!isNotFound) {
                String preferences = "themePreference";
                String preferences_prevHomeTheme = "prevHomeTheme_";
                SharedPreferences pref = this.mContext.getSharedPreferences("themePreference", 0);
                String previousTheme = pref.getString("prevHomeTheme_" + i, this.mContext.getPackageName());
                Long lastUpdateTime = Long.valueOf(pref.getLong("prevHomeTheme_" + i + "_lastUpdateTime", 0));
                Integer versionCode = Integer.valueOf(pref.getInt("prevHomeTheme_" + i + "_versionCode", 0));
                PackageInfo info = null;
                boolean isTheSameVersion = true;
                if (pm != null) {
                    try {
                        info = pm.getPackageInfo(themePackageName, 8192);
                        if (info.lastUpdateTime == lastUpdateTime.longValue()) {
                            if (info.versionCode == versionCode.intValue()) {
                                isTheSameVersion = true;
                            }
                        }
                        isTheSameVersion = false;
                    } catch (NameNotFoundException e2) {
                        Log.d(TAG, "Package not found", e2);
                    }
                } else {
                    Log.d(TAG, "the variable pm is null in setTheme()");
                }
                Log.i(TAG, "OpenThemeLoader::setTheme() (" + i + ") PreviousTheme : " + previousTheme + " , isTheSameVersion = " + isTheSameVersion);
                if (previousTheme.equals(themePackageName) && isTheSameVersion) {
                    setChangedTheme(i, false);
                } else if (i < ThemeType.EVENT.value()) {
                    this.isThemeChanged = true;
                    Editor editor = pref.edit();
                    editor.putString("prevHomeTheme_" + i, themePackageName);
                    if (info != null) {
                        editor.putLong("prevHomeTheme_" + i + "_lastUpdateTime", info.lastUpdateTime);
                        editor.putInt("prevHomeTheme_" + i + "_versionCode", info.versionCode);
                    }
                    editor.apply();
                    setChangedTheme(i, true);
                }
            }
        }
    }

    public Resources getResources(String themePackageName) {
        if (themePackageName == null || themePackageName.isEmpty()) {
            return this.mContext.getResources();
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            if (pm != null) {
                return pm.getResourcesForApplication(themePackageName);
            }
            Log.d(TAG, "the variable pm is null in getResources()");
            return null;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "NameNotFoundException:" + e.toString());
            return null;
        }
    }

    public String getThemePackageName(int type) {
        return this.mThemePackages[type];
    }

    private void setThemePackageName(int type, String themePackageName) {
        this.mThemePackages[type] = themePackageName;
    }

    public boolean getChangedTheme(int themeType) {
        if (themeType < this.mThemePackages.length) {
            return this.changedTheme[themeType];
        }
        return false;
    }

    private void setChangedTheme(int themeType, boolean changed) {
        if (themeType < this.mThemePackages.length) {
            this.changedTheme[themeType] = changed;
        }
    }

    public String getDefaultPackageName() {
        return this.mThemePackages[ThemeType.DEFAULT.value()];
    }
}
