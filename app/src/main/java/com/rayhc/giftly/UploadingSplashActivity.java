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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
            mDatabase.child("gifts").child(saveGift.getReceiver())
                    .child(saveGift.getHashValue()).setValue(saveGift);
            Log.d("LPC", "wrote gift to the rt DB");


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
    }


}