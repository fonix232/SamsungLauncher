package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageMoveStateHandler extends AbstractStateHandler {
    protected int mPage;
    protected int mPageDirection;

    HomePageMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mPage = LauncherProxy.INVALID_VALUE;
        this.mPageDirection = LauncherProxy.INVALID_VALUE;
        this.mNlgTargetState = "Home";
    }

    public String parseParameters(State state) {
        this.mPage = StateParamHelper.getIntParamValue(this, state.getParamMap(), "Page", this.mNlgTargetState, "PageLocation");
        if (this.mPage == LauncherProxy.INVALID_VALUE) {
            getLauncherProxy().moveToHomePage(-1, -6);
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
        getLauncherProxy().moveToHomePage(-1, -6);
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mPage >= 0 && !getLauncherProxy().isHomeValidPage(this.mPage)) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "no");
            getLauncherProxy().moveToHomePage(-1, -6);
            ret = 1;
        }
        if (ret == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "yes");
            getLauncherProxy().moveToHomePage(this.mPage, this.mPageDirection);
        }
        completeExecuteRequest(callback, ret);
    }
}
