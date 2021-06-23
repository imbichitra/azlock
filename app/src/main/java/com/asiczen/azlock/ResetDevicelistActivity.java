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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.BleMessagingService;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;


public class ResetDevicelistActivity extends AppCompatActivity implements HttpAsyncTask.AsyncResponse {
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mEmptyList;
    private ImageView refreshImageView;
    private static final String TAG = "ResetDevicelistActivity";
    private List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 15000; // 15 seconds
    private Handler mHandler;
    private boolean mScanning;
    int i=0;
    private Context context1;
    private static UserMode userMode;
    private AlertDialog dialog;
    public static String code;
    private int position1;
    private static String address="";
    public static BleMessagingService mBleService = null;
    private static ProgressDialog pDialog;
    private static String doorID;
    public static int count = 0;
    private static boolean  isresetCodeCorrect=true;
    private DatePickerDialog.OnDateSetListener listener;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }
        Log.d(TAG, "DeviceListActivity onCreate");
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.ble_device_list);
        getIntent().getStringExtra("text");
        //android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        //layoutParams.gravity=Gravity.CENTER;
        //layoutParams.y = 200;
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
        // BluetoothAdapter through BluetoothManager.
        //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = Utils.bluetoothAdapter; //bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        populateList();
        mEmptyList =  findViewById(R.id.empty);


        //SessionManager sessionManager = new SessionManager(this);
        //logoutBroadcastReceiver = new LogoutBroadcastReceiver(ResetDevicelistActivity.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        userMode=new UserMode(this);
        pDialog = new ProgressDialog(context1);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setIndeterminate(true);
        pDialog.setCancelable(false);
        HttpAsyncTask.context=this;
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
        Log.d(TAG, "populateList:"+deviceList);
        deviceList = new ArrayList<>();
        devRssiValues = new HashMap<>();
        deviceAdapter = new DeviceAdapter(this, deviceList,devRssiValues);
        //arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList);


        ListView newDevicesListView =  findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        scanLeDevice(true);
    }
    private void scanLeDevice(final boolean enable)
    {
        refreshImageView= findViewById(R.id.refresh);
        final ProgressBar progressBar =  findViewById(R.id.pbProgressBar);
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
                                    boolean equal= true;
                                    for (int i = 9; equal && i < 15; i++) {
                                        equal = scanRecord[i] == string[i-9];
                                    }
                                    if(equal)
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
        if(mBluetoothAdapter !=null)
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        /*Intent i = new Intent(this, Resetactivity.class);
        this.startActivity(i);*/
        Log.d(TAG, "onBackPressed: ");
        super.onBackPressed();
    }

    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
            address = deviceList.get(position).getAddress();
            doorID = deviceList.get(position).getAddress();
            Resetactivity.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

            if(!loginData.getUserId().equals(AppContext.getAdminlogin())) {
                Log.d(TAG, "pos :" + position);
                Log.d(TAG, "pos1 :" + position1);
                position1 = position;
                loginData.setMac_id(address.replaceAll(":", ""));
                dialog1();
            }
            else{
                if (loginData.isIsBatteryStatusCount()){
                    Intent intent = new Intent();
                    intent.putExtra(BluetoothDevice.EXTRA_DEVICE,address);
                    setResult(Activity.RESULT_OK,intent);
                    finish();
                }else {
                    dialog123("");
                }
            }
        }
    };

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    public void processFinish(String output,int errorCode) {
        Log.d(TAG,"data "+output);
        if(output!=null)
        switch (output) {
            case "DOB":
                pDialog.dismiss();
                dialog2();
                break;
            case "SQ1":
                pDialog.dismiss();
                dialog3();
                break;
            case "RESETCODE":
                pDialog.dismiss();
                dialog123( "Reset code is sent to your email");
                break;
            case "SQ2":
                pDialog.dismiss();
                dialog4();
                break;
            case "S_ANS":
                /*String URL = "send_mail.php";
                String URL_POST=BuildConfig.Port+URL;*/
                String URL_POST= AppContext.getIp_address()+AppContext.getSend_mail_url();
                JSONObject obj= new JSONObject();
                try {
                    Log.d(TAG,"mac_id"+loginData.getMac_id());
                    obj.put("user_id",loginData.getUserId());
                    obj.put("mac_id",loginData.getMac_id());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                verify(URL_POST,obj);
                break;
            default:
                switch (output) {
                    case "DOB_N":
                        pDialog.dismiss();
                        Toast.makeText(ResetDevicelistActivity.this, "Date of birth is incorrect", Toast.LENGTH_LONG).show();
                        break;
                    case "SQ1_N":
                        pDialog.dismiss();
                        Toast.makeText(ResetDevicelistActivity.this, "Secret Question 1 is incorrect", Toast.LENGTH_LONG).show();
                        break;
                    case "SQ2_N":
                        pDialog.dismiss();
                        Toast.makeText(ResetDevicelistActivity.this, "Secret Question 2 is incorrect", Toast.LENGTH_LONG).show();
                        break;
                    case "FAIL":
                        pDialog.dismiss();

                       final  AlertDialog.Builder builder = new AlertDialog.Builder(this);
                       builder.setTitle("Try again");
                        builder.setMessage("You are not owner of this Lock");
                        builder.setCancelable(true);
                        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface d, int arg1) {
                                d.cancel();
                            }
                        });
                        final AlertDialog closedialog= builder.create();

                        closedialog.show();

                        final Timer timer2 = new Timer();
                        timer2.schedule(new TimerTask() {
                            public void run() {
                                closedialog.dismiss();
                                timer2.cancel(); //this will cancel the timer of the system
                            }
                        }, 5000);
                        //Toast.makeText(ResetDevicelistActivity.this, "You are not owner of this Lock", Toast.LENGTH_LONG).show();
                        break;
                    case "MAIL_NOT_SEND":
                        pDialog.dismiss();
                        Toast.makeText(context1, "Unable to send mail", Toast.LENGTH_LONG).show();
                        break;
                    case "S_ANS_NOT":
                        pDialog.dismiss();
                        Toast.makeText(context1, "Secret answer is incorrect", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        pDialog.dismiss();
                        Toast.makeText(ResetDevicelistActivity.this, "Unable to contact server", Toast.LENGTH_LONG).show();
                        break;
                }
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
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, Utils.nullParent,false);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = vg.findViewById(R.id.address);
            final TextView tvname = vg.findViewById(R.id.name);
            final TextView tvpaired =  vg.findViewById(R.id.paired);
            final TextView tvrssi =  vg.findViewById(R.id.rssi);
            final TextView tvrssi1 =  vg.findViewById(R.id.rssi1);
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


    private void dialog1(){
        Log.d(TAG,"POSSS");
        final AlertDialog.Builder mBuilder=new AlertDialog.Builder(this);
        View view=getLayoutInflater().inflate(R.layout.enterdob,Utils.nullParent,false);
        Button next=view.findViewById(R.id.next);
        Button cancel= view.findViewById(R.id.cancel);
        final TextView dob=view.findViewById(R.id.dob2_editText);
        final TextView openReset = view.findViewById(R.id.openReset);
        mBuilder.setView(view);
        dialog=mBuilder.create();
        dialog.show();

        /*dob.addTextChangedListener(new TextWatcher() {
            int prevL = 0;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                prevL = dob.getText().toString().length();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }


            @Override
            public void afterTextChanged(Editable editable) {
                int length = editable.length();
                if ((prevL < length) && (length == 4 || length == 7)) {
                    editable.append("-");
                }
            }
        });*/
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                final int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(
                        ResetDevicelistActivity.this,
                        android.R.style.Theme_DeviceDefault_Dialog,
                        listener,
                        year, month, day
                );
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month + 1;
                String date = year + "-" + month + "-" + dayOfMonth;
                dob.setText(date);
            }
        };

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String d=dob.getText().toString();
                if(!d.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    /*String URL="dob_verification.php";
                    String URL_POST=BuildConfig.Port+URL;*/
                    String URL_POST=AppContext.getIp_address()+AppContext.getDob_verification_url();
                    pDialog.setMessage("Connecting...");
                    pDialog.show();
                    try {
                        obj.put("dob", d);
                        obj.put("user_id", loginData.getUserId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    verify(URL_POST, obj);
                    dialog.dismiss();
                }
                else{
                    dob.setError("This field can not be blank");
                }
            }

        });
       cancel.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               dialog.dismiss();
           }
       });
       openReset.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               dialog.dismiss();
               dialog123("");
           }
       });
    }
       private void verify(String url,JSONObject object){
           /*HttpAsyncTask httpTask = new HttpAsyncTask();
           httpTask.delegate = this;
           httpTask.execute(url,data);*/
           VolleyRequest.jsonObjectRequest(this, url, object, Request.Method.POST, new VolleyResponse() {
               @Override
               public void VolleyError(VolleyError error) {
                   if (dialog!=null && dialog.isShowing())
                       dialog.dismiss();
                   Toast.makeText(ResetDevicelistActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
               }

               @Override
               public void VolleyObjectResponse(JSONObject response) {
                   Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                   if (dialog!=null && dialog.isShowing())
                       dialog.dismiss();
                   try {
                       processFinish(response.getString(STATUS),0);
                   } catch (JSONException e) {
                       e.printStackTrace();
                   }
               }
           });
       }
    private void dialog2(){
        Log.d(TAG,"POSSS");
        final AlertDialog.Builder mBuilder=new AlertDialog.Builder(this);
        View view=getLayoutInflater().inflate(R.layout.sq1,Utils.nullParent,false);
        Button next=view.findViewById(R.id.next);
        Button cancel= view.findViewById(R.id.cancel);
        final EditText sq1=view.findViewById(R.id.editText);
        mBuilder.setView(view);
        dialog=mBuilder.create();
        dialog.show();
       next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String d=sq1.getText().toString();
                if(!d.isEmpty()) {
                    Log.d(TAG, "m" + d);
                    JSONObject obj = new JSONObject();

                    /*String URL="secret_Q1_verification.php";
                    String URL_POST=BuildConfig.Port+URL;*/
                    String URL_POST=AppContext.getIp_address()+AppContext.getSecret_Q1_verification_url();
                    pDialog.setMessage("Connecting...");
                    pDialog.show();
                    try {
                        obj.put("secret_Q1", d);
                        obj.put("user_id", loginData.getUserId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    verify(URL_POST, obj);
                    dialog.dismiss();
                }
                else{
                    sq1.setError("This field can not be blank");
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
    private void dialog3(){
        Log.d(TAG,"POSSS");
        final AlertDialog.Builder mBuilder=new AlertDialog.Builder(this);
        View view=getLayoutInflater().inflate(R.layout.sq2,Utils.nullParent,false);
        Button next=view.findViewById(R.id.next);
        Button cancel= view.findViewById(R.id.cancel);
        final EditText sq2=view.findViewById(R.id.dob2_editText);
        mBuilder.setView(view);
        dialog=mBuilder.create();
        dialog.show();
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String d=sq2.getText().toString();
                if(!d.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    /*String URL = "secret_Q2_verification.php";
                    String URL_POST=BuildConfig.Port+URL;*/
                    String URL_POST=AppContext.getIp_address()+AppContext.getSecret_Q2_verification_url();
                    pDialog.setMessage("Connecting...");
                    pDialog.show();
                    try {
                        obj.put("secret_Q2", d);
                        obj.put("user_id", loginData.getUserId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    verify(URL_POST, obj);
                    dialog.dismiss();
                }
                else{
                    sq2.setError("This field can not be blank");
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
    private void dialog4(){
        Log.d(TAG,"POSSS");
        final Dialog d=new Dialog(context1);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.secret_ans);
        Button next=d.findViewById(R.id.button2);
        Button cancel=d.findViewById(R.id.button);
        final EditText ans1=d.findViewById(R.id.editText);
        final EditText ans2=d.findViewById(R.id.editText2);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String ans01=ans1.getText().toString();
                String ans02=ans2.getText().toString();
                if(!ans01.isEmpty() && !ans02.isEmpty() ) {
                    JSONObject obj = new JSONObject();
                    /*String URL = "secret_ans.php";
                    String URL_POST=BuildConfig.Port+URL;*/
                    String URL_POST=AppContext.getIp_address()+AppContext.getSecret_ans_url();
                    pDialog.setMessage("Connecting...");
                    pDialog.show();
                    try {
                        obj.put("secret_Ans1", ans01);
                        obj.put("secret_Ans2", ans02);
                        obj.put("user_id", loginData.getUserId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    d.dismiss();
                    verify(URL_POST, obj);

                }
                else{
                    if(ans01.isEmpty())
                        ans1.setError("This field can not be blank");
                    else
                        ans2.setError("This field can not be blank");
                }

            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void dialog123(String text){
        Log.d(TAG,"count="+count);
        if(count!=2) {
            final Dialog d=new Dialog(context1);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.verification);
            Button next=d.findViewById(R.id.next);
            final EditText verify=d.findViewById(R.id.verify);
            final TextView error=d.findViewById(R.id.error);
            Button cancel= d.findViewById(R.id.cancel);
            error.setText(text);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    code=verify.getText().toString();
                    Log.d(TAG,"CODD"+code);
                    if(!code.isEmpty()){
                        d.dismiss();
                        pDialog.show();
                        pDialog.setMessage("Connecting...");
                        boolean b=mBleService.connect(address);
                        Log.d(TAG,"connect/disconnect="+b);
                    }
                    else {
                        verify.setError("This field can not be blank");
                    }

                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.postDelayed(new Runnable() {
                        public void run() {
                            // Run your task here
                            try {
                                Log.d(TAG,"isresetCodeCorrectt="+isresetCodeCorrect);
                                if(isresetCodeCorrect){
                                    pDialog.dismiss();mBleService.disconnect();
                                    Toast.makeText(ResetDevicelistActivity.this, "Check the lock is already reset or retry the process again", Toast.LENGTH_SHORT).show();
                                }
                                isresetCodeCorrect=true;
                              }
                            catch (Exception e){e.printStackTrace();}
                        }
                    }, 30000 );
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG,"count1="+count);
                    count = 0;
                    d.dismiss();

                }
            });
            d.show();
        }
        else{
            count = 0;
            Toast.makeText(context1, "Try again later", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG,"dialog");
    }
    public void recallToresetDialog(String msg){
        if(msg.equals("R")) {
            isresetCodeCorrect=false;
            pDialog.dismiss();
            dialog123("Reset Code is incorrect");
        }
        else{
            isresetCodeCorrect=false;
            doorID = Utils.getModifiedMac(doorID).toUpperCase();
            Log.d(TAG,"door name= "+doorID);
            userMode.deleteData(doorID);
            pDialog.dismiss();
            Toast.makeText(context1, "Lock Reset successfully", Toast.LENGTH_LONG).show();
            //added for reset not work
            mBleService.disconnect();
            finish();
            //end
        }
    }
}
class loginData{
    private static String user_id;
    private static String password;
    private static String mac_id;
    private static boolean isBatteryStatusCount;
    static void setUserId(String id){
        user_id=id;
    }
    public static void setPassword(String pw){
        password=pw;
    }
    static void setMac_id(String mac){
        mac_id=mac;
    }
    public static String getUserId(){
        return user_id;
    }
    /*public static String getPassword(){
        return password;
    }*/
    static String getMac_id(){
        return mac_id;
    }

    static boolean isIsBatteryStatusCount() {
        return isBatteryStatusCount;
    }

    static void setIsBatteryStatusCount(boolean isBatteryStatusCount) {
        loginData.isBatteryStatusCount = isBatteryStatusCount;
    }
}