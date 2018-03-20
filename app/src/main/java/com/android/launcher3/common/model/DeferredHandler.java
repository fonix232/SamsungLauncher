package com.android.launcher3.common.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.MessageQueue.IdleHandler;
import android.util.Log;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.Callbacks;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

public class DeferredHandler {
    private static final String TAG = "DeferredHandler";
    private static Impl sHandler = new Impl();
    private static MessageQueue sMessageQueue = Looper.myQueue();
    private static LinkedList<Runnable> sPendingQueue = new LinkedList();
    private static LinkedList<Runnable> sQueue = new LinkedList();
    private WeakReference<Callbacks> mCallbacks;
    private LauncherModel mModel;
    private boolean mStartFlushPendingQueue = false;

    private static class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            this.mRunnable = r;
        }

        public void run() {
            this.mRunnable.run();
        }
    }

    public static class Impl extends Handler implements IdleHandler {
        public void handleMessage(Message msg) {
            synchronized (DeferredHandler.sQueue) {
                if (DeferredHandler.sQueue.size() == 0) {
                    return;
                }
                Runnable r = (Runnable) DeferredHandler.sQueue.removeFirst();
                r.run();
                synchronized (DeferredHandler.sQueue) {
                    DeferredHandler.scheduleNextLocked();
                }
            }
        }

        public boolean queueIdle() {
            handleMessage(null);
            return false;
        }
    }

    public void setCallbacks(LauncherModel model, WeakReference<Callbacks> callbacks) {
        this.mModel = model;
        this.mCallbacks = callbacks;
    }

    public void post(Runnable runnable) {
        if (this.mModel != null && this.mModel.isModelIdle()) {
            synchronized (sPendingQueue) {
                if (this.mCallbacks != null) {
                    Callbacks callbacks = (Callbacks) this.mCallbacks.get();
                    if (callbacks != null && callbacks.isTrayAnimating()) {
                        sPendingQueue.add(runnable);
                        Log.d(TAG, "tray animating. add pendingQueue");
                        return;
                    }
                }
                if (this.mStartFlushPendingQueue && !sPendingQueue.isEmpty()) {
                    sPendingQueue.add(runnable);
                    Log.d(TAG, "tray animating end. but pendingQueue is not flush yet.");
                    return;
                }
            }
        }
        synchronized (sQueue) {
            sQueue.add(runnable);
            if (sQueue.size() == 1) {
                scheduleNextLocked();
            }
        }
    }

    public void flushPendingQueue() {
        LinkedList<Runnable> queue = new LinkedList();
        synchronized (sPendingQueue) {
            queue.addAll(sPendingQueue);
            sPendingQueue.clear();
        }
        Iterator it = queue.iterator();
        while (it.hasNext()) {
            Runnable r = (Runnable) it.next();
            synchronized (sQueue) {
                sQueue.add(r);
                if (sQueue.size() == 1) {
                    scheduleNextLocked();
                }
            }
        }
    }

    public void startPendingQueueFlush(boolean flushStart) {
        this.mStartFlushPendingQueue = flushStart;
    }

    public void postIdle(Runnable runnable) {
        post(new IdleRunnable(runnable));
    }

    public void cancelAll() {
        synchronized (sQueue) {
            sQueue.clear();
        }
        synchronized (sPendingQueue) {
            sPendingQueue.clear();
        }
    }

    public void flush() {
        LinkedList<Runnable> queue = new LinkedList();
        synchronized (sQueue) {
            queue.addAll(sPendingQueue);
            sPendingQueue.clear();
            queue.addAll(sQueue);
            sQueue.clear();
        }
        Iterator it = queue.iterator();
        while (it.hasNext()) {
            ((Runnable) it.next()).run();
        }
    }

    static void scheduleNextLocked() {
        if (sQueue.size() <= 0) {
            return;
        }
        if (((Runnable) sQueue.getFirst()) instanceof IdleRunnable) {
            sMessageQueue.addIdleHandler(sHandler);
        } else {
            sHandler.sendEmptyMessage(1);
        }
    }
}
