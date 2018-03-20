package com.android.launcher3.executor;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;

class HomeFolderMoveStateHandler extends AbstractStateHandler {
    private int mDetailDirection = LauncherProxy.INVALID_VALUE;
    private StateAppInfo mFolderInfo = new StateAppInfo();
    private int mPage = LauncherProxy.INVALID_VALUE;
    private int mPageDirection = LauncherProxy.INVALID_VALUE;

    HomeFolderMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mFolderInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper helper = StateParamHelper.newHelper(state.getParamMap());
        if (helper.hasSlotValue("FolderName", Type.STRING)) {
            this.mFolderInfo.setName(helper.getString("FolderName"));
            if (helper.hasSlotValue("Page", Type.INTEGER)) {
                int targetPage = helper.getInt("Page");
                if (targetPage < 0) {
                    this.mPageDirection = targetPage;
                    this.mPage = LauncherProxy.INVALID_VALUE;
                } else {
                    this.mPage = targetPage - 1;
                }
            } else {
                this.mPage = LauncherProxy.INVALID_VALUE;
            }
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("FolderName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        if (getLauncherProxy().hasItemInHome(this.mFolderInfo)) {
            View iv = null;
            if (this.mFolderInfo.getName() != null) {
                iv = getLauncherProxy().getFolderItemViewByTitle(this.mFolderInfo.getName());
            }
            if (iv == null) {
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("FolderName", "Match", "no");
                completeExecuteRequest(callback, 1);
                return;
            }
            List<ItemInfo> folders = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mFolderInfo);
            if (folders == null || folders.size() == 0) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("FolderName", "Match", "no");
                completeExecuteRequest(callback, 1);
                return;
            }
            int folderPage = getLauncherProxy().getHomePageNumberByScreenId(((ItemInfo) folders.get(0)).screenId);
            if (this.mPage == LauncherProxy.INVALID_VALUE && this.mPageDirection != LauncherProxy.INVALID_VALUE) {
                this.mPage = getLauncherProxy().mapDirectionToPage(this.mPage, this.mPageDirection, true);
            }
            if (!getLauncherProxy().isHomeValidPage(this.mPage)) {
                this.mPage = getLauncherProxy().moveHomeItemToPage(iv, folderPage + 1, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Page", "Exist", "no").addResultParam("FolderName", this.mFolderInfo.getName());
            } else if (getLauncherProxy().hasHomeEmptySpace(this.mPage, this.mPageDirection, 1, 1)) {
                getLauncherProxy().moveHomeItemToPage(iv, this.mPage, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Space", "Available", "yes").addResultParam("FolderName", this.mFolderInfo.getName());
            } else {
                this.mPage = getLauncherProxy().moveHomeItemToPage(iv, folderPage + 1, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("Space", "Available", "no").addResultParam("FolderName", this.mFolderInfo.getName()).addResultParam("ToPage", String.valueOf(this.mPage + 1));
            }
            completeExecuteRequest(callback, 0);
            return;
        }
        this.mNlgRequestInfo = new NlgRequestInfo("Home").addScreenParam("FolderName", "Match", "no");
        completeExecuteRequest(callback, 1);
    }
}
