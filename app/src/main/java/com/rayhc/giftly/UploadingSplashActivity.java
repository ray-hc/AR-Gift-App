package com.rayhc.giftly;

import androidx.annotation.NonNull;
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
                Toast.makeText(getApplicationContext(), "Gift has been sent", Toast.LENGTH_SHORT).show();
                startActivity(intent);
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


            //now upload media
            ArrayList<String> keys = new ArrayList<>(saveGift.getContentType().keySet());
            int index = 0;
            uploadFile(index, keys);
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
                        UserManager.sendGift(fromUser, toUser, mGift);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }


}