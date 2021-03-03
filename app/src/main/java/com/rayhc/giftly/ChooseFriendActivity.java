package com.rayhc.giftly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;

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
        friendName = startIntent.getStringExtra("FRIEND NAME");
        friendID = startIntent.getStringExtra("FRIEND ID");

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

                //TODO: make sure this is passing the right data around
                Intent intent;
                intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("MAKING GIFT", true);
                intent.putExtra("FRIEND ID", friendID);
                intent.putExtra("FRIEND NAME", label);
                intent.putExtra("FROM FRIEND CHOOSE", true);
                intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("MAKING GIFT", true);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        intent.putExtra("FRIEND NAME", friendName);
        intent.putExtra("FRIEND ID", friendID);
        startActivity(intent);
    }
}