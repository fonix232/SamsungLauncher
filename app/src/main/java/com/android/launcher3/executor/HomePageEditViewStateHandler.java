package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageEditViewStateHandler extends AbstractStateHandler {
    HomePageEditViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = getLauncherProxy().enterHomeEditView();
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString());
        completeExecuteRequest(callback, ret, 400);
    }
}
