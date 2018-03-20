package com.samsung.android.scloud.oem.lib.bnr;

public class BNRItem {
    private String data;
    private String localId;
    private long size;
    private long timestamp;

    public BNRItem(String localId, long timestamp) {
        this.localId = localId;
        setTimestamp(timestamp);
    }

    public BNRItem(String localId, String data, long timestamp) {
        this.localId = localId;
        this.data = data;
        setTimestamp(timestamp);
    }

    public long getTimeStamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timeStamp) {
        int len = 13 - (timeStamp + "").length();
        if (len > 0) {
            timeStamp = (long) (((double) timeStamp) * Math.pow(10.0d, (double) len));
        }
        this.timestamp = timeStamp;
    }

    public String getLocalId() {
        return this.localId;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return this.data;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
