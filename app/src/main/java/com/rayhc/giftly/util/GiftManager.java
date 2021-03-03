package com.rayhc.giftly.util;

import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class GiftManager {

    /**
     * generates cryptographic key based on a given pin
     * @param pin
     * @return
     */
    public static SecretKey generateKey(String pin){
        return new SecretKeySpec(pin.getBytes(), "AES");
    }

    /**
     * encrypts the gift's contents: link and message based on given pin
     * @param gift
     * @param pin
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static Gift encryptGift(Gift gift, String pin) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(pin));
        byte[] messageEncrypted = cipher.doFinal(gift.getMessage().getBytes(StandardCharsets.UTF_8));
//        byte[] linkEncrypted = cipher.doFinal(gift.getLink().getBytes(StandardCharsets.UTF_8));
        gift.setMessage(new String(messageEncrypted, StandardCharsets.UTF_8));
//        gift.setLink(new String(linkEncrypted, StandardCharsets.UTF_8));
        return gift;
    }

    /**
     * decrypts a given gift based on a pin
     * @param gift
     * @param pin
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static Gift decryptGift(Gift gift, String pin) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, generateKey(pin));
        gift.setMessage(new String(cipher.doFinal(gift.getMessage().getBytes()), StandardCharsets.UTF_8));
//        gift.setLink(new String(cipher.doFinal(gift.getLink().getBytes()), StandardCharsets.UTF_8));
        return gift;
    }
}
