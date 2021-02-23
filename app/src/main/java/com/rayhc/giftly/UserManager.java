package com.rayhc.giftly;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class UserManager {
    public static User snapshotToEmptyUser(DataSnapshot snapshot, FirebaseUser user){
        User tempUser = new User();
        if(user.getDisplayName() != null) tempUser.setName(user.getDisplayName());
        if(user.getEmail() != null) tempUser.setEmail(user.getEmail());
        if(user.getPhotoUrl() != null) tempUser.setPhotoUri(user.getPhotoUrl().toString());
        tempUser.setEmailVerified(user.isEmailVerified());
        tempUser.setUserId(user.getUid());

        tempUser.setReceivedGifts(new HashMap<>());
        tempUser.setSentGifts(new HashMap<>());
        tempUser.setReceivedFriends(new HashMap<>());
        tempUser.setSentFriends(new HashMap<>());
        tempUser.setFriends(new HashMap<>());

        new Thread(){
            public void run(){
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                db.child("users").child(tempUser.getUserId()).setValue(tempUser);
            }
        }.start();

        return tempUser;
    }

    public static User snapshotToUser(DataSnapshot snapshot, String uid){
        DataSnapshot storedUser = snapshot.child(uid);
        return storedUser.getValue(User.class);
    }

    public static void sendGift(User from, User to, Gift gift){
        from.addSentGifts(to.getUserId());
        to.addReceivedGifts(from.getUserId());
        gift.setReceiver(to.getUserId());
        gift.setSender(from.getUserId());
        if(gift.getTimeCreated() != 0) {
            gift.setTimeCreated(System.currentTimeMillis());
        }
        new Thread() {
            public void run(){
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                db.child("users").child(from.getUserId()).setValue(from);
                db.child("users").child(to.getUserId()).setValue(to);
                db.child("gifts").child(gift.getReceiver()).child(String.valueOf(gift.getTimeCreated())).setValue(gift);
            }
        }.start();
    }

    public static void addFriend(User from, User toFriend){
        from.addSentFriends(toFriend.getUserId());
        toFriend.addReceivedFriends(from.getUserId());
        new Thread() {
            public void run() {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                db.child("users").child(from.getUserId()).setValue(from);
                db.child("users").child(toFriend.getUserId()).setValue(toFriend);
            }
        }.start();
    }

}
