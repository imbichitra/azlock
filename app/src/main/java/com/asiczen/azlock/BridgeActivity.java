package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.Adapters.RecyclerAdapters;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.CountDownTimer;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BridgeActivity extends AppCompatActivity implements Packet {

    private static final String TAG = BridgeActivity.class.getSimpleName();
    RecyclerView recyclerView;
    ArrayList<Integer> images = new ArrayList<>();
    ArrayList<String> titles = new ArrayList<>();
    private final int CONFIG_BRIDGE = 0;
    private final int ADD_LOCK_TO_BRDIGE = 1;
    private final int ADD_BRDIGE = 2;
    private final int CHANGE_PASSWORD = 3;
    private final int ROUTER_CONFIG = 4;
    private final int REQUEST_BRIDGE_SELECT_DEVICE = 10;
    private final int WIFI_CONFIG = 11;
    private WifiManager mWifiManager;
    private AlertDialog pdialog;
    private TextView dialogTextView;
    private int whichListClicked = -1;
    private NetClientAsyncTask netClientAsyncTask;
    private AlertDialog configRouterAlertDialog;
    private DatabaseHandler databaseHandler;
    private String bridgeId;
    private String password;
    private SharedPreferences sharedpreferences;
    RadioGroup radioGroup;
    RadioButton switch_off,switch_on;
    LinearLayout main_layout;
    private int retryConnection = 0;

    ConnectivityManager cm;
    ConnectivityManager.NetworkCallback networkCallback;
    int wifiID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
        initRecyclerData();
        setTheAdapter();
        createDialog();
        databaseHandler = new DatabaseHandler(this);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        enableWiFi();

        String status = getIntent().getStringExtra(Utils.STATUS);
       /* if (status.equals("CONNECT")) {
            registerReceiver(wifiReciever, getIntentFilter());
            configBridge();
        }*/
        showAlertDialog();
        setBridgeSwitch();
        final SharedPreferences.Editor editor = sharedpreferences.edit();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId)
                {
                    case R.id.toggle_off:
                        editor.putBoolean(Utils.IS_STATIC, false);
                        editor.apply();
                        break;
                    case R.id.toggle_on:
                        /*editor.putBoolean(Utils.IS_STATIC, true);
                        editor.apply();*/
                        Snackbar.make(main_layout, "Static bridge is under implementation", Snackbar.LENGTH_LONG).show();
                        switch_off.setChecked(true);
                        break;
                }
            }
        });
    }

    private void setBridgeSwitch() {
        if(sharedpreferences.getBoolean(Utils.IS_STATIC,false)){
            switch_on.setChecked(true);
        }
        else{
            switch_off.setChecked(true);
        }
    }
    private void showAlertDialog() {
        new AlertDialog.Builder(BridgeActivity.this).setMessage("Make sure your mobile data is off!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    private void enableWiFi() {
        Log.d(TAG, "enableWiFi: ");
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    private void initRecyclerData() {
        //images
        images.add(R.drawable.ic_settings);
        images.add(R.drawable.cloud);
        images.add(R.drawable.add);
        images.add(R.drawable.password);

        //title
        titles.add("Configure bridge");
        titles.add("Add Lock To Bridge");
        titles.add("Add/Delete Bridge");
        titles.add("Change password");
    }

    private void init() {
        recyclerView = findViewById(R.id.add_bridge_list_data);
        sharedpreferences = getSharedPreferences(Utils.BRIDGE_FILE, Context.MODE_PRIVATE);
        radioGroup = findViewById(R.id.toggle);
        switch_off = findViewById(R.id.toggle_off);
        switch_on = findViewById(R.id.toggle_on);
        main_layout = findViewById(R.id.main_layout);
    }

    private void setTheAdapter() {
        RecyclerAdapters mAdapter = new RecyclerAdapters(images, titles, Utils.BRIDGE_OPERATION, R.layout.bridge_list_item);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new RecyclerAdapters.onRecyclerViewItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position) {
                //Toast.makeText(BridgeActivity.this, titles.get(position), Toast.LENGTH_SHORT).show();
                switch (position) {
                    case CONFIG_BRIDGE:
                        whichListClicked = CONFIG_BRIDGE;
                        Log.d(TAG, "onItemClickListener: ");
                        bridgeId = "azBridge";
                        password = "asiczen123";
                        Log.d(TAG, "getCurrentSSID " + getCurrentSSID());
                        if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
                            showWifiDevice();// sendBrokerDetail() is not required as server is registered as iot.asiczen.com
                        } else {
                            retryConnection = 1;
                            connectToBridge(bridgeId, password);
                        }
                        break;
                    case ADD_LOCK_TO_BRDIGE:
                        whichListClicked = ADD_LOCK_TO_BRDIGE;
                        Log.d(TAG, "onItemClickListener: getCurrentSSID " + getCurrentSSID() + "bridgeId: " + bridgeId);
                        if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
                            goToAddLockToBridge();
                        } else {
                            showBridge(ADD_LOCK_TO_BRDIGE);
                        }
                        break;
                    case ADD_BRDIGE:
                        goToAddBridge();
                        break;
                    case CHANGE_PASSWORD:
                        whichListClicked = CHANGE_PASSWORD;
                        if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
                            changePassword();
                        } else {
                            showBridge(CHANGE_PASSWORD);
                        }
                        break;
                }
            }
        });
    }

    /*private void configBridge() {
        if (!getSsid().contains("azBridge")) {
            Log.d(TAG, "configBridge: ");
            mWifiManager.setWifiEnabled(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //enableWiFi();
                    whichListClicked = CONFIG_BRIDGE;
                    showDialog();
                    mWifiManager.startScan();
                }
            }, 500);
            *//*enableWiFi();
            whichListClicked = CONFIG_BRIDGE;
            registerReceiver();
            mWifiManager.startScan();*//*
        } else {
            whichListClicked = CONFIG_BRIDGE;
            showWifiDevice();
        }
    }*/

    BroadcastReceiver wifiReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: called");
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> result = mWifiManager.getScanResults();
                for (ScanResult sc : result) {
                    if (sc.SSID.contains("azBridge")) {
                        bridgeId = sc.SSID;
                        try {
                            unregisterReceiver(wifiReciever);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }

                        connectToBridge(sc.SSID, "asiczen123");
                    }
                    Log.d(TAG, "onReceive123: " + sc.SSID);
                }
            }
            /*if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo =
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "onReceive: bridgeId "+bridgeId +" "+isWifiConnected(bridgeId));
                if (networkInfo.isConnected()) {
                    if (pdialog != null && pdialog.isShowing())
                        pdialog.dismiss();
                    // Wifi is connected
                    if (whichListClicked == ADD_LOCK_TO_BRDIGE) {
                        countDownTimer.cancel();
                        goToAddLockToBridge();
                    } else if (whichListClicked == CONFIG_BRIDGE) {
                        if (getSsid().contains("azBridge")) {
                            countDownTimer.cancel();
                            showWifiDevice();
                        }
                    }else if (whichListClicked == CHANGE_PASSWORD){
                        changePassword();
                    }
                    unregisterReceiver(wifiReciever);
                    //Toast.makeText(BridgeActivity.this, "Wifi is connected:", Toast.LENGTH_SHORT).show();
                }
            }*/
        }
    };

    /*private void sendBrokerDetail() {
        if (pdialog != null && pdialog.isShowing())
            pdialog.dismiss();
        Log.d(TAG, "sendBrokerDetail: ");
        sendBrokerDetail(Utils.brokerIp, 1883, Utils.userId, Utils.password);
    }*/

    private void sendBrokerDetail(String brokerIp, int port, String userId, String password) {
        byte[] packet = new byte[BrockerPacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.BROCKER_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_VISITOR;
        packet[REQUEST_PACKET_LENGTH_POS] = BrockerPacket.SENT_PACKET_LENGTH;
        String[] ip = brokerIp.split("\\.");
        for (int i = 0; i < ip.length; i++)
            packet[i + BrockerPacket.BROCKER_IP_START] = (byte) Integer.parseInt(ip[i]);

        packet[BrockerPacket.BROCKER_PORT_START] = (byte) (port >> 8);
        packet[BrockerPacket.BROCKER_PORT_START + 1] = (byte) (port);

        for (int i = 0; i < userId.length(); i++) {
            packet[i + BrockerPacket.USER_ID_START] = (byte) userId.charAt(i);
        }
        for (int i = 0; i < password.length(); i++) {
            packet[i + BrockerPacket.PASSWORD_START] = (byte) password.charAt(i);
        }


        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:" + value);
                checkConfigPacketStatus(resultCode, value);
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    public String getCurrentSSID() {
        String ssid = null;
        /*ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }*/
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
            ssid = connectionInfo.getSSID();
        }
        return ssid;
    }

    private String getSsid() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = null;
        if (wifiManager != null) {
            info = wifiManager.getConnectionInfo();
        }
        if (info != null) {
            Log.d(TAG, "getSsid: " + info.getSSID());
            return info.getSSID();
        }else {
            return "";
        }
    }

    private void showDialog() {
        //registerReceiver(wifiReciever, getIntentFilter());
        startTimer();
        Log.d(TAG, "registerReceiver: ");
        dialogTextView.setText("Connecting...");
        pdialog.show();
    }

    private void goToAddLockToBridge() {
        startActivity(new Intent(BridgeActivity.this, BridgeLockListActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BRIDGE_SELECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                enableWiFi();
                bridgeId = data.getStringExtra(Utils.BRIDGE_ID);
                password = data.getStringExtra(Utils.BRIDGE_PASSWORD);
                Log.d(TAG, "onActivityResult: " + bridgeId + " " + password);
                if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
                    processData();
                } else {
                    retryConnection = 1;
                    connectToBridge(bridgeId, password);
                }
            }
        }

        if (requestCode == WIFI_CONFIG) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String ssid = data.getStringExtra("SSID");
                String password = data.getStringExtra("PASSWORD");
                Log.d(TAG, "onActivityResult: " + ssid + " " + password);
                if (ssid != null) {
                    doConfigRouter(ssid, password);
                }
            }
        }
    }

    private void startTimer() {
        final int WAIT_TIME = 15000, INTERVAL = 1000;
        CountDownTimer countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                workToDoOnTimerFinish();
            }
        };
        countDownTimer.start();
    }

    private void workToDoOnTimerFinish(){
        Log.d(TAG, "onFinish: " + bridgeId + " getCurrentSSID " + getCurrentSSID());
        if (pdialog != null && pdialog.isShowing())
            pdialog.dismiss();
        if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
            if (whichListClicked == ADD_LOCK_TO_BRDIGE) {
                goToAddLockToBridge();
            } else if (whichListClicked == CONFIG_BRIDGE) {
                if (getSsid().contains("azBridge")) {
                    showWifiDevice();// sendBrokerDetail() is not required as server is registered as iot.asiczen.com
                }else{
                    Log.d(TAG, "workToDoOnTimerFinish: not connected to specified network");
                }
            } else if (whichListClicked == CHANGE_PASSWORD) {
                changePassword();
            } else if (whichListClicked == ROUTER_CONFIG) {
                isRouterConnected();
            }
        } else {
            if (pdialog != null && pdialog.isShowing())
                pdialog.dismiss();
            if (retryConnection == 1 || retryConnection == 2) {
                retryConnection++;
                connectToBridge(bridgeId, password);
            } else {
                String s = "Unable to configure please try again or connect to " + bridgeId + " manually and try again";

                SpannableString ss = new SpannableString(s);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 50, 50 + bridgeId.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                new AlertDialog.Builder(BridgeActivity.this).setMessage(ss)
                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Toast.makeText(BridgeActivity.this, "hello", Toast.LENGTH_SHORT).show();
                                openWifiSettings();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        }
    }

    public void openWifiSettings() {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    private void changeBridgePassword(String oldPassword, String newPassword) {
        byte[] packet = new byte[BridgeParam.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.BRIDGE_PASSWORD_CHANGE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = BridgeParam.SENT_PACKET_LENGTH;

        for (int i = 0; i < oldPassword.length(); i++)
            packet[i + BridgeParam.BRIDGE_SSID_START] = (byte) oldPassword.charAt(i);

        for (int i = 0; i < newPassword.length(); i++) {
            packet[i + BridgeParam.BRIDGE_PASSWORD_START] = (byte) newPassword.charAt(i);
        }
        password = newPassword;
        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:" + value);
                checkConfigPacketStatus(resultCode, value);
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    private void showBridge(int position) {
        List<BridgeDetail> bridgeDetail = databaseHandler.getBridgeData(0);
        if (bridgeDetail.size() == 0) {
            Toast.makeText(this, "Please add atleast one bridge", Toast.LENGTH_SHORT).show();
        } else if (bridgeDetail.size() == 1) {
            //enableWiFi(); //for testing
            String b_id = bridgeDetail.get(0).getBridgeId();
            String pwd = bridgeDetail.get(0).getPassword();
            //Log.d(TAG, "showBridge: " + b_id + " " + pwd);
            //Log.d(TAG, "showBridge: " + isWifiConnected(b_id));
            if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + b_id + "\"")) {
                processData();
            } else {
                Log.d(TAG, "showBridge: connectToBridge");
                bridgeId = b_id;
                password = pwd;
                retryConnection = 1;
                connectToBridge(b_id, pwd);
            }
        } else {
            Intent i = new Intent(this, ShowBridgeListActivity.class);
            startActivityForResult(i, REQUEST_BRIDGE_SELECT_DEVICE);
        }
    }

    private void changePassword() {
        View view1 = getLayoutInflater().inflate(R.layout.change_bridge_password, null, false);
        final EditText currentPasswordEditText = view1.findViewById(R.id.currentPasswordEditText);
        final EditText newPasswordEditText = view1.findViewById(R.id.newPasswordEditText);
        final EditText confirmPasswordEditText = view1.findViewById(R.id.confirmPasswordEditText);
        Button cancel = view1.findViewById(R.id.cancel);
        Button update = view1.findViewById(R.id.update);
        final AlertDialog changePasswordAlertDialog = new AlertDialog.Builder(this)
                .setView(view1)
                .create();
        changePasswordAlertDialog.show();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePasswordAlertDialog.dismiss();
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPassword = currentPasswordEditText.getText().toString().trim();
                String newPassword = newPasswordEditText.getText().toString().trim();
                String confirrmPwd = confirmPasswordEditText.getText().toString().trim();
                boolean isEmpty = TextUtils.isEmpty(oldPassword) && TextUtils.isEmpty(newPassword) && TextUtils.isEmpty(confirrmPwd);
                if (isEmpty) {
                    Toast.makeText(BridgeActivity.this, "Fill the data", Toast.LENGTH_SHORT).show();
                } else {
                    if (newPassword.equals(confirrmPwd)) {
                        if (oldPassword.length() >= 8 && oldPassword.length() <= 15)
                            if (newPassword.length() >= 8 && newPassword.length() <= 15) {
                                changePasswordAlertDialog.dismiss();
                                changeBridgePassword(oldPassword, newPassword);
                            }else
                                Toast.makeText(BridgeActivity.this, "New Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(BridgeActivity.this, "Old Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(BridgeActivity.this, "Confirm Password not match", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goToAddBridge() {
        Intent addBridge = new Intent(this, AddBridgeListActivity.class);
        startActivity(addBridge);
    }

    /*private IntentFilter getIntentFilter() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return mIntentFilter;
    }*/

    private void connectToBridge(String networkSSID, String networkPass) {

        bridgeId = networkSSID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            connectToBrideAboveQ(networkSSID,networkPass);
        }else{
            showDialog();
            connectToBrideBelowQ(networkSSID,networkPass);
        }
    }

    private void connectToBrideBelowQ(String networkSSID, String networkPass){
        Log.d(TAG, "connectToBridge: " + networkSSID.trim() + " " + networkPass);
        /*disableNetworks(networkSSID);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + networkSSID + "\"";
        wifiConfig.preSharedKey = "\"" + networkPass + "\"";
        wifiConfig.status = WifiConfiguration.Status.ENABLED;

        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        int netID= mWifiManager.addNetwork(wifiConfig);
        Log.d(TAG, "connectToWiFi: netID "+netID);
        Log.d(TAG, "connectToWiFi: status "+wifiConfig);
        mWifiManager.enableNetwork(netID,true);
        mWifiManager.saveConfiguration();
        if (netID == -1 ) {
            List<WifiConfiguration> network = mWifiManager.getConfiguredNetworks();
            try {
                for (WifiConfiguration id : network) {
                    Log.d(TAG, "connectToWiFi: "+id.SSID+" "+id.networkId);
                    if (id.SSID.equals("\"" + networkSSID + "\"")) {
                        Log.d(TAG, "connectToWiFi: for");
                        mWifiManager.enableNetwork(id.networkId,true);
                        mWifiManager.saveConfiguration();
                        return;
                    }
                }
            }catch (Exception e){
                Log.e(TAG, "connectToBridge Null pointer exception");
            }

        }else {
            Log.d(TAG, "connectToWiFi: elase");
            mWifiManager.enableNetwork(netID, true);
            mWifiManager.saveConfiguration();

        }*/

        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = String.format("\"%s\"", networkSSID);
        wifiConfiguration.preSharedKey = String.format("\"%s\"", networkPass);
        wifiID = mWifiManager.addNetwork(wifiConfiguration);
        mWifiManager.enableNetwork(wifiID, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToBrideAboveQ(String networkSSID, String networkPass){
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        builder.setSsid(networkSSID);
        builder.setWpa2Passphrase(networkPass);

        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();

        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkRequestBuilder.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);//added for staying connect check
        networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);

        NetworkRequest networkRequest = networkRequestBuilder.build();
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Toast.makeText(BridgeActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onAvailable: "+network);
                workToDoOnTimerFinish();
                cm.bindProcessToNetwork(network);
            }
        };
        cm.requestNetwork(networkRequest, networkCallback);
    }

    void disableWifi(){
        if (cm!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.bindProcessToNetwork(null);
                cm.unregisterNetworkCallback(networkCallback);
                cm = null;
            }
        }

        if(mWifiManager !=null){
            mWifiManager.disableNetwork(wifiID);
        }
    }

    /*private void disableNetworks(String ssid){
        List<WifiConfiguration> network = mWifiManager.getConfiguredNetworks();
        try {
            for (WifiConfiguration id : network){
                Log.d(TAG, "disableNetworks: "+id);
                if (id.SSID.equals("\"" + ssid + "\"")){
                    mWifiManager.enableNetwork(id.networkId,false);
                }else {
                    mWifiManager.disableNetwork(id.networkId);
                }
            }
        }catch (Exception e){
            Log.e(TAG, "disableNetworks Null pointer exception");
        }

    }*/

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView = getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent, false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        pdialog = builder.create();
    }

    /*private boolean isWifiConnected(String ssid) {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivity.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            Log.d(TAG, "checking ssid with connected ssidc " + ssid);
            return wifiInfo.getSSID().equals("\"" + ssid + "\"");
        }
        return false;
    }*/

    private void doConfigRouter(String ssid, String password) {
        try {
            Utils u = new Utils();
            u.requestType = Utils.ROUTER_CONFIG_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[Packet.RouterConfigPacket.SENT_PACKET_LENGTH];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.ROUTER_CONFIG_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = Packet.RouterConfigPacket.SENT_PACKET_LENGTH;

            for (int i = 0; i < ssid.length(); i++)
                packet[i + Packet.RouterConfigPacket.ROUTER_SSID_START] = (byte) ssid.charAt(i);

            for (int i = 0; i < password.length(); i++) {
                packet[i + Packet.RouterConfigPacket.ROUTER_PASSOWRD_START] = (byte) password.charAt(i);
            }
            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            u.setUtilsInfo(u);
            netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                    80,
                    packet, new OnTaskCompleted<String>() {
                @Override
                public void onTaskCompleted(int resultCode, String value) {
                    Log.d(TAG, "onTaskCompleted:" + value);
                    checkConfigPacketStatus(resultCode, value);
                }
            });
            netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
            netClientAsyncTask.execute();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    int i=1;
    private void bridgeParam() {
        Log.e(TAG, "bridgeParam: bridge Param called"+i++ );
        View bridgeParamView = getLayoutInflater().inflate(R.layout.config_router, null, false);
        final TextInputEditText ssidEditText = bridgeParamView.findViewById(R.id.ssid_editText);
        ssidEditText.setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        if(source.equals("")){ // for backspace
                            return source;
                        }
                        if(source.toString().matches("[a-zA-Z0-9]+")){ // here no space character
                            return source;
                        }
                        return "";
                    }
                }
        });
        final TextInputEditText passwordEditText = bridgeParamView.findViewById(R.id.password_editText);
        TextInputLayout textInputLayout = bridgeParamView.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Bridge Id");
        TextView title = bridgeParamView.findViewById(R.id.title);
        final Button cancel = bridgeParamView.findViewById(R.id.cancel);
        final Button next = bridgeParamView.findViewById(R.id.next);
        title.setTextSize(15);
        //String sourceString = "<b>" + "For registration : setting->Bridge registration" + "</b> ";
        title.setText(R.string.changin_bridgeid);
        //TextView errorTextView1 = bridgeParamView.findViewById(R.id.error_textView);
        configRouterAlertDialog = new AlertDialog.Builder(this)
                .setView(bridgeParamView)
                .setCancelable(false)
                .create();
        configRouterAlertDialog.show();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (configRouterAlertDialog !=null && configRouterAlertDialog.isShowing())
                    configRouterAlertDialog.dismiss();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bridgeId = Objects.requireNonNull(ssidEditText.getText()).toString();
                password = Objects.requireNonNull(passwordEditText.getText()).toString();
                Log.d(TAG, "onClick: bridgeId "+bridgeId+" "+password);
                boolean isContain = TextUtils.isEmpty(bridgeId) && TextUtils.isEmpty(password);
                String name = bridgeId.toLowerCase();
                if (!isContain) {
                    if (!name.contains("azbridge")) {
                        if (bridgeId.length() >= 8 && bridgeId.length() <= 15)
                            if (password.length() >= 8 && password.length() <= 15) {
                                //if (!databaseHandler.isBrdigeAContain(bridgeId)) {
                                if (configRouterAlertDialog !=null && configRouterAlertDialog.isShowing())
                                    configRouterAlertDialog.dismiss();
                                sendData(bridgeId, password);
                                /*} else {
                                    Toast.makeText(BridgeActivity.this, "Duplicate bridgeId", Toast.LENGTH_SHORT).show();
                                }*/
                            } else
                                Toast.makeText(BridgeActivity.this, "Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(BridgeActivity.this, "Bridge Id should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BridgeActivity.this, "Bridge is must not be azBridge", Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(BridgeActivity.this, "Fill the data", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void sendData(String bridgeId, String password) {
        byte[] packet = new byte[BridgeParam.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.BRIDGE_PARAM_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = BridgeParam.SENT_PACKET_LENGTH;

        for (int i = 0; i < bridgeId.length(); i++)
            packet[i + BridgeParam.BRIDGE_SSID_START] = (byte) bridgeId.charAt(i);

        for (int i = 0; i < password.length(); i++) {
            packet[i + BridgeParam.BRIDGE_PASSWORD_START] = (byte) password.charAt(i);
        }

        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:" + value);
                checkConfigPacketStatus(resultCode, value);
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    private void checkConfigPacketStatus(int resultCode, String value) {
        if (resultCode == Activity.RESULT_OK) {
            if (value != null) {
                Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                processConfigPacket(value);
            }
        } else {
            if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                Snackbar.make(main_layout, "Unable to communicate with bridge, please try again", Snackbar.LENGTH_LONG).show();

            } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {

                Snackbar.make(main_layout, "Timeout occurred", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void processConfigPacket(String packet) {
        Log.d(TAG, "Received Packet" + packet);
        if (packet != null && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= RouterConfigPacket.RECEIVED_PACKET_LENGTH) {
            try {
                Log.d(TAG, "processConfigPacket: " + Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                //strBytes = packet.getBytes(StandardCharsets.ISO_8859_1);
                char status = packet.charAt(RESPONSE_PACKET_TYPE_POS);
                Log.d(TAG, "processConfigPacket: status"+status);
                if ((status == Utils.ROUTER_CONFIG_REQUEST || status == Utils.BROCKER_REQUEST ||
                        status == Utils.BRIDGE_PARAM_REQUEST || status == Utils.BRIDGE_PASSWORD_CHANGE_REQ ||
                        status == Utils.ROUTER_PARAM_REQUEST || status == Utils.BATTERY_COUNT_REQUEST)
                        && Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        switch (status) {
                            case Utils.BROCKER_REQUEST:
                                //processData();
                                if (getSsid().contains("azBridge")) {
                                    showWifiDevice();
                                }
                                break;
                            case Utils.ROUTER_CONFIG_REQUEST:
                                whichListClicked = ROUTER_CONFIG;
                                dialogTextView.setText("Connecting...");
                                pdialog.show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (pdialog != null && pdialog.isShowing())
                                            pdialog.dismiss();
                                        if (getCurrentSSID() != null && getCurrentSSID().equals("\"" + bridgeId + "\"")) {
                                            isRouterConnected();
                                        } else {
                                            retryConnection = 1;
                                            bridgeId = "azBridge";
                                            password = "asiczen123";
                                            connectToBridge(bridgeId, password);
                                        }
                                    }
                                }, 25000);
                                break;
                            case Utils.BRIDGE_PARAM_REQUEST:
                                BridgeDetail bridgeDetail = new BridgeDetail(bridgeId, password);
                                long i = databaseHandler.insertBridgeData(bridgeDetail);
                                Log.d(TAG, "processConfigPacket: BRIDGE_PARAM_REQUEST " + i);
                                Utils.printByteArray(packet.getBytes(StandardCharsets.ISO_8859_1));
                                Snackbar.make(main_layout, "Bridge configuration successful", Snackbar.LENGTH_LONG).show();
                                //finish();
                                break;
                            case Utils.BRIDGE_PASSWORD_CHANGE_REQ:
                                Log.d(TAG, "processConfigPacket: bridgeId " + bridgeId + " password " + password);
                                //Snackbar.make(main_layout,"Password successfully changed",Snackbar.LENGTH_LONG);
                                Toast.makeText(this, "Password successfully changed", Toast.LENGTH_LONG).show();
                                databaseHandler.updatePassword(bridgeId, password);
                                break;
                            case Utils.BATTERY_COUNT_REQUEST:
                                int lockMacIdStart = LockDetail.LOCK_MAC_ID_START;
                                String lockMacId = Utils.getStringFromHex(packet.substring(lockMacIdStart,
                                        lockMacIdStart + LockDetail.LOCK_MAC_ID_SIZE));
                                lockMacId = Utils.generateMac(lockMacId); // add ":" in mac id

                                String version = packet.charAt(10) + "." +
                                        packet.charAt(11) + "." +
                                        packet.charAt(12);
                                showVersionDialog(lockMacId, version);
                                break;
                            case Utils.ROUTER_PARAM_REQUEST:
                                Log.d(TAG, "processConfigPacket: inside if switch case");
                                //bridgeParam();
                                break;
                        }

                        //Toast.makeText(BridgeActivity.this, "configuration saved", Toast.LENGTH_LONG).show();
                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Toast.makeText(BridgeActivity.this, "Failed to save configuration", Toast.LENGTH_LONG).show();
                        Snackbar.make(main_layout, CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "processConfigPacket: Time out" + Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                    Snackbar.make(main_layout, CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (packet != null) {
                if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.ROUTER_PARAM_REQUEST &&
                        packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                    Log.d(TAG, "processConfigPacket: " + Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                    Log.d(TAG, "processConfigPacket: inside else ");
                    //bridgeParam();
                } else {
                    Log.e(TAG, "Invalid Packet");
                    if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.ROUTER_PARAM_REQUEST && packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Snackbar.make(main_layout, "Please check your router configuration and try again.", Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(main_layout, CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                    }
                }
            } else {
                Snackbar.make(main_layout, "Invalid Packet", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void processData() {
        if (whichListClicked == ADD_LOCK_TO_BRDIGE) {
            goToAddLockToBridge();
        } else if (whichListClicked == CONFIG_BRIDGE) {
            if (getSsid().contains("azBridge")) {
                showWifiDevice();
            }
        } else if (whichListClicked == CHANGE_PASSWORD) {
            changePassword();
        }
    }

    private void showWifiDevice() {
        Intent intent = new Intent(BridgeActivity.this, ShowWifiDevicesActivity.class);
        startActivityForResult(intent, WIFI_CONFIG);
    }

    private void isRouterConnected() {
        Log.d(TAG, "isRouterConnected: ");
        byte[] packet = new byte[5];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.ROUTER_PARAM_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = 5;
        packet[RESPONSE_PACKET_LENGTH_POS] = Utils.ROUTER_PARAM_REQUEST;
        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:" + value);
                //checkConfigPacketStatus(resultCode, value);
                if (resultCode == Activity.RESULT_OK) {
                    if (value != null) {
                        Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                        //processConfigPacket(value);

                        if (value.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.ROUTER_PARAM_REQUEST &&
                                value.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                            Log.d(TAG, "processConfigPacket: " + Utils.parseInt(value, RESPONSE_COMMAND_STATUS_POS));
                            Log.d(TAG, "processConfigPacket: inside else ");
                            bridgeParam();
                        } else {
                            Log.e(TAG, "Invalid Packet");
                            if (value.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.ROUTER_PARAM_REQUEST && value.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                                Snackbar.make(main_layout, "Please check your router configuration and try again.", Snackbar.LENGTH_LONG).show();
                            } else {
                                Snackbar.make(main_layout, CommunicationError.getMessage(Utils.parseInt(value, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }
                } else {
                    if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                        Snackbar.make(main_layout, "Unable to communicate with bridge, please try again", Snackbar.LENGTH_LONG).show();

                    } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {

                        Snackbar.make(main_layout, "Timeout occurred", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    public void goBack(View view) {
        super.onBackPressed();
    }

    private int count = 0;
    private long startMillis=0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();
        if (eventaction == MotionEvent.ACTION_UP) {

            //get system current milliseconds
            long time = System.currentTimeMillis();


            //if it is the first time, or if it has been more than 5 seconds since the first tap ( so it is like a new try), we reset everything
            if (startMillis == 0 || (time - startMillis > 5000)) {
                startMillis = time;
                count = 1;
            }
            //it is not the first, and it has been  less than 3 seconds since the first
            else { //  time-startMillis< 3000
                count++;
            }

            if (count == 5) {
                String ssid = getCurrentSSID();
                Log.d(TAG, "onTouchEvent: "+ssid);
                if (ssid != null && ssid.equals("\"" + "azBridge" + "\"")) {
                    getBridgeDetail();
                }else if (ssid != null){
                    getRfPower();
                }
                //do whatever you need
            }
            return true;
        }

        return false;
    }

    private void getRfPower() {
        final AlertDialog.Builder mBuilder=new AlertDialog.Builder(this);
        View view=getLayoutInflater().inflate(R.layout.sq1,Utils.nullParent,false);

        TextView textView = view.findViewById(R.id.textView8);
        Button next=view.findViewById(R.id.next);
        Button cancel= view.findViewById(R.id.cancel);
        final EditText sq1=view.findViewById(R.id.editText);

        textView.setText("Set RF Power");
        sq1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        sq1.setHint("Enter RF Power.");
        mBuilder.setView(view);
        pdialog=mBuilder.create();
        pdialog.show();
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String d=sq1.getText().toString();
                    if(!d.isEmpty()) {
                        int power=Integer.parseInt(d);
                        if (power >= 0 && power <= 82){
                            pdialog.dismiss();
                            configRfPower(power);
                        }else{
                            Snackbar.make(findViewById(R.id.content),"Allowed Range for RF Power is [0-82]",Snackbar.LENGTH_LONG);
                        }
                    }
                    else{
                        sq1.setError("This field can not be blank");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(BridgeActivity.this, "Please enter a valid Number.", Toast.LENGTH_LONG).show();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdialog.dismiss();
            }
        });
    }

    private void configRfPower(int power) {
        byte[] packet = new byte[4];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.RF_POWER;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = 4;
        packet[RESPONSE_PACKET_LENGTH_POS] = (byte) power;

        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                try {
                    if (resultCode == Activity.RESULT_OK) {
                        if (value != null) {

                            if (Utils.parseInt(value, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK && value.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS){
                                Snackbar.make(main_layout,"RF Power configured successfully",Snackbar.LENGTH_LONG).show();
                            }else {
                                Snackbar.make(main_layout,CommunicationError.getMessage(Utils.parseInt(value, RESPONSE_COMMAND_STATUS_POS)),Snackbar.LENGTH_LONG).show();
                            }
                        }else {
                            Snackbar.make(main_layout,"Invalid packet",Snackbar.LENGTH_LONG).show();
                        }
                    }else{
                        if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                            Snackbar.make(main_layout, "Unable to communicate with bridge, please try again", Snackbar.LENGTH_LONG).show();

                        } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {

                            Snackbar.make(main_layout, "Timeout occurred", Snackbar.LENGTH_LONG).show();
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    private void getBridgeDetail() {
        byte[] packet = new byte[5];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.BATTERY_COUNT_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = 5;
        packet[RESPONSE_PACKET_LENGTH_POS] = Utils.ROUTER_PARAM_REQUEST;
        netClientAsyncTask = new NetClientAsyncTask(false, BridgeActivity.this, Utils.host,
                80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:" + value);
                checkConfigPacketStatus(resultCode, value);
            }
        });
        netClientAsyncTask.showProgressDialog(true, "Updating Configuration...");
        netClientAsyncTask.execute();
    }

    private void showVersionDialog(String lockMacId, String version) {
        final  AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bridge Detail");
        String message = "Bridge MAC : "+lockMacId+"\nVersion: "+version;
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface d, int arg1) {
                d.cancel();
            }
        });
        final AlertDialog closedialog= builder.create();

        closedialog.show();
    }

    @Override
    protected void onDestroy() {
        disableWifi();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        disableWifi();
        super.onBackPressed();
    }
}
