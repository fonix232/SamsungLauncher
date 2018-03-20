package com.android.launcher3.widget.folder;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.controller.WidgetController;
import com.android.launcher3.widget.controller.WidgetController.FolderManager;
import com.android.launcher3.widget.controller.WidgetDragController;
import com.android.launcher3.widget.controller.WidgetState;
import com.android.launcher3.widget.controller.WidgetState.ActionListener;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.view.WidgetFolder;
import com.android.launcher3.widget.view.WidgetItemSingleView;
import com.android.launcher3.widget.view.WidgetPagedView;
import com.android.launcher3.widget.view.WidgetPagedView.Listener;
import com.sec.android.app.launcher.R;
import java.util.HashMap;

public class WidgetFolderController extends Stage implements Listener {
    private static final String RUNTIME_WIDGETFOLDER_CURRENT_SCREEN = "RUNTIME_WIDGETFOLDER_CURRENT_SCREEN";
    private static final String TAG = "WidgetFolderController";
    private ActionListener mActionListener = new ActionListener() {
        public void openFolder(View view, boolean animate) {
        }

        public void notifyChangeState(State toState) {
            WidgetFolderController.this.changeState(toState, true);
        }

        public void startDrag(View view) {
            if (WidgetFolderController.this.mDragController == null) {
                WidgetFolderController.this.mDragController = new WidgetDragController(WidgetFolderController.this.mLauncher);
            }
            if (view.getTag() instanceof PendingAddWidgetInfo) {
                PendingAddWidgetInfo info = (PendingAddWidgetInfo) view.getTag();
                LauncherAppWidgetProviderInfo providerInfo = (LauncherAppWidgetProviderInfo) info.getProviderInfo();
                info.spanX = providerInfo.getSpanX();
                info.spanY = providerInfo.getSpanY();
            }
            if (!WidgetFolderController.this.mDragController.startDrag(view)) {
                Log.d(WidgetFolderController.TAG, "fail to widget drag : " + WidgetFolderController.this.getState() + " , " + view);
            }
        }
    };
    private float mBgBlurAmount;
    private float mBgDarkenAlpha;
    private WidgetDragController mDragController;
    private FolderManager mFolderManager;
    private boolean mFromHomeSetting;
    private boolean mIsFinishOnTouchOutSide;
    private boolean mIsOnStage = false;
    private LayoutInflater mLayoutInflater;
    private WidgetPagedView mPagedView;
    private boolean mWhiteWallpaper;
    private WidgetFolder mWidgetFolder;
    private WidgetState mWidgetState;
    private final HashMap<State, WidgetState> mWidgetStateMap = new HashMap();

    private static final class WidgetStateFolder extends WidgetState {
        public WidgetStateFolder(Context context, View titleBar) {
            super(context, titleBar);
        }

        public void enter(WidgetState fromState, AnimatorSet animSet) {
        }

        public void exit(WidgetState toState, AnimatorSet animSet) {
        }

        public void onWidgetItemClick(View view) {
            if (view instanceof WidgetItemSingleView) {
                clickNotAllowed(view);
            }
        }

        public boolean onWidgetItemLongClick(View view) {
            if (!(view instanceof WidgetItemSingleView) || this.mActionListener == null) {
                return false;
            }
            this.mActionListener.startDrag(view);
            return true;
        }

        public State getState() {
            return State.FOLDER;
        }

        public boolean onBackPressed() {
            return false;
        }

        public void setFocus() {
        }
    }

    public void setup() {
    }

    public void initStageView() {
        if (!this.mViewInitiated) {
            this.mBgBlurAmount = ((float) this.mLauncher.getResources().getInteger(R.integer.config_widgetBgBlur)) / 100.0f;
            this.mBgDarkenAlpha = ((float) this.mLauncher.getResources().getInteger(R.integer.config_widgetBgDarken)) / 100.0f;
            this.mLayoutInflater = this.mLauncher.getLayoutInflater();
            this.mWidgetFolder = (WidgetFolder) this.mLayoutInflater.inflate(R.layout.widget_folder, null);
            this.mPagedView = (WidgetPagedView) this.mWidgetFolder.findViewById(R.id.widget_folder_pagedview);
            this.mPagedView.setListener(this);
            this.mWidgetState = getWidgetState(State.FOLDER);
            super.initStageView();
        }
    }

    public void onResumeActivity() {
        this.mPagedView.onResume();
    }

    public void onPauseActivity() {
        this.mPagedView.onPause();
    }

    public void onRestoreInstanceState(Bundle state) {
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(RUNTIME_WIDGETFOLDER_CURRENT_SCREEN, this.mPagedView.getCurrentPage());
    }

    public void restoreState(Bundle savedState, boolean isOnTop) {
        this.mWidgetState = getWidgetState(State.FOLDER);
        int restorePage = savedState.getInt(RUNTIME_WIDGETFOLDER_CURRENT_SCREEN, -1);
        if (restorePage != -1) {
            this.mPagedView.setRestorePage(restorePage);
        }
    }

    public void onDestroyActivity() {
    }

    public DragSource getDragSourceForLongKey() {
        return super.getDragSourceForLongKey();
    }

    protected Animator onStageEnter(StageEntry data) {
        this.mIsOnStage = true;
        this.mIsFinishOnTouchOutSide = false;
        changeColorForBg(WhiteBgManager.isWhiteBg());
        if (this.mFolderManager == null) {
            this.mFolderManager = (FolderManager) data.getExtras(WidgetController.KEY_WIDGET_FOLDER_MANAGER);
        }
        View anchorView = this.mFolderManager != null ? this.mFolderManager.getAnchorView() : null;
        this.mFromHomeSetting = ((Boolean) data.getExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(false))).booleanValue();
        return this.mWidgetFolder.open(anchorView, data.enableAnimation, data.getLayerViews());
    }

    protected Animator onStageExit(StageEntry data) {
        boolean animate = true;
        this.mIsOnStage = false;
        if (data.toStage == 3) {
            if (this.mIsFinishOnTouchOutSide) {
                SALogging.getInstance().insertCloseWidgetFolderLog("2");
            } else {
                SALogging.getInstance().insertCloseWidgetFolderLog("1");
            }
        } else if (data.toStage == 1 && data.getInternalStateTo() == 1) {
            SALogging.getInstance().insertCloseWidgetFolderLog("3");
        }
        if (!(data.enableAnimation && data.toStage == 3)) {
            animate = false;
        }
        View anchorView = null;
        if (this.mFolderManager != null) {
            anchorView = this.mFolderManager.getAnchorView();
        }
        data.putExtras(WidgetController.KEY_WIDGET_FOLDER_ICON, anchorView);
        return this.mWidgetFolder.close(anchorView, animate, data.getLayerViews());
    }

    protected Animator onStageEnterByTray() {
        return null;
    }

    protected Animator onStageExitByTray() {
        return null;
    }

    public View getContainerView() {
        return this.mWidgetFolder;
    }

    public State getState() {
        return this.mWidgetState.getState();
    }

    public void onWidgetItemClick(View v) {
        this.mWidgetState.onWidgetItemClick(v);
    }

    public boolean onWidgetItemLongClick(View v) {
        return this.mWidgetState.onWidgetItemLongClick(v);
    }

    public boolean onBackPressed() {
        StageEntry entry = new StageEntry();
        entry.putExtras(WidgetController.KEY_WIDGET_FROM_SETTING, Boolean.valueOf(this.mFromHomeSetting));
        getStageManager().finishStage((Stage) this, entry);
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

    private void changeColorForBg(boolean whiteBg) {
        this.mWhiteWallpaper = whiteBg;
        this.mWidgetFolder.changeColorForBg(whiteBg);
        this.mPagedView.changeColorForBg(this.mWhiteWallpaper);
    }

    private void changeState(State toState, boolean animate) {
        WidgetState oldWidgetState = this.mWidgetState;
        WidgetState toWidgetState = getWidgetState(toState);
        AnimatorSet animSet = null;
        if (animate) {
            animSet = new AnimatorSet();
        }
        oldWidgetState.exit(toWidgetState, animSet);
        toWidgetState.enter(oldWidgetState, animSet);
        this.mWidgetState = toWidgetState;
        if (animSet != null) {
            animSet.start();
        }
    }

    private WidgetState getWidgetState(State state) {
        WidgetState widgetState = (WidgetState) this.mWidgetStateMap.get(state);
        if (widgetState != null) {
            return widgetState;
        }
        widgetState = new WidgetStateFolder(this.mLauncher, null);
        widgetState.setActionListener(this.mActionListener);
        this.mWidgetStateMap.put(state, widgetState);
        return widgetState;
    }

    protected boolean keepInstance() {
        return false;
    }

    public void onPagedViewTouchIntercepted() {
    }

    public void onSearchResult(boolean found) {
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

    public boolean finishOnTouchOutSide() {
        this.mIsFinishOnTouchOutSide = true;
        return true;
    }

    protected boolean isRestorable() {
        return false;
    }

    public void onConfigurationChangedIfNeeded() {
        this.mWidgetFolder.onConfigurationChangedIfNeeded();
    }

    public void onPagedViewFocusUp() {
    }

    public boolean searchBarHasFocus() {
        return false;
    }

    protected void setPaddingForNavigationBarIfNeeded() {
        if (LauncherFeature.supportNavigationBar() && this.mNavigationBarPosition != Utilities.getNavigationBarPositon()) {
            this.mNavigationBarPosition = Utilities.getNavigationBarPositon();
            this.mWidgetFolder.centerAboutIcon();
        }
    }
}
