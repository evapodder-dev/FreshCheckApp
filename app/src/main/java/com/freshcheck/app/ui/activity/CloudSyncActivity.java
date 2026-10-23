package com.freshcheck.app.ui.activity;

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
import com.freshcheck.app.firebase.FirebaseManager;
import com.freshcheck.app.model.FoodItem;

import java.util.List;

public class CloudSyncActivity extends AppCompatActivity {
    private static final String TAG = "CloudSync_Lifecycle";

    private TextView tvStatusBadge;
    private TextView tvLog;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
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
        foodRepository = new FoodRepository(this);

        tvStatusBadge = findViewById(R.id.tv_firebase_status_badge);
        tvLog = findViewById(R.id.tv_cloud_sync_log);
        progressBar = findViewById(R.id.pb_cloud_sync);

        updateConnectionBadge();

        findViewById(R.id.btn_upload_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToCloud();
            }
        });

        findViewById(R.id.btn_download_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFromCloud();
            }
        });
    }

    private void updateConnectionBadge() {
        if (firebaseManager.isConfigured()) {
            tvStatusBadge.setText("Firebase Connected & Ready");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_fresh));
        } else {
            tvStatusBadge.setText("Offline / Local Persistence Mode");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_expiring));
        }
    }

    private void uploadToCloud() {
        Log.d(TAG, "Initiating cloud inventory upload...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Preparing inventory for upload...");

        List<FoodItem> items = foodRepository.getAllActive();

        firebaseManager.uploadInventory(items, new FirebaseManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                tvLog.setText(message);
            }

            @Override
            public void onSuccess(String message, int itemsSynced) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("✓ " + message);
                Toast.makeText(CloudSyncActivity.this, message, Toast.LENGTH_SHORT).show();
                updateConnectionBadge();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("ℹ️ " + errorMessage);
                Toast.makeText(CloudSyncActivity.this, "Offline: Check Firebase configuration", Toast.LENGTH_SHORT).show();
                updateConnectionBadge();
            }
        });
    }

    private void downloadFromCloud() {
        Log.d(TAG, "Initiating cloud inventory download...");
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Checking remote Firebase database...");

        firebaseManager.downloadInventory(new FirebaseManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                tvLog.setText(message);
            }

            @Override
            public void onSuccess(String message, int itemsSynced) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("✓ " + message);
                Toast.makeText(CloudSyncActivity.this, message, Toast.LENGTH_SHORT).show();
                updateConnectionBadge();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("ℹ️ " + errorMessage);
                Toast.makeText(CloudSyncActivity.this, "Offline: Check Firebase configuration", Toast.LENGTH_SHORT).show();
                updateConnectionBadge();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
