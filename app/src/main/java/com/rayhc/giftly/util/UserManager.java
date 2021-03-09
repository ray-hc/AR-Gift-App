package com.rayhc.giftly.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.android.gms.tasks.Tasks.await;

public class UserManager {
    /**
     * EXAMPLE USAGE
     *             Thread thread = new Thread(() -> {
     *                 try {
     *                     UserManager.searchUsersByEmail("iank");
     *                 } catch (Exception e) {
     *                     e.printStackTrace();
     *                 }
     *             });
     *             thread.start();
     * @param email
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static List<User> searchUsersByEmail(String email) throws ExecutionException, InterruptedException {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        Query query = db.child("users").orderByChild("email").startAt(email).endAt(email+"\uf8ff");
        Task<DataSnapshot> task = query.get();
        await(task);
        DataSnapshot ds = task.getResult();
        List<User> list = new ArrayList<>();
        if(ds == null) return null;
        for(DataSnapshot child : ds.getChildren()) {
            String uid = (String) child.child("userId").getValue();
            list.add(snapshotToUser(ds, uid));
        }
        return list;
    }

    public static void removeFriend(User current, String toRemoveId){
        current.removeFriends(toRemoveId);
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        Query query = db.child("users").orderByChild("userId").equalTo(toRemoveId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                User newUser = new User();
                if(snapshot.exists()){
                    newUser = UserManager.snapshotToUser(snapshot, toRemoveId);
                }
                newUser.removeFriends(current.getUserId());
                db.child("users").child(current.getUserId()).setValue(current);
                db.child("users").child(newUser.getUserId()).setValue(newUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

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
        if(user.getEmail() != null){
            tempUser.setEmail(user.getEmail());
            if(user.getDisplayName() == null) tempUser.setName(user.getEmail());
        }
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
                        } if(!newUser.getFriends().containsKey(from.getUserId()) && !newUser.getReceivedFriends().containsKey(from.getUserId())) {
                            newUser.addReceivedFriends(from.getUserId());
                            db.child("users").child(from.getUserId()).setValue(from);
                            db.child("users").child(newUser.getUserId()).setValue(newUser);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }.start();
    }

    public static void sendAndAcceptFriendRequest(User from, User to){
        from.addSentFriends(to.getUserId());
        new Thread() {
            public void run() {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                if (!to.getFriends().containsKey(from.getUserId()) && !to.getReceivedFriends().containsKey(from.getUserId())) {
                    to.addReceivedFriends(from.getUserId());
                    db.child("users").child(from.getUserId()).setValue(from);
                    UserManager.acceptFriendRequest(to, from.getUserId());
                }
            }
        }.start();
    }

}