package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsChangeAppsGridStateHandler extends AbstractAppsStateHandler {
    private String mGridOption;

    HomeSettingsChangeAppsGridStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("GridOption", Type.STRING)) {
            this.mGridOption = paramHelper.getString("GridOption");
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (this.mGridOption.isEmpty()) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Exist", "no");
            completeExecuteRequest(callback, 1);
        } else if (!getLauncherProxy().checkValidAppsGridOption(this.mGridOption)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Valid", "no");
            completeExecuteRequest(callback, 1);
        } else if (getLauncherProxy().checkMatchAppsGridOption(this.mGridOption)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_APPS_GRID_SETTING_VIEW.toString()).addScreenParam("GridOption", "Valid", "AlreadySet");
            completeExecuteRequest(callback, 0);
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS_CHANGE_APPS_GRID.toString()).addScreenParam("GridOption", "Valid", "yes");
            getLauncherProxy().changeAppsScreengrid(this.mGridOption);
            completeExecuteRequest(callback, 0);
        }
    }
}
