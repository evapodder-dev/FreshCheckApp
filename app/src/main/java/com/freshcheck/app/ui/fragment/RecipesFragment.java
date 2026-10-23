package com.freshcheck.app.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freshcheck.app.R;
import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.Recipe;
import com.freshcheck.app.network.LocalRecipeRepository;
import com.freshcheck.app.network.RecipeApiService;
import com.freshcheck.app.ui.activity.RecipeDetailActivity;
import com.freshcheck.app.ui.adapter.RecipeAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {
    private static final String TAG = "RecipesFragment";

    private EditText etSearch;
    private ProgressBar pbLoading;
    private TextView tvBanner;
    private RecyclerView rvRecipes;
    private View layoutEmpty;

    private RecipeAdapter adapter;
    private FoodRepository foodRepository;
    private RecipeApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        foodRepository = new FoodRepository(requireContext());
        apiService = new RecipeApiService();

        etSearch = view.findViewById(R.id.et_search_recipe);
        pbLoading = view.findViewById(R.id.pb_recipe_loading);
        tvBanner = view.findViewById(R.id.tv_recipe_banner);
        rvRecipes = view.findViewById(R.id.rv_recipes);
        layoutEmpty = view.findViewById(R.id.layout_empty_recipes);

        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(requireContext(), this);
        rvRecipes.setAdapter(adapter);

        setupSearch();

        view.findViewById(R.id.btn_fetch_online).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOnlineRecipes();
            }
        });

        loadIngredientMatchedRecipes();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadIngredientMatchedRecipes() {
        Log.d(TAG, "Matching local recipes to current inventory...");
        List<FoodItem> active = foodRepository.getAllActive();
        List<String> ingredientNames = new ArrayList<>();

        for (FoodItem item : active) {
            ingredientNames.add(item.getName());
        }

        List<Recipe> suggested = LocalRecipeRepository.suggestForIngredients(ingredientNames);
        adapter.setRecipes(suggested);

        tvBanner.setText("Showing recipes prioritizing your " + active.size() + " inventory ingredients");
        updateEmptyState(suggested.isEmpty());
    }

    private void filterRecipes(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadIngredientMatchedRecipes();
            return;
        }

        String q = query.toLowerCase().trim();
        List<Recipe> all = LocalRecipeRepository.getCuratedRecipes();
        List<Recipe> filtered = new ArrayList<>();

        for (Recipe r : all) {
            if (r.getTitle().toLowerCase().contains(q) || r.matchesIngredient(q) || r.getCategory().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }

        adapter.setRecipes(filtered);
        tvBanner.setText("Filtered recipes matching '" + query + "'");
        updateEmptyState(filtered.isEmpty());
    }

    private void fetchOnlineRecipes() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            query = "Chicken"; // Default appealing online query
            etSearch.setText(query);
        }

        pbLoading.setVisibility(View.VISIBLE);
        tvBanner.setText("Querying TheMealDB API for '" + query + "'...");

        apiService.searchRecipes(query, new RecipeApiService.RecipeCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes, boolean isFromNetwork) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                adapter.setRecipes(recipes);
                updateEmptyState(recipes.isEmpty());

                if (isFromNetwork) {
                    tvBanner.setText("✓ Loaded " + recipes.size() + " live recipes from TheMealDB API");
                    Toast.makeText(getContext(), "Fetched " + recipes.size() + " online recipes", Toast.LENGTH_SHORT).show();
                } else {
                    tvBanner.setText("Offline mode: Loaded " + recipes.size() + " curated recipes");
                }
            }

            @Override
            public void onError(String errorMessage, List<Recipe> fallbackRecipes) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                adapter.setRecipes(fallbackRecipes);
                updateEmptyState(fallbackRecipes.isEmpty());
                tvBanner.setText("Offline: " + errorMessage);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvRecipes.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRecipes.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe);
        startActivity(intent);
    }
}
