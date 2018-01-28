package com.diamond.SmartVoice.OAuth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.common.exception.OAuthSystemException;

/**
 * @author Dmitriy Ponomarev
 */
public class OAuthAccessTokenTask extends AsyncTask<Void, Void, Void> {
    private final String TAG = getClass().getName();
    @SuppressLint("StaticFieldLeak")
    private Context context;

    public OAuthAccessTokenTask(Context context) {

        this.context = context;
    }

    protected Void doInBackground(Void... params) {
        try {
            System.setProperty("https.protocols", "SSLv3");
            OAuthClientRequest request = null;

            try {
                request = OAuthClientRequest
                        .authorizationLocation("https://accounts.athom.com/login")
                        .setClientId("5534df95588a5ed82aaef73d").setRedirectURI("https://my.athom.com/auth/callback")
                        .setResponseType("code")
                        .setParameter("origin", "https://accounts.athom.com/oauth2/authorise")
                        .buildQueryMessage();
            } catch (OAuthSystemException e) {
                e.printStackTrace();
            }

            if (request != null) {
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("url", request.getLocationUri());
                context.startActivity(intent);
            }
        } catch (Exception e) {

            Log.e(TAG, "Error during OAUth retrieve request token", e);
        }

        return null;
    }
}
