package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsStyleSettingViewStateHandler extends AbstractStateHandler {
    HomeSettingsStyleSettingViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsView").addScreenParam("EasyMode", "On", "yes");
            ret = 1;
        } else {
            getLauncherProxy().enterHomeSettingModeChangeView();
        }
        completeExecuteRequest(callback, ret);
    }
}
