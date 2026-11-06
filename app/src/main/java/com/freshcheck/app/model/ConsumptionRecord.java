package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ConsumptionRecord implements Serializable {
    private long id;
    private long foodId;
    private String foodName;
    private double quantity;
    private String unit;
    private String consumedDate; // YYYY-MM-DD
    private String category;
    private String householdId;

    public ConsumptionRecord() {
        this.foodName = "";
        this.quantity = 1.0;
        this.unit = "items";
        this.consumedDate = "";
        this.category = "Others";
        this.householdId = "";
    }

    public ConsumptionRecord(long id, long foodId, String foodName, double quantity,
                             String unit, String consumedDate, String category, String householdId) {
        this.id = id;
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.unit = unit;
        this.consumedDate = consumedDate;
        this.category = category;
        this.householdId = householdId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFoodId() { return foodId; }
    public void setFoodId(long foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName != null ? foodName : ""; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit != null ? unit : "items"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getConsumedDate() { return consumedDate != null ? consumedDate : ""; }
    public void setConsumedDate(String consumedDate) { this.consumedDate = consumedDate; }

    public String getCategory() { return category != null ? category : "Others"; }
    public void setCategory(String category) { this.category = category; }

    public String getHouseholdId() { return householdId != null ? householdId : ""; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("food_id", foodId);
        cv.put("food_name", foodName);
        cv.put("quantity", quantity);
        cv.put("unit", unit);
        cv.put("consumed_date", consumedDate);
        cv.put("category", category);
        cv.put("household_id", householdId);
        return cv;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("foodId", foodId);
        map.put("foodName", foodName);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("consumedDate", consumedDate);
        map.put("category", category);
        map.put("householdId", householdId);
        return map;
    }
}