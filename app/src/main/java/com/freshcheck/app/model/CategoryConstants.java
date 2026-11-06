package com.freshcheck.app.model;

public final class CategoryConstants {
    private CategoryConstants() {}

    public static final String CURRENCY_SYMBOL = "৳";

    // Standard Categories per specification
    public static final String[] CATEGORIES = {
            "Vegetables",
            "Fruits",
            "Dairy",
            "Meat",
            "Fish",
            "Bakery",
            "Grains",
            "Beverages",
            "Snacks",
            "Others"
    };

    // Standard Storage Locations per specification
    public static final String[] STORAGE_LOCATIONS = {
            "Refrigerator",
            "Freezer",
            "Pantry",
            "Kitchen Shelf",
            "Drawer",
            "Other"
    };

    public static final String[] UNITS = {
            "items",
            "kg",
            "g",
            "liters",
            "ml",
            "packs",
            "bottles",
            "cans",
            "lbs",
            "oz"
    };

    public static final String[] WASTE_REASONS = {
            "Spoiled / Moldy",
            "Past Expiry Date",
            "Overcooked / Burned",
            "Uneaten Leftover",
            "Other / Accidental"
    };

    public static final String[] MEAL_TYPES = {
            "Breakfast",
            "Lunch",
            "Dinner",
            "Snack"
    };

    public static final String STATUS_FRESH = "FRESH";
    public static final String STATUS_EXPIRING_SOON = "EXPIRING_SOON";
    public static final String STATUS_EXPIRES_TODAY = "EXPIRES_TODAY";
    public static final String STATUS_EXPIRED = "EXPIRED";
}