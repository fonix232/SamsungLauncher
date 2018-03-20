package com.android.launcher3.executor;

enum ExecutorState {
    NONE("None", new StateHandlerCreator() {
        public StateHandler create() {
            return null;
        }
    }),
    HOME("Home", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeStateHandler(ExecutorState.HOME);
        }
    }),
    HOME_PAGE_MOVE("HomePageMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageMoveStateHandler(ExecutorState.HOME_PAGE_MOVE);
        }
    }),
    HOME_WIDGET_PAGE_MOVE("HomeWidgetPageMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeWidgetPageMoveStateHandler(ExecutorState.HOME_WIDGET_PAGE_MOVE);
        }
    }),
    HOME_SINGLE_APP_MOVE("HomeSingleAppMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppMoveStateHandler(ExecutorState.HOME_SINGLE_APP_MOVE);
        }
    }),
    HOME_REMOVE_ITEM("HomeSingleAppRemove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppRemoveStateHandler(ExecutorState.HOME_REMOVE_ITEM);
        }
    }),
    HOME_SHOW_APP_INFO("HomeSettingsApplicationInfo", new StateHandlerCreator() {
        public StateHandler create() {
            return new CrossSettingsApplicationInfoStateHandler(ExecutorState.HOME_SHOW_APP_INFO);
        }
    }),
    HOME_SHOW_WIDGET_INFO("SettingsApplicationInfo", new StateHandlerCreator() {
        public StateHandler create() {
            return new CrossSettingsApplicationInfoStateHandler(ExecutorState.HOME_SHOW_WIDGET_INFO);
        }
    }),
    HOME_SINGLE_APP_SELECTED_VIEW("HomeSingleAppSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppSelectedViewStateHandler(ExecutorState.HOME_SINGLE_APP_SELECTED_VIEW);
        }
    }),
    APPS("AppsPageView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsPageStateHandler(ExecutorState.APPS);
        }
    }),
    APPS_PAGE_MOVE("AppsPageMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsPageMoveStateHandler(ExecutorState.APPS_PAGE_MOVE);
        }
    }),
    APPS_SORT_OPTION_VIEW("AppsSortOptionView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSortOptionViewStateHandler(ExecutorState.APPS_SORT_OPTION_VIEW);
        }
    }),
    APPS_CUSTOM_ORDER_SORTED("AppsCustomOrderSorted", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsCustomOrderSortedStateHandler(ExecutorState.APPS_CUSTOM_ORDER_SORTED);
        }
    }),
    APPS_ALPHABETICAL_ORDER_SORTED("AppsAlphabeticalOrderSorted", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsAlphabeticalOrderSortedStateHandler(ExecutorState.APPS_ALPHABETICAL_ORDER_SORTED);
        }
    }),
    APPS_TIDY_UP_PREVIEW_VIEW("AppsTidyUpPagesPreviewView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsTidyUpPagesPreviewViewStateHandler(ExecutorState.APPS_TIDY_UP_PREVIEW_VIEW);
        }
    }),
    APPS_TIDY_UP_PAGES("AppsTidyUpPages", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsTidyUpPagesStateHandler(ExecutorState.APPS_TIDY_UP_PAGES);
        }
    }),
    APPS_SINGLE_APP_SELECTED_VIEW("AppsSingleAppSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppSelectedViewStateHandler(ExecutorState.APPS_SINGLE_APP_SELECTED_VIEW);
        }
    }),
    APPS_SINGLE_APP_MOVE("AppsSingleAppMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppMoveStateHandler(ExecutorState.APPS_SINGLE_APP_MOVE);
        }
    }),
    APPS_SINGLE_APP_MAKE_SHORTCUT("AppsSingleAppMakeShortcut", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppMakeShortcutStateHandler(ExecutorState.APPS_SINGLE_APP_MAKE_SHORTCUT);
        }
    }),
    APPS_CONTACT_US("AppsContactUs", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsContactUsStateHandler(ExecutorState.APPS_CONTACT_US);
        }
    }),
    HOME_FOLDER_VIEW("HomeFolderView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderViewStateHandler(ExecutorState.HOME_FOLDER_VIEW);
        }
    }),
    HOME_FOLDER_MOVE("HomeFolderMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderMoveStateHandler(ExecutorState.HOME_FOLDER_MOVE);
        }
    }),
    HOME_FOLDER_CLOSE("HomeFolderViewClose", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderCloseStateHandler(ExecutorState.HOME_FOLDER_CLOSE);
        }
    }),
    HOME_FOLDER_PAGE_MOVE("HomeFolderPageMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderPageMoveStateHandler(ExecutorState.HOME_FOLDER_PAGE_MOVE);
        }
    }),
    HOME_FOLDER_CHANGE_TITLE("HomeFolderEditName", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderEditNameStateHandler(ExecutorState.HOME_FOLDER_CHANGE_TITLE);
        }
    }),
    HOME_FOLDER_CHANGE_COLOR_VIEW("HomeFolderChangeBackgroundColorSelectView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderChangeBackgroundColorSelectViewStateHandler(ExecutorState.HOME_FOLDER_CHANGE_COLOR_VIEW);
        }
    }),
    HOME_FOLDER_SELECTED_VIEW("HomeFolderSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderSelectedViewStateHandler(ExecutorState.HOME_FOLDER_SELECTED_VIEW);
        }
    }),
    HOME_FOLDER_ADD_ICON("HomeFolderAddIcon", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderAddIconStateHandler(ExecutorState.HOME_FOLDER_ADD_ICON);
        }
    }),
    HOME_FOLDER_ADD_ICON_VIEW("HomeFolderAddIconView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderAddIconViewStateHandler(ExecutorState.HOME_FOLDER_ADD_ICON_VIEW);
        }
    }),
    HOME_FOLDER_REMOVE_SHORTCUT("HomeFolderRemoveShortcut", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderRemoveShortcutStateHandler(ExecutorState.HOME_FOLDER_REMOVE_SHORTCUT);
        }
    }),
    HOME_FOLDER_CLEAR_BADGE("HomeFolderClearBadge", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderClearBadgeStateHandler(ExecutorState.HOME_FOLDER_CLEAR_BADGE);
        }
    }),
    HOME_FOLDER_ADD_ICON_SEARCH_VIEW("HomeFolderAddIconSearchView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderAddIconSearchViewStateHandler(ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_VIEW);
        }
    }),
    HOME_FOLDER_ADD_ICON_SEARCH_RESULT("HomeFolderAddIconSearchResult", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderAddIconSearchViewResultStateHandler(ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_RESULT);
        }
    }),
    HOME_FOLDER_REMOVE_ICON("HomeFolderRemoveIcon", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderRemoveIconStateHandler(ExecutorState.HOME_FOLDER_REMOVE_ICON);
        }
    }),
    HOME_FOLDER_SINGLE_APP_REMOVE_ICON("HomeFolderSingleAppRemoveIcon", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderSingleAppRemoveIconStateHandler(ExecutorState.HOME_FOLDER_SINGLE_APP_REMOVE_ICON);
        }
    }),
    HOME_FOLDER_SINGLE_APP_SELECTED_VIEW("HomeFolderSingleAppSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderSingleAppSelectedViewStateHandler(ExecutorState.HOME_FOLDER_SINGLE_APP_SELECTED_VIEW);
        }
    }),
    HOME_EDIT("HomePageEditView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageEditViewStateHandler(ExecutorState.HOME_EDIT);
        }
    }),
    HOME_EDIT_SET_MAIN_PAGE("HomePageSetMainPage", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageSetMainPageStateHandler(ExecutorState.HOME_EDIT_SET_MAIN_PAGE);
        }
    }),
    HOME_EDIT_ADD_PAGE("HomePageAddPage", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageAddPageStateHandler(ExecutorState.HOME_EDIT_ADD_PAGE);
        }
    }),
    HOME_EDIT_REMOVE_PAGE("HomePageRemovePage", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageRemovePageStateHandler(ExecutorState.HOME_EDIT_REMOVE_PAGE);
        }
    }),
    HOME_EDIT_REMOVE_PAGE_POPUP("HomePageRemovePopup", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageRemovePopupStateHandler(ExecutorState.HOME_EDIT_REMOVE_PAGE_POPUP);
        }
    }),
    HOME_EDIT_CHANGE_ORDER("HomePageChangeOrder", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageChangeOrderStateHandler(ExecutorState.HOME_EDIT_CHANGE_ORDER);
        }
    }),
    HOME_EDIT_ICON_ALIGN_TOP("HomePageAutoReArrangeToTop", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageAutoReArrangeToTopStateHandler(ExecutorState.HOME_EDIT_ICON_ALIGN_TOP);
        }
    }),
    HOME_EDIT_ICON_ALIGN_BOTTOM("HomePageAutoReArrangeToBottom", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageAutoReArrangeToBottomStateHandler(ExecutorState.HOME_EDIT_ICON_ALIGN_BOTTOM);
        }
    }),
    HOME_PAGE_WIDGET_VIEW("HomePageWidgetView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetViewStateHandler(ExecutorState.HOME_PAGE_WIDGET_VIEW);
        }
    }),
    HOME_PAGE_WIDGET_SEARCH_VIEW("HomePageWidgetSearchView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetSearchViewStateHandler(ExecutorState.HOME_PAGE_WIDGET_SEARCH_VIEW);
        }
    }),
    HOME_PAGE_WIDGET_SEARCH_RESULT("HomePageWidgetSearchResult", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetSearchResultStateHandler(ExecutorState.HOME_PAGE_WIDGET_SEARCH_RESULT);
        }
    }),
    HOME_PAGE_WIDGET_EDIT_VIEW("HomePageWidgetEditView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetEditViewStateHandler(ExecutorState.HOME_WIDGET_RESIZE_VIEW);
        }
    }),
    HOME_PAGE_WIDGET_UNINSALL("HomePageWidgetUninstall", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetUninstallStateHandler(ExecutorState.HOME_WIDGET_RESIZE_VIEW);
        }
    }),
    HOME_PAGE_WIDGET_UNINSALL_POPUP("HomePageWidgetUninstallPopup", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetUninstallPopupStateHandler(ExecutorState.HOME_WIDGET_RESIZE_VIEW);
        }
    }),
    HOME_PAGE_WIDGET_ADDED("HomePageWidgetAdded", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageWidgetAddedStateHandler(ExecutorState.HOME_PAGE_WIDGET_ADDED);
        }
    }),
    HOME_PAGE_AUTO_RE_ARRANGE_POPUP("HomePageAutoReArrangePopup", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomePageAutoReArrangePopupStateHandler(ExecutorState.HOME_PAGE_AUTO_RE_ARRANGE_POPUP);
        }
    }),
    HOME_SETTINGS("HomeSettingsView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsViewStateHandler(ExecutorState.HOME_SETTINGS);
        }
    }),
    HOME_SETTINGS_HIDE_APPS_VIEW("HomeSettingsHideAppsView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsHideAppsViewStateHandler(ExecutorState.HOME_SETTINGS_HIDE_APPS_VIEW);
        }
    }),
    HOME_SETTINGS_HIDE_APPS("HomeSettingsHideApps", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsHideAppsStateHandler(ExecutorState.HOME_SETTINGS_HIDE_APPS);
        }
    }),
    HOME_SETTINGS_UNHIDE_APPS("HomeSettingsUnHideApps", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsUnHideAppsStateHandler(ExecutorState.HOME_SETTINGS_UNHIDE_APPS);
        }
    }),
    HOME_SETTINGS_SHOW_APPS_BUTTON_ON("HomeSettingsAppsButtonOn", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsShowAppsButtonOnStateHandler(ExecutorState.HOME_SETTINGS_SHOW_APPS_BUTTON_ON);
        }
    }),
    HOME_SETTINGS_SHOW_APPS_BUTTON_OFF("HomeSettingsAppsButtonOff", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsShowAppsButtonOffStateHandler(ExecutorState.HOME_SETTINGS_SHOW_APPS_BUTTON_OFF);
        }
    }),
    HOME_SETTINGS_STYLE_VIEW("HomeSettingsStyleSettingView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsStyleSettingViewStateHandler(ExecutorState.HOME_SETTINGS_STYLE_VIEW);
        }
    }),
    HOME_SETTINGS_CHANGE_TO_HOME_ONLY("HomeSettingsChangeToHomescreenOnly", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsChangeToHomescreenOnlyStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_TO_HOME_ONLY);
        }
    }),
    HOME_SETTINGS_CHANGE_TO_HOME_APPS("HomeSettingsChangeToSamsungGalaxyHome", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsChangeToSamsungGalaxyHomeStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_TO_HOME_APPS);
        }
    }),
    HOME_SETTINGS_GRID_SETTING_VIEW("HomeSettingsGridSettingView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsGridSettingViewStateHandler(ExecutorState.HOME_SETTINGS_GRID_SETTING_VIEW);
        }
    }),
    HOME_SETTINGS_APPS_GRID_SETTING_VIEW("HomeSettingsAppsGridSettingView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsAppsGridSettingViewStateHandler(ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW);
        }
    }),
    HOME_SETTINGS_CHANGE_APPS_GRID("HomeSettingsChangeAppsGrid", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsChangeAppsGridStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_APPS_GRID);
        }
    }),
    HOME_SETTINGS_CHANGE_GRID("HomeSettingsChangeGrid", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsChangeGridStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_GRID);
        }
    }),
    HOME_SETTINGS_CHANGE_TO_HOMESCREEN_ONLY_POPUP("HomeSettingsChangeToHomescreenOnlyPopup", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsChangeToHomescreenOnlyPopupStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_TO_HOMESCREEN_ONLY_POPUP);
        }
    }),
    HOME_SETTINGS_ABOUT_HOME("HomeSettingsAboutHome", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsAboutHomeStateHandler(ExecutorState.HOME_SETTINGS_ABOUT_HOME);
        }
    }),
    HOME_SETTINGS_QUICKOPEN_NOTIPANEL_ON("HomeSettingsQuickOpenNotiPanelOn", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsQuickOpenNotiPanelOnStateHandler(ExecutorState.HOME_SETTINGS_QUICKOPEN_NOTIPANEL_ON);
        }
    }),
    HOME_SETTINGS_QUICKOPEN_NOTIPANEL_OFF("HomeSettingsQuickOpenNotiPanelOff", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsQuickOpenNotiPanelOffStateHandler(ExecutorState.HOME_SETTINGS_QUICKOPEN_NOTIPANEL_OFF);
        }
    }),
    HOME_SETTINGS_CHANGE_TO_HOME_APPS_POPUP("HomeSettingsChangeToHomeAppsScreenPopup", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingChangeToHomeAppsScreenPopupStateHandler(ExecutorState.HOME_SETTINGS_CHANGE_TO_HOME_APPS_POPUP);
        }
    }),
    HOME_SINGLE_APP_UNINSTALL_DISABLE("HomeSingleAppUninstallDisable", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppUninstallDisableStateHandler(ExecutorState.HOME_SINGLE_APP_UNINSTALL_DISABLE);
        }
    }),
    HOME_SINGLE_APP_UNINSTALL_DISABLE_POP_UP("HomeSingleAppUninstallDisabledPopUp", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppUninstallDisableStateHandler(ExecutorState.HOME_SINGLE_APP_UNINSTALL_DISABLE_POP_UP);
        }
    }),
    HOME_SINGLE_APP_TO_SLEEP("HomeSingleAppPutToSleepMode", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppPutToSleepModeStateHandler(ExecutorState.HOME_SINGLE_APP_TO_SLEEP);
        }
    }),
    HOME_SINGLE_APP_TO_SECUREFOLDER("HomeSingleAppAddToSecureFolder", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppAddToSecureFolderStateHandler(ExecutorState.HOME_SINGLE_APP_TO_SECUREFOLDER);
        }
    }),
    HOME_SINGLE_APP_CLEAR_BADGE("HomeSingleAppClearBadge", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppClearBadgeStateHandler(ExecutorState.HOME_SINGLE_APP_CLEAR_BADGE);
        }
    }),
    APPS_SINGLE_APP_UNINSTALL_DISABLE("AppsSingleAppUninstallDisabled", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppUninstallDisableStateHandler(ExecutorState.APPS_SINGLE_APP_UNINSTALL_DISABLE);
        }
    }),
    APPS_SINGLE_APP_TO_SLEEP("AppsSingleAppPutToSleepMode", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppPutToSleepModeStateHandler(ExecutorState.APPS_SINGLE_APP_TO_SLEEP);
        }
    }),
    APPS_SINGLE_APP_TO_SECUREFOLDER("AppsSingleAppAddToSecureFolder", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppAddToSecureFolderStateHandler(ExecutorState.APPS_SINGLE_APP_TO_SECUREFOLDER);
        }
    }),
    APPS_SINGLE_APP_CLEAR_BADGE("AppsSingleAppClearBadge", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsSingleAppClearBadgeStateHandler(ExecutorState.APPS_SINGLE_APP_CLEAR_BADGE);
        }
    }),
    HOME_WIDGET_SELECTED_VIEW("HomeWidgetSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeWidgetSelectedViewStateHandler(ExecutorState.HOME_WIDGET_SELECTED_VIEW);
        }
    }),
    HOME_WIDGET_REMOVE("HomeRemoveWidget", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeRemoveWidgetStateHandler(ExecutorState.HOME_WIDGET_REMOVE);
        }
    }),
    HOME_WIDGET_MOVE("HomeWidgetMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeWidgetMoveStateHandler(ExecutorState.HOME_WIDGET_MOVE);
        }
    }),
    HOME_WIDGET_RESIZE_VIEW("HomeWidgetResizeView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeWidgetResizeViewStateHandler(ExecutorState.HOME_WIDGET_RESIZE_VIEW);
        }
    }),
    APPS_FOLDER_VIEW("AppsFolderView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderViewStateHandler(ExecutorState.APPS_FOLDER_VIEW);
        }
    }),
    APPS_FOLDER_MOVE("AppsFolderMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderMoveStateHandler(ExecutorState.APPS_FOLDER_MOVE);
        }
    }),
    APPS_FOLDER_PAGE_MOVE("AppsFolderPageMove", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderPageMoveStateHandler(ExecutorState.APPS_FOLDER_PAGE_MOVE);
        }
    }),
    APPS_FOLDER_CHANGE_TITLE("AppsFolderEditName", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderEditNameStateHandler(ExecutorState.APPS_FOLDER_CHANGE_TITLE);
        }
    }),
    APPS_FOLDER_CHANGE_COLOR_VIEW("AppsFolderChangeBackgroundColorSelectView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderChangeBackgroundColorSelectViewStateHandler(ExecutorState.APPS_FOLDER_CHANGE_COLOR_VIEW);
        }
    }),
    APPS_FOLDER_SELECTED_VIEW("AppsFolderSelectedView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderSelectedViewStateHandler(ExecutorState.APPS_FOLDER_SELECTED_VIEW);
        }
    }),
    APPS_FOLDER_ADD_ICON("AppsFolderAddIcon", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderAddIconStateHandler(ExecutorState.APPS_FOLDER_ADD_ICON);
        }
    }),
    APPS_FOLDER_ADD_ICON_VIEW("AppsFolderAddIconView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderAddIconViewStateHandler(ExecutorState.APPS_FOLDER_ADD_ICON_VIEW);
        }
    }),
    APPS_FOLDER_CLEAR_BADGE("AppsFolderClearBadge", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderClearBadgeStateHandler(ExecutorState.APPS_FOLDER_CLEAR_BADGE);
        }
    }),
    APPS_FOLDER_ADD_ICON_SEARCH_VIEW("AppsFolderAddIconSearchView", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderAddIconSearchViewStateHandler(ExecutorState.APPS_FOLDER_ADD_ICON_SEARCH_VIEW);
        }
    }),
    APPS_FOLDER_ADD_ICON_SEARCH_RESULT("AppsFolderAddIconSearchResult", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderAddIconSearchViewResultStateHandler(ExecutorState.APPS_FOLDER_ADD_ICON_SEARCH_RESULT);
        }
    }),
    APPS_FOLDER_REMOVE_ICON("AppsFolderRemoveIcon", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderRemoveIconStateHandler(ExecutorState.APPS_FOLDER_REMOVE_ICON);
        }
    }),
    APPS_FOLDER_REMOVE("AppsFolderRemove", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderRemoveStateHandler(ExecutorState.APPS_FOLDER_REMOVE);
        }
    }),
    APPS_FOLDER_MAKE_SHORTCUT("AppsFolderMakeShortcut", new StateHandlerCreator() {
        public StateHandler create() {
            return new AppsFolderMakeShortcutStateHandler(ExecutorState.APPS_FOLDER_MAKE_SHORTCUT);
        }
    }),
    HOME_SETTINGS_BADGE_MANAGE_VIEW("HomeSettingsBadgeManagementView", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsBadgeManagementViewStateHandler(ExecutorState.HOME_SETTINGS_BADGE_MANAGE_VIEW);
        }
    }),
    HOME_SETTINGS_ENABLE_APPS_BADGE("HomeSettingsBadgeAllAppsEnable", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsBadgeAllAppsEnableStateHandler(ExecutorState.HOME_SETTINGS_ENABLE_APPS_BADGE);
        }
    }),
    HOME_SETTINGS_DISABLE_APPS_BADGE("HomeSettingsBadgeAllAppsDisable", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsBadgeAllAppsDisableStateHandler(ExecutorState.HOME_SETTINGS_DISABLE_APPS_BADGE);
        }
    }),
    HOME_SETTINGS_ENABLE_SINGLE_APP_BADGE("HomeSettingsBadgeSingleAppEnable", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsBadgeSingleAppEnableStateHandler(ExecutorState.HOME_SETTINGS_ENABLE_SINGLE_APP_BADGE);
        }
    }),
    HOME_SETTINGS_DISABLE_SINGLE_APP_BADGE("HomeSettingsBadgeSingleAppDisable", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSettingsBadgeSingleAppDisableStateHandler(ExecutorState.HOME_SETTINGS_DISABLE_SINGLE_APP_BADGE);
        }
    }),
    HOME_SINGLE_APP_LOCK("HomeSingleAppLock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppLockStateHandler(ExecutorState.HOME_SINGLE_APP_LOCK);
        }
    }),
    HOME_SINGLE_APP_UNLOCK("HomeSingleAppUnlock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppUnlockStateHandler(ExecutorState.HOME_SINGLE_APP_UNLOCK);
        }
    }),
    APPS_SINGLE_APP_LOCK("AppsSingleAppLock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppLockStateHandler(ExecutorState.APPS_SINGLE_APP_LOCK);
        }
    }),
    APPS_SINGLE_APP_UNLOCK("AppsSingleAppUnlock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeSingleAppUnlockStateHandler(ExecutorState.APPS_SINGLE_APP_UNLOCK);
        }
    }),
    HOME_FOLDER_LOCK("HomeFolderLock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderLockStateHandler(ExecutorState.HOME_FOLDER_LOCK);
        }
    }),
    HOME_FOLDER_UNLOCK("HomeFolderUnlock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderUnlockStateHandler(ExecutorState.HOME_FOLDER_UNLOCK);
        }
    }),
    APPS_FOLDER_LOCK("AppsFolderLock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderLockStateHandler(ExecutorState.APPS_FOLDER_LOCK);
        }
    }),
    APPS_FOLDER_UNLOCK("AppsFolderUnlock", new StateHandlerCreator() {
        public StateHandler create() {
            return new HomeFolderUnlockStateHandler(ExecutorState.APPS_FOLDER_UNLOCK);
        }
    });
    
    private StateHandlerCreator mCreator;
    private String mStateId;

    private ExecutorState(String stateId, StateHandlerCreator creator) {
        this.mStateId = stateId;
        this.mCreator = creator;
    }

    StateHandlerCreator getCreator() {
        return this.mCreator;
    }

    public String toString() {
        return this.mStateId;
    }
}
