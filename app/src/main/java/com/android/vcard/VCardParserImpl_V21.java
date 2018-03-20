package com.android.vcard;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.vcard.exception.VCardAgentNotSupportedException;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardInvalidCommentLineException;
import com.android.vcard.exception.VCardInvalidLineException;
import com.android.vcard.exception.VCardVersionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class VCardParserImpl_V21 {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_ENCODING = "8BIT";
    private static final String LOG_TAG = "vCard";
    private static final int STATE_GROUP_OR_PROPERTY_NAME = 0;
    private static final int STATE_PARAMS = 1;
    private static final int STATE_PARAMS_IN_DQUOTE = 2;
    private boolean mCanceled;
    protected String mCurrentCharset;
    protected String mCurrentEncoding;
    protected final String mIntermediateCharset;
    private final List<VCardInterpreter> mInterpreterList;
    private boolean mIsCustomLabel;
    private boolean mIsSSHParams;
    private boolean mIsShiftJis;
    protected CustomBufferedReader mReader;
    protected final Set<String> mUnknownTypeSet;
    protected final Set<String> mUnknownValueSet;

    protected static final class CustomBufferedReader extends BufferedReader {
        private String mNextLine;
        private boolean mNextLineIsValid;
        private long mTime;

        public CustomBufferedReader(Reader in) {
            super(in);
        }

        public String readLine() throws IOException {
            if (this.mNextLineIsValid) {
                String ret = this.mNextLine;
                this.mNextLine = null;
                this.mNextLineIsValid = false;
                return ret;
            }
            long start = System.currentTimeMillis();
            String line = super.readLine();
            this.mTime += System.currentTimeMillis() - start;
            return line;
        }

        public String peekLine() throws IOException {
            if (!this.mNextLineIsValid) {
                long start = System.currentTimeMillis();
                String line = super.readLine();
                this.mTime += System.currentTimeMillis() - start;
                this.mNextLine = line;
                this.mNextLineIsValid = true;
            }
            return this.mNextLine;
        }

        public long getTotalmillisecond() {
            return this.mTime;
        }
    }

    public VCardParserImpl_V21() {
        this(VCardConfig.VCARD_TYPE_DEFAULT);
    }

    public VCardParserImpl_V21(int vcardType) {
        this.mInterpreterList = new ArrayList();
        this.mUnknownTypeSet = new HashSet();
        this.mUnknownValueSet = new HashSet();
        this.mIntermediateCharset = VCardConfig.DEFAULT_INTERMEDIATE_CHARSET;
    }

    public VCardParserImpl_V21(int vcardType, String charset) {
        this.mInterpreterList = new ArrayList();
        this.mUnknownTypeSet = new HashSet();
        this.mUnknownValueSet = new HashSet();
        if (charset != null) {
            this.mIntermediateCharset = charset;
        } else {
            this.mIntermediateCharset = VCardConfig.DEFAULT_INTERMEDIATE_CHARSET;
        }
    }

    protected boolean isValidPropertyName(String propertyName) {
        if (!(getKnownPropertyNameSet().contains(propertyName.toUpperCase()) || propertyName.startsWith("X-") || this.mUnknownTypeSet.contains(propertyName))) {
            this.mUnknownTypeSet.add(propertyName);
            Log.w(LOG_TAG, "Property name unsupported by vCard 2.1: " + propertyName);
        }
        return true;
    }

    protected String getLine() throws IOException {
        String line = this.mReader.readLine();
        if (line != null && !line.contains(VCardConstants.PROPERTY_ADR) && line.contains("X-CUSTOM") && line.endsWith("=")) {
            do {
                line = line.substring(0, line.length() - 1) + this.mReader.readLine();
                if (line == null) {
                    break;
                }
            } while (!line.contains("):"));
        }
        return line;
    }

    protected String peekLine() throws IOException {
        return this.mReader.peekLine();
    }

    protected String getNonEmptyLine() throws IOException, VCardException {
        String line;
        do {
            line = getLine();
            if (line == null) {
                throw new VCardException("Reached end of buffer.");
            }
        } while (line.trim().length() <= 0);
        return line;
    }

    private boolean parseOneVCard() throws IOException, VCardException {
        this.mCurrentEncoding = "8BIT";
        this.mCurrentCharset = "UTF-8";
        if (!readBeginVCard(false)) {
            return false;
        }
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onEntryStarted();
        }
        parseItems();
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onEntryEnded();
        }
        return true;
    }

    protected boolean readBeginVCard(boolean allowGarbage) throws IOException, VCardException {
        while (true) {
            String line = getLine();
            if (line == null) {
                return false;
            }
            if (line.trim().length() > 0) {
                String[] strArray = line.split(":", 2);
                if (strArray.length == 2 && strArray[0].trim().equalsIgnoreCase(VCardConstants.PROPERTY_BEGIN) && strArray[1].trim().equalsIgnoreCase("VCARD")) {
                    return true;
                }
                if (!allowGarbage) {
                    throw new VCardException("Expected String \"BEGIN:VCARD\" did not come (Instead, \"" + line + "\" came)");
                } else if (!allowGarbage) {
                    throw new VCardException("Reached where must not be reached.");
                }
            }
        }
    }

    protected void parseItems() throws IOException, VCardException {
        boolean ended = false;
        try {
            ended = parseItem();
        } catch (VCardInvalidCommentLineException e) {
            Log.e(LOG_TAG, "Invalid line which looks like some comment was found. Ignored.");
        }
        while (!ended) {
            try {
                ended = parseItem();
            } catch (VCardInvalidCommentLineException e2) {
                Log.e(LOG_TAG, "Invalid line which looks like some comment was found. Ignored.");
            }
        }
    }

    protected boolean parseItem() throws IOException, VCardException {
        this.mCurrentEncoding = "8BIT";
        VCardProperty propertyData = constructPropertyData(getNonEmptyLine());
        if (VCardConfig.isJapanSpacialized() && propertyData == null) {
            return false;
        }
        String propertyNameUpper = propertyData.getName().toUpperCase();
        String propertyRawValue = propertyData.getRawValue();
        if (VCardConfig.isJapanSpacialized()) {
            if (propertyNameUpper.equals(VCardConstants.PROPERTY_BEGIN)) {
                if (propertyRawValue.equalsIgnoreCase("VCARD")) {
                    handleNest();
                    return false;
                }
                throw new VCardException("Unknown BEGIN type: " + propertyRawValue);
            } else if (propertyNameUpper.equals(VCardConstants.PROPERTY_END)) {
                if (propertyRawValue.equalsIgnoreCase("VCARD")) {
                    return true;
                }
                throw new VCardException("Unknown END type: " + propertyRawValue);
            } else if (propertyNameUpper.equals(VCardConstants.PROPERTY_PHOTO) || propertyNameUpper.equals(VCardConstants.PROPERTY_X_ANDROID_CUSTOM)) {
                parseItemInter(propertyData, propertyNameUpper);
                return false;
            } else if (!VCardConstants.PROPERTY_VERSION.equals(propertyNameUpper) || propertyRawValue.equals(getVersionString())) {
                handlePropertyValue(propertyData, propertyNameUpper);
                return false;
            } else {
                throw new VCardVersionException("Incompatible version: " + propertyRawValue + " != " + getVersion());
            }
        } else if (propertyNameUpper.equals(VCardConstants.PROPERTY_BEGIN)) {
            if (propertyRawValue.equalsIgnoreCase("VCARD")) {
                handleNest();
                return false;
            }
            throw new VCardException("Unknown BEGIN type: " + propertyRawValue);
        } else if (!propertyNameUpper.equals(VCardConstants.PROPERTY_END)) {
            parseItemInter(propertyData, propertyNameUpper);
            return false;
        } else if (propertyRawValue.equalsIgnoreCase("VCARD")) {
            return true;
        } else {
            throw new VCardException("Unknown END type: " + propertyRawValue);
        }
    }

    private void parseItemInter(VCardProperty property, String propertyNameUpper) throws IOException, VCardException {
        String propertyRawValue = property.getRawValue();
        if (propertyNameUpper.equals(VCardConstants.PROPERTY_AGENT)) {
            handleAgent(property);
        } else if (propertyNameUpper.matches(".*BDAY.*")) {
            if (property.getRawValue().endsWith("-")) {
                property.setRawValue("1");
            }
            handlePropertyValue(property, propertyNameUpper);
        } else if (!isValidPropertyName(propertyNameUpper)) {
            throw new VCardException("Unknown property name: \"" + propertyNameUpper + "\"");
        } else if (!propertyNameUpper.equals(VCardConstants.PROPERTY_VERSION) || propertyRawValue.equals(getVersionString())) {
            handlePropertyValue(property, propertyNameUpper);
        } else {
            throw new VCardVersionException("Incompatible version: " + propertyRawValue + " != " + getVersionString());
        }
    }

    private void handleNest() throws IOException, VCardException {
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onEntryStarted();
        }
        parseItems();
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onEntryEnded();
        }
    }

    protected VCardProperty constructPropertyData(String line) throws VCardException {
        VCardProperty propertyData = new VCardProperty();
        int length = line.length();
        if (length <= 0 || line.charAt(0) != '#') {
            int state = 0;
            int nameIndex = 0;
            boolean mIsShiftJisAsDefault = VCardConfig.isShiftJisAsDefault();
            if (mIsShiftJisAsDefault) {
                this.mIsShiftJis = false;
            }
            int i = 0;
            while (i < length) {
                char ch = line.charAt(i);
                switch (state) {
                    case 0:
                        if (VCardConfig.isJapanSpacialized() && this.mIsSSHParams && length >= 2 && i == 0 && line.charAt(i) == ' ' && line.charAt(i + 1) == ':') {
                            this.mIsSSHParams = false;
                            return null;
                        } else if (VCardConfig.isJapanSpacialized() && this.mIsSSHParams && length >= 2 && i == 0 && line.charAt(i) == ' ' && line.charAt(i + 1) != ':') {
                            return null;
                        } else {
                            if (ch == ':') {
                                propertyData.setName(line.substring(nameIndex, i));
                                propertyData.setRawValue(i < length + -1 ? line.substring(i + 1) : "");
                                if (!mIsShiftJisAsDefault || line.contains("CHARSET=")) {
                                    return propertyData;
                                }
                                handleParams(propertyData, "CHARSET=SHIFT_JIS");
                                this.mIsShiftJis = true;
                                return propertyData;
                            } else if (ch == '.') {
                                String groupName = line.substring(nameIndex, i);
                                if (groupName.length() == 0) {
                                    Log.w(LOG_TAG, "Empty group found. Ignoring.");
                                } else {
                                    propertyData.addGroup(groupName);
                                }
                                nameIndex = i + 1;
                                break;
                            } else if (ch == ';') {
                                String propertyName = line.substring(nameIndex, i);
                                if (!VCardConfig.isJapanSpacialized() || !VCardConstants.PARAM_X_SSH_VCARD_PARAMS.equalsIgnoreCase(propertyName)) {
                                    propertyData.setName(propertyName);
                                    nameIndex = i + 1;
                                    state = 1;
                                    break;
                                }
                                this.mIsSSHParams = true;
                                return null;
                            } else {
                                continue;
                            }
                        }
                        break;
                    case 1:
                        if (ch != '\"') {
                            String[] strArray;
                            if (ch != ';') {
                                if (ch != ':') {
                                    if (ch != ' ') {
                                        if (VCardConfig.isJapanSpacialized() && ch == 'X') {
                                            if ("X-".equals(line.substring(nameIndex, nameIndex + 2)) && !propertyData.getName().equals(VCardConstants.PROPERTY_SOUND)) {
                                                this.mIsCustomLabel = true;
                                            }
                                            if ((propertyData.getName().equals(VCardConstants.PROPERTY_TEL) || propertyData.getName().equals(VCardConstants.PROPERTY_EMAIL) || propertyData.getName().equals(VCardConstants.PROPERTY_ADR) || propertyData.getName().equals(VCardConstants.PROPERTY_ORG) || propertyData.getName().equals(VCardConstants.PROPERTY_TITLE)) && this.mIsCustomLabel) {
                                                String str;
                                                if (this.mIsShiftJis) {
                                                    str = VCardUtils.convertStringCharset(line, VCardConfig.DEFAULT_INTERMEDIATE_CHARSET, "Shift_JIS");
                                                    length = str.length();
                                                    this.mIsShiftJis = false;
                                                } else {
                                                    str = VCardUtils.convertStringCharset(line, VCardConfig.DEFAULT_INTERMEDIATE_CHARSET, "UTF-8");
                                                    length = str.length();
                                                }
                                                line = str;
                                                break;
                                            }
                                        }
                                    }
                                    nameIndex = i + 1;
                                    break;
                                } else if (VCardConfig.isJapanSpacialized()) {
                                    strArray = line.substring(nameIndex, i).split("=", 2);
                                    if (strArray.length == 2) {
                                        if (VCardConstants.PARAM_CHARSET.equals(strArray[0])) {
                                            if (VCardConfig.IMPORT_CHARSET_SHIFTJIS.equals(strArray[1])) {
                                                this.mIsShiftJis = true;
                                            }
                                        } else if (!"CHARSET=".contains(line) && mIsShiftJisAsDefault) {
                                            handleParams(propertyData, "CHARSET=SHIFT_JIS");
                                            this.mIsShiftJis = true;
                                        }
                                    }
                                    if (propertyData.getName().equals(VCardConstants.PROPERTY_TEL) && this.mIsCustomLabel) {
                                        if (line.substring(i + 1).contains(":")) {
                                            break;
                                        }
                                        handleParams(propertyData, line.substring(nameIndex, i));
                                        propertyData.setRawValue(i < length + -1 ? line.substring(i + 1) : "");
                                        this.mIsCustomLabel = false;
                                        return propertyData;
                                    } else if ((propertyData.getName().equals(VCardConstants.PROPERTY_EMAIL) || propertyData.getName().equals(VCardConstants.PROPERTY_ADR) || propertyData.getName().equals(VCardConstants.PROPERTY_ORG)) && this.mIsCustomLabel) {
                                        String temp = line.substring(i + 1);
                                        if (temp.contains(VCardConstants.PARAM_CHARSET) && temp.contains(VCardConstants.PARAM_ENCODING)) {
                                            break;
                                        }
                                        String substring;
                                        handleParams(propertyData, line.substring(nameIndex, i));
                                        if (i < length - 1) {
                                            substring = line.substring(i + 1);
                                        } else {
                                            substring = "";
                                        }
                                        propertyData.setRawValue(substring);
                                        this.mIsCustomLabel = false;
                                        return propertyData;
                                    } else {
                                        handleParams(propertyData, line.substring(nameIndex, i));
                                        propertyData.setRawValue(i < length + -1 ? line.substring(i + 1) : "");
                                        return propertyData;
                                    }
                                } else {
                                    handleParams(propertyData, line.substring(nameIndex, i));
                                    propertyData.setRawValue(i < length + -1 ? line.substring(i + 1) : "");
                                    return propertyData;
                                }
                            } else if (!VCardConfig.isJapanSpacialized()) {
                                handleParams(propertyData, line.substring(nameIndex, i));
                                nameIndex = i + 1;
                                break;
                            } else {
                                strArray = line.substring(nameIndex, i).split("=", 2);
                                if (strArray.length == 2 && strArray[0].equals(VCardConstants.PARAM_CHARSET) && strArray[1].equals(VCardConfig.IMPORT_CHARSET_SHIFTJIS)) {
                                    this.mIsShiftJis = true;
                                }
                                if (mIsShiftJisAsDefault && !line.contains("CHARSET=")) {
                                    handleParams(propertyData, "CHARSET=SHIFT_JIS");
                                    this.mIsShiftJis = true;
                                }
                                String nextChar = "";
                                if (length - 1 > nameIndex + 2) {
                                    if ("X-".equals(line.substring(nameIndex, nameIndex + 2)) && !propertyData.getName().equals(VCardConstants.PROPERTY_SOUND)) {
                                        this.mIsCustomLabel = true;
                                    }
                                }
                                for (int index = 1; index < 9; index++) {
                                    if (i + index < length) {
                                        nextChar = new StringBuilder(String.valueOf(nextChar)).append(line.charAt(i + index)).toString();
                                    }
                                }
                                if (propertyData.getName().equals(VCardConstants.PROPERTY_TEL) || propertyData.getName().equals(VCardConstants.PROPERTY_EMAIL) || propertyData.getName().equals(VCardConstants.PROPERTY_ADR) || propertyData.getName().equals(VCardConstants.PROPERTY_ORG)) {
                                    if (this.mIsCustomLabel) {
                                        if (!"CHARSET=".equals(nextChar) && !VCardConstants.PARAM_ENCODING.equals(nextChar)) {
                                            break;
                                        }
                                        handleParams(propertyData, line.substring(nameIndex, i));
                                        nameIndex = i + 1;
                                        break;
                                    }
                                    handleParams(propertyData, line.substring(nameIndex, i));
                                    nameIndex = i + 1;
                                    break;
                                }
                                handleParams(propertyData, line.substring(nameIndex, i));
                                nameIndex = i + 1;
                                break;
                            }
                        }
                        if (VCardConstants.VERSION_V21.equalsIgnoreCase(getVersionString())) {
                            Log.w(LOG_TAG, "Double-quoted params found in vCard 2.1. Silently allow it");
                        }
                        state = 2;
                        break;
                        break;
                    case 2:
                        if (ch == '\"') {
                            if (VCardConstants.VERSION_V21.equalsIgnoreCase(getVersionString())) {
                                Log.w(LOG_TAG, "Double-quoted params found in vCard 2.1. Silently allow it");
                            }
                            state = 1;
                            break;
                        }
                        break;
                    default:
                        break;
                }
                i++;
            }
            throw new VCardInvalidLineException("Invalid line: \"" + line + "\"");
        }
        throw new VCardInvalidCommentLineException();
    }

    protected boolean isCheckEscapeChar(String params) {
        int i = 0;
        while (i < params.length()) {
            if ((params.charAt(i) < '0' || params.charAt(i) > '9') && params.charAt(i) != '-' && params.charAt(i) != ' ' && params.charAt(i) != '*' && params.charAt(i) != '+' && params.charAt(i) != ';' && params.charAt(i) != ',' && params.charAt(i) != '#' && params.charAt(i) != '/') {
                return false;
            }
            i++;
        }
        return true;
    }

    protected void handleParams(VCardProperty propertyData, String params) throws VCardException {
        String[] strArray = params.split("=", 2);
        if (strArray.length == 2) {
            String paramName = strArray[0].trim().toUpperCase();
            String paramValue = strArray[1].trim();
            if (paramName.equals(VCardConstants.PARAM_TYPE)) {
                handleType(propertyData, paramValue);
                return;
            } else if (paramName.equals(VCardConstants.PARAM_VALUE)) {
                handleValue(propertyData, paramValue);
                return;
            } else if (paramName.equals(VCardConstants.PARAM_ENCODING)) {
                handleEncoding(propertyData, paramValue);
                return;
            } else if (paramName.equals(VCardConstants.PARAM_CHARSET)) {
                handleCharset(propertyData, paramValue);
                return;
            } else if (paramName.equals(VCardConstants.PARAM_LANGUAGE)) {
                handleLanguage(propertyData, paramValue);
                return;
            } else if (paramName.startsWith("X-")) {
                if (params.contains("X-CUSTOM")) {
                    handleParamWithoutName(propertyData, "X-" + VCardUtils.parseQuotedPrintable(params.substring(params.lastIndexOf(",") + 1, params.length() - 1), false, VCardConfig.DEFAULT_INTERMEDIATE_CHARSET, "UTF-8"));
                    return;
                }
                handleAnyParam(propertyData, paramName, paramValue);
                return;
            } else if (!paramName.startsWith("X_")) {
                throw new VCardException("Unknown type \"" + paramName + "\"");
            } else {
                return;
            }
        }
        handleParamWithoutName(propertyData, strArray[0]);
    }

    protected void handleParamWithoutName(VCardProperty propertyData, String paramValue) {
        handleType(propertyData, paramValue);
    }

    protected void handleType(VCardProperty propertyData, String ptypeval) {
        if (!(getKnownTypeSet().contains(ptypeval.toUpperCase()) || ptypeval.startsWith("X-") || this.mUnknownTypeSet.contains(ptypeval))) {
            this.mUnknownTypeSet.add(ptypeval);
            Log.w(LOG_TAG, String.format("TYPE unsupported by %s: ", new Object[]{Integer.valueOf(getVersion()), ptypeval}));
        }
        propertyData.addParameter(VCardConstants.PARAM_TYPE, ptypeval);
    }

    protected void handleValue(VCardProperty propertyData, String pvalueval) {
        if (!(getKnownValueSet().contains(pvalueval.toUpperCase()) || pvalueval.startsWith("X-") || this.mUnknownValueSet.contains(pvalueval))) {
            this.mUnknownValueSet.add(pvalueval);
            Log.w(LOG_TAG, String.format("The value unsupported by TYPE of %s: ", new Object[]{Integer.valueOf(getVersion()), pvalueval}));
        }
        propertyData.addParameter(VCardConstants.PARAM_VALUE, pvalueval);
    }

    protected void handleEncoding(VCardProperty propertyData, String pencodingval) throws VCardException {
        if (getAvailableEncodingSet().contains(pencodingval) || pencodingval.startsWith("X-")) {
            propertyData.addParameter(VCardConstants.PARAM_ENCODING, pencodingval);
            this.mCurrentEncoding = pencodingval.toUpperCase();
            return;
        }
        throw new VCardException("Unknown encoding \"" + pencodingval + "\"");
    }

    protected void handleCharset(VCardProperty propertyData, String charsetval) {
        this.mCurrentCharset = charsetval;
        propertyData.addParameter(VCardConstants.PARAM_CHARSET, charsetval);
    }

    protected void handleLanguage(VCardProperty propertyData, String langval) throws VCardException {
        String[] strArray = langval.split("-");
        if (strArray.length < 1) {
            throw new VCardException("Invalid Language: \"" + langval + "\"");
        }
        for (String tmp : strArray) {
            int length = tmp.length();
            int i = 0;
            while (i < length) {
                if (isAsciiLetter(tmp.charAt(i))) {
                    i++;
                } else {
                    throw new VCardException("Invalid Language: \"" + langval + "\"");
                }
            }
        }
        propertyData.addParameter(VCardConstants.PARAM_LANGUAGE, langval);
    }

    private boolean isAsciiLetter(char ch) {
        if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z')) {
            return false;
        }
        return true;
    }

    protected void handleAnyParam(VCardProperty propertyData, String paramName, String paramValue) {
        propertyData.addParameter(paramName, paramValue);
    }

    protected void handlePropertyValue(VCardProperty property, String propertyName) throws IOException, VCardException {
        String propertyNameUpper = property.getName().toUpperCase();
        String propertyRawValue = property.getRawValue();
        String sourceCharset = VCardConfig.DEFAULT_INTERMEDIATE_CHARSET;
        Collection<String> charsetCollection = property.getParameters(VCardConstants.PARAM_CHARSET);
        String targetCharset = charsetCollection != null ? (String) charsetCollection.iterator().next() : null;
        if (TextUtils.isEmpty(targetCharset)) {
            targetCharset = "UTF-8";
        }
        if (this.mIntermediateCharset.equals("EUC-KR")) {
            sourceCharset = this.mIntermediateCharset;
            targetCharset = this.mIntermediateCharset;
        } else if (this.mIntermediateCharset.equalsIgnoreCase(VCardConfig.IMPORT_CHARSET_SHIFTJIS)) {
            sourceCharset = this.mIntermediateCharset;
            targetCharset = this.mIntermediateCharset;
        }
        if (VCardConfig.isJapanSpacialized() && (propertyNameUpper.equals(VCardConstants.PROPERTY_ADR) || propertyNameUpper.equals(VCardConstants.PROPERTY_ORG) || propertyNameUpper.equals(VCardConstants.PROPERTY_N) || propertyNameUpper.equals(VCardConstants.PROPERTY_SOUND) || propertyNameUpper.equals(VCardConstants.PROPERTY_X_ANDROID_CUSTOM))) {
            handleAdrOrgN(property, propertyRawValue, sourceCharset, targetCharset);
        } else if (propertyNameUpper.equals(VCardConstants.PROPERTY_ADR) || propertyNameUpper.equals(VCardConstants.PROPERTY_ORG) || propertyNameUpper.equals(VCardConstants.PROPERTY_N)) {
            handleAdrOrgN(property, propertyRawValue, sourceCharset, targetCharset);
        } else if (this.mCurrentEncoding.equals(VCardConstants.PARAM_ENCODING_QP) || (propertyNameUpper.equals(VCardConstants.PROPERTY_FN) && property.getParameters(VCardConstants.PARAM_ENCODING) == null && VCardUtils.appearsLikeAndroidVCardQuotedPrintable(propertyRawValue))) {
            String quotedPrintablePart = getQuotedPrintablePart(propertyRawValue);
            String propertyEncodedValue = VCardUtils.parseQuotedPrintable(quotedPrintablePart, false, sourceCharset, targetCharset);
            property.setRawValue(quotedPrintablePart);
            if (!propertyNameUpper.equals(VCardConstants.PROPERTY_X_ANDROID_CUSTOM) || quotedPrintablePart.startsWith("vnd.android.cursor.item/relation")) {
                property.setValues(propertyEncodedValue);
            } else {
                List<String> quotedPrintableValueList = VCardUtils.constructListFromValue(quotedPrintablePart, getVersion());
                List encodedValueList = new ArrayList();
                for (String quotedPrintableValue : quotedPrintableValueList) {
                    encodedValueList.add(VCardUtils.parseQuotedPrintable(quotedPrintableValue, false, sourceCharset, targetCharset));
                }
                property.setValues(encodedValueList);
            }
            for (VCardInterpreter interpreter : this.mInterpreterList) {
                interpreter.onPropertyCreated(property);
            }
        } else if (this.mCurrentEncoding.equals(VCardConstants.PARAM_ENCODING_BASE64) || this.mCurrentEncoding.equalsIgnoreCase(VCardConstants.PARAM_ENCODING_B)) {
            try {
                property.setByteValue(Base64.decode(getBase64(propertyRawValue).getBytes(), 0));
                for (VCardInterpreter interpreter2 : this.mInterpreterList) {
                    interpreter2.onPropertyCreated(property);
                }
            } catch (IllegalArgumentException e) {
                Log.d(LOG_TAG, "Cannot decode Base64", e);
            } catch (OutOfMemoryError e2) {
                Log.e(LOG_TAG, "OutOfMemoryError happened during parsing BASE64 data!");
                for (VCardInterpreter interpreter22 : this.mInterpreterList) {
                    interpreter22.onPropertyCreated(property);
                }
            }
        } else {
            if (!(this.mCurrentEncoding.equals(VCardConstants.PARAM_ENCODING_7BIT) || this.mCurrentEncoding.equals("8BIT") || this.mCurrentEncoding.startsWith("X-"))) {
                Log.w(LOG_TAG, String.format("The encoding \"%s\" is unsupported by vCard %s", new Object[]{this.mCurrentEncoding, getVersionString()}));
            }
            if (getVersion() == 0) {
                StringBuilder builder = null;
                while (true) {
                    String nextLine = peekLine();
                    if (!TextUtils.isEmpty(nextLine) && nextLine.charAt(0) == ' ' && !"END:VCARD".contains(nextLine.toUpperCase())) {
                        getLine();
                        if (builder == null) {
                            builder = new StringBuilder();
                            builder.append(propertyRawValue);
                        }
                        builder.append(nextLine.substring(1));
                    } else if (builder != null) {
                        propertyRawValue = builder.toString();
                    }
                }
                if (builder != null) {
                    propertyRawValue = builder.toString();
                }
            }
            List propertyValueList = new ArrayList();
            propertyValueList.add(maybeUnescapeText(VCardUtils.convertStringCharset(propertyRawValue, sourceCharset, targetCharset)));
            property.setValues(propertyValueList);
            for (VCardInterpreter interpreter222 : this.mInterpreterList) {
                interpreter222.onPropertyCreated(property);
            }
        }
    }

    private String listToString(List<String> list) {
        int size = list.size();
        if (size > 1) {
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (String type : list) {
                builder.append(type);
                if (i < size - 1) {
                    builder.append(";");
                }
                i++;
            }
            return builder.toString();
        } else if (size == 1) {
            return (String) list.get(0);
        } else {
            return "";
        }
    }

    private void handleAdrOrgN(VCardProperty property, String propertyRawValue, String sourceCharset, String targetCharset) throws VCardException, IOException {
        List encodedValueList = new ArrayList();
        if (this.mCurrentEncoding.equals(VCardConstants.PARAM_ENCODING_QP)) {
            String quotedPrintablePart = getQuotedPrintablePart(propertyRawValue);
            property.setRawValue(quotedPrintablePart);
            for (String quotedPrintableValue : VCardUtils.constructListFromValue(quotedPrintablePart, getVersion())) {
                String encoded = VCardUtils.parseQuotedPrintable(quotedPrintableValue, false, sourceCharset, targetCharset);
                if (VCardConfig.isJapanSpacialized() && this.mIsShiftJis) {
                    encodedValueList.add(listToString(VCardUtils.constructListFromShiftJisValue(encoded, getVersion())).trim());
                } else {
                    int firstSepIndex = encoded.indexOf(";");
                    if (firstSepIndex != -1) {
                        String department;
                        String company = encoded.substring(0, firstSepIndex);
                        if (firstSepIndex + 1 < encoded.length()) {
                            department = encoded.substring(firstSepIndex + 1, encoded.length());
                        } else {
                            department = "";
                        }
                        if (TextUtils.isEmpty(company) || TextUtils.isEmpty(department)) {
                            encodedValueList.add(encoded);
                        } else {
                            encodedValueList.add(company);
                            encodedValueList.add(department);
                        }
                    } else {
                        encodedValueList.add(encoded);
                    }
                }
            }
        } else if (VCardConfig.isJapanSpacialized() && this.mIsShiftJis) {
            for (String value : VCardUtils.constructListFromShiftJisValue(VCardUtils.convertStringCharset(getPotentialMultiline(propertyRawValue), sourceCharset, targetCharset), getVersion())) {
                encodedValueList.add(value);
            }
            this.mIsShiftJis = false;
        } else {
            for (String value2 : VCardUtils.constructListFromValue(VCardUtils.convertStringCharset(getPotentialMultiline(propertyRawValue), sourceCharset, targetCharset), getVersion())) {
                encodedValueList.add(value2);
            }
        }
        property.setValues(encodedValueList);
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onPropertyCreated(property);
        }
    }

    private String getQuotedPrintablePart(String firstString) throws IOException, VCardException {
        if (!firstString.trim().endsWith("=")) {
            return firstString;
        }
        int pos = firstString.length() - 1;
        do {
        } while (firstString.charAt(pos) != '=');
        StringBuilder builder = new StringBuilder();
        builder.append(firstString.substring(0, pos + 1));
        builder.append(VCardBuilder.VCARD_END_OF_LINE);
        while (true) {
            String line = getLine();
            if (line == null) {
                throw new VCardException("File ended during parsing a Quoted-Printable String");
            } else if (line.trim().endsWith("=")) {
                pos = line.length() - 1;
                do {
                } while (line.charAt(pos) != '=');
                builder.append(line.substring(0, pos + 1));
                builder.append(VCardBuilder.VCARD_END_OF_LINE);
            } else {
                builder.append(line);
                return builder.toString();
            }
        }
    }

    private String getPotentialMultiline(String firstString) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(firstString);
        while (true) {
            String line = peekLine();
            if (line != null && line.length() != 0 && getPropertyNameUpperCase(line) == null) {
                getLine();
                builder.append(" ").append(line);
            }
        }
        return builder.toString();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected java.lang.String getBase64(java.lang.String r7) throws java.io.IOException, com.android.vcard.exception.VCardException {
        /*
        r6 = this;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r0.append(r7);
    L_0x0008:
        r1 = r6.peekLine();
        if (r1 != 0) goto L_0x0016;
    L_0x000e:
        r3 = new com.android.vcard.exception.VCardException;
        r4 = "File ended during parsing BASE64 binary";
        r3.<init>(r4);
        throw r3;
    L_0x0016:
        r2 = r6.getPropertyNameUpperCase(r1);
        r3 = r6.getKnownPropertyNameSet();
        r3 = r3.contains(r2);
        if (r3 == 0) goto L_0x0048;
    L_0x0024:
        r3 = "vCard";
        r4 = "Found a next property during parsing a BASE64 string, which must not contain semi-colon or colon. Treat the line as next property.";
        android.util.Log.w(r3, r4);
        r3 = "vCard";
        r4 = new java.lang.StringBuilder;
        r5 = "Problematic line: ";
        r4.<init>(r5);
        r5 = r1.trim();
        r4 = r4.append(r5);
        r4 = r4.toString();
        android.util.Log.w(r3, r4);
    L_0x0043:
        r3 = r0.toString();
        return r3;
    L_0x0048:
        r6.getLine();
        r3 = r1.trim();
        r3 = r3.length();
        if (r3 == 0) goto L_0x0043;
    L_0x0055:
        r3 = " ";
        r3 = r1.startsWith(r3);
        if (r3 != 0) goto L_0x0065;
    L_0x005d:
        r3 = ":";
        r3 = r1.contains(r3);
        if (r3 != 0) goto L_0x0043;
    L_0x0065:
        r0.append(r1);
        goto L_0x0008;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardParserImpl_V21.getBase64(java.lang.String):java.lang.String");
    }

    private String getPropertyNameUpperCase(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex <= -1) {
            return null;
        }
        int minIndex;
        int semiColonIndex = line.indexOf(";");
        if (colonIndex == -1) {
            minIndex = semiColonIndex;
        } else if (semiColonIndex == -1) {
            minIndex = colonIndex;
        } else {
            minIndex = Math.min(colonIndex, semiColonIndex);
        }
        return line.substring(0, minIndex).toUpperCase();
    }

    protected void handleAgent(VCardProperty property) throws VCardException {
        if (property.getRawValue().toUpperCase().contains("BEGIN:VCARD")) {
            throw new VCardAgentNotSupportedException("AGENT Property is not supported now.");
        }
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onPropertyCreated(property);
        }
    }

    protected String maybeUnescapeText(String text) {
        return text;
    }

    protected String maybeUnescapeCharacter(char ch) {
        return unescapeCharacter(ch);
    }

    static String unescapeCharacter(char ch) {
        if ((ch == '<' || ch == '>') && VCardConfig.isJapanSpacialized()) {
            return String.valueOf(ch);
        }
        if (ch == '\\' || ch == ';' || ch == ':' || ch == ',') {
            return String.valueOf(ch);
        }
        return null;
    }

    protected int getVersion() {
        return 0;
    }

    protected String getVersionString() {
        return VCardConstants.VERSION_V21;
    }

    protected Set<String> getKnownPropertyNameSet() {
        return VCardParser_V21.sKnownPropertyNameSet;
    }

    protected Set<String> getKnownTypeSet() {
        return VCardParser_V21.sKnownTypeSet;
    }

    protected Set<String> getKnownValueSet() {
        return VCardParser_V21.sKnownValueSet;
    }

    protected Set<String> getAvailableEncodingSet() {
        return VCardParser_V21.sAvailableEncoding;
    }

    protected String getDefaultEncoding() {
        return "8BIT";
    }

    protected String getDefaultCharset() {
        return "UTF-8";
    }

    protected String getCurrentCharset() {
        return this.mCurrentCharset;
    }

    public void addInterpreter(VCardInterpreter interpreter) {
        this.mInterpreterList.add(interpreter);
    }

    public void parse(InputStream is) throws IOException, VCardException {
        if (is == null) {
            throw new NullPointerException("InputStream must not be null.");
        }
        this.mReader = new CustomBufferedReader(new InputStreamReader(is, this.mIntermediateCharset));
        long start = System.currentTimeMillis();
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onVCardStarted();
        }
        do {
            synchronized (this) {
                if (this.mCanceled) {
                    Log.i(LOG_TAG, "Cancel request has come. exitting parse operation.");
                    break;
                }
            }
        } while (parseOneVCard());
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onVCardEnded();
        }
    }

    public void parseOne(InputStream is) throws IOException, VCardException {
        if (is == null) {
            throw new NullPointerException("InputStream must not be null.");
        }
        this.mReader = new CustomBufferedReader(new InputStreamReader(is, this.mIntermediateCharset));
        long start = System.currentTimeMillis();
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onVCardStarted();
        }
        parseOneVCard();
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onVCardEnded();
        }
    }

    public final synchronized void cancel() {
        Log.i(LOG_TAG, "ParserImpl received cancel operation.");
        this.mCanceled = true;
    }
}
