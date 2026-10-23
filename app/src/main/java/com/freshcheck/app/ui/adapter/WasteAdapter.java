package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.WasteRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WasteAdapter extends RecyclerView.Adapter<WasteAdapter.WasteViewHolder> {

    public interface OnWasteItemActionListener {
        void onDelete(WasteRecord item);
    }

    private final Context context;
    private final List<WasteRecord> items = new ArrayList<>();
    private final OnWasteItemActionListener listener;

    public WasteAdapter(Context context, OnWasteItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<WasteRecord> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WasteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_waste, parent, false);
        return new WasteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WasteViewHolder holder, int position) {
        final WasteRecord item = items.get(position);

        holder.tvFoodName.setText(item.getFoodName());
        holder.tvCost.setText(String.format(Locale.US, "-$%.2f", item.getEstimatedCost()));

        String details = String.format(Locale.US, "%.1f %s • Discarded on %s",
                item.getQuantity(), item.getUnit(), item.getWasteDate());
        holder.tvDetails.setText(details);

        holder.tvReasonBadge.setText(item.getReason());

        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            holder.tvNotes.setText(item.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class WasteViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFoodName;
        final TextView tvCost;
        final TextView tvDetails;
        final TextView tvReasonBadge;
        final TextView tvNotes;
        final ImageButton btnDelete;

        public WasteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tv_waste_food_name);
            tvCost = itemView.findViewById(R.id.tv_waste_cost);
            tvDetails = itemView.findViewById(R.id.tv_waste_details);
            tvReasonBadge = itemView.findViewById(R.id.tv_waste_reason_badge);
            tvNotes = itemView.findViewById(R.id.tv_waste_notes);
            btnDelete = itemView.findViewById(R.id.btn_delete_waste);
        }
    }
}
