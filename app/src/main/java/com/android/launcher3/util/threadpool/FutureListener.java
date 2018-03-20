package com.android.launcher3.util.threadpool;

public interface FutureListener<T> {
    void onFutureDone(Future<T> future);
}
