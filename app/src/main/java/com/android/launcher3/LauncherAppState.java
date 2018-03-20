package com.android.launcher3;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Point;
import android.os.Handler;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.PackageInstallerCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.OpenMarketCustomization;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.BadgeCache;
import com.android.launcher3.common.model.DisableableAppCache;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutCache;
import com.android.launcher3.executor.StateManager;
import com.android.launcher3.home.AppsButtonSettingsActivity;
import com.android.launcher3.home.ExternalRequestQueue;
import com.android.launcher3.proxy.LauncherProxy;
import com.android.launcher3.proxy.LauncherTopViewChangedMessageHandler;
import com.android.launcher3.remoteconfiguration.ExternalMethodQueue;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.util.threadpool.ThreadPool;
import com.android.launcher3.widget.model.WidgetPreviewLoader;
// import com.samsung.android.desktopmode.SemDesktopModeManager;
// import com.samsung.android.desktopmode.SemDesktopModeManager.EventListener;
import com.sec.android.app.launcher.R;
import java.lang.ref.WeakReference;

public class LauncherAppState {
    private static int EASY_GRID_CELLX = 3;
    private static int EASY_GRID_CELLY = 4;
    public static final int HOMESCREEN_BADGE_ALL_APPS_DISABLE = 0;
    public static final int HOMESCREEN_BADGE_ALL_APPS_ENABLE = 2;
    public static final int HOMESCREEN_BADGE_SINGLE_APP_DISABLE = 1;
    public static final String HOME_APPS_MODE = "home_apps_mode";
    public static final String HOME_ONLY_MODE = "home_only_mode";
    private static final String PREFERENCES_APPS_BUTTON_SETTINGS = "apps_button_settings";
    private static final String PREFERENCES_APPS_BUTTON_SETTINGS_EASY = "apps_button_settings_easy";
    private static final String PREFERENCES_BADGE_SETTINGS = "badge_settings";
    private static WeakReference<AppsButtonSettingsActivity> sAppsButtonSettingsActivity;
    private static Context sContext;
    private static boolean sEnableZeroPage = true;
    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static WeakReference<LauncherProviderID> sLauncherProviderID;
    private static WeakReference<SettingsActivity> sSettingsActivity;
    private static boolean sUseExternalRequestQueue = false;
    private final BadgeCache mBadgeCache;
    private final ContentObserver mBadgeObserver;
    private Runnable mBadgeRefreshRunnable;
    private final DeepShortcutManager mDeepShortcutManager;
//    private EventListener mDesktopModeEventListener;
//    private SemDesktopModeManager mDesktopModeManager;
    private final DisableableAppCache mDisableableAppCache;
    private Handler mHandler;
    private final IconCache mIconCache;
    private boolean mIsEasyMode;
    private boolean mIsHomeOnlyMode;
    public DeviceProfile mLandscapeProfile;
    private final LauncherProxy mLauncherProxy;
    private final LauncherModel mModel;
    private boolean mNotificationPanelExpansionEnabled;
    public DeviceProfile mPortraitProfile;
    private StateManager mStateManager;
    private ThreadPool mThreadPool;
    private LauncherTopViewChangedMessageHandler mTopViewChangedMessageHandler;
    private boolean mWallpaperChangedSinceLastCheck;
    private final WidgetPreviewLoader mWidgetCache;

    private static class SingletonHolder {
        private static final LauncherAppState sLauncherAppStateInstance = new LauncherAppState();

        private SingletonHolder() {
        }
    }

    public static LauncherAppState getInstance() {
        return SingletonHolder.sLauncherAppStateInstance;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return SingletonHolder.sLauncherAppStateInstance;
    }

    public Context getContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w("Launcher", "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        this.mNotificationPanelExpansionEnabled = false;
        this.mIsEasyMode = false;
        this.mIsHomeOnlyMode = false;
        this.mHandler = new Handler();
        this.mBadgeObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                if (LauncherFeature.supportNotificationAndProviderBadge()) {
                    LauncherAppState.this.mHandler.removeCallbacks(LauncherAppState.this.mBadgeRefreshRunnable);
                    LauncherAppState.this.mHandler.postDelayed(LauncherAppState.this.mBadgeRefreshRunnable, 200);
                }
            }
        };
        this.mBadgeRefreshRunnable = new Runnable() {
            public void run() {
                LauncherAppState.this.mModel.reloadBadges();
            }
        };
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }
        Log.v("Launcher", "LauncherAppState inited");
        if (Utilities.getOrientation() == 0) {
            Utilities.setOrientation(sContext.getResources().getConfiguration().orientation);
        }
        makeDeviceProfile(sContext);
        if (LauncherFeature.supportNotificationAndProviderBadge()) {
            sContext.getContentResolver().registerContentObserver(BadgeCache.BADGE_URI, true, this.mBadgeObserver);
        }
        this.mIconCache = new IconCache(sContext, getDeviceProfile().defaultIconSize);
        this.mBadgeCache = new BadgeCache(sContext);
        this.mWidgetCache = new WidgetPreviewLoader(sContext, this.mIconCache);
        this.mDisableableAppCache = new DisableableAppCache(sContext);
        this.mDeepShortcutManager = new DeepShortcutManager(sContext, new ShortcutCache());
        this.mModel = new LauncherModel(this, this.mIconCache, this.mBadgeCache, this.mDisableableAppCache, this.mDeepShortcutManager);
        LauncherAppsCompat.getInstance(sContext).addOnAppsChangedCallback(this.mModel);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction(LauncherModel.ICON_BACKGROUNDS_CHANGED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(LauncherModel.ACTION_EDM_UNINSTALL_STATUS_INTERNAL);
        filter.addAction(LauncherModel.ACTION_MANAGED_PROFILE_REFRESH);
        if (LauncherFeature.supportSprintExtension()) {
            Log.v("Launcher", "[SPRINT] Adding Force Launhcer Refresh Intent");
            filter.addAction(LauncherModel.ACTION_SPR_FORCE_REFRESH);
        }
        sContext.registerReceiver(this.mModel, filter);
        filter = new IntentFilter();
        filter.addAction(LauncherModel.ACTION_STK_TITLE_IS_LOADED);
        filter.addDataScheme("package");
        sContext.registerReceiver(this.mModel, filter);
        UserManagerCompat.getInstance(sContext).enableAndResetCache();
        SharedPreferences prefs = sContext.getSharedPreferences(getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportNotificationPanelExpansion()) {
            this.mNotificationPanelExpansionEnabled = prefs.getBoolean(Utilities.NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY, false);
        }
        if (LauncherFeature.supportEasyModeChange()) {
            initEasyMode();
        }
        initHomeOnlyMode();
        initScreenGrid(this.mIsHomeOnlyMode);
        if (LauncherFeature.supportNotificationAndProviderBadge()) {
            this.mModel.reloadBadges();
        }
        this.mStateManager = new StateManager();
        this.mLauncherProxy = new LauncherProxy();
        this.mTopViewChangedMessageHandler = new LauncherTopViewChangedMessageHandler();
        this.mTopViewChangedMessageHandler.registerOnLauncherTopViewChangedListener(this.mStateManager.getTopViewListener());
        // TODO: Samsung specific code
//        this.mDesktopModeManager = (SemDesktopModeManager) sContext.getSystemService("desktopmode");
//        this.mDesktopModeEventListener = new EventListener() {
//            public void onDesktopDockConnectionChanged(boolean isConnected) {
//                Log.d("Launcher", "onDesktopDockConnectionChanged : " + isConnected);
//            }
//
//            public void onDesktopModeChanged(boolean isEnter) {
//                Log.d("Launcher", "onDesktopModeChanged : " + isEnter);
//                if (!isEnter) {
//                    Process.killProcess(Process.myPid());
//                }
//            }
//        };
//        SemDesktopModeManager semDesktopModeManager = this.mDesktopModeManager;
//        SemDesktopModeManager.registerListener(this.mDesktopModeEventListener);
        OpenMarketCustomization.getInstance().setup(sContext);
    }

    public StateManager getStateManager() {
        return this.mStateManager;
    }

    public LauncherProxy getLauncherProxy() {
        return this.mLauncherProxy;
    }

    public LauncherTopViewChangedMessageHandler getTopViewChangedMessageHandler() {
        return this.mTopViewChangedMessageHandler;
    }

    public void onTerminate() {
        if (LauncherFeature.supportNotificationAndProviderBadge()) {
            sContext.getContentResolver().unregisterContentObserver(this.mBadgeObserver);
        }
        sContext.unregisterReceiver(this.mModel);
        LauncherAppsCompat.getInstance(sContext).removeOnAppsChangedCallback(this.mModel);
        PackageInstallerCompat.getInstance(sContext).onStop();
    }

    public void reloadWorkspace() {
        this.mModel.resetLoadedState(false, true);
        this.mModel.startLoaderFromBackground();
    }

    public void reloadApps() {
        this.mModel.resetLoadedState(true, false);
        this.mModel.startLoaderFromBackground();
    }

    LauncherModel setLauncher(Launcher launcher) {
        if (getLauncherProvider() != null) {
            getLauncherProvider().setLauncherProviderChangeListener(launcher);
        }
        this.mModel.initialize(launcher);
        return this.mModel;
    }

    public IconCache getIconCache() {
        return this.mIconCache;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference(provider);
    }

    public static LauncherProvider getLauncherProvider() {
        return (LauncherProvider) sLauncherProvider.get();
    }

    static void setLauncherProviderID(LauncherProviderID provider) {
        sLauncherProviderID = new WeakReference(provider);
    }

    public static LauncherProviderID getLauncherProviderID() {
        return sLauncherProviderID == null ? null : (LauncherProviderID) sLauncherProviderID.get();
    }

    public static String getSharedPreferencesKey() {
        return LauncherFiles.SHARED_PREFERENCES_KEY;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return this.mWidgetCache;
    }

    public void onWallpaperChanged() {
        this.mWallpaperChangedSinceLastCheck = true;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = this.mWallpaperChangedSinceLastCheck;
        this.mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    public DeviceProfile getDeviceProfile() {
        if (Utilities.canScreenRotate() && Utilities.getOrientation() == 2) {
            if (this.mLandscapeProfile == null) {
                makeDeviceProfile(sContext);
            }
            return this.mLandscapeProfile;
        }
        if (this.mPortraitProfile == null) {
            makeDeviceProfile(sContext);
        }
        return this.mPortraitProfile;
    }

    public boolean getNotificationPanelExpansionEnabled() {
        return this.mNotificationPanelExpansionEnabled;
    }

    public void setNotificationPanelExpansionEnabled(boolean value, boolean writePref) {
        if (LauncherFeature.supportNotificationPanelExpansion()) {
            int i;
            this.mNotificationPanelExpansionEnabled = value;
            SALogging.getInstance().insertEventLog(sContext.getResources().getString(R.string.event_Homesettings), sContext.getResources().getString(R.string.event_OpenQuickPanel), value ? 1 : 0);
            SALogging instance = SALogging.getInstance();
            String string = sContext.getResources().getString(R.string.status_OpenQuickPanel);
            if (value) {
                i = 1;
            } else {
                i = 0;
            }
            instance.insertStatusLog(string, i);
            if (writePref) {
                Editor editor = sContext.getSharedPreferences(getSharedPreferencesKey(), 0).edit();
                editor.putBoolean(Utilities.NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY, value);
                editor.apply();
            }
        }
    }

    public boolean isEasyModeEnabled() {
        return this.mIsEasyMode;
    }

    public void writeEasyModeEnabled(boolean isEasyMode) {
        this.mIsEasyMode = isEasyMode;
    }

    private void initEasyMode() {
        this.mIsEasyMode = FavoritesProvider.getInstance().tableExists(Favorites_Standard.TABLE_NAME);
        Log.d("Launcher", "initEasyMode : " + this.mIsEasyMode);
    }

    public boolean isHomeOnlyModeEnabled() {
        return isHomeOnlyModeEnabled(true);
    }

    public boolean isHomeOnlyModeEnabled(boolean checkEasyMode) {
        if (checkEasyMode && isEasyModeEnabled()) {
            return false;
        }
        return this.mIsHomeOnlyMode;
    }

    public void writeHomeOnlyModeEnabled(boolean isHomeOnlyMode) {
        this.mIsHomeOnlyMode = isHomeOnlyMode;
        Editor editor = sContext.getSharedPreferences(getSharedPreferencesKey(), 0).edit();
        editor.putBoolean(HOME_ONLY_MODE, this.mIsHomeOnlyMode);
        editor.apply();
    }

    public void initHomeOnlyMode() {
        SharedPreferences prefs = sContext.getSharedPreferences(getSharedPreferencesKey(), 0);
        if (!LauncherFeature.supportHomeModeChange()) {
            return;
        }
        if (prefs.contains(HOME_ONLY_MODE)) {
            Log.d("Launcher", "PREFERENCES_HOME_ONLY_MODE is exist");
            this.mIsHomeOnlyMode = prefs.getBoolean(HOME_ONLY_MODE, false);
            return;
        }
        Log.d("Launcher", "PREFERENCES_HOME_ONLY_MODE is not exist. Check CSC");
        int homeMode = LauncherFeature.getSupportHomeModeChangeIndex();
        Log.d("Launcher", "homeMode : " + homeMode);
        if (homeMode == 0) {
            this.mIsHomeOnlyMode = true;
        }
        Editor editor = prefs.edit();
        editor.putBoolean(HOME_ONLY_MODE, this.mIsHomeOnlyMode);
        editor.apply();
    }

    public void setBadgeSetings(int status) {
        Editor prefs = sContext.getSharedPreferences(getSharedPreferencesKey(), 0).edit();
        prefs.putInt(PREFERENCES_BADGE_SETTINGS, status);
        prefs.apply();
    }

    public int getBadgeSetings() {
        return sContext.getSharedPreferences(getSharedPreferencesKey(), 0).getInt(PREFERENCES_BADGE_SETTINGS, 2);
    }

    public void removeAppsButtonPref(boolean isEasyMode) {
        Editor prefs = sContext.getSharedPreferences(getSharedPreferencesKey(), 0).edit();
        prefs.remove(isEasyMode ? PREFERENCES_APPS_BUTTON_SETTINGS_EASY : PREFERENCES_APPS_BUTTON_SETTINGS);
        prefs.remove(Utilities.APPS_BUTTON_SETTING_PREFERENCE_KEY);
        prefs.apply();
    }

    public void setAppsButtonEnabled(boolean value) {
        setAppsButtonEnabled(value, this.mIsEasyMode);
    }

    public void setAppsButtonEnabled(boolean value, boolean isEasyMode) {
        Editor prefsEdit = sContext.getSharedPreferences(getSharedPreferencesKey(), 0).edit();
        prefsEdit.putBoolean(isEasyMode ? PREFERENCES_APPS_BUTTON_SETTINGS_EASY : PREFERENCES_APPS_BUTTON_SETTINGS, value);
        prefsEdit.apply();
    }

    public boolean getAppsButtonEnabled() {
        return sContext.getSharedPreferences(getSharedPreferencesKey(), 0).getBoolean(this.mIsEasyMode ? PREFERENCES_APPS_BUTTON_SETTINGS_EASY : PREFERENCES_APPS_BUTTON_SETTINGS, false);
    }

    public ThreadPool getThreadPool() {
        if (this.mThreadPool == null) {
            this.mThreadPool = new ThreadPool();
        }
        return this.mThreadPool;
    }

    public void makeDeviceProfile(Context context) {
        int defaultIconSize;
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int availableSmallSide = Math.min(dm.widthPixels, dm.heightPixels);
        int availableLargeSide = Math.max(dm.widthPixels, dm.heightPixels);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point realSize = new Point();
        display.getRealSize(realSize);
        int smallSide = Math.min(realSize.x, realSize.y);
        int largeSide = Math.max(realSize.x, realSize.y);
        boolean isMultiwindowMode = Utilities.isMultiWindowMode();
        if (Utilities.canScreenRotate() && config.orientation == 2) {
            Log.i("Launcher", "create land profile");
            this.mLandscapeProfile = new DeviceProfile(context, availableLargeSide, availableSmallSide, largeSide, smallSide, true, isMultiwindowMode);
            defaultIconSize = this.mLandscapeProfile.defaultIconSize;
        } else {
            Log.i("Launcher", "create port profile");
            this.mPortraitProfile = new DeviceProfile(context, availableSmallSide, availableLargeSide, smallSide, largeSide, false, isMultiwindowMode);
            defaultIconSize = this.mPortraitProfile.defaultIconSize;
        }
        if (this.mIconCache != null) {
            this.mIconCache.clearCache(defaultIconSize);
            this.mIconCache.clearDB();
        }
    }

    public void initScreenGrid(boolean isHomeOnly) {
        if (LauncherFeature.supportFlexibleGrid()) {
            DeviceProfile dp = getDeviceProfile();
            int defaultX = dp.homeGrid.getCellCountX();
            int defaultY = dp.homeGrid.getCellCountY();
            int[] cellSize = new int[2];
            if (LauncherFeature.supportHomeModeChange()) {
                boolean z;
                Context context = sContext;
                if (isHomeOnly) {
                    z = false;
                } else {
                    z = true;
                }
                ScreenGridUtilities.loadCurrentGridSize(context, cellSize, z);
                if (cellSize[0] <= 0 || cellSize[1] <= 0) {
                    context = sContext;
                    if (isHomeOnly) {
                        z = false;
                    } else {
                        z = true;
                    }
                    ScreenGridUtilities.storeGridLayoutPreference(context, defaultX, defaultY, z);
                }
            }
            ScreenGridUtilities.loadCurrentGridSize(sContext, cellSize, isHomeOnly);
            if (cellSize[0] <= 0 || cellSize[1] <= 0) {
                ScreenGridUtilities.storeGridLayoutPreference(sContext, defaultX, defaultY, isHomeOnly);
                ScreenGridUtilities.storeCurrentScreenGridSetting(sContext, defaultX, defaultY);
                return;
            }
            if (LauncherFeature.supportEasyModeChange() && this.mIsEasyMode) {
                EASY_GRID_CELLX = sContext.getResources().getInteger(R.integer.easy_home_cellCountX);
                EASY_GRID_CELLY = sContext.getResources().getInteger(R.integer.easy_home_cellCountY);
                dp.setCurrentGrid(EASY_GRID_CELLX, EASY_GRID_CELLY);
                dp.setAppsCurrentGrid(EASY_GRID_CELLX, EASY_GRID_CELLY);
            } else {
                dp.setCurrentGrid(cellSize[0], cellSize[1]);
                int[] appsGridXY = new int[2];
                ScreenGridUtilities.loadCurrentAppsGridSize(sContext, appsGridXY);
                if (appsGridXY[0] <= 0 || appsGridXY[1] <= 0) {
                    appsGridXY[0] = sContext.getResources().getInteger(R.integer.apps_default_cellCountX);
                    appsGridXY[1] = sContext.getResources().getInteger(R.integer.apps_default_cellCountY);
                }
                dp.setAppsCurrentGrid(appsGridXY[0], appsGridXY[1]);
            }
            ScreenGridUtilities.storeCurrentScreenGridSetting(sContext, cellSize[0], cellSize[1]);
        }
    }

    public SettingsActivity getSettingsActivity() {
        if (sSettingsActivity == null) {
            return null;
        }
        return (SettingsActivity) sSettingsActivity.get();
    }

    void setSettingsActivity(SettingsActivity activity) {
        sSettingsActivity = new WeakReference(activity);
    }

    public AppsButtonSettingsActivity getAppsButtonSettingsActivity() {
        if (sAppsButtonSettingsActivity == null) {
            return null;
        }
        return (AppsButtonSettingsActivity) sAppsButtonSettingsActivity.get();
    }

    public void setAppsButtonSettingsActivity(AppsButtonSettingsActivity activity) {
        sAppsButtonSettingsActivity = new WeakReference(activity);
    }

    public void enableExternalQueue(boolean enable) {
        sUseExternalRequestQueue = enable;
    }

    public boolean isExternalQueueEnabled() {
        return sUseExternalRequestQueue;
    }

    public void disableAndFlushExternalQueue() {
        sUseExternalRequestQueue = false;
        ExternalRequestQueue.disableAndFlushExternalRequestQueue(sContext, this);
        ExternalMethodQueue.disableAndFlushExternalMethodQueue(sContext, this);
    }

    public DeepShortcutManager getShortcutManager() {
        return this.mDeepShortcutManager;
    }

    public void setEnableZeroPage(boolean enable) {
        sEnableZeroPage = enable;
    }

    public boolean getEnableZeroPage() {
        return sEnableZeroPage;
    }
}
