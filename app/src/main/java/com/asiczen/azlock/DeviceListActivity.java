/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asiczen.azlock;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.util.CountDownTimer;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DeviceListActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mEmptyList;
    private ImageView refreshImageView;
    private static final String TAG = "DeviceListActivity";
    private List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;

    private Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 15000; // 15 seconds
    private Handler mHandler;
    private boolean mScanning;
    public static boolean isScannCall;
    private UserMode userMode;
    private String address="";
    //private final Handler handler=new Handler();
    //private final boolean isScann=false;
    private AlertDialog dialog;
    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }
        //private ProgressDialog pDialog;
        Log.d(TAG, "DeviceListActivity onCreate");
        setContentView(R.layout.ble_device_list);
        ConstraintLayout mLayout = findViewById(R.id.linearLayout);
        LinearLayout mLayout1 = findViewById(R.id.relativeLayout);

        if(isScannCall) { //for recent 3 lock
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, null,true);
            builder.setCancelable(false); // if you want user to wait for some process to finish,
            builder.setView(bridgeConnectView);
            TextView dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
            dialog = builder.create();
            dialogTextView.setText(R.string.searching_lock);
            dialog.show();

            mLayout1.setVisibility(View.GONE);
            int color = Color.parseColor("#1F618D");
            mLayout.setBackgroundColor(color);

            final int WAIT_TIME = 15001, INTERVAL = 1000;
            countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    dialog.dismiss();
                    Log.d("isScann","called");
                    Toast.makeText(DeviceListActivity.this, "Device not found", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK, null);
                    finish();
                }
            };
            countDownTimer.start();

        }
        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        mBluetoothAdapter = Utils.bluetoothAdapter; //bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        populateList();
        mEmptyList = findViewById(R.id.empty);
        SessionManager sessionManager = new SessionManager(this);
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        userMode=new UserMode(this);
        address= getIntent().getStringExtra("mylist");
    }

    public void onClickRefreshImageView(View v)
    {
        if (!mScanning) {
            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            scanLeDevice(true);
        }
        else
        {
            onBackPressed();
        }
    }

    private void populateList()
    {
        /* Initialize device list container */
        //Log.d(TAG, "populateList:"+deviceList);
        deviceList = new ArrayList<>();
        devRssiValues = new HashMap<>();
        deviceAdapter = new DeviceAdapter(this, deviceList,devRssiValues);


        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        scanLeDevice(true);
    }
    
    private void scanLeDevice(final boolean enable)
    {
        refreshImageView= findViewById(R.id.refresh);
        final ProgressBar progressBar = findViewById(R.id.pbProgressBar);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
					mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mEmptyList.setText(R.string.no_device_found);

                    progressBar.setVisibility(View.INVISIBLE);
                    refreshImageView.setVisibility(View.VISIBLE);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            if(mEmptyList!=null) {
                mEmptyList.setText(R.string.scanning);
            }
            progressBar.setVisibility(View.VISIBLE);
            refreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mEmptyList.setText(R.string.no_device_found);
            progressBar.setVisibility(View.INVISIBLE);
            refreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              //boolean equal = true;
                              byte[] string = {'A', 'Z', 'L', 'O', 'C', 'K'};
                              for (int i = 9; i < 15; i++) {
                                  if(scanRecord[i] != string[i-9])
                                      return;
                              }
                              //if(equal) {
                                  if(isScannCall){
                                      Log.d(TAG,"ADDRESS="+address);
                                      try {
                                          if(address.equals(device.getAddress()) ){
                                              dialog.dismiss();
                                              mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                              isScannCall = false;
                                              Bundle b = new Bundle();
                                              b.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
                                              b.putString(BluetoothDevice.EXTRA_NAME, device.getName());
                                              countDownTimer.cancel();
                                              Intent result = new Intent();
                                              result.putExtras(b);
                                              setResult(Activity.RESULT_OK, result);
                                              finish();
                                          }
                                      }
                                      catch (Exception e){
                                        e.printStackTrace();
                                      }
                                  }
                                  else {
                                      addDevice(device, rssi);
                                  }

                          }
                      });
                }
            });
        }
    };
    
    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
        	deviceList.add(device);
            mEmptyList.setVisibility(View.GONE);

        }
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        super.onStart();
    }

    @Override
    public void onStop() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy()");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");
        //sessionManager.logout();
        super.onBackPressed();
    }

    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());
            b.putString(BluetoothDevice.EXTRA_NAME, deviceList.get(position).getName());
            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    };

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
    
    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        Map<String, Integer> devRssiValues;
        LayoutInflater inflater;

        private DeviceAdapter(Context context, List<BluetoothDevice> devices, Map<String, Integer> devRssiValues) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
            this.devRssiValues = devRssiValues;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;
            String st;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                final ViewGroup nullParent = null;
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null,false);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = vg.findViewById(R.id.address);
            final TextView tvname = vg.findViewById(R.id.name);
            final TextView tvpaired = vg.findViewById(R.id.paired);
            final TextView tvrssi =  vg.findViewById(R.id.rssi);
            final TextView tvrssi1 =  vg.findViewById(R.id.rssi1);
            tvrssi.setVisibility(View.VISIBLE);
            st= deviceList.get(position).getAddress().replace(":", "");
            //Log.d(TAG,st);
            Cursor res=userMode.getData(st);
            if(res.getCount() > 0){
                res.moveToFirst();
                Log.e(TAG,res.getString(1));
                do {
                    if (res.getString(1).equals("guest")) {
                        tvrssi.setText(R.string.guest);
                       break;

                    } else if (res.getString(1).equals("owner")) {
                        tvrssi.setText(R.string.owner);
                        break;
                    }
                }while(res.moveToNext());
                res.close();
            }
            else
            {
                tvrssi.setText(" ");
            }

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            tvrssi1.setText(String.valueOf(devRssiValues.get(device.getAddress())));
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                //Log.i(TAG, "device::"+device.getName());
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
            } else {
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
            }
            return vg;
        }
    }
}
