package com.rayhc.giftly.frag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.CreateGiftActivity;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.Startup;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.GiftAdapter;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class HomeFragment extends Fragment {
    private ListView recievedGifts;
    private ListView sentGifts;

    //create gift button
    private Button createGiftButton;
    private ImageButton refreshButton;

    //firebase user info
    private DatabaseReference mDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String userID;

    //list stuff
    private GiftAdapter sentGiftsAdapter, receivedGiftsAdapter;

    private Startup startup;

    private SharedPreferences mSharedPref;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //get firebase user data
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if(mFirebaseUser == null) {
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            userID = mSharedPref.getString("userId", null);
        } else {
            userID = mFirebaseUser.getUid();
        }


        startup = (Startup) getActivity().getApplication();

        //get possible gift data
        Bundle extras = getArguments();
        Log.d("LPC", "home - is bundle null?: "+(extras == null));
        if(extras != null)
            Log.d("LPC", "home - need refresh?: "+(extras.getBoolean("NEED REFRESH", false)));
        if(extras != null && extras.getBoolean("NEED REFRESH", false)){
            Log.d("LPC", "create gift going to refresh");
            Intent intent = new Intent(getContext(), DownloadSplashActivity.class);
            intent.putExtra("GET GIFTS", true);
            intent.putExtra("USER ID", mFirebaseUser.getUid());
            startActivity(intent);
        }

    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        //wire lists
        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);

        //if the sent gift map exists, fill it
        if(startup.getSentGiftMap() != null) {
            ArrayList<String> sentGiftMessages = new ArrayList<>();
            sentGiftMessages.addAll(startup.getSentGiftMap().keySet());
            sentGiftsAdapter = new GiftAdapter(getActivity(), 0, sentGiftMessages);
            sentGiftsAdapter.hideArrow(true);
            sentGifts.setAdapter(sentGiftsAdapter);
            ListUtils.setDynamicHeight(sentGifts);
        }

        //if the received gift exists, fill it
        if(startup.getReceivedGiftMap() != null) {
            ArrayList<String> receivedGiftMessages = new ArrayList<>();
            receivedGiftMessages.addAll(startup.getReceivedGiftMap().keySet());
            receivedGiftsAdapter = new GiftAdapter(getActivity(), 0, receivedGiftMessages);
            recievedGifts.setAdapter(receivedGiftsAdapter);
            recievedGifts.setOnItemClickListener((parent, view, position, id) -> {
                String label = (String) parent.getItemAtPosition(position);
                //download the gift
                Intent intent;
                intent = new Intent(getContext(), DownloadSplashActivity.class);
                intent.putExtra("HASH VALUE", startup.getReceivedGiftMap().get(label));
                intent.putExtra("FROM RECEIVED", true);
                intent.putExtra("FROM OPEN", true);
                intent.putExtra("USER ID", mFirebaseUser.getUid());
                Log.d("LPC", "getting gift w hash: " + startup.getReceivedGiftMap().get(label));
                startActivity(intent);
            });
            ListUtils.setDynamicHeight(recievedGifts);
        }

        //run sent gifts thread
        GetSentGiftsThread getSentGiftsThread = new GetSentGiftsThread();
        getSentGiftsThread.start();


        //run received gifts thread
        GetReceivedGiftsThread getReceivedGiftsThread = new GetReceivedGiftsThread();
        getReceivedGiftsThread.start();


        //wire buttons
        createGiftButton = root.findViewById(R.id.create_gift_button);
        createGiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CreateGiftActivity.class);
                startActivity(intent);
            }
        });
        refreshButton = root.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetSentGiftsThread getSentGiftsThread1 = new GetSentGiftsThread();
                GetReceivedGiftsThread getReceivedGiftsThread1 = new GetReceivedGiftsThread();
                getSentGiftsThread1.start();
                getReceivedGiftsThread1.start();
            }
        });


        return root;
    }

    /**
     * Get the users sent gifts
     */
    public class GetSentGiftsThread extends Thread{
        private int numSentGifts = 0;
        private ArrayList<String> giftRecipientNames = new ArrayList<>();
        private HashMap<String, String> giftMsgMap = new HashMap<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> sentGiftMap = new HashMap<>();

        public GetSentGiftsThread(){
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (giftMsgMap.size() < numSentGifts) {
                    Log.d("LPC", "sent gifts handler didnt run");
                    return;
                }
                //make passable strings in form "To: *name* - *message*"
                Log.d("LPC", "sent gift msg map: " + giftMsgMap.toString());
                for (String hash : giftMsgMap.keySet()) {
                    String label = giftMsgMap.get(hash);
                    //put in map label -> gift hash
                    sentGiftMap.put(label, hash);
                }
                Log.d("LPC", "thread done - sent gift map: " + sentGiftMap.toString());
                Log.d("LPC", "has the sent gift map changed: "+(!sentGiftMap.equals(startup.getSentGiftMap())));
                if(!sentGiftMap.equals(startup.getSentGiftMap())){
                    startup.setSentGiftMap(sentGiftMap);
                    //set the adapter
                    ArrayList<String> sentGiftMessages = new ArrayList<>();
                    sentGiftMessages.addAll(sentGiftMap.keySet());
                    sentGiftsAdapter = new GiftAdapter(getActivity(), 0, sentGiftMessages);
                    sentGiftsAdapter.hideArrow(true);
                    sentGifts.setAdapter(sentGiftsAdapter);
                    ListUtils.setDynamicHeight(sentGifts);
                }

            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getSentGifts();
        }

        private void getSentGifts(){
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if(snapshot.exists()){
                        newUser = UserManager.snapshotToUser(snapshot, userID);
                        if(newUser.getSentGifts() == null) {
                            handler.post(runnable);
                        } else {
                            //get the number of sent gifts this user has
                            numSentGifts = newUser.getSentGifts().keySet().size();
                            Log.d("LPC", "num sentGifts: " + numSentGifts);
                            giftHashes = new ArrayList<>(newUser.getSentGifts().keySet());
                            Log.d("LPC", "gift hashes when getting name: "+giftHashes.toString());
                            for (String key : newUser.getSentGifts().keySet()) {
                                String otherUserID = newUser.getSentGifts().get(key);
                                //get the other user's name
                                Query userNameQuery = mDatabase.child("users").orderByChild("userId").equalTo(otherUserID);
                                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftRecipientNames.add(friendName);
                                        giftMsgMap.put(key, friendName);
                                        getGiftMessages();
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
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
        private void getGiftMessages(){
            if(giftRecipientNames.size()<numSentGifts) return;
            //get the gift messages
            Log.d("LPC", "gift hashes when getting msgs: "+giftHashes.toString());
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        String displayText = giftMsgMap.get(hash)+"|"+message;
                        giftMsgMap.put(hash, displayText);
                        Log.d("LPC", "getting gift with hash: "+hash+" with message: "+message);
                        handler.post(runnable);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }
    }

    /**
     * Get the users received gifts
     */
    public class GetReceivedGiftsThread extends Thread {
        private int numReceivedGifts = 0;
        private ArrayList<String> giftSenderNames = new ArrayList<>();
        private HashMap<String, String> giftMsgMap = new HashMap<>();
        private ArrayList<String> giftHashes = new ArrayList<>();
        private HashMap<String, String> receivedGiftsMap = new HashMap<>();

        public GetReceivedGiftsThread() {

        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (giftMsgMap.size() < numReceivedGifts) {
                    Log.d("LPC", "received gifts handler didnt run");
                    return;
                }
                //make passable strings in form "To: *name* - *message*"
                Log.d("LPC", "received gift msg map: " + giftMsgMap.toString());
                for (String hash : giftMsgMap.keySet()) {
                    String label = giftMsgMap.get(hash);
                    //put in map label -> gift hash
                    receivedGiftsMap.put(label, hash);
                }

                //update the map if it has changed
                if(!receivedGiftsMap.equals(startup.getReceivedGiftMap())){
                    startup.setReceivedGiftMap(receivedGiftsMap);
                    //set up and populate the adapter
                    ArrayList<String> receivedGiftMessages = new ArrayList<>();
                    receivedGiftMessages.addAll(receivedGiftsMap.keySet());
                    receivedGiftsAdapter = new GiftAdapter(getActivity(), 0, receivedGiftMessages);
                    recievedGifts.setAdapter(receivedGiftsAdapter);
                    recievedGifts.setOnItemClickListener((parent, view, position, id) -> {
                        String label = (String) parent.getItemAtPosition(position);
                        //download the gift
                        Intent intent;
                        intent = new Intent(getContext(), DownloadSplashActivity.class);
                        intent.putExtra("HASH VALUE", receivedGiftsMap.get(label));
                        intent.putExtra("FROM RECEIVED", true);
                        intent.putExtra("FROM OPEN", true);
                        intent.putExtra("USER ID", mFirebaseUser.getUid());
                        Log.d("LPC", "getting gift w hash: " + receivedGiftsMap.get(label));
                        startActivity(intent);
                    });
                    ListUtils.setDynamicHeight(recievedGifts);
                }

                Log.d("LPC", "thread done-received gift map: " + receivedGiftsMap.toString());
            }
        };

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            super.run();
            getReceivedGifts();
        }

        private void getReceivedGifts() {
            Query query = mDatabase.child("users").orderByChild("userId").equalTo(userID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User newUser = new User();
                    if (snapshot.exists()) {
                        newUser = UserManager.snapshotToUser(snapshot, userID);
                        if (newUser.getReceivedGifts() == null) {
                            handler.post(runnable);
                        } else {
                            numReceivedGifts = newUser.getReceivedGifts().keySet().size();
                            Log.d("LPC", "num receivedGifts: " + numReceivedGifts);
                            giftHashes = new ArrayList<>(newUser.getReceivedGifts().keySet());
                            for (String key : newUser.getReceivedGifts().keySet()) {
                                String otherUserID = newUser.getReceivedGifts().get(key);
                                //get the other user's name
                                Query userNameQuery = mDatabase.child("users").orderByChild("userId").equalTo(otherUserID);
                                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String friendName = (String) snapshot.child(otherUserID).child("name").getValue();
                                        giftSenderNames.add(friendName);
                                        giftMsgMap.put(key, friendName);
                                        getGiftMessages();
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

        public void getGiftMessages() {
            if (giftSenderNames.size() < numReceivedGifts) return;
            //get the gift messages
            for (String hash : giftHashes) {
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        boolean opened = false;
                        if (snapshot.child(hash).child("opened").getValue() != null)
                            opened = (boolean) snapshot.child(hash).child("opened").getValue();
                        if (opened) message += "OLD";
                        else message += "NEW";
                        String displayText = giftMsgMap.get(hash) + "|" + message;
                        giftMsgMap.put(hash, displayText);
                        Log.d("LPC", "getting gift with hash: " + hash + " with message: " + message);
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
