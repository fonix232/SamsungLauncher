package com.android.launcher3.common.customer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.PostPositionProvider.ITEM_TYPE;
import com.android.launcher3.util.BitmapUtils;
import java.net.URISyntaxException;

public class PostPositionItemRecord {
    private static final String TAG = "ItemRecord";
    private boolean mAppsAdd = false;
    private int mAppsCellOrder = -1;
    private String mAppsFolderName;
    private boolean mAppsForceAtoZ = false;
    private int mAppsIndex = -1;
    private boolean mAppsPreloadFolder = true;
    private String mComponentName;
    private boolean mHomeAdd = false;
    private int mHomeCellX = -1;
    private int mHomeCellY = -1;
    private String mHomeFolderName;
    private int mHomeIndex = -1;
    private boolean mHomeNewPage = false;
    private boolean mHomePreloadFolder = true;
    private boolean mHomeReplace = false;
    private Bitmap mHomeShortcutIcon = null;
    private String mHomeShortcutTitle = null;
    private ITEM_TYPE mItemType = ITEM_TYPE.APP;
    private boolean mRemoveAfterPosition = false;
    private int mWidgetSpanX = -1;
    private int mWidgetSpanY = -1;

    PostPositionItemRecord(String cmpName, int itemType) {
        this.mComponentName = cmpName;
        this.mItemType = ITEM_TYPE.values()[itemType];
    }

    public String getComponentName() {
        return this.mComponentName;
    }

    public ITEM_TYPE getItemType() {
        return this.mItemType;
    }

    public boolean isHomeAdd() {
        return this.mHomeAdd;
    }

    public boolean isHomePreloadFolder() {
        return this.mHomePreloadFolder;
    }

    public String getHomeFolderName() {
        return this.mHomeFolderName;
    }

    public boolean isAppsPreloadFolder() {
        return this.mAppsPreloadFolder;
    }

    public String getAppsFolderName() {
        return this.mAppsFolderName;
    }

    public boolean isHomeNewPage() {
        return this.mHomeNewPage;
    }

    public boolean isHomeReplace() {
        return this.mHomeReplace;
    }

    public int getHomeIndex() {
        return this.mHomeIndex;
    }

    public int getAppsIndex() {
        return this.mAppsIndex;
    }

    public int getHomeCellX() {
        return this.mHomeCellX;
    }

    public int getHomeCellY() {
        return this.mHomeCellY;
    }

    public int getWidgetSpanX() {
        return this.mWidgetSpanX;
    }

    public int getWidgetSpanY() {
        return this.mWidgetSpanY;
    }

    public String getHomeShortcutTitle() {
        return this.mHomeShortcutTitle;
    }

    public Bitmap getHomeShortcutIcon() {
        return this.mHomeShortcutIcon;
    }

    public boolean isAppsAdd() {
        return this.mAppsAdd;
    }

    public boolean isRemoveAfterPosition() {
        return this.mRemoveAfterPosition;
    }

    public void setHomePosition(boolean newpage, boolean replace, int index, int x, int y) {
        this.mHomeNewPage = newpage;
        this.mHomeReplace = replace;
        this.mHomeIndex = index;
        this.mHomeCellX = x;
        this.mHomeCellY = y;
    }

    public void setWidgetSpanXY(int x, int y) {
        this.mWidgetSpanX = x;
        this.mWidgetSpanY = y;
    }

    void setShortcutInfo(String title, byte[] icon, Context context) {
        try {
            this.mHomeShortcutTitle = title;
            this.mHomeShortcutIcon = BitmapUtils.createIconBitmap(BitmapFactory.decodeByteArray(icon, 0, icon.length), context);
            if (this.mHomeShortcutIcon == null) {
                this.mHomeShortcutIcon = LauncherAppState.getInstance().getIconCache().getDefaultIcon(UserHandleCompat.myUserHandle());
            }
        } catch (Exception e) {
            Log.e(TAG, "setShortcutInfo() " + e.getMessage());
        }
    }

    public void setShortcutInfo(String title, Bitmap icon) {
        this.mHomeShortcutTitle = title;
        this.mHomeShortcutIcon = icon;
    }

    public Intent getShorcutIntent() {
        Intent shortcutIntent = new Intent();
        try {
            shortcutIntent.putExtra("android.intent.extra.shortcut.INTENT", Intent.parseUri(this.mComponentName, 0));
            shortcutIntent.putExtra("android.intent.extra.shortcut.NAME", this.mHomeShortcutTitle);
            shortcutIntent.putExtra("android.intent.extra.shortcut.ICON", this.mHomeShortcutIcon);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return shortcutIntent;
    }

    public void setAppsPosition(int index, int order) {
        this.mAppsIndex = index;
        this.mAppsCellOrder = order;
    }

    boolean isValid() {
        if (this.mHomeAdd && !this.mHomePreloadFolder && ((this.mHomeFolderName == null || this.mHomeFolderName.equals("")) && this.mHomeIndex < 0 && this.mHomeCellX < 0 && this.mHomeCellY < 0)) {
            return false;
        }
        if (this.mAppsAdd && !this.mAppsPreloadFolder && (this.mAppsFolderName == null || this.mAppsFolderName.equals(""))) {
            return false;
        }
        return true;
    }

    public void setHomeAdded(boolean added) {
        this.mHomeAdd = added;
    }

    public void setAppsAdded(boolean added) {
        this.mAppsAdd = added;
    }

    public void setHomePreloadFolder(boolean preload) {
        this.mHomePreloadFolder = preload;
    }

    public void setAppsPreloadFolder(boolean preload) {
        this.mAppsPreloadFolder = preload;
    }

    public void setHomeFolderName(String folderName) {
        this.mHomeFolderName = folderName;
    }

    public void setAppsFolderName(String folderName) {
        this.mAppsFolderName = folderName;
    }

    public void setAppsForceAtoZ(boolean atoz) {
        this.mAppsForceAtoZ = atoz;
    }

    public void setRemoveAfterPosition(boolean remove) {
        this.mRemoveAfterPosition = remove;
    }
}
