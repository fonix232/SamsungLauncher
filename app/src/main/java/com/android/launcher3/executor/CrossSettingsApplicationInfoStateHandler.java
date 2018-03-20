package com.android.launcher3.executor;

import android.content.ComponentName;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.util.logging.GSIMLogging;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class CrossSettingsApplicationInfoStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    CrossSettingsApplicationInfoStateHandler(ExecutorState stateId) {
        super(stateId);
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        ComponentName componentName = null;
        LauncherAppWidgetInfo widgetItem = null;
        if (this.mAppInfo.getItemInfo() instanceof LauncherAppWidgetInfo) {
            widgetItem = (LauncherAppWidgetInfo) this.mAppInfo.getItemInfo();
        }
        if (widgetItem != null) {
            componentName = widgetItem.providerName;
        } else if (this.mAppInfo.getComponentName() != null) {
            componentName = this.mAppInfo.getComponentName();
        }
        if (componentName != null) {
            getLauncherProxy().showAppInfo(componentName, widgetItem.user);
            this.mNlgRequestInfo = new NlgRequestInfo("SettingsApplicationsInfo").addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam(GSIMLogging.HOME_EDIT_OPTION_WIDGET, "Match", "no");
            ret = 1;
        }
        completeExecuteRequest(callback, ret, 0);
    }
}
