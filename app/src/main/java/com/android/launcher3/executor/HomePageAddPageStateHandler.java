package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageAddPageStateHandler extends AbstractStateHandler {
    HomePageAddPageStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().addNewHomePageInOverViewMode();
        getLauncherProxy().goHome();
        this.mNlgRequestInfo = new NlgRequestInfo("Home");
        completeExecuteRequest(callback, 0);
    }
}
