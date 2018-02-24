package ai.kitt.snowboy;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.diamond.SmartVoice.Recognizer.SnowboyRecognizer.ResultListener;
import com.diamond.SmartVoice.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordingThread {
    static {
        System.loadLibrary("snowboy-detect-android");
    }

    private static final String TAG = RecordingThread.class.getSimpleName();

    private static final int SAMPLE_RATE = 16000;

    private boolean shouldContinue;
    private ResultListener handler = null;
    private Thread thread;

    private SnowboyDetect detector;

    public RecordingThread(ResultListener handler, String model, String sensitivity) {
        this.handler = handler;
        String path = Utils.assetDir.getAbsolutePath() + File.separatorChar + "snowboy" + File.separatorChar;
        System.out.println("path: " + path);
        detector = new SnowboyDetect(path + "common.res", path + model);
        detector.SetSensitivity(sensitivity);
        //-detector.SetAudioGain(1);
        detector.ApplyFrontend(true);
    }

    public void startRecording() {
        if (thread != null)
            return;

        shouldContinue = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        thread.start();
    }

    public void stopRecording() {
        if (thread == null)
            return;
        shouldContinue = false;
        thread = null;
    }

    private void record() {
        Log.v(TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Buffer size in bytes: for 0.1 second of audio
        int bufferSize = (int) (SAMPLE_RATE * 0.1 * 2);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        byte[] audioBuffer = new byte[bufferSize];
        AudioRecord record = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(TAG, "Start recording");

        long shortsRead = 0;
        detector.Reset();
        while (shouldContinue) {
            record.read(audioBuffer, 0, audioBuffer.length);

            // Converts to short array.
            short[] audioData = new short[audioBuffer.length / 2];
            ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData);

            shortsRead += audioData.length;

            // Snowboy hotword detection.
            int result = detector.RunDetection(audioData, audioData.length);
            if (result > 0) {
                Log.i("Snowboy: ", "Hotword " + Integer.toString(result) + " detected!");
                handler.onResult();
            }
        }

        record.stop();
        record.release();

        Log.v(TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }
}