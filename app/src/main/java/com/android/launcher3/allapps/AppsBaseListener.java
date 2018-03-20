package com.android.launcher3.allapps;

import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.folder.view.FolderIconView;
import java.util.ArrayList;

public interface AppsBaseListener extends ControllerBase {
    FolderIconView addFolder(CellLayout cellLayout, long j, int i, int i2, int i3);

    boolean changeState(int i, boolean z);

    View createItemView(ItemInfo itemInfo, ViewGroup viewGroup, View view);

    View getAppsIconByItemId(long j);

    boolean isItemInFolder(ItemInfo itemInfo);

    void normalizeWithExtraItems(ArrayList<ItemInfo> arrayList, ArrayList<ItemInfo> arrayList2);

    boolean removeEmptyPagesAndUpdateAllItemsInfo();

    void repositionByNormalizer(boolean z);

    void updateBadgeItems(ArrayList<ItemInfo> arrayList);

    void updateCountBadge(View view, boolean z);

    void updateDirtyItems();
}
