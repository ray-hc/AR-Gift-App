package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rayhc.giftly.frag.HomeFragment;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This is loading splash screen for the gift is being saved to the cloud
 * It will show a loading screen and run a thread to store the data in the background
 * Once the data is successfully uploaded, the user will be taken back to the home page
 */
public class UploadingSplashActivity extends AppCompatActivity {

    private Gift mGift;
    private String fromID, toID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading_splash);

        //data from create gift fragment
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        fromID = startIntent.getStringExtra("FROM USER ID");
        toID = startIntent.getStringExtra("TO USER ID");

        //start a thread to upload media to cloud
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("SENT GIFT", true);
        intent.putExtra("FROM USER ID", fromID);
        intent.putExtra("TO USER ID", toID);

        startActivity(intent);

    }

    @Override
    public void onBackPressed() {
        Log.d("LPC", "onBackPressed: in upload");
    }
}

