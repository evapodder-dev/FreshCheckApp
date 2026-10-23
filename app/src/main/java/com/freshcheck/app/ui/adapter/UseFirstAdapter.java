package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.CategoryConstants;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UseFirstAdapter extends RecyclerView.Adapter<UseFirstAdapter.UseFirstViewHolder> {

    public interface OnUseFirstActionListener {
        void onQuickConsumed(FoodItem item);
        void onQuickWaste(FoodItem item);
    }

    private final Context context;
    private final List<FoodItem> items = new ArrayList<>();
    private final OnUseFirstActionListener listener;

    public UseFirstAdapter(Context context, OnUseFirstActionListener listener) {
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
    public UseFirstViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_use_first, parent, false);
        return new UseFirstViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UseFirstViewHolder holder, int position) {
        final FoodItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvInfo.setText(String.format(Locale.US, "%.1f %s • %s • %s",
                item.getQuantity(), item.getUnit(), item.getLocation(), DateUtils.getRelativeExpiryText(item.getExpiryDate())));

        String status = item.getStatus();
        if (CategoryConstants.STATUS_EXPIRED.equals(status)) {
            holder.tvBadge.setText("Expired");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_expired);
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expired));
        } else if (CategoryConstants.STATUS_EXPIRES_TODAY.equals(status)) {
            holder.tvBadge.setText("Expires Today");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_today);
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.status_today));
        } else {
            holder.tvBadge.setText("Expiring Soon");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expiring));
        }

        holder.btnConsumed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onQuickConsumed(item);
            }
        });

        holder.btnWaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onQuickWaste(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class UseFirstViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvBadge;
        final TextView tvInfo;
        final Button btnConsumed;
        final Button btnWaste;

        public UseFirstViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_use_first_name);
            tvBadge = itemView.findViewById(R.id.tv_use_first_badge);
            tvInfo = itemView.findViewById(R.id.tv_use_first_info);
            btnConsumed = itemView.findViewById(R.id.btn_quick_consumed);
            btnWaste = itemView.findViewById(R.id.btn_quick_waste);
        }
    }
}
