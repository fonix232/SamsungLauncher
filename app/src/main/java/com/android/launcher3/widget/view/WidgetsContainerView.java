package com.android.launcher3.widget.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.InsetDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.BaseContainerView;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.WidgetsModel;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetsListAdapter;
import com.android.launcher3.widget.model.WidgetPreviewLoader;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class WidgetsContainerView extends BaseContainerView implements OnLongClickListener, OnClickListener, DragSource {
    private static final boolean DEBUG = false;
    private static final int PRELOAD_SCREEN_HEIGHT_MULTIPLE = 1;
    private static final String TAG = "WidgetsContainerView";
    private WidgetsListAdapter mAdapter;
    private View mContent;
    private DragManager mDragMgr;
    private IconCache mIconCache;
    private Launcher mLauncher;
    private Rect mPadding;
    private WidgetsRecyclerView mView;
    private Toast mWidgetInstructionToast;
    private WidgetPreviewLoader mWidgetPreviewLoader;

    public WidgetsContainerView(Context context) {
        this(context, null);
    }

    public WidgetsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mPadding = new Rect();
        this.mLauncher = (Launcher) context;
        this.mDragMgr = this.mLauncher.getDragMgr();
        this.mAdapter = new WidgetsListAdapter(context, this, this, this.mLauncher);
        this.mIconCache = LauncherAppState.getInstance().getIconCache();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findViewById(R.id.content);
        this.mView = (WidgetsRecyclerView) findViewById(R.id.widgets_list_view);
        this.mView.setAdapter(this.mAdapter);
        this.mView.setLayoutManager(new LinearLayoutManager(getContext()) {
            protected int getExtraLayoutSpace(State state) {
                return super.getExtraLayoutSpace(state) + (WidgetsContainerView.this.mLauncher.getDeviceProfile().availableHeightPx * 1);
            }
        });
        this.mPadding.set(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    public View getContentView() {
        return this.mView;
    }

    public View getRevealView() {
        return findViewById(R.id.widgets_reveal_view);
    }

    public void scrollToTop() {
        this.mView.scrollToPosition(0);
    }

    public void onClick(View v) {
        if (!this.mLauncher.isRunningAnimation() && (v instanceof WidgetCell)) {
            if (this.mWidgetInstructionToast != null) {
                this.mWidgetInstructionToast.cancel();
            }
            this.mWidgetInstructionToast = Toast.makeText(getContext(), R.string.long_press_widget_to_add, 0);
            this.mWidgetInstructionToast.show();
        }
    }

    public boolean onLongClick(View v) {
        boolean z = false;
        if ((Utilities.ATLEAST_O || v.isInTouchMode()) && !this.mLauncher.isRunningAnimation()) {
            Log.d(TAG, String.format("onLonglick dragging enabled?.", new Object[]{v}));
            if (this.mLauncher.isDraggingEnabled()) {
                z = beginDragging(v);
                if (z && (v.getTag() instanceof PendingAddWidgetInfo)) {
                    PendingAddWidgetInfo info = (PendingAddWidgetInfo) v.getTag();
                    LauncherAppWidgetProviderInfo providerInfo = (LauncherAppWidgetProviderInfo) info.getProviderInfo();
                    info.spanX = providerInfo.getSpanX();
                    info.spanY = providerInfo.getSpanY();
                    info.minSpanX = providerInfo.getMinSpanX();
                    info.minSpanY = providerInfo.getMinSpanY();
                    WidgetHostViewLoader hostLoader = new WidgetHostViewLoader(this.mLauncher, v);
                    boolean preloadStatus = hostLoader.preloadWidget();
                    this.mDragMgr.addDragListener(hostLoader);
                }
            }
        }
        return z;
    }

    private boolean beginDragging(View v) {
        if (v instanceof WidgetCell) {
            WidgetImageView image = (WidgetImageView) v.findViewById(R.id.widget_preview);
            if (image.getBitmap() == null) {
                return false;
            }
            this.mLauncher.getHomeController().enterDragState(true);
            beginDraggingWidget((WidgetCell) v, image);
            return true;
        }
        Log.e(TAG, "Unexpected dragging view: " + v);
        return true;
    }

    private boolean beginDraggingWidget(WidgetCell v, WidgetImageView image) {
        Bitmap preview;
        float scale;
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();
        Rect bounds = image.getBitmapBounds();
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            int[] size = this.mLauncher.getHomeController().getWorkspace().estimateItemSize(createWidgetInfo);
            Bitmap icon = image.getBitmap();
            int[] previewSizeBeforeScale = new int[1];
            preview = getWidgetPreviewLoader().generateWidgetPreview(createWidgetInfo.info, Math.min((int) (((float) icon.getWidth()) * 1.25f), size[0]), null, previewSizeBeforeScale);
            if (previewSizeBeforeScale[0] < icon.getWidth()) {
                int padding = (icon.getWidth() - previewSizeBeforeScale[0]) / 2;
                if (icon.getWidth() > image.getWidth()) {
                    padding = (image.getWidth() * padding) / icon.getWidth();
                }
                bounds.left += padding;
                bounds.right -= padding;
            }
            scale = ((float) bounds.width()) / ((float) preview.getWidth());
        } else {
            preview = BitmapUtils.createIconBitmap(this.mIconCache.getFullResIcon(((PendingAddShortcutInfo) v.getTag()).getActivityInfo()), this.mLauncher);
            createItemInfo.spanY = 1;
            createItemInfo.spanX = 1;
            scale = ((float) this.mLauncher.getDeviceProfile().homeGrid.getIconSize()) / ((float) preview.getWidth());
        }
        this.mLauncher.beginDragFromWidget(image, preview, this, createItemInfo, bounds, scale);
        preview.recycle();
        return true;
    }

    public int getIntrinsicIconSize() {
        return 0;
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (!success) {
            this.mLauncher.getHomeController().exitDragStateDelayed();
        }
        if (!success) {
            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        return 0;
    }

    protected void onUpdateBackgroundAndPaddings(Rect searchBarBounds, Rect padding) {
        this.mContent.setPadding(0, padding.top, 0, padding.bottom);
        InsetDrawable background = new InsetDrawable(getResources().getDrawable(R.drawable.quantum_panel_shape_dark), padding.left, 0, padding.right, 0);
        Rect bgPadding = new Rect();
        background.getPadding(bgPadding);
        this.mView.setBackground(background);
        getRevealView().setBackground(background.getConstantState().newDrawable());
        this.mView.updateBackgroundPadding(bgPadding);
    }

    public void addWidgets(WidgetsModel model) {
        this.mView.setWidgets(model);
        this.mAdapter.setWidgetsModel(model);
        this.mAdapter.notifyDataSetChanged();
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (this.mWidgetPreviewLoader == null) {
            this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return this.mWidgetPreviewLoader;
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
