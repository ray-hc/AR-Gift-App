package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainer;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

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


    private boolean mFromReview;
    private String mFileLabel;
    private Gift mGift;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading_splash);

        //storage stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //data from demo activity intent
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra("GIFT");
        Uri selectedData = startIntent.getParcelableExtra("URI");
        mFromReview = startIntent.getBooleanExtra("FROM REVIEW", false);
        mFileLabel = startIntent.getStringExtra("FILE LABEL");
//        String sender = startIntent.getStringExtra("SENDER");
//        String receiver = startIntent.getStringExtra("RECEIVER");
//        String hashValue = startIntent.getStringExtra("HASH");
//        HashMap<String, String> contentType = (HashMap<String, String>) startIntent.getSerializableExtra("CONTENT_TYPE");

        Log.d("LPC", "selectedData uri: " + selectedData.getPath());

        //TODO: change the end destination of this intent to the create gift fragment (not sure how to yet)
        Intent intent = new Intent(this, FragmentContainerActivity.class);
        intent.putExtra("GIFT", mGift);
        StorageLoaderThread storageLoaderThread = new StorageLoaderThread(selectedData, intent);
        storageLoaderThread.start();

//        FragmentManager fm = getSupportFragmentManager();
//        CreateGiftFragment createGiftFragment = new CreateGiftFragment();
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("GIFT", mGift);
//        createGiftFragment.setArguments(bundle);
//        FragmentTransaction ft = fm.beginTransaction();
//        //TODO: add an id parameter below?
//        ft.replace(R.id.frame_container, createGiftFragment);
//        ft.commit();



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
        private String sender, receiver, hashValue;
        private Intent intent;
        private HashMap<String, String> contentType;
//        private Gift threadGift;
        private FragmentManager fragmentManager = getSupportFragmentManager();
        private final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        private final CreateGiftFragment createGiftFragment = new CreateGiftFragment();

        public StorageLoaderThread(Uri selectedData, String sender, String receiver,
                                   String hashValue, HashMap<String, String> contentType, Intent intent) {
            this.selectedData = selectedData;
            this.sender = sender;
            this.receiver = receiver;
            this.hashValue = hashValue;
            this.contentType = contentType;
            this.intent = intent;
        }

        public StorageLoaderThread(Uri selectedData, Intent intent){
//            threadGift = gift;
            sender = mGift.getSender();
            receiver = mGift.getReceiver();
            hashValue = mGift.getHashValue();
            contentType = mGift.getContentType();
            this.selectedData = selectedData;
            this.intent = intent;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //go back to the demo activity
                startActivity(intent);
                Log.d("LPC", "stored the image in cloud");
                //go back to create gift fragment
//                Bundle b = new Bundle();
//                b.putSerializable("gift", gift);
//                createGiftFragment.setArguments(b);
//                fragmentTransaction.replace(R.id., createGiftFragment).commit();
            }
        };

        @Override
        public void run() {
            //should very solidly for now
            String fileName;
            String path;
            if (selectedData.toString().contains("image")) {
                Log.d("LPC", "onActivityResult: here");
                if(!mFromReview) {
                    fileName = "image_"+contentType.size();
                    path = "gift/" + hashValue + "/"+fileName+".jpg";
                }
                else {
                    fileName = mFileLabel;
                    path = "gift/"+hashValue+"/"+fileName+".jpg";
                }
                Log.d("LPC", "cloud storage image file path : "+path);
                StorageReference giftRef = storageRef.child(path);
                UploadTask uploadTask = giftRef.putFile(selectedData);
                uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Log.d("LPC", "image upload complete!");
                    }
                });
                mGift.addContentType(fileName);
            } else if (selectedData.toString().contains("video")) {
                Log.d("LPC", "onActivityResult: here");
                if(!mFromReview) {
                    fileName = "video_"+contentType.size();
                    path = "gift/" + hashValue + "/"+fileName+".mp4";
                }
                else {
                    fileName = mFileLabel;
                    path = "gift/"+hashValue+"/"+fileName+".mp4";
                }
                Log.d("LPC", "cloud storage video file path : "+path);
                StorageReference giftRef = storageRef.child(path);
                UploadTask uploadTask = giftRef.putFile(selectedData);
                uploadTask.addOnCompleteListener(UploadingSplashActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Log.d("LPC", "video upload complete!");
                    }
                });
                mGift.addContentType(fileName);
//                Log.d("LPC", "gift contentType: "+mGift.getContentType().toString());
            }
            handler.post(runnable);
        }
    }
}