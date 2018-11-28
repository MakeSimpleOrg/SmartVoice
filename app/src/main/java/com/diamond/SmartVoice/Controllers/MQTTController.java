package com.diamond.SmartVoice.Controllers;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class MQTTController extends Controller implements MqttCallback, IMqttActionListener {
    private static final String TAG = MQTTController.class.getSimpleName();

    private static final int MAX_RECONNECT_DELAY = 10000;
    private static final int DEFAULT_KEEPALIVE_SECONDS = 30;

    private final MqttConnectOptions connectOptions;
    private MqttAsyncClient pahoClient;
    private final ScheduledExecutorService connectScheduler;
    private int connectDelay;
    private ScheduledFuture<?> connectingFuture;
    protected boolean isConnecting;

    public MQTTController() {
        connectOptions = new MqttConnectOptions();
        connectOptions.setKeepAliveInterval(DEFAULT_KEEPALIVE_SECONDS);
        connectOptions.setConnectionTimeout(5);
        connectOptions.setCleanSession(true);
        connectScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void loadData() {
        try {
            if (pahoClient != null && pahoClient.isConnected()) {
                if (connectingFuture != null)
                    connectingFuture.cancel(false);
                pahoClient.disconnect();
            }
            pahoClient = new MqttAsyncClient(getHost(), UUID.randomUUID().toString(), new MemoryPersistence(), new TimerPingSender());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        pahoClient.setCallback(this);
        scheduleConnect(0);
    }

    synchronized private void scheduleConnect(int delay) {
        if (pahoClient.isConnected())
            return;
        if (connectingFuture != null)
            if (connectingFuture.getDelay(TimeUnit.MILLISECONDS) <= delay)
                return;
            else
                connectingFuture.cancel(false);
        if (!isConnecting) {
            isConnecting = true;
            Log.w(TAG, "Scheduling connect in " + connectDelay + " milliseconds");
            connectingFuture = connectScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        isConnecting = true;
                        connectingFuture = null;
                        pahoClient.connect(connectOptions, null, MQTTController.this);
                    } catch (MqttException e) {
                        isConnecting = false;
                        reconnect();
                    }
                }
            }, connectDelay, TimeUnit.MILLISECONDS);
        }
    }

    protected void subscribe(final String topic) {
        try {
            pahoClient.subscribe(topic, 1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected void publish(final String topic, String data) {
        try {
            pahoClient.publish(topic, data.getBytes(Charset.defaultCharset()), 1, false, null, this);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected void publish(final String topic, byte[] payload) {
        try {
            pahoClient.publish(topic, payload, 1, false, null, this);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return pahoClient.isConnected();
    }

    @Override
    public void onSuccess(IMqttToken iMqttToken) {
        //Log.w(TAG, "Connection to broker established");
        connectDelay = 0;
        isConnecting = false;
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable cause) {
        Log.w(TAG, "Connecting to broker failed: " + cause);
        isConnecting = false;
        reconnect();
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.w(TAG, "Connection lost: " + cause.getMessage());
        isConnecting = false;
        reconnect();
    }

    @Override
    public abstract void messageArrived(String topic, MqttMessage message);

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    private void reconnect() {
        if (connectDelay == 0)
            connectDelay = 1000;
        else
            connectDelay = Math.min(MAX_RECONNECT_DELAY, connectDelay << 1);
        scheduleConnect(connectDelay);
    }
}
