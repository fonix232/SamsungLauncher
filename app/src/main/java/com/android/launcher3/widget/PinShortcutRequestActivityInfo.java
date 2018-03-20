package com.android.launcher3.widget;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.DeferredLauncherActivityInfo;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.PinItemRequestCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutCache;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.PinnedShortcutUtils;
import com.sec.android.app.launcher.R;
import java.util.Arrays;
import java.util.List;

@TargetApi(25)
public class PinShortcutRequestActivityInfo extends DeferredLauncherActivityInfo {
    private static final String DUMMY_COMPONENT_CLASS = "pinned-shortcut";
    private final Context mContext;
    private final ShortcutInfoCompat mInfo = new ShortcutInfoCompat(this.mRequest.getShortcutInfo());
    private final PinItemRequestCompat mRequest;

    public PinShortcutRequestActivityInfo(PinItemRequestCompat request, Context context) {
        super(new ComponentName(request.getShortcutInfo().getPackage(), DUMMY_COMPONENT_CLASS), UserHandleCompat.fromUser(request.getShortcutInfo().getUserHandle()), context);
        this.mRequest = request;
        this.mContext = context;
    }

    public CharSequence getLabel() {
        return this.mInfo.getShortLabel();
    }

    public Drawable getFullResIcon(IconCache cache) {
        return LauncherAppState.getInstance().getShortcutManager().getShortcutIconDrawable(this.mInfo);
    }

    public IconInfo createShortcutInfo() {
        ShortcutInfoCompat compat = new ShortcutInfoCompat(this.mRequest.getShortcutInfo());
        List<ShortcutInfoCompat> shortcutInfoCompatList = new DeepShortcutManager(this.mContext, new ShortcutCache()).queryForFullDetails(compat.getPackage(), Arrays.asList(new String[]{compat.getId()}), compat.getUserHandle());
        if (!shortcutInfoCompatList.isEmpty()) {
            Intent intent = ((ShortcutInfoCompat) shortcutInfoCompatList.get(0)).getShortcutInfo().getIntent();
            if (intent != null && Utilities.isLauncherAppTarget(intent)) {
                IconInfo info;
                if (PinnedShortcutUtils.isRequestFromEDM(((ShortcutInfoCompat) shortcutInfoCompatList.get(0)).getShortcutInfo(), intent)) {
                    info = new IconInfo(this.mContext, new DeferredLauncherActivityInfo(intent.getComponent(), ((ShortcutInfoCompat) shortcutInfoCompatList.get(0)).getUserHandle(), this.mContext), compat.getUserHandle(), null, intent);
                } else {
                    info = new IconInfo(this.mContext, compat.getActivityInfo(this.mContext), compat.getUserHandle(), null);
                }
                if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled() || !PinnedShortcutUtils.shortcutExists(this.mContext, info.getIntent(), compat.getUserHandle())) {
                    return info;
                }
                Toast.makeText(new ContextThemeWrapper(this.mContext, 16974123), this.mContext.getString(R.string.shortcut_duplicate, new Object[]{info.title}), 0).show();
                return null;
            }
        }
        return new IconInfo(compat, this.mContext);
    }

    public void accept(boolean immediately) {
        LauncherAppsCompat.acceptPinItemRequest(this.mContext, this.mRequest, immediately ? 0 : 300);
    }

    public PinItemRequestCompat getPinItemRequestCompat() {
        return this.mRequest;
    }
}
