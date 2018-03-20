package com.android.launcher3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import com.android.launcher3.common.base.item.IconInfo;
import com.sec.android.app.launcher.R;

public class Stats {
    public static final String ACTION_LAUNCH = "com.android.launcher3.action.LAUNCH";
    public static final String CONTAINER_ALL_APPS = "all_apps";
    public static final String CONTAINER_HOMESCREEN = "homescreen";
    public static final String CONTAINER_HOTSEAT = "hotseat";
    private static final boolean DEBUG_BROADCASTS = false;
    public static final String EXTRA_CELLX = "cellX";
    public static final String EXTRA_CELLY = "cellY";
    public static final String EXTRA_CONTAINER = "container";
    public static final String EXTRA_INTENT = "intent";
    public static final String EXTRA_SCREEN = "screen";
    public static final String EXTRA_SOURCE = "source";
    public static final String SOURCE_EXTRA_CONTAINER = "container";
    public static final String SOURCE_EXTRA_CONTAINER_PAGE = "container_page";
    public static final String SOURCE_EXTRA_SUB_CONTAINER = "sub_container";
    public static final String SOURCE_EXTRA_SUB_CONTAINER_PAGE = "sub_container_page";
    public static final String SUB_CONTAINER_ALL_APPS_A_Z = "a-z";
    public static final String SUB_CONTAINER_ALL_APPS_PREDICTION = "prediction";
    public static final String SUB_CONTAINER_ALL_APPS_SEARCH = "search";
    public static final String SUB_CONTAINER_FOLDER = "folder";
    private final String mLaunchBroadcastPermission;
    private final Launcher mLauncher;

    public interface LaunchSourceProvider {
        void fillInLaunchSourceData(Bundle bundle);
    }

    public static class LaunchSourceUtils {
        public static Bundle createSourceData() {
            Bundle sourceData = new Bundle();
            sourceData.putString("container", Stats.CONTAINER_HOMESCREEN);
            sourceData.putInt(Stats.SOURCE_EXTRA_CONTAINER_PAGE, 0);
            sourceData.putInt(Stats.SOURCE_EXTRA_SUB_CONTAINER_PAGE, 0);
            return sourceData;
        }

        public static void populateSourceDataFromAncestorProvider(View v, Bundle sourceData) {
            if (v != null) {
                LaunchSourceProvider provider = null;
                ViewParent parent = v.getParent();
                while (parent != null && (parent instanceof View)) {
                    if (parent instanceof LaunchSourceProvider) {
                        provider = (LaunchSourceProvider) parent;
                        break;
                    }
                    parent = parent.getParent();
                }
                if (provider != null) {
                    provider.fillInLaunchSourceData(sourceData);
                }
            }
        }
    }

    public Stats(Launcher launcher) {
        this.mLauncher = launcher;
        this.mLaunchBroadcastPermission = launcher.getResources().getString(R.string.receive_launch_broadcasts_permission);
    }

    public void recordLaunch(View v, Intent intent, IconInfo shortcut) {
        Intent intent2 = new Intent(intent);
        intent2.setSourceBounds(null);
        Intent broadcastIntent = new Intent(ACTION_LAUNCH).putExtra("intent", intent2.toUri(0));
        if (shortcut != null) {
            broadcastIntent.putExtra("container", shortcut.container).putExtra("screen", shortcut.screenId).putExtra("cellX", shortcut.cellX).putExtra("cellY", shortcut.cellY);
        }
        Bundle sourceExtras = LaunchSourceUtils.createSourceData();
        LaunchSourceUtils.populateSourceDataFromAncestorProvider(v, sourceExtras);
        broadcastIntent.putExtra(EXTRA_SOURCE, sourceExtras);
        this.mLauncher.sendBroadcast(broadcastIntent, this.mLaunchBroadcastPermission);
    }
}
