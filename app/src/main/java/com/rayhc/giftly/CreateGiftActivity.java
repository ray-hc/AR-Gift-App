package com.rayhc.giftly;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rayhc.giftly.util.Gift;
import com.rayhc.giftly.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateGiftActivity extends AppCompatActivity {

    //widgets
    private TextView recipientLabel, reviewLabel;
    private Button sendButton, chooseFriendButton, reviewButton;
    private ImageButton linkButton, imageButton, videoButton;
    private EditText messageInput;
    private ListView linksList;
    private Spinner giftTypeSpinner;

    private static final HashMap<String, Integer> GIFT_TYPE_MAP = new HashMap<String, Integer>(){{
        put(Globals.OTHER, 0);
        put(Globals.BDAY, 1);
        put(Globals.XMAS, 2);
    }};

    //gift
    private Gift newGift;
    private HashMap<String, String> sentGiftMap;
    private HashMap<String, String> receivedGiftMap;

    //user id
    SharedPreferences mSharedPref;
    String mUserId;

    //from open stuff
    private boolean fromOpen;
    private String otherName;

    //recipient stuff
    String recipientName, recipientID;

    //firebase stuff
    private DatabaseReference mDatabase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_gift);
        //get userID from shared pref
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUserId = mSharedPref.getString("userId", null);
        Log.d("LPC", "create gift - user id: " + mUserId);


        //reference to DB
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //get possible gift data
        Intent extras = getIntent();
        if (extras != null && extras.getSerializableExtra(Globals.CURR_GIFT_KEY) != null) {
            newGift = (Gift) extras.getSerializableExtra(Globals.CURR_GIFT_KEY);
            Log.d("LPC", "create frag: got gift from bundle");
            Log.d("LPC", "got gift: " + newGift.toString());
//            Log.d("LPC", "create frag: gift from bundle content: "+newGift.getContentType().toString());
        }
        //otherwise make a new gift
        else {
            Log.d("LPC", "create gift frag: making new gift");
            newGift = new Gift();
            newGift.setSender(mUserId);
            newGift.setTimeCreated(System.currentTimeMillis());
            newGift.getHashValue();
            newGift.setContentType(new HashMap<>());
            newGift.setLinks(new HashMap<>());
        }

        //get possible recipient user data
        if (extras != null && extras.getStringExtra("FRIEND NAME") != null &&
                extras.getStringExtra("FRIEND ID") != null) {
            recipientID = extras.getStringExtra("FRIEND ID");
            recipientName = extras.getStringExtra("FRIEND NAME");

        }

        //from open data
        if (extras != null && extras.getBooleanExtra("FROM OPEN", false)) {
            fromOpen = true;
            Log.d("LPC", "from open is true");
            otherName = extras.getStringExtra("OTHER NAME");
        }

        sentGiftMap = (HashMap) extras.getSerializableExtra("SENT GIFT MAP");
        receivedGiftMap = (HashMap) extras.getSerializableExtra("RECEIVED GIFT MAP");
        

        //wire in widgets
        linkButton = findViewById(R.id.link_button);
        imageButton = findViewById(R.id.image_button);
        videoButton = findViewById(R.id.video_button);
        sendButton = findViewById(R.id.send_button);
        chooseFriendButton = findViewById(R.id.choose_recipient_button);
        sendButton.setEnabled(newGift.getMessage() != null && recipientID != null &&
                (newGift.getContentType().size() != 0 || newGift.getLinks().size() != 0));
        linksList = findViewById(R.id.linkList);
        recipientLabel = findViewById(R.id.recipient);
        reviewLabel = findViewById(R.id.review_label);
        reviewButton = findViewById(R.id.review_contents_button);
        messageInput = findViewById(R.id.message_input);

        //set up spinner
        giftTypeSpinner = findViewById(R.id.gift_type_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, Globals.GIFT_TYPE_ARRAY);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        giftTypeSpinner.setAdapter(spinnerArrayAdapter);
        giftTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                newGift.setGiftType(GIFT_TYPE_MAP.get(selectedItem));
            }

            //TODO: to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //handle if from open
        if (fromOpen) {
            //hide editing buttons
            linkButton.setEnabled(false);
            imageButton.setEnabled(false);
            videoButton.setEnabled(false);
            sendButton.setVisibility(View.INVISIBLE);
            chooseFriendButton.setVisibility(View.INVISIBLE);
            sendButton.setVisibility(View.INVISIBLE);

            //turn off message input & recipient
            messageInput.setFocusable(false);
            if (otherName != null && !fromOpen)
                recipientLabel.setText("This Gift is to: " + otherName);
            else if (otherName != null) recipientLabel.setText("This Gift is from: " + otherName);
        }

        //set up message input
        if (newGift.getMessage() != null) messageInput.setText(newGift.getMessage());
        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String input;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    input = v.getText().toString();
                    newGift.setMessage(input);
                    if (input.length() == 0) sendButton.setEnabled(false);
                    if (recipientID != null &&
                            (newGift.getContentType().size() != 0 || newGift.getLinks().size() != 0)
                            && input.length() > 0)
                        sendButton.setEnabled(true);
                    Log.d("LPC", "set gift message to: " + newGift.getMessage());
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true; // consume.
                }
                return false; // pass on to other listeners.
            }
        });

        //set up the recipient label
        if (recipientName != null) recipientLabel.setText(recipientName);

        //set up links list view
        if (newGift.getLinks() != null) {
            Log.d("LPC", "from download splash - links : " + newGift.getLinks().toString());
            ArrayList<String> linkNames = new ArrayList<>();
            linkNames.addAll(newGift.getLinks().keySet());
            HashMap<String, String> displayMap = new HashMap<>();
            for (String label : linkNames) {
                displayMap.put(newGift.getLinks().get(label), label);
            }
            linksList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                    new ArrayList<>(displayMap.keySet())));
            linksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String label = (String) parent.getItemAtPosition(position);
                    Log.d("LPC", "link list view position click label: " + label);
                    Log.d("LPC", "link list clicked link key: " + displayMap.get(label));
                    Intent intent;
                    //go to ViewContents if opening a gift, else go to LinkActivity
                    if (fromOpen) intent = new Intent(getApplicationContext(), ViewContentsActivity.class);
                    else intent = new Intent(getApplicationContext(), LinkActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, displayMap.get(label));
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    intent.putExtra("SENT GIFT MAP", sentGiftMap);
                    intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
                    startActivity(intent);
                }
            });
        }


        //click listeners for adding contents to the gift
        linkButton.setOnClickListener(v12 -> {
            Intent intent = new Intent(this, LinkActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });

        //set up review button
        if (newGift.getContentType() == null || newGift.getContentType().size() == 0)
            reviewButton.setEnabled(false);
        else {

            String text = "Click to review/edit your gift's " + newGift.getContentType().size() + " media files";
            reviewButton.setText(text);
        }
        reviewButton.setOnClickListener(v1 ->  {
            Intent intent;
            if (fromOpen) {
                intent = new Intent(this, ViewContentsActivity.class);
                intent.putExtra("GET MEDIA", true);
            } else intent = new Intent(this, EditContentsActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });


        imageButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(this, ImageActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });

        videoButton.setOnClickListener(v13 -> {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });


        sendButton.setOnClickListener(v14 -> {
            Intent intent = new Intent(this, UploadingSplashActivity.class);
            //set the message of the gift
            newGift.setMessage(messageInput.getText().toString());
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FROM USER ID", mUserId);
            intent.putExtra("TO USER ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });

        chooseFriendButton.setOnClickListener(v15 -> {
            Intent intent = new Intent(this, DownloadSplashActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("GET FRIENDS", true);
            intent.putExtra("USER ID", mUserId);
            intent.putExtra("FRIEND NAME", recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            intent.putExtra("SENT GIFT MAP", sentGiftMap);
            intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("GOT GIFTS", true);
        intent.putExtra("SENT GIFT MAP", sentGiftMap);
        intent.putExtra("RECEIVED GIFT MAP", receivedGiftMap);
//        intent.putExtra("USER ID", mUserId);
//        intent.putExtra("GET GIFTS", true);
        startActivity(intent);
    }
}