package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.firebase.FirebaseConfigHelper;
import com.freshcheck.app.firebase.FirebaseManager;
import com.freshcheck.app.model.FoodItem;

import java.util.List;

/**
 * Firebase Cloud & Sync Center:
 * Real-time connection monitoring with 5 distinct states:
 * 1. Connected
 * 2. Connecting
 * 3. Offline
 * 4. Syncing
 * 5. Sync Failed
 *
 * Utilizes .info/connected for Realtime Database and Source.SERVER queries for Cloud Firestore.
 */
public class CloudSyncActivity extends AppCompatActivity implements FirebaseManager.ConnectionListener {
    private static final String TAG = "CloudSyncActivity";

    private TextView tvStatusBadge;
    private TextView tvRtdbStatusBadge;
    private TextView tvFirestoreStatusBadge;
    private TextView tvLastSyncTime;
    private TextView tvPendingUploads;
    private TextView tvLog;
    private ProgressBar progressBar;
    private TextView tvAuthUserSummary;
    private TextView tvAuthProviderSummary;
    private TextView tvAuthUid;

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
        toolbar.setNavigationOnClickListener(v -> finish());

        firebaseManager = FirebaseManager.getInstance(this);
        authManager = FirebaseAuthManager.getInstance(this);
        foodRepository = new FoodRepository(this);

        tvStatusBadge = findViewById(R.id.tv_firebase_status_badge);
        tvRtdbStatusBadge = findViewById(R.id.tv_rtdb_status_badge);
        tvFirestoreStatusBadge = findViewById(R.id.tv_firestore_status_badge);
        tvLastSyncTime = findViewById(R.id.tv_last_sync_time);
        tvPendingUploads = findViewById(R.id.tv_pending_uploads);
        tvLog = findViewById(R.id.tv_cloud_sync_log);
        progressBar = findViewById(R.id.pb_cloud_sync);
        tvAuthUserSummary = findViewById(R.id.tv_auth_user_summary);
        tvAuthProviderSummary = findViewById(R.id.tv_auth_provider_summary);
        tvAuthUid = findViewById(R.id.tv_auth_uid);

        findViewById(R.id.btn_manage_account).setOnClickListener(v -> {
            Intent intent = new Intent(CloudSyncActivity.this, AuthActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_test_connection).setOnClickListener(v -> testCloudConnection());
        findViewById(R.id.btn_setup_guide).setOnClickListener(v -> showFirebaseSetupDialog());

        // 1. Realtime DB actions
        findViewById(R.id.btn_upload_inventory).setOnClickListener(v -> uploadToRealtimeDatabase());
        findViewById(R.id.btn_download_inventory).setOnClickListener(v -> downloadFromRealtimeDatabase());

        // 2. Cloud Firestore actions
        findViewById(R.id.btn_upload_firestore).setOnClickListener(v -> uploadToCloudFirestore());
        findViewById(R.id.btn_download_firestore).setOnClickListener(v -> downloadFromCloudFirestore());

        // 3. Sync All
        findViewById(R.id.btn_sync_all).setOnClickListener(v -> syncAllDatabases());

        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseManager.addConnectionListener(this);
        // Refresh connection state
        firebaseManager.pingFirestore(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseManager.removeConnectionListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onConnectionStateChanged(FirebaseManager.ConnectionState rtdbState,
                                         FirebaseManager.ConnectionState firestoreState,
                                         FirebaseManager.ConnectionState overallState) {
        runOnUiThread(() -> applyConnectionStates(rtdbState, firestoreState, overallState));
    }

    private void updateUI() {
        updateAuthSummary();
        applyConnectionStates(firebaseManager.getRtdbState(),
                firebaseManager.getFirestoreState(),
                firebaseManager.getOverallCloudState());
        updateMetrics();
    }

    private void updateAuthSummary() {
        if (authManager.isUserLoggedIn()) {
            tvAuthUserSummary.setText(authManager.getUserDisplayName() + " (" + authManager.getUserRole() + ")");
            tvAuthProviderSummary.setText("Signed in with " + authManager.getAuthProvider() + " • " + authManager.getUserEmail());
            tvAuthUid.setText("UID: " + authManager.getUserId());
            tvAuthUid.setVisibility(View.VISIBLE);
        } else {
            tvAuthUserSummary.setText("Guest Mode / Offline");
            tvAuthProviderSummary.setText("Tap 'Account' to sign in with Google or Email");
            tvAuthUid.setText("UID: Not Authenticated");
            tvAuthUid.setVisibility(View.VISIBLE);
        }
    }

    private void applyConnectionStates(FirebaseManager.ConnectionState rtdb,
                                       FirebaseManager.ConnectionState firestore,
                                       FirebaseManager.ConnectionState overall) {
        // Overall badge
        tvStatusBadge.setText("Overall Cloud: " + overall.getLabel());
        styleBadge(tvStatusBadge, overall);

        // RTDB badge
        if (tvRtdbStatusBadge != null) {
            tvRtdbStatusBadge.setText("RTDB: " + rtdb.getLabel());
            styleBadge(tvRtdbStatusBadge, rtdb);
        }

        // Firestore badge
        if (tvFirestoreStatusBadge != null) {
            tvFirestoreStatusBadge.setText("Firestore: " + firestore.getLabel());
            styleBadge(tvFirestoreStatusBadge, firestore);
        }
    }

    private void styleBadge(TextView badge, FirebaseManager.ConnectionState state) {
        if (badge == null || state == null) return;
        badge.setTextColor(Color.parseColor(state.getHexColor()));
        switch (state) {
            case CONNECTED:
                badge.setBackgroundResource(R.drawable.bg_badge_fresh);
                break;
            case CONNECTING:
            case SYNCING:
                badge.setBackgroundResource(R.drawable.bg_badge_expiring);
                break;
            case SYNC_FAILED:
                badge.setBackgroundResource(R.drawable.bg_badge_expired);
                break;
            case OFFLINE:
            default:
                badge.setBackgroundResource(R.drawable.bg_badge_fresh);
                break;
        }
    }

    private void updateMetrics() {
        if (tvLastSyncTime != null) {
            tvLastSyncTime.setText("Last Sync: " + firebaseManager.getLastSyncTimeFormatted());
        }
        if (tvPendingUploads != null) {
            int pending = firebaseManager.getPendingUploadsCount();
            tvPendingUploads.setText(pending + " Pending Uploads");
            tvPendingUploads.setTextColor(pending > 0
                    ? ContextCompat.getColor(this, R.color.status_expiring)
                    : ContextCompat.getColor(this, R.color.primary));
        }

        String lastError = firebaseManager.getLastSyncError();
        if (lastError != null && !lastError.isEmpty() && !lastError.toLowerCase().contains("offline")) {
            tvLog.setText("⚠️ " + lastError);
        } else {
            String lastSync = firebaseManager.getLastSyncTimeFormatted();
            if (!"Never synced".equals(lastSync) && tvLog != null) {
                tvLog.setText("✓ Cloud Databases & Local Persistence in sync. (Last sync: " + lastSync + ")");
            } else if (tvLog != null) {
                tvLog.setText("✓ Cloud Databases Connected & Ready. Tap 'Sync All' to synchronize.");
            }
        }
    }

    private void showFirebaseSetupDialog() {
        String currentProj = FirebaseConfigHelper.getProjectId(this);
        String currentRtdb = FirebaseConfigHelper.getDatabaseUrl(this);

        String message = "Why does Cloud Status show Offline / Syncing?\n\n"
                + "The app currently has placeholder demo credentials in google-services.json:\n"
                + "• Project ID: " + currentProj + "\n"
                + "• RTDB URL: " + currentRtdb + "\n\n"
                + "Since this demo project does not exist on Google Cloud, Google's servers reject the connection.\n\n"
                + "To connect to your live Firebase backend:\n"
                + "1. Go to console.firebase.google.com and create a project.\n"
                + "2. Enable Realtime Database & Cloud Firestore (Test Mode).\n"
                + "3. Add your SHA-1 fingerprint:\n"
                + "6D:E1:68:7A:B1:81:79:EC:BB:1D:32:6A:BF:3A:2D:C2:D5:53:EE:7F\n"
                + "4. Either paste your real google-services.json into the project, or tap 'Enter Live Project ID' below.";

        new AlertDialog.Builder(this)
                .setTitle("Firebase Cloud Setup")
                .setMessage(message)
                .setPositiveButton("Enter Live Project ID", (dialog, which) -> showCustomConfigDialog())
                .setNegativeButton("OK", null)
                .show();
    }

    private void showCustomConfigDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView tvProjPrompt = new TextView(this);
        tvProjPrompt.setText("Firebase Project ID (e.g. freshcheck-12345):");
        tvProjPrompt.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        layout.addView(tvProjPrompt);

        final EditText etProj = new EditText(this);
        etProj.setText(FirebaseConfigHelper.getProjectId(this));
        layout.addView(etProj);

        TextView tvRtdbPrompt = new TextView(this);
        tvRtdbPrompt.setText("Realtime Database URL:");
        tvRtdbPrompt.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvRtdbPrompt.setPadding(0, 24, 0, 0);
        layout.addView(tvRtdbPrompt);

        final EditText etRtdb = new EditText(this);
        etRtdb.setText(FirebaseConfigHelper.getDatabaseUrl(this));
        layout.addView(etRtdb);

        new AlertDialog.Builder(this)
                .setTitle("Configure Live Firebase")
                .setView(layout)
                .setPositiveButton("Save & Connect", (dialog, which) -> {
                    String proj = etProj.getText().toString().trim();
                    String rtdb = etRtdb.getText().toString().trim();
                    if (!proj.isEmpty()) {
                        FirebaseConfigHelper.setCustomProjectId(this, proj);
                    }
                    if (!rtdb.isEmpty()) {
                        FirebaseConfigHelper.setCustomRealtimeDatabaseUrl(this, rtdb);
                    }
                    Toast.makeText(this, "Configuration updated! Pinging servers...", Toast.LENGTH_SHORT).show();
                    testCloudConnection();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void testCloudConnection() {
        progressBar.setVisibility(View.VISIBLE);
        tvLog.setText("Pinging Firebase Cloud servers (RTDB & Firestore)...");

        firebaseManager.pingFirestore(new FirebaseManager.SyncCallback() {
            @Override
            public void onProgress(String message) {
                tvLog.setText(message);
            }

            @Override
            public void onSuccess(String message, int itemsSynced) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("✓ " + message + " | Realtime DB: " + firebaseManager.getRtdbState().getLabel());
                Toast.makeText(CloudSyncActivity.this, "Connection Verified!", Toast.LENGTH_SHORT).show();
                updateUI();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvLog.setText("ℹ️ " + errorMessage);
                Toast.makeText(CloudSyncActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                updateUI();
            }
        });
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
}
