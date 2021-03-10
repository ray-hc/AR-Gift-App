package com.rayhc.giftly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChooseFriendActivity extends AppCompatActivity {

    private ListView friendListView;

    private HashMap<String, String> friendMap;

    private Gift mGift;
    private String friendName, friendID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_friend);

        Intent startIntent = getIntent();
        friendMap = (HashMap<String, String>) startIntent.getSerializableExtra("FRIEND MAP");
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        friendName = startIntent.getStringExtra(Globals.FRIEND_NAME_KEY);
        friendID = startIntent.getStringExtra(Globals.FRIEND_ID_KEY);

        friendListView = (ListView) findViewById(R.id.friends_listView);
        //populate the listview for media
        ArrayList<String> friendNames = new ArrayList<>();
        friendNames.addAll(friendMap.keySet());
        friendListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendNames));
        friendListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String label = (String) parent.getItemAtPosition(position);
                String friendID = friendMap.get(label);

                //add the chosen friend as the gift's recipient
                Intent intent;
                intent = new Intent(getApplicationContext(), CreateGiftActivity.class);
                intent.putExtra("MAKING GIFT", true);
                intent.putExtra(Globals.FRIEND_ID_KEY, friendID);
                intent.putExtra(Globals.FRIEND_NAME_KEY, label);
                intent.putExtra("FROM FRIEND CHOOSE", true);
                intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
                startActivity(intent);
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //custom handle back press to persist data
        Intent intent = new Intent(getApplicationContext(), CreateGiftActivity.class);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra(Globals.FRIEND_NAME_KEY, friendName);
        intent.putExtra(Globals.FRIEND_ID_KEY, friendID);
        startActivity(intent);
    }
}