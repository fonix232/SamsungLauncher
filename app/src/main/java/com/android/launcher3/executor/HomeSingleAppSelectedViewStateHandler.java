package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;

class HomeSingleAppSelectedViewStateHandler extends AbstractStateHandler {
    protected StateAppInfo mAppInfo = new StateAppInfo();
    protected String mObjectName;

    HomeSingleAppSelectedViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
        this.mObjectName = "AppName";
        this.mNlgTargetState = "Home";
    }

    public String parseParameters(State state) {
        this.mAppInfo.setComponentName(StateParamHelper.getStringParamValue(this, state.getParamMap(), this.mObjectName, this.mNlgTargetState, "SingleApp"));
        if (!this.mAppInfo.isValid()) {
            return "PARAM_CHECK_ERROR";
        }
        if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
            this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        List<ItemInfo> items = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
        if (items == null || items.size() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
        } else {
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            if (this.mAppInfo.getItemInfo() != null) {
                ret = 0;
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "no").addResultParam("SingleApp", this.mAppInfo.getName());
            }
        }
        completeExecuteRequest(callback, ret, 0);
    }

    protected StateAppInfo getAppInfo() {
        return this.mAppInfo;
    }
}
