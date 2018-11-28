package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Patterns;
import android.widget.Toast;

/**
 * @author Dmitriy Ponomarev
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;

    protected SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);



        addPreferencesFromResource(R.xml.pref_general);

        bind("homeSSID");

        bind("homey_enabled");
        bind("homey_server_ip");
        bind("homey_server_ip_ext");
        bind("homey_bearer");

        bind("fibaro_enabled");
        bind("fibaro_server_ip");
        bind("fibaro_server_ip_ext");
        bind("fibaro_server_login");
        bind("fibaro_server_password");

        bind("vera_enabled");
        bind("vera_server_ip");
        bind("vera_server_ip_ext");

        bind("zipato_server_ip");
        bind("zipato_server_ip_ext");
        bind("zipato_server_login");
        bind("zipato_server_password");

        bind("offline_recognition");

        bind("vocalizerType");
        bind("keyRecognizerType");
        bind("voiceRecognizerType");

        bind("YandexKeyPhrase");
        bind("YandexVoice");
        bind("YandexEmotion");
        bind("YandexSpeechLang");
        bind("YandexRecognizerLang");

        bind("SnowboyKeyPhrase");
        bind("SnowboySensitivity");

        bind("PocketSphinxKeyPhrase");
        bind("PocketSphinxSensitivity");

        bind("GoogleKeyPhrase");

        bind("polling");

        process_vocalizerType(mainActivity.pref.getString("vocalizerType", "None"));
        process_keyRecognizerType(mainActivity.pref.getString("keyRecognizerType", "None"));
        process_voiceRecognizerType(mainActivity.pref.getString("voiceRecognizerType", "Google"));

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
                if (key.equals("homey_bearer")) {
                    Toast.makeText(getApplicationContext(), "Token updated", Toast.LENGTH_SHORT).show();
                    findPreference("homey_bearer").setSummary(pref.getString(key, ""));
                }
                else if (key.equals("homey_server_ip_ext"))
                    findPreference("homey_server_ip_ext").setSummary(pref.getString(key, ""));
            }
        };
        mainActivity.pref.registerOnSharedPreferenceChangeListener(listener);
    }

    private Preference.OnPreferenceChangeListener onChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();

            if (key.contains("server_ip") && !value.toString().isEmpty() && !Patterns.WEB_URL.matcher(value.toString()).matches()) {
                Toast.makeText(getApplicationContext(), R.string.IncorrectUrl, Toast.LENGTH_SHORT).show();
                return false;
            }

            apply(preference, value);
            setSummary(preference, value.toString());

            if (key.equals("vocalizerType")) {
                process_vocalizerType(value.toString());
                mainActivity.setupVocalizer();
            }

            if (key.equals("offline_recognition")) {
                mainActivity.offline_recognition = (Boolean) value;
                mainActivity.setupRecognizer();
            }

            if (key.equals("keyRecognizerType")) {
                process_keyRecognizerType(value.toString());
                mainActivity.setupKeyphraseRecognizer();
            }

            if (key.equals("voiceRecognizerType")) {
                process_voiceRecognizerType(value.toString());
                mainActivity.setupRecognizer();
            }

            if (key.equals("PocketSphinxKeyPhrase")) {
                mainActivity.PocketSphinxKeyPhrase = value.toString();
                mainActivity.setupKeyphraseRecognizer();
            }

            if (key.equals("YandexKeyPhrase") || key.equals("PocketSphinxSensitivity") || key.equals("SnowboyKeyPhrase") || key.equals("SnowboySensitivity") || key.equals("GoogleKeyPhrase"))
                mainActivity.setupKeyphraseRecognizer();


            if (key.equals("YandexVoice") || key.equals("YandexEmotion") || key.equals("YandexSpeechLang"))
                mainActivity.setupVocalizer();

            if (key.equals("YandexRecognizerLang"))
                mainActivity.setupRecognizer();

            return true;
        }
    };

    private void process_keyRecognizerType(String value) {
        switch (value) {
            case "None":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                findPreference("GoogleKeyPhrase").setEnabled(false);
                if(!"Google".equalsIgnoreCase(mainActivity.pref.getString("voiceRecognizerType", "Google")))
                    findPreference("offline_recognition").setEnabled(false);
                break;
            case "Yandex":
                findPreference("YandexKeyPhrase").setEnabled(true);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                findPreference("GoogleKeyPhrase").setEnabled(false);
                if(!"Google".equalsIgnoreCase(mainActivity.pref.getString("voiceRecognizerType", "Google")))
                    findPreference("offline_recognition").setEnabled(false);
                break;
            case "Snowboy":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(true);
                findPreference("SnowboySensitivity").setEnabled(true);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                findPreference("GoogleKeyPhrase").setEnabled(false);
                if(!"Google".equalsIgnoreCase(mainActivity.pref.getString("voiceRecognizerType", "Google")))
                    findPreference("offline_recognition").setEnabled(false);
                break;
            case "PocketSphinx":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(true);
                findPreference("PocketSphinxSensitivity").setEnabled(true);
                findPreference("GoogleKeyPhrase").setEnabled(false);
                if(!"Google".equalsIgnoreCase(mainActivity.pref.getString("voiceRecognizerType", "Google")))
                    findPreference("offline_recognition").setEnabled(false);
                break;
            case "Google":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                findPreference("GoogleKeyPhrase").setEnabled(true);
                findPreference("offline_recognition").setEnabled(true);
                break;
        }
    }

    private void process_voiceRecognizerType(String value) {
        switch (value) {
            case "Google":
                findPreference("offline_recognition").setEnabled(true);
                findPreference("YandexRecognizerLang").setEnabled(false);
                break;
            case "Yandex":
                if(!"Google".equalsIgnoreCase(mainActivity.pref.getString("keyRecognizerType", "None")))
                    findPreference("offline_recognition").setEnabled(false);
                findPreference("YandexRecognizerLang").setEnabled(true);
                break;
        }
    }

    private void process_vocalizerType(String value) {
        switch (value) {
            case "None":
            case "Google":
                findPreference("YandexVoice").setEnabled(false);
                findPreference("YandexEmotion").setEnabled(false);
                findPreference("YandexSpeechLang").setEnabled(false);
                break;
            case "Yandex":
                findPreference("YandexVoice").setEnabled(true);
                findPreference("YandexEmotion").setEnabled(true);
                findPreference("YandexSpeechLang").setEnabled(true);
                break;
        }
    }

    private void bind(String str) {
        Preference preference = findPreference(str);
        preference.setOnPreferenceChangeListener(onChangeListener);
        if (!(preference instanceof CheckBoxPreference))
            setSummary(preference, mainActivity.pref.getString(preference.getKey(), ""));
    }

    private void setSummary(Preference preference, String summary) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(summary);
            CharSequence value = index >= 0 ? listPreference.getEntries()[index] : "";
            preference.setSummary(value);
        } else if (!(preference instanceof CheckBoxPreference) && !preference.getKey().contains("password"))
            preference.setSummary(summary);
    }

    private void apply(Preference preference, Object value) {
        if (preference instanceof EditTextPreference || preference instanceof ListPreference) {
            mainActivity.pref.edit().putString(preference.getKey(), value.toString()).apply();
            System.out.println("Changed: " + preference.getKey() + " to: " + value);
        } else if (preference instanceof CheckBoxPreference)
            mainActivity.pref.edit().putBoolean(preference.getKey(), (Boolean) value).apply();
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }
}