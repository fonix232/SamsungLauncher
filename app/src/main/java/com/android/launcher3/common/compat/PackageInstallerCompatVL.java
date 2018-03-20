package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionCallback;
import android.content.pm.PackageInstaller.SessionInfo;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.compat.PackageInstallerCompat.PackageInstallInfo;
import com.android.launcher3.common.model.IconCache;
import java.util.HashMap;

@TargetApi(21)
public class PackageInstallerCompatVL extends PackageInstallerCompat {
    private static final String TAG = "PackageInstallerCompatVL";
    private final SparseArray<String> mActiveSessions = new SparseArray();
    private final IconCache mCache;
    private final SessionCallback mCallback = new SessionCallback() {
        public void onCreated(int sessionId) {
            pushSessionDisplayToLauncher(sessionId);
        }

        public void onFinished(int sessionId, boolean success) {
            String packageName = (String) PackageInstallerCompatVL.this.mActiveSessions.get(sessionId);
            PackageInstallerCompatVL.this.mActiveSessions.remove(sessionId);
            if (packageName != null) {
                PackageInstallerCompatVL.this.sendUpdate(new PackageInstallInfo(packageName, success ? 0 : 2, 0));
            }
        }

        public void onProgressChanged(int sessionId, float progress) {
            SessionInfo session = PackageInstallerCompatVL.this.mInstaller.getSessionInfo(sessionId);
            if (session != null) {
                PackageInstallerCompatVL.this.sendUpdate(new PackageInstallInfo(session.getAppPackageName(), 1, (int) (session.getProgress() * 100.0f)));
            }
        }

        public void onActiveChanged(int sessionId, boolean active) {
        }

        public void onBadgingChanged(int sessionId) {
            pushSessionDisplayToLauncher(sessionId);
        }

        private void pushSessionDisplayToLauncher(int sessionId) {
            SessionInfo session = PackageInstallerCompatVL.this.mInstaller.getSessionInfo(sessionId);
            if (session != null) {
                PackageInstallerCompatVL.this.addSessionInfoToCache(session, UserHandleCompat.myUserHandle());
                LauncherAppState app = LauncherAppState.getInstanceNoCreate();
                if (app != null) {
                    app.getModel().getHomeLoader().updateSessionDisplayInfo(session.getAppPackageName());
                    app.getModel().getAppsModel().updateSessionDisplayInfo(session.getAppPackageName());
                }
            }
        }
    };
    private final PackageInstaller mInstaller;
    private final Handler mWorker;

    PackageInstallerCompatVL(Context context) {
        this.mInstaller = context.getPackageManager().getPackageInstaller();
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        this.mCache = LauncherAppState.getInstance().getIconCache();
        this.mWorker = new Handler(LauncherModel.getWorkerLooper());
        this.mInstaller.registerSessionCallback(this.mCallback, this.mWorker);
    }

    public HashMap<String, Integer> updateAndGetActiveSessionCache() {
        HashMap<String, Integer> activePackages = new HashMap();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        for (SessionInfo info : this.mInstaller.getAllSessions()) {
            addSessionInfoToCache(info, user);
            if (info.getAppPackageName() != null) {
                activePackages.put(info.getAppPackageName(), Integer.valueOf((int) (info.getProgress() * 100.0f)));
                this.mActiveSessions.put(info.getSessionId(), info.getAppPackageName());
            }
        }
        return activePackages;
    }

    public void addAllSessionInfoToCache() {
        Log.d(TAG, "addAllSessionInfoToCache()");
        if (this.mInstaller != null) {
            UserHandleCompat user = UserHandleCompat.myUserHandle();
            for (SessionInfo info : this.mInstaller.getAllSessions()) {
                addSessionInfoToCache(info, user);
            }
        }
    }

    private void addSessionInfoToCache(SessionInfo info, UserHandleCompat user) {
        String packageName = info.getAppPackageName();
        if (packageName != null) {
            Log.d(TAG, "add PAI Info to Cache : " + packageName + ", " + info.getAppLabel() + ", " + info.getAppIcon());
            this.mCache.cachePackageInstallInfo(packageName, user, info.getAppIcon(), info.getAppLabel());
        }
    }

    public boolean isSessionInfoItem(String packageName) {
        if (!(this.mInstaller == null || packageName == null)) {
            for (SessionInfo info : this.mInstaller.getAllSessions()) {
                if (packageName.equals(info.getAppPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onStop() {
        this.mInstaller.unregisterSessionCallback(this.mCallback);
    }

    private void sendUpdate(PackageInstallInfo info) {
        LauncherAppState app = LauncherAppState.getInstanceNoCreate();
        if (app != null) {
            app.getModel().getHomeLoader().setPackageState(info);
            app.getModel().getAppsModel().setPackageState(info);
        }
    }
}
