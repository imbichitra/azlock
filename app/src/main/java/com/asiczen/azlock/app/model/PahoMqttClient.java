package com.asiczen.azlock.app.model;

import android.content.Context;
import androidx.annotation.NonNull;

import android.util.Base64;
import android.util.Log;

import com.asiczen.azlock.R;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by brijesh on 20/4/17.
 */

public class PahoMqttClient {

    private static final String TAG = "PahoMqttClient";
    private MqttAndroidClient mqttAndroidClient;
    private int status = Packet.SubscribeTopic.FAIL;

    public MqttAndroidClient getMqttClient(Context context, String brokerUrl, String clientId) {
        mqttAndroidClient = new MqttAndroidClient(context, brokerUrl, clientId);
        try {
            MqttConnectOptions MQTT_CONNECTION_OPTIONS =    getMqttConnectionOption();
            if (brokerUrl.contains("ssl")) {
                InputStream i=  context.getResources().openRawResource(R.raw.client_ca);
                InputStream i1=  context.getResources().openRawResource(R.raw.client_crt);
                InputStream inputStream =  context.getResources().openRawResource(R.raw.client_key);
                MQTT_CONNECTION_OPTIONS.setSocketFactory(createSSLSocketFactory(i,i1,inputStream));
            }
            IMqttToken token = mqttAndroidClient.connect(MQTT_CONNECTION_OPTIONS);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions());
                    Log.d(TAG, "MQTT CONNECTED:0 ");
                    Log.d(TAG, "Success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "MQTT CONNETION FAILED:0 ");
                    exception.printStackTrace();
                    Log.d(TAG, "Failure " + exception.toString());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        return mqttAndroidClient;
    }

    public void disconnect(@NonNull MqttAndroidClient client) throws MqttException {
        IMqttToken mqttToken = client.disconnect();
        mqttToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Successfully disconnected");
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.d(TAG, "Failed to disconnected " + throwable.toString());
            }
        });
    }

    @NonNull
    private DisconnectedBufferOptions getDisconnectedBufferOptions() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(true);
        disconnectedBufferOptions.setDeleteOldestMessages(true);
        return disconnectedBufferOptions;
    }

    @NonNull
    private MqttConnectOptions getMqttConnectionOption() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setAutomaticReconnect(false);
        //mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        //mqttConnectOptions.setWill(Packet.PublishTopic.PUBLISH_TOPIC, "I am going offline".getBytes(), 1, true);
        //mqttConnectOptions.setUserName(Utils.userId);
        //mqttConnectOptions.setPassword(Utils.password.toCharArray());
        return mqttConnectOptions;
    }


    public void publishMessage(@NonNull MqttAndroidClient client, @NonNull byte[] msg, int qos, @NonNull String topic)
            throws MqttException {
        /*byte[] encodedPayload = new byte[0];
        encodedPayload = msg.getBytes("UTF-8");*/
        /*for (byte b:encodedPayload)
            Utils.printByteArray(encodedPayload);*/
            //Log.d(TAG, "publishMessage: "+b);
        MqttMessage message = new MqttMessage(msg);
        message.setId(320);
        message.setRetained(true);
        message.setQos(qos);
        client.publish(topic, message);
    }

    public int subscribe(@NonNull MqttAndroidClient client, @NonNull final String topic, int qos) throws MqttException {
        IMqttToken token = client.subscribe(topic, qos);
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Subscribe Successfully " + topic);
                status = Packet.SubscribeTopic.SUCCESS;
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "Subscribe Failed " + topic);
                status = Packet.SubscribeTopic.FAIL;
            }
        });
        return status;
    }

    public void unSubscribe(@NonNull MqttAndroidClient client, @NonNull final String topic) throws MqttException {

        IMqttToken token = client.unsubscribe(topic);

        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "UnSubscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "UnSubscribe Failed " + topic);
            }
        });
    }

    private SSLSocketFactory createSSLSocketFactory(InputStream cac, InputStream key, InputStream inputStream)
    {
        try
        {
            TrustManagerFactory tmf;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca;
            try {
                ca = cf.generateCertificate(cac);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                cac.close();
            }
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            Certificate ca1;
            try {
                ca1 = cf.generateCertificate(key);
                System.out.println("ca=" + ((X509Certificate) ca1).getSubjectDN());
            } finally {
                key.close();
            }
            PrivateKey privateKey = loadPrivateKey(inputStream);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", ca1);
            ks.setKeyEntry("private-key", privateKey, null, new java.security.cert.Certificate[]{ca1});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, null);

            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        }
        catch (IOException | KeyStoreException | KeyManagementException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException e)
        {
            //LOG.error("Creating ssl socket factory failed", e);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PrivateKey loadPrivateKey(InputStream is)
            throws IOException, GeneralSecurityException {
        PrivateKey key = null;
        //InputStream is = null;
        try {
            //is = fileName.getClass().getResourceAsStream("/" + fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") &&
                            line.endsWith("-----")) {
                        /*&&
                            line.endsWith(" PRIVATE KEY-----")*/
                        inKey = true;
                    }
                }
                else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith("-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
            //
            byte[] encoded = Base64.decode(builder.toString(), Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);
        } finally {
            closeSilent(is);
        }
        return key;
    }

    public static void closeSilent(final InputStream is) {
        if (is == null) return;
        try { is.close(); } catch (Exception ign) {ign.printStackTrace();}
    }
}


