package com.android.launcher3.executor;

final class StateUtils {
    private StateUtils() {
    }

    static String getStateIdFromViewId(int viewId) {
        switch (viewId) {
            case 1:
                return ExecutorState.HOME.toString();
            case 2:
                return ExecutorState.APPS.toString();
            case 3:
                return ExecutorState.HOME_FOLDER_VIEW.toString();
            case 4:
                return ExecutorState.APPS_FOLDER_VIEW.toString();
            case 5:
                return ExecutorState.HOME_EDIT.toString();
            case 6:
                return ExecutorState.HOME_SETTINGS.toString();
            case 7:
                return ExecutorState.HOME_PAGE_WIDGET_SEARCH_VIEW.toString();
            case 8:
                return ExecutorState.HOME_SETTINGS_HIDE_APPS_VIEW.toString();
            case 9:
                return ExecutorState.HOME_FOLDER_ADD_ICON_VIEW.toString();
            case 10:
                return ExecutorState.APPS_TIDY_UP_PREVIEW_VIEW.toString();
            case 11:
                return ExecutorState.HOME_PAGE_AUTO_RE_ARRANGE_POPUP.toString();
            case 12:
                return ExecutorState.HOME_EDIT_REMOVE_PAGE_POPUP.toString();
            case 13:
                return ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_VIEW.toString();
            case 14:
                return ExecutorState.APPS_FOLDER_ADD_ICON_SEARCH_VIEW.toString();
            case 15:
                return ExecutorState.HOME_SETTINGS_CHANGE_TO_HOMESCREEN_ONLY_POPUP.toString();
            case 16:
                return ExecutorState.HOME_PAGE_WIDGET_SEARCH_RESULT.toString();
            case 17:
                return ExecutorState.HOME_SETTINGS_GRID_SETTING_VIEW.toString();
            case 18:
                return ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_RESULT.toString();
            case 19:
                return ExecutorState.APPS_FOLDER_ADD_ICON_SEARCH_RESULT.toString();
            case 20:
                return ExecutorState.HOME_SETTINGS_STYLE_VIEW.toString();
            case 21:
                return ExecutorState.HOME_PAGE_AUTO_RE_ARRANGE_POPUP.toString();
            case 22:
                return ExecutorState.HOME_PAGE_WIDGET_EDIT_VIEW.toString();
            case 23:
                return ExecutorState.HOME_SETTINGS_CHANGE_TO_HOME_APPS_POPUP.toString();
            case 24:
                return ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW.toString();
            case 25:
                return ExecutorState.APPS_SORT_OPTION_VIEW.toString();
            default:
                return ExecutorState.NONE.toString();
        }
    }
}
