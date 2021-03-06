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
import android.widget.Toast;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rayhc.giftly.CreateGiftActivity;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.Startup;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.GiftAdapter;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.ListUtils;
import com.rayhc.giftly.util.StorageLoaderThread;
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
    private View recCard, sentCard, nothingYet;

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

        Intent startIntent = getActivity().getIntent();
        if (startIntent.hasExtra("SENT GIFT")) {
            //storage stuff
            FirebaseStorage mStorage = FirebaseStorage.getInstance();
            StorageReference storageRef = mStorage.getReference();

            Gift mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
            String fromID = startIntent.getStringExtra("FROM USER ID");
            String toID = startIntent.getStringExtra("TO USER ID");

            StorageLoaderThread storageLoaderThread = new StorageLoaderThread(mGift, (MainActivity) getActivity(),
                    mDatabase, storageRef, mStorage, fromID, toID);

            Log.d("rhc","storage beginning");
            storageLoaderThread.start();
        }

    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = layoutInflater.inflate(R.layout.fragment_home, container, false);

        if (getActivity().getIntent().hasExtra("SENT GIFT")) {
            root.findViewById(R.id.downloading).setVisibility(View.VISIBLE);
        }

        //wire lists
        recievedGifts = root.findViewById(R.id.inbox_gifts_recieved);
        sentGifts = root.findViewById(R.id.inbox_gifts_sent);
        nothingYet = root.findViewById(R.id.nothing_yet);
        recCard = root.findViewById(R.id.recGifts);
        sentCard = root.findViewById(R.id.sentGifts);

        //if the sent gift map exists, fill it
        if(!startup.getSentGiftMap().isEmpty()) {
            ArrayList<String> sentGiftMessages = new ArrayList<>();
            sentGiftMessages.addAll(startup.getSentGiftMap().keySet());
            sentGiftsAdapter = new GiftAdapter(getActivity(), 0, sentGiftMessages);
            sentGiftsAdapter.hideArrow(true);
            sentGifts.setAdapter(sentGiftsAdapter);
            ListUtils.setDynamicHeight(sentGifts);
        } else {
            sentCard.setVisibility(View.GONE);
        }

        //if the received gift exists, fill it
        if(!startup.getReceivedGiftMap().isEmpty()) {
            ArrayList<String> receivedGiftMessages = new ArrayList<>();
            receivedGiftMessages.addAll(startup.getReceivedGiftMap().keySet());
            receivedGiftsAdapter = new GiftAdapter(getActivity(), 0, receivedGiftMessages);
            recievedGifts.setAdapter(receivedGiftsAdapter);
            recievedGifts.setOnItemClickListener((parent, view, position, id) -> {
                String label = (String) parent.getItemAtPosition(position);
                //download the gift
                Intent intent;
                intent = new Intent(getContext(), DownloadSplashActivity.class);
                intent.putExtra(Globals.HASH_VALUE_KEY, startup.getReceivedGiftMap().get(label));
                intent.putExtra(Globals.FROM_REC_KEY, true);
                intent.putExtra(Globals.FROM_OPEN_KEY, true);
                intent.putExtra("USER ID", mFirebaseUser.getUid());
                Log.d("LPC", "getting gift w hash: " + startup.getReceivedGiftMap().get(label));
                startActivity(intent);
            });
            ListUtils.setDynamicHeight(recievedGifts);
            nothingYet.setVisibility(View.GONE);
            recCard.setVisibility(View.VISIBLE);
        } else {
            nothingYet.setVisibility(View.VISIBLE);
            recCard.setVisibility(View.GONE);
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
                Toast toast = Toast.makeText(getActivity(), Globals.UPDATE_TOAST, Toast.LENGTH_SHORT);
                toast.show();

                //call gift sent & received threads to update list views
                GetSentGiftsThread getSentGiftsThread1 = new GetSentGiftsThread();
                GetReceivedGiftsThread getReceivedGiftsThread1 = new GetReceivedGiftsThread();
                getSentGiftsThread1.start();
                getReceivedGiftsThread1.start();
            }
        });


        return root;
    }

    public void updateSent() {
        //run sent gifts thread
        GetSentGiftsThread getSentGiftsThread = new GetSentGiftsThread();
        getSentGiftsThread.start();
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
                //force-synchronize all of the data
                if (giftMsgMap.size() < numSentGifts) {
                    Log.d("LPC", "sent gifts handler didnt run");
                    return;
                }
                //make passable strings in form "To: *name* - *message*"
                for (String hash : giftMsgMap.keySet()) {
                    String label = giftMsgMap.get(hash);
                    //put in map label -> gift hash
                    sentGiftMap.put(label, hash);
                }
                Log.d("LPC", "thread done - sent gift map: " + sentGiftMap.toString());
                Log.d("LPC", "has the sent gift map changed: "+(!sentGiftMap.equals(startup.getSentGiftMap())));
                //if the sent gift data has changed from last save, update adapter
                if(!sentGiftMap.equals(startup.getSentGiftMap())) {
                    startup.setSentGiftMap(sentGiftMap);
                    //set the adapter
                    ArrayList<String> sentGiftMessages = new ArrayList<>();
                    sentGiftMessages.addAll(sentGiftMap.keySet());
                    sentGiftsAdapter = new GiftAdapter(getActivity(), 0, sentGiftMessages);
                    sentGiftsAdapter.hideArrow(true);
                    sentGifts.setAdapter(sentGiftsAdapter);
                    ListUtils.setDynamicHeight(sentGifts);
                    sentCard.setVisibility(View.VISIBLE);
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
                        //edge case
                        if(newUser.getSentGifts() == null) {
                            handler.post(runnable);
                        } else {
                            //get the number of sent gifts this user has
                            numSentGifts = newUser.getSentGifts().keySet().size();
                            Log.d("LPC", "num sentGifts: " + numSentGifts);
                            giftHashes = new ArrayList<>(newUser.getSentGifts().keySet());
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
                                        //after getting each gift receiver's name, get the gift's message too
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
            for(String hash: giftHashes){
                Query userNameQuery = mDatabase.child("gifts").orderByChild("hashValue").equalTo(hash);
                userNameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String message = (String) snapshot.child(hash).child("message").getValue();
                        //delimit message and name by a pipe for saving puposes
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
                //force-synchronize all data
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
                        intent.putExtra(Globals.HASH_VALUE_KEY, startup.getReceivedGiftMap().get(label));
                        intent.putExtra(Globals.FROM_REC_KEY, true);
                        intent.putExtra(Globals.FROM_OPEN_KEY, true);
                        intent.putExtra("USER ID", mFirebaseUser.getUid());
                        Log.d("LPC", "getting gift w hash: " + receivedGiftsMap.get(label));
                        startActivity(intent);
                    });
                    ListUtils.setDynamicHeight(recievedGifts);
                    nothingYet.setVisibility(View.GONE);
                    recCard.setVisibility(View.VISIBLE);
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
                        //edge case
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
                                        //after getting sender's name, get the gift's message
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
                        //detemine if gift has been opened or not with string tag (OLD or NEW)
                        boolean opened = false;
                        if (snapshot.child(hash).child("opened").getValue() != null)
                            opened = (boolean) snapshot.child(hash).child("opened").getValue();
                        if (opened) message += "OLD";
                        else message += "NEW";
                        //delimit entire message with pipe
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
