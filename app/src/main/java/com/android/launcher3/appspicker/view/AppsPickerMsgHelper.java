package com.android.launcher3.appspicker.view;

import android.content.Context;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public final class AppsPickerMsgHelper {
    private static final char DELIMITER = '#';
    private static final char[] KEY_CREDITS = new char[]{'C', 'r', 'e', 'd', 'i', 't', 's'};
    private static final char[] KEY_ZEROPAGE = new char[]{'Z', 'e', 'r', 'o', 'p', 'a', 'g', 'e'};
    public static final int MODE_CREDITS = 0;
    private static final int MODE_CUSTOM_ZEROPAGE = 1;
    private static final String TAG = "AppsPickerMsgHelper";
    private static final char[] TITLE = new char[]{'D', 'e', 'v', 'e', 'l', 'o', 'p', 'e', 'd', ' ', 'b', 'y', '\n', '\n'};
    private static final char[] TOKEN1 = new char[]{'Y', 'o', 'u', 'n', 'g', 's', 'e', 'o', 'k', ' ', 'L', 'i', 'm', '\n', 'H', 'o', 'n', 'g', 's', 'e', 'o', 'k', ' ', 'K', 'w', 'o', 'n', '\n'};
    private static final char[] TOKEN2 = new char[]{'C', 'h', 'a', 'n', 'g', 'd', 'o', ' ', 'K', 'i', 'm', '\n', 'C', 'h', 'a', 'n', 'g', 'h', 'w', 'a', 'n', ' ', 'Y', 'a', 'n', 'g', '\n', 'D', 'e', 'u', 'k', 'j', 'a', 'e', ' ', 'L', 'e', 'e', '\n', 'D', 'o', 'n', 'g', 'i', 'n', ' ', 'K', 'i', 'm', '\n', 'D', 'o', 'n', 'g', 'w', 'o', 'o', 'k', ' ', 'K', 'i', 'm', '\n', 'D', 'o', 'o', 'w', 'o', 'o', 'k', ' ', 'K', 'i', 'm', '\n', 'E', 'u', 'n', 'm', 'i', ' ', 'C', 'h', 'e', 'o', 'n', '\n', 'E', 'u', 'n', 'k', 'y', 'u', 'n', 'g', ' ', 'K', 'i', 'm', '\n', 'G', 'i', 's', 'o', 'o', ' ', 'L', 'e', 'e', '\n', 'H', 'a', 'e', 'c', 'h', 'a', 'n', ' ', 'C', 'h', 'o', 'e', '\n', 'H', 'a', 'n', 'j', 'o', ' ', 'J', 'o', 'o', '\n', 'H', 'o', 'm', 'i', 'n', ' ', 'M', 'o', 'o', 'n', '\n', 'H', 'y', 'u', 'n', 'w', 'o', 'o', 'k', ' ', 'N', 'a', 'm', '\n', 'J', 'a', 'e', 'h', 'w', 'a', 'n', ' ', 'P', 'a', 'r', 'k', '\n', 'J', 'a', 'e', 'h', 'o', 'n', 'g', ' ', 'C', 'h', 'e', 'o', 'n', '\n', 'J', 'e', 'o', 'n', 'g', 'h', 'u', 'i', ' ', 'Y', 'u', 'n', '\n', 'J', 'i', 'n', 's', 'o', 'o', 'k', ' ', 'Y', 'o', 'o', 'n', '\n', 'J', 'o', 'o', 'y', 'o', 'u', 'n', 'g', ' ', 'J', 'e', 'o', 'n', '\n', 'J', 'u', 'n', 'g', 'e', 'u', 'n', ' ', 'P', 'a', 'r', 'k', '\n', 'J', 'u', 'y', 'o', 'n', 'g', ' ', 'Y', 'u', 'n', '\n', 'K', 'a', 'n', 'g', 'h', 'y', 'u', 'n', ' ', 'S', 'u', 'h', '\n', 'K', 'a', 'y', 'e', 'o', 'n', ' ', 'K', 'i', 'm', '\n', 'K', 'i', 'b', 'o', 'k', ' ', 'K', 'i', 'm', '\n', 'K', 'i', 's', 'e', 'o', 'n', 'g', ' ', 'J', 'a', 'n', 'g', '\n', 'K', 'w', 'a', 'n', 'g', 'h', 'y', 'u', 'n', ' ', 'L', 'i', 'm', '\n', 'S', 'o', 'n', 'g', 'b', 'o', ' ', 'S', 'i', 'm', '\n', 'S', 'u', 'n', 'g', 'j', 'i', 'n', ' ', 'P', 'a', 'r', 'k', '\n', 'W', 'o', 'o', 'y', 'o', 'u', 'n', 'g', ' ', 'P', 'a', 'r', 'k', '\n', 'Y', 'o', 'u', 'n', 'g', 'j', 'o', 'o', 'n', ' ', 'K', 'o', '\n', 'Y', 'u', 'n', 'k', 'y', 'o', 'u', 'n', 'g', ' ', 'J', 'e', 'o', 'n', 'g', '\n'};
    private static final char[] TOKEN3 = new char[]{'a', 'n', 'd', '\n', 'w', 'o', 'r', 'l', 'd', 'w', 'i', 'd', 'e', ' ', 'H', 'o', 'm', 'e', 's', 'c', 'r', 'e', 'e', 'n', ' ', 'm', 'e', 'm', 'b', 'e', 'r', 's'};
    private static String mBody;
    private static ArrayList<String> mKeyList;
    private static int mMode = 0;

    static String getQueryKey(Context context) {
        return DELIMITER + context.getClass().getSimpleName() + DELIMITER;
    }

    static String getKey() {
        return getKey(mMode);
    }

    private static String getKey(int mode) {
        switch (mode) {
            case 1:
                return new String(KEY_ZEROPAGE);
            default:
                return new String(KEY_CREDITS);
        }
    }

    static String getBody() {
        if (mBody == null) {
            mBody = new String(TITLE) + new String(TOKEN1) + new String(TOKEN2) + new String(TOKEN3);
        }
        return mBody;
    }

    static int findMode(Context context, String text) {
        if (mKeyList == null) {
            mKeyList = new ArrayList();
            mKeyList.add(0, DELIMITER + getKey(0) + DELIMITER + context.getClass().getSimpleName() + DELIMITER);
            mKeyList.add(1, DELIMITER + getKey(1) + DELIMITER + context.getClass().getSimpleName() + DELIMITER);
        }
        Iterator it = mKeyList.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (text != null && text.endsWith(key)) {
                return mKeyList.indexOf(key);
            }
        }
        return -1;
    }

    static void setMode(int mode) {
        boolean z = true;
        mMode = mode;
        if (mode != 1) {
            z = false;
        }
        LauncherFeature.setSupportSetToZeroPage(z);
    }

    private static void convertMessage() {
        String message = "";
        int[] array = new int[message.length()];
        for (int i = 0; i < message.length(); i++) {
            array[i] = message.charAt(i);
        }
        Log.d(TAG, Arrays.toString(array));
    }

    private static void toStringBody() {
        Log.d(TAG, getBody());
    }
}
