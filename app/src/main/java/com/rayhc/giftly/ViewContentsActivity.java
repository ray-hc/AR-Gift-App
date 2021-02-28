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
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
    private Button mSaveButton;

    //gift data
    private Gift mOpenedGift;
    private String mLabel;

    //files
    private File imageFile;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_contents);

        //get intent data
        Intent startIntent = getIntent();
        mOpenedGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        mLabel = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);

        //wire widgets and set them all to gone
        mLinkView = (TextView) findViewById(R.id.link_gift);
        mLinkView.setVisibility(View.GONE);
        mImageView = (ImageView) findViewById(R.id.image_gift);
        mImageView.setVisibility(View.GONE);
        mVideoView = (VideoView) findViewById(R.id.video_gift);
        mVideoView.setVisibility(View.GONE);
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

        //firebase stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //show the chosen gift
        if(mLabel != null){
            showGift();
        } else {
            showErrorDialog();
        }

    }

    /**
     * Shows the gifts contents
     */
    public void showGift(){
        //handle links
        if(mLabel.startsWith("link")){
            mLinkView.setVisibility(View.VISIBLE);
            mLinkView.setText(mOpenedGift.getLinks().get(mLabel));
        }
        //handle images
        else if(mLabel.startsWith("image")){
            mProgressBar.setVisibility(View.VISIBLE);
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
                        mProgressBar.setVisibility(View.GONE);
                        mImageView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "image download successful");
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        mImageView.setImageBitmap(bitmap);
                        mSaveButton.setVisibility(View.VISIBLE);
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
                        mProgressBar.setVisibility(View.GONE);
                        mVideoView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "video download successful");
                        mSaveButton.setVisibility(View.VISIBLE);
                        mVideoView.setVideoPath(videoFile.getPath());
                        mVideoView.start();
                    } else {
                        Log.d("LPC", "video download failed");
                    }
                }
            });
        }
    }


    /**
     * Save the image or video to the user's gallery
     */
    public void onSave(){
        File file;
        Uri newUri;
        //fill in content values for images
        if(mLabel.startsWith("image")) {
            file = new File(imageFile.getPath());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            newUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        }
        //fill in content values for videos
        else  {
            file = new File(videoFile.getPath());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            newUri = this.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
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
}