package com.asiczen.azlock.net;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface VolleyResponse {
    void VolleyError(VolleyError error);
    void VolleyObjectResponse(JSONObject response);
}
