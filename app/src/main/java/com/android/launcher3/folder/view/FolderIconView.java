package com.android.launcher3.folder.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.common.drawable.PreloadIconDrawable;
import com.android.launcher3.common.model.BadgeCache.CacheKey;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.folder.FolderEventListener;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.FolderStyle;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.AppIconBounceAnimation;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.android.launcher3.util.event.StylusEventHelper;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class FolderIconView extends IconView implements FolderEventListener {
    private static final int ALPHA_SCAN_AREA = 20;
    private static final int BG_BITMAP_INDEX_CUSTOM_BASE = 5;
    private static final int CONSUMPTION_ANIMATION_DURATION = 100;
    private static final int CONVERTED_FOLDER_RING_OPACITY = 51;
    private static final int DROP_IN_ANIMATION_DURATION = 300;
    private static final Boolean FEATURE_IS_TABLET = Boolean.valueOf(LauncherFeature.isTablet());
    private static final int FINAL_ITEM_ANIMATION_DURATION = 200;
    private static final int IMPROVE_PREVIEW_DENSITY_THRESHOLD = 400;
    private static final int IMPROVE_PREVIEW_ENLARGE_RATIO = 2;
    private static final int INITIAL_ITEM_ANIMATION_DURATION = 290;
    private static final float INNER_RING_GROWTH_FACTOR = 0.15f;
    private static final int INVALID_INT_VALUE = -1;
    public static final int NUM_ITEMS_IN_PREVIEW = 9;
    private static final String TAG = "FolderIconView";
    private static boolean sNeedToImprovePreviewImage = false;
    private static Bitmap[] sSharedIconBgBitmap = null;
    private static boolean sStaticValuesDirty = true;
    private PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0);
    private int mAvailableSpaceInPreview;
    private float mBaselineIconScale;
    private int mBaselineIconSize;
    private Bitmap mFolderIconBitmap;
    private FolderRingAnimator mFolderRingAnimator = null;
    private FolderStyle mFolderStyle;
    private FolderView mFolderView;
    private FolderInfo mInfo;
    private int mIntrinsicIconSize;
    private CheckLongPressHelper mLongPressHelper;
    private int mMiniIconCol;
    private float mMiniIconGap;
    private Rect mOldBounds = new Rect();
    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0);
    private ImageView mPreviewBackground;
    private boolean mPreviewFactorChanged = false;
    private ImageView mPreviewIcons;
    private ArrayList<View> mPreviewItems = new ArrayList(9);
    private int mPreviewLocalOffsetX;
    private int mPreviewLocalOffsetY;
    private int mPreviewOffsetX;
    private int mPreviewOffsetY;
    private float mPreviewPaddingRatio;
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;
    private int mTotalWidth = -1;

    public static class FolderRingAnimator {
        private static int sIconSize = -1;
        private static int sPreviewSize = -1;
        private static Drawable[] sSharedInnerRingDrawable = null;
        private ValueAnimator mAcceptAnimator;
        private CellLayout mCellLayout;
        public int mCellX;
        public int mCellY;
        private FolderIconView mFolderIconView = null;
        private float mInnerRingSize;
        private ValueAnimator mNeutralAnimator;

        public FolderRingAnimator(FolderIconView folderIconView, int iconSize) {
            this.mFolderIconView = folderIconView;
            sPreviewSize = iconSize;
            if (!FolderIconView.sStaticValuesDirty) {
                return;
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new RuntimeException("FolderRingAnimator loading drawables on non-UI thread " + Thread.currentThread());
            }
            sIconSize = iconSize;
            FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
            sSharedInnerRingDrawable = new Drawable[6];
            if (fs != null) {
                sSharedInnerRingDrawable[0] = new BitmapDrawable(fs.getCloseFolderBackground(0, sIconSize, sIconSize));
                sSharedInnerRingDrawable[1] = new BitmapDrawable(fs.getCloseFolderBackground(1, sIconSize, sIconSize));
                sSharedInnerRingDrawable[2] = new BitmapDrawable(fs.getCloseFolderBackground(2, sIconSize, sIconSize));
                sSharedInnerRingDrawable[3] = new BitmapDrawable(fs.getCloseFolderBackground(3, sIconSize, sIconSize));
                sSharedInnerRingDrawable[4] = new BitmapDrawable(fs.getCloseFolderBackground(4, sIconSize, sIconSize));
                if (FolderIconView.sSharedIconBgBitmap != null) {
                    sSharedInnerRingDrawable[5] = new BitmapDrawable(Bitmap.createScaledBitmap(FolderIconView.sSharedIconBgBitmap[5], sIconSize, sIconSize, true));
                }
            }
            FolderIconView.sStaticValuesDirty = false;
        }

        public void animateToAcceptState() {
            animateToAcceptState(sPreviewSize);
        }

        private void animateToAcceptState(int iconSize) {
            if (this.mNeutralAnimator != null) {
                this.mNeutralAnimator.cancel();
            }
            this.mAcceptAnimator = LauncherAnimUtils.ofFloat(this.mCellLayout, 0.0f, 1.0f);
            this.mAcceptAnimator.setDuration(100);
            sPreviewSize = iconSize;
            final int previewSize = iconSize;
            this.mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    FolderRingAnimator.this.mInnerRingSize = (1.0f + (FolderIconView.INNER_RING_GROWTH_FACTOR * ((Float) animation.getAnimatedValue()).floatValue())) * ((float) previewSize);
                    if (FolderRingAnimator.this.mCellLayout != null) {
                        FolderRingAnimator.this.mCellLayout.invalidate();
                    }
                }
            });
            this.mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (this.mAcceptAnimator != null) {
                this.mAcceptAnimator.cancel();
            }
            this.mNeutralAnimator = LauncherAnimUtils.ofFloat(this.mCellLayout, 0.0f, 1.0f);
            this.mNeutralAnimator.setDuration(100);
            final int previewSize = sPreviewSize;
            this.mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    FolderRingAnimator.this.mInnerRingSize = (((1.0f - ((Float) animation.getAnimatedValue()).floatValue()) * FolderIconView.INNER_RING_GROWTH_FACTOR) + 1.0f) * ((float) previewSize);
                    if (FolderRingAnimator.this.mCellLayout != null) {
                        FolderRingAnimator.this.mCellLayout.invalidate();
                    }
                }
            });
            this.mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (FolderRingAnimator.this.mCellLayout != null) {
                        FolderRingAnimator.this.mCellLayout.hideFolderAccept(FolderRingAnimator.this);
                    }
                }
            });
            this.mNeutralAnimator.start();
        }

        public void setCell(int x, int y) {
            this.mCellX = x;
            this.mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            this.mCellLayout = layout;
        }

        public Drawable getInnerRingDrawable() {
            FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
            if (this.mFolderIconView == null || this.mFolderIconView.getFolderInfo() == null || (fs != null && fs.getFolderType() == 1)) {
                return sSharedInnerRingDrawable[0];
            }
            int color = this.mFolderIconView.getFolderInfo().color;
            if (this.mFolderIconView.getFolderInfo().hasOption(8)) {
                color = convertColorOpacityIfNeeded(color);
                if (fs == null || fs.getCloseFolderShape() <= 0) {
                    if (!(FolderIconView.sSharedIconBgBitmap == null || FolderIconView.sSharedIconBgBitmap[5] == null || sSharedInnerRingDrawable[5] != null)) {
                        sSharedInnerRingDrawable[5] = new BitmapDrawable(Bitmap.createScaledBitmap(FolderIconView.sSharedIconBgBitmap[5], sIconSize, sIconSize, true));
                    }
                    if (sSharedInnerRingDrawable[5] != null) {
                        sSharedInnerRingDrawable[5].setColorFilter(color, Mode.MULTIPLY);
                    }
                    return sSharedInnerRingDrawable[5];
                }
                return new BitmapDrawable(this.mFolderIconView.getResources(), Bitmap.createScaledBitmap(fs.getCloseFolderShapedBitmapWithUserColor(this.mFolderIconView.getContext(), color), sIconSize, sIconSize, true));
            }
            if (color < 0 || color >= sSharedInnerRingDrawable.length) {
                color = 0;
            }
            return sSharedInnerRingDrawable[color];
        }

        public float getInnerRingSize() {
            return this.mInnerRingSize;
        }

        private int convertColorOpacityIfNeeded(int color) {
            if (Color.alpha(color) < FolderIconView.CONVERTED_FOLDER_RING_OPACITY) {
                return Color.argb(FolderIconView.CONVERTED_FOLDER_RING_OPACITY, Color.red(color), Color.green(color), Color.blue(color));
            }
            return color;
        }
    }

    private static class PreviewItemDrawingParams {
        Drawable drawable;
        int overlayAlpha;
        float scale;
        float transX;
        float transY;

        PreviewItemDrawingParams(float transX, float transY, float scale, int overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
    }

    public FolderIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FolderIconView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.mFolderStyle = OpenThemeManager.getInstance().getFolderStyle();
        if (sStaticValuesDirty || sSharedIconBgBitmap == null) {
            if (this.mFolderStyle != null) {
                sSharedIconBgBitmap = new Bitmap[6];
                sSharedIconBgBitmap[0] = this.mFolderStyle.getCloseFolderImage(0);
                sSharedIconBgBitmap[1] = this.mFolderStyle.getCloseFolderImage(1);
                sSharedIconBgBitmap[2] = this.mFolderStyle.getCloseFolderImage(2);
                sSharedIconBgBitmap[3] = this.mFolderStyle.getCloseFolderImage(3);
                sSharedIconBgBitmap[4] = this.mFolderStyle.getCloseFolderImage(4);
                sSharedIconBgBitmap[5] = BitmapUtils.getBitmap(context.getResources().getDrawable(R.mipmap.folder_transparent_shape, null));
            }
            if (getResources().getConfiguration().densityDpi < IMPROVE_PREVIEW_DENSITY_THRESHOLD) {
                sNeedToImprovePreviewImage = true;
            }
        }
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mPreviewPaddingRatio = getResources().getFraction(R.fraction.config_folderIconPreviewPaddingRatio, 1, 1);
    }

    public static void release() {
        sSharedIconBgBitmap = null;
        sStaticValuesDirty = true;
        sNeedToImprovePreviewImage = false;
    }

    public static FolderIconView fromXml(Launcher launcher, ViewGroup group, FolderInfo folderInfo, ControllerBase controller, OnClickListener onClickListener, OnLongClickListener onLongClickListener, int iconDisplay) {
        FolderIconView icon = (FolderIconView) folderInfo.getBoundView(FolderIconView.class);
        if (!(icon == null || (icon.getContext().equals(launcher) && folderInfo.opened))) {
            folderInfo.opened = false;
            folderInfo.unbind();
            icon = null;
        }
        if (icon != null) {
            Log.i(TAG, "already view bound. " + folderInfo + " - " + icon);
            if (icon.getParent() != null) {
                ((ViewGroup) icon.getParent()).removeView(icon);
            }
            icon.setOnClickListener(onClickListener);
            icon.setOnLongClickListener(onLongClickListener);
        } else {
            View view = LayoutInflater.from(launcher).inflate(R.layout.folder_icon, group, false);
            if (view instanceof FolderIconView) {
                icon = (FolderIconView) view;
                icon.setClipToPadding(false);
                icon.mTitleView = (TextView) icon.findViewById(R.id.iconview_titleView);
                icon.setText(folderInfo.title);
                icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.iconview_imageView);
                icon.mPreviewIcons = (ImageView) icon.findViewById(R.id.iconview_image_preview);
                icon.setIconDisplay(iconDisplay);
                final FolderIconView parentFolderIconView = icon;
                icon.mPreviewBackground.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (top != oldTop || bottom != oldBottom) {
                            parentFolderIconView.mPreviewFactorChanged = true;
                        }
                    }
                });
                FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
                icon.setIconBackgroundColor(folderInfo.color);
                icon.setTag(folderInfo);
                icon.setOnClickListener(onClickListener);
                icon.setOnLongClickListener(onLongClickListener);
                icon.mInfo = folderInfo;
                if (folderInfo.hasOption(8)) {
                    icon.updateCustomColor(folderInfo.color);
                }
                icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format), new Object[]{folderInfo.title}));
                FolderView folder = FolderView.fromXml(launcher);
                folder.setMultiSelectManager(launcher.getMultiSelectManager());
                folder.setDragMgr(launcher.getDragMgr());
                folder.setFolderIcon(icon);
                folder.bind(folderInfo, controller);
                icon.mFolderView = folder;
                icon.drawPreviews();
                icon.mFolderRingAnimator = new FolderRingAnimator(icon, icon.getIconSize());
                folderInfo.addListener(icon);
                icon.mBadgeView = (TextView) icon.findViewById(R.id.iconview_badge);
                icon.refreshBadge();
                if (folderInfo.container != -102) {
                    icon.changeTextColorForBg(WhiteBgManager.isWhiteBg());
                } else {
                    icon.changeTextColorForBg(false);
                }
            } else {
                throw new IllegalArgumentException("invalid resid : " + view);
            }
        }
        return icon;
    }

    public void applyStyle() {
        super.applyStyle();
        if (this.mPreviewBackground != null) {
            LayoutParams lp = (LayoutParams) this.mPreviewBackground.getLayoutParams();
            if (lp.width != this.mIconSize) {
                this.mPreviewFactorChanged = true;
            }
            lp.width = this.mIconSize;
            lp.height = this.mIconSize;
            int topMargin = lp.topMargin;
            int leftMargin = lp.leftMargin;
            lp = (LayoutParams) this.mPreviewIcons.getLayoutParams();
            if (lp != null) {
                lp.width = this.mIconSize;
                lp.height = this.mIconSize;
                if (this.mIsPhoneLandscape) {
                    lp.topMargin = 0;
                    lp.leftMargin = leftMargin;
                    lp.gravity = 19;
                } else {
                    lp.topMargin = topMargin;
                    lp.leftMargin = 0;
                    lp.gravity = 49;
                }
            }
        }
        this.mPreviewItems.clear();
        drawPreviews();
    }

    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public void changeTextColorForBg(boolean whiteBg) {
        super.changeTextColorForBg(whiteBg);
        if (this.mFolderView != null) {
            this.mFolderView.updateFolderColor();
        }
    }

    public FolderView getFolderView() {
        return this.mFolderView;
    }

    public FolderInfo getFolderInfo() {
        return this.mInfo;
    }

    private boolean willAcceptItem(ItemInfo item) {
        int itemType = item.itemType;
        if ((itemType != 0 && itemType != 1 && itemType != 6 && itemType != 7 && !this.mLauncher.getMultiSelectManager().acceptDropToFolder()) || this.mFolderView.isFull() || item == this.mInfo || this.mInfo.opened) {
            return false;
        }
        return true;
    }

    public boolean acceptDrop(Object dragInfo) {
        return !this.mFolderView.isDestroyed() && willAcceptItem((ItemInfo) dragInfo);
    }

    public void addItem(IconInfo item) {
        this.mInfo.add(item);
        Talk.INSTANCE.say(getContext().getString(R.string.tts_item_moved_into_folder));
    }

    public void addItems(ArrayList<IconInfo> items) {
        this.mInfo.add((ArrayList) items);
    }

    public void onDragEnter(Object dragInfo) {
        if (!this.mFolderView.isDestroyed() && willAcceptItem((ItemInfo) dragInfo)) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
            CellLayout layout = (CellLayout) getParent().getParent();
            this.mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
            this.mFolderRingAnimator.setCellLayout(layout);
            this.mFolderRingAnimator.animateToAcceptState(getIconSize());
            layout.showFolderAccept(this.mFolderRingAnimator);
        }
    }

    public void performCreateAnimation(IconInfo destInfo, View destView, IconInfo srcInfo, DragView srcView, Rect dstRect, float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {
        Drawable animateDrawable = getDrawable((IconView) destView);
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), destView.getMeasuredWidth());
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION, false, null);
        addItem(destInfo);
        ViewGroup parentView = (ViewGroup) getParent();
        if (parentView != null) {
            parentView.measure(0, 0);
            parentView.layout(parentView.getLeft(), parentView.getTop(), parentView.getRight(), parentView.getBottom());
        }
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(View finalView, Runnable onCompleteRunnable) {
        if (finalView instanceof IconView) {
            Drawable animateDrawable = getTopDrawable((IconView) finalView);
            computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), finalView.getMeasuredWidth());
            animateFirstItem(animateDrawable, 200, true, onCompleteRunnable);
        }
    }

    public void onDragExit() {
        this.mFolderRingAnimator.animateToNaturalState();
    }

    private void onDrop(IconInfo item, DragView animateView, Rect finalRect, float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable, DragObject d) {
        onDrop(item, animateView, finalRect, scaleRelativeToDragLayer, index, postAnimationRunnable, d, true);
    }

    private void onDrop(IconInfo item, DragView animateView, Rect finalRect, float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable, DragObject d, boolean immediateAdd) {
        boolean disallowFolderRingAnimation = false;
        if (item.container == this.mInfo.container) {
            if (this.mInfo.container == -101) {
                disallowFolderRingAnimation = true;
            } else if (this.mInfo.container == -102 && item.rank < this.mInfo.rank) {
                disallowFolderRingAnimation = true;
            }
            if (disallowFolderRingAnimation && this.mFolderRingAnimator.mCellLayout != null) {
                this.mFolderRingAnimator.mCellLayout.hideFolderAccept(this.mFolderRingAnimator);
            }
        }
        if (item.container != -1) {
            item.container = getFolderInfo().id;
        }
        item.cellX = -1;
        item.cellY = -1;
        if (animateView == null || this.mInfo.isAlphabeticalOrder()) {
            if (immediateAdd) {
                addItem(item);
            }
            if (d != null) {
                d.deferDragViewCleanupPostAnimation = false;
            }
            if (postAnimationRunnable != null) {
                postAnimationRunnable.run();
                return;
            }
            return;
        }
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(animateView, from);
        Rect to = finalRect;
        if (to == null) {
            to = new Rect();
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to, false);
            setScaleX(scaleX);
            setScaleY(scaleY);
        }
        computePreviewDrawingParams(this.mIntrinsicIconSize, getMeasuredWidth());
        center = new int[2];
        float scale = getLocalCenterForIndex(index, center);
        center[0] = Math.round(((float) center[0]) * scaleRelativeToDragLayer);
        center[1] = Math.round(((float) center[1]) * scaleRelativeToDragLayer);
        to.offset(center[0] - (animateView.getMeasuredWidth() / 2), center[1] - (animateView.getMeasuredHeight() / 2));
        float finalScale = scale * scaleRelativeToDragLayer;
        dragLayer.animateView(animateView, from, to, index < 9 ? 0.5f : 0.0f, 1.0f, 1.0f, finalScale, finalScale, 300, new DecelerateInterpolator(2.0f), new AccelerateInterpolator(2.0f), postAnimationRunnable, 0, this);
        if (immediateAdd) {
            addItem(item);
        }
    }

    private IconInfo getDropItem(DragObject dragObject) {
        if (!(dragObject.dragInfo instanceof PendingAddPinShortcutInfo)) {
            return dragObject.dragInfo;
        }
        IconInfo shortcutInfo = ((PendingAddPinShortcutInfo) dragObject.dragInfo).getShortcutInfo().createShortcutInfo();
        if (shortcutInfo != null) {
            return shortcutInfo;
        }
        return null;
    }

    public void onDrop(DragObject d) {
        IconInfo item = null;
        DragObject dragObject = null;
        if ((d.dragInfo instanceof IconInfo) || (d.dragInfo instanceof PendingAddPinShortcutInfo)) {
            item = getDropItem(d);
            if (this.mInfo.container != -102 && ((d.dragSource != null && d.dragSource.getDragSourceType() == 4) || item.container == -102)) {
                item = item.makeCloneInfo();
                dragObject = new DragObject();
                dragObject.dragInfo = item;
                dragObject.dragView = d.dragView;
            }
            if (dragObject == null) {
                dragObject = d;
            }
        }
        this.mFolderView.notifyDrop();
        if (item != null) {
            onDrop(item, dragObject.dragView, null, 1.0f, this.mInfo.contents.size(), d.postAnimationRunnable, dragObject);
        }
    }

    public void onDrop(ArrayList<DragObject> extraDragObjects, Rect dstRect) {
        int i;
        IconInfo item = null;
        ArrayList<DragObject> dragObjects = new ArrayList();
        for (i = 0; i < extraDragObjects.size(); i++) {
            DragObject d = (DragObject) extraDragObjects.get(i);
            DragObject dragObject = null;
            if ((d.dragInfo instanceof IconInfo) || (d.dragInfo instanceof PendingAddPinShortcutInfo)) {
                item = getDropItem(d);
                if (this.mInfo.container != -102 && (d.dragSource.getDragSourceType() == 4 || item.container == -102)) {
                    item = item.makeCloneInfo();
                    dragObject = new DragObject();
                    dragObject.dragInfo = item;
                    dragObject.dragView = d.dragView;
                    dragObject.postAnimationRunnable = d.postAnimationRunnable;
                }
                if (dragObject == null) {
                    dragObject = d;
                }
                dragObjects.add(dragObject);
            }
        }
        ArrayList<IconInfo> animateItems = new ArrayList();
        Iterator it = dragObjects.iterator();
        while (it.hasNext()) {
            animateItems.add((IconInfo) ((DragObject) it.next()).dragInfo);
        }
        this.mFolderView.setSuppressOnAdd(false);
        if (!animateItems.isEmpty()) {
            addItems(animateItems);
        }
        int contentSize = this.mInfo.contents.size();
        for (i = 0; i < dragObjects.size(); i++) {
            dragObject = (DragObject) dragObjects.get(i);
            onDrop(item, dragObject.dragView, dstRect, 1.0f, contentSize + i, dragObject.postAnimationRunnable, dragObject, false);
        }
    }

    private void computeMiniIconSize(int itemCount) {
        int oldIconCol = this.mMiniIconCol;
        if (itemCount > 0) {
            this.mMiniIconCol = 3;
            this.mMiniIconGap = getResources().getDimension(R.dimen.folder_mini_icon_3x3_gap);
        } else {
            this.mMiniIconCol = 2;
            this.mMiniIconGap = getResources().getDimension(R.dimen.folder_mini_icon_2x2_gap);
        }
        if (oldIconCol != this.mMiniIconCol) {
            this.mPreviewFactorChanged = true;
        }
    }

    private PreviewItemDrawingParams computeLockedPreviewDrawingParams(Drawable d) {
        int drawableSize = d.getIntrinsicWidth();
        int totalSize = getMeasuredWidth();
        PreviewItemDrawingParams params = new PreviewItemDrawingParams(0.0f, 0.0f, 1.0f, 0);
        if (!(this.mIntrinsicIconSize == drawableSize && this.mTotalWidth == totalSize && !this.mPreviewFactorChanged)) {
            int previewSize = this.mPreviewBackground.getLayoutParams().height;
            float mLockedmBaselineIconScale = ((float) (previewSize - (Math.round(this.mPreviewPaddingRatio * ((float) previewSize)) * 2))) / ((float) drawableSize);
            params.transX = 0.0f;
            params.transY = 0.0f;
            params.overlayAlpha = 0;
            params.scale = mLockedmBaselineIconScale;
            params.drawable = d;
            this.mPreviewFactorChanged = false;
        }
        return params;
    }

    private void computePreviewDrawingParams(int drawableSize, int totalSize) {
        computeMiniIconSize(this.mInfo.contents.size());
        if (this.mIntrinsicIconSize != drawableSize || this.mTotalWidth != totalSize || this.mPreviewFactorChanged) {
            this.mIntrinsicIconSize = drawableSize;
            this.mTotalWidth = totalSize;
            int previewPosY = this.mPreviewIcons.getTop();
            int previewSize = this.mPreviewBackground.getLayoutParams().height;
            int previewPadding = Math.round(this.mPreviewPaddingRatio * ((float) previewSize));
            this.mAvailableSpaceInPreview = previewSize - (previewPadding * 2);
            this.mBaselineIconSize = (int) ((((float) this.mAvailableSpaceInPreview) - (((float) (this.mMiniIconCol - 1)) * this.mMiniIconGap)) / ((float) this.mMiniIconCol));
            this.mBaselineIconScale = ((float) this.mBaselineIconSize) / ((float) this.mIntrinsicIconSize);
            if (this.mIsPhoneLandscape) {
                this.mPreviewOffsetX = getIconInfo().getIconStartPadding() + previewPadding;
            } else {
                this.mPreviewOffsetX = (this.mTotalWidth - this.mAvailableSpaceInPreview) / 2;
            }
            this.mPreviewOffsetY = previewPosY + previewPadding;
            this.mPreviewLocalOffsetX = previewPadding;
            this.mPreviewLocalOffsetY = previewPadding;
            this.mPreviewFactorChanged = false;
        }
    }

    private void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    private float getLocalCenterForIndex(int index, int[] center) {
        int targetIndex;
        if (index >= 9) {
            targetIndex = 4;
        } else {
            targetIndex = index;
        }
        this.mParams = computePreviewItemDrawingParams(Math.min(9, targetIndex), this.mParams);
        PreviewItemDrawingParams previewItemDrawingParams = this.mParams;
        previewItemDrawingParams.transX += (float) this.mPreviewOffsetX;
        previewItemDrawingParams = this.mParams;
        previewItemDrawingParams.transY += (float) this.mPreviewOffsetY;
        float offsetY = this.mParams.transY + ((this.mParams.scale * ((float) this.mIntrinsicIconSize)) / 2.0f);
        center[0] = Math.round(this.mParams.transX + ((this.mParams.scale * ((float) this.mIntrinsicIconSize)) / 2.0f));
        center[1] = Math.round(offsetY);
        return this.mParams.scale;
    }

    private PreviewItemDrawingParams computePreviewItemDrawingParams(int index, PreviewItemDrawingParams params) {
        float transX = (((float) this.mBaselineIconSize) + this.mMiniIconGap) * ((float) (Utilities.sIsRtl ? (this.mMiniIconCol - 1) - (index % this.mMiniIconCol) : index % this.mMiniIconCol));
        float transY = (((float) this.mBaselineIconSize) + this.mMiniIconGap) * ((float) (index / this.mMiniIconCol));
        float totalScale = this.mBaselineIconScale;
        if (params == null) {
            return new PreviewItemDrawingParams(transX, transY, totalScale, 0);
        }
        params.transX = transX;
        params.transY = transY;
        params.scale = totalScale;
        params.overlayAlpha = 0;
        return params;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        Drawable d = params.drawable;
        if (d != null) {
            int l = ((int) params.transX) + this.mPreviewLocalOffsetX;
            int t = ((int) params.transY) + this.mPreviewLocalOffsetY;
            if (sNeedToImprovePreviewImage) {
                canvas.scale(2.0f, 2.0f);
                Bitmap b = BitmapUtils.createIconBitmap(d, getContext(), d.getIntrinsicWidth(), d.getIntrinsicHeight());
                canvas.setMatrix(null);
                canvas.scale(2.0f, 2.0f);
                canvas.translate((float) l, (float) t);
                canvas.scale(params.scale, params.scale);
                canvas.drawBitmap(b, 0.0f, 0.0f, null);
                b.recycle();
            } else {
                this.mOldBounds.set(d.getBounds());
                d.setBounds(l, t, ((int) (((float) d.getIntrinsicWidth()) * params.scale)) + l, ((int) (((float) d.getIntrinsicHeight()) * params.scale)) + t);
                if (d instanceof FastBitmapDrawable) {
                    FastBitmapDrawable fd = (FastBitmapDrawable) d;
                    int oldBrightness = fd.getBrightness();
                    fd.setBrightness(params.overlayAlpha);
                    fd.draw(canvas);
                    fd.setBrightness(oldBrightness);
                } else {
                    d.setColorFilter(Color.argb(params.overlayAlpha, 255, 255, 255), Mode.SRC_ATOP);
                    d.draw(canvas);
                    d.clearColorFilter();
                }
                d.setBounds(this.mOldBounds);
            }
        }
        canvas.restore();
    }

    private boolean needMaskedIcon(Bitmap plate) {
        if (this.mFolderStyle != null && this.mFolderStyle.getFolderType() == 1) {
            return false;
        }
        int startX = (plate.getWidth() - 20) / 2;
        int startY = (plate.getHeight() - 20) / 2;
        int endX = startX + 20;
        for (int x = startX; x < endX; x++) {
            int endY = startY + 20;
            for (int y = startY; y < endY; y++) {
                if (Color.alpha(plate.getPixel(x, y)) != 255) {
                    return false;
                }
            }
        }
        return true;
    }

    private Bitmap getMaskedIcon(Bitmap plate, Bitmap icon) {
        if (!needMaskedIcon(plate)) {
            return icon;
        }
        Bitmap result = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), icon.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        float maskScale = ((float) icon.getScaledWidth(canvas)) / ((float) plate.getScaledWidth(canvas));
        if (maskScale != 1.0f) {
            canvas.save();
            canvas.scale(maskScale, maskScale);
            canvas.drawBitmap(plate, 0.0f, 0.0f, paint);
            canvas.restore();
        } else {
            canvas.drawBitmap(plate, 0.0f, 0.0f, paint);
        }
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(icon, 0.0f, 0.0f, paint);
        icon.recycle();
        return result;
    }

    private void drawPreviews() {
        if (this.mFolderView != null) {
            int i;
            int bitmapSize;
            ArrayList<View> items = this.mFolderView.getItemsInReadingOrder();
            if (this.mPreviewItems.size() >= 9 && items.size() >= 9) {
                boolean allEquals = true;
                for (i = 0; i < 9; i++) {
                    if (!((View) this.mPreviewItems.get(i)).equals(items.get(i))) {
                        allEquals = false;
                        break;
                    }
                }
                if (allEquals) {
                    Log.i(TAG, "Skip drawPreviews - already drawn same bitmap");
                    return;
                }
            }
            if (this.mFolderIconBitmap != null) {
                this.mFolderIconBitmap.recycle();
            }
            if (sNeedToImprovePreviewImage) {
                bitmapSize = this.mIconSize * 2;
            } else {
                bitmapSize = this.mIconSize;
            }
            this.mFolderIconBitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Config.ARGB_8888);
            Canvas canvas = new Canvas(this.mFolderIconBitmap);
            boolean drawDone = false;
            if (LauncherFeature.supportFolderLock()) {
                FolderLock folderLock = FolderLock.getInstance();
                if (folderLock != null && folderLock.isFolderLockEnabled() && this.mInfo.isLocked() && !this.mInfo.isLockedFolderOpenedOnce()) {
                    drawPreviewItem(canvas, computeLockedPreviewDrawingParams(getResources().getDrawable(R.drawable.sm_ic_home_foler_loc_kxx, null)));
                    drawDone = true;
                }
            }
            if (!drawDone) {
                int itemCount = Math.min(items.size(), 9);
                this.mPreviewItems.clear();
                for (i = itemCount - 1; i >= 0; i--) {
                    if (items.get(i) instanceof IconView) {
                        IconView v = (IconView) items.get(i);
                        IconInfo f = (IconInfo) v.getTag();
                        if (f == null) {
                            Log.w(TAG, "ignore drawPreviewItem because IconInfo is null");
                        } else {
                            Drawable d;
                            String pkg = null;
                            if (f.intent.getComponent() != null) {
                                pkg = f.intent.getComponent().getPackageName();
                            }
                            if (pkg == null) {
                                d = getTopDrawable(v);
                            } else if (applyKnoxLiveIcon(f)) {
                                d = new BitmapDrawable(getResources(), f.mIcon);
                            } else if (LiveIconManager.isCalendarPackage(pkg)) {
                                Drawable iconDrawable = getLiveIconDrawable(pkg, f.user);
                                boolean z = f.isPromise() || f.isDisabled != 0;
                                iconDrawable.setGhostModeEnabled(z);
                                d = iconDrawable;
                                this.mPreviewItems.clear();
                            } else {
                                d = getTopDrawable(v);
                            }
                            this.mBaselineIconScale = ((float) this.mBaselineIconSize) / ((float) d.getIntrinsicWidth());
                            this.mPreviewItems.add(0, v);
                            if (i == itemCount - 1) {
                                computePreviewDrawingParams(d);
                            }
                            this.mParams = computePreviewItemDrawingParams(i, this.mParams);
                            this.mParams.drawable = d;
                            drawPreviewItem(canvas, this.mParams);
                        }
                    }
                }
            }
            if (!(sSharedIconBgBitmap == null || this.mInfo.hasOption(8) || this.mInfo.color >= sSharedIconBgBitmap.length)) {
                this.mFolderIconBitmap = getMaskedIcon(sSharedIconBgBitmap[this.mInfo.color == -1 ? 0 : this.mInfo.color], this.mFolderIconBitmap);
            }
            this.mPreviewIcons.setImageBitmap(this.mFolderIconBitmap);
            setShadow();
        }
    }

    private Drawable getTopDrawable(IconView v) {
        Drawable icon = v.getIcon();
        return icon instanceof PreloadIconDrawable ? ((PreloadIconDrawable) icon).getIcon() : icon;
    }

    public Bitmap getFolderIconBitmapWithPlate() {
        Bitmap plate;
        int index = this.mInfo.color;
        if (index <= 0 || sSharedIconBgBitmap == null) {
            plate = BitmapUtils.getBitmap(getResources().getDrawable(R.mipmap.homescreen_ic_folder_default, this.mLauncher.getTheme()));
        } else {
            plate = sSharedIconBgBitmap[index];
        }
        int size = this.mFolderIconBitmap.getWidth();
        Bitmap result = Bitmap.createBitmap(this.mFolderIconBitmap);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Matrix matrix = new Matrix();
        matrix.setScale(((float) size) / ((float) plate.getWidth()), ((float) size) / ((float) plate.getHeight()));
        canvas.drawBitmap(plate, matrix, paint);
        canvas.drawBitmap(this.mFolderIconBitmap, 0.0f, 0.0f, paint);
        return result;
    }

    private FastBitmapDrawable getLiveIconDrawable(String pkg, UserHandleCompat user) {
        return BitmapUtils.createIconDrawable(LiveIconManager.getLiveIcon(this.mLauncher, pkg, user), this.mIconSize);
    }

    private Drawable getDrawable(IconView v) {
        Drawable drawable = v.getIconVew().getDrawable();
        return drawable instanceof PreloadIconDrawable ? ((PreloadIconDrawable) drawable).getIcon() : drawable;
    }

    private void animateFirstItem(Drawable d, int duration, boolean reverse, final Runnable onCompleteRunnable) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);
        final float scale0 = (((float) this.mIconSize) * 1.0f) / ((float) d.getIntrinsicWidth());
        final float transX0 = ((float) (this.mAvailableSpaceInPreview - this.mIconSize)) / 2.0f;
        final float transY0 = (((float) (this.mAvailableSpaceInPreview - this.mIconSize)) / 2.0f) + ((float) getPaddingTop());
        this.mAnimParams.drawable = d;
        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0.0f, 1.0f);
        final boolean z = reverse;
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                if (z) {
                    progress = 1.0f - progress;
                    FolderIconView.this.mPreviewBackground.setAlpha(progress);
                }
                FolderIconView.this.mAnimParams.transX = transX0 + ((finalParams.transX - transX0) * progress);
                FolderIconView.this.mAnimParams.transY = transY0 + ((finalParams.transY - transY0) * progress);
                FolderIconView.this.mAnimParams.scale = scale0 + ((finalParams.scale - scale0) * progress);
                FolderIconView.this.invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });
        va.setDuration((long) duration);
        va.start();
    }

    public Drawable getIcon() {
        if (!sNeedToImprovePreviewImage) {
            return this.mPreviewIcons.getDrawable();
        }
        Drawable iconDrawable = BitmapUtils.createIconDrawable(this.mFolderIconBitmap, this.mIconSize);
        iconDrawable.setBounds(new Rect(0, 0, this.mIconSize, this.mIconSize));
        return iconDrawable;
    }

    public Drawable getIconBackground() {
        return this.mPreviewBackground.getDrawable();
    }

    private Bitmap getFolderBorderBitmap() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), this.mFolderStyle.getCloseFolderBorderRes());
        if (!WhiteBgManager.isWhiteBg() || this.mInfo.container == -102) {
            return bitmap;
        }
        Paint paint = new Paint();
        paint.setColorFilter(new LightingColorFilter(getResources().getColor(R.color.apps_picker_black_color, null), 0));
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        new Canvas(result).drawBitmap(bitmap, 0.0f, 0.0f, paint);
        return result;
    }

    void updateCustomColor(int color) {
        if (this.mFolderStyle.getFolderType() == 1) {
            setIconBackgroundColor(0);
            return;
        }
        this.mPreviewBackground.setImageBitmap(BitmapUtils.getOverlaidIcon(this.mFolderStyle.getCloseFolderShapedBitmapWithUserColor(this.mLauncher, color), getFolderBorderBitmap()));
    }

    public void setIconBackgroundColor(int colorIndex) {
        if (sSharedIconBgBitmap != null) {
            if (colorIndex < 0 || colorIndex >= sSharedIconBgBitmap.length) {
                colorIndex = 0;
            }
            this.mPreviewBackground.setImageBitmap(sSharedIconBgBitmap[colorIndex]);
            return;
        }
        Log.e(TAG, "setIconBackgroundColor : sSharedIconBgBitmap is null");
    }

    protected AppIconBounceAnimation getBounceAnimation() {
        return new AppIconBounceAnimation(this.mPreviewBackground, this.mPreviewIcons);
    }

    public void onItemAdded(IconInfo item) {
        updateLayout();
    }

    public void onItemsAdded(ArrayList<IconInfo> arrayList) {
        updateLayout();
    }

    public void onItemRemoved(IconInfo item) {
        MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
        if (multiSelectMgr != null && multiSelectMgr.isMultiSelectMode()) {
            refreshCountBadge(0);
        }
        updateLayout();
    }

    public void onItemsRemoved(ArrayList<IconInfo> arrayList) {
        MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
        if (multiSelectMgr != null && multiSelectMgr.isMultiSelectMode()) {
            refreshCountBadge(0);
        }
        updateLayout();
    }

    public void onTitleChanged(CharSequence title) {
        setText(title);
        if (getTag() instanceof FolderInfo) {
            int badge = ((FolderInfo) getTag()).mBadgeCount;
            if (badge > 1) {
                setContentDescription(String.format(getContext().getString(R.string.folder_name_format), new Object[]{title}) + ", " + String.format(getResources().getString(R.string.new_items), new Object[]{Integer.valueOf(badge)}));
            } else if (badge == 1) {
                setContentDescription(String.format(getContext().getString(R.string.folder_name_format), new Object[]{title}) + ", " + getResources().getString(R.string.new_item));
            } else {
                setContentDescription(String.format(getContext().getString(R.string.folder_name_format), new Object[]{title}));
            }
        }
    }

    public void onOrderingChanged(boolean alphabeticalOrder) {
        updateLayout();
    }

    public void onLockedFolderOpenStateUpdated(Boolean opened) {
        updateLayout();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (onTouchOutofIconArea(event)) {
            cancelLongPress();
            return true;
        }
        boolean result = super.onTouchEvent(event);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (event.getAction()) {
            case 0:
                this.mLongPressHelper.postCheckForLongPress();
                return result;
            case 1:
            case 3:
                setAlpha(1.0f);
                this.mLongPressHelper.cancelLongPress();
                return result;
            case 2:
                if (Utilities.pointInView(this, event.getX(), event.getY(), this.mSlop)) {
                    return result;
                }
                this.mLongPressHelper.cancelLongPress();
                return result;
            default:
                return result;
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    public void refreshBadge() {
        Iterator it;
        if (this.mFolderView != null) {
            it = this.mFolderView.getItemsInReadingOrder().iterator();
            while (it.hasNext()) {
                View icon = (View) it.next();
                if (icon instanceof IconView) {
                    ((IconView) icon).refreshBadge();
                }
            }
        }
        FolderInfo tag = getTag();
        int sum = 0;
        if (tag != null && (tag instanceof FolderInfo)) {
            FolderInfo item = tag;
            ArrayList<CacheKey> componentList = new ArrayList();
            it = item.contents.iterator();
            while (it.hasNext()) {
                IconInfo iconInfo = (IconInfo) it.next();
                ComponentName cn = iconInfo.getTargetComponent();
                UserHandleCompat user = iconInfo.user;
                CacheKey cacheKey = new CacheKey(cn, user);
                if (!(cn == null || user == null || iconInfo.mBadgeCount <= 0 || componentList.contains(cacheKey) || !iconInfo.mShowBadge)) {
                    sum += iconInfo.mBadgeCount;
                    componentList.add(cacheKey);
                }
            }
            item.mBadgeCount = sum;
        }
        if (this.mBadgeView != null) {
            boolean shouldHideBadge = false;
            if (LauncherFeature.supportFolderLock()) {
                FolderLock folderLock = FolderLock.getInstance();
                if (folderLock != null && folderLock.isFolderLockEnabled() && this.mInfo.isLocked() && !this.mInfo.isLockedFolderOpenedOnce()) {
                    shouldHideBadge = true;
                }
            }
            MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
            boolean multiSelectMode = multiSelectMgr != null && multiSelectMgr.isMultiSelectMode();
            if (sum == 0 || shouldHideBadge) {
                setBadgeViewToInvisible(multiSelectMode);
                return;
            }
            int badge = ((FolderInfo) getTag()).mBadgeCount;
            if (badge >= 1000) {
                badge = BadgeInfo.MAX_COUNT;
            }
            if (badge == 1) {
                setContentDescription(String.format(getResources().getString(R.string.folder_name_format), new Object[]{folderInfo.title}) + ", " + getResources().getString(R.string.new_notification));
            } else {
                setContentDescription(String.format(getResources().getString(R.string.folder_name_format), new Object[]{folderInfo.title}) + ", " + String.format(getResources().getString(R.string.new_notifications), new Object[]{Integer.valueOf(badge)}));
            }
            int badgeSettingValue = Utilities.getBadgeSettingValue(getContext());
            Drawable badgeBgDrawable = getBadgeBgDrawable(badgeSettingValue);
            if (badgeBgDrawable != null) {
                this.mBadgeView.setBackground(badgeBgDrawable);
            }
            if (badgeSettingValue == 1) {
                this.mBadgeView.setTextSize(0.0f);
                this.mBadgeView.setPadding(0, 0, 0, 0);
                this.mBadgeView.setWidth(getResources().getDimensionPixelSize(R.dimen.badge_dot_icon_size));
                this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_dot_icon_size));
            } else {
                String badgeCount = String.valueOf(badge);
                String currentLanguage = Utilities.getLocale(this.mLauncher).getLanguage();
                if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                    badgeCount = Utilities.toArabicDigits(badgeCount, currentLanguage);
                } else {
                    badgeCount = String.valueOf(badgeCount);
                }
                this.mBadgeView.setText(badgeCount);
                int width;
                if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                    this.mBadgeView.setTextSize(0, getResources().getDimension(R.dimen.badge_text_size_easymode));
                    if (badge > 99) {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number_easymode);
                    } else if (badge > 9) {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_two_number_easymode);
                    } else {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size_easymode);
                    }
                    this.mBadgeView.setWidth(width);
                    this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_icon_size_easymode));
                } else {
                    this.mBadgeView.setTextSize(0, getResources().getDimension(R.dimen.badge_text_size));
                    if (FEATURE_IS_TABLET.booleanValue()) {
                        if (badge > 99) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number);
                        } else {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size);
                        }
                        this.mBadgeView.setWidth(width);
                    } else {
                        if (badge > 99) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number);
                        } else if (badge > 9) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_two_number);
                        } else {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size);
                        }
                        this.mBadgeView.setWidth(width);
                    }
                    this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_icon_size));
                }
            }
            updateBadgeLayout();
            if (multiSelectMode) {
                setBadgeViewToInvisible(multiSelectMode);
            } else {
                this.mBadgeView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setBadgeViewToInvisible(boolean multiSelectMode) {
        this.mBadgeView.setVisibility(4);
        if (getTag() instanceof FolderInfo) {
            FolderInfo info = (FolderInfo) getTag();
            if (multiSelectMode) {
                String string;
                StringBuilder append = new StringBuilder().append(String.format(getResources().getString(R.string.folder_name_format), new Object[]{info.title})).append(", ");
                if (getCheckBox().isChecked()) {
                    string = getResources().getString(R.string.selected);
                } else {
                    string = getResources().getString(R.string.not_selected);
                }
                setContentDescription(append.append(string).toString());
                return;
            }
            setContentDescription(String.format(getResources().getString(R.string.folder_name_format), new Object[]{info.title}));
        }
    }

    public boolean isGreyIcon() {
        FolderInfo item = (FolderInfo) getTag();
        UserManagerCompat sUserManager = UserManagerCompat.getInstance(getContext());
        Iterator it = item.contents.iterator();
        while (it.hasNext()) {
            if (!sUserManager.isQuietModeEnabled(((IconInfo) it.next()).getUserHandle())) {
                return false;
            }
        }
        return true;
    }

    public void refreshFolderIcon() {
        this.mPreviewItems.clear();
        refreshBadge();
        drawPreviews();
        invalidate();
    }

    public void animateChildScale(GridIconInfo prevGridIconInfo) {
        if (prevGridIconInfo != null) {
            float iconScale = ((float) prevGridIconInfo.getIconSize()) / ((float) this.mIconSize);
            View view = this.mPreviewIcons;
            PropertyValuesHolder[] propertyValuesHolderArr = new PropertyValuesHolder[2];
            propertyValuesHolderArr[0] = PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{iconScale, 1.0f});
            propertyValuesHolderArr[1] = PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{iconScale, 1.0f});
            super.animateChildScale(prevGridIconInfo, LauncherAnimUtils.ofPropertyValuesHolder(view, propertyValuesHolderArr));
        }
    }

    public void refreshCountBadge(int count) {
        if (!LauncherFeature.supportFolderSelect() && this.mCountBadgeView != null) {
            if (count > 0) {
                this.mCountBadgeView.setText(String.valueOf(count));
                this.mCountBadgeView.setVisibility(View.VISIBLE);
                return;
            }
            this.mCountBadgeView.setVisibility(View.GONE);
        }
    }

    private void updateLayout() {
        refreshBadge();
        drawPreviews();
    }
}
