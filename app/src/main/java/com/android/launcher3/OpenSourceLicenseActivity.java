package com.android.launcher3;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.sec.android.app.launcher.R;

public class OpenSourceLicenseActivity extends Activity {
    private static final String TAG = "OpenSourceLicenseInfoActivity";
    private WebSettings mWebSettings = null;
    private WebView mWebView = null;

    protected void onCreate(Bundle bundle) {
        setContentView(R.layout.about_licences);
        getActionBar().setDisplayOptions(28);
        this.mWebView = (WebView) findViewById(R.id.webview);
        this.mWebSettings = this.mWebView.getSettings();
        this.mWebView.setLongClickable(false);
        this.mWebView.setClickable(false);
        super.onCreate(bundle);
    }

    protected void onResume() {
        setTitle(R.string.about_licence);
        this.mWebView.loadUrl("file:///android_asset/NOTICE");
        this.mWebSettings.setTextZoom(65);
        this.mWebView.getSettings().setDefaultTextEncodingName("UTF-8");
        super.onResume();
    }

    public void onTrimMemory(int level) {
        switch (level) {
        }
        super.onTrimMemory(level);
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
}
