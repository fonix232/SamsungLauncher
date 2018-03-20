package com.android.launcher3.common.quickoption;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.PropertyListBuilder;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager.DragListener;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.quickoption.notifications.NotificationItemView;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.graphics.TriangleShape;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ElasticEaseOut;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickOptionView extends LinearLayout implements DragSource, DragListener {
    private static final int REMOVE_NOTIFICATION_FADE_DURATION = 213;
    private static final int REMOVE_NOTIFICATION_SCALE_DURATION = 433;
    private static final int STATE_REMOVED = 1;
    public static final int STATE_SHOWING = 0;
    private static final String TAG = "QuickOptionListAdapter";
    private final int APPSHORTCUT_ANIM_START_DELAY = 100;
    private final int QUICK_OPTION_ANIM_HIDE_DELAY = 167;
    private final int QUICK_OPTION_ANIM_HIDE_DURATION = 200;
    private final int QUICK_OPTION_ANIM_SHOW_ALPHA_DURATION = 200;
    private final int QUICK_OPTION_ANIM_SHOW_SCALE_DURATION = CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT;
    private final float QUICK_OPTION_BG_ALPHA = 0.95f;
    private Rect mAnchor;
    private AppShortcutItemView mAppShortcutItemView;
    private AnimatorSet mAppShortcutShowAnimSet;
    private View mArrow;
    private int mCurrentNavigationBarPosition;
    private final Interpolator mCustomOptical = new PathInterpolator(0.17f, 0.17f, 0.83f, 0.83f);
    private float mDx;
    private float mDy;
    private boolean mEnableFooter = false;
    private final int mGapSize;
    private GlobalOptionItemView mGlobalOptionItemView;
    private boolean mHasDeepShortcut = false;
    private boolean mHasNotifications = false;
    private boolean mIsAboveIcon;
    private final boolean mIsLandscape;
    private final boolean mIsMultiWindow;
    private final boolean mIsRtl;
    private ItemInfo mItemInfo;
    private String mItemPackageName = null;
    private final OnKeyListener mKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            switch (keyCode) {
                case 19:
                case 20:
                case 66:
                    if (v instanceof GlobalOption) {
                        v.setNextFocusUpId(v.getId());
                        if (QuickOptionView.this.mAppShortcutItemView != null) {
                            v.setNextFocusDownId(QuickOptionView.this.mAppShortcutItemView.getId());
                            return false;
                        } else if (QuickOptionView.this.mNotificationItemView != null) {
                            v.setNextFocusDownId(QuickOptionView.this.mNotificationItemView.getId());
                            return false;
                        } else {
                            v.setNextFocusDownId(v.getId());
                            return false;
                        }
                    } else if (v instanceof AppShortcut) {
                        v.setNextFocusUpId(QuickOptionView.this.mGlobalOptionItemView.getId());
                        if (QuickOptionView.this.mNotificationItemView != null) {
                            v.setNextFocusDownId(QuickOptionView.this.mNotificationItemView.getId());
                            return false;
                        }
                        v.setNextFocusDownId(v.getId());
                        return false;
                    } else if (!(v instanceof NotificationItemView)) {
                        return false;
                    } else {
                        if (QuickOptionView.this.mAppShortcutItemView != null) {
                            v.setNextFocusUpId(QuickOptionView.this.mAppShortcutItemView.getId());
                        } else {
                            v.setNextFocusUpId(QuickOptionView.this.mGlobalOptionItemView.getId());
                        }
                        v.setNextFocusDownId(v.getId());
                        return false;
                    }
                case 23:
                    return false;
                default:
                    QuickOptionView.this.remove(true);
                    return true;
            }
        }
    };
    private final Point mLastTouchPos = new Point();
    private final Launcher mLauncher;
    private NotificationItemView mNotificationItemView;
    private int mNotificationSize = 0;
    private final QuickOptionManager mQuickOptionManager;
    private int mQuickOptionSize = 0;
    private AnimatorSet mReduceHeightAnimatorSet = null;
    private AnimatorSet mShowAnim;
    private final Interpolator mSineInOut90 = ViInterpolator.getInterploator(35);
    private int mState = 1;

    public QuickOptionView(Context context, AttributeSet attrs) {
        int dimensionPixelSize;
        super(context, attrs);
        this.mLauncher = (Launcher) context;
        this.mQuickOptionManager = this.mLauncher.getQuickOptionManager();
        this.mIsRtl = Utilities.sIsRtl;
        this.mIsLandscape = this.mLauncher.getDeviceProfile().isLandscape;
        this.mIsMultiWindow = this.mLauncher.getDeviceProfile().isMultiwindowMode;
        this.mDy = 0.0f;
        this.mDx = 0.0f;
        if (!this.mIsMultiWindow || this.mIsLandscape) {
            dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.quick_options_container_gap);
        } else {
            dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.quick_options_container_small_gap);
        }
        this.mGapSize = dimensionPixelSize;
    }

    public void show(DragObject dragObject, int optionFlag, List<String> appShortcutIds) {
        boolean z = true;
        if (dragObject != null && this.mQuickOptionManager != null) {
            LayoutInflater inflater = this.mLauncher.getLayoutInflater();
            if (!(appShortcutIds == null || appShortcutIds.isEmpty())) {
                this.mHasDeepShortcut = true;
            }
            this.mState = 0;
            this.mItemInfo = (ItemInfo) dragObject.dragInfo;
            this.mAnchor = this.mQuickOptionManager.getAnchorRect();
            if (this.mQuickOptionManager.getAnchorView() instanceof IconView) {
                ((IconView) this.mQuickOptionManager.getAnchorView()).setIsBadgeHidden(true);
            }
            this.mCurrentNavigationBarPosition = Utilities.getNavigationBarPositon();
            createGlobalOptionsContainer(inflater, dragObject, optionFlag);
            List<NotificationKeyData> notificationKeys = null;
            if (Utilities.getNotificationPreviewEnable(this.mLauncher)) {
                notificationKeys = this.mLauncher.getLauncherModel().getNotificationKeysForItem(this.mItemInfo);
                this.mNotificationSize = notificationKeys.size();
                if (this.mNotificationSize > 0) {
                    boolean z2;
                    this.mHasNotifications = true;
                    if (this.mNotificationSize > 1) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    this.mEnableFooter = z2;
                }
            }
            if (this.mHasDeepShortcut) {
                createAppShortcutsContainer(inflater, appShortcutIds);
                setQuickOptionSize(this.mGlobalOptionItemView.getOptionSize() + this.mAppShortcutItemView.getAppShortcutListSize());
            } else {
                setQuickOptionSize(this.mGlobalOptionItemView.getOptionSize());
            }
            if (this.mHasNotifications) {
                createNotificationsContainer(inflater, dragObject, notificationKeys);
            }
            this.mLauncher.getDragLayer().addView(this);
            this.mLauncher.getDragMgr().addDragListener(this);
            shiftPopup();
            if (!(this.mIsAboveIcon && this.mHasNotifications)) {
                z = false;
            }
            setArrowBG(z);
            animateShow();
            Log.d(TAG, "QuickOption STATE_SHOWING : mHasNotifications = " + this.mHasNotifications + " mHasDeepShortcut = " + this.mHasDeepShortcut);
        }
    }

    private void createGlobalOptionsContainer(LayoutInflater inflater, DragObject dragObject, int optionFlag) {
        this.mGlobalOptionItemView = (GlobalOptionItemView) inflater.inflate(R.layout.global_option_container, this, false);
        List<QuickOptionListItem> globalOptions = this.mQuickOptionManager.getOptions(dragObject, optionFlag);
        this.mGlobalOptionItemView.setOnQuickOptionKeyListener(this.mKeyListener);
        this.mGlobalOptionItemView.createContainerItems(this.mLauncher, inflater, globalOptions, this.mHasDeepShortcut, this.mIsLandscape, this.mItemInfo instanceof LauncherAppWidgetInfo);
        this.mGlobalOptionItemView.getBackground().setAlpha(242);
        addView(this.mGlobalOptionItemView);
    }

    private void createAppShortcutsContainer(LayoutInflater inflater, List<String> appShortcutIds) {
        this.mAppShortcutItemView = (AppShortcutItemView) inflater.inflate(R.layout.app_shortcut_container, this, false);
        this.mAppShortcutItemView.setOnQuickOptionKeyListener(this.mKeyListener);
        this.mAppShortcutItemView.createContainerItems(this.mLauncher, inflater, this.mItemInfo, appShortcutIds, this.mHasNotifications);
        this.mAppShortcutItemView.getBackground().setAlpha(242);
        LayoutParams lp = (LayoutParams) this.mAppShortcutItemView.getLayoutParams();
        lp.topMargin = this.mGapSize;
        addView(this.mAppShortcutItemView, lp);
    }

    private void createNotificationsContainer(LayoutInflater inflater, DragObject dragObject, List<NotificationKeyData> notificationKeys) {
        this.mNotificationItemView = (NotificationItemView) inflater.inflate(R.layout.notification, this, false);
        this.mNotificationItemView.setOnQuickOptionKeyListener(this.mKeyListener);
        this.mNotificationItemView.show(dragObject, notificationKeys);
        LayoutParams lp = new LayoutParams(getResources().getDimensionPixelSize(R.dimen.quick_option_item_width), this.mNotificationItemView.getPopupHeight(this.mEnableFooter));
        lp.topMargin = this.mGapSize;
        addView(this.mNotificationItemView, lp);
    }

    public void trimNotifications(Map<PackageUserKey, BadgeInfo> updatedBadges) {
        if (this.mNotificationItemView != null && (this.mItemInfo instanceof IconInfo)) {
            BadgeInfo badgeInfo = (BadgeInfo) updatedBadges.get(PackageUserKey.fromItemInfo(this.mItemInfo));
            if (badgeInfo == null || badgeInfo.getNotificationKeys().size() == 0) {
                removeNotificationView();
            } else {
                this.mNotificationItemView.trimNotifications(NotificationKeyData.extractKeysOnly(badgeInfo.getNotificationKeys()));
            }
        }
    }

    public void removeNotificationView() {
        float f = 0.0f;
        if (this.mHasNotifications) {
            this.mHasNotifications = false;
            AnimatorSet removeNotification = LauncherAnimUtils.createAnimatorSet();
            removeNotification.play(reduceNotificationViewHeight(false));
            Animator scale = LauncherAnimUtils.ofPropertyValuesHolder(this.mNotificationItemView, new PropertyListBuilder().scale(0.0f).build());
            scale.setDuration(433);
            scale.setInterpolator(this.mSineInOut90);
            scale.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (QuickOptionView.this.mNotificationItemView != null) {
                        QuickOptionView.this.removeView(QuickOptionView.this.mNotificationItemView);
                        QuickOptionView.this.mNotificationItemView = null;
                        if (QuickOptionView.this.mItemPackageName != null) {
                            SALogging.getInstance().insertNotiPreviewEventLog(QuickOptionView.this.mItemPackageName, R.string.event_DismissNotification, QuickOptionView.this.mLauncher);
                        }
                    }
                }
            });
            ObjectAnimator alpha = LauncherAnimUtils.ofFloat(this.mNotificationItemView, View.ALPHA.getName(), 0.0f);
            alpha.setDuration(213);
            alpha.setInterpolator(this.mCustomOptical);
            this.mNotificationItemView.setPivotX(this.mIsLandscape ? 0.0f : this.mDx);
            NotificationItemView notificationItemView = this.mNotificationItemView;
            if (this.mIsAboveIcon) {
                f = (float) this.mNotificationItemView.getPopupHeight(this.mEnableFooter);
            }
            notificationItemView.setPivotY(f);
            removeNotification.playTogether(new Animator[]{scale, alpha});
            removeNotification.start();
        }
    }

    private ObjectAnimator createArrowScaleAnim(float scale) {
        return LauncherAnimUtils.ofPropertyValuesHolder(this.mArrow, new PropertyListBuilder().scale(scale).build());
    }

    private ObjectAnimator reduceQuickptionViewHeight(View view, long duration, int height, AnimatorListener listener) {
        ObjectAnimator reduceAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y.getName(), new float[]{view.getTranslationY() + ((float) height)}).setDuration(duration);
        if (listener != null) {
            reduceAnimator.addListener(listener);
        }
        reduceAnimator.setInterpolator(this.mSineInOut90);
        return reduceAnimator;
    }

    public Animator reduceNotificationViewHeight(final boolean reduceFooter) {
        int height;
        if (this.mReduceHeightAnimatorSet != null) {
            this.mReduceHeightAnimatorSet.cancel();
        }
        this.mReduceHeightAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        if (reduceFooter) {
            height = getResources().getDimensionPixelSize(R.dimen.notification_footer_height) + getResources().getDimensionPixelSize(R.dimen.popup_item_divider_height);
        } else {
            height = this.mNotificationItemView.getPopupHeight(this.mEnableFooter) + this.mGapSize;
        }
        int duration = reduceFooter ? CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT : REMOVE_NOTIFICATION_SCALE_DURATION;
        if (this.mNotificationItemView != null && this.mIsAboveIcon) {
            PropertyResetListener<View, Float> resetTranslationYListener = new PropertyResetListener(TRANSLATION_Y, Float.valueOf(0.0f));
            this.mReduceHeightAnimatorSet.play(reduceQuickptionViewHeight(this.mGlobalOptionItemView, (long) duration, height, resetTranslationYListener));
            if (this.mAppShortcutItemView != null) {
                this.mReduceHeightAnimatorSet.play(reduceQuickptionViewHeight(this.mAppShortcutItemView, (long) duration, height, resetTranslationYListener));
            }
            if (reduceFooter) {
                this.mReduceHeightAnimatorSet.play(this.mNotificationItemView.animateHeightRemoval());
                this.mReduceHeightAnimatorSet.play(reduceQuickptionViewHeight(this.mNotificationItemView, (long) duration, height, resetTranslationYListener));
            }
            this.mReduceHeightAnimatorSet.setInterpolator(this.mSineInOut90);
            long arrowScaleDuration = (long) getResources().getInteger(R.integer.config_deepShortcutArrowOpenDuration);
            Animator hideArrow = createArrowScaleAnim(0.0f).setDuration(arrowScaleDuration);
            hideArrow.setStartDelay(0);
            if (!reduceFooter) {
                hideArrow.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        QuickOptionView.this.setArrowBG(false);
                    }
                });
            }
            createArrowScaleAnim(1.0f).setDuration(arrowScaleDuration).setStartDelay(((long) duration) - (2 * arrowScaleDuration));
            this.mReduceHeightAnimatorSet.playSequentially(new Animator[]{hideArrow, showArrow});
        }
        this.mReduceHeightAnimatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (QuickOptionView.this.mNotificationItemView != null && reduceFooter) {
                    QuickOptionView.this.mEnableFooter = false;
                    ((LayoutParams) QuickOptionView.this.mNotificationItemView.getLayoutParams()).height = QuickOptionView.this.mNotificationItemView.getPopupHeight(QuickOptionView.this.mEnableFooter);
                }
                if (QuickOptionView.this.mIsAboveIcon) {
                    QuickOptionView.this.setTranslationY(QuickOptionView.this.getTranslationY() + ((float) height));
                    QuickOptionView.this.setPivotY(QuickOptionView.this.getPivotY() - ((float) height));
                }
                QuickOptionView.this.mReduceHeightAnimatorSet = null;
            }
        });
        return this.mReduceHeightAnimatorSet;
    }

    public ItemInfo getItemInfo() {
        return this.mItemInfo;
    }

    public void remove(boolean animate) {
        if (this.mState != 1) {
            this.mState = 1;
            Log.d(TAG, "QuickOption STATE_REMOVED");
            if (this.mShowAnim != null) {
                this.mShowAnim.cancel();
            }
            this.mQuickOptionManager.clearItemBounceAnimation();
            if (this.mQuickOptionManager.getAnchorView() instanceof IconView) {
                IconView anchorView = (IconView) this.mQuickOptionManager.getAnchorView();
                anchorView.setIsBadgeHidden(false);
                anchorView.refreshBadge();
            }
            if (animate) {
                final QuickOptionView view = this;
                animateHide(new Runnable() {
                    public void run() {
                        QuickOptionView.this.mLauncher.getDragLayer().removeViewInLayout(view);
                        if (QuickOptionView.this.mLauncher.getDragMgr() != null) {
                            QuickOptionView.this.mLauncher.getDragMgr().removeDragListener(view);
                        }
                    }
                });
            } else {
                this.mLauncher.getDragLayer().removeViewInLayout(this);
                if (this.mLauncher.getDragMgr() != null) {
                    this.mLauncher.getDragMgr().removeDragListener(this);
                }
            }
            this.mItemInfo = null;
        }
    }

    private void shiftPopup() {
        if (this.mItemInfo != null) {
            int screenWidth = this.mLauncher.getDragLayer().getWidth();
            int screenHeight = this.mLauncher.getDragLayer().getHeight();
            int containerPadding = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.quick_option_container_padding);
            int popupWidth = getPopupWidth();
            int popupHeight = getPopupHeight(this.mEnableFooter);
            int arrowWidth = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.quick_option_popup_arrow_width);
            int arrowHeight = getArrowHeight();
            int popupEdgeMargin = getEdgeMargin(this.mItemInfo.container, arrowWidth);
            int popupTopMargin = getResources().getDimensionPixelSize(this.mItemInfo instanceof LauncherAppWidgetInfo ? R.dimen.quick_option_popup_margin_widget : R.dimen.quick_option_popup_margin);
            int statusBarHeight = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            boolean isHorizontalAlign = this.mAnchor.top < (popupHeight + statusBarHeight) + arrowHeight && screenHeight < (this.mAnchor.bottom + popupHeight) + arrowHeight;
            if (this.mIsLandscape && isHorizontalAlign) {
                setAlignHorizontal(popupWidth, popupHeight, popupEdgeMargin, screenHeight, statusBarHeight);
            } else {
                setAlignVertical(popupWidth, popupHeight + getArrowHeight(), popupEdgeMargin, screenWidth, screenHeight, popupTopMargin, statusBarHeight, containerPadding);
                if (this.mArrow == null) {
                    this.mArrow = addArrowView(arrowWidth, arrowHeight);
                }
                float arrowDx = ((((float) this.mAnchor.left) + (((float) this.mAnchor.width()) / 2.0f)) - this.mDx) - (((float) arrowWidth) / 2.0f);
                if (this.mIsRtl) {
                    arrowDx -= (float) (popupWidth - arrowWidth);
                }
                this.mArrow.setTranslationX(arrowDx);
            }
            if (this.mIsRtl) {
                this.mDx -= (float) ((screenWidth - popupWidth) - containerPadding);
            } else {
                this.mDx -= (float) containerPadding;
            }
            setX(this.mDx);
            setY(this.mDy);
        }
    }

    public void navigationBarPositionChanged() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (this.mCurrentNavigationBarPosition != 0 && this.mCurrentNavigationBarPosition != Utilities.getNavigationBarPositon() && !this.mIsMultiWindow) {
            this.mCurrentNavigationBarPosition = Utilities.getNavigationBarPositon();
            this.mDx += (float) (Utilities.getNavigationBarPositon() != 1 ? dp.navigationBarHeight * -1 : dp.navigationBarHeight);
            setX(this.mDx);
        }
    }

    private void setAlignVertical(int popupWidth, int popupHeight, int popupEdgeMargin, int screenWidth, int screenHeight, int popupTopMargin, int statusBarHeight, int containerPadding) {
        boolean z = false;
        this.mDx = (((float) this.mAnchor.left) + (((float) this.mAnchor.width()) / 2.0f)) - (((float) popupWidth) / 2.0f);
        if (this.mDx < ((float) popupEdgeMargin)) {
            this.mDx = (float) popupEdgeMargin;
            setPivotX((((float) this.mAnchor.left) + (((float) this.mAnchor.width()) / 2.0f)) - ((float) popupEdgeMargin));
        } else if (this.mDx + ((float) popupWidth) > ((float) (screenWidth - popupEdgeMargin))) {
            this.mDx = (float) ((screenWidth - popupWidth) - popupEdgeMargin);
            setPivotX(((float) (popupWidth - ((screenWidth - this.mAnchor.right) - popupEdgeMargin))) - (((float) this.mAnchor.width()) / 2.0f));
        } else {
            setPivotX(((float) popupWidth) / 2.0f);
        }
        this.mDy = (float) ((this.mAnchor.top - popupHeight) - popupTopMargin);
        float f = this.mDy;
        if (this.mIsMultiWindow && !this.mIsLandscape) {
            statusBarHeight = 0;
        }
        if (f > ((float) statusBarHeight)) {
            z = true;
        }
        this.mIsAboveIcon = z;
        if (this.mIsAboveIcon) {
            setPivotY((float) (popupHeight - getArrowHeight()));
        } else {
            this.mDy = (float) (this.mAnchor.bottom + popupTopMargin);
            if (this.mDy + ((float) popupHeight) > ((float) screenHeight)) {
                this.mDy = (float) ((screenHeight - popupHeight) - popupTopMargin);
            }
            setPivotY((float) getArrowHeight());
        }
        this.mDy -= (float) containerPadding;
    }

    private void setAlignHorizontal(int popupWidth, int popupHeight, int popupEdgeMargin, int screenHeight, int statusBarHeight) {
        int gapSize = getIconSize(this.mLauncher.getDeviceProfile()) / 2;
        this.mDx = (float) ((this.mAnchor.left - popupWidth) - (gapSize / 2));
        if (this.mDx < ((float) popupEdgeMargin)) {
            this.mDx = (float) (this.mAnchor.right + gapSize);
            setPivotX(0.0f);
        } else {
            setPivotX((float) popupWidth);
        }
        this.mDy = (((float) this.mAnchor.top) + (((float) this.mAnchor.height()) / 2.0f)) - (((float) popupHeight) / 2.0f);
        if (this.mDy < ((float) statusBarHeight)) {
            this.mDy = (float) statusBarHeight;
            setPivotY((((float) this.mAnchor.top) + (((float) this.mAnchor.height()) / 2.0f)) - ((float) statusBarHeight));
        } else if (this.mAnchor.bottom >= statusBarHeight + popupHeight) {
            this.mDy = (float) (this.mAnchor.bottom - popupHeight);
            setPivotY(((float) popupHeight) - (((float) this.mAnchor.height()) / 2.0f));
        } else if (this.mDy + ((float) popupHeight) > ((float) (screenHeight - statusBarHeight))) {
            this.mDy = (float) ((screenHeight - popupHeight) - statusBarHeight);
            setPivotY((((float) this.mAnchor.top) - this.mDy) + (((float) this.mAnchor.height()) / 2.0f));
        } else {
            setPivotY(((float) popupHeight) / 2.0f);
        }
    }

    private int getIconSize(DeviceProfile dp) {
        int itemContainer = (int) this.mItemInfo.container;
        if (itemContainer == -100) {
            return dp.homeGrid.getIconSize();
        }
        if (itemContainer == Favorites.CONTAINER_HOTSEAT) {
            return dp.hotseatGridIcon.getIconSize();
        }
        if (itemContainer == Favorites.CONTAINER_APPS) {
            return dp.appsGrid.getIconSize();
        }
        if (itemContainer > 0) {
            return dp.folderGrid.getIconSize();
        }
        return 0;
    }

    private View addArrowView(int width, int height) {
        boolean z;
        int i = 1;
        int viewIndex = 0;
        LayoutParams layoutParams = new LayoutParams(width, height);
        View arrowView = new View(this.mLauncher);
        float f = (float) width;
        float f2 = (float) height;
        if (this.mIsAboveIcon) {
            z = false;
        } else {
            z = true;
        }
        arrowView.setBackground(new ShapeDrawable(TriangleShape.create(f, f2, z)));
        arrowView.setElevation(getElevation());
        if (this.mIsAboveIcon) {
            int i2;
            if (this.mHasNotifications) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (!this.mHasDeepShortcut) {
                i = 0;
            }
            viewIndex = (i2 + i) + 1;
        }
        addView(arrowView, viewIndex, layoutParams);
        return arrowView;
    }

    private void setArrowBG(boolean isAttachNotification) {
        if (this.mArrow != null) {
            this.mArrow.getBackground().setColorFilter(ContextCompat.getColor(this.mLauncher, R.color.quick_options_popup_color), Mode.SRC_IN);
            if (!isAttachNotification) {
                this.mArrow.setAlpha(0.95f);
            }
        }
    }

    private int getEdgeMargin(long itemContainer, int arrowWidth) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        int edgeMargin = 0;
        int iconSize = 0;
        if (itemContainer == -100) {
            edgeMargin = dp.homeGrid.getPagePadding();
            iconSize = dp.homeGrid.getIconSize();
            if (!this.mIsLandscape) {
                edgeMargin += (dp.homeGrid.getCellWidth() - iconSize) / 2;
            }
        } else if (itemContainer == -101) {
            edgeMargin = dp.homeGrid.getPagePadding();
            iconSize = dp.hotseatGridIcon.getIconSize();
            if (!this.mIsLandscape) {
                edgeMargin += (dp.getHotseatCellWidthSize() - iconSize) / 2;
            }
        } else if (itemContainer == -102) {
            edgeMargin = dp.appsGrid.getPagePadding();
            iconSize = dp.appsGrid.getIconSize();
            if (!this.mIsLandscape) {
                edgeMargin += (dp.appsGrid.getCellWidth() - iconSize) / 2;
            }
        } else if (itemContainer > 0) {
            edgeMargin = dp.folderGrid.getPagePadding();
            iconSize = dp.folderGrid.getIconSize();
        }
        if (this.mIsLandscape) {
            return edgeMargin + (((iconSize / 2) - (arrowWidth / 2)) - getResources().getDimensionPixelSize(R.dimen.quick_option_popup_radius));
        }
        return edgeMargin;
    }

    private int getPopupHeight(boolean enableFooter) {
        int appShortcutHeight;
        int notificationHeight;
        int globalOptionHeight = this.mGlobalOptionItemView.getPopupHeight();
        if (this.mHasDeepShortcut) {
            appShortcutHeight = this.mAppShortcutItemView.getPopupHeight() + this.mGapSize;
        } else {
            appShortcutHeight = 0;
        }
        if (this.mHasNotifications) {
            notificationHeight = this.mNotificationItemView.getPopupHeight(enableFooter) + this.mGapSize;
        } else {
            notificationHeight = 0;
        }
        return (globalOptionHeight + appShortcutHeight) + notificationHeight;
    }

    private int getPopupWidth() {
        return this.mLauncher.getResources().getDimensionPixelSize(R.dimen.quick_option_item_width);
    }

    private int getArrowHeight() {
        return getResources().getDimensionPixelSize(R.dimen.quick_option_popup_arrow_height);
    }

    private void animateShow() {
        setScaleX(0.2f);
        setScaleY(0.2f);
        setAlpha(0.0f);
        Animator scale = LauncherAnimUtils.ofPropertyValuesHolder(this, new PropertyListBuilder().scale(1.0f).build());
        scale.setDuration(300);
        scale.setInterpolator(new ElasticEaseOut(1.0f, 1.4f));
        ObjectAnimator alpha = LauncherAnimUtils.ofFloat(this, View.ALPHA.getName(), 1.0f);
        alpha.setDuration(200);
        alpha.setInterpolator(this.mSineInOut90);
        this.mShowAnim = new AnimatorSet();
        this.mShowAnim.playTogether(new Animator[]{scale, alpha});
        this.mShowAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                QuickOptionView.this.setScaleX(1.0f);
                QuickOptionView.this.setScaleY(1.0f);
                QuickOptionView.this.setAlpha(1.0f);
                QuickOptionView.this.mShowAnim = null;
                QuickOptionView.this.sayQuickOptionAccessibility();
            }
        });
        if (this.mAppShortcutItemView != null) {
            this.mAppShortcutShowAnimSet = this.mAppShortcutItemView.getItemShowAnim();
            this.mAppShortcutShowAnimSet.setStartDelay(100);
            this.mAppShortcutShowAnimSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    QuickOptionView.this.mAppShortcutShowAnimSet = null;
                }
            });
        }
        post(new Runnable() {
            public void run() {
                QuickOptionView.this.mShowAnim.start();
                if (QuickOptionView.this.mAppShortcutShowAnimSet != null) {
                    QuickOptionView.this.mAppShortcutShowAnimSet.start();
                }
            }
        });
    }

    private void animateHide(final Runnable onCompleteRunnable) {
        if (this.mShowAnim != null && this.mShowAnim.isRunning()) {
            this.mShowAnim.cancel();
            this.mShowAnim = null;
        }
        Animator hideAnim = LauncherAnimUtils.ofPropertyValuesHolder(this, new PropertyListBuilder().scale(0.5f).alpha(0.0f).build());
        hideAnim.setDuration(200);
        hideAnim.setInterpolator(this.mSineInOut90);
        hideAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                QuickOptionView.this.postDelayed(new Runnable() {
                    public void run() {
                        if (QuickOptionView.this.mQuickOptionManager.getItemRemoveAnimation() != null) {
                            if (QuickOptionView.this.mLauncher.getHomeController().getState() == 3) {
                                QuickOptionView.this.mLauncher.getHomeController().exitResizeState(false);
                            }
                            QuickOptionView.this.mQuickOptionManager.getItemRemoveAnimation().animate();
                            QuickOptionView.this.mQuickOptionManager.clearItemRemoveAnimation();
                        }
                    }
                }, 167);
                onCompleteRunnable.run();
            }

            public void onAnimationCancel(Animator animator) {
                QuickOptionView.this.setScaleX(0.2f);
                QuickOptionView.this.setScaleY(0.2f);
                QuickOptionView.this.setAlpha(0.0f);
            }
        });
        hideAnim.start();
    }

    public int getState() {
        return this.mState;
    }

    boolean onItemLongClick(ShortcutInfoCompat shortcut) {
        IconInfo iconInfo = new IconInfo(shortcut, this.mLauncher);
        IconView iconView = new IconView(this.mLauncher);
        iconView.setTag(iconInfo);
        return beginDragging(iconView);
    }

    private boolean beginDragging(View v) {
        if (v instanceof IconView) {
            Bitmap previewWithoutTray = ((IconInfo) v.getTag()).getIcon(null);
            if (previewWithoutTray == null) {
                return false;
            }
            int iconSize = this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
            Bitmap preview = OpenThemeManager.getInstance().getIconWithTrayIfNeeded(previewWithoutTray, iconSize, false);
            if (preview == null) {
                preview = previewWithoutTray;
            }
            if (preview.getWidth() != iconSize) {
                preview = Bitmap.createScaledBitmap(preview, iconSize, iconSize, true);
            }
            float scale = ((float) this.mLauncher.getDeviceProfile().homeGrid.getIconSize()) / ((float) preview.getWidth());
            Rect bounds = new Rect();
            bounds.left = this.mLastTouchPos.x + ((int) getX());
            bounds.top = (this.mLastTouchPos.y + ((int) getY())) - (BitmapUtils.getIconBitmapSize() / 2);
            this.mLauncher.beginDragFromQuickOptionPopup(v, preview, this, v.getTag(), bounds, scale);
            return true;
        }
        Log.e(TAG, "Unexpected dragging view: " + v);
        return false;
    }

    public boolean isLeftRightKeycodeInGlobalOption(KeyEvent event) {
        return (getFocusedChild() instanceof GlobalOptionItemView) && (event.getKeyCode() == 21 || event.getKeyCode() == 22);
    }

    public ArrayList<View> getAccessibilityFocusChildViewList() {
        ArrayList<View> childList = new ArrayList();
        if (this.mGlobalOptionItemView != null) {
            childList.addAll(this.mGlobalOptionItemView.getAccessibilityFocusChildViewList());
        }
        if (this.mAppShortcutItemView != null) {
            childList.addAll(this.mAppShortcutItemView.getAccessibilityFocusChildViewList());
        }
        if (this.mNotificationItemView != null) {
            childList.addAll(this.mNotificationItemView.getAccessibilityFocusChildViewList(this.mEnableFooter));
        }
        return childList;
    }

    private void sayQuickOptionAccessibility() {
        if (this.mItemInfo != null && this.mState != 1 && Talk.INSTANCE.isAccessibilityEnabled()) {
            String ttsQuickOption;
            String itemTitle = getItemTitle();
            if (this.mNotificationSize == 1) {
                ttsQuickOption = getResources().getString(R.string.quick_option_and_one_notification, new Object[]{Integer.valueOf(getQuickOptionSize()), itemTitle});
            } else if (this.mNotificationSize > 1) {
                ttsQuickOption = getResources().getString(R.string.quick_option_and_more_notification, new Object[]{Integer.valueOf(getQuickOptionSize()), Integer.valueOf(this.mNotificationSize), itemTitle});
            } else {
                ttsQuickOption = getResources().getString(R.string.quick_option_show, new Object[]{Integer.valueOf(getQuickOptionSize()), itemTitle});
            }
            Talk.INSTANCE.say(ttsQuickOption);
        }
    }

    private String getItemTitle() {
        if (this.mItemInfo instanceof FolderInfo) {
            return String.format(getResources().getString(R.string.folder_name_format), new Object[]{this.mItemInfo.title});
        } else if (this.mItemInfo instanceof LauncherAppWidgetInfo) {
            return ((LauncherAppWidgetInfo) this.mItemInfo).hostView.getContentDescription().toString();
        } else {
            return this.mItemInfo.title.toString();
        }
    }

    public void insertSALoggingEvent() {
        if (this.mItemInfo != null) {
            SALogging.getInstance().insertQuickViewEventLog(this.mItemInfo, this.mLauncher);
            if (this.mHasNotifications && this.mItemInfo.getIntent() != null && this.mItemInfo.getIntent().getComponent() != null) {
                this.mItemPackageName = this.mItemInfo.getIntent().getComponent().getPackageName();
                SALogging.getInstance().insertNotiPreviewEventLog(this.mItemPackageName, R.string.event_ViewMainNotification, this.mLauncher);
                if (this.mEnableFooter) {
                    SALogging.getInstance().insertNotiPreviewEventLog(this.mItemPackageName, R.string.event_ViewSubNotification, this.mLauncher);
                }
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
            case 2:
                this.mLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                break;
        }
        return super.onInterceptTouchEvent(ev);
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
        return 6;
    }

    public int getOutlineColor() {
        return 0;
    }

    public Stage getController() {
        return null;
    }

    public boolean onDragStart(DragSource source, Object info, int dragAction) {
        this.mLauncher.getDragMgr().removeQuickOptionView();
        return true;
    }

    public boolean onDragEnd() {
        return false;
    }

    public int getEmptyCount() {
        return 0;
    }

    public int getQuickOptionSize() {
        return this.mQuickOptionSize;
    }

    private void setQuickOptionSize(int quickOptionSize) {
        this.mQuickOptionSize = quickOptionSize;
    }
}
