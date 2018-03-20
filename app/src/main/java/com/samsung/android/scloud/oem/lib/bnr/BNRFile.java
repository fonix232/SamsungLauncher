package com.samsung.android.scloud.oem.lib.bnr;

import java.io.FileDescriptor;

public class BNRFile {
    private FileDescriptor fd;
    private boolean isExternal;
    private String path;
    private long size;
    private long timestamp;

    public BNRFile(String path, boolean isExternal) {
        this.path = path;
        this.isExternal = isExternal;
    }

    public BNRFile(String path, boolean isExternal, long timeStamp, long size) {
        this.path = path;
        this.size = size;
        this.isExternal = isExternal;
        int len = 13 - (timeStamp + "").length();
        if (len > 0) {
            timeStamp = (long) (((double) timeStamp) * Math.pow(10.0d, (double) len));
        }
        this.timestamp = timeStamp;
    }

    public BNRFile(String path, boolean isExternal, long timeStamp, long size, FileDescriptor fd) {
        this.path = path;
        this.size = size;
        this.isExternal = isExternal;
        int len = 13 - (timeStamp + "").length();
        if (len > 0) {
            timeStamp = (long) (((double) timeStamp) * Math.pow(10.0d, (double) len));
        }
        this.timestamp = timeStamp;
        this.fd = fd;
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

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean getisExternal() {
        return this.isExternal;
    }

    public void setisExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    public void setFileUri(FileDescriptor fd) {
        this.fd = fd;
    }
}
