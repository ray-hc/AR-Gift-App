package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
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

    private String recipientID, hashValue, userID;
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
        if(startIntent.getBooleanExtra("GET FRIENDS", false)){
            userID = startIntent.getStringExtra("USER ID");
            friendMap = new HashMap<>();

            Intent intent = new Intent(this, ChooseFriendActivity.class);
            GetFriendsThread getFriendsThread = new GetFriendsThread(intent);
            getFriendsThread.start();

        } else{
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
                        dummyMap.put("xa7JPQsISNQ8RWnCfwuZwJZml9s2", "xa7JPQsISNQ8RWnCfwuZwJZml9s2");
                        dummyMap.put("c3Vcn0FiA6XElC7PM5BbnFR5hEE2", "c3Vcn0FiA6XElC7PM5BbnFR5hEE2");
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