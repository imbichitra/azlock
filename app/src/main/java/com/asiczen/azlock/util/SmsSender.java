package com.asiczen.azlock.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

/**
 * Created by Somnath on 12/14/2016.
 */

public class SmsSender {

    private final Context context;


    public static final String SENT = "SMS_SENT";
    public static final String DELIVERED = "SMS_DELIVERED";

    public SmsSender(Context context){
        this.context=context;
    }

    public void send(String phoneNumber, String message)
    {
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    public interface SmsStatusListener
    {
        void onSmsStatusChange(int resultCode, String message);
    }
}
