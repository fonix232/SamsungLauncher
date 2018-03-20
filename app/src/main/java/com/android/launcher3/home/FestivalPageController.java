package com.android.launcher3.home;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Settings.System;
import android.util.Log;
import android.util.SparseArray;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;

class FestivalPageController {
    private static final String ACTION_FESTIVAL_CARDWIDGET_ADDED = "com.android.launcher.action.FESTIVAL_CARDWIDGET_ADDED";
    private static final String ACTION_FESTIVAL_EVENTWIDGET_ADDED = "com.android.launcher.action.FESTIVAL_MYEVENTWIDGET_ADDED";
    private static final String ACTION_FESTIVAL_EVENTWIDGET_PERMISSION_DENY = "com.sec.android.widget.myeventwidget.FESTIVAL_CANCEL_ACTION";
    private static final String ACTION_FESTIVAL_EVENTWIDGET_UPDATE = "com.sec.android.widget.myeventwidget.FESTIVAL_PERMISSION_CHECK_CALLBACK";
    private static final String ACTION_FESTIVAL_GREETINGWIDGET_ADDED = "com.android.launcher.action.FESTIVAL_GREETINGWIDGET_ADDED";
    private static final int BIRTHDAY_FESTIVAL = 999;
    private static final int CHILDREN_DAY_FESTIVAL = 6;
    private static final int CHINESE_VALENTINE_DAY_FESTIVAL = 15;
    private static final int CHRISTMAS_FESTIVAL = 11;
    private static final int CONGRATULATION_FESTIVAL = 998;
    private static final String CURRENT_FESTIVAL_STRING_KEY = "current_sec_theme_package_event_title";
    private static final boolean DEBUG = false;
    private static final boolean DEBUGGABLE = true;
    private static final int DOUBLE_NINTH_DAY_FESTIVAL = 17;
    private static final int DRAGON_BOAT_FESTIVAL = 14;
    private static final String FESTIVAL_EFFECT_ENABLED = "current_sec_theme_package_festival_enabled";
    private static final int FESTIVAL_PAGE_INIT_ERROR = -1;
    private static final int LANTERN_FESTIVAL = 13;
    private static final int MAY_DAY_FESTIVAL = 4;
    private static final int MID_AUTUMN_FESTIVAL = 16;
    private static final String MYEVENT_ENABLED = "current_sec_theme_package_myevent_enabled";
    private static final String MYEVENT_WIDGET_CALSS_NAME = "com.sec.android.widget.myeventwidget.MyEventWidgetProvider";
    private static final String MYEVENT_WIDGET_PACKAGE_NAME = "com.sec.android.widget.myeventwidget";
    private static final int NATIONAL_DAY_FESTIVAL = 10;
    private static final int NEW_YEAR_DAY_FESTIVAL = 1;
    private static final String PREFERENCES_FESTIVAL_INTENT_STRING = "festivalstring";
    private static final String PREFERENCES_FESTIVAL_INTENT_STRING_HOMEONLY = "festivalstring_homeonly";
    private static final String PREFERENCES_FESTIVAL_PERMISSION_ENABLE = "festivalpermission_enable";
    private static final int SPRING_FESTIVAL = 12;
    private static final String TAG = "FestivalPageManager";
    private static final int TEACHER_DAY_FESTIVAL = 9;
    private static final int THANK_YOU_DAY_FESTIVAL = 3;
    private static final int TOMB_SWEEPING_DAY_FESTIVAL = 130;
    private static final int VALENTINE_DAY_FESTIVAL = 2;
    private static final int[] festivalIndexArray = new int[]{1, 2, 3, 4, 6, 9, 10, 17, 11, 12, 13, 14, 15, 16, TOMB_SWEEPING_DAY_FESTIVAL, CONGRATULATION_FESTIVAL, 999};
    private static final SparseArray<String> festivalNameArray = new SparseArray<String>() {
        {
            append(1, "new_year");
            append(2, "valentine");
            append(3, "thank_you");
            append(4, "may_day");
            append(6, "children");
            append(9, "teacher");
            append(10, "national");
            append(17, "double_ninth");
            append(11, "christmas");
            append(12, "chinese_new_year");
            append(13, "lantern");
            append(14, "dragon_boat");
            append(15, "chinese_valentine");
            append(16, "mid_autumn");
            append(FestivalPageController.TOMB_SWEEPING_DAY_FESTIVAL, "tomb_sweeping");
            append(FestivalPageController.CONGRATULATION_FESTIVAL, "congratulation");
            append(999, "birthday");
        }
    };
    private List<CellLayout> mBackupPages = new ArrayList();
    private boolean mFestivalEnabled = false;
    private AppWidgetHostView mFestivalHostView = null;
    private int mFestivalPageIndex = -1;
    private LauncherAppWidgetInfo mFestivalWidget = null;
    private int mFestivalWidgetId = -1;
    private HomeController mHomeController;
    private Launcher mLauncher;
    private Workspace mWorkspace;
    private boolean mWorkspaceLoaded = false;

    FestivalPageController(Launcher launcher, HomeController homeController) {
        this.mLauncher = launcher;
        this.mHomeController = homeController;
        this.mWorkspace = this.mHomeController.getWorkspace();
    }

    private void setFestivalEnabled() {
        boolean isFestivalChanged = isFestivalChanged(this.mLauncher);
        boolean isFestivalEnabled;
        if (System.getInt(this.mLauncher.getContentResolver(), FESTIVAL_EFFECT_ENABLED, 0) != 0) {
            isFestivalEnabled = true;
        } else {
            isFestivalEnabled = false;
        }
        String festivalString = getCurrentFestivalString(this.mLauncher);
        if (isFestivalChanged) {
            setFestivalPermissionEnabled(this.mLauncher, true);
        }
        if (!getFestivalPermissionEnabled(this.mLauncher) || !isFestivalEnabled || festivalString == null || festivalString.isEmpty()) {
            this.mFestivalEnabled = false;
        } else {
            this.mFestivalEnabled = true;
        }
    }

    boolean getFestivalEnabled() {
        return this.mFestivalEnabled;
    }

    void initFestivalPageIfNecessary() {
        onDestroy();
    }

    void bindFestivalPageIfNecessary() {
        if (this.mHomeController != null && this.mWorkspace != null) {
            this.mWorkspaceLoaded = true;
            setFestivalEnabled();
            if (this.mFestivalEnabled) {
                removeCustomFestivalPage();
                createCustomFestivalPage();
                bindFestivalWidgetIfNeccessary();
            }
        }
    }

    private void bindFestivalWidgetIfNeccessary() {
        ComponentName widgetCN = getFestivalCompName();
        if (widgetCN != null) {
            LauncherAppWidgetProviderInfo appWidgetInfo = HomeLoader.getProviderInfo(this.mLauncher, widgetCN, UserHandleCompat.myUserHandle());
            if (appWidgetInfo != null) {
                PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this.mLauncher, appWidgetInfo, null);
                pendingInfo.spanX = 5;
                pendingInfo.spanY = 5;
                pendingInfo.minSpanX = 5;
                pendingInfo.minSpanY = 5;
                Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this.mLauncher, pendingInfo);
                if (this.mFestivalWidgetId == -1) {
                    this.mFestivalWidgetId = this.mHomeController.getAppWidgetHost().allocateAppWidgetId();
                }
                if (AppWidgetManagerCompat.getInstance(this.mLauncher).bindAppWidgetIdIfAllowed(this.mFestivalWidgetId, appWidgetInfo, options)) {
                    if (this.mFestivalWidget == null) {
                        this.mFestivalWidget = new LauncherAppWidgetInfo(this.mFestivalWidgetId, appWidgetInfo.provider);
                    }
                    this.mFestivalWidget.spanX = pendingInfo.spanX;
                    this.mFestivalWidget.spanY = pendingInfo.spanY;
                    this.mFestivalWidget.minSpanX = pendingInfo.minSpanX;
                    this.mFestivalWidget.minSpanY = pendingInfo.minSpanY;
                    this.mFestivalWidget.user = UserHandleCompat.myUserHandle();
                    this.mFestivalWidget.restoreStatus = 0;
                    if (this.mFestivalHostView == null) {
                        this.mFestivalHostView = this.mHomeController.getAppWidgetHost().createView(this.mLauncher, this.mFestivalWidgetId, appWidgetInfo);
                    }
                    this.mFestivalWidget.hostView = this.mFestivalHostView;
                    this.mFestivalWidget.hostView.setTag(this.mFestivalWidget);
                    this.mFestivalWidget.onBindAppWidget(this.mLauncher);
                    getFestivalPageInfo(this.mFestivalWidget);
                    this.mHomeController.addInScreen(this.mFestivalWidget.hostView, this.mFestivalWidget.container, this.mFestivalWidget.screenId, this.mFestivalWidget.cellX, this.mFestivalWidget.cellY, this.mFestivalWidget.spanX, this.mFestivalWidget.spanY);
                    this.mHomeController.getBindController().addWidgetToAutoAdvanceIfNeeded(this.mFestivalWidget.hostView, appWidgetInfo);
                    sendFestivalWidgetType(this.mFestivalWidgetId);
                    return;
                }
                this.mHomeController.getAppWidgetHost().deleteAppWidgetId(this.mFestivalWidgetId);
                Log.d(TAG, "Removing festival widget: id=" + this.mFestivalWidgetId + " belongs to component " + widgetCN + ", as the launcher is unable to bing a new widget id");
                this.mFestivalWidgetId = -1;
            }
        }
    }

    private void getFestivalPageInfo(LauncherAppWidgetInfo info) {
        info.container = (long) this.mFestivalPageIndex;
        info.screenId = -501;
        info.cellX = 0;
        info.cellY = 0;
    }

    private boolean hasFestivalPage() {
        if (!this.mFestivalEnabled || getFestivalPageCount() <= 0) {
            return false;
        }
        return true;
    }

    void onDestroy() {
        deleteFestivalWidget();
        removeCustomFestivalPage();
        this.mBackupPages.clear();
        this.mFestivalWidget = null;
        this.mFestivalHostView = null;
        this.mFestivalWidgetId = -1;
        this.mFestivalPageIndex = -1;
        this.mFestivalEnabled = false;
        this.mWorkspaceLoaded = false;
        this.mWorkspace = null;
        this.mHomeController = null;
        this.mLauncher = null;
    }

    private void deleteFestivalWidget() {
        if (getFestivalPageCount() > 0) {
            this.mHomeController.getBindController().removeAppWidget(this.mFestivalWidget);
            if (this.mHomeController.getAppWidgetHost() != null) {
                this.mHomeController.getAppWidgetHost().deleteAppWidgetId(this.mFestivalWidget.appWidgetId);
            }
        }
    }

    void createCustomFestivalPage() {
        if (this.mFestivalEnabled && this.mWorkspaceLoaded && getFestivalPageCount() <= 0) {
            CellLayout festivalPageScreen;
            if (this.mBackupPages.size() > 0) {
                festivalPageScreen = (CellLayout) this.mBackupPages.get(0);
            } else {
                festivalPageScreen = (CellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, this.mWorkspace, false);
                festivalPageScreen.setCellDimensions(-1, -1, 0, 0);
                festivalPageScreen.setGridSize(1, 1);
                festivalPageScreen.setPadding(0, 0, 0, 0);
            }
            this.mBackupPages.clear();
            this.mWorkspace.getWorkspaceScreens().put(-501, festivalPageScreen);
            this.mWorkspace.getScreenOrder().add(this.mWorkspace.getChildCount(), Long.valueOf(-501));
            this.mWorkspace.addView(festivalPageScreen);
            getFestivalPageCount();
        }
    }

    void removeCustomFestivalPage() {
        if (getFestivalPageCount() > 0) {
            CellLayout festivalPageScreen = this.mWorkspace.getScreenWithId(-501);
            if (festivalPageScreen == null) {
                Log.e(TAG, "removeCustomFestivalPage - Expected custom festival page to exist");
                return;
            }
            this.mBackupPages.clear();
            this.mBackupPages.add(festivalPageScreen);
            this.mWorkspace.getWorkspaceScreens().remove(-501);
            this.mWorkspace.getScreenOrder().remove(Long.valueOf(-501));
            this.mWorkspace.removeView(festivalPageScreen);
            this.mFestivalPageIndex = -1;
        }
    }

    private int getFestivalPageCount() {
        if (this.mWorkspace == null) {
            return -1;
        }
        int returnCount = 0;
        this.mFestivalPageIndex = -1;
        for (int i = 0; i < this.mWorkspace.getChildCount(); i++) {
            if (this.mWorkspace.getIdForScreen((CellLayout) this.mWorkspace.getChildAt(i)) == -501) {
                this.mFestivalPageIndex = i;
                returnCount++;
            }
        }
        return returnCount;
    }

    private int getFestivalType(String festivalName) {
        int festivalkey = -1;
        for (int i = 0; i < festivalNameArray.size(); i++) {
            festivalkey = festivalIndexArray[i];
            if (festivalName.equals(festivalNameArray.get(festivalIndexArray[i]))) {
                Log.i(TAG, "getFestivalType of festivalName : " + festivalName + " festivalIndexArray[ " + i + " ]  = " + festivalIndexArray[i]);
                return festivalkey;
            }
        }
        return festivalkey;
    }

    private void setFestivalString(Launcher launcher) {
        String festivalString = getCurrentFestivalString(launcher);
        Editor editor = launcher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        if (LauncherFeature.supportHomeModeChange()) {
            editor.putString(PREFERENCES_FESTIVAL_INTENT_STRING_HOMEONLY, festivalString);
        } else {
            editor.putString(PREFERENCES_FESTIVAL_INTENT_STRING, festivalString);
        }
        editor.apply();
    }

    private void setFestivalString(Launcher launcher, String festivalString) {
        Editor editor = launcher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        if (LauncherFeature.supportHomeModeChange()) {
            editor.putString(PREFERENCES_FESTIVAL_INTENT_STRING_HOMEONLY, festivalString);
        } else {
            editor.putString(PREFERENCES_FESTIVAL_INTENT_STRING, festivalString);
        }
        editor.apply();
    }

    private String getFestivalString(Launcher launcher) {
        SharedPreferences prefs = launcher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportHomeModeChange()) {
            return prefs.getString(PREFERENCES_FESTIVAL_INTENT_STRING_HOMEONLY, null);
        }
        return prefs.getString(PREFERENCES_FESTIVAL_INTENT_STRING, null);
    }

    private void setFestivalPermissionEnabled(Launcher launcher, boolean enable) {
        Editor editor = launcher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        editor.putBoolean(PREFERENCES_FESTIVAL_PERMISSION_ENABLE, enable);
        editor.apply();
    }

    private boolean getFestivalPermissionEnabled(Launcher launcher) {
        return launcher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(PREFERENCES_FESTIVAL_PERMISSION_ENABLE, true);
    }

    private String getCurrentFestivalString(Launcher launcher) {
        return System.getString(launcher.getContentResolver(), CURRENT_FESTIVAL_STRING_KEY);
    }

    private ComponentName getFestivalCompName() {
        if (isApplicationInstalled(MYEVENT_WIDGET_PACKAGE_NAME)) {
            return new ComponentName(MYEVENT_WIDGET_PACKAGE_NAME, MYEVENT_WIDGET_CALSS_NAME);
        }
        return null;
    }

    private void sendFestivalWidgetType(int appWidgetId) {
        String festivalDayList = getCurrentFestivalString(this.mLauncher);
        if (festivalDayList != null && !festivalDayList.isEmpty()) {
            Intent intent;
            String[] festivalName = festivalDayList.split(";");
            Log.d(TAG, "festivalName.length : " + festivalName.length + " festivalDayList : " + festivalDayList);
            Log.d(TAG, "festivalName[0]  = " + festivalName[0] + "  fesivalKey : " + getFestivalType(festivalName[0]));
            int widgetType = -1;
            if (isApplicationInstalled(MYEVENT_WIDGET_PACKAGE_NAME)) {
                widgetType = 2;
            }
            if (widgetType == 1) {
                intent = new Intent(ACTION_FESTIVAL_GREETINGWIDGET_ADDED);
            } else if (widgetType == 2) {
                intent = new Intent(ACTION_FESTIVAL_EVENTWIDGET_ADDED);
            } else if (widgetType == 3) {
                intent = new Intent(ACTION_FESTIVAL_CARDWIDGET_ADDED);
            } else {
                return;
            }
            boolean isChanged = isFestivalChanged(this.mLauncher);
            if (intent != null) {
                intent.putExtra("widgetId", appWidgetId);
                intent.putExtra("festivalType", festivalName);
                intent.putExtra("isFestivalChanged", isChanged);
                this.mLauncher.sendBroadcast(intent);
            }
            Log.i(TAG, "sendFestivalWidgetType  [ " + widgetType + " ]  = " + appWidgetId);
            setFestivalString(this.mLauncher);
        }
    }

    private boolean isApplicationInstalled(String packageName) {
        try {
            PackageManager pkgMgr = this.mLauncher.getPackageManager();
            if (pkgMgr == null || pkgMgr.getPackageInfo(packageName, 1) == null) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "festival widget is not installed");
            return false;
        }
    }

    private boolean isFestivalChanged(Launcher launcher) {
        boolean changed = false;
        boolean themeEnable = false;
        boolean isFestivalEnabled = isFestivalSettingsEnabled(launcher) || isMyEventSettingsEnabled(launcher);
        String festivalString = getCurrentFestivalString(launcher);
        String prevFestivalString = getFestivalString(launcher);
        Log.d(TAG, "isFestivalChanged prevFestivalString : " + prevFestivalString + " , festivalString : " + festivalString);
        if (prevFestivalString != null) {
            if (festivalString == null) {
                changed = true;
            } else if (isFestivalEnabled) {
                themeEnable = true;
                if (!prevFestivalString.equals(festivalString)) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        } else if (isFestivalEnabled && festivalString != null) {
            themeEnable = true;
            changed = true;
        }
        if (!themeEnable) {
            setFestivalString(launcher, null);
        }
        Log.d(TAG, "isFestivalChanged : " + changed + " themeEnable : " + themeEnable);
        return changed;
    }

    private boolean isFestivalSettingsEnabled(Launcher launcher) {
        return System.getInt(launcher.getContentResolver(), FESTIVAL_EFFECT_ENABLED, 0) != 0;
    }

    private boolean isMyEventSettingsEnabled(Launcher launcher) {
        return System.getInt(launcher.getContentResolver(), MYEVENT_ENABLED, 0) != 0;
    }
}
