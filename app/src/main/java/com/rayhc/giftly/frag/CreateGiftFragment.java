package com.rayhc.giftly.frag;

import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rayhc.giftly.DownloadSplashActivity;
import com.rayhc.giftly.EditContentsActivity;
import com.rayhc.giftly.ImageActivity;
import com.rayhc.giftly.LinkActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.ReviewGiftActivity;
import com.rayhc.giftly.UploadingSplashActivity;
import com.rayhc.giftly.VideoActivity;
import com.rayhc.giftly.ViewContentsActivity;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateGiftFragment extends Fragment {
    //widgets
    private TextView recipientLabel;
    private Button sendButton, chooseFriendButton, reviewButton;
    private ImageButton linkButton, imageButton, videoButton;
    private EditText messageInput;
    private ListView linksList;
    private Spinner giftTypeSpinner;
    private static final String[] GIFT_TYPE_ARRAY = {"Normal", "Birthday", "Christmas"};
    private static final HashMap<String, Integer> GIFT_TYPE_MAP = new HashMap<String, Integer>(){{
        put("Normal", 0);
        put("Birthday", 1);
        put("Christmas", 2);
    }};

    //gift
    private Gift newGift;

    //user id
    SharedPreferences mSharedPref;
    String mUserId;

    //from open stuff
    private boolean fromOpen, fromReceive;
    private String otherName;

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
            Log.d("LPC", "got gift: "+newGift.toString());
//            Log.d("LPC", "create frag: gift from bundle content: "+newGift.getContentType().toString());
        }
        //otherwise make a new gift
        else{
            Log.d("LPC", "create gift frag: making new gift");
            newGift = new Gift();
            newGift.setSender(mUserId);
            newGift.setTimeCreated(System.currentTimeMillis());
            newGift.getHashValue();
            newGift.setContentType(new HashMap<>());
            newGift.setLinks(new HashMap<>());
        }

        //get possible recipient user data
        if(extras != null && extras.getString("FRIEND NAME") != null &&
                extras.getString("FRIEND ID") != null){
            recipientID =  extras.getString("FRIEND ID");
            recipientName = extras.getString("FRIEND NAME");

        }

        //from open data
        if(extras != null && extras.getBoolean("FROM OPEN", false)){
            fromOpen = true;
            fromReceive = extras.getBoolean("FROM RECEIVE", false);
            Log.d("LPC", "from receive?: "+fromReceive);
            Log.d("LPC", "from open is true");
            otherName = extras.getString("OTHER NAME");
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
        sendButton = v.findViewById(R.id.send_button);
        chooseFriendButton = v.findViewById(R.id.choose_recipient_button);
        sendButton.setEnabled(recipientID != null && (newGift.getContentType().size() != 0 || newGift.getLinks().size() != 0));
        linksList = v.findViewById(R.id.linkList);
        reviewButton = v.findViewById(R.id.review_contents_button);
        messageInput = v.findViewById(R.id.message_input);

        //set up spinner
//        giftTypeSpinner = v.findViewById(R.id.gift_type_spinner);
//        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
//                (getContext(), android.R.layout.simple_spinner_item, GIFT_TYPE_ARRAY);
//        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
//        giftTypeSpinner.setAdapter(spinnerArrayAdapter);
//        giftTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
//        {
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String selectedItem = parent.getItemAtPosition(position).toString();
//                newGift.setGiftType(GIFT_TYPE_MAP.get(selectedItem));
//            }
//            //TODO: to close the onItemSelected
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });

        //handle if from open
        if(fromOpen){
            //hide editing buttons
            linkButton.setEnabled(false);
            imageButton.setEnabled(false);
            videoButton.setEnabled(false);
            sendButton.setVisibility(View.INVISIBLE);
            chooseFriendButton.setVisibility(View.INVISIBLE);
            sendButton.setVisibility(View.INVISIBLE);

            //turn off message input & recipient
            messageInput.setFocusable(false);
            recipientLabel = v.findViewById(R.id.recipient);
            if(otherName != null && !fromReceive) recipientLabel.setText("This Gift is to: "+otherName);
            else if(otherName != null)  recipientLabel.setText("This Gift is from: "+otherName);
        }

        //set up message input
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
        if(recipientName != null) recipientLabel.setText("This Gift is to: "+recipientName);

        //set up links list view
        if(newGift.getLinks() != null){
            Log.d("LPC", "from download splash - links : "+newGift.getLinks().toString());
            ArrayList<String> linkNames = new ArrayList<>();
            linkNames.addAll(newGift.getLinks().keySet());
            linksList.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, linkNames));
            linksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    Log.d("LPC", "media list view position click label: "+ label);

                    Intent intent;
                    //go to ViewContents if opening a gift, else go to LinkActivity
                    if(fromOpen) intent = new Intent(getContext(), ViewContentsActivity.class);
                    else intent = new Intent(getContext(), LinkActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, label);
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    startActivity(intent);
                }
            });
        }



        //click listeners for adding contents to the gift
        linkButton.setOnClickListener(v12 -> {
            Intent intent = new Intent(getActivity(), LinkActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        //set up review button
        if(newGift.getContentType() == null || newGift.getContentType().size() == 0) reviewButton.setEnabled(false);
        else {
            String text = "Click to review/edit your gift's "+newGift.getContentType().size()+" media files";
            reviewButton.setText(text);
        }
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if(fromOpen) {
                    intent = new Intent(getActivity(), ViewContentsActivity.class);
                    intent.putExtra("GET MEDIA", true);
                }
                else intent = new Intent(getActivity(), EditContentsActivity.class);
                intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
                intent.putExtra("FRIEND NAME", recipientName);
                intent.putExtra("FRIEND ID", recipientID);
                startActivity(intent);
            }
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
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        return v;
    }

}
