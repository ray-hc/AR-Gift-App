package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private ArrayList<Gift> giftsRecieved;
    private ArrayList<Gift> giftsSent;
    private ListView recievedGifts;
    private ListView sentGifts;
    private Button mOpenButton;

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);
        mOpenButton = root.findViewById(R.id.open_gift_button);

        giftsRecieved = new ArrayList<>();
        giftsSent = new ArrayList<>();

        //test
        Gift gift1 = new Gift();
        Gift gift2 = new Gift();
        Gift gift3 = new Gift();
        Gift gift4 = new Gift();
        Gift gift5 = new Gift();
        gift1.setSender("Friend 1");
        gift2.setSender("Friend 2");
        gift3.setSender("Friend 3");
        gift4.setSender("Friend 4");
        gift5.setSender("Friend 5");

        giftsRecieved.add(gift1);
        giftsRecieved.add(gift2);
        giftsRecieved.add(gift3);
        giftsRecieved.add(gift4);
        giftsRecieved.add(gift5);

        giftsSent.add(gift1);
        giftsSent.add(gift2);
        giftsSent.add(gift3);
        giftsSent.add(gift4);
        giftsSent.add(gift5);


        ArrayAdapter<Gift> receivedAdapter = new ArrayAdapter<>(getActivity(), R.layout.single_gift, giftsRecieved);
        ArrayAdapter<Gift> sentAdapter = new ArrayAdapter<>(getActivity(), R.layout.single_gift, giftsSent);

        recievedGifts.setAdapter(receivedAdapter);
        sentGifts.setAdapter(sentAdapter);


        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String recipientID = "Logan 2";
                String hashValue = "d41d8cd98f00b204e9800998ecf8427e";
                Intent intent = new Intent(getContext(), DownloadSplashActivity.class);
                intent.putExtra("RECIPIENT ID", recipientID);
                intent.putExtra("HASH VALUE", hashValue);
                startActivity(intent);
            }
        });

        return root;
    }

}
