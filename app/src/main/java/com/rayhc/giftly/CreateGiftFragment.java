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
        linkButton = getActivity().findViewById(R.id.link_button);
        imageButton = getActivity().findViewById(R.id.image_button);
        videoButton = getActivity().findViewById(R.id.video_button);
         newGift.setTimeCreated(System.currentTimeMillis());

         linkButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent = new Intent();
                 //start link activity
             }
         });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                //start image activity
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                //start video activity
            }
        });

        return v;
    }
}
