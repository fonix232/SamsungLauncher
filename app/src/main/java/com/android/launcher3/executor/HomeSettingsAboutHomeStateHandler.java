package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

public class HomeSettingsAboutHomeStateHandler extends AbstractStateHandler {
    public /* bridge */ /* synthetic */ boolean isAllowedInHomeOnlyMode() {
        return super.isAllowedInHomeOnlyMode();
    }

    HomeSettingsAboutHomeStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = getLauncherProxy().enterHomeAboutPageView();
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_ABOUT_HOME.toString());
        completeExecuteRequest(callback, ret);
    }
}
