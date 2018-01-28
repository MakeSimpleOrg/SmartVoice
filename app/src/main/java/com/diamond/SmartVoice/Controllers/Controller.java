package com.diamond.SmartVoice.Controllers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.diamond.SmartVoice.AI;
import com.google.gson.Gson;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class Controller {
    private static final String TAG = Controller.class.getSimpleName();

    protected Gson gson;
    protected String host;
    protected String auth;
    protected String bearer;
    protected boolean clearNames;

    protected String request(String request) throws IOException {
        String result;
        URL url = new URL("http://" + host + request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (auth != null)
            conn.setRequestProperty("Authorization", "Basic " + auth);
        else if (bearer != null)
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setConnectTimeout(5000);
        conn.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            buffer.append(line).append("\n");
        br.close();
        result = buffer.toString();
        conn.disconnect();
        return result;
    }

    protected void sendCommand(String request) {
        Log.d(TAG, "Sending command: " + request);
        try {
            URL url = new URL("http://" + host + request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (auth != null)
                conn.setRequestProperty("Authorization", "Basic " + auth);
            else if (bearer != null)
                conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setConnectTimeout(5000);
            conn.getResponseMessage();
        } catch (IOException e) {
            Log.w(TAG, "Error while get getJson: " + request);
            e.printStackTrace();
        }
    }

    protected void sendJSON(String request, String json) {
        try {
            URL url = new URL("http://" + host + request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
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
        } catch (IOException e) {
            Log.w(TAG, "Error while get getJson: " + request);
            e.printStackTrace();
        }
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
        String text = "Ошибка";
        ArrayList<UDevice> list = new ArrayList<>();
        for (UDevice u : devices) {
            Log.w(TAG, "найдено: " + u.ai_name);
            if (u.getCapabilities() != null) {
                String onoff = u.getCapabilities().get(Capability.onoff);
                String openclose = u.getCapabilities().get(Capability.openclose);
                if (onoff != null || openclose != null) {
                    list.add(u);
                    if(!finded) {
                        finded = true;
                        if (u.ai_name.contains("включить"))
                            enabled = true;
                        else if (u.ai_name.contains("выключить"))
                            enabled = false;
                         else if ("0".equals(onoff) || "0".equals(openclose))
                            enabled = true;
                         else
                            enabled = false;
                    }
                } else {
                    String measure_temperature = u.getCapabilities().get(Capability.measure_temperature);
                    if (measure_temperature != null)
                        return measure_temperature;
                    String measure_humidity = u.getCapabilities().get(Capability.openclose);
                    if (measure_humidity != null)
                        return measure_humidity;
                    String measure_light = u.getCapabilities().get(Capability.openclose);
                    if (measure_light != null)
                        return measure_light;
                    String measure_co2 = u.getCapabilities().get(Capability.openclose);
                    if (measure_co2 != null)
                        return measure_co2;
                    String measure_pressure = u.getCapabilities().get(Capability.openclose);
                    if (measure_pressure != null)
                        return measure_pressure;
                    String measure_noise = u.getCapabilities().get(Capability.openclose);
                    if (measure_noise != null)
                        return measure_noise;
                }
            }
        }

        for (UDevice u : list) {
            String onoff = u.getCapabilities().get(Capability.onoff);
            String openclose = u.getCapabilities().get(Capability.openclose);
            if (enabled) {
                turnDeviceOn(u);
                if (onoff != null) {
                    u.getCapabilities().put(Capability.onoff, "1");
                    text = "Включаю";
                }
                if (openclose != null) {
                    u.getCapabilities().put(Capability.openclose, "1");
                    text = "Закрываю";
                }
            } else {
                turnDeviceOff(u);
                if (onoff != null) {
                    u.getCapabilities().put(Capability.onoff, "0");
                    text = "Выключаю";
                }
                if (openclose != null) {
                    u.getCapabilities().put(Capability.openclose, "0");
                    text = "Открываю";
                }
            }
        }
        return text;
    }

    private String processScenes(UScene[] scenes) {
        for (UScene s : scenes)
            runScene(s);
        return "Выполняю";
    }
}