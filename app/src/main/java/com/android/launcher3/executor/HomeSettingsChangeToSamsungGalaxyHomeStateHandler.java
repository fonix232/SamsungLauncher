package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsChangeToSamsungGalaxyHomeStateHandler extends AbstractStateHandler {
    HomeSettingsChangeToSamsungGalaxyHomeStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            getLauncherProxy().changeHomeStyle(false);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_CHANGE_TO_HOME_APPS_POPUP.toString()).addScreenParam("HomeAndAppsScreen", "AlreadySet", "no");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS.toString()).addScreenParam("HomeAndAppsScreen", "AlreadySet", "yes");
        }
        completeExecuteRequest(callback, 0);
    }
}
