package com.diamond.SmartVoice.Recognizer;

import android.util.Log;
import android.widget.Toast;

import com.diamond.SmartVoice.MainActivity;
import com.diamond.SmartVoice.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * @author Dmitriy Ponomarev
 */
public class PocketSphinxRecognizer extends AbstractRecognizer {
    private static final String KEY_PHRASE_SEARCH = "wakeup";
    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private static final String TAG = PocketSphinxRecognizer.class.getSimpleName();
    private MainActivity mContext;

    public PocketSphinxRecognizer(MainActivity context) {
        this.mContext = context;
        String KEYPHRASE = context.PocketSphinxKeyPhrase;
        try {
            Grammar grammar = new Grammar(new PhonMapper());
            grammar.addWords(KEYPHRASE);
            File dict = new File(Utils.assetDir, "commands.dic");
            Utils.writeStringToFile(dict, grammar.getDict());
            SpeechRecognizerSetup setup = defaultSetup();
            setup.setAcousticModel(new File(Utils.assetDir, "dict"));
            setup.setDictionary(dict);
            setup.setKeywordThreshold(Float.valueOf("1e-" + (100-Integer.parseInt(context.pref.getString("PocketSphinxSensitivity", "95"))) + "f"));
            //setup.setBoolean("-remove_noise", false);
            recognizer = setup.getRecognizer();
            recognizer.addListener(new PocketSphinxRecognitionListener());
            recognizer.addKeyphraseSearch(KEY_PHRASE_SEARCH, KEYPHRASE);
        } catch (Exception e) {
            e.printStackTrace();
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContext.show("Failed to init recognizer ");
                }
            });
        }
    }

    public void startListening() {
        if (recognizer == null)
            return;
        try {
            recognizer.stop();
            recognizer.startListening(KEY_PHRASE_SEARCH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopListening() {
        if (recognizer == null)
            return;
        try {
            recognizer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            recognizer = null;
        }
    }

    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
            if (mContext.recognizer instanceof YandexRecognizer)
                mContext.OnKeyPhrase();
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            //Log.d(TAG, "onPartialResult");
            if (hypothesis == null)
                return;
            Log.d(TAG, "onPartialResult: " + hypothesis.getHypstr());
            if (KEY_PHRASE_SEARCH.equals(recognizer.getSearchName()))
                if (!(mContext.recognizer instanceof YandexRecognizer))
                    mContext.OnKeyPhrase();
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        public void onError(Exception error) {
            Log.d(TAG, "onError: " + error.toString());
        }

        @Override
        public void onTimeout() {
        }
    }
}