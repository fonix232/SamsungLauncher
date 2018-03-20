package com.android.launcher3.allapps.controller;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class CustomNormalizer extends Normalizer<Object> {
    public final int compare(Object obj1, Object obj2) {
        ItemInfo a;
        ItemInfo b;
        int ret;
        if (obj1 instanceof ItemInfo) {
            a = (ItemInfo) obj1;
            b = (ItemInfo) obj2;
        } else {
            a = (ItemInfo) ((View) obj1).getTag();
            b = (ItemInfo) ((View) obj2).getTag();
        }
        if (a.screenId != -1 && b.screenId != -1) {
            ret = Normalizer.longCompare(a.screenId, b.screenId);
            if (ret == 0) {
                ret = Normalizer.integerCompare(a.rank, b.rank);
            }
        } else if (a.screenId == b.screenId) {
            ret = 0;
        } else {
            ret = a.screenId == -1 ? 1 : -1;
        }
        if (ret != 0) {
            return ret;
        }
        if (a.title != null && b.title != null) {
            ret = this.mCollator.compare(a.title.toString(), b.title.toString());
        } else if (a.title != b.title) {
            ret = a.title == null ? -1 : 1;
        }
        if (ret != 0) {
            return ret;
        }
        if (a.componentName != null && b.componentName != null) {
            ret = Normalizer.longCompare(a.id, b.id);
            if (ret == 0) {
                ret = a.componentName.compareTo(b.componentName);
            }
        } else if (a.componentName != b.componentName) {
            ret = a.componentName == null ? -1 : 1;
        }
        if (ret == 0) {
            return Normalizer.longCompare(a.id, b.id);
        }
        return ret;
    }

    protected int normalize(ArrayList<?> list, int maxItemsPerScreen, int cellCountX, ArrayList<View> viewsOfScreen, int targetPage) {
        Collections.sort(list, this);
        int targetScreen = 0;
        int targetCell = 0;
        long priorScreen = 0;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ItemInfo item;
            Object o = it.next();
            if (o instanceof ItemInfo) {
                item = (ItemInfo) o;
            } else {
                item = (ItemInfo) ((View) o).getTag();
            }
            long screen = item.screenId;
            if (targetCell == maxItemsPerScreen) {
                targetScreen++;
                targetCell = 0;
            }
            if (!(screen == priorScreen || screen == -1)) {
                if (screen > ((long) targetScreen) && targetCell != 0) {
                    targetScreen++;
                    targetCell = 0;
                }
                priorScreen = screen;
            }
            if (!(screen == ((long) targetScreen) && item.rank == targetCell)) {
                if (viewsOfScreen == null || !(o instanceof View)) {
                    item.mDirty = true;
                    item.screenId = (long) targetScreen;
                    item.rank = targetCell;
                } else if (targetPage == targetScreen) {
                    viewsOfScreen.add((View) o);
                }
            }
            if (viewsOfScreen == null || !(o instanceof View)) {
                item.cellX = targetCell % cellCountX;
                item.cellY = targetCell / cellCountX;
                targetCell++;
            } else {
                targetCell++;
            }
        }
        return targetScreen;
    }

    public String toString() {
        return "CustomNormalizer";
    }
}
