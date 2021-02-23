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
    String mPin;
    private TextView mTextView;

    private Gift gift1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebasedemo);

        checkPermission(this);

        mImageView = (ImageView) findViewById(R.id.image_view);
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
        gift1.setId("1111");
//        gift1.setContentType(1);
//        gift1.setGiftType(1);
//        gift1.setFile(null);
        gift1.setHashValue("hash value 1");
        gift1.setQrCode("qr code 1");
        gift1.setOpened(false);
        gift1.setReceiver("B");
        gift1.setSender("A");
        gift1.setEncrypted(false);
        gift1.setLink("https://www.google.com/");

        Gift gift2 = new Gift();
        gift2.setId("1112");
//        gift2.setContentType(1);
//        gift2.setGiftType(1);
//        gift2.setFile(null);
//        gift2.setHashValue("hash value 1");
//        gift2.setQrCode("qr code 1");
        gift2.setOpened(false);
//        gift2.setReceiver("B");
//        gift2.setSender("A");
        gift2.setEncrypted(false);
        gift2.setLink("https://en.wikipedia.org/wiki/Main_Page");

        Gift gift3 = new Gift();
        gift3.setId("1113");
        gift3.setLink("This is Gift 3 link");

        Gift gift4 = new Gift();
        gift4.setId("1114");
        gift4.setLink("This is Gift 4 link");

        //text view
        mTextView = (TextView) findViewById(R.id.text);
        //instance data
        if(savedInstanceState != null){
            mTextView.setText(savedInstanceState.getString("TEXT"));
        }

        //get buttons
        Button getButton1 = (Button) findViewById(R.id.get_button1);
        Button getButton2 = (Button) findViewById(R.id.get_button2);
        Button getButton3 = (Button) findViewById(R.id.get_button3);
        Button getButton4 = (Button) findViewById(R.id.get_button4);

        //save buttons
        Button saveButton1 = (Button) findViewById(R.id.save_button1);
        Button saveButton2 = (Button) findViewById(R.id.save_button2);
        Button saveButton3 = (Button) findViewById(R.id.save_button3);
        Button saveButton4 = (Button) findViewById(R.id.save_button4);

        //save button callbacks
        saveButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child("gifts").child(gift1.getId()).setValue(gift1);
//                mDatabase.child("gifts").setValue(gift1);
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImgUri);
//                intent.putExtra("PIN_KEY", gift1.getId());
                startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);

            }
        });

        saveButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child("gifts").child((gift2.getId())).setValue(gift2);
//                mDatabase.child("gifts").setValue(gift1);

            }
        });


        //get button callbacks

        //gets the gift data for gift 1 from firebase
        //puts the link at the top
        getButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuery = mDatabase.child("gifts").orderByChild("id").equalTo(gift1.getId());
                mQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                        if (snapshot.exists()) {
                            mText = (String) snapshot.child((gift1.getId()))
                                    .child("link").getValue();
                            Log.d("LPC", "snapshot: " + snapshot.getValue());
                            if(mText != null) {
                                mTextView.setText("Link: "+ Html.fromHtml(mText));
                                mTextView.setMovementMethod(LinkMovementMethod.getInstance());                            }
                            else {
                                showErrorDialog();
                            }
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
        });

        //same click listener as the previous except corresponds to gift 2
        getButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuery = mDatabase.child("gifts").orderByChild("id").equalTo(gift2.getId());
                mQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                        if (snapshot.exists()) {
                            mText = (String) snapshot.child((gift2.getId()))
                                    .child("link").getValue();
                            Log.d("LPC", "snapshot: " + snapshot.getValue());
                            if(mText != null) {
                                mTextView.setText("Link: "+Html.fromHtml(mText));
                                mTextView.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                            else {
                                showErrorDialog();
                            }
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
        });


        //search bar and button
        EditText pinEnter = (EditText) findViewById(R.id.pin_enter);

        Button searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //field value
                mPin = pinEnter.getText().toString();
                Log.d("LPC", "mPin: "+mPin);
                mQuery = mDatabase.child("gifts");

                //listener for the newly added Gift's query based on the input pin
                //put its link at the top
                mQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                        Log.d("LPC", "snapshot: " + snapshot.getValue());
                        if (snapshot.exists()) {
                            mText = (String) snapshot.child(mPin)
                                    .child("link").getValue();
                            if(mText != null) {
                                mTextView.setText("Link: "+Html.fromHtml(mText));
                                mTextView.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                            else {
                                showErrorDialog();
                            }
                        } else {
                            showErrorDialog();
                            Log.d("LPC", "snapshot doesn't exist");
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                //get this gift's image and put it in the image view
                // (only works with gift 1's pin of 1111 since that the only gift I set with an image)

                String filePath = "gift/"+mPin+"/pictureGift.jpg";
                Log.d("LPC", "file path: "+filePath);
                StorageReference imgRef = storageRef.child(filePath);
                File localFile = null;
                try {
                    localFile = File.createTempFile("tempImg", "jpg");
                    Log.d("LPC", "local file was made ");
                } catch (IOException e){
                    e.printStackTrace();
                }

                File finalLocalFile = localFile;
                imgRef.getFile(localFile).addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            Log.d("LPC", "download successful");
                            Bitmap bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                            mImageView.setImageBitmap(bitmap);
                        } else {
                            Log.d("LPC", "download failed");
                        }
                    }
                });

            }
        });

    }

    /**
     * Pick an image from the phone's gallery to be associated with the gift
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //publish image to db
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null){
            Log.d("LPC", "onActivityResult: here");
            Uri selectedImg = data.getData();
            String path = "gift/"+gift1.getId()+"/pictureGift.jpg";
            StorageReference giftRef = storageRef.child(path);
            UploadTask uploadTask = giftRef.putFile(selectedImg);
            uploadTask.addOnCompleteListener(FirebaseDemoActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Log.d("LPC", "upload complete!");
                }
            });
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
}
