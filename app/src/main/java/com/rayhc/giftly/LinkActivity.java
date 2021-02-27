package com.rayhc.giftly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.net.MalformedURLException;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LinkActivity extends AppCompatActivity {

    private Button mSaveButton, mCancelButton, mDeleteButton;
    private EditText mEditText;

    //get gift
    private Gift mGift;

    //from review
    private boolean mFromReview;
    private String mFileLabel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        //get data from gift
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: " + mGift.toString());
        mFromReview = startIntent.getBooleanExtra("FROM REVIEW", false);
        mFileLabel = startIntent.getStringExtra("FILE LABEL");

        //wire button and edit text
        mSaveButton = (Button) findViewById(R.id.choose_link_save_button);
        mCancelButton = (Button) findViewById(R.id.choose_link_cancel_button);
        mDeleteButton = (Button) findViewById(R.id.link_delete_button);
        mDeleteButton.setVisibility(View.GONE);
        mEditText = (EditText) findViewById(R.id.choose_link_field);


        //wire button callbacks
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
            mEditText.setText("");
//            Log.d("LPC", "review uri: "+ Uri.parse(mGift.getContentType().get(mFileLabel)));
            mEditText.setText(mGift.getLinks().get(mFileLabel));
        }


    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", mGift);
    }

    //*******BUTTON CALLBACKS*******//
    /**
     * Just needs to save the gift's new link, if valid
     * Or replace it if replacing
     */
    public void onSave() {
        String key;
        if(mFileLabel == null) {
            //TODO: possibly change to a readable time format
            key = "video_" + System.currentTimeMillis();
        } else {
            //TODO: instead create a new key with curr time and delete the old entry
            key = mFileLabel;
        }
        String link = mEditText.getText().toString();
        try {
            new URL(link);
            mGift.getLinks().put(key, link);
            Log.d("LPC", "set gift link to: " + link);
            Intent intent = new Intent(this, FragmentContainerActivity.class);
            intent.putExtra("GIFT", mGift);
            startActivity(intent);
        } catch (MalformedURLException e) {
            showErrorDialog();
        }

    }
    /**
     * Remove the chosen link from the gifts contents
     */
    public void onDelete(){
        Intent intent = new Intent(this, FragmentContainerActivity.class);
        mGift.getLinks().remove(mFileLabel);
        intent.putExtra("GIFT", mGift);
        startActivity(intent);
    }

    /**
     * Error pop-up for bad queries
     */
    public void showErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LinkActivity.this);
        builder.setMessage("There has been an error saving your web link. Please try again")
                .setTitle("Error")
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
