package com.diamond.SmartVoice.Controllers.Zipato;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Fibaro.ModeType;
import com.diamond.SmartVoice.Controllers.HttpController;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.Controllers.Zipato.json.AttributesFull;
import com.diamond.SmartVoice.Controllers.Zipato.json.Device;
import com.diamond.SmartVoice.Controllers.Zipato.json.Init;
import com.diamond.SmartVoice.Controllers.Zipato.json.Room;
import com.diamond.SmartVoice.Controllers.Zipato.json.Scene;
import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.Utils;
import com.google.gson.Gson;
import com.rollbar.android.Rollbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Dmitriy Ponomarev
 */
public class Zipato extends HttpController {
    private static final String TAG = Zipato.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private UScene[] all_scenes;

    private String jsessionid = null;
    private String username;
    private String password;

    public Zipato(MainActivity activity) {
        name = "Zipato";
        mainActivity = activity;
        clearNames = true; // TODO config
        gson = new Gson();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (!key.equals("zipato_enabled") || !pref.getBoolean(key, false))
            return;
        if ((!pref.getString("zipato_server_ip", "").isEmpty() || !pref.getString("zipato_server_ip_ext", "").isEmpty()) //
                && !pref.getString("zipato_server_login", "").isEmpty() //
                && !pref.getString("zipato_server_password", "").isEmpty())
            loadData();
    }

    @Override
    public void loadData() {
        host = mainActivity.pref.getString("zipato_server_ip", "");
        host_ext = mainActivity.pref.getString("zipato_server_ip_ext", "https://my.zipato.com:443");
        username = mainActivity.pref.getString("zipato_server_login", "");
        password = mainActivity.pref.getString("zipato_server_password", "");
        initSession();
        updateScenes();
        updateData();
    }

    private String Build_Sha1(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        md.update(input.getBytes());
        byte[] output = md.digest();
        return bytesToHex(output);
    }

    private static String bytesToHex(byte[] b) {
        char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder buf = new StringBuilder();
        for (byte aB : b) {
            buf.append(hexDigit[(aB >> 4) & 0x0f]);
            buf.append(hexDigit[aB & 0x0f]);
        }
        return buf.toString().toLowerCase();
    }

    private void initSession() {
        String result = null;
        Init init = null;
        if (jsessionid == null) {
            String token = null;
            try {
                result = request("/zipato-web/v2/user/init", null);
                //System.out.println(result);
                try {
                    init = gson.fromJson(result, Init.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    Rollbar.instance().error(e, result);
                }
                if (init != null) {
                    String sha1 = Build_Sha1(password);
                    if (sha1 != null)
                        token = Build_Sha1(init.nonce + sha1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (token != null) {
                result = request("/zipato-web/v2/user/login?username=" + username + "&token=" + token, "JSESSIONID=" + init.jsessionid);
                if (result == null || !result.contains("true")) {
                    Log.w(TAG, "Failed to update data: " + result);
                } else
                    jsessionid = init.jsessionid;
            }
        }
    }

    public void updateData() {
        Room[] loaded_rooms = null;
        Device[] loaded_devices = null;
        if (jsessionid != null) {
            try {
                JSONArray result = getJson("/zipato-web/v2/attributes/full?full=true", "JSESSIONID=" + jsessionid, JSONArray.class);
                if (result != null) {
                    // Для тестов
                    /*
                    try {
                        result = mapper.readValue(new FileInputStream(new File(Utils.assetDir, "gson.txt")), JSONArray.class);
                        //result = Utils.getStringFromFile(new File(Utils.assetDir, "gson.txt"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    */

                    @SuppressLint("UseSparseArrays") HashMap<Integer, Room> rooms = new HashMap<>();
                    ArrayList<Device> devices = new ArrayList<>(result.length());

                    AttributesFull[] all = null;
                    try {
                        all = gson.fromJson(result.toString(), AttributesFull[].class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Rollbar.instance().error(e, result.toString());
                    }

                    if (all != null)
                        for (AttributesFull a : all) {
                            Device d = new Device();
                            if (a.value != null)
                                d.setValue(a.value.value);
                            if (a.uiType != null && a.uiType.endpointType != null)
                                switch (a.uiType.endpointType) {
                                    case "actuator.onoff":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().isEmpty() || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        break;
                                    case "meter.temperature":
                                        d.addCapability(Capability.measure_temperature, d.getValue());
                                        break;
                                    case "meter.light":
                                        d.addCapability(Capability.measure_light, String.valueOf((int) Float.parseFloat(d.getValue())));
                                        break;
                                    case "actuator.dimmer":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().isEmpty() || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        d.addCapability(Capability.dim, d.getValue() == null || d.getValue().isEmpty() ? "0" : d.getValue());
                                        break;
                                    case "actuator.door_lock":
                                        d.addCapability(Capability.openclose, d.getValue().equals("false") || d.getValue().equals("0") ? "close" : "open");
                                        break;
                                    default:
                                        continue;
                                }
                            else if (a.definition != null && a.definition.cluster != null)
                                switch (a.definition.cluster) {
                                    case "com.zipato.cluster.OnOff":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().isEmpty() || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        break;
                                    case "com.zipato.cluster.MultiLvlSensor":
                                        if (a.name != null)
                                            if ("°C".equalsIgnoreCase(a.config.unit) || "°F".equalsIgnoreCase(a.config.unit))
                                                d.addCapability(Capability.measure_temperature, d.getValue());
                                            else if ("lux".equalsIgnoreCase(a.config.unit))
                                                d.addCapability(Capability.measure_light, String.valueOf((int) Float.parseFloat(d.getValue())));
                                        break;
                                    case "com.zipato.cluster.Gauge":
                                        if ("°C".equalsIgnoreCase(a.config.unit) || "°F".equalsIgnoreCase(a.config.unit))
                                            d.addCapability(Capability.measure_temperature, d.getValue());
                                        break;
                                    case "com.zipato.cluster.ThermostatMode":
                                        d.addCapability(Capability.thermostat_mode, d.getValue());
                                        break;
                                    case "com.zipato.cluster.ThermostatSetpoint":
                                        d.addCapability(Capability.measure_temperature, d.getValue());
                                        d.addCapability(Capability.target_temperature, d.getValue());
                                        break;
                                    case "com.zipato.cluster.LevelControl":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().isEmpty() || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        d.addCapability(Capability.dim, d.getValue() == null || d.getValue().isEmpty() ? "0" : d.getValue());
                                        break;
                                    case "com.zipato.network.virtual.weather.WeatherData":
                                        //"definition"."attribute" : "icon","winddir16Point","precipMM","tempMinC","tempMaxC","windspeedKmph",
                                        continue;
                                    case "com.zipato.network.virtual.alarm.PartitionControl":
                                        //"attribute" : "armed",
                                        //"config"."enumValues" : {"true" : "Armed","false" : "Disarmed"},
                                    case "com.zipato.cluster.SensorBinary":
                                        //"config"."enumValues" : {"true" : "MOTION","false" : "NO_MOTION"},
                                        continue;
                                    case "com.zipato.cluster.AlarmSensor":
                                        //"config"."enumValues" : {"true" : "SHOCK","false" : "NO_SHOCK"},
                                        continue;
                                    case "com.zipato.cluster.Notifications":
                                        //"config"."enumValues" : {"3" : "TAMPER"},
                                        continue;
                                    case "com.zipato.network.virtual.thermostat.ThermostatMasterControl":
                                        continue;
                                    case "com.zipato.network.virtual.thermostat.ThermostatOnOff":
                                        continue;
                                    case "com.zipato.cluster.MeterMeter":
                                        continue;
                                    default:
                                        Log.w(TAG, "Unknown device type: " + a.definition.cluster);
                                        continue;
                                }

                            d.setId(a.uuid);

                            switch(a.name.toLowerCase())
                            {
                                case "mode":
                                case "state":
                                case "temperature":
                                case "setpoint_heating":
                                case "valve_position":
                                case "thermostat_mode":
                                    d.setName(AI.replaceTrash(a.clusterEndpoint.name));
                                    break;
                                default:
                                    d.setName(AI.replaceTrash(a.name));
                            }

                            if (a.room != null) {
                                a.room.setName(AI.replaceTrash(a.room.getName()));
                                rooms.put(a.room.id, a.room);
                                d.setRoomName(a.room.getName());
                            } else d.setRoomName("");

                            d.ai_name = d.getRoomName() + " " + d.getName();

                            if (d.getCapabilities().size() > 0)
                                devices.add(d);
                        }

                    loaded_rooms = new Room[rooms.size()];
                    int i = 0;
                    for (Room r : rooms.values())
                        loaded_rooms[i++] = r;
                    loaded_devices = new Device[devices.size()];
                    i = 0;
                    for (Device d : devices)
                        loaded_devices[i++] = d;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Rollbar.instance().error(e);
            }
        }

        if (loaded_rooms == null && all_rooms == null)
            all_rooms = new Room[0];
        else if (loaded_rooms != null)
            all_rooms = loaded_rooms;

        if (loaded_devices == null && all_devices == null)
            all_devices = new Device[0];
        else if (loaded_devices != null)
            all_devices = loaded_devices;
    }

    private void updateScenes() {
        Scene[] loaded_scenes = null;
        JSONObject obj = getJson("/zipato-web/rest/scenes/", "JSESSIONID=" + jsessionid, JSONObject.class);
        Iterator<String> it;
        String id;
        Scene s;
        if (obj != null) {
            loaded_scenes = new Scene[obj.length()];
            int i = 0;
            it = obj.keys();
            while (it.hasNext()) {
                id = it.next();
                if (id != null) {
                    try {
                        s = gson.fromJson(obj.getString(id), Scene.class);
                        s.setId(id);
                        if (s.isVisible()) {
                            s.ai_name = s.getName();
                            if (clearNames)
                                s.ai_name = AI.replaceTrash(s.ai_name);
                        }
                        loaded_scenes[i++] = s;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Rollbar.instance().error(e, obj.toString());
                    }
                }
            }
        }

        if (loaded_scenes == null && all_scenes == null)
            all_scenes = new UScene[0];
        else if (loaded_scenes != null)
            all_scenes = loaded_scenes;
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
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": true}", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": false}", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    @Override
    public void closeLock(UDevice d) {
        turnDeviceOn(d);
    }

    @Override
    public void openLock(UDevice d) {
        turnDeviceOff(d);
    }

    @Override
    public void closeWindow(UDevice d) {
        turnDeviceOn(d);
    }

    @Override
    public void openWindow(UDevice d) {
        turnDeviceOff(d);
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": " + level + "}", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    @Override
    public void setColor(UDevice d, int r, int g, int b, int w) {
        // TODO
    }

    @Override
    public void setMode(UDevice d, String mode) {
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": " + getThermostatMode(mode) + "}", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    @Override
    public void setTargetTemperature(UDevice d, String level) {
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": " + level + "}", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    @Override
    public void runScene(UScene s) {
        sendCommand("/zipato-web/rest/scenes/" + s.getId() + "/run", jsessionid == null ? null : ("JSESSIONID=" + jsessionid));
    }

    public String getThermostatMode(String mode)
    {
        switch(mode)
        {
            case "Auto":
            case "Heat":
            case "On":
                return "HEAT";
            case "Cool":
                return "COOL";
            case "Off":
                return "OFF";
            default:
                return "OFF";
        }
    }
}