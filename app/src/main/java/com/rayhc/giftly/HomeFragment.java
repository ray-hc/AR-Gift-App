package com.rayhc.giftly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private ArrayList<Gift> giftsRecieved;
    private ArrayList<Gift> giftsSent;
    private ListView recievedGifts;
    private ListView sentGifts;

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        /*recievedGifts = getActivity().findViewById(R.id.inbox_gifts_recieved);
        sentGifts = getActivity().findViewById(R.id.inbox_gifts_sent);

        giftsRecieved = new ArrayList<>();
        giftsSent = new ArrayList<>();

        //test
        Gift gift1 = new Gift();
        Gift gift2 = new Gift();
        giftsRecieved.add(gift1);
        giftsSent.add(gift2);


        ArrayAdapter<Gift> receivedAdapter = new ArrayAdapter<>(getActivity(), R.layout.single_gift, giftsRecieved);
        ArrayAdapter<Gift> sentAdapter = new ArrayAdapter<Gift>(getActivity(), R.layout.single_gift, giftsSent);

        recievedGifts.setAdapter(receivedAdapter);
        sentGifts.setAdapter(sentAdapter);*/

        return root;
    }

}
