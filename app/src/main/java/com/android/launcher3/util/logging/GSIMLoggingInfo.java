package com.android.launcher3.util.logging;

/* compiled from: GSIMLogging */
class GSIMLoggingInfo {
    private String mExtras;
    private String mFeatures;
    private long mValues;

    public String getFeatures() {
        return this.mFeatures;
    }

    public String getExtras() {
        return this.mExtras;
    }

    public long getValues() {
        return this.mValues;
    }

    public GSIMLoggingInfo(String features, String extras, long values) {
        this.mFeatures = features;
        this.mExtras = extras;
        this.mValues = values;
    }
}
