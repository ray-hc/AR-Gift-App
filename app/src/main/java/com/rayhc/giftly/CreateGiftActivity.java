package com.rayhc.giftly;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.rayhc.giftly.util.LinkAdapter;
import com.rayhc.giftly.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateGiftActivity extends AppCompatActivity {

    //widgets
    private TextView recipientLabel, reviewLabel,toTitle, createGiftTitle;
    private Button sendButton, chooseFriendButton, reviewButton, closeButton;
    private ImageButton linkButton, imageButton, videoButton;
    private EditText messageInput;
    private ListView linksList;
    private Spinner giftTypeSpinner;
    private ConstraintLayout spinnerCard;
    private View linkCard, editButtons;

    private static final HashMap<String, Integer> GIFT_TYPE_MAP = new HashMap<String, Integer>(){{
        put(Globals.BDAY, 0);
        put(Globals.XMAS, 1);
        put(Globals.OTHER, 2);
    }};


    //gift
    private Gift newGift;

    //user id
    SharedPreferences mSharedPref;
    String mUserId;

    //from open stuff
    private boolean fromOpen, wasOpened = true;
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
            Log.d("LPC", "got gift: " + newGift.toString());
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
            newGift.setGiftType(-1);
            wasOpened = true;
        }


        //get possible recipient user data
        if (extras != null && extras.getStringExtra(Globals.FRIEND_NAME_KEY) != null &&
                extras.getStringExtra(Globals.FRIEND_ID_KEY) != null) {
            recipientID = extras.getStringExtra(Globals.FRIEND_ID_KEY);
            recipientName = extras.getStringExtra(Globals.FRIEND_NAME_KEY);

        }

        //from open data
        if (extras != null && extras.getBooleanExtra(Globals.FROM_OPEN_KEY, false)) {
            fromOpen = true;
            wasOpened = extras.getBooleanExtra(Globals.WAS_OPENED_KEY, false);
            Log.d("LPC", "from open is true");
            otherName = extras.getStringExtra("OTHER NAME");
        }

        wireInWidgets();
        setUpSpinner();

        //handle if from open
        if (fromOpen) {
            //hide editing buttons
            editButtons.setVisibility(View.GONE);
            chooseFriendButton.setVisibility(View.INVISIBLE);

            //turn off message input & recipient
            messageInput.setFocusable(false);
            recipientLabel.setText(extras.getStringExtra(Globals.FRIEND_NAME_KEY));
            if(extras.getBooleanExtra("IS RECEIVED", false))
                toTitle.setText("From: ");
            spinnerCard.setVisibility(View.GONE);
            createGiftTitle.setText("View Gift");
        }
        Log.d("LPC", "was this gift opened before?: "+wasOpened);

        //set up message input
        if (newGift.getMessage() != null) messageInput.setText(newGift.getMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
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
                return true;
            }
            return false;
        });

        //set up the recipient label
        if (recipientName != null) recipientLabel.setText(recipientName);

        //set up links list view
        if (newGift.getLinks() != null && newGift.getLinks().size() > 0) {
            linkCard.setVisibility(View.VISIBLE);
            Log.d(Globals.TAG, newGift.getLinks().size()+"");

            Log.d("LPC", "from download splash - links : " + newGift.getLinks().toString());
            ArrayList<String> linkNames = new ArrayList<>();
            linkNames.addAll(newGift.getLinks().keySet());
            HashMap<String, String> displayMap = new HashMap<>();
            for (String label : linkNames) {
                displayMap.put(newGift.getLinks().get(label), label);
            }
            linksList.setAdapter(new LinkAdapter(this, 0, new ArrayList<>(displayMap.keySet())));
            ListUtils.setDynamicHeight(linksList);

            linksList.setOnItemClickListener((parent, view, position, id) -> {
                String label = (String) parent.getItemAtPosition(position);
                Intent intent;
                //open link if opening a gift, else go to LinkActivity
                if (fromOpen) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(label));
                    startActivity(intent);
                }
                else {
                    intent = new Intent(getApplicationContext(), LinkActivity.class);
                    intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
                    intent.putExtra(Globals.FILE_LABEL_KEY, displayMap.get(label));
                    intent.putExtra(Globals.FROM_REVIEW_KEY, true);
                    intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
                    intent.putExtra(Globals.FRIEND_ID_KEY, recipientID);
                }
                startActivity(intent);
            });
        } else {
            Log.d(Globals.TAG, "linkcard mia");
            linkCard.setVisibility(View.GONE);
        }


        //click listeners for adding contents to the gift
        linkButton.setOnClickListener(v12 -> {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent = new Intent(this, LinkActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
            intent.putExtra(Globals.FRIEND_ID_KEY, recipientID);
            startActivity(intent);
        });

        //set up review button
        if (newGift.getContentType() == null || newGift.getContentType().size() == 0) {
            reviewButton.setEnabled(false);
             reviewLabel.setText(
                     getResources().getQuantityString(R.plurals.has_media_items, 0, 0)
             );
        }
        else {
            reviewButton.setEnabled(true);
            reviewLabel.setText(
                        getResources().getQuantityString(R.plurals.has_media_items, newGift.getContentType().size(),
                                newGift.getContentType().size())
                );
        }

        reviewButton.setOnClickListener(v1 ->  {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent;
            if (fromOpen) {
                intent = new Intent(this, ViewContentsActivity.class);
                intent.putExtra("GET MEDIA", true);
            } else intent = new Intent(this, EditContentsActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
            intent.putExtra(Globals.FRIEND_ID_KEY, recipientID);
            startActivity(intent);
        });

        //set up buttons to add contents to the gift
        imageButton.setOnClickListener(v1 -> {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent = new Intent(this, ImageActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
            intent.putExtra(Globals.FRIEND_ID_KEY, recipientID);
            startActivity(intent);
        });

        videoButton.setOnClickListener(v13 -> {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
            intent.putExtra(Globals.FRIEND_ID_KEY, recipientID);
            startActivity(intent);
        });


        sendButton.setOnClickListener(v14 -> {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent = new Intent(this, UploadingSplashActivity.class);
            //set the message of the gift
            newGift.setMessage(messageInput.getText().toString());
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("FROM USER ID", mUserId);
            intent.putExtra("TO USER ID", recipientID);
            startActivity(intent);
        });

        chooseFriendButton.setOnClickListener(v15 -> {
            if(messageInput.getText().toString() != null) newGift.setMessage(messageInput.getText().toString());
            Intent intent = new Intent(this, DownloadSplashActivity.class);
            intent.putExtra(Globals.CURR_GIFT_KEY, newGift);
            intent.putExtra("GET FRIENDS", true);
            intent.putExtra("USER ID", mUserId);
            intent.putExtra(Globals.FRIEND_NAME_KEY, recipientName);
            intent.putExtra("FRIEND ID", recipientID);
            startActivity(intent);
        });

        closeButton.setOnClickListener(v16 ->{
            onBackPressed();
        });
    }

    // Set up spinner item.
    private void setUpSpinner() {
        //set up spinner
        giftTypeSpinner = findViewById(R.id.gift_type_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, Globals.GIFT_TYPE_ARRAY);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        giftTypeSpinner.setAdapter(spinnerArrayAdapter);
        if(newGift.getGiftType() != -1) giftTypeSpinner.setSelection(newGift.getGiftType());
        giftTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                newGift.setGiftType(GIFT_TYPE_MAP.get(selectedItem));
            }

            //TODO: to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // Make references to Layout items.
    private void wireInWidgets() {
        linkButton = findViewById(R.id.link_button);
        imageButton = findViewById(R.id.image_button);
        videoButton = findViewById(R.id.video_button);

        sendButton = findViewById(R.id.send_button);
        chooseFriendButton = findViewById(R.id.choose_recipient_button);
        sendButton.setEnabled(newGift.getMessage() != null && recipientID != null &&
                (newGift.getContentType().size() != 0 || newGift.getLinks().size() != 0));

        linksList = findViewById(R.id.linkList);
        linkCard = findViewById(R.id.linkCard);

        editButtons = findViewById(R.id.editButtons);
        recipientLabel = findViewById(R.id.recipient);
        reviewLabel = findViewById(R.id.review_label);
        reviewButton = findViewById(R.id.review_contents_button);
        closeButton = findViewById(R.id.close_button);
        messageInput = findViewById(R.id.message_input);

        toTitle = findViewById(R.id.toTitle);
        createGiftTitle = findViewById(R.id.createGiftTitle);
        spinnerCard = findViewById(R.id.spinnerCard);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        Log.d("LPC", "on back pressed: was opened? "+wasOpened);

        //added refresh on activity close no matter what - Logan
        intent.putExtra("NEED REFRESH", true);
        intent.putExtra("GOT GIFTS", true);
        startActivity(intent);
    }
}
