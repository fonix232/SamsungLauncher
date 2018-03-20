package com.android.launcher3.executor;

import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

interface StateHandler {
    void execute(StateExecutionCallback stateExecutionCallback);

    NlgRequestInfo getNlgRequestInfo();

    boolean isAllowedInHomeOnlyMode();

    String parseParameters(State state);
}
