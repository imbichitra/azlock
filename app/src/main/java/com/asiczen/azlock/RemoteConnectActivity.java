package com.asiczen.azlock;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.asiczen.azlock.content.MySharedPreferences;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.net.MqttDataSendListener;
import com.asiczen.azlock.net.MqttInterface;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Created by user on 10/5/2015.
 */
public class RemoteConnectActivity extends AppCompatActivity implements Packet {

    private final String TAG = RemoteConnectActivity.class.getSimpleName();
    private ArrayList<Door> routerConfiguredDoors;
    private ArrayList<String> doorNames;
    private Context mContext;
    private AppContext appContext;
    private ListView confoguredDoorListView;
    private String doorId, doorName;
    private SessionManager sessionManager;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private IntentFilter intentFilter;
    private PopulatelistAdapter populatelistAdapter;


    private SharedPreferences sharedpreferences;
    private MqttDataSendListener mqttDataSendListener;
    private  AlertDialog dialog;
    private MySharedPreferences mySharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_device_list);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            //actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Lacerta</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedpreferences = getSharedPreferences(Utils.BRIDGE_FILE, Context.MODE_PRIVATE);
        mySharedPreferences = new MySharedPreferences(this);
        mContext = this;
        appContext = AppContext.getContext();
        mqttDataSendListener = appContext.getMqttSendListener();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        sessionManager = new SessionManager(this, new SessionManager.RemoteDisconnect() {
            @Override
            public void disconnect() {
                Log.d(TAG, "RemoteDisconnect/disconnect [doorId:" + appContext.getDoor().getId()
                        + " Connected:" + appContext.isConnected() + "]");
                if (appContext.isConnected()) {
                    sendDisconnectCommand(appContext.getDoor().getId());
                }
            }
        });
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(RemoteConnectActivity.this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        routerConfiguredDoors = new ArrayList<>();
        doorNames = new ArrayList<>();

        /*routerConfiguredDoors = (ArrayList<Door>) getIntent().getSerializableExtra("availableDoors");
        doorNames = new ArrayList<>();
        for(Door d : routerConfiguredDoors){
            doorNames.add(d.getName()); Log.d(TAG,"doorId:"+d.getId());
        }*/
        UserMode userMode = new UserMode(this);
        confoguredDoorListView =  findViewById(R.id.configured_remote_devices_listView);
        confoguredDoorListView.setEmptyView(findViewById(R.id.empty));
        //configuredDoorAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, android.R.id.text1, doorNames);
        populatelistAdapter = new PopulatelistAdapter(mContext, R.layout.remote_device_element, routerConfiguredDoors, userMode);
        // confoguredDoorListView.setAdapter(populatelistAdapter);

        confoguredDoorListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doorId = routerConfiguredDoors.get(position).getId();
                doorName = routerConfiguredDoors.get(position).getName();
                //RouterInfo routerInfo = new DatabaseHandler(mContext).getRouterInfo(doorId);
                //appContext.setRouterInfo(routerInfo);
                //Log.i(TAG, "Router Info:\n"+routerInfo.toString()+"Door Id:"+doorId);
                sendConnectCommand(doorId);
            }
        });
        fetchAvailableDevices(ScanLockPacket.SCAN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() Called");
        registerReceiver(logoutBroadcastReceiver, intentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_menu, menu);
        MenuItem item = menu.findItem(R.id.download);
        invalidateOptionsMenu();
        item.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.Refresh) {
            routerConfiguredDoors.clear();
            populatelistAdapter.notifyDataSetChanged();
            fetchAvailableDevices(ScanLockPacket.REFRESH);
        }
        return true;
    }

    private void fetchAvailableDevices(char scan) {
        final byte[] packet = new byte[ScanLockPacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.SCAN_LOCK_REQUEST;
        packet[REQUEST_PACKET_LENGTH_POS1] = ScanLockPacket.LENGTH;
        packet[ScanLockPacket.SCAN_POSITION1] = (byte) scan;

        if (!sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
            BridgeDetail bridgeDetail = appContext.getBridgeDetail();
            if (bridgeDetail != null) {
                String bridgId = bridgeDetail.getBridgeId();
                String bridgePassword = bridgeDetail.getPassword();

                Utils.setPublishTopic(bridgId);
                Utils.setSubscribeTopic(bridgId);

                Log.d(TAG, "fetchAvailableDevices: "+bridgId+" pp :"+bridgePassword);
                try {
                    for (int i = 0; i < bridgId.length(); i++)
                        packet[i + ScanLockPacket.BRIDGE_ID_START_POSITION] = (byte) bridgId.charAt(i);
                    for (int i = 0; i < bridgePassword.length(); i++)
                        packet[i + ScanLockPacket.BRIDGE_PASSWORD_START_POSITION] = (byte) bridgePassword.charAt(i);

                    //for adding emi as key to mqtt
                    String mac = mySharedPreferences.getMac();
                    Log.d(TAG, "fetchAvailableDevices: "+mac.length());
                    for (int i=0;i<mac.length();i++) {
                        packet[i + ScanLockPacket.BRIDGE_KEY_START_POSITION] = (byte) mac.charAt(i);
                        Log.d(TAG, "fetchAvailableDevices: "+i+ScanLockPacket.BRIDGE_KEY_START_POSITION);
                    }
                    showProgressDilaog("Scanning...");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "fetchAvailableDevices: ");
                    Toast.makeText(this, "Bridge is not set", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(this, "Bridge is not set", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // compute checksum of the packet
        Log.d(TAG, "Scan lock request");
        Utils.printByteArray(packet);
        if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
            netClientAsyncTask = new NetClientAsyncTask(false, RemoteConnectActivity.this, appContext.getRouterInfo().getAddress(),
                    appContext.getRouterInfo().getPort(), packet,
                    new OnTaskCompleted<String>() {
                        @Override
                        public void onTaskCompleted(int resultCode, String value) {
                            Log.d(TAG, "onTaskCompleted:" + value + "resutCode: " + resultCode + " ERROR_CODE: " + NetClientAsyncTask.ERROR_CODE);
                            if (resultCode == Activity.RESULT_OK) {
                                if (value != null) {
                                    Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                                    processAvailableDevicesPacket(value);
                                }
                            } else {
                                if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                                    Toast.makeText(RemoteConnectActivity.this, "Unable to connect", Toast.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                                    Toast.makeText(RemoteConnectActivity.this, "Timeout occurred", Toast.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_CONNECT) {
                                    Toast.makeText(RemoteConnectActivity.this, "Unable to connect", Toast.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_DISCONNECT) {
                                    Toast.makeText(RemoteConnectActivity.this, "Unable to disconnect", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
            netClientAsyncTask.showProgressDialog(true, "Scanning...");
            netClientAsyncTask.execute();
        } else {
            mqttDataSendListener.sendData(packet, PublishTopic.SUBSCRIBE_TOPIC_YES, "",MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                @Override
                public void dataAvailable(byte[] data) {
                    dismissDialog();
                    //dialog.dismiss();
                    for (byte b : data)
                        Log.d(TAG, "scan packet : "+String.format("%02x",b));
                    String packet = Utils.getPacketData(data);
                    processAvailableDevicesPacket(packet);
                    //mqttDataSendListener.unSubscribe();
                }

                @Override
                public void timeOutError() {
                    dismissDialog();
                    //mqttDataSendListener.unSubscribe();
                    Toast.makeText(RemoteConnectActivity.this, "Connection timeout", Toast.LENGTH_SHORT).show();
                    //mqttDataSendListener.unSubscribe(Packet.SubscribeTopic.SUBSCRIBE_TOPIC);
                }

                @Override
                public void unableToSubscribe() {
                    dismissDialog();
                }

                @Override
                public void succOrFailToUnSubscribe() {

                }
            });
            //show dialog
        }
    }

    @SuppressLint("HardwareIds")
    private void processAvailableDevicesPacket(String packet) {
        Log.d(TAG, "Received Packet " + packet + "Packet Length" + packet.length());
        if (packet.length()>3 && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= ScanLockPacket.RECEIVED_PACKET_LENGTH_MIN) {
            int len = Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS);
            Log.d(TAG, "Length:" + len);
            try {
                if ((packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.SCAN_LOCK_REQUEST)
                        && packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        //boolean insertRouterInfo = new DatabaseHandler(mContext).insertRouterInfo(
                        //appContext.getDoor().getId(), ip, port);
                        //Log.d(TAG, "Inserting Router Info:" + insertRouterInfo);
                        //int numberOfAvailableDevices = Character.getNumericValue(packet.charAt(ScanLockPacket.NUMBER_OF_AVAILABLE_DEVICES_POSITION));
                        int numberOfAvailableDevices = packet.charAt(ScanLockPacket.NUMBER_OF_AVAILABLE_DEVICES_POSITION);
                        Log.d(TAG, "numberOfAvailableDevices:" + numberOfAvailableDevices);
                        if (numberOfAvailableDevices > 0) {
                            int doorIdIndex = ScanLockPacket.DEVICE_1_MAC_START;
                            int doorNameIndex = ScanLockPacket.DOOR_1_NAME_START;
                            routerConfiguredDoors.clear();
                            doorNames.clear();
                            for (int i = 0; i < numberOfAvailableDevices;
                                 i++, doorIdIndex += ScanLockPacket.ONE_DEVICE_DETAILS_LENGTH,
                                         doorNameIndex = doorIdIndex + ScanLockPacket.DOOR_MAC_LENGTH) {
                                //for static bridge getStringFromHex is used
                                //String doorId = Utils.getStringFromHex(packet.substring(doorIdIndex, doorNameIndex));
                                //for dynamic bridge getMacId is used
                                String doorId = getMacId(packet.substring(doorIdIndex, doorNameIndex));
                                String doorName = packet.substring(doorNameIndex, doorIdIndex + ScanLockPacket.ONE_DEVICE_DETAILS_LENGTH);
                                routerConfiguredDoors.add(new Door(doorId, doorName));
                                doorNames.add(doorName);
                            }
                            confoguredDoorListView.setAdapter(populatelistAdapter);
                            populatelistAdapter.notifyDataSetChanged();
                        }
                        else {
                            Toast.makeText(mContext, "No lock found", Toast.LENGTH_LONG).show();
                        }

                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        String msg = CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS ));
                        new AlertDialog.Builder(mContext).setMessage(msg)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        //finish();
                                    }
                                }).create().show();
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content),CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS )), Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Invalid Packet");
            //Snackbar.make(findViewById(android.R.id.content),"Invalid Packet", Snackbar.LENGTH_LONG).show();
        }
    }

    private String getMacId(String in){
        byte [] strBytes;
        StringBuilder sb = new StringBuilder();
        strBytes = in.getBytes(StandardCharsets.ISO_8859_1);
        for (byte strByte : strBytes) {
            Log.d(TAG, "getMacId: " + String.format("%02x", strByte));
            sb.append(String.format("%02x", strByte));
        }

        return sb.toString();
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                //finish();
                break;
            default:
                break;
        }
        return true;
    }*/

    @Override
    public void onDestroy() {
        if(logoutBroadcastReceiver!=null){
            unregisterReceiver(logoutBroadcastReceiver);
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        Runtime.getRuntime().gc();
    }

    private NetClientAsyncTask netClientAsyncTask;

    private void sendConnectCommand(final String doorId)
    {
        byte[] packet = new byte[RemoteConnectionModePacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.CONNECTION_MODE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = RemoteConnectionModePacket.SENT_PACKET_LENGTH;
        packet[RemoteConnectionModePacket.CONNECTION_MODE_POSITION] = RemoteConnectionModePacket.CONNECT;
        if(doorId != null) {
            // convert mac in to byte
            byte[] doorMac = Utils.toByteArray(doorId);
            System.arraycopy(doorMac, 0, packet, RemoteConnectionModePacket.DOOR_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }

        // todo - encrypt the packet before sending
        // compute checksum of the packet
        Log.d(TAG,"Sending connect command");
        Utils.printByteArray(packet);
        if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
            netClientAsyncTask = new NetClientAsyncTask(false, RemoteConnectActivity.this, appContext.getRouterInfo().getAddress(),
                    appContext.getRouterInfo().getPort(), packet,
                    new OnTaskCompleted<String>() {
                        @Override
                        public void onTaskCompleted(int resultCode, String value) {
                            Log.d(TAG, "sendConnectCommand/onTaskCompleted:");
                            //Utils.printByteArray(Utils.toPrimitive(value));
                            Log.d(TAG, "resutCode: " + resultCode + " ERROR_CODE: " + NetClientAsyncTask.ERROR_CODE);
                            if (resultCode == Activity.RESULT_OK) {
                                if (value != null) {
                                /*try {
                                    //Utils.printByteArray(value.getBytes("ISO-8859-1"));
                                    Utils.printByteArray(Utils.toPrimitive(value));
                                    processConnectionModePacket(new String(Utils.toPrimitive(value), "ISO-8859-1"), doorId);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }*/
                                    processConnectionModePacket(value, doorId);
                                } else {
                                    Snackbar.make(findViewById(android.R.id.content), "No Response", Snackbar.LENGTH_LONG).show();
                                }
                            } else {
                                if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                                    Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                                    Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_CONNECT) {
                                    Snackbar.make(findViewById(android.R.id.content), "Unable to connect", Snackbar.LENGTH_LONG).show();
                                } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_DISCONNECT) {
                                    Snackbar.make(findViewById(android.R.id.content), "Unable to disconnect", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
            netClientAsyncTask.showProgressDialog(true, "Connecting...");
            netClientAsyncTask.execute();
        }else {
            showProgressDilaog("Connecting...");
            mqttDataSendListener.sendData(packet, PublishTopic.SUBSCRIBE_TOPIC_NO, PublishTopic.PUBLISH_TOPIC,MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                @Override
                public void dataAvailable(byte[] data) {
                    for (byte b : data)
                        Log.d(TAG, "connection 1: "+String.format("%02X",b));
                    String packet = Utils.getPacketData(data);
                    processConnectionModePacket(packet, doorId);
                    //mqttDataSendListener.unSubscribe();
                }

                @Override
                public void timeOutError() {
                    dismissDialog();
                    Toast.makeText(RemoteConnectActivity.this, "Connection Timeout", Toast.LENGTH_SHORT).show();
                    //mqttDataSendListener.unSubscribe();
                }

                @Override
                public void unableToSubscribe() {
                    dismissDialog();
                }

                @Override
                public void succOrFailToUnSubscribe() {

                }
            });
        }
        /*mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
            @Override
            public void onDataAvailable(String data) {
                processConnectionModePacket(data, doorId);
            }
        });
        connectPacket=1;*/
    }

    private void processConnectionModePacket(String str, String doorId) {
        boolean isError = true;
        String errorMessage;
        Log.d(TAG, "Processing ConnectionMode Packet:" + str);
        // check checksum
        if (str != null && str.length() >= RemoteConnectionModePacket.RECEIVED_PACKET_LENGTH) {
            try {
                if (str.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.CONNECTION_MODE_REQ &&
                        Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                    if (str.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        isError = false;
                        Log.d(TAG, "Remote Connection Established");
                        appContext.setConnectionStatus(true);
                        appContext.setDoor(new Door(doorId, doorName));
                        //getAsyncTaskStatus();
                        Log.d(TAG, "processConnectionModePacket: ");
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Sending Handshake Packet");
                                updateConnectionStatus();
                            }
                        }, 1000);
                    } else if (str.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        appContext.setConnectionStatus(false);
                        Log.e(TAG, "Remote Connection Failed");
                        Toast.makeText(mContext, "Connection Failed", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Log.e(TAG, "Error (connection_mode):"+Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                        if (Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) != Utils.CMD_OK) {
                            errorMessage = CommunicationError.getMessage(Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                            Log.d(TAG, "processConnectionModePacket: "+errorMessage);
                        }
                    }
                }
                if (isError) {
                    //new AlertDialog.Builder(mContext).setIcon(R.drawable.ic_error).setTitle("Error")
                    //        .setMessage(errorMessage).create().show();
                    // Toast toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG);
                    //Toast toast = Toast.makeText(mContext, "TRY Again", Toast.LENGTH_LONG);
                    Snackbar.make(findViewById(android.R.id.content), CommunicationError.getMessage(Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS)), Snackbar.LENGTH_LONG).show();
                    dismissDialog();
                    //toast.show();
                        /*final String door=doorId;
                        if (Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) == CommunicationError.BLE_ALREADY_CONNECTED) {
                            isAlreadyConnected=true;
                            Log.d(TAG, "Already Connected");
                            appContext.setConnectionStatus(true);
                            appContext.setDoor(new Door(doorId, doorName));
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i(TAG, "Sending Handshake Packet");
                                    updateConnectionStatus();
                                }
                            }, 50);

                        }*/
                    }
                    //finish();
            } catch(Exception e) {
                Log.d(TAG, "Unsupported String Decoding Exception");
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Connection Failed", Toast.LENGTH_LONG);
            toast.show();
            Log.d(TAG, "Packet Received"+str);
        }
    }

    private static boolean isAlreadyConnected = false;
    private void sendDisconnectCommand(final String doorId)
    {
        final byte[] packet = new byte[RemoteConnectionModePacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.CONNECTION_MODE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = RemoteConnectionModePacket.SENT_PACKET_LENGTH;
        packet[RemoteConnectionModePacket.CONNECTION_MODE_POSITION] = RemoteConnectionModePacket.DISCONNECT;
        if(doorId != null) {
            // convert mac in to byte
            Log.e(TAG,"MAC:"+doorId);
            byte[] doorMac = Utils.toByteArray(doorId);
            System.arraycopy(doorMac, 0, packet, RemoteConnectionModePacket.DOOR_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }

        //packet[RemoteConnectionModePacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
        Utils.printByteArray(packet);
        netClientAsyncTask = new NetClientAsyncTask(false, this, appContext.getRouterInfo().getAddress(),
                appContext.getRouterInfo().getPort(), packet,
                new OnTaskCompleted<String>() {
                    @Override
                    public void onTaskCompleted(int resultCode, String value) {
                        Log.d(TAG, "onTaskCompleted:"+value);
                        Log.d(TAG, "resutCode: "+resultCode+" ERROR_CODE: "+NetClientAsyncTask.ERROR_CODE);
                        if(resultCode== Activity.RESULT_OK) {
                            if(value!=null) {
                                Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                                processDisconnectionModePacket(value, doorId);
                            }

                            /*try {
                                processDisconnectionModePacket(new String(Utils.toPrimitive(value), "ISO-8859-1"), doorId);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }*/
                        } else {
                            if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                                Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_CONNECT)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Unable to connect", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_DISCONNECT)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Unable to disconnect", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }
                });
        netClientAsyncTask.showProgressDialog(true, "Disconnecting...");
       // netClientAsyncTask.execute();

        netClientAsyncTask.disconnect();
        /*mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
            @Override
            public void onDataAvailable(String data) {
                processDisconnectionModePacket(data, doorId);
            }
        });
        disconnectPacket=1;*/
    }

    private void processDisconnectionModePacket(String str, String doorId) {
        boolean isError = true;
        String errorMessage = null;

        Log.d(TAG, "processDisconnectionModePacket Handshake Packet:" + str);
        // check checksum
        if (str != null && str.length() >= RemoteConnectionModePacket.RECEIVED_PACKET_LENGTH) {
            try {
                if (str.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.CONNECTION_MODE_REQ &&
                        Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                    if (str.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        isError = false;
                        Log.i(TAG, "Remote Device Disconnected");
                        if (isAlreadyConnected) {
                            appContext.setConnectionStatus(false);
                            sendConnectCommand(doorId);
                            isAlreadyConnected = false;
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Device Disconnected", Snackbar.LENGTH_LONG).show();
                        }
                    } else if (str.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Log.e(TAG, "Remote Connection Failed");
                        Toast.makeText(mContext, "Connection Failed", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (str.charAt(RESPONSE_COMMAND_STATUS_POS) != Utils.CMD_OK) {
                        errorMessage = CommunicationError.getMessage(Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                    }
                }
                if (isError) {
                    //new AlertDialog.Builder(mContext).setIcon(R.drawable.ic_error).setTitle("Error")
                    //        .setMessage(errorMessage).create().show();
                    Toast toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG);
                    toast.show();
                }
                //finish();
            } catch (Exception e) {
                Log.d("ConnectActivity", "Unsupported String Decoding Exception");
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Invalid or Null Data", Toast.LENGTH_LONG);
            toast.show();
            Log.d("ConnectActivity", "Packet Received"+str);
        }
    }

    public void disconnectRemoteDevice(){
        new RemoteDisconnectAsyncTask(this, new OnTaskCompleted<Boolean>() {
            @Override
            public void onTaskCompleted(int resultCode, Boolean data) {
                sessionManager.exit();
            }
        }, "Disconnecting...").execute(appContext.getDoor().getId());
    }

    public void getAsyncTaskStatus()
    {
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

    private static final Handler mHandler = new Handler() {
        @Override

        //Handler events that received from BLE service
        public void handleMessage(Message msg) {
            Log.d("RemoteConnectActivity", "mHandler/Message:" + msg);
        }
    };

    private void updateConnectionStatus() {
        //byte[] packet = new byte[HandshakePacket.SENT_PACKET_LENGTH];
        /*byte[] packet = new byte[MAX_PKT_SIZE];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.HANDSHAKE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_VISITOR;
        packet[REQUEST_PACKET_LENGTH_POS] = HandshakePacket.SENT_PACKET_LENGTH;
        String mac = Utils.getUserId();

        // convert mac in to byte
        Log.d(TAG, "MAC:" + mac + " IMEI:" + appContext.getImei());
        byte[] macHexBytes = Utils.toByteArray(mac);
        System.arraycopy(macHexBytes, 0, packet, 3, Utils.PHONE_MAC_ID_LEN_IN_HEX);*/

        byte[] data = new byte[MAX_PKT_SIZE];
        data[REQUEST_PACKET_TYPE_POS] = Utils.HANDSHAKE_REQ;
        data[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_VISITOR;
        data[REQUEST_PACKET_LENGTH_POS] = HandshakePacket.SENT_PACKET_LENGTH;
        String mac = mySharedPreferences.getMac();

        byte[] macHexBytes = Utils.toByteArray(mac);
        System.arraycopy(macHexBytes, 0, data, 3, Utils.PHONE_MAC_ID_LEN_IN_HEX);

        data[Utils.TIME_STAMP]=Utils.getTime()[0];
        data[Utils.TIME_STAMP+1]=Utils.getTime()[1];
        data[Utils.TIME]=Utils.getTime()[0];
        data[Utils.TIME+1]=Utils.getTime()[1];

        byte[] packet = Utils.encriptData(data);
        if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
            NetClientAsyncTask clientAsyncTask = new NetClientAsyncTask(true, RemoteConnectActivity.this, appContext.getRouterInfo().getAddress(),
                    appContext.getRouterInfo().getPort(), packet, new OnTaskCompleted<String>() {
                @Override
                public void onTaskCompleted(int resultCode, String value) {
                    Log.d(TAG, "onTaskCompleted:" + value);
                    Log.d(TAG, "resutCode: " + resultCode + " ERROR_CODE: " + NetClientAsyncTask.ERROR_CODE);
                    if (resultCode == Activity.RESULT_OK) {
                        if (value != null) {
                            Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                            processPacket(value);
                        }

                            /*try {
                                processPacket(new String(value), "ISO-8859-1"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }*/
                    } else {
                        if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                            Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                        } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                            Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                        } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_CONNECT) {
                            Snackbar.make(findViewById(android.R.id.content), "Unable to connect", Snackbar.LENGTH_LONG).show();
                        } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.UNABLE_TO_DISCONNECT) {
                            Snackbar.make(findViewById(android.R.id.content), "Unable to disconnect", Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
            });
            clientAsyncTask.showProgressDialog(true, "Authenticating...");
            clientAsyncTask.execute();
        } else {
            for (byte b : packet)
                Log.d(TAG, "dataAvailable: "+String.format("%02X",b));
            mqttDataSendListener.sendData(packet, PublishTopic.SUBSCRIBE_TOPIC_NO, PublishTopic.PUBLISH_TOPIC,MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                @Override
                public void dataAvailable(byte[] data) {
                    dismissDialog();
                    for (byte b : data)
                        Log.d(TAG, "dataAvailable 1: "+String.format("%02X",b));
                    String packet = Utils.getPacketData(data);
                    processPacket(packet);
                    //mqttDataSendListener.unSubscribe();
                }

                @Override
                public void timeOutError() {
                    dismissDialog();
                    Toast.makeText(RemoteConnectActivity.this, "Connection Timeout", Toast.LENGTH_SHORT).show();
                    //mqttDataSendListener.unSubscribe();
                }

                @Override
                public void unableToSubscribe() {
                    dismissDialog();
                }

                @Override
                public void succOrFailToUnSubscribe() {

                }
            });
        }

        /*new AlertDialog.Builder(mContext).setMessage("Send Handshake")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        /*mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                            @Override
                            public void onDataAvailable(String data) {
                                processPacket(data);
                            }
                        });*/
                    /*}
                }).setNegativeButton("Test", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getAsyncTaskStatus();
            }
        }).create().show();*/
        /*new NetClientAsyncTask(RemoteConnectActivity.this, RouterConfigActivity.getDeviceIpAddress(),
                RouterConfigActivity.getPortNumber(), packet,
                new OnTaskCompleted<String>() {
                    @Override
                    public void onTaskCompleted(int resultCode, String value) {
                        Log.d(TAG, "onTaskCompleted:"+value);
                        Log.d(TAG, "resutCode: "+resultCode+" ERROR_CODE: "+NetClientAsyncTask.ERROR_CODE);
                        if(resultCode== Activity.RESULT_OK) {
                            if(value!=null) {
                                try {
                                    Utils.printByteArray(value.getBytes("ISO-8859-1"));
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                processPacket(value);
                            }
                        }
                        else
                        {
                            if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_CONNECT)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Unable to connect", Snackbar.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_DISCONNECT)
                            {
                                Snackbar.make(findViewById(android.R.id.content), "Unable to disconnect", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }
                }, "Authenticating...").execute();*/

            /*final ProgressDialog pDialog = new ProgressDialog(this);
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setMessage("Authenticating...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.setProgressNumberFormat(null);
            pDialog.setProgressPercentFormat(null);
            pDialog.show();

            //initiate message service to send to device
            runTcpClientAsService();

            // timeout is 15 sec for handshaking between devices
            final String s = "Sending Handshake Packet";
            final String d = ".....";
            final int WAIT_TIME = 10000;
            new CountDownTimer(WAIT_TIME, 1000) {
                private Utils u = new Utils();
                private byte i = 0;
                public void onTick(long millisUntilFinished) {
                    pDialog.setMessage(s + d.substring(i++));
                    if(i >= d.length()) {
                        i = 0;
                    }
                    u = u.getUtilsInfo();
                    if (u.requestStatus == Utils.TCP_PACKET_SENT) {
                        new ConnectAsyncTask(getApplicationContext(), Utils.LockDemoUtils).execute();
                    }
                    if (u.requestStatus == Utils.TCP_PACKET_RECEIVED) {
                        processTcpPacket(u.responseDetails);
                        pDialog.dismiss();
                        cancel();
                        onBackPressed();
                    }
                }

                public void onFinish() {
                    Log.d("ConnectActivity", "CountDownTimer/onFinish called");
                    u = u.getUtilsInfo();
                    if (u.requestStatus != Utils.TCP_PACKET_RECEIVED) {
                        // timeout occurred
                        showTcpPacketError();
                        //exitMainActivity = true;
                    }
                    pDialog.dismiss();
                    //onBackPressed();
                }

            }.start();*/

    }

    private void processPacket(String str) {
        Intent returnIntent = new Intent();

        boolean isError = true;
        int result;
        String errorMessage = null;

        Log.d(TAG, "Handshake Packet:" + str);
        // check checksum
        if (str != null && Utils.parseInt(str, Utils.PACKET_LENGTH_POS) >= HandshakePacket.RECEIVED_PACKET_LENGTH) {
            try {
                if (str.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.HANDSHAKE_REQ &&
                        Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                        Log.d(TAG, "Processing TCP Packet"+" length"+str.charAt(HandshakePacket.PACKET_LENGTH_POS));

                    isError = false;
                    /* Return the Handshake Packet to MainActivity */
                    Log.d(TAG, "Returning Handshake packet to MainActivity");
                    returnIntent.putExtra("HANDSHAKE_PACKET", str);
                    returnIntent.putExtra("DOOR_ID", doorId);
                    returnIntent.putExtra("DOOR_NAME", doorName);

                    result = RESULT_OK;
                } else {
                    if (str.charAt(RESPONSE_COMMAND_STATUS_POS) != Utils.CMD_OK) {
                        errorMessage = CommunicationError.getMessage(Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                    }
                    //disconnectRemoteDevice();  //this is required for statice bridge
                    //Log.d("CMD_STS_ERR", errorMessage);
                    result = RESULT_CANCELED;
                }
                setResult(result, returnIntent);
                finish();
                if (isError) {
                    Toast toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (Exception e) {
                Log.d(TAG, "Unsupported String Decoding Exception");
            }
        } else {
            //disconnectRemoteDevice(); //this is required for statice bridge
            //Toast toast = Toast.makeText(mContext, "Invalid or Null Data", Toast.LENGTH_LONG);
            Toast toast = Toast.makeText(mContext, "Connection Lost", Toast.LENGTH_LONG);
            toast.show();
            Log.d(TAG, "Packet Received"+str);
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed called/isConnected:" + appContext.isConnected());
        mqttDataSendListener.disconnectMqtt();
    }

    private void showProgressDilaog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        TextView dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        dialogTextView.setText(message);
        dialog = builder.create();
        dialog.show();
    }

    private void dismissDialog(){
        if (dialog!= null && dialog.isShowing())
            dialog.dismiss();
    }
}
