package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

class AppsSingleAppMoveStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();
    private int mDetailDirection = LauncherProxy.INVALID_VALUE;
    private int mPage = LauncherProxy.INVALID_VALUE;
    private int mPageDirection = LauncherProxy.INVALID_VALUE;

    AppsSingleAppMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper helper = StateParamHelper.newHelper(params);
        if (helper.hasSlotValue("AppName", Type.STRING)) {
            this.mAppInfo.setComponentName(helper.getString("AppName"));
            if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
            if (helper.hasSlotValue("Page", Type.INTEGER)) {
                this.mPage = helper.getInt("Page");
            }
            if (this.mPage < 0) {
                this.mPageDirection = this.mPage;
                this.mPage = LauncherProxy.INVALID_VALUE;
            } else {
                this.mPage--;
            }
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("AppName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().hasItemInApps(this.mAppInfo)) {
            if ((this.mPage == LauncherProxy.INVALID_VALUE && this.mPageDirection == LauncherProxy.INVALID_VALUE) || !getLauncherProxy().isAppsValidPage(this.mPage, this.mPageDirection)) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Page", "Exist", "no").addResultParam("AppName", this.mAppInfo.getName());
                getLauncherProxy().moveAppsItemToPage(this.mAppInfo, AppsPageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), getLauncherProxy().getItemPageInApps(this.mAppInfo) + 1), this.mPageDirection, this.mDetailDirection);
            } else if (getLauncherProxy().hasAppsEmptySpace(this.mPage, this.mPageDirection)) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "yes").addResultParam("AppName", this.mAppInfo.getName());
                getLauncherProxy().moveAppsItemToPage(this.mAppInfo, this.mPage, this.mPageDirection, this.mDetailDirection);
            } else {
                int resultPageNum = AppsPageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), this.mPage);
                getLauncherProxy().moveAppsItemToPage(this.mAppInfo, resultPageNum, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "no").addResultParam("AppName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(resultPageNum + 1));
            }
            completeExecuteRequest(callback, 0);
            return;
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("AppName", "Match", "no").addResultParam("AppName", this.mAppInfo.getName());
        completeExecuteRequest(callback, 1);
    }
}
