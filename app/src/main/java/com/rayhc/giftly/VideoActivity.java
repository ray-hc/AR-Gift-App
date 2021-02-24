package com.rayhc.giftly;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class VideoActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    //widgets
    private VideoView mVideoView;
    private Button mChooseButton, mSaveButton, mCancelButton;
    private MyMediaController mMediaController;

    //data from gift
    private Gift gift;
    private String sender, recipient, hashValue;
    private HashMap<String, String> contentType;

    private Uri currentData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //get data from gift
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: "+gift.toString());

        //wire button and video view
        mChooseButton = (Button) findViewById(R.id.video_choose_button);
        mSaveButton = (Button) findViewById(R.id.video_save_button);
        mSaveButton.setEnabled(false);
        mCancelButton = (Button) findViewById(R.id.video_cancel_button);

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
        startActivity(splashIntent);

    }

    public void onCancel() {

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
            Log.d("LPC", "onActivityResult: currnent video path for vv: "+currentData.getPath());
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
}
