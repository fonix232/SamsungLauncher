package com.android.launcher3.folder;

import com.android.launcher3.common.base.item.IconInfo;
import java.util.ArrayList;

public interface FolderEventListener {
    void onItemAdded(IconInfo iconInfo);

    void onItemRemoved(IconInfo iconInfo);

    void onItemsAdded(ArrayList<IconInfo> arrayList);

    void onItemsRemoved(ArrayList<IconInfo> arrayList);

    void onLockedFolderOpenStateUpdated(Boolean bool);

    void onOrderingChanged(boolean z);

    void onTitleChanged(CharSequence charSequence);
}
