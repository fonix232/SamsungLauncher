package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeFolderChangeBackgroundColorSelectViewStateHandler extends AbstractStateHandler {
    HomeFolderChangeBackgroundColorSelectViewStateHandler(ExecutorState stateId) {
        super(stateId);
        if (stateId.toString().equals("HomeFolderChangeBackgroundColorSelectView")) {
            this.mNlgTargetState = "HomeFolderChangeBackgroundColorSelectView";
        } else {
            this.mNlgTargetState = "AppsFolderChangeBackgroundColorSelectView";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = getLauncherProxy().openFolderColorPanel();
        this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "yes");
        completeExecuteRequest(callback, ret);
    }
}
