package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsSingleAppClearBadgeStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    AppsSingleAppClearBadgeStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeSingleAppClearBadge")) {
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
        if (getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            getLauncherProxy().clearBadge(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Match", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
