package com.diamond.SmartVoice.Fibaro;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.UDevice;
import com.diamond.SmartVoice.UScene;
import com.google.gson.Gson;

import android.util.Base64;
import android.util.Log;

public class FibaroConnector {
    private static final String TAG = FibaroConnector.class.getSimpleName();

    private final String fibaroHost;
    private final String fibaroAuth;

    int roomCount = 0;
    int deviceCount = 0;
    int sceneCount = 0;

    public int getLastRoomsCount() {
        return roomCount;
    }

    public int getLastDevicesCount() {
        return deviceCount;
    }

    public int getLastScenesCount() {
        return sceneCount;
    }

    public FibaroConnector(String fibaroHost, String fibaroUser, String fibaroPassword) {
        this.fibaroHost = fibaroHost;
        this.fibaroAuth = Base64.encodeToString((fibaroUser + ":" + fibaroPassword).getBytes(), Base64.DEFAULT);
    }

    public synchronized Device[] getAllDevices() {
        String result = request("devices?enabled=true&visible=true");
        if (result == null)
            return new Device[0];
        return new Gson().fromJson(result, Device[].class);
    }

    public synchronized Scene[] getAllScenes() {
        String result = request("scenes?enabled=true&visible=true");
        if (result == null)
            return new Scene[0];
        return new Gson().fromJson(result, Scene[].class);
    }

    public synchronized Room[] getRooms() {
        String result = request("rooms");
        if (result == null)
            return new Room[0];
        return new Gson().fromJson(result, Room[].class);
    }

    private synchronized boolean sendCommand(String fibaroURL) {
        fibaroURL = "http://" + fibaroHost + "/api/" + fibaroURL;
        URL url;
        HttpURLConnection connection;
        String response = null;
        try {
            Log.d(TAG, "Sending command: " + fibaroURL);
            url = new URL(fibaroURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + fibaroAuth);
            response = connection.getResponseMessage();
            Log.d(TAG, "Received response: " + response);
        } catch (Exception e) {
            Log.w(TAG, "Error while sending command: " + fibaroURL);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private synchronized String request(String fibaroURL) {
        fibaroURL = "http://" + fibaroHost + "/api/" + fibaroURL;
        URL url;
        HttpURLConnection connection;
        String result;
        try {
            Log.d(TAG, "Sending command: " + fibaroURL);
            url = new URL(fibaroURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + fibaroAuth);
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            br.close();
            result = buffer.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error while get getJson: " + fibaroURL);
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public boolean turnDeviceOn(int id) {
        return sendCommand("callAction?deviceID=" + id + "&name=turnOn");
    }

    public boolean turnDeviceOff(int id) {
        return sendCommand("callAction?deviceID=" + id + "&name=turnOff");
    }

    public boolean runScene(int id) {
        return sendCommand("sceneControl?id=" + id + "&action=start");
    }

    public Device[] getDevices() {
        Room[] rooms = getRooms();
        roomCount = rooms.length;
        Device[] all_devices = getAllDevices();
        int count = 0;
        for (Device d : all_devices) {
            if (d.roomID > 0 && "true".equals(d.properties.saveLogs))
                count++;
        }
        Device[] devices = new Device[count];
        int i = 0;
        String name;
        for (Device d : all_devices)
            if (d.roomID > 0 && "true".equals(d.properties.saveLogs)) {
                d.ai_name = d.name.toLowerCase(Locale.getDefault()).trim();
                if (d.properties.userDescription != null && !d.properties.userDescription.isEmpty())
                    d.ai_name = d.properties.userDescription.toLowerCase(Locale.getDefault()).trim();
                for (Room room : rooms)
                    if (d.roomID == room.id) {
                        name = d.ai_name;
                        d.ai_name = room.name.trim() + " " + name;
                        break;
                    }
                d.ai_name = AI.replaceTrash(d.ai_name);
                devices[i++] = d;
            }
        deviceCount = count;
        return devices;
    }

    public Scene[] getScenes() {
        int count = 0;
        Scene[] scenes = getAllScenes();
        for (Scene s : scenes)
            if (s.liliStartCommand != null && !s.liliStartCommand.isEmpty())
                count++;
        Scene[] result = new Scene[count];
        int i = 0;
        for (Scene s : scenes)
            if (s.liliStartCommand != null && !s.liliStartCommand.isEmpty()) {
                s.ai_name = s.liliStartCommand.toLowerCase(Locale.getDefault()).trim();
                result[i++] = s;
            }
        sceneCount = count;
        return result;
    }

    public String process(String[] requests) {
        UDevice[] devices = AI.getDevices(getDevices(), requests);
        if (devices != null)
            return processDevices(devices);
        UScene[] scenes = AI.getScenes(getScenes(), requests);
        if (scenes != null)
            return processScenes(scenes);
        return null;
    }

    public String processDevices(UDevice[] devices) {
        boolean enabled = false;
        boolean finded = false;
        Device d;
        for (UDevice u : devices) {
            d = (Device) u;
            if (d.type.contains("temperatureSensor"))
                return "" + (int) Double.parseDouble(d.properties.value);
            else if (d.type.contains("RGB") || d.type.contains("Switch")) {
                if (d.ai_name.contains("включить")) {
                    turnDeviceOn(d.id);
                    finded = true;
                    enabled = true;
                } else if (d.ai_name.contains("выключить")) {
                    turnDeviceOff(d.id);
                    finded = true;
                    enabled = false;
                } else if (finded)
                    if (enabled)
                        turnDeviceOn(d.id);
                    else
                        turnDeviceOff(d.id);
                else if (d.properties.value.equals("false") || d.properties.value.equals("0")) {
                    turnDeviceOn(d.id);
                    finded = true;
                    enabled = true;
                } else {
                    turnDeviceOff(d.id);
                    finded = true;
                    enabled = false;
                }
            }
        }
        if (finded)
            if (enabled)
                return "Включаю";
            else
                return "Выключаю";
        return "Ошибка";
    }

    public String processScenes(UScene[] scenes) {
        for (UScene s : scenes)
            runScene(((Scene) s).id);
        return "Выполняю";
    }
}