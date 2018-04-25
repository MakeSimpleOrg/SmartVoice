package com.diamond.SmartVoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.diamond.SmartVoice.Controllers.Fibaro.Fibaro;
import com.diamond.SmartVoice.Controllers.Homey.Homey;
import com.diamond.SmartVoice.Controllers.Vera.Vera;
import com.diamond.SmartVoice.Controllers.Zipato.Zipato;
import com.diamond.SmartVoice.Recognizer.AbstractRecognizer;
import com.diamond.SmartVoice.Recognizer.GoogleRecognizer;
import com.diamond.SmartVoice.Recognizer.PocketSphinxRecognizer;
import com.diamond.SmartVoice.Recognizer.SnowboyRecognizer;
import com.diamond.SmartVoice.Recognizer.YandexRecognizer;
import com.rollbar.android.Rollbar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Dmitriy Ponomarev
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private AbstractRecognizer keyPhraseRecognizer;
    public AbstractRecognizer recognizer;

    private TextToSpeech textToSpeech;
    private View MicView;
    public SharedPreferences pref;
    public Homey HomeyController;
    public Fibaro FibaroController;
    public Vera VeraController;
    public Zipato ZipatoController;

    private boolean isLoading = true;
    private boolean homeyLoading = false;
    private boolean fibaroLoading = false;
    private boolean veraLoading = false;
    private boolean zipatoLoading = false;
    private boolean ttsLoading = false;
    private boolean keyPhraseRecognizerLoading = false;
    private boolean recognizerLoading = false;

    private View progressBar;
    private TextView textView;
    private TextView speakView;

    public String PocketSphinxKeyPhrase;
    public boolean offline_recognition = false;

    private boolean buttonPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "OS.ARCH : " + System.getProperty("os.arch"));

        Utils.load(this);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        Rollbar.init(this, "1462b05ac823412281cd257b13eb6c7f", "development");

        PocketSphinxKeyPhrase = pref.getString("PocketSphinxKeyPhrase", getString(R.string.defaultKeyPhrase));
        offline_recognition = pref.getBoolean("offline_recognition", false);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_NETWORK_STATE
                        }, 1);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        textView = findViewById(R.id.text_view_id);
        speakView = findViewById(R.id.speak_view_id);

        MicView = findViewById(R.id.mic);
        MicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading)
                    return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (buttonPressed && recognizer instanceof YandexRecognizer) {
                            Log.w(TAG, "onTouch");
                            recognizer.stopListening();
                            buttonOff();
                        } else {
                            if (keyPhraseRecognizer != null)
                                keyPhraseRecognizer.stopListening();
                            if (recognizer != null)
                                recognizer.startListening();
                            buttonOn();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                }
                return true;
            }
        });

        SettingsActivity.mainActivity = this;

        Button settingsBtn = findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading)
                    return;
                Intent activity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(activity);
            }
        });

        DevicesActivity.mainActivity = this;

        Button devicesBtn = findViewById(R.id.devicesButton);
        devicesBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading)
                    return;
                Intent activity = new Intent(MainActivity.this, DevicesActivity.class);
                startActivity(activity);
            }
        });

        ScenesActivity.mainActivity = this;

        Button scenesBtn = findViewById(R.id.scenesButton);
        scenesBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading || ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading)
                    return;
                Intent activity = new Intent(MainActivity.this, ScenesActivity.class);
                startActivity(activity);
            }
        });

        progressBar.setVisibility(View.VISIBLE);

        if (!pref.getString("keyRecognizerType", "None").equalsIgnoreCase("None")) {
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

                        if (pref.getString("voiceRecognizerType", "Google").equalsIgnoreCase("Google"))
                            setupGoogleRecognizer();
                        else
                            setupYandexRecognizer();
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
                        Rollbar.instance().error(e);
                    }
                }

                isLoading = false;

                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

                Log.w(TAG, "Start keyPhraseRecognizer");

                if (keyPhraseRecognizer != null)
                    keyPhraseRecognizer.startListening();

                /*
                if (pref.getBoolean("tts_enabled", false))
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speak("Слушаю", false);
                        }
                    });
                */
            }
        }).start();

        if (pref.getBoolean("homey_enabled", false) && !pref.getString("homey_server_ip", "").isEmpty() && !pref.getString("homey_bearer", "").isEmpty())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupHomey(MainActivity.this);
                }
            }).start();

        if (pref.getBoolean("fibaro_enabled", false) && !pref.getString("fibaro_server_ip", "").isEmpty() && !pref.getString("fibaro_server_login", "").isEmpty() && !pref.getString("fibaro_server_password", "").isEmpty())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupFibaro(MainActivity.this);
                }
            }).start();

        if (pref.getBoolean("vera_enabled", false) && !pref.getString("vera_server_ip", "").isEmpty())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupVera(MainActivity.this);
                }
            }).start();

        if (pref.getBoolean("zipato_enabled", false) && !pref.getString("zipato_server_ip", "my.zipato.com:443").isEmpty() && !pref.getString("zipato_server_login", "").isEmpty() && !pref.getString("zipato_server_password", "").isEmpty())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupZipato(MainActivity.this);
                }
            }).start();
    }

    private boolean isLoading() {
        return ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading || recognizerLoading || keyPhraseRecognizerLoading;
    }

    public void buttonOn() {
        buttonPressed = true;
        MicView.setBackgroundResource(R.drawable.background_big_mic_green);
    }

    public void buttonOff() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        buttonPressed = false;
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
            if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("Snowboy") && !System.getProperty("os.arch").equalsIgnoreCase("i686"))
                keyPhraseRecognizer = new SnowboyRecognizer(this);
            else if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("PocketSphinx"))
                keyPhraseRecognizer = new PocketSphinxRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }
        keyPhraseRecognizerLoading = false;
    }

    public void setupGoogleRecognizer() {
        recognizerLoading = true;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer = null;
        }
        try {
            recognizer = new GoogleRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }
        recognizerLoading = false;
    }

    public void setupYandexRecognizer() {
        recognizerLoading = true;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer = null;
        }
        try {
            recognizer = new YandexRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
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
            Rollbar.instance().error(e);
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
                    Rollbar.instance().error(e);
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
                    activity.show("Homey: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms_and) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
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
                    Rollbar.instance().error(e);
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
                    activity.show("Fibaro: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
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
                    Rollbar.instance().error(e);
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
                    activity.show("Vera: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
                }
                activity.veraLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public static void setupZipato(final MainActivity activity) {
        activity.zipatoLoading = true;
        activity.progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Zipato>() {
            @Override
            protected Zipato doInBackground(Void... params) {
                Zipato controller = null;
                try {
                    controller = new Zipato(activity);
                } catch (Exception e) {
                    e.printStackTrace();
                    Rollbar.instance().error(e);
                    return null;
                }
                return controller.getVisibleDevicesCount() > 0 || controller.getVisibleScenesCount() > 0 ? controller : null;
            }

            @Override
            protected void onPostExecute(Zipato controller) {
                activity.ZipatoController = controller;
                if (activity.ZipatoController == null) {
                    activity.show("Zipato: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("zipato_server_ip", ""));
                } else {
                    activity.show("Zipato: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
                }
                activity.zipatoLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public long lastKeyPhrase;

    public Handler OnKeyPhraseHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            lastKeyPhrase = System.currentTimeMillis();
            if (keyPhraseRecognizer != null) {
                keyPhraseRecognizer.stopListening();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (recognizer != null)
                recognizer.startListening();
            buttonOn();
        }
    };

    public void OnKeyPhrase() {
        OnKeyPhraseHandler.obtainMessage().sendToTarget();
    }

    public static void process(final String[] variants, final MainActivity activity) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String words = Arrays.toString(params);
                Log.w(TAG, activity.getString(R.string.you_say) + words);
                activity.showSpeak(words);

                if (!activity.pref.getBoolean("homey_enabled", false) && !activity.pref.getBoolean("fibaro_enabled", false) && !activity.pref.getBoolean("vera_enabled", false) && !activity.pref.getBoolean("zipato_enabled", false) || activity.HomeyController == null && activity.FibaroController == null && activity.VeraController == null && activity.ZipatoController == null)
                    return activity.getString(R.string.nothing_to_manage);
                String result = null;
                try {
                    if (activity.pref.getBoolean("homey_enabled", false) && activity.HomeyController != null)
                        result = activity.HomeyController.process(params, activity.pref);
                    if (result == null && activity.pref.getBoolean("fibaro_enabled", false) && activity.FibaroController != null)
                        result = activity.FibaroController.process(params, activity.pref);
                    if (result == null && activity.pref.getBoolean("vera_enabled", false) && activity.VeraController != null)
                        result = activity.VeraController.process(params, activity.pref);
                    if (result == null && activity.pref.getBoolean("zipato_enabled", false) && activity.ZipatoController != null)
                        result = activity.ZipatoController.process(params, activity.pref);
                    if (result != null && activity.recognizer instanceof YandexRecognizer) {
                        if (!activity.pref.getBoolean("tts_enabled", false))
                            Utils.dong.start();
                        activity.recognizer.stopListening();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Rollbar.instance().error(e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null)
                    activity.speak(result);
                if (activity.recognizer instanceof YandexRecognizer) {
                    if (result != null || !YandexRecognizer.continuousMode)
                        activity.buttonOff();
                } else {
                    //if (result == null)
                    //    activity.speak(activity.getString(R.string.repeat));
                    activity.buttonOff();
                }
            }
        }.execute(variants);
    }

    public void speak(String text) {
        speak(text, true);
    }

    public void speak(String text, boolean screen) {
        try {
            if (screen)
                show(text);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
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
            Rollbar.instance().error(e);
        }
    }

    @SuppressLint("SetTextI18n")
    public void showSpeak(final String text) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    speakView.setText(text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.w(TAG, text);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void show(final String text) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                    textView.setText(text + "\n" + textView.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.w(TAG, text);
            }
        });
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