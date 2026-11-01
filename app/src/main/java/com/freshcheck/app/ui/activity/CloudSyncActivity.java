package com.freshcheck.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.firebase.FirebaseManager;
import com.freshcheck.app.model.FoodItem;

import java.util.List;

public class CloudSyncActivity extends AppCompatActivity {
    private static final String TAG = "CloudSync_Lifecycle";

    private TextView tvStatusBadge;
    private TextView tvLog;
    private ProgressBar progressBar;
    private TextView tvAuthUserSummary;
    private TextView tvAuthProviderSummary;

    private FirebaseManager firebaseManager;
    private FirebaseAuthManager authManager;
    private FoodRepository foodRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_cloud_sync);

        Toolbar toolbar = findViewById(R.id.toolbar_cloud_sync);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        firebaseManager = FirebaseManager.getInstance(this);
        authManager = FirebaseAuthManager.getInstance(this);
        foodRepository = new FoodRepository(this);

        tvStatusBadge = findViewById(R.id.tv_firebase_status_badge);
        tvLog = findViewById(R.id.tv_cloud_sync_log);
        progressBar = findViewById(R.id.pb_cloud_sync);
        tvAuthUserSummary = findViewById(R.id.tv_auth_user_summary);
        tvAuthProviderSummary = findViewById(R.id.tv_auth_provider_summary);

        findViewById(R.id.btn_manage_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CloudSyncActivity.this, AuthActivity.class);
                startActivity(intent);
            }
        });

        // 1. Realtime DB actions
        findViewById(R.id.btn_upload_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToRealtimeDatabase();
            }
        });

        findViewById(R.id.btn_download_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFromRealtimeDatabase();
            }
        });

        // 2. Cloud Firestore actions
        findViewById(R.id.btn_upload_firestore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToCloudFirestore();
            }
        });

        findViewById(R.id.btn_download_firestore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFromCloudFirestore();
            }
        });

        // 3. Sync All
        findViewById(R.id.btn_sync_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncAllDatabases();
            }
        });

        updateUI();
    }

    private void updateUI() {
        updateConnectionBadge();
        updateAuthSummary();
    }

    private void updateAuthSummary() {
        if (authManager.isUserLoggedIn()) {
            tvAuthUserSummary.setText(authManager.getUserDisplayName());
            tvAuthProviderSummary.setText("Signed in with " + authManager.getAuthProvider() + " (" + authManager.getUserEmail() + ")");
        } else {
            tvAuthUserSummary.setText("Guest / Local Mode");
            tvAuthProviderSummary.setText("Tap 'Account' to sign in with Google, FB or GitHub");
        }
    }

    private void updateConnectionBadge() {
        if (firebaseManager.isConfigured()) {
            tvStatusBadge.setText("Firebase Connected & Ready (Realtime DB & Firestore)");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_fresh));
        } else {
            tvStatusBadge.setText("Offline / Local Persistence Mode");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_expiring));
        }
    }

    private void uploadToRealtimeDatabase() {
        Log.d(TAG, "Initiating Realtime DB upload...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Preparing items for Realtime Database upload...");

        List<FoodItem> items = foodRepository.getAllActive();
        firebaseManager.uploadToRealtimeDatabase(items, createSyncCallback("Realtime Database Upload"));
    }

    private void downloadFromRealtimeDatabase() {
        Log.d(TAG, "Initiating Realtime DB download...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Checking remote Realtime Database...");

        firebaseManager.downloadFromRealtimeDatabase(createSyncCallback("Realtime Database Download"));
    }

    private void uploadToCloudFirestore() {
        Log.d(TAG, "Initiating Cloud Firestore upload...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Preparing items for Cloud Firestore upload...");

        List<FoodItem> items = foodRepository.getAllActive();
        firebaseManager.uploadToFirestore(items, createSyncCallback("Cloud Firestore Upload"));
    }

    private void downloadFromCloudFirestore() {
        Log.d(TAG, "Initiating Cloud Firestore download...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Fetching documents from Cloud Firestore...");

        firebaseManager.downloadFromFirestore(createSyncCallback("Cloud Firestore Download"));
    }

    private void syncAllDatabases() {
        Log.d(TAG, "Initiating complete cloud multi-database sync...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Starting multi-database sync (Realtime DB + Firestore)...");

        List<FoodItem> items = foodRepository.getAllActive();
        firebaseManager.syncAll(items, createSyncCallback("Full Cloud Multi-Sync"));
    }

    private FirebaseManager.SyncCallback createSyncCallback(final String operationName) {
        return new FirebaseManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                tvLog.setText(message);
            }

            @Override
            public void onSuccess(String message, int itemsSynced) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("✓ [" + operationName + "]: " + message);
                Toast.makeText(CloudSyncActivity.this, message, Toast.LENGTH_SHORT).show();
                updateUI();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("ℹ️ [" + operationName + "]: " + errorMessage);
                Toast.makeText(CloudSyncActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}