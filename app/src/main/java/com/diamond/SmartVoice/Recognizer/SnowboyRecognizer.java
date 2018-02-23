package com.diamond.SmartVoice.Recognizer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.diamond.SmartVoice.MainActivity;

import java.io.IOException;

import ai.kitt.snowboy.AppResCopy;

import ai.kitt.snowboy.Constants;
import ai.kitt.snowboy.audio.AudioDataSaver;
import ai.kitt.snowboy.audio.RecordingThread;

public class SnowboyRecognizer extends AbstractRecognizer {

    private MainActivity mContext;

    private static final String TAG = SnowboyRecognizer.class.getSimpleName();

    private int preVolume = -1;

    private RecordingThread recordingThread;

    private MediaPlayer player = new MediaPlayer();

    public SnowboyRecognizer(MainActivity context) {
        this.mContext = context;
        setProperVolume();
        AppResCopy.copyResFromAssetsToSD(mContext);
        recordingThread = new RecordingThread(new ResultListener(), new AudioDataSaver(), "" + (Integer.parseInt(context.pref.getString("SnowboySensitivity", "70")) * 1f / 100f));
        try {
            player.setDataSource(Constants.DEFAULT_WORK_SPACE+"ding.wav");
            player.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Playing ding sound error", e);
        }
    }

    private void setMaxVolume() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(audioManager == null)
            return;
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
    }

    private void setProperVolume() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(audioManager == null)
            return;
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int properVolume = (int) ((float) maxVolume * 0.2);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, properVolume, 0);
    }

    private void restoreVolume() {
        if (preVolume >= 0) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if(audioManager == null)
                return;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preVolume, 0);
        }
    }

    public void startListening() {
        recordingThread.startRecording();
        Log.d(TAG, " ----> recording started ...");
    }

    public void stopListening() {
        recordingThread.stopRecording();
        Log.d(TAG, " ----> recording stopped ");
    }

    public void destroy() {
        restoreVolume();
        recordingThread.stopRecording();
    }

    public class ResultListener
    {
        public void onResult() {
            if (mContext.recognizer instanceof YandexRecognizer)
                player.start();
            mContext.OnKeyPhrase();
        }
    }
}