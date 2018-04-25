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
        PreferenceScreen sceneList = getPreferenceScreen();
        Context context = sceneList.getContext();

        PreferenceCategory controllerCat = new PreferenceCategory(sceneList.getContext());
        controllerCat.setTitle(controllerName);
        sceneList.addPreference(controllerCat);

        if (controller.getVisibleScenesCount() > 0) {
            for (UScene s : controller.getScenes())
                if (s.isVisible() && s.getName() != null) {
                    PreferenceScreen scene = getPreferenceManager().createPreferenceScreen(context);
                    scene.setTitle(s.getName());
                    scene.setSummary(s.getRoomName());
                    sceneList.addPreference(scene);

                    Preference pref = new CheckBoxPreference(context);
                    pref.setKey("scene_enabled_" + s.getId());
                    pref.setDefaultValue(Boolean.TRUE);
                    pref.setTitle(R.string.SceneEnabled);
                    scene.addPreference(pref);

                    pref = new Preference(context);
                    pref.setTitle(getString(R.string.VoiceCommand));
                    pref.setSummary(s.ai_name);
                    scene.addPreference(pref);

                    pref = new EditTextPreference(context);
                    pref.setKey("scene_alias_" + s.getId());
                    pref.setTitle(getString(R.string.Alias));
                    String alias = PreferenceManager.getDefaultSharedPreferences(this).getString(pref.getKey(), null);
                    if (alias == null)
                        alias = "";
                    pref.setSummary(alias);
                    pref.setOnPreferenceChangeListener(changeRefreshListener);
                    scene.addPreference(pref);

                    pref = new Preference(context);
                    pref.setTitle(getString(R.string.OriginalName));
                    pref.setSummary(s.getName());
                    scene.addPreference(pref);

                    pref = new Preference(context);
                    pref.setTitle(getString(R.string.RoomName));
                    pref.setSummary(s.getRoomName());
                    scene.addPreference(pref);

                    pref = new Preference(context);
                    pref.setTitle("Id");
                    pref.setSummary(s.getId());
                    scene.addPreference(pref);

                    pref = new Preference(context);
                    pref.setTitle(getString(R.string.RunScene));
                    pref.setSummary(s.ai_name);
                    pref.setOnPreferenceClickListener(sBindPreferenceChangeListener);
                    scene.addPreference(pref);
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
            }, 500);
            return true;
        }
    };
}
