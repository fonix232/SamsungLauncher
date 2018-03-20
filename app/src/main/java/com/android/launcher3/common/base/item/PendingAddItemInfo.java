package com.android.launcher3.common.base.item;

import android.content.Context;
import com.android.launcher3.Utilities;

public abstract class PendingAddItemInfo extends ItemInfo {
    protected static final int TYPE_INSTALLED_APP = 1;
    protected static final int TYPE_NONE = -1;
    protected static final int TYPE_SYSTEM_APP = 0;
    private String mAppLabel;
    public String mLabel;
    protected int mUninstallable = -1;

    public abstract String getLabel(Context context);

    public abstract Object getProviderInfo();

    public abstract int[] getSpan();

    public boolean uninstallable(Context context) {
        if (this.mUninstallable == -1) {
            int i;
            if (Utilities.canUninstall(context, this.componentName.getPackageName())) {
                i = 1;
            } else {
                i = 0;
            }
            this.mUninstallable = i;
        }
        if (this.mUninstallable == 1) {
            return true;
        }
        return false;
    }

    public void setApplicationLabel(String appLabel) {
        this.mAppLabel = appLabel;
    }

    public String getApplicationLabel() {
        return this.mAppLabel;
    }
}
