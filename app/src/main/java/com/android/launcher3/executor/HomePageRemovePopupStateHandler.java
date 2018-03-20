package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageRemovePopupStateHandler extends AbstractStateHandler {
    protected static int sPage = LauncherProxy.INVALID_VALUE;

    HomePageRemovePopupStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mNlgTargetState = "HomePageEditView";
    }

    public String parseParameters(State state) {
        int pageDirection = LauncherProxy.INVALID_VALUE;
        sPage = StateParamHelper.getIntParamValue(this, state.getParamMap(), "Page", this.mNlgTargetState, "PageLocation");
        if (sPage == LauncherProxy.INVALID_VALUE) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Exist", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            return "PARAM_CHECK_ERROR";
        }
        if (sPage < 0) {
            pageDirection = sPage;
        }
        sPage = getLauncherProxy().getPageNumberInOverview(sPage, pageDirection);
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        this.mNlgRequestInfo = new NlgRequestInfo("HomePageEditView");
        completeExecuteRequest(callback, 0);
    }
}
