package com.android.launcher3.common.compat;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class PinItemRequestCompat implements Parcelable {
    public static final Creator<PinItemRequestCompat> CREATOR = new Creator<PinItemRequestCompat>() {
        public PinItemRequestCompat createFromParcel(Parcel source) {
            return new PinItemRequestCompat(source.readParcelable(PinItemRequestCompat.class.getClassLoader()));
        }

        public PinItemRequestCompat[] newArray(int size) {
            return new PinItemRequestCompat[size];
        }
    };
    public static final String EXTRA_IS_FROM_LAUNCHER = "com.android.launcher3.extra.IS_FROM_LAUNCHER";
    public static final String EXTRA_IS_FROM_WORKSPACE = "com.android.launcher3.extra.IS_FROM_WORKSPACE";
    public static final String EXTRA_PIN_ITEM_REQUEST = "android.content.pm.extra.PIN_ITEM_REQUEST";
    public static final int REQUEST_TYPE_APPWIDGET = 2;
    public static final int REQUEST_TYPE_SHORTCUT = 1;
    private final Parcelable mObject;

    private PinItemRequestCompat(Parcelable object) {
        this.mObject = object;
    }

    public int getRequestType() {
        return ((Integer) invokeMethod("getRequestType")).intValue();
    }

    public ShortcutInfo getShortcutInfo() {
        return (ShortcutInfo) invokeMethod("getShortcutInfo");
    }

    public AppWidgetProviderInfo getAppWidgetProviderInfo(Context context) {
        try {
            return (AppWidgetProviderInfo) this.mObject.getClass().getDeclaredMethod("getAppWidgetProviderInfo", new Class[]{Context.class}).invoke(this.mObject, new Object[]{context});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValid() {
        return ((Boolean) invokeMethod("isValid")).booleanValue();
    }

    public boolean accept() {
        return ((Boolean) invokeMethod("accept")).booleanValue();
    }

    public boolean accept(Bundle options) {
        try {
            return ((Boolean) this.mObject.getClass().getDeclaredMethod("accept", new Class[]{Bundle.class}).invoke(this.mObject, new Object[]{options})).booleanValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Bundle getExtras() {
        try {
            return (Bundle) this.mObject.getClass().getDeclaredMethod("getExtras", new Class[0]).invoke(this.mObject, new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Object invokeMethod(String methodName) {
        try {
            return this.mObject.getClass().getDeclaredMethod(methodName, new Class[0]).invoke(this.mObject, new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mObject, i);
    }

    public static PinItemRequestCompat getPinItemRequest(Intent intent) {
        if (VERSION.SDK_INT < 26 || intent == null) {
            return null;
        }
        Parcelable extra = intent.getParcelableExtra(EXTRA_PIN_ITEM_REQUEST);
        if (extra != null) {
            return new PinItemRequestCompat(extra);
        }
        return null;
    }
}
