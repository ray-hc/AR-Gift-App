package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import static com.rayhc.giftly.util.Globals.REC_MAP_KEY;
import static com.rayhc.giftly.util.Globals.SENT_MAP_KEY;
import static com.rayhc.giftly.util.Globals.TAG;

public class LinkActivity extends AppCompatActivity {

    private Button mSaveButton, mCancelButton, mDeleteButton;
    private EditText mEditText;

    //get gift
    private Gift mGift;

    private String friendName, friendID;

    //from review
    private boolean mFromReview;
    private String mFileLabel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        //get data from gift
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        Log.d("LPC", "onCreate: saved gift: " + mGift.toString());
        mFromReview = startIntent.getBooleanExtra(Globals.FROM_REVIEW_KEY, false);
        mFileLabel = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);
        friendName = startIntent.getStringExtra("FRIEND NAME");
        friendID = startIntent.getStringExtra("FRIEND ID");

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
        if(startIntent.getBooleanExtra(Globals.FROM_REVIEW_KEY, false)){
            String label = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);
            mSaveButton.setEnabled(true);
            mDeleteButton.setVisibility(View.VISIBLE);
            mEditText.setText("");
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
        String link = mEditText.getText().toString();
        try {
            link = fixUpLink(link);
            new URL(link);
            String key = "link_" + Globals.sdf.format(System.currentTimeMillis());
            mGift.getLinks().put(key, link);
            //delete the old link if its a replacement

            if (mFileLabel != null) mGift.getLinks().remove(mFileLabel);
            Log.d("LPC", "set gift link to: " + link);
            Intent intent = new Intent(this, CreateGiftActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
            intent.putExtra("MAKING GIFT", true);
            intent.putExtra("FRIEND NAME", friendName);
            intent.putExtra("FRIEND ID", friendID);
            startActivity(intent);
        } catch (MalformedURLException e) {
            showErrorDialog();
        }
    }

    private String fixUpLink(String link) {

        String linkToRtrn = link;
        String[] linkParts = link.split(Pattern.quote("."));

        if (!link.contains("http://") && !link.contains("https://")) {
            if (linkParts.length == 2) {
                Log.d("rhc", "link size 2!");
                linkToRtrn = "http://www." + linkToRtrn;
            } else {
                Log.d("rhc", "link size not 2!");
                linkToRtrn = "http://" + linkToRtrn;
            }
        }
        return linkToRtrn;
    }
    /**
     * Remove the chosen link from the gifts contents
     */
    public void onDelete(){
        Intent intent = new Intent(this, CreateGiftActivity.class);
        mGift.getLinks().remove(mFileLabel);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra("FRIEND NAME", friendName);
        intent.putExtra("FRIEND ID", friendID);
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
