package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsChangeGridStateHandler extends AbstractStateHandler {
    private String mGridOption;

    HomeSettingsChangeGridStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("GridOption", Type.STRING)) {
            this.mGridOption = paramHelper.getString("GridOption");
            if (this.mGridOption != null) {
                return "PARAM_CHECK_OK";
            }
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        if (!getLauncherProxy().checkValidHomeGridOption(this.mGridOption)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Valid", "no");
        } else if (getLauncherProxy().checkMatchHomeGridOption(this.mGridOption)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("GridOption", "Valid", "AlreadySet");
            getLauncherProxy().goHome();
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_CHANGE_GRID.toString()).addScreenParam("GridOption", "Valid", "yes");
            getLauncherProxy().changeHomeScreengrid(this.mGridOption);
            ret = 0;
        }
        completeExecuteRequest(callback, ret);
    }
}
