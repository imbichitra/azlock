package com.asiczen.azlock;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.Notification;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.SmsDeliveryReportBroadcastReceiver;
import com.asiczen.azlock.content.SmsSentBroadcastReceiver;
import com.asiczen.azlock.util.SmsSender;
import com.asiczen.azlock.util.Utils;

/**
 * Created by Somnath on 12/14/2016.
 */

public class NotRegisteredActivity extends AppCompatActivity {

    private Context mContext;
    private AppContext appContext;
    private TextView errorTextView;
    private SmsSender smsSender;
    private SmsSentBroadcastReceiver smsSentBroadcastReceiver;
    private SmsDeliveryReportBroadcastReceiver smsDeliveryReportBroadcastReceiver;
    private IntentFilter smsSentIntentFilter, smsReportIntentFilter;
    private boolean isSmsDeliveryReportBroadcastReceiverSet, isSmsSentBroadcastReceiverSet;
    private String ownerPhoneNo;

    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private static final String TAG = NotRegisteredActivity.class.getSimpleName();
    private TextView dialogTextView;
    private AlertDialog dialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_registered);

        mContext = this;
        appContext = AppContext.getContext();
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        if(sessionManager.verify()){
            finish();
        }
        Intent intent = getIntent();
        errorTextView = findViewById(R.id.error_textView);
        ownerPhoneNo = intent.getStringExtra("ownerContact");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();

        smsSender=new SmsSender(this);
        smsSentIntentFilter = new IntentFilter(SmsSender.SENT);
        smsReportIntentFilter = new IntentFilter(SmsSender.DELIVERED);
        smsSentBroadcastReceiver = new SmsSentBroadcastReceiver(this, new SmsSender.SmsStatusListener() {
            @Override
            public void onSmsStatusChange(int resultCode, String message) {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
               dialog.dismiss();
                finish();
            }
        });
        smsDeliveryReportBroadcastReceiver = new SmsDeliveryReportBroadcastReceiver(this, new SmsSender.SmsStatusListener() {
            @Override
            public void onSmsStatusChange(int resultCode, String message) {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                finish();
            }
        });
    }

    public void onClickRequestAccessButton(View v){
        String message = "Hi,\nI would like to get access to your azLock.\nIMEI: " + appContext.getImei();
        if(appContext.getNotificationStatus(Notification.TAMPER) && ownerPhoneNo!=null && !ownerPhoneNo.isEmpty()) {
            dialogTextView.setText(R.string.please_wait);
            dialog.show();
            isSmsDeliveryReportBroadcastReceiverSet=isSmsSentBroadcastReceiverSet=true;

            //---when the SMS has been sent---
            registerReceiver(smsSentBroadcastReceiver, smsSentIntentFilter);

            //---when the SMS has been delivered---
            registerReceiver(smsDeliveryReportBroadcastReceiver, smsReportIntentFilter);
            smsSender.send(ownerPhoneNo, message);
        }
        else{
            errorTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart()
    {
        Log.d(TAG, "onStart");
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        super.onStart();
    }

    protected void onDestroy()
    {
        if(smsSentBroadcastReceiver!=null && isSmsSentBroadcastReceiverSet) {
            unregisterReceiver(smsSentBroadcastReceiver);
        }
        if(smsDeliveryReportBroadcastReceiver!=null && isSmsDeliveryReportBroadcastReceiverSet) {
            unregisterReceiver(smsDeliveryReportBroadcastReceiver);
        }
        if(logoutBroadcastReceiver!=null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        super.onDestroy();
    }

    public void onBackPressed()
    {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.popup_title)
                .setMessage(R.string.popup_message)
                .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.exit();
                    }
                })
                .setNegativeButton(R.string.popup_no, null)
                .show();
    }
}
