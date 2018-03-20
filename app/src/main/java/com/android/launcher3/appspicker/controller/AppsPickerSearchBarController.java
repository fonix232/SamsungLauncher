package com.android.launcher3.appspicker.controller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AllAppsSearchBarController;
import com.android.launcher3.appspicker.view.AppsPickerContainerView;
import com.android.launcher3.util.SIPHelper;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class AppsPickerSearchBarController extends AllAppsSearchBarController implements OnQueryTextListener {
    private static final String TAG = AppsPickerSearchBarController.class.getSimpleName();
    private final Context mContext;
    private SearchView mSearchBarEditView;
    private TextView mSearchEditText;
    private AppsPickerSearchAlgorithm mSearchManager;
    private LinearLayout mSearchViewContainer;

    public AppsPickerSearchBarController(Context context) {
        this.mContext = context;
    }

    protected void onInitialize() {
        this.mSearchManager = new AppsPickerSearchAlgorithm(this.mApps.getApps());
    }

    public View getView(ViewGroup parent) {
        View searchView = LayoutInflater.from(parent.getContext()).inflate(R.layout.apps_picker_search_bar, parent, false);
        this.mSearchBarEditView = (SearchView) searchView.findViewById(R.id.apps_picker_app_search_input);
        this.mSearchBarEditView.onActionViewCollapsed();
        this.mSearchBarEditView.setImeOptions(3);
        this.mSearchBarEditView.setInputType(8193);
        this.mSearchBarEditView.onActionViewExpanded();
        this.mSearchBarEditView.clearFocus();
        this.mSearchBarEditView.setBackgroundColor(0);
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchBarEditView.semGetAutoCompleteView().setPrivateImeOptions("nm");
            this.mSearchBarEditView.semGetAutoCompleteView().setFilters(Utilities.getEditTextMaxLengthFilter(this.mSearchBarEditView.getContext(), 30));
        }
        this.mSearchBarEditView.setOnQueryTextListener(this);
        ((Launcher) this.mSearchBarEditView.getContext()).enableVoiceSearch(this.mSearchBarEditView);
        ((LinearLayout) this.mSearchBarEditView.getParent()).setBackgroundColor(0);
        this.mSearchViewContainer = (LinearLayout) searchView.findViewById(R.id.apps_picker_search_bar_wrapper);
        ImageView voiceButton = (ImageView) this.mSearchBarEditView.findViewById(this.mContext.getResources().getIdentifier("android:id/search_voice_btn", null, null));
        if (voiceButton != null) {
            voiceButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Utilities.startVoiceRecognitionActivity(AppsPickerSearchBarController.this.mContext, getClass().getPackage().getName(), 601);
                }
            });
        }
        searchView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                new Handler().post(new Runnable() {
                    public void run() {
                        SIPHelper.hideInputMethod(AppsPickerSearchBarController.this.mSearchBarEditView, true);
                    }
                });
                return true;
            }
        });
        this.mSearchEditText = (TextView) searchView.findViewById(Utilities.getSearchEditId(this.mContext));
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchBarEditView.semGetAutoCompleteView().setOnFocusChangeListener(new OnFocusChangeListener() {
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        SIPHelper.hideInputMethod(view, true);
                    }
                    if (hasFocus && (AppsPickerSearchBarController.this.mCb instanceof AppsPickerContainerView)) {
                        Resources res = AppsPickerSearchBarController.this.mContext.getResources();
                        int pickerMode = ((AppsPickerContainerView) AppsPickerSearchBarController.this.mCb).getPickerMode();
                        if (pickerMode == 1) {
                            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_AppsPicker), res.getString(R.string.event_HA_Search));
                        } else if (pickerMode != 0) {
                        } else {
                            if (((Launcher) AppsPickerSearchBarController.this.mContext).getDragLayer().getBackgroundImageAlpha() > 0.0f) {
                                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_AppsFolder_AddApps), res.getString(R.string.event_FolderAddApps_Search));
                            } else {
                                SALogging.getInstance().insertEventLog(res.getString(R.string.screen_HomeFolder_AddApps), res.getString(R.string.event_FolderAddApps_Search));
                            }
                        }
                    }
                }
            });
        }
        changeColorAndBackground(WhiteBgManager.isWhiteBg());
        return searchView;
    }

    public void focusSearchField() {
        this.mSearchBarEditView.requestFocus();
    }

    public boolean isSearchFieldFocused() {
        return this.mSearchBarEditView.isFocused();
    }

    public void reset() {
        this.mSearchManager.cancel(true);
        this.mSearchBarEditView.setQuery("", false);
    }

    public boolean shouldShowPredictionBar() {
        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    public void onVoiceSearch(String query) {
        if (query != null) {
            this.mSearchBarEditView.setQuery(query, false);
            onQueryTextChange(query);
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (AppsPickerSearchBarController.this.mSearchEditText != null && AppsPickerSearchBarController.this.mSearchEditText.hasFocus()) {
                    InputMethodManager inputManager = (InputMethodManager) AppsPickerSearchBarController.this.mSearchEditText.getContext().getSystemService("input_method");
                    if (inputManager != null && !inputManager.semIsInputMethodShown()) {
                        inputManager.showSoftInput(AppsPickerSearchBarController.this.mSearchEditText, 1);
                    }
                }
            }
        }, 300);
    }

    public boolean onQueryTextChange(String newText) {
        if (newText.isEmpty()) {
            this.mSearchManager.cancel(true);
            this.mCb.clearSearchResult();
        } else {
            this.mSearchManager.cancel(false);
            this.mSearchManager.doSearch(newText, this.mCb);
        }
        return false;
    }

    public SearchView getSearchBarEditView() {
        return this.mSearchBarEditView;
    }

    public void changeColorAndBackground(boolean isWhiteBg) {
        int textColorId = isWhiteBg ? R.color.apps_picker_black_color : R.color.apps_picker_white_color;
        int textColor = this.mContext.getResources().getColor(textColorId, null);
        int searchButtonId = this.mContext.getResources().getIdentifier("android:id/search_button", null, null);
        ImageView voiceButton = (ImageView) this.mSearchBarEditView.findViewById(this.mContext.getResources().getIdentifier("android:id/search_voice_btn", null, null));
        ImageView searchButton = (ImageView) this.mSearchBarEditView.findViewById(searchButtonId);
        ImageView searchCloseButton = (ImageView) this.mSearchBarEditView.findViewById(this.mContext.getResources().getIdentifier("android:id/search_close_btn", null, null));
        ColorFilter filter = new LightingColorFilter(textColor, 0);
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchBarEditView.semGetAutoCompleteView().setTextColor(textColor);
            this.mSearchBarEditView.semGetAutoCompleteView().setHintTextColor(textColor);
        }
        if (voiceButton != null) {
            voiceButton.getDrawable().setColorFilter(filter);
            voiceButton.setMaxWidth(R.dimen.voice_btn_width);
            voiceButton.setMaxHeight(R.dimen.voice_btn_width);
        }
        if (searchButton != null) {
            searchButton.getDrawable().setColorFilter(filter);
        }
        if (searchCloseButton != null) {
            searchCloseButton.getDrawable().setColorFilter(filter);
        }
        this.mSearchViewContainer.findViewById(R.id.apps_picker_search_under_bar).setBackgroundResource(textColorId);
    }

    public void notifyLayoutChanged() {
        MarginLayoutParams lp = (MarginLayoutParams) this.mSearchViewContainer.getLayoutParams();
        lp.setMarginStart(this.mContext.getResources().getDimensionPixelOffset(R.dimen.appsPicker_container_margin_start));
        this.mSearchViewContainer.setLayoutParams(lp);
    }
}
