package com.diamond.SmartVoice.Vocalizer;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import com.diamond.SmartVoice.MainActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class GoogleVocalizer extends AbstractVocalizer implements TextToSpeech.OnInitListener {
    private MainActivity mContext;

    private TextToSpeech tts;

    public GoogleVocalizer(MainActivity context) {
        this.mContext = context;
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR && tts.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE)
            tts.setLanguage(Locale.getDefault());
        mContext.onVocalizerLoaded();
    }

    public void speak(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, UUID.randomUUID().toString());
        } else {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                params.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
            tts.speak(text, TextToSpeech.QUEUE_ADD, params);
        }
    }

    @Override
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}