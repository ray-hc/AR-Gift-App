package com.rayhc.giftly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.rayhc.giftly.ui.CreateGiftFragment;

public class FragmentContainerActivity extends AppCompatActivity {
    private Gift mGift;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_container);

        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        Log.d("LPC", "container activity got gift: "+mGift.toString());


        CreateGiftFragment createGiftFragment = new CreateGiftFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Globals.CURR_GIFT_KEY, mGift);
        createGiftFragment.setArguments(bundle);

        // Begin the transaction
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.frame_container, createGiftFragment);
        // or ft.add(R.id.your_placeholder, new FooFragment());
        // Complete the changes added above
        ft.commit();
    }
}
