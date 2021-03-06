package com.rayhc.giftly;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.frag.FriendsFragment;
import com.rayhc.giftly.frag.HomeFragment;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.UserManager;
import com.rayhc.giftly.util.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.rayhc.giftly.util.Globals.GOT_GIFTS_KEY;
import static com.rayhc.giftly.util.Globals.REC_MAP_KEY;
import static com.rayhc.giftly.util.Globals.SENT_MAP_KEY;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    public static final String NAV_ITEM_ID = "NAV_ITEM_ID";
    private static final int RC_SIGN_IN = 123;

    private User activityUser;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FriendsFragment friendsFragment;
    private HomeFragment homeFragment;

    private int navId;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private boolean firstRun = true;
    SharedPreferences prefs;

    private Gift mGift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get id to restore state if needed
        if (savedInstanceState != null) {
            navId = savedInstanceState.getInt(NAV_ITEM_ID);
        }
        else {
            navId = R.id.nav_home;
        }

        // get first run info
        if(prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Log.d("LPC", "is prefs null?: "+(prefs==null));
        firstRun = prefs.getBoolean("isFirstRun", true);
        Log.d("LPC", "is first run? "+firstRun);

        // define fragments
        friendsFragment = new FriendsFragment();
        homeFragment = new HomeFragment();

        // get navigation
        BottomNavigationView navigationView = findViewById(R.id.bottomNavigationView);
        navigationView.setOnNavigationItemSelectedListener(this);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        //check permissions
        checkPermissions();

        //determine if we've gotten gifts yet
        Intent startIntent = getIntent();
        if(startIntent.getBooleanExtra(GOT_GIFTS_KEY, false)){
            HashMap<String, String> sentGiftsMap, receivedGiftsMap;
            sentGiftsMap = (HashMap<String, String>)startIntent.getSerializableExtra(SENT_MAP_KEY);
            receivedGiftsMap = (HashMap<String, String>)startIntent.getSerializableExtra(REC_MAP_KEY);
            Log.d("LPC", "sent gifts map in main activity: "+sentGiftsMap.toString());
            Log.d("LPC", "received gifts map in main activity: "+receivedGiftsMap.toString());
            //homeFragment = new HomeFragment(); <-- I don't think needed bc created new fragment on line 87.
            Bundle bundle = new Bundle();

            bundle.putSerializable(SENT_MAP_KEY, startIntent.getSerializableExtra(SENT_MAP_KEY));
            bundle.putSerializable(REC_MAP_KEY, startIntent.getSerializableExtra(REC_MAP_KEY));
            homeFragment.setArguments(bundle);
            navId = R.id.nav_home;
        }
//        //go to create gift fragment
//        else if(startIntent.getBooleanExtra("MAKING GIFT", false)){
//            createGiftFragment = new CreateGiftFragment();
//            Bundle bundle = new Bundle();
//
//            bundle.putString("FRIEND NAME", startIntent.getStringExtra("FRIEND NAME"));
//            bundle.putString("FRIEND ID", startIntent.getStringExtra("FRIEND ID"));
//
//
//            mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
//            Log.d("LPC", "container activity got gift: " + mGift.toString());
//            bundle.putSerializable(Globals.CURR_GIFT_KEY, mGift);
//
//            createGiftFragment.setArguments(bundle);
//
//            navId = R.id.nav_create_gift;
//        }

        else{
            if(mFirebaseUser == null){
                loadFirebase();
            } else{
                if(startIntent.getBooleanExtra("SENT GIFT", false)){
                    mGift = new Gift();
                    Log.d("LPC", "onCreate: made a new gift");
                }
                //go to download splash
                if(firstRun) {
                    Log.d("LPC", "run in firstRun");
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean("isFirstRun", Boolean.FALSE);
                    edit.apply();
                    Intent intent = new Intent(this, DownloadSplashActivity.class);
                    intent.putExtra("USER ID", mFirebaseUser.getUid());
                    intent.putExtra("GET GIFTS", true);
                    startActivity(intent);
                }
            }
            navId = R.id.nav_home;
        }

        navigateToFragment(navId);
    }

    // launches authentication intent with preferences.
    private void loadFirebase() {
        //starts login page
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

    @Override
    protected void onDestroy() {
        Log.d("LPC", "onDestroy: set pref to true");
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("isFirstRun", Boolean.TRUE);
        edit.apply();
        super.onDestroy();
    }

    // creates fragment if chosen
    public void navigateToFragment(int navId) {
        if (navId == R.id.nav_friends_list){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, friendsFragment, "FriendsFragment").commit();
        }
        else if (navId == R.id.nav_home){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, homeFragment, "HomeFragment").commit();
        }
        else if (navId == R.id.nav_settings){
//            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, createGiftFragment, "CreateGiftFragment").commit();
        }
    }

    //navigates to and from fragment
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        navId = item.getItemId();

        //needs to update the gift lists on home, if home selected
//        if(navId == R.id.nav_home){
////            Intent intent = new Intent(this, DownloadSplashActivity.class);
////            intent.putExtra("USER ID", mFirebaseUser.getUid());
////            intent.putExtra("GET GIFTS", true);
////            startActivity(intent);
//        }
//        else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    navigateToFragment((item.getItemId()));
                }
            }, 250);

//        }

        return true;
    }

    // To handle state changes
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NAV_ITEM_ID, navId);
    }

    private void onAuthSuccess(FirebaseUser currentUser) {
        String userId = currentUser.getUid();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(Globals.USER_ID_KEY, currentUser.getUid());
        editor.apply();

        //go to download splash
        if(firstRun) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("isFirstRun", Boolean.FALSE);
            edit.apply();
            Log.d("LPC", "run in first run in auth success");
            Intent intent = new Intent(this, DownloadSplashActivity.class);
            intent.putExtra("USER ID", userId);
            intent.putExtra("GET GIFTS", true);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("LPC", "onActivityResult: called");
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
            if (resultCode == RESULT_OK) {
                Log.d("LPC", "result ok");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Query query = db.child("users").orderByChild("userId").equalTo(user.getUid());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot){
                        Log.d("LPC", "does data snap exist?: "+snapshot.exists());
                        if(snapshot.exists()){
                            Log.d("LPC", "snapshot exists");
                            activityUser = UserManager.snapshotToUser(snapshot, user.getUid());
                            Log.d("LPC", "user exists");
                        }
                        else activityUser = UserManager.snapshotToEmptyUser(snapshot, user);
                        onAuthSuccess(user);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            } else {
                Log.d("iandebug", "User Login Failed");
            }

        }
    }

    /**
     * Request user permission to write to external storage
     */
    public void checkPermissions() {
        if (Build.VERSION.SDK_INT < 23)
            return;
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    /**
     * Deal with denial of storage permissions (adapted from class code)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
        }else if (grantResults[0] == PackageManager.PERMISSION_DENIED ){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("This permission is important for the app.")
                            .setTitle("Important permission required");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                            }

                        }
                    });
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }else{
                    //Never ask again and handle your app without permission.
                }
            }
        }
    }
}

