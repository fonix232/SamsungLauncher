package com.android.launcher3.widget;

import android.annotation.TargetApi;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.PinItemRequestCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.widget.model.WidgetPreviewUtils;
import java.util.ArrayList;
import java.util.UUID;

public class PinItemDragListener implements Parcelable, OnDragListener, DragSource {
    public static final Creator<PinItemDragListener> CREATOR = new Creator<PinItemDragListener>() {
        public PinItemDragListener createFromParcel(Parcel source) {
            return new PinItemDragListener(source);
        }

        public PinItemDragListener[] newArray(int size) {
            return new PinItemDragListener[size];
        }
    };
    public static final String EXTRA_PIN_ITEM_DRAG_LISTENER = "pin_item_drag_listener";
    private static final String MIME_TYPE_PREFIX = "com.android.launcher3.drag_and_drop/";
    private static final String TAG = "PinItemDragListener";
    public static DropCompleteListener mDropCompleteListener;
    private DragManager mDragManager;
    private final String mId;
    private float mLastX;
    private float mLastY;
    private Launcher mLauncher;
    private final int mPreviewBitmapWidth;
    private final Rect mPreviewRect;
    private final int mPreviewViewWidth;
    private final PinItemRequestCompat mRequest;

    public interface DropCompleteListener {
        void onDropComplete();
    }

    public void setOnDropCompleteListener(DropCompleteListener listener) {
        mDropCompleteListener = listener;
    }

    public PinItemDragListener(PinItemRequestCompat request, Rect previewRect, int previewBitmapWidth, int previewViewWidth) {
        this.mLastX = 0.0f;
        this.mLastY = 0.0f;
        this.mRequest = request;
        this.mPreviewRect = previewRect;
        this.mPreviewBitmapWidth = previewBitmapWidth;
        this.mPreviewViewWidth = previewViewWidth;
        this.mId = UUID.randomUUID().toString();
    }

    private PinItemDragListener(Parcel parcel) {
        this.mLastX = 0.0f;
        this.mLastY = 0.0f;
        this.mRequest = (PinItemRequestCompat) PinItemRequestCompat.CREATOR.createFromParcel(parcel);
        this.mPreviewRect = (Rect) Rect.CREATOR.createFromParcel(parcel);
        this.mPreviewBitmapWidth = parcel.readInt();
        this.mPreviewViewWidth = parcel.readInt();
        this.mId = parcel.readString();
    }

    public String getMimeType() {
        return MIME_TYPE_PREFIX + this.mId;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        this.mRequest.writeToParcel(parcel, i);
        this.mPreviewRect.writeToParcel(parcel, i);
        parcel.writeInt(this.mPreviewBitmapWidth);
        parcel.writeInt(this.mPreviewViewWidth);
        parcel.writeString(this.mId);
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
        this.mDragManager = launcher.getDragMgr();
    }

    public boolean onDrag(View view, DragEvent event) {
        if (this.mLauncher == null || this.mDragManager == null) {
            postCleanup();
            return false;
        } else if (event.getAction() != 1) {
            return onDragEvent(event);
        } else {
            if (onDragStart(event)) {
                return true;
            }
            postCleanup();
            return false;
        }
    }

    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case 1:
                this.mLastX = event.getX();
                this.mLastY = event.getY();
                return true;
            case 2:
                this.mLastX = event.getX();
                this.mLastY = event.getY();
                this.mDragManager.onDriverDragMove(event.getX(), event.getY());
                return true;
            case 3:
                this.mLastX = event.getX();
                this.mLastY = event.getY();
                this.mDragManager.onDriverDragMove(event.getX(), event.getY());
                this.mDragManager.onDriverDragEnd(this.mLastX, this.mLastY);
                return true;
            case 4:
                this.mDragManager.onDriverDragCancel();
                return true;
            case 5:
                return true;
            case 6:
                this.mDragManager.onDriverDragExitWindow();
                return true;
            default:
                return false;
        }
    }

    @TargetApi(25)
    private boolean onDragStart(DragEvent event) {
        ClipDescription desc = event.getClipDescription();
        if (desc == null || !desc.hasMimeType(getMimeType())) {
            Log.e(TAG, "Someone started a dragAndDrop before us.");
            return false;
        }
        ItemInfo item = null;
        Bitmap preview = null;
        if (this.mRequest.getRequestType() == 1) {
            Bitmap unbadgedBitmap;
            item = new PendingAddPinShortcutInfo(new PinShortcutRequestActivityInfo(this.mRequest, this.mLauncher));
            LauncherAppState launcherAppState = LauncherAppState.getInstance();
            IconCache cache = LauncherAppState.getInstance().getIconCache();
            Drawable unbadgedDrawable = launcherAppState.getShortcutManager().getShortcutIconDrawable(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()));
            if (unbadgedDrawable == null) {
                unbadgedBitmap = cache.getDefaultIcon(UserHandleCompat.myUserHandle());
            } else {
                unbadgedBitmap = BitmapUtils.createIconBitmap(unbadgedDrawable, this.mLauncher);
            }
            preview = unbadgedBitmap;
        } else if (this.mRequest.getRequestType() == 2) {
            item = new PendingAddWidgetInfo(this.mLauncher, LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, this.mRequest.getAppWidgetProviderInfo(this.mLauncher)), null);
            int[] previewSizeBeforeScale = new int[1];
            preview = WidgetPreviewUtils.getInstance().generateWidgetPreview(this.mLauncher, LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, this.mRequest.getAppWidgetProviderInfo(this.mLauncher)), Math.min((int) (((float) this.mPreviewBitmapWidth) * 1.25f), this.mPreviewViewWidth), previewSizeBeforeScale);
        }
        View view = new View(this.mLauncher);
        view.setTag(item);
        Point downPos = new Point((int) event.getX(), (int) event.getY());
        float scale = 1.0f;
        if (preview != null) {
            scale = ((float) this.mLauncher.getDeviceProfile().homeGrid.getIconSize()) / ((float) preview.getWidth());
        }
        this.mPreviewRect.left = (int) event.getX();
        this.mPreviewRect.top = (int) event.getY();
        this.mLauncher.beginDragFromPinItem(view, preview, this, item, this.mPreviewRect, scale, downPos);
        return true;
    }

    private void postCleanup() {
        if (this.mLauncher != null) {
            Intent newIntent = new Intent(this.mLauncher.getIntent());
            newIntent.removeExtra(EXTRA_PIN_ITEM_DRAG_LISTENER);
            this.mLauncher.setIntent(newIntent);
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                PinItemDragListener.this.removeListener();
            }
        });
    }

    public void removeListener() {
        if (this.mLauncher != null) {
            this.mLauncher.getDragLayer().setOnDragListener(null);
        }
        if (mDropCompleteListener != null) {
            mDropCompleteListener = null;
        }
    }

    public static boolean handleDragRequest(Launcher launcher, Intent intent) {
        if (intent == null || !"android.intent.action.MAIN".equals(intent.getAction())) {
            return false;
        }
        Parcelable dragExtra = intent.getParcelableExtra(EXTRA_PIN_ITEM_DRAG_LISTENER);
        if (!(dragExtra instanceof PinItemDragListener)) {
            return false;
        }
        if (!(launcher.isHomeStage() || launcher.getStageManager() == null)) {
            StageEntry data = new StageEntry();
            data.enableAnimation = false;
            launcher.getStageManager().finishAllStage(data);
        }
        if (launcher.getHomeController() != null) {
            launcher.getHomeController().enterNormalState(false, false);
        }
        PinItemDragListener dragListener = (PinItemDragListener) dragExtra;
        dragListener.setLauncher(launcher);
        launcher.getDragLayer().setOnDragListener(dragListener);
        return true;
    }

    public int getIntrinsicIconSize() {
        return 0;
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (success && target == this.mLauncher.getHomeController().getWorkspace()) {
            if (!success) {
                this.mLauncher.getHomeController().exitDragStateDelayed();
                d.deferDragViewCleanupPostAnimation = false;
            }
            if (mDropCompleteListener != null) {
                mDropCompleteListener.onDropComplete();
            }
            postCleanup();
        }
        if (success) {
            if (d.cancelled && this.mLauncher.isHomeStage() && this.mLauncher.getHomeController() != null) {
                this.mLauncher.getHomeController().exitDragStateDelayed();
            }
            d.deferDragViewCleanupPostAnimation = false;
        }
        if (mDropCompleteListener != null) {
            mDropCompleteListener.onDropComplete();
        }
        postCleanup();
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
        return 7;
    }

    public int getOutlineColor() {
        return this.mLauncher.getResources().getColor(17170443);
    }

    public Stage getController() {
        return null;
    }

    public int getEmptyCount() {
        return 0;
    }
}
