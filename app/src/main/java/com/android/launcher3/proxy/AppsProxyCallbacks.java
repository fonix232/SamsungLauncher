package com.android.launcher3.proxy;

import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;

public interface AppsProxyCallbacks extends BaseProxyCallbacks {
    void changeFolderTitle(ItemInfo itemInfo, String str);

    void changeScreengrid(String str);

    boolean checkMatchGridOption(String str);

    boolean checkValidGridOption(String str);

    void deleteFolder(FolderInfo folderInfo);

    int getPageCount();

    ViewType getViewType();

    boolean hasPageEmptySpace(int i);

    void hideViewTypePopup();

    void moveItem(IconView iconView, int i);

    void moveItemInFolder(IconView iconView, int i, int i2);

    int moveItemToFollowedEmptyPage(IconView iconView, int i);

    void removeItem(ItemInfo itemInfo);

    void setViewType(ViewType viewType);

    void showAppsGridSettingView();

    void showTidyUpPreview();

    void showViewTypePopup();

    void startSecureFolder();

    void tidyUpPages();
}
