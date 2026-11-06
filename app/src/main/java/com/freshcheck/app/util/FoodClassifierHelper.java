package com.freshcheck.app.util;

import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FoodClassifierHelper {

    public static class ClassificationResult {
        public String foodName;
        public String category;
        public int confidence;
        public List<String> candidateAlternatives;

        public ClassificationResult(String foodName, String category, int confidence, List<String> candidates) {
            this.foodName = foodName;
            this.category = category;
            this.confidence = confidence;
            this.candidateAlternatives = candidates != null ? candidates : new ArrayList<String>();
        }
    }

    // Generic words to exclude as food names
    private static final Set<String> GENERIC_STOPWORDS = new HashSet<>(Arrays.asList(
            "food", "ingredient", "produce", "dish", "cuisine", "recipe", "plant",
            "tableware", "plate", "bowl", "cutlery", "kitchenware", "plastic", "bottle",
            "box", "container", "label", "product", "yellow", "red", "green", "white",
            "close-up", "photography", "wood", "metal", "glass", "indoor", "liquid",
            "drink", "snack food", "fast food", "junk food", "comfort food", "natural foods",
            "local food", "finger food", "vegan nutrition", "vegetarian food", "staple food",
            "vegetable", "fruit", "fruit tree", "root vegetable", "leaf vegetable", "nightshade family"
    ));

    // Rich catalog of specific food items mapped to categories
    private static final Map<String, String> FOOD_CATALOG = new HashMap<>();

    static {
        // Vegetables
        putFood("Tomato", "Vegetables");
        putFood("Potato", "Vegetables");
        putFood("Onion", "Vegetables");
        putFood("Garlic", "Vegetables");
        putFood("Ginger", "Vegetables");
        putFood("Carrot", "Vegetables");
        putFood("Cucumber", "Vegetables");
        putFood("Cabbage", "Vegetables");
        putFood("Cauliflower", "Vegetables");
        putFood("Broccoli", "Vegetables");
        putFood("Spinach", "Vegetables");
        putFood("Lettuce", "Vegetables");
        putFood("Eggplant", "Vegetables");
        putFood("Aubergine", "Vegetables");
        putFood("Bell Pepper", "Vegetables");
        putFood("Capsicum", "Vegetables");
        putFood("Chili", "Vegetables");
        putFood("Peas", "Vegetables");
        putFood("Green Beans", "Vegetables");
        putFood("Mushroom", "Vegetables");
        putFood("Pumpkin", "Vegetables");
        putFood("Radish", "Vegetables");
        putFood("Corn", "Vegetables");
        putFood("Okra", "Vegetables");
        putFood("Lady Finger", "Vegetables");

        // Fruits
        putFood("Apple", "Fruits");
        putFood("Green Apple", "Fruits");
        putFood("Red Apple", "Fruits");
        putFood("Banana", "Fruits");
        putFood("Orange", "Fruits");
        putFood("Citrus", "Fruits");
        putFood("Mango", "Fruits");
        putFood("Lemon", "Fruits");
        putFood("Lime", "Fruits");
        putFood("Watermelon", "Fruits");
        putFood("Papaya", "Fruits");
        putFood("Grape", "Fruits");
        putFood("Pineapple", "Fruits");
        putFood("Strawberry", "Fruits");
        putFood("Berry", "Fruits");
        putFood("Guava", "Fruits");
        putFood("Pomegranate", "Fruits");
        putFood("Coconut", "Fruits");
        putFood("Peach", "Fruits");
        putFood("Pear", "Fruits");
        putFood("Avocado", "Fruits");
        putFood("Kiwi", "Fruits");

        // Dairy
        putFood("Milk", "Dairy");
        putFood("Fresh Milk", "Dairy");
        putFood("Cow Milk", "Dairy");
        putFood("Yogurt", "Dairy");
        putFood("Curd", "Dairy");
        putFood("Cheese", "Dairy");
        putFood("Cheddar", "Dairy");
        putFood("Butter", "Dairy");
        putFood("Ghee", "Dairy");
        putFood("Cream", "Dairy");
        putFood("Paneer", "Dairy");
        putFood("Egg", "Dairy");
        putFood("Eggs", "Dairy");

        // Meat
        putFood("Chicken", "Meat");
        putFood("Chicken Breast", "Meat");
        putFood("Beef", "Meat");
        putFood("Mutton", "Meat");
        putFood("Lamb", "Meat");
        putFood("Duck", "Meat");
        putFood("Turkey", "Meat");
        putFood("Meat", "Meat");

        // Fish
        putFood("Fish", "Fish");
        putFood("Salmon", "Fish");
        putFood("Tuna", "Fish");
        putFood("Prawn", "Fish");
        putFood("Shrimp", "Fish");
        putFood("Crab", "Fish");
        putFood("Tilapia", "Fish");
        putFood("Rohu", "Fish");
        putFood("Hilsa", "Fish");

        // Bakery
        putFood("Bread", "Bakery");
        putFood("Sourdough Bread", "Bakery");
        putFood("White Bread", "Bakery");
        putFood("Brown Bread", "Bakery");
        putFood("Toast", "Bakery");
        putFood("Bun", "Bakery");
        putFood("Croissant", "Bakery");
        putFood("Biscuit", "Bakery");
        putFood("Cookie", "Bakery");
        putFood("Cake", "Bakery");
        putFood("Pastry", "Bakery");
        putFood("Muffin", "Bakery");

        // Grains
        putFood("Rice", "Grains");
        putFood("Basmati Rice", "Grains");
        putFood("Wheat", "Grains");
        putFood("Flour", "Grains");
        putFood("Oats", "Grains");
        putFood("Lentils", "Grains");
        putFood("Dal", "Grains");
        putFood("Pasta", "Grains");
        putFood("Noodles", "Grains");
        putFood("Spaghetti", "Grains");

        // Beverages
        putFood("Tea", "Beverages");
        putFood("Green Tea", "Beverages");
        putFood("Coffee", "Beverages");
        putFood("Juice", "Beverages");
        putFood("Orange Juice", "Beverages");
        putFood("Apple Juice", "Beverages");
        putFood("Soda", "Beverages");
        putFood("Mineral Water", "Beverages");

        // Snacks
        putFood("Chips", "Snacks");
        putFood("Crisps", "Snacks");
        putFood("Popcorn", "Snacks");
        putFood("Nuts", "Snacks");
        putFood("Peanut", "Snacks");
        putFood("Cashew", "Snacks");
        putFood("Almond", "Snacks");
        putFood("Chocolate", "Snacks");
    }

    private static void putFood(String name, String cat) {
        FOOD_CATALOG.put(name.toLowerCase(Locale.US), cat);
    }

    public static ClassificationResult classify(List<ImageLabel> labels, Text ocrText) {
        List<String> candidates = new ArrayList<>();
        String bestName = null;
        String bestCat = "Vegetables";
        int confidence = 85;

        // 1. Check OCR Text for packaged food items / brands
        if (ocrText != null && ocrText.getText() != null && !ocrText.getText().trim().isEmpty()) {
            String lowerOcr = ocrText.getText().toLowerCase(Locale.US);
            for (Map.Entry<String, String> entry : FOOD_CATALOG.entrySet()) {
                String foodKey = entry.getKey();
                if (lowerOcr.contains(foodKey)) {
                    bestName = capitalize(foodKey);
                    bestCat = entry.getValue();
                    confidence = 94;
                    candidates.add(bestName);
                    break;
                }
            }
        }

        // 2. Inspect Image Labels
        if (labels != null && !labels.isEmpty()) {
            // Sort descending by confidence
            List<ImageLabel> sortedLabels = new ArrayList<>(labels);
            Collections.sort(sortedLabels, new Comparator<ImageLabel>() {
                @Override
                public int compare(ImageLabel o1, ImageLabel o2) {
                    return Float.compare(o2.getConfidence(), o1.getConfidence());
                }
            });

            for (ImageLabel label : sortedLabels) {
                String raw = label.getText().trim();
                String lower = raw.toLowerCase(Locale.US);

                if (GENERIC_STOPWORDS.contains(lower)) {
                    continue; // Skip generic tags like "Food", "Tableware"
                }

                // Check exact catalog match
                if (FOOD_CATALOG.containsKey(lower)) {
                    String matchedCat = FOOD_CATALOG.get(lower);
                    String matchedName = capitalize(lower);
                    if (bestName == null) {
                        bestName = matchedName;
                        bestCat = matchedCat;
                        confidence = Math.max(confidence, Math.round(label.getConfidence() * 100));
                    }
                    if (!candidates.contains(matchedName)) {
                        candidates.add(matchedName);
                    }
                } else {
                    // Check partial catalog match
                    for (Map.Entry<String, String> entry : FOOD_CATALOG.entrySet()) {
                        if (lower.contains(entry.getKey()) || entry.getKey().contains(lower)) {
                            String matchedName = capitalize(entry.getKey());
                            if (bestName == null) {
                                bestName = matchedName;
                                bestCat = entry.getValue();
                                confidence = Math.max(confidence, Math.round(label.getConfidence() * 100));
                            }
                            if (!candidates.contains(matchedName)) {
                                candidates.add(matchedName);
                            }
                            break;
                        }
                    }

                    // Also add plausible specific label as a candidate
                    if (!candidates.contains(capitalize(raw)) && raw.length() >= 3) {
                        candidates.add(capitalize(raw));
                    }
                }
            }
        }

        // 3. Fallback if no specific food match was found
        if (bestName == null) {
            if (!candidates.isEmpty()) {
                bestName = candidates.get(0);
                bestCat = inferCategory(bestName);
            } else {
                bestName = "Fresh Produce";
                bestCat = "Vegetables";
                confidence = 70;
                candidates.add("Apple");
                candidates.add("Milk");
                candidates.add("Bread");
                candidates.add("Tomato");
            }
        }

        // Limit candidates to 5
        if (candidates.size() > 5) {
            candidates = candidates.subList(0, 5);
        }

        return new ClassificationResult(bestName, bestCat, confidence, candidates);
    }

    public static String inferCategory(String food) {
        if (food == null) return "Vegetables";
        String lower = food.toLowerCase(Locale.US);

        if (FOOD_CATALOG.containsKey(lower)) {
            return FOOD_CATALOG.get(lower);
        }

        if (lower.contains("apple") || lower.contains("banana") || lower.contains("orange") ||
                lower.contains("fruit") || lower.contains("berry") || lower.contains("mango") ||
                lower.contains("grape") || lower.contains("lemon") || lower.contains("melon")) {
            return "Fruits";
        } else if (lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") ||
                lower.contains("dairy") || lower.contains("butter") || lower.contains("egg") ||
                lower.contains("curd") || lower.contains("paneer")) {
            return "Dairy";
        } else if (lower.contains("chicken") || lower.contains("meat") || lower.contains("beef") ||
                lower.contains("mutton") || lower.contains("pork") || lower.contains("lamb")) {
            return "Meat";
        } else if (lower.contains("fish") || lower.contains("salmon") || lower.contains("shrimp") ||
                lower.contains("prawn") || lower.contains("crab") || lower.contains("tuna")) {
            return "Fish";
        } else if (lower.contains("bread") || lower.contains("toast") || lower.contains("bakery") ||
                lower.contains("cake") || lower.contains("cookie") || lower.contains("biscuit") ||
                lower.contains("muffin") || lower.contains("pastry")) {
            return "Bakery";
        } else if (lower.contains("rice") || lower.contains("grain") || lower.contains("oat") ||
                lower.contains("flour") || lower.contains("dal") || lower.contains("lentil") ||
                lower.contains("pasta") || lower.contains("noodle")) {
            return "Grains";
        } else if (lower.contains("juice") || lower.contains("tea") || lower.contains("coffee") ||
                lower.contains("drink") || lower.contains("water") || lower.contains("beverage") ||
                lower.contains("soda")) {
            return "Beverages";
        } else if (lower.contains("chip") || lower.contains("snack") || lower.contains("nut") ||
                lower.contains("popcorn") || lower.contains("chocolate") || lower.contains("candy")) {
            return "Snacks";
        } else {
            return "Vegetables";
        }
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
