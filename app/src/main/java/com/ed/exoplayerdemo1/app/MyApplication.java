package com.ed.exoplayerdemo1.app;

import android.app.Application;

/**
 * app
 * Created by ljg
 */

public class MyApplication extends Application {

    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }

    public static MyApplication getInstance() {
        return instance;
    }
}
