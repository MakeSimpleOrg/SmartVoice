package com.diamond.SmartVoice.Controllers.Fibaro;

import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Fibaro.json.Device;
import com.diamond.SmartVoice.Controllers.Fibaro.json.Room;
import com.diamond.SmartVoice.Controllers.Fibaro.json.Scene;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.MainActivity;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class Fibaro extends Controller {
    private static final String TAG = Fibaro.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private Scene[] all_scenes;

    public Fibaro(MainActivity activity) {
        mainActivity = activity;
        host = activity.pref.getString("fibaro_server_ip", "");
        auth = android.util.Base64.encodeToString((activity.pref.getString("fibaro_server_login", "") + ":" + activity.pref.getString("fibaro_server_password", "")).getBytes(), android.util.Base64.DEFAULT);
        clearNames = true; // TODO config
        gson = new Gson();
        updateData();
    }


    private void updateData() {
        try {
            String result = request("/api/rooms");
            all_rooms = result == null ? new Room[0] : gson.fromJson(result, Room[].class);
            if (clearNames)
                for (Room r : all_rooms)
                    r.setName(AI.replaceTrash(r.getName()));

            result = request("/api/devices?enabled=true&visible=true");
            all_devices = result == null ? new Device[0] : gson.fromJson(result, Device[].class);
            for (Device d : all_devices) {
                if (d.getProperties().getUserDescription() != null && !d.getProperties().getUserDescription().isEmpty())
                    d.ai_name = d.getProperties().getUserDescription();
                else
                    d.ai_name = d.getName();
                if (clearNames)
                    d.ai_name = AI.replaceTrash(d.ai_name);
                for (Room r : all_rooms)
                    if (r.getId().equals(d.getRoomID()))
                        d.setRoomName(r.getName());
                d.ai_name = d.getRoomName() + " " + d.ai_name;
                d.ai_name = d.ai_name.toLowerCase(Locale.getDefault());

                switch (d.getType()) {
                    case "com.fibaro.multilevelSwitch":
                    case "com.fibaro.FGMS001":
                    case "com.fibaro.motionSensor":
                    case "com.fibaro.multilevelSensor":
                    case "com.fibaro.doorSensor":
                    case "com.fibaro.windowSensor":
                    case "com.fibaro.FGFS101":
                    case "com.fibaro.floodSensor":
                    case "com.fibaro.FGSS001":
                        break;
                    case "com.fibaro.temperatureSensor":
                        d.addCapability(Capability.measure_temperature, "" + (int) Double.parseDouble(d.getValue()));
                        break;
                    case "com.fibaro.humiditySensor":
                        d.addCapability(Capability.measure_humidity, "" + (int) Double.parseDouble(d.getValue()));
                        break;
                    case "com.fibaro.lightSensor":
                        d.addCapability(Capability.measure_light, "" + (int) Double.parseDouble(d.getValue()));
                        break;
                    case "com.fibaro.doorLock":
                    case "com.fibaro.gerda":
                        d.addCapability(Capability.openclose, d.getValue().equals("false") || d.getValue().equals("0") ? "close" : "open");
                        break;
                    case "com.fibaro.FGD212":
                    case "com.fibaro.FGRGBW441M":
                    case "com.fibaro.colorController":
                    case "com.fibaro.FGRM222":
                    case "com.fibaro.FGR221":
                    case "com.fibaro.binarySwitch":
                    case "com.fibaro.developer.bxs.virtualBinarySwitch":
                    case "com.fibaro.FGWP101":
                    case "com.fibaro.FGWP102":
                    case "virtual_device":
                        d.addCapability(Capability.onoff, d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                        break;
                    case "com.fibaro.rollerShutter":
                        d.addCapability(Capability.windowcoverings_state, d.getValue().equals("false") || d.getValue().equals("0") ? "up" : "down");
                        break;
                }

                if (d.getProperties() != null && !d.getCapabilities().isEmpty()) {
                    if (d.getProperties().getBatteryLevel() != null)
                        d.addCapability(Capability.measure_battery, d.getProperties().getBatteryLevel());
                    if (d.getProperties().getEnergy() != null)
                        d.addCapability(Capability.meter_power, d.getProperties().getEnergy());
                    if (d.getProperties().getPower() != null)
                        d.addCapability(Capability.measure_power, d.getProperties().getPower());
                }
            }

            result = request("/api/scenes?enabled=true&visible=true");
            all_scenes = result == null ? new Scene[0] : gson.fromJson(result, Scene[].class);
            for (Scene s : all_scenes)
                if (s.isVisible()) {
                    if (s.getLiliStartCommand() != null && !s.getLiliStartCommand().isEmpty())
                        s.ai_name = s.getLiliStartCommand().toLowerCase(Locale.getDefault()).trim();
                    else
                        s.ai_name = s.getName();
                    if (clearNames)
                        s.ai_name = AI.replaceTrash(s.ai_name);
                    for (Room r : all_rooms)
                        if (r.getId().equals(s.getRoomID()))
                            s.setRoomName(r.getName());
                }
        } catch (IOException e) {
            if (mainActivity.isDebug())
                mainActivity.show(e.getMessage());
            Log.w(TAG, "Failed to update data");
            e.printStackTrace();
        }

        if (all_rooms == null)
            all_rooms = new Room[0];
        if (all_devices == null)
            all_devices = new Device[0];
        if (all_scenes == null)
            all_scenes = new Scene[0];
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
        sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=turnOn");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=turnOff");
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
        sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setTargetLevel&arg1=" + level);
    }

    @Override
    public void setColor(UDevice d, int r, int g, int b, int w) {
        //int r = (int) Math.round(255 * (hsb.getRed().doubleValue() / 100));
        //int g = (int) Math.round(255 * (hsb.getGreen().doubleValue() / 100));
        //int b = (int) Math.round(255 * (hsb.getBlue().doubleValue() / 100));
        //int w = (int) Math.round(255 * (hsb.getBrightness().doubleValue() / 100));

        sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setColor&arg1=" + r + "&arg2=" + g + "&arg3=" + b + "&arg4=" + w);

        // sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setR&arg1=" + r);
        // sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setG&arg1=" + g);
        // sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setB&arg1=" + b);
        // sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setW&arg1=" + w);
    }

    @Override
    public void setMode(UDevice d, String mode) {
        sendCommand("/api/callAction?deviceID=" + d.getId() + "&name=setMode&arg1=" + mode);
    }

    @Override
    public void runScene(UScene s) {
        sendCommand("/api/sceneControl?id=" + s.getId() + "&action=start");
    }
}