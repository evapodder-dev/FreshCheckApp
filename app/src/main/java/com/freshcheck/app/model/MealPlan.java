package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MealPlan implements Serializable {
    private long id;
    private String planDate; // YYYY-MM-DD
    private String mealType; // Breakfast, Lunch, Dinner, Snack
    private String recipeId;
    private String recipeTitle;
    private String notes;
    private String householdId; // null or empty for personal
    private boolean isCompleted;

    public MealPlan() {
        this.planDate = "";
        this.mealType = "Dinner";
        this.recipeId = "";
        this.recipeTitle = "";
        this.notes = "";
        this.householdId = "";
        this.isCompleted = false;
    }

    public MealPlan(long id, String planDate, String mealType, String recipeId,
                    String recipeTitle, String notes, String householdId, boolean isCompleted) {
        this.id = id;
        this.planDate = planDate;
        this.mealType = mealType;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.notes = notes;
        this.householdId = householdId;
        this.isCompleted = isCompleted;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPlanDate() { return planDate != null ? planDate : ""; }
    public void setPlanDate(String planDate) { this.planDate = planDate; }

    public String getMealType() { return mealType != null ? mealType : "Dinner"; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getRecipeId() { return recipeId != null ? recipeId : ""; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public String getRecipeTitle() { return recipeTitle != null ? recipeTitle : ""; }
    public void setRecipeTitle(String recipeTitle) { this.recipeTitle = recipeTitle; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getHouseholdId() { return householdId != null ? householdId : ""; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("plan_date", planDate);
        cv.put("meal_type", mealType);
        cv.put("recipe_id", recipeId);
        cv.put("recipe_title", recipeTitle);
        cv.put("notes", notes);
        cv.put("household_id", householdId);
        cv.put("is_completed", isCompleted ? 1 : 0);
        return cv;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("planDate", planDate);
        map.put("mealType", mealType);
        map.put("recipeId", recipeId);
        map.put("recipeTitle", recipeTitle);
        map.put("notes", notes);
        map.put("householdId", householdId);
        map.put("isCompleted", isCompleted);
        return map;
    }
}