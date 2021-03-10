package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import static android.util.Log.d;

public class AddFriendFromQRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend_from_q_r);

        //add a friend by scanning their QR code
        Intent intent = getIntent();
        String[] list = intent.getData().toString().split("\\?");
        String userId = "";
        if(list.length == 2) userId = list[1].replace("userId=", "");
        //get current user id
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentUserId= mSharedPref.getString("userId","");
        Log.d("iandebug", currentUserId + " | " + userId);
        if (!userId.equals("") && !currentUserId.equals("")) {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
            Query query = db.child("users").orderByChild("userId").equalTo(userId);
            String finalUserId = userId;
            //handle friend adding after successful scan
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User otherUser = UserManager.snapshotToUser(snapshot, finalUserId);
                    Query query = db.child("users").orderByChild("userId").equalTo(currentUserId);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User currentUser = UserManager.snapshotToUser(snapshot, currentUserId);
                            UserManager.sendAndAcceptFriendRequest(otherUser, currentUser);
                            Intent toHome = new Intent(getApplicationContext(), MainActivity.class);
                            Toast.makeText(getApplicationContext(), otherUser.getName() + " added", Toast.LENGTH_LONG).show();
                            startActivity(toHome);
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else this.finish();
    }
}