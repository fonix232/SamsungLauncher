package com.android.launcher3.executor;

import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

class HomeFolderRemoveIconStateHandler extends AbstractStateHandler {
    private static final String TAG = HomeFolderRemoveIconStateHandler.class.getSimpleName();
    private StateAppInfo mAppInfo;
    private String mAppName;
    private int mDetailedDirection;
    private boolean mIsPageNumberSpoken;
    private int mPage;
    private int mPageDirection;

    HomeFolderRemoveIconStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mAppInfo = new StateAppInfo();
        this.mPage = -1;
        this.mDetailedDirection = -1;
        this.mAppName = "AppName";
        this.mNlgTargetState = "Home";
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        this.mAppInfo.setComponentName(StateParamHelper.getStringParamValue(this, params, this.mAppName, this.mNlgTargetState));
        if (this.mAppInfo.isValid()) {
            if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
            this.mPage = StateParamHelper.getIntParamValue(this, params, "Page", this.mNlgTargetState);
            this.mIsPageNumberSpoken = this.mPage > 0;
            if (this.mPage != LauncherProxy.INVALID_VALUE && this.mPage < 0) {
                this.mPageDirection = this.mPage;
                this.mPage = LauncherProxy.INVALID_VALUE;
            } else if (this.mPage > 0) {
                this.mPage--;
            }
            if (this.mPage != LauncherProxy.INVALID_VALUE || this.mPageDirection != LauncherProxy.INVALID_VALUE) {
                return "PARAM_CHECK_OK";
            }
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("Page", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo("HomeFolderView").addScreenParam("AppName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (this.mPage == LauncherProxy.INVALID_VALUE) {
            this.mPage = getLauncherProxy().getOpenedHomeFolderPage();
        }
        if (this.mPage >= 0 && !getLauncherProxy().isHomeValidPage(this.mPage)) {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeFolderView").addScreenParam("Page", "Exist", "no");
            completeExecuteRequest(callback, 1);
        } else if (getLauncherProxy().hasItemInFolder(this.mAppInfo)) {
            int newPage = HomePageHelper.findAvailablePageAndCreateNewWhenFull(getLauncherProxy(), this.mPage);
            if (newPage != this.mPage && this.mIsPageNumberSpoken) {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Space", "Available", "no").addResultParam("AppName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(newPage + 1));
            } else if (this.mIsPageNumberSpoken) {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Space", "Available", "yes").addResultParam("AppName", this.mAppInfo.getName());
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Page", "Exist", "no").addResultParam("AppName", this.mAppInfo.getName());
            }
            this.mPage = newPage;
            doMove();
            completeExecuteRequest(callback, 0);
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("HomeFolderView").addScreenParam("AppName", "Match", "no");
            completeExecuteRequest(callback, 1);
        }
    }

    private void doMove() {
        getLauncherProxy().moveFolderItemToHome(this.mAppInfo);
        getLauncherProxy().closeFolder();
        getLauncherProxy().moveToHomePage(this.mPage);
    }
}
