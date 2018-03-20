package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsAlphabeticalOrderSortedStateHandler extends AbstractAppsStateHandler {
    AppsAlphabeticalOrderSortedStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mNlgTargetState = "AppsPageView";
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret;
        if (getLauncherProxy().isAppsViewTypeAlphabetic()) {
            ret = 1;
            getLauncherProxy().hideAppsViewTypePopup();
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("AlphabeticalOrder", "AlreadySet", "yes");
        } else {
            ret = getLauncherProxy().changeAppsViewTypeToAlphabetic();
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("AlphabeticalOrder", "AlreadySet", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
