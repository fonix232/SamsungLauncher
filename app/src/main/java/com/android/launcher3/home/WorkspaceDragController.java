package com.android.launcher3.home;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.Toast;
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
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragScroller;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DragViewHelper;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderIconDropController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.home.LauncherAppWidgetHostView.ResizeResult;
import com.android.launcher3.util.PinnedShortcutUtils;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class WorkspaceDragController implements DropTarget, DragSource, DragScroller, DragState, DragListener {
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    private static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    private static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;
    static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    private static final int WIDGET_DROP_ANIMATION_DELAY = 200;
    private CellInfo mDragInfo;
    private DragLayer mDragLayer;
    private int mDragMode = 0;
    private int mDragOverX = -1;
    private int mDragOverY = -1;
    private CellLayout mDragOverlappingLayout = null;
    private CellLayout mDragTargetLayout = null;
    private float[] mDragViewVisualCenter = new float[2];
    private CellLayout mDropToLayout = null;
    private CellLayout mFirstDropToLayout = null;
    private FolderIconDropController mFolderController;
    private FolderLock mFolderLock;
    private HomeController mHomeController;
    private boolean mInScrollArea = false;
    private Launcher mLauncher;
    private ArrayList<RemoveCandidateItem> mRemoveCandidateItems = new ArrayList();
    private WorkspaceReorderController mReorderController;
    private boolean mRestorePosition = false;
    private int[] mTargetCell = new int[2];
    private Workspace mWorkspace;

    private static class RemoveCandidateItem {
        DragView mDragView;
        ItemInfo mInfo;
        boolean mIsRestoreDeepShortCut;

        RemoveCandidateItem(ItemInfo info, DragView dragView, boolean isRestoreDeepShortCut) {
            this.mInfo = info;
            this.mDragView = dragView;
            this.mIsRestoreDeepShortCut = isRestoreDeepShortCut;
        }
    }

    WorkspaceDragController(Context context, Workspace workspace) {
        this.mLauncher = (Launcher) context;
        this.mWorkspace = workspace;
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    void setup(DragManager dragMgr, DragLayer dragLayer, HomeController homeController) {
        this.mDragLayer = dragLayer;
        this.mHomeController = homeController;
        this.mReorderController = new WorkspaceReorderController(this);
        this.mFolderController = new FolderIconDropController(this.mLauncher, this);
    }

    boolean startDrag(CellInfo cellInfo, boolean isSource, boolean fromEmptyCell) {
        View child = cellInfo.cell;
        this.mDragInfo = cellInfo;
        if (isSource) {
            this.mReorderController.prepareChildForDrag((CellLayout) child.getParent().getParent(), child);
        }
        this.mRestorePosition = !fromEmptyCell;
        return true;
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        if (dragObject == null) {
            return 0;
        }
        boolean app;
        boolean folder;
        boolean widget;
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
        if (info.itemType == 4 || info.itemType == 5) {
            widget = true;
        } else {
            widget = false;
        }
        boolean homeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        int optionFlags = 0;
        if (!widget) {
            optionFlags = 0 | 1;
        }
        if (!widget) {
            optionFlags |= 32;
        }
        if (homeOnlyMode && !widget) {
            optionFlags |= 4096;
        }
        if (homeOnlyMode && !widget) {
            optionFlags |= 8192;
        }
        if (app) {
            optionFlags |= 64;
        }
        if (app || widget) {
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

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        LauncherAppState.getInstance().enableExternalQueue(true);
        if (this.mHomeController.getState() != 2 && this.mHomeController.getState() != 6) {
            return false;
        }
        this.mWorkspace.updateChildrenLayersEnabled(false);
        return true;
    }

    public boolean onDragEnd() {
        LauncherAppState.getInstance().disableAndFlushExternalQueue();
        if (!(this.mDragInfo == null || this.mDragInfo.container == -100)) {
            this.mDragInfo = null;
        }
        if (this.mLauncher.isHomeStage() && this.mHomeController.getState() != 2 && this.mHomeController.getState() != 3 && this.mHomeController.getState() != 6) {
            return false;
        }
        this.mWorkspace.updateChildrenLayersEnabled(false);
        return true;
    }

    public boolean isDropEnabled(boolean isDrop) {
        return true;
    }

    public void onDrop(DragObject d) {
        this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
        if (this.mDropToLayout != null) {
            this.mWorkspace.mapPointFromSelfToChild(this.mDropToLayout, this.mDragViewVisualCenter);
        } else {
            this.mDropToLayout = getCurrentDropLayout();
        }
        this.mFirstDropToLayout = this.mDropToLayout;
        if (this.mDragInfo == null) {
            onDropExternal(d);
        } else {
            onDropInternal(d);
        }
        this.mWorkspace.forcelyAnimateReturnPages();
    }

    private void onDropInternal(DragObject d) {
        CellLayout parent;
        Runnable anonymousClass1 = new Runnable() {
            public void run() {
                WorkspaceDragController.this.mHomeController.exitDragStateDelayed();
            }
        };
        boolean resizeOnDrop = false;
        boolean isFolderDrop = d.dragInfo instanceof FolderInfo;
        boolean cancelDropFolder = false;
        View cell = this.mDragInfo.cell;
        ItemInfo cellInfo = (ItemInfo) cell.getTag();
        CellLayout originalLayout = this.mDragInfo.layout;
        boolean canEnterResizeMode = false;
        LauncherAppWidgetHostView hostView = null;
        if (!(this.mDropToLayout == null || d.cancelled)) {
            ItemInfo info;
            Resources res;
            boolean hasMovedFromHotseat = this.mDragInfo.container != -100;
            if (hasMovedFromHotseat) {
                if (isFolderDrop) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_DELETE, "Folder", -1, false);
                } else {
                    info = d.dragInfo;
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_DELETE, info.getIntent().getComponent() != null ? info.getIntent().getComponent().getPackageName() : null, -1, false);
                }
                res = this.mLauncher.getResources();
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_QuickOptions), res.getString(R.string.event_RemoveFromDock));
            }
            this.mTargetCell = this.mDropToLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], this.mDragInfo.spanX, this.mDragInfo.spanY, this.mTargetCell);
            d.postAnimationRunnable = anonymousClass1;
            if (hasMovedFromHotseat) {
                parent = null;
            } else {
                parent = originalLayout;
            }
            if (!this.mInScrollArea && this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, cell, parent, d)) {
                if (isFolderDrop) {
                    cancelDropFolder = true;
                } else {
                    return;
                }
            }
            if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, d)) {
                if (isFolderDrop) {
                    cancelDropFolder = true;
                } else {
                    if (!(hasMovedFromHotseat || originalLayout == null)) {
                        originalLayout.removeView(cell);
                    }
                    if (hasMovedFromHotseat || this.mDragInfo.screenId == ((long) this.mWorkspace.getDefaultPage())) {
                        this.mHomeController.notifyCapture(false);
                        this.mHomeController.updateNotificationHelp(true);
                        return;
                    }
                    return;
                }
            }
            info = (ItemInfo) d.dragInfo;
            SpanInfo spanInfo = SpanInfo.getSpanInfo(info);
            int[] resultSpan = new int[2];
            if (!cancelDropFolder) {
                this.mTargetCell = this.mReorderController.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanInfo, cell, this.mTargetCell, resultSpan, 2);
            } else if (findEmptyCellForExtraDragObject(this.mTargetCell, d.cancelled, d.cancelDropFolder, false, null)) {
                resultSpan[0] = 1;
                resultSpan[1] = 1;
            } else {
                ArrayList<DragObject> extraDragList = new ArrayList();
                if (d.dragView != null) {
                    d.dragView.setSourceView(cell);
                }
                DragObject source = new DragObject();
                source.copyFrom(d);
                extraDragList.add(source);
                if (d.extraDragInfoList != null) {
                    extraDragList.addAll(d.extraDragInfoList);
                }
                if (originalLayout != null) {
                    originalLayout.removeView(cell);
                }
                if (!(d.dragView == null || d.dragView.getParent() == null)) {
                    d.deferDragViewCleanupPostAnimation = false;
                    this.mDragLayer.removeView(d.dragView);
                }
                this.mHomeController.updateCountBadge(cell, false);
                anonymousClass1.run();
                onDropExtraObjects(extraDragList, null, this.mRestorePosition, false, true, false, extraDragList.size());
                return;
            }
            boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0 && resultSpan[0] > 0 && resultSpan[1] > 0;
            if (foundCell) {
                long screenId;
                if ((cell instanceof AppWidgetHostView) && !(resultSpan[0] == info.spanX && resultSpan[1] == info.spanY)) {
                    resizeOnDrop = true;
                    info.spanX = resultSpan[0];
                    info.spanY = resultSpan[1];
                    AppWidgetResizeFrame.updateWidgetSizeRanges(this.mLauncher, (AppWidgetHostView) cell, resultSpan[0], resultSpan[1]);
                }
                boolean hasMovedLayout = originalLayout != this.mDropToLayout || cancelDropFolder;
                if (this.mTargetCell[0] < 0) {
                    screenId = this.mDragInfo.screenId;
                } else {
                    screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
                }
                if (this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage()) != screenId) {
                    this.mWorkspace.snapToPage(this.mWorkspace.getPageIndexForScreenId(screenId));
                }
                if (hasMovedLayout) {
                    if (hasMovedFromHotseat || originalLayout == null) {
                        Log.d(DropTarget.TAG, "mDragInfo.cell has null parent");
                    } else {
                        originalLayout.removeView(cell);
                    }
                    this.mHomeController.addInScreen(cell, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], cellInfo.spanX, cellInfo.spanY);
                    if (this.mLauncher.getMultiSelectManager().isMultiSelectMode()) {
                        res = this.mLauncher.getResources();
                        SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_SM_MoveItem));
                    } else {
                        SALogging.getInstance().insertMoveToPageLog(d.dragInfo, true);
                    }
                }
                LayoutParams lp = (LayoutParams) cell.getLayoutParams();
                int i = this.mTargetCell[0];
                lp.tmpCellX = i;
                lp.cellX = i;
                i = this.mTargetCell[1];
                lp.tmpCellY = i;
                lp.cellY = i;
                lp.cellHSpan = info.spanX;
                lp.cellVSpan = info.spanY;
                lp.isLockedToGrid = true;
                if (!(hasMovedLayout || this.mDropToLayout == null)) {
                    this.mDropToLayout.markCellsAsOccupiedForView(cell);
                }
                if (cell instanceof LauncherAppWidgetHostView) {
                    hostView = (LauncherAppWidgetHostView) cell;
                    if (this.mHomeController.canEnterResizeMode(hostView, this.mDropToLayout)) {
                        canEnterResizeMode = true;
                    }
                }
                this.mHomeController.modifyItemInDb(cellInfo, -100, screenId, lp.cellX, lp.cellY, info.spanX, info.spanY);
                boolean z = hasMovedLayout && !hasMovedFromHotseat;
                sayDragTalkBack(true, z, lp.cellX, lp.cellY);
                if (screenId == ((long) this.mWorkspace.getDefaultPage()) && !(!hasMovedLayout && this.mDragInfo.cellX == lp.cellX && this.mDragInfo.cellY == lp.cellY)) {
                    this.mHomeController.notifyCapture(false);
                    this.mHomeController.updateNotificationHelp(true);
                }
                if (hasMovedLayout && this.mDragInfo.screenId == ((long) this.mWorkspace.getDefaultPage())) {
                    this.mHomeController.updateNotificationHelp(true);
                }
            } else {
                ArrayList<DragObject> restoredHotseatObjects = new ArrayList();
                if (cellInfo.container == -100) {
                    cell.setVisibility(View.VISIBLE);
                    if (originalLayout != null) {
                        originalLayout.markCellsAsOccupiedForView(cell);
                    }
                } else {
                    d.dragView.setSourceView(cell);
                    restoredHotseatObjects.add(d);
                }
                this.mHomeController.updateCountBadge(cell, false);
                if (d.extraDragInfoList != null) {
                    Iterator it = d.extraDragInfoList.iterator();
                    while (it.hasNext()) {
                        DragObject object = (DragObject) it.next();
                        if (((ItemInfo) object.dragInfo).container == -101) {
                            restoredHotseatObjects.add(object);
                        } else {
                            restoreExtraDropItems(object, false);
                        }
                    }
                }
                if (restoredHotseatObjects.size() > 0) {
                    this.mHomeController.getHotseat().getDragController().restoreHotseatObjects(restoredHotseatObjects);
                }
                if (d.dragView.getParent() != null) {
                    d.deferDragViewCleanupPostAnimation = false;
                    this.mDragLayer.removeView(d.dragView);
                }
                anonymousClass1.run();
                this.mHomeController.showNoSpacePage(false);
                return;
            }
        }
        parent = (CellLayout) cell.getParent().getParent();
        if (canEnterResizeMode) {
            final LauncherAppWidgetHostView launcherAppWidgetHostView = hostView;
            anonymousClass1 = new Runnable() {
                public void run() {
                    WorkspaceDragController.this.mHomeController.enterResizeStateDelay(launcherAppWidgetHostView, WorkspaceDragController.this.mDropToLayout, 50);
                }
            };
        } else {
            Runnable onCompleteRunnable = anonymousClass1;
        }
        if (d.dragView.hasDrawn()) {
            boolean isWidget = cellInfo.itemType == 4 || cellInfo.itemType == 5;
            if (isWidget) {
                animateWidgetDrop(cellInfo, parent, d.dragView, onCompleteRunnable, resizeOnDrop ? 2 : 0, cell, false);
            } else {
                this.mDragLayer.animateViewIntoPosition(d.dragView, cell, 300, onCompleteRunnable, this.mWorkspace);
            }
        } else {
            d.deferDragViewCleanupPostAnimation = false;
            cell.setVisibility(View.VISIBLE);
            this.mHomeController.updateCountBadge(cell, false);
            onCompleteRunnable.run();
        }
        parent.onDropChild(cell);
        if (!cancelDropFolder && d.extraDragInfoList != null) {
            onDropExtraObjects(d.extraDragInfoList, null, this.mRestorePosition, false, false, false, d.extraDragInfoList.size() + 1);
        }
    }

    private void onDropExternal(DragObject d) {
        ItemInfo info = d.dragInfo;
        if (!(info instanceof PendingAddItemInfo) || ((PendingAddItemInfo) info).getProviderInfo() == null) {
            onDropExternalFromOther(d);
        } else {
            onDropExternalFromWidget(d);
        }
        if (this.mWorkspace.getIdForScreen(this.mDropToLayout) == ((long) this.mWorkspace.getDefaultPage())) {
            this.mHomeController.notifyCapture(false);
            this.mHomeController.updateNotificationHelp(true);
        }
    }

    private void onDropExternalFromWidget(DragObject d) {
        Runnable anonymousClass3;
        final ItemInfo info = d.dragInfo;
        boolean findNearestVacantCell = true;
        boolean willAddToFolder = false;
        if (info.itemType == 1) {
            this.mTargetCell = this.mDropToLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], info.spanX, info.spanY, this.mTargetCell);
            float distance = this.mDropToLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            if (this.mFolderController.willCreateUserFolder((ItemInfo) d.dragInfo, null, this.mTargetCell, distance, true) || this.mFolderController.willAddToExistingUserFolder((ItemInfo) d.dragInfo, this.mTargetCell, distance)) {
                findNearestVacantCell = false;
                willAddToFolder = true;
            }
        } else {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ITEM_ARRANGMENT, "Home_Background_Longpress", -1, false);
        }
        boolean isWidget = info.itemType == 4 || info.itemType == 5;
        final View finalView = isWidget ? ((PendingAddWidgetInfo) info).boundWidget : null;
        boolean updateWidgetSize = false;
        if (findNearestVacantCell) {
            int[] resultSpan = new int[2];
            this.mTargetCell = this.mReorderController.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], SpanInfo.getSpanInfo(info), null, this.mTargetCell, resultSpan, 3);
            if (!(resultSpan[0] == info.spanX && resultSpan[1] == info.spanY)) {
                updateWidgetSize = true;
            }
            boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0 && resultSpan[0] > 0 && resultSpan[1] > 0;
            if (foundCell) {
                info.spanX = resultSpan[0];
                info.spanY = resultSpan[1];
            } else {
                if (d.dragView.getParent() != null) {
                    d.deferDragViewCleanupPostAnimation = false;
                    this.mDragLayer.removeView(d.dragView);
                }
                this.mHomeController.exitDragStateDelayed();
                this.mHomeController.showNoSpacePage(false);
                return;
            }
        }
        long screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
        int delay = 0;
        if (this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage()) != screenId) {
            final long j = screenId;
            anonymousClass3 = new Runnable() {
                public void run() {
                    if (WorkspaceDragController.this.mWorkspace != null) {
                        WorkspaceDragController.this.mWorkspace.snapToPage(WorkspaceDragController.this.mWorkspace.getPageIndexForScreenId(j));
                    }
                }
            };
            if (Utilities.sIsRtl) {
                this.mWorkspace.post(anonymousClass3);
            } else {
                anonymousClass3.run();
            }
            delay = 200;
        }
        final PendingAddItemInfo pendingAddItemInfo = info;
        final long j2 = screenId;
        anonymousClass3 = new Runnable() {
            public void run() {
                WorkspaceDragController.this.mHomeController.addPendingItem(pendingAddItemInfo, -100, j2, WorkspaceDragController.this.mTargetCell, pendingAddItemInfo.spanX, pendingAddItemInfo.spanY);
            }
        };
        if (finalView != null && updateWidgetSize) {
            AppWidgetResizeFrame.updateWidgetSizeRanges(this.mLauncher, (AppWidgetHostView) finalView, info.spanX, info.spanY);
        }
        if (isWidget && ((PendingAddWidgetInfo) info).info != null && ((PendingAddWidgetInfo) info).info.configure != null) {
            final DragView dragView = d.dragView;
            new Handler().postDelayed(new Runnable(1) {
                public void run() {
                    WorkspaceDragController.this.animateWidgetDrop(info, WorkspaceDragController.this.mDropToLayout, dragView, anonymousClass3, 1, finalView, true);
                }
            }, (long) delay);
        } else if (isWidget) {
            animateWidgetDrop(info, this.mDropToLayout, d.dragView, anonymousClass3, 0, finalView, true);
        } else if (willAddToFolder) {
            d.deferDragViewCleanupPostAnimation = false;
            anonymousClass3.run();
        } else {
            animateWidgetDrop(info, this.mDropToLayout, d.dragView, anonymousClass3, 1, null, true);
        }
        sayDragTalkBack(false, false, this.mTargetCell[0], this.mTargetCell[1]);
    }

    private void onDropExternalFromOther(DragObject d) {
        ItemInfo info = d.dragInfo;
        boolean isAcceptItem = false;
        Runnable anonymousClass6 = new Runnable() {
            public void run() {
                WorkspaceDragController.this.mHomeController.exitDragStateDelayed();
            }
        };
        if (info instanceof PendingAddPinShortcutInfo) {
            LauncherAppsCompat.acceptPinItemRequest(this.mLauncher, ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat(), 0);
            isAcceptItem = true;
            ItemInfo shortcutInfo = ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().createShortcutInfo();
            if (shortcutInfo != null) {
                info = shortcutInfo;
            } else {
                anonymousClass6.run();
                return;
            }
        }
        if (((info instanceof IconInfo) || (info instanceof FolderInfo)) && info.isAppOrShortcutType()) {
            ItemInfo localInfo;
            View view;
            boolean isFolderDrop = d.dragInfo instanceof FolderInfo;
            boolean cancelDropFolder = false;
            boolean isFromHomeFolder = (info.isContainApps() || this.mHomeController.getHomescreenIconByItemId(info.container) == null) ? false : true;
            boolean isFromApps = info.isContainApps() || !isFromHomeFolder;
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
                    view = this.mHomeController.getBindController().createShortcut(this.mDropToLayout, (IconInfo) localInfo);
                    break;
                case 2:
                    localInfo = ((FolderInfo) info).makeCloneInfo();
                    if (localInfo != null) {
                        ((FolderInfo) localInfo).setAlphabeticalOrder(false, true, this.mLauncher);
                    }
                    view = FolderIconView.fromXml(this.mLauncher, this.mDropToLayout, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 0);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
            if (info != localInfo) {
                info = localInfo;
            }
            if (info != null) {
                refreshFolderBadge(info);
                this.mTargetCell = this.mDropToLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], info.spanX, info.spanY, this.mTargetCell);
                if (!this.mFolderController.acceptDrop(this.mDragViewVisualCenter, this.mTargetCell, d, this.mDragInfo)) {
                    this.mTargetCell = this.mReorderController.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], new SpanInfo(1, 1, 1, 1, null), null, this.mTargetCell, null, 3);
                    boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0;
                    if (!foundCell) {
                        anonymousClass6.run();
                        if (isFromHomeFolder) {
                            restoreExtraDropItems(d, true);
                        }
                        if (d.dragView.getParent() != null) {
                            d.deferDragViewCleanupPostAnimation = false;
                            this.mDragLayer.removeView(d.dragView);
                        }
                        this.mHomeController.showNoSpacePage(isFromApps);
                        if ((info.itemType == 6 || isAcceptItem) && (d.dragInfo instanceof PendingAddPinShortcutInfo)) {
                            this.mLauncher.startAddItemActivity(((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat(), true);
                            return;
                        }
                        return;
                    }
                }
                d.postAnimationRunnable = anonymousClass6;
                if (this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, view, null, d)) {
                    if (isFolderDrop || Utilities.hasFolderItem(d.extraDragInfoList)) {
                        cancelDropFolder = true;
                    } else {
                        PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
                        return;
                    }
                }
                if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, d)) {
                    if (isFolderDrop || Utilities.hasFolderItem(d.extraDragInfoList)) {
                        cancelDropFolder = true;
                    } else {
                        PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
                        return;
                    }
                }
                if (cancelDropFolder) {
                    if (isFolderDrop) {
                        if (!findEmptyCellForExtraDragObject(this.mTargetCell, d.cancelled, d.cancelDropFolder, false, false)) {
                            ArrayList<DragObject> extraDragList = new ArrayList();
                            extraDragList.add(d);
                            if (d.extraDragInfoList != null) {
                                extraDragList.addAll(d.extraDragInfoList);
                            }
                            if (!(d.dragView == null || d.dragView.getParent() == null)) {
                                d.deferDragViewCleanupPostAnimation = false;
                                this.mDragLayer.removeView(d.dragView);
                            }
                            anonymousClass6.run();
                            onDropExtraObjects(extraDragList, null, false, false, true, true, extraDragList.size());
                            return;
                        }
                    } else if (d.extraDragInfoList != null) {
                        if (!(d.dragView == null || d.dragView.getParent() == null)) {
                            d.deferDragViewCleanupPostAnimation = false;
                            this.mDragLayer.removeView(d.dragView);
                        }
                        anonymousClass6.run();
                        onDropExtraObjects(d.extraDragInfoList, null, false, false, true, true, d.extraDragInfoList.size() + 1);
                        return;
                    } else {
                        return;
                    }
                }
                if (!info.isContainApps() && d.extraDragInfoList == null) {
                    View folder = this.mHomeController.getHomescreenIconByItemId(info.container);
                    if (folder instanceof FolderIconView) {
                        ((FolderIconView) folder).getFolderView().updateDeletedFolder();
                    }
                }
                long screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
                if (this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage()) != screenId) {
                    this.mWorkspace.snapToPage(this.mWorkspace.getPageIndexForScreenId(screenId));
                }
                this.mHomeController.addOrMoveItemInDb(info, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], -1);
                if (info instanceof FolderInfo) {
                    this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
                    if (this.mFolderLock != null && ((FolderInfo) info).isLocked()) {
                        this.mFolderLock.addLockedRecords((FolderInfo) info);
                    }
                }
                this.mHomeController.addInScreen(view, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                sayDragTalkBack(false, false, this.mTargetCell[0], this.mTargetCell[1]);
                this.mDropToLayout.onDropChild(view);
                this.mDropToLayout.getCellLayoutChildren().measureChild(view);
                if (d.dragView == null || this.mLauncher.isAppsStage()) {
                    d.deferDragViewCleanupPostAnimation = false;
                } else {
                    this.mDragLayer.animateViewIntoPosition(d.dragView, view, 300, anonymousClass6, this.mWorkspace);
                }
                PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
                if (isAcceptItem) {
                    PinnedShortcutUtils.unpinShortcutIfAppTarget(new ShortcutInfoCompat(((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat().getShortcutInfo()), this.mLauncher);
                }
                if (d.extraDragInfoList != null) {
                    onDropExtraObjects(d.extraDragInfoList, null, false, false, cancelDropFolder, true, d.extraDragInfoList.size() + 1);
                }
            }
        }
    }

    private void onDropExtraObjects(ArrayList<DragObject> extraDragObjects, Runnable postRunnable, boolean restored, boolean canceled, boolean cancelDropFolder, boolean fromOther, int fullCnt) {
        onDropExtraObjects(extraDragObjects, postRunnable, restored, canceled, cancelDropFolder, fromOther, fullCnt, false);
    }

    private void onDropExtraObjects(ArrayList<DragObject> extraDragObjects, Runnable postRunnable, boolean restored, boolean canceled, boolean cancelDropFolder, boolean fromOther, int fullCnt, boolean isTargetHotseat) {
        ArrayList<DragObject> restoredHotseatObjects = new ArrayList();
        ArrayList<DragObject> restoreExtraDropItems = new ArrayList();
        int remainCnt = extraDragObjects.size();
        boolean restoreDeepShortCut = false;
        this.mRemoveCandidateItems.clear();
        Iterator it = extraDragObjects.iterator();
        while (it.hasNext()) {
            DragObject d = (DragObject) it.next();
            if (!cancelDropFolder || (d.dragInfo instanceof FolderInfo)) {
                restored |= d.restored;
                ItemInfo info = d.dragInfo;
                if (restored && info.container == -101) {
                    restoredHotseatObjects.add(d);
                } else {
                    View view;
                    int remainCnt2;
                    boolean isItemInFolder = this.mHomeController.isItemInFolder(info);
                    View folder = null;
                    if (isItemInFolder) {
                        folder = this.mHomeController.getHomescreenIconByItemId(info.container);
                        if (!restored) {
                            restoreDeepShortCut = info.itemType == 6;
                            this.mRemoveCandidateItems.add(new RemoveCandidateItem(info, d.dragView, restoreDeepShortCut));
                        }
                    }
                    ItemInfo localInfo = null;
                    if (equals(d.dragSource) || (d.dragSource instanceof HotseatDragController)) {
                        view = d.dragView.getSourceView();
                    } else {
                        switch (info.itemType) {
                            case 0:
                            case 1:
                            case 6:
                            case 7:
                                localInfo = ((IconInfo) info).makeCloneInfo();
                                view = this.mHomeController.getBindController().createShortcut(this.mDropToLayout, (IconInfo) localInfo);
                                break;
                            case 2:
                                localInfo = ((FolderInfo) info).makeCloneInfo();
                                view = FolderIconView.fromXml(this.mLauncher, this.mDropToLayout, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 0);
                                break;
                            default:
                                throw new IllegalStateException("Unknown item type: " + info.itemType);
                        }
                    }
                    if (!(localInfo == null || info == localInfo)) {
                        info = localInfo;
                    }
                    if (restored) {
                        this.mTargetCell[0] = info.cellX;
                        this.mTargetCell[1] = info.cellY;
                        remainCnt2 = remainCnt;
                    } else {
                        boolean foundCell;
                        if (canceled || this.mWorkspace.getIdForScreen(this.mDropToLayout) == this.mWorkspace.getIdForScreen(this.mFirstDropToLayout)) {
                            if (findEmptyCellForExtraDragObject(this.mTargetCell, canceled, d.cancelDropFolder, false, false)) {
                                foundCell = true;
                                if (foundCell) {
                                    if (info.container == -101) {
                                        restoredHotseatObjects.add(d);
                                    }
                                    restoreExtraDropItems.add(d);
                                } else {
                                    remainCnt2 = remainCnt - 1;
                                }
                            }
                        }
                        foundCell = false;
                        if (foundCell) {
                            remainCnt2 = remainCnt - 1;
                        } else {
                            if (info.container == -101) {
                                restoredHotseatObjects.add(d);
                            }
                            restoreExtraDropItems.add(d);
                        }
                    }
                    if (!restored || !isItemInFolder) {
                        long screenId;
                        long container = restored ? info.container : -100;
                        if (restored) {
                            screenId = info.screenId;
                        } else {
                            screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
                        }
                        this.mHomeController.addOrMoveItemInDb(info, container, screenId, this.mTargetCell[0], this.mTargetCell[1], -1, restoreDeepShortCut);
                        if (fromOther && (info instanceof FolderInfo)) {
                            this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
                        }
                        this.mHomeController.addInScreen(view, container, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                        if (this.mDropToLayout != null) {
                            this.mDropToLayout.onDropChild(view);
                            this.mDropToLayout.getCellLayoutChildren().measureChild(view);
                        }
                        d.cancelled = false;
                        if (!(d.dragView == null || this.mLauncher.isAppsStage())) {
                            this.mDragLayer.animateViewIntoPosition(d.dragView, view, 300, postRunnable, this.mWorkspace);
                        }
                    } else if (folder != null) {
                        FolderInfo folderInfo = (FolderInfo) folder.getTag();
                        this.mTargetCell[0] = folderInfo.cellX;
                        this.mTargetCell[1] = folderInfo.cellY;
                        addToExistingFolderIfNecessary(this.mTargetCell, d);
                    }
                    remainCnt = remainCnt2;
                }
            } else {
                remainCnt--;
            }
        }
        if (restoredHotseatObjects.size() > 0) {
            this.mHomeController.getHotseat().getDragController().restoreHotseatObjects(restoredHotseatObjects);
        }
        if (restoreExtraDropItems.size() > 0) {
            final ArrayList<DragObject> arrayList = restoreExtraDropItems;
            Runnable anonymousClass7 = new Runnable() {
                public void run() {
                    if (!(WorkspaceDragController.this.mRemoveCandidateItems == null || WorkspaceDragController.this.mRemoveCandidateItems.isEmpty())) {
                        Iterator it = WorkspaceDragController.this.mRemoveCandidateItems.iterator();
                        while (it.hasNext()) {
                            RemoveCandidateItem removeItem = (RemoveCandidateItem) it.next();
                            WorkspaceDragController.this.mHomeController.removeHomeOrFolderItem(removeItem.mInfo, removeItem.mDragView, removeItem.mIsRestoreDeepShortCut);
                        }
                        WorkspaceDragController.this.mRemoveCandidateItems.clear();
                    }
                    WorkspaceDragController.this.addItemOnLastItem(arrayList, true);
                    WorkspaceDragController.this.setCurrentDropLayout(null);
                }
            };
            arrayList = restoreExtraDropItems;
            final boolean z = fromOther;
            AddItemOnLastPageDialog.createAndShow(this.mLauncher.getFragmentManager(), anonymousClass7, new Runnable() {
                public void run() {
                    WorkspaceDragController.this.restoreItems(arrayList, z);
                    if (WorkspaceDragController.this.mRemoveCandidateItems != null && WorkspaceDragController.this.mRemoveCandidateItems.size() == 1) {
                        WorkspaceDragController.this.updateDeleteFolder(z, ((RemoveCandidateItem) WorkspaceDragController.this.mRemoveCandidateItems.get(0)).mInfo, true);
                        WorkspaceDragController.this.mRemoveCandidateItems.clear();
                    }
                }
            }, remainCnt, fullCnt, isTargetHotseat);
        } else if (extraDragObjects.size() > 0) {
            updateDeleteFolder(fromOther, (ItemInfo) ((DragObject) extraDragObjects.get(0)).dragInfo, false);
        }
    }

    private void updateDeleteFolder(boolean fromOther, ItemInfo info, boolean isForced) {
        if (fromOther && !info.isContainApps()) {
            View folder = this.mHomeController.getHomescreenIconByItemId(info.container);
            if (folder instanceof FolderIconView) {
                ((FolderIconView) folder).getFolderView().updateDeletedFolder(isForced);
            }
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

    public void onDragEnter(DragObject dragObject, boolean dropTargetChanged) {
        this.mFolderController.onDragEnter();
        this.mFolderController.setMaxDistance(this.mLauncher.getDeviceProfile().homeGrid.getIconSize());
        this.mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
        this.mHomeController.showCancelDropTarget();
        if (dragObject.dragSource instanceof HotseatDragController) {
            ((HotseatDragController) dragObject.dragSource).removeEmptyCells(true, true);
        }
    }

    public void onDragOver(DragObject d) {
        if (!this.mInScrollArea) {
            ItemInfo info = d.dragInfo;
            if (info == null) {
                Log.d(DropTarget.TAG, "DragObject has null info");
                return;
            }
            DragView dragView = d.dragView;
            if (dragView != null) {
                this.mWorkspace.dragPullingPages((dragView.getTranslationX() + ((float) dragView.getRegistrationX())) - dragView.getOffsetX());
            }
            if (info.spanX < 0 || info.spanY < 0) {
                throw new RuntimeException("Improper spans found");
            }
            this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
            View child = this.mDragInfo == null ? null : this.mDragInfo.cell;
            CellLayout layout = getCurrentDropLayout();
            if (layout != this.mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
            if (this.mDragTargetLayout != null) {
                this.mWorkspace.mapPointFromSelfToChild(this.mDragTargetLayout, this.mDragViewVisualCenter);
                SpanInfo spanInfo = SpanInfo.getSpanInfo(info);
                this.mTargetCell = this.mDragTargetLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanInfo.minSpanX, spanInfo.minSpanY, this.mTargetCell);
                if (!(this.mTargetCell[0] == info.cellX && this.mTargetCell[1] == info.cellY)) {
                    this.mRestorePosition = false;
                }
                setCurrentDropOverCell(this.mTargetCell[0], this.mTargetCell[1]);
                this.mFolderController.onDragOver(this.mDragViewVisualCenter, this.mTargetCell, d, this.mDragInfo, this.mDragMode);
                this.mReorderController.startReorder(this.mDragViewVisualCenter, this.mTargetCell, spanInfo, d, child, this.mDragMode);
                if (this.mDragMode == 1 || this.mDragMode == 2) {
                    this.mReorderController.revertTempState();
                }
            }
        }
    }

    public void onDragExit(DragObject dragObject, boolean dropTargetChanged) {
        if (this.mDragInfo != null && dragObject.cancelled) {
            this.mHomeController.updateCountBadge(this.mDragInfo.cell, false);
        }
        if (dropTargetChanged) {
            this.mRestorePosition = false;
        }
        if (!dropTargetChanged && dragObject.cancelled && this.mHomeController.getState() == 2) {
            this.mHomeController.exitDragStateDelayed();
            if (!(this.mDragInfo == null || this.mDragInfo.layout == null)) {
                this.mDragInfo.layout.markCellsAsOccupiedForView(this.mDragInfo.cell);
            }
        }
        if (!this.mInScrollArea) {
            this.mDropToLayout = this.mDragTargetLayout;
        } else if (this.mWorkspace.isPageMoving()) {
            this.mDropToLayout = (CellLayout) this.mWorkspace.getPageAt(this.mWorkspace.getNextPage());
        } else {
            this.mDropToLayout = this.mDragOverlappingLayout;
        }
        if (this.mDropToLayout == null) {
            this.mDropToLayout = getCurrentDropLayout();
        }
        if (this.mDragTargetLayout != this.mDropToLayout) {
            this.mReorderController.setReorderTarget(this.mDropToLayout);
            this.mFolderController.setReorderTarget(this.mDropToLayout);
        }
        this.mFolderController.onDragExit(this.mDragMode);
        onResetScrollArea();
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);
    }

    public void onFlingToMove(DragObject d) {
        ItemInfo localInfo;
        View view;
        ItemInfo info;
        ItemInfo info2 = d.dragInfo;
        switch (info2.itemType) {
            case 0:
            case 1:
                localInfo = ((IconInfo) info2).makeCloneInfo();
                view = this.mHomeController.getBindController().createShortcut(this.mDropToLayout, (IconInfo) localInfo);
                break;
            case 2:
                localInfo = ((FolderInfo) info2).makeCloneInfo();
                view = FolderIconView.fromXml(this.mLauncher, this.mDropToLayout, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 0);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info2.itemType);
        }
        if (info2 != localInfo) {
            info = localInfo;
        } else {
            info = info2;
        }
        findEmptySpace(d);
        long screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
        if (this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage()) != screenId) {
            this.mWorkspace.snapToPage(this.mWorkspace.getPageIndexForScreenId(screenId));
        }
        this.mHomeController.addOrMoveItemInDb(info, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], -1);
        if (info instanceof FolderInfo) {
            this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
            if (this.mFolderLock != null && ((FolderInfo) info).isLocked()) {
                this.mFolderLock.addLockedRecords((FolderInfo) info);
            }
        }
        this.mHomeController.addInScreen(view, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
        this.mDropToLayout.getCellLayoutChildren().measureChild(view);
        if (d.extraDragInfoList != null) {
            addItemOnLastItem(d.extraDragInfoList, false);
        }
    }

    public boolean acceptDrop(DragObject d) {
        if (this.mDragInfo == null && (d.dragInfo instanceof PendingAddItemInfo) && isExistSingleInstanceAppWidget(d.dragInfo)) {
            return false;
        }
        if (this.mWorkspace.getIdForScreen(this.mDropToLayout) == -201) {
            this.mWorkspace.commitExtraEmptyScreen();
        }
        return true;
    }

    public View getTargetView() {
        return this.mWorkspace;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        this.mDragLayer.getDescendantRectRelativeToSelf(this.mWorkspace, outRect);
    }

    public int getLeft() {
        return this.mWorkspace.getLeft();
    }

    public int getTop() {
        return this.mWorkspace.getTop();
    }

    public int getOutlineColor() {
        return this.mLauncher.getOutlineColor();
    }

    public Stage getController() {
        return this.mHomeController;
    }

    public int getIntrinsicIconSize() {
        return this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (this.mDragInfo != null) {
            if (!success) {
                CellLayout cellLayout = this.mDragInfo.layout;
                if (cellLayout != null) {
                    cellLayout.onDropChild(this.mDragInfo.cell);
                } else {
                    Log.d(DropTarget.TAG, "Invalid state: cellLayout == null");
                }
                Runnable exitDragStateRunnable = new Runnable() {
                    public void run() {
                        WorkspaceDragController.this.mHomeController.exitDragStateDelayed();
                    }
                };
                if (!d.cancelled && (d.dragInfo instanceof LauncherAppWidgetInfo) && this.mDragInfo.cell != null && (this.mDragInfo.cell.getTag() instanceof ItemInfo)) {
                    ItemInfo info = (ItemInfo) this.mDragInfo.cell.getTag();
                    CellLayout parent = (CellLayout) this.mDragInfo.cell.getParent().getParent();
                    this.mTargetCell[0] = info.cellX;
                    this.mTargetCell[1] = info.cellY;
                    parent.markCellsAsOccupiedForView(this.mDragInfo.cell);
                    animateWidgetDrop(info, parent, d.dragView, exitDragStateRunnable, 0, this.mDragInfo.cell, false);
                }
                if (d.cancelled) {
                    exitDragStateRunnable.run();
                }
                d.deferDragViewCleanupPostAnimation = false;
            } else if ((target instanceof FolderView) && this.mDragInfo.layout != null) {
                this.mDragInfo.layout.removeView(this.mDragInfo.cell);
            }
            if (!((success && !d.cancelled) || this.mDragInfo.cell == null || d.cancelDropFolder)) {
                this.mDragInfo.cell.setVisibility(View.VISIBLE);
            }
            if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing()) {
                boolean canEnterResizeMode = false;
                if (this.mDragInfo.cell instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView hostView = this.mDragInfo.cell;
                    if (this.mDropToLayout != null && this.mHomeController.canEnterResizeMode(hostView, this.mDropToLayout, false)) {
                        canEnterResizeMode = true;
                    }
                }
                if (!canEnterResizeMode) {
                    this.mLauncher.getQuickOptionManager().startBounceAnimation();
                }
            }
            this.mDragInfo = null;
        }
    }

    public int getScrollZone() {
        return this.mLauncher.getResources().getDimensionPixelSize(R.dimen.home_scroll_zone);
    }

    public void scrollLeft() {
        this.mRestorePosition = false;
        if (this.mWorkspace.canDragScroll()) {
            if (!this.mHomeController.isSwitchingState()) {
                this.mWorkspace.scrollLeft();
            }
            FolderView openFolder = this.mLauncher.getOpenFolderView();
            if (openFolder != null) {
                openFolder.completeDragExit();
            }
        }
    }

    public void scrollRight() {
        this.mRestorePosition = false;
        if (this.mWorkspace.canDragScroll()) {
            if (!this.mHomeController.isSwitchingState()) {
                this.mWorkspace.scrollRight();
            }
            FolderView openFolder = this.mLauncher.getOpenFolderView();
            if (openFolder != null) {
                openFolder.completeDragExit();
            }
        }
    }

    public boolean onEnterScrollArea(int x, int y, int direction) {
        boolean result = false;
        if (!(this.mHomeController.isSwitchingState() || this.mLauncher.getOpenFolderView() != null || (this.mWorkspace.getCurrentPage() == 0 && direction == 0))) {
            int page = this.mWorkspace.getNextPage() + (direction == 0 ? -1 : 1);
            setCurrentDropLayout(null);
            if (page >= 0 && page < this.mWorkspace.getChildCount()) {
                if (this.mWorkspace.getScreenIdForPageIndex(page) == -301 || this.mWorkspace.getScreenIdForPageIndex(page) == -501) {
                    return false;
                }
                this.mInScrollArea = true;
                setCurrentDragOverlappingLayout((CellLayout) this.mWorkspace.getChildAt(page));
                this.mWorkspace.invalidate();
                result = true;
            }
        }
        return result;
    }

    public boolean onExitScrollArea() {
        if (!this.mInScrollArea) {
            return false;
        }
        this.mWorkspace.invalidate();
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
        this.mInScrollArea = false;
        return true;
    }

    private void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        this.mInScrollArea = false;
    }

    public void setDragMode(int dragMode) {
        if (dragMode != this.mDragMode) {
            if (dragMode == 0) {
                this.mReorderController.cleanupReorder(false);
                this.mFolderController.cleanup();
            } else if (dragMode == 2) {
                this.mReorderController.cleanupReorder(true);
            } else if (dragMode == 1) {
                this.mReorderController.cleanupReorder(true);
            } else if (dragMode == 3) {
                this.mFolderController.cleanup();
            } else if (dragMode == 4) {
                this.mReorderController.cleanupReorder(false);
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
        return this.mHomeController.getBindController().addFolder(layout, -100, destInfo.screenId, destInfo.cellX, destInfo.cellY);
    }

    public boolean canOpenFolder() {
        return this.mHomeController.canMoveHometray();
    }

    private CellLayout getCurrentDropLayout() {
        return (CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getNextPage());
    }

    private void setCurrentDropLayout(CellLayout layout) {
        if (this.mDragTargetLayout != null) {
            this.mReorderController.revertTempState();
            this.mReorderController.setUseTempCoords(false);
            this.mDragTargetLayout.onDragExit();
        }
        this.mDragTargetLayout = layout;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragEnter();
            this.mReorderController.setReorderTarget(this.mDragTargetLayout);
            this.mFolderController.setReorderTarget(this.mDragTargetLayout);
        }
        this.mReorderController.cleanupReorder(true);
        setCurrentDropOverCell(-1, -1);
    }

    private void setCurrentDragOverlappingLayout(CellLayout layout) {
        this.mDragOverlappingLayout = layout;
    }

    private void setCurrentDropOverCell(int x, int y) {
        if (x != this.mDragOverX || y != this.mDragOverY) {
            this.mDragOverX = x;
            this.mDragOverY = y;
            setDragMode(0);
        }
    }

    boolean createUserFolderIfNecessary(int[] targetCell, View newView, DragObject d) {
        return this.mFolderController.onDropCreateUserFolder(null, targetCell, newView, null, d);
    }

    boolean addToExistingFolderIfNecessary(int[] targetCell, DragObject d) {
        return this.mFolderController.onDropAddToExistingFolder(null, targetCell, d);
    }

    void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView, Runnable onCompleteRunnable, int animationType, View finalView, boolean external) {
        Rect from = new Rect();
        this.mDragLayer.getViewRectRelativeToSelf(dragView, from);
        int[] finalPos = new int[2];
        float[] scaleXY = new float[2];
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, this.mTargetCell, !(info instanceof PendingAddShortcutInfo));
        int duration = this.mLauncher.getResources().getInteger(R.integer.config_dropAnimMaxDuration) - 200;
        if ((finalView instanceof AppWidgetHostView) && external) {
            this.mDragLayer.removeView(finalView);
        }
        boolean isWidget = info.itemType == 4 || info.itemType == 5;
        if ((animationType == 2 || external) && finalView != null) {
            int[] unScaledSize = this.mWorkspace.estimateItemSize(info);
            if (LauncherFeature.supportFlexibleGrid() && (finalView instanceof LauncherAppWidgetHostView)) {
                ResizeResult resizeResult = LauncherAppWidgetHostView.calculateWidgetSize(info.spanX, info.spanY, unScaledSize[0], unScaledSize[1]);
                if (resizeResult != null) {
                    unScaledSize[0] = resizeResult.width;
                    unScaledSize[1] = resizeResult.height;
                }
            }
            dragView.setCrossFadeBitmap(DragViewHelper.createWidgetBitmap(finalView, unScaledSize));
            dragView.crossFade((int) (((float) duration) * 0.8f));
        } else if (isWidget && external) {
            float min = Math.min(scaleXY[0], scaleXY[1]);
            scaleXY[1] = min;
            scaleXY[0] = min;
        }
        if (animationType == 4) {
            this.mDragLayer.animateViewIntoPosition(dragView, finalPos, 0.0f, 0.1f, 0.1f, 0, onCompleteRunnable, duration);
            return;
        }
        int endStyle;
        if (animationType == 1) {
            endStyle = 2;
        } else {
            endStyle = 0;
        }
        final View view = finalView;
        final Runnable runnable = onCompleteRunnable;
        this.mDragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0], finalPos[1], 1.0f, 1.0f, 1.0f, scaleXY[0], scaleXY[1], new Runnable() {
            public void run() {
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        }, endStyle, duration, this.mWorkspace);
    }

    private void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY, DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell, boolean scale) {
        float dragViewScaleX;
        float dragViewScaleY;
        CellLayout cellLayout = layout;
        Rect r = this.mWorkspace.estimateItemPosition(cellLayout, targetCell[0], targetCell[1], info.spanX, info.spanY);
        loc[0] = r.left;
        loc[1] = r.top;
        float cellLayoutScale = this.mDragLayer.getDescendantCoordRelativeToSelf(layout, loc, true);
        if (scale) {
            dragViewScaleX = (1.0f * ((float) r.width())) / ((float) dragView.getMeasuredWidth());
            dragViewScaleY = (1.0f * ((float) r.height())) / ((float) dragView.getMeasuredHeight());
        } else {
            dragViewScaleX = 1.0f;
            dragViewScaleY = 1.0f;
        }
        if (!(info instanceof PendingAddShortcutInfo)) {
            loc[0] = (int) (((float) loc[0]) - ((((float) dragView.getMeasuredWidth()) - (((float) r.width()) * cellLayoutScale)) / 2.0f));
            loc[1] = (int) (((float) loc[1]) - ((((float) dragView.getMeasuredHeight()) - (((float) r.height()) * cellLayoutScale)) / 2.0f));
        } else if (this.mLauncher.getDeviceProfile().isLandscape) {
            loc[0] = loc[0] + ((dragView.getMeasuredWidth() - layout.getContentIconSize()) / 2);
            loc[1] = loc[1] + ((layout.getCellHeight() - dragView.getMeasuredHeight()) / 2);
        } else {
            loc[0] = (int) (((float) loc[0]) - ((((float) dragView.getMeasuredWidth()) - (((float) r.width()) * cellLayoutScale)) / 2.0f));
            loc[1] = (int) (((float) loc[1]) + (((float) layout.getContentTop()) * cellLayoutScale));
        }
        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    private void findEmptySpace(DragObject d) {
        int count;
        CellLayout cl;
        boolean checkExtra = this.mWorkspace.hasExtraEmptyScreen();
        if (checkExtra) {
            count = this.mWorkspace.getChildCount() - 2;
        } else {
            count = this.mWorkspace.getChildCount() - 1;
        }
        int index = count;
        while (index > 0) {
            cl = (CellLayout) this.mWorkspace.getPageAt(index);
            if (cl != null && cl.getCellLayoutChildren().getChildCount() > 0) {
                break;
            }
            index--;
        }
        ItemInfo info = d.dragInfo;
        cl = this.mWorkspace.getScreenWithId(this.mWorkspace.getScreenIdForPageIndex(index));
        if (cl == null || !cl.findCellForSpan(this.mTargetCell, info.spanX, info.spanY, true)) {
            if (index == count) {
                if (!checkExtra) {
                    this.mWorkspace.addExtraEmptyScreen();
                }
                this.mDropToLayout = this.mWorkspace.getScreenWithId(-201);
                this.mWorkspace.commitExtraEmptyScreen();
            } else {
                this.mDropToLayout = this.mWorkspace.getScreenWithId(this.mWorkspace.getScreenIdForPageIndex(index + 1));
            }
            setCurrentDropLayout(this.mDropToLayout);
            this.mTargetCell[0] = 0;
            this.mTargetCell[1] = 0;
            return;
        }
        this.mDropToLayout = cl;
        setCurrentDropLayout(this.mDropToLayout);
    }

    private boolean findEmptyCellForExtraDragObject(int[] targetCell, boolean canceled, boolean cancelDropFolder, boolean addEmptyScreen, boolean findTotalPage) {
        int screen = this.mWorkspace.getPageIndexForScreenId(this.mWorkspace.getIdForScreen(this.mDropToLayout));
        int totalPage = findTotalPage ? this.mWorkspace.getPageCount() : screen + 1;
        for (int i = screen; i < totalPage; i++) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(i);
            if (cl != null) {
                int prevTargetX;
                int prevTargetY;
                int countX = cl.getCountX();
                int countY = cl.getCountY();
                boolean[][] occupied = cl.getOccupied();
                if (i != screen || (canceled && !cancelDropFolder)) {
                    prevTargetX = 0;
                    prevTargetY = 0;
                } else {
                    prevTargetX = this.mTargetCell[0];
                    prevTargetY = this.mTargetCell[1];
                }
                if (prevTargetX < 0 || prevTargetY < 0) {
                    prevTargetX = 0;
                    prevTargetY = 0;
                }
                if (this.mWorkspace.getScreenIdForPageIndex(i) == -201) {
                    this.mWorkspace.commitExtraEmptyScreen();
                }
                if (Utilities.findVacantCellToRightBottom(targetCell, 1, 1, countX, countY, occupied, prevTargetX, prevTargetY)) {
                    this.mDropToLayout = cl;
                    setCurrentDropLayout(this.mDropToLayout);
                    return true;
                } else if (Utilities.findVacantCellToLeftTop(targetCell, 1, 1, countX, countY, occupied, prevTargetX, prevTargetY)) {
                    this.mDropToLayout = cl;
                    setCurrentDropLayout(this.mDropToLayout);
                    return true;
                }
            }
        }
        if (addEmptyScreen) {
            if (!this.mWorkspace.hasExtraEmptyScreen()) {
                this.mWorkspace.addExtraEmptyScreen();
            }
            this.mDropToLayout = this.mWorkspace.getScreenWithId(this.mWorkspace.commitExtraEmptyScreen());
            setCurrentDropLayout(this.mDropToLayout);
            targetCell[0] = 0;
            targetCell[1] = 0;
        }
        return false;
    }

    private boolean checkSingleInstanceAppWidget(HashMap<String, LongSparseArray<Integer>> list, String key, UserHandleCompat user) {
        if (!list.containsKey(key)) {
            return false;
        }
        LongSparseArray<Integer> userMap = (LongSparseArray) list.get(key);
        Long profileId = Long.valueOf(UserManagerCompat.getInstance(this.mLauncher).getSerialNumberForUser(user));
        if (userMap.get(profileId.longValue()) == null || ((Integer) userMap.get(profileId.longValue())).intValue() < 1) {
            return false;
        }
        return true;
    }

    private boolean isExistSingleInstanceAppWidget(PendingAddItemInfo info) {
        boolean isExistPackageListWidget = checkSingleInstanceAppWidget(HomeController.sSingleInstanceAppWidgetPackageList, info.componentName.getPackageName(), info.user);
        boolean isExistComponentListWidget = checkSingleInstanceAppWidget(HomeController.sSingleInstanceAppWidgetList, info.componentName.flattenToShortString(), info.user);
        if (!isExistPackageListWidget && !isExistComponentListWidget) {
            return false;
        }
        String popupSting;
        if (this.mLauncher.getResources().getBoolean(R.bool.config_isLightTheme)) {
            this.mLauncher.setTheme(16974130);
        }
        if (this.mLauncher.getLauncherModel().getHomeLoader().checkDuplicatedSingleInstanceWidgetExist(info) == null || !isExistPackageListWidget) {
            popupSting = this.mLauncher.getString(R.string.only_one_widget_instance_allowed, new Object[]{info.mLabel});
        } else {
            popupSting = this.mLauncher.getString(R.string.duplcation_widget_instance_allowed_differ_type, new Object[]{widgetLabel});
        }
        Toast.makeText(this.mLauncher, popupSting, 1).show();
        return true;
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
        }
    }

    public void onExtraObjectDropCompleted(View target, ArrayList<DragObject> arrayList, ArrayList<DragObject> failedDragObjects, int fullCnt) {
        if (failedDragObjects != null) {
            onDropExtraObjects(failedDragObjects, null, false, true, false, false, fullCnt, target instanceof Hotseat);
        }
    }

    public int getPageIndexForDragView(ItemInfo item) {
        if (item == null) {
            return this.mWorkspace.getNextPage();
        }
        int pageIndex = this.mWorkspace.getPageIndexForScreenId(item.screenId);
        if (!this.mHomeController.isItemInFolder(item)) {
            return pageIndex;
        }
        View folder = this.mHomeController.getHomescreenIconByItemId(item.container);
        if (folder == null) {
            return pageIndex;
        }
        FolderInfo folderInfo = (FolderInfo) folder.getTag();
        if (folderInfo.container == -101) {
            return this.mWorkspace.getNextPage();
        }
        return this.mWorkspace.getPageIndexForScreenId(folderInfo.screenId);
    }

    void dropCompletedWidgetFromHotseat(DragObject d) {
        findEmptyCellForExtraDragObject(this.mTargetCell, true, false, true, true);
        final PendingAddItemInfo info = d.dragInfo;
        final long screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
        animateWidgetDrop(info, this.mDropToLayout, d.dragView, new Runnable() {
            public void run() {
                WorkspaceDragController.this.mHomeController.addPendingItem(info, -100, screenId, WorkspaceDragController.this.mTargetCell, info.spanX, info.spanY);
            }
        }, 1, null, true);
    }

    void dropCompletedFromHotseat(ArrayList<DragObject> extraDragObjects, Runnable postRunnable, boolean fromOther, int fullCnt) {
        onDropExtraObjects(extraDragObjects, postRunnable, false, true, false, fromOther, fullCnt, true);
    }

    public int getDragSourceType() {
        return 0;
    }

    private void sayDragTalkBack(boolean internal, boolean isMovedLayout, int cellX, int cellY) {
        String description;
        Resources res = this.mLauncher.getResources();
        if (!internal) {
            description = res.getString(R.string.tts_item_moved) + " " + res.getString(R.string.tts_item_dims_format, new Object[]{Integer.valueOf(cellY + 1), Integer.valueOf(cellX + 1)});
        } else if (isMovedLayout) {
            int currentPage = this.mWorkspace.getCurrentPage() + 1;
            int maxPages = this.mWorkspace.getPageCount();
            if (currentPage != maxPages) {
                maxPages--;
            }
            description = res.getString(R.string.tts_item_moved_to_other_page, new Object[]{Integer.valueOf(currentPage), Integer.valueOf(maxPages)}) + ", " + res.getString(R.string.tts_item_dims_format, new Object[]{Integer.valueOf(cellY + 1), Integer.valueOf(cellX + 1)});
        } else if (!this.mLauncher.isQuickOptionShowing()) {
            description = res.getString(R.string.tts_item_moved);
        } else {
            return;
        }
        Talk.INSTANCE.say(description);
    }

    private void restoreExtraDropItems(DragObject d, boolean isFromFolder) {
        ItemInfo objectInfo = d.dragInfo;
        if (d.dragView == null) {
            Log.d(DropTarget.TAG, "dragView is null");
            return;
        }
        if (d.dragView.getParent() != null) {
            d.deferDragViewCleanupPostAnimation = false;
            this.mDragLayer.removeView(d.dragView);
        }
        if (isFromFolder) {
            View folder = this.mHomeController.getHomescreenIconByItemId(objectInfo.container);
            if (folder != null) {
                CellLayout original;
                FolderInfo folderInfo = (FolderInfo) folder.getTag();
                if (folderInfo.container == -101) {
                    original = this.mHomeController.getHotseat().getLayout();
                } else {
                    original = this.mWorkspace.getScreenWithId(folderInfo.screenId);
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
                long container = objectInfo.container;
                long screenId = objectInfo.screenId;
                int cellX = objectInfo.cellX;
                int cellY = objectInfo.cellY;
                CellLayout cl = this.mWorkspace.getScreenWithId(objectInfo.screenId);
                if (cl != null && cl.isOccupied(cellX, cellY)) {
                    findEmptyCellForExtraDragObject(this.mTargetCell, false, false, 1, true);
                    int cellX2 = this.mTargetCell[0];
                    int cellY2 = this.mTargetCell[1];
                    screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
                    this.mHomeController.addOrMoveItemInDb(objectInfo, container, screenId, this.mTargetCell[0], this.mTargetCell[1], -1);
                    cellY = cellY2;
                    cellX = cellX2;
                }
                this.mHomeController.addInScreen(objectView, container, screenId, cellX, cellY, objectInfo.spanX, objectInfo.spanY);
            } else {
                ArrayList<DragObject> restoredHotseatObjects = new ArrayList();
                restoredHotseatObjects.add(d);
                this.mHomeController.getHotseat().getDragController().restoreHotseatObjects(restoredHotseatObjects);
            }
        }
        this.mLauncher.getDragMgr().onDeferredEndDrag(d.dragView);
    }

    private boolean itemFromAppsController(ItemInfo info) {
        if (info.container == -102) {
            return true;
        }
        if (this.mHomeController.isItemInFolder(info) && this.mHomeController.getHomescreenIconByItemId(info.container) == null) {
            return true;
        }
        return false;
    }

    private void addItemOnLastItem(ArrayList<DragObject> objects, boolean removeOriginalView) {
        long screenId = 0;
        this.mTargetCell[0] = 0;
        this.mTargetCell[1] = 0;
        boolean canEnterResizeMode = false;
        LauncherAppWidgetHostView hostView = null;
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            View view;
            ItemInfo info;
            LayoutParams lp;
            DragObject object = (DragObject) it.next();
            ItemInfo localInfo = null;
            ItemInfo info2 = object.dragInfo;
            boolean removeView = removeOriginalView && !itemFromAppsController(info2);
            if (!equals(object.dragSource) && !(object.dragSource instanceof HotseatDragController)) {
                switch (info2.itemType) {
                    case 0:
                    case 1:
                    case 6:
                        if (removeView) {
                            localInfo = info2;
                        } else {
                            localInfo = ((IconInfo) info2).makeCloneInfo();
                        }
                        view = this.mHomeController.getBindController().createShortcut(this.mDropToLayout, (IconInfo) localInfo);
                        break;
                    case 2:
                        localInfo = ((FolderInfo) info2).makeCloneInfo();
                        view = FolderIconView.fromXml(this.mLauncher, this.mDropToLayout, (FolderInfo) localInfo, this.mHomeController, this.mLauncher, null, 0);
                        break;
                    default:
                        throw new IllegalStateException("Unknown item type: " + info2.itemType);
                }
            } else if (object.dragView != null) {
                view = object.dragView.getSourceView();
            } else {
                view = null;
            }
            if (localInfo == null || info2 == localInfo) {
                info = info2;
            } else {
                info = localInfo;
            }
            findEmptySpace(object);
            screenId = this.mWorkspace.getIdForScreen(this.mDropToLayout);
            if (view instanceof LauncherAppWidgetHostView) {
                this.mHomeController.enterDragState(false);
                hostView = (LauncherAppWidgetHostView) view;
                if (this.mHomeController.canEnterResizeMode(hostView, this.mDropToLayout)) {
                    canEnterResizeMode = true;
                }
                this.mHomeController.removeHomeItem((View) hostView);
            } else if (removeView) {
                this.mHomeController.removeHomeItem(info);
            }
            if (view == null) {
                lp = null;
            } else {
                lp = (LayoutParams) view.getLayoutParams();
            }
            if (lp != null) {
                lp.isLockedToGrid = true;
            }
            this.mHomeController.addOrMoveItemInDb(info, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], -1);
            if ((info instanceof FolderInfo) && !(removeOriginalView && removeView)) {
                this.mHomeController.addFolderItemsToDb(new ArrayList(((FolderInfo) info).contents), info.id);
                if (this.mFolderLock != null && ((FolderInfo) info).isLocked()) {
                    this.mFolderLock.addLockedRecords((FolderInfo) info);
                }
            }
            view.setVisibility(View.VISIBLE);
            this.mHomeController.addInScreen(view, -100, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
            if (removeOriginalView) {
                WorkspaceCellLayout targetCellLayout = (WorkspaceCellLayout) this.mWorkspace.getChildAt((int) screenId);
                if (targetCellLayout != null) {
                    targetCellLayout.updateIconViews(false);
                }
            }
        }
        final int pageIndexForScreenId = this.mWorkspace.getPageIndexForScreenId(screenId);
        final boolean z = canEnterResizeMode;
        final LauncherAppWidgetHostView launcherAppWidgetHostView = hostView;
        this.mWorkspace.post(new Runnable() {
            public void run() {
                WorkspaceDragController.this.mWorkspace.snapToPage(pageIndexForScreenId);
                if (z) {
                    WorkspaceDragController.this.mHomeController.enterResizeStateDelay(launcherAppWidgetHostView, WorkspaceDragController.this.mDropToLayout);
                } else {
                    WorkspaceDragController.this.mHomeController.exitDragStateDelayed();
                }
            }
        });
    }

    private void restoreItems(ArrayList<DragObject> objects, boolean fromOther) {
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DragObject object = (DragObject) it.next();
            ItemInfo info = object.dragInfo;
            if (info.container != -101) {
                boolean isFromHomeFolder;
                if (info.isContainApps() || this.mHomeController.getHomescreenIconByItemId(info.container) == null) {
                    isFromHomeFolder = false;
                } else {
                    isFromHomeFolder = true;
                }
                if (!fromOther || isFromHomeFolder) {
                    restoreExtraDropItems(object, isFromHomeFolder);
                } else if (!(object.dragView == null || object.dragView.getParent() == null)) {
                    object.deferDragViewCleanupPostAnimation = false;
                    this.mDragLayer.removeView(object.dragView);
                }
            }
        }
    }

    public int getEmptyCount() {
        return 0;
    }
}
