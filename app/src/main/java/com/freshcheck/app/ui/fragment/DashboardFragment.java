package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.database.HouseholdRepository;
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.ui.activity.AddEditFoodActivity;
import com.freshcheck.app.ui.activity.FoodScannerActivity;
import com.freshcheck.app.ui.activity.HouseholdActivity;
import com.freshcheck.app.ui.activity.MainActivity;
import com.freshcheck.app.ui.activity.MealPlannerActivity;
import com.freshcheck.app.ui.activity.ReceiptScannerActivity;
import com.freshcheck.app.ui.adapter.UseFirstAdapter;
import com.freshcheck.app.ui.custom.ExpiryStatusDonutView;
import com.freshcheck.app.util.DateUtils;
import com.freshcheck.app.firebase.FirebaseAuthManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements UseFirstAdapter.OnUseFirstActionListener {
    private static final String TAG = "DashboardFragment";

    private TextView tvGreeting;
    private TextView tvAvatar;
    private TextView tvScopePill;
    private ExpiryStatusDonutView donutChartView;
    private TextView tvLegendFresh, tvLegendExpiring, tvLegendExpired;
    private TextView tvStatTotal, tvStatExpiring, tvStatToday, tvStatExpired;
    private RecyclerView rvUseFirst;
    private TextView tvEmptyUseFirst;
    private TextView tvRecentlyAddedList;

    private UseFirstAdapter useFirstAdapter;
    private FoodRepository foodRepository;
    private HouseholdRepository householdRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        foodRepository = new FoodRepository(requireContext());
        householdRepository = new HouseholdRepository(requireContext());

        tvGreeting = view.findViewById(R.id.tv_dashboard_greeting);
        tvAvatar = view.findViewById(R.id.tv_dashboard_avatar);
        tvScopePill = view.findViewById(R.id.tv_dashboard_scope_pill);
        donutChartView = view.findViewById(R.id.donut_chart_view);
        tvLegendFresh = view.findViewById(R.id.tv_legend_fresh);
        tvLegendExpiring = view.findViewById(R.id.tv_legend_expiring);
        tvLegendExpired = view.findViewById(R.id.tv_legend_expired);

        tvStatTotal = view.findViewById(R.id.tv_stat_total);
        tvStatExpiring = view.findViewById(R.id.tv_stat_expiring);
        tvStatToday = view.findViewById(R.id.tv_stat_today);
        tvStatExpired = view.findViewById(R.id.tv_stat_expired);

        rvUseFirst = view.findViewById(R.id.rv_use_first);
        tvEmptyUseFirst = view.findViewById(R.id.tv_empty_use_first);
        tvRecentlyAddedList = view.findViewById(R.id.tv_recently_added_list);

        rvUseFirst.setLayoutManager(new LinearLayoutManager(getContext()));
        useFirstAdapter = new UseFirstAdapter(requireContext(), this);
        rvUseFirst.setAdapter(useFirstAdapter);

        // Scope selector click -> Household Management
        tvScopePill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), HouseholdActivity.class);
                startActivity(intent);
            }
        });

        // See all inventory link
        view.findViewById(R.id.tv_see_all_inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_inventory);
                }
            }
        });

        // Quick action: Add Food
        view.findViewById(R.id.btn_action_add_food).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddEditFoodActivity.class);
                startActivity(intent);
            }
        });

        // Quick action: Scanner Chooser (Food Item ML Kit or Receipt OCR)
        View btnScanner = view.findViewById(R.id.btn_action_scanner);
        if (btnScanner != null) {
            btnScanner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] options = {
                            "📷 Food Item Recognition (ML Kit)",
                            "🧾 Grocery Receipt Text OCR"
                    };
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
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        // Quick action: Grocery List
        View btnGrocery = view.findViewById(R.id.btn_action_grocery);
        if (btnGrocery != null) {
            btnGrocery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToTab(R.id.nav_shopping);
                    }
                }
            });
        }

        // Quick action: Meal Planner
        View btnPlanner = view.findViewById(R.id.btn_action_planner);
        if (btnPlanner != null) {
            btnPlanner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), MealPlannerActivity.class);
                    startActivity(intent);
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreetingAndScope();
        loadDashboardData();
    }

    private void updateGreetingAndScope() {
        try {
            FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(requireContext());
            if (authManager.isUserLoggedIn()) {
                String name = authManager.getUserDisplayName();
                tvGreeting.setText("Hello, " + name);
                if (tvAvatar != null && name != null && !name.trim().isEmpty()) {
                    String[] parts = name.trim().split("\\s+");
                    if (parts.length >= 2) {
                        tvAvatar.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
                    } else {
                        tvAvatar.setText(("" + parts[0].charAt(0)).toUpperCase());
                    }
                }
            } else {
                tvGreeting.setText("Welcome to FreshCheck");
                if (tvAvatar != null) tvAvatar.setText("FC");
            }
        } catch (Exception e) {
            tvGreeting.setText("Welcome to FreshCheck");
            if (tvAvatar != null) tvAvatar.setText("FC");
        }

        try {
            String activeScope = householdRepository.getActiveHouseholdName();
            tvScopePill.setText(activeScope != null && !activeScope.isEmpty() ? activeScope : "🏡 Personal");
        } catch (Exception e) {
            tvScopePill.setText("🏡 Personal");
        }
    }

    public void loadDashboardData() {
        try {
            Log.d(TAG, "Loading dashboard metrics and priority items...");
            String activeHouseholdId = householdRepository.getActiveHouseholdId();
            List<FoodItem> activeItems = foodRepository.getAllActive(activeHouseholdId);

            int total = activeItems.size();
            int fresh = 0;
            int expiringSoon = 0;
            int expiresToday = 0;
            int expired = 0;

            List<FoodItem> useFirstItems = new ArrayList<>();

            for (FoodItem item : activeItems) {
                String status = item.getStatus();
                if (CategoryConstants.STATUS_FRESH.equals(status)) {
                    fresh++;
                } else if (CategoryConstants.STATUS_EXPIRING_SOON.equals(status)) {
                    expiringSoon++;
                    useFirstItems.add(item);
                } else if (CategoryConstants.STATUS_EXPIRES_TODAY.equals(status)) {
                    expiresToday++;
                    useFirstItems.add(item);
                } else if (CategoryConstants.STATUS_EXPIRED.equals(status)) {
                    expired++;
                    useFirstItems.add(item);
                }
            }

            // Sort Use First by remaining days ascending safely
            Collections.sort(useFirstItems, new Comparator<FoodItem>() {
                @Override
                public int compare(FoodItem o1, FoodItem o2) {
                    return Long.compare(o1.getDaysRemaining(), o2.getDaysRemaining());
                }
            });

            // Update statistics cards
            tvStatTotal.setText(String.valueOf(total));
            tvStatExpiring.setText(String.valueOf(expiringSoon));
            tvStatToday.setText(String.valueOf(expiresToday));
            tvStatExpired.setText(String.valueOf(expired));

            // Update Custom View Donut Chart
            donutChartView.setData(fresh, expiringSoon + expiresToday, expired);

            // Update Legends
            tvLegendFresh.setText("Fresh: " + fresh);
            tvLegendExpiring.setText("Expiring: " + (expiringSoon + expiresToday));
            tvLegendExpired.setText("Expired: " + expired);

            // Update Use First Priority list
            useFirstAdapter.setItems(useFirstItems);
            if (useFirstItems.isEmpty()) {
                rvUseFirst.setVisibility(View.GONE);
                tvEmptyUseFirst.setVisibility(View.VISIBLE);
            } else {
                rvUseFirst.setVisibility(View.VISIBLE);
                tvEmptyUseFirst.setVisibility(View.GONE);
            }

            // Update Recently Added Section
            List<FoodItem> recentlyAdded = foodRepository.getRecentlyAdded(3);
            if (recentlyAdded.isEmpty()) {
                tvRecentlyAddedList.setText("No food items added yet.\nTap '+ Add Food' or '📷 Scan' above to record your first item.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < recentlyAdded.size(); i++) {
                    FoodItem item = recentlyAdded.get(i);
                    sb.append("• ").append(item.getName())
                            .append(" (").append(formatQuantity(item.getQuantity())).append(" ").append(item.getUnit()).append(")")
                            .append(" — ").append(item.getStatus());
                    if (item.getDaysRemaining() >= 0) {
                        sb.append(" (in ").append(item.getDaysRemaining()).append(" days)");
                    } else {
                        sb.append(" (expired ").append(Math.abs(item.getDaysRemaining())).append(" days ago)");
                    }
                    if (i < recentlyAdded.size() - 1) {
                        sb.append("\n");
                    }
                }
                tvRecentlyAddedList.setText(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading dashboard data: " + e.getMessage(), e);
        }
    }

    private String formatQuantity(double qty) {
        if (qty == (long) qty) {
            return String.format(Locale.US, "%d", (long) qty);
        } else {
            return String.format(Locale.US, "%.1f", qty);
        }
    }

    @Override
    public void onQuickConsumed(final FoodItem item) {
        // Offer partial consumption dialog
        final EditText etQty = new EditText(requireContext());
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etQty.setText(formatQuantity(item.getQuantity()));
        etQty.setHint("Quantity consumed");

        new AlertDialog.Builder(requireContext())
                .setTitle("Mark as Consumed")
                .setMessage("Enter the quantity of '" + item.getName() + "' you consumed (" + item.getUnit() + "):")
                .setView(etQty)
                .setPositiveButton("Consume", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        double consumedQty = item.getQuantity();
                        try {
                            consumedQty = Double.parseDouble(etQty.getText().toString().trim());
                        } catch (Exception ignored) {}

                        if (consumedQty <= 0) {
                            Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        foodRepository.consumePartial(item.getId(), consumedQty);
                        Toast.makeText(getContext(), "Consumed " + formatQuantity(consumedQty) + " " + item.getUnit() + " of " + item.getName(), Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    }
                })
                .setNeutralButton("Consume All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        foodRepository.markConsumed(item.getId());
                        Toast.makeText(getContext(), item.getName() + " finished! Saved from waste.", Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onQuickWaste(final FoodItem item) {
        showLogWasteDialog(item);
    }

    private void showLogWasteDialog(final FoodItem item) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_log_waste, null);

        final EditText etName = dialogView.findViewById(R.id.et_dialog_waste_food_name);
        final EditText etQty = dialogView.findViewById(R.id.et_dialog_waste_qty);
        final Spinner spUnit = dialogView.findViewById(R.id.spinner_dialog_waste_unit);
        final Spinner spReason = dialogView.findViewById(R.id.spinner_dialog_waste_reason);
        final EditText etCost = dialogView.findViewById(R.id.et_dialog_waste_cost);
        final EditText etNotes = dialogView.findViewById(R.id.et_dialog_waste_notes);

        etName.setText(item.getName());
        etQty.setText(String.valueOf(item.getQuantity()));
        etCost.setText(String.format(Locale.US, "%.2f", item.getEstimatedCost()));

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.UNITS);
        spUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.WASTE_REASONS);
        spReason.setAdapter(reasonAdapter);

        // Set default selection according to status
        if (CategoryConstants.STATUS_EXPIRED.equals(item.getStatus())) {
            spReason.setSelection(1); // Past Expiry Date
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Log Food Waste")
                .setView(dialogView)
                .setPositiveButton("Record Waste", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        double cost = 0.0;
                        try {
                            cost = Double.parseDouble(etCost.getText().toString().trim());
                        } catch (Exception ignored) {}

                        double qty = item.getQuantity();
                        try {
                            qty = Double.parseDouble(etQty.getText().toString().trim());
                        } catch (Exception ignored) {}

                        String reason = spReason.getSelectedItem().toString();
                        String unit = spUnit.getSelectedItem().toString();
                        String notes = etNotes.getText().toString().trim();

                        // Mark food item wasted and write record to waste table
                        foodRepository.markWasted(item.getId(), reason, notes);

                        // If user edited cost or quantity in dialog, ensure waste record has updated values
                        WasteRepository wasteRepo = new WasteRepository(requireContext());
                        WasteRecord customRecord = new WasteRecord(0, etName.getText().toString(),
                                qty, unit, DateUtils.getTodayString(), reason, cost, notes);
                        Toast.makeText(getContext(), "Recorded waste for " + item.getName(), Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
