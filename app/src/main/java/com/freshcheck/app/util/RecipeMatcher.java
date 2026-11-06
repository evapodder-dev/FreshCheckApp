package com.freshcheck.app.util;

import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.model.IngredientMatchResult;
import com.freshcheck.app.model.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeMatcher {

    public static List<IngredientMatchResult> matchAndRank(List<Recipe> recipes, List<FoodItem> availableFoods) {
        List<IngredientMatchResult> results = new ArrayList<>();
        if (recipes == null || recipes.isEmpty()) {
            return results;
        }

        // Build set of food names and set of expiring food names
        Set<String> foodNames = new HashSet<>();
        Set<String> expiringFoodNames = new HashSet<>();

        if (availableFoods != null) {
            for (FoodItem item : availableFoods) {
                String cleanName = cleanIngredientName(item.getName());
                foodNames.add(cleanName);
                if (item.isExpiringOrExpired()) {
                    expiringFoodNames.add(cleanName);
                }
            }
        }

        for (Recipe recipe : recipes) {
            IngredientMatchResult result = new IngredientMatchResult(recipe);
            List<String> ingredients = recipe.getIngredients();

            if (ingredients == null || ingredients.isEmpty()) {
                result.setMatchPercentage(100);
                results.add(result);
                continue;
            }

            int matchedCount = 0;
            for (String rawIngredient : ingredients) {
                String cleanIng = cleanIngredientName(rawIngredient);
                boolean matched = false;
                boolean isExpiring = false;

                for (String available : foodNames) {
                    if (isPartialMatch(cleanIng, available)) {
                        matched = true;
                        if (expiringFoodNames.contains(available)) {
                            isExpiring = true;
                        }
                        break;
                    }
                }

                if (matched) {
                    matchedCount++;
                    result.getAvailableIngredients().add(rawIngredient);
                    if (isExpiring) {
                        result.getExpiringIngredientsUsed().add(rawIngredient);
                    }
                } else {
                    result.getMissingIngredients().add(rawIngredient);
                }
            }

            int pct = (int) Math.round(((double) matchedCount / ingredients.size()) * 100.0);
            result.setMatchPercentage(pct);
            results.add(result);
        }

        // Rank recipes: Prioritize recipes using expiring ingredients, then highest match %
        Collections.sort(results, new Comparator<IngredientMatchResult>() {
            @Override
            public int compare(IngredientMatchResult r1, IngredientMatchResult r2) {
                // 1. Expiring ingredients used count (descending)
                int expCompare = Integer.compare(r2.getExpiringCount(), r1.getExpiringCount());
                if (expCompare != 0) return expCompare;

                // 2. Match percentage (descending)
                return Integer.compare(r2.getMatchPercentage(), r1.getMatchPercentage());
            }
        });

        return results;
    }

    private static String cleanIngredientName(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("\\(.*\\)", "") // remove parentheses
                .replaceAll("[0-9/]+", "") // remove numbers/fractions
                .replaceAll("\\b(tbsp|tsp|cup|cups|g|kg|ml|oz|lbs|slices|slice|cloves|can|cans|pinch|to taste)\\b", "")
                .trim();
    }

    private static boolean isPartialMatch(String ing1, String ing2) {
        if (ing1.isEmpty() || ing2.isEmpty()) return false;
        return ing1.contains(ing2) || ing2.contains(ing1);
    }
}