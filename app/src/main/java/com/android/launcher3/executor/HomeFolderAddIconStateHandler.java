package com.android.launcher3.executor;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import com.samsung.android.sdk.bixby.data.State;
import java.util.List;
import java.util.Map;

class HomeFolderAddIconStateHandler extends AbstractStateHandler {
    private String mAppAnapho = null;
    private StateAppInfo mAppInfo = new StateAppInfo();
    private boolean mIsSelectAll;
    private int mOrdinal = LauncherProxy.INVALID_VALUE;
    private String mRuleID = null;
    private final String mRuleID_91 = "Home_91";

    HomeFolderAddIconStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        this.mRuleID = state.getRuleId();
        Map<String, Parameter> params = state.getParamMap();
        if (params == null || params.size() < 0) {
            return "PARAM_CHECK_ERROR";
        }
        StateParamHelper helper = StateParamHelper.newHelper(params);
        if (!this.mRuleID.equalsIgnoreCase("Home_91")) {
            String value = null;
            if (helper.hasSlotValue("AppName", Type.STRING)) {
                value = helper.getString("AppName");
            }
            if ((value == null || "".equals(value)) && !this.mAppInfo.isValid()) {
                return "PARAM_CHECK_ERROR";
            }
            this.mAppInfo.setComponentName(value);
            if (this.mAppInfo.getComponentName() != null && this.mAppInfo.getName() == " ") {
                this.mAppInfo.setName(getLauncherProxy().getAppNamebyComponentName(this.mAppInfo));
            }
        } else if (helper.hasSlotValue("SelectedAll", Type.BOOLEAN)) {
            this.mIsSelectAll = helper.getBoolean("SelectedAll");
        } else if (helper.hasSlotValue("OrdinalNumber", Type.INTEGER)) {
            this.mOrdinal = helper.getInt("OrdinalNumber");
        } else if (helper.hasSlotValue("SelectedAppAnapho", Type.STRING)) {
            this.mAppAnapho = helper.getString("SelectedAppAnapho");
        }
        return "PARAM_CHECK_OK";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        String resultAppName = null;
        if (this.mRuleID.equalsIgnoreCase("Home_91")) {
            int searchResultCount = getLauncherProxy().getSearchResultListCount();
            if (searchResultCount == 0) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_RESULT.toString()).addScreenParam("Text", "Match", "no");
            } else if (searchResultCount == 1) {
                getLauncherProxy().addSearchResultItemToFolder();
                ItemInfo singleAppInfo = getLauncherProxy().getSearchResultSingleAppInfo();
                if (!(singleAppInfo == null || singleAppInfo.title == null)) {
                    resultAppName = getLauncherProxy().getSearchResultSingleAppInfo().title.toString();
                }
                if (resultAppName != null) {
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON.toString()).addScreenParam("Text", "Match", "yes").addResultParam("Text", resultAppName);
                }
            } else if (this.mIsSelectAll) {
                getLauncherProxy().addSearchResultItemToFolder();
                this.mNlgRequestInfo = new NlgRequestInfo(this.mStateId.toString()).addScreenParam("SelectedAll", "Exist", "yes").addResultParam("apps_count", String.valueOf(searchResultCount));
            } else if (this.mOrdinal != LauncherProxy.INVALID_VALUE) {
                if (searchResultCount < this.mOrdinal || this.mOrdinal <= 0) {
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_RESULT.toString()).addScreenParam("OrdinalNumber", "Valid", "no");
                } else {
                    getLauncherProxy().addSearchResultItemToFolder(this.mOrdinal - 1);
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON.toString()).addScreenParam("OrdinalNumber", "Valid", "yes").addResultParam("OrdinalNumber", Integer.toString(this.mOrdinal));
                }
            } else if (this.mAppAnapho != null) {
                int checkItemCount = getLauncherProxy().getSearchResultListCheckedCount();
                if (checkItemCount < 1) {
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_SEARCH_RESULT.toString()).addScreenParam("OrdinalNumber", "Exist", "no").addResultParam("apps_count", String.valueOf(searchResultCount));
                } else {
                    getLauncherProxy().addSearchResultItemToFolder(true);
                    this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON.toString()).addScreenParam("SelectedAll", "Exist", "yes").addResultParam("apps_count", String.valueOf(checkItemCount));
                }
            }
        } else {
            boolean isInstalledApp;
            List<ItemInfo> itemInfoList;
            if (getLauncherProxy().isHomeOnlyMode()) {
                isInstalledApp = getLauncherProxy().hasItemInHome(this.mAppInfo);
                itemInfoList = getLauncherProxy().getHomeItemInfoByStateAppInfo(this.mAppInfo);
            } else {
                isInstalledApp = getLauncherProxy().hasItemInApps(this.mAppInfo);
                itemInfoList = getLauncherProxy().getAppsItemInfo(this.mAppInfo);
            }
            if (!isInstalledApp || itemInfoList == null) {
                ret = 1;
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_VIEW.toString()).addScreenParam("SingleApp", "Match", "no");
            } else if (itemInfoList.size() > 1) {
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_VIEW.toString()).addScreenParam("SingleApp", "Match", "multi");
            } else {
                getLauncherProxy().addHomeFolderItem(this.mAppInfo);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME_FOLDER_ADD_ICON_VIEW.toString()).addScreenParam("SingleApp", "Match", "yes");
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
