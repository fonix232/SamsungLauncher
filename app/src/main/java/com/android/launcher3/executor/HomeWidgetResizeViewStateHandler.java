package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;

class HomeWidgetResizeViewStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    HomeWidgetResizeViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
        this.mNlgTargetState = stateId.toString();
    }

    public String parseParameters(State state) {
        this.mAppInfo.setComponentName(StateParamHelper.getStringParamValue(this, state.getParamMap(), "WidgetName", "Home", GSIMLogging.HOME_EDIT_OPTION_WIDGET));
        if (!this.mAppInfo.isValid()) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper.setWidgetNamebyComponentName(this, this.mAppInfo);
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        ArrayList<ItemInfo> items = getLauncherProxy().getHomeWidgetItemInfo(this.mAppInfo);
        if (items == null || items.size() != 1) {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
        } else {
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            if (getLauncherProxy().enterWidgetResizeMode(this.mAppInfo.getItemInfo()) == 0) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "yes").addResultParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, this.mAppInfo.getName());
            } else {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "unable").addResultParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, this.mAppInfo.getName());
            }
        }
        completeExecuteRequest(callback, 0);
    }
}
