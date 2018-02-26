package com.diamond.SmartVoice.Controllers.Zipato;

import android.annotation.SuppressLint;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;
import com.diamond.SmartVoice.Controllers.Zipato.json.AttributesFull;
import com.diamond.SmartVoice.Controllers.Zipato.json.Device;
import com.diamond.SmartVoice.Controllers.Zipato.json.Init;
import com.diamond.SmartVoice.Controllers.Zipato.json.Room;
import com.diamond.SmartVoice.MainActivity;
import com.google.gson.Gson;
import com.rollbar.android.Rollbar;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Dmitriy Ponomarev
 */
public class Zipato extends Controller {
    private static final String TAG = Zipato.class.getSimpleName();

    private Room[] all_rooms;
    private Device[] all_devices;
    private UScene[] all_scenes;

    private String jsessionid = null;
    private String username;
    private String password;

    public Zipato(MainActivity activity) {
        mainActivity = activity;
        host = activity.pref.getString("zipato_server_ip", "my.zipato.com:443");
        if (host.contains("443"))
            host_ext = host;
        username = activity.pref.getString("zipato_server_login", "");
        password = activity.pref.getString("zipato_server_password", "");
        clearNames = true; // TODO config
        gson = new Gson();
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

    private void updateData() {
        String result = null;
        Init init = null;
        if (jsessionid == null) {
            String token = null;
            try {
                result = request("/zipato-web/v2/user/init", null);
                System.out.println(result);
                init = gson.fromJson(result, Init.class);
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

        if (jsessionid != null)
            try {
                result = request("/zipato-web/v2/attributes/full?full=true", "JSESSIONID=" + jsessionid);

                @SuppressLint("UseSparseArrays") HashMap<Integer, Room> rooms = new HashMap<>();
                ArrayList<Device> devices = new ArrayList<>();
                if (result != null) {
                    // Для тестов
                    /*
                    try {
                        result = Utils.getStringFromFile(new File(Utils.assetDir, "gson.txt"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    */

                    AttributesFull[] all = null;
                    try {
                        all = gson.fromJson(result, AttributesFull[].class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Rollbar.instance().error(e, result);
                    }

                    if (all != null)
                        for (AttributesFull a : all) {
                            Device d = new Device();
                            if (a.value != null)
                                d.setValue(a.value.value);
                            if (a.uiType != null && a.uiType.endpointType != null)
                                switch (a.uiType.endpointType) {
                                    case "actuator.onoff":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        break;
                                    case "meter.temperature":
                                        d.addCapability(Capability.measure_temperature, d.getValue());
                                        break;
                                    case "meter.light":
                                        d.addCapability(Capability.measure_light, d.getValue());
                                        break;
                                    default:
                                        continue;
                                }
                            else if (a.definition != null && a.definition.cluster != null)
                                switch (a.definition.cluster) {
                                    case "com.zipato.cluster.OnOff":
                                        d.addCapability(Capability.onoff, d.getValue() == null || d.getValue().equals("false") || d.getValue().equals("0") ? "0" : "1");
                                        break;
                                    case "com.zipato.cluster.MultiLvlSensor":
                                        if (a.name != null)
                                            if (a.name.toLowerCase().contains("temperature"))
                                                d.addCapability(Capability.measure_temperature, d.getValue());
                                            else if (a.name.toLowerCase().contains("luminance"))
                                                d.addCapability(Capability.measure_temperature, d.getValue());
                                        break;
                                    case "com.zipato.cluster.Gauge":
                                        if (a.name != null && a.name.toLowerCase().contains("temperature"))
                                            d.addCapability(Capability.measure_temperature, d.getValue());
                                    default:
                                        continue;
                                }

                            d.setId(a.uuid);
                            d.setName(AI.replaceTrash(a.name));
                            if (a.room != null) {
                                a.room.setName(AI.replaceTrash(a.room.getName()));
                                rooms.put(a.room.id, a.room);
                                d.setRoomName(a.room.getName());
                            } else d.setRoomName("");

                            d.ai_name = d.getRoomName() + " " + d.getName();

                            if (d.getCapabilities().size() > 0)
                                devices.add(d);
                        }
                }
                all_rooms = new Room[rooms.size()];
                int i = 0;
                for (Room r : rooms.values())
                    all_rooms[i++] = r;
                all_devices = new Device[devices.size()];
                i = 0;
                for (Device d : devices)
                    all_devices[i++] = d;
            } catch (Exception e) {
                e.printStackTrace();
                Rollbar.instance().error(e);
            }

        if (all_rooms == null)
            all_rooms = new Room[0];
        if (all_devices == null)
            all_devices = new Device[0];
        if (all_scenes == null)
            all_scenes = new UScene[0];
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
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": true}");
    }

    @Override
    public void turnDeviceOff(UDevice d) {
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": false}");
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
        sendJSON("/zipato-web/v2/attributes/" + d.getId() + "/value", "{\"value\": " + level + "}");
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