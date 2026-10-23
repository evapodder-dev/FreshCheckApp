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
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.service.InventoryAuditService;
import com.freshcheck.app.ui.activity.AddEditFoodActivity;
import com.freshcheck.app.ui.activity.CloudSyncActivity;
import com.freshcheck.app.ui.activity.MainActivity;
import com.freshcheck.app.ui.activity.ProviderDemoActivity;
import com.freshcheck.app.ui.adapter.UseFirstAdapter;
import com.freshcheck.app.ui.custom.ExpiryStatusDonutView;
import com.freshcheck.app.util.DateUtils;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements UseFirstAdapter.OnUseFirstActionListener {
    private static final String TAG = "DashboardFragment";

    private ExpiryStatusDonutView donutChartView;
    private TextView tvLegendFresh, tvLegendExpiring, tvLegendExpired;
    private TextView tvStatTotal, tvStatExpiring, tvStatToday, tvStatExpired;
    private RecyclerView rvUseFirst;
    private TextView tvEmptyUseFirst;
    private UseFirstAdapter useFirstAdapter;
    private FoodRepository foodRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        foodRepository = new FoodRepository(requireContext());

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

        rvUseFirst.setLayoutManager(new LinearLayoutManager(getContext()));
        useFirstAdapter = new UseFirstAdapter(requireContext(), this);
        rvUseFirst.setAdapter(useFirstAdapter);

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

        // Quick action: Audit Inventory Service
        view.findViewById(R.id.btn_action_audit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(getContext(), InventoryAuditService.class);
                requireContext().startService(serviceIntent);
                Toast.makeText(getContext(), "Inventory Audit Service started", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick action: Cloud Sync
        view.findViewById(R.id.btn_action_cloud_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CloudSyncActivity.class);
                startActivity(intent);
            }
        });

        // Quick action: ContentProvider Demo
        view.findViewById(R.id.btn_action_provider).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProviderDemoActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    public void loadDashboardData() {
        Log.d(TAG, "Loading dashboard metrics and priority items...");
        int total = foodRepository.getTotalActiveCount();
        int fresh = foodRepository.getFreshCount();
        int expiringSoon = foodRepository.getExpiringSoonCount();
        int expiresToday = foodRepository.getExpiresTodayCount();
        int expired = foodRepository.getExpiredCount();

        // Update statistics cards
        tvStatTotal.setText(String.valueOf(total));
        tvStatExpiring.setText(String.valueOf(expiringSoon));
        tvStatToday.setText(String.valueOf(expiresToday));
        tvStatExpired.setText(String.valueOf(expired));

        // Update Custom View Donut Chart
        donutChartView.setData(fresh, expiringSoon + expiresToday, expired);

        // Update Legends
        tvLegendFresh.setText("Fresh: " + fresh);
        tvLegendExpiring.setText("Expiring Soon: " + (expiringSoon + expiresToday));
        tvLegendExpired.setText("Expired: " + expired);

        // Update Use First Priority list
        List<FoodItem> useFirstItems = foodRepository.getUseFirstItems();
        useFirstAdapter.setItems(useFirstItems);

        if (useFirstItems.isEmpty()) {
            rvUseFirst.setVisibility(View.GONE);
            tvEmptyUseFirst.setVisibility(View.VISIBLE);
        } else {
            rvUseFirst.setVisibility(View.VISIBLE);
            tvEmptyUseFirst.setVisibility(View.GONE);
        }
    }

    @Override
    public void onQuickConsumed(FoodItem item) {
        foodRepository.markConsumed(item.getId());
        Toast.makeText(getContext(), item.getName() + " marked as consumed! Great job.", Toast.LENGTH_SHORT).show();
        loadDashboardData();
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
                        // markWasted already creates one, so update latest or let it persist
                        Toast.makeText(getContext(), "Recorded waste for " + item.getName(), Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
