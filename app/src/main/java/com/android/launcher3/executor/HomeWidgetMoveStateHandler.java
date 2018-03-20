package com.android.launcher3.executor;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;

class HomeWidgetMoveStateHandler extends AbstractStateHandler {
    private StateAppInfo mAppInfo = new StateAppInfo();
    private int mDetailDirection = LauncherProxy.INVALID_VALUE;
    private int mPage = LauncherProxy.INVALID_VALUE;
    private int mPageDirection = LauncherProxy.INVALID_VALUE;

    HomeWidgetMoveStateHandler(ExecutorState stateId) {
        super(stateId);
        StateAppInfoHolder.INSTANCE.setStateAppInfo(this.mAppInfo);
    }

    public String parseParameters(State state) {
        StateParamHelper paramHelper = StateParamHelper.newHelper(state.getParamMap());
        if (paramHelper.hasSlotValue("WidgetName", Type.STRING)) {
            this.mAppInfo.setComponentName(paramHelper.getString("WidgetName"));
            if (!this.mAppInfo.isValid()) {
                return "PARAM_CHECK_ERROR";
            }
            if (paramHelper.hasSlotValue("Page", Type.INTEGER)) {
                this.mPage = paramHelper.getInt("Page");
                if (this.mPage < 0) {
                    this.mPageDirection = this.mPage;
                    this.mPage = LauncherProxy.INVALID_VALUE;
                } else {
                    this.mPage--;
                }
            } else {
                this.mPage = LauncherProxy.INVALID_VALUE;
            }
            return "PARAM_CHECK_OK";
        }
        this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("WidgetName", "Exist", "no");
        return "PARAM_CHECK_ERROR";
    }

    public void execute(StateExecutionCallback callback) {
        int ret = 0;
        ArrayList<ItemInfo> widgets = getLauncherProxy().getHomeWidgetItemInfo(this.mAppInfo);
        if (widgets == null || widgets.size() == 0) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("WidgetName", "Match", "no");
            ret = 1;
        }
        View iv = null;
        if (this.mAppInfo.getComponentName() != null) {
            iv = getLauncherProxy().getWidgetView(this.mAppInfo.getComponentName());
        } else if (this.mAppInfo.isValid() && widgets.size() > 0) {
            iv = getLauncherProxy().getWidgetView(((LauncherAppWidgetInfo) widgets.get(0)).providerName);
        }
        if (iv == null) {
            this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("WidgetName", "Match", "no");
            ret = 1;
        }
        if (ret == 0) {
            int widgetPage = getLauncherProxy().getHomePageNumberByScreenId(((ItemInfo) widgets.get(0)).screenId);
            if (this.mPage == LauncherProxy.INVALID_VALUE && this.mPageDirection != LauncherProxy.INVALID_VALUE) {
                this.mPage = getLauncherProxy().mapDirectionToPage(this.mPage, this.mPageDirection, true);
            }
            if (!getLauncherProxy().isHomeValidPage(this.mPage)) {
                this.mPage = getLauncherProxy().moveHomeItemToPage(iv, widgetPage + 1, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Page", "Exist", "no").addResultParam("WidgetName", this.mAppInfo.getName());
            } else if (getLauncherProxy().hasHomeEmptySpace(this.mPage, this.mPageDirection, ((ItemInfo) widgets.get(0)).spanX, ((ItemInfo) widgets.get(0)).spanY)) {
                getLauncherProxy().moveHomeItemToPage(iv, this.mPage, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Space", "Available", "yes").addResultParam("WidgetName", this.mAppInfo.getName());
            } else {
                this.mPage = getLauncherProxy().moveHomeItemToPage(iv, widgetPage + 1, this.mPageDirection, this.mDetailDirection);
                this.mNlgRequestInfo = new NlgRequestInfo(ExecutorState.HOME.toString()).addScreenParam("Space", "Available", "no").addResultParam("WidgetName", this.mAppInfo.getName()).addResultParam("ToPage", String.valueOf(this.mPage + 1));
            }
        }
        completeExecuteRequest(callback, ret);
    }
}
