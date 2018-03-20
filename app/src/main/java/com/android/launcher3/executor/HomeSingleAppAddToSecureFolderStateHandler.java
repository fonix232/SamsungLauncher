package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSingleAppAddToSecureFolderStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeSingleAppAddToSecureFolderStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeSingleAppAddToSecureFolder")) {
            this.mNlgTargetState = "HomeSingleAppAddToSecureFolder";
        } else {
            this.mNlgTargetState = "AppsSingleAppAddToSecureFolder";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            getLauncherProxy().addToSecureFolder(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo("Root").addScreenParam("SingleApp", "Match", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SecureFolder", "On", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
