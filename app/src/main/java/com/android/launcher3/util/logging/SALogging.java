package com.android.launcher3.util.logging;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.executor.StateManager;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.home.LauncherPairAppsInfo;
import com.android.launcher3.home.SessionCommitReceiver;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.alarm.Alarm;
//import com.samsung.context.sdk.samsunganalytics.Configuration;
//import com.samsung.context.sdk.samsunganalytics.LogBuilders.EventBuilder;
//import com.samsung.context.sdk.samsunganalytics.LogBuilders.SettingPrefBuilder;
//import com.samsung.context.sdk.samsunganalytics.SamsungAnalytics;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class SALogging extends Logging {
    static final String APPS_BUTTON_DETAIL = "AppsButton";
    private static final char EASY_MODE_ID = '5';
    static final String EMPTY_DETAIL = "Empty";
    static final String FOLDER_DETAIL = "Folder";
    private static final char HOME_APPS_MODE_ID = '0';
    private static final char HOME_ONLY_MODE_ID = '3';
    private static final int MODE_INDEX = 1;
    private static final int MULTI_SELECT_CANCEL_DELAY = 1000;
    private static final int QUICK_OPTION_ADD_TO_SECURE_FOLDER = 7;
    private static final int QUICK_OPTION_APP_INFO = 11;
    private static final int QUICK_OPTION_CLEAR_BADGE_APP = 5;
    private static final int QUICK_OPTION_CLEAR_BADGE_FOLDER = 6;
    public static final int QUICK_OPTION_ClOSE = 15;
    private static final int QUICK_OPTION_DISABLE = 10;
    private static final int QUICK_OPTION_PUT_TO_SLEEP = 8;
    private static final int QUICK_OPTION_REMOVE_FOLDER = 4;
    private static final int QUICK_OPTION_REMOVE_SHORTCUT = 2;
    private static final int QUICK_OPTION_REMOVE_WIDGET = 3;
    private static final int QUICK_OPTION_SELECT_APP = 1;
    private static final int QUICK_OPTION_SELECT_FOLDER = 16;
    private static final int QUICK_OPTION_SELECT_WIDGET = 17;
    private static final int QUICK_OPTION_UNINSTALL = 9;
    private static final int QUICK_OPTION_WIDGET_INFO = 12;
    private static final String STATUS_APPS = "status_apps";
    private static final String STATUS_HOME = "status_home";
    private static final String TAG = "Launcher.SALogging";
    private static final String TRACKING_ID = "401-399-1029810";
    private static final String VERSION = "9.1";
    private static Alarm mMultiSelectCancelAlarm;
    private static boolean sIsFinishAppBinding = false;
    private static boolean sIsFinishHomeBinding = false;
    private AppShortcutPinningInfo mAppShortcutPinningInfo = null;
    private final char[] mModeList = new char[]{HOME_APPS_MODE_ID, HOME_ONLY_MODE_ID, EASY_MODE_ID};
    private QOAction mQOInfo = null;
    private HashMap<String, String> mStatusIDMap;
    private final Runnable mUpdateStatusLogValuesForAppItem = new Runnable() {
        public void run() {
            Log.d(SALogging.TAG, "mUpdateStatusLogValuesForAppItem - run");
            Resources res = Logging.sContext.getResources();
            SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFolderCount), SALogging.this.getItemCountByContainer(Favorites.CONTAINER_APPS, true));
            SALogging.this.insertStatusLog(res.getString(R.string.status_AppsPageCount), SALogging.this.getAppsPageCount());
            SALogging.this.insertStatusLog(res.getString(R.string.status_AppsAppIConCount), SALogging.this.getItemCountByContainer(Favorites.CONTAINER_APPS, false));
            SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFoldersWithoutDefaultColor_Count), SALogUtils.countFolderColorNotDefault(Logging.sContext, false));
            SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFolderAppsCountAndColor), SALogging.this.getFolderAppsCountAndColorDetail(2));
        }
    };
    private final Runnable mUpdateStatusLogValuesForHomeItem = new Runnable() {
        public void run() {
//            Log.d(SALogging.TAG, "mUpdateStatusLogValuesForHomeItem - run");
//            Resources res = Logging.sContext.getResources();
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFolderCount), SALogUtils.getFolderCountInHome(Logging.sContext));
//            ArrayList<String> hotSeatAppList = SALogUtils.getHotseatAppItems(Logging.sContext);
//            for (int i = 0; i < hotSeatAppList.size(); i++) {
//                String detail = (String) hotSeatAppList.get(i);
//                SALogging.this.insertStatusLog(SALogging.this.getHotSeatAppStatusID(i), detail);
//            }
//            Items appShortcuts = SALogUtils.getHomeApps(Logging.sContext, false);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeAppShortcuts), appShortcuts.itemNames);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeAppIConCount), appShortcuts.itemcount);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_PairAppsInHome), SALogUtils.getHomePairApps(Logging.sContext).itemcount);
//            Items widgets = SALogUtils.getHomeWidgetList(Logging.sContext, false);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeWidgets), widgets.itemNames);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeWidgetCount), widgets.itemcount);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultPage), Utilities.getHomeDefaultPageKey(Logging.sContext));
//            SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultAppShortcuts), SALogUtils.getHomeApps(Logging.sContext, true).itemNames);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultWidgets), SALogUtils.getHomeWidgetList(Logging.sContext, true).itemNames);
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomePageCount), SALogging.this.getHomePageCount());
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeEmptyPageCount), SALogging.this.getHomeEmptyPageCount());
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeEmptyPageCount), SALogging.this.getHomeEmptyPageCount());
//            SALogging.this.insertStatusLog(res.getString(R.string.status_Home1X1IconCount), SALogUtils.getShortcutOnHomeCount(Logging.sContext));
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFoldersWithoutDefaultColor_Count), SALogUtils.countFolderColorNotDefault(Logging.sContext, true));
//            GSW data = SALogUtils.getGSWData(Logging.sContext);
//            if (data != null) {
//                SALogging.this.insertStatusLog(res.getString(R.string.status_Size_GSW), data.size);
//                SALogging.this.insertStatusLog(res.getString(R.string.status_PageOfGSW), data.page);
//                SALogging.this.insertStatusLog(res.getString(R.string.status_LocationOfGSW), data.location);
//            }
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFolderAppsCountAndColor), SALogging.this.getFolderAppsCountAndColorDetail(1));
//            SALogging.this.insertStatusLog(res.getString(R.string.status_HomePinnedShortcutsCount), SALogUtils.getPinnedShortcutsCount(Logging.sContext));
        }
    };

    private static class AppShortcutPinningInfo {
        final String mAppShortcutID;
        final String mPackageName;

        AppShortcutPinningInfo(String appShortcutID, String packageName) {
            this.mAppShortcutID = appShortcutID;
            this.mPackageName = packageName;
        }
    }

    private static class QOAction {
        final int mItemType;

        QOAction(int itemType) {
            this.mItemType = itemType;
        }
    }

    private static class SingletonHolder {
        private static final SALogging sSALoggingInstance = new SALogging();

        private SingletonHolder() {
        }
    }

    public static SALogging getInstance() {
        return SingletonHolder.sSALoggingInstance;
    }

    public void init(final Application application) {
        sContext = application;
        runOnLoggingThread(new Runnable() {
            public void run() {
                Log.d(SALogging.TAG, "init SALog");
//                try {
//                    SamsungAnalytics.setConfiguration(application, new Configuration().setTrackingId(SALogging.TRACKING_ID).setVersion(SALogging.VERSION).enableAutoDeviceId());
//                    SamsungAnalytics.getInstance().enableUncaughtExceptionLogging();
//                    SALogging.this.deleteLegacyFileIfExist();
//                    SALogging.this.registerStatusPref();
//                } catch (Exception e) {
//                    Log.w(SALogging.TAG, "init : Exception " + e.toString());
//                }
            }
        });
        mMultiSelectCancelAlarm = new Alarm();
    }

    private void deleteLegacyFileIfExist() {
        File legacyFile = new File(sContext.getApplicationInfo().dataDir + "/shared_prefs/" + "SASettingPref" + ".xml");
        if (legacyFile.exists()) {
            Log.d(TAG, "deleteLegacyFileIfExist : success = " + legacyFile.delete());
        }
    }

    public void startLoader() {
        sIsFinishHomeBinding = false;
        sIsFinishAppBinding = false;
    }

    private void registerStatusPref() {
        Resources res = sContext.getResources();
        HashSet<String> keySet = new HashSet();
        this.mStatusIDMap = new HashMap();
        this.mStatusIDMap.put(res.getString(R.string.status_HomeFolderCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_FirstHotseatApp), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_SecondHotseatApp), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_ThirdHotseatApp), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_FourthHotseatApp), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_FifthHotseatApp), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeAppShortcuts), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeWidgets), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_DefaultPage), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_DefaultAppShortcuts), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_DefaultWidgets), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomePageCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeAppIConCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeWidgetCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_Home1X1IconCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeEmptyPageCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeFoldersWithoutDefaultColor_Count), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsFolderCount), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsPageCount), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsAppIConCount), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsFoldersWithoutDefaultColor_Count), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeFolderAppsCountAndColor), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsFolderAppsCountAndColor), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_CountOfEnterGSW), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_Size_GSW), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_PageOfGSW), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_LocationOfGSW), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsSortStatus), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_zeropagesetting), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeScreenLayout), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomeScreenGrid), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsScreenGrid), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsButton), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HideAppsCount), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HideAppsName), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_OpenQuickPanel), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AddAppsToHome), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_PortraitModeOnly), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsInHomeFolder), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_AppsInAppsFolder), STATUS_APPS);
        this.mStatusIDMap.put(res.getString(R.string.status_PairAppsInHome), STATUS_HOME);
        this.mStatusIDMap.put(res.getString(R.string.status_HomePinnedShortcutsCount), STATUS_HOME);
        for (String key : this.mStatusIDMap.keySet()) {
            for (char mode : this.mModeList) {
                keySet.add(changeIdByMode(key, mode));
            }
        }
        //SamsungAnalytics.getInstance().registerSettingPref(new SettingPrefBuilder().addKeys(LauncherFiles.SAMSUNG_ANALYTICS_PREFERENCES_KEY, keySet).build());
    }

    private boolean isBixbyRunning() {
        StateManager stateManager = LauncherAppState.getInstance().getStateManager();
        return stateManager != null && stateManager.isRuleRunning();
    }

    private String changeIdByMode(String originID, char mode) {
        char[] changeID = originID.toCharArray();
        if (1 < changeID.length && mode != HOME_APPS_MODE_ID) {
            changeID[1] = mode;
        }
        return String.valueOf(changeID);
    }

    private String changeIdByMode(String originID) {
        char[] changeID = originID.toCharArray();
        if (1 < changeID.length) {
            char c = LauncherAppState.getInstance().isEasyModeEnabled() ? EASY_MODE_ID : LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? HOME_ONLY_MODE_ID : changeID[1];
            changeID[1] = c;
        }
        return String.valueOf(changeID);
    }

    private String changeEmptyDetail(String detail) {
        if (detail.isEmpty() || detail.equals(" ")) {
            return EMPTY_DETAIL;
        }
        return detail;
    }

    public void insertItemLaunchLog(final ItemInfo info, final Launcher launcher) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                String title = "";
                String screenID = null;
                String eventID = null;
                Resources res = launcher.getResources();
                int stageMode = launcher.getTopStageMode();
                if (stageMode == 1) {
                    screenID = res.getString(R.string.screen_Home_1xxx);
                    if (info instanceof IconInfo) {
                        title = SALogging.this.getPackageAndTitleString(info);
                        eventID = (DualAppUtils.supportDualApp(Logging.sContext) && DualAppUtils.isDualAppId(info.user)) ? res.getString(R.string.event_LaunchSecondApp) : res.getString(R.string.event_LaunchApp);
                    } else if (info instanceof LauncherAppWidgetInfo) {
                        if (((LauncherAppWidgetInfo) info).providerName != null) {
                            title = ((LauncherAppWidgetInfo) info).providerName.getPackageName();
                        }
                        eventID = res.getString(R.string.event_LaunchWidget);
                    }
                } else if (stageMode == 2) {
                    screenID = res.getString(R.string.screen_Apps_2xxx);
                    if (info instanceof IconInfo) {
                        title = SALogging.this.getPackageAndTitleString(info);
                        eventID = (DualAppUtils.supportDualApp(Logging.sContext) && DualAppUtils.isDualAppId(info.user)) ? res.getString(R.string.event_LaunchSecondApp) : res.getString(R.string.event_LaunchApp);
                    }
                } else if (stageMode == 5) {
                    title = SALogging.this.getPackageAndTitleString(info);
                    int secondTopStage = launcher.getSecondTopStageMode();
                    if (secondTopStage == 1) {
                        screenID = res.getString(R.string.screen_HomeFolder_Primary);
                    } else if (secondTopStage == 2) {
                        screenID = res.getString(R.string.screen_AppsFolder_Primary);
                    }
                    eventID = (DualAppUtils.supportDualApp(Logging.sContext) && DualAppUtils.isDualAppId(info.user)) ? res.getString(R.string.event_LaunchSecondApp) : res.getString(R.string.event_FolderLaunchApp);
                }
                SALogging.this.insertEventLog(screenID, eventID, title);
            }
        });
    }

    public void insertAddWidgetItemLog(final ItemInfo info) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (info != null && Logging.sContext != null) {
                    SALogging.this.insertEventLog(Logging.sContext.getResources().getString(R.string.screen_Widgets), Logging.sContext.getResources().getString(R.string.event_AddWidgetToHomeScreen));
                }
            }
        });
    }

    public void insertCloseWidgetLog(final String index) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (Logging.sContext != null) {
                    SALogging.this.insertEventLog(Logging.sContext.getResources().getString(R.string.screen_Widgets), Logging.sContext.getResources().getString(R.string.event_CloseWidgetScreen), index);
                }
            }
        });
    }

    public void insertCloseWidgetFolderLog(final String index) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (Logging.sContext != null) {
                    SALogging.this.insertEventLog(Logging.sContext.getResources().getString(R.string.screen_WidgetTray), Logging.sContext.getResources().getString(R.string.event_CloseWidgetFolder), index);
                }
            }
        });
    }

    public void insertLockedItemLaunchLog(final ItemInfo info, final Launcher launcher) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (info != null && launcher != null) {
                    String title = "";
                    String screenID = null;
                    String eventID = null;
                    Resources res = launcher.getResources();
                    int stageMode = launcher.getTopStageMode();
                    if (stageMode == 1) {
                        screenID = res.getString(R.string.screen_Home_1xxx);
                    } else if (stageMode == 2) {
                        screenID = res.getString(R.string.screen_Apps_2xxx);
                    } else if (stageMode == 5) {
                        int secondTopStage = launcher.getSecondTopStageMode();
                        if (secondTopStage == 1) {
                            screenID = res.getString(R.string.screen_HomeFolder_Primary);
                        } else if (secondTopStage == 2) {
                            screenID = res.getString(R.string.screen_AppsFolder_Primary);
                        }
                    }
                    if (info instanceof FolderInfo) {
                        title = SALogging.FOLDER_DETAIL;
                        eventID = res.getString(R.string.event_AppLockHome_OpenLockedFolder);
                    } else if (info instanceof IconInfo) {
                        title = "APP";
                        eventID = res.getString(R.string.event_AppLockHome_LaunchLockedApp);
                    }
                    if (!(info.title == null || info.title.toString().isEmpty())) {
                        title = info.title.toString();
                    }
                    SALogging.this.insertEventLog(screenID, eventID, title);
                }
            }
        });
    }

    public void insertQuickViewEventLog(final ItemInfo itemInfo, final Launcher launcher) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                int stageMode = launcher.getTopStageMode();
                String screenID = null;
                String eventID = null;
                int itemType = 0;
                String title = "";
                if (stageMode == 1) {
                    screenID = res.getString(R.string.screen_Home_1xxx);
                    if (itemInfo instanceof IconInfo) {
                        eventID = res.getString(R.string.event_QuickOptionsOfApp);
                        itemType = 1;
                    } else if (itemInfo instanceof FolderInfo) {
                        eventID = res.getString(R.string.event_QuickOptionsOfFolder);
                        itemType = 16;
                    } else if (itemInfo instanceof LauncherAppWidgetInfo) {
                        eventID = res.getString(R.string.event_QuickOptionsOfWidgetApp);
                        itemType = 17;
                    }
                } else if (stageMode == 2) {
                    screenID = res.getString(R.string.screen_Apps_2xxx);
                    if (itemInfo instanceof IconInfo) {
                        eventID = res.getString(R.string.event_QuickOptionsOfApp);
                        itemType = 1;
                    } else if (itemInfo instanceof FolderInfo) {
                        eventID = res.getString(R.string.event_QuickOptionsOfFolder);
                        itemType = 16;
                    }
                } else if (stageMode == 5) {
                    int secondTopStage = launcher.getSecondTopStageMode();
                    itemType = 1;
                    if (secondTopStage == 1) {
                        screenID = res.getString(R.string.screen_HomeFolder_Primary);
                    } else if (secondTopStage == 2) {
                        screenID = res.getString(R.string.screen_AppsFolder_Primary);
                    }
                    eventID = res.getString(R.string.event_FolderQuickOption);
                }
                SALogging.this.mQOInfo = new QOAction(itemType);
                SALogging.this.insertEventLog(screenID, eventID, title);
            }
        });
    }

    public void insertMoveToAppLog(final Launcher launcher, final boolean isMultiSelect) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                int stageMode = launcher.getTopStageMode();
                if (stageMode == 1) {
                    if (isMultiSelect) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_SM_MoveToApp));
                    } else {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_CreateFolder));
                    }
                } else if (stageMode != 2) {
                } else {
                    if (isMultiSelect) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_SM_MoveToApp));
                    } else {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_Selected), res.getString(R.string.event_Apps_CreateFolder));
                    }
                }
            }
        });
    }

    public void insertAddToFolderLog(final Launcher launcher, final boolean isMultiSelect) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                int stageMode = launcher.getTopStageMode();
                if (stageMode == 1) {
                    if (isMultiSelect) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_SM_MoveToFolder));
                    } else {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_AddToFolder));
                    }
                } else if (stageMode != 2) {
                } else {
                    if (isMultiSelect) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_SM_MoveToFolder));
                    } else {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_Selected), res.getString(R.string.event_Apps_AddToFolder));
                    }
                }
            }
        });
    }

    public void insertAddToLockedFolderLog(final Launcher launcher, final boolean isMultiSelect) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (launcher != null) {
                    Resources res = launcher.getResources();
                    int stageMode = launcher.getTopStageMode();
                    if (stageMode == 1) {
                        if (isMultiSelect) {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_AppLockHome_AddToLockFolder));
                        } else {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_AppLockHome_AddToLockFolder));
                        }
                    } else if (stageMode != 2) {
                    } else {
                        if (isMultiSelect) {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_AppLockHome_AddToLockFolder));
                        } else {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_Selected), res.getString(R.string.event_AppLockHome_AddToLockFolder));
                        }
                    }
                }
            }
        });
    }

    public void insertMoveToPageLog(final Object itemInfo, final boolean isHome) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                if (isHome) {
                    if (itemInfo instanceof IconInfo) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_MoveApp));
                    } else if (itemInfo instanceof FolderInfo) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_MoveFolder));
                    } else if (itemInfo instanceof LauncherAppWidgetInfo) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_MoveWidget));
                    }
                } else if (itemInfo instanceof IconInfo) {
                    SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_Selected), res.getString(R.string.event_Apps_MoveApps));
                } else if (itemInfo instanceof FolderInfo) {
                    SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_Selected), res.getString(R.string.event_Apps_MoveFolder));
                }
            }
        });
    }

    public void setDefaultValueForAppStatusLog(final Context context) {
        sIsFinishAppBinding = true;
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = context.getResources();
                char currentMode = LauncherAppState.getInstance().isEasyModeEnabled() ? SALogging.EASY_MODE_ID : LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? SALogging.HOME_ONLY_MODE_ID : SALogging.HOME_APPS_MODE_ID;
                for (char mode : SALogging.this.mModeList) {
                    if (currentMode == mode) {
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFolderCount), SALogging.this.getItemCountByContainer(Favorites.CONTAINER_APPS, true));
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsPageCount), SALogging.this.getAppsPageCount());
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsAppIConCount), String.valueOf(SALogging.this.getItemCountByContainer(Favorites.CONTAINER_APPS, false)));
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFoldersWithoutDefaultColor_Count), SALogUtils.countFolderColorNotDefault(Logging.sContext, false));
                        String viewType = Logging.sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getString("AppsController.ViewType", null);
                        boolean isCustom = viewType == null || viewType.equals("CUSTOM_GRID");
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsSortStatus), isCustom ? 1 : 0);
                        SALogging.this.insertStatusLog(res.getString(R.string.status_AppsFolderAppsCountAndColor), SALogging.this.getFolderAppsCountAndColorDetail(2));
                        int[] gridXY = new int[2];
                        ScreenGridUtilities.loadCurrentAppsGridSize(Logging.sContext, gridXY);
                        SALogging.this.insertChangeGridLog(gridXY[0], gridXY[1], true, false);
                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_AppsInAppsFolder), SALogUtils.getPackageListInFolder(Logging.sContext, Favorites.CONTAINER_APPS));
                    } else {
                        Editor prefEditor = context.getSharedPreferences(LauncherFiles.SAMSUNG_ANALYTICS_PREFERENCES_KEY, 0).edit();
                        for (String statusID : SALogging.this.mStatusIDMap.keySet()) {
                            if (((String) SALogging.this.mStatusIDMap.get(statusID)).equals(SALogging.STATUS_APPS)) {
                                prefEditor.remove(SALogging.this.changeIdByMode(statusID, mode));
                            }
                        }
                        prefEditor.apply();
                    }
                }
            }
        });
    }

    public void setDefaultValueForHomeStatusLog(final Context context) {
        sIsFinishHomeBinding = true;
        runOnLoggingThread(new Runnable() {
            public void run() {
//                Resources res = context.getResources();
//                char currentMode = LauncherAppState.getInstance().isEasyModeEnabled() ? SALogging.EASY_MODE_ID : LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? SALogging.HOME_ONLY_MODE_ID : SALogging.HOME_APPS_MODE_ID;
//                for (char mode : SALogging.this.mModeList) {
//                    if (currentMode == mode) {
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFolderCount), SALogUtils.getFolderCountInHome(Logging.sContext));
//                        ArrayList<String> hotSeatAppList = SALogUtils.getHotseatAppItems(context);
//                        for (int i = 0; i < hotSeatAppList.size(); i++) {
//                            SALogging.this.insertStatusLog(SALogging.this.getHotSeatAppStatusID(i), (String) hotSeatAppList.get(i));
//                        }
//                        Items appShortcuts = SALogUtils.getHomeApps(Logging.sContext, false);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeAppShortcuts), appShortcuts.itemNames);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeAppIConCount), appShortcuts.itemcount);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_PairAppsInHome), SALogUtils.getHomePairApps(Logging.sContext).itemcount);
//                        Items widgets = SALogUtils.getHomeWidgetList(Logging.sContext, false);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeWidgets), widgets.itemNames);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeWidgetCount), widgets.itemcount);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultPage), Utilities.getHomeDefaultPageKey(Logging.sContext));
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultAppShortcuts), SALogUtils.getHomeApps(Logging.sContext, true).itemNames);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultWidgets), SALogUtils.getHomeWidgetList(Logging.sContext, true).itemNames);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomePageCount), SALogging.this.getHomePageCount());
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeEmptyPageCount), SALogging.this.getHomeEmptyPageCount());
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_Home1X1IconCount), SALogUtils.getShortcutOnHomeCount(Logging.sContext));
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFoldersWithoutDefaultColor_Count), SALogUtils.countFolderColorNotDefault(Logging.sContext, true));
//                        GSW.insertEnterSearchCount(Logging.sContext, false);
//                        GSW data = SALogUtils.getGSWData(Logging.sContext);
//                        if (data != null) {
//                            SALogging.this.insertStatusLog(res.getString(R.string.status_Size_GSW), data.size);
//                            SALogging.this.insertStatusLog(res.getString(R.string.status_PageOfGSW), data.page);
//                            SALogging.this.insertStatusLog(res.getString(R.string.status_LocationOfGSW), data.location);
//                        }
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HomeFolderAppsCountAndColor), SALogging.this.getFolderAppsCountAndColorDetail(1));
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_zeropagesetting), ZeroPageController.getZeroPageActiveState(context, true) ? "1" : "0");
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_HomeScreenLayout), LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? "2" : "1");
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_AppsButton), LauncherAppState.getInstance().getAppsButtonEnabled() ? 1 : 0);
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_OpenQuickPanel), LauncherAppState.getInstance().getNotificationPanelExpansionEnabled() ? 1 : 0);
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_AddAppsToHome), SessionCommitReceiver.isEnabled(context) ? 1 : 0);
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_PortraitModeOnly), Utilities.isOnlyPortraitMode() ? 1 : 0);
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_AppsInHomeFolder), SALogUtils.getPackageListInFolder(Logging.sContext, -100));
//                        SALogging.this.insertStatusLog(context.getResources().getString(R.string.status_HomePinnedShortcutsCount), SALogUtils.getPinnedShortcutsCount(Logging.sContext));
//                        int[] gridXY = new int[2];
//                        Utilities.loadCurrentGridSize(Logging.sContext, gridXY);
//                        SALogging.this.insertChangeGridLog(gridXY[0], gridXY[1], true, true);
//                        Pair<Integer, String> info = SALogUtils.getHideApps(Logging.sContext);
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HideAppsCount), ((Integer) info.first).intValue());
//                        SALogging.this.insertStatusLog(res.getString(R.string.status_HideAppsName), (String) info.second);
//                    } else {
//                        Editor prefEditor = context.getSharedPreferences(LauncherFiles.SAMSUNG_ANALYTICS_PREFERENCES_KEY, 0).edit();
//                        for (String statusID : SALogging.this.mStatusIDMap.keySet()) {
//                            if (currentMode == '3') {
//                                prefEditor.remove(SALogging.this.changeIdByMode(statusID, mode));
//                            } else {
//                                if (((String) SALogging.this.mStatusIDMap.get(statusID)).equals(SALogging.STATUS_HOME)) {
//                                    prefEditor.remove(SALogging.this.changeIdByMode(statusID, mode));
//                                }
//                            }
//                        }
//                        prefEditor.apply();
//                    }
//                }
            }
        });
    }

    public void insertMoveFromFolderLog(long container, boolean isMultiSelect, int direction, DragObject dragObject) {
        final int numberOfPackages = analysePackagesOfDragObject(dragObject, new StringBuilder());
        final long j = container;
        final boolean z = isMultiSelect;
        final int i = direction;
        runOnLoggingThread(new Runnable() {
            public void run() {
                String eventID;
                Resources res = Logging.sContext.getResources();
                String screenID = null;
                if (j == -100 || j == -101) {
                    if (z) {
                        screenID = res.getString(R.string.screen_HomeFolder_SelectMode);
                    } else {
                        screenID = res.getString(R.string.screen_HomeFolder_Selected);
                    }
                } else if (j == -102) {
                    if (z) {
                        screenID = res.getString(R.string.screen_AppsFolder_SelectMode);
                    } else {
                        screenID = res.getString(R.string.screen_AppsFolder_Selected);
                    }
                }
                if (i > 0) {
                    eventID = res.getString(R.string.event_Folder_Selected_MoveFromFolderToBottom);
                } else {
                    eventID = res.getString(R.string.event_Folder_Selected_MoveFromFolderToTop);
                }
                SALogging.this.insertEventLog(screenID, eventID, (long) numberOfPackages);
            }
        });
    }

    public void insertFolderMoveAppLogs(final Launcher launcher, final boolean isMultiSelect) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                String eventId;
                Resources res = Logging.sContext.getResources();
                String screenId = null;
                int secondTopStage = launcher.getSecondTopStageMode();
                if (isMultiSelect) {
                    if (secondTopStage == 1) {
                        screenId = res.getString(R.string.screen_HomeFolder_SelectMode);
                    } else if (secondTopStage == 2) {
                        screenId = res.getString(R.string.screen_AppsFolder_SelectMode);
                    }
                    eventId = res.getString(R.string.event_Folder_SM_MoveItem);
                } else {
                    if (secondTopStage == 1) {
                        screenId = res.getString(R.string.screen_HomeFolder_Selected);
                    } else if (secondTopStage == 2) {
                        screenId = res.getString(R.string.screen_AppsFolder_Selected);
                    }
                    eventId = res.getString(R.string.event_Folder_Selected_MoveApp);
                }
                if (screenId != null) {
                    SALogging.this.insertEventLog(screenId, eventId);
                }
            }
        });
    }

    private int analysePackagesOfDragObject(DragObject dragObject, StringBuilder packageNameBuilder) {
        int packagesCount = 0;
        if (dragObject == null || !(dragObject.dragInfo instanceof ItemInfo) || packageNameBuilder == null) {
            Log.e(TAG, "analysePackagesOfDragObject : invalid parameter - " + dragObject);
        } else {
            //packageNameBuilder.append(getPackageAndTitleString(dragObject.dragInfo));
            packagesCount = 1;
            if (dragObject.extraDragInfoList != null) {
                for (DragObject anExtraDragInfoList : dragObject.extraDragInfoList) {
                    ItemInfo extraItem = (ItemInfo)(anExtraDragInfoList).dragInfo;
                    packageNameBuilder.append(",");
                    packageNameBuilder.append(getPackageAndTitleString(extraItem));
                    packagesCount++;
                }
            }
        }
        return packagesCount;
    }

    private String getPackageAndTitleString(ItemInfo item) {
        if (item == null) {
            return "APP";
        }
        if (item.componentName != null) {
            return item.componentName.getPackageName();
        }
        if (!(item instanceof IconInfo) || item.getIntent() == null || item.getIntent().getComponent() == null) {
            return EnvironmentCompat.MEDIA_UNKNOWN;
        }
        return item.getIntent().getComponent().getPackageName();
    }

    private String getFolderAppsCountAndColorDetail(int mode) {
        String detail = null;
        Cursor cursor = SALogUtils.getFolderItems(sContext, mode);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int appsCount = SALogUtils.getAppsCountInFolder(sContext, cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                int color = cursor.getInt(cursor.getColumnIndex(BaseLauncherColumns.COLOR));
                color = color < 0 ? 1 : color + 1;
                detail = (detail != null ? detail + "," : "") + String.format(Locale.ENGLISH, "a%dc%d", new Object[]{Integer.valueOf(appsCount), Integer.valueOf(color)});
            }
            cursor.close();
        }
        return detail == null ? EMPTY_DETAIL : detail;
    }

    private String getHotSeatAppStatusID(int i) {
        Resources res = sContext.getResources();
        String statusID = res.getString(R.string.status_FirstHotseatApp);
        switch (i) {
            case 0:
                return res.getString(R.string.status_FirstHotseatApp);
            case 1:
                return res.getString(R.string.status_SecondHotseatApp);
            case 2:
                return res.getString(R.string.status_ThirdHotseatApp);
            case 3:
                return res.getString(R.string.status_FourthHotseatApp);
            case 4:
                return res.getString(R.string.status_FifthHotseatApp);
            default:
                return statusID;
        }
    }

    private String getBixbyEventID(int option, Launcher activity) {
        Resources res = sContext.getResources();
        int stageMode = activity.getTopStageMode();
        switch (option) {
            case 1:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_SelectApp);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_HomeVoice_SelectApp);
                }
                return null;
            case 2:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_RemoveApp);
                }
                return null;
            case 3:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_RemoveWidget);
                }
                return null;
            case 4:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_RemoveFolder);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_Home_QuickOptionOfFolder);
                }
                return null;
            case 5:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_ClearBadgeApp);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_ClearBadgeApp);
                }
                return null;
            case 6:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_ClearBadgeFolder);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_ClearBadgeFolder);
                }
                return null;
            case 7:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_AddAppToSecureFolder);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_AddAppToSecureFolder);
                }
                return null;
            case 8:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_PutAppToSleep);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_PutAppToSleep);
                }
                return null;
            case 9:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_UninstallApp);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_UninstallApp);
                }
                return null;
            case 10:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_DisableApp);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_DisableApp);
                }
                return null;
            case 11:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_AppInfo);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_AppVoice_AppInfo);
                }
                return null;
            case 12:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_WidgetInfo);
                }
                return null;
            case 15:
                return res.getString(R.string.event_Home_CloseQuickOptions);
            case 16:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_SelectFolder);
                }
                if (stageMode == 2) {
                    return res.getString(R.string.event_HomeVoice_SelectFolder);
                }
                return null;
            case 17:
                if (stageMode == 1) {
                    return res.getString(R.string.event_HomeVoice_SelectWidget);
                }
                return null;
            default:
                return null;
        }
    }

    private String getQuickOptionEventID(int option, Launcher activity) {
        if (activity == null) {
            return null;
        }
        Resources res = activity.getResources();
        switch (option) {
            case 15:
                return res.getString(R.string.event_Home_CloseQuickOptions);
            default:
                return null;
        }
    }

    private String getQuickOptionScreenID(Launcher activity) {
        if (activity == null) {
            return null;
        }
        Resources res = activity.getResources();
        int stageMode = activity.getTopStageMode();
        int secondTopStageMode = activity.getSecondTopStageMode();
        if (stageMode == 1) {
            return res.getString(R.string.screen_Home_QuickOptions);
        }
        if (stageMode == 2) {
            return res.getString(R.string.screen_Apps_QuickOptions);
        }
        if (stageMode != 5) {
            return null;
        }
        if (secondTopStageMode == 1) {
            return res.getString(R.string.screen_HomeFolder_QuickOptions);
        }
        if (secondTopStageMode == 2) {
            return res.getString(R.string.screen_AppsFolder_QuickOptions);
        }
        return null;
    }

    private String getBixbyScreenID(Launcher activity) {
        Resources res = sContext.getResources();
        int stageMode = activity.getTopStageMode();
        if (stageMode == 1) {
            return res.getString(R.string.screen_Home_1xxx);
        }
        if (stageMode == 2) {
            return res.getString(R.string.screen_Apps_2xxx);
        }
        if (stageMode != 5) {
            return null;
        }
        int secondTopStage = activity.getSecondTopStageMode();
        if (secondTopStage == 1) {
            return activity.getResources().getString(R.string.screen_HomeFolder_Primary);
        }
        if (secondTopStage == 2) {
            return activity.getResources().getString(R.string.screen_AppsFolder_Primary);
        }
        return null;
    }

    public void insertQuickOptionEventLog(final int option, final Launcher activity, final String detail) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                String screenID;
                String eventID;
                if (SALogging.this.isBixbyRunning()) {
                    screenID = SALogging.this.getBixbyScreenID(activity);
                    eventID = SALogging.this.getBixbyEventID(option, activity);
                } else {
                    screenID = SALogging.this.getQuickOptionScreenID(activity);
                    eventID = SALogging.this.getQuickOptionEventID(option, activity);
                }
                if (screenID != null && eventID != null) {
                    if (detail != null) {
                        SALogging.this.insertEventLog(screenID, eventID, detail);
                    } else {
                        SALogging.this.insertEventLog(screenID, eventID);
                    }
                }
            }
        });
    }

    public void updateDefaultPageLog() {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultPage), Utilities.getHomeDefaultPageKey(Logging.sContext));
                SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultAppShortcuts), SALogUtils.getHomeApps(Logging.sContext, true).itemNames);
                SALogging.this.insertStatusLog(res.getString(R.string.status_DefaultWidgets), SALogUtils.getHomeWidgetList(Logging.sContext, true).itemNames);
            }
        });
    }

    public void updatePageLogs() {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                SALogging.this.insertStatusLog(res.getString(R.string.status_HomePageCount), SALogging.this.getHomePageCount());
                SALogging.this.insertStatusLog(res.getString(R.string.status_HomeEmptyPageCount), SALogging.this.getHomeEmptyPageCount());
            }
        });
    }

    public void insertMultiSelectCancelLog(final Launcher launcher, final boolean backPressed, final boolean homePressed) {
        if (mMultiSelectCancelAlarm == null || !mMultiSelectCancelAlarm.alarmPending()) {
            if (mMultiSelectCancelAlarm != null) {
                mMultiSelectCancelAlarm.setAlarm(1000);
            }
            runOnLoggingThread(new Runnable() {
                public void run() {
                    Resources res = launcher.getResources();
                    int stageMode = launcher.getTopStageMode();
                    String detail = backPressed ? "1" : homePressed ? "2" : "3";
                    if (stageMode == 1) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_SM_Cancel), detail);
                    } else if (stageMode == 2) {
                        SALogging.this.insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_SM_Cancel), detail);
                    } else if (stageMode == 5) {
                        int secondStageMode = launcher.getSecondTopStageMode();
                        if (secondStageMode == 2) {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_AppsFolder_SelectMode), res.getString(R.string.event_Folder_SM_Cancel), detail);
                        } else if (secondStageMode == 1) {
                            SALogging.this.insertEventLog(res.getString(R.string.screen_HomeFolder_SelectMode), res.getString(R.string.event_Folder_SM_Cancel), detail);
                        }
                    }
                }
            });
        }
    }

    public void insertMultiSelectLog(int id, ArrayList<View> mCheckedAppsViewList, Launcher launcher, String btnText) {
        final Launcher launcher2 = launcher;
        final int i = id;
        final String str = btnText;
        final ArrayList<View> arrayList = mCheckedAppsViewList;
        runOnLoggingThread(new Runnable() {
            public void run() {
                String screenID = null;
                if (launcher2.getTopStageMode() == 1) {
                    screenID = launcher2.getResources().getString(R.string.screen_Home_SelectMode);
                } else if (launcher2.getTopStageMode() == 2) {
                    screenID = launcher2.getResources().getString(R.string.screen_Apps_SelectMode);
                } else if (launcher2.getTopStageMode() == 5) {
                    if (launcher2.getSecondTopStageMode() == 2) {
                        screenID = launcher2.getResources().getString(R.string.screen_AppsFolder_SelectMode);
                    } else {
                        screenID = launcher2.getResources().getString(R.string.screen_HomeFolder_SelectMode);
                    }
                }
                SALogging.this.insertEventLog(screenID, SALogging.this.getEventIDForMultiSelect(i, launcher2, str), (long) arrayList.size());
            }
        });
    }

    private String getEventIDForMultiSelect(int id, Launcher launcher, String btnText) {
        int stage = launcher.getTopStageMode();
        Resources res = sContext.getResources();
        switch (id) {
            case 0:
                boolean isDisable = btnText != null && btnText.equals(launcher.getString(R.string.multi_select_disable));
                String eventID;
                if (stage == 1) {
                    if (isDisable) {
                        eventID = res.getString(R.string.event_SM_Disable);
                    } else {
                        eventID = res.getString(R.string.event_SM_Uninstall);
                    }
                    return eventID;
                } else if (stage == 2) {
                    if (isDisable) {
                        eventID = res.getString(R.string.event_SM_Disable);
                    } else {
                        eventID = res.getString(R.string.event_SM_Uninstall);
                    }
                    return eventID;
                } else if (stage != 5) {
                    return null;
                } else {
                    if (isDisable) {
                        eventID = res.getString(R.string.event_Folder_SM_Disable);
                    } else {
                        eventID = res.getString(R.string.event_Folder_SM_Uninstall);
                    }
                    return eventID;
                }
            case 1:
                if (stage == 1) {
                    return res.getString(R.string.event_SM_Remove);
                }
                if (stage == 5) {
                    return res.getString(R.string.event_Folder_SM_Remove);
                }
                return null;
            case 2:
                if (stage == 1) {
                    return res.getString(R.string.event_SM_CreateFolder);
                }
                if (stage == 2) {
                    return res.getString(R.string.event_SM_CreateFolder);
                }
                if (stage == 5) {
                    return res.getString(R.string.event_Folder_SM_CreateFolder);
                }
                return null;
            default:
                return null;
        }
    }

    public void insertHideAppsLog(ArrayList<IconInfo> selectedList) {
        String appNames = "";
        Iterator it = selectedList.iterator();
        while (it.hasNext()) {
            IconInfo info = (IconInfo) it.next();
            if (!(info.title == null || info.componentName == null)) {
                appNames = appNames + info.componentName.getPackageName() + ", ";
            }
        }
        insertStatusLog(sContext.getResources().getString(R.string.status_HideAppsName), appNames);
        insertStatusLog(sContext.getResources().getString(R.string.status_HideAppsCount), selectedList.size());
    }

    public void insertChangeGridLog(int x, int y, boolean statusLogOnly, boolean isHomeGrid) {
        final boolean z = isHomeGrid;
        final int i = x;
        final int i2 = y;
        final boolean z2 = statusLogOnly;
        runOnLoggingThread(new Runnable() {
            public void run() {
                String screenID;
                String eventID;
                String statusID;
                Resources res = Logging.sContext.getResources();
                if (z) {
                    screenID = res.getString(R.string.screen_HomeScreenGrid);
                } else {
                    screenID = res.getString(R.string.screen_AppsScreenGrid);
                }
                if (z) {
                    eventID = res.getString(R.string.event_SG_Apply);
                } else {
                    eventID = res.getString(R.string.event_SG_Apply_Apps);
                }
                if (z) {
                    statusID = res.getString(R.string.status_HomeScreenGrid);
                } else {
                    statusID = res.getString(R.string.status_AppsScreenGrid);
                }
                String detail = z ? (i == 4 && i2 == 5) ? "1" : (i == 4 && i2 == 6) ? "2" : (i == 5 && i2 == 5) ? "3" : (i == 5 && i2 == 6) ? "4" : (i == 4 && i2 == 4) ? "5" : "" : (i == 4 && i2 == 6) ? "1" : (i == 5 && i2 == 6) ? "2" : "";
                SALogging.this.insertStatusLog(statusID, detail);
                if (!z2) {
                    SALogging.this.insertEventLog(screenID, eventID, detail);
                }
            }
        });
    }

    public void insertClickGridButtonLog(final int x, final int y, final boolean isHomeGrid) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                String screenID;
                Resources res = Logging.sContext.getResources();
                if (isHomeGrid) {
                    screenID = res.getString(R.string.screen_HomeScreenGrid);
                } else {
                    screenID = res.getString(R.string.screen_AppsScreenGrid);
                }
                String eventID = isHomeGrid ? (x == 4 && y == 5) ? res.getString(R.string.event_SG_ChangeTo4X5) : (x == 4 && y == 6) ? res.getString(R.string.event_SG_ChangeTo4X6) : (x == 5 && y == 5) ? res.getString(R.string.event_SG_ChangeTo5X5) : (x == 5 && y == 6) ? res.getString(R.string.event_SG_ChangeTo5X6) : (x == 4 && y == 4) ? res.getString(R.string.event_SG_ChangeTo4X4) : "" : (x == 4 && y == 6) ? res.getString(R.string.event_SG_ChangeTo4X6_Apps) : (x == 5 && y == 6) ? res.getString(R.string.event_SG_ChangeTo5X6_Apps) : "";
                SALogging.this.insertEventLog(screenID, eventID);
            }
        });
    }

    public void updateStatusLogValuesForAppsItem() {
        if (sIsFinishAppBinding) {
            removeCallBacks(this.mUpdateStatusLogValuesForAppItem);
            runOnLoggingThreadDelayed(this.mUpdateStatusLogValuesForAppItem, 500);
        }
    }

    public void updateLogValuesForHomeItems() {
        if (sIsFinishHomeBinding) {
            removeCallBacks(this.mUpdateStatusLogValuesForHomeItem);
            runOnLoggingThreadDelayed(this.mUpdateStatusLogValuesForHomeItem, 500);
        }
    }

    public void insertStatusLog(final String statusID, final int value) {
        if (!TextUtils.isEmpty(statusID)) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    String changedStatusID = SALogging.this.changeIdByMode(statusID);
                    Editor editor = Logging.sContext.getSharedPreferences(LauncherFiles.SAMSUNG_ANALYTICS_PREFERENCES_KEY, 0).edit();
                    editor.putInt(changedStatusID, value);
                    editor.apply();
                }
            });
        }
    }

    public void insertStatusLog(final String statusID, final String detail) {
        if (!TextUtils.isEmpty(statusID) && detail != null) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    String changedStatusID = SALogging.this.changeIdByMode(statusID);
                    String changedDetail = SALogging.this.changeEmptyDetail(detail);
                    Editor editor = Logging.sContext.getSharedPreferences(LauncherFiles.SAMSUNG_ANALYTICS_PREFERENCES_KEY, 0).edit();
                    editor.putString(changedStatusID, changedDetail);
                    editor.apply();
                }
            });
        }
    }

    public void insertEventLog(final String screenID, final String eventID) {
        if (!TextUtils.isEmpty(screenID) && !TextUtils.isEmpty(eventID)) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    try {
                        //SamsungAnalytics.getInstance().sendLog(((EventBuilder) new EventBuilder().setScreenView(screenID)).setEventName(SALogging.this.changeIdByMode(eventID)).build());
                    } catch (Exception e) {
                        Log.w(SALogging.TAG, "insertEventLog : Exception " + e.toString());
                    }
                }
            });
        }
    }

    public void insertEventLog(String screenID, String eventID, long value) {
        if (!TextUtils.isEmpty(screenID) && !TextUtils.isEmpty(eventID)) {
            final String str = eventID;
            final String str2 = screenID;
            final long j = value;
            runOnLoggingThread(new Runnable() {
                public void run() {
                    try {
                        //SamsungAnalytics.getInstance().sendLog(((EventBuilder) new EventBuilder().setScreenView(str2)).setEventName(SALogging.this.changeIdByMode(str)).setEventValue(j).build());
                    } catch (Exception e) {
                        Log.w(SALogging.TAG, "insertEventLog : Exception " + e.toString());
                    }
                }
            });
        }
    }

    public void insertEventLog(final String screenID, final String eventID, final String detail) {
        if (!TextUtils.isEmpty(screenID) && !TextUtils.isEmpty(eventID) && detail != null) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    String changedEventID = SALogging.this.changeIdByMode(eventID);
                    try {
                        //SamsungAnalytics.getInstance().sendLog(((EventBuilder) new EventBuilder().setScreenView(screenID)).setEventName(changedEventID).setEventDetail(SALogging.this.changeEmptyDetail(detail)).build());
                    } catch (Exception e) {
                        Log.w(SALogging.TAG, "insertEventLog : Exception " + e.toString());
                    }
                }
            });
        }
    }

    public void insertEventLog(String screenID, String eventID, String detail, Map<String, String> customDimen) {
        if (!TextUtils.isEmpty(screenID) && !TextUtils.isEmpty(eventID) && detail != null && !customDimen.isEmpty()) {
            final String str = eventID;
            final String str2 = detail;
            final String str3 = screenID;
            final Map<String, String> map = customDimen;
            runOnLoggingThread(new Runnable() {
                public void run() {
                    String changedEventID = SALogging.this.changeIdByMode(str);
                    try {
                        //SamsungAnalytics.getInstance().sendLog(((EventBuilder) ((EventBuilder) new EventBuilder().setScreenView(str3)).setEventName(changedEventID).setEventDetail(SALogging.this.changeEmptyDetail(str2)).setDimension(map)).build());
                    } catch (Exception e) {
                        Log.w(SALogging.TAG, "insertEventLog : Exception " + e.toString());
                    }
                }
            });
        }
    }

    public void insertEventLog(String screenID, String eventID, long value, String detail) {
        if (!TextUtils.isEmpty(screenID) && !TextUtils.isEmpty(eventID) && detail != null) {
            final String str = eventID;
            final String str2 = detail;
            final String str3 = screenID;
            final long j = value;
            runOnLoggingThread(new Runnable() {
                public void run() {
                    String changedEventID = SALogging.this.changeIdByMode(str);
                    try {
                        //SamsungAnalytics.getInstance().sendLog(((EventBuilder) new EventBuilder().setScreenView(str3)).setEventName(changedEventID).setEventValue(j).setEventDetail(SALogging.this.changeEmptyDetail(str2)).build());
                    } catch (Exception e) {
                        Log.w(SALogging.TAG, "insertEventLog : Exception " + e.toString());
                    }
                }
            });
        }
    }

    public void insertQOEventLog(final int option, final Launcher activity) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (SALogging.this.mQOInfo == null) {
                    Log.e(SALogging.TAG, "mQOInfo is null object");
                    return;
                }
                Resources res = Logging.sContext.getResources();
                String optionName = Utilities.getStringByLocale(activity, option, Locale.ENGLISH.toString());
                if (option == R.string.quick_option_secure_folder) {
                    optionName = "Add to secure folder";
                }
                String detail = optionName;
                int stageMode = activity.getTopStageMode();
                String screenID = SALogging.this.getQuickOptionScreenId(res, stageMode, activity.getSecondTopStageMode());
                String eventID = null;
                switch (SALogging.this.mQOInfo.mItemType) {
                    case 1:
                        if (stageMode != 1) {
                            if (stageMode != 2) {
                                if (stageMode == 5) {
                                    eventID = res.getString(R.string.event_Folder_QO_QuickOptionOfApp);
                                    break;
                                }
                            }
                            eventID = res.getString(R.string.event_Home_QuickOptionOfApp);
                            break;
                        }
                        eventID = res.getString(R.string.event_Home_QuickOptionOfApp);
                        break;
                    case 16:
                        if (stageMode != 1) {
                            if (stageMode == 2) {
                                eventID = res.getString(R.string.event_Home_QuickOptionOfFolder);
                                break;
                            }
                        }
                        eventID = res.getString(R.string.event_Home_QuickOptionOfFolder);
                        break;
                    case 17:
                        if (stageMode == 1) {
                            eventID = res.getString(R.string.event_Home_QuickOptionOfWidget);
                            break;
                        }
                        break;
                }
                if (screenID != null && eventID != null) {
                    SALogging.this.insertEventLog(screenID, eventID, detail);
                }
            }
        });
    }

    public AppShortcutPinningInfo getAppShortcutPinningInfo() {
        return this.mAppShortcutPinningInfo;
    }

    public void insertAppShortcutEventLog(final String shortcutID, final String packageName, final Launcher activity) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                String screenID = SALogging.this.getQuickOptionScreenId(res, activity.getTopStageMode(), activity.getSecondTopStageMode());
                String eventID = res.getString(R.string.event_AppOptionsOfApp);
                Map customDimen = new HashMap();
                customDimen.put("Package", packageName);
                if (screenID != null && !customDimen.isEmpty()) {
                    SALogging.this.insertEventLog(screenID, eventID, shortcutID, customDimen);
                }
            }
        });
    }

    public void insertAppShortcutPinningStartEventLog(final String shortcutID, final String packageName) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                String screenID = res.getString(R.string.screen_Home_1xxx);
                String eventID = res.getString(R.string.event_PinningShortcutFromAppOptions);
                SALogging.this.mAppShortcutPinningInfo = new AppShortcutPinningInfo(shortcutID, packageName);
                SALogging.this.insertEventLog(screenID, eventID);
            }
        });
    }

    public void insertAppShortcutPinnedEventLog(final Launcher activity) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                if (SALogging.this.mAppShortcutPinningInfo != null) {
                    String screenID;
                    int stageMode = activity.getTopStageMode();
                    Resources res = Logging.sContext.getResources();
                    if (stageMode == 5) {
                        screenID = res.getString(R.string.screen_HomeFolder_Primary);
                    } else {
                        screenID = res.getString(R.string.screen_Home_1xxx);
                    }
                    String eventID = res.getString(R.string.event_PinnedShortcut);
                    Map customDimen = new HashMap();
                    customDimen.put("Package", SALogging.this.mAppShortcutPinningInfo.mPackageName);
                    SALogging.this.insertEventLog(screenID, eventID, SALogging.this.mAppShortcutPinningInfo.mAppShortcutID, customDimen);
                    SALogging.this.mAppShortcutPinningInfo = null;
                }
            }
        });
    }

    public void insertNotiPreviewEventLog(final String packageName, final int eventIDresId, final Launcher activity) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                String screenID = SALogging.this.getQuickOptionScreenId(res, activity.getTopStageMode(), activity.getSecondTopStageMode());
                String eventID = res.getString(eventIDresId);
                if (screenID != null) {
                    SALogging.this.insertEventLog(screenID, eventID, packageName);
                }
            }
        });
    }

    private String getQuickOptionScreenId(Resources res, int stageMode, int secondTopStageMode) {
        String screenID = "";
        if (stageMode == 1) {
            return res.getString(R.string.screen_Home_QuickOptions);
        }
        if (stageMode == 2) {
            return res.getString(R.string.screen_Apps_QuickOptions);
        }
        if (stageMode != 5) {
            return screenID;
        }
        if (secondTopStageMode == 1) {
            return res.getString(R.string.screen_HomeFolder_QuickOptions);
        }
        if (secondTopStageMode == 2) {
            return res.getString(R.string.screen_AppsFolder_QuickOptions);
        }
        return screenID;
    }

    public void insertCancelAddWidgetLog() {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                SALogging.this.insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_CancelAddingWidget));
            }
        });
    }

    public void insertEnterResizeWidgetLog() {
        runOnLoggingThread(new Runnable() {
            public void run() {
                Resources res = Logging.sContext.getResources();
                SALogging.this.insertEventLog(res.getString(R.string.screen_Home_QuickOptions), res.getString(R.string.event_Home_ResizeWidget));
            }
        });
    }

    public void insertGesturePointOnTrayChange(final int level, final int start, final int end) {
        runOnLoggingThread(new Runnable() {
            public void run() {
                String eventId;
                Resources res = Logging.sContext.getResources();
                String detail = "(" + start + "," + end + ")";
                if (level == 0) {
                    eventId = res.getString(R.string.event_GesturePointToApps);
                } else {
                    eventId = res.getString(R.string.event_GesturePointToHome);
                }
                SALogging.this.insertEventLog(res.getString(R.string.screen_Home_1xxx), eventId, detail);
            }
        });
    }

    public void insertGoogleSearchLaunchCount() {
        runOnLoggingThread(new Runnable() {
            public void run() {
                //GSW.insertEnterSearchCount(Logging.sContext, true);
            }
        });
    }

    public void insertAddPairAppsEventLog(final IconInfo iconInfo) {
        runOnLoggingThread(new Runnable() {
            public void run() {
//                LauncherPairAppsInfo pairInfo = iconInfo;
//                String f_pkgName = pairInfo.mFirstApp.getCN().getPackageName();
//                String s_pkgName = pairInfo.mSecondApp.getCN().getPackageName();
//                Resources res = Logging.sContext.getResources();
//                String screenID = res.getString(R.string.screen_Home_1xxx);
//                String eventID = res.getString(R.string.event_Add_PairApps);
//                Map customDimen = new HashMap();
//                customDimen.put("Package1", f_pkgName);
//                customDimen.put("Package2", s_pkgName);
//                SALogging.this.insertEventLog(screenID, eventID, "", customDimen);
            }
        });
    }
}
