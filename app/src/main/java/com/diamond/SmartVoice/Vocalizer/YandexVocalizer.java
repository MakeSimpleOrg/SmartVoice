package com.diamond.SmartVoice.Vocalizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.Utils;

import ru.yandex.speechkit.Emotion;
import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Language;
import ru.yandex.speechkit.OnlineVocalizer;
import ru.yandex.speechkit.Synthesis;
import ru.yandex.speechkit.Vocalizer;
import ru.yandex.speechkit.VocalizerListener;
import ru.yandex.speechkit.Voice;

/**
 * @author Dmitriy Ponomarev
 */
public class YandexVocalizer extends AbstractVocalizer implements VocalizerListener {
    private static final String TAG = YandexVocalizer.class.getSimpleName();

    private static final String API_KEY = "6d5c4e48-3c78-434e-96a2-2ecea92d8120";

    private OnlineVocalizer tts;

    public YandexVocalizer(MainActivity context) {
        Language lang = Utils.getYandexLanguage(context.pref.getString("YandexSpeechLang", "None"));
        Voice voice = Utils.getYandexVoice(context.pref.getString("YandexVoice", "None"));
        Emotion emotion = Utils.getYandexEmotion(context.pref.getString("YandexEmotion", "None"));
        Log.w(TAG, "Start Vocalizer, lang: " + lang + ", voice: " + voice + ", emotion: " + emotion);
        tts = new OnlineVocalizer.Builder(lang, this)
                .setEmotion(emotion)
                .setVoice(voice)
                .build();
        tts.prepare();
        context.onVocalizerLoaded();
    }

    @Override
    public void onSynthesisDone(@NonNull Vocalizer vocalizer) {
    }

    @Override
    public void onPartialSynthesis(@NonNull Vocalizer vocalizer, @NonNull Synthesis synthesis) {

    }

    @Override
    public void onPlayingBegin(@NonNull Vocalizer vocalizer) {

    }

    @Override
    public void onPlayingDone(@NonNull Vocalizer vocalizer) {

    }

    @Override
    public void onVocalizerError(@NonNull Vocalizer vocalizer, @NonNull Error error) {
        Log.w(TAG, error.getMessage());
    }

    public void speak(String text) {
        tts.synthesize(text, Vocalizer.TextSynthesizingMode.APPEND);
    }

    @Override
    public void destroy() {
        if (tts != null) {
            tts.cancel();
            tts.destroy();
            tts = null;
        }
    }
}
