package com.rayhc.giftly.util;

import android.util.Log;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Gift objects to be stored in the database
 * These get configured to JSON objects in the database
 *
 * Will look like this in the DB:
 * gifts:
 * - *user id of recipient*
 * - - *all of the gift data*
 *
 */
public class Gift implements Serializable {
    //public keys
    public static final int ADD_LINK_GIFT_KEY = 1;
    public static final int ADD_IMAGE_GIFT_KEY = 2;
    public static final int ADD_VIDEO_GIFT_KEY = 3;


    //attributes
    private HashMap<String, String> links;
    private HashMap<String, String> contentType;
    private int giftType;
    private String sender;
    private String receiver;
    private String message;
    private boolean isEncrypted;
    private long timeCreated;
    private long timeOpened;
    private String hashValue;
    private String qrCode;
    private boolean opened;


    /**
     * Default Constructor
     */
    public Gift() {
    }

    /**
     * Value constructor
     */
    public Gift(HashMap<String, String> links, HashMap<String, String> contentType, int giftType,
                String sender, String receiver, String message, long timeOpened, long timeCreated,
                boolean isEncrypted, String hashValue, String qrCode, boolean opened) {
        this.links = links;
        this.contentType = contentType;
        this.giftType = giftType;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.timeOpened = timeOpened;
        this.timeCreated = timeCreated;
        this.isEncrypted = isEncrypted;
        this.hashValue = hashValue;
        this.qrCode = qrCode;
        this.opened = opened;
    }



    /**
     * Getters & Setters
     */

    public HashMap<String, String> getLinks() { return links; }

    public HashMap<String, String> getContentType() {
        return contentType;
    }

    public int getGiftType() {
        return giftType;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public long getTimeOpened() {
        return timeOpened;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public boolean isOpened() {
        return opened;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getHashValue() {
        if(hashValue == null) hashValue = createHashValue();
        return hashValue;
    }



    public void setLinks(HashMap<String, String> links) { this.links = links; }

    public void setContentType(HashMap<String, String> contentType) { this.contentType = contentType; }

    public void setGiftType(int giftType) {
        this.giftType = giftType;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public void setTimeOpened(long timeOpened) {
        this.timeOpened = timeOpened;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    /**
     * Create MD5 hash from sender and time created
     */
    public String createHashValue() {
        String base =  timeCreated +
                " " + sender;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5 = messageDigest.digest(base.getBytes());
            BigInteger no = new BigInteger(1, md5);
            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLink(String link){
        int size = getContentType().size();
        String newKey = "link_ "+System.currentTimeMillis();
        getLinks().put(newKey, link);
    }


    public void addContentType(String fileName){
        String newValue = "";
        if(fileName.startsWith("image")) newValue = "image";
        else if(fileName.startsWith("video")) newValue = "video";
        getContentType().put(fileName, newValue);
    }

    //for testing purposes
    @Override
    public String toString() {
        return "Gift From " + sender;
    }
}
