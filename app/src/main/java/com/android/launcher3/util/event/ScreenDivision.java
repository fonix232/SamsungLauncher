package com.android.launcher3.util.event;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Iterator;

public class ScreenDivision {
    private static final int DIRECTION_X = 0;
    public static final int DIRECTION_Y = 1;
    public static final int NON_INCLUDED_POS = -1;
    public static final int TYPE_EQUAL_CLEAVAGE = 0;
    private final boolean mCountReverse;
    @SuppressLint({"UseSparseArrays"})
    private SparseArray<Boolean> mCustomized = new SparseArray();
    private final int mDirection;
    private final Rect mParentRect;
    private final Rect[] mSections;
    private final int mTypeOfDivision;

    public ScreenDivision(int numOfSections, int typeOfDivision, int direction, Rect parentRect, boolean countReverse) {
        this.mSections = new Rect[numOfSections];
        this.mTypeOfDivision = typeOfDivision;
        this.mDirection = direction;
        this.mParentRect = new Rect(parentRect);
        this.mCountReverse = countReverse;
        for (int i = 0; i < this.mSections.length; i++) {
            this.mSections[i] = new Rect();
        }
    }

    public ScreenDivision customPatition(int index, Rect rect) {
        if (this.mCountReverse) {
            index = (this.mSections.length - 1) - index;
        }
        if (index < this.mSections.length && index >= 0) {
            this.mSections[index] = rect;
            this.mCustomized.put(index, Boolean.valueOf(true));
        }
        return this;
    }

    public ScreenDivision builder() {
        if (this.mDirection == 0) {
            builderX();
        } else {
            builderY();
        }
        if (this.mCountReverse) {
            int fixedValue = this.mSections.length - 1;
            for (int i = 0; i < this.mSections.length / 2; i++) {
                Rect temp = new Rect(this.mSections[i]);
                this.mSections[i] = this.mSections[fixedValue - i];
                this.mSections[fixedValue - i] = temp;
            }
        }
        return this;
    }

    private void makrPartition(Partition partition) {
        makePartition(partition.mStartIndex, partition.mEndIndex, partition.mRect);
    }

    private void makePartition(int startIndex, int endIndex, Rect parentRect) {
        int divistion = Math.abs(endIndex - startIndex) + 1;
        if (this.mTypeOfDivision == 0 && this.mSections.length > 0) {
            if (this.mDirection == 0) {
                createSectionsX(parentRect, (parentRect.width() - (divistion - 1)) / divistion, (parentRect.width() - (divistion - 1)) % divistion, startIndex, endIndex);
                return;
            }
            createSectionsY(parentRect, (parentRect.height() - (divistion - 1)) / divistion, (parentRect.height() - (divistion - 1)) % divistion, startIndex, endIndex);
        }
    }

    private int binary(int num) {
        return num > 0 ? 1 : 0;
    }

    private int positive(int num) {
        return num > 0 ? num : 0;
    }

    private boolean isCustomized(int index) {
        return this.mCustomized.get(index) != null && ((Boolean) this.mCustomized.get(index)).booleanValue();
    }

    private void builderY() {
        ArrayList<Partition> partitions = new ArrayList();
        int startIndex = 0;
        int top = this.mParentRect.top;
        int curIndex = 0;
        while (curIndex < this.mSections.length) {
            if (isCustomized(curIndex)) {
                partitions.add(new Partition(startIndex, positive(curIndex - 1), new Rect(this.mParentRect.left, top, this.mParentRect.right, positive(this.mSections[curIndex].top - 1))));
                partitions.add(new Partition(curIndex, curIndex, this.mSections[curIndex]));
                top = this.mSections[curIndex].bottom + 1;
                startIndex = curIndex + 1;
            }
            curIndex++;
        }
        if (startIndex < curIndex) {
            partitions.add(new Partition(startIndex, this.mSections.length - 1, new Rect(this.mParentRect.left, top, this.mParentRect.right, this.mParentRect.bottom)));
        }
        Iterator it = partitions.iterator();
        while (it.hasNext()) {
            makrPartition((Partition) it.next());
        }
    }

    private void builderX() {
        ArrayList<Partition> partitions = new ArrayList();
        int startIndex = 0;
        int left = this.mParentRect.left;
        int curIndex = 0;
        while (curIndex < this.mSections.length) {
            if (isCustomized(curIndex)) {
                partitions.add(new Partition(startIndex, positive(curIndex - 1), new Rect(left, this.mParentRect.top, positive(this.mSections[curIndex].left - 1), this.mParentRect.bottom)));
                partitions.add(new Partition(curIndex, curIndex, this.mSections[curIndex]));
                left = this.mSections[curIndex].right + 1;
                startIndex = curIndex + 1;
            }
            curIndex++;
        }
        if (startIndex < curIndex) {
            partitions.add(new Partition(startIndex, this.mSections.length - 1, new Rect(left, this.mParentRect.top, this.mParentRect.right, this.mParentRect.bottom)));
        }
        Iterator it = partitions.iterator();
        while (it.hasNext()) {
            makrPartition((Partition) it.next());
        }
    }

    private void createSectionsY(Rect parentRect, int height, int remainder, int cur, int end) {
        if (cur != this.mSections.length && cur <= end) {
            int top = cur > 0 ? this.mSections[cur - 1].bottom + 1 : parentRect.top;
            this.mSections[cur] = new Rect(parentRect.left, top, parentRect.right, (top + height) + binary(remainder));
            createSectionsY(parentRect, height, remainder - 1, cur + 1, end);
        }
    }

    private void createSectionsX(Rect parentRect, int width, int remainder, int cur, int end) {
        if (cur != this.mSections.length && cur <= end) {
            int left = cur > 0 ? this.mSections[cur - 1].right + 1 : parentRect.left;
            this.mSections[cur] = new Rect(left, parentRect.top, (left + width) + binary(remainder), parentRect.bottom);
            createSectionsX(parentRect, width, remainder - 1, cur + 1, end);
        }
    }

    public int getNumOfSection(float x, float y) {
        for (int i = 0; i < this.mSections.length; i++) {
            if (this.mSections[i].contains((int) x, (int) y)) {
                return i;
            }
        }
        return -1;
    }
}
