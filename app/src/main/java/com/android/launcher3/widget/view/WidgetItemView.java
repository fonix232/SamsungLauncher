package com.android.launcher3.widget.view;

import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.util.threadpool.Future;
import com.android.launcher3.util.threadpool.FutureListener;
import com.android.launcher3.util.threadpool.ThreadPool;
import com.android.launcher3.util.threadpool.ThreadPool.Job;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.model.WidgetPreviewUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;

public abstract class WidgetItemView extends LinearLayout {
    private static final String TAG = "WidgetItemView";
    protected static final PropertyValuesHolder mBackgroundAlphaIn = PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{0.46f, 1.0f});
    protected static final PropertyValuesHolder mBackgroundAlphaOut = PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{1.0f, 0.46f});
    protected static final PropertyValuesHolder mWidgetInfoAlphaIn = PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{0.0f, 1.0f});
    protected static final PropertyValuesHolder mWidgetInfoAlphaOut = PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{1.0f, 0.0f});
    protected String mDimensionsFormatString;
    private int mHighlightColor;
    protected Launcher mLauncher;
    protected Future<Object> mLoadTask;
    protected WidgetPreviewUtils mPreviewUtils;
    protected String mTalkbackDimen;
    private final ThreadPool mThreadPool;
    protected Animation mUninstallEnterIconAnimation;
    protected Animation mUninstallExitIconAnimation;
    protected ImageView mUninstallIcon;
    protected List<PendingAddItemInfo> mWidgets;

    private class PreviewJobListener implements FutureListener<Object> {
        private final int mHeight;
        private final WidgetPageLayout mLayout;
        private final int mWidth;

        public PreviewJobListener(WidgetPageLayout layout, int width, int height) {
            this.mLayout = layout;
            this.mWidth = width;
            this.mHeight = height;
        }

        public void onFutureDone(final Future<Object> future) {
            WidgetItemView.this.mLoadTask = null;
            final Object object = future.get();
            if (object != null && !future.isCancelled()) {
                WidgetItemView.this.post(new Runnable() {
                    public void run() {
                        if (!future.isCancelled() && WidgetItemView.this.getParent() != null && PreviewJobListener.this.mLayout.equals(WidgetItemView.this.getParent())) {
                            WidgetItemView.this.postToSetPreview(object, PreviewJobListener.this.mWidth, PreviewJobListener.this.mHeight);
                        }
                    }
                });
            }
        }
    }

    private class PreviewLoadTask implements Job<Object> {
        private final int mHeight;
        private final List<PendingAddItemInfo> mItems;
        private final int mWidth;

        public PreviewLoadTask(List<PendingAddItemInfo> items, int width, int height) {
            this.mItems = items;
            this.mWidth = width;
            this.mHeight = height;
        }

        public Object run(JobContext jc) {
            if (jc.isCancelled()) {
                return null;
            }
            return WidgetItemView.this.loadPreview(jc, this.mItems, this.mWidth, this.mHeight);
        }
    }

    protected abstract void applyTitle(String str);

    protected abstract int getPreviewImageHeight();

    protected abstract int getPreviewImageWidth();

    protected abstract TextView getTitleTextView();

    protected abstract Object loadPreview(JobContext jobContext, List<PendingAddItemInfo> list, int i, int i2);

    protected abstract void postToSetPreview(Object obj, int i, int i2);

    public WidgetItemView(Context context) {
        this(context, null);
    }

    public WidgetItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWidgets = new ArrayList();
        Resources r = context.getResources();
        this.mUninstallEnterIconAnimation = AnimationUtils.loadAnimation(context, R.anim.uninstall_icon_show_anim);
        this.mUninstallExitIconAnimation = AnimationUtils.loadAnimation(context, R.anim.uninstall_icon_hide_anim);
        this.mUninstallExitIconAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                WidgetItemView.this.uninstallExitIconAnimationEnd();
            }
        });
        this.mDimensionsFormatString = r.getString(R.string.widget_dims_format);
        this.mTalkbackDimen = r.getString(R.string.talkback_widget_dims_format);
        setHapticFeedbackEnabled(false);
        setWillNotDraw(false);
        setClipToPadding(false);
        this.mHighlightColor = r.getColor(R.color.widget_text_highlight_color, null);
        this.mThreadPool = LauncherAppState.getInstance().getThreadPool();
        this.mLauncher = (Launcher) context;
        this.mPreviewUtils = WidgetPreviewUtils.getInstance();
    }

    protected void setTalkbackDescription(boolean isUninstall, boolean isSystemApp) {
        TextView name = getTitleTextView();
        if (name != null) {
            String nameDescription = name.getText().toString();
            if (isUninstall && !isSystemApp) {
                nameDescription = getResources().getString(R.string.multi_select_uninstall) + " " + nameDescription;
            }
            name.setContentDescription(nameDescription);
        }
    }

    public void enterUninstallMode(boolean animate) {
        boolean z = true;
        PendingAddItemInfo obj = getTag();
        if (obj instanceof PendingAddItemInfo) {
            boolean z2;
            boolean uninstallable = obj.uninstallable(this.mLauncher);
            if (uninstallable) {
                z2 = false;
            } else {
                z2 = true;
            }
            setTalkbackDescription(true, z2);
            if (uninstallable) {
                this.mUninstallIcon.setContentDescription(" ");
                this.mUninstallIcon.setVisibility(View.VISIBLE);
                if (animate) {
                    startUninstallEnterAnimation();
                }
            }
            if (uninstallable) {
                z = false;
            }
            prepareUninstallEnter(z, animate);
        }
    }

    public void exitUninstallMode(boolean animate) {
        boolean z = true;
        PendingAddItemInfo obj = getTag();
        if (obj instanceof PendingAddItemInfo) {
            boolean z2;
            boolean uninstallable = obj.uninstallable(this.mLauncher);
            if (uninstallable) {
                z2 = false;
            } else {
                z2 = true;
            }
            setTalkbackDescription(false, z2);
            if (uninstallable && animate) {
                startUninstallExitAnimation();
            }
            if (uninstallable) {
                z = false;
            }
            prepareUninstallExit(z, animate);
            this.mUninstallIcon.setVisibility(View.GONE);
            this.mUninstallIcon.setFocusable(false);
        }
    }

    protected final void applyTileAndSpan(String searchString) {
        PendingAddItemInfo info = (PendingAddItemInfo) this.mWidgets.get(0);
        applyTitle(this instanceof WidgetItemFolderView ? info.getApplicationLabel() : info.getLabel(getContext()));
        if (!(searchString == null || searchString.isEmpty())) {
            applyHighlightTitle(getTitleTextView().getText().toString(), searchString);
        }
        applyCellSpan();
    }

    protected void applyCellSpan() {
    }

    protected void prepareUninstallEnter(boolean isSystemApp, boolean animate) {
    }

    protected void prepareUninstallExit(boolean isSystemApp, boolean animate) {
    }

    private void uninstallExitIconAnimationEnd() {
        if (this.mUninstallIcon != null) {
            this.mUninstallIcon.setVisibility(View.GONE);
        }
    }

    private void startUninstallEnterAnimation() {
        if (this.mUninstallIcon != null) {
            this.mUninstallIcon.startAnimation(this.mUninstallEnterIconAnimation);
        }
    }

    private void startUninstallExitAnimation() {
        if (this.mUninstallIcon != null && this.mUninstallIcon.getVisibility() == 0) {
            this.mUninstallIcon.startAnimation(this.mUninstallExitIconAnimation);
        }
    }

    public void setWidgets(List<PendingAddItemInfo> widgets) {
        this.mWidgets.clear();
        this.mWidgets.addAll(widgets);
    }

    public List<PendingAddItemInfo> getWidgets() {
        return this.mWidgets;
    }

    public void applyHighlightTitle(String label, String highlight) {
        int indexOf;
        TextView name = getTitleTextView();
        highlight = highlight.trim();
        int highlightStrLength = highlight.length();
        Spannable highLightText = new SpannableString(label);
        char[] iQueryForIndian = TextUtils.semGetPrefixCharForSpan(name.getPaint(), label, highlight.toCharArray());
        if (iQueryForIndian != null) {
            String s = new String(iQueryForIndian);
            indexOf = label.toLowerCase().indexOf(s.toLowerCase());
            highlightStrLength = s.length();
        } else {
            indexOf = label.toLowerCase().indexOf(highlight.toLowerCase());
        }
        if (indexOf > -1) {
            try {
                highLightText.setSpan(new ForegroundColorSpan(this.mHighlightColor), indexOf, indexOf + highlightStrLength, 0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "highLightText<" + highLightText + ">length = " + highLightText.length() + " ,highlight<" + highlight + ">length = " + highlight.length() + " ,indexOf = " + indexOf + " ," + "highlightStrLength = " + highlightStrLength);
                if (indexOf < highLightText.length()) {
                    highLightText.setSpan(new ForegroundColorSpan(this.mHighlightColor), indexOf, highLightText.length(), 0);
                }
            }
        }
        name.setText(highLightText);
    }

    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        ViewParent ret = super.invalidateChildInParent(location, dirty);
        ((View) getParent()).invalidate();
        return ret;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
        }
        return true;
    }

    protected void resetToRecycle() {
        if (this.mLoadTask != null) {
            this.mLoadTask.cancel();
            this.mLoadTask = null;
        }
        setNextFocusLeftId(-1);
        setNextFocusRightId(-1);
        exitUninstallMode(false);
        setTag(null);
    }

    public void requestPreview(WidgetPageLayout layout) {
        this.mLoadTask = this.mThreadPool.submit(new PreviewLoadTask(this.mWidgets, getPreviewImageWidth(), getPreviewImageHeight()), new PreviewJobListener(layout, getPreviewImageWidth(), getPreviewImageHeight()));
    }

    public void changeState(State state, boolean anim) {
        if (state.equals(State.NORMAL)) {
            exitUninstallMode(anim);
        } else if (state.equals(State.UNINSTALL)) {
            enterUninstallMode(anim);
        }
    }

    protected int getWidgetPreviewBg(boolean whiteBg) {
        if (LauncherFeature.supportNewWidgetList()) {
            return whiteBg ? R.drawable.widget_item_background_w : R.drawable.widget_item_background;
        } else {
            return whiteBg ? R.drawable.homescreen_widgets_preview_bg_w : R.drawable.homescreen_widgets_preview_bg;
        }
    }

    public void changeColorForBg(boolean whiteBg) {
    }

    protected boolean supportLongClick() {
        return true;
    }
}
