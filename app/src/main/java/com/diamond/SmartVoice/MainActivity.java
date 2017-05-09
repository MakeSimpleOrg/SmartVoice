package com.diamond.SmartVoice;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.diamond.SmartVoice.Fibaro.FibaroConnector;
import com.diamond.SmartVoice.Recognizer.GoogleRecognizer;
import com.diamond.SmartVoice.Recognizer.PocketSphinxRecognizer;
import com.diamond.SmartVoice.Vera.VeraConnector;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private PocketSphinxRecognizer keyPhraseRecognizer;
    private GoogleRecognizer recognizer;

    private TextToSpeech textToSpeech;
    private View MicView;
    public SharedPreferences pref;
    public FibaroConnector FibaroController;
    public VeraConnector VeraController;

    private boolean isLoading = true;
    private boolean fibaroLoading = false;
    private boolean veraLoading = false;
    public boolean ttsLoading = false;
    private boolean keyPhraseRecognizerLoading = false;
    private boolean recognizerLoading = false;

    private View progressBar;

    public String keyphrase = "умный дом";
    public boolean offline_recognition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        keyphrase = pref.getString("keyphrase", "умный дом");
        offline_recognition = pref.getBoolean("offline_recognition", false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.INTERNET
                    }, 1);

        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        MicView = findViewById(R.id.mic);
        MicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLoading || ttsLoading || fibaroLoading || veraLoading)
                    return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (keyPhraseRecognizer != null)
                            keyPhraseRecognizer.stopListening();
                        if (recognizer != null)
                            recognizer.startListening();
                        buttonOn();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;
            }
        });

        SettingsActivity.mainActivity = this;

        Button settingsBtn = (Button) findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || fibaroLoading || veraLoading)
                    return;
                Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsActivity);
            }
        });

        progressBar.setVisibility(View.VISIBLE);

        Runnable load;
        Thread thread;

        if (pref.getBoolean("keyRecognizer", false)) {
            load = new LoadData1();
            thread = new Thread(load);
            thread.start();
        }

        load = new LoadData2();
        thread = new Thread(load);
        thread.start();

        if (pref.getBoolean("tts_enabled", false)) {
            load = new LoadData3();
            thread = new Thread(load);
            thread.start();
        }

        if (pref.getBoolean("fibaro_enabled", false)) {
            load = new LoadData4();
            thread = new Thread(load);
            thread.start();
        }

        if (pref.getBoolean("vera_enabled", false)) {
            load = new LoadData5();
            thread = new Thread(load);
            thread.start();
        }

        load = new LoadData6();
        thread = new Thread(load);
        thread.start();
    }

    class LoadData1 implements Runnable {
        @Override
        public void run() {
            setupKeyphraseRecognizer();
        }
    }

    class LoadData2 implements Runnable {
        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRecognizer();
                }
            });
        }
    }

    class LoadData3 implements Runnable {
        @Override
        public void run() {
            setupTTS();
        }
    }

    class LoadData4 implements Runnable {
        @Override
        public void run() {
            setupFibaro();
        }
    }

    class LoadData5 implements Runnable {
        @Override
        public void run() {
            setupVera();
        }
    }

    class LoadData6 implements Runnable {
        @Override
        public void run() {
            while (isLoading()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isLoading = false;

            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

            if (keyPhraseRecognizer != null)
                keyPhraseRecognizer.startListening();

            if (pref.getBoolean("tts_enabled", false))
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speak("Слушаю", false);
                    }
                });
        }
    }

    private boolean isLoading() {
        return ttsLoading || fibaroLoading || veraLoading || recognizerLoading || keyPhraseRecognizerLoading;
    }

    public void buttonOn() {
        MicView.setBackgroundResource(R.drawable.background_big_mic_green);
    }

    public void buttonOff() {
        MicView.setBackgroundResource(R.drawable.background_big_mic);
        if (keyPhraseRecognizer != null)
            keyPhraseRecognizer.startListening();
    }

    public void setupKeyphraseRecognizer() {
        keyPhraseRecognizerLoading = true;
        if (keyPhraseRecognizer != null) {
            keyPhraseRecognizer.destroy();
            keyPhraseRecognizer = null;
        }
        keyPhraseRecognizer = new PocketSphinxRecognizer(this);
        keyPhraseRecognizerLoading = false;
    }

    public void setupRecognizer() {
        recognizerLoading = true;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer = null;
        }
        recognizer = new GoogleRecognizer(this);
        recognizerLoading = false;
    }

    public void setupTTS() {
        ttsLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    ttsLoading = false;
                    return;
                }
                if (textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE)
                    textToSpeech.setLanguage(Locale.getDefault());
                ttsLoading = false;
                if (!isLoading())
                    progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setupFibaro() {
        fibaroLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, FibaroConnector>() {
            @Override
            protected FibaroConnector doInBackground(Void... params) {
                FibaroConnector controller = new FibaroConnector(pref.getString("fibaro_server_ip", ""), pref.getString("fibaro_server_login", ""), pref.getString("fibaro_server_password", ""));
                try {
                    controller.getDevices();
                    controller.getScenes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return controller.getLastDevicesCount() > 0 || controller.getLastScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(FibaroConnector controller) {
                FibaroController = controller;
                if (FibaroController == null)
                    show("Fibaro: контроллер не найден! IP: " + pref.getString("fibaro_server_ip", ""));
                else
                    show("Fibaro: Найдено " + controller.getLastRoomsCount() + " комнат, " + controller.getLastDevicesCount() + " устройств и " + controller.getLastScenesCount() + " сцен");
                fibaroLoading = false;
                if (!isLoading())
                    progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public void setupVera() {
        veraLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, VeraConnector>() {
            @Override
            protected VeraConnector doInBackground(Void... params) {
                VeraConnector controller = new VeraConnector(pref.getString("vera_server_ip", ""));
                try {
                    controller.getSdata();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return controller.getLastDevicesCount() > 0 || controller.getLastScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(VeraConnector controller) {
                VeraController = controller;
                if (VeraController == null) {
                    show("Vera: Контроллер не найден! IP: " + pref.getString("vera_server_ip", ""));
                } else {
                    show("Vera: Найдено " + controller.getLastRoomsCount() + " комнат, " + controller.getLastDevicesCount() + " устройств и " + controller.getLastScenesCount() + " сцен");
                }
                veraLoading = false;
                if (!isLoading())
                    progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public long lastKeyPhrase;

    public void OnKeyPhrase() {
        lastKeyPhrase = System.currentTimeMillis();
        if (keyPhraseRecognizer != null)
            keyPhraseRecognizer.stopListening();
        if (recognizer != null)
            recognizer.startListening();
        buttonOn();
    }

    public void process(final String[] variants) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                Log.w(TAG, "Вы сказали: " + Arrays.toString(params));
                if(!pref.getBoolean("fibaro_enabled", false) && !pref.getBoolean("vera_enabled", false) || FibaroController == null && VeraController == null)
                    return "Нечем управлять";
                String result = null;
                if(pref.getBoolean("fibaro_enabled", false) && FibaroController != null)
                    result = FibaroController.process(params);
                if(result == null && pref.getBoolean("vera_enabled", false) && VeraController != null)
                    result = VeraController.process(params);
                return result;
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

    public void speak(String text) {
        speak(text, true);
    }

    public void speak(String text, boolean screen) {
        if (screen)
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        if (pref.getBoolean("tts_enabled", false)) {
            if (textToSpeech == null)
                return;
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params, UUID.randomUUID().toString());
        }
    }

    private void show(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        Log.w(TAG, text);
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
        if (recognizer != null)
            recognizer.destroy();
        if (textToSpeech != null)
            textToSpeech.shutdown();
        Log.w(TAG, "onDestroy");
        super.onDestroy();
    }
}