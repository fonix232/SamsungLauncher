package com.android.launcher3.appspicker;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.sec.android.app.launcher.R;
import java.util.List;

public class AppsPickerListAdapter extends BaseAdapter {
    private static final String TAG = "AppsPickerListAdapter";
    private AppsPickerAlphabeticalAppsList mAppsList;
    private ComponentName mBouncedApp;
    private UserHandle mBouncedAppUser;
    public View mBouncedHiddenAppView;
    private Context mContext;
    private boolean mIsWhiteBg;
    private OnClickListener mItemCheckBoxClickListener;
    private OnClickListener mItemContainerClickListener;
    private AppsPickerInfoInterface mItemSelectedListener;
    private AppsPickerFocusListener mKeyListener = new AppsPickerFocusListener();
    private int mMaxNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_MaxAppsPerRow);
    private int mNumAppsPerRow = this.mAppsList.getNumAppsPerRow();
    private int mScreenMode = Utilities.getOrientation();
    private int mTextColor;

    public AppsPickerListAdapter(Context context, AppsPickerAlphabeticalAppsList apps, boolean isWhiteBg) {
        this.mContext = context;
        this.mAppsList = apps;
        this.mIsWhiteBg = isWhiteBg;
        setContentColorAndBackground(this.mIsWhiteBg);
        this.mItemContainerClickListener = new OnClickListener() {
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.list_item_checkbox);
                if (checkBox != null && (v.getTag() instanceof IconInfo)) {
                    String description;
                    IconInfo icon = (IconInfo) v.getTag();
                    checkBox.toggle();
                    if (checkBox.isChecked()) {
                        description = icon.title + " " + AppsPickerListAdapter.this.mContext.getResources().getString(R.string.selected);
                    } else {
                        description = icon.title + " " + AppsPickerListAdapter.this.mContext.getResources().getString(R.string.not_selected);
                    }
                    v.setContentDescription(description);
                    Talk.INSTANCE.say(description);
                    if (AppsPickerListAdapter.this.mItemSelectedListener != null) {
                        AppsPickerListAdapter.this.mItemSelectedListener.onToggleItem(icon);
                    }
                }
            }
        };
        this.mItemCheckBoxClickListener = new OnClickListener() {
            public void onClick(View v) {
                if ((v.getTag() instanceof IconInfo) && AppsPickerListAdapter.this.mItemSelectedListener != null) {
                    AppsPickerListAdapter.this.mItemSelectedListener.onToggleItem((IconInfo) v.getTag());
                }
            }
        };
    }

    public void setToSelectedListener(AppsPickerInfoInterface listener) {
        this.mItemSelectedListener = listener;
    }

    public int getCount() {
        return this.mAppsList.getAppsMapSize();
    }

    public IconInfo getItem(int position) {
        Log.d(TAG, "getItem position : " + position);
        List<AdapterItem> rowItems = this.mAppsList.getRowItems(position);
        if (rowItems == null || rowItems.get(0) == null) {
            return null;
        }
        return ((AdapterItem) this.mAppsList.getAdapterItems().get(((AdapterItem) rowItems.get(0)).position)).iconInfo;
    }

    public long getItemId(int position) {
        IconInfo item = null;
        if (this.mAppsList.getAppsMapSize() > 0) {
            item = getItem(position);
        }
        return item != null ? item.id : -1;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        AppIconViewHolder[] holders;
        TextView title;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.apps_picker_list_container, null);
            holders = createHolders(inflater, (LinearLayout) convertView.findViewById(R.id.item_container));
            title = (TextView) convertView.findViewById(R.id.header_text);
            convertView.setTag(holders);
        } else {
            holders = (AppIconViewHolder[]) convertView.getTag();
            title = (TextView) convertView.findViewById(R.id.header_text);
            if (holders[0].screenMode != this.mScreenMode) {
                convertView = inflater.inflate(R.layout.apps_picker_list_container, null);
                holders = createHolders(inflater, (LinearLayout) convertView.findViewById(R.id.item_container));
                title = (TextView) convertView.findViewById(R.id.header_text);
                convertView.setTag(holders);
            }
        }
        String titleInitial = "";
        LinearLayout header = (LinearLayout) convertView.findViewById(R.id.header);
        ((FrameLayout) convertView.findViewById(R.id.apps_picker_appsearch_divider)).setBackgroundColor(this.mTextColor);
        header.setVisibility(View.VISIBLE);
        List<AdapterItem> items = this.mAppsList.getRowItems(position);
        int i;
        if (items == null) {
            Log.d(TAG, "items is null");
            holders = (AppIconViewHolder[]) convertView.getTag();
            for (i = 0; i < holders.length; i++) {
                holders[i].iconInfo = null;
                holders[i].container.setFocusable(false);
                holders[i].container.setOnClickListener(null);
                holders[i].container.setVisibility(View.GONE);
                holders[i].checkBox.setTag(null);
                holders[i].container.setTag(null);
                holders[i].checkBox.setOnClickListener(null);
                holders[i].container.setOnClickListener(null);
                holders[i].checkBox.setChecked(false);
                holders[i].title.setText(null);
                holders[i].icon.setImageBitmap(null);
            }
        } else {
            i = 0;
            while (i < holders.length) {
                IconInfo info = null;
                AdapterItem rowFirstItem = null;
                if (i < items.size() && items.get(i) != null) {
                    rowFirstItem = (AdapterItem) items.get(0);
                    info = ((AdapterItem) items.get(i)).iconInfo;
                }
                if (info == null || i >= this.mNumAppsPerRow) {
                    holders[i].iconInfo = null;
                    holders[i].container.setFocusable(false);
                    holders[i].container.setOnClickListener(null);
                    if (i < this.mNumAppsPerRow) {
                        holders[i].container.setVisibility(4);
                        if (i > 0 && holders[i].leftGap != null) {
                            holders[i].leftGap.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holders[i].container.setVisibility(View.GONE);
                        if (i > 0 && holders[i].leftGap != null) {
                            holders[i].leftGap.setVisibility(View.GONE);
                        }
                    }
                    holders[i].checkBox.setTag(null);
                    holders[i].container.setTag(null);
                    holders[i].checkBox.setOnClickListener(null);
                    holders[i].container.setOnClickListener(null);
                    holders[i].checkBox.setChecked(false);
                    holders[i].title.setText(null);
                    holders[i].icon.setImageBitmap(null);
                } else {
                    holders[i].iconInfo = info;
                    holders[i].container.setFocusable(true);
                    holders[i].container.setOnClickListener(this.mItemContainerClickListener);
                    holders[i].container.setVisibility(View.VISIBLE);
                    holders[i].checkBox.setTag(holders[i].iconInfo);
                    holders[i].container.setOnKeyListener(this.mKeyListener);
                    holders[i].container.setTag(holders[i].iconInfo);
                    holders[i].colIndex = i;
                    holders[i].rowIndex = position;
                    if (i > 0 && holders[i].leftGap != null) {
                        holders[i].leftGap.setVisibility(View.VISIBLE);
                    }
                    holders[i].title.setText(info.title);
                    WhiteBgManager.changeTextColorForBg(this.mContext, holders[i].title, this.mIsWhiteBg);
                    holders[i].icon.setImageBitmap(info.mIcon);
                    boolean isChecked = false;
                    if (this.mItemSelectedListener != null) {
                        isChecked = this.mItemSelectedListener.isCheckedItem(info);
                    }
                    if (isChecked) {
                        holders[i].container.setContentDescription(holders[i].title.getText() + " " + this.mContext.getResources().getString(R.string.selected));
                    } else {
                        holders[i].container.setContentDescription(holders[i].title.getText() + " " + this.mContext.getResources().getString(R.string.not_selected));
                    }
                    holders[i].checkBox.setChecked(isChecked);
                    holders[i].checkBox.setOnClickListener(this.mItemCheckBoxClickListener);
                    if (this.mBouncedHiddenAppView == null && this.mBouncedApp != null && this.mBouncedAppUser != null && this.mBouncedApp.equals(info.componentName) && this.mBouncedAppUser.equals(info.getUserHandle().getUser())) {
                        this.mBouncedHiddenAppView = holders[i].container;
                        Log.d(TAG, "found : " + this.mBouncedHiddenAppView);
                    }
                }
                if (!(holders[i].iconInfo == null || rowFirstItem == null)) {
                    titleInitial = rowFirstItem.sectionName;
                }
                if (rowFirstItem != null && rowFirstItem.position >= 1) {
                    int index = rowFirstItem.position;
                    if (((AdapterItem) this.mAppsList.getAdapterItems().get(index - 1)).iconInfo != null && titleInitial.equals(((AdapterItem) this.mAppsList.getAdapterItems().get(index - 1)).sectionName)) {
                        header.setVisibility(View.GONE);
                    }
                }
                title.setText(titleInitial);
                title.setTextColor(this.mTextColor);
                i++;
            }
        }
        return convertView;
    }

    private AppIconViewHolder[] createHolders(LayoutInflater inflater, LinearLayout itemContainer) {
        AppIconViewHolder[] holders = new AppIconViewHolder[this.mMaxNumAppsPerRow];
        for (int i = 0; i < holders.length; i++) {
            ViewGroup itemView;
            if (Utilities.canScreenRotate() && this.mScreenMode == 2) {
                itemView = (ViewGroup) inflater.inflate(R.layout.apps_picker_list_item_landscape, null);
            } else {
                itemView = (ViewGroup) inflater.inflate(R.layout.apps_picker_list_item, null);
            }
            holders[i] = new AppIconViewHolder();
            holders[i].container = itemView;
            holders[i].icon = (ImageView) itemView.findViewById(R.id.list_item_icon);
            holders[i].title = (TextView) itemView.findViewById(R.id.list_item_text);
            holders[i].checkBox = (CheckBox) itemView.findViewById(R.id.list_item_checkbox);
            applyCheckBoxStyle(this.mContext, holders[i].checkBox);
            holders[i].screenMode = this.mScreenMode;
            if (i > 0) {
                LayoutParams blankViewParam = new LayoutParams(-2, -2);
                blankViewParam.weight = 1.0f;
                holders[i].leftGap = new View(this.mContext);
                itemContainer.addView(holders[i].leftGap, blankViewParam);
            }
            LayoutParams itemViewParam = new LayoutParams(-2, -2);
            itemViewParam.weight = 0.0f;
            itemContainer.addView(itemView, itemViewParam);
        }
        return holders;
    }

    public static void applyCheckBoxStyle(Context context, CheckBox checkBox) {
        boolean isPortrait = Utilities.isPortrait();
        Resources res = context.getResources();
        int iconPaddingStart = (int) (((double) res.getDimension(R.dimen.multi_select_checkbox_width)) * -0.25d);
        int iconPaddingTop = (int) (((double) res.getDimension(R.dimen.multi_select_checkbox_height)) * -0.085d);
        checkBox.semSetButtonDrawableSize(res.getDimensionPixelSize(R.dimen.multi_select_checkbox_width), res.getDimensionPixelSize(R.dimen.multi_select_checkbox_height));
        MarginLayoutParams lp = (MarginLayoutParams) checkBox.getLayoutParams();
        if (!isPortrait) {
            iconPaddingTop = 0;
        }
        lp.semSetMarginsRelative(iconPaddingStart, iconPaddingTop, 0, 0);
        checkBox.setLayoutParams(lp);
    }

    private void setContentColorAndBackground(boolean whiteBg) {
        this.mTextColor = this.mContext.getResources().getColor(whiteBg ? R.color.apps_picker_black_color : R.color.apps_picker_white_color, null);
    }

    public void setMaxNumbAppsPerRow() {
        this.mMaxNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_MaxAppsPerRow);
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        this.mScreenMode = Utilities.getOrientation();
        this.mNumAppsPerRow = this.mAppsList.getNumAppsPerRow();
    }

    public void setBouncedApp(ComponentName cn, UserHandle user) {
        this.mBouncedApp = cn;
        this.mBouncedAppUser = user;
    }

    public void resetBouncedAppInfo() {
        this.mBouncedApp = null;
        this.mBouncedAppUser = null;
        this.mBouncedHiddenAppView = null;
    }
}
