package com.android.launcher3.proxy;

import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.folder.view.FolderIconView;

public interface StageManagerProxyCallbacks {
    void closeFolder();

    void enterFolderAddAppsView(FolderIconView folderIconView);

    void enterHideAppsView();

    void enterWidgetListView();

    void exitSettingsView();

    void finishStage(int i, StageEntry stageEntry);

    int getSecondTopStageMode();

    int getTopStageMode();

    void goHome();

    void openAppsTray();

    void openFolder(FolderIconView folderIconView);

    void openFolderAddIconView(FolderIconView folderIconView);

    void switchStage(int i, StageEntry stageEntry);
}
