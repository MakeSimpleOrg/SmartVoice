package com.diamond.SmartVoice;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
    public static MainActivity mainActivity;

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        bindPreferenceSummaryToValue(findPreference("fibaro_server_ip"));
        bindPreferenceSummaryToValue(findPreference("fibaro_server_login"));
        bindPreferenceSummaryToValue(findPreference("vera_server_ip"));
        bindPreferenceSummaryToValue(findPreference("keyphrase"));
        bindPreferenceSummaryToValue(findPreference("keywordThreshold"));

        findPreference("fibaro_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("vera_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("tts_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("keyphrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("offline_recognition").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
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
            if (preference.getKey().equals("fibaro_enabled") && (Boolean) value)
                mainActivity.setupFibaro();
            else if (preference.getKey().equals("vera_enabled") && (Boolean) value)
                mainActivity.setupVera();
            else if (preference.getKey().equals("tts_enabled") && (Boolean) value)
                mainActivity.setupTTS();
            else if (preference.getKey().equals("keyphrase")) {
                mainActivity.keyphrase = value.toString();
                mainActivity.setupKeyphraseRecognizer();
            } else if (preference.getKey().equals("offline_recognition")) {
                mainActivity.offline_recognition = (Boolean) value;
                mainActivity.setupRecognizer();
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }
}
