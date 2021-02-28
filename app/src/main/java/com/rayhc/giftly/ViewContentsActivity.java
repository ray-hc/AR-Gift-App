package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
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
import java.io.IOException;

public class ViewContentsActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    private TextView mLinkView;
    private ImageView mImageView;
    private VideoView mVideoView;
    private VideoActivity.MyMediaController mMediaController;
    private ProgressBar mProgressBar;

    private Gift mOpenedGift;
    private String mLabel;

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
        //add a media controller
        mMediaController = new VideoActivity.MyMediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        mProgressBar = (ProgressBar) findViewById(R.id.contents_porgress_bar);
        mProgressBar.setVisibility(View.GONE);

        //firebase stuff
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();


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
        if(mLabel.startsWith("link")){
            mLinkView.setVisibility(View.VISIBLE);
            mLinkView.setText(mOpenedGift.getLinks().get(mLabel));
        } else if(mLabel.startsWith("image")){
            mProgressBar.setVisibility(View.VISIBLE);
            String filePath = "gift/" + mOpenedGift.getHashValue()+ "/"+mLabel+".jpg";
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
            imgRef.getFile(localFile).addOnCompleteListener(ViewContentsActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mProgressBar.setVisibility(View.GONE);
                        mImageView.setVisibility(View.VISIBLE);
                        Log.d("LPC", "image download successful");
                        Bitmap bitmap = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        Log.d("LPC", "image download failed");
                    }
                }
            });
        } else if(mLabel.startsWith("video")){
            mProgressBar.setVisibility(View.VISIBLE);
            String filePath = "gift/" + mOpenedGift.getHashValue()+ "/"+mLabel+".mp4";
            Log.d("LPC", "video file path: " + filePath);
            StorageReference imgRef = storageRef.child(filePath);
            File localFile = null;
            try {
                localFile = File.createTempFile("tempVid", "mp4");
                Log.d("LPC", "local image file was made ");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File finalLocalFile = localFile;
            imgRef.getFile(localFile).addOnCompleteListener(ViewContentsActivity.this, new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        mProgressBar.setVisibility(View.GONE);
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