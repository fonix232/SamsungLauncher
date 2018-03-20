package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderRemoveIconStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();
    private boolean mIsPageNumberSpoken = true;
    private int mPage = LauncherProxy.INVALID_VALUE;
    private int mPageDirection = LauncherProxy.INVALID_VALUE;
    private int mtargetPosition = -1;

    AppsFolderRemoveIconStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("AppName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("AppName"));
            if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
            if (paramHelper.hasSlotValue("Page", Type.INTEGER)) {
                this.mPage = paramHelper.getInt("Page");
            }
            if (this.mPage < 0) {
                this.mPageDirection = this.mPage;
                this.mPage = LauncherProxy.INVALID_VALUE;
            } else {
                this.mPage--;
            }
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_VIEW.toString()).addScreenParam("AppName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().hasItemInFolder(this.mAppInfo)) {
            if (this.mPage == LauncherProxy.INVALID_VALUE && this.mPageDirection == LauncherProxy.INVALID_VALUE) {
                this.mPage = getLauncherProxy().getOpenedAppsFolderPage();
                this.mIsPageNumberSpoken = false;
            }
            int newPage = AppsPageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), this.mPage);
            getLauncherProxy().moveItemInFolderToAppsPage(this.mAppInfo, this.mPage, this.mtargetPosition);
            getLauncherProxy().closeFolder();
            if (!this.mIsPageNumberSpoken) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Page", "Exist", "no").addResultParam("AppName", this.mAppInfo.getName());
            } else if (newPage != this.mPage) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "no").addResultParam("AppName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(newPage + 1));
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("Space", "Available", "yes").addResultParam("AppName", this.mAppInfo.getName());
            }
            completeExecuteRequest(callback, 0);
            return;
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_VIEW.toString()).addScreenParam("AppName", "Match", "no").addResultParam("AppName", this.mAppInfo.getName());
        completeExecuteRequest(callback, 1);
    }
}
