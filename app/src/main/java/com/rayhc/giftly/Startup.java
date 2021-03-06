package com.rayhc.giftly;

import android.app.Application;

public class Startup extends Application {
    private boolean isFistRun;

    @Override
    public void onCreate() {
        super.onCreate();
        isFistRun = true;
    }

    public boolean getFirstRun(){ return isFistRun; }

    public void setFistRun(boolean fistRun) { isFistRun = fistRun; }
}
