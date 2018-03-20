package com.android.launcher3.home;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Rect;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.view.LayoutInflater;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.TestHelper;
import java.util.ArrayList;
import java.util.Iterator;

public class LauncherAppWidgetHost extends AppWidgetHost {
    private static final String TAG = "LauncherAppWidgetHost";
    private Launcher mLauncher;
    private final ArrayList<Runnable> mProviderChangeListeners = new ArrayList();

    LauncherAppWidgetHost(Launcher launcher, int hostId) {
        super(launcher, hostId);
        this.mLauncher = launcher;
    }

    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new LauncherAppWidgetHostView(context);
    }

    public void startListening() {
        try {
            super.startListening();
        } catch (Exception e) {
            if (!(e.getCause() instanceof TransactionTooLargeException) && !TestHelper.isRoboUnitTest()) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stopListening() {
        super.stopListening();
        clearViews();
    }

    void addProviderChangeListener(Runnable callback) {
        this.mProviderChangeListeners.add(callback);
    }

    void removeProviderChangeListener(Runnable callback) {
        this.mProviderChangeListeners.remove(callback);
    }

    protected void onProvidersChanged() {
        Log.d(TAG, "onProvidersChanged");
        try {
            this.mLauncher.getLauncherModel().getWidgetsLoader().notifyDirty(null, null, true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (!this.mProviderChangeListeners.isEmpty()) {
            Iterator it = new ArrayList(this.mProviderChangeListeners).iterator();
            while (it.hasNext()) {
                ((Runnable) it.next()).run();
            }
        }
    }

    public AppWidgetHostView createView(Context context, int appWidgetId, LauncherAppWidgetProviderInfo appWidget) {
        Rect padding = DeviceProfile.getPaddingForWidget();
        if (appWidget.isCustomWidget) {
            LauncherAppWidgetHostView lahv = new LauncherAppWidgetHostView(context);
            ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(appWidget.initialLayout, lahv);
            lahv.setAppWidget(0, appWidget);
            lahv.updateLastInflationOrientation();
            lahv.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            return lahv;
        }
        AppWidgetHostView hv = super.createView(context, appWidgetId, appWidget);
        hv.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        return hv;
    }

    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        LauncherAppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, appWidget);
        if (info == null) {
            Log.e(TAG, "onProviderChanged. info is null. appWidgetId: " + appWidgetId);
        } else {
            super.onProviderChanged(appWidgetId, info);
        }
    }
}
