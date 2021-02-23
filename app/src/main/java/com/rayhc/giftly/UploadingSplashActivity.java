package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * This is loading splash screen for when the multi-media data is being upload to the cloud
 *
 * When a user "sends" a gift, this will show as all of the multimedia for that gift is stored in the cloud
 *
 * At the end, there'll be a "We just sent your gift" type of screen that'll show when this is done
 */
public class UploadingSplashActivity extends AppCompatActivity {

    private StorageReference storageRef;
    private FirebaseStorage mStorage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading_splash);

        //storage stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //data from demo activity intent
        Intent startIntent = getIntent();
        Uri selectedData = startIntent.getParcelableExtra("URI");
        String sender = startIntent.getStringExtra("SENDER");
        String receiver = startIntent.getStringExtra("RECEIVER");

        Log.d("LPC", "selectedData uri: " + selectedData.getPath());

        Intent intent = new Intent(this, FirebaseDemoActivity.class);

        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(selectedData, sender,
                receiver, intent);
        storageLoaderThread.start();
    }


    /**
     * Thread to store multimedia to the cloud
     */
    public class StorageLoaderThread extends Thread {
        private Uri selectedData;
        private String sender, receiver;
        private Intent intent;

        public StorageLoaderThread(Uri selectedData, String sender, String receiver, Intent intent) {
            this.selectedData = selectedData;
            this.sender = sender;
            this.receiver = receiver;
            this.intent = intent;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //go back to the demo activity
                startActivity(intent);
            }
        };

        @Override
        public void run() {
            //should very solidly for now
            if (selectedData.toString().contains("image")) {
                Log.d("LPC", "onActivityResult: here");
                String path = "gift/" + sender + "_to_" + receiver + "/pictureGift.jpg";
                StorageReference giftRef = storageRef.child(path);
                UploadTask uploadTask = giftRef.putFile(selectedData);
                uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Log.d("LPC", "image upload complete!");
                    }
                });
            } else if (selectedData.toString().contains("video")) {
                Log.d("LPC", "onActivityResult: here");
                String path = "gift/" + sender + "_to_" + receiver + "/videoGift.mp4";
                StorageReference giftRef = storageRef.child(path);
                UploadTask uploadTask = giftRef.putFile(selectedData);
                uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Log.d("LPC", "video upload complete!");
                    }
                });
            }
            handler.post(runnable);
        }
    }
}