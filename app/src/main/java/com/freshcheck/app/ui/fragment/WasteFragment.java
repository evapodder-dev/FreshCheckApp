package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.freshcheck.app.database.WasteRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.WasteRecord;
import com.freshcheck.app.ui.adapter.WasteAdapter;
import com.freshcheck.app.ui.custom.WasteBreakdownChartView;
import com.freshcheck.app.util.DateUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WasteFragment extends Fragment implements WasteAdapter.OnWasteItemActionListener {
    private static final String TAG = "WasteFragment";

    private TextView tvTotalItems;
    private TextView tvTotalLoss;
    private WasteBreakdownChartView chartView;
    private RecyclerView rvWaste;
    private View tvEmpty;

    private WasteAdapter adapter;
    private WasteRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waste, container, false);
        repository = new WasteRepository(requireContext());

        tvTotalItems = view.findViewById(R.id.tv_waste_total_items);
        tvTotalLoss = view.findViewById(R.id.tv_waste_total_loss);
        chartView = view.findViewById(R.id.waste_chart_view);
        rvWaste = view.findViewById(R.id.rv_waste_history);
        tvEmpty = view.findViewById(R.id.tv_empty_waste);

        rvWaste.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WasteAdapter(requireContext(), this);
        rvWaste.setAdapter(adapter);

        view.findViewById(R.id.btn_log_waste_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogWasteDialog();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWasteData();
    }

    public void loadWasteData() {
        Log.d(TAG, "Loading waste statistics and history...");
        int totalItems = repository.getTotalItemsWasted();
        double totalLoss = repository.getTotalFinancialLoss();

        tvTotalItems.setText(String.valueOf(totalItems));
        tvTotalLoss.setText(String.format(Locale.US, "৳%.2f", totalLoss));

        // Update custom chart view
        Map<String, Float> costByReason = repository.getWasteCostByReason();
        chartView.setData(costByReason);

        // Update waste history list
        List<WasteRecord> history = repository.getAll();
        adapter.setItems(history);

        if (history.isEmpty()) {
            rvWaste.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvWaste.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showLogWasteDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_log_waste, null);

        final EditText etName = dialogView.findViewById(R.id.et_dialog_waste_food_name);
        final EditText etQty = dialogView.findViewById(R.id.et_dialog_waste_qty);
        final Spinner spUnit = dialogView.findViewById(R.id.spinner_dialog_waste_unit);
        final Spinner spReason = dialogView.findViewById(R.id.spinner_dialog_waste_reason);
        final EditText etCost = dialogView.findViewById(R.id.et_dialog_waste_cost);
        final EditText etNotes = dialogView.findViewById(R.id.et_dialog_waste_notes);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.UNITS);
        spUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.WASTE_REASONS);
        spReason.setAdapter(reasonAdapter);

        new AlertDialog.Builder(getContext())
                .setTitle("Log Food Waste")
                .setView(dialogView)
                .setPositiveButton("Record Waste", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etName.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(getContext(), "Food name is required", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double qty = 1.0;
                        try {
                            qty = Double.parseDouble(etQty.getText().toString().trim());
                        } catch (Exception ignored) {}

                        double cost = 0.0;
                        try {
                            cost = Double.parseDouble(etCost.getText().toString().trim());
                        } catch (Exception ignored) {}

                        String unit = spUnit.getSelectedItem().toString();
                        String reason = spReason.getSelectedItem().toString();
                        String notes = etNotes.getText().toString().trim();

                        WasteRecord record = new WasteRecord(0, name, qty, unit,
                                DateUtils.getTodayString(), reason, cost, notes);
                        repository.insert(record);

                        Toast.makeText(getContext(), "Waste record logged", Toast.LENGTH_SHORT).show();
                        loadWasteData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDelete(final WasteRecord item) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to remove this waste record?")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repository.delete(item.getId());
                        Toast.makeText(getContext(), "Record deleted", Toast.LENGTH_SHORT).show();
                        loadWasteData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
