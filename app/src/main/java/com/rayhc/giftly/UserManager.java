package com.rayhc.giftly;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class UserManager {
    public static void acceptFriendRequest(User current, String toAcceptUid){
        if(current.getReceivedFriends() == null) return;
        if(!current.getReceivedFriends().containsKey(toAcceptUid)) return;
        current.removeReceivedFriends(toAcceptUid);
        current.addFriends(toAcceptUid);
        new Thread() {
            public void run(){
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                Query query = db.child("users").orderByChild("userId").equalTo(toAcceptUid);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User newUser = new User();
                        if(snapshot.exists()){
                            newUser = UserManager.snapshotToUser(snapshot, toAcceptUid);
                        }
                        newUser.removeSentFriends(current.getUserId());
                        newUser.addFriends(current.getUserId());
                        db.child("users").child(current.getUserId()).setValue(current);
                        db.child("users").child(newUser.getUserId()).setValue(newUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }.start();
    }

    public static void declineFriendRequest(User current, String toRejectUid){
        if(!current.getReceivedFriends().containsKey(toRejectUid)) return;
        current.removeReceivedFriends(toRejectUid);
        new Thread() {
            public void run(){
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                Query query = db.child("users").orderByChild("userId").equalTo(toRejectUid);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User newUser = new User();
                        if(snapshot.exists()){
                            newUser = UserManager.snapshotToUser(snapshot, toRejectUid);
                        }
                        newUser.removeSentFriends(current.getUserId());
                        db.child("users").child(current.getUserId()).setValue(current);
                        db.child("users").child(newUser.getUserId()).setValue(newUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }.start();
    }

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


    public static void sendFriendRequest(User from, String toFriendUid){
        from.addSentFriends(toFriendUid);
        new Thread() {
            public void run() {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                Query query = db.child("users").orderByChild("userId").equalTo(toFriendUid);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User newUser = new User();
                        if(snapshot.exists()){
                            newUser = UserManager.snapshotToUser(snapshot,toFriendUid);
                        } newUser.addReceivedFriends(from.getUserId());
                        db.child("users").child(from.getUserId()).setValue(from);
                        db.child("users").child(newUser.getUserId()).setValue(newUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }.start();
    }

}