package com.rayhc.giftly;

import androidx.annotation.NonNull;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ReviewGiftActivity extends AppCompatActivity {

    private ListView mLinkList, mMediaList;
    private Gift gift;

    private boolean fromOpen;
    private TextView mMessageView;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private HashMap<String, String> sentGiftMap, receivedGiftMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_gift);

        //firebase user data
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        //wire views
        mLinkList = (ListView)findViewById(R.id.link_listView);
        mMediaList = (ListView)findViewById(R.id.media_listView);

        //wire message view and hide
        mMessageView = (TextView) findViewById(R.id.message_view);
        mMessageView.setVisibility(View.GONE);

        //get gift object
        Intent startIntent = getIntent();
        gift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        sentGiftMap = (HashMap) startIntent.getSerializableExtra("SENT GIFT MAP");
        receivedGiftMap = (HashMap) startIntent.getSerializableExtra("RECEIVED GIFT MAP");

        fromOpen = startIntent.getBooleanExtra("FROM OPEN", false);
        if(fromOpen && gift.getMessage() != null) {
            mMessageView.setVisibility(View.VISIBLE);
            mMessageView.setText("Message: "+gift.getMessage());
        }

        //populate the listview for media
        if(gift.getContentType() != null){
            Log.d("LPC", "from download splash - contentType : "+gift.getContentType().toString());
            ArrayList<String> mediaFileNames = new ArrayList<>();
            mediaFileNames.addAll(gift.getContentType().keySet());
            mMediaList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mediaFileNames));
            mMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    Log.d("LPC", "media list view position click label: "+ label);

                    Intent intent;
                    //go to ViewContents if opening a gift, else go to ImageActivity
                    if(label.startsWith("image")) {
                        if(fromOpen) intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                        else intent = new Intent(getApplicationContext(), ImageActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                        intent.putExtra("FROM OPEN", startIntent.getBooleanExtra("FROM OPEN", false));
                        intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                        intent.putExtra("SENT GIFT MAP", sentGiftMap);
                        intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
                        startActivity(intent);
                    }
                    //go to ViewContents if opening a gift, else go to VideoActivity
                    else if(label.startsWith("video")){
                        if(fromOpen) intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                        else intent = new Intent(getApplicationContext(), VideoActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                        intent.putExtra("FROM OPEN", startIntent.getBooleanExtra("FROM OPEN", false));
                        intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                        intent.putExtra("SENT GIFT MAP", sentGiftMap);
                        intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
                        startActivity(intent);
                    }

                }
            });
        }


        //populate the listview for links
        if(gift.getLinks() != null){
            Log.d("LPC", "from download splash - contentType : "+gift.getLinks().toString());
            ArrayList<String> linkNames = new ArrayList<>();
            linkNames.addAll(gift.getLinks().keySet());
            mLinkList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, linkNames));
            mLinkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    Log.d("LPC", "media list view position click label: "+ label);

                    Intent intent;
                    //go to ViewContents if opening a gift, else go to LinkActivity
                    if(fromOpen) intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                    else intent = new Intent(getApplicationContext(), LinkActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    intent.putExtra("SENT GIFT MAP", sentGiftMap);
                    intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
                    startActivity(intent);
                }
            });
        }


        ListUtils.setDynamicHeight(mLinkList);
        ListUtils.setDynamicHeight(mMediaList);
    }
    /**
     * Go back to ReviewGift on back pressed, if from open
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(fromOpen){
            Intent intent = new Intent(this, DownloadSplashActivity.class);
            intent.putExtra("USER ID", mFirebaseUser.getUid());
            intent.putExtra("GET GIFTS", true);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        }
    }
}