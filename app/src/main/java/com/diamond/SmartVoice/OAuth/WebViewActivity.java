package com.diamond.SmartVoice.OAuth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Homey.Homey;
import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;

/**
 * @author Dmitriy Ponomarev
 */
public class WebViewActivity extends Activity {
    private static final String TAG = WebViewActivity.class.getSimpleName();
    //public static MainActivity mainActivity;

    WebView webview;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        webview = (WebView) findViewById(R.id.webview);
        webview.clearCache(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            webview.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
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
                            else if ("homey_id".equals(m[0]))
                                homey_id = m[1];
                            //else
                            //    Log.i(TAG, "Param: " + m[0] + "=" + m[1]);
                            //11-18 00:11:59.418 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_bootid=*
                            //11-18 00:11:59.418 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_language=en
                            //11-18 00:11:59.418 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_cloud=1
                            //11-18 00:11:59.419 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_channel=stable
                            //11-18 00:11:59.421 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_name=Homey
                            //11-18 00:11:59.421 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_forcechannel=false
                            //11-18 00:11:59.421 20470-20470/com.diamond.SmartVoice I/WebViewActivity: Param: homey_version=1.5.12
                        }

                        if (bearer != null) {
                            System.out.println("bearer: " + bearer);
                            System.out.println("homey_id: " + homey_id);

                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(WebViewActivity.this);

                            pref.edit().putString("homey_bearer", bearer).apply();
                            //settingsActivity.findPreference("homey_bearer").setSummary(bearer);

                            pref.edit().putString("homey_id", homey_id).apply();
                            pref.edit().putString("homey_server_ip_ext", "https://" + homey_id + ".homey.athom.com").apply();

                            if (!pref.getString("homey_server_ip", "").isEmpty())
                                for (final Controller controller : MainActivity.controllers)
                                    if (controller instanceof Homey) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                controller.loadData();
                                            }
                                        });
                                    }

                            webview.destroy();
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