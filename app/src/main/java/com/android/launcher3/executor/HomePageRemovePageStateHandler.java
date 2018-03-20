package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageRemovePageStateHandler extends HomePageRemovePopupStateHandler {
    HomePageRemovePageStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        boolean needGoHome = true;
        if (getLauncherProxy().isHomeValidPageInOverview(sPage, true)) {
            getLauncherProxy().moveToHomePage(sPage);
            if (!getLauncherProxy().hasPageDeleteButton(sPage)) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("RemoveButton", "Available", "no");
                ret = 1;
            } else if (getLauncherProxy().isEmptyPage(sPage)) {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Shortcuts", "InPage", "no");
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo("HomePageRemovePopup").addScreenParam("Shortcuts", "InPage", "yes");
                needGoHome = false;
            }
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("PageLocation", "Valid", "no").addResultParam("page_count", String.valueOf(getLauncherProxy().getHomePageCountInOverviewMode()));
            ret = 1;
        }
        if (ret == 0) {
            getLauncherProxy().removeHomeCurrentPage();
            if (needGoHome) {
                getLauncherProxy().goHome();
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
