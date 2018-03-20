package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsCustomOrderSortedStateHandler extends AbstractAppsStateHandler {
    AppsCustomOrderSortedStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mNlgTargetState = "AppsPageView";
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret;
        if (getLauncherProxy().isAppsViewTypeAlphabetic()) {
            ret = getLauncherProxy().changeAppsViewTypeToCustom();
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("CustomOrder", "AlreadySet", "no");
        } else {
            ret = 1;
            getLauncherProxy().hideAppsViewTypePopup();
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("CustomOrder", "AlreadySet", "yes");
        }
        completeExecuteRequest(callback, ret);
    }
}
