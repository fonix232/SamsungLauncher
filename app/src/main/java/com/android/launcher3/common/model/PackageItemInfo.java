package com.android.launcher3.common.model;

import android.graphics.Bitmap;
import com.android.launcher3.common.base.item.ItemInfo;
import java.util.Arrays;

public class PackageItemInfo extends ItemInfo {
    int flags = 0;
    public Bitmap iconBitmap;
    public String packageName;
    public String titleSectionName;
    public boolean usingLowResIcon;

    public PackageItemInfo(String packageName) {
        this.packageName = packageName;
    }

    public String toString() {
        return "PackageItemInfo(title=" + this.title + " id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }
}
