package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

    private StorageReference storageRef;
    private FirebaseStorage mStorage;

    private Gift mGift;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_splash);

        //storage stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //data from demo activity intent
        Intent startIntent = getIntent();
        Gift gift = (Gift) startIntent.getSerializableExtra("GIFT");

//        Log.d("LPC", "selectedData uri: " + selectedData.getPath());

        Intent intent = new Intent(this, ReviewGiftActivity.class);
        intent.putExtra("GIFT", gift);
        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(gift, intent);
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
        private Uri selectedData;
        private Intent intent;
        private HashMap<String, String> contentType;
        private Gift newGift;


        public StorageLoaderThread(Gift gift, Intent intent){
            newGift = gift;
            contentType = gift.getContentType();
            this.intent = intent;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //go to the list activity
                Log.d("LPC", "gift giftType after reading cloud: "+newGift.getContentType().toString());
                intent.putExtra("GIFT", newGift);
                startActivity(intent);
//                Log.d("LPC", "stored the image in cloud");
                //go back to create gift fragment
//                Bundle b = new Bundle();
//                b.putSerializable("gift", gift);
//                createGiftFragment.setArguments(b);
//                fragmentTransaction.replace(R.id., createGiftFragment).commit();
            }
        };

        @Override
        public void run() {
            String filePath = "gift/" + newGift.getHashValue();
            StorageReference giftRef = storageRef.child(filePath);
            giftRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    for(StorageReference item : listResult.getItems()){
                        String itemName = item.getName();
                        Log.d("LPC", "item: "+item.getName());
                        File localFile = null;
                        if(itemName.endsWith(".jpg")){
                            try {
                                localFile = File.createTempFile("tempImg", "jpg");
                                Log.d("LPC", "local image file was made ");
//                                gift.getContentType().put(itemName, localFile.getAbsolutePath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if(itemName.endsWith(".mp4")){
                            try {
                                localFile = File.createTempFile("tempImg", "mp4");
                                Log.d("LPC", "local video file was made ");
//                                gift.getContentType().put(itemName, localFile.getAbsolutePath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    handler.post(runnable);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                //TODO: fill this in somehow
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
//            handler.post(runnable);
        }
    }
}