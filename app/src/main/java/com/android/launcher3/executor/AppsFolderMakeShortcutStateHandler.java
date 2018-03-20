package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderMakeShortcutStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();

    AppsFolderMakeShortcutStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("FolderName", Type.STRING)) {
            this.mAppInfo.setName(paramHelper.getString("FolderName"));
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasFolderInApps(this.mAppInfo.getName())) {
            getLauncherProxy().createHomeFolderShortcut(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_MAKE_SHORTCUT.toString()).addScreenParam("FolderName", "Match", "yes").addResultParam("FolderName", this.mAppInfo.getName());
        } else {
            ret = 1;
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Match", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
