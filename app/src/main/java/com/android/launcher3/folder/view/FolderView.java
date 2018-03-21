package com.android.launcher3.folder.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Stats;
import com.android.launcher3.Stats.LaunchSourceProvider;
import com.android.launcher3.Stats.LaunchSourceUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.ItemOperator;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.CellInfo;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.tray.FakeView;
import com.android.launcher3.common.tray.TrayManager;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.DragLayer.LayoutParams;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.IconViewStub;
import com.android.launcher3.folder.FolderEventListener;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.home.CancelDropTarget;
import com.android.launcher3.home.Hotseat;
import com.android.launcher3.home.Workspace;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.FolderStyle;
import com.android.launcher3.theme.ThemeUtils;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.PinnedShortcutUtils;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.animation.SearchedAppBounceAnimation;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
//import com.samsung.android.app.SemColorPickerDialog;
//import com.samsung.android.app.SemColorPickerDialog.OnColorSetListener;
//import com.samsung.android.widget.SemColorPicker.OnColorChangedListener;
//import com.samsung.android.widget.SemHoverPopupWindow;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class FolderView extends FrameLayout implements DragSource, OnClickListener, OnLongClickListener, DropTarget, FolderEventListener, OnEditorActionListener, LaunchSourceProvider {
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final PathInterpolator CUSTOM_FOLDER_INTERPOLATOR = new PathInterpolator(0.8f, 0.0f, 0.67f, 1.0f);
    private static final int DURATION_ADD_APPS_BUTTON_ANIMATION = 200;
    private static final int DURATION_COLOR_BUTTON_APPEAR = 117;
    private static final int DURATION_COLOR_BUTTON_APPEAR_DELAY = 50;
    private static final int DURATION_COLOR_BUTTON_DISMISS = 167;
    private static final int DURATION_COLOR_PICKER_APPEAR = 250;
    private static final int DURATION_COLOR_PICKER_DISMISS = 167;
    private static final int DURATION_FOLDER_NAME_APPEAR = 167;
    private static final int DURATION_FOLDER_NAME_DISMISS = 200;
    private static final int FOLDER_NAME_BAR_COLOR_ALPHA_MASK = 1308622847;
    private static final int FOLDER_NAME_HINT_COLOR_ALPHA_MASK = -1879048193;
    private static final float ICON_OVERSCROLL_WIDTH_FACTOR = 0.45f;
    private static final float ICON_PRESS_ALPHA_VALUE = 0.5f;
    private static final int MIN_CONTENT_DIMEN = 5;
    private static final int ON_EXIT_CLOSE_DELAY = 400;
    private static final String RECENTLY_USED_COLOR = "FolderView.RECENTLY_USED_COLOR";
    private static final String RECENTLY_USED_COLOR_REGEX = ",";
    private static final int REORDER_DELAY = 250;
    public static final int SCROLL_HINT_DURATION = 500;
    static final int STATE_ANIMATING = 1;
    static final int STATE_NONE = -1;
    static final int STATE_OPEN = 2;
    static final int STATE_SMALL = 0;
    private static final String TAG = "FolderView";
    private static SparseArray<Drawable> sColorPickerImages;
    private static String sDefaultFolderName;
    private View mAddButton;
    private Comparator<DragObject> mAscComparator = new Comparator<DragObject>() {
        public int compare(DragObject lhs, DragObject rhs) {
            int lhsRank = Integer.MAX_VALUE;
            int rhsRank = Integer.MAX_VALUE;
            if (lhs.dragInfo instanceof ItemInfo) {
                lhsRank = ((ItemInfo) lhs.dragInfo).rank;
            }
            if (rhs.dragInfo instanceof ItemInfo) {
                rhsRank = ((ItemInfo) rhs.dragInfo).rank;
            }
            return lhsRank - rhsRank;
        }
    };
    private ImageView mBorder;
    private int mBorderHeight;
    private int mBorderWidth;
    private SearchedAppBounceAnimation mBounceAnimation;
    private HashMap<FolderColor, ImageView> mColorPickerItems;
    private View mColorPickerView;
    private FolderPagedView mContent;
    private View mContentContainer;
    private int mContentMinHeight;
    private int mContentMinMargin;
    private int mContentTopMargin;
    private ControllerBase mController;
    private IconInfo mCurrentDragInfo;
    private View mCurrentDragView;
    private int mCurrentScrollDir = -1;
    // TODO: Samsung specific code
//    private SemColorPickerDialog mCustomColorPicker;
//    private OnColorChangedListener mCustomColorPickerColorChangedListener = new OnColorChangedListener() {
//        public void onColorChanged(int color) {
//            FolderView.this.mCustomColorPickerCurrentColor = color;
//        }
//    };
    private int mCustomColorPickerCurrentColor;
    // TODO: Samsung specific code
//    private OnColorSetListener mCustomColorPickerListener = new OnColorSetListener() {
//        public void onColorSet(int color) {
//            String screenID;
//            if (FolderView.this.mCustomColorPickerCurrentColor != color) {
//                color = FolderView.this.mCustomColorPickerCurrentColor;
//            }
//            FolderView.this.mInfo.setOption(8, true, null);
//            FolderView.this.mInfo.color = FolderColor.FOLDER_COLOR_CUSTOM.ordinal();
//            FolderView.this.mFolderColor.put(FolderColor.FOLDER_COLOR_CUSTOM, Integer.valueOf(color));
//            FolderView.this.toggleColorPicker();
//            FolderView.this.setFolderColor(FolderColor.FOLDER_COLOR_CUSTOM, color, true, false);
//            SharedPreferences prefs = FolderView.this.mLauncher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
//            String[] recent_color_list = prefs.getString(FolderView.RECENTLY_USED_COLOR, "").split(FolderView.RECENTLY_USED_COLOR_REGEX);
//            String recent_color = color + FolderView.RECENTLY_USED_COLOR_REGEX;
//            if (!recent_color.isEmpty() && recent_color_list[0].length() > 0) {
//                int i = 0;
//                while (i < recent_color_list.length && i <= 5) {
//                    if (color != Integer.parseInt(recent_color_list[i])) {
//                        recent_color = recent_color + recent_color_list[i] + FolderView.RECENTLY_USED_COLOR_REGEX;
//                    }
//                    i++;
//                }
//            }
//            Editor editor = prefs.edit();
//            editor.putString(FolderView.RECENTLY_USED_COLOR, recent_color.substring(0, recent_color.length() - 1));
//            editor.apply();
//            Resources res = FolderView.this.getResources();
//            if (FolderView.this.mInfo.isContainApps()) {
//                screenID = res.getString(R.string.screen_AppsFolder_Primary);
//            } else {
//                screenID = res.getString(R.string.screen_HomeFolder_Primary);
//            }
//            SALogging.getInstance().insertEventLog(screenID, res.getString(R.string.event_FolderTransparency), Color.alpha(color) != 255 ? 1 : 0);
//        }
//    };
    private boolean mDeleteFolderOnDropCompleted = false;
    private Comparator<DragObject> mDescComparator = new Comparator<DragObject>() {
        public int compare(DragObject lhs, DragObject rhs) {
            int lhsRank = -1;
            int rhsRank = -1;
            if (lhs.dragInfo instanceof ItemInfo) {
                lhsRank = ((ItemInfo) lhs.dragInfo).rank;
            }
            if (rhs.dragInfo instanceof ItemInfo) {
                rhsRank = ((ItemInfo) rhs.dragInfo).rank;
            }
            return rhsRank - lhsRank;
        }
    };
    private boolean mDestroyed;
    private boolean mDragInProgress = false;
    private DragManager mDragMgr;
    private CellLayout mDragTargetLayout;
    private int mEmptyCellRank;
    private int mFadeInOutDuration;
    private HashMap<FolderColor, Integer> mFolderColor;
    private View mFolderContainer;
    private FolderController mFolderController;
    private FolderIconView mFolderIconView;
    private FolderLock mFolderLock;
    private FolderNameEditText mFolderName;
    private ImageView mFolderOptionButton;
    private View mFooter;
    private int mFooterHeight;
    private View mHeader;
    private View mHeaderBottomLine;
    private int mHeaderHeight;
    private boolean mHoverPointClosesFolder = false;
    private FolderInfo mInfo;
    private final InputMethodManager mInputMethodManager;
    private boolean mIsEditingName = false;
    private boolean mItemAddedBackToSelfViaIcon = false;
    private final ArrayList<View> mItemsInReadingOrder = new ArrayList();
    private boolean mItemsInvalidated = false;
    private final Launcher mLauncher;
    private MultiSelectManager mMultiSelectManager;
    private boolean mNeedToShowCursor;
    private final Alarm mOnExitAlarm = new Alarm();
    OnAlarmListener mOnExitAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            FolderView.this.completeDragExit();
        }
    };
    private final Alarm mOnScrollHintAlarm = new Alarm();
    private View mOuterAddButtonContainer;
    private int mOuterAddButtonContainerHeight;
    private int mPageSpacingOnDrag;
    private int mPrevTargetRank;
    private boolean mRearrangeOnClose = false;
    private final Alarm mReorderAlarm = new Alarm();
    OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            FolderView.this.mContent.realTimeReorder(FolderView.this.mEmptyCellRank, FolderView.this.mTargetRank, false);
            FolderView.this.mEmptyCellRank = FolderView.this.mTargetRank;
        }
    };
    private boolean mRestorePositionOnDrop = false;
    private int mScrollAreaOffset;
    private int mScrollHintDir = -1;
    private final Alarm mScrollPauseAlarm = new Alarm();
    private boolean mSuppressFolderClose = false;
    private boolean mSuppressFolderDeletion = false;
    private boolean mSuppressOnAdd = false;
    private boolean mSuppressOnRemove = false;
    private int mTargetRank;
    private int mTargetRankForRestore;
    private int mViewState = -1;

    private static class DropItem {
        DragView dragView;
        boolean fromApps;
        View iconView;
        int targetPageIndex;

        private DropItem() {
        }
    }

    enum FolderColor {
        FOLDER_COLOR_1,
        FOLDER_COLOR_2,
        FOLDER_COLOR_3,
        FOLDER_COLOR_4,
        FOLDER_COLOR_5,
        FOLDER_COLOR_CUSTOM
    }

    private class OnScrollFinishedListener implements OnAlarmListener {
        private final DragObject mDragObject;

        OnScrollFinishedListener(DragObject object) {
            this.mDragObject = object;
        }

        public void onAlarm(Alarm alarm) {
            FolderView.this.onDragOver(this.mDragObject, 1);
        }
    }

    private class OnScrollHintListener implements OnAlarmListener {
        private final DragObject mDragObject;

        OnScrollHintListener(DragObject object) {
            this.mDragObject = object;
        }

        public void onAlarm(Alarm alarm) {
            if (FolderView.this.mCurrentScrollDir == 0) {
                FolderView.this.mContent.scrollLeft();
                FolderView.this.mScrollHintDir = -1;
            } else if (FolderView.this.mCurrentScrollDir == 1) {
                FolderView.this.mContent.scrollRight();
                FolderView.this.mScrollHintDir = -1;
            } else {
                return;
            }
            FolderView.this.mCurrentScrollDir = -1;
            FolderView.this.mScrollPauseAlarm.setOnAlarmListener(new OnScrollFinishedListener(this.mDragObject));
            FolderView.this.mScrollPauseAlarm.setAlarm((long) DragManager.RESCROLL_DELAY);
        }
    }

    private void showSemColorPickerDialog(boolean isRecreate) {
        int currentColor = this.mInfo.color;
        if (!this.mInfo.hasOption(8)) {
            if (currentColor < 0) {
                currentColor = this.mFolderColor.get(FolderColor.values()[0]);
            } else {
                currentColor = this.mFolderColor.get(FolderColor.values()[this.mInfo.color]);
            }
        }
        if (!isRecreate) {
            this.mCustomColorPickerCurrentColor = currentColor;
        }
        String recent_color = this.mLauncher.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getString(RECENTLY_USED_COLOR, "");
        String[] recent_color_list = recent_color.split(RECENTLY_USED_COLOR_REGEX);
        if (recent_color.isEmpty() || recent_color_list.length <= 0) {
            // TODO: Samsung specific code
            //this.mCustomColorPicker = new SemColorPickerDialog(getContext(), this.mCustomColorPickerListener, currentColor);
        } else {
            int[] color_list = new int[recent_color_list.length];
            for (int i = 0; i < color_list.length; i++) {
                try {
                    color_list[i] = Integer.parseInt(recent_color_list[i]);
                } catch (NumberFormatException e) {
                    color_list[i] = -1;
                }
            }
            // TODO: Samsung specific code
            //this.mCustomColorPicker = new SemColorPickerDialog(getContext(), this.mCustomColorPickerListener, currentColor, color_list);
        }
        // TODO: Samsung specific code
//        this.mCustomColorPicker.getColorPicker().setOnColorChangedListener(this.mCustomColorPickerColorChangedListener);
//        this.mCustomColorPicker.create();
//        this.mCustomColorPicker.setNewColor(Integer.valueOf(this.mCustomColorPickerCurrentColor));
//        this.mCustomColorPicker.setTransparencyControlEnabled(true);
//        this.mCustomColorPicker.show();
    }

    public FolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlwaysDrawnWithCacheEnabled(false);
        this.mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Resources res = getResources();
        if (sDefaultFolderName == null) {
            sDefaultFolderName = res.getString(R.string.folder_name);
        }
        if (sColorPickerImages == null) {
            sColorPickerImages = new SparseArray();
        }
        this.mLauncher = (Launcher) context;
        this.mFolderColor = new HashMap();
        FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
        if (fs != null) {
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_1, fs.getCloseFolderColor(0));
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_2, fs.getCloseFolderColor(1));
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_3, fs.getCloseFolderColor(2));
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_4, fs.getCloseFolderColor(3));
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_5, fs.getCloseFolderColor(4));
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_CUSTOM, fs.getCloseFolderColor(0));
        }
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
        this.mFadeInOutDuration = getResources().getInteger(R.integer.config_folderEditTransitionDuration);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mFolderContainer = findViewById(R.id.folder_container);
        this.mContentContainer = findViewById(R.id.folder_content_container);
        this.mContent = findViewById(R.id.folder_content);
        this.mContent.setFolder(this);
        this.mFolderName = findViewById(R.id.folder_name);
        this.mFolderName.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus || FolderView.this.mViewState != 2) {
                    FolderView.this.mInputMethodManager.hideSoftInputFromWindow(FolderView.this.getWindowToken(), 0);
                    if (!v.isInTouchMode()) {
                        FolderView.this.mFolderName.setEllipsize(TruncateAt.END);
                    }
                } else if (FolderView.this.mLauncher.isPaused()) {
                    FolderView.this.mFolderName.setCursorVisible(false);
                } else {
                    if (!v.isInTouchMode()) {
                        FolderView.this.mFolderName.setEllipsize(TruncateAt.START);
                        FolderView.this.mFolderName.setSelection(FolderView.this.mFolderName.getText().length());
                    }
                    FolderView.this.mFolderName.setCursorVisible(true);
                    if (FolderView.this.mNeedToShowCursor || !v.isInTouchMode()) {
                        FolderView.this.startEditingFolderName();
                        FolderView.this.mNeedToShowCursor = false;
                    }
                }
            }
        });
        this.mFolderName.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                FolderView.this.mNeedToShowCursor = true;
                return false;
            }
        });
        this.mFolderName.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!FolderView.this.mInputMethodManager.isActive()) {
                    FolderView.this.mInputMethodManager.restartInput(FolderView.this.mFolderName);
                }
                FolderView.this.mInputMethodManager.viewClicked(FolderView.this.mFolderName);
                // TODO: Samsung specific code
//                if (!FolderView.this.mInputMethodManager.semIsInputMethodShown()) {
//                    FolderView.this.mInputMethodManager.showSoftInput(FolderView.this.mFolderName, 1);
//                }
                FolderView.this.startEditingFolderName();
            }
        });
        this.mFolderName.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (!FolderView.this.isEditingName()) {
                    FolderView.this.startEditingFolderName();
                }
                FolderView.this.mFolderName.requestFocus();
                FolderView.this.mInputMethodManager.showSoftInput(FolderView.this.mFolderName, 0);
                return false;
            }
        });
        this.mFolderName.setOnEventListener(new FolderNameEditText.OnEventListener() {
            boolean mBackKeyPressed = false;

            public boolean onPreImeBackKey() {
                this.mBackKeyPressed = true;
                return false;
            }

            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == 20) {
                    FolderView.this.mContent.setFocusOnFirstChild();
                } else if (keyCode == 22) {
                    FolderView.this.mFolderOptionButton.requestFocus();
                }
            }

            public void onLayoutUpdated() {
                // TODO: Samsung specific code
//                if (!FolderView.this.mInputMethodManager.semIsInputMethodShown() && this.mBackKeyPressed) {
//                    FolderView.this.post(new Runnable() {
//                        public void run() {
//                            FolderView.this.doneEditingFolderName();
//                        }
//                    });
//                }
                this.mBackKeyPressed = false;
            }
        });
        this.mFolderName.setOnEditorActionListener(this);
        this.mFolderName.setSelectAllOnFocus(false);
        this.mFolderName.setFocusableInTouchMode(true);
        this.mFolderName.setInputType(this.mFolderName.getInputType() | 8192);
        this.mFolderName.setFilters(Utilities.getEditTextMaxLengthFilter(getContext(), 30));
        this.mFolderOptionButton = findViewById(R.id.folder_option_btn);
        this.mColorPickerView = findViewById(R.id.folder_colorpicker);
        this.mFolderOptionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (FolderView.this.isEditingName()) {
                    FolderView.this.dismissEditingName();
                }
                FolderView.this.toggleColorPicker();
                Resources res = FolderView.this.getResources();
                SALogging.getInstance().insertEventLog(FolderView.this.mInfo.isContainApps() ? res.getString(R.string.screen_AppsFolder_Primary) : res.getString(R.string.screen_HomeFolder_Primary), FolderView.this.mColorPickerView.getVisibility() == View.VISIBLE ? res.getString(R.string.event_FolderExitColorList) : res.getString(R.string.event_FolderChangeColor));
            }
        });
        // TODO: Samsung specific code
//        try {
//            this.mFolderOptionButton.semSetHoverPopupType(1);
//            SemHoverPopupWindow hover = this.mFolderOptionButton.semGetHoverPopup(true);
//            if (hover != null) {
//                hover.setContent(this.mFolderOptionButton.getContentDescription());
//            }
//        } catch (NoSuchMethodError e) {
//            Log.e(TAG, "Method not found : " + e.toString());
//        }
        this.mHeader = findViewById(R.id.folder_header);
        this.mHeaderBottomLine = findViewById(R.id.bottom_line_color);
        this.mFooter = findViewById(R.id.folder_footer);
        this.mOuterAddButtonContainer = findViewById(R.id.folder_outer_add_button_container);
        this.mBorder = findViewById(R.id.folder_border);
        if (LauncherFeature.supportNavigationBar()) {
            this.mBorder.setContentDescription(getResources().getString(R.string.talkback_tab_back_button_to_close_folder));
        }
        setupFolderLayout();
        this.mAddButton = createAddButton();
        showAddButton(false);
        this.mColorPickerItems = new HashMap();
        this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_1, (ImageView) findViewById(R.id.folder_color_1));
        this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_2, (ImageView) findViewById(R.id.folder_color_2));
        this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_3, (ImageView) findViewById(R.id.folder_color_3));
        this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_4, (ImageView) findViewById(R.id.folder_color_4));
        this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_5, (ImageView) findViewById(R.id.folder_color_5));
        if (LauncherFeature.isSupportFolderColorPicker()) {
            this.mColorPickerItems.put(FolderColor.FOLDER_COLOR_CUSTOM, (ImageView) findViewById(R.id.folder_color_6));
        }
        for (ImageView colorView : this.mColorPickerItems.values()) {
            colorView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    int oldColor = FolderView.this.mInfo.color;
                    FolderColor color = FolderView.this.getFolderColorByView(v);
                    if (color == FolderColor.FOLDER_COLOR_CUSTOM) {
                        FolderView.this.showSemColorPickerDialog(false);
                        return;
                    }
                    FolderView.this.mInfo.setOption(8, false, null);
                    FolderView.this.toggleColorPicker();
                    FolderView.this.setFolderColor(color, true, false);
                    Talk.INSTANCE.say(FolderView.this.getContext().getString(R.string.talkback_changed_to_ps, new Object[]{FolderView.this.getFolderColorDescription(color, OpenThemeManager.getInstance().isDefaultTheme())}));
                    long colorChanged = oldColor != FolderView.this.mInfo.color ? 1 : 0;
                    Resources res = FolderView.this.getResources();
                    SALogging.getInstance().insertEventLog(FolderView.this.mInfo.isContainApps() ? res.getString(R.string.screen_AppsFolder_Primary) : res.getString(R.string.screen_HomeFolder_Primary), res.getString(R.string.event_FolderChangeToColor), colorChanged, String.valueOf(FolderView.this.mInfo.color + 1));
                }
            });
        }
    }

    private ImageView getColorPickerImageView(FolderColor color) {
        if (this.mColorPickerItems.containsKey(color)) {
            return this.mColorPickerItems.get(color);
        }
        return null;
    }

    private void setupFolderLayout() {
        int padding;
        Resources res = getResources();
        int borderMargin = res.getDimensionPixelSize(R.dimen.open_folder_border_margin);
        this.mContentMinHeight = res.getDimensionPixelSize(R.dimen.open_folder_content_min_height);
        this.mContentMinMargin = res.getDimensionPixelSize(R.dimen.open_folder_content_min_margin);
        this.mContentTopMargin = res.getDimensionPixelSize(R.dimen.open_folder_content_margin_top);
        this.mPageSpacingOnDrag = res.getDimensionPixelSize(R.dimen.open_folder_page_spacing_on_drag);
        Point screenSize = new Point();
        Utilities.getScreenSize(getContext(), screenSize);
        int minBorderWidth = screenSize.x - (borderMargin * 2);
        this.mBorder.setMinimumWidth(minBorderWidth);
        this.mHeader.setMinimumWidth(minBorderWidth);
        View headerContent = findViewById(R.id.folder_header_content);
        if (!(headerContent == null || headerContent.getLayoutParams() == null)) {
            MarginLayoutParams lp = (MarginLayoutParams) headerContent.getLayoutParams();
            lp.width = res.getDimensionPixelSize(R.dimen.open_folder_header_content_width);
            lp.height = res.getDimensionPixelSize(R.dimen.open_folder_header_content_height);
        }
        this.mFolderName.setTextSize(0, res.getDimension(R.dimen.open_folder_title_name_text_size_normal));
        View folderNameWrapper = findViewById(R.id.folder_name_wrapper);
        if (!(folderNameWrapper == null || folderNameWrapper.getLayoutParams() == null)) {
            MarginLayoutParams lp = (MarginLayoutParams) folderNameWrapper.getLayoutParams();
            lp.bottomMargin = res.getDimensionPixelSize(R.dimen.open_folder_title_name_margin_bottom);
            lp.setMarginStart(res.getDimensionPixelSize(R.dimen.open_folder_title_name_margin_start));
            lp.setMarginEnd(res.getDimensionPixelSize(R.dimen.open_folder_title_name_margin_end));
            folderNameWrapper.setLayoutParams(lp);
        }
        if (this.mFolderOptionButton.getLayoutParams() != null) {
            MarginLayoutParams lp = (MarginLayoutParams) this.mFolderOptionButton.getLayoutParams();
            if (LauncherFeature.isSupportFolderColorPicker()) {
                lp.width = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_size_new);
                lp.height = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_size_new);
                lp.bottomMargin = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_margin_bottom_new);
                lp.setMarginEnd(res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_margin_end_new));
                padding = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_drawable_padding_new);
            } else {
                lp.width = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_size);
                lp.height = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_size);
                lp.bottomMargin = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_margin_bottom);
                lp.setMarginEnd(res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_margin_end));
                padding = res.getDimensionPixelSize(R.dimen.open_folder_title_option_button_drawable_padding);
            }
            this.mFolderOptionButton.setLayoutParams(lp);
            this.mFolderOptionButton.setPadding(padding, padding, padding, padding);
        }
        if (this.mColorPickerView.getLayoutParams() != null) {
            MarginLayoutParams lp = (MarginLayoutParams) this.mColorPickerView.getLayoutParams();
            if (LauncherFeature.isSupportFolderColorPicker()) {
                lp.setMarginEnd(res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_layout_margin_end_new));
                lp.bottomMargin = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_layout_margin_bottom_new);
            } else {
                lp.setMarginEnd(res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_layout_margin_end));
                lp.bottomMargin = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_layout_margin_bottom);
            }
            this.mColorPickerView.setLayoutParams(lp);
        }
        if (this.mColorPickerItems != null && this.mColorPickerItems.size() > 0) {
            for (ImageView colorView : this.mColorPickerItems.values()) {
                if (!(colorView == null || colorView.getLayoutParams() == null)) {
                    MarginLayoutParams lp = (MarginLayoutParams) colorView.getLayoutParams();
                    if (LauncherFeature.isSupportFolderColorPicker()) {
                        lp.width = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size_new);
                        lp.height = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size_new);
                    } else {
                        lp.width = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size);
                        lp.height = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size);
                    }
                    if (lp.getMarginStart() > 0) {
                        if (LauncherFeature.isSupportFolderColorPicker()) {
                            lp.setMarginStart(res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_gap_x_new));
                        } else {
                            lp.setMarginStart(res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_gap_x));
                        }
                        colorView.setLayoutParams(lp);
                    }
                }
            }
        }
        this.mFooter.setPadding(0, res.getDimensionPixelSize(R.dimen.open_folder_page_indicator_margin_top), 0, 0);
        View pageIndicator = findViewById(R.id.folder_page_indicator);
        if (!(pageIndicator == null || pageIndicator.getLayoutParams() == null)) {
            ((MarginLayoutParams) pageIndicator.getLayoutParams()).height = res.getDimensionPixelSize(R.dimen.open_folder_page_indicator_height);
        }
        this.mHeader.measure(0, 0);
        this.mHeaderHeight = this.mHeader.getMeasuredHeight();
        this.mFooter.measure(0, 0);
        this.mFooterHeight = this.mFooter.getMeasuredHeight();
        TextView addButtonText = findViewById(R.id.folder_add_button_text);
        if (!(addButtonText == null || addButtonText.getLayoutParams() == null)) {
            addButtonText.setTextSize(0, res.getDimension(R.dimen.open_folder_outer_add_button_text_size));
            ((MarginLayoutParams) addButtonText.getLayoutParams()).height = res.getDimensionPixelSize(R.dimen.open_folder_outer_add_button_height);
            padding = res.getDimensionPixelSize(R.dimen.open_folder_outer_add_button_padding);
            addButtonText.setPadding(padding, padding, padding, padding);
        }
        this.mOuterAddButtonContainerHeight = res.getDimensionPixelSize(R.dimen.open_folder_outer_add_button_container_height);
    }

    public void onClick(View v) {
        if (this.mViewState == 2) {
            stopBounceAnimation();
            ItemInfo tag = (ItemInfo)v.getTag();
            if (tag instanceof IconInfo) {
                IconInfo shortcut = (IconInfo)tag;
                if (shortcut.isDisabled == 0 || ((shortcut.isDisabled & -5) & -9) == 0) {
                    CellLayout currentPage = (CellLayout) this.mContent.getChildAt(this.mContent.getCurrentPage());
                    if (currentPage != null && currentPage.isReorderAnimating()) {
                        return;
                    }
                    if (this.mMultiSelectManager == null || !this.mMultiSelectManager.isMultiSelectMode()) {
                        this.mLauncher.startAppShortcutOrInfoActivity(v);
                    } else {
                        ((IconView) v).getCheckBox().toggle();
                    }
                } else if (TextUtils.isEmpty(shortcut.disabledMessage)) {
                    int error = R.string.activity_not_found;
                    if ((shortcut.isDisabled & 1) != 0) {
                        error = R.string.safemode_shortcut_error;
                    }
                    Toast.makeText(this.mLauncher, error, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this.mLauncher, shortcut.disabledMessage, Toast.LENGTH_SHORT).show();
                }
            } else if (v.getId() == R.id.folder_add_button_text) {
                StageEntry data = new StageEntry();
                data.putExtras(AppsPickerController.KEY_PICKER_MODE, 0);
                data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, this.mFolderIconView);
                this.mLauncher.getStageManager().startStage(6, data);
                Resources res = getResources();
                SALogging.getInstance().insertEventLog(this.mInfo.isContainApps() ? res.getString(R.string.screen_AppsFolder_Primary) : res.getString(R.string.screen_HomeFolder_Primary), res.getString(R.string.event_FolderAddApps));
            }
        }
    }

    public boolean onLongClick(View v) {
        boolean z = true;
        if (this.mViewState != 2) {
            return true;
        }
        if (isAllIconViewInflated()) {
            stopBounceAnimation();
            this.mFolderName.clearFocus();
            if (!(this.mController instanceof AppsController) && !this.mLauncher.isDraggingEnabled()) {
                return true;
            }
            if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
                z = false;
            }
            return beginDrag(v, z);
        }
        Log.w(TAG, "onLongClick : all items are not bound yet");
        return true;
    }

    public void onConfigurationChangedIfNeeded() {
        this.mContent.onConfigurationChangedIfNeeded();
        // TODO: Samsung specific code
//        if (this.mCustomColorPicker != null && this.mCustomColorPicker.isShowing()) {
//            this.mCustomColorPicker.dismiss();
//            showSemColorPickerDialog(true);
//        }
        updateFolderLayout();
    }

    private boolean beginDrag(View v, boolean allowQuickOption) {
        ItemInfo tag = (ItemInfo)v.getTag();
        if (tag instanceof IconInfo) {
            IconInfo item = (IconInfo)tag;
            if (!Utilities.ATLEAST_O && !v.isInTouchMode()) {
                return false;
            }
            this.mCurrentDragInfo = item;
            this.mEmptyCellRank = item.rank;
            this.mCurrentDragView = v;
            this.mTargetRankForRestore = item.rank;
            this.mRestorePositionOnDrop = true;
            this.mFolderController.updateCheckBox(false);
            this.mLauncher.beginDragShared(v, this, allowQuickOption, false);
            this.mEmptyCellRank = item.rank;
            this.mContent.removeItem(this.mCurrentDragView);
            this.mInfo.remove(this.mCurrentDragInfo);
            this.mDragInProgress = true;
            this.mItemAddedBackToSelfViaIcon = false;
        }
        if (this.mFolderController != null) {
            this.mFolderController.enterDragState(true);
        }
        return true;
    }

    public void startDrag(CellInfo cellInfo, boolean allowQuickOption) {
        beginDrag(cellInfo.cell, allowQuickOption);
    }

    public boolean isEditingName() {
        return this.mIsEditingName;
    }

    public void startEditingFolderName() {
        this.mFolderName.setHint("");
        this.mIsEditingName = true;
        Resources res = getResources();
        SALogging.getInstance().insertEventLog(this.mInfo.isContainApps() ? res.getString(R.string.screen_AppsFolder_Primary) : res.getString(R.string.screen_HomeFolder_Primary), res.getString(R.string.event_FolderRename));
    }

    public void dismissEditingName() {
        this.mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        doneEditingFolderName();
    }

    private void doneEditingFolderName() {
        this.mFolderName.setHint(getResources().getString(R.string.folder_hint_text));
        String tempTitle = this.mFolderName.getText().toString();
        String newTitle = tempTitle.trim();
        if (!newTitle.equals(tempTitle)) {
            this.mFolderName.setText(newTitle);
        }
        if (!newTitle.equals(this.mInfo.title)) {
            this.mInfo.setTitle(newTitle);
            if (this.mLauncher.isFolderStage() && this.mFolderController != null) {
                this.mFolderController.setIsNeedToUpdateFolderIconView(true);
            }
            this.mFolderIconView.post(new Runnable() {
                public void run() {
                    try {
                        if (FolderView.this.mFolderIconView != null) {
                            FolderView.this.mFolderIconView.applyStyle();
                        }
                        if (FolderView.this.mController instanceof Stage) {
                            ((Stage) FolderView.this.mController).checkIfConfigIsDifferentFromActivity();
                        }
                    } catch (Exception e) {
                        Log.e(FolderView.TAG, e.toString());
                    }
                }
            });
            if (isAppsAlphabeticViewType()) {
                this.mController.onUpdateAlphabetList(this.mInfo);
                ItemInfo item = this.mController.getLocationInfoFromDB(this.mInfo);
                FolderInfo newFolderInfo = new FolderInfo();
                newFolderInfo.copyFrom(item);
                this.mController.updateItemInDb(newFolderInfo);
            } else {
                this.mController.updateItemInDb(this.mInfo);
            }
            int value = GSIMLogging.getInstance().getFolderNameValue(this.mInfo.container);
            if (value != -1) {
                if (this.mInfo.container == -100) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_FOLDER_NAME, String.valueOf(value), -1, true);
                } else if (this.mInfo.container == -102) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_FOLDER_NAME, String.valueOf(value), -1, true);
                }
            }
        }
        this.mFolderName.setFocusableInTouchMode(false);
        this.mFolderName.clearFocus();
        this.mFolderName.setFocusableInTouchMode(true);
        this.mNeedToShowCursor = false;
        Selection.setSelection(this.mFolderName.getText(), 0, 0);
        this.mIsEditingName = false;
        this.mController.notifyCapture(false);
        if (LauncherFeature.supportFolderLock() && this.mFolderLock != null && this.mFolderLock.isFolderLockEnabled()) {
            this.mFolderLock.applyFolderNameChanged();
        }
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != 6) {
            return false;
        }
        dismissEditingName();
        return true;
    }

    public View getEditTextRegion() {
        return this.mFolderName;
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            this.mFolderName.post(new Runnable() {
                public void run() {
                    if (FolderView.this.isEditingName() && FolderView.this.isInTouchMode() && FolderView.this.mInputMethodManager != null) {
                        FolderView.this.mInputMethodManager.showSoftInput(FolderView.this.mFolderName, 1);
                        Log.d(FolderView.TAG, "onWindowFocusChanged : call showSoftInput");
                        return;
                    }
                    FolderView.this.setSuppressFolderNameFocus((long) FolderView.this.mFadeInOutDuration);
                    FolderView.this.mFolderName.clearFocus();
                }
            });
        }
    }

    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent ev) {
        return (ev.getAction() != 0 || handleTouchDown(ev, false)) ? true : true;
    }

    public void setMultiSelectManager(MultiSelectManager multiSelectManager) {
        this.mMultiSelectManager = multiSelectManager;
    }

    public void setDragMgr(DragManager dragMgr) {
        this.mDragMgr = dragMgr;
    }

    public void setFolderIcon(FolderIconView icon) {
        this.mFolderIconView = icon;
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    public FolderInfo getInfo() {
        return this.mInfo;
    }

    public int getFolderState() {
        if (this.mFolderController != null) {
            return this.mFolderController.getState();
        }
        return 0;
    }

    @SuppressLint({"InflateParams"})
    public static FolderView fromXml(Launcher launcher) {
        return (FolderView) launcher.getLayoutInflater().inflate(R.layout.folder, null);
    }

    public void bind(FolderInfo info, ControllerBase controller) {
        Iterator it;
        this.mInfo = info;
        this.mController = controller;
        boolean needToUpdateDb = false;
        if (isAppsAlphabeticViewType()) {
            info.setAlphabeticalOrder(true, false, this.mLauncher);
        } else if (info.getItemCount() > 1) {
            this.mInfo.sortContents();
            int count = 0;
            it = info.contents.iterator();
            while (it.hasNext()) {
                if (((IconInfo) it.next()).rank != count) {
                    needToUpdateDb = true;
                    break;
                }
                count++;
            }
        }
        ArrayList<IconInfo> overflow = this.mContent.bindItems(info.contents);
        Log.i(TAG, "bind : items=" + info.getItemCount() + " , overflow=" + overflow.size() + ", " + info);
        it = overflow.iterator();
        while (it.hasNext()) {
            IconInfo item = (IconInfo) it.next();
            this.mInfo.remove(item);
            this.mController.deleteItemFromDb(item);
        }
        if (getLayoutParams() == null) {
            LayoutParams lp = new LayoutParams(0, 0);
            // TODO: Samsung specific code
            //lp.customPosition = true;
            setLayoutParams(lp);
        }
        centerAboutIcon();
        this.mItemsInvalidated = true;
        if (needToUpdateDb) {
            updateItemLocationsInDatabaseBatch();
        }
        updateContentFocus();
        this.mInfo.addListener(this);
        if (this.mInfo.title == null || sDefaultFolderName.contentEquals(this.mInfo.title)) {
            this.mFolderName.setText("");
        } else {
            this.mFolderName.setText(this.mInfo.title);
        }
        updateFolderColor();
        this.mFolderIconView.post(new Runnable() {
            public void run() {
                if (FolderView.this.getItemCount() <= 1) {
                    FolderView.this.replaceFolderWithFinalItem();
                }
            }
        });
    }

    public void updateFolderColor() {
        if (this.mInfo.hasOption(8)) {
            this.mFolderColor.put(FolderColor.FOLDER_COLOR_CUSTOM, this.mInfo.color);
            setFolderColor(FolderColor.FOLDER_COLOR_CUSTOM, this.mInfo.color, true, true);
            return;
        }
        FolderColor selectedColor = FolderColor.FOLDER_COLOR_1;
        if (this.mInfo.color >= 0 && this.mInfo.color < FolderColor.values().length) {
            selectedColor = FolderColor.values()[this.mInfo.color];
        }
        setFolderColor(selectedColor, false);
    }

    public boolean isAllIconViewInflated() {
        return this.mContent.isAllIconViewInflated();
    }

    void notifyIconViewInflated(int rank) {
        this.mItemsInvalidated = true;
        if (rank < 9) {
            this.mFolderIconView.refreshFolderIcon();
        }
    }

    public void prepareOpen() {
        this.mContent.setActiveMarker(this.mContent.getCurrentPage());
        this.mContent.completePendingPageChanges();
        this.mContent.updateCellDimensions();
        if (!this.mDragInProgress) {
            this.mContent.snapToPageImmediately(0);
        }
        if (!isAllIconViewInflated()) {
            this.mContent.inflateIconViewStubPerPage(0);
            this.mContent.inflateAllIconViewStubsInBackground();
        }
        updateFolderColor();
        updateFolderLayout();
        if (this.mDragMgr.isDragging()) {
            this.mDragMgr.forceTouchMove();
        }
        this.mNeedToShowCursor = false;
        if (this.mFolderName.getVisibility() != 0 || this.mColorPickerView.getVisibility() == 0) {
            this.mFolderName.setEnabled(true);
            this.mFolderName.setAlpha(1.0f);
            this.mFolderName.setVisibility(View.VISIBLE);
            this.mColorPickerView.setVisibility(View.GONE);
        }
        this.mContent.verifyVisibleHighResIcons(this.mContent.getNextPage());
        setFolderContentColor();
        IconInfo item = this.mFolderController.getSearchedAppInfo();
        if (item != null) {
            this.mContent.snapToPageImmediately(item.rank / this.mContent.itemsPerPage());
            View view = getViewForInfo(item);
            stopBounceAnimation();
            startBounceAnimationForSearchedApp(view);
            this.mFolderController.setSearchedAppInfo(null);
        } else {
            showAddButton(false);
        }
        setVisibility(4);
        setTranslationX(0.0f);
        setTranslationY(0.0f);
        setScaleX(1.0f);
        setScaleY(1.0f);
        this.mViewState = 0;
    }

    public void onOpen(Animator openFolderAnim) {
        if (getParent() instanceof DragLayer) {
            Log.d(TAG, "onOpen : " + this.mInfo);
            if (openFolderAnim != null) {
                openFolderAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animation) {
                        FolderView.this.sendCustomAccessibilityEvent(32, FolderView.this.mContent.getAccessibilityDescription());
                        FolderView.this.mViewState = 1;
                    }

                    public void onAnimationEnd(Animator animation) {
                        FolderView.this.mViewState = 2;
                        FolderView.this.mContent.setFocusOnFirstChild();
                    }
                });
                return;
            }
            this.mViewState = 2;
            this.mContent.setFocusOnFirstChild();
        }
    }

    public void onClose(Animator closeFolderAnim) {
        if (getParent() instanceof DragLayer) {
            if (isEditingName()) {
                dismissEditingName();
            }
            if (LauncherFeature.isTablet()) {
                this.mFolderName.setVisibility(4);
            }
            stopBounceAnimation();
            if (closeFolderAnim != null) {
                closeFolderAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        FolderView.this.mViewState = 0;
                        FolderView.this.onCloseComplete();
                    }

                    public void onAnimationStart(Animator animation) {
                        FolderView.this.sendCustomAccessibilityEvent(32, FolderView.this.getContext().getString(R.string.folder_closed));
                        FolderView.this.mViewState = 1;
                    }
                });
                return;
            }
            this.mViewState = 0;
            onCloseComplete();
        }
    }

    public void setFolderContentColor() {
        int bgColor = getOutlineColor();
        ColorFilter filter = new LightingColorFilter(bgColor, 0);
        this.mBorder.setImageDrawable(getResources().getDrawable(isWhiteBg() ? R.drawable.page_view_overlay_select_03_w : R.drawable.page_view_overlay_select_03, null));
        this.mFolderName.setTextColor(bgColor);
        this.mFolderName.setHintTextColor(FOLDER_NAME_HINT_COLOR_ALPHA_MASK & bgColor);
        this.mHeaderBottomLine.setBackgroundColor(FOLDER_NAME_BAR_COLOR_ALPHA_MASK & bgColor);
        this.mContent.onChangeFolderIconTextColor();
        if (this.mAddButton != null) {
            TextView addButtonText = this.mAddButton.findViewById(R.id.folder_add_button_text);
            if (addButtonText != null) {
                addButtonText.setTextColor(bgColor);
                if (addButtonText.getCompoundDrawablesRelative()[0] == null) {
                    return;
                }
                if (isWhiteBg()) {
                    addButtonText.getCompoundDrawablesRelative()[0].setColorFilter(filter);
                } else {
                    addButtonText.getCompoundDrawablesRelative()[0].clearColorFilter();
                }
            }
        }
    }

    private void sendCustomAccessibilityEvent(int type, String text) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(type);
            onInitializeAccessibilityEvent(event);
            event.getText().add(text);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32768) {
            super.onInitializeAccessibilityEvent(event);
        }
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == 32768) {
            sendAccessibilityEvent(65536);
        } else {
            super.onPopulateAccessibilityEvent(event);
        }
    }

    private CellLayout getCurrentDropLayout() {
        return (CellLayout) this.mContent.getChildAt(this.mContent.getNextPage());
    }

    private void setCurrentDropLayout(CellLayout layout) {
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragExit();
        }
        this.mDragTargetLayout = layout;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragEnter();
        }
    }

    public boolean isDropEnabled(boolean isDrop) {
        return true;
    }

    public void onDragEnter(DragObject d, boolean dropTargetChanged) {
        this.mPrevTargetRank = -1;
        this.mOnExitAlarm.cancelAlarm();
        this.mScrollAreaOffset = (d.dragView.getDragRegionWidth() / 2) - d.xOffset;
        if (this.mCurrentDragInfo == null) {
            this.mEmptyCellRank = this.mContent.allocateRankForNewItem(false);
        }
        setCurrentDropLayout(getCurrentDropLayout());
        if (this.mFolderController != null && this.mFolderController.getFolderBgView() != null) {
            this.mFolderController.getFolderBgView().onMoveInFolder();
        }
    }

    public void onDragOver(DragObject d) {
        onDragOver(d, Callback.DEFAULT_SWIPE_ANIMATION_DURATION);
    }

    private void onDragOver(DragObject d, int reorderDelay) {
        if (this.mViewState == 2) {
            float[] r = new float[2];
            this.mTargetRank = getTargetRank(d, r);
            DragView dragView = d.dragView;
            if (isAppsAlphabeticViewType()) {
                this.mTargetRank = this.mEmptyCellRank;
            } else {
                if (this.mTargetRank != this.mPrevTargetRank) {
                    this.mReorderAlarm.cancelAlarm();
                    this.mReorderAlarm.setOnAlarmListener(this.mReorderAlarmListener);
                    this.mReorderAlarm.setAlarm((long) reorderDelay);
                    this.mPrevTargetRank = this.mTargetRank;
                }
                if (this.mRestorePositionOnDrop) {
                    if (this.mCurrentDragInfo == null) {
                        this.mRestorePositionOnDrop = false;
                    } else if (this.mTargetRankForRestore != this.mTargetRank) {
                        this.mRestorePositionOnDrop = false;
                    }
                }
                CellLayout layout = getCurrentDropLayout();
                if (layout != this.mDragTargetLayout) {
                    setCurrentDropLayout(layout);
                }
                int pagePos = this.mTargetRank % this.mContent.itemsPerPage();
                this.mDragTargetLayout.visualizeDropLocation((ItemInfo) d.dragInfo, dragView.getDragOutline(), pagePos % layout.getCountX(), pagePos / layout.getCountX(), 1, 1, false);
            }
            if (!this.mScrollPauseAlarm.alarmPending()) {
                float x = r[0];
                int currentPageIndex = this.mContent.getNextPage();
                CellLayout currentPage = this.mContent.getCurrentCellLayout();
                if (currentPage != null) {
                    float cellOverlap = ((float) currentPage.getCellWidth()) * ICON_OVERSCROLL_WIDTH_FACTOR;
                    boolean isOutsideLeftEdge = x < cellOverlap;
                    boolean isOutsideRightEdge = x > ((float) getWidth()) - cellOverlap;
                    if (currentPageIndex > 0 && (!Utilities.sIsRtl ? isOutsideLeftEdge : isOutsideRightEdge)) {
                        showScrollHint(0, d);
                    } else if (currentPageIndex >= this.mContent.getPageCount() - 1 || (Utilities.sIsRtl ? isOutsideLeftEdge : isOutsideRightEdge)) {
                        this.mOnScrollHintAlarm.cancelAlarm();
                        if (this.mScrollHintDir != -1) {
                            this.mContent.clearScrollHint();
                            this.mScrollHintDir = -1;
                        }
                    } else {
                        showScrollHint(1, d);
                    }
                }
                if (this.mSuppressFolderClose) {
                    float touchY = (dragView.getTranslationY() + ((float) dragView.getRegistrationY())) - dragView.getOffsetY();
                    if (touchY >= ((float) (getTop() + this.mHeaderHeight)) && touchY <= ((float) (getHeight() + getTop()))) {
                        this.mSuppressFolderClose = false;
                        if (this.mFolderController != null) {
                            this.mFolderController.showFolderBgView(true, false);
                        }
                    }
                }
            }
        }
    }

    public void onDragExit(DragObject d, boolean dropTargetChanged) {
        if (!d.dragComplete && this.mViewState == 2) {
            this.mOnExitAlarm.setOnAlarmListener(this.mOnExitAlarmListener);
            this.mOnExitAlarm.setAlarm(400);
        }
        this.mReorderAlarm.cancelAlarm();
        this.mOnScrollHintAlarm.cancelAlarm();
        this.mScrollPauseAlarm.cancelAlarm();
        if (this.mScrollHintDir != -1) {
            this.mContent.clearScrollHint();
            this.mScrollHintDir = -1;
        }
        setCurrentDropLayout(null);
        if (dropTargetChanged && !d.dragComplete && this.mViewState == 2 && this.mFolderController != null) {
            int direction = 0;
            DragView dragView = d.dragView;
            FolderBgView folderBgView = this.mFolderController.getFolderBgView();
            if (!(dragView == null || folderBgView == null)) {
                if (((int) ((dragView.getTranslationY() + ((float) dragView.getRegistrationY())) - dragView.getOffsetY())) < Utilities.getFullScreenHeight(this.mLauncher) / 2) {
                    folderBgView.onMoveFromFolderTop();
                    direction = 0;
                } else {
                    folderBgView.onMoveFromFolderBottom();
                    direction = 1;
                }
            }
            SALogging.getInstance().insertMoveFromFolderLog(this.mInfo.container, d.extraDragInfoList != null, direction, d);
        }
    }

    public boolean acceptDrop(DragObject d) {
        int itemType = d.dragInfo.itemType;
        if ((itemType != 0 && itemType != 1 && itemType != 6 && itemType != 7 && !this.mLauncher.getMultiSelectManager().acceptDropToFolder()) || isFull() || this.mRestorePositionOnDrop) {
            return false;
        }
        return true;
    }

    public void onDrop(DragObject d) {
        View currentDragView;
        boolean hasMovedLayout;
        if (!(this.mContent.rankOnCurrentPage(this.mEmptyCellRank) || isAppsAlphabeticViewType())) {
            this.mTargetRank = getTargetRank(d, null);
            this.mReorderAlarmListener.onAlarm(this.mReorderAlarm);
            this.mOnScrollHintAlarm.cancelAlarm();
            this.mScrollPauseAlarm.cancelAlarm();
        }
        this.mContent.completePendingPageChanges();
        ItemInfo info = null;
        boolean needClone = false;
        boolean isAcceptItem = false;
        if (this.mCurrentDragInfo == null) {
            boolean isInApps = this.mInfo.container == -102;
            if (d.dragInfo instanceof FolderInfo) {
                d.cancelled = true;
                d.cancelDropFolder = true;
                this.mLauncher.getDragLayer().removeView(d.dragView);
                if (d.extraDragInfoList != null) {
                    onDropExtraObjects(d.extraDragInfoList, false, true);
                }
                if (this.mFolderController != null) {
                    this.mFolderController.enterNormalState(true);
                    return;
                }
                return;
            }
            ItemInfo info2;
            if (d.dragInfo instanceof PendingAddPinShortcutInfo) {
                LauncherAppsCompat.acceptPinItemRequest(this.mLauncher, ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat(), 0);
                isAcceptItem = true;
                ItemInfo shortcutInfo = ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().createShortcutInfo();
                if (shortcutInfo != null) {
                    info = shortcutInfo;
                }
                info2 = info;
            } else {
                info2 = (ItemInfo)d.dragInfo;
            }
            if (info2 == null) {
                Log.e(TAG, "onDrop() info is null");
                info = info2;
                return;
            }
            int dragSourceType = d.dragSource.getDragSourceType();
            needClone = (isInApps && (dragSourceType == 0 || dragSourceType == 2)) || (!isInApps && (dragSourceType == 1 || dragSourceType == 4));
            if (needClone) {
                switch (info2.itemType) {
                    case 0:
                        info = info2.makeCloneInfo();
                        break;
                    case 1:
                        ItemInfo iconInfo = new IconInfo(this.mLauncher, (IconInfo) info2);
                        break;
                    default:
                        throw new IllegalStateException("Not supported item type: " + info2.itemType);
                }
            }
            info = (ItemInfo)info2;
            currentDragView = this.mContent.createNewView(info);
            hasMovedLayout = true;
        } else {
            info = this.mCurrentDragInfo;
            currentDragView = this.mCurrentDragView;
            hasMovedLayout = false;
            if (this.mCurrentDragInfo.rank / this.mContent.itemsPerPage() != this.mTargetRank / this.mContent.itemsPerPage()) {
                SALogging.getInstance().insertFolderMoveAppLogs(this.mLauncher, d.extraDragInfoList != null);
            }
        }
        this.mContent.addViewForRank(currentDragView, info, this.mEmptyCellRank);
        if (d.dragView.hasDrawn()) {
            float scaleX = getScaleX();
            float scaleY = getScaleY();
            setScaleX(1.0f);
            setScaleY(1.0f);
            this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, currentDragView, 300, null, this.mContent);
            setScaleX(scaleX);
            setScaleY(scaleY);
        } else {
            d.deferDragViewCleanupPostAnimation = false;
            currentDragView.setVisibility(View.VISIBLE);
        }
        if (this.mCurrentDragInfo == null) {
            Talk.INSTANCE.say(getContext().getString(R.string.tts_item_moved_into_folder));
        }
        this.mItemsInvalidated = true;
        rearrangeChildren();
        this.mSuppressOnAdd = true;
        this.mInfo.add((IconInfo) info);
        this.mSuppressOnAdd = false;
        this.mCurrentDragInfo = null;
        this.mDragInProgress = false;
        if (d.extraDragInfoList != null) {
            onDropExtraObjects(d.extraDragInfoList, needClone, false);
        }
        if (hasMovedLayout) {
            this.mController.addOrMoveItemInDb(info, this.mInfo.id, 0, info.cellX, info.cellY, info.rank);
            updateItemLocationsInDatabaseBatch();
        }
        if (this.mFolderController != null) {
            this.mFolderController.enterNormalState(true);
        }
        if (info.itemType == 6) {
            PinnedShortcutUtils.acceptPinItemInfo(d, info, isAcceptItem);
        }
        if (isAcceptItem) {
            PinnedShortcutUtils.unpinShortcutIfAppTarget(new ShortcutInfoCompat(((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().getPinItemRequestCompat().getShortcutInfo()), this.mLauncher);
        }
    }

    private void onDropExtraObjects(ArrayList<DragObject> extraDragObjects, boolean clone, boolean isFolderDrop) {
        ArrayList<IconInfo> items = new ArrayList();
        ArrayList<DropItem> dropItems = new ArrayList();
        ArrayList<DropItem> urgentItems = new ArrayList();
        boolean addItemFromApps = false;
        this.mSuppressOnAdd = true;
        Iterator it = extraDragObjects.iterator();
        while (it.hasNext()) {
            DropItem dropItem;
            DragObject d = (DragObject) it.next();
            if (d.dragInfo instanceof FolderInfo) {
                d.cancelled = true;
                d.cancelDropFolder = true;
                d.deferDragViewCleanupPostAnimation = false;
            } else {
                View view;
                if (!isFolderDrop) {
                    this.mTargetRank++;
                }
                this.mContent.completePendingPageChanges();
                IconInfo info = clone ? ((IconInfo) d.dragInfo).makeCloneInfo() : (IconInfo) d.dragInfo;
                if (equals(d.dragSource)) {
                    view = d.dragView.getSourceView();
                } else {
                    view = this.mContent.createNewView(info, this.mContent.isInActiveRange(this.mTargetRank));
                }
                if (info.container == -102) {
                    addItemFromApps = true;
                }
                this.mContent.insertViewBeforeArrangeChildren(view, info, this.mTargetRank);
                this.mController.addOrMoveItemInDb(info, this.mInfo.id, 0, info.cellX, info.cellY, -1);
                dropItem = new DropItem();
                dropItem.dragView = d.dragView;
                dropItem.iconView = view;
                dropItem.targetPageIndex = this.mTargetRank / this.mContent.itemsPerPage();
                if (extraDragObjects.get(extraDragObjects.size() - 1).equals(d)) {
                    dropItem.fromApps = addItemFromApps;
                }
                if (dropItem.targetPageIndex == this.mContent.getCurrentPage()) {
                    urgentItems.add(dropItem);
                }
                dropItems.add(dropItem);
                items.add(info);
            }
        }
        if (!items.isEmpty()) {
            this.mInfo.add(items);
        }
        this.mSuppressOnAdd = false;
        rearrangeChildren();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        int currentPageIndex = this.mContent.getNextPage();
        Iterator it2 = dropItems.iterator();
        while (it2.hasNext()) {
            int translatedX;
            final DropItem dropItem = (DropItem) it2.next();
            final View iconView = dropItem.iconView;
            View page = this.mContent.getPageAt(dropItem.targetPageIndex);
            if (page != null) {
                int expectedX = (dropItem.targetPageIndex - currentPageIndex) * this.mContent.getDesiredWidth();
                Rect pageRect = new Rect();
                dragLayer.getViewRectRelativeToSelf(page, pageRect);
                translatedX = expectedX - pageRect.left;
            } else {
                translatedX = this.mContent.getDesiredWidth();
            }
            final ArrayList<DropItem> arrayList = urgentItems;
            final ArrayList<IconInfo> arrayList2 = items;
            Runnable onFinishAnimationRunnable = new Runnable() {
                public void run() {
                    if (arrayList.contains(dropItem)) {
                        arrayList.remove(dropItem);
                    }
                    if (iconView instanceof IconViewStub) {
                        IconViewStub stub = (IconViewStub)iconView;
                        stub.inflateInBackgroundUrgent((IconInfo) stub.getTag());
                    }
                    if (dropItem.fromApps && FolderView.this.isAppsAlphabeticViewType()) {
                        Iterator it = arrayList2.iterator();
                        while (it.hasNext()) {
                            ((IconInfo) it.next()).mDirty = true;
                        }
                        Log.d(FolderView.TAG, "onDropExtraObjects notifyFolderItemsChanged to apps");
                        FolderView.this.notifyFolderItemsChanged();
                    }
                }
            };
            if (!(dropItem.dragView == null || dropItem.iconView == null)) {
                dragLayer.animateViewIntoPosition(dropItem.dragView, dropItem.iconView, urgentItems.contains(dropItem) ? 300 : -1, onFinishAnimationRunnable, null, translatedX);
            }
        }
    }

    public View getTargetView() {
        return this;
    }

    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
        if (this.mSuppressFolderClose) {
            int screenHeight = Utilities.getFullScreenHeight(this.mLauncher);
            outRect.top = 0;
            if (outRect.bottom < screenHeight) {
                outRect.bottom = screenHeight;
                return;
            }
            return;
        }
        outRect.top += this.mHeaderHeight;
    }

    private int getTargetRank(DragObject d, float[] recycle) {
        recycle = d.getVisualCenter(recycle);
        return this.mContent.findNearestArea(((int) recycle[0]) - getPaddingLeft(), (((int) recycle[1]) - getPaddingTop()) - this.mHeaderHeight);
    }

    private void showScrollHint(int direction, DragObject d) {
        if (this.mScrollHintDir != direction) {
            this.mContent.showScrollHint(direction);
            this.mScrollHintDir = direction;
        }
        if (!this.mOnScrollHintAlarm.alarmPending() || this.mCurrentScrollDir != direction) {
            this.mCurrentScrollDir = direction;
            this.mOnScrollHintAlarm.cancelAlarm();
            this.mOnScrollHintAlarm.setOnAlarmListener(new OnScrollHintListener(d));
            this.mOnScrollHintAlarm.setAlarm(500);
            this.mReorderAlarm.cancelAlarm();
            this.mTargetRank = this.mEmptyCellRank;
        }
    }

    public void completeDragExit() {
        completeDragExit(true);
    }

    private void completeDragExit(boolean dropComplete) {
        if (this.mInfo.opened) {
            StageEntry data = new StageEntry();
            if (this.mDragMgr.isDragging() && dropComplete) {
                if (this.mController instanceof AppsController) {
                    data.setInternalStateTo(1);
                } else {
                    data.setInternalStateTo(2);
                }
                data.putExtras(TrayManager.KEY_SUPPRESS_CHANGE_STAGE_ONCE, 1);
            }
            if (this.mLauncher.isFolderStage()) {
                this.mLauncher.getStageManager().finishStage(5, data);
            }
            this.mRearrangeOnClose = true;
        } else if (this.mViewState == 1) {
            this.mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
            clearDragInfo();
        }
    }

    private void clearDragInfo() {
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;
    }

    public int getIntrinsicIconSize() {
        return this.mLauncher.getDeviceProfile().folderGrid.getIconSize();
    }

    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (this.mFolderController != null) {
            this.mFolderController.enterNormalState(true);
        }
        boolean needToMakeClone = isNeedToMakeClone(target);
        if (!success || needToMakeClone) {
            View icon;
            IconInfo info = (IconInfo)d.dragInfo;
            if (this.mCurrentDragView == null || this.mCurrentDragView.getTag() != info) {
                icon = this.mContent.createNewView(info);
            } else {
                icon = this.mCurrentDragView;
            }
            ArrayList<View> views = getItemsInReadingOrder();
            if (info.rank > views.size()) {
                info.rank = views.size();
            }
            views.add(info.rank, icon);
            this.mContent.arrangeChildren(views, views.size());
            this.mItemsInvalidated = true;
            this.mTargetRank = info.rank;
            if (!needToMakeClone) {
                this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, icon, null, this.mContent.getPageAt(info.rank / this.mContent.itemsPerPage()));
            }
            this.mSuppressOnAdd = true;
            this.mInfo.add(info);
            this.mSuppressOnAdd = false;
        } else if (!(!this.mDeleteFolderOnDropCompleted || this.mItemAddedBackToSelfViaIcon || target == this)) {
            boolean dropToFolder = false;
            if (d.dragInfo instanceof IconInfo) {
                dropToFolder = ((IconInfo) d.dragInfo).container > 0;
            }
            if (!((target instanceof Workspace) || (target instanceof Hotseat)) || dropToFolder) {
                replaceFolderWithFinalItem();
            }
        }
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing()) {
            this.mLauncher.getQuickOptionManager().startBounceAnimation();
        }
        this.mDragInProgress = false;
        if (target != this && (this.mOnExitAlarm.alarmPending() || this.mInfo.opened)) {
            this.mOnExitAlarm.cancelAlarm();
            if (!success) {
                this.mSuppressFolderDeletion = true;
            }
            this.mScrollPauseAlarm.cancelAlarm();
            completeDragExit(success);
        }
        this.mDeleteFolderOnDropCompleted = false;
        this.mItemAddedBackToSelfViaIcon = false;
        this.mCurrentDragInfo = null;
        this.mCurrentDragView = null;
        this.mSuppressOnAdd = false;
        if (!(this.mDestroyed || this.mInfo.isAlphabeticalOrder())) {
            updateItemLocationsInDatabaseBatch();
        }
        if (success && LauncherFeature.supportFolderLock() && this.mFolderLock != null) {
            this.mFolderLock.applyFolderNameChanged();
            FolderInfo folder = ((FolderView) d.dragSource).getInfo();
            if (this.mFolderLock.isFolderLockEnabled() && folder.isLocked() && (d.dragInfo instanceof ItemInfo) && target != this) {
                this.mFolderLock.showItemDropedConfirmDialog((ItemInfo) d.dragInfo);
            }
        }
    }

    public int getQuickOptionFlags(DragObject dragObject) {
        if (dragObject == null) {
            return 0;
        }
        boolean app;
        boolean homeFolder;
        if (dragObject.dragInfo.itemType == 0) {
            app = true;
        } else {
            app = false;
        }
        if (dragObject.dragSource.getDragSourceType() == 3) {
            homeFolder = true;
        } else {
            homeFolder = false;
        }
        boolean homeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        int optionFlags = 0 | 1;
        if (homeFolder) {
            optionFlags |= 2;
        }
        optionFlags |= 32;
        if (app) {
            optionFlags |= 64;
        }
        if (app) {
            optionFlags |= 128;
        }
        if (app) {
            optionFlags |= 256;
        }
        if (app) {
            optionFlags |= 512;
        }
        optionFlags |= 1024;
        if (app) {
            optionFlags |= 16384;
        }
        if (app) {
            optionFlags |= 32768;
        }
        optionFlags |= 16;
        if (!homeFolder) {
            optionFlags |= 4;
        }
        if (homeOnlyMode || !homeFolder) {
            optionFlags |= 4096;
        }
        if (homeOnlyMode || !homeFolder) {
            optionFlags |= 8192;
        }
        if (LauncherFeature.supportSetToZeroPage()) {
            optionFlags |= 65536;
        }
        return optionFlags;
    }

    public void onExtraObjectDragged(ArrayList<DragObject> extraDragObjects) {
        if (extraDragObjects != null) {
            int lastRank;
            ArrayList<DragObject> sortedList = new ArrayList(extraDragObjects);
            Collections.sort(sortedList, this.mDescComparator);
            this.mSuppressOnRemove = true;
            int count = 0;
            Iterator it = sortedList.iterator();
            while (it.hasNext()) {
                DragObject d = (DragObject) it.next();
                View sourceView = d.dragView.getSourceView();
                IconInfo info = d.dragInfo;
                if (sourceView != null) {
                    int removedAppRank = info.rank;
                    lastRank = this.mInfo.getItemCount() - 1;
                    boolean reorderImmediately = count < sortedList.size() + -1;
                    this.mContent.removeItem(sourceView);
                    this.mInfo.remove(info);
                    this.mContent.realTimeReorder(removedAppRank, lastRank, reorderImmediately);
                }
                count++;
            }
            this.mSuppressOnRemove = false;
            ArrayList<View> views = getItemsInReadingOrder();
            if (!(this.mCurrentDragInfo == null || this.mCurrentDragInfo.rank == this.mEmptyCellRank || this.mCurrentDragInfo.rank >= views.size())) {
                lastRank = views.size() - 1;
                int adjustedRank = this.mEmptyCellRank > lastRank ? lastRank : this.mEmptyCellRank;
                views.add(adjustedRank, views.remove(this.mCurrentDragInfo.rank));
                this.mContent.realTimeReorder(this.mCurrentDragInfo.rank, adjustedRank, false);
            }
            this.mContent.arrangeChildren(views, views.size());
        }
    }

    public void onExtraObjectDropCompleted(View target, ArrayList<DragObject> succeedDragObjects, ArrayList<DragObject> failedDragObjects, int fullCnt) {
        ArrayList<DragObject> dragObjectsToRestore = new ArrayList();
        boolean needToMakeClone = isNeedToMakeClone(target);
        if (succeedDragObjects != null && needToMakeClone) {
            dragObjectsToRestore.addAll(succeedDragObjects);
        }
        if (failedDragObjects != null) {
            dragObjectsToRestore.addAll(failedDragObjects);
        }
        if (!this.mDestroyed) {
            restoreDragObjectsPosition(dragObjectsToRestore, !needToMakeClone);
        }
    }

    public int getPageIndexForDragView(ItemInfo item) {
        return this.mController.getPageIndexForDragView(item);
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> list = getItemsInReadingOrder();
        ArrayList<ItemInfo> items = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            if (v.getTag() instanceof ItemInfo) {
                ItemInfo info = (ItemInfo) v.getTag();
                info.rank = i;
                items.add(info);
            }
        }
        this.mController.modifyItemsInDb(items, this.mInfo.id, 0);
    }

    private void restoreDragObjectsPosition(ArrayList<DragObject> extraDragObjects, boolean animate) {
        if (extraDragObjects != null) {
            DropItem dropItem;
            ArrayList<DragObject> arrayList = new ArrayList(extraDragObjects);
            Collections.sort(arrayList, this.mAscComparator);
            ArrayList<IconInfo> items = new ArrayList();
            ArrayList<DropItem> dropItems = new ArrayList();
            int rankToKeepPosition = this.mTargetRank;
            this.mSuppressOnAdd = true;
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                DragObject d = (DragObject) it.next();
                IconInfo info = d.dragInfo;
                View icon = d.dragView.getSourceView();
                this.mContent.insertViewBeforeArrangeChildren(icon, info, info.rank, rankToKeepPosition);
                dropItem = new DropItem();
                dropItem.dragView = d.dragView;
                dropItem.iconView = icon;
                dropItem.targetPageIndex = info.rank / this.mContent.itemsPerPage();
                dropItems.add(dropItem);
                items.add(info);
            }
            if (!items.isEmpty()) {
                this.mInfo.add(items);
            }
            this.mSuppressOnAdd = false;
            rearrangeChildren();
            if (animate) {
                DragLayer dragLayer = this.mLauncher.getDragLayer();
                int currentPageIndex = this.mContent.getNextPage();
                Iterator it2 = dropItems.iterator();
                while (it2.hasNext()) {
                    int translatedX;
                    dropItem = (DropItem) it2.next();
                    View page = this.mContent.getPageAt(dropItem.targetPageIndex);
                    if (page != null) {
                        int expectedX = (dropItem.targetPageIndex - currentPageIndex) * this.mContent.getDesiredWidth();
                        Rect pageRect = new Rect();
                        dragLayer.getViewRectRelativeToSelf(page, pageRect);
                        translatedX = expectedX - pageRect.left;
                    } else {
                        translatedX = this.mContent.getDesiredWidth();
                    }
                    if (!(dropItem.dragView == null || dropItem.iconView == null)) {
                        dragLayer.animateViewIntoPosition(dropItem.dragView, dropItem.iconView, 300, null, null, translatedX);
                    }
                }
            }
        }
    }

    public void notifyDrop() {
        if (this.mDragInProgress) {
            this.mItemAddedBackToSelfViaIcon = true;
        }
    }

    public boolean isFull() {
        return this.mContent.isFull();
    }

    private void updateFolderLayout() {
        this.mBorderWidth = 0;
        this.mBorderHeight = 0;
        setupFolderLayout();
        centerAboutIcon();
    }

    public void centerAboutIcon() {
        Point displaySize = new Point();
        Utilities.getScreenSize(getContext(), displaySize);
        LayoutParams lp = (LayoutParams) getLayoutParams();
        int width = calculateFolderWidth();
        int height = calculateFolderHeight();
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        boolean isNavigationLeft = dp.isLandscape && Utilities.getNavigationBarPositon() == 1;
        int extraPadding = (!isNavigationLeft || dp.isMultiwindowMode) ? 0 : dp.navigationBarHeight;
        Resources res = getResources();
        int left = ((displaySize.x - width) / 2) + extraPadding;
        int top = res.getDimensionPixelSize(R.dimen.open_folder_margin_top);
        if (LauncherFeature.supportMultiSelect()) {
            int multiSelectPanelHeight = res.getDimensionPixelSize(R.dimen.multi_select_panel_height);
            if (top <= multiSelectPanelHeight) {
                top = multiSelectPanelHeight + ((int) (1.0f * res.getDisplayMetrics().density));
            }
        }
        lp.width = width;
        lp.height = height;
        lp.x = left;
        lp.y = top;
        FrameLayout.LayoutParams addButtonLp = (FrameLayout.LayoutParams) this.mOuterAddButtonContainer.getLayoutParams();
        addButtonLp.gravity = 81;
        addButtonLp.topMargin = 0;
        addButtonLp.bottomMargin = this.mFooterHeight;
        addButtonLp.leftMargin = 0;
        LinearLayout outerAddButtonLayout = findViewById(R.id.folder_add_button_container);
        if (outerAddButtonLayout != null && outerAddButtonLayout.getLayoutParams() != null) {
            ((LinearLayout.LayoutParams) outerAddButtonLayout.getLayoutParams()).height = res.getDimensionPixelOffset(R.dimen.open_folder_outer_add_button_container_height);
            outerAddButtonLayout.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.open_folder_outer_add_button_container_margin_bottom));
        }
    }

    private int getContentAreaHeight() {
        return Math.max(this.mContentMinHeight, (this.mContent.getCellLayoutChildrenHeight() + this.mContentTopMargin) + Math.max(this.mContentMinMargin, this.mOuterAddButtonContainerHeight));
    }

    private int getContentAreaWidth() {
        return Math.max(this.mContent.getCellLayoutChildrenWidth(), 5);
    }

    private int calculateBorderWidth() {
        return Math.max(this.mHeader.getMeasuredWidth(), getContentAreaWidth() + (this.mContentMinMargin * 2));
    }

    private int calculateFolderWidth() {
        Point displaySize = new Point();
        Utilities.getScreenSize(getContext(), displaySize);
        return displaySize.x;
    }

    private int calculateFolderHeight() {
        return calculateFolderHeight(getContentAreaHeight());
    }

    private int calculateFolderHeight(int contentAreaHeight) {
        return (((getPaddingTop() + getPaddingBottom()) + contentAreaHeight) + this.mHeaderHeight) + this.mFooterHeight;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = getContentAreaWidth();
        int contentHeight = getContentAreaHeight();
        int borderWidth = calculateBorderWidth();
        int broderHeight = this.mHeaderHeight + contentHeight;
        int maxWidth = calculateFolderWidth();
        int maxHeight = calculateFolderHeight(contentHeight);
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(contentHeight, 1073741824);
        int borderWidthSpec = MeasureSpec.makeMeasureSpec(borderWidth, 1073741824);
        int borderHeightSpec = MeasureSpec.makeMeasureSpec(broderHeight, 1073741824);
        int maxWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth, 1073741824);
        this.mFolderContainer.measure(maxWidthSpec, MeasureSpec.makeMeasureSpec(maxHeight, 1073741824));
        this.mContent.setFixedSize(maxWidth, contentHeight);
        this.mContentContainer.measure(maxWidthSpec, contentAreaHeightSpec);
        this.mBorder.measure(borderWidthSpec, borderHeightSpec);
        this.mHeader.measure(borderWidthSpec, MeasureSpec.makeMeasureSpec(this.mHeaderHeight, 1073741824));
        this.mFooter.measure(borderWidthSpec, MeasureSpec.makeMeasureSpec(this.mFooterHeight, 1073741824));
        this.mOuterAddButtonContainer.measure(0, MeasureSpec.makeMeasureSpec(this.mOuterAddButtonContainerHeight, 1073741824));
        setMeasuredDimension(calculateFolderWidth(), calculateFolderHeight(contentHeight));
        this.mBorderWidth = borderWidth;
        this.mBorderHeight = broderHeight;
    }

    public void rearrangeChildren() {
        rearrangeChildren(-1);
    }

    public void rearrangeChildren(int itemCount) {
        ArrayList<View> views = getItemsInReadingOrder();
        this.mContent.arrangeChildren(views, Math.max(itemCount, views.size()));
        this.mItemsInvalidated = true;
    }

    public View getBorder() {
        return this.mBorder;
    }

    public View getHeader() {
        return this.mHeader;
    }

    public FolderPagedView getContent() {
        return this.mContent;
    }

    public int getBorderWidth() {
        if (this.mBorderWidth == 0) {
            measure(0, 0);
        }
        return this.mBorderWidth;
    }

    public int getBorderHeight() {
        if (this.mBorderHeight == 0) {
            measure(0, 0);
        }
        return this.mBorderHeight;
    }

    public int getContentBorderWidth() {
        return getBorderWidth();
    }

    public int getContentBorderHeight() {
        return getBorderHeight() - this.mHeaderHeight;
    }

    public int getItemCount() {
        return this.mContent.getItemCount();
    }

    void onCloseComplete() {
        clearFocus();
        this.mFolderIconView.requestFocus();
        setCrosshairsVisibility(false);
        if (this.mRearrangeOnClose) {
            rearrangeChildren();
            this.mRearrangeOnClose = false;
        }
        if (getItemCount() <= 1) {
            if (!this.mDragInProgress && !this.mSuppressFolderDeletion) {
                replaceFolderWithFinalItem();
            } else if (this.mDragInProgress) {
                this.mDeleteFolderOnDropCompleted = true;
            }
        }
        this.mRestorePositionOnDrop = false;
        this.mSuppressFolderDeletion = false;
        this.mSuppressFolderClose = false;
        clearDragInfo();
        if (this.mCustomColorPicker != null && this.mCustomColorPicker.isShowing()) {
            this.mCustomColorPicker.dismiss();
        }
    }

    private void replaceFolderWithFinalItem() {
        if (!this.mDestroyed) {
            Runnable onCompleteRunnable = new Runnable() {
                public void run() {
                    FolderView.this.mController.replaceFolderWithFinalItem(FolderView.this.mInfo, FolderView.this.getItemCount(), FolderView.this.mFolderIconView);
                }
            };
            View finalChild = this.mContent.getLastItem();
            if (finalChild == null) {
                onCompleteRunnable.run();
            } else if (isAppsAlphabeticViewType()) {
                onCompleteRunnable.run();
            } else {
                this.mFolderIconView.performDestroyAnimation(finalChild, onCompleteRunnable);
            }
            this.mDestroyed = true;
        }
    }

    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public void updateContentFocus() {
        View firstChild = this.mContent.getFirstItem();
        View lastChild = this.mContent.getLastItem();
        if (firstChild != null && lastChild != null) {
            this.mFolderName.setNextFocusDownId(firstChild.getId());
            this.mFolderOptionButton.setNextFocusDownId(firstChild.getId());
            if (Utilities.sIsRtl) {
                this.mFolderOptionButton.setNextFocusLeftId(firstChild.getId());
            } else {
                this.mFolderOptionButton.setNextFocusRightId(firstChild.getId());
            }
            if (this.mAddButton != null) {
                this.mAddButton.setNextFocusUpId(lastChild.getId());
                this.mAddButton.setNextFocusLeftId(lastChild.getId());
                this.mAddButton.setNextFocusRightId(lastChild.getId());
            }
        } else if (this.mAddButton != null) {
            this.mFolderName.setNextFocusDownId(this.mAddButton.getId());
            this.mFolderOptionButton.setNextFocusDownId(this.mAddButton.getId());
            this.mAddButton.setNextFocusDownId(this.mFolderName.getId());
        }
    }

    public void onItemAdded(IconInfo item) {
        if (!this.mSuppressOnAdd) {
            if (this.mInfo.isAlphabeticalOrder()) {
                item.rank = -1;
                this.mController.addOrMoveItemInDb(item, this.mInfo.id, 0, item.cellX, item.cellY, item.rank);
                this.mInfo.sortContents();
                this.mContent.rebindItems(this.mInfo.contents);
            } else {
                this.mContent.createAndAddViewForRank(item, this.mContent.allocateRankForNewItem(true));
                this.mController.addOrMoveItemInDb(item, this.mInfo.id, 0, item.cellX, item.cellY, item.rank);
            }
            this.mItemsInvalidated = true;
            updateContentFocus();
        }
    }

    public void onItemsAdded(ArrayList<IconInfo> items) {
        if (!this.mSuppressOnAdd) {
            Iterator it;
            if (this.mInfo.isAlphabeticalOrder()) {
                it = items.iterator();
                while (it.hasNext()) {
                    ((IconInfo) it.next()).rank = -1;
                }
                this.mController.addOrMoveItems(items, this.mInfo.id, 0);
                this.mInfo.sortContents();
                this.mContent.rebindItems(this.mInfo.contents);
                this.mItemsInvalidated = true;
            } else {
                it = items.iterator();
                while (it.hasNext()) {
                    this.mContent.createAndAddViewForRank((IconInfo) it.next(), this.mContent.allocateRankForNewItem(true));
                    this.mItemsInvalidated = true;
                }
                this.mController.addOrMoveItems(items, this.mInfo.id, 0);
            }
            updateContentFocus();
        }
    }

    public void onItemRemoved(final IconInfo item) {
        this.mItemsInvalidated = true;
        if (item != this.mCurrentDragInfo && !this.mSuppressOnRemove) {
            final View v = getViewForInfo(item);
            this.mContent.removeItem(v);
            if (this.mViewState == 1) {
                this.mRearrangeOnClose = true;
            } else {
                rearrangeChildren();
            }
            if (getItemCount() <= 1) {
                if (!this.mInfo.opened || this.mFolderController == null) {
                    replaceFolderWithFinalItem();
                } else {
                    if (isAppsAlphabeticViewType()) {
                        replaceFolderWithFinalItem();
                    }
                    this.mFolderController.closeFolderIfLackItem();
                }
            }
            post(new Runnable() {
                public void run() {
                    if (FolderView.this.mMultiSelectManager != null && FolderView.this.mMultiSelectManager.isMultiSelectMode() && v != null && !Utilities.isAppEnabled(FolderView.this.mLauncher, item)) {
                        FolderView.this.mMultiSelectManager.removeCheckedApp(v);
                    }
                }
            });
            updateContentFocus();
        }
    }

    public void onItemsRemoved(ArrayList<IconInfo> items) {
        if (!this.mSuppressOnRemove) {
            boolean rearrange = false;
            Iterator it = items.iterator();
            while (it.hasNext()) {
                IconInfo item = (IconInfo) it.next();
                if (item != this.mCurrentDragInfo) {
                    this.mItemsInvalidated = true;
                    this.mContent.removeItem(getViewForInfo(item));
                    rearrange = true;
                }
            }
            if (rearrange) {
                if (this.mViewState == 1) {
                    this.mRearrangeOnClose = true;
                } else {
                    rearrangeChildren();
                }
                if (getItemCount() <= 1) {
                    if (!this.mInfo.opened || this.mFolderController == null) {
                        replaceFolderWithFinalItem();
                    } else {
                        if (isAppsAlphabeticViewType()) {
                            replaceFolderWithFinalItem();
                        }
                        this.mFolderController.closeFolderIfLackItem();
                    }
                }
            }
            updateContentFocus();
        }
    }

    public View getViewForInfo(final IconInfo item) {
        return this.mContent.iterateOverItems(new ItemOperator() {
            public boolean evaluate(ItemInfo info, View view, View parent) {
                return info == item;
            }
        });
    }

    public void onTitleChanged(CharSequence title) {
    }

    public void onOrderingChanged(boolean alphabeticalOrder) {
        this.mContent.rebindItems(this.mInfo.contents);
        this.mItemsInvalidated = true;
    }

    public void onLockedFolderOpenStateUpdated(Boolean opened) {
        updateContentFocus();
    }

    public ArrayList<View> getItemsInReadingOrder() {
        if (this.mItemsInvalidated) {
            this.mItemsInReadingOrder.clear();
            this.mContent.iterateOverItems(new ItemOperator() {
                public boolean evaluate(ItemInfo info, View view, View parent) {
                    FolderView.this.mItemsInReadingOrder.add(view);
                    return false;
                }
            });
            this.mItemsInvalidated = false;
        }
        return this.mItemsInReadingOrder;
    }

    public void fillInLaunchSourceData(Bundle sourceData) {
        LaunchSourceUtils.populateSourceDataFromAncestorProvider(this.mFolderIconView, sourceData);
        sourceData.putString(Stats.SOURCE_EXTRA_SUB_CONTAINER, "folder");
        sourceData.putInt(Stats.SOURCE_EXTRA_SUB_CONTAINER_PAGE, this.mContent.getCurrentPage());
    }

    private FolderColor getFolderColorByView(View v) {
        for (FolderColor color : FolderColor.values()) {
            if (getColorPickerImageView(color) == v) {
                return color;
            }
        }
        return FolderColor.FOLDER_COLOR_1;
    }

    private void setFolderColorTalkback(FolderColor newColor) {
        boolean isDefaultTheme = OpenThemeManager.getInstance().isDefaultTheme();
        for (FolderColor color : FolderColor.values()) {
            ImageView colorView = getColorPickerImageView(color);
            if (colorView != null) {
                String description = getFolderColorDescription(color, isDefaultTheme);
                if (color == newColor) {
                    description = description + " " + getResources().getString(R.string.selected);
                }
                colorView.setContentDescription(description);
            }
        }
    }

    String getFolderColorDescription(FolderColor color, boolean isDefaultTheme) {
        String themeColorDescription = getResources().getString(R.string.folder_color_pd, new Object[]{color.ordinal() + 1});
        switch (color) {
            case FOLDER_COLOR_1:
                return getResources().getString(R.string.folder_color_default);
            case FOLDER_COLOR_2:
                if (isDefaultTheme) {
                    return getResources().getString(R.string.folder_color_light_blue);
                }
                return themeColorDescription;
            case FOLDER_COLOR_3:
                if (isDefaultTheme) {
                    return getResources().getString(R.string.folder_color_light_green);
                }
                return themeColorDescription;
            case FOLDER_COLOR_4:
                if (isDefaultTheme) {
                    return getResources().getString(R.string.folder_color_orange);
                }
                return themeColorDescription;
            case FOLDER_COLOR_5:
                if (isDefaultTheme) {
                    return getResources().getString(R.string.folder_color_yellow);
                }
                return themeColorDescription;
            case FOLDER_COLOR_CUSTOM:
                return getResources().getString(R.string.folder_color_picker);
            default:
                return "";
        }
    }

    private Bitmap getSelectedColorIcon(Bitmap shape, Bitmap border) {
        Bitmap result = Bitmap.createBitmap(shape.getWidth(), shape.getHeight(), shape.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.save();
        canvas.drawBitmap(shape, 0.0f, 0.0f, paint);
        canvas.restore();
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
        canvas.drawBitmap(border, 0.0f, 0.0f, paint);
        return result;
    }

    private void setFolderColor(FolderColor color_idx, boolean bUpdateItem) {
        setFolderColor(color_idx, 0, bUpdateItem, true);
    }

    private void setFolderColor(FolderColor color_idx, boolean bUpdateItem, boolean bUpdateButton) {
        setFolderColor(color_idx, 0, bUpdateItem, bUpdateButton);
    }

    private void setFolderColor(FolderColor color_idx, int color, boolean bUpdateItem, boolean bUpdateButton) {
        int openFolderColor;
        int iconResId;
        boolean isDefaultTheme = OpenThemeManager.getInstance().isDefaultTheme();
        setFolderColorTalkback(color_idx);
        Resources res = getContext().getResources();
        for (FolderColor folderColor : FolderColor.values()) {
            ImageView colorView = getColorPickerImageView(folderColor);
            if (colorView != null) {
                if (color_idx == folderColor) {
                    Drawable checkedImage = getResources().getDrawable(R.drawable.homescreen_folder_color_selected, null);
                    if (isDefaultTheme && color_idx == FolderColor.FOLDER_COLOR_1) {
                        checkedImage.mutate().setColorFilter(res.getColor(R.color.apps_picker_black_color, null), Mode.SRC_ATOP);
                    }
                    colorView.setImageDrawable(checkedImage);
                } else {
                    colorView.setImageDrawable(null);
                }
            }
        }
        if (!isWhiteBg()) {
            int titleColor = res.getColor(R.color.folder_header_title_color, null);
            this.mFolderName.setTextColor(titleColor);
            this.mFolderName.setHintTextColor(FOLDER_NAME_HINT_COLOR_ALPHA_MASK & titleColor);
            this.mHeaderBottomLine.setBackgroundColor(FOLDER_NAME_BAR_COLOR_ALPHA_MASK & titleColor);
        }
        if (color_idx == FolderColor.FOLDER_COLOR_CUSTOM) {
            openFolderColor = color;
        } else {
            openFolderColor = this.mFolderColor.get(color_idx);
        }
        if (LauncherFeature.isSupportFolderColorPicker()) {
            iconResId = R.drawable.folder_colorpicker_selected_shape;
        } else {
            iconResId = R.drawable.homescreen_folder_color_b;
        }
        Bitmap optionButtonImage = BitmapUtils.getBitmapWithColor(getContext(), iconResId, openFolderColor);
        if (bUpdateButton) {
            this.mFolderOptionButton.setImageBitmap(getSelectedColorIcon(optionButtonImage, BitmapFactory.decodeResource(getResources(), R.drawable.folder_colorpicker_selected_line)));
        }
        if (!isDefaultTheme) {
            FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
            if (fs == null || fs.getFolderType() != 1) {
                int iconSize;
                if (LauncherFeature.isSupportFolderColorPicker()) {
                    iconSize = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size_new);
                } else {
                    iconSize = res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_size);
                }
                int roundSize = iconSize - res.getDimensionPixelSize(R.dimen.open_folder_title_colorpicker_img_stroke_size);
                for (FolderColor folderColor2 : FolderColor.values()) {
                    colorView = getColorPickerImageView(folderColor2);
                    if (colorView != null) {
                        int itemColor = this.mFolderColor.get(folderColor2);
                        Drawable itemImage = sColorPickerImages.get(itemColor);
                        Drawable strokebase = res.getDrawable(R.drawable.folder_picker_stroke_white_ring);
                        if (folderColor2 == FolderColor.FOLDER_COLOR_CUSTOM) {
                            Bitmap customColor = ThemeUtils.roundBitmap(res.getDrawable(R.drawable.folder_colorpicker_color), iconSize, iconSize, roundSize / 2, strokebase);
                            if (customColor != null) {
                                itemImage = new BitmapDrawable(customColor);
                            } else {
                                Log.e(TAG, "setFolderColor : can't create custom color picker image");
                            }
                        } else if (itemImage == null) {
                            Bitmap bitmap = ThemeUtils.roundBitmap(itemColor, iconSize, iconSize, roundSize / 2, strokebase);
                            if (bitmap != null) {
                                itemImage = new BitmapDrawable(bitmap);
                                sColorPickerImages.put(itemColor, itemImage);
                            } else {
                                Log.e(TAG, "setFolderColor : can't create color picker image");
                            }
                        }
                        if (itemImage != null) {
                            colorView.setBackground(itemImage);
                        }
                    }
                }
            } else {
                this.mFolderOptionButton.setVisibility(View.GONE);
            }
        }
        if (bUpdateItem) {
            if (color_idx == FolderColor.FOLDER_COLOR_CUSTOM) {
                this.mInfo.color = color;
                this.mFolderIconView.updateCustomColor(color);
            } else {
                this.mInfo.color = color_idx.ordinal();
                this.mFolderIconView.setIconBackgroundColor(this.mInfo.color);
            }
            this.mController.updateItemInDb(this.mInfo);
        }
    }

    private void playAppearColorPickerAnimation() {
        this.mFolderName.setAlpha(1.0f);
        final Animator folderNameAnim = LauncherAnimUtils.ofFloat(this.mFolderName, "alpha", 0.0f);
        folderNameAnim.setDuration(200);
        folderNameAnim.setInterpolator(ViInterpolator.getInterploator(34));
        this.mColorPickerView.setAlpha(0.0f);
        Animator colorPickerAnim = LauncherAnimUtils.ofFloat(this.mColorPickerView, "alpha", 1.0f);
        colorPickerAnim.setDuration(167);
        colorPickerAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                folderNameAnim.start();
                FolderView.this.mColorPickerView.setVisibility(View.VISIBLE);
                List<Animator> animators = new ArrayList(FolderView.this.mColorPickerItems.size());
                for (ImageView colorView : FolderView.this.mColorPickerItems.values()) {
                    colorView.setAlpha(0.0f);
                    colorView.setScaleX(0.6f);
                    colorView.setScaleY(0.6f);
                    LauncherAnimUtils.ofFloat(colorView, "alpha", 1.0f).setInterpolator(ViInterpolator.getInterploator(30));
                    AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
                    r7 = new Animator[2];
                    r7[0] = LauncherAnimUtils.ofFloat(colorView, "scaleX", 1.0f);
                    r7[1] = LauncherAnimUtils.ofFloat(colorView, "scaleY", 1.0f);
                    scaleAnimSet.playTogether(r7);
                    scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(34));
                    AnimatorSet pickerAnimSet = LauncherAnimUtils.createAnimatorSet();
                    pickerAnimSet.playTogether(new Animator[]{alphaAnim, scaleAnimSet});
                    animators.add(pickerAnimSet);
                }
                AnimatorSet set = LauncherAnimUtils.createAnimatorSet();
                set.setDuration(250);
                set.playTogether(animators);
                set.start();
            }
        });
        colorPickerAnim.start();
    }

    private void playDismissColorPickerAnimation() {
        this.mFolderName.setAlpha(0.0f);
        final Animator folderNameAnim = LauncherAnimUtils.ofFloat(this.mFolderName, "alpha", 1.0f);
        folderNameAnim.setDuration(167);
        folderNameAnim.setInterpolator(CUSTOM_FOLDER_INTERPOLATOR);
        folderNameAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                FolderView.this.mColorPickerView.setVisibility(View.GONE);
                FolderView.this.setSuppressFolderNameFocus((long) FolderView.this.mFadeInOutDuration);
            }
        });
        this.mColorPickerView.setAlpha(1.0f);
        Animator colorPickerAnim = LauncherAnimUtils.ofFloat(this.mColorPickerView, "alpha", 1.0f);
        colorPickerAnim.setDuration(167);
        colorPickerAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                List<Animator> animators = new ArrayList(FolderView.this.mColorPickerItems.size());
                for (final ImageView colorView : FolderView.this.mColorPickerItems.values()) {
                    LauncherAnimUtils.ofFloat(colorView, "alpha", 0.0f).setInterpolator(ViInterpolator.getInterploator(32));
                    AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
                    r7 = new Animator[2];
                    r7[0] = LauncherAnimUtils.ofFloat(colorView, "scaleX", 0.0f);
                    r7[1] = LauncherAnimUtils.ofFloat(colorView, "scaleY", 0.0f);
                    scaleAnimSet.playTogether(r7);
                    scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(32));
                    AnimatorSet pickerAnimSet = LauncherAnimUtils.createAnimatorSet();
                    pickerAnimSet.playTogether(new Animator[]{alphaAnim, scaleAnimSet});
                    animators.add(pickerAnimSet);
                    pickerAnimSet.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationStart(Animator animation) {
                            colorView.setAlpha(1.0f);
                            colorView.setScaleX(1.0f);
                            colorView.setScaleY(1.0f);
                        }
                    });
                    AnimatorSet set = LauncherAnimUtils.createAnimatorSet();
                    set.setDuration(167);
                    set.playTogether(animators);
                    set.start();
                }
            }

            public void onAnimationEnd(Animator animation) {
                folderNameAnim.start();
            }
        });
        colorPickerAnim.start();
    }

    private void animateAppear(final View targetView) {
        if (targetView != null) {
            targetView.setVisibility(View.VISIBLE);
            Animator alphaAnim = LauncherAnimUtils.ofFloat(targetView, "alpha", 1.0f);
            alphaAnim.setDuration(200);
            alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
            alphaAnim.start();
            alphaAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    targetView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void animateDismiss(final View targetView, final boolean keepLayout) {
        if (targetView != null) {
            Animator alphaAnim = LauncherAnimUtils.ofFloat(targetView, "alpha", 0.0f);
            alphaAnim.setDuration(200);
            alphaAnim.setInterpolator(ViInterpolator.getInterploator(30));
            alphaAnim.start();
            alphaAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    targetView.setVisibility(keepLayout ? 4 : 8);
                }
            });
        }
    }

    private AnimatorSet getAppearButtonAnimatorSet(boolean isOpen) {
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        Animator[] r1 = new Animator[3];
        r1[0] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "scaleX", 1.0f);
        r1[1] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "scaleY", 1.0f);
        r1[2] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "alpha", 1.0f);
        scaleAnimSet.playTogether(r1);
        scaleAnimSet.setDuration(117);
        scaleAnimSet.setStartDelay(50);
        scaleAnimSet.setInterpolator(isOpen ? ViInterpolator.getInterploator(34) : CUSTOM_FOLDER_INTERPOLATOR);
        return scaleAnimSet;
    }

    private AnimatorSet getDimissButtonAnimatorSet() {
        AnimatorSet scaleAnimSet = LauncherAnimUtils.createAnimatorSet();
        Animator[] r1 = new Animator[3];
        r1[0] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "scaleX", 0.0f);
        r1[1] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "scaleY", 0.0f);
        r1[2] = LauncherAnimUtils.ofFloat(this.mFolderOptionButton, "alpha", 0.0f);
        scaleAnimSet.playTogether(r1);
        scaleAnimSet.setDuration(167);
        scaleAnimSet.setInterpolator(ViInterpolator.getInterploator(32));
        return scaleAnimSet;
    }

    private void animateColorPickerButton(final boolean isOpen) {
        AnimatorSet startAnim = getDimissButtonAnimatorSet();
        final AnimatorSet endAnim = getAppearButtonAnimatorSet(isOpen);
        startAnim.start();
        startAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (isOpen) {
                    FolderView.this.mFolderName.setEnabled(false);
                    FolderView.this.playAppearColorPickerAnimation();
                    if (LauncherFeature.isSupportFolderColorPicker()) {
                        FolderView.this.showCloseButton();
                    }
                } else {
                    FolderView.this.mFolderName.setEnabled(true);
                    FolderView.this.playDismissColorPickerAnimation();
                    if (LauncherFeature.isSupportFolderColorPicker()) {
                        FolderView.this.updateFolderColor();
                    }
                }
                endAnim.start();
            }
        });
    }

    private void showCloseButton() {
        Drawable closeImage = getResources().getDrawable(R.drawable.folder_colorpicker_close, null);
        if (isWhiteBg()) {
            closeImage.mutate().setColorFilter(getResources().getColor(R.color.apps_picker_black_color, null), Mode.SRC_ATOP);
        }
        this.mFolderOptionButton.setImageDrawable(closeImage);
        this.mFolderOptionButton.setContentDescription(getResources().getString(R.string.folder_color_close));
    }

    public void toggleColorPicker() {
        this.mNeedToShowCursor = false;
        if (this.mColorPickerView.getVisibility() == 0) {
            animateColorPickerButton(false);
            this.mFolderOptionButton.setContentDescription(getResources().getString(R.string.folder_color));
            return;
        }
        animateColorPickerButton(true);
    }

    private View createAddButton() {
        View buttonView = ((ViewStub) findViewById(R.id.folder_outer_add_button_stub)).inflate();
        TextView addButtonText = buttonView.findViewById(R.id.folder_add_button_text);
        if (addButtonText != null) {
            if (Utilities.isEnableBtnBg(getContext())) {
                addButtonText.setBackgroundResource(R.drawable.panel_btn_bg);
            }
            addButtonText.setContentDescription(addButtonText.getContentDescription() + " " + getResources().getString(R.string.button));
            addButtonText.setOnClickListener(this);
            Drawable[] addButtonDrawable = addButtonText.getCompoundDrawablesRelative();
            int addButtonDrawableSize = getResources().getDimensionPixelSize(R.dimen.open_folder_outer_add_button_height) - (getResources().getDimensionPixelSize(R.dimen.open_folder_outer_add_button_padding) * 2);
            addButtonDrawable[0].setBounds(0, 0, addButtonDrawableSize, addButtonDrawableSize);
            addButtonText.setCompoundDrawablesRelative(addButtonDrawable[0], null, null, null);
        }
        return buttonView;
    }

    public void showAddButton(boolean animate) {
        if (this.mAddButton == null) {
            return;
        }
        if (animate) {
            animateAppear(this.mAddButton);
        } else {
            this.mAddButton.setVisibility(View.VISIBLE);
        }
    }

    public void hideAddButton(boolean animate) {
        if (this.mAddButton == null) {
            return;
        }
        if (animate) {
            animateDismiss(this.mAddButton, true);
        } else {
            this.mAddButton.setVisibility(View.INVISIBLE);
        }
    }

    public void showHintPages() {
        if (getMeasuredWidth() == 0 || this.mBorderWidth == 0) {
            measure(0, 0);
        }
        this.mContent.setHintPageWidth(((getMeasuredWidth() - this.mBorderWidth) / 2) - this.mPageSpacingOnDrag);
        this.mContent.showHintPages();
    }

    public void hideHintPages() {
        this.mContent.hideHintPages();
    }

    public void setCrosshairsVisibility(boolean show) {
        this.mContent.setCrosshairsVisibilityChilds(show ? 0 : 8);
    }

    public boolean onInterceptHoverEvent(MotionEvent event) {
        if (this.mLauncher == null || !getInfo().opened || !Talk.INSTANCE.isTouchExplorationEnabled() || this.mController == null) {
            return false;
        }
        switch (event.getAction()) {
            case 7:
                boolean isOverFolderOrDropBar = isEventOverFolder(event);
                if (!isOverFolderOrDropBar && !this.mHoverPointClosesFolder) {
                    sendTapOutsideFolderAccessibilityEvent(isEditingName());
                    this.mHoverPointClosesFolder = true;
                    return true;
                } else if (!isOverFolderOrDropBar) {
                    return true;
                } else {
                    this.mHoverPointClosesFolder = false;
                    return false;
                }
            case 9:
                if (isEventOverFolder(event)) {
                    this.mHoverPointClosesFolder = false;
                    return false;
                }
                sendTapOutsideFolderAccessibilityEvent(isEditingName());
                this.mHoverPointClosesFolder = true;
                return true;
            default:
                return false;
        }
    }

    private void sendTapOutsideFolderAccessibilityEvent(boolean isEditingName) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            int stringId = isEditingName ? R.string.folder_tap_to_rename : R.string.folder_tap_to_close;
            AccessibilityEvent event = AccessibilityEvent.obtain(8);
            onInitializeAccessibilityEvent(event);
            event.getText().add(getContext().getString(stringId));
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0 && handleTouchDown(ev, true)) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean handleTouchDown(MotionEvent ev, boolean intercept) {
        if (getInfo().opened && intercept && this.mViewState == 2) {
            if (isEditingName() && !isEventOverFolderTextRegion(ev)) {
                dismissEditingName();
                if (isEventOverFolderHeaderRegion(ev)) {
                    return false;
                }
                return true;
            } else if (Utilities.getOrientation() == 2 && isEventOverFolderBorderOutside(ev)) {
                if (this.mMultiSelectManager != null && this.mMultiSelectManager.isMultiSelectMode()) {
                    Toast.makeText(this.mLauncher, R.string.multi_select_apps_deselected, 0).show();
                    this.mLauncher.onChangeSelectMode(false, true);
                    return true;
                } else if (!this.mLauncher.isFolderStage()) {
                    return true;
                } else {
                    this.mFolderController.setFolderCloseReasonOnTouchOutside();
                    this.mLauncher.getStageManager().finishStage(5, null);
                    return true;
                }
            } else if (!isEventOverFolder(ev) && this.mLauncher.isFolderStage()) {
                this.mFolderController.setFolderCloseReasonOnTouchOutside();
                this.mLauncher.getStageManager().finishStage(5, null);
                return true;
            }
        }
        return false;
    }

    private boolean isEventOverFolderTextRegion(MotionEvent ev) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect r = new Rect();
        if (dragLayer != null) {
            dragLayer.getDescendantRectRelativeToSelf(getEditTextRegion(), r);
            if (r.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEventOverFolderHeaderRegion(MotionEvent ev) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect r = new Rect();
        if (dragLayer != null) {
            dragLayer.getDescendantRectRelativeToSelf(getHeader(), r);
            if (r.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEventOverFolderBorderOutside(MotionEvent ev) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (!(dragLayer == null || getBorder() == null)) {
            Rect r = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(getBorder(), r);
            if (!r.contains((int) ev.getRawX(), r.centerY())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEventOverFolder(MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (dragLayer != null) {
            Rect r = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(this, r);
            if (r.contains(x, y)) {
                View pageIndicator = findViewById(R.id.folder_page_indicator);
                if (pageIndicator != null) {
                    dragLayer.getDescendantRectRelativeToSelf(pageIndicator, r);
                    if (r.contains(x, y)) {
                        return true;
                    }
                }
                dragLayer.getDescendantRectRelativeToSelf(this.mFooter, r);
                if (r.contains(x, y)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public ControllerBase getBaseController() {
        return this.mController;
    }

    private boolean isNeedToMakeClone(View target) {
        if (equals(target)) {
            return false;
        }
        boolean isSourceApps = this.mInfo.container == -102;
        boolean isTargetHome = false;
        if (target instanceof FolderView) {
            long targetContainer = ((FolderView) target).getInfo().container;
            if (targetContainer == -100 || targetContainer == -101) {
                isTargetHome = true;
            } else {
                isTargetHome = false;
            }
        } else if ((target instanceof Workspace) || (target instanceof Hotseat) || (target instanceof CancelDropTarget) || (target instanceof FakeView)) {
            isTargetHome = true;
        }
        if (isSourceApps && isTargetHome) {
            return true;
        }
        return false;
    }

    public boolean isAppsAlphabeticViewType() {
        boolean isInApps;
        if (this.mInfo.container == -102) {
            isInApps = true;
        } else {
            isInApps = false;
        }
        boolean isAlphabeticViewType = false;
        if (ViewType.valueOf(AppsController.getViewTypeFromSharedPreference(this.mLauncher)) == ViewType.ALPHABETIC_GRID) {
            isAlphabeticViewType = true;
        }
        return isInApps && isAlphabeticViewType;
    }

    public void setSuppressFolderCloseOnce() {
        Log.i(TAG, "setSuppressFolderCloseOnce");
        this.mSuppressFolderClose = true;
    }

    public void notifyFolderItemsChanged() {
        if (this.mController != null && isAppsAlphabeticViewType()) {
            this.mController.notifyControllerItemsChanged();
        }
    }

    public void bindController(FolderController controller) {
        this.mFolderController = controller;
    }

    public void setDragInProgress(boolean drag) {
        this.mDragInProgress = drag;
    }

    public void setBorderAlpha(float alpha) {
        this.mBorder.setAlpha(alpha);
    }

    public void stopBounceAnimation() {
        if (this.mBounceAnimation != null) {
            this.mBounceAnimation.stop();
            this.mBounceAnimation = null;
            showAddButton(true);
        }
    }

    private void startBounceAnimationForSearchedApp(View bounceView) {
        if (bounceView != null) {
            this.mBounceAnimation = new SearchedAppBounceAnimation(bounceView, this.mLauncher.getDeviceProfile().isLandscape);
            if (this.mBounceAnimation != null) {
                this.mBounceAnimation.animate();
                hideAddButton(false);
            }
        }
    }

    public int getDragSourceType() {
        if (this.mInfo.container == -102) {
            return 4;
        }
        return 3;
    }

    public int getOutlineColor() {
        if (isWhiteBg()) {
            return getResources().getColor(R.color.apps_picker_black_color, null);
        }
        return getResources().getColor(R.color.apps_picker_white_color, null);
    }

    public Stage getController() {
        return (Stage) this.mController;
    }

    public boolean isWhiteBg() {
        return WhiteBgManager.isWhiteBg() && this.mLauncher.getDragLayer().getBackgroundImageAlpha() <= 0.0f;
    }

    public void setSuppressOnAdd(boolean supressOnAdd) {
        this.mSuppressOnAdd = supressOnAdd;
    }

    public void setSuppressFolderNameFocus(long delayToRevert) {
        Log.i(TAG, "suppressFolderNameFocus : " + delayToRevert + "ms");
        this.mFolderName.setFocusableInTouchMode(false);
        FolderNameEditText folderNameEditText = this.mFolderName;
        Runnable anonymousClass30 = new Runnable() {
            public void run() {
                FolderView.this.mFolderName.setFocusableInTouchMode(true);
            }
        };
        if (delayToRevert <= 0) {
            delayToRevert = 0;
        }
        folderNameEditText.postDelayed(anonymousClass30, delayToRevert);
    }

    public void updateDeletedFolder() {
        updateDeletedFolder(false);
    }

    public void updateDeletedFolder(boolean isForced) {
        if (isForced || (this.mDeleteFolderOnDropCompleted && !this.mItemAddedBackToSelfViaIcon)) {
            replaceFolderWithFinalItem();
        }
    }

    public FolderNameEditText getFolderNameView() {
        return this.mFolderName;
    }

    public int getEmptyCount() {
        return 0;
    }
}
