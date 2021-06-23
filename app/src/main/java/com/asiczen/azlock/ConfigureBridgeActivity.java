package com.asiczen.azlock;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by somnath on 16-06-2017.
 */

public class ConfigureBridgeActivity extends AppCompatActivity implements Packet, OnBroadcastListener {

    private Context mContext;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private NetClientAsyncTask netClientAsyncTask;
    private static final int CONFIG_BRIDGE = 0;
    private static final int ADD_LOCK = 1;
    private static final int CHANGE_PASSWORD = 2;
    private IntentFilter mIntentFilter;
    private EditText ssidEditText, passwordEditText;
    private TextView errorTextView1;
    private static final String TAG = ConfigureBridgeActivity.class.getSimpleName();
    private AlertDialog configRouterAlertDialog;
    public static boolean isWifiConnected = true;
    private static String bridgeSsid = "";
    private TextView lock_mac;
    private TextView copy_txt;
    private final int WIFI_CONFIG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_bridge);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Remote Setup</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, this, this);

        ListView configListView =  findViewById(R.id.config_bridge_list);
        String[] configItems = getResources().getStringArray(R.array.conf_bridge_options);
        //List<String> data = Arrays.asList(getResources().getStringArray(R.array.conf_bridge_options));
        List<String> data = new ArrayList<>(Arrays.asList(configItems));


        LinearLayout error = findViewById(R.id.error_msg);
        if (bridgeSsid.contains("azBridge")) {
            data.remove("Change Password");
            error.setVisibility(View.VISIBLE);
        } else {
            data.remove("Bridge Param");
        }

        lock_mac = findViewById(R.id.lock_mac);
        copy_txt = findViewById(R.id.copy_txt);

        ArrayAdapter<String> arrayadapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        configListView.setAdapter(arrayadapter);
        // configListView.getChildAt(1).setEnabled(false);
        //arrayadapter.notifyDataSetChanged();

        //hide the copy option if no mac id is present
        if (lock_mac.getText().toString().isEmpty()) {
            copy_txt.setVisibility(View.GONE);
        } else {
            copy_txt.setVisibility(View.VISIBLE);
        }
        copy_txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = lock_mac.getText().toString();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData cData = ClipData.newPlainText("text", data);
                if (cm != null) {
                    cm.setPrimaryClip(cData);
                }
                Toast.makeText(ConfigureBridgeActivity.this, "MacId copied", Toast.LENGTH_SHORT).show();
            }
        });
        configListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d(TAG, "onItemClick: " + parent.getAdapter().getItem(position));
                switch (position) {
                    case CONFIG_BRIDGE:
                        Intent intent = new Intent(ConfigureBridgeActivity.this, ShowWifiDevicesActivity.class);
                        startActivityForResult(intent, WIFI_CONFIG);
                        break;
                    case ADD_LOCK:
                        startActivity(new Intent(ConfigureBridgeActivity.this, BridgeLockListActivity.class));
                        break;
                    case CHANGE_PASSWORD:
                        if (bridgeSsid.contains("azBridge")) {
                            bridgeParam();
                        } else {
                            changePassword();
                        }
                        break;
                    default:
                        break;

                }
            }
        });
        //sending mqtt detail to bridge
        sendBrokerDetail(Utils.brokerIp, 1883, Utils.userId, Utils.password);
    }
    private void changePassword() {
        View view1 = getLayoutInflater().inflate(R.layout.change_bridge_password, null,false);
        final EditText currentPasswordEditText = view1.findViewById(R.id.currentPasswordEditText);
        final EditText newPasswordEditText = view1.findViewById(R.id.newPasswordEditText);
        final EditText confirmPasswordEditText = view1.findViewById(R.id.confirmPasswordEditText);
        AlertDialog changePasswordAlertDialog = new AlertDialog.Builder(mContext)
                .setView(view1)
                .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String oldPassword = currentPasswordEditText.getText().toString().trim();
                        String newPassword = newPasswordEditText.getText().toString().trim();
                        String confirrmPwd = confirmPasswordEditText.getText().toString().trim();
                        boolean isEmpty = TextUtils.isEmpty(oldPassword) && TextUtils.isEmpty(newPassword) && TextUtils.isEmpty(confirrmPwd);
                        if (isEmpty) {
                            Toast.makeText(mContext, "Fill the data", Toast.LENGTH_SHORT).show();
                        } else {
                            if (newPassword.equals(confirrmPwd)) {
                                if (oldPassword.length() >= 8 && oldPassword.length() <= 15)
                                    if (newPassword.length() >= 8 && newPassword.length() <= 15)
                                        changeBridgePassword(oldPassword, newPassword);
                                    else
                                        Toast.makeText(ConfigureBridgeActivity.this, "New Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(ConfigureBridgeActivity.this, "Old Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(mContext, "Confirm Password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        changePasswordAlertDialog.show();
    }

    private void bridgeParam() {
        View bridgeParamView = getLayoutInflater().inflate(R.layout.config_router, null,false);
        ssidEditText = bridgeParamView.findViewById(R.id.ssid_editText);
        passwordEditText = bridgeParamView.findViewById(R.id.password_editText);
        TextView title = bridgeParamView.findViewById(R.id.title);
        final Button cancel = bridgeParamView.findViewById(R.id.cancel);
        final Button next = bridgeParamView.findViewById(R.id.next);
        title.setTextSize(15);
        //String sourceString = "<b>" + "For registration : setting->Bridge registration" + "</b> ";
        title.setText(R.string.changin_bridgeid/*+Html.fromHtml(sourceString)*/);
        errorTextView1 = bridgeParamView.findViewById(R.id.error_textView);
        ssidEditText.setHint("Bridge Id");
        configRouterAlertDialog = new AlertDialog.Builder(mContext)
                .setView(bridgeParamView)
                .create();
        configRouterAlertDialog.show();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configRouterAlertDialog.dismiss();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bridgeId = ssidEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                boolean isContain = TextUtils.isEmpty(bridgeId) && TextUtils.isEmpty(password);
                if (!isContain) {
                    if (bridgeId.length() >= 8 && bridgeId.length() <= 15)
                        if (password.length() >= 8 && password.length() <= 15) {
                            configRouterAlertDialog.dismiss();
                            sendData(bridgeId, password);
                        }else
                            Toast.makeText(ConfigureBridgeActivity.this, "Password should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(ConfigureBridgeActivity.this, "Bridge Id should be in between 8 to 15", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ConfigureBridgeActivity.this, "Fill the data", Toast.LENGTH_SHORT).show();
            }
        });
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

        netClientAsyncTask = new NetClientAsyncTask(false, ConfigureBridgeActivity.this, Utils.host,
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

        netClientAsyncTask = new NetClientAsyncTask(false, ConfigureBridgeActivity.this, Utils.host,
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


        netClientAsyncTask = new NetClientAsyncTask(false, ConfigureBridgeActivity.this, Utils.host,
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

    private void doConfigRouter(String ssid, String password) {
        try {
            Utils u = new Utils();
            u.requestType = Utils.ROUTER_CONFIG_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[RouterConfigPacket.SENT_PACKET_LENGTH];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.ROUTER_CONFIG_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = RouterConfigPacket.SENT_PACKET_LENGTH;

            for (int i = 0; i < ssid.length(); i++)
                packet[i + RouterConfigPacket.ROUTER_SSID_START] = (byte) ssid.charAt(i);

            for (int i = 0; i < password.length(); i++) {
                packet[i + RouterConfigPacket.ROUTER_PASSOWRD_START] = (byte) password.charAt(i);
            }
            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            u.setUtilsInfo(u);
            netClientAsyncTask = new NetClientAsyncTask(false, ConfigureBridgeActivity.this, Utils.host,
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
            errorTextView1.setVisibility(View.VISIBLE);
            errorTextView1.setText(R.string.port_wrong);
        }

    }

    private void checkConfigPacketStatus(int resultCode, String value) {
        if (resultCode == Activity.RESULT_OK) {
            if (value != null) {
                Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                processConfigPacket(value);
            }
        } else {
            if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                //Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                Toast.makeText(this, "Not connected to desired wifi", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                finish();
            } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
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
                if ((status == Utils.ROUTER_CONFIG_REQUEST || status == Utils.BROCKER_REQUEST || status == Utils.BRIDGE_PARAM_REQUEST || status == Utils.BRIDGE_PASSWORD_CHANGE_REQ)
                        && Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        if (configRouterAlertDialog != null)
                            configRouterAlertDialog.dismiss();
                        if (status == Utils.BROCKER_REQUEST){
                            int lockMacIdStart = LockDetail.LOCK_MAC_ID_START;
                            String lockMacId = Utils.getStringFromHex(packet.substring(lockMacIdStart,
                                    lockMacIdStart + LockDetail.LOCK_MAC_ID_SIZE));
                            lockMacId = Utils.generateMac(lockMacId); // add ":" in mac id
                            lock_mac.setText(lockMacId);
                            copy_txt.setVisibility(View.VISIBLE);
                        }
                        if (status == Utils.ROUTER_CONFIG_REQUEST){
                            bridgeParam();
                        }
                        Toast.makeText(mContext, "Configuration saved", Toast.LENGTH_LONG).show();
                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Toast.makeText(mContext, "Failed to save configuration", Toast.LENGTH_LONG).show();
                        Snackbar.make(findViewById(android.R.id.content), CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Invalid Packet");
            Snackbar.make(findViewById(android.R.id.content), "Invalid Packet", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(int resultCode, Object result) {
        switch (resultCode) {
            case OnBroadcastListener.CONNECTIVITY_CHANGED:
                isWifiConnected = (Boolean) result;
                Log.d(TAG, "isWifiConnected:" + isWifiConnected);
                if (!isWifiConnected) {
                    Toast.makeText(mContext, "Connection lost", Toast.LENGTH_LONG).show();
                    if (configRouterAlertDialog != null) {
                        configRouterAlertDialog.dismiss();
                    }
                    finish();
                }
                break;

            case OnBroadcastListener.CONNECTED_WIFI_INFO:
                String connectedBSSID = (String) result;
                Log.d(TAG, "connectedBSSID:" + connectedBSSID);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed: ");
    }

    /*public static void setBridgeSsid(String bridgeSSid) {
        bridgeSsid = bridgeSSid;
    }*/
}
