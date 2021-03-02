package com.rayhc.giftly;

import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateGiftFragment extends Fragment {
    private TextView recipientLabel;
    private Button linkButton, imageButton, videoButton, reviewButton, sendButton, chooseFriendButton;
    private EditText messageInput;
    private Gift newGift;

    //user id
    SharedPreferences mSharedPref;
    String mUserId;

    //recipient stuff
    String recipientName, recipientID;

    //firebase stuff
    private DatabaseReference mDatabase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        //get userID from shared pref
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mUserId = mSharedPref.getString("userId",null);
        Log.d("LPC", "create gift - user id: "+mUserId);


        //reference to DB
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //get possible gift data
        Bundle extras = getArguments();
        if(extras != null && extras.getSerializable(Globals.CURR_GIFT_KEY) != null){
            newGift = (Gift) extras.getSerializable(Globals.CURR_GIFT_KEY);
            Log.d("LPC", "create frag: got gift from bundle");
            Log.d("LPC", "create frag: gift from bundle content: "+newGift.getContentType().toString());
        }
        //otherwise make this dummy gift
        else{
            Log.d("LPC", "create gift frag: making new gift");
            newGift = new Gift();
            newGift.setReceiver("Logan 2");
            newGift.setSender("Logan 1");
            newGift.setTimeCreated(100);
            newGift.setHashValue(newGift.createHashValue());
            newGift.setContentType(new HashMap<>());
            newGift.setLinks(new HashMap<>());
            newGift.setGiftType(new HashMap<>());
        }

        //get possible recipient user data
        if(extras != null && extras.getString("FRIEND NAME") != null &&
                extras.getString("FRIEND ID") != null){
            recipientID =  extras.getString("FRIEND ID");
            recipientName = extras.getString("FRIEND NAME");

        }

    }

    public View onCreateView(LayoutInflater layoutInflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = layoutInflater.inflate(R.layout.fragment_create_gift, container, false);
        Log.d("LPC", "CreateGiftFragment: onCreateView: " + newGift.toString());

        //wire in widgets
        linkButton = v.findViewById(R.id.link_button);
        imageButton = v.findViewById(R.id.image_button);
        videoButton = v.findViewById(R.id.video_button);
        reviewButton = v.findViewById(R.id.review_button);
        sendButton = v.findViewById(R.id.send_button);
        chooseFriendButton = v.findViewById(R.id.choose_recipient_button);
        sendButton.setEnabled(newGift.getContentType().size() != 0 || newGift.getLinks().size() != 0);

        //set up message input
        messageInput = v.findViewById(R.id.message_input);
        if(newGift.getMessage() != null) messageInput.setText(newGift.getMessage());
        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String input;
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    input= v.getText().toString();
                    newGift.setMessage(input);
                    Log.d("LPC", "set gift message to: "+newGift.getMessage());
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true; // consume.
                }
                return false; // pass on to other listeners.
            }
        });

        //set up the recipient label
        recipientLabel = v.findViewById(R.id.recipient);
        if(recipientName != null) recipientLabel.setText("This Gift is to: "+recipientName);


        //click listeners for adding contents to the gift
        linkButton.setOnClickListener(v12 -> {
            Intent intent = new Intent(getActivity(), LinkActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        imageButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(getActivity(), ImageActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        videoButton.setOnClickListener(v13 -> {
            Intent intent = new Intent(getActivity(), VideoActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        reviewButton.setOnClickListener(v14 -> {
            Intent intent = new Intent(getActivity(), ReviewGiftActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        sendButton.setOnClickListener(v14 ->{
            Intent intent = new Intent(getActivity(), UploadingSplashActivity.class);
            //set the message of the gift
            newGift.setMessage(messageInput.getText().toString());
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FROM USER ID", mUserId);
            intent.putExtra("TO USER ID", recipientID);
            startActivity(intent);
        });

        chooseFriendButton.setOnClickListener(v15 ->{
            Intent intent = new Intent(getActivity(), DownloadSplashActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("GET FRIENDS", true);
            intent.putExtra("USER ID", mUserId);
            startActivity(intent);
        });

        return v;
    }

}
