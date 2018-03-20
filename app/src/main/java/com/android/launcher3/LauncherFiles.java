package com.android.launcher3;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LauncherFiles {
    public static final String APP_ICONS_DB = "app_icons.db";
    public static final String AUTOALIGN_SHOW_DIALOG_KEY = "com.sec.android.app.launcher.showdialog.prefs";
    public static final String CUSTOMER_PAGE_KEY = "customerPagePref";
    public static final String DEFAULT_WALLPAPER_THUMBNAIL = "default_thumb2.jpg";
    public static final String DEFAULT_WALLPAPER_THUMBNAIL_OLD = "default_thumb.jpg";
    public static final String HIDEAPPS_PREFERENCES_KEY = "com.sec.android.app.launcher.hideapps.prefs";
    public static final String HOMEEASY_DEFAULT_PAGE_KEY = "com.sec.android.app.launcher.homeeasy.defaultpage.prefs";
    public static final String HOMEONLY_DEFAULT_PAGE_KEY = "com.sec.android.app.launcher.homeonly.defaultpage.prefs";
    public static final String HOME_DEFAULT_PAGE_KEY = "com.sec.android.app.launcher.home.defaultpage.prefs";
    public static final String LAUNCHER_DB = "launcher.db";
    public static final String MANAGED_USER_PREFERENCES_KEY = "com.sec.android.app.launcher.managedusers.prefs";
    public static final String MULTI_SELECT_HELP_KEY = "com.sec.android.app.launcher.multiselect.help.prefs";
    public static final List<String> OBSOLETE_FILES = Collections.unmodifiableList(Arrays.asList("launches.log", "stats.log", "launcher.preferences", "com.android.launcher3.common.compat.PackageInstallerCompatV16.queue"));
    public static final String SAMSUNG_ANALYTICS_PREFERENCES_KEY = "com.sec.android.app.launcher.prefs.sa";
    public static final String SAMSUNG_ANALYTICS_PREFERENCES_KEY_LEGACY = "SASettingPref";
    public static final String SHARED_PREFERENCES_KEY = "com.sec.android.app.launcher.prefs";
    public static final String WALLPAPER_CROP_PREFERENCES_KEY = "com.android.launcher3.WallpaperCropActivity";
    public static final String WALLPAPER_IMAGES_DB = "saved_wallpaper_images.db";
    public static final String WIDGET_PREVIEWS_DB = "widgetpreviews.db";
    private static final String XML = ".xml";
    public static final String ZEROPAGE_ACTIVE_STATE_KEY = "com.sec.android.app.launcher.zeropage.state.prefs";
    public static final String ZEROPAGE_CLASS_NAME_KEY = "com.sec.android.app.launcher.zeropage.class.prefs";
    public static final String ZEROPAGE_PACKAGE_NAME_KEY = "com.sec.android.app.launcher.zeropage.package.prefs";

    public static final List<String> ALL_FILES = Collections.unmodifiableList(Arrays.asList(DEFAULT_WALLPAPER_THUMBNAIL, DEFAULT_WALLPAPER_THUMBNAIL_OLD, LAUNCHER_DB, "com.sec.android.app.launcher.prefs.xml", "com.android.launcher3.WallpaperCropActivity.xml", WALLPAPER_IMAGES_DB, WIDGET_PREVIEWS_DB, MANAGED_USER_PREFERENCES_KEY, HOME_DEFAULT_PAGE_KEY, ZEROPAGE_PACKAGE_NAME_KEY, ZEROPAGE_CLASS_NAME_KEY, ZEROPAGE_ACTIVE_STATE_KEY, APP_ICONS_DB, SAMSUNG_ANALYTICS_PREFERENCES_KEY));
}
