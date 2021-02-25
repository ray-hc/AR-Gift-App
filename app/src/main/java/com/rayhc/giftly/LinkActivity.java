package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.net.MalformedURLException;
import java.net.URL;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LinkActivity extends AppCompatActivity {

    private Button mSaveButton, mCancelButton;
    private EditText mEditText;
    private String mInput;

    private Gift gift;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        //get data from gift
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra("GIFT");
        Log.d("LPC", "onCreate: saved gift: " + gift.toString());

        //wire button and edit text
        mSaveButton = (Button) findViewById(R.id.choose_link_save_button);
        mCancelButton = (Button) findViewById(R.id.choose_link_cancel_button);
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


    }

    /**
     * Just needs to save the gift's new link, if valid
     */
    public void onSave() {
        String link = mEditText.getText().toString();
        try {
            new URL(link);
            gift.addLink(link);
            Log.d("LPC", "set gift link to: " + link);
        } catch (MalformedURLException e) {
            showErrorDialog();
        }

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
