package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsAppsGridSettingViewStateHandler extends AbstractAppsStateHandler {
    HomeSettingsAppsGridSettingViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS.toString()).addScreenParam("EasyMode", "On", "yes");
            completeExecuteRequest(callback, 1);
            return;
        }
        getLauncherProxy().enterHomeSettingAppsGridSettingView();
        completeExecuteRequest(callback, 0);
    }
}
