package com.android.launcher3.common.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Process;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserManagerCompat;

public abstract class DataUpdater {
    protected static ContentResolver sContentResolver;
    protected static Context sContext;
    protected static IconCache sIconCache;
    protected static boolean sIsSafeMode;
    protected static LauncherAppsCompat sLauncherApps;
    protected static LauncherModel sLauncherModel;
    protected static PackageManager sPackageManager;
    protected static UserManagerCompat sUserManager;
    protected static Handler sWorkerHandler;

    public interface UpdaterInterface {
        long addItem(ItemInfo itemInfo);

        void deleteItem(ItemInfo itemInfo);

        void updateItem(ContentValues contentValues, ItemInfo itemInfo);
    }

    protected void init(Context context, LauncherModel model, IconCache cache) {
        if (sContext == null) {
            sContext = context;
            sWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
            sContentResolver = context.getContentResolver();
            sIsSafeMode = context.getPackageManager().isSafeMode();
            sLauncherApps = LauncherAppsCompat.getInstance(context);
            sUserManager = UserManagerCompat.getInstance(context);
            sPackageManager = context.getPackageManager();
            sIconCache = cache;
            sLauncherModel = model;
        }
    }

    protected static void runOnWorkerThread(Runnable r) {
        if (LauncherModel.sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sWorkerHandler.post(r);
        }
    }
}
