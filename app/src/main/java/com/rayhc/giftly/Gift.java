//TODO: change so it matches the rest of the project
package com.rayhc.giftly;

import android.net.Uri;

/**
 * Gift objects to be stored in the database
 * These get configured to JSON objects in the database
 */
public class Gift {
    //attributes
    private String id;
    private String link;    //nulled out if not sending a link
    private Uri file;       //nulled out if not sending a file
    private int contentType;    //i think it'll be something like "1" is a link, "2" is a multimedia file, etc.
    private int giftType;       //follows the same principle as contentType
    private String sender;
    private String receiver;
    private boolean isEncrypted;
    private String hashValue;
    private String qrCode;
    private boolean opened;

    /**
     * Default Constructor
     */
    public Gift(){}

    /**
     * Value constructor
     */
    public Gift(String id, String link, Uri file, int contentType, int giftType, String sender, String receiver,
                boolean isEncrypted, String hashValue, String qrCode, boolean opened){
        this.id = id;
        this.link = link;
        this.file = file;
        this.contentType = contentType;
        this.giftType = giftType;
        this.sender = sender;
        this.receiver = receiver;
        this.isEncrypted = isEncrypted;
        this.hashValue = hashValue;
        this.qrCode = qrCode;
        this.opened = opened;
    }

    //TODO: Probably need an intermediate constructor that doesn't take every parameter?

    /**
     * Getters & Setters
     */
    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public Uri getFile() {
        return file;
    }

    public int getContentType() {
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
        return hashValue;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setFile(Uri file) {
        this.file = file;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public void setGiftType(int giftType) {
        this.giftType = giftType;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
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
}
