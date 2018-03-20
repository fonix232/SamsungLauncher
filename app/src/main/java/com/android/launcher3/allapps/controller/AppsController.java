package com.android.launcher3.allapps.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.AppSearchSettingActivity;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel.OnBadgeBindingCompletedListener;
import com.android.launcher3.LauncherModel.OnNotificationPreviewBindingListener;
import com.android.launcher3.LauncherModel.OnRefreshLiveIconListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AppsScreenGridPanel;
import com.android.launcher3.allapps.AppsTransitionAnimation;
import com.android.launcher3.allapps.AppsUtils;
import com.android.launcher3.allapps.AppsViewTypeDialog;
import com.android.launcher3.allapps.AppsViewTypeDialog.OnViewTypeChagnedListener;
import com.android.launcher3.allapps.DragAppIcon;
import com.android.launcher3.allapps.OrganizeAppsConfirmDialog;
import com.android.launcher3.allapps.model.AppsModel;
import com.android.launcher3.allapps.view.AppsContainer;
import com.android.launcher3.allapps.view.AppsPagedView;
import com.android.launcher3.allapps.view.AppsPagedView.Listener;
import com.android.launcher3.allapps.view.AppsSearch;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.ItemOperator;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.base.view.PagedView.PageScrollListener;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.drawable.PreloadIconDrawable;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.multiselect.MultiSelectManager.MultiSelectListener;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.tray.TrayManager.TrayEvent;
import com.android.launcher3.common.tray.TrayManager.TrayInteractionListener;
import com.android.launcher3.common.tray.TrayManager.TrayLevel;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.Removable;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.folderlock.FolderLock.FolderLockActionCallback;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.LightingEffectManager;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.animation.SearchedAppBounceAnimation;
import com.android.launcher3.util.event.ScreenDivision;
import com.android.launcher3.util.event.ScrollDeterminator;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppsController extends Stage implements OnFocusChangeListener, OnLongClickListener, TrayInteractionListener, OnBadgeBindingCompletedListener, OnNotificationPreviewBindingListener, OnRefreshLiveIconListener, MultiSelectListener, FolderLockActionCallback, Listener {
    private static final String APPS_ORGANIZE_APPS_ALERT = "AppsController.OrganizeAppsAlert";
    private static final String APPS_VIEW_TYPE = "AppsController.ViewType";
    private static final boolean DEBUG_APPS_CONTROLLER = true;
    private static final int EXIT_DRAG_STATE_DELAY = 100;
    private static final int FACTOR_INTERVAL = 2;
    private static final int FACTOR_VALID_DECIMAL_PLACE = 2;
    private static final String KEY_REPOSITION_BY = "KEY_REPOSITION_BY";
    private static final String KEY_TARGET_GRID_SIZE = "KEY_CHANGE_GRID_SIZE";
    private static final boolean MAP_NO_RECURSE = false;
    private static final boolean MAP_RECURSE = true;
    private static final int REPOSITION_CONFIGURATION = 3;
    private static final int REPOSITION_GRIDBY = 1;
    private static final int REPOSITION_TIDEUP = 2;
    private static final int REPOSITION_VIEWBY = 0;
    public static final int STATE_DRAG = 1;
    public static final int STATE_GRID = 5;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_SEARCH = 3;
    public static final int STATE_SELECT = 2;
    public static final int STATE_TIDY = 4;
    private static final String TAG = "Launcher.AppsController";
    private AppsAdapterProvider mAdapterProvider;
    private boolean mApplyTideUpPage = false;
    private float mAppsAlphaRatio;
    private AppsTransitionAnimation mAppsAnimation;
    private AppsContainer mAppsContainer;
    private AppsFocusListener mAppsFocusListener;
    private View mAppsPageIndicatorView;
    private AppsPagedView mAppsPagedView;
    private AppsScreenGridPanel mAppsScreenGridPanel;
    private AppsSearch mAppsSearch;
    private float mAppsShrinkFactor;
    private int mAppsSlipY;
    private ViewGroup mAppsTidyUpContainer;
    private SearchedAppBounceAnimation mBounceAnimation;
    private DataListener mDataListener = new DataListener() {
        public void updateApps(ArrayList<ItemInfo> apps) {
            AppsController.this.updateApps(apps);
        }

        public void removeApps(ArrayList<ItemInfo> apps) {
            AppsController.this.removeApps(apps);
        }

        public void rearrangeAllViews(boolean animate) {
            AppsController.this.mAppsPagedView.rearrangeAllViews(animate);
        }

        public int getMaxItemsPerScreen() {
            return AppsController.this.mAppsPagedView.getMaxItemsPerScreen();
        }

        public int getCellCountX() {
            return AppsController.this.mAppsPagedView.getCellCountX();
        }

        public View createItemView(ItemInfo item, View recycleView) {
            return AppsController.this.createItemView(item, AppsController.this.mAppsPagedView.getCellLayout((int) item.screenId), recycleView);
        }

        public void addItem(View view, ItemInfo item) {
            AppsController.this.addItem(view, item);
        }

        public void removeAllViews() {
            AppsController.this.mAppsPagedView.removeAllPages();
        }

        public boolean needDeferredUpdate() {
            return AppsController.this.deferToBind();
        }

        public void updateGridInfo() {
            AppsScreenGridPanel gridPanel;
            if (AppsController.this.mAppsScreenGridPanel != null) {
                gridPanel = AppsController.this.mAppsScreenGridPanel;
            } else {
                gridPanel = (AppsScreenGridPanel) AppsController.this.mLauncher.findViewById(R.id.apps_screen_grid_panel);
            }
            if (gridPanel != null) {
                gridPanel.updateGridBtnLayout();
            }
        }

        public void updateRestoreItems(HashSet<ItemInfo> updates) {
            AppsController.this.updateRestoreItems(updates);
        }

        public View getAppsIconByItemId(long id) {
            return AppsController.this.getAppsIconByItemId(id);
        }

        public void removeEmptyCellsAndViews(ArrayList<ItemInfo> apps) {
            try {
                ArrayList<CellLayout> dirtyScreen = new ArrayList();
                Iterator it = apps.iterator();
                while (it.hasNext()) {
                    View v = getAppsIconByItemId(((ItemInfo) it.next()).id);
                    if (v != null) {
                        CellLayout parent = (CellLayout) v.getParent().getParent();
                        if (parent != null) {
                            parent.removeView(v);
                            if (!dirtyScreen.contains(parent)) {
                                dirtyScreen.add(parent);
                            }
                        }
                    }
                }
                if (AppsController.this.mViewType != ViewType.ALPHABETIC_GRID) {
                    int endPos = AppsController.this.mAppsPagedView.getMaxItemsPerScreen() - 1;
                    it = dirtyScreen.iterator();
                    while (it.hasNext()) {
                        AppsController.this.mReorderController.removeEmptyCellAtPage(0, endPos, AppsController.this.mAppsPagedView.indexOfChild((CellLayout) it.next()), false);
                    }
                    AppsController.this.removeEmptyPagesAndUpdateAllItemsInfo();
                    return;
                }
                AppsController.this.mAppsPagedView.removeEmptyScreen();
            } catch (NullPointerException e) {
                Log.d(AppsController.TAG, "" + e);
            }
        }

        public void makeEmptyCellAndReorder(int screenId, int rank) {
            try {
                int cellCountX = AppsController.this.mAppsPagedView.getCellCountX();
                if (AppsController.this.mAppsPagedView.getCellLayout(screenId).getChildAt(rank % cellCountX, rank / cellCountX) != null) {
                    AppsController.this.mReorderController.makeEmptyCellAndReorder(screenId, rank);
                }
            } catch (NullPointerException e) {
                Log.d(AppsController.TAG, "" + e);
            }
        }
    };
    private int mDownwardFadeOutEnd;
    private int mDownwardFadeOutStart;
    private AppsDragController mDragController;
    private DragLayer mDragLayer;
    private DragManager mDragMgr;
    private Handler mExitDragStateHandler = new Handler();
    private float mFadeOutRange;
    private FolderLock mFolderLock;
    private boolean mFromSetting = false;
    private int mHwLayerPageIndexWhileTray = -1;
    private boolean mIsResumed = false;
    private Runnable mLoggingRunnable = new Runnable() {
        public void run() {
            if (AppsController.this.mAppsPagedView != null) {
                AppsController.this.mAppsPagedView.loggingPageCount();
            }
        }
    };
    private int mMoveToHomeApproachingStart;
    private MultiSelectManager mMultiSelectManager;
    private float mPageIndicatorScaleRatio;
    private float mPageIndicatorShrinkFactor;
    private float mPageSnapMovingRatioOnApps;
    private int mPrevState = 0;
    private boolean mRemoveInProgress = false;
    private AppsReorderController mReorderController;
    private ScrollDeterminator mScrollDeterminator = new ScrollDeterminator();
    private int mState = 0;
    private TrayManager mTrayManager;
    private int mUpwardFadeOutEnd;
    private int mUpwardFadeOutStart;
    private ViewType mViewType;

    public enum ViewType {
        CUSTOM_GRID,
        ALPHABETIC_GRID
    }

    Launcher getLauncher() {
        return this.mLauncher;
    }

    void setApplyTidyUpPage(boolean apply) {
        this.mApplyTideUpPage = apply;
    }

    public void setup() {
        this.mDragMgr = this.mLauncher.getDragMgr();
        this.mAppsFocusListener = new AppsFocusListener();
        this.mTrayManager = this.mLauncher.getTrayManager();
        if (this.mTrayManager != null) {
            this.mTrayManager.addTrayEventCallbacks(this);
        }
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
            this.mFolderLock.addFolderLockActionCallback(this);
        }
        LauncherAppState.getInstance().getModel().registerOnBadgeBindingCompletedListener(this);
        LauncherAppState.getInstance().getModel().registerOnNotificationPreviewListener(this);
        LauncherAppState.getInstance().getModel().registerOnLiveIconUpdateListener(this);
        LauncherAppState.getInstance().getLauncherProxy().setAppsProxyCallbacks(new AppsProxyCallbacksImpl(this));
        this.mScrollDeterminator.setSystemTouchSlop(this.mLauncher);
        this.mScrollDeterminator.registrateController(1);
    }

    private void initContainerView() {
        this.mAppsContainer = (AppsContainer) this.mLauncher.findViewById(R.id.apps_view);
        this.mAppsContainer.bindController(this);
        this.mAppsContainer.setTrayManager(this.mTrayManager);
        this.mAppsContainer.setVisibility(View.GONE);
    }

    private void initBackground() {
        this.mDragLayer.setBackgroundImageAlpha(0.0f);
        this.mDragLayer.setBackgroundImage(R.drawable.apps_dim);
    }

    private void initReorderController() {
        this.mReorderController = new AppsReorderController(this.mLauncher, this.mAppsPagedView);
        this.mReorderController.setListener(this);
    }

    private void initDragController() {
        this.mDragController = new AppsDragController(this.mLauncher, this.mAppsPagedView);
        this.mDragController.setup(this.mDragLayer);
        this.mDragController.setListener(this);
        this.mDragController.setReorderListener(this.mReorderController);
        this.mDragMgr.addDragListener(this.mDragController);
    }

    private void initAppsPagedView() {
        this.mAppsPagedView = (AppsPagedView) this.mLauncher.findViewById(R.id.apps_content);
        this.mAppsPagedView.setup(this.mDragMgr);
        this.mAppsPagedView.setListener(this);
        this.mAppsPagedView.setOnFocusChangeListener(this);
        this.mAppsPagedView.setScrollDeterminator(this.mScrollDeterminator);
    }

    private void initTrayResources() {
        Resources res = this.mLauncher.getResources();
        this.mAppsSlipY = res.getDimensionPixelSize(R.dimen.tray_apps_slip_y);
        this.mMoveToHomeApproachingStart = res.getDimensionPixelSize(R.dimen.approaching_start_on_apps);
        this.mPageSnapMovingRatioOnApps = 1.0f - res.getFraction(R.fraction.config_pageSnapMovingRatio, 1, 1);
        this.mAppsShrinkFactor = res.getFraction(R.fraction.config_appsShrinkFactor, 1, 1);
        this.mAppsAlphaRatio = res.getFraction(R.fraction.config_appsAlphaRatio, 1, 1);
        this.mPageIndicatorShrinkFactor = res.getFraction(R.fraction.config_appsPageIndicatorShrinkFactor, 1, 1);
        this.mPageIndicatorScaleRatio = res.getFraction(R.fraction.config_appsPageIndicatorScaleRatio, 1, 1);
        this.mUpwardFadeOutStart = res.getDimensionPixelSize(R.dimen.tray_overlay_start_on_transition_type_2);
        this.mUpwardFadeOutEnd = 0;
        this.mDownwardFadeOutStart = -this.mUpwardFadeOutStart;
        this.mDownwardFadeOutEnd = 0;
        this.mFadeOutRange = (float) (this.mUpwardFadeOutEnd - this.mUpwardFadeOutStart);
    }

    private void initAdapterProvider() {
        this.mAdapterProvider = new AppsAdapterProvider(this.mLauncher, this.mDataListener, this.mViewType);
    }

    private void initAppSearchBar() {
        this.mAppsSearch = new AppsSearch(this.mAppsContainer, this);
    }

    private void initPageIndicator() {
        this.mAppsPageIndicatorView = this.mLauncher.findViewById(R.id.apps_page_indicator);
        if (this.mAppsPageIndicatorView != null) {
            LayoutParams oldLp = (LayoutParams) this.mAppsPageIndicatorView.getLayoutParams();
            this.mAppsContainer.removeView(this.mAppsPageIndicatorView);
            int indexToAddView = this.mDragLayer.getChildCount();
            for (int i = 0; i < this.mDragLayer.getChildCount(); i++) {
                if (this.mAppsContainer.equals(this.mDragLayer.getChildAt(i))) {
                    indexToAddView = i;
                    break;
                }
            }
            DragLayer.LayoutParams newLp = new DragLayer.LayoutParams(oldLp);
            newLp.gravity = 81;
            this.mDragLayer.addView(this.mAppsPageIndicatorView, indexToAddView, newLp);
            this.mAppsContainer.initExternalPageIndicator(this.mAppsPageIndicatorView);
            if (!this.mLauncher.isAppsStage()) {
                this.mAppsPageIndicatorView.setAlpha(0.0f);
            }
        }
    }

    private void initScreenGridPanel() {
        this.mAppsScreenGridPanel = (AppsScreenGridPanel) this.mLauncher.findViewById(R.id.apps_screen_grid_panel);
        this.mAppsScreenGridPanel.bindController(this);
        this.mAppsScreenGridPanel.initScreenGridTopContainer();
    }

    public void initStageView() {
        this.mDragLayer = this.mLauncher.getDragLayer();
        this.mViewType = ViewType.valueOf(getViewTypeFromSharedPreference(this.mLauncher));
        initContainerView();
        initBackground();
        initAppsPagedView();
        initReorderController();
        initDragController();
        initTrayResources();
        initAppSearchBar();
        initAdapterProvider();
        initPageIndicator();
        this.mAppsTidyUpContainer = (ViewGroup) this.mLauncher.findViewById(R.id.apps_tidyup_container);
        this.mAppsAnimation = new AppsTransitionAnimation(this.mLauncher, this, this.mTrayManager);
        if (LauncherFeature.supportMultiSelect()) {
            this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
            if (this.mMultiSelectManager != null) {
                this.mMultiSelectManager.addMultiSelectCallbacks(this);
            }
        }
        View applyButton = this.mLauncher.findViewById(R.id.tidy_up_apply_button);
        applyButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AppsController.this.mApplyTideUpPage = true;
                AppsController.this.changeState(0, true);
            }
        });
        setTextViewDescription(applyButton);
        View cancelButton = this.mLauncher.findViewById(R.id.tidy_up_cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AppsController.this.changeState(0, true);
            }
        });
        setTextViewDescription(cancelButton);
        if (Utilities.isEnableBtnBg(this.mLauncher)) {
            applyButton.setBackgroundResource(R.drawable.panel_btn_bg);
            cancelButton.setBackgroundResource(R.drawable.panel_btn_bg);
        }
        initScreenGridPanel();
        searchSettingCheck();
        super.initStageView();
    }

    public boolean onLongClick(View v) {
        if (TestHelper.isRoboUnitTest() || !this.mLauncher.isAppsStage() || this.mLauncher.isRunningAnimation() || this.mDragMgr.isDragging()) {
            return false;
        }
        if ((this.mTrayManager != null && this.mTrayManager.isMoving()) || isTidyState() || isDragLocked()) {
            return false;
        }
        initBounceAnimation();
        CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            longClickCellInfo = new CellInfo(v, (ItemInfo) v.getTag());
            itemUnderLongClick = longClickCellInfo.cell;
        }
        if (itemUnderLongClick != null) {
            if (!this.mDragMgr.isDragging() && this.mState == 0) {
                startDrag(longClickCellInfo, true);
            } else if (this.mState == 2 && this.mMultiSelectManager.canLongClick(itemUnderLongClick)) {
                startDrag(longClickCellInfo, false);
            }
        }
        return true;
    }

    private void startDrag(CellInfo cellInfo, boolean allowQuickOption) {
        View child = cellInfo.cell;
        if (Utilities.ATLEAST_O || child.isInTouchMode()) {
            child.setVisibility(4);
            changeState(1, true);
            this.mDragController.startDrag(cellInfo);
            this.mLauncher.beginDragShared(child, this.mDragController, allowQuickOption, false);
        }
    }

    public void onStartActivity() {
        this.mAdapterProvider.start();
    }

    public void onResumeActivity() {
        this.mIsResumed = true;
        this.mAppsPagedView.onResume();
        this.mAdapterProvider.resume();
        this.mLauncher.getPageTransitionManager().setup(this.mAppsPagedView);
        this.mAppsSearch.resume();
        initBounceAnimation();
        String appinfo = this.mLauncher.getSearchedApp();
        if (appinfo != null) {
            String pkg = null;
            String cmp = null;
            if (appinfo.contains("/")) {
                int index = appinfo.indexOf("/");
                pkg = appinfo.substring(0, index);
                cmp = appinfo.substring(index + 1, appinfo.length());
            }
            if (Utilities.isPackageExist(this.mLauncher, pkg)) {
                ComponentName componentName = new ComponentName(pkg, cmp);
                ItemInfo info = this.mLauncher.getLauncherModel().getAppsModel().getItemInfoInAppsForComponentName(componentName, UserHandleCompat.fromUser(this.mLauncher.getSearchedAppUser()), true);
                if (info == null || !info.isHiddenByUser()) {
                    findSearchedApp(componentName, this.mLauncher.getSearchedAppUser());
                    this.mLauncher.setSearchedApp(null);
                    this.mLauncher.setSearchedAppUser(null);
                } else {
                    StageEntry data = new StageEntry();
                    data.enableAnimation = true;
                    data.fromStage = 2;
                    data.putExtras(AppsPickerController.KEY_PICKER_MODE, Integer.valueOf(1));
                    data.putExtras(AppsPickerController.KEY_BOUNCED_ITEM, appinfo);
                    data.putExtras(AppsPickerController.KEY_BOUNCED_ITEM_USER, this.mLauncher.getSearchedAppUser());
                    getStageManager().startStage(6, data);
                    this.mLauncher.setSearchedApp(null);
                    this.mLauncher.setSearchedAppUser(null);
                    return;
                }
            }
        }
        if (this.mLauncher.isAppsStage() && this.mState == 0) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
        }
    }

    public void onPauseActivity() {
        this.mIsResumed = false;
        this.mAppsPagedView.onPause();
        initBounceAnimation();
        if (isSelectState()) {
            if (this.mMultiSelectManager != null && this.mMultiSelectManager.isShowingHelpDialog()) {
                this.mMultiSelectManager.hideHelpDialog(false);
            }
            changeState(0, false);
        }
    }

    public void onStopActivity() {
        this.mAdapterProvider.stop();
    }

    public void onDestroyActivity() {
        if (this.mTrayManager != null) {
            this.mTrayManager.removeTrayEventCallbacks(this);
        }
        this.mAdapterProvider.destroy();
        this.mAppsContainer = null;
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.removeMultiSelectCallbacks(this);
        }
        if (this.mFolderLock != null) {
            this.mFolderLock.removeFolderLockActionCallback(this);
        }
        if (LauncherAppState.getInstanceNoCreate() != null) {
            LauncherAppState.getInstance().getModel().unregisterOnBadgeBindingCompletedListener(this);
            LauncherAppState.getInstance().getModel().unregisterOnNotificationPreviewListener(this);
            LauncherAppState.getInstance().getModel().unregisterOnLiveIconUpdateListener(this);
        }
    }

    public AppsDragController getDragController() {
        return this.mDragController;
    }

    public AppsPagedView getAppsPagedView() {
        return this.mAppsPagedView;
    }

    public View getAppsPageIndicatorView() {
        return this.mAppsPageIndicatorView;
    }

    public View getContainerView() {
        return this.mAppsContainer;
    }

    public int getState() {
        return this.mState;
    }

    public View getAppsSearchBarView() {
        return this.mAppsSearch.getAppsSearchBarView();
    }

    public View getTidyUpContainerView() {
        return this.mAppsTidyUpContainer;
    }

    public AppsScreenGridPanel getAppsScreenGridPanel() {
        return this.mAppsScreenGridPanel;
    }

    public void setPagedViewVisibility(boolean show) {
        int visibility = show ? 0 : 8;
        if (this.mAppsPagedView != null) {
            this.mAppsPagedView.setVisibility(visibility);
        }
        if (this.mAppsPageIndicatorView != null) {
            this.mAppsPageIndicatorView.setVisibility(visibility);
        }
    }

    protected Animator onStageEnter(StageEntry data) {
        this.mDragMgr.setDragScroller(this.mDragController);
        this.mDragMgr.addDropTarget(this.mDragController);
        this.mDragMgr.setMoveTarget(this.mAppsPagedView);
        Animator enterAnim = null;
        if (data != null) {
            boolean suppressChangeStageOnce;
            int fromViewMode = data.fromStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            boolean animated = data.enableAnimation;
            if (((Integer) data.getExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, Integer.valueOf(0))).intValue() > 0) {
                suppressChangeStageOnce = true;
            } else {
                suppressChangeStageOnce = false;
            }
            if (suppressChangeStageOnce && this.mTrayManager != null) {
                this.mTrayManager.setSuppressChangeStageOnce();
            }
            if (data.getInternalStateTo() == 5) {
                enterAnim = this.mAppsAnimation.getEnterFromSettingAnim(animated, layerViews);
                changeState(5, false);
            } else {
                if (fromViewMode == 1) {
                    if (data.getInternalStateTo() == 1) {
                        changeState(1, animated);
                    } else {
                        enterAnim = this.mAppsAnimation.getEnterFromHomeAnimation(animated, layerViews);
                        this.mAppsSearch.setSearchBarVisibility(0);
                        if (data.getInternalStateTo() == 0) {
                            Talk.INSTANCE.say(this.mLauncher.getResources().getString(R.string.apps_screen) + ", " + this.mAppsPagedView.getPageDescription());
                        }
                    }
                    this.mAppsPagedView.updateAccessibilityFlags(true);
                } else if (fromViewMode == 5 || fromViewMode == 6) {
                    if (fromViewMode == 6) {
                        ArrayList<ItemInfo> itemsToHide = (ArrayList) data.getExtras(AppsPickerController.KEY_ITEMS_TO_HIDE);
                        ArrayList<ItemInfo> itemsToShow = (ArrayList) data.getExtras(AppsPickerController.KEY_ITEMS_TO_SHOW);
                        if (!(itemsToHide == null || itemsToShow == null)) {
                            this.mLauncher.updateItemInfo(itemsToHide, itemsToShow);
                        }
                    }
                    enterAnim = this.mAppsAnimation.getEnterFromFolderAnimation(animated, layerViews, (View) data.getExtras(FolderController.KEY_FOLDER_ICON_VIEW));
                    changeState(data.getInternalStateTo(), animated);
                } else if (fromViewMode == 3) {
                    enterAnim = this.mAppsAnimation.getEnterFromSettingAnim(animated, layerViews);
                }
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
            }
            data.addOnCompleteRunnableCallBack(this.mLoggingRunnable);
        }
        return enterAnim;
    }

    protected Animator onStageExit(StageEntry data) {
        Animator exitAnim = null;
        Utilities.closeDialog(this.mLauncher);
        closeViewTypeDialog();
        closeOrganizeAppsConfirmDialog();
        initBounceAnimation();
        this.mAppsSearch.stageExit(data);
        if (data != null) {
            int toViewMode = data.toStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            boolean animated = data.enableAnimation;
            if (toViewMode == 1) {
                boolean homePressed;
                if (data.stageCountOnFinishAllStage > 0) {
                    homePressed = true;
                } else {
                    homePressed = false;
                }
                if (this.mState == 5 && homePressed) {
                    restoreScreenGrid(0, false);
                }
                changeState(0, true);
                exitAnim = this.mAppsAnimation.getExitToHomeAnimation(animated, layerViews);
                this.mAppsPagedView.updateAccessibilityFlags(false);
                this.mLauncher.getDragMgr().removeDropTarget(this.mDragController);
            } else if (toViewMode == 5 || toViewMode == 6) {
                exitAnim = this.mAppsAnimation.getExitToFolderAnimation(animated, layerViews, (View) data.getExtras(FolderController.KEY_FOLDER_ICON_VIEW));
            } else if (toViewMode == 3) {
                exitAnim = this.mAppsAnimation.getExitToWidgetAnim(false, layerViews);
            }
            data.addOnCompleteRunnableCallBack(this.mLoggingRunnable);
        }
        return exitAnim;
    }

    protected Animator onStageExitByTray() {
        Animator exitAni = this.mAppsAnimation.getExitToHomeAnimation(true, null);
        exitAni.addListener(new AnimatorListenerAdapter() {
            private boolean canceled = false;

            public void onAnimationCancel(Animator animation) {
                boolean z = AppsController.this.mTrayManager != null && AppsController.this.mTrayManager.isTouching();
                this.canceled = z;
            }

            public void onAnimationEnd(Animator animation) {
                if (this.canceled) {
                    Log.d(AppsController.TAG, "Apps onStageExitByTray canceled");
                } else if (AppsController.this.mAppsContainer == null) {
                    Log.d(AppsController.TAG, "Apps onDestroyActivity !");
                } else {
                    AppsController.this.mAppsSearch.stageExit(null);
                    AppsController.this.mAppsPagedView.updateAccessibilityFlags(false);
                    AppsController.this.mLauncher.getDragMgr().removeDropTarget(AppsController.this.mDragController);
                    AppsController.this.mLoggingRunnable.run();
                }
            }
        });
        return exitAni;
    }

    protected Animator onStageEnterByTray() {
        Animator enterAni = this.mAppsAnimation.getEnterFromHomeAnimation(true, null);
        enterAni.addListener(new AnimatorListenerAdapter() {
            private boolean canceled = false;

            public void onAnimationCancel(Animator animation) {
                boolean z = AppsController.this.mTrayManager != null && AppsController.this.mTrayManager.isTouching();
                this.canceled = z;
            }

            public void onAnimationEnd(Animator animation) {
                if (this.canceled) {
                    Log.d(AppsController.TAG, "Apps onStageEnterByTray canceled");
                } else if (AppsController.this.mAppsContainer == null) {
                    Log.d(AppsController.TAG, "Apps onDestroyActivity !");
                } else {
                    AppsController.this.mDragMgr.setDragScroller(AppsController.this.mDragController);
                    AppsController.this.mDragMgr.addDropTarget(AppsController.this.mDragController);
                    AppsController.this.mDragMgr.setMoveTarget(AppsController.this.mAppsPagedView);
                    AppsController.this.mAppsSearch.setSearchBarVisibility(0);
                    Talk.INSTANCE.say(AppsController.this.mLauncher.getResources().getString(R.string.apps_screen) + ", " + AppsController.this.mAppsPagedView.getPageDescription());
                    AppsController.this.mAppsPagedView.updateAccessibilityFlags(true);
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
                    AppsController.this.mLoggingRunnable.run();
                }
            }
        });
        return enterAni;
    }

    protected void onStageMovingToInitial(StageEntry data) {
        if (this.mTrayManager != null && Float.compare(this.mAppsContainer.getAlpha(), 1.0f) != 0) {
            this.mAppsContainer.setVisibility(View.GONE);
            this.mAppsContainer.setAlpha(1.0f);
            this.mDragMgr.removeDropTarget(this.mDragController);
        }
    }

    public void setViewType(ViewType viewType) {
        if (this.mViewType != viewType) {
            Log.d(TAG, "setViewType. old: " + this.mViewType + ", new: " + viewType);
            this.mViewType = viewType;
            this.mAppsPagedView.setCurrentPage(0);
            if (this.mAppsPagedView.getCellLayout(this.mAppsPagedView.getCurrentPage()) == null) {
                Log.d(TAG, "There are no items that should be moved to position by normalizer");
                return;
            }
            Resources res = this.mLauncher.getResources();
            SALogging.getInstance().insertStatusLog(res.getString(R.string.status_AppsSortStatus), this.mViewType.ordinal());
            if (this.mViewType == ViewType.CUSTOM_GRID) {
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_Apps_SortStatus), "1");
            } else if (this.mViewType == ViewType.ALPHABETIC_GRID) {
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_Apps_SortStatus), "2");
            }
            saveViewTypeSharefPref();
            changeState(0, true, true);
            getDragController().updateDragMode();
        }
    }

    private void applySetViewType() {
        this.mAdapterProvider.setAdapter(this.mViewType);
        if (this.mViewType == ViewType.ALPHABETIC_GRID) {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ATOZ_APPS_REORDER, null, -1, false);
        } else {
            this.mAdapterProvider.reloadAllItemsFromDB(true);
        }
        repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 0, false);
        if (this.mViewType == ViewType.CUSTOM_GRID) {
            updateDirtyItems();
        }
    }

    private void saveViewTypeSharefPref() {
        Editor editor = this.mAppsPagedView.getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        editor.putString(APPS_VIEW_TYPE, this.mViewType.name());
        editor.apply();
    }

    public void cancelChangeScreenGrid() {
        Log.w(TAG, "cancelChangeScreenGrid for preview");
        this.mDragMgr.removeDropTarget(this.mDragController);
        startSettingActivity();
    }

    public void chooseViewType() {
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(25);
        AppsViewTypeDialog.createAndShow(this.mViewType, this.mLauncher.getFragmentManager(), new OnViewTypeChagnedListener() {
            public void onResult(ViewType viewType) {
                AppsController.this.setViewType(viewType);
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
            }

            public void onDismiss() {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
            }
        });
    }

    void hideViewTypeDialog() {
        closeViewTypeDialog();
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
    }

    private void closeViewTypeDialog() {
        FragmentManager manager = this.mLauncher.getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        AppsViewTypeDialog.dismiss(ft, manager);
        ft.addToBackStack(null);
    }

    public ViewType getViewType() {
        return this.mViewType;
    }

    public DragSource getDragSourceForLongKey() {
        return getDragController();
    }

    public boolean onClick(View v) {
        if (this.mLauncher.isRunningAnimation()) {
            return false;
        }
        if ((this.mTrayManager != null && this.mTrayManager.isMoving()) || isTidyState()) {
            return false;
        }
        initBounceAnimation();
        IconInfo tag = v.getTag();
        if (!(tag instanceof IconInfo) && !(tag instanceof FolderInfo)) {
            return false;
        }
        switch (this.mState) {
            case 0:
                if (v instanceof FolderIconView) {
                    this.mLauncher.onClickFolderIcon(v);
                } else {
                    this.mLauncher.startAppShortcutOrInfoActivity(v);
                    if (tag instanceof IconInfo) {
                        sendGSIMLog(tag);
                    }
                }
                return true;
            case 1:
                return true;
            case 2:
                if (!(v instanceof FolderIconView)) {
                    ((IconView) v).getCheckBox().toggle();
                    SALogging.getInstance().insertEventLog(this.mLauncher.getResources().getString(R.string.screen_Apps_SelectMode), "0", "0");
                } else if (LauncherFeature.supportFolderSelect()) {
                    ((FolderIconView) v).getCheckBox().toggle();
                } else {
                    this.mLauncher.onClickFolderIcon(v);
                }
                return true;
            case 3:
                if (!(tag instanceof IconInfo)) {
                    return false;
                }
                IconInfo item = tag;
                this.mLauncher.startAppShortcutOrInfoActivity(v);
                this.mAppsSearch.updateRecentApp(item);
                sendGSIMLog(item);
                changeState(0, false);
                return true;
            default:
                return false;
        }
    }

    private void sendGSIMLog(IconInfo info) {
        if (info != null) {
            ComponentName cn = info.getTargetComponent();
            if (cn != null) {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_ICON_STARTED, cn.getPackageName(), -1, false);
            }
        }
    }

    public void exitDragStateDelayed() {
        exitDragStateDelayed(100);
    }

    public void exitDragStateDelayed(int delay) {
        if (this.mState == 1) {
            this.mExitDragStateHandler.postDelayed(new Runnable() {
                public void run() {
                    AppsController.this.changeState(0, true);
                }
            }, (long) delay);
        }
    }

    public boolean changeState(int toState, boolean animated) {
        return changeState(toState, animated, false);
    }

    private boolean changeState(int toState, boolean animated, boolean forced) {
        if (!forced && this.mState == toState) {
            return false;
        }
        Log.d(TAG, "changeState : " + this.mState + " > " + toState + ", animated : " + animated);
        StageEntry data = new StageEntry();
        data.enableAnimation = animated;
        data.setInternalStateFrom(this.mState);
        data.setInternalStateTo(toState);
        getStageManager().switchInternalState(this, data);
        return true;
    }

    public boolean onBackPressed() {
        initBounceAnimation();
        if (this.mFromSetting) {
            startSettingActivity();
            return true;
        } else if (this.mState == 0) {
            Resources res = this.mLauncher.getResources();
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_2xxx), res.getString(R.string.event_Apps_Home), "1");
            return false;
        } else if (this.mState == 3) {
            changeState(0, false);
            return true;
        } else if (this.mState == 5) {
            startSettingActivity();
            return true;
        } else {
            changeState(0, true);
            return true;
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            this.mAppsPagedView.requestFocus();
        }
    }

    public Animator switchInternalState(StageEntry data) {
        int fromState = data.getInternalStateFrom();
        this.mPrevState = fromState;
        int toState = data.getInternalStateTo();
        this.mState = toState;
        HashMap<View, Integer> layerViews = data.getLayerViews();
        boolean animated = data.enableAnimation;
        Animator stateChangeAnim = null;
        if (fromState == 0) {
            if (toState == 1) {
                showMoveToHomePanel(true);
                this.mAppsPagedView.addExtraEmptyScreenOnDrag();
                stateChangeAnim = this.mAppsAnimation.getDragAnimation(animated, layerViews, true, false);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(0);
            } else if (toState == 3) {
                stateChangeAnim = this.mAppsSearch.switchInternalState(this.mAppsAnimation, data);
            } else if (toState == 2) {
                this.mExitDragStateHandler.removeCallbacksAndMessages(null);
                this.mAppsPagedView.updateCheckBox(true);
                stateChangeAnim = this.mAppsAnimation.getSelectAnimation(animated, layerViews, true);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(0);
                this.mAppsPagedView.showHintPages();
            } else if (toState == 4) {
                data.putExtras(KEY_REPOSITION_BY, Integer.valueOf(2));
                stateChangeAnim = this.mAppsAnimation.getTidyUpAnimation(animated, layerViews, true, data);
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(10);
            } else if (toState == 0) {
                data.putExtras(KEY_REPOSITION_BY, Integer.valueOf(0));
                stateChangeAnim = this.mAppsAnimation.getChangeViewTypeAnimation(animated, layerViews, data);
            } else if (toState == 5) {
                if (this.mAppsScreenGridPanel != null) {
                    this.mAppsScreenGridPanel.updateButtonStatus();
                    this.mAppsScreenGridPanel.updateApplyCancelButton();
                    if (this.mAppsScreenGridPanel.getScreenGridTopContainer() != null) {
                        this.mAppsScreenGridPanel.getScreenGridTopContainer().setVisibility(View.VISIBLE);
                    }
                }
                stateChangeAnim = this.mAppsAnimation.getScreenGridEnterExitAnimation(false, layerViews, true);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(0, false);
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(24);
            }
        } else if (fromState == 1) {
            showMoveToHomePanel(false);
            if (toState == 0) {
                if (isSelectState()) {
                    this.mLauncher.onChangeSelectMode(false, animated);
                    this.mAppsPagedView.updateCheckBox(false);
                }
                if (this.mReorderController.getExistOverLastItemMoved()) {
                    this.mReorderController.undoOverLastItems();
                }
                stateChangeAnim = this.mAppsAnimation.getDragAnimation(animated, layerViews, false, false);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(8);
            } else if (toState == 2) {
                this.mExitDragStateHandler.removeCallbacksAndMessages(null);
                this.mAppsPagedView.updateCheckBox(true);
                stateChangeAnim = this.mAppsAnimation.getSelectAnimation(animated, layerViews, true);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(0);
            }
        } else if (fromState == 3) {
            if (toState == 0) {
                this.mAppsSearch.switchInternalState(this.mAppsAnimation, data);
            }
        } else if (fromState == 2) {
            if (toState == 0) {
                stateChangeAnim = this.mAppsAnimation.getSelectAnimation(animated, layerViews, false);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(8);
                this.mLauncher.onChangeSelectMode(false, animated);
                this.mAppsPagedView.updateCheckBox(false);
                this.mAppsPagedView.hideHintPages();
            } else if (toState == 1) {
                this.mAppsPagedView.updateCheckBox(false);
                this.mAppsPagedView.addExtraEmptyScreenOnDrag();
                showMoveToHomePanel(true);
                stateChangeAnim = this.mAppsAnimation.getDragAnimation(animated, layerViews, true, true);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(0);
            }
        } else if (fromState == 4) {
            if (toState == 0) {
                boolean z;
                data.putExtras(KEY_REPOSITION_BY, Integer.valueOf(2));
                AppsTransitionAnimation appsTransitionAnimation = this.mAppsAnimation;
                if (this.mApplyTideUpPage || !data.enableAnimation) {
                    z = false;
                } else {
                    z = true;
                }
                stateChangeAnim = appsTransitionAnimation.getTidyUpAnimation(z, layerViews, false, data);
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(2);
            }
        } else if (fromState == 5) {
            if (toState == 0) {
                stateChangeAnim = this.mAppsAnimation.getScreenGridEnterExitAnimation(animated, layerViews, false);
                this.mAppsPagedView.setCrosshairsVisibilityChilds(8, false);
            } else if (toState == 5) {
                data.addOnCompleteRunnableCallBack(new Runnable() {
                    public void run() {
                        AppsController.this.mAppsScreenGridPanel.updateApplyCancelButton();
                    }
                });
                stateChangeAnim = this.mAppsAnimation.getChangeGridAnimation(animated, layerViews, data);
            }
        }
        if (toState == 0) {
            data.addOnCompleteRunnableCallBack(this.mLoggingRunnable);
        }
        this.mAppsPagedView.updateAccessibilityFlags(true);
        return stateChangeAnim;
    }

    protected int getInternalState() {
        return this.mState;
    }

    protected boolean supportStatusBarForState(int internalState) {
        if (internalState == 1 || internalState == 2 || (isLandscapeOnPhoneModel() && internalState == 4)) {
            return false;
        }
        return true;
    }

    protected boolean supportNavigationBarForState(int internalState) {
        return isLandscapeOnPhoneModel() || internalState != 1;
    }

    protected float getBackgroundBlurAmountForState(int internalState) {
        return BlurUtils.getMaxBlurAmount();
    }

    protected float getBackgroundDimAlphaForState(int internalState) {
        return 0.0f;
    }

    protected float getBackgroundImageAlphaForState(int internalState) {
        return 1.0f;
    }

    public void updateBadgeItems(ArrayList<ItemInfo> apps) {
        final HashSet<ItemInfo> updates = new HashSet(apps);
        this.mAppsPagedView.mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (info != null && updates.contains(info)) {
                    View iconView = v;
                    if (AppsController.this.isItemInFolder(info)) {
                        iconView = AppsController.this.getAppsIconByItemId(info.container);
                    }
                    if (iconView instanceof IconView) {
                        ((IconView) iconView).refreshBadge();
                    }
                    if (parent instanceof FolderIconView) {
                        ((FolderIconView) parent).refreshBadge();
                    }
                }
                return false;
            }
        });
    }

    private void updateRestoreItems(final HashSet<ItemInfo> updates) {
        this.mAppsPagedView.mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((info instanceof IconInfo) && (v instanceof IconView) && updates.contains(info)) {
                    ((IconView) v).applyState(false);
                }
                return false;
            }
        });
    }

    private void updateApps(ArrayList<ItemInfo> apps) {
        final HashSet<ItemInfo> updates = new HashSet(apps);
        final ArrayList<FolderInfo> folderInfosToSort = new ArrayList();
        final ArrayList<FolderIconView> folderIconsToRefresh = new ArrayList();
        final boolean isAlphabeticalMode = isAlphabeticalMode();
        this.mAppsPagedView.mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                boolean z = true;
                if ((info instanceof IconInfo) && (v instanceof IconView) && updates.contains(info)) {
                    boolean oldPromiseState;
                    IconInfo iconInfo = (IconInfo) info;
                    IconView iconView = (IconView) v;
                    Drawable oldIcon = iconView.getIcon();
                    if ((oldIcon instanceof PreloadIconDrawable) && ((PreloadIconDrawable) oldIcon).hasNotCompleted()) {
                        oldPromiseState = true;
                    } else {
                        oldPromiseState = false;
                    }
                    if (iconInfo.isPromise() == oldPromiseState) {
                        z = false;
                    }
                    iconView.applyFromApplicationInfo(iconInfo, z);
                    if (parent instanceof FolderIconView) {
                        if (isAlphabeticalMode) {
                            FolderInfo folderInfo = (FolderInfo) parent.getTag();
                            if (!(folderInfo == null || folderInfosToSort.contains(folderInfo))) {
                                folderInfosToSort.add(folderInfo);
                            }
                        } else if (info.rank < 9 && !folderIconsToRefresh.contains(parent)) {
                            folderIconsToRefresh.add((FolderIconView) parent);
                        }
                    }
                }
                return false;
            }
        });
        int i;
        if (isAlphabeticalMode) {
            for (i = 0; i < folderInfosToSort.size(); i++) {
                FolderInfo folderInfo = (FolderInfo) folderInfosToSort.get(i);
                if (folderInfo != null) {
                    folderInfo.setAlphabeticalOrder(true, true, this.mLauncher);
                }
            }
            return;
        }
        for (i = 0; i < folderIconsToRefresh.size(); i++) {
            FolderIconView folderIconView = (FolderIconView) folderIconsToRefresh.get(i);
            if (folderIconView != null) {
                folderIconView.refreshFolderIcon();
            }
        }
    }

    void removeApps(ArrayList<ItemInfo> apps) {
        if (apps == null) {
            Log.d(TAG, "removeApps - no items");
            return;
        }
        Log.d(TAG, "removeApps : " + apps.size());
        this.mRemoveInProgress = true;
        HashMap<Long, ArrayList<IconInfo>> folderItemMap = new HashMap();
        ArrayList<ItemInfo> removeItems = new ArrayList();
        Iterator it = apps.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (isItemInFolder(info)) {
                if (!folderItemMap.containsKey(Long.valueOf(info.container))) {
                    folderItemMap.put(Long.valueOf(info.container), new ArrayList());
                }
                ((ArrayList) folderItemMap.get(Long.valueOf(info.container))).add((IconInfo) info);
            } else {
                removeItems.add(info);
                if (isSelectState()) {
                    View v = getAppsIconByItemId(info.id);
                    if (v != null) {
                        this.mMultiSelectManager.removeCheckedApp(v);
                    }
                }
            }
        }
        boolean animate = this.mLauncher.isAppsStage() && this.mIsResumed;
        this.mReorderController.removeEmptyCellsAndViews(removeItems, animate);
        for (Long longValue : folderItemMap.keySet()) {
            long containerId = longValue.longValue();
            ArrayList itemsInContainer = (ArrayList) folderItemMap.get(Long.valueOf(containerId));
            View view = getAppsIconByItemId(containerId);
            if (view != null) {
                FolderInfo folderInfo = (FolderInfo) view.getTag();
                it = itemsInContainer.iterator();
                while (it.hasNext()) {
                    ((ItemInfo) it.next()).container = -102;
                }
                folderInfo.remove(itemsInContainer);
            }
        }
        if (getViewType() == ViewType.ALPHABETIC_GRID) {
            repositionByNormalizer(true);
        } else if (!removeEmptyPagesAndUpdateAllItemsInfo()) {
            updateDirtyItems();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                AppsController.this.mRemoveInProgress = false;
            }
        }, 200);
    }

    public void addItem(View view, ItemInfo item) {
        this.mAppsPagedView.addItem(view, item);
        checkIfConfigIsDifferentFromActivity();
    }

    private void removeAppsItem(CellLayout parentCell, View v) {
        if (parentCell != null) {
            parentCell.removeView(v);
        } else {
            Log.e(TAG, "mDragInfo.cell has null parent");
        }
        if (v instanceof DropTarget) {
            this.mDragMgr.removeDropTarget((DropTarget) v);
        }
    }

    @Deprecated
    private void removeAppsItem(ItemInfo info) {
        removeAppsItem(info, true);
    }

    @Deprecated
    private void removeAppsItem(ItemInfo info, boolean removeEmptyCellAndPage) {
        Log.d(TAG, "removeAppsItem : " + info);
        if (isItemInFolder(info) && getAppsIconByItemId(info.container) != null && (getAppsIconByItemId(info.container).getTag() instanceof FolderInfo)) {
            ((FolderInfo) getAppsIconByItemId(info.container).getTag()).remove((IconInfo) info);
            Log.d(TAG, "remove : " + info.toString());
            return;
        }
        CellLayout parentCell = this.mAppsPagedView.getCellLayout((int) info.screenId);
        if (parentCell != null) {
            removeAppsItem(parentCell, parentCell.getChildAt(info.rank % this.mAppsPagedView.getCellCountX(), info.rank / this.mAppsPagedView.getCellCountX()));
            if (removeEmptyCellAndPage && getViewType() != ViewType.ALPHABETIC_GRID) {
                this.mReorderController.removeEmptyCellAtPage(0, this.mAppsPagedView.getMaxItemsPerScreen() - 1, (int) info.screenId, false);
                if (!removeEmptyPagesAndUpdateAllItemsInfo()) {
                    updateDirtyItems();
                }
            }
        }
    }

    public boolean isItemInFolder(ItemInfo info) {
        return info.container != -102;
    }

    public void updateItemInDb(ItemInfo info) {
        ContentValues values = new ContentValues();
        info.onAddToDatabase(this.mLauncher, values);
        this.mAdapterProvider.updateItem(values, info);
    }

    public long addItemToDb(ItemInfo info, long container, long screenId, int cellX, int cellY) {
        info.container = container;
        info.screenId = screenId;
        info.cellX = cellX;
        info.cellY = cellY;
        return this.mAdapterProvider.addItem(info);
    }

    public void addItemToDb(ItemInfo info, long container, long screenId, int rank) {
        info.container = container;
        info.rank = rank;
        info.screenId = screenId;
        this.mAdapterProvider.addItem(info);
    }

    public void deleteItemFromDb(ItemInfo info) {
        this.mAdapterProvider.deleteItem(info);
    }

    public void modifyItemsInDb(ArrayList<ItemInfo> items, long container, int screen) {
        ArrayList<ContentValues> contentValues = new ArrayList();
        int count = items.size();
        for (int i = 0; i < count; i++) {
            ItemInfo item = (ItemInfo) items.get(i);
            item.container = container;
            item.screenId = (long) screen;
            ContentValues values = new ContentValues();
            values.put("container", Long.valueOf(item.container));
            values.put("cellX", Integer.valueOf(item.cellX));
            values.put("cellY", Integer.valueOf(item.cellY));
            values.put(BaseLauncherColumns.RANK, Integer.valueOf(item.rank));
            values.put("screen", Long.valueOf(item.screenId));
            contentValues.add(values);
        }
        this.mAdapterProvider.updateItemsInDatabaseHelper(this.mLauncher, contentValues, items);
    }

    private void modifyItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int rank) {
        modifyItemInDb(item, container, screenId, cellX, cellY, rank, item.hidden);
    }

    private void modifyItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int rank, int hidden) {
        item.container = container;
        item.rank = rank;
        item.cellX = cellX;
        item.cellY = cellY;
        item.screenId = screenId;
        item.hidden = hidden;
        item.mDirty = true;
        ContentValues values = new ContentValues();
        values.put("container", Long.valueOf(item.container));
        values.put(BaseLauncherColumns.RANK, Integer.valueOf(item.rank));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("screen", Long.valueOf(item.screenId));
        values.put("hidden", Integer.valueOf(item.hidden));
        this.mAdapterProvider.updateItem(values, item);
    }

    public void addOrMoveItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int rank) {
        if (item.container == -1 || item.id == -1) {
            long folderId = addItemToDb(item, container, screenId, cellX, cellY);
            if (item instanceof FolderInfo) {
                FolderInfo folderInfo = (FolderInfo) item;
                ArrayList<IconInfo> appList = new ArrayList();
                Iterator it = folderInfo.contents.iterator();
                while (it.hasNext()) {
                    IconInfo info = (IconInfo) it.next();
                    addFolderChildItemToDb(info, folderId);
                    appList.add(info);
                }
                folderInfo.contents.clear();
                folderInfo.contents.addAll(appList);
                appList.clear();
                return;
            }
            return;
        }
        modifyItemInDb(item, container, screenId, cellX, cellY, rank);
    }

    public void deleteFolder(FolderInfo folderInfo) {
        ArrayList<IconInfo> contents = folderInfo.contents;
        removeAppsItem((ItemInfo) folderInfo, false);
        deleteItemFromDb(folderInfo);
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_DELETE_APPS_FOLDER, null, -1, false);
        ArrayList<ItemInfo> addItems = new ArrayList();
        int i;
        if (isAlphabeticalMode()) {
            for (i = 0; i < contents.size(); i++) {
                IconInfo iconInfo = (IconInfo) contents.get(i);
                iconInfo.container = -102;
                iconInfo.screenId = -1;
                iconInfo.rank = -1;
                iconInfo.mDirty = true;
                updateItemInDb(iconInfo);
                addItems.add(iconInfo);
            }
            normalizeWithExtraItems(addItems, null);
            this.mAppsPagedView.rearrangeAllViews(true);
            Iterator it = addItems.iterator();
            while (it.hasNext()) {
                ItemInfo itemInfo = (ItemInfo) it.next();
                addItem(createItemView(itemInfo, null, null), itemInfo);
            }
            return;
        }
        DragAppIcon dragAppIcon = new DragAppIcon();
        dragAppIcon.screenId = folderInfo.screenId;
        dragAppIcon.rank = folderInfo.rank;
        this.mReorderController.removeEmptyCell(dragAppIcon);
        for (i = 0; i < contents.size(); i++) {
            this.mAppsPagedView.addItemToLastPosition((IconInfo) contents.get(i));
        }
        if (!removeEmptyPagesAndUpdateAllItemsInfo()) {
            updateDirtyItems();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                AppsController.this.mAppsPagedView.snapToPageImmediately(AppsController.this.mAppsPagedView.getPageCount() - 1);
            }
        }, 500);
    }

    public void moveItemFromFolder(final IconInfo iconInfo) {
        FolderInfo folderInfo = (FolderInfo) this.mAdapterProvider.getItemById(iconInfo.container);
        if (folderInfo == null) {
            Log.e(TAG, "Adapter doesn't have an folder which extract item.");
            return;
        }
        folderInfo.remove(iconInfo);
        if (isAlphabeticalMode()) {
            iconInfo.container = -102;
            iconInfo.screenId = -1;
            iconInfo.rank = -1;
            iconInfo.mDirty = true;
            updateItemInDb(iconInfo);
            ArrayList<ItemInfo> addItems = new ArrayList();
            addItems.add(iconInfo);
            normalizeWithExtraItems(addItems, null);
            this.mAppsPagedView.rearrangeAllViews(false);
            addItem(createItemView(iconInfo, null, null), iconInfo);
        } else {
            this.mAppsPagedView.addItemToLastPosition(iconInfo);
            if (!removeEmptyPagesAndUpdateAllItemsInfo()) {
                updateDirtyItems();
            }
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                AppsController.this.mLauncher.closeFolder();
                AppsController.this.mAppsPagedView.snapToPageImmediately((int) iconInfo.screenId);
            }
        }, 500);
    }

    public void replaceFolderWithFinalItem(ItemInfo info, int itemCount, View folderIcon) {
        if (info instanceof FolderInfo) {
            FolderInfo folderInfo = (FolderInfo) info;
            CellLayout cellLayout = this.mAppsPagedView.getCellLayout((int) folderInfo.screenId);
            View child = null;
            IconInfo finalItem = null;
            if (itemCount <= 1) {
                deleteItemFromDb(folderInfo);
                if (cellLayout != null) {
                    cellLayout.removeView(folderIcon);
                }
                if (folderIcon instanceof DropTarget) {
                    this.mDragMgr.removeDropTarget((DropTarget) folderIcon);
                }
            }
            if (itemCount == 1 && folderInfo != null) {
                finalItem = (IconInfo) folderInfo.contents.get(0);
                child = createItemView(finalItem, cellLayout, null);
                if (isAlphabeticalMode()) {
                    addOrMoveItemInDb(finalItem, folderInfo.container, -1, -1, -1, -1);
                    finalItem.screenId = folderInfo.screenId;
                    finalItem.cellX = folderInfo.cellX;
                    finalItem.cellY = folderInfo.cellY;
                    finalItem.rank = folderInfo.rank;
                } else {
                    addOrMoveItemInDb(finalItem, folderInfo.container, folderInfo.screenId, folderInfo.cellX, folderInfo.cellY, folderInfo.rank);
                }
            }
            if (itemCount == 0 && !isAlphabeticalMode()) {
                this.mReorderController.realTimeReorder(0, 0.0f, folderInfo.rank, this.mAppsPagedView.getItemCountPageAt((int) folderInfo.screenId), 1, (int) info.screenId);
            }
            if (isAlphabeticalMode()) {
                ArrayList<ItemInfo> addItems = new ArrayList();
                if (finalItem != null) {
                    addItems.add(finalItem);
                }
                normalizeWithExtraItems(addItems, null);
                this.mAppsPagedView.rearrangeAllViews(true);
                this.mAppsPagedView.removeEmptyScreen();
            }
            if (!(child == null || finalItem == null)) {
                addItem(child, finalItem);
            }
            if (!isAlphabeticalMode()) {
                updateDirtyItems();
            }
        }
    }

    public TrayLevel getTrayLevel() {
        return TrayLevel.Underground;
    }

    public float getTrayScale() {
        return this.mAppsContainer.getScaleY();
    }

    public DropTarget getDropTarget() {
        return this.mDragController;
    }

    public void onReceiveTrayEvent(TrayEvent event) {
        switch (event.mEventType) {
            case 2:
                updateAppsViewByTrayPosition(event.mValue, event.mDisallowVisible);
                return;
            case 4:
                if (this.mLauncher.isHomeStage()) {
                    onStagePreEnter();
                }
                if (this.mAppsContainer.getVisibility() != 0) {
                    this.mAppsContainer.setVisibility(View.VISIBLE);
                    this.mAppsContainer.setAlpha(0.0f);
                }
                this.mAppsPagedView.updateOnlyCurrentPage(true);
                this.mHwLayerPageIndexWhileTray = this.mAppsPagedView.updateChildrenLayersEnabled(-1, true);
                return;
            case 5:
                if (((int) event.mValue) == 1) {
                    this.mAppsContainer.setVisibility(View.GONE);
                }
                this.mAppsPagedView.updateOnlyCurrentPage(false);
                this.mAppsPagedView.updateChildrenLayersEnabled(this.mHwLayerPageIndexWhileTray, false);
                this.mHwLayerPageIndexWhileTray = -1;
                return;
            default:
                return;
        }
    }

    public boolean determineStageChange(int velocity, float offset, float firstDownY, float lastDownY, int minSnapVelocity) {
        boolean swipeUp;
        boolean toBeChanged = false;
        if (offset > 0.0f) {
            swipeUp = true;
        } else {
            swipeUp = false;
        }
        if (this.mLauncher.isAppsStage()) {
            int range;
            if (this.mTrayManager != null) {
                range = this.mTrayManager.getTrayMovingRange();
            } else {
                range = Utilities.getFullScreenHeight(this.mLauncher);
            }
            if (offset == 0.0f) {
                toBeChanged = true;
            } else if (offset > 0.0f) {
                toBeChanged = (firstDownY > lastDownY && velocity < 0 && Math.abs(velocity) >= minSnapVelocity) || (Math.abs(velocity) < minSnapVelocity && offset <= ((float) range) * this.mPageSnapMovingRatioOnApps);
            } else {
                toBeChanged = (firstDownY < lastDownY && velocity > 0 && Math.abs(velocity) >= minSnapVelocity) || (Math.abs(velocity) < minSnapVelocity && (-offset) <= ((float) range) * this.mPageSnapMovingRatioOnApps);
            }
            if (toBeChanged) {
                SALogging.getInstance().insertEventLog(this.mLauncher.getResources().getString(R.string.screen_Apps_2xxx), this.mLauncher.getResources().getString(R.string.event_Apps_Home), swipeUp ? "3" : "4");
            }
        }
        return toBeChanged;
    }

    public void startTrayMove() {
        if (Talk.INSTANCE.isAccessibilityEnabled()) {
            this.mAppsContainer.semClearAccessibilityFocus();
        }
    }

    public boolean canMoveTray() {
        return this.mLauncher.isAppsStage() && this.mState == 0;
    }

    private void updateAppsViewByTrayPosition(float offsetY, boolean disallowVisible) {
        float toTranslationY;
        this.mAppsContainer.setDrawBoundaryY(offsetY, false, disallowVisible);
        float borderY = offsetY + ((float) this.mAppsContainer.getHeight());
        if (this.mTrayManager != null) {
            float factor;
            if (this.mDownwardFadeOutEnd == 0 || this.mUpwardFadeOutEnd == 0) {
                int fadeOutEnd = (int) (((float) this.mTrayManager.getTrayMovingRange()) * 0.9f);
                this.mDownwardFadeOutEnd = -fadeOutEnd;
                this.mUpwardFadeOutEnd = fadeOutEnd;
                this.mFadeOutRange = (float) (this.mUpwardFadeOutEnd - this.mUpwardFadeOutStart);
            }
            if (offsetY > 0.0f) {
                toTranslationY = offsetY - ((float) this.mTrayManager.getTrayMovingRange());
                if (toTranslationY >= ((float) this.mDownwardFadeOutStart)) {
                    factor = 1.0f;
                } else if (toTranslationY >= ((float) this.mDownwardFadeOutEnd)) {
                    factor = Math.min(1.0f, (toTranslationY - ((float) this.mDownwardFadeOutEnd)) / this.mFadeOutRange);
                } else {
                    factor = 0.0f;
                }
            } else {
                toTranslationY = offsetY + ((float) this.mTrayManager.getTrayMovingRange());
                if (toTranslationY <= ((float) this.mUpwardFadeOutStart)) {
                    factor = 1.0f;
                } else if (toTranslationY <= ((float) this.mUpwardFadeOutEnd)) {
                    factor = Math.min(1.0f, (((float) this.mUpwardFadeOutEnd) - toTranslationY) / this.mFadeOutRange);
                } else {
                    factor = 0.0f;
                }
            }
            Utilities.simplifyDecimalFraction(factor, 2, 2);
            this.mAppsContainer.setAlpha(Math.max(0.0f, 1.0f - ((1.0f - factor) * this.mAppsAlphaRatio)));
            float scale = this.mAppsShrinkFactor + ((1.0f - this.mAppsShrinkFactor) * factor);
            float bgAlpha = (float) Math.sqrt((double) (getBackgroundImageAlphaForState(this.mState) * factor));
            this.mAppsContainer.setScaleX(scale);
            this.mAppsContainer.setScaleY(scale);
            if (!disallowVisible) {
                this.mDragLayer.setBackgroundImageAlpha(bgAlpha);
            }
            if (this.mAppsPageIndicatorView != null) {
                float indicatorScale = this.mPageIndicatorShrinkFactor + (this.mPageIndicatorShrinkFactor * Math.max(0.0f, 1.0f - ((1.0f - factor) * this.mPageIndicatorScaleRatio)));
                this.mAppsPageIndicatorView.setScaleX(indicatorScale);
                this.mAppsPageIndicatorView.setScaleY(indicatorScale);
            }
        } else {
            int appsSlipStart = this.mMoveToHomeApproachingStart;
            toTranslationY = ((float) this.mAppsSlipY) * Math.max((borderY - ((float) appsSlipStart)) / ((float) (this.mAppsContainer.getHeight() - appsSlipStart)), 0.0f);
        }
        this.mAppsContainer.setTranslationY(toTranslationY);
    }

    private void showMoveToHomePanel(boolean showPanel) {
        if (this.mTrayManager == null) {
            return;
        }
        if (showPanel) {
            this.mTrayManager.pullTrayForDrag(this, this.mAppsContainer.getHeight());
        } else {
            this.mTrayManager.releaseTrayForDrag(this, this.mAppsContainer.getHeight());
        }
    }

    void onOptionSelectedSearchRecommend() {
        Intent intent = new Intent(this.mAppsPagedView.getContext(), AppSearchSettingActivity.class);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                AppsController.this.searchSettingCheck();
                AppsController.this.changeState(3, false);
            }
        }, 700);
        this.mAppsPagedView.getContext().startActivity(intent);
    }

    public void updateDirtyItems() {
        this.mAdapterProvider.updateDirtyItems();
    }

    private boolean isTidyState() {
        return this.mState == 4;
    }

    private boolean isGridState() {
        return this.mState == 5;
    }

    public FolderIconView addFolder(CellLayout layout, long screenId, int cellX, int cellY, int rank) {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = this.mLauncher.getText(R.string.folder_name);
        folderInfo.rank = rank;
        try {
            folderInfo.id = FavoritesProvider.getInstance().generateNewItemId();
        } catch (Exception e) {
            Log.e(TAG, "generate new item id for created folder is failed.");
            e.printStackTrace();
        }
        if (isAlphabeticalMode()) {
            addItemToDb(folderInfo, -102, -1, -1, -1);
            folderInfo.mDirty = true;
            folderInfo.container = -102;
            folderInfo.screenId = screenId;
            folderInfo.cellX = cellX;
            folderInfo.cellY = cellY;
        } else {
            addItemToDb(folderInfo, -102, screenId, cellX, cellY);
        }
        FolderIconView newFolder = FolderIconView.fromXml(this.mLauncher, layout, folderInfo, this, this.mLauncher, this, 2);
        newFolder.setOnFocusChangeListener(this.mAppsFocusListener);
        newFolder.setOnKeyListener(this.mAppsFocusListener);
        addItem(newFolder, folderInfo);
        layout.getCellLayoutChildren().measureChild(newFolder);
        return newFolder;
    }

    private void addFolderChildItemToDb(ItemInfo info, long container) {
        info.container = container;
        this.mAdapterProvider.addItem(info);
    }

    private View getFirstMatch(final ItemOperator operator) {
        final View[] value = new View[1];
        this.mAppsPagedView.mapOverItems(false, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (!operator.evaluate(info, v, parent)) {
                    return false;
                }
                value[0] = v;
                return true;
            }
        });
        return value[0];
    }

    public View getAppsIconByItemId(final long id) {
        return getFirstMatch(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info != null && info.id == id;
            }
        });
    }

    public void onBadgeBindingCompleted(ArrayList<ItemInfo> badgeItems) {
        if (!badgeItems.isEmpty()) {
            updateBadgeItems(badgeItems);
        }
    }

    public void prepareTidedUpPages() {
        if (getOrganizeAppsAlertEnable()) {
            new OrganizeAppsConfirmDialog().show(this.mLauncher.getFragmentManager(), this);
        } else if (this.mAppsPagedView.hasEmptyCellAtPages()) {
            changeState(4, true);
        } else {
            Toast.makeText(this.mLauncher, R.string.no_changes, 0).show();
        }
    }

    private void repositionByTypeUpPages() {
        this.mAdapterProvider.setStateAndUpdateLock(4, true);
        repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 2, false);
    }

    private void applyOrCancelTideUpPages() {
        Resources res = this.mLauncher.getResources();
        this.mAdapterProvider.setStateAndUpdateLock(0, false);
        if (this.mApplyTideUpPage) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_CleanUpPages), res.getString(R.string.event_Apps_CleanUp_Apply));
        } else {
            this.mAppsPagedView.setCurrentPage(0);
            this.mAdapterProvider.reloadAllItemsFromDB(true);
            repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 2, false);
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_CleanUpPages), res.getString(R.string.event_Apps_CleanUp_Cancel));
        }
        updateDirtyItems();
        this.mApplyTideUpPage = false;
    }

    public void startContactUs() {
        Utilities.startContactUsActivity(this.mLauncher);
    }

    public void startHomeScreenSetting() {
        Log.d(TAG, "onClickHomeSettings");
        this.mLauncher.startHomeSettingActivity();
    }

    public void startSfinderSettingActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.samsung.android.app.galaxyfinder", "com.samsung.android.app.galaxyfinder.GalaxyFinderActivity"));
        intent.putExtra("from", this.mLauncher.getPackageName());
        intent.putExtra("launch_mode", "launched_settings");
        intent.setFlags(268468224);
        try {
            this.mLauncher.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to start S Finder settings.  intent=" + intent, e);
        }
    }

    public void startGalaxyEssentials() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setClassName("com.sec.android.app.samsungapps", "com.sec.android.app.samsungapps.interim.essentials.InterimEssentialsActivity");
        try {
            this.mLauncher.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to start GalaxyEssentials");
        }
    }

    public void onRefreshLiveIcon() {
        boolean isHomeOnly = LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        if (!isHomeOnly) {
            this.mAppsPagedView.updateLiveIcon();
        }
    }

    public void onChangeSelectMode(boolean enter, boolean animated) {
        if (!this.mLauncher.isAppsStage()) {
            return;
        }
        if (enter) {
            changeState(2, animated);
            Talk.INSTANCE.postSay(this.mLauncher.getResources().getString(R.string.tts_changed_to_apps_edit_mode) + " " + String.format(this.mLauncher.getResources().getString(R.string.default_scroll_format), new Object[]{Integer.valueOf(this.mAppsPagedView.getCurrentPage() + 1), Integer.valueOf(this.mAppsPagedView.getPageCount())}));
            return;
        }
        this.mMultiSelectManager.clearCheckedApps();
    }

    public void onCheckedChanged(View view, boolean isChecked) {
        if (isChecked) {
            this.mMultiSelectManager.addCheckedApp(view, this.mDragController);
        } else {
            this.mMultiSelectManager.removeCheckedApp(view);
        }
    }

    public void onClickMultiSelectPanel(int id) {
        if (this.mLauncher.isAppsStage() || (this.mLauncher.isFolderStage() && this.mLauncher.getSecondTopStageMode() == 2)) {
            switch (id) {
                case 2:
                    createFolder();
                    break;
            }
            changeState(0, true, true);
        }
    }

    private void createFolder() {
        ArrayList<View> appsViewList = this.mMultiSelectManager.getCheckedAppsViewList();
        if (appsViewList != null && appsViewList.size() > 0) {
            View targetView = getTargetView(appsViewList);
            if (targetView != null) {
                IconInfo targetItem = (IconInfo) targetView.getTag();
                CellLayout targetCellLayout = (CellLayout) targetView.getParent().getParent();
                if (targetItem != null) {
                    Log.d(TAG, "Create folder with target item's position. target item is " + targetItem.title);
                    int delayToOpenFolder = this.mLauncher.isFolderStage() ? this.mLauncher.getResources().getInteger(R.integer.config_folderCloseDuration) : 0;
                    int toPage = (int) targetItem.screenId;
                    ArrayList<IconInfo> folderItemsList = removeCheckedAppView(appsViewList);
                    CellLayout page = this.mAppsPagedView.getCellLayout(toPage);
                    if (page != null) {
                        page.removeView(targetView);
                    }
                    final FolderIconView folder = getDragController().addFolder(targetCellLayout, targetItem);
                    if (folderItemsList.size() > 0) {
                        removeCheckedAppViewFromFolder(folderItemsList);
                    }
                    ArrayList<IconInfo> items = new ArrayList();
                    Iterator it = appsViewList.iterator();
                    while (it.hasNext()) {
                        items.add((IconInfo) ((View) it.next()).getTag());
                    }
                    folder.addItems(items);
                    if (isAlphabeticalMode()) {
                        repositionByNormalizer(true);
                        toPage = (int) folder.getFolderInfo().screenId;
                    }
                    removeEmptyPagesAndUpdateAllItemsInfo();
                    final int snapToPage = toPage;
                    final Runnable runnable = new Runnable() {
                        public void run() {
                            if (snapToPage != AppsController.this.mAppsPagedView.getNextPage()) {
                                AppsController.this.mAppsPagedView.snapToPage(snapToPage);
                            }
                        }
                    };
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            StageEntry data = new StageEntry();
                            data.addOnCompleteRunnableCallBack(runnable);
                            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folder);
                            AppsController.this.getStageManager().startStage(5, data);
                        }
                    }, (long) delayToOpenFolder);
                    return;
                }
                Log.e(TAG, "onClickCreateFolder : app info is null");
            }
        }
    }

    private View getTargetView(ArrayList<View> appsViewList) {
        View targetView = null;
        ItemInfo targetItem = null;
        if (appsViewList != null) {
            int i;
            boolean isItemInFolder = true;
            for (i = appsViewList.size() - 1; i >= 0; i--) {
                targetView = (View) appsViewList.get(i);
                targetItem = (ItemInfo) targetView.getTag();
                if (!isItemInFolder(targetItem)) {
                    isItemInFolder = false;
                    break;
                }
            }
            if (isItemInFolder) {
                int empty = -1;
                int orderablePage = this.mAppsPagedView.getNextPage();
                int pageCount = this.mAppsPagedView.getPageCount();
                for (i = this.mAppsPagedView.getNextPage(); i < pageCount; i++) {
                    empty = this.mAppsPagedView.findFirstEmptyCell(i);
                    if (empty >= 0) {
                        orderablePage = i;
                        break;
                    }
                }
                if (empty == -1) {
                    this.mAppsPagedView.createAppsPage();
                }
                CellLayout cl = this.mAppsPagedView.getCellLayout(orderablePage);
                targetView = createItemView(targetItem, cl, null);
                if (targetView != null) {
                    targetItem = (ItemInfo) targetView.getTag();
                    targetItem.screenId = (long) orderablePage;
                    targetItem.rank = cl.getPageChildCount();
                    targetItem.setChecked(false);
                    addItem(targetView, targetItem);
                }
            }
        }
        return targetView;
    }

    private ArrayList<IconInfo> removeCheckedAppView(ArrayList<View> appsViewList) {
        ArrayList<IconInfo> folderItemsList = new ArrayList();
        Iterator it = appsViewList.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) ((View) it.next()).getTag();
            if (item != null && isItemInFolder(item)) {
                folderItemsList.add((IconInfo) item);
            }
        }
        return folderItemsList;
    }

    private void removeCheckedAppViewFromFolder(ArrayList<IconInfo> appsViewList) {
        HashMap<Long, ArrayList<IconInfo>> itemMap = new HashMap();
        Iterator it = appsViewList.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (!itemMap.containsKey(Long.valueOf(item.container))) {
                itemMap.put(Long.valueOf(item.container), new ArrayList());
            }
            ((ArrayList) itemMap.get(Long.valueOf(item.container))).add((IconInfo) item);
        }
        for (Long longValue : itemMap.keySet()) {
            long containerId = longValue.longValue();
            ArrayList itemsInContainer = (ArrayList) itemMap.get(Long.valueOf(containerId));
            if (containerId > 0) {
                FolderIconView iconView = (FolderIconView) getAppsIconByItemId(containerId);
                if (iconView != null) {
                    iconView.getFolderInfo().remove(itemsInContainer);
                } else {
                    Log.w(TAG, "folder iconview is null");
                }
            }
        }
    }

    private void searchSettingCheck() {
        LauncherFeature.setSupportGalaxyAppsSearch(PreferenceManager.getDefaultSharedPreferences(this.mAppsPagedView.getContext()).getBoolean("search_recommend", true));
    }

    public void onUpdateAlphabetList(ItemInfo item) {
        if (item instanceof FolderInfo) {
            final FolderInfo folderItem = (FolderInfo) item;
            if (getViewType() == ViewType.ALPHABETIC_GRID) {
                repositionByNormalizer(false);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        AppsController.this.mAppsPagedView.snapToPageImmediately((int) folderItem.screenId);
                    }
                }, 50);
            }
        }
    }

    public ItemInfo getLocationInfoFromDB(ItemInfo item) {
        AppsModel appsModel = this.mLauncher.getLauncherModel().getAppsModel();
        if (item instanceof FolderInfo) {
            return appsModel.getLocationInfoFromDB(item);
        }
        return null;
    }

    public void notifyControllerItemsChanged() {
        repositionByNormalizer(true);
    }

    public void repositionByNormalizer(boolean animate) {
        repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 0, animate);
    }

    public void notifyCapture(boolean immediate) {
    }

    public boolean recoverCancelItemForFolderLock(IconInfo info, long container, long screenId, int cellX, int cellY, int rank) {
        int page = (int) screenId;
        CellLayout cl = (CellLayout) this.mAppsPagedView.getPageAt(page);
        View v;
        if (info.container != -102) {
            View folderIconView = getAppsIconByItemId(info.container);
            if (folderIconView != null) {
                FolderInfo folderObject = folderIconView.getTag();
                if (folderObject instanceof FolderInfo) {
                    folderObject.add(info);
                }
            } else {
                v = createItemView(info, cl, null);
                int[] cellXY = new int[]{cellX, cellY};
                DragObject dragObject = new DragObject();
                dragObject.dragInfo = info;
                getDragController().createUserFolderIfNecessary(cl, cellXY, v, dragObject, cl.getChildAt(rank));
            }
        } else {
            info.rank = rank;
            info.screenId = screenId;
            v = createItemView(info, cl, null);
            info.mDirty = true;
            addItem(v, info);
            this.mReorderController.realTimeReorder(0, 0.0f, this.mAppsPagedView.getItemCountPageAt(page) - 1, info.rank, -1, page);
        }
        return true;
    }

    public void moveOutItemsFromLockedFolder(FolderInfo folder, ArrayList<IconInfo> arrayList, ArrayList<IconInfo> appsNeedUpdateInfos) {
        if (folder.isContainApps()) {
            for (int index = 0; index < appsNeedUpdateInfos.size(); index++) {
                IconInfo info = (IconInfo) appsNeedUpdateInfos.get(index);
                folder.remove(info);
                if (info.id == -1) {
                    info.id = FavoritesProvider.getInstance().generateNewItemId();
                }
                info.container = -102;
                long screenId = folder.screenId;
                int screenCount = getAppsPagedView().getChildCount();
                int folderScreen = (int) screenId;
                boolean found = false;
                while (folderScreen < screenCount) {
                    if (this.mAppsPagedView.getItemCountPageAt(folderScreen) < this.mAppsPagedView.getCellCountX() * this.mAppsPagedView.getCellCountY()) {
                        found = true;
                        break;
                    }
                    folderScreen++;
                }
                if (folder.contents.size() < 1) {
                    info.cellX = folder.cellX;
                    info.cellY = folder.cellY;
                    info.screenId = folder.screenId;
                    info.rank = folder.rank;
                    removeAppsItem(folder);
                } else if (found) {
                    info.screenId = (long) folderScreen;
                    info.rank = this.mAppsPagedView.getItemCountPageAt(folderScreen);
                } else {
                    this.mAppsPagedView.createAppsPage();
                    info.screenId = (long) screenCount;
                    info.rank = 0;
                }
                addItem(createItemView(info, (CellLayout) this.mAppsPagedView.getChildAt((int) info.screenId), null), info);
                updateItemInDb(info);
            }
        }
    }

    public View getFolderIconView(FolderInfo folder) {
        if (folder.isContainApps()) {
            return getAppsIconByItemId(folder.id);
        }
        return null;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 34:
                    if (event.isCtrlPressed() && this.mAppsSearch.launchSfinder()) {
                        return true;
                    }
                case 82:
                    if (!this.mAppsSearch.showPopupMenu() || !LauncherFeature.supportQuickOption() || !this.mLauncher.getDragMgr().isQuickOptionShowing()) {
                        return true;
                    }
                    this.mDragMgr.removeQuickOptionView("3");
                    return true;
                case 84:
                    if (this.mAppsSearch.launchSfinder()) {
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void addOrMoveItems(ArrayList<IconInfo> items, long container, long screenId) {
        Log.d(TAG, "addOrMoveItems");
        ArrayList<ItemInfo> removeItemsFromApps = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            IconInfo item = (IconInfo) it.next();
            if (!isItemInFolder(item)) {
                removeItemsFromApps.add(item);
            }
        }
        this.mReorderController.removeEmptyCellsAndViews(removeItemsFromApps);
        HashMap<Long, ArrayList<IconInfo>> itemMap = new HashMap();
        it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item2 = (ItemInfo) it.next();
            if (!itemMap.containsKey(Long.valueOf(item2.container))) {
                itemMap.put(Long.valueOf(item2.container), new ArrayList());
            }
            ((ArrayList) itemMap.get(Long.valueOf(item2.container))).add((IconInfo) item2);
        }
        Set<Long> keys = itemMap.keySet();
        ArrayList<ItemInfo> updateItems = new ArrayList();
        ArrayList<ContentValues> contentValues = new ArrayList();
        for (Long longValue : keys) {
            long containerId = longValue.longValue();
            ArrayList itemsInContainer = (ArrayList) itemMap.get(Long.valueOf(containerId));
            if (containerId != container && containerId > 0) {
                View view = getAppsIconByItemId(containerId);
                if (view == null) {
                    Log.w(TAG, "folder iconview is null");
                } else if (getState() != 1) {
                    ((FolderInfo) view.getTag()).remove(itemsInContainer);
                }
            }
            it = itemsInContainer.iterator();
            while (it.hasNext()) {
                item = (IconInfo) it.next();
                item.container = container;
                item.screenId = screenId;
                item.mDirty = true;
                ContentValues values = new ContentValues();
                values.put("container", Long.valueOf(item.container));
                values.put(BaseLauncherColumns.RANK, Integer.valueOf(item.rank));
                values.put("cellX", Integer.valueOf(item.cellX));
                values.put("cellY", Integer.valueOf(item.cellY));
                values.put("screen", Long.valueOf(item.screenId));
                values.put("hidden", Integer.valueOf(item.hidden));
                updateItems.add(item);
                contentValues.add(values);
            }
        }
        this.mAdapterProvider.updateItemsInDatabaseHelper(this.mLauncher, contentValues, updateItems);
        if (getViewType() == ViewType.ALPHABETIC_GRID) {
            repositionByNormalizer(true);
            return;
        }
        if (getState() != 1) {
            removeEmptyPagesAndUpdateAllItemsInfo();
        }
        updateDirtyItems();
    }

    public int getPageIndexForDragView(ItemInfo item) {
        if (item != null) {
            return this.mDragController.getPageIndexForDragView(item);
        }
        return this.mAppsPagedView.getNextPage();
    }

    private void findSearchedApp(ComponentName cmp, UserHandle user) {
        Iterator it = this.mLauncher.getLauncherModel().getAppsModel().getTopLevelItemsInApps().iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            CellLayout parentCell = this.mAppsPagedView.getCellLayout((int) item.screenId);
            if (parentCell != null) {
                if (item.itemType == 0) {
                    if (item.componentName.equals(cmp) && item.getUserHandle().getUser().equals(user)) {
                        this.mAppsPagedView.snapToPage((int) item.screenId);
                        startBounceAnimationForSearchedApp(parentCell.getChildAt(item.rank % this.mAppsPagedView.getCellCountX(), item.rank / this.mAppsPagedView.getCellCountX()));
                        return;
                    }
                } else if (item.itemType == 2) {
                    FolderInfo folderInfo = (FolderInfo) item;
                    Iterator it2 = folderInfo.contents.iterator();
                    while (it2.hasNext()) {
                        IconInfo info = (IconInfo) it2.next();
                        if (info.componentName.equals(cmp) && info.getUserHandle().getUser().equals(user)) {
                            FolderIconView folderIconView = (FolderIconView) parentCell.getChildAt(item.rank % this.mAppsPagedView.getCellCountX(), item.rank / this.mAppsPagedView.getCellCountX());
                            final StageEntry data = new StageEntry();
                            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folderIconView);
                            data.putExtras(FolderController.KEY_FOLDER_ICON_APPSEARCHED, info);
                            this.mAppsPagedView.snapToPage((int) folderInfo.screenId);
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    AppsController.this.mLauncher.getStageManager().startStage(5, data);
                                }
                            }, 1000);
                            return;
                        }
                    }
                    continue;
                } else {
                    continue;
                }
            }
        }
    }

    public void initBounceAnimation() {
        if (this.mBounceAnimation != null) {
            this.mBounceAnimation.stop();
            this.mBounceAnimation = null;
        }
    }

    private void startBounceAnimationForSearchedApp(View bounceView) {
        if (bounceView != null) {
            this.mBounceAnimation = new SearchedAppBounceAnimation(bounceView, this.mLauncher.getDeviceProfile().isLandscape);
            this.mBounceAnimation.animate();
        }
    }

    public void onConfigurationChangedIfNeeded() {
        long cur = System.currentTimeMillis();
        if (this.mLauncher.isAppsStage()) {
            Utilities.hideStatusBar(this.mLauncher.getWindow(), !supportStatusBarForState(this.mState));
        }
        if (LauncherFeature.isTablet()) {
            repositionByConfiguration();
        }
        this.mAppsSearch.onConfigurationChangedIfNeeded();
        if (LauncherFeature.supportFlexibleGrid()) {
            if (isGridState()) {
                int[] storedGrid = new int[2];
                ScreenGridUtilities.loadCurrentAppsGridSize(this.mLauncher, storedGrid);
                int[] selectedGrid = this.mAppsScreenGridPanel.getSelectedGrid();
                if (!Arrays.equals(storedGrid, selectedGrid)) {
                    Log.d(TAG, "Restore selected apps grid option onConfigurationChanged");
                    this.mLauncher.getDeviceProfile().setAppsCurrentGrid(selectedGrid[0], selectedGrid[1]);
                }
            }
            this.mAppsScreenGridPanel.onConfigurationChangedIfNeeded();
        }
        this.mAppsPagedView.onConfigurationChangedIfNeeded();
        Log.d(TAG, "onConfigurationChangedIfNeeded consumed : " + (System.currentTimeMillis() - cur));
        if (this.mDragMgr.isQuickOptionShowing()) {
            this.mDragMgr.removeQuickOptionView();
        }
        updateTidyUpContainerLayout();
        LightingEffectManager.INSTANCE.setLightingImage(this.mLauncher);
    }

    public boolean removeEmptyPagesAndUpdateAllItemsInfo() {
        boolean pageRemoved = this.mAppsPagedView.removeEmptyScreen();
        this.mAppsPagedView.removeExtraEmptyScreen();
        if (!pageRemoved || isAlphabeticalMode()) {
            return false;
        }
        normalizeWithExtraItems(null, null);
        updateDirtyItems();
        return true;
    }

    public static String getViewTypeFromSharedPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs != null) {
            return prefs.getString(APPS_VIEW_TYPE, ViewType.CUSTOM_GRID.name());
        }
        return null;
    }

    public static void setViewTypeFromSharedPreference(Context context, ViewType viewType) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs != null) {
            Editor editor = prefs.edit();
            editor.putString(APPS_VIEW_TYPE, viewType.name());
            editor.apply();
        }
    }

    public void setOrganizeAppsAlertEnable(boolean set) {
        Editor editor = this.mLauncher.getSharedPrefs().edit();
        editor.putBoolean(APPS_ORGANIZE_APPS_ALERT, set);
        editor.apply();
    }

    private boolean getOrganizeAppsAlertEnable() {
        return this.mLauncher.getSharedPrefs().getBoolean(APPS_ORGANIZE_APPS_ALERT, true);
    }

    public void repositionBy(StageEntry entry) {
        if (this.mState == 4) {
            repositionByTypeUpPages();
        } else if (this.mState == 0) {
            int repostionBy = ((Integer) entry.getExtras(KEY_REPOSITION_BY, Integer.valueOf(0))).intValue();
            if (repostionBy == 2) {
                applyOrCancelTideUpPages();
            } else if (repostionBy == 0) {
                applySetViewType();
            }
        } else if (this.mState == 5) {
            repositionByGrid((int[]) entry.getExtras(KEY_TARGET_GRID_SIZE, Integer.valueOf(0)));
        }
    }

    public ArrayList<View> prepareViewsForReposition() {
        ArrayList<View> views = new ArrayList();
        int pageCount = this.mAppsPagedView.getPageCount();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            CellLayout cl = this.mAppsPagedView.getCellLayout(pageIndex);
            int itemCount = cl.getPageChildCount();
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                View view = cl.getChildOnPageAt(itemIndex);
                if (view instanceof IconView) {
                    views.add(view);
                }
            }
        }
        return views;
    }

    private void repositionByNormalizer(Normalizer<Object> normalizer, int repositionBy, boolean animate) {
        Log.d(TAG, "repositionByNormalizer start : " + repositionBy);
        long cur = System.currentTimeMillis();
        ArrayList<View> iconViews = new ArrayList();
        HashMap<FolderInfo, View> folderViewMap = new HashMap();
        int pageCount = this.mAppsPagedView.getPageCount();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            CellLayout cl = this.mAppsPagedView.getCellLayout(pageIndex);
            int childCount = cl.getPageChildCount();
            for (int itemIndex = 0; itemIndex < childCount; itemIndex++) {
                View view = cl.getChildOnPageAt(itemIndex);
                if ((view instanceof Removable) && !((Removable) view).isMarkToRemove()) {
                    iconViews.add(view);
                    if (view.getTag() instanceof FolderInfo) {
                        folderViewMap.put((FolderInfo) view.getTag(), view);
                    }
                }
            }
        }
        Log.d(TAG, "reposition - makeViewList : " + (System.currentTimeMillis() - cur) + " , " + iconViews.size() + " , " + folderViewMap.size() + " , " + this.mAppsPagedView.getMaxItemsPerScreen() + " , " + this.mAppsPagedView.getCellCountX());
        normalizer.normalize(iconViews, this.mAppsPagedView.getMaxItemsPerScreen(), this.mAppsPagedView.getCellCountX());
        Log.d(TAG, "reposition - normalize: " + (System.currentTimeMillis() - cur));
        Iterator it = new HashSet(folderViewMap.keySet()).iterator();
        while (it.hasNext()) {
            FolderInfo folderInfo = (FolderInfo) it.next();
            if (repositionBy == 0) {
                folderInfo.setAlphabeticalOrder(this.mViewType != ViewType.CUSTOM_GRID, false, this.mLauncher);
            }
            if (repositionBy != 3) {
                view = (View) folderViewMap.get(folderInfo);
                if (view instanceof FolderIconView) {
                    ((FolderIconView) view).applyStyle();
                    checkIfConfigIsDifferentFromActivity();
                }
            }
        }
        this.mAppsPagedView.rearrangeAllViews(animate);
        this.mAppsPagedView.removeEmptyScreen();
        Log.d(TAG, "repositionByNormalizer end " + (System.currentTimeMillis() - cur));
    }

    public void onSetPageScrollListener(PageScrollListener listener) {
        this.mAppsPagedView.setPageScrollListener(listener);
    }

    private void closeOrganizeAppsConfirmDialog() {
        FragmentManager manager = this.mLauncher.getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        OrganizeAppsConfirmDialog.dismiss(ft, manager);
        ft.addToBackStack(null);
    }

    public void updateCountBadge(View view, boolean animate) {
        if (view instanceof IconView) {
            TextView countBadge = ((IconView) view).getCountBadgeView();
            if (countBadge != null && countBadge.getVisibility() == 0) {
                ((IconView) view).updateCountBadge(false, animate);
            }
        }
    }

    public void normalizeWithExtraItems(@Nullable ArrayList<ItemInfo> withItems, @Nullable ArrayList<ItemInfo> withOutItems) {
        Iterator it;
        ArrayList<ItemInfo> itemInfos = new ArrayList();
        int pageCount = this.mAppsPagedView.getPageCount();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            CellLayout cl = this.mAppsPagedView.getCellLayout(pageIndex);
            int childCount = cl.getPageChildCount();
            for (int itemIndex = 0; itemIndex < childCount; itemIndex++) {
                View view = cl.getChildOnPageAt(itemIndex);
                if ((view instanceof Removable) && !((Removable) view).isMarkToRemove() && (view.getTag() instanceof ItemInfo)) {
                    itemInfos.add((ItemInfo) view.getTag());
                }
            }
        }
        if (withItems != null) {
            it = withItems.iterator();
            while (it.hasNext()) {
                itemInfos.add((ItemInfo) it.next());
            }
        }
        if (withOutItems != null) {
            it = withOutItems.iterator();
            while (it.hasNext()) {
                itemInfos.remove((ItemInfo) it.next());
            }
        }
        this.mAdapterProvider.getNormalizer().normalize(itemInfos, this.mAppsPagedView.getMaxItemsPerScreen(), this.mAppsPagedView.getCellCountX());
    }

    private void startSettingActivity() {
        this.mFromSetting = false;
        this.mLauncher.startHomeSettingActivity();
        restoreScreenGrid(150, true);
    }

    private void restoreScreenGrid(int delay, final boolean finishStage) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                int[] gridXY = new int[2];
                ScreenGridUtilities.loadCurrentAppsGridSize(AppsController.this.mLauncher, gridXY);
                AppsController.this.repositionByGrid(gridXY);
                AppsController.this.updateDirtyItems();
                if (finishStage) {
                    AppsController.this.changeState(0, false);
                    StageEntry data = new StageEntry();
                    data.enableAnimation = false;
                    AppsController.this.getStageManager().finishStage(AppsController.this, data);
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(6);
                }
            }
        }, (long) delay);
    }

    public void setDataWithOutStageChange(StageEntry data) {
        if (data == null) {
            return;
        }
        if (this.mState == 0) {
            changeState(data.getInternalStateTo(), true);
            return;
        }
        changeState(0, false);
        changeState(data.getInternalStateTo(), true);
    }

    public void onSwipeBlockListener(float x, float y) {
        this.mScrollDeterminator.setBlockArea(this.mAppsPagedView, x, y);
        if (this.mAppsPagedView != null && this.mAppsPagedView.getPageCount() <= 1) {
            this.mScrollDeterminator.setScrollableView(false);
        }
        this.mScrollDeterminator.setTalkBackEnabled(this.mLauncher);
    }

    public boolean isVerticalScroll() {
        return this.mScrollDeterminator == null || this.mScrollDeterminator.isVerticalScroll();
    }

    public boolean isMovingOnBlock() {
        return this.mScrollDeterminator != null && this.mScrollDeterminator.isMovingOnBlock();
    }

    public boolean isScrollLocked() {
        return this.mScrollDeterminator != null && this.mScrollDeterminator.isLocked();
    }

    public float getTrayBgBlurAmount() {
        return getBackgroundBlurAmountForState(0);
    }

    public boolean isOverBlurSlop(int slop) {
        return this.mScrollDeterminator == null || this.mScrollDeterminator.getCountTouchMove() > slop;
    }

    public void requestBlurChange(boolean show, Window dest, float amount, long duration) {
    }

    private boolean isDragLocked() {
        return this.mRemoveInProgress;
    }

    public boolean changeScreenGrid(boolean animated, int gridX, int gridY) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (dp.appsGrid.getCellCountX() == gridX && dp.appsGrid.getCellCountY() == gridY) {
            return false;
        }
        StageEntry data = new StageEntry();
        data.putExtras(KEY_REPOSITION_BY, Integer.valueOf(1));
        data.putExtras(KEY_TARGET_GRID_SIZE, new int[]{gridX, gridY});
        data.enableAnimation = animated;
        data.setInternalStateFrom(this.mState);
        data.setInternalStateTo(this.mState);
        getStageManager().switchInternalState(this, data);
        return true;
    }

    private void repositionByConfiguration() {
        int[] savedGrid = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(this.mLauncher, savedGrid);
        this.mLauncher.getDeviceProfile().setAppsCurrentGrid(savedGrid[0], savedGrid[1]);
        repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 3, false);
    }

    private void repositionByGrid(int[] grid) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellX = grid[0];
        int cellY = grid[1];
        dp.setAppsCurrentGrid(cellX, cellY);
        int[] savedGrid = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(this.mLauncher, savedGrid);
        Log.d(TAG, "repositionByGrid for preview, current cellX : " + cellX + ", cellY : " + cellY + ", savedGridX : " + savedGrid[0] + ", savedGridY : " + savedGrid[1]);
        if (savedGrid[0] == cellX && savedGrid[1] == cellY) {
            this.mAdapterProvider.reloadAllItemsFromDB(false);
        } else if (this.mViewType == ViewType.ALPHABETIC_GRID) {
            this.mAdapterProvider.reloadAllItemsFromDB(false);
        }
        this.mAppsPagedView.onChangeScreenGrid(cellX, cellY);
        repositionByNormalizer(this.mAdapterProvider.getNormalizer(), 1, false);
    }

    public void applyScreenGrid() {
        this.mLauncher.getStageManager().moveToOverHome(this, null);
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int cellX = dp.appsGrid.getCellCountX();
        int cellY = dp.appsGrid.getCellCountY();
        ScreenGridUtilities.storeAppsGridLayoutPreference(this.mLauncher, cellX, cellY);
        Log.d(TAG, "applyGrid : " + cellX + " , " + cellY);
        if (this.mViewType != ViewType.ALPHABETIC_GRID) {
            updateDirtyItems();
        }
        changeState(0, true);
    }

    public boolean isAlphabeticalMode() {
        return this.mViewType == ViewType.ALPHABETIC_GRID;
    }

    public boolean isSwitchingState() {
        return getStageManager().isRunningAnimation() || isSwitchingInternalState();
    }

    public boolean isSwitchingInternalState() {
        return isRunningStateChangeAnimation();
    }

    public boolean isSwitchingGridToNormal() {
        return isSwitchingInternalState() && this.mPrevState == 5 && this.mState == 0;
    }

    public boolean isSelectState() {
        return this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode();
    }

    public boolean deferToBind() {
        if ((this.mLauncher.isAppsStage() && this.mState == 1) || getDragController().needDefferToBind(this.mDragMgr)) {
            return true;
        }
        return false;
    }

    public void requestRunDeferredRunnable() {
        this.mAdapterProvider.requestRunDeferredRunnables();
    }

    public View createItemView(ItemInfo info, ViewGroup parent, View recycleView) {
        if (recycleView != null) {
            checkIfConfigIsDifferentFromActivity();
        }
        switch (info.itemType) {
            case 0:
                return AppsUtils.createAppIcon(this.mLauncher, parent, (IconView) recycleView, (IconInfo) info, this.mLauncher, this, this.mAppsFocusListener);
            case 2:
                View view = FolderIconView.fromXml(this.mLauncher, parent, (FolderInfo) info, this, this.mLauncher, this, 2);
                view.setOnFocusChangeListener(this.mAppsFocusListener);
                view.setOnKeyListener(this.mAppsFocusListener);
                return view;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }
    }

    AppsReorderController getReorderController() {
        return this.mReorderController;
    }

    private void setTextViewDescription(View view) {
        if (view instanceof TextView) {
            String buttonDescriptionFormat = ((TextView) view).getText().toString();
            view.setContentDescription(buttonDescriptionFormat + " " + this.mLauncher.getResources().getString(R.string.accessibility_button));
            return;
        }
        String str = TAG;
        StringBuilder append = new StringBuilder().append("This view can't cast to TextView : ");
        if (view == null) {
            view = "null";
        }
        Log.e(str, append.append(view).toString());
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        List<CharSequence> text = event.getText();
        text.clear();
        Resources res = this.mLauncher.getResources();
        if (isTidyState()) {
            text.add(res.getString(R.string.options_menu_tide_up_pages));
        } else if (isGridState()) {
            text.add(res.getString(R.string.setting_apps_screen_grid));
        } else {
            text.add(res.getString(R.string.apps_button_label));
        }
        return true;
    }

    public ScreenDivision getScreenDivision() {
        return this.mAppsContainer.getScreenDivision();
    }

    public void onNotificationPreviewBinding(Map<PackageUserKey, BadgeInfo> updatedatas) {
        Log.d(TAG, " onNotificationPreviewBinding");
        if (this.mLauncher.getDragMgr().getQuickOptionView() != null) {
            this.mLauncher.getDragMgr().getQuickOptionView().trimNotifications(updatedatas);
        }
    }

    public boolean searchBarHasFocus() {
        return getAppsSearchBarView() != null && getAppsSearchBarView().hasFocus();
    }

    private boolean isLandscapeOnPhoneModel() {
        return Utilities.canScreenRotate() && !LauncherFeature.isTablet() && Utilities.getOrientation() == 2;
    }

    private void updateTidyUpContainerLayout() {
        if (this.mAppsTidyUpContainer != null) {
            Resources res = this.mLauncher.getResources();
            LayoutParams lp = (LayoutParams) this.mAppsTidyUpContainer.getLayoutParams();
            int sideMargin = res.getDimensionPixelSize(R.dimen.tidyup_container_margin);
            lp.setMarginEnd(sideMargin);
            lp.setMarginStart(sideMargin);
            lp.topMargin = res.getDimensionPixelSize(R.dimen.tidyup_container_margin_top);
            lp.height = res.getDimensionPixelSize(R.dimen.tidyup_container_height);
            this.mAppsTidyUpContainer.setLayoutParams(lp);
            int childCount = this.mAppsTidyUpContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((TextView) this.mAppsTidyUpContainer.getChildAt(i)).setTextSize(0, res.getDimension(R.dimen.screen_grid_top_button_text_size));
            }
        }
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        super.setPaddingForNavigationBarIfNeeded(this.mAppsContainer, this.mAppsPagedView);
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.updateMultiSelectPanelLayout();
        }
    }
}
