package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;
import java.util.List;

class HomeSettingsBadgeSingleAppEnableStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo;
    private String mAppName;
    private String mClassName;

    HomeSettingsBadgeSingleAppEnableStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mAppInfo = new StateAppInfo();
        this.mNlgTargetState = "HomeSettingsBadgeSingleAppEnable";
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        String value = StateParamHelper.getStringParamValue(this, state.getParamMap(), "AppName", this.mNlgTargetState, "SingleApp");
        if (value == null) {
            return "PARAM_CHECK_ERROR";
        }
        this.mAppInfo.setComponentName(value);
        if (this.mAppInfo.getComponentName() == null) {
            this.mNlgTargetState = "HomeSettingsBadgeManagementView";
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        } else if (this.mAppInfo.isValid()) {
            return "PARAM_CHECK_OK";
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Valid", "no");
            return "PARAM_CHECK_ERROR";
        }
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        List<ItemInfo> items = new ArrayList();
        if (getLauncherProxy().isHomeOnlyMode()) {
            items = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
        } else {
            items = getLauncherProxy().getAppsItemInfoByStateAppInfo(this.mAppInfo);
        }
        ItemInfo item = null;
        this.mAppName = getLauncherProxy().getAppNamebyComponentName(this.mAppInfo);
        if (items != null) {
            for (ItemInfo info : items) {
                if (this.mAppName.equals(info.title)) {
                    item = info;
                    break;
                }
            }
        }
        if (item != null) {
            this.mClassName = this.mAppInfo.getComponentName().getClassName();
            if (getLauncherProxy().isSingleAppBadgeChecked(this.mClassName)) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppBadge", "AlreadyOn", "yes");
                ret = 1;
            } else {
                getLauncherProxy().enableSingleAppBadge(this.mAppName, true);
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleAppBadge", "AlreadyOn", "no");
            }
        } else {
            this.mNlgTargetState = "HomeSettingsBadgeManagementView";
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
