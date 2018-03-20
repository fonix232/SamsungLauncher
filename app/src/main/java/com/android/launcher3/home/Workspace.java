package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.Stats;
import com.android.launcher3.Stats.LaunchSourceProvider;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.Insettable;
import com.android.launcher3.common.base.view.LauncherTransitionable;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources.IndicatorType;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Workspace extends PagedView implements LauncherTransitionable, OnHierarchyChangeListener, Insettable, LaunchSourceProvider {
    public static final long EXTRA_FESTIVALPAGE_SCREEN_ID = -501;
    public static final long EXTRA_PLUS_SCREEN_ID = -401;
    private static final float EXTRA_TOUCH_SLOP_SCALE_RATIO_OVERVIEW_STATE = 1.6f;
    public static final long EXTRA_ZEROPAGE_SCREEN_ID = -301;
    protected static final int FADE_EMPTY_SCREEN_DURATION = 150;
    private static final int FADE_HOME_ICON_DURATION = 300;
    private static final int PAGE_REMOVE_POPUP_DELAY = 300;
    protected static final int PINCH_DISTANCE_DELTA = 50;
    protected static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    private static final String TAG = "Launcher.Workspace";
    private int mAdditionalCount;
    private final Runnable mBindPages;
    private Runnable mCellConfigChangeRunnable;
    boolean mChildrenLayersEnabled;
    private WorkspaceCellLayout mCustomPage;
    private View mDefaultHomeIcon;
    private long mDefaultHomeScreenId;
    private int mDefaultPage;
    private Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
    private Point mDisplaySize;
    private DragManager mDragMgr;
    private HomeBindController mHomeBindController;
    private HomeController mHomeController;
    private boolean mIsDefaultZeroPage;
    private Launcher mLauncher;
    private LayoutTransition mLayoutTransition;
    private WorkspaceCellLayout mLongClickedPage;
    private float mOverviewShrinkFactor;
    private List<Runnable> mPostRunnablesAfterTransitionFinished;
    private final List<WorkspaceCellLayout> mRemainedWsCellAfterRotated;
    private Runnable mRemoveEmptyScreenRunnable;
    private AlertDialog mRemoveScreenDialog;
    private boolean mRestartZeroPage;
    private final ArrayList<Integer> mRestoredPages;
    private SparseArray<Parcelable> mSavedStates;
    private ArrayList<Long> mScreenOrder;
    private int[] mTempCell;
    private int[] mTempVisiblePagesRange;
    private final WallpaperManager mWallpaperManager;
    private IBinder mWindowToken;
    private LongArrayMap<CellLayout> mWorkspaceScreens;
    private WorkspaceDragController mWsDragController;
    private float mXDown;
    private float mYDown;
    private ZeroPageController mZeroPageController;

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWorkspaceScreens = new LongArrayMap();
        this.mScreenOrder = new ArrayList();
        this.mTempCell = new int[2];
        this.mChildrenLayersEnabled = true;
        this.mTempVisiblePagesRange = new int[2];
        this.mDisplaySize = new Point();
        this.mRestoredPages = new ArrayList();
        this.mDefaultHomeIcon = null;
        this.mAdditionalCount = 0;
        this.mCustomPage = null;
        this.mLongClickedPage = null;
        this.mRestartZeroPage = false;
        this.mBindPages = new Runnable() {
            public void run() {
                LauncherAppState.getInstance().getModel().getHomeLoader().bindRemainingSynchronousPages();
            }
        };
        this.mRemainedWsCellAfterRotated = new ArrayList();
        this.mCellConfigChangeRunnable = new Runnable() {
            public void run() {
                Workspace.this.cellConfigChangeAfterRotation();
            }
        };
        this.mPostRunnablesAfterTransitionFinished = new ArrayList();
        this.mLauncher = (Launcher) context;
        Resources res = getResources();
        this.mFadeInAdjacentScreens = false;
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        if (ZeroPageController.supportVirtualScreen()) {
            this.mZeroPageController = new ZeroPageController(context, this);
        }
        context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0).recycle();
        setupShrinkFactor();
        this.mDefaultPage = Utilities.getHomeDefaultPageKey(this.mLauncher);
        if (this.mDefaultPage < 0) {
            Log.d(TAG, "Default Page Error : " + this.mDefaultPage);
            this.mDefaultPage = 0;
            Utilities.setHomeDefaultPageKey(this.mLauncher, this.mDefaultPage);
        }
        if (LauncherFeature.supportZeroPageHome()) {
            updateHomeDefaultZeroPageKey(this.mDefaultPage, true);
        }
        this.mPageSpacing = res.getDimensionPixelSize(R.dimen.home_workspace_page_spacing);
        setOnHierarchyChangeListener(this);
        setHapticFeedbackEnabled(false);
        initWorkspace();
        this.mWsDragController = new WorkspaceDragController(this.mLauncher, this);
        setMotionEventSplittingEnabled(true);
        setHintPageZone(res);
    }

    void setupShrinkFactor() {
        this.mOverviewShrinkFactor = ((float) getResources().getInteger(R.integer.config_workspaceOverviewShrinkPercentage)) / 100.0f;
    }

    void bindController(ControllerBase controller) {
        this.mHomeController = (HomeController) controller;
        this.mHomeBindController = this.mHomeController.getBindController();
    }

    public WorkspaceDragController getDragController() {
        return this.mWsDragController;
    }

    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    public int[] estimateItemSize(ItemInfo itemInfo) {
        int[] size = new int[2];
        if (getChildCount() > 0) {
            Rect r = estimateItemPosition((CellLayout) getChildAt(getPageIndexToStart()), 0, 0, itemInfo.spanX, itemInfo.spanY);
            size[0] = r.width();
            size[1] = r.height();
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
        }
        return size;
    }

    Rect estimateItemPosition(CellLayout cl, int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    private void initWorkspace() {
        this.mCurrentPage = this.mDefaultPage;
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);
        setMinScale(this.mOverviewShrinkFactor);
        setupLayoutTransition();
        this.mLauncher.getWindowManager().getDefaultDisplay().getSize(this.mDisplaySize);
        setWallpaperDimension();
    }

    private void setupLayoutTransition() {
        this.mLayoutTransition = new LayoutTransition();
        this.mLayoutTransition.enableTransitionType(3);
        this.mLayoutTransition.enableTransitionType(1);
        this.mLayoutTransition.disableTransitionType(2);
        this.mLayoutTransition.disableTransitionType(0);
        this.mLayoutTransition.setStartDelay(1, 0);
        setLayoutTransition(this.mLayoutTransition);
    }

    private void enableLayoutTransitions() {
        setLayoutTransition(this.mLayoutTransition);
    }

    private void disableLayoutTransitions() {
        setLayoutTransition(null);
    }

    public void onChildViewAdded(View parent, View child) {
        if (child instanceof CellLayout) {
            if (this.mZeroPageController != null && ZeroPageController.isActiveZeroPage(this.mLauncher, false)) {
                PageIndicator pageIndicator = getPageIndicator();
                if (!(pageIndicator == null || pageIndicator.getMarkers() == null || pageIndicator.getMarkers().size() != 0)) {
                    setZeroPageMarker(true);
                    setMarkerStartOffset(1);
                    addMarkerForView(-1);
                }
            }
            CellLayout cl = (CellLayout) child;
            cl.setClickable(true);
            cl.setImportantForAccessibility(2);
            super.onChildViewAdded(parent, child);
            return;
        }
        throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
    }

    public boolean isTouchActive() {
        return this.mTouchState != 0;
    }

    void removeAllWorkspaceScreens() {
        disableLayoutTransitions();
        if (hasCustomContentPage(-301)) {
            this.mZeroPageController.removeZeroPagePreview(false);
        }
        if (hasCustomContentPage(-401)) {
            removeCustomPlusPage();
        }
        removeAllViews();
        this.mScreenOrder.clear();
        this.mWorkspaceScreens.clear();
        enableLayoutTransitions();
    }

    long insertNewWorkspaceScreenBeforeEmptyScreen(long screenId) {
        int insertIndex = this.mScreenOrder.indexOf(Long.valueOf(-201));
        if (insertIndex < 0) {
            insertIndex = this.mScreenOrder.indexOf(Long.valueOf(-401));
        }
        if (insertIndex < 0) {
            insertIndex = this.mScreenOrder.size();
        }
        return insertNewWorkspaceScreen(screenId, insertIndex);
    }

    long insertNewWorkspaceScreen(long screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    long insertNewWorkspaceScreen(long screenId, int insertIndex) {
        if (this.mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }
        if (LauncherFeature.supportFestivalPage() && this.mHomeController.getFestivalPageController() != null) {
            this.mHomeController.getFestivalPageController().removeCustomFestivalPage();
            if (insertIndex > getChildCount()) {
                insertIndex = getChildCount();
            }
        }
        WorkspaceCellLayout newScreen = createPage(screenId, insertIndex);
        if (isOverviewState()) {
            newScreen.setBackgroundAlpha(1.0f);
        }
        if (this.mHomeController.getState() == 2) {
            newScreen.setCrossHairAnimatedVisibility(0, false);
        }
        if (LauncherFeature.supportFestivalPage() && this.mHomeController.getState() == 1 && this.mHomeController.getFestivalPageController() != null) {
            this.mHomeController.getFestivalPageController().createCustomFestivalPage();
        }
        return screenId;
    }

    private boolean hasCustomContentPage(long screenId) {
        if (this.mScreenOrder.size() <= 0) {
            return false;
        }
        if (screenId == -301) {
            return ((Long) this.mScreenOrder.get(0)).longValue() == -301;
        } else {
            if (screenId != -401) {
                return false;
            }
            return ((Long) this.mScreenOrder.get(this.mScreenOrder.size() + -1)).longValue() == -401;
        }
    }

    private int getPageIndexToStart() {
        return hasCustomContentPage(-301) ? 1 : 0;
    }

    void addExtraEmptyScreenOnDrag() {
        this.mRemoveEmptyScreenRunnable = null;
        if (!this.mWorkspaceScreens.containsKey(-201)) {
            insertNewWorkspaceScreen(-201);
        }
    }

    boolean addExtraEmptyScreen() {
        if (this.mWorkspaceScreens.containsKey(-201)) {
            return false;
        }
        insertNewWorkspaceScreen(-201);
        return true;
    }

    void removeExtraEmptyScreen() {
        removeExtraEmptyScreenDelayed(null, 0);
    }

    void removeExtraEmptyScreenOnDrop(final Runnable onComplete) {
        if (this.mHomeBindController.isWorkspaceLoading()) {
            Launcher.addDumpLog(TAG, "removeExtraEmptyScreenOnDrop - workspace loading, skip", true);
        } else if (hasExtraEmptyScreen()) {
            Runnable r = new Runnable() {
                public void run() {
                    if (Utilities.sIsRtl) {
                        Workspace.this.snapToPageImmediately(Workspace.this.getNextPage());
                    } else {
                        Workspace.this.snapToPage(Workspace.this.getNextPage(), 0);
                    }
                    Workspace.this.fadeAndRemoveEmptyScreen(0, 0, onComplete);
                }
            };
            if (Utilities.sIsRtl && isPageMoving()) {
                post(r);
            } else {
                r.run();
            }
        } else if (onComplete != null) {
            onComplete.run();
        }
    }

    void removeExtraEmptyScreenDelayed(final Runnable onComplete, int delay) {
        if (this.mHomeBindController.isWorkspaceLoading()) {
            Launcher.addDumpLog(TAG, "removeExtraEmptyScreenDelayed - workspace loading, skip", true);
        } else if (delay > 0) {
            postDelayed(new Runnable() {
                public void run() {
                    Workspace.this.removeExtraEmptyScreenDelayed(onComplete, 0);
                }
            }, (long) delay);
        } else if (hasExtraEmptyScreen()) {
            if (getNextPage() == this.mScreenOrder.indexOf(Long.valueOf(-201))) {
                snapToPage(getNextPage() - 1, SNAP_OFF_EMPTY_SCREEN_DURATION);
                fadeAndRemoveEmptyScreen(SNAP_OFF_EMPTY_SCREEN_DURATION, 150, onComplete);
                return;
            }
            Runnable r = new Runnable() {
                public void run() {
                    Workspace.this.snapToPage(Workspace.this.getNextPage(), 0);
                    Workspace.this.fadeAndRemoveEmptyScreen(0, 150, onComplete);
                }
            };
            if (Utilities.sIsRtl && isPageMoving()) {
                post(r);
            } else {
                r.run();
            }
        } else if (onComplete != null) {
            onComplete.run();
        }
    }

    private void fadeAndRemoveEmptyScreen(int delay, int duration, final Runnable onComplete) {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", new float[]{0.0f});
        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", new float[]{0.0f});
        final CellLayout cl = (CellLayout) this.mWorkspaceScreens.get(-201);
        this.mRemoveEmptyScreenRunnable = new Runnable() {
            public void run() {
                if (Workspace.this.hasExtraEmptyScreen()) {
                    Workspace.this.mWorkspaceScreens.remove(-201);
                    Workspace.this.mScreenOrder.remove(Long.valueOf(-201));
                    Workspace.this.removeView(cl);
                }
            }
        };
        if (duration > 0) {
            ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(cl, new PropertyValuesHolder[]{alpha, bgAlpha});
            oa.setDuration((long) duration);
            oa.setStartDelay((long) delay);
            oa.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (Workspace.this.mRemoveEmptyScreenRunnable != null) {
                        Workspace.this.mRemoveEmptyScreenRunnable.run();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            oa.start();
            return;
        }
        this.mRemoveEmptyScreenRunnable.run();
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void removeItemsOnScreen(long screenId) {
        CellLayoutChildren clc = ((CellLayout) this.mWorkspaceScreens.get(screenId)).getCellLayoutChildren();
        int count = clc.getChildCount();
        ArrayList<ItemInfo> removeItems = new ArrayList();
        for (int i = 0; i < count; i++) {
            View child = clc.getChildAt(i);
            if (child != null) {
                removeItems.add((ItemInfo) child.getTag());
            }
        }
        this.mHomeController.removeItemsOnScreen(removeItems);
    }

    boolean hasExtraEmptyScreen() {
        int nScreens = getChildCount() - getPageIndexToStart();
        if (!this.mWorkspaceScreens.containsKey(-201) || nScreens <= 1) {
            return false;
        }
        return true;
    }

    long commitExtraEmptyScreen() {
        if (this.mHomeBindController.isWorkspaceLoading()) {
            Launcher.addDumpLog(TAG, "    - workspace loading, skip", true);
            return -1;
        }
        int index = getPageIndexForScreenId(-201);
        CellLayout cl = (CellLayout) this.mWorkspaceScreens.get(-201);
        this.mWorkspaceScreens.remove(-201);
        this.mScreenOrder.remove(Long.valueOf(-201));
        long newId = FavoritesProvider.getInstance().generateNewScreenId();
        this.mWorkspaceScreens.put(newId, cl);
        this.mScreenOrder.add(Long.valueOf(newId));
        if (getPageIndicator().isGrouping()) {
            removeMarkerForView(getPageIndicator().getMarkerStartOffset() + index);
        } else {
            updateMarker(getPageIndicator().getMarkerStartOffset() + index, getPageIndicatorMarker(index));
        }
        this.mHomeController.updateWorkspaceScreenOrder(this.mScreenOrder);
        return newId;
    }

    CellLayout getScreenWithId(long screenId) {
        return (CellLayout) this.mWorkspaceScreens.get(screenId);
    }

    long getIdForScreen(CellLayout layout) {
        int index = this.mWorkspaceScreens.indexOfValue(layout);
        if (index != -1) {
            return this.mWorkspaceScreens.keyAt(index);
        }
        return -1;
    }

    int getPageIndexForScreenId(long screenId) {
        return indexOfChild((View) this.mWorkspaceScreens.get(screenId));
    }

    long getScreenIdForPageIndex(int index) {
        if (index < 0 || index >= this.mScreenOrder.size()) {
            return -1;
        }
        return ((Long) this.mScreenOrder.get(index)).longValue();
    }

    LongArrayMap<CellLayout> getWorkspaceScreens() {
        return this.mWorkspaceScreens;
    }

    ArrayList<Long> getScreenOrder() {
        return this.mScreenOrder;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & 255) {
            case 0:
                if (isOverviewState()) {
                    int x = (int) ev.getRawX();
                    int y = (int) ev.getRawY();
                    Rect homeIconRect = new Rect();
                    this.mDefaultHomeIcon.getGlobalVisibleRect(homeIconRect);
                    if (homeIconRect.contains(x, y)) {
                        return false;
                    }
                }
                this.mHomeController.initBounceAnimation();
                break;
            case 2:
                this.mHomeController.initBounceAnimation();
                break;
        }
        return super.onTouchEvent(ev);
    }

    protected void onUnhandledTap(MotionEvent ev) {
    }

    protected void onWindowVisibilityChanged(int visibility) {
        this.mLauncher.onWindowVisibilityChanged(visibility);
        if (visibility == 8 && this.mZeroPageController != null && !this.mRestartZeroPage) {
            Point offset = this.mZeroPageController.getOffset();
            if (offset != null && offset.x > 1) {
                this.mRestartZeroPage = true;
            }
        } else if (visibility == 0) {
            this.mRestartZeroPage = false;
        }
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (this.mHomeController.isModalState() || this.mHomeController.isSwitchingState()) {
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mHomeController.getState() == 3) {
            if (this.mLauncher.getDragLayer().isResizeFrameArea(ev.getRawX(), ev.getRawY())) {
                return false;
            }
            if (ev.getAction() == 0) {
                this.mHomeController.setScrollForceBlock();
                this.mHomeController.exitResizeState(true, "2");
                return true;
            }
        }
        HomeContainer homeContainer = this.mHomeController.getHomeContainer();
        switch (ev.getAction() & 255) {
            case 0:
                this.mXDown = ev.getX();
                this.mYDown = ev.getY();
                break;
            case 1:
            case 6:
                if (this.mTouchState == 0 && ((CellLayout) getChildAt(this.mCurrentPage)) != null) {
                    onWallpaperTap(ev);
                    break;
                }
            case 2:
                if (this.mXDown > ev.getX()) {
                    this.mLauncher.getPageTransitionManager().setLeftScroll(true);
                } else {
                    this.mLauncher.getPageTransitionManager().setLeftScroll(false);
                }
                if ((this.mHomeController.isNormalState() || this.mHomeController.isOverviewState()) && homeContainer.isMultiTouch()) {
                    cancelCurrentPageLongPress();
                    if (this.mTouchState != 0) {
                        snapToDestination();
                    }
                    this.mTouchState = 0;
                    if (!(this.mHomeController.isSwitchingState() || ZeroPageController.isMoving())) {
                        int pinchDelta = homeContainer.getPinchDelta();
                        if (Math.abs(pinchDelta) > PINCH_DISTANCE_DELTA) {
                            this.mTouchState |= 3;
                            if (pinchDelta > 0 && this.mHomeController.isNormalState()) {
                                Log.d(TAG, "enter Overview by pinch zoom");
                                this.mHomeController.enterOverviewState(true);
                                Resources res = this.mLauncher.getResources();
                                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_1xxx), res.getString(R.string.event_HomeOptions), "1");
                            } else if (pinchDelta < 0 && this.mHomeController.isOverviewState()) {
                                this.mHomeController.enterNormalState(true);
                            }
                            return true;
                        }
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean determineScrollingStart(android.view.MotionEvent r10) {
        /*
        r9 = this;
        r7 = 0;
        r6 = r9.mLauncher;
        r6 = r6.isPaused();
        if (r6 == 0) goto L_0x0011;
    L_0x0009:
        r6 = "Launcher.Workspace";
        r8 = "determineScrollingStart() return : activity paused";
        android.util.Log.d(r6, r8);
    L_0x0010:
        return r7;
    L_0x0011:
        r6 = r10.getX();
        r8 = r9.mXDown;
        r3 = r6 - r8;
        r0 = java.lang.Math.abs(r3);
        r6 = r10.getY();
        r8 = r9.mYDown;
        r6 = r6 - r8;
        r1 = java.lang.Math.abs(r6);
        r6 = 0;
        r6 = java.lang.Float.compare(r0, r6);
        if (r6 != 0) goto L_0x0037;
    L_0x002f:
        r6 = "Launcher.Workspace";
        r8 = "determineScrollingStart() return : case 1";
        android.util.Log.d(r6, r8);
        goto L_0x0010;
    L_0x0037:
        r6 = r9.shouldPageStop(r3, r7);
        if (r6 != 0) goto L_0x0044;
    L_0x003d:
        r6 = 1;
        r6 = r9.shouldPageStop(r3, r6);
        if (r6 == 0) goto L_0x0066;
    L_0x0044:
        r6 = r9.hasExtraEmptyScreen();
        if (r6 != 0) goto L_0x0057;
    L_0x004a:
        r8 = r9.mMaxScrollX;
        r6 = com.android.launcher3.Utilities.sIsRtl;
        if (r6 == 0) goto L_0x005f;
    L_0x0050:
        r6 = r7;
    L_0x0051:
        r6 = r9.getScrollForPage(r6);
        if (r8 == r6) goto L_0x0066;
    L_0x0057:
        r6 = "Launcher.Workspace";
        r8 = "determineScrollingStart() return : case 2";
        android.util.Log.d(r6, r8);
        goto L_0x0010;
    L_0x005f:
        r6 = r9.getChildCount();
        r6 = r6 + -1;
        goto L_0x0051;
    L_0x0066:
        r6 = r9.mCurrentPage;
        r2 = r9.getChildAt(r6);
        r2 = (com.android.launcher3.home.WorkspaceCellLayout) r2;
        if (r2 == 0) goto L_0x007e;
    L_0x0070:
        r6 = r2.hasPerformedLongPress();
        if (r6 == 0) goto L_0x007e;
    L_0x0076:
        r6 = "Launcher.Workspace";
        r8 = "determineScrollingStart() return : case 3";
        android.util.Log.d(r6, r8);
        goto L_0x0010;
    L_0x007e:
        r6 = r9.mHomeController;
        r6 = r6.isOverviewState();
        if (r6 == 0) goto L_0x009f;
    L_0x0086:
        r5 = 1070386381; // 0x3fcccccd float:1.6 double:5.288411386E-315;
    L_0x0089:
        r6 = r9.mTouchSlop;
        r6 = (float) r6;
        r4 = r6 * r5;
        r6 = (r0 > r4 ? 1 : (r0 == r4 ? 0 : -1));
        if (r6 > 0) goto L_0x0096;
    L_0x0092:
        r6 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1));
        if (r6 <= 0) goto L_0x0099;
    L_0x0096:
        r9.cancelCurrentPageLongPress();
    L_0x0099:
        r7 = super.determineScrollingStart(r10);
        goto L_0x0010;
    L_0x009f:
        r5 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        goto L_0x0089;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.Workspace.determineScrollingStart(android.view.MotionEvent):boolean");
    }

    private boolean shouldPageStop(float deltaX, boolean isPageIndexZero) {
        boolean shouldStop;
        if (!isPageIndexZero) {
            shouldStop = getCurrentPage() >= getChildCount() + -2;
        } else if (getCurrentPage() <= 0) {
            shouldStop = true;
        } else {
            shouldStop = false;
        }
        if (shouldStop) {
            if (Utilities.sIsRtl) {
                if (isPageIndexZero) {
                    if (Float.compare(deltaX, 0.0f) < 0) {
                        return true;
                    }
                } else if (Float.compare(deltaX, 0.0f) > 0) {
                    return true;
                }
            } else if (isPageIndexZero) {
                if (Float.compare(deltaX, 0.0f) > 0) {
                    return true;
                }
            } else if (Float.compare(deltaX, 0.0f) < 0) {
                return true;
            }
        }
        return false;
    }

    protected boolean determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        float adjustedTouchSlopScale = touchSlopScale;
        if (this.mHomeController.isSelectState()) {
            adjustedTouchSlopScale = touchSlopScale * 2.5f;
        } else if (this.mHomeController.isOverviewState()) {
            adjustedTouchSlopScale = touchSlopScale * EXTRA_TOUCH_SLOP_SCALE_RATIO_OVERVIEW_STATE;
        }
        return super.determineScrollingStart(ev, adjustedTouchSlopScale);
    }

    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        if (this.mLauncher.getPageTransitionManager() != null) {
            this.mLauncher.getPageTransitionManager().onPageBeginMoving();
        }
        if (this.mHomeController.getState() == 3 && this.mCurrentPage != this.mNextPage) {
            this.mHomeController.exitResizeState(false);
        }
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr() != null && this.mLauncher.getDragMgr().isQuickOptionShowing() && this.mCurrentPage != this.mNextPage) {
            this.mDragMgr.removeQuickOptionView();
        }
        this.mHomeController.stopEdgeLight();
        this.mHomeController.setDisableNotificationHelpTip();
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        if (this.mLauncher.getPageTransitionManager() != null) {
            this.mLauncher.getPageTransitionManager().onPageEndMoving();
        }
        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }
        if (this.mDragMgr.isDragging() && this.mHomeController.isModalState()) {
            this.mDragMgr.forceTouchMove();
        }
        int page = getCurrentPage();
        if (this.mDelayedResizeRunnable != null) {
            this.mDelayedResizeRunnable.run();
            this.mDelayedResizeRunnable = null;
        }
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
            this.mDelayedSnapToPageRunnable = null;
        }
        if (page == getDefaultPage()) {
            this.mHomeController.notifyCaptureIfNecessary();
        }
        if (this.mHomeController.getState() == 1) {
            if (LauncherFeature.supportHotword()) {
                this.mLauncher.setHotWordDetection(this.mLauncher.isGoogleSearchWidget(getCurrentPage()));
            }
            this.mHomeController.startEdgeLight();
            this.mHomeController.updateNotificationHelp(false);
        } else if (this.mHomeController.getState() == 2 && !this.mLauncher.getDragMgr().isInScrollArea()) {
            ((WorkspaceCellLayout) getChildAt(this.mCurrentPage)).startPageFullVI();
        }
    }

    protected void onScrollInteractionBegin() {
        super.onScrollInteractionEnd();
    }

    protected void onScrollInteractionEnd() {
        super.onScrollInteractionEnd();
    }

    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
    }

    protected void setWallpaperDimension() {
    }

    protected boolean isScrollableToZeroPage() {
        if (this.mHomeController.isNormalState() && this.mZeroPageController != null && ZeroPageController.isActiveZeroPage(this.mLauncher, false)) {
            return true;
        }
        return false;
    }

    public void snapToPageImmediately(int whichPage) {
        super.snapToPageImmediately(whichPage);
        if (isNormalState()) {
            CellLayout cellLayout = (CellLayout) getChildAt(whichPage);
            if (cellLayout != null) {
                cellLayout.setBackgroundAlpha(0.0f);
            }
        }
    }

    protected void snapToPage(int whichPage, Runnable r) {
        snapToPage(whichPage, (int) PagedView.SLOW_PAGE_SNAP_ANIMATION_DURATION, r);
    }

    protected void snapToPage(int whichPage, int duration, Runnable r) {
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
        }
        this.mDelayedSnapToPageRunnable = r;
        super.snapToPage(whichPage, duration);
    }

    protected void snapToPage(int whichPage, int duration, boolean immediate, TimeInterpolator interpolator) {
        super.snapToPage(whichPage, duration, immediate, interpolator);
        if (whichPage == -1 && !this.mHomeController.isModalState() && this.mZeroPageController != null && ZeroPageController.isActiveZeroPage(this.mLauncher, false)) {
            post(new Runnable() {
                public void run() {
                    Workspace.this.mZeroPageController.switchToZeroPage();
                    Workspace.this.resetNormalPageAlphaValue(0);
                }
            });
        }
    }

    public void snapToPage(int whichPage, int duration, TimeInterpolator interpolator) {
        super.snapToPage(whichPage, duration, false, interpolator);
    }

    void snapToScreenId(long screenId) {
        snapToScreenId(screenId, null);
    }

    private void snapToScreenId(long screenId, Runnable r) {
        snapToPage(getPageIndexForScreenId(screenId), r);
    }

    protected void snapToPage(int whichPage, int delta, int duration, boolean immediate, TimeInterpolator interpolator) {
        super.snapToPage(whichPage, delta, duration, immediate, interpolator);
        if (isOverviewState() && this.mCurrentPage != whichPage) {
            if ((!LauncherFeature.supportZeroPageHome() && whichPage == getPageIndexForScreenId(-301)) || whichPage == getPageIndexForScreenId(-401)) {
                hideDefaultHomeIcon();
            } else if (this.mTouchState != 2) {
                showDefaultHomeIcon(false);
            }
            if (this.mTouchState != 2) {
                updateDefaultHomeIcon(whichPage);
                setVisibilityOnCustomLayout(false, false, true, this.mCurrentPage);
                setVisibilityOnCustomLayout(true, false, true, whichPage);
            }
        }
    }

    public void announceForAccessibility(CharSequence text) {
        if (!this.mLauncher.isAppsStage()) {
            super.announceForAccessibility(text);
        }
    }

    private void updatePageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        if (!this.mHomeController.isOverviewState() && !this.mHomeController.isScreenGridState()) {
            if (this.mHomeController.getState() == 1) {
                updateNormalPageAlphaValues(screenCenter, leftScreen, rightScreen);
            } else if (this.mHomeController.isSwitchingState()) {
                updateDragExtraPageAlphaValue(screenCenter, rightScreen);
            } else {
                updateDragPageAlphaValues(screenCenter, leftScreen, rightScreen);
            }
        }
    }

    private void updateNormalPageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        if (!isPageMoving()) {
            return;
        }
        if (this.mZeroPageController == null || !this.mZeroPageController.isRunningAnimation()) {
            for (int i = leftScreen; i <= rightScreen; i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    if (child.getCellLayoutChildren().getChildCount() == 0) {
                        child.setBackgroundAlpha(backgroundAlphaThreshold(Math.abs(getScrollProgress(screenCenter, child, i))));
                    } else {
                        child.setBackgroundAlpha(0.0f);
                    }
                }
            }
        }
    }

    public void resetNormalPageAlphaValue(int index) {
        CellLayout child = (CellLayout) getChildAt(index);
        if (child != null) {
            child.setBackgroundAlpha(0.0f);
        }
    }

    private void updateDragPageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        for (int i = leftScreen; i <= rightScreen; i++) {
            CellLayout child = (CellLayout) getChildAt(i);
            if (child != null) {
                if (i == getNextPage()) {
                    child.setBackgroundAlpha(0.0f);
                } else {
                    float alpha = Math.min(1.0f, Math.abs(getScrollProgress(screenCenter, child, i)));
                    if (!(child instanceof WorkspaceCellLayout)) {
                        child.setBackgroundAlpha(alpha);
                    } else if (!((WorkspaceCellLayout) child).isPageFullVIStarted()) {
                        child.setBackgroundAlpha(alpha);
                    }
                }
            }
        }
    }

    private void updateDragExtraPageAlphaValue(int screenCenter, int rightScreen) {
        if (rightScreen == getPageCount() - 1) {
            CellLayout child = (CellLayout) getChildAt(rightScreen);
            if (child != null && Math.abs(getScrollProgress(screenCenter, child, rightScreen)) == 1.0f) {
                child.setBackgroundAlpha(1.0f);
            }
        }
    }

    private float backgroundAlphaThreshold(float r) {
        if (r < 0.0f) {
            return 0.0f;
        }
        if (r > 0.3f) {
            return 1.0f;
        }
        return (r - 0.0f) / (0.3f - 0.0f);
    }

    protected void screenScrolled(int screenCenter, int leftScreen, int rightScreen) {
        updatePageAlphaValues(screenCenter, leftScreen, rightScreen);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mWindowToken = getWindowToken();
        computeScroll();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mWindowToken = null;
    }

    public void onResume() {
        super.onResume();
        if (LauncherAppState.getInstance().hasWallpaperChangedSinceLastCheck()) {
            setWallpaperDimension();
        }
        post(this.mBindPages);
    }

    public void onPause() {
        super.onPause();
        if (this.mRemoveScreenDialog != null && this.mRemoveScreenDialog.isShowing()) {
            this.mRemoveScreenDialog.dismiss();
            this.mRemoveScreenDialog = null;
        }
        if (this.mZeroPageController != null) {
            this.mZeroPageController.closeZeroPageDownloadDialog();
        }
        if (isOverviewState()) {
            OverviewPanel overviewPanel = this.mHomeController.getOverviewPanel();
            if (overviewPanel != null) {
                overviewPanel.closeThemeDownloadDialog();
            }
        }
        cancelScroll();
        if (LauncherFeature.supportZeroPageHome() && !this.mLauncher.getVisible() && this.mRestartZeroPage && !Utilities.getZeroPageKey(this.mLauncher, ZeroPageProvider.START_FROM_ZEROPAGE)) {
            Utilities.setZeroPageKey(this.mLauncher, true, ZeroPageProvider.START_FROM_ZEROPAGE);
            ZeroPageProvider.notifyChange(this.mLauncher);
            Log.i(TAG, "RestartZeroPage: " + this.mRestartZeroPage);
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ENTER_ZEROPAGE, null, -1, false);
            GSIMLogging.getInstance().setZeroPageStartTime();
        }
    }

    void updatePageIndicatorForZeroPage(boolean visible, boolean forceUpdate) {
        PageIndicator indicator = getPageIndicator();
        if (indicator == null) {
            Log.d(TAG, "updatePageIndicatorForZeroPage, indicator is null");
        } else if (ZeroPageController.isActiveZeroPage(this.mLauncher, false)) {
            if (visible && indicator.getMarkerStartOffset() == 0) {
                setZeroPageMarker(true);
                addMarkerForView(-1);
                setMarkerStartOffset(1);
                setActiveMarker(getCurrentPage());
            } else if (!visible && indicator.getMarkerStartOffset() == 1) {
                setMarkerStartOffset(0);
                removeMarkerForView(0);
                setZeroPageMarker(false);
                setActiveMarker(getCurrentPage());
            } else if (forceUpdate) {
                setActiveMarker(getCurrentPage());
            }
        } else if (indicator.getMarkerStartOffset() == 1) {
            setZeroPageMarker(false);
            setMarkerStartOffset(0);
            setActiveMarker(getCurrentPage());
            removeMarkerForView(0);
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.mLauncher.getStageManager().isRunningAnimation()) {
            post(this.mBindPages);
            postDelayed(this.mCellConfigChangeRunnable, 10);
        } else if (this.mPostRunnablesAfterTransitionFinished.isEmpty()) {
            this.mPostRunnablesAfterTransitionFinished.add(this.mBindPages);
            this.mPostRunnablesAfterTransitionFinished.add(this.mCellConfigChangeRunnable);
        }
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (this.mLauncher.isAppsStage()) {
            return false;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!this.mLauncher.isAppsStage()) {
            FolderView openFolder = this.mLauncher.getOpenFolderView();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }

    private void clearChildrenCache() {
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }

    void updateChildrenLayersEnabled(boolean force) {
        updateChildrenLayersEnabled(force, true);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!this.mUpdateOnlyCurrentPage || this.mIsPageMoving || child == null || child.equals(getChildAt(getCurrentPage()))) {
            return super.drawChild(canvas, child, drawingTime);
        }
        Log.d(TAG, "drawChild, mUpdateOnlyCurrentPage && !mIsPageMoving && !currentPage => draw skip!");
        return false;
    }

    private void updateChildrenLayersEnabled(boolean force, boolean show) {
        boolean enableChildrenLayers;
        boolean small;
        if (this.mHomeController.getState() == 4 && show) {
            small = true;
        } else {
            small = false;
        }
        boolean isSwitching = this.mHomeController.isSwitchingState();
        if (force || small || isSwitching || isPageMoving()) {
            enableChildrenLayers = true;
        } else {
            enableChildrenLayers = false;
        }
        if (enableChildrenLayers != this.mChildrenLayersEnabled) {
            this.mChildrenLayersEnabled = enableChildrenLayers;
            if (this.mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
                return;
            }
            for (int i = 0; i < getPageCount(); i++) {
                ((CellLayout) getChildAt(i)).enableHardwareLayer(false);
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (this.mChildrenLayersEnabled) {
            int screenCount = getChildCount();
            getVisiblePages(this.mTempVisiblePagesRange);
            int leftScreen = this.mTempVisiblePagesRange[0];
            int rightScreen = this.mTempVisiblePagesRange[1];
            if (leftScreen == rightScreen) {
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }
            CellLayout customScreen = (CellLayout) this.mWorkspaceScreens.get(-301);
            CellLayout festivalScreen = (CellLayout) this.mWorkspaceScreens.get(-501);
            int i = 0;
            while (i < screenCount) {
                boolean enableLayer;
                CellLayout layout = (CellLayout) getPageAt(i);
                if (layout == customScreen || layout == festivalScreen || leftScreen > i || i > rightScreen || !shouldDrawChild(layout)) {
                    enableLayer = false;
                } else {
                    enableLayer = true;
                }
                layout.enableHardwareLayer(enableLayer);
                i++;
            }
        }
    }

    public void buildPageHardwareLayers() {
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((CellLayout) getChildAt(i)).buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
    }

    protected void onWallpaperTap(MotionEvent ev) {
        int[] position = this.mTempCell;
        getLocationOnScreen(position);
        int pointerIndex = ev.getActionIndex();
        position[0] = position[0] + ((int) ev.getX(pointerIndex));
        position[1] = position[1] + ((int) ev.getY(pointerIndex));
        this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), ev.getAction() == 1 ? "android.wallpaper.tap" : "android.wallpaper.secondaryTap", position[0], position[1], 0, null);
    }

    public void onStartReordering() {
        super.onStartReordering();
        disableLayoutTransitions();
        this.mDefaultHomeScreenId = getScreenIdForPageIndex(this.mDefaultPage);
    }

    public void onEndReordering() {
        super.onEndReordering();
        if (!this.mHomeBindController.isWorkspaceLoading()) {
            this.mScreenOrder.clear();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                this.mScreenOrder.add(Long.valueOf(getIdForScreen((CellLayout) getChildAt(i))));
            }
            this.mHomeController.updateWorkspaceScreenOrder(this.mScreenOrder);
            if (this.mDefaultPage != getPageIndexForScreenId(this.mDefaultHomeScreenId)) {
                setDefaultPage(getPageIndexForScreenId(this.mDefaultHomeScreenId));
                updateDefaultHomePageIndicator(this.mDefaultPage);
            }
            enableLayoutTransitions();
        }
    }

    boolean isOverviewState() {
        return this.mHomeController != null && this.mHomeController.isOverviewState();
    }

    boolean isNormalState() {
        return this.mHomeController.isNormalState();
    }

    boolean isScreenGridState() {
        return this.mHomeController.isScreenGridState();
    }

    boolean isVisibleGridPanel() {
        return this.mHomeController.isVisibleGridPanel();
    }

    void updateAccessibilityFlags(boolean show) {
        int total = getPageCount();
        for (int i = getPageIndexToStart(); i < total; i++) {
            updateAccessibilityFlags((CellLayout) getPageAt(i), i, show);
        }
        setImportantForAccessibility(show ? 0 : 4);
    }

    private void updateAccessibilityFlags(CellLayout page, int pageNo, boolean show) {
        int accessible = 4;
        int state = this.mHomeController.getState();
        if ((state == 4 || state == 5) && show) {
            page.setImportantForAccessibility(1);
            page.getCellLayoutChildren().setImportantForAccessibility(4);
            page.setContentDescription(getPageDescription(pageNo, false));
            return;
        }
        if ((state == 1 || state == 6) && show) {
            accessible = 0;
        }
        page.setImportantForAccessibility(2);
        page.getCellLayoutChildren().setImportantForAccessibility(accessible);
        page.setContentDescription(null);
        page.setAccessibilityDelegate(null);
    }

    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
        int i = true;
        if (toWorkspace) {
            int internalStateFrom = data.getInternalStateFrom();
            int internalStateTo = data.getInternalStateTo();
            if ((internalStateFrom == 1 && internalStateTo == 4) || (internalStateFrom == 4 && internalStateTo == 2)) {
                boolean z;
                if (internalStateTo != 4) {
                    z = false;
                }
                setCustomFlagOnChild(z, false);
            } else if (internalStateFrom == 4 && internalStateTo == 1) {
                int currentPage = getCurrentPage();
                int duration = getResources().getInteger(R.integer.config_overviewTransitionDuration);
                if (ZeroPageController.isEnableZeroPage() && getScreenIdForPageIndex(currentPage) == -301) {
                    if (animated) {
                        snapToPage(1, duration);
                    } else {
                        if (!isOverviewState()) {
                            i = 0;
                        }
                        snapToPageImmediately(i);
                    }
                } else if (getScreenIdForPageIndex(currentPage) == -401) {
                    if (animated) {
                        snapToPage(getPageCount() - 2, duration);
                    } else {
                        snapToPageImmediately(getPageCount() - 2);
                    }
                }
            }
        }
        invalidate();
        updateChildrenLayersEnabled(false, toWorkspace);
    }

    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
    }

    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
        updateChildrenLayersEnabled(false, toWorkspace);
        if (toWorkspace && data.getInternalStateTo() == 1) {
            if (data.getInternalStateFrom() == 4) {
                this.mHomeController.removeCustomPage(1);
            }
            if (data.getInternalStateFrom() == 4 || data.getInternalStateFrom() == 5) {
                new Handler().post(new Runnable() {
                    public void run() {
                        if (Workspace.this.mHomeController.isNormalState()) {
                            Workspace.this.setCustomFlagOnChild(false, true);
                        }
                    }
                });
            }
            if (isPageMoving()) {
                setCurrentPage(getNextPage());
                pageEndMoving();
            }
            resetNormalPageAlphaValue(this.mCurrentPage);
        }
        if (!this.mPostRunnablesAfterTransitionFinished.isEmpty()) {
            for (Runnable r : this.mPostRunnablesAfterTransitionFinished) {
                post(r);
            }
            this.mPostRunnablesAfterTransitionFinished.clear();
        }
        this.mHomeController.updateNotificationHelp(false);
    }

    private void setCustomFlagOnChild(boolean needCustomLayout, boolean needRequestLayout) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            WorkspaceCellLayout cellLayout = (WorkspaceCellLayout) getChildAt(i);
            if (cellLayout != null) {
                cellLayout.setCustomFlag(needCustomLayout);
                if (needRequestLayout) {
                    cellLayout.requestLayout();
                }
            }
        }
    }

    void mapPointFromSelfToChild(View v, float[] xy) {
        xy[0] = xy[0] - ((float) v.getLeft());
        xy[1] = xy[1] - ((float) v.getTop());
    }

    void setup(DragManager dragMgr, DragLayer dragLayer) {
        this.mDragMgr = dragMgr;
        updateChildrenLayersEnabled(false);
        this.mDragMgr.addDragListener(this.mWsDragController);
        this.mDragMgr.addDropTarget(this.mWsDragController);
        this.mWsDragController.setup(this.mDragMgr, dragLayer, this.mHomeController);
    }

    int getCurrentPageOffsetFromCustomContent() {
        return getNextPage() - getPageIndexToStart();
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        this.mSavedStates = container;
    }

    void restoreInstanceStateForChild(int child) {
        if (this.mSavedStates != null) {
            this.mRestoredPages.add(Integer.valueOf(child));
            CellLayout cl = (CellLayout) getChildAt(child);
            if (cl != null) {
                cl.restoreInstanceState(this.mSavedStates);
            }
        }
    }

    void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!this.mRestoredPages.contains(Integer.valueOf(i))) {
                restoreInstanceStateForChild(i);
            }
        }
        this.mRestoredPages.clear();
        this.mSavedStates = null;
    }

    void moveToScreen(int page, boolean animate) {
        if (animate) {
            snapToPage(page);
        } else {
            setCurrentPage(page);
        }
        View child = getChildAt(page);
        if (child != null) {
            child.requestFocus();
        }
    }

    public void moveToDefaultScreen(boolean animate) {
        moveToScreen(this.mDefaultPage, animate);
    }

    public PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        long screenId = getScreenIdForPageIndex(pageIndex);
        if (screenId == -201 || screenId == -401) {
            if (this.mScreenOrder.size() - getPageIndexToStart() > 1) {
                return new PageMarkerResources(IndicatorType.PLUS);
            }
        } else if (screenId == -501) {
            if (this.mScreenOrder.size() - getPageIndexToStart() > 1) {
                return new PageMarkerResources(IndicatorType.FESTIVAL);
            }
        } else if (pageIndex == this.mDefaultPage) {
            if (this.mScreenOrder.size() - getPageIndexToStart() > 0) {
                return new PageMarkerResources(IndicatorType.HOME);
            }
        } else if ((screenId == -301 || pageIndex == -1) && this.mScreenOrder.size() - getPageIndexToStart() > 0) {
            return new PageMarkerResources(IndicatorType.ZEROPAGE);
        }
        return super.getPageIndicatorMarker(pageIndex);
    }

    protected String getPageIndicatorDescription() {
        return getPageDescription(getNextPage(), false) + ", " + getResources().getString(R.string.homes_screen_settings);
    }

    public String getCurrentPageDescription() {
        boolean needToSwipeAffordance = this.mHomeController != null && this.mHomeController.isNormalState() && this.mHomeController.isStartedSwipeAffordanceAnim();
        return getPageDescription(getNextPage(), needToSwipeAffordance);
    }

    private String getPageDescription(int page, boolean needToSwipeAffordance) {
        int i;
        String pageDescription = "";
        int delta = getPageIndexToStart();
        int childCount = getChildCount() - delta;
        if (isOverviewState()) {
            i = 1;
        } else {
            i = 0;
        }
        int lastPage = childCount - i;
        int currentPage = (page + 1) - delta;
        if (needToSwipeAffordance) {
            pageDescription = getContext().getString(R.string.swipe_up_for_more_apps_with_two_fingers);
        }
        long screenId = getScreenIdForPageIndex(currentPage);
        if (screenId == -401) {
            return getContext().getString(R.string.accessibility_add_home_screen) + ", " + getContext().getString(R.string.accessibility_button);
        }
        if (screenId == -301 && this.mZeroPageController != null && this.mZeroPageController.getAppName() != null) {
            return this.mZeroPageController.getAppName();
        }
        if (lastPage > 0) {
            pageDescription = pageDescription + " " + String.format(getContext().getString(R.string.default_scroll_format), new Object[]{Integer.valueOf(currentPage), Integer.valueOf(lastPage)});
            if (currentPage == (this.mDefaultPage + 1) - delta) {
                pageDescription = pageDescription + " " + getContext().getString(R.string.default_page);
            } else if (currentPage == lastPage) {
                pageDescription = pageDescription + " " + getContext().getString(R.string.last_page);
            }
        }
        return pageDescription;
    }

    public void fillInLaunchSourceData(Bundle sourceData) {
        sourceData.putString("container", Stats.CONTAINER_HOMESCREEN);
        sourceData.putInt(Stats.SOURCE_EXTRA_CONTAINER_PAGE, getCurrentPage());
    }

    void setDelayedResizeRunnable(Runnable addResizeFrame) {
        this.mDelayedResizeRunnable = addResizeFrame;
    }

    void setDefaultPage(int defaultPage) {
        setDefaultPage(defaultPage, true);
    }

    void setDefaultPage(int defaultPage, boolean setPref) {
        if (setPref && this.mDefaultPage != defaultPage) {
            int defaultPageOnNormalState = ZeroPageController.isEnableZeroPage() ? defaultPage - 1 : defaultPage;
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_ChangeDefaultHome), (long) defaultPageOnNormalState);
            if (LauncherFeature.supportZeroPageHome()) {
                updateHomeDefaultZeroPageKey(defaultPageOnNormalState, false);
            }
            Utilities.setHomeDefaultPageKey(this.mLauncher, defaultPageOnNormalState);
        }
        this.mDefaultPage = defaultPage;
    }

    public int getDefaultPage() {
        return this.mDefaultPage;
    }

    private void updateHomeDefaultZeroPageKey(int defaultPage, boolean init) {
        boolean isDefaultZeroPage = false;
        if (defaultPage == -1) {
            isDefaultZeroPage = true;
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_ChangeDefaultHome), (long) defaultPage);
        }
        if (init || isDefaultZeroPage != this.mIsDefaultZeroPage) {
            this.mIsDefaultZeroPage = isDefaultZeroPage;
            Utilities.setZeroPageKey(getContext(), isDefaultZeroPage, ZeroPageProvider.ZEROPAGE_DEFAULT_HOME);
            ZeroPageProvider.notifyChange(getContext());
        }
    }

    ZeroPageController getZeroPageController() {
        return this.mZeroPageController;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!((ev.getAction() & 255) == 2 || this.mLongClickedPage == null)) {
            this.mLongClickedPage.setHasPerformedLongPress(false);
            this.mLongClickedPage = null;
        }
        if ((this.mZeroPageController == null || !this.mZeroPageController.dispatchTouchEvent(ev)) && !super.dispatchTouchEvent(ev)) {
            return false;
        }
        return true;
    }

    public void scrollBy(int x, int y) {
        if (this.mZeroPageController == null || this.mZeroPageController.canScroll()) {
            super.scrollBy(x, y);
        }
    }

    public void scrollTo(int x, int y) {
        if (!this.mHomeController.isSelectState() || (x >= 0 && x <= getMaxScrollX())) {
            super.scrollTo(x, y);
        }
    }

    private WorkspaceCellLayout createPage(long screenId, int insertIndex) {
        WorkspaceCellLayout newScreen = (WorkspaceCellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, this, false);
        newScreen.setBgImage(this.mHomeController.getState(), false);
        newScreen.setOnLongClickListener(this.mLongClickListener);
        newScreen.setOnClickListener(this.mLauncher);
        newScreen.setSoundEffectsEnabled(false);
        addCustomLayout(newScreen);
        this.mWorkspaceScreens.put(screenId, newScreen);
        this.mScreenOrder.add(insertIndex, Long.valueOf(screenId));
        addView(newScreen, insertIndex);
        if (isOverviewState()) {
            updateAccessibilityFlags(true);
            newScreen.setCustomFlag(true);
            checkAlignButtonEnabled();
        }
        return newScreen;
    }

    void createCustomPlusPage() {
        WorkspaceCellLayout customScreen = (WorkspaceCellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, this, false);
        customScreen.setBgImage(4, false);
        customScreen.setBackgroundAlpha(1.0f);
        customScreen.setupAddBtnLayout();
        customScreen.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Workspace.this.isOverviewState()) {
                    int index = Workspace.this.indexOfChild(v);
                    if (index == Workspace.this.mCurrentPage) {
                        Workspace.this.addNewWorkspaceScreen();
                    } else {
                        Workspace.this.moveToScreen(index, true);
                    }
                }
            }
        });
        this.mWorkspaceScreens.put(-401, customScreen);
        this.mScreenOrder.add(getChildCount(), Long.valueOf(-401));
        if (getPageIndicator() != null) {
            getPageIndicator().setPlusPage(true);
        }
        addView(customScreen, getChildCount());
        customScreen.setCustomFlag(true);
    }

    void removeCustomPlusPage() {
        CellLayout customScreen = getScreenWithId(-401);
        if (customScreen == null) {
            Launcher.addDumpLog(TAG, "Expected custom plus page to exist", true);
            return;
        }
        int currentIndex = getCurrentPage();
        if (getScreenIdForPageIndex(currentIndex) == -401) {
            setCurrentPage(currentIndex - 1);
        }
        this.mWorkspaceScreens.remove(-401);
        this.mScreenOrder.remove(Long.valueOf(-401));
        if (getPageIndicator() != null) {
            getPageIndicator().setPlusPage(false);
        }
        removeView(customScreen);
    }

    long addNewWorkspaceScreen() {
        int i = 1;
        if (this.mHomeBindController.isWorkspaceLoading()) {
            Launcher.addDumpLog(TAG, "    - workspace loading, skip", true);
            return -1;
        }
        int insertIndex = getPageIndexForScreenId(-401);
        long newId = FavoritesProvider.getInstance().generateNewScreenId();
        startAlphaAnimation(createPage(newId, insertIndex));
        if (getPageIndicator() != null) {
            getPageIndicator().updateMarker(insertIndex, getPageIndicatorMarker(insertIndex));
        }
        if (isOverviewState()) {
            if (insertIndex == this.mCurrentPage) {
                showDefaultHomeIcon(true);
                setVisibilityOnCustomLayout(true, true);
            } else {
                moveToScreen(insertIndex, true);
            }
        }
        this.mHomeController.updateWorkspaceScreenOrder(this.mScreenOrder);
        Talk.INSTANCE.say((int) R.string.new_page_created);
        SALogging instance = SALogging.getInstance();
        String string = getResources().getString(R.string.screen_HomeOption);
        String string2 = getResources().getString(R.string.event_Addpage);
        int pageCount = getPageCount();
        if (ZeroPageController.isEnableZeroPage()) {
            i = 2;
        }
        instance.insertEventLog(string, string2, (long) (pageCount - i));
        return newId;
    }

    protected int getNearestHoverOverPageIndex() {
        int index = super.getNearestHoverOverPageIndex();
        if (this.mScreenOrder.indexOf(Long.valueOf(-401)) == index || this.mScreenOrder.indexOf(Long.valueOf(-301)) == index || this.mScreenOrder.indexOf(Long.valueOf(-501)) == index) {
            return -1;
        }
        return index;
    }

    private void startAlphaAnimation(CellLayout target) {
        ValueAnimator bgAnim = ObjectAnimator.ofFloat(target, "backgroundAlpha", new float[]{0.0f, 1.0f});
        bgAnim.setDuration(300);
        bgAnim.start();
    }

    public void setRestorePage(int restorePage) {
        this.mRestorePage = restorePage;
    }

    public int getRestorePage() {
        return this.mRestorePage;
    }

    public void autoAlignItems(boolean upward) {
        int checkPopup;
        String string;
        if (checkNeedDisplayAutoalignDialog()) {
            AutoAlignConfirmDialog.createAndShow(this.mLauncher.getFragmentManager(), this.mHomeController, upward);
            checkPopup = 0;
        } else {
            this.mHomeController.autoAlignItems(upward, false);
            checkPopup = 1;
        }
        SALogging instance = SALogging.getInstance();
        String string2 = this.mLauncher.getResources().getString(R.string.screen_HomeOption);
        if (upward) {
            string = this.mLauncher.getResources().getString(R.string.event_AlignToTop);
        } else {
            string = this.mLauncher.getResources().getString(R.string.event_AlignToBottom);
        }
        instance.insertEventLog(string2, string, (long) checkPopup);
    }

    public boolean checkNeedDisplayAutoalignDialog() {
        return getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(LauncherFiles.AUTOALIGN_SHOW_DIALOG_KEY, true);
    }

    private void addCustomLayout(WorkspaceCellLayout child) {
        OnClickListener alignTopClickListner = new OnClickListener() {
            public void onClick(View view) {
                Log.d(Workspace.TAG, "onClick AlignTop");
                Workspace.this.autoAlignItems(true);
            }
        };
        OnClickListener alignBottomClickListner = new OnClickListener() {
            public void onClick(View view) {
                Log.d(Workspace.TAG, "onClick AlignBottom");
                Workspace.this.autoAlignItems(false);
            }
        };
        OnClickListener pageDeleteClickListner = new OnClickListener() {
            public void onClick(View view) {
                int checkEmpty;
                Log.d(Workspace.TAG, "onClick PageDelete");
                if (Workspace.this.isEmptyPage(Workspace.this.mCurrentPage)) {
                    Workspace.this.removeScreenWithItem(false, true);
                    checkEmpty = 0;
                } else {
                    Workspace.this.showRemoveScreenPopup();
                    checkEmpty = 1;
                }
                SALogging.getInstance().insertEventLog(Workspace.this.getResources().getString(R.string.screen_HomeOption), Workspace.this.getResources().getString(R.string.event_Removepage), (long) checkEmpty);
            }
        };
        child.addAlignLayoutTop(alignTopClickListner);
        child.addAlignLayoutBottom(alignBottomClickListner);
        child.addPageDeleteBtn(pageDeleteClickListner);
        Iterator it = child.getAlignLayoutList().iterator();
        while (it.hasNext()) {
            setAlphaWithVisibility((LinearLayout) it.next(), 8, false);
        }
        setAlphaWithVisibility(child.getPageDeleteBtn(), 8, false);
    }

    public void touchPageDeleteButton() {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(this.mCurrentPage);
        if (child != null) {
            child.touchPageDeleteBtn();
        }
    }

    private void removeScreenWithItem(boolean onlyScreen, boolean updateModel) {
        removeScreenWithItem(this.mCurrentPage, onlyScreen, updateModel);
    }

    public void removeScreenWithItem(int index, boolean onlyScreen, boolean updateModel) {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(index);
        if (child != null) {
            long screenId = getIdForScreen(child);
            if (!onlyScreen) {
                removeItemsOnScreen(screenId);
            }
            this.mWorkspaceScreens.remove(screenId);
            this.mScreenOrder.remove(Long.valueOf(screenId));
            removeView(child);
            updateDefaultHome();
            if (updateModel) {
                this.mHomeController.updateWorkspaceScreenOrder(this.mScreenOrder);
            }
            if (isOverviewState()) {
                updateAccessibilityFlags(true);
                setVisibilityOnCustomLayout(true, false, true, index);
                if (isLastScreen(index)) {
                    hideDefaultHomeIcon();
                }
            }
        }
    }

    void initDefaultHomeIcon() {
        if (this.mDefaultHomeIcon == null) {
            this.mDefaultHomeIcon = this.mLauncher.findViewById(R.id.default_home_button);
            this.mDefaultHomeIcon.setOnKeyListener(HomeFocusHelper.HOME_BUTTON_KEY_LISTENER);
            this.mDefaultHomeIcon.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    if (!Workspace.this.isPageMoving()) {
                        if (Workspace.this.mDefaultPage != Workspace.this.mCurrentPage) {
                            Workspace.this.mHomeController.setHomeDefaultIconClick(true);
                        }
                        Workspace.this.updateDefaultHome(Workspace.this.mDefaultPage, Workspace.this.mCurrentPage);
                    }
                }
            });
            if (WhiteBgManager.isWhiteBg()) {
                WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mDefaultHomeIcon.getForeground(), true);
            }
            this.mDefaultHomeIcon.bringToFront();
        }
        updateDefaultHomeIcon(this.mCurrentPage);
    }

    private void updateDefaultHomeIcon(int currentPage) {
        String selectTTS;
        this.mDefaultHomeIcon.setSelected(currentPage == this.mDefaultPage);
        if (currentPage == this.mDefaultPage) {
            selectTTS = getResources().getString(R.string.selected);
        } else {
            selectTTS = getResources().getString(R.string.not_selected);
        }
        String defaultDescription = getResources().getString(R.string.default_page) + ", " + selectTTS;
        this.mDefaultHomeIcon.setContentDescription(defaultDescription);
        Utilities.setHoverPopupContentDescription(this.mDefaultHomeIcon, defaultDescription);
    }

    void updateDefaultHomePageIndicator(int to) {
        if (getPageIndicator() != null) {
            getPageIndicator().updateHomeMarker(to, getPageIndicatorMarker(to));
        }
    }

    void updateDefaultHomePageBg(WorkspaceCellLayout child) {
        if (child != null) {
            child.setBgImage(4, indexOfChild(child) == this.mDefaultPage);
        }
    }

    void updateDefaultHome(int from, int to) {
        if (ZeroPageController.isActiveZeroPage(getContext(), false) || getScreenIdForPageIndex(to) != -301) {
            setDefaultPage(to);
            updateDefaultHomeIcon(this.mCurrentPage);
            updateDefaultHomePageIndicator(to);
            if (from != to) {
                updateDefaultHomePageBg((WorkspaceCellLayout) getChildAt(from));
            }
            updateDefaultHomePageBg((WorkspaceCellLayout) getChildAt(to));
            this.mHomeController.notifyCapture(false);
            this.mHomeController.updateNotificationHelp(true);
        }
    }

    private void updateDefaultHome() {
        if (this.mCurrentPage < this.mDefaultPage) {
            setDefaultPage(this.mDefaultPage - 1);
            updateDefaultHomeIcon(this.mCurrentPage);
        } else if (this.mCurrentPage == this.mDefaultPage) {
            updateDefaultHome(this.mDefaultPage, this.mCurrentPage - (getScreenIdForPageIndex(this.mCurrentPage) == -401 ? 1 : 0));
        }
    }

    void showDefaultHomeIcon(boolean isCheck) {
        if (!isCheck || this.mCurrentPage != getPageIndexForScreenId(-401)) {
            updateDefaultHomeIcon(this.mCurrentPage);
            setAlphaWithVisibility(this.mDefaultHomeIcon, 0, true);
        }
    }

    void hideDefaultHomeIcon() {
        hideDefaultHomeIcon(true);
    }

    void hideDefaultHomeIcon(boolean animate) {
        if (this.mDefaultHomeIcon != null) {
            setAlphaWithVisibility(this.mDefaultHomeIcon, 8, animate);
        }
    }

    void checkAlignButtonEnabled() {
        checkAlignButtonEnabled(this.mCurrentPage);
    }

    private void checkAlignButtonEnabled(int pageIndex) {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(pageIndex);
        if (child != null && !child.getAlignLayoutList().isEmpty()) {
            child.setEnabledOnAlignButton(this.mHomeController.autoAlignItems(true, true, pageIndex), this.mHomeController.autoAlignItems(false, true, pageIndex));
        }
    }

    public ArrayList<LinearLayout> getAlignLayoutList() {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(this.mCurrentPage);
        ArrayList<LinearLayout> alignLayoutList = new ArrayList();
        if (child != null) {
            return child.getAlignLayoutList();
        }
        return alignLayoutList;
    }

    public View getPageDeleteBtn(int index) {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(index);
        if (child != null) {
            return child.getPageDeleteBtn();
        }
        return null;
    }

    public View getPageDeleteBtn() {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(this.mCurrentPage);
        if (child != null) {
            return child.getPageDeleteBtn();
        }
        return null;
    }

    public LinearLayout getZeroPageSwitchLayout() {
        WorkspaceCellLayout child = (WorkspaceCellLayout) getChildAt(this.mCurrentPage);
        if (child != null) {
            return child.getZeroPageSwitchLayout();
        }
        return null;
    }

    void onChangeChildState() {
        checkVisibilityOfPageDeleteBtn();
        checkAlignButtonEnabled();
    }

    void checkVisibilityOfCustomLayout(int nextIndex) {
        if (isOverviewState()) {
            setVisibilityOnCustomLayout(false, true, false, this.mCurrentPage);
            setVisibilityOnCustomLayout(true, true, false, nextIndex);
        }
    }

    private void checkVisibilityOfPageDeleteBtn() {
        if (LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled() && isOverviewState()) {
            View pageDeleteBtn = getPageDeleteBtn(this.mCurrentPage);
            if (pageDeleteBtn != null) {
                int visibility = isEmptyPage(this.mCurrentPage) ? 0 : 8;
                if (pageDeleteBtn.getVisibility() != visibility) {
                    setAlphaWithVisibility(pageDeleteBtn, visibility, false);
                }
            }
        }
    }

    void setVisibilityOnCustomLayout(boolean visible, boolean checkVisibility) {
        setVisibilityOnCustomLayout(visible, checkVisibility, true);
    }

    void setVisibilityOnCustomLayout(boolean visible, boolean checkVisibility, boolean animate) {
        setVisibilityOnCustomLayout(visible, checkVisibility, animate, this.mCurrentPage);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setVisibilityOnCustomLayout(boolean r12, boolean r13, boolean r14, int r15) {
        /*
        r11 = this;
        r8 = 8;
        r7 = 0;
        if (r12 == 0) goto L_0x000c;
    L_0x0005:
        r6 = r11.isOverviewState();
        if (r6 != 0) goto L_0x000c;
    L_0x000b:
        return;
    L_0x000c:
        r2 = r11.getChildAt(r15);
        r2 = (com.android.launcher3.home.WorkspaceCellLayout) r2;
        if (r2 == 0) goto L_0x000b;
    L_0x0014:
        if (r12 == 0) goto L_0x0042;
    L_0x0016:
        r11.mCustomPage = r2;
    L_0x0018:
        r1 = r2.getAlignLayoutList();
        r4 = 0;
        r9 = r1.iterator();
    L_0x0021:
        r6 = r9.hasNext();
        if (r6 == 0) goto L_0x007d;
    L_0x0027:
        r0 = r9.next();
        r0 = (android.widget.LinearLayout) r0;
        if (r0 == 0) goto L_0x0021;
    L_0x002f:
        if (r13 == 0) goto L_0x003a;
    L_0x0031:
        r10 = r0.getVisibility();
        if (r12 == 0) goto L_0x0079;
    L_0x0037:
        r6 = r8;
    L_0x0038:
        if (r10 != r6) goto L_0x0021;
    L_0x003a:
        if (r12 == 0) goto L_0x007b;
    L_0x003c:
        r6 = r7;
    L_0x003d:
        r11.setAlphaWithVisibility(r0, r6, r14);
        r4 = r12;
        goto L_0x0021;
    L_0x0042:
        r6 = r11.mCustomPage;
        if (r6 == 0) goto L_0x0018;
    L_0x0046:
        r6 = r11.mCustomPage;
        r6 = r2.equals(r6);
        if (r6 != 0) goto L_0x0018;
    L_0x004e:
        r6 = "Launcher.Workspace";
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "setVisibilityOnCustomLayout : pageIndex = ";
        r9 = r9.append(r10);
        r9 = r9.append(r15);
        r10 = ", indexOfChild(mCustomPage) = ";
        r9 = r9.append(r10);
        r10 = r11.mCustomPage;
        r10 = r11.indexOfChild(r10);
        r9 = r9.append(r10);
        r9 = r9.toString();
        android.util.Log.d(r6, r9);
        r2 = r11.mCustomPage;
        goto L_0x0018;
    L_0x0079:
        r6 = r7;
        goto L_0x0038;
    L_0x007b:
        r6 = r8;
        goto L_0x003d;
    L_0x007d:
        if (r4 == 0) goto L_0x0082;
    L_0x007f:
        r11.checkAlignButtonEnabled(r15);
    L_0x0082:
        r5 = r2.getZeroPageSwitchLayout();
        if (r5 == 0) goto L_0x0099;
    L_0x0088:
        if (r13 == 0) goto L_0x0093;
    L_0x008a:
        r9 = r5.getVisibility();
        if (r12 == 0) goto L_0x00cc;
    L_0x0090:
        r6 = r8;
    L_0x0091:
        if (r9 != r6) goto L_0x0099;
    L_0x0093:
        if (r12 == 0) goto L_0x00ce;
    L_0x0095:
        r6 = r7;
    L_0x0096:
        r11.setAlphaWithVisibility(r5, r6, r14);
    L_0x0099:
        if (r12 == 0) goto L_0x00a1;
    L_0x009b:
        r6 = r11.canDeleteScreen();
        if (r6 == 0) goto L_0x000b;
    L_0x00a1:
        r3 = r2.getPageDeleteBtn();
        if (r3 == 0) goto L_0x000b;
    L_0x00a7:
        r6 = com.android.launcher3.LauncherAppState.getInstance();
        r6 = r6.isHomeOnlyModeEnabled();
        if (r6 == 0) goto L_0x00ba;
    L_0x00b1:
        r6 = r11.isEmptyPage(r15);
        if (r6 != 0) goto L_0x00ba;
    L_0x00b7:
        if (r12 != 0) goto L_0x000b;
    L_0x00b9:
        r13 = 1;
    L_0x00ba:
        if (r13 == 0) goto L_0x00c5;
    L_0x00bc:
        r9 = r3.getVisibility();
        if (r12 == 0) goto L_0x00d0;
    L_0x00c2:
        r6 = r8;
    L_0x00c3:
        if (r9 != r6) goto L_0x000b;
    L_0x00c5:
        if (r12 == 0) goto L_0x00d2;
    L_0x00c7:
        r11.setAlphaWithVisibility(r3, r7, r14);
        goto L_0x000b;
    L_0x00cc:
        r6 = r7;
        goto L_0x0091;
    L_0x00ce:
        r6 = r8;
        goto L_0x0096;
    L_0x00d0:
        r6 = r7;
        goto L_0x00c3;
    L_0x00d2:
        r7 = r8;
        goto L_0x00c7;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.Workspace.setVisibilityOnCustomLayout(boolean, boolean, boolean, int):void");
    }

    private boolean isLastScreen(int index) {
        return getChildCount() + -1 == index;
    }

    private boolean canDeleteScreen() {
        if (isOverviewState()) {
            if (ZeroPageController.isEnableZeroPage()) {
                if (getChildCount() > 3) {
                    return true;
                }
                return false;
            } else if (getChildCount() <= 2) {
                return false;
            } else {
                return true;
            }
        } else if (getChildCount() <= 1) {
            return false;
        } else {
            return true;
        }
    }

    void changeColorForBg(boolean whiteBg) {
        int pageCount = getPageCount();
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mDefaultHomeIcon.getForeground(), whiteBg);
        int currentPage = 0;
        while (currentPage < pageCount) {
            WorkspaceCellLayout cl = (WorkspaceCellLayout) getChildAt(currentPage);
            CellLayoutChildren clItems = cl.getCellLayoutChildren();
            int childCount = clItems.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clItems.getChildAt(j);
                if (v instanceof IconView) {
                    ((IconView) v).changeTextColorForBg(whiteBg);
                }
            }
            cl.setBgImageResource(this.mHomeController.getState(), currentPage == getDefaultPage(), whiteBg);
            cl.changeColorForBg(whiteBg);
            currentPage++;
        }
        changePlusPageColorFilterForBg(whiteBg);
        PageIndicator pageIndicator = getPageIndicator();
        if (pageIndicator != null) {
            pageIndicator.changeColorForBg(whiteBg);
        }
    }

    private void changePlusPageColorFilterForBg(boolean whiteBg) {
        WorkspaceCellLayout cl = (WorkspaceCellLayout) getScreenWithId(-401);
        if (cl != null) {
            WhiteBgManager.changeColorFilterForBg(this.mLauncher, cl.getAddBtnDrawable(), whiteBg);
        }
    }

    void updateCheckBox(boolean visible) {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if (v instanceof FolderIconView) {
                    if (LauncherFeature.supportFolderSelect() || !visible) {
                        ((FolderIconView) v).updateCheckBox(visible);
                        ((FolderIconView) v).refreshCountBadge(0);
                    }
                    ((FolderIconView) v).refreshBadge();
                } else if (v instanceof IconView) {
                    ((IconView) v).updateCheckBox(visible);
                }
            }
        }
    }

    void removeTempPage(CellLayout layout) {
        long screenId = getIdForScreen(layout);
        int index = getPageIndexForScreenId(screenId);
        this.mWorkspaceScreens.remove(screenId);
        this.mScreenOrder.remove(Long.valueOf(screenId));
        removeView(layout);
        if (this.mCurrentPage >= index) {
            this.mCurrentPage--;
        }
        if (index < this.mDefaultPage) {
            this.mDefaultPage--;
        }
    }

    int getAdditionPageCount() {
        return this.mAdditionalCount;
    }

    void resetAdditionalPageCount() {
        this.mAdditionalCount = 0;
    }

    void insertPageAndMoveItems(int currentIndex, int targetIndex) {
        long screenID = insertPage(currentIndex, targetIndex);
        WorkspaceCellLayout currentCellLayout = (WorkspaceCellLayout) getChildAt(currentIndex);
        WorkspaceCellLayout targetCellLayout = (WorkspaceCellLayout) getChildAt(targetIndex);
        targetCellLayout.mTempPage = true;
        List<Pair<ItemInfo, View>> moveItems = currentCellLayout.mOutSideItems;
        int cellX = 0;
        int cellY = 0;
        List<Pair<ItemInfo, View>> againMoveItems = new ArrayList();
        int previousSpanY = 1;
        targetCellLayout.clearOccupiedCells();
        for (Pair<ItemInfo, View> pairItem : moveItems) {
            ItemInfo item = pairItem.first;
            item.screenId = screenID;
            if (item.spanX + cellX > currentCellLayout.getCountX()) {
                cellX = 0;
                cellY += previousSpanY;
            }
            if (item.spanY + cellY > currentCellLayout.getCountY()) {
                againMoveItems.add(pairItem);
            } else {
                int[] tmpXY = new int[2];
                targetCellLayout.findNearestVacantAreaWithCell(cellX, cellY, item.spanX, item.spanY, tmpXY, false);
                if (!(tmpXY[0] == -1 || tmpXY[1] == -1)) {
                    item.cellX = tmpXY[0];
                    item.cellY = tmpXY[1];
                }
                if (item instanceof LauncherAppWidgetInfo) {
                    this.mHomeController.addInScreen(((LauncherAppWidgetInfo) item).hostView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
                } else {
                    this.mHomeController.addInScreen((View) pairItem.second, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
                }
                cellX += item.spanX;
                previousSpanY = item.spanY;
            }
        }
        if (againMoveItems.size() > 0) {
            insertAdditionalPageAndMoveItems(currentIndex + 1, targetIndex + 1, againMoveItems);
        }
        targetCellLayout.setCrossHairAnimatedVisibility(0, false);
        targetCellLayout.mGridChanged = true;
    }

    void insertAdditionalPageAndMoveItems(int currentIndex, int targetIndex, List<Pair<ItemInfo, View>> againMoveItems) {
        Log.d(TAG, "insertAdditionalPageAndMoveItems()");
        long screenID = insertPage(currentIndex, targetIndex);
        WorkspaceCellLayout currentCellLayout = (WorkspaceCellLayout) getChildAt(currentIndex);
        WorkspaceCellLayout targetCellLayout = (WorkspaceCellLayout) getChildAt(targetIndex);
        targetCellLayout.mTempPage = true;
        int cellX = 0;
        int cellY = 0;
        int previousSpanY = 1;
        targetCellLayout.clearOccupiedCells();
        for (Pair<ItemInfo, View> pairItem : againMoveItems) {
            ItemInfo item = pairItem.first;
            item.screenId = screenID;
            if (item.spanX + cellX > currentCellLayout.getCountX()) {
                cellX = 0;
                cellY += previousSpanY;
            }
            int[] tmpXY = new int[2];
            targetCellLayout.findNearestVacantAreaWithCell(cellX, cellY, item.spanX, item.spanY, tmpXY, false);
            if (!(tmpXY[0] == -1 || tmpXY[1] == -1)) {
                item.cellX = tmpXY[0];
                item.cellY = tmpXY[1];
            }
            if (item instanceof LauncherAppWidgetInfo) {
                this.mHomeController.addInScreen(((LauncherAppWidgetInfo) item).hostView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
            } else {
                this.mHomeController.addInScreen((View) pairItem.second, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
            }
            cellX += item.spanX;
            previousSpanY = item.spanY;
        }
        this.mAdditionalCount++;
        targetCellLayout.setCrossHairAnimatedVisibility(0, false);
        targetCellLayout.mGridChanged = true;
    }

    private long insertPage(int currentIndex, int targetIndex) {
        long newId = FavoritesProvider.getInstance().generateNewScreenId();
        int currentPage = this.mCurrentPage;
        if (targetIndex <= this.mDefaultPage) {
            this.mDefaultPage++;
        }
        startAlphaAnimation(createPage(newId, targetIndex));
        if (isOverviewState()) {
            showDefaultHomeIcon(true);
        }
        if (currentPage > currentIndex) {
            currentPage++;
        }
        if (getPageIndicator() != null) {
            getPageIndicator().updateMarker(targetIndex, getPageIndicatorMarker(targetIndex));
            getPageIndicator().updateMarker(currentPage, getPageIndicatorMarker(currentPage));
        }
        setCurrentPage(currentPage);
        return newId;
    }

    void setAlphaWithVisibility(final View target, final int visibility, boolean animate) {
        if (target != null) {
            float alpha;
            if (visibility == 0) {
                alpha = 1.0f;
            } else {
                alpha = 0.0f;
            }
            if (animate) {
                AnimatorListenerAdapter listener;
                ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(target, "alpha", new float[]{0.0f});
                alphaAnimator.setDuration(300);
                alphaAnimator.setAutoCancel(true);
                alphaAnimator.setFloatValues(new float[]{alpha});
                if (visibility == 0) {
                    listener = new AnimatorListenerAdapter() {
                        public void onAnimationStart(Animator animation) {
                            target.setVisibility(visibility);
                        }
                    };
                } else {
                    listener = new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            target.setVisibility(visibility);
                        }
                    };
                }
                alphaAnimator.addListener(listener);
                alphaAnimator.start();
                return;
            }
            target.setVisibility(visibility);
            target.setAlpha(alpha);
        }
    }

    public boolean isEmptyPage(int pageIndex) {
        CellLayout indexPage = (CellLayout) getChildAt(pageIndex);
        return indexPage != null && indexPage.getCellLayoutChildren().getChildCount() == 0;
    }

    public LauncherAppWidgetHostView findWidgetView(ComponentName cn) {
        if (cn == null) {
            return null;
        }
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if ((v instanceof LauncherAppWidgetHostView) && (v.getTag() instanceof LauncherAppWidgetInfo)) {
                    ComponentName compareCn = ((LauncherAppWidgetInfo) v.getTag()).providerName;
                    if (compareCn != null && compareCn.getClassName().equals(cn.getClassName())) {
                        return (LauncherAppWidgetHostView) v;
                    }
                }
            }
        }
        return null;
    }

    private boolean isCurrentTransitionEffectDefault() {
        return this.mLauncher.getPageTransitionManager().getCurrentTransitionEffect() == null;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mLauncher.getPageTransitionManager().onLayout(this, changed, left, top, right, bottom);
    }

    protected void overScroll(int amount) {
        if (ZeroPageController.isActiveZeroPage(this.mLauncher, false)) {
            if (Utilities.sIsRtl) {
                if (amount > 0) {
                    return;
                }
            } else if (amount < 0) {
                return;
            }
        }
        super.overScroll(amount);
    }

    protected void updatePageTransform(View page, int index, int screenCenter) {
        if (page != null && this.mHomeController.getState() == 1) {
            float scrollProgress = getScrollProgress(screenCenter, page, index);
            if (isCurrentTransitionEffectDefault()) {
                if (ZeroPageController.isActiveZeroPage(this.mLauncher, false) && index == 0) {
                    if (Utilities.sIsRtl) {
                    }
                    resetTransitionEffect(page);
                    return;
                }
                super.updatePageTransform(page, index, screenCenter);
                return;
            }
            if (Math.abs(scrollProgress) == 1.0f || Math.abs(scrollProgress) == 0.0f) {
                this.mLauncher.getPageTransitionManager().reset(page);
            }
            if (this.mTempVisiblePagesRange[0] <= index && index <= this.mTempVisiblePagesRange[1]) {
                this.mLauncher.getPageTransitionManager().transformPage(page, scrollProgress);
            }
        }
    }

    protected boolean canOverScroll() {
        return this.mHomeController.getState() == 1;
    }

    protected void resetTransitionEffect(View page) {
        if (this.mHomeController.getState() != 1) {
            return;
        }
        if (isCurrentTransitionEffectDefault()) {
            super.resetTransitionEffect(page);
        } else {
            this.mLauncher.getPageTransitionManager().reset(page);
        }
    }

    protected void resetTransitionEffectForInvisiblePage(View page) {
        resetTransitionEffect(page);
    }

    public void dropCompletedFromHotseat(ArrayList<DragObject> extraDragObjects, Runnable postRunnable, boolean fromOther, int fullCnt) {
        this.mWsDragController.dropCompletedFromHotseat(extraDragObjects, postRunnable, fromOther, fullCnt);
    }

    public boolean hasTargetView() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            if (cl.hasTargetView()) {
                return cl.hasTargetView();
            }
        }
        return false;
    }

    public void setTargetView(View targetView) {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            ((CellLayout) getChildAt(i)).setTargetView(targetView);
        }
    }

    public IBinder getToken() {
        return this.mWindowToken;
    }

    public void pageEndMoving() {
        super.pageEndMoving();
    }

    public void resetTouchState() {
        this.mTouchState = 0;
    }

    protected void onConfigurationChangedIfNeeded() {
        super.onConfigurationChangedIfNeeded();
        Resources res = getResources();
        setupShrinkFactor();
        setMinScale(this.mOverviewShrinkFactor);
        MarginLayoutParams lp = (MarginLayoutParams) this.mDefaultHomeIcon.getLayoutParams();
        int dimensionPixelSize = res.getDimensionPixelSize(R.dimen.overview_home_button_size);
        lp.height = dimensionPixelSize;
        lp.width = dimensionPixelSize;
        lp.topMargin = res.getDimensionPixelOffset(R.dimen.overview_home_button_margin_top);
        this.mDefaultHomeIcon.setLayoutParams(lp);
        float pageIndicatorMarginBottomNormal = (float) this.mLauncher.getDeviceProfile().homeGrid.getIndicatorBottom();
        if (isOverviewState()) {
            setScaleX(this.mOverviewShrinkFactor);
            setScaleY(this.mOverviewShrinkFactor);
            setTranslationY((float) res.getDimensionPixelOffset(R.dimen.home_workspace_animate_offsetY_overview));
            if (getPageIndicator() != null) {
                getPageIndicator().setTranslationY((pageIndicatorMarginBottomNormal - ((float) res.getDimensionPixelOffset(R.dimen.home_workspace_indicator_margin_bottom_overview))) + ((float) this.mLauncher.getDeviceProfile().getOffsetIndicator()));
            }
        } else if (isScreenGridState()) {
            float screengridShrinkFactor = ((float) res.getInteger(R.integer.config_workspaceScreenGridShrinkPercentage)) / 100.0f;
            setScaleX(screengridShrinkFactor);
            setScaleY(screengridShrinkFactor);
            setTranslationY((float) res.getDimensionPixelOffset(R.dimen.home_workspace_animate_offsetY_screengrid));
            if (getPageIndicator() != null) {
                getPageIndicator().setTranslationY((pageIndicatorMarginBottomNormal - ((float) res.getDimensionPixelOffset(R.dimen.home_workspace_indicator_margin_bottom_screengrid))) + ((float) this.mLauncher.getDeviceProfile().getOffsetIndicatorForScreenGrid()));
            }
        }
        this.mRemainedWsCellAfterRotated.clear();
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            WorkspaceCellLayout workspaceCellLayout = (WorkspaceCellLayout) getChildAt(i);
            if (i == this.mCurrentPage || isScreenGridState()) {
                workspaceCellLayout.onConfigurationChangedIfNeeded();
            } else {
                this.mRemainedWsCellAfterRotated.add(workspaceCellLayout);
            }
        }
        setHintPageZone(res);
        if (isShowingHintPages()) {
            showHintPages();
        }
        setPageSpacing(res.getDimensionPixelOffset(R.dimen.home_workspace_page_spacing));
        if (this.mZeroPageController != null) {
            this.mZeroPageController.onConfigurationChangedIfNeeded();
        }
    }

    public void setHintPageZone(Resources res) {
        this.mHintPageLeftZone = res.getDimensionPixelSize(R.dimen.hint_page_scroll_zone);
        this.mHintPageRightZone = res.getDisplayMetrics().widthPixels - this.mHintPageLeftZone;
        this.mHintPageWidth = res.getDimensionPixelSize(R.dimen.hint_page_width);
        this.mTranslatePagesOffset = (float) res.getDimensionPixelSize(R.dimen.home_pulling_pages_offset);
        if (LauncherFeature.supportNavigationBar() && !LauncherFeature.isTablet()) {
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (Utilities.getNavigationBarPositon() == 1) {
                this.mHintPageLeftZone += dp.navigationBarHeight;
            } else if (Utilities.getNavigationBarPositon() == 2) {
                this.mHintPageRightZone -= dp.navigationBarHeight;
            }
        }
    }

    void createAndBindWidget(LauncherAppWidgetInfo item) {
        this.mHomeBindController.bindAppWidget(item);
    }

    private void cellConfigChangeAfterRotation() {
        if (!this.mLauncher.isDestroyed() && !this.mRemainedWsCellAfterRotated.isEmpty()) {
            WorkspaceCellLayout cell = (WorkspaceCellLayout) this.mRemainedWsCellAfterRotated.remove(0);
            if (cell != null) {
                cell.onConfigurationChangedIfNeeded();
            }
            postDelayed(this.mCellConfigChangeRunnable, 10);
        }
    }

    void abortCellConfigChangeAfterRotation() {
        this.mRemainedWsCellAfterRotated.clear();
    }

    private void showRemoveScreenPopup() {
        if (this.mRemoveScreenDialog == null) {
            this.mRemoveScreenDialog = new Builder(this.mLauncher).setMessage(R.string.remove_popup_msg).setCancelable(true).setPositiveButton(R.string.remove_popup_positive, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    SALogging.getInstance().insertEventLog(Workspace.this.getResources().getString(R.string.screen_HomeOption), Workspace.this.getResources().getString(R.string.event_Removepage_positive));
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            Workspace.this.removeScreenWithItem(false, true);
                        }
                    }, 300);
                }
            }).setNegativeButton(17039360, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    SALogging.getInstance().insertEventLog(Workspace.this.getResources().getString(R.string.screen_HomeOption), Workspace.this.getResources().getString(R.string.event_Removepage_negative));
                }
            }).setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(5);
                }
            }).create();
        }
        this.mRemoveScreenDialog.show();
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(12);
    }

    void updateDefaultHomeScreenId(long screenId) {
        this.mDefaultHomeScreenId = screenId;
    }

    View getIconView(ComponentName cn, UserHandle user) {
        if (cn == null) {
            return null;
        }
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            if (cl == null) {
                Log.e(TAG, "getIconView(), getChildAt( " + i + ") return null object");
                return null;
            }
            CellLayoutChildren clc = cl.getCellLayoutChildren();
            for (int j = 0; j < clc.getChildCount(); j++) {
                View v = clc.getChildAt(j);
                IconInfo info;
                if (v.getTag() instanceof IconInfo) {
                    info = (IconInfo) v.getTag();
                    if (cn.equals(info.getTargetComponent()) && user.equals(info.getUserHandle().getUser())) {
                        return v;
                    }
                } else if (v.getTag() instanceof FolderInfo) {
                    Iterator it = ((FolderInfo) v.getTag()).contents.iterator();
                    while (it.hasNext()) {
                        info = (IconInfo) it.next();
                        if (cn.equals(info.getTargetComponent()) && user.equals(info.getUserHandle().getUser())) {
                            return v;
                        }
                    }
                    continue;
                } else {
                    continue;
                }
            }
        }
        Log.d(TAG, "getIconView(), Could't find app icon");
        return null;
    }

    protected void notifyPageScroll(int page, int x, int y, int scrollX, int pageCount) {
        if (this.mHomeController.isSelectState()) {
            super.notifyPageScroll(page, x, y, scrollX, pageCount);
        }
    }

    protected void notifyPageChange(int page, int scrollX, int pageCount) {
        if (this.mHomeController.isSelectState()) {
            super.notifyPageChange(page, scrollX, pageCount);
        }
    }

    public int getCustomPageCount() {
        int i;
        int i2;
        int i3 = 1;
        if (hasCustomContentPage(-301)) {
            i = 1;
        } else {
            i = 0;
        }
        if (hasCustomContentPage(-401)) {
            i2 = 1;
        } else {
            i2 = 0;
        }
        i += i2;
        if (!hasExtraEmptyScreen()) {
            i3 = 0;
        }
        return i + i3;
    }

    public int getSupportCustomPageCount() {
        return ZeroPageController.isEnableZeroPage() ? 2 : 1;
    }

    public void snapToPageSALogging(boolean isPageIndicator) {
        int method = isPageIndicator ? 1 : 0;
        Resources res = this.mLauncher.getResources();
        if (this.mLauncher.getMultiSelectManager().isMultiSelectMode()) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_SelectMode), res.getString(R.string.event_SM_ChangePage), (long) method);
        } else if (this.mHomeController.getState() == 4) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_HomeOption), res.getString(R.string.event_Changepage), (long) method);
        } else {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_1xxx), res.getString(R.string.event_Home_ChangePage), (long) method);
        }
    }

    public void callRefreshLiveIcon() {
        if (!this.mWorkspaceScreens.isEmpty()) {
            Iterator it = this.mWorkspaceScreens.iterator();
            while (it.hasNext()) {
                ((CellLayout) it.next()).callRefreshLiveIcon();
            }
        }
    }

    boolean isPlusPage(WorkspaceCellLayout cellLayout) {
        return cellLayout != null && getScreenIdForPageIndex(indexOfChild(cellLayout)) == -401;
    }

    public boolean performLongClick() {
        this.mLongClickedPage = (WorkspaceCellLayout) getChildAt(this.mCurrentPage);
        if (this.mLongClickedPage != null) {
            this.mLongClickedPage.setHasPerformedLongPress(true);
        }
        return super.performLongClick();
    }

    ArrayList<IconView> getIconList() {
        CellLayoutChildren clc = ((CellLayout) getChildAt(this.mDefaultPage)).getCellLayoutChildren();
        int count = clc.getChildCount();
        ArrayList<IconView> allItems = new ArrayList();
        for (int i = 0; i < count; i++) {
            View child = clc.getChildAt(i);
            if (child != null && (child instanceof IconView)) {
                allItems.add((IconView) child);
            }
        }
        return allItems;
    }
}
