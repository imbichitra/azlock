package com.asiczen.azlock;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.util.Packet;


/**
 * Created by user on 9/28/2015.
 */
public class RouterConfigActivity extends AppCompatActivity implements Packet, OnBroadcastListener {
    private static final String TAG = RouterConfigActivity.class.getSimpleName();
    private IntentFilter mIntentFilter;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private NetClientAsyncTask netClientAsyncTask;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_router);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Remote Setup</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mIntentFilter = new IntentFilter();
        //mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        //mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, this, this);
    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(wifiBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Runtime.getRuntime().gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.saveConfig:
                if(isWifiConnected) {
                    //doConfigRouter(securityPosition);
                    if(netClientAsyncTask.getStatus() == AsyncTask.Status.PENDING){
                        // My AsyncTask has not started yet
                        Log.e(TAG,"netClientAsyncTask [PENDING]");
                    }

                    if(netClientAsyncTask.getStatus() == AsyncTask.Status.RUNNING){
                        // My AsyncTask is currently doing work in doInBackground()
                        Log.e(TAG,"netClientAsyncTask [RUNNING]");
                    }

                    if(netClientAsyncTask.getStatus() == AsyncTask.Status.FINISHED){
                        // My AsyncTask is done and onPostExecute was called
                        Log.e(TAG,"netClientAsyncTask [FINISHED]");
                    }
                }
                else
                {
                    Snackbar.make(findViewById(android.R.id.content), "Not Connected", Snackbar.LENGTH_SHORT)
                            .setAction(null, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                }
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.router_config_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public static boolean isWifiConnected=false;

    @Override
    public void onReceive(int resultCode, Object result) {
        switch (resultCode)
        {
            case OnBroadcastListener.CONNECTIVITY_CHANGED:
                isWifiConnected = (Boolean) result;
                //mWifiInfo = mWifiManager.getConnectionInfo();
                Log.d(TAG, "isWifiConnected:"+isWifiConnected);
                if(!isWifiConnected)
                {
                    Snackbar.make(findViewById(android.R.id.content), "Connection Lost", Snackbar.LENGTH_LONG)
                            .setAction(null, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                }
                break;

            case OnBroadcastListener.CONNECTED_WIFI_INFO:
                //private WifiInfo mWifiInfo;
                String connectedBSSID = (String) result;
                Log.d(TAG, "connectedBSSID:"+ connectedBSSID);
                break;

            default:
                break;
        }
    }

}
