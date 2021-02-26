package com.rayhc.giftly;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

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

public class VideoActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    //storage ref
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    //widgets
    private VideoView mVideoView;
    private Button mChooseButton, mSaveButton, mCancelButton, mDeleteButton;
    private MyMediaController mMediaController;
    private ProgressBar mProgressBar;

    //data from gift
    private Gift gift;
    private Uri currentData;
    private String sender, recipient, hashValue;
    private HashMap<String, String> contentType;

    //from review
    private boolean mFromReview;
    private String mFileLabel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //firebase stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //get data from gift
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: "+gift.toString());
        mFromReview = startIntent.getBooleanExtra("FROM REVIEW", false);
        mFileLabel = startIntent.getStringExtra("FILE LABEL");

        //wire button and video view
        mChooseButton = (Button) findViewById(R.id.video_choose_button);
        mSaveButton = (Button) findViewById(R.id.video_save_button);
        mSaveButton.setEnabled(false);
        mDeleteButton = (Button) findViewById(R.id.video_delete_button);
        mDeleteButton.setVisibility(View.GONE);
        mCancelButton = (Button) findViewById(R.id.video_cancel_button);
        mProgressBar = (ProgressBar) findViewById(R.id.video_progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mVideoView = (VideoView) findViewById(R.id.chosen_video);

        //add a media controller
        mMediaController = new MyMediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

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
        if(startIntent.getBooleanExtra("FROM REVIEW", false)){
            String label = startIntent.getStringExtra("FILE LABEL");
//            String filePath = gift.getContentType().get(label);
//            Log.d("LPC", "video activity file path: "+filePath);
            mSaveButton.setEnabled(true);
            mDeleteButton.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            VideoReaderThread videoReaderThread = new VideoReaderThread(label);
            videoReaderThread.start();
//            updateView(label);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", gift);
    }

    //*******BUTTON CALLBACKS*******//
    public void onChoose() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImgUri);
        intent.setType("video/*");
//                intent.putExtra("PIN_KEY", gift1.getId());
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);
    }

    /**
     * Go to the uploading splash page, which will put the video in cloud storage too
     */
    public void onSave() {
        Intent splashIntent = new Intent(this, UploadingSplashActivity.class);
        splashIntent.putExtra("GIFT", gift);
        splashIntent.putExtra("URI", currentData);
        splashIntent.putExtra("FROM REVIEW", mFromReview);
        splashIntent.putExtra("FILE LABEL", mFileLabel);
        startActivity(splashIntent);

    }

    /**
     * Delete the chosen video from the db and remove it from the gifts contents
     */
    public void onDelete(){
        Intent intent = new Intent(this, ReviewGiftActivity.class);
        intent.putExtra("GIFT", gift);
        VideoDeleterThread videoDeleterThread = new VideoDeleterThread(mFileLabel, intent);
        videoDeleterThread.start();
    }

    //******ON ACTIVITY RESULT******//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if gallery pick was successful, save the URI and populate the video view
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null) {
            mSaveButton.setEnabled(true);
            Uri selectedData = data.getData();
            currentData = selectedData;
            Log.d("LPC", "onActivityResult: current video path for vv: "+currentData.getPath());
            mVideoView.setVideoURI(currentData);
            mVideoView.start();
//            mVideoView.setImageURI(currentData);
        }
    }

    /**
     * Custom Media Controller so it doesn't disappear
     */
    public class MyMediaController extends MediaController {
        public MyMediaController(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyMediaController(Context context, boolean useFastForward) {
            super(context, useFastForward);
        }

        public MyMediaController(Context context) {
            super(context);
        }

        @Override
        public void show(int timeout) {
            super.show(0);
        }

    }

    /**
     * Thread to load in the video when reading from cloud
     */
    public class VideoReaderThread extends Thread{
        private final String label;
        private Bitmap bitmap;
        public VideoReaderThread(String label){
            this.label = label;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.start();
//                mImageView.setImageBitmap(bitmap);
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            String filePath = "gift/" + gift.getHashValue()+ "/"+label;
            Log.d("LPC", "video file path: " + filePath);
            StorageReference vidRef = storageRef.child(filePath);
            File localFile = null;
            try {
                localFile = File.createTempFile("tempVid", "mp4");
                Log.d("LPC", "local video file was made ");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File finalLocalFile = localFile;
            vidRef.getFile(localFile).addOnCompleteListener(VideoActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mVideoView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "video download successful");
//                        bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
//                        mImageView.setImageBitmap(bitmap);
                        mVideoView.setVideoPath(finalLocalFile.getPath());
                        handler.post(runnable);
                    } else {
                        Log.d("LPC", "video download failed");
                    }
                }
            });
        }
    }

    /**
     * Thread for deleting the image from the DB and removing it from the gift's contents
     */
    public class VideoDeleterThread extends Thread{
        private String label;
        private Intent intent;

        public VideoDeleterThread(String label, Intent intent){
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
        AlertDialog.Builder builder = new AlertDialog.Builder(VideoActivity.this);
        builder.setMessage("There has been an error deleting your file. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
