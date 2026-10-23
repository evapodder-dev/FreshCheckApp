package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.ui.activity.AddEditFoodActivity;
import com.freshcheck.app.ui.adapter.FoodAdapter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class InventoryFragment extends Fragment implements FoodAdapter.OnFoodItemActionListener {
    private static final String TAG = "InventoryFragment";

    private EditText etSearch;
    private Spinner spCategory;
    private ChipGroup chipGroupStatus;
    private RecyclerView rvInventory;
    private TextView tvCount;
    private View layoutEmpty;
    private FloatingActionButton fabAdd;

    private FoodAdapter adapter;
    private FoodRepository repository;

    private String currentSearch = "";
    private String currentCategory = "All";
    private String currentStatus = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        repository = new FoodRepository(requireContext());

        etSearch = view.findViewById(R.id.et_search_food);
        spCategory = view.findViewById(R.id.spinner_category_filter);
        chipGroupStatus = view.findViewById(R.id.chip_group_status);
        rvInventory = view.findViewById(R.id.rv_inventory);
        tvCount = view.findViewById(R.id.tv_inventory_count);
        layoutEmpty = view.findViewById(R.id.layout_empty_inventory);
        fabAdd = view.findViewById(R.id.fab_add_food);

        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FoodAdapter(requireContext(), this);
        rvInventory.setAdapter(adapter);

        setupCategorySpinner();
        setupStatusChips();
        setupSearch();

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddEditFoodActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.addAll(Arrays.asList(CategoryConstants.CATEGORIES));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(spinnerAdapter);

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStatusChips() {
        chipGroupStatus.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                if (checkedId == R.id.chip_status_fresh) {
                    currentStatus = "Fresh";
                } else if (checkedId == R.id.chip_status_expiring) {
                    currentStatus = "Expiring Soon";
                } else if (checkedId == R.id.chip_status_expired) {
                    currentStatus = "Expired";
                } else {
                    currentStatus = "All";
                }
                applyFilters();
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        applyFilters();
    }

    private void applyFilters() {
        Log.d(TAG, "Filtering inventory: Query='" + currentSearch + "', Category=" + currentCategory + ", Status=" + currentStatus);
        List<FoodItem> filtered = repository.searchAndFilter(currentSearch, currentCategory, currentStatus);
        adapter.setItems(filtered);

        tvCount.setText(String.format(Locale.US, "Showing %d item%s", filtered.size(), filtered.size() == 1 ? "" : "s"));

        if (filtered.isEmpty()) {
            rvInventory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvInventory.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(FoodItem item) {
        Intent intent = new Intent(getContext(), AddEditFoodActivity.class);
        intent.putExtra(AddEditFoodActivity.EXTRA_FOOD_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(final FoodItem item) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repository.delete(item.getId());
                        Toast.makeText(getContext(), item.getName() + " removed from inventory", Toast.LENGTH_SHORT).show();
                        applyFilters();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onMarkConsumed(FoodItem item) {
        repository.markConsumed(item.getId());
        Toast.makeText(getContext(), item.getName() + " marked as consumed! 🎉", Toast.LENGTH_SHORT).show();
        applyFilters();
    }

    @Override
    public void onRecordWaste(FoodItem item) {
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

        if (CategoryConstants.STATUS_EXPIRED.equals(item.getStatus())) {
            spReason.setSelection(1);
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Log Food Waste")
                .setView(dialogView)
                .setPositiveButton("Record Waste", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String reason = spReason.getSelectedItem().toString();
                        String notes = etNotes.getText().toString().trim();
                        repository.markWasted(item.getId(), reason, notes);
                        Toast.makeText(getContext(), "Recorded waste for " + item.getName(), Toast.LENGTH_SHORT).show();
                        applyFilters();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
