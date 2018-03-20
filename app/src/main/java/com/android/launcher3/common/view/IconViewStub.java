package com.android.launcher3.common.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.util.threadpool.Future;
import com.android.launcher3.util.threadpool.FutureListener;
import com.android.launcher3.util.threadpool.ThreadPool.Job;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public final class IconViewStub extends View {
    private static final String TAG = "IconViewStub";
    private Future<View> mInflateBackgroundWorker;
    private final ArrayList<OnInflateListener> mInflateListeners;
    private InflateUrgentTask mInflateUrgentTask;
    private int mInflatedId;
    private WeakReference<View> mInflatedViewRef;
    private LayoutInflater mInflater;
    private Launcher mLauncher;
    private int mLayoutResource;

    private class InflateUrgentTask extends AsyncTask<IconInfo, Void, View> {
        private InflateUrgentTask() {
        }

        protected View doInBackground(IconInfo... params) {
            IconViewStub.this.prefetchIconResource(params[0]);
            return IconViewStub.this.inflateView();
        }

        protected void onPostExecute(final View view) {
            if (view != null) {
                IconViewStub.this.mLauncher.runOnUiThread(new Runnable() {
                    public void run() {
                        IconViewStub.this.replaceView(view);
                    }
                });
            }
        }
    }

    private class InflateBackgroundTask implements Job<View> {
        private IconInfo mIconInfo;

        public InflateBackgroundTask(IconInfo iconInfo) {
            this.mIconInfo = iconInfo;
        }

        public View run(JobContext jc) {
            if (jc.isCancelled()) {
                return null;
            }
            IconViewStub.this.prefetchIconResource(this.mIconInfo);
            return IconViewStub.this.inflateView();
        }
    }

    public IconViewStub(Context context) {
        this(context, 0);
    }

    public IconViewStub(Context context, int layoutResource) {
        this(context, null);
        this.mLayoutResource = layoutResource;
    }

    public IconViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IconViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mInflateListeners = new ArrayList();
        this.mLauncher = (Launcher) context;
        this.mInflatedId = -1;
        this.mLayoutResource = 0;
        setVisibility(View.GONE);
        setWillNotDraw(true);
    }

    public int getInflatedId() {
        return this.mInflatedId;
    }

    public void setInflatedId(int inflatedId) {
        this.mInflatedId = inflatedId;
    }

    public int getLayoutResource() {
        return this.mLayoutResource;
    }

    public void setLayoutResource(int layoutResource) {
        this.mLayoutResource = layoutResource;
    }

    public void setLayoutInflater(LayoutInflater inflater) {
        this.mInflater = inflater;
    }

    public LayoutInflater getLayoutInflater() {
        return this.mInflater;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @SuppressLint({"MissingSuperCall"})
    public void draw(Canvas canvas) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public void setVisibility(int visibility) {
        if (this.mInflatedViewRef != null) {
            View view = (View) this.mInflatedViewRef.get();
            if (view != null) {
                view.setVisibility(visibility);
                return;
            }
            throw new IllegalStateException("setVisibility called on un-referenced view");
        }
        super.setVisibility(visibility);
        if (visibility == 0 || visibility == 4) {
            inflateImmediately();
        }
    }

    @UiThread
    public View inflateImmediately() {
        if (this.mInflatedViewRef != null) {
            return (View) this.mInflatedViewRef.get();
        }
        if (!(this.mInflateUrgentTask == null || this.mInflateUrgentTask.isCancelled())) {
            this.mInflateUrgentTask.cancel(true);
        }
        if (!(this.mInflateBackgroundWorker == null || this.mInflateBackgroundWorker.isDone())) {
            this.mInflateBackgroundWorker.cancel();
        }
        View view = inflateView();
        if (view != null) {
            replaceView(view);
        } else {
            Log.e(TAG, "inflateImmediately : inflate fail");
        }
        return view;
    }

    public void inflateInBackground(@Nullable IconInfo infoToPrefetchIcon) {
        if (this.mInflatedViewRef == null && this.mInflateUrgentTask == null && this.mInflateBackgroundWorker == null) {
            this.mInflateBackgroundWorker = LauncherAppState.getInstance().getThreadPool().submit(new InflateBackgroundTask(infoToPrefetchIcon), new FutureListener<View>() {
                public void onFutureDone(Future<View> future) {
                    final View view = (View) future.get();
                    if (view != null) {
                        IconViewStub.this.mLauncher.runOnUiThread(new Runnable() {
                            public void run() {
                                IconViewStub.this.replaceView(view);
                            }
                        });
                    }
                }
            });
        }
    }

    public void inflateInBackgroundUrgent(@Nullable IconInfo infoToPrefetchIcon) {
        if (this.mInflatedViewRef == null && this.mInflateUrgentTask == null) {
            if (!(this.mInflateBackgroundWorker == null || this.mInflateBackgroundWorker.isDone())) {
                this.mInflateBackgroundWorker.cancel();
            }
            this.mInflateUrgentTask = new InflateUrgentTask();
            this.mInflateUrgentTask.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new IconInfo[]{infoToPrefetchIcon});
        }
    }

    private View inflateView() {
        if (this.mLayoutResource != 0) {
            LayoutInflater factory;
            if (this.mInflater != null) {
                factory = this.mInflater;
            } else {
                factory = LayoutInflater.from(getContext());
            }
            return factory.inflate(this.mLayoutResource, null, false);
        }
        Log.e(TAG, "InflateUrgentTask : inflater and layout resource id are not valid");
        return null;
    }

    @UiThread
    private void replaceView(final View view) {
        if (view != null) {
            if (!this.mLauncher.waitUntilResume(new Runnable() {
                public void run() {
                    IconViewStub.this.replaceView(view);
                }
            })) {
                ViewParent viewParent = getParent();
                if (viewParent == null || !(viewParent instanceof ViewGroup)) {
                    Log.w(TAG, "replaceView : already replaced or stub removed");
                    return;
                }
                ViewGroup parent = (ViewGroup) viewParent;
                if (this.mInflatedId != -1) {
                    view.setId(this.mInflatedId);
                }
                int index = parent.indexOfChild(this);
                parent.removeViewInLayout(this);
                LayoutParams layoutParams = getLayoutParams();
                if (layoutParams != null) {
                    parent.addView(view, index, layoutParams);
                } else {
                    parent.addView(view, index);
                }
                this.mInflatedViewRef = new WeakReference(view);
                Iterator it = this.mInflateListeners.iterator();
                while (it.hasNext()) {
                    ((OnInflateListener) it.next()).onInflate(this, view, false);
                }
                this.mInflateListeners.clear();
            }
        }
    }

    private void prefetchIconResource(IconInfo iconInfo) {
        if (iconInfo != null && !iconInfo.isPromise()) {
            LauncherAppState.getInstance().getIconCache().getTitleAndIcon(iconInfo, iconInfo.promisedIntent != null ? iconInfo.promisedIntent : iconInfo.intent, iconInfo.user, false);
        }
    }

    public void addOnInflateListener(OnInflateListener inflateListener) {
        if (!this.mInflateListeners.contains(inflateListener)) {
            this.mInflateListeners.add(inflateListener);
        }
    }
}
