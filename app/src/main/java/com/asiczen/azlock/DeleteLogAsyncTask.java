package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by user on 3/14/2016.
 */
public class DeleteLogAsyncTask extends AsyncTask<GuestLogActivity.GuestLog, Integer, Void> implements Packet {
    @SuppressLint("StaticFieldLeak")
    private final Activity activity;
    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final AppContext appContext;
    private final ProgressDialog progressDialog;
    private final String TAG = DeleteLogAsyncTask.class.getSimpleName();
    private final ArrayList<GuestLogActivity.GuestLog> guestLogArrayList;
    private final OnUpdateListener onUpdateListener;
    private long startTime;
    private final OnDataSendListener mOnDataSendListener;

    private static Door door;
    private GuestLogActivity.GuestLog thisLog;
    private static int logPosition = 0;
    private final boolean isDeleteAll;
    private int progressBarStatus;

    public DeleteLogAsyncTask(Activity activity, boolean isDeleteAll, OnUpdateListener onUpdateListener){
        this.activity = activity;
        this.context = activity;
        appContext=AppContext.getContext();
        this.onUpdateListener = onUpdateListener;
        this.mOnDataSendListener = appContext.getOnDataSendListener();
        progressDialog = new ProgressDialog(context);
        guestLogArrayList  = new ArrayList<>();
        this.isDeleteAll = isDeleteAll;
        progressBarStatus = 0;
    }
    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setProgressNumberFormat(null);
        this.progressDialog.setMessage("Deleting Logs");
        this.progressDialog.show();
    }
    @Override
    protected Void doInBackground(GuestLogActivity.GuestLog... guestLogs) {
        Log.d(TAG, "doInBackground");
        guestLogArrayList.addAll(Arrays.asList(guestLogs));
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //if (GuestLogActivity.isSelectAll) {
                if (isDeleteAll) {

                    doAllLogDelete();
                } else {

                    logPosition = 0;
                    GuestLogActivity.logCounter = 0;
                    startTime = System.currentTimeMillis();
                    doLogDelete(guestLogArrayList, logPosition);
                }
            }
        });
        return null;
    }
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.d(TAG, "onProgressUpdate(): " + values[0]);
        progressDialog.setProgress(values[0]);
        if(values[0] == 100){
            this.progressDialog.setMessage("Deletion Completed.");
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                cancel(true);
            }
        }
        else {

            /*if(GuestLogActivity.logCounter == 1){
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            }*/

            //remainingTime = elapsedTime * (GuestListActivity.selectedGuestsSize - GuestListActivity.guestCounter);
            this.progressDialog.setMessage(GuestLogActivity.logCounter + "/"
                    +GuestLogActivity.selectedLogSize+" Log deleted");

/*Log.d(TAG, "Remaining Time:" + remainingTime);
            if(remainingTime < 60) {
                this.progressDialog.setMessage(GuestListActivity.guestCounter + "/"
                        +GuestListActivity.selectedGuestsSize+" guest deleted (" +remainingTime+" seconds left)");
            } else {
                this.progressDialog.setMessage(GuestListActivity.guestCounter + " out of "
                        +GuestListActivity.selectedGuestsSize+" guest deleted (" +(remainingTime / 60)+" minutes left)");
            }*/        }
    }
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        Runtime.getRuntime().gc();
        Log.d(TAG, "onPostExecute");

    }

    private void doAllLogDelete()
    {
        door = new Door();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            try {
                Utils u = new Utils();
                u.requestType = Utils.KEY_REQ;
                u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
                u.requestDirection = Utils.TCP_SEND_PACKET;

                byte[] packet = new byte[DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_ALL_GUEST];
                packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
                packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_LENGTH_POS] = DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_ALL_GUEST;
                //String doorID = MainActivity.whichDoor.getId();
                door.setId(appContext.getDoor().getId());
                door.setName(appContext.getDoor().getName());//db.getDoor(doorID).getName());

                //packet[DeleteGuestPacket.CHECKSUM_DELETE_ALL_SENT] = u.calculateChecksum(packet, true);

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
                Log.d(TAG, "Sent Packet:" + u.commandDetails);
                u.setUtilsInfo(u);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    private void doLogDelete(ArrayList<GuestLogActivity.GuestLog> selectedLogs, int position)
    {

        if(selectedLogs.size()== position){
            Log.d(TAG, "Log Deletion Complete:" + position);
            //GuestListActivity.progressDialog1.dismiss();
            publishProgress(100);
            return;
        }
        door = new Door();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            try {
                Utils u = new Utils();
                u.requestType = Utils.LOG_REQUEST;
                u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
                u.requestDirection = Utils.TCP_SEND_PACKET;

                byte[] packet = new byte[MAX_PKT_SIZE];
                packet[REQUEST_PACKET_TYPE_POS] = Utils.LOG_REQUEST;
                packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_LENGTH_POS] = LogDeletePacket.SENT_PACKET_LENGTH;
                //String doorID = MainActivity.whichDoor.getId();
                door.setId(appContext.getDoor().getId());
                door.setName(appContext.getDoor().getName());//db.getDoor(doorID).getName());
                packet[LogDeletePacket.DELETE_ALL_FLAG_POS] = (byte) (isDeleteAll ? DeleteGuestPacket.DELETE_ALL
                        : DeleteGuestPacket.DELETE_SELECTED);

                if (isDeleteAll) {
                    Log.d(TAG, "Delete All:" + DeleteGuestPacket.DELETE_ALL);
                } else {
                    Log.d(TAG, "Selected Log:" + selectedLogs.get(position));
                    byte[] guestMac = u.getMacIdInHex(selectedLogs.get(position).getGuestId());
                    System.arraycopy(guestMac, 0, packet, LogDeletePacket.GUEST_MAC_START, guestMac.length);
                    thisLog = selectedLogs.get(position);
                    Log.d(TAG, "Sent Log:" + selectedLogs.get(position).getGuestName());
                    int[] dateTime = DateTimeFormat.splitDateTime(thisLog.getAccessDateTime());
                    for(int k = 0; k < dateTime.length; k++) {
                        packet[LogDeletePacket.ACCESS_DATE_START + k] = (byte) dateTime[k];
                    }
                    String accessStatus = thisLog.getAccessStatus();
                    packet[LogDeletePacket.ACCESS_STATUS_POS] = (byte) (accessStatus.equals("LOCKED") ? Utils.LOCKED
                            : (accessStatus.equals("UNLOCKED") ? Utils.UNLOCKED : 'X'));

                }
                //packet[LogDeletePacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
                Log.d(TAG, "Sent Packet:" + u.commandDetails);
                u.setUtilsInfo(u);

                if(appContext.getConnectionMode()== ConnectionMode.CONNECTION_MODE_BLE) {
                    mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                        @Override
                        public void onDataAvailable(String data) {
                            processPacket(data, thisLog);
                        }
                    },null);
                }
                else if(appContext.getConnectionMode()==ConnectionMode.CONNECTION_MODE_REMOTE) {
                    /*try {
                        new NetClientAsyncTask(activity, RouterConfigActivity.getDeviceIpAddress(),
                                RouterConfigActivity.getPortNumber(), new String(packet, "ISO-8859-1"),
                                new OnTaskCompleted<String>() {
                                    @Override
                                    public void onTaskCompleted(int resultCode, String value) {
                                        Log.d(TAG, "onTaskCompleted:"+value);
                                        try {
                                            Utils.printByteArray(value.getBytes("ISO-8859-1"));
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        if(resultCode== Activity.RESULT_OK) {
                                            onDataAvailable(value);
                                        }
                                        else
                                        {
                                            if (progressDialog != null && progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                                cancel(true);
                                            }
                                            if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
                                            {
                                                Snackbar.make(activity.findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                                            }
                                            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                                            {
                                                Snackbar.make(activity.findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                }, null).execute();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }*/
                }
            } catch (IndexOutOfBoundsException e) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    cancel(true);
                }
                e.printStackTrace();
            }
        }
    }

    private void processPacket(String packet, GuestLogActivity.GuestLog guestLog)
    {
        DatabaseHandler databaseHandler = new DatabaseHandler(context);
        Log.d(TAG, "processPacket/Received Packet:" + packet);
        if (packet != null && packet.length() >= Packet.DeleteGuestPacket.RECEIVED_PACKET_LENGTH_DELETE_GUEST
                && packet.charAt(LogDeletePacket.PACKET_ID_POS)==LogDeletePacket.PACKET_ID){
            //strBytes = packet.getBytes(StandardCharsets.ISO_8859_1);

            if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.LOG_REQUEST &&
                    packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                    //new DatabaseHandler(context).delete((Guest) guest, door);
                    //if(!GuestLogActivity.isSelectAll) {
                    Log.d(TAG, "Log Deleted [SUCCESS]");
                    if(!isDeleteAll) {
                        databaseHandler.delete(guestLog);

                        //guests.remove(guest);
                        GuestLogActivity.logCounter++;
                        logPosition++;
                        progressBarStatus = ((100/guestLogArrayList.size()) * GuestLogActivity.logCounter);
Log.d(TAG, "Log Deletion/Setting progress to " + progressBarStatus);
                            /*GuestListActivity.progressDialog1.setMessage(GuestListActivity.guestCounter + " guest deleted");
                            GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);*/
                        publishProgress(progressBarStatus);
                        if(GuestLogActivity.selectedLogSize != logPosition) {
                            doLogDelete(guestLogArrayList, logPosition);
                        } else {
                            Log.d(TAG, "Log Deletion Complete:" + logPosition);
                            //GuestListActivity.progressDialog1.dismiss();
                            publishProgress(100);
                            logPosition = 0;
                            GuestLogActivity.logCounter = 0;
                            onUpdateListener.onUpdate(OnUpdateListener.LOG_UPDATED, null);
                        }
                    } else {
                        for(GuestLogActivity.GuestLog obj : guestLogArrayList)
                        {
                            databaseHandler.delete(obj);
                            Log.d(TAG, "Deleted:" + obj.getGuestName());

                            GuestLogActivity.logCounter++;
                            progressBarStatus = ((100 / GuestLogActivity.selectedLogSize) *
                                    GuestLogActivity.logCounter);
                            //GuestListActivity.progressDialog1.setMessage(GuestListActivity.guestCounter + " guest deleted");
                            //GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);
                            publishProgress(progressBarStatus);
                        }
                        //GuestListActivity.progressDialog1.dismiss();
                        publishProgress(100);
                        logPosition = 0;
                        GuestLogActivity.logCounter = 0;
                        guestLogArrayList.clear();
                        onUpdateListener.onUpdate(OnUpdateListener.LOG_UPDATED, null);
                    }
                    //notifyDataSetChanged();
                    //onUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED);
                    Log.d(TAG, "Successfully Deleted");
                    //mOnDeleteListener.onDelete();
                } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                    Log.d(TAG, "Deletion Failed");
                    GuestLogActivity.logCounter++;
                    logPosition++;
                    progressBarStatus = ((100/GuestLogActivity.selectedLogSize) *
                            GuestLogActivity.logCounter);

                    //GuestListActivity.progressDialog1.setMessage("Guest deletion failed");
                    //GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);
                    publishProgress(progressBarStatus);
                    if(GuestLogActivity.selectedLogSize != logPosition) {
                        doLogDelete(GuestLogActivity.selectedLogs, logPosition);
                    } else {
                        Log.d(TAG, "Guest Deletion Complete:" + logPosition);
                        //GuestListActivity.progressDialog1.dismiss();
                        publishProgress(100);
                        logPosition = 0;
                        GuestLogActivity.logCounter = 0;
                    }
                }
                // Update the progress bar
                    /*GuestListActivity.progressBarHandler.post(new Runnable() {
                        public void run() {
                            Log.d("CustomAdapter", "GuestDeletion/Setting progress to "+GuestListActivity.progressBarStatus);
                            GuestListActivity.progressDialog1.setMessage(progressbarMsg);
                            GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);
                        }
                    });*/

            }
            else{
                //GuestListActivity.progressDialog1.dismiss();
                publishProgress(100);
                Log.d("DeleteLogAsyncTask", "Error:"+ Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                new AlertDialog.Builder(context).setMessage(CommunicationError.getMessage(
                        packet.charAt(RESPONSE_COMMAND_STATUS_POS)))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }


        }
        databaseHandler.close();
    }

}
