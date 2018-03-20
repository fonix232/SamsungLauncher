package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSingleAppPutToSleepModeStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeSingleAppPutToSleepModeStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeSingleAppPutToSleepMode")) {
            this.mNlgTargetState = "HomeSingleAppPutToSleepMode";
        } else {
            this.mNlgTargetState = "AppsSingleAppPutToSleepMode";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            getLauncherProxy().putAppToSleep(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleApp", "Match", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
