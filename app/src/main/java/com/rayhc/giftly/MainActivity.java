package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration myAppBarConfiguration;
    FloatingActionButton actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sets up navigation system
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home,
                R.id.nav_create_gift, R.id.nav_friends_list).setOpenableLayout(drawerLayout).
                build();
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, myAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        actionButton = findViewById(R.id.fab);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Create a new present", Toast.LENGTH_SHORT).show();
            }
        });

        //starts login page
//        Intent intent = new Intent(this, LoginActivity.class);
//        startActivity(intent);

        //go to db demo for now
//        Intent intent = new Intent(this, FirebaseDemoActivity.class);
//        startActivity(intent);

        //go to create gift
        Gift gift = new Gift();
        gift.setReceiver("Logan 2");
        gift.setSender("Logan 1");
        gift.setTimeCreated(100);
        gift.setHashValue(gift.createHashValue());
        gift.setContentType(new HashMap<>());
//        Intent intent = new Intent(this, ImageActivity.class);
//        Intent intent = new Intent(this, VideoActivity.class);
        Intent intent = new Intent(this, LinkActivity.class);
        intent.putExtra("GIFT", gift);
        startActivity(intent);

    }
}
