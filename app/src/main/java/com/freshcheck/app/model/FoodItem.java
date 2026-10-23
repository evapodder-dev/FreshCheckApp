package com.freshcheck.app.model;

import android.content.ContentValues;
import com.freshcheck.app.util.DateUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FoodItem implements Serializable {
    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private String purchaseDate;
    private String expiryDate;
    private String location;
    private double estimatedCost;
    private boolean isConsumed;
    private boolean isWasted;
    private String notes;
    private String firebaseId;

    public FoodItem() {
        this.quantity = 1.0;
        this.unit = "items";
        this.category = "Produce (Fruits & Veg)";
        this.location = "Refrigerator";
        this.purchaseDate = DateUtils.getTodayString();
        this.expiryDate = DateUtils.getFutureDateString(3);
        this.estimatedCost = 0.0;
        this.isConsumed = false;
        this.isWasted = false;
        this.notes = "";
    }

    public FoodItem(long id, String name, double quantity, String unit, String category,
                    String purchaseDate, String expiryDate, String location,
                    double estimatedCost, boolean isConsumed, boolean isWasted,
                    String notes, String firebaseId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.location = location;
        this.estimatedCost = estimatedCost;
        this.isConsumed = isConsumed;
        this.isWasted = isWasted;
        this.notes = notes;
        this.firebaseId = firebaseId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit != null ? unit : "items"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category != null ? category : "Other"; }
    public void setCategory(String category) { this.category = category; }

    public String getPurchaseDate() { return purchaseDate != null ? purchaseDate : ""; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getExpiryDate() { return expiryDate != null ? expiryDate : ""; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getLocation() { return location != null ? location : "Refrigerator"; }
    public void setLocation(String location) { this.location = location; }

    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }

    public boolean isConsumed() { return isConsumed; }
    public void setConsumed(boolean consumed) { isConsumed = consumed; }

    public boolean isWasted() { return isWasted; }
    public void setWasted(boolean wasted) { isWasted = wasted; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    /**
     * Calculates days until expiry based on current date.
     */
    public long getDaysRemaining() {
        return DateUtils.getDaysUntil(expiryDate);
    }

    /**
     * Determines status key for categorization and badge coloring.
     */
    public String getStatus() {
        long days = getDaysRemaining();
        if (days < 0) {
            return CategoryConstants.STATUS_EXPIRED;
        } else if (days == 0) {
            return CategoryConstants.STATUS_EXPIRES_TODAY;
        } else if (days <= 3) {
            return CategoryConstants.STATUS_EXPIRING_SOON;
        } else {
            return CategoryConstants.STATUS_FRESH;
        }
    }

    public String getStatusLabel() {
        String status = getStatus();
        switch (status) {
            case CategoryConstants.STATUS_EXPIRED:
                return "Expired";
            case CategoryConstants.STATUS_EXPIRES_TODAY:
                return "Expires Today";
            case CategoryConstants.STATUS_EXPIRING_SOON:
                return "Expiring Soon";
            default:
                return "Fresh";
        }
    }

    public boolean isExpiringOrExpired() {
        long days = getDaysRemaining();
        return days <= 3;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("category", category);
        values.put("purchase_date", purchaseDate);
        values.put("expiry_date", expiryDate);
        values.put("location", location);
        values.put("estimated_cost", estimatedCost);
        values.put("is_consumed", isConsumed ? 1 : 0);
        values.put("is_wasted", isWasted ? 1 : 0);
        values.put("notes", notes);
        values.put("firebase_id", firebaseId);
        return values;
    }

    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("category", category);
        map.put("purchaseDate", purchaseDate);
        map.put("expiryDate", expiryDate);
        map.put("location", location);
        map.put("estimatedCost", estimatedCost);
        map.put("isConsumed", isConsumed);
        map.put("isWasted", isWasted);
        map.put("notes", notes);
        return map;
    }
}
