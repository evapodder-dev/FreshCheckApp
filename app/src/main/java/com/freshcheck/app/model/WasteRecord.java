package com.freshcheck.app.model;

import android.content.ContentValues;
import com.freshcheck.app.util.DateUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WasteRecord implements Serializable {
    private long id;
    private String foodName;
    private double quantity;
    private String unit;
    private String wasteDate;
    private String reason;
    private double estimatedCost;
    private String notes;
    private String householdId;

    public WasteRecord() {
        this.quantity = 1.0;
        this.unit = "items";
        this.wasteDate = DateUtils.getTodayString();
        this.reason = "Past Expiry Date";
        this.estimatedCost = 0.0;
        this.notes = "";
        this.householdId = "";
    }

    public WasteRecord(long id, String foodName, double quantity, String unit,
                       String wasteDate, String reason, double estimatedCost, String notes) {
        this(id, foodName, quantity, unit, wasteDate, reason, estimatedCost, notes, "");
    }

    public WasteRecord(long id, String foodName, double quantity, String unit,
                       String wasteDate, String reason, double estimatedCost, String notes, String householdId) {
        this.id = id;
        this.foodName = foodName;
        this.quantity = quantity;
        this.unit = unit;
        this.wasteDate = wasteDate;
        this.reason = reason;
        this.estimatedCost = estimatedCost;
        this.notes = notes;
        this.householdId = householdId != null ? householdId : "";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFoodName() { return foodName != null ? foodName : ""; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit != null ? unit : "items"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getWasteDate() { return wasteDate != null ? wasteDate : ""; }
    public void setWasteDate(String wasteDate) { this.wasteDate = wasteDate; }

    public String getReason() { return reason != null ? reason : "Other"; }
    public void setReason(String reason) { this.reason = reason; }

    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getHouseholdId() { return householdId != null ? householdId : ""; }
    public void setHouseholdId(String householdId) { this.householdId = householdId; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("food_name", foodName);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("waste_date", wasteDate);
        values.put("reason", reason);
        values.put("estimated_cost", estimatedCost);
        values.put("notes", notes);
        values.put("household_id", householdId);
        return values;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("foodName", foodName);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("wasteDate", wasteDate);
        map.put("reason", reason);
        map.put("estimatedCost", estimatedCost);
        map.put("notes", notes);
        map.put("householdId", householdId);
        return map;
    }
}