package com.rayhc.giftly.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.MainActivity;
import com.rayhc.giftly.R;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class NotifService extends Service {

    //notification stuff
    public static final String CHANNEL_ID = "channel id";
    public static final String CHANNEL_NAME = "Joyshare";
    public static final int NOTIFICATION_ID = 1;

    private boolean firstRun = true;

    private DatabaseReference mDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String userID;
    private SharedPreferences sharedPref;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("iandebug", "service started");
        //set up firebase stuff
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        //get current user id
        if(mFirebaseUser == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            userID = sharedPref.getString("userId", null);
        } else {
            userID = mFirebaseUser.getUid();
        }

        //build the listener for the user's received gifts
        Query query = mDatabase.child("users").child(userID).child("receivedGifts");

        //listener for the user's receivedGifts data
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //BAD QUERIES (i.e. wrong pin) == !snapshot.exists()
                if(!firstRun){
                    Log.d("LPC", "snapshot: " + snapshot.getValue());
                    if (snapshot.exists()) {
                        buildNotification();
                    } else {
                        Log.d("LPC", "snapshot doesn't exist");
                    }
                } else {
                    firstRun = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        Log.d("iandebug","userid in service: " + userID);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Create a notification channel
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Create a notification that will launch when a gift is received
     */
    public void buildNotification() {
        Context context = getApplicationContext();
        String notificationTitle = "Joyshare";
        String notificationText = "You just received a new gift";

        Intent intent = new Intent(context, MainActivity.class);

//        intent.putExtras(bundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.gift_blue)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(NOTIFICATION_ID, builder.build());

        Log.d("LPC", "setUpNotification: built");
    }
}
