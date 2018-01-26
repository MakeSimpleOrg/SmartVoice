package com.diamond.SmartVoice;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.diamond.SmartVoice.Controllers.URoom;
import com.diamond.SmartVoice.Controllers.UScene;

/**
 * @author Dmitriy Ponomarev
 */
public class DevicesActivity extends PreferenceActivity {
    public static MainActivity mainActivity;

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_devices);

        if (mainActivity.FibaroController != null) load("Fibaro", mainActivity.FibaroController);

        if (mainActivity.VeraController != null) load("Vera", mainActivity.VeraController);

        // TODO bindPreferenceSummaryToValue(preference);
        // TODO  findPreference("fibaro_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
    }

    private void load(String controllerName, Controller controller) {
        PreferenceScreen preferenceScreen = this.getPreferenceScreen();
        PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle(controllerName);
        preferenceScreen.addPreference(preferenceCategory);

        preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle("Устройства");
        preferenceScreen.addPreference(preferenceCategory);

        for (UDevice u : controller.getDevices())
            if (u.isVisible()) {
                CheckBoxPreference preference = new CheckBoxPreference(preferenceScreen.getContext());
                preference.setTitle(u.getName());
                preference.setSummary(u.getStatus());
                preferenceCategory.addPreference(preference);
                preference.setChecked(true); // TODO
            }

        preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
        preferenceCategory.setTitle("Сцены");
        preferenceScreen.addPreference(preferenceCategory);

        for (UScene u : controller.getScenes())
            if (u.isVisible()) {
                CheckBoxPreference preference = new CheckBoxPreference(preferenceScreen.getContext());
                preference.setTitle(u.getName());
                preferenceCategory.addPreference(preference);
                preference.setChecked(true); // TODO
            }

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

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            preference.setSummary(stringValue);
            return true;
        }
    };

    private Preference.OnPreferenceChangeListener sBindPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }
}
