package com.rayhc.giftly.frag;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.WriterException;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.Globals;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class SettingsFragment extends Fragment {
    private Context context;
    private User user;

    private TextView tv1;
    private TextView tv2;
    private ImageView iv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        //wire in views
        tv1 = view.findViewById(R.id.tv_name);
        tv2 = view.findViewById(R.id.tv_email);
        iv = (ImageView)view.findViewById(R.id.qrview);


        context = getContext();

        //get the user's data
        loadEntry();

        return view;
    }

    /**
     * Close the app on logout and clear userID from shared preferences
     */
    public void onLogoutClicked(View view){
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.remove(Globals.USER_ID_KEY);
        editor.apply();
        getActivity().finish();
        System.exit(0);
    }

    /**
     * Load user data for viewing
     */
    public void loadEntry() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String displayUserID = sharedPref.getString("userId",null);

        WindowManager manager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        Point point = new Point();
        display.getSize(point);

        int width = point.x;
        int height = point.y;

        int dimen = Math.min(width, height);
        dimen = dimen * 3 / 4;


        //create a QR code to add this user as a friend via scan
        QRGEncoder qrEncoder = new QRGEncoder("https://ianmkim.com/joyshare?userId=" +  displayUserID, null, QRGContents.Type.TEXT, dimen);

        try {
            Bitmap bitmap = qrEncoder.encodeAsBitmap();
            iv.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        Log.d("kitani", "User ID: " + displayUserID);

        //query for user data
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