package com.android.launcher3.common.drag;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.stage.Stage;
import java.util.ArrayList;

public interface DragSource {
    public static final int DRAG_SOURCE_APPS = 1;
    public static final int DRAG_SOURCE_APPS_FOLDER = 4;
    public static final int DRAG_SOURCE_HOME = 0;
    public static final int DRAG_SOURCE_HOME_FOLDER = 3;
    public static final int DRAG_SOURCE_HOTSEAT = 2;
    public static final int DRAG_SOURCE_PINNING = 7;
    public static final int DRAG_SOURCE_QUICK_OPTION_POPUP = 6;
    public static final int DRAG_SOURCE_WIDGET = 5;

    Stage getController();

    int getDragSourceType();

    int getEmptyCount();

    int getIntrinsicIconSize();

    int getOutlineColor();

    int getPageIndexForDragView(ItemInfo itemInfo);

    int getQuickOptionFlags(DragObject dragObject);

    void onDropCompleted(View view, DragObject dragObject, boolean z);

    void onExtraObjectDragged(ArrayList<DragObject> arrayList);

    void onExtraObjectDropCompleted(View view, ArrayList<DragObject> arrayList, ArrayList<DragObject> arrayList2, int i);
}
