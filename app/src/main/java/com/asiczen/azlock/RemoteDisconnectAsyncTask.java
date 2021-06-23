package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;

/**
 * Created by somnath on 29-05-2017.
 */

public class RemoteDisconnectAsyncTask extends AsyncTask<String,Void,Void> implements Packet {

    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    @SuppressLint("StaticFieldLeak")
    private final Activity activity;
    private final AppContext appContext;
    private final OnTaskCompleted<Boolean> onTaskCompleted;
    private int resultCode = Activity.RESULT_CANCELED;
    private final String TAG = RemoteDisconnectAsyncTask.class.getSimpleName();

    public RemoteDisconnectAsyncTask(Activity activity, OnTaskCompleted<Boolean> onTaskCompleted, String progessbarMsg) {
        this.onTaskCompleted = onTaskCompleted;
        this.mContext=activity;
        this.activity=activity;
        //progressDialog = new ProgressDialog(mContext);
        appContext=AppContext.getContext();
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");
        /*if(progessbarMsg!=null) {
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(progessbarMsg);
            progressDialog.show();
        }*/
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG, "TaskCompleted/Received Msg:" + result);
        /*if (progressDialog!=null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }*/
    }

    @Override
    protected Void doInBackground(String... params) {
        final String p=params[0];
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendDisconnectCommand(p);
            }
        });

        return null;
    }

    private void sendDisconnectCommand(final String doorId)
    {
        NetClientAsyncTask netClientAsyncTask;
        final byte[] packet = new byte[Packet.RemoteConnectionModePacket.SENT_PACKET_LENGTH];

        packet[REQUEST_PACKET_TYPE_POS] = Utils.CONNECTION_MODE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = Packet.RemoteConnectionModePacket.SENT_PACKET_LENGTH;
        packet[Packet.RemoteConnectionModePacket.CONNECTION_MODE_POSITION] = Packet.RemoteConnectionModePacket.DISCONNECT;
        if(doorId != null) {
            // convert mac in to byte
            Log.e(TAG,"MAC:"+doorId);
            byte[] doorMac = Utils.toByteArray(doorId);
            System.arraycopy(doorMac, 0, packet, Packet.RemoteConnectionModePacket.DOOR_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }

        // todo - encrypt the packet before sending
        // compute checksum of the packet
        //Utils u = new Utils();
        //packet[Packet.RemoteConnectionModePacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
        Utils.printByteArray(packet);
        netClientAsyncTask=new NetClientAsyncTask(false,activity, appContext.getRouterInfo().getAddress(),
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
                        }
                        else
                        {
                            if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
                            {
                                Toast.makeText(mContext,"Not Connected", Toast.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                            {
                                Toast.makeText(mContext,"Timeout occurred", Toast.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_CONNECT)
                            {
                                Toast.makeText(mContext,"Unable to connect", Toast.LENGTH_LONG).show();
                            }
                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.UNABLE_TO_DISCONNECT)
                            {
                                Toast.makeText(mContext,"Unable to disconnect", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
        netClientAsyncTask.showProgressDialog(true,"Disconnecting...");
        netClientAsyncTask.execute();
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

        // check checksum
        if (str != null && str.length() >= Packet.RemoteConnectionModePacket.RECEIVED_PACKET_LENGTH) {
            try {
                if (str.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.CONNECTION_MODE_REQ &&
                            Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                        if(str.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS)
                        {
                            isError = false;
                            Log.i(TAG, "Remote Device Disconnected");
                            resultCode=Activity.RESULT_OK;
                            Toast.makeText(mContext,"Device Disconnected",Toast.LENGTH_SHORT).show();

                            onTaskCompleted.onTaskCompleted(resultCode, true);
                        }
                        else if(str.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE)
                        {
                            Log.e(TAG, "Failed to disconnect");
                            resultCode=Activity.RESULT_CANCELED;
                            Toast.makeText(mContext,"Failed to disconnect", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (str.charAt(RESPONSE_COMMAND_STATUS_POS) != Utils.CMD_OK) {
                            Log.e(TAG, "Failed to disconnect: "+Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                            errorMessage = CommunicationError.getMessage(Utils.parseInt(str, RESPONSE_COMMAND_STATUS_POS));
                            onTaskCompleted.onTaskCompleted(resultCode, false);
                        }
                    }
                    if (isError) {
                        Toast toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG);
                        toast.show();
                    }
            } catch(Exception e) {
                Log.d("ConnectActivity", "Unsupported String Decoding Exception");
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Invalid or Null Data", Toast.LENGTH_LONG);
            toast.show();
            Log.d("ConnectActivity", "Packet Received"+str);
        }
    }
}
