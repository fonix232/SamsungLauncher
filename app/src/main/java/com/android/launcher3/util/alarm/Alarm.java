package com.android.launcher3.util.alarm;

import android.os.Handler;

public class Alarm implements Runnable {
    private OnAlarmListener mAlarmListener;
    private boolean mAlarmPending = false;
    private long mAlarmTriggerTime;
    private final Handler mHandler = new Handler();
    private boolean mWaitingForCallback;

    public void setOnAlarmListener(OnAlarmListener alarmListener) {
        this.mAlarmListener = alarmListener;
    }

    public void setAlarm(long millisecondsInFuture) {
        long currentTime = System.currentTimeMillis();
        this.mAlarmPending = true;
        this.mAlarmTriggerTime = currentTime + millisecondsInFuture;
        if (!this.mWaitingForCallback) {
            this.mHandler.postDelayed(this, this.mAlarmTriggerTime - currentTime);
            this.mWaitingForCallback = true;
        }
    }

    public void cancelAlarm() {
        this.mAlarmTriggerTime = 0;
        this.mAlarmPending = false;
    }

    public void run() {
        this.mWaitingForCallback = false;
        if (this.mAlarmTriggerTime != 0) {
            long currentTime = System.currentTimeMillis();
            if (this.mAlarmTriggerTime > currentTime) {
                this.mHandler.postDelayed(this, Math.max(0, this.mAlarmTriggerTime - currentTime));
                this.mWaitingForCallback = true;
                return;
            }
            this.mAlarmPending = false;
            if (this.mAlarmListener != null) {
                this.mAlarmListener.onAlarm(this);
            }
        }
    }

    public boolean alarmPending() {
        return this.mAlarmPending;
    }
}
