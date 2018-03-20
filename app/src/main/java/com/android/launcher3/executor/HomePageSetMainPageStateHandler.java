package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageSetMainPageStateHandler extends AbstractStateHandler {
    private int mPage;
    private int mPageDirection;

    HomePageSetMainPageStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mPage = LauncherProxy.INVALID_VALUE;
        this.mPageDirection = LauncherProxy.INVALID_VALUE;
        this.mNlgTargetState = "HomePageEditView";
    }

    public String parseParameters(State state) {
        this.mPage = StateParamHelper.getIntParamValue(this, state.getParamMap(), "Page", this.mNlgTargetState, "PageLocation");
        if (this.mPage == LauncherProxy.INVALID_VALUE) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomePageEditView").addScreenParam("PageLocation", "Exist", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            return "PARAM_CHECK_ERROR";
        }
        if (this.mPage < 0) {
            this.mPageDirection = this.mPage;
        }
        this.mPage = getLauncherProxy().getPageNumberInOverview(this.mPage, this.mPageDirection);
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (!getLauncherProxy().isHomeValidPageInOverview(this.mPage, true)) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomePageEditView").addScreenParam("PageLocation", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            ret = 1;
        }
        if (ret == 0) {
            getLauncherProxy().moveToHomePage(this.mPage);
            getLauncherProxy().setHomeCurrentAsMainPage();
            getLauncherProxy().goHome();
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("PageLocation", "Valid", "yes");
        }
        completeExecuteRequest(callback, ret);
    }
}
