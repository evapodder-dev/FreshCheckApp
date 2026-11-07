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
    private View cardProfileHeader;

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

        // Header click -> Open Auth Details
        if (cardProfileHeader != null) {
            cardProfileHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showAuthScreen();
                    } else {
                        Intent intent = new Intent(getContext(), AuthActivity.class);
                        intent.putExtra("force_auth_view", true);
                        startActivity(intent);
                    }
                }
            });
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
            String provider = authManager.getAuthProvider();

            tvUserName.setText(name);
            tvUserEmail.setText(email);
            String badgeText = (provider != null && provider.toLowerCase().contains("account")) ? provider : provider + " Account";
            tvProviderBadge.setText(badgeText);
            tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
            tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_fresh));

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
        } else {
            tvUserName.setText("Guest / Local User");
            tvUserEmail.setText("Tap to sign in or create an account");
            tvProviderBadge.setText("Offline Mode");
            tvProviderBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            tvProviderBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_expiring));
            tvAvatar.setText("?");

            if (btnSignIn != null) btnSignIn.setVisibility(View.VISIBLE);
            if (btnSignOut != null) btnSignOut.setVisibility(View.GONE);
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