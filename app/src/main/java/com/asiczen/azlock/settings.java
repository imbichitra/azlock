package com.asiczen.azlock;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.asiczen.azlock.content.MySharedPreferences;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;


//import static com.asiczen.azlock.WifiConnectActivity.progressDialog;

public class settings extends AppCompatActivity implements OnClickListener, HttpAsyncTask.AsyncResponse {
    private Context mContext;
    private AppContext appContext;
    private final String TAG = settings.class.getSimpleName();
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private AlertDialog pdialog;
    private boolean isReceiverRegistered;
    private IntentFilter intentFilter;
    private SessionManager sessionManager;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;

    private TextView dialogTextView;
    IntentFilter mIntentFilter;
    RadioGroup sound_toggle, shake_toggle,pin_toggle;
    RadioButton sound_off, sound_on,shake_off, shake_on, pin_off, pin_on;

    private MySharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*ActionBar actionBar = getSupportActionBar();
        setContentView(R.layout.main2);
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Settings</font>"));
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }*/
        setContentView(R.layout.main2);
        sharedPreferences = new MySharedPreferences(this);
        init();
        mContext = this;
        appContext = AppContext.getContext();
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        //private Switch pinonoff;
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, mContext, wifiBroadcastListener);
        //pinonoff = findViewById(R.id.checkBox1);
        //Switch soundonoff = findViewById(R.id.checkBox2);
        //Switch shake = findViewById(R.id.checkBox3);
        /*,savednetwork*/
        TextView changepin = findViewById(R.id.text);
        //savednetwork = findViewById(R.id.text1);
        //TextView bridge = findViewById(R.id.text2);
        //TextView bridge_regd = findViewById(R.id.bridge_regd);
        TextView login = findViewById(R.id.text3);
        //TextView add_bridge_list = findViewById(R.id.add_bridge_list);

        //private WifiInfo mWifiInfo;
        // private ProgressDialog pDialog;
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        /*pDialog = new ProgressDialog(mContext);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setIndeterminate(true);
        pDialog.setCancelable(true);*/
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView = getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent, false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        pdialog = builder.create();

        setPinEvent();

        setShakeEvent();

        setLockSound();
        changepin.setOnClickListener(new View.OnClickListener() {

                                         @Override
                                         public void onClick(View view) {
                                             Intent i = new Intent(settings.this, AskPinActivity.class);
                                             i.putExtra("pinFlag", Utils.CHANGE_PIN_FLAG);
                                             i.putExtra("flag", "1");
                                             startActivity(i);
                                         }
                                     }

        );

        login.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //userlogin.issueRaise="NO";
                loginData.setUserId(sharedPreferences.getEmail());
                loginData.setPassword(sharedPreferences.getPassword());
                Intent intent = new Intent(settings.this, Resetactivity.class);
                startActivity(intent);

            }
        });


    }

    private void setPinEvent() {
        if (appContext.shouldAskPin()) {
            //pinonoff.setChecked(true);
            pin_on.setChecked(true);
        } else
            pin_off.setChecked(true);

        /*pinonoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean on) {
                String msg, buttonText;
                if (pinonoff.getTag() != null) {
                    pinonoff.setTag(null);
                    return;
                }
                if (on) {
                    //Do something when Switch button is on/checked
                    msg = "By enabling this feature, the App will ask every time for pin on start. Are you sure you want to enable this feature?";
                    buttonText = "Enable";
                    new AlertDialog.Builder(mContext).setTitle("Update Security")
                            .setCancelable(false)
                            .setMessage(msg)
                            .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    appContext.updateAskPinStatus(mContext, true);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    appContext.updateAskPinStatus(mContext, false);
                                    pinonoff.setTag("TAG");
                                    pinonoff.setChecked(false);
                                    dialog.dismiss();
                                }
                            }).create().show();
                } else {
                    //Do something when Switch is off/unchecked
                    msg = "By disabling this feature, the App will never ask for pin and whoever has your phone can access azLock. Are you sure you want to disable this feature?";
                    buttonText = "Disable";
                    new AlertDialog.Builder(mContext).setTitle("Update Security")
                            .setCancelable(false)
                            .setMessage(msg)
                            .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    appContext.updateAskPinStatus(mContext, false);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    appContext.updateAskPinStatus(mContext, true);
                                    pinonoff.setTag("TAG");
                                    pinonoff.setChecked(true);
                                    dialog.dismiss();
                                }
                            }).create().show();
                }

            }
        });*/

        pin_toggle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String msg,buttonText;
                if (pin_off.getTag() != null) {
                    pin_off.setTag(null);
                    return;
                }
                switch (checkedId) {
                    case R.id.pin_off:
                        msg = "By disabling this feature, the App will never ask for pin and whoever has your phone can access azLock. Are you sure you want to disable this feature?";
                        buttonText = "Disable";
                        new AlertDialog.Builder(mContext).setTitle("Update Security")
                                .setCancelable(false)
                                .setMessage(msg)
                                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appContext.updateAskPinStatus(mContext, false);
                                        pin_off.setChecked(true);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appContext.updateAskPinStatus(mContext, true);
                                        dialog.dismiss();
                                        pin_off.setTag("TAG");
                                        pin_on.setChecked(true);
                                    }
                                }).create().show();
                        break;
                    case R.id.pin_on:
                        msg = "By enabling this feature, the App will ask every time for pin on start. Are you sure you want to enable this feature?";
                        buttonText = "Enable";
                        new AlertDialog.Builder(mContext).setTitle("Update Security")
                                .setCancelable(false)
                                .setMessage(msg)
                                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appContext.updateAskPinStatus(mContext, true);
                                        pin_on.setChecked(true);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appContext.updateAskPinStatus(mContext, false);
                                        dialog.dismiss();
                                        pin_off.setTag("TAG");
                                        pin_off.setChecked(true);
                                    }
                                }).create().show();
                        break;
                }
            }
        });
    }

    private void setLockSound() {
        appContext.checkPlaySoundOnLockUnlock(mContext);
        Log.d(TAG, "setLockSound: " + appContext.shouldPlaySound());
        if (appContext.shouldPlaySound()) {
            //soundonoff.setChecked(true);
            sound_on.setChecked(true);

        } else {
            //soundonoff.setChecked(false);
            sound_off.setChecked(true);
        }

        /*soundonoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSelectPlaySound(!appContext.shouldPlaySound());
            }
        });*/

        sound_toggle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.sound_off:
                        onSelectPlaySound(false);
                        break;
                    case R.id.sound_on:
                        onSelectPlaySound(true);
                        break;
                }
            }
        });
    }

    private void setShakeEvent() {
        if (appContext.shakeonOff(this)) {
            //shake.setChecked(true);
            shake_on.setChecked(true);
        } else {
            //shake.setChecked(false);
            shake_off.setChecked(true);
        }

        /*shake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    appContext.setShakeToFile(mContext,"1");
                }
                else {

                    appContext.setShakeToFile(mContext,"0");
                }
            }
        });*/
        shake_toggle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.shake_off:
                        appContext.setShakeToFile(mContext, "0");
                        break;
                    case R.id.shake_on:
                        appContext.setShakeToFile(mContext, "1");
                        break;
                }
            }
        });
    }

    private void init() {
        sound_toggle = findViewById(R.id.sound_toggle);
        sound_off = findViewById(R.id.sound_off);
        sound_on = findViewById(R.id.sound_on);

        shake_toggle = findViewById(R.id.shake_toggle);
        shake_off = findViewById(R.id.shake_off);
        shake_on = findViewById(R.id.shake_on);

        pin_toggle = findViewById(R.id.pin_toggle);
        pin_off = findViewById(R.id.pin_off);
        pin_on = findViewById(R.id.pin_on);
    }

    /*private void sendDataToServer(String bridge_id, String bridge_mac) {
        dialogTextView.setText(R.string.connecting);
        pdialog.show();
        JSONObject object = new JSONObject();
        String url = "https://13.127.109.11/Azlock/insert_bridge_data.php";
        try {
            object.put("bridge_id", bridge_id);
            object.put("bridge_mac", bridge_mac);

        } catch (Exception e) {
            e.printStackTrace();
        }
        HttpAsyncTask.context = this;
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        Log.d(TAG, "hello " + object.toString());
        httpTask.execute(url, object.toString());
    }*/

    @Override
    protected void onStart() {
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if (logoutBroadcastReceiver != null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void onBackPressed() {
        sessionManager.logout();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (wifiBroadcastReceiver != null && isReceiverRegistered) {
            Log.d(TAG, "wifi receiver unregistered");
            isReceiverRegistered = false;
            unregisterReceiver(wifiBroadcastReceiver);
        }
        super.onPause();
    }


    private void onSelectPlaySound(boolean playSound) {
        appContext.setPlaySoundOnLockUnlock(mContext, playSound);
        appContext.checkPlaySoundOnLockUnlock(mContext);
    }

    private boolean isConnecting;

    /*private void connectWifi() {

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            // Do whatever
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();
            ConfigureBridgeActivity.setBridgeSsid(ssid);
            startActivity(new Intent(settings.this, ConfigureBridgeActivity.class));
        } else startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        *//*registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        String bridgeSsid = "swastik123";
        String bridgePassword = "swastik123";
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + bridgeSsid + "\"";
        wc.preSharedKey = "\"" + bridgePassword + "\"";
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        int res = mWifiManager.addNetwork(wc);
        if(res == -1)
        {
            Log.d(TAG,"invalid ssid or ssid not present");
            //Toast.makeText(ConnectActivity.this,"Invalid ssid or password",Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d("WifiPreferFe", "add Network returned " + res);
            boolean isNetworkEnabled = mWifiManager.enableNetwork(res, true);
            Log.d(TAG, "connectWifi: "+isNetworkEnabled);
        }*//*
    }*/

    private final OnBroadcastListener wifiBroadcastListener = new OnBroadcastListener() {
        @Override
        public void onReceive(int resultCode, Object result) {
            Log.w(TAG, "onReceive: ");
            if (resultCode == OnBroadcastListener.CONNECTED_WIFI_INFO) {
                String connectedBSSID = (String) result;
                Log.w(TAG, "onUpdate/SUPPLICANT_CONNECTED:" + connectedBSSID);
            } else if (resultCode == OnBroadcastListener.ERROR_AUTHENTICATING) {
                Log.w(TAG, "onUpdate/Authentication Error");
                if (pdialog != null && pdialog.isShowing()) {
                    pdialog.dismiss();
                }
                Snackbar.make(findViewById(android.R.id.content), "Authentication Failed", Snackbar.LENGTH_SHORT).show();
            } else if (resultCode == OnBroadcastListener.CONNECTIVITY_CHANGED) {
                boolean isConnected = (Boolean) result;
                Log.w(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED:" + isConnected);
                //ConfigureBridgeActivity.isWifiConnected = isConnected;
                //mWifiInfo = mWifiManager.getConnectionInfo();

                if (isConnected) {
                    if (pdialog != null && pdialog.isShowing()) {
                        pdialog.dismiss();
                    }
                    /*if(pDialog != null && pDialog.isShowing()) {
                        pDialog.dismiss();
                    }*/
                    Log.w(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED/Starting Bridge Config");
                    if (isConnecting) {
                        isConnecting = false;
                        //ConfigureBridgeActivity.setBridgeSsid(bridgeSsid);
                        Intent intent = new Intent(settings.this, ConfigureBridgeActivity.class);
                        startActivity(intent);
                    }
                }
            }
        }
    };

    @Override
    public void onClick(View view) {

    }

    @Override
    public void processFinish(String output, int errorCode) {
        pdialog.dismiss();
        if (output.equals("Y")) {
            Toast.makeText(this, "Bridge registered successfully", Toast.LENGTH_SHORT).show();
        } else if (output.equals("T")) {
            Toast.makeText(mContext, "Connection time out", Toast.LENGTH_SHORT).show();
        } else {
            String message = null;
            switch (errorCode) {
                case 0:
                    message = "Bridge id already registered";
                    break;
                case 1:
                    message = "MAC already registered";
                    break;
                case 3:
                    message = "Unable to insert data";
                    break;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void bridgeActivity(View view) {
        Intent intent = new Intent(this, BridgeActivity.class);
        intent.putExtra(Utils.STATUS, "SETTING");
        startActivity(intent);
    }

    public void goBack(View view) {
        super.onBackPressed();
    }
}
