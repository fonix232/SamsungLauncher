package com.android.launcher3.util.locale.hanzi;

import java.text.CollationKey;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Locale;

public class NameNormalizer {
    private static final RuleBasedCollator sComplexityCollator;
    private static final RuleBasedCollator sCompressingCollator;

    static {
        if ("ga_IE".equals(Locale.getDefault().toString()) || "et_EE".equals(Locale.getDefault().toString())) {
            //SemLog.secD("NameNormalizer", "@@ sCompressingCollator set to u k  : " + Locale.getDefault());
            sCompressingCollator = (RuleBasedCollator) Collator.getInstance(Locale.UK);
        } else {
            sCompressingCollator = (RuleBasedCollator) Collator.getInstance(Locale.getDefault());
        }
        sCompressingCollator.setStrength(0);
        sCompressingCollator.setDecomposition(1);
        if ("ga_IE".equals(Locale.getDefault().toString()) || "et_EE".equals(Locale.getDefault().toString())) {
            sComplexityCollator = (RuleBasedCollator) Collator.getInstance(Locale.UK);
        } else {
            sComplexityCollator = (RuleBasedCollator) Collator.getInstance(Locale.getDefault());
        }
        sComplexityCollator.setStrength(1);
    }

    public static String normalize(String name) {
        String result = "";
        CollationKey key = sCompressingCollator.getCollationKey(lettersAndDigitsOnly(name));
        if (key == null) {
            return result;
        }
        byte[] array = key.toByteArray();
        if (array != null) {
            return Hex.encodeHex(array, true);
        }
        return result;
    }

    private static String lettersAndDigitsOnly(String name) {
        char[] letters = name.toCharArray();
        int length = 0;
        for (char c : letters) {
            if (Character.isLetterOrDigit(c)) {
                int length2 = length + 1;
                letters[length] = c;
                length = length2;
            }
        }
        if (length != letters.length) {
            return new String(letters, 0, length);
        }
        return name;
    }
}
