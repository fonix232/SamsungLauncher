package com.android.launcher3.widget.controller;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.proxy.WidgetProxyCallbacks;
import com.android.launcher3.util.UninstallAppUtils;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.controller.WidgetState.StateActionListener;
import com.android.launcher3.widget.model.WidgetLoader;
import com.android.launcher3.widget.model.WidgetLoader.LoadListener;
import com.android.launcher3.widget.view.WidgetItemView;
import com.android.launcher3.widget.view.WidgetListPagedView;
import com.android.launcher3.widget.view.WidgetPagedView;
import com.android.launcher3.widget.view.WidgetPagedView.Filter;
import com.android.launcher3.widget.view.WidgetPagedView.Listener;
import com.android.launcher3.widget.view.WidgetSearchbar;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.ScreenParameter;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WidgetController extends Stage implements Listener {
    public static final String KEY_WIDGET_FOLDER_ICON = "KEY_WIDGET_FOLDER_ICON";
    public static final String KEY_WIDGET_FOLDER_MANAGER = "KEY_WIDGET_FOLDER_MANAGER";
    public static final String KEY_WIDGET_FROM_SETTING = "KEY_WIDGET_FROM_SETTING";
    public static final int REQUEST_CODE_VOICE_RECOGNITION = 301;
    private static final String TAG = "WidgetController";
    private float mBgBlurAmount;
    private float mBgDarkenAlpha;
    private long mDataVersion = -1;
    private WidgetDragController mDragController;
    private boolean mFromHomeSetting = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsOnStage = false;
    private final LoadListener mLoadingListener = new LoadListener() {
        public void onLoadComplete() {
            WidgetController.this.mHandler.post(new Runnable() {
                public void run() {
                    if (WidgetController.this.mIsOnStage) {
                        boolean invalidate = WidgetController.this.updateWidgetItems();
                        if (invalidate) {
                            WidgetController.this.mPagedView.invalidateWigetItems();
                        }
                        Log.d(WidgetController.TAG, "onLoadComplete : true ," + invalidate);
                        return;
                    }
                    Log.d(WidgetController.TAG, "onLoadComplete : false");
                }
            });
        }
    };
    private TextView mNoResultText;
    private LinearLayout mNoResultView;
    private final OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {
        public void onGlobalLayout() {
            Rect r = new Rect();
            WidgetController.this.mNoResultView.getWindowVisibleDisplayFrame(r);
            LayoutParams lp = WidgetController.this.mNoResultView.getLayoutParams();
            if (lp.height != r.bottom) {
                lp.height = r.bottom;
                WidgetController.this.mNoResultView.setLayoutParams(lp);
            }
        }
    };
    private WidgetPagedView mPagedView;
    private final OnKeyListener mSearchTextViewKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == 1 && ((WidgetController.this.getState() == State.NORMAL || WidgetController.this.getState() == State.SEARCH) && keyCode != 67)) {
                WidgetController.this.changeState(State.SEARCH, true);
                if (WidgetController.this.mWidgetSearchbar != null) {
                    if (keyCode == 23) {
                        WidgetController.this.mWidgetSearchbar.openKeyboard(true);
                    } else {
                        WidgetController.this.mWidgetSearchbar.openKeyboard();
                    }
                }
            }
            return false;
        }
    };
    private final OnTouchListener mSearchTextViewTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == 1 && (WidgetController.this.getState() == State.NORMAL || WidgetController.this.getState() == State.SEARCH)) {
                WidgetController.this.changeState(State.SEARCH, true);
            }
            return false;
        }
    };
    private final StateActionListener mStateActionListener = new StateActionListener() {
        public void openFolder(View view, boolean animate) {
            WidgetController.this.openFolder(view, animate);
        }

        public void notifyChangeState(State toState) {
            WidgetController.this.changeState(toState, true);
        }

        public void startDrag(View view) {
            if (WidgetController.this.mDragController == null) {
                WidgetController.this.mDragController = new WidgetDragController(WidgetController.this.mLauncher);
            }
            if (WidgetController.this.mDragController.startDrag(view)) {
                SALogging.getInstance().insertAddWidgetItemLog((PendingAddItemInfo) view.getTag());
            } else {
                Log.d(WidgetController.TAG, "fail to widget drag : " + WidgetController.this.getState() + " , " + view);
            }
        }

        public void setSearchString(String searchString) {
            WidgetController.this.mPagedView.setSearchString(searchString);
        }

        public void setSearchFilter(Filter filter) {
            WidgetController.this.mPagedView.setSearchFilter(filter);
        }

        public void applySearchResult(String searchString) {
            WidgetController.this.mPagedView.applySearchResult(searchString);
        }
    };
    private WidgetTransitAnimation mTransitAnimation;
    private boolean mWhiteWallpaper = false;
    private WidgetLoader mWidgetLoader;
    private WidgetSearchbar mWidgetSearchbar;
    private WidgetState mWidgetState;
    private final HashMap<State, WidgetState> mWidgetStateMap = new HashMap();
    private FrameLayout mWidgetView;

    public class FolderManager {
        private final PendingAddItemInfo mInfo;

        FolderManager(PendingAddItemInfo info) {
            this.mInfo = info;
        }

        public WidgetItemView getAnchorView() {
            return WidgetController.this.mPagedView.findItemView(this.mInfo);
        }
    }

    public void setup() {
        this.mWidgetLoader = this.mLauncher.getLauncherModel().getWidgetsLoader();
        this.mWidgetLoader.setLoadListener(this.mLoadingListener);
        LauncherAppState.getInstance().getLauncherProxy().setWidgetProxyCallbacks(new WidgetProxyCallbacks() {
            public void enterSearchState() {
                WidgetController.this.mStateActionListener.notifyChangeState(State.SEARCH);
            }

            public void enterUninstallState() {
                WidgetController.this.mStateActionListener.notifyChangeState(State.UNINSTALL);
            }

            public int search(String keyword) {
                WidgetController.this.mWidgetState.onVoiceSearch(keyword);
                if (WidgetController.this.mPagedView.getDisplayItemCount() == 1) {
                    Object widget = WidgetController.this.mPagedView.getDisplayItem(0);
                    if (widget instanceof ArrayList) {
                        return ((ArrayList) widget).size();
                    }
                }
                return WidgetController.this.mPagedView.getDisplayItemCount();
            }

            public boolean onParamFillingReceived(ParamFilling pf) {
                if (!pf.getScreenParamMap().containsKey("Text")) {
                    return false;
                }
                WidgetController.this.mWidgetState.onVoiceSearch(((ScreenParameter) pf.getScreenParamMap().get("Text")).getSlotValue());
                return true;
            }

            public PendingAddItemInfo getWidgetResultItem() {
                if (WidgetController.this.mPagedView.getDisplayItemCount() == 1) {
                    ArrayList<?> widget = WidgetController.this.mPagedView.getDisplayItem(0);
                    if (widget instanceof ArrayList) {
                        ArrayList<?> arrItem = widget;
                        if (arrItem.size() == 1) {
                            return (PendingAddItemInfo) arrItem.get(0);
                        }
                    }
                }
                return null;
            }

            public void uninstallWidget(PendingAddItemInfo info) {
                if (info != null && info.uninstallable(WidgetController.this.mLauncher)) {
                    UninstallAppUtils.startUninstallActivity(WidgetController.this.mLauncher, info.user, info.componentName);
                }
            }

            public ArrayList<ItemInfo> getWidgetItemsInfoByTitle(String itemTitle) {
                ArrayList findItems = new ArrayList();
                List<Object> wItems = WidgetController.this.mWidgetLoader.getWidgetItems();
                if (wItems != null) {
                    for (Object l : wItems) {
                        if (l instanceof ArrayList) {
                            findWidgetItemInList((ArrayList) l, itemTitle, findItems);
                        }
                    }
                }
                return findItems;
            }

            public ArrayList<ItemInfo> getWidgetItemsInfoByComponentName(ComponentName componentName) {
                ArrayList findItems = new ArrayList();
                List<Object> wItems = WidgetController.this.mWidgetLoader.getWidgetItems();
                if (wItems != null) {
                    for (Object l : wItems) {
                        if (l instanceof ArrayList) {
                            findWidgetItemInList((ArrayList) l, componentName, findItems);
                        }
                    }
                }
                return findItems;
            }

            private void findWidgetItemInList(ArrayList<ItemInfo> list, String itemTitle, ArrayList<ItemInfo> resultList) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    ItemInfo i = (ItemInfo) it.next();
                    if (i instanceof PendingAddWidgetInfo) {
                        PendingAddWidgetInfo pWidget = (PendingAddWidgetInfo) i;
                        String widgetName = null;
                        if (pWidget.mLabel != null) {
                            widgetName = pWidget.mLabel.toLowerCase();
                        } else if (pWidget.getApplicationLabel() != null) {
                            widgetName = pWidget.getApplicationLabel().toLowerCase();
                        }
                        if (widgetName != null && widgetName.contains(itemTitle.toLowerCase())) {
                            resultList.add(i);
                        }
                    }
                }
            }

            private void findWidgetItemInList(ArrayList<ItemInfo> list, ComponentName cn, ArrayList<ItemInfo> resultList) {
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    ItemInfo i = (ItemInfo) it.next();
                    if (i instanceof PendingAddWidgetInfo) {
                        PendingAddWidgetInfo pWidget = (PendingAddWidgetInfo) i;
                        if (cn != null && pWidget.componentName.flattenToShortString().equals(cn.flattenToShortString())) {
                            resultList.add(i);
                        }
                    }
                }
            }
        });
    }

    public void initStageView() {
        if (!this.mViewInitiated) {
            this.mBgBlurAmount = ((float) this.mLauncher.getResources().getInteger(R.integer.config_widgetBgBlur)) / 100.0f;
            this.mBgDarkenAlpha = ((float) this.mLauncher.getResources().getInteger(R.integer.config_widgetBgDarken)) / 100.0f;
            this.mWidgetView = (FrameLayout) this.mLauncher.findViewById(R.id.widget_view);
            this.mPagedView = (WidgetPagedView) this.mLauncher.findViewById(R.id.widget_paged_view);
            this.mNoResultView = (LinearLayout) this.mLauncher.findViewById(R.id.widget_search_no_result_view);
            this.mNoResultText = (TextView) this.mLauncher.findViewById(R.id.widget_search_no_result_text);
            this.mPagedView.setListener(this);
            this.mTransitAnimation = new WidgetTransitAnimation(this.mWidgetView);
            updateWidgetPagedView();
            super.initStageView();
        }
    }

    private void updateWidgetPagedView() {
        if (this.mPagedView instanceof WidgetListPagedView) {
            ((WidgetListPagedView) this.mPagedView).updateWidgetPagedView();
        }
    }

    public void onResumeActivity() {
        this.mPagedView.onResume();
    }

    public void onPauseActivity() {
        this.mPagedView.onPause();
    }

    public void onDestroyActivity() {
        this.mWidgetLoader.removeLoadListener(this.mLoadingListener);
    }

    protected Animator onStageEnter(StageEntry data) {
        this.mIsOnStage = true;
        this.mFromHomeSetting = ((Boolean) data.getExtras(KEY_WIDGET_FROM_SETTING, Boolean.valueOf(false))).booleanValue();
        if (this.mWidgetState == null || data.fromStage == 1) {
            changeState(State.NORMAL, false);
        } else {
            changeState(this.mWidgetState.getState(), false);
        }
        this.mWidgetState.onStageEnter();
        changeColorForBg(WhiteBgManager.isWhiteBg());
        this.mPagedView.updateCellSpan();
        setPreDrawListener();
        if (this.mWidgetSearchbar != null) {
            this.mWidgetSearchbar.setOnSearchTextViewKeyListener(this.mSearchTextViewKeyListener);
            this.mWidgetSearchbar.setOnSearchTextViewTouchListener(this.mSearchTextViewTouchListener);
        }
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(7);
        return createAnimationOnEnter(data.enableAnimation, data);
    }

    protected Animator onStageExit(StageEntry data) {
        boolean animate;
        boolean widgetDrag;
        boolean z = true;
        this.mIsOnStage = false;
        updateNoSearchResultView(false);
        if (this.mWidgetState != null) {
            this.mWidgetState.onStageExit();
        }
        this.mPagedView.prepareInOut(0, false);
        int toStage = data.toStage;
        if (!data.enableAnimation || data.broughtToHome) {
            animate = false;
        } else {
            animate = true;
        }
        if (data.toStage == 1 && data.getInternalStateTo() == 2) {
            widgetDrag = true;
        } else {
            widgetDrag = false;
        }
        if (toStage == 1) {
            if (data.getInternalStateTo() == 0) {
                SALogging.getInstance().insertCloseWidgetLog("1");
            } else if (data.getInternalStateTo() == 1) {
                SALogging.getInstance().insertCloseWidgetLog("2");
            }
        }
        if (this.mWidgetSearchbar != null) {
            this.mWidgetSearchbar.setOnSearchTextViewKeyListener(null);
            this.mWidgetSearchbar.setOnSearchTextViewTouchListener(null);
        }
        Log.d(TAG, "animate : " + animate + " , " + widgetDrag);
        if (!animate || widgetDrag) {
            z = false;
        }
        return createAnimationOnExit(z, data);
    }

    protected Animator onStageEnterByTray() {
        return null;
    }

    protected Animator onStageExitByTray() {
        return null;
    }

    public View getContainerView() {
        return this.mWidgetView;
    }

    protected void onStageMovingToInitial(StageEntry data) {
        if (data.toStage == 1) {
            this.mFromHomeSetting = false;
        }
        this.mWidgetView.setVisibility(View.GONE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String query = null;
        if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && resultCode == -1) {
            ArrayList<String> matches = data.getStringArrayListExtra("android.speech.extra.RESULTS");
            if (matches != null) {
                query = (String) matches.get(0);
            }
        }
        this.mWidgetState.onVoiceSearch(query);
    }

    protected int getInternalState() {
        return getState().ordinal();
    }

    protected boolean supportStatusBarForState(int internalState) {
        return true;
    }

    protected boolean supportNavigationBarForState(int internalState) {
        return true;
    }

    protected float getBackgroundBlurAmountForState(int internalState) {
        return this.mBgBlurAmount;
    }

    protected float getBackgroundDimAlphaForState(int internalState) {
        return this.mBgDarkenAlpha;
    }

    protected float getBackgroundImageAlphaForState(int internalState) {
        return 0.0f;
    }

    private boolean updateWidgetItems() {
        boolean invalidate;
        long dataVersion = this.mWidgetLoader.getDataVersion();
        if (this.mDataVersion < dataVersion) {
            invalidate = true;
        } else {
            invalidate = false;
        }
        List<Object> items = this.mWidgetLoader.getWidgetItems();
        if (invalidate) {
            if (items == null || items.isEmpty()) {
                this.mWidgetLoader.notifyDirty(null, null, true);
                Log.w(TAG, "notifyDirty because no items, dataVersion 1 : " + dataVersion + " , " + items);
                return false;
            }
            this.mPagedView.setWidgetItems(this.mWidgetLoader.getWidgetItems());
            this.mDataVersion = dataVersion;
            getWidgetState(State.NORMAL).setHasInstallableApp(this.mWidgetLoader.hasUninstallApps());
        } else if (dataVersion == -1 || items == null || items.isEmpty()) {
            Log.w(TAG, "notifyDirty because no items, dataVersion 2 : " + dataVersion + " , " + items);
            this.mWidgetLoader.notifyDirty(null, null, true);
            return false;
        }
        return invalidate;
    }

    private void setPreDrawListener() {
        this.mWidgetView.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            public boolean onPreDraw() {
                WidgetController.this.mPagedView.prepareInOut(1, WidgetController.this.updateWidgetItems());
                WidgetController.this.mWidgetView.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
    }

    private void updateNoSearchResultView(boolean isVisible) {
        int visible = isVisible ? 0 : 4;
        Window window = this.mLauncher.getWindow();
        int softInputParam = window.getAttributes().softInputMode;
        if (this.mNoResultView.getVisibility() != visible) {
            if (visible == 0) {
                softInputParam &= -17;
                this.mNoResultView.getViewTreeObserver().addOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
            } else {
                softInputParam |= 16;
                this.mNoResultView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
            }
            updateSoftInputParam(window, softInputParam);
            this.mNoResultView.setVisibility(visible);
        }
    }

    public State getState() {
        return this.mWidgetState != null ? this.mWidgetState.getState() : State.NONE;
    }

    public void onWidgetItemClick(View v) {
        if (this.mIsOnStage) {
            this.mWidgetState.onWidgetItemClick(v);
        }
    }

    public boolean onWidgetItemLongClick(View v) {
        return this.mWidgetState.onWidgetItemLongClick(v);
    }

    public void onPagedViewTouchIntercepted() {
        this.mWidgetState.onPagedViewTouchIntercepted();
    }

    public void onSearchResult(boolean found) {
        updateNoSearchResultView(!found);
    }

    public void onPagedViewFocusUp() {
        this.mWidgetState.setFocus();
    }

    public boolean onBackPressed() {
        if (!this.mFromHomeSetting) {
            return this.mWidgetState.onBackPressed();
        }
        this.mLauncher.startHomeSettingActivity();
        this.mFromHomeSetting = false;
        StageEntry data = new StageEntry();
        data.putExtras(KEY_WIDGET_FROM_SETTING, Boolean.valueOf(true));
        getStageManager().finishStage((Stage) this, data);
        return true;
    }

    public void onChangeColorForBg(boolean whiteBg) {
        if (this.mIsOnStage) {
            changeColorForBg(whiteBg);
        }
    }

    public boolean isWhiteWallpaper() {
        return this.mWhiteWallpaper;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 34:
                    if (event.isCtrlPressed()) {
                        changeState(State.SEARCH, false);
                        this.mWidgetState.setFocusToSearchEditText();
                        return true;
                    }
                    break;
                case 82:
                    return this.mWidgetState.showPopupMenu();
                case 84:
                    changeState(State.SEARCH, false);
                    this.mWidgetState.setFocusToSearchEditText();
                    return true;
            }
        }
        return false;
    }

    private void changeColorForBg(boolean whiteBg) {
        this.mWhiteWallpaper = whiteBg;
        this.mWidgetState.changeColorForBg(this.mWhiteWallpaper);
        this.mPagedView.changeColorForBg(this.mWhiteWallpaper);
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mNoResultText, whiteBg);
    }

    private void changeState(State toState, boolean animate) {
        if (this.mWidgetState == null || this.mWidgetState.getState() != toState) {
            WidgetState oldWidgetState = this.mWidgetState;
            WidgetState toWidgetState = getWidgetState(toState);
            AnimatorSet animSet = null;
            if (animate) {
                animSet = new AnimatorSet();
            }
            if (oldWidgetState != null) {
                oldWidgetState.exit(toWidgetState, animSet);
            }
            toWidgetState.enter(oldWidgetState, animSet);
            this.mPagedView.notifyChangeState(toWidgetState.getState(), null);
            this.mWidgetState = toWidgetState;
            if (animSet != null) {
                animSet.start();
            }
            if (toState == State.SEARCH) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(16);
            } else if (toState == State.NORMAL) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(7);
            }
        }
    }

    private WidgetState getWidgetState(State state) {
        WidgetState widgetState = (WidgetState) this.mWidgetStateMap.get(state);
        if (widgetState != null) {
            return widgetState;
        }
        View titleBar;
        if (state == State.SEARCH) {
            titleBar = this.mWidgetView.findViewById(R.id.widget_searchbar_container);
            widgetState = new WidgetStateSearch(this.mLauncher, titleBar);
        } else if (state == State.UNINSTALL) {
            titleBar = this.mWidgetView.findViewById(R.id.widget_uninstall_title_bar);
            widgetState = new WidgetStateUninstall(this.mLauncher, titleBar);
        } else {
            titleBar = this.mWidgetView.findViewById(R.id.widget_searchbar_container);
            widgetState = new WidgetStateNormal(this.mLauncher, titleBar);
        }
        if (this.mWidgetSearchbar == null && (titleBar instanceof WidgetSearchbar)) {
            this.mWidgetSearchbar = (WidgetSearchbar) titleBar;
        }
        widgetState.setActionListener(this.mStateActionListener);
        this.mWidgetStateMap.put(state, widgetState);
        return widgetState;
    }

    private Animator createAnimationOnEnter(boolean animated, StageEntry data) {
        this.mWidgetView.post(new Runnable() {
            public void run() {
                WidgetController.this.getContainerView().requestFocus();
            }
        });
        Animator anim = this.mTransitAnimation.getEnterWidgetAnimation(animated, data);
        getContainerView().bringToFront();
        return anim;
    }

    private Animator createAnimationOnExit(boolean animated, StageEntry data) {
        return this.mTransitAnimation.getExitWidgetAnimation(animated, data);
    }

    private void openFolder(View view, boolean animate) {
        if (view instanceof WidgetItemView) {
            StageEntry data = new StageEntry();
            data.putExtras(KEY_WIDGET_FOLDER_ICON, view);
            data.putExtras(KEY_WIDGET_FOLDER_MANAGER, new FolderManager((PendingAddItemInfo) view.getTag()));
            data.putExtras(KEY_WIDGET_FROM_SETTING, Boolean.valueOf(this.mFromHomeSetting));
            data.enableAnimation = animate;
            getStageManager().startStage(4, data);
        }
    }

    public void setDataWithOutStageChange(StageEntry data) {
        if (data != null) {
            this.mFromHomeSetting = ((Boolean) data.getExtras(KEY_WIDGET_FROM_SETTING, Boolean.valueOf(false))).booleanValue();
            changeState(State.NORMAL, true);
        }
    }

    public void onConfigurationChangedIfNeeded() {
        getWidgetState(State.NORMAL).onConfigurationChangedIfNeeded();
        getWidgetState(State.UNINSTALL).onConfigurationChangedIfNeeded();
        this.mPagedView.onConfigurationChangedIfNeeded();
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        List<CharSequence> text = event.getText();
        text.clear();
        text.add(this.mLauncher.getResources().getString(R.string.widget_button_text));
        return true;
    }

    public boolean searchBarHasFocus() {
        return this.mWidgetSearchbar != null && this.mWidgetSearchbar.hasFocus();
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        super.setPaddingForNavigationBarIfNeeded(this.mWidgetView, null);
    }
}
