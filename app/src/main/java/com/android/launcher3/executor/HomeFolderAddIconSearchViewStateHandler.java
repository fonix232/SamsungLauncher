package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.State;

class HomeFolderAddIconSearchViewStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeFolderAddIconSearchViewStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeFolderAddIconSearchView")) {
            this.mNlgTargetState = "HomeFolderAddIconSearchView";
        } else {
            this.mNlgTargetState = "AppsFolderAddIconSearchView";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret;
        if (this.mNlgTargetState.equals("HomeFolderAddIconSearchView")) {
            ret = getLauncherProxy().enterHomeFolderAddApps(this.mAppInfo);
        } else {
            ret = getLauncherProxy().enterAppsFolderAddApps(this.mAppInfo);
        }
        completeExecuteRequest(callback, ret);
    }
}
