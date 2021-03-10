package com.rayhc.giftly.util;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.UploadingSplashActivity;

import java.util.ArrayList;

/**
 * Thread to store multimedia to the cloud
 */
public class StorageLoaderThread extends Thread {

    private Gift saveGift;
    private MainActivity activity;

    private String fromID, toID;

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private FirebaseStorage mStorage;

    public StorageLoaderThread(Gift gift, MainActivity activity,
                               DatabaseReference mDatabase, StorageReference storageRef,
                               FirebaseStorage mStorage, String fromID, String toID) {
        saveGift = gift;
        this.activity = activity;

        this.mDatabase = mDatabase;
        this.storageRef = storageRef;
        this.mStorage = mStorage;

        this.fromID = fromID;
        this.toID = toID;


    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (activity != null) {
                activity.findViewById(R.id.downloading).setVisibility(View.INVISIBLE);
                activity.updateSent();
            }
        }
    };


    Handler handler = new Handler(Looper.getMainLooper());


    @Override
    public void run() {
        Log.d("rhc", "media thread start");
        //upload the strings first

        //send the gift
        sendGift(fromID, toID);

    }

    /**
     * First part of process to send gift
     */
    public void sendGift(String fromID, String toID) {
        Query query = mDatabase.child("users").orderByChild("userId").equalTo(fromID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User fromUser = new User();
                if (snapshot.exists()) {
                    fromUser = UserManager.snapshotToUser(snapshot, fromID);
                    addRecipient(toID, fromUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Second part of process to send gift
     */
    public void addRecipient(String toID, User fromUser) {
        Query query = mDatabase.child("users").orderByChild("userId").equalTo(toID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User toUser = new User();
                if (snapshot.exists()) {
                    toUser = UserManager.snapshotToUser(snapshot, toID);
                    saveGift.setReceiver(toID);
                    Log.d("LPC", "sendGift: gift sender: " + saveGift.getSender());
                    Log.d("LPC", "sendGift: gift time create: " + saveGift.getTimeCreated());
                    fromUser.addSentGifts(saveGift);
                    toUser.addReceivedGifts(saveGift);
                    User finalToUser = toUser;
                    new Thread() {
                        public void run() {
                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                            db.child("users").child(fromID).setValue(fromUser);
                            db.child("users").child(toID).setValue(finalToUser);
                            db.child("gifts").child(saveGift.getHashValue()).setValue(saveGift,
                                    new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            //now upload media
                                            Log.d("LPC", "save gift hash: " + saveGift.getHashValue());
                                            ArrayList<String> keys = new ArrayList<>(saveGift.getContentType().keySet());
                                            int index = 0;
                                            uploadFile(index, keys);
                                        }
                                    });

                        }
                    }.start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void uploadFile(int index, ArrayList<String> keys){
            Log.d("LPC", "uploadFile: called");
            String fileName;
            String path;
            String selectedData;
            if(index == keys.size()) {
                Log.d("LPC", "uploadFile: switched the flag to false");
                handler.post(runnable);
                return;
            }
            if(index<keys.size()) {
                for (String key : saveGift.getContentType().keySet()) {
                    selectedData = (saveGift.getContentType().get(key));
                    fileName = key;
                    if (selectedData.contains("image"))
                        path = "gift/" + saveGift.getHashValue() + "/" + fileName + ".jpg";
                    else path = "gift/" + saveGift.getHashValue() + "/" + fileName + ".mp4";
                    Log.d("LPC", "media upload file path: "+path);
                    StorageReference giftRef = storageRef.child(path);
                    UploadTask uploadTask = giftRef.putFile(Uri.fromFile(new File(selectedData)));
                    uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d("LPC", "media upload complete!");
                                uploadFile(index+1, keys);
                            } else{
                                Log.d("LPC", "media upload failed: ");
                            }
                        }
                    });
                }
            }
        }
}
