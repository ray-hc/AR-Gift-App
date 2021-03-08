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
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rayhc.giftly.CreateGiftActivity;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.Startup;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.GiftAdapter;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {
    private HashMap<String, String> giftsReceived;
    private HashMap<String, String> giftsSent;
    private ListView recievedGifts;
    private ListView sentGifts;

    //create gift button
    private Button createGiftButton;
    private ImageButton refreshButton;

    //firebase user info
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private Startup startup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //get firebase user data
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        startup = (Startup) getActivity().getApplication();

        if(startup.getSentGiftMap() != null) {
            Log.d("LPC", "set sent gift map from startup");
            giftsSent = startup.getSentGiftMap();
        }
        if(startup.getReceivedGiftMap() != null) {
            Log.d("LPC", "set sent gift map from startup");
            giftsReceived = startup.getReceivedGiftMap();
        }

        //get possible gift data
        Bundle extras = getArguments();
//        if(extras != null && extras.getSerializable("SENT GIFT MAP") != null && extras.getSerializable("RECEIVED GIFT MAP") != null){
////            Log.d("LPC", "home frag: read the gift maps");
////            giftsSent = (HashMap<String, String>) extras.getSerializable("SENT GIFT MAP");
////            Log.d("LPC", "home frag: sent gift map from bundle content: "+giftsSent.toString());
////            giftsRecieved = (HashMap<String, String>) extras.getSerializable("RECEIVED GIFT MAP");
//        }
        //refresh if it needs to
        Log.d("LPC", "home - is bundle null?: "+(extras == null));
        if(extras != null)
            Log.d("LPC", "home - need refresh?: "+(extras.getBoolean("NEED REFRESH", false)));
        if(extras != null && extras.getBoolean("NEED REFRESH", false)){
            Log.d("LPC", "create gift going to refresh");
            Intent intent = new Intent(getContext(), DownloadSplashActivity.class);
            intent.putExtra("GET GIFTS", true);
            intent.putExtra("USER ID", mFirebaseUser.getUid());
            startActivity(intent);
        }

    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        //wire lists
        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);

        //wire buttons
        createGiftButton = root.findViewById(R.id.create_gift_button);
        createGiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CreateGiftActivity.class);
                intent.putExtra("SENT GIFT MAP", giftsSent);
                intent.putExtra("RECEIVED GIFT MAP", giftsReceived);
                startActivity(intent);
            }
        });
        refreshButton = root.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), DownloadSplashActivity.class);
                intent.putExtra("GET GIFTS", true);
                intent.putExtra("USER ID", mFirebaseUser.getUid());
                startActivity(intent);
            }
        });

        //populate the sent gift list view
        if(giftsSent != null){
            ArrayList<String> sentGiftMessages = new ArrayList<>();
            sentGiftMessages.addAll(giftsSent.keySet());
            sentGifts.setAdapter(new GiftAdapter(getActivity(), 0, sentGiftMessages));
//            sentGifts.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, sentGiftMessages));

            //commenting out to remove responsiveness - Logan
//            sentGifts.setOnItemClickListener((parent, view, position, id) -> {
//                String label = (String) parent.getItemAtPosition(position);
//                Log.d("LPC", "get hash: "+giftsSent.get(label));
//                //download the gift
//                Intent intent;
//                intent = new Intent(getContext(), DownloadSplashActivity.class);
//                intent.putExtra("HASH VALUE", giftsSent.get(label));
//                intent.putExtra("FROM OPEN", true);
////                intent.putExtra("SENT GIFT MAP",giftsSent);
//                intent.putExtra("USER ID", mFirebaseUser.getUid());
////                intent.putExtra("RECEIVED GIFT MAP",giftsRecieved);
//                Log.d("LPC", "getting gift w hash: "+giftsSent.get(label));
//                startActivity(intent);
//            });
        }

        if(giftsReceived != null){ // added || true for testing.
            //populate the received gift list view
            ArrayList<String> receivedGiftMessages = new ArrayList<>();
            //add in real gifts
            receivedGiftMessages.addAll(giftsReceived.keySet());

            // testing code. -- commenting out for now (Logan)
//            receivedGiftMessages.add("Happy Holidays!");
//            receivedGiftMessages.add("Your First Gift");

            recievedGifts.setAdapter(new GiftAdapter(getActivity(), 0, receivedGiftMessages));

            // put back for testing:
            recievedGifts.setOnItemClickListener((parent, view, position, id) -> {
                String label = (String) parent.getItemAtPosition(position);
                //download the gift
                Intent intent;
                intent = new Intent(getContext(), DownloadSplashActivity.class);
                intent.putExtra("HASH VALUE", giftsReceived.get(label));
                intent.putExtra("FROM RECEIVED", true);
                intent.putExtra("FROM OPEN", true);
//                intent.putExtra("SENT GIFT MAP",giftsSent);
                intent.putExtra("USER ID", mFirebaseUser.getUid());
//                intent.putExtra("RECEIVED GIFT MAP",giftsRecieved);
                Log.d("LPC", "getting gift w hash: "+giftsReceived.get(label));
                startActivity(intent);
            });

        }

        ListUtils.setDynamicHeight(recievedGifts);
        ListUtils.setDynamicHeight(sentGifts);


        return root;
    }


}