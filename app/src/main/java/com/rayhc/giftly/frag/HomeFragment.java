package com.rayhc.giftly.frag;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rayhc.giftly.CreateGiftActivity;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {
    private HashMap<String, String> giftsRecieved;
    private HashMap<String, String> giftsSent;
    private ListView recievedGifts;
    private ListView sentGifts;

    //create gift button
    private Button createGiftButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //get possible gift data
        Bundle extras = getArguments();
        if(extras != null && extras.getSerializable("SENT GIFT MAP") != null && extras.getSerializable("RECEIVED GIFT MAP") != null){
            Log.d("LPC", "home frag: read the gift maps");
            giftsSent = (HashMap<String, String>) extras.getSerializable("SENT GIFT MAP");
            Log.d("LPC", "home frag: sent gift map from bundle content: "+giftsSent.toString());
            giftsRecieved = (HashMap<String, String>) extras.getSerializable("RECEIVED GIFT MAP");
        }
    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        //wire lists
        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);

        //wire button
        createGiftButton = root.findViewById(R.id.create_gift_button);
        createGiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), CreateGiftActivity.class));
            }
        });

        //populate the sent gift list view
        if(giftsSent != null){
            ArrayList<String> sentGiftMessages = new ArrayList<>();
            sentGiftMessages.addAll(giftsSent.keySet());
            sentGifts.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, sentGiftMessages));
            sentGifts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    //download the gift
                    Intent intent;
                    intent = new Intent(getContext(), DownloadSplashActivity.class);
                    intent.putExtra("LABEL", label);
                    intent.putExtra("HASH VALUE", giftsSent.get(label));
                    intent.putExtra("FROM OPEN", true);
                    Log.d("LPC", "getting gift w hash: "+giftsSent.get(label));
                    startActivity(intent);
                }
            });
        }

        if(giftsRecieved != null){
            //populate the received gift list view
            ArrayList<String> receivedGiftMessages = new ArrayList<>();
            receivedGiftMessages.addAll(giftsRecieved.keySet());
            recievedGifts.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, receivedGiftMessages));
            recievedGifts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    //download the gift
                    Intent intent;
                    intent = new Intent(getContext(), DownloadSplashActivity.class);
                    intent.putExtra("LABEL", label);
                    intent.putExtra("HASH VALUE", giftsRecieved.get(label));
                    intent.putExtra("FROM OPEN", true);
                    intent.putExtra("FROM RECEIVE", true);
                    Log.d("LPC", "getting gift w hash: "+giftsRecieved.get(label));
                    startActivity(intent);
                }
            });
        }





        ListUtils.setDynamicHeight(recievedGifts);
        ListUtils.setDynamicHeight(sentGifts);

        //give them on click listeners

        return root;
    }

}
