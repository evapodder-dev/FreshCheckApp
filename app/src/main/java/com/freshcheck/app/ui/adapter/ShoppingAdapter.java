package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.ShoppingItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder> {

    public interface OnShoppingItemActionListener {
        void onTogglePurchased(ShoppingItem item, boolean isPurchased);
        void onMoveToInventory(ShoppingItem item);
        void onDelete(ShoppingItem item);
    }

    private final Context context;
    private final List<ShoppingItem> items = new ArrayList<>();
    private final OnShoppingItemActionListener listener;

    public ShoppingAdapter(Context context, OnShoppingItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ShoppingItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShoppingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping, parent, false);
        return new ShoppingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ShoppingViewHolder holder, int position) {
        final ShoppingItem item = items.get(position);

        holder.tvName.setText(item.getName());

        String details = String.format(Locale.US, "%.1f %s • %s",
                item.getQuantity(), item.getUnit(), item.getCategory());
        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            details += " • " + item.getNotes();
        }
        holder.tvDetails.setText(details);

        // Avoid triggering listener during bind
        holder.cbPurchased.setOnCheckedChangeListener(null);
        holder.cbPurchased.setChecked(item.isPurchased());

        // Strike through text when purchased
        if (item.isPurchased()) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setAlpha(0.6f);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvName.setAlpha(1.0f);
        }

        holder.cbPurchased.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null) {
                    listener.onTogglePurchased(item, isChecked);
                }
            }
        });

        holder.btnMoveInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onMoveToInventory(item);
            }
        });

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

    public static class ShoppingViewHolder extends RecyclerView.ViewHolder {
        final CheckBox cbPurchased;
        final TextView tvName;
        final TextView tvDetails;
        final ImageButton btnMoveInventory;
        final ImageButton btnDelete;

        public ShoppingViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPurchased = itemView.findViewById(R.id.cb_shopping_purchased);
            tvName = itemView.findViewById(R.id.tv_shopping_name);
            tvDetails = itemView.findViewById(R.id.tv_shopping_details);
            btnMoveInventory = itemView.findViewById(R.id.btn_move_inventory);
            btnDelete = itemView.findViewById(R.id.btn_delete_shopping);
        }
    }
}
