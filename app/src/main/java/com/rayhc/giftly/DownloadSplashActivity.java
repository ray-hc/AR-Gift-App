package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.rayhc.giftly.util.UserManager;
import com.rayhc.giftly.util.User;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;
import java.util.HashMap;

import static com.rayhc.giftly.util.Globals.GOT_GIFTS_KEY;
import static com.rayhc.giftly.util.Globals.REC_MAP_KEY;
import static com.rayhc.giftly.util.Globals.SENT_MAP_KEY;

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

    private String recipientName, recipientID, hashValue, userID, giftHash, label;
    private HashMap<String, String> friendMap, sentGiftMap, receivedGiftMap;
    private boolean fromOpen, fromReceive;



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
        userID = startIntent.getStringExtra("USER ID");
        fromOpen = startIntent.getBooleanExtra("FROM OPEN", false);
        giftHash = startIntent.getStringExtra("HASH VALUE");
        label = startIntent.getStringExtra("LABEL");
        fromReceive = startIntent.getBooleanExtra("FROM RECEIVED", false);
        //if its getting friends
        if(startIntent.getBooleanExtra("GET FRIENDS", false)){
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
            Intent intent = new Intent(this, MainActivity.class);
            GetSentGiftsThread sentGiftsThread = new GetSentGiftsThread(intent);
            sentGiftsThread.start();

        }
        //if its getting a gift
        else{
            Log.d("LPC", "running gift downloader thread");
            hashValue = startIntent.getStringExtra("HASH VALUE");
            Log.d("LPC", "getting gift w hash: "+hashValue);
            Log.d("LPC", "running gift downloader thread: from open? "+ fromOpen);

            GiftDownloaderThread giftDownloaderThread = new GiftDownloaderThread();
            giftDownloaderThread.start();
        }


    }


    /**
     * Thread to store multimedia to the cloud
     */
    public class GiftDownloaderThread extends Thread {
        private Gift loadedGift;
        private Query query;
        private Intent intent;
        private boolean isReceived, wasOpened;
        String friendName;

        public GiftDownloaderThread(){
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //TODO: change the 'else' to the AR activity
                if(wasOpened) intent = new Intent(getApplicationContext(), CreateGiftActivity.class);
                else intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                intent.putExtra("sceneType", loadedGift.getGiftType());
//                intent.putExtra("OPENED GIFT", loadedGift);
                intent.putExtra("FROM OPEN", true);
                intent.putExtra("HASH VALUE", hashValue);
                intent.putExtra("LABEL", label);
                intent.putExtra(Globals.CURR_GIFT_KEY, loadedGift);
                Log.d("LPC", "runnable gift download get friend name: "+friendName);
                intent.putExtra("FRIEND NAME", friendName);
                intent.putExtra("IS RECEIVED", isReceived);
                intent.putExtra("WAS OPENED", wasOpened);
                if(!wasOpened) {
                    Log.d("LPC", "marking gift as opened in db from get gift thread");
                    MarkOpenedThread markOpenedThread = new MarkOpenedThread(intent, hashValue);
                    markOpenedThread.start();
                }
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
            query = mDatabase.child("gifts").child(hashValue);

            //listener for the newly added Gift's query based on the input pin
            //put its link at the top
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                    Log.d("LPC", "snapshot: " + snapshot.getValue());
                    if (snapshot.exists()) {
                        loadedGift = snapshot.getValue(Gift.class);
                        //TODO: I think this will stop senders from being shown gift contents upon recipient open
                        if(loadedGift.getSender().equals(userID)) {
                            Log.d("LPC", "i sent this gift");
                            return;
                        }
                        if(fromReceive) wasOpened = loadedGift.isOpened();
                        else wasOpened = true;
                        Log.d("LPC", "time loaded gift created "+loadedGift.getTimeCreated());
                        if(loadedGift.getSender().equals(userID)) getFriendName(loadedGift.getReceiver());
                        else {
                            getFriendName(loadedGift.getSender());
                            isReceived = true;
                        }
                    } else {
                        showErrorDialog();
                        Log.d("LPC", "snapshot doesn't exist");
                    }
                    query.removeEventListener(this);
                    Log.d("LPC","get gift listener was removed");
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        public void getFriendName(String id){
            query = mDatabase.child("users").child(id);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                    User user;
                    if (snapshot.exists()) {
                        friendName = (String) snapshot.child("name").getValue();
                        handler.post(runnable);
                    } else {
                        showErrorDialog();
                        Log.d("LPC", "snapshot doesn't exist");
                    }
                    query.removeEventListener(this);
                    Log.d("LPC","get friend name listener was removed");
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    /**
     * Thread to mark a gift as opened
     */
    public class MarkOpenedThread extends Thread{
        private Intent intent;
        private String giftHash;
        public MarkOpenedThread(Intent intent, String giftHash){
            this.intent = intent;
            this.giftHash = giftHash;
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
            mDatabase.child("gifts").child(giftHash).child("opened").setValue(true,
                    new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            handler.post(runnable);
                        }
                    });
        }
    }
    /**
     * Populate the spinner with this user's friends names
     */
    public class GetFriendsThread extends Thread{
        private Intent intent;
        private int numFriends = 0;

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
                        //get the number of friends this user has
                        numFriends = newUser.getFriends().keySet().size();
                        Log.d("LPC", "num friends: "+numFriends);
                        if(numFriends == 0){
                            handler.post(runnable);
                        }
                        for(String key: newUser.getFriends().keySet()){
                            String friendID = newUser.getFriends().get(key);
                            Query query = mDatabase.child("users").orderByChild("userId").equalTo(friendID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
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
        private Intent intent;
        private int numSentGifts = 0;
        private ArrayList<String> giftRecipientNames = new ArrayList<>();
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
                for (String hash : giftMsgMap.keySet()) {
                    String label = giftMsgMap.get(hash);
                    //put in map label -> gift hash
                    sentGiftMap.put(label, hash);
                }
                intent.putExtra(Globals.SENT_MAP_KEY, sentGiftMap);
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
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, userID);
                        if(newUser.getSentGifts() == null) {
                            handler.post(runnable);
                        } else {
                            //get the number of sent gifts this user has
                            numSentGifts = newUser.getSentGifts().keySet().size();
                            Log.d("LPC", "num sentGifts: " + numSentGifts);
                            giftHashes = new ArrayList<>(newUser.getSentGifts().keySet());
                            Log.d("LPC", "gift hashes when getting name: "+giftHashes.toString());
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
            Log.d("LPC", "gift hashes when getting msgs: "+giftHashes.toString());
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        String displayText = giftMsgMap.get(hash)+"|"+message;
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
        private int numReceivedGifts = 0;
        private ArrayList<String> giftSenderNames = new ArrayList<>();
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
                for (String hash : giftMsgMap.keySet()) {
                    String label = giftMsgMap.get(hash);
                    //put in map label -> gift hash
                    receivedGiftsMap.put(label, hash);
                }

                intent.putExtra(Globals.REC_MAP_KEY, receivedGiftsMap);
                intent.putExtra("GOT GIFTS", true);
                Log.d("LPC", "thread done-received gift map: " + receivedGiftsMap.toString());
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
            if(giftSenderNames.size()<numReceivedGifts) return;
            //get the gift messages
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        boolean opened = false;
                        if(snapshot.child(hash).child("opened").getValue() != null)
                            opened = (boolean) snapshot.child(hash).child("opened").getValue();
                        if(opened) message += "OLD";
                        else message += "NEW";
                        String displayText = giftMsgMap.get(hash)+"|"+message;
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
