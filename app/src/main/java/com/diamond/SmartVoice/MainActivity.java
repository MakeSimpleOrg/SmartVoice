package com.diamond.SmartVoice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
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

import com.diamond.SmartVoice.Controllers.Fibaro.Fibaro;
import com.diamond.SmartVoice.Controllers.Homey.Homey;
import com.diamond.SmartVoice.Controllers.Vera.Vera;
import com.diamond.SmartVoice.Recognizer.GoogleRecognizer;
import com.diamond.SmartVoice.Recognizer.PocketSphinxRecognizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Dmitriy Ponomarev
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private PocketSphinxRecognizer keyPhraseRecognizer;
    private GoogleRecognizer recognizer;

    private TextToSpeech textToSpeech;
    private View MicView;
    public SharedPreferences pref;
    public Homey HomeyController;
    public Fibaro FibaroController;
    public Vera VeraController;

    private boolean isLoading = true;
    private boolean homeyLoading = false;
    private boolean fibaroLoading = false;
    private boolean veraLoading = false;
    private boolean ttsLoading = false;
    private boolean keyPhraseRecognizerLoading = false;
    private boolean recognizerLoading = false;

    private View progressBar;

    public String keyphrase;
    public boolean offline_recognition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        keyphrase = pref.getString("keyphrase", getString(R.string.defaultKeyPhrase));
        offline_recognition = pref.getBoolean("offline_recognition", false);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.INTERNET
                        }, 1);
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 1: " + e.getMessage());
        }

        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        MicView = findViewById(R.id.mic);
        MicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading)
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
                        v.performClick();
                        break;
                }
                return true;
            }
        });

        SettingsActivity.mainActivity = this;

        Button settingsBtn = (Button) findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading)
                    return;
                Intent activity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(activity);
            }
        });

        DevicesActivity.mainActivity = this;

        Button devicesBtn = (Button) findViewById(R.id.devicesButton);
        devicesBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading)
                    return;
                Intent activity = new Intent(MainActivity.this, DevicesActivity.class);
                startActivity(activity);
            }
        });

        progressBar.setVisibility(View.VISIBLE);

        if (pref.getBoolean("keyRecognizer", false)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupKeyphraseRecognizer();
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupRecognizer();
                    }
                });
            }
        }).start();

        if (pref.getBoolean("tts_enabled", false))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupTTS();
                }
            }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isLoading()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (isDebug())
                            show("Error 2: " + e.getMessage());
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
        }).start();

        if (pref.getBoolean("homey_enabled", false))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupHomey(MainActivity.this);
                }
            }).start();

        if (pref.getBoolean("fibaro_enabled", false))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupFibaro(MainActivity.this);
                }
            }).start();

        if (pref.getBoolean("vera_enabled", false))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupVera(MainActivity.this);
                }
            }).start();
    }

    public boolean isDebug() {
        return pref.getBoolean("debug", false);
    }

    private boolean isLoading() {
        return ttsLoading || homeyLoading || fibaroLoading || veraLoading || recognizerLoading || keyPhraseRecognizerLoading;
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
        try {
            keyPhraseRecognizer = new PocketSphinxRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 3: " + e.getMessage());
        }
        keyPhraseRecognizerLoading = false;
    }

    public void setupRecognizer() {
        recognizerLoading = true;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer = null;
        }
        try {
            recognizer = new GoogleRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 4: " + e.getMessage());
        }
        recognizerLoading = false;
    }

    public void setupTTS() {
        ttsLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 5: " + e.getMessage());
            ttsLoading = false;
        }
    }

    public static void setupHomey(final MainActivity activity) {
        activity.homeyLoading = true;
        activity.progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Homey>() {
            @Override
            protected Homey doInBackground(Void... params) {
                Homey controller = null;
                try {
                    controller = new Homey(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (activity.isDebug())
                        activity.show("Error 6: " + e.getMessage());
                    return null;
                }
                return controller.getVisibleDevicesCount() > 0 || controller.getVisibleScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(Homey controller) {
                activity.HomeyController = controller;
                if (activity.HomeyController == null)
                    activity.show("Homey: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("homey_server_ip", ""));
                else
                    activity.show("Homey: " + activity.getString(R.string.found) + " " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms_and) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices));
                activity.homeyLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public static void setupFibaro(final MainActivity activity) {
        activity.fibaroLoading = true;
        activity.progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Fibaro>() {
            @Override
            protected Fibaro doInBackground(Void... params) {
                Fibaro controller = null;
                try {
                    controller = new Fibaro(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (activity.isDebug())
                        activity.show("Error 6: " + e.getMessage());
                    return null;
                }
                return controller.getVisibleDevicesCount() > 0 || controller.getVisibleScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(Fibaro controller) {
                activity.FibaroController = controller;
                if (activity.FibaroController == null)
                    activity.show("Fibaro: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("fibaro_server_ip", ""));
                else
                    activity.show("Fibaro: " + activity.getString(R.string.found) + " " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + activity.getString(R.string.found_scene));
                activity.fibaroLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public static void setupVera(final MainActivity activity) {
        activity.veraLoading = true;
        activity.progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Vera>() {
            @Override
            protected Vera doInBackground(Void... params) {
                Vera controller = null;
                try {
                    controller = new Vera(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (activity.isDebug())
                        activity.show("Error 7: " + e.getMessage());
                    return null;
                }
                return controller.getVisibleDevicesCount() > 0 || controller.getVisibleScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(Vera controller) {
                activity.VeraController = controller;
                if (activity.VeraController == null) {
                    activity.show("Vera: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("vera_server_ip", ""));
                } else {
                    activity.show("Vera: " + activity.getString(R.string.found) + " " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
                }
                activity.veraLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
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

    public static void process(final String[] variants, final MainActivity activity) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                Log.w(TAG, activity.getString(R.string.you_say) + Arrays.toString(params));
                if (!activity.pref.getBoolean("homey_enabled", false) && !activity.pref.getBoolean("fibaro_enabled", false) && !activity.pref.getBoolean("vera_enabled", false) || activity.HomeyController == null && activity.FibaroController == null && activity.VeraController == null)
                    return activity.getString(R.string.nothing_to_manage);
                String result = null;
                try {
                    if (activity.pref.getBoolean("homey_enabled", false) && activity.HomeyController != null)
                        result = activity.HomeyController.process(params);
                    if (result == null && activity.pref.getBoolean("fibaro_enabled", false) && activity.FibaroController != null)
                        result = activity.FibaroController.process(params);
                    if (result == null && activity.pref.getBoolean("vera_enabled", false) && activity.VeraController != null)
                        result = activity.VeraController.process(params);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (activity.isDebug())
                        activity.show("Error 8: " + e.getMessage());
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null)
                    activity.speak(result);
                else
                    activity.speak(activity.getString(R.string.repeat));
                activity.buttonOff();
            }
        }.execute(variants);
    }

    public void speak(String text) {
        speak(text, true);
    }

    public void speak(String text, boolean screen) {
        try {
            if (screen)
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 9: " + e.getMessage());
        }
        try {
            if (pref.getBoolean("tts_enabled", false)) {
                if (textToSpeech == null)
                    return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bundle params = new Bundle();
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
                    textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params, UUID.randomUUID().toString());
                } else {
                    Toast.makeText(MainActivity.this, "speak", Toast.LENGTH_SHORT).show();
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
                    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                    params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                        params.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
                    textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug())
                show("Error 10: " + e.getMessage());
        }
    }

    public void show(String text) {
        try {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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