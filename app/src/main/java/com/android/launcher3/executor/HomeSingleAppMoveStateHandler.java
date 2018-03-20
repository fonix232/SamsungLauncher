package com.android.launcher3.executor;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;
import java.util.Map;

class HomeSingleAppMoveStateHandler extends AbstractStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();
    private int mDetailDirection = -1;
    private String mObjectName = "AppName";
    private int mPage = -1;
    private int mPageDirection;

    HomeSingleAppMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue(this.mObjectName, Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString(this.mObjectName));
            if (this.mAppInfo.isValid()) {
                if (paramHelper.hasSlotValue("Page", Type.INTEGER)) {
                    this.mPage = paramHelper.getInt("Page");
                } else {
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Page", "Exist", "no").addResultParam("AppName", this.mAppInfo.getName());
                    this.mPage = -2;
                }
                if (this.mPage < 0) {
                    this.mPageDirection = this.mPage;
                    this.mPage = LauncherProxy.INVALID_VALUE;
                } else {
                    this.mPage--;
                }
                return "PARAM_CHECK_OK";
            }
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Match", "no");
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (getLauncherProxy().hasItemInHome(this.mAppInfo)) {
            List<ItemInfo> items = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
            if (items == null || items.size() == 0) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Match", "no").addResultParam("AppName", this.mAppInfo.getName());
                completeExecuteRequest(callback, 1);
                return;
            }
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            View iv = null;
            if (this.mAppInfo.getComponentName() != null) {
                iv = getLauncherProxy().getItemViewByComponentName(this.mAppInfo.getComponentName());
            } else if (this.mAppInfo.getName() != null) {
                iv = getLauncherProxy().getItemViewByTitle(this.mAppInfo.getName());
            }
            if (iv == null) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Match", "no").addResultParam("AppName", this.mAppInfo.getName());
                ret = 1;
            }
            if (ret == 0) {
                int itemPage = getLauncherProxy().getHomePageNumberByScreenId(((ItemInfo) items.get(0)).screenId);
                if (this.mPage == LauncherProxy.INVALID_VALUE) {
                    this.mPage = getLauncherProxy().mapDirectionToPage(itemPage, this.mPageDirection, true);
                }
                if (this.mPage >= 0 && !getLauncherProxy().isHomeValidPage(this.mPage)) {
                    this.mPage = getLauncherProxy().moveHomeItemToPage(iv, itemPage + 1, this.mPageDirection, this.mDetailDirection);
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Page", "Exist", "no").addResultParam("AppName", this.mAppInfo.getName());
                } else if (getLauncherProxy().hasHomeEmptySpace(this.mPage, this.mPageDirection, 1, 1)) {
                    getLauncherProxy().moveHomeItemToPage(iv, this.mPage, this.mPageDirection, this.mDetailDirection);
                    if (this.mNlgRequestInfo == null) {
                        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Space", "Available", "yes").addResultParam("AppName", this.mAppInfo.getName());
                    }
                } else {
                    this.mPage = getLauncherProxy().moveHomeItemToPage(iv, itemPage + 1, this.mPageDirection, this.mDetailDirection);
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Space", "Available", "no").addResultParam("AppName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(this.mPage + 1));
                }
            }
            completeExecuteRequest(callback, ret);
            return;
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("AppName", "Match", "no").addResultParam("AppName", this.mAppInfo.getName());
        completeExecuteRequest(callback, 1);
    }
}
