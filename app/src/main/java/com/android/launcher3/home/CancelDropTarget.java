package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.sec.android.app.launcher.R;
import java.util.Iterator;

public class CancelDropTarget extends FrameLayout implements DropTarget, DragListener, OnClickListener {
    private static final int ANIMATION_SCALE_DURATION = 175;
    private static final int CIRCLE_ANIMATION_DURATION = 233;
    private static final int DRAG_VIEW_DROP_DURATION = 266;
    private static final int IMAGEVIEW_SCALE_DURATION = 133;
    private static final int TEXTVIEW_FADE_DURATION = 233;
    private boolean mActive;
    private LinearLayout mCancelLayout;
    private ImageView mCircleView;
    private Drawable mDrawable;
    private boolean mEnable;
    private ImageView mImageView;
    private Launcher mLauncher;
    private TextView mTextView;

    public CancelDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CancelDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLauncher = (Launcher) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        FrameLayout iconContainer = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.drop_target_cancel_icon, this, true);
        this.mCancelLayout = (LinearLayout) iconContainer.findViewById(R.id.drop_target_cancel_layout);
        this.mTextView = (TextView) iconContainer.findViewById(R.id.drop_target_cancel_text);
        this.mImageView = (ImageView) iconContainer.findViewById(R.id.drop_target_cancel_image);
        this.mCircleView = (ImageView) iconContainer.findViewById(R.id.drop_target_cancel_circle);
        if (WhiteBgManager.isWhiteBg()) {
            changeColorForBg(true);
        }
        setCancelDropTargetVisible(false);
        if (!LauncherFeature.isTablet() && this.mLauncher.getDeviceProfile().isLandscape) {
            this.mCancelLayout.setOrientation(0);
        }
        setResource();
    }

    private void setResource() {
        int newHeight;
        CharSequence label = getResources().getString(R.string.cancel);
        this.mDrawable = getResources().getDrawable(R.drawable.toolbar_ic_cancel_normal, null);
        this.mImageView.setImageDrawable(this.mDrawable);
        this.mTextView.setText(label);
        boolean useRectangleImg = !LauncherFeature.isTablet() && this.mLauncher.getDeviceProfile().isLandscape;
        this.mCircleView.setImageDrawable(getResources().getDrawable(useRectangleImg ? R.drawable.drop_target_cancel_circle_red_land : R.drawable.drop_target_cancel_circle_red, null));
        if (LauncherFeature.isTablet()) {
            newHeight = getResources().getDimensionPixelOffset(R.dimen.drop_target_cancel_animation_circleview_width);
        } else {
            newHeight = getResources().getDimensionPixelOffset(R.dimen.drop_target_bar_height);
        }
        onConfigurationChangedIfNeeded(newHeight);
    }

    public void onDragEnter(DragObject d, boolean dropTargetChanged) {
        if (this.mActive) {
            this.mCircleView.setVisibility(View.VISIBLE);
            animateCircleView(true);
            AnimatorSet anim = new AnimatorSet();
            anim.setDuration(133);
            anim.playTogether(new Animator[]{ObjectAnimator.ofFloat(this.mImageView, "scaleX", new float[]{1.0f, 1.2f}), ObjectAnimator.ofFloat(this.mImageView, "scaleY", new float[]{1.0f, 1.2f})});
            anim.start();
            Talk.INSTANCE.say(getResources().getString(R.string.cancel) + " " + getResources().getString(R.string.button));
        }
    }

    public void onDragExit(DragObject d, boolean dropTargetChanged) {
        if (this.mActive) {
            animateCircleView(false);
            AnimatorSet anim = new AnimatorSet();
            anim.setDuration(133);
            anim.playTogether(new Animator[]{ObjectAnimator.ofFloat(this.mImageView, "scaleX", new float[]{1.2f, 1.0f}), ObjectAnimator.ofFloat(this.mImageView, "scaleY", new float[]{1.2f, 1.0f})});
            anim.start();
        }
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        return true;
    }

    public boolean onDragEnd() {
        if (this.mActive) {
            this.mActive = false;
            this.mEnable = false;
            animateScale(false);
        }
        return true;
    }

    public void onDragOver(DragObject d) {
    }

    public void onDrop(DragObject dragObject) {
        this.mActive = false;
        this.mEnable = false;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(dragObject.dragView, from);
        int width = this.mDrawable.getIntrinsicWidth();
        int height = this.mDrawable.getIntrinsicHeight();
        Rect to = getIconRect(dragObject.dragView.getMeasuredWidth(), dragObject.dragView.getMeasuredHeight(), width, height);
        float scale = ((float) to.width()) / ((float) from.width());
        Runnable anonymousClass1 = new Runnable() {
            public void run() {
                CancelDropTarget.this.mLauncher.getHomeController().exitDragStateDelayed(0);
            }
        };
        dragLayer.animateView(dragObject.dragView, from, to, scale, 1.0f, 1.0f, 0.1f, 0.1f, DRAG_VIEW_DROP_DURATION, new DecelerateInterpolator(2.0f), new LinearInterpolator(), anonymousClass1, 0, null);
        if (!(dragObject.extraDragInfoList == null || dragObject.extraDragSourceList == null)) {
            Iterator it = dragObject.extraDragInfoList.iterator();
            while (it.hasNext()) {
                DragObject d = (DragObject) it.next();
                dragLayer.getViewRectRelativeToSelf(d.dragView, from);
                to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(), width, height);
                dragLayer.animateView(d.dragView, from, to, ((float) to.width()) / ((float) from.width()), 1.0f, 1.0f, 0.1f, 0.1f, DRAG_VIEW_DROP_DURATION, new DecelerateInterpolator(2.0f), new LinearInterpolator(), anonymousClass1, 0, null);
            }
        }
        Talk.INSTANCE.say((int) R.string.cancel);
        Animator alphaAnimator = ObjectAnimator.ofFloat(this.mTextView, View.ALPHA, new float[]{0.0f});
        alphaAnimator.setDuration(233);
        alphaAnimator.start();
        Animator scaleAnimatorX = ObjectAnimator.ofFloat(this.mImageView, View.SCALE_X, new float[]{1.2f, 1.3f});
        scaleAnimatorX.setDuration(133);
        ((ObjectAnimator) scaleAnimatorX).setRepeatMode(2);
        ((ObjectAnimator) scaleAnimatorX).setRepeatCount(1);
        scaleAnimatorX.start();
        Animator scaleAnimatorY = ObjectAnimator.ofFloat(this.mImageView, View.SCALE_Y, new float[]{1.2f, 1.3f});
        scaleAnimatorY.setDuration(133);
        ((ObjectAnimator) scaleAnimatorY).setRepeatMode(2);
        ((ObjectAnimator) scaleAnimatorY).setRepeatCount(1);
        scaleAnimatorY.start();
        animateScale(false);
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_CANCEL_DROP_ITEM, "Cancel", -1, false);
        if (dragObject.dragSource.getDragSourceType() == 5 && (dragObject.dragInfo instanceof PendingAddWidgetInfo)) {
            LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) ((PendingAddWidgetInfo) dragObject.dragInfo).getProviderInfo();
            SALogging.getInstance().insertCancelAddWidgetLog();
        }
    }

    public final boolean acceptDrop(DragObject dragObject) {
        return this.mActive;
    }

    public boolean isDropEnabled(boolean isDrop) {
        if (isDrop) {
            return this.mActive;
        }
        return true;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        super.getHitRect(outRect);
        int[] coords = new int[2];
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, coords);
        outRect.offsetTo(coords[0], coords[1]);
    }

    public int getOutlineColor() {
        return this.mLauncher.getOutlineColor();
    }

    public View getTargetView() {
        return this;
    }

    public void onClick(View v) {
    }

    private void animateCircleView(boolean visible) {
        final ImageView imageView = this.mCircleView;
        if (visible) {
            AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    imageView.setAlpha(1.0f);
                }
            };
            Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.drop_target_cancel_circle_show);
            animator.addListener(listener);
            animator.setTarget(imageView);
            animator.setDuration(233);
            animator.start();
            return;
        }
        listener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                imageView.setAlpha(0.0f);
            }
        };
        animator = AnimatorInflater.loadAnimator(getContext(), R.animator.drop_target_cancel_circle_hide);
        animator.addListener(listener);
        animator.setTarget(imageView);
        animator.setDuration(233);
        animator.start();
    }

    void animateCancelDropTarget() {
        if (this.mEnable && !this.mActive) {
            this.mActive = true;
            animateScale(true);
        }
    }

    private void animateScale(boolean visible) {
        float fromValue;
        float toValue;
        AnimatorSet anim = new AnimatorSet();
        if (visible) {
            setCancelDropTargetVisible(true);
            fromValue = 0.0f;
            toValue = 1.0f;
        } else {
            fromValue = 1.0f;
            toValue = 0.0f;
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CancelDropTarget.this.setCancelDropTargetVisible(false);
                }
            });
        }
        anim.setDuration(175);
        r4 = new Animator[2];
        r4[0] = ObjectAnimator.ofFloat(this, "scaleX", new float[]{fromValue, toValue});
        r4[1] = ObjectAnimator.ofFloat(this, "scaleY", new float[]{fromValue, toValue});
        anim.playTogether(r4);
        anim.start();
    }

    private void setCancelDropTargetVisible(boolean isVisible) {
        int i;
        int i2 = 0;
        setVisibility(isVisible ? 0 : 4);
        ImageView imageView = this.mImageView;
        if (isVisible) {
            i = 0;
        } else {
            i = 4;
        }
        imageView.setVisibility(i);
        TextView textView = this.mTextView;
        if (!isVisible) {
            i2 = 4;
        }
        textView.setVisibility(i2);
        this.mCircleView.setVisibility(4);
        setDefaultState();
    }

    private void setDefaultState() {
        this.mTextView.setAlpha(1.0f);
        this.mImageView.setTranslationY(0.0f);
        this.mImageView.setScaleX(1.0f);
        this.mImageView.setScaleY(1.0f);
        setScaleX(1.0f);
        setScaleY(1.0f);
    }

    private Rect getIconRect(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(this.mImageView, to);
        int width = drawableWidth;
        int height = drawableHeight;
        int left = to.left + getPaddingLeft();
        int top = to.top + ((getMeasuredHeight() - height) / 2);
        to.set(left, top, left + width, top + height);
        to.offset((-(viewWidth - width)) / 2, (-(viewHeight - height)) / 2);
        return to;
    }

    void setEnable(boolean isEnable) {
        this.mEnable = isEnable;
    }

    void changeColorForBg(boolean whiteBg) {
        int color;
        if (whiteBg) {
            color = ContextCompat.getColor(getContext(), R.color.text_color_dark);
        } else {
            color = ContextCompat.getColor(getContext(), R.color.text_color);
        }
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mImageView, whiteBg);
        this.mTextView.setTextColor(color);
    }

    void onConfigurationChangedIfNeeded(final int size) {
        final DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (LauncherFeature.isTablet() || !dp.isLandscape) {
            this.mCancelLayout.setOrientation(1);
        } else {
            this.mCancelLayout.setOrientation(0);
        }
        final int imageSize = getResources().getDimensionPixelSize(R.dimen.drop_target_cancel_image_size);
        LayoutParams lp = this.mImageView.getLayoutParams();
        lp.height = imageSize;
        lp.width = imageSize;
        this.mImageView.setLayoutParams(lp);
        this.mImageView.post(new Runnable() {
            public void run() {
                LayoutParams lp = CancelDropTarget.this.mCircleView.getLayoutParams();
                lp.height = size;
                boolean useRectangleImg = !LauncherFeature.isTablet() && dp.isLandscape;
                lp.width = useRectangleImg ? (imageSize + CancelDropTarget.this.mTextView.getMeasuredWidth()) + CancelDropTarget.this.getResources().getDimensionPixelSize(R.dimen.drop_target_cancel_circle_margin) : size;
                CancelDropTarget.this.mCircleView.setLayoutParams(lp);
                CancelDropTarget.this.mCircleView.setImageDrawable(CancelDropTarget.this.getResources().getDrawable(useRectangleImg ? R.drawable.drop_target_cancel_circle_red_land : R.drawable.drop_target_cancel_circle_red, null));
            }
        });
        int padding = getResources().getDimensionPixelSize(R.dimen.drop_target_cancel_text_padding);
        this.mTextView.setPadding(padding, getResources().getDimensionPixelSize(R.dimen.drop_target_cancel_text_padding_top), padding, 0);
        this.mTextView.setTextSize(0, getResources().getDimension(R.dimen.drop_target_cancel_text_size));
    }
}
