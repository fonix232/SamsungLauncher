package com.android.launcher3.common.tray;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.tray.TrayManager.TrayLevel;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.home.WorkspaceDragController;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.sec.android.app.launcher.R;
import java.util.Iterator;

public class FakeView extends FrameLayout implements DropTarget {
    private static final int MOVE_STAGE_DELAY = 300;
    private static final String TAG = "Tray.FakeView";
    private int mAnimationDuration;
    private final Alarm mChangeStageAlarm;
    private final OnAlarmListener mChangeStageAlarmListener;
    private ViewPropertyAnimator mDescAnim;
    private TextView mDescText;
    private int mDirection;
    private DragEventCallback mDragEventCallback;
    private DropTarget mDropTarget;
    private View mDropView;
    private boolean mIsDragEntered;
    private boolean mIsDropEnabled;
    private final Launcher mLauncher;
    private boolean mSuppressChangeStage;
    private TrayLevel mTrayLevel;

    interface DragEventCallback {
        DropTarget getDropTarget(TrayLevel trayLevel);

        void onChangeStage(TrayLevel trayLevel, int i);

        void onDragEnter(int i);

        void onDragExit(int i);
    }

    public FakeView(Context context) {
        this(context, null);
    }

    public FakeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FakeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChangeStageAlarm = new Alarm();
        this.mChangeStageAlarmListener = new OnAlarmListener() {
            public void onAlarm(Alarm alarm) {
                if (FakeView.this.mDragEventCallback != null) {
                    FakeView.this.mDragEventCallback.onChangeStage(FakeView.this.mTrayLevel, FakeView.this.mDirection);
                }
            }
        };
        this.mLauncher = (Launcher) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDropView = findViewById(R.id.tray_fake_view_drop);
        this.mDescText = (TextView) findViewById(R.id.tray_fake_view_text);
        this.mAnimationDuration = getResources().getInteger(R.integer.config_homeDragTransitionDuration);
    }

    protected void onDetachedFromWindow() {
        this.mDragEventCallback = null;
        super.onDetachedFromWindow();
    }

    public void setDirection(int direction) {
        this.mDirection = direction;
    }

    public void setTrayLevel(TrayLevel level) {
        this.mTrayLevel = level;
        setTag(level);
        this.mDropTarget = null;
    }

    public void setDescription(CharSequence desc) {
        if (this.mDescText != null) {
            this.mDescText.setText(desc);
        }
    }

    public void setDescriptionHeight(int height) {
        if (this.mDescText != null) {
            LayoutParams lp = this.mDescText.getLayoutParams();
            if (lp != null) {
                lp.height = height;
            }
        }
    }

    private void showDescription(boolean toBeShown, int animDuration) {
        float finalAlpha = toBeShown ? 1.0f : 0.0f;
        if (this.mDescText != null && this.mDescText.getAlpha() != finalAlpha) {
            if (this.mDescAnim != null) {
                this.mDescAnim.cancel();
            }
            if (animDuration > 0) {
                this.mDescAnim = this.mDescText.animate();
                this.mDescAnim.alpha(finalAlpha).setDuration((long) animDuration).start();
                return;
            }
            this.mDescText.setAlpha(finalAlpha);
        }
    }

    public void setDragEventListener(DragEventCallback cb) {
        this.mDragEventCallback = cb;
    }

    private DropTarget getDropTarget() {
        if (this.mDropTarget == null && this.mDragEventCallback != null) {
            this.mDropTarget = this.mDragEventCallback.getDropTarget(this.mTrayLevel);
        }
        return this.mDropTarget;
    }

    public boolean isDragEntered() {
        return this.mIsDragEntered;
    }

    public boolean isDropEnabled(boolean isDrop) {
        return getVisibility() == 0 && (this.mIsDropEnabled || !isDrop);
    }

    public void onDrop(DragObject dragObject) {
        Log.v(TAG, "onDrop " + dragObject);
        this.mChangeStageAlarm.cancelAlarm();
        DropTarget dropTarget = getDropTarget();
        if (dropTarget != null) {
            DragObject d = new DragObject();
            d.dragSource = dragObject.dragSource;
            d.dragView = dragObject.dragView;
            d.dragInfo = dragObject.dragInfo;
            d.extraDragInfoList = dragObject.extraDragInfoList;
            d.extraDragSourceList = dragObject.extraDragSourceList;
            if (dropTarget instanceof WorkspaceDragController) {
                ((WorkspaceDragController) dropTarget).onFlingToMove(d);
            } else {
                dropTarget.onDragEnter(d, false);
                dropTarget.onDragExit(d, false);
                dropTarget.onDrop(d);
            }
        }
        if (dragObject.dragView.hasDrawn()) {
            int[] finalPos = new int[2];
            Rect r = new Rect();
            this.mLauncher.getDragLayer().getViewRectRelativeToSelf(dragObject.dragView, r);
            finalPos[0] = r.left;
            finalPos[1] = r.top;
            this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, finalPos, 0.5f, 0.3f, 0.3f, 0, new Runnable() {
                public void run() {
                    FakeView.this.mChangeStageAlarmListener.onAlarm(null);
                }
            }, DragLayer.ICON_FLICKING_DURATION);
            if (dragObject.extraDragInfoList != null) {
                Iterator it = dragObject.extraDragInfoList.iterator();
                while (it.hasNext()) {
                    this.mLauncher.getDragLayer().animateViewIntoPosition(((DragObject) it.next()).dragView, finalPos, 0.5f, 0.3f, 0.3f, 0, null, DragLayer.ICON_FLICKING_DURATION);
                }
                return;
            }
            return;
        }
        dragObject.deferDragViewCleanupPostAnimation = false;
    }

    public void onDragEnter(DragObject dragObject, boolean dropTargetChanged) {
        Log.v(TAG, "onDragEnter");
        this.mIsDragEntered = true;
        if (this.mSuppressChangeStage) {
            this.mSuppressChangeStage = false;
            return;
        }
        this.mIsDropEnabled = true;
        this.mChangeStageAlarm.cancelAlarm();
        this.mChangeStageAlarm.setOnAlarmListener(this.mChangeStageAlarmListener);
        this.mChangeStageAlarm.setAlarm(300);
        if (this.mDragEventCallback != null) {
            this.mDragEventCallback.onDragEnter(this.mDirection);
        }
    }

    public void onDragOver(DragObject dragObject) {
    }

    public void onDragExit(DragObject dragObject, boolean dropTargetChanged) {
        Log.v(TAG, "onDragExit");
        this.mChangeStageAlarm.cancelAlarm();
        this.mIsDragEntered = false;
        this.mIsDropEnabled = false;
        this.mSuppressChangeStage = false;
        if (this.mDragEventCallback != null) {
            this.mDragEventCallback.onDragExit(this.mDirection);
        }
    }

    public boolean acceptDrop(DragObject dragObject) {
        return true;
    }

    public View getTargetView() {
        return this;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        if (this.mDropView != null) {
            this.mDropView.getGlobalVisibleRect(outRect);
        } else {
            getGlobalVisibleRect(outRect);
        }
    }

    public int getOutlineColor() {
        return this.mLauncher.getOutlineColor();
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == 8 || visibility == 4) {
            this.mIsDragEntered = false;
            this.mIsDropEnabled = false;
            this.mSuppressChangeStage = false;
            if (this.mDescText != null) {
                this.mDescText.setAlpha(1.0f);
            }
        }
    }

    public void setSuppressChangeStage(boolean suppress) {
        if (suppress) {
            this.mChangeStageAlarm.cancelAlarm();
            this.mIsDropEnabled = false;
            this.mSuppressChangeStage = true;
            showDescription(false, 0);
            return;
        }
        this.mSuppressChangeStage = false;
        showDescription(true, this.mAnimationDuration);
    }
}
