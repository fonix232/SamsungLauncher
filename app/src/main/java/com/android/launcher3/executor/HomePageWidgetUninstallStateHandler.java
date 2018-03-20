package com.android.launcher3.executor;

import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

public class HomePageWidgetUninstallStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    public /* bridge */ /* synthetic */ boolean isAllowedInHomeOnlyMode() {
        return super.isAllowedInHomeOnlyMode();
    }

    HomePageWidgetUninstallStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_PAGE_WIDGET_UNINSALL_POPUP.toString()).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "yes");
        completeExecuteRequest(callback, 0, 0);
    }
}
