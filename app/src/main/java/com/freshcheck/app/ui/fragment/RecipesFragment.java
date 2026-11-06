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
import com.freshcheck.app.model.IngredientMatchResult;
import com.freshcheck.app.model.Recipe;
import com.freshcheck.app.network.LocalRecipeRepository;
import com.freshcheck.app.network.RecipeApiService;
import com.freshcheck.app.ui.activity.RecipeDetailActivity;
import com.freshcheck.app.ui.adapter.RecipeAdapter;
import com.freshcheck.app.util.RecipeMatcher;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {
    private static final String TAG = "RecipesFragment";

    private EditText etSearch;
    private ProgressBar pbLoading;
    private TextView tvBanner;
    private RecyclerView rvRecipes;
    private View layoutEmpty;
    private ChipGroup chipGroupCategory;

    private RecipeAdapter adapter;
    private FoodRepository foodRepository;
    private RecipeApiService apiService;

    private String currentCategory = "All";
    private String currentSearch = "";
    private List<Recipe> currentRecipePool = new ArrayList<>();

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
        chipGroupCategory = view.findViewById(R.id.chip_group_recipe_category);

        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(requireContext(), this);
        rvRecipes.setAdapter(adapter);

        setupSearch();
        setupCategoryChips();

        view.findViewById(R.id.btn_fetch_online).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOnlineRecipes();
            }
        });

        // Initialize with all curated offline recipes
        currentRecipePool = LocalRecipeRepository.getCuratedRecipes();
        loadIngredientMatchedRecipes();

        return view;
    }

    private void setupCategoryChips() {
        if (chipGroupCategory == null) return;
        chipGroupCategory.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                if (checkedId == R.id.chip_recipe_cat_breakfast) {
                    currentCategory = "Breakfast";
                } else if (checkedId == R.id.chip_recipe_cat_lunch) {
                    currentCategory = "Lunch";
                } else if (checkedId == R.id.chip_recipe_cat_dinner) {
                    currentCategory = "Dinner";
                } else if (checkedId == R.id.chip_recipe_cat_snacks) {
                    currentCategory = "Snacks";
                } else {
                    currentCategory = "All";
                }
                applyFilterAndMatch();
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                applyFilterAndMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        applyFilterAndMatch();
    }

    private void loadIngredientMatchedRecipes() {
        currentRecipePool = LocalRecipeRepository.getCuratedRecipes();
        applyFilterAndMatch();
    }

    private void applyFilterAndMatch() {
        List<FoodItem> activeFoods = foodRepository.getAllActive();
        List<Recipe> filteredRecipes = new ArrayList<>();

        String q = currentSearch != null ? currentSearch.toLowerCase().trim() : "";
        boolean filterCat = currentCategory != null && !currentCategory.equalsIgnoreCase("All");

        for (Recipe r : currentRecipePool) {
            boolean matchesSearch = q.isEmpty()
                    || r.getTitle().toLowerCase().contains(q)
                    || r.matchesIngredient(q)
                    || r.getCategory().toLowerCase().contains(q);

            boolean matchesCat = !filterCat || r.getCategory().equalsIgnoreCase(currentCategory);

            if (matchesSearch && matchesCat) {
                filteredRecipes.add(r);
            }
        }

        // Run intelligent matching algorithm
        List<IngredientMatchResult> matchedAndRanked = RecipeMatcher.matchAndRank(filteredRecipes, activeFoods);
        adapter.setMatchResults(matchedAndRanked);

        int expiringCount = 0;
        for (IngredientMatchResult res : matchedAndRanked) {
            if (res.hasExpiringIngredients()) expiringCount++;
        }

        if (expiringCount > 0) {
            tvBanner.setText("🔥 " + expiringCount + " recipes use food expiring soon! Prioritized for waste reduction.");
        } else {
            tvBanner.setText("Showing " + matchedAndRanked.size() + " recipes matched against your " + activeFoods.size() + " inventory items.");
        }

        updateEmptyState(matchedAndRanked.isEmpty());
    }

    private void fetchOnlineRecipes() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            query = "Chicken";
            etSearch.setText(query);
        }

        pbLoading.setVisibility(View.VISIBLE);
        tvBanner.setText("Searching TheMealDB online for '" + query + "'...");

        apiService.searchRecipes(query, new RecipeApiService.RecipeCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes, boolean isFromNetwork) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);

                currentRecipePool = recipes;
                applyFilterAndMatch();

                if (isFromNetwork) {
                    Toast.makeText(getContext(), "Fetched " + recipes.size() + " online recipes from TheMealDB", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Offline: Showing cached/curated recipes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage, List<Recipe> fallbackRecipes) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                currentRecipePool = fallbackRecipes;
                applyFilterAndMatch();
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
