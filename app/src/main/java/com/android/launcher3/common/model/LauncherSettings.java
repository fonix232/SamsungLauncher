package com.android.launcher3.common.model;

import android.net.Uri;
import android.provider.BaseColumns;

public class LauncherSettings {

    public interface ChangeLogColumns extends BaseColumns {
        public static final String LOCK = "lock";
        public static final String MODIFIED = "modified";
    }

    public static final class Settings {
        public static final String AUTHORITY = "com.sec.android.app.launcher.settings";
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/settings");
        public static final String EXTRA_DEFAULT_VALUE = "default_value";
        public static final String EXTRA_VALUE = "value";
        public static final String METHOD_GET_BOOLEAN = "get_boolean_setting";
        public static final String METHOD_SET_BOOLEAN = "set_boolean_setting";
    }

    public interface BaseLauncherColumns extends ChangeLogColumns {
        public static final String COLOR = "color";
        public static final String CONTAINER = "container";
        public static final int CONTAINER_DESKTOP = -100;
        public static final String HIDDEN = "hidden";
        public static final int HIDDEN_BY_GAME = 4;
        public static final int HIDDEN_BY_USER = 2;
        public static final int HIDDEN_BY_XML = 1;
        public static final String ICON = "icon";
        public static final String ICON_PACKAGE = "iconPackage";
        public static final String ICON_RESOURCE = "iconResource";
        public static final String ICON_TYPE = "iconType";
        public static final int ICON_TYPE_BITMAP = 1;
        public static final int ICON_TYPE_RESOURCE = 0;
        public static final String INTENT = "intent";
        public static final String ITEM_TYPE = "itemType";
        public static final int ITEM_TYPE_APPLICATION = 0;
        public static final int ITEM_TYPE_SHORTCUT = 1;
        public static final String NEWCUE = "newCue";
        public static final String OPTIONS = "options";
        public static final String PROFILE_ID = "profileId";
        public static final String RANK = "rank";
        public static final String RESTORED = "restored";
        public static final String SCREEN = "screen";
        public static final String TITLE = "title";
        public static final int UNHIDDEN = 0;
    }

    public static final class WorkspaceScreens implements ChangeLogColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/workspaceScreens");
        public static final String SCREEN_RANK = "screenRank";
        public static final String TABLE_NAME = "workspaceScreens";
    }

    public static final class WorkspaceScreens_Easy implements ChangeLogColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/workspaceScreens_easy");
        public static final String TABLE_NAME = "workspaceScreens_easy";
    }

    public static final class WorkspaceScreens_HomeApps implements ChangeLogColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/workspaceScreens_homeApps");
        public static final String TABLE_NAME = "workspaceScreens_homeApps";
    }

    public static final class WorkspaceScreens_HomeOnly implements ChangeLogColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/workspaceScreens_homeOnly");
        public static final String TABLE_NAME = "workspaceScreens_homeOnly";
    }

    public static final class WorkspaceScreens_Standard implements ChangeLogColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/workspaceScreens_standard");
        public static final String TABLE_NAME = "workspaceScreens_standard";
    }

    public static final class Favorites implements BaseLauncherColumns {
        public static final String APPWIDGET_ID = "appWidgetId";
        public static final String APPWIDGET_PROVIDER = "appWidgetProvider";
        public static final String CELLX = "cellX";
        public static final String CELLY = "cellY";
        public static final int CONTAINER_APPS = -102;
        public static final int CONTAINER_HOTSEAT = -101;
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/favorites");
        public static final String FESTIVAL = "festival";
        public static final int ITEM_TYPE_APPWIDGET = 4;
        public static final int ITEM_TYPE_CUSTOM_APPWIDGET = 5;
        public static final int ITEM_TYPE_DEEP_SHORTCUT = 6;
        public static final int ITEM_TYPE_FOLDER = 2;
        @Deprecated
        static final int ITEM_TYPE_LIVE_FOLDER = 3;
        public static final int ITEM_TYPE_PAIRAPPS_SHORTCUT = 7;
        public static final String SPANX = "spanX";
        public static final String SPANY = "spanY";
        public static final String TABLE_NAME = "favorites";

        public static Uri getContentUri(long id) {
            return Uri.parse("content://com.sec.android.app.launcher.settings/favorites/" + id);
        }

        public static final String containerToString(int container) {
            switch (container) {
                case CONTAINER_APPS /*-102*/:
                    return "apps";
                case CONTAINER_HOTSEAT /*-101*/:
                    return "hotseat";
                case -100:
                    return "desktop";
                default:
                    return String.valueOf(container);
            }
        }
    }

    public static final class Favorites_Easy implements BaseLauncherColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/favorites_easy");
        public static final String TABLE_NAME = "favorites_easy";

        public static Uri getContentUri(long id) {
            return Uri.parse("content://com.sec.android.app.launcher.settings/favorites_easy/" + id);
        }
    }

    public static final class Favorites_HomeApps implements BaseLauncherColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/favorites_homeApps");
        public static final String TABLE_NAME = "favorites_homeApps";

        public static Uri getContentUri(long id) {
            return Uri.parse("content://com.sec.android.app.launcher.settings/favorites_homeApps/" + id);
        }
    }

    public static final class Favorites_HomeOnly implements BaseLauncherColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/favorites_homeOnly");
        public static final String TABLE_NAME = "favorites_homeOnly";

        public static Uri getContentUri(long id) {
            return Uri.parse("content://com.sec.android.app.launcher.settings/favorites_homeOnly/" + id);
        }
    }

    public static final class Favorites_Standard implements BaseLauncherColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.sec.android.app.launcher.settings/favorites_standard");
        public static final String TABLE_NAME = "favorites_standard";

        public static Uri getContentUri(long id) {
            return Uri.parse("content://com.sec.android.app.launcher.settings/favorites_standard/" + id);
        }
    }
}
