package com.android.vcard;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.content.EntityIterator;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.knox.SemPersonaManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VCardComposer {
    private static final boolean DEBUG = false;
    public static long ENTERPRISE_CONTACT_ID_BASE = 1000000000;
    public static final String FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO = "Failed to get database information";
    public static final String FAILURE_REASON_NOT_INITIALIZED = "The vCard composer object is not correctly initialized";
    public static final String FAILURE_REASON_NO_ENTRY = "There's no exportable in the database";
    public static final String FAILURE_REASON_UNSUPPORTED_URI = "The Uri vCard composer received is not supported by the composer.";
    private static final String KNOX_CONTAINER_ID = "knoxContainerId";
    private static final String LOG_TAG = "VCardComposer";
    public static final String NO_ERROR = "No error";
    private static final String SHIFT_JIS = "SHIFT_JIS";
    private static final String SIM_NAME = "SIM";
    private static final String SIM_NAME_1 = "SIM1";
    private static final String SIM_NAME_2 = "SIM2";
    private static final String SIM_NAME_3 = "SIM3";
    private static final String UTF_8 = "UTF-8";
    private static final String[] sContactsProjection = new String[]{"_id"};
    private static final Map<Integer, String> sImMap = new HashMap();
    private final String mCharset;
    private final ContentResolver mContentResolver;
    private Uri mContentUriForRawContactsEntity;
    public long mCurrentContactID;
    private Cursor mCursor;
    private boolean mCursorSuppliedFromOutside;
    private String mErrorReason;
    private boolean mFirstVCardEmittedInDoCoMoCase;
    private int mIdColumn;
    private boolean mInitDone;
    private final boolean mIsChn;
    private final boolean mIsDoCoMo;
    private VCardPhoneNumberTranslationCallback mPhoneTranslationCallback;
    private RawContactEntitlesInfoCallback mRawContactEntitlesInfoCallback;
    private boolean mTerminateCalled;
    private final int mVCardType;

    public static class RawContactEntitlesInfo {
        public final long contactId;
        public final Uri rawContactEntitlesUri;

        public RawContactEntitlesInfo(Uri rawContactEntitlesUri, long contactId) {
            this.rawContactEntitlesUri = rawContactEntitlesUri;
            this.contactId = contactId;
        }
    }

    public interface RawContactEntitlesInfoCallback {
        RawContactEntitlesInfo getRawContactEntitlesInfo(long j);
    }

    static {
        sImMap.put(Integer.valueOf(0), VCardConstants.PROPERTY_X_AIM);
        sImMap.put(Integer.valueOf(1), VCardConstants.PROPERTY_X_MSN);
        sImMap.put(Integer.valueOf(2), VCardConstants.PROPERTY_X_YAHOO);
        sImMap.put(Integer.valueOf(6), VCardConstants.PROPERTY_X_ICQ);
        sImMap.put(Integer.valueOf(6), VCardConstants.PROPERTY_X_QQ);
        sImMap.put(Integer.valueOf(7), VCardConstants.PROPERTY_X_JABBER);
        sImMap.put(Integer.valueOf(3), VCardConstants.PROPERTY_X_SKYPE_USERNAME);
        sImMap.put(Integer.valueOf(9), VCardConstants.PROPERTY_X_WHATSAPP);
        sImMap.put(Integer.valueOf(10), VCardConstants.PROPERTY_X_FACEBOOK);
    }

    public VCardComposer(Context context) {
        this(context, VCardConfig.VCARD_TYPE_DEFAULT, null, true);
    }

    public VCardComposer(Context context, int vcardType) {
        this(context, vcardType, null, true);
    }

    public VCardComposer(Context context, int vcardType, String charset) {
        this(context, vcardType, charset, true);
    }

    public VCardComposer(Context context, int vcardType, boolean careHandlerErrors) {
        this(context, vcardType, null, careHandlerErrors);
    }

    public VCardComposer(Context context, int vcardType, String charset, boolean careHandlerErrors) {
        this(context, context.getContentResolver(), vcardType, charset, careHandlerErrors);
    }

    public VCardComposer(Context context, ContentResolver resolver, int vcardType, String charset, boolean careHandlerErrors) {
        boolean shouldAppendCharsetParam = true;
        this.mCurrentContactID = 0;
        this.mErrorReason = NO_ERROR;
        this.mTerminateCalled = true;
        if (VCardConfig.isJapanSpacialized()) {
            this.mVCardType = VCardConfig.getVCardTypeFromString("default") | VCardConfig.VCARD_TYPE_DOCOMO;
        } else {
            this.mVCardType = vcardType;
        }
        this.mContentResolver = resolver;
        this.mIsDoCoMo = VCardConfig.isDoCoMo(this.mVCardType);
        this.mIsChn = VCardConfig.isChineseSpacialized();
        if (TextUtils.isEmpty(charset)) {
            charset = "UTF-8";
        }
        if (VCardConfig.isVersion30(vcardType) && "UTF-8".equalsIgnoreCase(charset)) {
            shouldAppendCharsetParam = false;
        }
        if (this.mIsDoCoMo || shouldAppendCharsetParam) {
            if ("SHIFT_JIS".equalsIgnoreCase(charset)) {
                this.mCharset = charset;
            } else if (TextUtils.isEmpty(charset)) {
                this.mCharset = "SHIFT_JIS";
            } else {
                this.mCharset = charset;
            }
        } else if (TextUtils.isEmpty(charset)) {
            this.mCharset = "UTF-8";
        } else {
            this.mCharset = charset;
        }
        Log.d(LOG_TAG, "Use the charset \"" + this.mCharset + "\"");
    }

    public boolean init() {
        return init(null, null);
    }

    @Deprecated
    public boolean initWithRawContactsEntityUri(Uri contentUriForRawContactsEntity) {
        return init(Contacts.CONTENT_URI, sContactsProjection, null, null, null, contentUriForRawContactsEntity);
    }

    public boolean init(String selection, String[] selectionArgs) {
        return init(Contacts.CONTENT_URI, sContactsProjection, selection, selectionArgs, null, null);
    }

    public boolean init(Uri contentUri, String selection, String[] selectionArgs, String sortOrder) {
        return init(contentUri, sContactsProjection, selection, selectionArgs, sortOrder, null);
    }

    public boolean init(Uri contentUri, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        return init(contentUri, sContactsProjection, selection, selectionArgs, sortOrder, contentUriForRawContactsEntity);
    }

    public boolean init(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        if (!"com.android.contacts".equals(contentUri.getAuthority())) {
            this.mErrorReason = FAILURE_REASON_UNSUPPORTED_URI;
            return false;
        } else if (initInterFirstPart(contentUriForRawContactsEntity) && initInterCursorCreationPart(contentUri, projection, selection, selectionArgs, sortOrder) && initInterMainPart()) {
            return initInterLastPart();
        } else {
            return false;
        }
    }

    public boolean init(Cursor cursor) {
        return initWithCallback(cursor, null);
    }

    public boolean initWithCallback(Cursor cursor, RawContactEntitlesInfoCallback rawContactEntitlesInfoCallback) {
        if (!initInterFirstPart(null)) {
            return false;
        }
        this.mCursorSuppliedFromOutside = true;
        this.mCursor = cursor;
        this.mRawContactEntitlesInfoCallback = rawContactEntitlesInfoCallback;
        if (initInterMainPart()) {
            return initInterLastPart();
        }
        return false;
    }

    private boolean initInterFirstPart(Uri contentUriForRawContactsEntity) {
        if (contentUriForRawContactsEntity == null) {
            contentUriForRawContactsEntity = RawContactsEntity.CONTENT_URI;
        }
        this.mContentUriForRawContactsEntity = contentUriForRawContactsEntity;
        if (!this.mInitDone) {
            return true;
        }
        Log.e(LOG_TAG, "init() is already called");
        return false;
    }

    private boolean initInterCursorCreationPart(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.mCursorSuppliedFromOutside = false;
        this.mCursor = this.mContentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
        if (this.mCursor == null) {
            Log.e(LOG_TAG, String.format("Cursor became null unexpectedly", new Object[0]));
            this.mErrorReason = FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO;
            return false;
        } else if (getCount() != 0 && this.mCursor.moveToFirst()) {
            return true;
        } else {
            try {
                this.mCursor.close();
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
            } finally {
                this.mCursor = null;
                this.mErrorReason = FAILURE_REASON_NO_ENTRY;
            }
            return false;
        }
    }

    private boolean initInterMainPart() {
        if (this.mCursor.getCount() == 0 || !this.mCursor.moveToFirst()) {
            closeCursorIfAppropriate();
            return false;
        }
        this.mIdColumn = this.mCursor.getColumnIndex("contact_id");
        if (this.mIdColumn < 0) {
            this.mIdColumn = this.mCursor.getColumnIndex("_id");
        }
        if (this.mIdColumn >= 0) {
            return true;
        }
        return false;
    }

    private boolean initInterLastPart() {
        this.mInitDone = true;
        this.mTerminateCalled = false;
        return true;
    }

    public String createOneEntry() {
        return createOneEntry(null);
    }

    public String createOneEntry(Method getEntityIteratorMethod) {
        if (this.mIsDoCoMo && !this.mFirstVCardEmittedInDoCoMoCase) {
            this.mFirstVCardEmittedInDoCoMoCase = true;
        }
        String vcard = createOneEntryInternal(this.mCursor.getLong(this.mIdColumn), getEntityIteratorMethod);
        if (!this.mCursor.moveToNext()) {
            Log.e(LOG_TAG, "Cursor#moveToNext() returned false");
        }
        return vcard;
    }

    private String createOneEntryInternal(long contactId, Method getEntityIteratorMethod) {
        Map<String, List<ContentValues>> contentValuesListMap = new HashMap();
        EntityIterator entityIterator = null;
        Uri uri = this.mContentUriForRawContactsEntity;
        if (contactId < ENTERPRISE_CONTACT_ID_BASE * 100 || this.mRawContactEntitlesInfoCallback == null) {
            try {
                if (this.mRawContactEntitlesInfoCallback != null) {
                    RawContactEntitlesInfo rawContactEntitlesInfo = this.mRawContactEntitlesInfoCallback.getRawContactEntitlesInfo(contactId);
                    uri = rawContactEntitlesInfo.rawContactEntitlesUri;
                    contactId = rawContactEntitlesInfo.contactId;
                }
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "IllegalArgumentException has been thrown: " + e.getMessage());
            } catch (IllegalAccessException e2) {
                Log.e(LOG_TAG, "IllegalAccessException has been thrown: " + e2.getMessage());
            } catch (InvocationTargetException e3) {
                Log.e(LOG_TAG, "InvocationTargetException has been thrown: ", e3);
                throw new RuntimeException("InvocationTargetException has been thrown");
            } catch (Throwable th) {
                if (entityIterator != null) {
                    entityIterator.close();
                }
            }
        } else {
            int userId = (int) (Long.valueOf(contactId).longValue() / ENTERPRISE_CONTACT_ID_BASE);
            if (SemPersonaManager.isKnoxId(userId)) {
                uri = this.mRawContactEntitlesInfoCallback.getRawContactEntitlesInfo(contactId).rawContactEntitlesUri;
                contactId %= ENTERPRISE_CONTACT_ID_BASE;
                uri = uri.buildUpon().appendQueryParameter(KNOX_CONTAINER_ID, String.valueOf(userId)).build();
            }
        }
        String selection = "contact_id=?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};
        if (getEntityIteratorMethod != null) {
            entityIterator = (EntityIterator) getEntityIteratorMethod.invoke(null, new Object[]{this.mContentResolver, uri, "contact_id=?", selectionArgs, null});
        } else {
            entityIterator = RawContacts.newEntityIterator(this.mContentResolver.query(uri, null, "contact_id=?", selectionArgs, "is_super_primary, (CASE WHEN display_name IS (SELECT display_name FROM view_contacts WHERE _id = " + String.valueOf(contactId) + ") THEN 1 ELSE 0 END) DESC"));
        }
        if (entityIterator == null) {
            Log.e(LOG_TAG, "EntityIterator is null");
            if (entityIterator != null) {
                entityIterator.close();
            }
            return "";
        }
        if (!entityIterator.hasNext()) {
            Log.w(LOG_TAG, "Data does not exist. contactId: " + contactId);
            if (entityIterator != null) {
                entityIterator.close();
            }
            return "";
        }
        while (entityIterator.hasNext()) {
            Iterator it = ((Entity) entityIterator.next()).getSubValues().iterator();
            while (it.hasNext()) {
                ContentValues contentValues = ((NamedContentValues) it.next()).values;
                String key = contentValues.getAsString("mimetype");
                if (key != null) {
                    List<ContentValues> contentValuesList = (List) contentValuesListMap.get(key);
                    if (contentValuesList == null) {
                        contentValuesList = new ArrayList();
                        contentValuesListMap.put(key, contentValuesList);
                    }
                    contentValuesList.add(contentValues);
                }
            }
        }
        if (entityIterator != null) {
            entityIterator.close();
        }
        this.mCurrentContactID = contactId;
        return buildVCard(contentValuesListMap);
    }

    public void setPhoneNumberTranslationCallback(VCardPhoneNumberTranslationCallback callback) {
        this.mPhoneTranslationCallback = callback;
    }

    private boolean isSimcardAccount(long contactid) {
        boolean isSimAccount = false;
        Cursor cursor = null;
        try {
            cursor = this.mContentResolver.query(RawContacts.CONTENT_URI, new String[]{"account_name"}, "contact_id=?", new String[]{Long.toString(contactid)}, null);
            if (!(cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst())) {
                String accountName = cursor.getString(cursor.getColumnIndex("account_name"));
                if (SIM_NAME.equals(accountName) || SIM_NAME_1.equals(accountName) || SIM_NAME_2.equals(accountName) || SIM_NAME_3.equals(accountName)) {
                    isSimAccount = true;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            return isSimAccount;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String buildVCard(Map<String, List<ContentValues>> contentValuesListMap) {
        if (contentValuesListMap == null) {
            Log.e(LOG_TAG, "The given map is null. Ignore and return empty String");
            return "";
        }
        VCardBuilder builder = new VCardBuilder(this.mVCardType, this.mCharset);
        builder.appendNameProperties((List) contentValuesListMap.get("vnd.android.cursor.item/name")).appendNickNames((List) contentValuesListMap.get("vnd.android.cursor.item/nickname")).appendPhones((List) contentValuesListMap.get("vnd.android.cursor.item/phone_v2"), this.mPhoneTranslationCallback);
        if ((this.mVCardType & 4194304) != 0) {
            return builder.toString();
        }
        if ((this.mVCardType & 524288) == 0) {
            builder.appendEmails((List) contentValuesListMap.get("vnd.android.cursor.item/email_v2"));
        }
        if ((this.mVCardType & 262144) == 0) {
            builder.appendPostals((List) contentValuesListMap.get("vnd.android.cursor.item/postal-address_v2"));
        }
        if ((this.mVCardType & 131072) == 0) {
            builder.appendOrganizations((List) contentValuesListMap.get("vnd.android.cursor.item/organization"));
        }
        if ((this.mVCardType & 65536) == 0) {
            builder.appendWebsites((List) contentValuesListMap.get("vnd.android.cursor.item/website"));
        }
        if ((this.mVCardType & 8388608) == 0 && this.mCurrentContactID > 0 && !isSimcardAccount(this.mCurrentContactID)) {
            builder.appendPhotos((List) contentValuesListMap.get("vnd.android.cursor.item/photo"));
            builder.appendNameCard((List) contentValuesListMap.get("vnd.sec.cursor.item/name_card"), this.mContentResolver);
        }
        if ((this.mVCardType & 32768) == 0) {
            builder.appendNotes((List) contentValuesListMap.get("vnd.android.cursor.item/note"));
        }
        if ((this.mVCardType & 16384) == 0) {
            builder.appendEvents((List) contentValuesListMap.get("vnd.android.cursor.item/contact_event"));
        }
        if ((this.mVCardType & 8192) == 0) {
            builder.appendIms((List) contentValuesListMap.get("vnd.android.cursor.item/im"));
        }
        if ((this.mVCardType & 4096) == 0) {
            builder.appendSipAddresses((List) contentValuesListMap.get("vnd.android.cursor.item/sip_address"));
        }
        if ((this.mVCardType & 2048) == 0) {
            builder.appendRelation((List) contentValuesListMap.get("vnd.android.cursor.item/relation"));
        }
        if ((this.mIsDoCoMo || this.mIsChn) && contentValuesListMap.containsKey("vnd.android.cursor.item/group_membership")) {
            builder.appendGroupName((List) contentValuesListMap.get("vnd.android.cursor.item/group_membership"), this.mContentResolver);
        }
        return builder.toString();
    }

    public void terminate() {
        closeCursorIfAppropriate();
        this.mTerminateCalled = true;
    }

    private void closeCursorIfAppropriate() {
        if (!this.mCursorSuppliedFromOutside && this.mCursor != null) {
            try {
                this.mCursor.close();
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
            }
            this.mCursor = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.mTerminateCalled) {
                Log.e(LOG_TAG, "finalized() is called before terminate() being called");
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public int getCount() {
        if (this.mCursor != null) {
            return this.mCursor.getCount();
        }
        Log.w(LOG_TAG, "This object is not ready yet.");
        return 0;
    }

    public boolean isAfterLast() {
        if (this.mCursor != null) {
            return this.mCursor.isAfterLast();
        }
        Log.w(LOG_TAG, "This object is not ready yet.");
        return false;
    }

    public String getErrorReason() {
        return this.mErrorReason;
    }
}
