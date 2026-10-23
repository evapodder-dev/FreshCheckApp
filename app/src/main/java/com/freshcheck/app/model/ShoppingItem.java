package com.freshcheck.app.model;

import android.content.ContentValues;
import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private boolean isPurchased;
    private String notes;

    public ShoppingItem() {
        this.quantity = 1.0;
        this.unit = "items";
        this.category = "Produce (Fruits & Veg)";
        this.isPurchased = false;
        this.notes = "";
    }

    public ShoppingItem(long id, String name, double quantity, String unit,
                        String category, boolean isPurchased, String notes) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.isPurchased = isPurchased;
        this.notes = notes;
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

    public boolean isPurchased() { return isPurchased; }
    public void setPurchased(boolean purchased) { isPurchased = purchased; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("category", category);
        values.put("is_purchased", isPurchased ? 1 : 0);
        values.put("notes", notes);
        return values;
    }
}
