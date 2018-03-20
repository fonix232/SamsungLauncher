package com.android.launcher3.allapps.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.util.Log;
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
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.util.threadpool.Future;
import com.android.launcher3.util.threadpool.FutureListener;
import com.android.launcher3.util.threadpool.ThreadPool.Job;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public final class FolderIconViewStub extends View implements Stub, Removable {
    private static final String TAG = "FolderIconViewStub";
    private OnInflateListener mInflateListener;
    private WeakReference<View> mInflatedViewRef;
    private Launcher mLauncher;
    private boolean mMarkToRemove;
    private Future<ItemInfo> mPrefetchIconResBackgroundWorker;

    private class IconResourceBackgroundTask implements Job<ItemInfo> {
        private ItemInfo mItemInfo;

        public IconResourceBackgroundTask(ItemInfo itemInfo) {
            this.mItemInfo = itemInfo;
        }

        public ItemInfo run(JobContext jc) {
            FolderIconViewStub.this.prefetchIconResource(jc, this.mItemInfo);
            return this.mItemInfo;
        }
    }

    public FolderIconViewStub(Context context) {
        this(context, null);
    }

    public FolderIconViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderIconViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FolderIconViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMarkToRemove = false;
        this.mLauncher = (Launcher) context;
        setVisibility(View.GONE);
        setWillNotDraw(true);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @SuppressLint({"MissingSuperCall"})
    public void draw(Canvas canvas) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public void prefetchIconResInBackground(@Nullable ItemInfo infoToPrefetch, final Inflater inflater) {
        if (this.mInflatedViewRef == null && this.mPrefetchIconResBackgroundWorker == null && inflater != null) {
            this.mPrefetchIconResBackgroundWorker = LauncherAppState.getInstance().getThreadPool().submit(new IconResourceBackgroundTask(infoToPrefetch), new FutureListener<ItemInfo>() {
                public void onFutureDone(final Future<ItemInfo> future) {
                    final ItemInfo item = (ItemInfo) future.get();
                    FolderIconViewStub.this.mLauncher.runOnUiThread(new Runnable() {
                        public void run() {
                            FolderIconViewStub.this.replaceView(item, inflater, future.isCancelled());
                        }
                    });
                }
            });
        }
    }

    public void replaceView(ItemInfo item, Inflater inflater, boolean cancelled) {
        if (this.mLauncher.isDestroyed()) {
            Log.d(TAG, "ignore replaceView because launcher is destroyed");
            return;
        }
        ViewParent viewParent = getParent();
        View view = null;
        if (!cancelled) {
            Log.d(TAG, "folder bind start : " + item);
            view = inflater.inflateView(item);
            Log.d(TAG, "folder bind end : " + item);
        }
        if (cancelled || view == null || viewParent == null || !(viewParent instanceof ViewGroup)) {
            Log.w(TAG, "replaceView : already replaced or stub removed");
        } else {
            ViewGroup parent = (ViewGroup) viewParent;
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
            this.mInflateListener.onInflate(this, view, cancelled);
            this.mInflateListener = null;
        }
    }

    @UiThread
    public void replaceView(View view, boolean cancelled) {
        if (this.mLauncher.isDestroyed()) {
            Log.d(TAG, "ignore replaceView because launcher is destroyed");
            return;
        }
        ViewParent viewParent = getParent();
        if (cancelled || view == null || viewParent == null || !(viewParent instanceof ViewGroup)) {
            Log.w(TAG, "replaceView : already replaced or stub removed");
        } else {
            ViewGroup parent = (ViewGroup) viewParent;
            int index = parent.indexOfChild(this);
            parent.removeViewInLayout(this);
            LayoutParams layoutParams = getLayoutParams();
            if (layoutParams != null) {
                parent.addView(view, index, layoutParams);
            } else {
                parent.addView(view, index);
            }
            this.mInflatedViewRef = new WeakReference(view);
        }
        if (this.mInflateListener != null) {
            this.mInflateListener.onInflate(this, view, cancelled);
            this.mInflateListener = null;
        }
    }

    private boolean useCache(IconInfo icon) {
        return !icon.isPromise() && icon.isDisabled == 0;
    }

    private void prefetchIconResource(JobContext jc, ItemInfo itemInfo) {
        FolderInfo folderInfo = (FolderInfo) itemInfo;
        IconCache iconCache = LauncherAppState.getInstance().getIconCache();
        ArrayList<IconInfo> infos = folderInfo.contents;
        int prefetchCount = Math.min(infos.size(), 9);
        for (int i = 0; i < prefetchCount && !jc.isCancelled() && !this.mLauncher.isDestroyed(); i++) {
            IconInfo info = (IconInfo) infos.get(i);
            if (useCache(info)) {
                Intent intent;
                if (info.promisedIntent != null) {
                    intent = info.promisedIntent;
                } else {
                    intent = info.intent;
                }
                iconCache.getTitleAndIcon(info, intent, info.user, false);
            } else {
                Log.i(TAG, "This icon has exceptional bitmap, so we'll load the title from cache only! : " + info);
                LauncherActivityInfoCompat lai = LauncherAppsCompat.getInstance(this.mLauncher).resolveActivity(info.promisedIntent != null ? info.promisedIntent : info.intent, info.user);
                if (lai != null) {
                    info.title = iconCache.getPackageItemTitle(lai);
                }
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
        if (this.mPrefetchIconResBackgroundWorker != null && !this.mPrefetchIconResBackgroundWorker.isDone()) {
            this.mPrefetchIconResBackgroundWorker.cancel();
        }
    }

    public void inflateInBackground(ItemInfo infoToPrefetchIcon) {
    }
}
