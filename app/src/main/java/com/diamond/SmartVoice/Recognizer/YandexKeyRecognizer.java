package com.diamond.SmartVoice.Recognizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.Utils;

import java.io.File;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.PhraseSpotter;
import ru.yandex.speechkit.PhraseSpotterListener;

/**
 * @author Dmitriy Ponomarev
 */
public class YandexKeyRecognizer extends AbstractRecognizer implements PhraseSpotterListener {

    private MainActivity mContext;

    private static final String TAG = YandexKeyRecognizer.class.getSimpleName();

    private PhraseSpotter phraseSpotter;

    public YandexKeyRecognizer(MainActivity context) {
        this.mContext = context;
        String path = Utils.assetDir.getAbsolutePath() + File.separatorChar + "yandex";
        //String model = context.pref.getString("YandexKeyPhrase", context.getString(R.string.defaultYandexKeyPhrase)).toLowerCase();
        phraseSpotter = new PhraseSpotter.Builder(path, this).build();
        phraseSpotter.prepare();
    }

    @Override
    public void onPhraseSpotted(@NonNull PhraseSpotter phraseSpotter, @NonNull String s, int i) {
        if (mContext.recognizer instanceof YandexRecognizer)
            Utils.ding.start();
        System.out.println("onPhraseSpotted: " + s + ", i: " + i);
        mContext.OnKeyPhrase();
    }

    @Override
    public void onPhraseSpotterStarted(@NonNull PhraseSpotter phraseSpotter) {
    }

    @Override
    public void onPhraseSpotterError(@NonNull PhraseSpotter phraseSpotter, @NonNull Error error) {
        Log.d(TAG, error.getMessage());
    }

    public void startListening() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return;
        phraseSpotter.start();
        Log.d(TAG, " ----> recording started ...");
    }

    public void stopListening() {
        phraseSpotter.stop();
        Log.d(TAG, " ----> recording stopped ");
    }

    public void destroy() {
        if (phraseSpotter != null) {
            phraseSpotter.stop();
            phraseSpotter.destroy();
            phraseSpotter = null;
        }
    }
}