package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    public interface OnFoodItemActionListener {
        void onEdit(FoodItem item);
        void onDelete(FoodItem item);
        void onMarkConsumed(FoodItem item);
        void onRecordWaste(FoodItem item);
    }

    private final Context context;
    private final List<FoodItem> items = new ArrayList<>();
    private final OnFoodItemActionListener listener;

    public FoodAdapter(Context context, OnFoodItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<FoodItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_card, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FoodViewHolder holder, int position) {
        final FoodItem item = items.get(position);

        holder.tvFoodName.setText(item.getName());

        String details = String.format(Locale.US, "%.1f %s • %s • %s",
                item.getQuantity(), item.getUnit(), item.getCategory(), item.getLocation());
        holder.tvFoodDetails.setText(details);

        holder.tvExpiryCountdown.setText(DateUtils.getRelativeExpiryText(item.getExpiryDate()));

        // Status badge styling
        String status = item.getStatus();
        switch (status) {
            case CategoryConstants.STATUS_EXPIRED:
                holder.tvStatusBadge.setText("Expired");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expired);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expired));
                break;
            case CategoryConstants.STATUS_EXPIRES_TODAY:
                holder.tvStatusBadge.setText("Expires Today");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_today);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_today));
                break;
            case CategoryConstants.STATUS_EXPIRING_SOON:
                holder.tvStatusBadge.setText("Expiring Soon");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expiring));
                break;
            default:
                holder.tvStatusBadge.setText("Fresh");
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_fresh));
                break;
        }

        // Popup menu handling
        holder.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v);
                popup.inflate(R.menu.inventory_item_menu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (id == R.id.menu_mark_consumed) {
                            if (listener != null) listener.onMarkConsumed(item);
                            return true;
                        } else if (id == R.id.menu_record_waste) {
                            if (listener != null) listener.onRecordWaste(item);
                            return true;
                        } else if (id == R.id.menu_edit) {
                            if (listener != null) listener.onEdit(item);
                            return true;
                        } else if (id == R.id.menu_delete) {
                            if (listener != null) listener.onDelete(item);
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onEdit(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFoodName;
        final TextView tvFoodDetails;
        final TextView tvStatusBadge;
        final TextView tvExpiryCountdown;
        final ImageButton btnMenu;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDetails = itemView.findViewById(R.id.tv_food_details);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            tvExpiryCountdown = itemView.findViewById(R.id.tv_expiry_countdown);
            btnMenu = itemView.findViewById(R.id.btn_food_menu);
        }
    }
}
