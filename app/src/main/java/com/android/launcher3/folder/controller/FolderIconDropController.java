package com.android.launcher3.folder.controller;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderIconView.FolderRingAnimator;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.home.HotseatDragController;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class FolderIconDropController {
    private static final int FOLDER_CREATION_TIMEOUT = 0;
    private static final int FOLDER_OPEN_TIMEOUT = 1500;
    private static final String TAG = "FolderIconDropController";
    private boolean mAddToExistingFolderOnDrop = false;
    private boolean mCreateUserFolderOnDrop = false;
    private FolderIconView mDragOverFolderIconView = null;
    private final Alarm mFolderCreationAlarm;
    private FolderLock mFolderLock;
    private final Alarm mFolderNSecOpenAlarm;
    private FolderRingAnimator mFolderRingAnimator = null;
    private Launcher mLauncher;
    private CellLayout mLayout;
    private float mMaxDistanceForFolder;
    private DragState mTargetState;

    private class FolderCreationAlarmListener implements OnAlarmListener {
        int cellX;
        int cellY;
        CellLayout layout;

        private FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        public void onAlarm(Alarm alarm) {
            if (FolderIconDropController.this.mFolderRingAnimator != null) {
                FolderIconDropController.this.mFolderRingAnimator.animateToNaturalState();
            }
            FolderIconDropController.this.mFolderRingAnimator = new FolderRingAnimator(null, FolderIconDropController.this.mLayout.getContentIconSize());
            FolderIconDropController.this.mFolderRingAnimator.setCell(this.cellX, this.cellY);
            FolderIconDropController.this.mFolderRingAnimator.setCellLayout(this.layout);
            FolderIconDropController.this.mFolderRingAnimator.animateToAcceptState();
            this.layout.showFolderAccept(FolderIconDropController.this.mFolderRingAnimator);
            this.layout.clearDragOutlines();
            FolderIconDropController.this.cleanupAddToFolder();
            FolderIconDropController.this.mTargetState.setDragMode(1);
        }
    }

    public FolderIconDropController(Context context, DragState dragState) {
        this.mLauncher = (Launcher) context;
        this.mTargetState = dragState;
        this.mFolderNSecOpenAlarm = new Alarm();
        this.mFolderCreationAlarm = new Alarm();
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    public void setMaxDistance(int iconSize) {
        this.mMaxDistanceForFolder = 0.55f * ((float) iconSize);
    }

    public void setReorderTarget(CellLayout layout) {
        cleanup();
        this.mLayout = layout;
    }

    public void onDragEnter() {
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
    }

    public void onDragOver(float[] dragViewCenter, int[] targetCell, DragObject d, CellInfo dragInfo, int dragMode) {
        if (this.mLayout != null) {
            ItemInfo info = d.dragInfo;
            float distance = getDistanceFromCell(dragViewCenter, targetCell);
            View dragOverView = this.mLayout.getChildAt(targetCell[0], targetCell[1]);
            if (!isInvalidDropTarget(dragOverView)) {
                boolean userFolderPending = willCreateUserFolder(info, dragInfo, targetCell, distance, false);
                if (dragMode == 0 && userFolderPending && !this.mFolderCreationAlarm.alarmPending()) {
                    this.mFolderCreationAlarm.setOnAlarmListener(new FolderCreationAlarmListener(this.mLayout, targetCell[0], targetCell[1]));
                    this.mFolderCreationAlarm.setAlarm(0);
                    if (dragOverView instanceof IconView) {
                        String title;
                        if (info instanceof PendingAddShortcutInfo) {
                            title = ((PendingAddShortcutInfo) info).mLabel;
                        } else if (info instanceof FolderInfo) {
                            title = this.mLauncher.getString(R.string.folder_name_format, new Object[]{info.title});
                        } else {
                            title = (String) info.title;
                        }
                        Talk.INSTANCE.say(this.mLauncher.getString(R.string.tts_hover_item_over_other_item, new Object[]{title, ((IconView) dragOverView).getTitle()}) + ", " + this.mLauncher.getString(R.string.tts_release_to_create_folder));
                        return;
                    }
                    return;
                }
                boolean willAddToFolder = willAddToExistingUserFolder(info, targetCell, distance);
                if (willAddToFolder && dragMode == 0) {
                    this.mDragOverFolderIconView = (FolderIconView) dragOverView;
                    this.mDragOverFolderIconView.onDragEnter(info);
                    FolderInfo folderInfo = this.mDragOverFolderIconView.getFolderInfo();
                    if (!(!LauncherFeature.supportFolderNSecOpen() || this.mFolderNSecOpenAlarm.alarmPending() || isAppsAlphabeticViewType(folderInfo) || (d.dragInfo instanceof PendingAddShortcutInfo))) {
                        final FolderIconView folderIconView = this.mDragOverFolderIconView;
                        this.mFolderNSecOpenAlarm.setOnAlarmListener(new OnAlarmListener() {
                            public void onAlarm(Alarm alarm) {
                                FolderIconDropController.this.openFolderOnDragHold(folderIconView);
                            }
                        });
                        this.mFolderNSecOpenAlarm.setAlarm(1500);
                    }
                    this.mLayout.clearDragOutlines();
                    this.mTargetState.setDragMode(2);
                    cleanupFolderCreation();
                    String description = this.mLauncher.getString(R.string.tts_move_to_folder);
                    String folderName = this.mDragOverFolderIconView.getTitle();
                    if (!(folderName == null || folderName.isEmpty())) {
                        description = description + ", " + folderName;
                    }
                    Talk.INSTANCE.say(description);
                    return;
                }
                if (dragMode == 2 && !willAddToFolder) {
                    this.mTargetState.setDragMode(0);
                }
                if (dragMode == 1 && !userFolderPending) {
                    this.mTargetState.setDragMode(0);
                }
            }
        }
    }

    private float getDistanceFromCell(float[] dragViewCenter, int[] targetCell) {
        return this.mLayout.getDistanceFromCell(dragViewCenter[0], dragViewCenter[1], targetCell);
    }

    public void onDragExit(int dragMode) {
        if (dragMode == 1) {
            this.mCreateUserFolderOnDrop = true;
        } else if (dragMode == 2) {
            this.mAddToExistingFolderOnDrop = true;
        }
        if ((this.mTargetState instanceof HotseatDragController) && this.mLayout != null && this.mLayout.getFolderRings().size() > 0) {
            this.mLayout.hideFolderAcceptForcedly();
        }
        cleanup();
    }

    private boolean isInvalidDropTarget(View v) {
        return v == null || ((v.getTag() instanceof IconInfo) && ((IconInfo) v.getTag()).isAppsButton);
    }

    public boolean acceptDrop(float[] dragViewCenter, int[] targetCell, DragObject d, CellInfo dragInfo) {
        if (this.mLayout == null) {
            return false;
        }
        boolean isCreateUserFolder;
        float distance = getDistanceFromCell(dragViewCenter, targetCell);
        if (this.mCreateUserFolderOnDrop && willCreateUserFolder((ItemInfo) d.dragInfo, dragInfo, targetCell, distance, true)) {
            isCreateUserFolder = true;
        } else {
            isCreateUserFolder = false;
        }
        boolean isAddToExistingFolder;
        if (this.mAddToExistingFolderOnDrop && willAddToExistingUserFolder((ItemInfo) d.dragInfo, targetCell, distance)) {
            isAddToExistingFolder = true;
        } else {
            isAddToExistingFolder = false;
        }
        if (isCreateUserFolder || isAddToExistingFolder) {
            return true;
        }
        return false;
    }

    public boolean onDropCreateUserFolder(float[] dragViewCenter, int[] targetCell, View newView, CellLayout originalLayout, DragObject d) {
        return onDropCreateUserFolder(dragViewCenter, targetCell, newView, originalLayout, d, null);
    }

    public boolean onDropCreateUserFolder(float[] dragViewCenter, int[] targetCell, View newView, CellLayout originalLayout, DragObject d, View targetView) {
        if (this.mLayout == null) {
            return false;
        }
        boolean isRecoverModeWithFolderLock = false;
        if (this.mFolderLock != null && this.mFolderLock.isFolderLockEnabled()) {
            isRecoverModeWithFolderLock = this.mFolderLock.getRecoverMode();
            if (this.mFolderLock.getReroderLayout() != null && isRecoverModeWithFolderLock) {
                this.mLayout = this.mFolderLock.getReroderLayout();
            }
        }
        float distance = 0.0f;
        if (dragViewCenter != null) {
            distance = getDistanceFromCell(dragViewCenter, targetCell);
        }
        if (distance > this.mMaxDistanceForFolder) {
            return false;
        }
        if (!this.mCreateUserFolderOnDrop && dragViewCenter != null && !isRecoverModeWithFolderLock) {
            return false;
        }
        if (targetCell == null) {
            return false;
        }
        View v;
        this.mCreateUserFolderOnDrop = false;
        if (targetView == null) {
            v = this.mLayout.getChildAt(targetCell[0], targetCell[1]);
        } else {
            v = targetView;
        }
        if (isInvalidDropTarget(v)) {
            return false;
        }
        boolean aboveIconInfo = v.getTag() instanceof IconInfo;
        boolean willBecomeIconInfo = newView.getTag() instanceof IconInfo;
        DragView dragView = d.dragView;
        IconInfo item = null;
        if ((newView.getTag() instanceof FolderInfo) && this.mLauncher.getMultiSelectManager().acceptDropToFolder()) {
            d.cancelDropFolder = true;
            d.cancelled = true;
            Iterator it = d.extraDragInfoList.iterator();
            while (it.hasNext()) {
                DragObject dragObject = (DragObject) it.next();
                if (dragObject.dragInfo instanceof IconInfo) {
                    if (!(((IconInfo) v.getTag()).container == -102 || dragObject.dragSource == null || (dragObject.dragSource.getDragSourceType() != 1 && dragObject.dragSource.getDragSourceType() != 4))) {
                        item = ((IconInfo) dragObject.dragInfo).makeCloneInfo();
                    }
                    dragView = dragObject.dragView;
                    newView = dragObject.dragView.getSourceView();
                    willBecomeIconInfo = true;
                }
            }
        }
        if (!aboveIconInfo || !willBecomeIconInfo) {
            return false;
        }
        IconInfo sourceInfo;
        MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
        SALogging instance = SALogging.getInstance();
        Launcher launcher = this.mLauncher;
        boolean z = multiSelectMgr != null && multiSelectMgr.isMultiSelectMode();
        instance.insertMoveToAppLog(launcher, z);
        if (item == null) {
            sourceInfo = (IconInfo) newView.getTag();
        } else {
            sourceInfo = item;
        }
        IconInfo destInfo = (IconInfo) v.getTag();
        if (originalLayout != null) {
            originalLayout.removeView(newView);
        }
        Rect folderLocation = new Rect();
        float scale = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation, false);
        this.mLayout.removeView(v);
        final FolderIconView fi = this.mTargetState.addFolder(this.mLayout, destInfo);
        destInfo.cellX = -1;
        destInfo.cellY = -1;
        sourceInfo.cellX = -1;
        sourceInfo.cellY = -1;
        long folderId;
        if (isAppsAlphabeticViewType(destInfo)) {
            folderId = fi.getFolderInfo().id;
            destInfo.container = folderId;
            sourceInfo.container = folderId;
            fi.addItem(destInfo);
            fi.addItem(sourceInfo);
            if (dragView != null) {
                this.mLauncher.getDragLayer().removeAnimation(dragView, d.postAnimationRunnable);
            }
            Runnable postAnimationRunnable = d.postAnimationRunnable;
            int delayToOpenFolder = 0;
            if (postAnimationRunnable != null) {
                postAnimationRunnable.run();
                delayToOpenFolder = 150;
            }
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (FolderIconDropController.this.mTargetState.canOpenFolder()) {
                        FolderIconDropController.this.openFolder(fi, 1);
                    }
                }
            }, (long) delayToOpenFolder);
        } else {
            folderId = fi.getFolderInfo().id;
            destInfo.container = folderId;
            sourceInfo.container = folderId;
            if (dragView != null) {
                final Runnable runnable = d.postAnimationRunnable;
                fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, new Rect(folderLocation), scale, new Runnable() {
                    public void run() {
                        int delayToOpenFolder = 0;
                        if (runnable != null) {
                            runnable.run();
                            delayToOpenFolder = 150;
                        }
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                if (FolderIconDropController.this.mTargetState.canOpenFolder()) {
                                    FolderIconDropController.this.openFolder(fi, 1);
                                }
                            }
                        }, (long) delayToOpenFolder);
                    }
                });
            } else {
                fi.addItem(destInfo);
                fi.addItem(sourceInfo);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (FolderIconDropController.this.mTargetState.canOpenFolder()) {
                            FolderIconDropController.this.openFolder(fi, 1);
                        }
                    }
                }, 650);
            }
        }
        GameHomeManager.getInstance().checkGameAppList(this.mLauncher.getLauncherModel().getAppsModel().getAllAppItemInApps());
        GameHomeManager.getInstance().startGameHUN(this.mLauncher, destInfo, sourceInfo);
        if (d.extraDragInfoList != null) {
            onDropExtraObjects(fi, new Rect(folderLocation), d.extraDragInfoList, dragView);
            SALogging.getInstance().insertMoveToAppLog(this.mLauncher, true);
        } else {
            Talk.INSTANCE.say(this.mLauncher.getString(R.string.tts_folder_created));
        }
        return true;
    }

    public boolean onDropAddToExistingFolder(float[] dragViewCenter, int[] targetCell, DragObject d) {
        if (this.mLayout == null) {
            return false;
        }
        boolean isRecoverModeWithFolderLock = false;
        if (this.mFolderLock != null && this.mFolderLock.isFolderLockEnabled()) {
            isRecoverModeWithFolderLock = this.mFolderLock.getRecoverMode();
            if (this.mFolderLock.getReroderLayout() != null && isRecoverModeWithFolderLock) {
                this.mLayout = this.mFolderLock.getReroderLayout();
            }
        }
        float distance = 0.0f;
        if (dragViewCenter != null) {
            distance = getDistanceFromCell(dragViewCenter, targetCell);
        }
        if (distance > this.mMaxDistanceForFolder) {
            return false;
        }
        if (!this.mAddToExistingFolderOnDrop && dragViewCenter != null && !isRecoverModeWithFolderLock) {
            return false;
        }
        if (targetCell == null) {
            return false;
        }
        this.mAddToExistingFolderOnDrop = false;
        View dropOverView = this.mLayout.getChildAt(targetCell[0], targetCell[1]);
        if (dropOverView instanceof FolderIconView) {
            FolderIconView fi = (FolderIconView) dropOverView;
            if (d.dragInfo instanceof FolderInfo) {
                d.cancelDropFolder = true;
                d.cancelled = true;
                if (d.extraDragInfoList != null) {
                    onDropExtraObjects(fi, null, d.extraDragInfoList, null);
                }
                return true;
            } else if (fi.acceptDrop(d.dragInfo)) {
                ItemInfo info = (ItemInfo) d.dragInfo;
                if (!(this.mFolderLock == null || !this.mFolderLock.isFolderLockEnabled() || isRecoverModeWithFolderLock || !fi.getFolderInfo().isLocked() || fi.getFolderInfo().isLockedFolderOpenedOnce() || !(info instanceof IconInfo) || info.container == ((long) dropOverView.getId()))) {
                    if (this.mFolderLock.needPopupConfirm(fi.getFolderInfo())) {
                        this.mFolderLock.setBackupInfo(d, dropOverView);
                        this.mFolderLock.startLockVerifyActivity(info);
                    } else {
                        this.mFolderLock.lockItem(info);
                    }
                    if (d.extraDragInfoList != null) {
                        SALogging.getInstance().insertAddToLockedFolderLog(this.mLauncher, true);
                    }
                    if (!(d.dragInfo instanceof FolderInfo)) {
                        SALogging.getInstance().insertAddToLockedFolderLog(this.mLauncher, false);
                    }
                }
                fi.onDrop(d);
                if (d.extraDragInfoList != null) {
                    onDropExtraObjects(fi, null, d.extraDragInfoList, null);
                }
                if (!(d.dragInfo instanceof FolderInfo)) {
                    MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
                    SALogging instance = SALogging.getInstance();
                    Launcher launcher = this.mLauncher;
                    boolean z = multiSelectMgr != null && multiSelectMgr.isMultiSelectMode();
                    instance.insertAddToFolderLog(launcher, z);
                }
                return true;
            }
        }
        return false;
    }

    private void onDropExtraObjects(FolderIconView targetFolderIcon, Rect dstRect, ArrayList<DragObject> extraDragObjects, View dragView) {
        ArrayList<DragObject> dragObjects = new ArrayList();
        Iterator it = extraDragObjects.iterator();
        while (it.hasNext()) {
            DragObject d = (DragObject) it.next();
            if (d.dragView != dragView) {
                if (d.dragInfo instanceof FolderInfo) {
                    d.cancelled = true;
                    d.cancelDropFolder = true;
                } else {
                    dragObjects.add(d);
                }
            }
        }
        targetFolderIcon.onDrop(dragObjects, dstRect == null ? null : new Rect(dstRect));
    }

    public boolean willCreateUserFolder(ItemInfo info, CellInfo dragInfo, int[] targetCell, float distance, boolean considerTimeout) {
        boolean z = true;
        if (this.mLayout == null || distance > this.mMaxDistanceForFolder) {
            return false;
        }
        View dropOverView = this.mLayout.getChildAt(targetCell[0], targetCell[1]);
        if (dropOverView != null) {
            LayoutParams lp = (LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && !(lp.tmpCellX == lp.cellX && lp.tmpCellY == lp.cellY)) {
                return false;
            }
        }
        boolean hasntMoved = false;
        if (dragInfo != null) {
            if (dropOverView == dragInfo.cell) {
                hasntMoved = true;
            } else {
                hasntMoved = false;
            }
        }
        if (dropOverView == null || hasntMoved) {
            return false;
        }
        if (considerTimeout && !this.mCreateUserFolderOnDrop) {
            return false;
        }
        boolean aboveShortcut = dropOverView.getTag() instanceof IconInfo;
        boolean willBecomeShortcut;
        if (info.itemType == 0 || info.itemType == 1 || info.itemType == 6 || info.itemType == 7 || this.mLauncher.getMultiSelectManager().acceptDropToFolder()) {
            willBecomeShortcut = true;
        } else {
            willBecomeShortcut = false;
        }
        if (!(aboveShortcut && willBecomeShortcut)) {
            z = false;
        }
        return z;
    }

    public boolean willAddToExistingUserFolder(ItemInfo dragInfo, int[] targetCell, float distance) {
        if (this.mLayout == null || distance > this.mMaxDistanceForFolder) {
            return false;
        }
        View dropOverView = this.mLayout.getChildAt(targetCell[0], targetCell[1]);
        if (dropOverView != null) {
            LayoutParams lp = (LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && !(lp.tmpCellX == lp.cellX && lp.tmpCellY == lp.cellY)) {
                return false;
            }
        }
        if ((dropOverView instanceof FolderIconView) && ((FolderIconView) dropOverView).acceptDrop(dragInfo)) {
            return true;
        }
        return false;
    }

    public void cleanup() {
        cleanupFolderNSecOpen();
        cleanupFolderCreation();
        cleanupAddToFolder();
    }

    private void cleanupFolderNSecOpen() {
        if (this.mFolderNSecOpenAlarm.alarmPending()) {
            this.mFolderNSecOpenAlarm.setOnAlarmListener(null);
            this.mFolderNSecOpenAlarm.cancelAlarm();
        }
    }

    private void cleanupFolderCreation() {
        if (this.mFolderRingAnimator != null) {
            this.mFolderRingAnimator.animateToNaturalState();
            this.mFolderRingAnimator = null;
        }
        if (this.mFolderCreationAlarm.alarmPending()) {
            this.mFolderCreationAlarm.setOnAlarmListener(null);
            this.mFolderCreationAlarm.cancelAlarm();
        }
    }

    private void cleanupAddToFolder() {
        if (this.mDragOverFolderIconView != null) {
            this.mDragOverFolderIconView.onDragExit();
            this.mDragOverFolderIconView = null;
        }
    }

    private void openFolderOnDragHold(FolderIconView targetFolder) {
        if (LauncherFeature.supportFolderNSecOpen() && targetFolder != null) {
            if (!LauncherFeature.supportFolderLock() || !targetFolder.getFolderInfo().isLocked() || targetFolder.getFolderInfo().isLockedFolderOpenedOnce()) {
                if (targetFolder.getFolderView().isAllIconViewInflated()) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_FOLDER_N_SEC_OPEN, null, -1, false);
                    this.mTargetState.setDragMode(4);
                    targetFolder.getFolderView().setSuppressFolderCloseOnce();
                    openFolder(targetFolder, 2);
                    return;
                }
                Log.w(TAG, "openFolderOnDragHold : all items are not bound yet");
            }
        }
    }

    private void openFolder(FolderIconView targetFolder, int toInternalState) {
        if (targetFolder != null) {
            StageEntry data = new StageEntry();
            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, targetFolder);
            data.setInternalStateTo(toInternalState);
            this.mLauncher.getStageManager().startStage(5, data);
        }
    }

    private boolean isAppsAlphabeticViewType(ItemInfo icon) {
        boolean isApps;
        if (icon.container == -102) {
            isApps = true;
        } else {
            isApps = false;
        }
        boolean isAlphabeticViewType = false;
        if (ViewType.valueOf(AppsController.getViewTypeFromSharedPreference(this.mLauncher)) == ViewType.ALPHABETIC_GRID) {
            isAlphabeticViewType = true;
        }
        return isApps && isAlphabeticViewType;
    }
}
