package com.freshcheck.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final Context context;
    private final List<Recipe> recipes = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(Context context, OnRecipeClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRecipes(List<Recipe> newRecipes) {
        this.recipes.clear();
        if (newRecipes != null) {
            this.recipes.addAll(newRecipes);
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
        final Recipe recipe = recipes.get(position);

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvCategory.setText(recipe.getCategory());
        holder.tvTime.setText("⏱️ " + recipe.getPrepTimeMinutes() + " mins");
        holder.tvDifficulty.setText("• " + recipe.getDifficulty());

        // Ingredients summary
        StringBuilder ingPreview = new StringBuilder("Ingredients: ");
        List<String> ings = recipe.getIngredients();
        for (int i = 0; i < Math.min(ings.size(), 4); i++) {
            ingPreview.append(ings.get(i));
            if (i < Math.min(ings.size(), 4) - 1) ingPreview.append(", ");
        }
        if (ings.size() > 4) ingPreview.append("...");
        holder.tvIngredientsPreview.setText(ingPreview.toString());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvCategory;
        final TextView tvTime;
        final TextView tvDifficulty;
        final TextView tvIngredientsPreview;
        final TextView tvViewAction;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvCategory = itemView.findViewById(R.id.tv_recipe_category);
            tvTime = itemView.findViewById(R.id.tv_recipe_time);
            tvDifficulty = itemView.findViewById(R.id.tv_recipe_difficulty);
            tvIngredientsPreview = itemView.findViewById(R.id.tv_recipe_ingredients_preview);
            tvViewAction = itemView.findViewById(R.id.tv_view_recipe_action);
        }
    }
}
