package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsPageMoveStateHandler extends AbstractAppsStateHandler {
    private int mPage;
    private int mPageDirection;

    AppsPageMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mPage = LauncherProxy.INVALID_VALUE;
        this.mPageDirection = LauncherProxy.INVALID_VALUE;
        this.mNlgTargetState = "AppsPageView";
    }

    public String parseParameters(State state) {
        this.mPage = StateParamHelper.getIntParamValue(this, state.getParamMap(), "Page", this.mNlgTargetState, "PageLocation");
        if (this.mPage == LauncherProxy.INVALID_VALUE) {
            return "PARAM_CHECK_ERROR";
        }
        if (this.mPage < 0) {
            this.mPageDirection = this.mPage;
            this.mPage = LauncherProxy.INVALID_VALUE;
        } else {
            this.mPage--;
        }
        if (this.mPage != LauncherProxy.INVALID_VALUE || this.mPageDirection != LauncherProxy.INVALID_VALUE) {
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mPage >= 0 && !getLauncherProxy().isAppsValidPage(this.mPage, this.mPageDirection)) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getAppsPageCount()));
            ret = 1;
        }
        if (ret == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "yes").addResultParam("PageLocation", String.valueOf(this.mPage + 1));
            getLauncherProxy().moveAppsPage(this.mPage, this.mPageDirection);
        }
        completeExecuteRequest(callback, ret);
    }
}
