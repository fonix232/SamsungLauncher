package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;

class HomeFolderPageMoveStateHandler extends HomePageMoveStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeFolderPageMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId == ExecutorState.HOME_FOLDER_PAGE_MOVE) {
            this.mNlgTargetState = ExecutorState.HOME_FOLDER_VIEW.toString();
        } else {
            this.mNlgTargetState = ExecutorState.APPS_FOLDER_VIEW.toString();
        }
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mPage < 0 || getLauncherProxy().isFolderValidPage(this.mPage)) {
            getLauncherProxy().moveFolderPage(this.mPage, this.mPageDirection);
            if (this.mStateId == ExecutorState.APPS_FOLDER_PAGE_MOVE) {
                this.mNlgTargetState = this.mStateId.toString();
            }
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "no").addResultParam("FolderName", this.mAppInfo.getName()).addResultParam("page_count", String.valueOf(getLauncherProxy().getOpenedFolderPageCount()));
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
