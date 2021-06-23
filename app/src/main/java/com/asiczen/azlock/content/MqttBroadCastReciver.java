package com.asiczen.azlock.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.asiczen.azlock.net.MqttMessageService;
import com.asiczen.azlock.net.MqttReceiveListener;

import static com.android.volley.VolleyLog.TAG;

public class MqttBroadCastReciver extends BroadcastReceiver {

    private final MqttReceiveListener mqttReceiveListener;

    public MqttBroadCastReciver(Context context, MqttReceiveListener mqttReceiveListener)
    {
        //this.mContext=context;
        this.mqttReceiveListener=mqttReceiveListener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action){
                case MqttMessageService.MQTT_CONNECTED:
                    mqttReceiveListener.onConnect();
                    break;
                case MqttMessageService.MQTT_DISCONNECTED:
                    mqttReceiveListener.onDisconnect();
                    break;
                case MqttMessageService.MQTT_DATA_AVAILABLE:
                    Log.d(TAG, "broadcast onReceive: MQTT_DATA_AVAILABLE");
                    final byte[] data = intent.getByteArrayExtra(MqttMessageService.EXTRA_DATA);
                    mqttReceiveListener.onDataAvailable(data);
                    break;
                case MqttMessageService.MQTT_DELEVIERY_COMPLETED:
                    mqttReceiveListener.onDelivery();
                    break;
                case MqttMessageService.MQTT_UNABLE_TO_PUBLISH:
                    mqttReceiveListener.unableToPublish();
                    break;
                case MqttMessageService.MQTT_UNABLE_TO_SUBSCRIBE:
                    mqttReceiveListener.unableToSubscribe();
                    break;
                case MqttMessageService.MQTT_USUBSCRIBE:
                    mqttReceiveListener.subscribed();
                    break;
                case MqttMessageService.MQTT_UN_SCBSCRIBE:
                    mqttReceiveListener.succOrFailToUnSubscribe();
                    break;
            }
        }
    }
}
