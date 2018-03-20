package com.android.launcher3.proxy;

import android.content.ComponentName;
import android.util.Log;
import android.view.View;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.util.ItemListHelper;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LauncherProxy {
    public static final int INVALID_VALUE = -999;
    public static final int LAUNCHER_PROXY_NOT_READY = -1;
    public static final int LAUNCHER_PROXY_NOT_SUPPORTED_STATE = -2;
    public static final int LAUNCHER_PROXY_PARAMS_ERROR = -3;
    public static final int LAUNCHER_PROXY_RESULT_OK = 0;
    public static final int PAGE_MOVE_CURRENT = -3;
    public static final int PAGE_MOVE_DEFAULT = -6;
    public static final int PAGE_MOVE_EMPTY = 0;
    public static final int PAGE_MOVE_FIRST = -4;
    public static final int PAGE_MOVE_LAST = -5;
    public static final int PAGE_MOVE_NEXT = -2;
    public static final int PAGE_MOVE_PREV = -1;
    private static final String TAG = LauncherProxy.class.getSimpleName();
    private AppsPickerProxyCallbacks mAppsPickerProxyCallback;
    private AppsProxyCallbacks mAppsProxyCallbacks;
    private FolderProxyCallbacks mFolderProxyCallbacks;
    private HomeProxyCallbacks mHomeProxyCallbacks;
    private LauncherActivityProxyCallbacks mLauncherActivityProxyCallbacks;
    private StageManagerProxyCallbacks mStageManagerProxyCallbacks;
    private WidgetProxyCallbacks mWidgetProxyCallbacks;

    public interface AppInfo {
        ComponentName getComponentName();

        ItemInfo getItemInfo();

        String getName();

        int getOrdinalNumber();

        void setName(String str);
    }

    public void setLauncherActivityProxyCallbacks(LauncherActivityProxyCallbacks launcherActivityProxyCallback) {
        this.mLauncherActivityProxyCallbacks = launcherActivityProxyCallback;
    }

    public void setHomeProxyCallbacks(HomeProxyCallbacks homeProxyCallback) {
        this.mHomeProxyCallbacks = homeProxyCallback;
    }

    public void setAppsProxyCallbacks(AppsProxyCallbacks appsProxyCallback) {
        this.mAppsProxyCallbacks = appsProxyCallback;
    }

    public void setFolderProxyCallbacks(FolderProxyCallbacks folderProxyCallback) {
        this.mFolderProxyCallbacks = folderProxyCallback;
    }

    public void setWidgetProxyCallbacks(WidgetProxyCallbacks widgetProxyCallback) {
        this.mWidgetProxyCallbacks = widgetProxyCallback;
    }

    public void setAppsPickerProxyCallbacks(AppsPickerProxyCallbacks appsPickerProxyCallback) {
        this.mAppsPickerProxyCallback = appsPickerProxyCallback;
    }

    public void setStageManagerProxyCallbacks(StageManagerProxyCallbacks stageManagerProxyCallback) {
        this.mStageManagerProxyCallbacks = stageManagerProxyCallback;
    }

    public List<ItemInfo> getHomeItemInfoByStateAppInfo(AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        List<ItemInfo> itemList = new ArrayList();
        if (appInfo.getComponentName() == null) {
            return appInfo.getName() != null ? getItemsInfoByTitle(appInfo.getName(), -100) : null;
        } else {
            List<ItemInfo> desktopItemList = getItemsInfoByComponentName(appInfo.getComponentName(), -100);
            List<ItemInfo> hotseatItemList = getItemsInfoByComponentName(appInfo.getComponentName(), Favorites.CONTAINER_HOTSEAT);
            itemList.addAll(desktopItemList);
            itemList.addAll(hotseatItemList);
            return itemList;
        }
    }

    public List<ItemInfo> getAppsItemInfoByStateAppInfo(AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        if (appInfo.getComponentName() != null) {
            return getItemsInfoByComponentName(appInfo.getComponentName(), Favorites.CONTAINER_APPS);
        }
        if (appInfo.getName() != null) {
            return getItemsInfoByTitle(appInfo.getName(), Favorites.CONTAINER_APPS);
        }
        return null;
    }

    public int getOpenedHomeFolderPage() {
        FolderIconView folderIconView = this.mFolderProxyCallbacks.getOpenedFolderIconView();
        if (folderIconView == null) {
            return 0;
        }
        return this.mHomeProxyCallbacks.getPageIndexForScreenId(((ItemInfo) folderIconView.getTag()).screenId);
    }

    public int getOpenedAppsFolderPage() {
        FolderInfo folder = this.mFolderProxyCallbacks.getOpenedFolder();
        if (folder != null) {
            return (int) folder.screenId;
        }
        return 0;
    }

    public ArrayList<ItemInfo> getFolderItemInfoByStateAppInfo(AppInfo appInfo) {
        if (this.mFolderProxyCallbacks == null) {
            return null;
        }
        FolderIconView folderIconView = this.mFolderProxyCallbacks.getOpenedFolderIconView();
        ArrayList<ItemInfo> itemList = new ArrayList();
        if (folderIconView == null) {
            return itemList;
        }
        for (ItemInfo info : folderIconView.getFolderInfo().contents) {
            if (appInfo.getComponentName() != null) {
                if (!(info.getIntent() == null || info.getIntent().getComponent() == null || !info.getIntent().getComponent().toString().equalsIgnoreCase(appInfo.getComponentName().toString()))) {
                    itemList.add(info);
                }
            } else if (!(appInfo.getName() == null || info.title == null || info.title.toString().replaceAll("\\s", "").compareToIgnoreCase(appInfo.getName().replaceAll("\\s", "")) != 0)) {
                itemList.add(info);
            }
        }
        return itemList;
    }

    public View getWidgetView(ComponentName cn) {
        return this.mHomeProxyCallbacks.getWidgetView(cn);
    }

    public View getItemViewByTitle(String itemName) {
        return this.mHomeProxyCallbacks.getItemViewByTitle(itemName);
    }

    public View getItemViewByComponentName(ComponentName cn) {
        return this.mHomeProxyCallbacks.getItemViewByComponentName(cn);
    }

    public FolderIconView getFolderItemViewByTitle(String folderName) {
        return this.mHomeProxyCallbacks.getFolderItemViewByTitle(folderName);
    }

    public int getHomeFolderItemCountByTitle(String folderName) {
        return this.mHomeProxyCallbacks.getFolderItemCountByTitle(folderName);
    }

    public final int showAppInfo(ComponentName componentName, UserHandleCompat user) {
        if (componentName == null) {
            return -3;
        }
        LauncherAppsCompat.getInstance(LauncherAppState.getInstance().getContext()).showAppDetailsForProfile(componentName, user);
        return 0;
    }

    public final int contactUs() {
        Utilities.startContactUsActivity(LauncherAppState.getInstance().getContext());
        return 0;
    }

    public final int openHomeFolder(String folderName) {
        if (folderName == null) {
            return -3;
        }
        FolderIconView iv = this.mHomeProxyCallbacks.getFolderItemViewByTitle(folderName);
        if (iv == null) {
            return -3;
        }
        this.mStageManagerProxyCallbacks.openFolder(iv);
        return 0;
    }

    public final int closeFolder() {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 5) {
            return -1;
        }
        this.mStageManagerProxyCallbacks.closeFolder();
        return 0;
    }

    public final int openFolderColorPanel() {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 5 || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        this.mFolderProxyCallbacks.openBackgroundColorView();
        return 0;
    }

    public final int moveFolderPage(int pageNumber, int pageDirection) {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 5 || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mFolderProxyCallbacks, pageDirection);
        }
        if (!isFolderValidPage(p)) {
            return -3;
        }
        this.mFolderProxyCallbacks.movePage(p);
        return 0;
    }

    public final int getAppsPageCount() {
        return this.mAppsProxyCallbacks.getPageCount();
    }

    public int getOpenedFolderPageCount() {
        if (this.mFolderProxyCallbacks == null || this.mFolderProxyCallbacks.getPagedView() == null) {
            return 0;
        }
        return this.mFolderProxyCallbacks.getPagedView().getPageCount();
    }

    public int mapDirectionToPage(int itemPage, int direction, boolean isHome) {
        PagedView pv = (isHome ? this.mHomeProxyCallbacks : this.mAppsProxyCallbacks).getPagedView();
        switch (direction) {
            case -6:
                if (isHome) {
                    return this.mHomeProxyCallbacks.getDefaultPage();
                }
                return -1;
            case -5:
                return pv.getPageCount() - 1;
            case -4:
                return 0;
            case -3:
                return pv.getCurrentPage();
            case -2:
                return itemPage + 1;
            case -1:
                return itemPage - 1;
            default:
                return -1;
        }
    }

    public int getPageNumberToMove(BaseProxyCallbacks cb, int pageNumber) {
        Log.d(TAG, "pageNumber=" + pageNumber);
        PagedView pv = cb.getPagedView();
        int page = pv.getCurrentPage();
        if (-4 == pageNumber) {
            page = 0;
        } else if (-5 == pageNumber) {
            page = pv.getPageCount() - 1;
        } else if (-1 == pageNumber) {
            page--;
        } else if (-2 == pageNumber) {
            page++;
        } else if (-6 == pageNumber) {
            if (cb instanceof HomeProxyCallbacks) {
                page = this.mHomeProxyCallbacks.getDefaultPage();
            } else {
                page = 0;
            }
        } else if (-3 == pageNumber) {
            page = pv.getCurrentPage();
        } else {
            page = pageNumber - 1;
        }
        if (page < 0) {
            page = 0;
        }
        if (page >= pv.getPageCount()) {
            page = pv.getPageCount() - 1;
        }
        Log.d(TAG, "result page=" + page);
        return page;
    }

    public final int getPageNumberInOverview(int pageNumber) {
        if (pageNumber < 0) {
            return getPageNumberInOverview(this.mHomeProxyCallbacks.getPagedView().getCurrentPage(), pageNumber);
        }
        return pageNumber;
    }

    public int getPageNumberInOverview(int pageNumber, int pageDirection) {
        Log.d(TAG, "getPageNumberInOverview : pageNumber=" + pageNumber);
        boolean hasZeropage = this.mHomeProxyCallbacks.neededToAdjustZeroPage();
        PagedView pv = this.mHomeProxyCallbacks.getPagedView();
        int page = pv.getCurrentPage();
        switch (pageDirection) {
            case -6:
                page = this.mHomeProxyCallbacks.getDefaultPage();
                break;
            case -5:
                page = pv.getPageCount() - 2;
                break;
            case -4:
                page = hasZeropage ? 1 : 0;
                break;
            case -3:
                page = pv.getCurrentPage();
                break;
            case -2:
                page++;
                break;
            case -1:
                page--;
                break;
            default:
                if (!hasZeropage) {
                    page = pageNumber - 1;
                    break;
                }
                page = pageNumber;
                break;
        }
        Log.d(TAG, "getPageNumberInOverview : result page=" + page);
        return page;
    }

    public ArrayList<ItemInfo> getWidgetItemInfo(AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        if (appInfo.getComponentName() != null) {
            return this.mWidgetProxyCallbacks.getWidgetItemsInfoByComponentName(appInfo.getComponentName());
        }
        if (appInfo.getName() != null) {
            return this.mWidgetProxyCallbacks.getWidgetItemsInfoByTitle(appInfo.getName());
        }
        return null;
    }

    public ArrayList<ItemInfo> getHomeWidgetItemInfo(AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        if (appInfo.getComponentName() != null) {
            return this.mHomeProxyCallbacks.getWidgetItemsInfoByComponentName(appInfo.getComponentName());
        }
        if (appInfo.getName() != null) {
            return this.mHomeProxyCallbacks.getWidgetItemsInfoByTitle(appInfo.getName());
        }
        return null;
    }

    public final boolean isHomeValidPage(int pageNumber) {
        return pageNumber >= 0 && pageNumber < this.mHomeProxyCallbacks.getPagedView().getPageCount();
    }

    public final int getHomePageCount() {
        PagedView pv = this.mHomeProxyCallbacks.getPagedView();
        if (pv != null) {
            return pv.getPageCount();
        }
        return 1;
    }

    public final int getHomePageCountInOverviewMode() {
        int pageCount = this.mHomeProxyCallbacks.getPagedView().getPageCount() - 1;
        if (this.mHomeProxyCallbacks.neededToAdjustZeroPage()) {
            return pageCount - 1;
        }
        return pageCount;
    }

    public final boolean hasPageDeleteButton(int page) {
        return this.mHomeProxyCallbacks.hasPageDeleteButton(page);
    }

    public final boolean isEmptyPage(int page) {
        return this.mHomeProxyCallbacks.isEmptyPage(page);
    }

    public final int getHomeCurrentPage() {
        return this.mHomeProxyCallbacks.getPagedView().getCurrentPage();
    }

    public final boolean hasHomeEmptySpace(int pageNumber, int pageDirection, int spanX, int spanY) {
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mHomeProxyCallbacks, pageDirection);
        }
        return this.mHomeProxyCallbacks.hasPageEmptySpace(p, spanX, spanY);
    }

    public final boolean isHomeValidPageInOverview(int pageNumber, boolean exceptZeropage) {
        int firstPage;
        int pageCount = this.mHomeProxyCallbacks.getPagedView().getPageCount();
        if (this.mHomeProxyCallbacks.neededToAdjustZeroPage() && exceptZeropage) {
            firstPage = 1;
        } else {
            firstPage = 0;
        }
        if (pageNumber < firstPage || pageNumber >= pageCount - 1) {
            return false;
        }
        return true;
    }

    public final boolean hasAppsEmptySpace(int pageNumber, int pageDirection) {
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        return this.mAppsProxyCallbacks.hasPageEmptySpace(p);
    }

    public final boolean isFolderValidPage(int pageNumber) {
        PagedView pv = this.mFolderProxyCallbacks.getPagedView();
        if (pv == null) {
            return false;
        }
        int pageCount = pv.getPageCount();
        if (pageNumber < 0 || pageNumber >= pageCount) {
            return false;
        }
        return true;
    }

    public final boolean hasItemInHome(AppInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        List<ItemInfo> items = getHomeItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() <= 0) {
            return false;
        }
        return true;
    }

    public final boolean hasFolderInHome(String folderName) {
        if (folderName == null || this.mHomeProxyCallbacks.getFolderItemViewByTitle(folderName) == null) {
            return false;
        }
        return true;
    }

    public final boolean hasItemInFolder(AppInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        ArrayList<ItemInfo> items = getFolderItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() <= 0) {
            return false;
        }
        return true;
    }

    public final int moveToHomePage(int pageNumber, int pageDirection) {
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mHomeProxyCallbacks, pageDirection);
        }
        if (pageDirection == INVALID_VALUE && this.mHomeProxyCallbacks.neededToAdjustZeroPage()) {
            p++;
        }
        if (!isHomeValidPage(p)) {
            return -3;
        }
        this.mHomeProxyCallbacks.movePage(p);
        return 0;
    }

    public final int moveToHomePage(int pageNumber) {
        this.mHomeProxyCallbacks.movePage(pageNumber);
        return 0;
    }

    public final int moveToHomePage(int pageNumber, boolean needAnimation) {
        this.mHomeProxyCallbacks.movePage(pageNumber, needAnimation);
        return 0;
    }

    public final int moveHomePage(AppInfo appInfo) {
        List<ItemInfo> items = getHomeItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() <= 0) {
            return -2;
        }
        this.mHomeProxyCallbacks.movePageToItem((ItemInfo) items.get(0));
        return 0;
    }

    public final int moveHomePageByWidgetItem(AppInfo appInfo) {
        ArrayList<ItemInfo> items = getHomeWidgetItemInfo(appInfo);
        if (items == null || items.size() <= 0) {
            return -2;
        }
        this.mHomeProxyCallbacks.movePageToItem((ItemInfo) items.get(0));
        return 0;
    }

    public final int moveHomeItemToPage(View iv, int pageNumber, int pageDirection, int detailDirection) {
        if (pageNumber <= 0) {
            pageNumber = getPageNumberToMove(this.mHomeProxyCallbacks, pageDirection);
        }
        return this.mHomeProxyCallbacks.moveItem(iv, pageNumber);
    }

    public final int removeHomeShortcut(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        List<ItemInfo> items = getHomeItemInfoByStateAppInfo(appInfo);
        if (items == null) {
            return -2;
        }
        for (ItemInfo i : items) {
            this.mHomeProxyCallbacks.removeShortcut(i);
        }
        return 0;
    }

    public final int removeHomeWidget(ItemInfo itemInfo) {
        if (!(itemInfo instanceof LauncherAppWidgetInfo)) {
            return -3;
        }
        ComponentName cn = ((LauncherAppWidgetInfo) itemInfo).providerName;
        if (cn == null) {
            return -2;
        }
        Iterator it = this.mHomeProxyCallbacks.getWidgetItemsInfoByPackageName(cn.getPackageName()).iterator();
        while (it.hasNext()) {
            this.mHomeProxyCallbacks.removeWidget((ItemInfo) it.next());
        }
        return 0;
    }

    public final int enterWidgetResizeMode(ItemInfo itemInfo) {
        if (!(itemInfo instanceof LauncherAppWidgetInfo)) {
            return -3;
        }
        ComponentName cn = ((LauncherAppWidgetInfo) itemInfo).providerName;
        if (cn == null) {
            return -2;
        }
        Iterator it = this.mHomeProxyCallbacks.getWidgetItemsInfoByPackageName(cn.getPackageName()).iterator();
        if (!it.hasNext()) {
            return 0;
        }
        if (this.mHomeProxyCallbacks.enterWidgetResizeMode((ItemInfo) it.next())) {
            return 0;
        }
        return -2;
    }

    public final int createHomeAppShortcut(AppInfo appInfo, int page) {
        if (appInfo == null) {
            return -3;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items != null && items.size() > 0) {
            appInfo.setName(((ItemInfo) items.get(0)).title.toString());
            if (page >= 0) {
                this.mHomeProxyCallbacks.createShortcut(((ItemInfo) items.get(0)).getIntent().getComponent(), null, page);
            } else {
                this.mHomeProxyCallbacks.createShortcut(((ItemInfo) items.get(0)).getIntent().getComponent(), null, 0);
            }
        }
        return 0;
    }

    public final int createHomeFolderShortcut(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        View view = this.mAppsProxyCallbacks.getFolderItemViewByTitle(appInfo.getName());
        if (view == null) {
            return 0;
        }
        ItemInfo info = (ItemInfo) view.getTag();
        if (info == null) {
            return 0;
        }
        this.mHomeProxyCallbacks.createShortcut(null, info, 0);
        return 0;
    }

    private int hideOrUnHideApps(AppInfo appInfo, boolean isHide) {
        if (appInfo == null || appInfo.getItemInfo() == null) {
            return -3;
        }
        if (this.mStageManagerProxyCallbacks.getTopStageMode() == 6) {
            this.mStageManagerProxyCallbacks.finishStage(6, null);
        }
        ArrayList<ItemInfo> hideItems = new ArrayList();
        hideItems.add(appInfo.getItemInfo());
        if (isHide) {
            this.mLauncherActivityProxyCallbacks.hideApps(hideItems);
        } else {
            this.mLauncherActivityProxyCallbacks.unHideApps(hideItems);
        }
        return 0;
    }

    public final int hideApps(AppInfo appInfo) {
        return hideOrUnHideApps(appInfo, true);
    }

    public final int unHideApps(AppInfo appInfo) {
        return hideOrUnHideApps(appInfo, false);
    }

    public final int changeHomeFolderTitle(String newTitle) {
        if (newTitle == null) {
            return -3;
        }
        if (this.mFolderProxyCallbacks == null || this.mFolderProxyCallbacks.getOpenedFolder() == null) {
            return -1;
        }
        this.mHomeProxyCallbacks.changeFolderTitle(this.mFolderProxyCallbacks.getOpenedFolder(), newTitle);
        this.mFolderProxyCallbacks.changeTitle(newTitle);
        return 0;
    }

    public final int changeAppsFolderTitle(String newTitle) {
        if (newTitle == null) {
            return -3;
        }
        if (this.mFolderProxyCallbacks == null || this.mFolderProxyCallbacks.getOpenedFolder() == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.changeFolderTitle(this.mFolderProxyCallbacks.getOpenedFolder(), newTitle);
        this.mFolderProxyCallbacks.changeTitle(newTitle);
        return 0;
    }

    public final int enterHomeFolderAddApps(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        IconView iv = this.mHomeProxyCallbacks.getFolderItemViewByTitle(appInfo.getName());
        if (!(iv instanceof FolderIconView)) {
            return -3;
        }
        this.mStageManagerProxyCallbacks.enterFolderAddAppsView((FolderIconView) iv);
        return 0;
    }

    public final int enterAppsFolderAddApps(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        IconView iv = this.mAppsProxyCallbacks.getFolderItemViewByTitle(appInfo.getName());
        if (!(iv instanceof FolderIconView)) {
            return -3;
        }
        this.mStageManagerProxyCallbacks.enterFolderAddAppsView((FolderIconView) iv);
        return 0;
    }

    public final int setAddAppsSearchText(String searchText) {
        if (searchText == null) {
            return -3;
        }
        if (this.mAppsPickerProxyCallback.setSearchText(searchText)) {
            return 0;
        }
        return -1;
    }

    public final int addSearchResultItemToFolder() {
        this.mAppsPickerProxyCallback.addResultApps();
        return 0;
    }

    public final int addSearchResultItemToFolder(int ordinalNum) {
        this.mAppsPickerProxyCallback.addResultApps(ordinalNum);
        return 0;
    }

    public final int addSearchResultItemToFolder(boolean anapho) {
        this.mAppsPickerProxyCallback.addResultApps(true);
        return 0;
    }

    public final int getSearchResultListCheckedCount() {
        return this.mAppsPickerProxyCallback.getSearchResultListCheckedCount();
    }

    public final int getSearchResultListCount() {
        return this.mAppsPickerProxyCallback.getSearchResultListCount();
    }

    public final ItemInfo getSearchResultSingleAppInfo() {
        return this.mAppsPickerProxyCallback.getSearchResultSingleAppInfo();
    }

    public final int enterHomeEditView() {
        this.mHomeProxyCallbacks.enterHomeEditView();
        return 0;
    }

    public final int setHomeCurrentAsMainPage() {
        this.mHomeProxyCallbacks.setAsMainPage(this.mHomeProxyCallbacks.getPagedView().getCurrentPage());
        return 0;
    }

    public final int changeHomePageOrder(int fromPage, int toPage) {
        this.mHomeProxyCallbacks.changeHomePageOrder(fromPage, toPage);
        return 0;
    }

    public final int addNewPageInHome() {
        this.mHomeProxyCallbacks.addNewPage();
        return 0;
    }

    public final int addNewHomePageInOverViewMode() {
        this.mHomeProxyCallbacks.addNewHomePageInOverViewMode();
        return 0;
    }

    public final int removeHomeCurrentPage() {
        this.mHomeProxyCallbacks.removeCurrentPage();
        return 0;
    }

    public final int alignHomeIcon(int page, boolean isTop) {
        this.mHomeProxyCallbacks.alignHomeIcon(page, isTop);
        return 0;
    }

    public final int enterWidgetListView() {
        this.mStageManagerProxyCallbacks.enterWidgetListView();
        return 0;
    }

    public final int enterHideAppsView() {
        this.mStageManagerProxyCallbacks.enterHideAppsView();
        return 0;
    }

    public final int enterHomeSettingHomeGridSettingView() {
        this.mStageManagerProxyCallbacks.exitSettingsView();
        this.mHomeProxyCallbacks.enterHomeSettingGridSettingView();
        return 0;
    }

    public final int enterHomeSettingAppsGridSettingView() {
        this.mStageManagerProxyCallbacks.exitSettingsView();
        this.mAppsProxyCallbacks.showAppsGridSettingView();
        return 0;
    }

    public final int changeHomeScreengrid(String gridOption) {
        this.mHomeProxyCallbacks.changeScreengrid(gridOption);
        return 0;
    }

    public final int changeAppsScreengrid(String gridOption) {
        this.mAppsProxyCallbacks.changeScreengrid(gridOption);
        return 0;
    }

    public final boolean checkValidHomeGridOption(String gridOption) {
        return this.mHomeProxyCallbacks.checkValidGridOption(gridOption);
    }

    public final boolean checkValidAppsGridOption(String gridOption) {
        return this.mAppsProxyCallbacks.checkValidGridOption(gridOption);
    }

    public final boolean checkMatchHomeGridOption(String gridOption) {
        return this.mHomeProxyCallbacks.checkMatchGridOption(gridOption);
    }

    public final boolean checkMatchAppsGridOption(String gridOption) {
        return this.mAppsProxyCallbacks.checkMatchGridOption(gridOption);
    }

    public final int searchWidgetList(String keyword) {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 3) {
            return -2;
        }
        return this.mWidgetProxyCallbacks.search(keyword);
    }

    public final int enterWidgetSearchState() {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 3) {
            return -2;
        }
        this.mWidgetProxyCallbacks.enterSearchState();
        return 0;
    }

    public final int enterWidgetUninstallState() {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 3) {
            return -2;
        }
        this.mWidgetProxyCallbacks.enterUninstallState();
        return 0;
    }

    public final int uninstallWidget(ItemInfo info) {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 3 || !(info instanceof PendingAddWidgetInfo)) {
            return -2;
        }
        this.mWidgetProxyCallbacks.uninstallWidget((PendingAddWidgetInfo) info);
        return 0;
    }

    public final int addHomeItemToPage(AppInfo appInfo, int pageNumber, int pageDirection, int detailDirection) {
        if (appInfo == null) {
            return -3;
        }
        if (pageNumber > 0 && !isHomeValidPage(pageNumber)) {
            return -3;
        }
        createHomeAppShortcut(appInfo, pageNumber);
        return 0;
    }

    public final int moveFolderItemToHome(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        ArrayList<ItemInfo> infos = getFolderItemInfoByStateAppInfo(appInfo);
        if (infos == null || infos.size() == 0) {
            return -3;
        }
        if (infos.size() >= 1 && (infos.get(0) instanceof IconInfo)) {
            this.mHomeProxyCallbacks.moveItemFromFolder((IconInfo) infos.get(0));
        }
        return 0;
    }

    public final int addWidgetResultItemToHome() {
        PendingAddItemInfo widget = this.mWidgetProxyCallbacks.getWidgetResultItem();
        if (widget == null) {
            return -3;
        }
        if (!this.mHomeProxyCallbacks.addHomeWidget(widget)) {
            return -2;
        }
        goHome();
        return 0;
    }

    public List<ItemInfo> getAppsItemInfo(AppInfo appInfo) {
        return getAppsItemInfoByStateAppInfo(appInfo);
    }

    public final boolean hasItemInApps(AppInfo appInfo) {
        if (appInfo == null) {
            return false;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() <= 0) {
            return false;
        }
        return true;
    }

    public final boolean hasFolderInApps(String folderName) {
        if (folderName == null || this.mAppsProxyCallbacks.getFolderItemViewByTitle(folderName) == null) {
            return false;
        }
        return true;
    }

    public final boolean isAppsValidPage(int pageNumber) {
        return pageNumber >= 0 && pageNumber < this.mAppsProxyCallbacks.getPagedView().getPageCount();
    }

    public final boolean isAppsValidPage(int pageNumber, int pageDirection) {
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        return isAppsValidPage(p);
    }

    public final boolean isAppsViewTypeAlphabetic() {
        if (this.mAppsProxyCallbacks == null) {
            return false;
        }
        return this.mAppsProxyCallbacks.getViewType().equals(ViewType.ALPHABETIC_GRID);
    }

    public final int openAppsFolder(String folderName) {
        FolderIconView iv = this.mAppsProxyCallbacks.getFolderItemViewByTitle(folderName);
        if (folderName == null || iv == null) {
            return -3;
        }
        this.mStageManagerProxyCallbacks.openFolder(iv);
        return 0;
    }

    public final int moveAppsItemToPage(AppInfo appInfo, int pageNumber, int pageDirection, int detailDirection) {
        if (appInfo == null) {
            return -3;
        }
        if (pageNumber < 0) {
            pageNumber = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        View iv = null;
        if (appInfo.getComponentName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByComponentName(appInfo.getComponentName());
        } else if (appInfo.getName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByTitle(appInfo.getName());
        }
        if (iv == null) {
            return -3;
        }
        this.mAppsProxyCallbacks.moveItem((IconView) iv, pageNumber);
        return 0;
    }

    public final int moveAppsItemToFollowedEmptyPage(AppInfo appInfo, int pageNumber, int pageDirection) {
        int targetPage = pageNumber;
        if (appInfo == null) {
            return -3;
        }
        if (pageNumber < 0) {
            targetPage = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        View iv = null;
        if (appInfo.getComponentName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByComponentName(appInfo.getComponentName());
        } else if (appInfo.getName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByTitle(appInfo.getName());
        }
        if (iv == null) {
            return -3;
        }
        if (targetPage >= getAppsPageCount()) {
            targetPage = ((int) ((ItemInfo) iv.getTag()).screenId) + 1;
        }
        return this.mAppsProxyCallbacks.moveItemToFollowedEmptyPage((IconView) iv, targetPage);
    }

    public final int moveAppsFolderItemToPage(AppInfo appInfo, int pageNumber, int pageDirection, int detailDirection) {
        if (appInfo == null) {
            return -3;
        }
        if (pageNumber < 0) {
            pageNumber = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        View iv = null;
        if (appInfo.getComponentName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByComponentName(appInfo.getComponentName());
        } else if (appInfo.getName() != null) {
            iv = this.mAppsProxyCallbacks.getFolderItemViewByTitle(appInfo.getName());
        }
        if (iv == null) {
            return -3;
        }
        this.mAppsProxyCallbacks.moveItem((IconView) iv, pageNumber);
        return 0;
    }

    public final int moveAppsFolderToFollowedEmptyPage(AppInfo appInfo, int pageNumber, int pageDirection) {
        int targetPage = pageNumber;
        if (appInfo == null) {
            return -3;
        }
        if (pageNumber < 0) {
            targetPage = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        View iv = null;
        if (appInfo.getName() != null) {
            iv = this.mAppsProxyCallbacks.getFolderItemViewByTitle(appInfo.getName());
        }
        if (iv != null) {
            return this.mAppsProxyCallbacks.moveItemToFollowedEmptyPage((IconView) iv, targetPage);
        }
        return -3;
    }

    public final int getItemPageInApps(AppInfo appInfo) {
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() == 0) {
            return -1;
        }
        return (int) ((ItemInfo) items.get(0)).screenId;
    }

    public final int moveItemInFolderToAppsPage(AppInfo appInfo, int pageNumber, int targetPosition) {
        if (this.mFolderProxyCallbacks == null) {
            return -1;
        }
        if (appInfo == null || !hasItemInFolder(appInfo)) {
            return -3;
        }
        if (pageNumber > 0 && !isAppsValidPage(pageNumber)) {
            return -3;
        }
        View iv = null;
        if (appInfo.getComponentName() != null) {
            iv = this.mFolderProxyCallbacks.getItemViewByComponentName(appInfo.getComponentName());
        } else if (appInfo.getName() != null) {
            iv = this.mFolderProxyCallbacks.getItemViewByTitle(appInfo.getName());
        }
        if (iv == null) {
            return -3;
        }
        this.mAppsProxyCallbacks.moveItemInFolder((IconView) iv, pageNumber, targetPosition);
        return 0;
    }

    public final int removeItemInAppsPage(AppInfo appInfo) {
        View iv = null;
        if (appInfo.getComponentName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByComponentName(appInfo.getComponentName());
        } else if (appInfo.getName() != null) {
            iv = this.mAppsProxyCallbacks.getItemViewByTitle(appInfo.getName());
        }
        if (iv == null || iv.getTag() == null || !(iv.getTag() instanceof ItemInfo)) {
            return -3;
        }
        this.mAppsProxyCallbacks.removeItem((ItemInfo) iv.getTag());
        return 0;
    }

    public final int openAppsTray() {
        if (this.mStageManagerProxyCallbacks == null) {
            return -1;
        }
        this.mStageManagerProxyCallbacks.openAppsTray();
        return 0;
    }

    public final int showAppsViewTypePopup() {
        if (this.mAppsProxyCallbacks == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.showViewTypePopup();
        return 0;
    }

    public final int hideAppsViewTypePopup() {
        if (this.mAppsProxyCallbacks == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.hideViewTypePopup();
        return 0;
    }

    public final int changeAppsViewTypeToAlphabetic() {
        if (this.mAppsProxyCallbacks == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.setViewType(ViewType.ALPHABETIC_GRID);
        return 0;
    }

    public final int changeAppsViewTypeToCustom() {
        if (this.mAppsProxyCallbacks == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.setViewType(ViewType.CUSTOM_GRID);
        return 0;
    }

    public final int moveAppsPage(int pageNumber, int pageDirection) {
        int p = pageNumber;
        if (p <= 0) {
            p = getPageNumberToMove(this.mAppsProxyCallbacks, pageDirection);
        }
        if (!isAppsValidPage(pageNumber, pageDirection)) {
            return -3;
        }
        this.mAppsProxyCallbacks.movePage(p);
        return 0;
    }

    public final int moveAppsPage(int pageNumber) {
        this.mAppsProxyCallbacks.movePage(pageNumber);
        return 0;
    }

    public final int showAppsTidyUpPreview() {
        this.mAppsProxyCallbacks.showTidyUpPreview();
        return 0;
    }

    public final int appsTidyUpPages() {
        if (this.mAppsProxyCallbacks == null) {
            return -1;
        }
        this.mAppsProxyCallbacks.tidyUpPages();
        return 0;
    }

    public final int showAppsFolderRemovePopUp(AppInfo appInfo) {
        List<ItemInfo> infos = null;
        if (appInfo == null) {
            return -3;
        }
        if (appInfo.getName() != null) {
            infos = getItemsInfoByTitle(appInfo.getName(), Favorites.CONTAINER_APPS);
        } else if (appInfo.getComponentName() != null) {
            infos = getItemsInfoByComponentName(appInfo.getComponentName(), Favorites.CONTAINER_APPS);
        }
        if (infos == null) {
            return -3;
        }
        for (ItemInfo info : infos) {
            if (info instanceof FolderInfo) {
                this.mAppsProxyCallbacks.deleteFolder((FolderInfo) info);
            }
        }
        return 0;
    }

    public final int enterHomeSettingView() {
        this.mLauncherActivityProxyCallbacks.enterHomeSettingView();
        return 0;
    }

    public final int enterHomeAboutPageView() {
        this.mLauncherActivityProxyCallbacks.enterHomeAboutPageView();
        return 0;
    }

    public final int enterHomeSettingModeChangeView() {
        this.mLauncherActivityProxyCallbacks.enterHomeSettingChangeModeView();
        return 0;
    }

    public final int changeHomeStyle(boolean homeOnlyMode) {
        this.mLauncherActivityProxyCallbacks.changeHomeStyle(homeOnlyMode);
        return 0;
    }

    public final int enableAppsButton(boolean show) {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return -1;
        }
        if (show) {
            this.mLauncherActivityProxyCallbacks.showAppsButton();
        } else {
            this.mLauncherActivityProxyCallbacks.hideAppsButton();
        }
        this.mStageManagerProxyCallbacks.exitSettingsView();
        this.mHomeProxyCallbacks.exitSubState();
        return 0;
    }

    public final boolean isHomeOnlyMode() {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return false;
        }
        return this.mLauncherActivityProxyCallbacks.isHomeOnlyMode();
    }

    public final boolean isEnableAppsButton() {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return false;
        }
        return this.mLauncherActivityProxyCallbacks.isEnableAppsButton();
    }

    public final boolean isAvailableSleepMode(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null || appInfo == null || appInfo.getItemInfo() == null) {
            return false;
        }
        return this.mLauncherActivityProxyCallbacks.isAvailableSleepMode(appInfo.getItemInfo());
    }

    public final boolean isAlreadySleepMode(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null || appInfo == null || appInfo.getItemInfo() == null) {
            return false;
        }
        return this.mLauncherActivityProxyCallbacks.isAlreadySleepMode(appInfo.getItemInfo());
    }

    public final int putAppToSleep(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return -1;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() == 0) {
            return -1;
        }
        for (ItemInfo i : items) {
            this.mLauncherActivityProxyCallbacks.putToSleepMode(i);
        }
        return 0;
    }

    public final boolean isSecureFolderSetup() {
        return this.mLauncherActivityProxyCallbacks.isSecureFolderSetup();
    }

    public final boolean canAppAddToSecureFolder(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return false;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() == 0) {
            return false;
        }
        return this.mLauncherActivityProxyCallbacks.canAppAddToSecureFolder((ItemInfo) items.get(0));
    }

    public final int addToSecureFolder(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return -1;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() == 0) {
            return -1;
        }
        for (ItemInfo i : items) {
            this.mLauncherActivityProxyCallbacks.addToSecureFolder(i);
        }
        this.mAppsProxyCallbacks.startSecureFolder();
        return 0;
    }

    public final int clearBadge(AppInfo appInfo) {
        if (this.mLauncherActivityProxyCallbacks == null) {
            return -1;
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items == null || items.size() == 0) {
            return -1;
        }
        for (ItemInfo i : items) {
            this.mLauncherActivityProxyCallbacks.clearBadge(i);
        }
        return 0;
    }

    public final int clearFolderBadge(ItemInfo item) {
        if (this.mLauncherActivityProxyCallbacks == null || !(item instanceof FolderInfo)) {
            return -1;
        }
        Iterator it = ((FolderInfo) item).contents.iterator();
        while (it.hasNext()) {
            IconInfo iconInfo = (IconInfo) it.next();
            if (iconInfo.mBadgeCount != 0) {
                this.mLauncherActivityProxyCallbacks.clearBadge(iconInfo);
            }
        }
        return 0;
    }

    public final int goHome() {
        if (this.mStageManagerProxyCallbacks == null) {
            return -1;
        }
        this.mStageManagerProxyCallbacks.goHome();
        this.mHomeProxyCallbacks.exitSubState();
        return 0;
    }

    public final int openFolderAddItemView() {
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 5 || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        FolderIconView iv = this.mFolderProxyCallbacks.getOpenedFolderIconView();
        if (iv == null) {
            return -2;
        }
        this.mStageManagerProxyCallbacks.openFolderAddIconView(iv);
        return 0;
    }

    public final int addAppsFolderItem(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        if ((this.mStageManagerProxyCallbacks.getTopStageMode() != 5 && this.mStageManagerProxyCallbacks.getTopStageMode() != 6) || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        if (this.mStageManagerProxyCallbacks.getTopStageMode() == 6) {
            this.mStageManagerProxyCallbacks.finishStage(6, null);
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        boolean itemAdded = false;
        if (items != null) {
            for (ItemInfo i : items) {
                if (i.container < 0) {
                    this.mFolderProxyCallbacks.addFolderItem(i);
                    itemAdded = true;
                }
            }
        }
        if (itemAdded) {
            return 0;
        }
        return -3;
    }

    public final int addHomeFolderItem(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        if ((this.mStageManagerProxyCallbacks.getTopStageMode() != 5 && this.mStageManagerProxyCallbacks.getTopStageMode() != 6) || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        if (this.mStageManagerProxyCallbacks.getTopStageMode() == 6) {
            this.mStageManagerProxyCallbacks.finishStage(6, null);
        }
        List<ItemInfo> items = getAppsItemInfoByStateAppInfo(appInfo);
        if (items != null) {
            for (ItemInfo i : items) {
                if (i instanceof IconInfo) {
                    this.mFolderProxyCallbacks.addFolderItem(((IconInfo) i).makeCloneInfo());
                }
            }
        }
        return 0;
    }

    public final int removeFolderItem(AppInfo appInfo) {
        if (appInfo == null) {
            return -3;
        }
        if (this.mStageManagerProxyCallbacks.getTopStageMode() != 5 || this.mFolderProxyCallbacks == null) {
            return -1;
        }
        if (!hasItemInFolder(appInfo)) {
            return -3;
        }
        ArrayList<ItemInfo> items = getFolderItemInfoByStateAppInfo(appInfo);
        if (items == null || items.isEmpty()) {
            return -3;
        }
        this.mFolderProxyCallbacks.removeFolderItem((ItemInfo) items.get(0));
        return 0;
    }

    public final int openThemeApp() {
        return 0;
    }

    public final boolean isUninstallApp(ItemInfo item) {
        if (item.componentName == null || item.componentName.getPackageName() == null || Utilities.canUninstall(LauncherAppState.getInstance().getContext(), item.componentName.getPackageName())) {
            return true;
        }
        return false;
    }

    public final boolean isDisableApp(ItemInfo item) {
        if (item.componentName == null || item.componentName.getPackageName() == null || Utilities.canDisable(LauncherAppState.getInstance().getContext(), item.componentName.getPackageName())) {
            return true;
        }
        return false;
    }

    public final int uninstallOrDisableApp(ItemInfo item) {
        this.mLauncherActivityProxyCallbacks.uninstallOrDisableApp(item);
        return 0;
    }

    public final int putToSleepMode(ItemInfo item) {
        this.mLauncherActivityProxyCallbacks.uninstallOrDisableApp(item);
        return 0;
    }

    public final int getCurrentTopStage() {
        return this.mStageManagerProxyCallbacks.getTopStageMode();
    }

    public final int getSecondTopStage() {
        return this.mStageManagerProxyCallbacks.getSecondTopStageMode();
    }

    public final boolean checkNeedDisplayAutoalignDialog() {
        return this.mHomeProxyCallbacks.checkNeedDisplayAutoalignDialog();
    }

    public final boolean checkAbleAlignIcon(int page, boolean isUpward) {
        return this.mHomeProxyCallbacks.checkAbleAlignIcon(page, isUpward);
    }

    public boolean onParamFillingReceived(String topViewState, ParamFilling pf) {
        if ((this.mFolderProxyCallbacks != null && "AppsFolderView".equals(topViewState)) || "HomeFolderView".equals(topViewState)) {
            this.mFolderProxyCallbacks.onParamFillingReceived(pf);
        } else if ((this.mAppsPickerProxyCallback != null && "AppsFolderAddIconSearchView".equals(topViewState)) || "HomeFolderAddIconSearchView".equals(topViewState) || "HomeSettingsHideAppsView".equals(topViewState)) {
            this.mAppsPickerProxyCallback.onParamFillingReceived(pf);
        } else if (this.mWidgetProxyCallbacks != null && "HomePageWidgetSearchView".equals(topViewState)) {
            this.mWidgetProxyCallbacks.onParamFillingReceived(pf);
        }
        return true;
    }

    private List<ItemInfo> getItemsInfoByComponentName(ComponentName cn, int container) {
        List<ItemInfo> unhiddenAndComponentFilteredItemList = ItemListHelper.getUnhiddenItemList(DataLoader.getItemInfoByComponentName(cn, UserHandleCompat.myUserHandle(), false));
        List<ItemInfo> resultFilteredList = ItemListHelper.getContainerIdMatchedItemList(unhiddenAndComponentFilteredItemList, container);
        List<ItemInfo> folderItemList = ItemListHelper.getFolderItemList(unhiddenAndComponentFilteredItemList);
        Map<Long, ItemInfo> itemMap = ItemListHelper.getAllItemMap();
        for (ItemInfo folderItem : folderItemList) {
            ItemInfo item = (ItemInfo) itemMap.get(Long.valueOf(folderItem.container));
            if (item != null && item.container == ((long) container)) {
                resultFilteredList.add(folderItem);
            }
        }
        return resultFilteredList;
    }

    private List<ItemInfo> getItemsInfoByTitle(String itemTitle, int container) {
        List<ItemInfo> unhiddenAndTitleFilteredList = ItemListHelper.getTitleMatchedItemList(ItemListHelper.getUnhiddenItemList(DataLoader.getItemList()), itemTitle);
        List<ItemInfo> resultFilteredList = ItemListHelper.getContainerIdMatchedItemList(unhiddenAndTitleFilteredList, container);
        List<ItemInfo> folderItemList = ItemListHelper.getFolderItemList(unhiddenAndTitleFilteredList);
        Map<Long, ItemInfo> itemMap = ItemListHelper.getAllItemMap();
        for (ItemInfo folderItem : folderItemList) {
            ItemInfo item = (ItemInfo) itemMap.get(Long.valueOf(folderItem.container));
            if (item != null && item.container == ((long) container)) {
                resultFilteredList.add(folderItem);
            }
        }
        return resultFilteredList;
    }

    public final ItemInfo getItemInfoInHideApps(AppInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        if (appInfo.getOrdinalNumber() != INVALID_VALUE) {
            return this.mAppsPickerProxyCallback.getItem(appInfo.getOrdinalNumber());
        }
        if (appInfo.getComponentName() != null) {
            return this.mAppsPickerProxyCallback.getItem(appInfo.getComponentName());
        }
        if (appInfo.getName() != null) {
            return this.mAppsPickerProxyCallback.getItem(appInfo.getName());
        }
        return null;
    }

    public String getAppNamebyComponentName(AppInfo appInfo) {
        List<ItemInfo> items;
        if (isHomeOnlyMode()) {
            items = getHomeItemInfoByStateAppInfo(appInfo);
        } else {
            items = getAppsItemInfo(appInfo);
        }
        if (!(items == null || items.size() == 0)) {
            ItemInfo item = (ItemInfo) items.get(0);
            if (item != null && (item instanceof IconInfo)) {
                return item.title.toString();
            }
        }
        return "";
    }

    public final int enterBadgeManagementView() {
        this.mLauncherActivityProxyCallbacks.enterBadgeManagementView();
        return 0;
    }

    public final int enableAllAppsBadge(boolean enable) {
        if (this.mLauncherActivityProxyCallbacks.enableAllAppsBadge(enable)) {
            return 0;
        }
        return -1;
    }

    public final int enableSingleAppBadge(String name, boolean enable) {
        if (this.mLauncherActivityProxyCallbacks.enableSingleAppBadge(name, enable)) {
            return 0;
        }
        return -1;
    }

    public final boolean isSingleAppBadgeChecked(String className) {
        return this.mLauncherActivityProxyCallbacks.isSingleAppBadgeChecked(className);
    }

    public final boolean isAllAppsBadgeSwitchChecked() {
        return this.mLauncherActivityProxyCallbacks.isAllAppsBadgeSwitchChecked();
    }

    public final int lockFolder(ItemInfo folderInfo) {
        if (!(folderInfo instanceof FolderInfo)) {
            return -1;
        }
        FolderLock folderLock = FolderLock.getInstance();
        if (folderLock.isLockedFolder((FolderInfo) folderInfo)) {
            return -1;
        }
        folderLock.setBackupInfo(folderInfo);
        folderLock.startLockVerifyActivity(folderInfo);
        return 0;
    }

    public final int unlockFolder(ItemInfo folderInfo) {
        if (!(folderInfo instanceof FolderInfo)) {
            return -1;
        }
        FolderLock folderLock = FolderLock.getInstance();
        if (!folderLock.isLockedFolder((FolderInfo) folderInfo)) {
            return -1;
        }
        folderLock.setBackupInfo(folderInfo);
        folderLock.startUnlockVerifyActivity(folderInfo);
        return 0;
    }

    public final int lockSingleApp(ItemInfo itemInfo) {
        FolderLock folderLock = FolderLock.getInstance();
        if (!(itemInfo instanceof IconInfo) || folderLock.isLockedApp((IconInfo) itemInfo)) {
            return -1;
        }
        folderLock.setBackupInfo(itemInfo);
        folderLock.startLockVerifyActivity(itemInfo);
        return 0;
    }

    public final int unlockSingleApp(ItemInfo itemInfo) {
        FolderLock folderLock = FolderLock.getInstance();
        if (!(itemInfo instanceof IconInfo) || !folderLock.isLockedApp((IconInfo) itemInfo)) {
            return -1;
        }
        folderLock.setBackupInfo(itemInfo);
        folderLock.startUnlockVerifyActivity(itemInfo);
        return 0;
    }

    public final int getHomePageNumberByScreenId(long screenId) {
        return this.mHomeProxyCallbacks.getPageIndexForScreenId(screenId);
    }

    public void setNotificationPanelExpansionEnabled(boolean value) {
        this.mLauncherActivityProxyCallbacks.setNotificationPanelExpansionEnabled(value);
    }
}
