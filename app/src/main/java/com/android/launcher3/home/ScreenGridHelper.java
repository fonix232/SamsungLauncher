package com.android.launcher3.home;

import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.logging.GSIMLogging;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ScreenGridHelper {
    private static final String TAG = "ScreenGridHelper";
    private boolean mChangeGridState = false;
    private HomeController mHomeController;
    private Launcher mLauncher;
    private CopyOnWriteArrayList<Integer> mNeedNewPageList = new CopyOnWriteArrayList();
    private int mOriginalCellX = 0;
    private int mOriginalCellY = 0;
    private Workspace mWorkspace;

    ScreenGridHelper(Launcher launcher, HomeController homeController) {
        this.mLauncher = launcher;
        this.mHomeController = homeController;
        this.mWorkspace = this.mHomeController.getWorkspace();
    }

    boolean isChangeGridState() {
        return this.mChangeGridState;
    }

    void changeGrid(int cellX, int cellY, boolean animated) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (dp.homeGrid.getCellCountX() != cellX || dp.homeGrid.getCellCountY() != cellY) {
            final int diffX = Math.abs(this.mOriginalCellX - cellX) - 1;
            final int diffY = Math.abs(this.mOriginalCellY - cellY) - 1;
            this.mChangeGridState = true;
            if (this.mWorkspace != null) {
                int i;
                for (i = this.mWorkspace.getChildCount() - 1; i >= 0; i--) {
                    WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) this.mWorkspace.getChildAt(i);
                    if (cellLayout.mTempPage) {
                        cellLayout.removeAllViews();
                        if (cellLayout.mBackupItems != null) {
                            cellLayout.mBackupItems.clear();
                        }
                        this.mWorkspace.removeTempPage(cellLayout);
                    } else {
                        cellLayout.mGridChanged = false;
                        restorePage(cellLayout);
                    }
                }
                dp.setCurrentGrid(cellX, cellY);
                if (this.mNeedNewPageList != null) {
                    this.mNeedNewPageList.clear();
                }
                int currentIndex = this.mWorkspace.getCurrentPage();
                for (i = currentIndex - 1; i <= currentIndex + 1; i++) {
                    WorkspaceCellLayout currentPage = (WorkspaceCellLayout) this.mWorkspace.getChildAt(i);
                    if (!(this.mNeedNewPageList == null || currentPage == null || !doChangeWorkspace(currentPage, animated, diffX, diffY))) {
                        Log.d(TAG, "Change Grid Visible pages");
                        this.mNeedNewPageList.add(Integer.valueOf(i));
                    }
                }
                this.mWorkspace.resetAdditionalPageCount();
                addNewPageOnChangingGrid();
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (ScreenGridHelper.this.mNeedNewPageList != null) {
                            ScreenGridHelper.this.mNeedNewPageList.clear();
                        }
                        int screenCount = ScreenGridHelper.this.mWorkspace.getChildCount();
                        for (int i = 0; i < screenCount; i++) {
                            long screenId = ScreenGridHelper.this.mWorkspace.getScreenIdForPageIndex(i);
                            if (!(screenId == -401 || screenId == -301 || screenId == -501)) {
                                WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) ScreenGridHelper.this.mWorkspace.getChildAt(i);
                                if (!(ScreenGridHelper.this.mNeedNewPageList == null || cellLayout == null || cellLayout.mGridChanged || !ScreenGridHelper.this.doChangeWorkspace(cellLayout, false, diffX, diffY))) {
                                    ScreenGridHelper.this.mNeedNewPageList.add(Integer.valueOf(i));
                                }
                            }
                        }
                        ScreenGridHelper.this.mWorkspace.resetAdditionalPageCount();
                        ScreenGridHelper.this.addNewPageOnChangingGrid();
                        ScreenGridHelper.this.mChangeGridState = false;
                    }
                }, 333);
            }
        }
    }

    private boolean doChangeWorkspace(WorkspaceCellLayout cellLayout, boolean animated, int diffX, int diffY) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellWidth = dp.homeGrid.getCellWidth();
        int cellHeight = dp.homeGrid.getCellHeight();
        int cellGapX = dp.homeGrid.getCellGapX();
        int cellGapY = dp.homeGrid.getCellGapY();
        int cellCountX = dp.homeGrid.getCellCountX();
        int cellCountY = dp.homeGrid.getCellCountY();
        if (cellLayout.mOutSideItems != null) {
            cellLayout.mOutSideItems.clear();
        }
        int position = ScreenGridUtilities.getOutSidePosition(cellLayout.getCellLayoutChildren().getChildrenAllItems(), cellCountX, cellCountY, diffX, diffY);
        cellLayout.setCellDimensions();
        cellLayout.setGridSizeForScreenGrid(cellCountX, cellCountY, animated, position, diffX, diffY);
        int defaultX = diffX > 0 ? 2 : 1;
        int defaultY = diffY > 0 ? 2 : 1;
        for (int i = cellLayout.getPageChildCount() - 1; i > -1; i--) {
            int x;
            int y;
            Pair<ItemInfo, View> pairItem;
            boolean dirty = false;
            View childView = cellLayout.getChildOnPageAt(i);
            ItemInfo item = (ItemInfo) childView.getTag();
            if (item == null) {
                Log.w(TAG, "HomeView: doChangeWorkspace() item is Null");
            }
            if (item != null) {
                x = item.cellX;
                y = item.cellY;
                if (item.spanX > cellCountX) {
                    item.spanX = cellCountX;
                    if (position == 1 || position == 3) {
                        x += defaultX;
                    }
                }
                if (item.spanY > cellCountY) {
                    item.spanY = cellCountY;
                    if (position == 2 || position == 3) {
                        y += defaultY;
                    }
                }
                int spanX = item.spanX;
                int spanY = item.spanY;
                pairItem = Pair.create(item, childView);
                if (position == 0) {
                    if (x + spanX > cellCountX || y + spanY > cellCountY) {
                        addOutsideAndRemovedItem(cellLayout, pairItem);
                    }
                } else if (position == 1) {
                    if (x <= diffX || y + spanY > cellCountY) {
                        addOutsideAndRemovedItem(cellLayout, pairItem);
                    } else {
                        x -= defaultX;
                    }
                } else if (position == 2) {
                    if (x + spanX > cellCountX || y <= diffY) {
                        addOutsideAndRemovedItem(cellLayout, pairItem);
                    } else {
                        y -= defaultY;
                    }
                } else if (position == 3) {
                    if (x <= diffX || y <= diffY) {
                        addOutsideAndRemovedItem(cellLayout, pairItem);
                    } else {
                        x -= defaultX;
                        y -= defaultY;
                    }
                }
                if (!(item.cellX == x && item.cellY == y)) {
                    dirty = true;
                }
                if (childView instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetInfo widgetItem = (LauncherAppWidgetInfo) item;
                    if (widgetItem.hostView != null) {
                        LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) widgetItem.hostView;
                        hostView.setResizeScaleResult(LauncherAppWidgetHostView.calculateWidgetSize(item.spanX, item.spanY, (item.spanX * cellWidth) + (item.spanX >= 2 ? (item.spanX - 1) * cellGapX : 0), (item.spanY * cellHeight) + (item.spanY >= 2 ? (item.spanY - 1) * cellGapY : 0)));
                        AppWidgetResizeFrame.updateWidgetSizeRanges(this.mLauncher, widgetItem.hostView, item.spanX, item.spanY);
                        hostView.invalidate();
                    }
                }
                if (dirty) {
                    item.cellX = x;
                    item.cellY = y;
                    cellLayout.removeItem(item);
                    this.mHomeController.addInScreen(childView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
                }
            }
        }
        List<Pair<ItemInfo, View>> moveItems = cellLayout.getOutSideItems(position);
        List<Pair<ItemInfo, View>> removeFromOutSideItems = new ArrayList();
        if (moveItems != null) {
            for (Pair<ItemInfo, View> pairItem2 : moveItems) {
                item = (ItemInfo) pairItem2.first;
                int[] tmpXY = new int[2];
                x = item.cellX;
                y = item.cellY;
                if (x > cellCountX - 1) {
                    x -= defaultX;
                }
                if (y > cellCountY - 1) {
                    y -= defaultY;
                }
                cellLayout.findNearestVacantAreaWithCell(x, y, item.spanX, item.spanY, tmpXY, true);
                if (!(tmpXY[0] == -1 || tmpXY[1] == -1)) {
                    item.cellX = tmpXY[0];
                    item.cellY = tmpXY[1];
                    cellLayout.removeItem(item);
                    if (item instanceof LauncherAppWidgetInfo) {
                        this.mHomeController.addInScreen(((LauncherAppWidgetInfo) item).hostView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
                    } else {
                        this.mHomeController.addInScreen((View) pairItem2.second, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
                    }
                    cellLayout.markCellsForGrid(item.cellX, item.cellY, item.spanX, item.spanY);
                    removeFromOutSideItems.add(pairItem2);
                }
            }
        }
        if (cellLayout.mOutSideItems != null) {
            for (Pair<ItemInfo, View> it : removeFromOutSideItems) {
                cellLayout.mOutSideItems.remove(it);
            }
        }
        boolean needNewPage = false;
        if (!(cellLayout.mOutSideItems == null || cellLayout.mOutSideItems.isEmpty())) {
            needNewPage = true;
        }
        cellLayout.mGridChanged = true;
        return needNewPage;
    }

    private void addOutsideAndRemovedItem(WorkspaceCellLayout cellLayout, Pair<ItemInfo, View> pairItem) {
        cellLayout.removeItem((ItemInfo) pairItem.first);
        if (cellLayout.mOutSideItems != null) {
            cellLayout.mOutSideItems.add(pairItem);
        }
    }

    private void addNewPageOnChangingGrid() {
        if (this.mNeedNewPageList != null && this.mNeedNewPageList.size() > 0) {
            for (int i = 0; i < this.mNeedNewPageList.size(); i++) {
                int currentIndex = (((Integer) this.mNeedNewPageList.get(i)).intValue() + i) + this.mWorkspace.getAdditionPageCount();
                this.mWorkspace.insertPageAndMoveItems(currentIndex, currentIndex + 1);
            }
        }
    }

    private void restorePage(WorkspaceCellLayout restoreCell) {
        if (restoreCell.mBackupItems != null) {
            restoreCell.restoreGridSize(this.mOriginalCellX, this.mOriginalCellY);
            restoreCell.mRestoredItems.clear();
            int i = 0;
            while (i < restoreCell.mBackupItems.size()) {
                BackupItem backupItem = (BackupItem) restoreCell.mBackupItems.get(restoreCell.mBackupItems.keyAt(i));
                if (backupItem != null) {
                    ItemInfo homeItem = backupItem.getItem();
                    if (homeItem.cellX != backupItem.getCellX() || homeItem.cellY != backupItem.getCellY() || homeItem.screenId != backupItem.getScreen() || homeItem.spanX != backupItem.getSpanX() || homeItem.spanY != backupItem.getSpanY()) {
                        homeItem.cellX = backupItem.getCellX();
                        homeItem.cellY = backupItem.getCellY();
                        homeItem.spanX = backupItem.getSpanX();
                        homeItem.spanY = backupItem.getSpanY();
                        homeItem.screenId = backupItem.getScreen();
                        Pair<ItemInfo, View> pairItem = Pair.create(homeItem, backupItem.getView());
                        if (restoreCell.mOutSideItems.contains(pairItem)) {
                            restoreCell.mRestoredItems.add(pairItem);
                            restoreCell.removeItem(homeItem);
                            if (homeItem instanceof LauncherAppWidgetInfo) {
                                this.mHomeController.addInScreen(((LauncherAppWidgetInfo) homeItem).hostView, homeItem.container, homeItem.screenId, homeItem.cellX, homeItem.cellY, homeItem.spanX, homeItem.spanY);
                            } else {
                                this.mHomeController.addInScreen((View) pairItem.second, homeItem.container, homeItem.screenId, homeItem.cellX, homeItem.cellY, homeItem.spanX, homeItem.spanY);
                            }
                        } else {
                            restoreCell.updateItem(homeItem);
                        }
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    void restoreGridLayout() {
        changeGrid(this.mOriginalCellX, this.mOriginalCellY, false);
    }

    void applyGridChange(int delay) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        ScreenGridUtilities.storeGridLayoutPreference(this.mLauncher, dp.homeGrid.getCellCountX(), dp.homeGrid.getCellCountY(), LauncherAppState.getInstance().isHomeOnlyModeEnabled());
        ScreenGridUtilities.storeCurrentScreenGridSetting(this.mLauncher, dp.homeGrid.getCellCountX(), dp.homeGrid.getCellCountY());
        ScreenGridUtilities.storeChangeGridValue(this.mLauncher);
        dp.layoutGrid(this.mLauncher);
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_GRID_STATUS, Integer.toString(dp.homeGrid.getCellCountX()) + DefaultLayoutParser.ATTR_X + Integer.toString(dp.homeGrid.getCellCountY()), -1, true);
        int screenCount = this.mWorkspace.getChildCount();
        for (int i = 0; i < screenCount; i++) {
            WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) this.mWorkspace.getChildAt(i);
            cellLayout.mTempPage = false;
            for (int j = 0; j < cellLayout.getPageChildCount(); j++) {
                final ItemInfo homeItem = (ItemInfo) cellLayout.getChildOnPageAt(j).getTag();
                if (homeItem != null) {
                    BackupItem backupItem = (BackupItem) cellLayout.mBackupItems.get(homeItem.screenId);
                    if (!(backupItem != null && homeItem.cellX == backupItem.getCellX() && homeItem.cellY == backupItem.getCellY() && homeItem.spanX == backupItem.getSpanX() && homeItem.spanY == backupItem.getSpanY())) {
                        Log.i(TAG, "db update item = " + homeItem.title + " " + homeItem.componentName);
                        homeItem.requiresDbUpdate = false;
                        this.mHomeController.modifyItemInDb(homeItem, -100, homeItem.screenId, homeItem.cellX, homeItem.cellY, homeItem.spanX, homeItem.spanY);
                    }
                    if (homeItem instanceof FolderInfo) {
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                FolderView folder = ((FolderIconView) ScreenGridHelper.this.mHomeController.getHomescreenIconByItemId(homeItem.id)).getFolderView();
                                folder.getContent().updateFolderGrid();
                                folder.rearrangeChildren();
                            }
                        }, (long) delay);
                    }
                }
            }
            cellLayout.updateOccupied();
        }
        this.mNeedNewPageList.clear();
    }

    void backupOriginalData() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        this.mOriginalCellX = dp.homeGrid.getCellCountX();
        this.mOriginalCellY = dp.homeGrid.getCellCountY();
        int screenCount = this.mWorkspace.getChildCount();
        for (int i = 0; i < screenCount; i++) {
            WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) this.mWorkspace.getChildAt(i);
            if (cellLayout.mBackupItems != null) {
                cellLayout.mBackupItems.clear();
                for (int j = 0; j < cellLayout.getPageChildCount(); j++) {
                    BackupItem backup = new BackupItem();
                    View childView = cellLayout.getChildOnPageAt(j);
                    ItemInfo item = (ItemInfo) childView.getTag();
                    if (item != null) {
                        backup.setItem(item);
                        backup.setView(childView);
                        cellLayout.mBackupItems.put(item.id, backup);
                    }
                }
            }
        }
    }
}
