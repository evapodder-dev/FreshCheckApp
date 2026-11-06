package com.freshcheck.app.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StorageGuideData {

    public static class StorageItem implements Serializable {
        public String name;
        public String category;
        public String bestLocation; // Refrigerator, Freezer, Pantry
        public String shelfLife;
        public String tips;
        public String freezingTips;

        public StorageItem(String name, String category, String bestLocation, String shelfLife, String tips, String freezingTips) {
            this.name = name;
            this.category = category;
            this.bestLocation = bestLocation;
            this.shelfLife = shelfLife;
            this.tips = tips;
            this.freezingTips = freezingTips;
        }
    }

    public static List<StorageItem> getAllGuideItems() {
        List<StorageItem> list = new ArrayList<>();

        // Vegetables
        list.add(new StorageItem("Leafy Greens (Spinach, Lettuce)", "Vegetables", "Refrigerator (Crisper)",
                "5–7 days", "Wrap in paper towel inside a loose bag to absorb excess moisture.", "Blanch for 2 mins, shock in ice water, freeze up to 6 months."));

        list.add(new StorageItem("Tomatoes", "Vegetables", "Countertop / Pantry",
                "7–10 days", "Keep stem-side down at room temperature; chilling ruins texture and flavor.", "Roast or puree first, freeze up to 9 months."));

        list.add(new StorageItem("Potatoes & Onions", "Vegetables", "Pantry (Cool, dark, dry)",
                "2–4 weeks", "Never store potatoes and onions together; onions emit ethylene gas causing potatoes to sprout.", "Not recommended raw. Parboil before freezing."));

        list.add(new StorageItem("Carrots & Beets", "Vegetables", "Refrigerator (Crisper)",
                "3–4 weeks", "Cut off green leafy tops to prevent moisture loss.", "Chop and blanch before freezing (8–10 months)."));

        // Fruits
        list.add(new StorageItem("Apples", "Fruits", "Refrigerator / Crisper",
                "3–4 weeks", "Apples emit high ethylene; store separately from other vegetables.", "Slice and toss in lemon juice before freezing."));

        list.add(new StorageItem("Bananas", "Fruits", "Countertop",
                "4–7 days", "Wrap stems in plastic wrap to slow ripening. Keep away from direct sunlight.", "Peel and freeze whole or sliced for smoothies (up to 6 months)."));

        list.add(new StorageItem("Berries (Strawberries, Blueberries)", "Fruits", "Refrigerator",
                "3–5 days", "Do not wash until immediately before eating. Store in ventilated container.", "Freeze in a single layer on a sheet tray before bagging."));

        // Dairy & Eggs
        list.add(new StorageItem("Fresh Milk", "Dairy", "Refrigerator (Middle shelf)",
                "5–7 days after opening", "Store on interior shelves where temperature is stable; never on door shelves.", "Freeze in airtight container leaving 1 inch headspace (up to 3 months)."));

        list.add(new StorageItem("Eggs", "Dairy", "Refrigerator (Original carton)",
                "3–5 weeks", "Keep in original carton in the coldest part of the fridge to prevent odors.", "Whisk yolk and whites together with a pinch of salt before freezing."));

        list.add(new StorageItem("Hard Cheese (Cheddar, Parmesan)", "Dairy", "Refrigerator",
                "3–4 weeks", "Wrap in parchment paper then loose foil so the cheese can breathe without drying.", "Grate and store in freezer bags with a pinch of cornstarch."));

        // Meat & Seafood
        list.add(new StorageItem("Fresh Chicken / Poultry", "Meat", "Refrigerator (Bottom shelf)",
                "1–2 days", "Always store on the lowest shelf in a leak-proof bowl to prevent cross-contamination.", "Wrap tightly in plastic and foil, freeze up to 9 months."));

        list.add(new StorageItem("Ground Beef / Meat", "Meat", "Refrigerator (Bottom shelf)",
                "1–2 days", "Cook within 48 hours or transfer directly to freezer.", "Flatten inside zip bags for rapid freezing and defrosting (up to 4 months)."));

        list.add(new StorageItem("Fresh Fish / Seafood", "Meat", "Refrigerator (Bottom shelf)",
                "1–2 days", "Keep on ice in refrigerator if not cooking immediately.", "Vacuum-seal or double wrap, freeze up to 6 months."));

        // Bakery & Grains
        list.add(new StorageItem("Bread & Bakery", "Bakery", "Pantry / Freezer",
                "3–5 days (Pantry)", "Avoid the refrigerator—cool temperatures accelerate starch retrogradation (staling).", "Slice before freezing; toast directly from frozen!"));

        list.add(new StorageItem("Rice, Lentils & Grains", "Grains", "Pantry (Airtight container)",
                "6–12 months", "Keep in airtight glass or BPA-free containers away from heat and moisture.", "Cooked rice can be frozen in meal-sized portions (up to 2 months)."));

        return list;
    }

    public static String getFridgeZonesGuide() {
        return "🌡️ REFRIGERATOR TEMPERATURE ZONES GUIDE:\n\n"
                + "• Top Shelves (Most consistent temp): Leftovers, drinks, ready-to-eat foods, yogurt.\n\n"
                + "• Middle Shelves: Milk, cheeses, deli meats.\n\n"
                + "• Bottom Shelf (Coldest zone): Raw meat, poultry, and fish in sealed containers to avoid dripping.\n\n"
                + "• Crisper Drawers: Vegetables (high humidity) and fruits (low humidity).\n\n"
                + "• Door Shelves (Warmest zone): Condiments, butter, jams, juices. (Never store milk or eggs here!)";
    }

    public static String getGeneralSafetyNotice() {
        return "⚠️ Food Safety Notice: Expiration estimates are guidelines. Always inspect food for signs of spoilage (off-odors, discoloration, mold, slimy texture). When in doubt, throw it out!";
    }
}