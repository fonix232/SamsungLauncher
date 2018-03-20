package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.Map;

class AppsFolderAddIconSearchViewResultStateHandler extends AbstractAppsStateHandler {
    private String mSearchText;

    AppsFolderAddIconSearchViewResultStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public String parseParameters(State state) {
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper helper = StateParamHelper.newHelper(params);
        if (helper.hasSlotValue("Text", Type.STRING)) {
            this.mSearchText = helper.getString("Text");
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS_FOLDER_ADD_ICON_SEARCH_VIEW.toString()).addScreenParam("Text", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        getLauncherProxy().setAddAppsSearchText(this.mSearchText);
        completeExecuteRequest(callback, 0);
    }
}
