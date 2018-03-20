package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHost;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.PinItemRequestCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.home.InstallShortcutReceiver;
import com.android.launcher3.home.InstallWidgetReceiver;
import com.android.launcher3.util.SecureFolderHelper;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PinItemDragListener;
import com.android.launcher3.widget.PinItemDragListener.DropCompleteListener;
import com.android.launcher3.widget.PinShortcutRequestActivityInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.view.LivePreviewWidgetCell;
import com.android.launcher3.widget.view.WidgetImageView;
import com.sec.android.app.launcher.R;

public class AddItemActivity extends Activity implements OnLongClickListener, OnTouchListener {
    private static final String KEY_ADD_TO_SHORTCUT_PERSONAL = "add_to_shortcut_personal";
    private static final String KEY_DO_NOT_SHOW_POPUP = "do_not_show_popup";
    private static final int REQUEST_BIND_APPWIDGET = 1;
    private static final int SHADOW_SIZE = 10;
    private static final String TAG = "AddItemActivity";
    private static final String[] WHITE_LIST = new String[]{"com.android.server.enterprise.application.ApplicationPolicy", SecureFolderHelper.SECURE_FOLDER_PACKAGE_NAME};
    private TextView mAddItemTextView;
    private LauncherAppState mApp;
    private AppWidgetHost mAppWidgetHost;
    private AppWidgetManagerCompat mAppWidgetManager;
    private final PointF mLastTouchPos = new PointF();
    private int mPendingBindWidgetId;
    private PendingAddWidgetInfo mPendingWidgetInfo;
    private PinShortcutRequestActivityInfo mPinShortcutInfo;
    private PinItemRequestCompat mRequest;
    private String mTextViewBodyForShortcut;
    private String mTextViewBodyForWidget;
    private String mTextViewTitle;
    private LivePreviewWidgetCell mWidgetCell;
    private Bundle mWidgetOptions;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mRequest = PinItemRequestCompat.getPinItemRequest(getIntent());
        if (this.mRequest == null) {
            finish();
            Log.e(TAG, "PinItemRequest is null");
            return;
        }
        this.mApp = LauncherAppState.getInstance();
        if (this.mRequest.getRequestType() == 1) {
            if (this.mRequest.getShortcutInfo() == null) {
                finish();
                Log.d(TAG, "ShortcutInfo of Request is null");
                return;
            } else if (UserHandleCompat.myUserHandle().getUser().equals(this.mRequest.getShortcutInfo().getUserHandle()) || isRequestAddToPersonal()) {
                this.mPinShortcutInfo = new PinShortcutRequestActivityInfo(this.mRequest, this);
                if (isIncludedInWhiteList()) {
                    InstallShortcutReceiver.queuePendingShortcutInfo(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()), this, false);
                    this.mPinShortcutInfo.accept(true);
                    finish();
                    return;
                }
                initView();
                setupShortcut();
            } else {
                finish();
                Log.d(TAG, "UserHandle is different");
                return;
            }
        } else if (this.mRequest.getAppWidgetProviderInfo(this) == null) {
            finish();
            Log.d(TAG, "WidgetProviderInfo of Request is null");
            return;
        } else {
            initView();
            changeWidgetCellLayoutForWidget();
            if (!setupWidget()) {
                finish();
                return;
            }
        }
        sendBroadcast(new Intent("com.samsung.android.multiwindow.MINIMIZE_ALL"));
        this.mWidgetCell.setOnTouchListener(this);
        this.mWidgetCell.setOnLongClickListener(this);
    }

    protected void onPause() {
        super.onPause();
        if (isInMultiWindowMode()) {
            getWindow().addFlags(1048576);
        }
    }

    @TargetApi(25)
    private boolean isIncludedInWhiteList() {
        ShortcutInfo info = this.mRequest.getShortcutInfo();
        if (!(info == null || info.getExtras() == null || ((String) info.getExtras().get(KEY_DO_NOT_SHOW_POPUP)) == null)) {
            for (String pkg : WHITE_LIST) {
                if (pkg.contains((String) info.getExtras().get(KEY_DO_NOT_SHOW_POPUP))) {
                    return true;
                }
            }
        }
        return false;
    }

    @TargetApi(25)
    private boolean isRequestAddToPersonal() {
        ShortcutInfo info = this.mRequest.getShortcutInfo();
        if (info == null || info.getExtras() == null || !info.getExtras().getBoolean(KEY_ADD_TO_SHORTCUT_PERSONAL, false)) {
            return false;
        }
        return true;
    }

    private void initView() {
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
        setContentView(R.layout.add_item_confirmation_activity);
        this.mAddItemTextView = (TextView) findViewById(R.id.add_item_text);
        this.mTextViewTitle = getString(R.string.add_to_home_screen_title);
        this.mTextViewBodyForShortcut = getString(R.string.add_shortcut_request_home_screen);
        this.mTextViewBodyForWidget = getString(R.string.add_widget_request_home_screen);
        this.mWidgetCell = (LivePreviewWidgetCell) findViewById(R.id.widget_cell);
        Button addButton = (Button) findViewById(R.id.add_item_add_button);
        Button cancelButton = (Button) findViewById(R.id.add_item_cancel_button);
        Utilities.setMaxFontScale(getApplicationContext(), addButton);
        Utilities.setMaxFontScale(getApplicationContext(), cancelButton);
        setWidgetCellTitleColor();
        if (Utilities.isEnableBtnBg(this)) {
            cancelButton.setBackgroundResource(R.drawable.tw_text_action_btn_material_light);
            addButton.setBackgroundResource(R.drawable.tw_text_action_btn_material_light);
        }
        if (getIntent() != null && getIntent().getBooleanExtra(PinItemRequestCompat.EXTRA_IS_FROM_LAUNCHER, false)) {
            this.mTextViewTitle = getIntent().getBooleanExtra(PinItemRequestCompat.EXTRA_IS_FROM_WORKSPACE, true) ? getString(R.string.no_space_on_page) : getString(R.string.no_space_on_hotseat);
            if (this.mRequest.getRequestType() == 1) {
                this.mTextViewBodyForShortcut = getString(R.string.add_shortcut_request_different_screen);
            } else {
                this.mTextViewBodyForWidget = getString(R.string.add_widget_request_different_screen);
            }
        }
    }

    private void setWidgetCellTitleColor() {
        TextView textViewName = (TextView) findViewById(R.id.widget_name);
        TextView textViewDims = (TextView) findViewById(R.id.widget_dims);
        int titleColor = getResources().getColor(R.color.apps_screen_grid_text_color, null);
        textViewName.setTextColor(titleColor);
        textViewDims.setTextColor(titleColor);
    }

    private void setupShortcut() {
        this.mAddItemTextView.setText(Utilities.fromHtml(this.mTextViewTitle + "<br />" + this.mTextViewBodyForShortcut));
        this.mWidgetCell.getWidgetView().setTag(new PendingAddPinShortcutInfo(this.mPinShortcutInfo));
        this.mWidgetCell.applyFromShortcutInfo(this.mPinShortcutInfo, this.mApp.getIconCache());
    }

    private boolean setupWidget() {
        this.mAddItemTextView.setText(Utilities.fromHtml(this.mTextViewTitle + "<br />" + this.mTextViewBodyForWidget));
        LauncherAppWidgetProviderInfo widgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this, this.mRequest.getAppWidgetProviderInfo(this));
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);
        this.mAppWidgetHost = new AppWidgetHost(this, 1024);
        this.mPendingWidgetInfo = new PendingAddWidgetInfo(this, widgetInfo, null);
        this.mPendingWidgetInfo.spanX = widgetInfo.getSpanX();
        this.mPendingWidgetInfo.spanY = widgetInfo.getSpanY();
        this.mWidgetOptions = WidgetHostViewLoader.getDefaultOptionsForWidget(this, this.mPendingWidgetInfo);
        this.mWidgetCell.getWidgetView().setTag(this.mPendingWidgetInfo);
        this.mWidgetCell.applyFromAppWidgetProviderInfo(widgetInfo, this.mApp.getWidgetCache());
        this.mWidgetCell.ensurePreview();
        return true;
    }

    private void changeWidgetCellLayoutForWidget() {
        WidgetImageView imagePreview = (WidgetImageView) findViewById(R.id.widget_preview);
        this.mWidgetCell.setMinimumHeight(getResources().getDimensionPixelOffset(R.dimen.widget_cell_widget_min_height));
        LayoutParams param = imagePreview.getLayoutParams();
        param.width = getResources().getDimensionPixelSize(R.dimen.widget_cell_widget_width_size);
        param.height = getResources().getDimensionPixelSize(R.dimen.widget_cell_widget_height_size);
        imagePreview.setLayoutParams(param);
    }

    public void onCancelClick(View v) {
        if (this.mRequest.getRequestType() == 1) {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Shortcut), getResources().getString(R.string.event_Cancel_PinShortcut));
        } else {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Widget), getResources().getString(R.string.event_Cancel_PinShortcut));
        }
        finish();
    }

    @TargetApi(25)
    public void onPlaceAutomaticallyClick(View v) {
        if (this.mRequest.getRequestType() == 1) {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Shortcut), getResources().getString(R.string.event_Add_PinShortcut), 2);
            if (getIntent() == null || !getIntent().getBooleanExtra(PinItemRequestCompat.EXTRA_IS_FROM_LAUNCHER, false)) {
                InstallShortcutReceiver.queuePendingShortcutInfo(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()), this, false);
            } else {
                InstallShortcutReceiver.queuePendingShortcutInfo(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()), this, true);
            }
            this.mPinShortcutInfo.accept(true);
            finish();
            return;
        }
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Widget), getResources().getString(R.string.event_Add_PinShortcut), 2);
        this.mPendingBindWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(this.mPendingBindWidgetId, this.mRequest.getAppWidgetProviderInfo(this), this.mWidgetOptions)) {
            acceptWidget(this.mPendingBindWidgetId);
            return;
        }
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_BIND");
        intent.putExtra(Favorites.APPWIDGET_ID, this.mPendingBindWidgetId);
        intent.putExtra(Favorites.APPWIDGET_PROVIDER, this.mPendingWidgetInfo.componentName);
        intent.putExtra("appWidgetProviderProfile", this.mRequest.getAppWidgetProviderInfo(this).getProfile());
        startActivityForResult(intent, 1);
    }

    private void acceptWidget(int widgetId) {
        InstallWidgetReceiver.queuePendingWidgetInfo(this.mRequest.getAppWidgetProviderInfo(this), widgetId, this);
        this.mWidgetOptions.putInt(Favorites.APPWIDGET_ID, widgetId);
        this.mRequest.accept(this.mWidgetOptions);
        finish();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != 0 || event.getKeyCode() != 66) {
            return super.dispatchKeyEvent(event);
        }
        if (this.mRequest.getRequestType() == 1) {
            InstallShortcutReceiver.queuePendingShortcutInfo(new ShortcutInfoCompat(this.mRequest.getShortcutInfo()), this, false);
            this.mPinShortcutInfo.accept(true);
            finish();
            return true;
        }
        this.mPendingBindWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        if (!this.mAppWidgetManager.bindAppWidgetIdIfAllowed(this.mPendingBindWidgetId, this.mRequest.getAppWidgetProviderInfo(this), this.mWidgetOptions)) {
            return true;
        }
        acceptWidget(this.mPendingBindWidgetId);
        return true;
    }

    public boolean onLongClick(View view) {
        WidgetImageView img = this.mWidgetCell.getWidgetView();
        if (img.getBitmap() != null) {
            Rect bounds = img.getBitmapBounds();
            bounds.offset(img.getLeft() - ((int) this.mLastTouchPos.x), img.getTop() - ((int) this.mLastTouchPos.y));
            PinItemDragListener listener = new PinItemDragListener(this.mRequest, bounds, img.getBitmap().getWidth(), img.getWidth());
            Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setPackage(getPackageName()).putExtra(PinItemDragListener.EXTRA_PIN_ITEM_DRAG_LISTENER, listener);
            listener.setOnDropCompleteListener(new DropCompleteListener() {
                public void onDropComplete() {
                    AddItemActivity.this.finish();
                }
            });
            if (this.mRequest.getRequestType() == 1) {
                SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Shortcut), getResources().getString(R.string.event_Add_PinShortcut), 1);
            } else {
                SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Home_Add_Widget), getResources().getString(R.string.event_Add_PinShortcut), 1);
            }
            startActivity(homeIntent, ActivityOptions.makeCustomAnimation(this, 0, 17432577).toBundle());
            view.startDragAndDrop(new ClipData(new ClipDescription("", new String[]{listener.getMimeType()}), new Item("")), new DragShadowBuilder(view) {
                public void onDrawShadow(Canvas canvas) {
                }

                public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
                    outShadowSize.set(10, 10);
                    outShadowTouchPoint.set(5, 5);
                }
            }, null, 256);
        }
        return false;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        this.mLastTouchPos.set(motionEvent.getX(), motionEvent.getY());
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            int widgetId = data != null ? data.getIntExtra(Favorites.APPWIDGET_ID, this.mPendingBindWidgetId) : this.mPendingBindWidgetId;
            if (resultCode == -1) {
                acceptWidget(widgetId);
                return;
            }
            this.mAppWidgetHost.deleteAppWidgetId(widgetId);
            this.mPendingBindWidgetId = -1;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
