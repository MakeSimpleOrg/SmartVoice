package com.diamond.SmartVoice.OAuth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;
import com.diamond.SmartVoice.SettingsActivity;

/**
 * @author Dmitriy Ponomarev
 */
public class WebViewActivity extends Activity {
    public static MainActivity mainActivity;
    public static SettingsActivity settingsActivity;

    WebView webview;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        webview = (WebView) findViewById(R.id.webview);
        webview.clearCache(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    CookieSyncManager.getInstance().sync();
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null && !cookies.isEmpty() && cookies.contains("bearer_token")) {
                        String bearer = null;
                        String homey_id = null;
                        String[] all = cookies.split("; ");
                        for (String k : all) {
                            String[] m = k.split("=");
                            if ("bearer_token".equals(m[0]))
                                bearer = m[1];
                            if ("homey_id".equals(m[0]))
                                homey_id = m[1];
                        }

                        if (bearer != null) {
                            System.out.println("bearer: " + bearer);
                            System.out.println("homey_id: " + homey_id);
                            PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this).edit().putString("homey_bearer", bearer).apply();
                            settingsActivity.findPreference("homey_bearer").setSummary(bearer);

                            PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this).edit().putString("homey_id", homey_id).apply();

                            if (!mainActivity.pref.getString("homey_server_ip", "").isEmpty())
                                MainActivity.setupHomey(mainActivity);

                            WebViewActivity.this.finish();
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