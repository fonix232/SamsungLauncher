package com.android.launcher3.util;

public class StringJoiner {
    private final StringBuilder mBuilder = new StringBuilder();
    private final String mDelimiter;

    public StringJoiner(String delimiter) {
        this.mDelimiter = delimiter;
    }

    public void append(String string) {
        if (this.mBuilder.length() == 0) {
            this.mBuilder.append(string);
            return;
        }
        this.mBuilder.append(this.mDelimiter);
        this.mBuilder.append(string);
    }

    public void append(long value) {
        append(String.valueOf(value));
    }

    public String toString() {
        return this.mBuilder.toString();
    }
}
