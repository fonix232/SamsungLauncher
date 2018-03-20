package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSingleAppUninstallDisableStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeSingleAppUninstallDisableStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        } else if (stateId.toString().equals("HomeSingleAppUninstallDisable")) {
            this.mNlgTargetState = "HomeSingleAppUninstallDisabledPopUp";
        } else {
            this.mNlgTargetState = "AppsSingleAppUninstallDisabledPopUp";
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        if (this.mAppInfo.getItemInfo() != null) {
            ret = 0;
            if (getLauncherProxy().isUninstallApp(this.mAppInfo.getItemInfo())) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Uninstall", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
                getLauncherProxy().uninstallOrDisableApp(this.mAppInfo.getItemInfo());
            } else if (getLauncherProxy().isDisableApp(this.mAppInfo.getItemInfo())) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Disable", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
                getLauncherProxy().uninstallOrDisableApp(this.mAppInfo.getItemInfo());
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo("AppsPageView").addScreenParam("SingleApp", "Disable", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
