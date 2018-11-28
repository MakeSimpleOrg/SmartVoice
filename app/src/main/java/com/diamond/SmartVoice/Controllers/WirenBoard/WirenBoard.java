package com.diamond.SmartVoice.Controllers.WirenBoard;

import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.MQTTController;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.MainActivity;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Dmitriy Ponomarev
 */
public class WirenBoard extends MQTTController {
    private static final String TAG = WirenBoard.class.getSimpleName();

    private HashMap<String, Room> all_rooms = new HashMap<String, Room>();
    private HashMap<String, Device> all_devices = new HashMap<String, Device>();
    private ArrayList<UScene> all_scenes;

    private int loading;

    public WirenBoard(MainActivity activity) {
        super();
        name = "WirenBoard";
        mainActivity = activity;
        clearNames = true; // TODO config
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (!key.equals("wirenboard_enabled") || !pref.getBoolean(key, false))
            return;
        if ((!pref.getString("wirenboard_server_ip", "").isEmpty() || !pref.getString("wirenboard_server_ip_ext", "").isEmpty())) {
            loadData();
        }
    }

    @Override
    public void loadData() {
        loading = 2;

        host = mainActivity.pref.getString("wirenboard_server_ip", "");
        host_ext = mainActivity.pref.getString("wirenboard_server_ip_ext", "");

        Log.w(TAG, "Load data");

        super.loadData();

        Room r = new Room();
        r.setId("noroom");
        r.setName("No room");
        all_rooms.put(r.getId(), r);

        int i = 0;
        while (loading > 0) {
            try {
                Thread.sleep(10);
                if (i++ > 1000)
                    return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Device d : all_devices.values()) {
            switch (d.getTemplate()) {
                case "binary_sensor":
                    d.addCapability(Capability.measure_generic, "");
                    subscribe(d.getTopics().get(0));
                    break;
                case "sensor":
                    d.addCapability(Capability.measure_generic, "");
                    subscribe(d.getTopics().get(0));
                    break;
                case "light":
                    d.addCapability(Capability.onoff, "");
                    subscribe(d.getTopics().get(0));
                    break;
                case "switch":
                    d.addCapability(Capability.onoff, "");
                    subscribe(d.getTopics().get(0));
                    break;
                case "temperature":
                    d.addCapability(Capability.measure_temperature, "");
                    subscribe(d.getTopics().get(0));
                    break;
                case "alarm":
                    d.addCapability(Capability.onoff, "");
                    d.addCapability(Capability.alarm_contact, "");
                    subscribe(d.getTopics().get(0));
                    break;
            }
        }
    }

    @Override
    public void onSuccess(IMqttToken iMqttToken) {
        if (isConnecting) {
            subscribe("/config/rooms/+/uid");
            subscribe("/config/widgets/+/uid");
            subscribe("/devices/+/controls/+/meta/type");

            subscribe("/tmp/<random>");
            publish("/tmp/<random>", "loaded" + loading);
        }
        super.onSuccess(iMqttToken);
    }

    public void messageArrived(String topic, MqttMessage message) {
        try {
            byte[] payload = message.getPayload();
            String msg = new String(payload, Charset.defaultCharset());

            if (loading > 0 && topic.equals("/tmp/<random>") && msg.equals("loaded" + loading)) {
                Log.w(TAG, "Loaded: " + loading);
                loading--;
                if(loading > 0)
                    publish("/tmp/<random>", "loaded" + loading);
                return;
            }

            if (!topic.startsWith("/devices/"))
                Log.w(TAG, "Topic: " + topic + ", msg: " + msg);


            if (topic.contains("/config/rooms/")) {
                if (topic.endsWith("/uid")) {
                    Room r = all_rooms.get(msg);
                    if (r == null)
                        r = new Room();
                    r.setId(msg);
                    all_rooms.put(msg, r);
                    subscribe("/config/rooms/" + msg + "/name");
                } else {
                    String[] list = topic.split("/");
                    String id = list[3];
                    Room r = all_rooms.get(id);
                    if (r == null)
                        Log.w(TAG, "Not found room id: " + id);
                    else if (topic.endsWith("/name"))
                        r.setName(msg);
                }
            } else if (topic.contains("/config/widgets/")) {
                if (topic.endsWith("/uid")) {
                    //Log.w(TAG, "uid: " + msg);
                    Device d = all_devices.get(msg);
                    if (d == null)
                        d = new Device(this);
                    d.setId(msg);
                    all_devices.put(msg, d);
                    subscribe("/config/widgets/" + msg + "/name");
                    subscribe("/config/widgets/" + msg + "/room");
                    subscribe("/config/widgets/" + msg + "/template");
                    subscribe("/config/widgets/" + msg + "/controls/+/topic");
                } else {
                    String[] list = topic.split("/");
                    String id = list[3];
                    Device d = all_devices.get(id);
                    if (d == null)
                        Log.w(TAG, "Not found device id: " + id);
                    else {
                        if (topic.endsWith("/name")) {
                            d.setName(msg);
                            d.ai_name = msg;
                            //d.ai_name = AI.replaceTrash(d.ai_name);
                        } else if (topic.endsWith("/room"))
                            d.setRoomId(msg);
                        else if (topic.endsWith("/template"))
                            d.setTemplate(msg);
                        else if (topic.endsWith("/topic"))
                            d.addTopic(msg);
                    }
                }
            } else if (topic.startsWith("/devices/")) {
                if (topic.endsWith("/meta/type")) {
                    switch (msg) {
                        case "temperature":
                        case "switch":
                            String[] list = topic.split("/");
                            String id = list[2] + "/" + list[3] + "/" + list[4];
                            String name = list[4];
                            Device d = all_devices.get(id);
                            if (d == null)
                                d = new Device(this);
                            d.setId(id);
                            d.setName(name);
                            d.ai_name = name;
                            d.setTemplate(msg);
                            d.setRoomId("noroom");
                            d.addTopic("/" + list[1] + "/" + list[2] + "/" + list[3] + "/" + list[4]);
                            all_devices.put(id, d);
                            break;
                    }
                } else
                    for (Device d : all_devices.values())
                        if (d.getTopics().contains(topic))
                            switch (d.getTemplate()) {
                                case "binary_sensor":
                                    d.getCapabilities().put(Capability.measure_generic, msg);
                                    break;
                                case "sensor":
                                    d.getCapabilities().put(Capability.measure_generic, msg);
                                    break;
                                case "light":
                                    //Log.w(TAG, "Topic: " + topic + ", msg: " + msg);
                                    d.getCapabilities().put(Capability.onoff, msg);
                                    break;
                                case "switch":
                                    //Log.w(TAG, "Topic: " + topic + ", msg: " + msg);
                                    d.addCapability(Capability.onoff, msg);
                                    break;
                                case "temperature":
                                    //Log.w(TAG, "Topic: " + topic + ", msg: " + msg);
                                    d.addCapability(Capability.measure_temperature, msg);
                                    break;
                                case "alarm":
                                    //Log.w(TAG, "Topic: " + topic + ", msg: " + msg);
                                    d.getCapabilities().put(Capability.onoff, msg);
                                    d.getCapabilities().put(Capability.alarm_contact, msg);
                                    break;
                            }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateData() {
    }

    public Room getRoomById(String id) {
        return all_rooms.get(id);
    }

    @Override
    public URoom[] getRooms() {
        return all_rooms.values().toArray(new Room[0]);
    }

    @Override
    public UDevice[] getDevices() {
        return all_devices.values().toArray(new Device[0]);
    }

    @Override
    public UScene[] getScenes() {
        return all_scenes == null ? null : all_scenes.toArray(new UScene[0]);
    }

    @Override
    public void turnDeviceOn(UDevice d) {
        publish(((Device) d).getTopics().get(0), "1");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        publish(((Device) d).getTopics().get(0), "0");
    }

    @Override
    public void closeLock(UDevice d) {
        //TODO
    }

    @Override
    public void openLock(UDevice d) {
        //TODO
    }

    @Override
    public void closeWindow(UDevice d) {
        //TODO
    }

    @Override
    public void openWindow(UDevice d) {
        //TODO
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        //TODO
    }

    @Override
    public void setColor(UDevice d, int r, int g, int b, int w) {
        // TODO
    }

    @Override
    public void setMode(UDevice d, String mode) {
        //TODO
    }

    @Override
    public void setTargetTemperature(UDevice d, String level) {
        //TODO
    }

    @Override
    public void runScene(UScene s) {
        //TODO
    }

    /* TODO
    private String getThermostatMode(String mode) {
        switch (mode) {
            case "Auto":
                return "auto";
            case "Heat":
            case "On":
                return "heat";
            case "Cool":
                return "cool";
            case "Off":
                return "off";
            default:
                return "off";
        }
    }
    */
}