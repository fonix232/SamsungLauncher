package com.android.launcher3.common.model;

import android.content.Context;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.locale.LocaleUtils;
import java.text.Collator;
import java.util.Comparator;

public class AppNameComparator {
    private final AbstractUserComparator<ItemInfo> mAppInfoComparator;
    private final Collator mCollator = Collator.getInstance();
    private final Comparator<String> mSectionNameComparator;

    public AppNameComparator(Context context) {
        this.mAppInfoComparator = new AbstractUserComparator<ItemInfo>(context) {
            public final int compare(ItemInfo a, ItemInfo b) {
                int result = AppNameComparator.this.compareTitles(a.title.toString(), b.title.toString());
                if (!(result != 0 || a.componentName == null || b.componentName == null)) {
                    result = a.componentName.compareTo(b.componentName);
                    if (result == 0) {
                        return super.compare(a, b);
                    }
                }
                return result;
            }
        };
        this.mSectionNameComparator = new Comparator<String>() {
            public int compare(String o1, String o2) {
                return AppNameComparator.this.compareTitles(o1, o2);
            }
        };
    }

    public Comparator<ItemInfo> getAppInfoComparator() {
        return this.mAppInfoComparator;
    }

    public Comparator<String> getSectionNameComparator() {
        return this.mSectionNameComparator;
    }

    private int compareTitles(String aTitle, String bTitle) {
        boolean aStartsWithLetter;
        String titleA = aTitle.toString();
        String titleB = bTitle.toString();
        titleA = LocaleUtils.getInstance().getSortKey(titleA);
        titleB = LocaleUtils.getInstance().getSortKey(titleB);
        if (titleA.length() <= 0 || !Character.isLetterOrDigit(titleA.codePointAt(0))) {
            aStartsWithLetter = false;
        } else {
            aStartsWithLetter = true;
        }
        boolean bStartsWithLetter;
        if (titleB.length() <= 0 || !Character.isLetterOrDigit(titleB.codePointAt(0))) {
            bStartsWithLetter = false;
        } else {
            bStartsWithLetter = true;
        }
        if (aStartsWithLetter && !bStartsWithLetter) {
            return 1;
        }
        if (aStartsWithLetter || !bStartsWithLetter) {
            return this.mCollator.compare(titleA, titleB);
        }
        return -1;
    }
}
