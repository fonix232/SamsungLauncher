package com.android.launcher3.remoteconfiguration;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ExternalMethodInfo {
    private static final String TAG = "ExternalMethodInfo";
    static final String TYPE_BUNDLE = "bundle";
    static final String TYPE_METHOD = "method";
    static final String TYPE_TIME = "time";
    private final Bundle mRequestBundle;
    private final String mRequestMethod;
    private final long mRequestTime;

    ExternalMethodInfo(String method, Bundle bundle, long time) {
        this.mRequestMethod = method;
        this.mRequestBundle = bundle;
        if (time < 0) {
            time = SystemClock.uptimeMillis();
        }
        this.mRequestTime = time;
    }

    long getRequestTime() {
        return this.mRequestTime;
    }

    String encodeToString() {
        Gson gson = new GsonBuilder().create();
        try {
            JSONObject object;
            JSONArray bundle = new JSONArray();
            if (this.mRequestBundle != null) {
                for (String key : this.mRequestBundle.keySet()) {
                    object = new JSONObject();
                    String value = gson.toJson(this.mRequestBundle.get(key));
                    String classPath = this.mRequestBundle.get(key).getClass().getCanonicalName();
                    object.put(key, value);
                    object.put(value, classPath);
                    bundle.put(object);
                }
            }
            object = new JSONObject();
            object.put(TYPE_METHOD, gson.toJson(this.mRequestMethod));
            object.put(TYPE_TIME, gson.toJson(Long.valueOf(this.mRequestTime)));
            object.put(TYPE_BUNDLE, bundle);
            return object.toString();
        } catch (JSONException e) {
            Log.d(TAG, "Exception when encodeToString: " + e);
            return null;
        }
    }

    void runMethodInfo(RemoteConfigurationManager remoteConfigurationManager) {
        if (remoteConfigurationManager != null) {
            remoteConfigurationManager.handleRemoteConfigurationCall(this.mRequestMethod, null, this.mRequestBundle);
        }
    }
}
