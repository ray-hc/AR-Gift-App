package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class CreateGiftFragment extends Fragment {
    private Button linkButton, imageButton, videoButton;
    private Gift newGift;

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v =  layoutInflater.inflate(R.layout.fragment_create_gift, container, false);
        newGift = new Gift();
        linkButton = v.findViewById(R.id.link_button);
        imageButton = v.findViewById(R.id.image_button);
        videoButton = v.findViewById(R.id.video_button);
         newGift.setTimeCreated(System.currentTimeMillis());

         linkButton.setOnClickListener(v12 -> {
             Intent intent = new Intent(getActivity(), LinkActivity.class);
             startActivity(intent);
             //pass back string and save to string database (DB 1)
         });

        imageButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(getActivity(), ImageActivity.class);
            //extras here
            startActivity(intent);
            //save to image/vid db (DB 2)
        });

        videoButton.setOnClickListener(v13 -> {
            Intent intent = new Intent(getActivity(), VideoActivity.class);
            //extras here
            startActivity(intent);
            //save to image/vid db (DB 2)
        });

        return v;
    }

}
