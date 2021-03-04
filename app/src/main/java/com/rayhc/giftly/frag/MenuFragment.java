package com.rayhc.giftly.frag;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class MenuFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_menu, container, false);

        return root;
    }

}
