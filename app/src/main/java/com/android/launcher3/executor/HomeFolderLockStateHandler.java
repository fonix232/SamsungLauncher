package com.android.launcher3.executor;

import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;

class HomeFolderLockStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();

    HomeFolderLockStateHandler(ExecutorState stateId) {
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
        if (!(this.mAppInfo.getItemInfo() instanceof FolderInfo)) {
            this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("FolderName", "Match", "no");
            ret = 1;
        } else if (FolderLock.getInstance().isLockedFolder((FolderInfo) this.mAppInfo.getItemInfo())) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("Folder", "Already locked", "yes");
        } else {
            getLauncherProxy().lockFolder(this.mAppInfo.getItemInfo());
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("Folder", "Already locked", "no");
        }
        completeExecuteRequest(callback, ret);
    }
}
