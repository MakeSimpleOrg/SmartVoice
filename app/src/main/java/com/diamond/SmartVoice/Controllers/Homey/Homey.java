package com.diamond.SmartVoice.Controllers.Homey;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Homey.json.Device;
import com.diamond.SmartVoice.Controllers.Homey.json.Room;
import com.diamond.SmartVoice.Controllers.Homey.json.Scene;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.MainActivity;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.rollbar.android.Rollbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private UScene[] all_scenes;

    public Homey(MainActivity activity) {
        mainActivity = activity;
        host = activity.pref.getString("homey_server_ip", "");
        /* TODO config & home wifi detect
        String homey_id = pref.getString("homey_id", null);
        if (homey_id != null)
            host_ext = homey_id + ".homey.athom.com";
        */
        bearer = activity.pref.getString("homey_bearer", "");
        clearNames = true; // TODO config
        gson = new GsonBuilder().serializeNulls().create();
        updateData();
    }

    private void updateData() {
        try {
            String result = request("/api/manager/zones/zone", null);
            JSONObject zones = null;
            try {
                if (result != null)
                    zones = new JSONObject(result).getJSONObject("result");
            } catch (JSONException e) {
                e.printStackTrace();
                Rollbar.instance().error(e);
            }
            if (zones != null) {
                all_rooms = new Room[zones.length()];
                int i = 0;
                Iterator<String> it = zones.keys();
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            all_rooms[i++] = gson.fromJson(zones.getString(key), Room.class);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Rollbar.instance().error(e, result);
                        }
                    }
                }
            }

            if (clearNames)
                for (Room r : all_rooms)
                    r.setName(AI.replaceTrash(r.getName()));

            result = request("/api/manager/devices/device", null);
            JSONObject devices = null;
            try {
                if (result != null)
                    devices = new JSONObject(result).getJSONObject("result");
            } catch (JSONException e) {
                e.printStackTrace();
                Rollbar.instance().error(e, result);
            }

            ArrayList<Scene> scenes = new ArrayList<>();

            Iterator<String> it;
            String key;
            if (devices != null) {
                all_devices = new Device[devices.length()];
                int i = 0;
                it = devices.keys();
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            all_devices[i++] = gson.fromJson(devices.getString(key), Device.class);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Rollbar.instance().error(e, result);
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

                    if (d.state != null && !(d.state instanceof JsonNull)) {
                        Capability capability = null;
                        String value = null;
                        for (Map.Entry<String, JsonElement> entry : d.state.getAsJsonObject().entrySet()) {
                            if (entry.getKey() != null) {
                                try {
                                    value = entry.getValue() instanceof JsonNull ? "0" : entry.getValue().getAsString();
                                    capability = Capability.get(entry.getKey());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Rollbar.instance().error(e);
                                }
                                if (capability != null && value != null) {
                                    switch (capability) {
                                        case onoff:
                                            value = "true".equals(value) ? "1" : "0";
                                            break;
                                        case windowcoverings_state:
                                            value = "up".equals(value) ? "up" : "down";
                                            break;
                                        case dim:
                                            try {
                                                value = String.valueOf((int) Float.parseFloat(value));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
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
                                            value = "" + (int) Double.parseDouble(value);
                                            break;
                                    }

                                    if (capability == Capability.button) {
                                        Scene s = new Scene();
                                        s.setName(d.getName());
                                        s.setId(d.getId());
                                        s.setRoomName(d.getRoomName());
                                        s.ai_name = s.getName();
                                        if (clearNames)
                                            s.ai_name = AI.replaceTrash(s.ai_name);
                                        scenes.add(s);
                                    } else
                                        d.addCapability(capability, value);
                                }
                            }
                        }
                    }
                }
            }

            all_scenes = scenes.isEmpty() ? new Scene[0] : scenes.toArray(new Scene[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }

        if (all_rooms == null)
            all_rooms = new Room[0];
        if (all_devices == null)
            all_devices = new Device[0];
        if (all_scenes == null)
            all_scenes = new Scene[0];

        System.out.println("Devices: " + all_devices.length);
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
    public void closeLock(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"onoff\": true}");
    }

    @Override
    public void openLock(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"onoff\": false}");
    }

    @Override
    public void closeWindow(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"windowcoverings_state\": \"down\"}");
    }

    @Override
    public void openWindow(UDevice d) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"windowcoverings_state\": \"up\"}");
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"dim\": " + (level.equals("0") ? 0f : ((float) Integer.parseInt(level) / 100)) + "}");
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
        sendJSON("/api/manager/devices/device/" + s.getId() + "/state/", "{\"button\": true}");
    }
}