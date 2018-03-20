package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.State;

class AppsTidyUpPagesPreviewViewStateHandler extends AbstractAppsStateHandler {
    AppsTidyUpPagesPreviewViewStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().showAppsTidyUpPreview();
        completeExecuteRequest(callback, 0);
    }
}
