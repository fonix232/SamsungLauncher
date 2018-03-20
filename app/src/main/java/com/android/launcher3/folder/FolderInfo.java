package com.android.launcher3.folder;

import android.content.ContentValues;
import android.content.Context;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.AppNameComparator;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class FolderInfo extends ItemInfo {
    public static final int FLAG_USE_CUSTOM_COLOR = 8;
    public static final int FLAG_WORK_FOLDER = 2;
    private static final Comparator<ItemInfo> ITEM_POS_COMPARATOR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.rank != rhs.rank) {
                if (lhs.rank < 0) {
                    return 1;
                }
                if (rhs.rank < 0) {
                    return -1;
                }
                return lhs.rank - rhs.rank;
            } else if (lhs.cellY != rhs.cellY) {
                return lhs.cellY - rhs.cellY;
            } else {
                return lhs.cellX - rhs.cellX;
            }
        }
    };
    private static AppNameComparator mAppNameComparator;
    public int color;
    public ArrayList<IconInfo> contents;
    private ArrayList<FolderEventListener> listeners;
    private boolean mAlphabeticalOrder;
    private Comparator<ItemInfo> mCurrentComparator;
    private boolean mLocked;
    private boolean mLockedFolderOpenedOnce;
    public boolean opened;
    public int options;

    public FolderInfo() {
        this.mCurrentComparator = ITEM_POS_COMPARATOR;
        this.mAlphabeticalOrder = false;
        this.contents = new ArrayList();
        this.listeners = new ArrayList();
        this.itemType = 2;
        this.user = UserHandleCompat.myUserHandle();
    }

    public void sortContents() {
        Collections.sort(this.contents, this.mCurrentComparator);
    }

    public void add(IconInfo item) {
        if (item != null) {
            this.contents.add(item);
            for (int i = 0; i < this.listeners.size(); i++) {
                ((FolderEventListener) this.listeners.get(i)).onItemAdded(item);
            }
        }
    }

    public void add(ArrayList<IconInfo> items) {
        if (items != null && !items.isEmpty()) {
            this.contents.addAll(items);
            for (int i = 0; i < this.listeners.size(); i++) {
                ((FolderEventListener) this.listeners.get(i)).onItemsAdded(items);
            }
        }
    }

    public void remove(IconInfo item) {
        this.contents.remove(item);
        for (int i = 0; i < this.listeners.size(); i++) {
            ((FolderEventListener) this.listeners.get(i)).onItemRemoved(item);
        }
    }

    public void remove(ArrayList<IconInfo> items) {
        this.contents.removeAll(items);
        for (int i = 0; i < this.listeners.size(); i++) {
            ((FolderEventListener) this.listeners.get(i)).onItemsRemoved(items);
        }
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < this.listeners.size(); i++) {
            ((FolderEventListener) this.listeners.get(i)).onTitleChanged(title);
        }
    }

    public void setAlphabeticalOrder(boolean alphabeticalOrder, boolean forced, Context context) {
        if (this.mAlphabeticalOrder != alphabeticalOrder || forced) {
            this.mAlphabeticalOrder = alphabeticalOrder;
            if (alphabeticalOrder) {
                if ((mAppNameComparator == null || forced) && context != null) {
                    mAppNameComparator = new AppNameComparator(context);
                }
                if (mAppNameComparator != null) {
                    this.mCurrentComparator = mAppNameComparator.getAppInfoComparator();
                }
            } else {
                this.mCurrentComparator = ITEM_POS_COMPARATOR;
            }
            Collections.sort(this.contents, this.mCurrentComparator);
            for (int i = 0; i < this.listeners.size(); i++) {
                ((FolderEventListener) this.listeners.get(i)).onOrderingChanged(alphabeticalOrder);
            }
        }
    }

    public boolean isAlphabeticalOrder() {
        return this.mAlphabeticalOrder;
    }

    public void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put("title", this.title != null ? this.title.toString() : "");
        values.put(BaseLauncherColumns.OPTIONS, Integer.valueOf(this.options));
        values.put(BaseLauncherColumns.COLOR, Integer.valueOf(this.color));
    }

    public void addListener(FolderEventListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(FolderEventListener listener) {
        if (this.listeners.contains(listener)) {
            this.listeners.remove(listener);
        }
    }

    public void setLockedFolderOpenedOnce(boolean opened) {
        this.mLockedFolderOpenedOnce = opened;
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((FolderEventListener) it.next()).onLockedFolderOpenStateUpdated(Boolean.valueOf(opened));
        }
    }

    public boolean isLockedFolderOpenedOnce() {
        return this.mLockedFolderOpenedOnce;
    }

    public boolean isLocked() {
        return this.mLocked;
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
    }

    public void unbind() {
        super.unbind();
        if (!this.opened) {
            this.listeners.clear();
        }
    }

    public Object getBoundView(Class targetViewClass) {
        Object obj = null;
        if (targetViewClass != null) {
            for (int i = 0; i < this.listeners.size(); i++) {
                if (targetViewClass.isInstance(this.listeners.get(i))) {
                    obj = this.listeners.get(i);
                }
            }
        }
        return obj;
    }

    public String toString() {
        return "FolderInfo(title=" + this.title + " id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " rank=" + this.rank + " cellX=" + this.cellX + " cellY=" + this.cellY + " dropPos=" + Arrays.toString(this.dropPos) + ")";
    }

    public boolean hasOption(int optionFlag) {
        return (this.options & optionFlag) != 0;
    }

    public void setOption(int option, boolean isEnabled, Context context) {
        int oldOptions = this.options;
        if (isEnabled) {
            this.options |= option;
        } else {
            this.options &= option ^ -1;
        }
        if (context != null && oldOptions != this.options) {
            ((Launcher) context).getHomeController().updateItemInDb(this);
        }
    }

    public FolderInfo makeCloneInfo() {
        FolderInfo info = new FolderInfo();
        info.copyFrom(this);
        info.container = -1;
        info.id = -1;
        info.title = this.title;
        info.options = this.options;
        info.color = this.color;
        info.mCurrentComparator = this.mCurrentComparator;
        info.mAlphabeticalOrder = this.mAlphabeticalOrder;
        info.mLocked = this.mLocked;
        info.mLockedFolderOpenedOnce = this.mLockedFolderOpenedOnce;
        if (!this.contents.isEmpty()) {
            Iterator it = this.contents.iterator();
            while (it.hasNext()) {
                IconInfo icon = (IconInfo) it.next();
                if (icon != null) {
                    info.contents.add(icon.makeCloneInfo());
                }
            }
        }
        info.sortContents();
        return info;
    }

    public int getItemCount() {
        return this.contents.size();
    }
}
