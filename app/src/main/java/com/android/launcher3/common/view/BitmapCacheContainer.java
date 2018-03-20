package com.android.launcher3.common.view;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;
import com.android.launcher3.common.compat.UserHandleCompat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/* compiled from: LiveIconManager */
class BitmapCacheContainer {
    private static String TAG = "BitmapCacheContainer";
    private Map<Pair<String, UserHandleCompat>, BitmapInfo> mCache = new ConcurrentHashMap();

    /* compiled from: LiveIconManager */
    private static class BitmapInfo {
        public Bitmap bitmap;
        long timestamp = new Date().getTime();

        BitmapInfo(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        boolean isExpired() {
            return new Date().getTime() - this.timestamp >= 60000;
        }
    }

    BitmapCacheContainer() {
    }

    @Nullable
    Bitmap getBitmapCache(String packageName, UserHandleCompat user) {
        BitmapInfo bitmapInfo = (BitmapInfo) this.mCache.get(Pair.create(packageName, user));
        if (bitmapInfo == null) {
            return null;
        }
        if (!bitmapInfo.isExpired()) {
            return bitmapInfo.bitmap;
        }
        Log.i(TAG, "getBitmapCache: BitmapCache expired " + packageName + "/" + user);
        return null;
    }

    void putBitmapCache(String packageName, UserHandleCompat user, Bitmap bitmap) {
        this.mCache.put(Pair.create(packageName, user), new BitmapInfo(bitmap));
    }

    void removeBitmapCache(String packageName) {
        for (Entry<Pair<String, UserHandleCompat>, BitmapInfo> entry : this.mCache.entrySet()) {
            Pair<String, UserHandleCompat> key = (Pair) entry.getKey();
            if (key != null && key.first.equals(packageName)) {
                this.mCache.remove(key);
            }
        }
    }
}
