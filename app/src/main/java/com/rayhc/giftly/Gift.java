package com.rayhc.giftly;

import java.io.Serializable;
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
 * Gifts will go through "validation" as follows:
 * 1. the gift being opened was actually sent to the user (matching user id of recipient)
 * 2. double checking the sender & recipient ID's match
 *
 * new edit
 */
public class Gift implements Serializable {
    //public keys
    public static final int ADD_LINK_GIFT_KEY = 1;
    public static final int ADD_IMAGE_GIFT_KEY = 2;
    public static final int ADD_VIDEO_GIFT_KEY = 3;


    //attributes
//    private String id;                                  //synonymous to pin
    private HashMap<String, String> links;                                //nulled out if not sending a link
    private HashMap<String, String> contentType;        /*i think it'll be something like "1" is a link,
                                                        "2" is a multimedia file, etc.
                                                        Need to be a map of strings (but will hold ints) for sending multiple
                                                        "gifts" (many images, some images and some videos, etc.) in one gift object*/

    private HashMap<String, String> giftType;           //follows the same principle as contentType
    private String sender;                              //user id of the gift's sender from firebase authentication
    private String receiver;                            //user id of the gift's sender from firebase authentication
    private String message;
    private boolean isEncrypted;
    private long timeCreated;
    private long timeOpened;
    private String hashValue;
    private String qrCode;
    private boolean opened;                             //look at this value when opening the app + unopened gift page

    // comment by ray: :)
    // comment by uhuru (:

    /**
     * Default Constructor
     */
    public Gift() {
    }

    /**
     * Value constructor
     */
    public Gift(HashMap<String, String> links, HashMap<String, String> contentType, HashMap<String, String> giftType,
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

    //TODO: Probably need an intermediate constructor that doesn't take every parameter?
    //RESOLVED: just use intermediate setters as the gift is being built up


    /**
     * Getters & Setters
     */
//    public String getId() {
//        return id;
//    }

    public HashMap<String, String> getLinks() { return links; }

    public HashMap<String, String> getContentType() {
        return contentType;
    }

    public HashMap<String, String> getGiftType() {
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

//    public void setId(String id) {
//        this.id = id;
//    }


    public void setLinks(HashMap<String, String> links) { this.links = links; }

    public void setContentType(HashMap<String, String> contentType) { this.contentType = contentType; }

    public void setGiftType(HashMap<String, String> giftType) {
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

    public String createHashValue() {
        String base = "timeCreated=" + timeCreated +
                ", sender=" + sender;
        String res = "";
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.digest(base.getBytes());
            byte[] md5 = messageDigest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : md5) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            res = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return res;
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
        /*return "Gift{" +
                "id='" + id + '\'' +
                ", link='" + link + '\'' +
                ", file=" + file +
                ", contentType=" + contentType +
                ", giftType=" + giftType +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", isEncrypted=" + isEncrypted +
                ", hashValue='" + hashValue + '\'' +
                ", qrCode='" + qrCode + '\'' +
                ", opened=" + opened +
                '}';*/
    }
}
