package com.android.launcher3.allapps;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import java.util.ArrayList;

public interface AppsReorderListener {
    boolean getExistOverLastItemMoved();

    void makeEmptyCellAndReorder(int i, int i2);

    void realTimeReorder(int i, float f, int i2, int i3, int i4, int i5);

    void realTimeReorder(DragAppIcon dragAppIcon, DragAppIcon dragAppIcon2);

    void removeEmptyCell(DragAppIcon dragAppIcon);

    void removeEmptyCellsAndViews(ArrayList<DragAppIcon> arrayList, DragAppIcon dragAppIcon, boolean z);

    void removeEmptyCellsAndViews(ArrayList<ItemInfo> arrayList, boolean z);

    void setExistOverLastItemMoved(boolean z);

    void setReorderTarget(CellLayout cellLayout);

    void undoOverLastItems();
}
