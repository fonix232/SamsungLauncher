package com.android.launcher3.allapps;

public class BackupAppsPosition {
    public static int INVALID_DATA = -1;
    private int rank = INVALID_DATA;
    private long screenId = ((long) INVALID_DATA);

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setScreenId(long screenId) {
        this.screenId = screenId;
    }

    public int getRank() {
        return this.rank;
    }

    public long getScreenId() {
        return this.screenId;
    }
}
