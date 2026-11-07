package com.freshcheck.app.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.freshcheck.app.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Handles initialization, options validation, and live configuration management
 * for Google Firebase services (Authentication, Realtime Database, Cloud Firestore).
 */
public class FirebaseConfigHelper {
    private static final String TAG = "FirebaseConfigHelper";
    private static final String PREF_NAME = "freshcheck_firebase_config";
    private static final String KEY_CUSTOM_RTDB_URL = "custom_rtdb_url";
    private static final String KEY_CUSTOM_PROJECT_ID = "custom_project_id";

    private static boolean isInitialized = false;

    public static synchronized void initialize(Context context) {
        if (isInitialized && !FirebaseApp.getApps(context).isEmpty()) {
            return;
        }

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                String appId = getAppId(context);
                String apiKey = getApiKey(context);
                String projectId = getProjectId(context);
                String databaseUrl = getDatabaseUrl(context);
                String storageBucket = getStorageBucket(context);
                String gcmSenderId = getGcmSenderId(context);

                FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .setDatabaseUrl(databaseUrl);

                if (storageBucket != null && !storageBucket.isEmpty()) {
                    builder.setStorageBucket(storageBucket);
                }
                if (gcmSenderId != null && !gcmSenderId.isEmpty()) {
                    builder.setGcmSenderId(gcmSenderId);
                }

                FirebaseOptions options = builder.build();
                FirebaseApp.initializeApp(context.getApplicationContext(), options);
                Log.i(TAG, "FirebaseApp successfully initialized with project: " + projectId + " (RTDB: " + databaseUrl + ")");
            }

            // Configure Realtime Database offline persistence safely
            try {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                db.setPersistenceEnabled(true);
                Log.d(TAG, "Realtime Database offline persistence enabled");
            } catch (Exception e) {
                Log.d(TAG, "Realtime DB persistence note (already active): " + e.getMessage());
            }

            // Configure Cloud Firestore offline persistence safely
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build();
                firestore.setFirestoreSettings(settings);
                Log.d(TAG, "Cloud Firestore offline persistence enabled");
            } catch (Exception e) {
                Log.d(TAG, "Firestore settings note (already active): " + e.getMessage());
            }

            isInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    public static String getAppId(Context context) {
        try {
            return context.getString(R.string.google_app_id);
        } catch (Exception e) {
            return "1:123456789012:android:abcdef0123456789";
        }
    }

    public static String getApiKey(Context context) {
        try {
            return context.getString(R.string.google_api_key);
        } catch (Exception e) {
            return "AIzaSyDemoKeyReplaceWithYourOwnFirebaseKey";
        }
    }

    public static String getProjectId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String custom = prefs.getString(KEY_CUSTOM_PROJECT_ID, null);
        if (custom != null && !custom.trim().isEmpty()) {
            return custom.trim();
        }
        try {
            return context.getString(R.string.project_id);
        } catch (Exception e) {
            return "freshcheck-app-demo";
        }
    }

    public static String getDatabaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String custom = prefs.getString(KEY_CUSTOM_RTDB_URL, null);
        if (custom != null && !custom.trim().isEmpty()) {
            return custom.trim();
        }
        try {
            return context.getString(R.string.firebase_database_url);
        } catch (Exception e) {
            String projId = getProjectId(context);
            return "https://" + projId + "-default-rtdb.firebaseio.com";
        }
    }

    public static String getStorageBucket(Context context) {
        try {
            return context.getString(R.string.google_storage_bucket);
        } catch (Exception e) {
            return getProjectId(context) + ".appspot.com";
        }
    }

    public static String getGcmSenderId(Context context) {
        try {
            return context.getString(R.string.gcm_defaultSenderId);
        } catch (Exception e) {
            return "123456789012";
        }
    }

    public static void setCustomRealtimeDatabaseUrl(Context context, String url) {
        if (url == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_RTDB_URL, url.trim())
                .apply();
        isInitialized = false;
        initialize(context);
    }

    public static void setCustomProjectId(Context context, String projectId) {
        if (projectId == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_PROJECT_ID, projectId.trim())
                .apply();
        isInitialized = false;
        initialize(context);
    }
}
