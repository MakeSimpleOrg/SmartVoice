package com.diamond.SmartVoice.Recognizer;

import android.util.Log;
import android.widget.Toast;

import com.diamond.SmartVoice.MainActivity;

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
public class PocketSphinxRecognizer {
    private static final String KEY_PHRASE_SEARCH = "wakeup";
    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private static final String TAG = PocketSphinxRecognizer.class.getSimpleName();
    private MainActivity mContext;

    public PocketSphinxRecognizer(MainActivity context) {
        this.mContext = context;
        String KEYPHRASE = context.keyphrase;
        try {
            Assets assets = new Assets(mContext);
            File assetDir = assets.syncAssets();
            Grammar grammar = new Grammar(new PhonMapper());
            grammar.addWords(KEYPHRASE);
            File dict = new File(assetDir, "commands.dic");
            writeStringToFile(dict, grammar.getDict());
            SpeechRecognizerSetup setup = defaultSetup();
            setup.setAcousticModel(new File(assetDir, "dict"));
            setup.setDictionary(dict);
            setup.setKeywordThreshold(Float.valueOf("1e-" + context.pref.getString("keywordThreshold", "5") + "f"));
            //setup.setBoolean("-remove_noise", false);
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

    private static void writeStringToFile(final File file, final String data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            out.write(data.getBytes(Charset.forName("UTF8")));
            out.close();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (final IOException ignored) {
            }
        }
    }

    private static FileOutputStream openOutputStream(final File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory())
                throw new IOException("File '" + file + "' exists but is a directory");
            if (!file.canWrite())
                throw new IOException("File '" + file + "' cannot be written to");
        } else {
            final File parent = file.getParentFile();
            if (parent != null)
                if (!parent.mkdirs() && !parent.isDirectory())
                    throw new IOException("Directory '" + parent + "' could not be created");
        }
        return new FileOutputStream(file, false);
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

    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null)
                return;
            if (KEY_PHRASE_SEARCH.equals(recognizer.getSearchName()))
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