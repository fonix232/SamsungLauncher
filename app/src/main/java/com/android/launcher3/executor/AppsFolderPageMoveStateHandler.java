package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class AppsFolderPageMoveStateHandler extends AbstractAppsStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();
    protected int mPage = LauncherProxy.INVALID_VALUE;
    protected int mPageDirection = LauncherProxy.INVALID_VALUE;

    AppsFolderPageMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("Page", Type.INTEGER)) {
            this.mPage = paramHelper.getInt("Page");
            if (this.mPage < 0) {
                this.mPageDirection = this.mPage;
                this.mPage = LauncherProxy.INVALID_VALUE;
            } else {
                this.mPage--;
            }
            if (this.mPage != LauncherProxy.INVALID_VALUE || this.mPageDirection != LauncherProxy.INVALID_VALUE) {
                return "PARAM_CHECK_OK";
            }
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_VIEW.toString()).addScreenParam("PageLocation", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_VIEW.toString()).addScreenParam("PageLocation", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (this.mPage < 0 || getLauncherProxy().isFolderValidPage(this.mPage)) {
            getLauncherProxy().moveFolderPage(this.mPage, this.mPageDirection);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_PAGE_MOVE.toString()).addScreenParam("PageLocation", "Valid", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_VIEW.toString()).addScreenParam("PageLocation", "Valid", "no").addResultParam("FolderName", this.mAppInfo.getName()).addResultParam("page_count", String.valueOf(getLauncherProxy().getOpenedFolderPageCount()));
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
