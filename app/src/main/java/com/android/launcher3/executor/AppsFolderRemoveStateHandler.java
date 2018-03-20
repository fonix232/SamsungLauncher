package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderRemoveStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    AppsFolderRemoveStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
        this.mNlgTargetState = "AppsFolderRemovePopup";
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().showAppsFolderRemovePopUp(this.mAppInfo);
        this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "yes").addResultParam("FolderName", this.mAppInfo.getName());
        completeExecuteRequest(callback, 0);
    }
}
