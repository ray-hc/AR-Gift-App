package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.rayhc.giftly.frag.UserSearchFragment;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;

public class FindFriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        UserSearchFragment userSearchFragment = new UserSearchFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.search_fragment_container, userSearchFragment, "UserSearchFragment").commit();
    }
}
