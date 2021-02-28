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
import java.util.HashMap;

/**
 * This is loading splash screen for when the multi-media data is being download from the cloud
 *
 * When a user opens a gift, this will show as all of the multimedia for that gift is read in the cloud
 *
 * At the end, user will be directed to the ReviewGiftActivity
 */
public class DownloadSplashActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private FirebaseStorage mStorage;

    private String recipientID, hashValue;

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
        recipientID = startIntent.getStringExtra("RECIPIENT ID");
        hashValue = startIntent.getStringExtra("HASH VALUE");

        //data from demo activity intent
//        Intent startIntent = getIntent();
//        Gift gift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);

//        Log.d("LPC", "selectedData uri: " + selectedData.getPath());

        Intent intent = new Intent(this, ReviewGiftActivity.class);
//        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(intent);
        storageLoaderThread.start();


//        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(selectedData, sender,
//                receiver, hashValue, contentType, intent);
//        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(gift, selectedData, intent);
//        storageLoaderThread.start();
    }


    /**
     * Thread to store multimedia to the cloud
     */
    public class StorageLoaderThread extends Thread {
        private Gift loadedGift;
//        private String recipientID;
        private Query query;
        private Intent intent;

        public StorageLoaderThread(Intent intent){
//            this.recipientID = recipientID;
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
            query = mDatabase.child("gifts").child(recipientID).orderByChild(hashValue);

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