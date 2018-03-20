package com.android.launcher3.proxy;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;

public interface FolderProxyCallbacks extends BaseProxyCallbacks {
    void addFolderItem(ItemInfo itemInfo);

    void changeBackgroundColor(int i);

    void changeTitle(String str);

    FolderInfo getOpenedFolder();

    FolderIconView getOpenedFolderIconView();

    void openBackgroundColorView();

    void removeFolderItem(ItemInfo itemInfo);
}
