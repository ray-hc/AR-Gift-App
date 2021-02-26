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

    private String [] data1 ={"Link 1", "Link 2", "Link 3", "Link 4", "Link 5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_gift);

        //wire views
        mLinkList = (ListView)findViewById(R.id.link_listView);
        mMediaList = (ListView)findViewById(R.id.media_listView);

        //get gift object
        Intent fromDownloadSpalsh = getIntent();
        gift = (Gift) fromDownloadSpalsh.getSerializableExtra("GIFT");

        Log.d("LPC", "from download splash - giftType : "+gift.getContentType().toString());
        ArrayList<String> mediaFileNames = new ArrayList<>();
        mediaFileNames.addAll(gift.getContentType().keySet());


        mLinkList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data1));
        mMediaList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mediaFileNames));

        mMediaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String label = (String) parent.getItemAtPosition(position);
                Log.d("LPC", "media list view position click label: "+ label);
                String dataPath = gift.getContentType().get(label);
                Log.d("LPC", "media list view position click file: "+ dataPath);

                Intent intent;
                //go to an ImageReview if an image
                if(label.startsWith("image")) {
                    intent = new Intent(getApplicationContext(), ImageActivity.class);
                    intent.putExtra("GIFT", gift);
                    intent.putExtra("FILE LABEL", label);
                    intent.putExtra("FROM REVIEW", true);
                    startActivity(intent);
                } else if(label.startsWith("video")){
                    intent = new Intent(getApplicationContext(), VideoActivity.class);
                    intent.putExtra("GIFT", gift);
                    intent.putExtra("FILE LABEL", label);
                    intent.putExtra("FROM REVIEW", true);
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

//    /**
//     * Media listview adapter
//     */
//    public class MediaListAdapter extends ArrayAdapter<HashMap<String, String>>{
//
//        public MediaListAdapter(@NonNull Context context, int resource, @NonNull List<HashMap<String, String>> objects) {
//            super(context, resource, objects);
//        }
//    }
}