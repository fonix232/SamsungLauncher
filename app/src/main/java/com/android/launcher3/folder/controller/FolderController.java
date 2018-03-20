package com.android.launcher3.folder.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.PagedView.PageScrollListener;
import com.android.launcher3.common.dialog.DisableAppConfirmationDialog;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.multiselect.MultiSelectManager.MultiSelectListener;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.folder.FolderTransitionAnimation;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderBgView;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderPagedView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;

public class FolderController extends Stage implements MultiSelectListener {
    private static final int FOLDER_CLOSE_BACK = 1;
    private static final int FOLDER_CLOSE_HOME = 3;
    private static final int FOLDER_CLOSE_NONE = 0;
    private static final int FOLDER_CLOSE_OTHER = 4;
    public static final int FOLDER_CLOSE_OUTSIDE = 2;
    public static final String KEY_FOLDER_ICON_APPSEARCHED = "KEY_FOLDER_ICON_APPSEARCHED";
    public static final String KEY_FOLDER_ICON_VIEW = "KEY_FOLDER_ICON_VIEW";
    public static final int STATE_DRAG = 2;
    public static final int STATE_NONE = 0;
    public static final int STATE_NORMAL = 1;
    public static final int STATE_SELECT = 3;
    private static final String TAG = "FolderController";
    private float mBgBlurAmount;
    private float mBgDrakenAlpha;
    private DragLayer mDragLayer;
    private DragManager mDragMgr;
    private FolderBgView mFolderBgView;
    private int mFolderCloseReason = 0;
    private int mHelpTextContainerHeight;
    private boolean mIsNeedToUpdateFolderIconView = false;
    private MultiSelectManager mMultiSelectManager;
    private IconInfo mSearchedAppInfo;
    private int mState = 1;
    private int mStateTransitionDuration;
    private FolderIconView mTargetFolderIconView;
    private FolderTransitionAnimation mTransitionAnimation;

    public void setup() {
        this.mTransitionAnimation = new FolderTransitionAnimation(this.mLauncher);
        this.mBgBlurAmount = ((float) this.mLauncher.getResources().getInteger(R.integer.config_folderBgBlur)) / 100.0f;
        this.mBgDrakenAlpha = ((float) this.mLauncher.getResources().getInteger(R.integer.config_folderBgDarken)) / 100.0f;
        this.mStateTransitionDuration = this.mLauncher.getResources().getInteger(R.integer.config_folderEditTransitionDuration);
        this.mFolderBgView = (FolderBgView) View.inflate(this.mLauncher, R.layout.folder_bg, null);
        LauncherAppState.getInstance().getLauncherProxy().setFolderProxyCallbacks(new FolderProxyCallbacksImpl(this));
    }

    public void initStageView() {
        Log.v(TAG, "initStageView()");
        this.mDragLayer = this.mLauncher.getDragLayer();
        this.mDragMgr = this.mLauncher.getDragMgr();
        if (LauncherFeature.supportMultiSelect()) {
            this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
            if (this.mMultiSelectManager != null) {
                this.mMultiSelectManager.addMultiSelectCallbacks(this);
            }
        }
        super.initStageView();
    }

    protected Animator onStageEnter(StageEntry data) {
        Log.v(TAG, "onStageEnter()");
        Animator enterAnim = null;
        if (data != null) {
            int fromViewMode = data.fromStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            FolderIconView folderIconView = (FolderIconView) data.getExtras(KEY_FOLDER_ICON_VIEW);
            this.mSearchedAppInfo = (IconInfo) data.getExtras(KEY_FOLDER_ICON_APPSEARCHED);
            if (folderIconView != null) {
                this.mTargetFolderIconView = folderIconView;
            }
            FolderView folder = null;
            if (this.mTargetFolderIconView != null) {
                folder = this.mTargetFolderIconView.getFolderView();
            }
            if (folder != null) {
                if (fromViewMode != 6) {
                    this.mIsNeedToUpdateFolderIconView = false;
                }
                folder.bindController(this);
                layerViews.put(folder, Integer.valueOf(1));
                if (fromViewMode == 1 || fromViewMode == 2) {
                    onOpenFolder(folder);
                    folder.prepareOpen();
                    enterAnim = this.mTransitionAnimation.getEnterFromHomeOrAppsAnimation(folder, this.mTargetFolderIconView);
                    folder.onOpen(enterAnim);
                    int toState = getAdjustedInternalState(data.getInternalStateTo());
                    if (toState == 2) {
                        enterDragState(false);
                    }
                    this.mState = toState;
                } else if (fromViewMode == 6) {
                    if (!folder.getInfo().opened) {
                        this.mState = getAdjustedInternalState(data.getInternalStateTo());
                        onOpenFolder(folder);
                        folder.prepareOpen();
                        folder.onOpen(null);
                    }
                    ArrayList<IconInfo> addToFolderItems = (ArrayList) data.getExtras(AppsPickerController.KEY_SELECTED_ITEMS);
                    if (addToFolderItems != null && addToFolderItems.size() > 0) {
                        this.mTargetFolderIconView.addItems(addToFolderItems);
                        if (LauncherFeature.supportFolderLock()) {
                            FolderLock folderLock = FolderLock.getInstance();
                            if (folderLock != null && folder.getInfo().isLocked()) {
                                folderLock.lockFolderAfterAdd(folder.getInfo());
                            }
                        }
                    }
                    enterAnim = this.mTransitionAnimation.getEnterFromFolderAddAppsAnimation(folder, addToFolderItems);
                    if (this.mTargetFolderIconView.getFolderInfo().container == -100) {
                        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(3);
                    } else if (this.mTargetFolderIconView.getFolderInfo().container == -102) {
                        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(4);
                    }
                } else if (fromViewMode == 3) {
                    enterAnim = this.mTransitionAnimation.getEnterFromWidgetAnimation(folder);
                }
                if (fromViewMode == 1) {
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(3);
                } else if (fromViewMode == 2) {
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(4);
                }
            }
        }
        prepareFolderBgView();
        addListenerForEnterAnimation(enterAnim);
        this.mFolderCloseReason = 0;
        return enterAnim;
    }

    protected Animator onStageExit(StageEntry data) {
        Log.v(TAG, "onStageExit()");
        Utilities.closeDialog(this.mLauncher);
        Animator exitAnim = null;
        if (data != null) {
            int toViewMode = data.toStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            FolderView folder = this.mTargetFolderIconView != null ? this.mTargetFolderIconView.getFolderView() : null;
            this.mSearchedAppInfo = null;
            if (folder != null) {
                layerViews.put(folder, Integer.valueOf(1));
                boolean folderIconViewRemoved = this.mTargetFolderIconView.getParent() == null || this.mTargetFolderIconView.getParent().getParent() == null || this.mTargetFolderIconView.getParent().getParent().getParent() == null;
                if (!folderIconViewRemoved) {
                    data.putExtras(KEY_FOLDER_ICON_VIEW, this.mTargetFolderIconView);
                }
                if (toViewMode == 1 || toViewMode == 2) {
                    String screenID;
                    onCloseFolder(folder);
                    updateFolderIconView(toViewMode, folderIconViewRemoved);
                    if (data.stageCountOnFinishAllStage > 2 || data.broughtToHome) {
                        this.mFolderCloseReason = ((Integer) data.getExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, Integer.valueOf(0))).intValue() > 0 ? 4 : 3;
                        folder.onClose(null);
                    } else {
                        exitAnim = this.mTransitionAnimation.getExitToHomeOrAppsAnimation(folder, this.mTargetFolderIconView);
                        folder.onClose(exitAnim);
                    }
                    if (this.mFolderCloseReason == 0) {
                        this.mFolderCloseReason = 4;
                    }
                    Resources res = this.mLauncher.getResources();
                    if (folder.getInfo().isContainApps()) {
                        screenID = res.getString(R.string.screen_AppsFolder_Primary);
                    } else {
                        screenID = res.getString(R.string.screen_HomeFolder_Primary);
                    }
                    SALogging.getInstance().insertEventLog(screenID, res.getString(R.string.event_FolderClose), String.valueOf(this.mFolderCloseReason));
                    final FolderView closedFolder = folder;
                    if (exitAnim != null) {
                        exitAnim.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                Talk.INSTANCE.say((int) R.string.folder_closed);
                                FolderController.this.onCloseComplete(closedFolder);
                            }
                        });
                    } else {
                        this.mTargetFolderIconView.setVisibility(View.VISIBLE);
                        onCloseComplete(closedFolder);
                    }
                } else if (toViewMode == 6) {
                    exitAnim = this.mTransitionAnimation.getExitToFolderAddAppsAnimation(folder);
                } else if (toViewMode == 3) {
                    exitAnim = this.mTransitionAnimation.getExitToWidgetAnimation(folder);
                }
            }
            showFolderBgView(false, true);
        }
        return exitAnim;
    }

    private void updateFolderIconView(int toViewMode, boolean folderIconViewRemoved) {
        if (toViewMode == 2 && !folderIconViewRemoved && this.mIsNeedToUpdateFolderIconView) {
            this.mTargetFolderIconView.applyStyle();
        }
    }

    private void prepareFolderBgView() {
        if (this.mFolderBgView != null) {
            this.mFolderBgView.setHelpTextColor(isWhiteBg());
            if (this.mState != 2) {
                showFolderBgView(false, true);
            }
        }
    }

    private void addListenerForEnterAnimation(Animator enterAnim) {
        if (Talk.INSTANCE.isAccessibilityEnabled() && !TestHelper.isRoboUnitTest() && enterAnim != null) {
            enterAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    Talk.INSTANCE.say(FolderController.this.mLauncher.getResources().getString(R.string.folder_opened) + " " + FolderController.this.mTargetFolderIconView.getFolderView().getContent().getPageDescription());
                }
            });
        }
    }

    protected Animator onStageEnterByTray() {
        return null;
    }

    protected Animator onStageExitByTray() {
        return null;
    }

    public void onConfigurationChangedIfNeeded() {
        this.mHelpTextContainerHeight = 0;
        if (this.mTargetFolderIconView != null) {
            this.mTargetFolderIconView.getFolderView().onConfigurationChangedIfNeeded();
            this.mIsNeedToUpdateFolderIconView = true;
        }
        if (this.mDragMgr.isQuickOptionShowing()) {
            this.mDragMgr.removeQuickOptionView();
        }
    }

    public void onChangeColorForBg(boolean whiteBg) {
        if (this.mTargetFolderIconView != null) {
            this.mTargetFolderIconView.getFolderView().setFolderContentColor();
        }
        if (this.mFolderBgView != null) {
            this.mFolderBgView.setHelpTextColor(isWhiteBg());
        }
    }

    protected Animator switchInternalState(StageEntry data) {
        int fromState = getAdjustedInternalState(data.getInternalStateFrom());
        int toState = getAdjustedInternalState(data.getInternalStateTo());
        this.mState = toState;
        boolean animated = data.enableAnimation;
        Animator stateChangeAnim = null;
        FolderView folder = null;
        if (this.mTargetFolderIconView != null) {
            folder = this.mTargetFolderIconView.getFolderView();
            if (toState == 2 || toState == 3) {
                folder.hideAddButton(true);
                folder.setCrosshairsVisibility(true);
                if (toState == 2) {
                    folder.showHintPages();
                    folder.setDragInProgress(true);
                }
            } else {
                folder.showAddButton(true);
                folder.hideHintPages();
                folder.setCrosshairsVisibility(false);
            }
            if (folder.getInfo().opened) {
                boolean z;
                if (toState == 2) {
                    z = true;
                } else {
                    z = false;
                }
                showFolderBgView(z, false);
            }
        }
        if (fromState == 0 || fromState == 1) {
            if (toState == 2) {
                return this.mTransitionAnimation.getDragAnimation(animated, folder, true);
            }
            if (toState != 3 || this.mMultiSelectManager == null || !this.mMultiSelectManager.isMultiSelectMode()) {
                return null;
            }
            View panel = this.mMultiSelectManager.getMultiSelectPanel();
            if (panel == null || panel.getVisibility() == 0) {
                return null;
            }
            this.mMultiSelectManager.showMultiSelectPanel(true, true);
            return null;
        } else if (fromState == 2) {
            if (toState == 1) {
                return this.mTransitionAnimation.getDragAnimation(animated, folder, false);
            }
            return null;
        } else if (fromState != 3) {
            return null;
        } else {
            if (toState == 1) {
                stateChangeAnim = null;
            } else if (toState == 2) {
                stateChangeAnim = this.mTransitionAnimation.getDragAnimation(animated, folder, true);
            }
            if ((!LauncherFeature.supportFolderSelect() && !this.mLauncher.isFolderStage()) || this.mMultiSelectManager == null || !this.mMultiSelectManager.isMultiSelectMode()) {
                return stateChangeAnim;
            }
            this.mMultiSelectManager.showMultiSelectPanel(false, true);
            this.mLauncher.onChangeSelectMode(false, true);
            return stateChangeAnim;
        }
    }

    private int getAdjustedInternalState(int value) {
        return value == 0 ? this.mState : value;
    }

    protected void onStageMovingToInitial(StageEntry data) {
        if (isValidFolder()) {
            FolderView openFolder = this.mTargetFolderIconView.getFolderView();
            onCloseFolder(openFolder);
            detachFolderFromDragLayer(openFolder);
            openFolder.onClose(null);
            this.mTargetFolderIconView.setVisibility(View.VISIBLE);
            this.mTargetFolderIconView = null;
        }
    }

    public View getContainerView() {
        if (isValidFolder()) {
            return this.mTargetFolderIconView.getFolderView();
        }
        return null;
    }

    public void onPauseActivity() {
        if (isValidFolder() && this.mState == 3) {
            if (this.mMultiSelectManager != null && this.mMultiSelectManager.isShowingHelpDialog()) {
                this.mMultiSelectManager.hideHelpDialog(false);
            }
            enterNormalState(false);
        }
    }

    public void onDestroyActivity() {
        this.mDragLayer = null;
        this.mDragMgr = null;
        if (isValidFolder()) {
            this.mTargetFolderIconView.getFolderInfo().opened = false;
        }
        FolderIconView.release();
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.removeMultiSelectCallbacks(this);
        }
    }

    protected boolean onBackPressed() {
        if (isValidFolder()) {
            FolderView openFolder = this.mTargetFolderIconView.getFolderView();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
                if (openFolder.isInTouchMode()) {
                    return true;
                }
            }
            if (this.mState == 3) {
                enterNormalState(true);
                return true;
            } else if (this.mFolderCloseReason == 0) {
                this.mFolderCloseReason = 1;
            }
        }
        return false;
    }

    public int getState() {
        return this.mState;
    }

    public FolderBgView getFolderBgView() {
        return this.mFolderBgView;
    }

    public void showFolderBgView(boolean tobeShown, boolean forced) {
        if (this.mFolderBgView != null) {
            boolean withLighting = forced || this.mLauncher.isFolderStage();
            if (!tobeShown) {
                this.mFolderBgView.showHelpForEdit(false, this.mStateTransitionDuration, withLighting);
            } else if (setupFolderBgLayout()) {
                this.mFolderBgView.showHelpForEdit(true, this.mStateTransitionDuration, withLighting);
            } else {
                Log.w(TAG, "showFolderBgView : layouting is not completed yet");
            }
        }
    }

    private boolean isValidFolder() {
        if (this.mTargetFolderIconView == null) {
            return false;
        }
        if (this.mTargetFolderIconView.getFolderView() != null) {
            return true;
        }
        Log.e(TAG, "OpenFolder is null");
        return false;
    }

    private void callRefreshLiveIcon() {
        if (isValidFolder()) {
            FolderPagedView folderPagedView = this.mTargetFolderIconView.getFolderView().getContent();
            if (folderPagedView != null) {
                folderPagedView.callRefreshLiveIcon();
            }
        }
    }

    private void onOpenFolder(FolderView folder) {
        callRefreshLiveIcon();
        folder.getInfo().opened = true;
        ((LayoutParams) this.mTargetFolderIconView.getLayoutParams()).canReorder = false;
        if (folder.getParent() == null) {
            attachFolderToDragLayer(folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" + folder.getParent() + ").");
        }
        folder.sendAccessibilityEvent(32);
        this.mDragLayer.sendAccessibilityEvent(2048);
    }

    private void onCloseFolder(FolderView folder) {
        folder.getInfo().opened = false;
        ((LayoutParams) this.mTargetFolderIconView.getLayoutParams()).canReorder = true;
    }

    private void onCloseComplete(FolderView closedFolder) {
        enterNormalState(false);
        updateCheckBox(false);
        if (closedFolder != null) {
            refreshBadgeOnClosedFolder();
            detachFolderFromDragLayer(closedFolder);
            closedFolder.bindController(null);
        }
        this.mTargetFolderIconView = null;
        if (this.mDragLayer != null) {
            this.mDragLayer.sendAccessibilityEvent(32);
        }
    }

    private void attachFolderToDragLayer(FolderView folder) {
        if (this.mFolderBgView != null) {
            ViewGroup parent = (ViewGroup) this.mFolderBgView.getParent();
            if (parent != null) {
                parent.removeView(this.mFolderBgView);
            }
            this.mFolderBgView.showHelpForEdit(false, 0, false);
            this.mDragLayer.addView(this.mFolderBgView, new DragLayer.LayoutParams(-1, -1));
        }
        this.mDragLayer.addView(folder);
        this.mDragMgr.addDropTarget(folder);
    }

    private void detachFolderFromDragLayer(FolderView folder) {
        DragLayer parent;
        if (folder != null) {
            parent = (DragLayer) folder.getParent();
            if (parent != null) {
                parent.removeView(folder);
            }
            if (this.mDragMgr != null) {
                this.mDragMgr.removeDropTarget(folder);
            }
        }
        if (this.mFolderBgView != null) {
            parent = (DragLayer) this.mFolderBgView.getParent();
            if (parent != null) {
                parent.removeView(this.mFolderBgView);
            }
        }
    }

    private boolean setupFolderBgLayout() {
        if (this.mHelpTextContainerHeight > 0) {
            return true;
        }
        if (this.mFolderBgView == null || this.mFolderBgView.getParent() == null || !isValidFolder()) {
            return false;
        }
        int[] folderCoord = new int[2];
        FolderView folder = this.mTargetFolderIconView.getFolderView();
        float scale = this.mDragLayer.getDescendantCoordRelativeToSelf(folder, folderCoord);
        if (folderCoord[1] <= 0 || scale != 1.0f) {
            return false;
        }
        int helpTextContainerHeight = (folderCoord[1] + folder.getHeader().getHeight()) - this.mLauncher.getResources().getDimensionPixelSize(R.dimen.open_folder_edit_help_text_margin_from_folder_content);
        if (helpTextContainerHeight <= 0) {
            return false;
        }
        this.mHelpTextContainerHeight = helpTextContainerHeight;
        this.mFolderBgView.setHelpTextContainerHeightAndGravity(this.mHelpTextContainerHeight, 80);
        return true;
    }

    protected int getInternalState() {
        return this.mState;
    }

    protected boolean supportStatusBarForState(int internalState) {
        if (internalState == 2 || internalState == 3) {
            return false;
        }
        return true;
    }

    protected boolean supportNavigationBarForState(int internalState) {
        return internalState != 2;
    }

    protected float getBackgroundBlurAmountForState(int internalState) {
        return this.mDragLayer.getBackgroundImageAlpha() > 0.0f ? BlurUtils.getMaxBlurAmount() : this.mBgBlurAmount;
    }

    protected float getBackgroundDimAlphaForState(int internalState) {
        return this.mDragLayer.getBackgroundImageAlpha() > 0.0f ? 0.0f : this.mBgDrakenAlpha;
    }

    protected float getBackgroundImageAlphaForState(int internalState) {
        return -1.0f;
    }

    public boolean finishOnTouchOutSide() {
        if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
            Toast.makeText(this.mLauncher, R.string.multi_select_apps_deselected, 0).show();
        }
        if (isValidFolder()) {
            this.mFolderCloseReason = 2;
        }
        return true;
    }

    public void setFolderCloseReasonOnTouchOutside() {
        this.mFolderCloseReason = 2;
    }

    protected boolean isRestorable() {
        return false;
    }

    private void enterFolderState(int state, boolean animated) {
        if (this.mState != state) {
            StageEntry data = new StageEntry();
            data.enableAnimation = animated;
            data.setInternalStateFrom(this.mState);
            data.setInternalStateTo(state);
            getStageManager().switchInternalState(this, data);
        }
    }

    public void enterNormalState(boolean animated) {
        enterFolderState(1, animated);
    }

    public void enterDragState(boolean animated) {
        enterFolderState(2, animated);
    }

    public void enterSelectState(boolean animated) {
        enterFolderState(3, animated);
    }

    public void onChangeSelectMode(boolean enter, boolean animated) {
        if (this.mLauncher.isFolderStage()) {
            if (enter) {
                enterSelectState(animated);
                if (isValidFolder() && this.mTargetFolderIconView.getFolderView().getContent() != null) {
                    Talk.INSTANCE.postSay(this.mLauncher.getResources().getString(R.string.tts_changed_to_folder_edit_mode) + " " + String.format(this.mLauncher.getResources().getString(R.string.default_scroll_format), new Object[]{Integer.valueOf(folderPagedView.getCurrentPage() + 1), Integer.valueOf(folderPagedView.getPageCount())}));
                }
            } else {
                this.mMultiSelectManager.clearCheckedApps();
            }
            updateCheckBox(enter);
        }
    }

    public void updateCheckBox(boolean visible) {
        if (isValidFolder()) {
            this.mTargetFolderIconView.getFolderView().getContent().updateCheckBox(visible);
        }
    }

    public void onCheckedChanged(View view, boolean isChecked) {
        if (isChecked) {
            this.mMultiSelectManager.addCheckedApp(view, this.mTargetFolderIconView.getFolderView());
        } else {
            this.mMultiSelectManager.removeCheckedApp(view);
        }
        if (!LauncherFeature.supportFolderSelect()) {
            refreshCountBadge(view);
        }
    }

    public DragSource getDragSourceForLongKey() {
        return (DragSource) getContainerView();
    }

    public void onClickMultiSelectPanel(int id) {
        if (this.mLauncher.isFolderStage()) {
            switch (id) {
                case 0:
                    enterNormalState(true);
                    return;
                case 1:
                    enterNormalState(true);
                    return;
                case 2:
                    closeFolder();
                    return;
                default:
                    return;
            }
        }
    }

    public void closeFolderIfLackItem() {
        if (this.mLauncher.isFolderStage() && isValidFolder() && this.mTargetFolderIconView.getFolderView().getItemCount() <= 1) {
            closeFolder();
            DisableAppConfirmationDialog.showIfNeeded(this.mLauncher.getFragmentManager());
        }
    }

    private void closeFolder() {
        if (this.mLauncher.isFolderStage()) {
            getStageManager().finishStage(5, null);
        }
    }

    private void refreshCountBadge(View view) {
        if (this.mTargetFolderIconView != null) {
            this.mTargetFolderIconView.refreshCountBadge(this.mMultiSelectManager.getCheckedItemCountInFolder(((ItemInfo) view.getTag()).container));
        }
    }

    private void refreshBadgeOnClosedFolder() {
        if (this.mTargetFolderIconView != null) {
            this.mTargetFolderIconView.refreshBadge();
        }
    }

    public IconInfo getSearchedAppInfo() {
        return this.mSearchedAppInfo;
    }

    public void setSearchedAppInfo(IconInfo info) {
        this.mSearchedAppInfo = info;
    }

    private boolean isWhiteBg() {
        return WhiteBgManager.isWhiteBg() && this.mLauncher.getDragLayer().getBackgroundImageAlpha() <= 0.0f;
    }

    public void onSetPageScrollListener(PageScrollListener listener) {
        if (isValidFolder()) {
            this.mTargetFolderIconView.getFolderView().getContent().setPageScrollListener(listener);
        }
    }

    Launcher getLauncher() {
        return this.mLauncher;
    }

    FolderIconView getTargetFolderIconView() {
        return this.mTargetFolderIconView;
    }

    protected int getSupportSoftInputParam(Window window) {
        return window.getAttributes().softInputMode & -17;
    }

    public boolean searchBarHasFocus() {
        FolderView folder = null;
        if (this.mTargetFolderIconView != null) {
            folder = this.mTargetFolderIconView.getFolderView();
        }
        return (folder == null || folder.getFolderNameView() == null || !folder.getFolderNameView().hasFocus()) ? false : true;
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        if (LauncherFeature.supportNavigationBar() && this.mNavigationBarPosition != Utilities.getNavigationBarPositon()) {
            this.mNavigationBarPosition = Utilities.getNavigationBarPositon();
            if (isValidFolder()) {
                this.mTargetFolderIconView.getFolderView().centerAboutIcon();
            }
        }
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.updateMultiSelectPanelLayout();
        }
    }

    public void setIsNeedToUpdateFolderIconView(boolean isNeedToUpdateFolderIconView) {
        this.mIsNeedToUpdateFolderIconView = isNeedToUpdateFolderIconView;
    }
}
