package com.freshcheck.app.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.freshcheck.app.R;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.receiver.InventoryAuditReceiver;
import com.freshcheck.app.receiver.PowerStateReceiver;
import com.freshcheck.app.service.InventoryAuditService;
import com.freshcheck.app.ui.fragment.AuthFragment;
import com.freshcheck.app.ui.fragment.DashboardFragment;
import com.freshcheck.app.ui.fragment.InventoryFragment;
import com.freshcheck.app.ui.fragment.ProfileFragment;
import com.freshcheck.app.ui.fragment.RecipesFragment;
import com.freshcheck.app.ui.fragment.ShoppingFragment;
import com.freshcheck.app.util.NotificationHelper;
import com.freshcheck.app.util.WorkManagerScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_Lifecycle";
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 201;

    private BottomNavigationView bottomNav;
    private InventoryAuditReceiver auditReceiver;
    private PowerStateReceiver powerStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Notification Channel setup & Permission check safely
        try {
            NotificationHelper.createNotificationChannel(this);
            checkNotificationPermission();
        } catch (Exception e) {
            Log.w(TAG, "Notification channel or permission setup handled: " + e.getMessage());
        }

        // Schedule background worker safely
        try {
            WorkManagerScheduler.schedulePeriodicExpiryCheck(this);
        } catch (Exception e) {
            Log.w(TAG, "WorkManager initialization handled: " + e.getMessage());
        }

        // Handle navigation from notification extra if present
        handleNotificationIntent(getIntent());

        // Auth Gate: Check if user is logged in
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(this);
        if (!authManager.isUserLoggedIn()) {
            showAuthScreen();
        } else if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && "inventory".equals(intent.getStringExtra("target_tab"))) {
            navigateToTab(R.id.nav_inventory);
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_inventory) {
                selectedFragment = new InventoryFragment();
            } else if (itemId == R.id.nav_recipes) {
                selectedFragment = new RecipesFragment();
            } else if (itemId == R.id.nav_shopping) {
                selectedFragment = new ShoppingFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    public void navigateToTab(int menuItemId) {
        bottomNav.setSelectedItemId(menuItemId);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Expiry notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied; alerts will not show in tray", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_auth) {
            showAuthScreen();
            return true;
        } else if (id == R.id.action_cloud_sync) {
            Intent intent = new Intent(this, CloudSyncActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_audit_service) {
            Intent serviceIntent = new Intent(this, InventoryAuditService.class);
            startService(serviceIntent);
            Toast.makeText(this, "Started background InventoryAuditService", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_content_provider) {
            Intent intent = new Intent(this, ProviderDemoActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("FreshCheck — Syllabus & Architecture")
                .setMessage("FreshCheck: Smart Food Management & Waste Reduction App\n\n"
                        + "Key Syllabus Coverage:\n"
                        + "• Multi-Activity Architecture with logged lifecycles\n"
                        + "• SQLite Local Database & DAOs\n"
                        + "• Firebase Realtime Database cloud sync\n"
                        + "• Started Service (InventoryAuditService)\n"
                        + "• BroadcastReceivers (Audit, Expiry Alarm, Boot)\n"
                        + "• Custom ContentProvider (FoodContentProvider)\n"
                        + "• ContentResolver queries with live Cursor inspection\n"
                        + "• WorkManager (ExpiryCheckWorker)\n"
                        + "• Explicit & Implicit Intents\n"
                        + "• Custom Canvas Views (Expiry Donut & Waste Chart)\n"
                        + "• HTTP Requests & JSON Parsing (TheMealDB API)")
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
        auditReceiver = new InventoryAuditReceiver();
        IntentFilter filter = new IntentFilter(InventoryAuditService.ACTION_AUDIT_COMPLETE);
        ContextCompat.registerReceiver(this, auditReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Register system broadcast receiver for battery, power, and airplane mode transitions
        powerStateReceiver = new PowerStateReceiver();
        IntentFilter powerFilter = new IntentFilter();
        powerFilter.addAction(Intent.ACTION_BATTERY_LOW);
        powerFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        powerFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        powerFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        powerFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        ContextCompat.registerReceiver(this, powerStateReceiver, powerFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        // Auth Gate: Ensure user is still logged in when activity is resumed
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(this);
        if (!authManager.isUserLoggedIn()) {
            showAuthScreen();
        } else if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null 
                || getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof AuthFragment) {
            onUserAuthenticated();
        }
    }

    public void onUserAuthenticated() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
        View appBar = findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(View.VISIBLE);
        }
        loadFragment(new DashboardFragment());
    }

    public void showAuthScreen() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
        View appBar = findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(View.GONE);
        }
        loadFragment(new AuthFragment());
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof AuthFragment) {
            FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(this);
            if (!authManager.isUserLoggedIn()) {
                moveTaskToBack(true);
                return;
            }
        }
        super.onBackPressed();
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
        if (auditReceiver != null) {
            try {
                unregisterReceiver(auditReceiver);
            } catch (Exception ignored) {}
        }
        if (powerStateReceiver != null) {
            try {
                unregisterReceiver(powerStateReceiver);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
