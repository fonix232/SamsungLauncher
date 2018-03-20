package com.android.launcher3.widget.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.model.WidgetPreviewUtils;
import com.android.launcher3.widget.view.WidgetItemView;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class WidgetDragController implements DragSource {
    private static final String TAG = "WidgetDragController";
    private final DragManager mDragManager = this.mLauncher.getDragMgr();
    private WidgetHostViewLoader mHostViewLoader;
    private final IconCache mIconCache = LauncherAppState.getInstance().getIconCache();
    private final Launcher mLauncher;
    private final WidgetPreviewUtils mPreviewUtils = WidgetPreviewUtils.getInstance();

    public WidgetDragController(Context context) {
        this.mLauncher = (Launcher) context;
    }

    public boolean startDrag(View v) {
        boolean z = false;
        if ((Utilities.ATLEAST_O || v.isInTouchMode()) && !this.mLauncher.isRunningAnimation()) {
            Log.d(TAG, "onLongClick dragging enabled?." + v);
            if (this.mLauncher.isDraggingEnabled()) {
                if (v.getTag() instanceof PendingAddWidgetInfo) {
                    PendingAddWidgetInfo info = (PendingAddWidgetInfo) v.getTag();
                    LauncherAppWidgetProviderInfo providerInfo = (LauncherAppWidgetProviderInfo) info.getProviderInfo();
                    info.spanX = providerInfo.getSpanX();
                    info.spanY = providerInfo.getSpanY();
                    info.minSpanX = providerInfo.getMinSpanX();
                    info.minSpanY = providerInfo.getMinSpanY();
                }
                z = beginDragging(v);
                if (z && (v.getTag() instanceof PendingAddWidgetInfo)) {
                    this.mHostViewLoader = new WidgetHostViewLoader(this.mLauncher, v);
                    this.mHostViewLoader.preloadWidget();
                    this.mDragManager.addDragListener(this.mHostViewLoader);
                }
            }
        }
        return z;
    }

    private boolean beginDragging(View v) {
        if (v instanceof WidgetItemView) {
            ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
            if (image.getDrawable() == null) {
                return false;
            }
            this.mLauncher.getHomeController().enterDragState(true);
            beginDraggingWidget((WidgetItemView) v, image);
            return true;
        }
        Log.e(TAG, "Unexpected dragging view: " + v);
        return true;
    }

    private void beginDraggingWidget(WidgetItemView v, ImageView imageView) {
        Bitmap preview;
        float scale;
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();
        FastBitmapDrawable drawable = (FastBitmapDrawable) imageView.getDrawable();
        Rect bounds = drawable.getBounds();
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            int[] size = this.mLauncher.getHomeController().getWorkspace().estimateItemSize(createWidgetInfo);
            Bitmap icon = drawable.getBitmap();
            int[] previewSizeBeforeScale = new int[1];
            preview = this.mPreviewUtils.generateWidgetPreview(this.mLauncher, createWidgetInfo.info, Math.min((int) (((float) icon.getWidth()) * 1.25f), size[0]), previewSizeBeforeScale);
            if (previewSizeBeforeScale[0] < icon.getWidth()) {
                int padding = (icon.getWidth() - previewSizeBeforeScale[0]) / 2;
                if (icon.getWidth() > imageView.getWidth()) {
                    padding = (imageView.getWidth() * padding) / icon.getWidth();
                }
                bounds.left += padding;
                bounds.right -= padding;
            }
            scale = ((float) bounds.width()) / ((float) preview.getWidth());
        } else {
            Drawable icon2 = this.mIconCache.getFullResIcon(((PendingAddShortcutInfo) v.getTag()).getActivityInfo());
            int iconSize = this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
            Bitmap previewWithoutTray = BitmapUtils.createIconBitmap(icon2, this.mLauncher, iconSize, iconSize);
            preview = OpenThemeManager.getInstance().getIconWithTrayIfNeeded(previewWithoutTray, iconSize, false);
            if (preview == null) {
                preview = previewWithoutTray;
            }
            createItemInfo.spanY = 1;
            createItemInfo.spanX = 1;
            scale = ((float) this.mLauncher.getDeviceProfile().homeGrid.getIconSize()) / ((float) preview.getWidth());
        }
        this.mLauncher.beginDragFromWidget(imageView, preview, this, createItemInfo, bounds, scale);
        preview.recycle();
    }

    public int getIntrinsicIconSize() {
        return 0;
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (this.mHostViewLoader != null) {
            this.mHostViewLoader.onHostViewDropped();
            this.mHostViewLoader = null;
        }
        if (!success) {
            this.mLauncher.getHomeController().exitDragStateDelayed();
            d.deferDragViewCleanupPostAnimation = false;
        }
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
        return 5;
    }

    public int getOutlineColor() {
        return this.mLauncher.getOutlineColor();
    }

    public Stage getController() {
        return null;
    }

    public int getEmptyCount() {
        return 0;
    }
}
