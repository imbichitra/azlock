package com.asiczen.azlock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import androidx.annotation.NonNull;

import com.asiczen.azlock.content.MySharedPreferences;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.BleBroadcastReceiver;
import com.asiczen.azlock.net.BleMessagingService;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;

import static com.asiczen.azlock.app.CommunicationError.INVALID_RESET_CODE;

/**
 * Created by user on 10/27/2015.
 */
public class Resetactivity extends AppCompatActivity implements Packet, OnDataSendListener {

    private AppContext appContext;
    private final String TAG = Resetactivity.class.getSimpleName();
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_DANGEROUS_PERMISSION = 11;
    private boolean locationPermission, isAboveVersion6;
    //private boolean readPhoneStatePermission;
    private BleMessagingService mBleService = null;
    public static BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private boolean isFlag = true;
    private boolean flag = true;
    private byte[] packet = new byte[16];

    private final byte[] key = AppContext.getAppKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    private boolean isBind;
    private AlertDialog dialog;
    LinearLayout main_layout;
    private MySharedPreferences sharedPreferences;
    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockadd);
        //Cancel = (TextView) findViewById(R.id.cancel);
       /* ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Azloc</font>"));*/
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3399FF")));
        appContext = AppContext.getContext();
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(Resetactivity.this);
        SessionManager sessionManager = new SessionManager(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        main_layout = findViewById(R.id.main_layout);
        Utils.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtAdapter = Utils.bluetoothAdapter;
        if (mBtAdapter == null) {
            Log.e(TAG, "BT adapter null");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        isAboveVersion6 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        appContext.setDeviceStatus(DeviceStatus.NO_DEVICE);
        appContext.setOnDataSendListener(this);

        Log.d(TAG, "Starting conectivity manager");

        /*smsSentBroadcastReceiver = new SmsSentBroadcastReceiver(this, new SmsSender.SmsStatusListener()
        {
            @Override
            public void onSmsStatusChange(int resultCode, String message)
            {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });*/

        /*smsDeliveryReportBroadcastReceiver = new SmsDeliveryReportBroadcastReceiver(this, new SmsSender.SmsStatusListener()
        {
            @Override
            public void onSmsStatusChange(int resultCode, String message)
            {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });*/
        sharedPreferences = new MySharedPreferences(this);

        LinearLayout battery_status = findViewById(R.id.battery_status);
        if(!sharedPreferences.getEmail().equals("admin@azlock.in")){
            battery_status.setVisibility(View.GONE);
        }
        battery_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFlag = true;
                appContext.setConnectionMode(ConnectionMode.SHOW_DIALOG);
                loginData.setIsBatteryStatusCount(true);
                scanList();
            }
        });

        TextView changePassword = findViewById(R.id.pwd);
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Resetactivity.this, ChangePassword.class);
                startActivity(intent);
            }
        });

        TextView logOut = findViewById(R.id.logout);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.logoutUser();
            }
        });
    }

    private void showProgressDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        TextView dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialogTextView.setText(R.string.connecting);
        dialog = builder.create();
        dialog.show();
    }
    private void batteryStatus() {
        byte[] packet = new byte[MAX_PKT_SIZE];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.BATTERY_COUNT_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_VISITOR;
        packet[REQUEST_PACKET_LENGTH_POS] = Packet.BatteryCount.PACKET_LENGTH;
        packet[RESPONSE_PACKET_LENGTH_POS] = Packet.BatteryCount.IDENTIFIER;
        onSend(packet);
    }

    public void showDialog(View v) {
        isFlag = true; // this flag is use to go either to the add lock activity or reset device list activity
        loginData.setIsBatteryStatusCount(false); // This flag is used to check for getting the battery status or reset the lock
        appContext.setConnectionMode(ConnectionMode.SHOW_DIALOG);
        scanList();
        Log.d(TAG, "setting connect mode as SHOW");
    }

    public void onClickadd(View view) {
        isFlag = false;
        appContext.setConnectionMode(ConnectionMode.SHOW_DIALOG);
        scanList();
        Log.d(TAG, "setting connect mode as SHOW");
    }

    private void scanList() {
        ConnectionMode connectionMode = appContext.getConnectionMode();
        if (connectionMode == ConnectionMode.SHOW_DIALOG) {
            if (isAboveVersion6 && !(locationPermission /*&& readPhoneStatePermission*/)) {
                Log.d(TAG, "requesting Dangerous Permission");
                requestDangerousPermission();
            }
            if (!mBtAdapter.isEnabled()) {
                Log.i(TAG, "onResume - BT not enabled yet");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                Log.d(TAG, "onResume/else");
                if (!isAboveVersion6 || (locationPermission /*&& readPhoneStatePermission*/)) {
                    if (!isBind)
                        doBindBleMessagingService();
                    if (isFlag) {
                        if (loginData.isIsBatteryStatusCount()) {
                            Intent newIntent = new Intent(this, ResetDevicelistActivity.class);
                            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        } else {
                            Intent newIntent = new Intent(this, ResetDevicelistActivity.class);
                            startActivity(newIntent);
                        }
                    } else {
                        Intent newIntent = new Intent(this, AddDevicelistActivity.class);
                        startActivity(newIntent);
                    }
                }
            }
        }
    }

    private void requestDangerousPermission() {
        locationPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        /*readPhoneStatePermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);*/
        /*smsPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)== PackageManager.PERMISSION_GRANTED);*/

        if (!locationPermission /*|| !readPhoneStatePermission || !smsPermission*/) {
            boolean shouldShowLocationPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            /*boolean shouldPhoneStatePermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE);*/
            /*shouldShowSmsPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS) ;*/
            Log.d("MainActivity", "Permission Rationale:" + shouldShowLocationPermissionRationale);
            // Should we show an explanation?
            if (shouldShowLocationPermissionRationale /*|| shouldPhoneStatePermissionRationale || shouldShowSmsPermissionRationale*/) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this).setTitle("Permission Denied")
                        .setCancelable(false)
                        .setMessage("Without these permissions the app is unable to use Wi-Fi and can not store any doorMode on this device. Are you sure you want to deny these permissions?")
                        .setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Process.killProcess(Process.myPid());
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("RETRY", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(Resetactivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                                /*Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE*/},
                                        REQUEST_DANGEROUS_PERMISSION);
                            }
                        }).create().show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                /*Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE*/}, REQUEST_DANGEROUS_PERMISSION);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_DANGEROUS_PERMISSION) {
            boolean isGranted = true;
            for (int grantResult : grantResults) {
                isGranted = isGranted && grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (isGranted) {
                locationPermission = true;
                //readPhoneStatePermission = true;
                scanList();
            } else {
                Process.killProcess(Process.myPid());
                System.exit(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //comment added for reset not work
        //sessionManager.logout();
        Log.d(TAG, "onBackPressed: ");
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        Log.d(TAG, "onStart() Called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() Called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() Called");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() Called");
        if (mBleService != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(BleStatusChangeReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            unbindService(mBleServiceConnection);
            mBleService.stopSelf();
            mBleService = null;
        }

        if (logoutBroadcastReceiver != null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }

        Runtime.getRuntime().gc();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "\nmserviceValue" + mBleService);
                    showProgressDialog();
                    mBleService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                    //if(isFlag==true)
                    scanList();
                    /*else if ((isAboveVersion6 && locationPermission && readPhoneStatePermission) || !isAboveVersion6) {
                        doBindBleMessagingService();
                        Log.d(TAG, "DeviceId (IMEI):" + appContext.getImei());
                        Intent newIntent = new Intent(this, AddDevicelistActivity.class);
                        // startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        startActivity(newIntent);
                    }*/
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onSend(byte[] data) {
        //encrypt and send 32 bytes of data to be sent in two writes of 16 bytes each
        for (int i = 0; i <= 1; i++) {
            System.arraycopy(data, i * 16, packet, 0, 16);
            try {
                packet = encode.AESEncode(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "encrypted packet");
            Utils.printByteArray(packet);
            mBleService.writeRXCharacteristic(packet);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "onSend/Sent Packet");
        Utils.printByteArray(data);
    }

    @Override
    public void onSend(byte[] data, OnDataAvailableListener onDataAvailableListener, final String progressMessage) {

        //encrypt and send 32 bytes of data to be sent in two writes of 16 bytes each
        if (appContext.getConnectionMode() == ConnectionMode.SHOW_DIALOG) {
            for (int i = 0; i <= 1; i++) {
                System.arraycopy(data, i * 16, packet, 0, 16);
                try {
                    packet = encode.AESEncode(packet);
                    Log.d(TAG, "encrypted packet");
                    Utils.printByteArray(packet);
                    mBleService.writeRXCharacteristic(packet);
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String s = new String(data, StandardCharsets.ISO_8859_1);
            Log.d(TAG, "Sent Packet:" + s);
            Utils.printByteArray(data);
        }
    }


    private void doBindBleMessagingService() {
        Intent bindIntent = new Intent(this, BleMessagingService.class);
        bindService(bindIntent, mBleServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(BleStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMessagingService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleMessagingService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMessagingService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleMessagingService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleMessagingService.DEVICE_DOES_NOT_SUPPORT_BLE);
        return intentFilter;
    }

    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            isBind = true;
            mBleService = ((BleMessagingService.LocalBinder) rawBinder).getService();
            ResetDevicelistActivity.mBleService = mBleService;
            Log.d(TAG, "onServiceConnected mBleService= " + mBleService);
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            isBind = false;
            Log.e(TAG, "BleMessagingService disconnected");
            mBleService = null;
        }
    };

    private static final Handler mHandler = new Handler() {
        @Override
        //Handler events that received from BLE service
        public void handleMessage(Message msg) {
            Log.d("ResetActivity", "mHandler/Message:" + msg);
        }
    };

    private final BleBroadcastReceiver BleStatusChangeReceiver = new BleBroadcastReceiver(this, new OnReceiveListener() {
        @Override
        public void onConnect() {
            Log.i(TAG, "BLE_CONNECT_MSG");
            appContext.setConnectionStatus(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (loginData.isIsBatteryStatusCount()) {
                        batteryStatus();
                    } else {
                        factoryReset();
                    }
                   /* new AlertDialog.Builder(mContext).setMessage("Do you want to reset?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {


                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                }
                            }).create().show();*/
                }
            }, 2500);

        }

        @Override
        public void onDisconnect() {
            if (dialog!=null && dialog.isShowing())
                dialog.dismiss();
            if (flag) {
                Log.e(TAG, "BLE_DISCONNECT_MSG");
                Toast.makeText(Resetactivity.this, "Connection Lost", Toast.LENGTH_LONG).show();
                mBleService.close();
                appContext.setConnectionStatus(false);
                appContext.setDeviceStatus(DeviceStatus.DEVICE_DISCONNECTED);
                //commentd for reset
                //sessionManager.logout();
            }
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onDataAvailable(String data) {
            Log.i(TAG, "onDataAvailable: " + data);
            if (dialog!=null && dialog.isShowing())
                dialog.dismiss();
            if (data.charAt(Packet.REQUEST_PACKET_TYPE_POS)==Utils.BATTERY_COUNT_REQUEST &&
                    data.charAt(HandshakePacket.REGISTRATION_STATUS_POS) == SUCCESS){
                byte[] b = data.getBytes(StandardCharsets.ISO_8859_1);
                for (byte b1:b)
                    Log.d(TAG, "battery data: "+String.format("%02X", b1));
                int count = (b[4]&0xff)<<24 | (b[5]&0xff)<<16 | (b[6]&0xff)<<8 | (b[7]&0xff) ;
                Snackbar.make(main_layout,"No of Battery operation: "+count, Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "onDataAvailable111 : "+count);
                flag = false;
                mBleService.dis();
            }
            if(data.charAt(Packet.REQUEST_PACKET_TYPE_POS)==Utils.FACTORY_RESET_REQ) {
                Log.d(TAG, ">> Reset command data avialable");
                @SuppressLint("DefaultLocale") int s = Integer.parseInt(String.format("%02d", (int) data.charAt(1)));
                Log.d(TAG, "data is:" + s);
                if (data.charAt(HandshakePacket.REGISTRATION_STATUS_POS) == FAILURE) {
                    Log.d(TAG, "RESET FAILED");
                    if (Integer.parseInt(String.format("%02d", (int) data.charAt(FactoryResetPacket.RESET_ERR_CODE_POS))) == INVALID_RESET_CODE) {
                        flag = false;
                        mBleService.dis();
                        ResetDevicelistActivity.count++;
                        new ResetDevicelistActivity().recallToresetDialog("R");
                    }
                } else {
                    Log.d(TAG, "else part");
                    Toast.makeText(Resetactivity.this, "Lock Reset successfully", Toast.LENGTH_LONG).show();
                    new ResetDevicelistActivity().recallToresetDialog("");
                }
            }
        }

        @Override
        public void onDataAvailable(byte[] data) {
        }

        @Override
        public void onServicesDiscovered() {
            mBleService.enableTXNotification();
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, "BLE error occurred");
            if (dialog!=null && dialog.isShowing())
                dialog.dismiss();
            mBleService.disconnect();
        }
    });

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    private void factoryReset() {
        String s = ResetDevicelistActivity.code;
        Log.d(TAG, "CODE" + s);
        int i = Integer.parseInt(s);
        byte[] packet = new byte[MAX_PKT_SIZE];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.FACTORY_RESET_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_VISITOR;
        packet[REQUEST_PACKET_LENGTH_POS] = Packet.FactoryResetPacket.SENT_PACKET_LENGTH;
        packet[Packet.FactoryResetPacket.RESET_POS] = 'R';
        packet[FactoryResetPacket.RESET_CODE_POS0] = (byte) (i >> 24);
        packet[FactoryResetPacket.RESET_CODE_POS1] = (byte) (i >> 16);
        packet[FactoryResetPacket.RESET_CODE_POS2] = (byte) (i >> 8);
        packet[FactoryResetPacket.RESET_CODE_POS3] = (byte) (i);
        Utils.printByteArray(packet);
        try {
            onSend(packet);
            Log.d(TAG, new String(packet, StandardCharsets.ISO_8859_1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




