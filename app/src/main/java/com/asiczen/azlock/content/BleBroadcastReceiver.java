package com.asiczen.azlock.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.asiczen.azlock.OnReceiveListener;
import com.asiczen.azlock.net.BleMessagingService;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Somnath on 6/22/2016.
 */
public class BleBroadcastReceiver extends BroadcastReceiver {
    //private Context mContext;
    private final OnReceiveListener mOnReceiveListener;
    private static final String TAG = BleBroadcastReceiver.class.getSimpleName();

    public BleBroadcastReceiver(Context context, OnReceiveListener mOnReceiveListener)
    {
        //this.mContext=context;
        this.mOnReceiveListener=mOnReceiveListener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        //final Intent mIntent = intent;
        //*********************//
        assert action != null;
        if (action.equals(BleMessagingService.ACTION_GATT_CONNECTED)) {
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(TAG, "BLE_CONNECTED_MSG: ["+currentDateTimeString+"]");
            mOnReceiveListener.onConnect();
        }

        //*********************//
        if (action.equals(BleMessagingService.ACTION_GATT_DISCONNECTED)) {
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(TAG, "BLE_DISCONNECTED_MSG: ["+currentDateTimeString+"]");
            mOnReceiveListener.onDisconnect();
        }


        //*********************//
        if (action.equals(BleMessagingService.ACTION_GATT_SERVICES_DISCOVERED)) {
            mOnReceiveListener.onServicesDiscovered();
        }
        //*********************//
        if (action.equals(BleMessagingService.ACTION_DATA_AVAILABLE)) {

            final byte[] txValue = intent.getByteArrayExtra(BleMessagingService.EXTRA_DATA);
            try {
                //String text = new String(txValue, "UTF-8");
                int i = 0;
                StringBuilder sb = new StringBuilder();
                while(i != (txValue != null ? txValue.length : 0))
                {
                    sb.append(String.format("%02X ", txValue[i]));
                    i++;
                }
                String text=sb.toString();
                String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                Log.d(TAG, "BLE_DATA_AVAILABLE["+currentDateTimeString+"]:"+text);
                if (txValue != null) {
                    text = new String(txValue, StandardCharsets.ISO_8859_1);
                }
                mOnReceiveListener.onDataAvailable(text);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }
        }
        //*********************//
        if (action.equals(BleMessagingService.DEVICE_DOES_NOT_SUPPORT_BLE)){
            mOnReceiveListener.onError(1);
        }
    }
}
