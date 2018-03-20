package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.view.DragLayer.LayoutParams;
import com.android.launcher3.home.AppWidgetResizeFrame;
import com.android.launcher3.home.HomeController;
import com.android.launcher3.home.LauncherAppWidgetHost;
import com.android.launcher3.home.LauncherAppWidgetHostView;

public class WidgetHostViewLoader implements DragListener {
    private Runnable mBindWidgetRunnable = null;
    private Handler mHandler;
    private Runnable mInflateWidgetRunnable = null;
    private final PendingAddWidgetInfo mInfo;
    private Launcher mLauncher;
    private final View mView;
    private int mWidgetLoadingId = -1;

    public WidgetHostViewLoader(Launcher launcher, View view) {
        this.mLauncher = launcher;
        this.mHandler = new Handler();
        this.mView = view;
        this.mInfo = (PendingAddWidgetInfo) view.getTag();
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        return true;
    }

    public boolean onDragEnd() {
        this.mLauncher.getDragMgr().removeDragListener(this);
        this.mHandler.removeCallbacks(this.mBindWidgetRunnable);
        this.mHandler.removeCallbacks(this.mInflateWidgetRunnable);
        LauncherAppWidgetHost appWidgetHost = this.mLauncher.getHomeController().getAppWidgetHost();
        if (this.mWidgetLoadingId != -1) {
            appWidgetHost.deleteAppWidgetId(this.mWidgetLoadingId);
            this.mWidgetLoadingId = -1;
        }
        if (this.mInfo.boundWidget != null) {
            this.mLauncher.getDragLayer().removeView(this.mInfo.boundWidget);
            appWidgetHost.deleteAppWidgetId(this.mInfo.boundWidget.getAppWidgetId());
            this.mInfo.boundWidget = null;
        }
        return true;
    }

    public boolean preloadWidget() {
        final LauncherAppWidgetProviderInfo pInfo = this.mInfo.info;
        if (pInfo == null || pInfo.isCustomWidget) {
            return false;
        }
        final Bundle options = getDefaultOptionsForWidget(this.mLauncher, this.mInfo);
        if (pInfo.configure != null) {
            this.mInfo.bindOptions = options;
            return false;
        }
        this.mBindWidgetRunnable = new Runnable() {
            public void run() {
                WidgetHostViewLoader.this.mWidgetLoadingId = WidgetHostViewLoader.this.mLauncher.getHomeController().getAppWidgetHost().allocateAppWidgetId();
                if (AppWidgetManagerCompat.getInstance(WidgetHostViewLoader.this.mLauncher).bindAppWidgetIdIfAllowed(WidgetHostViewLoader.this.mWidgetLoadingId, pInfo, options)) {
                    WidgetHostViewLoader.this.mHandler.post(WidgetHostViewLoader.this.mInflateWidgetRunnable);
                }
            }
        };
        this.mInflateWidgetRunnable = new Runnable() {
            public void run() {
                if (WidgetHostViewLoader.this.mWidgetLoadingId != -1) {
                    HomeController homeController = WidgetHostViewLoader.this.mLauncher.getHomeController();
                    AppWidgetHostView hostView = homeController.getAppWidgetHost().createView(WidgetHostViewLoader.this.mLauncher, WidgetHostViewLoader.this.mWidgetLoadingId, pInfo);
                    WidgetHostViewLoader.this.mInfo.boundWidget = hostView;
                    WidgetHostViewLoader.this.mWidgetLoadingId = -1;
                    if (LauncherFeature.supportGSARoundingFeature() && pInfo.provider.getPackageName().equals(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME)) {
                        ((LauncherAppWidgetHostView) hostView).mIsGSB = true;
                        Bundle opts = new Bundle();
                        opts.putString("attached-launcher-identifier", "samsung-dream-launcher");
                        opts.putString("requested-widget-style", "cqsb");
                        opts.putFloat("widget-screen-bounds-left", (float) hostView.getLeft());
                        opts.putFloat("widget-screen-bounds-top", (float) hostView.getTop());
                        opts.putFloat("widget-screen-bounds-right", (float) hostView.getRight());
                        opts.putFloat("widget-screen-bounds-bottom", (float) hostView.getBottom());
                        hostView.updateAppWidgetOptions(opts);
                    }
                    hostView.setVisibility(4);
                    int[] unScaledSize = homeController.getWorkspace().estimateItemSize(WidgetHostViewLoader.this.mInfo);
                    LayoutParams lp = new LayoutParams(unScaledSize[0], unScaledSize[1]);
                    lp.y = 0;
                    lp.x = 0;
                    lp.customPosition = true;
                    hostView.setLayoutParams(lp);
                    WidgetHostViewLoader.this.mLauncher.getDragLayer().addView(hostView);
                    WidgetHostViewLoader.this.mView.setTag(WidgetHostViewLoader.this.mInfo);
                }
            }
        };
        this.mHandler.post(this.mBindWidgetRunnable);
        return true;
    }

    public static Bundle getDefaultOptionsForWidget(Context context, PendingAddWidgetInfo info) {
        Rect rect = AppWidgetResizeFrame.getWidgetSizeRanges(context, null, info.spanX, info.spanY);
        Rect padding = DeviceProfile.getPaddingForWidget();
        float density = context.getResources().getDisplayMetrics().density;
        int xPaddingDips = (int) (((float) (padding.left + padding.right)) / density);
        int yPaddingDips = (int) (((float) (padding.top + padding.bottom)) / density);
        Bundle options = new Bundle();
        options.putInt("appWidgetMinWidth", rect.left - xPaddingDips);
        options.putInt("appWidgetMinHeight", rect.top - yPaddingDips);
        options.putInt("appWidgetMaxWidth", rect.right - xPaddingDips);
        options.putInt("appWidgetMaxHeight", rect.bottom - yPaddingDips);
        return options;
    }

    public void onHostViewDropped() {
        this.mHandler.removeCallbacks(this.mBindWidgetRunnable);
        this.mHandler.removeCallbacks(this.mInflateWidgetRunnable);
    }
}
