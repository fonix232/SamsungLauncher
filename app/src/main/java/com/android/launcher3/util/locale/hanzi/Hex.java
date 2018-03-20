package com.android.launcher3.util.locale.hanzi;

public class Hex {
    private static final byte[] DIGITS = new byte[103];
    private static final char[] FIRST_CHAR = new char[256];
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] SECOND_CHAR = new char[256];

    static {
        int i;
        byte i2;
        for (i = 0; i < 256; i++) {
            FIRST_CHAR[i] = HEX_DIGITS[(i >> 4) & 15];
            SECOND_CHAR[i] = HEX_DIGITS[i & 15];
        }
        for (i = 0; i <= 70; i++) {
            DIGITS[i] = (byte) -1;
        }
        for (i2 = (byte) 0; i2 < (byte) 10; i2 = (byte) (i2 + 1)) {
            DIGITS[i2 + 48] = i2;
        }
        for (i2 = (byte) 0; i2 < (byte) 6; i2 = (byte) (i2 + 1)) {
            DIGITS[i2 + 65] = (byte) (i2 + 10);
            DIGITS[i2 + 97] = (byte) (i2 + 10);
        }
    }

    public static String encodeHex(byte[] array, boolean zeroTerminated) {
        char[] cArray = new char[(array.length * 2)];
        int j = 0;
        for (byte anArray : array) {
            int index = anArray & 255;
            if (index == 0 && zeroTerminated) {
                break;
            }
            int i = j + 1;
            cArray[j] = FIRST_CHAR[index];
            j = i + 1;
            cArray[i] = SECOND_CHAR[index];
        }
        return new String(cArray, 0, j);
    }
}
