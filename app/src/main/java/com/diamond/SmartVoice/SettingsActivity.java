package com.diamond.SmartVoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
public class SettingsActivity extends PreferenceActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;

    @SuppressWarnings("deprecation")
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
        bindPreferenceSummaryToValue("keyRecognizerType");
        bindPreferenceSummaryToValue("voiceRecognizerType");
        bindPreferenceSummaryToValue("SnowboyKeyPhrase");
        bindPreferenceSummaryToValue("SnowboySensitivity");
        bindPreferenceSummaryToValue("PocketSphinxKeyPhrase");
        bindPreferenceSummaryToValue("PocketSphinxSensitivity");
        bindPreferenceSummaryToValue("polling");

        findPreference("homey_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("fibaro_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("vera_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("zipato_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("tts_enabled").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("keyRecognizerType").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("PocketSphinxKeyPhrase").setOnPreferenceChangeListener(sBindPreferenceChangeListener);
        findPreference("offline_recognition").setOnPreferenceChangeListener(sBindPreferenceChangeListener);

        process_keyRecognizerType(findPreference("keyRecognizerType"), mainActivity.pref.getString("keyRecognizerType", "None"));
    }

    private Preference.OnPreferenceChangeListener sBindPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            SharedPreferences pref = preference.getSharedPreferences();

            if ((preference.getKey().equals("fibaro_server_ip") || preference.getKey().equals("vera_server_ip") || preference.getKey().equals("zipato_server_ip") || preference.getKey().equals("homey_server_ip")) && !Patterns.WEB_URL.matcher(value.toString()).matches()) {
                Toast.makeText(getApplicationContext(), R.string.IncorrectUrl, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (preference.getKey().equals("fibaro_enabled") && (Boolean) value && !pref.getString("fibaro_server_ip", "").isEmpty() && !pref.getString("fibaro_server_login", "").isEmpty() && !pref.getString("fibaro_server_password", "").isEmpty())
                MainActivity.setupFibaro(mainActivity);
            else if (preference.getKey().equals("vera_enabled") && (Boolean) value && !pref.getString("vera_server_ip", "").isEmpty())
                MainActivity.setupVera(mainActivity);
            if (preference.getKey().equals("zipato_enabled") && (Boolean) value && !pref.getString("zipato_server_ip", "").isEmpty() && !pref.getString("zipato_server_login", "my.zipato.com:443").isEmpty() && !pref.getString("zipato_server_password", "").isEmpty())
                MainActivity.setupZipato(mainActivity);
            else if (preference.getKey().equals("tts_enabled") && (Boolean) value)
                mainActivity.setupTTS();
            else if (preference.getKey().equals("PocketSphinxKeyPhrase")) {
                mainActivity.PocketSphinxKeyPhrase = value.toString();
                mainActivity.setupKeyphraseRecognizer();
                setSummary(preference, value.toString());
            } else if (preference.getKey().equals("offline_recognition")) {
                mainActivity.offline_recognition = (Boolean) value;
                mainActivity.setupGoogleRecognizer();
            } else if ((preference.getKey().equals("homey_enabled") && (Boolean) value)) {
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
            } else if (preference.getKey().equals("keyRecognizerType")) {
                process_keyRecognizerType(preference, value.toString());
                setSummary(preference, value.toString());
            }

            return true;
        }
    };

    private void process_keyRecognizerType(Preference preference, String value) {
        if (preference.getKey().equals("keyRecognizerType"))
            switch (value) {
                case "None":
                    findPreference("SnowboyKeyPhrase").setEnabled(false);
                    findPreference("SnowboySensitivity").setEnabled(false);
                    findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                    findPreference("PocketSphinxSensitivity").setEnabled(false);
                    break;
                case "Snowboy":
                    findPreference("SnowboyKeyPhrase").setEnabled(true);
                    findPreference("SnowboySensitivity").setEnabled(true);
                    findPreference("PocketSphinxKeyPhrase").setEnabled(false);
                    findPreference("PocketSphinxSensitivity").setEnabled(false);
                    break;
                case "PocketSphinx":
                    findPreference("SnowboyKeyPhrase").setEnabled(false);
                    findPreference("SnowboySensitivity").setEnabled(false);
                    findPreference("PocketSphinxKeyPhrase").setEnabled(true);
                    findPreference("PocketSphinxSensitivity").setEnabled(true);
                    break;
            }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            setSummary(preference, value.toString());
            return true;
        }
    };

    private void bindPreferenceSummaryToValue(String str) {
        Preference preference = findPreference(str);
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        setSummary(preference, PreferenceManager.getDefaultSharedPreferences(this).getString(preference.getKey(), ""));
    }

    private static void setSummary(Preference preference, String summary) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(summary);
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else
            preference.setSummary(summary);
        preference.getEditor().apply();
        //System.out.println(preference.getKey() + " summary: " + summary);
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }
}