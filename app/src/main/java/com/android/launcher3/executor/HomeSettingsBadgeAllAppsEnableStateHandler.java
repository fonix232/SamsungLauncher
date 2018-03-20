package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsBadgeAllAppsEnableStateHandler extends AbstractStateHandler {
    HomeSettingsBadgeAllAppsEnableStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().isAllAppsBadgeSwitchChecked()) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeAllAppsEnable").addScreenParam("AllappsBadge", "AlreadyOn", "yes");
            ret = 1;
        } else {
            getLauncherProxy().enableAllAppsBadge(true);
            this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeAllAppsEnable").addScreenParam("AllappsBadge", "AlreadyOn", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
