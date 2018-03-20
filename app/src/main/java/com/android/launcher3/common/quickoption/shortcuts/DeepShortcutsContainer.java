package com.android.launcher3.common.quickoption.shortcuts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.graphics.TriangleShape;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.animation.LauncherViewPropertyAnimator;
import com.android.launcher3.util.animation.LogAccelerateInterpolator;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TargetApi(25)
public class DeepShortcutsContainer extends LinearLayout implements OnLongClickListener, OnTouchListener, DragSource, DragListener {
    private static DeepShortcutManager mDeepShortcutsManager;
    private View mArrow;
    private boolean mDeferContainerRemoval;
    private IconView mDeferredDragIcon;
    private Point mIconLastTouchPos;
    private final Point mIconShift;
    private boolean mIsAboveIcon;
    private boolean mIsLeftAligned;
    private boolean mIsOpen;
    private final boolean mIsRtl;
    private final Launcher mLauncher;
    private Animator mOpenCloseAnimator;
    private final Rect mTempRect;

    private class UpdateShortcutChild implements Runnable {
        private int mShortcutChildIndex;
        private UnbadgedShortcutInfo mShortcutChildInfo;

        UpdateShortcutChild(int shortcutChildIndex, UnbadgedShortcutInfo shortcutChildInfo) {
            this.mShortcutChildIndex = shortcutChildIndex;
            this.mShortcutChildInfo = shortcutChildInfo;
        }

        public void run() {
            DeepShortcutsContainer.this.getShortcutAt(this.mShortcutChildIndex).applyShortcutInfo(this.mShortcutChildInfo, DeepShortcutsContainer.this);
        }
    }

    static class UnbadgedShortcutInfo extends IconInfo {
        final ShortcutInfoCompat mDetail;

        UnbadgedShortcutInfo(ShortcutInfoCompat shortcutInfo, Context context) {
            super(shortcutInfo, context);
            this.mDetail = shortcutInfo;
        }
    }

    public DeepShortcutsContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mIconShift = new Point();
        this.mTempRect = new Rect();
        this.mIconLastTouchPos = new Point();
        this.mLauncher = (Launcher) context;
        mDeepShortcutsManager = LauncherAppState.getInstance().getShortcutManager();
        this.mIsRtl = Utilities.sIsRtl;
    }

    public DeepShortcutsContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutsContainer(Context context) {
        this(context, null, 0);
    }

    public void populateAndShow(IconView originalIcon, List<String> ids) {
        Resources resources = getResources();
        int arrowWidth = resources.getDimensionPixelSize(R.dimen.deep_shortcuts_arrow_width);
        int arrowHeight = resources.getDimensionPixelSize(R.dimen.deep_shortcuts_arrow_height);
        int arrowHorizontalOffset = resources.getDimensionPixelSize(R.dimen.deep_shortcuts_arrow_horizontal_offset);
        int arrowVerticalOffset = resources.getDimensionPixelSize(R.dimen.deep_shortcuts_arrow_vertical_offset);
        int spacing = getResources().getDimensionPixelSize(R.dimen.deep_shortcuts_spacing);
        LayoutInflater inflater = this.mLauncher.getLayoutInflater();
        int numShortcuts = Math.min(ids.size(), 5);
        for (int i = 0; i < numShortcuts; i++) {
            View shortcut = (DeepShortcutView) inflater.inflate(R.layout.deep_shortcut, this, false);
            if (i < numShortcuts - 1) {
                ((LayoutParams) shortcut.getLayoutParams()).bottomMargin = spacing;
            }
            addView(shortcut);
        }
        measure(0, 0);
        orientAboutIcon(originalIcon, arrowHeight + arrowVerticalOffset);
        this.mArrow = addArrowView(arrowHorizontalOffset, arrowVerticalOffset, arrowWidth, arrowHeight);
        this.mArrow.setPivotX(((float) arrowWidth) / 2.0f);
        this.mArrow.setPivotY(this.mIsAboveIcon ? 0.0f : (float) arrowHeight);
        animateOpen();
        deferDrag(originalIcon);
        Looper workerLooper = LauncherModel.getWorkerLooper();
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        IconInfo originalInfo = (IconInfo) originalIcon.getTag();
        final UserHandleCompat user = originalInfo.user;
        final ComponentName activity = originalInfo.getTargetComponent();
        final List<String> list = ids;
        new Handler(workerLooper).postAtFrontOfQueue(new Runnable() {
            public void run() {
                List<ShortcutInfoCompat> shortcuts = ShortcutFilter.sortAndFilterShortcuts(DeepShortcutsContainer.mDeepShortcutsManager.queryForShortcutsContainer(activity, list, user));
                if (DeepShortcutsContainer.this.mIsAboveIcon) {
                    Collections.reverse(shortcuts);
                }
                for (int i = 0; i < shortcuts.size(); i++) {
                    uiHandler.post(new UpdateShortcutChild(i, new UnbadgedShortcutInfo((ShortcutInfoCompat) shortcuts.get(i), DeepShortcutsContainer.this.mLauncher)));
                }
            }
        });
    }

    public boolean onLongClick(View v) {
        if (v.isInTouchMode() && (v.getParent() instanceof DeepShortcutView) && this.mLauncher.isDraggingEnabled()) {
            this.mDeferContainerRemoval = true;
            DeepShortcutView sv = (DeepShortcutView) v.getParent();
            sv.setWillDrawIcon(false);
            this.mIconShift.x = this.mIconLastTouchPos.x - sv.getIconCenter().x;
            this.mIconShift.y = this.mIconLastTouchPos.y - this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
            this.mLauncher.beginDragShared(sv.getBubbleText(), this, false, false);
            this.mLauncher.closeFolder();
        }
        return false;
    }

    private DeepShortcutView getShortcutAt(int index) {
        if (!this.mIsAboveIcon) {
            index++;
        }
        return (DeepShortcutView) getChildAt(index);
    }

    private int getShortcutCount() {
        return getChildCount() - 1;
    }

    private void animateOpen() {
        setVisibility(View.VISIBLE);
        this.mIsOpen = true;
        Animator shortcutAnims = LauncherAnimUtils.createAnimatorSet();
        int shortcutCount = getShortcutCount();
        long duration = (long) getResources().getInteger(R.integer.config_deepShortcutOpenDuration);
        long arrowScaleDuration = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
        long arrowScaleDelay = duration - arrowScaleDuration;
        long stagger = (long) getResources().getInteger(R.integer.config_deepShortcutOpenStagger);
        TimeInterpolator fadeInterpolator = new LogAccelerateInterpolator(100, 0);
        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        for (int i = 0; i < shortcutCount; i++) {
            int animationIndex;
            final DeepShortcutView deepShortcutView = getShortcutAt(i);
            deepShortcutView.setVisibility(4);
            deepShortcutView.setAlpha(0.0f);
            Animator anim = deepShortcutView.createOpenAnimation(this.mIsAboveIcon, this.mIsLeftAligned);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    deepShortcutView.setVisibility(View.VISIBLE);
                }
            });
            anim.setDuration(duration);
            if (this.mIsAboveIcon) {
                animationIndex = (shortcutCount - i) - 1;
            } else {
                animationIndex = i;
            }
            anim.setStartDelay(((long) animationIndex) * stagger);
            anim.setInterpolator(interpolator);
            shortcutAnims.play(anim);
            Animator fadeAnim = new LauncherViewPropertyAnimator(deepShortcutView).alpha(1.0f);
            fadeAnim.setInterpolator(fadeInterpolator);
            fadeAnim.setDuration(arrowScaleDelay);
            shortcutAnims.play(fadeAnim);
        }
        shortcutAnims.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                DeepShortcutsContainer.this.mOpenCloseAnimator = null;
            }
        });
        this.mArrow.setScaleX(0.0f);
        this.mArrow.setScaleY(0.0f);
        Animator arrowScale = new LauncherViewPropertyAnimator(this.mArrow).scaleX(1.0f).scaleY(1.0f);
        arrowScale.setStartDelay(arrowScaleDelay);
        arrowScale.setDuration(arrowScaleDuration);
        shortcutAnims.play(arrowScale);
        this.mOpenCloseAnimator = shortcutAnims;
        shortcutAnims.start();
    }

    private void orientAboutIcon(IconView icon, int arrowHeight) {
        int xOffset;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight() + arrowHeight;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        dragLayer.getDescendantRectRelativeToSelf(icon, this.mTempRect);
        Rect insets = dragLayer.getInsets();
        int leftAlignedX = this.mTempRect.left + icon.getPaddingLeft();
        int rightAlignedX = (this.mTempRect.right - width) - icon.getPaddingRight();
        int x = leftAlignedX;
        boolean canBeLeftAligned = leftAlignedX + width < dragLayer.getRight() - insets.right;
        boolean canBeRightAligned = rightAlignedX > dragLayer.getLeft() + insets.left;
        if (!canBeLeftAligned || (this.mIsRtl && canBeRightAligned)) {
            x = rightAlignedX;
        }
        this.mIsLeftAligned = x == leftAlignedX;
        if (this.mIsRtl) {
            x -= dragLayer.getWidth() - width;
        }
        int iconWidth = (int) (((float) ((icon.getWidth() - icon.getPaddingLeft()) - icon.getPaddingRight())) * icon.getScaleX());
        Resources resources = getResources();
        if (isAlignedWithStart()) {
            xOffset = ((iconWidth / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_icon_size) / 2)) - resources.getDimensionPixelSize(R.dimen.deep_shortcut_padding_start);
        } else {
            xOffset = ((iconWidth / 2) - (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) / 2)) - resources.getDimensionPixelSize(R.dimen.deep_shortcut_padding_end);
        }
        if (!this.mIsLeftAligned) {
            xOffset = -xOffset;
        }
        x += xOffset;
        int iconHeight = icon.getIcon().getBounds().height();
        int y = (this.mTempRect.top + icon.getPaddingTop()) - height;
        this.mIsAboveIcon = y > dragLayer.getTop() + insets.top;
        if (!this.mIsAboveIcon) {
            y = (this.mTempRect.top + icon.getPaddingTop()) + iconHeight;
        }
        y -= insets.top;
        setX((float) x);
        setY((float) y);
    }

    private boolean isAlignedWithStart() {
        return (this.mIsLeftAligned && !this.mIsRtl) || (!this.mIsLeftAligned && this.mIsRtl);
    }

    private View addArrowView(int horizontalOffset, int verticalOffset, int width, int height) {
        boolean z;
        int i = 0;
        LayoutParams layoutParams = new LayoutParams(width, height);
        if (this.mIsLeftAligned) {
            layoutParams.gravity = 3;
            layoutParams.leftMargin = horizontalOffset;
        } else {
            layoutParams.gravity = 5;
            layoutParams.rightMargin = horizontalOffset;
        }
        if (this.mIsAboveIcon) {
            layoutParams.topMargin = verticalOffset;
        } else {
            layoutParams.bottomMargin = verticalOffset;
        }
        View arrowView = new View(getContext());
        float f = (float) width;
        float f2 = (float) height;
        if (this.mIsAboveIcon) {
            z = false;
        } else {
            z = true;
        }
        ShapeDrawable arrowDrawable = new ShapeDrawable(TriangleShape.create(f, f2, z));
        arrowDrawable.getPaint().setColor(-1);
        arrowView.setBackground(arrowDrawable);
        arrowView.setElevation(getElevation());
        if (this.mIsAboveIcon) {
            i = getChildCount();
        }
        addView(arrowView, i, layoutParams);
        return arrowView;
    }

    private void deferDrag(IconView originalIcon) {
        this.mDeferredDragIcon = originalIcon;
        this.mLauncher.getDragMgr().addDragListener(this);
    }

    public IconView getDeferredDragIcon() {
        return this.mDeferredDragIcon;
    }

    public boolean onTouch(View v, MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
            case 2:
                this.mIconLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                break;
        }
        return false;
    }

    public int getIntrinsicIconSize() {
        return this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        return 0;
    }

    public void onExtraObjectDragged(ArrayList<DragObject> arrayList) {
    }

    public void onExtraObjectDropCompleted(View target, ArrayList<DragObject> arrayList, ArrayList<DragObject> arrayList2, int fullCnt) {
    }

    public int getPageIndexForDragView(ItemInfo item) {
        return 0;
    }

    public int getDragSourceType() {
        return 0;
    }

    public int getOutlineColor() {
        return 0;
    }

    public Stage getController() {
        return null;
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        animateClose();
        return true;
    }

    public boolean onDragEnd() {
        if (!this.mIsOpen) {
            if (this.mOpenCloseAnimator != null) {
                this.mDeferContainerRemoval = false;
            } else if (this.mDeferContainerRemoval) {
                close();
            }
        }
        this.mDeferredDragIcon.setVisibility(View.VISIBLE);
        return true;
    }

    public void animateClose() {
        if (this.mIsOpen) {
            int i;
            if (this.mOpenCloseAnimator != null) {
                this.mOpenCloseAnimator.cancel();
            }
            this.mIsOpen = false;
            Animator shortcutAnims = LauncherAnimUtils.createAnimatorSet();
            int shortcutCount = getShortcutCount();
            int numOpenShortcuts = 0;
            for (i = 0; i < shortcutCount; i++) {
                if (getShortcutAt(i).isOpenOrOpening()) {
                    numOpenShortcuts++;
                }
            }
            long duration = (long) getResources().getInteger(R.integer.config_deepShortcutCloseDuration);
            long arrowScaleDuration = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
            long stagger = (long) getResources().getInteger(R.integer.config_deepShortcutCloseStagger);
            TimeInterpolator fadeInterpolator = new LogAccelerateInterpolator(100, 0);
            int firstOpenShortcutIndex = this.mIsAboveIcon ? shortcutCount - numOpenShortcuts : 0;
            for (i = firstOpenShortcutIndex; i < firstOpenShortcutIndex + numOpenShortcuts; i++) {
                Animator anim;
                View view = getShortcutAt(i);
                if (view.willDrawIcon()) {
                    int animationIndex;
                    anim = view.createCloseAnimation(this.mIsAboveIcon, this.mIsLeftAligned, duration);
                    if (this.mIsAboveIcon) {
                        animationIndex = i - firstOpenShortcutIndex;
                    } else {
                        animationIndex = (numOpenShortcuts - i) - 1;
                    }
                    anim.setStartDelay(((long) animationIndex) * stagger);
                    Animator fadeAnim = new LauncherViewPropertyAnimator(view).alpha(0.0f);
                    fadeAnim.setStartDelay((((long) animationIndex) * stagger) + arrowScaleDuration);
                    fadeAnim.setDuration(duration - arrowScaleDuration);
                    fadeAnim.setInterpolator(fadeInterpolator);
                    shortcutAnims.play(fadeAnim);
                } else {
                    anim = view.collapseToIcon();
                    anim.setDuration(150);
                    Point iconCenter = view.getIconCenter();
                    view.setPivotX((float) iconCenter.x);
                    view.setPivotY((float) iconCenter.y);
                    LauncherViewPropertyAnimator anim2 = new LauncherViewPropertyAnimator(view).scaleX(1.0f).scaleY(1.0f).translationX((float) this.mIconShift.x).translationY((float) this.mIconShift.y);
                    anim2.setDuration(150);
                    shortcutAnims.play(anim2);
                }
                final View view2 = view;
                anim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        view2.setVisibility(4);
                    }
                });
                shortcutAnims.play(anim);
            }
            Animator arrowAnim = new LauncherViewPropertyAnimator(this.mArrow).scaleX(0.0f).scaleY(0.0f).setDuration(arrowScaleDuration);
            arrowAnim.setStartDelay(0);
            shortcutAnims.play(arrowAnim);
            shortcutAnims.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    DeepShortcutsContainer.this.mOpenCloseAnimator = null;
                    if (DeepShortcutsContainer.this.mDeferContainerRemoval) {
                        DeepShortcutsContainer.this.setVisibility(4);
                    } else {
                        DeepShortcutsContainer.this.close();
                    }
                }
            });
            this.mOpenCloseAnimator = shortcutAnims;
            shortcutAnims.start();
        }
    }

    public void close() {
        if (this.mOpenCloseAnimator != null) {
            this.mOpenCloseAnimator.cancel();
            this.mOpenCloseAnimator = null;
        }
        this.mIsOpen = false;
        this.mDeferContainerRemoval = false;
        this.mLauncher.getDragLayer().removeView(this);
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public static DeepShortcutsContainer showForIcon(IconView icon) {
        if (mDeepShortcutsManager == null) {
            mDeepShortcutsManager = LauncherAppState.getInstance().getShortcutManager();
        }
        Launcher launcher = (Launcher) icon.getContext();
        if (mDeepShortcutsManager.getOpenShortcutsContainer(launcher) != null) {
            icon.clearFocus();
            return null;
        }
        List<String> ids = mDeepShortcutsManager.getShortcutIdsForItem((IconInfo) icon.getTag());
        if (ids.isEmpty()) {
            return null;
        }
        DeepShortcutsContainer container = (DeepShortcutsContainer) launcher.getLayoutInflater().inflate(R.layout.deep_shortcuts_container, launcher.getDragLayer(), false);
        container.setVisibility(4);
        launcher.getDragLayer().addView(container);
        container.populateAndShow(icon, ids);
        return container;
    }

    public int getEmptyCount() {
        return 0;
    }
}
