package com.android.launcher3.widget.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import com.sec.android.app.launcher.R;
import java.util.List;

public class WidgetItemSingleView extends WidgetItemView {
    private static final long sPreviewFadeInDuration = 80;
    private static final long sPreviewFadeInStaggerDuration = 20;
    private TextView mDims;
    private ImageView mImage;
    private TextView mTitle;

    public WidgetItemSingleView(Context context) {
        this(context, null);
    }

    public WidgetItemSingleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetItemSingleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTitle = (TextView) findViewById(R.id.widget_name);
        this.mDims = (TextView) findViewById(R.id.widget_dims);
        this.mImage = (ImageView) findViewById(R.id.widget_preview);
        this.mUninstallIcon = (ImageView) findViewById(R.id.widget_uninstall_icon);
    }

    protected TextView getTitleTextView() {
        return this.mTitle;
    }

    protected void applyTitle(String label) {
        this.mTitle.setText(label);
        this.mTitle.semSetHoverPopupType(0);
    }

    protected void applyCellSpan() {
        int[] cellSpan = ((PendingAddItemInfo) this.mWidgets.get(0)).getSpan();
        this.mDims.setText(String.format(this.mDimensionsFormatString, new Object[]{Integer.valueOf(cellSpan[0]), Integer.valueOf(cellSpan[1])}));
        this.mDims.setContentDescription(String.format(this.mTalkbackDimen, new Object[]{Integer.valueOf(cellSpan[0]), Integer.valueOf(cellSpan[1])}));
    }

    protected int getPreviewImageWidth() {
        return getResources().getDimensionPixelSize(R.dimen.widget_itemview_single_preview_image_width);
    }

    protected int getPreviewImageHeight() {
        return getResources().getDimensionPixelSize(R.dimen.widget_itemview_single_preview_image_height);
    }

    protected Object loadPreview(JobContext jc, List<PendingAddItemInfo> items, int width, int height) {
        return this.mPreviewUtils.generatePreview(this.mLauncher, ((PendingAddItemInfo) items.get(0)).getProviderInfo(), width, height);
    }

    protected void postToSetPreview(Object previews, int width, int height) {
        if (previews != null) {
            applyPreview(new FastBitmapDrawable((Bitmap) previews), 0, false, true);
        }
    }

    private void applyPreview(FastBitmapDrawable preview, int index, boolean scale, boolean animate) {
        if (this.mImage != null && preview != null) {
            this.mImage.setImageDrawable(preview);
            if (animate) {
                this.mImage.setAlpha(0.0f);
                this.mImage.animate().alpha(1.0f).setDuration(sPreviewFadeInDuration + (((long) index) * sPreviewFadeInStaggerDuration)).start();
            }
        }
    }

    protected void resetToRecycle() {
        super.resetToRecycle();
        this.mImage.setImageDrawable(null);
        ((TextView) findViewById(R.id.widget_name)).setText(null);
    }

    public void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeTextColorForBg(getContext(), this.mTitle, whiteBg);
        WhiteBgManager.changeTextColorForBg(getContext(), this.mDims, whiteBg);
        int bgDrawable = getWidgetPreviewBg(whiteBg);
        if (LauncherFeature.supportNewWidgetList()) {
            setBackgroundResource(bgDrawable);
        } else {
            findViewById(R.id.widget_preview_container).setBackgroundResource(bgDrawable);
        }
    }

    protected void prepareUninstallEnter(boolean isSystemApp, boolean animate) {
        if (LauncherFeature.supportNewWidgetList()) {
            if (isSystemApp) {
                setAlpha(0.4f);
            }
            if (animate) {
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(this.mDims, new PropertyValuesHolder[]{mWidgetInfoAlphaOut});
                animator.setInterpolator(ViInterpolator.getInterploator(35));
                animator.setDuration((long) getResources().getInteger(R.integer.widget_itemview_icon_anim_duration));
                animator.start();
            }
            this.mDims.setVisibility(4);
        }
    }

    protected void prepareUninstallExit(boolean isSystemApp, boolean animate) {
        if (LauncherFeature.supportNewWidgetList()) {
            if (isSystemApp) {
                setAlpha(1.0f);
            }
            if (animate) {
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(this.mDims, new PropertyValuesHolder[]{mWidgetInfoAlphaIn});
                animator.setInterpolator(ViInterpolator.getInterploator(35));
                animator.setDuration((long) getResources().getInteger(R.integer.widget_itemview_icon_anim_duration));
                animator.start();
            } else {
                this.mDims.setAlpha(1.0f);
            }
            this.mDims.setVisibility(View.VISIBLE);
        }
    }
}
