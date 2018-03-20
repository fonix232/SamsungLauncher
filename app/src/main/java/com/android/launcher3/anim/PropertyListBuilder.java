package com.android.launcher3.anim;

import android.animation.PropertyValuesHolder;
import android.view.View;
import java.util.ArrayList;

public class PropertyListBuilder {
    private final ArrayList<PropertyValuesHolder> mProperties = new ArrayList();

    public PropertyListBuilder translationX(float value) {
        this.mProperties.add(PropertyValuesHolder.ofFloat(View.TRANSLATION_X, new float[]{value}));
        return this;
    }

    public PropertyListBuilder translationY(float value) {
        this.mProperties.add(PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, new float[]{value}));
        return this;
    }

    public PropertyListBuilder scaleX(float value) {
        this.mProperties.add(PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{value}));
        return this;
    }

    public PropertyListBuilder scaleY(float value) {
        this.mProperties.add(PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{value}));
        return this;
    }

    public PropertyListBuilder scale(float value) {
        return scaleX(value).scaleY(value);
    }

    public PropertyListBuilder alpha(float value) {
        this.mProperties.add(PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{value}));
        return this;
    }

    public PropertyValuesHolder[] build() {
        return (PropertyValuesHolder[]) this.mProperties.toArray(new PropertyValuesHolder[this.mProperties.size()]);
    }
}
