package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;

public class PendingAddWidgetInfo extends PendingAddItemInfo {
    private static final String TAG = "PendingAddWidgetInfo";
    public Bundle bindOptions = null;
    public AppWidgetHostView boundWidget;
    public int icon;
    public LauncherAppWidgetProviderInfo info;
    public int previewImage;

    public PendingAddWidgetInfo(Context context, LauncherAppWidgetProviderInfo i, Parcelable data) {
        if (i.isCustomWidget) {
            this.itemType = 5;
        } else {
            this.itemType = 4;
        }
        this.info = i;
        this.user = AppWidgetManagerCompat.getInstance(context).getUser(i);
        this.componentName = i.provider;
        this.previewImage = i.previewImage;
        this.icon = i.icon;
        this.spanX = i.getSpanX();
        this.spanY = i.getSpanY();
        this.minSpanX = i.getMinSpanX();
        this.minSpanY = i.getMinSpanY();
    }

    public boolean isCustomWidget() {
        return this.itemType == 5;
    }

    public String toString() {
        return String.format("PendingAddWidgetInfo package=%s, name=%s", new Object[]{this.componentName.getPackageName(), this.componentName.getShortClassName()});
    }

    public Object getProviderInfo() {
        return this.info;
    }

    public int[] getSpan() {
        Log.d(TAG, "info = " + this.info + ", minWidth = " + this.info.minWidth + ", minHeight = " + this.info.minHeight);
        return new int[]{this.info.getSpanX(), this.info.getSpanY()};
    }

    public String getLabel(Context context) {
        if (this.mLabel == null) {
            this.mLabel = AppWidgetManagerCompat.getInstance(context).loadLabel(this.info);
        }
        return this.mLabel;
    }
}
