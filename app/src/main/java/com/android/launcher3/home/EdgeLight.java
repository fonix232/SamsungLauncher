package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.launcher3.util.ViInterpolator;
import com.sec.android.app.launcher.R;

public class EdgeLight extends FrameLayout {
    private static final int ARROW_ANIM_START_DELAY = 200;
    private static final int ARROW_ANIM_TOTAL_DELAY = 600;
    private static final int MAX_BIXBY_HOME_ENTER_COUNT_FOR_ALLOW = 3;
    private static final String PREF_KEY_BIXBY_HOME_ENTER_COUNT = "BixbyHomeEnterCountForEdgeLight";
    private static final String SPAGE_AUTHORITY = "com.samsung.android.app.spage";
    private static final String SPAGE_NOTIFICATION = "notification";
    private static final Uri SPAGE_NOTIFICATION_URI = Uri.parse("content://com.samsung.android.app.spage/notification");
    private static final String SPAGE_SHOW_NOTIFICATION = "show_notification";
    public static final String TAG = "EdgeLight";
    private AnimatorListenerAdapter mArrowAnimListenerAdapter;
    private AnimatorSet mArrowAnimSet;
    private View mArrowFrameView;
    private ImageView mArrowView1;
    private ImageView mArrowView2;
    private int mBixbyHomeEnterCount;
    private Context mContext;
    private AnimationDrawable mEdgeLightAnimator;
    private ImageView mEdgeLightImage;
    private HomeController mHomeController;
    private boolean mNotiExist;
    private SpageNotificationObserver mSpageNotificationObserver;

    private class SpageNotificationObserver extends ContentObserver {
        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onChange(boolean r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0068 in list [B:13:0x005f]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
            /*
            r9 = this;
            r8 = 1;
            r6 = 0;
            r0 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = r0.mContext;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = r0.getContentResolver();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = com.android.launcher3.home.EdgeLight.SPAGE_NOTIFICATION_URI;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = 0;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r3 = 0;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r4 = 0;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r5 = 0;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r6 = r0.query(r1, r2, r3, r4, r5);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            if (r6 == 0) goto L_0x005d;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x001a:
            r0 = r6.moveToFirst();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            if (r0 == 0) goto L_0x005d;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x0020:
            r1 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = "show_notification";	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = r6.getInt(r0);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            if (r0 != r8) goto L_0x0069;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x002e:
            r0 = r8;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x002f:
            r1.mNotiExist = r0;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = "EdgeLight";	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1.<init>();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = "SPage Notification onChange : ";	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.append(r2);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = r2.mNotiExist;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.append(r2);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.toString();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            android.util.Log.d(r0, r1);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0 = r0.mNotiExist;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            if (r0 == 0) goto L_0x006b;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x0058:
            r0 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0.startEdgeLight();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
        L_0x005d:
            if (r6 == 0) goto L_0x0068;
        L_0x005f:
            r0 = r6.isClosed();
            if (r0 != 0) goto L_0x0068;
        L_0x0065:
            r6.close();
        L_0x0068:
            return;
        L_0x0069:
            r0 = 0;
            goto L_0x002f;
        L_0x006b:
            r0 = com.android.launcher3.home.EdgeLight.this;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r0.stopEdgeLight();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            goto L_0x005d;
        L_0x0071:
            r7 = move-exception;
            r0 = "EdgeLight";	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1.<init>();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = "SPage Notification observing error : ";	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.append(r2);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r2 = r7.getMessage();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.append(r2);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            r1 = r1.toString();	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x0071, all -> 0x009a }
            if (r6 == 0) goto L_0x0068;
        L_0x0090:
            r0 = r6.isClosed();
            if (r0 != 0) goto L_0x0068;
        L_0x0096:
            r6.close();
            goto L_0x0068;
        L_0x009a:
            r0 = move-exception;
            if (r6 == 0) goto L_0x00a6;
        L_0x009d:
            r1 = r6.isClosed();
            if (r1 != 0) goto L_0x00a6;
        L_0x00a3:
            r6.close();
        L_0x00a6:
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.EdgeLight.SpageNotificationObserver.onChange(boolean):void");
        }

        public SpageNotificationObserver() {
            super(new Handler());
        }
    }

    public EdgeLight(Context context) {
        this(context, null);
    }

    public EdgeLight(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EdgeLight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mNotiExist = false;
        this.mSpageNotificationObserver = null;
        this.mBixbyHomeEnterCount = -1;
        this.mArrowAnimListenerAdapter = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                EdgeLight.this.mArrowFrameView.setLayerType(2, null);
            }

            public void onAnimationEnd(Animator animation) {
                EdgeLight.this.mArrowFrameView.setLayerType(0, null);
            }
        };
        this.mContext = context;
    }

    private void setupAinmator(boolean useArrow) {
        this.mEdgeLightImage = (ImageView) findViewById(R.id.edge_light_image);
        this.mArrowFrameView = findViewById(R.id.edge_light_arrow_frame);
        if (useArrow) {
            this.mArrowView1 = (ImageView) findViewById(R.id.edge_light_arrow1);
            this.mArrowView2 = (ImageView) findViewById(R.id.edge_light_arrow2);
            this.mArrowAnimSet = new AnimatorSet();
            this.mArrowAnimSet.playSequentially(new Animator[]{getArrowAnimSet(200), getArrowAnimSet(ARROW_ANIM_TOTAL_DELAY), getArrowAnimSet(ARROW_ANIM_TOTAL_DELAY)});
            this.mArrowAnimSet.addListener(this.mArrowAnimListenerAdapter);
            return;
        }
        removeView(this.mArrowFrameView);
    }

    public void startEdgeLight() {
        if (this.mNotiExist && this.mEdgeLightAnimator == null) {
            this.mEdgeLightAnimator = (AnimationDrawable) getResources().getDrawable(R.drawable.edge_light, this.mContext.getTheme());
            this.mEdgeLightImage.setBackground(this.mEdgeLightAnimator);
        }
        if (this.mNotiExist && this.mEdgeLightAnimator != null && !this.mEdgeLightAnimator.isRunning() && ZeroPageController.isActiveZeroPage(this.mContext, false) && this.mHomeController != null && this.mHomeController.checkEdgeLightDisplayAvailability()) {
            Log.d(TAG, "startEdgeLight");
            setVisibility(View.VISIBLE);
            this.mEdgeLightAnimator.start();
            if (this.mBixbyHomeEnterCount < 3) {
                this.mArrowAnimSet.start();
            }
        }
    }

    public void stopEdgeLight() {
        if (this.mEdgeLightAnimator != null && this.mEdgeLightAnimator.isRunning()) {
            Log.d(TAG, "stopEdgeLight");
            this.mEdgeLightAnimator.stop();
            if (this.mArrowAnimSet != null && this.mArrowAnimSet.isRunning()) {
                this.mArrowAnimSet.end();
            }
            setVisibility(View.GONE);
        }
        if (this.mEdgeLightAnimator != null) {
            this.mEdgeLightAnimator = null;
            this.mEdgeLightImage.setBackground(null);
        }
    }

    void registerContentObserver(HomeController homeController) {
        boolean z;
        if (this.mSpageNotificationObserver == null) {
            this.mSpageNotificationObserver = new SpageNotificationObserver();
        }
        this.mHomeController = homeController;
        this.mBixbyHomeEnterCount = this.mHomeController.getLauncher().getSharedPrefs().getInt(PREF_KEY_BIXBY_HOME_ENTER_COUNT, 0);
        if (this.mBixbyHomeEnterCount < 3) {
            z = true;
        } else {
            z = false;
        }
        setupAinmator(z);
        this.mContext.getContentResolver().registerContentObserver(SPAGE_NOTIFICATION_URI, false, this.mSpageNotificationObserver);
    }

    void unregisterContentObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSpageNotificationObserver);
    }

    private AnimatorSet getArrowAnimSet(int startDelay) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator arrowAnim = AnimatorInflater.loadAnimator(getContext(), R.animator.edge_light_arrow_x_translate);
        Animator arrowAnim1 = AnimatorInflater.loadAnimator(getContext(), R.animator.edge_light_arrow_alpha);
        Animator arrowAnim2 = AnimatorInflater.loadAnimator(getContext(), R.animator.edge_light_arrow_alpha);
        arrowAnim.setTarget(this.mArrowFrameView);
        arrowAnim.setInterpolator(ViInterpolator.getInterploator(34));
        arrowAnim1.setTarget(this.mArrowView1);
        arrowAnim2.setTarget(this.mArrowView2);
        arrowAnim2.setStartDelay(200);
        animatorSet.playTogether(new Animator[]{arrowAnim, arrowAnim1, arrowAnim2});
        animatorSet.setStartDelay((long) startDelay);
        return animatorSet;
    }

    void updateBixbyHomeEnterCount() {
        if (this.mHomeController.getLauncher() != null && this.mArrowAnimSet != null && this.mArrowAnimSet.isRunning()) {
            this.mBixbyHomeEnterCount++;
            Editor editor = this.mHomeController.getLauncher().getSharedPrefs().edit();
            editor.putInt(PREF_KEY_BIXBY_HOME_ENTER_COUNT, this.mBixbyHomeEnterCount);
            editor.apply();
            Log.d(TAG, "updateBixbyHomeEnterCount : " + this.mBixbyHomeEnterCount);
        }
    }
}
