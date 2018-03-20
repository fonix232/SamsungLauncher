package com.android.launcher3.common.base.item;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import java.util.Arrays;

public class ItemInfo {
    public static final String EXTRA_PROFILE = "profile";
    public static final int NO_ID = -1;
    public int cellX;
    public int cellY;
    public ComponentName componentName;
    public long container;
    public CharSequence contentDescription;
    public int[] dropPos;
    public int hidden;
    public long id;
    public boolean ignoreCheckItemInfo;
    boolean isGameApp;
    public int itemType;
    public int lock;
    public int mBadgeCount;
    public boolean mChecked;
    private long mDexID;
    public boolean mDirty;
    private long mPrevContainer;
    public boolean mShowBadge;
    public int minSpanX;
    public int minSpanY;
    public long oldScreenId;
    public int rank;
    public boolean requiresDbUpdate;
    public long screenId;
    public int spanX;
    public int spanY;
    public CharSequence title;
    public UserHandleCompat user;

    public long getDexID() {
        return this.mDexID;
    }

    public void setDexID(long dexID) {
        this.mDexID = dexID;
    }

    public long getPrevContainer() {
        return this.mPrevContainer;
    }

    public void setPrevContainer(long container) {
        this.mPrevContainer = container;
    }

    public boolean isGameApp() {
        return this.isGameApp;
    }

    public void setGameApp(boolean gameApp) {
        this.isGameApp = gameApp;
    }

    public ItemInfo() {
        this.id = -1;
        this.container = -1;
        this.screenId = -1;
        this.oldScreenId = -1;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.rank = 0;
        this.requiresDbUpdate = false;
        this.mShowBadge = false;
        this.mDirty = false;
        this.dropPos = null;
        this.componentName = null;
        this.mChecked = false;
        this.ignoreCheckItemInfo = false;
        this.mDexID = -1;
        this.mPrevContainer = -1;
        this.isGameApp = false;
        this.user = UserHandleCompat.myUserHandle();
    }

    public ItemInfo(ItemInfo info) {
        this.id = -1;
        this.container = -1;
        this.screenId = -1;
        this.oldScreenId = -1;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.rank = 0;
        this.requiresDbUpdate = false;
        this.mShowBadge = false;
        this.mDirty = false;
        this.dropPos = null;
        this.componentName = null;
        this.mChecked = false;
        this.ignoreCheckItemInfo = false;
        this.mDexID = -1;
        this.mPrevContainer = -1;
        this.isGameApp = false;
        copyFrom(info);
        if (info.container == -100) {
            FavoritesUpdater.checkItemInfo(this);
        }
    }

    public void copyFrom(ItemInfo info) {
        this.id = info.id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.rank = info.rank;
        this.title = info.title;
        this.screenId = info.screenId;
        this.itemType = info.itemType;
        this.container = info.container;
        this.user = info.user;
        this.componentName = info.componentName;
        this.contentDescription = info.contentDescription;
        this.hidden = info.hidden;
    }

    public Intent getIntent() {
        throw new RuntimeException("Unexpected Intent");
    }

    public UserHandleCompat getUserHandle() {
        return this.user;
    }

    public void onAddToDatabase(Context context, ContentValues values) {
        values.put("itemType", Integer.valueOf(this.itemType));
        values.put("container", Long.valueOf(this.container));
        values.put("screen", Long.valueOf(this.screenId));
        values.put("cellX", Integer.valueOf(this.cellX));
        values.put("cellY", Integer.valueOf(this.cellY));
        values.put("spanX", Integer.valueOf(this.spanX));
        values.put("spanY", Integer.valueOf(this.spanY));
        values.put(BaseLauncherColumns.RANK, Integer.valueOf(this.rank));
        values.put("hidden", Integer.valueOf(this.hidden));
        values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(UserManagerCompat.getInstance(context).getSerialNumberForUser(this.user)));
        values.put("_id", Long.valueOf(this.id));
        if (this.screenId == -201) {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }
    }

    public static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            values.put("icon", Utilities.flattenBitmap(bitmap));
        }
    }

    public void unbind() {
    }

    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + " hidden=" + this.hidden + ")";
    }

    public boolean isContainApps() {
        return this != null && this.container == -102;
    }

    public boolean isAppOrShortcutType() {
        if (this == null) {
            return false;
        }
        return this.itemType == 0 || this.itemType == 1 || this.itemType == 2 || this.itemType == 6 || this.itemType == 7;
    }

    public boolean getChecked() {
        return this.mChecked;
    }

    public void setChecked(boolean checked) {
        this.mChecked = checked;
    }

    public int setHidden(int hiddenFlag) {
        int i = this.hidden | hiddenFlag;
        this.hidden = i;
        return i;
    }

    public int setUnHidden(int hiddenFlag) {
        int i = this.hidden & (hiddenFlag ^ -1);
        this.hidden = i;
        return i;
    }

    private boolean isHiddenBy(int hiddenFlag) {
        switch (hiddenFlag) {
            case 1:
                if ((this.hidden & 1) == 0) {
                    return false;
                }
                return true;
            case 2:
                if ((this.hidden & 2) == 0) {
                    return false;
                }
                return true;
            case 4:
                return (this.hidden & 4) != 0;
            default:
                return false;
        }
    }

    public boolean isHiddenByXML() {
        return isHiddenBy(1);
    }

    public boolean isHiddenByUser() {
        return isHiddenBy(2);
    }

    public boolean isHiddenByGame() {
        return isHiddenBy(4);
    }
}
