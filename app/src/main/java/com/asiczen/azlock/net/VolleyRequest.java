package com.asiczen.azlock.net;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.asiczen.azlock.content.AppContext;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VolleyRequest {
    //private static final String TAG = "VolleyJsonObjectRequest";
    private static VolleyResponse mInterFace;
    public static final String STATUS = "status";
    public static final String STATUS_SUCCESS = "Y";
    public static final String STATUS_FAIL = "N";
    public static final String STATUS_EMAIL_EXIST = "E";

    public static void jsonObjectRequest(final Context context, String url, JSONObject postparams, int method, VolleyResponse interFace){
        mInterFace = interFace;
        JsonObjectRequest postRequest = new JsonObjectRequest(method, url, postparams,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mInterFace.VolleyObjectResponse(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mInterFace.VolleyError(error);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                String credentials = AppContext.getUserId()+":"+AppContext.getPassword();
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(),
                        Base64.NO_WRAP);
                headers.put("Authorization", auth);
                headers.put("Content-Type","application/json");
                return headers;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,//1 minute
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(context).addToRequestQueue(postRequest);
    }

    /*public static void jsonObjectRequest(final Context context,String url,VolleyResponse interFace){
        mInterFace = interFace;
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mInterFace.VolleyObjectResponse(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mInterFace.VolleyError(error);
            }
        });
        VolleySingleton.getInstance(context).addToRequestQueue(getRequest);
    }*/
}
