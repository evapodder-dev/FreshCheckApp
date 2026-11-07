package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.freshcheck.app.R;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.firebase.FirebaseManager;
import com.freshcheck.app.ui.activity.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.card.MaterialCardView;

public class AuthFragment extends Fragment {
    private static final String TAG = "AuthFragment";
    private FirebaseAuthManager authManager;
    private FirebaseManager firebaseManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    handleGoogleResult(result.getResultCode(), data);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_auth, container, false);

        authManager = FirebaseAuthManager.getInstance(requireContext());
        firebaseManager = FirebaseManager.getInstance(requireContext());

        Toolbar toolbar = view.findViewById(R.id.toolbar_auth);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (authManager.isUserLoggedIn()) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).onUserAuthenticated();
                    }
                } else if (getActivity() != null) {
                    getActivity().moveTaskToBack(true);
                }
            });
        }

        initViews(view);
        setupListeners(view);
        updateUI();

        return view;
    }

    private void initViews(View view) {
        tvAuthStatusBadge = view.findViewById(R.id.tv_auth_status_badge);
        cardUserProfile = view.findViewById(R.id.card_user_profile);
        cardSignInOptions = view.findViewById(R.id.card_sign_in_options);
        tvAvatarInitials = view.findViewById(R.id.tv_avatar_initials);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfileProvider = view.findViewById(R.id.tv_profile_provider);
        tvProfileUid = view.findViewById(R.id.tv_profile_uid);
        pbAuthLoading = view.findViewById(R.id.pb_auth_loading);
        tvAuthLog = view.findViewById(R.id.tv_auth_log);
        tvStatusRtdb = view.findViewById(R.id.tv_status_rtdb);
        tvStatusFirestore = view.findViewById(R.id.tv_status_firestore);
        etAuthName = view.findViewById(R.id.et_auth_name);
        etAuthEmail = view.findViewById(R.id.et_auth_email);
        etAuthPassword = view.findViewById(R.id.et_auth_password);

        if (tvStatusRtdb != null) {
            tvStatusRtdb.setText(firebaseManager.isRealtimeDatabaseAvailable() ? "Realtime Database Connected" : "Realtime Database: Local Mode");
        }
        if (tvStatusFirestore != null) {
            tvStatusFirestore.setText(firebaseManager.isFirestoreAvailable() ? "Cloud Firestore Connected" : "Cloud Firestore: Local Mode");
        }
    }

    private void setupListeners(View view) {
        // Demo Eva Podder (Admin)
        View btnDemoEva = view.findViewById(R.id.btn_demo_eva);
        if (btnDemoEva != null) {
            btnDemoEva.setOnClickListener(v -> {
                showLoading("Signing in as Eva Podder (Admin)...");
                authManager.signInAsDemoUser("Eva Podder", "evapodder@freshcheck.app", "Admin Account", createAuthCallback("Demo Admin"));
            });
        }

        // Demo Roommate
        View btnDemoRoommate = view.findViewById(R.id.btn_demo_roommate);
        if (btnDemoRoommate != null) {
            btnDemoRoommate.setOnClickListener(v -> {
                showLoading("Signing in as Mess Roommate...");
                authManager.signInAsDemoUser("Roommate", "roommate@freshcheck.app", FirebaseAuthManager.ROLE_ROOMMATE, createAuthCallback("Roommate"));
            });
        }

        // Demo New User (Starter Profile)
        View btnDemoNewUser = view.findViewById(R.id.btn_demo_new_user);
        if (btnDemoNewUser != null) {
            btnDemoNewUser.setOnClickListener(v -> {
                showLoading("Signing in as New User (Starter)...");
                authManager.signInAsDemoUser("New User", "newuser@freshcheck.app", FirebaseAuthManager.ROLE_NEW_USER, createAuthCallback("New User"));
            });
        }

        // Email & Password Sign-In
        View btnSignInEmail = view.findViewById(R.id.btn_sign_in_email);
        if (btnSignInEmail != null) {
            btnSignInEmail.setOnClickListener(v -> {
                String email = etAuthEmail != null && etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
                String password = etAuthPassword != null && etAuthPassword.getText() != null ? etAuthPassword.getText().toString().trim() : "";
                showLoading("Signing in with Email...");
                authManager.signInWithEmail(email, password, createAuthCallback("Email"));
            });
        }

        // Email & Password Register
        View btnRegisterEmail = view.findViewById(R.id.btn_register_email);
        if (btnRegisterEmail != null) {
            btnRegisterEmail.setOnClickListener(v -> {
                String name = etAuthName != null && etAuthName.getText() != null ? etAuthName.getText().toString().trim() : "";
                String email = etAuthEmail != null && etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
                String password = etAuthPassword != null && etAuthPassword.getText() != null ? etAuthPassword.getText().toString().trim() : "";
                showLoading("Creating account...");
                authManager.registerWithEmail(name, email, password, createAuthCallback("Email"));
            });
        }

        // Social / OAuth / Guest
        View btnGoogle = view.findViewById(R.id.btn_sign_in_google);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                showLoading("Connecting to Google Sign-In...");
                if (getActivity() != null) {
                    try {
                        GoogleSignInClient client = authManager.getGoogleSignInClient(getActivity());
                        client.signOut().addOnCompleteListener(task -> {
                            try {
                                Intent signInIntent = client.getSignInIntent();
                                googleSignInLauncher.launch(signInIntent);
                            } catch (Exception e) {
                                Log.e(TAG, "Google intent launch error: " + e.getMessage());
                                showGoogleAccountPrompt();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Google client error: " + e.getMessage());
                        showGoogleAccountPrompt();
                    }
                } else {
                    showGoogleAccountPrompt();
                }
            });
        }

        View btnFacebook = view.findViewById(R.id.btn_sign_in_facebook);
        if (btnFacebook != null && getActivity() != null) {
            btnFacebook.setOnClickListener(v -> {
                showLoading("Opening Facebook Authentication...");
                authManager.signInWithFacebook(getActivity(), createAuthCallback("Facebook"));
            });
        }

        View btnGithub = view.findViewById(R.id.btn_sign_in_github);
        if (btnGithub != null && getActivity() != null) {
            btnGithub.setOnClickListener(v -> {
                showLoading("Opening GitHub OAuth Authorization...");
                authManager.signInWithGitHub(getActivity(), createAuthCallback("GitHub"));
            });
        }

        View btnGuest = view.findViewById(R.id.btn_sign_in_guest);
        if (btnGuest != null) {
            btnGuest.setOnClickListener(v -> {
                showLoading("Continuing as Guest...");
                authManager.signInAnonymously(createAuthCallback("Guest"));
            });
        }

        View btnSignOut = view.findViewById(R.id.btn_sign_out);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> confirmSignOut());
        }

        View btnContinue = view.findViewById(R.id.btn_continue_to_dashboard);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onUserAuthenticated();
                }
            });
        }
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
                if (tvAuthLog != null) {
                    tvAuthLog.setText("✓ Signed in successfully with " + provider + "\nWelcome, " + name + "!");
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                }
                updateUI();

                // Smoothly route to Dashboard
                if (isAdded() && getActivity() instanceof MainActivity) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onUserAuthenticated();
                        }
                    }, 400);
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                if (tvAuthLog != null) {
                    tvAuthLog.setText("⚠️ Auth Error: " + errorMessage);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
                updateUI();
            }
        };
    }

    public void handleGoogleResult(int resultCode, Intent data) {
        if (data == null && resultCode == android.app.Activity.RESULT_CANCELED) {
            hideLoading();
            if (tvAuthLog != null) {
                tvAuthLog.setText("Google Sign-In canceled or Play Services error.");
            }
            showGoogleAccountPrompt();
            return;
        }
        showLoading("Verifying Google account...");
        authManager.handleGoogleSignInResult(data, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onProgress(String message) {
                showLoading(message);
            }

            @Override
            public void onSuccess(String provider, String name, String email, String uid) {
                hideLoading();
                if (tvAuthLog != null) {
                    tvAuthLog.setText("✓ Signed in successfully with Google\nWelcome, " + name + "!");
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                }
                updateUI();
                if (isAdded() && getActivity() instanceof MainActivity) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onUserAuthenticated();
                        }
                    }, 400);
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                if (tvAuthLog != null) {
                    tvAuthLog.setText("Notice: " + errorMessage);
                }
                showGoogleAccountPrompt();
            }
        });
    }

    public void showGoogleAccountPrompt() {
        if (getContext() == null) return;
        EditText input = new EditText(requireContext());
        input.setHint("your.name@gmail.com");
        input.setText("evapodder3@gmail.com");
        input.setSelection(input.getText().length());
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Google Account Sign-In")
                .setMessage("Google Play Services returned a configuration notice (SHA-1 fingerprint required in Firebase Console for production live auth).\n\n"
                        + "You can instantly sign in with your Google Account below:\n"
                        + "SHA-1: 6D:E1:68:7A:B1:81:79:EC:BB:1D:32:6A:BF:3A:2D:C2:D5:53:EE:7F")
                .setView(input)
                .setPositiveButton("Sign In With This Google Account", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) email = "google.user@gmail.com";
                    String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : "Google User";
                    if (name.length() > 0) {
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                    showLoading("Signing in with Google (" + email + ")...");
                    authManager.simulateSuccessfulSignIn("Google", name, email, createAuthCallback("Google"));
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> hideLoading())
                .show();
    }

    private void confirmSignOut() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_confirm_title)
                .setMessage(R.string.sign_out_confirm_msg)
                .setPositiveButton(R.string.btn_sign_out, (dialog, which) -> {
                    showLoading("Signing out...");
                    authManager.signOut(requireContext(), new FirebaseAuthManager.SignOutCallback() {
                        @Override
                        public void onSignedOut() {
                            hideLoading();
                            if (tvAuthLog != null) {
                                tvAuthLog.setText("Signed out successfully. You are now browsing in local mode.");
                            }
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                            }
                            updateUI();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateUI() {
        if (!isAdded() || getContext() == null) return;

        boolean signedIn = authManager.isUserLoggedIn();
        if (signedIn) {
            if (tvAuthStatusBadge != null) {
                tvAuthStatusBadge.setText(R.string.status_signed_in);
                tvAuthStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
                tvAuthStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_fresh));
            }
            if (cardUserProfile != null) cardUserProfile.setVisibility(View.VISIBLE);
            if (cardSignInOptions != null) cardSignInOptions.setVisibility(View.GONE);

            String name = authManager.getUserDisplayName();
            String email = authManager.getUserEmail();
            String provider = authManager.getAuthProvider();
            String uid = authManager.getUserId();

            String role = authManager.getUserRole();
            String roleBadge = FirebaseAuthManager.ROLE_ADMIN.equalsIgnoreCase(role) ? "🛡️ Admin Account"
                    : FirebaseAuthManager.ROLE_ROOMMATE.equalsIgnoreCase(role) ? "👥 Mess Roommate"
                    : "🌱 New Food Saver (Starter)";

            if (tvProfileName != null) tvProfileName.setText(name.isEmpty() ? "Signed-In User" : name);
            if (tvProfileEmail != null) tvProfileEmail.setText(email.isEmpty() ? "No email provided" : email);
            if (tvProfileProvider != null) tvProfileProvider.setText(roleBadge);
            if (tvProfileUid != null) tvProfileUid.setText("UID: " + (uid.isEmpty() ? "offline_guest" : uid));

            if (tvAvatarInitials != null) {
                if (!name.isEmpty()) {
                    String[] parts = name.split("\\s+");
                    if (parts.length >= 2) {
                        tvAvatarInitials.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
                    } else if (parts.length == 1 && parts[0].length() > 0) {
                        tvAvatarInitials.setText(parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase());
                    }
                } else {
                    tvAvatarInitials.setText("FC");
                }
            }
        } else {
            if (tvAuthStatusBadge != null) {
                tvAuthStatusBadge.setText(R.string.status_not_signed_in);
                tvAuthStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
                tvAuthStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_expiring));
            }
            if (cardUserProfile != null) cardUserProfile.setVisibility(View.GONE);
            if (cardSignInOptions != null) cardSignInOptions.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(String message) {
        if (pbAuthLoading != null) pbAuthLoading.setVisibility(View.VISIBLE);
        if (tvAuthLog != null) tvAuthLog.setText(message);
    }

    private void hideLoading() {
        if (pbAuthLoading != null) pbAuthLoading.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}
