package com.android.launcher3.home;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.dialog.FolderDeleteDialog;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.proxy.HomeProxyCallbacks;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HomeProxyCallbacksImpl implements HomeProxyCallbacks {
    private final String TAG = HomeProxyCallbacksImpl.class.getSimpleName();
    private HomeController mHomeController;

    HomeProxyCallbacksImpl(HomeController homeController) {
        this.mHomeController = homeController;
    }

    public FolderIconView getFolderItemViewByTitle(String itemTitle) {
        Iterator it = this.mHomeController.getWorkspace().findIconViews(itemTitle).iterator();
        while (it.hasNext()) {
            IconView v = (IconView) it.next();
            if (v instanceof FolderIconView) {
                return (FolderIconView) v;
            }
        }
        return null;
    }

    public int getFolderItemCountByTitle(String itemTitle) {
        return this.mHomeController.getFolderItemCount(itemTitle);
    }

    public View getWidgetView(ComponentName cn) {
        return this.mHomeController.getWorkspace().findWidgetView(cn);
    }

    public IconView getItemViewByTitle(String itemTitle) {
        Iterator it = this.mHomeController.getWorkspace().findIconViews(itemTitle).iterator();
        while (it.hasNext()) {
            IconView v = (IconView) it.next();
            if (!(v instanceof FolderIconView)) {
                return v;
            }
        }
        return null;
    }

    public IconView getItemViewByComponentName(ComponentName cn) {
        return this.mHomeController.getWorkspace().findIconView(cn);
    }

    public PagedView getPagedView() {
        return this.mHomeController.getWorkspace();
    }

    private long checkEmptySpace(int startPage, int spanX, int spanY, int[] resultPosition) {
        int[] emptyCell = new int[2];
        HomeItemPositionHelper positionHelper = this.mHomeController.getHomeLoader().getItemPositionHelper();
        boolean find = false;
        int toPage = startPage;
        long screenId = -1;
        int lastPageIdx = this.mHomeController.getHomeLoader().getWorkspaceScreenCount() - 1;
        if (startPage >= 0 && startPage <= lastPageIdx && positionHelper.findEmptyCell(emptyCell, this.mHomeController.getHomeLoader().getWorkspaceScreenId(startPage), spanX, spanY)) {
            find = true;
            screenId = this.mHomeController.getHomeLoader().getWorkspaceScreenId(toPage);
        }
        if (!find) {
            for (int i = startPage; i <= lastPageIdx; i++) {
                if (positionHelper.findEmptyCell(emptyCell, this.mHomeController.getHomeLoader().getWorkspaceScreenId(i), spanX, spanY)) {
                    toPage = i;
                    find = true;
                    screenId = this.mHomeController.getHomeLoader().getWorkspaceScreenId(toPage);
                    break;
                }
            }
            if (!find) {
                if (this.mHomeController.isOverviewState()) {
                    screenId = this.mHomeController.getWorkspace().addNewWorkspaceScreen();
                } else {
                    screenId = this.mHomeController.getHomeLoader().insertWorkspaceScreen(this.mHomeController.getLauncher(), lastPageIdx + 1, -1);
                }
                toPage = lastPageIdx + 1;
            }
        }
        resultPosition[0] = toPage;
        resultPosition[1] = emptyCell[0];
        resultPosition[2] = emptyCell[1];
        return screenId;
    }

    public int moveItem(View iv, int page) {
        Log.d(this.TAG, "moveItem : " + page);
        ItemInfo item = null;
        int spanX = 1;
        int spanY = 1;
        if (iv instanceof IconView) {
            item = (ItemInfo) iv.getTag();
        } else if (iv instanceof AppWidgetHostView) {
            item = (ItemInfo) iv.getTag();
            spanX = item.spanX;
            spanY = item.spanY;
        }
        if (item == null || iv == null) {
            return -1;
        }
        ((CellLayout) this.mHomeController.getWorkspace().getChildAt(this.mHomeController.getWorkspace().getPageIndexForScreenId(item.screenId))).removeView(iv);
        int[] resultPosition = new int[3];
        long screenId = checkEmptySpace(page, spanX, spanY, resultPosition);
        this.mHomeController.addInScreen(iv, -100, screenId, resultPosition[1], resultPosition[2], spanX, spanY);
        item.screenId = screenId;
        item.cellX = resultPosition[1];
        item.cellY = resultPosition[2];
        this.mHomeController.updateItemInDb(item);
        final int moveTo = resultPosition[0];
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (HomeProxyCallbacksImpl.this.mHomeController != null) {
                    HomeProxyCallbacksImpl.this.mHomeController.getWorkspace().moveToScreen(moveTo, true);
                }
            }
        }, 1000);
        return resultPosition[0];
    }

    public void moveItemFromFolder(IconInfo iconInfo) {
        this.mHomeController.moveItemFromFolder(iconInfo);
    }

    public void movePage(int pageNum) {
        movePage(pageNum, false);
    }

    public void movePage(int pageNum, boolean needAnimation) {
        Log.d(this.TAG, "movePage : " + pageNum);
        this.mHomeController.getWorkspace().checkVisibilityOfCustomLayout(pageNum);
        this.mHomeController.getWorkspace().moveToScreen(pageNum, needAnimation);
    }

    public void removeShortcut(ItemInfo item) {
        if (item instanceof FolderInfo) {
            new FolderDeleteDialog().show(this.mHomeController.getLauncher().getFragmentManager(), this.mHomeController, (FolderInfo) item);
            return;
        }
        this.mHomeController.getWorkspace().moveToScreen(this.mHomeController.getWorkspace().getPageIndexForScreenId(item.screenId), true);
        this.mHomeController.removeHomeOrFolderItem(item, this.mHomeController.getHomescreenIconByItemId(item.id));
    }

    public void createShortcut(ComponentName cn, ItemInfo itemInfo, int page) {
        ItemInfo item = null;
        if (cn != null) {
            ArrayList<ItemInfo> itemList = DataLoader.getItemInfoByComponentName(cn, UserHandleCompat.myUserHandle(), true);
            if (itemList.size() != 0) {
                item = (ItemInfo) itemList.get(0);
            } else {
                return;
            }
        } else if (itemInfo != null) {
            item = itemInfo;
        }
        if (item != null) {
            int[] emptyCell = new int[2];
            HomeItemPositionHelper positionHelper = this.mHomeController.getHomeLoader().getItemPositionHelper();
            for (int i = page; i < this.mHomeController.getWorkspace().getPageCount(); i++) {
                if (positionHelper.findEmptyCell(emptyCell, this.mHomeController.getHomeLoader().getWorkspaceScreenId(i), 1, 1)) {
                    ItemInfo hItem;
                    if (item instanceof FolderInfo) {
                        hItem = ((FolderInfo) item).makeCloneInfo();
                        hItem.id = FavoritesProvider.getInstance().generateNewItemId();
                    } else {
                        hItem = ((IconInfo) item).makeCloneInfo();
                    }
                    hItem.screenId = this.mHomeController.getHomeLoader().getWorkspaceScreenId(i);
                    hItem.cellX = emptyCell[0];
                    hItem.cellY = emptyCell[1];
                    hItem.spanY = 1;
                    hItem.spanX = 1;
                    this.mHomeController.addItemOnHome(hItem, emptyCell, hItem.screenId);
                    if (this.mHomeController.getLauncher().getStageManager().getTopStage() != this.mHomeController) {
                        this.mHomeController.getLauncher().getStageManager().startStage(1, null);
                    }
                }
            }
            if (this.mHomeController.getLauncher().getStageManager().getTopStage() != this.mHomeController) {
                this.mHomeController.getLauncher().getStageManager().startStage(1, null);
            }
        }
    }

    public void changeFolderTitle(ItemInfo item, String newTitle) {
        IconView iv = getItemViewByTitle(item.title.toString());
        if (iv != null) {
            item.title = newTitle;
            iv.setText(item.title);
            this.mHomeController.updateItemInDb(item);
        }
    }

    public void setAsMainPage(int pageNumber) {
        Log.d(this.TAG, "setAsMainPage : " + this.mHomeController.getWorkspace().getCurrentPage() + "/" + this.mHomeController.getWorkspace().getPageCount() + ", to " + pageNumber);
        if (pageNumber >= 1 && this.mHomeController.getWorkspace().getPageCount() > pageNumber) {
            this.mHomeController.getWorkspace().updateDefaultHome(this.mHomeController.getWorkspace().getDefaultPage(), pageNumber);
        }
    }

    public void addNewPage() {
        this.mHomeController.getHomeLoader().insertWorkspaceScreen(this.mHomeController.getLauncher(), this.mHomeController.getWorkspace().getPageCount(), -1);
    }

    public void addNewHomePageInOverViewMode() {
        this.mHomeController.getWorkspace().addNewWorkspaceScreen();
        this.mHomeController.getWorkspace().moveToScreen(this.mHomeController.getWorkspace().getPageCount() - 2, true);
    }

    public void removeCurrentPage() {
        if (this.mHomeController.getWorkspace().isPageMoving()) {
            this.mHomeController.getWorkspace().pageEndMoving();
        }
        removeScreen();
    }

    private void removeScreen() {
        if (this.mHomeController.isOverviewState()) {
            if (this.mHomeController.getWorkspace().getPageIndexForScreenId(-401) == this.mHomeController.getWorkspace().getCurrentPage()) {
                this.mHomeController.getWorkspace().setCurrentPage(this.mHomeController.getWorkspace().getCurrentPage() - 1);
            }
            this.mHomeController.getWorkspace().touchPageDeleteButton();
            return;
        }
        Log.e(this.TAG, "removeScreen - not isOverviewState");
    }

    public void alignHomeIcon(int page, boolean isTop) {
        if (this.mHomeController.getWorkspace().isPageMoving()) {
            this.mHomeController.getWorkspace().pageEndMoving();
        }
        this.mHomeController.getWorkspace().setCurrentPage(page);
        this.mHomeController.getWorkspace().autoAlignItems(isTop);
    }

    public void enterHomeEditView() {
        this.mHomeController.enterOverviewState(true);
    }

    public void enterHomeSettingGridSettingView() {
        this.mHomeController.enterHomeScreenGrid(true);
    }

    public void changeScreengrid(String gridOption) {
        Log.d(this.TAG, "changeScreengrid : " + gridOption);
        this.mHomeController.getScreenGridPanel().setScreenGridProxy(gridOption);
    }

    public boolean checkValidGridOption(String gridOption) {
        return this.mHomeController.getScreenGridPanel().checkValidGridOption(gridOption);
    }

    public boolean checkMatchGridOption(String gridOption) {
        return this.mHomeController.getScreenGridPanel().checkMatchGridOption(gridOption);
    }

    public void selectItem(IconView iv) {
        if (LauncherFeature.supportMultiSelect()) {
            this.mHomeController.onCheckedChanged(iv, true);
        }
    }

    public void unSelectItem(IconView iv) {
        if (LauncherFeature.supportMultiSelect()) {
            this.mHomeController.onCheckedChanged(iv, false);
        }
    }

    public void exitSubState() {
        int screenCount = this.mHomeController.getWorkspace().getChildCount();
        for (int i = 0; i < screenCount; i++) {
            this.mHomeController.getWorkspace().setVisibilityOnCustomLayout(false, false, false, i);
        }
        this.mHomeController.enterNormalState(true);
    }

    public ArrayList<ItemInfo> getWidgetItemsInfoByTitle(String itemTitle) {
        Iterator it;
        ArrayList<ItemInfo> findItems = new ArrayList();
        ArrayList<ItemInfo> returnItems = new ArrayList();
        List<Object> wItems = this.mHomeController.getLauncher().getLauncherModel().getWidgetsLoader().getWidgetItems();
        if (wItems != null) {
            for (Object l : wItems) {
                if (l instanceof ArrayList) {
                    findWidgetItemInList((ArrayList) l, itemTitle, findItems);
                }
            }
        }
        if (findItems.size() > 0) {
            it = findItems.iterator();
            while (it.hasNext()) {
                returnItems.addAll(DataLoader.getItemInfoByComponentName(((ItemInfo) it.next()).componentName, UserHandleCompat.myUserHandle(), false));
            }
        }
        return returnItems;
    }

    public ArrayList<ItemInfo> getWidgetItemsInfoByComponentName(ComponentName cn) {
        ArrayList<ItemInfo> itemList = new ArrayList();
        Iterator it = DataLoader.getItemInfoByComponentName(cn, UserHandleCompat.myUserHandle(), false).iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.container == -100 && item.itemType == 4) {
                itemList.add(item);
            }
        }
        return itemList;
    }

    public ArrayList<ItemInfo> getWidgetItemsInfoByPackageName(String pkgName) {
        ArrayList<ItemInfo> itemList = new ArrayList();
        Iterator it = DataLoader.getItemsByPackageName(pkgName, UserHandleCompat.myUserHandle()).iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.container == -100 && item.itemType == 4) {
                itemList.add(item);
            }
        }
        return itemList;
    }

    private void findWidgetItemInList(ArrayList<ItemInfo> list, String itemTitle, ArrayList<ItemInfo> resuleList) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ItemInfo i = (ItemInfo) it.next();
            if (i instanceof PendingAddWidgetInfo) {
                PendingAddWidgetInfo pWidget = (PendingAddWidgetInfo) i;
                String widgetName = null;
                if (pWidget.mLabel != null) {
                    widgetName = pWidget.mLabel.toLowerCase();
                } else if (pWidget.getApplicationLabel() != null) {
                    widgetName = pWidget.getApplicationLabel().toLowerCase();
                }
                if (widgetName != null && widgetName.contains(itemTitle.toLowerCase())) {
                    resuleList.add(i);
                }
            }
        }
    }

    public void removeWidget(ItemInfo item) {
        this.mHomeController.removeHomeOrFolderItem(item, this.mHomeController.getHomescreenIconByItemId(item.id));
    }

    public void changeHomePageOrder(int fromPage, int toPage) {
        Workspace workspace = this.mHomeController.getWorkspace();
        workspace.updateDefaultHomeScreenId(workspace.getScreenIdForPageIndex(workspace.getDefaultPage()));
        View v = workspace.getPageAt(fromPage);
        workspace.removeView(v);
        workspace.addView(v, toPage);
        workspace.setCurrentPage(toPage);
        if (fromPage == workspace.getDefaultPage()) {
            workspace.updateDefaultHomeScreenId(workspace.getIdForScreen((CellLayout) v));
        }
        workspace.onEndReordering();
    }

    public void movePageToItem(ItemInfo item) {
        int pageNum = (int) this.mHomeController.getHomeLoader().getWorkspaceScreenId((int) item.screenId);
        if (pageNum >= 0) {
            this.mHomeController.getWorkspace().moveToScreen(pageNum, true);
        }
    }

    public boolean enterWidgetResizeMode(ItemInfo item) {
        if (!(item instanceof LauncherAppWidgetInfo)) {
            return false;
        }
        LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) item;
        AppWidgetHostView hostView = widget.hostView;
        CellLayout cellLayout = this.mHomeController.getWorkspace().getScreenWithId(widget.screenId);
        this.mHomeController.getWorkspace().moveToScreen(this.mHomeController.getWorkspace().getPageIndexForScreenId(widget.screenId), true);
        if (!this.mHomeController.canEnterResizeMode(hostView, cellLayout, false)) {
            return false;
        }
        this.mHomeController.enterResizeStateDelay(hostView, cellLayout);
        return true;
    }

    public boolean checkNeedDisplayAutoalignDialog() {
        return this.mHomeController.getWorkspace().checkNeedDisplayAutoalignDialog();
    }

    public boolean checkAbleAlignIcon(int page, boolean isUpward) {
        return this.mHomeController.autoAlignItems(isUpward, true, page);
    }

    public boolean hasPageEmptySpace(int page, int spanX, int spanY) {
        if (page < 0 || page >= this.mHomeController.getWorkspace().getPageCount()) {
            return false;
        }
        return this.mHomeController.getHomeLoader().getItemPositionHelper().findEmptyCell(new int[]{0, 0}, this.mHomeController.getHomeLoader().getWorkspaceScreenId(page), spanX, spanY);
    }

    public boolean neededToAdjustZeroPage() {
        return this.mHomeController.isOverviewState() && ZeroPageController.isEnableZeroPage();
    }

    public boolean hasPageDeleteButton(int page) {
        View deleteButton = this.mHomeController.getWorkspace().getPageDeleteBtn(page);
        return deleteButton != null && deleteButton.getVisibility() == 0;
    }

    public boolean isEmptyPage(int page) {
        return this.mHomeController.getWorkspace().isEmptyPage(page);
    }

    public int getDefaultPage() {
        return this.mHomeController.getWorkspace().getDefaultPage();
    }

    public boolean onParamFillingReceived(ParamFilling pf) {
        return false;
    }

    public int getPageIndexForScreenId(long screenId) {
        return this.mHomeController.getWorkspace().getPageIndexForScreenId(screenId);
    }

    public boolean addHomeWidget(PendingAddItemInfo widget) {
        if (widget == null) {
            return false;
        }
        int[] resultPosition = new int[3];
        int startIdx = this.mHomeController.getWorkspace().getCurrentPage();
        if (neededToAdjustZeroPage()) {
            startIdx--;
        }
        PendingAddItemInfo pendingAddItemInfo = widget;
        this.mHomeController.addPendingItem(pendingAddItemInfo, -100, checkEmptySpace(startIdx, widget.spanX, widget.spanY, resultPosition), new int[]{resultPosition[1], resultPosition[2]}, widget.spanX, widget.spanY);
        final int moveTo = resultPosition[0];
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (HomeProxyCallbacksImpl.this.mHomeController != null) {
                    HomeProxyCallbacksImpl.this.mHomeController.getWorkspace().moveToScreen(moveTo, true);
                }
            }
        }, 1000);
        return true;
    }
}
