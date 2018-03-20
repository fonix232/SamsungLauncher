package com.android.launcher3.executor;

import android.content.Intent;
import com.android.launcher3.LauncherAppState;
import com.android.vcard.VCardConfig;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeStateHandler extends AbstractStateHandler {
    HomeStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public void execute(StateExecutionCallback callback) {
        Intent startMain = new Intent("android.intent.action.MAIN");
        startMain.addCategory("android.intent.category.HOME");
        startMain.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        LauncherAppState.getInstance().getContext().startActivity(startMain);
        completeExecuteRequest(callback, getLauncherProxy().goHome(), 100);
        this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString());
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }
}
