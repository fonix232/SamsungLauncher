package com.android.launcher3.executor;

import android.content.ComponentName;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.android.launcher3.proxy.LauncherProxy.AppInfo;

class StateAppInfo implements AppInfo {
    private ComponentName mComponentName = null;
    private ItemInfo mItemInfo;
    private String mName;
    private int mOrdinalNumber = LauncherProxy.INVALID_VALUE;

    StateAppInfo() {
    }

    public String getName() {
        if (this.mName == null || "".equals(this.mName.trim())) {
            return " ";
        }
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public ItemInfo getItemInfo() {
        return this.mItemInfo;
    }

    public int getOrdinalNumber() {
        return this.mOrdinalNumber;
    }

    public void clear() {
        this.mName = null;
        this.mItemInfo = null;
        this.mComponentName = null;
    }

    void setOrdinalNumber(int num) {
        this.mOrdinalNumber = num;
    }

    void setComponentName(String name) {
        this.mComponentName = null;
        if (name == null || !name.contains("_")) {
            this.mName = name;
            return;
        }
        String[] cnList = name.split("_");
        if (cnList.length == 2) {
            this.mComponentName = ComponentName.createRelative(cnList[1], cnList[0]);
        } else {
            this.mName = name;
        }
    }

    void setItemInfo(ItemInfo itemInfo) {
        this.mItemInfo = itemInfo;
        if (this.mItemInfo == null) {
            return;
        }
        if (this.mItemInfo.itemType == 0 || this.mItemInfo.itemType == 1 || this.mItemInfo.itemType == 6) {
            if (this.mItemInfo.getIntent() != null) {
                this.mItemInfo.componentName = this.mItemInfo.getIntent().getComponent();
            }
            if (this.mItemInfo.title != null) {
                this.mName = this.mItemInfo.title.toString();
            }
        }
    }

    boolean isValid() {
        if ((this.mName == null || "".equals(this.mName)) && this.mComponentName == null) {
            return false;
        }
        return true;
    }
}
