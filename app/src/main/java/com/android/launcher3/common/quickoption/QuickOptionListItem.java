package com.android.launcher3.common.quickoption;

import android.graphics.drawable.Drawable;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.sec.android.app.launcher.R;

class QuickOptionListItem {
    static final int ADD_APPS_ICON_ID = 2130837746;
    static final int ADD_APPS_TEXT_ID = 2131296425;
    static final int ADD_SHORTCUT_TO_HOME_ICON_ID = 2130837747;
    static final int ADD_SHORTCUT_TO_HOME_TEXT_ID = 2131296426;
    static final int ADD_SHORTCUT_TO_HOME_TTS_TEXT_ID = 2131296499;
    static final int ADD_TO_PERSONAL_ICON_ID = 2130837748;
    static final int ADD_TO_PERSONAL_TEXT_ID = 2131296427;
    static final int APP_INFO_ICON_ID = 2130837750;
    static final int APP_INFO_TEXT_ID = 2131296430;
    static final int CLEAR_BADGE_ICON_ID = 2130837751;
    static final int CLEAR_BADGE_TEXT_ID = 2131296432;
    static final int DELETE_FOLDER_ICON_ID = 2130837757;
    static final int DELETE_FOLDER_TEXT_ID = 2131296433;
    static final int DELETE_FOLDER_TTS_TEXT_ID = 2131296500;
    static final int DIMMED_DISABLE_TEXT_ID = 2131296436;
    static final int DISABLE_ICON_ID = 2130837752;
    static final int DISABLE_TEXT_ID = 2131296437;
    static final int DISABLE_TTS_TEXT_ID = 2131296501;
    static final int INSTALL_DUAL_IM_ICON_ID = 2130837754;
    static final int INSTALL_DUAL_IM_TEXT_ID = 2131296438;
    static final int LOCK_ICON_ID = 2130837755;
    static final int LOCK_TEXT_ID = 2131296439;
    static final int MOVE_FROM_FOLDER_ICON_ID = 2130837756;
    static final int MOVE_FROM_FOLDER_TEXT_ID = 2131296440;
    static final int REMOVE_ICON_ID = 2130837757;
    static final int REMOVE_SHORTCUT_TEXT_ID = 2131296443;
    static final int REMOVE_SHORTCUT_TTS_TEXT_ID = 2131296502;
    static final int REMOVE_TEXT_ID = 2131296441;
    static final int REMOVE_WIDGET_TEXT_ID = 2131296442;
    static final int REMOVE_WIDGET_TTS_TEXT_ID = 2131296503;
    static final int SECURE_FOLDER_ICON_ID = 2130837749;
    static final int SECURE_FOLDER_TEXT_ID = 2131296444;
    static final int SELECT_ICON_ID = 2130837758;
    static final int SELECT_TEXT_ID = 2131296445;
    static final int SELECT_TTS_TEXT_ID = 2131296504;
    static final int SET_TO_ZEROPAGE_ICON_ID = 2130837746;
    static final int SET_TO_ZEROPAGE_TEXT_ID = 2131296723;
    static final int SLEEP_ICON_ID = 2130837759;
    static final int SLEEP_TEXT_ID = 2131296447;
    static final int TYPE_DEEP_SHORTCUT = 1;
    static final int TYPE_GLOBAL_OPTION = 0;
    static final int UNINSTALL_ICON_ID = 2130837760;
    static final int UNINSTALL_TEXT_ID = 2131296449;
    static final int UNINSTALL_TTS_TEXT_ID = 2131296505;
    static final int UNLOCK_ICON_ID = 2130837761;
    static final int UNLOCK_TEXT_ID = 2131296450;
    private Runnable mCallBack;
    private ShortcutKey mDeepShortcutKey;
    private Drawable mIcon;
    private int mIconRsrId;
    private String mTitle;
    private int mTitleRsrId;
    private int mTtsTitleRsrId = -1;
    private int mType = 0;

    QuickOptionListItem() {
    }

    int getType() {
        return this.mType;
    }

    void setType(int type) {
        this.mType = type;
    }

    int getIconRsrId() {
        return this.mIconRsrId;
    }

    void setIconRsrId(int iconRsrId) {
        this.mIconRsrId = iconRsrId;
    }

    int getTitleRsrId() {
        return this.mTitleRsrId;
    }

    void setTtsTitleRsrId(int ttsTitleRsrId) {
        this.mTtsTitleRsrId = ttsTitleRsrId;
    }

    int getTtsTitleRsrId() {
        return this.mTtsTitleRsrId;
    }

    void setTitleRsrId(int titleRsrId) {
        this.mTitleRsrId = titleRsrId;
    }

    public Runnable getCallback() {
        return this.mCallBack;
    }

    public void setCallback(Runnable r) {
        this.mCallBack = r;
    }

    boolean isOptionRemove() {
        return getIconRsrId() == R.drawable.quick_ic_remove && (getTitleRsrId() == R.string.quick_option_remove || getTitleRsrId() == R.string.quick_option_remove_shortcut || getTitleRsrId() == R.string.quick_option_remove_from_home_screen);
    }

    ShortcutKey getShortcutKey() {
        return this.mDeepShortcutKey;
    }

    void setShortcutKey(ShortcutKey key) {
        this.mDeepShortcutKey = key;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public void setIcon(Drawable d) {
        this.mIcon = d;
    }
}
