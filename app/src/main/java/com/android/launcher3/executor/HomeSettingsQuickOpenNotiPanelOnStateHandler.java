package com.android.launcher3.executor;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

public class HomeSettingsQuickOpenNotiPanelOnStateHandler extends AbstractStateHandler {
    public /* bridge */ /* synthetic */ boolean isAllowedInHomeOnlyMode() {
        return super.isAllowedInHomeOnlyMode();
    }

    HomeSettingsQuickOpenNotiPanelOnStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        if (LauncherAppState.getInstance().getNotificationPanelExpansionEnabled()) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_SETTINGS.toString()).addScreenParam("QuickOpenNotiPanel", "AlreadyOn", "yes");
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("QuickOpenNotiPanel", "AlreadyOn", "no");
            if (LauncherFeature.supportNotificationPanelExpansion()) {
                getLauncherProxy().setNotificationPanelExpansionEnabled(true);
            } else {
                ret = 1;
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
