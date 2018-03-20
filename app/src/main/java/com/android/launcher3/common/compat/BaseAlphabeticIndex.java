package com.android.launcher3.common.compat;

/* compiled from: AlphabeticIndexCompat */
class BaseAlphabeticIndex {
    private static final String BUCKETS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-";
    private static final int UNKNOWN_BUCKET_INDEX = (BUCKETS.length() - 1);

    public void setMaxLabelCount(int count) {
    }

    protected int getBucketIndex(String s) {
        if (s.isEmpty()) {
            return UNKNOWN_BUCKET_INDEX;
        }
        int index = BUCKETS.indexOf(s.substring(0, 1).toUpperCase());
        return index == -1 ? UNKNOWN_BUCKET_INDEX : index;
    }

    protected String getBucketLabel(int index) {
        return BUCKETS.substring(index, index + 1);
    }
}
