package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsHideAppsViewStateHandler extends AbstractStateHandler {
    HomeSettingsHideAppsViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().enterHideAppsView();
        this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsHideAppsView");
        completeExecuteRequest(callback, 0);
    }
}
