package com.asiczen.azlock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.AppMode;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.LockStatus;
import com.asiczen.azlock.app.Notification;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.app.model.Owner;
import com.asiczen.azlock.app.model.User;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.MqttBroadCastReciver;
import com.asiczen.azlock.content.NetClientContext;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.SmsSentBroadcastReceiver;
import com.asiczen.azlock.net.BleBroadcastReceiver;
import com.asiczen.azlock.net.BleMessagingService;
import com.asiczen.azlock.net.MqttDataSendListener;
import com.asiczen.azlock.net.MqttInterface;
import com.asiczen.azlock.net.MqttMessageService;
import com.asiczen.azlock.net.MqttReceiveListener;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.CountDownTimer;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.SmsSender;
import com.asiczen.azlock.util.Utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 10/27/2015.
 */
public class ConnectActivity extends AppCompatActivity implements Packet,
        OnDataSendListener, MqttDataSendListener, NavigationView.OnNavigationItemSelectedListener {

    private Context mContext;
    private AppContext appContext;
    private SessionManager sessionManager;
    private ImageView internetLinearLayout;
    private final String TAG = ConnectActivity.class.getSimpleName();
    private NetworkChangeReceiver receiver;
    private IntentFilter filter;

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_DANGEROUS_PERMISSION = 11;
    private static final int REQUEST_OWNER_REGISTRATION = 1010;
    private static final int REQUEST_REMOTE_CONNECT_ACTIVITY = 75;
    private static final int REQUEST_BRIDGE_SELECT_DEVICE = 3;
    public static final String BATTERY_STATUS_EXTRA = "batteryStatus";
    public static final String TAMPER_STATUS_EXTRA = "tamperStatus";
    private boolean locationPermission, isAboveVersion6;
    //private boolean readPhoneStatePermission;
    private BleMessagingService mBleService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    //private SmsDeliveryReportBroadcastReceiver smsDeliveryReportBroadcastReceiver;
    private OnDataAvailableListener mOnDataAvailableListener;
    private int batteryStatus;
    //private ProgressBar pbar;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    //private AlertDialog alertDialog;
    private AlertDialog alertDialog1;
    //private HashSet<WifiNetwork> bridgeSsidSuggestionSet;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private boolean isReceiverRegistered;
    private CountDownTimer countDownTimer;
    private TextView dialogTextView;

    private UserMode userMode;
    private byte[] packet = new byte[16];
    private final byte[] key = AppContext.getAppKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    private boolean flag;
    private DatabaseHandler databaseHandler;
    private ArrayList<AppContext.DisplayTableContent> list;
    private String deviceAddress;
    private String deviceName;
    private String address = "";
    private AlertDialog dialog;
    //TextView progressdialog;

    private boolean isBind = false;
    private boolean isBleServiceConnected = false;
    private MqttMessageService mqttMessageService;
    private MqttInterface mqttInterface;
    private byte[] mattSentData;
    private SharedPreferences sharedpreferences;
    private List<BridgeDetail> bridgeDetails = new ArrayList<>();
    private boolean isInternetAvaulable;
    //final ViewGroup nullParent = null;

    DrawerLayout drawerLayout;
    //private MqttDataSendListener mqttDataSendListener;
    private int isDisconOrUnsub = -1; //it is used for check weather to send disconnect packet to  mqtt or unsubscribe to mqtt.
	private MySharedPreferences sharedPreferences;

	public static final int CONNECT_RECENT_LOCK = 0;
    public static final int OPEN_KEY = 3;
	public static int  actionToPerform = -1;
	private boolean isMqttConnected = false;
	private boolean isSubscribed = false;
	private boolean isDoubleBackPressed = false;
    ArrayList<ShortcutInfo> shortcutInfos = new ArrayList<>();
    private final int NO_TEMP_FLAG = 0; //Both temper flag and unauthorized access is not set to 0(binary form 0000 0000 8bit)
    static final int TEMP_FLAG_ENABLE = 17; //Boh temper flag and unauthorized is enabled (binary form 0001 0001 8bit)
    static final int UNAUTHORIZED_ACCESS = 1; //Unauthorized access flag is enabled (binary form 0000 0001 8bit)
    private final int TEMPER_NOTIFICATION_ON = 16;//Temper notification is on (binary form 0001 0000 8bit)
    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_mode);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setItemIconTintList(null);
        setupDrawerContent(navigationView);
        View headerView = navigationView.getHeaderView(0);

        TextView text_email = headerView.findViewById(R.id.txt_email);

        ImageView imageView = findViewById(R.id.nav_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        mContext = this;
        appContext = AppContext.getContext();
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(ConnectActivity.this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, mContext, wifiBroadcastListener);
        userMode = new UserMode(this);
        sharedpreferences = getSharedPreferences(Utils.BRIDGE_FILE, Context.MODE_PRIVATE);
        Utils.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtAdapter = Utils.bluetoothAdapter;
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        sharedPreferences = new MySharedPreferences(this);
        text_email.setText(sharedPreferences.getEmail());
        isAboveVersion6 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        //commented for android Q compatibility
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.setImei(telephonyManager.getImei());
        } else {
            appContext.setImei(telephonyManager.getDeviceId());
            Log.d(TAG, "IMEI: "+appContext.getImei());
        }*/
        appContext.setDeviceStatus(DeviceStatus.NO_DEVICE);
        appContext.setOnDataSendListener(this);
        appContext.setMqttSendListener(this);
        internetLinearLayout = findViewById(R.id.internet_linearLayout);
       /* ImageView connectInternet =  findViewById(R.id.connect_internet_imageView);

        int color = Color.parseColor("#FFFFFF");
        connectInternet.setColorFilter(color);
        ImageView connectWifi =  findViewById(R.id.connect_wifi_imageView);
        connectWifi.setColorFilter(color);*/

        Log.d(TAG, "Starting conectivity manager");
        filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkChangeReceiver();

        //private boolean isSmsDeliveryReportBroadcastReceiverSet, isSmsSentBroadcastReceiverSet;
        SmsSentBroadcastReceiver smsSentBroadcastReceiver = new SmsSentBroadcastReceiver(this, new SmsSender.SmsStatusListener() {
            @Override
            public void onSmsStatusChange(int resultCode, String message) {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });

        /*smsDeliveryReportBroadcastReceiver = new SmsDeliveryReportBroadcastReceiver(this, new SmsSender.SmsStatusListener()
        {
            @Override
            public void onSmsStatusChange(int resultCode, String message)
            {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });*/

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView = getLayoutInflater().inflate(R.layout.progressbar, null, false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();


        databaseHandler = new DatabaseHandler(this);
        list = databaseHandler.getDataFromDisplayTable();
        int l = list.size();
        Log.d(TAG, "length=" + l);
        LinearLayout b1 = findViewById(R.id.b1);
        LinearLayout b2 = findViewById(R.id.b2);
        LinearLayout b3 = findViewById(R.id.b3);
        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);
        TextView lock_one, lock_two, lock_three;
        lock_one = findViewById(R.id.lock_one);
        lock_two = findViewById(R.id.lock_two);
        lock_three = findViewById(R.id.lock_three);
        if (l > 0) {
            switch (l) {
                case 1:
                    lock_one.setText(list.get(0).getDootName());
                    b1.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    lock_one.setText(list.get(0).getDootName());
                    lock_two.setText(list.get(1).getDootName());
                    b1.setVisibility(View.VISIBLE);
                    b2.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    lock_one.setText(list.get(0).getDootName());
                    lock_two.setText(list.get(1).getDootName());
                    lock_three.setText(list.get(2).getDootName());
                    b1.setVisibility(View.VISIBLE);
                    b2.setVisibility(View.VISIBLE);
                    b3.setVisibility(View.VISIBLE);
                    break;
            }
        }
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                address = list.get(0).getMacId();
                appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_BLE);
                connect();
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                address = list.get(1).getMacId();
                appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_BLE);
                connect();
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                address = list.get(2).getMacId();
                appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_BLE);
                connect();
            }
        });

        if (actionToPerform !=-1){
            switch (actionToPerform){
                case 0:
                case 1:
                case 2:
                    resentLockConnect(actionToPerform);
                    break;
                case 3:
                    showkey();
                    break;
            }
            actionToPerform = -1;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            if (!list.isEmpty() && list.size()<=3){
                addShortcutInfo(list.size(),list);
            }
            createShortcutInfo("Key",
                    "Key",
                    "id1",
                    R.drawable.ic_key,
                    getIntent(ConnectActivity.OPEN_KEY)
            );

            if (shortcutManager != null) {
                shortcutManager.setDynamicShortcuts(shortcutInfos);
            }
        }
    }
    private Intent getIntent(int value){
        Intent intent = new Intent(this, SlideViewActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("val",value);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addCategory("android.shortcut.conversation");
        return intent;
    }
    private void addShortcutInfo(int length, ArrayList<AppContext.DisplayTableContent> list){
        for (int i=0;i<length;i++){
            Log.d(TAG, "addShortcutInfo: "+list.get(i).getDootName());
            createShortcutInfo(list.get(length-i-1).getMacId(),
                    list.get(length-i-1).getDootName(),
                    "id"+(i+2),
                    R.drawable.ic_lock,
                    getIntent(length-i-1)
            );
        }
    }
    private void createShortcutInfo(String shortLabel,String logLabel,String id,int icon,Intent intent){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id)
                    .setShortLabel(shortLabel)
                    .setLongLabel(logLabel)
                    .setIcon(Icon.createWithResource(this, icon))
                    .setIntent(intent)
                    .build();

            shortcutInfos.add(shortcut);
        }
    }

    private void resentLockConnect(int i){
        flag = true;
        address = list.get(i).getMacId();
        appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_BLE);
        connect();
    }
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (menuItem.getItemId()) {
                    case R.id.nav_settings:
                        Intent k = new Intent(ConnectActivity.this, settings.class);
                        startActivity(k);
                        break;
                    case R.id.nav_key:
                        showkey();
                        break;
                    case R.id.nav_guide:
                        Intent guideIntent = new Intent(ConnectActivity.this, GuideActivity.class);
                        Log.d("Main", "Starting GuideActivity");
                        startActivity(guideIntent);
                        break;
                    case R.id.nav_about:
                        Intent aboutIntent = new Intent(ConnectActivity.this, AboutActivity.class);
                        Log.d("Main", "Starting AboutActivity");
                        startActivity(aboutIntent);
                        break;
                    case R.id.nav_support:
                        //userlogin.issueRaise = "YES";
                        loginData.setUserId(sharedPreferences.getEmail());
                        loginData.setPassword(sharedPreferences.getPassword());
                        Intent i = new Intent(ConnectActivity.this, RaiseIssue.class);
                        startActivity(i);
                        break;
                    case R.id.nav_youtube:
                        loadYoutube();
                        break;
                }
            }
        }, 200);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadYoutube() {
        try {
            String url = "https://www.youtube.com/channel/UCIynVF90fNgjK-tMg3lLKaA";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.google.android.youtube");
            intent.setData(Uri.parse(url));
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void showkey() {
        LayoutInflater layoutInflater = LayoutInflater.from(ConnectActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_key, null, false);
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(ConnectActivity.this);
        alertDialogBuilder.setView(promptView);
        final TextView textView = promptView.findViewById(R.id.textView);
        ImageView imageView = promptView.findViewById(R.id.qrGenerator);
        String msg = "Key -";
        textView.setText(msg + sharedPreferences.getMac());
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(sharedPreferences.getMac(), BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        android.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void onClickInternetLayout(View v) {
        if (isInternetAvaulable) {
            if (!isBind)
                bindMqttService(); // bind service
            else if (!isMqttConnected && mqttMessageService != null){
                mqttMessageService.connectMqtt();
            }

            Log.d(TAG, "onClickInternetLayout: " + isBind);
            Log.d(TAG, "setting cntect mode as CONNECTION_MODE_REMOTE");
            appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_REMOTE);
            connect();
        } else {
            Toast.makeText(this, "Please check your internet connectivity", Toast.LENGTH_SHORT).show();
        }
        //goToRemoteConnectActivity();
    }


    public void onClickBleLayout(View v) {
        flag = false;
        Log.d(TAG, "setting connect mode as CONNECTION_MODE_BLE");
        appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_BLE);
        connect();
    }

    @SuppressLint("HardwareIds")
    private void connect() {
        ConnectionMode connectionMode = appContext.getConnectionMode();
        switch (connectionMode) {
            case CONNECTION_MODE_BLE:
                if (isAboveVersion6 && !(locationPermission/* && readPhoneStatePermission*/)) {
                    Log.d(TAG, "requesting Dangerous Permission");
                    requestDangerousPermission();
                } else if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onResume - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    Log.d(TAG, "onResume/else");
                    if (!isAboveVersion6 || (locationPermission /*&& readPhoneStatePermission*/)) {
                        if (!isBleServiceConnected)
                            doBindBleMessagingService();
                        if (flag) {
                            DeviceListActivity.isScannCall = true;  //connecting most frequently lock
                            Intent newIntent = new Intent(this, DeviceListActivity.class);
                            newIntent.putExtra("mylist", address);
                            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        } else {
                            DeviceListActivity.isScannCall = false;
                            Intent newIntent = new Intent(this, DeviceListActivity.class);
                            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        }
                    }
                }
                break;

            case CONNECTION_MODE_REMOTE:
                final View routerConnectView = getLayoutInflater().inflate(R.layout.router_connect, null, false);

                //final AutoCompleteTextView routerIpTextView = routerConnectView.findViewById(R.id.routerIpTextView);
                //final EditText routerPortEditText = routerConnectView.findViewById(R.id.routerPortEditText);

                if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
                    Toast.makeText(this, "Static bridge is under implementation", Toast.LENGTH_SHORT).show();
                    /*final View routerConnectView = getLayoutInflater().inflate(R.layout.router_connect, null);

                    final AutoCompleteTextView routerIpTextView = (AutoCompleteTextView) routerConnectView.findViewById(R.id.routerIpTextView);
                    final EditText routerPortEditText = (EditText) routerConnectView.findViewById(R.id.routerPortEditText);

                    final FileAccess fileAccess = new FileAccess(mContext, Utils.ROUTER_DETAILS_SUGGESTION_FILE);
                    final String readFile = fileAccess.read();
                    String[] routerSuggestions;
                    final HashSet<String> routerSuggestionSet;
                    if (fileAccess.FILE_NOT_FOUND || readFile == null) {
                        routerSuggestions = new String[]{};
                        routerSuggestionSet = new HashSet<>(Arrays.asList(routerSuggestions));
                    } else {
                        routerSuggestions = readFile.split(",");
                        routerSuggestionSet = new HashSet<>(Arrays.asList(routerSuggestions));
                    }
                    Log.d(TAG, "routerSuggestionSet:" + routerSuggestionSet);

                    ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, routerSuggestions);

                    routerIpTextView.setAdapter(adapter);
                    routerIpTextView.setThreshold(1);
                    alertDialog = new AlertDialog.Builder(mContext).setView(routerConnectView)
                            .setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create();
                    alertDialog.show();
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            routerIp = routerIpTextView.getText().toString();
                            routerPort = routerPortEditText.getText().toString();
                            boolean validatePort = true;
                            int port = -1;
                            try {
                                port = Integer.parseInt(routerPort);
                            } catch (NumberFormatException e) {
                                validatePort = false;
                            }

                            if (routerIp != null && !routerIp.isEmpty() && validatePort) {
                                if (!routerSuggestionSet.contains(routerIp)) {
                                    if (readFile != null) {
                                        fileAccess.write(readFile + routerIp + ",");
                                    } else {
                                        fileAccess.write(routerIp + ",");
                                    }
                                }
                                appContext.setRouterInfo(new RouterInfo(routerIp, port));
                                alertDialog.dismiss();
                                Intent remoteConnectIntent = new Intent(ConnectActivity.this, RemoteConnectActivity.class);
                                startActivityForResult(remoteConnectIntent, REQUEST_REMOTE_CONNECT_ACTIVITY);
                            } else {
                                if (!validatePort) {
                                    Toast.makeText(ConnectActivity.this, "port is not valid", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(ConnectActivity.this, "IP address is not valid", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });*/
                } else {
                    //bindMqttService();
                    try {
                        if (bridgeDetails.size() > 0)
                            bridgeDetails.clear();
                        bridgeDetails = databaseHandler.getBridgeData(0);
                        if (bridgeDetails.size() == 1) {
                            appContext.setBridgeDetail(bridgeDetails.get(0));
                            isMQTTConnected();
                        } else {
                            if (bridgeDetails.size() == 0) {
                                goToBridgeActivity();
                            } else {
                                Intent i = new Intent(this, ShowBridgeListActivity.class);
                                startActivityForResult(i, REQUEST_BRIDGE_SELECT_DEVICE);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                break;
        }
    }

    private void goToBridgeActivity(){
        Intent addBridge = new Intent(this, BridgeActivity.class);
        addBridge.putExtra(Utils.STATUS, "CONNECT");
        startActivity(addBridge);
    }

    private void goToRemoteConnectActivity() {
        Intent remoteConnectIntent = new Intent(ConnectActivity.this, RemoteConnectActivity.class);
        startActivityForResult(remoteConnectIntent, REQUEST_REMOTE_CONNECT_ACTIVITY);
        appContext.setConnectionMode(ConnectionMode.CONNECTION_MODE_REMOTE);
    }

    private void requestDangerousPermission() {
        locationPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
//        readPhoneStatePermission = (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);

        if (!locationPermission /*|| !readPhoneStatePermission*/) {
            boolean shouldShowLocationPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            /*boolean shouldPhoneStatePermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE);*/
            Log.d("MainActivity", "Permission Rationale:" + shouldShowLocationPermissionRationale);
            // Should we show an explanation?
            if (shouldShowLocationPermissionRationale /*|| shouldPhoneStatePermissionRationale*/ /*|| shouldShowSmsPermissionRationale*/) {
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
                                ActivityCompat.requestPermissions(ConnectActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION/*,
                                                Manifest.permission.SEND_SMS*//*, Manifest.permission.READ_PHONE_STATE*/},
                                        REQUEST_DANGEROUS_PERMISSION);
                            }
                        }).create().show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION/*,
                                Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE*/}, REQUEST_DANGEROUS_PERMISSION);
            }
        } else {
            connect();
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
                connect();
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {
                Process.killProcess(Process.myPid());
                System.exit(0);
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onBackPressed() {
       /* new AlertDialog.Builder(mContext).setMessage("Do you want to exit?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.exit();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();*/

        if (isDoubleBackPressed){
            //super.onBackPressed();

            sessionManager.exit();
            return;
        }else {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_LONG).show();
        }
        isDoubleBackPressed = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isDoubleBackPressed = false;
            }
        },2000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, filter);
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        Log.d(TAG, "onStart() Called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
        Log.d(TAG, "onStop() Called");
    }

    @Override
    protected void onPause() {
        if (wifiBroadcastReceiver != null && isReceiverRegistered) {
            Log.d(TAG, "wifi receiver unregistered");
            isReceiverRegistered = false;
            unregisterReceiver(wifiBroadcastReceiver);
        }
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
                Log.e(TAG, e.toString());
            }
            unbindService(mBleServiceConnection);
            mBleService.stopSelf();
            mBleService = null;
        }

        stopMqtt();
       /* if(smsSentBroadcastReceiver!=null  && isSmsSentBroadcastReceiverSet) {
            unregisterReceiver(smsSentBroadcastReceiver);
        }
        if(smsDeliveryReportBroadcastReceiver!=null && isSmsDeliveryReportBroadcastReceiverSet) {
            unregisterReceiver(smsDeliveryReportBroadcastReceiver);
        }*/
        if (logoutBroadcastReceiver != null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
		/* NEED to be looked for possible memory leak
        if (((BitmapDrawable) connectInternet.getDrawable()).getBitmap() != null &&
                !((BitmapDrawable) connectInternet.getDrawable()).getBitmap().isRecycled()) {
            ((BitmapDrawable) connectInternet.getDrawable()).getBitmap().recycle();
            connectInternet.setImageDrawable(null);
            connectInternet.setImageBitmap(null);
        }
        if (((BitmapDrawable) connectWifi.getDrawable()).getBitmap() != null &&
                !((BitmapDrawable) connectWifi.getDrawable()).getBitmap().isRecycled()) {
            ((BitmapDrawable) connectWifi.getDrawable()).getBitmap().recycle();
            connectWifi.setImageDrawable(null);
            connectWifi.setImageBitmap(null);
        }*/
        Runtime.getRuntime().gc();
        super.onDestroy();
    }

    private void stopMqtt(){
        if (mqttMessageService != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttBroadCastReciver);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            unbindService(mqttConnection);
            mqttMessageService.stopSelf();
            mqttMessageService = null;
            isBind = false;
            isMqttConnected = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    deviceName = data.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);


                    dialogTextView.setText(R.string.connecting);
                    dialog.show();

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "\nmserviceValue" + mBleService);
                    //added for reset not work
                    if (mBleService != null)
                        mBleService.connect(deviceAddress);
                    else
                        doBindBleMessagingService();
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                    connect();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_OWNER_REGISTRATION:
                int registrationSuccessExtra = Integer.MIN_VALUE; //renameDoorExtra=Integer.MIN_VALUE;
                if (data != null) {
                    registrationSuccessExtra = data.getIntExtra(RegisterOwnerActivity.REGISTRATION_SUCCESS_EXTRA, Integer.MIN_VALUE);
                    int renameDoorExtra = data.getIntExtra(RegisterOwnerActivity.RENAME_SUCCESS_EXTRA, Integer.MIN_VALUE);
                    Log.d(TAG, "onActivityResult: azLock " + registrationSuccessExtra + " " + renameDoorExtra);
                }
                if (resultCode == RESULT_OK) {
                    if (registrationSuccessExtra == Utils.SUCCESS) {
                        Log.d(TAG, "Registration Successful");
                        Intent homeIntent = new Intent(this, HomeActivity.class);
                        homeIntent.putExtra(BATTERY_STATUS_EXTRA, batteryStatus);
                        startActivity(homeIntent);
                    }
                } else {
                    //added after raise issue in not going to home page after owner registration
                    mBleService.disconnect();
                    Log.d(TAG, "RESULT_CANCELLED");
                }
                break;

            case REQUEST_REMOTE_CONNECT_ACTIVITY:
                if (resultCode == Activity.RESULT_OK) {
                    String handshakePacket = data.getStringExtra("HANDSHAKE_PACKET");
                    doorID = data.getStringExtra("DOOR_ID");
                    Log.d(TAG, "doorid=" + doorID);
                    doorName = data.getStringExtra("DOOR_NAME");
                    authenticate(handshakePacket, doorID, doorName);
                }
                break;
            case REQUEST_BRIDGE_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {

                    String bridgeId = data.getStringExtra(Utils.BRIDGE_ID);
                    String bridgePassword = data.getStringExtra(Utils.BRIDGE_PASSWORD);
                    Log.d(TAG, "onActivityResult: bridgeId " + bridgeId + " bridgePassword " + bridgePassword);
                    appContext.setBridgeDetail(new BridgeDetail(bridgeId, bridgePassword));
                    isMQTTConnected();
                }
                break;
        }
    }

    private void isMQTTConnected(){
        Log.d(TAG, "isMQTTConnected: "+isMqttConnected);
        dialogTextView.setText(R.string.connecting);
        dialog.show();
        final int WAIT_TIME = 10000, INTERVAL = 1000;
        countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isBind && isMqttConnected){
                    Log.d(TAG, "onTick: CALLED"+isMqttConnected);
                    dialog.dismiss();
                    countDownTimer.cancel();
                    goToRemoteConnectActivity();
                }
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
                if (isBind && isMqttConnected) {
                    Log.d(TAG, "onFinish: called");
                    goToRemoteConnectActivity();
                }else
                    Toast.makeText(ConnectActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }

    /*
    * Send data to Ble device*/
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
            //Log.d(TAG,"encrypted packet");
            // Utils.printByteArray(packet);
            mBleService.writeRXCharacteristic(packet);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "onSend/Sent Packet");
        // Utils.printByteArray(data);
    }

    /*
    * Receive data from sender activity and send data to Ble device*/
    @Override
    public void onSend(byte[] data, OnDataAvailableListener onDataAvailableListener, final String progressMessage) {
        mOnDataAvailableListener = onDataAvailableListener;
        //encrypt and send 32 bytes of data to be sent in two writes of 16 bytes each
        if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE) {
            for (int i = 0; i <= 1; i++) {
                System.arraycopy(data, i * 16, packet, 0, 16);
                try {
                    packet = encode.AESEncode(packet);
                    Utils.printByteArray(packet);
                    Log.d(TAG, "encrypted packet");
                    //Utils.printByteArray(packet);
                    mBleService.writeRXCharacteristic(packet);
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String s = new String(data, StandardCharsets.ISO_8859_1);
            Log.d(TAG, "Sent Packet:" + s);
            // Utils.printByteArray(data);
        } else if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
            if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
                Log.w(TAG, "Connecting [Address=" + appContext.getRouterInfo().getAddress() + " Port="
                        + appContext.getRouterInfo().getPort() + "]");
                NetClientContext clientContext = NetClientContext.getContext();
                NetClientAsyncTask clientAsyncTask = new NetClientAsyncTask(true, ConnectActivity.this, appContext.getRouterInfo().getAddress(),
                        appContext.getRouterInfo().getPort(), data, new OnTaskCompleted<String>() {
                    @Override
                    public void onTaskCompleted(int resultCode, String value) {
                        Log.d(TAG, "onTaskCompleted:" + value);
                        Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                        if (resultCode == Activity.RESULT_OK) {
                            Log.d(TAG, "Sending doorMode to listener");
                            mOnDataAvailableListener.onDataAvailable(value);
                        } else {
                            if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                                Toast.makeText(mContext, "Unable to connect", Toast.LENGTH_LONG).show();
                            } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                                Toast.makeText(mContext, "Timeout occurred", Toast.LENGTH_LONG).show();
                            }
                            mOnDataAvailableListener.onDataAvailable(null);
                        }
                    }
                });
                if (progressMessage != null) {
                    clientAsyncTask.showProgressDialog(true, progressMessage);
                }
                clientContext.setNetClient(clientAsyncTask);
                clientAsyncTask.execute();
            }else{
                byte[] en_data = Utils.encriptData(data);
                sendData(en_data, "", "",MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                    @Override
                    public void dataAvailable(byte[] data) {
                        String value = Utils.getPacketData(data);
                        if(mOnDataAvailableListener !=null)
                            mOnDataAvailableListener.onDataAvailable(value);
                    }

                    @Override
                    public void timeOutError() {
                        mOnDataAvailableListener.onDataAvailable(null);
                        Toast.makeText(ConnectActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void unableToSubscribe() {

                    }

                    @Override
                    public void succOrFailToUnSubscribe() {

                    }
                });
            }
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (isNetworkAvailable(context)) {
                internetLinearLayout.setClickable(true);
                isInternetAvaulable = true;
            } else {
                //internetLinearLayout.setClickable(false);
                isInternetAvaulable = false;
            }
        }

        private boolean isNetworkAvailable(Context context) {
            ConnectivityManager connectivity = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = null;
            if (connectivity != null) {
                netInfo = connectivity.getActiveNetworkInfo();
            }
            return netInfo != null && netInfo.isConnected();
        }
    }

    /*
    * Register service and bind it and also register broad cast receiver to receive data from BleMessagingService.
    * */
    private void doBindBleMessagingService() {
        Intent bindIntent = new Intent(this, BleMessagingService.class);
        bindService(bindIntent, mBleServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(BleStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    /*
    * Register what type of action that the Broadcast receiver want to listen*/
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMessagingService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleMessagingService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMessagingService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleMessagingService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleMessagingService.DEVICE_DOES_NOT_SUPPORT_BLE);
        return intentFilter;
    }

    /*
    * The call back method get called by BleMessagingService when service is successfully bind or unbind
    * here we get a instance of BleMessagingService to send data to service*/
    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mBleService = ((BleMessagingService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mBleService= " + mBleService);
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            isBleServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            Log.e(TAG, "BleMessagingService disconnected");
            mBleService = null;
            isBleServiceConnected = false;
        }
    };

    private static final Handler mHandler = new Handler() {
        @Override
        //Handler events that received from BLE service
        public void handleMessage(Message msg) {
            Log.d("ConnectActivity", "mHandler/Message:" + msg);
        }
    };

    private String doorID, doorName;

    /*
    * Here we get all action thrown from the ble device it may be data,connect or disconnect etc.
    * It work as a bridge between service and activity ,we get all data from the device and send data back
    * to corresponding caller activity*/
    private final BleBroadcastReceiver BleStatusChangeReceiver = new BleBroadcastReceiver(this, new OnReceiveListener() {

        /*
        *onConnect is called when ble is successfully connected */
        @Override
        public void onConnect() {
            //String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.i(TAG, "BLE_CONNECT_MSG");
            appContext.setConnectionStatus(true);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    byte[] packet = new byte[MAX_PKT_SIZE];
                    packet[REQUEST_PACKET_TYPE_POS] = Utils.HANDSHAKE_REQ;
                    packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_VISITOR;
                    packet[REQUEST_PACKET_LENGTH_POS] = HandshakePacket.SENT_PACKET_LENGTH;
                    String mac = sharedPreferences.getMac();

                    // convert mac in to byte
                    //Log.d(TAG,"IMEI:"+appContext.getImei()+"MAC"+mac);
                    byte[] macHexBytes = Utils.toByteArray(mac);
                    System.arraycopy(macHexBytes, 0, packet, 3, Utils.PHONE_MAC_ID_LEN_IN_HEX);

                    //time stamp is added in 14,15,29 and 30 number byte for security purpose
                    packet[Utils.TIME_STAMP] = Utils.getTime()[0];
                    packet[Utils.TIME_STAMP + 1] = Utils.getTime()[1];
                    packet[Utils.TIME] = Utils.getTime()[0];
                    packet[Utils.TIME + 1] = Utils.getTime()[1];

                    //Log.d(TAG, "WhoAmI Sent:");
                    // Utils.printByteArray(packet);
                    try {
                        onSend(packet);
                        Log.d(TAG, new String(packet, StandardCharsets.ISO_8859_1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 2500);
        }

        /*
         *onDisconnect is called when ble is successfully disconnected */
        @Override
        public void onDisconnect() {
            Log.e(TAG, "BLE_DISCONNECT_MSG");
            Toast.makeText(ConnectActivity.this, "Connection Lost", Toast.LENGTH_LONG).show();
            mBleService.close();
            appContext.setConnectionStatus(false);
            appContext.setDeviceStatus(DeviceStatus.DEVICE_DISCONNECTED);
            sessionManager.logout();
            if (mBtAdapter != null){
                mBtAdapter.disable();
                try {
                    Thread.sleep(1000);
                    mBtAdapter.enable();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /*It receive the data send from Ble device*/
        @Override
        public void onDataAvailable(String data) {

            doorName = mDevice.getName();
            Log.d(TAG, "onDataAvailable: " + data);

            if (data.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.HANDSHAKE_REQ) {
                Log.d(TAG, ">> onDataAvailable: Handshake Packet Received");
                doorID = mDevice.getAddress();
                databaseHandler.insertDisplayTableInfo(deviceName, deviceAddress);
                authenticate(data, doorID, doorName);

            } else {
                mOnDataAvailableListener.onDataAvailable(data);
            }

            dialog.dismiss();
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

            dialog.dismiss();
            Toast.makeText(mContext, "Connection Lost", Toast.LENGTH_SHORT).show();
            mBleService.disconnect();
        }
    });

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    /*
    * It process the Who am i packet */
    private void authenticate(String packet, String deviceAddress, String deviceName) {
        DatabaseHandler db = new DatabaseHandler(mContext);
        appContext.updateNotificationStatus(Notification.TAMPER, mContext);
        appContext.setConnectionStatus(true);
        try {
            appContext.setDeviceStatus(DeviceStatus.DEVICE_HANDSHAKED);
            char registrationStatus = packet.charAt(HandshakePacket.REGISTRATION_STATUS_POS);
            Log.d(TAG, "registrationStatus:" + registrationStatus);
            AppMode appMode = (registrationStatus == Utils.OWNER_REGISTERED ||
                    registrationStatus == Utils.OWNER_NOT_REGISTERED) ? AppMode.OWNER : AppMode.GUEST;
            appContext.setAppMode(appMode);
            doorID = Utils.getModifiedMac(deviceAddress).toUpperCase();
            Log.d(TAG, "door Id, BLE MAC=" + doorID);
            Door door = new Door(doorID, deviceName);
            appContext.setDoor(door);
            batteryStatus = Utils.parseInt(packet, HandshakePacket.BATTERY_STATUS_POS);
            Log.d("MainActivity", "Battery Status:" + Utils.parseInt(packet, HandshakePacket.BATTERY_STATUS_POS));
            boolean isGuestRegistered = (registrationStatus == Utils.GUEST_REGISTERED);
            boolean isOwnerRegistered = (registrationStatus == Utils.OWNER_REGISTERED);
            char lockStatus = packet.charAt(HandshakePacket.DOOR_STATUS_POS);
            appContext.setLockStatus(lockStatus == Utils.LOCKED ? LockStatus.LOCKED : LockStatus.UNLOCKED);
            if (appMode == AppMode.OWNER) {
                int isTemperEnable=packet.charAt(16);
                Log.d(TAG, "isTemperEnable: "+isTemperEnable);
                appContext.setTemperStatus(isTemperEnable == TEMP_FLAG_ENABLE || isTemperEnable == TEMPER_NOTIFICATION_ON);
                int ajarStatue = Utils.parseInt(packet, AjarPacket.AJAR_STATUS);
                int autolockStatus = Utils.parseInt(packet, AjarPacket.AUTOLOCK_STATU);
                int autoLockTime = Utils.parseInt(packet, AjarPacket.AUTOLOCK_TIME);
                appContext.setAjarStatus(ajarStatue);
                appContext.setAutolockStatus(autolockStatus);
                appContext.setAutolockTime(autoLockTime);
                Log.d(TAG, "isTemperEnable get: "+appContext.getTemperStatus());
                Log.d(TAG, "authenticate: ajarStatue =" + Utils.parseInt(packet, AjarPacket.AJAR_STATUS) + " autolockStatus=" + autolockStatus + " autoLockTime=" + autoLockTime);
                if (!isOwnerRegistered) {
                    //check db
                    Cursor res = userMode.getData(doorID);
                    if (res.getCount() <= 0) {
                        userMode.insertData(doorID, "owner");
                    } else {
                        res.moveToFirst();
                        do {
                            if (res.getString(1).equals("guest")) {
                                userMode.updateData(doorID, "owner");
                            } else
                                break;

                        } while (res.moveToNext());
                        res.close();
                    }
                    // Owner Not Registered
                    Log.d("MainActivity", "Owner Not Registered, Starting Master Registration");
                    Intent registerOwnerIntent = new Intent(this, RegisterOwnerActivity.class);
                    registerOwnerIntent.putExtra(RegisterOwnerActivity.DOOR_ID_EXTRA, doorID);
                    startActivityForResult(registerOwnerIntent, REQUEST_OWNER_REGISTRATION);
                } else {
                    //check db
                    Cursor res = userMode.getData(doorID);
                    if (res.getCount() <= 0) {
                        userMode.insertData(doorID, "owner");
                    } else {
                        res.moveToFirst();
                        do {
                            if (res.getString(1).equals("guest")) {
                                userMode.updateData(doorID, "owner");
                            } else
                                break;

                        } while (res.moveToNext());
                        res.close();
                    }
                    //!close checking
                    Log.d("MainActivity", "Owner Registered");
                    // Owner Registered
                    // Check the doorMode is present in DB or not. If not, insert it (may be someone clear App cache).

                    User whoLoggedIn = db.getUser(sharedPreferences.getMac());
                    Door whichDoor = db.getDoor(doorID);
                    if (whichDoor == null) {
                        whichDoor = new Door();
                        whichDoor.setName(deviceName);
                        whichDoor.setId(doorID);
                        Log.d("MainActivity", "Recovering door details");
                        if (db.insert(whichDoor)) {
                            Log.d("MainActivity", "Owner's doorMode has been recovered Successfully");
                        } else {
                            Log.d("MainActivity", "[Error]: Owner's doorMode cannot be recovered");
                        }
                    }
                    appContext.setDoor(whichDoor);
                    if (whoLoggedIn == null) {
                        Log.d(TAG, "Handshake packet [name end]:" + packet.substring(
                                HandshakePacket.OWNER_NAME_START, HandshakePacket.TAMPER_STATUS_POS).indexOf('ÿ'));
                        // if App cache has been deleted, then Recover owner's doorMode by updating the DB
                        whoLoggedIn = new Owner();
                        whoLoggedIn.setId(sharedPreferences.getMac());
                        Log.d(TAG, "Owner Id=  " + whoLoggedIn.getId());
                        int nameEndIndex = packet.substring(
                                HandshakePacket.OWNER_NAME_START, HandshakePacket.TAMPER_STATUS_POS).indexOf('ÿ');
                        if (nameEndIndex != -1) {
                            whoLoggedIn.setName(packet.substring(HandshakePacket.OWNER_NAME_START,
                                    HandshakePacket.TAMPER_STATUS_POS).substring(0, nameEndIndex));
                        }

                        whoLoggedIn.setAccessMode("owner");
                        appContext.setUser(whoLoggedIn);
                        Owner owner = Owner.getInstance(whoLoggedIn);

                        if (db.insert(owner) && db.registerDoor(owner, whichDoor)) {
                            Log.d("MainActivity", "Owner's doorMode has been recovered Successfully");
                        } else {
                            Log.d("MainActivity", "[Error]: Owner's doorMode cannot be recovered");
                        }
                    }


                    Log.d("MainActivity", "Door ID: " + whichDoor.getId());
                    int temp = packet.charAt(16);
                    Intent homeIntent = new Intent(this, HomeActivity.class);
                    homeIntent.putExtra(BATTERY_STATUS_EXTRA, batteryStatus);
                    homeIntent.putExtra(TAMPER_STATUS_EXTRA, temp);
                    startActivity(homeIntent);
                }
            } else {
                if (isGuestRegistered) {
                    Cursor res = userMode.getData(doorID);
                    if (res.getCount() <= 0) {
                        userMode.insertData(doorID, "guest");
                    } else {
                        res.moveToFirst();
                        do {
                            if (res.getString(1).equals("owner")) {
                                userMode.updateData(doorID, "guest");
                            } else
                                break;

                        } while (res.moveToNext());
                        res.close();
                    }

                    // Guest Registered
                    Log.d("MainActivity", "Guest Registered");
                    User whoLoggedIn = new Guest();
                    whoLoggedIn.setId(sharedPreferences.getMac());
                    whoLoggedIn.setAccessMode("guest");
                    appContext.setUser(whoLoggedIn);

                    Door whichDoor = new Door();
                    whichDoor.setName(deviceName);
                    whichDoor.setId(doorID);
                    appContext.setDoor(whichDoor);

                    Intent homeIntent = new Intent(this, HomeActivity.class);
                    homeIntent.putExtra(BATTERY_STATUS_EXTRA, batteryStatus);
                    startActivity(homeIntent);
                } else {
                    // Guest Not Registered
                    // Owner is Registered
                    //isGuestRegistered = isOwnerRegistered = false;
                    String contact = "+91" + packet.substring(HandshakePacket.OWNER_NAME_START, HandshakePacket.TAMPER_STATUS_POS);
                    String strDate = DateTimeFormat.getDateTime(4);
                    //String message = deviceName+" lock has been accessed by an unregistered person on "+strDate+". This is an alert from azLock.";
                    Log.d("MainActivity", "Owner Contact:" + contact + " [" + strDate + "]");

                    final int WAIT_TIME = 10000, INTERVAL = 2000;
                    final CountDownTimer countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            if (alertDialog1 != null) {
                                alertDialog1.dismiss();
                            }
                            if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
                                //disconnectRemoteDevice();
                                disconnectFromMqtt();
                            } else {
                                sessionManager.exit();
                            }
                        }
                    };

                    int isTemperEnable=packet.charAt(16);
                    Log.d(TAG,"isTemperEnable guest :"+isTemperEnable);
                    if(isTemperEnable==TEMP_FLAG_ENABLE || isTemperEnable == UNAUTHORIZED_ACCESS){
                    //Log.d(TAG,"sending sms");
                    /* Send sms is removed form this line from 19-Nov-2018
                     *  because google stope the service of SMS and Call Log in android
                     *  Message :- (phone number) This person is trying to access your door : (device name)
                     * phone no- packet.substring(6,16) ,device name-deviceName
                     *  */
                           /* SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(packet.substring(6,16), "AzLock", " This person is trying to access your door : "+deviceName, null, null);*/
                        String message = "Dear Aziczen Customer, The mobile number: "+sharedPreferences.getValues(MySharedPreferences.MOB_NO).trim()+", is trying to access your Smart Door LOCK - "+deviceName+".";
                        sendMessageToOwner(packet.substring(6,16),message);
                    }

                    countDownTimer.start();
                    alertDialog1 = new AlertDialog.Builder(mContext).setCancelable(false)
                            .setMessage("You are not registered. Please contact owner to get the access key. azLock will automatically exit after 10 seconds.")
                            .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    countDownTimer.cancel();
                                    userMode.deleteData(doorID);
                                    dialog.dismiss();
                                    if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
                                        //disconnectRemoteDevice();
                                        disconnectFromMqtt();
                                    } else {
                                        sessionManager.exit();
                                    }
                                }
                            }).create();
                    alertDialog1.show();
                }
            }
        } catch (Exception e) {
            Log.d("OwnerProfileActivity", "Unsupported String Decoding Exception");
        }
    }


    private void sendMessageToOwner(String ownerNo, String message) {
        Log.d(TAG, "sendMessageToOwner: "+message);
        Log.d(TAG, "sendMessageToOwner: "+ownerNo);
        JSONObject object = new JSONObject();
        try {
            object.put(	"owner_no",ownerNo);
            object.put(	"message",message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String url = "https://13.127.109.11/Azlock/send_msg_to_owner.php";
        VolleyRequest.jsonObjectRequest(this, url,object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                //Toast.makeText(ConnectActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
            }
        });
    }

    private void disconnectFromMqtt() {
        byte[] packet = new byte[RemoteConnectionModePacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.CONNECTION_MODE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = RemoteConnectionModePacket.SENT_PACKET_LENGTH;
        packet[RemoteConnectionModePacket.CONNECTION_MODE_POSITION] = RemoteConnectionModePacket.DISCONNECT;
        String doorId = appContext.getDoor().getId();
        if (doorId != null) {
            // convert mac in to byte
            byte[] doorMac = Utils.toByteArray(doorId);
            for (byte b : doorMac)
                Log.d(TAG, "sendConnectCommand: " + String.format("%02X", b));
            System.arraycopy(doorMac, 0, packet, RemoteConnectionModePacket.DOOR_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }
        isDisconOrUnsub = MqttInterface.UN_SUBSCRIBE;
        sendData(packet, "", "",MqttInterface.DISCONNECT_TIME, new MqttInterface() {
            @Override
            public void dataAvailable(byte[] data) {
                isDisconOrUnsub = MqttInterface.DISCONNECT_TO_MQTT;
                unSubscribe();
            }

            @Override
            public void timeOutError() {
                if (isDisconOrUnsub == MqttInterface.UN_SUBSCRIBE) {
                    isDisconOrUnsub = MqttInterface.DISCONNECT_TO_MQTT;
                    unSubscribe();
                }else {
                    disconnectMqtt();
                    sessionManager.exit();
                }
                //Toast.makeText(ConnectActivity.this, "Unable to disconnect", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void unableToSubscribe() {

            }

            @Override
            public void succOrFailToUnSubscribe() {
                disconnectMqtt();
                sessionManager.exit();
            }
        });
    }

    public void disconnectRemoteDevice() {
        new RemoteDisconnectAsyncTask(this, new OnTaskCompleted<Boolean>() {
            @Override
            public void onTaskCompleted(int resultCode, Boolean data) {
                sessionManager.exit();
            }
        }, "Disconnecting...").execute(appContext.getDoor().getId());
    }

    //private String bridgeSsid, bridgePassword;
    private boolean isConnecting;
    //private boolean isNetworkEnabled;

    /*public void connectWifi() {
       *//* progressDialog = new ProgressDialog(mContext);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);*//*

        isConnecting = true;
        Log.d(TAG, "we are here");
        final int WAIT_TIME = 20000, INTERVAL = 6000;
        countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isWifiConnected(mContext, bridgeSsid)) {
                    Log.w(TAG, "Bridge already connected");
                    dialog.dismiss();
                    if (!ConfigureBridgeActivity.isWifiConnected && isConnecting && isNetworkEnabled) {
                        isConnecting = false;
                    }
                    Log.w(TAG, "connect wifi countdown timer/Starting Bridge Config");
                    startActivity(new Intent(ConnectActivity.this, ConfigureBridgeActivity.class));
                    countDownTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
                if (!ConfigureBridgeActivity.isWifiConnected && isConnecting && isNetworkEnabled) {
                    isConnecting = false;
                }
                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
            }
        };

        final View bridgeConnectView = getLayoutInflater().inflate(R.layout.bridge_connect, nullParent, false);
        final AutoCompleteTextView bridgeSsidTextView = bridgeConnectView.findViewById(R.id.bridge_ssid_textView);
        final EditText bridgePasswordEditText = bridgeConnectView.findViewById(R.id.bridge_password_editText);

        final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        ArrayList<WifiNetwork> wifiNetworks = databaseHandler.getNetworks();
        bridgeSsidSuggestionSet = new HashSet<>(wifiNetworks);
        String[] bridgeSsidSuggestions = new String[wifiNetworks.size()];
        int i = 0;
        for (WifiNetwork network : bridgeSsidSuggestionSet) {
            bridgeSsidSuggestions[i++] = network.toString();
        }
        Log.d(TAG, "ssidSuggestionSet:" + bridgeSsidSuggestionSet);
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bridgeSsidSuggestions);
        bridgeSsidTextView.setAdapter(adapter);
        bridgeSsidTextView.setThreshold(1);
        alertDialog = new AlertDialog.Builder(mContext).setView(bridgeConnectView)
                .setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        alertDialog.show();
        Objects.requireNonNull(alertDialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bridgeSsid = bridgeSsidTextView.getText().toString();
                bridgePassword = bridgePasswordEditText.getText().toString();

                if (bridgeSsid != null && !bridgeSsid.isEmpty() && !bridgePassword.isEmpty()) {

                    registerReceiver(wifiBroadcastReceiver, mIntentFilter);
                    isReceiverRegistered = true;

                    if (!bridgeSsidSuggestionSet.contains(bridgeSsid) && databaseHandler.getNetwork(bridgeSsid) == null) {
                        Log.d(TAG, bridgeSsid + " adding to DB");
                        databaseHandler.addNetwork(new WifiNetwork(bridgeSsid));
                    }

                    alertDialog.dismiss();
                    if (isWifiConnected(mContext, bridgeSsid)) {
                        isConnecting = false;
                        Log.w(TAG, "Bridge already connected starting bridge activity");
                        startActivity(new Intent(ConnectActivity.this, ConfigureBridgeActivity.class));
                    } else {
                        Log.d(TAG, "bridge no connected");
                        dialogTextView.setText(R.string.please_wait);
                        dialog.show();
                        if (countDownTimer != null) {
                            countDownTimer.start();
                        }
                        // connect
                        WifiConfiguration wc = new WifiConfiguration();
                        wc.SSID = "\"" + bridgeSsid + "\"";
                        wc.preSharedKey = "\"" + bridgePassword + "\"";
                        wc.hiddenSSID = true;
                        wc.status = WifiConfiguration.Status.ENABLED;
                        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        int res = mWifiManager.addNetwork(wc);
                        if (res == -1) {
                            Log.d(TAG, "invalid ssid or ssid not present");
                        } else {
                            Log.d("WifiPreference", "add Network returned " + res);
                            isNetworkEnabled = mWifiManager.enableNetwork(res, true);
                        }
                    }
                } else {
                    Toast.makeText(ConnectActivity.this, "Field can't be empty", Toast.LENGTH_LONG).show();
                }
                databaseHandler.close();
            }
        });
    }*/

    private final OnBroadcastListener wifiBroadcastListener = new OnBroadcastListener() {
        @Override
        public void onReceive(int resultCode, Object result) {
            if (resultCode == OnBroadcastListener.CONNECTED_WIFI_INFO) {
                String connectedBSSID = (String) result;
                Log.w(TAG, "onUpdate/SUPPLICANT_CONNECTED:" + connectedBSSID);
            } else if (resultCode == OnBroadcastListener.ERROR_AUTHENTICATING) {
                Log.w(TAG, "onUpdate/Authentication Error");
                dialog.dismiss();
                Snackbar.make(findViewById(android.R.id.content), "Authentication Failed", Snackbar.LENGTH_INDEFINITE).show();
            } else if (resultCode == OnBroadcastListener.CONNECTIVITY_CHANGED) {
                boolean isConnected = (Boolean) result;
                Log.w(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED:" + isConnected);
                ConfigureBridgeActivity.isWifiConnected = isConnected;
                //private ProgressDialog progressDialog;
                //AccessPoint temp = new AccessPoint(mWifiInfo.getBSSID(), mWifiInfo.getSSID());

                if (isConnected) {
                    dialog.dismiss();

                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    Log.w(TAG, "OnBroadcastListener.CONNECTIVITY_CHANGED/Starting Bridge Config");
                    if (isConnecting) {
                        isConnecting = false;
                        Intent intent = new Intent(ConnectActivity.this, ConfigureBridgeActivity.class);
                        startActivity(intent);
                    }
                }
            }
        }
    };

    /*private boolean isWifiConnected(Context context, String ssid) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivity.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            Log.d(TAG, "checking ssid with connected ssidc " + ssid);
            return wifiInfo.getSSID().equals("\"" + ssid + "\"");
        }
        return false;
    }*/

    /*
    * It recive the data send from the remote mqtt device*/
    private final MqttBroadCastReciver mqttBroadCastReciver = new MqttBroadCastReciver(this, new MqttReceiveListener() {
        @Override
        public void onConnect() {
            isMqttConnected = true;
            Log.d(TAG, "onConnect: "+sharedPreferences.getMac()+"_RESPONSE");
            mqttMessageService.msubscribe(sharedPreferences.getMac()+"_RESPONSE");
            //Toast.makeText(ConnectActivity.this, "connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect() {
            isMqttConnected = false;
            isSubscribed = false;
            Log.d(TAG, "MQTT onDisconnect: cONNECT ACTVITY"+isMqttConnected);
            //sessionManager.logout();
            //Toast.makeText(ConnectActivity.this, "Disconnected from lock", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDataAvailable(byte[] data) {
            String value = Utils.getPacketData(data);
            try{
                if (value!=null && value.length()>=3 && value.charAt(RESPONSE_ACTION_STATUS_POS) != Utils.STS_FETCH)
                    countDownTimer.cancel();
                if (mqttInterface != null) {
                    //unSubscribe();

                    Log.d(TAG, "onDataAvailable: DATA GET");
                    Utils.printByteArray(data);
                    mqttInterface.dataAvailable(data);
                }
                //Toast.makeText(ConnectActivity.this, "DataAvailable", Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "Exception" );
            }
        }

        @Override
        public void onDelivery() {
            //Toast.makeText(ConnectActivity.this, "data send", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void subscribed() {
            Log.d(TAG, "data send: ");
            isSubscribed = true;
            if(mqttMessageService!=null)
            mqttMessageService.publish(mattSentData);
            //Toast.makeText(ConnectActivity.this, "subscribed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void unableToSubscribe() {
            countDownTimer.cancel();
            mqttInterface.unableToSubscribe();
            //Toast.makeText(ConnectActivity.this, "unableToSubscribe", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void unableToPublish() {
            //Toast.makeText(ConnectActivity.this, "unableToPublish", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "unableToPublish: ");
        }

        @Override
        public void succOrFailToUnSubscribe() {
            if (mqttInterface != null)
            mqttInterface.succOrFailToUnSubscribe();
        }
    });

    /*
    * On successfully bind MqttMessageService this callback is called*/
    private final ServiceConnection mqttConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MqttMessageService.LocalService localService = (MqttMessageService.LocalService) service;
            mqttMessageService = localService.getService();
            isBind = true;
            Log.d(TAG, "onServiceConnected: mqttConnection");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
            Log.d(TAG, "onServiceDisconnected: mqttConnection");
        }
    };

    /*
    * Start MqttMessageService and register broad cast receiver */
    private void bindMqttService() {
        Intent intent = new Intent(this, MqttMessageService.class);
        bindService(intent, mqttConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttBroadCastReciver, mqttIntentFilet());
    }

    private IntentFilter mqttIntentFilet() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MqttMessageService.MQTT_CONNECTED);
        intentFilter.addAction(MqttMessageService.MQTT_DISCONNECTED);
        intentFilter.addAction(MqttMessageService.MQTT_DATA_AVAILABLE);
        intentFilter.addAction(MqttMessageService.MQTT_DELEVIERY_COMPLETED);
        intentFilter.addAction(MqttMessageService.MQTT_UNABLE_TO_PUBLISH);
        intentFilter.addAction(MqttMessageService.MQTT_UNABLE_TO_SUBSCRIBE);
        intentFilter.addAction(MqttMessageService.MQTT_USUBSCRIBE);
        intentFilter.addAction(MqttMessageService.MQTT_UN_SCBSCRIBE);
        return intentFilter;
    }

    /*
    * Receive data from corresponding activity and send data to Ble device*/
    @Override
    public void sendData(byte[] data, String subscribeTopic, String publishTopic,int WAIT_TIME, MqttInterface myInterface) {
        StringBuilder sb =new StringBuilder();
        for (byte b : data)
            sb.append(String.format("%02X", b)).append(" ");
        Log.d(TAG, "sendData: " + sb);
        mqttInterface = myInterface;
        mattSentData = data;
        if (!isSubscribed && subscribeTopic!=null && subscribeTopic.equals(PublishTopic.SUBSCRIBE_TOPIC_YES)) {
            if(mqttMessageService!=null)
            mqttMessageService.subcribe();
        } else {
            Log.d(TAG, "second call: ");
            if(mqttMessageService!=null)
            mqttMessageService.publish(mattSentData);
        }
        int INTERVAL = 1000;
        countDownTimer = new CountDownTimer(WAIT_TIME, INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                mqttInterface.timeOutError();
            }
        };
        countDownTimer.start();
    }

    @Override
    public void unSubscribe() {
        if (mqttMessageService!=null)
        mqttMessageService.unSubscribe();
    }

    @Override
    public void disconnectMqtt() {
        if (mqttMessageService != null) {
            try {
                mqttMessageService.disconnect();
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttBroadCastReciver);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            unbindService(mqttConnection);
            mqttMessageService.stopSelf();
            mqttMessageService = null;
            isBind = false;
            isMqttConnected = false;
        }
    }
}

