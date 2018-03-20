package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageAutoReArrangeToBottomStateHandler extends AbstractStateHandler {
    private int mPage;

    HomePageAutoReArrangeToBottomStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mPage = LauncherProxy.INVALID_VALUE;
        this.mNlgTargetState = "HomePageEditView";
    }

    public String parseParameters(State state) {
        int pageDirection = LauncherProxy.INVALID_VALUE;
        this.mPage = StateParamHelper.getIntParamValue(this, state.getParamMap(), "Page", this.mNlgTargetState, "PageLocation");
        if (this.mPage == LauncherProxy.INVALID_VALUE) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomePageEditView").addScreenParam("PageLocation", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        if (this.mPage < 0) {
            pageDirection = this.mPage;
        }
        this.mPage = getLauncherProxy().getPageNumberInOverview(this.mPage, pageDirection);
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (!getLauncherProxy().isHomeValidPageInOverview(this.mPage, true)) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomePageEditView").addScreenParam("PageLocation", "Valid", "no");
            ret = 1;
        }
        if (ret == 0) {
            getLauncherProxy().moveToHomePage(this.mPage);
            if (getLauncherProxy().checkAbleAlignIcon(this.mPage, false)) {
                getLauncherProxy().alignHomeIcon(this.mPage, false);
                if (getLauncherProxy().checkNeedDisplayAutoalignDialog()) {
                    this.mNlgRequestInfo = new NlgRequestInfo("HomePageAutoReArrangePopup").addScreenParam("Popup", "Appeared", "yes");
                } else {
                    getLauncherProxy().goHome();
                    this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Popup", "Appeared", "no");
                }
            } else {
                getLauncherProxy().goHome();
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("AlignToBottom", "Available", "no");
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
