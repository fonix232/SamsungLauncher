package com.android.launcher3.appspicker.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.locale.LocaleUtils;
import com.samsung.android.widget.SemArrayIndexer;
import com.samsung.android.widget.SemIndexScrollView;
import com.samsung.android.widget.SemIndexScrollView.OnIndexBarEventListener;
import com.sec.android.app.launcher.R;
import java.text.Collator;
import java.util.List;

public class AppsPickerContentView extends FrameLayout {
    private static final String TAG = AppsPickerContentView.class.getSimpleName();
    private ListView mAllListView;
    private ViewGroup mAllListViewContainer;
    private int mBgColor;
    private final Collator mCollator;
    private TextView mDefaultSearchViewText;
    private TextView mEmptyView;
    private View mHeader;
    private int[] mIndexCharactersPosition;
    private boolean mIsAppsPickerViewTop;
    private boolean mIsParentHome;
    private boolean mNeedToReset;
    private View mSearchHeader;
    private ListView mSearchListView;
    private ViewGroup mSearchListViewContainer;
    private TextView mSearchResultText;
    private SemIndexScrollView mTwIndexScrollView;

    public AppsPickerContentView(Context context) {
        this(context, null);
    }

    public AppsPickerContentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsPickerContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCollator = Collator.getInstance();
        this.mIsParentHome = false;
        this.mIsAppsPickerViewTop = false;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAllListViewContainer = (ViewGroup) findViewById(R.id.apps_picker_all_list);
        initAllListView(this.mAllListViewContainer);
        this.mSearchListViewContainer = (ViewGroup) findViewById(R.id.apps_picker_search_list);
        initSearchListView(this.mSearchListViewContainer);
    }

    private void initAllListView(ViewGroup container) {
        this.mAllListView = (ListView) container.findViewById(R.id.apps_picker_all_list_view);
        this.mAllListView.setFocusable(false);
        if (!TestHelper.isRoboUnitTest()) {
            setupIndexScrollView(false);
        }
    }

    private void initSearchListView(ViewGroup container) {
        this.mSearchHeader = container.findViewById(R.id.header);
        setSearchHeaderPadding();
        TextView searchHeaderText1 = (TextView) container.findViewById(R.id.header_text1);
        searchHeaderText1.setVisibility(View.VISIBLE);
        searchHeaderText1.setText(R.string.appsPicker_apps);
        this.mDefaultSearchViewText = searchHeaderText1;
        this.mHeader = container.findViewById(R.id.header_line);
        this.mEmptyView = (TextView) container.findViewById(R.id.no_result_text);
        TextView searchHeaderText2 = (TextView) container.findViewById(R.id.header_text2);
        searchHeaderText2.setVisibility(View.VISIBLE);
        this.mSearchResultText = searchHeaderText2;
        this.mSearchListView = (ListView) container.findViewById(R.id.apps_picker_search_list_view);
        this.mSearchListView.setEmptyView(container.findViewById(R.id.no_result_text_layout));
    }

    private void setupIndexScrollView(boolean isWhiteBg) {
        Resources res = getContext().getResources();
        if (this.mTwIndexScrollView == null && !TestHelper.isRoboUnitTest()) {
            this.mTwIndexScrollView = new SemIndexScrollView(getContext());
            this.mAllListViewContainer.addView(this.mTwIndexScrollView);
        }
        if (res != null) {
            Drawable d;
            if (isWhiteBg) {
                d = res.getDrawable(R.drawable.fluid_index_scroll_background, getContext().getTheme());
            } else {
                d = res.getDrawable(R.drawable.fluid_index_scroll_whitebg_background, getContext().getTheme());
            }
            this.mTwIndexScrollView.setIndexBarBackgroundDrawable(d);
        }
        if (this.mTwIndexScrollView != null) {
            this.mTwIndexScrollView.setIndexBarPressedTextColor(this.mBgColor);
            this.mTwIndexScrollView.setIndexBarTextColor(this.mBgColor);
            if (Utilities.sIsRtl) {
                this.mTwIndexScrollView.setIndexBarGravity(0);
            } else {
                this.mTwIndexScrollView.setIndexBarGravity(1);
            }
            this.mTwIndexScrollView.setOnIndexBarEventListener(new OnIndexBarEventListener() {
                public void onPressed(float sectionIndex) {
                }

                public void onReleased(float sectionIndex) {
                }

                public void onIndexChanged(int sectionIndex) {
                    if (AppsPickerContentView.this.mAllListView == null) {
                        return;
                    }
                    if (!LocaleUtils.isChineseHK() || AppsPickerContentView.this.mIndexCharactersPosition == null || sectionIndex >= AppsPickerContentView.this.mIndexCharactersPosition.length) {
                        AppsPickerContentView.this.mAllListView.setSelection(sectionIndex);
                        AppsPickerContentView.this.mAllListView.smoothScrollToPosition(sectionIndex);
                        return;
                    }
                    AppsPickerContentView.this.mAllListView.setSelection(AppsPickerContentView.this.mIndexCharactersPosition[sectionIndex]);
                    AppsPickerContentView.this.mAllListView.smoothScrollToPosition(AppsPickerContentView.this.mIndexCharactersPosition[sectionIndex]);
                }
            });
        }
    }

    public ListView getListViewForAllApps() {
        return this.mAllListView;
    }

    public ListView getListViewForSearchApps() {
        return this.mSearchListView;
    }

    void setParentMode(boolean home) {
        this.mIsParentHome = home;
    }

    void setAppsPickerViewTop(boolean appsPickerViewTop) {
        this.mIsAppsPickerViewTop = appsPickerViewTop;
    }

    public void showAllListView() {
        Log.v(TAG, "showAllListView()");
        this.mAllListViewContainer.setVisibility(View.VISIBLE);
        this.mSearchListViewContainer.setVisibility(View.GONE);
        if (!this.mIsAppsPickerViewTop) {
            return;
        }
        if (this.mIsParentHome) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(13);
        } else {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(14);
        }
    }

    public void showSearchListView() {
        Log.v(TAG, "showSearchListView()");
        this.mSearchListViewContainer.setVisibility(View.VISIBLE);
        this.mAllListViewContainer.setVisibility(View.GONE);
        if (!this.mIsAppsPickerViewTop) {
            return;
        }
        if (this.mIsParentHome) {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(18);
        } else {
            LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(19);
        }
    }

    public void setScrollIndexer(List<String> listData, CharSequence headerIndex) {
        if (this.mTwIndexScrollView != null) {
            Point screenSize = new Point();
            Utilities.getScreenSize(getContext(), screenSize);
            int indexBottomMargin = 0;
            int calculatedIndexInterval = screenSize.y / headerIndex.length();
            int default_interval = (int) getResources().getDimension(R.dimen.appsPicker_index_scroll_default_interval);
            if (calculatedIndexInterval > default_interval) {
                indexBottomMargin = (calculatedIndexInterval - default_interval) * (headerIndex.length() - 1);
            }
            this.mTwIndexScrollView.setIndexScrollMargin(0, indexBottomMargin);
            if (LocaleUtils.isChineseHK()) {
                String[] indexCharacters = getResources().getStringArray(R.array.index_string_favorite_array_stroke);
                indexCharacters[0] = "&";
                this.mIndexCharactersPosition = new int[indexCharacters.length];
                for (int i = 1; i < indexCharacters.length; i++) {
                    int j = 0;
                    while (j < listData.size()) {
                        int cmpRes;
                        if (this.mCollator.compare(indexCharacters[i], "a") >= 0 || this.mCollator.compare((String) listData.get(j), "a") >= 0) {
                            cmpRes = this.mCollator.compare((String) listData.get(j), indexCharacters[i]);
                        } else {
                            try {
                                cmpRes = Integer.parseInt((String) listData.get(j)) - Integer.parseInt(indexCharacters[i]);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "NumberFormatException : " + indexCharacters[i] + " - " + ((String) listData.get(j)));
                                cmpRes = this.mCollator.compare((String) listData.get(j), indexCharacters[i]);
                            }
                        }
                        if (cmpRes >= 0 || (cmpRes < 0 && j == listData.size() - 1)) {
                            this.mIndexCharactersPosition[i] = j;
                            break;
                        }
                        j++;
                    }
                }
                try {
                    this.mTwIndexScrollView.setSimpleIndexScroll(indexCharacters, (int) getResources().getDimension(R.dimen.tw_indexview_first_handle_width));
                } catch (IllegalStateException e2) {
                    Log.e(TAG, "IllegalStateException.", e2);
                }
            } else {
                this.mTwIndexScrollView.setIndexer(new SemArrayIndexer(listData, headerIndex));
            }
            this.mTwIndexScrollView.requestLayout();
        }
    }

    public void setSearchResultText(int foundNum) {
        if (this.mSearchResultText == null) {
            return;
        }
        if (foundNum >= 0) {
            if (this.mNeedToReset) {
                this.mNeedToReset = false;
                this.mDefaultSearchViewText.setText(R.string.appsPicker_apps);
                this.mEmptyView.setText(R.string.app_search_no_results_found);
                ((LayoutParams) this.mEmptyView.getLayoutParams()).gravity = 17;
            }
            String found = String.format(getResources().getString(R.string.appsPicker_search_result_found), new Object[]{Integer.valueOf(foundNum)});
            this.mSearchResultText.setText(found);
            this.mSearchResultText.setTextColor(this.mBgColor);
            this.mDefaultSearchViewText.setTextColor(this.mBgColor);
            this.mHeader.setBackgroundColor(this.mBgColor);
            this.mEmptyView.setTextColor(this.mBgColor);
            this.mSearchResultText.setContentDescription(getResources().getString(R.string.appsPicker_apps) + found);
            return;
        }
        this.mDefaultSearchViewText.setText(getContext().getClass().getSimpleName());
        this.mSearchResultText.setText(AppsPickerMsgHelper.getKey());
        this.mEmptyView.setText(AppsPickerMsgHelper.getBody());
        ((LayoutParams) this.mEmptyView.getLayoutParams()).gravity = 49;
        this.mNeedToReset = true;
    }

    public void setContentBgColor(int color, boolean isWhiteBg) {
        this.mBgColor = color;
        if (this.mTwIndexScrollView != null) {
            setupIndexScrollView(isWhiteBg);
        }
    }

    public void notifyLayoutChanged() {
        boolean isLandscape;
        int i = 0;
        if (Utilities.getOrientation() == 2) {
            isLandscape = true;
        } else {
            isLandscape = false;
        }
        MarginLayoutParams lp = (MarginLayoutParams) this.mAllListViewContainer.getLayoutParams();
        if (isLandscape) {
            i = (int) getResources().getDimension(R.dimen.appsPicker_index_scroller_margin_end);
        }
        lp.setMarginEnd(i);
        this.mAllListViewContainer.setLayoutParams(lp);
        setSearchHeaderPadding();
    }

    private void setSearchHeaderPadding() {
        if (this.mSearchHeader != null) {
            this.mSearchHeader.setPadding(getResources().getDimensionPixelSize(R.dimen.appsPicker_search_header_margin_start), this.mSearchHeader.getPaddingTop(), getResources().getDimensionPixelSize(R.dimen.appsPicker_header_margin_end), this.mSearchHeader.getPaddingBottom());
        }
    }
}
