package com.android.launcher3.folder.folderlock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class LockedItemRecords {
    public static final String APPLOCK = "APPLOCK";
    private static final String EMPTY_STRING = "";
    public static final String LOCKED_RECORD_NAME_DEF = "locked_folder_records";
    private static final String SEPARATOR_STRING = ",";
    private int mMode;
    private String mName;

    public LockedItemRecords() {
        this.mName = LOCKED_RECORD_NAME_DEF;
        this.mMode = 0;
        this.mName = LOCKED_RECORD_NAME_DEF;
    }

    public String getName() {
        return this.mName;
    }

    protected int getMode() {
        return this.mMode;
    }

    protected void setMode(int mode) {
        this.mMode = mode;
    }

    public int size(Context context) {
        if (context == null) {
            return 0;
        }
        return context.getSharedPreferences(getName(), getMode()).getAll().size();
    }

    public void add(Context context, String container, long[] itemIds) {
        if (context != null) {
            Editor editor = context.getSharedPreferences(getName(), getMode()).edit();
            StringBuilder builder = new StringBuilder("");
            for (long item : itemIds) {
                builder.append(String.valueOf(item));
                builder.append(SEPARATOR_STRING);
            }
            editor.putString(container, builder.toString());
            editor.apply();
        }
    }

    public void add(Context context, String container, String itemId) {
        if (context != null && !contains(context, container, itemId)) {
            SharedPreferences prefs = context.getSharedPreferences(getName(), getMode());
            Editor editor = prefs.edit();
            String key = SEPARATOR_STRING + itemId;
            String items = prefs.getString(container, "");
            if (items.isEmpty()) {
                key = itemId;
            }
            editor.putString(container, items + key);
            editor.apply();
        }
    }

    public void add(Context context, String container, long itemId) {
        add(context, container, String.valueOf(itemId));
    }

    public void removeAll(Context context, String container) {
        String containerAddition = FolderLock.UNLOCK_CONTAINER_ADDITION;
        Editor editor = context.getSharedPreferences(getName(), getMode()).edit();
        editor.remove(container);
        editor.remove(container + containerAddition);
        editor.apply();
    }

    public void remove(Context context, String container, long itemId) {
        if (context != null) {
            if (container == null) {
                Log.d(FolderLock.TAG, "container is null can not remove the recorder");
                return;
            }
            String key = String.valueOf(itemId);
            SharedPreferences prefs = context.getSharedPreferences(getName(), getMode());
            Editor editor = prefs.edit();
            String[] items = prefs.getString(container, "").split(SEPARATOR_STRING);
            StringBuilder builder = new StringBuilder("");
            for (int i = 0; i < items.length; i++) {
                if (!key.equals(items[i])) {
                    builder.append(items[i]);
                    builder.append(SEPARATOR_STRING);
                }
            }
            editor.putString(container, builder.toString());
            editor.apply();
        }
    }

    public String getString(Context context, String container, String def) {
        if (context == null) {
            return def;
        }
        return context.getSharedPreferences(getName(), getMode()).getString(container, def);
    }

    public boolean contains(Context context, String container, String itemId) {
        if (context == null) {
            return false;
        }
        String key = itemId;
        String[] itemList = context.getSharedPreferences(getName(), getMode()).getString(container, "").split(SEPARATOR_STRING);
        for (Object equals : itemList) {
            if (key.equals(equals)) {
                return true;
            }
        }
        return false;
    }
}
