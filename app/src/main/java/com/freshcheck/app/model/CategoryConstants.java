package com.freshcheck.app.model;

public final class CategoryConstants {
    private CategoryConstants() {}

    public static final String[] CATEGORIES = {
            "Produce (Fruits & Veg)",
            "Dairy & Eggs",
            "Meat & Poultry",
            "Seafood",
            "Bakery & Bread",
            "Pantry & Dry Goods",
            "Frozen Foods",
            "Beverages",
            "Other"
    };

    public static final String[] STORAGE_LOCATIONS = {
            "Refrigerator",
            "Freezer",
            "Pantry",
            "Countertop"
    };

    public static final String[] UNITS = {
            "items",
            "kg",
            "g",
            "lbs",
            "oz",
            "liters",
            "ml",
            "packs",
            "bottles",
            "cans"
    };

    public static final String[] WASTE_REASONS = {
            "Spoiled / Moldy",
            "Past Expiry Date",
            "Overcooked / Burned",
            "Uneaten Leftover",
            "Other / Accidental"
    };

    public static final String STATUS_FRESH = "FRESH";
    public static final String STATUS_EXPIRING_SOON = "EXPIRING_SOON";
    public static final String STATUS_EXPIRES_TODAY = "EXPIRES_TODAY";
    public static final String STATUS_EXPIRED = "EXPIRED";
}
