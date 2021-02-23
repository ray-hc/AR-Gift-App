package com.rayhc.giftly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class CreateGiftFragment extends Fragment {

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return layoutInflater.inflate(R.layout.fragment_create_gift, container, false);
    }
}
