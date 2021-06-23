package com.asiczen.azlock.util;

import android.content.Context;
import com.asiczen.azlock.R;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import java.io.InputStream;
import java.security.KeyStore;

/**
 * Created by bpradhan on 13-03-2018.
 */

class MyHttpClient extends DefaultHttpClient {
    private final Context context;

    public MyHttpClient(Context context){
        this.context=context;
    }



    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        // Register for port 443 our SSLSocketFactory with our keystore
        // to the ConnectionManager
        registry.register(new Scheme("https", newSslSocketFactory(), 443));
        return new SingleClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSslSocketFactory() {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            //name of your keystore file here
            try (InputStream in = context.getResources().openRawResource(R.raw.keystore)) {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                trusted.load(in, "mysecret".toCharArray());
            }
            // Pass the keystore to the SSLSocketFactory. The factory is responsible
            // for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER); // This can be changed to less stricter verifiers, according to need
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    /*public void send(String url,String data){

        //DefaultHttpClient  client = new MyHttpClient(this);
        HttpGet get = new HttpGet(url);
// Execute the GET call and obtain the response
        try {
            HttpResponse getResponse = client.execute(get);
            HttpEntity responseEntity = getResponse.getEntity();
            Log.d("hhhh","mssg "+responseEntity);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }*/
}
