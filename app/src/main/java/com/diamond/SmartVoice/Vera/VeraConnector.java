package com.diamond.SmartVoice.Vera;

import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
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
        denormalizeSdata(theSdata);
        roomCount = theSdata.getRooms().size();
        deviceCount = theSdata.getDevices().size();
        sceneCount = theSdata.getScenes().size();
        return theSdata;
    }

    private void toggle(Device device) {
        String status = device.getStatus();
        status = "1".equals(status) ? "0" : "1";
        get(veraUri.buildUpon().appendQueryParameter("id", "action")
                .appendQueryParameter("DeviceNum", device.getId() + "")
                .appendQueryParameter("serviceId", "urn:upnp-org:serviceId:SwitchPower1")
                .appendQueryParameter("action", "SetTarget")
                .appendQueryParameter("newTargetValue", status).build());
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

    private void denormalizeSdata(Sdata theSdata) {
        Map<String, Room> roomMap = new HashMap<String, Room>();
        for (Room i : theSdata.getRooms()) roomMap.put(i.getId(), i);
        Map<String, Categorie> categoryMap = new HashMap<String, Categorie>();
        for (Categorie i : theSdata.getCategoriess()) categoryMap.put(i.getId(), i);
        Categorie controllerCat = new Categorie();
        controllerCat.setName("Controller");
        controllerCat.setId("0");
        categoryMap.put(controllerCat.getId(), controllerCat);
        ListIterator<Device> theIterator = theSdata.getDevices().listIterator();
        Device theDevice;
        while (theIterator.hasNext()) {
            theDevice = theIterator.next();
            if (theDevice.getRoom() != null && roomMap.get(theDevice.getRoom()) != null)
                theDevice.setRoom(roomMap.get(theDevice.getRoom()).getName());
            else
                theDevice.setRoom("no room");

            if (theDevice.getCategory() != null && categoryMap.get(theDevice.getCategory()) != null)
                theDevice.setCategory(categoryMap.get(theDevice.getCategory()).getName());
            else
                theDevice.setCategory("<unknown>");
        }

        ListIterator<Scene> theSecneIter = theSdata.getScenes().listIterator();
        Scene theScene;
        while (theSecneIter.hasNext()) {
            theScene = theSecneIter.next();
            if (theScene.getRoom() != null && roomMap.get(theScene.getRoom()) != null)
                theScene.setRoom(roomMap.get(theScene.getRoom()).getName());
            else
                theScene.setRoom("no room");
        }
    }
}
