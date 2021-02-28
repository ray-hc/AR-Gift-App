package com.rayhc.giftly;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FriendsFragment extends ListFragment {
    private User activityUser;
    private ArrayList<String> friendsList;
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.friends_list_fragment, container, false);

        Activity activity = this.getActivity();

        context = this.getActivity().getApplicationContext();

        getUserFromDB();

        ThreadLoader tl = new ThreadLoader();
        tl.start();

        return view;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.add_friend:
                DialogFragment.newFragment(R.id.add_friend).show(
                        getActivity().getSupportFragmentManager(), getString(R.string.normal_dialog_fragment));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

    }

    public class ThreadLoader extends Thread {
        MyListAdapter adapter;
        Runnable setAdapter = () -> setListAdapter(adapter);

        public void run() {
            String[] friendArray = activityUser.getFriends().values().toArray(new String[0]);

            ArrayList<String> friends =  new ArrayList<String>();

            for(String text:friendArray) {
                friends.add(text);
            }

            adapter = new MyListAdapter(getActivity(), R.layout.friend_entry, friends);
            getActivity().runOnUiThread(setAdapter);
        }
    }

    public void getUserFromDB() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Query query = db.child("users").orderByChild("userId").equalTo(user.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    activityUser = UserManager.snapshotToUser(snapshot, user.getUid());
                } else activityUser = UserManager.snapshotToEmptyUser(snapshot, user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public static void setAddedFriend(String addedFriend) {

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
