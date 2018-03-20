package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;

class AppsFolderSelectedViewStateHandler extends AbstractAppsStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();

    AppsFolderSelectedViewStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("FolderName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("FolderName"));
            if (this.mAppInfo.isValid()) {
                return "PARAM_CHECK_OK";
            }
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Exist", "no");
            return "PARAM_CHECK_ERROR";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        List<ItemInfo> items = getLauncherProxy().getAppsItemInfo(this.mAppInfo);
        int count = items == null ? -1 : items.size();
        if (count <= 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Match", "no");
        } else if (count == 1) {
            this.mAppInfo.setItemInfo((ItemInfo) items.get(0));
            if (this.mAppInfo.getItemInfo() != null) {
                ret = 0;
            }
        } else {
            getFirstPageNumber(items);
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.APPS.toString()).addScreenParam("FolderName", "Match", "multi").addResultParam("FolderName_count", Integer.toString(count)).addResultParam("FolderName", this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, ret);
    }

    private void getFirstPageNumber(List<ItemInfo> items) {
        if (items != null && items.size() != 0) {
            int firstPageNum = -1;
            for (ItemInfo item : items) {
                if (firstPageNum == -1) {
                    firstPageNum = (int) item.screenId;
                }
                if (firstPageNum > ((int) item.screenId)) {
                    firstPageNum = (int) item.screenId;
                }
            }
            if (getLauncherProxy().isAppsValidPage(firstPageNum)) {
                getLauncherProxy().moveAppsPage(firstPageNum);
            }
        }
    }
}
