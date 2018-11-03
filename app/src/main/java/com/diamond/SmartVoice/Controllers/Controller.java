package com.diamond.SmartVoice.Controllers;

import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class Controller {
    private static final String TAG = Controller.class.getSimpleName();

    protected MainActivity mainActivity;

    private static ObjectMapper mapper = new ObjectMapper().registerModule(new JsonOrgModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    protected Gson gson;
    protected String host;
    protected String host_ext;
    protected String auth;
    protected String bearer;
    protected boolean clearNames;

    protected String request(String request, String cookie) {
        String result = null;
        HttpURLConnection conn = null;
        try {
            URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
            Log.d(TAG, "Sending request: " + url);
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
            result = new String(ByteStreams.toByteArray(conn.getInputStream()));
            conn.disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Error while send request: " + request);
        } finally {
            if (conn != null)
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    Log.w(TAG, "Cannot close connection: " + e);
                }
        }
        return result;
    }

    protected <T> T getJson(String request, String cookie, Class<T> c) {
        T result = null;
        HttpURLConnection conn = null;
        try {
            URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
            Log.d(TAG, "Sending request: " + url);
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
            result = mapper.readValue(conn.getInputStream(), c);
            conn.disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Error while send request: " + request, e);
        } finally {
            if (conn != null)
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    Log.w(TAG, "Cannot close connection: " + e);
                }
        }
        return result;
    }

    protected void sendCommand(final String request) {
        sendCommand(request, null);
    }

    protected void sendCommand(final String request, final String cookie) {
        Log.d(TAG, "Command: " + request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (auth != null)
                        conn.setRequestProperty("Authorization", "Basic " + auth);
                    else if (bearer != null)
                        conn.setRequestProperty("Authorization", "Bearer " + bearer);
                    if (cookie != null)
                        conn.setRequestProperty("Cookie", cookie);
                    conn.setConnectTimeout(10000);
                    conn.getResponseMessage();
                } catch (Exception e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                } finally {
                    if (conn != null)
                        try {
                            conn.disconnect();
                        } catch (Exception e) {
                            Log.w(TAG, "Cannot close connection: " + e);
                        }
                }
            }
        }).start();
    }

    protected void sendJSON(final String request, final String json) {
        sendJSON(request, json, null);
    }

    protected void sendJSON(final String request, final String json, final String cookie) {
        Log.d(TAG, "Json: " + json);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
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
                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes("UTF-8"));
                    os.close();

                    String result = new String(ByteStreams.toByteArray(conn.getInputStream()));

                    /*
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder buffer = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        buffer.append(line).append("\n");
                    br.close();
                    */

                    Log.d(TAG, "Result: " + result);

                    conn.disconnect();
                } catch (Exception e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                } finally {
                    if (conn != null)
                        try {
                            conn.disconnect();
                        } catch (Exception e) {
                            Log.w(TAG, "Cannot close connection: " + e);
                        }
                }
            }
        }).start();
    }

    public abstract URoom[] getRooms();

    public abstract UDevice[] getDevices();

    public abstract UScene[] getScenes();

    public UDevice getDevice(String deviceId) {
        for (UDevice d : getDevices())
            if (deviceId.equals(d.getId()))
                return d;
        return null;
    }

    public UScene getScene(String sceneId) {
        for (UScene s : getScenes())
            if (sceneId.equals(s.getId()))
                return s;
        return null;
    }

    public int getVisibleRoomsCount() {
        int c = 0;
        if (getRooms() != null)
            for (URoom u : getRooms())
                if (u.isVisible())
                    c++;
        return c;
    }

    public int getVisibleDevicesCount() {
        int c = 0;
        if (getDevices() != null)
            for (UDevice u : getDevices()) {
                if (u.isVisible())
                    c++;
            }
        return c;
    }

    public int getVisibleScenesCount() {
        int c = 0;
        if (getScenes() != null)
            for (UScene u : getScenes())
                if (u.isVisible())
                    c++;
        return c;
    }

    public abstract void turnDeviceOn(UDevice d);

    public abstract void turnDeviceOff(UDevice d);

    public abstract void closeLock(UDevice d);

    public abstract void openLock(UDevice d);

    public abstract void closeWindow(UDevice d);

    public abstract void openWindow(UDevice d);

    public abstract void setDimLevel(UDevice d, String level);

    public abstract void setColor(UDevice d, int r, int g, int b, int w);

    public abstract void setMode(UDevice d, String mode);

    public abstract void runScene(UScene s);

    public String process(String[] requests, SharedPreferences pref) {
        UDevice[] devices = AI.getDevices(getDevices(), requests, pref);
        if (devices != null) {
            mainActivity.show(mainActivity.getString(R.string.Recognized) + ": " + devices[0].ai_name);
            return processDevices(devices);
        }
        UScene[] scenes = AI.getScenes(getScenes(), requests, pref);
        if (scenes != null) {
            mainActivity.show(mainActivity.getString(R.string.Recognized) + ": " + scenes[0].ai_name);
            return processScenes(scenes);
        }
        return null;
    }

    private String processDevices(UDevice[] devices) {
        boolean enabled = false;
        boolean finded = false;
        String text = mainActivity.getString(R.string.error);
        ArrayList<UDevice> list = new ArrayList<>();
        for (UDevice u : devices) {
            Log.d(TAG, mainActivity.getString(R.string.Recognized) + ": " + u.getId() + " " + u.ai_name);
            if (u.getCapabilities() != null) {
                String onoff = u.getCapabilities().get(Capability.onoff);
                String openclose = u.getCapabilities().get(Capability.openclose);
                String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
                if (u.ai_flag == 3) {
                    setDimLevel(u, u.ai_value);
                    u.getCapabilities().put(Capability.dim, u.ai_value);
                    text = u.ai_value + " " + mainActivity.getString(R.string.percent);
                } else if (onoff != null || openclose != null || windowcoverings_state != null) {
                    list.add(u);
                    if (!finded) {
                        finded = true;
                        enabled = u.ai_flag == 1 || u.ai_flag == 0 && ("0".equals(onoff) || "open".equals(openclose) || "up".equals(windowcoverings_state));
                    }
                } else {
                    String measure_temperature = u.getCapabilities().get(Capability.measure_temperature);
                    if (measure_temperature != null)
                        return measure_temperature;
                    String measure_humidity = u.getCapabilities().get(Capability.measure_humidity);
                    if (measure_humidity != null)
                        return measure_humidity;
                    String measure_light = u.getCapabilities().get(Capability.measure_light);
                    if (measure_light != null)
                        return measure_light;
                    String measure_co2 = u.getCapabilities().get(Capability.measure_co2);
                    if (measure_co2 != null)
                        return measure_co2;
                    String measure_pressure = u.getCapabilities().get(Capability.measure_pressure);
                    if (measure_pressure != null)
                        return measure_pressure;
                    String measure_noise = u.getCapabilities().get(Capability.measure_noise);
                    if (measure_noise != null)
                        return measure_noise;
                }
            } else
                Log.w(TAG, "u.getCapabilities() == null: " + u.getId() + u.ai_name);
        }

        for (UDevice u : list) {
            String onoff = u.getCapabilities().get(Capability.onoff);
            String openclose = u.getCapabilities().get(Capability.openclose);
            String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
            String dim = u.getCapabilities().get(Capability.dim);
            if (enabled) {
                if (onoff != null) {
                    turnDeviceOn(u);
                    u.getCapabilities().put(Capability.onoff, "1");
                    text = mainActivity.getString(R.string.switched_on);
                }
                if (openclose != null) {
                    closeLock(u);
                    u.getCapabilities().put(Capability.openclose, "close");
                    text = mainActivity.getString(R.string.closed);
                }
                if (windowcoverings_state != null) {
                    closeWindow(u);
                    u.getCapabilities().put(Capability.windowcoverings_state, "down");
                    text = mainActivity.getString(R.string.closed);
                }
                if (dim != null) {
                    setDimLevel(u, "100");
                    u.getCapabilities().put(Capability.dim, "100");
                }
            } else {
                if (onoff != null) {
                    turnDeviceOff(u);
                    u.getCapabilities().put(Capability.onoff, "0");
                    text = mainActivity.getString(R.string.switched_off);
                }
                if (openclose != null) {
                    openLock(u);
                    u.getCapabilities().put(Capability.openclose, "open");
                    text = mainActivity.getString(R.string.open);
                }
                if (windowcoverings_state != null) {
                    openWindow(u);
                    u.getCapabilities().put(Capability.windowcoverings_state, "up");
                    text = mainActivity.getString(R.string.open);
                }
            }
        }
        return text;
    }

    private String processScenes(UScene[] scenes) {
        for (UScene s : scenes)
            runScene(s);
        return mainActivity.getString(R.string.scene_launched);
    }
}