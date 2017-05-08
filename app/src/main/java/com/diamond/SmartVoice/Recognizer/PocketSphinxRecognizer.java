package com.diamond.SmartVoice.Recognizer;

import android.util.Log;
import android.widget.Toast;

import com.diamond.SmartVoice.MainActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class PocketSphinxRecognizer {

    private static final String KEY_PHRASE_SEARCH = "wakeup";
    public final String KEYPHRASE;
    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private static final String TAG = PocketSphinxRecognizer.class.getSimpleName();
    private MainActivity mContext;

    public PocketSphinxRecognizer(MainActivity context) {
        this.mContext = context;

        KEYPHRASE = context.keyphrase;

        try {
            Assets assets = new Assets(mContext);
            File assetDir = assets.syncAssets();
            Grammar grammar = new Grammar(new PhonMapper());
            grammar.addWords(KEYPHRASE);
            File dict = new File(assetDir, "commands.dic");
            FileUtils.writeStringToFile(dict, grammar.getDict(), "UTF8");
            SpeechRecognizerSetup setup = defaultSetup();
            setup.setAcousticModel(new File(assetDir, "dict"));
            setup.setDictionary(dict);
            setup.setKeywordThreshold(Float.valueOf(context.pref.getString("keywordThreshold", "1e-15f")));
            recognizer = setup.getRecognizer();
            recognizer.addListener(new PocketSphinxRecognitionListener());
            recognizer.addKeyphraseSearch(KEY_PHRASE_SEARCH, KEYPHRASE);
        } catch (Exception e) {
            e.printStackTrace();
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Failed to init recognizer ", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            recognizer = null;
        }
    }

    public void startListening() {
        if (recognizer == null)
            return;
        recognizer.stop();
        recognizer.startListening(KEY_PHRASE_SEARCH);
    }

    public void stopListening() {
        if (recognizer == null)
            return;
        recognizer.stop();
    }

    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null)
                return;
            String text = hypothesis.getHypstr();
            if (KEY_PHRASE_SEARCH.equals(recognizer.getSearchName()))
                if (text.contains(KEYPHRASE)) {
                    mContext.OnKeyPhrase();
                }
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