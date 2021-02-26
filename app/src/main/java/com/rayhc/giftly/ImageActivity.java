package com.rayhc.giftly;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ImageActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    //storage ref
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    //widgets
    private ImageView mImageView;
    private Button mChooseButton, mSaveButton, mCancelButton, mDeleteButton;
    private ProgressBar mProgressBar;

    //data from gift
    private Gift gift;
    private String sender, recipient, hashValue;
    private HashMap<String, String> contentType;

    //from review
    private boolean mFromReview;
    private String mFileLabel;

    private Uri currentData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        //firebase stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //get data from gift
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: "+gift.toString());
        Log.d("LPC", "image activity: gift contentType: "+gift.getContentType().toString());
        mFromReview = startIntent.getBooleanExtra("FROM REVIEW", false);
        mFileLabel = startIntent.getStringExtra("FILE LABEL");

        //wire button and image view
        mChooseButton = (Button) findViewById(R.id.image_choose_button);
        mSaveButton = (Button) findViewById(R.id.image_save_button);
        mSaveButton.setEnabled(false);
        mDeleteButton = (Button) findViewById(R.id.image_delete_button);
        mDeleteButton.setVisibility(View.GONE);
        mCancelButton = (Button) findViewById(R.id.image_cancel_button);
        mImageView = (ImageView) findViewById(R.id.chosen_image);
        mProgressBar = (ProgressBar) findViewById(R.id.image_progress_bar);
        mProgressBar.setVisibility(View.GONE);

        //wire button callbacks
        mChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChoose();
            }
        });
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave();
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDelete();
            }
        });

        //handle if from the review activity
        if(mFromReview){
//            String filePath = gift.getContentType().get(label);
//            Log.d("LPC", "image activity file path: "+filePath);
            mSaveButton.setEnabled(true);
            mDeleteButton.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            ImageReaderThread imageReaderThread = new ImageReaderThread(mFileLabel);
            imageReaderThread.start();
//            updateView(label);
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", gift);
    }

    /**
     * Go to uploading splash screen on save button
     * <p>
     * Keep a class reference to the URI which gets altered on every successful image selection
     */

    //*******BUTTON CALLBACKS*******//
    public void onChoose() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImgUri);
        intent.setType("image/*");
//                intent.putExtra("PIN_KEY", gift1.getId());
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);
    }

    /**
     * Go to the uploading splash page, which will put the image in cloud storage too
     */
    public void onSave() {
        Intent splashIntent = new Intent(this, UploadingSplashActivity.class);
        splashIntent.putExtra("GIFT", gift);
        splashIntent.putExtra("URI", currentData);
        splashIntent.putExtra("FROM REVIEW", mFromReview);
        splashIntent.putExtra("FILE LABEL", mFileLabel);
//        splashIntent.putExtra("SENDER", sender);
//        splashIntent.putExtra("RECIPIENT", recipient);
//        splashIntent.putExtra("HASH", hashValue);
//        splashIntent.putExtra("CONTENT_TYPE", contentType);
        startActivity(splashIntent);

    }

    /**
     * Delete the chosen image from the db and remove it from the gifts contents
     */
    public void onDelete(){
        Intent intent = new Intent(this, ReviewGiftActivity.class);
        intent.putExtra("GIFT", gift);
        ImageDeleterThread imageDeleterThread = new ImageDeleterThread(mFileLabel, intent);
        imageDeleterThread.start();
    }

    //******ON ACTIVITY RESULT******//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if gallery pick was successful, save the URI and populate the image view
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null) {
            mSaveButton.setEnabled(true);
            Uri selectedData = data.getData();
            currentData = selectedData;
            mImageView.setImageURI(null);
            mImageView.setImageURI(currentData);
        }
    }

    /**
     * Thread to load in the image when reading from cloud
     */
    public class ImageReaderThread extends Thread{
        private final String label;
        private Bitmap bitmap;
        public ImageReaderThread(String label){
            this.label = label;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mImageView.setVisibility(View.VISIBLE);
                mImageView.setImageBitmap(bitmap);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            String filePath = "gift/" + gift.getHashValue()+ "/"+label;
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
            imgRef.getFile(localFile).addOnCompleteListener(ImageActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mImageView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "image download successful");
                        bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
//                        mImageView.setImageBitmap(bitmap);
                        handler.post(runnable);
                    } else {
                        Log.d("LPC", "image download failed");
                    }
                }
            });
        }
    }

    /**
     * Thread for deleting the image from the DB and removing it from the gift's contents
     */
    public class ImageDeleterThread extends Thread{
        private String label;
        private Intent intent;

        public ImageDeleterThread(String label, Intent intent){
            this.label = label;
            this.intent = intent;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                Toast.makeText(getApplicationContext(), ""+label+" was deleted", Toast.LENGTH_SHORT)
                .show();
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            String filePath = "gift/" + gift.getHashValue()+ "/"+label;
            Log.d("LPC", "image file path: " + filePath);
            StorageReference imgRef = storageRef.child(filePath);
            imgRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    gift.getContentType().remove(label);
                    handler.post(runnable);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    showErrorDialog();
                }
            });
        }
    }

    /**
     * Error pop-up
     */
    public void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageActivity.this);
        builder.setMessage("There has been an error deleting your file. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }




    public void updateView(String label){
        String filePath = "gift/" + gift.getHashValue()+ "/"+label;
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
        imgRef.getFile(localFile).addOnCompleteListener(ImageActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
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
    }
}
