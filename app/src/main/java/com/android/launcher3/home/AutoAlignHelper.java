package com.android.launcher3.home;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AutoAlignHelper {
    private static final int ALIGN_ANIMATION_DURATION = 250;
    private static Comparator<ItemInfo> ITEM_ALIGN_DOWNWARD = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.cellY == rhs.cellY) {
                return rhs.cellX - lhs.cellX;
            }
            return rhs.cellY - lhs.cellY;
        }
    };
    static Comparator<ItemInfo> ITEM_ALIGN_UPWARD = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.cellY == rhs.cellY) {
                return lhs.cellX - rhs.cellX;
            }
            return lhs.cellY - rhs.cellY;
        }
    };

    static boolean autoAlignItems(CellLayout cellLayout, boolean isUpward, boolean checkToAlign) {
        if (cellLayout.getCellLayoutChildren() == null) {
            return false;
        }
        ArrayList<ItemInfo> oneByOneItems = new ArrayList();
        int countX = cellLayout.getCountX();
        int countY = cellLayout.getCountY();
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{countX, countY});
        findOneByOneItemsAndOccupiedPosition(cellLayout, occupied, oneByOneItems);
        if (oneByOneItems.size() == 0) {
            return false;
        }
        sortItemsByCellPosition(oneByOneItems, isUpward);
        return findEmptyCellAndAnimateToPosition(cellLayout, occupied, countX, countY, oneByOneItems, isUpward, checkToAlign);
    }

    private static void findOneByOneItemsAndOccupiedPosition(CellLayout cellLayout, boolean[][] occupied, ArrayList<ItemInfo> oneByOneItems) {
        int childCount = cellLayout.getCellLayoutChildren().getChildCount();
        int countX = cellLayout.getCountX();
        int countY = cellLayout.getCountY();
        for (int i = 0; i < childCount; i++) {
            ItemInfo tag = cellLayout.getCellLayoutChildren().getChildAt(i).getTag();
            if (tag instanceof ItemInfo) {
                ItemInfo item = tag;
                if (item.spanX == 1 && item.spanY == 1) {
                    oneByOneItems.add(item);
                } else {
                    int x = item.cellX;
                    while (x < item.cellX + item.spanX && x < countX) {
                        int y = item.cellY;
                        while (y < item.cellY + item.spanY && y < countY) {
                            occupied[x][y] = true;
                            y++;
                        }
                        x++;
                    }
                }
            }
        }
    }

    private static boolean findEmptyCellAndAnimateToPosition(CellLayout cellLayout, boolean[][] occupied, int countX, int countY, ArrayList<ItemInfo> oneByOneItems, boolean isUpward, boolean checkToAlign) {
        boolean isAligned = false;
        int startX = isUpward ? 0 : countX - 1;
        int startY = isUpward ? 0 : countY - 1;
        int increment = isUpward ? 1 : -1;
        int y = startY;
        while (true) {
            if (!isUpward) {
                if (y < 0) {
                    break;
                }
            } else if (y >= countY) {
                break;
            }
            int x = startX;
            while (true) {
                if (!isUpward) {
                    if (x < 0) {
                        continue;
                        break;
                    }
                } else if (x >= countX) {
                    continue;
                    break;
                }
                if (!occupied[x][y]) {
                    occupied[x][y] = true;
                    if (oneByOneItems.isEmpty()) {
                        return isAligned;
                    }
                    ItemInfo oneByOneItem = (ItemInfo) oneByOneItems.get(0);
                    oneByOneItems.remove(oneByOneItem);
                    if (x != oneByOneItem.cellX || y != oneByOneItem.cellY) {
                        if (checkToAlign) {
                            return true;
                        }
                        oneByOneItem.cellX = x;
                        oneByOneItem.cellY = y;
                        View childView = cellLayout.getCellLayoutChildren().getChildAt(oneByOneItem);
                        if (childView != null) {
                            LayoutParams lp = (LayoutParams) childView.getLayoutParams();
                            if (lp.useTmpCoords) {
                                lp.useTmpCoords = false;
                            }
                            cellLayout.animateChildToPosition(childView, oneByOneItem.cellX, oneByOneItem.cellY, 250, 0, true, true, (boolean[][]) null);
                        }
                        oneByOneItem.requiresDbUpdate = true;
                        isAligned = true;
                    }
                }
                x += increment;
            }
            y += increment;
        }
        return isAligned;
    }

    private static void sortItemsByCellPosition(List<ItemInfo> items, boolean isUpward) {
        if (items != null && items.size() > 0) {
            if (isUpward) {
                Collections.sort(items, ITEM_ALIGN_UPWARD);
            } else {
                Collections.sort(items, ITEM_ALIGN_DOWNWARD);
            }
        }
    }
}
