package com.android.vcard;

import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VCardConfig {
    public static final String DEFAULT_EXPORT_CHARSET = "UTF-8";
    public static final String DEFAULT_IMPORT_CHARSET = "UTF-8";
    public static final String DEFAULT_INTERMEDIATE_CHARSET = "ISO-8859-1";
    public static final String EXPORT_CHARSET_EUCKR = "EUC-KR";
    public static final int FLAG_APPEND_TYPE_PARAM = 67108864;
    public static final int FLAG_CONVERT_PHONETIC_NAME_STRINGS = 134217728;
    private static final int FLAG_DOCOMO = 536870912;
    public static final int FLAG_NAME_NUMBER_ONLY_EXPORT = 4194304;
    public static final int FLAG_REFRAIN_ADDRESS = 262144;
    public static final int FLAG_REFRAIN_EMAIL = 524288;
    public static final int FLAG_REFRAIN_EVENTS = 16384;
    public static final int FLAG_REFRAIN_IMAGE_EXPORT = 8388608;
    public static final int FLAG_REFRAIN_IMS = 8192;
    public static final int FLAG_REFRAIN_NOTES = 32768;
    public static final int FLAG_REFRAIN_ORGANIZATION = 131072;
    public static final int FLAG_REFRAIN_PHONE_NUMBER_FORMATTING = 33554432;
    public static final int FLAG_REFRAIN_QP_TO_NAME_PROPERTIES = 268435456;
    public static final int FLAG_REFRAIN_RELATION = 2048;
    public static final int FLAG_REFRAIN_SIP_ADDRESS = 4096;
    public static final int FLAG_REFRAIN_WEBSITES = 65536;
    private static final int FLAG_SAMSUNG = 1048576;
    private static final int FLAG_USE_ANDROID_PROPERTY = Integer.MIN_VALUE;
    private static final int FLAG_USE_DEFACT_PROPERTY = 1073741824;
    public static final String IMPORT_CHARSET_EUCKR = "EUC-KR";
    public static final String IMPORT_CHARSET_SHIFTJIS = "SHIFT_JIS";
    static final int LOG_LEVEL = 0;
    static final int LOG_LEVEL_NONE = 0;
    static final int LOG_LEVEL_PERFORMANCE_MEASUREMENT = 1;
    static final int LOG_LEVEL_SHOW_WARNING = 2;
    static final int LOG_LEVEL_VERBOSE = 3;
    private static final String LOG_TAG = "vCard";
    public static final int NAME_ORDER_DEFAULT = 0;
    public static final int NAME_ORDER_EUROPE = 4;
    public static final int NAME_ORDER_JAPANESE = 8;
    private static final int NAME_ORDER_MASK = 12;

    public static final int VCARD_TYPE_DOCOMO = 939524104;
    static final String VCARD_TYPE_DOCOMO_STR = "docomo";
    public static final int VCARD_TYPE_UNKNOWN = 0;
    public static final int VCARD_TYPE_V21_COREA = -1005584384;
    static String VCARD_TYPE_V21_COREA_STR = "v21_corea";
    public static final int VCARD_TYPE_V21_EUROPE = -1073741820;
    static final String VCARD_TYPE_V21_EUROPE_STR = "v21_europe";
    public static final int VCARD_TYPE_V21_GENERIC = -1073741824;
    static String VCARD_TYPE_V21_GENERIC_STR = "v21_generic";
    public static final int VCARD_TYPE_V21_JAPANESE = -1073741816;
    public static final int VCARD_TYPE_V21_JAPANESE_MOBILE = 402653192;
    static final String VCARD_TYPE_V21_JAPANESE_MOBILE_STR = "v21_japanese_mobile";
    static final String VCARD_TYPE_V21_JAPANESE_STR = "v21_japanese_utf8";
    public static final int VCARD_TYPE_V30_EUROPE = -1073741819;
    static final String VCARD_TYPE_V30_EUROPE_STR = "v30_europe";
    public static final int VCARD_TYPE_V30_GENERIC = -1073741823;
    static final String VCARD_TYPE_V30_GENERIC_STR = "v30_generic";
    public static final int VCARD_TYPE_V30_JAPANESE = -1073741815;
    static final String VCARD_TYPE_V30_JAPANESE_STR = "v30_japanese_utf8";
    public static final int VCARD_TYPE_V40_GENERIC = -1073741822;
    static final String VCARD_TYPE_V40_GENERIC_STR = "v40_generic";
    public static int VCARD_TYPE_DEFAULT = VCARD_TYPE_V21_GENERIC;
    public static final int VERSION_21 = 0;
    public static final int VERSION_30 = 1;
    public static final int VERSION_40 = 2;
    public static final int VERSION_MASK = 3;
    public static boolean isChineseSpacialized = false;
    public static boolean isJapaneseSpacialized = false;
    public static boolean isShiftJisAsDefault = false;
    public static boolean isValidChnCscFeature = false;
    public static boolean isValidCscFeature = false;
    public static boolean isValidShiftJisCscFeature = false;
    private static final Set<Integer> sJapaneseMobileTypeSet = new HashSet();
    private static final Map<String, Integer> sVCardTypeMap = new HashMap();

    static {
        sVCardTypeMap.put(VCARD_TYPE_V21_GENERIC_STR, VCARD_TYPE_V21_GENERIC);
        sVCardTypeMap.put(VCARD_TYPE_V30_GENERIC_STR, VCARD_TYPE_V30_GENERIC);
        sVCardTypeMap.put(VCARD_TYPE_V21_COREA_STR, VCARD_TYPE_V21_COREA);
        sVCardTypeMap.put(VCARD_TYPE_V21_EUROPE_STR, VCARD_TYPE_V21_EUROPE);
        sVCardTypeMap.put(VCARD_TYPE_V30_EUROPE_STR, VCARD_TYPE_V30_EUROPE);
        sVCardTypeMap.put(VCARD_TYPE_V21_JAPANESE_STR, VCARD_TYPE_V21_JAPANESE);
        sVCardTypeMap.put(VCARD_TYPE_V30_JAPANESE_STR, VCARD_TYPE_V30_JAPANESE);
        sVCardTypeMap.put(VCARD_TYPE_V21_JAPANESE_MOBILE_STR, VCARD_TYPE_V21_JAPANESE_MOBILE);
        sVCardTypeMap.put(VCARD_TYPE_DOCOMO_STR, VCARD_TYPE_DOCOMO);
        sJapaneseMobileTypeSet.add(VCARD_TYPE_V21_JAPANESE);
        sJapaneseMobileTypeSet.add(VCARD_TYPE_V30_JAPANESE);
        sJapaneseMobileTypeSet.add(VCARD_TYPE_V21_JAPANESE_MOBILE);
        sJapaneseMobileTypeSet.add(VCARD_TYPE_DOCOMO);
    }

    public static int getVCardTypeFromString(String vcardTypeString) {
        String loweredKey = vcardTypeString.toLowerCase();
        if (sVCardTypeMap.containsKey(loweredKey)) {
            return sVCardTypeMap.get(loweredKey);
        }
        if ("default".equalsIgnoreCase(vcardTypeString)) {
            return VCARD_TYPE_DEFAULT;
        }
        Log.e(LOG_TAG, "Unknown vCard type String: \"" + vcardTypeString + "\"");
        return VCARD_TYPE_DEFAULT;
    }

    public static boolean isVersion21(int vcardType) {
        return (vcardType & 3) == 0;
    }

    public static boolean isVersion30(int vcardType) {
        // TODO: Samsung specific code
//        if ("CMCC".equals(SemCscFeature.getInstance().getString("CscFeature_Contact_ConfigProfileService")) || (vcardType & 3) == 1) {
//            return true;
//        }
        return (vcardType & 3) == 1;
    }

    public static boolean isVersion40(int vcardType) {
        return (vcardType & 3) == 2;
    }

    public static boolean shouldUseQuotedPrintable(int vcardType) {
        return !isVersion30(vcardType);
    }

    public static int getNameOrderType(int vcardType) {
        return vcardType & 12;
    }

    public static boolean usesAndroidSpecificProperty(int vcardType) {
        return (Integer.MIN_VALUE & vcardType) != 0;
    }

    public static boolean usesDefactProperty(int vcardType) {
        return (FLAG_USE_DEFACT_PROPERTY & vcardType) != 0;
    }

    public static boolean showPerformanceLog() {
        return false;
    }

    public static boolean shouldRefrainQPToNameProperties(int vcardType) {
        return (shouldUseQuotedPrintable(vcardType) && (FLAG_REFRAIN_QP_TO_NAME_PROPERTIES & vcardType) == 0) ? false : true;
    }

    public static boolean appendTypeParamName(int vcardType) {
        return isVersion30(vcardType) || (FLAG_APPEND_TYPE_PARAM & vcardType) != 0;
    }

    public static boolean isJapaneseDevice(int vcardType) {
        return sJapaneseMobileTypeSet.contains(vcardType);
    }

    static boolean refrainPhoneNumberFormatting(int vcardType) {
        return (FLAG_REFRAIN_PHONE_NUMBER_FORMATTING & vcardType) != 0;
    }

    public static boolean needsToConvertPhoneticString(int vcardType) {
        return (FLAG_CONVERT_PHONETIC_NAME_STRINGS & vcardType) != 0;
    }

    public static boolean onlyOneNoteFieldIsAvailable(int vcardType) {
        return vcardType == VCARD_TYPE_DOCOMO;
    }

    public static boolean isDoCoMo(int vcardType) {
        return (FLAG_DOCOMO & vcardType) != 0;
    }

    public static boolean isJapanSpacialized() {
        // TODO: Samsung specific code
//        if (!isValidCscFeature) {
//            isJapaneseSpacialized = "JPN".equals(SemCscFeature.getInstance().getString("CscFeature_Contact_VcardException4"));
//            isValidCscFeature = true;
//        }
        return isJapaneseSpacialized;
    }

    public static boolean isChineseSpacialized() {
        // TODO: Samsung specific code
//        if (!isValidChnCscFeature) {
//            isChineseSpacialized = "CHN".equals(SemCscFeature.getInstance().getString("CscFeature_Contact_VcardException4"));
//            isValidChnCscFeature = true;
//        }
        return isChineseSpacialized;
    }

    public static boolean isShiftJisAsDefault() {
        // TODO: Samsung specific code
//        if (!isValidShiftJisCscFeature) {
//            isShiftJisAsDefault = "SHIFT_JIS".equals(SemCscFeature.getInstance().getString("CscFeature_Contact_ConfigDefaultCharsetVCard"));
//            isValidShiftJisCscFeature = true;
//        }
        return isShiftJisAsDefault;
    }

    private VCardConfig() {
    }
}
