package com.android.launcher3.util;

import android.util.LongSparseArray;
import java.util.Iterator;

public class LongArrayMap<E> extends LongSparseArray<E> implements Iterable<E> {

    class ValueIterator implements Iterator<E> {
        private int mNextIndex = 0;

        ValueIterator() {
        }

        public boolean hasNext() {
            return this.mNextIndex < LongArrayMap.this.size();
        }

        public E next() {
            LongArrayMap longArrayMap = LongArrayMap.this;
            int i = this.mNextIndex;
            this.mNextIndex = i + 1;
            return longArrayMap.valueAt(i);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public boolean containsKey(long key) {
        return indexOfKey(key) >= 0;
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    public LongArrayMap<E> clone() {
        return (LongArrayMap) super.clone();
    }

    public Iterator<E> iterator() {
        return new ValueIterator();
    }
}
