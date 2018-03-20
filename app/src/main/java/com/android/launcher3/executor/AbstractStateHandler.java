package com.android.launcher3.executor;

import android.os.Handler;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;

abstract class AbstractStateHandler implements StateHandler {
    NlgRequestInfo mNlgRequestInfo;
    @Deprecated
    String mNlgTargetState;
    ExecutorState mStateId;

    AbstractStateHandler(ExecutorState stateId) {
        this.mStateId = stateId;
    }

    public final NlgRequestInfo getNlgRequestInfo() {
        return this.mNlgRequestInfo;
    }

    public boolean isAllowedInHomeOnlyMode() {
        return true;
    }

    final LauncherProxy getLauncherProxy() {
        return LauncherAppState.getInstance().getLauncherProxy();
    }

    final void completeExecuteRequest(StateExecutionCallback callback, int ret) {
        completeExecuteRequest(callback, ret, 1000);
    }

    final void completeExecuteRequest(final StateExecutionCallback callback, int ret, int delay) {
        if (ret == 0) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    callback.executionCompleted(true);
                }
            }, (long) delay);
        } else {
            callback.executionCompleted(false);
        }
    }
}
