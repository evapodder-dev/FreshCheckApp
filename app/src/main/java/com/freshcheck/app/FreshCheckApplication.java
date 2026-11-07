package com.freshcheck.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class FreshCheckApplication extends Application {
    private static final String TAG = "FreshCheckApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application initializing...");
        try {
            com.freshcheck.app.firebase.FirebaseConfigHelper.initialize(this);
            Log.d(TAG, "Firebase initialized via FirebaseConfigHelper");
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization handled safely: " + e.getMessage());
        }
    }
}
