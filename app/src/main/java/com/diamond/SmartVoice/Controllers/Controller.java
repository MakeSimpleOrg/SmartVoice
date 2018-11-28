package com.diamond.SmartVoice.Controllers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.diamond.SmartVoice.AI;
import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


/**
 * @author Dmitriy Ponomarev
 */
public abstract class Controller {
    private static final String TAG = Controller.class.getSimpleName();

    protected MainActivity mainActivity;

    protected String name;
    protected String host;
    protected String host_ext;
    protected String auth;
    protected boolean clearNames;

    public String getHost() {
        String address = mainActivity.wifi() ? host : host_ext;
        if (address == null || address.isEmpty())
            address = host_ext;
        if (address == null || address.isEmpty())
            address = host;
        if (address == null || address.isEmpty())
            return "";
        if(this instanceof HttpController) {
            if (!address.contains("http:") && !address.contains("https:"))
                address = "http://" + address;
        }
        else if(this instanceof MQTTController) {
            address = address.replace("http:", "tcp:");
            address = address.replace("https:", "ssl:");
            if (!address.contains("tcp:") && !address.contains("ssl:"))
                address = "tcp://" + address;
        }
        return address;
    }

    URL getURL(String request) throws MalformedURLException {
        String address = getHost();
        if (address.isEmpty())
            throw new MalformedURLException("no host");
        return new URL(address + request);
    }

    public abstract void onSharedPreferenceChanged(SharedPreferences pref, String key);

    public abstract void loadData();

    public abstract void updateData();

    public abstract URoom[] getRooms();

    public abstract UDevice[] getDevices();

    public abstract UScene[] getScenes();

    public String getName() {
        return name;
    }

    public boolean isLoaded() {
        return getVisibleRoomsCount() > 0 || getVisibleDevicesCount() > 0 || getVisibleScenesCount() > 0;
    }

    public boolean isEnabled() {
        if (mainActivity == null)
            return false;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        if (pref == null)
            return false;
        return pref.getBoolean(name.toLowerCase() + "_enabled", false);
    }

    public UDevice getDevice(String deviceId) {
        for (UDevice d : getDevices())
            if (deviceId.equals(d.getId()))
                return d;
        return null;
    }

    public UScene getScene(String sceneId) {
        for (UScene s : getScenes())
            if (sceneId.equals(s.getId()))
                return s;
        return null;
    }

    public int getVisibleRoomsCount() {
        int c = 0;
        if (getRooms() != null)
            for (URoom u : getRooms())
                if (u.isVisible())
                    c++;
        return c;
    }

    public int getVisibleDevicesCount() {
        int c = 0;
        if (getDevices() != null)
            for (UDevice u : getDevices()) {
                if (u.isVisible())
                    c++;
            }
        return c;
    }

    public int getVisibleScenesCount() {
        int c = 0;
        if (getScenes() != null)
            for (UScene u : getScenes())
                if (u.isVisible())
                    c++;
        return c;
    }

    public abstract void turnDeviceOn(UDevice d);

    public abstract void turnDeviceOff(UDevice d);

    public abstract void closeLock(UDevice d);

    public abstract void openLock(UDevice d);

    public abstract void closeWindow(UDevice d);

    public abstract void openWindow(UDevice d);

    public abstract void setDimLevel(UDevice d, String level);

    public abstract void setColor(UDevice d, int r, int g, int b, int w);

    public abstract void setMode(UDevice d, String mode);

    public abstract void setTargetTemperature(UDevice d, String level);

    public abstract void runScene(UScene s);

    public String process(String[] requests, SharedPreferences pref) {
        UDevice[] devices = AI.getDevices(getDevices(), requests, pref);
        if (devices != null) {
            mainActivity.show(mainActivity.getString(R.string.Recognized) + ": " + devices[0].getAiName());
            return processDevices(devices);
        }
        UScene[] scenes = AI.getScenes(getScenes(), requests, pref);
        if (scenes != null) {
            mainActivity.show(mainActivity.getString(R.string.Recognized) + ": " + scenes[0].ai_name);
            return processScenes(scenes);
        }
        return null;
    }

    private String processDevices(UDevice[] devices) {
        boolean enabled = false;
        boolean finded = false;
        String text = mainActivity.getString(R.string.error);
        ArrayList<UDevice> list = new ArrayList<>();
        for (UDevice u : devices) {
            Log.d(TAG, mainActivity.getString(R.string.Recognized) + ": " + u.getId() + " " + u.getAiName());
            if (u.getCapabilities() != null) {
                String onoff = u.getCapabilities().get(Capability.onoff);
                String openclose = u.getCapabilities().get(Capability.openclose);
                String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
                Log.d(TAG, "onoff: " + onoff);
                if (u.ai_flag == 3) {
                    setDimLevel(u, u.ai_value);
                    u.getCapabilities().put(Capability.dim, u.ai_value);
                    text = u.ai_value + " " + mainActivity.getString(R.string.percent);
                } else if (onoff != null || openclose != null || windowcoverings_state != null) {
                    list.add(u);
                    if (!finded) {
                        finded = true;
                        enabled = u.ai_flag == 1 || u.ai_flag == 0 && (onoff == null || onoff.isEmpty() || "0".equals(onoff) || "open".equals(openclose) || "up".equals(windowcoverings_state));
                    }
                } else {
                    String measure_temperature = u.getCapabilities().get(Capability.measure_temperature);
                    if (measure_temperature != null)
                        return measure_temperature;
                    String measure_humidity = u.getCapabilities().get(Capability.measure_humidity);
                    if (measure_humidity != null)
                        return measure_humidity;
                    String measure_light = u.getCapabilities().get(Capability.measure_light);
                    if (measure_light != null)
                        return measure_light;
                    String measure_co2 = u.getCapabilities().get(Capability.measure_co2);
                    if (measure_co2 != null)
                        return measure_co2;
                    String measure_pressure = u.getCapabilities().get(Capability.measure_pressure);
                    if (measure_pressure != null)
                        return measure_pressure;
                    String measure_noise = u.getCapabilities().get(Capability.measure_noise);
                    if (measure_noise != null)
                        return measure_noise;
                    String measure_generic = u.getCapabilities().get(Capability.measure_generic);
                    if (measure_generic != null)
                        return measure_generic;
                }
            } else
                Log.w(TAG, "u.getCapabilities() == null: " + u.getId() + u.getAiName());
        }

        for (UDevice u : list) {
            String onoff = u.getCapabilities().get(Capability.onoff);
            String openclose = u.getCapabilities().get(Capability.openclose);
            String windowcoverings_state = u.getCapabilities().get(Capability.windowcoverings_state);
            String dim = u.getCapabilities().get(Capability.dim);
            if (enabled) {
                if (onoff != null) {
                    turnDeviceOn(u);
                    u.getCapabilities().put(Capability.onoff, "1");
                    text = mainActivity.getString(R.string.switched_on);
                }
                if (openclose != null) {
                    closeLock(u);
                    u.getCapabilities().put(Capability.openclose, "close");
                    text = mainActivity.getString(R.string.closed);
                }
                if (windowcoverings_state != null) {
                    closeWindow(u);
                    u.getCapabilities().put(Capability.windowcoverings_state, "down");
                    text = mainActivity.getString(R.string.closed);
                }
                if (dim != null) {
                    setDimLevel(u, "100");
                    u.getCapabilities().put(Capability.dim, "100");
                }
            } else {
                if (onoff != null) {
                    turnDeviceOff(u);
                    u.getCapabilities().put(Capability.onoff, "0");
                    text = mainActivity.getString(R.string.switched_off);
                }
                if (openclose != null) {
                    openLock(u);
                    u.getCapabilities().put(Capability.openclose, "open");
                    text = mainActivity.getString(R.string.open);
                }
                if (windowcoverings_state != null) {
                    openWindow(u);
                    u.getCapabilities().put(Capability.windowcoverings_state, "up");
                    text = mainActivity.getString(R.string.open);
                }
            }
        }
        return text;
    }

    private String processScenes(UScene[] scenes) {
        for (UScene s : scenes)
            runScene(s);
        return mainActivity.getString(R.string.scene_launched);
    }
}