package com.android.launcher3.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

public class MultiHashMap<K, V> extends HashMap<K, ArrayList<V>> {
    public MultiHashMap(int size) {
        super(size);
    }

    public void addToList(K key, V value) {
        ArrayList<V> list = (ArrayList) get(key);
        if (list == null) {
            list = new ArrayList();
            list.add(value);
            put(key, list);
            return;
        }
        list.add(value);
    }

    public MultiHashMap<K, V> clone() {
        MultiHashMap<K, V> map = new MultiHashMap(size());
        for (Entry<K, ArrayList<V>> entry : entrySet()) {
            map.put(entry.getKey(), new ArrayList((Collection) entry.getValue()));
        }
        return map;
    }
}
