package com.android.launcher3.common.model;

import android.content.Context;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import com.android.launcher3.common.model.BadgeSettingsFragment.BadgeAppItem;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.List;

public class BadgeSettingsAdapter extends Adapter<ViewHolder> {
    private static final String TAG = "BadgeSettingsAdapter";
    private Context mContext;
    private OnChangeListener mOnChangeListener;
    private final List<BadgeAppItem> mValues;

    public interface OnChangeListener {
        void onChange(View view, int i);
    }

    public class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder {
        public final ImageView appIcon;
        public BadgeAppItem mItem;
        public final Switch mSwitch;
        public final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view.findViewById(R.id.badge_item_layout);
            this.appIcon = (ImageView) view.findViewById(R.id.badge_icon);
            this.mSwitch = (Switch) view.findViewById(R.id.switchBadge);
        }

        public String toString() {
            return super.toString() + " '" + this.mSwitch.getText() + "'";
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        this.mOnChangeListener = listener;
    }

    public BadgeSettingsAdapter(Context context, List<BadgeAppItem> items) {
        this.mValues = items;
        this.mContext = context;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.badge_settings_list_item, parent, false));
    }

    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = (BadgeAppItem) this.mValues.get(position);
        if (holder.appIcon != null) {
            holder.appIcon.setImageDrawable(holder.mItem.getAppIcon());
        }
        if (holder.mSwitch != null) {
            holder.mSwitch.setText(holder.mItem.getTitle());
            holder.mSwitch.setChecked(!holder.mItem.isHidden());
        }
        Log.d(TAG, "onBindViewHolder: postion=" + position + "," + holder.mItem.getTitle());
        holder.mView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean checked = holder.mSwitch.isChecked();
                holder.mItem.setHidden(checked);
                holder.mSwitch.setChecked(!checked);
                Log.d(BadgeSettingsAdapter.TAG, "onClick: " + holder.mItem.toString());
                BadgeSettingsAdapter.this.mOnChangeListener.onChange(v, position);
                SALogging.getInstance().insertEventLog(BadgeSettingsAdapter.this.mContext.getResources().getString(R.string.screen_HomeSettings), BadgeSettingsAdapter.this.mContext.getResources().getString(R.string.event_HideAppsBadge), holder.mItem.toString());
            }
        });
    }

    public int getItemCount() {
        return this.mValues.size();
    }
}
