package com.rayhc.giftly;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * WRAPPER OBJECT FOR FIREBASE USERS
 *  - why?
 *      - because we need to save additional metadata information
 */
public class User implements Serializable {
    private String id;
    private String userId;
    // Apparently maps with non string keys are not supported
    // also using arrays is not encouraged because they get stored as a map anyways
    private Map<String, String> receivedGifts;
    private Map<String, String> sentGifts;
    private Map<String, String> receivedFriends;
    private Map<String, String> sentFriends;
    private Map<String, String> friends;

    private String name;
    private String email;
    private String photoUri;
    boolean emailVerified;


    public User(){}

    public String toString(){
        String ret = "";
        ret += "Name: " + this.name;
        ret += "\nEmail: " + this.email;
        if(this.receivedGifts != null)  ret += "\nlen rec gifts: " + this.receivedGifts.size();
        if(this.sentGifts!= null) ret += "\nlen sent gifts: " + this.sentGifts.size();
        return ret;
    }

    /*
    public void deepCopy(User user){
        this.userId = user.getUserId();
        this.receivedGifts = user.getReceivedGifts();
        this.sentGifts = user.getSentGifts();
        this.receivedFriends = user.getReceivedFriends();
        this.sentFriends = user.getSentFriends();
        this.friends = user.getFriends();
        this.name = user.getName();
        this.email = user.getEmail();
        this.photoUri = user.getPhotoUri();
        this.emailVerified = user.isEmailVerified();
    }
     */

    public User(String id, String userId, Map<String, String> receivedGifts, Map<String, String> sentGifts, Map<String, String> receivedFriends, Map<String, String> sentFriends, Map<String, String> friends, String name, String email, String photoUri, boolean emailVerified) {
        this.id = id;
        this.userId = userId;
        this.receivedGifts = receivedGifts;
        this.sentGifts = sentGifts;
        this.receivedFriends = receivedFriends;
        this.sentFriends = sentFriends;
        this.friends = friends;
        this.name = name;
        this.email = email;
        this.photoUri = photoUri;
        this.emailVerified = emailVerified;
    }

    public void addFriends(String uid){
        if(this.friends == null) this.friends = new HashMap<>();
        this.friends.put(uid, uid);
    }

    public void removeFriends(String uid){
        if(this.friends == null) return;
        this.friends.remove(uid);
    }

    public void addSentGifts(Gift gift){
        if(this.sentGifts == null) this.sentGifts = new HashMap<>();
        this.sentGifts.put(gift.getHashValue(), gift.getReceiver());
    }

    public void addSentGifts(String uid){
        if(this.sentGifts == null) this.sentGifts = new HashMap<>();
        this.sentGifts.put(uid, uid);
    }

    public void addSentFriends(String uid){
        if(this.sentFriends == null) this.sentFriends = new HashMap<>();
        this.sentFriends.put(uid, uid);
    }

    public void addReceivedGifts(Gift gift){
        if(this.receivedGifts == null) this.receivedGifts = new HashMap<>();
        this.receivedGifts.put(gift.getHashValue(), gift.getSender());
    }

    public void addReceivedGifts(String uid){
        if(this.receivedGifts == null) this.receivedGifts = new HashMap<>();
        this.receivedGifts.put(uid, uid);
    }

    public void addReceivedFriends(String uid){
        if(this.receivedFriends == null) this.receivedFriends = new HashMap<>();
        this.receivedFriends.put(uid, uid);
    }

    public void removeSentGifts(String uid){
        if(this.sentGifts == null) return;
        this.sentGifts.remove(uid);
    }

    public void removeSentFriends(String uid){
        if(this.sentFriends == null) return;
        this.sentFriends.remove(uid);
    }

    public void removeReceivedGifts(String uid){
        if(this.receivedGifts == null) return;
        this.receivedGifts.remove(uid);
    }

    public void removeReceivedFriends(String uid){
        if(this.receivedFriends== null) return;
        this.receivedFriends.remove(uid);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, String> getReceivedGifts() {
        return receivedGifts;
    }

    public void setReceivedGifts(Map<String, String> receivedGifts) {
        this.receivedGifts = receivedGifts;
    }

    public Map<String, String> getSentGifts() {
        return sentGifts;
    }

    public void setSentGifts(Map<String, String> sentGifts) {
        this.sentGifts = sentGifts;
    }

    public Map<String, String> getReceivedFriends() {
        if(receivedFriends == null) return new HashMap<String, String>();
        return receivedFriends;
    }

    public void setReceivedFriends(Map<String, String> receivedFriends) {
        this.receivedFriends = receivedFriends;
    }

    public Map<String, String> getSentFriends() {
        return sentFriends;
    }

    public void setSentFriends(Map<String, String> sentFriends) {
        this.sentFriends = sentFriends;
    }

    public Map<String, String> getFriends() {
        if(friends == null) return new HashMap<String, String>();
        return friends;
    }

    public void setFriends(Map<String, String> friends) {
        this.friends = friends;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}