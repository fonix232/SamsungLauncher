package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderViewStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    AppsFolderViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
        this.mNlgTargetState = "AppsPageView";
    }

    public String parseParameters(State state) {
        this.mAppInfo.setName(StateParamHelper.getStringParamValue(this, state.getParamMap(), "FolderName", this.mNlgTargetState));
        if (this.mAppInfo.isValid()) {
            return "PARAM_CHECK_OK";
        }
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (!getLauncherProxy().hasFolderInApps(this.mAppInfo.getName())) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "no");
            ret = 1;
        }
        if (ret == 0) {
            getLauncherProxy().openAppsFolder(this.mAppInfo.getName());
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("FolderName", "Match", "yes");
        }
        completeExecuteRequest(callback, ret);
    }
}
