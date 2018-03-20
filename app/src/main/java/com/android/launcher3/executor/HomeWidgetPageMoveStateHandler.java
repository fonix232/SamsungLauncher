package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;

class HomeWidgetPageMoveStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo;

    HomeWidgetPageMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mAppInfo = new StateAppInfo();
        this.mNlgTargetState = "Home";
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        this.mAppInfo.setComponentName(StateParamHelper.getStringParamValue(this, state.getParamMap(), "WidgetName", this.mNlgTargetState, GSIMLogging.HOME_EDIT_OPTION_WIDGET));
        if (!this.mAppInfo.isValid()) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper.setWidgetNamebyComponentName(this, this.mAppInfo);
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        ArrayList<ItemInfo> items = getLauncherProxy().getHomeWidgetItemInfo(this.mAppInfo);
        if (items == null || items.size() <= 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
            ret = 1;
        } else {
            getLauncherProxy().moveHomePageByWidgetItem(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "yes");
        }
        completeExecuteRequest(callback, ret);
    }
}
