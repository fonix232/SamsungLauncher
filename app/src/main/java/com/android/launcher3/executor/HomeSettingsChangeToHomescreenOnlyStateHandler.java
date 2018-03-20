package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsChangeToHomescreenOnlyStateHandler extends AbstractStateHandler {
    HomeSettingsChangeToHomescreenOnlyStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsView").addScreenParam("HomescreenOnly", "AlreadySet", "yes");
        } else {
            getLauncherProxy().changeHomeStyle(true);
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsChangeToHomescreenOnlyPopup").addScreenParam("HomescreenOnly", "AlreadySet", "no");
        }
        completeExecuteRequest(callback, 0);
    }
}
