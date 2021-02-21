package com.rayhc.giftly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private User activityUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    public void onViewClick(View view){
        TextView tv = (TextView)findViewById(R.id.userTextView);
        if(this.activityUser != null)
            tv.setText(this.activityUser.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Query query = db.child("users").orderByChild("userId").equalTo(user.getUid());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot){
                        if(snapshot.exists()){
                            Log.d("iandebug", "fetched old one");
                            DataSnapshot storedUser = snapshot.child(user.getUid());
                            activityUser = storedUser.getValue(User.class);
                            Log.d("iandebug", "" + activityUser.getSentGifts());
                            activityUser.addSentGifts("some gift id");
                            db.child("users").child(activityUser.getUserId()).setValue(activityUser);
                            Log.d("iandebug", "old one: " + activityUser);
                        }else {
                            User tempUser = new User();

                            tempUser.setName(user.getDisplayName());
                            tempUser.setEmail(user.getEmail());
                            tempUser.setPhotoUri(user.getPhotoUrl().toString());
                            tempUser.setEmailVerified(user.isEmailVerified());
                            tempUser.setUserId(user.getUid());

                            tempUser.setReceivedGifts(new HashMap<>());
                            tempUser.setSentGifts(new HashMap<>());
                            tempUser.setReceivedFriends(new HashMap<>());
                            tempUser.setSentFriends(new HashMap<>());
                            tempUser.setFriends(new HashMap<>());

                            db.child("users").child(tempUser.getUserId()).setValue(tempUser);
                            activityUser = tempUser;

                            Log.d("iandebug", "created new user");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                /*
                //logan says this is poopoo
                db.child("users").child(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if(!task.isSuccessful()){
                            User tempUser = new User();

                            tempUser.setName(user.getDisplayName());
                            tempUser.setEmail(user.getEmail());
                            tempUser.setPhotoUri(user.getPhotoUrl().toString());
                            tempUser.setEmailVerified(user.isEmailVerified());
                            tempUser.setUserId(user.getUid());

                            tempUser.setReceivedGifts(new HashMap<>());
                            tempUser.setSentGifts(new HashMap<>());
                            tempUser.setReceivedFriends(new HashMap<>());
                            tempUser.setSentFriends(new HashMap<>());
                            tempUser.setFriends(new HashMap<>());

                            db.child("users").child(tempUser.getUserId()).setValue(tempUser);
                            activityUser = tempUser;

                            Log.d("iandebug", "created new user");
                        } else {
                            Log.d("iandebug", "fetched old one");
                            activityUser = task.getResult().getValue(User.class);
                            if(activityUser != null) {
                                activityUser.addSentGifts("some gift id");
                                db.child("users").child(activityUser.getUserId()).setValue(activityUser);
                            }
                            Log.d("iandebug", "old one: " + activityUser);
                        }
                    }
                });
                */

            } else {
                Log.d("iandebug", "User Login Failed");
            }
        }
    }

}