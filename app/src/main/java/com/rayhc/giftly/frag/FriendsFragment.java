package com.rayhc.giftly.frag;

import android.content.Context;
import android.content.Intent;
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
import com.rayhc.giftly.CreateGiftActivity;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.FindFriendsActivity;
import com.rayhc.giftly.Startup;
import com.rayhc.giftly.util.GiftAdapter;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
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

    private Startup startup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.friends_list_fragment, container, false);

        friendsList = new ArrayList<>();
        requestsList = new ArrayList<>();

        context = this.getActivity().getApplicationContext();

        startup = (Startup) getActivity().getApplication();

        //wire button to search for friends
        Button freindsSearch = view.findViewById(R.id.add_friend);
        freindsSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FindFriendsActivity.class);
                startActivity(intent);
            }
        });

        //wite in list views
        friendsListView = view.findViewById(R.id.friends_list);
        requestsListView = view.findViewById(R.id.requests_list);

        //populate list views if there is persistent data
        if(!startup.getFriendsList().isEmpty()) {
            friendsList = new ArrayList<>();
            friendsList = startup.getFriendsList();
            friendsListAdapter = new MyFriendsListAdapter(context, 0, friendsList);
            friendsListView.setAdapter(friendsListAdapter);
            ListUtils.setDynamicHeight(friendsListView);
        }
        if(!startup.getFriendRequestsList().isEmpty()) {
            requestsList = new ArrayList<>();
            requestsList = startup.getFriendRequestsList();
            requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, requestsList);
            requestsListView.setAdapter(requestsListAdapter);
            ListUtils.setDynamicHeight(requestsListView);
        }

        //start threads to possibly update friends lists data
        getUserFromDB();

        return view;
    }

    /**
     * Start threads to possibly update friends lists data
     */
    public void getUserFromDB() {
        GetFriendsListThread friendThread = new GetFriendsListThread();
        GetRequestsThread reqThread = new GetRequestsThread();
        friendThread.start();
        reqThread.start();
    }

    /**
     * Thread to get mutual friends
     */
    public class GetFriendsListThread extends Thread {
        private ArrayList<String> threadFriendsList = new ArrayList<>();
        private int numFriends = -1;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if((numFriends == -1 || threadFriendsList.size()<numFriends))
                    return;

                Log.d("kitani", "Requests Added");

                Log.d("CHECKING THREAD", "" + threadFriendsList.toString());

                //if the db data is different than application data, update it and the list view
                if (!threadFriendsList.equals(startup.getFriendsList())) {
                    friendsList = threadFriendsList;
                    startup.setFriendsList(threadFriendsList);
                    friendsListAdapter = new MyFriendsListAdapter(context, 0, threadFriendsList);
                    friendsListView.setAdapter(friendsListAdapter);
                    Log.d("CHECKING THREAD", "set friend list adapter to " + threadFriendsList.toString());
                    ListUtils.setDynamicHeight(friendsListView);
                }

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

            //query db for the user's friends
            Query query = db.child("users").orderByChild("userId").equalTo(displayUserID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        activityUser = UserManager.snapshotToUser(snapshot, displayUserID);

                        //edge checks
                        if (activityUser == null){
                            Log.d("kitani", "User object is null.");
                        }
                        if(activityUser.getFriends() == null){
                            handler.post(runnable);
                        }

                        //get the friends data
                        if(activityUser.getFriends() != null){
                            numFriends = activityUser.getFriends().keySet().size();
                            for(String key: activityUser.getFriends().keySet()) {
                                String friendID = activityUser.getFriends().get(key);
                                Query query = db.child("users").orderByChild("userId").equalTo(friendID);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(friendID).child("name").getValue();
                                        threadFriendsList.add(friendName);
                                        handler.post(runnable);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });
                            }
                        }
                        Log.d("kitani", "Friends Added");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

    }

    /**
     * Thread to get waiting friend requests
     */
    public class GetRequestsThread extends Thread {
        private ArrayList<String> threadRequestsList = new ArrayList<>();
        private int numFriendRequests = -1;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if((numFriendRequests == -1 || threadRequestsList.size()<numFriendRequests))
                    return;

                Log.d("kitani", "Requests Added");

                //if the db data is different than application data, update it and the list view
                if (!threadRequestsList.equals(startup.getFriendRequestsList())) {
                    requestsList = threadRequestsList;
                    startup.setFriendRequestsList(threadRequestsList);
                    requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, threadRequestsList);
                    requestsListView.setAdapter(requestsListAdapter);
                    ListUtils.setDynamicHeight(requestsListView);
                }

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

            //query the friend request data from db
            Query query = db.child("users").orderByChild("userId").equalTo(displayUserID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        activityUser = UserManager.snapshotToUser(snapshot, displayUserID);

                        //edge checks
                        if (activityUser == null){
                            Log.d("kitani", "User object is null.");
                        }
                        if(activityUser.getReceivedFriends() == null){
                            handler.post(runnable);
                        }

                        //get the friend request data
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
                                        threadRequestsList.add(requestName);
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

    /**
     * Adapter for friends list
     */
    public class MyFriendsListAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> f;
        LayoutInflater inflater;

        public MyFriendsListAdapter(Context c, int resource, ArrayList<String> friends) {
            super(c, resource, friends);
            this.context = c;
            this.f = friends;
            inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String friend = f.get(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.friend_entry, null);
                Log.d(Globals.TAG, "Friend: " + friend);

                //fill in text view with friend name
                TextView friendName = convertView.findViewById(R.id.friend);
                friendName.setText(friend);

                //wiring and callback for remove freind
                Button remove = convertView.findViewById(R.id.remove_button);
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

                                        //handle removing friend from db and the list views
                                        if (friend.equals(friendName1)) {
                                            UserManager.removeFriend(activityUser, friendID);
                                            friendsList.remove(friendName1);
                                            startup.setFriendsList(friendsList);

                                            friendsListAdapter = new MyFriendsListAdapter(context, R.layout.friend_entry, friendsList);
                                            friendsListView.setAdapter(friendsListAdapter);
                                            ListUtils.setDynamicHeight(friendsListView);
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
            }

            return convertView;
        }
    }

    /**
     * Friend Request list adapter
     */
    public class MyRequestsListAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> r;
        int res;
        LayoutInflater inflater;

        public MyRequestsListAdapter(Context c, int resource, ArrayList<String> requests) {
            super(c, resource, requests);
            this.context = c;
            this.res = resource;
            this.r = requests;
            inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String request = r.get(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.friend_request_entry, null);

                //text view of friend requestor's name
                TextView friendName = convertView.findViewById(R.id.friend_request);
                friendName.setText(request);

                //wire buttons for add and decline friend request
                Button add = convertView.findViewById(R.id.add_button);
                Button decline = convertView.findViewById(R.id.decline_button);

                //add frend callback
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

                                        //add the friend in the db and in the list views
                                        if (request.equals(requestName)) {
                                            UserManager.acceptFriendRequest(activityUser, requestID);
                                            requestsList.remove(requestName);
                                            friendsList.add(requestName);

                                            startup.setFriendsList(friendsList);
                                            startup.setFriendRequestsList(requestsList);

                                            requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, requestsList);
                                            requestsListView.setAdapter(requestsListAdapter);
                                            ListUtils.setDynamicHeight(requestsListView);

                                            friendsListAdapter = new MyFriendsListAdapter(context, R.layout.friend_entry, friendsList);
                                            friendsListView.setAdapter(friendsListAdapter);
                                            ListUtils.setDynamicHeight(friendsListView);
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

                                        //remove the friend request from db and list views
                                        if (request.equals(requestName)) {
                                            UserManager.declineFriendRequest(activityUser, requestID);
                                            requestsList.remove(requestName);
                                            startup.setFriendRequestsList(requestsList);

                                            requestsListAdapter = new MyRequestsListAdapter(context, R.layout.friend_request_entry, requestsList);
                                            requestsListView.setAdapter(requestsListAdapter);
                                            ListUtils.setDynamicHeight(requestsListView);
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
            }

            return convertView;
        }
    }
}
