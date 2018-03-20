package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;

class HomeFolderSingleAppRemoveIconStateHandler extends AbstractStateHandler {
    private StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeFolderSingleAppRemoveIconStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        if (this.mAppInfo == null) {
            return "PARAM_CHECK_ERROR";
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        if (this.mAppInfo != null) {
            this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
        }
        List<ItemInfo> items = getLauncherProxy().getFolderItemInfoByStateAppInfo(this.mAppInfo);
        if (items == null || items.size() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_VIEW.toString()).addScreenParam("SingleApp", "Match", "no");
            completeExecuteRequest(callback, 1);
        } else if (items.size() != 1) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_VIEW.toString()).addScreenParam("SingleApp", "Match", "multi");
            completeExecuteRequest(callback, 1);
        } else {
            getLauncherProxy().removeFolderItem(this.mAppInfo);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_VIEW.toString()).addScreenParam("SingleApp", "Match", "yes");
            completeExecuteRequest(callback, 0);
        }
    }
}
