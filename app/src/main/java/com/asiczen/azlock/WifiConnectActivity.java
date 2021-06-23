package com.asiczen.azlock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.asiczen.azlock.app.model.AccessPoint;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.util.CountDownTimer;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.util.DatabaseUtility;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by prasant on 25-02-2015.
 */
public class WifiConnectActivity extends AppCompatActivity implements DatabaseUtility, Packet, OnSearchListener, OnBroadcastListener {
    private static final String TAG = WifiConnectActivity.class.getSimpleName();
    private Context mContext = null;
    private AppContext appContext;
    private static ProgressDialog progressDialog = null;
    //private CheckBox checkBox;

    private WifiManager mWifiManager, wifi;
    //ArrayList<AccessPoint> accessPoints;
    private ListView apListView;
    //CustomAdapter<AccessPoint> accessPointCustomAdapter;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private ArrayList<WifiNetwork> wifiNetworks;
    private ArrayAdapter<WifiNetwork> wifiNetworkArrayAdapter;
    private IntentFilter mIntentFilter;
    private final Utils utils = new Utils();
    private WifiNetwork network;
    private CountDownTimer countDownTimer;

    private ListView savedNetworksListView;
    private ArrayList<WifiNetwork> deleteNetworksList;
    private ArrayAdapter<WifiNetwork> savedNetworkArrayAdapter;
    private CheckBox confirmCheckbox;
    private SparseBooleanArray selectedItemPositions;
    private String ssid, passwd;
    private static final int REQUEST_DANGEROUS_PERMISSION = 11;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bridge_list);

        mContext = this;
        appContext=AppContext.getContext();
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Devices</font>"));
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isAboveVersion6 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, this, this);

        if(isAboveVersion6) {
            boolean locationPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            boolean storagePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            Log.d("MainActivity", "Permission:" + locationPermission + ", " + storagePermission);
            if (!locationPermission || !storagePermission) {
                boolean shouldShowLocationPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                boolean shouldShowStoragePermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                // Should we show an explanation?
                if (shouldShowLocationPermissionRationale || shouldShowStoragePermissionRationale) {
                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this).setTitle("Permission Denied")
                            .setCancelable(false)
                            .setMessage("Without these permissions the app is unable to use Wi-Fi and can not store any data on this device. Are you sure you want to deny these permissions?")
                            .setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("data", Utils.CANCELLED);
                                    setResult(RESULT_CANCELED, returnIntent);
                                    finish();
                                }
                            })
                            .setNegativeButton("RETRY", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(WifiConnectActivity.this,
                                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            REQUEST_DANGEROUS_PERMISSION);
                                }
                            }).create().show();

                } else {

                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_DANGEROUS_PERMISSION);


                    // REQUEST_DANGEROUS_PERMISSION is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                initialize();
            }
        }
        else {
            initialize();
        }
    }

    private void initialize()
    {
        final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        wifiNetworks = databaseHandler.getNetworks();
        databaseHandler.close();
        if(wifiNetworks == null || wifiNetworks.size() == 0)
        {
            addNetwork();
        }

        apListView =  findViewById(R.id.listView);
        wifiNetworkArrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, wifiNetworks);
        apListView.setAdapter(wifiNetworkArrayAdapter);

        apListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                network = wifiNetworks.get(position);
                if(!Utils.isWifiNetworkConnected(mContext)) {
                    isConnecting=true;
                    connect();
                    final int WAIT_TIME=10000, INTERVAL=2000;
                    countDownTimer=new CountDownTimer(WAIT_TIME, INTERVAL) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            if(progressDialog!=null && progressDialog.isShowing())
                            {
                                progressDialog.dismiss();
                            }
                            if(!RouterConfigActivity.isWifiConnected && isConnecting && (isNetworkAdded || isNetworkEnabled))
                            {
                                //databaseHandler.deleteNetwork(network);
                                //wifiNetworkArrayAdapter.notifyDataSetChanged();
                                //Utils.forget(WifiConnectActivity.this);
                                isConnecting=false;
                            }
                            Snackbar.make(findViewById(android.R.id.content), "Device Unavailable", Snackbar.LENGTH_LONG).show();
                        }
                    };
                    Log.d(TAG, "Returened from Connect()");
                }
                else {
                    //authenticate();
                    Log.d(TAG, "OnItemClick/Starting Router Config");
                    Intent intent = new Intent(WifiConnectActivity.this, RouterConfigActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void connect()
    {
        View connectView = getLayoutInflater().inflate(R.layout.authenticate_access_point, Utils.nullParent,false);
        TextView ssidTextView =  connectView.findViewById(R.id.ap_ssid_textView);
        final EditText password =  connectView.findViewById(R.id.ap_password_editText);
        final TextView errorText = connectView.findViewById(R.id.error_paswd_textView);
        final CheckBox connectAutoCheckBox =  connectView.findViewById(R.id.connect_checkBox);
        ssidTextView.setText(network.getSSID());

        final AlertDialog alertDialog1 = new AlertDialog.Builder(mContext).setView(connectView)
                .setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Runtime.getRuntime().gc();
                    }
                }).create();
        alertDialog1.show();
        alertDialog1.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwd = password.getText().toString();
                if(countDownTimer!=null)
                {
                    countDownTimer.start();
                }
                if (!passwd.isEmpty() && passwd.length() > 8) {
                    errorText.setVisibility(View.GONE);
                    if (!mWifiManager.isWifiEnabled()) {
                        mWifiManager.setWifiEnabled(true);
                    }
                    DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                    progressDialog = new ProgressDialog(mContext);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setIndeterminate(true);
                    progressDialog.setMessage("Connecting...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    //Utils.forget(WifiConnectActivity.this);
                    utils.requestStatus = Utils.TCP_DIRECTION_UNDEFINED;
                    wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiConfiguration wc = new WifiConfiguration();
                    //Log.d(TAG, "Selected:"+itemPosition+">>\n"+accessPoints);
                    ssid = network.getSSID();

                    if (connectAutoCheckBox.isChecked()) {
                        WifiNetwork network = new WifiNetwork(ssid);
                        network.setPassword(passwd);
                        network.setAutoConnect(1);
                        databaseHandler.update(network, ssid, WifiNetwork.UPDATE_PASSWORD);
                        databaseHandler.update(network, ssid, WifiNetwork.UPDATE_AUTO_CONNECT);
                    }

                    wc.SSID = "\"" + ssid + "\"";
                    wc.preSharedKey = "\"" + passwd + "\"";
                    //wc.preSharedKey  = "\""+"$asiczen$"+"\"";
                    wc.hiddenSSID = true;
                    wc.status = WifiConfiguration.Status.ENABLED;
                    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

                    int res = wifi.addNetwork(wc);
                    isNetworkAdded = (res != -1);
                    Log.d("WifiPreference", "add Network returned " + res);
                    isNetworkEnabled = wifi.enableNetwork(res, true);
                    Log.d("WifiPreference", "enableNetwork returned " + isNetworkEnabled);
                    databaseHandler.close();
                    alertDialog1.dismiss();
                } else {
                    errorText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isConnecting = false;
    private boolean isNetworkAdded = false;
    private boolean isNetworkEnabled = false;

    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG, "onStart called");
        //start the main task here, to be debugged if it runs infinitely
        //new ConnectAsyncTask(this, Utils.LockDemoUtils).execute();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG, "onStop called/isConnected:" + appContext.isConnected());

    }

    /* register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        Log.d(TAG, "onResume called");
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(wifiBroadcastReceiver);
        Log.d(TAG, "onPause called");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.gc();
        Runtime.getRuntime().gc();
        Log.d(TAG, "onDestroy called");
    }

    @SuppressLint("HardwareIds")
    private String getMacId()
    {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo mInfo = null;
        if (manager != null) {
            mInfo = manager.getConnectionInfo();
        }
        return (mInfo == null ? null : mInfo.getMacAddress());
    }


    private final int securityType = WifiNetwork.WPA;

    private void addNetwork()
    {
        View addNetworkView = getLayoutInflater().inflate(R.layout.add_network, Utils.nullParent,false);
        final EditText ssidEditText =  addNetworkView.findViewById(R.id.ssid_editText);
        final String negativeButton = (wifiNetworks == null || wifiNetworks.size() == 0) ? "BACK" : "CANCEL";
        new AlertDialog.Builder(mContext).setTitle("Add Network")
                .setView(addNetworkView)
                .setCancelable(false)
                .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String ssid = ssidEditText.getText().toString();
                        if (!ssid.isEmpty()) {
                            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                            WifiNetwork wifiNetwork = new WifiNetwork(ssid, securityType);
                            wifiNetwork.setAutoConnect(0);
                            if(databaseHandler.addNetwork(wifiNetwork)) {
                                wifiNetworks = databaseHandler.getNetworks();
                                wifiNetworkArrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, wifiNetworks);
                                apListView.setAdapter(wifiNetworkArrayAdapter);
                                wifiNetworkArrayAdapter.notifyDataSetChanged();
                            }
                            databaseHandler.close();
                        }
                    }
                })
                .setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(negativeButton.equalsIgnoreCase("CANCEL")) {
                            dialog.dismiss();
                        }
                        else {
                            /*finish();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);*/
                            /*Intent returnIntent = new Intent();
                            returnIntent.putExtra(MainActivity.ARGUMENT_FROM_MAIN, Utils.CANCELLED);
                            setResult(RESULT_CANCELED, returnIntent);
                            finish();*/
                            dialog.dismiss();
                            finish();
                        }
                    }
                }).create().show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_DANGEROUS_PERMISSION) {
            boolean isGranted = true;
            for (int grantResult : grantResults) {
                isGranted = isGranted && grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (!isGranted) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("data", Utils.CANCELLED);
                setResult(RESULT_CANCELED, returnIntent);
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            } else {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                Log.d(TAG, "onRequestPermissionsResult: granted");
            }
            finish();

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wifi_connect_activity_menu, menu);
        super.onCreateOptionsMenu(menu);

        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.add_network:
                addNetwork();
                return true;
            case R.id.delete_network:
                deleteNetwork();
                return true;
            case R.id.show_mac:
                new AlertDialog.Builder(mContext).setTitle("Wi-Fi MAC Address")
                        .setMessage(getMacId())
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();

                return true;

            case R.id.Guide:
                Intent guideIntent = new Intent(this, GuideActivity.class);
                Log.d("Main", "Starting GuideActivity");
                startActivity(guideIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteNetwork()
    {
        View savedNetworksView = getLayoutInflater().inflate(R.layout.saved_networks, Utils.nullParent,false);
        confirmCheckbox =  savedNetworksView.findViewById(R.id.delete_network_checkBox);
        savedNetworksListView  =  savedNetworksView.findViewById(R.id.listView);
        TextView matchNotFound =  savedNetworksView.findViewById(R.id.no_match_textView);
        savedNetworksListView.setEmptyView(matchNotFound);
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        ArrayList<WifiNetwork> savedNetworks = databaseHandler.getNetworks();
        savedNetworkArrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_multiple_choice, savedNetworks);
        savedNetworksListView.setAdapter(savedNetworkArrayAdapter);
        savedNetworksListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        databaseHandler.close();

        final AlertDialog alertDialog = new AlertDialog.Builder(mContext).setView(savedNetworksView)
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedItemPositions = savedNetworksListView.getCheckedItemPositions();
                        int size = savedNetworkArrayAdapter.getCount();
                        deleteNetworksList = new ArrayList<>();
                        for(int i=0;i<size;i++){
                            if(selectedItemPositions.get(i)){
                                if(savedNetworksListView.isItemChecked(i)){
                                    WifiNetwork wifiNetwork = ((WifiNetwork) savedNetworksListView.getItemAtPosition(i));
                                    if (!deleteNetworksList.contains(wifiNetwork)) {
                                        deleteNetworksList.add(wifiNetwork);
                                        wifiNetworkArrayAdapter.remove(wifiNetwork);
                                        wifiNetworkArrayAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                        DatabaseHandler databaseHandler1 = new DatabaseHandler(mContext);
                        databaseHandler1.deleteNetworks(deleteNetworksList);
                        databaseHandler1.close();
                        dialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content),"Networks Deleted", Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        confirmCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(confirmCheckbox.isChecked());
            }
        });
    }


    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed called");
        /*if(!MainActivity.isConnected) {
            // close app, invoke exitfragment
            //ConnectActivity.exitMainActivity = true;
            Log.d(TAG, "onBackPressed called/isConnected:"+MainActivity.isConnected);
            MainActivity.vibrator.vibrate(20);
            View exitView = getLayoutInflater().inflate(R.layout.exit, null);
            checkBox = (CheckBox) exitView.findViewById(R.id.checkBox2);
            if(MainActivity.isWifiEnabledOnStart) {
                checkBox.setChecked(false);
            }
            new AlertDialog.Builder(mContext).setMessage("Do you want to Exit?")
                    .setView(exitView)
                    .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (checkBox.isChecked()) {
                                MainActivity.wifiManager.setWifiEnabled(false);
                            }
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra(MainActivity.ARGUMENT_FROM_MAIN, Utils.CANCELLED);
                            setResult(RESULT_CANCELED, returnIntent);
                            finish();
                        }
                    })
                    .setNegativeButton("REFRESH", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.wifiManager.setWifiEnabled(false);
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra(MainActivity.ARGUMENT_FROM_MAIN, Utils.REFRESHED);
                            setResult(RESULT_CANCELED, returnIntent);
                            finish();
                        }
                    }).create().show();

        }
        else{
            Log.d(TAG, "calling super.onBackPressed");
            super.onBackPressed();
        }*/
    }

    @Override
    public void onSearch(List results) {

    }


    @Override
    public void onReceive(int resultCode, Object result) {
        if(resultCode == OnBroadcastListener.SCAN_RESULTS_UPDATED) {
            /*scanResultList.clear();
            accessPoints.clear();
            scanResultList = mWifiManager.getScanResults();
            for(ScanResult scanResult : scanResultList){
                //Log.d(TAG, scanResult.toString());
                accessPoints.add(new AccessPoint(scanResult.BSSID, scanResult.SSID, scanResult.capabilities, scanResult.level));
            }
            accessPointCustomAdapter.onRefresh(accessPoints);*/
            //Log.d("Main", "onUpdate/UPDATED_RESULTS_SIZE:" + mWifiManager.getScanResults().size());
            List<ScanResult> scanResults = (List<ScanResult>) result;
            //miActionProgressItem.setVisible(true);
            Log.d(TAG, "onUpdate/UPDATED_RESULTS_SIZE:\n" + scanResults);
        }
        else if(resultCode == OnBroadcastListener.NEW_RSSI){
            int rssi = (Integer) result;
            Log.d("Main", "onUpdate/NEW_RSSI:"+rssi);
        }
        else if(resultCode == OnBroadcastListener.CONNECTED_WIFI_INFO){
            String connectedBSSID = (String) result;
            //Log.d("Main", "onUpdate/SUPPLICANT_CONNECTED:"+MainActivity.mWifiInfo.getBSSID());
            Log.d(TAG, "onUpdate/SUPPLICANT_CONNECTED:"+ connectedBSSID);
        }
        else if(resultCode == OnBroadcastListener.ERROR_AUTHENTICATING){
            Log.d(TAG, "onUpdate/Authentication Error");
            if(progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Snackbar.make(findViewById(android.R.id.content), "Authentication Failed", Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            connect();
                        }
                    }).show();
        }
        else if(resultCode == OnBroadcastListener.CONNECTIVITY_CHANGED) {
            boolean isConnected = (Boolean) result;
            Log.d(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED:"+isConnected);
            RouterConfigActivity.isWifiConnected = isConnected;
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            AccessPoint temp = new AccessPoint(mWifiInfo.getBSSID(), mWifiInfo.getSSID());
            Log.d(TAG, "Request Status:"+utils.requestStatus);

            if(isConnected)
            {
                if(progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if(countDownTimer!=null)
                {
                    countDownTimer.cancel();
                }
                Log.d(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED/Starting Router Config");
                isConnecting = false;
                Intent intent = new Intent(WifiConnectActivity.this, RouterConfigActivity.class);
                startActivity(intent);
                finish();
            }
            //Log.d("Main", "onUpdate/CONNECTIVITY_CHANGED:"+isConnected);
        }
        else if(resultCode == OnBroadcastListener.SUPPLICANT_NEW_STATE){
            /*SupplicantState supplicantState = (SupplicantState) result;
            String state = supplicantState.toString();
            AccessPoint temp = new AccessPoint(wifiConnectionInfo[1], wifiConnectionInfo[0]);
            for(int i=0;i<accessPoints.size();i++){
                if(accessPoints.get(i).equals(temp)){
                    if(state.equalsIgnoreCase("ASSOCIATED")) {
                        accessPoints.get(i).setStatus(AccessPoint.ASSOCIATED);
                        if(progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.setMessage("Associated...");
                        }
                    }
                    else if(state.equalsIgnoreCase("ASSOCIATING"))
                    {
                        accessPoints.get(i).setStatus(AccessPoint.ASSOCIATING);
                        if(progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.setMessage("Associating...");
                        }
                    }
                    else if(state.equalsIgnoreCase("AUTHENTICATING"))
                    {
                        accessPoints.get(i).setStatus(AccessPoint.AUTHENTICATING);
                        if(progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.setMessage("Authenticating...");
                        }
                    }
                    else if(state.equalsIgnoreCase("COMPLETED"))
                    {
                        accessPoints.get(i).setStatus(AccessPoint.COMPLETED);
                        if(progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.setMessage("Completed...");
                        }
                    }
                    accessPointCustomAdapter.onRefresh(accessPoints);
                    break;
                }
            }*/
            //Log.d("Main", "onUpdate/SUPPLICANT_NEW_STATE:"+supplicantState.toString());
        }
    }


}
