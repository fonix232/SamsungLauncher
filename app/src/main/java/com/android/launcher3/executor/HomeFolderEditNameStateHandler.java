package com.android.launcher3.executor;

import android.text.TextUtils;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeFolderEditNameStateHandler extends AbstractStateHandler {
    private String mNewTitle;

    HomeFolderEditNameStateHandler(ExecutorState stateId) {
        super(stateId);
        if (stateId.toString().equals("HomeFolderEditName")) {
            this.mNlgTargetState = "HomeFolderView";
        } else {
            this.mNlgTargetState = "AppsFolderView";
        }
    }

    public String parseParameters(State state) {
        this.mNewTitle = StateParamHelper.getStringParamValue(this, state.getParamMap(), "Text", this.mNlgTargetState);
        if (!TextUtils.isEmpty(this.mNewTitle)) {
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("Text", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mNlgTargetState.equals("HomeFolderView")) {
            this.mNlgTargetState = "HomeFolderEditName";
        } else {
            this.mNlgTargetState = "AppsFolderEditName";
        }
        if (this.mNewTitle.length() > 30) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("Text", "ExceedMaxChar", "yes");
            ret = 1;
        } else {
            getLauncherProxy().changeHomeFolderTitle(this.mNewTitle);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("Text", "ExceedMaxChar", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
