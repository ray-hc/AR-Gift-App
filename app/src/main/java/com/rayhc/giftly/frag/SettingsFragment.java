package com.rayhc.giftly.frag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

public class SettingsFragment extends Fragment {
    private Context context;
    private User user;

    private TextView tv1;
    private TextView tv2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tv1 = view.findViewById(R.id.tv_name);
        tv2 = view.findViewById(R.id.tv_email);

        context = getContext();

        loadEntry();

        return view;
    }

    public void onLogoutClicked(View view){
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.remove(Globals.USER_ID_KEY);
        editor.apply();
        getActivity().finish();
        System.exit(0);
    }

    public void loadEntry() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String displayUserID = sharedPref.getString("userId",null);

        Log.d("kitani", "User ID: " + displayUserID);

        Query query = db.child("users").orderByChild("userId").equalTo(displayUserID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user = UserManager.snapshotToUser(snapshot, displayUserID);

                tv1.setText(user.getName());
                tv2.setText(user.getEmail());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}