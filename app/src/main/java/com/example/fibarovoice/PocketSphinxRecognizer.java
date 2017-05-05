package com.diamond.SmartVoice;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class PocketSphinxRecognizer {
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    /* Keyword we are looking for to activate menu */
    public static final String KEYPHRASE = "умный дом";
    private edu.cmu.pocketsphinx.SpeechRecognizer mPocketSphinxRecognizer;
    private static final String TAG = PocketSphinxRecognizer.class.getSimpleName();
    private MainActivity mContext;

    public PocketSphinxRecognizer(MainActivity context) {
        this.mContext = context;
    }

    public void initPocketSphinx() {

        //new AsyncTask<Void, Void, Exception>() {
        //    @Override
        //    protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mContext);

                    //Performs the synchronization of assets in the application and external storage
                    File assetDir = assets.syncAssets();

                    //Creates a new SpeechRecognizer builder with a default configuration
                    SpeechRecognizerSetup speechRecognizerSetup = defaultSetup();

                    //Set Dictionary and Acoustic Model files
                    //speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    //speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "ru-ru-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "ru.dic"));

                    // Threshold to tune for keyphrase to balance between false positives and false negatives
                    speechRecognizerSetup.setKeywordThreshold(1e-15f);

                    //Creates a new SpeechRecognizer object based on previous set up.
                    mPocketSphinxRecognizer = speechRecognizerSetup.getRecognizer();

                    mPocketSphinxRecognizer.addListener(new PocketSphinxRecognitionListener());

                    // Create keyword-activation search.
                    mPocketSphinxRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

                    restartSearch();
                    //            Toast.makeText(mContext, "Скажите: " + KEYPHRASE, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Failed to init mPocketSphinxRecognizer ", Toast.LENGTH_SHORT).show();
                }
    }

    public void destroy() {
        if (mPocketSphinxRecognizer != null) {
            mPocketSphinxRecognizer.cancel();
            mPocketSphinxRecognizer.shutdown();
            mPocketSphinxRecognizer = null;
        }
    }

    public void restartSearch() {
        if(mPocketSphinxRecognizer == null)
            return;
        mPocketSphinxRecognizer.stop();
        mPocketSphinxRecognizer.startListening(KWS_SEARCH);
    }

    public void stopSearch() {
        if(mPocketSphinxRecognizer == null)
            return;
        mPocketSphinxRecognizer.stop();
    }

    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {}

        /**
         * In partial result we get quick updates about current hypothesis. In
         * keyword spotting mode we can react here, in other modes we need to wait
         * for final result in onResult.
         */
        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null)
            {
                //Log.d(TAG,"onPartialResult: null");
                return;
            }

            String text = hypothesis.getHypstr();
            Log.d(TAG, "onPartialResult: " + text);
            if (text.contains(KEYPHRASE)) {
                mPocketSphinxRecognizer.cancel();
                //Toast.makeText(mContext, "You said: "+text, Toast.LENGTH_SHORT).show();
                mContext.OnResult(text);
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            if(hypothesis == null)
            {
                //Log.d(TAG, "onResult: null");
                return;
            }
            String text = hypothesis.getHypstr();
            Log.d(TAG, "onResult: " + text);
        }

        /**
         * We stop mPocketSphinxRecognizer here to get a final result
         */
        @Override
        public void onEndOfSpeech() {}

        public void onError(Exception error) {}

        @Override
        public void onTimeout() {}
    }

    public interface OnResultListener
    {
        public void OnResult(String command);
    }
}