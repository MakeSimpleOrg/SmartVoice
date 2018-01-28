package com.diamond.SmartVoice.OAuth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.diamond.SmartVoice.R;
import com.diamond.SmartVoice.SettingsActivity;

public class WebViewActivity extends Activity {
    public static SettingsActivity settingsActivity;

    WebView webview;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);

        webview = (WebView) findViewById(R.id.webview);

        //Log.w("Webview", "WebviewActivity" + getIntent().getExtras().getString("url"));

        webview.clearCache(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    CookieSyncManager.getInstance().sync();
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null && !cookies.isEmpty() && cookies.contains("bearer_token")) {
                        String[] all = cookies.split("; ");
                        for (String k : all) {
                            String[] m = k.split("=");
                            if ("bearer_token".equals(m[0])) {
                                System.out.println("BEARER: " + m[1]);
                                PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this).edit().putString("homey_bearer", m[1]).apply();
                                settingsActivity.findPreference("homey_bearer").setSummary(m[1]);
                                WebViewActivity.this.finish();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(getIntent().getExtras().getString("url"));
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
}