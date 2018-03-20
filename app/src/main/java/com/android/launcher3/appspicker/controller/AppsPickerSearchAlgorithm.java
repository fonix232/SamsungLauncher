package com.android.launcher3.appspicker.controller;

import android.os.Handler;
import com.android.launcher3.allapps.controller.AllAppsSearchBarController.Callbacks;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.locale.LocaleUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

class AppsPickerSearchAlgorithm {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s|\\p{javaSpaceChar}]+");
    private final List<IconInfo> mApps;
    private final Handler mResultHandler = new Handler();

    AppsPickerSearchAlgorithm(List<IconInfo> apps) {
        this.mApps = apps;
    }

    void cancel(boolean interruptActiveRequests) {
        if (interruptActiveRequests) {
            this.mResultHandler.removeCallbacksAndMessages(null);
        }
    }

    void doSearch(final String query, final Callbacks callback) {
        final ArrayList<ComponentKey> result = getTitleMatchResult(query);
        this.mResultHandler.post(new Runnable() {
            public void run() {
                callback.onSearchResult(query, result);
            }
        });
    }

    private ArrayList<ComponentKey> getTitleMatchResult(String query) {
        String[] queryWords = SPLIT_PATTERN.split(query.toLowerCase());
        ArrayList<ComponentKey> result = new ArrayList();
        for (IconInfo info : this.mApps) {
            if (matches(info, queryWords)) {
                result.add(info.toComponentKey());
            }
        }
        return result;
    }

    private boolean matches(IconInfo info, String[] queryWords) {
        String title = info.title.toString();
        boolean foundMatch;
        if (LocaleUtils.isChineseLookupSearching()) {
            Iterator<String> appIterator = LocaleUtils.getInstance().getNameLookupKeys(title);
            for (String queryWord : queryWords) {
                foundMatch = false;
                while (appIterator != null && appIterator.hasNext()) {
                    if (((String) appIterator.next()).toLowerCase().indexOf(queryWord) == 0) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    return false;
                }
            }
        } else {
            String[] words = SPLIT_PATTERN.split(title.toLowerCase());
            for (String queryWord2 : queryWords) {
                foundMatch = false;
                for (String word : words) {
                    if (word.contains(queryWord2)) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    return false;
                }
            }
        }
        return true;
    }
}
