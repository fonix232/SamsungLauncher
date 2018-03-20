package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.p010a;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c.a.b */
public class C0791b extends SQLiteOpenHelper {
    /* renamed from: a */
    public static final String f142a = "SamsungAnalytics.db";
    /* renamed from: b */
    public static final int f143b = 1;
    /* renamed from: c */
    public static final String f144c = "create table logs (_id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER, data TEXT)";

    public C0791b(Context context) {
        super(context, f142a, null, 1);
    }

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(f144c);
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }
}
