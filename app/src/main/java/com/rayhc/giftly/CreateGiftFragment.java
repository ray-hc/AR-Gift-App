package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;

public class CreateGiftFragment extends Fragment {
    private Button linkButton, imageButton, videoButton, reviewButton, sendButton;
    private Gift newGift;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getArguments();
        //get possible gift data
        if(extras != null && extras.getSerializable(Globals.CURR_GIFT_KEY) != null){
            newGift = (Gift) extras.getSerializable(Globals.CURR_GIFT_KEY);
            Log.d("LPC", "create frag: got gift from bundle");
            Log.d("LPC", "create frag: gift from bundle content: "+newGift.getContentType().toString());
        }
        //otherwise make this dummy gift
        else{
            Log.d("LPC", "create gift frag: making new gift");
            newGift = new Gift();
            newGift.setReceiver("Logan 2");
            newGift.setSender("Logan 1");
            newGift.setTimeCreated(100);
            newGift.setHashValue(newGift.createHashValue());
            newGift.setContentType(new HashMap<>());
            newGift.setLinks(new HashMap<>());
            newGift.setGiftType(new HashMap<>());
        }
    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = layoutInflater.inflate(R.layout.fragment_create_gift, container, false);
        Log.d("LPC", "CreateGiftFragment: onCreateView: " + newGift.toString());

        //wire in widgets
        linkButton = v.findViewById(R.id.link_button);
        imageButton = v.findViewById(R.id.image_button);
        videoButton = v.findViewById(R.id.video_button);
        reviewButton = v.findViewById(R.id.review_button);
        sendButton = v.findViewById(R.id.send_button);
        if(newGift.getContentType().size() == 0 && newGift.getLinks().size() == 0)
            sendButton.setEnabled(false);
//        newGift.setTimeCreated(System.currentTimeMillis());


        //click listeners for adding contents to the gift
        linkButton.setOnClickListener(v12 -> {
            Intent intent = new Intent(getActivity(), LinkActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            startActivity(intent);
        });

        imageButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(getActivity(), ImageActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            startActivity(intent);
        });

        videoButton.setOnClickListener(v13 -> {
            Intent intent = new Intent(getActivity(), VideoActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            startActivity(intent);
        });

        reviewButton.setOnClickListener(v14 -> {
            Intent intent = new Intent(getActivity(), ReviewGiftActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            startActivity(intent);
        });

        sendButton.setOnClickListener(v14 ->{
            Intent intent = new Intent(getActivity(), UploadingSplashActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            startActivity(intent);
        });

        return v;
    }

}
