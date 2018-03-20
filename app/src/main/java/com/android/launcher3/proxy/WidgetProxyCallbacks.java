package com.android.launcher3.proxy;

import android.content.ComponentName;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import java.util.ArrayList;

public interface WidgetProxyCallbacks extends CommonProxyCallbacks {
    void enterSearchState();

    void enterUninstallState();

    ArrayList<ItemInfo> getWidgetItemsInfoByComponentName(ComponentName componentName);

    ArrayList<ItemInfo> getWidgetItemsInfoByTitle(String str);

    PendingAddItemInfo getWidgetResultItem();

    int search(String str);

    void uninstallWidget(PendingAddItemInfo pendingAddItemInfo);
}
