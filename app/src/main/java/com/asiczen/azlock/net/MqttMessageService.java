package com.asiczen.azlock.net;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.asiczen.azlock.app.model.PahoMqttClient;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MqttMessageService extends Service {
    private static final String TAG = MqttMessageService.class.getSimpleName();
    public static final String EXTRA_DATA = "android.nfc.extra.DATA";
    public static final String MQTT_CONNECTED = "android.nfc.extra.MQTT_CONNECTED";
    public static final String MQTT_DISCONNECTED = "android.nfc.extra.MQTT_DISCONNECTED";
    public static final String MQTT_DATA_AVAILABLE = "android.nfc.extra.MQTT_DATA_AVAILABLE";
    public static final String MQTT_DELEVIERY_COMPLETED = "android.nfc.extra.MQTT_DELEVIERY_COMPLETED";
    public static final String MQTT_UNABLE_TO_PUBLISH = "android.nfc.extra.MQTT_UNABLE_TO_PUBLISH";
    public static final String MQTT_UNABLE_TO_SUBSCRIBE = "android.nfc.extra.MQTT_UNABLE_TO_SUBSCRIBE";
    public static final String MQTT_USUBSCRIBE = "android.nfc.extra.MQTT_USUBSCRIBE";
    public static final String MQTT_UN_SCBSCRIBE = "android.nfc.extra.MQTT_UN_SCBSCRIBE";

    private MqttAndroidClient mqttAndroidClient;

    public void connectMqtt(){
        PahoMqttClient pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Packet.PublishTopic.MQTT_BROKER_URL, AppContext.getClientId(this));

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                broadcastUpdate(MqttMessageService.MQTT_CONNECTED);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                broadcastUpdate(MqttMessageService.MQTT_DISCONNECTED);
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) {
                //setMessageNotification(s, new String(mqttMessage.getPayload()));

                String msg = new String(mqttMessage.getPayload(), StandardCharsets.ISO_8859_1);
                Log.d(TAG, "messageArrived: "+msg);
                byte[] b = mqttMessage.getPayload();
                broadcastUpdate(b);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        connectMqtt();
    }
    private final IBinder mBinder = new LocalService();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        Log.d(TAG, "MQTT ONUNBIND: ");
        return super.onUnbind(intent);
    }
    public class LocalService extends Binder {
        public MqttMessageService getService(){return  MqttMessageService.this;}
    }

    public void subcribe(){
        try {
            //pahoMqttClient.subscribe(mqttAndroidClient, "BridgeId_SCAN_RESPONSE", 0);
            IMqttToken token = mqttAndroidClient.subscribe(Utils.getSubscribeTopic(), 0);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.d(TAG, "MQTT SUBSCRIBED 0: ");
                    Log.d(TAG, "Subscribe Successfully " + "BridgeId_SCAN_RESPONSE");
                    broadcastUpdate(MqttMessageService.MQTT_USUBSCRIBE);
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.e(TAG, "Subscribe Failed " + "BridgeId_SCAN_RESPONSE");
                    throwable.printStackTrace();
                    broadcastUpdate(MqttMessageService.MQTT_UNABLE_TO_SUBSCRIBE);
                }
            });
        } catch (MqttException e) {
            broadcastUpdate(MqttMessageService.MQTT_UNABLE_TO_SUBSCRIBE);
            e.printStackTrace();
        }catch (Exception e){
            broadcastUpdate(MqttMessageService.MQTT_UNABLE_TO_SUBSCRIBE);
            e.printStackTrace();
        }

    }

    public void msubscribe(String topic){
        try {
            mqttAndroidClient.subscribe(topic, 0);
            Log.d(TAG, "MQTT SUBSCRIBED 1: ");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void publish(byte[] payload){
        Log.d(TAG, "publish: ");
        MqttMessage message = new MqttMessage(payload);
        message.setId(320);
        message.setRetained(false);
        message.setQos(0);
        try {
            mqttAndroidClient.publish(Utils.getPublishTopic(), message);
            Log.d(TAG, "MQTT DATA PUBLISHED: ");
        } catch (MqttException e) {
            e.printStackTrace();
            broadcastUpdate(MqttMessageService.MQTT_UNABLE_TO_PUBLISH);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void unSubscribe(){

        IMqttToken token;
        try {
            token = mqttAndroidClient.unsubscribe(Utils.getSubscribeTopic());
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.d(TAG, "UnSubscribe Successfully ");
                    Log.d(TAG, "MQTT UNSUBSCRIBED: ");
                    broadcastUpdate(MqttMessageService.MQTT_UN_SCBSCRIBE);
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.e(TAG, "UnSubscribe Failed ");
                    broadcastUpdate(MqttMessageService.MQTT_UN_SCBSCRIBE);
                }
            });
        } catch (MqttException e) {
            broadcastUpdate(MqttMessageService.MQTT_UN_SCBSCRIBE);
            e.printStackTrace();
        }
    }

    public void disconnect()  {
        try {
            IMqttToken mqttToken = mqttAndroidClient.disconnect();
            mqttToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.d(TAG, "MQTT DISCONNECTED: ");
                    Log.d(TAG, "Successfully disconnect");
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.d(TAG, "Failed to disconnect " + throwable.toString());
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(byte[] data) {
        final Intent intent = new Intent(MqttMessageService.MQTT_DATA_AVAILABLE);
        intent.putExtra(EXTRA_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
