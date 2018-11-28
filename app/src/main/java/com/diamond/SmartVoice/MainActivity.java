package com.diamond.SmartVoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.diamond.SmartVoice.Controllers.Controller;
import com.diamond.SmartVoice.Controllers.Fibaro.Fibaro;
import com.diamond.SmartVoice.Controllers.Homey.Homey;
import com.diamond.SmartVoice.Controllers.MQTTController;
import com.diamond.SmartVoice.Controllers.Vera.Vera;
import com.diamond.SmartVoice.Controllers.WirenBoard.WirenBoard;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ru.yandex.speechkit.SpeechKit;

/**
 * @author Dmitriy Ponomarev
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String YANDEX_API_KEY = "6d5c4e48-3c78-434e-96a2-2ecea92d8120";

    public AbstractRecognizer keyPhraseRecognizer;
    public AbstractRecognizer recognizer;
    private AbstractVocalizer vocalizer;

    AIDataService aiService;

    private View MicView;
    public SharedPreferences pref;

    public static ArrayList<Controller> controllers;

    protected SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private boolean isLoading = true;
    private int controllersLoading = 0;
    private boolean ttsLoading = false;
    private boolean keyPhraseRecognizerLoading = false;
    private boolean recognizerLoading = false;

    private View progressBar;
    private TextView textView;
    private TextView speakView;

    public String PocketSphinxKeyPhrase;
    public boolean offline_recognition = false;

    private boolean buttonPressed = false;

    public static String currentSSID = null;

    public boolean wifi() {
        if (currentSSID == null) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)
                    currentSSID = wifiInfo.getSSID();
                if (currentSSID != null)
                    currentSSID = currentSSID.replaceAll("\"", "");
            }
        }
        return pref.getString("homeSSID", "").equalsIgnoreCase(currentSSID);
    }

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

        wifi();

        Log.i(TAG, "currentSSID: " + currentSSID);

        String homeSSID = pref.getString("homeSSID", "");
        if (homeSSID.isEmpty())
            pref.edit().putString("homeSSID", currentSSID).apply();

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
                if (isLoading || ttsLoading || controllersLoading > 0)
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
                if (isLoading())
                    return;
                Intent activity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(activity);
            }
        });

        DevicesActivity.mainActivity = this;

        Button devicesBtn = findViewById(R.id.devicesButton);
        devicesBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading())
                    return;
                Intent activity = new Intent(MainActivity.this, DevicesActivity.class);
                startActivity(activity);
            }
        });

        ScenesActivity.mainActivity = this;

        Button scenesBtn = findViewById(R.id.scenesButton);
        scenesBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isLoading())
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

        controllers = new ArrayList<Controller>();
        controllers.add(new Homey(this));
        controllers.add(new Fibaro(this));
        controllers.add(new Vera(this));
        controllers.add(new Zipato(this));
        controllers.add(new WirenBoard(this));

        for (Controller controller : controllers)
            if (controller.isEnabled())
                loadController(this, controller);

        if (Integer.parseInt(pref.getString("polling", "300")) > 0)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int polling = Integer.parseInt(pref.getString("polling", "300"));
                        while (polling > 0) {
                            Thread.sleep(polling * 1000);
                            polling = Integer.parseInt(pref.getString("polling", "300"));
                            long time;
                            for (Controller controller : controllers)
                                if (controller.isLoaded() && controller.isEnabled() && !(controller instanceof MQTTController)) {
                                    time = System.currentTimeMillis();
                                    controller.updateData();
                                    Log.d(TAG, controller.getName() + " poll time: " + (System.currentTimeMillis() - time));
                                }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Rollbar.instance().error(e);
                    }
                }
            }).start();

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
                for (Controller controller : controllers)
                    controller.onSharedPreferenceChanged(pref, key);
            }
        };
        pref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        AIConfiguration configuration = new AIConfiguration("923bfde517fe45e89f75e05fbcc699f5");
        aiService = new AIDataService(configuration);

        /* API V2
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Storage storage = StorageOptions.getDefaultInstance().getService();

                GoogleCredentials credentials = null;
                try {
                    System.out.println(Utils.assetDir.getAbsolutePath());
                    credentials = GoogleCredentials.fromStream(new FileInputStream(Utils.assetDir.getAbsolutePath() + "/small-talk-58989-e8d7173db7a0.json"))
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
                    Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
                    System.out.println("Buckets:");
                    Page<Bucket> buckets = storage.list();
                    for (Bucket bucket : buckets.iterateAll()) {
                        System.out.println(bucket.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                answer("Тест");
            }
        }).start();
        */
    }

    private boolean isLoading() {
        return ttsLoading || controllersLoading > 0 || recognizerLoading || keyPhraseRecognizerLoading;
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

    public static void loadController(final MainActivity activity, final Controller controller) {
        activity.controllersLoading++;
        activity.progressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    controller.loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                    Rollbar.instance().error(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void param) {
                if (controller.isLoaded())
                    activity.show(controller.getName() + ": " + controller.getVisibleRoomsCount() + " " + activity.getString(R.string.found_rooms_and) + " " + controller.getVisibleDevicesCount() + " " + activity.getString(R.string.found_devices_and) + " " + controller.getVisibleScenesCount() + " " + activity.getString(R.string.found_scene));
                else
                    activity.show(controller.getName() + ": " + activity.getString(R.string.controler_not_found) + " " + controller.getHost());
                activity.controllersLoading--;
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
        }, keyPhraseRecognizer instanceof GoogleKeyRecognizer ? 1000 : 200);
    }

    public static void process(final String[] variants, final MainActivity activity) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String words = Arrays.toString(params).replace("[", "").replace("]", "");
                Log.w(TAG, activity.getString(R.string.you_say) + words);
                activity.showSpeak(words);
                boolean enabled = false;
                for (Controller controller : controllers)
                    if (controller.isEnabled())
                        enabled = true;
                if (!enabled)
                    return activity.getString(R.string.nothing_to_manage);
                String result = null;
                try {
                    for (Controller controller : controllers)
                        if (result == null && controller.isEnabled())
                            result = controller.process(params, activity.pref);
                    if (result != null && activity.recognizer instanceof YandexRecognizer) {
                        activity.recognizer.stopListening();
                        if (!activity.pref.getBoolean("tts_enabled", false))
                            Utils.dong.start();
                    }
                    if (result == null)
                        result = activity.answer(params[0]);
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

    public String answer(String text) {
        Log.i(TAG, "question: " + text);

        try {
            AIRequest request = new AIRequest(text);
            AIResponse response = aiService.request(request);
            if (response.getStatus().getCode() == 200) {
                {
                    text = response.getResult().getFulfillment().getSpeech();
                    Log.i(TAG, "answer: " + text);
                    return text;
                }
            } else {
                System.err.println(response.getStatus().getErrorDetails());

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        /* API V2
        try {
            SessionsClient sessionsClient = SessionsClient.create();
            SessionName session = SessionName.of("small-talk-58989", pref.getString("device_uuid", UUID.randomUUID().toString()));
            String languageCode = "en-US";
            if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage()))
                languageCode = "ru-RU";

            TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode(languageCode);
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);
            QueryResult queryResult = response.getQueryResult();

            Log.i(TAG, "Query Text: " + queryResult.getQueryText());
            Log.i(TAG, "Detected Intent: " + queryResult.getIntent().getDisplayName() + ", confidence: " + queryResult.getIntentDetectionConfidence());
            Log.i(TAG, "Fulfillment Text: " + queryResult.getFulfillmentText());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */

        return null;
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
                //Log.w(TAG, text);
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