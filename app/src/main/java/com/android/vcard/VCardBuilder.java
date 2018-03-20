package com.android.vcard;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.support.v4.view.MotionEventCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.samsung.context.sdk.samsunganalytics.a.g.c.a.c;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VCardBuilder {
    public static final int DEFAULT_EMAIL_TYPE = 3;
    public static final int DEFAULT_ORG_TYPE = 1;
    public static final int DEFAULT_PHONE_TYPE = 1;
    public static final int DEFAULT_POSTAL_TYPE = 1;
    private static final String LOG_TAG = "vCard";
    private static final String SHIFT_JIS = "SHIFT_JIS";
    private static final String VCARD_DATA_PUBLIC = "PUBLIC";
    private static final String VCARD_DATA_SEPARATOR = ":";
    private static final String VCARD_DATA_VCARD = "VCARD";
    public static final String VCARD_END_OF_LINE = "\r\n";
    private static final String VCARD_ITEM_SEPARATOR = ";";
    private static final String VCARD_PARAM_ENCODING_BASE64_AS_B = "ENCODING=B";
    private static final String VCARD_PARAM_ENCODING_BASE64_V21 = "ENCODING=BASE64";
    private static final String VCARD_PARAM_ENCODING_QP = "ENCODING=QUOTED-PRINTABLE";
    private static final String VCARD_PARAM_EQUAL = "=";
    private static final String VCARD_PARAM_SEPARATOR = ";";
    private static final String VCARD_WS = " ";
    private static final Set<String> sAllowedAndroidPropertySet = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{"vnd.android.cursor.item/nickname", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/im"})));
    private static final Map<Integer, Integer> sPostalTypePriorityMap = new HashMap();
    private final boolean mAppendTypeParamName;
    private StringBuilder mBuilder;
    private final String mCharset;
    private boolean mEndAppended;
    private final boolean mIsCoreanMobilePhone;
    private final boolean mIsDoCoMo;
    private final boolean mIsJapaneseMobilePhone;
    private final boolean mIsV30OrV40;
    private final boolean mNeedsToConvertPhoneticString;
    private final boolean mOnlyOneNoteFieldIsAvailable;
    private final boolean mRefrainsQPToNameProperties;
    private final boolean mShouldAppendCharsetParam;
    private final boolean mShouldUseQuotedPrintable;
    private final boolean mUsesAndroidProperty;
    private final boolean mUsesDefactProperty;
    private final String mVCardCharsetParameter;
    private final int mVCardType;

    private static class PostalStruct {
        final String addressData;
        final boolean appendCharset;
        final boolean reallyUseQuotedPrintable;

        public PostalStruct(boolean reallyUseQuotedPrintable, boolean appendCharset, String addressData) {
            this.reallyUseQuotedPrintable = reallyUseQuotedPrintable;
            this.appendCharset = appendCharset;
            this.addressData = addressData;
        }
    }

    static {
        sPostalTypePriorityMap.put(Integer.valueOf(1), Integer.valueOf(0));
        sPostalTypePriorityMap.put(Integer.valueOf(2), Integer.valueOf(1));
        sPostalTypePriorityMap.put(Integer.valueOf(3), Integer.valueOf(2));
        sPostalTypePriorityMap.put(Integer.valueOf(0), Integer.valueOf(3));
    }

    public VCardBuilder(int vcardType) {
        this(vcardType, null);
    }

    public VCardBuilder(int vcardType, String charset) {
        boolean z = false;
        this.mVCardType = vcardType;
        if (VCardConfig.isVersion40(vcardType)) {
            Log.w(LOG_TAG, "Should not use vCard 4.0 when building vCard. It is not officially published yet.");
        }
        boolean z2 = VCardConfig.isVersion30(vcardType) || VCardConfig.isVersion40(vcardType);
        this.mIsV30OrV40 = z2;
        this.mShouldUseQuotedPrintable = VCardConfig.shouldUseQuotedPrintable(vcardType);
        if (vcardType == VCardConfig.VCARD_TYPE_V21_COREA) {
            this.mIsCoreanMobilePhone = true;
        } else {
            this.mIsCoreanMobilePhone = false;
        }
        this.mIsDoCoMo = VCardConfig.isDoCoMo(vcardType);
        this.mIsJapaneseMobilePhone = VCardConfig.needsToConvertPhoneticString(vcardType);
        this.mOnlyOneNoteFieldIsAvailable = VCardConfig.onlyOneNoteFieldIsAvailable(vcardType);
        this.mUsesAndroidProperty = VCardConfig.usesAndroidSpecificProperty(vcardType);
        this.mUsesDefactProperty = VCardConfig.usesDefactProperty(vcardType);
        this.mRefrainsQPToNameProperties = VCardConfig.shouldRefrainQPToNameProperties(vcardType);
        this.mAppendTypeParamName = VCardConfig.appendTypeParamName(vcardType);
        this.mNeedsToConvertPhoneticString = VCardConfig.needsToConvertPhoneticString(vcardType);
        if (!(VCardConfig.isVersion30(vcardType) && "UTF-8".equalsIgnoreCase(charset))) {
            z = true;
        }
        this.mShouldAppendCharsetParam = z;
        if (VCardConfig.isDoCoMo(vcardType)) {
            Log.i(LOG_TAG, "Use the charset \"UTF-8\" for export.");
            this.mCharset = "UTF-8";
            this.mVCardCharsetParameter = "CHARSET=UTF-8";
        } else if (TextUtils.isEmpty(charset)) {
            Log.i(LOG_TAG, "Use the charset \"UTF-8\" for export.");
            this.mCharset = "UTF-8";
            this.mVCardCharsetParameter = "CHARSET=UTF-8";
        } else {
            this.mCharset = charset;
            this.mVCardCharsetParameter = "CHARSET=" + charset;
        }
        clear();
    }

    public void clear() {
        this.mBuilder = new StringBuilder();
        this.mEndAppended = false;
        appendLine(VCardConstants.PROPERTY_BEGIN, VCARD_DATA_VCARD);
        if (VCardConfig.isVersion40(this.mVCardType)) {
            appendLine(VCardConstants.PROPERTY_VERSION, VCardConstants.VERSION_V40);
        } else if (VCardConfig.isVersion30(this.mVCardType)) {
            appendLine(VCardConstants.PROPERTY_VERSION, "3.0");
        } else {
            if (!VCardConfig.isVersion21(this.mVCardType)) {
                Log.w(LOG_TAG, "Unknown vCard version detected.");
            }
            appendLine(VCardConstants.PROPERTY_VERSION, VCardConstants.VERSION_V21);
        }
    }

    private boolean containsNonEmptyName(ContentValues contentValues) {
        return (TextUtils.isEmpty(contentValues.getAsString("data3")) && TextUtils.isEmpty(contentValues.getAsString("data5")) && TextUtils.isEmpty(contentValues.getAsString("data2")) && TextUtils.isEmpty(contentValues.getAsString("data4")) && TextUtils.isEmpty(contentValues.getAsString("data6")) && TextUtils.isEmpty(contentValues.getAsString("data9")) && TextUtils.isEmpty(contentValues.getAsString("data8")) && TextUtils.isEmpty(contentValues.getAsString("data7")) && TextUtils.isEmpty(contentValues.getAsString("data1"))) ? false : true;
    }

    private ContentValues getPrimaryContentValueWithStructuredName(List<ContentValues> contentValuesList) {
        ContentValues primaryContentValues = null;
        ContentValues subprimaryContentValues = null;
        for (ContentValues contentValues : contentValuesList) {
            if (contentValues != null) {
                Integer isSuperPrimary = contentValues.getAsInteger("is_super_primary");
                if (isSuperPrimary != null && isSuperPrimary.intValue() > 0) {
                    primaryContentValues = contentValues;
                    break;
                } else if (primaryContentValues == null) {
                    Integer isPrimary = contentValues.getAsInteger("is_primary");
                    if (isPrimary != null && isPrimary.intValue() > 0 && containsNonEmptyName(contentValues)) {
                        primaryContentValues = contentValues;
                    } else if (subprimaryContentValues == null && containsNonEmptyName(contentValues)) {
                        subprimaryContentValues = contentValues;
                    }
                }
            }
        }
        if (primaryContentValues != null) {
            return primaryContentValues;
        }
        if (subprimaryContentValues != null) {
            return subprimaryContentValues;
        }
        return new ContentValues();
    }

    private VCardBuilder appendNamePropertiesV40(List<ContentValues> contentValuesList) {
        if (this.mIsDoCoMo || this.mNeedsToConvertPhoneticString) {
            Log.w(LOG_TAG, "Invalid flag is used in vCard 4.0 construction. Ignored.");
        }
        if (contentValuesList == null || contentValuesList.isEmpty()) {
            appendLine(VCardConstants.PROPERTY_FN, "");
        } else {
            ContentValues contentValues = getPrimaryContentValueWithStructuredName(contentValuesList);
            String familyName = contentValues.getAsString("data3");
            String middleName = contentValues.getAsString("data5");
            String givenName = contentValues.getAsString("data2");
            String prefix = contentValues.getAsString("data4");
            String suffix = contentValues.getAsString("data6");
            String formattedName = contentValues.getAsString("data1");
            if (TextUtils.isEmpty(familyName) && TextUtils.isEmpty(givenName) && TextUtils.isEmpty(middleName) && TextUtils.isEmpty(prefix) && TextUtils.isEmpty(suffix)) {
                if (TextUtils.isEmpty(formattedName)) {
                    appendLine(VCardConstants.PROPERTY_FN, "");
                } else {
                    familyName = formattedName;
                }
            }
            String phoneticFamilyName = contentValues.getAsString("data9");
            String phoneticMiddleName = contentValues.getAsString("data8");
            String phoneticGivenName = contentValues.getAsString("data7");
            String escapedFamily = escapeCharacters(familyName);
            String escapedGiven = escapeCharacters(givenName);
            String escapedMiddle = escapeCharacters(middleName);
            String escapedPrefix = escapeCharacters(prefix);
            String escapedSuffix = escapeCharacters(suffix);
            this.mBuilder.append(VCardConstants.PROPERTY_N);
            if (!(TextUtils.isEmpty(phoneticFamilyName) && TextUtils.isEmpty(phoneticMiddleName) && TextUtils.isEmpty(phoneticGivenName))) {
                this.mBuilder.append(";");
                this.mBuilder.append("SORT-AS=").append(VCardUtils.toStringAsV40ParamValue(escapeCharacters(phoneticFamilyName) + ';' + escapeCharacters(phoneticGivenName) + ';' + escapeCharacters(phoneticMiddleName)));
            }
            this.mBuilder.append(VCARD_DATA_SEPARATOR);
            this.mBuilder.append(escapedFamily);
            this.mBuilder.append(";");
            this.mBuilder.append(escapedGiven);
            this.mBuilder.append(";");
            this.mBuilder.append(escapedMiddle);
            this.mBuilder.append(";");
            this.mBuilder.append(escapedPrefix);
            this.mBuilder.append(";");
            this.mBuilder.append(escapedSuffix);
            this.mBuilder.append(VCARD_END_OF_LINE);
            if (TextUtils.isEmpty(formattedName)) {
                Log.w(LOG_TAG, "DISPLAY_NAME is empty.");
                appendLine(VCardConstants.PROPERTY_FN, escapeCharacters(VCardUtils.constructNameFromElements(VCardConfig.getNameOrderType(this.mVCardType), familyName, middleName, givenName, prefix, suffix)));
            } else {
                String escapedFormatted = escapeCharacters(formattedName);
                this.mBuilder.append(VCardConstants.PROPERTY_FN);
                this.mBuilder.append(VCARD_DATA_SEPARATOR);
                this.mBuilder.append(escapedFormatted);
                this.mBuilder.append(VCARD_END_OF_LINE);
            }
            appendPhoneticNameFields(contentValues);
        }
        return this;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.android.vcard.VCardBuilder appendNameProperties(java.util.List<android.content.ContentValues> r23) {
        /*
        r22 = this;
        r0 = r22;
        r2 = r0.mVCardType;
        r2 = com.android.vcard.VCardConfig.isVersion40(r2);
        if (r2 == 0) goto L_0x000f;
    L_0x000a:
        r22 = r22.appendNamePropertiesV40(r23);
    L_0x000e:
        return r22;
    L_0x000f:
        if (r23 == 0) goto L_0x0017;
    L_0x0011:
        r2 = r23.isEmpty();
        if (r2 == 0) goto L_0x004a;
    L_0x0017:
        r0 = r22;
        r2 = r0.mVCardType;
        r2 = com.android.vcard.VCardConfig.isVersion30(r2);
        if (r2 == 0) goto L_0x0038;
    L_0x0021:
        r2 = "N";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        r2 = "FN";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        goto L_0x000e;
    L_0x0038:
        r0 = r22;
        r2 = r0.mIsDoCoMo;
        if (r2 == 0) goto L_0x000e;
    L_0x003e:
        r2 = "N";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        goto L_0x000e;
    L_0x004a:
        r8 = r22.getPrimaryContentValueWithStructuredName(r23);
        r2 = "data3";
        r3 = r8.getAsString(r2);
        r2 = "data5";
        r4 = r8.getAsString(r2);
        r2 = "data2";
        r5 = r8.getAsString(r2);
        r2 = "data4";
        r6 = r8.getAsString(r2);
        r2 = "data6";
        r7 = r8.getAsString(r2);
        r2 = "data1";
        r9 = r8.getAsString(r2);
        r2 = android.text.TextUtils.isEmpty(r3);
        if (r2 == 0) goto L_0x007e;
    L_0x0078:
        r2 = android.text.TextUtils.isEmpty(r5);
        if (r2 != 0) goto L_0x027f;
    L_0x007e:
        r2 = 5;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r3;
        r21 = 1;
        r2[r21] = r5;
        r21 = 2;
        r2[r21] = r4;
        r21 = 3;
        r2[r21] = r6;
        r21 = 4;
        r2[r21] = r7;
        r0 = r22;
        r18 = r0.shouldAppendCharsetParam(r2);
        r0 = r22;
        r2 = r0.mRefrainsQPToNameProperties;
        if (r2 != 0) goto L_0x023f;
    L_0x00a1:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r3;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 == 0) goto L_0x00e2;
    L_0x00ae:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r5;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 == 0) goto L_0x00e2;
    L_0x00bb:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r4;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 == 0) goto L_0x00e2;
    L_0x00c8:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r6;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 == 0) goto L_0x00e2;
    L_0x00d5:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r7;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 != 0) goto L_0x023f;
    L_0x00e2:
        r20 = 1;
    L_0x00e4:
        r2 = android.text.TextUtils.isEmpty(r9);
        if (r2 != 0) goto L_0x0243;
    L_0x00ea:
        r16 = r9;
    L_0x00ec:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r16;
        r0 = r22;
        r17 = r0.shouldAppendCharsetParam(r2);
        r0 = r22;
        r2 = r0.mRefrainsQPToNameProperties;
        if (r2 != 0) goto L_0x0251;
    L_0x00ff:
        r2 = 1;
        r2 = new java.lang.String[r2];
        r21 = 0;
        r2[r21] = r16;
        r2 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r2);
        if (r2 != 0) goto L_0x0251;
    L_0x010c:
        r19 = 1;
    L_0x010e:
        if (r20 == 0) goto L_0x0255;
    L_0x0110:
        r0 = r22;
        r10 = r0.encodeQuotedPrintable(r3);
        r0 = r22;
        r12 = r0.encodeQuotedPrintable(r5);
        r0 = r22;
        r13 = r0.encodeQuotedPrintable(r4);
        r0 = r22;
        r14 = r0.encodeQuotedPrintable(r6);
        r0 = r22;
        r15 = r0.encodeQuotedPrintable(r7);
    L_0x012e:
        if (r19 == 0) goto L_0x0275;
    L_0x0130:
        r0 = r22;
        r1 = r16;
        r11 = r0.encodeQuotedPrintable(r1);
    L_0x0138:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "N";
        r0 = r21;
        r2.append(r0);
        if (r18 == 0) goto L_0x015f;
    L_0x0145:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r0 = r22;
        r0 = r0.mVCardCharsetParameter;
        r21 = r0;
        r0 = r21;
        r2.append(r0);
    L_0x015f:
        if (r20 == 0) goto L_0x0177;
    L_0x0161:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "ENCODING=QUOTED-PRINTABLE";
        r0 = r21;
        r2.append(r0);
    L_0x0177:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ":";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r10);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r12);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r13);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r14);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r15);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "\r\n";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "FN";
        r0 = r21;
        r2.append(r0);
        if (r17 == 0) goto L_0x0203;
    L_0x01e9:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r0 = r22;
        r0 = r0.mVCardCharsetParameter;
        r21 = r0;
        r0 = r21;
        r2.append(r0);
    L_0x0203:
        if (r19 == 0) goto L_0x021b;
    L_0x0205:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "ENCODING=QUOTED-PRINTABLE";
        r0 = r21;
        r2.append(r0);
    L_0x021b:
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ":";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r2.append(r11);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "\r\n";
        r0 = r21;
        r2.append(r0);
    L_0x0238:
        r0 = r22;
        r0.appendPhoneticNameFields(r8);
        goto L_0x000e;
    L_0x023f:
        r20 = 0;
        goto L_0x00e4;
    L_0x0243:
        r0 = r22;
        r2 = r0.mVCardType;
        r2 = com.android.vcard.VCardConfig.getNameOrderType(r2);
        r16 = com.android.vcard.VCardUtils.constructNameFromElements(r2, r3, r4, r5, r6, r7);
        goto L_0x00ec;
    L_0x0251:
        r19 = 0;
        goto L_0x010e;
    L_0x0255:
        r0 = r22;
        r10 = r0.escapeCharacters(r3);
        r0 = r22;
        r12 = r0.escapeCharacters(r5);
        r0 = r22;
        r13 = r0.escapeCharacters(r4);
        r0 = r22;
        r14 = r0.escapeCharacters(r6);
        r0 = r22;
        r15 = r0.escapeCharacters(r7);
        goto L_0x012e;
    L_0x0275:
        r0 = r22;
        r1 = r16;
        r11 = r0.escapeCharacters(r1);
        goto L_0x0138;
    L_0x027f:
        r2 = android.text.TextUtils.isEmpty(r9);
        if (r2 != 0) goto L_0x02d7;
    L_0x0285:
        r2 = "N";
        r0 = r22;
        r0.buildSinglePartNameField(r2, r9);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = ";";
        r0 = r21;
        r2.append(r0);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "\r\n";
        r0 = r21;
        r2.append(r0);
        r2 = "FN";
        r0 = r22;
        r0.buildSinglePartNameField(r2, r9);
        r0 = r22;
        r2 = r0.mBuilder;
        r21 = "\r\n";
        r0 = r21;
        r2.append(r0);
        goto L_0x0238;
    L_0x02d7:
        r0 = r22;
        r2 = r0.mVCardType;
        r2 = com.android.vcard.VCardConfig.isVersion30(r2);
        if (r2 == 0) goto L_0x02f9;
    L_0x02e1:
        r2 = "N";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        r2 = "FN";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        goto L_0x0238;
    L_0x02f9:
        r0 = r22;
        r2 = r0.mIsDoCoMo;
        if (r2 == 0) goto L_0x0238;
    L_0x02ff:
        r2 = "N";
        r21 = "";
        r0 = r22;
        r1 = r21;
        r0.appendLine(r2, r1);
        goto L_0x0238;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendNameProperties(java.util.List):com.android.vcard.VCardBuilder");
    }

    private void buildSinglePartNameField(String property, String part) {
        boolean reallyUseQuotedPrintable;
        String encodedPart;
        if (!this.mRefrainsQPToNameProperties) {
            if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(part)) {
                reallyUseQuotedPrintable = true;
                if (reallyUseQuotedPrintable) {
                    encodedPart = escapeCharacters(part);
                } else {
                    encodedPart = encodeQuotedPrintable(part);
                }
                this.mBuilder.append(property);
                if (shouldAppendCharsetParam(part)) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(this.mVCardCharsetParameter);
                }
                if (reallyUseQuotedPrintable) {
                    this.mBuilder.append(";");
                    this.mBuilder.append(VCARD_PARAM_ENCODING_QP);
                }
                this.mBuilder.append(VCARD_DATA_SEPARATOR);
                this.mBuilder.append(encodedPart);
            }
        }
        reallyUseQuotedPrintable = false;
        if (reallyUseQuotedPrintable) {
            encodedPart = escapeCharacters(part);
        } else {
            encodedPart = encodeQuotedPrintable(part);
        }
        this.mBuilder.append(property);
        if (shouldAppendCharsetParam(part)) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (reallyUseQuotedPrintable) {
            this.mBuilder.append(";");
            this.mBuilder.append(VCARD_PARAM_ENCODING_QP);
        }
        this.mBuilder.append(VCARD_DATA_SEPARATOR);
        this.mBuilder.append(encodedPart);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void appendPhoneticNameFields(android.content.ContentValues r15) {
        /*
        r14 = this;
        r12 = "data9";
        r9 = r15.getAsString(r12);
        r12 = "data8";
        r11 = r15.getAsString(r12);
        r12 = "data7";
        r10 = r15.getAsString(r12);
        r12 = r14.mNeedsToConvertPhoneticString;
        if (r12 == 0) goto L_0x0078;
    L_0x0016:
        r4 = com.android.vcard.VCardUtils.toHalfWidthString(r9);
        r6 = com.android.vcard.VCardUtils.toHalfWidthString(r11);
        r5 = com.android.vcard.VCardUtils.toHalfWidthString(r10);
    L_0x0022:
        r12 = android.text.TextUtils.isEmpty(r4);
        if (r12 == 0) goto L_0x007c;
    L_0x0028:
        r12 = android.text.TextUtils.isEmpty(r6);
        if (r12 == 0) goto L_0x007c;
    L_0x002e:
        r12 = android.text.TextUtils.isEmpty(r5);
        if (r12 == 0) goto L_0x007c;
    L_0x0034:
        r12 = r14.mIsDoCoMo;
        if (r12 == 0) goto L_0x0077;
    L_0x0038:
        r12 = r14.mBuilder;
        r13 = "SOUND";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "X-IRMC-N";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
    L_0x0077:
        return;
    L_0x0078:
        r4 = r9;
        r6 = r11;
        r5 = r10;
        goto L_0x0022;
    L_0x007c:
        r12 = r14.mVCardType;
        r12 = com.android.vcard.VCardConfig.isVersion40(r12);
        if (r12 != 0) goto L_0x00d2;
    L_0x0084:
        r12 = r14.mVCardType;
        r12 = com.android.vcard.VCardConfig.isVersion30(r12);
        if (r12 == 0) goto L_0x0201;
    L_0x008c:
        r12 = r14.mVCardType;
        r8 = com.android.vcard.VCardUtils.constructNameFromElements(r12, r4, r6, r5);
        r12 = r14.mBuilder;
        r13 = "SORT-STRING";
        r12.append(r13);
        r12 = r14.mVCardType;
        r12 = com.android.vcard.VCardConfig.isVersion30(r12);
        if (r12 == 0) goto L_0x00bb;
    L_0x00a1:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r8;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x00bb;
    L_0x00ad:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x00bb:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.escapeCharacters(r8);
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
    L_0x00d2:
        r12 = r14.mUsesDefactProperty;
        if (r12 == 0) goto L_0x0077;
    L_0x00d6:
        r12 = com.android.vcard.VCardConfig.isJapanSpacialized();
        if (r12 != 0) goto L_0x0077;
    L_0x00dc:
        r12 = android.text.TextUtils.isEmpty(r5);
        if (r12 != 0) goto L_0x013d;
    L_0x00e2:
        r12 = r14.mShouldUseQuotedPrintable;
        if (r12 == 0) goto L_0x035c;
    L_0x00e6:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r5;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 != 0) goto L_0x035c;
    L_0x00f2:
        r7 = 1;
    L_0x00f3:
        if (r7 == 0) goto L_0x035f;
    L_0x00f5:
        r1 = r14.encodeQuotedPrintable(r5);
    L_0x00f9:
        r12 = r14.mBuilder;
        r13 = "X-PHONETIC-FIRST-NAME";
        r12.append(r13);
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r5;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x011a;
    L_0x010c:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x011a:
        if (r7 == 0) goto L_0x012a;
    L_0x011c:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "ENCODING=QUOTED-PRINTABLE";
        r12.append(r13);
    L_0x012a:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = r14.mBuilder;
        r12.append(r1);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
    L_0x013d:
        r12 = android.text.TextUtils.isEmpty(r6);
        if (r12 != 0) goto L_0x019e;
    L_0x0143:
        r12 = r14.mShouldUseQuotedPrintable;
        if (r12 == 0) goto L_0x0365;
    L_0x0147:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r6;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 != 0) goto L_0x0365;
    L_0x0153:
        r7 = 1;
    L_0x0154:
        if (r7 == 0) goto L_0x0368;
    L_0x0156:
        r2 = r14.encodeQuotedPrintable(r6);
    L_0x015a:
        r12 = r14.mBuilder;
        r13 = "X-PHONETIC-MIDDLE-NAME";
        r12.append(r13);
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r6;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x017b;
    L_0x016d:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x017b:
        if (r7 == 0) goto L_0x018b;
    L_0x017d:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "ENCODING=QUOTED-PRINTABLE";
        r12.append(r13);
    L_0x018b:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = r14.mBuilder;
        r12.append(r2);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
    L_0x019e:
        r12 = android.text.TextUtils.isEmpty(r4);
        if (r12 != 0) goto L_0x0077;
    L_0x01a4:
        r12 = r14.mShouldUseQuotedPrintable;
        if (r12 == 0) goto L_0x036e;
    L_0x01a8:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r4;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 != 0) goto L_0x036e;
    L_0x01b4:
        r7 = 1;
    L_0x01b5:
        if (r7 == 0) goto L_0x0371;
    L_0x01b7:
        r0 = r14.encodeQuotedPrintable(r4);
    L_0x01bb:
        r12 = r14.mBuilder;
        r13 = "X-PHONETIC-LAST-NAME";
        r12.append(r13);
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r4;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x01dc;
    L_0x01ce:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x01dc:
        if (r7 == 0) goto L_0x01ec;
    L_0x01de:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "ENCODING=QUOTED-PRINTABLE";
        r12.append(r13);
    L_0x01ec:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = r14.mBuilder;
        r12.append(r0);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
        goto L_0x0077;
    L_0x0201:
        r12 = r14.mIsJapaneseMobilePhone;
        if (r12 == 0) goto L_0x00d2;
    L_0x0205:
        r12 = r14.mBuilder;
        r13 = "SOUND";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "X-IRMC-N";
        r12.append(r13);
        r12 = r14.mRefrainsQPToNameProperties;
        if (r12 != 0) goto L_0x02c8;
    L_0x021e:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r4;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 == 0) goto L_0x0242;
    L_0x022a:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r6;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 == 0) goto L_0x0242;
    L_0x0236:
        r12 = 1;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r5;
        r12 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r12);
        if (r12 != 0) goto L_0x02c8;
    L_0x0242:
        r7 = 1;
    L_0x0243:
        if (r7 == 0) goto L_0x02cb;
    L_0x0245:
        r0 = r14.encodeQuotedPrintable(r4);
        r2 = r14.encodeQuotedPrintable(r6);
        r1 = r14.encodeQuotedPrintable(r5);
    L_0x0251:
        r12 = r14.mIsDoCoMo;
        if (r12 == 0) goto L_0x02d9;
    L_0x0255:
        r12 = com.android.vcard.VCardConfig.isShiftJisAsDefault();
        if (r12 != 0) goto L_0x02d9;
    L_0x025b:
        r12 = 3;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r0;
        r13 = 1;
        r12[r13] = r2;
        r13 = 2;
        r12[r13] = r1;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x027b;
    L_0x026d:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x027b:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r12 = android.text.TextUtils.isEmpty(r0);
        if (r12 != 0) goto L_0x028d;
    L_0x0288:
        r12 = r14.mBuilder;
        r12.append(r0);
    L_0x028d:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = android.text.TextUtils.isEmpty(r1);
        if (r12 != 0) goto L_0x029f;
    L_0x029a:
        r12 = r14.mBuilder;
        r12.append(r1);
    L_0x029f:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = android.text.TextUtils.isEmpty(r2);
        if (r12 != 0) goto L_0x02b1;
    L_0x02ac:
        r12 = r14.mBuilder;
        r12.append(r2);
    L_0x02b1:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
        goto L_0x00d2;
    L_0x02c8:
        r7 = 0;
        goto L_0x0243;
    L_0x02cb:
        r0 = r14.escapeCharacters(r4);
        r2 = r14.escapeCharacters(r6);
        r1 = r14.escapeCharacters(r5);
        goto L_0x0251;
    L_0x02d9:
        r12 = 3;
        r12 = new java.lang.String[r12];
        r13 = 0;
        r12[r13] = r0;
        r13 = 1;
        r12[r13] = r2;
        r13 = 2;
        r12[r13] = r1;
        r12 = r14.shouldAppendCharsetParam(r12);
        if (r12 == 0) goto L_0x02f9;
    L_0x02eb:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = r14.mVCardCharsetParameter;
        r12.append(r13);
    L_0x02f9:
        r12 = r14.mBuilder;
        r13 = ":";
        r12.append(r13);
        r3 = 1;
        r12 = android.text.TextUtils.isEmpty(r0);
        if (r12 != 0) goto L_0x030d;
    L_0x0307:
        r12 = r14.mBuilder;
        r12.append(r0);
        r3 = 0;
    L_0x030d:
        r12 = android.text.TextUtils.isEmpty(r2);
        if (r12 != 0) goto L_0x031b;
    L_0x0313:
        if (r3 == 0) goto L_0x0354;
    L_0x0315:
        r3 = 0;
    L_0x0316:
        r12 = r14.mBuilder;
        r12.append(r2);
    L_0x031b:
        r12 = android.text.TextUtils.isEmpty(r1);
        if (r12 != 0) goto L_0x032f;
    L_0x0321:
        if (r3 != 0) goto L_0x032a;
    L_0x0323:
        r12 = r14.mBuilder;
        r13 = 32;
        r12.append(r13);
    L_0x032a:
        r12 = r14.mBuilder;
        r12.append(r1);
    L_0x032f:
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = ";";
        r12.append(r13);
        r12 = r14.mBuilder;
        r13 = "\r\n";
        r12.append(r13);
        goto L_0x00d2;
    L_0x0354:
        r12 = r14.mBuilder;
        r13 = 32;
        r12.append(r13);
        goto L_0x0316;
    L_0x035c:
        r7 = 0;
        goto L_0x00f3;
    L_0x035f:
        r1 = r14.escapeCharacters(r5);
        goto L_0x00f9;
    L_0x0365:
        r7 = 0;
        goto L_0x0154;
    L_0x0368:
        r2 = r14.escapeCharacters(r6);
        goto L_0x015a;
    L_0x036e:
        r7 = 0;
        goto L_0x01b5;
    L_0x0371:
        r0 = r14.escapeCharacters(r4);
        goto L_0x01bb;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendPhoneticNameFields(android.content.ContentValues):void");
    }

    public VCardBuilder appendNickNames(List<ContentValues> contentValuesList) {
        boolean useAndroidProperty;
        if (this.mIsV30OrV40 || this.mIsDoCoMo) {
            useAndroidProperty = false;
        } else {
            if (this.mUsesAndroidProperty) {
                useAndroidProperty = true;
            }
            return this;
        }
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                String nickname = contentValues.getAsString("data1");
                if (!TextUtils.isEmpty(nickname)) {
                    if (useAndroidProperty) {
                        appendAndroidSpecificProperty("vnd.android.cursor.item/nickname", contentValues);
                    } else {
                        appendLineWithCharsetAndQPDetection(VCardConstants.PROPERTY_NICKNAME, nickname);
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendPhones(List<ContentValues> contentValuesList, VCardPhoneNumberTranslationCallback translationCallback) {
        boolean phoneLineExists = false;
        if (contentValuesList != null) {
            Set<String> phoneSet = new HashSet();
            for (ContentValues contentValues : contentValuesList) {
                Integer typeAsObject = contentValues.getAsInteger("data2");
                String label = contentValues.getAsString("data3");
                Integer isPrimaryAsInteger = contentValues.getAsInteger("is_primary");
                boolean isPrimary = isPrimaryAsInteger != null ? isPrimaryAsInteger.intValue() > 0 : false;
                String phoneNumber = contentValues.getAsString("data1");
                if (phoneNumber != null) {
                    phoneNumber = phoneNumber.trim();
                }
                if (!TextUtils.isEmpty(phoneNumber)) {
                    int type = typeAsObject != null ? typeAsObject.intValue() : 1;
                    if (translationCallback != null) {
                        phoneNumber = translationCallback.onValueReceived(phoneNumber, type, label, isPrimary);
                        if (!phoneSet.contains(phoneNumber)) {
                            phoneSet.add(phoneNumber);
                            appendTelLine(Integer.valueOf(type), label, phoneNumber, isPrimary);
                        }
                    } else if (this.mIsDoCoMo || type == 6 || VCardConfig.refrainPhoneNumberFormatting(this.mVCardType)) {
                        phoneLineExists = true;
                        if (!phoneSet.contains(phoneNumber)) {
                            phoneSet.add(phoneNumber);
                            appendTelLine(Integer.valueOf(type), label, phoneNumber, isPrimary);
                        }
                    } else {
                        List<String> phoneNumberList = splitPhoneNumbers(phoneNumber);
                        if (!phoneNumberList.isEmpty()) {
                            phoneLineExists = true;
                            for (String actualPhoneNumber : phoneNumberList) {
                                if (!phoneSet.contains(actualPhoneNumber)) {
                                    String formatted;
                                    String numberWithControlSequence = actualPhoneNumber.replace(',', 'p').replace(';', 'w');
                                    if (TextUtils.equals(numberWithControlSequence, actualPhoneNumber)) {
                                        StringBuilder digitsOnlyBuilder = new StringBuilder();
                                        int length = actualPhoneNumber.length();
                                        for (int i = 0; i < length; i++) {
                                            char ch = actualPhoneNumber.charAt(i);
                                            if (Character.isDigit(ch) || ch == '+' || ch == '*' || ch == '#') {
                                                digitsOnlyBuilder.append(ch);
                                            }
                                        }
                                        formatted = digitsOnlyBuilder.toString();
                                    } else {
                                        formatted = numberWithControlSequence;
                                    }
                                    if (!(!VCardConfig.isVersion40(this.mVCardType) || TextUtils.isEmpty(formatted) || formatted.startsWith("tel:"))) {
                                        formatted = "tel:" + formatted;
                                    }
                                    phoneSet.add(actualPhoneNumber);
                                    appendTelLine(Integer.valueOf(type), label, formatted, isPrimary);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!phoneLineExists && this.mIsDoCoMo) {
            appendTelLine(Integer.valueOf(1), "", "", false);
        }
        return this;
    }

    private List<String> splitPhoneNumbers(String phoneNumber) {
        List<String> phoneList = new ArrayList();
        StringBuilder builder = new StringBuilder();
        int length = phoneNumber.length();
        for (int i = 0; i < length; i++) {
            char ch = phoneNumber.charAt(i);
            if (ch != '\n' || builder.length() <= 0) {
                builder.append(ch);
            } else {
                phoneList.add(builder.toString());
                builder = new StringBuilder();
            }
        }
        if (builder.length() > 0) {
            phoneList.add(builder.toString());
        }
        return phoneList;
    }

    public VCardBuilder appendEmails(List<ContentValues> contentValuesList) {
        boolean emailAddressExists = false;
        if (contentValuesList != null) {
            Set<String> addressSet = new HashSet();
            for (ContentValues contentValues : contentValuesList) {
                String emailAddress = contentValues.getAsString("data1");
                if (emailAddress != null) {
                    emailAddress = emailAddress.trim();
                }
                if (!TextUtils.isEmpty(emailAddress)) {
                    Integer typeAsObject = contentValues.getAsInteger("data2");
                    int type = typeAsObject != null ? typeAsObject.intValue() : 3;
                    String label = contentValues.getAsString("data3");
                    Integer isPrimaryAsInteger = contentValues.getAsInteger("is_primary");
                    boolean isPrimary = isPrimaryAsInteger != null ? isPrimaryAsInteger.intValue() > 0 : false;
                    emailAddressExists = true;
                    if (!addressSet.contains(emailAddress)) {
                        addressSet.add(emailAddress);
                        appendEmailLine(type, label, emailAddress, isPrimary);
                    }
                }
            }
        }
        if (!emailAddressExists && this.mIsDoCoMo) {
            appendEmailLine(1, "", "", false);
        }
        return this;
    }

    public VCardBuilder appendPostals(List<ContentValues> contentValuesList) {
        if (contentValuesList == null || contentValuesList.isEmpty()) {
            if (this.mIsDoCoMo) {
                this.mBuilder.append(VCardConstants.PROPERTY_ADR);
                this.mBuilder.append(";");
                this.mBuilder.append("HOME");
                this.mBuilder.append(VCARD_DATA_SEPARATOR);
                this.mBuilder.append(VCARD_END_OF_LINE);
            }
        } else if (this.mIsDoCoMo) {
            appendPostalsForDoCoMo(contentValuesList);
        } else {
            appendPostalsForGeneric(contentValuesList);
        }
        return this;
    }

    private void appendPostalsForDoCoMo(List<ContentValues> contentValuesList) {
        for (ContentValues contentValues : contentValuesList) {
            if (contentValues != null) {
                int currentType;
                Integer typeAsInteger = contentValues.getAsInteger("data2");
                if (typeAsInteger != null) {
                    currentType = typeAsInteger.intValue();
                } else {
                    currentType = 1;
                }
                appendPostalLine(currentType, contentValues.getAsString("data3"), contentValues, false, true);
            }
        }
    }

    private void appendPostalsForGeneric(List<ContentValues> contentValuesList) {
        for (ContentValues contentValues : contentValuesList) {
            if (contentValues != null) {
                int type;
                Integer typeAsInteger = contentValues.getAsInteger("data2");
                if (typeAsInteger != null) {
                    type = typeAsInteger.intValue();
                } else {
                    type = 1;
                }
                String label = contentValues.getAsString("data3");
                Integer isPrimaryAsInteger = contentValues.getAsInteger("is_primary");
                boolean isPrimary = isPrimaryAsInteger != null ? isPrimaryAsInteger.intValue() > 0 : false;
                appendPostalLine(type, label, contentValues, isPrimary, false);
            }
        }
    }

    private PostalStruct tryConstructPostalStruct(ContentValues contentValues) {
        String rawPoBox = contentValues.getAsString("data5");
        String rawNeighborhood = contentValues.getAsString("data6");
        String rawStreet = contentValues.getAsString("data4");
        String rawLocality = contentValues.getAsString("data7");
        String rawRegion = contentValues.getAsString("data8");
        String rawPostalCode = contentValues.getAsString("data9");
        String rawCountry = contentValues.getAsString("data10");
        String[] rawAddressArray = new String[]{rawPoBox, rawNeighborhood, rawStreet, rawLocality, rawRegion, rawPostalCode, rawCountry};
        boolean reallyUseQuotedPrintable;
        boolean appendCharset;
        StringBuilder addressBuilder;
        if (VCardUtils.areAllEmpty(rawAddressArray)) {
            String rawFormattedAddress = contentValues.getAsString("data1");
            if (TextUtils.isEmpty(rawFormattedAddress)) {
                return null;
            }
            String encodedFormattedAddress;
            if (this.mShouldUseQuotedPrintable) {
                if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(rawFormattedAddress)) {
                    reallyUseQuotedPrintable = true;
                    appendCharset = VCardUtils.containsOnlyPrintableAscii(rawFormattedAddress);
                    if (reallyUseQuotedPrintable) {
                        encodedFormattedAddress = escapeCharacters(rawFormattedAddress);
                    } else {
                        encodedFormattedAddress = encodeQuotedPrintable(rawFormattedAddress);
                    }
                    addressBuilder = new StringBuilder();
                    addressBuilder.append(";");
                    addressBuilder.append(encodedFormattedAddress);
                    addressBuilder.append(";");
                    addressBuilder.append(";");
                    addressBuilder.append(";");
                    addressBuilder.append(";");
                    addressBuilder.append(";");
                    return new PostalStruct(reallyUseQuotedPrintable, appendCharset, addressBuilder.toString());
                }
            }
            reallyUseQuotedPrintable = false;
            if (VCardUtils.containsOnlyPrintableAscii(rawFormattedAddress)) {
            }
            if (reallyUseQuotedPrintable) {
                encodedFormattedAddress = escapeCharacters(rawFormattedAddress);
            } else {
                encodedFormattedAddress = encodeQuotedPrintable(rawFormattedAddress);
            }
            addressBuilder = new StringBuilder();
            addressBuilder.append(";");
            addressBuilder.append(encodedFormattedAddress);
            addressBuilder.append(";");
            addressBuilder.append(";");
            addressBuilder.append(";");
            addressBuilder.append(";");
            addressBuilder.append(";");
            return new PostalStruct(reallyUseQuotedPrintable, appendCharset, addressBuilder.toString());
        }
        String rawLocality2;
        String encodedPoBox;
        String encodedStreet;
        String encodedLocality;
        String encodedRegion;
        String encodedPostalCode;
        String encodedCountry;
        String encodedNeighborhood;
        reallyUseQuotedPrintable = this.mShouldUseQuotedPrintable && !VCardUtils.containsOnlyNonCrLfPrintableAscii(rawAddressArray);
        if (VCardUtils.containsOnlyPrintableAscii(rawAddressArray)) {
            appendCharset = false;
        } else {
            appendCharset = true;
        }
        if (TextUtils.isEmpty(rawLocality)) {
            if (TextUtils.isEmpty(rawNeighborhood)) {
                rawLocality2 = "";
            } else {
                rawLocality2 = rawNeighborhood;
            }
        } else if (TextUtils.isEmpty(rawNeighborhood)) {
            rawLocality2 = rawLocality;
        } else {
            rawLocality2 = new StringBuilder(String.valueOf(rawLocality)).append(VCARD_WS).append(rawNeighborhood).toString();
        }
        if (reallyUseQuotedPrintable) {
            if (this.mIsDoCoMo) {
                encodedPoBox = encodeQuotedPrintable(rawPoBox);
                encodedStreet = encodeQuotedPrintable(rawStreet);
                encodedLocality = encodeQuotedPrintable(rawLocality);
                encodedRegion = encodeQuotedPrintable(rawRegion);
                encodedPostalCode = encodeQuotedPrintable(rawPostalCode);
                encodedCountry = encodeQuotedPrintable(rawCountry);
                encodedNeighborhood = encodeQuotedPrintable(rawNeighborhood);
            } else {
                encodedPoBox = encodeQuotedPrintable(rawPoBox);
                encodedStreet = encodeQuotedPrintable(rawStreet);
                encodedLocality = encodeQuotedPrintable(rawLocality2);
                encodedRegion = encodeQuotedPrintable(rawRegion);
                encodedPostalCode = encodeQuotedPrintable(rawPostalCode);
                encodedCountry = encodeQuotedPrintable(rawCountry);
                encodedNeighborhood = encodeQuotedPrintable(rawNeighborhood);
            }
        } else if (this.mIsDoCoMo) {
            encodedPoBox = escapeCharacters(rawPoBox);
            encodedStreet = escapeCharacters(rawStreet);
            encodedLocality = escapeCharacters(rawLocality);
            encodedRegion = escapeCharacters(rawRegion);
            encodedPostalCode = escapeCharacters(rawPostalCode);
            encodedCountry = escapeCharacters(rawCountry);
            encodedNeighborhood = escapeCharacters(rawNeighborhood);
        } else {
            encodedPoBox = escapeCharacters(rawPoBox);
            encodedStreet = escapeCharacters(rawStreet);
            encodedLocality = escapeCharacters(rawLocality2);
            encodedRegion = escapeCharacters(rawRegion);
            encodedPostalCode = escapeCharacters(rawPostalCode);
            encodedCountry = escapeCharacters(rawCountry);
            encodedNeighborhood = escapeCharacters(rawNeighborhood);
        }
        addressBuilder = new StringBuilder();
        addressBuilder.append(encodedPoBox);
        addressBuilder.append(";");
        if (this.mIsDoCoMo) {
            addressBuilder.append(encodedNeighborhood);
        }
        addressBuilder.append(";");
        addressBuilder.append(encodedStreet);
        addressBuilder.append(";");
        addressBuilder.append(encodedLocality);
        addressBuilder.append(";");
        addressBuilder.append(encodedRegion);
        addressBuilder.append(";");
        addressBuilder.append(encodedPostalCode);
        addressBuilder.append(";");
        addressBuilder.append(encodedCountry);
        return new PostalStruct(reallyUseQuotedPrintable, appendCharset, addressBuilder.toString());
    }

    public VCardBuilder appendIms(List<ContentValues> contentValuesList) {
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                Integer protocolAsObject = contentValues.getAsInteger("data5");
                if (protocolAsObject != null) {
                    if (this.mIsDoCoMo && protocolAsObject.intValue() == -1) {
                        appendAndroidSpecificProperty("vnd.android.cursor.item/im", contentValues);
                    } else {
                        String propertyName;
                        if (protocolAsObject.intValue() == -1) {
                            propertyName = contentValues.getAsString("data6");
                            if (propertyName != null) {
                                propertyName = propertyName.trim();
                            }
                            if (!VCardUtils.containsOnlyPrintableAscii(propertyName) && this.mShouldUseQuotedPrintable) {
                                if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(propertyName)) {
                                    propertyName = "X-CUSTOM(CHARSET=UTF-8,ENCODING=QUOTED-PRINTABLE," + encodeQuotedPrintable(propertyName) + ")";
                                }
                            }
                            propertyName = "X-CUSTOM(CHARSET=UTF-8,ENCODING=QUOTED-PRINTABLE," + propertyName + ")";
                        } else {
                            propertyName = VCardUtils.getPropertyNameForIm(protocolAsObject.intValue());
                        }
                        if (propertyName != null) {
                            String data = contentValues.getAsString("data1");
                            if (data != null) {
                                data = data.trim();
                            }
                            if (!TextUtils.isEmpty(data)) {
                                int intValue;
                                String typeAsString;
                                Integer typeAsInteger = contentValues.getAsInteger("data2");
                                if (typeAsInteger != null) {
                                    intValue = typeAsInteger.intValue();
                                } else {
                                    intValue = 3;
                                }
                                switch (intValue) {
                                    case 0:
                                        String label = contentValues.getAsString("data3");
                                        typeAsString = label != null ? "X-" + label : null;
                                        break;
                                    case 1:
                                        typeAsString = "HOME";
                                        break;
                                    case 2:
                                        typeAsString = VCardConstants.PARAM_TYPE_WORK;
                                        break;
                                    default:
                                        typeAsString = null;
                                        break;
                                }
                                List parameterList = new ArrayList();
                                if (!TextUtils.isEmpty(typeAsString)) {
                                    parameterList.add(typeAsString);
                                }
                                Integer isPrimaryAsInteger = contentValues.getAsInteger("is_primary");
                                boolean isPrimary = isPrimaryAsInteger != null ? isPrimaryAsInteger.intValue() > 0 : false;
                                if (isPrimary) {
                                    parameterList.add(VCardConstants.PARAM_TYPE_PREF);
                                }
                                appendLineWithCharsetAndQPDetection(propertyName, parameterList, data);
                            }
                        }
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendWebsites(List<ContentValues> contentValuesList) {
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                String website = contentValues.getAsString("data1");
                if (website != null) {
                    website = website.trim();
                }
                if (!TextUtils.isEmpty(website)) {
                    appendLineWithCharsetAndQPDetection(VCardConstants.PROPERTY_URL, website);
                }
            }
        }
        return this;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.android.vcard.VCardBuilder appendOrganizations(java.util.List<android.content.ContentValues> r38) {
        /*
        r37 = this;
        if (r38 == 0) goto L_0x000c;
    L_0x0002:
        r34 = r38.iterator();
    L_0x0006:
        r32 = r34.hasNext();
        if (r32 != 0) goto L_0x000d;
    L_0x000c:
        return r37;
    L_0x000d:
        r7 = r34.next();
        r7 = (android.content.ContentValues) r7;
        r32 = "data1";
        r0 = r32;
        r6 = r7.getAsString(r0);
        if (r6 == 0) goto L_0x0021;
    L_0x001d:
        r6 = r6.trim();
    L_0x0021:
        r32 = "data5";
        r0 = r32;
        r8 = r7.getAsString(r0);
        if (r8 == 0) goto L_0x002f;
    L_0x002b:
        r8 = r8.trim();
    L_0x002f:
        r32 = "data4";
        r0 = r32;
        r29 = r7.getAsString(r0);
        if (r29 == 0) goto L_0x003d;
    L_0x0039:
        r29 = r29.trim();
    L_0x003d:
        r0 = r37;
        r0 = r0.mIsDoCoMo;
        r32 = r0;
        if (r32 == 0) goto L_0x0314;
    L_0x0045:
        r32 = "data6";
        r0 = r32;
        r18 = r7.getAsString(r0);
        if (r18 == 0) goto L_0x0053;
    L_0x004f:
        r18 = r18.trim();
    L_0x0053:
        r32 = "data7";
        r0 = r32;
        r28 = r7.getAsString(r0);
        if (r28 == 0) goto L_0x0061;
    L_0x005d:
        r28 = r28.trim();
    L_0x0061:
        r32 = "data8";
        r0 = r32;
        r25 = r7.getAsString(r0);
        if (r25 == 0) goto L_0x006f;
    L_0x006b:
        r25 = r25.trim();
    L_0x006f:
        r32 = "data9";
        r0 = r32;
        r20 = r7.getAsString(r0);
        if (r20 == 0) goto L_0x007d;
    L_0x0079:
        r20 = r20.trim();
    L_0x007d:
        r32 = 6;
        r0 = r32;
        r0 = new java.lang.String[r0];
        r26 = r0;
        r32 = 0;
        r26[r32] = r6;
        r32 = 1;
        r26[r32] = r8;
        r32 = 2;
        r26[r32] = r18;
        r32 = 3;
        r26[r32] = r28;
        r32 = 4;
        r26[r32] = r25;
        r32 = 5;
        r26[r32] = r20;
        r32 = "data2";
        r0 = r32;
        r31 = r7.getAsInteger(r0);
        r32 = "data3";
        r0 = r32;
        r19 = r7.getAsString(r0);
        r32 = "is_primary";
        r0 = r32;
        r17 = r7.getAsInteger(r0);
        if (r17 == 0) goto L_0x0265;
    L_0x00b7:
        r32 = r17.intValue();
        if (r32 <= 0) goto L_0x0261;
    L_0x00bd:
        r16 = 1;
    L_0x00bf:
        if (r31 == 0) goto L_0x0269;
    L_0x00c1:
        r30 = r31.intValue();
    L_0x00c5:
        r24 = new java.util.ArrayList;
        r24.<init>();
        if (r16 == 0) goto L_0x00d5;
    L_0x00cc:
        r32 = "PREF";
        r0 = r24;
        r1 = r32;
        r0.add(r1);
    L_0x00d5:
        switch(r30) {
            case 0: goto L_0x0278;
            case 1: goto L_0x026d;
            case 2: goto L_0x02b4;
            default: goto L_0x00d8;
        };
    L_0x00d8:
        r32 = "vCard";
        r33 = new java.lang.StringBuilder;
        r35 = "Unknown Organizationl type: ";
        r0 = r33;
        r1 = r35;
        r0.<init>(r1);
        r0 = r33;
        r1 = r30;
        r33 = r0.append(r1);
        r33 = r33.toString();
        android.util.Log.e(r32, r33);
    L_0x00f4:
        r32 = com.android.vcard.VCardUtils.areAllEmpty(r26);
        if (r32 != 0) goto L_0x02fc;
    L_0x00fa:
        r0 = r37;
        r0 = r0.mShouldUseQuotedPrintable;
        r32 = r0;
        if (r32 == 0) goto L_0x02bf;
    L_0x0102:
        r32 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r26);
        if (r32 != 0) goto L_0x02bf;
    L_0x0108:
        r27 = 1;
    L_0x010a:
        r32 = com.android.vcard.VCardUtils.containsOnlyPrintableAscii(r26);
        if (r32 == 0) goto L_0x02c3;
    L_0x0110:
        r5 = 0;
    L_0x0111:
        if (r27 == 0) goto L_0x02c6;
    L_0x0113:
        r0 = r37;
        r9 = r0.encodeQuotedPrintable(r6);
        r0 = r37;
        r10 = r0.encodeQuotedPrintable(r8);
        r0 = r37;
        r1 = r29;
        r15 = r0.encodeQuotedPrintable(r1);
        r0 = r37;
        r1 = r18;
        r11 = r0.encodeQuotedPrintable(r1);
        r0 = r37;
        r1 = r28;
        r14 = r0.encodeQuotedPrintable(r1);
        r0 = r37;
        r1 = r25;
        r13 = r0.encodeQuotedPrintable(r1);
        r0 = r37;
        r1 = r20;
        r12 = r0.encodeQuotedPrintable(r1);
    L_0x0147:
        r22 = new java.lang.StringBuilder;
        r22.<init>();
        r0 = r22;
        r0.append(r9);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r0 = r22;
        r0.append(r10);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r0 = r22;
        r0.append(r11);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r0 = r22;
        r0.append(r14);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r0 = r22;
        r0.append(r13);
        r32 = ";";
        r0 = r22;
        r1 = r32;
        r0.append(r1);
        r0 = r22;
        r0.append(r12);
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = "ORG";
        r32.append(r33);
        r32 = r24.isEmpty();
        if (r32 != 0) goto L_0x01c3;
    L_0x01b1:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = ";";
        r32.append(r33);
        r0 = r37;
        r1 = r24;
        r0.appendTypeParameters(r1);
    L_0x01c3:
        if (r5 == 0) goto L_0x01df;
    L_0x01c5:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = ";";
        r32.append(r33);
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r0 = r37;
        r0 = r0.mVCardCharsetParameter;
        r33 = r0;
        r32.append(r33);
    L_0x01df:
        if (r27 == 0) goto L_0x01f7;
    L_0x01e1:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = ";";
        r32.append(r33);
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = "ENCODING=QUOTED-PRINTABLE";
        r32.append(r33);
    L_0x01f7:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = ":";
        r32.append(r33);
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r0 = r32;
        r1 = r22;
        r0.append(r1);
    L_0x020f:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = "\r\n";
        r32.append(r33);
    L_0x021a:
        r32 = android.text.TextUtils.isEmpty(r29);
        if (r32 != 0) goto L_0x0006;
    L_0x0220:
        r35 = "TITLE";
        r32 = 1;
        r0 = r32;
        r0 = new java.lang.String[r0];
        r32 = r0;
        r33 = 0;
        r32[r33] = r29;
        r32 = com.android.vcard.VCardUtils.containsOnlyPrintableAscii(r32);
        if (r32 == 0) goto L_0x0389;
    L_0x0234:
        r32 = 0;
    L_0x0236:
        r0 = r37;
        r0 = r0.mShouldUseQuotedPrintable;
        r33 = r0;
        if (r33 == 0) goto L_0x038d;
    L_0x023e:
        r33 = 1;
        r0 = r33;
        r0 = new java.lang.String[r0];
        r33 = r0;
        r36 = 0;
        r33[r36] = r29;
        r33 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r33);
        if (r33 != 0) goto L_0x038d;
    L_0x0250:
        r33 = 1;
    L_0x0252:
        r0 = r37;
        r1 = r35;
        r2 = r29;
        r3 = r32;
        r4 = r33;
        r0.appendLine(r1, r2, r3, r4);
        goto L_0x0006;
    L_0x0261:
        r16 = 0;
        goto L_0x00bf;
    L_0x0265:
        r16 = 0;
        goto L_0x00bf;
    L_0x0269:
        r30 = 1;
        goto L_0x00c5;
    L_0x026d:
        r32 = "WORK";
        r0 = r24;
        r1 = r32;
        r0.add(r1);
        goto L_0x00f4;
    L_0x0278:
        r32 = android.text.TextUtils.isEmpty(r19);
        if (r32 != 0) goto L_0x00f4;
    L_0x027e:
        r0 = r37;
        r0 = r0.mIsDoCoMo;
        r32 = r0;
        if (r32 != 0) goto L_0x0298;
    L_0x0286:
        r32 = 1;
        r0 = r32;
        r0 = new java.lang.String[r0];
        r32 = r0;
        r33 = 0;
        r32[r33] = r19;
        r32 = com.android.vcard.VCardUtils.containsOnlyAlphaDigitHyphen(r32);
        if (r32 == 0) goto L_0x00f4;
    L_0x0298:
        r32 = new java.lang.StringBuilder;
        r33 = "X-";
        r32.<init>(r33);
        r0 = r32;
        r1 = r19;
        r32 = r0.append(r1);
        r32 = r32.toString();
        r0 = r24;
        r1 = r32;
        r0.add(r1);
        goto L_0x00f4;
    L_0x02b4:
        r32 = "OTHER";
        r0 = r24;
        r1 = r32;
        r0.add(r1);
        goto L_0x00f4;
    L_0x02bf:
        r27 = 0;
        goto L_0x010a;
    L_0x02c3:
        r5 = 1;
        goto L_0x0111;
    L_0x02c6:
        r0 = r37;
        r9 = r0.escapeCharacters(r6);
        r0 = r37;
        r10 = r0.escapeCharacters(r8);
        r0 = r37;
        r1 = r29;
        r15 = r0.escapeCharacters(r1);
        r0 = r37;
        r1 = r18;
        r11 = r0.escapeCharacters(r1);
        r0 = r37;
        r1 = r28;
        r14 = r0.escapeCharacters(r1);
        r0 = r37;
        r1 = r25;
        r13 = r0.escapeCharacters(r1);
        r0 = r37;
        r1 = r20;
        r12 = r0.escapeCharacters(r1);
        goto L_0x0147;
    L_0x02fc:
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = "ORG";
        r32.append(r33);
        r0 = r37;
        r0 = r0.mBuilder;
        r32 = r0;
        r33 = ":";
        r32.append(r33);
        goto L_0x020f;
    L_0x0314:
        r21 = new java.lang.StringBuilder;
        r21.<init>();
        r32 = android.text.TextUtils.isEmpty(r6);
        if (r32 != 0) goto L_0x0324;
    L_0x031f:
        r0 = r21;
        r0.append(r6);
    L_0x0324:
        r32 = android.text.TextUtils.isEmpty(r8);
        if (r32 != 0) goto L_0x033e;
    L_0x032a:
        r32 = r21.length();
        if (r32 <= 0) goto L_0x0339;
    L_0x0330:
        r32 = 59;
        r0 = r21;
        r1 = r32;
        r0.append(r1);
    L_0x0339:
        r0 = r21;
        r0.append(r8);
    L_0x033e:
        r23 = r21.toString();
        r35 = "ORG";
        r32 = 1;
        r0 = r32;
        r0 = new java.lang.String[r0];
        r32 = r0;
        r33 = 0;
        r32[r33] = r23;
        r32 = com.android.vcard.VCardUtils.containsOnlyPrintableAscii(r32);
        if (r32 == 0) goto L_0x0383;
    L_0x0356:
        r32 = 0;
    L_0x0358:
        r0 = r37;
        r0 = r0.mShouldUseQuotedPrintable;
        r33 = r0;
        if (r33 == 0) goto L_0x0386;
    L_0x0360:
        r33 = 1;
        r0 = r33;
        r0 = new java.lang.String[r0];
        r33 = r0;
        r36 = 0;
        r33[r36] = r23;
        r33 = com.android.vcard.VCardUtils.containsOnlyNonCrLfPrintableAscii(r33);
        if (r33 != 0) goto L_0x0386;
    L_0x0372:
        r33 = 1;
    L_0x0374:
        r0 = r37;
        r1 = r35;
        r2 = r23;
        r3 = r32;
        r4 = r33;
        r0.appendLine(r1, r2, r3, r4);
        goto L_0x021a;
    L_0x0383:
        r32 = 1;
        goto L_0x0358;
    L_0x0386:
        r33 = 0;
        goto L_0x0374;
    L_0x0389:
        r32 = 1;
        goto L_0x0236;
    L_0x038d:
        r33 = 0;
        goto L_0x0252;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendOrganizations(java.util.List):com.android.vcard.VCardBuilder");
    }

    public VCardBuilder appendPhotos(List<ContentValues> contentValuesList) {
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                if (contentValues != null) {
                    byte[] data = contentValues.getAsByteArray("data15");
                    if (data != null) {
                        String photoType = VCardUtils.guessImageType(data);
                        if (photoType == null) {
                            Log.d(LOG_TAG, "Unknown photo type. Ignored.");
                        } else {
                            String photoString = new String(Base64.encode(data, 2));
                            if (!TextUtils.isEmpty(photoString)) {
                                appendPhotoLine(photoString, photoType);
                            }
                            String photoState = contentValues.getAsString("data11");
                            if (!TextUtils.isEmpty(photoState) && "1".equals(photoState)) {
                                appendLine(VCardConstants.PROPERTY_X_PHOTOSTATE, photoState);
                            }
                        }
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendNotes(List<ContentValues> contentValuesList) {
        if (contentValuesList != null) {
            boolean shouldAppendCharsetInfo;
            String noteStr;
            boolean reallyUseQuotedPrintable;
            if (this.mOnlyOneNoteFieldIsAvailable) {
                StringBuilder noteBuilder = new StringBuilder();
                boolean first = true;
                for (ContentValues contentValues : contentValuesList) {
                    String note = contentValues.getAsString("data1");
                    if (note == null) {
                        note = "";
                    }
                    if (note.length() > 0) {
                        if (first) {
                            first = false;
                        } else {
                            noteBuilder.append('\n');
                        }
                        noteBuilder.append(note);
                    }
                }
                if (VCardUtils.containsOnlyPrintableAscii(noteBuilder.toString())) {
                    shouldAppendCharsetInfo = false;
                } else {
                    shouldAppendCharsetInfo = true;
                }
                if (this.mShouldUseQuotedPrintable) {
                    if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(noteStr)) {
                        reallyUseQuotedPrintable = true;
                        appendLine(VCardConstants.PROPERTY_NOTE, noteStr, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
                    }
                }
                reallyUseQuotedPrintable = false;
                appendLine(VCardConstants.PROPERTY_NOTE, noteStr, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
            } else {
                for (ContentValues contentValues2 : contentValuesList) {
                    noteStr = contentValues2.getAsString("data1");
                    if (!TextUtils.isEmpty(noteStr)) {
                        if (VCardUtils.containsOnlyPrintableAscii(noteStr)) {
                            shouldAppendCharsetInfo = false;
                        } else {
                            shouldAppendCharsetInfo = true;
                        }
                        if (this.mShouldUseQuotedPrintable) {
                            if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(noteStr)) {
                                reallyUseQuotedPrintable = true;
                                appendLine(VCardConstants.PROPERTY_NOTE, noteStr, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
                            }
                        }
                        reallyUseQuotedPrintable = false;
                        appendLine(VCardConstants.PROPERTY_NOTE, noteStr, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendEvents(List<ContentValues> contentValuesList) {
        if (contentValuesList != null) {
            String primaryBirthday = null;
            String secondaryBirthday = null;
            String primaryBirthdayType = null;
            String primaryBirthdaySolarDate = null;
            for (ContentValues contentValues : contentValuesList) {
                if (contentValues != null) {
                    int eventType;
                    Integer eventTypeAsInteger = contentValues.getAsInteger("data2");
                    if (eventTypeAsInteger != null) {
                        eventType = eventTypeAsInteger.intValue();
                    } else {
                        eventType = 2;
                    }
                    if (eventType == 3) {
                        String birthdayCandidate = contentValues.getAsString("data1");
                        if (birthdayCandidate != null) {
                            Integer isSuperPrimaryAsInteger = contentValues.getAsInteger("is_super_primary");
                            boolean isSuperPrimary = isSuperPrimaryAsInteger != null ? isSuperPrimaryAsInteger.intValue() > 0 : false;
                            if (isSuperPrimary) {
                                primaryBirthday = birthdayCandidate;
                                break;
                            }
                            Integer isPrimaryAsInteger = contentValues.getAsInteger("is_primary");
                            boolean isPrimary = isPrimaryAsInteger != null ? isPrimaryAsInteger.intValue() > 0 : false;
                            if (isPrimary) {
                                primaryBirthday = birthdayCandidate;
                            } else if (secondaryBirthday == null) {
                                secondaryBirthday = birthdayCandidate;
                            }
                            primaryBirthdayType = contentValues.getAsString("data15");
                            if (!TextUtils.isEmpty(primaryBirthdayType) && "1".equals(primaryBirthdayType)) {
                                primaryBirthdaySolarDate = contentValues.getAsString("data14");
                            }
                        } else {
                            continue;
                        }
                    } else if (this.mUsesAndroidProperty || this.mIsDoCoMo) {
                        appendAndroidSpecificProperty("vnd.android.cursor.item/contact_event", contentValues);
                    }
                }
            }
            if (primaryBirthday != null) {
                if (this.mIsCoreanMobilePhone && (primaryBirthday.length() != 0 || primaryBirthday.contains("-"))) {
                    primaryBirthday = primaryBirthday.replace("-", "");
                }
                appendLineWithCharsetAndQPDetection(VCardConstants.PROPERTY_BDAY, primaryBirthday.trim());
            } else if (secondaryBirthday != null) {
                if (this.mIsCoreanMobilePhone && (secondaryBirthday.length() != 0 || secondaryBirthday.contains("-"))) {
                    secondaryBirthday = secondaryBirthday.replace("-", "");
                }
                appendLineWithCharsetAndQPDetection(VCardConstants.PROPERTY_BDAY, secondaryBirthday.trim());
                if (!TextUtils.isEmpty(primaryBirthdayType) && "1".equals(primaryBirthdayType)) {
                    appendLine(VCardConstants.PROPERTY_X_BIRTHDAY_SOLATYPE, primaryBirthdayType);
                    if (!TextUtils.isEmpty(primaryBirthdaySolarDate)) {
                        appendLine(VCardConstants.PROPERTY_X_BIRTHDAY_SOLADATE, primaryBirthdaySolarDate);
                    }
                }
            }
        }
        return this;
    }

    public VCardBuilder appendRelation(List<ContentValues> contentValuesList) {
        if (this.mUsesAndroidProperty && contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                if (contentValues != null) {
                    appendAndroidSpecificProperty("vnd.android.cursor.item/relation", contentValues);
                }
            }
        }
        return this;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void appendPostalLine(int r10, java.lang.String r11, android.content.ContentValues r12, boolean r13, boolean r14) {
        /*
        r9 = this;
        r3 = r9.tryConstructPostalStruct(r12);
        if (r3 != 0) goto L_0x007a;
    L_0x0006:
        if (r14 == 0) goto L_0x0079;
    L_0x0008:
        r4 = 0;
        r1 = 0;
        r0 = "";
    L_0x000c:
        r2 = new java.util.ArrayList;
        r2.<init>();
        if (r13 == 0) goto L_0x0018;
    L_0x0013:
        r6 = "PREF";
        r2.add(r6);
    L_0x0018:
        switch(r10) {
            case 0: goto L_0x008d;
            case 1: goto L_0x0081;
            case 2: goto L_0x0087;
            case 3: goto L_0x00d5;
            default: goto L_0x001b;
        };
    L_0x001b:
        r6 = "vCard";
        r7 = new java.lang.StringBuilder;
        r8 = "Unknown StructuredPostal type: ";
        r7.<init>(r8);
        r7 = r7.append(r10);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
    L_0x002f:
        r6 = r9.mBuilder;
        r7 = "ADR";
        r6.append(r7);
        r6 = r2.isEmpty();
        if (r6 != 0) goto L_0x0046;
    L_0x003c:
        r6 = r9.mBuilder;
        r7 = ";";
        r6.append(r7);
        r9.appendTypeParameters(r2);
    L_0x0046:
        if (r1 == 0) goto L_0x0056;
    L_0x0048:
        r6 = r9.mBuilder;
        r7 = ";";
        r6.append(r7);
        r6 = r9.mBuilder;
        r7 = r9.mVCardCharsetParameter;
        r6.append(r7);
    L_0x0056:
        if (r4 == 0) goto L_0x0066;
    L_0x0058:
        r6 = r9.mBuilder;
        r7 = ";";
        r6.append(r7);
        r6 = r9.mBuilder;
        r7 = "ENCODING=QUOTED-PRINTABLE";
        r6.append(r7);
    L_0x0066:
        r6 = r9.mBuilder;
        r7 = ":";
        r6.append(r7);
        r6 = r9.mBuilder;
        r6.append(r0);
        r6 = r9.mBuilder;
        r7 = "\r\n";
        r6.append(r7);
    L_0x0079:
        return;
    L_0x007a:
        r4 = r3.reallyUseQuotedPrintable;
        r1 = r3.appendCharset;
        r0 = r3.addressData;
        goto L_0x000c;
    L_0x0081:
        r6 = "HOME";
        r2.add(r6);
        goto L_0x002f;
    L_0x0087:
        r6 = "WORK";
        r2.add(r6);
        goto L_0x002f;
    L_0x008d:
        r6 = android.text.TextUtils.isEmpty(r11);
        if (r6 != 0) goto L_0x00b7;
    L_0x0093:
        r6 = r9.mIsDoCoMo;
        if (r6 != 0) goto L_0x00a3;
    L_0x0097:
        r6 = 1;
        r6 = new java.lang.String[r6];
        r7 = 0;
        r6[r7] = r11;
        r6 = com.android.vcard.VCardUtils.containsOnlyAlphaDigitHyphen(r6);
        if (r6 == 0) goto L_0x00b7;
    L_0x00a3:
        r6 = new java.lang.StringBuilder;
        r7 = "X-";
        r6.<init>(r7);
        r6 = r6.append(r11);
        r6 = r6.toString();
        r2.add(r6);
        goto L_0x002f;
    L_0x00b7:
        r5 = r9.encodeQuotedPrintable(r11);
        r6 = new java.lang.StringBuilder;
        r7 = "X-CUSTOM(CHARSET=UTF-8,ENCODING=QUOTED-PRINTABLE,";
        r6.<init>(r7);
        r6 = r6.append(r5);
        r7 = ")";
        r6 = r6.append(r7);
        r6 = r6.toString();
        r2.add(r6);
        goto L_0x002f;
    L_0x00d5:
        r6 = r9.mIsDoCoMo;
        if (r6 == 0) goto L_0x002f;
    L_0x00d9:
        r6 = "OTHER";
        r2.add(r6);
        goto L_0x002f;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendPostalLine(int, java.lang.String, android.content.ContentValues, boolean, boolean):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void appendEmailLine(int r7, java.lang.String r8, java.lang.String r9, boolean r10) {
        /*
        r6 = this;
        switch(r7) {
            case 0: goto L_0x0033;
            case 1: goto L_0x007c;
            case 2: goto L_0x007f;
            case 3: goto L_0x0082;
            case 4: goto L_0x008b;
            default: goto L_0x0003;
        };
    L_0x0003:
        r3 = "vCard";
        r4 = new java.lang.StringBuilder;
        r5 = "Unknown Email type: ";
        r4.<init>(r5);
        r4 = r4.append(r7);
        r4 = r4.toString();
        android.util.Log.e(r3, r4);
        r2 = 0;
    L_0x0018:
        r0 = new java.util.ArrayList;
        r0.<init>();
        if (r10 == 0) goto L_0x0024;
    L_0x001f:
        r3 = "PREF";
        r0.add(r3);
    L_0x0024:
        r3 = android.text.TextUtils.isEmpty(r2);
        if (r3 != 0) goto L_0x002d;
    L_0x002a:
        r0.add(r2);
    L_0x002d:
        r3 = "EMAIL";
        r6.appendLineWithCharsetAndQPDetection(r3, r0, r9);
        return;
    L_0x0033:
        r3 = com.android.vcard.VCardUtils.isMobilePhoneLabel(r8);
        if (r3 == 0) goto L_0x003c;
    L_0x0039:
        r2 = "CELL";
        goto L_0x0018;
    L_0x003c:
        r3 = android.text.TextUtils.isEmpty(r8);
        if (r3 != 0) goto L_0x0062;
    L_0x0042:
        r3 = r6.mIsDoCoMo;
        if (r3 != 0) goto L_0x0052;
    L_0x0046:
        r3 = 1;
        r3 = new java.lang.String[r3];
        r4 = 0;
        r3[r4] = r8;
        r3 = com.android.vcard.VCardUtils.containsOnlyAlphaDigitHyphen(r3);
        if (r3 == 0) goto L_0x0062;
    L_0x0052:
        r3 = new java.lang.StringBuilder;
        r4 = "X-";
        r3.<init>(r4);
        r3 = r3.append(r8);
        r2 = r3.toString();
        goto L_0x0018;
    L_0x0062:
        r1 = r6.encodeQuotedPrintable(r8);
        r3 = new java.lang.StringBuilder;
        r4 = "X-CUSTOM(CHARSET=UTF-8,ENCODING=QUOTED-PRINTABLE,";
        r3.<init>(r4);
        r3 = r3.append(r1);
        r4 = ")";
        r3 = r3.append(r4);
        r2 = r3.toString();
        goto L_0x0018;
    L_0x007c:
        r2 = "HOME";
        goto L_0x0018;
    L_0x007f:
        r2 = "WORK";
        goto L_0x0018;
    L_0x0082:
        r3 = r6.mIsDoCoMo;
        if (r3 == 0) goto L_0x0089;
    L_0x0086:
        r2 = "OTHER";
        goto L_0x0018;
    L_0x0089:
        r2 = 0;
        goto L_0x0018;
    L_0x008b:
        r2 = "CELL";
        goto L_0x0018;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendEmailLine(int, java.lang.String, java.lang.String, boolean):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void appendTelLine(java.lang.Integer r12, java.lang.String r13, java.lang.String r14, boolean r15) {
        /*
        r11 = this;
        r10 = 2;
        r9 = 1;
        r8 = 0;
        r5 = 44;
        r6 = 112; // 0x70 float:1.57E-43 double:5.53E-322;
        r5 = r14.replace(r5, r6);
        r6 = 59;
        r7 = 119; // 0x77 float:1.67E-43 double:5.9E-322;
        r2 = r5.replace(r6, r7);
        r5 = r11.mBuilder;
        r6 = "TEL";
        r5.append(r6);
        r5 = r11.mBuilder;
        r6 = ";";
        r5.append(r6);
        if (r12 != 0) goto L_0x0063;
    L_0x0023:
        r3 = 7;
    L_0x0024:
        r0 = new java.util.ArrayList;
        r0.<init>();
        switch(r3) {
            case 0: goto L_0x0135;
            case 1: goto L_0x0068;
            case 2: goto L_0x00a8;
            case 3: goto L_0x0076;
            case 4: goto L_0x0096;
            case 5: goto L_0x0084;
            case 6: goto L_0x00af;
            case 7: goto L_0x00b6;
            case 8: goto L_0x012e;
            case 9: goto L_0x00cc;
            case 10: goto L_0x00d3;
            case 11: goto L_0x00e5;
            case 12: goto L_0x00ec;
            case 13: goto L_0x00ef;
            case 14: goto L_0x01f4;
            case 15: goto L_0x0101;
            case 16: goto L_0x01ff;
            case 17: goto L_0x0108;
            case 18: goto L_0x011b;
            case 19: goto L_0x01b2;
            case 20: goto L_0x0127;
            default: goto L_0x002c;
        };
    L_0x002c:
        if (r15 == 0) goto L_0x0033;
    L_0x002e:
        r5 = "PREF";
        r0.add(r5);
    L_0x0033:
        r5 = r11.mIsCoreanMobilePhone;
        if (r5 == 0) goto L_0x003c;
    L_0x0037:
        r5 = "VOICE";
        r0.add(r5);
    L_0x003c:
        r5 = r0.isEmpty();
        if (r5 == 0) goto L_0x020a;
    L_0x0042:
        r5 = r11.mBuilder;
        r6 = java.lang.Integer.valueOf(r3);
        r11.appendUncommonPhoneType(r5, r6);
    L_0x004b:
        r5 = r11.mBuilder;
        r6 = ":";
        r5.append(r6);
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x020f;
    L_0x0056:
        r5 = r11.mBuilder;
        r5.append(r14);
    L_0x005b:
        r5 = r11.mBuilder;
        r6 = "\r\n";
        r5.append(r6);
        return;
    L_0x0063:
        r3 = r12.intValue();
        goto L_0x0024;
    L_0x0068:
        r5 = new java.lang.String[r9];
        r6 = "HOME";
        r5[r8] = r6;
        r5 = java.util.Arrays.asList(r5);
        r0.addAll(r5);
        goto L_0x002c;
    L_0x0076:
        r5 = new java.lang.String[r9];
        r6 = "WORK";
        r5[r8] = r6;
        r5 = java.util.Arrays.asList(r5);
        r0.addAll(r5);
        goto L_0x002c;
    L_0x0084:
        r5 = new java.lang.String[r10];
        r6 = "HOME";
        r5[r8] = r6;
        r6 = "FAX";
        r5[r9] = r6;
        r5 = java.util.Arrays.asList(r5);
        r0.addAll(r5);
        goto L_0x002c;
    L_0x0096:
        r5 = new java.lang.String[r10];
        r6 = "WORK";
        r5[r8] = r6;
        r6 = "FAX";
        r5[r9] = r6;
        r5 = java.util.Arrays.asList(r5);
        r0.addAll(r5);
        goto L_0x002c;
    L_0x00a8:
        r5 = "CELL";
        r0.add(r5);
        goto L_0x002c;
    L_0x00af:
        r5 = "PAGER";
        r0.add(r5);
        goto L_0x002c;
    L_0x00b6:
        r5 = r11.mIsCoreanMobilePhone;
        if (r5 != 0) goto L_0x00be;
    L_0x00ba:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x00c5;
    L_0x00be:
        r5 = "OTHER";
        r0.add(r5);
        goto L_0x002c;
    L_0x00c5:
        r5 = "VOICE";
        r0.add(r5);
        goto L_0x002c;
    L_0x00cc:
        r5 = "CAR";
        r0.add(r5);
        goto L_0x002c;
    L_0x00d3:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x00df;
    L_0x00d7:
        r5 = "COMPANY-MAIN";
        r0.add(r5);
    L_0x00dc:
        r15 = 1;
        goto L_0x002c;
    L_0x00df:
        r5 = "WORK";
        r0.add(r5);
        goto L_0x00dc;
    L_0x00e5:
        r5 = "ISDN";
        r0.add(r5);
        goto L_0x002c;
    L_0x00ec:
        r15 = 1;
        goto L_0x002c;
    L_0x00ef:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x00fa;
    L_0x00f3:
        r5 = "OTHER-FAX";
        r0.add(r5);
        goto L_0x002c;
    L_0x00fa:
        r5 = "FAX";
        r0.add(r5);
        goto L_0x002c;
    L_0x0101:
        r5 = "TLX";
        r0.add(r5);
        goto L_0x002c;
    L_0x0108:
        r5 = new java.lang.String[r10];
        r6 = "WORK";
        r5[r8] = r6;
        r6 = "CELL";
        r5[r9] = r6;
        r5 = java.util.Arrays.asList(r5);
        r0.addAll(r5);
        goto L_0x002c;
    L_0x011b:
        r5 = "WORK";
        r0.add(r5);
        r5 = "PAGER";
        r0.add(r5);
        goto L_0x002c;
    L_0x0127:
        r5 = "MSG";
        r0.add(r5);
        goto L_0x002c;
    L_0x012e:
        r5 = "CALLBACK";
        r0.add(r5);
        goto L_0x002c;
    L_0x0135:
        r5 = android.text.TextUtils.isEmpty(r13);
        if (r5 == 0) goto L_0x014d;
    L_0x013b:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x0146;
    L_0x013f:
        r5 = "OTHER";
        r0.add(r5);
        goto L_0x002c;
    L_0x0146:
        r5 = "VOICE";
        r0.add(r5);
        goto L_0x002c;
    L_0x014d:
        r5 = com.android.vcard.VCardUtils.isMobilePhoneLabel(r13);
        if (r5 == 0) goto L_0x015a;
    L_0x0153:
        r5 = "CELL";
        r0.add(r5);
        goto L_0x002c;
    L_0x015a:
        r5 = r11.mIsV30OrV40;
        if (r5 == 0) goto L_0x0163;
    L_0x015e:
        r0.add(r13);
        goto L_0x002c;
    L_0x0163:
        r4 = r13.toUpperCase();
        r5 = com.android.vcard.VCardUtils.isValidInV21ButUnknownToContactsPhoteType(r4);
        if (r5 == 0) goto L_0x0172;
    L_0x016d:
        r0.add(r4);
        goto L_0x002c;
    L_0x0172:
        r5 = r11.mIsDoCoMo;
        if (r5 != 0) goto L_0x0180;
    L_0x0176:
        r5 = new java.lang.String[r9];
        r5[r8] = r13;
        r5 = com.android.vcard.VCardUtils.containsOnlyAlphaDigitHyphen(r5);
        if (r5 == 0) goto L_0x0194;
    L_0x0180:
        r5 = new java.lang.StringBuilder;
        r6 = "X-";
        r5.<init>(r6);
        r5 = r5.append(r13);
        r5 = r5.toString();
        r0.add(r5);
        goto L_0x002c;
    L_0x0194:
        r1 = r11.encodeQuotedPrintable(r13);
        r5 = new java.lang.StringBuilder;
        r6 = "X-CUSTOM(CHARSET=UTF-8,ENCODING=QUOTED-PRINTABLE,";
        r5.<init>(r6);
        r5 = r5.append(r1);
        r6 = ")";
        r5 = r5.append(r6);
        r5 = r5.toString();
        r0.add(r5);
        goto L_0x002c;
    L_0x01b2:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x002c;
    L_0x01b6:
        r5 = android.text.TextUtils.isEmpty(r13);
        if (r5 == 0) goto L_0x01c3;
    L_0x01bc:
        r5 = "ASSISTANT";
        r0.add(r5);
        goto L_0x002c;
    L_0x01c3:
        r4 = r13.toUpperCase();
        r5 = com.android.vcard.VCardUtils.isValidInV21ButUnknownToContactsPhoteType(r4);
        if (r5 == 0) goto L_0x01d2;
    L_0x01cd:
        r0.add(r4);
        goto L_0x002c;
    L_0x01d2:
        r5 = r11.mIsDoCoMo;
        if (r5 != 0) goto L_0x01e0;
    L_0x01d6:
        r5 = new java.lang.String[r9];
        r5[r8] = r13;
        r5 = com.android.vcard.VCardUtils.containsOnlyAlphaDigitHyphen(r5);
        if (r5 == 0) goto L_0x002c;
    L_0x01e0:
        r5 = new java.lang.StringBuilder;
        r6 = "X-";
        r5.<init>(r6);
        r5 = r5.append(r13);
        r5 = r5.toString();
        r0.add(r5);
        goto L_0x002c;
    L_0x01f4:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x01ff;
    L_0x01f8:
        r5 = "RADIO";
        r0.add(r5);
        goto L_0x002c;
    L_0x01ff:
        r5 = r11.mIsDoCoMo;
        if (r5 == 0) goto L_0x002c;
    L_0x0203:
        r5 = "TTY-TDD";
        r0.add(r5);
        goto L_0x002c;
    L_0x020a:
        r11.appendTypeParameters(r0);
        goto L_0x004b;
    L_0x020f:
        r5 = r11.mBuilder;
        r5.append(r2);
        goto L_0x005b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardBuilder.appendTelLine(java.lang.Integer, java.lang.String, java.lang.String, boolean):void");
    }

    private void appendUncommonPhoneType(StringBuilder builder, Integer type) {
        if (this.mIsDoCoMo) {
            builder.append("OTHER");
            return;
        }
        String phoneType = VCardUtils.getPhoneTypeString(type);
        if (phoneType != null) {
            appendTypeParameter(phoneType);
        } else {
            Log.e(LOG_TAG, "Unknown or unsupported (by vCard) Phone type: " + type);
        }
    }

    public void appendPhotoLine(String encodedValue, String photoType) {
        StringBuilder tmpBuilder = new StringBuilder();
        tmpBuilder.append(VCardConstants.PROPERTY_PHOTO);
        tmpBuilder.append(";");
        if (this.mIsV30OrV40) {
            tmpBuilder.append(VCARD_PARAM_ENCODING_BASE64_AS_B);
        } else {
            tmpBuilder.append(VCARD_PARAM_ENCODING_BASE64_V21);
        }
        tmpBuilder.append(";");
        appendTypeParameter(tmpBuilder, photoType);
        tmpBuilder.append(VCARD_DATA_SEPARATOR);
        tmpBuilder.append(encodedValue);
        String tmpStr = tmpBuilder.toString();
        tmpBuilder = new StringBuilder();
        int lineCount = 0;
        int length = tmpStr.length();
        int maxNumForFirstLine = 75 - VCARD_END_OF_LINE.length();
        int maxNumInGeneral = maxNumForFirstLine - VCARD_WS.length();
        int maxNum = maxNumForFirstLine;
        for (int i = 0; i < length; i++) {
            tmpBuilder.append(tmpStr.charAt(i));
            lineCount++;
            if (lineCount > maxNum) {
                tmpBuilder.append(VCARD_END_OF_LINE);
                tmpBuilder.append(VCARD_WS);
                maxNum = maxNumInGeneral;
                lineCount = 0;
            }
        }
        this.mBuilder.append(tmpBuilder.toString());
        this.mBuilder.append(VCARD_END_OF_LINE);
        this.mBuilder.append(VCARD_END_OF_LINE);
    }

    public VCardBuilder appendSipAddresses(List<ContentValues> contentValuesList) {
        boolean useXProperty;
        if (this.mIsV30OrV40) {
            useXProperty = false;
        } else {
            if (this.mUsesDefactProperty) {
                useXProperty = true;
            }
            return this;
        }
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                String sipAddress = contentValues.getAsString("data1");
                if (!TextUtils.isEmpty(sipAddress)) {
                    if (useXProperty) {
                        if (sipAddress.startsWith("sip:")) {
                            if (sipAddress.length() != 4) {
                                sipAddress = sipAddress.substring(4);
                            }
                        }
                        appendLineWithCharsetAndQPDetection(VCardConstants.PROPERTY_X_SIP, sipAddress);
                    } else {
                        String propertyName;
                        if (!sipAddress.startsWith("sip:")) {
                            sipAddress = "sip:" + sipAddress;
                        }
                        if (VCardConfig.isVersion40(this.mVCardType)) {
                            propertyName = VCardConstants.PROPERTY_TEL;
                        } else {
                            propertyName = VCardConstants.PROPERTY_IMPP;
                        }
                        appendLineWithCharsetAndQPDetection(propertyName, sipAddress);
                    }
                }
            }
        }
        return this;
    }

    public void appendAndroidSpecificProperty(String mimeType, ContentValues contentValues) {
        if (sAllowedAndroidPropertySet.contains(mimeType)) {
            boolean needCharset;
            boolean reallyUseQuotedPrintable;
            Collection<String> rawValueList = new ArrayList();
            for (int i = 1; i <= 15; i++) {
                String value = contentValues.getAsString(new StringBuilder(c.c).append(i).toString());
                if (value == null) {
                    value = "";
                }
                rawValueList.add(value);
            }
            if (!this.mShouldAppendCharsetParam || VCardUtils.containsOnlyNonCrLfPrintableAscii((Collection) rawValueList)) {
                needCharset = false;
            } else {
                needCharset = true;
            }
            if (!this.mShouldUseQuotedPrintable || VCardUtils.containsOnlyNonCrLfPrintableAscii((Collection) rawValueList)) {
                reallyUseQuotedPrintable = false;
            } else {
                reallyUseQuotedPrintable = true;
            }
            this.mBuilder.append(VCardConstants.PROPERTY_X_ANDROID_CUSTOM);
            if (needCharset) {
                this.mBuilder.append(";");
                this.mBuilder.append(this.mVCardCharsetParameter);
            }
            if (reallyUseQuotedPrintable) {
                this.mBuilder.append(";");
                this.mBuilder.append(VCARD_PARAM_ENCODING_QP);
            }
            this.mBuilder.append(VCARD_DATA_SEPARATOR);
            this.mBuilder.append(mimeType);
            for (String rawValue : rawValueList) {
                String encodedValue;
                if (reallyUseQuotedPrintable) {
                    encodedValue = encodeQuotedPrintable(rawValue);
                } else {
                    encodedValue = escapeCharacters(rawValue);
                }
                this.mBuilder.append(";");
                this.mBuilder.append(encodedValue);
            }
            this.mBuilder.append(VCARD_END_OF_LINE);
        }
    }

    public void appendLineWithCharsetAndQPDetection(String propertyName, String rawValue) {
        appendLineWithCharsetAndQPDetection(propertyName, null, rawValue);
    }

    public void appendLineWithCharsetAndQPDetection(String propertyName, List<String> rawValueList) {
        appendLineWithCharsetAndQPDetection(propertyName, null, (List) rawValueList);
    }

    public void appendLineWithCharsetAndQPDetection(String propertyName, List<String> parameterList, String rawValue) {
        boolean needCharset;
        boolean reallyUseQuotedPrintable;
        if (VCardUtils.containsOnlyPrintableAscii(rawValue)) {
            needCharset = false;
        } else {
            needCharset = true;
        }
        if (this.mShouldUseQuotedPrintable) {
            if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(rawValue)) {
                reallyUseQuotedPrintable = true;
                appendLine(propertyName, (List) parameterList, rawValue, needCharset, reallyUseQuotedPrintable);
            }
        }
        reallyUseQuotedPrintable = false;
        appendLine(propertyName, (List) parameterList, rawValue, needCharset, reallyUseQuotedPrintable);
    }

    public void appendLineWithCharsetAndQPDetection(String propertyName, List<String> parameterList, List<String> rawValueList) {
        boolean needCharset;
        boolean reallyUseQuotedPrintable;
        if (!this.mShouldAppendCharsetParam || VCardUtils.containsOnlyNonCrLfPrintableAscii((Collection) rawValueList)) {
            needCharset = false;
        } else {
            needCharset = true;
        }
        if (!this.mShouldUseQuotedPrintable || VCardUtils.containsOnlyNonCrLfPrintableAscii((Collection) rawValueList)) {
            reallyUseQuotedPrintable = false;
        } else {
            reallyUseQuotedPrintable = true;
        }
        appendLine(propertyName, (List) parameterList, (List) rawValueList, needCharset, reallyUseQuotedPrintable);
    }

    public void appendLine(String propertyName, String rawValue) {
        appendLine(propertyName, rawValue, false, false);
    }

    public void appendLine(String propertyName, List<String> rawValueList) {
        appendLine(propertyName, (List) rawValueList, false, false);
    }

    public void appendLine(String propertyName, String rawValue, boolean needCharset, boolean reallyUseQuotedPrintable) {
        appendLine(propertyName, null, rawValue, needCharset, reallyUseQuotedPrintable);
    }

    public void appendLine(String propertyName, List<String> parameterList, String rawValue) {
        appendLine(propertyName, (List) parameterList, rawValue, false, false);
    }

    public void appendLine(String propertyName, List<String> parameterList, String rawValue, boolean needCharset, boolean reallyUseQuotedPrintable) {
        String encodedValue;
        this.mBuilder.append(propertyName);
        if (parameterList != null && parameterList.size() > 0) {
            this.mBuilder.append(";");
            appendTypeParameters(parameterList);
        }
        if (needCharset) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (reallyUseQuotedPrintable) {
            this.mBuilder.append(";");
            this.mBuilder.append(VCARD_PARAM_ENCODING_QP);
            encodedValue = encodeQuotedPrintable(rawValue);
        } else if (this.mIsDoCoMo) {
            encodedValue = rawValue;
        } else if (VCardConstants.PROPERTY_ORG.equals(propertyName)) {
            encodedValue = escapeCharactersForOrg(rawValue);
        } else {
            encodedValue = escapeCharacters(rawValue);
        }
        this.mBuilder.append(VCARD_DATA_SEPARATOR);
        this.mBuilder.append(encodedValue);
        this.mBuilder.append(VCARD_END_OF_LINE);
    }

    public void appendLine(String propertyName, List<String> rawValueList, boolean needCharset, boolean needQuotedPrintable) {
        appendLine(propertyName, null, (List) rawValueList, needCharset, needQuotedPrintable);
    }

    public void appendLine(String propertyName, List<String> parameterList, List<String> rawValueList, boolean needCharset, boolean needQuotedPrintable) {
        this.mBuilder.append(propertyName);
        if (parameterList != null && parameterList.size() > 0) {
            this.mBuilder.append(";");
            appendTypeParameters(parameterList);
        }
        if (needCharset) {
            this.mBuilder.append(";");
            this.mBuilder.append(this.mVCardCharsetParameter);
        }
        if (needQuotedPrintable) {
            this.mBuilder.append(";");
            this.mBuilder.append(VCARD_PARAM_ENCODING_QP);
        }
        this.mBuilder.append(VCARD_DATA_SEPARATOR);
        boolean first = true;
        for (String rawValue : rawValueList) {
            String encodedValue;
            if (needQuotedPrintable) {
                encodedValue = encodeQuotedPrintable(rawValue);
            } else {
                encodedValue = escapeCharacters(rawValue);
            }
            if (first) {
                first = false;
            } else {
                this.mBuilder.append(";");
            }
            this.mBuilder.append(encodedValue);
        }
        this.mBuilder.append(VCARD_END_OF_LINE);
    }

    private void appendTypeParameters(List<String> types) {
        boolean first = true;
        for (String typeValue : types) {
            if (VCardConfig.isVersion30(this.mVCardType) || VCardConfig.isVersion40(this.mVCardType)) {
                String encoded;
                if (VCardConfig.isVersion40(this.mVCardType)) {
                    encoded = VCardUtils.toStringAsV40ParamValue(typeValue);
                } else {
                    encoded = VCardUtils.toStringAsV30ParamValue(typeValue);
                }
                if (!TextUtils.isEmpty(encoded)) {
                    if (first) {
                        first = false;
                    } else {
                        this.mBuilder.append(";");
                    }
                    appendTypeParameter(encoded);
                }
            } else if (this.mIsDoCoMo || VCardUtils.isV21Word(typeValue) || typeValue.contains("X-CUSTOM")) {
                if (first) {
                    first = false;
                } else {
                    this.mBuilder.append(";");
                }
                appendTypeParameter(typeValue);
            }
        }
    }

    private void appendTypeParameter(String type) {
        appendTypeParameter(this.mBuilder, type);
    }

    private void appendTypeParameter(StringBuilder builder, String type) {
        if (VCardConfig.isVersion40(this.mVCardType) || ((VCardConfig.isVersion30(this.mVCardType) || this.mAppendTypeParamName) && !this.mIsDoCoMo)) {
            builder.append(VCardConstants.PARAM_TYPE).append(VCARD_PARAM_EQUAL);
        }
        builder.append(type);
    }

    private boolean shouldAppendCharsetParam(String... propertyValueList) {
        if (!this.mShouldAppendCharsetParam) {
            return false;
        }
        int length = propertyValueList.length;
        for (int i = 0; i < length; i++) {
            if (!VCardUtils.containsOnlyPrintableAscii(propertyValueList[i])) {
                return true;
            }
        }
        return false;
    }

    private String encodeQuotedPrintable(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        byte[] strArray;
        StringBuilder builder = new StringBuilder();
        int index = 0;
        int lineCount = 0;
        try {
            strArray = str.getBytes(this.mCharset);
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Charset " + this.mCharset + " cannot be used. " + "Try default charset");
            strArray = str.getBytes();
        }
        while (index < strArray.length) {
            builder.append(String.format("=%02X", new Object[]{Byte.valueOf(strArray[index])}));
            index++;
            lineCount += 3;
            if (lineCount >= 67) {
                builder.append("=\r\n");
                lineCount = 0;
            }
        }
        return builder.toString();
    }

    private String escapeCharactersForOrg(String unescaped) {
        if (TextUtils.isEmpty(unescaped)) {
            return "";
        }
        StringBuilder tmpBuilder = new StringBuilder();
        int length = unescaped.length();
        int i = 0;
        while (i < length) {
            char ch = unescaped.charAt(i);
            switch (ch) {
                case '\r':
                    if (i + 1 < length && unescaped.charAt(i) == '\n') {
                        break;
                    }
                case '\n':
                    tmpBuilder.append("\\n");
                    break;
                case MotionEventCompat.AXIS_GENERIC_13 /*44*/:
                    if (!this.mIsV30OrV40) {
                        tmpBuilder.append(ch);
                        break;
                    }
                    tmpBuilder.append("\\,");
                    break;
                case '\\':
                    if (this.mIsV30OrV40) {
                        tmpBuilder.append("\\\\");
                        break;
                    }
                case '<':
                case '>':
                    if (!this.mIsDoCoMo) {
                        tmpBuilder.append(ch);
                        break;
                    }
                    tmpBuilder.append('\\');
                    tmpBuilder.append(ch);
                    break;
                default:
                    tmpBuilder.append(ch);
                    break;
            }
            i++;
        }
        return tmpBuilder.toString();
    }

    private String escapeCharacters(String unescaped) {
        if (TextUtils.isEmpty(unescaped)) {
            return "";
        }
        StringBuilder tmpBuilder = new StringBuilder();
        int length = unescaped.length();
        int i = 0;
        while (i < length) {
            char ch = unescaped.charAt(i);
            switch (ch) {
                case '\r':
                    if (i + 1 < length && unescaped.charAt(i) == '\n') {
                        break;
                    }
                case '\n':
                    tmpBuilder.append("\\n");
                    break;
                case MotionEventCompat.AXIS_GENERIC_13 /*44*/:
                    if (!this.mIsV30OrV40) {
                        tmpBuilder.append(ch);
                        break;
                    }
                    tmpBuilder.append("\\,");
                    break;
                case ';':
                    tmpBuilder.append('\\');
                    tmpBuilder.append(';');
                    break;
                case '\\':
                    if (this.mIsV30OrV40) {
                        tmpBuilder.append("\\\\");
                        break;
                    }
                case '<':
                case '>':
                    if (!this.mIsDoCoMo) {
                        tmpBuilder.append(ch);
                        break;
                    }
                    tmpBuilder.append('\\');
                    tmpBuilder.append(ch);
                    break;
                default:
                    tmpBuilder.append(ch);
                    break;
            }
            i++;
        }
        return tmpBuilder.toString();
    }

    public String toString() {
        if (!this.mEndAppended) {
            if (this.mIsDoCoMo) {
                appendLine(VCardConstants.PROPERTY_X_CLASS, VCARD_DATA_PUBLIC);
                appendLine(VCardConstants.PROPERTY_X_REDUCTION, "");
                appendLine(VCardConstants.PROPERTY_X_NO, "");
                appendLine(VCardConstants.PROPERTY_X_DCM_HMN_MODE, "");
            }
            appendLine(VCardConstants.PROPERTY_END, VCARD_DATA_VCARD);
            this.mEndAppended = true;
        }
        return this.mBuilder.toString();
    }

    public void appendGroupName(List<ContentValues> list, ContentResolver mContentResolver) {
        HashMap<String, String> groups = new HashMap();
        if (list != null) {
            Cursor cursor = mContentResolver.query(Groups.CONTENT_URI, new String[]{"_id", "title", "system_id"}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            groups.put(cursor.getString(cursor.getColumnIndex("_id")), cursor.getString(cursor.getColumnIndex("title")));
                        } while (cursor.moveToNext());
                    } else {
                        Log.d(LOG_TAG, "cursor is empty");
                    }
                    cursor.close();
                } catch (Throwable th) {
                    cursor.close();
                }
            }
            for (ContentValues contentValues : list) {
                String groupName = (String) groups.get(contentValues.getAsString("data1"));
                if (groupName != null) {
                    boolean reallyUseQuotedPrintable;
                    boolean shouldAppendCharsetInfo = !VCardUtils.containsOnlyPrintableAscii(groupName);
                    if (this.mShouldUseQuotedPrintable) {
                        if (!VCardUtils.containsOnlyNonCrLfPrintableAscii(groupName)) {
                            reallyUseQuotedPrintable = true;
                            appendLine(VCardConstants.PROPERTY_XGROUPNAME, groupName, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
                        }
                    }
                    reallyUseQuotedPrintable = false;
                    appendLine(VCardConstants.PROPERTY_XGROUPNAME, groupName, shouldAppendCharsetInfo, reallyUseQuotedPrintable);
                }
            }
        }
    }

    public void appendNameCard(List<ContentValues> contentValuesList, ContentResolver mContentResolver) {
        AssetFileDescriptor fd;
        if (contentValuesList != null) {
            for (ContentValues contentValues : contentValuesList) {
                if (contentValues != null) {
                    LinkedHashMap<String, String> nameCardFiles = new LinkedHashMap();
                    nameCardFiles.put("FRONT", contentValues.getAsString("data14"));
                    nameCardFiles.put("BACK", contentValues.getAsString("data12"));
                    for (String nameCardFile : nameCardFiles.keySet()) {
                        String nameCardFileId = (String) nameCardFiles.get(nameCardFile);
                        if (nameCardFileId != null) {
                            FileInputStream fis;
                            try {
                                fd = mContentResolver.openAssetFileDescriptor(ContactsContract.AUTHORITY_URI.buildUpon().appendEncodedPath("display_photo").appendEncodedPath(nameCardFileId).build(), "r");
                                byte[] buffer = new byte[16384];
                                fis = fd.createInputStream();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                while (true) {
                                    int size = fis.read(buffer);
                                    if (size == -1) {
                                        break;
                                    }
                                    baos.write(buffer, 0, size);
                                }
                                byte[] data = baos.toByteArray();
                                fis.close();
                                fd.close();
                                if (data != null) {
                                    String photoType = VCardUtils.guessImageType(data);
                                    String str = new String(Base64.encode(data, 2));
                                    if (!TextUtils.isEmpty(str)) {
                                        StringBuilder tmpBuilder = new StringBuilder();
                                        if (TextUtils.equals(nameCardFile, "FRONT")) {
                                            tmpBuilder.append(VCardConstants.PROPERTY_X_NAMECARDPHOTO);
                                        } else if (TextUtils.equals(nameCardFile, "BACK")) {
                                            tmpBuilder.append(VCardConstants.PROPERTY_X_NAMECARDPHOTO_REVERSE);
                                        }
                                        tmpBuilder.append(";");
                                        if (this.mIsV30OrV40) {
                                            tmpBuilder.append(VCARD_PARAM_ENCODING_BASE64_AS_B);
                                        } else {
                                            tmpBuilder.append(VCARD_PARAM_ENCODING_BASE64_V21);
                                        }
                                        tmpBuilder.append(";");
                                        appendTypeParameter(tmpBuilder, photoType);
                                        tmpBuilder.append(VCARD_DATA_SEPARATOR);
                                        tmpBuilder.append(str);
                                        String tmpStr = tmpBuilder.toString();
                                        tmpBuilder = new StringBuilder();
                                        int lineCount = 0;
                                        int length = tmpStr.length();
                                        int maxNumForFirstLine = 75 - VCARD_END_OF_LINE.length();
                                        int maxNumInGeneral = maxNumForFirstLine - VCARD_WS.length();
                                        int maxNum = maxNumForFirstLine;
                                        for (int i = 0; i < length; i++) {
                                            tmpBuilder.append(tmpStr.charAt(i));
                                            lineCount++;
                                            if (lineCount > maxNum) {
                                                tmpBuilder.append(VCARD_END_OF_LINE);
                                                tmpBuilder.append(VCARD_WS);
                                                maxNum = maxNumInGeneral;
                                                lineCount = 0;
                                            }
                                        }
                                        this.mBuilder.append(tmpBuilder.toString());
                                        this.mBuilder.append(VCARD_END_OF_LINE);
                                        this.mBuilder.append(VCARD_END_OF_LINE);
                                    }
                                }
                            } catch (IOException e) {
                            } catch (Throwable th) {
                                fis.close();
                                fd.close();
                            }
                        }
                    }
                    continue;
                }
            }
        }
    }
}
