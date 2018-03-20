package com.android.launcher3.allapps.controller;

import android.view.View;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;

public abstract class Normalizer<T> implements Comparator<T> {
    protected Collator mCollator = Collator.getInstance();

    protected abstract int normalize(ArrayList<?> arrayList, int i, int i2, ArrayList<View> arrayList2, int i3);

    public int normalize(ArrayList<?> list, int maxItemsPerScreen, int cellCountX) {
        return normalize(list, maxItemsPerScreen, cellCountX, null, 0);
    }

    public ArrayList<View> getViewsForScreenWithPreNormalize(ArrayList<?> list, int maxItemsPerScreen, int cellCountX, int targetPage) {
        ArrayList<View> views = new ArrayList();
        normalize(list, maxItemsPerScreen, cellCountX, views, targetPage);
        return views;
    }

    protected static int integerCompare(int a, int b) {
        if (a > b) {
            return 1;
        }
        if (a < b) {
            return -1;
        }
        return 0;
    }

    protected static int longCompare(long a, long b) {
        if (a > b) {
            return 1;
        }
        if (a < b) {
            return -1;
        }
        return 0;
    }
}
