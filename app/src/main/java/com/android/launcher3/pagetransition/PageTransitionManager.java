package com.android.launcher3.pagetransition;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.pagetransition.effects.Accordian;
import com.android.launcher3.pagetransition.effects.Card;
import com.android.launcher3.pagetransition.effects.CardFlip;
import com.android.launcher3.pagetransition.effects.Carousal;
import com.android.launcher3.pagetransition.effects.Cascade;
import com.android.launcher3.pagetransition.effects.Conveyor;
import com.android.launcher3.pagetransition.effects.Fan;
import com.android.launcher3.pagetransition.effects.InnerCube;
import com.android.launcher3.pagetransition.effects.OuterCube;
import com.android.launcher3.pagetransition.effects.PageTransitionEffects;
import com.android.launcher3.pagetransition.effects.Plain;
import com.android.launcher3.pagetransition.effects.Rotate;
import com.android.launcher3.pagetransition.effects.Spiral;
import com.android.launcher3.pagetransition.effects.ZoomOut;
import java.util.HashMap;

public class PageTransitionManager {
    private static final int DEFAULT_TRANSITION = 0;
    private static final String TAG = "HomePageTransitionController";
    public static boolean mLeftMove = false;
    private static PagedView mPagedView;
    public boolean isPageTransformEnabled = true;
    protected TransitionEffect mCurrentTransitionEffect = TransitionEffect.DEFAULT;
    private Launcher mLauncher;
    private PageTransitionEffects mPageTransitionEffects;
    private final HashMap<TransitionEffect, Class<? extends PageTransitionEffects>> mTransitionMap = new HashMap();

    public enum TransitionEffect {
        DEFAULT,
        CASCADE,
        OUTERCUBE,
        INNERCUBE,
        CAROUSAL,
        PLAIN,
        CONVEYOR,
        CARD,
        ACCORDIAN,
        CARDFLIP,
        FAN,
        ROTATE,
        SPIRAL,
        ZOOM
    }

    public void onPageBeginMoving() {
        this.isPageTransformEnabled = true;
    }

    public void onPageEndMoving() {
        this.isPageTransformEnabled = false;
    }

    public PageTransitionManager(Context context) {
        this.mLauncher = (Launcher) context;
        initializeTransitionMap();
    }

    public static float getScrollX() {
        return (float) mPagedView.getScrollX();
    }

    public static float getMaxScrollX() {
        return (float) mPagedView.getMaxScrollX();
    }

    public static float getShrinkX() {
        return (float) mPagedView.getMaxScrollX();
    }

    public static int getChildCount() {
        return mPagedView.getChildCount();
    }

    public static boolean isLeftScroll() {
        return mLeftMove;
    }

    public void setLeftScroll(boolean value) {
        mLeftMove = value;
    }

    public static float getPageBackgroundAlpha() {
        return mPagedView.getPageBackgroundAlpha();
    }

    public PageTransitionEffects getPageTransitionEffects() {
        return this.mPageTransitionEffects;
    }

    public void onLayout(ViewGroup workspace, boolean changed, int left, int top, int right, int bottom) {
        if (this.mPageTransitionEffects != null) {
            this.mPageTransitionEffects.onLayout(workspace, changed, left, top, right, bottom);
        }
    }

    public TransitionEffect getCurrentTransitionEffect() {
        return this.mCurrentTransitionEffect;
    }

    public void setCurrentTransitionEffect(int transitionEffect) {
        if (transitionEffect < 0 || transitionEffect >= TransitionEffect.values().length) {
            transitionEffect = 0;
        }
        this.mCurrentTransitionEffect = transitionEffect == 0 ? null : TransitionEffect.values()[transitionEffect];
        if (this.mCurrentTransitionEffect == null) {
            this.mPageTransitionEffects = null;
            return;
        }
        Class<? extends PageTransitionEffects> klass = (Class) this.mTransitionMap.get(this.mCurrentTransitionEffect);
        if (klass == null) {
            throw new IllegalArgumentException(this.mCurrentTransitionEffect + " effect not found!!");
        }
        try {
            this.mPageTransitionEffects = (PageTransitionEffects) klass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(this.mCurrentTransitionEffect + " fail to create instance");
        }
    }

    public void setup(PagedView pagedView) {
        mPagedView = pagedView;
    }

    private void initializeTransitionMap() {
        this.mTransitionMap.put(TransitionEffect.CASCADE, Cascade.class);
        this.mTransitionMap.put(TransitionEffect.OUTERCUBE, OuterCube.class);
        this.mTransitionMap.put(TransitionEffect.INNERCUBE, InnerCube.class);
        this.mTransitionMap.put(TransitionEffect.CAROUSAL, Carousal.class);
        this.mTransitionMap.put(TransitionEffect.PLAIN, Plain.class);
        this.mTransitionMap.put(TransitionEffect.CONVEYOR, Conveyor.class);
        this.mTransitionMap.put(TransitionEffect.CARD, Card.class);
        this.mTransitionMap.put(TransitionEffect.ACCORDIAN, Accordian.class);
        this.mTransitionMap.put(TransitionEffect.CARDFLIP, CardFlip.class);
        this.mTransitionMap.put(TransitionEffect.FAN, Fan.class);
        this.mTransitionMap.put(TransitionEffect.ROTATE, Rotate.class);
        this.mTransitionMap.put(TransitionEffect.SPIRAL, Spiral.class);
        this.mTransitionMap.put(TransitionEffect.ZOOM, ZoomOut.class);
    }

    public int getCurrentWorkspaceScreen() {
        return mPagedView.getCurrentPage();
    }

    public void reset(View page) {
        this.mPageTransitionEffects.reset(page);
        page.setLayerType(0, null);
    }

    public void transformPage(View page, float scrollProgress) {
        if (this.isPageTransformEnabled && this.mCurrentTransitionEffect != null) {
            CellLayout cl = (CellLayout) page;
            if (cl.getCellLayoutChildren() != null) {
                cl.getCellLayoutChildren().setPadding(0, 0, 0, 0);
            }
            if (page.getLayerType() != 2) {
                page.setLayerType(2, null);
            }
            this.mPageTransitionEffects.applyTransform(page, scrollProgress, mPagedView.indexOfChild(page));
        }
    }

    Context getContext() {
        return this.mLauncher;
    }
}
