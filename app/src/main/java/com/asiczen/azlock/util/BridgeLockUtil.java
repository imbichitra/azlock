package com.asiczen.azlock.util;

import android.content.Context;
import android.util.Log;

/**
 * Created by somnath on 07-07-2017.
 */

public class BridgeLockUtil {

    private final FileAccess fileAccess;
    private final String TAG=BridgeLockUtil.class.getSimpleName();

    public BridgeLockUtil(Context context){
        fileAccess=new FileAccess(context, Utils.LOCK_MAC_LIST_FILE);
    }

    public String[] getLocks(){
        String readMacFile=fileAccess.read();
        Log.d(TAG,"Saved Mac List:"+readMacFile);
        String[] addedLockLists;
        if(fileAccess.FILE_NOT_FOUND || readMacFile==null){
            addedLockLists=new String[]{};
        }
        else {
            addedLockLists=readMacFile.split(",");
        }
        return addedLockLists;
    }

    public void add(String mac){
        String readMacFile=fileAccess.read();
        Log.d(TAG, "add: "+readMacFile);
        mac=mac.toUpperCase();
        Log.d(TAG, "fileAccess.FILE_NOT_FOUND: "+fileAccess.FILE_NOT_FOUND);
        if(fileAccess.FILE_NOT_FOUND || readMacFile==null || readMacFile.isEmpty()){
            fileAccess.write(mac);
        }
        else {
            if (readMacFile.charAt(readMacFile.length()-1) == ','){
                readMacFile = readMacFile.substring(0,readMacFile.length()-1);
                fileAccess.write(readMacFile+","+mac);
            }else
                fileAccess.write(readMacFile+","+mac);
        }
        String readMacFile1=fileAccess.read();
        Log.d(TAG, "add:1 "+readMacFile1);
    }

    public void delete(String mac){
        String readMacFile=fileAccess.read();
        if(fileAccess.FILE_NOT_FOUND || readMacFile==null){
            return;
        }
        String[] macList = readMacFile.split(",");
        StringBuilder newList= new StringBuilder();
        for (int i=0;i<macList.length;i++) {
            if(macList[i].equalsIgnoreCase(mac)){
                continue;
            }
            if(i==macList.length-1){
                newList.append(macList[i]);
            }
            else {
                newList.append(macList[i]).append(",");
            }
        }
        Log.d(TAG,"Mac List after deletion:"+newList);
        fileAccess.write(newList.toString());
    }

}
