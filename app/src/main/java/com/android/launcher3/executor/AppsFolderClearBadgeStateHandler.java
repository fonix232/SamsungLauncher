package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderClearBadgeStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    AppsFolderClearBadgeStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeFolderClearBadge")) {
            this.mNlgTargetState = "Home";
        } else {
            this.mNlgTargetState = "AppsPageView";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mAppInfo.getItemInfo() != null) {
            getLauncherProxy().clearFolderBadge(this.mAppInfo.getItemInfo());
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
