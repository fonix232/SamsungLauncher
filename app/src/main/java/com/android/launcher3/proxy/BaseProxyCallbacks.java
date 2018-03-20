package com.android.launcher3.proxy;

import android.content.ComponentName;
import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView;

public interface BaseProxyCallbacks extends CommonProxyCallbacks {
    FolderIconView getFolderItemViewByTitle(String str);

    View getItemViewByComponentName(ComponentName componentName);

    IconView getItemViewByTitle(String str);

    PagedView getPagedView();

    void movePage(int i);

    void movePageToItem(ItemInfo itemInfo);

    void selectItem(IconView iconView);

    void unSelectItem(IconView iconView);
}
