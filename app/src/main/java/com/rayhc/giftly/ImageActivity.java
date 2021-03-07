package com.rayhc.giftly;

import android.content.Intent;
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

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.Date;
import java.util.HashMap;

public class ImageActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

    //storage ref
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    //widgets
    private ImageView mImageView;
    private Button mChooseButton, mSaveButton, mCancelButton, mDeleteButton;

    //data from gift
    private Gift mGift;
    private Uri currentData;

    private String friendName, friendID;
    private HashMap<String, String> sentGiftMap, receivedGiftMap;

    //from review
    private boolean mFromReview;
    private String mFileLabel;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        //firebase stuff
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();

        //get data from gift
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        Log.d("LPC", "onCreate: saved gift: "+mGift.toString());
        Log.d("LPC", "image activity: gift contentType: "+mGift.getContentType().toString());
        mFromReview = startIntent.getBooleanExtra(Globals.FROM_REVIEW_KEY, false);
        mFileLabel = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);
        friendName = startIntent.getStringExtra("FRIEND NAME");
        friendID = startIntent.getStringExtra("FRIEND ID");
        sentGiftMap = (HashMap) startIntent.getSerializableExtra("SENT GIFT MAP");
        receivedGiftMap = (HashMap) startIntent.getSerializableExtra("RECEIVED GIFT MAP");

        //wire button and image view
        mChooseButton = (Button) findViewById(R.id.image_choose_button);
        mSaveButton = (Button) findViewById(R.id.image_save_button);
        mSaveButton.setEnabled(false);
        mDeleteButton = (Button) findViewById(R.id.image_delete_button);
        mDeleteButton.setVisibility(View.GONE);
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
            mSaveButton.setEnabled(true);
            mDeleteButton.setVisibility(View.VISIBLE);
            mImageView.setImageURI(null);
            Log.d("LPC", "review uri: "+Uri.parse(mGift.getContentType().get(mFileLabel)));
            mImageView.setImageURI(Uri.parse(mGift.getContentType().get(mFileLabel)));
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", mGift);
    }

    /**
     * Go to uploading splash screen on save button
     * <p>
     * Keep a class reference to the URI which gets altered on every successful image selection
     */

    //*******BUTTON CALLBACKS*******//
    public void onChoose() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);
    }

    /**
     * Update the content type of the gift with an image and its URI
     */
    public void onSave() {
        String key = "image_" + Globals.sdf.format(new Date(System.currentTimeMillis()));
        mGift.getContentType().put(key, "content://media/" + currentData.getPath());
        //delete the old file if its a replacement
        if(mFileLabel != null) mGift.getContentType().remove(mFileLabel);
        Log.d("LPC", "just saved image: "+mGift.getContentType().get(key));
        Intent intent = new Intent(this, CreateGiftActivity.class);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra("FRIEND NAME", friendName);
        intent.putExtra("FRIEND ID", friendID);
        intent.putExtra("SENT GIFT MAP", sentGiftMap);
        intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
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
        intent.putExtra("FRIEND NAME", friendName);
        intent.putExtra("FRIEND ID", friendID);
        intent.putExtra("SENT GIFT MAP", sentGiftMap);
        intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
        startActivity(intent);

    }

    //******ON ACTIVITY RESULT******//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if gallery pick was successful, save the URI and populate the image view
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null) {
            mSaveButton.setEnabled(true);
            Uri selectedData = data.getData();
            Log.d("LPC", "image activity selected data: "+selectedData.getPath());
            currentData = selectedData;
            mImageView.setImageURI(null);
            mImageView.setImageURI(currentData);
        }
    }
}
