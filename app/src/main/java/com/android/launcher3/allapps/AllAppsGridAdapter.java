package com.android.launcher3.allapps;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.LayoutParams;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.allapps.AlphabeticalAppsList.SectionInfo;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.view.IconView;
import com.sec.android.app.launcher.R;
import java.util.HashMap;
import java.util.List;

public class AllAppsGridAdapter extends Adapter<ViewHolder> {
    public static final int APPS_ALL_LIST_HEADER_VIEW_TYPE = 11;
    private static final long DEALYMILLISEC = 700;
    public static final int EMPTY_SEARCH_VIEW_TYPE = 3;
    public static final int GALAXY_APPS_RESULT_ICON_VIEW_TYPE = 12;
    public static final int ICON_VIEW_TYPE = 1;
    public static final int NO_RECENT_HISTORY_VIEW_TYPE = 6;
    public static final int PREDICTION_ICON_VIEW_TYPE = 2;
    public static final int RECENT_HISTORY_VIEW_TYPE = 7;
    public static final int SEARCH_GALAXY_BTN_VIEW_TYPE = 9;
    public static final int SEARCH_MARKET_BTN_VIEW_TYPE = 10;
    public static final int SEARCH_MARKET_DIVIDER_VIEW_TYPE = 4;
    public static final int SEARCH_MARKET_TITLE_VIEW_TYPE = 8;
    public static final int SEARCH_MARKET_VIEW_TYPE = 5;
    public static final int SECTION_BREAK_VIEW_TYPE = 0;
    public static final String TAG = "AppsGridAdapter";
    private AlphabeticalAppsList mApps;
    private int mAppsPerRow;
    private final Rect mBackgroundPadding = new Rect();
    private String mEmptySearchMessage;
    OnClickListener mGalaxyAppsBtnClickListener = new OnClickListener() {
        public void onClick(View v) {
            String title = AllAppsGridAdapter.this.mSearchText.toString();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("samsungapps://SearchResult/"));
            intent.putExtra("sKeyword", title);
            intent.addFlags(270532608);
            intent.addFlags(65536);
            try {
                AllAppsGridAdapter.this.mLauncher.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(AllAppsGridAdapter.TAG, "Unable to launch. tag= intent=" + intent, e);
            } catch (SecurityException e2) {
                Log.e(AllAppsGridAdapter.TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + " intent=" + intent, e2);
            }
        }
    };
    OnClickListener mGalaxyAppsIconClickListener = new OnClickListener() {
        public void onClick(View v) {
            String string_of_uri = "samsungapps://ProductDetail/" + ((ItemDetails) v.getTag()).getPkgName();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse(string_of_uri));
            intent.addFlags(270532608);
            intent.addFlags(65536);
            try {
                AllAppsGridAdapter.this.mLauncher.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(AllAppsGridAdapter.TAG, "Unable to launch. tag= intent=" + intent, e);
            } catch (SecurityException e2) {
                Log.e(AllAppsGridAdapter.TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + " intent=" + intent, e2);
            }
        }
    };
    private GridLayoutManager mGridLayoutMgr;
    private GridSpanSizer mGridSizer;
    private OnClickListener mIconClickListener;
    private int mIconSize;
    private GridItemDecoration mItemDecoration;
    private Launcher mLauncher;
    private LayoutInflater mLayoutInflater;
    private String mMarketAppName;
    private Intent mMarketSearchIntent;
    private String mMarketSearchMessage;
    OnClickListener mPlayStoreBtnClickListener = new OnClickListener() {
        public void onClick(View v) {
            String title = AllAppsGridAdapter.this.mSearchText.toString();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("market://search?q=" + title));
            intent.addFlags(270532608);
            intent.addFlags(65536);
            try {
                AllAppsGridAdapter.this.mLauncher.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(AllAppsGridAdapter.TAG, "Unable to launch. tag= intent=" + intent, e);
            } catch (SecurityException e2) {
                Log.e(AllAppsGridAdapter.TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + " intent=" + intent, e2);
            }
        }
    };
    private Paint mPredictedAppsDividerPaint;
    private int mPredictionBarDividerOffset;
    String mSearchText;
    private int mSectionHeaderOffset;
    private int mSectionNamesMargin;
    private Paint mSectionTextPaint;
    private OnTouchListener mTouchListener;

    public class GridItemDecoration extends ItemDecoration {
        private static final boolean DEBUG_SECTION_MARGIN = false;
        private static final boolean FADE_OUT_SECTIONS = false;
        private HashMap<String, PointF> mCachedSectionBounds = new HashMap();
        private Rect mTmpBounds = new Rect();

        public void onDraw(Canvas c, RecyclerView parent, State state) {
            if (!AllAppsGridAdapter.this.mApps.hasFilter() && AllAppsGridAdapter.this.mAppsPerRow != 0) {
                List<AdapterItem> items = AllAppsGridAdapter.this.mApps.getAdapterItems();
                boolean hasDrawnPredictedAppsDivider = false;
                boolean showSectionNames = AllAppsGridAdapter.this.mSectionNamesMargin > 0;
                int childCount = parent.getChildCount();
                int lastSectionTop = 0;
                int lastSectionHeight = 0;
                int i = 0;
                while (i < childCount) {
                    View child = parent.getChildAt(i);
                    ViewHolder holder = (ViewHolder) parent.getChildViewHolder(child);
                    if (isValidHolderAndChild(holder, child, items)) {
                        if (shouldDrawItemDivider(holder, items) && !hasDrawnPredictedAppsDivider) {
                            int top = (child.getTop() + child.getHeight()) + AllAppsGridAdapter.this.mPredictionBarDividerOffset;
                            c.drawLine((float) AllAppsGridAdapter.this.mBackgroundPadding.left, (float) top, (float) (parent.getWidth() - AllAppsGridAdapter.this.mBackgroundPadding.right), (float) top, AllAppsGridAdapter.this.mPredictedAppsDividerPaint);
                            hasDrawnPredictedAppsDivider = true;
                        } else if (showSectionNames && shouldDrawItemSection(holder, i, items)) {
                            int viewTopOffset = child.getPaddingTop() * 2;
                            int pos = holder.getPosition();
                            AdapterItem item = (AdapterItem) items.get(pos);
                            SectionInfo sectionInfo = item.sectionInfo;
                            String lastSectionName = item.sectionName;
                            int j = item.sectionAppIndex;
                            while (j < sectionInfo.numApps) {
                                AdapterItem nextItem = (AdapterItem) items.get(pos);
                                String sectionName = nextItem.sectionName;
                                if (nextItem.sectionInfo != sectionInfo) {
                                    break;
                                }
                                if (j <= item.sectionAppIndex || !sectionName.equals(lastSectionName)) {
                                    int x;
                                    PointF sectionBounds = getAndCacheSectionBounds(sectionName);
                                    int sectionBaseline = (int) (((float) viewTopOffset) + sectionBounds.y);
                                    if (Utilities.sIsRtl) {
                                        x = (parent.getWidth() - AllAppsGridAdapter.this.mBackgroundPadding.left) - AllAppsGridAdapter.this.mSectionNamesMargin;
                                    } else {
                                        x = AllAppsGridAdapter.this.mBackgroundPadding.left;
                                    }
                                    x += (int) ((((float) AllAppsGridAdapter.this.mSectionNamesMargin) - sectionBounds.x) / 2.0f);
                                    int y = child.getTop() + sectionBaseline;
                                    if (!(!sectionName.equals(((AdapterItem) items.get(Math.min(items.size() + -1, (AllAppsGridAdapter.this.mAppsPerRow + pos) - (((AdapterItem) items.get(pos)).sectionAppIndex % AllAppsGridAdapter.this.mAppsPerRow)))).sectionName))) {
                                        y = Math.max(sectionBaseline, y);
                                    }
                                    if (lastSectionHeight > 0 && y <= lastSectionTop + lastSectionHeight) {
                                        y += (lastSectionTop - y) + lastSectionHeight;
                                    }
                                    c.drawText(sectionName, (float) x, (float) y, AllAppsGridAdapter.this.mSectionTextPaint);
                                    lastSectionTop = y;
                                    lastSectionHeight = (int) (sectionBounds.y + ((float) AllAppsGridAdapter.this.mSectionHeaderOffset));
                                    lastSectionName = sectionName;
                                }
                                j++;
                                pos++;
                            }
                            i += sectionInfo.numApps - item.sectionAppIndex;
                        }
                    }
                    i++;
                }
            }
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
        }

        private PointF getAndCacheSectionBounds(String sectionName) {
            PointF bounds = (PointF) this.mCachedSectionBounds.get(sectionName);
            if (bounds != null) {
                return bounds;
            }
            AllAppsGridAdapter.this.mSectionTextPaint.getTextBounds(sectionName, 0, sectionName.length(), this.mTmpBounds);
            bounds = new PointF(AllAppsGridAdapter.this.mSectionTextPaint.measureText(sectionName), (float) this.mTmpBounds.height());
            this.mCachedSectionBounds.put(sectionName, bounds);
            return bounds;
        }

        private boolean isValidHolderAndChild(ViewHolder holder, View child, List<AdapterItem> items) {
            if (((LayoutParams) child.getLayoutParams()).isItemRemoved() || holder == null) {
                return false;
            }
            int pos = holder.getPosition();
            if (pos < 0 || pos >= items.size()) {
                return false;
            }
            return true;
        }

        private boolean shouldDrawItemDivider(ViewHolder holder, List<AdapterItem> items) {
            return ((AdapterItem) items.get(holder.getPosition())).viewType == 2;
        }

        private boolean shouldDrawItemSection(ViewHolder holder, int childIndex, List<AdapterItem> items) {
            int pos = holder.getPosition();
            AdapterItem item = (AdapterItem) items.get(pos);
            if (item.viewType != 1 && item.viewType != 7) {
                return false;
            }
            boolean z = childIndex == 0 || ((AdapterItem) items.get(pos - 1)).viewType == 0;
            return z;
        }
    }

    public class GridSpanSizer extends SpanSizeLookup {
        public GridSpanSizer() {
            setSpanIndexCacheEnabled(true);
        }

        public int getSpanSize(int position) {
            switch (((AdapterItem) AllAppsGridAdapter.this.mApps.getAdapterItems().get(position)).viewType) {
                case 1:
                case 2:
                case 7:
                    return 1;
                default:
                    return AllAppsGridAdapter.this.mAppsPerRow;
            }
        }
    }

    public static class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder {
        public View mContent;

        public ViewHolder(View v) {
            super(v);
            this.mContent = v;
        }
    }

    public class AppsGridLayoutManager extends GridLayoutManager {
        public AppsGridLayoutManager(Context context) {
            super(context, 1, 1, false);
        }

        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            AccessibilityEventCompat.asRecord(event).setItemCount(AllAppsGridAdapter.this.mApps.getNumFilteredApps());
        }

        public int getRowCountForAccessibility(Recycler recycler, State state) {
            if (AllAppsGridAdapter.this.mApps.hasNoFilteredResults()) {
                return 0;
            }
            return super.getRowCountForAccessibility(recycler, state);
        }
    }

    public AllAppsGridAdapter(Launcher launcher, AlphabeticalAppsList apps, OnTouchListener touchListener, OnClickListener iconClickListener) {
        Resources res = launcher.getResources();
        this.mLauncher = launcher;
        this.mApps = apps;
        this.mEmptySearchMessage = res.getString(R.string.all_apps_loading_message);
        this.mGridSizer = new GridSpanSizer();
        this.mGridLayoutMgr = new AppsGridLayoutManager(launcher);
        this.mGridLayoutMgr.setSpanSizeLookup(this.mGridSizer);
        this.mItemDecoration = new GridItemDecoration();
        this.mLayoutInflater = LayoutInflater.from(launcher);
        this.mTouchListener = touchListener;
        this.mIconClickListener = iconClickListener;
        this.mSectionNamesMargin = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        this.mSectionHeaderOffset = res.getDimensionPixelSize(R.dimen.all_apps_grid_section_y_offset);
        this.mSectionTextPaint = new Paint();
        this.mSectionTextPaint.setTextSize((float) res.getDimensionPixelSize(R.dimen.all_apps_grid_section_text_size));
        this.mSectionTextPaint.setColor(res.getColor(R.color.all_apps_grid_section_text_color));
        this.mSectionTextPaint.setAntiAlias(true);
        this.mPredictedAppsDividerPaint = new Paint();
        this.mPredictedAppsDividerPaint.setStrokeWidth((float) Utilities.pxFromDp(1.0f, res.getDisplayMetrics()));
        this.mPredictedAppsDividerPaint.setColor(503316480);
        this.mPredictedAppsDividerPaint.setAntiAlias(true);
        this.mPredictionBarDividerOffset = ((-res.getDimensionPixelSize(R.dimen.all_apps_prediction_icon_bottom_padding)) + res.getDimensionPixelSize(R.dimen.all_apps_icon_top_bottom_padding)) / 2;
        PackageManager pm = launcher.getPackageManager();
        ResolveInfo marketInfo = pm.resolveActivity(createMarketSearchIntent(""), 65536);
        if (marketInfo != null) {
            this.mMarketAppName = marketInfo.loadLabel(pm).toString();
        }
        this.mSearchText = "";
    }

    public void setNumAppsPerRow(int appsPerRow) {
        this.mAppsPerRow = appsPerRow;
        this.mGridLayoutMgr.setSpanCount(appsPerRow);
    }

    public void setLastSearchQuery(String query) {
        Resources res = this.mLauncher.getResources();
        this.mEmptySearchMessage = res.getString(R.string.app_search_no_results_found);
        setSearchText(query);
        if (this.mMarketAppName != null) {
            this.mMarketSearchMessage = String.format(res.getString(R.string.all_apps_search_market_message), new Object[]{this.mMarketAppName});
            this.mMarketSearchIntent = createMarketSearchIntent(query);
        }
    }

    public void updateBackgroundPadding(Rect padding) {
        this.mBackgroundPadding.set(padding);
    }

    public GridLayoutManager getLayoutManager() {
        return this.mGridLayoutMgr;
    }

    public ItemDecoration getItemDecoration() {
        return this.mItemDecoration;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        IconView icon;
        View MarketBtnView;
        switch (viewType) {
            case 0:
                return new ViewHolder(new View(parent.getContext()));
            case 1:
                icon = (IconView) this.mLayoutInflater.inflate(R.layout.all_apps_icon, parent, false);
                icon.setOnTouchListener(this.mTouchListener);
                icon.setOnClickListener(this.mIconClickListener);
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon.setFocusable(true);
                this.mIconSize = icon.getIconSize();
                return new ViewHolder(icon);
            case 2:
                icon = (IconView) this.mLayoutInflater.inflate(R.layout.all_apps_icon, parent, false);
                icon.setOnTouchListener(this.mTouchListener);
                icon.setOnClickListener(this.mIconClickListener);
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon.setFocusable(true);
                return new ViewHolder(icon);
            case 3:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_empty_search, parent, false));
            case 4:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_search_market_divider, parent, false));
            case 5:
                View searchMarketView = this.mLayoutInflater.inflate(R.layout.all_apps_search_market, parent, false);
                searchMarketView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        AllAppsGridAdapter.this.mLauncher.startSearchFromAllApps(v, AllAppsGridAdapter.this.mMarketSearchIntent);
                    }
                });
                return new ViewHolder(searchMarketView);
            case 6:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_no_recent_history, parent, false));
            case 7:
                icon = (IconView) this.mLayoutInflater.inflate(R.layout.all_apps_icon, parent, false);
                icon.setOnTouchListener(this.mTouchListener);
                icon.setOnClickListener(this.mIconClickListener);
                icon.setLongPressTimeout(ViewConfiguration.getLongPressTimeout());
                icon.setFocusable(true);
                return new ViewHolder(icon);
            case 8:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.apps_galaxy_search_title, parent, false));
            case 9:
                MarketBtnView = this.mLayoutInflater.inflate(R.layout.apps_galaxy_search_button, parent, false);
                MarketBtnView.setOnClickListener(this.mGalaxyAppsBtnClickListener);
                return new ViewHolder(MarketBtnView);
            case 10:
                MarketBtnView = this.mLayoutInflater.inflate(R.layout.apps_galaxy_search_button, parent, false);
                MarketBtnView.setOnClickListener(this.mPlayStoreBtnClickListener);
                return new ViewHolder(MarketBtnView);
            case 11:
                return new ViewHolder(this.mLayoutInflater.inflate(R.layout.all_apps_list_header, parent, false));
            case 12:
                View resultView = this.mLayoutInflater.inflate(R.layout.item_details_view, parent, false);
                ((ImageView) resultView.findViewById(R.id.photo)).setOnClickListener(this.mGalaxyAppsIconClickListener);
                return new ViewHolder(resultView);
            default:
                throw new RuntimeException("Unexpected view type");
        }
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 1:
                IconInfo info = ((AdapterItem) this.mApps.getAdapterItems().get(position)).iconInfo;
                IconView icon = holder.mContent;
                icon.applyFromApplicationInfo(info);
                if (!this.mSearchText.isEmpty()) {
                    displayHighlightedName(icon, info.title.toString());
                    return;
                }
                return;
            case 2:
                ((IconView) holder.mContent).applyFromApplicationInfo(((AdapterItem) this.mApps.getAdapterItems().get(position)).iconInfo);
                return;
            case 3:
                TextView emptyViewText = holder.mContent;
                emptyViewText.setText(this.mEmptySearchMessage);
                emptyViewText.setGravity(this.mApps.hasNoFilteredResults() ? 17 : 8388627);
                return;
            case 5:
                TextView searchView = (TextView) holder.mContent;
                if (this.mMarketSearchIntent != null) {
                    searchView.setVisibility(View.VISIBLE);
                    searchView.setContentDescription(this.mMarketSearchMessage);
                    searchView.setGravity(this.mApps.hasNoFilteredResults() ? 17 : 8388627);
                    searchView.setText(this.mMarketSearchMessage);
                    return;
                }
                searchView.setVisibility(View.GONE);
                return;
            case 6:
                TextView notiText = holder.mContent;
                if (this.mApps.size() > 0) {
                    notiText.setVisibility(View.VISIBLE);
                    return;
                }
                return;
            case 7:
                ((IconView) holder.mContent).applyFromApplicationInfo(((AdapterItem) this.mApps.getAdapterItems().get(position)).iconInfo);
                return;
            case 9:
                ((TextView) holder.mContent.findViewById(R.id.viewmore_galaxyapps_text)).setText(this.mLauncher.getResources().getString(R.string.viewmore_galaxyapps).toUpperCase());
                return;
            case 10:
                ((TextView) holder.mContent.findViewById(R.id.viewmore_galaxyapps_text)).setText(this.mLauncher.getResources().getString(R.string.viewmore_playstore).toUpperCase());
                return;
            case 11:
                ((TextView) holder.mContent.findViewById(R.id.header_text)).setText(((AdapterItem) this.mApps.getAdapterItems().get(position)).sectionName);
                return;
            case 12:
                ItemDetails itemDetails = ((AdapterItem) this.mApps.getAdapterItems().get(position)).itemDetails;
                View resultView = holder.mContent;
                String price = this.mLauncher.getResources().getString(R.string.free);
                ImageView ItemImage = (ImageView) resultView.findViewById(R.id.photo);
                ItemImage.setTag(itemDetails);
                TextView ItemName = (TextView) resultView.findViewById(R.id.name);
                TextView ItemSeller = (TextView) resultView.findViewById(R.id.seller);
                TextView ItemPrice = (TextView) resultView.findViewById(R.id.price);
                TextView Itemrating = (TextView) resultView.findViewById(R.id.rating);
                if (this.mIconSize > 0) {
                    ItemImage.setImageBitmap(Bitmap.createScaledBitmap(itemDetails.getIconImage(), this.mIconSize, this.mIconSize, true));
                    ItemName.setText(itemDetails.getName());
                    ItemSeller.setText(itemDetails.getItemSeller());
                    if (price.equals(itemDetails.getPrice())) {
                        ItemPrice.setText("Free");
                    } else {
                        ItemPrice.setText(itemDetails.getPrice());
                    }
                    Itemrating.setText(itemDetails.getRating());
                    return;
                }
                return;
            default:
                return;
        }
    }

    public int getItemCount() {
        return this.mApps.getAdapterItems().size();
    }

    public int getItemViewType(int position) {
        return ((AdapterItem) this.mApps.getAdapterItems().get(position)).viewType;
    }

    private Intent createMarketSearchIntent(String query) {
        Uri marketSearchUri = Uri.parse("market://search").buildUpon().appendQueryParameter("q", query).build();
        Intent marketSearchIntent = new Intent("android.intent.action.VIEW");
        marketSearchIntent.setData(marketSearchUri);
        return marketSearchIntent;
    }

    public void setSearchText(String search) {
        this.mSearchText = search;
    }

    private void displayHighlightedName(IconView view, String appName) {
        int indexOf;
        int highlightStrLength = this.mSearchText.length();
        Spannable highLightText = new SpannableString(appName);
        char[] iQueryForIndian = TextUtils.semGetPrefixCharForSpan(view.getPaint(), appName, this.mSearchText.toCharArray());
        if (iQueryForIndian != null) {
            String s = new String(iQueryForIndian);
            indexOf = appName.toLowerCase().indexOf(s.toLowerCase());
            highlightStrLength = s.length();
        } else {
            indexOf = appName.toLowerCase().indexOf(this.mSearchText.toLowerCase());
        }
        if (indexOf <= indexOf + highlightStrLength && indexOf > -1) {
            highLightText.setSpan(new ForegroundColorSpan(this.mLauncher.getResources().getColor(R.color.apps_search_app_name_highlight_color)), indexOf, indexOf + highlightStrLength, 0);
            view.setText(highLightText);
        }
    }
}
