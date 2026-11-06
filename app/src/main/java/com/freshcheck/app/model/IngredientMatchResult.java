package com.freshcheck.app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IngredientMatchResult implements Serializable {
    private Recipe recipe;
    private int matchPercentage;
    private List<String> availableIngredients;
    private List<String> missingIngredients;
    private List<String> expiringIngredientsUsed;

    public IngredientMatchResult(Recipe recipe) {
        this.recipe = recipe;
        this.matchPercentage = 0;
        this.availableIngredients = new ArrayList<>();
        this.missingIngredients = new ArrayList<>();
        this.expiringIngredientsUsed = new ArrayList<>();
    }

    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { this.matchPercentage = matchPercentage; }

    public List<String> getAvailableIngredients() { return availableIngredients; }
    public void setAvailableIngredients(List<String> availableIngredients) {
        this.availableIngredients = availableIngredients != null ? availableIngredients : new ArrayList<String>();
    }

    public List<String> getMissingIngredients() { return missingIngredients; }
    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients != null ? missingIngredients : new ArrayList<String>();
    }

    public List<String> getExpiringIngredientsUsed() { return expiringIngredientsUsed; }
    public void setExpiringIngredientsUsed(List<String> expiringIngredientsUsed) {
        this.expiringIngredientsUsed = expiringIngredientsUsed != null ? expiringIngredientsUsed : new ArrayList<String>();
    }

    public int getExpiringCount() {
        return expiringIngredientsUsed.size();
    }

    public boolean hasExpiringIngredients() {
        return !expiringIngredientsUsed.isEmpty();
    }
}