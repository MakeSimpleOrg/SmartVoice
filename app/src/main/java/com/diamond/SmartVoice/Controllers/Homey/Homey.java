package com.diamond.SmartVoice.Controllers.Homey;

import android.content.Intent;
import android.content.SharedPreferences;

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
import com.diamond.SmartVoice.OAuth.WebViewActivity;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.rollbar.android.Rollbar;

import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.common.exception.OAuthSystemException;

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
        name = "Homey";
        mainActivity = activity;
        clearNames = true; // TODO config
        gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (!key.equals("homey_enabled") || !pref.getBoolean(key, false))
            return;
        if ((!pref.getString("homey_server_ip", "").isEmpty() || !pref.getString("homey_server_ip_ext", "").isEmpty()) && !pref.getString("homey_bearer", "").isEmpty()) {
            loadData();
            return;
        }
        if (!pref.getString("homey_bearer", "").isEmpty())
            return;
        OAuthClientRequest request = null;
        try {
            request = OAuthClientRequest
                    .authorizationLocation("https://accounts.athom.com/login")
                    .setClientId("5534df95588a5ed82aaef73d").setRedirectURI("https://my.athom.com/auth/callback")
                    .setResponseType("code")
                    .setParameter("origin", "https://accounts.athom.com/oauth2/authorise")
                    .buildQueryMessage();
        } catch (OAuthSystemException e) {
            e.printStackTrace();
        }
        if (request != null) {
            //WebViewActivity.mainActivity = mainActivity;
            Intent intent = new Intent(mainActivity, WebViewActivity.class);
            intent.putExtra("url", request.getLocationUri());
            mainActivity.startActivity(intent);
        }
    }

    @Override
    public void loadData() {
        host = mainActivity.pref.getString("homey_server_ip", "");
        host_ext = mainActivity.pref.getString("homey_server_ip_ext", "");
        bearer = mainActivity.pref.getString("homey_bearer", "");
        updateRooms();
        updateData();
    }

    private void updateRooms() {
        Room[] loaded_rooms = null;
        try {
            JSONObject zones = getJson("/api/manager/zones/zone", null, JSONObject.class);
            if (zones != null)
                zones = zones.getJSONObject("result");
            if (zones != null) {
                loaded_rooms = new Room[zones.length()];
                int i = 0;
                Iterator<String> it = zones.keys();
                String key;
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            loaded_rooms[i++] = gson.fromJson(zones.getString(key), Room.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Rollbar.instance().error(e, zones.toString());
                        }
                    }
                }

                if (clearNames)
                    for (Room r : loaded_rooms)
                        r.setName(AI.replaceTrash(r.getName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }

        if (loaded_rooms == null && all_rooms == null)
            all_rooms = new Room[0];
        else if (loaded_rooms != null)
            all_rooms = loaded_rooms;
    }

    public void updateData() {
        Device[] loaded_devices = null;
        Scene[] loaded_scenes = null;

        try {
            String request = "/api/manager/devices/device";
            // for faster processing, loading only known device types
            request += "?capabilities_any=onoff,windowcoverings_state,dim,measure_battery,measure_power,meter_power,measure_temperature,measure_co2,measure_humidity,measure_light,measure_noise,measure_pressure,button,target_temperature,thermostat_mode";
            JSONObject devices = getJson(request, null, JSONObject.class);
            if (devices != null)
                devices = devices.getJSONObject("result");

            ArrayList<Scene> scenes = new ArrayList<>();
            Iterator<String> it;
            String key;
            if (devices != null) {
                //System.out.println(devices.toString());
                loaded_devices = new Device[devices.length()];
                int i = 0;
                it = devices.keys();
                while (it.hasNext()) {
                    key = it.next();
                    if (key != null) {
                        try {
                            loaded_devices[i++] = gson.fromJson(devices.getString(key), Device.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Rollbar.instance().error(e, devices.toString());
                        }
                    }
                }

                for (Device d : loaded_devices) {
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

            loaded_scenes = scenes.isEmpty() ? new Scene[0] : scenes.toArray(new Scene[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }

        if (loaded_devices == null && all_devices == null)
            all_devices = new Device[0];
        else if (loaded_devices != null)
            all_devices = loaded_devices;

        if (loaded_scenes == null && all_scenes == null)
            all_scenes = new Scene[0];
        else if (loaded_scenes != null)
            all_scenes = loaded_scenes;

        // System.out.println("Devices: " + all_devices.length);
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
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"windowcoverings_state\": \"" + getThermostatMode(mode) + "\"}");
    }

    @Override
    public void setTargetTemperature(UDevice d, String level) {
        sendJSON("/api/manager/devices/device/" + d.getId() + "/state/", "{\"target_temperature\": " + (level.equals("0") ? 0 : (Integer.parseInt(level) / 100)) + "}");
    }

    @Override
    public void runScene(UScene s) {
        sendJSON("/api/manager/devices/device/" + s.getId() + "/state/", "{\"button\": true}");
    }

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
}