package com.android.launcher3.widget;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.WidgetsModel;
import com.android.launcher3.widget.model.WidgetPreviewLoader;
import com.android.launcher3.widget.view.WidgetCell;
import com.sec.android.app.launcher.R;
import java.util.List;

public class WidgetsListAdapter extends Adapter<WidgetsRowViewHolder> {
    private static final boolean DEBUG = false;
    private static final int PRESET_INDENT_SIZE_TABLET = 56;
    private static final String TAG = "WidgetsListAdapter";
    private OnClickListener mIconClickListener;
    private OnLongClickListener mIconLongClickListener;
    private int mIndent = 0;
    private Launcher mLauncher;
    private LayoutInflater mLayoutInflater;
    private WidgetPreviewLoader mWidgetPreviewLoader;
    private WidgetsModel mWidgetsModel;

    public WidgetsListAdapter(Context context, OnClickListener iconClickListener, OnLongClickListener iconLongClickListener, Launcher launcher) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mIconClickListener = iconClickListener;
        this.mIconLongClickListener = iconLongClickListener;
        this.mLauncher = launcher;
        setContainerHeight();
    }

    public void setWidgetsModel(WidgetsModel w) {
        this.mWidgetsModel = w;
    }

    public int getItemCount() {
        if (this.mWidgetsModel == null) {
            return 0;
        }
        return this.mWidgetsModel.getPackageSize();
    }

    public void onBindViewHolder(WidgetsRowViewHolder holder, int pos) {
        int i;
        List<Object> infoList = this.mWidgetsModel.getSortedWidgets(pos);
        ViewGroup row = (ViewGroup) holder.getContent().findViewById(R.id.widgets_cell_list);
        int diff = infoList.size() - row.getChildCount();
        if (diff > 0) {
            for (i = 0; i < diff; i++) {
                WidgetCell widget = (WidgetCell) this.mLayoutInflater.inflate(R.layout.widget_cell, row, false);
                widget.setOnClickListener(this.mIconClickListener);
                widget.setOnLongClickListener(this.mIconLongClickListener);
                LayoutParams lp = widget.getLayoutParams();
                lp.height = widget.cellSize;
                lp.width = widget.cellSize;
                widget.setLayoutParams(lp);
                row.addView(widget);
            }
        } else if (diff < 0) {
            for (i = infoList.size(); i < row.getChildCount(); i++) {
                row.getChildAt(i).setVisibility(View.GONE);
            }
        }
        if (getWidgetPreviewLoader() != null) {
            for (i = 0; i < infoList.size(); i++) {
                widget = (WidgetCell) row.getChildAt(i);
                if (infoList.get(i) instanceof LauncherAppWidgetProviderInfo) {
                    LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) infoList.get(i);
                    widget.setTag(new PendingAddWidgetInfo(this.mLauncher, info, null));
                    widget.applyFromAppWidgetProviderInfo(info, this.mWidgetPreviewLoader);
                } else if (infoList.get(i) instanceof ResolveInfo) {
                    ResolveInfo info2 = (ResolveInfo) infoList.get(i);
                    widget.setTag(new PendingAddShortcutInfo(info2));
                    widget.applyFromResolveInfo(this.mLauncher.getPackageManager(), info2, this.mWidgetPreviewLoader);
                }
                widget.ensurePreview();
                widget.setVisibility(View.VISIBLE);
            }
        }
    }

    public WidgetsRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup container = (ViewGroup) this.mLayoutInflater.inflate(R.layout.widgets_list_row_view, parent, false);
        ((LinearLayout) container.findViewById(R.id.widgets_cell_list)).setPaddingRelative(this.mIndent, 0, 1, 0);
        return new WidgetsRowViewHolder(container);
    }

    public void onViewRecycled(WidgetsRowViewHolder holder) {
        ViewGroup row = (ViewGroup) holder.getContent().findViewById(R.id.widgets_cell_list);
        for (int i = 0; i < row.getChildCount(); i++) {
            ((WidgetCell) row.getChildAt(i)).clear();
        }
    }

    public boolean onFailedToRecycleView(WidgetsRowViewHolder holder) {
        return true;
    }

    public long getItemId(int pos) {
        return (long) pos;
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (this.mWidgetPreviewLoader == null) {
            this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return this.mWidgetPreviewLoader;
    }

    private void setContainerHeight() {
        Resources r = this.mLauncher.getResources();
        if (LauncherFeature.isLargeTablet() || LauncherFeature.isTablet()) {
            this.mIndent = Utilities.pxFromDp(56.0f, r.getDisplayMetrics());
        }
    }
}
