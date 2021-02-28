package com.rayhc.giftly;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * A skinny demo of how our firebase database will work
 *
 * Only the GET and SAVE buttons for gifts 1 and 2 work; only gift 1 has an associated image
 *
 * Pressing the SAVE buttons will save a premade gift object to the DB
 * SAVE gift 1 will also prompt user to pick an image from gallery to be associated with the gift
 *
 * GET button will get that gifts link and put it in the top text view
 *
 * PIN edit text allows you to type in the pin number of that gift and retrieve its data
 * Not only will it put the link at the top, but it will also put the image of gift 1 at the bottom
 * if you type in the pin "1111" and you have saved gift 1
 *
 * GIFT PINS are 1111 and 1112 for gifts 1 and 2, respectively
 */

public class FirebaseDemoActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;
    private Uri mImgUri;
    private final String ImgFileName = "giftImage.png";

    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;
    private StorageReference storageRef;
    private String mText;
    private Query mQuery;
    private ImageView mImageView;
    private VideoView mVideoView;
    String mPin;
    private TextView mTextView;

    private Gift gift1;
    private Gift gift2;
    private HashMap<String, String> contentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebasedemo);

        checkPermission(this);

        mImageView = (ImageView) findViewById(R.id.image_view);
        mVideoView = (VideoView) findViewById(R.id.video_view);
        mImgUri = FileProvider.getUriForFile(this, "com.rayhc.giftly", new File(getExternalFilesDir(null), ImgFileName));

        // Initialize Firebase Auth and Database Reference
//        mFirebaseAuth = FirebaseAuth.getInstance();
//        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mStorage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        storageRef = mStorage.getReference();



        //gift objects
        gift1 = new Gift();
        gift1.setContentType(null);
        gift1.setGiftType(null);
        gift1.setHashValue("hash value 1");
        gift1.setQrCode("qr code 1");
        gift1.setOpened(false);
        gift1.setReceiver("B");
        gift1.setSender("A");
        gift1.setEncrypted(false);
        gift1.setLinks(new HashMap<>());
        gift1.addLink("https://google.com");

        gift2 = new Gift();
        gift2.setContentType(null);
        gift2.setGiftType(null);
//        gift2.setHashValue("hash value 1");
//        gift2.setQrCode("qr code 1");
        gift2.setOpened(false);
        gift2.setReceiver("C");
        gift2.setSender("B");
        gift2.setEncrypted(false);
        gift1.setLinks(new HashMap<>());
        gift1.addLink("https://wikipedia.com");

        //text view
        mTextView = (TextView) findViewById(R.id.text);
        //instance data
        if(savedInstanceState != null){
            mTextView.setText(savedInstanceState.getString("TEXT"));
        }

        //get buttons
        Button getButton1 = (Button) findViewById(R.id.get_button1);
        Button getButton2 = (Button) findViewById(R.id.get_button2);

        //save buttons
        Button saveButton1 = (Button) findViewById(R.id.save_button1);
        Button saveButton2 = (Button) findViewById(R.id.save_button2);

        //save button callbacks
        saveButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //last min gift data
                HashMap<String, String> map = new HashMap<>();
                map.put("ID 1", "image");
                gift1.setContentType(map);
                gift1.setTimeCreated(10);
                //hash value
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
//                    messageDigest.digest(gift1.getHashString().getBytes());
                    byte[] md5 = messageDigest.digest();
                    // Create Hex String
                    StringBuilder hexString = new StringBuilder();
                    for (byte aMessageDigest : md5) {
                        String h = Integer.toHexString(0xFF & aMessageDigest);
                        while (h.length() < 2)
                            h = "0" + h;
                        hexString.append(h);
                    }
                    gift1.setHashValue(gift1.createHashValue());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                DBLoaderThread dbLoaderThread = new DBLoaderThread(gift1.getReceiver());
                dbLoaderThread.start();

//                mDatabase.child("gifts").child(gift1.getReceiver()).setValue(gift1);
//                mDatabase.child("gifts").setValue(gift1);
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImgUri);
                intent.setType("image/*");
//                intent.putExtra("PIN_KEY", gift1.getId());
                startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);

            }
        });

        saveButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //last min gift data
                HashMap<String, String> map = new HashMap<>();
                map.put("ID 1", "video");
                gift2.setContentType(map);
                gift2.setTimeCreated(System.currentTimeMillis());
                //hash value
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
//                    messageDigest.digest(gift2.getHashString().getBytes());
                    byte[] md5 = messageDigest.digest();
                    // Create Hex String
                    StringBuilder hexString = new StringBuilder();
                    for (byte aMessageDigest : md5) {
                        String h = Integer.toHexString(0xFF & aMessageDigest);
                        while (h.length() < 2)
                            h = "0" + h;
                        hexString.append(h);
                    }
                    gift2.setHashValue(hexString.toString());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                DBLoaderThread dbLoaderThread = new DBLoaderThread(gift2.getReceiver());
                dbLoaderThread.start();
//                mDatabase.child("gifts").child((gift2.getReceiver())).setValue(gift2);
//                mDatabase.child("gifts").setValue(gift1);
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImgUri);
                intent.setType("video/*");
//                intent.putExtra("PIN_KEY", gift1.getId());
                startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);

            }
        });

        getButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LPC", "gift1 hash: "+gift1.getHashValue());
                mQuery = mDatabase.child("gifts").child(gift1.getReceiver()).orderByChild("hashValue")
                .equalTo(gift1.getHashValue());

                //listener for the newly added Gift's query based on the input pin
                //put its link at the top
                mQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
//                        Log.d("LPC", "snapshot: " + snapshot.getValue());
//                        if (snapshot.exists()) {
//                            Gift gift = snapshot.child(gift1.getHashValue()).getValue(Gift.class);
//                            mText = gift.getLinks().get("ID "+(gift.getLinks().size()-1));
//                            Log.d("LPC", "link from search bar press: "+mText);
//                            //set content map
//                            contentMap = gift.getContentType();
//                            Log.d("LPC", "content Map key value: "+contentMap.get("ID 1"));
//                            handleMedia(contentMap.get("ID 1"));
//                            if (mText != null) {
//                                mTextView.setText("Link: " + Html.fromHtml(mText));
//                                mTextView.setMovementMethod(LinkMovementMethod.getInstance());
//                            } else {
//                                showErrorDialog();
//                            }
//                        } else {
//                            showErrorDialog();
//                            Log.d("LPC", "snapshot doesn't exist");
//                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });


        //search bar and button
        EditText pinEnter = (EditText) findViewById(R.id.pin_enter);

        Button searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //field value
                mPin = pinEnter.getText().toString();
                Log.d("LPC", "mPin: " + mPin);
                mQuery = mDatabase.child("gifts");

                //listener for the newly added Gift's query based on the input pin
                //put its link at the top
                mQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
//                        Log.d("LPC", "snapshot: " + snapshot.getValue());
//                        if (snapshot.exists()) {
//                            Gift gift = snapshot.child(mPin).getValue(Gift.class);
//                            mText = gift.getLinks().get("ID "+(gift.getLinks().size()-1));
//                            Log.d("LPC", "link from search bar press: "+mText);
//                            //set content map
//                            contentMap = gift.getContentType();
//                            Log.d("LPC", "content Map key value: "+contentMap.get("ID 1"));
//                            handleMedia(contentMap.get("ID 1"));
//                            if (mText != null) {
//                                mTextView.setText("Link: " + Html.fromHtml(mText));
//                                mTextView.setMovementMethod(LinkMovementMethod.getInstance());
//                            } else {
//                                showErrorDialog();
//                            }
//                        } else {
//                            showErrorDialog();
//                            Log.d("LPC", "snapshot doesn't exist");
//                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

    }

    public void handleMedia(String type){
        //get this gift's image and put it in the image view
        // (only works with gift 1's pin of 1111 since that the only gift I set with an image)
        mImageView.setVisibility(View.GONE);
        mVideoView.setVisibility(View.GONE);
        if (type.equals("image")) {
            String filePath = "gift/" + gift1.getHashValue()+ "/image_1.jpg";
            Log.d("LPC", "image file path: " + filePath);
            StorageReference imgRef = storageRef.child(filePath);
            File localFile = null;
            try {
                localFile = File.createTempFile("tempImg", "jpg");
                Log.d("LPC", "local image file was made ");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File finalLocalFile = localFile;
            imgRef.getFile(localFile).addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mImageView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "image download successful");
                        Bitmap bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        Log.d("LPC", "image download failed");
                    }
                }
            });

        } else if(type.equals("video")){
            String filePath = "gift/" + gift2.getSender()+"_to_"+gift2.getReceiver()+ "/videoGift.mp4";
            Log.d("LPC", "video file path: " + filePath);
            StorageReference imgRef = storageRef.child(filePath);
            File localFile = null;
            try {
                localFile = File.createTempFile("tempImg", "mp4");
                Log.d("LPC", "local video file was made ");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File finalLocalFile = localFile;
            imgRef.getFile(localFile).addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mVideoView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "video download successful");
                        Bitmap bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                        mVideoView.setVideoPath(finalLocalFile.getPath());
                        mVideoView.start();
                    } else {
                        Log.d("LPC", "video download failed");
                    }
                }
            });
        }
    }

    /**
     * Pick an image from the phone's gallery to be associated with the gift
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //publish image to db
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null){
            Uri selectedData = data.getData();
            String sender, receiver;
            if(selectedData.toString().contains("image")) {
                Log.d("LPC", "onActivityResult: here");
                sender = gift1.getSender();
                receiver = gift1.getReceiver();

//                Intent splashIntent = new Intent(this, UploadingSplashActivity.class);
//                splashIntent.putExtra("URI", selectedData);
//                splashIntent.putExtra("GIFT", gift1);
//                splashIntent.putExtra("SENDER", sender);
//                splashIntent.putExtra("RECEIVER", receiver);
//                startActivity(splashIntent);


//                String path = "gift/" + gift1.getSender()+"_to_"+gift1.getReceiver()+ "/pictureGift.jpg";
//                StorageReference giftRef = storageRef.child(path);
//                UploadTask uploadTask = giftRef.putFile(selectedData);
//                uploadTask.addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        Log.d("LPC", "image upload complete!");
//                    }
//                });
            } else if(selectedData.toString().contains("video")) {
                Log.d("LPC", "onActivityResult: here");
                sender = gift2.getSender();
                receiver = gift2.getReceiver();

//                Intent splashIntent = new Intent(this, UploadingSplashActivity.class);
//                splashIntent.putExtra("URI", selectedData);
//                splashIntent.putExtra("SENDER", sender);
//                splashIntent.putExtra("RECEIVER", receiver);
//                startActivity(splashIntent);

//
//                String path = "gift/" + gift2.getSender()+"_to_"+gift2.getReceiver()+ "/videoGift.mp4";
//                StorageReference giftRef = storageRef.child(path);
//                UploadTask uploadTask = giftRef.putFile(selectedData);
//                uploadTask.addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        Log.d("LPC", "video upload complete!");
//                    }
//                });
            }
        }
    }

    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("TEXT", mTextView.getText().toString());
    }

    /**
     * Error pop-up for bad queries
     */
    public void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(FirebaseDemoActivity.this);
        builder.setMessage("There has been an error. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Read/Write permission checks
     */
    public static void checkPermission(Activity activity){
        if(Build.VERSION.SDK_INT < 23)
            return;
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
    }

    /**
     * Loader thread for the realtime DB only (gift objects)
     */
    public class DBLoaderThread extends Thread{
        private String id;
        public DBLoaderThread(String id){
            this.id = id;
        }

        @Override
        public void run() {
            if(id.equals("B")){
//                HashMap<String, String> map = new HashMap<>();
//                map.put("ID 1", "image");
//                gift1.setContentType(map);
                mDatabase.child("gifts").child(gift1.getReceiver())
                        .child(gift1.getHashValue()).setValue(gift1);
            } else if (id.equals("C")){
//                HashMap<String, String> map = new HashMap<>();
//                map.put("ID 1", "video");
//                gift2.setContentType(map);
                mDatabase.child("gifts").child((gift2.getReceiver()))
                        .child(gift2.getHashValue()).setValue(gift2);
            }
        }
    }
}
