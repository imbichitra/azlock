package com.asiczen.azlock.net;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asiczen.azlock.R;
import com.asiczen.azlock.content.NetClientContext;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * Created by Somnath on 9/19/2016.
 */
public class NetClientAsyncTask extends AsyncTask<Void, Void, String> {

    private final String TAG = NetClientAsyncTask.class.getSimpleName();
    public static int SOCKET_NOT_CONNECTED = 1000;
    public static int UNABLE_TO_CONNECT = 1002;
    public static int UNABLE_TO_DISCONNECT = 1003;
    public static int MESSAGE_NOT_RECEIVED = 1001;
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
    //private ProgressDialog progressDialog;
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    public static int ERROR_CODE = Integer.MIN_VALUE;
    private int resultCode = Activity.RESULT_CANCELED;
    //private volatile boolean isTimerExpired = false;
    //private boolean isMsgReceived = false;
    private NetClientContext netClientContext;
    private LinkedList<String> multipleResponseMsg;
    private String responseMessage = null;
    @SuppressLint("StaticFieldLeak")
    private Activity activity;
    private int counter = 1;
    private String activityName;

    private final String host;
    private final int port;

    private final byte[] key = {(byte) 0x2B, (byte) 0x7E, (byte) 0x15, (byte) 0x16, (byte) 0x28, (byte) 0xAE, (byte) 0xD2, (byte) 0xA6,
            (byte) 0xAB, (byte) 0xF7, (byte) 0x15, (byte) 0x88, (byte) 0x09, (byte) 0xCF, (byte) 0x4F, (byte) 0x3C};
    private final CryptoUtils encode = new CryptoUtils(key);
    private final Boolean encrypt;

    private AlertDialog pdialog;
    @SuppressLint("StaticFieldLeak")
    private TextView dialogTextView;

    /**
     * Constructor with Host, Port
     * param host
     * param port
     */
    public NetClientAsyncTask(Boolean encrypt, Activity activity, String host, int port, String packet, OnTaskCompleted<String> onTaskCompleted, String progessbarMsg) {
        this.host = host;
        this.port = port;
        this.mOnTaskCompleted = onTaskCompleted;
        this.mContext = activity;
        this.encrypt = encrypt;
        //progressDialog = new ProgressDialog(mContext);

        this.progessbarMsg = progessbarMsg;
    }

    public NetClientAsyncTask(Boolean encrypt, Activity activity, String host, int port, byte[] packet,
                              OnTaskCompleted<String> onTaskCompleted) {
        this.host = host;
        this.port = port;
        this.mOnTaskCompleted = onTaskCompleted;
        this.bytePacket = packet;
        this.mContext = activity;
        this.activity = activity;
        this.encrypt = encrypt;
        //progressDialog = new ProgressDialog(mContext);

        netClientContext = NetClientContext.getContext();
        multipleResponseMsg = new LinkedList<>();
    }

    public NetClientAsyncTask(Boolean encrypt, Activity activity, byte[] packet, String host, int port,
                              OnTaskCompleted<LinkedList<String>> onTaskCompleted) {
        this.host = host;
        this.port = port;
        this.receiveListOnTaskCompleted = onTaskCompleted;
        this.bytePacket = packet;
        this.mContext = activity;
        this.activity = activity;
        this.encrypt = encrypt;
        //progressDialog = new ProgressDialog(mContext);

        netClientContext = NetClientContext.getContext();
        multipleResponseMsg = new LinkedList<>();
    }

    private void dialogCreate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewGroup nullParent = null;
        View bridgeConnectView = null;
        if (inflater != null) {
            bridgeConnectView = inflater.inflate(R.layout.progressbar, nullParent, false);
        }
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        pdialog = builder.create();
        dialogTextView.setText(progessbarMsg);
        pdialog.show();
    }

    private void connect() {
        int SO_TIMEOUT = 30000;
        int CONNECTION_TIMEOUT = 10000;
        SocketAddress socketAddress;
        try {
            if (socket == null) {
                Log.d(TAG, "Connecting [" + host + " " + port + "]");
                InetAddress inetAddress = InetAddress.getByName(host);
                socketAddress = new InetSocketAddress(inetAddress, port);
                socket = new Socket();
                socket.connect(socketAddress, CONNECTION_TIMEOUT);
                socket.setSoTimeout(SO_TIMEOUT);

                /* create stream to send String message */
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                /* create stream to send byte[] message */
                outputStream = socket.getOutputStream();
                dos = new DataOutputStream(outputStream);
                inputStream = socket.getInputStream();
                dis = new DataInputStream(inputStream);
                Log.d(TAG, "SOCKET OPEN: ");
                Log.d(TAG, "SocketConnectionStatus:" + socket.isConnected());
            } else {
                Log.d(TAG, "Socket is not null and connected = " + socket.isConnected());
            }
        } catch (IOException e) {
            if (pdialog != null && pdialog.isShowing()) {
                pdialog.dismiss();
            }
            Log.d(TAG, "Unable to conect");
            ERROR_CODE = UNABLE_TO_CONNECT;
            resultCode = Activity.RESULT_CANCELED;
            e.printStackTrace();
        }
    }

    public void disconnect() {
        Log.d(TAG, "disconnect: ");
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
                    Log.d(TAG, "SOCKET CLOSED: ");
                    Log.d(TAG, "disconnect: socket closed");
                } catch (IOException e) {
                    Log.d(TAG, "unable to disconnect: ");
                    if (pdialog != null && pdialog.isShowing()) {
                        pdialog.dismiss();
                    }
                    ERROR_CODE = UNABLE_TO_DISCONNECT;
                    resultCode = Activity.RESULT_CANCELED;
                    cancel(true);
                    e.printStackTrace();
                }
            }
        }
    }

    public void send(String message) {
        if (message != null) {
            connect();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (socket != null && socket.isConnected()) {
                Log.d(TAG, "send/Sent Msg:" + message);
                printByteArray(message);
                out.write(message);
                out.flush();
                //initializeTimer();
            } else {
                ERROR_CODE = SOCKET_NOT_CONNECTED;
                resultCode = Activity.RESULT_CANCELED;
                Log.e(TAG, "Socket not connected");
                cancel(true);
                if (pdialog != null && pdialog.isShowing()) {
                    pdialog.dismiss();
                }
            }
        }
    }

    private void send(byte[] message) {
        byte[] packet = new byte[16];
        try {
            if (socket != null && socket.isConnected()) {
                //isTimerExpired = false;
                //isMsgReceived = false;
                Log.d(TAG, "sending " + message.length + " byte message:");
                Utils.printByteArray(message);
                if (encrypt) {
                    Log.d(TAG, "sending Encrypted message");
                    for (int i = 0; i <= 1; i++) {
                        System.arraycopy(message, i * 16, packet, 0, 16);
                        try {
                            packet = encode.AESEncode(packet);
                            System.arraycopy(packet, 0, message, i * 16, 16);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                sendBytes(message, 0, message.length);
                Utils.printByteArray(message);
                //initializeTimer();
            } else {
                ERROR_CODE = SOCKET_NOT_CONNECTED;
                resultCode = Activity.RESULT_CANCELED;
                Log.e(TAG, "Socket not connected");
                cancel(true);
                if (pdialog != null && pdialog.isShowing()) {
                    pdialog.dismiss();
                }
            }
        } catch (IOException e) {
            if (pdialog != null && pdialog.isShowing()) {
                pdialog.dismiss();
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
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "writing " + len + " bytes data");
            dos.write(myByteArray, start, len);
            dos.flush();
            Log.d(TAG, "SOCKET DATA WRITE: ");
        }
    }

    private byte[] readBytes() {
        byte[] data = null;

        try {

            int len = 120;//dis.readInt();
            data = new byte[len];

            Log.i(TAG, "readBytes");
            //isMsgReceived = true;
            resultCode = Activity.RESULT_OK;
            dis.read(data, 0, len);
            Log.d(TAG, "read complete");
            Log.d(TAG, "SOCKET DATA READ: ");
        } catch (IOException e) {
            Log.d(TAG, "read exception happened");
            e.printStackTrace();
        }
        if (!netClientContext.shouldReceiveMultiple()) {
            Log.d(TAG, "disconnect: shouldReceiveMultiple "+netClientContext.shouldReceiveMultiple());
            Log.d(TAG, "SOCKET DISCONNECT CALLED: ");
            disconnect();
        }
        return data;
    }

    public String receive() {
        Log.i(TAG, "Receiving");
        try {
            StringBuilder message = new StringBuilder();
            int charsRead;
            char[] buffer = new char[BUFFER_SIZE];

            while (((charsRead = in.read(buffer)) != -1)) {
                Log.i(TAG, "Reading");
                message.append(new String(buffer).substring(0, charsRead));
                resultCode = Activity.RESULT_OK;
            }
            //isMsgReceived = true;
            Log.d(TAG, "receive/Received Msg:" + message);
            printByteArray(message.toString());
            disconnect(); // disconnect server
            return message.toString();
        } catch (SocketTimeoutException e) {
            if (pdialog != null && pdialog.isShowing()) {
                pdialog.dismiss();
            }
            resultCode = Activity.RESULT_CANCELED;
            ERROR_CODE = MESSAGE_NOT_RECEIVED;
            Log.e(TAG, "Socket Timeout");
            e.printStackTrace();
            return "Socket Timeout";// + e.getMessage();
        } catch (IOException e) {
            if (pdialog != null && pdialog.isShowing()) {
                pdialog.dismiss();
            }
            e.printStackTrace();
            return "Error receiving response: ";// + e.getMessage();
        }
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    private void printByteArray(String byteArray) {
        Utils.printByteArray(byteArray.getBytes(StandardCharsets.ISO_8859_1));

        Log.d(TAG, "Unsigned Byte Array:");
        Utils.printByteArray(Utils.toUnsignedBytes(byteArray));
    }

    public void showProgressDialog(boolean isShowProgressDialog, String progessbarMsg) {
        this.isShowProgressDialog = isShowProgressDialog;
        this.progessbarMsg = progessbarMsg;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "onCancelled()");
        mOnTaskCompleted.onTaskCompleted(resultCode, responseMessage);
        if (pdialog != null && pdialog.isShowing()) {
            pdialog.dismiss();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        //send(packet);
        Log.d(TAG, "doInBackground");
        Log.d(TAG, "SOCKET START: ");
        connect();
        Log.d(TAG, "SOCKET OPENED: ");
        if (socket != null && socket.isConnected()) {
            send(bytePacket);
            if (!isCancelled()) {
                //responseMsg = receive();
                Log.d(TAG, "shouldReceiveMultiple:" + netClientContext.shouldReceiveMultiple());
                if (!netClientContext.shouldReceiveMultiple()) {
                    //responseMessage = new String(readBytes(), "ISO-8859-1");
                    responseMessage = (new String(readBytes(), StandardCharsets.ISO_8859_1)).replaceAll("^\\x00*", "");
                    if (responseMessage.isEmpty()) {
                        resultCode = Activity.RESULT_CANCELED;
                        ERROR_CODE = MESSAGE_NOT_RECEIVED;
                        Log.d(TAG, "No data received");
                    } else {
                        Log.d(TAG, "Data received");
                        printByteArray(responseMessage);
                    }

                    //mOnTaskCompleted.onTaskCompleted(resultCode, responseMessage);
                } else {
                    if (counter <= 150) {
                        do {
                            responseMessage = new String(readBytes(), StandardCharsets.ISO_8859_1);
                            multipleResponseMsg.add(responseMessage);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (activityName != null) {
                                        if (activityName.equals("GuestListActivity")) {
                                            counter++;
                                            if (counter % 2 == 0) {
                                                dialogTextView.setText((counter / 2) + R.string.guest_download);
                                            }
                                        } else if (activityName.equals("GuestLogActivity")) {
                                            counter++;
                                            dialogTextView.setText(counter + R.string.log_download);
                                        }
                                    }
                                }
                            });

                            //printByteArray(multipleResponseMsg);
                            //mOnTaskCompleted.onTaskCompleted(resultCode, responseMessage);
                            Log.d(TAG, "count:" + counter + " getStopConditionIndex:" + netClientContext.getStopConditionIndex() + " stopFlag:" + netClientContext.getStopFlag());
                            Log.d(TAG, "Condition:" + (responseMessage != null && responseMessage.charAt(netClientContext.getStopConditionIndex()) != netClientContext.getStopFlag()));
                        } while (responseMessage != null && responseMessage.charAt(netClientContext.getStopConditionIndex()) != netClientContext.getStopFlag());
                        //responseMessage=multipleResponseMsg;
                    } else {
                        disconnect();
                    }
                }
            }
        } else {
            Log.e(TAG, "Socket not connected");
            resultCode = Activity.RESULT_CANCELED;
            ERROR_CODE = SOCKET_NOT_CONNECTED;
        }
        Log.d(TAG, "doInBackground completed");
        return responseMessage;
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute: [" + isShowProgressDialog + ", " + progessbarMsg + "]");
        if (isShowProgressDialog && progessbarMsg != null) {
           /* progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(progessbarMsg);
            progressDialog.show();*/
            dialogCreate();
            // Log.d(TAG, "dialogShowing:"+progressDialog.isShowing());
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "TaskCompleted/Received Msg:" + result);
        if (socket != null) {
            Log.d(TAG, "TaskCompleted/Socket:" + socket.isClosed());
        } else {
            Log.d(TAG, "TaskCompleted/Socket: null");
        }
        if (pdialog != null && pdialog.isShowing()) {
            pdialog.dismiss();
        }
        if (netClientContext.shouldReceiveMultiple()) {
            receiveListOnTaskCompleted.onTaskCompleted(resultCode, multipleResponseMsg);
        } else {
            mOnTaskCompleted.onTaskCompleted(resultCode, result);
        }
    }
}
