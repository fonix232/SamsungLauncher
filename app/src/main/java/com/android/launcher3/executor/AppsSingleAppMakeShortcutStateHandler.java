package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsSingleAppMakeShortcutStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();

    AppsSingleAppMakeShortcutStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper helper = StateParamHelper.newHelper(state.getParamMap());
        if (helper.hasSlotValue("AppName", Type.STRING)) {
            this.mAppInfo.setComponentName(helper.getString("AppName"));
            if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("SingleApp", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            getLauncherProxy().createHomeAppShortcut(this.mAppInfo, LauncherProxy.INVALID_VALUE);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_SINGLE_APP_MAKE_SHORTCUT.toString()).addScreenParam("SingleApp", "Match", "yes").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
