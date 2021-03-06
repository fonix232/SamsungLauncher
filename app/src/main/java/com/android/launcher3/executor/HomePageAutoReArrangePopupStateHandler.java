package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.State;

class HomePageAutoReArrangePopupStateHandler extends AbstractStateHandler {
    HomePageAutoReArrangePopupStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        completeExecuteRequest(callback, 0, 0);
    }
}
