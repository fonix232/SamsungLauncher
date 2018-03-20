package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSingleAppRemoveStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeSingleAppRemoveStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().isHomeOnlyMode()) {
            ret = 1;
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("HomeAndAppsScreen", "AlreadySet", "no");
        } else if (this.mAppInfo.getItemInfo() == null) {
            ret = 1;
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            getLauncherProxy().removeHomeShortcut(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, ret);
    }
}
