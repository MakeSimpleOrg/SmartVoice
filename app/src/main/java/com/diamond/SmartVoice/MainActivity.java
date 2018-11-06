package com.diamond.SmartVoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.diamond.SmartVoice.Controllers.Fibaro.Fibaro;
import com.diamond.SmartVoice.Controllers.Homey.Homey;
import com.diamond.SmartVoice.Controllers.Vera.Vera;
import com.diamond.SmartVoice.Controllers.Zipato.Zipato;
import com.diamond.SmartVoice.Recognizer.AbstractRecognizer;
import com.diamond.SmartVoice.Recognizer.GoogleKeyRecognizer;
import com.diamond.SmartVoice.Recognizer.GoogleRecognizer;
import com.diamond.SmartVoice.Recognizer.PocketSphinxRecognizer;
import com.diamond.SmartVoice.Recognizer.SnowboyRecognizer;
import com.diamond.SmartVoice.Recognizer.YandexKeyRecognizer;
import com.diamond.SmartVoice.Recognizer.YandexRecognizer;
import com.diamond.SmartVoice.Vocalizer.AbstractVocalizer;
import com.diamond.SmartVoice.Vocalizer.GoogleVocalizer;
import com.diamond.SmartVoice.Vocalizer.YandexVocalizer;
import com.rollbar.android.Rollbar;

import java.util.Arrays;
import java.util.UUID;

import ru.yandex.speechkit.SpeechKit;

/**
 * @author Dmitriy Ponomarev
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String YANDEX_API_KEY = "6d5c4e48-3c78-434e-96a2-2ecea92d8120";

    private AbstractRecognizer keyPhraseRecognizer;
    public AbstractRecognizer recognizer;

    private AbstractVocalizer vocalizer;
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

        String userKey = pref.getString("yandexUserKey", "");
        String device_uuid = pref.getString("device_uuid", "");
        if (device_uuid.isEmpty()) {
            device_uuid = UUID.randomUUID().toString();
            pref.edit().putString("device_uuid", device_uuid).apply();
        }

        try {
            SpeechKit.getInstance().init(this, userKey.isEmpty() ? YANDEX_API_KEY : userKey);
            SpeechKit.getInstance().setUuid(device_uuid);
        } catch (Exception e) {
            e.printStackTrace();
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

        setupKeyphraseRecognizer();

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

        setupVocalizer();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isLoading()) {
                    try {
                        Thread.sleep(10);
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

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (keyPhraseRecognizer != null)
                            keyPhraseRecognizer.startListening();
                    }
                });

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

        if (isHomeyEnabled())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupHomey(MainActivity.this);
                }
            }).start();

        if (isFibaroEnabled())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupFibaro(MainActivity.this);
                }
            }).start();

        if (isVeraEnabled())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupVera(MainActivity.this);
                }
            }).start();

        if (isZipatoEnabled())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setupZipato(MainActivity.this);
                }
            }).start();

        if (Integer.parseInt(pref.getString("polling", "300")) > 0)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int polling = Integer.parseInt(pref.getString("polling", "300"));
                        while (polling > 0) {
                            Thread.sleep(polling * 1000);
                            polling = Integer.parseInt(pref.getString("polling", "300"));
                            long time = System.currentTimeMillis();
                            if (HomeyController != null && isHomeyEnabled()) {
                                HomeyController.updateData();
                                Log.d(TAG, "Homey poll time: " + (System.currentTimeMillis() - time));
                            }
                            time = System.currentTimeMillis();
                            if (FibaroController != null && isFibaroEnabled()) {
                                FibaroController.updateData();
                                Log.d(TAG, "Fibaro poll time: " + (System.currentTimeMillis() - time));
                                time = System.currentTimeMillis();
                            }
                            if (VeraController != null && isVeraEnabled()) {
                                VeraController.updateData();
                                Log.d(TAG, "Vera poll time: " + (System.currentTimeMillis() - time));
                                time = System.currentTimeMillis();
                            }
                            if (ZipatoController != null && isZipatoEnabled()) {
                                ZipatoController.updateData();
                                Log.d(TAG, "Zipato poll time: " + (System.currentTimeMillis() - time));
                                time = System.currentTimeMillis();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Rollbar.instance().error(e);
                    }
                }
            }).start();
    }

    private boolean isHomeyEnabled() {
        return pref.getBoolean("homey_enabled", false) && !pref.getString("homey_server_ip", "").isEmpty() && !pref.getString("homey_bearer", "").isEmpty();
    }

    private boolean isFibaroEnabled() {
        return pref.getBoolean("fibaro_enabled", false) && !pref.getString("fibaro_server_ip", "").isEmpty() && !pref.getString("fibaro_server_login", "").isEmpty() && !pref.getString("fibaro_server_password", "").isEmpty();
    }

    private boolean isVeraEnabled() {
        return pref.getBoolean("vera_enabled", false) && !pref.getString("vera_server_ip", "").isEmpty();
    }

    private boolean isZipatoEnabled() {
        return pref.getBoolean("zipato_enabled", false) && !pref.getString("zipato_server_ip", "my.zipato.com:443").isEmpty() && !pref.getString("zipato_server_login", "").isEmpty() && !pref.getString("zipato_server_password", "").isEmpty();
    }

    private boolean isLoading() {
        return ttsLoading || homeyLoading || fibaroLoading || veraLoading || zipatoLoading || recognizerLoading || keyPhraseRecognizerLoading;
    }

    public void setupKeyphraseRecognizer() {
        keyPhraseRecognizerLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        if (keyPhraseRecognizer != null) {
            keyPhraseRecognizer.destroy();
            keyPhraseRecognizer = null;
        }
        try {
            if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("Snowboy") && !System.getProperty("os.arch").equalsIgnoreCase("i686"))
                keyPhraseRecognizer = new SnowboyRecognizer(this);
            else if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("PocketSphinx"))
                keyPhraseRecognizer = new PocketSphinxRecognizer(this);
            else if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("Yandex"))
                keyPhraseRecognizer = new YandexKeyRecognizer(this);
            else if (pref.getString("keyRecognizerType", "None").equalsIgnoreCase("Google"))
                keyPhraseRecognizer = new GoogleKeyRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }
        keyPhraseRecognizerLoading = false;
        if (!isLoading())
            progressBar.setVisibility(View.INVISIBLE);
    }

    public void setupRecognizer() {
        recognizerLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        try {
            if (pref.getString("voiceRecognizerType", "Google").equalsIgnoreCase("Google"))
                recognizer = new GoogleRecognizer(this);
            else
                recognizer = new YandexRecognizer(this);
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
        }
        recognizerLoading = false;
        if (!isLoading())
            progressBar.setVisibility(View.INVISIBLE);
    }

    public void setupVocalizer() {
        ttsLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        if (vocalizer != null) {
            vocalizer.destroy();
            vocalizer = null;
        }
        try {
            if (pref.getString("vocalizerType", "None").equalsIgnoreCase("Google"))
                vocalizer = new GoogleVocalizer(this);
            else if (pref.getString("vocalizerType", "None").equalsIgnoreCase("Yandex"))
                vocalizer = new YandexVocalizer(this);
            else
                onVocalizerLoaded();
        } catch (Exception e) {
            e.printStackTrace();
            Rollbar.instance().error(e);
            ttsLoading = false;
            if (!isLoading())
                progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void onVocalizerLoaded() {
        ttsLoading = false;
        if (!isLoading())
            progressBar.setVisibility(View.INVISIBLE);
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
                if (activity.VeraController == null)
                    activity.show("Vera: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("vera_server_ip", ""));
                else
                    activity.show("Vera: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
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
                if (activity.ZipatoController == null)
                    activity.show("Zipato: " + activity.getString(R.string.controler_not_found) + " " + activity.pref.getString("zipato_server_ip", ""));
                else
                    activity.show("Zipato: " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
                activity.zipatoLoading = false;
                if (!activity.isLoading())
                    activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    public void buttonOn() {
        buttonPressed = true;
        MicView.setBackgroundResource(R.drawable.background_big_mic_green);
    }

    private Handler keyPhraseRecognizerRestart = new Handler();

    public void buttonOff() {
        keyPhraseRecognizerRestart.postDelayed(new Runnable() {
            @Override
            public void run() {
                buttonPressed = false;
                MicView.setBackgroundResource(R.drawable.background_big_mic);
                if (keyPhraseRecognizer != null)
                    keyPhraseRecognizer.startListening();
            }
        }, keyPhraseRecognizer instanceof GoogleKeyRecognizer ? 3000 : 200);
    }

    private Handler keyPhraseHandler = new Handler();

    public void OnKeyPhrase() {
        if (keyPhraseRecognizer != null && !(keyPhraseRecognizer instanceof GoogleKeyRecognizer))
            keyPhraseRecognizer.stopListening();
        keyPhraseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (keyPhraseRecognizer instanceof GoogleKeyRecognizer)
                    ((GoogleKeyRecognizer) keyPhraseRecognizer).muteAudio(false);
                if (recognizer != null)
                    recognizer.startListening();
                buttonOn();
            }
        }, keyPhraseRecognizer instanceof GoogleKeyRecognizer ? 500 : 200);
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
                        activity.recognizer.stopListening();
                        if (!activity.pref.getBoolean("tts_enabled", false))
                            Utils.dong.start();
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
                    if (result == null)
                        activity.speak(activity.getString(R.string.repeat));
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
            if (vocalizer != null)
                vocalizer.speak(text);
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
        if (vocalizer != null)
            vocalizer.destroy();
        Log.w(TAG, "onDestroy");
        super.onDestroy();
    }
}