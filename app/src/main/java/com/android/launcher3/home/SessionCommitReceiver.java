package com.android.launcher3.home;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller.SessionInfo;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import java.util.List;

@TargetApi(26)
public class SessionCommitReceiver extends BroadcastReceiver {
    private static final String MARKER_PROVIDER_PREFIX = ".addtohomescreen";
    private static final String TAG = "SessionCommitReceiver";

    private static class PrefInitTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;

        PrefInitTask(Context context) {
            this.mContext = context;
        }

        protected Void doInBackground(Void... voids) {
            boolean value = false;
            SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
            if (LauncherFeature.isChinaModel() || prefs.getBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, false)) {
                value = true;
            }
            prefs.edit().putBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, value).putBoolean(Utilities.ADD_ICON_PREFERENCE_INITIALIZED_KEY, true).apply();
            return null;
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (isEnabled(context) && VERSION.SDK_INT >= 26) {
            SessionInfo info = (SessionInfo) intent.getParcelableExtra("android.content.pm.extra.SESSION");
            UserHandle user = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
            if (info != null && !TextUtils.isEmpty(info.getAppPackageName()) && info.getInstallReason() == 4 && Process.myUserHandle().equals(user)) {
                List<LauncherActivityInfoCompat> activities = LauncherAppsCompat.getInstance(context).getActivityList(info.getAppPackageName(), UserHandleCompat.fromUser(user));
                if (activities != null && !activities.isEmpty()) {
                    InstallShortcutReceiver.queueActivityInfo((LauncherActivityInfoCompat) activities.get(0), context);
                }
            }
        }
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, false);
    }

    public static void applyDefaultUserPrefs(Context context) {
        if (VERSION.SDK_INT >= 26) {
            SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
            if (prefs.getAll().isEmpty()) {
                prefs.edit().putBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, false).apply();
            } else if (!prefs.contains(Utilities.ADD_ICON_PREFERENCE_INITIALIZED_KEY)) {
                new PrefInitTask(context).executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
            }
        }
    }
}
