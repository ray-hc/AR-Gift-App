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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ReviewGiftActivity extends AppCompatActivity {

    private ListView mLinkList, mMediaList;
    private Gift gift;
    private boolean fromOpen;

    //TODO: change this placeholder
    private String [] data1 ={"Link 1", "Link 2", "Link 3", "Link 4", "Link 5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_gift);

        //wire views
        mLinkList = (ListView)findViewById(R.id.link_listView);
        mMediaList = (ListView)findViewById(R.id.media_listView);

        //get gift object
        Intent startIntent = getIntent();
        fromOpen = startIntent.getBooleanExtra("FROM OPEN", false);
        if(fromOpen){
            gift = (Gift) startIntent.getSerializableExtra("OPENED GIFT");
        } else {
            gift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
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
                if(fromOpen){
                    intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                } else{
                    intent = new Intent(getApplicationContext(), LinkActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                }
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
                    if(fromOpen){
                        intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    } else{
                        intent = new Intent(getApplicationContext(), LinkActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                        intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    }
                    startActivity(intent);
                }
                //go to VideoActivity if a video
                else if(label.startsWith("video")){
                    if(fromOpen){
                        intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    } else{
                        intent = new Intent(getApplicationContext(), LinkActivity.class);
                        intent.putExtra(Globals.CURR_GIFT_KEY, gift);
                        intent.putExtra(Globals.FILE_LABEL_KEY, label);
                        intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    }
                    startActivity(intent);
                }

            }
        });

        ListUtils.setDynamicHeight(mLinkList);
        ListUtils.setDynamicHeight(mMediaList);
    }


    /**
     * Util class so the listviews all fit
     */
    public static class ListUtils {
        public static void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
        }
    }

}