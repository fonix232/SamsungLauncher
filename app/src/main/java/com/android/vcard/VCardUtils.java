package com.android.vcard;

import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.vcard.exception.VCardException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VCardUtils {
    private static final String LOG_TAG = "vCard";
    private static final int[] sEscapeIndicatorsV30 = new int[]{58, 59, 44, 32};
    private static final int[] sEscapeIndicatorsV40 = new int[]{59, 58};
    private static final Map<Integer, String> sKnownImPropNameMap_ItoS = new HashMap();
    private static final Map<String, Integer> sKnownPhoneTypeMap_StoI = new HashMap();
    private static final Map<Integer, String> sKnownPhoneTypesMap_ItoS = new HashMap();
    private static final Set<String> sMobilePhoneLabelSet = new HashSet(Arrays.asList(new String[]{"MOBILE", "携帯電話", "携帯", "ケイタイ", "ｹｲﾀｲ"}));
    private static final Set<String> sPhoneTypesUnknownToContactsSet = new HashSet();
    private static final Set<Character> sUnAcceptableAsciiInV21WordSet = new HashSet(Arrays.asList(new Character[]{Character.valueOf('['), Character.valueOf(']'), Character.valueOf('='), Character.valueOf(':'), Character.valueOf('.'), Character.valueOf(','), Character.valueOf(' ')}));

    private static class DecoderException extends Exception {
        public DecoderException(String pMessage) {
            super(pMessage);
        }
    }

    public static class PhoneNumberUtilsPort {
        public static String formatNumber(String source, int defaultFormattingType) {
            SpannableStringBuilder text = new SpannableStringBuilder(source);
            PhoneNumberUtils.formatNumber(text, defaultFormattingType);
            return text.toString();
        }
    }

    private static class QuotedPrintableCodecPort {
        private static byte ESCAPE_CHAR = (byte) 61;

        private QuotedPrintableCodecPort() {
        }

        public static final byte[] decodeQuotedPrintable(byte[] bytes) throws DecoderException {
            if (bytes == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int i = 0;
            while (i < bytes.length) {
                byte b = bytes[i];
                if (b == ESCAPE_CHAR) {
                    i++;
                    int u = Character.digit((char) bytes[i], 16);
                    i++;
                    int l = Character.digit((char) bytes[i], 16);
                    if (u == -1 || l == -1) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                    try {
                        buffer.write((char) ((u << 4) + l));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                }
                buffer.write(b);
                i++;
            }
            return buffer.toByteArray();
        }
    }

    public static class TextUtilsPort {
        public static boolean isPrintableAscii(char c) {
            return (' ' <= c && c <= '~') || c == '\r' || c == '\n';
        }

        public static boolean isPrintableAsciiOnly(CharSequence str) {
            int len = str.length();
            for (int i = 0; i < len; i++) {
                if (!isPrintableAscii(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    static {
        sKnownPhoneTypesMap_ItoS.put(Integer.valueOf(9), VCardConstants.PARAM_TYPE_CAR);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_CAR, Integer.valueOf(9));
        sKnownPhoneTypesMap_ItoS.put(Integer.valueOf(6), VCardConstants.PARAM_TYPE_PAGER);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_PAGER, Integer.valueOf(6));
        sKnownPhoneTypesMap_ItoS.put(Integer.valueOf(11), VCardConstants.PARAM_TYPE_ISDN);
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_ISDN, Integer.valueOf(11));
        sKnownPhoneTypeMap_StoI.put("HOME", Integer.valueOf(1));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_WORK, Integer.valueOf(3));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_CELL, Integer.valueOf(2));
        sKnownPhoneTypeMap_StoI.put("OTHER", Integer.valueOf(7));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_CALLBACK, Integer.valueOf(8));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_COMPANY_MAIN, Integer.valueOf(10));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_RADIO, Integer.valueOf(14));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_TTY_TDD, Integer.valueOf(16));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_PHONE_EXTRA_TYPE_ASSISTANT, Integer.valueOf(19));
        sKnownPhoneTypeMap_StoI.put(VCardConstants.PARAM_TYPE_VOICE, Integer.valueOf(7));
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_MODEM);
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_MSG);
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_BBS);
        sPhoneTypesUnknownToContactsSet.add(VCardConstants.PARAM_TYPE_VIDEO);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(0), VCardConstants.PROPERTY_X_AIM);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(1), VCardConstants.PROPERTY_X_MSN);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(2), VCardConstants.PROPERTY_X_YAHOO);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(3), VCardConstants.PROPERTY_X_SKYPE_USERNAME);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(5), VCardConstants.PROPERTY_X_GOOGLE_TALK);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(6), VCardConstants.PROPERTY_X_ICQ);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(7), VCardConstants.PROPERTY_X_JABBER);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(4), VCardConstants.PROPERTY_X_QQ);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(8), VCardConstants.PROPERTY_X_NETMEETING);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(9), VCardConstants.PROPERTY_X_WHATSAPP);
        sKnownImPropNameMap_ItoS.put(Integer.valueOf(10), VCardConstants.PROPERTY_X_FACEBOOK);
    }

    public static String getPhoneTypeString(Integer type) {
        return (String) sKnownPhoneTypesMap_ItoS.get(type);
    }

    public static Object getPhoneTypeFromStrings(Collection<String> types, String number) {
        if (number == null) {
            number = "";
        }
        int type = -1;
        String label = null;
        boolean isFax = false;
        boolean isCell = false;
        boolean hasPref = false;
        boolean isCar = false;
        boolean isAssist = false;
        boolean isRadio = false;
        boolean isWork = false;
        boolean isCompanyMain = false;
        boolean isTelex = false;
        boolean isTTY_TDD = false;
        boolean isMMS = false;
        boolean isOtherFax = false;
        if (types != null) {
            for (String typeStringOrg : types) {
                if (typeStringOrg != null) {
                    String typeStringUpperCase = typeStringOrg.toUpperCase();
                    String labelCandidate;
                    Integer tmp;
                    int typeCandidate;
                    int indexOfAt;
                    if (VCardConfig.isJapanSpacialized()) {
                        if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_PREF)) {
                            hasPref = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_FAX)) {
                            isFax = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_CELL)) {
                            isCell = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_CAR)) {
                            isCar = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_PHONE_EXTRA_TYPE_RADIO)) {
                            isRadio = true;
                        } else if (typeStringOrg.equals(VCardConstants.PARAM_PHONE_EXTRA_TYPE_ASSISTANT)) {
                            isAssist = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_WORK)) {
                            isWork = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_PHONE_EXTRA_TYPE_COMPANY_MAIN)) {
                            isCompanyMain = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_TLX)) {
                            isTelex = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_PHONE_EXTRA_TYPE_TTY_TDD)) {
                            isTTY_TDD = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_MSG)) {
                            isMMS = true;
                        } else if (typeStringUpperCase.equals(VCardConstants.PARAM_PHONE_EXTRA_TYPE_OTHER_FAX)) {
                            isOtherFax = true;
                        } else {
                            if (!typeStringUpperCase.startsWith("X-") || type >= 0) {
                                labelCandidate = typeStringOrg;
                            } else {
                                labelCandidate = typeStringOrg.substring(2);
                            }
                            if (labelCandidate.length() != 0) {
                                tmp = (Integer) sKnownPhoneTypeMap_StoI.get(labelCandidate.toUpperCase());
                                if (tmp != null) {
                                    typeCandidate = tmp.intValue();
                                    indexOfAt = number.indexOf("@");
                                    if ((typeCandidate == 6 && indexOfAt > 0 && indexOfAt < number.length() - 1) || type < 0 || type == 0) {
                                        type = tmp.intValue();
                                    }
                                } else if (type < 0) {
                                    type = 0;
                                    label = labelCandidate;
                                }
                            }
                        }
                    } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_PREF)) {
                        hasPref = true;
                    } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_FAX)) {
                        isFax = true;
                    } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_CELL)) {
                        isCell = true;
                    } else {
                        if (!typeStringUpperCase.startsWith("X-") || type >= 0) {
                            labelCandidate = typeStringOrg;
                        } else {
                            labelCandidate = typeStringOrg.substring(2);
                        }
                        if (labelCandidate.length() != 0) {
                            tmp = (Integer) sKnownPhoneTypeMap_StoI.get(labelCandidate.toUpperCase());
                            if (tmp != null) {
                                typeCandidate = tmp.intValue();
                                indexOfAt = number.indexOf("@");
                                if ((typeCandidate == 6 && indexOfAt > 0 && indexOfAt < number.length() - 1) || type < 0 || type == 0 || type == 7) {
                                    type = tmp.intValue();
                                }
                            } else if (type < 0) {
                                type = 0;
                                label = labelCandidate;
                            }
                        }
                    }
                }
            }
        }
        if (type < 0) {
            if (hasPref) {
                type = 12;
            } else {
                type = 1;
            }
        }
        if (isFax) {
            if (type == 1) {
                type = 5;
            } else if (type == 3) {
                type = 4;
            } else if (type == 7 && !VCardConfig.isJapanSpacialized()) {
                type = 13;
            }
        }
        if (isCell) {
            type = 2;
        }
        if (VCardConfig.isJapanSpacialized()) {
            if (isOtherFax) {
                type = 13;
            }
            if (isCar) {
                type = 9;
            }
            if (isAssist) {
                type = 19;
            }
            if (isRadio) {
                type = 14;
            }
            if (isWork) {
                if (type == 2) {
                    type = 17;
                } else if (type == 6) {
                    type = 18;
                } else if (isFax) {
                    type = 4;
                } else {
                    type = 3;
                }
            }
            if (isCompanyMain) {
                type = 10;
            }
            if (isTelex) {
                type = 15;
            }
            if (isTTY_TDD) {
                type = 16;
            }
            if (isMMS) {
                type = 20;
            }
            if (isAssist) {
                type = 19;
            }
        }
        return type == 0 ? label : Integer.valueOf(type);
    }

    public static boolean isMobilePhoneLabel(String label) {
        return "_AUTO_CELL".equals(label) || sMobilePhoneLabelSet.contains(label);
    }

    public static boolean isValidInV21ButUnknownToContactsPhoteType(String label) {
        return sPhoneTypesUnknownToContactsSet.contains(label);
    }

    public static String getPropertyNameForIm(int protocol) {
        return (String) sKnownImPropNameMap_ItoS.get(Integer.valueOf(protocol));
    }

    public static String[] sortNameElements(int nameOrder, String familyName, String middleName, String givenName) {
        String[] list = new String[3];
        switch (VCardConfig.getNameOrderType(nameOrder)) {
            case 4:
                list[0] = middleName;
                list[1] = givenName;
                list[2] = familyName;
                break;
            case 8:
                if (containsOnlyPrintableAscii(familyName)) {
                    if (containsOnlyPrintableAscii(givenName)) {
                        list[0] = givenName;
                        list[1] = middleName;
                        list[2] = familyName;
                        break;
                    }
                }
                list[0] = familyName;
                list[1] = middleName;
                list[2] = givenName;
                break;
            default:
                list[0] = givenName;
                list[1] = middleName;
                list[2] = familyName;
                break;
        }
        return list;
    }

    public static int getPhoneNumberFormat(int vcardType) {
        if (VCardConfig.isJapaneseDevice(vcardType)) {
            return 2;
        }
        return 1;
    }

    public static String constructNameFromElements(int nameOrder, String familyName, String middleName, String givenName) {
        return constructNameFromElements(nameOrder, familyName, middleName, givenName, null, null);
    }

    public static String constructNameFromElements(int nameOrder, String familyName, String middleName, String givenName, String prefix, String suffix) {
        StringBuilder builder = new StringBuilder();
        String[] nameList = sortNameElements(nameOrder, familyName, middleName, givenName);
        boolean first = true;
        if (!TextUtils.isEmpty(prefix)) {
            first = false;
            builder.append(prefix);
        }
        for (String namePart : nameList) {
            if (!TextUtils.isEmpty(namePart)) {
                if (first) {
                    first = false;
                } else {
                    builder.append(' ');
                }
                builder.append(namePart);
            }
        }
        if (!TextUtils.isEmpty(suffix)) {
            if (!first) {
                builder.append(' ');
            }
            builder.append(suffix);
        }
        return builder.toString();
    }

    public static List<String> constructListFromValue(String value, int vcardType) {
        List<String> list = new ArrayList();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < length - 1) {
                String unescapedString;
                char nextCh = value.charAt(i + 1);
                if (VCardConfig.isVersion40(vcardType)) {
                    unescapedString = VCardParserImpl_V40.unescapeCharacter(nextCh);
                } else if (VCardConfig.isVersion30(vcardType)) {
                    unescapedString = VCardParserImpl_V30.unescapeCharacter(nextCh);
                } else {
                    if (!VCardConfig.isVersion21(vcardType)) {
                        Log.w(LOG_TAG, "Unknown vCard type");
                    }
                    unescapedString = VCardParserImpl_V21.unescapeCharacter(nextCh);
                }
                if (unescapedString != null) {
                    builder.append(unescapedString);
                    i++;
                } else {
                    builder.append(ch);
                }
            } else if (ch == ';') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
            i++;
        }
        list.add(builder.toString());
        return list;
    }

    public static List<String> constructListFromRawValue(String value, String rawValue, int vcardType) {
        List<String> list = new ArrayList();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int raw_length = rawValue.length();
        int cnt = 0;
        int count = 0;
        int i = raw_length;
        while (i > 0 && rawValue.charAt(i - 1) == ';') {
            cnt++;
            i--;
        }
        for (int j = 0; j < raw_length - cnt; j++) {
            if (rawValue.substring(0, raw_length - cnt).charAt(j) == ';') {
                count++;
            }
        }
        i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < length - 1) {
                String unescapedString;
                char nextCh = value.charAt(i + 1);
                if (VCardConfig.isVersion40(vcardType)) {
                    unescapedString = VCardParserImpl_V40.unescapeCharacter(nextCh);
                } else if (VCardConfig.isVersion30(vcardType)) {
                    unescapedString = VCardParserImpl_V30.unescapeCharacter(nextCh);
                } else {
                    if (!VCardConfig.isVersion21(vcardType)) {
                        Log.w(LOG_TAG, "Unknown vCard type");
                    }
                    unescapedString = VCardParserImpl_V21.unescapeCharacter(nextCh);
                }
                if (unescapedString != null) {
                    builder.append(unescapedString);
                    i++;
                } else {
                    builder.append(ch);
                }
            } else if (ch == ';') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
            i++;
        }
        list.add(builder.toString());
        if ("vnd.android.cursor.item/relation".equals(list.get(0))) {
            if (count == 2) {
                List<String> list2 = new ArrayList();
                String tmp_V = value.substring(0, length - cnt);
                String param_value = tmp_V.substring(tmp_V.indexOf(";") + 1, tmp_V.lastIndexOf(";"));
                String type_value = tmp_V.substring(tmp_V.lastIndexOf(";") + 1);
                list2.add((String) list.get(0));
                list2.add(param_value);
                list2.add(type_value);
                return list2;
            } else if (count == 3) {
                List<String> list3 = new ArrayList();
                StringBuilder rel_builder = new StringBuilder();
                int idx = 2;
                while (idx < list.size() && (((String) list.get(idx)).length() != 1 || !((String) list.get(idx)).equals("0"))) {
                    idx++;
                }
                for (i = 0; i < list.size(); i++) {
                    if (i == 0) {
                        list3.add((String) list.get(0));
                    } else if (i < idx) {
                        rel_builder.append((String) list.get(i));
                    } else if (i == idx) {
                        list3.add(rel_builder.toString());
                        list3.add((String) list.get(idx));
                        rel_builder = new StringBuilder();
                    } else {
                        rel_builder.append((String) list.get(i));
                    }
                }
                list3.add(rel_builder.toString());
                return list3;
            }
        }
        return list;
    }

    private static boolean isCheckCharInFirstAreaJIS(char ch) {
        if (('' > ch || ch > '') && ('à' > ch || ch > 'ï')) {
            return false;
        }
        return true;
    }

    private static boolean isCheckCharInSecondAreaJIS(char ch) {
        if (('@' > ch || ch > '~') && ('' > ch || ch > 'ü')) {
            return false;
        }
        return true;
    }

    public static List<String> constructListFromShiftJisValue(String value, int vcardType) {
        List<String> list = new ArrayList();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int state = 78;
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            switch (state) {
                case 70:
                    if (!isCheckCharInSecondAreaJIS(ch)) {
                        state = 78;
                        break;
                    }
                    state = 83;
                    break;
                case 78:
                    if (isCheckCharInFirstAreaJIS(ch)) {
                        state = 70;
                        break;
                    }
                    break;
                case 83:
                    if (!isCheckCharInFirstAreaJIS(ch)) {
                        state = 78;
                        break;
                    }
                    state = 70;
                    break;
            }
            if (ch == '\\' && i < length - 1) {
                String unescapedString;
                char nextCh = value.charAt(i + 1);
                if (VCardConfig.isVersion40(vcardType)) {
                    unescapedString = VCardParserImpl_V40.unescapeCharacter(nextCh);
                } else if (VCardConfig.isVersion30(vcardType)) {
                    unescapedString = VCardParserImpl_V30.unescapeCharacter(nextCh);
                } else {
                    if (!VCardConfig.isVersion21(vcardType)) {
                        Log.w(LOG_TAG, "Unknown vCard type");
                    }
                    unescapedString = VCardParserImpl_V21.unescapeCharacter(nextCh);
                }
                if (unescapedString == null || state == 83) {
                    builder.append(ch);
                } else {
                    builder.append(unescapedString);
                    i++;
                }
            } else if (ch == ';') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
            i++;
        }
        list.add(builder.toString());
        return list;
    }

    public static List<String> constructListFromIMValue(String value, int vcardType) {
        int sequenceCount = 0;
        List<String> list = new ArrayList();
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int state = 78;
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            switch (state) {
                case 70:
                    if (!isCheckCharInSecondAreaJIS(ch)) {
                        state = 78;
                        break;
                    }
                    state = 83;
                    break;
                case 78:
                    if (isCheckCharInFirstAreaJIS(ch)) {
                        state = 70;
                        break;
                    }
                    break;
                case 83:
                    if (!isCheckCharInFirstAreaJIS(ch)) {
                        state = 78;
                        break;
                    }
                    state = 70;
                    break;
            }
            if (ch == '\\' && i < length - 1) {
                String unescapedString;
                char nextCh = value.charAt(i + 1);
                if (VCardConfig.isVersion40(vcardType)) {
                    unescapedString = VCardParserImpl_V40.unescapeCharacter(nextCh);
                } else if (VCardConfig.isVersion30(vcardType)) {
                    unescapedString = VCardParserImpl_V30.unescapeCharacter(nextCh);
                } else {
                    if (!VCardConfig.isVersion21(vcardType)) {
                        Log.w(LOG_TAG, "Unknown vCard type");
                    }
                    unescapedString = VCardParserImpl_V21.unescapeCharacter(nextCh);
                }
                if (!(unescapedString == null || state == 83)) {
                    builder.append(ch);
                }
            } else if (ch != ';') {
                builder.append(ch);
            } else if (sequenceCount < 6) {
                sequenceCount++;
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(ch);
            }
            i++;
        }
        String customValue = builder.toString();
        if (sequenceCount >= 6) {
            list.add(customValue.substring(0, customValue.length() - 9));
        } else {
            list.add(customValue);
        }
        return list;
    }

    public static boolean containsOnlyPrintableAscii(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyPrintableAscii(Arrays.asList(values));
    }

    public static boolean containsOnlyPrintableAscii(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !TextUtilsPort.isPrintableAsciiOnly(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyNonCrLfPrintableAscii(Arrays.asList(values));
    }

    public static boolean containsOnlyNonCrLfPrintableAscii(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                int length = value.length();
                for (int i = 0; i < length; i = value.offsetByCodePoints(i, 1)) {
                    int c = value.codePointAt(i);
                    if (32 > c || c > 126) {
                        return false;
                    }
                }
                continue;
            }
        }
        return true;
    }

    public static boolean containsOnlyAlphaDigitHyphen(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyAlphaDigitHyphen(Arrays.asList(values));
    }

    public static boolean containsOnlyAlphaDigitHyphen(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String str : values) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
                    int codepoint = str.codePointAt(i);
                    if ((97 > codepoint || codepoint >= FolderLock.REQUEST_CODE_FOLDER_UNLOCK) && ((65 > codepoint || codepoint >= 91) && ((48 > codepoint || codepoint >= 58) && codepoint != 45))) {
                        return false;
                    }
                }
                continue;
            }
        }
        return true;
    }

    public static boolean containsOnlyWhiteSpaces(String... values) {
        if (values == null) {
            return true;
        }
        return containsOnlyWhiteSpaces(Arrays.asList(values));
    }

    public static boolean containsOnlyWhiteSpaces(Collection<String> values) {
        if (values == null) {
            return true;
        }
        for (String str : values) {
            if (!TextUtils.isEmpty(str)) {
                int length = str.length();
                for (int i = 0; i < length; i = str.offsetByCodePoints(i, 1)) {
                    if (!Character.isWhitespace(str.codePointAt(i))) {
                        return false;
                    }
                }
                continue;
            }
        }
        return true;
    }

    public static boolean isV21Word(String value) {
        if (TextUtils.isEmpty(value)) {
            return true;
        }
        int length = value.length();
        int i = 0;
        while (i < length) {
            int c = value.codePointAt(i);
            if (32 > c || c > 126 || sUnAcceptableAsciiInV21WordSet.contains(Character.valueOf((char) c))) {
                return false;
            }
            i = value.offsetByCodePoints(i, 1);
        }
        return true;
    }

    public static String toStringAsV30ParamValue(String value) {
        return toStringAsParamValue(value, sEscapeIndicatorsV30);
    }

    public static String toStringAsV40ParamValue(String value) {
        return toStringAsParamValue(value, sEscapeIndicatorsV40);
    }

    private static String toStringAsParamValue(String value, int[] escapeIndicators) {
        if (TextUtils.isEmpty(value)) {
            value = "";
        }
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        boolean needQuote = false;
        for (int i = 0; i < length; i = value.offsetByCodePoints(i, 1)) {
            int codePoint = value.codePointAt(i);
            if (codePoint >= 32 && codePoint != 34) {
                builder.appendCodePoint(codePoint);
                for (int indicator : escapeIndicators) {
                    if (codePoint == indicator) {
                        needQuote = true;
                        break;
                    }
                }
            }
        }
        String result = builder.toString();
        if (!result.isEmpty()) {
            if (!containsOnlyWhiteSpaces(result)) {
                if (needQuote) {
                    return new StringBuilder(String.valueOf('\"')).append(result).append('\"').toString();
                }
                return result;
            }
        }
        return "";
    }

    public static String toHalfWidthString(String orgString) {
        if (TextUtils.isEmpty(orgString)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int length = orgString.length();
        int i = 0;
        while (i < length) {
            char ch = orgString.charAt(i);
            String halfWidthText = JapaneseUtils.tryGetHalfWidthText(ch);
            if (halfWidthText != null) {
                builder.append(halfWidthText);
            } else {
                builder.append(ch);
            }
            if (VCardConfig.isJapanSpacialized() && orgString.offsetByCodePoints(i, 1) - i == 2) {
                char tempChar = orgString.charAt(i + 1);
                String tempHalfWidthText = JapaneseUtils.tryGetHalfWidthText(tempChar);
                if (tempHalfWidthText != null) {
                    builder.append(tempHalfWidthText);
                } else {
                    builder.append(tempChar);
                }
            }
            i = orgString.offsetByCodePoints(i, 1);
        }
        return builder.toString();
    }

    public static String guessImageType(byte[] input) {
        if (input == null) {
            return null;
        }
        if (input.length >= 3 && input[0] == (byte) 71 && input[1] == (byte) 73 && input[2] == (byte) 70) {
            return "GIF";
        }
        if (input.length >= 4 && input[0] == (byte) -119 && input[1] == (byte) 80 && input[2] == (byte) 78 && input[3] == (byte) 71) {
            return "PNG";
        }
        if (input.length >= 2 && input[0] == (byte) -1 && input[1] == (byte) -40) {
            return "JPEG";
        }
        return null;
    }

    public static boolean areAllEmpty(String... values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean appearsLikeAndroidVCardQuotedPrintable(String value) {
        int remainder = value.length() % 3;
        if (value.length() < 2) {
            return false;
        }
        if (remainder != 1 && remainder != 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i += 3) {
            if (value.charAt(i) != '=') {
                return false;
            }
        }
        return true;
    }

    public static String parseQuotedPrintable(String value, boolean strictLineBreaking, String sourceCharset, String targetCharset) {
        String line;
        byte[] rawBytes;
        byte[] decodedBytes;
        String str;
        StringBuilder builder = new StringBuilder();
        int length = value.length();
        int i = 0;
        while (i < length) {
            char ch = value.charAt(i);
            if (ch == '=' && i < length - 1) {
                char nextCh = value.charAt(i + 1);
                if (nextCh == ' ' || nextCh == '\t') {
                    builder.append(nextCh);
                    i++;
                    i++;
                }
            }
            builder.append(ch);
            i++;
        }
        String quotedPrintable = builder.toString();
        String[] lines;
        if (strictLineBreaking) {
            lines = quotedPrintable.split(VCardBuilder.VCARD_END_OF_LINE);
        } else {
            builder = new StringBuilder();
            length = quotedPrintable.length();
            ArrayList<String> list = new ArrayList();
            i = 0;
            while (i < length) {
                ch = quotedPrintable.charAt(i);
                if (ch == '\n') {
                    list.add(builder.toString());
                    builder = new StringBuilder();
                } else if (ch == '\r') {
                    list.add(builder.toString());
                    builder = new StringBuilder();
                    if (i < length - 1 && quotedPrintable.charAt(i + 1) == '\n') {
                        i++;
                    }
                } else {
                    builder.append(ch);
                }
                i++;
            }
            String lastLine = builder.toString();
            if (lastLine.length() > 0) {
                list.add(lastLine);
            }
            lines = (String[]) list.toArray(new String[0]);
        }
        ArrayList<String> list_new = new ArrayList();
        for (String line2 : lines) {
            list_new.add(getLine(line2.split("=0D=0A")));
        }
        String[] lines_new = (String[]) list_new.toArray(new String[0]);
        builder = new StringBuilder();
        for (String line22 : lines_new) {
            if (line22.endsWith("=")) {
                line22 = line22.substring(0, line22.length() - 1);
            }
            builder.append(line22);
        }
        String rawString = builder.toString();
        if (TextUtils.isEmpty(rawString)) {
            Log.w(LOG_TAG, "Given raw string is empty.");
        }
        try {
            rawBytes = rawString.getBytes(sourceCharset);
        } catch (UnsupportedEncodingException e) {
            Log.w(LOG_TAG, "Failed to decode: " + sourceCharset);
            rawBytes = rawString.getBytes();
        }
        try {
            decodedBytes = QuotedPrintableCodecPort.decodeQuotedPrintable(rawBytes);
        } catch (DecoderException e2) {
            Log.e(LOG_TAG, "DecoderException is thrown.");
            decodedBytes = rawBytes;
        }
        try {
            str = new String(decodedBytes, targetCharset);
        } catch (UnsupportedEncodingException e3) {
            Log.e(LOG_TAG, "Failed to encode: charset=" + targetCharset);
            str = new String(decodedBytes);
        }
        if (VCardConfig.isJapanSpacialized() && VCardConfig.IMPORT_CHARSET_SHIFTJIS.equals(targetCharset)) {
            return unicode50to60ForDCMEmoji.convert(reVal_Str);
        }
        return reVal_Str;
    }

    public static final VCardParser getAppropriateParser(int vcardType) throws VCardException {
        if (VCardConfig.isVersion21(vcardType)) {
            return new VCardParser_V21();
        }
        if (VCardConfig.isVersion30(vcardType)) {
            return new VCardParser_V30();
        }
        if (VCardConfig.isVersion40(vcardType)) {
            return new VCardParser_V40();
        }
        throw new VCardException("Version is not specified");
    }

    public static final String convertStringCharset(String originalString, String sourceCharset, String targetCharset) {
        if (sourceCharset.equalsIgnoreCase(targetCharset)) {
            return originalString;
        }
        ByteBuffer byteBuffer = Charset.forName(sourceCharset).encode(originalString);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        try {
            if (VCardConfig.isJapanSpacialized() && VCardConfig.IMPORT_CHARSET_SHIFTJIS.equals(targetCharset)) {
                return unicode50to60ForDCMEmoji.convert(new String(bytes, targetCharset));
            }
            return new String(bytes, targetCharset);
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Failed to encode: charset=" + targetCharset);
            return null;
        }
    }

    private VCardUtils() {
    }

    private static String getLine(String[] split) {
        StringBuilder builder = new StringBuilder();
        for (String data : split) {
            builder.append(data);
        }
        return builder.toString();
    }
}
