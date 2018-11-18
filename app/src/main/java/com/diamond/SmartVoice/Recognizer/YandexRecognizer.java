package com.diamond.SmartVoice.Recognizer;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.Utils;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Language;
import ru.yandex.speechkit.OnlineModel;
import ru.yandex.speechkit.OnlineRecognizer;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.RecognitionHypothesis;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.Track;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * @author Dmitriy Ponomarev
 */
public class YandexRecognizer extends AbstractRecognizer implements RecognizerListener {
    private static final String TAG = YandexRecognizer.class.getSimpleName();

    private OnlineRecognizer recognizer;

    private MainActivity mContext;

    private RecognitionHypothesis[] PartialResults;

    private boolean SpeechDetected, Result;

    private long lastSpeech;

    public static boolean continuousMode = true;

    public YandexRecognizer(MainActivity context) {
        this.mContext = context;
        Language lang = Utils.getYandexLanguage(context.pref.getString("YandexRecognizerLang", "None"));

        Log.w(TAG, "Start Recognizer, lang: " + lang + ", p: " + context.pref.getString("YandexRecognizerLang", "None"));
        recognizer = new OnlineRecognizer.Builder(lang, OnlineModel.NOTES, this)
                .setDisableAntimat(false)
                .setEnablePunctuation(false)
                .build();
        recognizer.prepare();
    }

    public void startListening() {
        //if (!(mContext.keyPhraseRecognizer instanceof GoogleKeyRecognizer))
        Utils.ding.start();
        Result = false;
        if (continuousMode)
            lastSpeech = System.currentTimeMillis();
        if (mContext == null)
            return;
        if (ContextCompat.checkSelfPermission(mContext, RECORD_AUDIO) == PERMISSION_GRANTED) {
            SpeechDetected = false;
            PartialResults = null;
            recognizer.startRecording();
        }
    }

    public void stopListening() {
        resetRecognizer();
    }

    @Override
    public void onRecordingBegin(@NonNull Recognizer recognizer) {
        Log.w(TAG, "Recording begin");
    }

    @Override
    public void onSpeechDetected(@NonNull Recognizer recognizer) {
        if (continuousMode) {
            SpeechDetected = true;
            lastWords = null;
        }
    }

    @Override
    public void onSpeechEnds(@NonNull Recognizer recognizer) {
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
    public void onRecordingDone(@NonNull Recognizer recognizer) {
    }

    @Override
    public void onMusicResults(@NonNull Recognizer recognizer, @NonNull Track track) {
        Log.w(TAG, "onMusicResults");
    }

    @Override
    public void onPowerUpdated(@NonNull Recognizer recognizer, float power) {
        if (continuousMode && System.currentTimeMillis() - lastSpeech > 5000) {
            resetRecognizer();
            mContext.buttonOff();
        }
    }

    private String lastWords;

    @Override
    public void onPartialResults(@NonNull Recognizer recognizer, @NonNull Recognition recognition, boolean b) {
        if (continuousMode && SpeechDetected) {
            lastSpeech = System.currentTimeMillis();
            PartialResults = recognition.getHypotheses();
            String result = recognition.getBestResultText();
            if (!result.isEmpty() && !result.equals(lastWords)) {
                lastWords = result;
                Log.w(TAG, "Partial results " + b + " " + recognition.getBestResultText());
                mContext.showSpeak(result);
                if (b && result.trim().split(" ").length > 1) {
                    PartialResults = null;
                    MainActivity.process(new String[]{result}, mContext);
                }
            }
        }
    }

    @Override
    public void onRecognitionDone(@NonNull Recognizer recognizer) {
        Log.w(TAG, "onRecognitionDone");
        resetRecognizer();
        mContext.buttonOff();
    }

    @Override
    public void onRecognizerError(@NonNull Recognizer recognizer, @NonNull Error error) {
        Log.w(TAG, "Error occurred " + error.getMessage());
    }

    private void resetRecognizer() {
        if (recognizer != null) {
            recognizer.stopRecording();
            SpeechDetected = false;
            PartialResults = null;
        }
    }

    public void destroy() {
        try {
            if (recognizer != null) {
                recognizer.stopRecording();
                recognizer.cancel();
                recognizer.destroy();
                recognizer = null;
                SpeechDetected = false;
                PartialResults = null;
            }
        } catch (Exception ignored) {
        }
    }
}