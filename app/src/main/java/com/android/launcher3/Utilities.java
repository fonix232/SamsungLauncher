package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;
import android.os.StrictMode.VmPolicy.Builder;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.extractor.LCExtractor;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.dialog.DisableAppConfirmationDialog;
import com.android.launcher3.common.dialog.FolderDeleteDialog;
import com.android.launcher3.common.dialog.SleepAppConfirmationDialog;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.DisableableAppCache;
import com.android.launcher3.common.model.LauncherSettings.Settings;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.features.CscFeature;
import com.android.launcher3.features.FloatingFeature;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.LauncherPairAppsInfo;
import com.android.launcher3.util.CatEventDownload;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.SecureFolderHelper;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.logging.SALogging;
import com.android.vcard.VCardConfig;
import com.sec.android.app.launcher.BuildConfig;
import com.sec.android.app.launcher.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class Utilities {
    public static final String ABOUT_PREFERENCE_KEY = "pref_about_page";
    private static final String ACTION_EDGE_HANDLE_STATE = "com.sec.android.launcher.action.EDGE_HANDLE_STATE";
    public static final String ACTION_SHOW_APPS_VIEW = "com.sec.launcher.action.SHOW_APPS_VIEW";
    public static final String ADD_APPS_TO_HOME_SCREEN_PREFERENCE_KEY = "pref_add_apps_to_home_screen";
    public static final String ADD_ICON_PREFERENCE_INITIALIZED_KEY = "pref_add_icon_to_home_initialized";
    public static final String ADD_ICON_PREFERENCE_KEY = "pref_add_icon_to_home";
    public static final String APPS_BUTTON_SETTING_PREFERENCE_KEY = "pref_apps_button_setting";
    public static final String APPS_GRID_PREFERENCE_KEY = "pref_apps_screen_grid";
    public static final String APPS_SCREEN_GRID_SUMMARY = "apps_screen_grid_summary";
    public static final String APP_ICON_BADGES_PREFERENCE_KEY = "pref_app_icon_badges";
    public static final boolean ATLEAST_JB_MR1;
    public static final boolean ATLEAST_KITKAT;
    public static final boolean ATLEAST_LOLLIPOP;
    public static final boolean ATLEAST_MARSHMALLOW;
    public static final boolean ATLEAST_N;
    public static final boolean ATLEAST_N_MR1;
    public static final boolean ATLEAST_O = (VERSION.SDK_INT >= 26);
    public static final String AXEL_UPDAY_CLASS_NAME = "de.axelspringer.yana.activities.HomeActivity";
    public static final String AXEL_UPDAY_PACKAGE_NAME = "de.axelspringer.yana.zeropage";
    private static final String BADGE_APP_ICON_TYPE = "badge_app_icon_type";
    public static final String BADGE_MANAGER_PREFERENCE_KEY = "badge_manager";
    public static final int BADGE_WITH_DOT = 1;
    private static final int BADGE_WITH_NUMBER = 0;
    public static final String BLOCK_CREATE_SHORTCUT_PREFIX = "block_create_shortcut_";
    public static final String CONTACT_SHORTCUT_IDS = "contact_shortcut_ids";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = (CPU_COUNT + 1);
    public static final String DAYLITE_CLASS_NAME_MAIN = "com.samsung.android.app.spage.main.MainActivity";
    public static final String DAYLITE_CLASS_NAME_SETTING = "com.samsung.android.app.spage.main.settings.SettingsActivity";
    public static final String DAYLITE_PACKAGE_NAME = "com.samsung.android.app.spage";
    public static final String DAYLITE_SETTING_PREFERENCE_KEY = "pref_daylite_setting";
    public static final String EXTRA_ENTER_APPS_SCREEN_GRID = "extra_enter_apps_screen_grid";
    public static final String EXTRA_ENTER_HIDE_APPS = "extra_enter_hide_apps";
    public static final String EXTRA_ENTER_SCREEN_GRID = "extra_enter_screen_grid";
    public static final String EXTRA_ENTER_WIDGETS = "extra_enter_widgets";
    public static final String[] EXTRA_KEY_BLACK_LIST = new String[]{"isCreateShortcut", "FROM_SHORTCUT", "package", "extra_data", "tName", "duplicate", "isShortCut", "key.from", "fromAppShortCut", "i_from"};
    private static final int FLAG_SUSPENDED = 1073741824;
    public static final String FLIPBOARD_BRIEFING_CLASS_NAME = "flipboard.boxer.gui.LaunchActivity";
    public static final String FLIPBOARD_BRIEFING_PACKAGE_NAME = "flipboard.boxer.app";
    public static final String GRID_PREFERENCE_KEY = "pref_screen_grid";
    public static final String HIDE_APPS_PREFERENCE_KEY = "pref_hide_apps";
    private static final int HOMEEASY_DEFAULT_PAGE_INDEX = 1;
    public static final String HOMESCREEN_MODE_PREFERENCE_KEY = "pref_home_screen_mode";
    private static final int HOME_DEFAULT_PAGE_INDEX = 0;
    public static final String INSTALLED_PACKAGES_PREFIX = "installed_packages_for_user_";
    public static final String INSTALLED_PACKAGES_PREFIX_HOME_ONLY = "home_only_installed_packages_for_user_";
    private static final int KEEP_ALIVE = 1;
    private static final int MAXIMUM_POOL_SIZE = ((CPU_COUNT * 2) + 1);
    private static final float MAX_FONT_SCALE = 1.2f;
    public static final int NAVIGATION_POSITION_BOTTOM = 0;
    public static final int NAVIGATION_POSITION_LEFT = 1;
    public static final int NAVIGATION_POSITION_RIGHT = 2;
    private static final String NOTIFICATION_BADGING = "notification_badging";
    public static final String NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY = "pref_notification_panel_setting";
    private static final String NOTIFICATION_PREVIEW = "home_show_notification_enabled";
    public static final int NO_BADGE = -1;
    public static final String ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY = "pref_only_portrait_mode_setting";
    private static final String SAMSUNG_APPS = "com.sec.android.app.samsungapps";
    public static final String SCREEN_GRID_SUMMARY = "screen_grid_summary";
    public static final String SMARTSWITCH_RESTORE_ERROR_CODE = "smartswitch_restore_error_code";
    public static final String SMARTSWITCH_RESTORE_RESULT = "smartswitch_restore_result";
    public static final String SMARTSWITCH_RESTORE_SOURCE = "smartswitch_restore_source";
    public static final String SMARTSWITCH_SAVE_FILE_LENGTH = "smartswich_save_file_length";
    public static final String SOHU_NEWS_CLASS_NAME = "com.mobilesrepublic.sohu.launcher.MainActivity";
    public static final String SOHU_NEWS_PACKAGE_NAME = "com.mobilesrepublic.sohu.launcher";
    public static final int SUPPORT_THEME_STORE_UNINSTALL = 3;
    public static final int SUPPORT_THEME_STORE_WALLPAPERS_AND_THEMES = 0;
    public static final int SUPPORT_THEME_STORE_WALLPAPERS_ONLY = 1;
    public static final int SUPPORT_WALLPAPER_PICKER = 2;
    private static final String TAG = "Launcher.Utilities";
    public static final int THEME_CONTENT_TYPE_THEME = 1;
    private static final String THEME_CONTENT_TYPE_THEME_URI = "themestore://MainPage?contentsType=THEMES&from=homeScreen";
    public static final int THEME_CONTENT_TYPE_WALLPAPER = 0;
    private static final String THEME_CONTENT_TYPE_WALLPAPER_URI = "themestore://MainPage?contentsType=WALLPAPERS&from=homeScreen";
    private static final String THEME_STORE_INTENT = "com.samsung.android.action.THEME_SERVICE_LAUNCH";
    private static final int THEME_STORE_NEW_VERSION = 20000;
    private static final String THEME_STORE_PACKAGE = "com.samsung.android.themestore";
    private static final String THEME_STORE_WALLPAPER_ONLY_URI = "themestore://MyTheme/Wallpaper";
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 1, TimeUnit.SECONDS, new LinkedBlockingQueue());
    public static final String TOUTIAO_NEWS_CLASS_NAME = "com.ss.android.sdk.minusscreen.samsung.activity.FeedFragmentActivity";
    public static final String TOUTIAO_NEWS_PACKAGE_NAME = "com.ss.android.sdk.minusscreen.samsung";
    public static final String USER_FOLDER_ID_PREFIX = "user_folder_";
    public static final String USER_FOLDER_ID_PREFIX_HOME_ONLY = "home_only_user_folder_";
    public static final String USER_FOLDER_NAME_PREFIX = "user_folder_name_";
    public static final String WIDGETS_PREFERENCE_KEY = "pref_hide_widgets";
    private static final int alphanumSupEndCodePoint = 127487;
    private static final int alphanumSupStartCodePoint = 127232;
    private static final char[] arabicNumberArray = new char[]{'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
    private static final String cscClockPackageName = CscFeature.getString("CscFeature_Clock_ConfigReplacePackage");
    private static final int emojiEndCodePoint = 128767;
    private static final int emojiStartCodePoint = 127744;
    private static final char[] farsiNumberArray = new char[]{'٠', '١', '٢', '٣', '۴', '۵', '۶', '٧', '٨', '٩'};
    private static final String floatingClockPackageName = FloatingFeature.getString("SEC_FLOATING_FEATURE_CLOCK_CONFIG_PACKAGE_NAME", LiveIconManager.DEFAULT_PACKAGE_NAME_CLOCK);
    @Deprecated
    private static int launcherResumeCounter = 0;
    private static boolean sIsDoingExpandNotiPanel = false;
    private static boolean sIsInMultiWindowMode = false;
    private static boolean sIsMobileKeyboardMode = false;
    private static boolean sIsOnlyPortraitMode = (!LauncherFeature.isTablet());
    public static boolean sIsRtl;
    private static int sNavigationBarPosition = 0;
    private static int sOrientation = 0;
    private static int sThemeStoreState = 0;
    private static final Pattern sTrimPattern = Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");

    static {
        boolean z;
        if (VERSION.SDK_INT >= 24) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_N = z;
        if (VERSION.SDK_INT >= 25) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_N_MR1 = z;
        if (VERSION.SDK_INT >= 21) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_LOLLIPOP = z;
        if (VERSION.SDK_INT >= 19) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_KITKAT = z;
        if (VERSION.SDK_INT >= 17) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_JB_MR1 = z;
        if (VERSION.SDK_INT >= 23) {
            z = true;
        } else {
            z = false;
        }
        ATLEAST_MARSHMALLOW = z;
    }

    static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }

    public static float getDescendantCoordRelativeToParent(View descendant, View root, int[] coord, boolean includeRootScroll) {
        return getDescendantCoordRelativeToParent(null, descendant, root, coord, includeRootScroll);
    }

    public static float getDescendantCoordRelativeToParent(Context context, View descendant, View root, int[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList();
        float[] pt = new float[]{(float) coord[0], (float) coord[1]};
        View v = descendant;
        while (v != root && v != null) {
            ancestorChain.add(v);
            if (!(v.getParent() instanceof View)) {
                break;
            }
            v = (View) v.getParent();
        }
        ancestorChain.add(root);
        int leftDelta = 0;
        if (context instanceof Launcher) {
            DeviceProfile dp = ((Launcher) context).getDeviceProfile();
            leftDelta = (getNavigationBarPositon() != 1 || dp.isMultiwindowMode) ? 0 : dp.navigationBarHeight;
        }
        float scale = 1.0f;
        int count = ancestorChain.size();
        int i = 0;
        while (i < count) {
            View v0 = (View) ancestorChain.get(i);
            if (v0 != descendant || includeRootScroll) {
                pt[0] = pt[0] - ((float) v0.getScrollX());
                pt[1] = pt[1] - ((float) v0.getScrollY());
            }
            v0.getMatrix().mapPoints(pt);
            pt[0] = ((float) (v0.getLeft() - (i == 0 ? leftDelta : 0))) + pt[0];
            pt[1] = pt[1] + ((float) v0.getTop());
            scale *= v0.getScaleX();
            i++;
        }
        coord[0] = Math.round(pt[0]);
        coord[1] = Math.round(pt[1]);
        return scale;
    }

    public static float mapCoordInSelfToDescendent(View descendant, View root, int[] coord) {
        ArrayList<View> ancestorChain = new ArrayList();
        float[] pt = new float[]{(float) coord[0], (float) coord[1]};
        for (View v = descendant; v != root; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        Matrix inverse = new Matrix();
        int i = ancestorChain.size() - 1;
        while (i >= 0) {
            View ancestor = (View) ancestorChain.get(i);
            View next = i > 0 ? (View) ancestorChain.get(i - 1) : null;
            pt[0] = pt[0] + ((float) ancestor.getScrollX());
            pt[1] = pt[1] + ((float) ancestor.getScrollY());
            if (next != null) {
                pt[0] = pt[0] - ((float) next.getLeft());
                pt[1] = pt[1] - ((float) next.getTop());
                next.getMatrix().invert(inverse);
                inverse.mapPoints(pt);
                scale *= next.getScaleX();
            }
            i--;
        }
        coord[0] = Math.round(pt[0]);
        coord[1] = Math.round(pt[1]);
        return scale;
    }

    public static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= (-slop) && localY >= (-slop) && localX < ((float) v.getWidth()) + slop && localY < ((float) v.getHeight()) + slop;
    }

    public static void rectAboutCenter(Rect r) {
        int cx = r.centerX();
        int cy = r.centerY();
        r.offset(-cx, -cy);
        r.offset(cx, cy);
    }

    public static void startActivityForResultSafely(Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e2) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity.", e2);
        }
    }

    private static void appLauncherBoosting(Activity activity, Intent intent) {
        new DvfsUtil(activity).acquireAppLaunch(intent);
    }

    public static boolean startActivityTouchDown(Activity activity, View v) {
        if (activity == null || v == null || !LauncherFeature.enableStartActivityTouchDown()) {
            return false;
        }
        if ((v.getTag() instanceof IconInfo) && ((IconInfo) v.getTag()).intent != null) {
            ComponentName componentName = ((IconInfo) v.getTag()).intent.getComponent();
            if (componentName != null) {
                String packageName = componentName.getPackageName();
                Intent intent = new Intent();
                intent.putExtra("package_name", packageName);
                intent.setAction("com.samsung.DO_ACTIVE_LAUNCH");
                Log.e("ProActivieLaunch", "Sending Broadcast");
                activity.sendBroadcast(intent);
                return true;
            }
        }
        return false;
    }

    public static boolean startActivitySafelyForPair(Context context, Activity activity, Object tag) {
        LauncherPairAppsInfo info = (LauncherPairAppsInfo) tag;
        Intent firstIntent = info.mFirstApp.getIntent();
        Intent secondIntent = info.mSecondApp.getIntent();
        // TODO: Samsung specific code
//        if (!activity.getPackageManager().isSafeMode() || (isSystemApp(activity, firstIntent) && isSystemApp(activity, secondIntent))) {
//            new SemMultiWindowManager().startPairActivitiesAsUser(context, firstIntent, secondIntent, 0, info.mFirstApp.getUserCompat().getUser(), info.mSecondApp.getUserCompat().getUser());
//            return true;
//        }
        Toast.makeText(activity, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
        return false;
    }

    public static boolean startActivitySafely(Activity activity, View v, Intent intent, Object tag) {
        RuntimeException e;
        if (!activity.getPackageManager().isSafeMode() || isSystemApp(activity, intent)) {
            appLauncherBoosting(activity, intent);
            // TODO: Fix intent flags
            // intent.addFlags(268435456);
            if (v != null) {
                try {
                    boolean useLaunchAnimation = !intent.hasExtra("com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION");
                    LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(activity);
                    UserManagerCompat userManager = UserManagerCompat.getInstance(activity);
                    UserHandleCompat user = null;
                    if (intent.hasExtra(ItemInfo.EXTRA_PROFILE)) {
                        user = userManager.getUserForSerialNumber(intent.getLongExtra(ItemInfo.EXTRA_PROFILE, -1));
                    }
                    Bundle optsBundle = useLaunchAnimation ? buildActivityOptions(v) : null;
                    ItemInfo itemInfo = (ItemInfo) v.getTag();
                    if (ATLEAST_MARSHMALLOW && itemInfo != null && ((itemInfo.itemType == 1 || itemInfo.itemType == 6) && ((IconInfo) itemInfo).promisedIntent == null)) {
                        startActivityVmPolicy(activity, intent, optsBundle, itemInfo);
                    } else if (user == null || user.equals(UserHandleCompat.myUserHandle())) {
                        activity.startActivity(intent, optsBundle);
                    } else {
                        launcherApps.startActivityForProfile(intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
                    }
                    return true;
                } catch (ActivityNotFoundException e2) {
                    e = e2;
                    Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
                    return false;
                } catch (NullPointerException e3) {
                    e = e3;
                    Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
                    return false;
                } catch (SecurityException e4) {
                    Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + tag + " intent=" + intent, e4);
                }
            }
            return false;
        }
        Toast.makeText(activity, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
        return false;
    }

    private static void startActivityVmPolicy(Activity activity, Intent intent, Bundle optsBundle, ItemInfo itemInfo) {
        VmPolicy oldPolicy = StrictMode.getVmPolicy();
        try {
            StrictMode.setVmPolicy(new Builder().detectAll().penaltyLog().build());
            if (itemInfo.itemType == 6) {
                String id = ((IconInfo) itemInfo).getDeepShortcutId();
                LauncherAppState.getInstance().getShortcutManager().startShortcut(intent.getPackage(), id, intent.getSourceBounds(), optsBundle, itemInfo.user);
            } else {
                activity.startActivity(intent, optsBundle);
            }
            StrictMode.setVmPolicy(oldPolicy);
        } catch (Throwable th) {
            StrictMode.setVmPolicy(oldPolicy);
        }
    }

    @Nullable
    private static Bundle buildActivityOptions(View v) {
        ActivityOptions opts = null;
        if (!TestHelper.isRoboUnitTest()) {
            boolean fromHomescreen = false;
            if ((v instanceof IconView) && (((IconView) v).getIconDisplay() == 0 || ((IconView) v).getIconDisplay() == 1)) {
                fromHomescreen = true;
            }
            // TODO: Samsung specific code
//            try {
//                if (VERSION.SEM_INT >= 2403) {
//                    opts = ActivityOptions.semMakeCustomScaleUpAnimation(v, v.getMeasuredWidth(), v.getMeasuredHeight(), fromHomescreen);
//                }
//            } catch (NoSuchMethodError e) {
//                Log.e(TAG, "startActivitySafely : " + e.toString());
//            }
        }
        return opts != null ? opts.toBundle() : null;
    }

    public static boolean isSystemApp(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = intent.getComponent();
        String packageName = null;
        if (cn == null) {
            ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!(info == null || info.activityInfo == null)) {
                packageName = info.activityInfo.packageName;
            }
        } else {
            packageName = cn.getPackageName();
        }
        if (packageName == null) {
            return false;
        }
        try {
            PackageInfo info2 = pm.getPackageInfo(packageName, 0);
            if (info2 == null || info2.applicationInfo == null || (info2.applicationInfo.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static Pair<String, Resources> findSystemApk(String action, PackageManager pm) {
        Iterator it = pm.queryBroadcastReceivers(new Intent(action), 0).iterator();
        while (it.hasNext()) {
            ResolveInfo info = (ResolveInfo) it.next();
            if (!(info.activityInfo == null || (info.activityInfo.applicationInfo.flags & 1) == 0)) {
                String packageName = info.activityInfo.packageName;
                try {
                    return Pair.create(packageName, pm.getResourcesForApplication(packageName));
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "Failed to find resources for " + packageName);
                }
            }
        }
        return null;
    }

    public static byte[] flattenBitmap(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream((bitmap.getWidth() * bitmap.getHeight()) * 4);
        try {
            bitmap.compress(CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write bitmap");
            return null;
        }
    }

    public static String trim(CharSequence s) {
        if (s == null) {
            return null;
        }
        return sTrimPattern.matcher(s).replaceAll("$1");
    }

    public static void assertWorkerThread() {
        if (LauncherModel.sWorkerThread.getThreadId() != Process.myTid()) {
            throw new IllegalStateException();
        }
    }

    public static boolean isLauncherAppTarget(Intent launchIntent) {
        if (launchIntent == null || !"android.intent.action.MAIN".equals(launchIntent.getAction()) || launchIntent.getComponent() == null || launchIntent.getCategories() == null || launchIntent.getCategories().size() != 1 || ((!launchIntent.hasCategory("android.intent.category.LAUNCHER") && !launchIntent.hasCategory("android.intent.category.INFO")) || !TextUtils.isEmpty(launchIntent.getDataString()))) {
            return false;
        }
        Bundle extras = launchIntent.getExtras();
        if (extras == null) {
            return true;
        }
        Set<String> keys = extras.keySet();
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            boolean isInBlackList = false;
            if (EXTRA_KEY_BLACK_LIST != null && EXTRA_KEY_BLACK_LIST.length > 0) {
                for (String key : EXTRA_KEY_BLACK_LIST) {
                    if (keys.contains(key)) {
                        isInBlackList = true;
                        break;
                    }
                }
            }
            if (isInBlackList) {
                Log.d(TAG, "isAppShortcut : This shortcut has extra infos in black list");
                return true;
            } else if (keys.size() == 1 && keys.contains(ItemInfo.EXTRA_PROFILE)) {
                return true;
            } else {
                return false;
            }
        } else if (keys.size() == 1 && keys.contains(ItemInfo.EXTRA_PROFILE)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDeepShortcut(Intent launchIntent) {
        if (LauncherFeature.supportDeepShortcut() && launchIntent != null && "android.intent.action.MAIN".equals(launchIntent.getAction()) && launchIntent.getComponent() != null && launchIntent.getCategories() != null && launchIntent.getCategories().size() == 1 && launchIntent.hasCategory(ShortcutInfoCompat.INTENT_CATEGORY)) {
            return true;
        }
        return false;
    }

    public static boolean isDeepShortcutType(ItemInfo item) {
        return item != null && item.itemType == 6;
    }

    public static int pxFromDp(float size, DisplayMetrics metrics) {
        return Math.round(TypedValue.applyDimension(1, size, metrics));
    }

    public static String createDbSelectionQuery(String columnName, Iterable<?> values) {
        return String.format(Locale.ENGLISH, "%s IN (%s)", new Object[]{columnName, TextUtils.join(", ", values)});
    }

    public static InputFilter[] getEditTextMaxLengthFilter(Context context, final int maxSize) {
        InputFilter[] FilterArray = new InputFilter[1];
        final Toast mToast = Toast.makeText(context, context.getString(R.string.max_characters_available, new Object[]{Integer.valueOf(maxSize)}), Toast.LENGTH_SHORT);
        FilterArray[0] = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (start == 0 && end == 0) {
                    return null;
                }
                int keep = maxSize - (dest.length() - (dend - dstart));
                if (keep <= 0) {
                    mToast.show();
                    return "";
                } else if (keep == 1 && end - start == 2) {
                    mToast.show();
                    int endPos = start + keep;
                    if (source.length() == 2 && dstart == maxSize - 1 && dend == maxSize) {
                        return source.subSequence(start, endPos);
                    }
                    return "";
                } else if (keep >= end - start) {
                    return null;
                } else {
                    if (keep >= end - start) {
                        return null;
                    }
                    try {
                        mToast.show();
                        int endPosition = start + keep;
                        if (Utilities.isEmoji(source.toString().codePointAt(endPosition - 1))) {
                            endPosition--;
                        }
                        for (int i = 0; i < source.length(); i++) {
                            if (Utilities.isEnclosedAlphanumSuppplement(source.toString().codePointAt(i))) {
                                return "";
                            }
                        }
                        return source.subSequence(start, endPosition);
                    } catch (IndexOutOfBoundsException e) {
                        return "";
                    }
                }
            }
        };
        return FilterArray;
    }

    private static boolean isEmoji(int code) {
        return emojiStartCodePoint <= code && code <= emojiEndCodePoint;
    }

    private static boolean isEnclosedAlphanumSuppplement(int code) {
        return alphanumSupStartCodePoint <= code && code <= alphanumSupEndCodePoint;
    }

    public static boolean canUninstall(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        if (pm == null || DisableableAppCache.mUninstallBlockedItems.contains(packageName)) {
            return false;
        }
        try {
            if (!isBlockUninstallAndDisableByEDM(packageName) && (pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES).flags & 1) == 0) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "canUninstall:" + e.toString());
            return false;
        }
    }

    public static boolean canDisable(Context context, String packageName) {
        if (isBlockUninstallAndDisableByEDM(packageName)) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // TODO: Samsung specific code
//        if (dpm != null && !TestHelper.isRoboUnitTest() && dpm.semHasActiveAdminForPackage(packageName)) {
//            return false;
//        }
        if (SecureFolderHelper.SECURE_FOLDER_PACKAGE_NAME.equals(packageName) && !DisableableAppCache.mDisableBlockedItems.contains(packageName) && SecureFolderHelper.isSecureFolderExist(context)) {
            DisableableAppCache.mDisableBlockedItems.add(packageName);
        }
        if (canUninstall(context, packageName) || !DisableableAppCache.mDisableableItems.contains(packageName) || DisableableAppCache.mDisableBlockedItems.contains(packageName)) {
            return false;
        }
        return true;
    }

    public static boolean isBlockUninstallAndDisableByEDM(String packageName) {
        return (LauncherAppState.getInstance() == null || LauncherAppState.getInstance().getModel() == null || LauncherAppState.getInstance().getModel().getDisableableAppCache() == null || !LauncherAppState.getInstance().getModel().getDisableableAppCache().isBlockUninstall(packageName)) ? false : true;
    }

    public static void getScreenSize(Context context, Point size) {
        if (context != null && size != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            if (isPortrait()) {
                size.x = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
                size.y = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
                return;
            }
            size.x = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
            size.y = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
    }

    public static boolean isPortrait() {
        return (!canScreenRotate() && sOrientation == 2) || sOrientation == 1;
    }

    private static void getFullScreenSize(Activity activity, Point size) {
        if (activity != null && size != null) {
            activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        }
    }

    public static int getFullScreenHeight(Activity activity) {
        Point size = new Point();
        getFullScreenSize(activity, size);
        return size.y;
    }

    public static float simplifyDecimalFraction(float value, int validDecimalPlace, int interval) {
        if (validDecimalPlace <= 0) {
            return value;
        }
        float decimalPlace = (float) Math.pow(10.0d, (double) validDecimalPlace);
        return ((float) (Math.round((value * decimalPlace) / ((float) interval)) * interval)) / decimalPlace;
    }

    public static boolean isKnoxMode() {
        return LauncherFeature.isKnoxMode();
    }

    public static boolean isSecureFolderMode() {
        return LauncherFeature.isSecureFolderMode();
    }

    public static boolean isGuest() {
        return false; // TODO: Samsung specific code //UserHandle.semGetCallingUserId() != 0;
    }

    public static boolean DEBUGGABLE() {
        return !TestHelper.isRoboUnitTest(); // TODO: Samsung specific code  //&& Debug.semIsProductDev();
    }

    public static void hideStatusBar(Window window, boolean hideBar) {
        boolean hasFlags = (window.getAttributes().flags & 2048) != 0;
        if (hasFlags && hideBar) {
            window.clearFlags(2048);
        } else if (!hasFlags && !hideBar) {
            window.addFlags(2048);
        }
    }

    public static void hideNavigationBar(Window window, boolean hideBar) {
        if (LauncherFeature.supportNavigationBar()) {
            View decorView = window.getDecorView();
            if (hideBar) {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | 2);
            } else {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & -3);
            }
        }
    }

    public static void expandNotificationsPanel(final Context context) {
        if (!sIsDoingExpandNotiPanel && context != null) {
            sIsDoingExpandNotiPanel = true;
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... args) {
                    // TODO: Samsung specific code
                    // TODO: Open notification shade here
//                    Log.d(Utilities.TAG, "expandNotificationsPanel start");
//                    SemStatusBarManager statusBar = (SemStatusBarManager) context.getSystemService("sem_statusbar");
//                    if (statusBar != null) {
//                        statusBar.expandNotificationsPanel();
//                        SALogging.getInstance().insertEventLog(context.getResources().getString(R.string.screen_Home_1xxx), context.getResources().getString(R.string.event_OpenNotiPanel));
//                    }
//                    Log.d(Utilities.TAG, "expandNotificationsPanel end");
                    return null;
                }

                protected void onPostExecute(Void aVoid) {
                    Utilities.sIsDoingExpandNotiPanel = false;
                }
            }.executeOnExecutor(THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public static Boolean checkClockPackageName(String pkg) {
        if ("".equals(cscClockPackageName)) {
            return Boolean.valueOf(floatingClockPackageName.equals(pkg));
        }
        return Boolean.valueOf(cscClockPackageName.equals(pkg));
    }

    public static String getClockPackageName() {
        if ("".equals(cscClockPackageName)) {
            return floatingClockPackageName;
        }
        return cscClockPackageName;
    }

    public static int getVersionCode(Context context, String packageName) {
        if (context == null) {
            return -1;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return -1;
        }
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);
            if (pkgInfo != null) {
                return pkgInfo.versionCode;
            }
            return -1;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getVersionCode:" + e.toString());
            return -1;
        }
    }

    public static boolean isSamsungMembersEnabled(Context context) {
        String samsungMembersPackageName = "com.samsung.android.voc";
        if (context == null || !isAppEnabled(context, samsungMembersPackageName) || getVersionCode(context, samsungMembersPackageName) < 170001000) {
            return false;
        }
        return true;
    }

    public static boolean isSamsungAppEnabled(Context context) {
        return isAppEnabled(context, SAMSUNG_APPS);
    }

    public static void startContactUsActivity(Context context) {
        if (isSamsungMembersEnabled(context)) {
            String packageName = BuildConfig.APPLICATION_ID;
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("voc://view/contactUs"));
            intent.putExtra(DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
            intent.putExtra("appId", "lwyvkp07y7");
            intent.putExtra("appName", "TouchWiz home");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Log.e(TAG, "ContactUS(SamsungMembers) is not enabled");
            }
        }
    }

    public static void loadCurrentGridSize(Context context, int[] xy) {
        ScreenGridUtilities.loadCurrentGridSize(context, xy, LauncherAppState.getInstance().isHomeOnlyModeEnabled());
    }

    public static boolean findVacantCellToRightBottom(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied, int datumX, int datumY) {
        if (datumX > xCount || datumY > yCount) {
            return false;
        }
        vacant[0] = 0;
        vacant[1] = 0;
        for (int y = datumY; y + spanY <= yCount; y++) {
            int x = 0;
            while (x + spanX <= xCount) {
                if (y != datumY || x >= datumX) {
                    boolean available;
                    if (occupied[x][y]) {
                        available = false;
                    } else {
                        available = true;
                    }
                    int i = x;
                    while (i < x + spanX) {
                        int j = y;
                        while (j < y + spanY) {
                            if (!available || occupied[i][j]) {
                                available = false;
                            } else {
                                available = true;
                            }
                            if (!available) {
                                break;
                            }
                            j++;
                        }
                        i++;
                    }
                    if (available) {
                        vacant[0] = x;
                        vacant[1] = y;
                        return true;
                    }
                }
                x++;
            }
        }
        return false;
    }

    public static boolean findVacantCellToLeftTop(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied, int datumX, int datumY) {
        if (datumX > xCount || datumY > yCount) {
            return false;
        }
        vacant[0] = 0;
        vacant[1] = 0;
        int y = datumY;
        while (y >= 0 && y + spanY <= yCount) {
            int x;
            boolean available;
            int i;
            int j;
            if (y == datumY) {
                x = datumX;
                while (x >= 0 && x + spanX <= xCount) {
                    if (occupied[x][y]) {
                        available = false;
                    } else {
                        available = true;
                    }
                    i = x;
                    while (i < x + spanX) {
                        j = y;
                        while (j < y + spanY) {
                            if (!available || occupied[i][j]) {
                                available = false;
                            } else {
                                available = true;
                            }
                            if (!available) {
                                break;
                            }
                            j++;
                        }
                        i++;
                    }
                    if (available) {
                        vacant[0] = x;
                        vacant[1] = y;
                        return true;
                    }
                    x--;
                }
            } else {
                for (x = 0; x + spanX <= xCount; x++) {
                    if (occupied[x][y]) {
                        available = false;
                    } else {
                        available = true;
                    }
                    i = x;
                    while (i < x + spanX) {
                        j = y;
                        while (j < y + spanY) {
                            if (!available || occupied[i][j]) {
                                available = false;
                            } else {
                                available = true;
                            }
                            if (!available) {
                                break;
                            }
                            j++;
                        }
                        i++;
                    }
                    if (available) {
                        vacant[0] = x;
                        vacant[1] = y;
                        return true;
                    }
                }
                continue;
            }
            y--;
        }
        return false;
    }

    public static String getAppLabel(Context context, String packageName) {
        if (context == null) {
            return "";
        }
        ApplicationInfo ai;
        PackageManager pm = context.getPackageManager();
        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            ai = null;
        }
        return ai != null ? ai.loadLabel(pm).toString() : "";
    }

    public static boolean isAppSuspended(ApplicationInfo info) {
        return ATLEAST_N && (info.flags & FLAG_SUSPENDED) != 0;
    }

    public static boolean hasPermissionForActivity(Context context, Intent intent, String srcPackage) {
        boolean z = true;
        PackageManager pm = context.getPackageManager();
        ResolveInfo target = pm.resolveActivity(intent, 0);
        if (target == null) {
            return false;
        }
        if (TextUtils.isEmpty(target.activityInfo.permission)) {
            return true;
        }
        if (TextUtils.isEmpty(srcPackage) || pm.checkPermission(target.activityInfo.permission, srcPackage) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (!ATLEAST_MARSHMALLOW) {
            return true;
        }
        if (TextUtils.isEmpty(AppOpsManager.permissionToOp(target.activityInfo.permission))) {
            return true;
        }
        try {
            if (pm.getApplicationInfo(srcPackage, 0).targetSdkVersion < 23) {
                z = false;
            }
            return z;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void clearBadge(Context context, ComponentName componentName, UserHandleCompat user) {
        if (componentName != null && componentName.getPackageName() != null && componentName.getClassName() != null) {
            try {
                Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
                intent.putExtra("badge_count_package_name", componentName.getPackageName());
                intent.putExtra("badge_count_class_name", componentName.getClassName());
                intent.putExtra("badge_count", 0);
                // TODO: Fix intent flag
                // intent.addFlags(268435456);
                if (user != null) {
                    context.sendBroadcastAsUser(intent, user.getUser());
                } else {
                    context.sendBroadcast(intent);
                }
            } catch (Exception e) {
                Log.d(TAG, "removeBadge():Can't send the broadcast >>> " + e);
            }
        }
    }

    public static int checkThemeStoreState(Context context) {
        if (getVersionCode(context, THEME_STORE_PACKAGE) >= THEME_STORE_NEW_VERSION) {
            if (isGuest() || isKnoxMode()) {
                sThemeStoreState = 1;
            } else {
                sThemeStoreState = 0;
            }
        } else if (LauncherFeature.isChinaModel()) {
            sThemeStoreState = 3;
        } else {
            sThemeStoreState = 2;
        }
        return sThemeStoreState;
    }

    public static void startThemeStore(Context context, int contentType) {
        Log.d(TAG, "onClickWallpapersAndThemesButton");
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(THEME_STORE_PACKAGE, 0);
            if (appInfo != null && appInfo.enabled) {
                Intent themeIntent;
                if (sThemeStoreState == 1) {
                    themeIntent = new Intent();
                    themeIntent.setData(Uri.parse(THEME_STORE_WALLPAPER_ONLY_URI));
                    themeIntent.addCategory("android.intent.category.DEFAULT");
                } else {
                    Uri parse;
                    themeIntent = new Intent();
                    if (contentType == 0) {
                        parse = Uri.parse(THEME_CONTENT_TYPE_WALLPAPER_URI);
                    } else {
                        parse = Uri.parse(THEME_CONTENT_TYPE_THEME_URI);
                    }
                    themeIntent.setData(parse);
                }
                // TODO: Fix intent flag
                // themeIntent.addFlags(268468256);
                themeIntent.putExtra("UpButton", false);
                themeIntent.putExtra("prevPackage", "homeScreen");
                context.startActivity(themeIntent);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "ThemeStore is not installed.", e);
            if (sThemeStoreState == 3) {
                downloadThemeFromSamsungApps(context);
            }
        } catch (ActivityNotFoundException e2) {
            Log.e(TAG, "Unable to launch OpenThemes.", e2);
        }
    }

    private static void downloadThemeFromSamsungApps(Context context) {
        if (context != null && isAppEnabled(context, SAMSUNG_APPS)) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("samsungapps://ProductDetail/com.samsung.android.themestore?simpleMode=true&signId=50-1061-26"));
            // TODO: Fix intent flag
            // intent.addFlags(335544352);
            context.startActivity(intent);
        }
    }

    public static boolean isEnableBtnBg(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return System.getInt(context.getContentResolver(), "show_button_background") != 0;
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String toArabicDigits(String engNumber, String lang) {
        char[] numberArray = null;
        if ("ar".equals(lang)) {
            numberArray = arabicNumberArray;
        } else if ("fa".equals(lang)) {
            numberArray = farsiNumberArray;
        }
        StringBuilder builder = new StringBuilder();
        if (numberArray != null) {
            int length = engNumber.length();
            for (int i = 0; i < length; i++) {
                if (Character.isDigit(engNumber.charAt(i))) {
                    builder.append(numberArray[engNumber.charAt(i) - 48]);
                }
            }
        }
        return builder.toString();
    }

    public static void startVoiceRecognitionActivity(Context context, String pkgName, int requestCode) {
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("calling_package", pkgName);
        intent.putExtra("android.speech.extra.PROMPT", context.getResources().getString(R.string.speak_now));
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.MAX_RESULTS", 5);
        Log.d("TAG", "start voice recognition activity");
        startActivityForResultSafely((Launcher) context, intent, requestCode);
    }

    public static long getCustomerPageKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        long ret = prefs.getLong(LauncherFiles.CUSTOMER_PAGE_KEY, -1);
        if (ret != -1) {
            return ret;
        }
        ret = (long) getHomeDefaultPageKey(context);
        prefs.edit().putLong(LauncherFiles.CUSTOMER_PAGE_KEY, ret).apply();
        Log.d(TAG, "save customer page key " + ret);
        return ret;
    }

    public static int getHomeDefaultPageKey(Context context) {
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            return getHomeDefaultPageKey(context, LauncherFiles.HOMEEASY_DEFAULT_PAGE_KEY);
        }
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            return getHomeDefaultPageKey(context, LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY);
        }
        return getHomeDefaultPageKey(context, LauncherFiles.HOME_DEFAULT_PAGE_KEY);
    }

    public static void setHomeDefaultPageKey(Context context, int defaultPage) {
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            setHomeDefaultPageKey(context, defaultPage, LauncherFiles.HOMEEASY_DEFAULT_PAGE_KEY);
        } else if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            setHomeDefaultPageKey(context, defaultPage, LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY);
        } else {
            setHomeDefaultPageKey(context, defaultPage, LauncherFiles.HOME_DEFAULT_PAGE_KEY);
        }
    }

    public static void setHomeDefaultPageKey(Context context, int defaultPage, String key) {
        if (defaultPage < -1) {
            throw new RuntimeException("Error: setHomeDefaultPageKey use wrong defaultPage - " + defaultPage);
        }
        Editor editor = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        editor.putInt(key, defaultPage);
        editor.apply();
        SALogging.getInstance().updateDefaultPageLog();
        if (LauncherFiles.HOME_DEFAULT_PAGE_KEY.equals(key)) {
            LauncherProviderID providerID = LauncherAppState.getLauncherProviderID();
            if (providerID != null) {
                Log.d(TAG, "[SPRINT] updating home screen index of prefs table");
                providerID.updateScreenIndex();
            }
        }
    }

    public static int getHomeDefaultPageKey(Context context, String key) {
        // TODO: Samsung specific code
//        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
//        if (key.equals(LauncherFiles.HOMEEASY_DEFAULT_PAGE_KEY)) {
//            return prefs.getInt(key, 1);
//        }
//        return prefs.getInt(key, SemCscFeature.getInstance().getInt("CscFeature_Launcher_DefaultPageNumber", 0));
        return 0;
    }

    public static void setZeroPageKey(Context context, boolean value, String key) {
        Log.i(TAG, "setZeroPageKey: " + key + ", " + value);
        Editor editor = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getZeroPageKey(Context context, String key) {
        return context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(key, false);
    }

    public static void removeAppsInFolder(ArrayList<FolderInfo> folderInfos, ArrayList<ItemInfo> apps) {
        HashMap<FolderInfo, ArrayList<IconInfo>> folderItemMap = new HashMap();
        int size = folderInfos.size();
        for (int i = 0; i < size; i++) {
            FolderInfo folderInfo = (FolderInfo) folderInfos.get(i);
            if (!folderItemMap.containsKey(folderInfo)) {
                folderItemMap.put(folderInfo, new ArrayList());
            }
            ((ArrayList) folderItemMap.get(folderInfo)).add((IconInfo) apps.get(i));
        }
        for (FolderInfo folderInfo2 : folderItemMap.keySet()) {
            folderInfo2.remove((ArrayList) folderItemMap.get(folderInfo2));
        }
    }

    public static int getRandomColor(float colorAlpha) {
        return (((int) (255.0f * colorAlpha)) << 24) | (0x00ffffff & ((int) (Math.random() * 1.6777215E7d)));
    }

    static void setMobileKeyboardMode(Configuration newConfig) {
        // TODO: Samsung specific code
//        boolean z = true;
//        if (LauncherFeature.supportNfcHwKeyboard()) {
//            if (newConfig == null || newConfig.semMobileKeyboardCovered != 1) {
//                z = false;
//            }
//            sIsMobileKeyboardMode = z;
//        }
    }

    public static boolean isMobileKeyboardMode() {
        return sIsMobileKeyboardMode;
    }

    public static String getKnoxContainerName(Context context) {
        // TODO: Samsung specific code
//        SemPersonaManager personaManager = (SemPersonaManager) context.getSystemService("persona");
//        String knoxName = null;
//        try {
//            knoxName = SemPersonaManager.getPersonaName(context, SemPersonaManager.getKnoxInfoForApp(context).getInt("userId"));
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage());
//        }
//        return knoxName == null ? "Knox" : knoxName;

        return "null";
    }

    public static void addToPersonal(Context context, String packageName, ComponentName componentName, String title, Bitmap icon) {
        // TODO: Samsung specific code
//        String PERSONA_SHORTCUT = "com.samsung.intent.action.LAUNCH_PERSONA_SHORTCUT";
//        try {
//            int userId = SemPersonaManager.getKnoxInfoForApp(context).getInt("userId");
//            Intent intent = new Intent(PERSONA_SHORTCUT);
//            intent.setData(Uri.parse("persona_shortcut://"));
//            Bundle bundle = new Bundle();
//            bundle.putString("package", packageName);
//            bundle.putParcelable("component", componentName);
//            bundle.putString("label", title);
//            bundle.putParcelable("iconBitmap", icon);
//            bundle.putInt("personalId", userId);
//            bundle.putString("commandType", "createShortcut");
//            intent.putExtras(bundle);
//            context.sendBroadcast(intent);
//        } catch (Exception e) {
//            Log.e(TAG, "Exception in adding shortcut to personal." + e);
//        }
    }

    public static IconInfo createAppsButton(Context context) {
        IconInfo info = new IconInfo();
        info.title = context.getResources().getString(R.string.apps_button_label);
        info.intent = new Intent(ACTION_SHOW_APPS_VIEW);
        info.itemType = 1;
        info.isAppsButton = true;
        info.container = -101;
        return info;
    }

    public static boolean isPackageExist(Context context, String strAppPackage) {
        try {
            context.getPackageManager().getPackageInfo(strAppPackage, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            Log.e(TAG, strAppPackage + " is not installed ");
            return false;
        }
    }

    public static boolean isAppEnabled(Context context, IconInfo item) {
        String pkgName = "";
        ComponentName compName = item.componentName;
        if (compName == null) {
            compName = item.getTargetComponent();
        }
        if (compName != null) {
            pkgName = compName.getPackageName();
        }
        return isAppEnabled(context, pkgName);
    }

    private static boolean isAppEnabled(Context context, String pkgName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkgName, 0);
            return appInfo != null && appInfo.enabled;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static void changeEdgeHandleState(Context context, boolean isShow) {
        if (!TestHelper.isRoboUnitTest()) {
            if (isKnoxMode() || LauncherFeature.isEdgeDevice()) {
                Intent intent = new Intent();
                intent.setAction(ACTION_EDGE_HANDLE_STATE);
                intent.putExtra("isShow", isShow);
                context.sendBroadcast(intent);
            }
        }
    }

    static void broadcastStkIntent(Context context) {
        // TODO: Samsung specific code
//        try {
//            if ("1".equals(TelephonyManager.semGetTelephonyProperty(0, "gsm.sim.screenEvent", "0")) || "1".equals(TelephonyManager.semGetTelephonyProperty(1, "gsm.sim.screenEvent", "0"))) {
//                Intent intent = new Intent(CatEventDownload.STK_EVENT_ACTION);
//                new CatEventDownload().putExtra(intent, "STK EVENT");
//                context.sendBroadcast(intent);
//                Log.v(TAG, "broadcastStkIntent sent");
//            }
//        } catch (NoSuchMethodError e) {
//            Log.e(TAG, "NoSuchMethodError occur broadcastStkIntent.", e);
//        }
    }

    public static Locale getLocale(Context context) {
        if (ATLEAST_N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        }
        return context.getResources().getConfiguration().locale;
    }

    public static boolean isTalkBackEnabled(Context context) {
        String accesibilityService = Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
        if (accesibilityService != null) {
            return accesibilityService.matches("(?i).*com.samsung.android.app.talkback.TalkBackService.*") || accesibilityService.matches("(?i).*com.google.android.marvin.talkback.TalkBackService.*");
        } else {
            return false;
        }
    }

    public static int generateRandomNumber(int limit) {
        Random r = new Random();
        r.setSeed(java.lang.System.nanoTime());
        return r.nextInt(limit);
    }

    public static void closeDialog(Activity activity) {
        FragmentManager manager = activity.getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        if (DisableAppConfirmationDialog.isActive(manager)) {
            DisableAppConfirmationDialog.dismiss(ft, manager);
            ft.addToBackStack(null);
        } else if (SleepAppConfirmationDialog.isActive(manager)) {
            SleepAppConfirmationDialog.dismiss(ft, manager);
            ft.addToBackStack(null);
        } else if (FolderDeleteDialog.isActive(manager)) {
            FolderDeleteDialog.dismiss(ft, manager);
            ft.addToBackStack(null);
        }
    }

    public static boolean isDeskTopMode(Context context) {
        return false; // TODO: Samsung specific code //return ((SemDesktopModeManager) context.getSystemService("desktopmode")) != null && SemDesktopModeManager.isDesktopMode();
    }

    public static String getStringByLocale(Activity activity, int id, String locale) {
        Configuration configuration = new Configuration(activity.getResources().getConfiguration());
        configuration.setLocale(new Locale(locale));
        return activity.createConfigurationContext(configuration).getResources().getString(id);
    }

    public static boolean hasFolderItem(ArrayList<DragObject> dragObjects) {
        if (dragObjects == null) {
            return false;
        }
        for (DragObject dragObject : dragObjects) {
            if (dragObject != null && (dragObject.dragInfo instanceof FolderInfo)) {
                return true;
            }
        }
        return false;
    }

    public static long getAnimationDuration(Animator animator) {
        long duration = -1;
        if (animator == null) {
            return -1;
        }
        if (!(animator instanceof AnimatorSet)) {
            return animator.getDuration();
        }
        for (Animator o : ((AnimatorSet) animator).getChildAnimations()) {
            long childDuration = (o).getDuration();
            if (duration < childDuration) {
                duration = childDuration;
            }
        }
        return duration;
    }

    public static boolean isComponentActive(Context context, ComponentName cn, ComponentInfo[] components) {
        if (components == null) {
            return false;
        }
        String className = cn.getClassName();
        for (ComponentInfo ci : components) {
            if (className.equals(ci.name)) {
                int ces = context.getPackageManager().getComponentEnabledSetting(cn);
                return ces == 1 || (ces == 0 && ci.enabled);
            }
        }
        return false;
    }

    public static boolean isBootCompleted() {
        return "1".equals(getSystemProperty("sys.boot_completed", "0"));
    }

    private static String getSystemProperty(String property, String defaultValue) {
        try {
            String value = (String) Class.forName("android.os.SystemProperties").getDeclaredMethod("get", new Class[]{String.class}).invoke(null, new Object[]{property});
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to read system properties e=" + e.toString());
        }
        return defaultValue;
    }

    public static void setNavigationBarPosition(int naviPosition) {
        sNavigationBarPosition = naviPosition;
    }

    public static int getNavigationBarPositon() {
        return sNavigationBarPosition;
    }

    public static void setHoverPopupContentDescription(View view, CharSequence description) {
        // TODO: Samsung specific code
//        try {
//            view.semSetHoverPopupType(1);
//            SemHoverPopupWindow hover = view.semGetHoverPopup(true);
//            if (hover != null) {
//                hover.setContent(description);
//            }
//        } catch (NoSuchMethodError e) {
//            Log.e(TAG, "Method not found : " + e.toString());
//        } catch (Exception e2) {
//            Log.e(TAG, "setHoverPopupContentDescription : " + e2.toString());
//        }
    }

    public static int checkHomeHiddenDir() {
        if (new File(Environment.getExternalStorageDirectory(), LCExtractor.HOMESCREEN_DIR).exists()) {
            return 0;
        }
        if (new File(Environment.getExternalStorageDirectory(), LCExtractor.HOMEDATA_DIR).exists()) {
            return 1;
        }
        return -1;
    }

    @Deprecated
    public static void launcherResumeTesterStart() {
        launcherResumeCounter++;
    }

    @Deprecated
    public static void launcherResumeTesterEnd() {
        launcherResumeCounter--;
    }

    @Deprecated
    public static boolean isNeededToTestLauncherResume() {
        return launcherResumeCounter >= 10;
    }

    @Deprecated
    public static void printCallStack(String tag) {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        Log.i(TAG, "CallStack: " + tag + "-3:" + stackTrace[3]);
        Log.i(TAG, "CallStack: " + tag + "-4:" + stackTrace[4]);
    }

    public static <T> HashSet<T> singletonHashSet(T elem) {
        HashSet<T> hashSet = new HashSet(1);
        hashSet.add(elem);
        return hashSet;
    }

    public static boolean isValidComponent(Context context, ComponentName cn) {
        if (cn == null) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            ActivityInfo activityInfo = pm.getActivityInfo(cn, PackageManager.GET_META_DATA);
            if (activityInfo == null) {
                Log.d(TAG, "PairApps's " + cn.flattenToShortString() + " is null");
            }
            return activityInfo != null;
        } catch (NameNotFoundException e) {
            Log.i(TAG, "Activity Not Found : " + cn.flattenToShortString());
            return false;
        }
    }

    public static int getBadgeSettingValue(Context context) {
        int enable = Secure.getInt(context.getContentResolver(), NOTIFICATION_BADGING, 1);
        int type = Secure.getInt(context.getContentResolver(), BADGE_APP_ICON_TYPE, 0);
        if (enable == 0) {
            return -1;
        }
        if (enable != 1) {
            return -1;
        }
        if (type == 0) {
            return 0;
        }
        if (type == 1) {
            return 1;
        }
        return -1;
    }

    public static void setOnlyPortraitMode(Context context, boolean value) {
        int i;
        sIsOnlyPortraitMode = value;
        SALogging instance = SALogging.getInstance();
        String string = context.getResources().getString(R.string.status_PortraitModeOnly);
        if (value) {
            i = 1;
        } else {
            i = 0;
        }
        instance.insertStatusLog(string, i);
        context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putBoolean(ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY, value).apply();
        context.getContentResolver().notifyChange(Settings.CONTENT_URI, null);
    }

    public static boolean isOnlyPortraitMode() {
        return sIsOnlyPortraitMode;
    }

    public static boolean canScreenRotate() {
        return sIsInMultiWindowMode || !sIsOnlyPortraitMode;
    }

    static void setMultiWindowMode(boolean set) {
        sIsInMultiWindowMode = set;
    }

    public static boolean isMultiWindowMode() {
        return sIsInMultiWindowMode;
    }

    public static int getSearchEditId(Context context) {
        return context.getResources().getIdentifier("android:id/search_src_text", "int", null);
    }

    public static Spanned fromHtml(String source) {
        if (ATLEAST_N) {
            return Html.fromHtml(source, 0);
        }
        return Html.fromHtml(source);
    }

    public static boolean getNotificationPreviewEnable(Context context) {
        return Secure.getInt(context.getContentResolver(), NOTIFICATION_PREVIEW, 0) == 1 && getBadgeSettingValue(context) >= 0;
    }

    public static void setMaxFontScale(Context context, TextView textView) {
        float fontScale = context.getResources().getConfiguration().fontScale;
        float fontsize = textView.getTextSize() / context.getResources().getDisplayMetrics().scaledDensity;
        if (fontScale > MAX_FONT_SCALE) {
            fontScale = MAX_FONT_SCALE;
        }
        textView.setTextSize(1, fontsize * fontScale);
    }

    public static Rect getDrawableBounds(Drawable d) {
        Rect bounds = new Rect();
        d.copyBounds(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) {
            bounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            bounds.offsetTo(0, 0);
        }
        return bounds;
    }

    static void setOrientation(int orientation) {
        sOrientation = orientation;
    }

    public static int getOrientation() {
        return sOrientation;
    }
}
