package com.android.launcher3.util.threadpool;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private static final int CORE_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 10;
    private static final int MAX_POOL_SIZE = 4;
    public static final int MODE_CPU = 1;
    public static final int MODE_NONE = 0;
    private static final String TAG = "ThreadPool";
    ResourceCounter mCpuCounter;
    private final Executor mExecutor;

    public interface CancelListener {
        void onCancel();
    }

    public interface Job<T> {
        T run(JobContext jobContext);
    }

    public interface JobContext {
        int getMode();

        boolean isCancelled();

        void setCancelListener(CancelListener cancelListener);

        boolean setMode(int i);
    }

    private static class ResourceCounter {
        public int value;

        public ResourceCounter(int v) {
            this.value = v;
        }
    }

    private static class JobContextStub implements JobContext {
        private JobContextStub() {
        }

        public boolean isCancelled() {
            return false;
        }

        public void setCancelListener(CancelListener listener) {
        }

        public boolean setMode(int mode) {
            return true;
        }

        public int getMode() {
            return 1;
        }
    }

    private class Worker<T> implements Runnable, Future<T>, JobContext {
        private static final String TAG = "Worker";
        private CancelListener mCancelListener;
        private volatile boolean mIsCancelled;
        private boolean mIsDone;
        private Job<T> mJob;
        private FutureListener<T> mListener;
        private int mMode;
        private T mResult;
        private ResourceCounter mWaitOnResource;

        public Worker(ThreadPool threadPool, Job<T> job, FutureListener<T> listener) {
            this(job, listener, false);
        }

        public Worker(Job<T> job, FutureListener<T> listener, boolean networkPool) {
            this.mJob = job;
            this.mListener = listener;
        }

        public void run() {
            Object result = null;
            if (setMode(1)) {
                try {
                    result = this.mJob.run(this);
                } catch (OutOfMemoryError ex) {
                    ex.printStackTrace();
                    Log.w(TAG, "Exception in running a job", ex);
                } catch (Exception ex2) {
                    Log.w(TAG, "Exception in running a job", ex2);
                }
            }
            synchronized (this) {
                setMode(0);
                this.mResult = result;
                this.mIsDone = true;
                notifyAll();
            }
            if (this.mListener != null) {
                this.mListener.onFutureDone(this);
            }
        }

        public synchronized void cancel() {
            if (!this.mIsCancelled) {
                this.mIsCancelled = true;
                if (this.mWaitOnResource != null) {
                    synchronized (this.mWaitOnResource) {
                        this.mWaitOnResource.notifyAll();
                    }
                }
                if (this.mCancelListener != null) {
                    this.mCancelListener.onCancel();
                }
            }
        }

        public synchronized boolean isCancelled() {
            return this.mIsCancelled;
        }

        public synchronized boolean isDone() {
            return this.mIsDone;
        }

        public synchronized T get() {
            while (!this.mIsDone) {
                try {
                    wait();
                } catch (Exception ex) {
                    Log.w(TAG, "ingore exception", ex);
                }
            }
            return this.mResult;
        }

        public void waitDone() {
            get();
        }

        public synchronized void setCancelListener(CancelListener listener) {
            this.mCancelListener = listener;
            if (this.mIsCancelled && this.mCancelListener != null) {
                this.mCancelListener.onCancel();
            }
        }

        public boolean setMode(int mode) {
            ResourceCounter rc = modeToCounter(this.mMode);
            if (rc != null) {
                releaseResource(rc);
            }
            this.mMode = 0;
            rc = modeToCounter(mode);
            if (rc != null) {
                if (!acquireResource(rc)) {
                    return false;
                }
                this.mMode = mode;
            }
            return true;
        }

        public int getMode() {
            return this.mMode;
        }

        private ResourceCounter modeToCounter(int mode) {
            if (mode == 1) {
                return ThreadPool.this.mCpuCounter;
            }
            return null;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private boolean acquireResource(com.android.launcher3.util.threadpool.ThreadPool.ResourceCounter r2) {
            /*
            r1 = this;
        L_0x0000:
            monitor-enter(r1);
            r0 = r1.mIsCancelled;	 Catch:{ all -> 0x0021 }
            if (r0 == 0) goto L_0x000b;
        L_0x0005:
            r0 = 0;
            r1.mWaitOnResource = r0;	 Catch:{ all -> 0x0021 }
            r0 = 0;
            monitor-exit(r1);	 Catch:{ all -> 0x0021 }
        L_0x000a:
            return r0;
        L_0x000b:
            r1.mWaitOnResource = r2;	 Catch:{ all -> 0x0021 }
            monitor-exit(r1);	 Catch:{ all -> 0x0021 }
            monitor-enter(r2);
            r0 = r2.value;	 Catch:{ all -> 0x0029 }
            if (r0 <= 0) goto L_0x0024;
        L_0x0013:
            r0 = r2.value;	 Catch:{ all -> 0x0029 }
            r0 = r0 + -1;
            r2.value = r0;	 Catch:{ all -> 0x0029 }
            monitor-exit(r2);	 Catch:{ all -> 0x0029 }
            monitor-enter(r1);
            r0 = 0;
            r1.mWaitOnResource = r0;	 Catch:{ all -> 0x002c }
            monitor-exit(r1);	 Catch:{ all -> 0x002c }
            r0 = 1;
            goto L_0x000a;
        L_0x0021:
            r0 = move-exception;
            monitor-exit(r1);	 Catch:{ all -> 0x0021 }
            throw r0;
        L_0x0024:
            r2.wait();	 Catch:{ InterruptedException -> 0x002f }
        L_0x0027:
            monitor-exit(r2);	 Catch:{ all -> 0x0029 }
            goto L_0x0000;
        L_0x0029:
            r0 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x0029 }
            throw r0;
        L_0x002c:
            r0 = move-exception;
            monitor-exit(r1);	 Catch:{ all -> 0x002c }
            throw r0;
        L_0x002f:
            r0 = move-exception;
            goto L_0x0027;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.threadpool.ThreadPool.Worker.acquireResource(com.android.launcher3.util.threadpool.ThreadPool$ResourceCounter):boolean");
        }

        private void releaseResource(ResourceCounter counter) {
            synchronized (counter) {
                counter.value++;
                counter.notifyAll();
            }
        }
    }

    public ThreadPool() {
        this(4, 4);
    }

    public ThreadPool(int initPoolSize, int maxPoolSize) {
        this.mCpuCounter = new ResourceCounter(6);
        this.mExecutor = new ThreadPoolExecutor(initPoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue(), new PriorityThreadFactory("thread-pool", 10));
    }

    public <T> Future<T> submit(Job<T> job, FutureListener<T> listener) {
        Worker<T> w = new Worker(this, job, listener);
        this.mExecutor.execute(w);
        return w;
    }

    public <T> Future<T> submitUrgent(Job<T> job, FutureListener<T> listener) {
        ThreadPoolExecutor excutor = this.mExecutor;
        BlockingQueue<Runnable> queue = excutor.getQueue();
        int size = queue.size();
        Future<T> res = submit(job, listener);
        for (int i = 0; i < size; i++) {
            Worker<T> w = (Worker) queue.poll();
            if (w != null) {
                excutor.remove(w);
                excutor.execute(w);
            }
        }
        return res;
    }
}
