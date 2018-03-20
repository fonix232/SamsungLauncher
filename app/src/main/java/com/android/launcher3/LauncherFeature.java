package com.android.launcher3;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.features.CscFeature;
import com.android.launcher3.features.FloatingFeature;
import com.sec.android.app.launcher.R;
import java.util.List;

public class LauncherFeature {
    private static final String OEM_DIALER_CLASS_NAME = "com.android.dialer.DialtactsActivity";
    private static String TAG = LauncherFeature.class.getSimpleName();
    private static final String T_DIALER_CLASS_NAME = "com.skt.prod.dialer.activities.main.MainActivity";
    private static final String T_DIALER_PACKAGE_NAME = "com.skt.prod.dialer";
    private static boolean mSupportFolderLock = false;
    private static boolean mSupportSSecure = false;
    private static String sBuildFlavor;
    private static String sCountryCode;
    private static String sCscClockPackageName = null;
    private static boolean sDisableFullyHideKeypad = false;
    private static boolean sEnableStartActivityTouchDown = false;
    private static String sFloatingClockPackageName = null;
    private static String sHomeAppsStructureFeature = null;
    private static boolean sIsATT = false;
    private static boolean sIsChinaModel = false;
    private static boolean sIsCruiserProject = false;
    private static boolean sIsDreamProject = false;
    private static boolean sIsEdge = false;
    private static boolean sIsJapanModel = false;
    private static int sIsKnoxMode = -1;
    private static boolean sIsLargeTablet = false;
    private static boolean sIsSPR = false;
    private static int sIsSecureFolderMode = -1;
    private static boolean sIsTabAOSupProject = false;
    private static boolean sIsTablet = false;
    private static boolean sIsVZW = false;
    private static String sOemDialerPackageName = null;
    private static String sProductName;
    private static String sSalesCode;
    private static boolean sSupportAboutPage = true;
    private static boolean sSupportBackgroundBlurByWindow = true;
    private static boolean sSupportContextServiceSurveyMode = false;
    private static boolean sSupportCustomerDialerChange = false;
    private static boolean sSupportDeepShortcut = Utilities.ATLEAST_O;
    private static boolean sSupportEasyModeChange = true;
    private static boolean sSupportFestivalPage = false;
    private static boolean sSupportFlexibleGrid = true;
    private static boolean sSupportFolderColorPicker = false;
    private static boolean sSupportFolderLock = false;
    private static boolean sSupportFolderNSecOpen = true;
    private static boolean sSupportFolderSelect = true;
    private static boolean sSupportGSAPreWarming = true;
    private static boolean sSupportGSARoundingFeature = false;
    private static boolean sSupportGalaxyAppsSearch = true;
    private static boolean sSupportHomeModeChange = true;
    private static int sSupportHomeModeChangeIndex = 1;
    private static boolean sSupportHotword = false;
    private static boolean sSupportMultiSelect = true;
    private static boolean sSupportNavigationBar = false;
    private static boolean sSupportNewWidgetList = Utilities.ATLEAST_O;
    private static boolean sSupportNfcHwKeyboard = false;
    private static boolean sSupportNotificationBadge = false;
    private static boolean sSupportNotificationPanelExpansion = true;
    private static boolean sSupportNotificationPanelExpansionWithHomeMoving = false;
    private static boolean sSupportPairApps = Utilities.ATLEAST_O;
    private static boolean sSupportQuickOption = true;
    private static boolean sSupportRotationSetting = false;
    private static boolean sSupportSetToZeroPage = false;
    private static boolean sSupportWallpaperTilt = true;
    private static boolean sSupportZeroPageHome = false;
    private static String sWallpaperUseFixedOrientaion = null;

    public static synchronized void init(Context context) {
        synchronized (LauncherFeature.class) {
            long start = System.currentTimeMillis();
            readSystemProperties();
            readCSCFeature();
            readFloatingFeature();
            setFeatureBySystemProperties();
            checkNavigationBar(context);
            checkEdgeDevice(context);
            readConfigValue(context);
            Log.d(TAG, "LauncherFeature init : " + (System.currentTimeMillis() - start));
        }
    }

    private static void readSystemProperties() {
        // TODO: Samsung specific code
//        String omcSalesCode = "";
//        String cscSalesCode = "";
//        String omcCountryCode = "";
//        String cscCountryCode = "";
//        try {
//            omcSalesCode = SemSystemProperties.get("persist.omc.sales_code");
//            cscSalesCode = SemSystemProperties.get("ro.csc.sales_code");
//            omcCountryCode = SemSystemProperties.get("persist.omc.country_code");
//            cscCountryCode = SemSystemProperties.get("ro.csc.country_code");
//            if (TextUtils.isEmpty(cscCountryCode)) {
//                cscCountryCode = SemSystemProperties.get("ril.sales_code");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "read SalesCode or CountryCode exception occurred" + e);
//        }
//        sProductName = SemSystemProperties.get("ro.product.name");
//        sBuildFlavor = SemSystemProperties.get("ro.build.flavor");
//        if (omcSalesCode == null || "".equals(omcSalesCode)) {
//            omcSalesCode = cscSalesCode;
//        }
//        sSalesCode = omcSalesCode;
//        if (omcCountryCode == null || "".equals(omcCountryCode)) {
//            omcCountryCode = cscCountryCode;
//        }
//        sCountryCode = omcCountryCode;
//        sEnableStartActivityTouchDown = "true".equals(SemSystemProperties.get("sys.config.activelaunch_enable"));
        if (sSupportHotword) {
            sSupportHotword = !("CHZ".equals(sSalesCode) || "CHN".equals(sSalesCode) || "CHM".equals(sSalesCode) || "CHU".equals(sSalesCode) || "CTC".equals(sSalesCode) || "CHC".equals(sSalesCode));
        }
    }

    private static void readFloatingFeature() {
        sSupportNfcHwKeyboard = FloatingFeature.getBoolean("SEC_FLOATING_FEATURE_COMMON_SUPPORT_NFC_HW_KEYBOARD");
        sSupportContextServiceSurveyMode = FloatingFeature.getBoolean("SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE");
        sSupportBackgroundBlurByWindow = FloatingFeature.getBoolean("SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG");
        sFloatingClockPackageName = FloatingFeature.getString("SEC_FLOATING_FEATURE_CLOCK_CONFIG_PACKAGE_NAME", LiveIconManager.DEFAULT_PACKAGE_NAME_CLOCK);
        sCscClockPackageName = FloatingFeature.getString("CscFeature_Clock_ConfigReplacePackage", LiveIconManager.DEFAULT_PACKAGE_NAME_CLOCK);
        sWallpaperUseFixedOrientaion = FloatingFeature.getString("SEC_FLOATING_FEATURE_LOCKSCREEN_CONFIG_WALLPAPER_STYLE");
    }

    private static void readCSCFeature() {
        boolean z = false;
        sSupportHotword = !CscFeature.getBoolean("CscFeature_Common_DisableGoogle");
        sHomeAppsStructureFeature = CscFeature.getString("CscFeature_Launcher_ConfigHomeAppsStructure", null);
        if (!sIsVZW) {
            sIsVZW = "VZW".equals(sSalesCode);
        }
        if (!sIsATT) {
            sIsATT = "ATT".equals(sSalesCode);
        }
        if (!sIsChinaModel) {
            sIsChinaModel = "China".equalsIgnoreCase(sCountryCode);
        }
        if (!sIsJapanModel) {
            if ("DCM".equals(sSalesCode) || "KDI".equals(sSalesCode)) {
                z = true;
            }
            sIsJapanModel = z;
        }
        if (!sIsSPR) {
            sIsSPR = CscFeature.getBoolean("CscFeature_Common_EnableSprintExtension");
        }
    }

    private static void setFeatureBySystemProperties() {
        if (!sDisableFullyHideKeypad) {
            sDisableFullyHideKeypad = "USA".equals(sCountryCode);
        }
        sSupportCustomerDialerChange = "SKT".equals(sSalesCode) || "SKC".equals(sSalesCode);
        setSupportFolderLock();
        setSupportSSecure();
    }

    private static void checkNavigationBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId > 0) {
            sSupportNavigationBar = res.getBoolean(resourceId);
        }
    }

    private static void checkEdgeDevice(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            List<ResolveInfo> list = packageManager.queryIntentActivities(new Intent("com.samsung.app.cocktailbarservice.settings.SETTINGSMAIN"), PackageManager.MATCH_DEFAULT_ONLY);
            if (list != null && list.size() > 0) {
                sIsEdge = true;
                return;
            }
            return;
        }
        Log.e(TAG, "PackageManager is null in checkEdgeDevice()");
    }

    private static void readConfigValue(Context context) {
        boolean z = true;
        Resources res = context.getResources();
        PackageManager pm = context.getPackageManager();
        boolean z2 = pm != null && (pm.hasSystemFeature("com.samsung.feature.device_category_tablet") || pm.hasSystemFeature("com.samsung.feature.device_category_tablet_high_end") || pm.hasSystemFeature("com.samsung.feature.device_category_tablet_low_end"));
        sIsTablet = z2;
        if (!isStarProject() || sIsTablet) {
            z = false;
        }
        sSupportRotationSetting = z;
        sIsLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        sSupportFlexibleGrid = res.getBoolean(R.bool.is_supportFlexibleGrid);
        sSupportNotificationBadge = res.getBoolean(R.bool.notification_badging_enabled);
        sSupportFolderColorPicker = res.getBoolean(R.bool.folder_custom_color_picker_enabled);
    }

    public static boolean isChinaModel() {
        return sIsChinaModel;
    }

    public static boolean isJapanModel() {
        return sIsJapanModel;
    }

    public static boolean isVZWModel() {
        return sIsVZW;
    }

    public static boolean isATTModel() {
        return sIsATT;
    }

    public static boolean supportNavigationBar() {
        return sSupportNavigationBar;
    }

    static boolean isEdgeDevice() {
        return sIsEdge;
    }

    public static boolean supportHomeModeChange() {
        if (sHomeAppsStructureFeature != null && sHomeAppsStructureFeature.contains("support_homeonly")) {
            if (sHomeAppsStructureFeature.contains("homeapps")) {
                sSupportHomeModeChangeIndex = 1;
            } else {
                sSupportHomeModeChangeIndex = 0;
            }
            sHomeAppsStructureFeature = null;
        }
        return sSupportHomeModeChange;
    }

    static int getSupportHomeModeChangeIndex() {
        return sSupportHomeModeChangeIndex;
    }

    public static boolean supportFlexibleGrid() {
        return sSupportFlexibleGrid;
    }

    public static boolean supportNotificationBadge() {
        return sSupportNotificationBadge;
    }

    public static void setSupportFlexibleGrid(boolean support) {
        sSupportFlexibleGrid = support;
    }

    public static String getOmcPath() {
        return ""; // TODO: Samsung specific code // SemSystemProperties.get("persist.sys.omc_path");
    }

    public static boolean supportContextServiceSurveyMode() {
        return sSupportContextServiceSurveyMode;
    }

    public static boolean supportBackgroundBlurByWindow() {
        return sSupportBackgroundBlurByWindow;
    }

    public static boolean supportFolderNSecOpen() {
        return sSupportFolderNSecOpen;
    }

    public static boolean disableFullyHideKeypad() {
        return sDisableFullyHideKeypad;
    }

    public static boolean supportFestivalPage() {
        return sSupportFestivalPage;
    }

    public static boolean isSupportBadgeManage() {
        return false;
    }

    public static boolean supportQuickOption() {
        return sSupportQuickOption;
    }

    public static boolean supportDeepShortcut() {
        return sSupportDeepShortcut;
    }

    public static boolean supportNewWidgetList() {
        return sSupportNewWidgetList;
    }

    public static boolean supportPairApps() {
        return sSupportPairApps;
    }

    public static boolean supportNotificationPreview() {
        return sSupportNotificationBadge;
    }

    public static boolean supportGalaxyAppsSearch() {
        if (isVZWModel()) {
            sSupportGalaxyAppsSearch = false;
        }
        return sSupportGalaxyAppsSearch;
    }

    public static String getCustomerDialerClassName() {
        return T_DIALER_CLASS_NAME;
    }

    public static String getOemDialerClassName() {
        return OEM_DIALER_CLASS_NAME;
    }

    public static String getCustomerDialerPackageName() {
        return T_DIALER_PACKAGE_NAME;
    }

    public static String getOemDialerPackageName(Context context) {
        if (sOemDialerPackageName == null) {
            String originalPackageName = "com.android.contacts";
            String packageName = FloatingFeature.getString("SEC_FLOATING_FEATURE_CONTACTS_CONFIG_PACKAGE_NAME", "com.android.contacts");
            if ("com.android.contacts".equals(packageName)) {
                packageName = CscFeature.getString("CscFeature_Contact_ReplacePackageAs");
                if (TextUtils.isEmpty(packageName)) {
                    sOemDialerPackageName = "com.android.contacts";
                } else {
                    sOemDialerPackageName = packageName;
                }
            } else {
                sOemDialerPackageName = packageName;
                try {
                    context.getApplicationContext().getPackageManager().getPackageInfo(packageName, 0);
                } catch (NameNotFoundException e) {
                    sOemDialerPackageName = "com.android.contacts";
                }
            }
        }
        return sOemDialerPackageName;
    }

    public static boolean supportCustomerDialerChange() {
        return sSupportCustomerDialerChange;
    }

    public static void setSupportGalaxyAppsSearch(boolean set) {
        sSupportGalaxyAppsSearch = set;
    }

    public static boolean supportMultiSelect() {
        return sSupportMultiSelect;
    }

    public static boolean supportMultiSelectSlideVI() {
        return (sSupportNavigationBar || sIsTablet) ? false : true;
    }

    public static boolean supportFolderSelect() {
        return sSupportFolderSelect;
    }

    public static boolean supportEasyModeChange() {
        return sSupportEasyModeChange;
    }

    public static boolean supportRotationSetting() {
        return sSupportRotationSetting;
    }

    public static boolean isTablet() {
        return sIsTablet;
    }

    public static boolean isLargeTablet() {
        return sIsLargeTablet;
    }

    static boolean supportNfcHwKeyboard() {
        return sSupportNfcHwKeyboard;
    }

    public static boolean supportNotificationPanelExpansion() {
        return sSupportNotificationPanelExpansion;
    }

    public static boolean supportNotificationPanelExpansionWithHomeMoving() {
        return sSupportNotificationPanelExpansion && sSupportNotificationPanelExpansionWithHomeMoving;
    }

    public static boolean supportAboutPage() {
        if (sProductName != null) {
            sSupportAboutPage = !sProductName.contains("j3y17qltez");
        }
        return sSupportAboutPage;
    }

    public static boolean supportZeroPageHome() {
        return sSupportZeroPageHome;
    }

    private static void setSupportFolderLock() {
        String features = CscFeature.getString("CscFeature_SmartManager_ConfigSubFeatures");
        if (!TextUtils.isEmpty(features) && features.contains("applock") && isChinaModel()) {
            sSupportFolderLock = true;
        } else {
            sSupportFolderLock = false;
        }
        Log.i(TAG, "setSupportFolderLock supportFolderLock = " + sSupportFolderLock);
    }

    public static boolean supportFolderLock() {
        return sSupportFolderLock;
    }

    public static void setSupportSSecure() {
        // TODO: Samsung specific code
//        String features = SemCscFeature.getInstance().getString("CscFeature_Common_ConfigYuva");
//        if (TextUtils.isEmpty(features) || !features.contains("sprotect")) {
//            mSupportSSecure = false;
//        } else {
//            mSupportSSecure = true;
//        }

        mSupportSSecure = false;
        Log.i(TAG, "setSupportSSecure supportSSecure = " + mSupportSSecure + ", mSupportFolderLock = " + mSupportFolderLock);
    }

    public static boolean isSSecureSupported() {
        return mSupportSSecure;
    }

    static void setSupportWallpaperTilt(boolean support) {
        sSupportWallpaperTilt = support;
    }

    static boolean supportWallpaperTilt() {
        return sSupportWallpaperTilt;
    }

    public static boolean supportHotword() {
        return sSupportHotword;
    }

    public static boolean supportSprintExtension() {
        return sIsSPR;
    }

    public static boolean supportGSAPreWarming() {
        return sSupportGSAPreWarming;
    }

    public static boolean supportGSARoundingFeature() {
        if (sProductName != null) {
            boolean z = sProductName.contains("dream") || sBuildFlavor.contains("dream") || sProductName.contains("gracer") || sBuildFlavor.contains("gracer") || sProductName.contains("great") || sBuildFlavor.contains("great") || sProductName.contains("star") || sBuildFlavor.contains("star");
            sSupportGSARoundingFeature = z;
        }
        return sSupportGSARoundingFeature;
    }

    public static boolean isDreamProject() {
        if (sProductName != null) {
            boolean z = sProductName.contains("dream") || sBuildFlavor.contains("dream");
            sIsDreamProject = z;
        }
        return sIsDreamProject;
    }

    public static boolean isCruiserProject() {
        if (sProductName != null) {
            boolean z = sProductName.contains("cruiser") || sBuildFlavor.contains("cruiser");
            sIsCruiserProject = z;
        }
        return sIsCruiserProject;
    }

    public static boolean isStarProject() {
        return sProductName != null && (sProductName.startsWith("star") || sProductName.contains("SC-02K") || sProductName.contains("SC-03K") || sProductName.contains("SCV38") || sProductName.contains("SCV39") || sProductName.contains("SGH-N327") || sProductName.contains("SGH-N943") || sProductName.contains("PXZ") || sProductName.contains("QYA"));
    }

    public static boolean isSupportFolderColorPicker() {
        return sSupportFolderColorPicker;
    }

    public static boolean isTabAOSupProject() {
        if (sProductName != null) {
            boolean z = sProductName.contains("gt58") || sBuildFlavor.contains("gt58") || sProductName.contains("gt510") || sBuildFlavor.contains("gt510") || sProductName.contains("gt5note8") || sBuildFlavor.contains("gt5note8") || sProductName.contains("gt5note10") || sBuildFlavor.contains("gt5note10");
            sIsTabAOSupProject = z;
        }
        return sIsTabAOSupProject;
    }

    public static boolean enableStartActivityTouchDown() {
        return sEnableStartActivityTouchDown;
    }

    public static boolean supportSetToZeroPage() {
        return sSupportSetToZeroPage;
    }

    public static void setSupportSetToZeroPage(boolean support) {
        sSupportSetToZeroPage = support;
    }

    @NonNull
    public static final String getFloatingClockPackageName() {
        return sFloatingClockPackageName == null ? LiveIconManager.DEFAULT_PACKAGE_NAME_CLOCK : sFloatingClockPackageName;
    }

    @NonNull
    public static final String getCscClockPackageName() {
        return sCscClockPackageName == null ? LiveIconManager.DEFAULT_PACKAGE_NAME_CLOCK : sCscClockPackageName;
    }

    @NonNull
    public static boolean isWallpaperUseFixedOrientation() {
        return sWallpaperUseFixedOrientaion.contains("INCONSISTENCY");
    }

    public static boolean isKnoxMode() {
         // TODO: Samsung specific code
//        if (sIsKnoxMode == -1) {
//            int i;
//            if (SemPersonaManager.isKnoxId(UserHandle.semGetMyUserId())) {
//                i = 1;
//            } else {
//                i = 0;
//            }
//            sIsKnoxMode = i;
//        }
//        if (sIsKnoxMode == 1) {
//            return true;
//        }
        return false;
    }

    public static boolean isSecureFolderMode() {
        // TODO: Samsung specific code
//        if (sIsSecureFolderMode == -1) {
//            int i;
//            if (SemPersonaManager.isSecureFolderId(UserHandle.semGetMyUserId())) {
//                i = 1;
//            } else {
//                i = 0;
//            }
//            sIsSecureFolderMode = i;
//        }
//        if (sIsSecureFolderMode == 1) {
//            return true;
//        }
        return false;
    }

    public static boolean supportNotificationAndProviderBadge() {
        return Utilities.ATLEAST_O;
    }
}
