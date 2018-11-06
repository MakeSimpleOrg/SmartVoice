package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Patterns;
import android.widget.Toast;

import com.diamond.SmartVoice.OAuth.WebViewActivity;

import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.common.exception.OAuthSystemException;

/**
 * @author Dmitriy Ponomarev
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        bindPreferenceSummaryToValue("homey_server_ip");
        bindPreferenceSummaryToValue("homey_bearer");
        bindPreferenceSummaryToValue("fibaro_server_ip");
        bindPreferenceSummaryToValue("fibaro_server_login");
        bindPreferenceSummaryToValue("vera_server_ip");
        bindPreferenceSummaryToValue("zipato_server_ip");
        bindPreferenceSummaryToValue("zipato_server_login");

        bindPreferenceSummaryToValue("vocalizerType");
        bindPreferenceSummaryToValue("keyRecognizerType");
        bindPreferenceSummaryToValue("voiceRecognizerType");

        bindPreferenceSummaryToValue("YandexKeyPhrase");
        bindPreferenceSummaryToValue("YandexVoice");
        bindPreferenceSummaryToValue("YandexEmotion");
        bindPreferenceSummaryToValue("YandexSpeechLang");
        bindPreferenceSummaryToValue("YandexRecognizerLang");

        bindPreferenceSummaryToValue("SnowboyKeyPhrase");
        bindPreferenceSummaryToValue("SnowboySensitivity");

        bindPreferenceSummaryToValue("PocketSphinxKeyPhrase");
        bindPreferenceSummaryToValue("PocketSphinxSensitivity");

        bindPreferenceSummaryToValue("GoogleKeyPhrase");

        bindPreferenceSummaryToValue("polling");

        findPreference("homey_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("fibaro_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("vera_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("zipato_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        findPreference("offline_recognition").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("vocalizerType").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("keyRecognizerType").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("voiceRecognizerType").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        findPreference("YandexKeyPhrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("YandexVoice").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("YandexEmotion").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("YandexSpeechLang").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("YandexRecognizerLang").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        findPreference("PocketSphinxKeyPhrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("PocketSphinxSensitivity").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        findPreference("SnowboyKeyPhrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("SnowboySensitivity").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        findPreference("GoogleKeyPhrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        process_vocalizerType(mainActivity.pref.getString("vocalizerType", "None"));
        process_keyRecognizerType(mainActivity.pref.getString("keyRecognizerType", "None"));
        process_voiceRecognizerType(mainActivity.pref.getString("voiceRecognizerType", "Google"));
    }

    private Preference.OnPreferenceChangeListener sBindPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            SharedPreferences pref = preference.getSharedPreferences();

            apply(preference, value.toString());

            String key = preference.getKey();

            if ((key.equals("fibaro_server_ip") || key.equals("vera_server_ip") || key.equals("zipato_server_ip") || key.equals("homey_server_ip")) && !Patterns.WEB_URL.matcher(value.toString()).matches()) {
                Toast.makeText(getApplicationContext(), R.string.IncorrectUrl, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (key.equals("fibaro_enabled") && (Boolean) value && !pref.getString("fibaro_server_ip", "").isEmpty() && !pref.getString("fibaro_server_login", "").isEmpty() && !pref.getString("fibaro_server_password", "").isEmpty())
                MainActivity.setupFibaro(mainActivity);

            if (key.equals("vera_enabled") && (Boolean) value && !pref.getString("vera_server_ip", "").isEmpty())
                MainActivity.setupVera(mainActivity);

            if (key.equals("zipato_enabled") && (Boolean) value && !pref.getString("zipato_server_ip", "").isEmpty() && !pref.getString("zipato_server_login", "my.zipato.com:443").isEmpty() && !pref.getString("zipato_server_password", "").isEmpty())
                MainActivity.setupZipato(mainActivity);

            if ((key.equals("homey_enabled") && (Boolean) value)) {
                if (!pref.getString("homey_server_ip", "").isEmpty() && !pref.getString("homey_bearer", "").isEmpty())
                    MainActivity.setupHomey(mainActivity);
                else if (pref.getString("homey_bearer", "").isEmpty()) {
                    OAuthClientRequest request = null;
                    try {
                        request = OAuthClientRequest
                                .authorizationLocation("https://accounts.athom.com/login")
                                .setClientId("5534df95588a5ed82aaef73d").setRedirectURI("https://my.athom.com/auth/callback")
                                .setResponseType("code")
                                .setParameter("origin", "https://accounts.athom.com/oauth2/authorise")
                                .buildQueryMessage();
                    } catch (OAuthSystemException e) {
                        e.printStackTrace();
                    }
                    if (request != null) {
                        WebViewActivity.mainActivity = mainActivity;
                        WebViewActivity.settingsActivity = SettingsActivity.this;
                        Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
                        intent.putExtra("url", request.getLocationUri());
                        startActivity(intent);
                    }
                }
            }

            if (key.equals("vocalizerType")) {
                process_vocalizerType(value.toString());
                setSummary(preference, value.toString());
                mainActivity.setupVocalizer();
            }

            if (key.equals("offline_recognition")) {
                mainActivity.offline_recognition = (Boolean) value;
                mainActivity.setupRecognizer();
            }

            if (key.equals("keyRecognizerType")) {
                process_keyRecognizerType(value.toString());
                setSummary(preference, value.toString());
                mainActivity.setupKeyphraseRecognizer();
            }

            if (key.equals("voiceRecognizerType")) {
                process_voiceRecognizerType(value.toString());
                setSummary(preference, value.toString());
                mainActivity.setupRecognizer();
            }

            if (key.equals("PocketSphinxKeyPhrase")) {
                mainActivity.PocketSphinxKeyPhrase = value.toString();
                setSummary(preference, value.toString());
                mainActivity.setupKeyphraseRecognizer();
            }

            if (key.equals("YandexKeyPhrase") || key.equals("PocketSphinxSensitivity") || key.equals("SnowboyKeyPhrase") || key.equals("SnowboySensitivity") || key.equals("GoogleKeyPhrase")) {
                setSummary(preference, value.toString());
                mainActivity.setupKeyphraseRecognizer();
            }

            if (key.equals("YandexVoice") || key.equals("YandexEmotion") || key.equals("YandexSpeechLang")) {
                setSummary(preference, value.toString());
                mainActivity.setupVocalizer();
            }

            if (key.equals("YandexRecognizerLang")) {
                setSummary(preference, value.toString());
                mainActivity.setupRecognizer();
            }

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
                break;
            case "Yandex":
                findPreference("YandexKeyPhrase").setEnabled(true);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                break;
            case "Snowboy":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(true);
                findPreference("SnowboySensitivity").setEnabled(true);
                findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                findPreference("PocketSphinxSensitivity").setEnabled(false);
                break;
            case "PocketSphinx":
                findPreference("YandexKeyPhrase").setEnabled(false);
                findPreference("SnowboyKeyPhrase").setEnabled(false);
                findPreference("SnowboySensitivity").setEnabled(false);
                findPreference("PocketSphinxKeyPhrase").setEnabled(true);
                findPreference("PocketSphinxSensitivity").setEnabled(true);
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

    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            System.out.println("setSummary: " + preference.getKey() + " to: " + value.toString());
            setSummary(preference, value.toString());
            return true;
        }
    };

    private void bindPreferenceSummaryToValue(String str) {
        Preference preference = findPreference(str);
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        setSummary(preference, PreferenceManager.getDefaultSharedPreferences(this).getString(preference.getKey(), ""));
    }

    private void setSummary(Preference preference, String summary) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(summary);
            CharSequence value = index >= 0 ? listPreference.getEntries()[index] : "";
            preference.setSummary(value);
        } else
            preference.setSummary(summary);
    }

    private void apply(Preference preference, String value) {
        if (preference instanceof EditTextPreference || preference instanceof ListPreference) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(preference.getKey(), value).apply();
            System.out.println("Changed: " + preference.getKey() + " to: " + value);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }
}