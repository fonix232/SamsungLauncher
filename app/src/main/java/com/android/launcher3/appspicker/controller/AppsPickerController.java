package com.android.launcher3.appspicker.controller;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.SearchView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel.OnAllAppItemListLoadCompletedListener;
import com.android.launcher3.appspicker.AppsPickerAlphabeticalAppsList;
import com.android.launcher3.appspicker.view.AppsPickerContainerView;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.proxy.AppsPickerProxyCallbacks;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.vcard.VCardConfig;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.ScreenParameter;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AppsPickerController extends Stage implements OnAllAppItemListLoadCompletedListener {
    private static final int EXIT_HIDE_APPS_DELAY = 200;
    public static final String KEY_BOUNCED_ITEM = "KEY_BOUNCED_ITEM";
    public static final String KEY_BOUNCED_ITEM_USER = "KEY_BOUNCED_ITEM_USER";
    public static final String KEY_ITEMS_TO_HIDE = "KEY_ITEMS_TO_HIDE";
    public static final String KEY_ITEMS_TO_SHOW = "KEY_ITEMS_TO_SHOW";
    public static final String KEY_PICKER_MODE = "KEY_PICKER_MODE";
    public static final String KEY_SELECTED_ITEMS = "KEY_SELECTED_ITEMS";
    public static final int MODE_FOLDER_ADD_APPS = 0;
    public static final int MODE_HIDE_APPS = 1;
    static final int REQUEST_CODE_VOICE_RECOGNITION = 601;
    private static final String TAG = "AppsPickerController";
    private AppsPickerAlphabeticalAppsList mAllApps;
    private ArrayList<IconInfo> mAppsToHideForAllApps;
    private float mBgBlurAmount;
    private float mBgDimAmount;
    private AppsPickerContainerView mContainerView;
    private DragLayer mDragLayer;
    private FolderIconView mFolderIconView;
    private int mFromStageMode;
    private boolean mNeedSearchFocus;
    private int mPickerMode;
    private SearchView mSearchView;

    public void setup() {
        this.mAppsToHideForAllApps = new ArrayList();
        this.mAllApps = new AppsPickerAlphabeticalAppsList(this.mLauncher);
        this.mBgBlurAmount = ((float) this.mLauncher.getResources().getInteger(R.integer.config_folderBgBlur)) / 100.0f;
        this.mBgDimAmount = ((float) this.mLauncher.getResources().getInteger(R.integer.config_folderBgDarken)) / 100.0f;
        LauncherAppState.getInstance().getModel().registerOnAllAppItemListLoadCompletedListener(this);
        LauncherAppState.getInstance().getLauncherProxy().setAppsPickerProxyCallbacks(new AppsPickerProxyCallbacks() {
            public boolean setSearchText(String searchText) {
                if (AppsPickerController.this.mContainerView == null) {
                    return false;
                }
                AppsPickerController.this.mContainerView.setSearchText(searchText);
                return true;
            }

            public void addResultApps() {
                if (AppsPickerController.this.mLauncher.getTopStageMode() == 6) {
                    AppsPickerController.this.addResultApps(AppsPickerController.this.mAllApps.getFilteredAppsList());
                }
            }

            public void addResultApps(int ordinal) {
                if (AppsPickerController.this.mLauncher.getTopStageMode() == 6 && AppsPickerController.this.mAllApps.getFilteredAppsList() != null) {
                    IconInfo iconInfo = (IconInfo) AppsPickerController.this.mAllApps.getFilteredAppsList().get(ordinal);
                    List<IconInfo> list = new ArrayList();
                    list.add(iconInfo);
                    AppsPickerController.this.addResultApps(list);
                }
            }

            public void addResultApps(boolean anapho) {
                if (AppsPickerController.this.mLauncher.getTopStageMode() == 6) {
                    AppsPickerController.this.addResultApps(AppsPickerController.this.mContainerView.getSelectedItems());
                }
            }

            public int getSearchResultListCheckedCount() {
                if (AppsPickerController.this.mContainerView.getSelectedItems() != null) {
                    return AppsPickerController.this.mContainerView.getSelectedItems().size();
                }
                return 0;
            }

            public int getSearchResultListCount() {
                return AppsPickerController.this.mAllApps.getFilteredAppsList().size();
            }

            public ItemInfo getSearchResultSingleAppInfo() {
                if (AppsPickerController.this.mAllApps.getFilteredAppsList().size() != 1) {
                    return null;
                }
                return (ItemInfo) AppsPickerController.this.mAllApps.getFilteredAppsList().get(0);
            }

            public ItemInfo getItem(ComponentName name) {
                for (IconInfo info : AppsPickerController.this.mAllApps.getApps()) {
                    if (name.equals(info.componentName)) {
                        return info;
                    }
                }
                return null;
            }

            public ItemInfo getItem(String title) {
                for (IconInfo info : AppsPickerController.this.mAllApps.getApps()) {
                    if (info != null && info.title != null && title.replaceAll("\\s", "").compareToIgnoreCase(info.title.toString().replaceAll("\\s", "")) == 0) {
                        return info;
                    }
                }
                return null;
            }

            public ItemInfo getItem(int index) {
                int indexOnList = index - 1;
                if (indexOnList < 0 || indexOnList >= AppsPickerController.this.mAllApps.getApps().size()) {
                    return null;
                }
                return (ItemInfo) AppsPickerController.this.mAllApps.getApps().get(indexOnList);
            }

            public boolean onParamFillingReceived(ParamFilling pf) {
                if (pf.getScreenParamMap().containsKey("Text")) {
                    setSearchText(((ScreenParameter) pf.getScreenParamMap().get("Text")).getSlotValue());
                    return true;
                } else if (!pf.getScreenParamMap().containsKey("AppName")) {
                    return false;
                } else {
                    setSearchText(((ScreenParameter) pf.getScreenParamMap().get("AppName")).getSlotValue());
                    return true;
                }
            }
        });
    }

    public void initStageView() {
        Log.v(TAG, "initStageView()");
        this.mDragLayer = this.mLauncher.getDragLayer();
        if (this.mContainerView == null) {
            this.mContainerView = (AppsPickerContainerView) this.mLauncher.getInflater().inflate(R.layout.apps_picker, null);
        }
        this.mContainerView.bindController(this, this.mAllApps);
        this.mContainerView.setSearchBarController(this.mContainerView.newAllAppsSearchBarController());
        this.mSearchView = (SearchView) this.mContainerView.findViewById(R.id.apps_picker_app_search_input);
        this.mSearchView.setImeOptions(this.mSearchView.getImeOptions() | VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        super.initStageView();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case 34:
            case 84:
                this.mSearchView.requestFocus();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    protected Animator onStageEnter(StageEntry data) {
        Log.v(TAG, "onStageEnter()");
        this.mFromStageMode = data.fromStage;
        this.mPickerMode = ((Integer) data.getExtras(KEY_PICKER_MODE, Integer.valueOf(0))).intValue();
        this.mFolderIconView = (FolderIconView) data.getExtras(FolderController.KEY_FOLDER_ICON_VIEW);
        String bouncedApp = (String) data.getExtras(KEY_BOUNCED_ITEM, null);
        UserHandle bouncedAppUser = (UserHandle) data.getExtras(KEY_BOUNCED_ITEM_USER, UserHandleCompat.myUserHandle().getUser());
        LauncherAppState.getInstance().getModel().loadAllAppItemList(this);
        notifyAppsListChanged(true);
        this.mContainerView.setPickerMode(this.mPickerMode);
        this.mContainerView.bindAdapter();
        this.mContainerView.setVisibility(4);
        if (bouncedApp != null) {
            for (IconInfo info : this.mAllApps.getApps()) {
                ComponentName cn = ComponentName.unflattenFromString(bouncedApp);
                if (cn != null && cn.equals(info.componentName) && bouncedAppUser.equals(info.getUserHandle().getUser())) {
                    this.mContainerView.setBouncedApp(cn, bouncedAppUser);
                    int row = this.mAllApps.getRowFromTitle((String) info.title);
                    this.mContainerView.setSelection(row);
                    Log.i(TAG, "row = " + row);
                    break;
                }
            }
        }
        attachViewToDragLayer();
        Animator enterAnimator = null;
        if (data.enableAnimation) {
            enterAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.enter_folder_add_apps);
            enterAnimator.setTarget(this.mContainerView);
            enterAnimator.setInterpolator(ViInterpolator.getInterploator(30));
            enterAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    AppsPickerController.this.mContainerView.setAlpha(0.0f);
                    AppsPickerController.this.mContainerView.setVisibility(View.VISIBLE);
                    AppsPickerController.this.mContainerView.setScrollIndexer();
                }

                public void onAnimationCancel(Animator animation) {
                    AppsPickerController.this.mContainerView.setAlpha(1.0f);
                }

                public void onAnimationEnd(Animator animation) {
                    AppsPickerController.this.mContainerView.setAlpha(1.0f);
                    AppsPickerController.this.mContainerView.startBounceAnimation();
                }
            });
        } else {
            this.mContainerView.setVisibility(View.VISIBLE);
            this.mContainerView.setAlpha(1.0f);
        }
        boolean isParentHome = false;
        if (this.mFolderIconView != null && this.mFolderIconView.getFolderInfo().container == -100) {
            isParentHome = true;
        }
        this.mContainerView.setParentMode(isParentHome);
        this.mContainerView.setAppsPickerViewTop(true);
        if (this.mPickerMode == 1) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(8);
        } else if (this.mPickerMode == 0 && this.mFolderIconView != null) {
            if (this.mFolderIconView.getFolderInfo().container != -102) {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_FOLDER_ADD_APPS_IN_HOME, null, -1, false);
            } else {
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_FOLDER_ADD_APPS_IN_APPS, null, -1, false);
            }
            if (isParentHome) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(13);
            } else {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(14);
            }
        }
        return enterAnimator;
    }

    protected Animator onStageExit(StageEntry data) {
        boolean pressedHomeKey = true;
        Log.v(TAG, "onStageExit()");
        if (this.mContainerView == null) {
            return null;
        }
        this.mContainerView.resetBouncedApp();
        this.mContainerView.initBounceAnimation();
        Animator exitAnimator = null;
        if (!(data.fromStage == 6 && data.toStage == 1)) {
            pressedHomeKey = false;
        }
        if (data.stageCountOnFinishAllStage > 2 || data.broughtToHome || pressedHomeKey) {
            reset();
        } else {
            exitAnimator = AnimatorInflater.loadAnimator(this.mLauncher, R.animator.exit_folder_add_apps);
            exitAnimator.setTarget(this.mContainerView);
            exitAnimator.setInterpolator(ViInterpolator.getInterploator(30));
            exitAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    AppsPickerController.this.reset();
                }
            });
        }
        this.mContainerView.setAppsPickerViewTop(false);
        this.mFolderIconView = null;
        return exitAnimator;
    }

    private void reset() {
        if (this.mContainerView != null) {
            this.mContainerView.setVisibility(View.GONE);
            this.mContainerView.reset();
        }
        if (this.mAppsToHideForAllApps != null) {
            this.mAppsToHideForAllApps.clear();
        }
        if (this.mSearchView != null) {
            this.mSearchView.clearFocus();
            this.mNeedSearchFocus = false;
        }
        detachViewFromDragLayer();
    }

    protected Animator onStageEnterByTray() {
        return null;
    }

    protected Animator onStageExitByTray() {
        return null;
    }

    public View getContainerView() {
        return this.mContainerView;
    }

    public void onDestroyActivity() {
        this.mDragLayer = null;
        if (this.mContainerView != null) {
            this.mContainerView.reset();
        }
        this.mContainerView = null;
        LauncherAppState.getInstance().getModel().unregisterOnAllAppItemListLoadCompletedListener(this.mLauncher);
    }

    public void onChangeColorForBg(boolean whiteBg) {
        if (this.mContainerView != null) {
            this.mContainerView.changeColorAndBackground();
        }
    }

    public boolean onClick(View v) {
        if (this.mContainerView == null) {
            return false;
        }
        this.mContainerView.initBounceAnimation();
        if (v.getId() != R.id.select_add_button_text) {
            return false;
        }
        Log.i(TAG, "onClick : add_button");
        if (this.mPickerMode == 1) {
            ArrayList<ItemInfo> itemsToHide = new ArrayList();
            ArrayList<ItemInfo> itemsToShow = new ArrayList();
            this.mContainerView.getItemsForHideApps(itemsToHide, itemsToShow);
            this.mContainerView.resetSearchText();
            SALogging.getInstance().insertEventLog(this.mLauncher.getResources().getString(R.string.screen_AppsPicker), this.mLauncher.getResources().getString(R.string.event_HA_HideAppsApply), (long) (itemsToHide.size() + itemsToShow.size()));
            if (itemsToHide.size() != itemsToShow.size()) {
                SALogging.getInstance().insertHideAppsLog(this.mContainerView.getSelectedItems());
            }
            StageEntry data = new StageEntry();
            data.putExtras(KEY_ITEMS_TO_HIDE, itemsToHide);
            data.putExtras(KEY_ITEMS_TO_SHOW, itemsToShow);
            if (this.mLauncher.getSecondTopStageMode() == 2 || this.mLauncher.getSecondTopStageMode() == 1) {
                this.mLauncher.getStageManager().finishStage(6, data);
            } else {
                this.mLauncher.getStageManager().finishAllStage(data);
            }
            this.mLauncher.startHomeSettingActivity();
            return true;
        } else if (this.mPickerMode != 0) {
            return false;
        } else {
            addResultApps(this.mContainerView.getSelectedItems());
            Resources res = this.mLauncher.getResources();
            if (this.mLauncher.getSecondTopStageMode() == 1) {
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_HomeFolder_AddApps), res.getString(R.string.event_FolderAddApps_Add), (long) this.mContainerView.getSelectedItems().size());
            } else if (this.mLauncher.getSecondTopStageMode() == 2) {
                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_AppsFolder_AddApps), res.getString(R.string.event_FolderAddApps_Add), (long) this.mContainerView.getSelectedItems().size());
            }
            return true;
        }
    }

    protected boolean isRestorable() {
        return false;
    }

    protected boolean onBackPressed() {
        if (this.mFromStageMode != 5 && this.mFolderIconView != null) {
            StageEntry data = new StageEntry();
            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, this.mFolderIconView);
            data.putExtras(KEY_SELECTED_ITEMS, null);
            this.mLauncher.getStageManager().switchStage(5, data);
            return true;
        } else if (this.mPickerMode != 1) {
            return false;
        } else {
            this.mLauncher.startHomeSettingActivity();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (AppsPickerController.this.mLauncher != null && AppsPickerController.this.mLauncher.getStageManager() != null) {
                        AppsPickerController.this.mLauncher.getStageManager().finishStage(6, null);
                    }
                }
            }, 200);
            return true;
        }
    }

    public void onConfigurationChangedIfNeeded() {
        if (this.mContainerView != null) {
            this.mContainerView.resetKeyboardState();
        }
        notifyAppsListChanged(false);
        notifyLayoutChanged();
    }

    public int getPickerMode() {
        return this.mPickerMode;
    }

    private void attachViewToDragLayer() {
        if (this.mContainerView.getParent() == null) {
            this.mDragLayer.addView(this.mContainerView);
        }
    }

    private void detachViewFromDragLayer() {
        if (this.mContainerView != null) {
            DragLayer parent = (DragLayer) this.mContainerView.getParent();
            if (parent != null) {
                parent.removeView(this.mContainerView);
            }
        }
    }

    public void setApps(ArrayList<IconInfo> apps) {
        this.mAllApps.setApps(apps);
        if (this.mPickerMode == 1 || this.mFolderIconView == null || this.mFolderIconView.getFolderInfo() == null) {
            setHiddenApps(null);
        } else {
            setHiddenApps(this.mFolderIconView.getFolderInfo().contents);
        }
        this.mAllApps.setHiddenApps(this.mAppsToHideForAllApps);
        this.mNeedSearchFocus = false;
        notifyAppsListChanged(true);
    }

    private boolean isTop() {
        return this.mLauncher.getTopStageMode() == 6;
    }

    public boolean isTopStage() {
        return isTop();
    }

    private void notifyAppsListChanged(boolean needCheckHiddenItem) {
        if (isTop() && this.mContainerView != null) {
            this.mContainerView.notifyAppsListChanged(needCheckHiddenItem);
        }
    }

    private void notifyLayoutChanged() {
        if (isTop() && this.mContainerView != null) {
            this.mContainerView.notifyLayoutChanged();
        }
    }

    private void setHiddenApps(ArrayList<IconInfo> appsToHide) {
        this.mAppsToHideForAllApps.clear();
        if (appsToHide != null) {
            IconInfo itemInfo;
            IconInfo appInfo;
            List<IconInfo> allAppsList = this.mAllApps.getApps();
            for (IconInfo itemInfo2 : allAppsList) {
                if (itemInfo2.isHiddenByUser()) {
                    appInfo = new IconInfo();
                    appInfo.componentName = itemInfo2.getTargetComponent();
                    appInfo.user = itemInfo2.user;
                    this.mAppsToHideForAllApps.add(appInfo);
                }
            }
            Iterator it = appsToHide.iterator();
            while (it.hasNext()) {
                itemInfo2 = (IconInfo) it.next();
                if (itemInfo2.itemType == 0 && itemInfo2.getIntent() != null) {
                    Iterator it2;
                    boolean isExist = false;
                    if (allAppsList.size() > 0) {
                        for (IconInfo allAppsInfo : allAppsList) {
                            if (itemInfo2.getTargetComponent().equals(allAppsInfo.getTargetComponent())) {
                                isExist = true;
                                break;
                            }
                        }
                    }
                    isExist = true;
                    if (isExist) {
                        appInfo = null;
                        if (itemInfo2.hidden == 0) {
                            appInfo = new IconInfo();
                            appInfo.componentName = itemInfo2.getTargetComponent();
                            appInfo.user = itemInfo2.user;
                        } else {
                            Log.w(TAG, "setHiddenApps : already hidden status or invalid info " + itemInfo2);
                        }
                        if (appInfo != null) {
                            boolean toBeFiltered = true;
                            it2 = this.mAppsToHideForAllApps.iterator();
                            while (it2.hasNext()) {
                                IconInfo info = (IconInfo) it2.next();
                                if (info.toComponentKey().equals(appInfo.toComponentKey()) && info.user.equals(appInfo.user)) {
                                    toBeFiltered = false;
                                    break;
                                }
                            }
                            if (toBeFiltered) {
                                this.mAppsToHideForAllApps.add(appInfo);
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "setHiddenApps : " + this.mAppsToHideForAllApps.size() + " items will be filtered");
        }
        this.mAllApps.setHiddenApps(this.mAppsToHideForAllApps);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String query = null;
        if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && resultCode == -1) {
            ArrayList<String> matches = data.getStringArrayListExtra("android.speech.extra.RESULTS");
            if (matches != null) {
                query = (String) matches.get(0);
            }
        }
        this.mContainerView.onVoiceSearch(query);
    }

    protected int getInternalState() {
        return 0;
    }

    protected boolean supportStatusBarForState(int internalState) {
        return false;
    }

    protected boolean supportNavigationBarForState(int internalState) {
        return true;
    }

    protected float getBackgroundBlurAmountForState(int internalState) {
        return (this.mDragLayer.getBackgroundImageAlpha() > 0.0f ? 1 : (this.mDragLayer.getBackgroundImageAlpha() == 0.0f ? 0 : -1)) > 0 ? BlurUtils.getMaxBlurAmount() : this.mBgBlurAmount;
    }

    protected float getBackgroundDimAlphaForState(int internalState) {
        if (this.mDragLayer.getBackgroundImageAlpha() > 0.0f) {
            return 0.0f;
        }
        return this.mBgDimAmount;
    }

    protected float getBackgroundImageAlphaForState(int internalState) {
        return -1.0f;
    }

    public void onAllAppItemListLoadCompleted(ArrayList<IconInfo> appItems) {
        if (isTop()) {
            setRestoredHiddenItems();
            setApps(appItems);
            this.mContainerView.clearRestoredHiddenItems();
        }
    }

    private void setRestoredHiddenItems() {
        List<IconInfo> oldItems = this.mAllApps.getApps();
        HashMap<IconInfo, Boolean> restoredItems = this.mContainerView.getRestoredHiddenItems();
        ArrayList<IconInfo> selectedItems = this.mContainerView.getSelectedItems();
        if (selectedItems != null && restoredItems != null) {
            restoredItems.clear();
            for (IconInfo info : oldItems) {
                if (selectedItems.contains(info)) {
                    restoredItems.put(info, Boolean.valueOf(true));
                } else {
                    restoredItems.put(info, Boolean.valueOf(false));
                }
            }
        }
    }

    private void addResultApps(List<IconInfo> appList) {
        boolean finishStage;
        ArrayList<IconInfo> cloneItems = new ArrayList();
        StageEntry data = new StageEntry();
        if (this.mFolderIconView == null || this.mFolderIconView.getFolderInfo().container == -102 || LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            data.putExtras(KEY_SELECTED_ITEMS, appList);
        } else {
            for (int i = 0; i < appList.size(); i++) {
                IconInfo info = ((IconInfo) appList.get(i)).makeCloneInfo();
                info.id = FavoritesProvider.getInstance().generateNewItemId();
                info.container = this.mFolderIconView.getFolderInfo().id;
                cloneItems.add(info);
            }
            data.putExtras(KEY_SELECTED_ITEMS, cloneItems);
        }
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_FOLDER_ADD_MULTIPLE_APPS, String.valueOf(appList.size()), -1, false);
        if (this.mFromStageMode == 5) {
            finishStage = true;
        } else {
            finishStage = false;
        }
        if (finishStage) {
            this.mLauncher.getStageManager().finishStage((Stage) this, data);
        } else if (this.mFolderIconView != null) {
            data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, this.mFolderIconView);
            this.mLauncher.getStageManager().switchStage(5, data);
        } else {
            this.mLauncher.getStageManager().finishAllStage(data);
            this.mLauncher.startHomeSettingActivity();
        }
    }

    public boolean isWhiteBg() {
        return WhiteBgManager.isWhiteBg() && this.mDragLayer.getBackgroundImageAlpha() <= 0.0f;
    }

    protected int getSupportSoftInputParam(Window window) {
        return window.getAttributes().softInputMode & -17;
    }

    public boolean searchBarHasFocus() {
        return this.mSearchView != null && this.mSearchView.hasFocus();
    }

    public void onPauseActivity() {
        super.onPauseActivity();
        if (isTop() && searchBarHasFocus()) {
            this.mSearchView.clearFocus();
            this.mNeedSearchFocus = true;
        }
    }

    public void onResumeActivity() {
        super.onResumeActivity();
        if (this.mNeedSearchFocus) {
            this.mSearchView.requestFocus();
            this.mNeedSearchFocus = false;
        }
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        if (this.mContainerView != null) {
            super.setPaddingForNavigationBarIfNeeded(this.mContainerView, null);
        }
    }
}
