package com.android.launcher3.executor;

import android.util.Log;
import com.android.launcher3.proxy.LauncherProxy;

class AppsPageHelper {
    private static final String TAG = HomePageMoveStateHandler.class.getSimpleName();

    private AppsPageHelper() {
    }

    static int findAvailablePageAndCreateNewWhenFull(LauncherProxy proxy, int page) {
        Log.d(TAG, "findAvailablePageAndCreateNewWhenFull: " + page);
        boolean hasPageEmptySlot = false;
        int newPage = page;
        if (newPage < 0) {
            newPage = 0;
        }
        while (proxy.isAppsValidPage(newPage)) {
            if (proxy.hasAppsEmptySpace(newPage, 0)) {
                hasPageEmptySlot = true;
                break;
            }
            newPage++;
        }
        if (!hasPageEmptySlot) {
            newPage = proxy.getAppsPageCount();
        }
        Log.i(TAG, "new page: " + newPage);
        return newPage;
    }
}
