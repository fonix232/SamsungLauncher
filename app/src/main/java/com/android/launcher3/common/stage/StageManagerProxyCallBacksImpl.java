package com.android.launcher3.common.stage;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.proxy.StageManagerProxyCallbacks;

public class StageManagerProxyCallBacksImpl implements StageManagerProxyCallbacks {
    private final Launcher mActivity;
    private final StageManager mStageManager;

    public StageManagerProxyCallBacksImpl(Launcher activity, StageManager stageManager) {
        this.mActivity = activity;
        this.mStageManager = stageManager;
    }

    public void openAppsTray() {
        this.mStageManager.finishAllStage(null);
        this.mStageManager.startStage(2, null);
    }

    public void openFolder(FolderIconView folderItem) {
        this.mActivity.openFolder(folderItem);
    }

    public void closeFolder() {
        this.mStageManager.finishStage(5, null);
    }

    public int getTopStageMode() {
        return this.mStageManager.getTopStage().getMode();
    }

    public int getSecondTopStageMode() {
        if (this.mStageManager.getSecondTopStage() != null) {
            return this.mStageManager.getSecondTopStage().getMode();
        }
        return 0;
    }

    public void enterWidgetListView() {
        this.mActivity.showWidgetsView(true, true);
    }

    public void enterHideAppsView() {
        exitSettingsView();
        StageEntry data = new StageEntry();
        data.putExtras(AppsPickerController.KEY_PICKER_MODE, Integer.valueOf(1));
        this.mStageManager.startStage(6, data);
    }

    public void exitSettingsView() {
        SettingsActivity settingsActivity = LauncherAppState.getInstance().getSettingsActivity();
        if (settingsActivity != null) {
            settingsActivity.finish();
        }
    }

    public void enterFolderAddAppsView(FolderIconView folderItem) {
        StageEntry data = new StageEntry();
        data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folderItem);
        this.mStageManager.startStage(6, data);
    }

    public void goHome() {
        this.mStageManager.finishAllStage(null);
        this.mStageManager.startStage(1, null);
    }

    public void openFolderAddIconView(FolderIconView folderItem) {
        StageEntry data = new StageEntry();
        data.putExtras(AppsPickerController.KEY_PICKER_MODE, Integer.valueOf(0));
        data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, folderItem);
        this.mStageManager.startStage(6, data);
    }

    public void finishStage(int stageMode, StageEntry data) {
        this.mStageManager.finishStage(stageMode, data);
    }

    public void switchStage(int stageMode, StageEntry data) {
        this.mStageManager.switchStage(stageMode, data);
    }
}
