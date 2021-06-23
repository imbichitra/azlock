package com.asiczen.azlock.net;

import android.util.Log;

import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

public class VolleyErrors {
    public static final String TAG = VolleyErrors.class.getSimpleName();
    public static final String TIME_OUT = "Unable to contact server or check your internet";
    public static final String ERROR_500 = "Server Error";
    public static final String ERROR_409 = "Record already present in database.";
    public static final String ERROR_401 = "UnAuthorised Access to Server";
    public static final String ERROR_404 = "Bad Request";
    public static final String DEFAULT = "Unable to process";

    public static String error(VolleyError error){
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            Log.d(TAG, "Volleyerror: "+error.getMessage());
            return TIME_OUT;
        }else {
            try {
                switch (error.networkResponse.statusCode) {
                    case 500:
                        return ERROR_500;
                    case 409:
                        return ERROR_409;
                    case 401:
                        return ERROR_401;
                    case 404:
                        return ERROR_404;
                    default:
                        return DEFAULT;
                }
            } catch (Exception e) {
                Log.e(TAG, "Volleyerror:networkResponse.statusCode " + e.getMessage());
                e.printStackTrace();
                return e.getMessage();
            }
        }
    }
}
