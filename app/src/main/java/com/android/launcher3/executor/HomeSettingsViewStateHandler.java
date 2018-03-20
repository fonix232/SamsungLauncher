package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsViewStateHandler extends AbstractStateHandler {
    HomeSettingsViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = getLauncherProxy().enterHomeSettingView();
        this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsView");
        completeExecuteRequest(callback, ret, 100);
    }
}
