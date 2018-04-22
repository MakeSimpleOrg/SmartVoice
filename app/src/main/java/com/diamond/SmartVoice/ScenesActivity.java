package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Dmitriy Ponomarev
 */
public class ScenesActivity extends PreferenceActivity {

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
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle(controllerName);
        preferenceScreen.addPreference(preferenceCategory);

        if (controller.getVisibleScenesCount() > 0) {
            for (UScene s : controller.getScenes())
                if (s.isVisible() && s.getName() != null) {
                    Preference preference = new Preference(preferenceScreen.getContext());
                    preference.setTitle(s.getName());
                    if (s.getRoomName() != null)
                        preference.setSummary(s.getRoomName());
                    preferenceCategory.addPreference(preference);
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
