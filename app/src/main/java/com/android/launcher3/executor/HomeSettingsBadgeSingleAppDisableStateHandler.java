package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;
import java.util.List;

class HomeSettingsBadgeSingleAppDisableStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo;
    private String mAppName;
    private String mClassName;

    HomeSettingsBadgeSingleAppDisableStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mAppInfo = new StateAppInfo();
        this.mNlgTargetState = "HomeSettingsBadgeSingleAppDisable";
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("AppName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("AppName"));
            if (this.mAppInfo.getComponentName() != null) {
                return "PARAM_CHECK_OK";
            }
            this.mNlgTargetState = "HomeSettingsBadgeManagementView";
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo("HomeSettingsBadgeSingleAppDisable").addScreenParam("AppName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
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
                getLauncherProxy().enableSingleAppBadge(this.mAppName, false);
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleAppBadge", "AlreadyOff", "no");
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleAppBadge", "AlreadyOff", "yes");
                ret = 1;
            }
        } else {
            this.mNlgTargetState = "HomeSettingsBadgeManagementView";
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("SingleApp", "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret);
    }
}
