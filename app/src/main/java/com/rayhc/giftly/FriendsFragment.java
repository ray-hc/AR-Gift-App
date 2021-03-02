package com.rayhc.giftly;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    private User activityUser;
    private ArrayList<String> friendsList;
    private ArrayList<String> requestsList;
    private Context context;

    private MyFriendsListAdapter friendsListAdapter;
    private MyRequestsListAdapter requestsListAdapter;

    private ListView friendsListView;
    private ListView requestsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.friends_list_fragment, container, false);

        friendsList = new ArrayList<String>();
        requestsList = new ArrayList<String>();

        context = this.getActivity().getApplicationContext();

        friendsListView = (ListView) view.findViewById(R.id.friends_list);
        requestsListView = (ListView) view.findViewById(R.id.requests_list);

        getUserFromDB();

        ListUtils.setDynamicHeight(friendsListView);
        ListUtils.setDynamicHeight(requestsListView);

        setHasOptionsMenu(true);

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

    // To send a friend request
    public void addFriend(String addedFriendEmail) {
        Thread thread = new Thread(new Runnable() {
            User user;

            @Override
            public void run() {
                try {
                    user = UserManager.searchUsersByEmail(addedFriendEmail).get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (user != null){
                    UserManager.sendFriendRequest(activityUser, user.getUserId());
                }
                else{
                    Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show();
                }

            }
        });
        thread.start();
    }

    public void getUserFromDB() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                String displayUserID = sharedPref.getString("userId",null);

                Log.d("kitani", "User ID: " + displayUserID);

                Query query = db.child("users").orderByChild("userId").equalTo(displayUserID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            activityUser = UserManager.snapshotToUser(snapshot, displayUserID);

                            if (activityUser == null){
                                Log.d("kitani", "User object is null.");
                            }

                            if(activityUser.getFriends() != null){
                                for(String key: activityUser.getFriends().keySet()) {
                                    String friendID = activityUser.getFriends().get(key);
                                    Query query = db.child("users").orderByChild("userId").equalTo(friendID);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String friendName = (String) snapshot.child(friendID).child("name").getValue();
                                            friendsList.add(friendName);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                            Log.d("kitani", "Friends Added");

                            if (activityUser.getReceivedFriends() != null){
                                for(String key: activityUser.getReceivedFriends().keySet()) {
                                    String requestID = activityUser.getReceivedFriends().get(key);
                                    Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String requestName = (String) snapshot.child(requestID).child("name").getValue();
                                            requestsList.add(requestName);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                            Log.d("kitani", "Requests Added");


                            friendsListAdapter = new MyFriendsListAdapter(context, R.layout.friend_entry, friendsList);
                            requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, requestsList);

                            friendsListView.setAdapter(friendsListAdapter);
                            requestsListView.setAdapter(requestsListAdapter);

                            Log.d("kitani", "Adapters Set");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
        });
        thread.start();
    }

    public class MyFriendsListAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> f;
        int res;

        public MyFriendsListAdapter(Context c, int resource, ArrayList<String> friends) {
            super(c, resource, friends);
            this.context = c;
            this.res = resource;
            this.f = friends;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String friend = f.get(position);
            convertView = LayoutInflater.from(getContext()).inflate(res, parent, false);
            Log.d("kitani", "Friend: " + friend );

            TextView friendName = (TextView) convertView.findViewById(R.id.friend);
            friendName.setText(friend);

            Button remove = (Button) convertView.findViewById(R.id.remove_button);

            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            return convertView;
        }
    }

    public class MyRequestsListAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> r;
        int res;

        public MyRequestsListAdapter(Context c, int resource, ArrayList<String> requests) {
            super(c, resource, requests);
            this.context = c;
            this.res = resource;
            this.r = requests;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String request = r.get(position);
            convertView = LayoutInflater.from(getContext()).inflate(res, parent, false);

            TextView friendName = (TextView) convertView.findViewById(R.id.friend_request);
            friendName.setText(request);

            Button add = (Button) convertView.findViewById(R.id.add_button);
            Button decline = (Button) convertView.findViewById(R.id.decline_button);

            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                            if (activityUser.getReceivedFriends() != null) {
                                for (String key : activityUser.getReceivedFriends().keySet()) {
                                    String requestID = activityUser.getReceivedFriends().get(key);
                                    Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String requestName = (String) snapshot.child(requestID).child("name").getValue();

                                            if (request == requestName){
                                                UserManager.acceptFriendRequest(activityUser, requestID);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                        }
                    });
                    thread.start();
                }
            });

            decline.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                            if (activityUser.getReceivedFriends() != null) {
                                for (String key : activityUser.getReceivedFriends().keySet()) {
                                    String requestID = activityUser.getReceivedFriends().get(key);
                                    Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String requestName = (String) snapshot.child(requestID).child("name").getValue();

                                            if (request == requestName){
                                                UserManager.declineFriendRequest(activityUser, requestID);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    });
                                }
                            }
                        }
                    });
                    thread.start();
                }
            });

            return convertView;
        }
    }
}
