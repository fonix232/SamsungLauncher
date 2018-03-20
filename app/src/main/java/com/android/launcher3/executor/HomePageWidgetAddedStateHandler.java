package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageWidgetAddedStateHandler extends AbstractStateHandler {
    HomePageWidgetAddedStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().addWidgetResultItemToHome() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Text", "Match", "yes");
            completeExecuteRequest(callback, 0, 0);
            return;
        }
        completeExecuteRequest(callback, 1, 0);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }
}
