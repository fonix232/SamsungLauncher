package com.android.launcher3.common.compat;

import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

public class UserManagerCompatV16 extends UserManagerCompat {
    UserManagerCompatV16() {
    }

    public List<UserHandleCompat> getUserProfiles() {
        List<UserHandleCompat> profiles = new ArrayList(1);
        profiles.add(UserHandleCompat.myUserHandle());
        return profiles;
    }

    public UserHandleCompat getUserForSerialNumber(long serialNumber) {
        return UserHandleCompat.myUserHandle();
    }

    public Drawable getBadgedDrawableForUser(Drawable unbadged, UserHandleCompat user) {
        return unbadged;
    }

    public long getSerialNumberForUser(UserHandleCompat user) {
        return 0;
    }

    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user) {
        return label;
    }

    public long getUserCreationTime(UserHandleCompat user) {
        return 0;
    }

    public void enableAndResetCache() {
    }

    public boolean isQuietModeEnabled(UserHandleCompat user) {
        return false;
    }
}
