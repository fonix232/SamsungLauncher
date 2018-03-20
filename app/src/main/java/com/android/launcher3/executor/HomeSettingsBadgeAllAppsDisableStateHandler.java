package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsBadgeAllAppsDisableStateHandler extends AbstractStateHandler {
    HomeSettingsBadgeAllAppsDisableStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().isAllAppsBadgeSwitchChecked()) {
            getLauncherProxy().enableAllAppsBadge(false);
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeAllAppsDisable").addScreenParam("AllappsBadge", "AlreadyOff", "no");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeAllAppsDisable").addScreenParam("AllappsBadge", "AlreadyOff", "yes");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
