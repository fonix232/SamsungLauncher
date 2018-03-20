package com.android.launcher3.widget.model;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MainThreadExecutor;
import com.android.launcher3.widget.PinShortcutRequestActivityInfo;
import com.android.launcher3.widget.view.PreviewLoadListener;
import com.sec.android.app.launcher.R;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WidgetPreviewLoader {
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetPreviewLoader";
    private static final float WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE = 0.25f;
    private final Context mContext;
    private final CacheDb mDb;
    private final IconCache mIconCache;
    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();
    private final AppWidgetManagerCompat mManager;
    private final HashMap<String, long[]> mPackageVersions = new HashMap();
    private final int mProfileBadgeMargin;
    private final Set<Bitmap> mUnusedBitmaps = Collections.newSetFromMap(new WeakHashMap());
    private final UserManagerCompat mUserManager;
    private final Handler mWorkerHandler;

    private static class CacheDb extends SQLiteOpenHelper {
        private static final String COLUMN_COMPONENT = "componentName";
        private static final String COLUMN_LAST_UPDATED = "lastUpdated";
        private static final String COLUMN_PACKAGE = "packageName";
        private static final String COLUMN_PREVIEW_BITMAP = "preview_bitmap";
        private static final String COLUMN_SIZE = "size";
        private static final String COLUMN_USER = "profileId";
        private static final String COLUMN_VERSION = "version";
        private static final int DB_VERSION = 4;
        private static final String TABLE_NAME = "shortcut_and_widget_previews";

        public CacheDb(Context context) {
            super(context, LauncherFiles.WIDGET_PREVIEWS_DB, null, 4);
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS shortcut_and_widget_previews (componentName TEXT NOT NULL, profileId INTEGER NOT NULL, size TEXT NOT NULL, packageName TEXT NOT NULL, lastUpdated INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, preview_bitmap BLOB, PRIMARY KEY (componentName, profileId, size) );");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        private void clearDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS shortcut_and_widget_previews");
            onCreate(db);
        }
    }

    public class PreviewLoadRequest {
        private final PreviewLoadTask mTask;

        public PreviewLoadRequest(PreviewLoadTask task) {
            this.mTask = task;
        }

        public void cleanup() {
            if (this.mTask != null) {
                this.mTask.cancel(true);
                if (this.mTask.mBitmapToRecycle != null) {
                    WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                        public void run() {
                            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                                WidgetPreviewLoader.this.mUnusedBitmaps.add(PreviewLoadRequest.this.mTask.mBitmapToRecycle);
                            }
                            PreviewLoadRequest.this.mTask.mBitmapToRecycle = null;
                        }
                    });
                }
            }
        }
    }

    public class PreviewLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private Bitmap mBitmapToRecycle;
        private final PreviewLoadListener mCaller;
        private final Object mInfo;
        private final WidgetCacheKey mKey;
        private final int mPreviewHeight;
        private final int mPreviewWidth;
        private long[] mVersions;

        PreviewLoadTask(WidgetCacheKey key, Object info, int previewWidth, int previewHeight, PreviewLoadListener caller) {
            this.mKey = key;
            this.mInfo = info;
            this.mPreviewHeight = previewHeight;
            this.mPreviewWidth = previewWidth;
            this.mCaller = caller;
        }

        protected Bitmap doInBackground(Void... params) {
            Bitmap unusedBitmap = null;
            if (isCancelled()) {
                return null;
            }
            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                for (Bitmap candidate : WidgetPreviewLoader.this.mUnusedBitmaps) {
                    if (candidate != null && candidate.isMutable() && candidate.getWidth() == this.mPreviewWidth && candidate.getHeight() == this.mPreviewHeight) {
                        unusedBitmap = candidate;
                        WidgetPreviewLoader.this.mUnusedBitmaps.remove(unusedBitmap);
                        break;
                    }
                }
            }
            if (unusedBitmap == null) {
                unusedBitmap = Bitmap.createBitmap(this.mPreviewWidth, this.mPreviewHeight, Config.ARGB_8888);
            }
            if (isCancelled()) {
                return unusedBitmap;
            }
            Bitmap preview = WidgetPreviewLoader.this.readFromDb(this.mKey, unusedBitmap, this);
            if (isCancelled() || preview != null) {
                return preview;
            }
            this.mVersions = WidgetPreviewLoader.this.getPackageVersion(this.mKey.componentName.getPackageName());
            if (!(this.mCaller instanceof View)) {
                return null;
            }
            return WidgetPreviewLoader.this.generatePreview(((View) this.mCaller).getContext(), this.mInfo, unusedBitmap, this.mPreviewWidth, this.mPreviewHeight);
        }

        protected void onPostExecute(final Bitmap preview) {
            this.mCaller.applyPreview(this.mInfo, preview);
            if (this.mVersions != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    public void run() {
                        if (PreviewLoadTask.this.isCancelled()) {
                            synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                                WidgetPreviewLoader.this.mUnusedBitmaps.add(preview);
                            }
                            return;
                        }
                        WidgetPreviewLoader.this.writeToDb(PreviewLoadTask.this.mKey, PreviewLoadTask.this.mVersions, preview);
                        PreviewLoadTask.this.mBitmapToRecycle = preview;
                    }
                });
            } else {
                this.mBitmapToRecycle = preview;
            }
        }

        protected void onCancelled(final Bitmap preview) {
            if (preview != null) {
                WidgetPreviewLoader.this.mWorkerHandler.post(new Runnable() {
                    public void run() {
                        synchronized (WidgetPreviewLoader.this.mUnusedBitmaps) {
                            WidgetPreviewLoader.this.mUnusedBitmaps.add(preview);
                        }
                    }
                });
            }
        }
    }

    private static final class WidgetCacheKey extends ComponentKey {
        private final String size;

        public WidgetCacheKey(ComponentName componentName, UserHandleCompat user, String size) {
            super(componentName, user);
            this.size = size;
        }

        public int hashCode() {
            return super.hashCode() ^ this.size.hashCode();
        }

        public boolean equals(Object o) {
            return super.equals(o) && ((WidgetCacheKey) o).size.equals(this.size);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeObsoletePreviews(java.util.ArrayList<java.lang.Object> r32) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x010a in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r31 = this;
        com.android.launcher3.Utilities.assertWorkerThread();
        r25 = new android.util.LongSparseArray;
        r25.<init>();
        r6 = r32.iterator();
    L_0x000c:
        r7 = r6.hasNext();
        if (r7 == 0) goto L_0x0069;
    L_0x0012:
        r20 = r6.next();
        r0 = r20;
        r7 = r0 instanceof android.content.pm.ResolveInfo;
        if (r7 == 0) goto L_0x0052;
    L_0x001c:
        r24 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();
        r20 = (android.content.pm.ResolveInfo) r20;
        r0 = r20;
        r7 = r0.activityInfo;
        r0 = r7.packageName;
        r23 = r0;
    L_0x002a:
        r0 = r31;
        r7 = r0.mUserManager;
        r0 = r24;
        r26 = r7.getSerialNumberForUser(r0);
        r21 = r25.get(r26);
        r21 = (java.util.HashSet) r21;
        if (r21 != 0) goto L_0x004a;
    L_0x003c:
        r21 = new java.util.HashSet;
        r21.<init>();
        r0 = r25;
        r1 = r26;
        r3 = r21;
        r0.put(r1, r3);
    L_0x004a:
        r0 = r21;
        r1 = r23;
        r0.add(r1);
        goto L_0x000c;
    L_0x0052:
        r17 = r20;
        r17 = (com.android.launcher3.common.model.LauncherAppWidgetProviderInfo) r17;
        r0 = r31;
        r7 = r0.mManager;
        r0 = r17;
        r24 = r7.getUser(r0);
        r0 = r17;
        r7 = r0.provider;
        r23 = r7.getPackageName();
        goto L_0x002a;
    L_0x0069:
        r22 = new android.util.LongSparseArray;
        r22.<init>();
        r14 = 0;
        r0 = r31;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r0.mDb;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r6.getReadableDatabase();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r7 = "shortcut_and_widget_previews";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8 = 4;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8 = new java.lang.String[r8];	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r9 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r10 = "profileId";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8[r9] = r10;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r9 = 1;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r10 = "packageName";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8[r9] = r10;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r9 = 2;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r10 = "lastUpdated";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8[r9] = r10;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r9 = 3;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r10 = "version";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r8[r9] = r10;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r9 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r10 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r11 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r12 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r13 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r14 = r6.query(r7, r8, r9, r10, r11, r12, r13);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x0099:
        r6 = r14.moveToNext();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r6 == 0) goto L_0x010b;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x009f:
        r6 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r26 = r14.getLong(r6);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = 1;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r23 = r14.getString(r6);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = 2;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r18 = r14.getLong(r6);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = 3;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r28 = r14.getLong(r6);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r21 = r25.get(r26);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r21 = (java.util.HashSet) r21;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r21 == 0) goto L_0x00db;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00bb:
        r0 = r21;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r23;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r0.contains(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r6 == 0) goto L_0x00db;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00c5:
        r0 = r31;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r23;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r30 = r0.getPackageVersion(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = 0;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r30[r6];	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = (r6 > r28 ? 1 : (r6 == r28 ? 0 : -1));	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r6 != 0) goto L_0x00db;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00d4:
        r6 = 1;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r30[r6];	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = (r6 > r18 ? 1 : (r6 == r18 ? 0 : -1));	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r6 == 0) goto L_0x0099;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00db:
        r0 = r22;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r26;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r21 = r0.get(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r21 = (java.util.HashSet) r21;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r21 != 0) goto L_0x00f5;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00e7:
        r21 = new java.util.HashSet;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r21.<init>();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r22;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r26;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r3 = r21;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0.put(r1, r3);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x00f5:
        r0 = r21;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r23;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0.add(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        goto L_0x0099;
    L_0x00fd:
        r15 = move-exception;
        r6 = "WidgetPreviewLoader";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r7 = "Error updatating widget previews";	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        android.util.Log.e(r6, r7, r15);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r14 == 0) goto L_0x010a;
    L_0x0107:
        r14.close();
    L_0x010a:
        return;
    L_0x010b:
        r16 = 0;
    L_0x010d:
        r6 = r22.size();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r16;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r0 >= r6) goto L_0x0157;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x0115:
        r0 = r22;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r16;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r26 = r0.keyAt(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r31;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r0.mUserManager;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r26;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r24 = r6.getUserForSerialNumber(r0);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r22;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r16;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r0.valueAt(r1);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = (java.util.HashSet) r6;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r6 = r6.iterator();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x0135:
        r7 = r6.hasNext();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        if (r7 == 0) goto L_0x0154;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
    L_0x013b:
        r23 = r6.next();	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r23 = (java.lang.String) r23;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0 = r31;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r1 = r23;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r2 = r24;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r3 = r26;	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        r0.removePackage(r1, r2, r3);	 Catch:{ SQLException -> 0x00fd, all -> 0x014d }
        goto L_0x0135;
    L_0x014d:
        r6 = move-exception;
        if (r14 == 0) goto L_0x0153;
    L_0x0150:
        r14.close();
    L_0x0153:
        throw r6;
    L_0x0154:
        r16 = r16 + 1;
        goto L_0x010d;
    L_0x0157:
        if (r14 == 0) goto L_0x010a;
    L_0x0159:
        r14.close();
        goto L_0x010a;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.widget.model.WidgetPreviewLoader.removeObsoletePreviews(java.util.ArrayList):void");
    }

    public WidgetPreviewLoader(Context context, IconCache iconCache) {
        this.mContext = context;
        this.mIconCache = iconCache;
        this.mManager = AppWidgetManagerCompat.getInstance(context);
        this.mUserManager = UserManagerCompat.getInstance(context);
        this.mDb = new CacheDb(context);
        this.mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
        this.mProfileBadgeMargin = context.getResources().getDimensionPixelSize(R.dimen.profile_badge_margin);
    }

    public PreviewLoadRequest getPreview(Object o, int previewWidth, int previewHeight, PreviewLoadListener caller) {
        PreviewLoadTask task = new PreviewLoadTask(getObjectKey(o, previewWidth + DefaultLayoutParser.ATTR_X + previewHeight), o, previewWidth, previewHeight, caller);
        task.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        return new PreviewLoadRequest(task);
    }

    private WidgetCacheKey getObjectKey(Object o, String size) {
        if (o instanceof LauncherAppWidgetProviderInfo) {
            LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) o;
            return new WidgetCacheKey(info.provider, this.mManager.getUser(info), size);
        } else if (o instanceof PinShortcutRequestActivityInfo) {
            return new WidgetCacheKey(((PinShortcutRequestActivityInfo) o).getComponentName(), UserHandleCompat.myUserHandle(), size);
        } else {
            ResolveInfo info2 = (ResolveInfo) o;
            return new WidgetCacheKey(new ComponentName(info2.activityInfo.packageName, info2.activityInfo.name), UserHandleCompat.myUserHandle(), size);
        }
    }

    private void writeToDb(WidgetCacheKey key, long[] versions, Bitmap preview) {
        ContentValues values = new ContentValues();
        values.put(PostPositionProvider.COL_COMPONENT_NAME, key.componentName.flattenToShortString());
        values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(this.mUserManager.getSerialNumberForUser(key.user)));
        values.put(Key.SIZE, key.size);
        values.put(DefaultLayoutParser.ATTR_PACKAGE_NAME, key.componentName.getPackageName());
        values.put("version", Long.valueOf(versions[0]));
        values.put("lastUpdated", Long.valueOf(versions[1]));
        values.put("preview_bitmap", Utilities.flattenBitmap(preview));
        try {
            this.mDb.getWritableDatabase().insertWithOnConflict("shortcut_and_widget_previews", null, values, 5);
        } catch (SQLException e) {
            Log.e(TAG, "Error saving image to DB", e);
        }
    }

    public void removePackage(String packageName, UserHandleCompat user) {
        removePackage(packageName, user, this.mUserManager.getSerialNumberForUser(user));
    }

    private void removePackage(String packageName, UserHandleCompat user, long userSerial) {
        synchronized (this.mPackageVersions) {
            this.mPackageVersions.remove(packageName);
        }
        try {
            this.mDb.getWritableDatabase().delete("shortcut_and_widget_previews", "packageName = ? AND profileId = ?", new String[]{packageName, Long.toString(userSerial)});
        } catch (SQLException e) {
            Log.e(TAG, "Unable to delete items from DB", e);
        }
    }

    private Bitmap readFromDb(WidgetCacheKey key, Bitmap recycle, PreviewLoadTask loadTask) {
        Cursor cursor = null;
        try {
            cursor = this.mDb.getReadableDatabase().query("shortcut_and_widget_previews", new String[]{"preview_bitmap"}, "componentName = ? AND profileId = ? AND size = ?", new String[]{key.componentName.flattenToString(), Long.toString(this.mUserManager.getSerialNumberForUser(key.user)), key.size}, null, null, null);
            if (!loadTask.isCancelled()) {
                if (cursor.moveToNext()) {
                    byte[] blob = cursor.getBlob(0);
                    Options opts = new Options();
                    opts.inBitmap = recycle;
                    try {
                        if (!loadTask.isCancelled()) {
                            Bitmap decodeByteArray = BitmapFactory.decodeByteArray(blob, 0, blob.length, opts);
                            if (cursor == null) {
                                return decodeByteArray;
                            }
                            cursor.close();
                            return decodeByteArray;
                        }
                    } catch (Exception e) {
                        if (cursor == null) {
                            return null;
                        }
                        cursor.close();
                        return null;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            } else if (cursor == null) {
                return null;
            } else {
                cursor.close();
                return null;
            }
        } catch (SQLException e2) {
            Log.w(TAG, "Error loading preview from DB", e2);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Bitmap generatePreview(Context context, Object info, Bitmap recycle, int previewWidth, int previewHeight) {
        if (info instanceof LauncherAppWidgetProviderInfo) {
            return generateWidgetPreview((LauncherAppWidgetProviderInfo) info, previewWidth, recycle, null);
        }
        if (info instanceof PinShortcutRequestActivityInfo) {
            return generateShortcutPreview(context, (PinShortcutRequestActivityInfo) info);
        }
        return generateShortcutPreview((ResolveInfo) info, previewWidth, previewHeight, recycle);
    }

    public Bitmap generateWidgetPreview(LauncherAppWidgetProviderInfo info, int maxPreviewWidth, Bitmap preview, int[] preScaledWidthOut) {
        int previewWidth;
        int previewHeight;
        if (maxPreviewWidth < 0) {
            maxPreviewWidth = Integer.MAX_VALUE;
        }
        Drawable drawable = null;
        if (info.previewImage != 0) {
            drawable = this.mManager.loadPreview(info);
            if (drawable != null) {
                drawable = mutateOnMainThread(drawable);
            } else {
                Log.w(TAG, "Can't load widget preview drawable 0x" + Integer.toHexString(info.previewImage) + " for provider: " + info.provider);
            }
        }
        boolean widgetPreviewExists = drawable != null;
        int spanX = info.getSpanX();
        int spanY = info.getSpanY();
        Bitmap tileBitmap = null;
        if (widgetPreviewExists) {
            previewWidth = drawable.getIntrinsicWidth();
            previewHeight = drawable.getIntrinsicHeight();
        } else {
            tileBitmap = ((BitmapDrawable) this.mContext.getResources().getDrawable(R.drawable.widget_tile)).getBitmap();
            previewWidth = tileBitmap.getWidth() * spanX;
            previewHeight = tileBitmap.getHeight() * spanY;
        }
        float scale = 1.0f;
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth;
        }
        if (previewWidth > maxPreviewWidth) {
            scale = ((float) (maxPreviewWidth - (this.mProfileBadgeMargin * 2))) / ((float) previewWidth);
        }
        if (scale != 1.0f) {
            previewWidth = (int) (((float) previewWidth) * scale);
            previewHeight = (int) (((float) previewHeight) * scale);
        }
        Canvas c = new Canvas();
        if (preview == null) {
            preview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
            c.setBitmap(preview);
        } else {
            c.setBitmap(preview);
            c.drawColor(0, Mode.CLEAR);
        }
        int x = (preview.getWidth() - previewWidth) / 2;
        if (widgetPreviewExists) {
            drawable.setBounds(x, 0, x + previewWidth, previewHeight);
            drawable.draw(c);
        } else {
            Paint p = new Paint();
            p.setFilterBitmap(true);
            int appIconSize = LauncherAppState.getInstance().getDeviceProfile().defaultIconSize;
            Rect rect = new Rect(0, 0, tileBitmap.getWidth(), tileBitmap.getHeight());
            float tileW = scale * ((float) tileBitmap.getWidth());
            float tileH = scale * ((float) tileBitmap.getHeight());
            RectF dst = new RectF(0.0f, 0.0f, tileW, tileH);
            float tx = (float) x;
            int i = 0;
            while (i < spanX) {
                float ty = 0.0f;
                int j = 0;
                while (j < spanY) {
                    dst.offsetTo(tx, ty);
                    c.drawBitmap(tileBitmap, rect, dst, p);
                    j++;
                    ty += tileH;
                }
                i++;
                tx += tileW;
            }
            float iconScale = Math.min(((float) Math.min(previewWidth, previewHeight)) / ((float) ((((int) (((float) appIconSize) * WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE)) * 2) + appIconSize)), scale);
            try {
                Drawable icon = mutateOnMainThread(this.mManager.loadIcon(info, this.mIconCache));
                if (icon != null) {
                    int hoffset = ((int) ((tileW - (((float) appIconSize) * iconScale)) / 2.0f)) + x;
                    int yoffset = (int) ((tileH - (((float) appIconSize) * iconScale)) / 2.0f);
                    icon.setBounds(hoffset, yoffset, ((int) (((float) appIconSize) * iconScale)) + hoffset, ((int) (((float) appIconSize) * iconScale)) + yoffset);
                    icon.draw(c);
                }
            } catch (NotFoundException e) {
            }
            c.setBitmap(null);
        }
        return this.mManager.getBadgeBitmap(info, preview, Math.min(preview.getHeight(), this.mProfileBadgeMargin + previewHeight));
    }

    private Bitmap generateShortcutPreview(ResolveInfo info, int maxWidth, int maxHeight, Bitmap preview) {
        Canvas c = new Canvas();
        if (preview == null) {
            preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
            c.setBitmap(preview);
        } else if (preview.getWidth() == maxWidth && preview.getHeight() == maxHeight) {
            c.setBitmap(preview);
            c.drawColor(0, Mode.CLEAR);
        } else {
            throw new RuntimeException("Improperly sized bitmap passed as argument");
        }
        Drawable icon = mutateOnMainThread(this.mIconCache.getFullResIcon(info.activityInfo));
        icon.setFilterBitmap(true);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.0f);
        icon.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        icon.setAlpha(15);
        Resources res = this.mContext.getResources();
        int paddingTop = res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft = res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int scaledIconWidth = (maxWidth - paddingLeft) - res.getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);
        icon.setBounds(paddingLeft, paddingTop, paddingLeft + scaledIconWidth, paddingTop + scaledIconWidth);
        icon.draw(c);
        int appIconSize = LauncherAppState.getInstance().getDeviceProfile().defaultIconSize;
        icon.setAlpha(255);
        icon.setColorFilter(null);
        icon.setBounds(0, 0, appIconSize, appIconSize);
        icon.draw(c);
        c.setBitmap(null);
        return preview;
    }

    private Bitmap generateShortcutPreview(Context launcher, PinShortcutRequestActivityInfo info) {
        Drawable unbadgedDrawable = info.getFullResIcon(this.mIconCache);
        if (unbadgedDrawable == null) {
            return this.mIconCache.getDefaultIcon(UserHandleCompat.myUserHandle());
        }
        return BitmapUtils.createIconBitmap(unbadgedDrawable, launcher);
    }

    private Drawable mutateOnMainThread(final Drawable drawable) {
        try {
            return (Drawable) this.mMainThreadExecutor.submit(new Callable<Drawable>() {
                public Drawable call() throws Exception {
                    return drawable.mutate();
                }
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e2) {
            throw new RuntimeException(e2);
        }
    }

    private long[] getPackageVersion(String packageName) {
        long[] versions;
        synchronized (this.mPackageVersions) {
            versions = (long[]) this.mPackageVersions.get(packageName);
            if (versions == null) {
                versions = new long[2];
                try {
                    PackageInfo info = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
                    versions[0] = (long) info.versionCode;
                    versions[1] = info.lastUpdateTime;
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "PackageInfo not found", e);
                }
                this.mPackageVersions.put(packageName, versions);
            }
        }
        return versions;
    }
}
