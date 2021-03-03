package com.rayhc.giftly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;

public class ReviewGiftActivity extends AppCompatActivity {

    private ListView mLinkList, mMediaList;
    private Gift gift;

    //TODO: differentiate if it is from editing or opening
    private boolean fromOpen;
    private TextView mMessageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_gift);

        //wire views
        mLinkList = (ListView)findViewById(R.id.link_listView);
        mMediaList = (ListView)findViewById(R.id.media_listView);

        //wire message view and hide
        mMessageView = (TextView) findViewById(R.id.message_view);
        mMessageView.setVisibility(View.GONE);

        //get gift object
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);

        //TODO: put in the message, if it is a gift with a message
        fromOpen = startIntent.getBooleanExtra("FROM OPEN", false);
        if(fromOpen && gift.getMessage() != null) {
            mMessageView.setVisibility(View.VISIBLE);
            mMessageView.setText("Message: "+gift.getMessage());
        }

        //populate the listview for media
        Log.d("LPC", "from download splash - contentType : "+gift.getContentType().toString());
        ArrayList<String> mediaFileNames = new ArrayList<>();
        mediaFileNames.addAll(gift.getContentType().keySet());

        //populate the listview for links
        Log.d("LPC", "from download splash - contentType : "+gift.getLinks().toString());
        ArrayList<String> linkNames = new ArrayList<>();
        linkNames.addAll(gift.getLinks().keySet());


        mLinkList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, linkNames));
        mMediaList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mediaFileNames));

        mLinkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String label = (String) parent.getItemAtPosition(position);
                Log.d("LPC", "media list view position click label: "+ label);
//                String dataPath = gift.getContentType().get(label);
//                Log.d("LPC", "media list view position click file: "+ dataPath);

                Intent intent;
                intent = new Intent(getApplicationContext(), LinkActivity.class);
                intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                intent.putExtra(Globals.FILE_LABEL_KEY, label);
                intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                startActivity(intent);
            }
        });

        mMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String label = (String) parent.getItemAtPosition(position);
                Log.d("LPC", "media list view position click label: "+ label);
//                String dataPath = gift.getContentType().get(label);
//                Log.d("LPC", "media list view position click file: "+ dataPath);

                Intent intent;
                //go to ImageActivity if an image
                if(label.startsWith("image")) {
                    intent = new Intent(getApplicationContext(), ImageActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    startActivity(intent);
                }
                //go to VideoActivity if a video
                else if(label.startsWith("video")){
                    intent = new Intent(getApplicationContext(), VideoActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    startActivity(intent);
                }

            }
        });

        ListUtils.setDynamicHeight(mLinkList);
        ListUtils.setDynamicHeight(mMediaList);
    }

}