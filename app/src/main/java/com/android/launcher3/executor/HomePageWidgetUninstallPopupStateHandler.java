package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;

class HomePageWidgetUninstallPopupStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = new StateAppInfo();

    HomePageWidgetUninstallPopupStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        for (String key : state.getParamMap().keySet()) {
            if (paramHelper.hasSlotValue(key, Type.STRING)) {
                this.mAppInfo.setComponentName(paramHelper.getString(key));
                if (!this.mAppInfo.isValid()) {
                    return "PARAM_CHECK_ERROR";
                }
            }
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_PAGE_WIDGET_EDIT_VIEW.toString()).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        ArrayList<ItemInfo> items = getLauncherProxy().getWidgetItemInfo(this.mAppInfo);
        if (items == null || items.size() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_PAGE_WIDGET_EDIT_VIEW.toString()).addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
        } else {
            ret = 0;
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            getLauncherProxy().uninstallWidget((ItemInfo) items.get(0));
        }
        completeExecuteRequest(callback, ret, 0);
    }
}
