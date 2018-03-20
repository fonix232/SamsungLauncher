package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;

class HomeWidgetSelectedViewStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    HomeWidgetSelectedViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("WidgetName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("WidgetName"));
            if (this.mAppInfo.isValid()) {
                return "PARAM_CHECK_OK";
            }
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        ArrayList<ItemInfo> items = getLauncherProxy().getHomeWidgetItemInfo(this.mAppInfo);
        if (items == null || items.size() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
        } else {
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            if (this.mAppInfo.getItemInfo() != null) {
                ret = 0;
            }
        }
        completeExecuteRequest(callback, ret, 0);
    }

    StateAppInfo getAppInfo() {
        return this.mAppInfo;
    }
}
