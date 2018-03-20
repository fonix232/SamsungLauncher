package com.android.launcher3.allapps.controller;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.locale.LocaleUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class AlphabetNormalizer extends Normalizer<Object> {
    public int compare(Object obj1, Object obj2) {
        ItemInfo a;
        ItemInfo b;
        if (obj1 instanceof ItemInfo) {
            a = (ItemInfo) obj1;
            b = (ItemInfo) obj2;
        } else {
            a = (ItemInfo) ((View) obj1).getTag();
            b = (ItemInfo) ((View) obj2).getTag();
        }
        if (a.itemType != b.itemType) {
            if (a.itemType == 2) {
                return -1;
            }
            return 1;
        } else if (LocaleUtils.isChinesePinyinSortingOnApps()) {
            return compareChineseTitle(a, b);
        } else {
            return this.mCollator.compare(a.title.toString(), b.title.toString());
        }
    }

    private int compareChineseTitle(ItemInfo a, ItemInfo b) {
        String info1Title = a.title.toString();
        String info2Title = b.title.toString();
        int result = this.mCollator.compare(info1Title.isEmpty() ? info1Title : LocaleUtils.getInstance().makeSectionString(info1Title, true), info2Title.isEmpty() ? info2Title : LocaleUtils.getInstance().makeSectionString(info2Title, true));
        if (result == 0) {
            return this.mCollator.compare(info1Title, info2Title);
        }
        return result;
    }

    protected int normalize(ArrayList<?> list, int maxItemsPerScreen, int cellCountX, ArrayList<View> viewsOfScreen, int targetPage) {
        Collections.sort(list, this);
        int targetScreen = 0;
        int targetCell = 0;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ItemInfo item;
            ItemInfo o = it.next();
            if (o instanceof ItemInfo) {
                item = o;
            } else {
                item = (ItemInfo) ((View) o).getTag();
            }
            if (targetCell == maxItemsPerScreen) {
                targetScreen++;
                targetCell = 0;
            }
            if (viewsOfScreen == null || !(o instanceof View)) {
                item.screenId = (long) targetScreen;
                item.rank = targetCell;
                item.cellX = targetCell % cellCountX;
                item.cellY = targetCell / cellCountX;
                targetCell++;
            } else {
                if (targetPage == targetScreen) {
                    viewsOfScreen.add((View) o);
                }
                targetCell++;
            }
        }
        return targetScreen;
    }

    public String toString() {
        return "MENU_ALPHA_NORMALIZER";
    }
}
