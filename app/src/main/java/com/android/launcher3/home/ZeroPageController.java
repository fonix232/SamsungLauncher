package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.PagedView.ScrollInterpolator;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.vcard.VCardConfig;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.sdk.virtualscreen.SVirtualScreen;
import com.samsung.android.sdk.virtualscreen.SVirtualScreenManager;
import com.samsung.android.sdk.virtualscreen.SVirtualScreenManager.LaunchParams;
import com.sec.android.app.launcher.R;
import java.lang.ref.WeakReference;

public class ZeroPageController {
    private static final String ACTION_INTENT_ACTIVE_ZEROPAGE = "com.sec.android.intent.action.ACTIVE_ZERO_PAGE";
    public static final String ACTION_INTENT_SET_ZEROPAGE = "com.sec.android.intent.action.SET_ZERO_PAGE";
    private static final int FROM_HOMEPAGE = 2;
    private static final int FROM_ZEROPAGE = 1;
    private static final String METADATA_ZEROPAGE = "com.samsung.launcher.zeropage.metadata";
    private static final int MINIMUM_SNAP_VELOCITY = 1500;
    private static final int MOVE_CONTINUE = 22;
    private static final int MOVE_NONE = 0;
    private static final float PAGE_SNAP_MOVING_RATIO = 0.33f;
    private static final int PAGE_SNAP_VALUE_ANIMATION_DURATION = 300;
    private static final String TAG = "ZeroPageController";
    private static final int THREAD_EXIT_DELAY = 3000;
    private static final int TO_HOMEPAGE = 8;
    private static final int TO_ZEROEPAGE = 4;
    private static final ComponentName[] ZERO_PAGE_APP_LIST = new ComponentName[]{new ComponentName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_MAIN), new ComponentName(Utilities.TOUTIAO_NEWS_PACKAGE_NAME, Utilities.TOUTIAO_NEWS_CLASS_NAME), new ComponentName(Utilities.AXEL_UPDAY_PACKAGE_NAME, Utilities.AXEL_UPDAY_CLASS_NAME), new ComponentName(Utilities.SOHU_NEWS_PACKAGE_NAME, Utilities.SOHU_NEWS_CLASS_NAME), new ComponentName(Utilities.FLIPBOARD_BRIEFING_PACKAGE_NAME, Utilities.FLIPBOARD_BRIEFING_CLASS_NAME)};
    public static final int ZERO_PAGE_SCREEN_INDEX = -1;
    private static boolean sActiveZeroPage = true;
    private static boolean sEnableZeroPage = supportVirtualScreen();
    private static boolean sSupportVirtualScreen = false;
    private static boolean sVirtualScreenAvailableChecked = false;
    private static SVirtualScreenManager sVirtualScreenManager;
    public static ComponentName sZeroPageCompName = new ComponentName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_MAIN);
    private static boolean sZeroPageDefaultOnOffState = true;
    private String mAppName;
    private int mAppPrevResId;
    private final ZeroPagePreview mAppPreview;
    private int mBezelSize = 0;
    private boolean mBezelSwipe = false;
    private int mDownX = 0;
    private boolean mInstalled;
    private long mInterval = 0;
    private boolean mIsFromZeroPageSetting = false;
    private int mLastDownX = 0;
    private final Launcher mLauncher;
    private int mMaximumVelocity;
    private boolean mMovedToVirtualScreen;
    private int mMovingState = 0;
    private int mPreValues = 0;
    private DisplayMetrics mRealMetric = null;
    private Alarm mThreadExitAlarm;
    private boolean mTouchDowned = false;
    private int mTouchSlop;
    private ValueAnimator mValueAnimator = new ValueAnimator();
    private VelocityTracker mVelocityTracker;
    private VirtualScreenHandler mVirtualScreenHandler;
    private HandlerThread mVirtualScreenThread;
    private final Workspace mWorkspace;
    private LinearLayout mZeroPageBgView = null;
    private int mZeroPageBitmapHeight;
    private int mZeroPageBitmapWidth;
    private ZeroPagePreviewTask mZeroPagePreviewTask;
    private AlertDialog mZeropageDownloadDialog = null;

    private class VirtualScreenHandler extends Handler {
        private static final long MAX_INTERVAL = 25;
        private static final long MIN_INTERVAL = 11;
        private static final int MSG_SET_OFFSET = 1;
        private final WeakReference<ZeroPageController> mController;
        private int mPreOffset = -1;
        private boolean mStop = false;

        VirtualScreenHandler(ZeroPageController controller, Looper looper) {
            super(looper);
            this.mController = new WeakReference(controller);
        }

        synchronized void initPreOffset() {
            this.mPreOffset = -1;
        }

        synchronized void removeMsg() {
            removeCallbacksAndMessages(null);
            this.mStop = true;
            this.mPreOffset = -1;
            ZeroPageController.this.mThreadExitAlarm.cancelAlarm();
        }

        synchronized void setOffsetHandler(Bundle options) {
            Message msg = new Message();
            msg.what = 1;
            msg.setData(options);
            sendMessage(msg);
        }

        public void handleMessage(Message msg) {
            if (msg != null) {
                ZeroPageController controller = (ZeroPageController) this.mController.get();
                if (controller != null) {
                    switch (msg.what) {
                        case 1:
                            if (msg.getData() != null) {
                                int offset = msg.getData().getInt("offsetX", 0);
                                long interval = msg.getData().getLong("interval", 0);
                                if (offset != this.mPreOffset) {
                                    boolean force = msg.getData().getBoolean("force", false);
                                    ZeroPageController.this.mThreadExitAlarm.setAlarm(3000);
                                    this.mStop = false;
                                    if (interval != 0 && (interval < MIN_INTERVAL || interval > MAX_INTERVAL)) {
                                        interval = Math.min(MAX_INTERVAL, Math.max(interval, MIN_INTERVAL));
                                    }
                                    do {
                                        if (controller.setOffset(offset, 0, force)) {
                                            if (force && offset != 0 && offset == this.mPreOffset) {
                                                ZeroPageController.this.restoreOffset();
                                            }
                                            this.mStop = true;
                                        }
                                        try {
                                            Thread.sleep(interval);
                                        } catch (InterruptedException e) {
                                            Log.e(ZeroPageController.TAG, "handleMessage - InterruptedException");
                                        }
                                        if (this.mStop) {
                                        }
                                        this.mPreOffset = offset;
                                        ZeroPageController.this.mThreadExitAlarm.cancelAlarm();
                                        return;
                                    } while (!ZeroPageController.this.mLauncher.isPaused());
                                    this.mPreOffset = offset;
                                    ZeroPageController.this.mThreadExitAlarm.cancelAlarm();
                                    return;
                                }
                                if (interval < 0 || interval > MAX_INTERVAL) {
                                    interval = Math.max(0, Math.min(interval, MAX_INTERVAL));
                                }
                                try {
                                    Thread.sleep(interval);
                                } catch (InterruptedException e2) {
                                    Log.e(ZeroPageController.TAG, "handleMessage - InterruptedException");
                                }
                                Log.d(ZeroPageController.TAG, "skip setOffset - offsetX = " + offset);
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                }
            }
        }
    }

    private static class ZeroPagePreview {
        private FastBitmapDrawable landscapePreview;
        private FastBitmapDrawable portraitPreview;

        private ZeroPagePreview() {
        }

        FastBitmapDrawable getMatchedPreview() {
            if (Utilities.isPortrait()) {
                return this.portraitPreview;
            }
            return this.landscapePreview;
        }

        void setMatchedPreview(FastBitmapDrawable preview) {
            if (Utilities.isPortrait()) {
                this.portraitPreview = preview;
            } else {
                this.landscapePreview = preview;
            }
        }

        void removePreview() {
            if (!(this.portraitPreview == null || this.portraitPreview.getBitmap() == null)) {
                this.portraitPreview.getBitmap().recycle();
            }
            if (!(this.landscapePreview == null || this.landscapePreview.getBitmap() == null)) {
                this.landscapePreview.getBitmap().recycle();
            }
            this.portraitPreview = null;
            this.landscapePreview = null;
        }
    }

    private class ZeroPagePreviewTask extends AsyncTask<Void, Void, Void> {
        private ZeroPagePreviewTask() {
        }

        protected Void doInBackground(Void... voids) {
            Bitmap bmp = ZeroPageController.this.getPreviewFromPackageManager(ZeroPageController.this.mLauncher.getPackageManager());
            if (bmp != null) {
                ZeroPageController.this.mAppPreview.setMatchedPreview(new FastBitmapDrawable(ZeroPageController.this.scaleBitmapIfNecessary(bmp)));
            } else {
                Log.e(ZeroPageController.TAG, "ZeroPreviewTask: doInBackground() : bitmap not found for app=" + ZeroPageController.sZeroPageCompName);
                if (!ZeroPageController.this.mInstalled) {
                    ZeroPageController.this.mAppPreview.setMatchedPreview(new FastBitmapDrawable(ZeroPageController.this.scaleBitmapIfNecessary(BitmapFactory.decodeResource(ZeroPageController.this.mLauncher.getResources(), ZeroPageController.this.getZeroPagePreviewId(ZeroPageController.sZeroPageCompName.getPackageName())))));
                }
            }
            return null;
        }

        public void onCancelled() {
            Log.d(ZeroPageController.TAG, "cancelled ZeroPreviewTask");
        }

        protected void onPostExecute(Void result) {
            Log.d(ZeroPageController.TAG, "ZeroPreviewTask onPostExecute()");
            if (ZeroPageController.this.mZeroPageBgView == null) {
                return;
            }
            if (ZeroPageController.this.mAppPreview.getMatchedPreview() != null) {
                ZeroPageController.this.changePreviewImage();
            } else if (LauncherFeature.supportSetToZeroPage()) {
                Resources res = ZeroPageController.this.mLauncher.getResources();
                int zeroPageSwitchHeight = (int) (((float) res.getDimensionPixelSize(R.dimen.overview_zeropage_switch_height)) * (100.0f / ((float) res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage))));
                int margin = res.getDimensionPixelSize(R.dimen.zero_page_bg_margin);
                MarginLayoutParams mlp = (MarginLayoutParams) ZeroPageController.this.mZeroPageBgView.getLayoutParams();
                mlp.setMargins(margin, margin + zeroPageSwitchHeight, margin, margin);
                ZeroPageController.this.mZeroPageBgView.setLayoutParams(mlp);
                ZeroPageController.this.mZeroPageBgView.setBackgroundColor(-1);
                ZeroPageController.this.mZeroPageBgView.setGravity(17);
                PackageManager pm = ZeroPageController.this.mLauncher.getPackageManager();
                if (pm != null) {
                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(ZeroPageController.this.getZeroPagePackageName(), 0);
                        if (appInfo.loadIcon(pm) != null) {
                            ImageView iv = new ImageView(ZeroPageController.this.mLauncher);
                            iv.setLayoutParams(new LayoutParams(-2, -2));
                            iv.setBackground(appInfo.loadIcon(pm));
                            ZeroPageController.this.mZeroPageBgView.addView(iv);
                            return;
                        }
                        TextView tv = new TextView(ZeroPageController.this.mLauncher);
                        tv.setLayoutParams(new LayoutParams(-2, -2));
                        tv.setText(appInfo.loadLabel(pm));
                        ZeroPageController.this.mZeroPageBgView.addView(tv);
                    } catch (NameNotFoundException e) {
                        Log.e(ZeroPageController.TAG, "application info load failed : " + e.toString());
                    }
                }
            }
        }
    }

    ZeroPageController(Context context, Workspace workspace) {
        this.mLauncher = (Launcher) context;
        this.mWorkspace = workspace;
        this.mAppPreview = new ZeroPagePreview();
        init();
        setZeroPageActiveState(this.mLauncher);
    }

    void setup() {
        if (sEnableZeroPage) {
            this.mVirtualScreenThread = new HandlerThread("VirtualScreenThread");
            this.mVirtualScreenThread.setPriority(10);
            this.mVirtualScreenThread.start();
            this.mVirtualScreenHandler = new VirtualScreenHandler(this, this.mVirtualScreenThread.getLooper());
            sVirtualScreenManager = new SVirtualScreenManager(this.mLauncher);
            if (Utilities.isDeskTopMode(this.mLauncher)) {
                Log.e(TAG, "DeX mode - do not startActivityInVirtualScreen");
            } else if (getZeroPageActiveState(this.mLauncher, false)) {
                startActivityInVirtualScreen(true, true);
                bindVirtualScreen();
            }
            this.mThreadExitAlarm = new Alarm();
            this.mThreadExitAlarm.setOnAlarmListener(new OnAlarmListener() {
                public void onAlarm(Alarm alarm) {
                    Log.d(ZeroPageController.TAG, "Virtual screen thread exit - onAlarm");
                    ZeroPageController.this.restoreOffset();
                }
            });
            ViewConfiguration configuration = ViewConfiguration.get(this.mLauncher.getApplicationContext());
            this.mTouchSlop = configuration.getScaledTouchSlop();
            this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
            this.mBezelSize = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.zero_page_bezel_swipe_size);
        }
    }

    private void changePreviewImage() {
        if (this.mZeroPageBgView == null) {
            return;
        }
        if (this.mAppPreview.getMatchedPreview() != null) {
            ImageView imageView = new ImageView(this.mLauncher);
            imageView.setImageDrawable(this.mAppPreview.getMatchedPreview());
            this.mZeroPageBgView.addView(imageView, 0, new LinearLayout.LayoutParams(-1, -1));
            return;
        }
        loadZeroPagePreviewBitmap();
    }

    private Bitmap scaleBitmapIfNecessary(Bitmap bmp) {
        Log.d(TAG, "scaleBitmapIfNecessary() ");
        updatePreviewSize();
        if (this.mZeroPageBitmapWidth == bmp.getWidth() && this.mZeroPageBitmapHeight == bmp.getHeight()) {
            return bmp;
        }
        Log.d(TAG, "scaleBitmapIfNecessary(): scaling bitmap");
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, this.mZeroPageBitmapWidth, this.mZeroPageBitmapHeight, true);
        if (bmp != scaledBitmap) {
            bmp.recycle();
        }
        return scaledBitmap;
    }

    private Bitmap getPreviewFromPackageManager(PackageManager pm) {
        try {
            return BitmapFactory.decodeResource(pm.getResourcesForApplication(pm.getActivityInfo(sZeroPageCompName, 640).applicationInfo), this.mAppPrevResId);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getPreviewFromPackageManager(): NameNotFoundException: " + e.getMessage());
            return null;
        }
    }

    private void loadZeroPagePreviewBitmap() {
        cancelZeroPagePreviewTask();
        this.mZeroPagePreviewTask = new ZeroPagePreviewTask();
        this.mZeroPagePreviewTask.execute(new Void[0]);
    }

    private void cancelZeroPagePreviewTask() {
        if (this.mZeroPagePreviewTask != null) {
            this.mZeroPagePreviewTask.cancel(true);
            this.mZeroPagePreviewTask = null;
        }
    }

    void onDestroy() {
        if (this.mVirtualScreenHandler != null) {
            this.mVirtualScreenHandler.removeCallbacksAndMessages(null);
        }
        if (this.mVirtualScreenThread != null) {
            this.mVirtualScreenThread.quitSafely();
        }
    }

    public static boolean supportVirtualScreen() {
        if (!(sVirtualScreenAvailableChecked || LauncherAppState.getInstance().getContext() == null || LauncherAppState.getInstance().getContext().getPackageManager().isSafeMode())) {
            sSupportVirtualScreen = new SVirtualScreen().isFeatureEnabled(1);
            sVirtualScreenAvailableChecked = true;
        }
        return sSupportVirtualScreen;
    }

    public static boolean isActiveZeroPage(Context context, boolean getPreferences) {
        return sEnableZeroPage && getZeroPageActiveState(context, getPreferences);
    }

    public static ComponentName getZeroPageContents(Context context) {
        ComponentName componentName = null;
        int i = 0;
        if (supportVirtualScreen()) {
            String cscFeatureZeroPageApp = SemCscFeature.getInstance().getString("CscFeature_Launcher_ConfigZeroPageApp", null);
            componentName = new ComponentName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_MAIN);
            ComponentName[] componentNameArr;
            int length;
            ComponentName name;
            if (cscFeatureZeroPageApp == null) {
                componentNameArr = ZERO_PAGE_APP_LIST;
                length = componentNameArr.length;
                while (i < length) {
                    name = componentNameArr[i];
                    if (Utilities.isPackageExist(context, name.getPackageName())) {
                        componentName = name;
                        break;
                    }
                    i++;
                }
            } else {
                Log.d(TAG, "csc zero page app is not null : " + cscFeatureZeroPageApp);
                ComponentName zeroPageApp = ComponentName.unflattenFromString(cscFeatureZeroPageApp);
                componentNameArr = ZERO_PAGE_APP_LIST;
                length = componentNameArr.length;
                while (i < length) {
                    name = componentNameArr[i];
                    if (name.equals(zeroPageApp)) {
                        componentName = name;
                        break;
                    }
                    i++;
                }
            }
            Log.d(TAG, "ZeroPageContents : " + componentName);
        } else {
            Log.d(TAG, "not support virtual screen");
        }
        return componentName;
    }

    public static boolean isEnableZeroPage() {
        return sEnableZeroPage;
    }

    private void init() {
        String cscFeatureZeroPageApp = SemCscFeature.getInstance().getString("CscFeature_Launcher_ConfigZeroPageApp", null);
        String cscFeatureZeroPageEnable = SemCscFeature.getInstance().getString("CscFeature_Launcher_ConfigMagazineHome", null);
        boolean isDeletable = false;
        this.mInstalled = false;
        sZeroPageDefaultOnOffState = !"off".equalsIgnoreCase(cscFeatureZeroPageEnable);
        if (cscFeatureZeroPageApp == null) {
            for (ComponentName name : ZERO_PAGE_APP_LIST) {
                this.mInstalled = Utilities.isPackageExist(this.mLauncher, name.getPackageName());
                if (this.mInstalled) {
                    sZeroPageCompName = name;
                    break;
                }
            }
        } else {
            ComponentName zeroPageApp = ComponentName.unflattenFromString(cscFeatureZeroPageApp);
            for (ComponentName name2 : ZERO_PAGE_APP_LIST) {
                if (name2.equals(zeroPageApp)) {
                    isDeletable = true;
                    this.mInstalled = Utilities.isPackageExist(this.mLauncher, name2.getPackageName());
                    sZeroPageCompName = name2;
                    break;
                }
            }
        }
        if (sZeroPageCompName.equals(new ComponentName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_MAIN))) {
            cscFeatureZeroPageEnable = null;
            sZeroPageDefaultOnOffState = true;
        }
        if ("disable".equalsIgnoreCase(cscFeatureZeroPageEnable) || Utilities.isKnoxMode() || (!(this.mInstalled || isDeletable) || Utilities.isGuest())) {
            sEnableZeroPage = false;
        } else {
            sEnableZeroPage = true;
            updateZeroPageAppMetadata(sZeroPageCompName);
            if (!this.mInstalled) {
                setZeroPageActiveState(this.mLauncher, this.mInstalled);
            }
        }
        this.mRealMetric = getRealMetrics();
        LauncherAppState.getInstance().setEnableZeroPage(sEnableZeroPage);
    }

    private void updateZeroPageAppMetadata(ComponentName componentName) {
        String name;
        try {
            Exception e;
            PackageManager pm = this.mLauncher.getPackageManager();
            ActivityInfo info = pm.getActivityInfo(componentName, 640);
            XmlResourceParser parser = info.loadXmlMetaData(pm, METADATA_ZEROPAGE);
            if (parser == null) {
                Log.e(TAG, "parser is null");
                if (LauncherFeature.supportSetToZeroPage() && componentName != null) {
                    setZeroPagePackageName(componentName.getPackageName());
                    setZeroPageClassName(componentName.getClassName());
                    name = pm.getApplicationInfo(componentName.getPackageName(), 0).loadLabel(pm).toString();
                    if (Utilities.sIsRtl) {
                        name = "‏" + name;
                    }
                    this.mAppName = name;
                    sZeroPageCompName = componentName;
                    this.mAppPrevResId = -1;
                    return;
                }
                return;
            }
            try {
                int depth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                        break;
                    } else if (type == 2) {
                        int appnameStrId = parser.getAttributeResourceValue(null, "apptitle", 0);
                        int previewResId = parser.getAttributeResourceValue(null, "preview", 0);
                        Resources res = pm.getResourcesForApplication(info.applicationInfo);
                        if (res == null) {
                            continue;
                        } else {
                            String str;
                            if (previewResId != 0) {
                                try {
                                    this.mAppPrevResId = previewResId;
                                } catch (NotFoundException e2) {
                                    Log.e(TAG, "exception on updateZeroPageAppMetadata : " + e2.getMessage());
                                }
                            }
                            if (appnameStrId == 0) {
                                str = null;
                            } else if (Utilities.sIsRtl) {
                                str = "‏" + res.getString(appnameStrId);
                            } else {
                                str = res.getString(appnameStrId);
                            }
                            this.mAppName = str;
                        }
                    }
                }
            } catch (Exception e3) {
                e = e3;
            } catch (Exception e32) {
                e = e32;
            } catch (Exception e322) {
                e = e322;
            }
            setZeroPagePackageName(componentName.getPackageName());
            setZeroPageClassName(componentName.getClassName());
            Log.e(TAG, "exception on updateZeroPageAppMetadata : " + e.getMessage());
            setZeroPagePackageName(componentName.getPackageName());
            setZeroPageClassName(componentName.getClassName());
        } catch (NameNotFoundException e4) {
            Log.e(TAG, "ZeroApp doesn't have Metadata : " + componentName);
            if (!this.mInstalled) {
                name = getZeroPageTitle(componentName.getPackageName());
                if (Utilities.sIsRtl) {
                    name = "‏" + name;
                }
                this.mAppName = name;
            }
        }
    }

    private void setZeroPagePackageName(String packageName) {
        Editor editor = this.mLauncher.getSharedPrefs().edit();
        editor.putString(LauncherFiles.ZEROPAGE_PACKAGE_NAME_KEY, packageName);
        editor.apply();
    }

    private String getZeroPagePackageName() {
        return this.mLauncher.getSharedPrefs().getString(LauncherFiles.ZEROPAGE_PACKAGE_NAME_KEY, "");
    }

    private void setZeroPageClassName(String className) {
        Editor editor = this.mLauncher.getSharedPrefs().edit();
        editor.putString(LauncherFiles.ZEROPAGE_CLASS_NAME_KEY, className);
        editor.apply();
    }

    private String getZeroPageClassName() {
        return this.mLauncher.getSharedPrefs().getString(LauncherFiles.ZEROPAGE_CLASS_NAME_KEY, "");
    }

    private static void setZeroPageActiveState(Context context) {
        setZeroPageActiveState(context, getZeroPageActiveState(context, true));
    }

    static void setZeroPageActiveState(Context context, boolean active) {
        Log.i(TAG, "setZeroPageActiveState, active: " + active + ", enable: " + sEnableZeroPage);
        if (sEnableZeroPage) {
            Intent intent = new Intent(ACTION_INTENT_ACTIVE_ZEROPAGE);
            intent.addFlags(32);
            intent.putExtra("active", active);
            context.sendBroadcast(intent);
            Editor editor = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
            editor.putBoolean(LauncherFiles.ZEROPAGE_ACTIVE_STATE_KEY, active);
            editor.apply();
            sActiveZeroPage = active;
            SALogging.getInstance().insertStatusLog(context.getResources().getString(R.string.status_zeropagesetting), sActiveZeroPage ? "1" : "0");
        }
    }

    public static boolean getZeroPageActiveState(Context context, boolean getPreferences) {
        if (!sEnableZeroPage) {
            return false;
        }
        if (getPreferences) {
            sActiveZeroPage = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(LauncherFiles.ZEROPAGE_ACTIVE_STATE_KEY, sZeroPageDefaultOnOffState);
        }
        return sActiveZeroPage;
    }

    private void startActivityInVirtualScreen(boolean changeActivity, boolean created) {
        Log.d(TAG, "startActivityInVirtualScreen - changeActivity = " + changeActivity + ", created = " + created);
        if (sVirtualScreenManager == null) {
            Log.e(TAG, "startActivityInVirtualScreen - return by sVirtualScreenManager is null");
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getZeroPagePackageName(), getZeroPageClassName()));
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        intent.putExtra("fromHome", true);
        intent.putExtra("fingerSwipe", true);
        intent.putExtra("supportRtl", Utilities.sIsRtl);
        LaunchParams params = new LaunchParams();
        if (Utilities.sIsRtl) {
            params.bounds = new Rect(this.mRealMetric.widthPixels, 0, this.mRealMetric.widthPixels * 2, this.mRealMetric.heightPixels);
        } else {
            params.bounds = new Rect(-this.mRealMetric.widthPixels, 0, 0, this.mRealMetric.heightPixels);
        }
        params.flags |= LaunchParams.FLAG_BASE_ACTIVITY;
        params.flags |= LaunchParams.FLAG_ZEROPAGE_POLICY;
        if (changeActivity) {
            params.flags |= LaunchParams.FLAG_CLEAR_TASKS;
        }
        if (created) {
            params.flags |= LaunchParams.FLAG_RECREATE_VIRTUALSCREEN;
        }
        sVirtualScreenManager.startActivity(intent, null, params);
    }

    private void bindVirtualScreen() {
        if (sVirtualScreenManager != null) {
            sVirtualScreenManager.bindVirtualScreen();
        }
    }

    private void unBindVirtualScreen() {
        if (sVirtualScreenManager != null) {
            sVirtualScreenManager.unBindVirtualScreen();
        }
    }

    private boolean setOffset(int offsetX, int offsetY, boolean force) {
        Log.d(TAG, "setOffset - offsetX = " + offsetX + ", force = " + force);
        return sVirtualScreenManager != null && sVirtualScreenManager.setOffset(offsetX, offsetY, force);
    }

    private void setOffsetMsg(int offsetX, int offsetY, boolean force, long interval) {
        Log.d(TAG, "setOffsetMsg - offsetX = " + offsetX + ", force = " + force);
        Bundle options = new Bundle();
        options.putInt("offsetX", offsetX);
        options.putBoolean("force", force);
        options.putLong("interval", interval);
        if (this.mVirtualScreenHandler != null) {
            this.mVirtualScreenHandler.setOffsetHandler(options);
        }
    }

    public static boolean isMoving() {
        return sVirtualScreenManager != null && sVirtualScreenManager.isMoving();
    }

    Point getOffset() {
        if (sVirtualScreenManager != null) {
            return sVirtualScreenManager.getOffset();
        }
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean dispatchTouchEvent(android.view.MotionEvent r23) {
        /*
        r22 = this;
        r2 = supportVirtualScreen();
        if (r2 == 0) goto L_0x0025;
    L_0x0006:
        r0 = r22;
        r2 = r0.mLauncher;
        r4 = 0;
        r2 = isActiveZeroPage(r2, r4);
        if (r2 == 0) goto L_0x0025;
    L_0x0011:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.isHomeStage();
        if (r2 == 0) goto L_0x0025;
    L_0x001b:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.isNormalState();
        if (r2 != 0) goto L_0x0028;
    L_0x0025:
        r16 = 0;
    L_0x0027:
        return r16;
    L_0x0028:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.getHomeController();
        r2 = r2.isSwitchingState();
        if (r2 == 0) goto L_0x0043;
    L_0x0036:
        r2 = "ZeroPageController";
        r4 = "isSwitchingState restore";
        android.util.Log.d(r2, r4);
        r22.restoreOffset();
        r16 = 0;
        goto L_0x0027;
    L_0x0043:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 != 0) goto L_0x004f;
    L_0x0049:
        r2 = r22.hasMessages();
        if (r2 == 0) goto L_0x0069;
    L_0x004f:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getCurrentPage();
        if (r2 == 0) goto L_0x0069;
    L_0x0059:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getNextPage();
        if (r2 == 0) goto L_0x0069;
    L_0x0063:
        r22.restoreOffset();
        r16 = 0;
        goto L_0x0027;
    L_0x0069:
        r2 = r23.getAction();
        r10 = r2 & 255;
        r2 = r23.getRawX();
        r0 = (int) r2;
        r21 = r0;
        r13 = 0;
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 != 0) goto L_0x00a2;
    L_0x007d:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getCurrentPage();
        if (r2 == 0) goto L_0x0092;
    L_0x0087:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getCurrentPage();
        r4 = 1;
        if (r2 != r4) goto L_0x009c;
    L_0x0092:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getNextPage();
        if (r2 == 0) goto L_0x00a2;
    L_0x009c:
        r0 = r22;
        r2 = r0.mBezelSwipe;
        if (r2 == 0) goto L_0x013f;
    L_0x00a2:
        r22.acquireVelocityTrackerAndAddMovement(r23);
        r0 = r22;
        r2 = r0.mWorkspace;
        r4 = 0;
        r18 = r2.getScrollForPage(r4);
        r0 = r22;
        r2 = r0.mValueAnimator;
        if (r2 == 0) goto L_0x018d;
    L_0x00b4:
        r0 = r22;
        r2 = r0.mValueAnimator;
        r2 = r2.isRunning();
        if (r2 == 0) goto L_0x018d;
    L_0x00be:
        r11 = 1;
    L_0x00bf:
        if (r11 != 0) goto L_0x019c;
    L_0x00c1:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getCurrentPage();
        if (r2 != 0) goto L_0x019c;
    L_0x00cb:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0190;
    L_0x00cf:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getScrollX();
        r0 = r18;
        if (r2 >= r0) goto L_0x019c;
    L_0x00db:
        r14 = 1;
    L_0x00dc:
        if (r10 != 0) goto L_0x012a;
    L_0x00de:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 == 0) goto L_0x00e6;
    L_0x00e4:
        if (r11 == 0) goto L_0x012a;
    L_0x00e6:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 != 0) goto L_0x012a;
    L_0x00ec:
        if (r14 != 0) goto L_0x012a;
    L_0x00ee:
        r2 = isMoving();
        if (r2 == 0) goto L_0x012a;
    L_0x00f4:
        r17 = 0;
        r0 = r22;
        r2 = r0.mValueAnimator;
        if (r2 == 0) goto L_0x01a1;
    L_0x00fc:
        r0 = r22;
        r2 = r0.mValueAnimator;
        r2 = r2.isRunning();
        if (r2 == 0) goto L_0x01a1;
    L_0x0106:
        r0 = r22;
        r0 = r0.mPreValues;
        r19 = r0;
        r0 = r22;
        r2 = r0.mMovingState;
        r2 = r2 & 2;
        r4 = 2;
        if (r2 != r4) goto L_0x0117;
    L_0x0115:
        r17 = 1;
    L_0x0117:
        r22.cancelAnimation();
        r22.removeMsg();
        r0 = r19;
        r1 = r22;
        r1.mPreValues = r0;
        if (r17 == 0) goto L_0x019f;
    L_0x0125:
        r2 = 1;
    L_0x0126:
        r0 = r22;
        r0.mMovingState = r2;
    L_0x012a:
        r0 = r22;
        r2 = r0.mRealMetric;
        r0 = r2.widthPixels;
        r20 = r0;
        r0 = r22;
        r2 = r0.mLastDownX;
        r2 = r21 - r2;
        r12 = java.lang.Math.abs(r2);
        switch(r10) {
            case 0: goto L_0x01b7;
            case 1: goto L_0x035c;
            case 2: goto L_0x01cf;
            case 3: goto L_0x03a6;
            default: goto L_0x013f;
        };
    L_0x013f:
        r2 = 1;
        if (r10 == r2) goto L_0x0158;
    L_0x0142:
        r2 = 3;
        if (r10 == r2) goto L_0x0158;
    L_0x0145:
        r0 = r22;
        r2 = r0.mMovingState;
        r2 = r2 & 2;
        r4 = 2;
        if (r2 != r4) goto L_0x0158;
    L_0x014e:
        r0 = r22;
        r2 = r0.mMovingState;
        r2 = r2 & 8;
        r4 = 8;
        if (r2 != r4) goto L_0x0180;
    L_0x0158:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.isPaused();
        if (r2 == 0) goto L_0x0172;
    L_0x0162:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.getVisible();
        if (r2 != 0) goto L_0x0172;
    L_0x016c:
        r2 = 1;
        if (r10 == r2) goto L_0x0172;
    L_0x016f:
        r2 = 3;
        if (r10 != r2) goto L_0x0180;
    L_0x0172:
        if (r13 == 0) goto L_0x03b3;
    L_0x0174:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x03ab;
    L_0x0178:
        r0 = r22;
        r2 = r0.mDownX;
        r2 = r21 - r2;
        if (r2 >= 0) goto L_0x03b3;
    L_0x0180:
        r16 = 1;
    L_0x0182:
        if (r16 == 0) goto L_0x0027;
    L_0x0184:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2.resetTouchState();
        goto L_0x0027;
    L_0x018d:
        r11 = 0;
        goto L_0x00bf;
    L_0x0190:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getScrollX();
        r0 = r18;
        if (r2 > r0) goto L_0x00db;
    L_0x019c:
        r14 = 0;
        goto L_0x00dc;
    L_0x019f:
        r2 = 2;
        goto L_0x0126;
    L_0x01a1:
        r0 = r22;
        r2 = r0.mMovingState;
        r2 = r2 & 1;
        r4 = 1;
        if (r2 != r4) goto L_0x01ac;
    L_0x01aa:
        r17 = 1;
    L_0x01ac:
        if (r17 == 0) goto L_0x01b5;
    L_0x01ae:
        r2 = 2;
    L_0x01af:
        r0 = r22;
        r0.mMovingState = r2;
        goto L_0x012a;
    L_0x01b5:
        r2 = 1;
        goto L_0x01af;
    L_0x01b7:
        r2 = 1;
        r0 = r22;
        r0.mTouchDowned = r2;
        r0 = r21;
        r1 = r22;
        r1.mLastDownX = r0;
        r0 = r21;
        r1 = r22;
        r1.mDownX = r0;
        r2 = 0;
        r0 = r22;
        r0.mMovedToVirtualScreen = r2;
        goto L_0x013f;
    L_0x01cf:
        r0 = r22;
        r2 = r0.mTouchDowned;
        if (r2 != 0) goto L_0x01ea;
    L_0x01d5:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 != 0) goto L_0x01ea;
    L_0x01db:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 != 0) goto L_0x01ea;
    L_0x01e1:
        r2 = "ZeroPageController";
        r4 = "dispatchTouchEvent - skipped wrong touch move event on virtual screen";
        android.util.Log.d(r2, r4);
        goto L_0x013f;
    L_0x01ea:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 == 0) goto L_0x0283;
    L_0x01f0:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0250;
    L_0x01f4:
        r0 = r22;
        r2 = r0.mLastDownX;
        r0 = r21;
        if (r2 >= r0) goto L_0x01ff;
    L_0x01fc:
        r22.removeMsg();
    L_0x01ff:
        r0 = r21;
        r1 = r22;
        r1.mLastDownX = r0;
        r0 = r22;
        r2 = r0.mDownX;
        r3 = r21 - r2;
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0259;
    L_0x020f:
        if (r3 <= 0) goto L_0x025b;
    L_0x0211:
        r3 = 0;
    L_0x0212:
        r0 = r22;
        r2 = r0.mBezelSwipe;
        if (r2 != 0) goto L_0x0233;
    L_0x0218:
        if (r3 == 0) goto L_0x0233;
    L_0x021a:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0274;
    L_0x021e:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getScrollX();
        r0 = r18;
        if (r2 >= r0) goto L_0x0233;
    L_0x022a:
        r0 = r22;
        r2 = r0.mWorkspace;
        r0 = r18;
        r2.setScrollX(r0);
    L_0x0233:
        r4 = 0;
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 == 0) goto L_0x0281;
    L_0x023a:
        r5 = 1;
    L_0x023b:
        r6 = 0;
        r2 = r22;
        r2.setOffsetMsg(r3, r4, r5, r6);
        if (r3 != 0) goto L_0x013f;
    L_0x0244:
        r2 = 0;
        r0 = r22;
        r0.mMovedToVirtualScreen = r2;
        r2 = 0;
        r0 = r22;
        r0.mMovingState = r2;
        goto L_0x013f;
    L_0x0250:
        r0 = r22;
        r2 = r0.mLastDownX;
        r0 = r21;
        if (r2 <= r0) goto L_0x01ff;
    L_0x0258:
        goto L_0x01fc;
    L_0x0259:
        if (r3 < 0) goto L_0x0211;
    L_0x025b:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x026c;
    L_0x025f:
        r0 = r20;
        r2 = -r0;
        if (r3 >= r2) goto L_0x0212;
    L_0x0264:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0271;
    L_0x0268:
        r0 = r20;
        r3 = -r0;
    L_0x026b:
        goto L_0x0212;
    L_0x026c:
        r0 = r20;
        if (r3 <= r0) goto L_0x0212;
    L_0x0270:
        goto L_0x0264;
    L_0x0271:
        r3 = r20;
        goto L_0x026b;
    L_0x0274:
        r0 = r22;
        r2 = r0.mWorkspace;
        r2 = r2.getScrollX();
        r0 = r18;
        if (r2 <= r0) goto L_0x0233;
    L_0x0280:
        goto L_0x022a;
    L_0x0281:
        r5 = 0;
        goto L_0x023b;
    L_0x0283:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 != 0) goto L_0x02ad;
    L_0x0289:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x02a5;
    L_0x028d:
        r0 = r22;
        r2 = r0.mLastDownX;
        r0 = r21;
        if (r2 >= r0) goto L_0x02ad;
    L_0x0295:
        r0 = r22;
        r2 = r0.mDownX;
        r0 = r22;
        r0.mLastDownX = r2;
        r22.removeMsg();
        r22.resetOffset();
        goto L_0x013f;
    L_0x02a5:
        r0 = r22;
        r2 = r0.mLastDownX;
        r0 = r21;
        if (r2 > r0) goto L_0x0295;
    L_0x02ad:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 != 0) goto L_0x02c7;
    L_0x02b3:
        r0 = r22;
        r2 = r0.mTouchSlop;
        if (r12 < r2) goto L_0x013f;
    L_0x02b9:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.getHomeController();
        r2 = r2.isHorizontalScoll();
        if (r2 == 0) goto L_0x013f;
    L_0x02c7:
        r2 = 1;
        r0 = r22;
        r0.mMovedToVirtualScreen = r2;
        r0 = r21;
        r1 = r22;
        r1.mDownX = r0;
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 != 0) goto L_0x0336;
    L_0x02d8:
        r0 = r22;
        r2 = r0.mLauncher;
        r2 = r2.getHomeController();
        r2.updateBixbyHomeEnterCount();
        r2 = 0;
        r4 = 0;
        r0 = r22;
        r0.startActivityInVirtualScreen(r2, r4);
        r2 = com.android.launcher3.util.logging.SALogging.getInstance();
        r0 = r22;
        r4 = r0.mLauncher;
        r4 = r4.getResources();
        r5 = 2131296748; // 0x7f0901ec float:1.8211421E38 double:1.0530005043E-314;
        r4 = r4.getString(r5);
        r0 = r22;
        r5 = r0.mLauncher;
        r5 = r5.getResources();
        r6 = 2131296594; // 0x7f090152 float:1.821111E38 double:1.053000428E-314;
        r5 = r5.getString(r6);
        r6 = "Page Scroll";
        r2.insertEventLog(r4, r5, r6);
        r0 = r22;
        r2 = r0.mVirtualScreenHandler;
        if (r2 == 0) goto L_0x031e;
    L_0x0317:
        r0 = r22;
        r2 = r0.mVirtualScreenHandler;
        r2.initPreOffset();
    L_0x031e:
        r2 = com.android.launcher3.Utilities.sIsRtl;
        if (r2 == 0) goto L_0x0332;
    L_0x0322:
        r2 = -1;
    L_0x0323:
        r5 = 0;
        r0 = r22;
        r4 = r0.mMovingState;
        if (r4 == 0) goto L_0x0334;
    L_0x032a:
        r4 = 1;
    L_0x032b:
        r0 = r22;
        r0.setOffset(r2, r5, r4);
        goto L_0x013f;
    L_0x0332:
        r2 = 1;
        goto L_0x0323;
    L_0x0334:
        r4 = 0;
        goto L_0x032b;
    L_0x0336:
        r15 = r22.getOffset();
        if (r15 == 0) goto L_0x013f;
    L_0x033c:
        r0 = r22;
        r2 = r0.mDownX;
        r4 = r15.x;
        r2 = r2 - r4;
        r0 = r22;
        r0.mDownX = r2;
        r5 = r15.x;
        r6 = 0;
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 == 0) goto L_0x035a;
    L_0x0350:
        r7 = 1;
    L_0x0351:
        r8 = 0;
        r4 = r22;
        r4.setOffsetMsg(r5, r6, r7, r8);
        goto L_0x013f;
    L_0x035a:
        r7 = 0;
        goto L_0x0351;
    L_0x035c:
        r0 = r22;
        r2 = r0.mTouchDowned;
        if (r2 != 0) goto L_0x0377;
    L_0x0362:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 != 0) goto L_0x0377;
    L_0x0368:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 != 0) goto L_0x0377;
    L_0x036e:
        r2 = "ZeroPageController";
        r4 = "dispatchTouchEvent - skipped wrong touch up event on virtual screen";
        android.util.Log.d(r2, r4);
        goto L_0x013f;
    L_0x0377:
        r0 = r22;
        r2 = r0.mMovedToVirtualScreen;
        if (r2 == 0) goto L_0x0392;
    L_0x037d:
        r13 = 1;
        r0 = r22;
        r1 = r21;
        r0.moveToVirtualScreen(r1);
        r0 = r22;
        r2 = r0.mWorkspace;
        r4 = 0;
        r2.resetNormalPageAlphaValue(r4);
    L_0x038d:
        r22.resetTouchState();
        goto L_0x013f;
    L_0x0392:
        r0 = r22;
        r2 = r0.mMovingState;
        if (r2 == 0) goto L_0x03a0;
    L_0x0398:
        r0 = r22;
        r1 = r21;
        r0.moveToVirtualScreen(r1);
        goto L_0x038d;
    L_0x03a0:
        r2 = 0;
        r0 = r22;
        r0.mBezelSwipe = r2;
        goto L_0x038d;
    L_0x03a6:
        r22.restoreOffset();
        goto L_0x013f;
    L_0x03ab:
        r0 = r22;
        r2 = r0.mDownX;
        r2 = r21 - r2;
        if (r2 > 0) goto L_0x0180;
    L_0x03b3:
        r16 = 0;
        goto L_0x0182;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.ZeroPageController.dispatchTouchEvent(android.view.MotionEvent):boolean");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void moveToVirtualScreen(int r14) {
        /*
        r13 = this;
        r6 = 0;
        r12 = -750; // 0xfffffffffffffd12 float:NaN double:NaN;
        r11 = -1500; // 0xfffffffffffffa24 float:NaN double:NaN;
        r10 = 1051260355; // 0x3ea8f5c3 float:0.33 double:5.19391626E-315;
        r5 = 1;
        r7 = r13.mRealMetric;
        r4 = r7.widthPixels;
        r7 = r13.mVelocityTracker;
        r8 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r9 = r13.mMaximumVelocity;
        r9 = (float) r9;
        r7.computeCurrentVelocity(r8, r9);
        r7 = r13.mVelocityTracker;
        r7 = r7.getXVelocity();
        r3 = (int) r7;
        r7 = r13.mDownX;
        r1 = r14 - r7;
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x006b;
    L_0x0026:
        if (r1 <= 0) goto L_0x006d;
    L_0x0028:
        r1 = 0;
    L_0x0029:
        r0 = 750; // 0x2ee float:1.051E-42 double:3.705E-321;
        r7 = r13.mMovingState;
        if (r7 == 0) goto L_0x003b;
    L_0x002f:
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x007f;
    L_0x0033:
        if (r3 > r12) goto L_0x0083;
    L_0x0035:
        r7 = r13.mMovingState;
        r7 = r7 | 4;
        r13.mMovingState = r7;
    L_0x003b:
        r2 = 0;
        r7 = r13.mMovingState;
        if (r7 == 0) goto L_0x009b;
    L_0x0040:
        r6 = r13.mMovingState;
        r6 = r6 & 2;
        r7 = 2;
        if (r6 != r7) goto L_0x004e;
    L_0x0047:
        r6 = r13.mMovingState;
        r6 = r6 & 4;
        r7 = 4;
        if (r6 == r7) goto L_0x0064;
    L_0x004e:
        r6 = r13.mMovingState;
        r6 = r6 & 1;
        if (r6 != r5) goto L_0x005c;
    L_0x0054:
        r5 = r13.mMovingState;
        r5 = r5 & 8;
        r6 = 8;
        if (r5 == r6) goto L_0x0064;
    L_0x005c:
        r5 = r13.mMovingState;
        r5 = r5 & 22;
        r6 = 22;
        if (r5 != r6) goto L_0x0065;
    L_0x0064:
        r2 = 1;
    L_0x0065:
        r5 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r13.animatePage(r1, r5, r2);
        return;
    L_0x006b:
        if (r1 < 0) goto L_0x0028;
    L_0x006d:
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x007a;
    L_0x0071:
        r7 = -r4;
        if (r1 >= r7) goto L_0x0029;
    L_0x0074:
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x007d;
    L_0x0078:
        r1 = -r4;
    L_0x0079:
        goto L_0x0029;
    L_0x007a:
        if (r1 <= r4) goto L_0x0029;
    L_0x007c:
        goto L_0x0074;
    L_0x007d:
        r1 = r4;
        goto L_0x0079;
    L_0x007f:
        r7 = 750; // 0x2ee float:1.051E-42 double:3.705E-321;
        if (r3 >= r7) goto L_0x0035;
    L_0x0083:
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x0092;
    L_0x0087:
        r7 = 750; // 0x2ee float:1.051E-42 double:3.705E-321;
        if (r3 < r7) goto L_0x0094;
    L_0x008b:
        r7 = r13.mMovingState;
        r7 = r7 | 8;
        r13.mMovingState = r7;
        goto L_0x003b;
    L_0x0092:
        if (r3 <= r12) goto L_0x008b;
    L_0x0094:
        r7 = r13.mMovingState;
        r7 = r7 | 22;
        r13.mMovingState = r7;
        goto L_0x003b;
    L_0x009b:
        r7 = com.android.launcher3.Utilities.sIsRtl;
        if (r7 == 0) goto L_0x00b1;
    L_0x009f:
        r7 = 1500; // 0x5dc float:2.102E-42 double:7.41E-321;
        if (r3 >= r7) goto L_0x00af;
    L_0x00a3:
        if (r3 <= r11) goto L_0x00ad;
    L_0x00a5:
        r7 = -r1;
        r7 = (float) r7;
        r8 = (float) r4;
        r8 = r8 * r10;
        r7 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1));
        if (r7 < 0) goto L_0x00af;
    L_0x00ad:
        r2 = r5;
    L_0x00ae:
        goto L_0x0065;
    L_0x00af:
        r2 = r6;
        goto L_0x00ae;
    L_0x00b1:
        if (r3 <= r11) goto L_0x00c0;
    L_0x00b3:
        r7 = 1500; // 0x5dc float:2.102E-42 double:7.41E-321;
        if (r3 >= r7) goto L_0x00be;
    L_0x00b7:
        r7 = (float) r1;
        r8 = (float) r4;
        r8 = r8 * r10;
        r7 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1));
        if (r7 < 0) goto L_0x00c0;
    L_0x00be:
        r2 = r5;
    L_0x00bf:
        goto L_0x0065;
    L_0x00c0:
        r2 = r6;
        goto L_0x00bf;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.ZeroPageController.moveToVirtualScreen(int):void");
    }

    private void animatePage(int offset, int duration, final boolean pageChanged) {
        if (this.mValueAnimator == null) {
            this.mValueAnimator = new ValueAnimator();
        }
        if (!this.mValueAnimator.isRunning()) {
            int values;
            this.mValueAnimator.removeAllUpdateListeners();
            this.mValueAnimator.removeAllListeners();
            int widthPixels = this.mRealMetric.widthPixels;
            if (pageChanged) {
                if (this.mMovingState == 0) {
                    if (Utilities.sIsRtl) {
                        values = -widthPixels;
                    } else {
                        values = widthPixels;
                    }
                } else if ((this.mMovingState & 8) == 8) {
                    values = 0;
                } else if ((this.mMovingState & 22) == 22) {
                    values = this.mPreValues;
                } else {
                    values = Utilities.sIsRtl ? -widthPixels : widthPixels;
                }
            } else if (this.mMovingState == 0) {
                values = 0;
            } else if ((this.mMovingState & 1) == 1) {
                values = Utilities.sIsRtl ? -widthPixels : widthPixels;
            } else {
                values = 0;
            }
            this.mPreValues = values;
            if (offset == 0 || offset != values) {
                if (!this.mMovedToVirtualScreen && this.mMovingState == 0) {
                    startActivityInVirtualScreen(false, false);
                }
                this.mValueAnimator.setIntValues(new int[]{offset, values});
                this.mValueAnimator.setInterpolator(new ScrollInterpolator());
                ValueAnimator valueAnimator = this.mValueAnimator;
                long j = (offset == 0 && values == 0) ? 0 : (long) duration;
                valueAnimator.setDuration(j);
                this.mValueAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        reset();
                    }

                    private void reset() {
                        ZeroPageController.this.mBezelSwipe = false;
                        ZeroPageController.this.mMovingState = 0;
                        ZeroPageController.this.mPreValues = 0;
                        ZeroPageController.this.mWorkspace.pageEndMoving();
                        if (pageChanged) {
                            if (LauncherFeature.supportZeroPageHome()) {
                                Utilities.setZeroPageKey(ZeroPageController.this.mLauncher, true, ZeroPageProvider.START_FROM_ZEROPAGE);
                                ZeroPageProvider.notifyChange(ZeroPageController.this.mLauncher);
                            }
                            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ENTER_ZEROPAGE, null, -1, false);
                            GSIMLogging.getInstance().setZeroPageStartTime();
                        }
                    }
                });
                this.mInterval = System.currentTimeMillis();
                this.mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        boolean z;
                        Integer value = (Integer) animation.getAnimatedValue();
                        long currentTime = System.currentTimeMillis();
                        ZeroPageController zeroPageController = ZeroPageController.this;
                        int intValue = value.intValue();
                        if (ZeroPageController.this.mMovingState != 0) {
                            z = true;
                        } else {
                            z = false;
                        }
                        zeroPageController.setOffsetMsg(intValue, 0, z, currentTime - ZeroPageController.this.mInterval);
                        ZeroPageController.this.mInterval = currentTime;
                    }
                });
                this.mValueAnimator.start();
                return;
            }
            this.mBezelSwipe = false;
            this.mMovingState = 0;
        }
    }

    public void restoreOffset() {
        Log.d(TAG, "restoreOffset");
        cancelAnimation();
        removeMsg();
        resetTouchState();
        this.mBezelSwipe = false;
        this.mMovingState = 0;
        this.mPreValues = 0;
        resetOffset();
        if (LauncherFeature.supportZeroPageHome() && Utilities.getZeroPageKey(this.mLauncher, ZeroPageProvider.START_FROM_ZEROPAGE)) {
            Utilities.setZeroPageKey(this.mLauncher, false, ZeroPageProvider.START_FROM_ZEROPAGE);
            ZeroPageProvider.notifyChange(this.mLauncher);
        }
    }

    private void resetOffset() {
        this.mVirtualScreenHandler.post(new Runnable() {
            public void run() {
                ZeroPageController.this.setOffset(0, 0, true);
            }
        });
    }

    private void cancelAnimation() {
        if (this.mValueAnimator != null && this.mValueAnimator.isRunning()) {
            this.mValueAnimator.removeAllUpdateListeners();
            this.mValueAnimator.removeAllListeners();
            this.mValueAnimator.cancel();
        }
    }

    boolean isRunningAnimation() {
        return this.mValueAnimator != null && this.mValueAnimator.isRunning();
    }

    private void resetTouchState() {
        this.mTouchDowned = false;
        this.mMovedToVirtualScreen = false;
        releaseVelocityTracker();
        this.mInterval = 0;
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void removeMsg() {
        if (this.mVirtualScreenHandler != null) {
            this.mVirtualScreenHandler.removeMsg();
        }
    }

    public boolean hasMessages() {
        if (this.mVirtualScreenHandler == null || !this.mVirtualScreenHandler.hasMessages(1)) {
            return false;
        }
        return true;
    }

    boolean canScroll() {
        return (supportVirtualScreen() && isActiveZeroPage(this.mLauncher, false) && this.mMovedToVirtualScreen) ? false : true;
    }

    void createZeroPagePreview(boolean force) {
        int i = 0;
        if (sEnableZeroPage) {
            WorkspaceCellLayout zeroPageScreen = (WorkspaceCellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, this.mWorkspace, false);
            this.mWorkspace.setZeroPageMarker(true);
            this.mWorkspace.getWorkspaceScreens().put(-301, zeroPageScreen);
            this.mWorkspace.getScreenOrder().add(0, Long.valueOf(-301));
            this.mWorkspace.setMarkerStartOffset(0);
            if (!getZeroPageActiveState(this.mLauncher, false) || force) {
                this.mWorkspace.addMarkerForView(-1);
            }
            zeroPageScreen.setCellDimensions(-1, -1, 0, 0);
            zeroPageScreen.setGridSize(1, 1);
            zeroPageScreen.setPadding(0, 0, 0, 0);
            addZeroPageSwitch(zeroPageScreen);
            final Switch zeroPageSwitch = zeroPageScreen.getZeroPageSwitch();
            this.mZeroPageBgView = new LinearLayout(this.mLauncher);
            zeroPageScreen.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!ZeroPageController.this.mInstalled) {
                        ZeroPageController.this.showZeroPageDownloadDialog(zeroPageSwitch);
                    } else if (ZeroPageController.isActiveZeroPage(ZeroPageController.this.mLauncher, false)) {
                        ZeroPageController.this.startZeroPage();
                    }
                }
            });
            CellLayout.LayoutParams lp = getLayoutParamsForZeroPagePreview(this.mLauncher);
            this.mZeroPageBgView.setFocusable(true);
            this.mZeroPageBgView.setContentDescription(this.mAppName);
            zeroPageScreen.addViewToCellLayout(this.mZeroPageBgView, 0, -1, lp, true);
            loadZeroPagePreviewBitmap();
            this.mWorkspace.addView(zeroPageScreen, 0);
            this.mWorkspace.removeMarkerForView(0);
            this.mWorkspace.setDefaultPage(Math.min(this.mWorkspace.getPageCount() - 1, this.mWorkspace.getDefaultPage() + 1), false);
            updateZeroPageBg(getZeroPageActiveState(this.mLauncher, false), WhiteBgManager.isWhiteBg());
            zeroPageScreen.setCustomFlag(true);
            if (this.mWorkspace.getRestorePage() != -1001) {
                this.mWorkspace.setRestorePage(this.mWorkspace.getRestorePage() + 1);
            } else {
                Workspace workspace = this.mWorkspace;
                if (!this.mIsFromZeroPageSetting) {
                    i = this.mWorkspace.getCurrentPage() + 1;
                }
                workspace.setCurrentPage(i);
            }
            this.mWorkspace.updateDefaultHomePageIndicator(this.mWorkspace.getDefaultPage());
        }
    }

    private void addZeroPageSwitch(final WorkspaceCellLayout zeroPageScreen) {
        zeroPageScreen.addZeroPageSwitch(this.mAppName, getZeroPageActiveState(this.mLauncher, false), new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ZeroPageController.this.mInstalled || !isChecked) {
                    ZeroPageController.setZeroPageActiveState(ZeroPageController.this.mLauncher, isChecked);
                    ZeroPageController.this.updateZeroPageBg(isChecked, WhiteBgManager.isWhiteBg());
                    if (zeroPageScreen.getZeroPageSwitchLayout().getVisibility() == 0) {
                        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_OPTION, GSIMLogging.HOME_EDIT_OPTION_ZEROPAGE, -1, false);
                    }
                    if (isChecked) {
                        ZeroPageController.this.startActivityInVirtualScreen(true, false);
                        ZeroPageController.this.bindVirtualScreen();
                        return;
                    }
                    try {
                        if (ZeroPageController.this.mWorkspace.getScreenIdForPageIndex(ZeroPageController.this.mWorkspace.getDefaultPage()) == -301) {
                            ZeroPageController.this.mWorkspace.updateDefaultHome(ZeroPageController.this.mWorkspace.getDefaultPage(), ZeroPageController.this.mWorkspace.getDefaultPage() + 1);
                        }
                        ((ActivityManager) ZeroPageController.this.mLauncher.getSystemService("activity")).semForceStopPackage(ZeroPageController.this.getZeroPagePackageName());
                        return;
                    } catch (Exception e) {
                        Log.e(ZeroPageController.TAG, "forceStopPackage exception for zero page - onCheckedChanged");
                        return;
                    }
                }
                zeroPageScreen.getZeroPageSwitch().setChecked(false);
                ZeroPageController.this.showZeroPageDownloadDialog(zeroPageScreen.getZeroPageSwitch());
            }
        });
        this.mWorkspace.setAlphaWithVisibility(zeroPageScreen.getZeroPageSwitchLayout(), 8, false);
    }

    void removeZeroPagePreview(boolean force) {
        if (sEnableZeroPage) {
            if (this.mZeroPageBgView != null) {
                this.mZeroPageBgView.setBackground(null);
                this.mZeroPageBgView = null;
            }
            CellLayout zeroPageScreen = this.mWorkspace.getScreenWithId(-301);
            if (zeroPageScreen == null) {
                Log.e(TAG, "removeZeroPageContentPage - Expected custom zero page to exist");
                return;
            }
            int i;
            Workspace workspace = this.mWorkspace;
            boolean z = getZeroPageActiveState(this.mLauncher, false) && !force;
            workspace.setZeroPageMarker(z);
            this.mWorkspace.getWorkspaceScreens().remove(-301);
            this.mWorkspace.getScreenOrder().remove(Long.valueOf(-301));
            this.mWorkspace.removeView(zeroPageScreen);
            cancelZeroPagePreviewTask();
            workspace = this.mWorkspace;
            if (getZeroPageActiveState(this.mLauncher, false)) {
                i = -1;
            } else {
                i = 0;
            }
            workspace.setDefaultPage(Math.max(i, this.mWorkspace.getDefaultPage() - 1), false);
            if (getZeroPageActiveState(this.mLauncher, false) && !force) {
                this.mWorkspace.addMarkerForView(-1);
                this.mWorkspace.setMarkerStartOffset(1);
            }
            if (this.mWorkspace.getRestorePage() != -1001) {
                this.mWorkspace.setRestorePage(this.mWorkspace.getRestorePage() - 1);
            } else {
                this.mWorkspace.setCurrentPage(this.mWorkspace.getNextPage() - 1);
            }
            this.mAppPreview.removePreview();
            if (!force) {
                this.mWorkspace.updateDefaultHomePageIndicator(this.mWorkspace.getDefaultPage());
            }
        }
    }

    private void updateZeroPageBg(boolean isChecked, boolean whiteBg) {
        float f = 1.0f;
        CellLayout cl = this.mWorkspace.getScreenWithId(-301);
        if (cl != null) {
            changeColorForBg(whiteBg);
            cl.setBackgroundAlpha(isChecked ? 1.0f : 0.4f);
        }
        if (this.mZeroPageBgView != null) {
            LinearLayout linearLayout = this.mZeroPageBgView;
            if (!isChecked) {
                f = 0.4f;
            }
            linearLayout.setAlpha(f);
        }
    }

    void changeColorForBg(boolean whiteBg) {
        WorkspaceCellLayout cl = (WorkspaceCellLayout) this.mWorkspace.getScreenWithId(-301);
        if (cl != null) {
            cl.setBgImageResource(this.mLauncher.getHomeController().getState(), this.mWorkspace.getDefaultPage() == 0, whiteBg);
        }
    }

    void switchToZeroPage() {
        animatePage(0, 300, true);
    }

    public void enterZeroPageSetting() {
        this.mIsFromZeroPageSetting = true;
        this.mLauncher.getHomeController().enterOverviewState(false);
        this.mWorkspace.setAlphaWithVisibility(this.mWorkspace.getZeroPageSwitchLayout(), 0, true);
        if (!LauncherFeature.supportZeroPageHome()) {
            this.mWorkspace.hideDefaultHomeIcon();
        }
        this.mIsFromZeroPageSetting = false;
    }

    public void startZeroPage() {
        RuntimeException e;
        Log.d(TAG, "launch zeropage Activity.");
        try {
            int displayId = sVirtualScreenManager.getDisplayIdByPackage(this.mLauncher.getPackageName());
            if (displayId > -1) {
                Intent intent = new Intent();
                intent.setClassName(getZeroPagePackageName(), getZeroPageClassName());
                intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                LaunchParams params = new LaunchParams();
                params.flags |= LaunchParams.FLAG_BASE_ACTIVITY;
                params.displayId = 0;
                params.baseDisplayId = displayId;
                intent = sVirtualScreenManager.updateMultiScreenLaunchParams(intent, params);
                intent.putExtra("fromHome", true);
                sVirtualScreenManager.startActivity(intent, null, params);
                bindVirtualScreen();
                this.mLauncher.getHomeController().enterNormalState(false, true);
                if (LauncherFeature.supportZeroPageHome()) {
                    Utilities.setZeroPageKey(this.mLauncher, true, ZeroPageProvider.START_FROM_ZEROPAGE);
                    ZeroPageProvider.notifyChange(this.mLauncher);
                }
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_ENTER_ZEROPAGE, null, -1, false);
                GSIMLogging.getInstance().setZeroPageStartTime();
            }
        } catch (ActivityNotFoundException e2) {
            e = e2;
            Log.e(TAG, "startZeroPage:" + e.toString());
        } catch (NullPointerException e3) {
            e = e3;
            Log.e(TAG, "startZeroPage:" + e.toString());
        }
    }

    public boolean isCurrentZeroPage() {
        return isActiveZeroPage(this.mLauncher, false) && this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getCurrentPage()) == -301;
    }

    public void changeZeroPage(ComponentName componentName) {
        sZeroPageCompName = componentName;
        updateZeroPageAppMetadata(componentName);
        unBindVirtualScreen();
        startActivityInVirtualScreen(true, true);
        bindVirtualScreen();
    }

    void updateZeroPage(int op) {
        if (this.mLauncher != null) {
            boolean active = getZeroPageActiveState(this.mLauncher, false);
            if (op == 3) {
                active = false;
                this.mInstalled = false;
            } else if (op == 1) {
                active = true;
                this.mInstalled = true;
            }
            updateZeroPageAppMetadata(sZeroPageCompName);
            setZeroPageActiveState(this.mLauncher, active);
            CellLayout zeroPageScreen = this.mWorkspace.getScreenWithId(-301);
            if (zeroPageScreen instanceof WorkspaceCellLayout) {
                ((WorkspaceCellLayout) zeroPageScreen).getZeroPageSwitch().setChecked(active);
                loadZeroPagePreviewBitmap();
            } else if (active) {
                startActivityInVirtualScreen(false, true);
                bindVirtualScreen();
            }
        }
    }

    private String getZeroPageTitle(String packageName) {
        if (Utilities.TOUTIAO_NEWS_PACKAGE_NAME.equals(packageName)) {
            return this.mLauncher.getResources().getString(R.string.zeropage_toutiao_title);
        }
        if (Utilities.DAYLITE_PACKAGE_NAME.equals(packageName)) {
            return this.mLauncher.getResources().getString(R.string.zeropage_hellobixby_title);
        }
        if (Utilities.FLIPBOARD_BRIEFING_PACKAGE_NAME.equals(packageName)) {
            return this.mLauncher.getResources().getString(R.string.zeropage_briefing_title);
        }
        return this.mLauncher.getResources().getString(R.string.zeropage_sohu_title);
    }

    private int getZeroPagePreviewId(String packageName) {
        if (Utilities.DAYLITE_PACKAGE_NAME.equals(packageName)) {
            return R.drawable.daylite;
        }
        if (Utilities.FLIPBOARD_BRIEFING_PACKAGE_NAME.equals(packageName)) {
            return R.drawable.briefing;
        }
        if (Utilities.TOUTIAO_NEWS_PACKAGE_NAME.equals(packageName)) {
            return R.drawable.toutiao_preview;
        }
        return R.drawable.sohu_news;
    }

    private void showZeroPageDownloadDialog(final Switch zeroPageSwitch) {
        Builder builder = new Builder(this.mLauncher);
        final String packageName = sZeroPageCompName.getPackageName();
        String zeropage = getZeroPageTitle(packageName);
        builder.setMessage(this.mLauncher.getResources().getString(R.string.zeropage_download_msg, new Object[]{zeropage, zeropage}));
        builder.setPositiveButton(this.mLauncher.getResources().getString(R.string.zeropage_download), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ZeroPageController.this.startDownloadZeroPage(packageName);
            }
        });
        builder.setNegativeButton(this.mLauncher.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                zeroPageSwitch.setChecked(false);
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                zeroPageSwitch.setChecked(false);
            }
        });
        builder.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                ZeroPageController.this.mZeropageDownloadDialog = null;
            }
        });
        this.mZeropageDownloadDialog = builder.create();
        if (this.mZeropageDownloadDialog != null) {
            this.mZeropageDownloadDialog.setCanceledOnTouchOutside(false);
            if (!this.mZeropageDownloadDialog.isShowing()) {
                this.mZeropageDownloadDialog.show();
            }
        }
    }

    public void closeZeroPageDownloadDialog() {
        if (this.mZeropageDownloadDialog != null) {
            this.mZeropageDownloadDialog.dismiss();
            this.mZeropageDownloadDialog = null;
        }
    }

    private void startDownloadZeroPage(String packageName) {
        Intent intent = new Intent();
        if (packageName != null) {
            intent.setData(Uri.parse("samsungapps://ProductDetail/" + packageName));
            intent.putExtra("type", "cover");
            intent.addFlags(335544352);
            this.mLauncher.startActivity(intent);
        }
    }

    String getAppName() {
        return this.mAppName;
    }

    void onZeroPageActiveChanged(final boolean active) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                if (ZeroPageController.this.mWorkspace == null) {
                    ZeroPageController.setZeroPageActiveState(LauncherAppState.getInstance().getContext(), active);
                    return;
                }
                CellLayout zeroPageScreen = ZeroPageController.this.mWorkspace.getScreenWithId(-301);
                if (zeroPageScreen instanceof WorkspaceCellLayout) {
                    ((WorkspaceCellLayout) zeroPageScreen).getZeroPageSwitch().setChecked(active);
                    return;
                }
                ZeroPageController.setZeroPageActiveState(ZeroPageController.this.mLauncher, active);
                ZeroPageController.this.mWorkspace.updatePageIndicatorForZeroPage(active, false);
            }
        });
    }

    private DisplayMetrics getRealMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mLauncher.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    public static CellLayout.LayoutParams getLayoutParamsForZeroPagePreview(Context context) {
        Resources res = context.getResources();
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(0, 0, 1, 1);
        int zeroPageSwitchHeight = (int) (((float) res.getDimensionPixelSize(R.dimen.overview_zeropage_switch_height)) * (100.0f / ((float) res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage))));
        int margin = res.getDimensionPixelSize(R.dimen.zero_page_bg_margin);
        lp.setMargins(margin, margin + zeroPageSwitchHeight, margin, margin);
        return lp;
    }

    public void onConfigurationChangedIfNeeded() {
        this.mRealMetric = getRealMetrics();
        if (this.mLauncher.getStageManager().getTopStage().getMode() == 1 && this.mWorkspace.isOverviewState()) {
            changePreviewImage();
        }
    }

    private void updatePreviewSize() {
        this.mZeroPageBitmapWidth = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.zero_page_single_preview_width);
        this.mZeroPageBitmapHeight = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.zero_page_single_preview_height);
    }
}
