package com.android.launcher3.executor;

import android.util.Log;
import com.android.launcher3.proxy.LauncherProxy;

class HomePageHelper {
    private static final String TAG = HomePageMoveStateHandler.class.getSimpleName();

    private HomePageHelper() {
    }

    static int findAvailablePageAndCreateNewWhenFull(LauncherProxy proxy, int page) {
        Log.d(TAG, "findAvailablePageAndCreateNewWhenFull: " + page);
        boolean hasPageEmptySlot = false;
        int newPage = page;
        if (newPage < 0) {
            newPage = 0;
        }
        while (proxy.isHomeValidPage(newPage)) {
            if (proxy.hasHomeEmptySpace(newPage, 0, 1, 1)) {
                hasPageEmptySlot = true;
                break;
            }
            newPage++;
        }
        if (!hasPageEmptySlot) {
            proxy.addNewPageInHome();
            newPage = proxy.getHomePageCount() - 1;
        }
        Log.i(TAG, "new page: " + newPage);
        return newPage;
    }
}
