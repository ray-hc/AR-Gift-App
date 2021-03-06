package com.rayhc.giftly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayerActivity;

public class DemoStartUnityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_start_unity);

        Intent intent = new Intent(this, UnityPlayerActivity.class);
        startActivity(intent);
    }
}