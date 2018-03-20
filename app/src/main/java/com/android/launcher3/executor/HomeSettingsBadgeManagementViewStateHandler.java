package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsBadgeManagementViewStateHandler extends AbstractStateHandler {
    HomeSettingsBadgeManagementViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().enterBadgeManagementView();
        this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeManagementView");
        completeExecuteRequest(callback, 0);
    }
}
