package com.android.launcher3.util;

import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.model.DataLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemListHelper {

    interface ItemInfoFilter {
        boolean filter(ItemInfo itemInfo);
    }

    private ItemListHelper() {
    }

    public static Map<Long, ItemInfo> getAllItemMap() {
        List<ItemInfo> allItems = DataLoader.getItemList();
        Map<Long, ItemInfo> itemMap = new HashMap();
        for (ItemInfo item : getUnhiddenItemList(allItems)) {
            itemMap.put(Long.valueOf(item.id), item);
        }
        return itemMap;
    }

    public static List<ItemInfo> getTitleMatchedItemList(List<ItemInfo> allItems, final String itemTitle) {
        return filterItems(allItems, new ItemInfoFilter() {
            public boolean filter(ItemInfo itemInfo) {
                return itemInfo == null || itemInfo.title == null || itemTitle.replaceAll("\\s", "").compareToIgnoreCase(itemInfo.title.toString().replaceAll("\\s", "")) != 0;
            }
        });
    }

    public static List<ItemInfo> getFolderList(List<ItemInfo> allList) {
        return filterItems(allList, new ItemInfoFilter() {
            public boolean filter(ItemInfo itemInfo) {
                if (itemInfo.itemType == 2) {
                    return true;
                }
                return false;
            }
        });
    }

    public static List<ItemInfo> getFolderItemList(List<ItemInfo> allList) {
        return filterItems(allList, new ItemInfoFilter() {
            public boolean filter(ItemInfo itemInfo) {
                if (itemInfo.container > 0) {
                    return false;
                }
                return true;
            }
        });
    }

    public static List<ItemInfo> getContainerIdMatchedItemList(List<ItemInfo> allItems, final int... containers) {
        return filterItems(allItems, new ItemInfoFilter() {
            public boolean filter(ItemInfo itemInfo) {
                for (int container : containers) {
                    if (itemInfo.container == ((long) container)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    public static List<ItemInfo> getUnhiddenItemList(List<ItemInfo> allItems) {
        return filterItems(allItems, new ItemInfoFilter() {
            public boolean filter(ItemInfo itemInfo) {
                if (itemInfo.hidden == 0) {
                    return false;
                }
                return true;
            }
        });
    }

    public static List<ItemInfo> filterItems(List<ItemInfo> list, ItemInfoFilter filter) {
        List<ItemInfo> filteredList = new ArrayList();
        for (ItemInfo itemInfo : list) {
            if (!filter.filter(itemInfo)) {
                filteredList.add(itemInfo);
            }
        }
        return filteredList;
    }
}
