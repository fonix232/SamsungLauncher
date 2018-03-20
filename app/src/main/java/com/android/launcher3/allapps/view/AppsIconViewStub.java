package com.android.launcher3.allapps.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
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
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.view.OnInflateListener;
import com.android.launcher3.common.view.Removable;
import com.android.launcher3.util.threadpool.Future;
import com.android.launcher3.util.threadpool.FutureListener;
import com.android.launcher3.util.threadpool.ThreadPool.Job;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import java.lang.ref.WeakReference;

public final class AppsIconViewStub extends View implements Stub, Removable {
    private static final String TAG = "AppsIconViewStub";
    private Future<View> mInflateBackgroundWorker;
    private OnInflateListener mInflateListener;
    private int mInflatedId;
    private WeakReference<View> mInflatedViewRef;
    private LayoutInflater mInflater;
    private Launcher mLauncher;
    private int mLayoutResource;
    private boolean mMarkToRemove;

    private class InflateBackgroundTask implements Job<View> {
        private ItemInfo mItemInfo;

        public InflateBackgroundTask(ItemInfo itemInfo) {
            this.mItemInfo = itemInfo;
        }

        public View run(JobContext jc) {
            if (jc.isCancelled() || AppsIconViewStub.this.mLauncher.isDestroyed()) {
                return null;
            }
            AppsIconViewStub.this.prefetchIconResource(this.mItemInfo);
            return AppsIconViewStub.this.inflateView();
        }
    }

    public AppsIconViewStub(Context context) {
        this(context, 0);
    }

    public AppsIconViewStub(Context context, int layoutResource) {
        this(context, null);
        this.mLayoutResource = layoutResource;
    }

    public AppsIconViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsIconViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppsIconViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMarkToRemove = false;
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

    public void inflateInBackground(@Nullable ItemInfo infoToPrefetchIcon) {
        if (this.mInflatedViewRef == null && this.mInflateBackgroundWorker == null) {
            this.mInflateBackgroundWorker = LauncherAppState.getInstance().getThreadPool().submit(new InflateBackgroundTask(infoToPrefetchIcon), new FutureListener<View>() {
                public void onFutureDone(final Future<View> future) {
                    final View view = (View) future.get();
                    AppsIconViewStub.this.mLauncher.runOnUiThread(new Runnable() {
                        public void run() {
                            AppsIconViewStub.this.replaceView(view, future.isCancelled());
                        }
                    });
                }
            });
        }
    }

    private View inflateView() {
        View view = null;
        if (this.mLayoutResource != 0) {
            LayoutInflater factory;
            if (this.mInflater != null) {
                factory = this.mInflater;
            } else {
                factory = LayoutInflater.from(getContext());
            }
            view = factory.inflate(this.mLayoutResource, null, false);
            if (view == null) {
                Log.e(TAG, "inflateView : inflate fail");
            }
        } else {
            Log.e(TAG, "InflateUrgentTask : inflater and layout resource id are not valid");
        }
        return view;
    }

    @UiThread
    public void replaceView(View view, boolean cancelled) {
        if (this.mLauncher.isDestroyed()) {
            Log.d(TAG, "ignore replaceView because launcher is destroyed");
            return;
        }
        ViewParent viewParent = getParent();
        if (cancelled || view == null || viewParent == null || !(viewParent instanceof ViewGroup) || this.mMarkToRemove) {
            Log.w(TAG, "replaceView ignored : " + (viewParent != null) + " , " + cancelled + " , " + this.mMarkToRemove);
        } else {
            ViewGroup parent = (ViewGroup) viewParent;
            if (this.mInflatedId != -1) {
                view.setId(this.mInflatedId);
            }
            int index = parent.indexOfChild(this);
            parent.removeViewInLayout(this);
            LayoutParams layoutParams = getLayoutParams();
            if (layoutParams != null) {
                if (layoutParams instanceof CellLayout.LayoutParams) {
                    ((CellLayout.LayoutParams) layoutParams).isLockedToGrid = true;
                }
                parent.addView(view, index, layoutParams);
            } else {
                parent.addView(view, index);
            }
            this.mInflatedViewRef = new WeakReference(view);
        }
        if (this.mInflateListener != null) {
            boolean cancel;
            if (this.mMarkToRemove || cancelled) {
                cancel = true;
            } else {
                cancel = false;
            }
            this.mInflateListener.onInflate(this, view, cancel);
            this.mInflateListener = null;
        }
    }

    private boolean useCache(IconInfo icon) {
        return !icon.isPromise() && icon.isDisabled == 0;
    }

    private void prefetchIconResource(ItemInfo itemInfo) {
        if (itemInfo instanceof IconInfo) {
            IconInfo iconInfo = (IconInfo) itemInfo;
            IconCache iconCache = LauncherAppState.getInstance().getIconCache();
            if (useCache(iconInfo)) {
                Intent intent;
                if (iconInfo.promisedIntent != null) {
                    intent = iconInfo.promisedIntent;
                } else {
                    intent = iconInfo.intent;
                }
                iconCache.getTitleAndIcon(iconInfo, intent, iconInfo.user, false);
                return;
            }
            Log.i(TAG, "This icon has exceptional bitmap, so we'll load the title from cache only! : " + itemInfo);
            LauncherActivityInfoCompat lai = LauncherAppsCompat.getInstance(this.mLauncher).resolveActivity(iconInfo.promisedIntent != null ? iconInfo.promisedIntent : iconInfo.intent, iconInfo.user);
            if (lai != null) {
                iconInfo.title = iconCache.getPackageItemTitle(lai);
            }
        }
    }

    public void setInflateListener(OnInflateListener inflateListener) {
        this.mInflateListener = inflateListener;
    }

    public void markToRemove(boolean tobeRemove) {
        this.mMarkToRemove = tobeRemove;
    }

    public boolean isMarkToRemove() {
        return this.mMarkToRemove;
    }

    public void cancelTask() {
        if (this.mInflateBackgroundWorker != null && !this.mInflateBackgroundWorker.isDone()) {
            this.mInflateBackgroundWorker.cancel();
        }
    }

    public void prefetchIconResInBackground(ItemInfo infoToPrefetch, Inflater inflater) {
    }

    public void replaceView(ItemInfo item, Inflater inflater, boolean cancelled) {
    }
}
