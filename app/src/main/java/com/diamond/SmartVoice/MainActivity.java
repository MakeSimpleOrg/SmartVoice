package com.diamond.SmartVoice;

import java.util.Locale;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.diamond.SmartVoice.Fibaro.Device;
import com.diamond.SmartVoice.Fibaro.FibaroConnector;
import com.diamond.SmartVoice.Fibaro.Scene;
import com.diamond.SmartVoice.Recognizer.GoogleRecognizer;
import com.diamond.SmartVoice.Recognizer.PocketSphinxRecognizer;

public class MainActivity extends Activity {
    private String LOG_TAG = "MainActivity";

    private PocketSphinxRecognizer keyPhraseRecognizer;
    private GoogleRecognizer recognizer;

    private TextToSpeech textToSpeech;
    private View MicView;
    public SharedPreferences pref;
    public FibaroConnector controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.INTERNET
                }, 1);

        MicView = findViewById(R.id.mic);
        MicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        keyPhraseRecognizer.stopListening();
                        recognizer.startListening();
                        buttonOn();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;
            }
        });

        Button settingsBtn = (Button) findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsActivity);
            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR)
                    return;
                if (textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE)
                    textToSpeech.setLanguage(Locale.getDefault());
                speak("Слушаю", false);
            }
        });

        keyPhraseRecognizer = new PocketSphinxRecognizer(this);
        recognizer = new GoogleRecognizer(this);

        setupController();
    }

    public void buttonOn() {
        MicView.setBackgroundResource(R.drawable.background_big_mic_green);
    }

    public void buttonOff() {
        MicView.setBackgroundResource(R.drawable.background_big_mic);
        keyPhraseRecognizer.startListening();
    }

    private void setupController() {
        new AsyncTask<Void, Void, FibaroConnector>() {
            @Override
            protected FibaroConnector doInBackground(Void... params) {
                FibaroConnector controller = new FibaroConnector(pref.getString("server_ip", ""), pref.getString("server_login", ""), pref.getString("server_password", ""));
                controller.getDevices();
                controller.getScenes();
                return controller.getLastDevicesCount() > 0 || controller.getLastScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(FibaroConnector controller) {
                MainActivity.this.controller = controller;
                if (MainActivity.this.controller == null)
                    Toast.makeText(MainActivity.this, "Контроллер не найден! IP: " + pref.getString("server_ip", ""), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this, "Найдено " + controller.getLastRoomsCount() + " комнат, " + controller.getLastDevicesCount() + " устройств и " + controller.getLastScenesCount() + " сцен", Toast.LENGTH_LONG).show();
                keyPhraseRecognizer.startListening();
            }
        }.execute();
    }

    public long lastKeyPhrase;

    public void OnKeyPhrase() {
        lastKeyPhrase = System.currentTimeMillis();
        keyPhraseRecognizer.stopListening();
        recognizer.startListening();
        buttonOn();
    }

    public void process(final String[] variants) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                Device[] devices = controller.getDevices(params);
                if (devices.length != 0)
                    return controller.process(devices);
                Scene[] scenes = controller.getScenes(params);
                if (scenes.length != 0)
                    return controller.process(scenes);
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null)
                    speak(result);
                else
                    speak("Повтори!");
                buttonOff();
            }
        }.execute(variants);
    }

    public void speak(String text)
    {
        speak(text, true);
    }

    public void speak(String text, boolean screen) {
        if(screen)
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params, UUID.randomUUID().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (keyPhraseRecognizer != null)
            keyPhraseRecognizer.destroy();
        if (textToSpeech != null)
            textToSpeech.shutdown();
        Log.w(LOG_TAG, "onDestroy");
        super.onDestroy();
    }
}