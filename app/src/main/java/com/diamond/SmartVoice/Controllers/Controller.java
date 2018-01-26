package com.diamond.SmartVoice.Controllers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
    protected boolean clearNames;

    protected String request(String request) throws IOException {
        String result;
        URL url = new URL("http://" + host + request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (auth != null)
            connection.setRequestProperty("Authorization", "Basic " + auth);
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        connection.setConnectTimeout(5000);
        connection.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            buffer.append(line).append("\n");
        br.close();
        result = buffer.toString();
        return result;
    }

    protected boolean sendCommand(String request) {
        Log.d(TAG, "Sending command: " + request);
        try {
            URL url = new URL("http://" + host + request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (auth != null)
                connection.setRequestProperty("Authorization", "Basic " + auth);
            connection.setConnectTimeout(5000);
            connection.getResponseMessage();
        } catch (IOException e) {
            Log.w(TAG, "Error while get getJson: " + request);
            e.printStackTrace();
            return false;
        }
        return true;
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
        for (UDevice u : devices) {
            Log.w(TAG, "найдено: " + u.ai_name + ", " + u.uType);
            switch (u.uType) {
                case Value:
                    return u.getValue();
                case Humidity:
                    return u.getHumidity();
                case Light:
                    return u.getLight();
                case Temperature:
                    return u.getTemperature();
                case OnOff:
                case OpenClose:
                    if (u.ai_name.contains("включить")) {
                        turnDeviceOn(u);
                        finded = true;
                        enabled = true;
                        text = u.uType == UType.OnOff ? "Включаю" : "Закрываю";
                    } else if (u.ai_name.contains("выключить")) {
                        turnDeviceOff(u);
                        finded = true;
                        enabled = false;
                        text = u.uType == UType.OnOff ? "Выключаю" : "Открываю";
                    } else if (finded)
                        if (enabled)
                            turnDeviceOn(u);
                        else
                            turnDeviceOff(u);
                    else if (u.getValue().equals("false") || u.getValue().equals("0")) {
                        turnDeviceOn(u);
                        finded = true;
                        enabled = true;
                        text = u.uType == UType.OnOff ? "Включаю" : "Закрываю";
                    } else {
                        turnDeviceOff(u);
                        finded = true;
                        enabled = false;
                        text = u.uType == UType.OnOff ? "Выключаю" : "Открываю";
                    }
                    break;
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