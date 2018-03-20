package com.android.launcher3.proxy;

import android.content.ComponentName;
import com.android.launcher3.common.base.item.ItemInfo;

public interface AppsPickerProxyCallbacks extends CommonProxyCallbacks {
    void addResultApps();

    void addResultApps(int i);

    void addResultApps(boolean z);

    ItemInfo getItem(int i);

    ItemInfo getItem(ComponentName componentName);

    ItemInfo getItem(String str);

    int getSearchResultListCheckedCount();

    int getSearchResultListCount();

    ItemInfo getSearchResultSingleAppInfo();

    boolean setSearchText(String str);
}
