package com.asiczen.azlock.content;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Somnath on 12/16/2016.
 */

public class LogoutBroadcastReceiver extends BroadcastReceiver {

    private final Activity activity;

    public LogoutBroadcastReceiver(Activity activity)
    {
        this.activity=activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activity.finish();
    }
}
