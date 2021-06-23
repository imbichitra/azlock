package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by user on 11/2/2015.
 */
public class DeleteGuestsAsyncTask extends AsyncTask<Guest, Integer, Void> implements Packet {
    @SuppressLint("StaticFieldLeak")
    private final Activity activity;
    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final AppContext appContext;
    private final ProgressDialog progressDialog;
    private final String TAG = DeleteGuestsAsyncTask.class.getSimpleName();
    private final ArrayList<Guest> guestList;
    private final OnUpdateListener onUpdateListener;
    private long startTime;
    private final OnDataSendListener mOnDataSendListener;

    private static Door door;
    private Guest thisGuest;
    private static int guestPosition = 0;

    public DeleteGuestsAsyncTask(Activity activity, OnUpdateListener onUpdateListener){
        this.activity = activity;
        this.context = activity;
        appContext=AppContext.getContext();
        this.onUpdateListener = onUpdateListener;
        progressDialog = new ProgressDialog(context);
        guestList  = new ArrayList<>();
        mOnDataSendListener=appContext.getOnDataSendListener();
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
        this.progressDialog.setMessage("Deleting Guests");
        this.progressDialog.show();
    }
    @Override
    protected Void doInBackground(Guest... guests) {
        Log.d(TAG, "doInBackground");
        guestList.addAll(Arrays.asList(guests));
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (GuestListActivity.isSelectAll) {

                    doAllGuestDelete();
                } else {

                    guestPosition = 0;
                    GuestListActivity.guestCounter = 0;
                    startTime = System.currentTimeMillis();
                    doGuestDelete(guestList, 0);
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
            }
        }
        else {

            /*if(GuestListActivity.guestCounter == 1){
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            }*/

            //remainingTime = elapsedTime * (GuestListActivity.selectedGuestsSize - GuestListActivity.guestCounter);
            this.progressDialog.setMessage(GuestListActivity.guestCounter + "/"
                    + Utils.selectedGuestsSize+" guest deleted");
            /*Log.d(TAG, "Remaining Time:" + remainingTime);
            if(remainingTime < 60) {
                this.progressDialog.setMessage(GuestListActivity.guestCounter + "/"
                        +GuestListActivity.selectedGuestsSize+" guest deleted (" +remainingTime+" seconds left)");
            } else {
                this.progressDialog.setMessage(GuestListActivity.guestCounter + " out of "
                        +GuestListActivity.selectedGuestsSize+" guest deleted (" +(remainingTime / 60)+" minutes left)");
            }*/
        }
    }
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        Runtime.getRuntime().gc();
        Log.d(TAG, "onPostExecute");

    }

    private void doAllGuestDelete()
    {
        door = new Door();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            try {
                Utils u = new Utils();
                u.requestType = Utils.KEY_REQ;
                u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
                u.requestDirection = Utils.TCP_SEND_PACKET;

                byte[] packet = new byte[MAX_PKT_SIZE];
                packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
                packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_LENGTH_POS] = DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_ALL_GUEST;
                //String doorID = MainActivity.whichDoor.getId();
                door.setId(appContext.getDoor().getId());
                door.setName(appContext.getDoor().getName());//db.getDoor(doorID).getName());

                //packet[DeleteGuestPacket.CHECKSUM_DELETE_ALL_SENT] = u.calculateChecksum(packet, true);

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
                Log.d(TAG, "doAllGuestDelete/Sent Packet:" + u.commandDetails);
                u.setUtilsInfo(u);
                mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                    @Override
                    public void onDataAvailable(String data) {
                        Log.d(TAG, data);
                        processPacket(data, thisGuest);
                    }
                },null);

            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    private void doGuestDelete(ArrayList<Guest> selectedGuests, int position)
    {

        if(selectedGuests.size()== position){
            Log.d(TAG, "Guest Deletion Complete:" + position);
            //GuestListActivity.progressDialog1.dismiss();
            publishProgress(100);
            return;
        }
        door = new Door();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            try {
                Utils u = new Utils();
                u.requestType = Utils.KEY_REQ;
                u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
                u.requestDirection = Utils.TCP_SEND_PACKET;

                byte[] packet = new byte[MAX_PKT_SIZE];
                packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
                packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_LENGTH_POS] = DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_SELECTED_GUEST;
                //String doorID = MainActivity.whichDoor.getId();
                door.setId(appContext.getDoor().getId());
                door.setName(appContext.getDoor().getName());//db.getDoor(doorID).getName());

                if (GuestListActivity.isSelectAll) {
                    Log.d(TAG, "Delete All:" + DeleteGuestPacket.DELETE_ALL);
                } else {
                    Log.d(TAG, "Selected Guest:"+selectedGuests.get(position));
                    byte[] guestMac = u.getMacIdInHex(selectedGuests.get(position).getId());
                    System.arraycopy(guestMac, 0, packet, DeleteGuestPacket.GUEST_MAC_START, guestMac.length);
                    thisGuest = selectedGuests.get(position);
                    Log.d(TAG, "Sent Guest:" + selectedGuests.get(position).getName());
                }
                //packet[DeleteGuestPacket.CHECKSUM_DELETE_SELECTED_SENT] = u.calculateChecksum(packet, true);

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
                Log.d(TAG, "Sent Packet:" + u.commandDetails);
                u.setUtilsInfo(u);
                mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                    @Override
                    public void onDataAvailable(String data) {
                        Log.d(TAG, data);
                        processPacket(data, thisGuest);
                    }
                },null);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    private void processPacket(String packet, Guest guest)
    {
        DatabaseHandler databaseHandler = new DatabaseHandler(context);
        Log.d(TAG, "processPacket/Received Packet:" + packet);
        if (packet != null && packet.length() >= Packet.DeleteGuestPacket.RECEIVED_PACKET_LENGTH_DELETE_GUEST){
            //strBytes = packet.getBytes(StandardCharsets.ISO_8859_1);

            if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ &&
                    packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        //new DatabaseHandler(context).delete((Guest) guest, door);
                        if(!GuestListActivity.isSelectAll) {
                            Log.d(TAG, "Deleted/!selectAll:" + databaseHandler.delete(guest, door));
                            //guests.remove(guest);
                            GuestListActivity.guestCounter++;
                            guestPosition++;
                            GuestListActivity.progressBarStatus = ((100/guestList.size())*
                                    GuestListActivity.guestCounter);
Log.d(TAG, "GuestDeletion/Setting progress to " + GuestListActivity.progressBarStatus);
                            /*GuestListActivity.progressDialog1.setMessage(GuestListActivity.guestCounter + " guest deleted");
                            GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);*/
                            publishProgress(GuestListActivity.progressBarStatus);
                            if(Utils.selectedGuestsSize != guestPosition) {
                                doGuestDelete(guestList, guestPosition);
                            } else {
                                Log.d(TAG, "Guest Deletion Complete:" + guestPosition);
                                //GuestListActivity.progressDialog1.dismiss();
                                publishProgress(100);
                                guestPosition = 0;
                                GuestListActivity.guestCounter = 0;
                                onUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED, null);
                            }
                        } else {
                            for(Guest obj : guestList)
                            {
                                Log.d(TAG, obj.getId()+" Deleted:" + databaseHandler.delete(obj, door));

                                GuestListActivity.guestCounter++;
                                GuestListActivity.progressBarStatus = ((100/ Utils.selectedGuestsSize)*
                                        GuestListActivity.guestCounter);
                                //GuestListActivity.progressDialog1.setMessage(GuestListActivity.guestCounter + " guest deleted");
                                //GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);
                                publishProgress(GuestListActivity.progressBarStatus);
                            }
                            //GuestListActivity.progressDialog1.dismiss();
                            publishProgress(100);
                            guestPosition = 0;
                            GuestListActivity.guestCounter = 0;
                            guestList.clear();
                            onUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED, null);
                        }
                        //notifyDataSetChanged();
                        //onUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED);
                        Log.d(TAG, "Successfully Deleted");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(activity.findViewById(R.id.menu_fab), "Guest Successfully Deleted", Snackbar.LENGTH_LONG).show();
                            }
                        });

                        // mOnDeleteListener.onDelete();
                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Log.d(TAG, "Deletion Failed");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(activity.findViewById(R.id.menu_fab), "Deletion Failed", Snackbar.LENGTH_LONG).show();
                            }
                        });
                        GuestListActivity.guestCounter++;
                        guestPosition++;
                        GuestListActivity.progressBarStatus = ((100/ Utils.selectedGuestsSize)*
                                GuestListActivity.guestCounter);

                        //GuestListActivity.progressDialog1.setMessage("Guest deletion failed");
                        //GuestListActivity.progressDialog1.setProgress(GuestListActivity.progressBarStatus);
                        publishProgress(GuestListActivity.progressBarStatus);
                        if(Utils.selectedGuestsSize!=guestPosition) {
                            doGuestDelete(Utils.selectedGuests, guestPosition);
                        } else {
                            Log.d(TAG, "Guest Deletion Complete:" + guestPosition);
                            //GuestListActivity.progressDialog1.dismiss();
                            publishProgress(100);
                            guestPosition = 0;
                            GuestListActivity.guestCounter = 0;
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
