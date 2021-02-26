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
    private Button linkButton, imageButton, videoButton, reviewButton;
    private Gift newGift;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (newGift == null) {
            newGift = new Gift();
            newGift.setReceiver("Logan 2");
            newGift.setSender("Logan 1");
            newGift.setTimeCreated(100);
            newGift.setHashValue(newGift.createHashValue());
            newGift.setContentType(new HashMap<>());
            newGift.setGiftType(new HashMap<>());
        }
    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = layoutInflater.inflate(R.layout.fragment_create_gift, container, false);
        Log.d("LPC", "CreateGiftFragment: onCreateView: " + newGift.toString());
        linkButton = v.findViewById(R.id.link_button);
        imageButton = v.findViewById(R.id.image_button);
        videoButton = v.findViewById(R.id.video_button);
        reviewButton = v.findViewById(R.id.review_button);
        newGift.setTimeCreated(System.currentTimeMillis());

        linkButton.setOnClickListener(v12 -> {
            Intent intent = new Intent(getActivity(), LinkActivity.class);
            intent.putExtra("GIFT", newGift);
            startActivity(intent);
            //pass back string and save to string database (DB 1)
        });

        imageButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(getActivity(), ImageActivity.class);
            //extras here
            intent.putExtra("GIFT", newGift);
            startActivity(intent);
            //save to image/vid db (DB 2)
        });

        videoButton.setOnClickListener(v13 -> {
            Intent intent = new Intent(getActivity(), VideoActivity.class);
            //extras here
            intent.putExtra("GIFT", newGift);
            startActivity(intent);
            //save to image/vid db (DB 2)
        });

        reviewButton.setOnClickListener(v14 -> {
            Intent intent = new Intent(getActivity(), DownloadSplashActivity.class);
            //extras here
            intent.putExtra("GIFT", newGift);
            startActivity(intent);
        });

        return v;
    }

}
