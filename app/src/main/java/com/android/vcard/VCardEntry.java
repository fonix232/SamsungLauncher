package com.android.vcard;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.vcard.VCardConstants.ImportOnly;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VCardEntry {
    private static final int DEFAULT_ORGANIZATION_TYPE = 1;
    private static final String LOG_TAG = "vCard";
    private static final List<String> sEmptyList = Collections.unmodifiableList(new ArrayList(0));
    private static final Map<String, Integer> sImMap = new HashMap();
    private final Account mAccount;
    private List<AndroidCustomData> mAndroidCustomDataList;
    private AnniversaryData mAnniversary;
    private BirthdayData mBirthday;
    private List<VCardEntry> mChildren;
    private ContentResolver mContentsResolver;
    private List<EmailData> mEmailList;
    private List<ImData> mImList;
    private double mLatitude;
    private double mLongitude;
    private String mMapImageFilename;
    private List<NameCardData> mNameCardList;
    private final NameData mNameData;
    private List<NicknameData> mNicknameList;
    private List<NoteData> mNoteList;
    private List<OrganizationData> mOrganizationList;
    private List<PhoneData> mPhoneList;
    private List<PhotoData> mPhotoList;
    private List<PhotoStateData> mPhotoStateList;
    private List<PostalData> mPostalList;
    private List<SipData> mSipList;
    private List<Pair<String, String>> mUnknownXData;
    private final int mVCardType;
    private List<WebsiteData> mWebsiteList;
    private List<XGroupNameData> mXGroupNameList;

    public interface EntryElement {
        void constructInsertOperation(List<ContentProviderOperation> list, int i);

        EntryLabel getEntryLabel();

        boolean isEmpty();
    }

    public interface EntryElementIterator {
        boolean onElement(EntryElement entryElement);

        void onElementGroupEnded();

        void onElementGroupStarted(EntryLabel entryLabel);

        void onIterationEnded();

        void onIterationStarted();
    }

    public enum EntryLabel {
        NAME,
        PHONE,
        EMAIL,
        POSTAL_ADDRESS,
        ORGANIZATION,
        IM,
        PHOTO,
        WEBSITE,
        SIP,
        NICKNAME,
        NOTE,
        BIRTHDAY,
        ANNIVERSARY,
        ANDROID_CUSTOM,
        XGROUPNAME,
        PHOTOSTATE,
        NAMECARD
    }

    public static class AndroidCustomData implements EntryElement {
        private final List<String> mDataList;
        private final String mMimeType;

        public AndroidCustomData(String mimeType, List<String> dataList) {
            this.mMimeType = mimeType;
            this.mDataList = dataList;
        }

        public static AndroidCustomData constructAndroidCustomData(List<String> list) {
            String mimeType;
            List<String> dataList;
            int max = 16;
            if (list == null) {
                mimeType = null;
                dataList = null;
            } else if (list.size() < 2) {
                mimeType = (String) list.get(0);
                dataList = null;
            } else {
                if (list.size() < 16) {
                    max = list.size();
                }
                mimeType = (String) list.get(0);
                dataList = list.subList(1, max);
            }
            return new AndroidCustomData(mimeType, dataList);
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", this.mMimeType);
            for (int i = 0; i < this.mDataList.size(); i++) {
                String value = (String) this.mDataList.get(i);
                if (!TextUtils.isEmpty(value)) {
                    builder.withValue(new StringBuilder(c.c).append(i + 1).toString(), value);
                }
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mMimeType) || this.mDataList == null || this.mDataList.size() == 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidCustomData)) {
                return false;
            }
            AndroidCustomData data = (AndroidCustomData) obj;
            if (!TextUtils.equals(this.mMimeType, data.mMimeType)) {
                return false;
            }
            if (this.mDataList == null) {
                return data.mDataList == null;
            } else {
                int size = this.mDataList.size();
                if (size != data.mDataList.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (!TextUtils.equals((CharSequence) this.mDataList.get(i), (CharSequence) data.mDataList.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }

        public int hashCode() {
            int hash;
            if (this.mMimeType != null) {
                hash = this.mMimeType.hashCode();
            } else {
                hash = 0;
            }
            if (this.mDataList != null) {
                for (String data : this.mDataList) {
                    hash = (hash * 31) + (data != null ? data.hashCode() : 0);
                }
            }
            return hash;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("android-custom: " + this.mMimeType + ", data: ");
            builder.append(this.mDataList == null ? "null" : Arrays.toString(this.mDataList.toArray()));
            return builder.toString();
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.ANDROID_CUSTOM;
        }

        public String getMimeType() {
            return this.mMimeType;
        }

        public List<String> getDataList() {
            return this.mDataList;
        }
    }

    public static class AnniversaryData implements EntryElement {
        private final String mAnniversary;

        public AnniversaryData(String anniversary) {
            this.mAnniversary = anniversary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            builder.withValue("data1", this.mAnniversary);
            builder.withValue("data2", Integer.valueOf(1));
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAnniversary);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AnniversaryData)) {
                return false;
            }
            return TextUtils.equals(this.mAnniversary, ((AnniversaryData) obj).mAnniversary);
        }

        public int hashCode() {
            return this.mAnniversary != null ? this.mAnniversary.hashCode() : 0;
        }

        public String toString() {
            return "anniversary: " + this.mAnniversary;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.ANNIVERSARY;
        }

        public String getAnniversary() {
            return this.mAnniversary;
        }
    }

    public static class BirthdayData implements EntryElement {
        private final String mBirthday;
        private String mBirthdaySolaType = null;
        private String mBirthdaySolarDate = null;

        public BirthdayData(String birthday) {
            this.mBirthday = birthday;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/contact_event");
            String[] array = this.mBirthday.split("\\.");
            if (VCardConfig.isJapanSpacialized() && array.length == 3) {
                builder.withValue("data1", array[0] + "-" + array[1] + "-" + array[2]);
            } else {
                builder.withValue("data1", this.mBirthday);
            }
            builder.withValue("data2", Integer.valueOf(3));
            if (this.mBirthdaySolaType != null) {
                builder.withValue("mimetype", "vnd.android.cursor.item/contact_event");
                builder.withValue("data15", this.mBirthdaySolaType);
                builder.withValue("data14", this.mBirthdaySolarDate);
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mBirthday);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BirthdayData)) {
                return false;
            }
            return TextUtils.equals(this.mBirthday, ((BirthdayData) obj).mBirthday);
        }

        public int hashCode() {
            return this.mBirthday != null ? this.mBirthday.hashCode() : 0;
        }

        public String toString() {
            return "birthday: " + this.mBirthday;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.BIRTHDAY;
        }

        public String getBirthday() {
            return this.mBirthday;
        }

        public void setBirthdayType(String type) {
            this.mBirthdaySolaType = type;
        }

        public void setBirthdaySolarDate(String solarDate) {
            this.mBirthdaySolarDate = solarDate;
        }
    }

    public static class EmailData implements EntryElement {
        private final String mAddress;
        private final boolean mIsPrimary;
        private final String mLabel;
        private final int mType;

        public EmailData(String data, int type, String label, boolean isPrimary) {
            this.mType = type;
            this.mAddress = data;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/email_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            builder.withValue("data1", this.mAddress);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EmailData)) {
                return false;
            }
            EmailData emailData = (EmailData) obj;
            if (this.mType == emailData.mType && TextUtils.equals(this.mAddress, emailData.mAddress) && TextUtils.equals(this.mLabel, emailData.mLabel) && this.mIsPrimary == emailData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = 0;
            int i2 = this.mType * 31;
            if (this.mAddress != null) {
                hashCode = this.mAddress.hashCode();
            } else {
                hashCode = 0;
            }
            hashCode = (i2 + hashCode) * 31;
            if (this.mLabel != null) {
                i = this.mLabel.hashCode();
            }
            return ((hashCode + i) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, data: %s, label: %s, isPrimary: %s", new Object[]{Integer.valueOf(this.mType), this.mAddress, this.mLabel, Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.EMAIL;
        }

        public String getAddress() {
            return this.mAddress;
        }

        public int getType() {
            return this.mType;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    public static class ImData implements EntryElement {
        private final String mAddress;
        private final String mCustomProtocol;
        private final boolean mIsPrimary;
        private final int mProtocol;
        private final int mType;

        public ImData(int protocol, String customProtocol, String address, int type, boolean isPrimary) {
            this.mProtocol = protocol;
            this.mCustomProtocol = customProtocol;
            this.mType = type;
            this.mAddress = address;
            this.mIsPrimary = isPrimary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/im");
            builder.withValue("data2", Integer.valueOf(this.mType));
            builder.withValue("data5", Integer.valueOf(this.mProtocol));
            builder.withValue("data1", this.mAddress);
            if (this.mProtocol == -1) {
                builder.withValue("data6", this.mCustomProtocol);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ImData)) {
                return false;
            }
            ImData imData = (ImData) obj;
            if (this.mType == imData.mType && this.mProtocol == imData.mProtocol && TextUtils.equals(this.mCustomProtocol, imData.mCustomProtocol) && TextUtils.equals(this.mAddress, imData.mAddress) && this.mIsPrimary == imData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = 0;
            int i2 = ((this.mType * 31) + this.mProtocol) * 31;
            if (this.mCustomProtocol != null) {
                hashCode = this.mCustomProtocol.hashCode();
            } else {
                hashCode = 0;
            }
            hashCode = (i2 + hashCode) * 31;
            if (this.mAddress != null) {
                i = this.mAddress.hashCode();
            }
            return ((hashCode + i) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, protocol: %d, custom_protcol: %s, data: %s, isPrimary: %s", new Object[]{Integer.valueOf(this.mType), Integer.valueOf(this.mProtocol), this.mCustomProtocol, this.mAddress, Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.IM;
        }

        public String getAddress() {
            return this.mAddress;
        }

        public int getProtocol() {
            return this.mProtocol;
        }

        public String getCustomProtocol() {
            return this.mCustomProtocol;
        }

        public int getType() {
            return this.mType;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    private class InsertOperationConstrutor implements EntryElementIterator {
        private final int mBackReferenceIndex;
        private final List<ContentProviderOperation> mOperationList;

        public InsertOperationConstrutor(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            this.mOperationList = operationList;
            this.mBackReferenceIndex = backReferenceIndex;
        }

        public void onIterationStarted() {
        }

        public void onIterationEnded() {
        }

        public void onElementGroupStarted(EntryLabel label) {
        }

        public void onElementGroupEnded() {
        }

        public boolean onElement(EntryElement elem) {
            if (!elem.isEmpty()) {
                elem.constructInsertOperation(this.mOperationList, this.mBackReferenceIndex);
            }
            return true;
        }
    }

    private class IsIgnorableIterator implements EntryElementIterator {
        private boolean mEmpty;

        private IsIgnorableIterator() {
            this.mEmpty = true;
        }

        public void onIterationStarted() {
        }

        public void onIterationEnded() {
        }

        public void onElementGroupStarted(EntryLabel label) {
        }

        public void onElementGroupEnded() {
        }

        public boolean onElement(EntryElement elem) {
            if (elem.isEmpty()) {
                return true;
            }
            this.mEmpty = false;
            return false;
        }

        public boolean getResult() {
            return this.mEmpty;
        }
    }

    public static class NameCardData implements EntryElement {
        private final byte[] mBytes;
        private final String mFormat;
        private Integer mHashCode = null;
        private final boolean mIsPrimary;
        private final String mSide;

        public NameCardData(String format, byte[] photoBytes, boolean isPrimary, String side) {
            this.mFormat = format;
            this.mBytes = photoBytes;
            this.mIsPrimary = isPrimary;
            this.mSide = side;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder;
            if (TextUtils.equals(this.mSide, "FRONT")) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValue("data15", this.mBytes);
                builder.withValueBackReference("raw_contact_id", backReferenceIndex);
                builder.withValue("mimetype", "vnd.sec.cursor.item/name_card");
                if (this.mIsPrimary) {
                    builder.withValue("is_primary", Integer.valueOf(1));
                }
                operationList.add(builder.build());
            } else if (TextUtils.equals(this.mSide, "BACK")) {
                builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
                builder.withSelection("raw_contact_id= ? AND mimetype = \"vnd.sec.cursor.item/name_card\"", new String[1]);
                builder.withSelectionBackReference(0, backReferenceIndex);
                builder.withValue("data13", this.mBytes);
                if (this.mIsPrimary) {
                    builder.withValue("is_primary", Integer.valueOf(1));
                }
                operationList.add(builder.build());
            }
        }

        public boolean isEmpty() {
            return this.mBytes == null || this.mBytes.length == 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NameCardData)) {
                return false;
            }
            NameCardData NameCardData = (NameCardData) obj;
            if (TextUtils.equals(this.mFormat, NameCardData.mFormat) && Arrays.equals(this.mBytes, NameCardData.mBytes) && this.mIsPrimary == NameCardData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int i = 0;
            if (this.mHashCode != null) {
                return this.mHashCode.intValue();
            }
            int hash;
            if (this.mFormat != null) {
                hash = this.mFormat.hashCode();
            } else {
                hash = 0;
            }
            hash *= 31;
            if (this.mBytes != null) {
                byte[] bArr = this.mBytes;
                while (i < bArr.length) {
                    hash += bArr[i];
                    i++;
                }
            }
            hash = (hash * 31) + (this.mIsPrimary ? 1231 : 1237);
            this.mHashCode = Integer.valueOf(hash);
            return hash;
        }

        public String toString() {
            return String.format("format: %s: size: %d, isPrimary: %s", new Object[]{this.mFormat, Integer.valueOf(this.mBytes.length), Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.NAMECARD;
        }

        public String getFormat() {
            return this.mFormat;
        }

        public byte[] getBytes() {
            return this.mBytes;
        }
    }

    public static class NameData implements EntryElement {
        public String displayName;
        private String mFamily;
        private String mFormatted;
        private String mGiven;
        private String mMiddle;
        private String mPhoneticFamily;
        private String mPhoneticGiven;
        private String mPhoneticMiddle;
        private String mPrefix;
        private String mSortString;
        private String mSuffix;

        public boolean emptyStructuredName() {
            return TextUtils.isEmpty(this.mFamily) && TextUtils.isEmpty(this.mGiven) && TextUtils.isEmpty(this.mMiddle) && TextUtils.isEmpty(this.mPrefix) && TextUtils.isEmpty(this.mSuffix);
        }

        public boolean emptyPhoneticStructuredName() {
            return TextUtils.isEmpty(this.mPhoneticFamily) && TextUtils.isEmpty(this.mPhoneticGiven) && TextUtils.isEmpty(this.mPhoneticMiddle);
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/name");
            if (!TextUtils.isEmpty(this.mGiven)) {
                builder.withValue("data2", this.mGiven);
            }
            if (!TextUtils.isEmpty(this.mFamily)) {
                builder.withValue("data3", this.mFamily);
            }
            if (!TextUtils.isEmpty(this.mMiddle)) {
                builder.withValue("data5", this.mMiddle);
            }
            if (!TextUtils.isEmpty(this.mPrefix)) {
                builder.withValue("data4", this.mPrefix);
            }
            if (!TextUtils.isEmpty(this.mSuffix)) {
                builder.withValue("data6", this.mSuffix);
            }
            boolean phoneticNameSpecified = false;
            if (!TextUtils.isEmpty(this.mPhoneticGiven)) {
                builder.withValue("data7", this.mPhoneticGiven);
                phoneticNameSpecified = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticFamily)) {
                builder.withValue("data9", this.mPhoneticFamily);
                phoneticNameSpecified = true;
            }
            if (!TextUtils.isEmpty(this.mPhoneticMiddle)) {
                builder.withValue("data8", this.mPhoneticMiddle);
                phoneticNameSpecified = true;
            }
            if (!phoneticNameSpecified) {
                builder.withValue("data7", this.mSortString);
            }
            builder.withValue("data1", this.displayName);
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mFamily) && TextUtils.isEmpty(this.mMiddle) && TextUtils.isEmpty(this.mGiven) && TextUtils.isEmpty(this.mPrefix) && TextUtils.isEmpty(this.mSuffix) && TextUtils.isEmpty(this.mFormatted) && TextUtils.isEmpty(this.mPhoneticFamily) && TextUtils.isEmpty(this.mPhoneticMiddle) && TextUtils.isEmpty(this.mPhoneticGiven) && TextUtils.isEmpty(this.mSortString);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NameData)) {
                return false;
            }
            NameData nameData = (NameData) obj;
            if (TextUtils.equals(this.mFamily, nameData.mFamily) && TextUtils.equals(this.mMiddle, nameData.mMiddle) && TextUtils.equals(this.mGiven, nameData.mGiven) && TextUtils.equals(this.mPrefix, nameData.mPrefix) && TextUtils.equals(this.mSuffix, nameData.mSuffix) && TextUtils.equals(this.mFormatted, nameData.mFormatted) && TextUtils.equals(this.mPhoneticFamily, nameData.mPhoneticFamily) && TextUtils.equals(this.mPhoneticMiddle, nameData.mPhoneticMiddle) && TextUtils.equals(this.mPhoneticGiven, nameData.mPhoneticGiven) && TextUtils.equals(this.mSortString, nameData.mSortString)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hash = 0;
            for (String hashTarget : new String[]{this.mFamily, this.mMiddle, this.mGiven, this.mPrefix, this.mSuffix, this.mFormatted, this.mPhoneticFamily, this.mPhoneticMiddle, this.mPhoneticGiven, this.mSortString}) {
                int hashCode;
                int i = hash * 31;
                if (hashTarget != null) {
                    hashCode = hashTarget.hashCode();
                } else {
                    hashCode = 0;
                }
                hash = i + hashCode;
            }
            return hash;
        }

        public String toString() {
            return String.format("family: %s, given: %s, middle: %s, prefix: %s, suffix: %s", new Object[]{this.mFamily, this.mGiven, this.mMiddle, this.mPrefix, this.mSuffix});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.NAME;
        }

        public String getFamily() {
            return this.mFamily;
        }

        public String getMiddle() {
            return this.mMiddle;
        }

        public String getGiven() {
            return this.mGiven;
        }

        public String getPrefix() {
            return this.mPrefix;
        }

        public String getSuffix() {
            return this.mSuffix;
        }

        public String getFormatted() {
            return this.mFormatted;
        }

        public String getSortString() {
            return this.mSortString;
        }

        public void setFamily(String family) {
            this.mFamily = family;
        }

        public void setMiddle(String middle) {
            this.mMiddle = middle;
        }

        public void setGiven(String given) {
            this.mGiven = given;
        }

        public void setPrefix(String prefix) {
            this.mPrefix = prefix;
        }

        public void setSuffix(String suffix) {
            this.mSuffix = suffix;
        }
    }

    public static class NicknameData implements EntryElement {
        private final String mNickname;

        public NicknameData(String nickname) {
            this.mNickname = nickname;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/nickname");
            builder.withValue("data2", Integer.valueOf(1));
            builder.withValue("data1", this.mNickname);
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNickname);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof NicknameData)) {
                return false;
            }
            return TextUtils.equals(this.mNickname, ((NicknameData) obj).mNickname);
        }

        public int hashCode() {
            return this.mNickname != null ? this.mNickname.hashCode() : 0;
        }

        public String toString() {
            return "nickname: " + this.mNickname;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.NICKNAME;
        }

        public String getNickname() {
            return this.mNickname;
        }
    }

    public static class NoteData implements EntryElement {
        public final String mNote;

        public NoteData(String note) {
            this.mNote = note;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/note");
            builder.withValue("data1", this.mNote);
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNote);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NoteData)) {
                return false;
            }
            return TextUtils.equals(this.mNote, ((NoteData) obj).mNote);
        }

        public int hashCode() {
            return this.mNote != null ? this.mNote.hashCode() : 0;
        }

        public String toString() {
            return "note: " + this.mNote;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.NOTE;
        }

        public String getNote() {
            return this.mNote;
        }
    }

    public static class OrganizationData implements EntryElement {
        private String mDepartmentName;
        private boolean mIsPrimary;
        private String mJobDescription;
        private String mLabel;
        private String mOfficeLocation;
        private String mOrganizationName;
        private String mPhoneticName;
        private String mSymbol;
        private String mTitle;
        private final int mType;

        public OrganizationData(int type, String organizationName, String departmentName, String titleName, String phoneticName, boolean isPrimary) {
            this(type, null, organizationName, departmentName, titleName, null, null, phoneticName, null, isPrimary);
        }

        public OrganizationData(int type, String label, String organizationName, String departmentName, String titleName, String jobDescription, String symbol, String phoneticName, String officeLocation, boolean isPrimary) {
            this.mType = type;
            this.mLabel = label;
            this.mOrganizationName = organizationName;
            this.mDepartmentName = departmentName;
            this.mTitle = titleName;
            this.mJobDescription = jobDescription;
            this.mSymbol = symbol;
            this.mPhoneticName = phoneticName;
            this.mOfficeLocation = officeLocation;
            this.mIsPrimary = isPrimary;
        }

        public OrganizationData(String organizationName, String departmentName, String titleName, String phoneticName, int type, boolean isPrimary) {
            this.mType = type;
            this.mOrganizationName = organizationName;
            this.mDepartmentName = departmentName;
            this.mTitle = titleName;
            this.mPhoneticName = phoneticName;
            this.mIsPrimary = isPrimary;
            this.mLabel = null;
            this.mJobDescription = null;
            this.mSymbol = null;
            this.mOfficeLocation = null;
        }

        public String getFormattedString() {
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(this.mOrganizationName)) {
                builder.append(this.mOrganizationName);
            }
            if (!TextUtils.isEmpty(this.mDepartmentName)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(this.mDepartmentName);
            }
            if (!TextUtils.isEmpty(this.mTitle)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(this.mTitle);
            }
            return builder.toString();
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/organization");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mOrganizationName != null) {
                builder.withValue("data1", this.mOrganizationName);
            }
            if (this.mDepartmentName != null) {
                builder.withValue("data5", this.mDepartmentName);
            }
            if (this.mTitle != null) {
                builder.withValue("data4", this.mTitle);
            }
            if (this.mPhoneticName != null) {
                builder.withValue("data8", this.mPhoneticName);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
            if (VCardConfig.isJapanSpacialized()) {
                if (this.mJobDescription != null) {
                    builder.withValue("data6", this.mJobDescription);
                }
                if (this.mSymbol != null) {
                    builder.withValue("data7", this.mSymbol);
                }
                if (this.mOfficeLocation != null) {
                    builder.withValue("data9", this.mOfficeLocation);
                }
            }
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mOrganizationName) && TextUtils.isEmpty(this.mDepartmentName) && TextUtils.isEmpty(this.mTitle) && TextUtils.isEmpty(this.mPhoneticName);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OrganizationData)) {
                return false;
            }
            OrganizationData organization = (OrganizationData) obj;
            if (this.mType == organization.mType && TextUtils.equals(this.mOrganizationName, organization.mOrganizationName) && TextUtils.equals(this.mDepartmentName, organization.mDepartmentName) && TextUtils.equals(this.mTitle, organization.mTitle) && TextUtils.equals(this.mJobDescription, organization.mJobDescription) && TextUtils.equals(this.mSymbol, organization.mSymbol) && TextUtils.equals(this.mPhoneticName, organization.mPhoneticName) && TextUtils.equals(this.mOfficeLocation, organization.mOfficeLocation) && this.mIsPrimary == organization.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = 0;
            int i2 = this.mType * 31;
            if (this.mOrganizationName != null) {
                hashCode = this.mOrganizationName.hashCode();
            } else {
                hashCode = 0;
            }
            i2 = (i2 + hashCode) * 31;
            if (this.mDepartmentName != null) {
                hashCode = this.mDepartmentName.hashCode();
            } else {
                hashCode = 0;
            }
            hashCode = (i2 + hashCode) * 31;
            if (this.mTitle != null) {
                i = this.mTitle.hashCode();
            }
            return ((hashCode + i) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, organization: %s, department: %s, title: %s, isPrimary: %s", new Object[]{Integer.valueOf(this.mType), this.mOrganizationName, this.mDepartmentName, this.mTitle, Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.ORGANIZATION;
        }

        public String getOrganizationName() {
            return this.mOrganizationName;
        }

        public String getDepartmentName() {
            return this.mDepartmentName;
        }

        public String getTitle() {
            return this.mTitle;
        }

        public String getPhoneticName() {
            return this.mPhoneticName;
        }

        public int getType() {
            return this.mType;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    public static class PhoneData implements EntryElement {
        private boolean mIsPrimary;
        private final String mLabel;
        private final String mNumber;
        private final int mType;

        public PhoneData(String data, int type, String label, boolean isPrimary) {
            this.mNumber = data;
            this.mType = type;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/phone_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            builder.withValue("data1", this.mNumber);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mNumber);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PhoneData)) {
                return false;
            }
            PhoneData phoneData = (PhoneData) obj;
            if (this.mType == phoneData.mType && TextUtils.equals(this.mNumber, phoneData.mNumber) && TextUtils.equals(this.mLabel, phoneData.mLabel) && this.mIsPrimary == phoneData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = 0;
            int i2 = this.mType * 31;
            if (this.mNumber != null) {
                hashCode = this.mNumber.hashCode();
            } else {
                hashCode = 0;
            }
            hashCode = (i2 + hashCode) * 31;
            if (this.mLabel != null) {
                i = this.mLabel.hashCode();
            }
            return ((hashCode + i) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return String.format("type: %d, data: %s, label: %s, isPrimary: %s", new Object[]{Integer.valueOf(this.mType), this.mNumber, this.mLabel, Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.PHONE;
        }

        public String getNumber() {
            return this.mNumber;
        }

        public int getType() {
            return this.mType;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    public static class PhotoData implements EntryElement {
        private final byte[] mBytes;
        private final String mFormat;
        private Integer mHashCode = null;
        private final boolean mIsPrimary;

        public PhotoData(String format, byte[] photoBytes, boolean isPrimary) {
            this.mFormat = format;
            this.mBytes = photoBytes;
            this.mIsPrimary = isPrimary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/photo");
            builder.withValue("data15", this.mBytes);
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return this.mBytes == null || this.mBytes.length == 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PhotoData)) {
                return false;
            }
            PhotoData photoData = (PhotoData) obj;
            if (TextUtils.equals(this.mFormat, photoData.mFormat) && Arrays.equals(this.mBytes, photoData.mBytes) && this.mIsPrimary == photoData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int i = 0;
            if (this.mHashCode != null) {
                return this.mHashCode.intValue();
            }
            int hash;
            if (this.mFormat != null) {
                hash = this.mFormat.hashCode();
            } else {
                hash = 0;
            }
            hash *= 31;
            if (this.mBytes != null) {
                byte[] bArr = this.mBytes;
                while (i < bArr.length) {
                    hash += bArr[i];
                    i++;
                }
            }
            hash = (hash * 31) + (this.mIsPrimary ? 1231 : 1237);
            this.mHashCode = Integer.valueOf(hash);
            return hash;
        }

        public String toString() {
            return String.format("format: %s: size: %d, isPrimary: %s", new Object[]{this.mFormat, Integer.valueOf(this.mBytes.length), Boolean.valueOf(this.mIsPrimary)});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.PHOTO;
        }

        public String getFormat() {
            return this.mFormat;
        }

        public byte[] getBytes() {
            return this.mBytes;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    public static class PhotoStateData implements EntryElement {
        public final String mPhotoState;

        public PhotoStateData(String photoState) {
            this.mPhotoState = photoState;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
            builder.withSelection("raw_contact_id= ? AND mimetype = \"vnd.android.cursor.item/photo\"", new String[1]);
            builder.withSelectionBackReference(0, backReferenceIndex);
            builder.withValue("data11", this.mPhotoState);
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mPhotoState);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PhotoStateData)) {
                return false;
            }
            return TextUtils.equals(this.mPhotoState, ((PhotoStateData) obj).mPhotoState);
        }

        public int hashCode() {
            return this.mPhotoState != null ? this.mPhotoState.hashCode() : 0;
        }

        public String toString() {
            return "photoState: " + this.mPhotoState;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.PHOTOSTATE;
        }

        public String getPhotoState() {
            return this.mPhotoState;
        }
    }

    public static class PostalData implements EntryElement {
        private static final int ADDR_MAX_DATA_SIZE = 7;
        private final String mCountry;
        private final String mExtendedAddress;
        private String mFormattedAddress;
        private boolean mIsPrimary;
        private final String mLabel;
        private final String mLocalty;
        private final String mPobox;
        private final String mPostalCode;
        private final String mRegion;
        private final String mStreet;
        private final int mType;
        private int mVCardType;

        public PostalData(String pobox, String extendedAddress, String street, String localty, String region, String postalCode, String country, int type, String label, boolean isPrimary, int vcardType) {
            this.mType = type;
            this.mPobox = pobox;
            this.mExtendedAddress = extendedAddress;
            this.mStreet = street;
            this.mLocalty = localty;
            this.mRegion = region;
            this.mPostalCode = postalCode;
            this.mCountry = country;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
            this.mVCardType = vcardType;
        }

        public static PostalData constructPostalData(List<String> propValueList, int type, String label, boolean isPrimary, int vcardType) {
            int i;
            String[] dataArray = new String[7];
            int size = propValueList.size();
            if (size > 7) {
                size = 7;
            }
            int i2 = 0;
            for (String addressElement : propValueList) {
                dataArray[i2] = addressElement;
                i2++;
                if (i2 >= size) {
                    i = i2;
                    break;
                }
            }
            i = i2;
            while (i < 7) {
                i2 = i + 1;
                dataArray[i] = null;
                i = i2;
            }
            return new PostalData(dataArray[0], dataArray[1], dataArray[2], dataArray[3], dataArray[4], dataArray[5], dataArray[6], type, label, isPrimary, vcardType);
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            String streetString;
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/postal-address_v2");
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            if (TextUtils.isEmpty(this.mStreet)) {
                if (TextUtils.isEmpty(this.mExtendedAddress)) {
                    streetString = null;
                } else {
                    streetString = this.mExtendedAddress;
                }
            } else if (TextUtils.isEmpty(this.mExtendedAddress)) {
                streetString = this.mStreet;
            } else {
                streetString = this.mStreet + " " + this.mExtendedAddress;
            }
            builder.withValue("data5", this.mPobox);
            if (VCardConfig.isJapanSpacialized()) {
                builder.withValue("data4", this.mStreet);
                builder.withValue("data6", this.mExtendedAddress);
            } else {
                builder.withValue("data4", streetString);
            }
            builder.withValue("data7", this.mLocalty);
            builder.withValue("data8", this.mRegion);
            builder.withValue("data9", this.mPostalCode);
            builder.withValue("data10", this.mCountry);
            this.mFormattedAddress = getFormattedAddress(this.mVCardType);
            if (this.mFormattedAddress != null) {
                builder.withValue("data1", this.mFormattedAddress);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Integer.valueOf(1));
            }
            operationList.add(builder.build());
        }

        public String getFormattedAddress(int vcardType) {
            StringBuilder builder = new StringBuilder();
            boolean empty = true;
            String[] dataArray = new String[]{this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry};
            boolean useJapaneseOrder = Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
            boolean useKoreanOrder = Locale.KOREAN.getLanguage().equals(Locale.getDefault().getLanguage());
            if (!VCardConfig.isJapanSpacialized()) {
                int i;
                String addressPart;
                if (VCardConfig.isJapaneseDevice(vcardType) || vcardType == VCardConfig.VCARD_TYPE_V21_COREA) {
                    for (i = 6; i >= 0; i--) {
                        addressPart = dataArray[i];
                        if (!TextUtils.isEmpty(addressPart)) {
                            if (empty) {
                                empty = false;
                            } else {
                                builder.append(' ');
                            }
                            builder.append(addressPart);
                        }
                    }
                } else {
                    for (i = 0; i < 7; i++) {
                        addressPart = dataArray[i];
                        if (!TextUtils.isEmpty(addressPart)) {
                            if (empty) {
                                empty = false;
                            } else {
                                builder.append(' ');
                            }
                            builder.append(addressPart);
                        }
                    }
                }
                return builder.toString().trim();
            } else if (useJapaneseOrder) {
                if (!TextUtils.isEmpty(dataArray[5])) {
                    builder.append(dataArray[5]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[4])) {
                    builder.append(dataArray[4]);
                }
                if (!(TextUtils.isEmpty(dataArray[5]) && TextUtils.isEmpty(dataArray[4]))) {
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[3])) {
                    builder.append(dataArray[3]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[2])) {
                    builder.append(dataArray[2]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[1])) {
                    builder.append(dataArray[1]);
                }
                if (!(TextUtils.isEmpty(dataArray[3]) && TextUtils.isEmpty(dataArray[2]) && TextUtils.isEmpty(dataArray[1]))) {
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[0])) {
                    builder.append(dataArray[0]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[6])) {
                    builder.append(dataArray[6]);
                }
                return builder.toString().trim();
            } else if (useKoreanOrder) {
                if (!TextUtils.isEmpty(dataArray[6])) {
                    builder.append(dataArray[6]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[5])) {
                    builder.append(dataArray[5]);
                }
                if (!(TextUtils.isEmpty(dataArray[6]) && TextUtils.isEmpty(dataArray[5]))) {
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[4])) {
                    builder.append(dataArray[4]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[3])) {
                    builder.append(dataArray[3]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[1])) {
                    builder.append(dataArray[1]);
                }
                if (!(TextUtils.isEmpty(dataArray[4]) && TextUtils.isEmpty(dataArray[3]) && TextUtils.isEmpty(dataArray[1]))) {
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[2])) {
                    builder.append(dataArray[2]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[0])) {
                    builder.append(dataArray[0]);
                }
                return builder.toString().trim();
            } else {
                if (!TextUtils.isEmpty(dataArray[2])) {
                    builder.append(dataArray[2]).append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[0])) {
                    builder.append(dataArray[0]).append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[1])) {
                    builder.append(dataArray[1]).append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[3])) {
                    builder.append(dataArray[3]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[4])) {
                    builder.append(dataArray[4]).append(" ");
                }
                if (!TextUtils.isEmpty(dataArray[5])) {
                    builder.append(dataArray[5]);
                }
                if (!(TextUtils.isEmpty(dataArray[3]) && TextUtils.isEmpty(dataArray[4]) && TextUtils.isEmpty(dataArray[5]))) {
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(dataArray[6])) {
                    builder.append(dataArray[6]);
                }
                return builder.toString().trim();
            }
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mPobox) && TextUtils.isEmpty(this.mExtendedAddress) && TextUtils.isEmpty(this.mStreet) && TextUtils.isEmpty(this.mLocalty) && TextUtils.isEmpty(this.mRegion) && TextUtils.isEmpty(this.mPostalCode) && TextUtils.isEmpty(this.mCountry);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PostalData)) {
                return false;
            }
            PostalData postalData = (PostalData) obj;
            if (this.mType == postalData.mType && ((this.mType != 0 || TextUtils.equals(this.mLabel, postalData.mLabel)) && this.mIsPrimary == postalData.mIsPrimary && TextUtils.equals(this.mPobox, postalData.mPobox) && TextUtils.equals(this.mExtendedAddress, postalData.mExtendedAddress) && TextUtils.equals(this.mStreet, postalData.mStreet) && TextUtils.equals(this.mLocalty, postalData.mLocalty) && TextUtils.equals(this.mRegion, postalData.mRegion) && TextUtils.equals(this.mPostalCode, postalData.mPostalCode) && TextUtils.equals(this.mCountry, postalData.mCountry))) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = this.mType * 31;
            if (this.mLabel != null) {
                hashCode = this.mLabel.hashCode();
            } else {
                hashCode = 0;
            }
            int hash = ((i + hashCode) * 31) + (this.mIsPrimary ? 1231 : 1237);
            for (String hashTarget : new String[]{this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry}) {
                int i2 = hash * 31;
                if (hashTarget != null) {
                    hashCode = hashTarget.hashCode();
                } else {
                    hashCode = 0;
                }
                hash = i2 + hashCode;
            }
            return hash;
        }

        public String toString() {
            return String.format("type: %d, label: %s, isPrimary: %s, pobox: %s, extendedAddress: %s, street: %s, localty: %s, region: %s, postalCode %s, country: %s", new Object[]{Integer.valueOf(this.mType), this.mLabel, Boolean.valueOf(this.mIsPrimary), this.mPobox, this.mExtendedAddress, this.mStreet, this.mLocalty, this.mRegion, this.mPostalCode, this.mCountry});
        }

        public final EntryLabel getEntryLabel() {
            return EntryLabel.POSTAL_ADDRESS;
        }

        public String getPobox() {
            return this.mPobox;
        }

        public String getExtendedAddress() {
            return this.mExtendedAddress;
        }

        public String getStreet() {
            return this.mStreet;
        }

        public String getLocalty() {
            return this.mLocalty;
        }

        public String getRegion() {
            return this.mRegion;
        }

        public String getPostalCode() {
            return this.mPostalCode;
        }

        public String getCountry() {
            return this.mCountry;
        }

        public int getType() {
            return this.mType;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public boolean isPrimary() {
            return this.mIsPrimary;
        }
    }

    public static class SipData implements EntryElement {
        private final String mAddress;
        private final boolean mIsPrimary;
        private final String mLabel;
        private final int mType;

        public SipData(String rawSip, int type, String label, boolean isPrimary) {
            if (rawSip.startsWith("sip:")) {
                this.mAddress = rawSip.substring(4);
            } else {
                this.mAddress = rawSip;
            }
            this.mType = type;
            this.mLabel = label;
            this.mIsPrimary = isPrimary;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/sip_address");
            builder.withValue("data1", this.mAddress);
            builder.withValue("data2", Integer.valueOf(this.mType));
            if (this.mType == 0) {
                builder.withValue("data3", this.mLabel);
            }
            if (this.mIsPrimary) {
                builder.withValue("is_primary", Boolean.valueOf(this.mIsPrimary));
            }
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mAddress);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SipData)) {
                return false;
            }
            SipData sipData = (SipData) obj;
            if (this.mType == sipData.mType && TextUtils.equals(this.mLabel, sipData.mLabel) && TextUtils.equals(this.mAddress, sipData.mAddress) && this.mIsPrimary == sipData.mIsPrimary) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int hashCode;
            int i = 0;
            int i2 = this.mType * 31;
            if (this.mLabel != null) {
                hashCode = this.mLabel.hashCode();
            } else {
                hashCode = 0;
            }
            hashCode = (i2 + hashCode) * 31;
            if (this.mAddress != null) {
                i = this.mAddress.hashCode();
            }
            return ((hashCode + i) * 31) + (this.mIsPrimary ? 1231 : 1237);
        }

        public String toString() {
            return "sip: " + this.mAddress;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.SIP;
        }

        public String getAddress() {
            return this.mAddress;
        }

        public int getType() {
            return this.mType;
        }

        public String getLabel() {
            return this.mLabel;
        }
    }

    private class ToStringIterator implements EntryElementIterator {
        private StringBuilder mBuilder;
        private boolean mFirstElement;

        private ToStringIterator() {
        }

        public void onIterationStarted() {
            this.mBuilder = new StringBuilder();
            this.mBuilder.append("[[hash: " + VCardEntry.this.hashCode() + "\n");
        }

        public void onElementGroupStarted(EntryLabel label) {
            this.mBuilder.append(label.toString() + ": ");
            this.mFirstElement = true;
        }

        public boolean onElement(EntryElement elem) {
            if (!this.mFirstElement) {
                this.mBuilder.append(", ");
                this.mFirstElement = false;
            }
            this.mBuilder.append("[").append(elem.toString()).append("]");
            return true;
        }

        public void onElementGroupEnded() {
            this.mBuilder.append("\n");
        }

        public void onIterationEnded() {
            this.mBuilder.append("]]\n");
        }

        public String toString() {
            return this.mBuilder.toString();
        }
    }

    public static class WebsiteData implements EntryElement {
        private final String mWebsite;

        public WebsiteData(String website) {
            this.mWebsite = website;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/website");
            builder.withValue("data1", this.mWebsite);
            builder.withValue("data2", Integer.valueOf(1));
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.mWebsite);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WebsiteData)) {
                return false;
            }
            return TextUtils.equals(this.mWebsite, ((WebsiteData) obj).mWebsite);
        }

        public int hashCode() {
            return this.mWebsite != null ? this.mWebsite.hashCode() : 0;
        }

        public String toString() {
            return "website: " + this.mWebsite;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.WEBSITE;
        }

        public String getWebsite() {
            return this.mWebsite;
        }
    }

    public static class XGroupNameData implements EntryElement {
        public static ContentResolver mResover;
        private Account mGroupAccount = null;
        private String mGroupName = null;

        public XGroupNameData(String pGroupName, Account account, ContentResolver resolver) {
            this.mGroupName = pGroupName;
            this.mGroupAccount = account;
        }

        public EntryLabel getEntryLabel() {
            return EntryLabel.XGROUPNAME;
        }

        public void constructInsertOperation(List<ContentProviderOperation> operationList, int backReferenceIndex) {
            Builder builder = ContentProviderOperation.newInsert(Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "data/group_membership_only_raw_contact_id"));
            builder.withValue("account_name", this.mGroupAccount.name);
            builder.withValue(Key.ACCOUNT_TYPE, this.mGroupAccount.type);
            builder.withValueBackReference("raw_contact_id", backReferenceIndex);
            builder.withValue("mimetype", "vnd.android.cursor.item/group_membership");
            builder.withValue("title", this.mGroupName);
            operationList.add(builder.build());
        }

        public boolean isEmpty() {
            if (this.mGroupName == null) {
                return true;
            }
            return this.mGroupName.isEmpty();
        }

        public String toString() {
            return "group Name : " + this.mGroupName;
        }

        public int hashCode() {
            return this.mGroupName != null ? this.mGroupName.hashCode() : 0;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof XGroupNameData)) {
                return false;
            }
            return TextUtils.equals(this.mGroupName, ((XGroupNameData) obj).mGroupName);
        }
    }

    static {
        sImMap.put(VCardConstants.PROPERTY_X_AIM, Integer.valueOf(0));
        sImMap.put(VCardConstants.PROPERTY_X_MSN, Integer.valueOf(1));
        sImMap.put(VCardConstants.PROPERTY_X_YAHOO, Integer.valueOf(2));
        sImMap.put(VCardConstants.PROPERTY_X_ICQ, Integer.valueOf(6));
        sImMap.put(VCardConstants.PROPERTY_X_JABBER, Integer.valueOf(7));
        sImMap.put(VCardConstants.PROPERTY_X_SKYPE_USERNAME, Integer.valueOf(3));
        sImMap.put(VCardConstants.PROPERTY_X_QQ, Integer.valueOf(4));
        sImMap.put(VCardConstants.PROPERTY_X_GOOGLE_TALK, Integer.valueOf(5));
        sImMap.put(ImportOnly.PROPERTY_X_GOOGLE_TALK_WITH_SPACE, Integer.valueOf(5));
        sImMap.put(VCardConstants.PROPERTY_X_WHATSAPP, Integer.valueOf(9));
        sImMap.put(VCardConstants.PROPERTY_X_FACEBOOK, Integer.valueOf(10));
    }

    public final void iterateAllData(EntryElementIterator iterator) {
        iterator.onIterationStarted();
        iterator.onElementGroupStarted(this.mNameData.getEntryLabel());
        iterator.onElement(this.mNameData);
        iterator.onElementGroupEnded();
        iterateOneList(this.mPhoneList, iterator);
        iterateOneList(this.mEmailList, iterator);
        iterateOneList(this.mPostalList, iterator);
        iterateOneList(this.mOrganizationList, iterator);
        iterateOneList(this.mImList, iterator);
        iterateOneList(this.mPhotoList, iterator);
        iterateOneList(this.mPhotoStateList, iterator);
        iterateOneList(this.mNameCardList, iterator);
        iterateOneList(this.mWebsiteList, iterator);
        iterateOneList(this.mSipList, iterator);
        iterateOneList(this.mNicknameList, iterator);
        iterateOneList(this.mNoteList, iterator);
        iterateOneList(this.mAndroidCustomDataList, iterator);
        if (this.mBirthday != null) {
            iterator.onElementGroupStarted(this.mBirthday.getEntryLabel());
            iterator.onElement(this.mBirthday);
            iterator.onElementGroupEnded();
        }
        if (this.mAnniversary != null) {
            iterator.onElementGroupStarted(this.mAnniversary.getEntryLabel());
            iterator.onElement(this.mAnniversary);
            iterator.onElementGroupEnded();
        }
        if (VCardConfig.isChineseSpacialized() || SemCscFeature.getInstance().getBoolean("CscFeature_Contact_EnableDocomoAccountAsDefault")) {
            XGroupNameData.mResover = this.mContentsResolver;
            iterateOneList(this.mXGroupNameList, iterator);
        }
        iterator.onIterationEnded();
    }

    private void iterateOneList(List<? extends EntryElement> elemList, EntryElementIterator iterator) {
        if (elemList != null && elemList.size() > 0) {
            iterator.onElementGroupStarted(((EntryElement) elemList.get(0)).getEntryLabel());
            for (EntryElement elem : elemList) {
                iterator.onElement(elem);
            }
            iterator.onElementGroupEnded();
        }
    }

    public String toString() {
        ToStringIterator iterator = new ToStringIterator();
        iterateAllData(iterator);
        return iterator.toString();
    }

    public VCardEntry() {
        this(VCardConfig.VCARD_TYPE_V21_GENERIC);
    }

    public VCardEntry(int vcardType) {
        this(vcardType, null);
    }

    public VCardEntry(int vcardType, Account account) {
        this.mNameData = new NameData();
        this.mXGroupNameList = null;
        this.mVCardType = vcardType;
        this.mAccount = account;
    }

    private void addPhone(int type, String data, String label, boolean isPrimary) {
        String formattedNumber;
        if (this.mPhoneList == null) {
            this.mPhoneList = new ArrayList();
        }
        StringBuilder builder = new StringBuilder();
        String trimmed = data.trim();
        if (type == 6 || VCardConfig.refrainPhoneNumberFormatting(this.mVCardType)) {
            formattedNumber = trimmed;
        } else {
            int length = trimmed.length();
            for (int i = 0; i < length; i++) {
                char ch = trimmed.charAt(i);
                if (ch == 'p' || ch == 'P') {
                    builder.append(',');
                } else if (ch == 'w' || ch == 'W') {
                    builder.append(';');
                } else {
                    builder.append(ch);
                }
            }
            formattedNumber = builder.toString();
        }
        this.mPhoneList.add(new PhoneData(formattedNumber, type, label, isPrimary));
    }

    private void addSip(String sipData, int type, String label, boolean isPrimary) {
        if (this.mSipList == null) {
            this.mSipList = new ArrayList();
        }
        this.mSipList.add(new SipData(sipData, type, label, isPrimary));
    }

    private void addNickName(String nickName) {
        if (this.mNicknameList == null) {
            this.mNicknameList = new ArrayList();
        }
        this.mNicknameList.add(new NicknameData(nickName));
    }

    private void addEmail(int type, String data, String label, boolean isPrimary) {
        if (this.mEmailList == null) {
            this.mEmailList = new ArrayList();
        }
        this.mEmailList.add(new EmailData(data, type, label, isPrimary));
    }

    private void addPostal(int type, List<String> propValueList, String label, boolean isPrimary) {
        if (this.mPostalList == null) {
            this.mPostalList = new ArrayList(0);
        }
        this.mPostalList.add(PostalData.constructPostalData(propValueList, type, label, isPrimary, this.mVCardType));
    }

    private void addNewOrganization(String organizationName, String departmentName, String titleName, String phoneticName, int type, boolean isPrimary) {
        if (this.mOrganizationList == null) {
            this.mOrganizationList = new ArrayList();
        }
        this.mOrganizationList.add(new OrganizationData(organizationName, departmentName, titleName, phoneticName, type, isPrimary));
    }

    private void addNewOrganization(int type, String label, String companyName, String departmentName, String titleName, String jobDescription, String symbol, String phoneticName, String officeLocation, boolean isPrimary) {
        if (this.mOrganizationList == null) {
            this.mOrganizationList = new ArrayList();
        }
        this.mOrganizationList.add(new OrganizationData(type, label, companyName, departmentName, titleName, jobDescription, symbol, phoneticName, officeLocation, isPrimary));
    }

    private String buildSinglePhoneticNameFromSortAsParam(Map<String, Collection<String>> paramMap) {
        Collection<String> sortAsCollection = (Collection) paramMap.get(VCardConstants.PARAM_SORT_AS);
        if (sortAsCollection == null || sortAsCollection.size() == 0) {
            return null;
        }
        if (sortAsCollection.size() > 1) {
            Log.w(LOG_TAG, "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(sortAsCollection.toArray()));
        }
        List<String> sortNames = VCardUtils.constructListFromValue((String) sortAsCollection.iterator().next(), this.mVCardType);
        StringBuilder builder = new StringBuilder();
        for (String elem : sortNames) {
            builder.append(elem);
        }
        return builder.toString();
    }

    private void handleOrgValue(int type, List<String> orgList, Map<String, Collection<String>> paramMap, boolean isPrimary) {
        String departmentName;
        String title;
        String jobDescription;
        String symbol;
        String officeLocation;
        String phoneticName = buildSinglePhoneticNameFromSortAsParam(paramMap);
        if (orgList == null) {
            orgList = sEmptyList;
        }
        int size = orgList.size();
        String organizationName;
        if (VCardConfig.isJapanSpacialized()) {
            switch (size) {
                case 0:
                    organizationName = "";
                    departmentName = null;
                    title = null;
                    jobDescription = null;
                    symbol = null;
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 1:
                    organizationName = (String) orgList.get(0);
                    departmentName = null;
                    title = null;
                    jobDescription = null;
                    symbol = null;
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 2:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = null;
                    jobDescription = null;
                    symbol = null;
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 3:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = (String) orgList.get(2);
                    jobDescription = null;
                    symbol = null;
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 4:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = (String) orgList.get(2);
                    jobDescription = (String) orgList.get(3);
                    symbol = null;
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 5:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = (String) orgList.get(2);
                    jobDescription = (String) orgList.get(3);
                    symbol = (String) orgList.get(4);
                    phoneticName = null;
                    officeLocation = null;
                    break;
                case 6:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = (String) orgList.get(2);
                    jobDescription = (String) orgList.get(3);
                    symbol = (String) orgList.get(4);
                    phoneticName = (String) orgList.get(5);
                    officeLocation = null;
                    break;
                case 7:
                    organizationName = (String) orgList.get(0);
                    departmentName = (String) orgList.get(1);
                    title = (String) orgList.get(2);
                    jobDescription = (String) orgList.get(3);
                    symbol = (String) orgList.get(4);
                    phoneticName = (String) orgList.get(5);
                    officeLocation = (String) orgList.get(6);
                    break;
                default:
                    organizationName = "";
                    if (!(orgList.get(0) == null || TextUtils.isEmpty((CharSequence) orgList.get(0)))) {
                        organizationName = TextUtils.isEmpty(organizationName) ? (String) orgList.get(0) : new StringBuilder(String.valueOf(organizationName)).append(" ").append((String) orgList.get(0)).toString();
                    }
                    if (!(orgList.get(1) == null || TextUtils.isEmpty((CharSequence) orgList.get(1)))) {
                        if (TextUtils.isEmpty(organizationName)) {
                            organizationName = (String) orgList.get(1);
                        } else {
                            organizationName = new StringBuilder(String.valueOf(organizationName)).append(" ").append((String) orgList.get(1)).toString();
                        }
                    }
                    if (!(orgList.get(3) == null || TextUtils.isEmpty((CharSequence) orgList.get(3)))) {
                        if (TextUtils.isEmpty(organizationName)) {
                            organizationName = (String) orgList.get(3);
                        } else {
                            organizationName = new StringBuilder(String.valueOf(organizationName)).append(" ").append((String) orgList.get(3)).toString();
                        }
                    }
                    if (!(orgList.get(6) == null || TextUtils.isEmpty((CharSequence) orgList.get(6)))) {
                        if (TextUtils.isEmpty(organizationName)) {
                            organizationName = (String) orgList.get(6);
                        } else {
                            organizationName = new StringBuilder(String.valueOf(organizationName)).append(" ").append((String) orgList.get(6)).toString();
                        }
                    }
                    departmentName = null;
                    title = (String) orgList.get(2);
                    jobDescription = null;
                    symbol = (String) orgList.get(4);
                    phoneticName = (String) orgList.get(5);
                    officeLocation = null;
                    break;
            }
        }
        switch (size) {
            case 0:
                organizationName = "";
                departmentName = null;
                break;
            case 1:
                organizationName = (String) orgList.get(0);
                departmentName = null;
                break;
            default:
                organizationName = (String) orgList.get(0);
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < size; i++) {
                    if (i > 1) {
                        builder.append(' ');
                    }
                    builder.append((String) orgList.get(i));
                }
                departmentName = builder.toString();
                break;
        }
        title = null;
        jobDescription = null;
        symbol = null;
        officeLocation = null;
        if (this.mOrganizationList != null) {
            for (OrganizationData organizationData : this.mOrganizationList) {
                if (VCardConfig.isJapanSpacialized() && organizationData.mOrganizationName == null && organizationData.mDepartmentName == null && organizationData.mTitle == null && organizationData.mJobDescription == null && organizationData.mSymbol == null && organizationData.mPhoneticName == null && organizationData.mOfficeLocation == null) {
                    organizationData.mOrganizationName = organizationName;
                    organizationData.mDepartmentName = departmentName;
                    organizationData.mTitle = title;
                    organizationData.mJobDescription = jobDescription;
                    organizationData.mSymbol = symbol;
                    organizationData.mPhoneticName = phoneticName;
                    organizationData.mOfficeLocation = officeLocation;
                    organizationData.mIsPrimary = isPrimary;
                    return;
                } else if (organizationData.mOrganizationName == null && organizationData.mDepartmentName == null) {
                    organizationData.mOrganizationName = organizationName;
                    organizationData.mDepartmentName = departmentName;
                    organizationData.mIsPrimary = isPrimary;
                    return;
                }
            }
            addNewOrganization(organizationName, departmentName, null, phoneticName, type, isPrimary);
        } else if (VCardConfig.isJapanSpacialized()) {
            addNewOrganization(type, null, organizationName, departmentName, title, jobDescription, symbol, phoneticName, officeLocation, isPrimary);
        } else {
            addNewOrganization(organizationName, departmentName, null, phoneticName, type, isPrimary);
        }
    }

    private void handleTitleValue(String title) {
        if (this.mOrganizationList == null) {
            addNewOrganization(null, null, title, null, 1, false);
        } else if (VCardConfig.isJapanSpacialized()) {
            organizationData = (OrganizationData) this.mOrganizationList.get(this.mOrganizationList.size() - 1);
            if (organizationData.mTitle == null || "".equals(organizationData.mTitle) || organizationData.mTitle.isEmpty()) {
                organizationData.mTitle = title;
            }
        } else {
            for (OrganizationData organizationData : this.mOrganizationList) {
                if (organizationData.mTitle == null) {
                    organizationData.mTitle = title;
                    return;
                }
            }
            if (!VCardConfig.isJapanSpacialized()) {
                addNewOrganization(null, null, title, null, 1, false);
            }
        }
    }

    private void addIm(int protocol, String customProtocol, String propValue, int type, boolean isPrimary) {
        if (this.mImList == null) {
            this.mImList = new ArrayList();
        }
        this.mImList.add(new ImData(protocol, customProtocol, propValue, type, isPrimary));
    }

    private void addNote(String note) {
        if (this.mNoteList == null) {
            this.mNoteList = new ArrayList(1);
        }
        this.mNoteList.add(new NoteData(note));
    }

    private void addPhotoBytes(String formatName, byte[] photoBytes, boolean isPrimary) {
        if (this.mPhotoList == null) {
            this.mPhotoList = new ArrayList(1);
        }
        if (photoBytes == null || photoBytes.length != 0) {
            this.mPhotoList.add(new PhotoData(formatName, photoBytes, isPrimary));
        }
    }

    private void addPhotoState(String photoState) {
        if (this.mPhotoStateList == null) {
            this.mPhotoStateList = new ArrayList(1);
        }
        this.mPhotoStateList.add(new PhotoStateData(photoState));
    }

    private void addNameCardBytes(String formatName, byte[] nameCardBytes, boolean isPrimary, String side) {
        if (this.mNameCardList == null) {
            this.mNameCardList = new ArrayList(1);
        }
        if (nameCardBytes == null || nameCardBytes.length != 0) {
            this.mNameCardList.add(new NameCardData(formatName, nameCardBytes, isPrimary, side));
        }
    }

    private void tryHandleSortAsName(Map<String, Collection<String>> paramMap) {
        if (!VCardConfig.isVersion30(this.mVCardType) || (TextUtils.isEmpty(this.mNameData.mPhoneticFamily) && TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) && TextUtils.isEmpty(this.mNameData.mPhoneticGiven))) {
            Collection<String> sortAsCollection = (Collection) paramMap.get(VCardConstants.PARAM_SORT_AS);
            if (sortAsCollection != null && sortAsCollection.size() != 0) {
                if (sortAsCollection.size() > 1) {
                    Log.w(LOG_TAG, "Incorrect multiple SORT_AS parameters detected: " + Arrays.toString(sortAsCollection.toArray()));
                }
                List<String> sortNames = VCardUtils.constructListFromValue((String) sortAsCollection.iterator().next(), this.mVCardType);
                int size = sortNames.size();
                if (size > 3) {
                    size = 3;
                }
                switch (size) {
                    case 2:
                        break;
                    case 3:
                        this.mNameData.mPhoneticMiddle = (String) sortNames.get(2);
                        break;
                }
                this.mNameData.mPhoneticGiven = (String) sortNames.get(1);
                this.mNameData.mPhoneticFamily = (String) sortNames.get(0);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleNProperty(java.util.List<java.lang.String> r5, java.util.Map<java.lang.String, java.util.Collection<java.lang.String>> r6) {
        /*
        r4 = this;
        r3 = 1;
        r4.tryHandleSortAsName(r6);
        if (r5 == 0) goto L_0x000c;
    L_0x0006:
        r0 = r5.size();
        if (r0 >= r3) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r1 = 5;
        if (r0 <= r1) goto L_0x0011;
    L_0x0010:
        r0 = 5;
    L_0x0011:
        switch(r0) {
            case 2: goto L_0x0045;
            case 3: goto L_0x0039;
            case 4: goto L_0x002d;
            case 5: goto L_0x0021;
            default: goto L_0x0014;
        };
    L_0x0014:
        r2 = r4.mNameData;
        r1 = 0;
        r1 = r5.get(r1);
        r1 = (java.lang.String) r1;
        r2.mFamily = r1;
        goto L_0x000c;
    L_0x0021:
        r2 = r4.mNameData;
        r1 = 4;
        r1 = r5.get(r1);
        r1 = (java.lang.String) r1;
        r2.mSuffix = r1;
    L_0x002d:
        r2 = r4.mNameData;
        r1 = 3;
        r1 = r5.get(r1);
        r1 = (java.lang.String) r1;
        r2.mPrefix = r1;
    L_0x0039:
        r2 = r4.mNameData;
        r1 = 2;
        r1 = r5.get(r1);
        r1 = (java.lang.String) r1;
        r2.mMiddle = r1;
    L_0x0045:
        r2 = r4.mNameData;
        r1 = r5.get(r3);
        r1 = (java.lang.String) r1;
        r2.mGiven = r1;
        goto L_0x0014;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.vcard.VCardEntry.handleNProperty(java.util.List, java.util.Map):void");
    }

    private void handlePhoneticNameFromSound(List<String> elems) {
        if (TextUtils.isEmpty(this.mNameData.mPhoneticFamily) && TextUtils.isEmpty(this.mNameData.mPhoneticMiddle) && TextUtils.isEmpty(this.mNameData.mPhoneticGiven) && elems != null) {
            int size = elems.size();
            if (size >= 1) {
                if (size > 3) {
                    size = 3;
                }
                if (((String) elems.get(0)).length() > 0) {
                    boolean onlyFirstElemIsNonEmpty = true;
                    for (int i = 1; i < size; i++) {
                        if (((String) elems.get(i)).length() > 0) {
                            onlyFirstElemIsNonEmpty = false;
                            break;
                        }
                    }
                    if ((!VCardConfig.isJapanSpacialized() || VCardConfig.isShiftJisAsDefault()) && onlyFirstElemIsNonEmpty) {
                        String[] namesArray = ((String) elems.get(0)).split(" ");
                        int nameArrayLength = namesArray.length;
                        if (nameArrayLength == 3) {
                            this.mNameData.mPhoneticFamily = namesArray[0];
                            this.mNameData.mPhoneticMiddle = namesArray[1];
                            this.mNameData.mPhoneticGiven = namesArray[2];
                            return;
                        } else if (nameArrayLength == 2) {
                            this.mNameData.mPhoneticFamily = namesArray[0];
                            this.mNameData.mPhoneticGiven = namesArray[1];
                            return;
                        } else {
                            this.mNameData.mPhoneticGiven = (String) elems.get(0);
                            return;
                        }
                    }
                }
                switch (size) {
                    case 2:
                        break;
                    case 3:
                        this.mNameData.mPhoneticMiddle = (String) elems.get(2);
                        break;
                }
                this.mNameData.mPhoneticGiven = (String) elems.get(1);
                this.mNameData.mPhoneticFamily = (String) elems.get(0);
            }
        }
    }

    public void addProperty(VCardProperty property) {
        String propertyName = property.getName();
        Map<String, Collection<String>> paramMap = property.getParameterMap();
        List<String> propertyValueList = property.getValueList();
        byte[] propertyBytes = property.getByteValue();
        if ((propertyValueList != null && propertyValueList.size() != 0) || propertyBytes != null) {
            String propValue;
            if (propertyValueList != null) {
                propValue = listToString(propertyValueList).trim();
            } else {
                propValue = null;
            }
            if (!propertyName.equals(VCardConstants.PROPERTY_VERSION)) {
                if (propertyName.equals(VCardConstants.PROPERTY_FN)) {
                    if (VCardConfig.isJapanSpacialized()) {
                        propValue = listToString(VCardUtils.constructListFromValue(propValue, 0));
                    }
                    this.mNameData.mFormatted = propValue;
                    return;
                }
                if (!propertyName.equals(VCardConstants.PROPERTY_NAME)) {
                    if (propertyName.equals(VCardConstants.PROPERTY_N)) {
                        handleNProperty(propertyValueList, paramMap);
                        return;
                    }
                    if (propertyName.equals(VCardConstants.PROPERTY_SORT_STRING)) {
                        this.mNameData.mSortString = propValue;
                        return;
                    }
                    if (!propertyName.equals(VCardConstants.PROPERTY_NICKNAME)) {
                        if (!propertyName.equals(ImportOnly.PROPERTY_X_NICKNAME)) {
                            Collection<String> typeCollection;
                            if (propertyName.equals(VCardConstants.PROPERTY_SOUND)) {
                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                if (typeCollection != null) {
                                    if (!typeCollection.contains("X-IRMC-N")) {
                                        return;
                                    }
                                    if (!VCardConfig.isJapanSpacialized()) {
                                        handlePhoneticNameFromSound(VCardUtils.constructListFromValue(propValue, this.mVCardType));
                                        return;
                                    } else if (propertyValueList.size() > 1) {
                                        handlePhoneticNameFromSound(propertyValueList);
                                        return;
                                    } else {
                                        handlePhoneticNameFromSound(VCardUtils.constructListFromValue(propValue, this.mVCardType));
                                        return;
                                    }
                                }
                                return;
                            }
                            int type;
                            String label;
                            boolean isPrimary;
                            String typeStringUpperCase;
                            if (propertyName.equals(VCardConstants.PROPERTY_ADR)) {
                                boolean valuesAreAllEmpty = true;
                                for (String value : propertyValueList) {
                                    if (!TextUtils.isEmpty(value)) {
                                        valuesAreAllEmpty = false;
                                        break;
                                    }
                                }
                                if (!valuesAreAllEmpty) {
                                    type = -1;
                                    label = null;
                                    isPrimary = false;
                                    typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                    if (typeCollection != null) {
                                        for (String typeStringOrg : typeCollection) {
                                            typeStringUpperCase = typeStringOrg.toUpperCase();
                                            if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_PREF)) {
                                                isPrimary = true;
                                            } else {
                                                if (typeStringUpperCase.equals("HOME")) {
                                                    type = 1;
                                                    label = null;
                                                } else {
                                                    if (!typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_WORK)) {
                                                        if (!typeStringUpperCase.equalsIgnoreCase(VCardConstants.PARAM_EXTRA_TYPE_COMPANY)) {
                                                            if (!typeStringUpperCase.equals(VCardConstants.PARAM_ADR_TYPE_PARCEL)) {
                                                                if (!typeStringUpperCase.equals(VCardConstants.PARAM_ADR_TYPE_DOM)) {
                                                                    if (!typeStringUpperCase.equals(VCardConstants.PARAM_ADR_TYPE_INTL)) {
                                                                        if (VCardConfig.isJapanSpacialized()) {
                                                                            if (typeStringUpperCase.equals("OTHER")) {
                                                                                type = 3;
                                                                                label = "";
                                                                            }
                                                                        }
                                                                        if (type < 0) {
                                                                            type = 0;
                                                                            if (typeStringUpperCase.startsWith("X-")) {
                                                                                label = typeStringOrg.substring(2);
                                                                            } else {
                                                                                label = typeStringOrg;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    type = 2;
                                                    label = null;
                                                }
                                            }
                                        }
                                    }
                                    if (type < 0) {
                                        type = 1;
                                    }
                                    addPostal(type, propertyValueList, label, isPrimary);
                                    return;
                                }
                                return;
                            }
                            if (propertyName.equals(VCardConstants.PROPERTY_EMAIL)) {
                                type = -1;
                                label = null;
                                isPrimary = false;
                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                if (typeCollection != null) {
                                    for (String typeStringOrg2 : typeCollection) {
                                        typeStringUpperCase = typeStringOrg2.toUpperCase();
                                        if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_PREF)) {
                                            isPrimary = true;
                                        } else {
                                            if (typeStringUpperCase.equals("HOME")) {
                                                type = 1;
                                            } else {
                                                if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_WORK)) {
                                                    type = 2;
                                                } else {
                                                    if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_CELL)) {
                                                        type = 4;
                                                    } else {
                                                        if (VCardConfig.isJapanSpacialized()) {
                                                            if (typeStringUpperCase.equals("OTHER")) {
                                                                type = 3;
                                                            }
                                                        }
                                                        if (type < 0) {
                                                            if (typeStringUpperCase.startsWith("X-")) {
                                                                label = typeStringOrg2.substring(2);
                                                            } else {
                                                                label = typeStringOrg2;
                                                            }
                                                            type = 0;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (type < 0) {
                                    type = 3;
                                }
                                addEmail(type, propValue, label, isPrimary);
                                return;
                            }
                            String typeString;
                            if (!propertyName.equals(VCardConstants.PROPERTY_ORG)) {
                                if (propertyName.equals(VCardConstants.PROPERTY_TITLE)) {
                                    handleTitleValue(propValue);
                                    return;
                                }
                                if (!propertyName.equals(VCardConstants.PROPERTY_ROLE)) {
                                    Collection<String> paramMapValue;
                                    String formatName;
                                    if (!propertyName.equals(VCardConstants.PROPERTY_PHOTO)) {
                                        if (!propertyName.equals(VCardConstants.PROPERTY_LOGO)) {
                                            if (propertyName.equals(VCardConstants.PROPERTY_X_PHOTOSTATE)) {
                                                addPhotoState(propValue);
                                                return;
                                            }
                                            if (propertyName.equals(VCardConstants.PROPERTY_X_NAMECARDPHOTO)) {
                                                paramMapValue = (Collection) paramMap.get(VCardConstants.PARAM_VALUE);
                                                if (paramMapValue != null) {
                                                    if (paramMapValue.contains(VCardConstants.PROPERTY_URL)) {
                                                        return;
                                                    }
                                                }
                                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                                formatName = null;
                                                isPrimary = false;
                                                if (typeCollection != null) {
                                                    for (String typeValue : typeCollection) {
                                                        if (VCardConstants.PARAM_TYPE_PREF.equals(typeValue)) {
                                                            isPrimary = true;
                                                        } else if (formatName == null) {
                                                            formatName = typeValue;
                                                        }
                                                    }
                                                }
                                                addNameCardBytes(formatName, propertyBytes, isPrimary, "FRONT");
                                                return;
                                            }
                                            if (propertyName.equals(VCardConstants.PROPERTY_X_NAMECARDPHOTO_REVERSE)) {
                                                paramMapValue = (Collection) paramMap.get(VCardConstants.PARAM_VALUE);
                                                if (paramMapValue != null) {
                                                    if (paramMapValue.contains(VCardConstants.PROPERTY_URL)) {
                                                        return;
                                                    }
                                                }
                                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                                formatName = null;
                                                isPrimary = false;
                                                if (typeCollection != null) {
                                                    for (String typeValue2 : typeCollection) {
                                                        if (VCardConstants.PARAM_TYPE_PREF.equals(typeValue2)) {
                                                            isPrimary = true;
                                                        } else if (formatName == null) {
                                                            formatName = typeValue2;
                                                        }
                                                    }
                                                }
                                                addNameCardBytes(formatName, propertyBytes, isPrimary, "BACK");
                                                return;
                                            }
                                            if (propertyName.equals(VCardConstants.PROPERTY_TEL)) {
                                                String phoneNumber = null;
                                                boolean isSip = false;
                                                if (!VCardConfig.isVersion40(this.mVCardType)) {
                                                    phoneNumber = propValue;
                                                } else if (propValue.startsWith("sip:")) {
                                                    isSip = true;
                                                } else if (propValue.startsWith("tel:")) {
                                                    phoneNumber = propValue.substring(4);
                                                } else {
                                                    phoneNumber = propValue;
                                                }
                                                if (isSip) {
                                                    handleSipCase(propValue, (Collection) paramMap.get(VCardConstants.PARAM_TYPE));
                                                    return;
                                                } else if (propValue.length() != 0) {
                                                    typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                                    Object typeObject = VCardUtils.getPhoneTypeFromStrings(typeCollection, phoneNumber);
                                                    if (typeObject instanceof Integer) {
                                                        type = ((Integer) typeObject).intValue();
                                                        label = null;
                                                    } else {
                                                        type = 0;
                                                        if (typeObject != null) {
                                                            label = typeObject.toString();
                                                        } else {
                                                            label = null;
                                                        }
                                                    }
                                                    if (typeCollection != null) {
                                                        if (typeCollection.contains(VCardConstants.PARAM_TYPE_PREF)) {
                                                            isPrimary = true;
                                                            addPhone(type, phoneNumber, label, isPrimary);
                                                            return;
                                                        }
                                                    }
                                                    isPrimary = false;
                                                    addPhone(type, phoneNumber, label, isPrimary);
                                                    return;
                                                } else {
                                                    return;
                                                }
                                            }
                                            if (propertyName.equals(VCardConstants.PROPERTY_X_SKYPE_PSTNNUMBER)) {
                                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                                if (typeCollection != null) {
                                                    if (typeCollection.contains(VCardConstants.PARAM_TYPE_PREF)) {
                                                        isPrimary = true;
                                                        addPhone(7, propValue, null, isPrimary);
                                                        return;
                                                    }
                                                }
                                                isPrimary = false;
                                                addPhone(7, propValue, null, isPrimary);
                                                return;
                                            } else if (sImMap.containsKey(propertyName)) {
                                                int protocol = ((Integer) sImMap.get(propertyName)).intValue();
                                                isPrimary = false;
                                                type = -1;
                                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                                if (typeCollection != null) {
                                                    for (String typeString2 : typeCollection) {
                                                        if (typeString2.equals(VCardConstants.PARAM_TYPE_PREF)) {
                                                            isPrimary = true;
                                                        } else if (type < 0) {
                                                            if (typeString2.equalsIgnoreCase("HOME")) {
                                                                type = 1;
                                                            } else {
                                                                if (typeString2.equalsIgnoreCase(VCardConstants.PARAM_TYPE_WORK)) {
                                                                    type = 2;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                if (type < 0) {
                                                    type = 1;
                                                }
                                                addIm(protocol, null, propValue, type, isPrimary);
                                                return;
                                            } else {
                                                if (propertyName.equals(VCardConstants.PROPERTY_NOTE)) {
                                                    addNote(propValue);
                                                    return;
                                                }
                                                if (propertyName.equals(VCardConstants.PROPERTY_URL)) {
                                                    if (this.mWebsiteList == null) {
                                                        this.mWebsiteList = new ArrayList(1);
                                                    }
                                                    this.mWebsiteList.add(new WebsiteData(propValue));
                                                    return;
                                                }
                                                if (!propertyName.equals(VCardConstants.PROPERTY_BDAY)) {
                                                    if (propertyName.matches(".*BDAY.*")) {
                                                        if (propertyName.endsWith(VCardConstants.PARAM_TYPE) && this.mBirthday != null && propValue != null && propValue.equals("1")) {
                                                            this.mBirthday.setBirthdayType(propValue);
                                                        }
                                                        if (propertyName.endsWith("DATE") && this.mBirthday != null && propValue != null) {
                                                            this.mBirthday.setBirthdaySolarDate(propValue);
                                                            return;
                                                        }
                                                        return;
                                                    }
                                                    if (propertyName.equals(VCardConstants.PROPERTY_ANNIVERSARY)) {
                                                        this.mAnniversary = new AnniversaryData(propValue);
                                                        return;
                                                    }
                                                    if (propertyName.equals(VCardConstants.PROPERTY_X_PHONETIC_FIRST_NAME)) {
                                                        this.mNameData.mPhoneticGiven = propValue;
                                                        return;
                                                    }
                                                    if (propertyName.equals(VCardConstants.PROPERTY_X_PHONETIC_MIDDLE_NAME)) {
                                                        this.mNameData.mPhoneticMiddle = propValue;
                                                        return;
                                                    }
                                                    if (propertyName.equals(VCardConstants.PROPERTY_X_PHONETIC_LAST_NAME)) {
                                                        this.mNameData.mPhoneticFamily = propValue;
                                                        return;
                                                    }
                                                    if (!propertyName.equals(VCardConstants.PROPERTY_IMPP)) {
                                                        if (!propertyName.equals(VCardConstants.PROPERTY_X_SIP)) {
                                                            if (propertyName.equals(VCardConstants.PROPERTY_X_ANDROID_CUSTOM)) {
                                                                List<String> customPropertyList;
                                                                if (VCardConfig.isJapanSpacialized()) {
                                                                    if (propertyValueList != null) {
                                                                        customPropertyList = propertyValueList;
                                                                    } else {
                                                                        customPropertyList = VCardUtils.constructListFromIMValue(propValue, this.mVCardType);
                                                                    }
                                                                } else if (propValue.startsWith("vnd.android.cursor.item/relation")) {
                                                                    String propertyRawValue = property.getRawValue();
                                                                    if (propValue.equals(propertyRawValue)) {
                                                                        customPropertyList = VCardUtils.constructListFromValue(propValue, this.mVCardType);
                                                                    } else {
                                                                        customPropertyList = VCardUtils.constructListFromRawValue(propValue, propertyRawValue, this.mVCardType);
                                                                    }
                                                                } else {
                                                                    Collection<String> encCollection = (Collection) paramMap.get(VCardConstants.PARAM_ENCODING);
                                                                    if (encCollection == null || !encCollection.contains(VCardConstants.PARAM_ENCODING_QP)) {
                                                                        customPropertyList = VCardUtils.constructListFromValue(propValue, this.mVCardType);
                                                                    } else {
                                                                        customPropertyList = propertyValueList;
                                                                    }
                                                                }
                                                                handleAndroidCustomProperty(customPropertyList);
                                                                return;
                                                            }
                                                            if (propertyName.equals(VCardConstants.PROPERTY_X_VZW_NGM_LOC)) {
                                                                Log.d("NGM", "PROPERTY_X_VZW_NGM_LOC tag");
                                                                this.mMapImageFilename = propValue;
                                                                return;
                                                            }
                                                            if (propertyName.equals(VCardConstants.PROPERTY_GEO)) {
                                                                Log.d("NGM", "GEO tag" + propValue);
                                                                if (!TextUtils.isEmpty(propValue)) {
                                                                    String[] data = propValue.replace("geo:", "").split(",");
                                                                    if (data != null && data.length == 2) {
                                                                        this.mLatitude = Double.valueOf(data[0]).doubleValue();
                                                                        this.mLongitude = Double.valueOf(data[1]).doubleValue();
                                                                        return;
                                                                    }
                                                                    return;
                                                                }
                                                                return;
                                                            }
                                                            if (SemCscFeature.getInstance().getBoolean("CscFeature_Contact_EnableDocomoAccountAsDefault") || VCardConfig.isChineseSpacialized()) {
                                                                if (propertyName.equals(VCardConstants.PROPERTY_XGROUPNAME)) {
                                                                    if (this.mXGroupNameList == null) {
                                                                        this.mXGroupNameList = new ArrayList();
                                                                    }
                                                                    this.mXGroupNameList.add(new XGroupNameData(propValue, this.mAccount, this.mContentsResolver));
                                                                    return;
                                                                }
                                                            }
                                                            if (propertyName.toUpperCase().startsWith("X-")) {
                                                                if (this.mUnknownXData == null) {
                                                                    this.mUnknownXData = new ArrayList();
                                                                }
                                                                this.mUnknownXData.add(new Pair(propertyName, propValue));
                                                                return;
                                                            }
                                                            return;
                                                        } else if (!TextUtils.isEmpty(propValue)) {
                                                            handleSipCase(propValue, (Collection) paramMap.get(VCardConstants.PARAM_TYPE));
                                                            return;
                                                        } else {
                                                            return;
                                                        }
                                                    } else if (propValue.startsWith("sip:")) {
                                                        handleSipCase(propValue, (Collection) paramMap.get(VCardConstants.PARAM_TYPE));
                                                        return;
                                                    } else {
                                                        return;
                                                    }
                                                } else if (!VCardConfig.isJapanSpacialized()) {
                                                    this.mBirthday = new BirthdayData(propValue);
                                                    return;
                                                } else if (propValue.length() == 8 && !propValue.contains("-")) {
                                                    BdayBuilder = new StringBuilder();
                                                    BdayBuilder.append(propValue.substring(0, 4));
                                                    BdayBuilder.append('-');
                                                    BdayBuilder.append(propValue.substring(4, 6));
                                                    BdayBuilder.append('-');
                                                    BdayBuilder.append(propValue.substring(6, 8));
                                                    this.mBirthday = new BirthdayData(BdayBuilder.toString());
                                                    return;
                                                } else if (propValue.length() != 4 || propValue.contains("-")) {
                                                    this.mBirthday = new BirthdayData(propValue);
                                                    return;
                                                } else {
                                                    BdayBuilder = new StringBuilder();
                                                    BdayBuilder.append('-');
                                                    BdayBuilder.append('-');
                                                    BdayBuilder.append(propValue.substring(0, 2));
                                                    BdayBuilder.append('-');
                                                    BdayBuilder.append(propValue.substring(2, 4));
                                                    this.mBirthday = new BirthdayData(BdayBuilder.toString());
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    paramMapValue = (Collection) paramMap.get(VCardConstants.PARAM_VALUE);
                                    if (paramMapValue != null) {
                                        if (paramMapValue.contains(VCardConstants.PROPERTY_URL)) {
                                            return;
                                        }
                                    }
                                    typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                    formatName = null;
                                    isPrimary = false;
                                    if (typeCollection != null) {
                                        for (String typeValue22 : typeCollection) {
                                            if (VCardConstants.PARAM_TYPE_PREF.equals(typeValue22)) {
                                                isPrimary = true;
                                            } else if (formatName == null) {
                                                formatName = typeValue22;
                                            }
                                        }
                                    }
                                    addPhotoBytes(formatName, propertyBytes, isPrimary);
                                    return;
                                }
                                return;
                            } else if (VCardConfig.isJapanSpacialized()) {
                                isPrimary = false;
                                type = -1;
                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                if (typeCollection != null) {
                                    for (String typeString22 : typeCollection) {
                                        String typeStringOrigin = typeString22;
                                        typeString22 = typeString22.toUpperCase();
                                        if (typeString22.equals(VCardConstants.PARAM_TYPE_PREF)) {
                                            isPrimary = true;
                                        } else {
                                            if (typeString22.equals(VCardConstants.PARAM_TYPE_WORK)) {
                                                type = 1;
                                            } else {
                                                if (typeString22.equals("OTHER")) {
                                                    type = 2;
                                                } else {
                                                    if (typeStringOrigin.startsWith("X-") && type < 0) {
                                                        typeStringOrigin = typeStringOrigin.substring(2);
                                                    }
                                                    type = 0;
                                                    label = typeStringOrigin;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (type < 0) {
                                    type = 1;
                                }
                                handleOrgValue(type, propertyValueList, paramMap, isPrimary);
                                return;
                            } else {
                                isPrimary = false;
                                typeCollection = (Collection) paramMap.get(VCardConstants.PARAM_TYPE);
                                if (typeCollection != null) {
                                    for (String equals : typeCollection) {
                                        if (equals.equals(VCardConstants.PARAM_TYPE_PREF)) {
                                            isPrimary = true;
                                        }
                                    }
                                }
                                handleOrgValue(1, propertyValueList, paramMap, isPrimary);
                                return;
                            }
                        }
                    }
                    addNickName(propValue);
                } else if (TextUtils.isEmpty(this.mNameData.mFormatted)) {
                    this.mNameData.mFormatted = propValue;
                }
            }
        }
    }

    private void handleSipCase(String propValue, Collection<String> typeCollection) {
        if (!TextUtils.isEmpty(propValue)) {
            if (propValue.startsWith("sip:")) {
                propValue = propValue.substring(4);
                if (propValue.length() == 0) {
                    return;
                }
            }
            int type = -1;
            String label = null;
            boolean isPrimary = false;
            if (typeCollection != null) {
                for (String typeStringOrg : typeCollection) {
                    String typeStringUpperCase = typeStringOrg.toUpperCase();
                    if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_PREF)) {
                        isPrimary = true;
                    } else if (typeStringUpperCase.equals("HOME")) {
                        type = 1;
                    } else if (typeStringUpperCase.equals(VCardConstants.PARAM_TYPE_WORK)) {
                        type = 2;
                    } else if (type < 0) {
                        if (typeStringUpperCase.startsWith("X-")) {
                            label = typeStringOrg.substring(2);
                        } else {
                            label = typeStringOrg;
                        }
                        type = 0;
                    }
                }
            }
            if (type < 0) {
                type = 3;
            }
            addSip(propValue, type, label, isPrimary);
        }
    }

    public void addChild(VCardEntry child) {
        if (this.mChildren == null) {
            this.mChildren = new ArrayList();
        }
        this.mChildren.add(child);
    }

    private void handleAndroidCustomProperty(List<String> customPropertyList) {
        if (this.mAndroidCustomDataList == null) {
            this.mAndroidCustomDataList = new ArrayList();
        }
        this.mAndroidCustomDataList.add(AndroidCustomData.constructAndroidCustomData(customPropertyList));
    }

    private String constructDisplayName() {
        String displayName = null;
        if (!TextUtils.isEmpty(this.mNameData.mFormatted)) {
            displayName = this.mNameData.mFormatted;
        } else if (!this.mNameData.emptyStructuredName()) {
            displayName = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mFamily, this.mNameData.mMiddle, this.mNameData.mGiven, this.mNameData.mPrefix, this.mNameData.mSuffix);
        } else if (!this.mNameData.emptyPhoneticStructuredName()) {
            displayName = VCardUtils.constructNameFromElements(this.mVCardType, this.mNameData.mPhoneticFamily, this.mNameData.mPhoneticMiddle, this.mNameData.mPhoneticGiven);
        } else if (this.mPhoneList != null && this.mPhoneList.size() > 0) {
            displayName = ((PhoneData) this.mPhoneList.get(0)).mNumber;
        } else if (this.mEmailList != null && this.mEmailList.size() > 0) {
            displayName = ((EmailData) this.mEmailList.get(0)).mAddress;
        } else if (this.mPostalList != null && this.mPostalList.size() > 0) {
            displayName = ((PostalData) this.mPostalList.get(0)).getFormattedAddress(this.mVCardType);
        } else if (this.mOrganizationList != null && this.mOrganizationList.size() > 0) {
            displayName = ((OrganizationData) this.mOrganizationList.get(0)).getFormattedString();
        }
        if (displayName == null) {
            return "";
        }
        return displayName;
    }

    public void consolidateFields() {
        this.mNameData.displayName = constructDisplayName();
    }

    public boolean isIgnorable() {
        IsIgnorableIterator iterator = new IsIgnorableIterator();
        iterateAllData(iterator);
        return iterator.getResult();
    }

    public ArrayList<ContentProviderOperation> constructInsertOperations(ContentResolver resolver, ArrayList<ContentProviderOperation> operationList) {
        if (operationList == null) {
            operationList = new ArrayList();
        }
        if (!isIgnorable()) {
            int backReferenceIndex = operationList.size();
            Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
            if (this.mAccount != null) {
                builder.withValue("account_name", this.mAccount.name);
                builder.withValue(Key.ACCOUNT_TYPE, this.mAccount.type);
            } else {
                builder.withValue("account_name", null);
                builder.withValue(Key.ACCOUNT_TYPE, null);
            }
            operationList.add(builder.build());
            int start = operationList.size();
            iterateAllData(new InsertOperationConstrutor(operationList, backReferenceIndex));
            int end = operationList.size();
        }
        return operationList;
    }

    public static VCardEntry buildFromResolver(ContentResolver resolver) {
        return buildFromResolver(resolver, Contacts.CONTENT_URI);
    }

    public static VCardEntry buildFromResolver(ContentResolver resolver, Uri uri) {
        return null;
    }

    private String listToString(List<String> list) {
        int size = list.size();
        if (size > 1) {
            StringBuilder builder = new StringBuilder();
            for (String type : list) {
                builder.append(type);
                if (0 < size - 1) {
                    builder.append(";");
                }
            }
            return builder.toString();
        } else if (size == 1) {
            return (String) list.get(0);
        } else {
            return "";
        }
    }

    public final NameData getNameData() {
        return this.mNameData;
    }

    public final List<NicknameData> getNickNameList() {
        return this.mNicknameList;
    }

    public final String getBirthday() {
        return this.mBirthday != null ? this.mBirthday.mBirthday : null;
    }

    public final List<NoteData> getNotes() {
        return this.mNoteList;
    }

    public final List<PhoneData> getPhoneList() {
        return this.mPhoneList;
    }

    public final List<EmailData> getEmailList() {
        return this.mEmailList;
    }

    public final List<PostalData> getPostalList() {
        return this.mPostalList;
    }

    public final List<OrganizationData> getOrganizationList() {
        return this.mOrganizationList;
    }

    public final List<ImData> getImList() {
        return this.mImList;
    }

    public final List<PhotoData> getPhotoList() {
        return this.mPhotoList;
    }

    public final List<PhotoStateData> getPhotoStateList() {
        return this.mPhotoStateList;
    }

    public final List<NameCardData> getNameCardList() {
        return this.mNameCardList;
    }

    public final List<WebsiteData> getWebsiteList() {
        return this.mWebsiteList;
    }

    public String getMapImageFilename() {
        return this.mMapImageFilename;
    }

    public final double getLatitude() {
        return this.mLatitude;
    }

    public final double getLongitude() {
        return this.mLongitude;
    }

    public final List<VCardEntry> getChildlen() {
        return this.mChildren;
    }

    public String getDisplayName() {
        if (this.mNameData.displayName == null) {
            this.mNameData.displayName = constructDisplayName();
        }
        return this.mNameData.displayName;
    }

    public List<Pair<String, String>> getUnknownXData() {
        return this.mUnknownXData;
    }
}
