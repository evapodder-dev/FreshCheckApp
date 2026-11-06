package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.IngredientMatchResult;
import com.freshcheck.app.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final Context context;
    private final List<IngredientMatchResult> matchResults = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(Context context, OnRecipeClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setMatchResults(List<IngredientMatchResult> results) {
        this.matchResults.clear();
        if (results != null) {
            this.matchResults.addAll(results);
        }
        notifyDataSetChanged();
    }

    public void setRecipes(List<Recipe> recipes) {
        this.matchResults.clear();
        if (recipes != null) {
            for (Recipe r : recipes) {
                IngredientMatchResult res = new IngredientMatchResult(r);
                res.setMatchPercentage(100);
                this.matchResults.add(res);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        final IngredientMatchResult matchResult = matchResults.get(position);
        final Recipe recipe = matchResult.getRecipe();

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvCategory.setText(recipe.getCategory());
        holder.tvTime.setText("⏱️ " + recipe.getPrepTimeMinutes() + " mins");
        holder.tvDifficulty.setText("• " + recipe.getDifficulty());

        // Match Percentage Badge
        int pct = matchResult.getMatchPercentage();
        holder.tvMatchBadge.setText(pct + "% Match");
        if (pct >= 80) {
            holder.tvMatchBadge.setBackgroundResource(R.drawable.bg_badge_fresh);
            holder.tvMatchBadge.setTextColor(ContextCompat.getColor(context, R.color.status_fresh));
        } else if (pct >= 50) {
            holder.tvMatchBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
            holder.tvMatchBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expiring));
        } else {
            holder.tvMatchBadge.setBackgroundResource(R.drawable.bg_badge_today);
            holder.tvMatchBadge.setTextColor(ContextCompat.getColor(context, R.color.status_today));
        }

        // Expiring badge
        if (matchResult.hasExpiringIngredients()) {
            holder.tvExpiringBadge.setVisibility(View.VISIBLE);
            holder.tvExpiringBadge.setText("🔥 " + matchResult.getExpiringCount() + " Expiring Used");
        } else {
            holder.tvExpiringBadge.setVisibility(View.GONE);
        }

        // Ingredients summary
        StringBuilder ingPreview = new StringBuilder("Ingredients: ");
        List<String> ings = recipe.getIngredients();
        for (int i = 0; i < Math.min(ings.size(), 4); i++) {
            ingPreview.append(ings.get(i));
            if (i < Math.min(ings.size(), 4) - 1) ingPreview.append(", ");
        }
        if (ings.size() > 4) ingPreview.append("...");
        holder.tvIngredientsPreview.setText(ingPreview.toString());

        // Missing ingredients notice
        List<String> missing = matchResult.getMissingIngredients();
        if (missing != null && !missing.isEmpty() && pct < 100) {
            holder.tvMissingPreview.setVisibility(View.VISIBLE);
            StringBuilder sbMissing = new StringBuilder("Missing: ");
            for (int i = 0; i < Math.min(missing.size(), 3); i++) {
                sbMissing.append(missing.get(i));
                if (i < Math.min(missing.size(), 3) - 1) sbMissing.append(", ");
            }
            if (missing.size() > 3) sbMissing.append(" (+").append(missing.size() - 3).append(" more)");
            holder.tvMissingPreview.setText(sbMissing.toString());
        } else {
            holder.tvMissingPreview.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return matchResults.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMatchBadge;
        final TextView tvCategory;
        final TextView tvTime;
        final TextView tvDifficulty;
        final TextView tvExpiringBadge;
        final TextView tvIngredientsPreview;
        final TextView tvMissingPreview;
        final TextView tvViewAction;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvMatchBadge = itemView.findViewById(R.id.tv_recipe_match_badge);
            tvCategory = itemView.findViewById(R.id.tv_recipe_category);
            tvTime = itemView.findViewById(R.id.tv_recipe_time);
            tvDifficulty = itemView.findViewById(R.id.tv_recipe_difficulty);
            tvExpiringBadge = itemView.findViewById(R.id.tv_recipe_expiring_badge);
            tvIngredientsPreview = itemView.findViewById(R.id.tv_recipe_ingredients_preview);
            tvMissingPreview = itemView.findViewById(R.id.tv_recipe_missing_preview);
            tvViewAction = itemView.findViewById(R.id.tv_view_recipe_action);
        }
    }
}
