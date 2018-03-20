package com.android.launcher3.gamehome;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.samsung.android.game.SemGameManager;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GameHomeManager {
    static final Uri GAMEHOME_CONTENT_URI = Uri.parse(GAMEHOME_DATA_URI);
    private static final String GAMEHOME_DATA_URI = "content://com.samsung.android.game.gamehome.data/applist";
    private static final String GAME_APP_HIDDEN_ENABLE = "game_home_hidden_games";
    private static final String GAME_HOME_ENABLE = "game_home_enable";
    private static final int GAME_HOME_HIDDEN_MENU_DISABLE = 0;
    private static final int GAME_HOME_HIDDEN_OFF = 2;
    private static final int GAME_HOME_HIDDEN_ON = 1;
    public static final String GAME_HOME_PACKAGE = "com.samsung.android.game.gamehome";
    public static final int REQUEST_GAMEHOME_ENABLED = 101;
    private static final String TAG = GameHomeManager.class.getSimpleName();
    final String gameLauncherPkgName = GAME_HOME_PACKAGE;
    private BindGameAppRunnable mBindGameAppRunnable = new BindGameAppRunnable();
    private final ContentObserver mGameAppHideenSettingProviderObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.i(GameHomeManager.TAG, "game app hidden setting provider changed : ");
            GameHomeManager.this.updateGameAppsVisibility();
        }
    };
    private final ContentObserver mGameHomeProviderObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.i(GameHomeManager.TAG, "game home provider changed");
            GameHomeManager.this.updateGameAppsVisibility();
        }
    };
    private final ContentObserver mGameHomeSettingProviderObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.i(GameHomeManager.TAG, "game home setting provider changed : ");
            GameHomeManager.this.updateGameAppsVisibility();
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Launcher mLauncher;

    private class BindGameAppRunnable implements Runnable {
        private Runnable mRunnable;

        private BindGameAppRunnable() {
        }

        public synchronized void setRunnable(Runnable run) {
            this.mRunnable = run;
        }

        public synchronized void run() {
            Runnable runnable = null;
            if (this.mRunnable != null) {
                runnable = this.mRunnable;
                this.mRunnable = null;
            }
            if (runnable != null) {
                runnable.run();
            } else {
                GameHomeManager.this.bindGameAppsVisibility();
            }
        }
    }

    private static class SingletonHolder {
        private static final GameHomeManager sGameHomeManager = new GameHomeManager();

        private SingletonHolder() {
        }
    }

    public static GameHomeManager getInstance() {
        return SingletonHolder.sGameHomeManager;
    }

    public void initGameHomeManager(Launcher launcher) {
        this.mLauncher = launcher;
        ContentResolver resolver = this.mLauncher.getContentResolver();
        resolver.registerContentObserver(GAMEHOME_CONTENT_URI, true, this.mGameHomeProviderObserver);
        resolver.registerContentObserver(Secure.getUriFor(GAME_HOME_ENABLE), false, this.mGameHomeSettingProviderObserver);
        resolver.registerContentObserver(Secure.getUriFor(GAME_APP_HIDDEN_ENABLE), false, this.mGameAppHideenSettingProviderObserver);
        Log.d(TAG, "game_home_hidden_games(0/2:off, 1:on) initial value : " + Secure.getInt(resolver, GAME_APP_HIDDEN_ENABLE, 0));
        if (Secure.getInt(resolver, GAME_APP_HIDDEN_ENABLE, 0) == 0) {
            Secure.putInt(resolver, GAME_APP_HIDDEN_ENABLE, 2);
        }
    }

    public synchronized void updateGameAppsVisibility() {
        updateGameAppsVisibility(null);
    }

    public synchronized void updateGameAppsVisibility(Runnable run) {
        ContentResolver resolver = this.mLauncher.getContentResolver();
        int gameHidden = Secure.getInt(resolver, GAME_APP_HIDDEN_ENABLE, 0);
        if (run != null) {
            Log.d(TAG, "Game Home Hidden setting : " + gameHidden + " , GameLauncher is disabled");
        } else {
            Log.d(TAG, "Game Home Hidden setting : " + gameHidden);
        }
        if (gameHidden == 0) {
            Secure.putInt(resolver, GAME_APP_HIDDEN_ENABLE, 2);
        }
        if (run != null) {
            this.mBindGameAppRunnable.setRunnable(run);
        }
        if (!this.mLauncher.waitUntilResume(this.mBindGameAppRunnable, true)) {
            this.mBindGameAppRunnable.run();
        }
    }

    public void bindGameAppsVisibility() {
        ArrayList<ItemInfo> apps;
        HashMap<String, String> gameAppList = getGameAppListFromGameHome(this.mLauncher);
        ContentResolver resolver = this.mLauncher.getContentResolver();
        int hiddenFlag = 0;
        if (gameAppList.isEmpty()) {
            Log.e(TAG, "GameHomeProvider does not have any item, but we should care this case.");
        }
        boolean isHomeOnly = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        int gameHidden = Secure.getInt(resolver, GAME_APP_HIDDEN_ENABLE, 0);
        Log.d(TAG, "Game Home Hidden setting before binding(0/2:off, 1:on) : " + gameHidden);
        if (gameHidden == 1) {
            hiddenFlag = 4;
        }
        LauncherModel launcherModel = this.mLauncher.getLauncherModel();
        if (isHomeOnly) {
            apps = launcherModel.getHomeLoader().getAllAppItemInHome();
        } else {
            apps = launcherModel.getAppsModel().getAllAppItemInApps();
        }
        ArrayList<ItemInfo> hiddenApps = new ArrayList();
        ArrayList<ItemInfo> unHiddenApps = new ArrayList();
        ArrayList<ItemInfo> updateApps = new ArrayList();
        Log.d(TAG, "bindGameAppsVisibility hiddenFlag = " + hiddenFlag);
        UserHandleCompat myUserHandle = UserHandleCompat.myUserHandle();
        Iterator it = apps.iterator();
        while (it.hasNext()) {
            String packageName;
            ItemInfo app = (ItemInfo) it.next();
            if (app.getIntent() == null || app.getIntent().getComponent() == null || app.getIntent().getComponent().getPackageName() == null) {
                packageName = null;
            } else {
                packageName = app.getIntent().getComponent().getPackageName();
            }
            if (gameAppList.containsKey(packageName) && myUserHandle.equals(app.user)) {
                app.setGameApp(true);
                Log.i(TAG, "bindGameAppsVisibility " + app.title + " hidden = " + app.hidden);
                switch (hiddenFlag) {
                    case 0:
                        if (!app.isHiddenByUser()) {
                            if (!app.isHiddenByGame()) {
                                break;
                            }
                            Log.d(TAG, "4. " + app.title + ": This app is in the game list but the game hidden setting is unhidden, so we unhide this app newly.");
                            unHiddenApps.add(app);
                            break;
                        }
                        Log.d(TAG, "3. " + app.title + ": This app is already hidden by user, so we just remove the game hidden flag ");
                        app.setUnHidden(4);
                        updateApps.add(app);
                        break;
                    case 4:
                        if (!app.isHiddenByUser()) {
                            if (!app.isHiddenByGame()) {
                                if (!LauncherFeature.supportHomeModeChange() || !LauncherAppState.getInstance().isEasyModeEnabled()) {
                                    Log.i(TAG, "2. " + app.title + ": This app is newly added by game, so we should hide this app by game.");
                                    hiddenApps.add(app);
                                    break;
                                }
                                Log.i(TAG, "2-0. " + app.title + ": This app is game app but not hidden in EASY MODE");
                                break;
                            }
                            break;
                        }
                        Log.d(TAG, "1. " + app.title + ": This app is already hidden by user, so we just add the game hidden flag");
                        app.setHidden(4);
                        updateApps.add(app);
                        break;
                    default:
                        break;
                }
            }
            if (app.isHiddenByUser() && app.isGameApp()) {
                Log.d(TAG, "5. " + app.title + ": This app is remove in game launcher newly but already user hidden, so we just remove the game hidden flag ");
                app.setUnHidden(4);
                updateApps.add(app);
            } else if (!app.isHiddenByUser() && app.isHiddenByGame()) {
                Log.e(TAG, "6. " + app.title + ": This app is newly removed in game launcher, so we unhide this app newly.");
                unHiddenApps.add(app);
            }
            app.setGameApp(false);
        }
        if (hiddenApps.size() > 0) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(GameHomeManager.this.mLauncher, R.string.shortcut_moved_to_game_launcher, 0).show();
                }
            });
        }
        this.mLauncher.updateItemInfo(hiddenApps, unHiddenApps, true);
        if (!updateApps.isEmpty()) {
            Log.d(TAG, "just update db updateApps.size=" + updateApps.size());
            this.mLauncher.getLauncherModel().updateAppsOnlyDB(updateApps);
        }
    }

    private HashMap<String, String> getGameAppListFromGameHome(Context context) {
        Uri uri = Uri.parse(GAMEHOME_DATA_URI);
        Cursor c = null;
        HashMap<String, String> gameAppList = new HashMap();
        try {
            c = context.getContentResolver().query(uri, new String[0], null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    String packageName = c.getString(0);
                    gameAppList.put(packageName, "game");
                    if (Utilities.DEBUGGABLE()) {
                        Log.i(TAG, "getGameAppListFromGameHomeProvider gameApp=" + packageName);
                    }
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Game home provider does not exist");
            if (c != null) {
                c.close();
            }
        }
        return gameAppList;
    }

    public void checkGameAppList(ArrayList<ItemInfo> apps) {
        HashMap<String, String> gameAppList = getGameAppListFromGameHome(LauncherAppState.getInstance().getContext());
        if (gameAppList == null) {
            Log.e(TAG, "checkGameAppList : GameHomeProvider does not ready yet!!!!!");
            return;
        }
        Iterator it = apps.iterator();
        while (it.hasNext()) {
            ItemInfo app = (ItemInfo) it.next();
            if ((app instanceof IconInfo) && gameAppList.containsKey(((IconInfo) app).getTargetComponent().getPackageName()) && !app.isGameApp()) {
                if (Utilities.DEBUGGABLE()) {
                    Log.i(TAG, "" + app.getIntent().getComponent().getPackageName() + " is game app !!");
                }
                app.setGameApp(true);
            }
        }
    }

    public void onTerminate() {
        if (this.mLauncher != null) {
            ContentResolver cr = this.mLauncher.getContentResolver();
            cr.unregisterContentObserver(this.mGameHomeProviderObserver);
            cr.unregisterContentObserver(this.mGameHomeSettingProviderObserver);
            cr.unregisterContentObserver(this.mGameAppHideenSettingProviderObserver);
        }
    }

    public boolean isGamePackages(String package1, String package2) {
        try {
            if (SemGameManager.isAvailable()) {
                boolean ret1 = SemGameManager.isGamePackage(package1);
                boolean ret2 = SemGameManager.isGamePackage(package2);
                if (Utilities.DEBUGGABLE()) {
                    Log.i(TAG, package1 + " = " + ret1 + "  " + package2 + " = " + ret2);
                }
                if (ret1 && ret2) {
                    return true;
                }
                return false;
            }
            Log.e(TAG, "Game Manager is unavailable");
            return false;
        } catch (RuntimeException e) {
            Log.e(TAG, "identifyGamePackage error");
            e.printStackTrace();
            return false;
        } catch (NoClassDefFoundError e2) {
            Log.e(TAG, "No GameManager class!!!");
            return false;
        }
    }

    public void startGameHUN(Context context, IconInfo item1, IconInfo item2) {
        if (Utilities.isPackageExist(context, GAME_HOME_PACKAGE) && Secure.getInt(context.getContentResolver(), GAME_HOME_ENABLE, 0) != 1) {
            ComponentName itemComponent1 = item1.getIntent().getComponent();
            ComponentName itemComponent2 = item2.getIntent().getComponent();
            if (itemComponent1 != null && itemComponent2 != null && isGamePackages(itemComponent1.getPackageName(), itemComponent2.getPackageName())) {
                Intent intent = new Intent();
                intent.setAction("com.samsung.android.game.gamehome.action.ENABLE_GAMEHOME");
                intent.putExtra("package1", itemComponent1.getPackageName());
                intent.putExtra("package2", itemComponent2.getPackageName());
                Utilities.startActivityForResultSafely(this.mLauncher, intent, REQUEST_GAMEHOME_ENABLED);
            }
        }
    }

    public void resetGameHomeHiddenValue() {
        Secure.putInt(this.mLauncher.getContentResolver(), GAME_APP_HIDDEN_ENABLE, 2);
    }

    public boolean isGameHomeHidden() {
        int hidden = Secure.getInt(this.mLauncher.getContentResolver(), GAME_APP_HIDDEN_ENABLE, 2);
        if (Utilities.DEBUGGABLE()) {
            Log.i(TAG, "hidden setting (0/2:off, 1:on) = " + hidden);
        }
        if (hidden == 1) {
            return true;
        }
        return false;
    }

    public boolean hasGameHomeThisPackage(LauncherActivityInfoCompat info) {
        HashMap<String, String> gameAppList = getGameAppListFromGameHome(LauncherAppState.getInstance().getContext());
        if (gameAppList == null) {
            Log.e(TAG, "isGamePackage GameHomeProvider does not ready yet!!!");
            return false;
        } else if (info == null || info.getApplicationInfo() == null) {
            return false;
        } else {
            String packageName = info.getApplicationInfo().packageName;
            if (!gameAppList.containsKey(packageName)) {
                return false;
            }
            Log.d(TAG, "This app is Game : " + packageName);
            return true;
        }
    }
}
