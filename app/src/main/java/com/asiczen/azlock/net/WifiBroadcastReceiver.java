package com.asiczen.azlock.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.OnBroadcastListener;
import com.asiczen.azlock.content.DatabaseHandler;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by user on 1/25/2016.
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {

    private final WifiManager wifiManager;
    private final OnBroadcastListener mOnBroadcastListener;
    private ArrayList<WifiNetwork> wifiNetworks;

    public WifiBroadcastReceiver(WifiManager wifiManager, Context context, OnBroadcastListener mOnBroadcastListener){
        this.wifiManager = wifiManager;
        //ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.mOnBroadcastListener = mOnBroadcastListener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //Log.d("BroadcastReceiver >", action);

        if (action != null && action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d("BroadcastReceiver", WifiManager.NETWORK_STATE_CHANGED_ACTION);
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if (networkInfo != null) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    String BSSID = intent.getStringExtra(WifiManager.EXTRA_BSSID);
                    //WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    DatabaseHandler databaseHandler = new DatabaseHandler(context);
                    wifiNetworks = databaseHandler.getNetworks();
                    for (WifiNetwork wifiNetwork : wifiNetworks) {
                        //Log.d("BroadcastReceiver", "Matching:"+wifiInfo.getSSID()+" <> "+wifiNetwork.getSSID());
                        if (wifiInfo.getSSID().equals("\"" + wifiNetwork.getSSID() + "\"")) {
                            Log.d("BroadcastReceiver", "Initiating Callback:" + BSSID);
                            mOnBroadcastListener.onReceive(OnBroadcastListener.CONNECTED_WIFI_INFO, BSSID);
                            mOnBroadcastListener.onReceive(OnBroadcastListener.CONNECTIVITY_CHANGED, true);
                            break;
                        }
                    }

                    //Log.d("BroadcastReceiver", "NetworkInfo.State.CONNECTED:"+BSSID+"<>"+wifiInfo);
                } else if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                    mOnBroadcastListener.onReceive(OnBroadcastListener.CONNECTIVITY_CHANGED, false);
                }
            }
        }
        if (action != null && action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            Log.d("BroadcastReceiver", WifiManager.WIFI_STATE_CHANGED_ACTION);
        }
        if (action != null && action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            //WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -11111);
            Log.d("BroadcastReceiver", WifiManager.RSSI_CHANGED_ACTION + ":" + rssi);
        }
        if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
            //Log.d("BroadcastReceiver", WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            //Log.d("BroadcastReceiver->", "RESULTS_UPDATED:"+intent.getBooleanExtra(wifiManager.EXTRA_RESULTS_UPDATED, false));
            Log.d("BroadcastReceiver->", "RESULTS_SIZE:"+wifiManager.getScanResults().size());
            mOnBroadcastListener.onReceive(OnBroadcastListener.SCAN_RESULTS_UPDATED, wifiManager.getScanResults());
        }
        if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            //Log.d("BroadcastReceiver", ConnectivityManager.CONNECTIVITY_ACTION);
            if (isNetworkAvailable(context)) {
                Log.d("BroadcastReceiver", "Connected");
            }
            else {
                Log.d("BroadcastReceiver", "Not Connected");
            }
        }
        if(action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
            //Log.d("BroadcastReceiver", WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            Log.d("BroadcastReceiver->", Objects.requireNonNull(intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)).toString());
            //Log.d("BroadcastReceiver->", "Connection State:"+intent.getBooleanExtra(wifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

            //Log.d("BroadcastReceiver->", "Supplicant Error:" + supplicantError);
            //SupplicantState supplicantNewState = wifiInfo.getSupplicantState();
            SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if(supplicantState == SupplicantState.ASSOCIATED)
            {
                //Log.d("BroadcastReceiver", "ASSOCIATED");
                mOnBroadcastListener.onReceive(OnBroadcastListener.SUPPLICANT_NEW_STATE, supplicantState);
            }
            if(supplicantState == SupplicantState.ASSOCIATING)
            {
                //Log.d("BroadcastReceiver", "ASSOCIATING");
                mOnBroadcastListener.onReceive(OnBroadcastListener.SUPPLICANT_NEW_STATE, supplicantState);
            }
            if(supplicantState == SupplicantState.AUTHENTICATING)
            {
                //Log.d("BroadcastReceiver", "AUTHENTICATING");
                mOnBroadcastListener.onReceive(OnBroadcastListener.SUPPLICANT_NEW_STATE, supplicantState);
            }
            if(supplicantState == SupplicantState.COMPLETED)
            {
                //Log.d("BroadcastReceiver", "AUTHENTICATION COMPLETED");
                mOnBroadcastListener.onReceive(OnBroadcastListener.SUPPLICANT_NEW_STATE, supplicantState);
            }
            if(supplicantState == SupplicantState.SCANNING)
            {
                Log.d("BroadcastReceiver", "SCANNING");
            }
            if(supplicantState == SupplicantState.DISCONNECTED)
            {
                Log.d("BroadcastReceiver", "DISCONNECTED");
            }
            if(supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE)
            {
                Log.d("BroadcastReceiver", "FOUR_WAY_HANDSHAKE");
            }
            if(supplicantState == SupplicantState.UNINITIALIZED)
            {
                Log.d("BroadcastReceiver", "UNINITIALIZED");
            }
            if(supplicantState == SupplicantState.GROUP_HANDSHAKE)
            {
                Log.d("BroadcastReceiver", "GROUP_HANDSHAKE");
            }

            int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -111);
            if(supplicantError == WifiManager.ERROR_AUTHENTICATING){
                mOnBroadcastListener.onReceive(OnBroadcastListener.ERROR_AUTHENTICATING, WifiManager.ERROR_AUTHENTICATING);
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (connectivity != null) {
            netInfo = connectivity.getActiveNetworkInfo();
        }
        if(netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED){
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            DatabaseHandler databaseHandler = new DatabaseHandler(context);
            wifiNetworks = databaseHandler.getNetworks();
            for(WifiNetwork wifiNetwork : wifiNetworks) {
                if(wifiInfo.getSSID().equals("\""+wifiNetwork.getSSID()+"\"")) {
                    return true;
                }
            }
        }
        return false;
    }

    /*@RequiresApi(api = Build.VERSION_CODES.M)
    private Boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }*/
}
