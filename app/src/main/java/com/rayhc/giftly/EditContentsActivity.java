package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class EditContentsActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    private HashMap<String, String> contentMap;
    private Gift mGift;
    private Uri currentData;
    private String friendName, friendID, mFileLabel;
    int currIndex = 0;



    private Button mChooseButton, mSaveButton, mCancelButton, mDeleteButton, mNextButton, mPreviousButton;
    private ImageView mImageView;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contents);


        //widgets
        mChooseButton = (Button) findViewById(R.id.contents_choose_button);
        mSaveButton = (Button) findViewById(R.id.contents_save_button);
        mDeleteButton = (Button) findViewById(R.id.contents_delete_button);
        mCancelButton = (Button) findViewById(R.id.contents_cancel_button);
        mNextButton = (Button) findViewById(R.id.next_button);
        mPreviousButton = (Button) findViewById(R.id.previous_button);
        mPreviousButton.setEnabled(false);
        mImageView = (ImageView) findViewById(R.id.chosen_contents_image);
        mVideoView = (VideoView) findViewById(R.id.chosen_contents_video);

        //gift data
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        contentMap = mGift.getContentType();
        if(currIndex == contentMap.size()-1) mNextButton.setEnabled(false);
        friendName = startIntent.getStringExtra(Globals.FRIEND_NAME_KEY);
        friendID = startIntent.getStringExtra(Globals.FRIEND_ID_KEY);

        ArrayList<String> keyList = new ArrayList<>(contentMap.keySet());
        handleMedia(keyList);

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
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext(keyList);
            }
        });
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevious(keyList);
            }
        });
    }

    public void handleMedia(ArrayList<String> keyList){
        mFileLabel = keyList.get(currIndex);
        if(mFileLabel.startsWith("image")){
            mVideoView.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
            mImageView.setImageURI(null);
            mImageView.setImageURI(Uri.parse(mGift.getContentType().get(mFileLabel)));
        } else{
            mImageView.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setVideoURI(null);
            mVideoView.setVideoURI(Uri.parse(mGift.getContentType().get(mFileLabel)));
            mVideoView.start();
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", mGift);
    }

    /**
     * Go to uploading splash screen on save button
     * Keep a class reference to the URI which gets altered on every successful image selection
     */

    //*******BUTTON CALLBACKS*******//

    public void onChoose() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(mFileLabel.startsWith("image")) intent.setType("image/*");
        else intent.setType("video/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);
    }

    /**
     * Update the content type of the gift with an image and its URI
     */
    public void onSave() {
        String key;
        if(currentData != null) {
            if (mFileLabel.startsWith("image"))
                key = "image_" + Globals.sdf.format(new Date(System.currentTimeMillis()));
            else key = "video_" + Globals.sdf.format(new Date(System.currentTimeMillis()));
            mGift.getContentType().put(key, "content://media/" + currentData.getPath());
            //delete the old file if its a replacement
            if (mFileLabel != null) mGift.getContentType().remove(mFileLabel);
            Log.d("LPC", "just saved image: " + mGift.getContentType().get(key));
        }
        Intent intent = new Intent(this, CreateGiftActivity.class);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra(Globals.FRIEND_NAME_KEY, friendName);
        intent.putExtra(Globals.FRIEND_ID_KEY, friendID);
        startActivity(intent);

    }

    /**
     * Remove the chosen image from the gifts contents
     */

    public void onDelete(){
        Intent intent = new Intent(this, CreateGiftActivity.class);
        mGift.getContentType().remove(mFileLabel);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra(Globals.FRIEND_NAME_KEY, friendName);
        intent.putExtra(Globals.FRIEND_ID_KEY", friendID);
        startActivity(intent);

    }

    public void onNext(ArrayList<String> keyList){
        currIndex++;
        if(currIndex>0) mPreviousButton.setEnabled(true);
        if(currIndex == keyList.size()-1) mNextButton.setEnabled(false);
        handleMedia(keyList);
    }

    public void onPrevious(ArrayList<String> keyList){
        currIndex--;
        if(currIndex==0) mPreviousButton.setEnabled(false);
        if(currIndex < keyList.size()-1) mNextButton.setEnabled(true);
        handleMedia(keyList);
    }

    //******ON ACTIVITY RESULT******/


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if gallery pick was successful, save the URI and populate the image view
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null) {
            Uri selectedData = data.getData();
            Log.d("LPC", "image activity selected data: "+selectedData.getPath());
            currentData = selectedData;
            if(mFileLabel.startsWith("image")){
                mImageView.setImageURI(null);
                mImageView.setImageURI(currentData);
            } else {
                mVideoView.setVideoURI(currentData);
                mVideoView.start();
            }
        }
    }

}