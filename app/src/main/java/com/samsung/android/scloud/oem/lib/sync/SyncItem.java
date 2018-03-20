package com.samsung.android.scloud.oem.lib.sync;

public class SyncItem {
    private boolean deleted;
    private String localId;
    private String syncKey;
    private String tag;
    private long timestamp;

    public SyncItem(String localId, String syncKey) {
        this.localId = localId;
        this.syncKey = syncKey;
    }

    public SyncItem(String localId, String syncKey, long timestamp, boolean deleted, String tag) {
        this.localId = localId;
        this.syncKey = syncKey;
        this.timestamp = timestamp;
        this.deleted = deleted;
        this.tag = tag;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getLocalId() {
        return this.localId;
    }

    public String getSyncKey() {
        return this.syncKey;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public String getTag() {
        return this.tag;
    }
}
