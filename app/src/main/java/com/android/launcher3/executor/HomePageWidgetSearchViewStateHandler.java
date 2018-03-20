package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.State;

class HomePageWidgetSearchViewStateHandler extends AbstractStateHandler {
    HomePageWidgetSearchViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().enterWidgetSearchState();
        completeExecuteRequest(callback, 0);
    }
}
