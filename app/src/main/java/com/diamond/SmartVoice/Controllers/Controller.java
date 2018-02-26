package com.diamond.SmartVoice.Controllers;

import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;
import com.google.gson.Gson;
import com.rollbar.android.Rollbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class Controller {
    private static final String TAG = Controller.class.getSimpleName();

    protected MainActivity mainActivity;

    protected Gson gson;
    protected String host;
    protected String host_ext;
    protected String auth;
    protected String bearer;
    protected boolean clearNames;

    protected String request(String request, String cookie) {
        String result = null;
        try {
            URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
            Log.d(TAG, "Sending request: " + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                buffer.append(line).append("\n");
            br.close();
            result = buffer.toString();
            conn.disconnect();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (NoRouteToHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.w(TAG, "Error while send request: " + request);
            e.printStackTrace();
            Rollbar.instance().error(e);
        }
        return result;
    }

    protected void sendCommand(final String request) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (auth != null)
                        conn.setRequestProperty("Authorization", "Basic " + auth);
                    else if (bearer != null)
                        conn.setRequestProperty("Authorization", "Bearer " + bearer);
                    conn.setConnectTimeout(10000);
                    conn.getResponseMessage();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (NoRouteToHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                    e.printStackTrace();
                    Rollbar.instance().error(e);
                }
            }
        }).start();
    }

    protected void sendJSON(final String request, final String json) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = host_ext != null ? new URL("https://" + host_ext + request) : new URL("http://" + host + request);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
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

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder buffer = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        buffer.append(line).append("\n");
                    br.close();

                    Log.w(TAG, "Result: " + buffer.toString());

                    conn.disconnect();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (NoRouteToHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.w(TAG, "Error while get getJson: " + request);
                    e.printStackTrace();
                    Rollbar.instance().error(e);
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
            for (UDevice u : getDevices())
                if (u.isVisible())
                    c++;
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

    public String process(String[] requests) {
        UDevice[] devices = AI.getDevices(getDevices(), requests);
        if (devices != null)
            return processDevices(devices);
        UScene[] scenes = AI.getScenes(getScenes(), requests);
        if (scenes != null)
            return processScenes(scenes);
        return null;
    }

    private String processDevices(UDevice[] devices) {
        boolean enabled = false;
        boolean finded = false;
        String text = mainActivity.getString(R.string.error);
        ArrayList<UDevice> list = new ArrayList<>();
        for (UDevice u : devices) {
            Log.w(TAG, "найдено: " + u.ai_name);
            if (u.getCapabilities() != null) {
                String onoff = u.getCapabilities().get(Capability.onoff);
                String openclose = u.getCapabilities().get(Capability.openclose);
                String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
                if (onoff != null || openclose != null || windowcoverings_state != null) {
                    list.add(u);
                    if (!finded) {
                        finded = true;
                        enabled = u.ai_flag == 1 || u.ai_flag != 2 && ("0".equals(onoff) || "open".equals(openclose) || "up".equals(windowcoverings_state));
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
            }
        }

        for (UDevice u : list) {
            String onoff = u.getCapabilities().get(Capability.onoff);
            String openclose = u.getCapabilities().get(Capability.openclose);
            String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
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