package com.android.launcher3.util.locale.hanzi;

import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.util.SemLog;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

public class HanziToPinyin {
    private static final Collator COLLATOR = Collator.getInstance(Locale.CHINESE);
    private static final boolean DEBUG = false;
    private static final String FIRST_PINYIN_UNIHAN = "阿";
    private static final char FIRST_UNIHAN = '㐀';
    private static final String LAST_PINYIN_UNIHAN = "蓙";
    private static final int[] OFFSET_POLYPHONE_PRIOR_PINYIN = new int[]{261, 367, 234, 290, 367, 379, 245, 380, 362, 294, 262, 200, 381, 19, 347, 134, 32, 149, 337, 81, 22, 208, 208, 83, 83, 118, 344};
    private static final byte[][] PINYINS = new byte[][]{new byte[]{(byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 66, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 69, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 69, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 79, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 79, (byte) 85, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 65, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 65, (byte) 73, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 71}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 72, (byte) 85, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 67, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 68, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 69, (byte) 82, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 70, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 71, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 77, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 72, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 79, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 74, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 74, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 75, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 76, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 77, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 78, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 80, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 79, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 81, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 81, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 82, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 69, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 69, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 79, (byte) 85, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 65, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 65, (byte) 73, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 71}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 72, (byte) 85, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 83, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 84, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 87, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 79, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 88, (byte) 73, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 88, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 73, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 73, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 79, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 85, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 89, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 65, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 65, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 65, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 65, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 65, (byte) 79, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 69, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 69, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 69, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 65, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 65, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 65, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 65, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 69, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 69, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 69, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 79, (byte) 78, (byte) 71, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 79, (byte) 85, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 65, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 65, (byte) 73, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 65, (byte) 78, (byte) 71}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 73, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 72, (byte) 85, (byte) 79, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 79, (byte) 78, (byte) 71, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 79, (byte) 85, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 85, (byte) 0, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 85, (byte) 65, (byte) 78, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 85, (byte) 73, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 85, (byte) 78, (byte) 0, (byte) 0, (byte) 0}, new byte[]{(byte) 90, (byte) 85, (byte) 79, (byte) 0, (byte) 0, (byte) 0}};
    private static final char[] PINYIN_LV = new char[]{'郘', '勴', '侣', '侶', '偻', '僂', '儢', '葎', '藘', '律', '吕', '呂', '驴', '闾', '嵂', '屡', '屢', '履', '挔', '捛', '捋', '滤', '濾', '绿', '缕', '垏', '旅', '爈', '氀', '梠', '榈', '櫚', '櫖', '氯', '祣', '慮', '膂', '膢', '膟', '稆', '穭', '铝', '率', '褛', '褸', '虑', '膐', '絽', '綠', '緑', '縷', '繂', '箻', '鋁', '鑢', '閭', '驢'};
    private static final char[] PINYIN_LVE = new char[]{'略', '掠', '锊', '畧', '擽', '鋢', '圙', '稤', '詻', '寽', '剠', '鋝', '鋝'};
    private static final String PINYIN_LVE_STR = "LVE";
    private static final String PINYIN_LV_STR = "LV";
    private static final char[] PINYIN_NV = new char[]{'女', '聏', '沑', '釹', '朒', '衂', '衄', '籹', '恧', '钕', '釹'};
    private static final char[] PINYIN_NVE = new char[]{'虐', '疟', '婩', '硸', '瘧'};
    private static final String PINYIN_NVE_STR = "NVE";
    private static final String PINYIN_NV_STR = "NV";
    private static final char[] POLYPHONE_PRIOR_PINYIN = new char[]{'仇', '俞', '区', '单', '於', '曾', '朴', '查', '殷', '沈', '瞿', '糜', '翟', '薄', '解', '贾', '长', '阚', '隗', '嗯', '偲', '嬷', '嬤', '儿', '兒', '呵', '蹊'};
    private static final int SPECIAL_SUFFIX_LV = 1;
    private static final int SPECIAL_SUFFIX_LVE = 2;
    private static final int SPECIAL_SUFFIX_NONE = 0;
    private static final int SPECIAL_SUFFIX_NV = 3;
    private static final int SPECIAL_SUFFIX_NVE = 4;
    private static final String TAG = "HanziToPinyin";
    private static final char[] UNIHANS = new char[]{'阿', '哎', '安', '肮', '凹', '八', '挀', '扳', '邦', '勹', '陂', '奔', '伻', '屄', '边', '灬', '憋', '汃', '冫', '癶', '峬', '嚓', '偲', '参', '仓', '撡', '冊', '嵾', '曽', '叉', '芆', '辿', '伥', '抄', '车', '抻', '阷', '吃', '充', '抽', '出', '欻', '揣', '巛', '刅', '吹', '旾', '逴', '呲', '匆', '凑', '粗', '汆', '崔', '邨', '搓', '咑', '呆', '丹', '当', '刀', '嘚', '脦', '扥', '灯', '氐', '嗲', '甸', '刁', '爹', '丁', '丟', '东', '吺', '厾', '耑', '垖', '吨', '多', '妸', '诶', '奀', '鞥', '而', '发', '帆', '匚', '飞', '分', '丰', '覅', '仏', '紑', '伕', '旮', '侅', '甘', '冈', '皋', '戈', '给', '根', '刯', '工', '勾', '估', '瓜', '乖', '关', '光', '归', '衮', '呙', '哈', '咳', '佄', '苀', '茠', '诃', '黒', '拫', '亨', '噷', '叿', '齁', '匢', '花', '怀', '犿', '巟', '灰', '昏', '吙', '丌', '加', '戋', '江', '艽', '阶', '巾', '坕', '冂', '勼', '匊', '姢', '噘', '军', '咔', '开', '刊', '忼', '尻', '匼', '嵙', '肎', '劥', '空', '抠', '扝', '夸', '蒯', '宽', '匡', '亏', '坤', '扩', '垃', '来', '兰', '啷', '捞', '肋', '勒', '棱', '刕', '俩', '奁', '良', '撩', '列', '拎', '〇', '溜', '龙', '瞜', '噜', '娈', '畧', '抡', '罗', '呣', '妈', '埋', '嫚', '牤', '猫', '么', '沒', '门', '甿', '咪', '宀', '喵', '乜', '民', '名', '谬', '摸', '哞', '毪', '拏', '腉', '囡', '囊', '孬', '疒', '娞', '恁', '能', '妮', '拈', '嬢', '鸟', '捏', '您', '宁', '妞', '农', '羺', '奴', '奻', '疟', '挪', '喔', '讴', '趴', '拍', '眅', '乓', '抛', '呸', '喷', '匉', '丕', '偏', '剽', '氕', '姘', '乒', '钋', '剖', '仆', '七', '掐', '千', '呛', '悄', '癿', '亲', '靑', '芎', '丘', '区', '峑', '缺', '夋', '呥', '穣', '娆', '惹', '人', '扔', '日', '茸', '厹', '如', '堧', '婑', '瞤', '捼', '仨', '毢', '三', '桒', '掻', '色', '森', '僧', '杀', '筛', '山', '伤', '弰', '奢', '申', '升', '尸', '収', '书', '刷', '衰', '闩', '双', '谁', '吮', '说', '厶', '忪', '捜', '苏', '狻', '夊', '孙', '唆', '他', '囼', '坍', '汤', '夲', '忑', '熥', '剔', '天', '旫', '帖', '厅', '囲', '偷', '凸', '湍', '推', '吞', '乇', '挖', '歪', '弯', '尣', '危', '昷', '翁', '挝', '乌', '夕', '虾', '仚', '乡', '灱', '些', '心', '星', '凶', '休', '吁', '轩', '削', '坃', '丫', '恹', '央', '幺', '耶', '一', '囙', '应', '哟', '佣', '优', '扜', '囦', '曰', '晕', '帀', '災', '兂', '匨', '傮', '则', '贼', '怎', '増', '扎', '捚', '沾', '张', '佋', '蜇', '贞', '争', '之', '中', '州', '朱', '抓', '跩', '专', '妆', '隹', '宒', '卓', '孜', '宗', '邹', '租', '钻', '厜', '尊', '昨'};
    private static HanziToPinyin sInstance;
    private final boolean mHasChinaCollator;

    public static class Token {
        public static final int LATIN = 1;
        public static final int PINYIN = 2;
        public static final int UNKNOWN = 3;
        public String source;
        public String target;
        public int type;

        public Token(int type, String source, String target) {
            this.type = type;
            this.source = source;
            this.target = target;
        }
    }

    protected HanziToPinyin(boolean hasChinaCollator) {
        this.mHasChinaCollator = hasChinaCollator;
    }

    public static HanziToPinyin getInstance() {
        HanziToPinyin hanziToPinyin;
        synchronized (HanziToPinyin.class) {
            if (sInstance != null) {
                hanziToPinyin = sInstance;
            } else {
                for (Locale aLocale : Collator.getAvailableLocales()) {
                    if (aLocale.equals(Locale.CHINESE)) {
                        sInstance = new HanziToPinyin(true);
                        hanziToPinyin = sInstance;
                        break;
                    }
                }
                SemLog.secW(TAG, "There is no Chinese collator, HanziToPinyin is disabled");
                sInstance = new HanziToPinyin(false);
                hanziToPinyin = sInstance;
            }
        }
        return hanziToPinyin;
    }

    private static boolean doSelfValidation() {
        char lastChar = UNIHANS[0];
        String lastString = Character.toString(lastChar);
        for (char c : UNIHANS) {
            if (lastChar != c) {
                String curString = Character.toString(c);
                if (COLLATOR.compare(lastString, curString) >= 0) {
                    SemLog.secE(TAG, "Internal error in Unihan table. The last string \"" + lastString + "\" is greater than current string \"" + curString + "\".");
                    return false;
                }
                lastString = curString;
            }
        }
        return true;
    }

    public String getPinyin(char character) {
        Token token = new Token();
        String letter = Character.toString(character);
        token.source = letter;
        int offset = checkPolyphone(letter);
        if (offset > -1) {
            token.type = 2;
        } else if (character < 'Ā') {
            token.type = 1;
            token.target = letter;
            return letter;
        } else if (character < FIRST_UNIHAN) {
            token.type = 3;
            token.target = letter;
            return letter;
        } else {
            int cmp = COLLATOR.compare(letter, FIRST_PINYIN_UNIHAN);
            if (cmp < 0) {
                token.type = 3;
                token.target = letter;
                return letter;
            }
            if (cmp == 0) {
                token.type = 2;
                offset = 0;
            } else {
                cmp = COLLATOR.compare(letter, LAST_PINYIN_UNIHAN);
                if (cmp > 0) {
                    token.type = 3;
                    token.target = letter;
                    return letter;
                } else if (cmp == 0) {
                    token.type = 2;
                    offset = UNIHANS.length - 1;
                }
            }
            token.type = 2;
            if (offset < 0) {
                int begin = 0;
                int end = UNIHANS.length - 1;
                int suffixType = pinyinSuffixType(character);
                SemLog.secD(TAG, "getPinyin   suffixType =  " + suffixType);
                switch (suffixType) {
                    case 1:
                        return PINYIN_LV_STR;
                    case 2:
                        return PINYIN_LVE_STR;
                    case 3:
                        return PINYIN_NV_STR;
                    case 4:
                        return PINYIN_NVE_STR;
                    default:
                        while (begin <= end) {
                            offset = (begin + end) / 2;
                            cmp = COLLATOR.compare(letter, Character.toString(UNIHANS[offset]));
                            if (cmp == 0) {
                                break;
                            } else if (cmp > 0) {
                                begin = offset + 1;
                            } else {
                                end = offset - 1;
                            }
                        }
                        break;
                }
            }
            if (cmp < 0) {
                offset--;
            }
        }
        StringBuilder pinyin = new StringBuilder();
        int j = 0;
        while (j < PINYINS[offset].length && PINYINS[offset][j] != (byte) 0) {
            pinyin.append((char) PINYINS[offset][j]);
            j++;
        }
        token.target = pinyin.toString();
        return pinyin.toString();
    }

    protected Token getTokenIncludingSpecialSuffix(char character) {
        Token token = new Token();
        String letter = Character.toString(character);
        token.source = letter;
        int offset = checkPolyphone(letter);
        if (offset > -1) {
            token.type = 2;
        } else {
            if (character < 'Ā') {
                token.type = 1;
                token.target = letter;
            } else if (character < FIRST_UNIHAN) {
                token.type = 3;
                token.target = letter;
            } else {
                int cmp = COLLATOR.compare(letter, FIRST_PINYIN_UNIHAN);
                if (cmp < 0) {
                    token.type = 3;
                    token.target = letter;
                } else {
                    if (cmp == 0) {
                        token.type = 2;
                        offset = 0;
                    } else {
                        cmp = COLLATOR.compare(letter, LAST_PINYIN_UNIHAN);
                        if (cmp > 0) {
                            token.type = 3;
                            token.target = letter;
                        } else if (cmp == 0) {
                            token.type = 2;
                            offset = UNIHANS.length - 1;
                        }
                    }
                    token.type = 2;
                    if (offset < 0) {
                        int begin = 0;
                        int end = UNIHANS.length - 1;
                        switch (pinyinSuffixType(character)) {
                            case 1:
                                token.type = 2;
                                token.target = PINYIN_LV_STR;
                                break;
                            case 2:
                                token.type = 2;
                                token.target = PINYIN_LVE_STR;
                                break;
                            case 3:
                                token.type = 2;
                                token.target = PINYIN_NV_STR;
                                break;
                            case 4:
                                token.type = 2;
                                token.target = PINYIN_NVE_STR;
                                break;
                            default:
                                while (begin <= end) {
                                    offset = (begin + end) / 2;
                                    cmp = COLLATOR.compare(letter, Character.toString(UNIHANS[offset]));
                                    if (cmp == 0) {
                                        break;
                                    } else if (cmp > 0) {
                                        begin = offset + 1;
                                    } else {
                                        end = offset - 1;
                                    }
                                }
                                break;
                        }
                    }
                    if (cmp < 0) {
                        offset--;
                    }
                }
            }
            return token;
        }
        StringBuilder pinyin = new StringBuilder();
        int j = 0;
        while (j < PINYINS[offset].length && PINYINS[offset][j] != (byte) 0) {
            pinyin.append((char) PINYINS[offset][j]);
            j++;
        }
        token.target = pinyin.toString();
        return token;
    }

    public ArrayList<Token> get(String input) {
        ArrayList<Token> tokens = new ArrayList();
        if (this.mHasChinaCollator && !TextUtils.isEmpty(input)) {
            int inputLength = input.length();
            StringBuilder sb = new StringBuilder();
            int tokenType = 1;
            for (int i = 0; i < inputLength; i++) {
                char character = input.charAt(i);
                if (character == ' ') {
                    if (sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                } else if (character < 'Ā') {
                    if (tokenType != 1 && sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                    tokenType = 1;
                    sb.append(Character.toUpperCase(character));
                } else if (character < FIRST_UNIHAN) {
                    if (tokenType != 3 && sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                    tokenType = 3;
                    sb.append(character);
                } else {
                    Token t = getTokenIncludingSpecialSuffix(character);
                    if (t.type == 2) {
                        if (sb.length() > 0) {
                            addToken(sb, tokens, tokenType);
                        }
                        tokens.add(t);
                        tokenType = 2;
                    } else {
                        if (tokenType != t.type && sb.length() > 0) {
                            addToken(sb, tokens, tokenType);
                        }
                        tokenType = t.type;
                        sb.append(character);
                    }
                }
            }
            if (sb.length() > 0) {
                addToken(sb, tokens, tokenType);
            }
        }
        return tokens;
    }

    private void addToken(StringBuilder sb, ArrayList<Token> tokens, int tokenType) {
        String str = sb.toString();
        tokens.add(new Token(tokenType, str, str));
        sb.setLength(0);
    }

    public boolean isHanzi(char character) {
        String letter = Character.toString(character);
        if (character >= 'Ā' && character >= FIRST_UNIHAN && COLLATOR.compare(letter, FIRST_PINYIN_UNIHAN) >= 0) {
            return true;
        }
        return false;
    }

    private int pinyinSuffixType(char character) {
        int i = 0;
        boolean matched = false;
        int result = 0;
        String letter = Character.toString(character);
        for (char lv : PINYIN_LV) {
            if (letter.equals(Character.toString(lv))) {
                result = 1;
                matched = true;
                break;
            }
        }
        if (!matched) {
            for (char lve : PINYIN_LVE) {
                if (letter.equals(Character.toString(lve))) {
                    result = 2;
                    matched = true;
                    break;
                }
            }
        }
        if (!matched) {
            for (char nv : PINYIN_NV) {
                if (letter.equals(Character.toString(nv))) {
                    result = 3;
                    matched = true;
                    break;
                }
            }
        }
        if (matched) {
            return result;
        }
        char[] cArr = PINYIN_NVE;
        int length = cArr.length;
        while (i < length) {
            if (letter.equals(Character.toString(cArr[i]))) {
                return 4;
            }
            i++;
        }
        return result;
    }

    private int checkPolyphone(String letter) {
        int arrayLen = POLYPHONE_PRIOR_PINYIN.length;
        for (int i = 0; i < arrayLen; i++) {
            if (letter.equals(Character.toString(POLYPHONE_PRIOR_PINYIN[i]))) {
                return OFFSET_POLYPHONE_PRIOR_PINYIN[i];
            }
        }
        return -1;
    }

    public boolean isHasChinese(String source) {
        boolean hadHanzi = false;
        for (char c : source.toCharArray()) {
            if (isHanzi(c)) {
                hadHanzi = true;
            }
        }
        return hadHanzi;
    }

    public String getPinyinString(String source) {
        if (!isHasChinese(source)) {
            return source;
        }
        ArrayList<Token> tokens = get(source);
        String pinyinTitle = "";
        for (int i = 0; i < tokens.size(); i++) {
            pinyinTitle = pinyinTitle + ((Token) tokens.get(i)).target;
        }
        Log.d("AoS ******", "getPinyinString   :" + pinyinTitle);
        return pinyinTitle;
    }
}
