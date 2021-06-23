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

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;


public class AddDevicelistActivity extends AppCompatActivity implements HttpAsyncTask.AsyncResponse{

    private final String URL_POST= AppContext.getIp_address()+AppContext.getAddLock_url();
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mEmptyList;
    private ImageView refreshImageView;
    private static final String TAG = "AddDevicelistActivity";
    private List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 15000; // 15 seconds
    private Handler mHandler;
    private boolean mScanning;
    //int i=0;
    private Context context1;
    private UserMode userMode;
    //private AlertDialog dialog;
    //public static String code;
    //public static BleMessagingService mBleService = null;

    private ProgressDialog pdialog;
    private MySharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }
        sharedPreferences = new MySharedPreferences(this);
        setContentView(R.layout.ble_device_list);
        getIntent().getStringExtra("text");
        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        context1=this;
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


        //SessionManager sessionManager = new SessionManager(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        userMode=new UserMode(this);
        pdialog = new ProgressDialog(context1);
        pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pdialog.setIndeterminate(true);
        pdialog.setCancelable(false);
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
            //invalidateOptionsMenu();
            if(mEmptyList!=null) {
                mEmptyList.setText(R.string.scanning);
            }
            //cancelButton.setText(R.string.cancel);
            progressBar.setVisibility(View.VISIBLE);
            refreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //invalidateOptionsMenu();
            mEmptyList.setText(R.string.no_device_found);
            //cancelButton.setText(R.string.scan);
            progressBar.setVisibility(View.INVISIBLE);
            refreshImageView.setVisibility(View.VISIBLE);
        }
    }
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    final byte[] string = {'A', 'Z', 'L', 'O', 'C', 'K'};
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
							Log.d(TAG, "mLeScanCallback DeviceListActivity");


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 9; i < 15; i++) {
                                        if(scanRecord[i] != string[i-9])
                                            return;
                                    }
                                    addDevice(device,rssi);
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
        Log.d(TAG, "addDevice: 1");
        if(!deviceFound){
            deviceFound=true;
            String address=device.getAddress().replace(":","");
            Cursor res=userMode.getData(address);
            if(res.getCount() > 0){
                res.moveToFirst();
                do{
                    if(res.getString(1).equals("owner")){
                        deviceFound=false;
                        break;
                    }
                }while (res.moveToNext());
                res.close();
            }

            if (!deviceFound) {
                Log.d(TAG, "addDevice: 11");
                deviceList.add(device);
                mEmptyList.setVisibility(View.GONE);

            }
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
        Intent i = new Intent(this, Resetactivity.class);
        this.startActivity(i);
        super.onBackPressed();
        super.onBackPressed();
    }

    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
            pdialog.show();
            pdialog.setMessage("Adding Lock......");
            //public static  int reset_code=0;
            //int position1;
            String address = deviceList.get(position).getAddress();
            addDeviceToLock(address,sharedPreferences.getEmail());

        }
    };

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        //int seconds = 59;
    }

    @Override
    public void processFinish(String output,int errorCode) {
        pdialog.dismiss();
        if(output!=null)
        if(output.equals("Y")){
            Toast.makeText(context1, "Lock add successfully", Toast.LENGTH_LONG).show();
        }
        else if(output.equals("N")){
            Toast.makeText(context1, "Lock is not added", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(context1, "Unable to contact server", Toast.LENGTH_LONG).show();
        }

    }


    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        Map<String, Integer> devRssiValues;
        LayoutInflater inflater;

        DeviceAdapter(Context context, List<BluetoothDevice> devices, Map<String, Integer> devRssiValues) {
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

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, parent,false);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = vg.findViewById(R.id.address);
            final TextView tvname = vg.findViewById(R.id.name);
            final TextView tvpaired = vg.findViewById(R.id.paired);
            final TextView tvrssi = vg.findViewById(R.id.rssi);
            final TextView tvrssi1 = vg.findViewById(R.id.rssi1);
            tvrssi.setVisibility(View.VISIBLE);
            String st = deviceList.get(position).getAddress().replace(":", "");
            Log.d(TAG, st);
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
            if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                Log.i(TAG, "device::"+device.getName());
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
    private void addDeviceToLock(String mac,String id){
        JSONObject object= new JSONObject();

        try {
            object.put("lock_mac",mac.replaceAll(":",""));
            object.put("user_id",id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*HttpAsyncTask.context=this;
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        Log.d(TAG,"hello "+object.toString());
        httpTask.execute(URL_POST,object.toString());*/
        VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                Toast.makeText(AddDevicelistActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        Toast.makeText(context1, "Lock add successfully", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(context1, "Lock is not added", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
