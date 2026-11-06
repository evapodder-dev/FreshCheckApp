package com.freshcheck.app.ui.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
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
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.ShoppingItem;
import com.freshcheck.app.ui.activity.ReceiptScannerActivity;
import com.freshcheck.app.ui.adapter.ShoppingAdapter;
import com.freshcheck.app.util.DateUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingFragment extends Fragment implements ShoppingAdapter.OnShoppingItemActionListener {
    private static final String TAG = "ShoppingFragment";

    private EditText etName, etQty;
    private Spinner spUnit, spCategory;
    private RecyclerView rvShopping;
    private View layoutEmpty;
    private TextView tvHeader;

    private ShoppingAdapter adapter;
    private ShoppingRepository repository;
    private FoodRepository foodRepository;
    private HouseholdRepository householdRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping, container, false);
        repository = new ShoppingRepository(requireContext());
        foodRepository = new FoodRepository(requireContext());
        householdRepository = new HouseholdRepository(requireContext());

        etName = view.findViewById(R.id.et_shopping_name);
        etQty = view.findViewById(R.id.et_shopping_qty);
        spUnit = view.findViewById(R.id.spinner_shopping_unit);
        spCategory = view.findViewById(R.id.spinner_shopping_category);
        rvShopping = view.findViewById(R.id.rv_shopping);
        layoutEmpty = view.findViewById(R.id.layout_empty_shopping);
        tvHeader = view.findViewById(R.id.tv_shopping_header);

        rvShopping.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShoppingAdapter(requireContext(), this);
        rvShopping.setAdapter(adapter);

        setupSpinners();

        // Add item button
        view.findViewById(R.id.btn_add_shopping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        // Scan receipt button
        View btnReceipt = view.findViewById(R.id.btn_scan_receipt);
        if (btnReceipt != null) {
            btnReceipt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), ReceiptScannerActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Share button - Implicit Intent
        view.findViewById(R.id.btn_share_shopping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareShoppingList();
            }
        });

        // Clear purchased button
        view.findViewById(R.id.btn_clear_purchased).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearPurchasedItems();
            }
        });

        loadShoppingItems();

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.UNITS);
        spUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.CATEGORIES);
        spCategory.setAdapter(catAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShoppingItems();
    }

    private void loadShoppingItems() {
        Log.d(TAG, "Loading shopping items...");
        List<ShoppingItem> items = repository.getAll();
        adapter.setItems(items);

        tvHeader.setText("Grocery Items (" + items.size() + ")");

        if (items.isEmpty()) {
            rvShopping.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvShopping.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void addItem() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Enter grocery item name");
            return;
        }

        double qty = 1.0;
        try {
            qty = Double.parseDouble(etQty.getText().toString().trim());
        } catch (Exception ignored) {}

        String unit = spUnit.getSelectedItem().toString();
        String cat = spCategory.getSelectedItem().toString();

        ShoppingItem item = new ShoppingItem(0, name, qty, unit, cat, false, "");
        repository.insert(item);

        etName.setText("");
        etQty.setText("1.0");
        Toast.makeText(getContext(), name + " added to list", Toast.LENGTH_SHORT).show();
        loadShoppingItems();
    }

    private void shareShoppingList() {
        Log.d(TAG, "Triggering implicit Intent to share shopping list");
        String shareText = repository.getFormattedShareText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "FreshCheck Grocery Shopping List");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Grocery List via"));
    }

    private void clearPurchasedItems() {
        int count = repository.clearPurchased();
        Toast.makeText(getContext(), "Cleared " + count + " purchased items", Toast.LENGTH_SHORT).show();
        loadShoppingItems();
    }

    @Override
    public void onTogglePurchased(ShoppingItem item, boolean isPurchased) {
        repository.togglePurchased(item.getId(), isPurchased);
        loadShoppingItems();
    }

    @Override
    public void onMoveToInventory(final ShoppingItem item) {
        // Show confirmation dialog: Never invent expiration dates! Prompt user for actual expiry & location.
        showTransferToInventoryDialog(item);
    }

    private void showTransferToInventoryDialog(final ShoppingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Transfer to Food Inventory");

        // Simple customized layout programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText("Set actual package expiry date and storage location for " + item.getName() + ":");
        tvDesc.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvDesc);

        // Expiry Date Button
        final TextView tvExpiryBtn = new TextView(requireContext());
        final String[] chosenExpiry = {DateUtils.getFutureDateString(5)};
        tvExpiryBtn.setText("📅 Expiration Date: " + chosenExpiry[0]);
        tvExpiryBtn.setPadding(20, 24, 20, 24);
        tvExpiryBtn.setBackgroundResource(R.drawable.bg_search_bar);
        tvExpiryBtn.setTextColor(getResources().getColor(R.color.primary));
        android.widget.LinearLayout.LayoutParams lpDate = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpDate.topMargin = 16;
        layout.addView(tvExpiryBtn, lpDate);

        tvExpiryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date d = DateUtils.parseDate(chosenExpiry[0]);
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar nc = Calendar.getInstance();
                        nc.set(year, month, dayOfMonth);
                        chosenExpiry[0] = DateUtils.getDateFormat().format(nc.getTime());
                        tvExpiryBtn.setText("📅 Expiration Date: " + chosenExpiry[0]);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Location Spinner
        TextView tvLocLabel = new TextView(requireContext());
        tvLocLabel.setText("Storage Location:");
        tvLocLabel.setPadding(0, 16, 0, 4);
        layout.addView(tvLocLabel);

        final Spinner spLoc = new Spinner(requireContext());
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CategoryConstants.STORAGE_LOCATIONS);
        spLoc.setAdapter(locAdapter);
        spLoc.setBackgroundResource(R.drawable.bg_search_bar);
        layout.addView(spLoc);

        // Purchase Cost
        final EditText etPrice = new EditText(requireContext());
        etPrice.setHint("Purchase Price (৳ Taka)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setText(item.getEstimatedCost() > 0 ? String.format(Locale.US, "%.1f", item.getEstimatedCost()) : "");
        etPrice.setBackgroundResource(R.drawable.bg_search_bar);
        etPrice.setPadding(20, 20, 20, 20);
        android.widget.LinearLayout.LayoutParams lpPrice = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPrice.topMargin = 16;
        layout.addView(etPrice, lpPrice);

        builder.setView(layout);
        builder.setPositiveButton("Transfer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                double cost = 0.0;
                try {
                    cost = Double.parseDouble(etPrice.getText().toString().trim());
                } catch (Exception ignored) {}

                String loc = spLoc.getSelectedItem().toString();
                String activeHouseholdId = householdRepository.getActiveHouseholdId();

                FoodItem foodItem = new FoodItem();
                foodItem.setName(item.getName());
                foodItem.setQuantity(item.getQuantity());
                foodItem.setUnit(item.getUnit());
                foodItem.setCategory(item.getCategory());
                foodItem.setPurchaseDate(DateUtils.getTodayString());
                foodItem.setExpiryDate(chosenExpiry[0]);
                foodItem.setLocation(loc);
                foodItem.setEstimatedCost(cost);
                foodItem.setNotes(item.getNotes());
                if (activeHouseholdId != null && !activeHouseholdId.isEmpty()) {
                    foodItem.setHouseholdId(activeHouseholdId);
                }

                foodRepository.insert(foodItem);
                repository.delete(item.getId());

                Toast.makeText(getContext(), item.getName() + " transferred to inventory! Track freshness in Dashboard.", Toast.LENGTH_SHORT).show();
                loadShoppingItems();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    public void onDelete(ShoppingItem item) {
        repository.delete(item.getId());
        loadShoppingItems();
    }
}
