package com.android.launcher3.executor;

import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeRemoveWidgetStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeRemoveWidgetStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
        this.mNlgTargetState = "Home";
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().removeHomeWidget(this.mAppInfo.getItemInfo()) == -2) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "yes").addResultParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, 0);
    }
}
