package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class ViewContentsActivity extends AppCompatActivity {

    //firebase stuff
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    //widgets
    private TextView mLinkView;
    private ImageView mImageView;
    private VideoView mVideoView;
    private VideoActivity.MyMediaController mMediaController;
    private ProgressBar mProgressBar;
    private Button mSaveButton, mNextButton, mPreviousButton;

    //gift data
    private Gift mOpenedGift;
    private HashMap<String, String> contentMap;
    private int currIndex;
    private boolean fromMedia;
    private String mLabel, friendName, friendID;

    //files
    private File imageFile;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_contents);


        //wire widgets and set them all to gone
        mLinkView = (TextView) findViewById(R.id.link_gift);
        mLinkView.setVisibility(View.GONE);
        mImageView = (ImageView) findViewById(R.id.image_gift);
        mImageView.setVisibility(View.GONE);
        mVideoView = (VideoView) findViewById(R.id.video_gift);
        mVideoView.setVisibility(View.GONE);
        mPreviousButton = (Button) findViewById(R.id.view_previous_button);
        mNextButton = (Button) findViewById(R.id.view_next_button);
        mSaveButton = (Button) findViewById(R.id.save_contents_button);
        mSaveButton.setVisibility(View.GONE);
        //give save button a callback
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave();
            }
        });
        //add a media controller
        mMediaController = new VideoActivity.MyMediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        mProgressBar = (ProgressBar) findViewById(R.id.contents_porgress_bar);
        mProgressBar.setVisibility(View.GONE);

        //get intent data
        //gift data
        Intent startIntent = getIntent();
        ArrayList<String> keyList = null;
        mOpenedGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        if(startIntent.getBooleanExtra("GET MEDIA", false)) {
            fromMedia = true;
            contentMap = mOpenedGift.getContentType();
            if (currIndex == contentMap.size() - 1) mNextButton.setEnabled(false);
            friendName = startIntent.getStringExtra(Globals.FRIEND_NAME_KEY);
            friendID = startIntent.getStringExtra(Globals.FRIEND_ID_KEY);
            keyList = new ArrayList<>(contentMap.keySet());
            mLabel = keyList.get(currIndex);
            //wire prev and next buttons
            if(currIndex == 0) mPreviousButton.setEnabled(false);
            ArrayList<String> finalKeyList = keyList;
            mPreviousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPrevious(finalKeyList);
                }
            });
            if(currIndex == contentMap.size()-1) mNextButton.setEnabled(false);
            ArrayList<String> finalKeyList1 = keyList;
            mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNext(finalKeyList1);
                }
            });
        }
        //handle links
        else{
            mPreviousButton.setVisibility(View.GONE);
            mNextButton.setVisibility(View.GONE);
            friendName = startIntent.getStringExtra(Globals.FRIEND_NAME_KEY);
            friendID = startIntent.getStringExtra(Globals.FRIEND_ID_KEY);
            keyList = new ArrayList<>(mOpenedGift.getLinks().keySet());
            mLabel = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);
        }


        //firebase stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //show the chosen gift
        if(mLabel != null){
            showGift(keyList);
        } else {
            showErrorDialog();
        }


    }

    /**
     * Previous button callback
     */
    public void onPrevious(ArrayList<String> keyList){
        currIndex--;
        if(currIndex==0) mPreviousButton.setEnabled(false);
        if(currIndex < keyList.size()-1) mNextButton.setEnabled(true);
        showGift(keyList);
    }

    /**
     * Next button callback
     */
    public void onNext(ArrayList<String> keyList){
        currIndex++;
        if(currIndex>0) mPreviousButton.setEnabled(true);
        if(currIndex == keyList.size()-1) mNextButton.setEnabled(false);
        showGift(keyList);
    }

    /**
     * Shows the gifts contents
     */
    public void showGift(ArrayList<String> keyList){
        mProgressBar.setVisibility(View.VISIBLE);
        DownloadMediaThread downloadMediaThread = new DownloadMediaThread(keyList);
        downloadMediaThread.start();
    }


    /**
     * Save the image or video to the user's gallery
     */
    public void onSave(){
        GalleryWriterThread galleryWriterThread = new GalleryWriterThread(this.getContentResolver());
        galleryWriterThread.run();
    }


    /**
     * Error pop-up for bad queries
     */
    public void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewContentsActivity.this);
        builder.setMessage("There has been an error. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Thread to download media from cloud
     */
    public class DownloadMediaThread extends Thread{
        private Bitmap bitmap;
        private ArrayList<String> keyList;

        public DownloadMediaThread(ArrayList<String> keyList){
            this.keyList = keyList;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                if(mLabel.startsWith("link")){
                    mImageView.setVisibility(View.GONE);
                    mVideoView.setVisibility(View.GONE);
                    mLinkView.setVisibility(View.VISIBLE);
                } else if(mLabel.startsWith("image")){
                    mLinkView.setVisibility(View.GONE);
                    mVideoView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                    mImageView.setImageBitmap(bitmap);
                    mSaveButton.setVisibility(View.VISIBLE);
                } else if(mLabel.startsWith("video")){
                    mLinkView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.GONE);
                    mVideoView.setVisibility(View.VISIBLE);
                    Log.d("LPC", "video download successful");
                    mSaveButton.setVisibility(View.VISIBLE);
                    mVideoView.start();
                }
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            mImageView.setVisibility(View.GONE);
            mVideoView.setVisibility(View.GONE);
            mLinkView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            if(fromMedia) mLabel = keyList.get(currIndex);
//            Log.d("LPC", "mLabel");
            if(mLabel.startsWith("link")){
                mLinkView.setText(mOpenedGift.getLinks().get(mLabel));
                handler.post(runnable);
            }
            //handle images
            else if(mLabel.startsWith("image")){
                //read from cloud
                String filePath = "gift/" + mOpenedGift.getHashValue()+ "/"+mLabel+".jpg";
                Log.d("LPC", "image file path: " + filePath);
                StorageReference imgRef = storageRef.child(filePath);
                File localFile = null;
                try {
                    localFile = File.createTempFile("tempImg", ".jpg");
                    Log.d("LPC", "local image file was made ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //populate temp file and fill in view
                File finalLocalFile = localFile;
                imgRef.getFile(localFile).addOnCompleteListener(ViewContentsActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            imageFile = finalLocalFile;
                            Log.d("LPC", "image download successful");
                            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                            handler.post(runnable);
                        } else {
                            Log.d("LPC", "image download failed");
                        }
                    }
                });
            }
            //handle videos
            else if(mLabel.startsWith("video")){
                mProgressBar.setVisibility(View.VISIBLE);
                //read from cloud
                String filePath = "gift/" + mOpenedGift.getHashValue()+ "/"+mLabel+".mp4";
                Log.d("LPC", "video file path: " + filePath);
                StorageReference imgRef = storageRef.child(filePath);
                File localFile = null;
                try {
                    localFile = File.createTempFile("tempVid", ".mp4");
                    Log.d("LPC", "local video file was made ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //populate temp file and fill in view
                File finalLocalFile = localFile;
                imgRef.getFile(localFile).addOnCompleteListener(ViewContentsActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            videoFile = finalLocalFile;
                            Log.d("LPC", "video download successful");
                            mVideoView.setVideoPath(videoFile.getPath());
                            handler.post(runnable);
                        } else {
                            Log.d("LPC", "video download failed");
                        }
                    }
                });
            }
        }
    }

    /**
     * Thread to write the file to the gallery
     */
    public class GalleryWriterThread extends Thread {
        private File file;
        private Uri newUri;
        private ContentResolver contentResolver;

        public GalleryWriterThread(ContentResolver contentResolver){
            this.contentResolver = contentResolver;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "File has been saved to gallery", Toast.LENGTH_SHORT)
                        .show();
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            //fill in content values for images
            if(mLabel.startsWith("image")) {
                file = new File(imageFile.getPath());
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                newUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            }
            //fill in content values for videos
            else  {
                file = new File(videoFile.getPath());
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
                newUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                values.put(MediaStore.Video.Media.MIME_TYPE, "image/jpg");
            }
            Log.d("LPC", "will save this file: "+file.getPath());
            //copy temp file to gallery file
            FileInputStream inputStream = null;
            FileOutputStream outputStream = null;
            try{
                inputStream = new FileInputStream(file);
                outputStream =  (FileOutputStream) getContentResolver().openOutputStream(newUri);
                byte[] buffer = new byte[1024];

                int length;
                while ((length = inputStream.read(buffer)) > 0){
                    outputStream.write(buffer, 0, length);
                }
                Log.d("LPC", "File copied successfully!!");
                handler.post(runnable);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}