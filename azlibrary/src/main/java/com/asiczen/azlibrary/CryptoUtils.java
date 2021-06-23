package com.asiczen.azlibrary;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class CryptoUtils {
    private Cipher cipher;

    CryptoUtils(byte[] key) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        }
        catch(Exception exp){
            exp.printStackTrace();}
    }
    public byte[] AESEncode(byte[] data)
            throws Exception {

        return cipher.doFinal(data);
    }
}
