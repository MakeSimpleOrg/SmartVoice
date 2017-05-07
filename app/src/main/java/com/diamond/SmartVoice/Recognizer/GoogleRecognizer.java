package com.diamond.SmartVoice.Recognizer;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class GoogleRecognizer implements RecognitionListener {

    private static final String TAG = GoogleRecognizer.class.getSimpleName();
    private MainActivity mContext;
    private SpeechRecognizer recognizer = null;
    private Intent intent;

    public GoogleRecognizer(MainActivity context) {
        this.mContext = context;

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);

        intent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, context.pref.getBoolean("offline_recognition", false));
    }

    public void startListening() {
        recognizer.startListening(intent);
    }

    public void stopListening() {
        recognizer.stopListening();
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.w(TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        if (errorCode != SpeechRecognizer.ERROR_CLIENT) {
            Log.w(TAG, "FAILED " + errorMessage);
            //if(System.currentTimeMillis() - mContext.lastKeyPhrase < 5000)
            //{
                //recognizer.cancel();
                //recognizer.startListening(intent);
            //}
            //else {
                recognizer.stopListening();
                mContext.buttonOff();
            //}
        }
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.w(TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.w(TAG, "onPartialResults: " + matches);
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.w(TAG, "onReadyForSpeech");
        mContext.buttonOn();
    }

    @Override
    public void onEndOfSpeech() {
        Log.w(TAG, "onEndOfSpeech");
        //buttonOff();
    }

    @Override
    public void onResults(Bundle results) {
        Log.w(TAG, "onResults");

        recognizer.stopListening();

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null)
            mContext.process(matches.toArray(new String[matches.size()]));
        else {
            mContext.speak("Повтори!");
            mContext.buttonOff();
        }
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
}