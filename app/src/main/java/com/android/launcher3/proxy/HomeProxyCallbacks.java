package com.android.launcher3.proxy;

import android.content.ComponentName;
import android.view.View;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import java.util.ArrayList;

public interface HomeProxyCallbacks extends BaseProxyCallbacks {
    boolean addHomeWidget(PendingAddItemInfo pendingAddItemInfo);

    void addNewHomePageInOverViewMode();

    void addNewPage();

    void alignHomeIcon(int i, boolean z);

    void changeFolderTitle(ItemInfo itemInfo, String str);

    void changeHomePageOrder(int i, int i2);

    void changeScreengrid(String str);

    boolean checkAbleAlignIcon(int i, boolean z);

    boolean checkMatchGridOption(String str);

    boolean checkNeedDisplayAutoalignDialog();

    boolean checkValidGridOption(String str);

    void createShortcut(ComponentName componentName, ItemInfo itemInfo, int i);

    void enterHomeEditView();

    void enterHomeSettingGridSettingView();

    boolean enterWidgetResizeMode(ItemInfo itemInfo);

    void exitSubState();

    int getDefaultPage();

    int getFolderItemCountByTitle(String str);

    int getPageIndexForScreenId(long j);

    ArrayList<ItemInfo> getWidgetItemsInfoByComponentName(ComponentName componentName);

    ArrayList<ItemInfo> getWidgetItemsInfoByPackageName(String str);

    ArrayList<ItemInfo> getWidgetItemsInfoByTitle(String str);

    View getWidgetView(ComponentName componentName);

    boolean hasPageDeleteButton(int i);

    boolean hasPageEmptySpace(int i, int i2, int i3);

    boolean isEmptyPage(int i);

    int moveItem(View view, int i);

    void moveItemFromFolder(IconInfo iconInfo);

    void movePage(int i, boolean z);

    boolean neededToAdjustZeroPage();

    void removeCurrentPage();

    void removeShortcut(ItemInfo itemInfo);

    void removeWidget(ItemInfo itemInfo);

    void setAsMainPage(int i);
}
