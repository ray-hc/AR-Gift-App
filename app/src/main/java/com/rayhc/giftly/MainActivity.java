package com.rayhc.giftly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String NAV_ITEM_ID = "NAV_ITEM_ID";
    private static final int RC_SIGN_IN = 123;

    private User activityUser;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    private FriendsFragment friendsFragment;
    private CreateGiftFragment createGiftFragment;
    private HomeFragment homeFragment;

    private int navId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create the toolbar view and navigation view
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // get id to restore state if needed
        if (savedInstanceState != null) {
            navId = savedInstanceState.getInt(NAV_ITEM_ID);
        }
        else {
            navId = R.id.nav_home;
        }

        //define fragments
        friendsFragment = new FriendsFragment();
        createGiftFragment = new CreateGiftFragment();
        homeFragment = new HomeFragment();

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(navId).setChecked(true);

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

        navigateToFragment(navId);

        //go to db demo for now
//        Intent intent = new Intent(this, FirebaseDemoActivity.class);
//        startActivity(intent);

        //go to create gift
//        Gift gift = new Gift();
//        gift.setReceiver("Logan 2");
//        gift.setSender("Logan 1");
//        gift.setTimeCreated(100);
//        gift.setHashValue(gift.createHashValue());
//        gift.setContentType(new HashMap<>());
////        Intent intent = new Intent(this, ImageActivity.class);
////        Intent intent = new Intent(this, VideoActivity.class);
//        Intent intent = new Intent(this, LinkActivity.class);
//        intent.putExtra("GIFT", gift);
//        startActivity(intent);

    }

    // creates fragment if chosen
    public void navigateToFragment(int navId) {
        if (navId == R.id.nav_home){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, homeFragment, "HomeFragment").commit();
        }
        else if (navId == R.id.nav_create_gift){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, createGiftFragment, "CreateGiftFragment").commit();
        }
        else if (navId == R.id.nav_friends_list){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, friendsFragment, "FriendsFragment").commit();
        }
    }

    //navigates to and from fragment
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        navId = item.getItemId();

        drawerLayout.closeDrawer(GravityCompat.START);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToFragment((item.getItemId()));
            }
        }, 250);

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

        editor.putString("userId", currentUser.getUid());
        editor.apply();
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
                            activityUser = UserManager.snapshotToUser(snapshot, user.getUid());
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
}
