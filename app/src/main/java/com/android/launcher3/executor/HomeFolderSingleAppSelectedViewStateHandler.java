package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeFolderSingleAppSelectedViewStateHandler extends AbstractStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();

    HomeFolderSingleAppSelectedViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("AppName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("AppName"));
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_VIEW.toString()).addScreenParam("SingleApp", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        completeExecuteRequest(callback, 0);
    }
}
