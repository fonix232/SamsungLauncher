package com.android.launcher3.common.model;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.BadgeCache.CacheKey;
import com.android.launcher3.common.model.BadgeSettingsAdapter.OnChangeListener;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BadgeSettingsFragment extends Fragment {
    private static final String TAG = "BadgeSettingsFragment";
    private final Comparator<BadgeAppItem> BADGE_APP_COMPARATOR = new Comparator<BadgeAppItem>() {
        private Collator mCollator = Collator.getInstance(Locale.getDefault());

        public int compare(BadgeAppItem a, BadgeAppItem b) {
            if ((a.isHidden() && b.isHidden()) || (!a.isHidden() && !b.isHidden())) {
                return this.mCollator.compare(a.getTitle(), b.getTitle());
            }
            if (a.isHidden()) {
                return 1;
            }
            return -1;
        }
    };
    private final String[] BADGE_MANAGE_COLUMNS = new String[]{"package", "class", "hidden"};
    private LinearLayout allSwitchLayout;
    private boolean isDatabaseLoaderRunning = false;
    private BadgeSettingsAdapter mAdapter;
    private Switch mAllSwitch;
    private List<BadgeAppItem> mBadgeAppItems = new ArrayList();
    private BadgeAppLoader mBadgeAppLoader;
    private int mBadgeSettings = 2;
    private final List<CacheKey> mBadges_Hidden_DualApp = new ArrayList();
    private Context mContext;
    private DatabaseLoader mDatabaseLoader;
    private ProgressDialog progressDialog;
    private RecyclerView recyclerView;

    public class BadgeAppItem {
        private Drawable appIcon;
        private String className;
        private boolean hasChange = false;
        private boolean hidden;
        private UserHandleCompat mUser;
        private String packageName;
        final /* synthetic */ BadgeSettingsFragment this$0;
        private String title;

        BadgeAppItem(BadgeSettingsFragment this$0, LauncherActivityInfoCompat infoCompat) {
            boolean z = false;
            this.this$0 = this$0;
            this.title = infoCompat.getLabel().toString();
            this.appIcon = infoCompat.getBadgedIconForIconTray(LauncherAppState.getInstance().getIconCache().getIconDpi());
            this.packageName = infoCompat.getComponentName().getPackageName();
            this.className = infoCompat.getComponentName().getClassName();
            this.mUser = infoCompat.getUser();
            if (this$0.mBadges_Hidden_DualApp.contains(new CacheKey(infoCompat.getComponentName(), infoCompat.getUser())) || this$0.isDisableAllAppsBadge()) {
                z = true;
            }
            this.hidden = z;
            if (!this$0.mBadges_Hidden_DualApp.contains(new CacheKey(infoCompat.getComponentName(), infoCompat.getUser())) && this$0.isDisableAllAppsBadge()) {
                this.hasChange = true;
            }
        }

        public String getTitle() {
            return this.title;
        }

        public Drawable getAppIcon() {
            return this.appIcon;
        }

        public boolean isHidden() {
            return this.hidden;
        }

        public void setHidden(boolean hid) {
            if (hid != this.hidden) {
                this.hasChange = !this.hasChange;
                this.hidden = hid;
            }
        }

        public String getPackageName() {
            return this.packageName;
        }

        public String getClassName() {
            return this.className;
        }

        public UserHandleCompat getUser() {
            return this.mUser;
        }

        public boolean isHasChange() {
            return this.hasChange;
        }

        public void setHasChange(boolean hasChange) {
            this.hasChange = hasChange;
        }

        public String toString() {
            return "BadgeAppItem{title='" + this.title + '\'' + ", hidden=" + this.hidden + ", hasChange=" + this.hasChange + '}';
        }
    }

    private class BadgeAppLoader extends AsyncTask<Void, Void, Void> {
        private BadgeAppLoader() {
        }

        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            BadgeSettingsFragment.this.mAllSwitch.setChecked(BadgeSettingsFragment.this.mBadges_Hidden_DualApp.isEmpty());
            BadgeSettingsFragment.this.mAdapter.notifyDataSetChanged();
            BadgeSettingsFragment.this.dismissProgress();
            Log.d(BadgeSettingsFragment.TAG, "BadgeAppLoader onPostExecute: ");
        }

        protected Void doInBackground(Void... params) {
            Log.d(BadgeSettingsFragment.TAG, "BadgeAppLoader doInBackground: ");
            BadgeSettingsFragment.this.loadBadgeProvider();
            BadgeSettingsFragment.this.createAppItemArray();
            return null;
        }
    }

    private class DatabaseLoader extends AsyncTask<Void, Void, Void> {
        private DatabaseLoader() {
        }

        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            BadgeSettingsFragment.this.mDatabaseLoader = null;
            if (BadgeSettingsFragment.this.isDatabaseLoaderRunning) {
                BadgeSettingsFragment.this.updateList();
                BadgeSettingsFragment.this.dismissProgress();
                BadgeSettingsFragment.this.isDatabaseLoaderRunning = false;
            }
            Log.d(BadgeSettingsFragment.TAG, "updateDatabaseLoader onPostExecute: UpdateDatabase finish!");
        }

        protected Void doInBackground(Void... params) {
            Log.d(BadgeSettingsFragment.TAG, "updateDatabaseLoader doInBackground: ");
            BadgeSettingsFragment.this.updateAppBadgeIntoDatabase();
            return null;
        }

        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
        }

        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        Log.d(TAG, "onCreate: ");
        startLoader();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.badge_settings_fragment_layout, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.list);
        this.mAllSwitch = (Switch) view.findViewById(R.id.badge_switch_all);
        this.allSwitchLayout = (LinearLayout) view.findViewById(R.id.all_switch_layout);
        this.progressDialog = new ProgressDialog(this.mContext, R.style.LoadingTheme);
        if (this.recyclerView != null) {
            this.recyclerView.setLayoutManager(new LinearLayoutManager(this.mContext));
            this.mAdapter = new BadgeSettingsAdapter(this.mContext, this.mBadgeAppItems);
            this.mAdapter.setOnChangeListener(new OnChangeListener() {
                public void onChange(View view, int position) {
                    BadgeSettingsFragment.this.updateAllSwitch();
                }
            });
            this.recyclerView.setAdapter(this.mAdapter);
        }
        this.mBadgeSettings = LauncherAppState.getInstance().getBadgeSetings();
        if (this.mBadgeSettings == 2) {
            this.mAllSwitch.setChecked(true);
        } else {
            this.mAllSwitch.setChecked(false);
        }
        if (this.allSwitchLayout != null) {
            this.allSwitchLayout.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    boolean oldState = BadgeSettingsFragment.this.mAllSwitch.isChecked();
                    BadgeSettingsFragment.this.mAllSwitch.setChecked(!oldState);
                    BadgeSettingsFragment.this.refreshAllAppItems(oldState);
                    SALogging.getInstance().insertEventLog(BadgeSettingsFragment.this.mContext.getResources().getString(R.string.screen_HomeSettings), BadgeSettingsFragment.this.mContext.getResources().getString(R.string.event_HideAllBadges), oldState ? 1 : 0);
                }
            });
        }
        return view;
    }

    private void refreshAllAppItems(boolean hidden) {
        for (BadgeAppItem app : this.mBadgeAppItems) {
            app.setHidden(hidden);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    private void updateAllSwitch() {
        boolean all = true;
        for (BadgeAppItem app : this.mBadgeAppItems) {
            if (app.isHidden()) {
                all = false;
                break;
            }
        }
        SALogging.getInstance().insertEventLog(this.mContext.getResources().getString(R.string.screen_HomeSettings), this.mContext.getResources().getString(R.string.event_HideAllBadges), all ? 1 : 0);
        this.mAllSwitch.setChecked(all);
    }

    public void updateAppBadgeIntoDatabase() {
        if (this.mAdapter != null) {
            Log.d(TAG, "updateAppBadgeIntoDatabase: count = " + this.mAdapter.getItemCount());
            if (this.mAdapter.getItemCount() > 0) {
                boolean hasShow = false;
                boolean hasHide = false;
                for (BadgeAppItem app : this.mBadgeAppItems) {
                    if (!(hasShow || app.isHidden())) {
                        hasShow = true;
                    }
                    if (!hasHide && app.isHidden()) {
                        hasHide = true;
                    }
                    if (app.isHasChange()) {
                        String pakcgeName = app.getPackageName();
                        String className = app.getClassName();
                        boolean hidden = app.isHidden();
                        UserHandleCompat user = app.getUser();
                        app.setHasChange(false);
                        setAppBadgeStatus(this.mContext, pakcgeName, className, user, hidden ? 1 : 0);
                    }
                }
                if (hasHide && hasShow) {
                    LauncherAppState.getInstance().setBadgeSetings(1);
                } else if (hasHide) {
                    LauncherAppState.getInstance().setBadgeSetings(0);
                } else {
                    LauncherAppState.getInstance().setBadgeSetings(2);
                }
            }
        }
        Log.d(TAG, "updateAppBadgeIntoDatabase: done ");
    }

    public static void setAppBadgeStatus(Context context, String packageName, String className, UserHandleCompat user, int value) {
        Uri badgeUri = BadgeCache.BADGE_URI;
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            badgeUri = BadgeCache.maybeAddUserId(BadgeCache.BADGE_URI, user.hashCode());
            if (badgeUri != null) {
                badgeUri = badgeUri.buildUpon().appendQueryParameter("noMultiUser", String.valueOf(true)).build();
            } else {
                return;
            }
        }
        String[] args = new String[]{packageName, className};
        ContentValues contentValues = new ContentValues();
        contentValues.put("hidden", Integer.valueOf(value));
        if (context.getContentResolver().update(badgeUri, contentValues, "package=? AND class=?", args) == 0) {
            contentValues.put("package", packageName);
            contentValues.put("class", className);
            contentValues.put("badgecount", Integer.valueOf(0));
            context.getContentResolver().insert(badgeUri, contentValues);
        }
    }

    public void onPause() {
        super.onPause();
        startDatabaseLoader();
        Log.d(TAG, "onPause: ");
    }

    public void onDestroy() {
        if (!(this.mBadgeAppLoader == null || this.mBadgeAppLoader.getStatus() == Status.FINISHED)) {
            this.mBadgeAppLoader.cancel(true);
            this.mBadgeAppLoader = null;
        }
        if (this.mDatabaseLoader != null) {
            Log.d(TAG, "onDestroy: mDatabaseLoader" + this.mDatabaseLoader.getStatus());
        }
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    private boolean isDisableAllAppsBadge() {
        return this.mBadgeSettings == 0;
    }

    private void createAppItemArray() {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this.mContext);
        List<UserHandleCompat> profiles = UserManagerCompat.getInstance(this.mContext).getUserProfiles();
        this.mBadgeAppItems.clear();
        for (UserHandleCompat profileUser : profiles) {
            List<LauncherActivityInfoCompat> appList = launcherApps.getActivityList(null, profileUser);
            if (!(appList == null || appList.isEmpty())) {
                for (LauncherActivityInfoCompat app : appList) {
                    this.mBadgeAppItems.add(new BadgeAppItem(this, app));
                }
            }
        }
        Collections.sort(this.mBadgeAppItems, this.BADGE_APP_COMPARATOR);
    }

    public void showProgress() {
        if (this.progressDialog != null) {
            this.progressDialog.setCancelable(false);
            this.progressDialog.setIndeterminate(true);
            this.progressDialog.show();
        }
        this.recyclerView.setVisibility(View.INVISIBLE);
    }

    private void dismissProgress() {
        if (this.progressDialog != null) {
            this.progressDialog.dismiss();
        }
        this.recyclerView.setVisibility(View.VISIBLE);
    }

    public boolean isRunning() {
        if (this.mDatabaseLoader == null || this.mDatabaseLoader.getStatus() == Status.FINISHED) {
            return false;
        }
        return true;
    }

    public void updateList() {
        if (this.mDatabaseLoader != null && this.mDatabaseLoader.getStatus() != Status.FINISHED) {
            showProgress();
            this.isDatabaseLoaderRunning = true;
            Log.d(TAG, "updateList: mDatabaseLoader is Running...");
        } else if (this.mBadgeAppLoader != null) {
            Log.d(TAG, "updateList() called with: mBadgeAppLoader.getStatus()= " + this.mBadgeAppLoader.getStatus());
            if (this.mBadgeAppLoader.getStatus() != Status.FINISHED) {
                showProgress();
            }
            if (this.mBadgeAppLoader.getStatus() == Status.FINISHED) {
                Collections.sort(this.mBadgeAppItems, this.BADGE_APP_COMPARATOR);
                this.mAdapter.notifyDataSetChanged();
                this.recyclerView.scrollToPosition(0);
            }
        }
    }

    private boolean hasActivityForComponent(ComponentName cn, UserHandleCompat user) {
        List<LauncherActivityInfoCompat> apps = LauncherAppsCompat.getInstance(this.mContext).getActivityList(cn.getPackageName(), user);
        if (apps != null) {
            for (LauncherActivityInfoCompat info : apps) {
                if (cn.equals(info.getComponentName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadBadgeProvider() {
        Log.d(TAG, "loadBadgeProvider");
        this.mBadges_Hidden_DualApp.clear();
        for (UserHandleCompat profileUser : UserManagerCompat.getInstance(this.mContext).getUserProfiles()) {
            Uri badgeUri = BadgeCache.BADGE_URI;
            if (!profileUser.equals(UserHandleCompat.myUserHandle())) {
                badgeUri = BadgeCache.maybeAddUserId(BadgeCache.BADGE_URI, profileUser.hashCode());
                if (badgeUri != null) {
                    badgeUri = badgeUri.buildUpon().appendQueryParameter("noMultiUser", String.valueOf(true)).build();
                } else {
                    return;
                }
            }
            Cursor c = null;
            try {
                c = this.mContext.getContentResolver().query(badgeUri, this.BADGE_MANAGE_COLUMNS, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String pkgName = c.getString(0);
                        String className = c.getString(1);
                        if (c.getInt(2) == 1 && className != null) {
                            ComponentName cn = new ComponentName(pkgName, className);
                            if (hasActivityForComponent(cn, profileUser)) {
                                this.mBadges_Hidden_DualApp.add(new CacheKey(cn, profileUser));
                            }
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException e = " + e);
                if (c != null) {
                    c.close();
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    public void startLoader() {
        if (LauncherFeature.isSupportBadgeManage()) {
            if (!(this.mBadgeAppLoader == null || this.mBadgeAppLoader.getStatus() == Status.FINISHED)) {
                this.mBadgeAppLoader.cancel(true);
            }
            this.mBadgeAppLoader = new BadgeAppLoader();
            this.mBadgeAppLoader.execute(new Void[0]);
        }
    }

    public void startDatabaseLoader() {
        if (!LauncherFeature.isSupportBadgeManage()) {
            return;
        }
        if (this.mDatabaseLoader == null || this.mDatabaseLoader.getStatus() == Status.FINISHED) {
            this.mDatabaseLoader = new DatabaseLoader();
            this.mDatabaseLoader.execute(new Void[0]);
            return;
        }
        Log.d(TAG, "startDatabaseLoader: return ,mDatabaseLoader" + this.mDatabaseLoader.getStatus());
    }

    public void enableAllAppsbadge(boolean enable) {
        if ((enable && this.mAllSwitch.isChecked())) {
            this.allSwitchLayout.performClick();
        }
    }

    public void enableAppBadge(String name, boolean enable) {
        int size = this.mBadgeAppItems.size();
        for (int i = 0; i < size; i++) {
            BadgeAppItem item = (BadgeAppItem) this.mBadgeAppItems.get(i);
            if (item.getTitle().equals(name)) {
                item.setHidden(!enable);
                if (item.isHasChange()) {
                    this.mAdapter.notifyItemChanged(i);
                }
            }
        }
        updateAllSwitch();
    }

    public boolean isSingleAppBadgeChecked(String className) {
        int size = this.mBadgeAppItems.size();
        int i = 0;
        while (i < size) {
            BadgeAppItem item = (BadgeAppItem) this.mBadgeAppItems.get(i);
            if (!item.getClassName().equals(className)) {
                i++;
            } else if (item.isHidden()) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean isAllAppsBadgeSwitchChecked() {
        return this.mAllSwitch.isChecked();
    }
}
