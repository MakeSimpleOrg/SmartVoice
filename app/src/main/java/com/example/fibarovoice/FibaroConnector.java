package com.diamond.SmartVoice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.gson.Gson;

import android.util.Base64;

public class FibaroConnector {
    private final static Logger LOGGER = Logger.getLogger(FibaroConnector.class.getName());
    private final String fibaroHost;
    private final String fibaroAuth;

    public FibaroConnector(String fibaroHost, String fibaroUser, String fibaroPassword) {
        LOGGER.setLevel(Level.WARNING);
        this.fibaroHost = fibaroHost;
        this.fibaroAuth = Base64.encodeToString((fibaroUser + ":" + fibaroPassword).getBytes(), Base64.DEFAULT);
    }

    private boolean matches(String s1, String s2) {
        if (s1.equalsIgnoreCase(s2))
            return true;
        String[] split1 = s1.split(" ");
        String[] split2 = s2.split(" ");
        if (split1.length != split2.length || split1.length == 0)
            return false;
        //System.out.println("var: " + split1[0] + " " + split1[1] + ", " + split2[0] + " " + split2[1] + ", orig: " + s1 + ", " + s2);
        String c1, c2;
        for(int i = 0; i < split1.length; i++)
        {
            c1 = split1[i].substring(0, Math.min(4, split1[i].length()));
            c2 = split2[i].substring(0, Math.min(4, split2[i].length()));
            if(!c1.equalsIgnoreCase(c2))
                return false;
        }
        System.out.println("matches: " + split1 + ", orig: " + s1 + ", " + s2);
        return true;
    }

    public Device[] getDevices(String str) {
        System.out.println("getDevices: " + str);
        int count = 0;
        for (Device d : getDevices())
            if (d.name.equalsIgnoreCase(str))
                count++;
        Device[] devices = new Device[count];
        int i = 0;
        for (Device d : getDevices())
            if (d.name.equalsIgnoreCase(str)) {
                System.out.println("device id: " + d.id + ", name: " + d.name + ", input name: " + str);
                devices[i] = d;
                i++;
            }
        return devices;
    }

    public Device[] getDevices(String[] strs) {
        System.out.println("getDevices: " + Arrays.toString(strs));
        int count = 0;
        String str2;
        Device[] devices = getDevices();
        d1:
        for (Device d : devices)
            for (String str : strs) {
                if (matches(d.name, str)) {
                    count++;
                    continue d1;
                }
                else if(matchesOnOff(d.name, str))
                {
                    count++;
                    continue d1;
                }
            }
        Device[] result = new Device[count];
        int i = 0;
        String name2[];
        d2:
        for (Device d : devices) {
            //System.out.println("device id: " + d.id + ", type: " + d.type + ", name: " + d.name);
            for (String str : strs)
                if (matches(d.name, str)) {
                    //System.out.println("device id: " + d.id + ", name: " + d.name + ", input name: " + str);
                    result[i++] = d;
                    continue d2;
                }
                else if(matchesOnOff(d.name, str))
                {
                    result[i++] = d;
                    name2 = d.name.split(" ");
                    if(str.contains("включи"))
                        d.name = name2[0] + " включить " + name2[1] + (name2.length > 2 ? (" " + name2[2]) : "");
                    else if(str.contains("выключи"))
                        d.name = name2[0] + " выключить " + name2[1] + (name2.length > 2 ? (" " + name2[2]) : "");
                    continue d2;
                }
        }
        return result;
    }

    private boolean matchesOnOff(String name, String str)
    {
        String[] s =  str.split(" ");
        if(s.length > 2 && name.split(" ").length > 1)
        {
            String str2 = s[0] + " " + s[2] + (s.length > 3 ? (" " + s[3]) : "");
            if(str.contains("включи") && matches(name, str2))
                return true;
            if(str.contains("выключи") && matches(name, str2))
                return true;
        }
        return false;
    }

    public Scene[] getScenes(String[] strs) {
        System.out.println("getScenes: " + Arrays.toString(strs));
        int count = 0;
        Scene[] scenes = getAllScenes();
        d1: for(Scene s : scenes)
            for (String str : strs)
                if(s.liliStartCommand != null && !s.liliStartCommand.isEmpty())
                    if (matches(s.liliStartCommand, str)) {
                        count++;
                        continue d1;
                    }
        Scene[] result = new Scene[count];
        int i = 0;
        d2: for(Scene s : scenes)
            for (String str : strs)
                if(s.liliStartCommand != null && !s.liliStartCommand.isEmpty())
                    if (matches(s.liliStartCommand, str)) {
                        result[i++] = s;
                        continue d2;
                    }
        return result;
    }

    public String process(Device[] devices) {
        boolean enabled = false;
        boolean finded = false;

        for (Device d : devices) {
            //System.out.println("process id: " + d.id + ", type: " + d.type + ", name: " + d.name + ", value: " + d.properties.value);
            if(d.type.contains("temperatureSensor"))
            {
                return "" + (int) Double.parseDouble(d.properties.value);
            }
            else if (d.type.contains("RGB") || d.type.contains("Switch")) {
                if (d.name.contains("включить")) {
                    turnDeviceOn(d);
                    finded = true;
                    enabled = true;
                }
                else if (d.name.contains("выключить")) {
                    turnDeviceOff(d);
                    finded = true;
                    enabled = false;
                }
                else if (finded)
                    if (enabled)
                        turnDeviceOn(d);
                    else
                        turnDeviceOff(d);
                else if (d.properties.value.equals("false") || d.properties.value.equals("0")) {
                    turnDeviceOn(d);
                    finded = true;
                    enabled = true;
                } else {
                    turnDeviceOff(d);
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

    public String process(Scene[] scenes) {
        for (Scene s : scenes) {
            runScene(s);
        }
        return "Выполняю";
    }

    public boolean turnDeviceOff(Device d) {
        System.out.println("turnDeviceOff id: " + d.id + ", type: " + d.type + ", name: " + d.name + ", value: " + d.properties.value);
        return sendCommand("callAction?deviceID=" + d.id + "&name=turnOff");
    }

    public boolean turnDeviceOn(Device d) {
        System.out.println("turnDeviceOn id: " + d.id + ", type: " + d.type + ", name: " + d.name + ", value: " + d.properties.value);
        return sendCommand("callAction?deviceID=" + d.id + "&name=turnOn");
    }

    public boolean runScene(Scene s) {
        System.out.println("runScene id: " + s.id + ", type: " + s.type + ", name: " + s.name);
        return sendCommand("sceneControl?id=" + s.id + "&action=start");
    }

    public boolean stopScene(Scene s) {
        System.out.println("runScene id: " + s.id + ", type: " + s.type + ", name: " + s.name);
        return sendCommand("sceneControl?id=" + s.id + "&action=stop");
    }

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

    public Device[] getDevices() {
        Room[] rooms = getRooms();
        roomCount = rooms.length;
        Device[] all_devices = getAllDevices();
        int count = 0;
        for (Device d : all_devices) {
            if (d.roomID > 0 && "true".equals(d.properties.saveLogs))// && d.roomID != 41 && d.roomID != 42 && d.roomID != 85)
                count++;
        }
        Device[] devices = new Device[count];
        int i = 0;
        String name;
        for (Device d : all_devices)
            if (d.roomID > 0 && "true".equals(d.properties.saveLogs))// && d.roomID != 41 && d.roomID != 42 && d.roomID != 85)
            {
                if (d.properties.userDescription != null && !d.properties.userDescription.isEmpty())
                    d.name = d.properties.userDescription;
                for (Room room : rooms)
                    if (d.roomID == room.id) {
                        name = d.name.trim();
                        d.name = room.name.trim() + " " + name;
                        //d.name2 = room.name.trim() + " включить " + name;
                        //d.name3 = room.name.trim() + " выключить " + name;
                        break;
                    }
                d.name = replaceDigits(d.name);
                //d.name2 = replaceDigits(d.name2);
                //d.name3 = replaceDigits(d.name3);
                devices[i++] = d;
            }
        deviceCount = count;
        System.out.println("Founded: " + deviceCount + " devices");
        return devices;
    }

    public Scene[] getScenes() {
        int count = 0;
        Scene[] scenes = getAllScenes();
        for(Scene s : scenes)
            if(s.liliStartCommand != null && !s.liliStartCommand.isEmpty())
                count++;
        Scene[] result = new Scene[count];
        int i = 0;
        for(Scene s : scenes)
            if(s.liliStartCommand != null && !s.liliStartCommand.isEmpty())
                result[i++] = s;
        sceneCount = count;
        System.out.println("Founded: " + sceneCount + " scenes");
        return result;
    }

    private String replaceDigits(String name)
    {
        name = name.replaceAll("1", "");
        name = name.replaceAll("2", "");
        name = name.replaceAll("3", "");
        name = name.replaceAll("4", "");
        name = name.replaceAll("5", "");
        name = name.replaceAll("6", "");
        name = name.replaceAll("7", "");
        name = name.replaceAll("8", "");
        name = name.replaceAll("9", "");
        name = name.replaceAll("0", "");
        name = name.trim();
        return name;
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
        int responseCode = -1;
        String response = null;
        try {
            LOGGER.info("Sending command: " + fibaroURL);
            url = new URL(fibaroURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + fibaroAuth);
            response = connection.getResponseMessage();
            LOGGER.info("Received response: " + response);
        } catch (Exception e) {
            LOGGER.severe("Error while sending command: " + fibaroURL);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private synchronized JSONArray getJson(String fibaroURL) {
        String result = request(fibaroURL);
        if (result == null)
            return null;
        JSONArray dataJsonObj = null;
        try {
            dataJsonObj = new JSONArray(result);
        } catch (JSONException e) {
            LOGGER.severe("Error while parse Json: " + result);
            e.printStackTrace();
        }
        return dataJsonObj;
    }

    private synchronized String request(String fibaroURL) {
        fibaroURL = "http://" + fibaroHost + "/api/" + fibaroURL;
        URL url;
        HttpURLConnection connection;
        String result = null;
        int responseCode = -1;
        String response = null;
        try {
            LOGGER.info("Sending command: " + fibaroURL);
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
            LOGGER.severe("Error while get getJson: " + fibaroURL);
            e.printStackTrace();
            return null;
        }
        return result;
    }
}