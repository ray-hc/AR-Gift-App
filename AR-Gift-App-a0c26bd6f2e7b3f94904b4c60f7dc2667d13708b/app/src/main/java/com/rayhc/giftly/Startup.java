package com.rayhc.giftly;

import android.app.Application;

import java.util.HashMap;

public class Startup extends Application {
    private boolean isFistRun;
    private HashMap<String, String> sentGiftMap;
    private HashMap<String, String> receivedGiftMap;

    @Override
    public void onCreate() {
        super.onCreate();
        isFistRun = true;
        sentGiftMap = new HashMap<>();
        receivedGiftMap = new HashMap<>();
    }

    public boolean getFirstRun(){ return isFistRun; }

    public void setFistRun(boolean fistRun) { isFistRun = fistRun; }

    public HashMap<String, String> getSentGiftMap() {
        return sentGiftMap;
    }

    public void setSentGiftMap(HashMap<String, String> sentGiftMap) {
        this.sentGiftMap = sentGiftMap;
    }

    public HashMap<String, String> getReceivedGiftMap() {
        return receivedGiftMap;
    }

    public void setReceivedGiftMap(HashMap<String, String> receivedGiftMap) {
        this.receivedGiftMap = receivedGiftMap;
    }
}
