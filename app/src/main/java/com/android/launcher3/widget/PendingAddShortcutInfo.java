package com.android.launcher3.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;

public class PendingAddShortcutInfo extends PendingAddItemInfo {
    ActivityInfo activityInfo;
    ResolveInfo resolveInfo;

    public PendingAddShortcutInfo(ResolveInfo resolveInfo) {
        this.resolveInfo = resolveInfo;
        this.activityInfo = resolveInfo.activityInfo;
        this.componentName = new ComponentName(this.activityInfo.packageName, this.activityInfo.name);
        this.itemType = 1;
    }

    public String toString() {
        return String.format("PendingAddShortcutInfo package=%s, name=%s", new Object[]{this.activityInfo.packageName, this.activityInfo.name});
    }

    public ActivityInfo getActivityInfo() {
        return this.activityInfo;
    }

    public Object getProviderInfo() {
        return this.resolveInfo;
    }

    public int[] getSpan() {
        return new int[]{1, 1};
    }

    public String getLabel(Context context) {
        if (this.mLabel == null) {
            CharSequence label = this.activityInfo.loadLabel(context.getPackageManager());
            this.mLabel = label != null ? label.toString() : "";
        }
        return this.mLabel;
    }
}
