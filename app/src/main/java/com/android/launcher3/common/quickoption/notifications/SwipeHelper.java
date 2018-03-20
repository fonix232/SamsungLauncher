package com.android.launcher3.common.quickoption.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import com.android.launcher3.util.ViInterpolator;
import com.sec.android.app.launcher.R;

class SwipeHelper {
    private static final boolean CHANGE_COLOR_DURING_SWIPE = true;
    private static final boolean CONSTRAIN_SWIPE = true;
    private static final int SNAP_ANIM_LEN = 150;
    public static final int X = 0;
    public static final int Y = 1;
    private final int DEFAULT_ESCAPE_ANIMATION_DURATION = 200;
    private final int MAX_DISMISS_VELOCITY = 4000;
    private final int MAX_ESCAPE_ANIMATION_DURATION = 400;
    private final float SWIPE_ESCAPE_VELOCITY = 100.0f;
    private final Callback mCallback;
    private final int mColorFrom;
    private final int mColorTo;
    private View mCurrView;
    private final float mDensityScale;
    private boolean mDragging;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private final Handler mHandler;
    private float mInitialTouchPos;
    private boolean mLongPressSent;
    private final NotificationItemView mNotificationView;
    private final float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final int mSwipeDirection;
    private float mTranslation = 0.0f;
    private final VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        void onChildDismissed(View view);
    }

    SwipeHelper(int swipeDirection, Callback callback, Context context, NotificationItemView mainView) {
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = context.getResources().getDisplayMetrics().density;
        this.mPagingTouchSlop = (float) ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, ((float) getMaxEscapeAnimDuration()) / 1000.0f);
        this.mColorFrom = ContextCompat.getColor(context, R.color.notification_color_beneath);
        this.mColorTo = ContextCompat.getColor(context, R.color.quick_options_popup_color);
        this.mNotificationView = mainView;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getPerpendicularPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getY() : ev.getX();
    }

    private float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        if (this.mSwipeDirection == 0) {
            return vt.getXVelocity();
        }
        return vt.getYVelocity();
    }

    private ObjectAnimator createTranslationAnimation(View v, float newPos) {
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y, new float[]{newPos});
    }

    private Animator getViewTranslationAnimator(View v, float target, AnimatorUpdateListener listener) {
        ObjectAnimator anim = createTranslationAnimation(v, target);
        if (listener != null) {
            anim.addUpdateListener(listener);
        }
        return anim;
    }

    private void setTranslation(View v, float translate) {
        if (v != null) {
            if (this.mSwipeDirection == 0) {
                v.setTranslationX(translate);
            } else {
                v.setTranslationY(translate);
            }
        }
    }

    private float getSize(View v) {
        if (this.mSwipeDirection == 0) {
            return (float) v.getMeasuredWidth();
        }
        return (float) v.getMeasuredHeight();
    }

    private void dismissChangeBackgroundColor(View animView, boolean dismissable) {
        if (dismissable) {
            ValueAnimator currentAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(this.mColorFrom), Integer.valueOf(this.mColorTo)});
            final GradientDrawable background = (GradientDrawable) this.mNotificationView.getBackground();
            currentAnimation.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animator) {
                    if (background != null) {
                        background.setTint(((Integer) animator.getAnimatedValue()).intValue());
                    }
                }
            });
            currentAnimation.setDuration(400);
            currentAnimation.setInterpolator(this.mSineInOut33);
            currentAnimation.start();
        }
    }

    private static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    private static void invalidateGlobalRegion(View view, RectF childBounds) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor((double) childBounds.left), (int) Math.floor((double) childBounds.top), (int) Math.ceil((double) childBounds.right), (int) Math.ceil((double) childBounds.bottom));
        }
    }

    private void removeLongPressCallback() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean z = false;
        switch (ev.getAction()) {
            case 0:
                this.mDragging = false;
                this.mLongPressSent = false;
                this.mVelocityTracker.clear();
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                if (this.mCurrView != null) {
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    this.mPerpendicularInitialTouchPos = getPerpendicularPos(ev);
                    this.mTranslation = getTranslation(this.mCurrView);
                    break;
                }
                break;
            case 1:
            case 3:
                boolean captured;
                if (this.mDragging || this.mLongPressSent) {
                    captured = true;
                } else {
                    captured = false;
                }
                this.mDragging = false;
                this.mCurrView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (captured) {
                    return true;
                }
                break;
            case 2:
                if (!(this.mCurrView == null || this.mLongPressSent)) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    float deltaPerpendicular = getPerpendicularPos(ev) - this.mPerpendicularInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop && Math.abs(delta) > Math.abs(deltaPerpendicular)) {
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev);
                        this.mTranslation = getTranslation(this.mCurrView);
                        removeLongPressCallback();
                        break;
                    }
                }
        }
        if (this.mDragging || this.mLongPressSent) {
            z = true;
        }
        return z;
    }

    private void dismissChild(View view, float velocity, boolean useAccelerateInterpolator) {
        dismissChild(view, velocity, null, 0, useAccelerateInterpolator, 0, false);
    }

    private void dismissChild(View animView, float velocity, Runnable endAction, long delay, boolean useAccelerateInterpolator, long fixedDuration, boolean isDismissAll) {
        float newPos;
        long duration;
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        boolean isLayoutRtl = animView.getLayoutDirection() == 1;
        boolean animateUpForMenu = velocity == 0.0f && ((getTranslation(animView) == 0.0f || isDismissAll) && this.mSwipeDirection == 1);
        boolean animateLeftForRtl = velocity == 0.0f && ((getTranslation(animView) == 0.0f || isDismissAll) && isLayoutRtl);
        boolean animateLeft = velocity < 0.0f || (velocity == 0.0f && getTranslation(animView) < 0.0f && !isDismissAll);
        if (animateLeft || animateLeftForRtl || animateUpForMenu) {
            newPos = -getSize(animView);
        } else {
            newPos = getSize(animView);
        }
        if (fixedDuration != 0) {
            duration = fixedDuration;
        } else if (velocity != 0.0f) {
            duration = Math.min(400, (long) ((int) ((Math.abs(newPos - getTranslation(animView)) * 1000.0f) / Math.abs(velocity))));
        } else {
            duration = 200;
        }
        final View view = animView;
        Animator anim = getViewTranslationAnimator(animView, newPos, new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        });
        if (anim != null) {
            if (useAccelerateInterpolator) {
                anim.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                anim.setDuration(duration);
            } else {
                this.mFlingAnimationUtils.applyDismissing(anim, getTranslation(animView), newPos, velocity, getSize(animView));
            }
            if (delay > 0) {
                anim.setStartDelay(delay);
            }
            final View view2 = animView;
            final Runnable runnable = endAction;
            anim.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    SwipeHelper.invalidateGlobalRegion(view2);
                    if (!this.mCancelled) {
                        SwipeHelper.this.mCallback.onChildDismissed(view2);
                    }
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            });
            dismissChangeBackgroundColor(animView, canBeDismissed);
            anim.start();
        }
    }

    private void snapChild(final View animView, float targetLeft, float velocity) {
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        Animator anim = getViewTranslationAnimator(animView, targetLeft, new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(animView, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        });
        if (anim != null) {
            anim.setDuration((long) 150);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    SwipeHelper.invalidateGlobalRegion(animView);
                }
            });
            anim.start();
        }
    }

    private void onTranslationUpdate(View animView, float value, boolean canBeDismissed) {
        invalidateGlobalRegion(animView);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (this.mDragging) {
            this.mVelocityTracker.addMovement(ev);
            switch (ev.getAction()) {
                case 1:
                case 3:
                    if (this.mCurrView == null) {
                        return true;
                    }
                    this.mVelocityTracker.computeCurrentVelocity(1000, getMaxVelocity());
                    float velocity = getVelocity(this.mVelocityTracker);
                    if (isDismissGesture(ev)) {
                        boolean z;
                        View view = this.mCurrView;
                        if (swipedFastEnough()) {
                            z = false;
                        } else {
                            z = true;
                        }
                        dismissChild(view, velocity, z);
                    } else {
                        snapChild(this.mCurrView, 0.0f, velocity);
                    }
                    this.mCurrView = null;
                    this.mDragging = false;
                    return true;
                case 2:
                case 4:
                    if (this.mCurrView == null) {
                        return true;
                    }
                    float delta = getPos(ev) - this.mInitialTouchPos;
                    float absDelta = Math.abs(delta);
                    if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                        float size = getSize(this.mCurrView);
                        float maxScrollDistance = 0.25f * size;
                        delta = absDelta >= size ? delta > 0.0f ? maxScrollDistance : -maxScrollDistance : maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                    }
                    setTranslation(this.mCurrView, this.mTranslation + delta);
                    invalidateGlobalRegion(this.mCurrView);
                    return true;
                default:
                    return true;
            }
        } else if (this.mCallback.getChildAtPosition(ev) != null) {
            onInterceptTouchEvent(ev);
            return true;
        } else {
            removeLongPressCallback();
            return false;
        }
    }

    private float getMaxVelocity() {
        return 4000.0f * this.mDensityScale;
    }

    private float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    private float getUnscaledEscapeVelocity() {
        return 100.0f;
    }

    private long getMaxEscapeAnimDuration() {
        return 400;
    }

    private boolean swipedFarEnough() {
        return ((double) Math.abs(getTranslation(this.mCurrView))) > 0.4d * ((double) getSize(this.mCurrView));
    }

    private boolean isDismissGesture(MotionEvent ev) {
        if ((swipedFastEnough() || swipedFarEnough()) && ev.getActionMasked() == 1 && this.mCallback.canChildBeDismissed(this.mCurrView)) {
            return true;
        }
        return false;
    }

    private boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        if (Math.abs(velocity) > getEscapeVelocity()) {
            if ((velocity > 0.0f) == (translation > 0.0f)) {
                return true;
            }
        }
        return false;
    }
}
