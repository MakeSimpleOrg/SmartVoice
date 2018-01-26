package com.diamond.SmartVoice.Controllers.Vera;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.Controllers.UType;
import com.diamond.SmartVoice.Controllers.Vera.json.Device;
import com.diamond.SmartVoice.Controllers.Vera.json.Room;
import com.diamond.SmartVoice.Controllers.Vera.json.Scene;
import com.diamond.SmartVoice.Controllers.Vera.json.Sdata;
import com.google.gson.Gson;

/**
 * @author Dmitriy Ponomarev
 */
public class Vera extends Controller {
    private static final String TAG = Vera.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private Scene[] all_scenes;

    public Vera(SharedPreferences pref) {
        host = pref.getString("vera_server_ip", "");
        clearNames = true; // TODO config
        gson = new Gson();
        updateData();
    }

    private void updateData() {
        try {
            String result = request("/data_request?id=sdata&output_format=json");
            Sdata data = gson.fromJson(result, Sdata.class);
            all_rooms = new Room[data.getRooms().size()];
            all_devices = new Device[data.getDevices().size()];
            all_scenes = new Scene[data.getScenes().size()];

            if (clearNames)
                for (Room r : all_rooms)
                    r.setName(AI.replaceTrash(r.getName()));

            int i = 0;
            for (Device d : data.getDevices()) {
                all_devices[i++] = d;
                d.ai_name = d.getName();
                if (clearNames)
                    d.ai_name = AI.replaceTrash(d.ai_name);

                for (Room r : all_rooms)
                    if (r.getId() == d.getRoomID())
                        d.setRoomName(r.getName());

                d.ai_name = d.getRoomName() + " " + d.ai_name;
                d.ai_name = d.ai_name.toLowerCase(Locale.getDefault());

                try {
                    d.setCategoryType(CategoryType.values()[Integer.parseInt(d.getCategory())]);
                    d.uType = d.getCategoryType().getUType();
                } catch (IndexOutOfBoundsException e) {
                    Log.w(TAG, "Unknown category type: " + d.getCategory());
                }
            }

            i = 0;
            for (Scene s : data.getScenes())
                if (s.isVisible()) {
                    all_scenes[i++] = s;
                    s.ai_name = s.getName();
                    if (clearNames)
                        s.ai_name = AI.replaceTrash(s.ai_name);
                    for (Room r : all_rooms)
                        if (r.getId() == s.getRoomID())
                            s.setRoomName(r.getName());
                }
        } catch (IOException e) {
            Log.w(TAG, "Failed to update data");
        }
    }

    private void setStatus(Device d, String status) {
        //d.setStatus(status);
        String service = "urn:upnp-org:serviceId:SwitchPower1";
        if (CategoryType.DoorLock.equals(d.getCategoryType()))
            service = "urn:micasaverde-com:serviceId:DoorLock1";
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=" + service + "&action=SetTarget&newTargetValue=" + status);
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
        setStatus((Device) d, "1");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        setStatus((Device) d, "0");
    }

    @Override
    public void setDimLevel(UDevice d, String level) {
        //d.setLevel(level);
        sendCommand("/data_request?id=action&DeviceNum=" + d.getId() + "&serviceId=urn:upnp-org:serviceId:Dimming1&action=SetLoadLevelTarget&newLoadlevelTarget=" + level);
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
        sendCommand("/data_request?id=action&SceneNum=" + s.getId() + "&serviceId=urn:micasaverde-com:serviceId:HomeAutomationGateway1&action=RunScene");
    }
}