package com.android.launcher3.util.locale.hanzi;

import android.support.v4.view.MotionEventCompat;
import android.text.TextUtils;
import com.android.launcher3.util.ViInterpolator;
import java.util.ArrayList;

public class HanziToStroke {
    private static final int MAP_10_BEGIN = 24468;
    private static final int MAP_11_BEGIN = 24968;
    private static final int MAP_12_BEGIN = 25468;
    private static final int MAP_13_BEGIN = 25968;
    private static final int MAP_14_BEGIN = 26468;
    private static final int MAP_15_BEGIN = 26968;
    private static final int MAP_16_BEGIN = 27468;
    private static final int MAP_17_BEGIN = 27968;
    private static final int MAP_18_BEGIN = 28468;
    private static final int MAP_19_BEGIN = 28968;
    private static final int MAP_1_BEGIN = 19968;
    private static final int MAP_20_BEGIN = 29468;
    private static final int MAP_21_BEGIN = 29968;
    private static final int MAP_22_BEGIN = 30468;
    private static final int MAP_23_BEGIN = 30968;
    private static final int MAP_24_BEGIN = 31468;
    private static final int MAP_25_BEGIN = 31968;
    private static final int MAP_26_BEGIN = 32468;
    private static final int MAP_27_BEGIN = 32968;
    private static final int MAP_28_BEGIN = 33468;
    private static final int MAP_29_BEGIN = 33968;
    private static final int MAP_2_BEGIN = 20468;
    private static final int MAP_30_BEGIN = 34468;
    private static final int MAP_31_BEGIN = 34968;
    private static final int MAP_32_BEGIN = 35468;
    private static final int MAP_33_BEGIN = 35968;
    private static final int MAP_34_BEGIN = 36468;
    private static final int MAP_35_BEGIN = 36968;
    private static final int MAP_36_BEGIN = 37468;
    private static final int MAP_37_BEGIN = 37968;
    private static final int MAP_38_BEGIN = 38468;
    private static final int MAP_39_BEGIN = 38968;
    private static final int MAP_3_BEGIN = 20968;
    private static final int MAP_40_BEGIN = 39468;
    private static final int MAP_41_BEGIN = 39968;
    private static final int MAP_42_BEGIN = 40468;
    private static final int MAP_4_BEGIN = 21468;
    private static final int MAP_5_BEGIN = 21968;
    private static final int MAP_6_BEGIN = 22468;
    private static final int MAP_7_BEGIN = 22968;
    private static final int MAP_8_BEGIN = 23468;
    private static final int MAP_9_BEGIN = 23968;
    private static final int MAP_BEGIN = 19968;
    private static final int MAP_END = 40869;
    private static int[] devider = new int[]{19968, MAP_2_BEGIN, MAP_3_BEGIN, MAP_4_BEGIN, MAP_5_BEGIN, MAP_6_BEGIN, MAP_7_BEGIN, MAP_8_BEGIN, MAP_9_BEGIN, MAP_10_BEGIN, MAP_11_BEGIN, MAP_12_BEGIN, MAP_13_BEGIN, MAP_14_BEGIN, MAP_15_BEGIN, MAP_16_BEGIN, MAP_17_BEGIN, MAP_18_BEGIN, MAP_19_BEGIN, MAP_20_BEGIN, MAP_21_BEGIN, MAP_22_BEGIN, MAP_23_BEGIN, MAP_24_BEGIN, MAP_25_BEGIN, MAP_26_BEGIN, MAP_27_BEGIN, MAP_28_BEGIN, MAP_29_BEGIN, MAP_30_BEGIN, MAP_31_BEGIN, MAP_32_BEGIN, MAP_33_BEGIN, MAP_34_BEGIN, MAP_35_BEGIN, MAP_36_BEGIN, MAP_37_BEGIN, MAP_38_BEGIN, MAP_39_BEGIN, MAP_40_BEGIN, MAP_41_BEGIN, MAP_42_BEGIN, MAP_END};
    private static HanziToStroke sSingleton;

    public static class Token {
        public static final int ASCII = 1;
        public static final int STROKE = 2;
        public static final int UNKNOWN = 3;
        public String source;
        public String target;
        public int type;
    }

    public static synchronized HanziToStroke getIntance() {
        HanziToStroke hanziToStroke;
        synchronized (HanziToStroke.class) {
            if (sSingleton == null) {
                sSingleton = new HanziToStroke();
            }
            hanziToStroke = sSingleton;
        }
        return hanziToStroke;
    }

    public ArrayList<Token> get(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        ArrayList<Token> tokens = new ArrayList();
        int inputLength = input.length();
        tokens.add(getToken(input.charAt(0)));
        for (int i = 1; i < inputLength; i++) {
            Token token = getToken(input.charAt(i));
            switch (token.type) {
                case 1:
                case 3:
                    tokens.add(token);
                    break;
                case 2:
                    tokens.add(token);
                    break;
                default:
                    break;
            }
        }
        return tokens;
    }

    private Token getToken(char character) {
        Token token = new Token();
        String letter = Character.toString(character);
        token.source = letter;
        token.type = 2;
        if (character < 'Ā') {
            token.type = 1;
            token.target = letter;
        } else if (character < '一' || character > '龥') {
            token.type = 3;
            token.target = letter;
        } else {
            int[] array;
            switch (findMapper(character)) {
                case 1:
                    array = find(MapStroke1.HANZI_TO_STROKE_MAP_1, character);
                    break;
                case 2:
                    array = find(MapStroke2.HANZI_TO_STROKE_MAP_2, character);
                    break;
                case 3:
                    array = find(MapStroke3.HANZI_TO_STROKE_MAP_3, character);
                    break;
                case 4:
                    array = find(MapStroke4.HANZI_TO_STROKE_MAP_4, character);
                    break;
                case 5:
                    array = find(MapStroke5.HANZI_TO_STROKE_MAP_5, character);
                    break;
                case 6:
                    array = find(MapStroke6.HANZI_TO_STROKE_MAP_6, character);
                    break;
                case 7:
                    array = find(MapStroke7.HANZI_TO_STROKE_MAP_7, character);
                    break;
                case 8:
                    array = find(MapStroke8.HANZI_TO_STROKE_MAP_8, character);
                    break;
                case 9:
                    array = find(MapStroke9.HANZI_TO_STROKE_MAP_9, character);
                    break;
                case 10:
                    array = find(MapStroke10.HANZI_TO_STROKE_MAP_10, character);
                    break;
                case 11:
                    array = find(MapStroke11.HANZI_TO_STROKE_MAP_11, character);
                    break;
                case 12:
                    array = find(MapStroke12.HANZI_TO_STROKE_MAP_12, character);
                    break;
                case 13:
                    array = find(MapStroke13.HANZI_TO_STROKE_MAP_13, character);
                    break;
                case 14:
                    array = find(MapStroke14.HANZI_TO_STROKE_MAP_14, character);
                    break;
                case 15:
                    array = find(MapStroke15.HANZI_TO_STROKE_MAP_15, character);
                    break;
                case 16:
                    array = find(MapStroke16.HANZI_TO_STROKE_MAP_16, character);
                    break;
                case 17:
                    array = find(MapStroke17.HANZI_TO_STROKE_MAP_17, character);
                    break;
                case 18:
                    array = find(MapStroke18.HANZI_TO_STROKE_MAP_18, character);
                    break;
                case 19:
                    array = find(MapStroke19.HANZI_TO_STROKE_MAP_19, character);
                    break;
                case 20:
                    array = find(MapStroke20.HANZI_TO_STROKE_MAP_20, character);
                    break;
                case 21:
                    array = find(MapStroke21.HANZI_TO_STROKE_MAP_21, character);
                    break;
                case 22:
                    array = find(MapStroke22.HANZI_TO_STROKE_MAP_22, character);
                    break;
                case 23:
                    array = find(MapStroke23.HANZI_TO_STROKE_MAP_23, character);
                    break;
                case 24:
                    array = find(MapStroke24.HANZI_TO_STROKE_MAP_24, character);
                    break;
                case 25:
                    array = find(MapStroke25.HANZI_TO_STROKE_MAP_25, character);
                    break;
                case MotionEventCompat.AXIS_SCROLL /*26*/:
                    array = find(MapStroke26.HANZI_TO_STROKE_MAP_26, character);
                    break;
                case MotionEventCompat.AXIS_RELATIVE_X /*27*/:
                    array = find(MapStroke27.HANZI_TO_STROKE_MAP_27, character);
                    break;
                case MotionEventCompat.AXIS_RELATIVE_Y /*28*/:
                    array = find(MapStroke28.HANZI_TO_STROKE_MAP_28, character);
                    break;
                case 29:
                    array = find(MapStroke29.HANZI_TO_STROKE_MAP_29, character);
                    break;
                case ViInterpolator.SINE_IN_OUT_33 /*30*/:
                    array = find(MapStroke30.HANZI_TO_STROKE_MAP_30, character);
                    break;
                case ViInterpolator.SINE_IN_OUT_50 /*31*/:
                    array = find(MapStroke31.HANZI_TO_STROKE_MAP_31, character);
                    break;
                case 32:
                    array = find(MapStroke32.HANZI_TO_STROKE_MAP_32, character);
                    break;
                case 33:
                    array = find(MapStroke33.HANZI_TO_STROKE_MAP_33, character);
                    break;
                case 34:
                    array = find(MapStroke34.HANZI_TO_STROKE_MAP_34, character);
                    break;
                case 35:
                    array = find(MapStroke35.HANZI_TO_STROKE_MAP_35, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_5 /*36*/:
                    array = find(MapStroke36.HANZI_TO_STROKE_MAP_36, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_6 /*37*/:
                    array = find(MapStroke37.HANZI_TO_STROKE_MAP_37, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_7 /*38*/:
                    array = find(MapStroke38.HANZI_TO_STROKE_MAP_38, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_8 /*39*/:
                    array = find(MapStroke39.HANZI_TO_STROKE_MAP_39, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_9 /*40*/:
                    array = find(MapStroke40.HANZI_TO_STROKE_MAP_40, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_10 /*41*/:
                    array = find(MapStroke41.HANZI_TO_STROKE_MAP_41, character);
                    break;
                case MotionEventCompat.AXIS_GENERIC_11 /*42*/:
                    array = find(MapStroke42.HANZI_TO_STROKE_MAP_42, character);
                    break;
                default:
                    token.type = 3;
                    token.target = letter;
                    break;
            }
            if (array[0] == 0) {
                token.type = 3;
                token.target = letter;
            } else {
                StringBuilder strokes = new StringBuilder();
                for (int i : array) {
                    strokes.append((char) i);
                }
                token.target = strokes.toString();
            }
        }
        return token;
    }

    private static int findMapper(int c) {
        int i = 0;
        while (i < devider.length) {
            if (c >= devider[i] && c < devider[i + 1]) {
                return i + 1;
            }
            i++;
        }
        return -1;
    }

    private static int[] find(int[][][] values, int key) {
        int lowerBound = 0;
        int upperBound = values.length - 1;
        while (true) {
            int curIn = (lowerBound + upperBound) / 2;
            if (values[curIn][0][0] == key) {
                return values[curIn][1];
            }
            if (lowerBound > upperBound) {
                return new int[]{0};
            } else if (values[curIn][0][0] < key) {
                lowerBound = curIn + 1;
            } else {
                upperBound = curIn - 1;
            }
        }
    }
}
