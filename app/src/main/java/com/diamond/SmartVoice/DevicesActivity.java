package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;

import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Dmitriy Ponomarev
 */
public class DevicesActivity extends PreferenceActivity {

    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_devices);
        reload();
    }

    private void reload() {
        getPreferenceScreen().removeAll();
        if (mainActivity.HomeyController != null) list("Homey", mainActivity.HomeyController);
        if (mainActivity.FibaroController != null) list("Fibaro", mainActivity.FibaroController);
        if (mainActivity.VeraController != null) list("Vera", mainActivity.VeraController);
        if (mainActivity.ZipatoController != null) list("Zipato", mainActivity.ZipatoController);
    }

    private void list(String controllerName, Controller controller) {
        PreferenceScreen deviceList = getPreferenceScreen();
        Context context = deviceList.getContext();

        PreferenceCategory controllerCat = new PreferenceCategory(context);
        controllerCat.setTitle(controllerName);
        deviceList.addPreference(controllerCat);

        if (controller.getVisibleRoomsCount() > 0 && controller.getVisibleDevicesCount() > 0) {
            for (URoom r : controller.getRooms())
                if (r.isVisible()) {
                    PreferenceScreen room = getPreferenceManager().createPreferenceScreen(context);
                    room.setTitle(r.getName());
                    deviceList.addPreference(room);

                    for (UDevice d : controller.getDevices())
                        if (d.ai_name != null && d.getRoomName() != null && d.getRoomName().equalsIgnoreCase(r.getName()) && d.isVisible()) {

                            PreferenceScreen device = getPreferenceManager().createPreferenceScreen(context);
                            device.setTitle(d.getName());
                            device.setSummary(d.ai_name);
                            room.addPreference(device);

                            Preference pref = new CheckBoxPreference(context);
                            pref.setKey("device_enabled_" + d.getId());
                            pref.setDefaultValue(Boolean.TRUE);
                            pref.setTitle(getString(R.string.DeviceEnabled));
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.Alias));
                            pref.setSummary(d.ai_name);
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.OriginalName));
                            pref.setSummary(d.getName());
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.RoomName));
                            pref.setSummary(d.getRoomName());
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle("Id");
                            pref.setSummary(d.getId());
                            device.addPreference(pref);

                            for (Map.Entry<Capability, String> entry : d.getCapabilities().entrySet()) {
                                pref = new Preference(context);
                                pref.setTitle(entry.getKey().toString());
                                pref.setSummary(entry.getValue());
                                device.addPreference(pref);
                            }

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.Activate));
                            pref.setSummary(d.ai_name);
                            pref.setOnPreferenceClickListener(sBindPreferenceChangeListener);
                            device.addPreference(pref);
                        }
                }
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private Preference.OnPreferenceClickListener sBindPreferenceChangeListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String name = preference.getSummary().toString();
            MainActivity.process(new String[]{name}, mainActivity);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    reload();
                }
            }, 1000);

            return true;
        }
    };
}
