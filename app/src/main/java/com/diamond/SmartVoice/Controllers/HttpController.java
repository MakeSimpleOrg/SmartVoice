package com.diamond.SmartVoice.Controllers;

import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLProtocolException;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class HttpController extends Controller {
    private static final String TAG = HttpController.class.getSimpleName();

    private static ObjectMapper mapper = new ObjectMapper().registerModule(new JsonOrgModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    protected Gson gson;
    protected String bearer;

    protected String request(String request, String cookie) {
        String result = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = getURL(request);
            Log.v(TAG, "Sending request: " + url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (auth != null)
                conn.setRequestProperty("Authorization", "Basic " + auth);
            else if (bearer != null)
                conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            if (cookie != null)
                conn.setRequestProperty("Cookie", cookie);
            conn.setConnectTimeout(10000);
            conn.connect();
            is = conn.getInputStream();
            result = new String(ByteStreams.toByteArray(is));
        } catch (SSLProtocolException e) { /**/ } catch (Exception e) {
            Log.v(TAG, "Error while send request: " + request);
        } finally {
            if (conn != null)
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    Log.v(TAG, "Cannot close connection: " + e);
                }
        }
        return result;
    }

    protected <T> T getJson(String request, String cookie, Class<T> c) {
        long time = System.currentTimeMillis();
        T result = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = getURL(request);
            Log.v(TAG, "Sending request: " + url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (auth != null)
                conn.setRequestProperty("Authorization", "Basic " + auth);
            else if (bearer != null)
                conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            if (cookie != null)
                conn.setRequestProperty("Cookie", cookie);
            conn.setConnectTimeout(10000);
            conn.connect();
            is = conn.getInputStream();
            result = mapper.readValue(is, c);
        } catch (SSLProtocolException e) { /**/ } catch (Exception e) {
            Log.v(TAG, "Error while send request: " + request, e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (Exception e) { /**/ }
            if (conn != null)
                try {
                    conn.disconnect();
                } catch (Exception e) { /**/ }
        }
        Log.v(TAG, "Request time: " + (System.currentTimeMillis() - time));
        return result;
    }

    protected void sendCommand(final String request) {
        sendCommand(request, null);
    }

    protected void sendCommand(final String request, final String cookie) {
        Log.v(TAG, "Command: " + request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                try {
                    URL url = getURL(request);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (auth != null)
                        conn.setRequestProperty("Authorization", "Basic " + auth);
                    else if (bearer != null)
                        conn.setRequestProperty("Authorization", "Bearer " + bearer);
                    if (cookie != null)
                        conn.setRequestProperty("Cookie", cookie);
                    conn.setConnectTimeout(10000);
                    is = conn.getInputStream();
                } catch (SSLProtocolException e) { /**/ } catch (Exception e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (Exception e) { /**/ }
                    if (conn != null)
                        try {
                            conn.disconnect();
                        } catch (Exception e) { /**/ }
                }
            }
        }).start();
    }

    protected void sendJSON(final String request, final String json) {
        sendJSON(request, json, null);
    }

    protected void sendJSON(final String request, final String json, final String cookie) {
        Log.v(TAG, "Json: " + json);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                OutputStream os = null;
                try {
                    URL url = getURL(request);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    if (cookie != null)
                        conn.setRequestProperty("Cookie", cookie);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("PUT");
                    if (auth != null)
                        conn.setRequestProperty("Authorization", "Basic " + auth);
                    else if (bearer != null)
                        conn.setRequestProperty("Authorization", "Bearer " + bearer);
                    os = conn.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    is = conn.getInputStream();
                } catch (SSLProtocolException e) { /**/ } catch (Exception e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                } finally {
                    if (os != null)
                        try {
                            os.close();
                        } catch (Exception e) { /**/ }
                    if (is != null)
                        try {
                            is.close();
                        } catch (Exception e) { /**/ }
                    if (conn != null)
                        try {
                            conn.disconnect();
                        } catch (Exception e) { /**/ }
                }
            }
        }).start();
    }
}