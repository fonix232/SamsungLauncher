package com.android.launcher3.common.drag;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.View.MeasureSpec;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView;
import java.util.concurrent.atomic.AtomicInteger;

public final class DragViewHelper {
    public static final int DRAG_BITMAP_PADDING = 6;
    private static final Canvas sCanvas = new Canvas();
    private static final Paint sDragOutlinePaint = new Paint();
    private static final Rect sTempRect = new Rect();

    public static Drawable createDragOutline(Context context, View v, int outlineColor) {
        Bitmap b = null;
        Drawable d = null;
        DragOutlineHelper outlineHelper = DragOutlineHelper.obtain(context);
        int width = 0;
        int height = 0;
        if (v instanceof FolderIconView) {
            d = ((FolderIconView) v).getIconVew().getDrawable();
            height = ((FolderIconView) v).getIconSize();
            width = height;
        } else if (v instanceof IconView) {
            d = ((IconView) v).getIcon();
            height = d.getIntrinsicWidth();
            width = height;
        }
        if (d != null) {
            b = Bitmap.createBitmap(width + 6, height + 6, Config.ARGB_8888);
        }
        if (b == null) {
            b = Bitmap.createBitmap(v.getWidth() + 6, v.getHeight() + 6, Config.ARGB_8888);
        }
        sCanvas.setBitmap(b);
        drawDragView(v, sCanvas, 6, true, false);
        outlineHelper.createIconDragOutline(b, sCanvas, outlineColor);
        sCanvas.setBitmap(null);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Drawable createDragOutline(Context context, Bitmap bmp) {
        DragOutlineHelper outlineHelper = DragOutlineHelper.obtain(context);
        int outlineColor = ((Launcher) context).getOutlineColor();
        Bitmap b = Bitmap.createBitmap(bmp);
        sCanvas.setBitmap(b);
        outlineHelper.createIconDragOutline(b, sCanvas, outlineColor);
        sCanvas.setBitmap(null);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Drawable createDeepShortcutDragOutline(Context context, Bitmap bmp) {
        DragOutlineHelper outlineHelper = DragOutlineHelper.obtain(context);
        int outlineColor = ((Launcher) context).getOutlineColor();
        int iconSize = ((Launcher) context).getDeviceProfile().homeGrid.getIconSize();
        Bitmap scaledPreview = Bitmap.createScaledBitmap(bmp, iconSize, iconSize, true);
        Bitmap b = Bitmap.createBitmap(iconSize + 6, iconSize + 6, Config.ARGB_8888);
        sCanvas.setBitmap(b);
        sCanvas.drawBitmap(scaledPreview, 3.0f, 3.0f, null);
        outlineHelper.createIconDragOutline(b, sCanvas, outlineColor);
        sCanvas.setBitmap(null);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Drawable createWidgetDragOutline(Context context, int width, int height) {
        DragOutlineHelper outlineHelper = DragOutlineHelper.obtain(context);
        int outlineColor = ((Launcher) context).getOutlineColor();
        Bitmap b = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        int rectPadding = ((int) context.getResources().getDisplayMetrics().density) * 4;
        sDragOutlinePaint.setAntiAlias(true);
        sCanvas.setBitmap(b);
        sCanvas.drawRoundRect(new RectF((float) rectPadding, (float) rectPadding, (float) (width - rectPadding), (float) (height - rectPadding)), 2.0f, 2.0f, sDragOutlinePaint);
        outlineHelper.createWidgetDragOutline(b, sCanvas, outlineColor);
        sCanvas.setBitmap(null);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Bitmap createDragBitmap(View v, AtomicInteger expectedPadding, boolean showBadge) {
        Bitmap b;
        int padding = expectedPadding.get();
        if (!(v instanceof IconView)) {
            b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Config.ARGB_8888);
        } else if (showBadge) {
            b = Bitmap.createBitmap(((IconView) v).isLandscape() ? getBadgeIconViewWidth((IconView) v) : v.getWidth(), v.getHeight() + padding, Config.ARGB_8888);
        } else {
            Rect bounds = Utilities.getDrawableBounds(((IconView) v).getIcon());
            b = Bitmap.createBitmap(bounds.width() + padding, bounds.height() + padding, Config.ARGB_8888);
            expectedPadding.set((padding - bounds.left) - bounds.top);
        }
        sCanvas.setBitmap(b);
        drawDragView(v, sCanvas, padding, false, showBadge);
        sCanvas.setBitmap(null);
        return b;
    }

    protected static int getBadgeIconViewWidth(IconView v) {
        return Utilities.getDrawableBounds(v.getIcon()).width() + v.getCountBadgeView().getWidth();
    }

    public static Bitmap createWidgetBitmap(View layout, int[] unScaledSize) {
        int visibility = layout.getVisibility();
        layout.setVisibility(View.VISIBLE);
        int width = MeasureSpec.makeMeasureSpec(unScaledSize[0], 1073741824);
        int height = MeasureSpec.makeMeasureSpec(unScaledSize[1], 1073741824);
        Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1], Config.ARGB_8888);
        sCanvas.setBitmap(b);
        layout.measure(width, height);
        layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
        layout.draw(sCanvas);
        sCanvas.setBitmap(null);
        layout.setVisibility(visibility);
        return b;
    }

    private static void drawDragView(View v, Canvas destCanvas, int padding, boolean isOutline, boolean showBadge) {
        Rect clipRect = sTempRect;
        v.getDrawingRect(clipRect);
        destCanvas.save();
        if ((v instanceof FolderIconView) && isOutline) {
            Drawable d = ((FolderIconView) v).getIconVew().getDrawable();
            destCanvas.translate(((float) padding) / 3.0f, (((float) padding) / 3.0f) - ((float) Utilities.getDrawableBounds(d).top));
            int iconSize = ((FolderIconView) v).getIconSize();
            sTempRect.set(d.getBounds());
            d.setBounds(0, 0, (padding / 2) + iconSize, (padding / 2) + iconSize);
            d.draw(destCanvas);
            d.setBounds(sTempRect);
        } else if (!(v instanceof IconView)) {
            destCanvas.translate((float) ((-v.getScrollX()) + (padding / 2)), (float) ((-v.getScrollY()) + (padding / 2)));
            destCanvas.clipRect(clipRect, Op.REPLACE);
            v.draw(destCanvas);
        } else if (showBadge) {
            ((IconView) v).setTitleViewVisibility(8);
            destCanvas.translate((float) ((-v.getScrollX()) + (padding / 2)), (float) ((-v.getScrollY()) + (padding / 2)));
            destCanvas.clipRect(clipRect, Op.REPLACE);
            v.draw(destCanvas);
            ((IconView) v).setTitleViewVisibility(0);
        } else {
            Drawable icon = ((IconView) v).getIcon();
            Rect bounds = Utilities.getDrawableBounds(icon);
            clipRect.set(0, 0, bounds.width() + padding, bounds.height() + padding);
            destCanvas.translate((float) ((padding / 2) - bounds.left), (float) ((padding / 2) - bounds.top));
            if (v instanceof FolderIconView) {
                Drawable background = ((FolderIconView) v).getIconBackground();
                Drawable copyBackground = null;
                try {
                    copyBackground = background.mutate().getConstantState().newDrawable();
                } catch (Exception e) {
                }
                if (copyBackground == null) {
                    copyBackground = background;
                }
                Drawable[] folderDrawable = new Drawable[]{copyBackground, icon};
                copyBackground.setBounds(bounds);
                new LayerDrawable(folderDrawable).draw(destCanvas);
            } else {
                icon.draw(destCanvas);
            }
        }
        destCanvas.restore();
    }
}
