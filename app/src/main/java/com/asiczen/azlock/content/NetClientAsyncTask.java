package com.asiczen.azlock.content;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.asiczen.azlock.OnTaskCompleted;
import com.asiczen.azlock.util.Utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * Created by Somnath on 9/19/2016.
 */
class NetClientAsyncTask extends AsyncTask<Void, Void, String> {

    private final String TAG = NetClientAsyncTask.class.getSimpleName();
    private static final int BUFFER_SIZE = 256;
    private Socket socket = null;
    private PrintWriter out = null;
    private OutputStream outputStream = null;
    private DataOutputStream dos = null;
    private InputStream inputStream = null;
    private DataInputStream dis = null;
    private BufferedReader in = null;
    private OnTaskCompleted<String> mOnTaskCompleted;
    private OnTaskCompleted<LinkedList<String>> receiveListOnTaskCompleted;
    private String progessbarMsg;
    private boolean isShowProgressDialog;
    private byte[] bytePacket;
    private final ProgressDialog progressDialog;
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private int resultCode = Activity.RESULT_CANCELED;
    private NetClientContext netClientContext;
    private LinkedList<String> multipleResponseMsg;
    private String responseMessage=null;
    @SuppressLint("StaticFieldLeak")
    private Activity activity;
    private int counter = 1;
    private String activityName;

    private final String host;
    private final int port;

    /**
     * Constructor with Host, Port
     * param host
     * param port
     */
    public NetClientAsyncTask(Activity activity, String host, int port, String packet, OnTaskCompleted<String> onTaskCompleted, String progessbarMsg) {
        this.host = host;
        this.port = port;
        this.mOnTaskCompleted = onTaskCompleted;
        this.mContext=activity;
        progressDialog = new ProgressDialog(mContext);
        this.progessbarMsg=progessbarMsg;
    }

    public NetClientAsyncTask(Activity activity, String host, int port, byte[] packet,
                              OnTaskCompleted<String> onTaskCompleted) {
        this.host = host;
        this.port = port;
        this.mOnTaskCompleted = onTaskCompleted;
        this.bytePacket=packet;
        this.mContext=activity;
        this.activity=activity;
        progressDialog = new ProgressDialog(mContext);
        netClientContext = NetClientContext.getContext();
        multipleResponseMsg=new LinkedList<>();
    }

    public NetClientAsyncTask(Activity activity, byte[] packet, String host, int port,
                              OnTaskCompleted<LinkedList<String>> onTaskCompleted) {
        this.host = host;
        this.port = port;
        this.receiveListOnTaskCompleted = onTaskCompleted;
        this.bytePacket=packet;
        this.mContext=activity;
        this.activity=activity;
        progressDialog = new ProgressDialog(mContext);
        netClientContext = NetClientContext.getContext();
        multipleResponseMsg=new LinkedList<>();
    }

    private void connect() {
        try {
            if (socket == null) {
                Log.i(TAG, "Connecting ["+host+" "+port+"]");
                SocketAddress socketAddress = new InetSocketAddress(host, port);
                socket = new Socket();
                socket.connect(socketAddress, 10000); //10 seconds
                socket.setSoTimeout(15000); //15 seconds
                /* create stream to send String message */
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                /* create stream to send byte[] message */
                outputStream = socket.getOutputStream();
                dos = new DataOutputStream(outputStream);
                inputStream = socket.getInputStream();
                dis = new DataInputStream(inputStream);
                Log.d(TAG, "SocketConnectionStatus:"+socket.isConnected());
            }
        }
        catch (IOException e) {
            if (progressDialog!=null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            resultCode=Activity.RESULT_CANCELED;
        }
    }

    private void disconnect() {
        if (socket != null) {
            if (socket.isConnected()) {
                try {
                    in.close();
                    out.close();

                    outputStream.close();
                    inputStream.close();
                    dos.close();
                    dis.close();

                    socket.close();
                } catch (IOException e) {
                    if (progressDialog!=null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    resultCode=Activity.RESULT_CANCELED;
                    cancel(true);
                    e.printStackTrace();
                }
            }
        }
    }

    public void send(String message) {
        if (message != null) {
            connect();
            if(socket!=null && socket.isConnected()) {
                Log.d(TAG, "send/Sent Msg:" + message);
                printByteArray(message);
                out.write(message);
                out.flush();
            }
            else
            {
                resultCode=Activity.RESULT_CANCELED;
                Log.e(TAG, "Socket not connected");
                cancel(true);
                if (progressDialog!=null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        }
    }

    private void send(byte[] message)
    {
        try {
            if(socket!=null && socket.isConnected()) {
                //isTimerExpired = false;
                Log.d(TAG, "sending "+message.length);
                sendBytes(message, 0, message.length);
                Utils.printByteArray(message);
            }
            else
            {
                resultCode=Activity.RESULT_CANCELED;
                Log.e(TAG, "Socket not connected");
                cancel(true);
                if (progressDialog!=null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        } catch (IOException e) {
            if (progressDialog!=null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            e.printStackTrace();
        }
    }

    private void sendBytes(byte[] myByteArray, int start, int len) throws IOException {
        if (len < 0)
            throw new IllegalArgumentException("Negative length not allowed");
        if (start < 0 || start >= myByteArray.length)
            throw new IndexOutOfBoundsException("Out of bounds: " + start);
        // Other checks if needed.

        //dos.writeInt(len); /* write if exact length is need to know on receiver side */
        if (len > 0) {
            Log.d(TAG, "writing "+len);
            dos.write(myByteArray, start, len);
            Log.d(TAG, "write complete");
        }
    }

    private byte[] readBytes() {
        byte[] data = null;
        try {
            int len = 120;
            data = new byte[len];

            Log.i(TAG,"readBytes");
            resultCode = Activity.RESULT_OK;
            //dis.readFully(data);
            dis.read(data, 0, len);
            Log.d(TAG, "read complete");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if(!netClientContext.shouldReceiveMultiple()) {
            disconnect();
        }
        return data;
    }

    public String receive() {
        Log.i(TAG,"Receiving");
        try {
            StringBuilder message = new StringBuilder();
            int charsRead;
            char[] buffer = new char[BUFFER_SIZE];

            while(((charsRead = in.read(buffer)) != -1)) {
                Log.i(TAG,"Reading");
                message.append(new String(buffer).substring(0, charsRead));
                resultCode = Activity.RESULT_OK;
            }
            Log.d(TAG, "receive/Received Msg:" + message);
            printByteArray(message.toString());
            disconnect(); // disconnect server
            return message.toString();
        }
        catch (SocketTimeoutException e)
        {
            if (progressDialog!=null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            resultCode = Activity.RESULT_CANCELED;
            int MESSAGE_NOT_RECEIVED = 1001;
            Log.e(TAG, "Socket Timeout");
            e.printStackTrace();
            return "Socket Timeout";
        }
        catch (IOException e) {
            if (progressDialog!=null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            e.printStackTrace();
            return "Error receiving response: ";// + e.getMessage();
        }
    }


    private void printByteArray(String byteArray)
    {
        Utils.printByteArray(byteArray.getBytes(StandardCharsets.ISO_8859_1));
        Log.d(TAG, "Unsigned Byte Array:");
        Utils.printByteArray(Utils.toUnsignedBytes(byteArray));
    }

    @Override
    protected void onCancelled(){
        Log.e(TAG, "onCancelled()");
        mOnTaskCompleted.onTaskCompleted(resultCode, responseMessage);
        if (progressDialog!=null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
       Log.d(TAG, "NetClientAsyncTask doInBackground");
        if(socket!=null)
            Log.d(TAG, "doInBackground/SocketConnectionStatus:"+socket.isConnected());
        connect();
        if(socket!=null && socket.isConnected()){
            send(bytePacket);
            if(!isCancelled()) {
                Log.d(TAG, "shouldReceiveMultiple:"+netClientContext.shouldReceiveMultiple());
                if(!netClientContext.shouldReceiveMultiple()) {
                    responseMessage = new String(readBytes(), StandardCharsets.ISO_8859_1);
                    printByteArray(responseMessage);
                }
                else {
                    do{
                        responseMessage = new String(readBytes(), StandardCharsets.ISO_8859_1);
                        multipleResponseMsg.add(responseMessage);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(activityName!=null){
                                    if (activityName.equals("GuestListActivity")) {
                                        counter++;
                                        if(counter%2==0) {
                                            progressDialog.setMessage((counter/2)+" guests downloaded...");
                                        }
                                    }
                                    else if(activityName.equals("GuestLogActivity")){
                                        counter++;
                                        progressDialog.setMessage(counter+" logs downloaded...");
                                    }
                                }
                            }
                        });
                        Log.d(TAG,"count:"+counter+" getStopConditionIndex:"+netClientContext.getStopConditionIndex()+" stopFlag:"+netClientContext.getStopFlag());
                        Log.d(TAG, "Condition:"+(responseMessage!=null && responseMessage.charAt(netClientContext.getStopConditionIndex())!=netClientContext.getStopFlag()));
                    }while(responseMessage!=null && responseMessage.charAt(netClientContext.getStopConditionIndex())!=netClientContext.getStopFlag());
                }
            }
        }
        else
        {
            Log.e(TAG, "Socket not connected");
            resultCode=Activity.RESULT_CANCELED;
        }
        Log.d(TAG, "doInBackground_2");
        return responseMessage;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute: ["+isShowProgressDialog+", "+progessbarMsg+"]");
        if(isShowProgressDialog && progessbarMsg!=null) {
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(progessbarMsg);
            progressDialog.show();
            Log.d(TAG, "dialogShowing:"+progressDialog.isShowing());
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "TaskCompleted/Received Msg:" + result);
        if(socket!=null) {
            Log.d(TAG, "TaskCompleted/Socket:" + socket.isClosed());
        }
        else
        {
            Log.d(TAG, "TaskCompleted/Socket: null");
        }
        if (progressDialog!=null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if(netClientContext.shouldReceiveMultiple()){
            receiveListOnTaskCompleted.onTaskCompleted(resultCode, multipleResponseMsg);
        }
        else {
            mOnTaskCompleted.onTaskCompleted(resultCode, result);
        }
    }
}
