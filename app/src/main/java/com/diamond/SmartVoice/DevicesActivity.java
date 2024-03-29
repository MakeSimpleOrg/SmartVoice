package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

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
        for (Controller controller : MainActivity.controllers)
            if(controller.getVisibleDevicesCount() > 0)
                list(controller);
    }

    private void list(Controller controller) {
        PreferenceScreen deviceList = getPreferenceScreen();
        Context context = deviceList.getContext();

        PreferenceCategory controllerCat = new PreferenceCategory(context);
        controllerCat.setTitle(controller.getName());
        deviceList.addPreference(controllerCat);

        if (controller.getVisibleRoomsCount() > 0 && controller.getVisibleDevicesCount() > 0) {
            for (URoom r : controller.getRooms())
                if (r.isVisible()) {
                    PreferenceScreen room = getPreferenceManager().createPreferenceScreen(context);
                    room.setTitle(r.getName());
                    deviceList.addPreference(room);

                    TreeMap<String, UDevice> tree = new TreeMap<String, UDevice>();
                    for (UDevice d : controller.getDevices())
                        tree.put(d.getName(), d);

                    for (UDevice d : tree.values())
                        if (d.getAiName() != null && d.getRoomName() != null && d.getRoomName().equalsIgnoreCase(r.getName()) && d.isVisible()) {

                            PreferenceScreen device = getPreferenceManager().createPreferenceScreen(context);
                            device.setTitle(d.getName());
                            device.setSummary(d.getAiName());
                            room.addPreference(device);

                            Preference pref = new CheckBoxPreference(context);
                            pref.setKey("device_enabled_" + d.getId());
                            pref.setDefaultValue(Boolean.TRUE);
                            pref.setTitle(getString(R.string.DeviceEnabled));
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.ActivateDevice));
                            pref.setSummary(d.getAiName());
                            pref.setOnPreferenceClickListener(activateListener);
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.VoiceCommand));
                            pref.setSummary(d.getAiName());
                            device.addPreference(pref);

                            pref = new EditTextPreference(context);
                            pref.setKey("device_alias_" + d.getId());
                            pref.setTitle(getString(R.string.Alias));
                            String alias = PreferenceManager.getDefaultSharedPreferences(this).getString(pref.getKey(), null);
                            if (alias == null)
                                alias = "";
                            pref.setSummary(alias);
                            pref.setOnPreferenceChangeListener(changeRefreshListener);
                            device.addPreference(pref);

                            pref = new Preference(context);
                            pref.setTitle(getString(R.string.DeviceName));
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
                        }
                }
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private Preference.OnPreferenceClickListener activateListener = new Preference.OnPreferenceClickListener() {
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

    private Preference.OnPreferenceChangeListener changeRefreshListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            preference.setSummary(value.toString());
            preference.getEditor().apply();
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
