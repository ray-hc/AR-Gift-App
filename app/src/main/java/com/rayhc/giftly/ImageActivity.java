package com.rayhc.giftly;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class ImageActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    //widgets
    private ImageView mImageView;
    private Button mChooseButton, mSaveButton, mCancelButton;

    //data from gift
    private Gift gift;
    private String sender, recipient, hashValue;
    private HashMap<String, String> contentType;

    private Uri currentData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        //get data from gift
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: "+gift.toString());

        //wire button and image view
        mChooseButton = (Button) findViewById(R.id.image_choose_button);
        mSaveButton = (Button) findViewById(R.id.image_save_button);
        mSaveButton.setEnabled(false);
        mCancelButton = (Button) findViewById(R.id.image_cancel_button);
        mImageView = (ImageView) findViewById(R.id.chosen_image);

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
//        splashIntent.putExtra("SENDER", sender);
//        splashIntent.putExtra("RECIPIENT", recipient);
//        splashIntent.putExtra("HASH", hashValue);
//        splashIntent.putExtra("CONTENT_TYPE", contentType);
        startActivity(splashIntent);

    }

    public void onCancel() {

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
}
