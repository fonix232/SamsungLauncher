package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;

class HomeFolderSelectedViewStateHandler extends HomeSingleAppSelectedViewStateHandler {
    HomeFolderSelectedViewStateHandler(ExecutorState stateId) {
        super(stateId);
        this.mObjectName = "FolderName";
        this.mNlgTargetState = "Home";
    }

    public String parseParameters(State state) {
        this.mAppInfo.setName(StateParamHelper.getStringParamValue(this, state.getParamMap(), this.mObjectName, this.mNlgTargetState));
        if (this.mAppInfo.isValid()) {
            return "PARAM_CHECK_OK";
        }
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 1;
        List<ItemInfo> items = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
        int count = items == null ? -1 : items.size();
        if (count <= 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "no");
        } else if (count == 1) {
            for (ItemInfo i : items) {
                if (i != null && i.itemType == 2) {
                    this.mAppInfo.setItemInfo(i);
                    if (this.mAppInfo.getItemInfo() != null) {
                        ret = 0;
                    }
                }
            }
        } else {
            getFirstPageNumber(items);
            this.mNlgRequestInfo = new NlgRequestInfo(this.mNlgTargetState).addScreenParam("FolderName", "Match", "multi").addResultParam("FolderName_count", Integer.toString(count)).addResultParam("FolderName", this.mAppInfo.getName());
        }
        completeExecuteRequest(callback, ret);
    }

    private void getFirstPageNumber(List<ItemInfo> items) {
        if (items != null && items.size() != 0) {
            int firstPageNum = -1;
            for (ItemInfo item : items) {
                int pageNum = getLauncherProxy().getHomePageNumberByScreenId(item.screenId);
                if (firstPageNum == -1) {
                    firstPageNum = pageNum;
                }
                if (firstPageNum > pageNum) {
                    firstPageNum = pageNum;
                }
            }
            if (getLauncherProxy().isHomeValidPage(firstPageNum)) {
                getLauncherProxy().moveToHomePage(firstPageNum, true);
            }
        }
    }
}
