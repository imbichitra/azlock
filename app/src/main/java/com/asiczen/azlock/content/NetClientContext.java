package com.asiczen.azlock.content;

import android.os.AsyncTask;
import android.util.Log;

import com.asiczen.azlock.net.NetClientAsyncTask;

/**
 * Created by somnath on 08-06-2017.
 */

public class NetClientContext {
    private static final NetClientContext ourInstance = new NetClientContext();
    private NetClientAsyncTask clientAsyncTask;
    private boolean receiveMultiple = false;
    private final String TAG = NetClientContext.class.getSimpleName();
    private int stopConditionIndex;
    private char stopFlag;

    public static NetClientContext getContext() {
        return ourInstance;
    }

    private NetClientContext() {
    }

    public void setNetClient(NetClientAsyncTask clientAsyncTask){
        this.clientAsyncTask=clientAsyncTask;
    }

    public void disconnectClient(){
        if(clientAsyncTask!=null) {
            clientAsyncTask.disconnect();
            receiveMultiple = false;
        }
        else{
            throw new NullPointerException(TAG+": Failed to disconnect client. \nCause: NetClientAsyncTask object is null.");
        }
    }

    public boolean shouldReceiveMultiple() {
        return receiveMultiple;
    }

    public void setReceiveMultiple(boolean receiveMultiple, int stopConditionIndex, char stopFlag) {
        this.receiveMultiple = receiveMultiple;
        this.stopConditionIndex=stopConditionIndex;
        this.stopFlag=stopFlag;
    }

    public int getStopConditionIndex() {
        return stopConditionIndex;
    }

    public char getStopFlag() {
        return stopFlag;
    }

    public AsyncTask.Status getAsyncTaskStatus()
    {
        switch (clientAsyncTask.getStatus()){
            case PENDING:
                // My AsyncTask has not started yet
                Log.e(TAG,"netClientAsyncTask [PENDING]");
                break;
            case RUNNING:
                // My AsyncTask is currently doing work in doInBackground()
                Log.e(TAG,"netClientAsyncTask [RUNNING]");
                break;
            case FINISHED:
                // My AsyncTask is done and onPostExecute was called
                Log.e(TAG,"netClientAsyncTask [FINISHED]");
                break;
        }
        return clientAsyncTask.getStatus();
    }
}
