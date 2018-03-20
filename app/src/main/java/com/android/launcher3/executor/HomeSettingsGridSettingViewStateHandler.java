package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsGridSettingViewStateHandler extends AbstractStateHandler {
    HomeSettingsGridSettingViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS.toString()).addScreenParam("EasyMode", "On", "yes");
            ret = 1;
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_CHANGE_GRID.toString());
            getLauncherProxy().enterHomeSettingHomeGridSettingView();
        }
        completeExecuteRequest(callback, ret);
    }
}
