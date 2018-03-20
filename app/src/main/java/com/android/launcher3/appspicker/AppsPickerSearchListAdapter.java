package com.android.launcher3.appspicker;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.locale.LocaleUtils;
import com.android.launcher3.util.locale.hanzi.PrefixHighlighter;
import com.sec.android.app.launcher.R;

public class AppsPickerSearchListAdapter extends BaseAdapter {
    private static final String TAG = AppsPickerSearchListAdapter.class.getSimpleName();
    private AppsPickerAlphabeticalAppsList mAppsList;
    private Context mContext;
    private int mHighlightTextColor = this.mContext.getResources().getColor(R.color.apps_search_app_name_highlight_color, null);
    private PrefixHighlighter mHighlighterForHans;
    private boolean mIsWhiteBg;
    private OnClickListener mItemCheckBoxClickListener;
    private OnClickListener mItemContainerClickListener;
    private AppsPickerInfoInterface mItemSelectedListener;
    private AppsPickerFocusListener mKeyListener;
    private int mMaxNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_MaxAppsPerRow);
    private int mNumAppsPerRow = this.mAppsList.getNumAppsPerRow();
    private int mScreenMode = Utilities.getOrientation();
    private String mSearchText = "";

    public AppsPickerSearchListAdapter(Context context, AppsPickerAlphabeticalAppsList apps, boolean isWhiteBg) {
        this.mContext = context;
        this.mAppsList = apps;
        this.mIsWhiteBg = isWhiteBg;
        this.mKeyListener = new AppsPickerFocusListener();
        this.mItemContainerClickListener = new OnClickListener() {
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.list_item_checkbox);
                if (checkBox != null && (v.getTag() instanceof IconInfo)) {
                    String description;
                    IconInfo icon = (IconInfo) v.getTag();
                    checkBox.toggle();
                    if (checkBox.isChecked()) {
                        description = icon.title + " " + AppsPickerSearchListAdapter.this.mContext.getResources().getString(R.string.selected);
                    } else {
                        description = icon.title + " " + AppsPickerSearchListAdapter.this.mContext.getResources().getString(R.string.not_selected);
                    }
                    v.setContentDescription(description);
                    Talk.INSTANCE.say(description);
                    if (AppsPickerSearchListAdapter.this.mItemSelectedListener != null) {
                        AppsPickerSearchListAdapter.this.mItemSelectedListener.onToggleItem(icon);
                    }
                }
            }
        };
        this.mItemCheckBoxClickListener = new OnClickListener() {
            public void onClick(View v) {
                if ((v.getTag() instanceof IconInfo) && AppsPickerSearchListAdapter.this.mItemSelectedListener != null) {
                    AppsPickerSearchListAdapter.this.mItemSelectedListener.onToggleItem((IconInfo) v.getTag());
                }
            }
        };
    }

    public void setToSelectedListener(AppsPickerInfoInterface listener) {
        this.mItemSelectedListener = listener;
    }

    public int getCount() {
        return ((this.mAppsList.getNumFilteredApps() + this.mNumAppsPerRow) - 1) / this.mNumAppsPerRow;
    }

    public IconInfo getItem(int position) {
        Log.d(TAG, "getItem position : " + position);
        return ((AdapterItem) this.mAppsList.getAdapterItems().get(this.mNumAppsPerRow * position)).iconInfo;
    }

    public long getItemId(int position) {
        IconInfo item = getItem(position);
        return item != null ? item.id : -1;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        AppIconViewHolder[] holders;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.apps_picker_searchlist_container, null);
            holders = createHolders(inflater, (LinearLayout) convertView.findViewById(R.id.item_container));
            convertView.setTag(holders);
        } else {
            holders = (AppIconViewHolder[]) convertView.getTag();
            if (holders[0].screenMode != this.mScreenMode) {
                convertView = inflater.inflate(R.layout.apps_picker_searchlist_container, null);
                holders = createHolders(inflater, (LinearLayout) convertView.findViewById(R.id.item_container));
                convertView.setTag(holders);
            }
        }
        int row = position * this.mNumAppsPerRow;
        int i = 0;
        while (i < this.mMaxNumAppsPerRow) {
            int adapterIndex = row + i;
            IconInfo info = null;
            if (adapterIndex < this.mAppsList.getAdapterItems().size()) {
                info = ((AdapterItem) this.mAppsList.getAdapterItems().get(adapterIndex)).iconInfo;
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
                holders[i].container.setTag(holders[i].iconInfo);
                holders[i].colIndex = i;
                holders[i].container.setOnKeyListener(this.mKeyListener);
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
                if (!this.mSearchText.isEmpty()) {
                    displayHighlightedName(holders[i].title, info.title.toString());
                }
            }
            i++;
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
            AppsPickerListAdapter.applyCheckBoxStyle(this.mContext, holders[i].checkBox);
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

    public void setSearchText(String search) {
        this.mSearchText = search;
    }

    public String getSearchText() {
        return this.mSearchText;
    }

    private void displayHighlightedName(TextView view, String appName) {
        this.mSearchText = this.mSearchText.trim();
        int highlightStrLength = this.mSearchText.length();
        Spannable highLightText = new SpannableString(appName);
        String queryForIndian = queryForIndia(view, appName);
        if (LocaleUtils.isChinesePinyinSearching()) {
            if (this.mHighlighterForHans == null) {
                this.mHighlighterForHans = new PrefixHighlighter(this.mHighlightTextColor);
            }
            view.setText(this.mHighlighterForHans.apply(appName, this.mSearchText));
        } else if (queryForIndian != null) {
            setTextHighLight(view, appName.toLowerCase().indexOf(queryForIndian.toLowerCase()), highLightText, queryForIndian.length());
        } else {
            setTextHighLight(view, appName.toLowerCase().indexOf(this.mSearchText.toLowerCase()), highLightText, highlightStrLength);
        }
    }

    private void setTextHighLight(TextView view, int indexOf, Spannable highLightText, int highlightStrLength) {
        if (indexOf <= indexOf + highlightStrLength && indexOf > -1) {
            highLightText.setSpan(new ForegroundColorSpan(this.mHighlightTextColor), indexOf, indexOf + highlightStrLength, 0);
            view.setText(highLightText);
        }
    }

    private String queryForIndia(TextView view, String appName) {
        char[] isQueryForIndian = TextUtils.semGetPrefixCharForSpan(view.getPaint(), appName, this.mSearchText.toCharArray());
        if (isQueryForIndian == null) {
            return null;
        }
        return new String(isQueryForIndian);
    }

    public void setMaxNumbAppsPerRow() {
        this.mMaxNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_MaxAppsPerRow);
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        this.mNumAppsPerRow = this.mAppsList.getNumAppsPerRow();
        this.mScreenMode = Utilities.getOrientation();
    }
}
