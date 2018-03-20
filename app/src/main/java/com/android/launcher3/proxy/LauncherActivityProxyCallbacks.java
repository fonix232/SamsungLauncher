package com.android.launcher3.proxy;

import com.android.launcher3.common.base.item.ItemInfo;
import java.util.ArrayList;

public interface LauncherActivityProxyCallbacks {
    void addToSecureFolder(ItemInfo itemInfo);

    boolean canAppAddToSecureFolder(ItemInfo itemInfo);

    void changeHomeStyle(boolean z);

    void clearBadge(ItemInfo itemInfo);

    boolean enableAllAppsBadge(boolean z);

    boolean enableSingleAppBadge(String str, boolean z);

    void enterBadgeManagementView();

    void enterHomeAboutPageView();

    void enterHomeSettingChangeModeView();

    void enterHomeSettingView();

    void hideApps(ArrayList<ItemInfo> arrayList);

    void hideAppsButton();

    boolean isAllAppsBadgeSwitchChecked();

    boolean isAlreadySleepMode(ItemInfo itemInfo);

    boolean isAvailableSleepMode(ItemInfo itemInfo);

    boolean isEnableAppsButton();

    boolean isHomeOnlyMode();

    boolean isSecureFolderSetup();

    boolean isSingleAppBadgeChecked(String str);

    void putToSleepMode(ItemInfo itemInfo);

    void setNotificationPanelExpansionEnabled(boolean z);

    void showAppsButton();

    void unHideApps(ArrayList<ItemInfo> arrayList);

    void uninstallOrDisableApp(ItemInfo itemInfo);
}
