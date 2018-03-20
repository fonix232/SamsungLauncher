package com.android.launcher3.allapps.controller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.util.Log;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.allapps.AppsReorderListener;
import com.android.launcher3.allapps.DragAppIcon;
import com.android.launcher3.allapps.view.AppsPagedView;
import com.android.launcher3.allapps.view.AppsPagedView.Listener;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragScroller;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderIconDropController;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppsDragController implements DropTarget, DragSource, DragScroller, DragState, DragListener {
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final Comparator<DragObject> DRAG_OBJECT_COMPARATOR = new Comparator<DragObject>() {
        public int compare(DragObject lhs, DragObject rhs) {
            return ((((int) ((ItemInfo) lhs.dragInfo).screenId) * 100) + ((ItemInfo) lhs.dragInfo).rank) - (((ItemInfo) rhs.dragInfo).rank + (((int) ((ItemInfo) rhs.dragInfo).screenId) * 100));
        }
    };
    private static final String TAG = "AppsDragController";
    private static final int[] sTempPosArray = new int[2];
    private DragOperator mAlphabetOperator = new AlphabetOperator();
    private AppsPagedView mAppsPagedView;
    private DragOperator mCustomOperator = new CustomOperator();
    private boolean mDragComplete = true;
    private CellInfo mDragInfo;
    private DragLayer mDragLayer;
    private int mDragMode = 0;
    private DragOperator mDragOperator;
    private CellLayout mDragOverlappingLayout = null;
    private CellLayout mDragTargetLayout = null;
    private float[] mDragViewVisualCenter = new float[2];
    private CellLayout mDropToLayout = null;
    private DragAppIcon mEmpty = new DragAppIcon();
    private FolderIconDropController mFolderController;
    private boolean mInScrollArea = false;
    private Launcher mLauncher;
    private Listener mListener;
    private DragAppIcon mPrevTarget = new DragAppIcon();
    private final Alarm mReorderAlarm = new Alarm();
    private OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            AppsDragController.this.mReorderListener.realTimeReorder(AppsDragController.this.mEmpty, AppsDragController.this.mTarget);
            try {
                AppsDragController.this.mEmpty = (DragAppIcon) AppsDragController.this.mTarget.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(AppsDragController.TAG, "ReorderAlarm:" + e.toString());
            }
            AppsDragController.this.setDragMode(3);
        }
    };
    private AppsReorderListener mReorderListener;
    private DragAppIcon mTarget = new DragAppIcon();
    private int[] mTargetCell = new int[2];

    class AlphabetOperator extends DragOperator {
        AlphabetOperator() {
        }

        void addItemToTarget(View cell, DragAppIcon target) {
            ItemInfo item = (ItemInfo) cell.getTag();
            long screenId = item.screenId;
            if (target.screenId != screenId) {
                AppsDragController.this.mAppsPagedView.snapToPage((int) screenId);
            }
            AppsDragController.this.mAppsPagedView.addItem(cell, item);
        }

        void dropCreateFolder(ItemInfo dragItem, boolean internal) {
            if (!internal && (dragItem instanceof FolderInfo) && ((FolderInfo) dragItem).getItemCount() >= 2) {
                AppsDragController.this.mListener.repositionByNormalizer(true);
            }
        }

        void updateItemPosition(ItemInfo info, long screenId, int rank) {
            AppsDragController.this.mListener.addOrMoveItemInDb(info, -102, -1, -1, -1, -1);
        }

        void animateViewIntoPosition(DragObject dragObject, ItemInfo info, View child, int duration, Runnable onFinishAnimationRunnable, View anchorView, boolean animate, boolean needRunnableDelay) {
            Runnable runnable;
            if (needRunnableDelay) {
                runnable = null;
            } else {
                runnable = onFinishAnimationRunnable;
            }
            if (needRunnableDelay) {
                new Handler().postDelayed(onFinishAnimationRunnable, (long) duration);
            }
            int currentPageIndex = AppsDragController.this.mAppsPagedView.getNextPage();
            int translatedX = 0;
            int translatedY = 0;
            if (AppsDragController.this.mDropToLayout != null) {
                int expectedX = (AppsDragController.this.mAppsPagedView.getPaddingStart() + ((((int) info.screenId) - currentPageIndex) * AppsDragController.this.mAppsPagedView.getDesiredWidth())) + ((View) AppsDragController.this.mAppsPagedView.getParent()).getPaddingStart();
                Rect pageRect = new Rect();
                AppsDragController.this.mDragLayer.getViewRectRelativeToSelf(AppsDragController.this.mDropToLayout, pageRect);
                translatedX = expectedX - pageRect.left;
                translatedY = (AppsDragController.this.mLauncher.getDeviceProfile().appsGrid.getPageTop() - AppsDragController.this.mLauncher.getDeviceProfile().appsExtraPaddingTop) - pageRect.top;
            }
            if (!animate) {
                AppsDragController.this.mDragLayer.removeAnimation(dragObject.dragView, runnable);
            } else if (dragObject.dragView != null) {
                AppsDragController.this.mDragLayer.animateViewIntoPosition(dragObject.dragView, child, duration, onFinishAnimationRunnable, null, translatedX, translatedY);
            }
        }

        void dragOver(DragObject dragObject) {
            if (!AppsDragController.this.equals(dragObject.dragSource) || AppsDragController.this.mDragMode == 2 || AppsDragController.this.mDragMode == 1) {
                setRestorePosition(false);
            } else {
                setRestorePosition(true);
            }
        }

        void refreshObjectsToPosition(DragObject dragObject, int indexScreen, int indexRank, ArrayList<DragObject> extraDragObjects) {
            dropExtraObjects(dragObject, indexScreen, indexRank, extraDragObjects, false);
        }

        void dropCompletedWithOutExtra(boolean restorePosition) {
            if (!getRestorePosition()) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (!LauncherFeature.supportQuickOption() || !AppsDragController.this.mLauncher.getDragMgr().isQuickOptionShowing()) {
                            AppsDragController.this.mListener.repositionByNormalizer(true);
                        }
                    }
                }, 300);
            }
        }

        void dropExtraObjects(DragObject dragObject, int indexScreen, int indexRank, ArrayList<DragObject> extraDragObjects, boolean internal) {
            normalizeBeforeDrop(dragObject, extraDragObjects, true);
            Iterator it = extraDragObjects.iterator();
            while (it.hasNext()) {
                DragObject d = (DragObject) it.next();
                ItemInfo info = d.dragInfo;
                int screen = (int) info.screenId;
                if (AppsDragController.this.mAppsPagedView.getCellLayout(screen) == null) {
                    AppsDragController.this.createEmptyScreen();
                }
                AppsDragController.this.mDropToLayout = AppsDragController.this.mAppsPagedView.getCellLayout(screen);
                AppsDragController.this.mAppsPagedView.rearrangeAllViews(true);
                View recycle = null;
                if (AppsDragController.this.equals(d.dragSource)) {
                    recycle = (!d.equals(dragObject) || AppsDragController.this.mDragInfo == null) ? d.dragView.getSourceView() : AppsDragController.this.mDragInfo.cell;
                } else if (d.dragView != null && (d.dragView.getSourceView() instanceof IconView) && d.dragView.getSourceView().getParent() == null) {
                    recycle = d.dragView.getSourceView();
                }
                View view = AppsDragController.this.mListener.createItemView(info, AppsDragController.this.mDropToLayout, recycle);
                AppsDragController.this.mAppsPagedView.addItem(view, info);
                if (view.getParent() != null) {
                    CellLayout parent = (CellLayout) view.getParent().getParent();
                    view.setVisibility(View.VISIBLE);
                    parent.onDropChild(view);
                    parent.getCellLayoutChildren().measureChild(view);
                }
                boolean animate = d.dragView != null && AppsDragController.this.mLauncher.isAppsStage();
                animateViewIntoPosition(d, info, view, animate ? 300 : 0, d.postAnimationRunnable, AppsDragController.this.mAppsPagedView, true, false);
            }
        }

        void dropExternal(DragObject dragObject, ItemInfo item, View view, Runnable exitDragStateRunnable) {
            boolean animate;
            int duration = 0;
            normalizeBeforeDrop(dragObject, false);
            if (AppsDragController.this.mAppsPagedView.getCellLayout((int) item.screenId) == null) {
                AppsDragController.this.createEmptyScreen();
            }
            AppsDragController.this.mAppsPagedView.rearrangeAllViews(true);
            AppsDragController.this.mAppsPagedView.addItem(view, item);
            AppsDragController.this.mDropToLayout.onDropChild(view);
            AppsDragController.this.mDropToLayout.getCellLayoutChildren().measureChild(view);
            if (dragObject.dragView == null || !AppsDragController.this.mLauncher.isAppsStage()) {
                animate = false;
            } else {
                animate = true;
            }
            if (animate) {
                duration = 300;
            }
            animateViewIntoPosition(dragObject, item, view, duration, exitDragStateRunnable, null, true, true);
        }

        private void normalizeBeforeDrop(DragObject dragObject, boolean afterDrop) {
            normalizeBeforeDrop(dragObject, dragObject.extraDragInfoList, afterDrop);
        }

        private void normalizeBeforeDrop(DragObject dragObject, ArrayList<DragObject> extraDragInfoList, boolean afterDrop) {
            ArrayList<ItemInfo> addInfos = new ArrayList();
            ArrayList<ItemInfo> removeInfos = new ArrayList();
            if (dragObject.dragSource instanceof FolderView) {
                FolderInfo folderInfo = ((FolderView) dragObject.dragSource).getInfo();
                if (!(folderInfo == null || folderInfo.contents == null)) {
                    int folderContentCount = folderInfo.contents.size();
                    if (folderContentCount <= 1) {
                        removeInfos.add(folderInfo);
                        if (folderContentCount == 1) {
                            addInfos.add(folderInfo.contents.get(0));
                        }
                    }
                }
            }
            if ((dragObject.dragInfo instanceof ItemInfo) && !afterDrop) {
                addInfos.add((ItemInfo) dragObject.dragInfo);
            }
            if (dragObject.extraDragInfoList != null) {
                Iterator it = extraDragInfoList.iterator();
                while (it.hasNext()) {
                    addInfos.add((ItemInfo) ((DragObject) it.next()).dragInfo);
                }
            }
            Iterator it2 = addInfos.iterator();
            while (it2.hasNext()) {
                updateItemPosition((ItemInfo) it2.next(), -1, -1);
            }
            AppsDragController.this.mListener.normalizeWithExtraItems(addInfos, removeInfos);
        }

        void makeEmptyCellAndReorderIfNecessary(int screenId, int rank) {
            int cellCountX = AppsDragController.this.mAppsPagedView.getCellCountX();
            if (AppsDragController.this.mAppsPagedView.getCellLayout(screenId).getChildAt(rank % cellCountX, rank / cellCountX) != null) {
                AppsDragController.this.mReorderListener.makeEmptyCellAndReorder(screenId, rank);
            }
        }
    }

    class CustomOperator extends DragOperator {
        CustomOperator() {
        }

        void dropCreateFolder(ItemInfo dragItem, boolean internal) {
            AppsDragController.this.restoreOverLastItems();
            AppsDragController.this.mReorderListener.removeEmptyCell(AppsDragController.this.mEmpty);
        }

        void dropAddToExistingFolder() {
            AppsDragController.this.restoreOverLastItems();
            AppsDragController.this.mReorderListener.removeEmptyCell(AppsDragController.this.mEmpty);
        }

        void addItemToTarget(View cell, DragAppIcon target) {
            ItemInfo item = (ItemInfo) cell.getTag();
            item.rank = target.rank;
            item.screenId = target.screenId;
            item.mDirty = true;
            AppsDragController.this.mAppsPagedView.addItem(cell, item);
            long screenId = AppsDragController.this.mTargetCell[0] < 0 ? AppsDragController.this.mDragInfo.screenId : AppsDragController.this.mAppsPagedView.getIdForScreen(AppsDragController.this.mDropToLayout);
            if (((long) AppsDragController.this.mAppsPagedView.getNextPage()) != screenId) {
                AppsDragController.this.mAppsPagedView.snapToPage((int) screenId);
            }
        }

        void updateItemPosition(ItemInfo info, long screenId, int rank) {
            info.container = -102;
            info.screenId = screenId;
            info.rank = rank;
            info.mDirty = true;
        }

        void animateViewIntoPosition(DragObject dragObject, ItemInfo info, View child, int duration, Runnable onFinishAnimationRunnable, View anchorView, boolean animate, boolean needRunnableDelay) {
            int currentPageIndex = AppsDragController.this.mAppsPagedView.getNextPage();
            int translatedX = 0;
            int translatedY = 0;
            if (AppsDragController.this.mDropToLayout != null) {
                int expectedX = (AppsDragController.this.mAppsPagedView.getPaddingStart() + ((((int) info.screenId) - currentPageIndex) * AppsDragController.this.mAppsPagedView.getDesiredWidth())) + ((View) AppsDragController.this.mAppsPagedView.getParent()).getPaddingStart();
                Rect pageRect = new Rect();
                AppsDragController.this.mDragLayer.getViewRectRelativeToSelf(AppsDragController.this.mDropToLayout, pageRect);
                translatedX = expectedX - pageRect.left;
                translatedY = (AppsDragController.this.mLauncher.getDeviceProfile().appsGrid.getPageTop() - AppsDragController.this.mLauncher.getDeviceProfile().appsExtraPaddingTop) - pageRect.top;
            }
            if (dragObject.dragView != null) {
                AppsDragController.this.mDragLayer.animateViewIntoPosition(dragObject.dragView, child, 300, onFinishAnimationRunnable, null, translatedX, translatedY);
                return;
            }
            new Handler().postDelayed(onFinishAnimationRunnable, (long) duration);
        }

        void makeEmptyCellAndReorderIfNecessary(int screenId, int rank) {
            int cellCountX = AppsDragController.this.mAppsPagedView.getCellCountX();
            if (AppsDragController.this.mAppsPagedView.getCellLayout(screenId).getChildAt(rank % cellCountX, rank / cellCountX) != null) {
                AppsDragController.this.mReorderListener.makeEmptyCellAndReorder(screenId, rank);
            }
        }

        void updateDirtyItemsToDb() {
            AppsDragController.this.mListener.updateDirtyItems();
        }

        void dragOver(DragObject dragObject) {
            if (AppsDragController.this.mDragMode != 1 && AppsDragController.this.mDragMode != 2) {
                if (!(AppsDragController.this.mTarget.rank == AppsDragController.this.mPrevTarget.rank && AppsDragController.this.mTarget.screenId == AppsDragController.this.mPrevTarget.screenId)) {
                    long j;
                    AppsDragController.this.setDragMode(0);
                    if (!(AppsDragController.this.mEmpty.rank == AppsDragController.this.mTarget.rank && AppsDragController.this.mTarget.screenId == AppsDragController.this.mPrevTarget.screenId)) {
                        setRestorePosition(false);
                        Log.d(AppsDragController.TAG, "onDragOver mRestorePositionOnDrop = false");
                    }
                    Alarm access$1400 = AppsDragController.this.mReorderAlarm;
                    if (AppsDragController.this.mAppsPagedView.isPageMoving()) {
                        j = (long) ((PagedView.PAGE_SNAP_ANIMATION_DURATION / 2) + Callback.DEFAULT_SWIPE_ANIMATION_DURATION);
                    } else {
                        j = (long) (AppsDragController.this.mLauncher.isAppsStage() ? Callback.DEFAULT_SWIPE_ANIMATION_DURATION : 500);
                    }
                    access$1400.setAlarm(j);
                    AppsDragController.this.mPrevTarget.rank = AppsDragController.this.mTarget.rank;
                    AppsDragController.this.mPrevTarget.screenId = AppsDragController.this.mTarget.screenId;
                }
                AppsDragController.this.mDragTargetLayout.visualizeDropLocation((ItemInfo) dragObject.dragInfo, dragObject.dragView.getDragOutline(), AppsDragController.this.mTargetCell[0], AppsDragController.this.mTargetCell[1], 1, 1, false);
            }
        }

        void onAdjustDraggedObjectPosition(DragObject dragObject, int startPos, int endPos, int screenId) {
            AppsDragController.this.mReorderListener.realTimeReorder(0, 0.0f, startPos, endPos, 1, screenId);
            AppsDragController.this.mTarget.rank = endPos;
            AppsDragController.this.mTarget.screenId = (long) screenId;
            AppsDragController.this.mEmpty.rank = endPos;
            AppsDragController.this.mEmpty.screenId = (long) screenId;
            ((ItemInfo) dragObject.dragInfo).rank = endPos;
            ((ItemInfo) dragObject.dragInfo).screenId = (long) screenId;
        }

        void removeEmptyCellIfNecessary(DragAppIcon empty) {
            AppsDragController.this.mReorderListener.removeEmptyCell(empty);
        }

        void refreshObjectsToPosition(DragObject dragObject, int indexScreen, int indexRank, ArrayList<DragObject> extraDragObjects) {
            dropExtraObjects(dragObject, indexScreen, indexRank, extraDragObjects, false);
        }

        void dropCompletedWithOutExtra(boolean restorePosition) {
            if (!restorePosition) {
                AppsDragController.this.mListener.updateDirtyItems();
            }
        }

        void dropExtraObjects(DragObject dragObject, int indexScreen, int indexRank, ArrayList<DragObject> extraDragObjects, boolean internal) {
            Iterator it = extraDragObjects.iterator();
            while (it.hasNext()) {
                DragObject d = (DragObject) it.next();
                ItemInfo info = d.dragInfo;
                if (indexRank >= AppsDragController.this.mAppsPagedView.getMaxItemsPerScreen() - 1) {
                    indexRank = 0;
                    indexScreen++;
                } else {
                    indexRank++;
                }
                if (indexScreen == AppsDragController.this.mAppsPagedView.getExtraEmptyScreenIndex() || AppsDragController.this.mAppsPagedView.getCellLayout(indexScreen) == null) {
                    AppsDragController.this.createEmptyScreen();
                }
                AppsDragController.this.mDropToLayout = AppsDragController.this.mAppsPagedView.getCellLayout(indexScreen);
                makeEmptyCellAndReorderIfNecessary(indexScreen, indexRank);
                View recycle = null;
                if (AppsDragController.this.equals(d.dragSource)) {
                    recycle = (!d.equals(dragObject) || AppsDragController.this.mDragInfo == null) ? d.dragView.getSourceView() : AppsDragController.this.mDragInfo.cell;
                } else if (d.dragView != null && (d.dragView.getSourceView() instanceof IconView) && d.dragView.getSourceView().getParent() == null) {
                    recycle = d.dragView.getSourceView();
                }
                View view = AppsDragController.this.mListener.createItemView(info, AppsDragController.this.mDropToLayout, recycle);
                updateItemPosition(info, (long) indexScreen, indexRank);
                AppsDragController.this.mAppsPagedView.addItem(view, info);
                if (view.getParent() != null) {
                    CellLayout parent = (CellLayout) view.getParent().getParent();
                    view.setVisibility(View.VISIBLE);
                    parent.onDropChild(view);
                    parent.getCellLayoutChildren().measureChild(view);
                }
                boolean animate = d.dragView != null && AppsDragController.this.mLauncher.isAppsStage();
                animateViewIntoPosition(d, info, view, 300, d.postAnimationRunnable, AppsDragController.this.mAppsPagedView, animate, false);
                AppsDragController.this.mListener.updateItemInDb(info);
            }
            if (internal) {
                AppsDragController.this.mListener.updateDirtyItems();
            }
        }

        void dropInternal(DragObject dragObject, ItemInfo item, View dragView) {
            if (AppsDragController.this.mReorderAlarm.alarmPending()) {
                AppsDragController.this.mReorderAlarm.cancelAlarm();
                AppsDragController.this.mReorderListener.realTimeReorder(AppsDragController.this.mEmpty, AppsDragController.this.mTarget);
            }
            if (AppsDragController.this.mTarget.screenId != item.screenId) {
                if (AppsDragController.this.mLauncher.getMultiSelectManager().isMultiSelectMode()) {
                    Resources res = AppsDragController.this.mLauncher.getResources();
                    SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_SM_MoveItem));
                } else {
                    SALogging.getInstance().insertMoveToPageLog(dragObject.dragInfo, false);
                }
            }
            addItemToTarget(dragView, AppsDragController.this.mTarget);
            if (dragView.getParent() != null) {
                CellLayout parent = (CellLayout) dragView.getParent().getParent();
                if (dragObject.dragView.hasDrawn()) {
                    AppsDragController.this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, dragView, 300, dragObject.postAnimationRunnable, AppsDragController.this.mAppsPagedView);
                } else {
                    dragObject.deferDragViewCleanupPostAnimation = false;
                    dragView.setVisibility(View.VISIBLE);
                    AppsDragController.this.mListener.updateCountBadge(dragView, false);
                }
                parent.onDropChild(dragView);
            }
        }

        void dropExternal(DragObject dragObject, ItemInfo item, View view, Runnable exitDragStateRunnable) {
            long screenId = AppsDragController.this.mTargetCell[0] < 0 ? AppsDragController.this.mDragInfo.screenId : AppsDragController.this.mAppsPagedView.getIdForScreen(AppsDragController.this.mDropToLayout);
            if (((long) AppsDragController.this.mAppsPagedView.getNextPage()) != screenId) {
                AppsDragController.this.mAppsPagedView.snapToPage((int) screenId);
            }
            int pageIndex = AppsDragController.this.mAppsPagedView.getNextPage();
            CellLayout page = AppsDragController.this.mAppsPagedView.getCellLayout(pageIndex);
            if (page.getChildAt(AppsDragController.this.mTargetCell[0], AppsDragController.this.mTargetCell[1]) == null) {
                int count = page.getCellLayoutChildren().getChildCount();
                if (AppsDragController.this.mAppsPagedView.findFirstEmptyCell(pageIndex) >= count || AppsDragController.this.mTarget.rank > count) {
                    AppsDragController.this.mTarget.rank = count;
                    if (AppsDragController.this.mTarget.screenId != AppsDragController.this.mEmpty.screenId) {
                        AppsDragController.this.mDragOperator.removeEmptyCellIfNecessary(AppsDragController.this.mEmpty);
                        Log.d(AppsDragController.TAG, "remove emptycell if necessary : " + AppsDragController.this.mEmpty.screenId + " , " + AppsDragController.this.mEmpty.rank + " , " + AppsDragController.this.mTarget.screenId);
                    }
                    AppsDragController.this.mEmpty.rank = count;
                    AppsDragController.this.mEmpty.screenId = AppsDragController.this.mTarget.screenId;
                    Log.d(AppsDragController.TAG, "assign empty : " + AppsDragController.this.mEmpty.screenId + " , " + AppsDragController.this.mEmpty.rank);
                }
            }
            if (AppsDragController.this.mReorderAlarm.alarmPending()) {
                AppsDragController.this.mReorderAlarm.cancelAlarm();
                AppsDragController.this.mReorderListener.realTimeReorder(AppsDragController.this.mEmpty, AppsDragController.this.mTarget);
            }
            if (AppsDragController.this.mAppsPagedView.getItemCountPageAt((int) screenId) >= AppsDragController.this.mAppsPagedView.getMaxItemsPerScreen() && AppsDragController.this.mAppsPagedView.getCellLayout(((int) screenId) + 1) == null) {
                AppsDragController.this.createEmptyScreen();
            }
            makeEmptyCellAndReorderIfNecessary((int) AppsDragController.this.mTarget.screenId, AppsDragController.this.mTarget.rank);
            updateItemPosition(item, screenId, AppsDragController.this.mTarget.rank);
            AppsDragController.this.mAppsPagedView.addItem(view, item);
            AppsDragController.this.mDropToLayout.onDropChild(view);
            AppsDragController.this.mDropToLayout.getCellLayoutChildren().measureChild(view);
            AppsDragController.this.mListener.updateItemInDb(item);
            animateViewIntoPosition(dragObject, item, view, 300, exitDragStateRunnable, null, true, true);
        }
    }

    public AppsDragController(Context context, AppsPagedView appsPagedView) {
        this.mLauncher = (Launcher) context;
        this.mAppsPagedView = appsPagedView;
        this.mDragOperator = this.mCustomOperator;
    }

    public void updateDragMode() {
        this.mDragOperator = this.mListener.isAlphabeticalMode() ? this.mAlphabetOperator : this.mCustomOperator;
    }

    public void setup(DragLayer dragLayer) {
        this.mDragLayer = dragLayer;
        this.mFolderController = new FolderIconDropController(this.mLauncher, this);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
        updateDragMode();
    }

    public void setReorderListener(AppsReorderListener listener) {
        this.mReorderListener = listener;
    }

    public void startDrag(CellInfo cellInfo) {
        this.mDragInfo = cellInfo;
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        this.mDragComplete = false;
        this.mAppsPagedView.showHintPages();
        setEmptyItemInfo((ItemInfo) info);
        this.mDragOperator.dragStart();
        if (this.mDragInfo != null) {
            this.mDragInfo.layout.removeView(this.mDragInfo.cell);
        }
        if (source instanceof AppsDragController) {
            Talk.INSTANCE.say((int) R.string.tts_add_shortcut_on_home_screen_notice);
        }
        return true;
    }

    private void setEmptyItemInfo(ItemInfo info) {
        this.mEmpty.rank = info.rank;
        this.mEmpty.screenId = info.screenId;
        this.mEmpty.cellX = info.cellX;
        this.mEmpty.cellY = info.cellY;
    }

    public boolean onDragEnd() {
        this.mAppsPagedView.hideHintPages();
        this.mListener.changeState(0, true);
        this.mListener.removeEmptyPagesAndUpdateAllItemsInfo();
        this.mDragComplete = true;
        return true;
    }

    public boolean isDropEnabled(boolean isDrop) {
        return true;
    }

    public void onDrop(DragObject dragObject) {
        this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
        if (this.mDropToLayout != null) {
            this.mAppsPagedView.mapPointFromSelfToChild(this.mDropToLayout, this.mDragViewVisualCenter);
        }
        if (this.mDragInfo == null) {
            this.mAppsPagedView.addExtraEmptyScreenOnDrag();
            onDropExternal(dragObject);
        } else {
            onDropInternal(dragObject);
        }
        this.mReorderListener.setExistOverLastItemMoved(false);
    }

    private void onDropInternal(DragObject dragObject) {
        Runnable exitDragStateRunnable = new Runnable() {
            public void run() {
                AppsDragController.this.mListener.exitDragStateDelayed();
            }
        };
        View dragView = this.mDragInfo.cell;
        ItemInfo item = dragObject.dragInfo;
        if (this.mDropToLayout == null && item != null) {
            Log.d(TAG, "can not find dropLayout : " + item.title + " , " + item.container + " , " + item.screenId + " , " + item.rank);
            this.mDropToLayout = this.mAppsPagedView.getCellLayout((int) item.screenId);
            this.mTarget.rank = item.rank;
            this.mTarget.screenId = item.screenId;
        }
        if (!dragObject.cancelled) {
            dragObject.postAnimationRunnable = exitDragStateRunnable;
            if (this.mDropToLayout != null) {
                this.mTargetCell = this.mDropToLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
            }
            if (this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, dragView, null, dragObject)) {
                this.mDragOperator.dropCreateFolder(item, true);
            } else if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, dragObject)) {
                this.mDragOperator.dropAddToExistingFolder();
            } else {
                this.mDragOperator.dropInternal(dragObject, item, dragView);
                onDropExtraObjects(dragObject, true);
            }
        }
    }

    private void onDropExternal(DragObject dragObject) {
        Runnable exitDragStateRunnable = new Runnable() {
            public void run() {
                AppsDragController.this.mListener.exitDragStateDelayed();
            }
        };
        dragObject.postAnimationRunnable = exitDragStateRunnable;
        if (this.mDropToLayout == null) {
            this.mDropToLayout = this.mAppsPagedView.getCellLayout(this.mAppsPagedView.getNextPage());
            Log.d(TAG, "onDropExternal drop through fakeview");
        }
        ItemInfo info = dragObject.dragInfo;
        refreshFolderBadge(info);
        if (this.mDropToLayout != null) {
            this.mTargetCell = this.mDropToLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], info.spanX, info.spanY, this.mTargetCell);
        }
        View view = this.mListener.createItemView(info, this.mDropToLayout, null);
        if (this.mFolderController.onDropCreateUserFolder(this.mDragViewVisualCenter, this.mTargetCell, view, null, dragObject)) {
            View folderView = this.mListener.getAppsIconByItemId(info.container);
            if (folderView != null) {
                this.mDragOperator.dropCreateFolder((FolderInfo) folderView.getTag(), false);
            }
        } else if (this.mFolderController.onDropAddToExistingFolder(this.mDragViewVisualCenter, this.mTargetCell, dragObject)) {
            this.mDragOperator.dropAddToExistingFolder();
        } else {
            this.mDragOperator.dropExternal(dragObject, info, view, exitDragStateRunnable);
            onDropExtraObjects(dragObject, false);
            this.mDragOperator.updateDirtyItemsToDb();
        }
    }

    private void onDropExtraObjects(DragObject dragObject, boolean internal) {
        if (dragObject != null && dragObject.extraDragInfoList != null) {
            ItemInfo dragItemInfo = dragObject.dragInfo;
            this.mDragOperator.dropExtraObjects(dragObject, (int) dragItemInfo.screenId, dragItemInfo.rank, dragObject.extraDragInfoList, internal);
        }
    }

    public void onDragEnter(DragObject dragObject, boolean dropTargetChanged) {
        this.mPrevTarget.rank = -1;
        this.mFolderController.onDragEnter();
        this.mFolderController.setMaxDistance(this.mLauncher.getDeviceProfile().appsGrid.getIconSize());
        this.mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        long screenId = this.mAppsPagedView.getIdForScreen(layout);
        this.mPrevTarget.screenId = screenId;
        if (this.mDragInfo == null) {
            this.mEmpty.screenId = -1;
            this.mEmpty.rank = this.mAppsPagedView.getRankForNewItem((int) screenId);
        }
        if (!equals(dragObject.dragSource)) {
            this.mDragOperator.setRestorePosition(false);
        }
        this.mReorderAlarm.setOnAlarmListener(this.mReorderAlarmListener);
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
    }

    private int getTargetRank() {
        return findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1]);
    }

    private int findNearestArea(int pixelX, int pixelY) {
        CellLayout page = this.mAppsPagedView.getCellLayout(this.mAppsPagedView.getNextPage());
        if (page != null) {
            page.findNearestArea(pixelX, pixelY, 1, 1, sTempPosArray);
        }
        return (sTempPosArray[1] * this.mAppsPagedView.getCellCountX()) + sTempPosArray[0];
    }

    public void onDragOver(DragObject dragObject) {
        ItemInfo item = dragObject.dragInfo;
        if (item == null) {
            Log.d(TAG, "DragObject has null info");
        } else if (item.spanX < 0 || item.spanY < 0) {
            throw new RuntimeException("Improper spans found");
        } else {
            this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
            DragView dragView = dragObject.dragView;
            this.mAppsPagedView.dragPullingPages((dragView.getTranslationX() + ((float) dragView.getRegistrationX())) - dragView.getOffsetX());
            CellLayout layout = getCurrentDropLayout();
            if (layout != this.mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
            if (this.mDragTargetLayout != null) {
                this.mAppsPagedView.mapPointFromSelfToChild(this.mDragTargetLayout, this.mDragViewVisualCenter);
                int pageIndex = this.mAppsPagedView.getNextPage();
                this.mTarget.rank = getTargetRank();
                this.mTarget.screenId = (long) pageIndex;
                if (this.mListener.isAlphabeticalMode() || this.mTarget.rank <= this.mAppsPagedView.getRankForNewItem(pageIndex)) {
                    this.mTargetCell = this.mDragTargetLayout.findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, this.mTargetCell);
                } else {
                    this.mTarget.rank = this.mAppsPagedView.getRankForNewItem(pageIndex);
                    this.mTargetCell[0] = this.mTarget.rank % this.mAppsPagedView.getCellCountX();
                    this.mTargetCell[1] = this.mTarget.rank / this.mAppsPagedView.getCellCountX();
                }
                this.mFolderController.onDragOver(this.mDragViewVisualCenter, this.mTargetCell, dragObject, this.mDragInfo, this.mDragMode);
                this.mDragOperator.dragOver(dragObject);
            }
        }
    }

    public void onDragExit(DragObject dragObject, boolean dropTargetChanged) {
        if (!this.mInScrollArea) {
            this.mDropToLayout = this.mDragTargetLayout;
        } else if (this.mAppsPagedView.isPageMoving()) {
            this.mDropToLayout = this.mAppsPagedView.getCellLayout(this.mAppsPagedView.getNextPage());
        } else {
            this.mDropToLayout = this.mDragOverlappingLayout;
        }
        if (this.mDragTargetLayout != this.mDropToLayout) {
            this.mFolderController.setReorderTarget(this.mDropToLayout);
        }
        this.mFolderController.onDragExit(this.mDragMode);
        onResetScrollArea();
        this.mReorderAlarm.setOnAlarmListener(null);
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);
        if (this.mDragInfo != null) {
            return;
        }
        if (!dragObject.dragComplete || dragObject.cancelled) {
            Log.d(TAG, "onDragExit with cancel or incompleted from external : " + dragObject.cancelled);
            this.mReorderListener.removeEmptyCell(this.mEmpty);
            restoreOverLastItems();
            try {
                this.mEmpty = (DragAppIcon) this.mTarget.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "onDragExit:" + e.toString());
            }
        }
    }

    public boolean acceptDrop(DragObject dragObject) {
        if (dragObject.dragInfo instanceof ItemInfo) {
            ItemInfo info = dragObject.dragInfo;
            if (!(info.itemType == 0 || info.itemType == 2)) {
                Log.w(TAG, "invalid item drop : " + info);
                return false;
            }
        }
        if (this.mDragOperator.acceptDrop(dragObject)) {
            long screenId = this.mAppsPagedView.getIdForScreen(this.mDropToLayout);
            if (screenId != -1 && screenId == ((long) this.mAppsPagedView.getExtraEmptyScreenIndex())) {
                this.mAppsPagedView.commitExtraEmptyScreen();
            }
            return true;
        }
        Log.d(TAG, "acceptDrop mRestorePositionOnDrop = " + this.mDragOperator.getRestorePosition());
        return false;
    }

    public View getTargetView() {
        return this.mAppsPagedView;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        this.mDragLayer.getDescendantRectRelativeToSelf((View) this.mAppsPagedView.getParent(), outRect, false);
    }

    public int getLeft() {
        return this.mAppsPagedView.getLeft();
    }

    public int getTop() {
        return this.mAppsPagedView.getTop();
    }

    public int getOutlineColor() {
        return this.mLauncher.getResources().getColor(17170443);
    }

    public Stage getController() {
        return (Stage) this.mListener;
    }

    public int getIntrinsicIconSize() {
        return this.mLauncher.getDeviceProfile().appsGrid.getIconSize();
    }

    public void onDropCompleted(View target, DragObject dragObject, boolean success) {
        this.mAppsPagedView.forcelyAnimateReturnPages();
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing() && this.mDragInfo != null) {
            this.mLauncher.getQuickOptionManager().startBounceAnimation();
        }
        Log.d(TAG, "onDropCompleted mRestorePositionOnDrop = " + this.mDragOperator.getRestorePosition());
        if (dragObject.extraDragInfoList == null) {
            onDropCompletedWithOutExtraItems(target, dragObject, success);
        } else {
            onDropCompletedWithExtraItems(target, dragObject);
        }
        this.mDragOperator.dropCompleted();
        this.mDragInfo = null;
    }

    private void onDropCompletedWithOutExtraItems(View target, DragObject dragObject, boolean success) {
        boolean restorePosition = !success;
        if (this.mDragInfo != null) {
            boolean homeMakeClone = false;
            ItemInfo item = dragObject.dragInfo;
            if (!(target instanceof AppsPagedView)) {
                homeMakeClone = item.container == -102;
            }
            restorePosition = (!success || dragObject.cancelled || homeMakeClone) && this.mDragInfo.cell != null;
            if (restorePosition) {
                final View cell = this.mDragInfo.cell;
                ItemInfo cellInfo = (ItemInfo) cell.getTag();
                restoreOverLastItems();
                this.mTarget.rank = cellInfo.rank;
                this.mTarget.screenId = cellInfo.screenId;
                if (this.mReorderAlarm.alarmPending()) {
                    this.mReorderAlarm.cancelAlarm();
                }
                this.mReorderListener.realTimeReorder(this.mEmpty, this.mTarget);
                cellInfo.rank = this.mTarget.rank;
                cellInfo.screenId = this.mTarget.screenId;
                this.mAppsPagedView.addItem(cell, cellInfo);
                long screenId = this.mEmpty.screenId;
                if (((long) this.mAppsPagedView.getNextPage()) != screenId) {
                    this.mAppsPagedView.snapToPage((int) screenId);
                }
                if (cell.getParent() != null) {
                    final CellLayout parent = (CellLayout) cell.getParent().getParent();
                    if (homeMakeClone) {
                        final View view = target;
                        cell.postDelayed(new Runnable() {
                            public void run() {
                                parent.onDropChild(cell);
                                cell.setVisibility(View.VISIBLE);
                                AppsDragController.this.mListener.updateCountBadge(cell, view instanceof AppsPagedView);
                            }
                        }, (long) (this.mLauncher.isAppsStage() ? 720 : 0));
                    } else {
                        if (dragObject.dragView.hasDrawn()) {
                            this.mDragLayer.animateViewIntoPosition(dragObject.dragView, cell, 300, null, this.mAppsPagedView);
                        } else {
                            dragObject.deferDragViewCleanupPostAnimation = false;
                            cell.setVisibility(View.VISIBLE);
                            this.mListener.updateCountBadge(cell, false);
                        }
                        parent.onDropChild(cell);
                    }
                }
                updateBadgeItems(dragObject);
            } else {
                this.mReorderListener.removeEmptyCell(this.mEmpty);
            }
        }
        this.mDragOperator.dropCompletedWithOutExtra(restorePosition);
    }

    private void onDropCompletedWithExtraItems(View target, DragObject dragObject) {
        if (this.mReorderAlarm.alarmPending()) {
            this.mReorderAlarm.cancelAlarm();
        }
        boolean targetIsAppsFolder = (target instanceof FolderView) && ((FolderView) target).getInfo().container == -102;
        boolean targetIsApps = target instanceof AppsPagedView;
        ArrayList<DragObject> extraDragObjects = new ArrayList();
        ArrayList<DragObject> successDragObjects = new ArrayList();
        ArrayList<DragObject> cancelDragObjects = new ArrayList();
        extraDragObjects.add(dragObject);
        if (dragObject.cancelled) {
            cancelDragObjects.add(dragObject);
        } else {
            successDragObjects.add(dragObject);
        }
        Iterator it = dragObject.extraDragInfoList.iterator();
        while (it.hasNext()) {
            DragObject dObject = (DragObject) it.next();
            extraDragObjects.add(dObject);
            if (dObject.cancelled) {
                cancelDragObjects.add(dObject);
            } else {
                successDragObjects.add(dObject);
            }
        }
        this.mDragOperator.removeEmptyCellIfNecessary(this.mEmpty);
        if ((!targetIsAppsFolder && !targetIsApps) || (targetIsApps && successDragObjects.size() == 0)) {
            placeObjectsToOriginalPosition(target, dragObject, extraDragObjects);
        } else if (targetIsAppsFolder || (targetIsApps && cancelDragObjects.size() > 0)) {
            int droppedPosition;
            int indexScreen = (int) this.mAppsPagedView.getIdForScreen(this.mDropToLayout);
            if (this.mTargetCell[0] < 0) {
                droppedPosition = this.mAppsPagedView.getItemCountPageAt(indexScreen) - 1;
            } else {
                droppedPosition = this.mTargetCell[0] + (this.mTargetCell[1] * this.mAppsPagedView.getCellCountX());
            }
            this.mDragOperator.refreshObjectsToPosition(dragObject, indexScreen, droppedPosition, cancelDragObjects);
        }
        this.mDragOperator.updateDirtyItemsToDb();
        updateBadgeItems(dragObject);
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        if (dragObject == null) {
            return 0;
        }
        boolean folder;
        if (dragObject.dragInfo.itemType == 2) {
            folder = true;
        } else {
            folder = false;
        }
        int optionFlags = ((((0 | 1) | 32) | 4096) | 8192) | 4;
        if (folder) {
            return (optionFlags | 8) | 2048;
        }
        optionFlags = (((((optionFlags | 64) | 128) | 256) | 512) | 1024) | 16384;
        optionFlags = 32768 | 30693;
        if (LauncherFeature.supportSetToZeroPage()) {
            return optionFlags | 65536;
        }
        return optionFlags;
    }

    public int getScrollZone() {
        return this.mLauncher.getResources().getDimensionPixelSize(R.dimen.apps_scroll_zone);
    }

    public void scrollLeft() {
        if (this.mAppsPagedView.canDragScroll()) {
            this.mAppsPagedView.scrollLeft();
            FolderView openFolder = this.mLauncher.getOpenFolderView();
            if (openFolder != null) {
                openFolder.completeDragExit();
            }
        }
    }

    public void scrollRight() {
        if (this.mAppsPagedView.canDragScroll()) {
            this.mAppsPagedView.scrollRight();
            FolderView openFolder = this.mLauncher.getOpenFolderView();
            if (openFolder != null) {
                openFolder.completeDragExit();
            }
        }
    }

    public boolean onEnterScrollArea(int x, int y, int direction) {
        if (this.mLauncher.getOpenFolderView() == null) {
            int page = this.mAppsPagedView.getNextPage() + (direction == 0 ? -1 : 1);
            if (page >= 0 && page < this.mAppsPagedView.getChildCount()) {
                this.mInScrollArea = true;
                this.mAppsPagedView.invalidate();
                return true;
            }
        }
        return false;
    }

    public boolean onExitScrollArea() {
        if (!this.mInScrollArea) {
            return false;
        }
        this.mAppsPagedView.invalidate();
        this.mInScrollArea = false;
        return true;
    }

    private void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        this.mInScrollArea = false;
    }

    private CellLayout getCurrentDropLayout() {
        return (CellLayout) this.mAppsPagedView.getChildAt(this.mAppsPagedView.getNextPage());
    }

    private void setCurrentDropLayout(CellLayout layout) {
        if (this.mDragTargetLayout != null) {
            if (layout != null) {
                restoreOverLastItems();
            }
            this.mDragTargetLayout.onDragExit();
        }
        this.mDragTargetLayout = layout;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragEnter();
            this.mReorderListener.setReorderTarget(this.mDragTargetLayout);
            this.mFolderController.setReorderTarget(this.mDragTargetLayout);
        }
        setDragMode(0);
    }

    private void setCurrentDragOverlappingLayout(CellLayout layout) {
        this.mDragOverlappingLayout = layout;
    }

    public void setDragMode(int dragMode) {
        if (dragMode != this.mDragMode) {
            if (dragMode == 0) {
                this.mReorderAlarm.cancelAlarm();
                this.mFolderController.cleanup();
            } else if (dragMode == 2) {
                this.mReorderAlarm.cancelAlarm();
                this.mPrevTarget.rank = -1;
            } else if (dragMode == 1) {
                this.mReorderAlarm.cancelAlarm();
                this.mPrevTarget.rank = -1;
            } else if (dragMode == 3) {
                this.mFolderController.cleanup();
            } else if (dragMode == 4) {
                this.mReorderAlarm.cancelAlarm();
                this.mFolderController.cleanup();
                this.mListener.changeState(0, false);
            }
            this.mDragMode = dragMode;
        }
    }

    public void commit(CellLayout layout) {
    }

    public FolderIconView addFolder(CellLayout layout, IconInfo destInfo) {
        return this.mListener.addFolder(layout, destInfo.screenId, destInfo.cellX, destInfo.cellY, destInfo.rank);
    }

    public boolean canOpenFolder() {
        return this.mListener.getState() == 0;
    }

    public void onExtraObjectDragged(ArrayList<DragObject> extraDragObjects) {
        if (extraDragObjects != null) {
            ArrayList<DragAppIcon> targetIconList = new ArrayList();
            Iterator it = extraDragObjects.iterator();
            while (it.hasNext()) {
                View sourceView = ((DragObject) it.next()).dragView.getSourceView();
                if (sourceView != null) {
                    ItemInfo tag = sourceView.getTag();
                    if (tag instanceof ItemInfo) {
                        ItemInfo item = tag;
                        DragAppIcon targetIcon = new DragAppIcon();
                        targetIcon.rank = item.rank;
                        targetIcon.screenId = item.screenId;
                        targetIcon.cellX = item.cellX;
                        targetIcon.cellY = item.cellY;
                        targetIconList.add(targetIcon);
                    }
                }
            }
            this.mTarget.rank = this.mEmpty.rank;
            this.mTarget.screenId = this.mEmpty.screenId;
            this.mTarget.cellX = this.mEmpty.cellX;
            this.mTarget.cellY = this.mEmpty.cellY;
            this.mReorderListener.removeEmptyCellsAndViews(targetIconList, this.mEmpty, false);
        }
    }

    private void placeObjectsToOriginalPosition(View target, DragObject mainDragObject, ArrayList<DragObject> extraDragObjects) {
        View dragIcon;
        if (this.mDragInfo == null) {
            dragIcon = this.mListener.createItemView(mainDragObject.dragInfo, this.mDropToLayout, null);
        } else {
            dragIcon = this.mDragInfo.cell;
        }
        Collections.sort(extraDragObjects, DRAG_OBJECT_COMPARATOR);
        Iterator it = extraDragObjects.iterator();
        while (it.hasNext()) {
            DragObject dragObject = (DragObject) it.next();
            ItemInfo info = dragObject.dragInfo;
            View view = dragObject.dragView.getSourceView();
            if (dragObject.equals(mainDragObject)) {
                view = dragIcon;
            }
            int indexRank = info.rank;
            int indexScreen = (int) info.screenId;
            info.container = -102;
            this.mDragOperator.makeEmptyCellAndReorderIfNecessary(indexScreen, indexRank);
            this.mAppsPagedView.addItem(view, info);
            if (view.getParent() != null) {
                CellLayout parent = (CellLayout) view.getParent().getParent();
                view.setVisibility(View.VISIBLE);
                parent.onDropChild(view);
                parent.getCellLayoutChildren().measureChild(view);
            }
            if (target instanceof AppsPagedView) {
                this.mDragLayer.animateViewIntoPosition(dragObject.dragView, view, 300, null, this.mAppsPagedView);
            }
        }
        if (!(target instanceof AppsPagedView)) {
            this.mListener.updateCountBadge(dragIcon, false);
        }
    }

    public void onExtraObjectDropCompleted(View target, ArrayList<DragObject> arrayList, ArrayList<DragObject> arrayList2, int fullCnt) {
    }

    public boolean createUserFolderIfNecessary(CellLayout targetLayout, int[] targetCell, View newView, DragObject dragObject, View targetView) {
        this.mFolderController.setReorderTarget(targetLayout);
        return this.mFolderController.onDropCreateUserFolder(null, targetCell, newView, null, dragObject, targetView);
    }

    public void onAdjustDraggedObjectPosition(DragObject dragObject, int startPos, int endPos, int screenId) {
        this.mDragOperator.onAdjustDraggedObjectPosition(dragObject, startPos, endPos, screenId);
    }

    public int getPageIndexForDragView(ItemInfo item) {
        if (item == null) {
            return this.mAppsPagedView.getNextPage();
        }
        int pageIndex = (int) item.screenId;
        if (!this.mListener.isItemInFolder(item)) {
            return pageIndex;
        }
        View folder = this.mListener.getAppsIconByItemId(item.container);
        if (folder != null) {
            return (int) ((FolderInfo) folder.getTag()).screenId;
        }
        return pageIndex;
    }

    public int getDragSourceType() {
        return 1;
    }

    public boolean needDefferToBind(DragManager dragManager) {
        boolean dragFromAppsOrAppsFolder = false;
        if (!this.mDragComplete) {
            DragObject dragObject = dragManager.getDragObject();
            if (dragObject != null && (dragObject.dragSource.getDragSourceType() == 1 || dragObject.dragSource.getDragSourceType() == 4)) {
                dragFromAppsOrAppsFolder = true;
            }
        }
        Log.d(TAG, "needDeferToBind : " + this.mDragComplete + " , " + dragFromAppsOrAppsFolder);
        if (this.mDragComplete || !dragFromAppsOrAppsFolder) {
            return false;
        }
        return true;
    }

    private void createEmptyScreen() {
        if (this.mAppsPagedView.hasExtraEmptyScreen()) {
            this.mAppsPagedView.commitExtraEmptyScreen();
        } else {
            this.mAppsPagedView.createAppsPage();
        }
    }

    private void restoreOverLastItems() {
        if (this.mReorderListener.getExistOverLastItemMoved()) {
            this.mReorderListener.undoOverLastItems();
        }
    }

    private void updateBadgeItems(DragObject dragObject) {
        ArrayList<ItemInfo> items = new ArrayList();
        if (dragObject.dragInfo instanceof ItemInfo) {
            items.add((ItemInfo) dragObject.dragInfo);
        }
        if (dragObject.extraDragInfoList != null) {
            Iterator it = dragObject.extraDragInfoList.iterator();
            while (it.hasNext()) {
                DragObject extra = (DragObject) it.next();
                if (extra.dragInfo instanceof ItemInfo) {
                    items.add((ItemInfo) extra.dragInfo);
                }
            }
        }
        if (!items.isEmpty()) {
            this.mListener.updateBadgeItems(items);
        }
    }

    private void refreshFolderBadge(ItemInfo info) {
        if (info != null) {
            View folder = this.mListener.getAppsIconByItemId(info.container);
            if (folder instanceof FolderIconView) {
                ((FolderIconView) folder).refreshBadge();
            }
        }
    }

    public int getEmptyCount() {
        return 0;
    }
}
