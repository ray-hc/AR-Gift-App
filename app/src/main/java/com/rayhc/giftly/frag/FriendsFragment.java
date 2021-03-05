package com.rayhc.giftly.frag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.util.ListUtils;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

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

        Button b = (Button) view.findViewById(R.id.add_friend);

        friendsListView = (ListView) view.findViewById(R.id.friends_list);
        requestsListView = (ListView) view.findViewById(R.id.requests_list);

        getUserFromDB();

        ListUtils.setDynamicHeight(friendsListView);
        ListUtils.setDynamicHeight(requestsListView);

//        setHasOptionsMenu(true);

        return view;
    }

    public void onAddFriendsClick(View view) {
        DialogFragment.newFragment(R.id.add_friend).show(
                getActivity().getSupportFragmentManager(), getString(R.string.normal_dialog_fragment));
    }


    // To send a friend request
    public void addFriend(String addedFriendEmail) {
        Thread thread = new Thread(new Runnable() {
            private User user;

            @Override
            public void run() {
                try {
                    List<User> list = UserManager.searchUsersByEmail(addedFriendEmail);
                    user = list.get(0);
                    Log.d("iandebug", "" + list.size());
                    Log.d("iandebug", "" + list.get(0).getUserId());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (user != null){
                    UserManager.sendFriendRequest(activityUser, user.getUserId());
                }
                else{
                }

            }
        });
        thread.start();
    }

    public void getUserFromDB() {
        GetFriendsListThread thread = new GetFriendsListThread();
        thread.start();
    }

    public class GetFriendsListThread extends Thread {
        private int numFriends = -1;
        private int numFriendRequests = -1;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if((numFriends == -1 || friendsList.size()>numFriends) &&
                        (numFriendRequests == -1 || requestsList.size()>numFriendRequests))
                    return;

                Log.d("kitani", "Requests Added");

                friendsListAdapter = new MyFriendsListAdapter(context, R.layout.friend_entry, friendsList);
                requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, requestsList);

                friendsListView.setAdapter(friendsListAdapter);
                requestsListView.setAdapter(requestsListAdapter);

                Log.d("kitani", "Adapters Set");
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

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
                            numFriends = activityUser.getFriends().keySet().size();
                            for(String key: activityUser.getFriends().keySet()) {
                                String friendID = activityUser.getFriends().get(key);
                                Query query = db.child("users").orderByChild("userId").equalTo(friendID);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(friendID).child("name").getValue();
                                        friendsList.add(friendName);
                                        handler.post(runnable);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            }
                        }
                        Log.d("kitani", "Friends Added");

                        if (activityUser.getReceivedFriends() != null){
                            numFriendRequests = activityUser.getReceivedFriends().keySet().size();
                            for(String key: activityUser.getReceivedFriends().keySet()) {
                                String requestID = activityUser.getReceivedFriends().get(key);
                                Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String requestName = (String) snapshot.child(requestID).child("name").getValue();
                                        Log.d("kitani", "Friend Request from: " + requestName);
                                        requestsList.add(requestName);
                                        handler.post(runnable);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

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

            remove.setOnClickListener(v -> {
                Thread thread = new Thread(() -> {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                    if (activityUser.getFriends() != null) {
                        for (String key : activityUser.getFriends().keySet()) {
                            String friendID = activityUser.getFriends().get(key);
                            Query query = db.child("users").orderByChild("userId").equalTo(friendID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String friendName1 = (String) snapshot.child(friendID).child("name").getValue();

                                    if (friend.equals(friendName1)){
                                        UserManager.removeFriend(activityUser, friendID);
                                        getUserFromDB();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    }
                });
                thread.start();
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

            add.setOnClickListener(v -> {
                Thread thread = new Thread(() -> {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                    if (activityUser.getReceivedFriends() != null) {
                        for (String key : activityUser.getReceivedFriends().keySet()) {
                            String requestID = activityUser.getReceivedFriends().get(key);
                            Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String requestName = (String) snapshot.child(requestID).child("name").getValue();

                                    if (request.equals(requestName)){
                                        UserManager.acceptFriendRequest(activityUser, requestID);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    }
                });
                thread.start();
            });

            decline.setOnClickListener(v -> {
                Thread thread = new Thread(() -> {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                    if (activityUser.getReceivedFriends() != null) {
                        for (String key : activityUser.getReceivedFriends().keySet()) {
                            String requestID = activityUser.getReceivedFriends().get(key);
                            Query query = db.child("users").orderByChild("userId").equalTo(requestID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String requestName = (String) snapshot.child(requestID).child("name").getValue();

                                    if (request.equals(requestName)){
                                        UserManager.declineFriendRequest(activityUser, requestID);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    }
                });
                thread.start();
            });

            return convertView;
        }
    }
}
