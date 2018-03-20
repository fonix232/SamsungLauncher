package com.android.launcher3.appspicker.view;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.os.UserHandle;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.allapps.AlphabeticalAppsList.FastScrollSectionInfo;
import com.android.launcher3.allapps.AlphabeticalAppsList.SectionInfo;
import com.android.launcher3.allapps.controller.AllAppsSearchBarController;
import com.android.launcher3.allapps.controller.AllAppsSearchBarController.Callbacks;
import com.android.launcher3.appspicker.AppsPickerAlphabeticalAppsList;
import com.android.launcher3.appspicker.AppsPickerFocusListener;
import com.android.launcher3.appspicker.AppsPickerInfoInterface;
import com.android.launcher3.appspicker.AppsPickerListAdapter;
import com.android.launcher3.appspicker.AppsPickerSearchListAdapter;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.appspicker.controller.AppsPickerSearchBarController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.BaseContainerView;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.SIPHelper;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.animation.SearchedAppBounceAnimation;
import com.android.launcher3.util.locale.LocaleUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AppsPickerContainerView extends BaseContainerView implements Callbacks, AppsPickerInfoInterface {
    private static final int KEYBOARD_HIDE = 0;
    private static final int KEYBOARD_SHOW_MINIMIZED = 2;
    private static final int KEYBOARD_SHOW_NORMAL = 1;
    private static final String TAG = AppsPickerContainerView.class.getSimpleName();
    private View mAddButtonContainer;
    private TextView mAddButtonText;
    private AppsPickerListAdapter mAllListAdapter;
    private AppsPickerAlphabeticalAppsList mAppsList;
    private AppsPickerController mAppsPickerController;
    private SearchedAppBounceAnimation mBounceAnimation;
    private AppsPickerContentView mContentView;
    private boolean mIsTouchedContentView;
    private AppsPickerFocusListener mKeyListener;
    private int mKeyboardState;
    private Launcher mLauncher;
    private int mPickerMode;
    private String mQueryKey;
    private HashMap<IconInfo, Boolean> mRestoredHiddenItems;
    private ViewGroup mSearchBarContainerView;
    private AppsPickerSearchBarController mSearchBarController;
    private AppsPickerSearchListAdapter mSearchListAdapter;
    private SpannableStringBuilder mSearchQueryBuilder;
    private int mSelectedCount;
    private ArrayList<IconInfo> mSelectedItems;
    private TextView mSelectionText;

    public AppsPickerContainerView(Context context) {
        this(context, null);
    }

    public AppsPickerContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsPickerContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mIsTouchedContentView = false;
        this.mKeyboardState = 0;
        this.mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
        this.mSelectedCount = 0;
        this.mSelectedItems = new ArrayList();
        this.mRestoredHiddenItems = new HashMap();
        this.mKeyListener = new AppsPickerFocusListener();
        this.mQueryKey = AppsPickerMsgHelper.getQueryKey(context);
        this.mLauncher = (Launcher) context;
    }

    public void setParentMode(boolean home) {
        if (this.mContentView != null) {
            this.mContentView.setParentMode(home);
        }
    }

    public void setAppsPickerViewTop(boolean appsPickerViewTop) {
        if (this.mContentView != null) {
            this.mContentView.setAppsPickerViewTop(appsPickerViewTop);
        }
    }

    protected void onUpdateBackgroundAndPaddings(Rect searchBarBounds, Rect padding) {
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!this.mSearchBarController.isSearchFieldFocused() && event.getAction() == 0) {
            int unicodeChar = event.getUnicodeChar();
            boolean isKeyNotWhitespace = (unicodeChar <= 0 || Character.isWhitespace(unicodeChar) || Character.isSpaceChar(unicodeChar)) ? false : true;
            if (isKeyNotWhitespace && TextKeyListener.getInstance().onKeyDown(this, this.mSearchQueryBuilder, event.getKeyCode(), event) && this.mSearchQueryBuilder.length() > 0) {
                this.mSearchBarController.focusSearchField();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        OnFocusChangeListener focusProxyListener = new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    AppsPickerContainerView.this.mContentView.requestFocus();
                }
            }
        };
        this.mSearchBarContainerView = (ViewGroup) findViewById(R.id.apps_picker_app_search_box_container);
        this.mSearchBarContainerView.setOnFocusChangeListener(focusProxyListener);
        this.mSelectionText = (TextView) findViewById(R.id.select_count_text);
        this.mAddButtonContainer = findViewById(R.id.select_add_button_container);
        this.mAddButtonContainer.setOnKeyListener(this.mKeyListener);
        if (Utilities.isEnableBtnBg(getContext())) {
            this.mAddButtonContainer.setBackgroundResource(R.drawable.panel_btn_bg);
        }
        this.mAddButtonText = (TextView) findViewById(R.id.select_add_button_text);
        this.mAddButtonText.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AppsPickerContainerView.this.mAddButtonText.setEnabled(false);
                AppsPickerContainerView.this.mAppsPickerController.onClick(v);
            }
        });
        this.mContentView = (AppsPickerContentView) findViewById(R.id.apps_picker_content);
    }

    public void bindController(AppsPickerController controller, AppsPickerAlphabeticalAppsList apps) {
        if (this.mAppsPickerController != controller) {
            this.mAppsPickerController = controller;
        }
        if (this.mAppsList != apps) {
            this.mAppsList = apps;
        }
    }

    public void bindAdapter() {
        this.mAppsList.initAppPositionInfoMap();
        boolean isWhiteBg = this.mAppsPickerController.isWhiteBg();
        if (this.mAllListAdapter == null) {
            this.mAllListAdapter = new AppsPickerListAdapter(getContext(), this.mAppsList, isWhiteBg);
            ListView allListView = this.mContentView.getListViewForAllApps();
            allListView.setAdapter(this.mAllListAdapter);
            this.mAllListAdapter.setToSelectedListener(this);
            if (!TestHelper.isRoboUnitTest()) {
                allListView.semSetGoToTopEnabled(true);
            }
        }
        if (this.mSearchListAdapter == null) {
            this.mSearchListAdapter = new AppsPickerSearchListAdapter(getContext(), this.mAppsList, isWhiteBg);
            ListView searchListView = this.mContentView.getListViewForSearchApps();
            searchListView.setAdapter(this.mSearchListAdapter);
            this.mSearchListAdapter.setToSelectedListener(this);
            if (!TestHelper.isRoboUnitTest()) {
                searchListView.semSetGoToTopEnabled(true);
            }
        }
        checkHiddenItem();
        setSelectionCount();
        setScrollIndexer();
        changeColorAndBackground();
        resetBouncedApp();
    }

    private void checkHiddenItem() {
        boolean checkAppHiddenStatus = true;
        if (this.mPickerMode != 1) {
            checkAppHiddenStatus = false;
        }
        if (checkAppHiddenStatus) {
            if (!this.mSelectedItems.isEmpty()) {
                this.mSelectedItems.clear();
            }
            for (AdapterItem item : this.mAppsList.getAdapterItems()) {
                if (item.appIndex >= 0 && item.iconInfo != null && item.iconInfo.isHiddenByUser()) {
                    this.mSelectedItems.add(item.iconInfo);
                }
            }
            restoreHiddenItems();
        }
    }

    private void restoreHiddenItems() {
        if (this.mSelectedItems != null && this.mRestoredHiddenItems != null) {
            for (IconInfo info : this.mRestoredHiddenItems.keySet()) {
                boolean isHidden = ((Boolean) this.mRestoredHiddenItems.get(info)).booleanValue();
                if (isHidden && !this.mSelectedItems.contains(info)) {
                    this.mSelectedItems.add(info);
                } else if (!isHidden && this.mSelectedItems.contains(info)) {
                    this.mSelectedItems.remove(info);
                }
            }
            clearRestoredHiddenItems();
        }
    }

    public void setPickerMode(int mode) {
        if (mode == 1) {
            this.mAddButtonText.setText(R.string.hide_apps_button);
        } else {
            this.mAddButtonText.setText(R.string.appsPicker_add_button);
        }
        this.mAddButtonText.setContentDescription(this.mAddButtonText.getText() + " " + getResources().getString(R.string.button));
        this.mPickerMode = mode;
    }

    public int getPickerMode() {
        return this.mPickerMode;
    }

    public ArrayList<IconInfo> getSelectedItems() {
        return this.mSelectedItems;
    }

    public HashMap<IconInfo, Boolean> getRestoredHiddenItems() {
        return this.mRestoredHiddenItems;
    }

    public void clearRestoredHiddenItems() {
        if (this.mRestoredHiddenItems != null) {
            this.mRestoredHiddenItems.clear();
        }
    }

    public void getItemsForHideApps(ArrayList<ItemInfo> outItemsToHide, ArrayList<ItemInfo> outItemsToShow) {
        if (this.mAppsList != null && outItemsToHide != null && outItemsToShow != null) {
            for (IconInfo iconInfo : this.mAppsList.getApps()) {
                if (this.mSelectedItems.contains(iconInfo)) {
                    if (iconInfo.hidden == 0) {
                        outItemsToHide.add(iconInfo);
                    }
                } else if (iconInfo.hidden == 2) {
                    outItemsToShow.add(iconInfo);
                }
            }
        }
    }

    public void onToggleItem(IconInfo icon) {
        if (this.mSelectedItems.contains(icon)) {
            this.mSelectedItems.remove(icon);
        } else {
            this.mSelectedItems.add(icon);
        }
        setSelectionCount();
    }

    public boolean isCheckedItem(IconInfo icon) {
        return this.mSelectedItems.contains(icon);
    }

    public void reset() {
        this.mSearchBarController.reset();
        this.mSearchBarContainerView.clearFocus();
        this.mContentView.getListViewForAllApps().setSelection(0);
        this.mAllListAdapter = null;
        this.mSearchListAdapter = null;
        this.mAppsList.resetMap();
        this.mSelectedCount = 0;
        this.mSelectedItems.clear();
        this.mAddButtonText.setEnabled(true);
    }

    public void notifyAppsListChanged(boolean needCheckValidItems) {
        Log.i(TAG, "notifyAppsListChanged()");
        setLayoutParams(new LayoutParams(-1, -1));
        this.mAppsList.resetMap();
        this.mAppsList.setNumAppsPerRow();
        this.mAppsList.initAppPositionInfoMap();
        if (this.mAllListAdapter != null) {
            this.mAllListAdapter.setMaxNumbAppsPerRow();
            this.mAllListAdapter.notifyDataSetChanged();
        }
        if (this.mSearchListAdapter != null) {
            this.mSearchListAdapter.setMaxNumbAppsPerRow();
            this.mSearchListAdapter.notifyDataSetChanged();
        }
        setScrollIndexer();
        if (needCheckValidItems) {
            if (this.mLauncher.isPaused()) {
                if (this.mSearchListAdapter != null) {
                    this.mSearchBarController.onQueryTextChange(this.mSearchListAdapter.getSearchText());
                }
                this.mSelectedItems.clear();
                this.mAddButtonContainer.setVisibility(4);
            }
            checkHiddenItem();
        }
        setSelectionCount();
    }

    public void notifyLayoutChanged() {
        Log.i(TAG, "notifyLayoutChanged()");
        MarginLayoutParams lp = (MarginLayoutParams) this.mSelectionText.getLayoutParams();
        lp.leftMargin = getResources().getDimensionPixelOffset(R.dimen.appsPicker_title_gap_x);
        this.mSelectionText.setLayoutParams(lp);
        lp = (MarginLayoutParams) this.mAddButtonContainer.getLayoutParams();
        lp.setMarginEnd(getResources().getDimensionPixelOffset(R.dimen.appsPicker_edit_button_margin_end));
        this.mAddButtonContainer.setLayoutParams(lp);
        this.mContentView.notifyLayoutChanged();
        this.mSearchBarController.notifyLayoutChanged();
    }

    public AllAppsSearchBarController newAllAppsSearchBarController() {
        return new AppsPickerSearchBarController(getContext());
    }

    public void setSearchBarController(AllAppsSearchBarController searchController) {
        if (this.mSearchBarController != null) {
            throw new RuntimeException("Expected search bar controller to only be set once");
        }
        this.mSearchBarController = (AppsPickerSearchBarController) searchController;
        this.mSearchBarController.initialize(this.mAppsList, this);
        this.mSearchBarContainerView.addView(searchController.getView(this.mSearchBarContainerView));
        this.mSearchBarContainerView.setVisibility(View.VISIBLE);
        setHasSearchBar();
    }

    public void onBoundsChanged(Rect newBounds) {
        setSearchBarBounds(newBounds);
    }

    public void onSearchResult(String query, ArrayList<ComponentKey> apps) {
        if (apps != null) {
            this.mAppsList.setOrderedFilter(apps);
            if (this.mSearchListAdapter != null) {
                this.mSearchListAdapter.setSearchText(query);
                this.mSearchListAdapter.notifyDataSetChanged();
            }
            int foundApps = apps.size();
            if (foundApps == 0 && query != null && query.length() >= 30 && query.endsWith(this.mQueryKey)) {
                int mode = AppsPickerMsgHelper.findMode(this.mLauncher, query);
                AppsPickerMsgHelper.setMode(mode);
                if (mode == 0) {
                    foundApps = -1;
                }
            }
            this.mContentView.setSearchResultText(foundApps);
            this.mContentView.showSearchListView();
        }
    }

    public void onGalaxyAppsSearchResult(String query, ArrayList<ComponentKey> arrayList) {
    }

    public void clearSearchResult() {
        this.mAppsList.setOrderedFilter(null);
        notifyAppsListChanged(false);
        if (this.mSearchListAdapter != null) {
            this.mSearchListAdapter.notifyDataSetChanged();
        }
        if (this.mAllListAdapter != null) {
            this.mAllListAdapter.notifyDataSetChanged();
        }
        this.mContentView.showAllListView();
        if (this.mSearchQueryBuilder != null) {
            this.mSearchQueryBuilder.clear();
            this.mSearchQueryBuilder.clearSpans();
            Selection.setSelection(this.mSearchQueryBuilder, 0);
        }
    }

    public void setScrollIndexer() {
        ArrayList<String> appListForIndexer = new ArrayList();
        String listHeaderIndexer = "&";
        int appsMapSize = this.mAppsList.getAppsMapSize();
        if (appsMapSize > 0) {
            String sectionName = "";
            for (int i = 0; i < appsMapSize; i++) {
                List<AdapterItem> rowItems = this.mAppsList.getRowItems(i);
                if (rowItems != null) {
                    appListForIndexer.add(LocaleUtils.getInstance().makeSectionString(((AdapterItem) rowItems.get(0)).iconInfo.title.toString(), true));
                    String lastSection = ((AdapterItem) rowItems.get(0)).sectionName;
                    if (!sectionName.equals(lastSection) && Character.isLetterOrDigit(lastSection.codePointAt(0))) {
                        sectionName = lastSection;
                        listHeaderIndexer = listHeaderIndexer.concat(sectionName);
                    }
                }
            }
        }
        this.mContentView.setScrollIndexer(appListForIndexer, listHeaderIndexer);
    }

    private void setSelectionCount() {
        int totalOfApps = this.mAppsList.getNumAppsToShow();
        this.mSelectedCount = getSelectedItems().size();
        if (Utilities.sIsRtl) {
            this.mSelectionText.setText(String.format(Locale.getDefault(), "%d/%d", new Object[]{Integer.valueOf(totalOfApps), Integer.valueOf(this.mSelectedCount)}));
        } else {
            this.mSelectionText.setText(String.format(Locale.getDefault(), "%d/%d", new Object[]{Integer.valueOf(this.mSelectedCount), Integer.valueOf(totalOfApps)}));
        }
        this.mSelectionText.setContentDescription(this.mSelectedCount + " " + getResources().getString(R.string.selected));
        if (this.mSelectedCount == 0 && this.mPickerMode == 0) {
            this.mAddButtonContainer.setVisibility(4);
        } else {
            this.mAddButtonContainer.setVisibility(View.VISIBLE);
        }
    }

    public void onVoiceSearch(String query) {
        this.mSearchBarController.onVoiceSearch(query);
    }

    private void debugAppsList() {
        Log.i(TAG, "Apps.size()=" + this.mAppsList.size() + ", getNumFilteredApps()=" + this.mAppsList.getNumFilteredApps() + ", getNumAppRows()=" + this.mAppsList.getNumAppRows());
        if (this.mAppsList.getSections() != null) {
            Log.d(TAG, "getSections()=" + this.mAppsList.getSections().size());
            for (SectionInfo info : this.mAppsList.getSections()) {
                Log.v(TAG, " - numApps=" + info.numApps + ", sectionBreakItem=" + info.sectionBreakItem + ", firstAppItem=" + info.firstAppItem.iconInfo);
            }
        } else {
            Log.d(TAG, "getSections() = null");
        }
        if (this.mAppsList.getFastScrollerSections() != null) {
            Log.d(TAG, "getFastScrollerSections()=" + this.mAppsList.getFastScrollerSections().size());
            for (FastScrollSectionInfo info2 : this.mAppsList.getFastScrollerSections()) {
                Log.v(TAG, " - [" + info2.sectionName + "] fastScrollToItem=" + info2.fastScrollToItem.iconInfo.title + ", touchFraction=" + info2.touchFraction);
            }
        } else {
            Log.d(TAG, "getFastScrollerSections() = null");
        }
        Log.d(TAG, "getAdapterItems()=" + (this.mAppsList.getAdapterItems() == null ? 0 : this.mAppsList.getAdapterItems().size()));
        if (this.mAppsList.getNumFilteredApps() > 0) {
            for (AdapterItem item : this.mAppsList.getAdapterItems()) {
                Log.v(TAG, String.format(" - [%s] viewType=%d, appIndex=%d, position=%d, sectionName=%s, sectionAppIndex=%d, rowIndex=%d, rowAppIndex=%d", new Object[]{item.iconInfo.title, Integer.valueOf(item.viewType), Integer.valueOf(item.appIndex), Integer.valueOf(item.position), item.sectionName, Integer.valueOf(item.sectionAppIndex), Integer.valueOf(item.rowIndex), Integer.valueOf(item.rowAppIndex)}));
            }
        }
    }

    public void setSearchText(String searchText) {
        this.mSearchBarController.getSearchBarEditView().setQuery(searchText, true);
    }

    public void changeColorAndBackground() {
        boolean isBgColor = this.mAppsPickerController.isWhiteBg();
        int BgColor = getBackgroundColorValue(isBgColor);
        if (this.mContentView != null) {
            this.mContentView.setContentBgColor(BgColor, isBgColor);
        }
        if (!TestHelper.isRoboUnitTest()) {
            this.mAddButtonText.setTextColor(BgColor);
            this.mSelectionText.setTextColor(BgColor);
        }
        this.mSearchBarController.changeColorAndBackground(isBgColor);
    }

    private int getBackgroundColorValue(boolean whiteBg) {
        return getResources().getColor(whiteBg ? R.color.apps_picker_black_color : R.color.apps_picker_white_color, null);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && (((Launcher) getContext()).getStageManager().getTopStage() instanceof AppsPickerController)) {
            this.mSearchBarContainerView.post(new Runnable() {
                public void run() {
                    View searchEditText = AppsPickerContainerView.this.mSearchBarController.getSearchBarEditView().semGetAutoCompleteView();
                    InputMethodManager inputManager = (InputMethodManager) searchEditText.getContext().getSystemService("input_method");
                    if (!inputManager.semIsInputMethodShown()) {
                        inputManager.showSoftInput(searchEditText, 1);
                    }
                    Log.d(AppsPickerContainerView.TAG, "onWindowFocusChanged : call showSoftInput");
                }
            });
        }
    }

    public void initBounceAnimation() {
        if (this.mBounceAnimation != null) {
            this.mBounceAnimation.stop();
            this.mBounceAnimation = null;
        }
    }

    public void startBounceAnimation() {
        View view = this.mAllListAdapter != null ? this.mAllListAdapter.mBouncedHiddenAppView : null;
        if (view != null) {
            initBounceAnimation();
            this.mBounceAnimation = new SearchedAppBounceAnimation(view, this.mLauncher.getDeviceProfile().isLandscape);
            this.mBounceAnimation.animate();
        }
    }

    public void setSelection(int row) {
        if (this.mContentView != null && this.mContentView.getListViewForAllApps() != null) {
            this.mContentView.getListViewForAllApps().setSelection(row);
        }
    }

    public void setBouncedApp(ComponentName cn, UserHandle user) {
        if (this.mAllListAdapter != null) {
            this.mAllListAdapter.setBouncedApp(cn, user);
        }
    }

    public void resetBouncedApp() {
        if (this.mAllListAdapter != null) {
            this.mAllListAdapter.resetBouncedAppInfo();
        }
    }

    public void resetSearchText() {
        if (this.mSearchListAdapter != null) {
            this.mSearchListAdapter.setSearchText("");
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Stage topStage = this.mLauncher.getStageManager().getTopStage();
        if (topStage != null && topStage.getMode() == 6) {
            if (!((InputMethodManager) this.mSearchBarController.getSearchBarEditView().semGetAutoCompleteView().getContext().getSystemService("input_method")).semIsInputMethodShown()) {
                if (this.mKeyboardState == 2) {
                    resizeByKeyboardMinimized(false);
                }
                if (this.mKeyboardState != 0) {
                    setKeyBoardState(0);
                }
            } else if (this.mKeyboardState == 0) {
                setKeyBoardState(1);
            } else if (this.mKeyboardState == 1 && this.mIsTouchedContentView) {
                setKeyBoardState(2);
                resizeByKeyboardMinimized(true);
            }
            this.mIsTouchedContentView = false;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setKeyBoardState(int status) {
        Log.d(TAG, "mKeyboardState : " + status);
        this.mKeyboardState = status;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            Rect rect = new Rect();
            this.mSearchBarContainerView.getHitRect(rect);
            if (((InputMethodManager) this.mSearchBarController.getSearchBarEditView().semGetAutoCompleteView().getContext().getSystemService("input_method")).semIsInputMethodShown() && !rect.contains((int) ev.getX(), (int) ev.getY())) {
                SIPHelper.hideInputMethod(this, true);
                initBounceAnimation();
                setTouchedContentView();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setTouchedContentView() {
        this.mIsTouchedContentView = LauncherFeature.disableFullyHideKeypad();
    }

    private void resizeByKeyboardMinimized(boolean isMinimized) {
        int keypadHeight = (int) TypedValue.applyDimension(1, 66.0f, getResources().getDisplayMetrics());
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        if (!isMinimized) {
            keypadHeight = -keypadHeight;
        }
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + keypadHeight);
    }

    public void resetKeyboardState() {
        if (this.mKeyboardState == 2) {
            setKeyBoardState(1);
        }
    }
}
