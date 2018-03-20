package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsSingleAppAddToSecureFolderStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    AppsSingleAppAddToSecureFolderStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
        this.mNlgTargetState = stateId.toString();
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        if (!getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            this.mNlgRequestInfo = new NlgRequestInfo("AppsPageView").addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
        } else if (!getLauncherProxy().isSecureFolderSetup()) {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SecureFolder", "Setup", "no");
        } else if (getLauncherProxy().canAppAddToSecureFolder(this.mAppInfo)) {
            getLauncherProxy().addToSecureFolder(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SecureFolder", "Available", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 0;
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("AppsPageView").addScreenParam("SecureFolder", "Available", "no").addResultParam("SingleApp", this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, ret);
    }
}
