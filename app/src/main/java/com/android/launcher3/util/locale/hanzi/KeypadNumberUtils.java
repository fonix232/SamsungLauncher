package com.android.launcher3.util.locale.hanzi;

import android.support.v4.view.InputDeviceCompat;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.text.TextUtils;
import android.util.SparseIntArray;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.gamehome.GameHomeManager;
import java.lang.Character.UnicodeBlock;

public class KeypadNumberUtils {
    private static final SparseIntArray LATIN_EXTENDED_KEYPAD_MAP = new SparseIntArray();

    private KeypadNumberUtils() {
    }

    static {
        LATIN_EXTENDED_KEYPAD_MAP.put(48, 48);
        LATIN_EXTENDED_KEYPAD_MAP.put(49, 49);
        LATIN_EXTENDED_KEYPAD_MAP.put(50, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(97, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(98, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(99, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(224, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(225, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(226, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(227, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(228, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(229, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(230, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(InputDeviceCompat.SOURCE_KEYBOARD, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(259, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(261, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(231, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(263, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(269, 50);
        LATIN_EXTENDED_KEYPAD_MAP.put(51, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(100, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(GameHomeManager.REQUEST_GAMEHOME_ENABLED, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(102, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(395, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(396, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(232, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(233, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(234, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(234, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(235, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(275, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(277, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(281, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(477, 51);
        LATIN_EXTENDED_KEYPAD_MAP.put(52, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(103, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(104, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(105, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(289, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(487, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(236, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(237, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(238, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(239, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(297, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(303, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(305, 52);
        LATIN_EXTENDED_KEYPAD_MAP.put(53, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(106, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(107, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(108, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(311, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(316, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(322, 53);
        LATIN_EXTENDED_KEYPAD_MAP.put(54, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(109, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(110, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(111, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(241, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(324, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(326, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(242, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(243, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(244, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(245, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(246, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(248, 54);
        LATIN_EXTENDED_KEYPAD_MAP.put(55, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(112, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(113, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(114, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(115, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(341, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(345, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(352, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(353, 55);
        LATIN_EXTENDED_KEYPAD_MAP.put(56, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(116, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(117, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(118, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(355, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(357, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(249, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(Callback.DEFAULT_SWIPE_ANIMATION_DURATION, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(251, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(252, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(363, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(367, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(371, 56);
        LATIN_EXTENDED_KEYPAD_MAP.put(57, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(119, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(120, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(121, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(FolderLock.REQUEST_CODE_FOLDER_LOCK, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(253, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(255, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(378, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(380, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(382, 57);
        LATIN_EXTENDED_KEYPAD_MAP.put(42, 42);
        LATIN_EXTENDED_KEYPAD_MAP.put(35, 35);
        LATIN_EXTENDED_KEYPAD_MAP.put(32, 94);
    }

    private static boolean isLatinUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.BASIC_LATIN || unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    public static String makeActionCodeHKTW(String Display_name) {
        if (TextUtils.isEmpty(Display_name)) {
            return null;
        }
        StringBuilder action_code = new StringBuilder();
        int size = Display_name.length();
        for (int idx = 0; idx < size; idx++) {
            action_code.append(convertTraditionalChineseKeypadLettersToDigits(Display_name.charAt(idx)));
        }
        return action_code.toString();
    }

    public static char convertTraditionalChineseKeypadLettersToDigits(char code) {
        if (isLatinUnicodeBlock(UnicodeBlock.of(code))) {
            code = (char) LATIN_EXTENDED_KEYPAD_MAP.get(Character.toLowerCase(code), 42);
        }
        switch (code) {
            case '0':
            case 'ㄧ':
            case 'ㄨ':
            case 'ㄩ':
                return 'Э';
            case '1':
            case 'ㄅ':
            case 'ㄆ':
            case 'ㄇ':
            case 'ㄈ':
            case '一':
                return 'Ё';
            case '2':
            case 'ㄉ':
            case 'ㄊ':
            case 'ㄋ':
            case 'ㄌ':
            case '丨':
                return 'Ђ';
            case '3':
            case 'ㄍ':
            case 'ㄎ':
            case 'ㄏ':
            case '丿':
                return 'Ѓ';
            case '4':
            case 'ㄐ':
            case 'ㄑ':
            case 'ㄒ':
            case '丶':
                return 'Є';
            case '5':
            case 'ㄓ':
            case 'ㄔ':
            case 'ㄕ':
            case 'ㄖ':
            case '乛':
                return 'Ѕ';
            case '6':
            case 'ㄗ':
            case 'ㄘ':
            case 'ㄙ':
                return 'І';
            case '7':
            case 'ㄚ':
            case 'ㄛ':
            case 'ㄜ':
            case 'ㄝ':
                return 'Ї';
            case '8':
            case 'ㄞ':
            case 'ㄟ':
            case 'ㄠ':
            case 'ㄡ':
                return 'Ј';
            case '9':
            case 'ㄢ':
            case 'ㄣ':
            case 'ㄤ':
            case 'ㄥ':
            case 'ㄦ':
                return 'Љ';
            default:
                return '*';
        }
    }
}
