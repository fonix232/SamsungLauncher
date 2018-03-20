package com.android.launcher3.remoteconfiguration;

class RemoteConfigurationConstants {
    static final int FEATURE_SHORTCUT_FOR_EASYMODE = 1001;
    static final String KEY_CELLDIMENSION_COLS_INT = "cols";
    static final String KEY_CELLDIMENSION_ROWS_INT = "rows";
    static final String KEY_COMPONENT_COMPONENTNAME = "component";
    static final String KEY_COORDINATION_POSITION_POINT = "coordination_position";
    static final String KEY_COORDINATION_SIZE_POINT = "coordination_size";
    static final String KEY_DELAY_RESULT_BOOLEAN = "delay_result";
    static final String KEY_FEATURE_INT = "feature";
    static final String KEY_HOMEMODE_STRING = "home_mode";
    static final String KEY_INDEX_INT = "index";
    static final String KEY_INVOCATION_RESULT_INT = "invocation_result";
    static final String KEY_ITEMCOUNT_INT = "itemcount";
    static final String KEY_PAGE_INT = "page";
    static final String KEY_STATE_BOOLEAN = "state";
    static final String KEY_SUPPLEMENT_SERVICE_PAGE_VISIBILITY_BOOLEAN = "visibility";
    static final String KEY_USERID_LONG = "user_id";
    static final String METHOD_ADD_HOTSEAT_ITEM = "add_hotseat_item";
    static final String METHOD_ADD_SHORTCUT = "add_shortcut";
    static final String METHOD_ADD_WIDGET = "add_widget";
    static final String METHOD_DISABLE_APPS_BUTTON = "disable_apps_button";
    static final String METHOD_ENABLE_APPS_BUTTON = "enable_apps_button";
    static final String METHOD_GET_APPS_BUTTON_STATE = "get_apps_button_state";
    static final String METHOD_GET_APPS_CELL_DIMENSION = "get_apps_cell_dimension";
    static final String METHOD_GET_HOME_CELL_DIMENSION = "get_home_cell_dimension";
    static final String METHOD_GET_HOME_MODE = "get_home_mode";
    static final String METHOD_GET_HOTSEAT_ITEM = "get_hotseat_item";
    static final String METHOD_GET_HOTSEAT_ITEM_COUNT = "get_hotseat_item_count";
    static final String METHOD_GET_HOTSEAT_MAXITEM_COUNT = " get_hotseat_maxitem_count";
    static final String METHOD_GET_LAUNCHER_SUPPORT_FEATURE = "get_support_feature";
    static final String METHOD_GET_ROTATION_STATE = "get_rotation_state";
    static final String METHOD_GET_SUPPLEMENT_SERVICE_PAGE_VISIBILITY = "get_supplement_service_page_visibility";
    static final String METHOD_MAKE_EMPTY_POSITION = "make_empty_position";
    static final String METHOD_REMOVE_HOTSEAT_ITEM = "remove_hotseat_item";
    static final String METHOD_REMOVE_PAGE_FROM_HOME = "remove_page_from_home";
    static final String METHOD_REMOVE_SHORTCUT = "remove_shortcut";
    static final String METHOD_REMOVE_WIDGET = "remove_widget";
    static final String METHOD_SET_SUPPLEMENT_SERVICE_PAGE_VISIBILITY = "set_supplement_service_page_visibility";
    static final String METHOD_SWITCH_HOME_MODE = "switch_home_mode";
    static final int RESULT_ACCESS_DENIED = -100;
    static final int RESULT_FAILURE = -2;
    static final int RESULT_NOT_FOUND = -3;
    static final int RESULT_NOT_SUPPORTED = -1;
    static final int RESULT_PARAM_ERROR = -4;
    static final int RESULT_SUCCESS = 0;
    static final int RESULT_SUPPORTED = 0;
    static final String VALUE_HOMEMODE_EASYMODE = "easy_mode";
    static final String VALUE_HOMEMODE_HOMEANDAPPSMODE = "home_apps_mode";
    static final String VALUE_HOMEMODE_HOMEONLYMODE = "home_only_mode";
    static final int VALUE_INVALID = -999;

    private RemoteConfigurationConstants() {
    }
}
