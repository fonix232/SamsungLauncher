package com.android.launcher3;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PointerIconCompat;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnDrawListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.Toast;
import com.android.launcher3.LauncherModel.Callbacks;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.bnr.extractor.LCExtractor;
import com.android.launcher3.common.compat.PinItemRequestCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.dialog.DisableAppConfirmationDialog;
import com.android.launcher3.common.dialog.SleepAppConfirmationDialog;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DragViewHelper;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.quickoption.QuickOptionManager;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.stage.StageManager;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.common.wallpaperscroller.GyroForShadow;
import com.android.launcher3.common.wallpaperscroller.WallpaperScroller;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.home.AddItemOnLastPageDialog;
import com.android.launcher3.home.AppsButtonSettingsActivity;
import com.android.launcher3.home.HomeBindController;
import com.android.launcher3.home.HomeController;
import com.android.launcher3.home.HomeModeChangeActivity;
import com.android.launcher3.home.HotWord;
import com.android.launcher3.home.LauncherAppWidgetHost;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.home.LauncherPairAppsInfo;
import com.android.launcher3.home.Workspace;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.home.ZeroPageProvider;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.pagetransition.PageTransitionManager;
import com.android.launcher3.proxy.LauncherActivityProxyCallbacks;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.AppFreezerUtils;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.LightingEffectManager;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.NavigationBarPolicy;
import com.android.launcher3.util.PermissionUtils;
import com.android.launcher3.util.SSecureUpdater;
import com.android.launcher3.util.SecureFolderHelper;
import com.android.launcher3.util.ShortcutTray;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.FirstFrameAnimatorHelper;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PinItemDragListener;
import com.android.launcher3.widget.controller.WidgetController;
import com.android.vcard.VCardConfig;
import com.google.android.libraries.launcherclient.LauncherClient;
import com.google.android.libraries.launcherclient.LauncherClient.ClientOptions;
import com.google.android.libraries.launcherclient.LauncherClientCallbacks;
import com.sec.android.app.launcher.R;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class Launcher extends Activity implements OnClickListener, Callbacks, LauncherProviderChangeListener {
    public static final int APPWIDGET_HOST_ID = 1024;
    private static final boolean DEBUG_DUMP_LOG = false;
    private static final boolean DEBUG_RESUME_TIME = false;
    private static final String DUMP_STATE_PROPERTY = "launcher_dump_state";
    private static final int EASY_MODE = 0;
    private static final String EXTRA_LAUNCHER_ACTION = "sec.android.intent.extra.LAUNCHER_ACTION";
    public static final ComponentName GOOGLE_SEARCH_WIDGET = new ComponentName(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME, "com.google.android.googlequicksearchbox.SearchWidgetProvider");
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";
    private static final String LAUNCHER_ACTION_ALL_APPS = "com.android.launcher2.ALL_APPS";
    private static final int LAUNCHER_SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 8192;
    private static final boolean LOGD = false;
    private static final boolean PROFILE_STARTUP = false;
    private static final Object RESUME_CALLBACKS_TOKEN = new Object();
    private static final String RUNTIME_STATE_VIEW_IDS = "launcher.view_ids";
    private static final String SETTINGS_WALLPAPER_TILT_STATUS = "wallpaper_tilt_status";
    private static final int STANDARD_MODE = 1;
    static final String TAG = "Launcher";
    private static DateFormat sDateFormat = null;
    private static final Date sDateStamp = new Date();
    private static int sDensityDpi = 0;
    private static final ArrayList<String> sDumpLogs = new ArrayList();
    private static boolean sIsRecreateModeChange = false;
    private static boolean sNeedCheckEasyMode = false;
    private static int sRecreateCountOnCreate = 0;
    private static final long sRunStart = java.lang.System.currentTimeMillis();
    private boolean mAttached = false;
    private final ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList();
    private final Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            Workspace ws = Launcher.this.mHomeController.getWorkspace();
            if (ws != null) {
                ws.buildPageHardwareLayers();
            }
        }
    };
    private boolean mChangeMode = false;
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
    private final ContentObserver mDarkFontObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(Launcher.TAG, "need_dark_font is changed!!" + selfChange);
            WhiteBgManager.setup(Launcher.this.getApplicationContext());
            Launcher.this.changeColorForBg();
        }
    };
    private final ContentObserver mDarkNavigationBarObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(Launcher.TAG, "need_dark_navigationBar is changed!!" + selfChange);
            WhiteBgManager.setupForNavigationBar(Launcher.this.getApplicationContext());
            Launcher.this.changeNavigationBarColor(WhiteBgManager.isWhiteNavigationBar());
        }
    };
    private final ContentObserver mDarkStatusBarObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(Launcher.TAG, "need_dark_statusBar is changed!!" + selfChange);
            WhiteBgManager.setupForStatusBar(Launcher.this.getApplicationContext());
            Launcher.this.changeStatusBarColor(WhiteBgManager.isWhiteStatusBar());
        }
    };
    private BroadcastReceiver mDateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    Log.i(Launcher.TAG, "onReceive: " + action);
                    Object obj = -1;

                    switch(action) {
                        case LiveIconManager.CLOCK_ALARM_INTENT_NAME:
                            refreshClock();
                            break;
                        case LiveIconManager.CALENDAR_ALARM_INTENT_NAME:
                            refreshCalendar();
                            break;
                        case "android.intent.action.DATE_CHANGED":
                        case "android.intent.action.TIMEZONE_CHANGED":
                        case "android.intent.action.TIME_SET":
                            LiveIconManager.cancelCalendarAlarm(context);
                            refreshCalendar();
                            LiveIconManager.cancelClockAlarm(context);
                            refreshClock();
                            break;
                        default:
                            break;
                    }
                    Launcher.this.mModel.onRefreshLiveIcon();
                }
            }
        }

        private void refreshCalendar() {
            for (String pkgName : LiveIconManager.getCalendarPackages()) {
                LiveIconManager.clearLiveIconCache(pkgName);
            }
            LiveIconManager.setCalendarAlarm(Launcher.this.getApplicationContext());
        }

        private void refreshClock() {
            LiveIconManager.clearLiveIconCache(Utilities.getClockPackageName());
            LiveIconManager.setClockAlarm(Launcher.this.getApplicationContext());
        }
    };
    private SpannableStringBuilder mDefaultKeySsb = null;
    private DeviceProfile mDeviceProfile;
    private DragLayer mDragLayer;
    private DragManager mDragMgr;
    private final ContentObserver mEasyModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Launcher.sNeedCheckEasyMode = true;
        }
    };
    private FolderLock mFolderLock;
    private GlobalSettingUtils mGlobalSettingUtils;
    private boolean mHasFocus = false;
    private HomeBindController mHomeBindController;
    private HomeController mHomeController;
    private HotWord mHotWord;
    private Runnable mHotseatOnResumeCallback = null;
    private LayoutInflater mInflater;
    private boolean mIsSafeModeEnabled;
    @SuppressLint({"UseSparseArrays"})
    private HashMap<Integer, Integer> mItemIdToViewId = new HashMap();
    private LCExtractor mLCExtractor = null;
    private final BroadcastReceiver mLCExtractorReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Launcher.this.startLCExtractor(0);
        }
    };
    private boolean mLandConfigInPort = false;
    private boolean mLongPress = false;
    private LauncherModel mModel;
    private MultiSelectManager mMultiSelectManager;
    private NavigationBarPolicy mNavibarPolicy;
    private final ArrayList<Runnable> mOnResumeCallbacks = new ArrayList();
    private boolean mOnResumeNeedsLoad;
    private int mOnResumeState = 0;
    private PageTransitionManager mPageTransitionManager = null;
    private boolean mPaused = true;
    private LauncherClient mPreWarmingClient;
    private QuickOptionManager mQuickOptionManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                Launcher.this.mHomeBindController.setUserPresent(false);
                Launcher.this.mHomeController.exitResizeState(false);
                Launcher.this.mHomeBindController.updateAutoAdvanceState();
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                Launcher.this.mHomeBindController.setUserPresent(true);
                Launcher.this.mHomeBindController.updateAutoAdvanceState();
                Launcher.this.mModel.onRefreshLiveIcon();
            } else if (ZeroPageController.ACTION_INTENT_SET_ZEROPAGE.equals(action)) {
                if (ZeroPageController.supportVirtualScreen()) {
                    String packageName = intent.getStringExtra("zeroapp_package_name");
                    String className = intent.getStringExtra("zeroapp_class_name");
                    ZeroPageController zeroPageController = Launcher.this.mHomeController.getZeroPageController();
                    if (zeroPageController != null) {
                        zeroPageController.changeZeroPage(new ComponentName(packageName, className));
                        Log.d(Launcher.TAG, "Action : " + action + ", packageName = " + packageName + ", className = " + className);
                    }
                }
            } else if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                if ("homekey".equals(intent.getStringExtra("reason"))) {
                    if (LauncherFeature.supportQuickOption() && Launcher.this.isQuickOptionShowing()) {
                        Launcher.this.mDragMgr.removeQuickOptionView("3");
                    }
                    if (Launcher.this.mMultiSelectManager != null && Launcher.this.mMultiSelectManager.isMultiSelectMode()) {
                        Launcher.this.mMultiSelectManager.homeKeyPressed();
                    }
                    Launcher.this.mHomeController.homeKeyPressed();
                }
            }
        }
    };
    private Handler mResumeCallbacksHandler;
    private RotationPrefChangeListener mRotationPrefChangeListener = null;
    private RunResumeCallbackInSchedule mRunResumeCallbackInSchedule;
    private SSecureUpdater mSSecureUpdater;
    private Bundle mSavedState;
    private String mSearchedApp = null;
    private UserHandle mSearchedAppUser = null;
    private SensorManager mSensorManager;
    private SharedPreferences mSharedPrefs;
    private boolean mShortPress = false;
    private boolean mSkipAnim = false;
    private StageManager mStageManager;
    private Stats mStats;
    private WallpaperScroller mTiltWallpaperScroller;
    private TrayManager mTrayManager;
    private boolean mVisible = false;
    private boolean mWallpaperTiltSettingEnabled = false;
    private IBinder mWindowToken = null;
    private boolean mZeroPageStartedByHomeKey = false;

    class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        CloseSystemDialogsIntentReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Launcher.this.closeSystemDialogs();
        }
    }

    private class RotationPrefChangeListener implements OnSharedPreferenceChangeListener {
        private RotationPrefChangeListener() {
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!key.equals(Utilities.ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY)) {
                return;
            }
            if (Utilities.isOnlyPortraitMode()) {
                Launcher.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            } else {
                Launcher.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }

    private class RunResumeCallbackInSchedule implements Runnable {
        private static final String TAG = "RunCallbackInSchedule";
        private boolean runInSchedule;

        private RunResumeCallbackInSchedule() {
            this.runInSchedule = false;
        }

        public void run() {
            if (Launcher.this.mBindOnResumeCallbacks.isEmpty()) {
                Log.i(TAG, "mBindOnResumeCallbacks, empty");
            } else if (!Launcher.this.mPaused || Launcher.this.mVisible) {
                Log.i(TAG, "mBindOnResumeCallbacks, size " + Launcher.this.mBindOnResumeCallbacks.size());
                Runnable r = (Runnable) Launcher.this.mBindOnResumeCallbacks.remove(0);
                if (r != null) {
                    this.runInSchedule = true;
                    r.run();
                    this.runInSchedule = false;
                }
                scheduleNext();
            } else {
                Log.i(TAG, "RunResumeCallbackInSchedule, stop! becauseof pause & no visible state");
            }
        }

        private void scheduleNext() {
            Launcher.this.mResumeCallbacksHandler.postAtTime(this, Launcher.RESUME_CALLBACKS_TOKEN, SystemClock.uptimeMillis() + 1);
        }
    }

    private static class GSAPreWarmingClientCallbacks implements LauncherClientCallbacks {
        private GSAPreWarmingClientCallbacks() {
        }

        public void onOverlayScrollChanged(float progress) {
        }

        public void onServiceStateChanged(boolean overlayAttached, boolean hotWordActive) {
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        Bundle bundle;
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        Configuration configuration = res.getConfiguration();
        Utilities.setOrientation(configuration.orientation);
        Log.i(TAG, "onCreate configuration = " + configuration + ", displayMetrics = " + res.getDisplayMetrics());
        if (Utilities.isDeskTopMode(this)) {
            Log.i(TAG, "kill Process cause of wrong info from DeX");
            Process.killProcess(Process.myPid());
        } else if (LauncherFeature.isTablet() || !configuration.isLayoutSizeAtLeast(4)) {
            sRecreateCountOnCreate = 0;
        } else {
            sRecreateCountOnCreate++;
            Log.e(TAG, "Wrong configuration -> recreateLauncher (count = " + sRecreateCountOnCreate + ")");
            if (sRecreateCountOnCreate <= 5) {
                recreateLauncher();
            } else {
                Log.e(TAG, "We can't recreate activity any more");
                sRecreateCountOnCreate = 0;
            }
        }
        NotificationListener.setApplicationContext(getApplicationContext());
        LauncherAppState.setApplicationContext(getApplicationContext());
        LauncherAppState app = LauncherAppState.getInstance();
        if (LauncherFeature.supportEasyModeChange()) {
            changeEasyModeIfNecessary(false);
        }
        if (LauncherFeature.supportHomeModeChange() && !LauncherAppState.getInstance().isEasyModeEnabled()) {
            changeHomeModeIfNecessary();
        }
        this.mSharedPrefs = getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportRotationSetting()) {
            Utilities.setOnlyPortraitMode(getApplicationContext(), this.mSharedPrefs.getBoolean(Utilities.ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY, true));
            this.mRotationPrefChangeListener = new RotationPrefChangeListener();
            this.mSharedPrefs.registerOnSharedPreferenceChangeListener(this.mRotationPrefChangeListener);
        } else if (LauncherFeature.isTablet()) {
            Utilities.setOnlyPortraitMode(getApplicationContext(), false);
        }
        if (Utilities.ATLEAST_O) {
            Utilities.setMultiWindowMode(isInMultiWindowMode());
        }
        Utilities.setMobileKeyboardMode(configuration);
        app.makeDeviceProfile(this);
        app.initScreenGrid(app.isHomeOnlyModeEnabled());
        if (LauncherFeature.supportHotword() && !LauncherFeature.supportGSAPreWarming()) {
            this.mHotWord = new HotWord(this);
        }
        if (LauncherFeature.supportGSAPreWarming()) {
            this.mPreWarmingClient = new LauncherClient(this, new GSAPreWarmingClientCallbacks(), new ClientOptions(false, LauncherFeature.supportHotword(), true));
        }
        this.mDeviceProfile = app.getDeviceProfile();
        Utilities.sIsRtl = getResources().getConfiguration().getLayoutDirection() == 1;
        boolean isDpiChanged = false;
        if (sDensityDpi > 0 && sDensityDpi != res.getDisplayMetrics().densityDpi) {
            Log.i(TAG, "sDensityDpi = " + sDensityDpi + ", densityDpi = " + res.getDisplayMetrics().densityDpi);
            if (app.getIconCache() != null) {
                app.getIconCache().clearCache(this.mDeviceProfile.defaultIconSize);
            }
            isDpiChanged = true;
        }
        sDensityDpi = res.getDisplayMetrics().densityDpi;
        OpenThemeManager.getInstance().initThemeForIconLoading(isDpiChanged);
        OpenThemeManager.getInstance().preloadResources();
        this.mIsSafeModeEnabled = getPackageManager().isSafeMode();
        if (LauncherFeature.supportMultiSelect()) {
            this.mMultiSelectManager = new MultiSelectManager();
        }
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
            this.mFolderLock.setup(this);
        }
        if (LauncherFeature.isSSecureSupported()) {
            this.mSSecureUpdater = SSecureUpdater.getInstance();
            this.mSSecureUpdater.setup();
        }
        this.mModel = app.setLauncher(this);
        this.mDragMgr = new DragManager(this);
        if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            this.mTrayManager = new TrayManager();
        }
        WhiteBgManager.setup(this);
        WhiteBgManager.setupForStatusBar(this);
        WhiteBgManager.setupForNavigationBar(this);
        changeNavigationBarColor(WhiteBgManager.isWhiteNavigationBar());
        changeStatusBarColor(WhiteBgManager.isWhiteStatusBar());
        if (sIsRecreateModeChange) {
            bundle = null;
        } else {
            bundle = savedInstanceState;
        }
        this.mStageManager = new StageManager(this, bundle);
        this.mHomeController = (HomeController) this.mStageManager.getStage(1);
        this.mHomeBindController = this.mHomeController.getBindController();
        if (LauncherFeature.supportQuickOption()) {
            this.mQuickOptionManager = new QuickOptionManager(this);
        }
        this.mInflater = getLayoutInflater();
        this.mStats = new Stats(this);
        this.mPaused = false;
        this.mDefaultKeySsb = new SpannableStringBuilder();
        setOrientation();
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            setContentView(R.layout.launcher_homeonly);
        } else {
            setContentView(R.layout.launcher);
        }
        setupViews();
        this.mDeviceProfile.layoutGrid(this);
        this.mSavedState = savedInstanceState;
        if (this.mSavedState == null || sIsRecreateModeChange) {
            this.mStageManager.startStage(1, null);
        } else {
            restoreState(this.mSavedState);
        }
        sIsRecreateModeChange = false;
        setIndicatorTransparency();
        this.mResumeCallbacksHandler = new Handler();
        this.mRunResumeCallbackInSchedule = new RunResumeCallbackInSchedule();
        ShortcutTray.checkIconTrayEnabled(this);
        if (!this.mHomeController.isRestoring()) {
            if (this.mChangeMode) {
                this.mHomeController.getWorkspace().setRestorePage(-1001);
                this.mChangeMode = false;
            }
            this.mModel.startLoader(this.mHomeController.getWorkspace().getRestorePage());
        }
        Selection.setSelection(this.mDefaultKeySsb, 0);
        GameHomeManager.getInstance().initGameHomeManager(this);
        LightingEffectManager.INSTANCE.setup(this);
        registerReceiversAndObservers();
        this.mPageTransitionManager = new PageTransitionManager(this);
        this.mGlobalSettingUtils = new GlobalSettingUtils(this);
        if (!this.mHomeBindController.isWorkspaceLoading()) {
            PinItemDragListener.handleDragRequest(this, getIntent());
        }
        if (getIntent() != null && getIntent().getBooleanExtra("StartEdit", false)) {
            this.mGlobalSettingUtils.startHomeSettingBySettingMenu(getIntent());
        }
        LauncherAppState.getInstance().getLauncherProxy().setLauncherActivityProxyCallbacks(new LauncherActivityProxyCallbacks() {
            public void enterHomeSettingView() {
                Launcher.this.startHomeSettingActivity();
            }

            public void showAppsButton() {
                callAppsButtonEnabled(true);
            }

            public boolean isEnableAppsButton() {
                return LauncherAppState.getInstance().getAppsButtonEnabled();
            }

            public boolean isHomeOnlyMode() {
                return LauncherAppState.getInstance().isHomeOnlyModeEnabled();
            }

            public boolean isAvailableSleepMode(ItemInfo item) {
                if (item == null || item.user == null || item.componentName == null || !AppFreezerUtils.canPutIntoSleepMode(Launcher.this, item.user, item.componentName.getPackageName())) {
                    return false;
                }
                return true;
            }

            public boolean isAlreadySleepMode(ItemInfo item) {
                if (item == null || item.user == null || item.componentName == null || !AppFreezerUtils.isInSleepMode(Launcher.this, item.user, item.componentName.getPackageName())) {
                    return false;
                }
                return true;
            }

            public void hideAppsButton() {
                callAppsButtonEnabled(false);
            }

            private void callAppsButtonEnabled(boolean enabled) {
                if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() || LauncherAppState.getInstance().getAppsButtonEnabled() == enabled) {
                    Log.i(Launcher.TAG, "setAppsButtonEnabled return");
                    return;
                }
                try {
                    AppsButtonSettingsActivity appsButtonActivity = LauncherAppState.getInstance().getAppsButtonSettingsActivity();
                    if (appsButtonActivity == null) {
                        Intent intent = new Intent("showApps");
                        intent.putExtra("showApps", enabled);
                        intent.setClassName(Launcher.this.getPackageName(), "com.android.launcher3.home.AppsButtonSettingsActivity");
                        Launcher.this.startActivity(intent);
                        return;
                    }
                    appsButtonActivity.changeAppsButtonEnabled(enabled);
                    appsButtonActivity.finish();
                    LauncherAppState.getInstance().setAppsButtonSettingsActivity(null);
                    SettingsActivity settingsActivity = LauncherAppState.getInstance().getSettingsActivity();
                    if (settingsActivity != null) {
                        settingsActivity.finish();
                        LauncherAppState.getInstance().setSettingsActivity(null);
                    }
                } catch (SecurityException e) {
                    Log.w(Launcher.TAG, "SecurityException e = " + e);
                }
            }

            public void hideApps(ArrayList<ItemInfo> items) {
                if (items != null) {
                    Launcher.this.updateItemInfo(items, new ArrayList());
                }
            }

            public void unHideApps(ArrayList<ItemInfo> items) {
                if (items != null) {
                    Launcher.this.updateItemInfo(new ArrayList(), items);
                }
            }

            public void uninstallOrDisableApp(ItemInfo item) {
                if (item instanceof IconInfo) {
                    IconInfo iconInfo = (IconInfo) item;
                    int flags = iconInfo.flags;
                    if ((flags & 1) == 0) {
                        DisableAppConfirmationDialog.createAndShow(Launcher.this, iconInfo.user, item.componentName.getPackageName(), iconInfo.title.toString(), new BitmapDrawable(Launcher.this.getResources(), iconInfo.mIcon), Launcher.this.getFragmentManager(), null);
                    } else {
                        Launcher.this.startApplicationUninstallActivity(item.componentName, flags, item.user, true);
                    }
                }
            }

            public void putToSleepMode(ItemInfo item) {
                if (item != null && item.componentName != null) {
                    SleepAppConfirmationDialog.createAndShow(Launcher.this, item.user, item.componentName.getPackageName());
                }
            }

            public void addToSecureFolder(ItemInfo item) {
                SecureFolderHelper.addAppToSecureFolder(Launcher.this, item.componentName.getPackageName());
            }

            public boolean isSecureFolderSetup() {
                return SecureFolderHelper.isSecureFolderExist(Launcher.this);
            }

            public boolean canAppAddToSecureFolder(ItemInfo item) {
                return (item == null || item.componentName == null || !SecureFolderHelper.canAddAppToSecureFolder(Launcher.this, item.user, item.componentName.getPackageName())) ? false : true;
            }

            public void clearBadge(ItemInfo item) {
                if (item instanceof IconInfo) {
                    IconInfo iconInfo = (IconInfo) item;
                    ComponentName componentName = iconInfo.getTargetComponent();
                    try {
                        Intent intent = new Intent("com.sec.intent.action.BADGE_COUNT_UPDATE");
                        intent.putExtra("badge_count_package_name", componentName.getPackageName());
                        intent.putExtra("badge_count_class_name", componentName.getClassName());
                        intent.putExtra("badge_count", 0);
                        // TODO: Fix intent flag
                        // intent.addFlags(268435456);
                        if (iconInfo.getUserHandle() == null || iconInfo.getUserHandle().getUser() == null) {
                            Launcher.this.sendBroadcast(intent);
                        } else {
                            Launcher.this.sendBroadcastAsUser(intent, iconInfo.getUserHandle().getUser());
                        }
                    } catch (Exception e) {
                        Log.d(Launcher.TAG, "removeBadge():Can't send the broadcast >>> " + e);
                    }
                }
            }

            public void enterHomeSettingChangeModeView() {
                try {
                    Intent intent = new Intent();
                    intent.setClassName(Launcher.this.getPackageName(), "com.android.launcher3.home.HomeModeChangeActivity");
                    Launcher.this.startActivity(intent);
                } catch (SecurityException e) {
                    Log.w(Launcher.TAG, "SecurityException e = " + e);
                }
            }

            public void changeHomeStyle(boolean homeOnlyMode) {
                Intent intent = new Intent(HomeModeChangeActivity.ACTION_CHANGE_HOMEONLYMODE);
                intent.putExtra(HomeModeChangeActivity.EXTRA_HOMEONLYEMODE, homeOnlyMode);
                Launcher.this.sendBroadcast(intent, "com.samsung.android.launcher.permission.CHANGE_HOMEONLYMODE");
            }

            public void enterBadgeManagementView() {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                if (activity != null) {
                    activity.showBadgeManagerSettings();
                }
            }

            public boolean enableAllAppsBadge(boolean enable) {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                return activity != null && activity.enableAllAppsBadge(enable);
            }

            public boolean enableSingleAppBadge(String title, boolean enable) {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                return activity != null && activity.enableSingleAppBadge(title, enable);
            }

            public void enterHomeAboutPageView() {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                if (activity != null) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName(activity.getPackageName(), "com.android.launcher3.AboutPageActivity");
                        activity.startActivity(intent);
                    } catch (SecurityException e) {
                        Log.w(Launcher.TAG, "SecurityException e = " + e);
                    }
                }
            }

            public boolean isSingleAppBadgeChecked(String className) {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                return activity != null && activity.isSingleAppBadgeChecked(className);
            }

            public boolean isAllAppsBadgeSwitchChecked() {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                return activity != null && activity.isAllAppsBadgeSwitchChecked();
            }

            public void setNotificationPanelExpansionEnabled(boolean value) {
                SettingsActivity activity = LauncherAppState.getInstance().getSettingsActivity();
                if (activity != null && activity.isSettingFragmentShowing()) {
                    activity.updatePreNotificationPanelSetting(value);
                }
                LauncherAppState.getInstance().setNotificationPanelExpansionEnabled(value, true);
            }
        });
    }

    private void setupViews() {
        View launcherView = findViewById(R.id.launcher);
        this.mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        launcherView.setSystemUiVisibility(1792);
        this.mDragLayer.setup(this, this.mDragMgr);
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.setup(this);
        }
        this.mStageManager.setupStartupViews();
        if (this.mTrayManager != null) {
            this.mTrayManager.setup(this, this.mDragMgr);
        }
    }

    private void setOrientation() {
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);
        this.mStageManager.onStop();
        NotificationListener.removeNotificationsChangedListener();
    }

    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
        this.mStageManager.onStart();
    }

    protected void onRestart() {
        super.onRestart();
        if (this.mTrayManager != null) {
            this.mTrayManager.resetMoving();
        }
    }

    protected void onResume() {
        Utilities.launcherResumeTesterStart();
        Log.v(TAG, "Launcher.onResume()");
        super.onResume();
        if (ZeroPageController.isMoving()) {
            Log.i(TAG, "move to home stage");
            if (this.mStageManager != null) {
                StageEntry data = new StageEntry();
                data.enableAnimation = false;
                this.mStageManager.startStage(1, data);
            }
            this.mOnResumeState = 0;
        }
        if (LauncherFeature.supportZeroPageHome() && !this.mZeroPageStartedByHomeKey && Utilities.getZeroPageKey(getApplicationContext(), ZeroPageProvider.START_FROM_ZEROPAGE)) {
            Utilities.setZeroPageKey(getApplicationContext(), false, ZeroPageProvider.START_FROM_ZEROPAGE);
            ZeroPageProvider.notifyChange(getApplicationContext());
        }
        long stayTime = GSIMLogging.getInstance().getZeroPageStayTime();
        if (stayTime != -1) {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ZERO_PAGE_STAY_TIME, String.valueOf(GSIMLogging.getInstance().classifyZeroPageStayTime(stayTime)), stayTime, false);
        }
        this.mZeroPageStartedByHomeKey = false;
        if (LauncherFeature.supportEasyModeChange() && sNeedCheckEasyMode) {
            changeEasyModeIfNecessary(true);
            if (sIsRecreateModeChange) {
                Log.i(TAG, "recreateModeChange return");
                return;
            }
        }
        int transitionId = OpenThemeManager.getInstance().getInteger(ThemeItems.TRANSITON_EFFECT.value());
        if (transitionId == -1) {
            transitionId = 0;
        }
        setWhichTransitionEffect(transitionId);
        if (LauncherFeature.supportGSAPreWarming() && this.mPreWarmingClient != null) {
            this.mPreWarmingClient.onResume();
        }
        if (!(this.mOnResumeState == 0 || this.mStageManager == null)) {
            this.mStageManager.startStage(this.mOnResumeState, null);
            this.mOnResumeState = 0;
        }
        this.mPaused = false;
        if (this.mHomeController.isRestoring() || this.mOnResumeNeedsLoad) {
            this.mHomeBindController.setWorkspaceLoading(true);
            this.mBindOnResumeCallbacks.clear();
            this.mModel.startLoader(-1001);
            this.mHomeController.setRestoring(false);
            this.mOnResumeNeedsLoad = false;
        }
        if (this.mBindOnResumeCallbacks.size() > 0) {
            Log.d(TAG, "mRunResumeCallbackInSchedule needToRun!");
            this.mResumeCallbacksHandler.removeCallbacksAndMessages(RESUME_CALLBACKS_TOKEN);
            new Handler().post(new Runnable() {
                public void run() {
                    Log.i(Launcher.TAG, "schedule start!");
                    Launcher.this.mRunResumeCallbackInSchedule.scheduleNext();
                }
            });
        }
        if (this.mOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < this.mOnResumeCallbacks.size(); i++) {
                ((Runnable) this.mOnResumeCallbacks.get(i)).run();
            }
            this.mOnResumeCallbacks.clear();
        }
        if (this.mHotseatOnResumeCallback != null) {
            this.mHotseatOnResumeCallback.run();
            this.mHotseatOnResumeCallback = null;
        }
        if (this.mMultiSelectManager != null) {
            if (!this.mMultiSelectManager.isMultiSelectMode() && this.mMultiSelectManager.isShowingHelpDialog()) {
                this.mMultiSelectManager.hideHelpDialog(false);
            }
            if (!isSkipAnim()) {
                this.mMultiSelectManager.postUninstallActivity();
            }
        }
        if (!this.mHomeBindController.isWorkspaceLoading()) {
            LauncherAppState.getInstance().disableAndFlushExternalQueue();
        }
        if (this.mStageManager != null) {
            this.mStageManager.onResume();
        }
        this.mSkipAnim = false;
        finishSettingsActivity();
        if (LauncherFeature.supportWallpaperTilt()) {
            setupWallpaperScroller();
        }
        Utilities.broadcastStkIntent(this);
        this.mGlobalSettingUtils.checkEnterNormalState();
        Utilities.launcherResumeTesterEnd();
        if (this.mModel.isModelIdle()) {
            NotificationListener.setNotificationsChangedListener(this.mModel, getApplicationContext());
        }
    }

    protected void onPause() {
        Log.v(TAG, "Launcher.onPause()");
        this.mStageManager.onPause();
        LauncherAppState.getInstance().enableExternalQueue(true);
        if (LauncherFeature.supportGSAPreWarming() && this.mPreWarmingClient != null) {
            this.mPreWarmingClient.onPause();
        }
        super.onPause();
        this.mPaused = true;
        if (this.mDragMgr != null) {
            this.mDragMgr.cancelDrag();
            this.mDragMgr.resetLastGestureUpTime();
            if (LauncherFeature.supportQuickOption()) {
                this.mDragMgr.removeQuickOptionView();
            }
        }
        if (LauncherFeature.supportWallpaperTilt()) {
            shutdownWallpaperScroller();
        }
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && this.mHomeController.hasStartedSFinder()) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    BlurUtils.blurByWindowManager(false, Launcher.this.getWindow(), 0.0f, 0);
                    if (Launcher.this.mHomeController != null) {
                        Launcher.this.mHomeController.resetStartedSFinder();
                    }
                }
            }, 600);
        }
        closeDialogIfNeeded();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged. newConfig : " + newConfig);
        super.onConfigurationChanged(newConfig);
        if (Utilities.ATLEAST_O) {
            boolean isMultiWindowMode = isInMultiWindowMode();
            if (Utilities.isMultiWindowMode() != isMultiWindowMode) {
                if (isMultiWindowMode) {
                    SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_DockingMode), getResources().getString(R.string.event_Docking_mode), newConfig.orientation == 2 ? "2" : "1");
                } else {
                    SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_DockingMode), getResources().getString(R.string.event_Exit_Docking_mode));
                }
            }
            Utilities.setMultiWindowMode(isMultiWindowMode);
        }
        Utilities.setMobileKeyboardMode(newConfig);
        if (Utilities.canScreenRotate() || newConfig.orientation != 2) {
            Utilities.setOrientation(newConfig.orientation);
            if (this.mLandConfigInPort && !this.mPaused) {
                DataLoader.reinflateWidgetsIfNecessary();
                this.mLandConfigInPort = false;
                Log.d(TAG, "reInflateWidgetsIfNecessary by onConfigurationChanged");
            }
            LauncherAppState app = LauncherAppState.getInstance();
            app.makeDeviceProfile(this);
            app.initScreenGrid(app.isHomeOnlyModeEnabled());
            this.mDeviceProfile = app.getDeviceProfile();
            this.mDeviceProfile.layoutGrid(this);
            this.mStageManager.setConfiguration(newConfig);
            if (this.mMultiSelectManager != null) {
                this.mMultiSelectManager.onConfigurationChanged(newConfig);
            }
            if (this.mTrayManager != null) {
                this.mTrayManager.setBottomViewDragEnable();
                this.mTrayManager.onConfigurationChanged();
                return;
            }
            return;
        }
        Log.d(TAG, "onConfigurationChanged. launcher do not support landscape");
        this.mLandConfigInPort = true;
    }

    public Object onRetainNonConfigurationInstance() {
        if (this.mModel.isCurrentCallbacks(this)) {
            this.mModel.stopLoader();
        }
        return Boolean.TRUE;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.mHasFocus = hasFocus;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i(TAG, "onAttachedToWindow");
        if (LauncherFeature.supportGSAPreWarming()) {
            if (this.mPreWarmingClient != null) {
                this.mPreWarmingClient.onAttachedToWindow();
            }
        } else if (LauncherFeature.supportHotword() && this.mHotWord != null) {
            this.mHotWord.onAttachedToWindow();
        }
        if (LauncherFeature.supportHotword()) {
            setHotWordDetection(false);
        }
        if (LauncherFeature.supportWallpaperTilt() && this.mTiltWallpaperScroller != null && this.mTiltWallpaperScroller.isRunning() && this.mWindowToken == null) {
            View v = getWindow().peekDecorView();
            if (v != null) {
                this.mWindowToken = v.getWindowToken();
            }
            if (this.mWindowToken != null) {
                this.mTiltWallpaperScroller.setWindowToken(this.mWindowToken);
                Log.d(TAG, "WallpaperScroller - onAttachedToWindow - set mWindowToken");
            } else {
                Log.d(TAG, "WallpaperScroller - onAttachedToWindow - mWindowToken is null");
            }
        }
        setWallpaperOffsetToCenter();
        updateWhiteBgIfNecessary();
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        this.mAttached = true;
        this.mVisible = true;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        Log.i(TAG, "onDetachedFromWindow mAttached=" + this.mAttached);
        if (LauncherFeature.supportGSAPreWarming() && this.mPreWarmingClient != null) {
            this.mPreWarmingClient.onDetachedFromWindow();
        }
        if (this.mAttached) {
            if (!(!LauncherFeature.supportHotword() || LauncherFeature.supportGSAPreWarming() || this.mHotWord == null)) {
                this.mHotWord.onDetachedFromWindow();
            }
            this.mAttached = false;
        }
        this.mHomeBindController.updateAutoAdvanceState();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        boolean alreadyOnHome = this.mHasFocus && (intent.getFlags() & 4194304) != 4194304;
        this.mSkipAnim = alreadyOnHome;
        boolean isModalState = alreadyOnHome && this.mHomeController.isModalState();
        boolean isActionMain = "android.intent.action.MAIN".equals(intent.getAction());
        int topStage = this.mStageManager.getTopStage().getMode();
        if (isActionMain) {
            if (intent.getBooleanExtra(Utilities.EXTRA_ENTER_SCREEN_GRID, false)) {
                if (this.mHomeController != null) {
                    this.mHomeController.enterHomeScreenGrid(false);
                    return;
                }
                return;
            } else if (intent.getBooleanExtra("ZeroPageSetting", false)) {
                overridePendingTransition(R.anim.zero_page_setting_in, R.anim.zero_page_setting_out);
                ZeroPageController zeroPageController = this.mHomeController.getZeroPageController();
                if (zeroPageController != null) {
                    zeroPageController.enterZeroPageSetting();
                    return;
                }
                return;
            } else if (intent.getBooleanExtra("StartEdit", false)) {
                this.mGlobalSettingUtils.startHomeSettingBySettingMenu(intent);
                return;
            } else if (intent.getBooleanExtra(Utilities.EXTRA_ENTER_WIDGETS, false)) {
                showAppsOrWidgets(3, true, true);
                return;
            } else if (intent.getBooleanExtra(Utilities.EXTRA_ENTER_APPS_SCREEN_GRID, false)) {
                showAppsOrWidgets(2, false, true);
                return;
            } else if (intent.getBooleanExtra(Utilities.EXTRA_ENTER_HIDE_APPS, false)) {
                if (getTopStageMode() == 6) {
                    this.mStageManager.finishAllStage(null);
                }
                StageEntry data = new StageEntry();
                data.enableAnimation = false;
                data.putExtras(AppsPickerController.KEY_PICKER_MODE, Integer.valueOf(1));
                this.mStageManager.startStage(6, data);
                return;
            } else {
                this.mSearchedApp = intent.getStringExtra("AppSearch");
                this.mSearchedAppUser = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
                if (this.mSearchedAppUser == null) {
                    this.mSearchedAppUser = UserHandleCompat.myUserHandle().getUser();
                }
                if (this.mSearchedApp == null || LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    closeSystemDialogs();
                    if (this.mHomeController.getContainerView() != null) {
                        this.mHomeController.exitResizeState(true, "3");
                        boolean fromHomeKey = intent.getBooleanExtra("android.intent.extra.FROM_HOME_KEY", false);
                        if (intent.getStringExtra(EXTRA_LAUNCHER_ACTION) != null) {
                            if (LAUNCHER_ACTION_ALL_APPS.equals(intent.getStringExtra(EXTRA_LAUNCHER_ACTION)) && !LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                                StageEntry data = new StageEntry();
                                data.enableAnimation = false;
                                if (!isHomeStage()) {
                                    if (isFolderStage()) {
                                        this.mStageManager.finishStage(5, data);
                                    } else if (!isAppsStage()) {
                                        this.mStageManager.finishAllStage(null);
                                    }
                                }
                                if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
                                    this.mStageManager.onBackPressed();
                                }
                                this.mStageManager.startStage(2, data);
                            }
                        } else if (alreadyOnHome || fromHomeKey) {
                            if (this.mHomeController.isScreenGridState()) {
                                this.mHomeController.cancelGridChange();
                                this.mHomeController.finishAllStage();
                            } else if (!alreadyOnHome && isHomeStage()) {
                                this.mHomeController.finishAllStage();
                            }
                            if (this.mMultiSelectManager != null) {
                                if (this.mMultiSelectManager.isMultiSelectMode()) {
                                    this.mMultiSelectManager.showMultiSelectPanel(false, false);
                                    onChangeSelectMode(false, false);
                                }
                                this.mMultiSelectManager.clearUninstallApplist();
                            }
                            if (!alreadyOnHome || (topStage != 1 && isModalState)) {
                                this.mHomeController.enableCustomLayoutAnimation(false);
                            }
                            ZeroPageController zeroPageController = this.mHomeController.getZeroPageController();
                            if (zeroPageController == null || !zeroPageController.isCurrentZeroPage()) {
                                GlobalSettingUtils.resetSettingsValue();
                                this.mHomeController.enterNormalState(alreadyOnHome);
                                this.mHomeController.enableCustomLayoutAnimation(true);
                            } else {
                                zeroPageController.startZeroPage();
                                this.mZeroPageStartedByHomeKey = true;
                            }
                        } else {
                            new StageEntry().broughtToHome = true;
                        }
                        View v = getWindow().peekDecorView();
                        if (!(v == null || v.getWindowToken() == null)) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            }
                        }
                        Utilities.closeDialog(this);
                    } else {
                        return;
                    }
                }
                StageEntry entry = new StageEntry();
                entry.enableAnimation = false;
                entry.fromStage = 1;
                entry.toStage = 2;
                this.mStageManager.startStage(2, entry);
                return;
            }
        }
        PinItemDragListener.handleDragRequest(this, intent);
        if (isActionMain) {
            Workspace workspace = this.mHomeController.getWorkspace();
            if (alreadyOnHome && !isModalState && topStage == 1 && !workspace.isTouchActive()) {
                final Workspace workspace2 = workspace;
                workspace.post(new Runnable() {
                    public void run() {
                        workspace2.moveToDefaultScreen(true);
                    }
                });
            }
        }
    }

    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (this.mStageManager != null) {
            this.mStageManager.onRestoreInstanceState(state);
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mStageManager != null) {
            this.mStageManager.onSaveInstanceState(outState);
        }
        outState.putSerializable(RUNTIME_STATE_VIEW_IDS, this.mItemIdToViewId);
    }

    public void onDestroy() {
        if (LauncherFeature.supportGSAPreWarming() && this.mPreWarmingClient != null) {
            this.mPreWarmingClient.onDestroy();
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.clearCheckedApps();
            this.mMultiSelectManager.clearUninstallApplist();
        }
        if (this.mQuickOptionManager != null) {
            this.mQuickOptionManager.onDestroy();
            this.mQuickOptionManager = null;
        }
        this.mHomeController.getBindController().removeAdvanceMessage();
        this.mHomeController.getWorkspace().removeCallbacks(this.mBuildLayersRunnable);
        this.mResumeCallbacksHandler.removeCallbacksAndMessages(RESUME_CALLBACKS_TOKEN);
        LauncherAppState app = LauncherAppState.getInstance();
        if (this.mModel.isCurrentCallbacks(this)) {
            this.mModel.stopLoader();
            app.setLauncher(null);
            this.mModel.getHomeLoader().unRegisterCallbacks();
        }
        this.mHomeController.exitResizeState(false);
        this.mStageManager.onDestroy();
        this.mStageManager = null;
        if (this.mTrayManager != null) {
            this.mTrayManager.onDestroy();
            this.mTrayManager = null;
        }
        TextKeyListener.getInstance().release();
        this.mHomeController = null;
        this.mDragMgr = null;
        if (LauncherFeature.supportFolderLock() && this.mFolderLock != null) {
            this.mFolderLock.onDestroy();
        }
        if (LauncherFeature.isSSecureSupported() && this.mSSecureUpdater != null) {
            this.mSSecureUpdater.onDestroy();
        }
        BlurUtils.resetBlur();
        this.mPageTransitionManager = null;
        LauncherAnimUtils.onDestroyActivity();
        unregisterReceiversAndObservers();
        if (this.mRotationPrefChangeListener != null) {
            this.mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this.mRotationPrefChangeListener);
        }
    }

    private boolean acceptFilter() {
        return !((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).isFullscreenMode();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyUp(keyCode, event);
        if (getResources().getConfiguration().keyboard != 3) {
            return handled;
        }
        if (keyCode != 18 && keyCode != 17) {
            return handled;
        }
        if (this.mStageManager.getTopStage() != null && this.mStageManager.getTopStage().searchBarHasFocus()) {
            return false;
        }
        if (this.mShortPress) {
            Uri uri;
            if (keyCode == 18) {
                uri = Uri.parse("tel:" + Uri.encode("#"));
            } else {
                uri = Uri.parse("tel:" + Uri.encode("*"));
            }
            Intent myIntentDial = new Intent("android.intent.action.DIAL", uri);
            myIntentDial.putExtra("isPoundKey", true);
            myIntentDial.putExtra("firstKeycode", keyCode);
            myIntentDial.putExtra("isKeyTone", true);
            startActivity(myIntentDial);
            clearTypedText();
        }
        this.mShortPress = true;
        this.mLongPress = false;
        return handled;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int uniChar = event.getUnicodeChar();
        boolean handled = super.onKeyDown(keyCode, event);
        boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (getResources().getConfiguration().keyboard == 3) {
            if (keyCode == 18 || keyCode == 17) {
                event.startTracking();
                if (this.mLongPress) {
                    this.mShortPress = false;
                } else {
                    this.mShortPress = true;
                    this.mLongPress = false;
                }
                return true;
            } else if (keyCode >= 7 && keyCode <= 18) {
                Intent myIntentDial = new Intent("android.intent.action.DIAL", Uri.parse("tel:"));
                myIntentDial.putExtra("firstKeycode", keyCode);
                myIntentDial.putExtra("isKeyTone", true);
                startActivity(myIntentDial);
                clearTypedText();
                return handled;
            } else if (keyCode == 27) {
                try {
                    Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
                    // TODO: Fix intent flags
//                    intent.setFlags(67108864);
//                    intent.addFlags(268435456);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                }
            } else if (keyCode == 67) {
                if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    return true;
                }
                if (!(this.mHomeController == null || getOpenFolderView() != null || this.mHomeController.isModalState())) {
                    showAppsView(true, false);
                }
            }
        }
        if (!handled && acceptFilter() && isKeyNotWhitespace && TextKeyListener.getInstance().onKeyDown(this.mHomeController.getContainerView(), this.mDefaultKeySsb, keyCode, event) && this.mDefaultKeySsb != null && this.mDefaultKeySsb.length() > 0) {
            return onSearchRequested();
        }
        if (keyCode == 82 && event.isLongPress()) {
            return true;
        }
        if (keyCode != PointerIconCompat.TYPE_HAND || LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            return handled;
        }
        if (isHomeStage()) {
            this.mHomeController.enterNormalState(false, false);
        } else if (isFolderStage()) {
            this.mStageManager.finishStage(5, null);
            if (getSecondTopStageMode() != 2) {
                this.mStageManager.startStage(2, null);
            }
        } else if (!isAppsStage()) {
            this.mStageManager.finishAllStage(null);
            this.mHomeController.enterNormalState(false, false);
        }
        if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
            this.mStageManager.onBackPressed();
        }
        if (LauncherFeature.supportQuickOption() && this.mDragMgr != null) {
            this.mDragMgr.removeQuickOptionView();
        }
        this.mStageManager.startStage(2, null);
        return true;
    }

    private String getTypedText() {
        return this.mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        this.mDefaultKeySsb.clear();
        this.mDefaultKeySsb.clearSpans();
        Selection.setSelection(this.mDefaultKeySsb, 0);
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (getResources().getConfiguration().keyboard == 3) {
            if (keyCode == 18) {
                this.mShortPress = false;
                this.mLongPress = true;
                mannerModeSet();
                return false;
            } else if (keyCode == 17) {
                this.mShortPress = false;
                this.mLongPress = true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean z = false;
        if (event.getAction() != 0) {
            if (event.getAction() == 1) {
                switch (event.getKeyCode()) {
                    case 3:
                        return true;
                    default:
                        break;
                }
            }
        }
        switch (event.getKeyCode()) {
            case 3:
                return true;
            case 25:
                if (Utilities.isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                    dumpState();
                    return true;
                }
                break;
        }
        if (this.mDragMgr != null && this.mDragMgr.getQuickOptionView() != null && this.mDragMgr.getQuickOptionView().isLeftRightKeycodeInGlobalOption(event)) {
            return false;
        }
        if (this.mStageManager.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)) {
            z = true;
        }
        return z;
    }

    public void onBackPressed() {
        if (this.mDragMgr != null) {
            if (this.mDragMgr.isDragging()) {
                this.mDragMgr.cancelDrag();
                return;
            } else if (LauncherFeature.supportQuickOption() && this.mDragMgr.isQuickOptionShowing()) {
                this.mDragMgr.removeQuickOptionView("1");
                return;
            }
        }
        this.mStageManager.onBackPressed();
    }

    public void onClick(View v) {
        if (v.getWindowToken() != null) {
            this.mStageManager.onClick(v);
        }
    }

    public void startAppShortcutOrInfoActivity(View v) {
        IconInfo tag = (IconInfo)v.getTag();
        if (tag instanceof IconInfo) {
            IconInfo shortcut = tag;
            SALogging.getInstance().insertItemLaunchLog(shortcut, this);
            Intent intent = (Intent) shortcut.intent.clone();
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
            if (LauncherFeature.supportFolderLock() && this.mFolderLock != null && this.mFolderLock.isFolderLockEnabled() && this.mFolderLock.isLockedApp(shortcut)) {
                try {
                    SALogging.getInstance().insertLockedItemLaunchLog(shortcut, this);
                } catch (Exception e) {
                    Log.d(TAG, " can not SALogging locked app ");
                }
            }
            if (tag instanceof LauncherPairAppsInfo) {
                Utilities.startActivitySafelyForPair(getApplicationContext(), this, tag);
            } else {
                Utilities.startActivitySafely(this, v, intent, tag);
            }
            this.mStats.recordLaunch(v, intent, shortcut);
            GSIMLogging.getInstance().runAllStatusLogging();
            return;
        }
        throw new IllegalArgumentException("Input must be a Shortcut or AppInfo");
    }

    public void onClickFolderIcon(View v) {
        if (v instanceof FolderIconView) {
            FolderIconView folderIconView = (FolderIconView) v;
            FolderInfo info = folderIconView.getFolderInfo();
            FolderView openFolder = this.mHomeController.getFolderForTag(info);
            if (info.opened && openFolder == null) {
                Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: " + info.screenId + " (" + info.cellX + ", " + info.cellY + ")");
                info.opened = false;
            }
            if (LauncherFeature.supportFolderLock() && this.mFolderLock != null && this.mFolderLock.isFolderLockEnabled() && info.isLocked() && !info.isLockedFolderOpenedOnce()) {
                try {
                    this.mFolderLock.setBackupInfo(folderIconView);
                    this.mFolderLock.openLockedFolder(info);
                    SALogging.getInstance().insertLockedItemLaunchLog(info, this);
                    return;
                } catch (Exception e) {
                    Log.d(TAG, " can not open that locked folder ");
                    return;
                }
            } else if (!info.opened && !folderIconView.getFolderView().isDestroyed()) {
                closeFolder();
                openFolder(folderIconView);
                return;
            } else if (openFolder != null) {
                int folderScreen = this.mHomeController.getWorkspace().getPageForView(openFolder);
                closeFolderStage();
                if (folderScreen != this.mHomeController.getWorkspace().getCurrentPage()) {
                    closeFolder();
                    openFolder(folderIconView);
                    return;
                }
                return;
            } else {
                return;
            }
        }
        throw new IllegalArgumentException("Input must be a FolderIcon");
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        this.mStageManager.onStartForResult(requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        this.mStageManager.onStartForResult(requestCode);
        try {
            super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        if (initialQuery == null) {
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString(Stats.EXTRA_SOURCE, "launcher-search");
        }
        ((SearchManager) getSystemService(Context.SEARCH_SERVICE)).startSearch(initialQuery, selectInitialQuery, getComponentName(), appSearchData, globalSearch);
        this.mHomeController.enterNormalState(true);
    }

    public void startSearchFromAllApps(View v, Intent searchIntent) {
        Utilities.startActivitySafely(this, v, searchIntent, null);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return this.mStageManager.onPrepareOptionMenu(menu);
    }

    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        return true;
    }

    private void restoreState(Bundle savedState) {
        if (savedState != null) {
            this.mStageManager.restoreState(savedState);
            this.mItemIdToViewId = (HashMap) savedState.getSerializable(RUNTIME_STATE_VIEW_IDS);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.mStageManager.onActivityResult(requestCode, resultCode, data);
        if (LauncherFeature.supportFolderLock() && this.mFolderLock != null) {
            this.mFolderLock.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onSettingsChanged(String settings, boolean value) {
        Log.d(TAG, "onSettingsChanged : " + settings);
        if (Utilities.HOMESCREEN_MODE_PREFERENCE_KEY.equals(settings)) {
            this.mHomeController.changeHomeScreenMode(settings, value);
        } else if (Utilities.APPS_BUTTON_SETTING_PREFERENCE_KEY.equals(settings)) {
            this.mHomeController.setAppsButtonEnabled(value);
        }
    }

    public void onAppWidgetHostReset() {
        if (this.mHomeController != null && this.mModel.isCurrentCallbacks(this)) {
            LauncherAppWidgetHost appWidgetHost = this.mHomeController.getAppWidgetHost();
            if (appWidgetHost != null) {
                appWidgetHost.startListening();
            }
        }
    }

    public LayoutInflater getInflater() {
        return this.mInflater;
    }

    public int getViewIdForItem(ItemInfo info) {
        int itemId = (int) info.id;
        if (this.mItemIdToViewId.containsKey(itemId)) {
            return this.mItemIdToViewId.get(itemId);
        }
        int viewId = View.generateViewId();
        this.mItemIdToViewId.put(itemId, viewId);
        return viewId;
    }

    private void setIndicatorTransparency() {
        getWindow().clearFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        int flags = 1024;
        if (this.mStageManager.getTopStage().supportStatusBar()) {
            flags = 1024 | 2048;
        }
        getWindow().addFlags(flags);
    }

    public void onWindowVisibilityChanged(int visibility) {
        this.mVisible = visibility == 0;
        Log.i(TAG, "onWindowVisibilityChanged : " + this.mVisible);
        this.mHomeBindController.updateAutoAdvanceState();
        if (this.mVisible) {
            if (!this.mHomeBindController.isWorkspaceLoading()) {
                this.mHomeController.getContainerView().getViewTreeObserver().addOnDrawListener(new OnDrawListener() {
                    private boolean mStarted = false;

                    public void onDraw() {
                        if (!this.mStarted && Launcher.this.mHomeController != null) {
                            this.mStarted = true;
                            final View view = Launcher.this.mHomeController.getContainerView();
                            view.postDelayed(Launcher.this.mBuildLayersRunnable, 500);
                            //final AnonymousClass6 listener = this;
                            final android.view.ViewTreeObserver.OnDrawListener listener = this;
                            view.post(new Runnable() {
                                public void run() {
                                    if (view.getViewTreeObserver() != null) {
                                        view.getViewTreeObserver().removeOnDrawListener(listener);
                                    }
                                }
                            });
                        }
                    }
                });
            }
            clearTypedText();
        }
    }

    public boolean startApplicationUninstallActivity(ComponentName componentName, int flags, UserHandleCompat user, boolean showToast) {
        if ((flags & 1) != 0) {
            Intent intent = new Intent("android.intent.action.DELETE", Uri.fromParts("package", componentName.getPackageName(), componentName.getClassName()));
            // TODO: Fix intent flags
            // intent.setFlags(276856832);
            if (user != null) {
                user.addToIntent(intent, "android.intent.extra.USER");
            }
            startActivity(intent);
            return true;
        } else if (!showToast) {
            return false;
        } else {
            Toast.makeText(this, String.format(getString(R.string.multi_select_uninstall_app_notice_one), ""), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= 20) {
            SQLiteDatabase.releaseMemory();
        }
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        if (!this.mStageManager.dispatchPopulateAccessibilityEvent(event)) {
            List<CharSequence> text = event.getText();
            text.clear();
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    public boolean isSafeModeEnabled() {
        return this.mIsSafeModeEnabled;
    }

    public Bundle getSavedState() {
        return this.mSavedState;
    }

    public void setSavedState(Bundle option) {
        this.mSavedState = option;
    }

    public boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if ((this.mVisible || !this.mPaused) && (this.mRunResumeCallbackInSchedule.runInSchedule || this.mBindOnResumeCallbacks.isEmpty())) {
            return false;
        }
        if (deletePreviousRunnables) {
            this.mBindOnResumeCallbacks.remove(run);
        }
        this.mBindOnResumeCallbacks.add(run);
        return true;
    }

    public boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public boolean waitUntilResumeForHotseat(Runnable run) {
        if (!this.mPaused) {
            return false;
        }
        this.mHotseatOnResumeCallback = run;
        return true;
    }

    public void addOnResumeCallback(Runnable run) {
        this.mOnResumeCallbacks.add(run);
    }

    public boolean setLoadOnResume() {
        if (!this.mPaused) {
            return false;
        }
        this.mOnResumeNeedsLoad = true;
        return true;
    }

    public void recreateLauncher() {
        sIsRecreateModeChange = true;
        recreate();
    }

    public boolean isHomeNormal() {
        return (this.mHomeController == null || this.mHomeController.isModalState()) ? false : true;
    }

    public void relayoutLauncher() {
        this.mDeviceProfile.layoutGrid(this);
    }

    public PageTransitionManager getPageTransitionManager() {
        return this.mPageTransitionManager;
    }

    public ArrayList<Runnable> getBindOnResumeCallbacks() {
        return this.mBindOnResumeCallbacks;
    }

    public SharedPreferences getSharedPrefs() {
        return this.mSharedPrefs;
    }

    public DeviceProfile getDeviceProfile() {
        return this.mDeviceProfile;
    }

    public DragManager getDragMgr() {
        return this.mDragMgr;
    }

    public DragLayer getDragLayer() {
        return this.mDragLayer;
    }

    public HomeController getHomeController() {
        return this.mHomeController;
    }

    public StageManager getStageManager() {
        return this.mStageManager;
    }

    public QuickOptionManager getQuickOptionManager() {
        return this.mQuickOptionManager;
    }

    public boolean isQuickOptionShowing() {
        return this.mDragMgr != null && this.mDragMgr.isQuickOptionShowing();
    }

    public int getTopStageMode() {
        Stage topStage = this.mStageManager == null ? null : this.mStageManager.getTopStage();
        return topStage == null ? 0 : topStage.getMode();
    }

    public int getSecondTopStageMode() {
        Stage secondTopStage = this.mStageManager == null ? null : this.mStageManager.getSecondTopStage();
        return secondTopStage == null ? 0 : secondTopStage.getMode();
    }

    public boolean isRunningAnimation() {
        return this.mStageManager.isRunningAnimation();
    }

    public boolean getVisible() {
        return this.mVisible;
    }

    public boolean isDraggingEnabled() {
        return !this.mHomeBindController.isWorkspaceLoading();
    }

    public void beginDragShared(View child, DragSource source, boolean allowQuickOption, boolean fromEmptyCell) {
        if (child.getTag() instanceof ItemInfo) {
            Drawable dragOutline;
            int topDelta = 0;
            if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
                if (this.mMultiSelectManager.isShowingHelpDialog()) {
                    this.mMultiSelectManager.hideHelpDialog(true);
                }
                if ((LauncherFeature.supportFolderSelect() && (child instanceof FolderIconView)) || (child instanceof IconView)) {
                    String format;
                    ItemInfo info = (ItemInfo) child.getTag();
                    if (info.getChecked()) {
                        ((IconView) child).updateCountBadge(true, 0);
                        ((IconView) child).getCheckBox().setChecked(false);
                    } else {
                        ((IconView) child).updateCountBadge(true, 1);
                    }
                    ((IconView) child).updateCheckBox(false, false);
                    topDelta = ((IconView) child).getIconVew().getTop();
                    Talk talk = Talk.INSTANCE;
                    String string = getString(R.string.tts_holding_items);
                    Object[] objArr = new Object[2];
                    if (child instanceof FolderIconView) {
                        format = String.format(getResources().getString(R.string.folder_name_format), info.title);
                    } else {
                        format = info.title.toString();
                    }
                    objArr[0] = format;
                    objArr[1] = this.mMultiSelectManager.getCheckedAppCount();
                    talk.postSay(String.format(string, objArr));
                }
            }
            child.clearFocus();
            child.setPressed(false);
            if (child instanceof LauncherAppWidgetHostView) {
                int[] size = this.mHomeController.getWorkspace().estimateItemSize((ItemInfo) child.getTag());
                dragOutline = DragViewHelper.createWidgetDragOutline(this, size[0], size[1]);
            } else {
                dragOutline = DragViewHelper.createDragOutline(this, child, source.getOutlineColor());
            }
            AtomicInteger atomicInteger = new AtomicInteger(6);
            boolean z = this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode();
            Bitmap b = DragViewHelper.createDragBitmap(child, atomicInteger, z);
            int bmpWidth = b.getWidth();
            int bmpHeight = b.getHeight();
            int[] tempPt = new int[2];
            float scale = this.mDragLayer.getLocationInDragLayer(child, tempPt);
            if (((child instanceof IconView) && this.mMultiSelectManager == null) || !this.mMultiSelectManager.isMultiSelectMode()) {
                scale *= 0.9f;
            }
            int dragLayerX = Math.round(((float) tempPt[0]) - ((((float) bmpWidth) - (((float) child.getWidth()) * scale)) / 2.0f));
            int dragLayerY = Math.round((((float) tempPt[1]) - ((((float) bmpHeight) - (((float) bmpHeight) * scale)) / 2.0f)) - (((float) atomicInteger.get()) / 2.0f));
            Point dragVisualizeOffset = null;
            Rect dragRect = null;
            Rect quickOptionAnchorRect = null;
            if (child instanceof IconView) {
                IconView iconView = (IconView) child;
                LayoutParams lp = (LayoutParams) iconView.getIconVew().getLayoutParams();
                int iconSize = iconView.getIconSize();
                int top = child.getPaddingTop() + lp.topMargin;
                int left = (bmpWidth - iconSize) / 2;
                int right = left + iconSize;
                int bottom = top + iconSize;
                if (iconView.isLandscape() && !LauncherFeature.isTablet()) {
                    GridIconInfo gridIconInfo = iconView.getIconInfo();
                    quickOptionAnchorRect = new Rect();
                    child.getGlobalVisibleRect(quickOptionAnchorRect);
                    dragLayerX = tempPt[0] + ((int) (((float) gridIconInfo.getIconStartPadding()) * scale));
                    quickOptionAnchorRect = new Rect(quickOptionAnchorRect.left + gridIconInfo.getIconStartPadding(), quickOptionAnchorRect.top, quickOptionAnchorRect.left + iconView.getIconSize(), quickOptionAnchorRect.bottom);
                }
                dragLayerY += top;
                dragVisualizeOffset = new Point((-atomicInteger.get()) / 2, atomicInteger.get() / 2);
                dragRect = new Rect(left, top, right, bottom);
            }
            if (LauncherFeature.supportQuickOption() && allowQuickOption) {
                Rect r;
                if (quickOptionAnchorRect != null) {
                    r = quickOptionAnchorRect;
                } else {
                    r = new Rect();
                    child.getGlobalVisibleRect(r);
                }
                this.mQuickOptionManager.setAnchorRect(r);
                this.mQuickOptionManager.setAnchorView(child);
            }
            DragView dv = this.mDragMgr.startDrag(child, b, dragLayerX, dragLayerY - topDelta, source, child.getTag(), 0, dragVisualizeOffset, dragRect, scale, dragOutline, allowQuickOption, fromEmptyCell);
            dv.setIntrinsicIconSize(source.getIntrinsicIconSize());
            dv.setTopDelta(topDelta);
            b.recycle();
            return;
        }
        throw new IllegalStateException("Drag started with a view that has no tag set. This will cause a crash (issue 11627249) down the line. View: " + child + "  tag: " + child.getTag());
    }

    public void beginDragFromWidget(View v, Bitmap bmp, DragSource source, Object dragInfo, Rect viewImageBounds, float initialDragViewScale) {
        Drawable dragOutline;
        ItemInfo info = (ItemInfo) dragInfo;
        int[] size = this.mHomeController.getWorkspace().estimateItemSize(info);
        if (info instanceof PendingAddShortcutInfo) {
            dragOutline = DragViewHelper.createDragOutline(this, bmp);
        } else {
            dragOutline = DragViewHelper.createWidgetDragOutline(this, size[0], size[1]);
        }
        this.mDragMgr.startDrag(v, bmp, source, dragInfo, viewImageBounds, 1, initialDragViewScale, dragOutline, true);
    }

    public void beginDragFromQuickOptionPopup(View v, Bitmap b, DragSource source, Object dragInfo, Rect bounds, float initialDragViewScale) {
        this.mDragMgr.startDrag(v, b, source, dragInfo, bounds, 1, initialDragViewScale, DragViewHelper.createDeepShortcutDragOutline(this, b), true);
        this.mStageManager.getStage(1).onStageEnterDragState(true);
    }

    public void beginDragFromPinItem(View v, Bitmap bmp, DragSource source, Object dragInfo, Rect viewImageBounds, float initialDragViewScale, Point point) {
        Drawable dragOutline;
        ItemInfo info = (ItemInfo) dragInfo;
        int[] size = this.mHomeController.getWorkspace().estimateItemSize(info);
        if ((info instanceof PendingAddPinShortcutInfo) || (info instanceof IconInfo)) {
            dragOutline = DragViewHelper.createDeepShortcutDragOutline(this, bmp);
        } else {
            dragOutline = DragViewHelper.createWidgetDragOutline(this, size[0], size[1]);
        }
        this.mDragMgr.startDrag(v, bmp, source, dragInfo, viewImageBounds, 1, initialDragViewScale, dragOutline, true, point);
        this.mStageManager.getStage(1).onStageEnterDragState(false);
    }

    public void openFolder(FolderIconView folderIconView) {
        if (this.mStageManager.getTopStage().getMode() == 5) {
            if (!folderIconView.getFolderView().equals(this.mStageManager.getTopStage().getContainerView())) {
                this.mStageManager.finishStage(5, null);
            } else {
                return;
            }
        } else if (this.mStageManager.getTopStage().getMode() == 6) {
            this.mStageManager.finishStage(6, null);
            this.mStageManager.finishStage(5, null);
        }
        StageEntry data = new StageEntry();
        data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folderIconView);
        this.mStageManager.startStage(5, data);
    }

    private void setWhichTransitionEffect(int whichTransitionEffect) {
        if (this.mPageTransitionManager != null) {
            this.mPageTransitionManager.setCurrentTransitionEffect(whichTransitionEffect);
        }
    }

    public void closeFolder() {
        if (getOpenFolderView() != null) {
            closeFolderStage();
        }
    }

    public void closeFolderStage() {
        this.mStageManager.finishStage(5, null);
    }

    public FolderView getOpenFolderView() {
        int count = this.mDragLayer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.mDragLayer.getChildAt(i);
            if (child instanceof FolderView) {
                FolderView folder = (FolderView) child;
                if (this.mStageManager.getTopStage() == null) {
                    return null;
                }
                if (folder.getInfo().opened && this.mStageManager.getTopStage().getMode() == 5) {
                    return folder;
                }
            }
        }
        return null;
    }

    public boolean finishStageOnTouchOutSide() {
        Stage stage = this.mStageManager.getTopStage();
        if (stage == null || !stage.finishOnTouchOutSide()) {
            return false;
        }
        this.mStageManager.onBackPressed();
        return true;
    }

    private void changeEasyModeIfNecessary(boolean needReCreate) {
        boolean isEasyMode;
        if (System.getInt(getContentResolver(), "easy_mode_switch", 1) == 0) {
            isEasyMode = true;
        } else {
            isEasyMode = false;
        }
        sNeedCheckEasyMode = false;
        if (isEasyMode != LauncherAppState.getInstance().isEasyModeEnabled()) {
            Log.d(TAG, "changeEasyMode : " + isEasyMode);
            this.mChangeMode = true;
            if (FavoritesProvider.getInstance().switchTable(2, isEasyMode)) {
                LauncherAppState.getInstance().writeEasyModeEnabled(isEasyMode);
                LauncherAppState.getInstance().getModel().resetLoadedState(true, true);
                if (needReCreate) {
                    recreateLauncher();
                }
            }
        }
    }

    private void changeHomeModeIfNecessary() {
        SharedPreferences prefs = getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs.contains(Utilities.HOMESCREEN_MODE_PREFERENCE_KEY)) {
            boolean HomeOnlySettingValue = prefs.getBoolean(Utilities.HOMESCREEN_MODE_PREFERENCE_KEY, false);
            if (HomeOnlySettingValue != LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                Log.d(TAG, "change home mode setting value : " + HomeOnlySettingValue + " appState value : " + LauncherAppState.getInstance().isHomeOnlyModeEnabled());
                if (FavoritesProvider.getInstance().switchTable(1, HomeOnlySettingValue)) {
                    LauncherAppState.getInstance().writeHomeOnlyModeEnabled(HomeOnlySettingValue);
                    LauncherAppState.getInstance().getModel().resetLoadedState(true, true);
                }
            }
        }
    }

    private void changeColorForBg() {
        if (this.mStageManager != null) {
            this.mStageManager.onChangeColorForBg(WhiteBgManager.isWhiteBg());
        }
    }

    public void changeStatusBarColor(boolean whiteBg) {
        if (whiteBg && isThereDimEffectForApps()) {
            Log.w(TAG, "changeStatusBarColor : Now dont support WhiteBg, There is dim effect");
            return;
        }
        Log.d(TAG, "changeStatusBarColor whiteBg = " + whiteBg);
        if (whiteBg) {
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | 8192);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & -8193);
        }
    }

    public void changeNavigationBarColor(boolean whiteBg) {
        if (whiteBg && isThereDimEffectForApps()) {
            Log.w(TAG, "changeNavigationBarColor : Now dont support WhiteBg, There is dim effect");
        } else if (VERSION.SDK_INT < 26) {
            try {
                // TODO: Samsung specific code
                //getWindow().getAttributes().semSetNavigationBarIconColor(ContextCompat.getColor(this, whiteBg ? R.color.text_color_dark : R.color.text_color));
                if (LauncherFeature.supportNavigationBar() && this.mAttached) {
                    getWindowManager().updateViewLayout(getWindow().getDecorView(), getWindow().getAttributes());
                }
            } catch (NoSuchMethodError e) {
                Log.e(TAG, "NoSuchMethodError occur when change navigation color.", e);
            }
        } else {
            if (whiteBg) {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | 16);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & -17);
            }
            Log.d(TAG, "changeNavigationBarColor whiteBg = " + whiteBg + " FLAG : " + (getWindow().getDecorView().getSystemUiVisibility() & 16));
        }
    }

    private void closeSystemDialogs() {
        getWindow().closeAllPanels();
        this.mHomeController.setWaitingForResult(false);
    }

    private void closeDialogIfNeeded() {
        FragmentManager manager = getFragmentManager();
        if (DisableAppConfirmationDialog.isActive(manager)) {
            DisableAppConfirmationDialog.dismissIfNeeded(this, getFragmentManager());
        }
        if (AddItemOnLastPageDialog.isActive(manager)) {
            AddItemOnLastPageDialog.dismiss(manager);
        }
    }

    private void dumpState() {
        Log.d(TAG, "BEGIN launcher3 dump state for launcher " + this);
        Log.d(TAG, "mSavedState=" + this.mSavedState);
        Log.d(TAG, "mWorkspaceLoading=" + this.mHomeBindController.isWorkspaceLoading());
        Log.d(TAG, "mRestoring=" + this.mHomeController.isRestoring());
        Log.d(TAG, "mWaitingForResult=" + this.mHomeController.isWaitingForResult());
        Log.d(TAG, "sFolders.size=" + HomeBindController.sFolders.size());
        this.mModel.dumpState();
        Log.d(TAG, "END launcher3 dump state");
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        synchronized (sDumpLogs) {
            writer.println(" ");
            writer.println("Debug logs: ");
            for (int i = 0; i < sDumpLogs.size(); i++) {
                writer.println("  " + ((String) sDumpLogs.get(i)));
            }
        }
    }

    public static void addDumpLog(String tag, String log, boolean debugLog) {
        addDumpLog(tag, log, null, debugLog);
    }

    public static void addDumpLog(String tag, String log, Exception e, boolean debugLog) {
        if (!debugLog) {
            return;
        }
        if (e != null) {
            Log.d(tag, log, e);
        } else {
            Log.d(tag, log);
        }
    }

    public void dumpLogsToLocalData() {
    }

    public void updateZeroPage(final int op) {
        if (!waitUntilResume(new Runnable() {
            public void run() {
                Launcher.this.updateZeroPage(op);
            }
        }) && ZeroPageController.supportVirtualScreen() && this.mHomeController != null) {
            this.mHomeController.updateZeroPage(op);
        }
    }

    public boolean isHomeStage() {
        return getTopStageMode() == 1 || this.mOnResumeState == 1;
    }

    public boolean isAppsStage() {
        return getTopStageMode() == 2 || this.mOnResumeState == 2;
    }

    public boolean isFolderStage() {
        return getTopStageMode() == 5 || this.mOnResumeState == 5;
    }

    public void showAppsView(boolean animated, boolean resetToTop) {
        Log.d(TAG, "showAppsView:" + animated + " resetToTop:" + resetToTop);
        showAppsOrWidgets(2, animated, false);
    }

    public void showWidgetsView(boolean animated, boolean resetToTop) {
        Log.d(TAG, "showWidgetsView:" + animated + " resetToTop:" + resetToTop);
        showAppsOrWidgets(3, animated, false);
    }

    public boolean showAppsOrWidgets(int toMode, boolean animated, boolean fromSetting) {
        StageEntry data;
        if (getTopStageMode() != toMode) {
            data = new StageEntry();
            data.enableAnimation = animated;
            if (fromSetting) {
                if (toMode == 2) {
                    data.setInternalStateTo(5);
                } else if (toMode == 3) {
                    data.putExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(true));
                }
            }
            this.mStageManager.startStage(toMode, data);
            this.mHomeBindController.setUserPresent(false);
            this.mHomeBindController.updateAutoAdvanceState();
            getWindow().getDecorView().sendAccessibilityEvent(32);
            return true;
        } else if (fromSetting && toMode == 3) {
            data = new StageEntry();
            data.putExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(fromSetting));
            this.mStageManager.deliverDataWithOutStageChange(toMode, data);
            return false;
        } else if (!fromSetting || toMode != 2) {
            return false;
        } else {
            data = new StageEntry();
            data.setInternalStateTo(5);
            this.mStageManager.deliverDataWithOutStageChange(toMode, data);
            return false;
        }
    }

    public LauncherModel getLauncherModel() {
        return this.mModel;
    }

    public void enableVoiceSearch(SearchView searchView) {
        if (searchView != null) {
            try {
                searchView.setSearchableInfo(((SearchManager) getSystemService(SEARCH_SERVICE)).getSearchableInfo(new ComponentName(getPackageName(), getClass().getName())));
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException:" + e.toString());
            }
        }
    }

    public boolean hasVoiceSearch() {
        SearchableInfo searchableInfo = null;
        try {
            searchableInfo = ((SearchManager) getSystemService(SEARCH_SERVICE)).getSearchableInfo(new ComponentName(getPackageName(), getClass().getName()));
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException:" + e.toString());
        }
        if (searchableInfo != null && searchableInfo.getVoiceSearchEnabled()) {
            Intent micIntent = null;
            if (searchableInfo.getVoiceSearchLaunchRecognizer()) {
                boolean supportGoogleService = true;
                // TODO: Samsung specific code
//                if (SemCscFeature.getInstance().getBoolean("CscFeature_Common_DisableGoogle", false)) {
//                    supportGoogleService = false;
//                } else {
//                    supportGoogleService = true;
//                }
                if (supportGoogleService) {
                    micIntent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
                    micIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
                } else {
                    micIntent = new Intent("samsung.svoiceime.action.RECOGNIZE_SPEECH");
                    if (Locale.getDefault() != null) {
                        micIntent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
                    }
                }
            }
            if (micIntent != null) {
                if (getPackageManager().resolveActivity(micIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public Stats getStats() {
        return this.mStats;
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public TrayManager getTrayManager() {
        return this.mTrayManager;
    }

    public void updateItemInfo(ArrayList<ItemInfo> hideItems, ArrayList<ItemInfo> addItems) {
        this.mModel.updateItemInfo(hideItems, addItems, false);
    }

    public void updateItemInfo(ArrayList<ItemInfo> hideItems, ArrayList<ItemInfo> addItems, boolean isGameApp) {
        this.mModel.updateItemInfo(hideItems, addItems, isGameApp);
    }

    public boolean isSkipAnim() {
        return this.mSkipAnim;
    }

    private void setLiveIconAlarm() {
        Context context = getApplicationContext();
        LiveIconManager.setCalendarAlarm(context);
        LiveIconManager.setClockAlarm(context);
    }

    public void onChangeSelectMode(boolean enter, boolean animated) {
        this.mMultiSelectManager.onChangeSelectMode(enter, animated);
    }

    public MultiSelectManager getMultiSelectManager() {
        return this.mMultiSelectManager;
    }

    public void startHomeSettingActivity() {
        startHomeSettingActivity(false);
    }

    public void startHomeSettingActivity(boolean fromGlobalSettings) {
        Log.d(TAG, "launch setting Activity.");
        Intent homeScreenSetting = new Intent(this, SettingsActivity.class);
        // TODO: Fix intent flags
        // homeScreenSetting.addFlags(32768);
        startActivity(homeScreenSetting);
        if (fromGlobalSettings) {
            overridePendingTransition(R.anim.settings_activity_in, R.anim.global_settings_out);
        }
    }

    public void startAddItemActivity(PinItemRequestCompat pinItemRequestCompat, boolean isFromWorkSpace) {
        Log.d(TAG, "launch AddItemActivity.");
        Intent addItemActivity = new Intent(this, AddItemActivity.class);
        // TODO: Fix intent flags
        // addItemActivity.addFlags(32768);
        addItemActivity.putExtra(PinItemRequestCompat.EXTRA_PIN_ITEM_REQUEST, pinItemRequestCompat);
        addItemActivity.putExtra(PinItemRequestCompat.EXTRA_IS_FROM_LAUNCHER, true);
        addItemActivity.putExtra(PinItemRequestCompat.EXTRA_IS_FROM_WORKSPACE, isFromWorkSpace);
        startActivity(addItemActivity);
    }

    public String getSearchedApp() {
        return this.mSearchedApp;
    }

    public UserHandle getSearchedAppUser() {
        return this.mSearchedAppUser;
    }

    public void setSearchedApp(String SearchedApp) {
        this.mSearchedApp = SearchedApp;
    }

    public void setSearchedAppUser(UserHandle user) {
        this.mSearchedAppUser = user;
    }

    private void setupWallpaperScroller() {
        boolean z;
        Log.d(TAG, "setupWallpaperScroller");
        if (System.getInt(getContentResolver(), SETTINGS_WALLPAPER_TILT_STATUS, 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mWallpaperTiltSettingEnabled = z;
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if (this.mSensorManager.getDefaultSensor(GyroForShadow.SENSOR_TYPE_INTERRUPT_GYROSCOPE) == null) {
            Log.d(TAG, "Launcher::onCreate() - gyroSensor not support");
            LauncherFeature.setSupportWallpaperTilt(false);
            return;
        }
        if (this.mTiltWallpaperScroller == null) {
            this.mTiltWallpaperScroller = new WallpaperScroller(this);
        }
        if (this.mWallpaperTiltSettingEnabled) {
            GyroForShadow.initialize(this, this);
            if (this.mTiltWallpaperScroller != null) {
                this.mTiltWallpaperScroller.start(true);
            }
            View v = getWindow().peekDecorView();
            if (v != null) {
                this.mWindowToken = v.getWindowToken();
            }
            if (this.mWindowToken == null || this.mTiltWallpaperScroller == null) {
                Log.d(TAG, "WallpaperScroller - mWindowToken is null");
            } else {
                this.mTiltWallpaperScroller.setWindowToken(this.mWindowToken);
            }
            this.mTiltWallpaperScroller.resume(true);
        }
    }

    private void shutdownWallpaperScroller() {
        if (this.mTiltWallpaperScroller != null && this.mTiltWallpaperScroller.isRunning()) {
            this.mTiltWallpaperScroller.pause();
            new Handler().post(new Runnable() {
                public void run() {
                    if (Launcher.this.mPaused) {
                        Launcher.this.mTiltWallpaperScroller.shutdown();
                        Launcher.this.resetWallpaperOffsets();
                    }
                }
            });
        }
    }

    private void setWallpaperOffsetToCenter() {
        if (Utilities.ATLEAST_N_MR1 && !this.mWallpaperTiltSettingEnabled) {
            Log.d(TAG, "set wallpaper offset to center");
            if (this.mWindowToken == null) {
                View v = getWindow().peekDecorView();
                if (v != null) {
                    this.mWindowToken = v.getWindowToken();
                }
            }
            resetWallpaperOffsets();
        }
    }

    private void resetWallpaperOffsets() {
        if (this.mWindowToken != null) {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            if (wallpaperManager != null) {
                try {
                    Log.d(TAG, "resetWallpaperOffsets");
                    wallpaperManager.setWallpaperOffsets(this.mWindowToken, 0.5f, 0.5f);
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "resetWallpaperOffsets exception = " + e);
                    return;
                }
            }
            Log.d(TAG, "resetWallpaperOffsets - wallpaper manager is null");
            return;
        }
        Log.d(TAG, "resetWallpaperOffsets - mWindowToken is null");
    }

    public HotWord getHotWordInstance() {
        return this.mHotWord;
    }

    public int getOutlineColor() {
        if (WhiteBgManager.isWhiteBg()) {
            return getResources().getColor(android.R.color.black, null);
        }
        return getResources().getColor(android.R.color.white, null);
    }

    public boolean isGoogleSearchWidget(int currentPage) {
        if (this.mHomeController == null) {
            return false;
        }
        CellLayout cellLayout = (CellLayout) this.mHomeController.getWorkspace().getChildAt(currentPage);
        if (cellLayout == null) {
            return false;
        }
        CellLayoutChildren cl = cellLayout.getCellLayoutChildren();
        if (cl == null) {
            return false;
        }
        int itemCount = cl.getChildCount();
        for (int i = 0; i < itemCount; i++) {
            Object tag = cl.getChildAt(i).getTag();
            if ((tag instanceof LauncherAppWidgetInfo) && ((LauncherAppWidgetInfo) tag).providerName.equals(GOOGLE_SEARCH_WIDGET)) {
                return true;
            }
        }
        return false;
    }

    public void setHotWordDetection(boolean enable) {
        if (LauncherFeature.supportGSAPreWarming()) {
            if (this.mPreWarmingClient != null) {
                this.mPreWarmingClient.requestHotwordDetection(enable);
                Log.d(TAG, "setHotWordDetection : call requestHotWordDetection " + enable);
            }
        } else if (this.mHotWord != null) {
            this.mHotWord.setEnableHotWord(enable);
        }
    }

    private void updateWhiteBgIfNecessary() {
        boolean isWhiteBg = WhiteBgManager.isWhiteBg();
        WhiteBgManager.setup(this);
        if (isWhiteBg != WhiteBgManager.isWhiteBg()) {
            this.mDarkFontObserver.onChange(true);
        }
        boolean isWhiteStatusBar = WhiteBgManager.isWhiteStatusBar();
        WhiteBgManager.setupForStatusBar(this);
        if (isWhiteStatusBar != WhiteBgManager.isWhiteStatusBar()) {
            this.mDarkStatusBarObserver.onChange(true);
        }
        boolean isWhiteNavigationBar = WhiteBgManager.isWhiteNavigationBar();
        WhiteBgManager.setupForNavigationBar(this);
        if (isWhiteNavigationBar != WhiteBgManager.isWhiteNavigationBar()) {
            this.mDarkNavigationBarObserver.onChange(true);
        }
    }

    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        LauncherAppState.getInstance().getShortcutManager().bindDeepShortcutMap(deepShortcutMapCopy);
    }

    public boolean isTrayAnimating() {
        return this.mTrayManager != null && this.mTrayManager.isMoveAndAnimated();
    }

    public void refreshNotifications() {
        NotificationListener.setNotificationsChangedListener(this.mModel, getApplicationContext());
    }

    private void registerReceiversAndObservers() {
        registerReceiver(this.mCloseSystemDialogsReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.DATE_CHANGED");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction(LiveIconManager.CALENDAR_ALARM_INTENT_NAME);
        filter.addAction(LiveIconManager.CLOCK_ALARM_INTENT_NAME);
        registerReceiver(this.mDateChangedReceiver, filter);
        setLiveIconAlarm();
        registerReceiver(this.mLCExtractorReceiver, new IntentFilter(LCExtractor.ACTION_INTENT_LCEXTRACTOR));
        filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        if (ZeroPageController.supportVirtualScreen()) {
            filter.addAction(ZeroPageController.ACTION_INTENT_SET_ZEROPAGE);
        }
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        registerReceiver(this.mReceiver, filter);
        getContentResolver().registerContentObserver(System.getUriFor(WhiteBgManager.PREFERENCES_NEED_DARK_FONT), true, this.mDarkFontObserver);
        getContentResolver().registerContentObserver(System.getUriFor(WhiteBgManager.PREFERENCES_NEED_DARK_STATUSBAR), true, this.mDarkStatusBarObserver);
        getContentResolver().registerContentObserver(System.getUriFor(WhiteBgManager.PREFERENCES_NEED_DARK_NAVIGATIONBAR), true, this.mDarkNavigationBarObserver);
        if (LauncherFeature.supportEasyModeChange()) {
            getContentResolver().registerContentObserver(System.getUriFor("easy_mode_switch"), true, this.mEasyModeObserver);
        }
        if (LauncherFeature.supportNavigationBar()) {
            this.mNavibarPolicy = new NavigationBarPolicy(this);
            if (this.mNavibarPolicy.isDetected()) {
                this.mNavibarPolicy.setOn();
            }
            this.mNavibarPolicy.registerObserver();
        }
    }

    private void unregisterReceiversAndObservers() {
        try {
            unregisterReceiver(this.mCloseSystemDialogsReceiver);
            unregisterReceiver(this.mLCExtractorReceiver);
            if (this.mDateChangedReceiver != null) {
                unregisterReceiver(this.mDateChangedReceiver);
                this.mDateChangedReceiver = null;
            }
            unregisterReceiver(this.mReceiver);
            getContentResolver().unregisterContentObserver(this.mDarkFontObserver);
            getContentResolver().unregisterContentObserver(this.mDarkStatusBarObserver);
            getContentResolver().unregisterContentObserver(this.mDarkNavigationBarObserver);
            if (LauncherFeature.supportEasyModeChange()) {
                getContentResolver().unregisterContentObserver(this.mEasyModeObserver);
            }
            if (this.mNavibarPolicy != null) {
                this.mNavibarPolicy.unRegisterObserver();
            }
        } catch (Exception e) {
        }
    }

    public void finishSettingsActivity() {
        new Handler().post(new Runnable() {
            public void run() {
                SettingsActivity settingsActivity = LauncherAppState.getInstance().getSettingsActivity();
                if (!(settingsActivity == null || GlobalSettingUtils.getStartSetting())) {
                    settingsActivity.finish();
                    LauncherAppState.getInstance().setSettingsActivity(null);
                }
                AppsButtonSettingsActivity appsButtonActivity = LauncherAppState.getInstance().getAppsButtonSettingsActivity();
                if (appsButtonActivity != null) {
                    appsButtonActivity.finish();
                    LauncherAppState.getInstance().setSettingsActivity(null);
                }
            }
        });
    }

    public void onZeroPageActiveChanged(boolean active) {
        this.mHomeController.onZeroPageActiveChanged(active);
    }

    private void mannerModeSet() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.getRingerMode() == 0) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (VERSION.SDK_INT < 24 || notificationManager.isNotificationPolicyAccessGranted()) {
                audioManager.setRingerMode(2);
                Toast.makeText(this, R.string.sound_mode_to_sound, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"));
        } else if (audioManager.getRingerMode() == 1) {
            audioManager.setRingerMode(2);
            Toast.makeText(this, R.string.sound_mode_to_sound, Toast.LENGTH_SHORT).show();
        } else {
            audioManager.setRingerMode(1);
            Toast.makeText(this, R.string.sound_mode_to_vibrate, Toast.LENGTH_SHORT).show();
        }
    }

    public void startLCExtractor(int extractType) {
        this.mLCExtractor = new LCExtractor(this, extractType);
        this.mLCExtractor.checkCondition();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 2:
                if (!PermissionUtils.verifyPermissions(grantResults)) {
                    Log.d(TAG, "REQUEST_LAUNCHER_EXTRACTOR not granted.");
                    return;
                } else if (this.mLCExtractor != null) {
                    this.mLCExtractor.startExtractLayout();
                    return;
                } else {
                    Log.e(TAG, "mLCExtractor object didn't created.");
                    return;
                }
            default:
                return;
        }
    }

    private boolean isThereDimEffectForApps() {
        return (isHomeStage() || this.mDragLayer == null || this.mDragLayer.getBackgroundImageAlpha() != 1.0f) ? false : true;
    }
}
