package com.android.launcher3.widget;

import android.content.Context;
import com.android.launcher3.common.base.item.PendingAddItemInfo;

public class PendingAddPinShortcutInfo extends PendingAddItemInfo {
    PinShortcutRequestActivityInfo mShortcutInfo;

    public PendingAddPinShortcutInfo(PinShortcutRequestActivityInfo shortcutInfo) {
        this.mShortcutInfo = shortcutInfo;
        this.componentName = shortcutInfo.getComponentName();
        this.itemType = 6;
    }

    public String toString() {
        if (this.componentName == null) {
            return "";
        }
        return String.format("PendingAddPinShortcutInfo package=%s, name=%s", new Object[]{this.componentName.getPackageName(), this.componentName.getClassName()});
    }

    public Object getProviderInfo() {
        return null;
    }

    public PinShortcutRequestActivityInfo getShortcutInfo() {
        return this.mShortcutInfo;
    }

    public int[] getSpan() {
        return new int[]{1, 1};
    }

    public String getLabel(Context context) {
        if (this.mLabel == null) {
            this.mLabel = this.mShortcutInfo != null ? String.valueOf(this.mShortcutInfo.getLabel()) : "";
        }
        return this.mLabel;
    }
}
