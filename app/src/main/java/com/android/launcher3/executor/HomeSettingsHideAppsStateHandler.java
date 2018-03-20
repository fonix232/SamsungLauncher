package com.android.launcher3.executor;

import android.text.TextUtils;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeSettingsHideAppsStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo;

    HomeSettingsHideAppsStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mAppInfo = new StateAppInfo();
        this.mNlgTargetState = "HomeSettingsHideAppsView";
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        String value = StateParamHelper.getStringParamValue(this, state.getParamMap(), "AppName", this.mNlgTargetState, "SingleAppOrdinal");
        if (TextUtils.isEmpty(value)) {
            int ordinalNumber = StateParamHelper.getIntParamValue(this, state.getParamMap(), "OrdinalNumber", this.mNlgTargetState, "SingleAppOrdinal");
            if (ordinalNumber == LauncherProxy.INVALID_VALUE) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppOrdinal", "Exist", "no");
                return "PARAM_CHECK_ERROR";
            }
            this.mAppInfo.setOrdinalNumber(ordinalNumber);
        } else {
            this.mAppInfo.setComponentName(value);
            if (!this.mAppInfo.isValid()) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppOrdinal", "Exist", "no");
                return "PARAM_CHECK_ERROR";
            } else if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        ItemInfo info = getLauncherProxy().getItemInfoInHideApps(this.mAppInfo);
        if (info == null) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppOrdinal", "Match", "no");
            ret = 1;
        } else if (info.isHiddenByUser()) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppOrdinal", "Hidden", "yes");
            ret = 1;
        } else {
            this.mAppInfo.setItemInfo(info);
            getLauncherProxy().hideApps(this.mAppInfo);
            getLauncherProxy().goHome();
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleAppOrdinal", "Match", "yes").addResultParam("AppName", this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, ret);
    }
}
