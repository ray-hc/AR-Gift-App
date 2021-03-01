package com.rayhc.giftly.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.ListFragment;

import com.rayhc.giftly.R;

import java.util.ArrayList;

public class FriendsFragment extends ListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.friends_list_fragment, container, false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

    }

    public class ThreadLoader extends Thread {
        MyListAdapter adapter;
        Runnable setAdapter = () -> setListAdapter(adapter);

        public void run() {
            String[] friendArray = getResources().getStringArray(R.array.test_friends);

            ArrayList<String> friends =  new ArrayList<String>();

            for(String text:friendArray) {
                friends.add(text);
            }

            adapter = new MyListAdapter(getActivity(), R.layout.friend_entry, friends);
            getActivity().runOnUiThread(setAdapter);
        }
    }

    public class MyListAdapter extends ArrayAdapter<String> {
        Context context;

        public MyListAdapter(Context c, int resource, ArrayList<String> friends) {
            super(c, resource, friends);
            context = c;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String friend = getResources().getStringArray(R.array.test_friends)[position];

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.friend_entry, parent, false);

            TextView friendName = (TextView) convertView.findViewById(R.id.friend);

            friendName.setText(friend);

            return convertView;
        }

    }
}
