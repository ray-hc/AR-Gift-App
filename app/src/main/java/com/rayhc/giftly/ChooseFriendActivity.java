package com.rayhc.giftly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChooseFriendActivity extends AppCompatActivity {

    private ListView friendListView;

    private HashMap<String, String> friendMap;

    private Gift mGift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_friend);

        Intent startIntent = getIntent();
        friendMap = (HashMap<String, String>) startIntent.getSerializableExtra("FRIEND MAP");
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);

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

                Intent intent;
                intent = new Intent(getApplicationContext(), FragmentContainerActivity.class);
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
        Intent intent = new Intent(getApplicationContext(), FragmentContainerActivity.class);
        intent.putExtra("BACK PRESSED", true);
        startActivity(intent);
    }
}