package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;
import java.util.List;

class HomeSingleAppLockStateHandler extends AbstractStateHandler {
    StateAppInfo mAppInfo;

    HomeSingleAppLockStateHandler(ExecutorState stateId) {
        super(stateId);
        if (stateId.toString().equals("HomeSingleAppLock")) {
            this.mNlgTargetState = "HomeSingleAppLock";
        } else {
            this.mNlgTargetState = "AppsSingleAppLock";
        }
        this.mAppInfo = StateAppInfoHolder.INSTANCE.getStateAppInfo();
        if (this.mAppInfo == null) {
            throw new IllegalStateException("StateAppInfo is not set");
        }
    }

    public String parseParameters(State state) {
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        List<ItemInfo> items = new ArrayList();
        if (this.mNlgTargetState.equals("HomeSingleAppLock")) {
            items = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
        } else {
            items = getLauncherProxy().getAppsItemInfo(this.mAppInfo);
        }
        if (items == null || items.size() != 1) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleApp", "Match", "no");
            ret = 1;
        } else {
            FolderLock folderLock = FolderLock.getInstance();
            if (!(items.get(0) instanceof IconInfo)) {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("SingleApp", "Match", "no");
                ret = 1;
            } else if (folderLock.isLockedApp((IconInfo) items.get(0))) {
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleApp", "Already locked", "yes");
            } else {
                getLauncherProxy().lockSingleApp((ItemInfo) items.get(0));
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SingleApp", "Already locked", "no");
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
