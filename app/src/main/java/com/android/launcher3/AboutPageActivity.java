package com.android.launcher3;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
//import android.os.SemSystemProperties;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class AboutPageActivity extends Activity {
    private static final String INCALLUI_PACKAGE_NAME = "com.sec.android.app.launcher";
    private static final String LOG_TAG = "CheckForUpdates";
    private static final String MCC_OF_CHINA = "460";
    public static final int RESULT_CODE_NOT_AVAILABLE = 0;
    public static final int RESULT_CODE_NOT_NECESSARY_TO_UPDATE = 1;
    public static final int RESULT_CODE_NO_NETWORK = 3;
    public static final int RESULT_CODE_PARAMETER_MISSING = 1000;
    public static final int RESULT_CODE_UNKNOWN = -1000;
    public static final int RESULT_CODE_UPDATE_AVAILABLE = 2;
    private static final String SAMSUNG_APPS_CLASS_NAME = "com.sec.android.app.samsungapps.Main";
    private static final String SAMSUNG_APPS_PACKAGE_NAME = "com.sec.android.app.samsungapps";
    private static final String STUB_UPDATE_CHECK_CHINA_URL = "http://cn-ms.samsungapps.com/getCNVasURL.as";
    private static final String STUB_UPDATE_CHECK_URL = "http://vas.samsungapps.com/stub/stubUpdateCheck.as";
    private static final String XML_TAG_APP_ID = "appId";
    private static final String XML_TAG_RESULT_CODE = "resultCode";
    private static final String XML_TAG_RESULT_MSG = "resultMsg";
    private static final String XML_TAG_VERSION_CODE = "versionCode";
    private static final String XML_TAG_VERSION_NAME = "versionName";
    private ActionBar mActionBar;
    private TextView mAppsInfo;
    private UpdateCheckTask mCheckUpdateTask;
    private Context mContext;
    private TextView mHelpText;
    private TextView mHelpTextTitle;
    private ImageView mImageView;
    private int mNeedUpdate;
    private Button mRetryButton;
    private Button mUpdateButton;

    private class UpdateCheckTask extends AsyncTask<Void, Void, Integer> {
        ProgressBar progressBar;

        private UpdateCheckTask() {
            this.progressBar = (ProgressBar) AboutPageActivity.this.findViewById(R.id.progreess_bar);
        }

        protected void onPreExecute() {
            this.progressBar.setVisibility(View.VISIBLE);
        }

        protected Integer doInBackground(Void... args) {
            return Integer.valueOf(AboutPageActivity.this.check("com.sec.android.app.launcher", true));
        }

        protected void onPostExecute(Integer needUpdate) {
            this.progressBar.setVisibility(View.GONE);
            AboutPageActivity.this.mNeedUpdate = needUpdate;
            if (needUpdate == 1 || needUpdate.intValue() == 0) {
                AboutPageActivity.this.mHelpText.setText(AboutPageActivity.this.getString(R.string.about_already_installed));
                AboutPageActivity.this.mUpdateButton.setVisibility(View.GONE);
                AboutPageActivity.this.mRetryButton.setVisibility(View.GONE);
            } else if (needUpdate.intValue() == 2) {
                AboutPageActivity.this.mHelpText.setText(AboutPageActivity.this.getString(R.string.about_new_version_exist));
                AboutPageActivity.this.mUpdateButton.setText(AboutPageActivity.this.getString(R.string.about_update));
                AboutPageActivity.this.mUpdateButton.setVisibility(View.VISIBLE);
                AboutPageActivity.this.mRetryButton.setVisibility(View.GONE);
            } else {
                if (LauncherFeature.isTablet()) {
                    AboutPageActivity.this.mHelpText.setText(AboutPageActivity.this.getString(R.string.about_network_err_tablet));
                } else {
                    AboutPageActivity.this.mHelpText.setText(AboutPageActivity.this.getString(R.string.about_network_err_phone));
                }
                AboutPageActivity.this.mRetryButton.setText(AboutPageActivity.this.getString(R.string.about_retry));
                AboutPageActivity.this.mRetryButton.setVisibility(View.VISIBLE);
                AboutPageActivity.this.mUpdateButton.setVisibility(View.GONE);
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getApplicationContext();
        this.mActionBar = getActionBar();
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        if (Utilities.canScreenRotate()) {
            switch (getResources().getConfiguration().orientation) {
                case 1:
                    setContentView(R.layout.about_page_layout);
                    init();
                    break;
                case 2:
                    setContentView(R.layout.about_page_layout_land);
                    init();
                    break;
            }
        }
        setContentView(R.layout.about_page_layout);
        init();
        this.mCheckUpdateTask = new UpdateCheckTask();
        this.mCheckUpdateTask.execute();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Utilities.canScreenRotate()) {
            switch (newConfig.orientation) {
                case 1:
                    setContentView(R.layout.about_page_layout);
                    init();
                    break;
                case 2:
                    setContentView(R.layout.about_page_layout_land);
                    init();
                    break;
            }
            if (this.mCheckUpdateTask != null) {
                this.mCheckUpdateTask.onPostExecute(this.mNeedUpdate);
            }
        }
    }

    private void init() {
        this.mHelpTextTitle = (TextView) findViewById(R.id.notice_title);
        this.mHelpText = (TextView) findViewById(R.id.notice_instruction);
        this.mImageView = (ImageView) findViewById(R.id.touchWiz_icon);
        this.mImageView.setImageResource(R.drawable.aboutpage_tw_home);
        this.mAppsInfo = (TextView) findViewById(R.id.app_info);
        this.mAppsInfo.setText(getText(R.string.about_app_info).toString().toUpperCase());
        this.mAppsInfo.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                LauncherAppsCompat.getInstance(AboutPageActivity.this.mContext).showAppDetailsForProfile(new ComponentName(AboutPageActivity.this.mContext.getPackageName(), AboutPageActivity.this.mContext.getApplicationInfo().className), UserHandleCompat.myUserHandle());
            }
        });
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            this.mActionBar.setDisplayShowTitleEnabled(true);
            this.mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (LauncherFeature.isJapanModel()) {
            ((TextView) findViewById(R.id.about_app_name)).setText(getText(R.string.application_name_galaxy).toString().toUpperCase());
        }
        setTitle(String.format(getString(R.string.about), getString(R.string.about_home_screen).toUpperCase()));
        this.mHelpTextTitle.setText(String.format(getString(R.string.about_version), getAppVersion()));
        TextView openSource = (TextView) findViewById(R.id.licence_textview);
        openSource.setText(Html.fromHtml("<u>" + getString(R.string.about_licence) + "</u>"));
        openSource.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                AboutPageActivity.this.startActivity(new Intent(AboutPageActivity.this.getBaseContext(), OpenSourceLicenseActivity.class));
            }
        });
        this.mUpdateButton = (Button) findViewById(R.id.redirect_button);
        this.mRetryButton = (Button) findViewById(R.id.retry_button);
        this.mRetryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (AboutPageActivity.this.mCheckUpdateTask != null) {
                    AboutPageActivity.this.mCheckUpdateTask.cancel(true);
                    AboutPageActivity.this.mCheckUpdateTask = null;
                }
                AboutPageActivity.this.mUpdateButton.setVisibility(View.GONE);
                AboutPageActivity.this.mRetryButton.setVisibility(View.GONE);
                AboutPageActivity.this.mCheckUpdateTask = new UpdateCheckTask();
                AboutPageActivity.this.mCheckUpdateTask.execute();
            }
        });
        this.mUpdateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AboutPageActivity.jumpToSamsungApps(AboutPageActivity.this.mContext);
            }
        });
        this.mAppsInfo.requestFocus();
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "NULL";
        }
    }

    private int check(String packageName, boolean needTimeOut) {
        String szModel = Build.MODEL;
        String szPrefix = "SAMSUNG-";
        String stubUpdateCheckURL = STUB_UPDATE_CHECK_URL;
        if (szModel.contains(szPrefix)) {
            szModel = szModel.replaceFirst(szPrefix, "");
        }
        if (isNetWorkConnected(this.mContext)) {
            // TODO: Check if this code is needed
//            int ret;
//            String mcc = SemSystemProperties.get("");
//            String mnc = SemSystemProperties.get("");
//            String operator = ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperator();
//            if (MCC_OF_CHINA.equals(mcc)) {
//                SharedPreferences pref = this.mContext.getSharedPreferences("StubAPI", 0);
//                String cnVasURL = pref.getString("cnVasURL", null);
//                long cnVasTime = pref.getLong("cnVasTime", 0);
//                if (cnVasURL == null || System.currentTimeMillis() - cnVasTime > 86400000) {
//                    cnVasURL = getCNVasURL();
//                    Editor editor = pref.edit();
//                    editor.putString("cnVasURL", cnVasURL);
//                    editor.putLong("cnVasTime", System.currentTimeMillis());
//                    editor.commit();
//                }
//                stubUpdateCheckURL = "http:" + cnVasURL + "/stub/stubUpdateCheck.as";
//            }
//            if (operator != null && operator.length() > 3) {
//                mcc = operator.substring(0, 3);
//                mnc = operator.substring(3);
//            }
//            try {
//                String versionCode = String.valueOf(this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionCode);
//                String requestUrl = String.format("%s?appId=%s&callerId=%s&versionCode=%s&deviceId=%s&mcc=%s&mnc=%s&csc=%s&sdkVer=%s&pd=%s", new Object[]{stubUpdateCheckURL, packageName, packageName, versionCode, szModel, mcc, mnc, SemSystemProperties.get("ro.csc.sales_code"), String.valueOf(VERSION.SDK_INT), isPdEnabled()});
//                Log.d(LOG_TAG, requestUrl);
//                ret = getResult(packageName, new URL(requestUrl), needTimeOut);
//            } catch (Exception e) {
//                Log.e(LOG_TAG, e.toString());
//                ret = 3;
//            }
//            return ret;
        }
        Log.i(LOG_TAG, "Connection failed");
        return 3;
    }

    // TODO: Check if this ode is needed
//    private int getResult(String packageName, URL url, boolean needTimeOut) {
//        int ret;
//        InputStream inputStream = null;
//        String resultCode = null;
//        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
//        if (needTimeOut) {
//            URLConnection con = url.openConnection();
//            con.setConnectTimeout(20000);
//            con.setReadTimeout(15000);
//            inputStream = con.getInputStream();
//        } else {
//            inputStream = url.openStream();
//        }
//        parser.setInput(inputStream, null);
//        for (int parserEvent = parser.getEventType(); parserEvent != 1; parserEvent = parser.next()) {
//            switch (parserEvent) {
//                case 2:
//                    String tag = parser.getName();
//                    if (XML_TAG_APP_ID.equals(tag)) {
//                        if (parser.next() != 4) {
//                            break;
//                        }
//                        Log.i(LOG_TAG, "appId: " + parser.getText());
//                        break;
//                    }
//                    try {
//                        if (!XML_TAG_RESULT_CODE.equals(tag)) {
//                            if (!XML_TAG_RESULT_MSG.equals(tag)) {
//                                if (!XML_TAG_VERSION_CODE.equals(tag)) {
//                                    if (XML_TAG_VERSION_NAME.equals(tag) && parser.next() == 4) {
//                                        Log.i(LOG_TAG, "versionName: " + parser.getText());
//                                        break;
//                                    }
//                                } else if (parser.next() != 4) {
//                                    break;
//                                } else {
//                                    Log.i(LOG_TAG, "versionCode: " + parser.getText());
//                                    break;
//                                }
//                            } else if (parser.next() != 4) {
//                                break;
//                            } else {
//                                Log.i(LOG_TAG, "resultMsg: " + parser.getText());
//                                break;
//                            }
//                        } else if (parser.next() != 4) {
//                            break;
//                        } else {
//                            resultCode = parser.getText();
//                            Log.i(LOG_TAG, "resultCode: " + resultCode);
//                            break;
//                        }
//                    } catch (Exception e) {
//                        Log.e(LOG_TAG, e.toString());
//                        ret = 3;
//                        if (inputStream != null) {
//                            try {
//                                inputStream.close();
//                                break;
//                            } catch (IOException e2) {
//                                Log.e(LOG_TAG, e2.toString());
//                                break;
//                            }
//                        }
//                    } catch (Throwable th) {
//                        if (inputStream != null) {
//                            try {
//                                inputStream.close();
//                            } catch (IOException e22) {
//                                Log.e(LOG_TAG, e22.toString());
//                            }
//                        }
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//        int resultCodeInt = Integer.parseInt(resultCode);
//        if (2 == resultCodeInt) {
//            ret = 2;
//        } else if (resultCodeInt == 0) {
//            ret = 0;
//        } else if (1 == resultCodeInt) {
//            ret = 1;
//        } else if (1000 == resultCodeInt) {
//            ret = 1000;
//        } else {
//            ret = -1000;
//        }
//        if (inputStream != null) {
//            try {
//                inputStream.close();
//            } catch (IOException e222) {
//                Log.e(LOG_TAG, e222.toString());
//            }
//        }
//        return ret;
//    }

    private String isPdEnabled() {
        return new File(Environment.getExternalStorageDirectory(), getPD()).exists() ? "1" : "0";
    }

    private String getPD() {
        return "go_" + "to_" + "andromeda" + ".test";
    }

    public static void jumpToSamsungApps(Context context) {
        Intent intent = new Intent();
        intent.setClassName(SAMSUNG_APPS_PACKAGE_NAME, SAMSUNG_APPS_CLASS_NAME);
        intent.putExtra("directcall", true);
        intent.putExtra("CallerType", 1);
        intent.putExtra("GUID", "com.sec.android.app.launcher");
        // TODO: Intent flag
        // intent.addFlags(335544352);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    public static boolean isNetWorkConnected(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getCNVasURL() {
        return STUB_UPDATE_CHECK_CHINA_URL;
    }
}
