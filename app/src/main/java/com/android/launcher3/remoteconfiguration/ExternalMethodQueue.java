package com.android.launcher3.remoteconfiguration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ExternalMethodQueue {
    private static final String EXTERNAL_METHOD_LIST_HOME_APPS = "external_method_list_home_apps";
    private static final String EXTERNAL_METHOD_LIST_HOME_ONLY = "external_method_list_home_only";
    private static final String TAG = "ExternalMethodQueue";
    private static final Object sLock = new Object();
    private static SharedPreferences sSharedPref = null;

    public static void disableAndFlushExternalMethodQueue(Context context, LauncherAppState app) {
        app.enableExternalQueue(false);
        flushExternalMethodQueue(context, app);
    }

    private static void flushExternalMethodQueue(Context context, LauncherAppState app) {
        if (app.getModel().getCallback().isHomeNormal()) {
            ArrayList<ExternalMethodInfo> installQueue = getAndClearExternalMethodQueue(getSharedPreference(context));
            Collections.sort(installQueue, new Comparator<ExternalMethodInfo>() {
                public int compare(ExternalMethodInfo lhs, ExternalMethodInfo rhs) {
                    return Long.compare(lhs.getRequestTime(), rhs.getRequestTime());
                }
            });
            if (!installQueue.isEmpty()) {
                Iterator<ExternalMethodInfo> iterator = installQueue.iterator();
                RemoteConfigurationManager rcm = LauncherAppState.getLauncherProvider().getRemoteConfigurationManager();
                while (iterator.hasNext()) {
                    ((ExternalMethodInfo) iterator.next()).runMethodInfo(rcm);
                }
            }
        }
    }

    static boolean queueExternalMethodInfo(ExternalMethodInfo info, Context context) {
        boolean launcherLoaded;
        if (((Launcher) LauncherAppState.getInstance().getModel().getCallback()) != null) {
            launcherLoaded = true;
        } else {
            launcherLoaded = false;
        }
        if (launcherLoaded) {
            return false;
        }
        addToExternalMethodQueue(info, context);
        return true;
    }

    private static void addToExternalMethodQueue(ExternalMethodInfo info, Context context) {
        synchronized (sLock) {
            String encoded = info.encodeToString();
            if (encoded != null) {
                Set<String> strings = getExternalMethodList(getSharedPreference(context));
                if (strings == null) {
                    strings = new LinkedHashSet(1);
                } else {
                    strings = new LinkedHashSet(strings);
                }
                Log.d(TAG, "Adding encoded sting EXTERNAL_METHOD_LIST: " + encoded);
                strings.add(encoded);
                setExternalMethodList(getSharedPreference(context), strings);
            }
        }
    }

    private static ArrayList<ExternalMethodInfo> getAndClearExternalMethodQueue(SharedPreferences sharedPrefs) {
        ArrayList<ExternalMethodInfo> arrayList;
        synchronized (sLock) {
            Set<String> strings = getExternalMethodList(sharedPrefs);
            Log.d(TAG, "Getting and clearing EXTERNAL_METHOD_LIST: " + strings);
            if (strings == null) {
                arrayList = new ArrayList();
            } else {
                arrayList = new ArrayList();
                for (String encoded : strings) {
                    ExternalMethodInfo info = decode(encoded);
                    if (info != null) {
                        arrayList.add(info);
                    }
                }
                setExternalMethodList(sharedPrefs, new LinkedHashSet());
            }
        }
        return arrayList;
    }

    private static SharedPreferences getSharedPreference(Context context) {
        if (sSharedPref == null) {
            sSharedPref = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        }
        return sSharedPref;
    }

    private static void setExternalMethodList(SharedPreferences sharedPrefs, Set<String> strings) {
        sharedPrefs.edit().putStringSet(LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? EXTERNAL_METHOD_LIST_HOME_ONLY : EXTERNAL_METHOD_LIST_HOME_APPS, strings).apply();
    }

    private static Set<String> getExternalMethodList(SharedPreferences sharedPrefs) {
        return sharedPrefs.getStringSet(LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? EXTERNAL_METHOD_LIST_HOME_ONLY : EXTERNAL_METHOD_LIST_HOME_APPS, null);
    }

    private static ExternalMethodInfo decode(String encoded) {
        Exception e;
        Gson gson = new GsonBuilder().create();
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            String gsonMethod = object.getString("method");
            String gsonTime = object.getString("time");
            JSONArray array = object.getJSONArray("bundle");
            return new ExternalMethodInfo((String) gson.fromJson(gsonMethod, String.class), toBundle(array, gson), ((Long) gson.fromJson(gsonTime, Long.TYPE)).longValue());
        } catch (JSONException e2) {
            e = e2;
        } catch (IOException e3) {
            e = e3;
        }
        Log.d(TAG, "Exception decode externalMethodInfo: " + e);
        return null;
    }

    private static Bundle toBundle(JSONArray bundleArray, Gson gson) throws IOException {
        Bundle bundle = new Bundle();
        int bundleCount = bundleArray.length();
        String value = null;
        String key = null;
        String classPath = null;
        int i = 0;
        while (i < bundleCount) {
            try {
                JSONObject arrayObject = bundleArray.getJSONObject(i);
                Iterator iterator = arrayObject.keys();
                if (iterator.hasNext()) {
                    key = iterator.next().toString();
                    value = arrayObject.getString(key);
                }
                if (value != null) {
                    classPath = arrayObject.getString(value);
                    if (classPath == null) {
                        Log.d(TAG, "classPath of " + value + "is null");
                        return null;
                    }
                    Object bundleObject = gson.fromJson(value, Class.forName(classPath));
                    if (bundleObject instanceof String) {
                        bundle.putString(key, (String) bundleObject);
                    } else if (bundleObject instanceof Integer) {
                        bundle.putInt(key, ((Integer) bundleObject).intValue());
                    } else if (bundleObject instanceof Boolean) {
                        bundle.putBoolean(key, ((Boolean) bundleObject).booleanValue());
                    } else if (bundleObject instanceof Parcelable) {
                        bundle.putParcelable(key, (Parcelable) bundleObject);
                    } else {
                        throw new IOException("Unsupported key, value: " + key + ", " + bundleObject);
                    }
                }
                i++;
            } catch (JSONException e) {
                e = e;
            } catch (ClassNotFoundException e2) {
                e = e2;
            }
        }
        return bundle;
        Exception e3;
        Log.d(TAG, "Exception toBundle: " + e3 + " classPath : " + classPath + " key : " + key);
        return bundle;
    }
}
