package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsTidyUpPagesStateHandler extends AbstractAppsStateHandler {
    AppsTidyUpPagesStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = getLauncherProxy().appsTidyUpPages();
        this.mNlgRequestInfo = new NlgRequestInfo("AppsTidyUpPagesPreviewView");
        completeExecuteRequest(callback, ret);
    }
}
