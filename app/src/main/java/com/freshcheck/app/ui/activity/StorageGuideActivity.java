package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.util.StorageGuideData;

import java.util.ArrayList;
import java.util.List;

public class StorageGuideActivity extends AppCompatActivity {
    private static final String TAG = "StorageGuideActivity";

    private EditText etSearch;
    private RecyclerView rvGuide;
    private StorageGuideAdapter adapter;
    private List<StorageGuideData.StorageItem> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_guide);

        Toolbar toolbar = findViewById(R.id.toolbar_storage_guide);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        etSearch = findViewById(R.id.et_search_storage);
        rvGuide = findViewById(R.id.rv_storage_guide);
        rvGuide.setLayoutManager(new LinearLayoutManager(this));

        allItems = StorageGuideData.getAllGuideItems();
        adapter = new StorageGuideAdapter(allItems);
        rvGuide.setAdapter(adapter);

        findViewById(R.id.card_fridge_zones).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(StorageGuideActivity.this)
                        .setTitle("Refrigerator Temperature Guide")
                        .setMessage(StorageGuideData.getFridgeZonesGuide())
                        .setPositiveButton("Got it", null)
                        .show();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            adapter.updateItems(allItems);
            return;
        }

        List<StorageGuideData.StorageItem> filtered = new ArrayList<>();
        for (StorageGuideData.StorageItem item : allItems) {
            if (item.name.toLowerCase().contains(q) || item.category.toLowerCase().contains(q) || item.tips.toLowerCase().contains(q)) {
                filtered.add(item);
            }
        }
        adapter.updateItems(filtered);
    }

    private static class StorageGuideAdapter extends RecyclerView.Adapter<StorageGuideAdapter.ViewHolder> {
        private final List<StorageGuideData.StorageItem> items = new ArrayList<>();

        public StorageGuideAdapter(List<StorageGuideData.StorageItem> initialItems) {
            this.items.addAll(initialItems);
        }

        public void updateItems(List<StorageGuideData.StorageItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_storage_guide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StorageGuideData.StorageItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvCategory.setText(item.category);
            holder.tvLocation.setText("📍 " + item.bestLocation + " • Shelf Life: " + item.shelfLife);
            holder.tvTips.setText(item.tips);
            holder.tvFreezing.setText("❄️ Freezing: " + item.freezingTips);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvLocation, tvTips, tvFreezing;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_storage_food_name);
                tvCategory = itemView.findViewById(R.id.tv_storage_category_tag);
                tvLocation = itemView.findViewById(R.id.tv_storage_location);
                tvTips = itemView.findViewById(R.id.tv_storage_tips);
                tvFreezing = itemView.findViewById(R.id.tv_freezing_tips);
            }
        }
    }
}