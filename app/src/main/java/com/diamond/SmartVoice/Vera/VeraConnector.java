package com.diamond.SmartVoice.Vera;

import android.net.Uri;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.UDevice;
import com.diamond.SmartVoice.UScene;
import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.http.impl.client.DefaultHttpClient;

public class VeraConnector {
    private static final String TAG = VeraConnector.class.getSimpleName();

    private final DefaultHttpClient httpClient = new DefaultHttpClient();

    private Uri veraUri;

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

    public VeraConnector(String veraHost) {
        veraUri = Uri.parse("http://" + veraHost + ":3480/data_request");
    }

    public Sdata getSdata() {
        if (veraUri == null) return null;
        String theData = get(veraUri.buildUpon().appendQueryParameter("id", "sdata").appendQueryParameter("output_format", "json").build());
        Sdata theSdata = new Gson().fromJson(theData, Sdata.class);
        if (theSdata == null)
            return null;
        denormalizeSdata(theSdata);
        roomCount = theSdata.rooms.size();
        deviceCount = theSdata.devices.size();
        sceneCount = theSdata.scenes.size();
        return theSdata;
    }

    private void denormalizeSdata(Sdata theSdata) {
        Map<String, Room> roomMap = new HashMap<String, Room>();
        for (Room i : theSdata.rooms) roomMap.put(i.id, i);
        Map<String, Categorie> categoryMap = new HashMap<String, Categorie>();
        for (Categorie i : theSdata.categories) categoryMap.put(i.id, i);
        Categorie controllerCat = new Categorie();
        controllerCat.name = "Controller";
        controllerCat.id = "0";
        categoryMap.put(controllerCat.id, controllerCat);
        ListIterator<Device> theIterator = theSdata.devices.listIterator();
        Device theDevice;
        while (theIterator.hasNext()) {
            theDevice = theIterator.next();
            if (theDevice.room != null && roomMap.get(theDevice.room) != null)
                theDevice.room = roomMap.get(theDevice.room).name;
            else
                theDevice.room = "no room";
            if (theDevice.category != null && categoryMap.get(theDevice.category) != null)
                theDevice.categoryName = categoryMap.get(theDevice.category).name;
            else
                theDevice.categoryName = "<unknown>";
        }
        ListIterator<Scene> theSecneIter = theSdata.scenes.listIterator();
        Scene theScene;
        while (theSecneIter.hasNext()) {
            theScene = theSecneIter.next();
            if (theScene.room != null && roomMap.get(theScene.room) != null)
                theScene.room = roomMap.get(theScene.room).name;
            else
                theScene.room = "no room";
        }
    }

    private String get(Uri uri) {
        final String url = uri.toString();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                response.getEntity().consumeContent();
                return null;
            }
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Can't execute [" + url + "]", e);
            return null;
        }
    }

    public void turnDeviceOn(String id, int cat) {
        setStatus(id, "1", cat == 7 ? "urn:micasaverde-com:serviceId:DoorLock1" : "urn:upnp-org:serviceId:SwitchPower1");
    }

    public void turnDeviceOff(String id, int cat) {
        setStatus(id, "0", cat == 7 ? "urn:micasaverde-com:serviceId:DoorLock1" : "urn:upnp-org:serviceId:SwitchPower1");
    }

    public void runScene(String id) {
        get(veraUri.buildUpon().appendQueryParameter("id", "action")
                .appendQueryParameter("SceneNum", id)
                .appendQueryParameter("serviceId", "urn:micasaverde-com:serviceId:HomeAutomationGateway1")
                .appendQueryParameter("action", "RunScene").build());
    }

    public void setDimLevel(String id, int level) {
        get(veraUri.buildUpon().appendQueryParameter("id", "action")
                .appendQueryParameter("DeviceNum", id)
                .appendQueryParameter("serviceId", "urn:upnp-org:serviceId:Dimming1")
                .appendQueryParameter("action", "SetLoadLevelTarget")
                .appendQueryParameter("newLoadlevelTarget", level + "").build());
    }

    private void setStatus(String id, String status, String service) {
        get(veraUri.buildUpon().appendQueryParameter("id", "action")
                .appendQueryParameter("DeviceNum", id)
                .appendQueryParameter("serviceId", service)
                .appendQueryParameter("action", "SetTarget")
                .appendQueryParameter("newTargetValue", status).build());
    }

    public Device[] getDevices() {
        Sdata sdata = getSdata();
        List<Device> all_devices = sdata.devices;
        int count = 0;
        for (Device d : all_devices) {
            if (!d.room.equals("no room"))
                count++;
        }
        Device[] devices = new Device[count];
        int i = 0;
        String name;
        for (Device d : all_devices)
            if (!d.room.equals("no room")) {
                d.ai_name = d.name.toLowerCase().trim();
                d.ai_name = d.room.toLowerCase() + " " + d.ai_name;
                d.ai_name = AI.replaceTrash(d.ai_name);
                Log.w(TAG, d.ai_name);
                devices[i++] = d;
            }
        return devices;
    }

    public Scene[] getScenes() {
        Sdata sdata = getSdata();
        List<Scene> scenes = sdata.scenes;
        Scene[] result = new Scene[sdata.scenes.size()];
        int i = 0;
        for (Scene s : scenes) {
            s.ai_name = s.name.toLowerCase().trim();
            result[i++] = s;
        }
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
        int cat = 0;
        for (UDevice u : devices) {
            d = (Device) u;
            Log.w(TAG, "найдено: " + d.ai_name + ", " + Integer.parseInt(d.category) + ", " + d.category);
            cat = Integer.parseInt(d.category);
            switch (cat) {
                case 2:
                case 3:
                case 7:
                    if (d.ai_name.contains("включить")) {
                        turnDeviceOn(d.id, cat);
                        finded = true;
                        enabled = true;
                    } else if (d.ai_name.contains("выключить")) {
                        turnDeviceOff(d.id, cat);
                        finded = true;
                        enabled = false;
                    } else if (finded)
                        if (enabled)
                            turnDeviceOn(d.id, cat);
                        else
                            turnDeviceOff(d.id, cat);
                    else if (d.status.equals("0")) {
                        turnDeviceOn(d.id, cat);
                        finded = true;
                        enabled = true;
                    } else {
                        turnDeviceOff(d.id, cat);
                        finded = true;
                        enabled = false;
                    }
                    break;
                case 16:
                    return "" + (int) Double.parseDouble(d.humidity);
                case 17:
                    return "" + (int) Double.parseDouble(d.temperature);
                case 18:
                    return "" + (int) Double.parseDouble(d.light);
            }
        }
        if (finded)
            if (enabled)
                return cat == 7 ? "Закрываю" : "Включаю";
            else
                return cat == 7 ? "Открываю" : "Выключаю";
        return "Ошибка";
    }

    public String processScenes(UScene[] scenes) {
        for (UScene s : scenes)
            runScene(((Scene) s).id);
        return "Выполняю";
    }
}
