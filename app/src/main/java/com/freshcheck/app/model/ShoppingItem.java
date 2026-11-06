package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ShoppingItem implements Serializable {
    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private boolean isPurchased;
    private double estimatedCost;
    private String notes;
    private String householdId;

    public ShoppingItem() {
        this.quantity = 1.0;
        this.unit = "items";
        this.category = "Vegetables";
        this.isPurchased = false;
        this.estimatedCost = 0.0;
        this.notes = "";
        this.householdId = "";
    }

    public ShoppingItem(long id, String name, double quantity, String unit,
                        String category, boolean isPurchased, String notes) {
        this(id, name, quantity, unit, category, isPurchased, 0.0, notes, "");
    }

    public ShoppingItem(long id, String name, double quantity, String unit,
                        String category, boolean isPurchased, double estimatedCost,
                        String notes, String householdId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.isPurchased = isPurchased;
        this.estimatedCost = estimatedCost;
        this.notes = notes;
        this.householdId = householdId != null ? householdId : "";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit != null ? unit : "items"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category != null ? category : "Others"; }
    public void setCategory(String category) { this.category = category; }

    public boolean isPurchased() { return isPurchased; }
    public void setPurchased(boolean purchased) { isPurchased = purchased; }

    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getHouseholdId() { return householdId != null ? householdId : ""; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("category", category);
        values.put("is_purchased", isPurchased ? 1 : 0);
        values.put("estimated_cost", estimatedCost);
        values.put("notes", notes);
        values.put("household_id", householdId);
        return values;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("category", category);
        map.put("isPurchased", isPurchased);
        map.put("estimatedCost", estimatedCost);
        map.put("notes", notes);
        map.put("householdId", householdId);
        return map;
    }
}