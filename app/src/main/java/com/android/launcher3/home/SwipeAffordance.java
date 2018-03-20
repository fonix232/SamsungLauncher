package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.sec.android.app.launcher.R;

public class SwipeAffordance extends FrameLayout {
    public static final String KEY_APPS_VISIT_COUNT = "KEY_APPS_VISIT_COUNT_BY_SWIPE";
    public static final String KEY_EXIT_TIME_STAMP = "KEY_AFFORDANCE_EXIT_TIME_STAMP";
    public static final int MAX_APPS_VISIT_COUNT = 25;
    public static final String TAG = "SwipeAffordance";
    private final int ANIM_START_DELAY;
    private final int CANCEL_ANIM_DURATION;
    private final int GAP_OF_ARROW_START_ANIM;
    private final float SHOW_ANIM_ALPHA;
    private final int TEXT_HIDE_ANIM_DURATION;
    private final int TEXT_HIDE_ANIM_START_DELAY;
    private final int TEXT_SHOW_ANIM_DURATION;
    private AnimatorListenerAdapter mAnimListenerAdapter;
    private View mArrowFrameView;
    private ImageView mArrowView1;
    private ImageView mArrowView2;
    private AnimatorSet mCancelAnim;
    private AnimatorListenerAdapter mCancelAnimListenerAdapter;
    private AnimatorSet mCancelWithIndicatorAnim;
    private long mExitTime;
    private boolean mIsStartedAnim;
    private Launcher mLauncher;
    private AnimatorSet mOneTimeAnim;
    private float mTextReduceSize;
    private float mTextSize;
    private float mTextTranslateY;
    private TextView mTextView;
    private int mTextViewMaxWidth;
    private AnimatorSet mThreeTimesAnim;
    private int mVisitCountToApps;

    public SwipeAffordance(Context context) {
        this(context, null);
    }

    public SwipeAffordance(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeAffordance(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.ANIM_START_DELAY = CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT;
        this.TEXT_SHOW_ANIM_DURATION = 667;
        this.TEXT_HIDE_ANIM_DURATION = CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT;
        this.CANCEL_ANIM_DURATION = CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT;
        this.TEXT_HIDE_ANIM_START_DELAY = 2000;
        this.GAP_OF_ARROW_START_ANIM = 200;
        this.SHOW_ANIM_ALPHA = 0.8f;
        this.mVisitCountToApps = 0;
        this.mIsStartedAnim = false;
        this.mAnimListenerAdapter = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                SwipeAffordance.this.mTextView.setLayerType(2, null);
                SwipeAffordance.this.mArrowView1.setLayerType(2, null);
                SwipeAffordance.this.mArrowView2.setLayerType(2, null);
            }

            public void onAnimationEnd(Animator animation) {
                SwipeAffordance.this.mTextView.setLayerType(0, null);
                SwipeAffordance.this.mArrowView1.setLayerType(0, null);
                SwipeAffordance.this.mArrowView2.setLayerType(0, null);
                SwipeAffordance.this.mIsStartedAnim = false;
            }
        };
        this.mCancelAnimListenerAdapter = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                SwipeAffordance.this.setLayerType(2, null);
            }

            public void onAnimationEnd(Animator animation) {
                SwipeAffordance.this.setLayerType(0, null);
                SwipeAffordance.this.endAnimators();
                SwipeAffordance.this.mIsStartedAnim = false;
            }
        };
        Resources res = context.getResources();
        this.mTextSize = res.getDimension(R.dimen.home_swipe_affordance_text_size);
        this.mTextTranslateY = res.getDimension(R.dimen.home_swipe_affordance_anim_text_translate_y);
        this.mTextReduceSize = res.getDimension(R.dimen.home_swipe_affordance_text_reduce_size);
        this.mTextViewMaxWidth = (LauncherAppState.getInstance().getDeviceProfile().availableWidthPx - res.getDimensionPixelSize(R.dimen.home_swipe_affordance_horizontal_margin_size)) - res.getDimensionPixelSize(R.dimen.home_swipe_affordance_horizontal_extra_margin_size);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTextView = (TextView) findViewById(R.id.swipe_affordance_text);
        this.mArrowFrameView = findViewById(R.id.swipe_affordance_arrow_frame);
        this.mArrowView1 = (ImageView) findViewById(R.id.swipe_affordance_arrow1);
        this.mArrowView2 = (ImageView) findViewById(R.id.swipe_affordance_arrow2);
        changeColorForBg(WhiteBgManager.isWhiteBg());
        setSuitableTextSize();
    }

    private void setSuitableTextSize() {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(this.mTextSize);
        paint.setTypeface(this.mTextView.getTypeface());
        paint.getTextBounds(this.mTextView.getText().toString(), 0, this.mTextView.getText().length(), bounds);
        while (bounds.width() > this.mTextViewMaxWidth) {
            this.mTextSize -= this.mTextReduceSize;
            paint.setTextSize(this.mTextSize);
            paint.getTextBounds(this.mTextView.getText().toString(), 0, this.mTextView.getText().length(), bounds);
        }
        this.mTextView.setTextSize(0, this.mTextSize);
        Log.d(TAG, "set swipeAffordance TextSize : " + this.mTextSize);
    }

    void setup(Launcher launcher, final PageIndicator pageIndicator) {
        this.mLauncher = launcher;
        Runnable indicatorVisibility = new Runnable() {
            public void run() {
                if (pageIndicator != null && SwipeAffordance.this.mLauncher != null) {
                    pageIndicator.setVisibility(SwipeAffordance.this.mLauncher.isHomeStage() ? 0 : 4);
                }
            }
        };
        this.mThreeTimesAnim = new AnimatorSet();
        this.mThreeTimesAnim.playSequentially(new Animator[]{pageIndicator.getPageIndicatorAnimatorSet(false, indicatorVisibility), getArrowAnimSet(), getTextShowHideAnimSet(), getArrowAnimSet(), getTextShowHideAnimSet(), getArrowAnimSet(), getTextShowHideAnimSet(), pageIndicator.getPageIndicatorAnimatorSet(true, indicatorVisibility)});
        this.mThreeTimesAnim.setStartDelay(300);
        this.mThreeTimesAnim.addListener(this.mAnimListenerAdapter);
        this.mOneTimeAnim = new AnimatorSet();
        this.mOneTimeAnim.playSequentially(new Animator[]{pageIndicator.getPageIndicatorAnimatorSet(false, indicatorVisibility), getArrowAnimSet(), getTextShowHideAnimSet(), pageIndicator.getPageIndicatorAnimatorSet(true, indicatorVisibility)});
        this.mOneTimeAnim.setStartDelay(300);
        this.mOneTimeAnim.addListener(this.mAnimListenerAdapter);
        this.mCancelWithIndicatorAnim = new AnimatorSet();
        this.mCancelWithIndicatorAnim.playSequentially(new Animator[]{getCancelAnimator(), pageIndicator.getPageIndicatorAnimatorSet(true, indicatorVisibility)});
        this.mCancelWithIndicatorAnim.addListener(this.mCancelAnimListenerAdapter);
        this.mCancelAnim = new AnimatorSet();
        this.mCancelAnim.playSequentially(new Animator[]{getCancelAnimator()});
        this.mCancelAnim.addListener(this.mCancelAnimListenerAdapter);
        setCountForSwipe();
        setExitTime();
    }

    void startAnim() {
        if (!isUnderVisitCount() || !isAffordanceTime() || LauncherAppState.getInstance().getAppsButtonEnabled()) {
            return;
        }
        if (Global.getFloat(this.mLauncher.getContentResolver(), "animator_duration_scale", 1.0f) == 0.0f) {
            Log.d(TAG, "SwipeAffordance will not show due to value of ANIMATOR_DURATION_SCALE");
            return;
        }
        endAnimators();
        setAnimatingString();
        this.mTextView.setVisibility(View.VISIBLE);
        this.mArrowView1.setVisibility(View.VISIBLE);
        this.mArrowView2.setVisibility(View.VISIBLE);
        setAlpha(1.0f);
        this.mTextView.setAlpha(0.0f);
        this.mArrowView1.setAlpha(0.0f);
        this.mArrowView2.setAlpha(0.0f);
        this.mIsStartedAnim = true;
        if (this.mVisitCountToApps < 0) {
            appsVisitCountUp();
            this.mThreeTimesAnim.start();
            return;
        }
        this.mOneTimeAnim.start();
    }

    void startCancelAnim(boolean needToShowIndicator) {
        if (needToShowIndicator) {
            this.mCancelWithIndicatorAnim.start();
        } else {
            this.mCancelAnim.start();
        }
    }

    private void endAnimators() {
        if (this.mOneTimeAnim.isStarted()) {
            this.mOneTimeAnim.end();
        }
        if (this.mThreeTimesAnim.isStarted()) {
            this.mThreeTimesAnim.end();
        }
    }

    private AnimatorSet getTextShowHideAnimSet() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator showAnim = getShowObjectAnimator();
        ObjectAnimator hideAnim = getHideObjectAnimator();
        showAnim.setDuration(667);
        hideAnim.setDuration(300);
        showAnim.setInterpolator(ViInterpolator.getInterploator(34));
        hideAnim.setInterpolator(ViInterpolator.getInterploator(30));
        hideAnim.setStartDelay(2000);
        animatorSet.playSequentially(new Animator[]{showAnim, hideAnim});
        return animatorSet;
    }

    private AnimatorSet getArrowAnimSet() {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator arrowAnim = AnimatorInflater.loadAnimator(getContext(), R.animator.swipe_affordance_arrow_y_translate);
        Animator arrowAnim1 = AnimatorInflater.loadAnimator(getContext(), R.animator.swipe_affordance_arrow1_alpha);
        Animator arrowAnim2 = AnimatorInflater.loadAnimator(getContext(), R.animator.swipe_affordance_arrow2_alpha);
        arrowAnim.setTarget(this.mArrowFrameView);
        arrowAnim.setInterpolator(ViInterpolator.getInterploator(34));
        arrowAnim1.setTarget(this.mArrowView1);
        arrowAnim2.setTarget(this.mArrowView2);
        arrowAnim2.setStartDelay(200);
        animatorSet.playTogether(new Animator[]{arrowAnim, arrowAnim1, arrowAnim2});
        return animatorSet;
    }

    private ObjectAnimator getShowObjectAnimator() {
        PropertyValuesHolder pvhShowAlpha = PropertyValuesHolder.ofFloat(View.ALPHA.getName(), new float[]{0.0f, 0.8f});
        PropertyValuesHolder pvhShowTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y.getName(), new float[]{this.mTextTranslateY, 0.0f});
        return ObjectAnimator.ofPropertyValuesHolder(this.mTextView, new PropertyValuesHolder[]{pvhShowAlpha, pvhShowTransY});
    }

    private ObjectAnimator getHideObjectAnimator() {
        PropertyValuesHolder pvhHideAlpha = PropertyValuesHolder.ofFloat(View.ALPHA.getName(), new float[]{0.8f, 0.0f});
        return ObjectAnimator.ofPropertyValuesHolder(this.mTextView, new PropertyValuesHolder[]{pvhHideAlpha});
    }

    private ObjectAnimator getCancelAnimator() {
        PropertyValuesHolder pvhHideAlpha = PropertyValuesHolder.ofFloat(View.ALPHA.getName(), new float[]{0.0f});
        return ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{pvhHideAlpha}).setDuration(300);
    }

    void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeTextViewColorForBg(getContext(), this.mTextView, whiteBg, true);
    }

    boolean isStartedAnim() {
        return this.mIsStartedAnim;
    }

    private boolean isUnderVisitCount() {
        return this.mVisitCountToApps < 25;
    }

    private boolean isAffordanceTime() {
        return this.mExitTime > System.currentTimeMillis();
    }

    void appsVisitCountUp() {
        if (isUnderVisitCount()) {
            this.mVisitCountToApps++;
            Editor editor = this.mLauncher.getSharedPrefs().edit();
            editor.putInt(KEY_APPS_VISIT_COUNT, this.mVisitCountToApps);
            editor.apply();
        }
    }

    private void setAnimatingString() {
        String animatingString;
        Resources res = this.mLauncher.getResources();
        if (LauncherAppState.getInstance().getNotificationPanelExpansionEnabled()) {
            if (Utilities.isTalkBackEnabled(getContext())) {
                animatingString = res.getString(R.string.swipe_up_for_all_apps_tts);
            } else {
                animatingString = res.getString(LauncherFeature.isATTModel() ? R.string.swipe_up_for_all_apps_att : R.string.swipe_up_for_all_apps);
            }
        } else if (Utilities.isTalkBackEnabled(getContext())) {
            animatingString = res.getString(R.string.swipe_up_for_more_apps_with_two_fingers);
        } else {
            animatingString = res.getString(LauncherFeature.isATTModel() ? R.string.swipe_up_for_more_apps_att : R.string.swipe_up_for_more_apps);
        }
        if (!this.mTextView.getText().equals(animatingString)) {
            this.mTextView.setText(animatingString);
            this.mTextView.setContentDescription(animatingString);
            setSuitableTextSize();
        }
    }

    private void setCountForSwipe() {
        this.mVisitCountToApps = this.mLauncher.getSharedPrefs().getInt(KEY_APPS_VISIT_COUNT, -1);
        Log.d(TAG, "Create and show swipe affordance : " + this.mVisitCountToApps);
    }

    private void setExitTime() {
        long exitTime = this.mLauncher.getSharedPrefs().getLong(KEY_EXIT_TIME_STAMP, -1);
        if (exitTime < 0) {
            this.mExitTime = System.currentTimeMillis() + 172800000;
            Editor editor = this.mLauncher.getSharedPrefs().edit();
            editor.putLong(KEY_EXIT_TIME_STAMP, this.mExitTime);
            editor.apply();
        } else {
            this.mExitTime = exitTime;
        }
        Log.d(TAG, "Set SwipeAffordance exit time : " + this.mExitTime);
    }

    public static boolean needToCreateAffordance(Launcher launcher) {
        long currentTimeMillis = System.currentTimeMillis();
        return launcher.getSharedPrefs().getInt(KEY_APPS_VISIT_COUNT, -1) < 25 && launcher.getSharedPrefs().getLong(KEY_EXIT_TIME_STAMP, currentTimeMillis) >= currentTimeMillis;
    }

    public void onConfigurationChangedIfNeeded() {
        this.mTextSize = (float) getResources().getDimensionPixelSize(R.dimen.home_swipe_affordance_text_size);
        setSuitableTextSize();
    }
}
