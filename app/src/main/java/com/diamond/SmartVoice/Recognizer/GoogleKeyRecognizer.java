package com.diamond.SmartVoice.Recognizer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class GoogleKeyRecognizer extends AbstractRecognizer implements RecognitionListener {
    private static final String TAG = GoogleKeyRecognizer.class.getSimpleName();

    private final static int MAX_PAUSE_TIME = 100;
    private final static int ERROR_TIMEOUT = 5000;

    private long startListeningTime;
    private boolean onReadyForSpeech = false;

    private MainActivity mContext;
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private AudioManager audioManager;
    private Handler restart = new Handler();

    public GoogleKeyRecognizer(MainActivity context) {
        this.mContext = context;

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, context.offline_recognition);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        onReadyForSpeech = true;
    }

    @Override
    public void onBeginningOfSpeech() { /**/ }

    @Override
    public void onRmsChanged(float rmsdB) { /**/ }

    @Override
    public void onBufferReceived(byte[] bytes) { /**/ }

    @Override
    public void onEndOfSpeech() { /**/ }

    @Override
    public void onError(int error) {
        long duration = System.currentTimeMillis() - startListeningTime;
        if (duration < ERROR_TIMEOUT && error == SpeechRecognizer.ERROR_NO_MATCH && !onReadyForSpeech)
            return;
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            restartRecognition();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResults(Bundle results) {
        final ArrayList<String> list;
        if ((results == null || (list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)) == null || list.isEmpty() || list.get(0).trim().isEmpty())) {
            restartRecognition();
            return;
        }
        onResult(list.get(0));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    private void onResult(String finalSpeechResult) {
        Log.d(TAG, "onResult: " + finalSpeechResult);
        if (!finalSpeechResult.toLowerCase().contains(mContext.pref.getString("GoogleKeyPhrase", mContext.getString(R.string.defaultYandexKeyPhrase)))) {
            recognizer.stopListening();
            recognizer.startListening(speechIntent);
            return;
        }

        Log.d(TAG, "keyPhrase: " + finalSpeechResult);
        restart.removeCallbacksAndMessages(null);
        if (recognizer != null)
            recognizer.stopListening();
        mContext.OnKeyPhrase();
    }

    private void restartRecognition() {
        restart.postDelayed(new Runnable() {
            @Override
            public void run() {
                startListeningTime = System.currentTimeMillis();
                if (recognizer != null)
                    recognizer.startListening(speechIntent);
            }
        }, MAX_PAUSE_TIME);
    }

    @Override
    public void onEvent(int i, Bundle bundle) { /**/ }

    public void startListening() {
        Log.d(TAG, "startListening");
        startListeningTime = System.currentTimeMillis();
        muteAudio(true);
        if (recognizer != null)
            recognizer.startListening(speechIntent);
    }

    public void stopListening() {
        Log.d(TAG, "stopListening");
        if (recognizer != null)
            recognizer.stopListening();
        muteAudio(false);
    }

    private int current_volume;
    private boolean muted;

    private void mute2(Boolean mute) {
        if (mute && muted)
            return;
        if (mute) {
            muted = true;
            current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else if (muted) {
            muted = false;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    @SuppressWarnings("deprecation")
    public void muteAudio(Boolean mute) {
        if (mute && muted)
            return;
        muted = mute;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
            else
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
        } catch (Exception e) {
            if (audioManager == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            else
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }

    public void destroy() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
                recognizer = null;
                muteAudio(false);
            } catch (Exception ignored) {
            }
        }
    }
}