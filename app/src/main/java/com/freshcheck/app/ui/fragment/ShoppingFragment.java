package com.freshcheck.app.ui.fragment;

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
import com.freshcheck.app.database.ShoppingRepository;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.ShoppingItem;
import com.freshcheck.app.ui.adapter.ShoppingAdapter;

import java.util.List;

public class ShoppingFragment extends Fragment implements ShoppingAdapter.OnShoppingItemActionListener {
    private static final String TAG = "ShoppingFragment";

    private EditText etName, etQty;
    private Spinner spUnit, spCategory;
    private RecyclerView rvShopping;
    private View layoutEmpty;
    private TextView tvHeader;

    private ShoppingAdapter adapter;
    private ShoppingRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping, container, false);
        repository = new ShoppingRepository(requireContext());

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
    public void onMoveToInventory(ShoppingItem item) {
        boolean success = repository.moveToInventory(item.getId());
        if (success) {
            Toast.makeText(getContext(), item.getName() + " moved to Food Inventory!", Toast.LENGTH_SHORT).show();
            loadShoppingItems();
        }
    }

    @Override
    public void onDelete(ShoppingItem item) {
        repository.delete(item.getId());
        loadShoppingItems();
    }
}
