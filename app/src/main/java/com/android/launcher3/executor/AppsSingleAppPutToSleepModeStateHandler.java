package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

class AppsSingleAppPutToSleepModeStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    AppsSingleAppPutToSleepModeStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
        this.mNlgTargetState = "AppsPageView";
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (!getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 1;
        } else if (getLauncherProxy().isAlreadySleepMode(this.mAppInfo)) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PutToSleep", "AlreadySet", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 1;
        } else if (getLauncherProxy().isAvailableSleepMode(this.mAppInfo)) {
            getLauncherProxy().putAppToSleep(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo("AppsSingleAppPutToSleepPopUp").addScreenParam("PutToSleep", "Available", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PutToSleep", "Available", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
