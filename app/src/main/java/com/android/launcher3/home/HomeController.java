package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.OnBadgeBindingCompletedListener;
import com.android.launcher3.LauncherModel.OnNotificationPreviewBindingListener;
import com.android.launcher3.LauncherModel.OnRefreshLiveIconListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.ItemOperator;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.PagedView.PageScrollListener;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.PinItemRequestCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.drawable.PreloadIconDrawable;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.DataLoader.ItemInfoFilter;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.multiselect.MultiSelectManager.MultiSelectListener;
import com.android.launcher3.common.quickoption.QuickOptionView;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.stage.StageManager;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.tray.TrayManager.TrayEvent;
import com.android.launcher3.common.tray.TrayManager.TrayInteractionListener;
import com.android.launcher3.common.tray.TrayManager.TrayLevel;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.folderlock.FolderLock.FolderLockActionCallback;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.notification.NotificationHelpTipManager;
import com.android.launcher3.util.BlurRunnable;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.animation.SearchedAppBounceAnimation;
import com.android.launcher3.util.capture.CapturePreview;
import com.android.launcher3.util.capture.CapturePreview.CaptureListener;
import com.android.launcher3.util.event.ScreenDivision;
import com.android.launcher3.util.event.ScrollDeterminator;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PinItemDragListener;
import com.android.launcher3.widget.PinShortcutRequestActivityInfo;
//import com.samsung.android.feature.SemGateConfig;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeController extends Stage implements ControllerBase, OnLongClickListener, TrayInteractionListener, OnBadgeBindingCompletedListener, OnNotificationPreviewBindingListener, MultiSelectListener, FolderLockActionCallback, OnRefreshLiveIconListener {
    private static final int BOUNCE_ANIMATION_DURATION = 200;
    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;
    private static final int ENTER_RESIZE_STATE_DELAY = 200;
    private static final int EXIT_DRAG_STATE_DELAY = 100;
    private static final int EXIT_SCREEN_GRID_STATE_DELAY = 200;
    private static final int FIND_OPEN_FOLDER_DELAY = 300;
    private static final String KEY_PREF_CURRENT_SET_DIALER = "current_set_dialer_pref";
    private static final boolean MAP_NO_RECURSE = false;
    private static final boolean MAP_RECURSE = true;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
    private static final int OPEN_FOLDER_DELAY = 500;
    private static final int REQUEST_BIND_APPWIDGET = 11;
    static final int REQUEST_CREATE_APPWIDGET = 5;
    static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    static final int REQUEST_PICK_WALLPAPER = 10;
    static final int REQUEST_RECONFIGURE_APPWIDGET = 12;
    private static final String RUNTIME_HOME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    private static final String RUNTIME_HOME_STATE_PENDING_CELL_X = "launcher.add_cell_x";
    private static final String RUNTIME_HOME_STATE_PENDING_CELL_Y = "launcher.add_cell_y";
    private static final String RUNTIME_HOME_STATE_PENDING_COMPONENT = "launcher.add_component";
    private static final String RUNTIME_HOME_STATE_PENDING_CONTAINER = "launcher.add_container";
    private static final String RUNTIME_HOME_STATE_PENDING_SCREEN = "launcher.add_screen";
    private static final String RUNTIME_HOME_STATE_PENDING_SPAN_X = "launcher.add_span_x";
    private static final String RUNTIME_HOME_STATE_PENDING_SPAN_Y = "launcher.add_span_y";
    private static final String RUNTIME_HOME_STATE_PENDING_WIDGET_ID = "launcher.add_widget_id";
    private static final String RUNTIME_HOME_STATE_PENDING_WIDGET_INFO = "launcher.add_widget_info";
    private static final String TAG = "Launcher.HomeController";
    private static PendingAddArguments sPendingAddItem;
    static HashMap<String, LongSparseArray<Integer>> sSingleInstanceAppWidgetList = new HashMap();
    static HashMap<String, LongSparseArray<Integer>> sSingleInstanceAppWidgetPackageList = new HashMap();
    private LauncherAppWidgetHost mAppWidgetHost;
    AppWidgetManagerCompat mAppWidgetManager;
    private Handler mBlurRunnableHandler = new Handler();
    private SearchedAppBounceAnimation mBounceAnimation;
    private CaptureListener mCaptureListener = new CaptureListener() {
        public ViewGroup getTargetView() {
            return HomeController.this.mHomeContainer;
        }

        public boolean canCapture() {
            if (HomeController.this.mWorkspace != null && HomeController.this.getState() == 1 && HomeController.this.mWorkspace.getCurrentPage() == HomeController.this.mWorkspace.getDefaultPage() && !HomeController.this.mWorkspace.isPageMoving() && !HomeController.this.isSwitchingState() && !HomeController.this.mLauncher.isDestroyed()) {
                return true;
            }
            Log.d(HomeController.TAG, "canCapture false");
            return false;
        }
    };
    private DialerChangeObserver mDialerChangeObserver = null;
    private DragLayer mDragLayer;
    private DragManager mDragMgr;
    private DropTargetBar mDropTargetBar;
    private EdgeLight mEdgeLight;
    private boolean mEnabledCustomLayoutAnimation = true;
    private Handler mExitDragStateHandler = new Handler();
    private FavoritesUpdater mFavoritesUpdater;
    private FestivalPageController mFestivalPageController;
    private Handler mFindAppPositionHandler = new Handler();
    private FolderLock mFolderLock;
    private HomeTransitionAnimation mHomeAnimation;
    private HomeBindController mHomeBindController;
    private CapturePreview mHomeCapturePreview;
    private HomeContainer mHomeContainer;
    private boolean mHomeDefaultIconClick = false;
    private boolean mHomeKeyPressed = false;
    private HomeLoader mHomeLoader;
    private boolean mHomePageReorder = false;
    private Hotseat mHotseat;
    private int mHotseatMoveRange;
    private boolean mIsStartedTrayEventSetY = false;
    private int mMoveToAppsPanelHeight;
    private MultiSelectManager mMultiSelectManager;
    private NotificationHelpTipManager mNotificationHelpTipManager;
    private float mOverviewBlurAmount;
    private float mOverviewDrakenAlpha;
    private OverviewPanel mOverviewPanel;
    private View mPageIndicatorView;
    private float mPageSnapMovingRatioOnHome;
    private ItemInfo mPendingAddInfo = new ItemInfo();
    private int mPendingAddWidgetId = -1;
    private LauncherAppWidgetProviderInfo mPendingAddWidgetInfo;
    private boolean mRestoring;
    private ScreenGridHelper mScreenGridHelper;
    private ScreenGridPanel mScreenGridPanel;
    private ScrollDeterminator mScrollDeterminator = new ScrollDeterminator();
    private float mStartSFinderRatio;
    private State mState = new State(1);
    private SwipeAffordance mSwipeAffordance;
    private int[] mTmpAddItemCellCoordinates = new int[2];
    private TrayManager mTrayManager;
    private boolean mWaitingForResult;
    private Workspace mWorkspace;
    private ZeroPageController mZeroPageController;

    private class DialerChangeObserver extends ContentObserver {
        DialerChangeObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            HomeController.this.changeDialerApp();
        }
    }

    static class PendingAddArguments {
        int appWidgetId;
        int cellX;
        int cellY;
        long container;
        Intent intent;
        int requestCode;
        long screenId;

        PendingAddArguments() {
        }
    }

    public static class State {
        public static final int DRAG = 2;
        public static final int NONE = 0;
        public static final int NORMAL = 1;
        public static final int OVERVIEW = 4;
        public static final int RESIZE = 3;
        public static final int SCREENGRID = 5;
        public static final int SELECT = 6;
        private int mCurrentState;

        State(int state) {
            this.mCurrentState = state;
        }

        private void set(int state) {
            this.mCurrentState = state;
        }

        private int get() {
            return this.mCurrentState;
        }

        private boolean equal(int state) {
            return this.mCurrentState == state;
        }
    }

    HomeLoader getHomeLoader() {
        return this.mHomeLoader;
    }

    Launcher getLauncher() {
        return this.mLauncher;
    }

    public void setup() {
        LauncherModel model = this.mLauncher.getLauncherModel();
        LauncherAppState app = LauncherAppState.getInstance();
        this.mDragMgr = this.mLauncher.getDragMgr();
        this.mHomeLoader = model.getHomeLoader();
        this.mFavoritesUpdater = (FavoritesUpdater) this.mHomeLoader.getUpdater();
        this.mTrayManager = this.mLauncher.getTrayManager();
        if (this.mTrayManager != null) {
            this.mTrayManager.addTrayEventCallbacks(this);
        }
        this.mHomeBindController = new HomeBindController(this.mLauncher, this, model, app.getIconCache());
        this.mHomeAnimation = new HomeTransitionAnimation(this.mLauncher, this, this.mTrayManager);
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this.mLauncher);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this.mLauncher, 1024);
        this.mAppWidgetHost.startListening();
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
            this.mFolderLock.addFolderLockActionCallback(this);
        }
        LauncherAppState.getInstance().getModel().registerOnBadgeBindingCompletedListener(this);
        LauncherAppState.getInstance().getModel().registerOnNotificationPreviewListener(this);
        LauncherAppState.getInstance().getModel().registerOnLiveIconUpdateListener(this);
        if (LauncherFeature.supportCustomerDialerChange()) {
            this.mDialerChangeObserver = new DialerChangeObserver();
            this.mLauncher.getContentResolver().registerContentObserver(System.getUriFor("skt_phone20_settings"), false, this.mDialerChangeObserver);
        }
        this.mHomeCapturePreview = new CapturePreview(this.mLauncher);
        this.mHomeCapturePreview.setListener(this.mCaptureListener);
        LauncherAppState.getInstance().getLauncherProxy().setHomeProxyCallbacks(new HomeProxyCallbacksImpl(this));
        this.mScrollDeterminator.setSystemTouchSlop(this.mLauncher);
        this.mScrollDeterminator.registrateController(0);
    }

    public void initStageView() {
        this.mDragLayer = this.mLauncher.getDragLayer();
        this.mHomeAnimation.setupView();
        this.mHomeContainer = (HomeContainer) this.mLauncher.findViewById(R.id.home_view);
        this.mHomeContainer.bindController(this);
        this.mHomeContainer.setTrayManager(this.mTrayManager);
        this.mWorkspace = (Workspace) this.mLauncher.findViewById(R.id.workspace);
        this.mWorkspace.bindController(this);
        this.mWorkspace.setHapticFeedbackEnabled(false);
        this.mWorkspace.setOnLongClickListener(this);
        this.mWorkspace.setup(this.mDragMgr, this.mDragLayer);
        this.mWorkspace.initDefaultHomeIcon();
        this.mWorkspace.setScrollDeterminator(this.mScrollDeterminator);
        this.mHotseat = (Hotseat) this.mLauncher.findViewById(R.id.hotseat);
        if (this.mHotseat != null) {
            this.mHotseat.bindController(this);
            this.mHotseat.resetLayout();
            this.mHotseat.setOnLongClickListener(this);
            this.mHotseat.setup(this.mDragMgr);
        }
        this.mDropTargetBar = (DropTargetBar) this.mLauncher.findViewById(R.id.drop_target_bar);
        this.mDropTargetBar.setup(this.mDragMgr);
        this.mOverviewPanel = (OverviewPanel) this.mLauncher.findViewById(R.id.overview_panel);
        this.mOverviewPanel.bindController(this);
        this.mScreenGridPanel = (ScreenGridPanel) this.mLauncher.findViewById(R.id.screen_grid_panel);
        this.mScreenGridPanel.bindController(this);
        this.mScreenGridHelper = new ScreenGridHelper(this.mLauncher, this);
        this.mScreenGridPanel.initScreenGridTopContainer();
        this.mPageIndicatorView = this.mLauncher.findViewById(R.id.home_page_indicator);
        if (this.mPageIndicatorView != null) {
            LayoutParams oldLp = (LayoutParams) this.mPageIndicatorView.getLayoutParams();
            this.mHomeContainer.removeView(this.mPageIndicatorView);
            int indexToAddView = this.mDragLayer.getChildCount();
            for (int i = 0; i < this.mDragLayer.getChildCount(); i++) {
                if (this.mHomeContainer.equals(this.mDragLayer.getChildAt(i))) {
                    indexToAddView = i;
                    break;
                }
            }
            DragLayer.LayoutParams newLp = new DragLayer.LayoutParams(oldLp);
            newLp.gravity = 81;
            this.mDragLayer.addView(this.mPageIndicatorView, indexToAddView, newLp);
        }
        Resources res = this.mLauncher.getResources();
        this.mOverviewBlurAmount = ((float) res.getInteger(R.integer.config_homeOverviewBgBlur)) / 100.0f;
        this.mOverviewDrakenAlpha = ((float) res.getInteger(R.integer.config_homeOverviewBgDarken)) / 100.0f;
        this.mPageSnapMovingRatioOnHome = res.getFraction(R.fraction.config_pageSnapMovingRatio, 1, 1);
        this.mHotseatMoveRange = res.getDimensionPixelSize(R.dimen.hotseat_move_range_for_move_to_apps);
        this.mStartSFinderRatio = res.getFraction(R.fraction.config_start_sfinder_from_home, 1, 1);
        this.mHomeBindController.setup(this.mDragMgr, this.mWorkspace, this.mHotseat);
        this.mZeroPageController = this.mWorkspace.getZeroPageController();
        if (this.mZeroPageController != null) {
            this.mZeroPageController.setup();
            if (LauncherFeature.supportZeroPageHome() && !isModalState() && ZeroPageController.isActiveZeroPage(this.mLauncher, false) && this.mWorkspace.getDefaultPage() == -1) {
                this.mZeroPageController.startZeroPage();
            }
            this.mEdgeLight = (EdgeLight) this.mLauncher.findViewById(R.id.edge_light_container);
            if (this.mEdgeLight != null) {
                this.mEdgeLight.registerContentObserver(this);
            }
        }
        if (LauncherFeature.supportMultiSelect()) {
            this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
            if (this.mMultiSelectManager != null) {
                this.mMultiSelectManager.addMultiSelectCallbacks(this);
            }
        }
        if (LauncherFeature.supportFestivalPage()) {
            this.mFestivalPageController = new FestivalPageController(this.mLauncher, this);
        }
        this.mNotificationHelpTipManager = new NotificationHelpTipManager(this.mLauncher, this.mDragLayer);
        super.initStageView();
    }

    public void onResumeActivity() {
        boolean isHomeStage;
        if (getStageManager() == null || getStageManager().getTopStage() != this) {
            isHomeStage = false;
        } else {
            isHomeStage = true;
        }
        if (isHomeStage && !this.mLauncher.isSkipAnim()) {
            DataLoader.reinflateWidgetsIfNecessary();
        }
        if (this.mSwipeAffordance != null && !this.mLauncher.isSkipAnim() && this.mState.equal(1) && isHomeStage) {
            this.mSwipeAffordance.startAnim();
        }
        initBounceAnimation();
        this.mLauncher.getPageTransitionManager().setup(this.mWorkspace);
        this.mWorkspace.onResume();
        if (this.mState.equal(1)) {
            if (LauncherFeature.supportHotword() && this.mLauncher.isGoogleSearchWidget(this.mWorkspace.getCurrentPage())) {
                this.mLauncher.setHotWordDetection(true);
            }
            startEdgeLight();
            if (!(isWorkspaceLocked() || getStageManager() == null || getStageManager().isRunningAnimation())) {
                updateNotificationHelp(false);
            }
        }
        // TODO: Samsung specific code
//        if (SemGateConfig.isGateEnabled() && isHomeStage) {
//            Log.i("GATE", "<GATE-M>SCREEN_LOADED_HOME</GATE-M>");
//        }
        if (isHomeStage && this.mState.equal(4)) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(5);
        }
        this.mHomeLoader.clearPreservedPosition();
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && this.mLauncher.getSearchedApp() != null) {
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
                    findSearchedApp(new ComponentName(pkg, cmp), this.mLauncher.getSearchedAppUser());
                    this.mLauncher.setSearchedApp(null);
                    this.mLauncher.setSearchedAppUser(null);
                }
            }
        }
        Log.d(TAG, "onResume HomeContainer current alpha = " + this.mHomeContainer.getAlpha());
    }

    public void onPauseActivity() {
        initBounceAnimation();
        if (isSelectState()) {
            if (this.mMultiSelectManager != null && this.mMultiSelectManager.isShowingHelpDialog()) {
                this.mMultiSelectManager.hideHelpDialog(false);
            }
            enterNormalState(false);
        }
        if (this.mState.equal(3)) {
            exitResizeState(false);
        }
        if (LauncherFeature.supportHotword() && this.mState.equal(1) && this.mLauncher.isGoogleSearchWidget(this.mWorkspace.getCurrentPage())) {
            this.mLauncher.setHotWordDetection(false);
        }
        if (getStageManager() != null && getStageManager().getTopStage() == this && this.mState.equal(2) && this.mPendingAddInfo.container == -1) {
            exitDragStateDelayed();
        }
        if (isStartedSwipeAffordanceAnim() && this.mLauncher.isHomeStage() && this.mLauncher.isSkipAnim()) {
            this.mSwipeAffordance.startCancelAnim(true);
        }
        FragmentManager fragmentManager = this.mLauncher.getFragmentManager();
        if (AutoAlignConfirmDialog.isActive(fragmentManager)) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            AutoAlignConfirmDialog.dismiss(fragmentTransaction, fragmentManager);
            fragmentTransaction.commit();
        }
        stopEdgeLight();
        setDisableNotificationHelpTip();
        this.mWorkspace.onPause();
    }

    public void onRestoreInstanceState(Bundle state) {
        this.mHomeBindController.restoreInstanceState();
    }

    public void onSaveInstanceState(Bundle outState) {
        if (this.mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_HOME_STATE_CURRENT_SCREEN, this.mWorkspace.getCurrentPageOffsetFromCustomContent());
        }
        if (this.mPendingAddInfo.container != -1 && this.mPendingAddInfo.screenId > -1 && this.mWaitingForResult) {
            outState.putLong(RUNTIME_HOME_STATE_PENDING_CONTAINER, this.mPendingAddInfo.container);
            outState.putLong(RUNTIME_HOME_STATE_PENDING_SCREEN, this.mPendingAddInfo.screenId);
            outState.putInt(RUNTIME_HOME_STATE_PENDING_CELL_X, this.mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_HOME_STATE_PENDING_CELL_Y, this.mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_HOME_STATE_PENDING_SPAN_X, this.mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_HOME_STATE_PENDING_SPAN_Y, this.mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_HOME_STATE_PENDING_COMPONENT, this.mPendingAddInfo.componentName);
            outState.putParcelable(RUNTIME_HOME_STATE_PENDING_WIDGET_INFO, this.mPendingAddWidgetInfo);
            outState.putInt(RUNTIME_HOME_STATE_PENDING_WIDGET_ID, this.mPendingAddWidgetId);
        }
    }

    public void restoreState(Bundle savedState, boolean isOnTop) {
        if (!isOnTop) {
            boolean accessibilityEnabled = Talk.INSTANCE.isAccessibilityEnabled();
            this.mHomeContainer.setVisibility(accessibilityEnabled ? View.GONE : View.INVISIBLE);
            if (!(this.mPageIndicatorView == null || this.mHomeContainer.equals(this.mPageIndicatorView.getParent()))) {
                this.mPageIndicatorView.setVisibility(accessibilityEnabled ? View.GONE : View.INVISIBLE);
            }
        }
        int currentScreen = savedState.getInt(RUNTIME_HOME_STATE_CURRENT_SCREEN, -1001);
        if (currentScreen != -1001) {
            this.mWorkspace.setRestorePage(currentScreen);
        }
        long pendingAddContainer = savedState.getLong(RUNTIME_HOME_STATE_PENDING_CONTAINER, -1);
        long pendingAddScreen = savedState.getLong(RUNTIME_HOME_STATE_PENDING_SCREEN, -1);
        if (pendingAddContainer != -1 && pendingAddScreen > -1) {
            LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo;
            this.mPendingAddInfo.container = pendingAddContainer;
            this.mPendingAddInfo.screenId = pendingAddScreen;
            this.mPendingAddInfo.cellX = savedState.getInt(RUNTIME_HOME_STATE_PENDING_CELL_X);
            this.mPendingAddInfo.cellY = savedState.getInt(RUNTIME_HOME_STATE_PENDING_CELL_Y);
            this.mPendingAddInfo.spanX = savedState.getInt(RUNTIME_HOME_STATE_PENDING_SPAN_X);
            this.mPendingAddInfo.spanY = savedState.getInt(RUNTIME_HOME_STATE_PENDING_SPAN_Y);
            this.mPendingAddInfo.componentName = (ComponentName) savedState.getParcelable(RUNTIME_HOME_STATE_PENDING_COMPONENT);
            AppWidgetProviderInfo info = (AppWidgetProviderInfo) savedState.getParcelable(RUNTIME_HOME_STATE_PENDING_WIDGET_INFO);
            if (info == null) {
                launcherAppWidgetProviderInfo = null;
            } else {
                launcherAppWidgetProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, info);
            }
            this.mPendingAddWidgetInfo = launcherAppWidgetProviderInfo;
            this.mPendingAddWidgetId = savedState.getInt(RUNTIME_HOME_STATE_PENDING_WIDGET_ID);
            setWaitingForResult(true);
            Log.d(TAG, "restoreState exist pendingAddInfo " + this.mPendingAddInfo);
            this.mRestoring = true;
        }
    }

    public void onDestroyActivity() {
        if (LauncherFeature.supportFestivalPage() && this.mFestivalPageController != null) {
            this.mFestivalPageController.onDestroy();
        }
        this.mAppWidgetHost = null;
        if (this.mTrayManager != null) {
            this.mTrayManager.removeTrayEventCallbacks(this);
        }
        if (this.mHomeBindController != null) {
            this.mHomeBindController.clearWidgetsToAdvance();
            this.mHomeBindController = null;
        }
        if (this.mWorkspace != null) {
            ((ViewGroup) this.mWorkspace.getParent()).removeAllViews();
            this.mWorkspace.removeAllWorkspaceScreens();
            this.mWorkspace = null;
        }
        if (this.mZeroPageController != null) {
            // TODO: This call is no longer needed
            //this.mZeroPageController.onDestroy();
        }
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.removeMultiSelectCallbacks(this);
        }
        if (this.mFolderLock != null) {
            this.mFolderLock.removeFolderLockActionCallback(this);
        }
        if (this.mExitDragStateHandler != null) {
            this.mExitDragStateHandler.removeCallbacksAndMessages(null);
        }
        if (this.mFindAppPositionHandler != null) {
            this.mFindAppPositionHandler.removeCallbacksAndMessages(null);
        }
        resetBlurRunnable();
        if (LauncherAppState.getInstanceNoCreate() != null) {
            LauncherAppState.getInstance().getModel().unregisterOnBadgeBindingCompletedListener(this);
            LauncherAppState.getInstance().getModel().unregisterOnNotificationPreviewListener(this);
            LauncherAppState.getInstance().getModel().unregisterOnLiveIconUpdateListener(this);
        }
        if (LauncherFeature.supportCustomerDialerChange()) {
            this.mLauncher.getContentResolver().unregisterContentObserver(this.mDialerChangeObserver);
        }
        if (this.mEdgeLight != null) {
            this.mEdgeLight.unregisterContentObserver();
        }
        this.mHomeCapturePreview.stopCapture();
    }

    protected Animator onStageEnter(StageEntry data) {
        this.mDragMgr.setDragScroller(this.mWorkspace.getDragController());
        this.mDragMgr.setMoveTarget(this.mWorkspace);
        Animator enterAnim = null;
        if (data != null) {
            int fromViewMode = data.fromStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            boolean animated = data.enableAnimation;
            if ((((Integer) data.getExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, 0)).intValue() > 0) && this.mTrayManager != null) {
                this.mTrayManager.setSuppressChangeStageOnce();
            }
            this.mWorkspace.setVisibility(View.VISIBLE);
            int fromState;
            int toState;
            if (fromViewMode == 2) {
                fromState = getAdjustedInternalState(data.getInternalStateFrom());
                toState = getAdjustedInternalState(data.getInternalStateTo());
                enterAnim = this.mHomeAnimation.getEnterFromAppsAnimation(animated, layerViews);
                if (toState == 2) {
                    enterDragState(false);
                    if (this.mDropTargetBar != null) {
                        this.mDropTargetBar.setDropTargetBarVisible(true);
                    }
                }
                if (fromState == 4 && toState == 1) {
                    enterNormalState(false);
                }
                this.mState.set(toState);
                this.mWorkspace.updateAccessibilityFlags(true);
                if (WhiteBgManager.isWhiteNavigationBar()) {
                    this.mLauncher.changeNavigationBarColor(true);
                }
                if (WhiteBgManager.isWhiteStatusBar()) {
                    this.mLauncher.changeStatusBarColor(true);
                }
                if (toState == 1) {
                    Talk.INSTANCE.say(this.mLauncher.getResources().getString(R.string.home_screen) + ", " + this.mWorkspace.getCurrentPageDescription());
                } else if (toState == 2) {
                    Talk.INSTANCE.say(this.mLauncher.getResources().getString(R.string.home_screen) + ", " + String.format(this.mLauncher.getResources().getString(R.string.default_scroll_format), this.mWorkspace.getCurrentPage() + 1, this.mWorkspace.getPageCount()));
                }
            } else if (fromViewMode == 5 || fromViewMode == 6) {
                View anchorView = (View) data.getExtras(FolderController.KEY_FOLDER_ICON_VIEW);
                Animator animatorSet = new AnimatorSet();
                // TODO: Samsung specific code
//                animatorSet.play(this.mHomeAnimation.getEnterFromFolderAnimation(animated, layerViews, anchorView));
//                if (this.mHomeContainer.getTranslationY() != 0.0f) {
//                    animatorSet.play(this.mHomeAnimation.getEnterFromAppsAnimation(animated, layerViews));
//                }
                enterAnim = animatorSet;
                toState = getAdjustedInternalState(data.getInternalStateTo());
                fromState = getAdjustedInternalState(data.getInternalStateFrom());
                if (toState == 2) {
                    enterDragState(animated);
                    if (this.mDropTargetBar != null && fromViewMode == 5 && data.stageCountOnFinishAllStage > 2) {
                        this.mDropTargetBar.setDropTargetBarVisible(true);
                    }
                }
                if (WhiteBgManager.isWhiteStatusBar()) {
                    this.mLauncher.changeStatusBarColor(true);
                }
                if (WhiteBgManager.isWhiteNavigationBar()) {
                    this.mLauncher.changeNavigationBarColor(true);
                }
                if (fromViewMode == 6) {
                    ArrayList<ItemInfo> itemsToHide = (ArrayList) data.getExtras(AppsPickerController.KEY_ITEMS_TO_HIDE);
                    ArrayList<ItemInfo> itemsToShow = (ArrayList) data.getExtras(AppsPickerController.KEY_ITEMS_TO_SHOW);
                    if (!(itemsToHide == null || itemsToShow == null)) {
                        this.mLauncher.updateItemInfo(itemsToHide, itemsToShow);
                    }
                    if (toState == 1 && fromState == 4) {
                        this.mState.set(toState);
                        switchInternalStateChange(fromState, toState);
                        // TODO: Samsung specific code
                        // animatorSet.play(this.mHomeAnimation.getEnterFromAppsPickerAnimation(animated, layerViews));
                        enterAnim = animatorSet;
                    } else if (toState == 1) {
                        enterAnim = animatorSet;
                    }
                }
                this.mWorkspace.updateAccessibilityFlags(true);
            } else if (fromViewMode == 3 || fromViewMode == 4) {
                fromState = getAdjustedInternalState(data.getInternalStateFrom());
                toState = getAdjustedInternalState(data.getInternalStateTo());
                this.mState.set(toState);
                switchInternalStateChange(fromState, toState);
                if (toState == 2) {
                    enterAnim = this.mHomeAnimation.getDragAnimation(animated, null, true, true, false);
                    if (this.mDropTargetBar != null) {
                        this.mDropTargetBar.setDropTargetBarVisible(true);
                    }
                } else {
                    enterAnim = this.mHomeAnimation.getEnterFromWidgetsAnimation(animated, layerViews, toState == 1);
                }
                this.mWorkspace.updateAccessibilityFlags(true);
            }
            if (enterAnim != null && data.broughtToHome) {
                enterAnim.setStartDelay(30);
            }
        }
        if (this.mState.equal(4)) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(5);
        } else {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(1);
        }
        notifyCaptureIfNecessary();
        if (LauncherFeature.supportHotword() && this.mState.equal(1) && this.mLauncher.isGoogleSearchWidget(this.mWorkspace.getCurrentPage())) {
            this.mLauncher.setHotWordDetection(true);
        }
        startEdgeLight();
        callRefreshLiveIcon();
        return enterAnim;
    }

    private void callRefreshLiveIcon() {
        if (this.mWorkspace != null) {
            this.mWorkspace.callRefreshLiveIcon();
        }
    }

    private int getAdjustedInternalState(int value) {
        return value == 0 ? this.mState.get() : value;
    }

    protected Animator onStageExit(StageEntry data) {
        Utilities.closeDialog(this.mLauncher);
        Animator exitAnim = null;
        if (data != null) {
            int toViewMode = data.toStage;
            HashMap<View, Integer> layerViews = data.getLayerViews();
            boolean animated = data.enableAnimation;
            if (toViewMode == 2) {
                exitAnim = this.mHomeAnimation.getExitToAppsAnimation(animated, layerViews);
                this.mWorkspace.updateAccessibilityFlags(false);
                if (WhiteBgManager.isWhiteNavigationBar()) {
                    this.mLauncher.changeNavigationBarColor(false);
                }
                if (WhiteBgManager.isWhiteStatusBar()) {
                    this.mLauncher.changeStatusBarColor(false);
                }
            } else if (toViewMode == 5 || toViewMode == 6) {
                exitAnim = this.mHomeAnimation.getExitToFolderAnimation(animated, layerViews, (View) data.getExtras(FolderController.KEY_FOLDER_ICON_VIEW));
                this.mWorkspace.updateAccessibilityFlags(false);
            } else if (toViewMode == 3) {
                exitAnim = this.mHomeAnimation.getExitToWidgetsAnimation(animated, layerViews);
                this.mWorkspace.updateAccessibilityFlags(false);
            }
            if (isStartedSwipeAffordanceAnim()) {
                this.mSwipeAffordance.startCancelAnim(false);
            }
        }
        if (LauncherFeature.supportHotword()) {
            this.mLauncher.setHotWordDetection(false);
        }
        stopEdgeLight();
        setDisableNotificationHelpTip();
        return exitAnim;
    }

    protected Animator onStageExitByTray() {
        if (WhiteBgManager.isWhiteNavigationBar()) {
            this.mLauncher.changeNavigationBarColor(false);
        }
        if (WhiteBgManager.isWhiteStatusBar()) {
            this.mLauncher.changeStatusBarColor(false);
        }
        Animator exitAni = this.mHomeAnimation.getExitToAppsAnimation(true, null);
        exitAni.addListener(new AnimatorListenerAdapter() {
            private boolean canceled = false;

            public void onAnimationCancel(Animator animation) {
                boolean z = HomeController.this.mTrayManager != null && HomeController.this.mTrayManager.isTouching();
                this.canceled = z;
            }

            public void onAnimationEnd(Animator animation) {
                if (HomeController.this.mSwipeAffordance != null) {
                    HomeController.this.mSwipeAffordance.appsVisitCountUp();
                }
                if (this.canceled) {
                    Log.d(HomeController.TAG, "Home onStageExitByTray canceled");
                } else if (HomeController.this.mWorkspace == null) {
                    Log.d(HomeController.TAG, "Home onDestroy !");
                } else {
                    HomeController.this.mWorkspace.updateAccessibilityFlags(false);
                    if (HomeController.this.isStartedSwipeAffordanceAnim()) {
                        HomeController.this.mSwipeAffordance.startCancelAnim(false);
                    }
                    if (LauncherFeature.supportHotword()) {
                        HomeController.this.mLauncher.setHotWordDetection(false);
                    }
                    HomeController.this.stopEdgeLight();
                    HomeController.this.setDisableNotificationHelpTip();
                }
            }
        });
        return exitAni;
    }

    protected Animator onStageEnterByTray() {
        if (WhiteBgManager.isWhiteNavigationBar()) {
            this.mLauncher.changeNavigationBarColor(true);
        }
        if (WhiteBgManager.isWhiteStatusBar()) {
            this.mLauncher.changeStatusBarColor(true);
        }
        Animator enterAni = this.mHomeAnimation.getEnterFromAppsAnimation(true, null);
        enterAni.addListener(new AnimatorListenerAdapter() {
            private boolean canceled = false;

            public void onAnimationCancel(Animator animation) {
                boolean z = HomeController.this.mTrayManager != null && HomeController.this.mTrayManager.isTouching();
                this.canceled = z;
            }

            public void onAnimationEnd(Animator animation) {
                if (this.canceled) {
                    Log.d(HomeController.TAG, "Home onStageEnterByTray cancel");
                } else if (HomeController.this.mWorkspace == null) {
                    Log.d(HomeController.TAG, "Home onDestroyActivity !");
                } else {
                    HomeController.this.mDragMgr.setDragScroller(HomeController.this.mWorkspace.getDragController());
                    HomeController.this.mDragMgr.setMoveTarget(HomeController.this.mWorkspace);
                    HomeController.this.mWorkspace.updateAccessibilityFlags(true);
                    Talk.INSTANCE.say(HomeController.this.mLauncher.getResources().getString(R.string.home_screen) + ", " + HomeController.this.mWorkspace.getCurrentPageDescription());
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(1);
                    HomeController.this.notifyCaptureIfNecessary();
                    if (LauncherFeature.supportHotword() && HomeController.this.mState.equal(1) && HomeController.this.mLauncher.isGoogleSearchWidget(HomeController.this.mWorkspace.getCurrentPage())) {
                        HomeController.this.mLauncher.setHotWordDetection(true);
                    }
                    HomeController.this.startEdgeLight();
                    HomeController.this.callRefreshLiveIcon();
                    HomeController.this.updateNotificationHelp(false);
                }
            }
        });
        return enterAni;
    }

    protected Animator switchInternalState(StageEntry data) {
        int fromState = getAdjustedInternalState(data.getInternalStateFrom());
        int toState = getAdjustedInternalState(data.getInternalStateTo());
        this.mState.set(toState);
        HashMap<View, Integer> layerViews = data.getLayerViews();
        boolean animated = data.enableAnimation;
        this.mWorkspace.setVisibility(View.VISIBLE);
        switchInternalStateChange(fromState, toState);
        Animator stateChangeAnim = null;
        if (fromState == 1) {
            if (toState == 4) {
                stateChangeAnim = this.mHomeAnimation.getOverviewAnimation(animated, layerViews, false, true);
            } else if (toState == 2) {
                stateChangeAnim = this.mHomeAnimation.getDragAnimation(animated, layerViews, true, false, false);
            } else if (toState == 6) {
                this.mExitDragStateHandler.removeCallbacksAndMessages(null);
                updateCheckBox(true);
                stateChangeAnim = this.mHomeAnimation.getSelectAnimation(animated, layerViews, true);
            }
        } else if (fromState == 4) {
            if (toState == 1) {
                stateChangeAnim = this.mHomeAnimation.getOverviewAnimation(animated, layerViews, false, false);
            } else if (toState == 5) {
                stateChangeAnim = this.mHomeAnimation.getScreenGridAnimation(animated, layerViews, false, true, false);
            }
        } else if (fromState == 2 || fromState == 3) {
            if (fromState == 3) {
                this.mDragLayer.clearAllResizeFrames();
            }
            if (toState == 1) {
                if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
                    this.mLauncher.onChangeSelectMode(false, animated);
                    updateCheckBox(false);
                }
                stateChangeAnim = this.mHomeAnimation.getDragAnimation(animated, layerViews, false, false, false);
            } else if (toState == 6) {
                this.mExitDragStateHandler.removeCallbacksAndMessages(null);
                updateCheckBox(true);
                stateChangeAnim = this.mHomeAnimation.getSelectAnimation(animated, layerViews, true);
            }
        } else if (fromState == 5) {
            stateChangeAnim = this.mHomeAnimation.getScreenGridAnimation(animated, layerViews, false, false, toState == 1);
        } else if (fromState == 6) {
            if (toState == 1) {
                stateChangeAnim = this.mHomeAnimation.getSelectAnimation(animated, layerViews, false);
                this.mLauncher.onChangeSelectMode(false, animated);
                updateCheckBox(false);
            } else if (toState == 2) {
                updateCheckBox(false);
                stateChangeAnim = this.mHomeAnimation.getDragAnimation(animated, layerViews, true, false, true);
            }
        }
        if (toState == 1) {
            notifyCaptureIfNecessary();
            if (LauncherFeature.supportHotword()) {
                this.mLauncher.setHotWordDetection(this.mLauncher.isGoogleSearchWidget(this.mWorkspace.getCurrentPage()));
            }
            GlobalSettingUtils.resetSettingsValue();
        } else {
            if (LauncherFeature.supportHotword() && this.mLauncher.getHotWordInstance() != null) {
                this.mLauncher.getHotWordInstance().setEnableHotWord(false);
            }
            stopEdgeLight();
            setDisableNotificationHelpTip();
        }
        this.mWorkspace.updateAccessibilityFlags(true);
        return stateChangeAnim;
    }

    private void changePaddingforScreenGrid() {
        if (this.mWorkspace != null) {
            Rect padding;
            LayoutParams lp = (LayoutParams) this.mWorkspace.getLayoutParams();
            lp.gravity = 17;
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (isScreenGridState()) {
                padding = dp.getWorkspacePadding(dp.homeMaxGrid);
            } else {
                padding = dp.getWorkspacePadding(dp.homeGrid);
            }
            this.mWorkspace.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            this.mWorkspace.setLayoutParams(lp);
        }
    }

    protected int getInternalState() {
        return this.mState.mCurrentState;
    }

    protected boolean supportStatusBarForState(int internalState) {
        if (internalState == 0) {
            internalState = getInternalState();
        }
        if (internalState == 2 || internalState == 4 || internalState == 6) {
            return false;
        }
        return true;
    }

    protected boolean supportNavigationBarForState(int internalState) {
        return true;
    }

    protected float getBackgroundBlurAmountForState(int internalState) {
        if (internalState == 0) {
            internalState = getInternalState();
        }
        if (internalState == 4 || internalState == 5) {
            return this.mOverviewBlurAmount;
        }
        return 0.0f;
    }

    protected float getBackgroundDimAlphaForState(int internalState) {
        if (internalState == 0) {
            internalState = getInternalState();
        }
        if (internalState == 4 || internalState == 5) {
            return this.mOverviewDrakenAlpha;
        }
        return 0.0f;
    }

    protected float getBackgroundImageAlphaForState(int internalState) {
        return 0.0f;
    }

    public void enterNormalState(boolean animated) {
        enterNormalState(animated, null, false);
    }

    public void enterNormalState(boolean animated, boolean forced) {
        enterNormalState(animated, null, forced);
    }

    private void enterNormalState(boolean animated, Runnable onCompleteRunnable, boolean forced) {
        if (!this.mLauncher.isHomeStage() || !this.mState.equal(1)) {
            StageEntry data = new StageEntry();
            data.enableAnimation = animated;
            data.setInternalStateFrom(this.mState.get());
            data.setInternalStateTo(1);
            data.addOnCompleteRunnableCallBack(onCompleteRunnable);
            if (forced || this.mLauncher.isHomeStage()) {
                getStageManager().switchInternalState(this, data);
            } else {
                Resources res = this.mLauncher.getResources();
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_2xxx), res.getString(R.string.event_Apps_Home), "2");
                getStageManager().finishAllStage(data);
            }
            this.mHomeBindController.setUserPresent(true);
            this.mHomeBindController.updateAutoAdvanceState();
        }
    }

    void enterOverviewState(boolean animated) {
        if (!this.mLauncher.isHomeStage() || ((isModalState() && !isScreenGridState()) || isWorkspaceLocked() || isOverviewState())) {
            Log.d(TAG, "Can not enterOverviewState : mLauncher.isHomeStage() = " + this.mLauncher.isHomeStage() + " isModalState() = " + isModalState() + " sScreenGridState() = " + isScreenGridState() + " workspace loading = " + this.mHomeBindController.isWorkspaceLoading() + " mWaitingForResult = " + this.mWaitingForResult + " isOverviewState() = " + isOverviewState());
            return;
        }
        if (this.mZeroPageController != null && this.mZeroPageController.hasMessages()) {
            this.mZeroPageController.restoreOffset();
        }
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_ENTER, null, -1, false);
        StageEntry data = new StageEntry();
        data.enableAnimation = animated;
        data.setInternalStateFrom(this.mState.get());
        data.setInternalStateTo(4);
        getStageManager().switchInternalState(this, data);
    }

    public void onStopActivity() {
        if (this.mZeroPageController != null && this.mZeroPageController.hasMessages()) {
            this.mZeroPageController.restoreOffset();
        }
    }

    private void enterScreenGridState(boolean animated) {
        if (isWorkspaceLocked()) {
            Log.d(TAG, "Can not enterScreenGridState : isWorkspaceLocked() = " + isWorkspaceLocked());
            return;
        }
        StageEntry data;
        if (!this.mLauncher.isHomeStage()) {
            data = new StageEntry();
            data.enableAnimation = false;
            getStageManager().startStage(1, data);
        }
        enterOverviewState(false);
        data = new StageEntry();
        data.enableAnimation = animated;
        data.setInternalStateFrom(this.mState.get());
        data.setInternalStateTo(5);
        getStageManager().switchInternalState(this, data);
    }

    void enterSelectState(boolean animated) {
        if (this.mLauncher.isHomeStage() && !this.mState.equal(6)) {
            StageEntry data = new StageEntry();
            data.enableAnimation = animated;
            data.setInternalStateFrom(this.mState.get());
            data.setInternalStateTo(6);
            getStageManager().switchInternalState(this, data);
        }
    }

    public void enterDragState(boolean animated) {
        if (!this.mState.equal(2)) {
            StageEntry data = new StageEntry();
            data.enableAnimation = animated;
            data.setInternalStateFrom(this.mState.get());
            data.setInternalStateTo(2);
            if (this.mLauncher.isHomeStage()) {
                getStageManager().switchInternalState(this, data);
            } else {
                getStageManager().finishAllStage(data);
            }
        }
    }

    public void onStageEnterDragState(boolean animated) {
        enterDragState(animated);
    }

    public void exitDragStateDelayed() {
        exitDragStateDelayed(100);
    }

    void exitDragStateDelayed(int delay) {
        StageManager stageManager = getStageManager();
        if (stageManager != null) {
            if (stageManager.getTopStage().getMode() == 5) {
                enterNormalState(true);
            }
            if (!this.mState.equal(2)) {
                return;
            }
            if (stageManager.getTopStage() == this) {
                this.mExitDragStateHandler.postDelayed(new Runnable() {
                    public void run() {
                        HomeController.this.enterNormalState(true);
                    }
                }, (long) delay);
                return;
            }
            StageEntry data = new StageEntry();
            data.enableAnimation = false;
            data.setInternalStateFrom(this.mState.get());
            data.setInternalStateTo(1);
            switchInternalState(data);
        }
    }

    boolean canEnterResizeMode(AppWidgetHostView hostView, CellLayout layout) {
        return canEnterResizeMode(hostView, layout, true);
    }

    boolean canEnterResizeMode(AppWidgetHostView hostView, CellLayout layout, boolean checkState) {
        if (checkState && (hostView == null || !this.mState.equal(2))) {
            return false;
        }
        LauncherAppWidgetProviderInfo pInfo = (LauncherAppWidgetProviderInfo) hostView.getAppWidgetInfo();
        if (pInfo == null) {
            return false;
        }
        int resizeMode = (pInfo.getSupportedSpans() == null || pInfo.getSupportedSpans().size() <= 0) ? pInfo.resizeMode : pInfo.resizeMode();
        if ((resizeMode == 1 && pInfo.getMinSpanX() >= layout.getCountX()) || ((resizeMode == 2 && pInfo.getMinSpanY() >= layout.getCountY()) || (pInfo.getMinSpanX() >= layout.getCountX() && pInfo.getMinSpanY() >= layout.getCountY()))) {
            return false;
        }
        if (resizeMode == 0) {
            return false;
        }
        ArrayList<int[]> supportSpans = pInfo.getSupportedSpans();
        if (supportSpans != null && supportSpans.size() > 0) {
            int supportCount = 0;
            int[] cellXY = new int[]{layout.getCountX(), layout.getCountY()};
            Iterator it;
            if (pInfo.resizeMode() == 1 || pInfo.resizeMode() == 2) {
                int idx = pInfo.resizeMode() == 1 ? 0 : 1;
                it = supportSpans.iterator();
                while (it.hasNext()) {
                    if (((int[]) it.next())[idx] <= cellXY[idx]) {
                        supportCount++;
                    }
                }
            } else {
                it = supportSpans.iterator();
                while (it.hasNext()) {
                    int[] span = (int[]) it.next();
                    if (span[0] <= cellXY[0] && span[1] <= cellXY[1]) {
                        supportCount++;
                    }
                }
            }
            if (supportCount < 2) {
                return false;
            }
        }
        return true;
    }

    void enterResizeStateDelay(final AppWidgetHostView hostView, final CellLayout layout, int delay) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                HomeController.this.enterResizeState(hostView, layout);
                HomeController.this.mWorkspace.hideHintPages();
            }
        }, (long) delay);
    }

    void enterResizeStateDelay(AppWidgetHostView hostView, CellLayout layout) {
        enterResizeStateDelay(hostView, layout, 200);
    }

    private void enterResizeState(final AppWidgetHostView hostView, final CellLayout layout) {
        Runnable addResizeFrame = new Runnable() {
            public void run() {
                Utilities.changeEdgeHandleState(HomeController.this.mLauncher, false);
                HomeController.this.mState.set(3);
                HomeController.this.mWorkspace.setCrosshairsVisibilityChilds(0);
                HomeController.this.mDragLayer.addResizeFrame(HomeController.this.mWorkspace.getDragController(), (LauncherAppWidgetHostView) hostView, layout);
            }
        };
        if (((long) this.mWorkspace.getPageIndexForScreenId(this.mWorkspace.getIdForScreen(layout))) != ((long) this.mWorkspace.getNextPage())) {
            enterNormalState(true);
        } else if (this.mWorkspace.isPageMoving()) {
            this.mWorkspace.setDelayedResizeRunnable(addResizeFrame);
        } else {
            addResizeFrame.run();
        }
    }

    public void exitResizeState(boolean animated) {
        exitResizeState(animated, "4");
        this.mHomeKeyPressed = false;
    }

    public void exitResizeState(boolean animated, String detail) {
        if (this.mLauncher.isHomeStage() && this.mDragLayer.clearAllResizeFrames()) {
            Resources res = this.mLauncher.getResources();
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_DoneResizingWidget), detail);
            Talk.INSTANCE.say(this.mLauncher.getResources().getString(R.string.home_screen) + ", " + this.mWorkspace.getCurrentPageDescription());
            Utilities.changeEdgeHandleState(this.mLauncher, true);
            StageEntry data = new StageEntry();
            data.enableAnimation = animated;
            data.setInternalStateFrom(this.mState.get());
            data.setInternalStateTo(1);
            getStageManager().switchInternalState(this, data);
        }
    }

    public int getState() {
        return this.mState.get();
    }

    public View getContainerView() {
        return this.mWorkspace;
    }

    public boolean onLongClick(View v) {
        if (isWorkspaceLocked() || !this.mLauncher.isHomeStage()) {
            return false;
        }
        if ((this.mTrayManager != null && this.mTrayManager.isMoving()) || isScreenGridState() || this.mScrollDeterminator.cancelLongPressOnHScroll() || isSwitchingState() || ZeroPageController.isMoving() || this.mDragMgr.isQuickOptionShowing()) {
            return false;
        }
        initBounceAnimation();
        Resources res;
        if (v instanceof Workspace) {
            if (isOverviewState() || this.mWorkspace.isTouchActive()) {
                return false;
            }
            this.mWorkspace.performHapticFeedback(50025, 1);
            enterOverviewState(true);
            res = this.mLauncher.getResources();
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_1xxx), res.getString(R.string.event_HomeOptions), "2");
            return true;
        } else if ((v instanceof WorkspaceCellLayout) && this.mWorkspace.hasTargetView()) {
            return false;
        } else {
            if ((v.getTag() instanceof IconInfo) && ((IconInfo) v.getTag()).isAppsButton) {
                return false;
            }
            boolean animated = true;
            if (v instanceof AppWidgetResizeFrame) {
                v = ((AppWidgetResizeFrame) v).getResizeWidgetView();
                animated = false;
            }
            CellInfo longClickCellInfo = null;
            View itemUnderLongClick = null;
            if (v.getTag() instanceof ItemInfo) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (v.getParent() == null) {
                    Log.d(TAG, "v's getParent() is null, v screenID = " + info.screenId);
                    return false;
                }
                longClickCellInfo = new CellInfo(v, info);
                itemUnderLongClick = longClickCellInfo.cell;
                resetAddInfo();
            }
            if (!this.mDragMgr.isDragging()) {
                if (itemUnderLongClick == null) {
                    if (isOverviewState()) {
                        if (this.mWorkspace.getCurrentPage() == this.mWorkspace.indexOfChild(v)) {
                            this.mWorkspace.startReordering(v);
                            this.mHomePageReorder = true;
                        }
                    } else if (isSelectState()) {
                        startDragEmptyCell();
                    } else {
                        this.mWorkspace.performHapticFeedback(50025, 1);
                        enterOverviewState(true);
                        res = this.mLauncher.getResources();
                        SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_1xxx), res.getString(R.string.event_HomeOptions), "2");
                    }
                } else if (!(itemUnderLongClick instanceof FolderView)) {
                    if (!isSelectState()) {
                        boolean allowQuickOption;
                        enterDragState(animated);
                        if (isOverviewState()) {
                            allowQuickOption = false;
                        } else {
                            allowQuickOption = true;
                        }
                        if (!startDrag(longClickCellInfo, allowQuickOption, false)) {
                            exitDragStateDelayed();
                        }
                    } else if (this.mMultiSelectManager.canLongClick(itemUnderLongClick)) {
                        enterDragState(animated);
                        if (!startDrag(longClickCellInfo, false, false)) {
                            exitDragStateDelayed();
                        }
                    }
                }
            }
            return true;
        }
    }

    private void startDragEmptyCell() {
        if (!isSwitchingState()) {
            ArrayList<View> appsViewList = this.mMultiSelectManager.getCheckedAppsViewList();
            if (appsViewList != null && appsViewList.size() > 0) {
                View target = getTargetView(appsViewList);
                if (target != null) {
                    CellInfo cellInfo = new CellInfo(target, (ItemInfo) target.getTag());
                    enterDragState(true);
                    startDrag(cellInfo, false, true);
                }
            }
        }
    }

    public DragSource getDragSourceForLongKey() {
        if (getWorkspace() != null) {
            return getWorkspace().getDragController();
        }
        return super.getDragSourceForLongKey();
    }

    public boolean onClick(View v) {
        if (isSwitchingState() || ZeroPageController.isMoving()) {
            return true;
        }
        if (this.mTrayManager != null && this.mTrayManager.isMoving()) {
            return true;
        }
        initBounceAnimation();
        if ((v instanceof CellLayout) && isOverviewState()) {
            int indexToClick = this.mWorkspace.indexOfChild(v);
            if (indexToClick != this.mWorkspace.getCurrentPage()) {
                this.mWorkspace.checkVisibilityOfCustomLayout(indexToClick);
                this.mWorkspace.moveToScreen(indexToClick, false);
            }
            enterNormalState(true);
            return true;
        }
        Object tag = v.getTag();
        if (tag instanceof FolderInfo) {
            if (!(v instanceof FolderIconView)) {
                return true;
            }
            if (LauncherFeature.supportFolderSelect() && isSelectState()) {
                ((FolderIconView) v).getCheckBox().toggle();
                return true;
            } else if (!this.mState.equal(1)) {
                return true;
            } else {
                this.mLauncher.onClickFolderIcon(v);
                return true;
            }
        } else if (tag instanceof IconInfo) {
            if (!isSelectState()) {
                onClickAppShortcut(v);
                return true;
            } else if (!(v instanceof IconView) || ((IconInfo) tag).isAppsButton) {
                return true;
            } else {
                ((IconView) v).getCheckBox().toggle();
                return true;
            }
        } else if (!(tag instanceof LauncherAppWidgetInfo)) {
            return false;
        } else {
            if (isSelectState() || !(v instanceof PendingAppWidgetHostView)) {
                return true;
            }
            if (this.mLauncher.isSafeModeEnabled()) {
                Toast.makeText(this.mLauncher, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            }
            onClickPendingWidget((PendingAppWidgetHostView) v);
            return true;
        }
    }

    private void onClickAppShortcut(View v) {
        ItemInfo tag = (ItemInfo)v.getTag();
        if (tag instanceof IconInfo) {
            IconInfo shortcut = (IconInfo)tag;
            if (shortcut.isAppsButton) {
                this.mLauncher.showAppsView(true, false);
                Resources res = this.mLauncher.getResources();
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_1xxx), res.getString(R.string.event_Apps), "3");
                return;
            } else if (shortcut.isDisabled == 0 || ((shortcut.isDisabled & -5) & -9) == 0) {
                this.mLauncher.startAppShortcutOrInfoActivity(v);
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_ICON_STARTED, shortcut.getIntent().getComponent() != null ? shortcut.getIntent().getComponent().getPackageName() : null, -1, false);
                return;
            } else if (TextUtils.isEmpty(shortcut.disabledMessage)) {
                int error = R.string.activity_not_found;
                if ((shortcut.isDisabled & 1) != 0) {
                    error = R.string.safemode_shortcut_error;
                }
                Toast.makeText(this.mLauncher, error, Toast.LENGTH_SHORT).show();
                return;
            } else {
                Toast.makeText(this.mLauncher, shortcut.disabledMessage, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        throw new IllegalArgumentException("Input must be a Shortcut");
    }

    private void onClickPendingWidget(PendingAppWidgetHostView v) {
        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            int widgetId = info.appWidgetId;
            AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(widgetId);
            if (appWidgetInfo != null) {
                this.mPendingAddWidgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, appWidgetInfo);
                this.mPendingAddInfo.copyFrom(info);
                this.mPendingAddWidgetId = widgetId;
                AppWidgetManagerCompat.getInstance(this.mLauncher).startConfigActivity(appWidgetInfo, info.appWidgetId, this.mLauncher, this.mAppWidgetHost, 12);
                return;
            }
            return;
        }
        Utilities.startActivitySafely(this.mLauncher, v, LauncherModel.getMarketIntent(info.providerName.getPackageName()), info);
    }

    public boolean isRestoring() {
        return this.mRestoring;
    }

    public void setRestoring(boolean restoring) {
        this.mRestoring = restoring;
    }

    boolean isWorkspaceLocked() {
        return this.mHomeBindController.isWorkspaceLoading() || this.mWaitingForResult;
    }

    public void setWaitingForResult(boolean value) {
        this.mWaitingForResult = value;
    }

    public boolean isWaitingForResult() {
        return this.mWaitingForResult;
    }

    public HomeBindController getBindController() {
        return this.mHomeBindController;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    public Hotseat getHotseat() {
        return this.mHotseat;
    }

    OverviewPanel getOverviewPanel() {
        return this.mOverviewPanel;
    }

    ScreenGridPanel getScreenGridPanel() {
        return this.mScreenGridPanel;
    }

    View getHomePageIndicatorView() {
        return this.mPageIndicatorView;
    }

    FestivalPageController getFestivalPageController() {
        return this.mFestivalPageController;
    }

    boolean isSwitchingState() {
        return (getStageManager() != null && getStageManager().isRunningAnimation()) || (!isSelectState() && isRunningStateChangeAnimation());
    }

    public boolean isModalState() {
        return !this.mState.equal(1);
    }

    boolean isOverviewState() {
        return this.mState.equal(4);
    }

    boolean isNormalState() {
        return this.mState.equal(1);
    }

    boolean isSelectState() {
        return this.mState.equal(6);
    }

    public boolean isScreenGridState() {
        return this.mState.equal(5);
    }

    boolean startDrag(CellInfo cellInfo, boolean allowQuickOption, boolean fromEmptyCell) {
        View child = cellInfo.cell;
        if (!Utilities.ATLEAST_O && !child.isInTouchMode()) {
            return false;
        }
        if ((child instanceof AppWidgetHostView) && ((LauncherAppWidgetHostView) child).mPrvHostView != null) {
            return false;
        }
        if ((child instanceof FrameLayout) && LauncherAppWidgetHostView.PRV_HOSTVIEW.equals(child.getTag())) {
            return false;
        }
        if (child.getParent() == null || child.getParent().getParent() == null) {
            Log.i(TAG, "parent of child is null, child = " + child.getTag());
            return false;
        }
        boolean isHotseat;
        boolean z;
        child.setVisibility(View.INVISIBLE);
        if (cellInfo.container == -101) {
            isHotseat = true;
        } else {
            isHotseat = false;
        }
        if (isHotseat) {
            this.mLauncher.beginDragShared(child, this.mHotseat.getDragController(), allowQuickOption, fromEmptyCell);
        } else {
            this.mLauncher.beginDragShared(child, this.mWorkspace.getDragController(), allowQuickOption, fromEmptyCell);
        }
        WorkspaceDragController dragController = this.mWorkspace.getDragController();
        if (isHotseat) {
            z = false;
        } else {
            z = true;
        }
        dragController.startDrag(cellInfo, z, fromEmptyCell);
        this.mHotseat.getDragController().startDrag(cellInfo, isHotseat);
        return true;
    }

    private void requestCreateOrPickAppWidget(int requestCode, int resultCode, Intent data) {
        int appWidgetId;
        int pendingAddWidgetId = this.mPendingAddWidgetId;
        this.mPendingAddWidgetId = -1;
        int widgetId = data != null ? data.getIntExtra(Favorites.APPWIDGET_ID, -1) : -1;
        if (widgetId < 0) {
            appWidgetId = pendingAddWidgetId;
        } else {
            appWidgetId = widgetId;
        }
        if (appWidgetId < 0 || resultCode == 0) {
            Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the widget configuration activity.");
            completeTwoStageWidgetDrop(0, appWidgetId);
            Runnable onComplete = new Runnable() {
                public void run() {
                    HomeController.this.exitDragStateDelayed(0);
                }
            };
            if (isWorkspaceLocked()) {
                this.mWorkspace.postDelayed(onComplete, 500);
            } else {
                this.mWorkspace.removeExtraEmptyScreenDelayed(onComplete, 500);
            }
        } else if (isWorkspaceLocked()) {
            sPendingAddItem = preparePendingAddArgs(requestCode, data, appWidgetId, this.mPendingAddInfo);
        } else {
            if (this.mPendingAddInfo.container == -100) {
                this.mPendingAddInfo.screenId = ensurePendingDropLayoutExists(this.mPendingAddInfo.screenId);
            }
            completeTwoStageWidgetDrop(resultCode, appWidgetId);
            if (resultCode != -1) {
                this.mWorkspace.removeExtraEmptyScreenDelayed(null, 500);
            }
        }
    }

    private void requestBindAppWidget(int resultCode, Intent data) {
        int appWidgetId;
        this.mPendingAddWidgetId = -1;
        Runnable exitDragStateRunnable = new Runnable() {
            public void run() {
                HomeController.this.exitDragStateDelayed();
            }
        };
        if (data != null) {
            appWidgetId = data.getIntExtra(Favorites.APPWIDGET_ID, -1);
        } else {
            appWidgetId = -1;
        }
        if (resultCode == 0) {
            completeTwoStageWidgetDrop(0, appWidgetId);
            this.mWorkspace.removeExtraEmptyScreenDelayed(exitDragStateRunnable, 500);
        } else if (resultCode == -1) {
            addAppWidgetImpl(appWidgetId, this.mPendingAddInfo, null, this.mPendingAddWidgetInfo, 500);
        }
    }

    private void requestReconfigureAppWidget(int requestCode, int resultCode, Intent data) {
        int pendingAddWidgetId = this.mPendingAddWidgetId;
        this.mPendingAddWidgetId = -1;
        if (resultCode == -1) {
            PendingAddArguments args = preparePendingAddArgs(requestCode, data, pendingAddWidgetId, this.mPendingAddInfo);
            if (isWorkspaceLocked()) {
                sPendingAddItem = args;
            } else {
                completeAdd(args);
            }
        }
    }

    void requestCreateShortcut(int requestCode, int resultCode, Intent data) {
        Runnable exitDragStateRunnable = new Runnable() {
            public void run() {
                HomeController.this.exitDragStateDelayed();
            }
        };
        if (resultCode == -1 && this.mPendingAddInfo.container != -1) {
            PendingAddArguments args = preparePendingAddArgs(requestCode, data, -1, this.mPendingAddInfo);
            if (isWorkspaceLocked()) {
                sPendingAddItem = args;
                return;
            }
            completeAdd(args);
            if (this.mDragLayer.getAnimatedView() == null) {
                exitDragStateRunnable.run();
            } else {
                this.mWorkspace.postDelayed(exitDragStateRunnable, 500);
            }
        } else if (resultCode == 0) {
            this.mHotseat.getDragController().removeEmptyCells(true, true);
            this.mWorkspace.postDelayed(exitDragStateRunnable, 500);
        }
    }

    long completeAdd(PendingAddArguments args) {
        long screenId = args.screenId;
        if (args.container == -100) {
            screenId = ensurePendingDropLayoutExists(args.screenId);
        }
        switch (args.requestCode) {
            case 1:
                completeAddShortcut(args.intent, args.container, screenId, args.cellX, args.cellY);
                break;
            case 5:
                completeAddAppWidget(args.appWidgetId, args.container, screenId, null, null);
                break;
            case 12:
                completeRestoreAppWidget(args.appWidgetId);
                break;
        }
        resetAddInfo();
        return screenId;
    }

    private PendingAddArguments preparePendingAddArgs(int requestCode, Intent data, int appWidgetId, ItemInfo info) {
        PendingAddArguments args = new PendingAddArguments();
        args.requestCode = requestCode;
        args.intent = data;
        args.container = info.container;
        args.screenId = info.screenId;
        args.cellX = info.cellX;
        args.cellY = info.cellY;
        args.appWidgetId = appWidgetId;
        return args;
    }

    private long ensurePendingDropLayoutExists(long screenId) {
        if (this.mWorkspace.getScreenWithId(screenId) != null) {
            return screenId;
        }
        this.mWorkspace.addExtraEmptyScreen();
        return this.mWorkspace.commitExtraEmptyScreen();
    }

    private void completeTwoStageWidgetDrop(int resultCode, final int appWidgetId) {
        final CellLayout cellLayout = this.mWorkspace.getScreenWithId(this.mPendingAddInfo.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;
        AppWidgetHostView boundWidget = null;
        if (resultCode == -1) {
            animationType = 3;
            final AppWidgetHostView hostView = this.mAppWidgetHost.createView(this.mLauncher, appWidgetId, this.mPendingAddWidgetInfo);
            boundWidget = hostView;
            onCompleteRunnable = new Runnable() {
                public void run() {
                    HomeController.this.completeAddAppWidget(appWidgetId, HomeController.this.mPendingAddInfo.container, HomeController.this.mPendingAddInfo.screenId, hostView, null);
                    if (HomeController.this.canEnterResizeMode(hostView, cellLayout)) {
                        HomeController.this.enterResizeStateDelay(hostView, cellLayout);
                    } else {
                        HomeController.this.exitDragStateDelayed();
                    }
                }
            };
        } else if (resultCode == 0) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = 4;
        }
        if (this.mLauncher.getDragLayer().getAnimatedView() != null) {
            this.mWorkspace.getDragController().animateWidgetDrop(this.mPendingAddInfo, cellLayout, (DragView) this.mLauncher.getDragLayer().getAnimatedView(), onCompleteRunnable, animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            onCompleteRunnable.run();
        }
    }

    private void completeAddShortcut(Intent data, long container, long screenId, int cellX, int cellY) {
        boolean foundCellSpan;
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        int[] touchXY = this.mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screenId);
        IconInfo info = null;
        if (LauncherFeature.supportDeepShortcut()) {
            PinItemRequestCompat request = PinItemRequestCompat.getPinItemRequest(data);
            if (request != null) {
                PinShortcutRequestActivityInfo pinShortcutRequestActivityInfo = new PinShortcutRequestActivityInfo(request, this.mLauncher);
                LauncherAppsCompat.acceptPinItemRequest(this.mLauncher, request, 0);
                IconInfo shortcutInfo = pinShortcutRequestActivityInfo.createShortcutInfo();
                if (shortcutInfo != null) {
                    info = shortcutInfo;
                }
            }
        }
        if (info == null) {
            info = InstallShortcutReceiver.fromShortcutIntent(this.mLauncher, data);
            if (info != null && this.mPendingAddInfo.componentName != null) {
                if (!Utilities.hasPermissionForActivity(this.mLauncher, info.intent, this.mPendingAddInfo.componentName.getPackageName())) {
                    Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
                    return;
                }
            }
            return;
        }
        View view = this.mHomeBindController.createShortcut(info);
        if (cellX < 0 || cellY < 0) {
            foundCellSpan = touchXY != null ? layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY) != null : layout.findCellForSpan(cellXY, 1, 1, false);
        } else {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;
            DragObject dragObject;
            if (layout instanceof HotseatCellLayout) {
                dragObject = new DragObject();
                dragObject.dragInfo = info;
                if (this.mHotseat.getDragController().createUserFolderIfNecessary(cellXY, view, dragObject) || this.mHotseat.getDragController().addToExistingFolderIfNecessary(cellXY, dragObject)) {
                    return;
                }
            }
            dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (!this.mWorkspace.getDragController().createUserFolderIfNecessary(cellXY, view, dragObject)) {
                if (this.mWorkspace.getDragController().addToExistingFolderIfNecessary(cellXY, dragObject)) {
                    return;
                }
            }
            return;
        }
        if (foundCellSpan) {
            addItemToDb(info, container, screenId, cellXY[0], cellXY[1]);
            if (!this.mRestoring) {
                addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1);
                return;
            }
            return;
        }
        showOutOfSpaceMessage();
    }

    private AppWidgetHostView completeAddAppWidget(int appWidgetId, long container, long screenId, AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {
        // TODO: Samsung specific code
//        AppWidgetProviderInfo appWidgetProviderInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
//        if (appWidgetProviderInfo == null) {
//            Log.d(TAG, "App widget provider info is null. AppWidgetID = " + appWidgetId);
//            return null;
//        }
//        AppWidgetProviderInfo appWidgetInfo2;
//        ItemInfo info = this.mPendingAddInfo;
//        if (appWidgetInfo == null) {
//            appWidgetInfo2 = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, appWidgetProviderInfo);
//        }
//        if (appWidgetInfo2.isCustomWidget) {
//            appWidgetId = -100;
//        }
//        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo2.provider);
//        launcherInfo.spanX = info.spanX;
//        launcherInfo.spanY = info.spanY;
//        launcherInfo.minSpanX = info.minSpanX;
//        launcherInfo.minSpanY = info.minSpanY;
//        launcherInfo.user = this.mAppWidgetManager.getUser(appWidgetInfo2);
//        addItemToDb(launcherInfo, container, screenId, info.cellX, info.cellY);
//        if (!this.mRestoring) {
//            if (hostView == null) {
//                launcherInfo.hostView = this.mAppWidgetHost.createView(this.mLauncher, appWidgetId, appWidgetInfo2);
//            } else {
//                launcherInfo.hostView = hostView;
//            }
//            launcherInfo.hostView.setTag(launcherInfo);
//            launcherInfo.hostView.setVisibility(View.VISIBLE);
//            launcherInfo.notifyWidgetSizeChanged(this.mLauncher);
//            addInScreen(launcherInfo.hostView, container, screenId, info.cellX, info.cellY, launcherInfo.spanX, launcherInfo.spanY);
//            this.mHomeBindController.addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo2);
//        }
//        resetAddInfo();
//        String packageName = launcherInfo.providerName.getPackageName();
//        if (sSingleInstanceAppWidgetList.containsKey(launcherInfo.providerName.flattenToShortString())) {
//            ((LongSparseArray) sSingleInstanceAppWidgetList.get(launcherInfo.providerName.flattenToShortString())).put(Long.valueOf(UserManagerCompat.getInstance(this.mLauncher).getSerialNumberForUser(launcherInfo.user)).longValue(), Integer.valueOf(1));
//        } else if (sSingleInstanceAppWidgetPackageList.containsKey(packageName)) {
//            ((LongSparseArray) sSingleInstanceAppWidgetPackageList.get(packageName)).put(Long.valueOf(UserManagerCompat.getInstance(this.mLauncher).getSerialNumberForUser(launcherInfo.user)).longValue(), Integer.valueOf(1));
//        }
//        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_WIDGET_ADD, packageName, -1, false);
//        return launcherInfo.hostView;
        return null;
    }

    private void resetAddInfo() {
        this.mPendingAddInfo.container = -1;
        this.mPendingAddInfo.screenId = -1;
        ItemInfo itemInfo = this.mPendingAddInfo;
        this.mPendingAddInfo.cellY = -1;
        itemInfo.cellX = -1;
        itemInfo = this.mPendingAddInfo;
        this.mPendingAddInfo.spanY = -1;
        itemInfo.spanX = -1;
        itemInfo = this.mPendingAddInfo;
        this.mPendingAddInfo.minSpanY = 1;
        itemInfo.minSpanX = 1;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.componentName = null;
    }

    private void addAppWidgetImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, LauncherAppWidgetProviderInfo appWidgetInfo) {
        addAppWidgetImpl(appWidgetId, info, boundWidget, appWidgetInfo, 0);
    }

    private void addAppWidgetImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, LauncherAppWidgetProviderInfo appWidgetInfo, int delay) {
        if (appWidgetInfo.configure != null) {
            this.mPendingAddWidgetInfo = appWidgetInfo;
            this.mPendingAddWidgetId = appWidgetId;
            this.mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this.mLauncher, this.mAppWidgetHost, 5);
            return;
        }
        Runnable onComplete;
        final AppWidgetHostView hostView = completeAddAppWidget(appWidgetId, info.container, info.screenId, boundWidget, appWidgetInfo);
        final CellLayout layout = this.mWorkspace.getScreenWithId(info.screenId);
        if (canEnterResizeMode(hostView, layout)) {
            onComplete = new Runnable() {
                public void run() {
                    HomeController.this.enterResizeStateDelay(hostView, layout);
                }
            };
        } else {
            onComplete = new Runnable() {
                public void run() {
                    HomeController.this.exitDragStateDelayed();
                }
            };
        }
        this.mWorkspace.postDelayed(onComplete, (long) delay);
    }

    void addPendingItem(PendingAddItemInfo info, long container, long screenId, int[] cell, int spanX, int spanY) {
        switch (info.itemType) {
            case 1:
                processShortcutFromDrop(info.componentName, container, screenId, cell);
                return;
            case 4:
            case 5:
                addAppWidgetFromDrop((PendingAddWidgetInfo) info, container, screenId, cell, new int[]{spanX, spanY});
                return;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }
    }

    private void processShortcutFromDrop(ComponentName componentName, long container, long screenId, int[] cell) {
        resetAddInfo();
        this.mPendingAddInfo.container = container;
        this.mPendingAddInfo.screenId = screenId;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.componentName = componentName;
        if (cell != null) {
            this.mPendingAddInfo.cellX = cell[0];
            this.mPendingAddInfo.cellY = cell[1];
        }
        Intent createShortcutIntent = new Intent("android.intent.action.CREATE_SHORTCUT");
        createShortcutIntent.setComponent(componentName);
        Utilities.startActivityForResultSafely(this.mLauncher, createShortcutIntent, 1);
    }

    private void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, long screenId, int[] cell, int[] span) {
        resetAddInfo();
        ItemInfo itemInfo = this.mPendingAddInfo;
        info.container = container;
        itemInfo.container = container;
        itemInfo = this.mPendingAddInfo;
        info.screenId = screenId;
        itemInfo.screenId = screenId;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.minSpanX = info.minSpanX;
        this.mPendingAddInfo.minSpanY = info.minSpanY;
        if (cell != null) {
            this.mPendingAddInfo.cellX = cell[0];
            this.mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            this.mPendingAddInfo.spanX = span[0];
            this.mPendingAddInfo.spanY = span[1];
        }
        AppWidgetHostView hostView = info.boundWidget;
        if (hostView != null) {
            addAppWidgetImpl(hostView.getAppWidgetId(), info, hostView, info.info);
            info.boundWidget = null;
            return;
        }
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.info, info.bindOptions)) {
            addAppWidgetImpl(appWidgetId, info, null, info.info);
        } else {
            this.mPendingAddWidgetInfo = info.info;
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_BIND");
            intent.putExtra(Favorites.APPWIDGET_ID, appWidgetId);
            intent.putExtra(Favorites.APPWIDGET_PROVIDER, info.componentName);
            this.mAppWidgetManager.getUser(this.mPendingAddWidgetInfo).addToIntent(intent, "appWidgetProviderProfile");
            Utilities.startActivityForResultSafely(this.mLauncher, intent, 11);
        }
        this.mHomeLoader.getItemPositionHelper().addToPreservedPosition(screenId, cell[0], cell[1], span[0], span[1]);
    }

    PendingAddArguments getPendingAddItem() {
        return sPendingAddItem;
    }

    void setPendingAddItem(PendingAddArguments arg) {
        sPendingAddItem = arg;
    }

    private void completeRestoreAppWidget(int appWidgetId) {
        LauncherAppWidgetHostView view = getWidgetForAppWidgetId(appWidgetId);
        if (view == null || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return;
        }
        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = 0;
        DataLoader.reinflateWidgetsIfNecessary();
        updateItemInDb(info);
    }

    private void showOutOfSpaceMessage() {
        Toast.makeText(this.mLauncher, R.string.out_of_space, Toast.LENGTH_SHORT).show();
    }

    void showNoSpacePage(boolean isFromApps) {
        if (isFromApps) {
            Toast.makeText(this.mLauncher, this.mLauncher.getString(R.string.no_space_page), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this.mLauncher, this.mLauncher.getString(R.string.no_space_page_for_widget), Toast.LENGTH_SHORT).show();
        }
    }

    void showNoSpacePageforHotseat() {
        Toast.makeText(this.mLauncher, this.mLauncher.getString(R.string.no_space_page_for_hotseat), Toast.LENGTH_SHORT).show();
    }

    public CellLayout getCellLayout(long container, long screenId) {
        if (container != -101) {
            return this.mWorkspace.getScreenWithId(screenId);
        }
        if (this.mHotseat != null) {
            return this.mHotseat.getLayout();
        }
        return null;
    }

    private CellLayout getParentCellLayoutForView(View v) {
        if (this.mWorkspace != null) {
            Iterator it = getWorkspaceAndHotseatCellLayouts().iterator();
            while (it.hasNext()) {
                CellLayout layout = (CellLayout) it.next();
                if (layout.getCellLayoutChildren().indexOfChild(v) > -1) {
                    return layout;
                }
            }
        }
        return null;
    }

    private ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList();
        int screenCount = this.mWorkspace.getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add((CellLayout) this.mWorkspace.getChildAt(screen));
        }
        if (this.mHotseat != null) {
            layouts.add(this.mHotseat.getLayout());
        }
        return layouts;
    }

    public FolderView getFolderForTag(final Object tag) {
        return (FolderView) getFirstMatch(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return (v instanceof FolderView) && ((FolderView) v).getInfo() == tag && ((FolderView) v).getInfo().opened;
            }
        });
    }

    public View getHomescreenIconByItemId(final long id) {
        return getFirstMatch(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info != null && info.id == id;
            }
        });
    }

    private View getViewForTag(final Object tag) {
        return getFirstMatch(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info == tag;
            }
        });
    }

    private LauncherAppWidgetHostView getWidgetForAppWidgetId(final int appWidgetId) {
        return (LauncherAppWidgetHostView) getFirstMatch(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return (info instanceof LauncherAppWidgetInfo) && ((LauncherAppWidgetInfo) info).appWidgetId == appWidgetId;
            }
        });
    }

    void clearDropTargets() {
        mapOverItems(false, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (v instanceof DropTarget) {
                    HomeController.this.mDragMgr.removeDropTarget((DropTarget) v);
                }
                return false;
            }
        });
    }

    private View getFirstMatch(final ItemOperator operator) {
        final View[] value = new View[1];
        mapOverItems(false, new ItemOperator() {
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

    private void mapOverItems(boolean recurse, ItemOperator op) {
        Iterator it = getAllCellLayoutChildren().iterator();
        while (it.hasNext()) {
            CellLayoutChildren container = (CellLayoutChildren) it.next();
            int itemCount = container.getChildCount();
            for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                View item = container.getChildAt(itemIdx);
                if (item.getTag() instanceof ItemInfo) {
                    ItemInfo info = (ItemInfo) item.getTag();
                    if (recurse && (info instanceof FolderInfo) && (item instanceof FolderIconView)) {
                        FolderIconView folder = (FolderIconView) item;
                        Iterator it2 = folder.getFolderView().getItemsInReadingOrder().iterator();
                        while (it2.hasNext()) {
                            View child = (View) it2.next();
                            if (op.evaluate((ItemInfo) child.getTag(), child, folder)) {
                                return;
                            }
                        }
                    } else if (op.evaluate(info, item, null)) {
                        return;
                    }
                }
            }
        }
    }

    private ArrayList<CellLayoutChildren> getAllCellLayoutChildren() {
        ArrayList<CellLayoutChildren> childrenLayouts = new ArrayList();
        int screenCount = 0;
        if (this.mWorkspace != null) {
            screenCount = this.mWorkspace.getChildCount();
        }
        for (int screen = 0; screen < screenCount; screen++) {
            childrenLayouts.add(((CellLayout) this.mWorkspace.getChildAt(screen)).getCellLayoutChildren());
        }
        if (this.mHotseat != null) {
            childrenLayouts.add(this.mHotseat.getLayout().getCellLayoutChildren());
        }
        return childrenLayouts;
    }

    ValueAnimator createNewAppBounceAnimation(final View v, int i) {
        PropertyValuesHolder[] r1 = new PropertyValuesHolder[3];
        r1[0] = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        r1[1] = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        r1[2] = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
        ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v, r1);
        bounceAnim.setDuration(450);
        bounceAnim.setStartDelay((long) (i * 85));
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        bounceAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                v.setAlpha(1.0f);
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }
        });
        return bounceAnim;
    }

    boolean canRunNewAppsAnimation() {
        return java.lang.System.currentTimeMillis() - this.mDragMgr.getLastGestureUpTime() > 5000;
    }

    void removeHomeItem(View v) {
        CellLayout parentCell = getParentCellLayoutForView(v);
        if (parentCell == null) {
            Log.e(TAG, "mDragInfo.cell has null parent");
        } else if (parentCell instanceof HotseatCellLayout) {
            parentCell.removeViewInLayout(v);
        } else {
            parentCell.removeView(v);
        }
        if ((v instanceof DropTarget) && this.mDragMgr != null) {
            this.mDragMgr.removeDropTarget((DropTarget) v);
        }
        if (this.mDragMgr != null && getState() == 2) {
            this.mDragMgr.cancelDragIfViewRemoved(v);
        }
    }

    public void removeHomeItem(ItemInfo item) {
        if (item instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) item;
            if (!(this.mAppWidgetHost == null || widget.isCustomWidget() || !widget.isWidgetIdValid())) {
                new AsyncTask<Void, Void, Void>() {
                    public Void doInBackground(Void... args) {
                        HomeController.this.mAppWidgetHost.deleteAppWidgetId(widget.appWidgetId);
                        return null;
                    }
                }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
            }
            removeHomeItem(widget.hostView);
            this.mHomeBindController.removeAppWidget(widget);
        } else if ((item instanceof IconInfo) || (item instanceof FolderInfo)) {
            View view = getHomescreenIconByItemId(item.container);
            if (isItemInFolder(item) && view != null && (view.getTag() instanceof FolderInfo)) {
                ((FolderInfo) view.getTag()).remove((IconInfo) item);
                Log.d(TAG, "removeHomeItem : " + item.toString());
                return;
            }
            removeHomeItem(getHomescreenIconByItemId(item.id));
        }
    }

    public boolean isItemInFolder(ItemInfo info) {
        return (info.container == -100 || info.container == -101 || info.container == -102) ? false : true;
    }

    void addInScreenFromBind(View child, long container, long screenId, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, false, false);
    }

    void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, isWorkspaceLocked(), false);
    }

    private void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY, boolean insert, boolean computeXYFromRank) {
        if (container == -100 && this.mWorkspace.getScreenWithId(screenId) == null) {
            Log.e(TAG, "Skipping child, screenId " + screenId + " not found");
            new Throwable().printStackTrace();
        } else if (screenId == -201) {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        } else {
            CellLayout layout;
            CellLayout.LayoutParams lp;
            ItemInfo info = (ItemInfo) child.getTag();
            if (container == -101) {
                CellLayout layout2 = this.mHotseat.getLayout();
                if (child instanceof IconView) {
                    ((IconView) child).setIconDisplay(1);
                }
                child.setOnKeyListener(HomeFocusHelper.HOTSEAT_ICON_KEY_LISTENER);
                if (computeXYFromRank) {
                    x = this.mHotseat.getCellXFromOrder((int) screenId);
                    y = this.mHotseat.getCellYFromOrder((int) screenId);
                    info.cellX = x;
                    info.cellY = y;
                    layout = layout2;
                } else {
                    if (!this.mHomeBindController.isWorkspaceLoading()) {
                        if (info instanceof FolderInfo) {
                            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_ADD, "Folder", -1, false);
                        } else {
                            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOTSEAT_ADD, info.getIntent().getComponent() != null ? info.getIntent().getComponent().getPackageName() : null, -1, false);
                        }
                        if (info.container != -101) {
                            Resources res = this.mLauncher.getResources();
                            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_QuickOptions), res.getString(R.string.event_AddToDock));
                            layout = layout2;
                        }
                    }
                    layout = layout2;
                }
            } else {
                layout = this.mWorkspace.getScreenWithId(screenId);
                if (child instanceof IconView) {
                    ((IconView) child).setIconDisplay(0);
                    ((IconView) child).changeTextColorForBg(WhiteBgManager.isWhiteBg());
                }
                child.setOnKeyListener(HomeFocusHelper.WORKSPACE_ICON_KEY_LISTENER);
            }
            ViewGroup.LayoutParams genericLp = child.getLayoutParams();
            if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
                lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
            } else {
                lp = (CellLayout.LayoutParams) genericLp;
                lp.tmpCellX = x;
                lp.cellX = x;
                lp.tmpCellY = y;
                lp.cellY = y;
                lp.cellHSpan = spanX;
                lp.cellVSpan = spanY;
            }
            if (spanX < 0 && spanY < 0) {
                lp.isLockedToGrid = false;
            }
            if (!layout.addViewToCellLayout(child, insert ? 0 : -1, this.mLauncher.getViewIdForItem(info), lp, !(child instanceof FolderView))) {
                Launcher.addDumpLog(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout", true);
            }
            if (!(child instanceof FolderView)) {
                child.setHapticFeedbackEnabled(false);
                child.setOnLongClickListener(this);
            }
            if (child instanceof DropTarget) {
                this.mDragMgr.addDropTarget((DropTarget) child);
            }
            checkIfConfigIsDifferentFromActivity();
        }
    }

    void removeItemsOnScreen(ArrayList<ItemInfo> removeItems) {
        Iterator it = removeItems.iterator();
        while (it.hasNext()) {
            removeHomeOrFolderItem((ItemInfo) it.next(), null, false);
        }
    }

    public boolean removeHomeOrFolderItem(ItemInfo item, View view) {
        return removeHomeOrFolderItem(item, view, false);
    }

    public boolean removeHomeOrFolderItem(ItemInfo item, View view, boolean restoreDeepShortCut) {
        if (item instanceof IconInfo) {
            deleteItemFromDb(item, restoreDeepShortCut);
            if (((IconInfo) item).isAppsButton) {
                LauncherAppState.getInstance().setAppsButtonEnabled(false);
            } else if (isItemInFolder(item)) {
                View folderView = getHomescreenIconByItemId(item.container);
                if (folderView != null) {
                    FolderInfo folderItem = (FolderInfo) folderView.getTag();
                    if (folderItem != null) {
                        folderItem.remove((IconInfo) item);
                    }
                }
            }
        } else if (item instanceof FolderInfo) {
            FolderInfo folder = (FolderInfo) item;
            if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                int moveToPage = moveFolderItemsToHome(folder.contents);
                if (this.mWorkspace.getCurrentPage() != moveToPage) {
                    this.mWorkspace.snapToPage(moveToPage);
                }
            }
            deleteItemInFolderFromDb(folder);
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_DELETE_HOME_FOLDER, null, -1, false);
        } else if (!(item instanceof LauncherAppWidgetInfo)) {
            return false;
        } else {
            final LauncherAppWidgetInfo widget = (LauncherAppWidgetInfo) item;
            if (LauncherFeature.supportHotword() && widget.providerName != null && widget.providerName.equals(Launcher.GOOGLE_SEARCH_WIDGET)) {
                this.mLauncher.setHotWordDetection(false);
            }
            this.mHomeBindController.removeAppWidget(widget);
            deleteItemFromDb(widget);
            if (!(this.mAppWidgetHost == null || widget.isCustomWidget() || !widget.isWidgetIdValid())) {
                new AsyncTask<Void, Void, Void>() {
                    public Void doInBackground(Void... args) {
                        HomeController.this.mAppWidgetHost.deleteAppWidgetId(widget.appWidgetId);
                        return null;
                    }
                }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
            }
            if (widget.providerName != null && sSingleInstanceAppWidgetList.containsKey(widget.providerName.flattenToShortString())) {
                ((LongSparseArray) sSingleInstanceAppWidgetList.get(widget.providerName.flattenToShortString())).put(Long.valueOf(UserManagerCompat.getInstance(this.mLauncher).getSerialNumberForUser(widget.user)).longValue(), 0);
            } else if (widget.providerName != null && sSingleInstanceAppWidgetPackageList.containsKey(widget.providerName.getPackageName())) {
                ((LongSparseArray) sSingleInstanceAppWidgetPackageList.get(widget.providerName.getPackageName())).put(Long.valueOf(UserManagerCompat.getInstance(this.mLauncher).getSerialNumberForUser(widget.user)).longValue(), 0);
            }
        }
        if (view != null) {
            removeHomeItem(view);
        }
        return true;
    }

    private int moveFolderItemsToHome(ArrayList<IconInfo> folderContents) {
        int moveToScreen = 0;
        int index = this.mWorkspace.getChildCount() - 1;
        while (index > 0) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(index);
            if (cl != null && cl.getCellLayoutChildren().getChildCount() > 0) {
                break;
            }
            index--;
        }
        Iterator it = folderContents.iterator();
        while (it.hasNext()) {
            IconInfo iconInfo = (IconInfo) it.next();
            int[] targetCell = new int[2];
            iconInfo.container = -100;
            iconInfo.screenId = findEmptyCell(targetCell, index, true);
            iconInfo.cellX = targetCell[0];
            iconInfo.cellY = targetCell[1];
            moveToScreen = this.mWorkspace.getPageIndexForScreenId(iconInfo.screenId);
            updateItemInDb(iconInfo);
            this.mHomeBindController.bindItem(iconInfo, false);
        }
        return moveToScreen;
    }

    void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getCellLayoutChildren().getChildCount();
        long screenId = this.mWorkspace.getIdForScreen(cl);
        int container = -100;
        if (cl instanceof HotseatCellLayout) {
            screenId = -1;
            container = Favorites.CONTAINER_HOTSEAT;
        }
        for (int i = 0; i < count; i++) {
            View v = cl.getCellLayoutChildren().getChildAt(i);
            if (v.getTag() instanceof ItemInfo) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (info != null && info.requiresDbUpdate) {
                    info.requiresDbUpdate = false;
                    modifyItemInDb(info, (long) container, screenId, info.cellX, info.cellY, info.spanX, info.spanY);
                }
            }
        }
    }

    void disableShortcutsByPackageName(ArrayList<String> packages, UserHandleCompat user, int reason, IconCache iconCache) {
        final HashSet<String> packageNames = new HashSet();
        final ArrayList<FolderIconView> folderIconsToRefresh = new ArrayList();
        packageNames.addAll(packages);
        final UserHandleCompat userHandleCompat = user;
        final IconCache iconCache2 = iconCache;
        mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((info instanceof IconInfo) && (v instanceof IconView)) {
                    IconInfo iconInfo = (IconInfo) info;
                    ComponentName cn = iconInfo.getTargetComponent();
                    if (userHandleCompat.equals(iconInfo.user) && cn != null && packageNames.contains(cn.getPackageName())) {
                        ((IconView) v).applyFromShortcutInfo(iconInfo, iconCache2);
                        if ((parent instanceof FolderIconView) && !folderIconsToRefresh.contains(parent)) {
                            folderIconsToRefresh.add((FolderIconView) parent);
                        }
                    }
                }
                return false;
            }
        });
        for (int i = 0; i < folderIconsToRefresh.size(); i++) {
            FolderIconView folderIconView = (FolderIconView) folderIconsToRefresh.get(i);
            if (folderIconView != null) {
                folderIconView.refreshFolderIcon();
            }
        }
    }

    void removeItemsByPackageName(ArrayList<String> packages, final UserHandleCompat user) {
        final HashSet<String> packageNames = new HashSet();
        packageNames.addAll(packages);
        HashSet<ItemInfo> infos = new HashSet();
        final HashSet<ComponentName> cns = new HashSet();
        Iterator it = getWorkspaceAndHotseatCellLayouts().iterator();
        while (it.hasNext()) {
            ViewGroup layout = ((CellLayout) it.next()).getCellLayoutChildren();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = layout.getChildAt(i);
                if (view.getTag() instanceof ItemInfo) {
                    infos.add((ItemInfo) view.getTag());
                }
            }
        }
        DataLoader.filterItemInfo(infos, new ItemInfoFilter() {
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (!packageNames.contains(cn.getPackageName()) || !info.user.equals(user)) {
                    return false;
                }
                cns.add(cn);
                return true;
            }
        }, false);
        removeItemsByComponentName(cns, user);
    }

    void removeItemsByComponentName(HashSet<ComponentName> componentNames, UserHandleCompat user) {
        for (CellLayout layoutParent : getWorkspaceAndHotseatCellLayouts()) {
            Iterator it2;
            ViewGroup layout = layoutParent.getCellLayoutChildren();
            final HashMap<ItemInfo, View> children = new HashMap();
            for (int j = 0; j < layout.getChildCount(); j++) {
                View view = layout.getChildAt(j);
                if (view.getTag() instanceof ItemInfo) {
                    children.put((ItemInfo) view.getTag(), view);
                }
            }
            final ArrayList<View> childrenToRemove = new ArrayList();
            final HashMap<FolderInfo, ArrayList<IconInfo>> folderAppsToRemove = new HashMap();
            final HashSet<ComponentName> hashSet = componentNames;
            final UserHandleCompat userHandleCompat = user;
            DataLoader.filterItemInfo(children.keySet(), new ItemInfoFilter() {
                public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                    if (parent instanceof FolderInfo) {
                        if (hashSet.contains(cn) && info.user.equals(userHandleCompat)) {
                            ArrayList<IconInfo> appsToRemove;
                            FolderInfo folder = (FolderInfo) parent;
                            if (folderAppsToRemove.containsKey(folder)) {
                                appsToRemove = (ArrayList) folderAppsToRemove.get(folder);
                            } else {
                                appsToRemove = new ArrayList();
                                folderAppsToRemove.put(folder, appsToRemove);
                            }
                            appsToRemove.add((IconInfo) info);
                            return true;
                        }
                    } else if (hashSet.contains(cn) && info.user.equals(userHandleCompat)) {
                        childrenToRemove.add(children.get(info));
                        return true;
                    }
                    return false;
                }
            }, false);
            for (FolderInfo folder : folderAppsToRemove.keySet()) {
                it2 = ((ArrayList) folderAppsToRemove.get(folder)).iterator();
                while (it2.hasNext()) {
                    folder.remove((IconInfo) it2.next());
                }
            }
            boolean needToVI = this.mWorkspace.getCurrentPage() == this.mWorkspace.indexOfChild(layoutParent);
            it2 = childrenToRemove.iterator();
            while (it2.hasNext()) {
                final ItemInfo item;
                final View child = (View) it2.next();
                if (child == null) {
                    item = null;
                } else {
                    item = (ItemInfo) child.getTag();
                }
                if (needToVI) {
                    Animator animator = AnimatorInflater.loadAnimator(this.mWorkspace.getContext(), R.animator.icon_view_remove);
                    animator.setTarget(child);
                    animator.setDuration(200);
                    animator.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animator) {
                        }

                        public void onAnimationRepeat(Animator animator) {
                        }

                        public void onAnimationEnd(Animator animator) {
                            HomeController.this.removeHomeOrFolderItem(item, child);
                        }

                        public void onAnimationCancel(Animator animator) {
                        }
                    });
                    animator.start();
                } else {
                    removeHomeOrFolderItem(item, child);
                }
                if (child instanceof DropTarget) {
                    this.mDragMgr.removeDropTarget((DropTarget) child);
                }
                if (!(this.mMultiSelectManager == null || child == null || !isSelectState())) {
                    this.mMultiSelectManager.removeCheckedApp(child);
                }
            }
            if (childrenToRemove.size() > 0) {
                layout.requestLayout();
                layout.invalidate();
            }
            if (childrenToRemove.size() > 0 || folderAppsToRemove.size() > 0) {
                notifyCapture(true);
                if (this.mWorkspace.getDefaultPage() == this.mWorkspace.indexOfChild(layoutParent)) {
                    updateNotificationHelp(true);
                }
            }
        }
    }

    void removeItemsByMatcher(ItemInfoMatcher matcher) {
        for (CellLayout layoutParent : getWorkspaceAndHotseatCellLayouts()) {
            Iterator it2;
            ViewGroup layout = layoutParent.getCellLayoutChildren();
            final HashMap<ItemInfo, View> children = new HashMap();
            for (int j = 0; j < layout.getChildCount(); j++) {
                View view = layout.getChildAt(j);
                if (view.getTag() instanceof ItemInfo) {
                    children.put((ItemInfo) view.getTag(), view);
                }
            }
            final ArrayList<View> childrenToRemove = new ArrayList();
            final HashMap<FolderInfo, ArrayList<IconInfo>> folderAppsToRemove = new HashMap();
            final ItemInfoMatcher itemInfoMatcher = matcher;
            DataLoader.filterItemInfo(children.keySet(), new ItemInfoFilter() {
                public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                    if (parent instanceof FolderInfo) {
                        if (!itemInfoMatcher.matches(info, cn)) {
                            return true;
                        }
                        ArrayList<IconInfo> appsToRemove;
                        FolderInfo folder = (FolderInfo) parent;
                        if (folderAppsToRemove.containsKey(folder)) {
                            appsToRemove = (ArrayList) folderAppsToRemove.get(folder);
                        } else {
                            appsToRemove = new ArrayList();
                            folderAppsToRemove.put(folder, appsToRemove);
                        }
                        appsToRemove.add((IconInfo) info);
                        return true;
                    } else if (!itemInfoMatcher.matches(info, cn)) {
                        return false;
                    } else {
                        childrenToRemove.add(children.get(info));
                        return true;
                    }
                }
            }, false);
            for (FolderInfo folder : folderAppsToRemove.keySet()) {
                it2 = ((ArrayList) folderAppsToRemove.get(folder)).iterator();
                while (it2.hasNext()) {
                    folder.remove((IconInfo) it2.next());
                }
            }
            boolean needToVI = this.mWorkspace.getCurrentPage() == this.mWorkspace.indexOfChild(layoutParent);
            it2 = childrenToRemove.iterator();
            while (it2.hasNext()) {
                final View child = (View) it2.next();
                if (needToVI) {
                    Animator animator = AnimatorInflater.loadAnimator(this.mWorkspace.getContext(), R.animator.icon_view_remove);
                    animator.setTarget(child);
                    animator.setDuration(200);
                    final CellLayout view2 = layoutParent;
                    animator.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animator) {
                        }

                        public void onAnimationRepeat(Animator animator) {
                        }

                        public void onAnimationEnd(Animator animator) {
                            view2.removeViewInLayout(child);
                        }

                        public void onAnimationCancel(Animator animator) {
                        }
                    });
                    animator.start();
                } else {
                    layoutParent.removeViewInLayout(child);
                    layoutParent.removeViewInLayout(child);
                }
                if (child instanceof DropTarget) {
                    this.mDragMgr.removeDropTarget((DropTarget) child);
                }
                if (!(this.mMultiSelectManager == null || child == null || !isSelectState())) {
                    this.mMultiSelectManager.removeCheckedApp(child);
                }
            }
            if (childrenToRemove.size() > 0) {
                layout.requestLayout();
                layout.invalidate();
            }
            if (childrenToRemove.size() > 0 || folderAppsToRemove.size() > 0) {
                notifyCapture(true);
                if (this.mWorkspace.getDefaultPage() == this.mWorkspace.indexOfChild(layoutParent)) {
                    updateNotificationHelp(true);
                }
            }
        }
    }

    void updateShortcuts(ArrayList<IconInfo> shortcuts, final IconCache iconCache) {
        final HashSet<IconInfo> updates = new HashSet(shortcuts);
        final ArrayList<FolderIconView> folderIconsToRefresh = new ArrayList();
        mapOverItems(true, new ItemOperator() {
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
                    //IconCache iconCache = iconCache;
                    if (iconInfo.isPromise() == oldPromiseState) {
                        z = false;
                    }
                    iconView.applyFromShortcutInfo(iconInfo, iconCache, z);
                    if ((parent instanceof FolderIconView) && info.rank < 9 && !folderIconsToRefresh.contains(parent)) {
                        folderIconsToRefresh.add((FolderIconView) parent);
                    }
                }
                return false;
            }
        });
        for (int i = 0; i < folderIconsToRefresh.size(); i++) {
            FolderIconView folderIconView = (FolderIconView) folderIconsToRefresh.get(i);
            if (folderIconView != null) {
                folderIconView.refreshFolderIcon();
            }
        }
    }

    private void updateBadgeItems(ArrayList<ItemInfo> apps) {
        final HashSet<ItemInfo> updates = new HashSet(apps);
        mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((info instanceof IconInfo) && updates.contains(info)) {
                    View iconView = v;
                    if (HomeController.this.isItemInFolder(info)) {
                        iconView = HomeController.this.getHomescreenIconByItemId(info.container);
                    }
                    if (iconView instanceof IconView) {
                        ((IconView) iconView).refreshBadge();
                        HomeController.this.notifyCapture(true);
                    }
                    if (parent instanceof FolderIconView) {
                        ((FolderIconView) parent).refreshBadge();
                    }
                }
                return false;
            }
        });
    }

    void updateRestoreItems(final HashSet<ItemInfo> updates) {
        mapOverItems(true, new ItemOperator() {
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((info instanceof IconInfo) && (v instanceof IconView) && updates.contains(info)) {
                    ((IconView) v).applyState(false);
                } else if ((v instanceof PendingAppWidgetHostView) && (info instanceof LauncherAppWidgetInfo) && updates.contains(info)) {
                    ((PendingAppWidgetHostView) v).applyState();
                }
                return false;
            }
        });
    }

    void widgetsRestored(ArrayList<LauncherAppWidgetInfo> changedInfo) {
        if (!changedInfo.isEmpty()) {
            DeferredWidgetRefresh widgetRefresh = new DeferredWidgetRefresh(this, changedInfo, this.mAppWidgetHost);
            if (HomeLoader.getProviderInfo(this.mWorkspace.getContext(), ((LauncherAppWidgetInfo) changedInfo.get(0)).providerName, ((LauncherAppWidgetInfo) changedInfo.get(0)).user) != null) {
                widgetRefresh.run();
                return;
            }
            Iterator it = changedInfo.iterator();
            while (it.hasNext()) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) it.next();
                if (info.hostView instanceof PendingAppWidgetHostView) {
                    info.installProgress = 100;
                    ((PendingAppWidgetHostView) info.hostView).applyState();
                }
            }
        }
    }

    public boolean onBackPressed() {
        initBounceAnimation();
        if (this.mState.equal(4) || this.mState.equal(6)) {
            if (!this.mState.equal(4)) {
                enterNormalState(true);
            } else if (this.mZeroPageController == null || !this.mZeroPageController.isCurrentZeroPage()) {
                enterNormalState(true);
            } else {
                this.mZeroPageController.startZeroPage();
            }
        } else if (this.mState.equal(5)) {
            this.mLauncher.startHomeSettingActivity();
            exitScreenGridStateDelayed();
        } else {
            exitResizeState(true, "1");
        }
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setWaitingForResult(false);
        switch (requestCode) {
            case 5:
            case 9:
                requestCreateOrPickAppWidget(requestCode, resultCode, data);
                return;
            case 10:
                if (resultCode == -1 && isOverviewState()) {
                    enterNormalState(false);
                    return;
                }
                return;
            case 11:
                requestBindAppWidget(resultCode, data);
                return;
            case 12:
                requestReconfigureAppWidget(requestCode, resultCode, data);
                return;
            case GameHomeManager.REQUEST_GAMEHOME_ENABLED /*101*/:
                return;
            default:
                requestCreateShortcut(requestCode, resultCode, data);
                this.mLauncher.getDragLayer().clearAnimatedView();
                return;
        }
    }

    public void onStartForResult(int requestCode) {
        if (requestCode >= 0) {
            setWaitingForResult(true);
        }
    }

    public void deleteItemFromDb(ItemInfo info) {
        this.mFavoritesUpdater.deleteItem(info);
    }

    public void deleteItemFromDb(ItemInfo info, boolean restoreDeepShortCut) {
        this.mFavoritesUpdater.deleteItem(info, restoreDeepShortCut);
    }

    public void replaceFolderWithFinalItem(ItemInfo info, int itemCount, View folderIcon) {
        if (info instanceof FolderInfo) {
            FolderInfo folderInfo = (FolderInfo) info;
            CellLayout cellLayout = getCellLayout(folderInfo.container, folderInfo.screenId);
            View child = null;
            if (itemCount <= 1) {
                View child2;
                if (folderInfo.contents.isEmpty()) {
                    child2 = null;
                } else {
                    IconInfo finalItem = (IconInfo) folderInfo.contents.get(0);
                    if (this.mHomeBindController != null) {
                        child2 = this.mHomeBindController.createShortcut(cellLayout, finalItem);
                    } else {
                        child2 = null;
                    }
                    addOrMoveItemInDb(finalItem, folderInfo.container, folderInfo.screenId, folderInfo.cellX, folderInfo.cellY, folderInfo.rank);
                }
                deleteItemFromDb(folderInfo);
                if (cellLayout != null) {
                    if (cellLayout instanceof HotseatCellLayout) {
                        cellLayout.removeViewInLayout(folderIcon);
                    } else {
                        cellLayout.removeView(folderIcon);
                    }
                }
                if (folderIcon instanceof DropTarget) {
                    this.mDragMgr.removeDropTarget((DropTarget) folderIcon);
                }
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_DELETE_HOME_FOLDER, null, -1, false);
                child = child2;
            }
            if (child != null) {
                addInScreenFromBind(child, folderInfo.container, folderInfo.screenId, folderInfo.cellX, folderInfo.cellY, folderInfo.spanX, folderInfo.spanY);
            }
        }
    }

    public void deleteFolder(FolderInfo folderInfo) {
        removeHomeOrFolderItem(folderInfo, getViewForTag(folderInfo));
    }

    public void moveItemFromFolder(final IconInfo iconInfo) {
        ((FolderInfo) this.mLauncher.getLauncherModel().getHomeLoader().getItemById(iconInfo.container)).remove(iconInfo);
        int[] targetCell = new int[2];
        iconInfo.container = -100;
        int index = this.mWorkspace.getChildCount() - 1;
        while (index > 0) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(index);
            if (cl != null && cl.getCellLayoutChildren().getChildCount() > 0) {
                break;
            }
            index--;
        }
        iconInfo.screenId = findEmptyCell(targetCell, index, true);
        iconInfo.cellX = targetCell[0];
        iconInfo.cellY = targetCell[1];
        updateItemInDb(iconInfo);
        this.mHomeBindController.bindItem(iconInfo, false);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                HomeController.this.mLauncher.closeFolder();
                HomeController.this.mWorkspace.snapToPageImmediately(HomeController.this.mWorkspace.getPageIndexForScreenId(iconInfo.screenId));
            }
        }, 500);
    }

    public void addShortcutToHome(ItemInfo itemInfo) {
        int[] targetCell = new int[2];
        int index = this.mWorkspace.getChildCount() - 1;
        while (index > 0) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(index);
            if (cl != null && cl.getCellLayoutChildren().getChildCount() > 0) {
                break;
            }
            index--;
        }
        addItemOnHome(itemInfo, targetCell, findEmptyCell(targetCell, index, true));
    }

    void addItemOnHome(ItemInfo itemInfo, int[] targetCell, long screenId) {
        View view;
        int cellX = targetCell[0];
        int cellY = targetCell[1];
        CellLayout cl = (CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getPageIndexForScreenId(screenId));
        switch (itemInfo.itemType) {
            case 0:
            case 1:
                itemInfo = ((IconInfo) itemInfo).makeCloneInfo();
                view = this.mHomeBindController.createShortcut((IconInfo) itemInfo);
                break;
            case 2:
                itemInfo = ((FolderInfo) itemInfo).makeCloneInfo();
                view = FolderIconView.fromXml(this.mLauncher, cl, (FolderInfo) itemInfo, this, this.mLauncher, null, 0);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + itemInfo.itemType);
        }
        this.mWorkspace.snapToPageImmediately(this.mWorkspace.getPageIndexForScreenId(screenId));
        addInScreen(view, -100, screenId, cellX, cellY, 1, 1);
        cl.getCellLayoutChildren().measureChild(view);
        addOrMoveItemInDb(itemInfo, -100, screenId, cellX, cellY, -1);
        if (itemInfo instanceof FolderInfo) {
            addFolderItemsToDb(new ArrayList(((FolderInfo) itemInfo).contents), itemInfo.id);
            if (this.mFolderLock != null && ((FolderInfo) itemInfo).isLocked()) {
                this.mFolderLock.addLockedRecords((FolderInfo) itemInfo);
            }
        }
    }

    private void deleteItemInFolderFromDb(ItemInfo info) {
        this.mFavoritesUpdater.deleteFolderContentsFromDatabase((FolderInfo) info);
    }

    public void updateItemInDb(ItemInfo info) {
        ContentValues values = new ContentValues();
        info.onAddToDatabase(this.mLauncher, values);
        this.mFavoritesUpdater.updateItem(values, info);
    }

    public long addItemToDb(ItemInfo info, long container, long screenId, int cellX, int cellY) {
        return addItemToDb(info, container, screenId, cellX, cellY, false);
    }

    public long addItemToDb(ItemInfo info, long container, long screenId, int cellX, int cellY, boolean restoreDeepShortCut) {
        info.container = container;
        info.cellX = cellX;
        info.cellY = cellY;
        if (screenId >= 0 || container != -101) {
            info.screenId = screenId;
        } else {
            info.screenId = (long) this.mHotseat.getOrderInHotseat(cellX, cellY);
        }
        return this.mFavoritesUpdater.addItem(info, restoreDeepShortCut);
    }

    void addFolderItemsToDb(ArrayList<ItemInfo> infos, long container) {
        if (infos != null && !infos.isEmpty()) {
            Iterator it = infos.iterator();
            while (it.hasNext()) {
                ((ItemInfo) it.next()).container = container;
            }
            this.mFavoritesUpdater.addItems(infos);
        }
    }

    public void addItemToDb(ItemInfo info, long container, long screenId, int rank) {
    }

    private void modifyItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY) {
        modifyItemInDb(item, container, screenId, cellX, cellY, -1, -1, item.hidden);
    }

    void modifyItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int spanX, int spanY) {
        modifyItemInDb(item, container, screenId, cellX, cellY, spanX, spanY, item.hidden);
    }

    private void modifyItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int spanX, int spanY, int hidden) {
        if (!(item.cellY == cellY && item.cellX == cellX)) {
            if (item instanceof LauncherAppWidgetInfo) {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ITEM_ARRANGMENT, "Home_Widget_Longpress", -1, false);
            } else {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ITEM_ARRANGMENT, "Home_Icon_Longpress", -1, false);
            }
        }
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        if (spanX > -1 && spanY > -1) {
            item.spanX = spanX;
            item.spanY = spanY;
        }
        if (screenId >= 0 || container != -101) {
            item.screenId = screenId;
        } else {
            item.screenId = (long) this.mHotseat.getOrderInHotseat(cellX, cellY);
        }
        item.hidden = hidden;
        ContentValues values = new ContentValues();
        values.put("container", item.container);
        values.put("cellX", item.cellX);
        values.put("cellY", item.cellY);
        values.put(BaseLauncherColumns.RANK, item.rank);
        values.put("screen", item.screenId);
        values.put("hidden", item.hidden);
        if (spanX > -1 && spanY > -1) {
            values.put("spanX", item.spanX);
            values.put("spanY", item.spanY);
        }
        this.mFavoritesUpdater.updateItem(values, item);
    }

    public void modifyItemsInDb(ArrayList<ItemInfo> items, long container, int screen) {
        ArrayList<ContentValues> contentValues = new ArrayList();
        int count = items.size();
        for (int i = 0; i < count; i++) {
            ItemInfo item = (ItemInfo) items.get(i);
            item.container = container;
            if (screen >= 0 || container != -101) {
                item.screenId = (long) screen;
            } else {
                item.screenId = (long) this.mHotseat.getOrderInHotseat(item.cellX, item.cellY);
            }
            ContentValues values = new ContentValues();
            values.put("container", item.container);
            values.put("cellX", item.cellX);
            values.put("cellY", item.cellY);
            values.put(BaseLauncherColumns.RANK, item.rank);
            values.put("screen", item.screenId);
            contentValues.add(values);
        }
        this.mFavoritesUpdater.updateItemsInDatabaseHelper(this.mLauncher, contentValues, items);
    }

    void updateWorkspaceScreenOrder(ArrayList<Long> screenOrder) {
        this.mFavoritesUpdater.updateScreenOrder(this.mLauncher, screenOrder);
    }

    public void addOrMoveItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int rank) {
        addOrMoveItemInDb(item, container, screenId, cellX, cellY, rank, false);
    }

    public void addOrMoveItemInDb(ItemInfo item, long container, long screenId, int cellX, int cellY, int rank, boolean restoreDeepShortCut) {
        if (item.container == -1 || item.id == -1) {
            addItemToDb(item, container, screenId, cellX, cellY, restoreDeepShortCut);
        } else {
            modifyItemInDb(item, container, screenId, cellX, cellY);
        }
    }

    public void changeHomeScreenMode(String pref_key, final boolean startLoaderOnBackground) {
        if (FavoritesProvider.getInstance() != null && this.mLauncher != null) {
            final boolean HomeOnlySettingValue = this.mLauncher.getSharedPrefs().getBoolean(pref_key, false);
            if (HomeOnlySettingValue != LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                if (this.mZeroPageController != null) {
                    this.mZeroPageController.removeZeroPagePreview(true);
                }
                if (FavoritesProvider.getInstance().switchTable(1, HomeOnlySettingValue)) {
                    LauncherAppState.getInstance().writeHomeOnlyModeEnabled(HomeOnlySettingValue);
                    int[] cellXY = new int[2];
                    Utilities.loadCurrentGridSize(this.mLauncher, cellXY);
                    this.mLauncher.getDeviceProfile().setCurrentGrid(cellXY[0], cellXY[1]);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            LauncherAppState.getInstance().getModel().resetLoadedState(!HomeOnlySettingValue, true);
                            if (startLoaderOnBackground) {
                                LauncherAppState.getInstance().getModel().startLoader(-1001);
                            }
                            HomeController.this.mLauncher.recreateLauncher();
                        }
                    });
                }
            }
        }
    }

    public void setAppsButtonEnabled(boolean enabled) {
        IconInfo appsButton = null;
        if (enabled) {
            appsButton = Utilities.createAppsButton(this.mLauncher);
        }
        this.mLauncher.getLauncherModel().updateAppsButton(this.mLauncher, enabled, appsButton);
    }

    private void switchInternalStateChange(final int fromState, int toState) {
        if (fromState != toState) {
            if ((fromState == 1 || fromState == 5) && toState == 4) {
                if (this.mZeroPageController != null) {
                    this.mZeroPageController.createZeroPagePreview(fromState == 5);
                }
                this.mWorkspace.createCustomPlusPage();
                this.mWorkspace.setVisibilityOnCustomLayout(true, false);
                this.mWorkspace.showDefaultHomeIcon(true);
                this.mWorkspace.checkAlignButtonEnabled();
            }
            if (fromState == 4 && (toState == 1 || toState == 2 || toState == 5)) {
                if (toState != 1) {
                    removeCustomPage(toState);
                }
                Workspace workspace = this.mWorkspace;
                boolean z = this.mEnabledCustomLayoutAnimation && toState == 1;
                workspace.setVisibilityOnCustomLayout(false, false, z);
                workspace = this.mWorkspace;
                if (this.mEnabledCustomLayoutAnimation && toState == 1) {
                    z = true;
                } else {
                    z = false;
                }
                workspace.hideDefaultHomeIcon(z);
            }
            if (isStartedSwipeAffordanceAnim() && fromState == 1) {
                this.mSwipeAffordance.startCancelAnim(true);
            }
            if (toState == 1) {
                if (LauncherFeature.supportFestivalPage() && this.mFestivalPageController != null) {
                    this.mFestivalPageController.createCustomFestivalPage();
                }
                this.mWorkspace.setCrosshairsVisibilityChilds(8);
                this.mWorkspace.hideHintPages();
                this.mWorkspace.removeExtraEmptyScreenOnDrop(new Runnable() {
                    public void run() {
                        if (HomeController.this.mZeroPageController != null && fromState != 4 && HomeController.this.getState() != 6) {
                            HomeController.this.mWorkspace.updatePageIndicatorForZeroPage(true, false);
                        }
                    }
                });
                if (this.mDropTargetBar != null) {
                    this.mDropTargetBar.setDropTargetBarVisible(false);
                }
                LauncherAppState.getInstance().disableAndFlushExternalQueue();
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(1);
            } else if (LauncherFeature.supportFestivalPage() && this.mFestivalPageController != null) {
                this.mFestivalPageController.removeCustomFestivalPage();
            }
            if (toState == 2 || toState == 6 || toState == 5) {
                this.mWorkspace.setCrosshairsVisibilityChilds(0);
                this.mWorkspace.updatePageIndicatorForZeroPage(false, false);
                if (toState == 5) {
                    if (this.mScreenGridPanel != null) {
                        this.mScreenGridPanel.updateButtonStatus();
                        if (this.mScreenGridPanel.getScreenGridTopConatiner() != null) {
                            this.mScreenGridPanel.getScreenGridTopConatiner().setVisibility(View.VISIBLE);
                        }
                    }
                } else if (toState == 2) {
                    if (this.mDragMgr.isDragging() && this.mDragMgr.getDragObject() != null) {
                        DragSource dragSource = this.mDragMgr.getDragObject().dragSource;
                        if ((dragSource instanceof QuickOptionView) || (dragSource instanceof PinItemDragListener)) {
                            this.mDropTargetBar.setDropTargetBarVisible(true);
                            showCancelDropTarget();
                        }
                    }
                    this.mWorkspace.addExtraEmptyScreenOnDrag();
                    this.mWorkspace.showHintPages();
                } else if (toState == 6) {
                    if (fromState == 2) {
                        this.mWorkspace.removeExtraEmptyScreenOnDrop(null);
                    }
                    this.mWorkspace.showHintPages();
                }
            }
            if (toState == 4) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(5);
                this.mWorkspace.clearFocus();
                this.mWorkspace.setCrosshairsVisibilityChilds(8);
                this.mHomeDefaultIconClick = false;
                this.mHomePageReorder = false;
            } else if (toState == 5) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(17);
                changePaddingforScreenGrid();
            } else if (fromState == 5) {
                changePaddingforScreenGrid();
            } else if (fromState == 4) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(1);
                if (this.mHomeDefaultIconClick) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_DEFAULT_ICON_CLICK, null, -1, false);
                }
                if (this.mHomePageReorder) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_PAGE_REORDER, null, -1, false);
                }
            }
        }
    }

    void removeCustomPage(int toState) {
        if (this.mZeroPageController != null) {
            ZeroPageController zeroPageController = this.mZeroPageController;
            boolean z = toState == 2 || toState == 5;
            zeroPageController.removeZeroPagePreview(z);
            if (toState == 1) {
                this.mWorkspace.updatePageIndicatorForZeroPage(true, false);
                startEdgeLight();
            }
        }
        this.mWorkspace.removeCustomPlusPage();
    }

    public TrayLevel getTrayLevel() {
        return TrayLevel.Overground;
    }

    public void startTrayMove() {
        // TODO: Samsung specific code
//        if (Talk.INSTANCE.isAccessibilityEnabled()) {
//            this.mHomeContainer.semClearAccessibilityFocus();
//        }
    }

    public float getTrayScale() {
        return this.mHomeContainer.getScaleY();
    }

    public DropTarget getDropTarget() {
        return this.mWorkspace.getDragController();
    }

    public void onReceiveTrayEvent(TrayEvent event) {
        int finalStageMode = 2;
        switch (event.mEventType) {
            case 1:
                float toTranslationY = event.mValue;
                this.mHomeContainer.setTranslationY(toTranslationY);
                if (!(!isStartedSwipeAffordanceAnim() || this.mIsStartedTrayEventSetY || toTranslationY == 0.0f)) {
                    this.mIsStartedTrayEventSetY = true;
                    this.mSwipeAffordance.startCancelAnim(false);
                }
                stopEdgeLight();
                return;
            case 2:
                float borderY = event.mValue + ((float) this.mHomeContainer.getHeight());
                float bottomOfHome = this.mHomeContainer.getTranslationY() + ((float) this.mHomeContainer.getHeight());
                if (borderY < bottomOfHome) {
                    updateHotseatByMoveToAppsPosition(bottomOfHome - borderY);
                    return;
                } else if (this.mHotseat.getTranslationY() != 0.0f) {
                    updateHotseatByMoveToAppsPosition(0.0f);
                    return;
                } else {
                    return;
                }
            case 3:
                this.mIsStartedTrayEventSetY = false;
                if (((int) event.mValue) <= 0) {
                    Animator returnAnimator = this.mHomeAnimation.getTrayReturnAnimation(true, this.mLauncher.isHomeStage());
                    if (this.mLauncher.isHomeStage()) {
                        finalStageMode = 1;
                    }
                    if (returnAnimator != null) {
                        returnAnimator.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                if (HomeController.this.mTrayManager != null) {
                                    // TODO: Samsung specific code
                                    // HomeController.this.mTrayManager.trayMoveEnd(finalStageMode);
                                }
                            }
                        });
                        returnAnimator.start();
                        return;
                    } else if (this.mTrayManager != null) {
                        this.mTrayManager.trayMoveEnd(finalStageMode);
                        return;
                    } else {
                        return;
                    }
                } else if (this.mLauncher.isHomeStage()) {
                    this.mLauncher.getStageManager().startStageByTray(2);
                    return;
                } else {
                    this.mLauncher.getStageManager().startStageByTray(1);
                    return;
                }
            case 4:
                if (this.mLauncher.isAppsStage()) {
                    onStagePreEnter();
                }
                if (this.mHomeContainer.getVisibility() != View.VISIBLE) {
                    this.mHomeContainer.setVisibility(View.VISIBLE);
                    this.mHomeContainer.setAlpha(0.0f);
                }
                this.mWorkspace.updateOnlyCurrentPage(true);
                this.mWorkspace.updateChildrenLayersEnabled(true);
                return;
            case 5:
                if (((int) event.mValue) == 2) {
                    this.mHomeContainer.setVisibility(View.GONE);
                }
                this.mWorkspace.updateOnlyCurrentPage(false);
                this.mWorkspace.updateChildrenLayersEnabled(false);
                this.mLauncher.getLauncherModel().flushPendingQueue();
                return;
            case 10:
                boolean fromHomeToApps;
                if (event.mValue > 0.0f) {
                    fromHomeToApps = true;
                } else {
                    fromHomeToApps = false;
                }
                StageEntry data;
                if (fromHomeToApps) {
                    enterNormalState(false, true);
                    this.mWorkspace.removeExtraEmptyScreen();
                    data = new StageEntry();
                    data.enableAnimation = true;
                    data.setInternalStateTo(1);
                    data.putExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, 1);
                    getStageManager().startStage(2, data);
                    return;
                }
                data = new StageEntry();
                data.enableAnimation = true;
                if (this.mDragMgr.isDragging()) {
                    data.setInternalStateTo(2);
                } else {
                    data.setInternalStateTo(1);
                }
                data.putExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, 1);
                getStageManager().finishAllStage(data);
                return;
            default:
                return;
        }
    }

    public boolean determineStageChange(int velocity, float offset, float firstDownY, float lastDownY, int minSnapVelocity) {
        boolean toBeChanged = false;
        if (this.mLauncher.isHomeStage()) {
            int range;
            boolean swipeDown;
            if (this.mTrayManager != null) {
                range = this.mTrayManager.getTrayMovingRange();
            } else {
                range = Utilities.getFullScreenHeight(this.mLauncher);
            }
            if (offset > 0.0f) {
                swipeDown = true;
            } else {
                swipeDown = false;
            }
            if (offset == 0.0f) {
                toBeChanged = false;
            } else if (swipeDown) {
                toBeChanged = (firstDownY < lastDownY && velocity > 0 && Math.abs(velocity) >= minSnapVelocity) || (Math.abs(velocity) < minSnapVelocity && offset >= ((float) range) * this.mPageSnapMovingRatioOnHome);
            } else {
                toBeChanged = (firstDownY > lastDownY && velocity < 0 && Math.abs(velocity) >= minSnapVelocity) || (Math.abs(velocity) < minSnapVelocity && (-offset) >= ((float) range) * this.mPageSnapMovingRatioOnHome);
            }
            if (toBeChanged) {
                SALogging.getInstance().insertEventLog(this.mLauncher.getResources().getString(R.string.screen_Home_1xxx), this.mLauncher.getResources().getString(R.string.event_Apps), swipeDown ? "2" : "1");
            }
        }
        return toBeChanged;
    }

    boolean canMoveHometray() {
        if (!this.mLauncher.isHomeStage() || !this.mState.equal(1) || this.mHomeAnimation.isRunningOverviewAnimation() || this.mWorkspace.hasTargetView()) {
            return false;
        }
        return true;
    }

    private void updateHotseatByMoveToAppsPosition(float overlayedRange) {
        if (this.mTrayManager != null) {
            if (this.mMoveToAppsPanelHeight == 0) {
                this.mMoveToAppsPanelHeight = this.mTrayManager.getHeightOfTrayForDrag();
            }
            this.mHotseat.setTranslationY(-(((float) this.mHotseatMoveRange) * (overlayedRange / ((float) this.mTrayManager.getHeightOfTrayForDrag()))));
        }
    }

    HomeContainer getHomeContainer() {
        return this.mHomeContainer;
    }

    public boolean hasStartedSFinder() {
        return this.mHomeContainer.hasStartedSFinder();
    }

    public void resetStartedSFinder() {
        this.mHomeContainer.resetStartedSFinder();
    }

    public void onConfigurationChangedIfNeeded() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (isStartedSwipeAffordanceAnim() && this.mLauncher.isHomeStage()) {
            this.mSwipeAffordance.startCancelAnim(true);
        }
        if (this.mLauncher.isHomeStage()) {
            this.mDragMgr.setScrollZone();
        }
        if (getState() == 3) {
            exitResizeState(false);
        } else if (getState() == 5) {
            int[] storedGrid = new int[2];
            ScreenGridUtilities.loadCurrentGridSize(this.mLauncher, storedGrid, LauncherAppState.getInstance().isHomeOnlyModeEnabled());
            int[] selectedGrid = this.mScreenGridPanel.getSelectedGrid();
            if (!Arrays.equals(storedGrid, selectedGrid)) {
                Log.d(TAG, "Restore selected home grid option onConfigurationChanged");
                dp.setCurrentGrid(selectedGrid[0], selectedGrid[1]);
            }
        }
        if (this.mDragMgr.isQuickOptionShowing()) {
            this.mDragMgr.removeQuickOptionView();
        }
        this.mWorkspace.onConfigurationChangedIfNeeded();
        this.mHotseat.onConfigurationChangedIfNeeded();
        this.mScreenGridPanel.onConfigurationChangedIfNeeded();
        this.mOverviewPanel.onConfigurationChangedIfNeeded();
        this.mHomeAnimation.onConfigurationChangedIfNeeded();
        this.mDropTargetBar.onConfigurationChangedIfNeeded();
        if (this.mSwipeAffordance != null) {
            this.mSwipeAffordance.onConfigurationChangedIfNeeded();
        }
        notifyCapture(true);
        setDisableNotificationHelpTip();
    }

    public void onChangeColorForBg(boolean whiteBg) {
        this.mHotseat.changeColorForBg(whiteBg);
        this.mWorkspace.changeColorForBg(whiteBg);
        this.mOverviewPanel.changeColorForBg(whiteBg);
        this.mDropTargetBar.changeColorForBg(whiteBg);
        if (this.mSwipeAffordance != null) {
            this.mSwipeAffordance.changeColorForBg(whiteBg);
        }
        if (this.mZeroPageController != null) {
            this.mZeroPageController.changeColorForBg(whiteBg);
        }
        if (LauncherFeature.supportFlexibleGrid()) {
            this.mScreenGridPanel.changeColorForBg(whiteBg);
        }
    }

    public void onRefreshLiveIcon() {
        Iterator it = this.mWorkspace.getWorkspaceScreens().iterator();
        while (it.hasNext()) {
            updateLiveIcon((CellLayout) it.next());
        }
        updateLiveIcon(this.mHotseat.getLayout());
    }

    public void onBadgeBindingCompleted(ArrayList<ItemInfo> badgeItems) {
        if (!badgeItems.isEmpty()) {
            updateBadgeItems(badgeItems);
        }
        updateNotificationHelp(false);
    }

    private void updateLiveIcon(CellLayout cell) {
        if (cell != null) {
            CellLayoutChildren cl = cell.getCellLayoutChildren();
            if (cl != null) {
                int itemCount = cl.getChildCount();
                for (int i = 0; i < itemCount; i++) {
                    View view = cl.getChildAt(i);
                    if (view.getTag() instanceof ItemInfo) {
                        ItemInfo item = (ItemInfo) view.getTag();
                        if (item instanceof IconInfo) {
                            Intent intent = ((IconInfo) item).intent;
                            if (intent != null) {
                                if (LiveIconManager.isKnoxLiveIcon(intent)) {
                                    ((IconView) view).applyFromShortcutInfo((IconInfo) item, LauncherAppState.getInstance().getIconCache());
                                    view.invalidate();
                                } else if (((IconInfo) item).intent.getComponent() != null && LiveIconManager.isLiveIconPackage(((IconInfo) item).intent.getComponent().getPackageName())) {
                                    ((IconView) view).applyFromShortcutInfo((IconInfo) item, LauncherAppState.getInstance().getIconCache());
                                    view.invalidate();
                                }
                            }
                        } else if (item instanceof FolderInfo) {
                            boolean needToRefreshFolderIcon = false;
                            FolderIconView folderIconView = (FolderIconView) cl.getChildAt(item);
                            for (int j = 0; j < folderIconView.getFolderInfo().contents.size(); j++) {
                                IconInfo insideItem = (IconInfo) folderIconView.getFolderInfo().contents.get(j);
                                if (!(insideItem == null || insideItem.getIntent().getComponent() == null)) {
                                    String packageName = insideItem.getIntent().getComponent().getPackageName();
                                    if (packageName != null && insideItem.rank < 9 && (LiveIconManager.isCalendarPackage(packageName) || IconView.isKnoxShortcut(insideItem.intent))) {
                                        needToRefreshFolderIcon = true;
                                        break;
                                    }
                                }
                            }
                            if (needToRefreshFolderIcon) {
                                folderIconView.refreshFolderIcon();
                            }
                        }
                    }
                }
            }
        }
    }

    boolean autoAlignItems(boolean isUpward, boolean checkToAlign) {
        return autoAlignItems(isUpward, checkToAlign, this.mWorkspace.getCurrentPage());
    }

    boolean autoAlignItems(boolean isUpward, boolean checkToAlign, int pageIndex) {
        if (checkToAlign || !this.mWorkspace.isPageMoving()) {
            WorkspaceCellLayout currentPage = (WorkspaceCellLayout) this.mWorkspace.getChildAt(pageIndex);
            if (currentPage != null) {
                boolean isAligned = AutoAlignHelper.autoAlignItems(currentPage, isUpward, checkToAlign);
                if (checkToAlign) {
                    return isAligned;
                }
                if (isAligned) {
                    updateItemLocationsInDatabase(currentPage);
                    currentPage.setEnabledOnAlignButton(isUpward);
                    if (this.mWorkspace.getCurrentPage() == this.mWorkspace.getDefaultPage()) {
                        notifyCapture(false);
                    }
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_AUTO_ALIGN, null, -1, false);
                }
            }
        }
        return false;
    }

    boolean isReorderAnimating() {
        CellLayout currentPage = (CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage());
        if (currentPage != null) {
            return currentPage.isReorderAnimating();
        }
        return false;
    }

    public void onChangeSelectMode(boolean enter, boolean animated) {
        if (!this.mLauncher.isHomeStage()) {
            return;
        }
        if (enter) {
            enterSelectState(animated);
            Talk.INSTANCE.postSay(this.mLauncher.getResources().getString(R.string.tts_changed_to_home_screen_edit_mode) + " " + String.format(this.mLauncher.getResources().getString(R.string.default_scroll_format), this.mWorkspace.getCurrentPage() + 1, this.mWorkspace.getPageCount()));
            return;
        }
        this.mMultiSelectManager.clearCheckedApps();
    }

    private void updateCheckBox(boolean visible) {
        this.mWorkspace.updateCheckBox(visible);
        this.mHotseat.updateCheckBox(visible);
    }

    public void onCheckedChanged(View view, boolean isChecked) {
        boolean isHotseat = ((ItemInfo) view.getTag()).container == -101;
        if (isChecked) {
            this.mMultiSelectManager.addCheckedApp(view, isHotseat ? this.mHotseat.getDragController() : this.mWorkspace.getDragController());
        } else {
            this.mMultiSelectManager.removeCheckedApp(view);
        }
    }

    public void onClickMultiSelectPanel(int id) {
        if (this.mLauncher.isHomeStage() || (this.mLauncher.isFolderStage() && this.mLauncher.getSecondTopStageMode() == 1)) {
            switch (id) {
                case 1:
                    removeShortcut();
                    break;
                case 2:
                    createFolder();
                    break;
            }
            enterNormalState(true, true);
        }
    }

    private void removeShortcut() {
        final ArrayList<View> appsViewList = new ArrayList(this.mMultiSelectManager.getCheckedAppsViewList());
        if (appsViewList.size() > 0) {
            removeShortcutAnimation(appsViewList, new Runnable() {
                public void run() {
                    Iterator it = appsViewList.iterator();
                    while (it.hasNext()) {
                        View checkedApp = (View) it.next();
                        HomeController.this.removeHomeOrFolderItem((ItemInfo) checkedApp.getTag(), checkedApp);
                    }
                }
            });
            if (appsViewList.size() > 1) {
                this.mMultiSelectManager.showToast(1);
            }
        }
    }

    private void removeShortcutAnimation(ArrayList<View> appsViewList, final Runnable r) {
        AnimatorSet animatorSet = new AnimatorSet();
        Iterator it = appsViewList.iterator();
        while (it.hasNext()) {
            View v = (View) it.next();
            Animator scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 0.5f), PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 0.5f));
            scaleAnimator.setInterpolator(ViInterpolator.getInterploator(34));
            animatorSet.play(scaleAnimator);
            Animator alphaAnimator = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.0f));
            alphaAnimator.setInterpolator(ViInterpolator.getInterploator(30));
            animatorSet.play(alphaAnimator);
        }
        animatorSet.setDuration(200);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                new Handler().post(r);
            }
        });
        animatorSet.start();
    }

    private void createFolder() {
        ArrayList<View> appsViewList = this.mMultiSelectManager.getCheckedAppsViewList();
        if (appsViewList != null && appsViewList.size() > 0) {
            View targetView = getTargetView(appsViewList);
            if (targetView == null) {
                Log.e(TAG, "onClickCreateFolder : targetView is null");
                return;
            }
            IconInfo targetItem = (IconInfo) targetView.getTag();
            CellLayout targetCellLayout = (CellLayout) targetView.getParent().getParent();
            if (targetItem != null) {
                int toPage;
                final FolderIconView folder;
                int delayToOpenFolder = this.mLauncher.isFolderStage() ? this.mLauncher.getResources().getInteger(R.integer.config_folderCloseDuration) : 500;
                boolean isHotseat = targetItem.container == -101;
                if (isHotseat) {
                    toPage = this.mWorkspace.getNextPage();
                } else {
                    toPage = this.mWorkspace.getPageIndexForScreenId(targetItem.screenId);
                }
                if (isHotseat) {
                    this.mHotseat.setTargetView(targetView);
                } else {
                    removeHomeItem(targetItem);
                    this.mWorkspace.setTargetView(targetView);
                }
                removeCheckedAppView(appsViewList);
                if (isHotseat) {
                    folder = this.mHotseat.getDragController().addFolder(targetCellLayout, targetItem);
                } else {
                    folder = this.mWorkspace.getDragController().addFolder(targetCellLayout, targetItem);
                }
                folder.setVisibility(View.INVISIBLE);
                ArrayList<IconInfo> items = new ArrayList();
                Iterator it = appsViewList.iterator();
                while (it.hasNext()) {
                    items.add((IconInfo) ((View) it.next()).getTag());
                }
                folder.addItems(items);
                if (toPage != this.mWorkspace.getNextPage()) {
                    this.mWorkspace.snapToPage(toPage);
                }
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (HomeController.this.mLauncher.isHomeStage()) {
                            HomeController.this.mWorkspace.setTargetView(null);
                            HomeController.this.mHotseat.setTargetView(null);
                            HomeController.this.mHotseat.getDragController().removeEmptyCells(false, true);
                            StageEntry data = new StageEntry();
                            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folder);
                            HomeController.this.getStageManager().startStage(5, data);
                        }
                    }
                }, (long) delayToOpenFolder);
                return;
            }
            Log.e(TAG, "onClickCreateFolder : app info is null");
        }
    }

    private View getTargetView(ArrayList<View> appsViewList) {
        boolean isItemInFolder = true;
        View targetView = null;
        ItemInfo targetItem = null;
        for (int i = appsViewList.size() - 1; i >= 0; i--) {
            targetView = (View) appsViewList.get(i);
            targetItem = (ItemInfo) targetView.getTag();
            if (!isItemInFolder(targetItem)) {
                isItemInFolder = false;
                break;
            }
        }
        if (isItemInFolder) {
            removeHomeItem(targetItem);
            int[] cellXY = new int[2];
            long screenId = findEmptyCell(cellXY);
            CellLayout cl = this.mWorkspace.getScreenWithId(screenId);
            targetItem.container = -100;
            targetView = this.mHomeBindController.createShortcut(cl, (IconInfo) targetItem);
            if (targetView != null) {
                ItemInfo item = (ItemInfo) targetView.getTag();
                item.cellX = cellXY[0];
                item.cellY = cellXY[1];
                item.screenId = screenId;
                addInScreen(targetView, -100, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
            }
        }
        return targetView;
    }

    private long findEmptyCell(int[] targetCell) {
        int index = this.mWorkspace.getChildCount() - 1;
        while (index > 0) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(index);
            if (cl != null && cl.getCellLayoutChildren().getChildCount() > 0) {
                break;
            }
            index--;
        }
        return findEmptyCell(targetCell, index, true);
    }

    private long findEmptyCell(int[] targetCell, int targetScreen, boolean lastPosition) {
        for (int i = targetScreen; i < this.mWorkspace.getPageCount(); i++) {
            CellLayout cl = (CellLayout) this.mWorkspace.getPageAt(i);
            if (cl != null) {
                int countX = cl.getCountX();
                int countY = cl.getCountY();
                boolean[][] occupied = cl.getOccupied();
                long screenId = this.mWorkspace.getScreenIdForPageIndex(i);
                if (screenId == -201) {
                    screenId = this.mWorkspace.commitExtraEmptyScreen();
                }
                if (screenId == -1) {
                    continue;
                } else if (lastPosition) {
                    if (this.mHomeLoader.getItemPositionHelper().findVacantCell(targetCell, 1, 1, countX, countY, occupied, true)) {
                        return screenId;
                    }
                } else if (Utilities.findVacantCellToRightBottom(targetCell, 1, 1, countX, countY, occupied, 0, 0)) {
                    return screenId;
                } else {
                    if (Utilities.findVacantCellToLeftTop(targetCell, 1, 1, countX, countY, occupied, 0, 0)) {
                        return screenId;
                    }
                }
            }
        }
        if (!this.mWorkspace.hasExtraEmptyScreen()) {
            this.mWorkspace.addExtraEmptyScreen();
        }
        targetCell[0] = 0;
        targetCell[1] = 0;
        return this.mWorkspace.commitExtraEmptyScreen();
    }

    private void removeCheckedAppView(ArrayList<View> appsViewList) {
        Iterator it = new ArrayList(appsViewList).iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) ((View) it.next()).getTag();
            if (item != null) {
                removeHomeItem(item);
            }
        }
    }

    boolean isChangeGridState() {
        return this.mScreenGridHelper.isChangeGridState();
    }

    void changeGrid(int cellX, int cellY, boolean animated) {
        this.mScreenGridHelper.changeGrid(cellX, cellY, animated);
    }

    private void backupOriginalData() {
        this.mScreenGridHelper.backupOriginalData();
    }

    private void updateGridSize() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int[] xy = new int[2];
        Utilities.loadCurrentGridSize(LauncherAppState.getInstance().getContext(), xy);
        int column = xy[0];
        int row = xy[1];
        if (column != dp.homeGrid.getCellCountX() || row != dp.homeGrid.getCellCountY()) {
            changeGrid(column, row, false);
        }
    }

    public void cancelGridChange() {
        this.mScreenGridHelper.restoreGridLayout();
    }

    void exitScreenGridStateDelayed() {
        exitScreenGridStateDelayed(200);
    }

    private void exitScreenGridStateDelayed(int delay) {
        if (this.mState.equal(5)) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    HomeController.this.cancelGridChange();
                    HomeController.this.enterOverviewState(false);
                    HomeController.this.mWorkspace.updateDefaultHomePageBg((WorkspaceCellLayout) HomeController.this.mWorkspace.getChildAt(HomeController.this.mWorkspace.getDefaultPage()));
                    if (HomeController.this.getStageManager().getStackSize() > 1 && !GlobalSettingUtils.getStartSetting()) {
                        HomeController.this.enterNormalState(false);
                        StageEntry data = new StageEntry();
                        data.enableAnimation = false;
                        HomeController.this.getStageManager().finishStage(1, data);
                    }
                    HomeController.this.changePaddingforScreenGrid();
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(6);
                }
            }, (long) delay);
        }
    }

    void applyGridChangeFinally() {
        int delay = this.mLauncher.getResources().getInteger(R.integer.config_delay_AppsGridChanged);
        this.mScreenGridHelper.applyGridChange(delay);
        updateWorkspaceScreenOrder(this.mWorkspace.getScreenOrder());
        this.mHotseat.changeGrid(false, true);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                HomeController.this.mLauncher.getStageManager().onChangeGrid();
            }
        }, (long) delay);
        notifyCapture(false);
        updateNotificationHelp(true);
    }

    void changeDialerApp() {
        int currentSetDialer = this.mLauncher.getSharedPrefs().getInt(KEY_PREF_CURRENT_SET_DIALER, -1);
        int currentSystemSetDialer = System.getInt(this.mLauncher.getContentResolver(), "skt_phone20_settings", 0);
        if (currentSetDialer != currentSystemSetDialer) {
            ComponentName addToHotseatCN;
            ComponentName addToWorkspaceCN;
            Iterator it;
            ItemInfo item;
            IconInfo iconInfo;
            Editor editor = this.mLauncher.getSharedPrefs().edit();
            editor.putInt(KEY_PREF_CURRENT_SET_DIALER, currentSystemSetDialer);
            editor.apply();
            boolean findHotseatItem = false;
            Context context = this.mLauncher.getApplicationContext();
            ComponentName customerDialer = new ComponentName(LauncherFeature.getCustomerDialerPackageName(), LauncherFeature.getCustomerDialerClassName());
            ComponentName componentName = new ComponentName(LauncherFeature.getOemDialerPackageName(context), LauncherFeature.getOemDialerClassName());
            if (currentSystemSetDialer == 1) {
                Log.d(TAG, "OEM -> T phone app");
                addToHotseatCN = customerDialer;
                addToWorkspaceCN = componentName;
            } else {
                Log.d(TAG, "T -> OEM phone app");
                addToHotseatCN = componentName;
                addToWorkspaceCN = customerDialer;
            }
            ArrayList<ItemInfo> hotseatItems = this.mHotseat.getLayout().getCellLayoutChildren().getChildrenAllItems();
            boolean isHomeOnly = LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled();
            if (isHomeOnly) {
                boolean existOemDialer = false;
                boolean existTPhoneDialer = false;
                it = hotseatItems.iterator();
                while (it.hasNext()) {
                    item = (ItemInfo) it.next();
                    if (item instanceof IconInfo) {
                        iconInfo = (IconInfo) item;
                        if (!(iconInfo.getIntent() == null || iconInfo.getIntent().getComponent() == null)) {
                            String className = iconInfo.getIntent().getComponent().flattenToShortString();
                            if (className.equals(customerDialer.flattenToShortString())) {
                                existTPhoneDialer = true;
                            } else if (className.equals(componentName.flattenToShortString())) {
                                existOemDialer = true;
                            }
                        }
                    }
                }
                if (existTPhoneDialer && existOemDialer) {
                    Log.d(TAG, "changeDialerApp() - both items are exist in hotseat.");
                    return;
                }
            }
            it = hotseatItems.iterator();
            while (it.hasNext()) {
                item = (ItemInfo) it.next();
                if (item instanceof IconInfo) {
                    iconInfo = (IconInfo) item;
                    if (!(iconInfo.getIntent() == null || iconInfo.getIntent().getComponent() == null || !addToWorkspaceCN.flattenToShortString().equals(iconInfo.getIntent().getComponent().flattenToShortString()))) {
                        changeItemInfo(iconInfo, addToHotseatCN);
                        findHotseatItem = true;
                        break;
                    }
                }
            }
            if (findHotseatItem) {
                boolean checkOnlyCustomerPage = !LauncherAppState.getInstance().isHomeOnlyModeEnabled();
                int screenCount = this.mWorkspace.getChildCount();
                long customerPageId = Utilities.getCustomerPageKey(context);
                int customerPageIndex = this.mWorkspace.getPageIndexForScreenId(customerPageId);
                if (!checkOnlyCustomerPage || customerPageIndex >= 0) {
                    int screen = 0;
                    while (screen < screenCount) {
                        CellLayoutChildren cellLayoutChildren = ((CellLayout) this.mWorkspace.getChildAt(screen)).getCellLayoutChildren();
                        if (!checkOnlyCustomerPage || screen == customerPageIndex) {
                            it = cellLayoutChildren.getChildrenAllItems().iterator();
                            while (it.hasNext()) {
                                item = (ItemInfo) it.next();
                                if (item instanceof IconInfo) {
                                    iconInfo = (IconInfo) item;
                                    if (!(iconInfo.getIntent() == null || iconInfo.getIntent().getComponent() == null || !addToHotseatCN.flattenToShortString().equals(iconInfo.getIntent().getComponent().flattenToShortString()))) {
                                        changeItemInfo(iconInfo, addToWorkspaceCN);
                                        return;
                                    }
                                }
                            }
                            continue;
                        }
                        screen++;
                    }
                    return;
                }
                Log.d(TAG, "customer page is not exist. id " + customerPageId + " index " + customerPageIndex);
                return;
            }
            Log.d(TAG, addToWorkspaceCN.flattenToShortString() + " are not in the hotseat");
        }
    }

    private void changeItemInfo(IconInfo item, ComponentName changeCN) {
        View view = getViewForTag(item);
        if (view instanceof IconView) {
            Log.d(TAG, "changeItemInfo item " + item + " changeCN " + changeCN);
            item.intent.setComponent(changeCN);
            item.componentName = changeCN;
            item.mBadgeCount = this.mHomeLoader.getBadgeCount(changeCN, item.user);
            item.updateIcon(LauncherAppState.getInstance().getIconCache());
            ((IconView) view).applyFromShortcutInfo(item, LauncherAppState.getInstance().getIconCache());
            ContentValues values = new ContentValues();
            values.put("intent", item.intent.toUri(0));
            values.put("title", item.title.toString());
            this.mFavoritesUpdater.updateItem(values, item);
        }
    }

    public void onUpdateAlphabetList(ItemInfo item) {
    }

    public ItemInfo getLocationInfoFromDB(ItemInfo item) {
        return null;
    }

    public ZeroPageController getZeroPageController() {
        return this.mZeroPageController;
    }

    public void notifyControllerItemsChanged() {
        notifyCapture(getStageManager().getTopStage() == this);
    }

    public void finishAllStage() {
        if (getStageManager().getStackSize() > 1) {
            StageEntry data = new StageEntry();
            data.enableAnimation = false;
            getStageManager().finishAllStage(data);
        }
    }

    public boolean recoverCancelItemForFolderLock(IconInfo info, long container, long screenId, int cellX, int cellY, int rank) {
        if (info.container == -102) {
            Log.d(FolderLock.TAG, "drag a item from Apps to Home no need recover");
            return false;
        }
        int[] cellXY = new int[2];
        CellLayout layout = getCellLayout(container, screenId);
        View view = this.mHomeBindController.createShortcut(info);
        boolean foundCellSpan = false;
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;
            if (!(info.container == -101 || info.container == -100)) {
                DragObject dragObject;
                if (layout instanceof HotseatCellLayout) {
                    Log.d(FolderLock.TAG, "drag from Hotseat  ");
                    dragObject = new DragObject();
                    dragObject.dragInfo = info;
                    if (this.mHotseat.getDragController().createUserFolderIfNecessary(cellXY, view, dragObject)) {
                        return true;
                    }
                    if (this.mHotseat.getDragController().addToExistingFolderIfNecessary(cellXY, dragObject)) {
                        return true;
                    }
                }
                dragObject = new DragObject();
                dragObject.dragInfo = info;
                FolderLock folderLock = FolderLock.getInstance();
                folderLock.setReorderLayout(layout);
                if (this.mWorkspace.getDragController().createUserFolderIfNecessary(cellXY, view, dragObject)) {
                    folderLock.setReorderLayout(null);
                    return true;
                } else if (this.mWorkspace.getDragController().addToExistingFolderIfNecessary(cellXY, dragObject)) {
                    folderLock.setReorderLayout(null);
                    return true;
                } else {
                    folderLock.setReorderLayout(null);
                }
            }
        }
        if (foundCellSpan) {
            if (!this.mRestoring) {
                if (layout instanceof HotseatCellLayout) {
                    this.mHotseat.getDragController().makeEmptyCell(cellXY[0], true, true);
                    addInScreen(view, container, screenId, cellXY[0], 0, 1, 1, true, true);
                    DragView dragView = this.mDragMgr.createDragView(view, cellXY[0], 0);
                    this.mLauncher.getDragLayer().animateViewIntoPosition(dragView, view, null, this.mHotseat);
                } else {
                    addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1);
                }
            }
            return true;
        }
        showOutOfSpaceMessage();
        return false;
    }

    public void moveOutItemsFromLockedFolder(FolderInfo folder, ArrayList<IconInfo> homeNeedUpdateInfos, ArrayList<IconInfo> arrayList) {
        if (!folder.isContainApps()) {
            HomeBindController homeBindController = this.mHomeBindController;
            HomeItemPositionHelper homeItemPositionHelper = this.mHomeLoader.getItemPositionHelper();
            Log.d(TAG, "the folder title of moveoutHomeOrAppsItemsFromLockedFolder is  :  " + folder.title);
            for (int index = 0; index < homeNeedUpdateInfos.size(); index++) {
                ItemInfo info = (IconInfo) homeNeedUpdateInfos.get(index);
                folder.remove((IconInfo) info);
                int[] xy = new int[2];
                long screenId = folder.screenId;
                boolean found = homeItemPositionHelper.findNearEmptyCell(xy, screenId, folder.cellX, folder.cellY);
                Log.d(TAG, " moveout found pos is   :  " + found + " / " + xy[0] + " / " + xy[1]);
                if (!found) {
                    int screenCount = this.mHomeLoader.getWorkspaceScreenCount();
                    for (int i = 0; i < screenCount && !found; i++) {
                        screenId = this.mHomeLoader.getWorkspaceScreenId(i);
                        if (((long) i) != folder.screenId) {
                            found = homeItemPositionHelper.findEmptyCell(xy, (long) i, 1, 1);
                        }
                    }
                    if (!found) {
                        screenId = this.mHomeLoader.insertWorkspaceScreen(this.mLauncher, screenCount, -1);
                    }
                }
                View v = homeBindController.createShortcut((IconInfo)info);
                if (folder.contents.size() < 1) {
                    info.cellX = folder.cellX;
                    info.cellY = folder.cellY;
                    info.screenId = folder.screenId;
                    removeHomeItem((ItemInfo) folder);
                } else {
                    info.cellX = xy[0];
                    info.cellY = xy[1];
                    info.screenId = screenId;
                }
                info.container = -100;
                addInScreen(v, info.container, info.screenId, info.cellX, info.cellY, 1, 1);
                if (info.id == -1) {
                    info.id = FavoritesProvider.getInstance().generateNewItemId();
                    addItemToDb(info, info.container, info.screenId, info.cellX, info.cellY);
                    Log.d(TAG, "moveoutHomeOrAppsItemsFromLockedFolder Home no ID ");
                } else {
                    updateItemInDb(info);
                    Log.d(TAG, "moveoutHomeOrAppsItemsFromLockedFolder Home has ID");
                }
            }
        }
    }

    public View getFolderIconView(FolderInfo folder) {
        if (folder.isContainApps()) {
            return null;
        }
        return getHomescreenIconByItemId(folder.id);
    }

    public void notifyCapture(boolean immediate) {
        this.mHomeCapturePreview.notifyCapture(immediate);
    }

    void notifyCaptureIfNecessary() {
        this.mHomeCapturePreview.notifyCaptureIfNecessary();
    }

    public void addOrMoveItems(ArrayList<IconInfo> changedItems, long container, long screenId) {
        ArrayList<IconInfo> items = changedItems;
        HashMap<Long, ArrayList<IconInfo>> itemMap = new HashMap();
        for (ItemInfo item : items) {
            if (!itemMap.containsKey(item.container)) {
                itemMap.put(item.container, new ArrayList());
            }
            ((ArrayList) itemMap.get(item.container)).add((IconInfo) item);
        }
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            boolean hasHotseatItems = false;
            Iterator it = items.iterator();
            while (it.hasNext()) {
                IconInfo item2 = (IconInfo) it.next();
                if (!isItemInFolder(item2)) {
                    View iconView = getHomescreenIconByItemId(item2.id);
                    if (iconView != null) {
                        CellLayout cl;
                        if (item2.container == -101) {
                            hasHotseatItems = true;
                            cl = this.mHotseat.getLayout();
                        } else {
                            cl = (CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getPageIndexForScreenId(item2.screenId));
                        }
                        cl.removeView(iconView);
                    }
                }
            }
            if (hasHotseatItems) {
                this.mHotseat.getDragController().removeEmptyCells(false, true);
                this.mHotseat.changeGrid(true);
            }
        }
        Set<Long> keys = itemMap.keySet();
        ArrayList<ItemInfo> updateItems = new ArrayList();
        ArrayList<ContentValues> contentValues = new ArrayList();
        for (Long longValue : keys) {
            long containerId = longValue.longValue();
            ArrayList itemsInContainer = (ArrayList) itemMap.get(containerId);
            if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && containerId != container && containerId > 0) {
                FolderIconView iconView2 = (FolderIconView) getHomescreenIconByItemId(containerId);
                if (iconView2 != null) {
                    iconView2.getFolderInfo().remove(itemsInContainer);
                }
            }
            for (Object anItemsInContainer : itemsInContainer) {
                IconInfo info = (IconInfo) anItemsInContainer;
                info.container = container;
                info.screenId = screenId;
                if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    this.mFavoritesUpdater.addItem(info);
                }
                ContentValues values = new ContentValues();
                values.put("container", info.container);
                values.put(BaseLauncherColumns.RANK, info.rank);
                values.put("cellX", info.cellX);
                values.put("cellY", info.cellY);
                values.put("screen", info.screenId);
                values.put("hidden", info.hidden);
                updateItems.add(info);
                contentValues.add(values);
            }
        }
        this.mFavoritesUpdater.updateItemsInDatabaseHelper(this.mLauncher, contentValues, updateItems);
    }

    public int getPageIndexForDragView(ItemInfo item) {
        return this.mWorkspace.getDragController().getPageIndexForDragView(item);
    }

    void dropCompletedWidgetFromHotseat(DragObject d) {
        this.mWorkspace.getDragController().dropCompletedWidgetFromHotseat(d);
    }

    void dropCompletedFromHotseat(ArrayList<DragObject> extraDragObjects, Runnable postRunnable, boolean fromOther, int fullCnt) {
        this.mWorkspace.dropCompletedFromHotseat(extraDragObjects, postRunnable, fromOther, fullCnt);
    }

    void showCancelDropTarget() {
        if (this.mDropTargetBar != null) {
            this.mDropTargetBar.showCancelDropTarget();
        }
    }

    public void enableCustomLayoutAnimation(boolean enabled) {
        this.mEnabledCustomLayoutAnimation = enabled;
    }

    int getFolderItemCount(String title) {
        int resultCount = 0;
        if (!(this.mWorkspace == null || title == null)) {
            int pageCount = this.mWorkspace.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                CellLayout cl = (CellLayout) this.mWorkspace.getChildAt(i);
                if (cl != null) {
                    CellLayoutChildren clc = cl.getCellLayoutChildren();
                    if (clc != null) {
                        int childCount = clc.getChildCount();
                        for (int j = 0; j < childCount; j++) {
                            View v = clc.getChildAt(j);
                            if ((v instanceof FolderIconView) && title.equalsIgnoreCase(((IconView) v).getTitle())) {
                                resultCount++;
                            }
                        }
                    }
                }
            }
        }
        return resultCount;
    }

    private void findSearchedApp(ComponentName componentName, UserHandle user) {
        View view = this.mWorkspace.getIconView(componentName, user);
        if (view == null) {
            view = this.mHotseat.getIconView(componentName, user);
        }
        if (view == null) {
            ItemInfo info = null;
            if (componentName != null) {
                String searchedComp = componentName.flattenToShortString();
                Iterator it = this.mHomeLoader.getAllAppItemInHome().iterator();
                while (it.hasNext()) {
                    ItemInfo i = (ItemInfo) it.next();
                    if (i.componentName != null && i.getUserHandle() != null && searchedComp.equalsIgnoreCase(i.componentName.flattenToShortString()) && i.getUserHandle().getUser().equals(user)) {
                        info = i;
                        break;
                    }
                }
                if (info != null && info.isHiddenByUser()) {
                    StageEntry data = new StageEntry();
                    data.enableAnimation = true;
                    data.fromStage = 1;
                    data.putExtras(AppsPickerController.KEY_PICKER_MODE, 1);
                    data.putExtras(AppsPickerController.KEY_BOUNCED_ITEM, this.mLauncher.getSearchedApp());
                    data.putExtras(AppsPickerController.KEY_BOUNCED_ITEM_USER, this.mLauncher.getSearchedAppUser());
                    getStageManager().startStage(6, data);
                    this.mLauncher.setSearchedApp(null);
                    this.mLauncher.setSearchedAppUser(null);
                }
            }
        } else if (view.getTag() instanceof IconInfo) {
            IconInfo info2 = (IconInfo) view.getTag();
            if (info2.container == -100) {
                this.mWorkspace.snapToScreenId(info2.screenId);
            }
            startBounceAnimationForSearchedApp(view);
        } else if (view.getTag() instanceof FolderInfo) {
            findAppInFolder(view, componentName);
        }
    }

    private void findAppInFolder(View view, ComponentName componentName) {
        FolderInfo folderInfo = (FolderInfo) view.getTag();
        boolean needToScrollPage = false;
        if (folderInfo.container == -100 && this.mWorkspace.getCurrentPage() != this.mWorkspace.getPageIndexForScreenId(folderInfo.screenId)) {
            needToScrollPage = true;
            this.mWorkspace.snapToScreenId(folderInfo.screenId);
        }
        Iterator it = folderInfo.contents.iterator();
        while (it.hasNext()) {
            IconInfo info = (IconInfo) it.next();
            if (componentName.equals(info.getTargetComponent())) {
                final StageEntry data = new StageEntry();
                data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, view);
                data.putExtras(FolderController.KEY_FOLDER_ICON_APPSEARCHED, info);
                this.mFindAppPositionHandler.postDelayed(new Runnable() {
                    public void run() {
                        HomeController.this.mLauncher.getStageManager().startStage(5, data);
                    }
                }, needToScrollPage ? 950 : 300);
            }
        }
    }

    void initBounceAnimation() {
        if (this.mBounceAnimation != null) {
            this.mBounceAnimation.stop();
            this.mBounceAnimation = null;
        }
    }

    private void startBounceAnimationForSearchedApp(View bounceView) {
        if (bounceView != null) {
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            boolean z = dp.isLandscape || dp.homeGrid.getCellCountY() > 5;
            this.mBounceAnimation = new SearchedAppBounceAnimation(bounceView, z);
            this.mBounceAnimation.animate();
        }
    }

    public void onSetPageScrollListener(PageScrollListener listener) {
        this.mWorkspace.setPageScrollListener(listener);
    }

    void createAndShowSwipeAffordance() {
        PageIndicator indicator = this.mWorkspace.findPageIndicator();
        if (this.mSwipeAffordance == null && !LauncherAppState.getInstance().isHomeOnlyModeEnabled() && SwipeAffordance.needToCreateAffordance(this.mLauncher) && indicator != null) {
            this.mSwipeAffordance = (SwipeAffordance) this.mLauncher.findViewById(R.id.swipe_affordance);
            this.mSwipeAffordance.setup(this.mLauncher, indicator);
            if (!this.mLauncher.isPaused() && this.mState.equal(1) && this.mLauncher.isHomeStage()) {
                this.mSwipeAffordance.startAnim();
            }
        }
    }

    boolean isStartedSwipeAffordanceAnim() {
        return this.mSwipeAffordance != null && this.mSwipeAffordance.isStartedAnim();
    }

    public void updateZeroPage(int op) {
        if (this.mZeroPageController != null) {
            this.mZeroPageController.updateZeroPage(op);
            this.mWorkspace.updatePageIndicatorForZeroPage(isNormalState(), true);
            Log.d(TAG, "updateZeroPage:" + op);
        }
    }

    boolean isVisibleGridPanel() {
        return this.mScreenGridPanel != null && this.mScreenGridPanel.getVisibility() == 0 && this.mScreenGridPanel.getAlpha() > 0.0f;
    }

    void updateCountBadge(View view, boolean animate) {
        if (view instanceof IconView) {
            TextView countBadge = ((IconView) view).getCountBadgeView();
            if (countBadge != null && countBadge.getVisibility() == View.VISIBLE) {
                ((IconView) view).updateCountBadge(false, animate);
            }
        }
    }

    public void onSwipeBlockListener(float x, float y) {
        if (!(ZeroPageController.isActiveZeroPage(this.mLauncher, false) || this.mWorkspace == null || this.mWorkspace.getPageCount() > 1)) {
            this.mScrollDeterminator.setScrollableView(false);
        }
        this.mScrollDeterminator.setTalkBackEnabled(this.mLauncher);
        this.mScrollDeterminator.setBlockArea(this.mWorkspace, x, y);
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
        return 0.0f;
    }

    public boolean isOverBlurSlop(int slop) {
        return this.mScrollDeterminator == null || this.mScrollDeterminator.getCountTouchMove() > slop;
    }

    public void requestBlurChange(boolean show, Window dest, float amount, long duration) {
        if (this.mLauncher.isPaused() || this.mHomeContainer.getVisibility() != View.VISIBLE) {
            BlurUtils.blurByWindowManager(show, dest, amount, duration);
            return;
        }
        this.mBlurRunnableHandler.post(new BlurRunnable(show, dest, amount, duration, this.mLauncher));
    }

    void resetBlurRunnable() {
        if (this.mBlurRunnableHandler != null) {
            this.mBlurRunnableHandler.removeCallbacksAndMessages(null);
        }
    }

    boolean isHorizontalScoll() {
        return this.mScrollDeterminator == null || this.mScrollDeterminator.isHorizontalScroll();
    }

    void setScrollBlockArea(float x, float y) {
        this.mScrollDeterminator.setBlockArea(this.mWorkspace, x, y);
    }

    void setScrollForceBlock() {
        this.mScrollDeterminator.setForceBlock();
    }

    void setScrollTalkBackEnabled() {
        this.mScrollDeterminator.setTalkBackEnabled(this.mLauncher);
    }

    void setHomeDefaultIconClick(boolean isClicked) {
        this.mHomeDefaultIconClick = isClicked;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mWorkspace != null && this.mHotseat != null && this.mWorkspace.getFocusedChild() == null && this.mHotseat.getFocusedChild() == null && event.getAction() == 0 && !this.mWorkspace.isPageMoving()) {
            switch (event.getKeyCode()) {
                case 21:
                case 92:
                    this.mWorkspace.dispatchUnhandledMove(this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), 17);
                    break;
                case 22:
                case 93:
                    this.mWorkspace.dispatchUnhandledMove(this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), 66);
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    void startEdgeLight() {
        if (this.mEdgeLight != null) {
            this.mEdgeLight.startEdgeLight();
        }
    }

    void stopEdgeLight() {
        if (this.mEdgeLight != null) {
            this.mEdgeLight.stopEdgeLight();
        }
    }

    void updateBixbyHomeEnterCount() {
        if (this.mEdgeLight != null) {
            this.mEdgeLight.updateBixbyHomeEnterCount();
        }
    }

    boolean checkEdgeLightDisplayAvailability() {
        boolean isHomeStage;
        if (getStageManager() == null || getStageManager().getTopStage() != this) {
            isHomeStage = false;
        } else {
            isHomeStage = true;
        }
        boolean isQuickOptionShowing;
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing()) {
            isQuickOptionShowing = true;
        } else {
            isQuickOptionShowing = false;
        }
        if (!isHomeStage || this.mWorkspace == null || this.mLauncher.isPaused() || this.mWorkspace.getCurrentPage() != 0 || this.mWorkspace.isPageMoving() || isQuickOptionShowing || !this.mState.equal(1) || ZeroPageController.isMoving()) {
            return false;
        }
        return true;
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        List<CharSequence> text = event.getText();
        text.clear();
        if (this.mWorkspace != null) {
            text.add(this.mWorkspace.getCurrentPageDescription());
        } else {
            text.add(this.mLauncher.getResources().getString(R.string.all_apps_home_button_label));
        }
        return true;
    }

    boolean canMoveVertically() {
        return this.mScrollDeterminator != null && this.mScrollDeterminator.isVerticalScrollWithThreshold((int) (((float) Utilities.getFullScreenHeight(this.mLauncher)) * this.mStartSFinderRatio));
    }

    public void onZeroPageActiveChanged(boolean active) {
        this.mZeroPageController.onZeroPageActiveChanged(active);
    }

    public ScreenDivision getScreenDivision() {
        return this.mHomeContainer.mScreenDivision;
    }

    public void homeKeyPressed() {
        if (this.mLauncher.isHomeStage() && this.mState.equal(3)) {
            this.mHomeKeyPressed = true;
        }
    }

    public boolean searchBarHasFocus() {
        return false;
    }

    public void onNotificationPreviewBinding(Map<PackageUserKey, BadgeInfo> updatedatas) {
        Log.d(TAG, " onNotificationPreviewBinding");
        if (this.mLauncher.getDragMgr().getQuickOptionView() != null) {
            this.mLauncher.getDragMgr().getQuickOptionView().trimNotifications(updatedatas);
        }
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        super.setPaddingForNavigationBarIfNeeded(this.mHomeContainer, this.mWorkspace);
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.updateMultiSelectPanelLayout();
        }
        if (this.mWorkspace != null) {
            this.mWorkspace.setHintPageZone(this.mWorkspace.getResources());
        }
    }

    public void enterHomeScreenGrid(final boolean animated) {
        Runnable r = new Runnable() {
            public void run() {
                HomeController.this.enterScreenGridState(animated);
                HomeController.this.updateGridSize();
                HomeController.this.backupOriginalData();
            }
        };
        if (!isWorkspaceLocked() || !this.mLauncher.waitUntilResume(r)) {
            new Handler().post(r);
        }
    }

    void updateNotificationHelp(boolean updateList) {
        if (this.mNotificationHelpTipManager.isValidToShowHelpTip()) {
            if (updateList || needToIconList()) {
                setItemListNotificationHelp();
            }
            boolean isHomeStage = getStageManager() != null && getStageManager().getTopStage() == this;
            if (!isHomeStage || this.mWorkspace == null || this.mLauncher.isPaused() || this.mWorkspace.isPageMoving() || !this.mState.equal(1)) {
                setDisableNotificationHelpTip();
            } else if (this.mWorkspace.getCurrentPage() == this.mWorkspace.getDefaultPage()) {
                setEnableNotificationHelpTip();
                updateHelpTip();
            } else {
                setDisableNotificationHelpTip();
            }
        }
    }

    void setItemListNotificationHelp() {
        ArrayList<IconView> workSpaceList = this.mWorkspace.getIconList();
        ArrayList<IconView> hotSeatList = this.mHotseat.getIconList();
        Log.d(TAG, "workSpaceList.count = " + workSpaceList.size() + ", hotSeatList.count = " + hotSeatList.size());
        this.mNotificationHelpTipManager.setIconViewList(workSpaceList, hotSeatList);
    }

    void updateHelpTip() {
        this.mNotificationHelpTipManager.updateHelpTip();
    }

    void setEnableNotificationHelpTip() {
        this.mNotificationHelpTipManager.enableShowHelpTip();
    }

    void setDisableNotificationHelpTip() {
        this.mNotificationHelpTipManager.disableShowHelpTip();
    }

    private boolean needToIconList() {
        return (this.mNotificationHelpTipManager.hasIconList() || isWorkspaceLocked()) ? false : true;
    }
}
