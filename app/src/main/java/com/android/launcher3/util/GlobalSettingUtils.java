package com.android.launcher3.util;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.home.HomeController;

public class GlobalSettingUtils {
    private static final String TAG = "GlobalSettingUtils";
    static boolean mIsBackToSetting = false;
    static boolean mIsSettingMultiWindow = false;
    static boolean mIsStartSetting = false;
    static String mSettingActivityName = "";
    static String mSettingPackageName = "";
    private HomeController mHomeController = this.mLauncher.getHomeController();
    private Launcher mLauncher;

    public GlobalSettingUtils(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public void startHomeSettingBySettingMenu(Intent intent) {
        String settingPackageName = intent.getStringExtra("PackageName");
        String settingActivityName = intent.getStringExtra("ClassName");
        mSettingPackageName = settingPackageName;
        mSettingActivityName = settingActivityName;
        mIsStartSetting = true;
        mIsBackToSetting = false;
        mIsSettingMultiWindow = intent.getBooleanExtra("isInMultiWindowMode", false);
        this.mLauncher.startHomeSettingActivity(true);
    }

    public void checkEnterNormalState() {
        if (mIsBackToSetting) {
            StageEntry data = new StageEntry();
            data.enableAnimation = false;
            this.mLauncher.getStageManager().finishAllStage(data);
            this.mHomeController.enterNormalState(false);
            mIsBackToSetting = false;
        }
    }

    public static void resetSettingsValue() {
        mSettingPackageName = "";
        mSettingActivityName = "";
        mIsStartSetting = false;
    }

    public static boolean getStartSetting() {
        return mIsStartSetting;
    }

    public static void setBackToSetting(boolean isBackToSetting) {
        mIsBackToSetting = isBackToSetting;
    }

    public static boolean getSettingMultiWindow() {
        return mIsSettingMultiWindow;
    }

    public static ComponentName getSettingCN() {
        ComponentName result;
        if (mSettingPackageName == null || mSettingActivityName == null) {
            result = new ComponentName("com.android.settings", "com.android.settings.Settings");
        } else {
            result = new ComponentName(mSettingPackageName, mSettingActivityName);
        }
        if (Utilities.DEBUGGABLE()) {
            Log.d(TAG, "Setting Component = " + result.toString());
        }
        return result;
    }
}
