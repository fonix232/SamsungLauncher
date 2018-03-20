package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Advanceable;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.PairAppsIconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class HomeBindController implements HomeCallbacks {
    static final String ACTION_FIRST_LOAD_COMPLETE = "com.android.launcher3.action.FIRST_LOAD_COMPLETE";
    static final boolean DEBUG_WIDGETS = false;
    static final String FIRST_LOAD_COMPLETE = "launcher.first_load_complete";
    private static int NEW_APPS_ANIMATION_DELAY = 500;
    private static final String TAG = "HomeBindController";
    public static LongArrayMap<FolderInfo> sFolders = new LongArrayMap();
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
    private boolean mAutoAdvanceRunning = false;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private DragManager mDragMgr;
    private FavoritesUpdater mFavoritesUpdater;
    private FolderLock mFolderLock;
    private final Handler mHandler = new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                int i = 0;
                for (View key : HomeBindController.this.mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(((AppWidgetProviderInfo) HomeBindController.this.mWidgetsToAdvance.get(key)).autoAdvanceViewId);
                    int delay = i * Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
                    if (v instanceof Advanceable) {
                        HomeBindController.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                ((Advanceable) v).advance();
                            }
                        }, (long) delay);
                    }
                    i++;
                }
                HomeBindController.this.sendAdvanceMessage(20000);
            }
            return true;
        }
    });
    private HomeController mHomeController;
    private HomeLoader mHomeLoader;
    private Hotseat mHotseat;
    private boolean mHotseatLoading = true;
    private IconCache mIconCache;
    private Launcher mLauncher;
    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList();
    private boolean mUserPresent = true;
    private HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap();
    private Workspace mWorkspace;
    private boolean mWorkspaceLoading = true;

    HomeBindController(Context context, HomeController homeController, LauncherModel model, IconCache cache) {
        this.mLauncher = (Launcher) context;
        this.mIconCache = cache;
        this.mHomeLoader = model.getHomeLoader();
        this.mHomeLoader.registerCallbacks(this);
        this.mFavoritesUpdater = (FavoritesUpdater) model.getHomeLoader().getUpdater();
        this.mHomeController = homeController;
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    void setup(DragManager dragMgr, Workspace workspace, Hotseat hotseat) {
        this.mDragMgr = dragMgr;
        this.mWorkspace = workspace;
        this.mHotseat = hotseat;
    }

    public int getCurrentWorkspaceScreen() {
        if (this.mWorkspace != null) {
            return this.mWorkspace.getCurrentPage();
        }
        return 0;
    }

    public void startBinding() {
        setWorkspaceLoading(true);
        this.mLauncher.getBindOnResumeCallbacks().clear();
        this.mLauncher.closeFolder();
        this.mHomeController.clearDropTargets();
        this.mWorkspace.removeAllWorkspaceScreens();
        this.mWorkspace.abortCellConfigChangeAfterRotation();
        this.mWidgetsToAdvance.clear();
    }

    public void bindHotseatItems(final ArrayList<ItemInfo> items) {
        if (!this.mLauncher.waitUntilResumeForHotseat(new Runnable() {
            public void run() {
                HomeBindController.this.bindHotseatItems(items);
            }
        })) {
            bindHotseatItems(items, false, null);
        }
    }

    public void bindItem(ItemInfo item, boolean forceAnimateIcons) {
        final ItemInfo itemInfo = item;
        final boolean z = forceAnimateIcons;
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindItem(itemInfo, z);
            }
        })) {
            boolean animateIcons = forceAnimateIcons && this.mHomeController.canRunNewAppsAnimation();
            if (item.container != -101 || this.mHotseat != null) {
                View view;
                switch (item.itemType) {
                    case 0:
                    case 1:
                    case 6:
                    case 7:
                        view = createShortcut((IconInfo) item);
                        if (item.container == -100) {
                            CellLayout cl = this.mWorkspace.getScreenWithId(item.screenId);
                            if (cl != null) {
                                if (cl.isOccupied(item.cellX, item.cellY)) {
                                    View v = cl.getChildAt(item.cellX, item.cellY);
                                    if (v != null) {
                                        Log.d(TAG, "Collision while binding workspace item: " + item + ". Collides with " + v.getTag());
                                    } else {
                                        Log.d(TAG, "child view is null " + item);
                                    }
                                    Log.d(TAG, "This item will be bind after change the position");
                                    this.mHomeLoader.bindItemAfterChangePosition(item);
                                    return;
                                }
                            }
                        }
                        break;
                    case 2:
                        view = FolderIconView.fromXml(this.mLauncher, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), (FolderInfo) item, this.mHomeController, this.mLauncher, null, item.container == -101 ? 1 : 0);
                        break;
                    default:
                        throw new RuntimeException("Invalid Item Type");
                }
                this.mHomeController.addInScreenFromBind(view, item.container, item.screenId, item.cellX, item.cellY, 1, 1);
                if (animateIcons) {
                    if (this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getCurrentPage()) != item.screenId) {
                        this.mWorkspace.snapToPage(this.mWorkspace.getPageIndexForScreenId(item.screenId));
                    }
                    view.setAlpha(0.0f);
                    view.setScaleX(0.0f);
                    view.setScaleY(0.0f);
                    view.setLayerType(2, null);
                    Animator bounceAnim = this.mHomeController.createNewAppBounceAnimation(view, 0);
                    bounceAnim.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            view.setLayerType(0, null);
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    bounceAnim.start();
                }
            }
        }
    }

    private void bindHotseatItems(ArrayList<ItemInfo> shortcuts, boolean animateIcons, Collection<Animator> bounceAnims) {
        this.mHotseat.resetLayout();
        this.mHotseat.beginBind(shortcuts.size());
        Iterator it = shortcuts.iterator();
        while (it.hasNext()) {
            View view;
            ItemInfo item = (ItemInfo) it.next();
            switch (item.itemType) {
                case 0:
                case 1:
                case 6:
                case 7:
                    view = createShortcut((IconInfo) item);
                    break;
                case 2:
                    if (LauncherFeature.supportFolderLock()) {
                        FolderInfo folderInfo = (FolderInfo) item;
                        if (this.mFolderLock != null && this.mFolderLock.isLockedFolder(folderInfo)) {
                            this.mFolderLock.markAsLockedFolderWhenBind(folderInfo);
                        }
                    }
                    view = FolderIconView.fromXml(this.mLauncher, this.mHotseat, (FolderInfo) item, this.mHomeController, this.mLauncher, null, 1);
                    break;
                default:
                    throw new RuntimeException("Invalid Item Type");
            }
            this.mHomeController.addInScreenFromBind(view, item.container, item.screenId, item.cellX, item.cellY, 1, 1);
            if (animateIcons) {
                view.setAlpha(0.0f);
                view.setScaleX(0.0f);
                view.setScaleY(0.0f);
                bounceAnims.add(this.mHomeController.createNewAppBounceAnimation(view, 0));
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void bindItems(java.util.ArrayList<com.android.launcher3.common.base.item.ItemInfo> r32, int r33, int r34, boolean r35) {
        /*
        r31 = this;
        r4 = new com.android.launcher3.home.HomeBindController$4;
        r5 = r31;
        r6 = r32;
        r7 = r33;
        r8 = r34;
        r9 = r35;
        r4.<init>(r6, r7, r8, r9);
        r0 = r31;
        r5 = r0.mLauncher;
        r5 = r5.waitUntilResume(r4);
        if (r5 == 0) goto L_0x001a;
    L_0x0019:
        return;
    L_0x001a:
        r16 = com.android.launcher3.util.animation.LauncherAnimUtils.createAnimatorSet();
        r18 = new java.util.ArrayList;
        r18.<init>();
        r22 = new java.util.ArrayList;
        r22.<init>();
        if (r35 == 0) goto L_0x0064;
    L_0x002a:
        r0 = r31;
        r5 = r0.mHomeController;
        r5 = r5.canRunNewAppsAnimation();
        if (r5 == 0) goto L_0x0064;
    L_0x0034:
        r17 = 1;
    L_0x0036:
        r26 = -1;
        r23 = r33;
    L_0x003a:
        r0 = r23;
        r1 = r34;
        if (r0 >= r1) goto L_0x01ec;
    L_0x0040:
        r0 = r32;
        r1 = r23;
        r25 = r0.get(r1);
        r25 = (com.android.launcher3.common.base.item.ItemInfo) r25;
        r0 = r25;
        r8 = r0.container;
        r10 = -101; // 0xffffffffffffff9b float:NaN double:NaN;
        r5 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1));
        if (r5 != 0) goto L_0x0067;
    L_0x0054:
        r0 = r31;
        r5 = r0.mHotseat;
        if (r5 == 0) goto L_0x0061;
    L_0x005a:
        r0 = r22;
        r1 = r25;
        r0.add(r1);
    L_0x0061:
        r23 = r23 + 1;
        goto L_0x003a;
    L_0x0064:
        r17 = 0;
        goto L_0x0036;
    L_0x0067:
        r0 = r25;
        r5 = r0.itemType;
        switch(r5) {
            case 0: goto L_0x0076;
            case 1: goto L_0x0076;
            case 2: goto L_0x0161;
            case 3: goto L_0x006e;
            case 4: goto L_0x006e;
            case 5: goto L_0x006e;
            case 6: goto L_0x0076;
            case 7: goto L_0x0076;
            default: goto L_0x006e;
        };
    L_0x006e:
        r5 = new java.lang.RuntimeException;
        r6 = "Invalid Item Type";
        r5.<init>(r6);
        throw r5;
    L_0x0076:
        r24 = r25;
        r24 = (com.android.launcher3.common.base.item.IconInfo) r24;
        r0 = r31;
        r1 = r24;
        r7 = r0.createShortcut(r1);
        r0 = r25;
        r8 = r0.container;
        r10 = -100;
        r5 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1));
        if (r5 != 0) goto L_0x01ae;
    L_0x008c:
        r0 = r31;
        r5 = r0.mWorkspace;
        r0 = r25;
        r8 = r0.screenId;
        r19 = r5.getScreenWithId(r8);
        r0 = r25;
        r5 = r0.cellX;
        r6 = -1;
        if (r5 == r6) goto L_0x00a6;
    L_0x009f:
        r0 = r25;
        r5 = r0.cellY;
        r6 = -1;
        if (r5 != r6) goto L_0x00e6;
    L_0x00a6:
        r5 = "HomeBindController";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "bindItems item cellXY is -1 : ";
        r6 = r6.append(r8);
        r8 = r24.toString();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Log.d(r5, r6);
        r0 = r25;
        r5 = r0.hidden;
        if (r5 == 0) goto L_0x00e6;
    L_0x00c8:
        r5 = "HomeBindController";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "bind hidden item. skip... ";
        r6 = r6.append(r8);
        r8 = r24.toString();
        r6 = r6.append(r8);
        r6 = r6.toString();
        android.util.Log.d(r5, r6);
        goto L_0x0061;
    L_0x00e6:
        if (r19 == 0) goto L_0x01ae;
    L_0x00e8:
        r0 = r25;
        r5 = r0.cellX;
        r0 = r25;
        r6 = r0.cellY;
        r0 = r19;
        r5 = r0.isOccupied(r5, r6);
        if (r5 == 0) goto L_0x01ae;
    L_0x00f8:
        r0 = r25;
        r5 = r0.cellX;
        r0 = r25;
        r6 = r0.cellY;
        r0 = r19;
        r30 = r0.getChildAt(r5, r6);
        if (r30 == 0) goto L_0x0146;
    L_0x0108:
        r29 = r30.getTag();
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Collision while binding workspace item: ";
        r5 = r5.append(r6);
        r0 = r25;
        r5 = r5.append(r0);
        r6 = ". Collides with ";
        r5 = r5.append(r6);
        r0 = r29;
        r5 = r5.append(r0);
        r20 = r5.toString();
        r5 = "HomeBindController";
        r0 = r20;
        android.util.Log.d(r5, r0);
    L_0x0134:
        r5 = "HomeBindController";
        r6 = "This item will be bind after change the position";
        android.util.Log.d(r5, r6);
        r0 = r31;
        r5 = r0.mHomeLoader;
        r0 = r25;
        r5.bindItemAfterChangePosition(r0);
        goto L_0x0061;
    L_0x0146:
        r5 = "HomeBindController";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r8 = "child view is null ";
        r6 = r6.append(r8);
        r0 = r25;
        r6 = r6.append(r0);
        r6 = r6.toString();
        android.util.Log.d(r5, r6);
        goto L_0x0134;
    L_0x0161:
        r5 = com.android.launcher3.LauncherFeature.supportFolderLock();
        if (r5 == 0) goto L_0x0186;
    L_0x0167:
        r21 = r25;
        r21 = (com.android.launcher3.folder.FolderInfo) r21;
        r0 = r31;
        r5 = r0.mFolderLock;
        if (r5 == 0) goto L_0x0186;
    L_0x0171:
        r0 = r31;
        r5 = r0.mFolderLock;
        r0 = r21;
        r5 = r5.isLockedFolder(r0);
        if (r5 == 0) goto L_0x0186;
    L_0x017d:
        r0 = r31;
        r5 = r0.mFolderLock;
        r0 = r21;
        r5.markAsLockedFolderWhenBind(r0);
    L_0x0186:
        r0 = r31;
        r5 = r0.mLauncher;
        r0 = r31;
        r6 = r0.mWorkspace;
        r0 = r31;
        r8 = r0.mWorkspace;
        r8 = r8.getCurrentPage();
        r6 = r6.getChildAt(r8);
        r6 = (android.view.ViewGroup) r6;
        r7 = r25;
        r7 = (com.android.launcher3.folder.FolderInfo) r7;
        r0 = r31;
        r8 = r0.mHomeController;
        r0 = r31;
        r9 = r0.mLauncher;
        r10 = 0;
        r11 = 0;
        r7 = com.android.launcher3.folder.view.FolderIconView.fromXml(r5, r6, r7, r8, r9, r10, r11);
    L_0x01ae:
        r0 = r31;
        r6 = r0.mHomeController;
        r0 = r25;
        r8 = r0.container;
        r0 = r25;
        r10 = r0.screenId;
        r0 = r25;
        r12 = r0.cellX;
        r0 = r25;
        r13 = r0.cellY;
        r14 = 1;
        r15 = 1;
        r6.addInScreenFromBind(r7, r8, r10, r12, r13, r14, r15);
        if (r17 == 0) goto L_0x0061;
    L_0x01c9:
        r5 = 0;
        r7.setAlpha(r5);
        r5 = 0;
        r7.setScaleX(r5);
        r5 = 0;
        r7.setScaleY(r5);
        r0 = r31;
        r5 = r0.mHomeController;
        r0 = r23;
        r5 = r5.createNewAppBounceAnimation(r7, r0);
        r0 = r18;
        r0.add(r5);
        r0 = r25;
        r0 = r0.screenId;
        r26 = r0;
        goto L_0x0061;
    L_0x01ec:
        r5 = r22.size();
        if (r5 <= 0) goto L_0x020c;
    L_0x01f2:
        r0 = r31;
        r1 = r22;
        r2 = r17;
        r3 = r18;
        r0.bindHotseatItems(r1, r2, r3);
        if (r17 == 0) goto L_0x0207;
    L_0x01ff:
        r8 = 0;
        r5 = (r26 > r8 ? 1 : (r26 == r8 ? 0 : -1));
        if (r5 >= 0) goto L_0x0207;
    L_0x0205:
        r26 = 0;
    L_0x0207:
        r5 = 0;
        r0 = r31;
        r0.mHotseatLoading = r5;
    L_0x020c:
        if (r17 == 0) goto L_0x022d;
    L_0x020e:
        r8 = -1;
        r5 = (r26 > r8 ? 1 : (r26 == r8 ? 0 : -1));
        if (r5 <= 0) goto L_0x022d;
    L_0x0214:
        r28 = new com.android.launcher3.home.HomeBindController$5;
        r0 = r28;
        r1 = r31;
        r2 = r16;
        r3 = r18;
        r0.<init>(r2, r3);
        r0 = r31;
        r5 = r0.mWorkspace;
        r6 = NEW_APPS_ANIMATION_DELAY;
        r8 = (long) r6;
        r0 = r28;
        r5.postDelayed(r0, r8);
    L_0x022d:
        r0 = r31;
        r5 = r0.mWorkspace;
        r5.requestLayout();
        goto L_0x0019;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeBindController.bindItems(java.util.ArrayList, int, int, boolean):void");
    }

    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        bindAddScreens(orderedScreenIds);
        if (orderedScreenIds.size() == 0) {
            this.mWorkspace.addExtraEmptyScreen();
        }
    }

    public void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        Iterator it = orderedScreenIds.iterator();
        while (it.hasNext()) {
            this.mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(((Long) it.next()).longValue());
        }
    }

    public void bindInsertScreens(final long screenId, final int insertIndex) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindInsertScreens(screenId, insertIndex);
            }
        })) {
            Workspace workspace = this.mWorkspace;
            if (this.mHomeController.isOverviewState() && ZeroPageController.isEnableZeroPage()) {
                insertIndex++;
            }
            workspace.insertNewWorkspaceScreen(screenId, insertIndex);
        }
    }

    public void bindFolders(final LongArrayMap<FolderInfo> folders) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindFolders(folders);
            }
        })) {
            sFolders = folders.clone();
        }
    }

    public void bindFolderTitle(ItemInfo item) {
        if (item instanceof FolderInfo) {
            IconView v = (IconView) this.mHomeController.getFolderIconView((FolderInfo) item);
            if (v != null) {
                v.setText(item.title);
            }
        }
    }

    public void bindUpdatePosition(ArrayList<ItemInfo> updated) {
        final ArrayList<ItemInfo> arrayList = updated;
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindUpdatePosition(arrayList);
            }
        })) {
            Iterator it = updated.iterator();
            while (it.hasNext()) {
                View addView;
                ItemInfo info = (ItemInfo) it.next();
                if (info.getPrevContainer() != -1) {
                    long container = info.container;
                    info.container = info.getPrevContainer();
                    this.mHomeController.removeHomeItem(info);
                    info.setPrevContainer(-1);
                    info.container = container;
                    if (info instanceof IconInfo) {
                        addView = createShortcut((IconInfo) info);
                    }
                    addView = null;
                } else if (info.oldScreenId == -1 || info.oldScreenId == info.screenId) {
                    CellLayout cellLayout = this.mWorkspace.getScreenWithId(info.screenId);
                    childView = cellLayout.getCellLayoutChildren().getChildAt(info);
                    if (childView != null) {
                        LayoutParams lp = (LayoutParams) childView.getLayoutParams();
                        if (lp.useTmpCoords) {
                            lp.useTmpCoords = false;
                        }
                        cellLayout.animateChildToPosition(childView, info.cellX, info.cellY, 0, 0, true, true, (boolean[][]) null);
                    }
                    addView = null;
                } else {
                    CellLayout oldLayout = this.mWorkspace.getScreenWithId(info.oldScreenId);
                    childView = oldLayout.getCellLayoutChildren().getChildAt(info);
                    if (childView != null) {
                        oldLayout.removeView(childView);
                        addView = childView;
                    }
                    addView = null;
                }
                if (addView != null) {
                    this.mHomeController.addInScreenFromBind(addView, info.container, info.screenId, info.cellX, info.cellY, info.spanX, info.spanY);
                }
                info.oldScreenId = -1;
            }
        }
    }

    public void bindUpdateContainer(final boolean addToFolder, final FolderInfo folder, final IconInfo item) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindUpdateContainer(addToFolder, folder, item);
            }
        })) {
            if (item.getPrevContainer() != -1) {
                long container = item.container;
                item.container = item.getPrevContainer();
                this.mHomeController.removeHomeItem((ItemInfo) item);
                item.setPrevContainer(-1);
                item.container = container;
            }
            if (folder == null) {
                return;
            }
            if (!addToFolder) {
                folder.remove(item);
            } else if (!folder.contents.contains(item)) {
                folder.add(item);
            }
        }
    }

    public void finishBindingItems() {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.finishBindingItems();
            }
        })) {
            if (this.mLauncher.getSavedState() != null) {
                if (!this.mWorkspace.hasFocus()) {
                    View child = this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage());
                    if (child != null) {
                        child.requestFocus();
                    }
                }
                this.mLauncher.setSavedState(null);
            }
            this.mWorkspace.restoreInstanceStateForRemainingPages();
            if (this.mHotseatLoading) {
                this.mHotseatLoading = false;
            }
            if (LauncherFeature.supportCustomerDialerChange()) {
                this.mHomeController.changeDialerApp();
            }
            setWorkspaceLoading(false);
            sendLoadingCompleteBroadcastIfNecessary();
            if (this.mHomeController.getPendingAddItem() != null) {
                final long screenId = this.mHomeController.completeAdd(this.mHomeController.getPendingAddItem());
                this.mWorkspace.post(new Runnable() {
                    public void run() {
                        HomeBindController.this.mWorkspace.snapToScreenId(screenId);
                    }
                });
                this.mHomeController.setPendingAddItem(null);
            }
            LauncherAppState.getInstance().disableAndFlushExternalQueue();
            this.mHomeController.notifyCapture(true);
            SALogging.getInstance().setDefaultValueForHomeStatusLog(this.mLauncher);
            new Handler(LauncherModel.getWorkerLooper()).post(new Runnable() {
                public void run() {
                    GSIMLogging.getInstance().runFirstAppStatusLogging();
                }
            });
            if (!(this.mHomeController.isOverviewState() || this.mWorkspace.getPageIndicator() == null)) {
                this.mWorkspace.updatePageIndicatorForZeroPage(this.mHomeController.isNormalState(), true);
            }
            this.mDragMgr.setWindowToken(this.mWorkspace.getToken());
            this.mHomeController.createAndShowSwipeAffordance();
            if (!this.mLauncher.isPaused()) {
                this.mHomeController.updateNotificationHelp(true);
            }
        }
    }

    public void bindAppWidget(LauncherAppWidgetInfo item) {
        final LauncherAppWidgetInfo launcherAppWidgetInfo = item;
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindAppWidget(launcherAppWidgetInfo);
            }
        })) {
            if (this.mLauncher.isSafeModeEnabled()) {
                bindSafeModeWidget(item);
                return;
            }
            LauncherAppWidgetProviderInfo appWidgetInfo;
            Workspace workspace = this.mWorkspace;
            if (item.hasRestoreFlag(2)) {
                appWidgetInfo = null;
            } else if (item.hasRestoreFlag(1)) {
                appWidgetInfo = this.mHomeController.mAppWidgetManager.findProvider(item.providerName, item.user);
            } else {
                appWidgetInfo = this.mHomeController.mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
            }
            if (!(item.hasRestoreFlag(2) || item.restoreStatus == 0)) {
                if (appWidgetInfo == null) {
                    this.mFavoritesUpdater.deleteItem(item);
                    return;
                } else if ((item.restoreStatus & 1) != 0) {
                    PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this.mLauncher, appWidgetInfo, null);
                    pendingInfo.spanX = item.spanX;
                    pendingInfo.spanY = item.spanY;
                    pendingInfo.minSpanX = item.minSpanX;
                    pendingInfo.minSpanY = item.minSpanY;
                    WidgetHostViewLoader.getDefaultOptionsForWidget(this.mLauncher, pendingInfo);
                    int newWidgetId = this.mHomeController.getAppWidgetHost().allocateAppWidgetId();
                    if (AppWidgetManagerCompat.getInstance(this.mLauncher).bindAppWidgetIdIfAllowed(newWidgetId, appWidgetInfo, null)) {
                        item.appWidgetId = newWidgetId;
                        item.restoreStatus = appWidgetInfo.configure == null ? 0 : 4;
                        this.mHomeController.updateItemInDb(item);
                    } else {
                        this.mHomeController.getAppWidgetHost().deleteAppWidgetId(newWidgetId);
                        this.mFavoritesUpdater.deleteItem(item);
                        return;
                    }
                } else if (item.hasRestoreFlag(4) && appWidgetInfo.configure == null) {
                    item.restoreStatus = 0;
                    this.mHomeController.updateItemInDb(item);
                }
            }
            if (item.restoreStatus == 0) {
                int appWidgetId = item.appWidgetId;
                if (appWidgetInfo == null) {
                    Log.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                    deleteWidgetInfo(item);
                    return;
                }
                item.hostView = this.mHomeController.getAppWidgetHost().createView(this.mLauncher, appWidgetId, appWidgetInfo);
                item.minSpanX = appWidgetInfo.getMinSpanX();
                item.minSpanY = appWidgetInfo.getMinSpanY();
                addAppWidgetToWorkspace(item, appWidgetInfo);
                if (LauncherFeature.supportGSARoundingFeature() && appWidgetInfo != null && appWidgetInfo.provider.getPackageName().equals(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME)) {
                    ((LauncherAppWidgetHostView) item.hostView).mIsGSB = true;
                    Bundle opts = new Bundle();
                    opts.putString("attached-launcher-identifier", "samsung-dream-launcher");
                    opts.putString("requested-widget-style", "cqsb");
                    opts.putFloat("widget-screen-bounds-left", (float) item.hostView.getLeft());
                    opts.putFloat("widget-screen-bounds-top", (float) item.hostView.getTop());
                    opts.putFloat("widget-screen-bounds-right", (float) item.hostView.getRight());
                    opts.putFloat("widget-screen-bounds-bottom", (float) item.hostView.getBottom());
                    item.hostView.updateAppWidgetOptions(opts);
                }
                if (LauncherFeature.supportHotword() && item.providerName != null && item.providerName.equals(Launcher.GOOGLE_SEARCH_WIDGET)) {
                    this.mLauncher.setHotWordDetection(true);
                }
            } else {
                PendingAppWidgetHostView view = new PendingAppWidgetHostView(this.mLauncher, item, this.mLauncher.isSafeModeEnabled());
                view.updateIcon(this.mIconCache);
                item.hostView = view;
                item.hostView.updateAppWidget(null);
                item.hostView.setOnClickListener(this.mLauncher);
                addAppWidgetToWorkspace(item, null);
            }
            workspace.requestLayout();
        }
    }

    private void bindSafeModeWidget(LauncherAppWidgetInfo item) {
        PendingAppWidgetHostView view = new PendingAppWidgetHostView(this.mLauncher, item, true);
        view.updateIcon(this.mIconCache);
        item.hostView = view;
        item.hostView.updateAppWidget(null);
        item.hostView.setOnClickListener(this.mLauncher);
        addAppWidgetToWorkspace(item, null);
        this.mWorkspace.requestLayout();
    }

    private void addAppWidgetToWorkspace(LauncherAppWidgetInfo item, LauncherAppWidgetProviderInfo appWidgetInfo) {
        item.hostView.setTag(item);
        if (appWidgetInfo != null) {
            item.onBindAppWidget(this.mLauncher, true);
        } else {
            item.onBindAppWidget(this.mLauncher);
        }
        this.mHomeController.addInScreen(item.hostView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY);
        this.mHomeLoader.checkAppWidgetSingleInstanceList(item);
        if (!item.isCustomWidget()) {
            addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
        }
    }

    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = this.mHomeController.getAppWidgetHost();
        if (!(appWidgetHost == null || widgetInfo.isCustomWidget() || !widgetInfo.isWidgetIdValid())) {
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        }
        this.mHomeController.deleteItemFromDb(widgetInfo);
    }

    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindRestoreItemsChange(updates);
            }
        })) {
            this.mHomeController.updateRestoreItems(updates);
        }
    }

    public void bindShortcutsChanged(final ArrayList<IconInfo> updated, final ArrayList<IconInfo> removed, final UserHandleCompat user) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindShortcutsChanged(updated, removed, user);
            }
        })) {
            if (!(updated == null || updated.isEmpty())) {
                this.mHomeController.updateShortcuts(updated, this.mIconCache);
                this.mHomeController.notifyCapture(true);
            }
            if (removed != null && !removed.isEmpty() && user != null) {
                HashSet<ComponentName> removedComponents = new HashSet();
                HashSet<ShortcutKey> removedDeepShortcuts = new HashSet();
                Iterator it = removed.iterator();
                while (it.hasNext()) {
                    IconInfo ii = (IconInfo) it.next();
                    if (ii.itemType == 6) {
                        removedDeepShortcuts.add(ShortcutKey.fromShortcutInfo(ii));
                    } else {
                        removedComponents.add(ii.getTargetComponent());
                    }
                }
                if (!removedComponents.isEmpty()) {
                    this.mHomeController.removeItemsByComponentName(removedComponents, user);
                    this.mDragMgr.onAppsRemoved(new ArrayList(), removedComponents);
                }
                if (!removedDeepShortcuts.isEmpty()) {
                    ItemInfoMatcher matcher = ItemInfoMatcher.ofShortcutKeys(removedDeepShortcuts);
                    this.mHomeController.removeItemsByMatcher(matcher);
                    this.mDragMgr.onAppsRemoved(matcher);
                }
            }
        }
    }

    public void bindItemsRemoved(final ArrayList<ItemInfo> removed) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindItemsRemoved(removed);
            }
        }) && !removed.isEmpty()) {
            Iterator it = removed.iterator();
            while (it.hasNext()) {
                this.mHomeController.removeHomeItem((ItemInfo) it.next());
            }
        }
    }

    public void bindRemoveScreen(final int index) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindRemoveScreen(index);
            }
        })) {
            Workspace workspace = this.mWorkspace;
            if (this.mHomeController.isOverviewState() && ZeroPageController.isEnableZeroPage()) {
                index++;
            }
            workspace.removeScreenWithItem(index, true, false);
        }
    }

    public void bindAppsAdded(final ArrayList<Long> newScreens, final ArrayList<ItemInfo> addNotAnimated, final ArrayList<ItemInfo> addAnimated) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindAppsAdded(newScreens, addNotAnimated, addAnimated);
            }
        })) {
            if (newScreens != null) {
                bindAddScreens(newScreens);
            }
            if (!(addNotAnimated == null || addNotAnimated.isEmpty())) {
                bindItems(addNotAnimated, 0, addNotAnimated.size(), false);
            }
            if (!(addAnimated == null || addAnimated.isEmpty())) {
                bindItems(addAnimated, 0, addAnimated.size(), true);
            }
            this.mWorkspace.removeExtraEmptyScreen();
            this.mWorkspace.onChangeChildState();
        }
    }

    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindWidgetsRestored(widgets);
            }
        })) {
            this.mHomeController.widgetsRestored(widgets);
        }
    }

    public void bindAppsInFolderRemoved(final ArrayList<FolderInfo> folderInfos, final ArrayList<ItemInfo> removed) {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindAppsInFolderRemoved(folderInfos, removed);
            }
        }) && folderInfos != null && removed != null && !folderInfos.isEmpty() && !removed.isEmpty()) {
            HashSet<ComponentName> removedComponents = new HashSet();
            Iterator it = removed.iterator();
            while (it.hasNext()) {
                removedComponents.add(((ItemInfo) it.next()).componentName);
            }
            Utilities.removeAppsInFolder(folderInfos, removed);
            this.mDragMgr.onAppsRemoved(new ArrayList(), removedComponents);
        }
    }

    public void bindComponentsRemoved(ArrayList<String> packageNames, HashSet<ComponentName> ComponentsNames, UserHandleCompat user, int reason) {
        final ArrayList<String> arrayList = packageNames;
        final HashSet<ComponentName> hashSet = ComponentsNames;
        final UserHandleCompat userHandleCompat = user;
        final int i = reason;
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindComponentsRemoved(arrayList, hashSet, userHandleCompat, i);
            }
        })) {
            if (reason == 0) {
                if (!(packageNames == null || packageNames.isEmpty())) {
                    this.mHomeController.removeItemsByPackageName(packageNames, user);
                }
                if (!(ComponentsNames == null || ComponentsNames.isEmpty())) {
                    this.mHomeController.removeItemsByComponentName(ComponentsNames, user);
                }
                this.mDragMgr.onAppsRemoved(packageNames, ComponentsNames);
            } else {
                this.mHomeController.disableShortcutsByPackageName(packageNames, user, reason, this.mIconCache);
            }
            this.mWorkspace.onChangeChildState();
        }
    }

    public void bindFestivalPageIfNecessary() {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.bindFestivalPageIfNecessary();
            }
        }) && this.mHomeController.getFestivalPageController() != null) {
            this.mHomeController.getFestivalPageController().bindFestivalPageIfNecessary();
        }
    }

    public void initFestivalPageIfNecessary() {
        if (!this.mLauncher.waitUntilResume(new Runnable() {
            public void run() {
                HomeBindController.this.initFestivalPageIfNecessary();
            }
        }) && this.mHomeController.getFestivalPageController() != null) {
            this.mHomeController.getFestivalPageController().initFestivalPageIfNecessary();
        }
    }

    public void onPageBoundSynchronously(int page) {
        this.mSynchronouslyBoundPages.add(Integer.valueOf(page));
    }

    public boolean isWorkspaceLoading() {
        return this.mWorkspaceLoading;
    }

    public void setWorkspaceLoading(boolean value) {
        this.mWorkspaceLoading = value;
    }

    public boolean isHotseatLoading() {
        return this.mHotseatLoading;
    }

    public void removeAdvanceMessage() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
    }

    void clearWidgetsToAdvance() {
        this.mWidgetsToAdvance.clear();
    }

    private void sendAdvanceMessage(long delay) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delay);
        this.mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    public void updateAutoAdvanceState() {
        boolean autoAdvanceRunning;
        long delay = 20000;
        if (this.mLauncher.getVisible() && this.mUserPresent && !this.mWidgetsToAdvance.isEmpty()) {
            autoAdvanceRunning = true;
        } else {
            autoAdvanceRunning = false;
        }
        if (autoAdvanceRunning != this.mAutoAdvanceRunning) {
            this.mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                if (this.mAutoAdvanceTimeLeft != -1) {
                    delay = this.mAutoAdvanceTimeLeft;
                }
                sendAdvanceMessage(delay);
                return;
            }
            if (!this.mWidgetsToAdvance.isEmpty()) {
                this.mAutoAdvanceTimeLeft = Math.max(0, 20000 - (System.currentTimeMillis() - this.mAutoAdvanceSentTime));
            }
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(0);
        }
    }

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo != null && appWidgetInfo.autoAdvanceViewId != -1) {
            View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
            if (v instanceof Advanceable) {
                this.mWidgetsToAdvance.put(hostView, appWidgetInfo);
                ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
                updateAutoAdvanceState();
            }
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (this.mWidgetsToAdvance.containsKey(hostView)) {
            this.mWidgetsToAdvance.remove(hostView);
            updateAutoAdvanceState();
        }
    }

    public void setUserPresent(boolean userPresent) {
        this.mUserPresent = userPresent;
    }

    View createShortcut(IconInfo info) {
        return createShortcut((ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), info);
    }

    public View createShortcut(ViewGroup parent, IconInfo info) {
        IconView favorite;
        if (info instanceof LauncherPairAppsInfo) {
            favorite = (PairAppsIconView) this.mLauncher.getInflater().inflate(R.layout.pairapps_icon, parent, false);
        } else {
            favorite = (IconView) this.mLauncher.getInflater().inflate(R.layout.icon, parent, false);
        }
        if (info.container == -101) {
            favorite.setIconDisplay(1);
        } else {
            favorite.setIconDisplay(0);
        }
        favorite.applyFromShortcutInfo(info, this.mIconCache);
        favorite.setOnClickListener(this.mLauncher);
        return favorite;
    }

    private void sendLoadingCompleteBroadcastIfNecessary() {
        if (!this.mLauncher.getSharedPrefs().getBoolean(FIRST_LOAD_COMPLETE, false)) {
            String permission = this.mLauncher.getResources().getString(R.string.receive_first_load_broadcast_permission);
            this.mLauncher.sendBroadcast(new Intent(ACTION_FIRST_LOAD_COMPLETE), permission);
            Editor editor = this.mLauncher.getSharedPrefs().edit();
            editor.putBoolean(FIRST_LOAD_COMPLETE, true);
            editor.apply();
        }
    }

    void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
        if (launcherInfo.providerName != null) {
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_WIDGET_DELETE, launcherInfo.providerName.getPackageName(), -1, false);
        }
    }

    FolderIconView addFolder(CellLayout layout, long container, long screenId, int cellX, int cellY) {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = this.mLauncher.getText(R.string.folder_name);
        this.mHomeController.addItemToDb(folderInfo, container, screenId, cellX, cellY);
        sFolders.put(folderInfo.id, folderInfo);
        FolderIconView newFolder = FolderIconView.fromXml(this.mLauncher, layout, folderInfo, this.mHomeController, this.mLauncher, null, container == -101 ? 1 : 0);
        this.mHomeController.addInScreen(newFolder, container, screenId, cellX, cellY, 1, 1);
        layout.getCellLayoutChildren().measureChild(newFolder);
        return newFolder;
    }

    void restoreInstanceState() {
        Iterator it = this.mSynchronouslyBoundPages.iterator();
        while (it.hasNext()) {
            this.mWorkspace.restoreInstanceStateForChild(((Integer) it.next()).intValue());
        }
    }
}
