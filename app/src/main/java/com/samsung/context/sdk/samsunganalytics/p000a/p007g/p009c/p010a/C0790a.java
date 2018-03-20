package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.p010a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c.a.a */
public class C0790a {
    /* renamed from: a */
    private SQLiteDatabase f139a;
    /* renamed from: b */
    private C0791b f140b;
    /* renamed from: c */
    private Queue<C0796d> f141c = new LinkedBlockingQueue();

    public C0790a(Context context) {
        this.f140b = new C0791b(context);
        m111a(5);
    }

    /* renamed from: b */
    private Queue<C0796d> m108b(String str) {
        this.f141c.clear();
        this.f139a = this.f140b.getReadableDatabase();
        Cursor rawQuery = this.f139a.rawQuery(str, null);
        while (rawQuery.moveToNext()) {
            C0796d c0796d = new C0796d();
            c0796d.m140a(rawQuery.getString(rawQuery.getColumnIndex("_id")));
            c0796d.m142b(rawQuery.getString(rawQuery.getColumnIndex(C0792c.f147c)));
            c0796d.m139a(rawQuery.getLong(rawQuery.getColumnIndex("timestamp")));
            this.f141c.add(c0796d);
        }
        rawQuery.close();
        return this.f141c;
    }

    /* renamed from: a */
    public Queue<C0796d> m109a() {
        return m108b("select * from logs");
    }

    /* renamed from: a */
    public Queue<C0796d> m110a(int i) {
        return m108b("select * from logs LIMIT " + i);
    }

    /* renamed from: a */
    public void m111a(long j) {
        this.f139a = this.f140b.getWritableDatabase();
        this.f139a.delete(C0792c.f145a, "timestamp <= " + j, null);
    }

    /* renamed from: a */
    public void m112a(C0796d c0796d) {
        this.f139a = this.f140b.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("timestamp", Long.valueOf(c0796d.m141b()));
        contentValues.put(C0792c.f147c, c0796d.m143c());
        this.f139a.insert(C0792c.f145a, null, contentValues);
    }

    /* renamed from: a */
    public void m113a(String str) {
        this.f139a = this.f140b.getWritableDatabase();
        this.f139a.delete(C0792c.f145a, "_id = " + str, null);
    }

    /* renamed from: a */
    public void m114a(List<String> list) {
        this.f139a = this.f140b.getWritableDatabase();
        this.f139a.beginTransaction();
        try {
            int size = list.size();
            int i = 0;
            while (size > 0) {
                int i2 = size < 900 ? size : 900;
                List subList = list.subList(i, i + i2);
                this.f139a.delete(C0792c.f145a, ("_id IN(" + new String(new char[(subList.size() - 1)]).replaceAll("\u0000", "?,")) + "?)", (String[]) subList.toArray(new String[0]));
                size -= i2;
                i += i2;
            }
            list.clear();
            this.f139a.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.f139a.endTransaction();
        }
    }

    /* renamed from: b */
    public void m115b() {
        if (this.f140b != null) {
            this.f140b.close();
        }
    }

    /* renamed from: c */
    public boolean m116c() {
        return m117d() <= 0;
    }

    /* renamed from: d */
    public long m117d() {
        this.f139a = this.f140b.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(this.f139a, C0792c.f145a);
    }
}
