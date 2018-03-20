package com.android.launcher3.widget.view;

import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.util.SIPHelper;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.controller.WidgetController;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.view.WidgetPagedView.Filter;
import com.android.vcard.VCardConfig;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WidgetSearchbar extends LinearLayout implements OnKeyListener, OnQueryTextListener {
    private static final String RUNTIME_WIDGET_SEARCH_STRING = "RUNTIME_WIDGET_SEARCH_STRING";
    private static final String TAG = "WidgetSearchbar";
    private static Style[] sStyles = new Style[2];
    private View mDivider;
    private boolean mHasUninstallableApps;
    private MenuActionListener mMenuActionListener;
    private OnMenuItemClickListener mMenuItemClickListener;
    private ImageButton mMoreButton;
    private View mMoreButtonContainer;
    private PopupMenu mPopupMenu;
    private boolean mSamsungMembersEnabled;
    private ImageView mSearchButton;
    private ImageView mSearchCloseButton;
    private TextView mSearchEditText;
    private SearchListener mSearchListener;
    private SearchView mSearchView;
    private ImageView mSearchVoiceButton;
    private boolean mSkippedFirst;
    private State mState;
    private LinearLayout mTitlebarDivider;

    public interface MenuActionListener {
        void changeStateToUninstall();

        void startContactUs();
    }

    public interface SearchListener {
        void applySearchResult(String str);

        void onUpkeyPressed(View view);

        void setSearchFilter(Filter filter);

        void setSearchString(String str);
    }

    private static class Style {
        int backgroundColorId;
        int dividerColorId;
        int iconColorId;
        int textColorId;

        private Style() {
        }
    }

    private class SearchFilter implements Filter {
        private SearchFilter() {
        }

        public List<Object> filterWidgets(List<Object> in) {
            List<Object> out = new ArrayList();
            String search = WidgetSearchbar.this.mSearchView.getQuery().toString().toUpperCase();
            if (WidgetSearchbar.this.mSearchListener != null) {
                WidgetSearchbar.this.mSearchListener.setSearchString(search);
            }
            Iterator it = in.iterator();
            while (it.hasNext()) {
                List<PendingAddItemInfo> items = (List) it.next();
                String label = ((PendingAddItemInfo) items.get(0)).getApplicationLabel();
                if (label == null || label.toUpperCase().indexOf(search) == -1) {
                    List<PendingAddItemInfo> buf = null;
                    for (PendingAddItemInfo info : items) {
                        if (info.getLabel(WidgetSearchbar.this.getContext()).toUpperCase().indexOf(search) != -1) {
                            if (buf == null) {
                                buf = new ArrayList();
                            }
                            buf.add(info);
                        }
                    }
                    if (buf != null) {
                        out.add(buf);
                    }
                } else {
                    out.add(items);
                }
            }
            return out;
        }
    }

    static {
        sStyles[0] = new Style();
        sStyles[0].textColorId = R.color.widget_searchview_text_color;
        sStyles[0].iconColorId = R.color.searchbar_background_color;
        sStyles[0].backgroundColorId = R.color.searchbar_background_color;
        sStyles[0].dividerColorId = R.color.searchbar_divider_color;
        sStyles[1] = new Style();
        sStyles[1].textColorId = R.color.widget_searchview_text_color_darken;
        sStyles[1].iconColorId = R.color.widget_searchview_text_color_darken;
        sStyles[1].backgroundColorId = R.color.searchbar_background_color_darken;
        sStyles[1].dividerColorId = R.color.searchbar_divider_color_darken;
    }

    public WidgetSearchbar(Context context) {
        this(context, null);
    }

    public WidgetSearchbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetSearchbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSkippedFirst = false;
        this.mHasUninstallableApps = false;
        this.mSamsungMembersEnabled = false;
        this.mState = State.NORMAL;
        this.mMenuItemClickListener = new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (WidgetSearchbar.this.mMenuActionListener == null) {
                    return false;
                }
                if (item.getItemId() == R.id.options_widget_menu_contactus) {
                    WidgetSearchbar.this.mMenuActionListener.startContactUs();
                    SALogging.getInstance().insertEventLog(WidgetSearchbar.this.getResources().getString(R.string.screen_Widgets), WidgetSearchbar.this.getResources().getString(R.string.event_ContactUs));
                } else if (item.getItemId() == R.id.options_widget_menu_uninstall) {
                    WidgetSearchbar.this.mMenuActionListener.changeStateToUninstall();
                    SALogging.getInstance().insertEventLog(WidgetSearchbar.this.getResources().getString(R.string.screen_Widgets), WidgetSearchbar.this.getResources().getString(R.string.event_Unistall));
                }
                return true;
            }
        };
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSamsungMembersEnabled = Utilities.isSamsungMembersEnabled(getContext());
        this.mSearchView = (SearchView) findViewById(R.id.widget_searchbar_view);
        ((EditText) this.mSearchView.findViewById(this.mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null))).setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.widget_searchview_text_size));
        this.mSearchView.setImeOptions(3);
        this.mSearchView.setInputType(8193);
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchView.semGetAutoCompleteView().setPrivateImeOptions("nm");
        }
        this.mSearchView.onActionViewExpanded();
        ((Launcher) this.mSearchView.getContext()).enableVoiceSearch(this.mSearchView);
        this.mSearchView.setBackgroundColor(0);
        int searchButtonId = getResources().getIdentifier("android:id/search_button", null, null);
        int searchVoiceId = getResources().getIdentifier("android:id/search_voice_btn", null, null);
        int searchCloseId = getResources().getIdentifier("android:id/search_close_btn", null, null);
        this.mSearchVoiceButton = (ImageView) this.mSearchView.findViewById(searchVoiceId);
        if (this.mSearchVoiceButton.getLayoutParams() instanceof LayoutParams) {
            LayoutParams lp = (LayoutParams) this.mSearchVoiceButton.getLayoutParams();
            lp.width = getResources().getDimensionPixelSize(R.dimen.voice_btn_width);
            lp.height = getResources().getDimensionPixelSize(R.dimen.voice_btn_width);
            lp.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.apps_search_titlebar_btn_margin));
            this.mSearchVoiceButton.setPadding(0, 0, 0, 0);
            this.mSearchVoiceButton.setLayoutParams(lp);
        }
        this.mSearchVoiceButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Utilities.startVoiceRecognitionActivity(WidgetSearchbar.this.getContext(), getClass().getPackage().getName(), WidgetController.REQUEST_CODE_VOICE_RECOGNITION);
                SALogging.getInstance().insertEventLog(WidgetSearchbar.this.getResources().getString(R.string.screen_Widgets), WidgetSearchbar.this.getResources().getString(R.string.event_VoiceSearch));
            }
        });
        this.mSearchButton = (ImageView) this.mSearchView.findViewById(searchButtonId);
        this.mSearchCloseButton = (ImageView) this.mSearchView.findViewById(searchCloseId);
        this.mDivider = findViewById(R.id.widget_searchbar_divider);
        this.mTitlebarDivider = (LinearLayout) findViewById(R.id.widget_searchbar_titlebar_divider);
        this.mMoreButtonContainer = findViewById(R.id.widget_more_menu_btn);
        this.mMoreButton = (ImageButton) findViewById(R.id.widget_more_menu_btn_imagebutton);
        this.mMoreButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WidgetSearchbar.this.mSearchView.clearFocus();
                WidgetSearchbar.this.showPopupMenu();
            }
        });
        this.mSearchEditText = (TextView) this.mSearchView.findViewById(Utilities.getSearchEditId(getContext()));
        this.mSearchEditText.setImeOptions(this.mSearchEditText.getImeOptions() | VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchEditText.semSetDirectPenInputEnabled(false);
        }
        changeColorAndBackground(false);
    }

    public void setSearchListener(SearchListener l) {
        this.mSearchListener = l;
    }

    public void setMenuActionListener(MenuActionListener l) {
        this.mMenuActionListener = l;
    }

    public void setOnSearchTextViewKeyListener(OnKeyListener l) {
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchView.semGetAutoCompleteView().setOnKeyListener(l);
        }
    }

    public void setOnSearchTextViewTouchListener(OnTouchListener l) {
        if (!TestHelper.isRoboUnitTest()) {
            this.mSearchView.semGetAutoCompleteView().setOnTouchListener(l);
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && (((Launcher) getContext()).getStageManager().getTopStage() instanceof WidgetController)) {
            if (this.mState == State.NORMAL && !this.mSearchView.getQuery().toString().isEmpty()) {
                this.mSearchView.setQuery("", false);
            }
            this.mSearchView.post(new Runnable() {
                public void run() {
                    WidgetSearchbar.this.openKeyboard();
                    Log.d(WidgetSearchbar.TAG, "onWindowFocusChanged : call showSoftInput");
                }
            });
        }
    }

    public void enter(State toState, AnimatorSet animSet) {
        if (toState == State.NORMAL) {
            this.mSearchView.setQuery("", false);
            this.mSearchView.clearFocus();
            this.mSearchView.setFocusable(false);
        } else if (toState == State.SEARCH) {
            if (!TestHelper.isRoboUnitTest()) {
                this.mSearchEditText.semSetDirectPenInputEnabled(true);
                this.mSearchView.semGetAutoCompleteView().setFilters(Utilities.getEditTextMaxLengthFilter(getContext(), 30));
            }
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Widgets), getResources().getString(R.string.event_Search));
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_WIDGET_SEARCH, null, -1, false);
            this.mSearchView.setOnKeyListener(this);
            this.mSearchView.setOnQueryTextListener(this);
            this.mSkippedFirst = false;
            this.mSearchView.requestFocus();
            if (this.mSearchListener != null) {
                this.mSearchListener.setSearchFilter(new SearchFilter());
            }
        }
        if (toState == State.NORMAL || toState == State.SEARCH) {
            this.mSearchEditText.setEnabled(true);
        }
        this.mState = toState;
        updateMoreIconVisibity();
    }

    public void exit(State fromState, AnimatorSet animSet) {
        dismissPopupMenu();
        if (fromState == State.SEARCH) {
            this.mSearchView.performAccessibilityAction(128, null);
            this.mSearchView.setOnKeyListener(null);
            this.mSearchView.setOnQueryTextListener(null);
            if (!this.mSearchView.getQuery().toString().isEmpty()) {
                this.mSearchView.setQuery("", true);
            }
            if (this.mSearchListener != null) {
                this.mSearchListener.applySearchResult(null);
                this.mSearchListener.setSearchFilter(null);
            }
            if (!TestHelper.isRoboUnitTest()) {
                this.mSearchEditText.semSetDirectPenInputEnabled(false);
            }
        }
    }

    public void changeColorAndBackground(boolean whiteBg) {
        Style style = whiteBg ? sStyles[1] : sStyles[0];
        this.mMoreButtonContainer.setRotation(Utilities.sIsRtl ? 180.0f : 0.0f);
        if (!TestHelper.isRoboUnitTest()) {
            int textColor = getResources().getColor(style.textColorId, null);
            this.mSearchView.semGetAutoCompleteView().setTextColor(textColor);
            this.mSearchView.semGetAutoCompleteView().setHintTextColor(textColor);
        }
        ColorFilter filter = new LightingColorFilter(getResources().getColor(style.iconColorId, null), 0);
        setColorFilterToDrawable(this.mSearchButton.getDrawable(), filter);
        setColorFilterToDrawable(this.mSearchVoiceButton.getDrawable(), filter);
        setColorFilterToDrawable(this.mSearchCloseButton.getDrawable(), filter);
        setColorFilterToDrawable(this.mMoreButton.getDrawable(), filter);
        ColorFilter dividerColorFilter = new LightingColorFilter(getResources().getColor(style.dividerColorId, null), 0);
        setColorFilterToDrawable(this.mDivider.getBackground(), dividerColorFilter);
        setColorFilterToDrawable(this.mTitlebarDivider.getBackground(), dividerColorFilter);
    }

    private void setColorFilterToDrawable(Drawable d, ColorFilter filter) {
        d.setColorFilter(filter);
    }

    public void save(Bundle outState) {
        String search = this.mSearchView.getQuery().toString();
        if (search.isEmpty()) {
            outState.putString(RUNTIME_WIDGET_SEARCH_STRING, search);
        }
    }

    public void restore(Bundle bundle) {
    }

    public boolean onQueryTextSubmit(String query) {
        closeKeyboard(false);
        return false;
    }

    public boolean onQueryTextChange(String searchString) {
        if ((this.mSkippedFirst || !searchString.isEmpty()) && this.mSearchListener != null) {
            this.mSearchListener.applySearchResult(searchString);
        }
        this.mSkippedFirst = true;
        updateMoreIconVisibity();
        return false;
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (v == this.mSearchView && event.getAction() == 0 && keyCode == 66) {
            return true;
        }
        return false;
    }

    public void openKeyboard(boolean needCallViewClicked) {
        if (this.mState == State.SEARCH) {
            InputMethodManager inputManager = (InputMethodManager) this.mSearchEditText.getContext().getSystemService("input_method");
            if (needCallViewClicked) {
                inputManager.viewClicked(this.mSearchEditText);
            }
            if (!inputManager.semIsInputMethodShown()) {
                inputManager.showSoftInput(this.mSearchEditText, 1);
            }
        }
    }

    public void openKeyboard() {
        openKeyboard(false);
    }

    public void closeKeyboard(boolean minimize) {
        if (!this.mSearchView.hasFocus()) {
            return;
        }
        if (LauncherFeature.disableFullyHideKeypad()) {
            SIPHelper.hideInputMethod(this.mSearchView, minimize);
        } else {
            this.mSearchView.clearFocus();
        }
    }

    public void onStageEnter() {
        this.mSearchEditText.setEnabled(true);
    }

    public void onStageExit() {
        if (this.mPopupMenu != null) {
            this.mPopupMenu.dismiss();
        }
        this.mSearchEditText.setEnabled(false);
    }

    public void setHasInstallableApp(boolean has) {
        this.mHasUninstallableApps = has;
        updateMoreIconVisibity();
    }

    public void onPagedViewTouchIntercepted() {
        closeKeyboard(true);
    }

    public void onVoiceSearch(String query) {
        if (query != null) {
            setQueryString(query);
        }
        if (this.mSearchEditText.hasFocus()) {
            Log.d(TAG, "open keyboard");
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    WidgetSearchbar.this.openKeyboard();
                }
            }, 300);
        }
    }

    private void setQueryString(String query) {
        if (this.mSearchView != null) {
            this.mSearchView.setQuery(query, false);
            onQueryTextChange(query);
        }
    }

    public boolean showPopupMenu() {
        if (this.mMoreButtonContainer.getVisibility() != 0) {
            return false;
        }
        dismissPopupMenu();
        this.mPopupMenu = new PopupMenu(getContext(), ((ViewGroup) getParent()).findViewById(R.id.widget_menu_anchorview), GravityCompat.END);
        this.mPopupMenu.getMenuInflater().inflate(R.menu.options_widget_menu, this.mPopupMenu.getMenu());
        this.mPopupMenu.setOnMenuItemClickListener(this.mMenuItemClickListener);
        this.mPopupMenu.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                WidgetSearchbar.this.mPopupMenu = null;
            }
        });
        Menu menu = this.mPopupMenu.getMenu();
        updateMenuItemVisibility(menu, R.id.options_widget_menu_uninstall, this.mHasUninstallableApps);
        updateMenuItemVisibility(menu, R.id.options_widget_menu_contactus, this.mSamsungMembersEnabled);
        this.mPopupMenu.show();
        return true;
    }

    private void dismissPopupMenu() {
        if (this.mPopupMenu != null) {
            this.mPopupMenu.dismiss();
            this.mPopupMenu = null;
        }
    }

    private void updateMenuItemVisibility(Menu menu, int id, boolean visible) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    public boolean onBackPressed() {
        if (this.mSearchView.getQuery().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    public void setFocus() {
        this.mMoreButton.requestFocus();
    }

    public void setFocusToSearchEditText() {
        this.mSearchEditText.requestFocus();
    }

    private void updateMoreIconVisibity() {
        boolean hasMenu = true;
        int visibility = this.mMoreButtonContainer.getVisibility();
        if (this.mState == State.NORMAL) {
            if (!(this.mHasUninstallableApps || this.mSamsungMembersEnabled)) {
                hasMenu = false;
            }
            if (hasMenu) {
                visibility = 0;
            } else {
                visibility = 8;
            }
        } else if (this.mState == State.SEARCH) {
            if (!(this.mHasUninstallableApps || this.mSamsungMembersEnabled)) {
                hasMenu = false;
            }
            boolean noSearchText = this.mSearchView.getQuery().toString().isEmpty();
            if (hasMenu && noSearchText) {
                visibility = 0;
            } else {
                visibility = 8;
            }
        }
        if (this.mMoreButtonContainer.getVisibility() != visibility) {
            this.mMoreButtonContainer.setVisibility(visibility);
            this.mDivider.setVisibility(visibility);
        }
    }

    public void onConfigurationChangedIfNeeded() {
        ((FrameLayout.LayoutParams) getLayoutParams()).height = getResources().getDimensionPixelSize(R.dimen.widget_search_titlebar_height);
    }
}
