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
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "FirebaseApp initialized in Application");
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization handled safely: " + e.getMessage());
        }
    }
}
