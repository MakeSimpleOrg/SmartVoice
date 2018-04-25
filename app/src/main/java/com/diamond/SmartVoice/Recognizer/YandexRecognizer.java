package com.diamond.SmartVoice.Recognizer;

import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;

import java.util.Arrays;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.RecognitionHypothesis;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.SpeechKit;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * @author Dmitriy Ponomarev
 */
public class YandexRecognizer extends AbstractRecognizer implements RecognizerListener {
    private static final String TAG = YandexRecognizer.class.getSimpleName();

    private static final String API_KEY = "0b9f1c33-9350-4413-a7ad-2e9565bcfec5";

    private Recognizer recognizer;

    private MainActivity mContext;

    private RecognitionHypothesis[] PartialResults;

    private boolean SpeechDetected;

    private long lastSpeech;

    public static boolean continuousMode = true;

    public YandexRecognizer(MainActivity context) {
        this.mContext = context;
        SpeechKit.getInstance().configure(mContext, API_KEY);
    }

    public void startListening() {
        if(continuousMode)
            lastSpeech = System.currentTimeMillis();
        createAndStartRecognizer();
    }

    public void stopListening() {
        resetRecognizer();
    }

    @Override
    public void onRecordingBegin(Recognizer recognizer) {
        Log.w(TAG, "Recording begin");
    }

    @Override
    public void onSpeechDetected(Recognizer recognizer) {
        Log.w(TAG, "Speech detected");
        if (continuousMode)
            SpeechDetected = true;
    }

    @Override
    public void onSpeechEnds(Recognizer recognizer) {
        Log.w(TAG, "Speech ends");
        if (continuousMode) {
            SpeechDetected = false;
            if (PartialResults != null) {
                String[] result = new String[PartialResults.length];
                for (int i = 0; i < PartialResults.length; i++) {
                    result[i] = PartialResults[i].getNormalized();
                    Log.w(TAG, "result: " + result[i]);
                }
                PartialResults = null;
                MainActivity.process(result, mContext);
            }
        }
    }

    @Override
    public void onRecordingDone(Recognizer recognizer) {
        Log.w(TAG, "Recording done");
    }

    @Override
    public void onSoundDataRecorded(Recognizer recognizer, byte[] bytes) {
    }

    @Override
    public void onPowerUpdated(Recognizer recognizer, float power) {
        //Log.w(TAG, "power: " + power);
        if (continuousMode && System.currentTimeMillis() - lastSpeech > 10000) {
            resetRecognizer();
            mContext.buttonOff();
        }
    }

    @Override
    public void onPartialResults(Recognizer recognizer, Recognition recognition, boolean b) {
        if (continuousMode && SpeechDetected) {
            Log.w(TAG, "Partial results " + b + " " + recognition.getBestResultText());
            PartialResults = recognition.getHypotheses();
            mContext.showSpeak(recognition.getBestResultText());
        }
    }

    @Override
    public void onRecognitionDone(Recognizer recognizer, Recognition recognition) {
        Log.w(TAG, "onRecognitionDone");
        resetRecognizer();
        RecognitionHypothesis[] hypotheses = recognition.getHypotheses();
        if (hypotheses == null || hypotheses.length == 0) {
            mContext.speak("Повтори!");
            mContext.buttonOff();
            return;
        }
        String[] result = new String[hypotheses.length];
        for (int i = 0; i < hypotheses.length; i++)
            result[i] = hypotheses[i].getNormalized();
        MainActivity.process(result, mContext);
    }

    @Override
    public void onError(Recognizer recognizer, ru.yandex.speechkit.Error error) {
        if (error.getCode() == Error.ERROR_CANCELED) {
            Log.w(TAG, "Cancelled");
        } else {
            Log.w(TAG, "Error occurred " + error.getString());
            resetRecognizer();
            mContext.speak("Повтори!");
            mContext.buttonOff();
        }
    }

    private void createAndStartRecognizer() {
        if (mContext == null)
            return;
        if (ContextCompat.checkSelfPermission(mContext, RECORD_AUDIO) == PERMISSION_GRANTED) {
            resetRecognizer();
            recognizer = Recognizer.create(Recognizer.Language.RUSSIAN, Recognizer.Model.QUERIES, this, continuousMode);
            recognizer.start();
        }
    }

    private void resetRecognizer() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer = null;
            SpeechDetected = false;
            PartialResults = null;
        }
    }

    public void destroy() {
        try {
            resetRecognizer();
        } catch (Exception ignored) {
        }
    }
}