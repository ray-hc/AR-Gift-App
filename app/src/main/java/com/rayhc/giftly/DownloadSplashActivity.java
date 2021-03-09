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

    private String recipientName, recipientID, userID, giftHash, label;
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

            Intent intent = new Intent(this, ChooseFriendActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            GetFriendsThread getFriendsThread = new GetFriendsThread(intent);
            getFriendsThread.start();

        }
        //if its getting a gift
        else{
            Log.d("LPC", "running gift downloader thread");
            Log.d("LPC", "getting gift w hash: "+giftHash);
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
                if(wasOpened) intent = new Intent(getApplicationContext(), CreateGiftActivity.class);
                else intent = new Intent(getApplicationContext(), UnityPlayerActivity.class);
                //pass relevant gift data
                intent.putExtra("sceneType", loadedGift.getGiftType());
                intent.putExtra("FROM OPEN", true);
                intent.putExtra("HASH VALUE", giftHash);
                intent.putExtra("LABEL", label);
                intent.putExtra(Globals.CURR_GIFT_KEY, loadedGift);
                Log.d("LPC", "runnable gift download get friend name: "+friendName);
                intent.putExtra("FRIEND NAME", friendName);
                intent.putExtra("IS RECEIVED", isReceived);
                intent.putExtra("WAS OPENED", wasOpened);
                if(!wasOpened) {
                    //if the gift wasn't previously opened, mark it opened in the db
                    MarkOpenedThread markOpenedThread = new MarkOpenedThread(intent, giftHash);
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


        /**
         * Get the chosen gift object from the db
         */
        public void getGift(){
            query = mDatabase.child("gifts").child(giftHash);

            //listener for the chosen Gift's query
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                    Log.d("LPC", "snapshot: " + snapshot.getValue());
                    if (snapshot.exists()) {
                        loadedGift = snapshot.getValue(Gift.class);
                        //prevent changes on the sender side
                        if(loadedGift.getSender().equals(userID)) {
                            Log.d("LPC", "i sent this gift");
                            return;
                        }
                        if(fromReceive) wasOpened = loadedGift.isOpened();
                        else wasOpened = true;
                        //get the gifts sender
                        getFriendName(loadedGift.getSender());
                        isReceived = true;
                    } else {
                        showErrorDialog();
                        Log.d("LPC", "snapshot doesn't exist");
                    }
                    Log.d("LPC","get gift listener was removed");
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        /**
         * Get the chosen gifts sender
         */
        public void getFriendName(String id){
            query = mDatabase.child("users").child(id);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                    if (snapshot.exists()) {
                        friendName = (String) snapshot.child("name").getValue();
                        handler.post(runnable);
                    } else {
                        showErrorDialog();
                        Log.d("LPC", "snapshot doesn't exist");
                    }
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
        private HashMap<String, String>  friendMap = new HashMap<>();
        private Intent intent;
        private int numFriends = -1;

        public GetFriendsThread(Intent intent){
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("LPC", "in runnable - size of friend map: "+friendMap.size());
                //check for strange bound errors
                if(numFriends == -1 || friendMap.size()<numFriends) return;
                intent.putExtra("FRIEND MAP", friendMap);
                Log.d("LPC", "in runnable - friend map "+friendMap.toString());
                startActivity(intent);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());


        @Override
        public void run() {
            super.run();
            //first get all of the current users friend's id's
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
                        //if the user has no friend :(, we can call the runnable to go back
                        if(numFriends == 0){
                            handler.post(runnable);
                        }
                        //grt each friend's screen name
                        for(String key: newUser.getFriends().keySet()){
                            String friendID = newUser.getFriends().get(key);
                            Query query = mDatabase.child("users").orderByChild("userId").equalTo(friendID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String friendName = (String) snapshot.child(friendID).child("name").getValue();
                                    //store each friend's screen name in form name -> userID
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
