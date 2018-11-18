package com.diamond.SmartVoice.Controllers.Vera;

import android.content.SharedPreferences;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.Controllers.Vera.json.Device;
import com.diamond.SmartVoice.Controllers.Vera.json.Room;
import com.diamond.SmartVoice.Controllers.Vera.json.Scene;
import com.diamond.SmartVoice.Controllers.Vera.json.Sdata;
import com.diamond.SmartVoice.MainActivity;
import com.google.gson.Gson;
import com.rollbar.android.Rollbar;

import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class Vera extends Controller {
    private static final String TAG = Vera.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private Scene[] all_scenes;

    public Vera(MainActivity activity) {
        name = "Vera";
        mainActivity = activity;
        clearNames = true; // TODO config
        gson = new Gson();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (key.equals("vera_enabled") && pref.getBoolean(key, false) && (!pref.getString("vera_server_ip", "").isEmpty() || !pref.getString("vera_server_ip_ext", "").isEmpty()))
            loadData();
    }

    @Override
    public void loadData()
    {
        host = mainActivity.pref.getString("vera_server_ip", "");
        if (!host.replaceAll("http://", "").replaceAll("https://", "").contains(":"))
            host += ":3480";
        host_ext = mainActivity.pref.getString("vera_server_ip_ext", "");
        updateData();
    }

    public void updateData() {
        Room[] loaded_rooms = null;
        Device[] loaded_devices = null;
        Scene[] loaded_scenes = null;
        String result = request("/data_request?id=sdata&output_format=json", null);
        Sdata data = null;
        try {
            data = result == null ? null : gson.fromJson(result, Sdata.class);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e, result);
        }
        if (data != null)
            try {
                loaded_rooms = new Room[data.getRooms().size()];
                loaded_devices = new Device[data.getDevices().size()];
                loaded_scenes = new Scene[data.getScenes().size()];

                int i = 0;
                for (Room r : data.getRooms())
                    loaded_rooms[i++] = r;

                if (clearNames)
                    for (Room r : loaded_rooms)
                        r.setName(AI.replaceTrash(r.getName()));

                i = 0;
                for (Device d : data.getDevices()) {
                    loaded_devices[i++] = d;
                    d.ai_name = d.getName();
                    if (clearNames)
                        d.ai_name = AI.replaceTrash(d.ai_name);

                    for (Room r : loaded_rooms)
                        if (r.getId().equals(d.getRoomID()))
                            d.setRoomName(r.getName());

                    d.ai_name = d.getRoomName() + " " + d.ai_name;
                    d.ai_name = d.ai_name.toLowerCase(Locale.getDefault());

                    try {
                        d.setCategoryType(CategoryType.getById(Integer.parseInt(d.getCategory())));
                        switch (d.getCategoryType()) {
                            case DimmableLight:
                                d.addCapability(Capability.onoff, d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                d.addCapability(Capability.dim, d.getValue());
                                break;
                            case Switch:
                                d.addCapability(Capability.onoff, d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                break;
                            case DoorLock:
                                d.addCapability(Capability.openclose, d.getValue().equals("false") || d.getValue().equals("0") ? "close" : "open");
                                break;
                            case WindowCovering:
                                d.addCapability(Capability.windowcoverings_state, d.getValue().equals("false") || d.getValue().equals("0") ? "down" : "up");
                                break;
                            case HumiditySensor:
                                d.addCapability(Capability.measure_humidity, d.getHumidity());
                                break;
                            case TemperatureSensor:
                                d.addCapability(Capability.measure_temperature, d.getTemperature());
                                break;
                            case LightSensor:
                                d.addCapability(Capability.measure_light, d.getLight());
                                break;
                            case HVAC:
                                d.addCapability(Capability.measure_temperature, d.getTemperature());
                                d.addCapability(Capability.target_temperature, d.getValue());
                                //Subcategory
                                //1	HVAC
                                //2	Heater
                                //3	Custom HVAC
                                break;
                        }
                        if (d.getBatterylevel() != null)
                            d.addCapability(Capability.measure_battery, d.getBatterylevel());
                        if (d.getKwh() != null)
                            d.addCapability(Capability.meter_power, d.getKwh());
                        if (d.getWatts() != null)
                            d.addCapability(Capability.measure_power, d.getWatts());
                    } catch (IndexOutOfBoundsException e) {
                        Log.w(TAG, "Unknown category type: " + d.getCategory());
                        Rollbar.instance().error(e);
                    }
                }

                i = 0;
                for (Scene s : data.getScenes())
                    if (s.isVisible()) {
                        loaded_scenes[i++] = s;
                        s.ai_name = s.getName();
                        if (clearNames)
                            s.ai_name = AI.replaceTrash(s.ai_name);
                        for (Room r : loaded_rooms)
                            if (r.getId().equals(s.getRoomID()))
                                s.setRoomName(r.getName());
                    }
            } catch (Exception e) {
                e.printStackTrace();
                Rollbar.instance().error(e);
            }

        if (loaded_rooms == null && all_rooms == null)
            all_rooms = new Room[0];
        else if (loaded_rooms != null)
            all_rooms = loaded_rooms;

        if (loaded_devices == null && all_devices == null)
            all_devices = new Device[0];
        else if (loaded_devices != null)
            all_devices = loaded_devices;

        if (loaded_scenes == null && all_scenes == null)
            all_scenes = new Scene[0];
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
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:SwitchPower1&action=SetTarget&newTargetValue=1");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:SwitchPower1&action=SetTarget&newTargetValue=0");
    }

    @Override
    public void closeLock(UDevice d) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:micasaverde-com:serviceId:DoorLock1&action=SetTarget&newTargetValue=1");
    }

    @Override
    public void openLock(UDevice d) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:micasaverde-com:serviceId:DoorLock1&action=SetTarget&newTargetValue=0");

    }

    @Override
    public void closeWindow(UDevice d) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:WindowCovering1&action=Down");
    }

    @Override
    public void openWindow(UDevice d) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:WindowCovering1&action=Up");
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:Dimming1&action=SetLoadLevelTarget&newLoadlevelTarget=" + level);
    }

    @Override
    public void setColor(UDevice d, int r, int g, int b, int w) {
        // TODO
    }

    private String lastMode = "Off"; // TODO запоминать в устройстве режим

    @Override
    public void setMode(UDevice d, String mode) {
        lastMode = mode;
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "urn:upnp-org:serviceId:HVAC_UserOperatingMode1&action=SetModeTarget&NewModeTarget=" + getThermostatMode(mode));
    }

    @Override
    public void setTargetTemperature(UDevice d, String level)
    {
        String service = lastMode.equals("Cool") ? "urn:upnp-org:serviceId:TemperatureSetpoint1_Cool" : "urn:upnp-org:serviceId:TemperatureSetpoint1_Heat";
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + service + "&action=SetCurrentSetpoint&NewCurrentSetpoint=" + level);
    }

    @Override
    public void runScene(UScene s) {
        sendCommand("/data_request?id=action&SceneNum=" + s.getId() + "&serviceId=urn:micasaverde-com:serviceId:HomeAutomationGateway1&action=RunScene");
    }

    public String getThermostatMode(String mode)
    {
        switch(mode)
        {
            case "Auto":
                return "AutoChangeOver";
            case "Heat":
            case "On":
                return "HeatOn";
            case "Cool":
                return "CoolOn";
            case "Off":
                return "Off";
            default:
                return "Off";
        }
    }
}