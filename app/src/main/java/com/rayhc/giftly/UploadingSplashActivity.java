package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This is loading splash screen for the gift is being saved to the cloud
 * It will show a loading screen and run a thread to store the data in the background
 * Once the data is successfully uploaded, the user will be taken back to the home page
 */
public class UploadingSplashActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private FirebaseStorage mStorage;


    private boolean mFromReview;
    private String mFileLabel;
    private Gift mGift;
    private String fromID, toID;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading_splash);

        //storage stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //data from create gift fragment
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        fromID = startIntent.getStringExtra("FROM USER ID");
        toID = startIntent.getStringExtra("TO USER ID");


        //start a thread to upload media to cloud
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("GOT GIFTS", true);
        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(mGift, intent);
        storageLoaderThread.start();

    }


    /**
     * Thread to store multimedia to the cloud
     */
    public class StorageLoaderThread extends Thread {

        private Gift saveGift;
        private Intent intent;

        public StorageLoaderThread(Gift gift, Intent intent){
            saveGift = gift;
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                GetSentGiftsThread getSentGiftsThread = new GetSentGiftsThread(intent);
                getSentGiftsThread.start();
            }
        };


        Handler handler = new Handler(Looper.getMainLooper());


        @Override
        public void run() {
            Log.d("LPC", "media thread start");
            //upload the strings first
            Log.d("LPC", "save gift hash: "+saveGift.getHashValue());
//            mDatabase.child("gifts").child(saveGift.getHashValue()).setValue(saveGift);
//            Log.d("LPC", "wrote gift to the rt DB");

            //send the gift
            sendGift(fromID, toID);

        }
        /**
         * First part of process to send gift
         */
        public void sendGift(String fromID, String toID){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(fromID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User fromUser = new User();
                    if(snapshot.exists()){
                        fromUser = UserManager.snapshotToUser(snapshot, fromID);
                        addRecipient(toID, fromUser);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }

        /**
         * Second part of process to send gift
         */
        public void addRecipient(String toID, User fromUser){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(toID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User toUser = new User();
                    if(snapshot.exists()){
                        toUser = UserManager.snapshotToUser(snapshot, toID);
                        mGift.setReceiver(toID);
                        Log.d("LPC", "sendGift: gift sender: "+mGift.getSender());
                        Log.d("LPC", "sendGift: gift time create: "+mGift.getTimeCreated());
                        fromUser.addSentGifts(mGift);
                        toUser.addReceivedGifts(mGift);
                        User finalToUser = toUser;
                        new Thread() {
                            public void run(){
                                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                                db.child("users").child(fromID).setValue(fromUser);
                                db.child("users").child(toID).setValue(finalToUser);
                                db.child("gifts").child(mGift.getHashValue()).setValue(mGift,
                                        new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                //now upload media
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
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }


        public void uploadFile(int index, ArrayList<String> keys){
            Log.d("LPC", "uploadFile: called");
            String fileName;
            String path;
            Uri selectedData;
            if(index == keys.size()) {
                Log.d("LPC", "uploadFile: switched the flag to false");
                handler.post(runnable);
                return;
            }
            if(index<keys.size()) {
                for (String key : saveGift.getContentType().keySet()) {
                    selectedData = Uri.parse(saveGift.getContentType().get(key));
                    fileName = key;
                    if (selectedData.toString().contains("image"))
                        path = "gift/" + saveGift.getHashValue() + "/" + fileName + ".jpg";
                    else path = "gift/" + saveGift.getHashValue() + "/" + fileName + ".mp4";
                    StorageReference giftRef = storageRef.child(path);
                    UploadTask uploadTask = giftRef.putFile(selectedData);
                    uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d("LPC", "media upload complete!");
                                uploadFile(index+1, keys);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Get the users sent gifts
     */
    public class GetSentGiftsThread extends Thread{
        private Intent intent;
        private int numSentGifts;
        private ArrayList<String> giftRecipientNames = new ArrayList<>();
        //        private ArrayList<String> giftMessages = new ArrayList<>();
        private HashMap<String, String> giftMsgMap = new HashMap<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> sentGiftMap = new HashMap<>();

        public GetSentGiftsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (giftMsgMap.size() < numSentGifts) {
                    Log.d("LPC", "sent gifts handler didnt run");
                    return;
                }
                //make passable strings in form "To: *name* - *message*"
                Log.d("LPC", "sent gift msg map: " + giftMsgMap.toString());
//                ArrayList<String> msgList = new ArrayList<>(giftMsgMap.keySet());
                for (String hash : giftMsgMap.keySet()) {
                    String label = "To: "+giftMsgMap.get(hash);
//                    if (giftMessages.get(i) == null) label += giftRecipientNames.get(i);
//                    else label += (giftRecipientNames.get(i) + " - " + giftMessages.get(i));
                    //put in map label -> gift hash
                    sentGiftMap.put(label, hash);
                }
                intent.putExtra("SENT GIFT MAP", sentGiftMap);
                Log.d("LPC", "thread done - sent gift map: " + sentGiftMap.toString());
                GetReceivedGiftsThread getReceivedGiftsThread = new GetReceivedGiftsThread(intent);
                getReceivedGiftsThread.start();

            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getSentGifts();
        }

        private void getSentGifts(){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(fromID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, fromID);
                        if(newUser.getSentGifts() == null) {
                            handler.post(runnable);
                        } else {
                            //get the number of sent gifts this user has
                            numSentGifts = newUser.getSentGifts().keySet().size();
                            Log.d("LPC", "num sentGifts: " + numSentGifts);
                            giftHashes = new ArrayList<>(newUser.getSentGifts().keySet());
                            for (String key : newUser.getSentGifts().keySet()) {
                                String otherUserID = newUser.getSentGifts().get(key);
                                //get the other user's name
                                Query userNameQuery = mDatabase.child("users").orderByChild("userId").equalTo(otherUserID);
                                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftRecipientNames.add(friendName);
                                        giftMsgMap.put(key, friendName);
                                        getGiftMessages();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
        private void getGiftMessages(){
            if(giftRecipientNames.size()<numSentGifts) return;
            //get the gift messages
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        String displayText = giftMsgMap.get(hash)+" - "+message;
//                        giftMessages.add(message);
                        giftMsgMap.put(hash, displayText);
                        Log.d("LPC", "getting gift with hash: "+hash+" with message: "+message);
                        handler.post(runnable);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }
    }

    /**
     * Get the users received gifts
     */
    public class GetReceivedGiftsThread extends Thread{
        private Intent intent;
        private int numReceivedGifts;
        private ArrayList<String> giftSenderNames = new ArrayList<>();
        //        private ArrayList<String> giftMessages = new ArrayList<>();
        private HashMap<String, String> giftMsgMap = new HashMap<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> receivedGiftsMap = new HashMap<>();

        public GetReceivedGiftsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (giftMsgMap.size() < numReceivedGifts) {
                    Log.d("LPC", "received gifts handler didnt run");
                    return;
                }
                //make passable strings in form "To: *name* - *message*"
                Log.d("LPC", "received gift msg map: " + giftMsgMap.toString());
//                ArrayList<String> msgList = new ArrayList<>(giftMsgMap.keySet());
                for (String hash : giftMsgMap.keySet()) {
                    String label = "From: "+giftMsgMap.get(hash);
//                    if (giftMessages.get(i) == null) label += giftRecipientNames.get(i);
//                    else label += (giftRecipientNames.get(i) + " - " + giftMessages.get(i));
                    //put in map label -> gift hash
                    receivedGiftsMap.put(label, hash);
                }

                intent.putExtra("RECEIVED GIFT MAP", receivedGiftsMap);
                Log.d("LPC", "thread done-received gift map: " + receivedGiftsMap.toString());
                intent.putExtra("SENT GIFT", true);
                startActivity(intent);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getReceivedGifts();
        }

        private void getReceivedGifts(){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(fromID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, fromID);
                        if(newUser.getReceivedGifts() == null) {
                            handler.post(runnable);
                        } else {
                            numReceivedGifts = newUser.getReceivedGifts().keySet().size();
                            Log.d("LPC", "num receivedGifts: " + numReceivedGifts);
                            giftHashes = new ArrayList<>(newUser.getReceivedGifts().keySet());
                            for (String key : newUser.getReceivedGifts().keySet()) {
                                String otherUserID = newUser.getReceivedGifts().get(key);
                                //get the other user's name
                                Query userNameQuery = mDatabase.child("users").orderByChild("userId").equalTo(otherUserID);
                                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftSenderNames.add(friendName);
                                        giftMsgMap.put(key, friendName);
                                        getGiftMessages();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
        public void getGiftMessages(){
            if(giftHashes.size()<numReceivedGifts) return;
            //get the gift messages
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        String displayText = giftMsgMap.get(hash)+" - "+message;
//                        giftMessages.add(message);
                        giftMsgMap.put(hash, displayText);
                        Log.d("LPC", "getting gift with hash: "+hash+" with message: "+message);
                        handler.post(runnable);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }
    }


}