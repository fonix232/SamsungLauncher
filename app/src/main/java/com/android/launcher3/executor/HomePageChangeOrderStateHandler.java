package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageChangeOrderStateHandler extends AbstractStateHandler {
    private int mFromPage = LauncherProxy.INVALID_VALUE;
    private int mToPage = LauncherProxy.INVALID_VALUE;

    HomePageChangeOrderStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("Page", Type.INTEGER)) {
            this.mFromPage = paramHelper.getInt("Page");
        }
        if (paramHelper.hasSlotValue("MoveLocation", Type.INTEGER)) {
            this.mToPage = paramHelper.getInt("MoveLocation");
        }
        if (this.mFromPage == LauncherProxy.INVALID_VALUE) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString()).addScreenParam("Page", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            return "PARAM_CHECK_ERROR";
        } else if (this.mToPage == LauncherProxy.INVALID_VALUE) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString()).addScreenParam("PageLocation", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            return "PARAM_CHECK_ERROR";
        } else {
            this.mFromPage = getLauncherProxy().getPageNumberInOverview(this.mFromPage);
            this.mToPage = getLauncherProxy().getPageNumberInOverview(this.mToPage);
            return "PARAM_CHECK_OK";
        }
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (!getLauncherProxy().isHomeValidPageInOverview(this.mFromPage, true)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString()).addScreenParam("Page", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            ret = 1;
        } else if (!getLauncherProxy().isHomeValidPageInOverview(this.mToPage, true)) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString()).addScreenParam("PageLocation", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            ret = 1;
        } else if (this.mFromPage == this.mToPage) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_EDIT.toString()).addScreenParam("Page", "SameAsLocation", "yes");
            ret = 1;
        } else {
            getLauncherProxy().changeHomePageOrder(this.mFromPage, this.mToPage);
            getLauncherProxy().goHome();
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Page", "SameAsLocation", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
