package com.android.launcher3.allapps.controller;

import android.content.ComponentName;
import android.util.Log;
import android.view.View;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.dialog.FolderDeleteDialog;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.proxy.AppsProxyCallbacks;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.SecureFolderHelper;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import java.util.ArrayList;
import java.util.Iterator;

class AppsProxyCallbacksImpl implements AppsProxyCallbacks {
    private static final String TAG = AppsProxyCallbacks.class.getSimpleName();
    private AppsController mAppsController;

    AppsProxyCallbacksImpl(AppsController appsController) {
        this.mAppsController = appsController;
    }

    public void showViewTypePopup() {
        this.mAppsController.chooseViewType();
    }

    public void hideViewTypePopup() {
        this.mAppsController.hideViewTypeDialog();
    }

    public void setViewType(ViewType viewType) {
        this.mAppsController.setViewType(viewType);
        this.mAppsController.hideViewTypeDialog();
    }

    public ViewType getViewType() {
        return this.mAppsController.getViewType();
    }

    public FolderIconView getFolderItemViewByTitle(String itemTitle) {
        Iterator it = this.mAppsController.getAppsPagedView().findIconViews(itemTitle).iterator();
        while (it.hasNext()) {
            IconView v = (IconView) it.next();
            if (v instanceof FolderIconView) {
                return (FolderIconView) v;
            }
        }
        return null;
    }

    public IconView getItemViewByComponentName(ComponentName cn) {
        return this.mAppsController.getAppsPagedView().findIconView(cn);
    }

    public IconView getItemViewByTitle(String itemTitle) {
        Iterator it = this.mAppsController.getAppsPagedView().findIconViews(itemTitle).iterator();
        while (it.hasNext()) {
            IconView v = (IconView) it.next();
            if (!(v instanceof FolderIconView)) {
                return v;
            }
        }
        return null;
    }

    public void movePage(int pageNum) {
        this.mAppsController.getAppsPagedView().snapToPage(pageNum);
    }

    public PagedView getPagedView() {
        return this.mAppsController.getAppsPagedView();
    }

    public void tidyUpPages() {
        this.mAppsController.setApplyTidyUpPage(true);
        this.mAppsController.changeState(0, true);
    }

    public void selectItem(IconView iv) {
        this.mAppsController.onCheckedChanged(iv, true);
    }

    public void unSelectItem(IconView iv) {
        this.mAppsController.onCheckedChanged(iv, false);
    }

    public void moveItem(IconView iconView, int page) {
        if (page > this.mAppsController.getAppsPagedView().getPageCount() + 1 || page < 0) {
            Log.d(TAG, "move to the invalid page");
            return;
        }
        ItemInfo item = (ItemInfo) iconView.getTag();
        ((CellLayout) this.mAppsController.getAppsPagedView().getChildAt((int) item.screenId)).removeView(iconView);
        int lastRank = this.mAppsController.getAppsPagedView().getItemCountPageAt(page) - 1;
        AppsReorderController reorderController = this.mAppsController.getReorderController();
        reorderController.realTimeReorder(0, 0.0f, item.rank, this.mAppsController.getAppsPagedView().getItemCountPageAt((int) item.screenId), 1, (int) item.screenId);
        if (lastRank == this.mAppsController.getAppsPagedView().getMaxItemsPerScreen() - 1) {
            if (page == this.mAppsController.getAppsPagedView().getPageCount()) {
                this.mAppsController.getAppsPagedView().createAppsPage();
            } else {
                for (int i = reorderController.getNumScreenNeededChange(page + 1); i >= page + 1; i--) {
                    if (this.mAppsController.getAppsPagedView().getItemCountPageAt(i) >= this.mAppsController.getAppsPagedView().getMaxItemsPerScreen()) {
                        reorderController.overLastItemNextScreen(0, 0.0f, i);
                    }
                    int startPos = this.mAppsController.getAppsPagedView().getItemCountPageAt(i);
                    int direction = 1;
                    if (startPos > 0) {
                        direction = -1;
                    }
                    reorderController.realTimeReorder(0, 0.0f, startPos, 0, direction, i);
                }
            }
            item.rank = 0;
            item.screenId = (long) (page + 1);
        } else {
            item.rank = lastRank + 1;
            item.screenId = (long) page;
        }
        this.mAppsController.removeEmptyPagesAndUpdateAllItemsInfo();
        this.mAppsController.addItem(iconView, item);
        this.mAppsController.addOrMoveItemInDb(item, -102, item.screenId, item.rank % this.mAppsController.getAppsPagedView().getCellCountX(), item.rank / this.mAppsController.getAppsPagedView().getCellCountX(), item.rank);
        this.mAppsController.updateDirtyItems();
        this.mAppsController.getAppsPagedView().snapToPageImmediately(page);
    }

    public int moveItemToFollowedEmptyPage(IconView iconView, int page) {
        int emptyPageFound = -1;
        for (int i = page; i < getPageCount(); i++) {
            if (hasPageEmptySpace(i)) {
                emptyPageFound = i;
                break;
            }
        }
        if (emptyPageFound == -1) {
            this.mAppsController.getAppsPagedView().createAppsPage();
            emptyPageFound = getPageCount() - 1;
        }
        moveItem(iconView, emptyPageFound);
        return emptyPageFound;
    }

    public void moveItemInFolder(IconView iconView, int pageNumber, int targetPosition) {
        if (pageNumber > this.mAppsController.getAppsPagedView().getPageCount() + 1) {
            Log.d(TAG, "move to the invalid page");
            return;
        }
        int screenId = pageNumber;
        if (pageNumber < 0) {
            screenId = this.mAppsController.getAppsPagedView().getPageCount() - 1;
        }
        ItemInfo item = (ItemInfo) iconView.getTag();
        FolderView folder = this.mAppsController.getLauncher().getOpenFolderView();
        if (folder != null) {
            screenId = (int) folder.getInfo().screenId;
            if (item instanceof IconInfo) {
                folder.getInfo().remove((IconInfo) item);
            }
            folder.getBaseController().deleteItemFromDb(item);
        }
        int lastRank = this.mAppsController.getAppsPagedView().getItemCountPageAt(screenId);
        AppsReorderController reorderController = this.mAppsController.getReorderController();
        if (lastRank == this.mAppsController.getAppsPagedView().getMaxItemsPerScreen()) {
            if (screenId == this.mAppsController.getAppsPagedView().getPageCount()) {
                this.mAppsController.getAppsPagedView().createAppsPage();
                lastRank = 0;
                screenId++;
            } else {
                int pageNum = reorderController.getNumScreenNeededChange(screenId);
                if (pageNum == 0) {
                    this.mAppsController.getAppsPagedView().createAppsPage();
                    screenId = this.mAppsController.getAppsPagedView().getPageCount() - 1;
                    lastRank = 0;
                } else {
                    screenId = pageNum;
                    lastRank = this.mAppsController.getAppsPagedView().getItemCountPageAt(pageNum);
                }
            }
        }
        item.rank = lastRank;
        item.screenId = (long) screenId;
        item.container = -102;
        this.mAppsController.addItem(this.mAppsController.createItemView(item, this.mAppsController.getAppsPagedView().getCellLayout((int) item.screenId), null), item);
        this.mAppsController.addItemToDb(item, item.container, item.screenId, item.rank);
        this.mAppsController.updateDirtyItems();
        this.mAppsController.getAppsPagedView().snapToPage(screenId);
    }

    public boolean hasPageEmptySpace(int page) {
        if (page < 0 || page > this.mAppsController.getAppsPagedView().getPageCount() + 1) {
            Log.d(TAG, "move to the invalid page");
            return false;
        } else if (this.mAppsController.getAppsPagedView().getItemCountPageAt(page) != this.mAppsController.getAppsPagedView().getMaxItemsPerScreen()) {
            return true;
        } else {
            return false;
        }
    }

    public int getPageCount() {
        return this.mAppsController.getAppsPagedView().getPageCount();
    }

    public void movePageToItem(ItemInfo item) {
    }

    public void showTidyUpPreview() {
        this.mAppsController.prepareTidedUpPages();
    }

    public void removeItem(ItemInfo removeItem) {
        ArrayList<ItemInfo> removeItems = new ArrayList();
        removeItems.add(removeItem);
        this.mAppsController.removeApps(removeItems);
    }

    public void deleteFolder(FolderInfo folderInfo) {
        new FolderDeleteDialog().show(this.mAppsController.getLauncher().getFragmentManager(), this.mAppsController, folderInfo);
    }

    public void startSecureFolder() {
        View v = getItemViewByComponentName(new ComponentName(SecureFolderHelper.SECURE_FOLDER_PACKAGE_NAME, "com.samsung.knox.securefolder.switcher.SecureFolderShortcutActivity"));
        if (v != null) {
            this.mAppsController.getLauncher().startAppShortcutOrInfoActivity(v);
        }
    }

    public void changeFolderTitle(ItemInfo item, String newTitle) {
        IconView iv = getItemViewByTitle(item.title.toString());
        if (iv != null) {
            item.title = newTitle;
            iv.setText(item.title);
            this.mAppsController.updateItemInDb(item);
        }
    }

    public void showAppsGridSettingView() {
        this.mAppsController.getLauncher().showAppsOrWidgets(this.mAppsController.getMode(), true, true);
    }

    public void changeScreengrid(String gridOption) {
        this.mAppsController.getAppsScreenGridPanel().setScreenGridProxy(gridOption);
    }

    public boolean checkValidGridOption(String gridOption) {
        return this.mAppsController.getAppsScreenGridPanel().checkValidGridOption(gridOption);
    }

    public boolean checkMatchGridOption(String gridOption) {
        int[] xy = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(this.mAppsController.getLauncher(), xy);
        if ((xy[0] + DefaultLayoutParser.ATTR_X + xy[1]).compareToIgnoreCase(gridOption) == 0) {
            return true;
        }
        return false;
    }

    public boolean onParamFillingReceived(ParamFilling pf) {
        return false;
    }
}
