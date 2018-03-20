package com.android.launcher3.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.compat.UserHandleCompat;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ExternalRequestQueue {
    private static final String EXTERNAL_REQUEST_LIST_HOME_APPS = "external_request_list_home_apps";
    private static final String EXTERNAL_REQUEST_LIST_HOME_ONLY = "external_request_list_home_only";
    private static final String INSTALL_SHORTCUT_FLUSHED = "com.samsung.android.launcher.action.INSTALL_SHORTCUT_FLUSHED";
    private static final String TAG = "ExternalRequestQueue";
    private static final String TASK_EDGE_PACKAGE = "com.samsung.android.app.taskedge";
    private static final Object sLock = new Object();
    private static SharedPreferences sSharedPref = null;

    public static void disableAndFlushExternalRequestQueue(Context context, LauncherAppState app) {
        app.enableExternalQueue(false);
        flushExternalRequestQueue(context, app);
    }

    private static SharedPreferences getSharedPreference(Context context) {
        if (sSharedPref == null) {
            sSharedPref = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        }
        return sSharedPref;
    }

    private static void flushExternalRequestQueue(Context context, LauncherAppState app) {
        if (app.getModel().getCallback().isHomeNormal()) {
            ArrayList<ExternalRequestInfo> installQueue = getAndClearExternalRequestQueue(getSharedPreference(context), context);
            Collections.sort(installQueue, new Comparator<ExternalRequestInfo>() {
                public int compare(ExternalRequestInfo lhs, ExternalRequestInfo rhs) {
                    return Long.compare(lhs.requestTime, rhs.requestTime);
                }
            });
            if (!installQueue.isEmpty()) {
                Iterator<ExternalRequestInfo> iterator = installQueue.iterator();
                while (iterator.hasNext()) {
                    ((ExternalRequestInfo) iterator.next()).runRequestInfo(context);
                }
                sendBroadCaseToTaskEdge(context);
            }
        }
    }

    private static Set<String> getExternalRequestList(SharedPreferences sharedPrefs) {
        return sharedPrefs.getStringSet(LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? EXTERNAL_REQUEST_LIST_HOME_ONLY : EXTERNAL_REQUEST_LIST_HOME_APPS, null);
    }

    static ArrayList<ExternalRequestInfo> getExternalRequestListByType(Context context, int type) {
        Set<String> strings = getExternalRequestList(getSharedPreference(context));
        ArrayList<ExternalRequestInfo> infoList = new ArrayList();
        if (strings != null) {
            for (String encoded : strings) {
                ExternalRequestInfo savedInfo = decode(encoded, context);
                if (savedInfo != null && savedInfo.getRequestType() == type) {
                    infoList.add(savedInfo);
                }
            }
        }
        return infoList;
    }

    private static void setExternalRequestList(SharedPreferences sharedPrefs, Set<String> strings) {
        sharedPrefs.edit().putStringSet(LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? EXTERNAL_REQUEST_LIST_HOME_ONLY : EXTERNAL_REQUEST_LIST_HOME_APPS, strings).apply();
    }

    private static ArrayList<ExternalRequestInfo> getAndClearExternalRequestQueue(SharedPreferences sharedPrefs, Context context) {
        ArrayList<ExternalRequestInfo> arrayList;
        synchronized (sLock) {
            Set<String> strings = getExternalRequestList(sharedPrefs);
            Log.d(TAG, "Getting and clearing EXTERNAL_REQUEST_LIST: " + strings);
            if (strings == null) {
                arrayList = new ArrayList();
            } else {
                arrayList = new ArrayList();
                for (String encoded : strings) {
                    ExternalRequestInfo info = decode(encoded, context);
                    if (info != null) {
                        arrayList.add(info);
                    }
                }
                setExternalRequestList(sharedPrefs, new LinkedHashSet());
            }
        }
        return arrayList;
    }

    static boolean removeFromExternalRequestQueue(Context context, int type, Intent intent) {
        boolean isRemove = false;
        synchronized (sLock) {
            Set<String> strings = getExternalRequestList(getSharedPreference(context));
            if (strings != null) {
                Set<String> newStrings = new LinkedHashSet(strings);
                Iterator<String> newStringsIterator = newStrings.iterator();
                while (newStringsIterator.hasNext()) {
                    try {
                        JSONObject object = (JSONObject) new JSONTokener((String) newStringsIterator.next()).nextValue();
                        int requestType = object.getInt("type");
                        Intent launcherIntent = Intent.parseUri(object.getString("intent.launch"), 4);
                        if (requestType == type && launcherIntent.toUri(0).equals(intent.toUri(0))) {
                            newStringsIterator.remove();
                            isRemove = true;
                            break;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException occured" + e);
                    } catch (URISyntaxException e2) {
                        Log.e(TAG, "URISyntaxException occured" + e2);
                    }
                }
                if (isRemove) {
                    setExternalRequestList(getSharedPreference(context), newStrings);
                    Log.i(TAG, "removeFromExternalRequestQueue, type = " + type);
                }
            }
        }
        return isRemove;
    }

    static void removeFromExternalRequestQueue(Context context, ExternalRequestInfo inputInfo) {
        synchronized (sLock) {
            Set<String> strings = getExternalRequestList(getSharedPreference(context));
            if (strings != null) {
                Set<String> newStrings = new LinkedHashSet(strings);
                Iterator<String> newStringsIterator = newStrings.iterator();
                while (newStringsIterator.hasNext()) {
                    ExternalRequestInfo info = decode((String) newStringsIterator.next(), context);
                    if (info != null && info.equals(inputInfo)) {
                        newStringsIterator.remove();
                        break;
                    }
                }
                setExternalRequestList(getSharedPreference(context), newStrings);
            }
        }
    }

    static void removeFromExternalRequestQueue(Context context, ArrayList<String> packageNames, UserHandleCompat user) {
        if (!packageNames.isEmpty()) {
            synchronized (sLock) {
                Set<String> strings = getExternalRequestList(getSharedPreference(context));
                if (strings != null) {
                    Set<String> newStrings = new LinkedHashSet(strings);
                    Iterator<String> newStringsIterator = newStrings.iterator();
                    while (newStringsIterator.hasNext()) {
                        ExternalRequestInfo info = decode((String) newStringsIterator.next(), context);
                        if (info == null || (info.getContainPackage(packageNames) && user.equals(info.getUser()))) {
                            newStringsIterator.remove();
                        }
                    }
                    setExternalRequestList(getSharedPreference(context), newStrings);
                }
            }
        }
    }

    private static ExternalRequestInfo decode(String encoded, Context context) {
        try {
            switch (((JSONObject) new JSONTokener(encoded).nextValue()).getInt("type")) {
                case 1:
                    return InstallShortcutReceiver.decode(encoded, context);
                case 2:
                    return UninstallShortcutReceiver.decode(encoded, context);
                case 3:
                    return InstallWidgetReceiver.decode(encoded, context);
                case 4:
                    return UninstallWidgetReceiver.decode(encoded, context);
                case 5:
                    return InstallPairAppsReceiver.decode(encoded, context);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        }
        return null;
    }

    private static void addToExternalRequestQueue(SharedPreferences sharedPrefs, ExternalRequestInfo info) {
        synchronized (sLock) {
            String encoded = info.encodeToString();
            if (encoded != null) {
                Set<String> strings = getExternalRequestList(sharedPrefs);
                if (strings == null) {
                    strings = new LinkedHashSet(1);
                } else {
                    strings = new LinkedHashSet(strings);
                }
                strings.add(encoded);
                setExternalRequestList(sharedPrefs, strings);
            }
        }
    }

    static void queueExternalRequestInfo(ExternalRequestInfo info, Context context, LauncherAppState app) {
        boolean launcherNotLoaded = app.getModel().getCallback() == null;
        if (app.isExternalQueueEnabled() || launcherNotLoaded || !app.getModel().getCallback().isHomeNormal()) {
            addToExternalRequestQueue(getSharedPreference(context), info);
            return;
        }
        info.runRequestInfo(context);
        sendBroadCaseToTaskEdge(context);
    }

    private static void sendBroadCaseToTaskEdge(final Context context) {
        if (LauncherAppState.getInstance().getModel() != null) {
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    Log.d(ExternalRequestQueue.TAG, "flush end intent!");
                    Intent flushEndIntent = new Intent(ExternalRequestQueue.INSTALL_SHORTCUT_FLUSHED);
                    flushEndIntent.setPackage(ExternalRequestQueue.TASK_EDGE_PACKAGE);
                    context.sendBroadcast(flushEndIntent);
                }
            });
        }
    }
}
