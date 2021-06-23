package com.asiczen.azlock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShowWifiDevicesActivity extends AppCompatActivity implements OnBroadcastListener{

    private static final String TAG = ShowWifiDevicesActivity.class.getSimpleName();
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private IntentFilter mIntentFilter;
    private WifiManager mWifiManager;
    private final List<String> listData = new ArrayList<>();
    private ArrayAdapter adapter;
    private ProgressBar progress_bar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_wifi_devices);
        ListView listView = findViewById(R.id.list);
        progress_bar = findViewById(R.id.progress_bar);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null && !mWifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, this, this);
        mWifiManager.startScan();
        adapter = new ArrayAdapter<>(getApplicationContext(),R.layout.wifi_list_item, listData);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // selected item
                String ssid = ((TextView) view).getText().toString();
                //Toast.makeText(ShowWifiDevicesActivity.this,"Wifi SSID : "+ssid,Toast.LENGTH_SHORT).show();
                showDialog(ssid,false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            progress_bar.setVisibility(View.VISIBLE);
            listData.clear();
            adapter.clear();
            mWifiManager.startScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onReceive(int resultCode, Object result) {
        if (resultCode == OnBroadcastListener.SCAN_RESULTS_UPDATED) {
            progress_bar.setVisibility(View.GONE);
            List<ScanResult> resultData = mWifiManager.getScanResults();
            for (ScanResult sc : resultData) {
                Log.d(TAG, "onReceive: " + sc.SSID);
                if (!sc.SSID.isEmpty()) {
                    if (!sc.SSID.equals("azBridge")) {
                        listData.add(sc.SSID);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(wifiBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        super.onResume();
    }

    private void showDialog(String ssid,boolean enable){
        View bridgeConfigView = getLayoutInflater().inflate(R.layout.config_router, Utils.nullParent,false);
        final TextInputEditText ssidEditText = bridgeConfigView.findViewById(R.id.ssid_editText);
        final TextInputEditText passwordEditText = bridgeConfigView.findViewById(R.id.password_editText);
        TextInputLayout textInputLayout = bridgeConfigView.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Router SSID");
        final TextView title = bridgeConfigView.findViewById(R.id.title);
        final Button cancel = bridgeConfigView.findViewById(R.id.cancel);
        final Button next = bridgeConfigView.findViewById(R.id.next);
        ssidEditText.setEnabled(enable);
        title.setText(R.string.router_detail);
        ssidEditText.setText(ssid);
        final AlertDialog configRouterAlertDialog = new AlertDialog.Builder(this)
                .setView(bridgeConfigView)
                .create();
        configRouterAlertDialog.show();
        configRouterAlertDialog.setCancelable(false);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configRouterAlertDialog.dismiss();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssid = Objects.requireNonNull(ssidEditText.getText()).toString();
                String password = Objects.requireNonNull(passwordEditText.getText()).toString();
                boolean isContain = TextUtils.isEmpty(ssid) ;
                if (!isContain) {
                    if (ssid.length()>=2 && ssid.length() <= 31)
                        if (TextUtils.isEmpty(password)){
                            configRouterAlertDialog.dismiss();
                            goBackTCallerActivity(ssid, "");
                        }else {
                            if (password.length() >= 8 && password.length() <= 31) {
                                configRouterAlertDialog.dismiss();
                                goBackTCallerActivity(ssid, password);
                            } else
                                Toast.makeText(ShowWifiDevicesActivity.this, "Password should be in between 8 to 31", Toast.LENGTH_SHORT).show();
                        }
                    else
                        Toast.makeText(ShowWifiDevicesActivity.this, "Router SSID should be in between 2 to 31", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ShowWifiDevicesActivity.this, "Fill the data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goBackTCallerActivity(String ssid,String password){
        Intent i = new Intent();
        i.putExtra("SSID", ssid);
        i.putExtra("PASSWORD",password);
        setResult(Activity.RESULT_OK,i);
        finish();
    }

    public void hiddenNetwork(View view) {
        showDialog("",true);
    }
}
