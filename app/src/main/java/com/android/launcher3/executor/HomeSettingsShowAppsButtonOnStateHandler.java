package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsShowAppsButtonOnStateHandler extends AbstractStateHandler {
    HomeSettingsShowAppsButtonOnStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().isHomeOnlyMode()) {
            ret = 1;
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsView").addScreenParam("HomeAndAppsScreen", "AlreadySet", "no");
        } else if (getLauncherProxy().isEnableAppsButton()) {
            ret = 1;
            this.mNlgRequestInfo = new NlgRequestInfo("AppsButtonSettingsView").addScreenParam("AppsButton", "AlreadyOn", "yes");
        } else {
            getLauncherProxy().enableAppsButton(true);
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("AppsButton", "AlreadyOn", "no");
        }
        completeExecuteRequest(callback, ret, 100);
    }
}
