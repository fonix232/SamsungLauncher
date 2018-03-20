package com.android.launcher3.home;

import android.os.Handler;
import com.android.launcher3.common.base.view.CellLayout;
import java.util.ArrayList;
import java.util.Iterator;

class DeferredWidgetRefresh implements Runnable {
    private final Handler mHandler = new Handler();
    private HomeController mHomeController;
    private final LauncherAppWidgetHost mHost;
    private final ArrayList<LauncherAppWidgetInfo> mInfos;
    private boolean mRefreshPending = true;

    DeferredWidgetRefresh(HomeController homeController, ArrayList<LauncherAppWidgetInfo> infos, LauncherAppWidgetHost host) {
        this.mHomeController = homeController;
        this.mInfos = infos;
        this.mHost = host;
        this.mHost.addProviderChangeListener(this);
        this.mHandler.postDelayed(this, 10000);
    }

    public void run() {
        this.mHost.removeProviderChangeListener(this);
        this.mHandler.removeCallbacks(this);
        if (this.mRefreshPending) {
            this.mRefreshPending = false;
            Iterator it = this.mInfos.iterator();
            while (it.hasNext()) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) it.next();
                if (info.hostView instanceof PendingAppWidgetHostView) {
                    PendingAppWidgetHostView view = info.hostView;
                    this.mHomeController.getBindController().removeAppWidget(info);
                    ((CellLayout) view.getParent().getParent()).removeView(view);
                    this.mHomeController.getBindController().bindAppWidget(info);
                }
            }
        }
    }
}
