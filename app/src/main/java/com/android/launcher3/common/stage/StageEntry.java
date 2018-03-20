package com.android.launcher3.common.stage;

import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class StageEntry {
    public boolean broughtToHome;
    public boolean enableAnimation = true;
    public int fromStage;
    private ArrayList<Runnable> mCompleteRunnableCallBacks;
    private HashMap<String, Object> mExtras;
    private int mInternalStateFrom;
    private int mInternalStateTo;
    private HashMap<View, Integer> mLayerViews;
    public int stageCountOnFinishAllStage;
    public int toStage;

    public void setInternalStateFrom(int state) {
        this.mInternalStateFrom = state;
    }

    public int getInternalStateFrom() {
        return this.mInternalStateFrom;
    }

    public void setInternalStateTo(int state) {
        this.mInternalStateTo = state;
    }

    public int getInternalStateTo() {
        return this.mInternalStateTo;
    }

    public HashMap<View, Integer> getLayerViews() {
        if (this.mLayerViews == null) {
            this.mLayerViews = new HashMap();
        }
        return this.mLayerViews;
    }

    public void addOnCompleteRunnableCallBack(Runnable onCompleteRunnable) {
        if (onCompleteRunnable != null) {
            if (this.mCompleteRunnableCallBacks == null) {
                this.mCompleteRunnableCallBacks = new ArrayList();
            }
            this.mCompleteRunnableCallBacks.add(onCompleteRunnable);
        }
    }

    public void notifyOnCompleteRunnables() {
        if (this.mCompleteRunnableCallBacks != null) {
            Iterator it = this.mCompleteRunnableCallBacks.iterator();
            while (it.hasNext()) {
                ((Runnable) it.next()).run();
            }
        }
    }

    public void putExtras(String key, Object value) {
        if (this.mExtras == null) {
            this.mExtras = new HashMap();
        }
        this.mExtras.put(key, value);
    }

    public Object getExtras(String key) {
        if (this.mExtras == null) {
            return null;
        }
        return this.mExtras.get(key);
    }

    public Object getExtras(String key, Object defaultValue) {
        if (this.mExtras == null) {
            return defaultValue;
        }
        Object result = this.mExtras.get(key);
        return result != null ? result : defaultValue;
    }
}
