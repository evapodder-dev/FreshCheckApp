package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.firebase.FirebaseManager;
import com.google.android.material.card.MaterialCardView;

public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity_Lifecycle";

    private FirebaseAuthManager authManager;
    private FirebaseManager firebaseManager;

    private TextView tvAuthStatusBadge;
    private MaterialCardView cardUserProfile;
    private MaterialCardView cardSignInOptions;
    private TextView tvAvatarInitials;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileProvider;
    private TextView tvProfileUid;
    private ProgressBar pbAuthLoading;
    private TextView tvAuthLog;
    private TextView tvStatusRtdb;
    private TextView tvStatusFirestore;
    private com.google.android.material.textfield.TextInputEditText etAuthName;
    private com.google.android.material.textfield.TextInputEditText etAuthEmail;
    private com.google.android.material.textfield.TextInputEditText etAuthPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        authManager = FirebaseAuthManager.getInstance(this);
        firebaseManager = FirebaseManager.getInstance(this);

        // If user is already logged in, forward immediately to MainActivity
        // (unless explicitly requested to view account details via extra)
        boolean forceAuthView = getIntent().getBooleanExtra("force_auth_view", false);
        if (!forceAuthView && authManager.isUserLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        setContentView(R.layout.activity_auth);

        Toolbar toolbar = findViewById(R.id.toolbar_auth);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(forceAuthView && !isTaskRoot());
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (authManager.isUserLoggedIn()) {
                    finish();
                } else {
                    moveTaskToBack(true);
                }
            }
        });

        initViews();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        tvAuthStatusBadge = findViewById(R.id.tv_auth_status_badge);
        cardUserProfile = findViewById(R.id.card_user_profile);
        cardSignInOptions = findViewById(R.id.card_sign_in_options);
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileProvider = findViewById(R.id.tv_profile_provider);
        tvProfileUid = findViewById(R.id.tv_profile_uid);
        pbAuthLoading = findViewById(R.id.pb_auth_loading);
        tvAuthLog = findViewById(R.id.tv_auth_log);
        tvStatusRtdb = findViewById(R.id.tv_status_rtdb);
        tvStatusFirestore = findViewById(R.id.tv_status_firestore);
        etAuthName = findViewById(R.id.et_auth_name);
        etAuthEmail = findViewById(R.id.et_auth_email);
        etAuthPassword = findViewById(R.id.et_auth_password);

        if (firebaseManager.isRealtimeDatabaseAvailable()) {
            tvStatusRtdb.setText("Realtime Database Connected");
        } else {
            tvStatusRtdb.setText("Realtime Database: Local Mode");
        }

        if (firebaseManager.isFirestoreAvailable()) {
            tvStatusFirestore.setText("Cloud Firestore Connected");
        } else {
            tvStatusFirestore.setText("Cloud Firestore: Local Mode");
        }
    }

    private void setupListeners() {
        // Quick Demo Sign-Ins
        View btnDemoEva = findViewById(R.id.btn_demo_eva);
        if (btnDemoEva != null) {
            btnDemoEva.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLoading("Signing in as Eva Podder (Admin)...");
                    authManager.signInAsDemoUser("Eva Podder", "evapodder@freshcheck.app", "Admin Account", createAuthCallback("Demo Admin"));
                }
            });
        }

        View btnDemoRoommate = findViewById(R.id.btn_demo_roommate);
        if (btnDemoRoommate != null) {
            btnDemoRoommate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLoading("Signing in as Mess Roommate...");
                    authManager.signInAsDemoUser("Roommate", "roommate@freshcheck.app", "Roommate", createAuthCallback("Roommate"));
                }
            });
        }

        // Email & Password Sign-In
        View btnSignInEmail = findViewById(R.id.btn_sign_in_email);
        if (btnSignInEmail != null) {
            btnSignInEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = etAuthEmail != null && etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
                    String password = etAuthPassword != null && etAuthPassword.getText() != null ? etAuthPassword.getText().toString().trim() : "";
                    showLoading("Signing in with Email...");
                    authManager.signInWithEmail(email, password, createAuthCallback("Email"));
                }
            });
        }

        // Email & Password Register
        View btnRegisterEmail = findViewById(R.id.btn_register_email);
        if (btnRegisterEmail != null) {
            btnRegisterEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = etAuthName != null && etAuthName.getText() != null ? etAuthName.getText().toString().trim() : "";
                    String email = etAuthEmail != null && etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
                    String password = etAuthPassword != null && etAuthPassword.getText() != null ? etAuthPassword.getText().toString().trim() : "";
                    showLoading("Creating account...");
                    authManager.registerWithEmail(name, email, password, createAuthCallback("Email"));
                }
            });
        }

        // 1. Google Sign-In
        findViewById(R.id.btn_sign_in_google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading("Initiating Google Sign-In...");
                authManager.startGoogleSignIn(AuthActivity.this);
            }
        });

        // 2. Facebook Sign-In
        findViewById(R.id.btn_sign_in_facebook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading("Opening Facebook Authentication...");
                authManager.signInWithFacebook(AuthActivity.this, createAuthCallback("Facebook"));
            }
        });

        // 3. GitHub Sign-In
        findViewById(R.id.btn_sign_in_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading("Opening GitHub OAuth Authorization...");
                authManager.signInWithGitHub(AuthActivity.this, createAuthCallback("GitHub"));
            }
        });

        // 4. Guest / Anonymous
        findViewById(R.id.btn_sign_in_guest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading("Continuing as Guest...");
                authManager.signInAnonymously(createAuthCallback("Guest"));
            }
        });

        // 5. Sign Out
        findViewById(R.id.btn_sign_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSignOut();
            }
        });

        // 6. Continue to Dashboard
        View btnContinue = findViewById(R.id.btn_continue_to_dashboard);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // 7. Jump to Cloud Sync Center
        findViewById(R.id.btn_open_cloud_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AuthActivity.this, CloudSyncActivity.class);
                startActivity(intent);
            }
        });
    }

    private FirebaseAuthManager.AuthCallback createAuthCallback(final String providerLabel) {
        return new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onProgress(String message) {
                showLoading(message);
            }

            @Override
            public void onSuccess(String provider, String name, String email, String uid) {
                hideLoading();
                tvAuthLog.setText("✓ Signed in successfully with " + provider + "\nWelcome, " + name + "!");
                Toast.makeText(AuthActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                updateUI();

                // Smoothly finish and reveal MainActivity Dashboard
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            finish();
                        }
                    }
                }, 400);
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                tvAuthLog.setText("⚠️ Auth Error: " + errorMessage);
                Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                updateUI();
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FirebaseAuthManager.RC_SIGN_IN_GOOGLE) {
            authManager.handleGoogleSignInResult(data, createAuthCallback("Google"));
        }
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sign_out_confirm_title)
                .setMessage(R.string.sign_out_confirm_msg)
                .setPositiveButton(R.string.btn_sign_out, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLoading("Signing out...");
                        authManager.signOut(AuthActivity.this, new FirebaseAuthManager.SignOutCallback() {
                            @Override
                            public void onSignedOut() {
                                hideLoading();
                                tvAuthLog.setText("Signed out successfully. You are now browsing in local mode.");
                                Toast.makeText(AuthActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                                updateUI();
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateUI() {
        boolean signedIn = authManager.isUserLoggedIn();
        if (signedIn) {
            tvAuthStatusBadge.setText(R.string.status_signed_in);
            tvAuthStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
            tvAuthStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_fresh));

            cardUserProfile.setVisibility(View.VISIBLE);
            cardSignInOptions.setVisibility(View.GONE);

            String name = authManager.getUserDisplayName();
            String email = authManager.getUserEmail();
            String provider = authManager.getAuthProvider();
            String uid = authManager.getUserId();

            tvProfileName.setText(name);
            tvProfileEmail.setText(email);
            tvProfileProvider.setText(provider);
            tvProfileUid.setText("UID: " + uid);

            // Set avatar initials
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                if (parts.length >= 2) {
                    tvAvatarInitials.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
                } else {
                    tvAvatarInitials.setText(("" + parts[0].charAt(0)).toUpperCase());
                }
            } else {
                tvAvatarInitials.setText("FC");
            }
        } else {
            tvAuthStatusBadge.setText(R.string.status_not_signed_in);
            tvAuthStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            tvAuthStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_expiring));

            cardUserProfile.setVisibility(View.GONE);
            cardSignInOptions.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(String message) {
        pbAuthLoading.setVisibility(View.VISIBLE);
        tvAuthLog.setText(message);
    }

    private void hideLoading() {
        pbAuthLoading.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onBackPressed() {
        if (!authManager.isUserLoggedIn()) {
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}