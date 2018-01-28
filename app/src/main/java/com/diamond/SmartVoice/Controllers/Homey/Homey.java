package com.diamond.SmartVoice.Controllers.Homey;

import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Homey.json.Device;
import com.diamond.SmartVoice.Controllers.Homey.json.Room;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * @author Dmitriy Ponomarev
 */
public class Homey extends Controller {
    private static final String TAG = Homey.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private UScene[] all_scenes = new UScene[0]; // TODO заглушка

    public Homey(SharedPreferences pref) {
        host = pref.getString("homey_server_ip", "");
        bearer = pref.getString("homey_bearer", "");
        clearNames = true; // TODO config
        gson = new Gson();
        updateData();
    }

    private void updateData() {
        try {
            String result = request("/api/manager/zones/zone");
            System.out.println("result: " + result);
            JSONObject zones = null;
            try {
                zones = new JSONObject(result).getJSONObject("result");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (zones != null) {
                all_rooms = new Room[zones.length()];
                int i = 0;
                Iterator<String> it = zones.keys();
                JSONObject zone;
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            all_rooms[i++] = gson.fromJson(zones.getString(key), Room.class);
                            //System.out.println("Room: " + all_rooms[i - 1].getName());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (clearNames)
                for (Room r : all_rooms)
                    r.setName(AI.replaceTrash(r.getName()));

            result = request("/api/manager/devices/device");
            JSONObject devices = null;
            try {
                devices = new JSONObject(result).getJSONObject("result");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (devices != null) {
                all_devices = new Device[devices.length()];
                int i = 0;
                Iterator<String> it = devices.keys();
                JSONObject device;
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            all_devices[i++] = gson.fromJson(devices.getString(key), Device.class);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                for (Device d : all_devices) {
                    d.ai_name = d.getName();
                    if (clearNames)
                        d.ai_name = AI.replaceTrash(d.ai_name);
                    for (Room r : all_rooms)
                        if (r.getId().equals(d.getRoomID()))
                            d.setRoomName(r.getName());
                    d.ai_name = d.getRoomName() + " " + d.ai_name;
                    d.ai_name = d.ai_name.toLowerCase(Locale.getDefault());

                    if (d.state != null)
                        for (Map.Entry<Capability, String> entry : d.state.entrySet())
                            if (entry.getKey() != null && entry.getValue() != null) {
                                switch (entry.getKey()) {
                                    case onoff:
                                        entry.setValue("true".equals(entry.getValue()) ? "1" : "0");
                                        break;
                                    case measure_battery:
                                    case measure_power:
                                    case meter_power:
                                    case measure_temperature:
                                    case measure_co2:
                                    case measure_humidity:
                                    case measure_light:
                                    case measure_noise:
                                    case measure_pressure:
                                        entry.setValue("" + (int) Double.parseDouble(entry.getValue()));
                                        break;
                                }

                                d.addCapability(entry.getKey(), entry.getValue());
                                //System.out.println("Cap: " + entry.getKey() + ", value: " + entry.getValue());
                            }
                }
            }

            /* TODO сцены
            result = request("/api/scenes?enabled=true&visible=true");
            all_scenes = result == null ? new Scene[0] : gson.fromJson(result, Scene[].class);
            for (Scene s : all_scenes)
                if (s.isVisible()) {
                        s.ai_name = s.getName();
                    if (clearNames)
                        s.ai_name = AI.replaceTrash(s.ai_name);
                    for (Room r : all_rooms)
                        if (r.getId().equals(s.getRoomID()))
                            s.setRoomName(r.getName());
                }
            */
        } catch (IOException e) {
            Log.w(TAG, "Failed to update data");
            e.printStackTrace();
        }

        if(all_rooms == null)
            all_rooms = new Room[0];
        if(all_devices == null)
            all_devices = new Device[0];
    }

    @Override
    public URoom[] getRooms() {
        return all_rooms;
    }

    @Override
    public UDevice[] getDevices() {
        return all_devices;
    }

    @Override
    public UScene[] getScenes() {
        return all_scenes;
    }

    @Override
    public void turnDeviceOn(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"onoff\": true}");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"onoff\": false}");
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"dim\": " + level + "}");
    }

    @Override
    public void setColor(UDevice d, int r, int g, int b, int w) {
        // TODO
    }

    @Override
    public void setMode(UDevice d, String mode) {
        // TODO
    }

    @Override
    public void runScene(UScene s) {
        // TODO
    }
}