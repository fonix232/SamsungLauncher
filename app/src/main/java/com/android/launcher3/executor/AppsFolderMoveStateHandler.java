package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

class AppsFolderMoveStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();
    private int mDetailDirection = LauncherProxy.INVALID_VALUE;
    private int mPage = LauncherProxy.INVALID_VALUE;
    private int mPageDirection = LauncherProxy.INVALID_VALUE;

    AppsFolderMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper helper = StateParamHelper.newHelper(params);
        if (helper.hasSlotValue("FolderName", Type.STRING)) {
            this.mAppInfo.setName(helper.getString("FolderName"));
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
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().hasFolderInApps(this.mAppInfo.getName())) {
            if ((this.mPage == LauncherProxy.INVALID_VALUE && this.mPageDirection == LauncherProxy.INVALID_VALUE) || !getLauncherProxy().isAppsValidPage(this.mPage, this.mPageDirection)) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Page", "Exist", "no");
                getLauncherProxy().moveAppsFolderItemToPage(this.mAppInfo, AppsPageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), getLauncherProxy().getItemPageInApps(this.mAppInfo) + 1), this.mPageDirection, this.mDetailDirection);
            } else if (getLauncherProxy().hasAppsEmptySpace(this.mPage, this.mPageDirection)) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "yes").addResultParam("FolderName", this.mAppInfo.getName());
                getLauncherProxy().moveAppsFolderItemToPage(this.mAppInfo, this.mPage, this.mPageDirection, this.mDetailDirection);
            } else {
                int resultPageNum = AppsPageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), this.mPage);
                getLauncherProxy().moveAppsFolderItemToPage(this.mAppInfo, resultPageNum, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "no").addResultParam("FolderName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(resultPageNum + 1));
            }
            completeExecuteRequest(callback, 0);
            return;
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Match", "no");
        completeExecuteRequest(callback, 1);
    }
}
