package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.HouseholdRepository;
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.ui.activity.AnalyticsActivity;
import com.freshcheck.app.ui.activity.AuthActivity;
import com.freshcheck.app.ui.activity.MainActivity;
import com.freshcheck.app.ui.activity.BudgetTrackerActivity;
import com.freshcheck.app.ui.activity.CloudSyncActivity;
import com.freshcheck.app.ui.activity.FoodScannerActivity;
import com.freshcheck.app.ui.activity.HouseholdActivity;
import com.freshcheck.app.ui.activity.MealPlannerActivity;
import com.freshcheck.app.ui.activity.ReceiptScannerActivity;
import com.freshcheck.app.ui.activity.StorageGuideActivity;
import com.freshcheck.app.ui.activity.WasteChallengeActivity;
import com.freshcheck.app.util.BackupRestoreManager;

import java.io.File;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private FirebaseAuthManager authManager;
    private HouseholdRepository householdRepo;
    private FoodRepository foodRepo;
    private ShoppingRepository shoppingRepo;
    private WasteRepository wasteRepo;

    private TextView tvAvatar;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvProviderBadge;
    private TextView tvScope;
    private View btnSignIn;
    private View btnSignOut;
    private View btnSwitchRole;
    private View cardProfileHeader;
    private View cardRoleBanner;
    private TextView tvRoleBannerIcon;
    private TextView tvRoleBannerTitle;
    private TextView tvRoleBannerDesc;
    private TextView tvRoleBannerDetails;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        authManager = FirebaseAuthManager.getInstance(requireContext());
        householdRepo = new HouseholdRepository(requireContext());
        foodRepo = new FoodRepository(requireContext());
        shoppingRepo = new ShoppingRepository(requireContext());
        wasteRepo = new WasteRepository(requireContext());

        tvAvatar = view.findViewById(R.id.tv_profile_avatar);
        tvUserName = view.findViewById(R.id.tv_profile_user_name);
        tvUserEmail = view.findViewById(R.id.tv_profile_user_email);
        tvProviderBadge = view.findViewById(R.id.tv_profile_provider_badge);
        tvScope = view.findViewById(R.id.tv_profile_scope);
        cardProfileHeader = view.findViewById(R.id.card_profile_header);
        btnSignIn = view.findViewById(R.id.btn_profile_sign_in);
        btnSignOut = view.findViewById(R.id.btn_profile_sign_out);
        btnSwitchRole = view.findViewById(R.id.btn_profile_switch_role);
        cardRoleBanner = view.findViewById(R.id.card_role_banner);
        tvRoleBannerIcon = view.findViewById(R.id.tv_role_banner_icon);
        tvRoleBannerTitle = view.findViewById(R.id.tv_role_banner_title);
        tvRoleBannerDesc = view.findViewById(R.id.tv_role_banner_desc);
        tvRoleBannerDetails = view.findViewById(R.id.tv_role_banner_details);

        // Header click -> Switch Profile Dialog
        if (cardProfileHeader != null) {
            cardProfileHeader.setOnClickListener(v -> showSwitchProfileDialog());
        }

        // Switch Profile Button
        if (btnSwitchRole != null) {
            btnSwitchRole.setOnClickListener(v -> showSwitchProfileDialog());
        }

        // Sign In button click -> Open Auth
        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showAuthScreen();
                    } else {
                        startActivity(new Intent(getContext(), AuthActivity.class));
                    }
                }
            });
        }

        // Click listeners for smart modules
        view.findViewById(R.id.btn_manage_household).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), HouseholdActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_household).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), HouseholdActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_meal_planner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), MealPlannerActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_budget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), BudgetTrackerActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_storage_guide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), StorageGuideActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_waste_challenge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), WasteChallengeActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_analytics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AnalyticsActivity.class));
            }
        });

        view.findViewById(R.id.card_nav_scanner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScannerChooserDialog();
            }
        });

        view.findViewById(R.id.card_nav_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportDialog();
            }
        });

        view.findViewById(R.id.card_nav_cloud_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), CloudSyncActivity.class));
            }
        });

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmSignOut();
                }
            });
        }

        updateProfileUI();
        return view;
    }

    private void updateProfileUI() {
        if (!isAdded() || getContext() == null) return;

        boolean isLoggedIn = authManager.isUserLoggedIn();
        if (isLoggedIn) {
            String name = authManager.getUserDisplayName();
            String email = authManager.getUserEmail();
            String role = authManager.getUserRole();

            tvUserName.setText(name);
            tvUserEmail.setText(email);
            String provider = authManager.getUserProvider();

            if (FirebaseAuthManager.ROLE_ADMIN.equalsIgnoreCase(role)) {
                tvProviderBadge.setText("👑 Household Admin & Pantry Owner");
                tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
                tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_fresh));
                tvAvatar.setBackgroundResource(R.drawable.bg_avatar_circle);

                if (cardRoleBanner != null) {
                    cardRoleBanner.setVisibility(View.VISIBLE);
                    if (tvRoleBannerIcon != null) tvRoleBannerIcon.setText("👑");
                    if (tvRoleBannerTitle != null) tvRoleBannerTitle.setText("Household Admin Control Center");
                    if (tvRoleBannerDesc != null) tvRoleBannerDesc.setText("Full administrative privileges for Eva's Pantry & Mess (Invite: MESS-PM27).");
                    if (tvRoleBannerDetails != null) tvRoleBannerDetails.setText("• Full Pantry Control: Add, edit, or delete any household item\n• Invite Code Management: Share code (MESS-PM27) with roommates\n• Cloud Backup & Analytics: Realtime sync and full JSON/CSV export\n• Food Saver Score: 95/100 (Expert Food Saver)");
                }
            } else if (FirebaseAuthManager.ROLE_ROOMMATE.equalsIgnoreCase(role)) {
                tvProviderBadge.setText("👥 Shared Mess Roommate (Room 304)");
                tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
                tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_expiring));

                if (cardRoleBanner != null) {
                    cardRoleBanner.setVisibility(View.VISIBLE);
                    if (tvRoleBannerIcon != null) tvRoleBannerIcon.setText("🤝");
                    if (tvRoleBannerTitle != null) tvRoleBannerTitle.setText("Roommate Mess Hub (Room 304)");
                    if (tvRoleBannerDesc != null) tvRoleBannerDesc.setText("Active member of Room 304 Mess (Admin: Eva Podder).");
                    if (tvRoleBannerDetails != null) tvRoleBannerDetails.setText("• Shared Inventory: Add groceries & track shared expiry dates\n• Consume & Cook: Mark shared groceries as consumed\n• Meal Planner: Collaborate on weekly hostel/mess meal schedules\n• Food Saver Score: 82/100 (Eco Roommate)");
                }
            } else if (FirebaseAuthManager.PROVIDER_GOOGLE.equalsIgnoreCase(provider)) {
                tvProviderBadge.setText("🌐 Google Cloud Identity");
                tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
                tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));

                if (cardRoleBanner != null) {
                    cardRoleBanner.setVisibility(View.VISIBLE);
                    if (tvRoleBannerIcon != null) tvRoleBannerIcon.setText("🌐");
                    if (tvRoleBannerTitle != null) tvRoleBannerTitle.setText("Google Cloud Synchronized Identity");
                    if (tvRoleBannerDesc != null) tvRoleBannerDesc.setText("Linked with Google Account (" + email + ").");
                    if (tvRoleBannerDetails != null) tvRoleBannerDetails.setText("• Cloud Firestore & Realtime Database sync active\n• Multi-Device food inventory backup & cross-device sync\n• Food Saver Score: 88/100 (Cloud Pioneer)");
                }
            } else {
                tvProviderBadge.setText("🌱 Personal Food Saver (Starter)");
                tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
                tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent));

                if (cardRoleBanner != null) {
                    cardRoleBanner.setVisibility(View.VISIBLE);
                    if (tvRoleBannerIcon != null) tvRoleBannerIcon.setText("🌱");
                    if (tvRoleBannerTitle != null) tvRoleBannerTitle.setText("New User Quick Start Guide");
                    if (tvRoleBannerDesc != null) tvRoleBannerDesc.setText("Welcome to FreshCheck! Start building your smart pantry.");
                    if (tvRoleBannerDetails != null) tvRoleBannerDetails.setText("1. Tap '+ Add' on Dashboard to log your first food item\n2. Use AI Camera Scanner to recognize fresh produce\n3. Join a roommate mess with an invite code or create your own\n• Food Saver Score: 50/100 (Starter)");
                }
            }

            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                if (parts.length >= 2) {
                    tvAvatar.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
                } else {
                    tvAvatar.setText(("" + parts[0].charAt(0)).toUpperCase());
                }
            } else {
                tvAvatar.setText("FC");
            }

            if (btnSignIn != null) btnSignIn.setVisibility(View.GONE);
            if (btnSignOut != null) btnSignOut.setVisibility(View.VISIBLE);
            if (btnSwitchRole != null) btnSwitchRole.setVisibility(View.VISIBLE);
        } else {
            tvUserName.setText("Guest / Local User");
            tvUserEmail.setText("Tap to sign in or create an account");
            tvProviderBadge.setText("Offline Mode");
            tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_expiring));
            tvAvatar.setText("?");

            if (cardRoleBanner != null) cardRoleBanner.setVisibility(View.GONE);
            if (btnSignIn != null) btnSignIn.setVisibility(View.VISIBLE);
            if (btnSignOut != null) btnSignOut.setVisibility(View.GONE);
            if (btnSwitchRole != null) btnSwitchRole.setVisibility(View.GONE);
        }

        String scopeName = householdRepo.getActiveHouseholdName();
        tvScope.setText("Active Scope: " + scopeName);
    }

    private void showScannerChooserDialog() {
        String[] options = {"📷 Food Item Image Recognition (ML Kit)", "🧾 Grocery Receipt Text OCR"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Scanner Mode")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startActivity(new Intent(getContext(), FoodScannerActivity.class));
                        } else {
                            startActivity(new Intent(getContext(), ReceiptScannerActivity.class));
                        }
                    }
                })
                .show();
    }

    private void showExportDialog() {
        String[] options = {
                "📄 Export Food Inventory (CSV)",
                "📊 Export Waste Report (CSV)",
                "💾 Complete Data Backup (JSON)"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Data Export & Backup")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            File file = BackupRestoreManager.exportInventoryToCsv(requireContext(), foodRepo);
                            if (file != null) {
                                BackupRestoreManager.shareFile(requireContext(), file, "text/csv", "Share Food Inventory CSV");
                            } else {
                                Toast.makeText(getContext(), "Export failed", Toast.LENGTH_SHORT).show();
                            }
                        } else if (which == 1) {
                            File file = BackupRestoreManager.exportWasteReportToCsv(requireContext(), wasteRepo);
                            if (file != null) {
                                BackupRestoreManager.shareFile(requireContext(), file, "text/csv", "Share Waste Report CSV");
                            } else {
                                Toast.makeText(getContext(), "Export failed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            File file = BackupRestoreManager.createFullJsonBackup(requireContext(), foodRepo, shoppingRepo, wasteRepo);
                            if (file != null) {
                                BackupRestoreManager.shareFile(requireContext(), file, "application/json", "Share FreshCheck JSON Backup");
                            } else {
                                Toast.makeText(getContext(), "Backup failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .show();
    }

    private void showSwitchProfileDialog() {
        if (getContext() == null) return;
        String[] options = {
                "👑 Eva Podder (Admin) — Personal & Admin Scope",
                "👥 Tanvir Ahmed (Roommate) — Room 304 Mess Scope",
                "🌱 New User (Fresh Starter) — Starter Pantry Scope",
                "🌐 Google / Other Account (Open Sign-In)"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("Switch Account Profile")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        authManager.signInAsDemoUser("Eva Podder", "evapodder@freshcheck.app", FirebaseAuthManager.ROLE_ADMIN, createSwitchCallback("Eva Podder (Admin)"));
                    } else if (which == 1) {
                        authManager.signInAsDemoUser("Tanvir Ahmed", "tanvir@freshcheck.app", FirebaseAuthManager.ROLE_ROOMMATE, createSwitchCallback("Tanvir Ahmed (Roommate)"));
                    } else if (which == 2) {
                        authManager.signInAsDemoUser("New User", "newuser@freshcheck.app", FirebaseAuthManager.ROLE_NEW_USER, createSwitchCallback("New User (Fresh Starter)"));
                    } else {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showAuthScreen();
                        } else {
                            startActivity(new Intent(getContext(), AuthActivity.class));
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private FirebaseAuthManager.AuthCallback createSwitchCallback(final String profileLabel) {
        return new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onProgress(String message) {}

            @Override
            public void onSuccess(String provider, String name, String email, String uid) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Switched profile to " + profileLabel, Toast.LENGTH_SHORT).show();
                }
                updateProfileUI();
            }

            @Override
            public void onError(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out from your FreshCheck account?")
                .setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        authManager.signOut(requireContext(), new FirebaseAuthManager.SignOutCallback() {
                            @Override
                            public void onSignedOut() {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                                    updateProfileUI();
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).showAuthScreen();
                                    } else {
                                        Intent intent = new Intent(getContext(), AuthActivity.class);
                                        startActivity(intent);
                                    }
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileUI();
    }
}