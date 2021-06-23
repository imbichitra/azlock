/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asiczen.azlock.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private Cipher cipher;

    public CryptoUtils(byte[] key) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        }
        catch(Exception exp){exp.printStackTrace();}
    }
    public CryptoUtils(){}
    public byte[] AESEncode(byte[] data)
            throws Exception {

        return cipher.doFinal(data);
    }
    public void generateKey(byte[] key){
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        }
        catch(Exception exp){exp.printStackTrace();}
    }

}
