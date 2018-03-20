package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import java.util.HashMap;
import java.util.Map.Entry;

public class AnimationLayerSet extends AnimatorListenerAdapter {
    private final HashMap<View, Integer> mViewsToLayerTypeMap;

    public AnimationLayerSet() {
        this.mViewsToLayerTypeMap = new HashMap();
    }

    public AnimationLayerSet(View v) {
        this.mViewsToLayerTypeMap = new HashMap(1);
        addView(v);
    }

    public void addView(View v) {
        this.mViewsToLayerTypeMap.put(v, Integer.valueOf(v.getLayerType()));
    }

    public void onAnimationStart(Animator animation) {
        for (Entry<View, Integer> entry : this.mViewsToLayerTypeMap.entrySet()) {
            View v = (View) entry.getKey();
            entry.setValue(Integer.valueOf(v.getLayerType()));
            v.setLayerType(2, null);
            if (v.isAttachedToWindow() && v.getVisibility() == 0) {
                v.buildLayer();
            }
        }
    }

    public void onAnimationEnd(Animator animation) {
        for (Entry<View, Integer> entry : this.mViewsToLayerTypeMap.entrySet()) {
            ((View) entry.getKey()).setLayerType(((Integer) entry.getValue()).intValue(), null);
        }
    }
}
