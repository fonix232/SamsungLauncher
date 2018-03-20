package com.android.launcher3.common.model;

import android.content.Context;
import android.content.Intent.ShortcutIconResource;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.BitmapUtils;

public class CursorInfo {
    public final int appWidgetIdIndex;
    public final int appWidgetProviderIndex;
    public final int cellXIndex;
    public final int cellYIndex;
    public final int colorIndex;
    public final int containerIndex;
    public final int festivalIndex;
    public final int hiddenIndex;
    public final int iconIndex;
    public final int iconPackageIndex;
    public final int iconResourceIndex;
    public final int iconTypeIndex;
    public final int idIndex;
    public final int intentIndex;
    public final int itemTypeIndex;
    public final int lockIndex;
    public final int newCueIndex;
    public final int optionsIndex;
    public final int profileIdIndex;
    public final int rankIndex;
    public final int restoredIndex;
    public final int screenIndex;
    public final int spanXIndex;
    public final int spanYIndex;
    public final int titleIndex;

    public CursorInfo(Cursor c) {
        this.idIndex = c.getColumnIndexOrThrow("_id");
        this.intentIndex = c.getColumnIndexOrThrow("intent");
        this.titleIndex = c.getColumnIndexOrThrow("title");
        this.containerIndex = c.getColumnIndexOrThrow("container");
        this.itemTypeIndex = c.getColumnIndexOrThrow("itemType");
        this.appWidgetIdIndex = c.getColumnIndexOrThrow(Favorites.APPWIDGET_ID);
        this.appWidgetProviderIndex = c.getColumnIndexOrThrow(Favorites.APPWIDGET_PROVIDER);
        this.screenIndex = c.getColumnIndexOrThrow("screen");
        this.cellXIndex = c.getColumnIndexOrThrow("cellX");
        this.cellYIndex = c.getColumnIndexOrThrow("cellY");
        this.spanXIndex = c.getColumnIndexOrThrow("spanX");
        this.spanYIndex = c.getColumnIndexOrThrow("spanY");
        this.rankIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.RANK);
        this.restoredIndex = c.getColumnIndexOrThrow("restored");
        this.profileIdIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.PROFILE_ID);
        this.optionsIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.OPTIONS);
        this.iconTypeIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.ICON_TYPE);
        this.iconIndex = c.getColumnIndexOrThrow("icon");
        this.iconPackageIndex = c.getColumnIndexOrThrow("iconPackage");
        this.iconResourceIndex = c.getColumnIndexOrThrow("iconResource");
        this.colorIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.COLOR);
        this.hiddenIndex = c.getColumnIndexOrThrow("hidden");
        this.newCueIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.NEWCUE);
        this.festivalIndex = c.getColumnIndexOrThrow(Favorites.FESTIVAL);
        this.lockIndex = c.getColumnIndexOrThrow(ChangeLogColumns.LOCK);
    }

    public Bitmap loadIcon(Cursor c, IconInfo info, Context context) {
        boolean z = true;
        Bitmap icon = null;
        int iconType = c.getInt(this.iconTypeIndex);
        if (iconType == 0) {
            String packageName = c.getString(this.iconPackageIndex);
            String resourceName = c.getString(this.iconResourceIndex);
            if (!(TextUtils.isEmpty(packageName) && TextUtils.isEmpty(resourceName))) {
                info.iconResource = new ShortcutIconResource();
                info.iconResource.packageName = packageName;
                info.iconResource.resourceName = resourceName;
                icon = BitmapUtils.createIconBitmap(packageName, resourceName, context);
            }
            if (icon == null) {
                return BitmapUtils.createIconBitmap(c, this.iconIndex, context);
            }
            return icon;
        } else if (iconType != 1) {
            return null;
        } else {
            icon = BitmapUtils.createIconBitmap(c, this.iconIndex, context);
            if (icon == null) {
                z = false;
            }
            info.customIcon = z;
            return icon;
        }
    }
}
