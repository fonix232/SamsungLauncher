package com.android.launcher3.proxy;

import java.util.ArrayList;
import java.util.Iterator;

public class LauncherTopViewChangedMessageHandler {
    public static final int LAUNCHER_VIEW_ADD_FOLDER = 9;
    public static final int LAUNCHER_VIEW_APPS = 2;
    public static final int LAUNCHER_VIEW_APPS_FOLDER = 4;
    public static final int LAUNCHER_VIEW_APPS_FOLDER_ADD_ICON_SEARCH_RESULT = 19;
    public static final int LAUNCHER_VIEW_APPS_FOLDER_ADD_ICON_SEARCH_VIEW = 14;
    public static final int LAUNCHER_VIEW_APPS_GRID_CHANGE = 24;
    public static final int LAUNCHER_VIEW_APPS_SORT_OPTION_POPUP = 25;
    public static final int LAUNCHER_VIEW_APPS_TIDY_UP_PREVIEW_VIEW = 10;
    public static final int LAUNCHER_VIEW_HIDE_APPS = 8;
    public static final int LAUNCHER_VIEW_HOME = 1;
    public static final int LAUNCHER_VIEW_HOME_EDIT = 5;
    public static final int LAUNCHER_VIEW_HOME_EDIT_REMOVE_PAGE_POPUP = 12;
    public static final int LAUNCHER_VIEW_HOME_FOLDER = 3;
    public static final int LAUNCHER_VIEW_HOME_FOLDER_ADD_ICON_SEARCH_RESULT = 18;
    public static final int LAUNCHER_VIEW_HOME_FOLDER_ADD_ICON_SEARCH_VIEW = 13;
    public static final int LAUNCHER_VIEW_HOME_MODE_CHANGE = 20;
    public static final int LAUNCHER_VIEW_HOME_PAGE_AUTO_RE_ARRANGE_POPUP = 11;
    public static final int LAUNCHER_VIEW_HOME_PAGE_REARANGE_POPUP = 21;
    public static final int LAUNCHER_VIEW_HOME_PAGE_WIDGET_SEARCH_RESULT = 16;
    public static final int LAUNCHER_VIEW_HOME_PAGE_WIDGET_SEARCH_VIEW = 7;
    public static final int LAUNCHER_VIEW_HOME_PAGE_WIDGET_UNINSTALL_VIEW = 22;
    public static final int LAUNCHER_VIEW_HOME_SETTING = 6;
    public static final int LAUNCHER_VIEW_HOME_SETTINGS_CHANGE_TO_HOMESCREEN_ONLY_POPUP = 15;
    public static final int LAUNCHER_VIEW_HOME_SETTINGS_CHANGE_TO_HOME_APPS_POPUP = 23;
    public static final int LAUNCHER_VIEW_HOME_SETTINGS_GRID = 17;
    public static final int LAUNCHER_VIEW_NONE = 0;
    private ArrayList<OnLauncherTopViewChangedListener> mOnLauncherTopViewChangedListeners = new ArrayList();

    public void sendMessage(int topViewId) {
        Iterator it = this.mOnLauncherTopViewChangedListeners.iterator();
        while (it.hasNext()) {
            ((OnLauncherTopViewChangedListener) it.next()).onLauncherTopViewChanged(topViewId);
        }
    }

    public void registerOnLauncherTopViewChangedListener(OnLauncherTopViewChangedListener l) {
        this.mOnLauncherTopViewChangedListeners.add(l);
    }

    public void unregisterOnLauncherTopViewChangedListener(OnLauncherTopViewChangedListener l) {
        this.mOnLauncherTopViewChangedListeners.remove(l);
    }
}
