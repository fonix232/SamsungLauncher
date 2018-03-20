package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomePageWidgetSearchResultStateHandler extends AbstractStateHandler {
    boolean mIsLastState = false;
    String mSearchKey = "";

    HomePageWidgetSearchResultStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mNlgTargetState = stateId.toString();
    }

    public String parseParameters(State state) {
        this.mSearchKey = StateParamHelper.getStringParamValue(this, state.getParamMap(), "Text", this.mNlgTargetState);
        this.mIsLastState = state.isLastState().booleanValue();
        if (this.mSearchKey != null && !"".equals(this.mSearchKey)) {
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo("HomePageWidgetView").addScreenParam("Text", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret;
        if (this.mIsLastState) {
            ret = 0;
        } else {
            ret = 1;
        }
        int count = getLauncherProxy().searchWidgetList(this.mSearchKey);
        if (count == 1) {
            ret = 0;
            if (this.mIsLastState) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("Text", "Match", "yes").addResultParam("widgets_count", String.valueOf(count));
            }
        } else if (count > 1) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("Text", "Match", this.mIsLastState ? "yes" : "multi").addResultParam("widgets_count", String.valueOf(count));
        } else {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("Text", "Match", "no").addResultParam("Text", this.mSearchKey);
        }
        completeExecuteRequest(callback, ret, 0);
    }
}
