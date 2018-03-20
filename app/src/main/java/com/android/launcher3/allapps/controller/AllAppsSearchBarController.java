package com.android.launcher3.allapps.controller;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.util.ComponentKey;
import java.util.ArrayList;

public abstract class AllAppsSearchBarController {
    protected AlphabeticalAppsList mApps;
    protected Callbacks mCb;

    public interface Callbacks {
        void clearSearchResult();

        void onBoundsChanged(Rect rect);

        void onGalaxyAppsSearchResult(String str, ArrayList<ComponentKey> arrayList);

        void onSearchResult(String str, ArrayList<ComponentKey> arrayList);
    }

    public abstract void focusSearchField();

    public abstract SearchView getSearchBarEditView();

    public abstract View getView(ViewGroup viewGroup);

    public abstract boolean isSearchFieldFocused();

    protected abstract void onInitialize();

    public abstract void reset();

    @Deprecated
    public abstract boolean shouldShowPredictionBar();

    public final void initialize(AlphabeticalAppsList apps, Callbacks cb) {
        this.mApps = apps;
        this.mCb = cb;
        onInitialize();
    }

    public void onVoiceSearch(String query) {
    }
}
