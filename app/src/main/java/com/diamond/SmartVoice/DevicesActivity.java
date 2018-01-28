package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;

import java.util.Arrays;
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
        if (mainActivity.HomeyController != null) list("Homey", mainActivity.HomeyController);
        if (mainActivity.FibaroController != null) list("Fibaro", mainActivity.FibaroController);
        if (mainActivity.VeraController != null) list("Vera", mainActivity.VeraController);
    }

    private void list(String controllerName, Controller controller) {
        PreferenceScreen preferenceScreen = this.getPreferenceScreen();
        preferenceScreen.removeAll();

        PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle(controllerName);
        preferenceScreen.addPreference(preferenceCategory);

        if (controller.getVisibleRoomsCount() > 0 && controller.getVisibleDevicesCount() > 0) {
            preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
            preferenceCategory.setTitle("Устройства");
            preferenceScreen.addPreference(preferenceCategory);

            for (URoom r : controller.getRooms())
                for (UDevice d : controller.getDevices())
                    if (d.ai_name != null && d.getRoomName() != null && d.getRoomName().equals(r.getName()) && d.isVisible()) {
                        Preference preference = new Preference(preferenceScreen.getContext());
                        preference.setTitle(d.ai_name);
                        preference.setSummary(d.getId() + "\n" + d.getCapabilities());
                        preferenceCategory.addPreference(preference);
                        preference.setOnPreferenceClickListener(sBindPreferenceChangeListener);
                    }
        }

        if (controller.getVisibleScenesCount() > 0) {
            preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
            preferenceCategory.setTitle("Сцены");
            preferenceScreen.addPreference(preferenceCategory);

            for (UScene s : controller.getScenes())
                if (s.isVisible() && s.getName() != null) {
                    Preference preference = new Preference(preferenceScreen.getContext());
                    preference.setTitle(s.getName());
                    if(s.getRoomName() != null)
                        preference.setSummary(s.getRoomName());
                    preferenceCategory.addPreference(preference);
                }
        }

        /*
        if (controller.getVisibleRoomsCount() > 0) {
            preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
            preferenceCategory.setTitle("Комнаты");
            preferenceScreen.addPreference(preferenceCategory);

            for (URoom u : controller.getRooms())
                if (u.isVisible()) {
                    CheckBoxPreference preference = new CheckBoxPreference(preferenceScreen.getContext());
                    preference.setTitle(u.getName());
                    preferenceCategory.addPreference(preference);
                    preference.setChecked(true); // TODO
                }
        }
        */
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private Preference.OnPreferenceClickListener sBindPreferenceChangeListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String name = preference.getTitle().toString();
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
