package com.rayhc.giftly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.rayhc.giftly.util.Gift;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private ArrayList<Gift> giftsRecieved;
    private ArrayList<Gift> giftsSent;
    private ListView recievedGifts;
    private ListView sentGifts;

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);

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

        return root;
    }

}
