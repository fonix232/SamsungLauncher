package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeFolderViewStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    HomeFolderViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
        this.mNlgTargetState = "Home";
    }

    public String parseParameters(State state) {
        this.mAppInfo.setName(StateParamHelper.getStringParamValue(this, state.getParamMap(), "FolderName", this.mNlgTargetState));
        if (this.mAppInfo.isValid()) {
            return "PARAM_CHECK_OK";
        }
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasFolderInHome(this.mAppInfo.getName())) {
            int folderItemCount = getLauncherProxy().getHomeFolderItemCountByTitle(this.mAppInfo.getName());
            if (folderItemCount != 1) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "multi").addResultParam("FolderName_count", String.valueOf(folderItemCount)).addResultParam("FolderName", this.mAppInfo.getName());
                ret = 1;
            }
            if (ret == 0) {
                getLauncherProxy().openHomeFolder(this.mAppInfo.getName());
                this.mNlgRequestInfo = new NlgRequestInfo("HomeFolderView").addScreenParam("FolderName", "Match", "yes");
            }
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
