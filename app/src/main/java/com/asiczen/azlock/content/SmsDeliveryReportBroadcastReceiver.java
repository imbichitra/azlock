package com.asiczen.azlock.content;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.asiczen.azlock.util.SmsSender;

/**
 * Created by Somnath on 12/14/2016.
 */

public class SmsDeliveryReportBroadcastReceiver extends BroadcastReceiver {

    private final SmsSender.SmsStatusListener smsStatusListener;
    public SmsDeliveryReportBroadcastReceiver(Context context, SmsSender.SmsStatusListener smsStatusListener){
        this.smsStatusListener=smsStatusListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                smsStatusListener.onSmsStatusChange(Activity.RESULT_OK, "Message delivered successfully");
                //Toast.makeText(getBaseContext(), "SMS delivered",Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_CANCELED:
                smsStatusListener.onSmsStatusChange(Activity.RESULT_CANCELED, "Message not delivered");
                //Toast.makeText(getBaseContext(), "SMS not delivered",Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
