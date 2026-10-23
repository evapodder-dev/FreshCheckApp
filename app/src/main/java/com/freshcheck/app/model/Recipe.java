package com.freshcheck.app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Recipe implements Serializable {
    private String id;
    private String title;
    private String category;
    private int prepTimeMinutes;
    private String difficulty;
    private List<String> ingredients;
    private List<String> instructions;
    private String imageUrl;
    private String sourceUrl;
    private boolean isLocal;

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.difficulty = "Easy";
        this.prepTimeMinutes = 20;
        this.isLocal = true;
    }

    public Recipe(String id, String title, String category, int prepTimeMinutes,
                  String difficulty, List<String> ingredients, List<String> instructions,
                  String imageUrl, String sourceUrl, boolean isLocal) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.prepTimeMinutes = prepTimeMinutes;
        this.difficulty = difficulty;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.sourceUrl = sourceUrl != null ? sourceUrl : "";
        this.isLocal = isLocal;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category != null ? category : "General"; }
    public void setCategory(String category) { this.category = category; }

    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public void setPrepTimeMinutes(int prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }

    public String getDifficulty() { return difficulty != null ? difficulty : "Easy"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public boolean isLocal() { return isLocal; }
    public void setLocal(boolean local) { isLocal = local; }

    public boolean matchesIngredient(String query) {
        if (query == null || query.trim().isEmpty()) return false;
        String q = query.toLowerCase().trim();
        for (String ing : ingredients) {
            if (ing.toLowerCase().contains(q)) return true;
        }
        return false;
    }

    public String getIngredientsBulletList() {
        StringBuilder sb = new StringBuilder();
        for (String ing : ingredients) {
            sb.append("• ").append(ing).append("\n");
        }
        return sb.toString().trim();
    }

    public String getInstructionsFormatted() {
        StringBuilder sb = new StringBuilder();
        int step = 1;
        for (String inst : instructions) {
            sb.append(step++).append(". ").append(inst).append("\n\n");
        }
        return sb.toString().trim();
    }
}
