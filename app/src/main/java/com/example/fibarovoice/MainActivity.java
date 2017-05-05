package com.diamond.SmartVoice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements RecognitionListener, PocketSphinxRecognizer.OnResultListener {
    private String LOG_TAG = "MainActivity";

    private PocketSphinxRecognizer pocketSphinxRecognizer;

    private SpeechRecognizer googleRecognizer = null;
    private Intent recognizerIntent;
    private FibaroConnector controller;
    private TextToSpeech textToSpeech;
    private View MicView;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        pocketSphinxRecognizer = new PocketSphinxRecognizer(this);

        pocketSphinxRecognizer.initPocketSphinx();

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR)
                    return;
                if (textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE)
                    textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        setupController();

        googleRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        googleRecognizer.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, true);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);

        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true); //TODO сравнить качество онлайн и оффлайн

        MicView = findViewById(R.id.mic);
        MicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //MicView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        pocketSphinxRecognizer.stopSearch();
                        googleRecognizer.startListening(recognizerIntent);
                        buttonOn();
                        break;
                    case MotionEvent.ACTION_UP:
                        //speech.stopListening();
                        //MicView.setBackgroundResource(R.drawable.background_big_mic);
                        //buttonOff();
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
    }

    public void buttonOn() {
        MicView.setBackgroundResource(R.drawable.background_big_mic_green);
    }

    public void buttonOff() {
        MicView.setBackgroundResource(R.drawable.background_big_mic);
        pocketSphinxRecognizer.restartSearch();
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
                if (MainActivity.this.controller == null) {
                    //Toast.makeText(MainActivity.this, "Controller is not found! IP: " + pref.getString("server_ip", ""), Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Контроллер не найден! IP: " + pref.getString("server_ip", ""), Toast.LENGTH_SHORT).show();
                    //speak("Контроллер не найден");
                }
                else {
                    //Toast.makeText(MainActivity.this, "Controller is found with " + controller.getLastDevicesCount() + " devices", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Найдено " + controller.getLastRoomsCount() + " комнат, " + controller.getLastDevicesCount() + " устройств и " + controller.getLastScenesCount() + " сцен", Toast.LENGTH_SHORT).show();
                    //speak("Контроллер найден");
                }
            }
        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //if (speech != null) {
        //    speech.destroy();
        //    Log.i(LOG_TAG, "destroy");
        //}
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        if(errorCode != SpeechRecognizer.ERROR_CLIENT) {
            Log.d(LOG_TAG, "FAILED " + errorMessage);
            buttonOff();
        }
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.i(LOG_TAG, "onPartialResults: " + matches);
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
        buttonOn();
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        buttonOff();
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");

        //speech.stopListening();

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null)
            process(matches.toArray(new String[matches.size()]));
        else
            speak("Повтори!");

        //String text = "";
        //for (String result : matches)
        //	text += result + "\n";
        //String scores = "";
        //for (float s : results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES))
        //	scores += s + " ";
        //Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        //System.out.println("results: " + results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).toString() + ", scores: " + scores);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_CDMA_PIP, 200);
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    private void speak(String text) {
        googleRecognizer.stopListening();
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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
            params.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
        }
    }

    public void process(final String[] variants) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                Device[] devices = controller.getDevices(params);
                if(devices.length != 0)
                    return controller.process(devices);
                Scene[] scenes = controller.getScenes(params);
                if(scenes.length != 0)
                    return controller.process(scenes);
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    //Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    speak(result);
                } else {
                    //Toast.makeText(MainActivity.this, "Повтори!", Toast.LENGTH_SHORT).show();
                    speak("Повтори!");
                }
            }
        }.execute(variants);
    }

    @Override
    protected void onDestroy() {
        if (googleRecognizer != null)
            googleRecognizer.cancel();
        if (textToSpeech != null)
            textToSpeech.shutdown();
        Log.i(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void OnResult(String command) {
            if (command.contains(pocketSphinxRecognizer.KEYPHRASE)){
                //Toast.makeText(this,"You said:"+command, Toast.LENGTH_SHORT).show();
                googleRecognizer.startListening(recognizerIntent);
                buttonOn();
                return;
            }
    }
}