package com.asiczen.azlock.net;

import android.annotation.SuppressLint;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.asiczen.azlock.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

public class VolleySingleton {
    @SuppressLint("StaticFieldLeak")
    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private VolleySingleton(Context context){
        mContext = context;
    }

    private RequestQueue getRequestQueue() {
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(mContext.getApplicationContext(),hurlStack);
        }
        return requestQueue;
    }

    public static synchronized VolleySingleton getInstance(Context context){
        if(instance == null){
            instance = new VolleySingleton(context);
        }
        return  instance;
    }

    public<T> void addToRequestQueue(Request<T> request){
        getRequestQueue().add(request);
    }

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore trusted = KeyStore.getInstance(keyStoreType);
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            try (InputStream inputStream = mContext.getResources().openRawResource(R.raw.keystore)) {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                String KEYSTORE_PASSWORD = "mysecret";
                trusted.load(inputStream, KEYSTORE_PASSWORD.toCharArray());
            }

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trusted);

            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, tmf.getTrustManagers(), null);
             return context.getSocketFactory();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    HurlStack hurlStack = new HurlStack() {
        @SuppressLint("AllowAllHostnameVerifier")
        @Override
        protected HttpURLConnection createConnection(URL url) throws IOException {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) super.createConnection(url);
            try {
                httpsURLConnection.setSSLSocketFactory(getSSLSocketFactory());
                httpsURLConnection.setHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return httpsURLConnection;
        }
    };
}
