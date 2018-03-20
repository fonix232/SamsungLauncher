package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.State;

public class AppsSortOptionViewStateHandler extends AbstractStateHandler {
    public /* bridge */ /* synthetic */ boolean isAllowedInHomeOnlyMode() {
        return super.isAllowedInHomeOnlyMode();
    }

    AppsSortOptionViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        completeExecuteRequest(callback, getLauncherProxy().showAppsViewTypePopup());
    }
}
