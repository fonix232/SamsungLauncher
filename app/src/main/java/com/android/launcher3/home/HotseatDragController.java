package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderIconDropController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.PinnedShortcutUtils;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class HotseatDragController implements DragListener, DropTarget, DragSource, DragState {
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final int REORDER_DELAY = 150;
    private static final Comparator<DragObject> SCREENID_COMPARATOR = new Comparator<DragObject>() {
        public final int compare(DragObject a, DragObject b) {
            ItemInfo aInfo = a.dragInfo;
            ItemInfo bInfo = b.dragInfo;
            if (aInfo.screenId == bInfo.screenId) {
                return 0;
            }
            if (aInfo.screenId < bInfo.screenId) {
                return -1;
            }
            return 1;
        }
    };
    private static AnimatorSet mReorderAnimSet;
    private HotseatCellLayout mContent;
    private CellInfo mDragInfo;
    private DragLayer mDragLayer;
    private int mDragMode = 0;
    private float[] mDragViewVisualCenter = new float[2];
    private int mEmptyCellRank;
    private FolderIconDropController mFolderController;
    private FolderLock mFolderLock;
    private HomeController mHomeController;
    private Hotseat mHotseat;
    private Launcher mLauncher;
    private int mPrevTargetRank;
    private final Alarm mReorderAlarm = new Alarm();
    OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            if (HotseatDragController.mReorderAnimSet == null || !HotseatDragController.mReorderAnimSet.isRunning()) {
                HotseatDragController.mReorderAnimSet = HotseatDragController.this.mContent.realTimeReorder(HotseatDragController.this.mEmptyCellRank, HotseatDragController.this.mTargetRank);
                if (HotseatDragController.mReorderAnimSet != null) {
                    HotseatDragController.mReorderAnimSet.start();
                }
                HotseatDragController.this.mEmptyCellRank = HotseatDragController.this.mTargetRank;
            }
        }
    };
    private boolean mRestorePosition = false;
    private int[] mTargetCell = new int[2];
    private int mTargetRank;
    private int[] mTempPt = new int[2];

    HotseatDragController(Context context, Hotseat hotseat) {
        this.mLauncher = (Launcher) context;
        this.mHotseat = hotseat;
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    void setup(HomeController homeController) {
        this.mDragLayer = this.mLauncher.getDragLayer();
        this.mHomeController = homeController;
        this.mContent = (HotseatCellLayout) this.mHotseat.getLayout();
        this.mFolderController = new FolderIconDropController(this.mLauncher, this);
        this.mFolderController.setReorderTarget(this.mContent);
        initDragRanks();
    }

    private void initDragRanks() {
        this.mTargetRank = -1;
        this.mPrevTargetRank = -1;
        this.mEmptyCellRank = -1;
    }

    void startDrag(CellInfo cellInfo, boolean isSource) {
        this.mDragInfo = cellInfo;
        if (mReorderAnimSet == null) {
            mReorderAnimSet = new AnimatorSet();
        }
        if (isSource) {
            this.mContent.removeView(this.mDragInfo.cell);
        }
        this.mRestorePosition = true;
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        return this.mHomeController.getState() == 2 || this.mHomeController.getState() == 6;
    }

    public boolean onDragEnd() {
        if (!(this.mDragInfo == null || this.mDragInfo.container == -101)) {
            this.mDragInfo = null;
        }
        initDragRanks();
        this.mHotseat.changeGrid(true);
        if (this.mHomeController.getState() == 2 || this.mHomeController.getState() == 6) {
            return true;
        }
        return false;
    }

    public boolean isDropEnabled(boolean isDrop) {
        return true;
    }

    public void onDrop(DragObject d) {
        commitTempPlacement();
        this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
        mapPointFromSelfToHotseatLayout(this.mDragViewVisualCenter);
        if (this.mDragInfo == null) {
            onDropExternal(d);
            return;
        }
        Runnable anonymousClass1 = new Runnable() {
            public void run() {
                HotseatDragController.this.mHomeController.exitDragStateDelayed();
            }
        };
        View cell = this.mDragInfo.cell;
        CellLayout originalLayout = this.mDragInfo.layout;
        boolean isFolderDrop = d.dragInfo instanceof FolderInfo;
        boolean cancelDropFolder = false;
        boolean hasMovedLayout = this.mDragInfo.container != -101;
        if (!d.cancelled) {
            this.mTargetCell = this.mContent.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
            d.postAnimationRunnable = anonymousClass1;
            if (this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, cell, hasMovedLayout ? originalLayout : null, d)) {
                if (isFolderDrop) {
                    cancelDropFolder = true;
                } else {
                    if (this.mContent.hasEmptyCell()) {
                        removeEmptyCells(true, true);
                    }
                    this.mHomeController.notifyCapture(false);
                    return;
                }
            }
            if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, d)) {
                if (isFolderDrop) {
                    cancelDropFolder = true;
                } else {
                    if (hasMovedLayout && originalLayout != null) {
                        originalLayout.removeView(cell);
                    }
                    if (this.mContent.hasEmptyCell()) {
                        removeEmptyCells(true, true);
                    }
                    this.mHomeController.notifyCapture(false);
                    this.mHomeController.updateNotificationHelp(true);
                    return;
                }
            }
            if (d.extraDragInfoList != null) {
                makeEmptyCells(this.mTargetRank, d.extraDragInfoList.size(), true, true);
            }
            this.mTargetCell = this.mContent.findNearestVacantArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
            this.mReorderAlarm.cancelAlarm();
            ItemInfo item = d.dragInfo;
            boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0;
            if (isDragWidget(d) || this.mContent.isFull()) {
                foundCell = false;
            }
            ItemInfo info = (ItemInfo) cell.getTag();
            if (foundCell) {
                if (hasMovedLayout) {
                    if (originalLayout != null) {
                        originalLayout.removeView(cell);
                    } else {
                        Log.d(DropTarget.TAG, "mDragInfo.cell has null parent");
                    }
                }
                this.mHomeController.addInScreen(cell, -101, -1, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                LayoutParams lp = (LayoutParams) cell.getLayoutParams();
                int i = this.mTargetCell[0];
                lp.tmpCellX = i;
                lp.cellX = i;
                i = this.mTargetCell[1];
                lp.tmpCellY = i;
                lp.cellY = i;
                lp.cellHSpan = item.spanX;
                lp.cellVSpan = item.spanY;
                lp.isLockedToGrid = true;
                lp.useTmpCoords = false;
                this.mHomeController.modifyItemInDb(info, -101, -1, lp.cellX, lp.cellY, item.spanX, item.spanY);
                sayDragTalkBack(true, false, lp.cellX, lp.cellY);
                if (!(!hasMovedLayout && this.mDragInfo.cellX == this.mTargetCell[0] && this.mDragInfo.cellY == this.mTargetCell[1])) {
                    this.mHomeController.notifyCapture(false);
                    this.mHomeController.updateNotificationHelp(true);
                }
            } else {
                if (((ItemInfo) cell.getTag()).container == -100) {
                    cell.setVisibility(View.VISIBLE);
                    if (originalLayout != null) {
                        originalLayout.markCellsAsOccupiedForView(cell);
                    }
                }
                this.mHomeController.updateCountBadge(cell, false);
                if (d.extraDragInfoList != null && d.extraDragInfoList.size() > 0) {
                    Iterator it = d.extraDragInfoList.iterator();
                    while (it.hasNext()) {
                        DragObject object = (DragObject) it.next();
                        restoreExtraDropItems(object, false);
                        object.cancelled = false;
                    }
                }
                if (d.dragView.getParent() != null) {
                    d.deferDragViewCleanupPostAnimation = false;
                    this.mDragLayer.removeView(d.dragView);
                }
                anonymousClass1.run();
                this.mHomeController.showNoSpacePageforHotseat();
                return;
            }
        } else if (d.cancelled && hasMovedLayout && !this.mRestorePosition && originalLayout != null) {
            originalLayout.removeView(cell);
        }
        if (!(cell == null || cell.getParent() == null)) {
            CellLayout parent = (CellLayout) cell.getParent().getParent();
            if (d.dragView.hasDrawn()) {
                this.mDragLayer.animateViewIntoPosition(d.dragView, cell, 300, anonymousClass1, this.mHotseat);
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(View.VISIBLE);
                this.mHomeController.updateCountBadge(cell, false);
                anonymousClass1.run();
            }
            parent.onDropChild(cell);
        }
        if (!cancelDropFolder && d.extraDragInfoList != null) {
            onDropExtraObjects(d.extraDragInfoList, this.mRestorePosition, false, true, false);
        }
    }

    private void getFinalPositionForDropAnimation(int[] loc, DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell) {
        int spanX = info.spanX;
        int spanY = info.spanY;
        Rect r = new Rect();
        this.mContent.cellToRect(targetCell[0], targetCell[1], spanX, spanY, r);
        loc[0] = r.left;
        loc[1] = r.top;
        float cellLayoutScale = this.mDragLayer.getDescendantCoordRelativeToSelf(layout, loc, true);
        float iconMarginTop = (float) this.mLauncher.getDeviceProfile().hotseatGridIcon.getContentTop();
        if (iconMarginTop < 0.0f) {
            iconMarginTop = 0.0f;
        }
        if (this.mLauncher.getDeviceProfile().isLandscape) {
            loc[0] = loc[0] + ((dragView.getMeasuredWidth() - layout.getContentIconSize()) / 2);
            loc[1] = loc[1] + ((layout.getCellHeight() - dragView.getMeasuredHeight()) / 2);
        } else {
            loc[0] = (int) (((float) loc[0]) - ((((float) dragView.getMeasuredWidth()) - (((float) r.width()) * cellLayoutScale)) / 2.0f));
            loc[1] = (int) (((float) loc[1]) + (((float) Math.round(cellLayoutScale * iconMarginTop)) - ((((float) dragView.getMeasuredHeight()) * (1.0f - cellLayoutScale)) / 2.0f)));
        }
        if (dragView.getDragVisualizeOffset() != null) {
            loc[1] = loc[1] - Math.round(((float) dragView.getDragVisualizeOffset().y) * cellLayoutScale);
        }
    }

    private void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView, Runnable onCompleteRunnable) {
        Rect from = new Rect();
        this.mDragLayer.getViewRectRelativeToSelf(dragView, from);
        int[] finalPos = new int[2];
        getFinalPositionForDropAnimation(finalPos, dragView, cellLayout, info, this.mTargetCell);
        this.mDragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0], finalPos[1], 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, onCompleteRunnable, 2, 300, this.mHotseat);
    }

    private void onDropExternalFromWidget(DragObject d) {
        final PendingAddItemInfo info = d.dragInfo;
        this.mTargetCell = this.mContent.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
        if (isDragOverAppsButton(this.mTargetCell)) {
            this.mTargetCell = this.mContent.findNearestVacantArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
        }
        float distance = this.mContent.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
        boolean willAddToFolder = false;
        View dropOverView;
        if (this.mFolderController.willCreateUserFolder((ItemInfo) d.dragInfo, null, this.mTargetCell, distance, true) || this.mFolderController.willAddToExistingUserFolder((ItemInfo) d.dragInfo, this.mTargetCell, distance)) {
            willAddToFolder = true;
            dropOverView = this.mContent.getChildAt(this.mTargetCell[0], this.mTargetCell[1]);
            if (this.mContent.hasEmptyCell()) {
                removeEmptyCells(true, true);
                this.mTargetCell[0] = ((ItemInfo) dropOverView.getTag()).cellX;
                this.mTargetCell[1] = ((ItemInfo) dropOverView.getTag()).cellY;
            }
        } else if (this.mContent.isFull()) {
            if (d.dragView.getParent() != null) {
                d.deferDragViewCleanupPostAnimation = false;
                this.mDragLayer.removeView(d.dragView);
            }
            this.mHomeController.exitDragStateDelayed();
            this.mHomeController.showNoSpacePageforHotseat();
            return;
        } else if (this.mContent.hasEmptyCell()) {
            dropOverView = this.mContent.getChildAt(this.mTargetCell[0], this.mTargetCell[1]);
            if (dropOverView != null) {
                removeEmptyCells(true, true);
                this.mTargetCell[0] = ((ItemInfo) dropOverView.getTag()).cellX;
                this.mTargetCell[1] = ((ItemInfo) dropOverView.getTag()).cellY;
            }
        }
        Runnable onAnimationCompleteRunnable = new Runnable() {
            public void run() {
                HotseatDragController.this.mHomeController.addPendingItem(info, -101, -1, HotseatDragController.this.mTargetCell, info.spanX, info.spanY);
            }
        };
        if (willAddToFolder) {
            d.deferDragViewCleanupPostAnimation = false;
            onAnimationCompleteRunnable.run();
        } else {
            animateWidgetDrop(info, this.mContent, d.dragView, onAnimationCompleteRunnable);
        }
        sayDragTalkBack(false, false, this.mTargetCell[0], this.mTargetCell[1]);
    }

    private void onDropExternalFromOther(DragObject d) {
        Runnable anonymousClass3 = new Runnable() {
            public void run() {
                HotseatDragController.this.mHomeController.exitDragStateDelayed();
            }
        };
        ItemInfo info = d.dragInfo;
        boolean isAcceptItem = false;
        if (info instanceof PendingAddPinShortcutInfo) {
            LauncherAppsCompat.acceptPinItemRequest(this.mLauncher, ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat(), 0);
            isAcceptItem = true;
            ItemInfo shortcutInfo = ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().createShortcutInfo();
            if (shortcutInfo != null) {
                info = shortcutInfo;
            } else {
                anonymousClass3.run();
                if (this.mContent.hasEmptyCell()) {
                    removeEmptyCells(true, true);
                    return;
                }
                return;
            }
        }
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (((info instanceof IconInfo) || (info instanceof FolderInfo)) && info.isAppOrShortcutType()) {
            ItemInfo localInfo;
            View view;
            boolean cancelDropFolder;
            boolean isFolderDrop = d.dragInfo instanceof FolderInfo;
            boolean isFromHomeFolder = (info.isContainApps() || this.mHomeController.getHomescreenIconByItemId(info.container) == null) ? false : true;
            ArrayList<DragObject> canceledObjects = new ArrayList();
            switch (info.itemType) {
                case 0:
                case 1:
                case 6:
                case 7:
                    if (isFromHomeFolder) {
                        localInfo = info;
                    } else {
                        localInfo = ((IconInfo) info).makeCloneInfo();
                    }
                    view = this.mHomeController.getBindController().createShortcut(this.mContent, (IconInfo) localInfo);
                    break;
                case 2:
                    localInfo = ((FolderInfo) info).makeCloneInfo();
                    if (this.mFolderLock != null && ((FolderInfo) info).isLocked()) {
                        this.mFolderLock.addLockedRecords((FolderInfo) localInfo);
                    }
                    if (localInfo != null) {
                        ((FolderInfo) localInfo).setAlphabeticalOrder(false, true, this.mLauncher);
                    }
                    view = FolderIconView.fromXml(this.mLauncher, this.mContent, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 1);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
            if (info != localInfo) {
                info = localInfo;
            }
            refreshFolderBadge(info);
            if (this.mDragViewVisualCenter != null) {
                this.mTargetCell = this.mContent.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanX, spanY, this.mTargetCell);
                d.postAnimationRunnable = anonymousClass3;
                if (!this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, view, null, d)) {
                    cancelDropFolder = false;
                } else if (isFolderDrop || Utilities.hasFolderItem(d.extraDragInfoList)) {
                    cancelDropFolder = true;
                } else {
                    if (this.mContent.hasEmptyCell()) {
                        removeEmptyCells(true, true);
                    }
                    PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
                    return;
                }
                if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, d)) {
                    if (isFolderDrop || Utilities.hasFolderItem(d.extraDragInfoList)) {
                        cancelDropFolder = true;
                    } else {
                        if (this.mContent.hasEmptyCell()) {
                            removeEmptyCells(true, true);
                        }
                        PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
                        return;
                    }
                }
                this.mReorderAlarm.cancelAlarm();
                if (!cancelDropFolder) {
                    this.mTargetCell = this.mContent.findNearestVacantArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanX, spanY, this.mTargetCell);
                } else if (!isFolderDrop) {
                    if (d.extraDragInfoList != null) {
                        canceledObjects.addAll(onDropExtraObjects(d.extraDragInfoList, false, cancelDropFolder, false, true));
                    }
                    if (canceledObjects.size() > 0) {
                        this.mHomeController.dropCompletedFromHotseat(canceledObjects, anonymousClass3, true, (d.extraDragInfoList != null ? d.extraDragInfoList.size() : canceledObjects.size()) + 1);
                        return;
                    }
                    return;
                }
            }
            this.mContent.findCellForSpan(this.mTargetCell, 1, 1, false);
            cancelDropFolder = false;
            boolean isFull = this.mContent.isFull();
            if (isFull || (cancelDropFolder && isFolderDrop)) {
                canceledObjects.add(d);
            } else {
                if (!info.isContainApps() && d.extraDragInfoList == null) {
                    View folder = this.mHomeController.getHomescreenIconByItemId(info.container);
                    if (folder instanceof FolderIconView) {
                        ((FolderIconView) folder).getFolderView().updateDeletedFolder();
                    }
                }
                if (d.extraDragInfoList != null) {
                    makeEmptyCells(this.mTargetRank, d.extraDragInfoList.size(), true, true);
                }
                this.mHomeController.addOrMoveItemInDb(info, -101, -1, this.mTargetCell[0], this.mTargetCell[1], -1);
                if (info instanceof FolderInfo) {
                    this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
                    if (this.mFolderLock != null && ((FolderInfo) info).isLocked()) {
                        this.mFolderLock.addLockedRecords((FolderInfo) localInfo);
                    }
                }
                this.mHomeController.addInScreen(view, -101, -1, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                sayDragTalkBack(false, false, this.mTargetCell[0], this.mTargetCell[1]);
                this.mContent.onDropChild(view);
                this.mContent.getCellLayoutChildren().measureChild(view);
                if (d.dragView != null) {
                    this.mDragLayer.animateViewIntoPosition(d.dragView, view, anonymousClass3, this.mHotseat);
                }
            }
            if (d.extraDragInfoList != null) {
                canceledObjects.addAll(onDropExtraObjects(d.extraDragInfoList, false, cancelDropFolder, false, true));
            }
            if (isFull) {
                anonymousClass3.run();
                restoreExtraDropItems(d, isFromHomeFolder);
                this.mHomeController.showNoSpacePageforHotseat();
                if ((info.itemType == 6 || isAcceptItem) && (d.dragInfo instanceof PendingAddPinShortcutInfo)) {
                    this.mLauncher.startAddItemActivity(((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat(), false);
                    return;
                }
                return;
            }
            if (canceledObjects.size() > 0) {
                this.mHomeController.dropCompletedFromHotseat(canceledObjects, anonymousClass3, true, (d.extraDragInfoList != null ? d.extraDragInfoList.size() : canceledObjects.size()) + 1);
            }
            PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
            if (isAcceptItem) {
                PinnedShortcutUtils.unpinShortcutIfAppTarget(new ShortcutInfoCompat(((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat().getShortcutInfo()), this.mLauncher);
            }
        }
    }

    private void onDropExternal(DragObject d) {
        ItemInfo info = d.dragInfo;
        if (!(info instanceof PendingAddItemInfo) || ((PendingAddItemInfo) info).getProviderInfo() == null) {
            onDropExternalFromOther(d);
        } else {
            onDropExternalFromWidget(d);
        }
        this.mHomeController.notifyCapture(false);
        this.mHomeController.updateNotificationHelp(true);
    }

    private ArrayList<DragObject> onDropExtraObjects(ArrayList<DragObject> extraDragObjects, boolean restored, boolean cancelDropFolder, boolean notExternal, boolean fromOther) {
        View folder;
        ArrayList<DragObject> restoredHotseatObjects = new ArrayList();
        ArrayList<DragObject> canceledObjects = new ArrayList();
        boolean restoreDeepShortCut = false;
        Iterator it = extraDragObjects.iterator();
        while (it.hasNext()) {
            ItemInfo info;
            DragObject d = (DragObject) it.next();
            if (!cancelDropFolder || (d.dragInfo instanceof FolderInfo)) {
                if (isDragWidget(d) || this.mContent.isFull()) {
                    if (notExternal) {
                        d.cancelled = true;
                    }
                    d.restored = restored;
                    canceledObjects.add(d);
                } else {
                    ItemInfo info2 = d.dragInfo;
                    if (restored && info2.container == -101) {
                        restoredHotseatObjects.add(d);
                    } else {
                        long container = restored ? info2.container : -101;
                        if (container == -101) {
                            View view;
                            boolean isItemInFolder = this.mHomeController.isItemInFolder(info2);
                            folder = null;
                            if (isItemInFolder) {
                                folder = this.mHomeController.getHomescreenIconByItemId(info2.container);
                                if (!restored) {
                                    restoreDeepShortCut = info2.itemType == 6;
                                    this.mHomeController.removeHomeOrFolderItem(info2, d.dragView, restoreDeepShortCut);
                                }
                            }
                            boolean isFromHomeFolder = (info2.isContainApps() || this.mHomeController.getHomescreenIconByItemId(info2.container) == null) ? false : true;
                            ItemInfo localInfo = null;
                            if (equals(d.dragSource) || (d.dragSource instanceof WorkspaceDragController)) {
                                view = d.dragView.getSourceView();
                            } else {
                                switch (info2.itemType) {
                                    case 0:
                                    case 1:
                                    case 6:
                                    case 7:
                                        if (isFromHomeFolder) {
                                            localInfo = info2;
                                        } else {
                                            localInfo = ((IconInfo) info2).makeCloneInfo();
                                        }
                                        view = this.mHomeController.getBindController().createShortcut(this.mContent, (IconInfo) localInfo);
                                        break;
                                    case 2:
                                        localInfo = ((FolderInfo) info2).makeCloneInfo();
                                        view = FolderIconView.fromXml(this.mLauncher, this.mContent, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 1);
                                        break;
                                    default:
                                        throw new IllegalStateException("Unknown item type: " + info2.itemType);
                                }
                            }
                            if (localInfo == null || info2 == localInfo) {
                                info = info2;
                            } else {
                                info = localInfo;
                            }
                            if (restored) {
                                this.mTargetCell[0] = info.cellX;
                                this.mTargetCell[1] = info.cellY;
                            } else {
                                this.mContent.findCellForSpan(this.mTargetCell, 1, 1, false);
                            }
                            this.mTargetRank = this.mContent.cellToPosition(this.mTargetCell[0], this.mTargetCell[1]);
                            this.mEmptyCellRank = this.mTargetRank;
                            if (!restored || !isItemInFolder) {
                                this.mHomeController.addOrMoveItemInDb(info, container, -1, this.mTargetCell[0], this.mTargetCell[1], -1, restoreDeepShortCut);
                                if (fromOther && (info instanceof FolderInfo)) {
                                    this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
                                }
                                this.mHomeController.addInScreen(view, container, -1, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                                this.mContent.onDropChild(view);
                                this.mContent.getCellLayoutChildren().measureChild(view);
                                if (d.dragView != null) {
                                    this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view, null, this.mHotseat);
                                }
                            } else if (folder != null) {
                                FolderInfo folderInfo = (FolderInfo) folder.getTag();
                                this.mTargetCell[0] = folderInfo.cellX;
                                this.mTargetCell[1] = folderInfo.cellY;
                                addToExistingFolderIfNecessary(this.mTargetCell, d);
                            }
                        } else {
                            d.cancelled = true;
                            d.restored = restored;
                            info = info2;
                        }
                    }
                }
            }
        }
        if (restoredHotseatObjects.size() > 0) {
            restoreHotseatObjects(restoredHotseatObjects);
        }
        commitTempPlacement();
        if (extraDragObjects.size() > 0 && canceledObjects.size() == 0) {
            info = (ItemInfo) ((DragObject) extraDragObjects.get(0)).dragInfo;
            if (!(notExternal || info.isContainApps())) {
                folder = this.mHomeController.getHomescreenIconByItemId(info.container);
                if (folder instanceof FolderIconView) {
                    ((FolderIconView) folder).getFolderView().updateDeletedFolder();
                }
            }
        }
        return canceledObjects;
    }

    public void onDragEnter(DragObject dragObject, boolean dropTargetChanged) {
        if (!isDragWidget(dragObject)) {
            this.mContent.markCellsAsOccupiedForAllChild();
            this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
            mapPointFromSelfToHotseatLayout(this.mDragViewVisualCenter);
            if (this.mContent.isFull() || this.mContent.hasEmptyCell()) {
                this.mTargetCell = this.mContent.findNearestVacantArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
                int cellToPosition = this.mContent.cellToPosition(this.mTargetCell[0], this.mTargetCell[1]);
                this.mEmptyCellRank = cellToPosition;
                this.mTargetRank = cellToPosition;
            } else {
                setDragEnterTarget(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1]);
                makeEmptyCell(this.mTargetRank, true, false);
                this.mEmptyCellRank = this.mTargetRank;
            }
            this.mFolderController.onDragEnter();
            this.mFolderController.setReorderTarget(this.mContent);
            this.mFolderController.setMaxDistance(this.mLauncher.getDeviceProfile().hotseatGridIcon.getIconSize());
            this.mPrevTargetRank = -1;
            setDragMode(0);
            this.mHomeController.showCancelDropTarget();
        }
    }

    public void onDragExit(DragObject dragObject, boolean dropTargetChanged) {
        if (!isDragWidget(dragObject)) {
            if (this.mDragInfo != null && dragObject.cancelled) {
                this.mHomeController.updateCountBadge(this.mDragInfo.cell, false);
            }
            if (dropTargetChanged) {
                this.mRestorePosition = false;
            }
            if (!dropTargetChanged && dragObject.cancelled && this.mHomeController.getState() == 2) {
                removeEmptyCells(true, true);
                this.mHomeController.exitDragStateDelayed();
            }
            this.mFolderController.onDragExit(this.mDragMode);
            this.mContent.clearDragOutlines();
            setDragMode(0);
            if (!dragObject.dragComplete) {
                this.mContent.markCellsAsOccupiedForAllChild();
                if (this.mContent.hasEmptyCell()) {
                    removeEmptyCells(true, false);
                }
            }
        }
    }

    public void onDragOver(DragObject d) {
        if (!isReorderRunning()) {
            ItemInfo item = d.dragInfo;
            if (item == null) {
                Log.d(DropTarget.TAG, "DragObject has null info");
            } else if (item.spanX < 0 || item.spanY < 0) {
                throw new RuntimeException("Improper spans found");
            } else {
                this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
                if (!isDragWidget(d)) {
                    mapPointFromSelfToHotseatLayout(this.mDragViewVisualCenter);
                    this.mTargetCell = this.mContent.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
                    if (!(this.mTargetCell[0] == item.cellX && this.mTargetCell[1] == item.cellY)) {
                        this.mRestorePosition = false;
                    }
                    this.mFolderController.onDragOver(this.mDragViewVisualCenter, this.mTargetCell, d, this.mDragInfo, this.mDragMode);
                    if (!this.mContent.isFull()) {
                        if (this.mDragMode == 1 || this.mDragMode == 2) {
                            this.mReorderAlarm.cancelAlarm();
                            this.mPrevTargetRank = this.mEmptyCellRank;
                            if (isDragOverAppsButton(this.mTargetCell)) {
                                setDragMode(0);
                                return;
                            }
                            return;
                        }
                        if (setReorderTarget()) {
                            this.mContent.visualizeDropLocation((ItemInfo) d.dragInfo, d.dragView.getDragOutline(), this.mTargetCell[0], this.mTargetCell[1], 1, 1, false);
                        } else {
                            this.mContent.clearDragOutlines();
                        }
                        if (this.mTargetRank != this.mPrevTargetRank) {
                            setDragMode(0);
                            this.mReorderAlarm.setOnAlarmListener(this.mReorderAlarmListener);
                            this.mReorderAlarm.setAlarm(150);
                            this.mPrevTargetRank = this.mTargetRank;
                        } else if (this.mPrevTargetRank != this.mEmptyCellRank && !this.mReorderAlarm.alarmPending()) {
                            this.mPrevTargetRank = this.mEmptyCellRank;
                        }
                    }
                }
            }
        }
    }

    public boolean acceptDrop(DragObject d) {
        if (isDragWidget(d)) {
            return false;
        }
        return true;
    }

    public View getTargetView() {
        return this.mHotseat;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        int screenHeight = Utilities.getFullScreenHeight(this.mLauncher);
        this.mDragLayer.getDescendantRectRelativeToSelf(this.mHotseat, outRect);
        if (!this.mHotseat.isVerticalHotseat()) {
            outRect.bottom = screenHeight;
        }
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (!dp.isLandscape) {
            outRect.top -= this.mLauncher.getDeviceProfile().getMultiWindowPanelSize();
        } else if (!dp.isMultiwindowMode) {
        } else {
            if (Utilities.getNavigationBarPositon() == 1) {
                outRect.left -= dp.getMultiWindowPanelSize() + dp.navigationBarHeight;
            } else {
                outRect.left -= dp.getMultiWindowPanelSize();
            }
        }
    }

    public int getLeft() {
        return this.mHotseat.getLeft();
    }

    public int getTop() {
        return this.mHotseat.getTop();
    }

    public int getOutlineColor() {
        return this.mLauncher.getOutlineColor();
    }

    public Stage getController() {
        return this.mHomeController;
    }

    public int getIntrinsicIconSize() {
        return this.mLauncher.getDeviceProfile().hotseatGridIcon.getIconSize();
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (this.mDragInfo != null) {
            if (!success) {
                removeEmptyCells(true, false);
                this.mHomeController.addInScreen(this.mDragInfo.cell, -101, -1, this.mDragInfo.cellX, this.mDragInfo.cellY, 1, 1);
                this.mContent.onDropChild(this.mDragInfo.cell);
                if (d.cancelled) {
                    this.mHomeController.exitDragStateDelayed();
                }
                d.deferDragViewCleanupPostAnimation = false;
            }
            if ((!success || d.cancelled) && this.mDragInfo.cell != null) {
                this.mDragInfo.cell.setVisibility(View.VISIBLE);
            }
            if (this.mContent.hasEmptyCell()) {
                removeEmptyCells(true, true);
            }
            if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing()) {
                this.mLauncher.getQuickOptionManager().startBounceAnimation();
            }
            this.mDragInfo = null;
            commitTempPlacement();
            this.mHomeController.updateNotificationHelp(true);
        }
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        if (dragObject == null) {
            return 0;
        }
        boolean app;
        boolean folder;
        ItemInfo info = dragObject.dragInfo;
        if (info.itemType == 0) {
            app = true;
        } else {
            app = false;
        }
        if (info.itemType == 2) {
            folder = true;
        } else {
            folder = false;
        }
        boolean homeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        int optionFlags = (0 | 1) | 32;
        if (homeOnlyMode) {
            optionFlags |= 4096;
        }
        if (homeOnlyMode) {
            optionFlags |= 8192;
        }
        if (app) {
            optionFlags |= 64;
        }
        if (app) {
            optionFlags |= 128;
        }
        if (!folder) {
            optionFlags |= 2;
        }
        if (app) {
            optionFlags |= 256;
        }
        if (app) {
            optionFlags |= 512;
        }
        if (!folder) {
            optionFlags |= 1024;
        }
        if (folder) {
            optionFlags |= 8;
        }
        if (folder) {
            optionFlags |= 2048;
        }
        if (app) {
            optionFlags |= 16384;
        }
        if (app) {
            optionFlags |= 32768;
        }
        if (LauncherFeature.supportSetToZeroPage() && app) {
            optionFlags |= 65536;
        }
        return optionFlags;
    }

    public void setDragMode(int dragMode) {
        if (dragMode != this.mDragMode) {
            if (dragMode == 0) {
                this.mReorderAlarm.cancelAlarm();
                this.mFolderController.cleanup();
            } else if (dragMode == 2) {
                this.mReorderAlarm.cancelAlarm();
            } else if (dragMode == 1) {
                this.mReorderAlarm.cancelAlarm();
            } else if (dragMode == 3) {
                this.mFolderController.cleanup();
            } else if (dragMode == 4) {
                this.mReorderAlarm.cancelAlarm();
                this.mFolderController.cleanup();
                this.mHomeController.enterNormalState(false);
            }
            this.mDragMode = dragMode;
        }
    }

    public void commit(CellLayout layout) {
        this.mHomeController.updateItemLocationsInDatabase(layout);
    }

    public FolderIconView addFolder(CellLayout layout, IconInfo destInfo) {
        return this.mHomeController.getBindController().addFolder(layout, -101, -1, destInfo.cellX, destInfo.cellY);
    }

    public boolean canOpenFolder() {
        return this.mHomeController.canMoveHometray();
    }

    void mapPointFromSelfToHotseatLayout(float[] xy) {
        this.mTempPt[0] = (int) xy[0];
        this.mTempPt[1] = (int) xy[1];
        this.mDragLayer.getDescendantCoordRelativeToSelf(this.mHotseat, this.mTempPt, true);
        this.mDragLayer.mapCoordInSelfToDescendent(this.mContent, this.mTempPt);
        xy[0] = (float) this.mTempPt[0];
        xy[1] = (float) this.mTempPt[1];
    }

    private boolean isDragWidget(DragObject d) {
        return (d.dragInfo instanceof LauncherAppWidgetInfo) || (d.dragInfo instanceof PendingAddWidgetInfo);
    }

    boolean createUserFolderIfNecessary(int[] targetCell, View newView, DragObject d) {
        return this.mFolderController.onDropCreateUserFolder(null, targetCell, newView, null, d);
    }

    boolean addToExistingFolderIfNecessary(int[] targetCell, DragObject d) {
        return this.mFolderController.onDropAddToExistingFolder(null, targetCell, d);
    }

    void setDragEnterTarget(float x, float y) {
        this.mTargetCell = this.mContent.findNearestArea((int) x, (int) y, 1, 1, this.mTargetCell);
        int[] regionToCenterPoint = new int[2];
        this.mContent.regionToCenterPoint(this.mTargetCell[0], this.mTargetCell[1], 1, 1, regionToCenterPoint);
        int centerPoint = this.mContent.cellToPosition(regionToCenterPoint[0], regionToCenterPoint[1]);
        if (this.mHotseat.isVerticalHotseat()) {
            if (((float) centerPoint) < y) {
                this.mTargetRank = this.mTargetCell[1] + 1;
                return;
            }
            this.mTargetRank = this.mTargetCell[1];
            if (isDragOverAppsButton(this.mTargetCell)) {
                this.mTargetRank++;
            }
        } else if (((float) centerPoint) < x) {
            this.mTargetRank = this.mTargetCell[0] + 1;
            if (Utilities.sIsRtl || isDragOverAppsButton(this.mTargetCell)) {
                this.mTargetRank--;
            }
        } else {
            this.mTargetRank = this.mTargetCell[0];
            if (Utilities.sIsRtl && !isDragOverAppsButton(this.mTargetCell)) {
                this.mTargetRank++;
            }
        }
    }

    private void commitTempPlacement() {
        CellLayoutChildren clc = this.mContent.getCellLayoutChildren();
        int childCount = clc.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            if (info != null) {
                info.requiresDbUpdate = true;
                info.cellX = lp.cellX;
                info.cellY = lp.cellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        commit(this.mContent);
    }

    private void cancelReorder() {
        if (mReorderAnimSet != null) {
            mReorderAnimSet.cancel();
        }
    }

    boolean isReorderRunning() {
        return mReorderAnimSet != null && mReorderAnimSet.isRunning();
    }

    void makeEmptyCell(int targetCell, boolean animate, boolean commit) {
        makeEmptyCells(targetCell, 1, animate, commit);
    }

    void makeEmptyCells(int targetCell, int emptyCells, boolean animate, boolean commit) {
        if (!this.mContent.isFull()) {
            int cellCount;
            if (this.mHotseat.isVerticalHotseat()) {
                cellCount = this.mContent.getCountY() + emptyCells;
            } else {
                cellCount = this.mContent.getCountX() + emptyCells;
            }
            if (cellCount > this.mContent.getMaxCellCount()) {
                cellCount = this.mContent.getMaxCellCount();
            }
            this.mRestorePosition = false;
            boolean[][] occupied = this.mContent.getOccupied();
            if (this.mHotseat.isVerticalHotseat()) {
                if (this.mContent.getCountY() != cellCount) {
                    this.mContent.setGridSize(1, cellCount);
                } else {
                    return;
                }
            } else if (this.mContent.getCountX() != cellCount) {
                this.mContent.setGridSize(cellCount, 1);
            } else {
                return;
            }
            if (isReorderRunning()) {
                cancelReorder();
            }
            mReorderAnimSet = this.mContent.reorderMakeCells(animate, occupied, targetCell);
            if (mReorderAnimSet != null) {
                mReorderAnimSet.start();
            }
            if (commit) {
                commitTempPlacement();
            }
        }
    }

    void removeEmptyCells(boolean animate, boolean commit) {
        if (this.mContent.hasEmptyCell()) {
            this.mRestorePosition = false;
            boolean[][] occupied = this.mContent.getOccupied();
            if (this.mHotseat.isVerticalHotseat()) {
                this.mContent.setGridSize(1, this.mContent.getCountY() - this.mContent.getEmptyCount(this.mContent.getCountY()));
            } else {
                this.mContent.setGridSize(this.mContent.getCountX() - this.mContent.getEmptyCount(this.mContent.getCountX()), 1);
            }
            if (isReorderRunning()) {
                cancelReorder();
            }
            mReorderAnimSet = this.mContent.reorderRemoveCells(animate, occupied);
            if (mReorderAnimSet != null) {
                mReorderAnimSet.start();
            }
            if (commit) {
                commitTempPlacement();
            }
        }
    }

    public void onExtraObjectDragged(ArrayList<DragObject> extraDragObjects) {
        if (extraDragObjects != null) {
            Iterator it = extraDragObjects.iterator();
            while (it.hasNext()) {
                View sourceView = ((DragObject) it.next()).dragView.getSourceView();
                if (!(sourceView == null || sourceView.getParent() == null)) {
                    ((CellLayout) sourceView.getParent().getParent()).removeView(sourceView);
                }
            }
            removeEmptyCells(true, false);
        }
    }

    public void onExtraObjectDropCompleted(View target, ArrayList<DragObject> arrayList, ArrayList<DragObject> failedDragObjects, int fullCnt) {
        if (failedDragObjects != null && failedDragObjects.size() > 0) {
            this.mHomeController.dropCompletedFromHotseat(failedDragObjects, null, false, fullCnt);
        }
        commitTempPlacement();
    }

    private boolean isDragOverAppsButton(int[] targetCell) {
        View dragOverView = this.mContent.getChildAt(targetCell[0], targetCell[1]);
        if (dragOverView != null && (dragOverView.getTag() instanceof IconInfo) && ((IconInfo) dragOverView.getTag()).isAppsButton) {
            return true;
        }
        return false;
    }

    public int getPageIndexForDragView(ItemInfo item) {
        return this.mHomeController.getWorkspace().getNextPage();
    }

    public int getDragSourceType() {
        return 2;
    }

    void restoreHotseatObjects(ArrayList<DragObject> hotseatObjects) {
        if (hotseatObjects.size() > 1) {
            Collections.sort(hotseatObjects, SCREENID_COMPARATOR);
        }
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList();
        Iterator it = hotseatObjects.iterator();
        while (it.hasNext()) {
            DragObject d = (DragObject) it.next();
            ItemInfo info = d.dragInfo;
            boolean isFromFolder = (info.isContainApps() || this.mHomeController.getHomescreenIconByItemId(info.container) == null) ? false : true;
            if (isFromFolder) {
                View folder = this.mHomeController.getHomescreenIconByItemId(info.container);
                if (folder != null) {
                    FolderInfo folderInfo = (FolderInfo) folder.getTag();
                    this.mFolderController.setReorderTarget(this.mHomeController.getWorkspace().getScreenWithId(folderInfo.screenId));
                    this.mTargetCell[0] = folderInfo.cellX;
                    this.mTargetCell[1] = folderInfo.cellY;
                    addToExistingFolderIfNecessary(this.mTargetCell, d);
                }
            } else {
                DragView dragView = d.dragView;
                View view = dragView.getSourceView();
                if (view != null) {
                    int countMax;
                    if (d.dragView.getParent() != null) {
                        d.deferDragViewCleanupPostAnimation = false;
                        this.mDragLayer.removeView(d.dragView);
                    }
                    ((LayoutParams) view.getLayoutParams()).isLockedToGrid = true;
                    if (LauncherAppState.getInstance().getAppsButtonEnabled()) {
                        countMax = this.mContent.getCountX() - 1;
                    } else {
                        countMax = this.mContent.getCountX();
                    }
                    if (info.cellX > countMax) {
                        info.cellX = countMax;
                    }
                    this.mHomeController.addOrMoveItemInDb(info, info.container, info.screenId, info.cellX, info.cellY, -1);
                    this.mHomeController.addInScreen(view, info.container, info.screenId, info.cellX, info.cellY, info.spanX, info.spanY);
                    if (view.getVisibility() != 0) {
                        view.setVisibility(View.VISIBLE);
                    }
                    view.setAlpha(0.0f);
                    view.setScaleX(0.0f);
                    view.setScaleY(0.0f);
                    ValueAnimator bounceAnimator = this.mHomeController.createNewAppBounceAnimation(view, 1);
                    final DragView dragView2 = dragView;
                    bounceAnimator.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            HotseatDragController.this.mLauncher.getDragMgr().onDeferredEndDrag(dragView2);
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    bounceAnims.add(bounceAnimator);
                } else {
                    return;
                }
            }
        }
        if (bounceAnims.size() > 0) {
            anim.playTogether(bounceAnims);
            anim.start();
        }
    }

    private void restoreExtraDropItems(DragObject d, boolean isFromFolder) {
        ItemInfo objectInfo = d.dragInfo;
        if (d.dragView.getParent() != null) {
            d.deferDragViewCleanupPostAnimation = false;
            this.mDragLayer.removeView(d.dragView);
        }
        Workspace workspace = this.mHomeController.getWorkspace();
        if (isFromFolder) {
            View folder = this.mHomeController.getHomescreenIconByItemId(objectInfo.container);
            if (folder != null) {
                CellLayout original;
                FolderInfo folderInfo = (FolderInfo) folder.getTag();
                if (folderInfo.container == -101) {
                    original = this.mHomeController.getHotseat().getLayout();
                } else {
                    original = workspace.getScreenWithId(folderInfo.screenId);
                }
                this.mFolderController.setReorderTarget(original);
                this.mTargetCell[0] = folderInfo.cellX;
                this.mTargetCell[1] = folderInfo.cellY;
                addToExistingFolderIfNecessary(this.mTargetCell, d);
            }
        } else {
            View objectView = d.dragView.getSourceView();
            if (objectInfo.container == -100) {
                objectView.setVisibility(View.VISIBLE);
                this.mHomeController.addInScreen(objectView, objectInfo.container, objectInfo.screenId, objectInfo.cellX, objectInfo.cellY, objectInfo.spanX, objectInfo.spanY);
            }
        }
        this.mLauncher.getDragMgr().onDeferredEndDrag(d.dragView);
    }

    private void sayDragTalkBack(boolean internal, boolean isMovedLayout, int cellX, int cellY) {
        String description;
        Resources res = this.mLauncher.getResources();
        int pos;
        if (!internal) {
            if (this.mHotseat.isVerticalHotseat()) {
                pos = cellY;
            } else {
                pos = cellX;
            }
            description = res.getString(R.string.tts_hotseat_item_moved_from_external) + " " + res.getString(R.string.tts_hotseat_move_to, new Object[]{Integer.valueOf(pos + 1)});
        } else if (isMovedLayout) {
            description = res.getString(R.string.tts_item_moved) + " " + res.getString(R.string.tts_item_dims_format, new Object[]{Integer.valueOf(cellY + 1), Integer.valueOf(cellX + 1)});
        } else if (!this.mLauncher.isQuickOptionShowing()) {
            if (this.mHotseat.isVerticalHotseat()) {
                pos = cellY;
            } else {
                pos = cellX;
            }
            description = res.getString(R.string.tts_hotseat_move_to, new Object[]{Integer.valueOf(pos + 1)});
        } else {
            return;
        }
        if (description != null) {
            Talk.INSTANCE.say(description);
        }
    }

    private void refreshFolderBadge(ItemInfo info) {
        if (info != null && !info.isContainApps()) {
            View folder = this.mHomeController.getHomescreenIconByItemId(info.container);
            if (folder instanceof FolderIconView) {
                ((FolderIconView) folder).refreshBadge();
            }
        }
    }

    private boolean setReorderTarget() {
        int target = this.mContent.cellToPosition(this.mTargetCell[0], this.mTargetCell[1]);
        if (target == this.mEmptyCellRank) {
            return true;
        }
        if (isDragOverAppsButton(this.mTargetCell)) {
            this.mTargetRank = this.mHotseat.isVerticalHotseat() ? target + 1 : target - 1;
            return false;
        }
        int revertRank;
        int empty = Utilities.sIsRtl ? target : this.mEmptyCellRank;
        if (Utilities.sIsRtl) {
            target = this.mEmptyCellRank;
        }
        int[] regionToCenterPoint = new int[2];
        this.mContent.regionToCenterPoint(this.mTargetCell[0], this.mTargetCell[1], 1, 1, regionToCenterPoint);
        int overViewCenterPoint = this.mContent.cellToPosition(regionToCenterPoint[0], regionToCenterPoint[1]);
        int dragViewCenterPoint = this.mContent.cellToPosition((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1]);
        int targetCell = this.mContent.cellToPosition(this.mTargetCell[0], this.mTargetCell[1]);
        if (!Utilities.sIsRtl || this.mHotseat.isVerticalHotseat()) {
            revertRank = 1;
        } else {
            revertRank = -1;
        }
        if (target > empty) {
            if (dragViewCenterPoint > overViewCenterPoint) {
                this.mTargetRank = targetCell;
                return true;
            }
            this.mTargetRank = targetCell - revertRank;
        } else if (dragViewCenterPoint < overViewCenterPoint) {
            this.mTargetRank = targetCell;
            return true;
        } else {
            this.mTargetRank = targetCell + revertRank;
        }
        if (this.mHotseat.isVerticalHotseat()) {
            this.mTargetCell[1] = this.mTargetRank;
            return true;
        }
        this.mTargetCell[0] = this.mTargetRank;
        return true;
    }

    public int getEmptyCount() {
        return this.mContent.getMaxCellCount() - (this.mHotseat.isVerticalHotseat() ? this.mContent.getCountY() : this.mContent.getCountX());
    }
}
