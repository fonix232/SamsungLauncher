package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsContactUsStateHandler extends AbstractAppsStateHandler {
    AppsContactUsStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().contactUs();
        this.mNlgRequestInfo = new NlgRequestInfo("AppsContactUs");
        completeExecuteRequest(callback, 0);
    }
}
