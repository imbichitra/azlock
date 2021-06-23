package com.asiczen.azlock.content;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import com.asiczen.azlock.util.SmsSender;

/**
 * Created by Somnath on 12/14/2016.
 */

public class SmsSentBroadcastReceiver extends BroadcastReceiver {

    private final SmsSender.SmsStatusListener smsStatusListener;
    public SmsSentBroadcastReceiver(Context context, SmsSender.SmsStatusListener smsStatusListener){
        this.smsStatusListener=smsStatusListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                smsStatusListener.onSmsStatusChange(Activity.RESULT_OK, "Message sent successfully");
                //Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                smsStatusListener.onSmsStatusChange(SmsManager.RESULT_ERROR_GENERIC_FAILURE, "Generic failure");
                //Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                smsStatusListener.onSmsStatusChange(SmsManager.RESULT_ERROR_NO_SERVICE, "No service");
                //Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                smsStatusListener.onSmsStatusChange(SmsManager.RESULT_ERROR_NULL_PDU, "Null PDU");
                //Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                smsStatusListener.onSmsStatusChange(SmsManager.RESULT_ERROR_RADIO_OFF, "Radio off");
                //Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
