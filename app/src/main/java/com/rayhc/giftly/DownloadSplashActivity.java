package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is loading splash screen for when the gift data is being download from the cloud
 *
 * When a user opens a gift, this will show as the gift is being read from the cloud
 *
 * At the end, user will be directed to the ReviewGiftActivity
 */
public class DownloadSplashActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private FirebaseStorage mStorage;

    private String recipientName, recipientID, hashValue, userID;
    private HashMap<String, String> friendMap;


    private Gift mGift;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_splash);

        //storage stuff
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //recipient and hash
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        recipientName = startIntent.getStringExtra("FRIEND NAME");
        recipientID = startIntent.getStringExtra("FRIEND ID");
        //if its getting friends
        if(startIntent.getBooleanExtra("GET FRIENDS", false)){
            userID = startIntent.getStringExtra("USER ID");
            friendMap = new HashMap<>();

            Intent intent = new Intent(this, ChooseFriendActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            GetFriendsThread getFriendsThread = new GetFriendsThread(intent);
            getFriendsThread.start();

        }
        //if its getting sent & received gifts
        else if(startIntent.getBooleanExtra("GET GIFTS", false)){
            userID = startIntent.getStringExtra("USER ID");
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("GOT GIFTS", true);
            GetSentGiftsThread sentGiftsThread = new GetSentGiftsThread(intent);
            sentGiftsThread.start();
        }
        //if its getting a gift
        else{
            recipientID = startIntent.getStringExtra("RECIPIENT ID");
            hashValue = startIntent.getStringExtra("HASH VALUE");

            Intent intent = new Intent(this, ReviewGiftActivity.class);
            GiftDownloaderThread storageLoaderThread = new GiftDownloaderThread(intent);
            storageLoaderThread.start();
        }


    }


    /**
     * Thread to store multimedia to the cloud
     */
    public class GiftDownloaderThread extends Thread {
        private Gift loadedGift;
        private Query query;
        private Intent intent;

        public GiftDownloaderThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getGift();
        }


        public void getGift(){
            query = mDatabase.child("gifts").orderByChild(hashValue);

            //listener for the newly added Gift's query based on the input pin
            //put its link at the top
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                    Log.d("LPC", "snapshot: " + snapshot.getValue());
                    if (snapshot.exists()) {
                        loadedGift = snapshot.child(hashValue).getValue(Gift.class);
                        Log.d("LPC", "time loaded gift created "+loadedGift.getTimeCreated());
                        intent.putExtra("OPENED GIFT", loadedGift);
                        intent.putExtra("FROM OPEN", true);
                        handler.post(runnable);
                    } else {
                        showErrorDialog();
                        Log.d("LPC", "snapshot doesn't exist");
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
    /**
     * Populate the spinner with this user's friends names
     */
    public class GetFriendsThread extends Thread{
        private Intent intent;
        private int numFriends = -1;

        public GetFriendsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("LPC", "in runnable - size of friend map: "+friendMap.size());
                if(numFriends == -1 || friendMap.size()>numFriends) return;
                intent.putExtra("FRIEND MAP", friendMap);
                Log.d("LPC", "in runnable - friend map "+friendMap.toString());
                startActivity(intent);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());


        @Override
        public void run() {
            super.run();
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, userID);
                        //DUMMY CODE
                        HashMap<String, String> dummyMap = new HashMap<>();
                        //karim and ian
                        dummyMap.put("pszb1aJGa1YZ5LZBascG7xfbMSI2", "pszb1aJGa1YZ5LZBascG7xfbMSI2");
                        dummyMap.put("2XORnShjizLqK2UZwJb87Z8oi8L2", "2XORnShjizLqK2UZwJb87Z8oi8L2");
                        newUser.setFriends(dummyMap);
                        Log.d("LPC", "set my friends to :"+newUser.getFriends().toString());
                        //get the number of friends this user has
                        numFriends = newUser.getFriends().keySet().size();
                        Log.d("LPC", "num friends: "+numFriends);
                        for(String key: newUser.getFriends().keySet()){
                            String friendID = newUser.getFriends().get(key);
                            Query query = mDatabase.child("users").orderByChild("userId").equalTo(friendID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Log.d("LPC", "inner snapshot: "+snapshot.getValue());
                                    String friendName = (String) snapshot.child(friendID).child("name").getValue();
                                    friendMap.put(friendName, friendID);
                                    handler.post(runnable);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) { }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    /**
     * Get the users sent gifts
     */
    public class GetSentGiftsThread extends Thread{
        private boolean isEmpty;
        private Intent intent;
        private int numSentGifts;
        private ArrayList<String> giftRecipientNames = new ArrayList<>();
        private ArrayList<String> giftMessages = new ArrayList<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> sentGiftMap = new HashMap<>();

        public GetSentGiftsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(isEmpty){
                    intent.putExtra("SENT GIFT MAP", sentGiftMap);
                    Log.d("LPC", "put in an empty sent gift map: ");
                    GetReceivedGiftsThread getReceivedGiftsThread = new GetReceivedGiftsThread(intent);
                    getReceivedGiftsThread.start();
                } else {
                    if (giftMessages.size() > numSentGifts && giftRecipientNames.size() > numSentGifts)
                        return;
                    //make passable strings in form "To *name*: *message*"
                    for (int i = 0; i < numSentGifts; i++) {
                        String label = "To ";
                        label += (giftRecipientNames.get(i) + ": " + giftMessages.get(i));
                        //put in map label -> gift hash
                        sentGiftMap.put(label, giftHashes.get(i));
                    }
                    intent.putExtra("SENT GIFT MAP", sentGiftMap);
                    Log.d("LPC", "thread done - sent gift map: "+sentGiftMap.toString());
                    GetReceivedGiftsThread getReceivedGiftsThread = new GetReceivedGiftsThread(intent);
                    getReceivedGiftsThread.start();
                }
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getSentGifts();
        }

        private void getSentGifts(){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, userID);
//                        //DUMMY CODE
//                        HashMap<String, String> dummyMap = new HashMap<>();
//                        dummyMap.put("xa7JPQsISNQ8RWnCfwuZwJZml9s2", "xa7JPQsISNQ8RWnCfwuZwJZml9s2");
//                        dummyMap.put("c3Vcn0FiA6XElC7PM5BbnFR5hEE2", "c3Vcn0FiA6XElC7PM5BbnFR5hEE2");
//                        newUser.setFriends(dummyMap);
                        Log.d("LPC", "sent gifts thread - is freinds null: "+(newUser.getFriends() == null));
                        if(newUser.getFriends() == null) {
                            isEmpty = true;
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
                                        Log.d("LPC", "inner snapshot: " + snapshot.getValue());
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftRecipientNames.add(friendName);
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
            if(giftHashes.size()>numSentGifts) return;
            //get the gift messages
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gift").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("LPC", "inner snapshot: "+snapshot.getValue());
                        String message = (String) snapshot.child(hashValue).child("message").getValue();
                        giftMessages.add(message);
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
        private boolean isEmpty;
        private int numReceivedGifts;
        private ArrayList<String> giftSenderNames = new ArrayList<>();
        private ArrayList<String> giftMessages = new ArrayList<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> receivedGiftsMap = new HashMap<>();

        public GetReceivedGiftsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isEmpty) {
                    if (giftMessages.size() > numReceivedGifts && giftSenderNames.size() > numReceivedGifts)
                        return;
                    //make passable strings in form "From *name*: *message*"
                    for (int i = 0; i < numReceivedGifts; i++) {
                        String label = "From ";
                        label += (giftSenderNames.get(i) + ": " + giftMessages.get(i));
                        //put in map label -> gift hash
                        receivedGiftsMap.put(label, giftHashes.get(i));
                    }
                }
                intent.putExtra("RECEIVED GIFT MAP", receivedGiftsMap);
                Log.d("LPC", "thread done-received gift map: "+receivedGiftsMap.toString());
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
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, userID);
//                        //DUMMY CODE
//                        HashMap<String, String> dummyMap = new HashMap<>();
//                        dummyMap.put("xa7JPQsISNQ8RWnCfwuZwJZml9s2", "xa7JPQsISNQ8RWnCfwuZwJZml9s2");
//                        dummyMap.put("c3Vcn0FiA6XElC7PM5BbnFR5hEE2", "c3Vcn0FiA6XElC7PM5BbnFR5hEE2");
//                        newUser.setFriends(dummyMap);
                        //get the number of received gifts this user has
                        if(newUser.getReceivedFriends() == null) {
                            isEmpty = true;
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
                                        Log.d("LPC", "inner snapshot: " + snapshot.getValue());
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftSenderNames.add(friendName);
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
            if(giftHashes.size()>numReceivedGifts) return;
            //get the gift messages
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gift").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("LPC", "inner snapshot: "+snapshot.getValue());
                        String message = (String) snapshot.child(hashValue).child("message").getValue();
                        giftMessages.add(message);
                        handler.post(runnable);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }
    }

    /**
     * Error pop-up for bad queries
     */
    public void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(DownloadSplashActivity.this);
        builder.setMessage("There has been an error. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}