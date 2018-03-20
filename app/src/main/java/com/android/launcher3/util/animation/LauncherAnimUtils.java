package com.android.launcher3.util.animation;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewTreeObserver.OnDrawListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

public class LauncherAnimUtils {
    static WeakHashMap<Animator, Object> sAnimators = new WeakHashMap();
    static AnimatorListener sEndAnimListener = new AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            LauncherAnimUtils.sAnimators.put(animation, null);
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            LauncherAnimUtils.sAnimators.remove(animation);
        }

        public void onAnimationCancel(Animator animation) {
            LauncherAnimUtils.sAnimators.remove(animation);
        }
    };

    public static void cancelOnDestroyActivity(Animator a) {
        a.addListener(sEndAnimListener);
    }

    public static void startAnimationAfterNextDraw(final Animator animator, final View view) {
        view.getViewTreeObserver().addOnDrawListener(new OnDrawListener() {
            private boolean mStarted = false;

            public void onDraw() {
                if (!this.mStarted) {
                    this.mStarted = true;
                    if (animator.getDuration() != 0) {
                        animator.start();
                        final AnonymousClass2 listener = this;
                        view.post(new Runnable() {
                            public void run() {
                                view.getViewTreeObserver().removeOnDrawListener(listener);
                            }
                        });
                    }
                }
            }
        });
    }

    public static void onDestroyActivity() {
        Iterator it = new HashSet(sAnimators.keySet()).iterator();
        while (it.hasNext()) {
            Animator a = (Animator) it.next();
            if (a.isRunning()) {
                a.cancel();
            }
            sAnimators.remove(a);
        }
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet anim = new AnimatorSet();
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ValueAnimator ofFloat(View target, float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        return anim;
    }

    public static ObjectAnimator ofFloat(View target, String propertyName, float... values) {
        ValueAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setPropertyName(propertyName);
        anim.setFloatValues(values);
        cancelOnDestroyActivity(anim);
        FirstFrameAnimatorHelper firstFrameAnimatorHelper = new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(View target, PropertyValuesHolder... values) {
        ValueAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        FirstFrameAnimatorHelper firstFrameAnimatorHelper = new FirstFrameAnimatorHelper(anim, target);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target, View view, PropertyValuesHolder... values) {
        ValueAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        cancelOnDestroyActivity(anim);
        FirstFrameAnimatorHelper firstFrameAnimatorHelper = new FirstFrameAnimatorHelper(anim, view);
        return anim;
    }
}
