package com.asiczen.azlock.content;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.asiczen.azlock.userlogin;

public class MySharedPreferences {
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private static final String PREF_NAME = "OnBoardScreen";
    private static final String KEY_IS_ONBOARD_SCREEN_COMPLIT = "onboard";
    private static final String EMAIL ="email";
    private static final String PASSWORD = "password";
    private static final String MAC = "mac";
    public static final String MOB_NO = "mob_no";
    private final Context context;
    @SuppressLint("CommitPrefEdits")
    public MySharedPreferences(Context context){
        int PRIVATE_MODE = 0;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        this.context = context;
    }

    public void setOnBrodScreenStatus(){
        editor.putBoolean(KEY_IS_ONBOARD_SCREEN_COMPLIT, true);
        editor.commit();
    }

    public boolean getOnBoradScreenStatus(){
        return pref.getBoolean(KEY_IS_ONBOARD_SCREEN_COMPLIT, false);
    }

    public void setUserDetail(String email,String password,String mac){
        editor.putString(EMAIL, email);
        editor.putString(PASSWORD, password);
        editor.putString(MAC, mac);
        editor.commit();
    }

    public String getMac(){
        return pref.getString(MAC, "");
    }

    public boolean isUserDataSet(){
        //Log.d("SHARED", "isUserDataSet: "+pref.getString(EMAIL,"")+" pass "+pref.getString(MAC,""));
        return pref.contains(EMAIL) && pref.contains(PASSWORD) && pref.contains(MAC);
    }

    public String getEmail(){
        return pref.getString(EMAIL, "");
    }

    public String getPassword(){
        return pref.getString(PASSWORD, "");
    }

    public void logoutUser(){
        // Clearing all data from Shared Preferences
        String no = getValues(MOB_NO);
        editor.clear();
        editor.commit();
        setOnBrodScreenStatus();
        setUserMobile(no);
        Intent i = new Intent(context, userlogin.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(i);
    }

    private void setUserMobile(String no) {
        setValues(MOB_NO,no);
    }

    public void setValues(String key,String value){
        editor.putString(key, value);
        editor.commit();
    }

    public String getValues(String key){
        return pref.getString(key, "");
    }
}
