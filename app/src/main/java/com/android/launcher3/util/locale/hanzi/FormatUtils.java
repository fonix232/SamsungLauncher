package com.android.launcher3.util.locale.hanzi;

import android.util.Log;

public class FormatUtils {
    public static int indexOfWordPrefix(CharSequence text, String prefix) {
        if (prefix == null || text == null) {
            return -1;
        }
        int textLength = text.length();
        int prefixLength = prefix.length();
        if (prefixLength == 0 || textLength < prefixLength) {
            return -1;
        }
        int i = 0;
        while (i < textLength) {
            while (i < textLength && !Character.isLetterOrDigit(text.charAt(i))) {
                Log.i("STT", "text.charAt(i) = " + text.charAt(i));
                i++;
            }
            if (i + prefixLength > textLength) {
                return -1;
            }
            int j = 0;
            while (j < prefixLength && Character.toUpperCase(text.charAt(i + j)) == prefix.charAt(j)) {
                j++;
            }
            if (j == prefixLength) {
                return i;
            }
            while (i < textLength && Character.isLetterOrDigit(text.charAt(i))) {
                Log.i("STT", "text.charAt(i) = " + text.charAt(i));
                i++;
            }
        }
        return -1;
    }
}
