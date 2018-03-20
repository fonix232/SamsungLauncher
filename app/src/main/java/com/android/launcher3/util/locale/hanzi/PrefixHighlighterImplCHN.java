package com.android.launcher3.util.locale.hanzi;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class PrefixHighlighterImplCHN {
    private ForegroundColorSpan mPrefixColorSpan;
    private StyleSpan mPrefixStyleSpan;

    public CharSequence doApply(CharSequence text, String prefix, int prefixHighlightColor) {
        if (prefix == null) {
            return text;
        }
        int index = FormatUtils.indexOfWordPrefix(text, prefix);
        CharSequence result;
        if (index == -1) {
            int[] indexInitial = new int[prefix.length()];
            if (!ChineseHighlightHelper.findPosToPinyin(text, prefix, indexInitial)) {
                return text;
            }
            if (this.mPrefixColorSpan == null) {
                this.mPrefixColorSpan = new ForegroundColorSpan(prefixHighlightColor);
            }
            if (this.mPrefixStyleSpan == null) {
                this.mPrefixStyleSpan = new StyleSpan(0);
            }
            result = new SpannableString(text);
            int count = 0;
            while (count < indexInitial.length) {
                if (count == 0 || indexInitial[count] != 0) {
                    int start = indexInitial[0];
                    int end = indexInitial[indexInitial.length - 1] + 1;
                    if (end >= start) {
                        result.setSpan(this.mPrefixColorSpan, start, end, 0);
                        result.setSpan(this.mPrefixStyleSpan, start, end, 0);
                    }
                }
                count++;
            }
            return result;
        }
        if (this.mPrefixColorSpan == null) {
            this.mPrefixColorSpan = new ForegroundColorSpan(prefixHighlightColor);
        }
        if (this.mPrefixStyleSpan == null) {
            this.mPrefixStyleSpan = new StyleSpan(0);
        }
        result = new SpannableString(text);
        result.setSpan(this.mPrefixColorSpan, index, prefix.length() + index, 0);
        result.setSpan(this.mPrefixStyleSpan, index, prefix.length() + index, 0);
        return result;
    }
}
