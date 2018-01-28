package com.diamond.SmartVoice.OAuth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

import com.diamond.SmartVoice.R;

/**
 * @author Dmitriy Ponomarev
 */
public class GetAccessTokenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_access_token);
        new OAuthAccessTokenTask(this).execute();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();
        System.out.println("OAuth onNewIntent: " + uri);
        if (uri != null && uri.toString().startsWith("x-oauthflow://callback")) {
            String code = uri.getQueryParameter("code");
            System.out.println("OAuth code: " + code);
        }
    }
}